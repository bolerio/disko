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

import java.util.UUID;

import org.hypergraphdb.annotation.HGIgnore;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.linkgrammar.LGConfig;

import disko.AnalysisContext;
import disko.DU;
import disko.TextDocument;
import disko.data.relex.RelexParse;

import relex.ParsedSentence;
import relex.Sentence;
import relex.entity.EntityMaintainer;
import relex.feature.FeatureNode;
import relex.parser.LGParser;
import relex.parser.LocalLGParser;
import relex.parser.ParseException;
import relex.parser.RemoteLGParser;

public class LinkGrammarProcessor extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    public static final String PARSED_SENTENCE_CHANNEL = "LG_SENTENCE";
    public static final RelexParse EOS_PARSE = new RelexParse();
    
    @HGIgnore
    private LGParser parser;
    private LGConfig config;
    private String host = null;
    private int port = 0;
    
    private void initParser()
    {
        // We recreate the parser even there's one already created because
        // configuration might have changed in between runs.
        if (host == null)
            parser = new LocalLGParser();
        else
        {
            RemoteLGParser rp = new RemoteLGParser();
            rp.getLinkGrammarClient().setHostname(host);
            rp.getLinkGrammarClient().setPort(port);
            rp.getLinkGrammarClient().getConfig().setMaxLinkages(5);
            parser = rp;
        }
        parser.setConfig(getConfig());
    }
    
    public LinkGrammarProcessor()
    {        
    }
    
    public LinkGrammarProcessor(LGConfig config)
    {
        this.config = config;
    }
    
    public LinkGrammarProcessor(LGConfig config, String host, int port)
    {
        this(config);
        this.host = host;
        this.port = port;
    }
    
    public void process(AnalysisContext<TextDocument> ctx, Ports ports)
            throws InterruptedException
    {
        initParser();        
        final InputPort<EntityMaintainer> in = ports.getInput(EntityAnalyzer.ENTITY_CHANNEL);
        final OutputPort<RelexParse> out = ports.getOutput(PARSED_SENTENCE_CHANNEL);
        UUID sentenceId = null;
        for (EntityMaintainer em : in)
        {
            String convertedSentence = em.getConvertedSentence().replace('\n', ' ').replace('\r', ' ');            
            Sentence sentence = null;
            try
            {
                sentence = parser.parse(convertedSentence);
            }
            catch (ParseException ex)
            {
                DU.log.error("While parsing sentence '" + em.getOriginalSentence() + "'", ex);
                sentence = new Sentence();
                sentence.setSentence(convertedSentence); 
            }
            sentenceId = UUID.randomUUID();
            if (sentence == null || sentence.getParses().isEmpty())
            {
                DU.log.info("No parses for " + em.getOriginalSentence());                
                // Put a dummy parse just to sync up with the EntityMaintainer upstream.
                RelexParse rp = new RelexParse();
                rp.setEntityMaintainer(em);
                rp.setSentenceId(sentenceId);  
                FeatureNode meta = new FeatureNode();
                meta.set("num_skipped_words", 
                         new FeatureNode(Integer.toString(convertedSentence.split("\\s+").length)));
                meta.set("and_cost", new FeatureNode(Integer.toString(Integer.MAX_VALUE)));
                meta.set("disjunct_cost", new FeatureNode(Integer.toString(Integer.MAX_VALUE)));
                meta.set("link_cost", new FeatureNode(Integer.toString(Integer.MAX_VALUE)));
                meta.set("num_violations", new FeatureNode(Integer.toString(0)));
                rp.setMetaData(meta);                          
                out.put(rp);
            }
            else for (ParsedSentence parse : sentence.getParses())
            {
                em.prepareSentence(parse.getLeft());
                RelexParse rp = RelexParse.make(parse);
                rp.setEntityMaintainer(em);
                rp.setSentenceId(sentenceId);
                out.put(rp);
            }
            if (!out.isOpen())
                break;
        }
    }

    public LGConfig getConfig()
    {
        if (config == null)
            config = new LGConfig();
        return config;
    }

    public void setConfig(LGConfig config)
    {
        this.config = config;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }    
}
