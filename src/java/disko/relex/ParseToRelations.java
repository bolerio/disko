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
package disko.relex;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.Word;
import org.hypergraphdb.indexing.ByPartIndexer;

import disko.AnalysisContext;
import disko.Ann;
import disko.BaseAnn;
import disko.DU;
import disko.TextDocument;
import disko.data.NamedEntity;
import disko.data.UnknownWord;
import disko.data.owl.OWLClass;
import disko.data.owl.OWLClassConstructor;
import disko.data.owl.OWLIndividual;
import disko.data.owl.OWLPropertyInstance;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SyntacticPredicate;

import relex.ParsedSentence;
import relex.entity.EntityInfo;
import relex.entity.EntityMaintainer;
import relex.entity.EntityType;
import relex.feature.FeatureForeach;
import relex.feature.FeatureNode;
import relex.feature.RelationCallback;
//import relex.output.ParseView;
import relex.output.RawView;

public class ParseToRelations
{
	protected static Log log = LogFactory.getLog(ParseToRelations.class);
	
	private HyperGraph graph;
	private HashMap<EntityType, OWLClass> eTypeToOWL = new HashMap<EntityType, OWLClass>();
	
	private void init()
	{
		OWLClass cl = hg.getOne(graph, 
				hg.and(hg.type(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE),
						   hg.eq("localName", "AutoDetectLocation")));
		if (cl != null)
		{
			eTypeToOWL.put(EntityType.LOCATION, cl);		
			graph.getIndexManager().register(new ByPartIndexer(graph.getHandle(cl), "localName"));
		}
		
		cl = hg.getOne(graph, 
				hg.and(hg.type(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE),
						   hg.eq("localName", "AutoDetectOrganization")));
		if (cl != null)
		{
			eTypeToOWL.put(EntityType.ORGANIZATION, cl);
			graph.getIndexManager().register(new ByPartIndexer(graph.getHandle(cl), "localName"));
		}
		
		cl = hg.getOne(graph, 
				hg.and(hg.type(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE),
						   hg.eq("localName", "AutoDetectPerson")));
		if (cl != null)
		{
			eTypeToOWL.put(EntityType.PERSON, cl);
			graph.getIndexManager().register(new ByPartIndexer(graph.getHandle(cl), "localName"));
		}
		
		// Make sure any unindexed data gets indexed, this should actually complete immediately in a production environment.		
		graph.runMaintenance(); 
	}
	
	public ParseToRelations(HyperGraph graph)
	{
		this.graph = graph;
		init();
	}
	
	public Set<RelOccurrence> getAnaphoraRelations(AnalysisContext<TextDocument> context,
												   EntityMaintainer em,
												   ParsedSentence parse,												 
												   HashMap<FeatureNode,ArrayList<FeatureNode>> amap)
	{
		HashSet<RelOccurrence> relations = new HashSet<RelOccurrence>();
		addAnaphoraRelations(context, em, parse, relations, amap);
		return relations;
	}
	
	public void addAnaphoraRelations(AnalysisContext<TextDocument> context,
									 EntityMaintainer em,
									 ParsedSentence parse,
									 Set<RelOccurrence> relations,
									 HashMap<FeatureNode,ArrayList<FeatureNode>> amap)
	{
		Visit v = new Visit();
		v.context = context;
		v.relations = relations;
		v.root = parse.getLeft();
		v.em = em;

		// Handle the anaphora resolution
		if (amap != null)
			v.anaphora(amap);		
	}
	
	/**
	 * <p>
	 * See full version of this method, called with null <code>amap</code> parameter. 
	 * </p>
	 */
	public Set<RelOccurrence> getRelations(AnalysisContext<TextDocument> context,
										   EntityMaintainer em,
										   ParsedSentence parse)
    {
		return getRelations(context, em, parse, null);
    }

    /**
     * <p>
     * See full version of this method, called with null <code>ignore</code> parameter. 
     * </p>
     */	
	public Set<RelOccurrence> getRelations(AnalysisContext<TextDocument> context,
										   EntityMaintainer em,
										   ParsedSentence parse,
										   HashMap<FeatureNode,ArrayList<FeatureNode>> amap)
    {
		return getRelations(context, em, parse, amap, null);
	}
	
