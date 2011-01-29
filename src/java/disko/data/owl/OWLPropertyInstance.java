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
 * An <code>OWLPropertyInstance</code> is represented as a <code>HGLink</code> between
 * an object (the first target) and a property value (the second target). The property
 * value is going to be an atom of either some primitive type, or another OWL object/individual. 
 * </p>
 *
 * <p>
 * The type of a property instance is simply represented as a HG type, i.e.
 * an <code>OWLProperty</code>. 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class OWLPropertyInstance extends HGPlainLink
{
	public OWLPropertyInstance(HGHandle...targets)
	{
		super(targets);
		if (targets.length != 2)
			throw new IllegalArgumentException("An OWLPropertyInstance link must have the form [ObjectHandle, PropertyValueHandle].");
	}
	
	public HGHandle getObject()
	{
		return getTargetAt(0);
	}
	
	public HGHandle getValue()
	{
		return getTargetAt(1);
	}
}
