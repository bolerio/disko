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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGIndex;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HGSearchResult;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.wordnet.data.Word;
import org.hypergraphdb.indexing.TargetToTargetIndexer;
import org.hypergraphdb.query.impl.ZigZagIntersectionResult;
import org.hypergraphdb.storage.DBKeyedSortedSet;
import org.hypergraphdb.util.ArrayBasedSet;
import org.hypergraphdb.util.HGSortedSet;
import org.hypergraphdb.util.HGUtils;

import disko.ScopeLink;
import disko.data.BasicWords;

@SuppressWarnings("unchecked")
public class DiskoSearch
{
	private HGIndex<HGPersistentHandle, HGPersistentHandle> scopeIndex = null;
	private HyperGraph graph = null;	
	private int inMemoryThreshold = 10000;
	private double sentenceBoostFactor = 2.0;
	private HGHandle scopeLinkType;
	private HGHandle [] relations;
	private double [] scores;
	private ArrayList<SearchSet> population;
	private int totalScopesFound;
	private HashSet<Signature> examined = new HashSet<Signature>();

	private double score(Signature s)
	{
		double result = 0.0;
		for (int idx : s.A)
			result += sentenceBoostFactor*scores[idx];
		return result;
	}

	private boolean ignoreRelation(HGHandle h)
	{
		boolean ignore = true;
		HGLink rel = graph.get(h);
		for (int i = 0; i < rel.getArity() && ignore; i++)
		{
			Object arg = graph.get(rel.getTargetAt(i));
			if (arg instanceof Word && !BasicWords.isBasicWord(((Word)arg).getLemma()))
					ignore = false;
		}
		return ignore;
	}
	
	private SearchSet getInitialSet(int idx)
	{
		HGHandle sr = relations[idx];
		HGPersistentHandle pHandle = graph.getPersistentHandle(sr);
		int srCount = (int)scopeIndex.count(graph.getPersistentHandle(sr));
		if (srCount <= inMemoryThreshold)
		{
			// we're not using transactions here deliberately because searching
			// should not be in a transactional environment for faster operations
			HGSearchResult<HGPersistentHandle> rs = scopeIndex.find(pHandle);
			HGPersistentHandle [] A = new HGPersistentHandle[srCount];
			try
			{
				for (int i = 0; rs.hasNext(); i++)
					A[i] = rs.next();
			}
			finally
			{
				HGUtils.closeNoException(rs);
			}				 
			return new SearchSet(idx, new ArrayBasedSet<HGHandle>(A), 0.0);
		}
		else			
			return new SearchSet(idx, new DBKeyedSortedSet(scopeIndex, pHandle), 0.0);
	}

	HGSortedSet<HGHandle> intersect(HGSortedSet<HGHandle> x, HGSortedSet<HGHandle> y)
	{
		ArrayBasedSet<HGHandle> S = new ArrayBasedSet<HGHandle>(new HGHandle[0]);
		ZigZagIntersectionResult<HGHandle> zigzag = new ZigZagIntersectionResult<HGHandle>(x.getSearchResult(), y.getSearchResult());
		while (zigzag.hasNext())
			S.add(zigzag.next());
		return S;
	}
	
	boolean canMate(SearchSet x, SearchSet y)
	{
		if (x == null)
			System.out.println("x is null");
		else if (y == null)
			System.out.println("y is null");
		else if (x.signature == null)
			System.out.println("x.signature is null");
		else if (y.signature == null)
			System.out.println("y.signature is null");
		return !x.signature.prefixOrSuperFix(y.signature) && 
			   !examined.contains(new Signature(x.signature, y.signature)); 
	}
	
	Map<SearchSet, SearchSet> getMates(List<SearchSet> population)
	{
		Map<SearchSet, SearchSet> result = new IdentityHashMap<SearchSet, SearchSet>();
		if (population.size() < 2)
			return result;
		for (int i = 0; i < population.size(); i++)
		{
			SearchSet left = population.get(i);
			SearchSet right = null;		
			int j = i + 1;
			if (j == population.size())
				j = 0;
			while(right == null && j != i)
			{
				right = population.get(j);
				if (right == null)
					System.out.println("null at index " + j);
				if (!canMate(left, right))
					right = null;
				// if we didn't find anything for elements after i, then rollover to the beginning
				j++;
				if (j == population.size())
					j = 0;
			}
			 // maybe do something if right==null, cause it means left's not gonna mate with anything in the future
			if (right != null)
				result.put(left, right);
		}
		return result;
	}
	
	List<SearchSet> mate(Map<SearchSet, SearchSet> couples)
	{
		ArrayList<SearchSet> children = new ArrayList<SearchSet>(couples.size());
		for (Map.Entry<SearchSet, SearchSet> couple : couples.entrySet())
		{
			HGSortedSet<HGHandle> childSet = intersect(couple.getKey().set, couple.getValue().set);
			SearchSet child = new SearchSet();
			child.set = childSet;			
			child.signature = new Signature(couple.getKey().signature, couple.getValue().signature);
			child.score = score(child.signature);
			children.add(child);
		}
		return children;
	}	
	
	void killUnfit(Collection<SearchSet> population)
	{
		
	}
	
