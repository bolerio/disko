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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;

public class OWLIndividual
{
	OWLClass owlClass;
	String localName;
	Map<String, Object> properties = null;

	void fetchProperties(HyperGraph graph)
	{
		properties = new HashMap<String, Object>();
		HGHandle thisHandle = graph.getHandle(this);
		for (OWLProperty propType : owlClass.getProperties().values())
		{
			List<OWLPropertyInstance> L = hg.getAll(graph, 
					hg.and(hg.type(graph.getHandle(propType)), 
						   hg.incident(thisHandle),
						   hg.orderedLink(thisHandle, hg.anyHandle())));
			if (L.size() == 1)
				properties.put(propType.getLocalName(), graph.get(L.get(0).getValue()));
			else if (L.size() > 1)
			{
				Set<Object> set = new HashSet<Object>();
				for (OWLPropertyInstance p : L)
					set.add(graph.get(p.getValue()));
				properties.put(propType.getLocalName(), set);
			}
		}
	}
	
	public OWLIndividual(OWLClass owlClass)
	{
		this.owlClass = owlClass;
	}
	
	public Map<String, Object> getProperties()
	{
		if (properties == null)
			fetchProperties(owlClass.getHyperGraph());
		return properties;
	}
	
	public OWLClass getOwlClass()
	{
		return owlClass;
	}

	public String getLocalName()
	{
		return localName;
	}

	public void setLocalName(String localName)
	{
		this.localName = localName;
	}
}
