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
package disko.relex;

import java.util.HashSet;
import java.util.Set;

import relex.morphy.Morphed;
import relex.morphy.Morphy;
import relex.morphy.MorphyJWNL;
import relex.feature.FeatureNode;

public class TestingMorphy extends MorphyJWNL
{
	Morphy defaultMorphy;
	Morphy hgdbMorphy;
	
	HashSet<String> mismatches = new HashSet<String>();
	
	boolean comparePOS(FeatureNode n1, FeatureNode n2)
	{
		if (n1 == null)
			return n2 == null;
		else if (n2 == null)
			return false;
		String root1 = n1.get(Morphy.ROOT_F).getValue();
		String root2 = n2.get(Morphy.ROOT_F).getValue();
		if (!root1.equals(root2))
			return false;
		if (n1.get(Morphy.NEG_F) != null)
			return n2.get(Morphy.NEG_F) != null && n1.get(Morphy.NEG_F).getValue().equals(n2.get(Morphy.NEG_F).getValue());
		else
			return true;
	}
	
	boolean compare(Morphed m1, Morphed m2)
	{
		return comparePOS(m1.getNoun(), m2.getNoun()) &&
			   comparePOS(m1.getVerb(), m2.getVerb()) &&
			   comparePOS(m1.getAdj(), m2.getAdj()) &&
			   comparePOS(m1.getAdv(), m2.getAdv());
	}
	
	public Morphed morph(String word)
	{
		Morphed m1 = defaultMorphy.morph(word);
		Morphed m2 = hgdbMorphy.morph(word);
		if (!compare(m1, m2))
			mismatches.add(word);
		return m1;
	}
	
	public void initialize()
	{
		defaultMorphy = new MorphyJWNL();
		defaultMorphy.initialize();
		hgdbMorphy = new MorphyHGDB();
		hgdbMorphy.initialize();
	}
	
	public Set<String> getMismatches()
	{
		return mismatches;
	}
}
