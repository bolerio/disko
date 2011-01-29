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
package disko.flow.analyzers.hgdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGIndex;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HGSearchResult;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.indexing.ByPartIndexer;
import org.hypergraphdb.indexing.CompositeIndexer;
import org.hypergraphdb.indexing.HGIndexer;
import org.hypergraphdb.indexing.HGKeyIndexer;

import disko.data.relex.RelationCount;
import disko.data.relex.RelexParse;

import relex.ParsedSentence;
import relex.entity.EntityInfo;
import relex.entity.EntityMaintainer;
import relex.feature.FeatureForeach;
import relex.feature.FeatureNode;
import relex.feature.RelationCallback;

public class RelationCountFactory 
{
	private static final String PERSON = "PERSON";
	public static final String ANY = "*";
	
	// TEMP stuff - we need to optimize the find query, for now we just cache relations...
	private static Map<String, RelationCount> rcCache = Collections.synchronizedMap(new HashMap<String, RelationCount>());
	private static HGIndex<Object, HGPersistentHandle> rcIndex = null;
	private static HGKeyIndexer rcIndexer = null;
	
	private static HGKeyIndexer makeRCIndexer(HyperGraph graph)
	{
		HGHandle typeH = graph.getTypeSystem().getTypeHandle(RelationCount.class);
		ByPartIndexer pind = new ByPartIndexer(typeH, "predicate");
		ByPartIndexer parg0 = new ByPartIndexer(typeH, "arg0");
		ByPartIndexer parg1 = new ByPartIndexer(typeH, "arg1");
		ByPartIndexer ppos0 = new ByPartIndexer(typeH, "pos0");
		ByPartIndexer ppos1 = new ByPartIndexer(typeH, "pos1");
		return new CompositeIndexer(typeH, new HGKeyIndexer[]
		{
		    pind, parg0, parg1, ppos0, ppos1
		});			
	}
	
	private static synchronized HGIndex<Object, HGPersistentHandle> getRCIndex(HyperGraph graph)
	{
		if (rcIndex == null)
		{
			rcIndexer = makeRCIndexer(graph);
			rcIndex = graph.getIndexManager().register(rcIndexer);
			graph.runMaintenance();
		}
		return rcIndex;
	}
	
	public static HashMap<String, String> PRONOUNS = new HashMap<String, String>();
	static 
	{
		PRONOUNS.put("he", PERSON);
		PRONOUNS.put("she", PERSON);
		PRONOUNS.put("you", PERSON);
		PRONOUNS.put("him", PERSON);
		PRONOUNS.put("her", PERSON);
		PRONOUNS.put("we", PERSON);
		PRONOUNS.put("us", PERSON);
	}
	
	public static void loadCounts(HyperGraph graph)
	{
		System.out.println("Loading all relation counts.");
		HGSearchResult<HGHandle> rs = graph.find(hg.type(RelationCount.class));
		int cnt = 0;
		try
		{
			while (rs.hasNext())
			{
				RelationCount r = graph.get(rs.next());
				String key = r.getPredicate() + "|" + r.getArg0() + "|" + r.getArg1() + 
			     "|" + r.getPos0() + "|" + r.getPos1(); 
				rcCache.put(key, r);
				if (++cnt % 1000 == 0)
					System.out.println("Loading " + cnt + " RelationCount instances.");
			}
		}
		finally
		{
			rs.close();
		}
	}
	
	/**
	 * Find a RelationCount in the HyperGraph
	 * 
	 * @param graph The graph 
	 * @param r The RelationCount
	 * @return The RelationCount handle, or null if not found. 
	 */
	public static HGHandle find(HyperGraph graph, RelationCount r) 
	{		
		Object key = rcIndexer.getKey(graph, r); 
		HGHandle found = rcIndex.findFirst(key);
		
/*		String key = r.getPredicate() + "|" + r.getArg0() + "|" + r.getArg1() + 
	     "|" + r.getPos0() + "|" + r.getPos1(); 
		RelationCount rc = rcCache.get(key);
		if (rc != null)
			found = graph.getHandle(rc);
		if (found != null) return found; */ 
		
/*		System.out.println("Looking for relation " + r.getPredicate() + 
				"(" + r.getArg0() + ":" + r.getPos0() + 
				"," + r.getArg1() + ":" + r.getPos1() + "," + ")"); 
		
		found = hg.findOne(graph, 
			hg.and(
				hg.type(RelationCount.class), 
				hg.eq("predicate", r.getPredicate()),
				hg.eq("arg0", r.getArg0()),
				hg.eq("arg1", r.getArg1()),
				hg.eq("pos0", r.getPos0()),
				hg.eq("pos1", r.getPos1())
			)
		); */
//		if (found != null)
//			rcCache.put(key, (RelationCount)graph.get(found));
		return found;
	}
	
