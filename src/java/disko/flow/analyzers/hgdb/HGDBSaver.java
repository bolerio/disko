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

import java.util.concurrent.Callable;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.DataFlowException;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.hypergraphdb.util.HGUtils;

import disko.AnalysisContext;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SentenceInterpretation;
import disko.data.relex.SynRel;
import disko.flow.analyzers.ParseSelectAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.ToRelOccProcessor;

public class HGDBSaver extends AbstractProcessor<AnalysisContext<TextDocument>> 
{
	private static Log log = LogFactory.getLog(HGDBSaver.class);

	public void process(final AnalysisContext<TextDocument> context, Ports ports) throws InterruptedException 
	{
	    HGHandle docHandle = context.getGraph().getHandle(context.getDocument());
	    if (docHandle == null)
	        docHandle = context.getGraph().add(context.getDocument());
	    if (context.getTopScope() == null)
	    	context.pushScoping(docHandle);
	    
		InputPort<SentenceAnn> sentenceInput = ports.getInput(SentenceAnalyzer.SENTENCE_CHANNEL);
		InputPort<SentenceInterpretation> parseInput = ports.getInput(ToRelOccProcessor.SENTENCE_INTERPRETATIONS);		
		
		if (parseInput == null)
			parseInput = ports.getInput(ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL);
		
		final int [] C = new int[] { 0 };
		
		for (SentenceAnn sentence = sentenceInput.take(); 
			!sentenceInput.isEOS(sentence); 
			sentence = sentenceInput.take())
		{
						
			final SentenceInterpretation si = parseInput.take();
/*			if (si == null)
				log.error("SI is null");
			else if (si.getParse() == null)
				log.error("SI.getParse is null");
			else if (si.getParse().getSentence() == null)
				log.error("SI.getParse().getSEntence is null");
			else if (si.getParse().getSentence().getSentence() == null)
				log.error("SI.getParse().getSEntence().getSentence() is null");
			else 
				log.debug("Sentence to save " + si.getParse().getSentence().getSentence()); */

			if (parseInput.isEOS(si)) 
				throw new DataFlowException("HGDBSave: missing parse on " + sentence.getSentence());
			else if (!si.getSentence().equals(sentence.getSentence()))
				throw new DataFlowException("HGDBSave: out of sync for sentence <<" + 
						sentence.getSentence() + ">>, received interpretation for <<" + 
						si.getSentence() + ">>");
			else if (si.getRelOccs().isEmpty())
			{
				log.warn("No parses found for sentence " + sentence.getSentence());
				continue;
			}
			
			final SentenceAnn S = sentence;			
			
			try
			{
				context.getGraph().getTransactionManager().transact(new Callable<Object>() {
					public Object call()
					{		
						context.pushScoping(context.add(S));
						try 
						{
							for (RelOccurrence occ : si.getRelOccs())
							{
								int [] positions = occ.getPositions();
								for (int i = 0; i < positions.length; i++)
									positions[i] += S.getInterval().getStart();

								//
								// Don't end RelOccurrence instances for now because 
								// they only pollute the data set and are not used for
								// anything. 
								// context.add(occ);
								
							    HGHandle [] targets = HGUtils.toHandleArray(occ);
							    HGHandle synRelHandle = hg.findOne(context.getGraph(), 
							    		hg.and(hg.type(SynRel.class), 
							    			   hg.link(targets)));
							    C[0] = C[0] + 1;
							    if (synRelHandle == null)
							        synRelHandle = context.getGraph().add(new SynRel(targets));
							    context.addInScope(synRelHandle);
							}
						} 
						finally 
						{
							context.popScope();
						}
						return null;
					}
				});
			}
			catch (Throwable t)
			{
				log.error("While saving relations for '" + sentence.getSentence() + "'", t);
			}
			finally
			{
				log.debug("Total synrel lookup: " + C[0]);
			}
		}
		log.debug("HGDB Saver ended");
		System.out.println("HGDB Saver ended");
	}
}
