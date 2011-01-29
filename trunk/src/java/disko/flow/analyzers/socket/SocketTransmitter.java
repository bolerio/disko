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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.flow.analyzers.FileReaderAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;

public class SocketTransmitter<C> extends AbstractProcessor<C>
{

    private static Log log = LogFactory.getLog(SocketTransmitter.class);

    private String host;

    private int port;

    public SocketTransmitter(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public void writeMessage(Message message)
    {
        log.debug("Establishing connection to " + host + ":" + port);
        while (true)
        {
            Socket socket = null;
            ObjectOutputStream out = null;
            try
            {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(message);
                out.flush();
                log.debug("Sent " + message);
                return;
            }
            catch (UnknownHostException e)
            {
                log.error("Error connecting to " + host + ":" + port, e);
            }
            catch (IOException e)
            {
                log.error("Error sending to " + host + ":" + port + " message "
                          + message.getData(), e);
            }
            finally
            {
                if (out != null)
                    try
                    {
                        out.close();
                    }
                    catch (Throwable ignored)
                    {
                        log.error("While closing transmitter output.", ignored);
                    }
                if (socket != null)
                    try
                    {
                        socket.close();
                    }
                    catch (Throwable ignored)
                    {
                        log.error("While closing transmitter socket.", ignored);

                    }
            }
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignored)
            {
                break;
            }
        }
    }

    public void process(C ctx, Ports ports) throws InterruptedException
    {
        HashSet<InputPort<?>> closedPorts = new HashSet<InputPort<?>>();
        log.debug("starting socket transmitter.");
        while (closedPorts.size() < ports.getInputCount())
        {
            for (InputPort<?> inputPort : ports.getInputPorts())
            {
                if (closedPorts.contains(inputPort))
                    continue;
                Object data = inputPort.take();
                if (inputPort.isEOS(data))
                {
                    closedPorts.add(inputPort);
                    log.debug("Input port " + inputPort + " closed.");
                    final String channelId = inputPort.getChannel().getId();
                    writeMessage(new Message(channelId, null));
                    log.debug("Sent EOS to " + channelId);
                    continue;
                }
                final Message message = new Message(inputPort.getChannel().getId(),
                                                    (Serializable) data);
                log.debug("Sending msg " + data);
                writeMessage(message);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException,
                                          ExecutionException
    {
        AnalysisContext<TextDocument> ctx = null;
        DataFlowNetwork<AnalysisContext<TextDocument>> network = 
        	new DataFlowNetwork<AnalysisContext<TextDocument>>(ctx);

        network.addChannel(new Channel<String>(SentenceAnalyzer.TEXT_CHANNEL,
                                               "\0"));

        network.addChannel(new Channel<SentenceAnn>(SentenceAnalyzer.SENTENCE_CHANNEL,
                                                    new SentenceAnn(0, 0)));

        network.addNode(new FileReaderAnalyzer<AnalysisContext<TextDocument>>("test/headstart.txt"),
                        new String[] {}, new String[]
                        { SentenceAnalyzer.TEXT_CHANNEL });

        network.addNode(new SentenceAnalyzer(), new String[]
        { SentenceAnalyzer.TEXT_CHANNEL }, new String[]
        { SentenceAnalyzer.SENTENCE_CHANNEL });

        network.addNode(new SocketTransmitter<AnalysisContext<TextDocument>>("localhost",
                                                                             10000),
                        new String[]
                        { SentenceAnalyzer.SENTENCE_CHANNEL }, new String[] {});

        while (true)
        {
            final Future<?> finished = network.start();
            finished.get();
        }
    }
}
