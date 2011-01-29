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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.IncidenceSetRef;
import org.hypergraphdb.LazyRef;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.atom.HGSubsumes;
import org.hypergraphdb.type.HGAtomType;
import org.hypergraphdb.type.HGAtomTypeBase;
import org.hypergraphdb.type.HGCompositeType;
import org.hypergraphdb.type.HGProjection;

public class OWLClass extends HGAtomTypeBase implements HGCompositeType
{
	String localName = null;
	Map<String, OWLProperty> properties = null;
		
	private Map<String, HGProjection> projections = null;
	
	private Map<String, HGProjection> projections()
	{
		if (projections == null)
			projections = CommonProjections.get(graph);
		return projections;
	}	
	void fetchProperties(HyperGraph graph)
	{
		properties = new HashMap<String, OWLProperty>();
		HGHandle thisHandle = graph.getHandle(this);
		List<HGHandle> supers = hg.findAll(graph, 
							hg.bfs(thisHandle, 
							       hg.type(HGSubsumes.class), null, true, false));
		supers.add(thisHandle);
		for (HGHandle clHandle : supers)
		{
			List<OWLDomainLink> L = hg.getAll(graph, hg.and(hg.type(OWLDomainLink.class),
													   	    hg.orderedLink(hg.anyHandle(), clHandle)));
			for (OWLDomainLink link : L)
			{
				OWLProperty p = graph.get(link.getPropertyType());
				properties.put(p.getLocalName(), p);
			}
		}
	}
	
	public Object make(HGPersistentHandle handle,
					   LazyRef<HGHandle[]> targetSet, 
					   IncidenceSetRef incidenceSet)
	{
		OWLIndividual result = new OWLIndividual(this);
		HGAtomType stringType = graph.getTypeSystem().getAtomType(String.class);
		result.setLocalName((String)stringType.make(handle, null, null));
		return result;
	}

	public void release(HGPersistentHandle handle)
	{
		HGAtomType stringType = graph.getTypeSystem().getAtomType(String.class);
		stringType.release(handle);
	}

	public HGPersistentHandle store(Object instance)
	{
		HGAtomType stringType = graph.getTypeSystem().getAtomType(String.class);
		return stringType.store(((OWLIndividual)instance).getLocalName());		
	}
	
	public Map<String, OWLProperty> getProperties()
	{
		if (properties == null)
			fetchProperties(graph);
		return properties;		
	}

	public String getLocalName()
	{
		return localName;
	}

	public void setLocalName(String localName)
	{
		this.localName = localName;
	}	
	
	public Iterator<String> getDimensionNames()
	{
		return projections.keySet().iterator();
	}

	public HGProjection getProjection(String dimensionName)
	{
		return projections().get(dimensionName);
	}
	
	public String toString()
	{
		return "OWLClass(" + localName + ")";
	}
}
