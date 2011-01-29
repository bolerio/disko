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
package disko;

import java.beans.BeanInfo;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.IncidenceSet;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;
import org.hypergraphdb.query.HGAtomPredicate;
import org.hypergraphdb.util.HGUtils;
import org.hypergraphdb.util.Mapping;
import org.hypergraphdb.util.Pair;

import disko.data.relex.RelOccurrence;
import disko.data.relex.SynRel;

/**
 * 
 * <p>
 * DU stands for "Disco Utilities". A bunch of useful static methods.  
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class DU
{
    /**
     * A global Disko log.
     */
    public static final Log log = LogFactory.getLog("org.disko");
    
    private static Timer globalTimer = null;
    
    public static synchronized Timer getTimer()
    {
    	if (globalTimer == null)
    		globalTimer = new Timer(true);
    	return globalTimer;
    }
    
	/**
	 * 
	 * <p>
	 * Make a URL from its string representation, but convert the checked exception
	 * thrown by URL constructor into an unchecked one.
	 * </p>
	 *
	 * @param url
	 * @return
	 */
	public static URL toUrl(String url)
	{
		try { return new URL(url); }
		catch (MalformedURLException ex) { throw new RuntimeException(ex); }
	}
	
	public static boolean isSubset(Set<Object> left, Set<Object> right)
	{
		for (Object x : left)
			if (!right.contains(x))
				return false;
		return true;
	}
	
	public static boolean isEmpty(String s) { return s == null || s.length() == 0; }	
	
	public static String readCharacterStream(InputStream in) throws IOException
	{
		StringBuffer result = new StringBuffer();
		/* Reader reader = new InputStreamReader(in);
		char [] buffer = new char[1024];
		int c = reader.read(buffer); 
		while (c > -1)
		{
			result.append((char[])buffer);
			c = reader.read(buffer);
		} */
		for (int c = in.read(); c > -1; c = in.read())
			result.append((char)c);
		return result.toString();
	}
	
	public static String readResource(String resource) throws IOException
	{
		InputStream in = DU.class.getResourceAsStream(resource);
		try
		{
			return readCharacterStream(in);
		}
		finally
		{
			try { in.close(); }
			catch (Throwable t) { }
		}
	}
	
	public static String readFile(String filename) throws IOException
	{
		InputStream in = new FileInputStream(filename);
		try
		{
			return readCharacterStream(in);
		}
		finally
		{
			try { in.close(); }
			catch (Throwable t) { }
		}		
	}
	
