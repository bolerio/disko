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

import java.util.Iterator;

import java.util.Map;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.IncidenceSetRef;
import org.hypergraphdb.LazyRef;
import org.hypergraphdb.handle.UUIDHandleFactory;
import org.hypergraphdb.type.HGAtomType;
import org.hypergraphdb.type.HGAtomTypeBase;
import org.hypergraphdb.type.HGCompositeType;
import org.hypergraphdb.type.HGProjection;

public class OWLClassConstructor extends HGAtomTypeBase implements HGCompositeType
{
	public static HGPersistentHandle OWL_CLASS_CONSTRUCTOR_HANDLE = 
		UUIDHandleFactory.I.makeHandle("5af303dd-16c2-4478-ad27-62aefac01a6d");

	private Map<String, HGProjection> projections = null;
	
	private Map<String, HGProjection> projections()
	{
		if (projections == null)
			projections = CommonProjections.get(graph);
		return projections;
	}
	
	public Object make(HGPersistentHandle handle,
					   LazyRef<HGHandle[]> targetSet, 
					   IncidenceSetRef incidenceSet)
	{
		OWLClass result = new OWLClass();
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
		return stringType.store(((OWLClass)instance).getLocalName());
	}

	public Iterator<String> getDimensionNames()
	{
		return projections.keySet().iterator();
	}

	public HGProjection getProjection(String dimensionName)
	{
		return projections().get(dimensionName);
	}	
}
