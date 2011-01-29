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

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;

/**
 * 
 * <p>
 * A link establishing a scoping relationships between two atoms. The first
 * target is the atom that represents the scope (e.g. in an hierarchical 
 * organization) and the second represents the object being scoped. 
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class ScopeLink extends HGPlainLink
{
	public ScopeLink(HGHandle [] targets)
	{
		super(targets);
	}
	
	public ScopeLink(HGHandle scoping, HGHandle scoped)
	{
		super(new HGHandle[] { scoping, scoped});
	}
	
	public HGHandle getScoping()
	{
		return getTargetAt(0);
	}
	
	public HGHandle getScoped()
	{
		return getTargetAt(1);
	}
}