    /**
     * Collect all relations found and put them into the Set "relations".
     *
     * The root node must be the LEFT-WALL FeatureNode: parse.getLeft()
     *
     * In unary relations the predicate comes from a feature of the
     * node being visited.
     *
     * Most of the time the predicate is given by the value of the
     * feature (e.g. the word "ate" becomes a node named "eat",
     * which has a feature named "tense" with value "past" -
     * producing the unary relation "past(eat)".
     *
     * Flags, however, always have value equals "T" ("True"). In this
     * case, we use the name of the flag to create the relation (e.g.
     * a node "book" with a flag DETERMINED-FLAG equals "T" produces
     * a relation "determined(book)".
     *
     * @param context The current analysis context.
     * @param em The <code>EntityMaintainer</code> for the sentence of the parse.
     * @param parse
     * @param anaphora A map of antecedents for anaphora resolution. If null, co-reference
     * relations are not added to the result set.
     * @param ignore A set of features to ignore.
     * @return The set of generated <code>RelOccurrence</code>s.
     */
	public Set<RelOccurrence> getRelations(AnalysisContext<TextDocument> context,
										   EntityMaintainer em,
										   ParsedSentence parse,
										   HashMap<FeatureNode,ArrayList<FeatureNode>> amap,
										   Set<String> ignore) 
    {
		if (DU.log.isTraceEnabled()) 
		{
		    DU.log.info("get relations for '" + em.getOriginalSentence() + "'");
		    DU.log.info("\n" + RawView.printZHeads(parse.getLeft()));
		    DU.log.info("link string " + parse.getLinkString());			
		}		

		HashSet<RelOccurrence> relations = new HashSet<RelOccurrence>();

		if (parse.getLeafConstituents().isEmpty())
		    return relations;
		
		Visit v = new Visit();
		v.context = context;
		v.relations = relations;
		v.root = parse.getLeft();
		v.em = em;
		v.ignore= ignore!=null ? ignore : new TreeSet<String>();
		FeatureForeach.foreach(parse.getLeft(), v);

		// Handle the anaphora resolution
		if (amap != null)
			v.anaphora(amap);
		
//		if (log.isTraceEnabled())
//		{
//			ParseView cerview = new ParseView();
//			cerview.setParse (parse);
//			log.trace(cerview.printCerego());
//		}

		return relations;		
	}
	
	private class Visit implements RelationCallback
	{
		AnalysisContext<TextDocument> context;
		Set<RelOccurrence> relations;
		FeatureNode root;
		EntityMaintainer em;
		Set<String> ignore; 
		
		int positionOffset = 0;
		
		public Boolean BinaryHeadCB(FeatureNode node)
		{
			return false;
		}
		
		public Boolean BinaryRelationCB(String linkName, FeatureNode srcNode, FeatureNode tgtNode)
		{
			if (ignore.contains(linkName)) return false;
			
			FeatureNode sourceNameSource = srcNode.get("nameSource");
			FeatureNode targetNameSource = tgtNode.get("nameSource");
			if ( (sourceNameSource!=null) && (targetNameSource!=null) ) 
			{
				relations.add(createRelation(linkName, 
										     sourceNameSource, 
										     targetNameSource));
			}
			else
                DU.log.debug("No nameSource node for \n" + srcNode + " or " + tgtNode + 
                            " in sentence " + em.getOriginalSentence());			    
			return false;
		}
		
		public Boolean UnaryRelationCB(FeatureNode srcNode, String attrName)
		{
			FeatureNode attr = srcNode.get(attrName);
			if (!attr.isValued()) return false;

			String value = null;			
			
			if (attrName.endsWith("-FLAG"))
				value = attrName.replaceAll("-FLAG","").toLowerCase();
			else if (attrName.endsWith("-TAG"))
			    value = attrName.replaceAll("-TAG","").toLowerCase();
			else if (attrName.equals("HYP")) 
			    value = attrName.toLowerCase();
			else
			    value = attr.getValue();
			
            if (ignore.contains(value)) return false;
            
			FeatureNode nameSource = srcNode.get("nameSource");
			if (nameSource == null)
			    DU.log.debug("No nameSource node for \n" + srcNode + 
			                " in sentence " + em.getOriginalSentence());
			else
			    relations.add(createRelation(value, nameSource));
			return false;
		}	
	
		/**
		 * Add one or more antecedent relations for each anaphora found.
		 */
		void anaphora(HashMap<FeatureNode,ArrayList<FeatureNode>> amap)
		{
			
			for (FeatureNode prn: amap.keySet())
			{
				// Get the anaphore (pronoun)
				FeatureNode srcNSource = prn;
				String srcName = getName(srcNSource);
				if (srcName == null) continue;

				// Loop over list of antecedents
				ArrayList<FeatureNode> ante_list = amap.get(prn);
				for (Integer i =0; i<ante_list.size(); i++)
				{
					// The larger the "i", the less likely this is the correct antecedent.
					String linkName = "_ante_" + i;
					FeatureNode targetNSource = ante_list.get(i);
					relations.add(createRelation(linkName, srcNSource, targetNSource));
				}
			}
		}
		
