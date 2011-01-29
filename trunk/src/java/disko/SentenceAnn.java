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

public class SentenceAnn extends BaseAnn
{
	private static final long serialVersionUID = 6477572068354477968L;
	
	private String sentence;

	public SentenceAnn()
	{		
	}
	
	public SentenceAnn(int start, int end)
	{
		super(start, end);
	}
	
	public SentenceAnn(int start, int end, String sentence)
	{
		super(start, end);
		this.sentence = sentence;
	}

	public String getSentence()
	{
		return sentence;
	}

	public void setSentence(String sentence)
	{
		this.sentence = sentence;
	} 
	
	public String toString(){
		return super.toString()+": "+getSentence();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((sentence == null) ? 0 : sentence.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SentenceAnn other = (SentenceAnn) obj;
		if (sentence == null) {
			if (other.sentence != null)
				return false;
		} else if (!sentence.equals(other.sentence))
			return false;
		return true;
	}
	
	
}
