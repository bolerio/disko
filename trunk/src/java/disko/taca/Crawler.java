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
package disko.taca;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hypergraphdb.HGGraphHolder;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.util.Mapping;
import org.hypergraphdb.util.SimpleStack;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import disko.Ann;
import disko.DU;
import disko.DefaultTextDocument;
import disko.HTMLDocument;
import disko.MarkupAnn;
import disko.PDFDocument;
import disko.data.MimeType;
import disko.utils.DiscoProxySettings;

import au.id.jericho.lib.html.HTMLElementName;

public class Crawler implements HGGraphHolder
{
	private HyperGraph graph;
	private Mapping<CrawlResult, Boolean> callback;
	@Option(name = "-threads", required = true, usage="The number of threads to use.")
	private int nThreads = 4;
	private ExecutorService executorService;
	private Map<CrawlRoot, TimerTask> tasks = new HashMap<CrawlRoot, TimerTask>();
	
	@Option(name = "-f", required = true, usage="A HyperGraphPeer JSON configuration file.")
	private String configurationFile = null;
	@Option(name = "-urlHandler", required = true, usage="The class name of the CrawlResult processor.")
	private String callbackClassname = null;
	
	@SuppressWarnings("unchecked")
	public static void main(String [] argv)
	{
        Crawler crawler = new Crawler();
        CmdLineParser parser = new CmdLineParser(crawler);
        try
        {
            parser.parseArgument(argv);
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java org.disko.taca.Crawler [options...]");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            return;
        }
        System.out.println("Starting Crawler configured at: " + crawler.configurationFile);
        try
        {
            HyperGraphPeer thePeer = new HyperGraphPeer(new File(crawler.configurationFile));
            thePeer.getObjectContext().put("crawler", crawler);
            thePeer.start(null, null).get();
            crawler.setHyperGraph(thePeer.getGraph());
            if (crawler.callbackClassname != null)
            {
            	Class<?> cl = Class.forName(crawler.callbackClassname);
            	crawler.setCallback((Mapping<CrawlResult, Boolean>)cl.newInstance());
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            System.exit(-1);
        }
	}
	
	public Crawler()
	{
	}

	public Crawler(HyperGraph graph)
	{
		this.graph = graph;
	}
	
	public Crawler(HyperGraph graph, 
				   Mapping<CrawlResult, Boolean> callback,
				   int nThreads)
	{
		this.graph = graph;
		this.callback = callback;
		this.nThreads = nThreads;
	}

	public void addRoot(CrawlRoot root)
	{
		HGHandle handle = hg.findOne(graph, hg.and(hg.type(CrawlRoot.class), 
												   hg.eq("url", root.getUrl())));
		if (handle == null)
		{
			graph.add(root);
		}
	}
	
	public void removeRoot(CrawlRoot root)
	{
		HGHandle handle = graph.getHandle(root);
		if (handle == null)
			handle = hg.findOne(graph, hg.and(hg.type(CrawlRoot.class), 
											  hg.eq("url", root.getUrl())));
		if (handle != null)
			graph.remove(handle);
	}
	
	public void updateRoot(CrawlRoot root)
	{
        HGHandle handle = graph.getHandle(root);
        if (handle == null)
            handle = hg.findOne(graph, hg.and(hg.type(CrawlRoot.class), 
                                              hg.eq("url", root.getUrl())));
        if (handle != null)
            graph.replace(handle, root);
	}
	
	public CrawlRoot findRoot(String url)
	{
		return hg.getOne(graph, hg.and(hg.type(CrawlRoot.class), 
									   hg.eq("url", url)));
	}

	public List<CrawlRoot> getRoots()
	{
		return hg.getAll(graph, hg.type(CrawlRoot.class));
	}
	
	public HyperGraph getHyperGraph()
	{
		return graph;
	}
	
	public void setHyperGraph(HyperGraph graph)
	{
		this.graph = graph;
	}

	public Mapping<CrawlResult, Boolean> getCallback()
	{
		return callback;
	}

	public void setCallback(Mapping<CrawlResult, Boolean> callback)
	{
		this.callback = callback;
	}
	
	public String getCallbackClassname()
	{
		return callbackClassname;
	}

	public void setCallbackClassname(String callbackClassname)
	{
		this.callbackClassname = callbackClassname;
	}
	
	public String getConfigurationFile()
	{
		return configurationFile;
	}

	public void setConfigurationFile(String configurationFile)
	{
		this.configurationFile = configurationFile;
	}

	public int getNThreads()
	{
		return nThreads;
	}

	public void setNThreads(int threads)
	{
		nThreads = threads;
	}

	public synchronized void start()
	{
		if (executorService != null && !executorService.isTerminated())
			throw new IllegalStateException("The DISCO Crawler is already running (or wasn't properly shut down).");
		executorService = Executors.newFixedThreadPool(nThreads);
		Timer timer = DU.getTimer();
		for (CrawlRoot root : getRoots())
		{
		    if (!root.isActive())
		        continue;
		    TimerTask task = makeTimerTask(root);
			tasks.put(root, task);
			if (root.getLastCrawlTimestamp() == 0) 
				timer.schedule(task, 
							   0, 
							   root.getCrawlingInterval());			
			else
			{
				long delay = root.getLastCrawlTimestamp() + root.getCrawlingInterval() - System.currentTimeMillis();				
				timer.schedule(task, 
							   Math.max(0, delay), 
							   root.getCrawlingInterval());
			}
		}
	}
	
	public synchronized void stop()
	{
		for (TimerTask task : tasks.values())
		{
		    ((CrawlingTask)task).getCrawlingThread().stopRunning();
			task.cancel();
		}
		executorService.shutdownNow();
		tasks.clear();		
	}
	
	public boolean isRunning()
	{
	    return !executorService.isShutdown();
	}

	public int getTaskCount()
	{
	    return tasks == null ? 0 : tasks.size();
	}
	
	private class CrawlingThread implements Runnable
	{
		private volatile boolean running = true;
		private CrawlRoot root;
		private Set<String> examined = new HashSet<String>();
		private SimpleStack<CrawlResult> remaining = new SimpleStack<CrawlResult>();	
		
		private boolean shouldExamine(URL url)
		{
		    String s = url.toExternalForm();
		    if (!s.startsWith("http://") && !s.startsWith("https://") && 
		        !s.startsWith("ftp://") && !s.startsWith("sftp://"))
		        return false;
		    if (root.isDomainRestricted())
		    {
		        URL rootUrl = DU.toUrl(root.getUrl());
		        if (!root.isCrawlSubdomains())
		            return rootUrl.getHost().equals(url.getHost());
		        else
		            return url.getHost().indexOf(rootUrl.getHost()) > -1;
		    }
		    else
		        return true;
		}
		
		private Map<String, Object> collectMetaData(URL url)
		{
		    Map<String, Object> result = new HashMap<String, Object>();
		    try
		    {
		        URLConnection conn = DiscoProxySettings.newConnection(url);
		        String contentType = conn.getContentType();
		        if (contentType != null)
		        {
		            String [] parts = contentType.split(";");
		            for (String x : parts)
		            {
		                String [] nameValue = x.split("=");
		                if (nameValue.length > 1)
		                {
		                    String name = nameValue[0].trim();
		                    String value = nameValue[1].trim();
		                    if ("mime".equals(name))
		                        result.put(CrawlResult.MIME, value);
		                    else if ("charset".equals(name))
		                        result.put(CrawlResult.ENCODING, value);
		                }
		                else
		                    result.put(CrawlResult.MIME, x.trim());       
		            }		            
		        }
		        if (!result.containsKey(CrawlResult.ENCODING))
		            result.put(CrawlResult.ENCODING, conn.getContentEncoding());
		    }
		    catch (IOException ex)
		    {
		        DU.log.warn("Failed to open url '" + url.toExternalForm() + "'", ex);
		    }
		    return result;
		}

        private Set<String> detectLinks(String plainText)
        {
            Set<String> result = new HashSet<String>();
            // We only need to identify a potential URL here, so the regex is very simple!
            String regex ="http(s)?://\\S*";
            Pattern pat = Pattern.compile(regex);
            Matcher matcher = pat.matcher(plainText);
            while (matcher.find())
                result.add(plainText.substring(matcher.start(), matcher.end() + 1));
            return result;
        }
        
		private Set<String> collectLinks(CrawlResult cr)
		{
		    Set<String> result = new HashSet<String>();
		    if (MimeType.HTML.toString().equals(cr.getMime()))
		    {
		        HTMLDocument doc = new HTMLDocument(DU.toUrl(cr.getUrl()));
		        doc.getDesiredTags().clear();
		        doc.getDesiredTags().add(HTMLElementName.A);
		        doc.getDesiredAttributes().clear();
		        doc.getDesiredAttributes().add("href");
		        doc.load();
		        for (Ann ann : doc.getAnnotations())
		        {
		            if (! (ann instanceof MarkupAnn) ) continue;
		            MarkupAnn mann = (MarkupAnn)ann;
		            String href = mann.getAttributes().get("href");
		            if (!DU.isEmpty(href))
		                result.add(href);
		        }
		    }
		    else if (MimeType.PLAIN.toString().equals(cr.getMime()))
		    {		        
		        DefaultTextDocument doc = new DefaultTextDocument(DU.toUrl(cr.getUrl()));
		        String text = doc.getFullText();
		        result.addAll(detectLinks(text));
		    }
		    else if (MimeType.PDF.toString().equals(cr.getMime()))
		    {
                PDFDocument doc = new PDFDocument(DU.toUrl(cr.getUrl()));
                String text = doc.getFullText();
                result.addAll(detectLinks(text));		        
		    }
		    return result;
		}
		
		private void visit(CrawlResult cr)
		{		    
		    URL url = null;		     
		    try 
		    { 
		        url = new URL(cr.getUrl()); 
		    } 
		    catch (MalformedURLException ex) 
		    {                        
		        DU.log.error("Malformed URL '" + cr.getUrl() + 
		                     "'  while crawling '" + root.getUrl() + "'");
		        return; 
		    }		    
		    Map<String, Object> meta = collectMetaData(url);
		    if (meta == null)
		        return;
		    cr.getMetaData().putAll(meta);
		    if (cr.getMime() == null) // we ignore unknown MIME types.
		        return;
		    if (root.getMimesToReturn().contains(cr.getMime()))		    
		        callback.eval(cr);
		    if (cr.getDepth() < root.getDepth() && root.getMimesToFollow().contains(cr.getMime()))
		    {
    		    Set<String> links = collectLinks(cr);
    		    for (String l : links)
    		    {   
    		        URL link = null;    		        
    		        try { link = new URL(url, l); } 
    		        catch (Throwable e) { continue; }
    		        
    		        l = link.toExternalForm();
    		        if (!examined.contains(l) && shouldExamine(link))
    		        {
    		            examined.add(l);
    		            remaining.push(new CrawlResult(l, cr.getDepth()+1, root));
    		        }
    		    }
		    }
		}
		
		public CrawlingThread(CrawlRoot root)
		{
			this.root = root;
		}
		
		public void run()
		{
			String name = Thread.currentThread().getName();
			try
			{				
				Thread.currentThread().setName("Crawling " + root.getUrl());
				remaining.push(new CrawlResult(root.getUrl(), 0, root));
				while (!remaining.isEmpty() && running)
				{
				    CrawlResult next = remaining.pop();
				    try
				    {
				        visit(next);
				    }
				    catch (Throwable t)
				    {
				        DU.log.error("While visiting '" + next.getUrl() + "'" + 
				                     " within root '" + root.getUrl() + "'", t);
				    }
				}
			}
			finally
			{
				Thread.currentThread().setName(name);
				root.setLastCrawlTimestamp(System.currentTimeMillis());
				graph.update(root);
			}
		}
		
		public void stopRunning()		
		{
			running = false;
		}
		
		public boolean isRunning()
		{
			return running;
		}
	}
	
	private TimerTask makeTimerTask(final CrawlRoot root)
	{
	    return new CrawlingTask(root);
	}
	
	public class CrawlingTask extends TimerTask
	{
	    private CrawlRoot root;
	    private CrawlingThread crawlingThread;
	    
	    public CrawlingTask(CrawlRoot root)
	    {
	        this.root = root;
	        crawlingThread = new CrawlingThread(root);
	    }
	    
        public void run()
        {
            executorService.submit(crawlingThread);
        }	    
        
        public CrawlRoot getCrawlRoot()
        {
            return root;
        }
        
        public CrawlingThread getCrawlingThread()
        {
            return crawlingThread;
        }
	}
}
