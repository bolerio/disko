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
package test.disco.relex;

import relex.ParsedSentence;
import relex.feature.PrologList;
import relex.output.RawView;


public class TestUtils
{
	public static String getHeadAndBackground(ParsedSentence parse)
	{
		relex.feature.FeatureNode headSet = new relex.feature.FeatureNode();
		headSet.set("head", parse.getLeft().get("head"));
		headSet.set("background", parse.getLeft().get("background"));
	 	String testCase = new PrologList().toPrologList(headSet, RawView.getZHeadsFilter(), false);
	 	return testCase;
	}
}
