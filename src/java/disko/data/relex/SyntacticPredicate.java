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
package disko.data.relex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.handle.UUIDHandleFactory;
import org.hypergraphdb.util.TwoWayMap;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HyperGraph;

public class SyntacticPredicate
{
	private static TwoWayMap<String, String> relationNames = new TwoWayMap<String, String>();
	
	static
	{
		relationNames.add("rx_action", "_to-do");
		relationNames.add("rx_adj_modifier", "_amod");
		relationNames.add("rx_adv_modifier", "_advmod");
		relationNames.add("rx_appositive", "_appo");
		relationNames.add("rx_cause", "_%because");
		relationNames.add("rx_ind_obj", "_iobj");
		relationNames.add("rx_is", "_%inheritance");
		relationNames.add("rx_location", "_%atLocation");
		relationNames.add("rx_noun_mod", "_nn");
		relationNames.add("rx_object", "_obj");
		relationNames.add("rx_prep_object", "_pobj");
		relationNames.add("rx_prep_subject", "_psubj");
		relationNames.add("rx_pred_adjective", "_predadj");
		relationNames.add("rx_possesion", "_poss");
		relationNames.add("rx_property", "_to-be");
		relationNames.add("rx_quant_mult", "_%quantity_mult");
		relationNames.add("rx_quantifier", "_%quantity_mod");
		relationNames.add("rx_quantity", "_%quantity");
		relationNames.add("rx_subject", "_subj");
		relationNames.add("rx_that", "_that");
		relationNames.add("rx_time", "_%atTime");
		relationNames.add("rx_comparison", "_more");
		relationNames.add("rx_gender", "gender");
		relationNames.add("rx_number", "noun_number");
		relationNames.add("rx_pos", "POS");
		relationNames.add("rx_quantification", "DEFINITE-FLAG");
		relationNames.add("rx_query", "QUERY-TYPE");
		relationNames.add("rx_tense", "tense");
		relationNames.add("rx_hyp", "hyp");
		relationNames.add("rx_copula", "_%copula");
		
		// unary relations on words, coming from the value of rx_quantification etc.
		relationNames.add("rx_word", "WORD"); // a lonely word that wasn't included in a linkage
		relationNames.add("rx_noun", "noun");
		relationNames.add("rx_verb", "verb");
		relationNames.add("rx_adjective", "adj");
		relationNames.add("rx_adverb", "adv");
		relationNames.add("rx_definite", "definite");
		relationNames.add("rx_past", "past");
		relationNames.add("rx_present", "present");
		relationNames.add("rx_future", "future");
		relationNames.add("rx_singular", "singular");
		relationNames.add("rx_plural", "plural");
		relationNames.add("rx_punctuation", "punctuation");
		relationNames.add("rx_det", "det");
		relationNames.add("rx_prep", "prep");
		relationNames.add("rx_progressive", "progressive");
		relationNames.add("rx_present_progressive", "present_progressive");
		relationNames.add("rx_infinitive", "infinitive");
		relationNames.add("rx_particle", "particle");
		relationNames.add("rx_uncountable", "uncountable");
		relationNames.add("rx_countable", "countable");
	}
	
	private static Map<String, HGPersistentHandle> syntacticPredicates = new HashMap<String, HGPersistentHandle>();
	
