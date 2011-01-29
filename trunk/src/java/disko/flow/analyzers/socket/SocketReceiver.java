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
package disko.flow.analyzers.socket;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.flow.analyzers.ConsoleOutputAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;

public class SocketReceiver<C> extends AbstractProcessor<C>
{
    private static Log log = LogFactory.getLog(SocketReceiver.class);

    private int port;
    private boolean ignoreEOF = false; // if true, don't close output ports
                                        // when EOF is received

    private Message readMessage(ServerSocket serverSocket)
    {
        while (true)
        {
            Socket clientSocket = null;
            ObjectInputStream in = null;
            try
            {
                clientSocket = serverSocket.accept();
                in = new ObjectInputStream(clientSocket.getInputStream());
                Message message = null;
                try
                {
                    message = (Message) in.readObject();
                }
                catch (IOException e)
                {
                    log.error("Error reading object.", e);
                    continue;
                }
                catch (ClassNotFoundException e)
                {
                    log.error("Class not found.", e);
                    continue;
                }
                // while (in.read() != -1);
                // if (in.available() > 0)
                // {
                // System.err.println("OOPS, MORE THAT ON SOCKET HASN'T BEEN
                // READ: " + in.available());
                // while (in.available() > 0)
                // in.read();
                // }
                return message;
            }
            catch (SocketTimeoutException ex)
            {
                if (Thread.interrupted())
                    return null;
            }
            catch (InterruptedIOException e) // this doesn't really work, that's why we put an Socket timeout and we check for interruption
            {
                return null;
            }
            catch (IOException e)
            {
                log.error("Could not listen on port: " + port, e);
            }
            finally
            {
                if (in != null)
                    try
                    {
                        in.close();
                    }
                    catch (Throwable t)
                    {
                        log.error("While closing receiver input.", t);
                    }
                if (clientSocket != null)
                    try
                    {
                        clientSocket.close();
                    }
                    catch (Throwable t)
                    {
                        log.error("While closing receiver socket.", t);
                    }
            }
        }
    }

    public SocketReceiver(int port)
    {
        this.port = port;
    }

    public SocketReceiver(int port, boolean ignoreEOF)
    {
        this.port = port;
        this.ignoreEOF = ignoreEOF;
    }

    @SuppressWarnings("unchecked")
    public void process(C ctx, Ports ports) throws InterruptedException
    {
        log.debug("Listening to port " + port);
        ServerSocket serverSocket = null;

        try
        {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(5000);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        log.debug("Server socket created.");
        Set<OutputPort> closedPorts = new HashSet<OutputPort>();
        try
        {
            while (closedPorts.size() < ports.getOutputCount())
            {
                Message message = readMessage(serverSocket);
                if (message == null)
                    break;                
                OutputPort output = ports.getOutput(message.getChannelId());
                if (output == null)
                {
                    log.error("OutputPort to channel " + message.getChannelId()
                              + " not found!");
                }
                else  if (message.getData() == null)                    
                {
                    log.debug("Received EOS of " + message.getChannelId());
                    if (!ignoreEOF)
                    {
                        output.close();
                        closedPorts.add(output);
                    }
                }
                else if (output.put(message.getData()))
                {
                    log.debug("Accepted " + message);
                }
                else
                {
                    closedPorts.add(output);
                    log.debug("Ignoring " + message);
                }
            }
        }
        catch (Throwable t)
        {
            log.error("Exception at SocketReceiver.", t);
        }
        log.debug("All SocketREceiver outputs closed, exiting normally.");
        try
        {
            serverSocket.close();
        }
        catch (Exception ioe)
        {
            log.error(ioe);
        }
    }

    public static void main(String[] args) throws InterruptedException,
                                          ExecutionException
    {
        AnalysisContext<TextDocument> ctx = null;
        DataFlowNetwork<AnalysisContext<TextDocument>> network = 
        	new DataFlowNetwork<AnalysisContext<TextDocument>>(ctx);

        network.addChannel(new Channel<SentenceAnn>(SentenceAnalyzer.SENTENCE_CHANNEL,
                                                    new SentenceAnn(0, 0)));

        network.addNode(new SocketReceiver<AnalysisContext<TextDocument>>(10000),
                        new String[] {}, new String[]
                        { SentenceAnalyzer.SENTENCE_CHANNEL });

        network.addNode(new ConsoleOutputAnalyzer(), new String[]
        { SentenceAnalyzer.SENTENCE_CHANNEL }, new String[] {});

        while (true)
        {
            network.start().get();
        }

    }
}
