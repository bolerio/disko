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

import java.util.Iterator;

/**
 * <p>
 * Implements a trie structure for storing arbitrary strings where each
 * string carries a certain weight. The structure is used for on-the-fly
 * suggestions for common user input. The more frequent input is given
 * bigger weight and it's returned first.
 * </p>
 * 
 * @author boris
 *
 */
public class WeightedTrie implements Iterable<String>
{
	public void add(String text, double weight)
	{
		
	}
	
	public void remove(String text)
	{
		
	}
	
	public Iterator<String> iterator()
	{
		return null;
	}
}
