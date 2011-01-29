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
package disko.flow.dist;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.ContextJobAdapter;
import org.hypergraphdb.app.dataflow.Job;
import org.hypergraphdb.indexing.ByPartIndexer;
import org.hypergraphdb.query.impl.IndexBasedQuery;

import disko.AnalysisContext;
import disko.DU;
import disko.HTMLDocument;
import disko.StringTextDocument;
import disko.TextDocument;
import disko.UrlTextDocument;

public class DocJobAdapter implements ContextJobAdapter<AnalysisContext<TextDocument>>
{	
	private TextDocument makeDoc(DocJob task)
	{
    	if (task instanceof DocAtUrlJob)
    	{
            URL url = null;
            try
            {
                url = new URL(((DocAtUrlJob)task).getUrl());
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
            return DU.makeDocument(url);
    	}
    	else if (task instanceof DocHTMLJob)
    	{
    		return new HTMLDocument(((DocHTMLJob)task).getHtml());
    	}
    	else if (task instanceof DocTextJob)
    	{
    		return new StringTextDocument(((DocTextJob)task).getText());
    	}
    	else
    		throw new RuntimeException("Unkown DocJob type: " + task.getClass());
	}
	
	@SuppressWarnings("unchecked")
	private HGHandle getDocHandle(HyperGraph graph, HGHandle predefined, DocJob task)
	{
    	List<HGHandle> L = null;
    	if (task instanceof DocAtUrlJob)
    	{
            try
            {
            	HGHandle typeHandle = graph.getTypeSystem().getTypeHandle(UrlTextDocument.class);
            	ByPartIndexer indexer = new ByPartIndexer(typeHandle, "urlString");
            	String url = new URL(((DocAtUrlJob)task).getUrl()).toExternalForm();
            	IndexBasedQuery query = new IndexBasedQuery(
            			graph.getIndexManager().getIndex(indexer), url);
            	query.setHyperGraph(graph);
            	L = (List<HGHandle>)(Object)hg.findAll(query);            	
// TODO - the following would be the correct way query for a doc with a given url string,
// but querying indexed sub-types is not implemented yet, so until then we lookup in the
// index directly.
            	
//                L = hg.findAll(graph, 
//	                hg.and(hg.typePlus(UrlTextDocument.class), 
//	                       hg.eq("urlString", 
//	                    		 new URL(((DocAtUrlJob)task).getUrl()).toExternalForm())));
                
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
    	}
    	else if (task instanceof DocHTMLJob)
    	{
    		L = new ArrayList<HGHandle>();
    	}
    	else if (task instanceof DocTextJob)
    	{
    		L = new ArrayList<HGHandle>();
    	}
    	else
    		throw new RuntimeException("Unkown DocJob type: " + task.getClass());
    	
        TextDocument doc;
        //  We create a new doc atom when none exists already or it exists but not under
        // a the 'task.getScope()' handle.
        if (L.isEmpty())
        {            
        	doc = makeDoc(task);
            if (predefined != null)
            {
                graph.define(graph.getPersistentHandle(predefined), doc);
                return predefined;
            }
            else
                return graph.add(doc);
        }
        else if (predefined != null)
        {
            if (!L.contains(task.getScope()))
            {            	
                graph.define(graph.getPersistentHandle(predefined), makeDoc(task));
            }
            return predefined;
        }
        else
            return L.get(0);
		
	}
	
    public synchronized AnalysisContext<TextDocument> adapt(
        AnalysisContext<TextDocument> context, Job taskParam)
    {
        if (taskParam == null)
            throw new NullPointerException("Job is null.");
        else if (! (taskParam instanceof DocJob))
            throw new RuntimeException(this.getClass().getName() + 
                " expects a DocJob instance rather than a '" + taskParam.getClass().getName() + "'.");
        HyperGraph graph = context.getGraph();
        DocJob task = (DocJob)taskParam;        
        HGHandle scopeHandle;
        HGHandle docHandle;
        
        if (task.getScopeHandle() != null || task.getScope() != null)
        {
        	scopeHandle = task.getScopeHandle();
        	if (scopeHandle == null)
        		scopeHandle = graph.add(task.getScope());
        	else if (graph.get(scopeHandle) == null)
        	{ 
        		if (task.getScope() != null)        		
        			graph.define(graph.getPersistentHandle(scopeHandle), task.getScope());
        		else
        			getDocHandle(graph, scopeHandle, task);
        	}
        } 
        else
        	scopeHandle = getDocHandle(graph, null, task);

        if (graph.get(scopeHandle) instanceof TextDocument)
        	docHandle = scopeHandle;
        else
        	docHandle = getDocHandle(context.getGraph(), null, task);
        
        TextDocument doc = context.getGraph().get(docHandle);       
        
        AnalysisContext<TextDocument> result = 
            new AnalysisContext<TextDocument>(context.getGraph(),doc);
        result.pushScoping(scopeHandle);
        return result;
    }
}
