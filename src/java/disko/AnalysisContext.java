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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.query.HGQueryCondition;
import org.hypergraphdb.query.Or;

import relex.corpus.TextInterval;

public class AnalysisContext<DocumentType>
{
	protected static Log log = LogFactory.getLog(AnalysisContext.class);
	
	private HyperGraph graph;
	private ThreadLocal<LinkedList<HGHandle>> scope = new ThreadLocal<LinkedList<HGHandle>>();
	private HGHandle initialScope = null;
	private DocumentType document;
	
	protected LinkedList<HGHandle> getScope()
	{
		LinkedList<HGHandle> S = scope.get();
		if (S == null)
		{
			S = new LinkedList<HGHandle>();
			if (initialScope != null)
				S.push(initialScope);
			scope.set(S);
		}
		return S;
	}
	
	public AnalysisContext(HyperGraph graph, DocumentType document)
	{
		this.graph = graph;
		this.document = document;
	}
	
	public HyperGraph getGraph()
	{
		return graph;
	}

	public void setGraph(HyperGraph graph)
	{
		this.graph = graph;
	}

	public DocumentType getDocument()
	{
		return document;
	}
		
	public HGHandle getInitialScope()
	{
		return initialScope;
	}

	public void setInitialScope(HGHandle initialScope)
	{
		this.initialScope = initialScope;
	}

	public HGHandle getTopScope()
	{
		if (getScope().isEmpty())
			return null;
		else
			return getScope().getLast();
	}
	
	public HGHandle getScoping()
	{
		return getScope().peek();
	}

	public void pushScoping(HGHandle scoping)
	{
		getScope().add(0, scoping);
	}
	
	public HGHandle popScope()
	{
		return getScope().remove(0);
	}
	
	public void addInScope(HGHandle h)
	{
		graph.add(new ScopeLink(getScope().peek(), h));
	}
	
	public HGHandle add(Object x)
	{
		HGHandle h = graph.add(x);
		addInScope(h);
		return h;
	}

	public HGHandle addTyped(Object x, HGHandle type)
	{
		HGHandle h = graph.add(x, type);
		graph.add(new ScopeLink(getScope().peek(), h));
		return h;
	}
	
	public HGHandle add(Object x, HGHandle ...scope)
	{
		HGHandle result = graph.add(x);
		for (HGHandle s:scope)
			graph.add(new ScopeLink(s, result));
		return result;
	}

	public HGHandle addTyped(Object x, HGHandle type, HGHandle ...scope)
	{
		HGHandle result = graph.add(x, type);
		for (HGHandle s:scope)
			graph.add(new ScopeLink(s, result));
		return result;
	}
	
	/**
	 * <p>
	 * Return all scopes of a given atom. 
	 * </p>
	 * 
	 * @param data
	 * @return
	 */
	public List<HGHandle> getScopes(HGHandle data)
	{
		return hg.findAll(graph, hg.apply(hg.linkProjection(0),
				hg.apply(hg.deref(graph), 
				hg.and(hg.type(ScopeLink.class), 
					   hg.incident(data), 
					   hg.orderedLink(hg.anyHandle(), data)))));
	}

	/**
	 * <p>
	 * Return a single scope of a given atom. This is a convenience method
	 * when it is known that an atom has only one scope.
	 * </p>
	 * 
	 * @param data
	 * @return
	 */
	public HGHandle getOneScope(HGHandle data)
	{
		List<HGHandle> scopes = getScopes(data);
		return scopes.size() > 0 ? scopes.get(0) : null;
	}
	
	/**
	 * <p>
	 * Find a set of objects within the current scope.
	 * </p>
	 * 
	 * @param <T>
	 * @param type
	 * @return
	 */
	public <T> Set<T> find(Class<T> type)
	{
		return find(hg.type(type), getScoping());
	}
	
	/**
	 * <p>
	 * Find a set of annotations within the current scope and within 
	 * the specified interval.  
	 * </p>
	 * 
	 * @param <T>
	 * @param type
	 * @param within
	 * @return
	 */
	public <T extends Ann> Set<T> find(Class<T> type, TextInterval within)
	{
		return find(hg.and(hg.type(type), 
					       hg.gte("interval.start", within.getStart()), 
						   hg.lte("interval.end", within.getEnd())),
				    getScoping());
	}
	
	/**
	 * <p>
	 * Find a set of objects satisfying a particular condition and within
	 * the specified scope.
	 * </p>
	 * 
	 * <p>
	 * For each atom satisfying the condition <code>cond</code>, its scope
	 * is recursively constructed and matched against the passed in scope
	 * (i.e. the set of scope handles passed as parameters must be a subset
	 * of the full scope of the atom).
	 * </p>
	 * 
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Set<T> find(HGQueryCondition cond, HGHandle...scope)
	{
		Set<T> result = new HashSet<T>();
		if (scope != null && scope.length > 0)
		{
			Or or = new Or();
			for (HGHandle s : scope)
				or.add(hg.and(cond, hg.bfs(s, hg.type(ScopeLink.class), null, false, true)));
			cond = or;
		}
		List<HGHandle> tmp = hg.findAll(graph, cond);
		for (HGHandle h : tmp)			
			result.add((T)graph.get(h));
		return result;
	}
}
