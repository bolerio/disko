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

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.DocumentAnn;
import disko.ParagraphAnn;
import disko.TextDocument;


public class RegexpAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    private static Log log = LogFactory.getLog(RegexpAnalyzer.class);

    private boolean saveAnnotations = false;
    private TreeMap<String, Pattern> patterns = new TreeMap<String, Pattern>();

    public static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");

    public static final String ADDRESS = "address";
    public static final Pattern DEFAULT_ADDRESS_PATTERN = Pattern.compile(
          "((PO|P\\.O|P\\.O\\.|po|p\\.o|p\\.o\\.)? *(box|BOX|Box)|"
                  + "[0-9]+)[0-9A-Za-z\\-\\. #,]{3,50}[, \\*]+"
                  + "(A[LKSZRAP]|C[AOT]|D[EC]|F[LM]|G[AU]|HI|I[ADLN]|"
                  + "K[SY]|LA|M[ADEHINOPST]|N[CDEHJMVY]|O[HKR]|P[ARW]|"
                  + "RI|S[CD]|T[NX]|UT|V[AIT]|W[AIVY]) "
                  + "[0-9]{5}(-?[0-9]{4})?[ ,\\.$\\r]",
          Pattern.MULTILINE);

    public static final String WORK_HOURS = "work_hours";
    public static final Pattern DEFAULT_WORK_HOURS_PATTERN = Pattern.compile(
         "(from)? +[0-2 ]{0,1}[0-9]:[0-9][0-9] "
                 + "+(am|pm|a.m|p.m|a.m.|p.m.) *"
                 + "(to|-) *+[0-2 ]{0,1}[0-9]:[0-9][0-9] "
                 + "+(am|pm|a.m|p.m|a.m.|p.m.)",
         Pattern.CASE_INSENSITIVE);

    public RegexpAnalyzer()
    {
        addDefaultPatterns();
    }

    public RegexpAnalyzer(boolean saveAnnotations)
    {
        this();
        this.saveAnnotations = saveAnnotations;
    }

    public void addDefaultPatterns()
    {
        patterns.put(ADDRESS, DEFAULT_ADDRESS_PATTERN);
        patterns.put(WORK_HOURS, DEFAULT_WORK_HOURS_PATTERN);
    }

    public void addPattern(String tag, Pattern pattern)
    {
        patterns.put(tag, pattern);
    }

    public TreeMap<String, Pattern> getPatterns()
    {
        return patterns;
    }

    public void setPatterns(TreeMap<String, Pattern> patterns)
    {
        this.patterns = patterns;
    }

    public void process(AnalysisContext<TextDocument> context, Ports ports) throws InterruptedException
    {
        log.debug("Regexp Analyzer started");

        if (ports.getInputCount() == 0)
        {
            log.debug("RegexpAnalyzer doesn't have input ports, reading document from context");
            String text = context.getDocument().getFullText();
            processText(context, ports, text, 0);
        }
        else
        {
            InputPort<ParagraphAnn> paragraphInput = ports.getInput(ParagraphAnalyzer.PARAGRAPH_CHANNEL);
            if (paragraphInput != null)
            {
                log.debug("RegexpAnalyzer reading from PARAGRAPH_CHANNEL");
                for (ParagraphAnn paragraph = paragraphInput.take(); !paragraphInput.isEOS(paragraph); paragraph = paragraphInput.take())
                {
                    processText(context, ports, paragraph.getParagraph(),
                                paragraph.getInterval().getStart());
                }
            }
            else
            {
                log.debug("RegexpAnalyzer reading from TEXT_CHANNEL");
                InputPort<String> textInput = ports.getInput(SentenceAnalyzer.TEXT_CHANNEL);
                String text = textInput.take();
                processText(context, ports, text, 0);
            }
        }

        log.debug("RegExp Analyzer ended");
    }

    private void processText(AnalysisContext<TextDocument> context,
                             Ports ports, String docText, int offset)
    {
        String cleanText = cleanText(docText);
        for (Map.Entry<String, Pattern> e : patterns.entrySet())
        {
            String tag = e.getKey();
            Pattern p = e.getValue();
            Matcher m = p.matcher(cleanText.replaceAll("[\\n\\r]", " "));
            log.debug("Looking for " + tag);
            while (m.find())
            {
                final String found = m.group();
                final int start = m.start() + offset;
                final int end = m.end() + offset;
                DocumentAnn ann = new DocumentAnn(start, end, tag);
                log.debug("Found " + ann + ": " + found);
                if (saveAnnotations)
                {
                    context.add(ann);
                }
            }
        }
    }

    /** Replaces HTML tags with same number of whitespaces */
    private String cleanText(String docText)
    {
        StringBuffer cleanText = new StringBuffer();
        Matcher tagMatcher = TAG_PATTERN.matcher(docText);
        while (tagMatcher.find())
        {
            String found = tagMatcher.group();
            tagMatcher.appendReplacement(cleanText,
                                         getWhiteSpace(found.length()));
        }
        return cleanText.toString();
    }

    /** Returns a string of white spaces of the given length */
    private String getWhiteSpace(int length)
    {
        // TODO optimize whitespace generation
        StringBuilder whitespace = new StringBuilder();
        for (int i = 0; i < length; i++)
            whitespace.append(" ");
        String replacement = whitespace.toString();
        return replacement;
    }
}
