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
package disko.utils;

import java.util.Comparator;

import disko.Ann;

public class AnnComparator implements Comparator<Ann>
{
	private static AnnComparator instance = new AnnComparator();
	
	public static AnnComparator getInstance() { return instance; }
	
	public int compare(Ann x, Ann y)
	{
		return x.getInterval().getStart() - y.getInterval().getStart();
	}
}
