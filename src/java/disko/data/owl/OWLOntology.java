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

/**
 * 
 * <p>
 * Represents a single OWL ontology identify by a URL namespace;
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class OWLOntology
{
	private String namespaceUrl;

	public OWLOntology()
	{		
	}
	
	public OWLOntology(String namespaceUrl)
	{
		this.namespaceUrl = namespaceUrl;
	}
	
	public String getNamespaceUrl()
	{
		return namespaceUrl;
	}

	public void setNamespaceUrl(String namespaceUrl)
	{
		this.namespaceUrl = namespaceUrl;
	}
}