		RelOccurrence createRelation(String predicateName, FeatureNode...nodes)
		{		
			HGHandle[] handles = new HGHandle[nodes.length + 1];
			int [] locations = new int[nodes.length];
			handles[0] = getPredicateHandle(predicateName);
			String args = "";
			for (int i = 0; i < nodes.length; i++)
			{
				Ann ann = getLocation(nodes[i]);
				locations[i] = ann != null ?  positionOffset + ann.getInterval().getStart() : -1;
				handles[i + 1] = getLexicalEntity(getName(nodes[i]), 
												  ann != null ? ann.getInterval().getStart() : -1);
				args += graph.get(handles[i+1]);
				if (i < nodes.length - 1)
				    args += ",";
			}			
//			System.out.println("Created " + predicateName + "(" + args + ")");
			return new RelOccurrence(handles, locations);
		}
	
	
		HGHandle getPredicateHandle(String name)
		{		
			HGHandle h = SyntacticPredicate.getHandle(name);
			return h == null ? getLexicalEntity(name, -1) : h;
		}
	
		Ann getLocation(FeatureNode nameSourceNode)
		{
			Ann location = null;
			if (nameSourceNode != null) 
			{
				int startChar = -1;
				int endChar = -1;
				try 
				{
					startChar = Integer.parseInt(nameSourceNode.get("start_char").getValue());
					if (startChar < 0)					
						throw new RuntimeException("start char is < 0 for " + 
						                           getName(nameSourceNode) +
						                           " in sentence " + em.getOriginalSentence());
				    endChar = Integer.parseInt(nameSourceNode.get("end_char").getValue());
					if (endChar < 0)
                        throw new RuntimeException("end char is < 0 for " + 
                                                   getName(nameSourceNode) +
                                                   " in sentence " + em.getOriginalSentence());					    
				} 
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
				location = new BaseAnn(startChar, endChar);
			}
			return location;
		}
	
		HGHandle getLexicalEntity(String lemma, int position)
		{		    
			if (lemma == null)
				throw new IllegalArgumentException("getLexicalEntity: lemma is null");
			if (lemma.startsWith("[") && lemma.endsWith("]"))
			    lemma = lemma.substring(1, lemma.length() - 1);
			HGHandle result = null;
	
			// A named entity
			EntityInfo eInfo = null;
			if (em != null)
			{
				for (EntityInfo ei : em.getEntities())
					if (ei.getFirstCharIndex() == position)
						eInfo = ei;
			}
			
			if (eInfo != null)
			{
				lemma = eInfo.getOriginalString();
				if (lemma.startsWith("location"))
					System.err.println("oops, we have a problem!");
				String handleAttr = (String)eInfo.getAttributes().get("HGHANDLE");
				if (handleAttr != null)
					return context.getGraph().getHandleFactory().makeHandle(handleAttr);
				if ( (result = getOntologyEntity(lemma, eInfo)) == null)
					result = getGenericEntity(lemma, eInfo);
				if (result == null)
					throw new RuntimeException("Unable to find atom for named entity: " + 
							lemma + " at position " + 
							position + " for sentence " + em.getOriginalSentence());
				else
					return result;
			}
						
			// A common word
			result = hg.findOne(context.getGraph(), 
			                    hg.and(hg.typePlus(Word.class), 
			                           hg.eq("lemma", lemma.toLowerCase())));
	
			// Return existing word or...
			if (result != null) 
			    return result;
	
			DU.log.debug("NEW WORD:" + lemma);
	
			result = context.getGraph().add(new UnknownWord(lemma.toLowerCase()));
			return result;		
		}
		
		String getName(FeatureNode fn)
		{
			if (fn == null) 
			    return null;			
			fn = fn.get("ref");
			if (fn == null) 
			    return null;			
			fn = fn.get("name");
			if (fn == null) 
			    return null;	
			return fn.getValue();
		}	
		
		HGHandle getOntologyEntity(String lemma, EntityInfo eInfo)
		{
			// Check if we have actually loaded an ontology 
			if (context.getGraph().get(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE) == null)
				return null;
			OWLClass owlClass = eTypeToOWL.get(EntityType.valueOf(eInfo.getType()));
			if (owlClass == null)
				return null;
			HGHandle indHandle = hg.findOne(graph, 
										hg.and(hg.type(graph.getHandle(owlClass)), 
											   hg.eq("localName", lemma)));
			if (indHandle != null)
				return indHandle;
			OWLIndividual owlInd = new OWLIndividual(owlClass);					
			owlInd.setLocalName(lemma);
			indHandle = context.getGraph().add(owlInd, context.getGraph().getHandle(owlClass));
			HGHandle propHandle = context.getGraph().getHandle(owlClass.getProperties().get("Name"));
			HGHandle valueAtom = hg.findOne(context.getGraph(), hg.eq(lemma));
			if (valueAtom == null)
				valueAtom = context.getGraph().add(lemma);
			context.getGraph().add(new OWLPropertyInstance(indHandle, valueAtom), propHandle);
			return indHandle;
		}
		
		HGHandle getGenericEntity(String lemma, EntityInfo eInfo)
		{
			NamedEntity n = new NamedEntity(lemma, eInfo.getType());
			HGHandle result = hg.findOne(context.getGraph(), hg.eq(n));
			if (result == null)
				result = context.getGraph().add(n);
			return result;
		}
	}
}
