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
package disko.flow.analyzers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

public class FileReaderAnalyzer<C> extends AbstractProcessor<C>
{
    private static Log log = LogFactory.getLog(FileReaderAnalyzer.class);

    private File file;

    public FileReaderAnalyzer(File file)
    {
        this.file = file;
    }

    public FileReaderAnalyzer(String fileName)
    {
        this.file = new File(fileName);
    }

    public void process(C ctx, Ports ports) throws InterruptedException
    {
        log.debug("File Reader started");
        BufferedReader in = null;
        try
        {
            in = new BufferedReader(new FileReader(file));
            OutputPort<String> singleOutputPort = ports.getSingleOutput();
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = in.readLine()) != null)
            {
                sb.append(line);
            }
            singleOutputPort.put(sb.toString());
            singleOutputPort.close();

        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
        finally
        {
            if (in != null)
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
        }
        log.debug("File Reader ended");
    }

    public File getFile()
    {
        return file;
    }

    public void setFile(File file)
    {
        this.file = file;
    }    
}