//	public static String readUrl(URL url) throws Exception
//	{
//		StringBuffer buffer = new StringBuffer();
//		InputStream in = null;
//		try
//		{			
//			URLConnection connection = DiscoProxySettings.openConnection(url);
//			connection.setConnectTimeout(60000);
//			connection.setReadTimeout(60000);
//			in = DiscoProxySettings.openConnection(url).getInputStream();
//			InputStreamReader reader = new InputStreamReader(in);
//			int len = 4096;
//			char [] A = new char[len];			
//			for (int read = reader.read(A, 0, len); read > -1; read = reader.read(A, 0, len))
//				buffer.append(A, 0, read);
//			return buffer.toString();
//		}
//		finally
//		{
//			if (in != null) try { in.close(); } catch (Throwable t) { }
//		}		
//	}
	
	/**
	 * Insert a set of strings at specified positions in the given text. A TreeMap is
	 * used to ensure that the insert position are in increasing order.
	 */
	public static String insertStuff(String text, TreeMap<Integer, String> stuff)
	{
        int last = 0;
        StringBuffer result = new StringBuffer();
        for (Map.Entry<Integer, String> e : stuff.entrySet())
        {
            int pos = e.getKey();
            System.out.println("Inserting text b/w " + last + " and " + pos);
            result.append(text.substring(last, pos));
            result.append(e.getValue());
            last = pos;
        }
        result.append(text.substring(last));
        return result.toString(); 		
	}
	
	public static void loadSystemProperties(String filename)
	{
		loadSystemProperties(new File(filename));
	}

	public static void loadSystemProperties()
	{
		loadSystemProperties(new File("discorun.properties"));
	}
	
	public static void loadSystemProperties(File discoPropertiesFile)
	{
		if (discoPropertiesFile.exists())
		{
			FileInputStream in = null;
			try
			{
				in = new FileInputStream(discoPropertiesFile);
				Properties props = new Properties();
				props.load(in);
				System.getProperties().putAll(props);
				System.out.println("Disco Properties [" + discoPropertiesFile.getAbsolutePath() + "]:");
				System.out.println("==============================================");
				for (Object name : props.keySet())
					System.out.println(name + "=" + props.getProperty((String)name));
				System.out.println("==============================================");	
			}
			catch (Exception ex) { throw new RuntimeException(ex); }
			finally 
			{
				try { if (in != null) in.close(); } catch (Throwable t) { }
			}
		}
	}
	
    /**
     * <p>
     * Print the full stack trace of a <code>Throwable</code> object into a
     * string buffer and return the corresponding string.  
     * </p>
     */
    public static String printStackTrace(Throwable t)
    {
    	if (t == null)
    		return null;
    	java.io.StringWriter strWriter = new java.io.StringWriter();
    	java.io.PrintWriter prWriter = new PrintWriter(strWriter);
    	t.printStackTrace(prWriter);
    	prWriter.flush();
    	return strWriter.toString();
    }
    
	public static String request(String targetUrl, 
								 String method, 
							   	 String text, 
							   	 String contentType) throws Exception
	{
		URL url = new URL(targetUrl);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();

		if (method != null)
			conn.setRequestMethod(method);

		if (contentType != null)
			conn.setRequestProperty("Content-Type", contentType);

		if (text != null)
		{
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.getOutputStream().write(text.getBytes());
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.write(text.getBytes());
			out.flush();
			out.close();
		}

		if (conn.getResponseCode() != 200)
			throw new RuntimeException("HTTPClient failed: " + conn.getResponseCode());

		StringBuffer responseBuffer = new StringBuffer();			
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;    
		String eol= new String(new byte[] {13});
		while ((line = in.readLine()) != null)
		{
			responseBuffer.append(line);     
			responseBuffer.append(eol);
		}
		in.close();

		return responseBuffer.toString();
	}   
	
	public static Map<Object, Object> map(Object...args)
	{
		if (args == null)
			return null;
		Map<Object, Object> m = new HashMap<Object, Object>();		
		if (args.length % 2 != 0)
			throw new IllegalArgumentException("The arguments array to struct must be of even size: a flattened list of name/value pairs");
		for (int i = 0; i < args.length; i+=2)
			m.put(args[i], args[i+1]);
		return m;		
	}
	
	public static Map<String, Object> stringMap(Object...args)
	{
		if (args == null)
			return null;
		Map<String, Object> m = new HashMap<String, Object>();		
		if (args.length % 2 != 0)
			throw new IllegalArgumentException("The arguments array to struct must be of even size: a flattened list of name/value pairs");
		for (int i = 0; i < args.length; i+=2)
			m.put((String)args[i], args[i+1]);
		return m;		
	}
	
	/**
	 * 
	 * <p>
	 * Construct a string out of the first N tokens of a string. Used as a simple
	 * "document summary" 
	 * </p>
	 *
	 * @param s
	 * @return
	 */
	public static String getFirstNWords(String s, int N)
	{
		String [] tokens = s.split("\\s+");
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < N && i < tokens.length; i++)
		{
			result.append(tokens[i]);
			result.append(' ');
		}
		return result.toString();
	}
	
	public static UrlTextDocument makeDocument(URL url)
	{
		UrlTextDocument result;
		if (url.toString().endsWith(".pdf"))
		{
			result = new PDFDocument(url);
			result.setSummary(getFirstNWords(result.getFullText(), 60));
		}
		else
		{
			result = new HTMLDocument(url);
			result.setSummary(getFirstNWords(((HTMLDocument)result).getPlainText(), 60));
		}
		return result;
	}
	
	/**
	 * 
	 * <p>
	 * Remove a scope tree rooted at <code>scope</code> from the hypergraph.
	 * </p>
	 *
	 * @param graph The graph from which the tree must be removed.
	 * @param scope The root of the tree.
	 * @param removeScopePredicate If the <code>scope</code> atom satisfies this predicate, the
	 * atom itself will be deleted from the graph as well.  
	 * @param stopRecursionPredicate If not null, stop at the node of the scope tree that satisfies
	 * that predicate. This predicate lets you "prune" the deletion process for
	 * certain nodes. 
	 */
	public static void deleteScope(HyperGraph graph, 
								   HGHandle scope, 
								   HGAtomPredicate removeScopePredicate,
								   HGAtomPredicate stopRecursionPredicate)
	{ 		
	    List<HGHandle> scopeLinks = hg.findAll(graph, hg.and(hg.type(ScopeLink.class), 
	    										   hg.incident(scope), 
	    										   hg.orderedLink(scope, graph.getHandleFactory().anyHandle())));
	    for (HGHandle lh : scopeLinks)
	    {
	        ScopeLink link = graph.get(lh);
	        if (link == null)
	        {
	            System.err.println("Opps, missing atom for " + lh);
	            continue;
	        }
	        HGHandle scoped = link.getTargetAt(1);
	        graph.remove(lh);
	        if ((graph.isLoaded(scoped) ||  graph.getStore().getLink(graph.getPersistentHandle(scoped)) != null) && 
	            (stopRecursionPredicate == null || !stopRecursionPredicate.satisfies(graph, scoped)))
	        	deleteScope(graph, scoped, removeScopePredicate, stopRecursionPredicate);
	    }
    	if (removeScopePredicate != null && removeScopePredicate.satisfies(graph, scope))
    		graph.remove(scope);
	}
	
	public static void printScope(PrintStream out,
								  HyperGraph graph, 
								  HGHandle scope, 
								  HGAtomPredicate stopRecursionPredicate, 
								  String indent)
	{
	    Object scopeAtom = graph.get(scope);
	    if (scopeAtom instanceof SynRel)
	        out.println(indent + ((SynRel)scopeAtom).toString(graph));
	    else
	        out.println(indent + scopeAtom);
        List<HGHandle> scopeLinks = hg.findAll(graph, hg.and(hg.type(ScopeLink.class), 
                                                   hg.incident(scope), 
                                                   hg.orderedLink(scope, graph.getHandleFactory().anyHandle())));
        for (HGHandle lh : scopeLinks)
        {
            ScopeLink link = graph.get(lh);
            if (link == null)
            {
                System.err.println("Opps, missing atom for " + lh);
                continue;
            }
            HGHandle scoped = link.getTargetAt(1);
            if (stopRecursionPredicate == null || !stopRecursionPredicate.satisfies(graph, scoped))
                printScope(out, graph, scoped, stopRecursionPredicate, indent + "\t");
        }
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> getInScope(HyperGraph graph, HGHandle scope)
	{
//		System.out.println("Get in scope " + scope);
		IncidenceSet inc = graph.getIncidenceSet(scope);
		List<T> L = new ArrayList<T>();
		for (HGHandle h : inc)
		{
			Object x = graph.get(h);
			if (x instanceof ScopeLink && ((ScopeLink)x).getTargetAt(0).equals(scope))
				L.add((T)graph.get(((ScopeLink)x).getTargetAt(1)));
		}
		//System.out.println("Got it " + inc.size());
//	    List<ScopeLink> scopeLinks = hg.getAll(graph, 
//	                                           hg.and( hg.type(ScopeLink.class), 
//	                                                   hg.incident(scope), 
//	                                                   hg.orderedLink(scope, HGHandleFactory.anyHandle())));
//	    //List<T> L = new ArrayList<T>();
//	    for (ScopeLink link : scopeLinks)
//	    {
//	        L.add((T)graph.get(link.getTargetAt(1)));
//	    }
	    return L;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> getScopes(HyperGraph graph, HGHandle scoped)
	{
	    List<ScopeLink> scopeLinks = hg.getAll(graph, 
                hg.and(hg.type(ScopeLink.class),  
                       hg.orderedLink(graph.getHandleFactory().anyHandle(), scoped)));
	    List<T> L = new ArrayList<T>();
	    for (ScopeLink link : scopeLinks)
	    {
	    	L.add((T)graph.get(link.getTargetAt(0)));
	    }
	    return L;
	}			
	
	public static <DocumentType> void  
		analyzeDocument(AnalysisContext<DocumentType> ctx, 
					    DocumentType doc,
					    HGHandle scope,
					    DataFlowNetwork<AnalysisContext<DocumentType>> network)
	{
		ctx.pushScoping(scope);
		network.setContext(ctx);
		Future<?> future;
		future = network.start();
		try 
		{
			future.get();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
    public static HGHandle toSynRel(HyperGraph graph, HGHandle h)
	{
	    RelOccurrence o = graph.get(h);
	    HGHandle [] targets = HGUtils.toHandleArray(o);
	    HashSet<HGHandle> L = new HashSet<HGHandle>();
	    L.addAll((List<HGHandle>)(List<?>)hg.findAll(graph, hg.and(hg.type(ScopeLink.class), hg.orderedLink(new HGHandle[]{hg.anyHandle(), h}))));
	    if(L.isEmpty()) // scope unknown?!!?
	    	return null;
	    HGHandle synRelHandle = hg.findOne(graph, hg.and(hg.type(SynRel.class), hg.link(targets)));
	    if (synRelHandle == null)
	        synRelHandle = graph.add(new SynRel(targets));
	    for (HGHandle scopeHandle : L)
	    {
	        ScopeLink scopeLink = graph.get(scopeHandle);
	        HGHandle scoping = scopeLink.getTargetAt(0);
	        if (hg.findOne(graph, hg.and(hg.type(ScopeLink.class), hg.orderedLink(scoping, synRelHandle))) == null)
	        	graph.add(new ScopeLink(scoping, synRelHandle));
	    }
	    return synRelHandle;
	}	
	
    @SuppressWarnings("unchecked")
    public static <T> T cloneObject(T p, 
                                    Mapping<Pair<Object, String>, Boolean> propertyFilter) throws Exception
    {
        if (p == null)
            return null;
        
        if (p instanceof Cloneable)
        {
            Method cloneMethod = p.getClass().getMethod("clone", (Class[])null);
            if (cloneMethod != null)
                return (T)cloneMethod.invoke(p, (Object[])null);

        }
        else if (p.getClass().isArray())
        {
            Object [] A = (Object[])p;
            Class<?> type = p.getClass(); 
            Object [] ac = (Object[])Array.newInstance(type.getComponentType(), A.length);
            for (int i = 0; i < A.length; i++)
                ac[i] = cloneObject(A[i], propertyFilter);
            return (T)ac;
        }
        else if (identityCloneClasses.contains(p.getClass()))
            return p;
        
        //
        // Need to implement cloning ourselves. We do this by copying bean properties.
        //
        Constructor<?> cons = null;
        
        try
        {
            cons = p.getClass().getConstructor((Class[])null);
        }
        catch (Throwable t)
        {
            return p;
        }
        
        Object copy = cons.newInstance((Object[])null);
        
        if (p instanceof Collection)
        {
            Collection<Object> cc = (Collection<Object>)copy;
            for (Object el : (Collection<?>)p)
                cc.add(cloneObject(el, propertyFilter));            
        }
        else if (p instanceof Map)
        {
            Map<Object, Object> cm = (Map<Object, Object>)copy;
            for (Object key : ((Map<Object, Object>)p).keySet())
                cm.put(key, cloneObject(((Map<Object, Object>)p).get(key), propertyFilter));
        }
        else
        {
            BeanInfo bean_info = Introspector.getBeanInfo(p.getClass());        
            PropertyDescriptor beanprops [] = bean_info.getPropertyDescriptors();
            if (beanprops == null || beanprops.length == 0)
                copy = p;
            else for (PropertyDescriptor desc : beanprops)
            {
                Method rm = desc.getReadMethod();
                Method wm = desc.getWriteMethod();
                if (rm == null || wm == null)
                    continue;
                Object value = rm.invoke(p, (Object[])null);
                if (propertyFilter == null || propertyFilter.eval(new Pair<Object, String>(p, desc.getName())))
                    value = cloneObject(value, propertyFilter); 
                wm.invoke(copy, new Object[] { value });
            }
        }
        return (T)copy;
    }
    
    static final Set<Class<?>> identityCloneClasses = new HashSet<Class<?>>();
    static
    {
        identityCloneClasses.add(String.class);
        identityCloneClasses.add(Byte.class);
        identityCloneClasses.add(Short.class);
        identityCloneClasses.add(Integer.class);
        identityCloneClasses.add(Long.class);
        identityCloneClasses.add(Float.class);
        identityCloneClasses.add(Double.class);
        identityCloneClasses.add(Boolean.class);
        identityCloneClasses.add(Character.class);        
    }	
    
    public static String replaceUnicodePunctuation(String s)
    {
        return s.replace((char) 147, '"')
                .replace((char) 148, '"')
                .replace((char) 146, '\'')
                .replace((char) 145, '\'')
                .replace((char) 153, ' ')
                .replace((char)8217, '\'')
                .replace((char)8211, '-')
                .replace((char)8212, '-')
                .replace((char)8216, '\'')
                .replace((char)8217, '\'')
                .replace((char)8220, '"')
                .replace((char)8221, '"')
                .replace((char)8242, '\'')
                .replace((char)8243, '"')
                .replace((char) '\u2011', '-');        
    }
}
