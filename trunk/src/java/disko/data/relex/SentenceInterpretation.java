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
package disko.data.relex;

import java.util.Set;

public class SentenceInterpretation
{
    private String sentence;
	private Set<RelOccurrence> relOccs = null;
	
	public SentenceInterpretation()
	{
	    
	}
	
	public SentenceInterpretation(String sentence, Set<RelOccurrence> occSet)
	{
	    this.sentence = sentence;
		this.relOccs = occSet;
	}
	
	public String getSentence()
    {
        return sentence;
    }

    public void setSentence(String sentence)
    {
        this.sentence = sentence;
    }

    public Set<RelOccurrence> getRelOccs()
	{
		return relOccs;
	}

	public void setRelOccs(Set<RelOccurrence> relOccs)
	{
		this.relOccs = relOccs;
	}	
	
	public int hashCode()
	{
		int hash = 0;
		if (relOccs != null)
			hash += relOccs.size();
		return hash;
	}
	
	public boolean equals(Object x)
	{
		if (! (x instanceof SentenceInterpretation))
			return false;
		SentenceInterpretation s = (SentenceInterpretation)x;
		if (sentence == null)
		    return s.sentence == null;
		else if (!sentence.equals(s.sentence))
		    return false;
		else if (s.relOccs == null)
		    return relOccs == null;
		else
		    return s.relOccs.equals(relOccs);
	}
}
