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
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.Ann;
import disko.MarkupAnn;
import disko.ParagraphAnn;
import disko.TextDocument;

public class ParagraphAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    public static final String PARAGRAPH_CHANNEL = "Paragraphs";

    public static final String ATTRIBUTE_CHANNEL = "Attributes";

    private static Log log = LogFactory.getLog(ParagraphAnalyzer.class);

    private boolean saveAnnotations = false;

    /**
     * Reads a Document from the context. If this analyzer is connected to a
     * TEXT_CHANNEL, it sends the entire document text to it. If it's connected
     * to a ParagraphChannel, the analyzer sends the corresponding
     * ParagraphAnns.
     */
    public void process(AnalysisContext<TextDocument> context, Ports ports) throws InterruptedException
    {
        log.debug("Paragraph Detector started");
        InputPort<String> dbCleanSignal = ports.getInput(ScopeCleaner.SIGNAL_DB_CLEAN);
        
        if (dbCleanSignal != null) 
            dbCleanSignal.take();
        
        OutputPort<String> textOutput = ports.getOutput(SentenceAnalyzer.TEXT_CHANNEL);
        OutputPort<ParagraphAnn> paragraphOutput = ports.getOutput(ParagraphAnalyzer.PARAGRAPH_CHANNEL);
        OutputPort<MarkupAnn> attributesOutput = ports.getOutput(ParagraphAnalyzer.ATTRIBUTE_CHANNEL);

        if (ports.getInputCount() == 0 || ports.getInputCount() == 1 && dbCleanSignal != null)
        {
            log.debug("Paragraph Detector doesn't have input ports, reading document from context");
            TextDocument document = context.getDocument();

            String fullText = document.getFullText();
            if (textOutput != null)
                textOutput.put(fullText);

            for (Ann a : document.getAnnotations())
            {
                if (saveAnnotations)
                    context.add(a);
                if (a instanceof ParagraphAnn)
                {
                    if (paragraphOutput != null)
                        paragraphOutput.put((ParagraphAnn) a);
                }
                if (a instanceof MarkupAnn)
                {
                    if (attributesOutput != null)
                        attributesOutput.put((MarkupAnn) a);

                }
            }
        }
        log.debug("Paragraph Detector ended");
    }

}
