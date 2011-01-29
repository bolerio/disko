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
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.HyperGraph;
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
import org.hypergraphdb.query.And;
import org.hypergraphdb.query.HGQueryCondition;
import org.hypergraphdb.util.Pair;

import disko.AnalysisContext;
import disko.DU;
import disko.Danalyzer;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SemRel;
import disko.utils.SAN;

import alice.tuprolog.Library;
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
 * This will run some Prolog rules to assign senses to words in <code>RelOccurrence</code>
 * instances.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class SenseMaker implements Danalyzer<AnalysisContext<TextDocument>>
{
	private Prolog prolog = null;
	private PrologExporter exporter = new PrologExporter(true);
	private double sanThreshold = 0.2;
	private double similarityThreshold = 0.5;
	private SAN<Pair<Pair<HGHandle, Integer>, HGHandle>> san = 
		new SAN<Pair<Pair<HGHandle, Integer>, HGHandle>>(sanThreshold);
	
	private static class WSDLibrary extends Library
	{
		private static final long serialVersionUID = -1;
		
		@Override
		public String getName() { return "discoWSD"; }
		
		@Override
		public String getTheory()
		{
			try
			{
				return DU.readResource("/prolog/wsd2.pl");
			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}
		}
	}
	
	private void initProlog(HyperGraph graph)
	{
		if (prolog != null)
			return;
		try
		{
			prolog = exporter.makePrologInstance(graph);
			JavaLibrary java = (JavaLibrary)prolog.getLibraryManager().getLibrary(JavaLibrary.class.getName());			
			java.register(new Struct("san"), san);
			prolog.getLibraryManager().loadLibrary(new WSDLibrary());
		}
		catch (Throwable t)
		{
			throw new RuntimeException("Failed to initialize SenseMaker.", t);
		}			
	}
	

	
	private HGHandle [] getSenses(RelOccurrence occ, Map<Pair<HGHandle, Integer>, HGHandle> senseResult)
	{
		HGHandle [] result = new HGHandle[occ.getArity()];
		result[0] = occ.getTargetAt(0);		
		for (int i = 1; i < occ.getArity(); i++)
		{
			result[i] = senseResult.get(new Pair<HGHandle, Integer>(occ.getTargetAt(i), occ.getPosition(i-1)));
			if (result[i] == null)
				return null; // throw new RuntimeException("No sense for token at position " + occ.getPosition(i-1));
		}
		return result;
	}
	
	private SemRel getSemRel(AnalysisContext<TextDocument> ctx, 
							 RelOccurrence occ, 
							 HGHandle [] senses)
	{
		if (senses == null)
			return null;
		And cond = new And(hg.type(SemRel.class), hg.orderedLink(senses));
		for (HGHandle s : senses)
			cond.add(hg.incident(s));
		HGHandle relHandle = hg.findOne(ctx.getGraph(), cond);
		if (relHandle != null)
			return ctx.getGraph().get(relHandle);
		else
		{
			SemRel rel = new SemRel(senses);
			ctx.getGraph().add(rel);
			return rel;
		}
	}
	
	private void applyResults(AnalysisContext<TextDocument>  ctx, Set<RelOccurrence> occurrences)
	{
		Map<Pair<HGHandle, Integer>, HGHandle> senseResult = new HashMap<Pair<HGHandle, Integer>, HGHandle>();
		Map<Pair<HGHandle, Integer>, Pair<Integer, Double>> values = new HashMap<Pair<HGHandle, Integer>, Pair<Integer, Double>>(); 
		for (Pair<Pair<HGHandle, Integer>, HGHandle> x:san.getTimeOfDeathMap().keySet())
		{
		    Pair<Integer, Double> current = values.get(x.getFirst());
		    Pair<Integer, Double> value = san.getTimeOfDeathMap().get(x);
		    if (current == null)
		        senseResult.put(x.getFirst(), x.getSecond());
		    else if (value.getFirst() > current.getFirst() || 
		             (value.getFirst() == current.getFirst() && value.getSecond() > current.getSecond()))
		        senseResult.put(x.getFirst(), x.getSecond());
		}
		int failed = 0;
		for (RelOccurrence occ : occurrences)
		{
			occ.setRelation(getSemRel(ctx, occ, getSenses(occ, senseResult)));
			if (occ.getRelation() == null)
			{
				System.out.println("WARN: " + " could not create SemRel for occurrence: " + occ);
				failed++;
			}
			ctx.getGraph().update(occ);
		}
		System.out.println("Processed " + occurrences.size() + " occurrences with " + failed + " failures.");		
	}
	
	public void process(AnalysisContext<TextDocument> ctx)
	{
		initProlog(ctx.getGraph());
		try
		{
			Set<RelOccurrence> occurrences = ctx.find(RelOccurrence.class);
			Theory t = exporter.getPrologTheory(ctx, occurrences);
			t.append(new Theory(new Struct(new Struct("similarityThreshold", 
												      new Term[] { new alice.tuprolog.Double(similarityThreshold)}), new Struct())));			
			prolog.setTheory(t);			
			prolog.getTheoryManager().consult(new Theory(new WSDLibrary().getTheory()), true, "discoWSD");			
			san.clear();			
			prolog.solve("buildSAN.");
			applyResults(ctx, occurrences);
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	public double getSanThreshold()
	{
		return sanThreshold;
	}

	public void setSanThreshold(double sanThreshold)
	{
		this.sanThreshold = sanThreshold;
	}

	public double getSimilarityThreshold()
	{
		return similarityThreshold;
	}

	public void setSimilarityThreshold(double similarityThreshold)
	{
		this.similarityThreshold = similarityThreshold;
	}	
}
