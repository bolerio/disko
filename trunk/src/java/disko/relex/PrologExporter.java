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

import java.util.HashMap;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.SemTools;
import org.hypergraphdb.app.wordnet.WNGraph;
import org.hypergraphdb.app.wordnet.data.AdjSynsetLink;
import org.hypergraphdb.app.wordnet.data.AdverbSynsetLink;
import org.hypergraphdb.app.wordnet.data.Antonym;
import org.hypergraphdb.app.wordnet.data.Cause;
import org.hypergraphdb.app.wordnet.data.DerivedFrom;
import org.hypergraphdb.app.wordnet.data.Entails;
import org.hypergraphdb.app.wordnet.data.Hasa;
import org.hypergraphdb.app.wordnet.data.InstanceOf;
import org.hypergraphdb.app.wordnet.data.Isa;
import org.hypergraphdb.app.wordnet.data.KindOf;
import org.hypergraphdb.app.wordnet.data.MemberOf;
import org.hypergraphdb.app.wordnet.data.NounSynsetLink;
import org.hypergraphdb.app.wordnet.data.Similar;
import org.hypergraphdb.app.wordnet.data.SubstanceOf;
import org.hypergraphdb.app.wordnet.data.VerbSynsetLink;
import org.hypergraphdb.app.wordnet.data.Word;
import org.hypergraphdb.query.HGQueryCondition;
import org.hypergraphdb.util.Pair;

import disko.AnalysisContext;
import disko.SentenceAnn;
import disko.data.NamedEntity;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SyntacticPredicate;

import relex.entity.EntityType;

import alice.tuprolog.Prolog;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.clausestore.HGAtomTerm;
import alice.tuprolog.clausestore.JavaCollectionStoreFactory;
import alice.tuprolog.clausestore.JavaMapStoreFactory;
import alice.tuprolog.hgdb.HGPrologLibrary;
import alice.tuprolog.lib.JavaLibrary;

