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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.hypergraphdb.util.HGUtils;

import disko.AnalysisContext;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SentenceInterpretation;
import disko.data.relex.SynRel;


/**
 * 
 * <p>
 * This implements a simple search based on a sentence and its most likely parse. The inputs
 * are the sentence channel and parse selection channel. It outputs <code>HGHandle</code>s of
 * documents in the search result. The sentence parse is taken as a set of RelOccurrence. First
 * all documents containing any of the relations in the input sentence are found. Then they
 * are scored based on how many of those relations they contain. Unary relations have less
 * wait then binary relations. Also, relations with lower overall count (the same counts that
 * are used in the parse ranking are reused here) add less to the score.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class DiskoSearchAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>> 
{
	private static Log log = LogFactory.getLog(DiskoSearchAnalyzer.class);

	public static final String SEARCH_RESULTS = "SEARCH_RESULTS";


	public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
	{
		log.info("Starting DiscoSearchAnalyzer");
		InputPort<SentenceAnn> sentenceInput = ports.getInput(SentenceAnalyzer.SENTENCE_CHANNEL);
		InputPort<SentenceInterpretation> parseInput = ports.getInput(ToRelOccProcessor.SENTENCE_INTERPRETATIONS);
		OutputPort<HGHandle> out = ports.getOutput(SEARCH_RESULTS);
		
		for (SentenceAnn sentence = sentenceInput.take(); 
			 !sentenceInput.isEOS(sentence); 
			 sentence = sentenceInput.take())
		{
			final SentenceInterpretation si = parseInput.take();
			
			for (RelOccurrence occ : si.getRelOccs())
			{
				HGHandle [] targets = HGUtils.toHandleArray(occ);
			    HGHandle synRelHandle = hg.findOne(ctx.getGraph(), 
			    								hg.and(hg.type(SynRel.class), hg.link(targets)));
				if (synRelHandle!=null)
					out.put(synRelHandle);
			}
		}
	}
}
