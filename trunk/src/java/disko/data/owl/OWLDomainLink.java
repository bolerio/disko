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
package disko.data.owl;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;

/**
 * 
 * <p>
 * An <code>OWLDomainLink</code> states the fact that a property (an <code>OWLProperty</code>), the 1st target,
 * has in its domain a given <code>OWLClass</code> , the second target.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class OWLDomainLink extends HGPlainLink
{
	public OWLDomainLink(HGHandle...targets)
	{
		super(targets);
		if (targets.length != 2)
			throw new IllegalArgumentException("An OWLDomainLink must be of the form [PropertyType, DomainType].");
	}
	
	public HGHandle getPropertyType()
	{
		return getTargetAt(0);
	}
	
	public HGHandle getDomainType()
	{
		return getTargetAt(1);
	}
}
