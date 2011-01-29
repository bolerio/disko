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

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.DataFlowException;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.TextDocument;
import disko.data.relex.SentenceInterpretation;
import disko.relex.ParseToRelations;

import relex.ParsedSentence;
import relex.Sentence;
import relex.anaphora.Antecedents;
import relex.anaphora.Hobbs;
import relex.entity.EntityMaintainer;

public class AnaphoraAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    protected static Log log = LogFactory.getLog(AnaphoraAnalyzer.class);
    public static final String ANAPHORA_CHANNEL = "ANAPHORA_CHANNEL";

    public AnaphoraAnalyzer()
    {
    }

    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
    {
        Antecedents antecedents = new Antecedents();
        Hobbs hobbs = new Hobbs(antecedents);
        ParseToRelations parseToRelations = new ParseToRelations(ctx.getGraph());

        InputPort<SentenceInterpretation> inputPort = ports.getInput(ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL);
        InputPort<EntityMaintainer> entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        OutputPort<SentenceInterpretation> outputPort = ports.getOutput(ANAPHORA_CHANNEL);

        if (1==1)
        throw new UnsupportedOperationException("AnaphoraAnalyzer needs a re-write as ParsedSentence instance is not available anymore.");
        
        for (SentenceInterpretation si = inputPort.take(); 
             !inputPort.isEOS(si); 
             si = inputPort.take())
        {
            EntityMaintainer em = entityInput.take();
            if (entityInput.isEOS(em))
                throw new DataFlowException(
                                            "AnaphoraAnalyzer: missing entity maintainer on "
                                                    + si.getSentence());

//            if (si.getRelOccs().isEmpty()) // no parses found
                outputPort.put(si);
//            else
//            {
//                ArrayList<ParsedSentence> L = new ArrayList<ParsedSentence>();
//                L.add(si.getParse());
//                Sentence relexInfo = new Sentence(si.getSentence(), L);
//                hobbs.addParse(relexInfo);
//                hobbs.resolve(relexInfo);
//                parseToRelations.addAnaphoraRelations(ctx,
//                                                      em,
//                                                      si.getParse(),
//                                                      si.getRelOccs(),
//                                                      antecedents.getAntecedents());
//                outputPort.put(si);
//            }
        }
        antecedents.clear();
        outputPort.close();
    }
}
