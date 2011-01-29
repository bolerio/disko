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

public class ParagraphAnn extends BaseAnn
{
    private static final long serialVersionUID = -5576127475742461696L;

    private String paragraph;

    public ParagraphAnn()
    {
    }

    public ParagraphAnn(int start, int end)
    {
        super(start, end);
    }

    public ParagraphAnn(int start, int end, String paragraph)
    {
        super(start, end);
        this.paragraph = paragraph;
    }

    public String getParagraph()
    {
        return paragraph;
    }

    public void setParagraph(String paragraph)
    {
        this.paragraph = paragraph;
    }

    public String toString()
    {
        int max = Math.min(getParagraph().length(), 50);
        String s = getParagraph().substring(0, max);
        if (getParagraph().length() > max)
            s += "...";
        return super.toString() + ": " + s;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                 + ((paragraph == null) ? 0 : paragraph.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ParagraphAnn other = (ParagraphAnn) obj;
        if (paragraph == null)
        {
            if (other.paragraph != null)
                return false;
        }
        else if (!paragraph.equals(other.paragraph))
            return false;
        return true;
    }

}
