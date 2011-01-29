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
import org.hypergraphdb.atom.HGAtomRef;

import relex.corpus.TextInterval;

/**
 * 
 * <p>
 * An annotation that tags a piece of input with an arbitrary HyperGraph atom.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class HGAtomAnn extends BaseAnn
{
	private static final long serialVersionUID = 2472064394708637074L;
	
	private HGAtomRef ref;
	
	public HGAtomAnn()
	{		
	}
	
	public HGAtomAnn(HGHandle atom, int start, int end)
	{
		super(start, end);
		this.ref = new HGAtomRef(atom, HGAtomRef.Mode.hard);
	}

	public HGAtomAnn(HGHandle atom, TextInterval I)
	{
		super(I);
		this.ref = new HGAtomRef(atom, HGAtomRef.Mode.hard);
	}
	
	public HGHandle getAtom()
	{
		return ref.getReferent();
	}
	
	public void setAtom(HGHandle reference)
	{
		ref = new HGAtomRef(reference, HGAtomRef.Mode.hard);
	}
	
	public String toString(){
		return super.toString()+": ref("+ ref.toString() +")";
	}
}
