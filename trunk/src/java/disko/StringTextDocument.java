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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.hypergraphdb.annotation.HGIgnore;

import disko.utils.CharSequenceReader;

import relex.corpus.TextInterval;

public class StringTextDocument implements TextDocument
{
    @HGIgnore
    private List<Ann> annotations = null;
    private String string;

    public StringTextDocument()
    {
    }

    public String getString()
    {
        return string;
    }

    public void setString(String string)
    {
        this.string = string;
    }

    public StringTextDocument(String string)
    {
        this.string = string;
    }

    public Reader createReader() throws IOException
    {
        return new CharSequenceReader(string);
    }

    public String getFullText()
    {
        return string;
    }

    public CharSequence getText(TextInterval interval)
    {
        return string.subSequence(interval.getStart(), interval.getEnd());
    }

    public List<Ann> getAnnotations()
    {
        if (annotations == null)
        {
            annotations = new ArrayList<Ann>();
            ParagraphDetector.detectParagraphs(string, this.annotations);
        }
        return annotations;
    }
    
    public String toString()
    {
        int max = Math.min(getFullText().length(), 60);
        String s = getFullText().substring(0, max);
        if (getFullText().length() > max)
            s += "...";
        return s;
    }
}
