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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.DU;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.RelationCount;
import disko.data.relex.SentenceInterpretation;
import disko.flow.analyzers.hgdb.RelationCountFactory;

import relex.entity.EntityMaintainer;

/**
 * 
 * <p>
 * Take input from the parser, rank all parses from the sentence and produce the
 * SentenceInterpretation with the highest rank.
 * </p>
 * 
 * @author Borislav Iordanov, Murilo Queiroz
 * 
 */
public class ParseSelectAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
//    private static Log log = LogFactory.getLog(ParseSelectAnalyzer.class);

    public static final String SELECTED_PARSE_CHANNEL = "SELECTED_PARSE_CHANNEL";

    private transient HyperGraph graph;

    public ParseSelectAnalyzer()
    {
        String graphLocation = System.getProperty("hgdb.relation.stats");
        if (!DU.isEmpty(graphLocation))
        {
            graph = HGEnvironment.get(graphLocation);
            RelationCountFactory.loadCounts(graph);
        }
    }

    public ParseSelectAnalyzer(HyperGraph graph)
    {
        this.graph = graph;
        RelationCountFactory.loadCounts(graph);
    }

    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
    {
        final HyperGraph graph = this.graph != null ? this.graph
                                                   : ctx.getGraph();

        RelationCountFactory.createCountingIndices(graph);

        InputPort<EntityMaintainer> entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        InputPort<Set<SentenceInterpretation>> sentenceInput = ports.getInput(ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS);
        OutputPort<SentenceInterpretation> out = ports.getOutput(SELECTED_PARSE_CHANNEL);

        for (Set<SentenceInterpretation> iset = sentenceInput.take(); !sentenceInput.isEOS(iset); iset = sentenceInput.take())
        {
            EntityMaintainer em = entityInput.take();
            if (entityInput.isEOS(em))
                break;

            HashMap<String, String> entityTypes = RelationCountFactory.getEntityTypes(em);

            TreeMap<Double, SentenceInterpretation> ranked = new TreeMap<Double, SentenceInterpretation>();
            for (SentenceInterpretation i : iset)
            {
                ArrayList<RelationCount> relationCounts = new ArrayList<RelationCount>();
                for (RelOccurrence occ : i.getRelOccs())
                {
                	if (occ.getArity() < 3)
                		continue;
                    List<String> components = occ.getComponents(ctx.getGraph());
                    relationCounts.add(RelationCountFactory.getRelationCount(entityTypes, 
                                                                             components, 
                                                                             occ.getPositions()));
                }
//                    RelationCountFactory.getRelationCounts(entityTypes, i.getParse());

                double score = computeScores(graph, relationCounts);
                ranked.put(score, i);
            }
            SentenceInterpretation best = ranked.get(ranked.lastKey());            
            out.put(best);
        }
        out.close();
    }

    private double computeScores(final HyperGraph graph,
                                 ArrayList<RelationCount> relationCounts)
    {
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
                                                     new RelationCount(ANY,
                                                                       ANY,
                                                                       ANY,
                                                                       ANY, ANY));
            double P01 = RelationCountFactory.getCount(graph,
                                                       new RelationCount(pred,
                                                                         arg0,
                                                                         arg1,
                                                                         pos0,
                                                                         pos1));
            double PXX = RelationCountFactory.getCount(graph,
                                                       new RelationCount(pred,
                                                                         ANY,
                                                                         ANY,
                                                                         pos0,
                                                                         pos1));
            double PX1 = RelationCountFactory.getCount(graph,
                                                       new RelationCount(pred,
                                                                         ANY,
                                                                         arg1,
                                                                         pos0,
                                                                         pos1));
            double P0X = RelationCountFactory.getCount(graph,
                                                       new RelationCount(pred,
                                                                         arg0,
                                                                         ANY,
                                                                         pos0,
                                                                         pos1));
            double X0X = RelationCountFactory.getCount(graph,
                                                       new RelationCount(ANY,
                                                                         arg0,
                                                                         ANY,
                                                                         pos0,
                                                                         ANY));
            double XX1 = RelationCountFactory.getCount(graph,
                                                       new RelationCount(ANY,
                                                                         ANY,
                                                                         arg1,
                                                                         ANY,
                                                                         pos1));
            ;

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