	static
	{
		syntacticPredicates.put("rx_action", UUIDHandleFactory.I.makeHandle("bc3ab551-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_adj_modifier", UUIDHandleFactory.I.makeHandle("c44349b2-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_adv_modifier", UUIDHandleFactory.I.makeHandle("c81ed633-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_appositive", UUIDHandleFactory.I.makeHandle("1827ae1d-4eb4-411f-bbb1-12d99d95b700"));
		syntacticPredicates.put("rx_cause", UUIDHandleFactory.I.makeHandle("cc97b424-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_ind_obj", UUIDHandleFactory.I.makeHandle("d04fda25-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_is", UUIDHandleFactory.I.makeHandle("d4ae0426-a801-11dc-a076-0019b91e4d7d"));		
		syntacticPredicates.put("rx_location", UUIDHandleFactory.I.makeHandle("de214c18-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_noun_mod", UUIDHandleFactory.I.makeHandle("e5398d09-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_object", UUIDHandleFactory.I.makeHandle("e7a9548a-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_pred_adjective", UUIDHandleFactory.I.makeHandle("d6b6d744-42fb-4b0a-9c81-726894214950"));
		syntacticPredicates.put("rx_prep_object", UUIDHandleFactory.I.makeHandle("f026077c-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_prep_subject", UUIDHandleFactory.I.makeHandle("f2f15c7d-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_possesion", UUIDHandleFactory.I.makeHandle("f61335ee-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_property", UUIDHandleFactory.I.makeHandle("f98e52ef-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_quant_mult", UUIDHandleFactory.I.makeHandle("fc86d270-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_quantifier", UUIDHandleFactory.I.makeHandle("ffcd4ae1-a801-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_quantity", UUIDHandleFactory.I.makeHandle("02d1ff62-a802-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_subject", UUIDHandleFactory.I.makeHandle("058893e3-a802-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_that", UUIDHandleFactory.I.makeHandle("082f70f4-a802-11dc-a076-0019b91e4d7d"));		
		syntacticPredicates.put("rx_time", UUIDHandleFactory.I.makeHandle("0b364855-a802-11dc-a076-0019b91e4d7d"));
		syntacticPredicates.put("rx_comparison", UUIDHandleFactory.I.makeHandle("0fd524d7-a802-11dc-a076-0019b91e4d7d"));		
		syntacticPredicates.put("rx_gender", UUIDHandleFactory.I.makeHandle("2e3654ef-a9f0-11dc-9b4d-0019b91e4d7d"));
		syntacticPredicates.put("rx_number", UUIDHandleFactory.I.makeHandle("47ebc0b0-a9f0-11dc-9b4d-0019b91e4d7d"));		
		syntacticPredicates.put("rx_pos", UUIDHandleFactory.I.makeHandle("73c3d9fb-a9fc-11dc-9353-0019b91e4d7d"));		
		syntacticPredicates.put("rx_quantification", UUIDHandleFactory.I.makeHandle("1ec30c25-ac37-11dc-98a3-0019b91e4d7d"));
		syntacticPredicates.put("rx_query", UUIDHandleFactory.I.makeHandle("30352476-ac37-11dc-98a3-0019b91e4d7d"));
		syntacticPredicates.put("rx_tense", UUIDHandleFactory.I.makeHandle("02886586-ac4b-11dc-bff6-0019b91e4d7d"));
		syntacticPredicates.put("rx_hyp", UUIDHandleFactory.I.makeHandle("09ab54d7-ac4b-11dc-bff6-0019b91e4d7d"));
		syntacticPredicates.put("rx_copula", UUIDHandleFactory.I.makeHandle("0e903b08-ac4b-11dc-bff6-0019b91e4d7d"));
		
		syntacticPredicates.put("rx_word", UUIDHandleFactory.I.makeHandle("13877cfb-292a-402d-80a1-9d25d62154d1"));
		syntacticPredicates.put("rx_noun", UUIDHandleFactory.I.makeHandle("79e54364-f745-4737-a24a-dcfeb0abcf06"));
		syntacticPredicates.put("rx_verb", UUIDHandleFactory.I.makeHandle("61a56220-06bf-451b-8e78-3a1e1b971015"));
		syntacticPredicates.put("rx_adjective", UUIDHandleFactory.I.makeHandle("c2198a89-2870-4562-b00b-701af7731016"));
		syntacticPredicates.put("rx_adverb", UUIDHandleFactory.I.makeHandle("00c7a572-25bd-4fa1-9738-288f43929756"));
		syntacticPredicates.put("rx_definite", UUIDHandleFactory.I.makeHandle("2c06f616-e2bc-43bd-a679-56f8da67cea3"));
		syntacticPredicates.put("rx_past", UUIDHandleFactory.I.makeHandle("420dc7ab-a687-4f81-846c-091f829367da"));
		syntacticPredicates.put("rx_present", UUIDHandleFactory.I.makeHandle("f0b5399e-ddd9-4dd5-83a6-7aad5adb9f91"));
		syntacticPredicates.put("rx_future", UUIDHandleFactory.I.makeHandle("3b82d4fe-71c3-4061-9338-75c1929cba3b"));
		syntacticPredicates.put("rx_singular", UUIDHandleFactory.I.makeHandle("6c48dce5-edd4-4247-bc98-6e822b65e9a6"));
		syntacticPredicates.put("rx_plural", UUIDHandleFactory.I.makeHandle("06fe55d4-0d6f-45d9-9366-741b8b8edc10"));
		syntacticPredicates.put("rx_punctuation", UUIDHandleFactory.I.makeHandle("7fa318db-8768-4477-b730-274cd679df76"));
		syntacticPredicates.put("rx_det", UUIDHandleFactory.I.makeHandle("55d1037c-c435-434b-a666-85ac3d5e68b6"));
		syntacticPredicates.put("rx_prep", UUIDHandleFactory.I.makeHandle("07db74f4-8190-49f4-bc3e-cacb2be49221"));
		syntacticPredicates.put("rx_progressive", UUIDHandleFactory.I.makeHandle("feeda882-7f4d-4373-bb97-8ddde7420343"));
		syntacticPredicates.put("rx_present_progressive", UUIDHandleFactory.I.makeHandle("bec8e93f-641a-4f9e-8ffe-eb05602f3b47"));
		syntacticPredicates.put("rx_infinitive", UUIDHandleFactory.I.makeHandle("6756e967-5f11-479d-b3a1-d37c9ff93722"));
		syntacticPredicates.put("rx_particle", UUIDHandleFactory.I.makeHandle("b2263e0d-d06a-4a09-b62f-75cd8b58daf7"));
		syntacticPredicates.put("rx_uncountable", UUIDHandleFactory.I.makeHandle("9f621dc4-7c06-48e7-b203-c81f6bf65eb9"));
		syntacticPredicates.put("rx_countable", UUIDHandleFactory.I.makeHandle("2d263969-c332-4592-b705-1ee731fc7391"));
	}
	
	
	private String name;

	public static void loadAll(HyperGraph graph)
	{
		graph.getTransactionManager().beginTransaction();
		try
		{
			for (Map.Entry<String, HGPersistentHandle> e: syntacticPredicates.entrySet())
			{
				if (graph.get(e.getValue()) == null)
					graph.define(e.getValue(), new SyntacticPredicate(e.getKey()));
			}
			graph.getTransactionManager().endTransaction(true);
		}
		catch (Throwable ex)
		{
			try { graph.getTransactionManager().endTransaction(false); }
			catch (Throwable t) { t.printStackTrace(System.err); }
			if (ex instanceof RuntimeException) throw (RuntimeException)ex;
			else throw new RuntimeException(ex);
		}
	}
	
	public static void deleteAll(HyperGraph graph)
	{
		//
		// We can't do a single transaction here, because we are potentially deleting
		// a lot of relations and BDB will run out of locks.
		//	
		
		List<HGHandle> all = hg.findAll(graph, hg.type(SyntacticPredicate.class));
		for (HGHandle h : all)
		{
			try { graph.remove(h); }
			catch (RuntimeException ex)
			{ ex.printStackTrace(System.err); }			
		}
	}
	
	public SyntacticPredicate() { }
	public SyntacticPredicate(String name) { this.name = name; }
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public static HGHandle getHandle(String name) 
	{ 
		HGHandle result = syntacticPredicates.get(name);
		if (result == null)
		{
			String s = relationNames.getX(name);
			if (s != null)
				result = syntacticPredicates.get(s);
		}
		return result;
	}
	
	public String toString()
	{
		return name;
	}
	
	public static String relexToIdFriendly(String relexName)
	{
		String x = relationNames.getX(relexName);
		return x != null ? x : relexName;
	}
	
	public static String idFriendlyToRelex(String idFriendlyName)
	{
		String y = relationNames.getY(idFriendlyName);
		return y!=null ? y : idFriendlyName;
	}	
}
