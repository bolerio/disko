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
package disko.data;

import org.hypergraphdb.util.HGUtils;

public class NamedEntity
{
	private String name;
	private String type;
	
	public NamedEntity()
	{		
	}
	
	public NamedEntity(String name, String type)
	{
		this.name = name;		
		this.type = type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String toString()
	{
		return name + ":" + type;
	}

	public boolean equals(Object o)
	{
		if (o instanceof NamedEntity == false) return false;
		NamedEntity that = (NamedEntity) o;
		return HGUtils.eq(name, that.name) && HGUtils.eq(type, that.type);
	}
}
