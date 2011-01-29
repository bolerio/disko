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
import java.util.Map;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.type.HGProjection;

class CommonProjections extends HashMap<String, HGProjection>
{
	private static final long serialVersionUID = -1;
	
	static Map<String, CommonProjections> M = new HashMap<String, CommonProjections>();
	
	static CommonProjections get(final HyperGraph graph)
	{
		CommonProjections cp = M.get(graph.getLocation());
		if (cp == null)
		{
			cp = new CommonProjections();
			cp.put("localName", 
					new HGProjection()
					{

						public int[] getLayoutPath()
						{
							return null;
						}

						public String getName()
						{
							return "localName";
						}

						public HGHandle getType()
						{
							return graph.getTypeSystem().getTypeHandle(String.class);
						}

						public void inject(Object atomValue, Object value)
						{
							((OWLClass)atomValue).setLocalName((String)value);
						}

						public Object project(Object atomValue)
						{
							return ((OWLClass)atomValue).getLocalName();
						}					
					});			
			M.put(graph.getLocation(), cp);
		}
		return cp;
	}
}
