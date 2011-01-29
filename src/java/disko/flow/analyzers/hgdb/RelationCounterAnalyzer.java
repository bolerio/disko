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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.DU;
import disko.DefaultTextDocument;
import disko.TextDocument;
import disko.data.relex.RelationCount;
import disko.data.relex.SyntacticPredicate;
import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.FullRelexAnalyzer;
import disko.flow.networks.RelationCounterNetwork;

import relex.ParsedSentence;
import relex.concurrent.RelexTaskResult;
import relex.entity.EntityMaintainer;

public class RelationCounterAnalyzer<T> extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    private static Log log = LogFactory.getLog(RelationCounterAnalyzer.class);

    private transient HyperGraph graph = null;

    public RelationCounterAnalyzer()
    {
        String graphLocation = System.getProperty("hgdb.relation.stats");
        if (!DU.isEmpty(graphLocation))
            graph = HGEnvironment.get(graphLocation);
    }

    public void process(AnalysisContext<TextDocument> context, Ports ports) throws InterruptedException
    {
        final HyperGraph graph = this.graph != null ? 
                                 this.graph : context.getGraph();

        RelationCountFactory.createCountingIndices(graph);

        InputPort<EntityMaintainer> entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        InputPort<RelexTaskResult> parseInput = ports.getInput(FullRelexAnalyzer.PARSE_CHANNEL);

        for (RelexTaskResult parses = parseInput.take(); !parseInput.isEOS(parses); parses = parseInput.take())
        {

            EntityMaintainer em = entityInput.take();
            if (entityInput.isEOS(em))
                break;

            final HashMap<String, String> entityTypes = RelationCountFactory.getEntityTypes(em);

            log.debug("Counting relations for all parses for: "
                      + em.getOriginalSentence());

            final RelexTaskResult currentParses = parses;
            //
            // We encapsulate the processing of a single in a HGDB transaction.
            // This gives a considerable
            // performance boost because when there is no current transaction in
            // effect, HGDB will create
            // a transaction for every single query. For a sentence with, say,
            // 20 parses of about 20 relations
            // each this yields 2800 transactions (opening and committing a
            // transaction is a costly operation).
            //
            try
            {
                log.info("Saving parses for " + em.getOriginalSentence());
                long startTime = System.currentTimeMillis();
                graph.getTransactionManager().transact(new Callable<Object>()
                {
                    public Object call()
                    {
                        for (ParsedSentence parsedSentence : currentParses.result.getParses())
                        {
                            log.debug(parsedSentence);

                            ArrayList<RelationCount> relationCounts = 
                                RelationCountFactory.getRelationCounts(entityTypes,
                                                                       parsedSentence);
                            HashMap<HGHandle, RelationCount> counts = new HashMap<HGHandle, RelationCount>();
                            for (RelationCount r : relationCounts)
                                incrementCounts(graph,
                                                counts,
                                                r,
                                                currentParses.result.getParses().size());
                            for (RelationCount r : counts.values())
                                graph.update(r);
                        }
                        return null;
                    }
                });
                log.info("Parses saved, total time elapsed="
                         + (System.currentTimeMillis() - startTime) / 1000.0);
            }
            catch (Throwable t)
            {
                log.error("While storing counts for "
                          + em.getOriginalSentence(), t);
            }
            log.debug("Relation count completed.");
        }
        log.debug("RelationCounterProcessor ended");
    }

    private void incrementCounts(HyperGraph graph,
                                 Map<HGHandle, RelationCount> counts,
                                 RelationCount r, int numParses)
    {
        String pred = r.getPredicate();
        String arg0 = r.getArg0();
        String arg1 = r.getArg1();
        String pos0 = r.getPos0();
        String pos1 = r.getPos1();
        final String ANY = RelationCountFactory.ANY;
        incrementCount(graph, counts,
                       new RelationCount(ANY, ANY, ANY, ANY, ANY), numParses);
        incrementCount(graph, counts, new RelationCount(pred, arg0, arg1, pos0,
                                                        pos1), numParses);
        incrementCount(graph, counts, new RelationCount(pred, ANY, arg1, pos0,
                                                        pos1), numParses);
        incrementCount(graph, counts, new RelationCount(pred, arg0, ANY, pos0,
                                                        pos1), numParses);
        incrementCount(graph, counts, new RelationCount(pred, ANY, ANY, pos0,
                                                        pos1), numParses);
        incrementCount(graph, counts, new RelationCount(ANY, arg0, ANY, pos0,
                                                        ANY), numParses);
        incrementCount(graph, counts, new RelationCount(ANY, ANY, arg1, ANY,
                                                        pos1), numParses);
    }

    public static void incrementCount(final HyperGraph graph,
                                      final Map<HGHandle, RelationCount> counts,
                                      final RelationCount r, final int numParses)
    {
        HGHandle found = RelationCountFactory.find(graph, r);
        if (found == null)
        {
            r.setCount(1.0 / numParses);
            graph.getTransactionManager().transact(new Callable<Object>()
            {
                public Object call()
                {
                    counts.put(graph.add(r), r);
                    return null;
                }
            });
            log.debug("Added " + r + " to HGDB, handle " + found);
            return;
        }
        else
        {
            RelationCount existingCount = counts.get(found);
            if (existingCount == null)
            {
                existingCount = graph.get(found);
                counts.put(found, existingCount);
            }
            existingCount.setCount(existingCount.getCount() + 1.0 / numParses);
            // graph.update(existingCount);
            log.debug("Incremented " + existingCount);
        }
    }
    
    public static void main(String[] args) throws InterruptedException,
                                          ExecutionException
    {

        // Document
        DefaultTextDocument doc = new DefaultTextDocument(new File("test/trivial-corpus.txt"));

        // HGDB
        HyperGraph graph = HGEnvironment.get("../hgdb_counts");
        SyntacticPredicate.loadAll(graph);

        // Context
        AnalysisContext<TextDocument> ctx = new AnalysisContext<TextDocument>(graph,
                                                                              doc);
        ctx.pushScoping(graph.add(doc));

        // Network
        DataFlowNetwork<AnalysisContext<TextDocument>> network = 
            new RelationCounterNetwork(ctx);

        network.start().get();

        graph.close();

        network.shutdown();
        log.debug("Network shutdown");
    }
}
