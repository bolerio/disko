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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.IncidenceSetRef;
import org.hypergraphdb.LazyRef;
import org.hypergraphdb.type.HGAtomTypeBase;

public class OWLProperty extends HGAtomTypeBase
{
	Set<HGHandle> domain;
	private String localName;
	private HGHandle rangeType;
	
	
	public Object make(HGPersistentHandle handle,
					   LazyRef<HGHandle[]> targetSet, 
					   IncidenceSetRef incidenceSet)
	{
		return new OWLPropertyInstance(targetSet.deref());
	}
	
	public void release(HGPersistentHandle handle)
	{
	}
	
	public HGPersistentHandle store(Object instance)
	{
		return graph.getHandleFactory().nullHandle();
	}
	
	public Set<HGHandle> getDomain()
	{
		if (domain == null)
		{
			domain = new HashSet<HGHandle>();
			HGHandle thisHandle = graph.getHandle(this);
			List<OWLDomainLink> L = hg.getAll(graph, hg.and(hg.type(OWLDomainLink.class),
													   hg.orderedLink(thisHandle, hg.anyHandle())));
			for (OWLDomainLink link : L)
				domain.add(link.getDomainType());
		}
		return domain;
	}
	
	public String getLocalName()
	{
		return localName;
	}
	
	public void setLocalName(String localName)
	{
		this.localName = localName;
	}
	
	public HGHandle getRangeType()
	{
		return rangeType;
	}
	
	public void setRangeType(HGHandle rangeType)
	{
		this.rangeType = rangeType;
	}
	
	public String toString()
	{
		return "Property[" + localName + ":" + rangeType + "]"; 
	}
}
