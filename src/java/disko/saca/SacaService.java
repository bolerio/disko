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
package disko.saca;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.peer.Structs;
import org.hypergraphdb.util.Mapping;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import disko.AnalysisContext;
import disko.DU;
import disko.StringTextDocument;
import disko.TextDocument;
import disko.flow.networks.SearchQueryNetwork;

public class SacaService
{
    public static final String ACT = "performative";
    public static final String QUERY_REQUEST = "QueryRef";
    public static final String QUERY_RESPONSE = "InformRef";
    public static final String FAILURE = "Failure";
    public static final String PING = "Ping";
    public static final String PONG = "Pong";
    public static final String REASON = "reason";
    
    @Option(name = "-v", usage="detailed tracing")
    private boolean verbose = false;
    @Option(name = "-threads", usage="number of client handling threads")
    private int threadPoolSize = 4;
    @Option(name = "-port", usage="server port number to which clients connect")
    private int port = 9696;
    @Option(name = "-graph", required = true, usage="the HyperGraph database")
    private String graphLocation;
    @Option(name = "-pp", usage="the name of a result post-processing class")
    private String postProcessorClassName;
    @Option(name = "-lgport", usage="the port of the LinkGrammar server")
    private int lgPort = 9000;
    @Option(name = "-lghost", usage="the hostname of the LinkGrammar server")
    private String lgHost = "localhost";
    @Option(name = "-lgparses", usage="the maximum number of parses of the LinkGrammar server")
    private int lgMaxParses;
    @Option(name = "-lgseconds", usage="the maximum number of seconds for LinkGrammar processing")
    private int lgMaxSeconds;
    
    private HyperGraph graph;
    private Mapping<Object, Object> postProcessor = null;
    private volatile boolean running = false;
    
    public static void main(String [] argv)
    {
        SacaService service = new SacaService();
        CmdLineParser parser = new CmdLineParser(service);
        try
        {
            parser.parseArgument(argv);
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java SacaService [options...]");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            return;
        }
        service.start();
    }

    @SuppressWarnings("unchecked")
	public void start()
    {
        System.setProperty("gate.home", "c:/tools/gate");
        System.setProperty("relex.morphy.Morphy", "org.disco.relex.MorphyHGDB");
        System.setProperty("morphy.hgdb.location", graphLocation);
        System.setProperty("EnglishModelFilename", "c:/work/disco/trunk/data/sentence-detector/EnglishSD.bin.gz");        
        System.setProperty("opennlp.model.namedentity", "c:/work/disco/trunk/data/namedentity");        
        graph = HGEnvironment.get(graphLocation);
        if (postProcessorClassName != null)
        {
            try { postProcessor = (Mapping<Object, Object>)Class.forName(postProcessorClassName).newInstance(); }
            catch (Exception ex) { throw new RuntimeException(ex); }
        }
        
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(threadPoolSize, 
                                                               threadPoolSize,
                                                               Long.MAX_VALUE, 
                                                               TimeUnit.SECONDS,
                                                               new LinkedBlockingQueue<Runnable>());
        try
        {            
            ServerSocket serverSocket = new ServerSocket(port);
//            serverSocket.setSoTimeout(2000);
            DU.log.info("Waiting for client connections on port " + port + "...");            
            for (running = true; running; )
            {                
                try
                {
                    final Socket clientSocket = serverSocket.accept();              
                    threadPool.submit(new Runnable() { public void run() { handleClient(clientSocket); } });
                }
                catch (SocketTimeoutException ex)
                {
                    // ignore, we just want to check if 'stop' has been called here
                    DU.log.info("Timed out, loop again.");
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
        finally
        {
            running = false;
            threadPool.shutdownNow();
        }
    }

    public void stop()
    {
        running = false;
    }
        
    public boolean isRunning()
    {
        return running;
    }
    
    public String getPostProcessorClassName()
    {
        return postProcessorClassName;
    }

    public void setPostProcessorClassName(String postProcessorClassName)
    {
        this.postProcessorClassName = postProcessorClassName;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    public int getThreadPoolSize()
    {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize)
    {
        this.threadPoolSize = threadPoolSize;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getGraphLocation()
    {
        return graphLocation;
    }

    public void setGraphLocation(String graphLocation)
    {
        this.graphLocation = graphLocation;
    }
    
    private SearchQueryNetwork getSearchNetwork(String question)
    {
        SearchQueryNetwork network = new SearchQueryNetwork(); 
        network.setContext(new AnalysisContext<TextDocument>(graph, 
                                                             new StringTextDocument(question)));
        network.setUseOpenNlpEntityDetection(true);
        network.setDebug(true);
        network.create();
        network.getLinkGrammarProcessor().setHost(lgHost);
        network.getLinkGrammarProcessor().setPort(lgPort);
        network.getLinkGrammarProcessor().getConfig().setMaxLinkages(lgMaxParses);
        network.getLinkGrammarProcessor().getConfig().setMaxParseSeconds(lgMaxSeconds);
        return network;
    }
     
    private Object doSearch(Map<String, String> query) throws InterruptedException, ExecutionException
    {
        String question = query.get("question");
        if (question == null)
            throw new RuntimeException("Missing question parameter from search query.");        
        SearchQueryNetwork net = getSearchNetwork(question);
        Future<?> future = net.start();
        future.get();
        DiskoSearch search = new DiskoSearch(graph, net.getAccumulator().getData());
        SortedSet<DiskoSearch.Result> resultSet = search.search(graph, 20);       
        ArrayList<Object> L = new ArrayList<Object>();
        for (DiskoSearch.Result R : resultSet)
        {
            Object x = Structs.struct("handle", R.resource, "score", R.score, "atom", 
                                      graph.get(R.resource));
            if (postProcessor != null)
                x = postProcessor.eval(x);
            
            L.add(x);
        }
        return Structs.struct(ACT, QUERY_RESPONSE, "result-list", L);
    }
    
    private void handleClient(Socket clientSocket)
    {
        try
        {
            DU.log.info("Connection accepted from : "
                        + clientSocket.getInetAddress());
            Map<String, String> msg = Saca.readMsg(clientSocket);
            if (verbose)
                DU.log.debug("Received msg '" + msg + "' from "
                             + clientSocket.getInetAddress());
            String act = msg.get(ACT);
            if (QUERY_REQUEST.equals(act))
            {
                try
                {                    
                    Saca.writeMsg(clientSocket, doSearch(msg));
                }
                catch (Throwable t)
                {
                	t.printStackTrace(System.err);
                    Saca.writeMsg(clientSocket, Structs.struct(ACT, FAILURE, REASON, t.toString()));
                }
            }
            else
            {
                Saca.writeMsg(clientSocket, Structs.struct(ACT, FAILURE, REASON, "unknown act '" + act + "'"));
            }
            DU.log.info("Response written to " + clientSocket.getInetAddress()
                  + ", closing client connection...");
        }
        catch (Throwable t)
        {
            DU.log.error(t);
        }
        finally
        {
            if (clientSocket != null)
                try
                {
                    clientSocket.close();
                }
                catch (Throwable t)
                {
                }
        }
    }
}
