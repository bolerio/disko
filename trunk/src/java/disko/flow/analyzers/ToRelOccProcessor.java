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

import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.DataFlowException;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.hypergraphdb.util.HGUtils;

import disko.AnalysisContext;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.RelexParse;
import disko.data.relex.SentenceInterpretation;
import disko.relex.ParseToRelations;

import relex.entity.EntityMaintainer;

public class ToRelOccProcessor extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    public static final String SENTENCE_INTERPRETATIONS = "SENTENCE_INTERPRETATIONS";
    public static final SentenceInterpretation EOS_SENTENCE_INTERPRETATION = 
        new SentenceInterpretation(null, null);
    
    private Set<String> ignoredFeatures = new HashSet<String>();
    
    public ToRelOccProcessor()
    {
        ignoredFeatures.add("inflection");
        ignoredFeatures.add("punctuation");
    }
    
    public void process(AnalysisContext<TextDocument> ctx, Ports ports)
            throws InterruptedException
    {
        InputPort<EntityMaintainer>        entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        InputPort<RelexParse>              parseInput = ports.getInput(ParseSelectProcessor.SELECTED_PARSE_CHANNEL);
        OutputPort<SentenceInterpretation> out = ports.getOutput(SENTENCE_INTERPRETATIONS);
        
        ParseToRelations parseToRelations = new ParseToRelations(ctx.getGraph());
        
        for (EntityMaintainer em : entityInput)
        {
            RelexParse parse = parseInput.take();
            if (parseInput.isEOS(parse))
                throw new DataFlowException("EntityMaintainer and Selected Parse channels are out of sync. - missing parse.");
            else if (parse == null)
                throw new DataFlowException("Parse is null.");
            if (!HGUtils.eq(parse.getEntityMaintainer(),em))
                throw new DataFlowException("EntityMaintainer and Selected Parse channels are out of sync.");
            Set<RelOccurrence> relations = parseToRelations.getRelations(ctx, 
                                                                         em, 
                                                                         parse.toParsedSentence(), 
                                                                         null,
                                                                         ignoredFeatures);
            out.put(new SentenceInterpretation(parse.getEntityMaintainer().getOriginalSentence(), relations));
        }
    }

    public Set<String> getIgnoredFeatures()
    {
        return ignoredFeatures;
    }

    public void setIgnoredFeatures(Set<String> ignoredFeatures)
    {
        this.ignoredFeatures = ignoredFeatures;
    }    
}
