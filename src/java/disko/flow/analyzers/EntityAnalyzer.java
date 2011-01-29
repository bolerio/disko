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
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.SentenceAnn;
import disko.TextDocument;
import relex.corpus.EntityMaintainerFactory;
import relex.entity.EntityInfo;
import relex.entity.EntityMaintainer;

public class EntityAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    private static Log log = LogFactory.getLog(EntityAnalyzer.class);

    public static final String ENTITY_CHANNEL = "entities";
    public static final EntityMaintainer EMPTY_EM = 
        new EntityMaintainer("", new ArrayList<EntityInfo>(0));

    private EntityMaintainerFactory entityDetector;
    private String inputChannel = SentenceAnalyzer.SENTENCE_CHANNEL;
    private String outputChannel = ENTITY_CHANNEL;

    public EntityAnalyzer()
    {
        entityDetector = EntityMaintainerFactory.get();
    }

    public EntityAnalyzer(EntityMaintainerFactory entityDetector)
    {
        this.entityDetector = entityDetector;
    }

    public EntityAnalyzer(EntityMaintainerFactory entityDetector,
                          String inputChannel, 
                          String outputChannel)
    {
        this(entityDetector);
        if (inputChannel != null)
            this.inputChannel = inputChannel;
        if (outputChannel != null)
            this.outputChannel = outputChannel;
    }

    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
    {
        InputPort<SentenceAnn> inputPort = ports.getInput(inputChannel);
        OutputPort<EntityMaintainer> outputPort = ports.getOutput(outputChannel);
        for (SentenceAnn sentence = inputPort.take(); !inputPort.isEOS(sentence); sentence = inputPort.take())
        {
            log.debug("Read: '" + sentence + "'" + " on detector "
                      + entityDetector);
            EntityMaintainer entityMaintainer = entityDetector.makeEntityMaintainer(sentence.getSentence());
            if (entityMaintainer.getOriginalSentence() == null)
            {
                entityMaintainer = entityDetector.makeEntityMaintainer(sentence.getSentence());
            }
            log.debug("Writing: '" + entityMaintainer.getConvertedSentence()
                      + "'");
            if (!outputPort.put(entityMaintainer))
                break;
        }
    }

    public EntityMaintainerFactory getEntityDetector()
    {
        return entityDetector;
    }

    public void setEntityDetector(EntityMaintainerFactory entityDetector)
    {
        this.entityDetector = entityDetector;
    }

    public String getInputChannel()
    {
        return inputChannel;
    }

    public void setInputChannel(String inputChannel)
    {
        this.inputChannel = inputChannel;
    }

    public String getOutputChannel()
    {
        return outputChannel;
    }

    public void setOutputChannel(String outputChannel)
    {
        this.outputChannel = outputChannel;
    }
}
