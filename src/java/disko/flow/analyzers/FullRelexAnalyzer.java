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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.TextDocument;
import relex.algs.SentenceAlgorithmApplier;
import relex.concurrent.RelexContext;
import relex.concurrent.RelexTask;
import relex.concurrent.RelexTaskResult;
import relex.entity.EntityMaintainer;
import relex.morphy.Morphy;
import relex.morphy.MorphyFactory;
import relex.parser.LGParser;
import relex.parser.LocalLGParser;
import relex.parser.RemoteLGParser;

// import relex.tree.PhraseMarkup;

/**
 * <p>
 * Implements the standard Relex processing including Link Grammar parsing
 * and application of Relex's algorithms for semantic relation extraction.
 * </p>
 * 
 * <p>
 * This processor can be configured to use several Link Grammar parsers in
 * parallel. Parsers can be all remote or all local or a mix of remote and
 * local parsers. The remote ones are configured by adding a number of 
 * host/port pairs acting as link grammar servers (the same pair can be 
 * repeated several times) - see the {@link addHost} method . Local parsers 
 * are configured just be setting how many threads you want with the
 * {@link setInProcessParsers} method.  
 * </p>
 */
public class FullRelexAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    public class HostPort implements Serializable, Cloneable
    {
        private static final long serialVersionUID = 204877374662953058L;
        public String host;
        public int port;

        public HostPort(String host, int port)
        {
            this.host = host;
            this.port = port;
        }

        public String toString()
        {
            return host + ":" + port;
        }
    }

    private static Log log = LogFactory.getLog("org.disco");
    private static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_CLIENT_COUNT = 1;
    public static final int DEFAULT_FIRST_PORT = 9000;
    public static final String PARSE_CHANNEL = "PARSE_CHANNEL";

    private transient ExecutorService exec;
    private transient BlockingQueue<RelexContext> pool;
    private ArrayList<HostPort> hosts;
    private int inProcessParsers = 0;
    public int count = 0;

    /** Syntactic processing */
    // private LinkParser linkParser = new LinkParser();
    // Morphological analysis
    private transient Morphy morphy = null;

    /** Semantic (RelEx) processing */
    private transient SentenceAlgorithmApplier sentenceAlgorithmApplier;

    /** Penn tree-bank style phrase structure markup. */
    // private PhraseMarkup phraseMarkup;
    private int maxParses = -1, maxCost = -1, maxParseSeconds = -1;

    private void configureParser(LGParser parser)
    {
        parser.getConfig().setAllowSkippedWords(true);
        if (maxParses > -1)
            parser.getConfig().setMaxLinkages(maxParses);
        if (maxCost > -1)
            parser.getConfig().setMaxCost(maxCost);
        if (maxParseSeconds > -1)
            parser.getConfig().setMaxParseSeconds(maxParseSeconds);        
    }
    
    public FullRelexAnalyzer()
    {
    }

    /**
     * Creates a LinkGrammarAnalzyer using the specified hosts (String) and
     * ports (int), for example new LinkGrammarAnalyzer("localhost", 9000,
     * "192.168.254.10", 8001, ...)
     * 
     * @param hostsAndPorts
     */
    public FullRelexAnalyzer(Object... hostsAndPorts)
    {
        for (int i = 0; i < hostsAndPorts.length; i += 2)
        {
            String host = (String) hostsAndPorts[i];
            int port = (Integer) hostsAndPorts[i + 1];
            addHost(host, port);
        }
    }

    /**
     * Initialize the pool of LinkParserClients, creating CLIENT_POOL_SIZE
     * instances, which connects to ports FIRST_PORT, FIRST_PORT+1, ...,
     * FIRST_PORT+(CLIENT_POOL_SIZE-1)
     */
    private void initializePool() throws InterruptedException
    {
        sentenceAlgorithmApplier = new SentenceAlgorithmApplier();
        // phraseMarkup = new PhraseMarkup();
        if (morphy == null)
            morphy = MorphyFactory.getImplementation();

        if ((hosts == null) || (hosts.size() == 0))
        {
            for (int i = 0; i < DEFAULT_CLIENT_COUNT; i++)
            {
                addHost(DEFAULT_HOST, DEFAULT_FIRST_PORT + i);
            }
        }
        
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // +1 submission thread
        exec = Executors.newFixedThreadPool(hosts.size() + 1,
                                            new ThreadFactory()
                                            {
                                                public Thread newThread(Runnable r)
                                                {
                                                    Thread t = new Thread(r);
                                                    t.setContextClassLoader(loader);
                                                    t.setDaemon(true);
                                                    return t;
                                                }
                                            }                                    
                                            );
        pool = new ArrayBlockingQueue<RelexContext>(hosts.size() + inProcessParsers);

        for (HostPort hp : hosts)
        {
            RemoteLGParser parser = new RemoteLGParser();
            parser.getLinkGrammarClient().setHostname(hp.host);
            parser.getLinkGrammarClient().setPort(hp.port);
            configureParser(parser);
            RelexContext context = new RelexContext(parser, morphy);
            pool.put(context);
        }
        
        for (int i = hosts.size(); i < pool.size(); i++)
        {
            LocalLGParser parser = new LocalLGParser();
            configureParser(parser);
            RelexContext context = new RelexContext(parser, morphy);            
            pool.put(context);
        }
    }

    public void addHost(String host, int port)
    {
        if (hosts == null)
            hosts = new ArrayList<HostPort>();
        hosts.add(new HostPort(host, port));
    }

    public void init()
    {
        try
        {
            initializePool();
        }
        catch (InterruptedException ex)
        {
            throw new RuntimeException("Initialization interrupted.");
        }
    }

    public void destroy()
    {
        // for (RelexContext ctx : pool)
        // ctx.getLinkParserClient().close();
        if (pool != null)
        {
            pool.clear();
            pool = null;
        }
        if (exec != null)
        {
            exec.shutdownNow();
            exec = null;
        }
    }

    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
    {
        if (pool == null)
            init();
        final InputPort<EntityMaintainer> inputPort = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        final OutputPort<RelexTaskResult> outputPort = ports.getOutput(PARSE_CHANNEL);
        final LinkedBlockingQueue<Future<RelexTaskResult>> futureResults = 
            new LinkedBlockingQueue<Future<RelexTaskResult>>(outputPort.getChannel().getCapacity());
        log.debug("Starting LinkGrammarAnalyzer...");
        exec.submit(new Callable<Integer>()
        {
            public Integer call() throws Exception
            {
                try
                {
                    log.debug("LinkGrammarAnalyzer from channel + " + inputPort.getChannel());
                    for (EntityMaintainer em = inputPort.take(); !inputPort.isEOS(em); em = inputPort.take())
                        submitTask(em, futureResults);
                }
                catch (Throwable t)
                {
                    log.error("Unable to submit parsing task.", t);
                }
                finally
                {
                    futureResults.put(new FutureRelexTaskResultEOS());
                }
                return (futureResults.size() - 1);
            }
        });

        try
        {
            while (true)
            {
                try
                {
                    Future<RelexTaskResult> futureResult = futureResults.take();
                    RelexTaskResult relexTaskResult;
                    relexTaskResult = futureResult.get();
                    if (relexTaskResult == null)
                        break;
                    log.debug("LinkGrammarAnalyzer received "
                              + relexTaskResult.index + ": "
                              + relexTaskResult.result.getParses().size()
                              + " parses of sentences "
                              + relexTaskResult.sentence);
                    relexTaskResult.result.setSentence(relexTaskResult.entityMaintainer.getOriginalSentence());
                    outputPort.put(relexTaskResult);
                }
                catch (InterruptedException e)
                {
                    for (Future<RelexTaskResult> future : futureResults)
                    {
                        try
                        {
                            future.cancel(true);
                        }
                        catch (Throwable t)
                        {
                            log.error(t);
                        }
                    }
                    break;
                }
            }
            for (Future<RelexTaskResult> future : futureResults)
            {
                future.cancel(true);
            }
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            outputPort.close();
            /*
             * exec.shutdown(); for (RelexContext context: pool){
             * context.getLinkParserClient().close(); }
             */
            destroy();
        }
    }

    protected void submitTask(EntityMaintainer input, LinkedBlockingQueue<Future<RelexTaskResult>> results)
        throws InterruptedException
    {
        RelexContext context = pool.take();
        Callable<RelexTaskResult> callable = new RelexTask(count++,
                                                           input.getOriginalSentence()
                                                               .replace('\n',' ')
                                                               .replace('\r',' '),
                                                           input,
                                                           sentenceAlgorithmApplier,
                                                           null /* phraseMarkup */,
                                                           context, pool);
        Future<RelexTaskResult> submit = exec.submit(callable);
        log.debug("LinkGrammarAnalyzer submitted " + callable);
        results.put(submit);
    }

    public int getMaxParses()
    {
        return maxParses;
    }

    public void setMaxParses(int maxParses)
    {
        this.maxParses = maxParses;
    }

    public int getMaxCost()
    {
        return maxCost;
    }

    public void setMaxCost(int maxCost)
    {
        this.maxCost = maxCost;
    }

    public int getMaxParseSeconds()
    {
        return maxParseSeconds;
    }

    public void setMaxParseSeconds(int maxParseSeconds)
    {
        this.maxParseSeconds = maxParseSeconds;
    }

    /**
     * <p>Return the number of threads dedicated to in-process instances of the
     * LinkGrammar parser.</p>
     */
    public int getInProcessParsers()
    {
        return inProcessParsers;
    }

    /**
     * <p>
     * Set the number of instances of in-process LinkGrammar parsers to use. 
     * This setting must be specified before this processor is executed. Each
     * LinkGrammar parser, in-process or remote will get its own thread of execution.
     * </p>
     */
    public void setInProcessParsers(int inProcessParsers)
    {
        this.inProcessParsers = inProcessParsers;
    }


    private class FutureRelexTaskResultEOS implements Future<RelexTaskResult>
    {

        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return false;
        }

        public RelexTaskResult get() throws InterruptedException,
                                    ExecutionException
        {
            return null;
        }

        public RelexTaskResult get(long timeout, TimeUnit unit)
        {
            return null;
        }

        public boolean isCancelled()
        {
            return false;
        }

        public boolean isDone()
        {
            return true;
        }
    }
}
