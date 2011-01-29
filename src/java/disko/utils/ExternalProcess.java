/*******************************************************************************
 * Copyright (c) 2005, Kobrix Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Borislav Iordanov - initial API and implementation
 *     Murilo Saraiva de Queiroz - initial API and implementation
 ******************************************************************************/
package disko.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hypergraphdb.util.Mapping;

/**
 * 
 * <p>
 * Run an external process and a sub-process to the JVM. The external process if
 * bound to a separate Java thread. The Java thread
 * will wait and monitor the process until it is complete. You can configure
 * stderr, stdout and stdin and you can explicitly kill the process.
 * </p>
 * 
 * <p>
 * Since there's no standard mechanism to get notified when an external process
 * ends, this thread will monitor by checking that the process is still alive
 * every X milliseconds - the <code>sleepTime</code> property of this class whose
 * default value is 2000 milliseconds.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class ExternalProcess extends Thread
{
    private List<String> command = new ArrayList<String>();
    private File workingDirectory;
    private Map<String, String> environment = new HashMap<String, String>();
    private long keepAliveCount;
    private boolean killOnShutdown;
    private boolean mergeStdoutStderr; 
    private long sleepTime = 2000;
    private int inoutBufferSize = 1024;
    private File stdoutFile;
    private File stderrFile;
    private File stdinFile;
    FileOutputStream stdout = null;
    FileOutputStream stderr = null;
    FileInputStream stdin = null;
    private boolean killedByUser = false;
    private Process theProcess;       
    private Mapping<ExternalProcess, Boolean> alreadyRunning = null;
    
    private static void syntax()
    {
        System.out.println("ExternalProcess [OPTIONS] program arguments");
        System.out.println("OPTIONS are:");
        System.out.println("-dir PATH             working directory");
        System.out.println("-restart COUNT        number of times to restart (-1 for indefinitely)");
        System.out.println("-stdout FILENAME      send standard output to that file");
        System.out.println("-stderr FILENAME      send standard error to that file");
        System.out.println("-stdin  FILENAME      read standard input from that file");
    }
    
    public static void main(String [] argv)
    {
        if (argv.length < 1)
        {
            syntax();
            System.exit(0);
        }
        
        ExternalProcess P = new ExternalProcess();
        int arg = 0;
        while (arg < argv.length)
        {
            if (argv[arg].startsWith("-"))
            {
                if (arg >= argv.length - 1)
                {
                    System.out.println("Missing option value for " + argv[arg]);
                    syntax();
                    System.exit(-1);
                }
            }
            else
                break;
            
            String option = argv[arg++];
            
            if (option.equals("-dir"))
                P.setWorkingDirectory(new File(argv[arg++]));
            else if (option.equals("-restart"))
            {
                P.setKeepAliveCount(Long.parseLong(argv[arg++]));
                if (P.getKeepAliveCount() < 0)
                    P.setKeepAliveCount(Long.MAX_VALUE);
            }
            else if (option.equals("-stdout"))
                P.setStdoutFile(new File(argv[arg++]));
            else if (option.equals("-stderr"))
                P.setStderrFile(new File(argv[arg++]));
            else if (option.equals("-stdin"))
                P.setStdinFile(new File(argv[arg++]));
            else
            {
                System.out.println("Unrecognized options " + argv[arg]);
                syntax();
                System.exit(-1);
            }
        }
        if (arg >= argv.length)
        {
            System.out.println("Missing program and arguments");
            syntax();
            System.exit(-1);
        }
        String fullCommand = "";
        while (arg < argv.length)
        {
            fullCommand += argv[arg] + " ";
            P.getCommandList().add(argv[arg++]);            
        }
        System.out.println("Running " + fullCommand);
        P.run();
    }
    
    private void initiate()
    {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory);
        builder.environment().putAll(environment);
        if (mergeStdoutStderr)
            builder.redirectErrorStream(true);
        
        try
        {
            if (stdoutFile != null)
                stdout = new FileOutputStream(stdoutFile);
            if (stderrFile != null)
                stderr = new FileOutputStream(stderrFile);
            if (stdinFile != null)
                stdin = new FileInputStream(stdinFile);
            theProcess = builder.start();
            System.out.println("Processing started " + theProcess);
        }
        catch (java.io.IOException ex)
        {
            throw new RuntimeException(ex);
        }              
    }
    
    private synchronized void closeStreams()
    {
        if (stdout != null)
            try { stdout.close(); stdout = null; } catch (Throwable t) { }
        if (stderr != null)
            try { stderr.close(); stderr = null; } catch (Throwable t) { }
        if (stdin != null)
            try { stdin.close(); stdin = null; } catch (Throwable t) { }            
    }
    
    /**
     * Return true if some data was transferred b/w in and out
     * @param in
     * @param out
     * @param buffer
     * @return
     */
    private boolean connectInOut(InputStream in, OutputStream out, byte [] buffer) throws IOException
    {
        if (in == null) in = System.in; // return false;
        int read = Math.min(buffer.length, in.available());
        System.out.println("Read " + read + " bytes.");
        if (read <= 0) return false;
        read = in.read(buffer, 0, read);
        if (read <= 0) return false;     
        if (out == null)
            out = System.out;
        out.write(buffer, 0, read);
        return true;
    }
    
    public void run()
    {
        ensureShutdownHook();
        if (alreadyRunning == null || !alreadyRunning.eval(this))
        {
        	initiate();
        	processes.add(this);
        }
        byte [] buffer = new byte[inoutBufferSize];
        while (true)
        {
            if (!isStillRunning() && (alreadyRunning == null || !alreadyRunning.eval(this)))
            {
                System.out.println("Process dead,closing streams and maybe restart.");
            	if (theProcess != null)
            		closeStreams();
                if (killedByUser || keepAliveCount <= 0)
                    break;
                else
                {
                    keepAliveCount--;
                    initiate();
                }
            }
            
            try
            {
                boolean iddle = false;                
                while (!iddle && theProcess != null)
                {
                    iddle = !connectInOut(stdin, theProcess.getOutputStream(), buffer) &&
                            !connectInOut(theProcess.getInputStream(), stdout, buffer) &&
                            !connectInOut(theProcess.getErrorStream(), stderr, buffer);
                }            
                Thread.sleep(sleepTime);
            }
            catch (Throwable ex)
            {
                kill();
            }
        }
       	processes.remove(this);
    }
    
    public int getExitCode()
    {
        return theProcess.exitValue();
    }
    
    public boolean isStillRunning()
    {
        if (theProcess == null)
            return false;
        try
        {
            theProcess.exitValue();
            return false;
        }
        catch (IllegalThreadStateException ex)
        {
            return true;
        }
    }
    
    public synchronized void kill()
    {
    	if (theProcess == null)
    		return;
        if (isAlive())
            theProcess.destroy();
        closeStreams();
        processes.remove(this);
        killedByUser = true;
    }
    
    public InputStream getStdout()
    {
        return theProcess.getInputStream();
    }
    
    public InputStream getStderr()
    {
        return theProcess.getErrorStream();
    }
    
    public OutputStream getStdin()
    {
        return theProcess.getOutputStream();
    }
    
    private static Thread shutdownHook = null;
    private static List<ExternalProcess> processes = 
        Collections.synchronizedList(new ArrayList<ExternalProcess>());
    
    private static synchronized void ensureShutdownHook()
    {
        if (shutdownHook == null)
        {
            shutdownHook = new Thread()
            {
                public void run()
                {
                    for (ExternalProcess p : processes)
                        if (p.isKillOnShutdown())
                            p.kill();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public String getCommand()
    {
        if (command.isEmpty()) 
            return null;
        else
            return command.get(0);
    }
    
    public void setCommand(String command)
    {
    	if (this.command == null)    	
    		this.command = new ArrayList<String>();
    	if (this.command.isEmpty())
    		this.command.add(command);
    	else
    		this.command.set(0, command);
    }
    
    public String [] getArguments()
    {
        if (command.size() <= 1)
            return new String[0];
        else
        {
            String [] A = new String[command.size() - 1];
            for (int i = 1; i < command.size(); i++)
                A[i-1] = command.get(i);
            return A;
        }
    }

    
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    public long getKeepAliveCount()
    {
        return keepAliveCount;
    }

    public void setKeepAliveCount(long keepAliveCount)
    {
        this.keepAliveCount = keepAliveCount;
    }

    public boolean isKillOnShutdown()
    {
        return killOnShutdown;
    }

    public void setKillOnShutdown(boolean killOnShutdown)
    {
        this.killOnShutdown = killOnShutdown;
    }

    public boolean isMergeStdoutStderr()
    {
        return mergeStdoutStderr;
    }

    public void setMergeStdoutStderr(boolean mergeStdoutStderr)
    {
        this.mergeStdoutStderr = mergeStdoutStderr;
    }

    public long getSleepTime()
    {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime)
    {
        this.sleepTime = sleepTime;
    }

    public int getInoutBufferSize()
    {
        return inoutBufferSize;
    }

    public void setInoutBufferSize(int inoutBufferSize)
    {
        this.inoutBufferSize = inoutBufferSize;
    }

    public File getStdoutFile()
    {
        return stdoutFile;
    }

    public void setStdoutFile(File stdoutFile)
    {
        this.stdoutFile = stdoutFile;
    }

    public String getStdoutFilename()
    {
    	return stdoutFile == null ? null : stdoutFile.getAbsolutePath();
    }

    public void setStdoutFilename(String stdoutFilename)
    {
        this.stdoutFile = new File(stdoutFilename);
    }
    
    public File getStderrFile()
    {
        return stderrFile;
    }

    public void setStderrFile(File stderrFile)
    {
        this.stderrFile = stderrFile;
    }

    public String getStderrFilename()
    {
        return stderrFile == null ? null : stderrFile.getAbsolutePath();
    }

    public void setStderrFilename(String stderrFilename)
    {
        this.stderrFile = new File(stderrFilename);
    }
    
    public File getStdinFile()
    {
        return stdinFile;
    }

    public void setStdinFile(File stdinFile)
    {
        this.stdinFile = stdinFile;
    }

    public String getStdinFilename()
    {
        return stdinFile == null ? null : stdinFile.getAbsolutePath();
    }

    public void setStdinFilename(String stdinFilename)
    {
        this.stdinFile = new File(stdinFilename);
    }
    
    public Map<String, String> getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment)
    {
        this.environment = environment;
    }

	public void setCommandList(List<String> command)
	{
		this.command = command;
	}
    
	public List<String> getCommandList()
	{
		return command;
	}

	public Mapping<ExternalProcess, Boolean> getAlreadyRunning()
	{
		return alreadyRunning;
	}

	public void setAlreadyRunning(Mapping<ExternalProcess, Boolean> alreadyRunning)
	{
		this.alreadyRunning = alreadyRunning;
	}	
}
