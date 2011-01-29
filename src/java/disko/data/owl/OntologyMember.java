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
 * A link representing membership in an ontology: [OWLOntology handle, OWL entity handle]. That is,
 * the first target is the OWLOntology instance and the second is the thing that's part of it (
 * a class, individual or whatever)
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class OntologyMember extends HGPlainLink
{
	public OntologyMember(HGHandle...targets)
	{
		super(targets);
	}
	
	public HGHandle getOntology() { return getTargetAt(0); }
	public HGHandle getMember() { return getTargetAt(1); }
}
