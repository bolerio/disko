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

import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.DU;
import disko.TextDocument;
import disko.data.relex.RelexParse;
import relex.ParsedSentence;
import relex.algs.SentenceAlgorithmApplier;
import relex.concurrent.RelexContext;
import relex.morphy.Morphy;
import relex.morphy.MorphyFactory;

/**
 * <p>
 * The processor applies Relex's algorithms to a single parse coming from Link Grammar.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class RelexProcessor extends AbstractProcessor<AnalysisContext<TextDocument>>
{    
    public static final String RELEX_ANNOTATED_CHANNEL  = "RELEX_SENTENCE";
    
    private static Morphy morphy = null;
    private static SentenceAlgorithmApplier sentenceAlgorithmApplier;
    
    /**
     * <p>Return a single instance of the Morphy algorithm held in this class
     * because Relex doesn't maintain it as a singleton.
     */
    public static synchronized Morphy getMorphy()
    {
        if (morphy == null)
            morphy = MorphyFactory.getImplementation();
        return morphy;
    }
    
    public static synchronized SentenceAlgorithmApplier getAlgorithmApplier()
    {
        if (sentenceAlgorithmApplier == null)
            sentenceAlgorithmApplier = new SentenceAlgorithmApplier();
        return sentenceAlgorithmApplier;
    }
    
    public void process(AnalysisContext<TextDocument> ctx, Ports ports)
            throws InterruptedException
    {
//        InputPort<EntityMaintainer> entityInput = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL); 
        InputPort<RelexParse> parseInput = ports.getInput(LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL);
        OutputPort<RelexParse> out = ports.getOutput(RELEX_ANNOTATED_CHANNEL);
        RelexContext context = new RelexContext();
        context.setMorphy(getMorphy());
        SentenceAlgorithmApplier algos = getAlgorithmApplier();
        
        //
        // RelexParse contained only the 'originalSentence' so we needed to get the entity maintainer
        // from its channel. However, this pauses problems to load balancing (synchronizing 1 datum
        // from the entities channel with multiple parses from the LG and RELEX channels) that we haven't
        // solved yet. So we moved the entityMaintainer in the RelexParse object...
        
//        RelexParse sen = parseInput.take();
//
//        for (EntityMaintainer em : entityInput)
//        {
//            if (parseInput.isEOS(sen))
//                throw new DataFlowException("EntityMaintainer and LG_SENTENCE channels are out of sync, " +
//                         "no parse for sentence ---" + em.getOriginalSentence() + "---");                
//            if (!HGUtils.eq(sen.getEntityMaintainer(), em.getOriginalSentence()))
//                throw new DataFlowException("EntityMaintainer and LG_SENTENCE channels are out of sync - sentences differ.");
//            
//            UUID currentSentence = sen.getSentenceId();
//            
//            while (currentSentence.equals(sen.getSentenceId()))
//            {
//                if (sen.getLeafConstituents().isEmpty())
//                    DU.log.warn("No leaf constituents for " + sen.getEntityMaintainer().getOriginalSentence());
//                else
//                {
//                    ParsedSentence parse = sen.toParsedSentence();     
//                    em.prepareSentence(parse.getLeft());
//                    algos.applyAlgs(parse, context);
//                    em.repairSentence(parse.getLeft());
//                }
//                
//                if (!out.put(sen))
//                    break;
//                sen = parseInput.take();                
//            }            
//            if (!out.isOpen())
//                break;
//        }

        
        for (RelexParse sen : parseInput)
        {
            if (sen.getLeafConstituents().isEmpty())
                DU.log.warn("No leaf constituents for " + sen.getEntityMaintainer().getOriginalSentence());
            else
            {
                ParsedSentence parse = sen.toParsedSentence();
                sen.getEntityMaintainer().prepareSentence(parse.getLeft());
                algos.applyAlgs(parse, context);
                sen.getEntityMaintainer().repairSentence(parse.getLeft());
            }
            if (!out.put(sen))
                break;
        }
    }
}