	public DiskoSearch(HyperGraph graph, List<HGHandle> allRelations)
	{
		this.graph = graph;		
		this.scopeLinkType = graph.getTypeSystem().getTypeHandle(ScopeLink.class);
		this.scopeIndex = graph.getIndexManager().getIndex(new TargetToTargetIndexer(scopeLinkType, 1, 0));
		
		List<HGHandle> rels = new ArrayList<HGHandle>();
		for (HGHandle h : allRelations)
			if (!ignoreRelation(h))
				rels.add(h);
		
		this.relations = new HGHandle[rels.size()];
		this.scores  = new double[rels.size()];
		SearchSet [] SSA = new SearchSet[rels.size()];		
		totalScopesFound = 0;
		int i = 0;
		for (HGHandle h : rels)
		{
			if (ignoreRelation(h))
				continue;
			this.relations[i] = h;
			SSA[i] = getInitialSet(i);
			totalScopesFound += SSA[i].count();
			i++;
		}
		for (i = 0; i < SSA.length; i++)
			scores[i] = SSA[i].score = 
				((HGLink)graph.get(relations[i])).getArity()*(1.0 - (double)SSA[i].count()/totalScopesFound);
		this.population = new ArrayList<SearchSet>(Arrays.asList(SSA));
	}
	
	public SortedSet<Result> search(HyperGraph graph,  int maxResults)
	{	
		int generations = 1;
		while (generations < 10) //(!shouldStop(population))
		{
			killUnfit(population);
			Map<SearchSet, SearchSet> couples = getMates(population);
			List<SearchSet> nextGeneration = mate(couples);
			for (SearchSet S : nextGeneration)
			{
				population.add(S);
				examined.add(S.signature);
			}			
			generations++;
		}
		
		// Collect results
		
		// First collect sentences
		Map<HGHandle, Signature> sentences = new HashMap<HGHandle, Signature>();
		for (SearchSet set : population)
		{
			HGSearchResult<HGHandle> rs = set.set.getSearchResult();
			try
			{
				while (rs.hasNext())
				{
					HGHandle sentenceHandle = rs.next();
					Signature current = sentences.get(sentenceHandle);
					if (current == null)
						sentences.put(sentenceHandle, set.signature);
					else
						sentences.put(sentenceHandle, new Signature(current, set.signature));
				}
			}
			catch (Throwable t)
			{
				t.printStackTrace(System.err);
			}
			finally
			{
				rs.close();
			}
		}
		
		// Get their documents
		Map<HGHandle, Result> results = new HashMap<HGHandle, Result>();
		for (Map.Entry<HGHandle, Signature> e : sentences.entrySet())
		{
			HGHandle docHandle = scopeIndex.findFirst(graph.getPersistentHandle(e.getKey()));
			Result R = results.get(docHandle);
			if (R == null)
			{
				R = new Result();
				R.resource = docHandle;
				R.score = score(e.getValue());
				results.put(docHandle, R);
			}
			else
				R.score += score(e.getValue());
			for (int idx : e.getValue().A)
				R.relations.add(this.relations[idx]);				
			R.subScopes.add(e.getKey());
		}
		
		// Sort results be score
		Result [] RSET = new Result[results.size()];
		int idx = 0;
		for (Result r : results.values())
			RSET[idx++] = r;
		Arrays.sort(RSET);
		ArrayBasedSet<Result> result = new ArrayBasedSet<Result>(RSET);
		return result;
	}
	
	static class Signature
	{
		int [] A;
		int hash = -1;
		
		Signature(int i) 
		{ 
			A = new int[1]; 
			A[0] = i; 
		}
		
		Signature(Signature x, Signature y)
		{
			// Merge the 2 signatures
			int [] B = new int[x.A.length + y.A.length]; // max possible length, we'll reduce below
			int xi = 0, yi = 0, i = 0;
			while (xi < x.A.length && yi < y.A.length)
			{
				if (x.A[xi] < y.A[yi]) B[i++] = x.A[xi++];
				else if (x.A[xi] > y.A[yi]) B[i++] = y.A[yi++];
				else { B[i++] = y.A[yi++]; xi++; }
			}
			while (xi < x.A.length) B[i++] = x.A[xi++];
			while (yi < y.A.length) B[i++] = y.A[yi++];
			A = new int[i];
			System.arraycopy(B, 0, A, 0, i);
		}
		
		boolean prefixOrSuperFix(Signature s)
		{
			int max = Math.min(A.length, s.A.length);
			for (int i = 0; i < max; i++)
				if (A[i] != s.A[i])
					return false;
			return true;
		}
		
		public int hashCode()
		{
			if (hash == -1)
				hash = Arrays.hashCode(A);
			return hash;
		}
		
		public boolean equals(Object x)
		{
			return Arrays.equals(A, ((Signature)x).A);
		}
	}
	
	/**
	 * <p>Note: this class has a natural ordering that is inconsistent with
	 * <code>equals</code>.
	 * </p.
	 * 
	 * @author boris
	 *
	 */
	static class SearchSet implements Comparable<SearchSet>, Iterable<HGHandle>
	{
		double score;
		Signature signature; // all "ancestors", i.e. sets whose intersections this holds
		HGSortedSet<HGHandle> set;
		
		SearchSet()
		{			
		}
		
		SearchSet(int i, HGSortedSet<HGHandle> set, double score)
		{
			this.set = set;
			this.score = score;
			signature = new Signature(i);
		}		
		
		public int count()
		{
			return set.size();
		}
		
		public int generation() 
		{ 
			return signature.A.length / 2 + 1; 
		}
		
		public double fitness()
		{
			return score; // *set.size();
		}
		
		public int compareTo(SearchSet other)
		{
			return -Double.compare(fitness(), other.fitness());
		}
		
		public Iterator<HGHandle> iterator()
		{
			return set.iterator();
		}
	}
	
	public static class Result implements Comparable<Result>
	{
		HGHandle resource;
		double score;
		Set<HGHandle> subScopes = new HashSet<HGHandle>();
		Set<HGHandle> relations = new HashSet<HGHandle>();
		
		public int compareTo(Result other)
		{
			return -Double.compare(score, other.score);
		}		
	}
}
