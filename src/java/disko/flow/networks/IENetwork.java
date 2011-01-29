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
package disko.flow.networks;

import java.util.HashMap;

import java.util.Map;

import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.PredefinedNetwork;

import disko.AnalysisContext;
import disko.ParagraphAnn;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.RelexParse;
import disko.data.relex.SentenceInterpretation;
import disko.flow.analyzers.CombinedEntityAnalyzer;
import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.LinkGrammarProcessor;
import disko.flow.analyzers.ParagraphAnalyzer;
import disko.flow.analyzers.ParseSelectProcessor;
import disko.flow.analyzers.RelexProcessor;
import disko.flow.analyzers.ScopeCleaner;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.ToRelOccProcessor;
import disko.flow.analyzers.hgdb.HGDBSaver;
import disko.relex.OpenNLPEntityMaintainerFactory;

import relex.corpus.GateEntityDetector;
import relex.entity.EntityMaintainer;

public class IENetwork extends PredefinedNetwork<AnalysisContext<TextDocument>>
{    
    private LinkGrammarProcessor lg = null;
    private boolean useOpenNlpEntityDetection = false;
    
    @Override
    public void create()
    {
        addChannel(new Channel<String>(
                ScopeCleaner.SIGNAL_DB_CLEAN, 
                new String(""), 30));
        
        addChannel(new Channel<ParagraphAnn>(
                ParagraphAnalyzer.PARAGRAPH_CHANNEL, 
                new ParagraphAnn(0, 0), 30));
        
        addChannel(new Channel<SentenceAnn>(
                SentenceAnalyzer.SENTENCE_CHANNEL, 
                new SentenceAnn(0, 0), 30));
        
        addChannel(new Channel<EntityMaintainer>(
                EntityAnalyzer.ENTITY_CHANNEL, 
                EntityAnalyzer.EMPTY_EM, 30));
        
        if (useOpenNlpEntityDetection)
        {
    		addChannel(new Channel<EntityMaintainer>(
    				CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL, 
    				EntityAnalyzer.EMPTY_EM ));
    		addChannel(new Channel<EntityMaintainer>(
    				CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL, 
    				EntityAnalyzer.EMPTY_EM ));        	
        }
        
        addChannel(new Channel<RelexParse>(
                LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL, 
                LinkGrammarProcessor.EOS_PARSE, 30));

        addChannel(new Channel<RelexParse>(
                RelexProcessor.RELEX_ANNOTATED_CHANNEL, 
                LinkGrammarProcessor.EOS_PARSE, 30));
        
        addChannel(new Channel<RelexParse>(
                ParseSelectProcessor.SELECTED_PARSE_CHANNEL, 
                LinkGrammarProcessor.EOS_PARSE, 30));
        
        addChannel(new Channel<SentenceInterpretation>(
                ToRelOccProcessor.SENTENCE_INTERPRETATIONS, 
                ToRelOccProcessor.EOS_SENTENCE_INTERPRETATION, 30));      

        addNode(new ScopeCleaner(), 
                new String[] { },
                new String[] { ScopeCleaner.SIGNAL_DB_CLEAN });
        
        addNode(new ParagraphAnalyzer(), 
                new String[] { ScopeCleaner.SIGNAL_DB_CLEAN},
                new String[] { ParagraphAnalyzer.PARAGRAPH_CHANNEL });
        
        addNode(new SentenceAnalyzer(), 
                new String[] { ParagraphAnalyzer.PARAGRAPH_CHANNEL },
                new String[] { SentenceAnalyzer.SENTENCE_CHANNEL });

        if (useOpenNlpEntityDetection)
        {
    		GateEntityDetector gateDetector = new GateEntityDetector();
    		Map<Object, Object> gazParams = new HashMap<Object, Object>();
    		gazParams.put("caseSensitive", Boolean.TRUE);
    		gazParams.put("gazetteerFeatureSeparator", "&");
    		gateDetector.getAnnieParams().put("gate.creole.gazetteer.DefaultGazetteer", gazParams);
        	
    		addNode(new EntityAnalyzer(gateDetector, null, CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL), 
    				new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
    				new String[]{CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL});
    		
    		addNode(new EntityAnalyzer(new OpenNLPEntityMaintainerFactory(), null, CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL), 
    				new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
    				new String[]{CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL});

    		addNode(new CombinedEntityAnalyzer(), 
    				new String[]{CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL, CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL}, 
    				new String[]{EntityAnalyzer.ENTITY_CHANNEL});        	
        }
        else
	        addNode(new EntityAnalyzer(), 
	                new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
	                new String[]{EntityAnalyzer.ENTITY_CHANNEL});
        
        addNode(lg = new LinkGrammarProcessor(),
                new String[] { EntityAnalyzer.ENTITY_CHANNEL },
                new String[] { LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL});
        
        addNode(new RelexProcessor(),
                new String[] { LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL},
                new String[] { RelexProcessor.RELEX_ANNOTATED_CHANNEL });
        
        addNode(new ParseSelectProcessor(),
                new String[] { EntityAnalyzer.ENTITY_CHANNEL, RelexProcessor.RELEX_ANNOTATED_CHANNEL },
                new String[] { ParseSelectProcessor.SELECTED_PARSE_CHANNEL });
        
        addNode(new ToRelOccProcessor(),
                new String[] { EntityAnalyzer.ENTITY_CHANNEL, ParseSelectProcessor.SELECTED_PARSE_CHANNEL },
                new String[] { ToRelOccProcessor.SENTENCE_INTERPRETATIONS });
        
        addNode(new HGDBSaver(),
                new String[] { SentenceAnalyzer.SENTENCE_CHANNEL, ToRelOccProcessor.SENTENCE_INTERPRETATIONS },
                new String[] { });
    }

    public LinkGrammarProcessor getLinkGrammarProcessor()
    {
        return lg;
    }

	public boolean isUseOpenNlpEntityDetection()
	{
		return useOpenNlpEntityDetection;
	}

	public void setUseOpenNlpEntityDetection(boolean useOpenNlpEntityDetection)
	{
		this.useOpenNlpEntityDetection = useOpenNlpEntityDetection;
	}    
}
