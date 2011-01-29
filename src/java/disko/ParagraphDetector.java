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
package disko;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple paragraph detector; look for blank lines separating text blocks to
 * create ParagraphAnns.
 * 
 * @author muriloq
 */
public class ParagraphDetector
{
    private static final String BLANK_LINE = "(?<=(\r\n|\r|\n))([ \\t]*$)+";
    private static final Pattern paragraphSplitter = Pattern.compile(BLANK_LINE,
                                                                     Pattern.MULTILINE);

    private static Log log = LogFactory.getLog(ParagraphDetector.class);

    public static void detectParagraphs(String s, Collection<Ann> annotations)
    {
        Matcher m = paragraphSplitter.matcher(s);
        int start = 0;
        while (m.find())
        {
            int end = m.start();
            String found = s.subSequence(start, end).toString();
            ParagraphAnn ann = new ParagraphAnn(start, end, found);
            annotations.add(ann);
            log.debug("Found ParagraphAnn " + ann);
            start = m.end();
        }
        if (annotations.isEmpty() && s.trim().length() > 0)
            annotations.add(new ParagraphAnn(0, s.length(), s));
    }
}
