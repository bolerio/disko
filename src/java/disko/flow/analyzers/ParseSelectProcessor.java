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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.DataFlowException;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.hypergraphdb.util.HGUtils;

import disko.AnalysisContext;
import disko.DU;
import disko.TextDocument;
import disko.data.relex.RelationCount;
import disko.data.relex.RelexParse;
import disko.flow.analyzers.hgdb.RelationCountFactory;

import relex.entity.EntityMaintainer;

public class ParseSelectProcessor extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    public static final String SELECTED_PARSE_CHANNEL = "SELECTED_PARSE_CHANNEL";
   
    public void process(AnalysisContext<TextDocument> ctx, Ports ports)
            throws InterruptedException
    {
        InputPort<EntityMaintainer> entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        InputPort<RelexParse> parseInput = ports.getInput(RelexProcessor.RELEX_ANNOTATED_CHANNEL);       
        OutputPort<RelexParse> out = ports.getOutput(SELECTED_PARSE_CHANNEL);
        
        RelationCountFactory.createCountingIndices(ctx.getGraph());
        
        RelexParse parse = parseInput.take();
        RelexParse best = parse;
        double lastScore = 0.0;
        DU.log.debug("Got parse " + parse.getSentenceId() + " -- " + parse.getEntityMaintainer().getOriginalSentence());
        
        for (EntityMaintainer em : entityInput)
        {
            DU.log.debug("Got EM " + em.getOriginalSentence());
            
            HashMap<String, String> entityTypes = RelationCountFactory.getEntityTypes(em);
            
            if (parseInput.isEOS(parse))
                throw new DataFlowException("EntityMaintainer and Sentence Parse channels are out of sync, " +
                         "no parse for sentence ---" + em.getOriginalSentence() + "---");                
            if (!HGUtils.eq(parse.getEntityMaintainer(), em))
                throw new DataFlowException("EntityMaintainer and Sentence Parse channels are out of sync.");
            
            UUID currentSentence = parse.getSentenceId();
            
            while (currentSentence.equals(parse.getSentenceId()))
            {
                double score = computeScores(ctx.getGraph(), 
                                             RelationCountFactory.getRelationCounts(entityTypes, 
                                                                                    parse));
                double linkageFactor = 1.0;
                if (parse.getNumSkippedWords() > 0)
                    linkageFactor *= (double)parse.getNumSkippedWords(); 
                if (score * linkageFactor > lastScore)
                	best = parse;
                lastScore = score * linkageFactor;
                
                parse = parseInput.take();
                
                DU.log.debug("Got parse " + parse.getSentenceId() + " -- " + parse.getEntityMaintainer().getOriginalSentence());                
            }            
            if (!out.put(best))
                break;
            best = parse; // first parse of next sentence
            lastScore = 0.0;
        }
        
        if (!parseInput.isEOS(parse))
            throw new DataFlowException("EntityMaintainer and Sentence Parse channels are out of sync.");            
    }
    
    private double computeScores(final HyperGraph graph,
                                 List<RelationCount> relationCounts)
    {
        if (relationCounts.isEmpty())
            return 0;
        double score = 0;
        for (RelationCount r : relationCounts)
        {
            String pred = r.getPredicate();
            String arg0 = r.getArg0();
            String arg1 = r.getArg1();
            String pos0 = r.getPos0();
            String pos1 = r.getPos1();
            final String ANY = RelationCountFactory.ANY;
            double T = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(ANY, ANY, ANY, ANY, ANY));
            double P01 = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(pred, arg0, arg1, pos0, pos1));
            double PXX = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(pred, ANY, ANY, pos0, pos1));
            double PX1 = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(pred, ANY, arg1, pos0, pos1));
            double P0X = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(pred, arg0, ANY, pos0, pos1));
            double X0X = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(ANY, arg0, ANY, pos0, ANY));
            double XX1 = RelationCountFactory.getCount(
                 graph,
                 new RelationCount(ANY, ANY, arg1, ANY, pos1));
            if (T == 0)
                T = 1;
            if (P01 == 0)
                P01 = 1;
            if (PXX == 0)
                PXX = 1;
            if (PX1 == 0)
                PX1 = 1;
            if (P0X == 0)
                P0X = 1;
            if (X0X == 0)
                X0X = T;
            if (XX1 == 0)
                XX1 = T;

            double FR = P01 / T;
            double FP = PXX / T;
            double F0 = P0X / X0X;
            double F1 = PX1 / XX1;

            double s = Math.log(FR) + Math.log(FP) + Math.log(F0)
                       + Math.log(F1);

            score += s;
        }
               
        score /= relationCounts.size();

        return score;
    }    
}