	/**
	 * Find the stored value (count) of the given RelationCount, or 0 if the relation
	 * wasn't found
	 * 
	 * @param graph
	 * @param r
	 * @return The relation count
	 */
	public static double getCount(HyperGraph graph, RelationCount r) 
	{
		final HGHandle h = find(graph,r);
		if (h==null) return 0;

//		String key = r.getPredicate() + "|" + r.getArg0() + "|" + r.getArg1() + 
//	     "|" + r.getPos0() + "|" + r.getPos1(); 
		
		RelationCount found = graph.get(h);
		if (found==null) return 0;
		return found.getCount();
	}	
	
	public static class Visit implements RelationCallback	
	{
		HashMap<String, String> pos = new HashMap<String, String>();
		ArrayList<ArrayList<String>> relations = new ArrayList<ArrayList<String>>(); 
		
		public Boolean BinaryHeadCB(FeatureNode node) 
		{
			return false;
		}
		
		public Boolean BinaryRelationCB(String linkName, 
		                                FeatureNode srcNode, 
		                                FeatureNode tgtNode) 
		{
			ArrayList<String> elements = new ArrayList<String>();
			elements.add(linkName);
			elements.add(getName(srcNode.get("nameSource")));
			elements.add(getName(tgtNode.get("nameSource")));
			relations.add(elements);
			return false;
		}
		
		public Boolean UnaryRelationCB(FeatureNode srcNode, String attrName) 
		{
			FeatureNode attr = srcNode.get(attrName);
			if (!attr.isValued()) return false;
			if (attrName.equals("pos"))
			{
				pos.put(getName(srcNode.get("nameSource")), attr.getValue());
			}
			return false;
		}
	}
	
	public static String getName(FeatureNode fn)
	{
		if (fn == null) return null;
		fn = fn.get("ref");
		if (fn == null) return null;
		fn = fn.get("name");
		if (fn == null) return null;
		return fn.getValue();
	}

	public static void createCountingIndices(HyperGraph graph) 
	{
		getRCIndex(graph);
/*		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "predicate" }));
		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "arg0" }));
		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "arg1" }));
		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "pos0" }));
		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "pos1" })); */
		
	}

    public static RelationCount getRelationCount(HashMap<String, String> entityTypes, 
                                                 List<String> components,
                                                 int [] pos) 
    {
        String pred = components.get(0);
        String arg0 = components.get(1);
        String arg1 = components.get(2);
        String pos0 = Integer.toString(pos[0]);
        String pos1 = Integer.toString(pos[1]);
        
        String type0 = entityTypes.get(arg0);
        if (type0 != null) arg0 = type0;
        
        String type1 = entityTypes.get(arg1);
        if (type1 != null) arg1 = type1;
        
        return new RelationCount(pred, arg0, arg1, pos0, pos1);        
    }
    
    public static ArrayList<RelationCount> getRelationCounts(HashMap<String, String> entityTypes, 
                                                             RelexParse parse) 
    {
        return getRelationCounts(entityTypes, parse.toParsedSentence());
    }
    
	public static ArrayList<RelationCount> getRelationCounts(HashMap<String, String> entityTypes, 
	                                                         ParsedSentence parsedSentence) 
    {
		ArrayList<RelationCount> relationCounts = new ArrayList<RelationCount>();
		
		if (parsedSentence.getNumWords()==0) return relationCounts; 
		
		Visit v = new Visit();
		FeatureForeach.foreach(parsedSentence.getLeft(), v);
		for(ArrayList<String> relation: v.relations)
		{
			String pred = relation.get(0);
			String arg0 = relation.get(1);
			String arg1 = relation.get(2);
			String pos0 = v.pos.get(arg0);
			String pos1 = v.pos.get(arg1);
			
			String type0 = entityTypes.get(arg0);
			if (type0!=null) arg0 = type0;
			
			String type1 = entityTypes.get(arg1);
			if (type1!=null) arg1 = type1;
			
			final RelationCount relationCount = new RelationCount(pred, arg0, arg1, pos0, pos1);
			relationCounts.add(relationCount);
			
		}
		return relationCounts;
	}

	public static HashMap<String, String> getEntityTypes(EntityMaintainer em) 
	{
		HashMap<String, String> entityTypes = new HashMap<String, String>();
		for (EntityInfo ei: em.orderedEntityInfos) 
		{
			entityTypes.put(ei.getOriginalString(), ei.getType());
		}
		entityTypes.putAll(PRONOUNS);
		return entityTypes;
	}
}