/**
 * 
 * <p>
 * This is used to convert relex relation in a given context and scope to
 * Prolog fact for the <em>tuProlog</em> engine.
 * </p>
 *
 * <p>
 * Relation occurrences are converted as follows:
 * 
 * <ol>
 * <li>The head is converted to the top-level functor name. If it's 
 * <code>SyntacticPrediate</code> its "id friendly" name is used. Otherwise
 * the lemma of the English word is used as is.</li>
 * <li>
 * Each argument to the relation is converted into a one parameter functor
 * with name the actual word (lemma) and the integer position as its single
 * argument. 
 * </li>
 * </ol>
 * 
 * </p>
 * 
 * <p>
 * There's an option (set the <code>homogenousRelations</code> flag to true), to
 * convert all relations into the flat for 'exrel(relationName, arg1, arg2, ...)'. This
 * is useful when Prolog reasoning need to treat all relations in a uniform way.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class PrologExporter
{
	private boolean homogenousRelations = false;
	private IdentityHashMap<RelOccurrence, Integer> sentenceOffsets = 
			new IdentityHashMap<RelOccurrence, Integer>(); 

	protected int getOffset(AnalysisContext<?> ctx, RelOccurrence occ)
	{
		Integer result = sentenceOffsets.get(occ);
		if (result == null)
		{
			HGHandle scope = ctx.getOneScope(ctx.getGraph().getHandle(occ));
			if (scope == null)
				throw new RuntimeException("No sentence scope for occurrence : " + occ);
			SentenceAnn ann = ctx.getGraph().get(scope);
			result = ann.getInterval().getStart();
			sentenceOffsets.put(occ, result);
		}
		return result;
	}
	
	protected String getFunctorName(Object x)
	{
		if (x instanceof Word)
			return ((Word)x).getLemma();			
		if (x instanceof SyntacticPredicate)
			return ((SyntacticPredicate)x).getName();
		else if (x instanceof NamedEntity)
		{			
			switch (EntityType.valueOf(((NamedEntity)x).getType()))
			{
				case DATE:				
					return "date_entity"; 
				case LOCATION:				
					return "location_entity";
				case MONEY:		
					return "money_entity";
				case ORGANIZATION:				
					return "organization_entity";
				case PERSON:				
					return "person_entity";
				default:
					return "entity";
			}
		}
		else
			throw new RuntimeException("Unknown occurrence argument type: " + x.getClass());		
	}
		
	private Struct exportFlatRelations(AnalysisContext<?> ctx, 
									  Map<Pair<String, Integer>, Term> words, 
									  RelOccurrence occ)
	{
		int offset = 0; //getOffset(ctx, occ);
		Term [] terms = new Term[occ.getArity()];
		terms[0] = new Struct(getFunctorName(ctx.getGraph().get(occ.getTargetAt(0))));
		for (int i = 1; i < occ.getArity(); i++)
		{	
			String fname = getFunctorName(ctx.getGraph().get(occ.getTargetAt(i)));
			Term arg = words.get(new Pair<String, Integer>(fname, offset + occ.getPosition(i - 1)));
			if (arg == null)
			{				
				arg = new Struct("word", new Term[] { 
						new Struct(fname), 
						new Struct("unknown"), 
						new alice.tuprolog.Int(offset + occ.getPosition(i - 1)), 
					  	new HGAtomTerm(occ.getTargetAt(i), ctx.getGraph())						
				});				
			}				
			terms[i] = arg;
		}
		return new Struct("exrel", terms);
	}
	
	public PrologExporter()
	{		
	}
	
	public PrologExporter(boolean homogenousRelations)
	{
		this.homogenousRelations = homogenousRelations;
	}
	
	
	public boolean isHomogenousRelations()
	{
		return homogenousRelations;
	}

	public void setHomogenousRelations(boolean homogenousRelations)
	{
		this.homogenousRelations = homogenousRelations;
	}

	public Theory getPrologTheory(AnalysisContext<?> ctx)
	{
		return getPrologTheory(ctx, ctx.find(RelOccurrence.class));
	}

	public Struct toPlainStruct(HyperGraph graph, RelOccurrence occ)
	{
		int offset = 0; // getOffset(ctx, occ);
		Term [] terms = new Term[occ.getArity() - 1];
		for (int i = 0; i < terms.length; i++)
		{
			HGHandle argHandle = occ.getTargetAt(i + 1);
			Object arg = graph.get(argHandle);
			if (arg == null)
				throw new RuntimeException("Null occurrence argument at pos " + i + " for " + occ);			
			String fname = getFunctorName(arg);
			terms[i] = new Struct(fname);
		}
		return new Struct(getFunctorName(graph.get(occ.getTargetAt(0))), terms);
	}
	
	public Struct occurrenceToStruct(HyperGraph graph, RelOccurrence occ)
	{
		int offset = 0; // getOffset(ctx, occ);
		Term [] terms = new Term[occ.getArity() - 1];
		for (int i = 0; i < terms.length; i++)
		{
			HGHandle argHandle = occ.getTargetAt(i + 1);
			Object arg = graph.get(argHandle);
			if (arg == null)
				throw new RuntimeException("Null occurrence argument at pos " + i + " for " + occ);			
			String fname = getFunctorName(arg);
			terms[i] = new Struct(fname, 
								  new alice.tuprolog.Int(offset + occ.getPosition(i)), 
								  new HGAtomTerm(argHandle, graph));
		}
		return new Struct(getFunctorName(graph.get(occ.getTargetAt(0))), terms);
	}
	
	/**
	 * <p>Get all <code>RelOccurrence</code> from the current scope and 
	 * convert them into Prolog facts (i.e. instances of <code>Struct</code>.
	 * </p>
	 * 
	 * <p>
	 * A version that is independent of the <em>tuProlog</em> could make
	 * the conversion to a textual representation, but for performance
	 * purposes we use the <em>tuProlog</em> internal representation directly.
	 * </p>
	 * 
	 * @param ctx
	 * @return
	 */
	public Theory getPrologTheory(AnalysisContext<?> ctx, Set<RelOccurrence> occurrences)
	{
		Struct result = new Struct();		
		sentenceOffsets.clear();
		
		if (homogenousRelations)
		{
			HashMap<Pair<String, Integer>, Term> words = new HashMap<Pair<String, Integer>, Term>();
			for (RelOccurrence occ : occurrences)
			{
				if (occ.getArity() > 2) 
					continue;
				String relname = getFunctorName(ctx.getGraph().get(occ.getTargetAt(0)));
				String pos = null;
				if ("rx_noun".equals(relname))
					pos = "noun";
				else if ("rx_verb".equals(relname))
					pos = "verb";				
				else if ("rx_adverb".equals(relname))
					pos = "adverb";				
				else if ("rx_adjective".equals(relname))
					pos = "adjective";
				else if ("rx_det".equals(relname))
					pos="det";
				else if ("rx_prep".equals(relname))
					pos="prep";
				else if ("rx_particle".equals(relname))
					pos="particle";				
				else
					continue;
				String wordname = getFunctorName(ctx.getGraph().get(occ.getTargetAt(1)));
				int offset = 0;// getOffset(ctx, occ);
				Term term = new Struct("word", new Term[] { 
												new Struct(wordname), 
												new Struct(pos), 
												new alice.tuprolog.Int(offset + occ.getPosition(0)), 
											  	new HGAtomTerm(occ.getTargetAt(1), ctx.getGraph())						
										});
				words.put(new Pair<String, Integer>(wordname, offset + occ.getPosition(0)), term);
			}
			for (RelOccurrence occ : occurrences)
				result = new Struct(exportFlatRelations(ctx, words, occ), result);
			for (Term wordTerm : words.values())
				result = new Struct(wordTerm, result);
		}
		else for (RelOccurrence occ : occurrences)
				result = new Struct(occurrenceToStruct(ctx.getGraph(), occ), result); 
		try 
		{ 
			Theory t = new Theory(result);
			t.append(this.getEntitySenses(ctx.getGraph()));
			return t;
		}
		catch (Exception ex) {throw new RuntimeException(ex); } 
	}
	
	/**
	 * Get the WordNet senses for each type of entity. The predicates are in the form
	 * 'entitySense(typeOfEntity, senseAtom)' where type of entity is one of "entity_location",
	 * "entity_organization", etc...
	 * 
	 *  The ids (offsets) for those senses are hard-coded for WordNet 2.1. We don't have a general
	 *  sense identifier across WordNet versions, so this is the best we can do as HGDB handles
	 *  are even less stable.
	 * @param graph
	 * @return
	 */
	public Theory getEntitySenses(HyperGraph graph) throws Exception
	{
		WNGraph wn = new WNGraph(graph);
		Struct result = new Struct();
		Term [] terms = new Term[2];
		terms[0] = new Struct("date_entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(14961539L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		terms[0] = new Struct("location_entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(152507L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		terms[0] = new Struct("money_entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(13212169L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		terms[0] = new Struct("organization_entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(8052497L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		terms[0] = new Struct("person_entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(7626L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		terms[0] = new Struct("program_entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(5824218L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		terms[0] = new Struct("entity");
		terms[1] = new HGAtomTerm(wn.getSenseById(1740L), graph);
		result = new Struct(new Struct("entitySense", terms), result);
		
		return new Theory(result);
	}
	
	public Prolog makePrologInstance(HyperGraph graph)
	{
		try
		{
			Prolog prolog = new Prolog();
			prolog.getEngineManager().getClauseStoreManager().getFactories().add(new JavaCollectionStoreFactory());
			prolog.getEngineManager().getClauseStoreManager().getFactories().add(new JavaMapStoreFactory());			
			HGPrologLibrary lib = HGPrologLibrary.attach(graph, prolog);
			Map<String, HGQueryCondition> m = lib.getClauseFactory().getPredicateMapping();
			m.put("wn_word/1", hg.type(Word.class));
			m.put("wn_isa/2", hg.type(Isa.class));
			m.put("wn_kindof/2", hg.type(KindOf.class));
			m.put("wn_instanceof/2", hg.type(InstanceOf.class));
			m.put("wn_similar/2", hg.type(Similar.class));			
			m.put("wn_hasa/2", hg.type(Hasa.class));
			m.put("wn_member/2", hg.type(MemberOf.class));			
			m.put("wn_substance/2", hg.type(SubstanceOf.class));
			m.put("wn_derivedfrom/2", hg.type(DerivedFrom.class));
			
			JavaLibrary java = (JavaLibrary)prolog.getLibraryManager().getLibrary(JavaLibrary.class.getName());
			java.register(new Struct("graph"), graph);
			SemTools semtools = new SemTools(graph);
			java.register(new Struct("wnSemTools"), semtools);
			java.register(new Struct("wngraph"), semtools.getWNGraph());			
			java.register(new Struct("hg"), new HGQuery.hg());
			java.register(new Struct("nounSenseType"), NounSynsetLink.class);
			java.register(new Struct("verbSenseType"), VerbSynsetLink.class);
			java.register(new Struct("adjSenseType"), AdjSynsetLink.class);
			java.register(new Struct("adverbSenseType"), AdverbSynsetLink.class);
			java.register(new Struct("semSimilar"), Similar.class);
			java.register(new Struct("semAntonym"), Antonym.class);
			java.register(new Struct("semEntails"), Entails.class);
			java.register(new Struct("semCause"), Cause.class);
			java.register(new Struct("lexDerivedFrom"), DerivedFrom.class);
			return prolog;
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
