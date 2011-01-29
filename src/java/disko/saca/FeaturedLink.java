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
package disko.saca;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;

public class FeaturedLink extends HGPlainLink
{
	private double weight = Double.POSITIVE_INFINITY;

	public FeaturedLink(HGHandle...targets)
	{
		super(targets);
	}

	public FeaturedLink(double weight, HGHandle...targets)
	{
		super(targets);
		this.weight = weight;
	}
	
	public HGHandle getResource()
	{
		return getTargetAt(0);
	}
	
	public HGHandle getSearchTrigger()
	{
		if (getArity() > 1)
			return getTargetAt(1);
		else
			return null;
	}

	public double getWeight()
	{
		return weight;
	}

	public void setWeight(double weight)
	{
		this.weight = weight;
	}
}
