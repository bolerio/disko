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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.ParagraphAnn;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.taca.OpenNLPSentenceDetector;

import relex.corpus.DocSplitter;
import relex.corpus.TextInterval;

public class SentenceAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    private static Log log = LogFactory.getLog(SentenceAnalyzer.class);

    public static final String TEXT_CHANNEL = "texts";
    public static final String SENTENCE_CHANNEL = "sentences";

    private transient DocSplitter splitter;

    public SentenceAnalyzer()
    {
        splitter = new OpenNLPSentenceDetector();
    }

    public void initialize()
    {
    }

    /**
     * Identifies sentences in the input and emit SentenceAnns to the output
     * channel. The input is determined by channel type; if there's no input
     * channels the analyzer reads the document in the context.
     */
    public void process(AnalysisContext<TextDocument> context, Ports ports) throws InterruptedException
    {
        log.debug("Sentence Detector started");
        
        if (ports.getInputCount() == 0)
        {
            log.debug("Sentence Detector doesn't have input ports, reading document from context");
            String text = context.getDocument().getFullText();
            processText(context, ports, text, 0);
        }
        else
        {
            InputPort<String> textInput = ports.getInput(TEXT_CHANNEL);
            if (textInput != null)
            {
                for (String text = textInput.take(); !textInput.isEOS(text); text = textInput.take())
                {
                    if (!processText(context, ports, text, 0))
                        break;
                }
            }
            InputPort<ParagraphAnn> paragraphInput = ports.getInput(ParagraphAnalyzer.PARAGRAPH_CHANNEL);
            if (paragraphInput != null)
            {
                for (ParagraphAnn paragraph = paragraphInput.take(); 
                     !paragraphInput.isEOS(paragraph); 
                     paragraph = paragraphInput.take())
                {
                    if (!processText(context, 
                                     ports, 
                                     paragraph.getParagraph(),
                                     paragraph.getInterval().getStart()))
                        break;
                }
            }
        }
        log.debug("Sentence Detector ended");
    }

    /**
     * Return true if output was written and false otherwise 
     */
    private boolean processText(AnalysisContext<TextDocument> context,
                             Ports ports, String text, int offset) throws InterruptedException
    {
        /*
         * String substring = text.substring(0, Math.min(100, text.length()));
         * if (!GoogleLanguageDetector.isEnglish(substring)) {
         * log.warn("Ignoring non-English text: "+substring+"..."); return; }
         */
        OutputPort<SentenceAnn> sentenceOutput = ports.getOutput(SENTENCE_CHANNEL);
        List<TextInterval> lst = splitter.process(text);
        for (TextInterval ivl : lst)
        {
        	 // When fed garbage, the splitter will sometimes produces garbage.
        	if (ivl.getStart() < 0 || ivl.getEnd() < 0) 
        		continue;
            String trimmedSentence = text.substring(ivl.getStart(),
                                                    ivl.getEnd());
            trimmedSentence = trimmedSentence.replaceAll("\n", " ").trim();
            if (trimmedSentence.length() == 0)
            	continue;
            SentenceAnn ann = new SentenceAnn(ivl.getStart() + offset,
                                              ivl.getEnd() + offset,
                                              trimmedSentence);
            log.debug("Writing: " + ann);
            if (!sentenceOutput.put(ann))
                return false;
        }
        return true;
    }
}
