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
package disko.flow.analyzers;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.DataFlowException;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.hypergraphdb.app.wordnet.data.Word;
import org.hypergraphdb.indexing.ByPartIndexer;

import disko.AnalysisContext;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SentenceInterpretation;
import disko.relex.ParseToRelations;

import relex.ParsedSentence;
import relex.concurrent.RelexTaskResult;
import relex.entity.EntityMaintainer;

public class ToRelOccAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
	private static Log log = LogFactory.getLog(ToRelOccAnalyzer.class);
	
	public static final String SENTENCE_INTERPRETATIONS = "SENTENCE_INTERPRETATIONS";
	
	public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
	{
		HGHandle typeH = ctx.getGraph().getTypeSystem().getTypeHandle(Word.class);
		ctx.getGraph().getIndexManager().register(new ByPartIndexer(typeH, new String[] { "lemma" }));
			
		InputPort<RelexTaskResult> parseInput =ports.getInput(FullRelexAnalyzer.PARSE_CHANNEL);
		InputPort<EntityMaintainer> entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
		OutputPort<Set<SentenceInterpretation>> out = ports.getOutput(SENTENCE_INTERPRETATIONS);
		
		ParseToRelations parseToRelations = new ParseToRelations(ctx.getGraph());
				
		long totalTime = System.currentTimeMillis();
		int processedSentences = 0;
				
		for (RelexTaskResult relexResult = parseInput.take(); 
		     !parseInput.isEOS(relexResult); 
		     relexResult = parseInput.take()) 
		{
		    EntityMaintainer em = entityInput.take();
		    
			if (entityInput.isEOS(em))
				throw new DataFlowException(
						"Parse channel and entity channel out of sync: no entity maintainer for " + 
						relexResult.sentence);		                                    
			Set<SentenceInterpretation> interpretations = new HashSet<SentenceInterpretation>();
			int numParses = 0;
			for (ParsedSentence parse : relexResult.result.getParses())
			{
				try
				{
					long parseTime = System.currentTimeMillis();
					Set<RelOccurrence> relations = parseToRelations.getRelations(ctx, em, parse, null);
					interpretations.add(new SentenceInterpretation(em.getOriginalSentence(), 
					                                               relations));
					log.debug("Elapsed time on ParseToRelations.getRelations(...) : "+(System.currentTimeMillis() - parseTime)+" ms for "+relations.size()+" relations of parse #"+numParses+" , sentence #"+relexResult.index+": "+relexResult.sentence);
				}
				catch (RuntimeException t)
				{
					log.error("Problem translating sentence " + 
					          em.getOriginalSentence() + 
					          " to RelOccurrences. Current document=" + ctx.getDocument(), t);
				}
				numParses++;
			}
			if (interpretations.isEmpty())
			{
				ParsedSentence ps = new ParsedSentence("");
				ps.setSentence(relexResult.result);
				interpretations.add(new SentenceInterpretation(em.getOriginalSentence(), 
				                                               new HashSet<RelOccurrence>()));
			}
			if (!out.put(interpretations))
			    break;
			processedSentences++;			            
		}		
		log.debug("Elapsed time on ToRelOccAnalyzer: "+(System.currentTimeMillis() - totalTime)+" ms for "+processedSentences+" sentences");
	}
}
