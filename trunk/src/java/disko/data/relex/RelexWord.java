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

public class RelexWord
{
	private String partOfSpeech;
	private String tense;
	private String root;
	private String plurality;
	private boolean hypotetical;
	private boolean negative;
	private String name;
	
	public boolean isHypotetical()
	{
		return hypotetical;
	}
	public void setHypotetical(boolean hypotetical)
	{
		this.hypotetical = hypotetical;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public boolean isNegative()
	{
		return negative;
	}
	public void setNegative(boolean negative)
	{
		this.negative = negative;
	}
	public String getPartOfSpeech()
	{
		return partOfSpeech;
	}
	public void setPartOfSpeech(String partOfSpeech)
	{
		this.partOfSpeech = partOfSpeech;
	}
	public String getPlurality()
	{
		return plurality;
	}
	public void setPlurality(String plurality)
	{
		this.plurality = plurality;
	}
	public String getRoot()
	{
		return root;
	}
	public void setRoot(String root)
	{
		this.root = root;
	}
	public String getTense()
	{
		return tense;
	}
	public void setTense(String tense)
	{
		this.tense = tense;
	}	
}
