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
package disko.data.relex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.annotation.AtomReference;
import org.hypergraphdb.util.HGUtils;

/**
 * 
 * <p>
 * A <code>RelOccurrence</code> represents a plain syntactic relation between
 * words. Syntactic relations are either one of the predefined in the
 * <code>SyntacticPredicate</code> class or regular English preposition and
 * particles that convey relational information between words in a sentence.
 * </p>
 * 
 * <p>
 * The link's arity depends on the arity of the relation. The first target is
 * always the relation itself and the rest of the targets are its arguments in
 * order.
 * </p>
 * 
 * <p>
 * Each <code>RelOccurrence</code> can be seen as an instance of a more
 * general semantic relation which is a relation between meanings of words
 * rather than the word tokens. Semantic relation are inferred only after proper
 * word sense disambiguation is performed. The semantic relation of which a
 * given <code>RelOccurrence</code> is an instance is represented by the
 * <code>relation SemRel</code> property of the occurrence. When the
 * <code>relation</code> property is null, this simply means that the actual
 * senses of the words in the occurrence have not been assigned.
 * </p>
 * 
 * <p>
 * The position in the original text of each argument in a
 * <code>RelOccurrence</code> is stored in a <code>int[]</code> positions
 * array. The head of the relation doesn't have a position even when it's a word
 * coming from the text. Position information is used to distinguish different
 * occurrence of the same word.
 * </p>
 * 
 * @author Borislav Iordanov
 * 
 */
public class RelOccurrence extends HGPlainLink
{
	@AtomReference("symbolic")
	private SemRel relation = null;
	private int[] positions;

	// public RelOccurrence()
	// {
	// }
	//
	public RelOccurrence(HGHandle[] targetSet)
	{
		super(targetSet);
	}

	public RelOccurrence(HGHandle[] targetSet, int... position)
	{
		super(targetSet);
		this.positions = position;
	}

	// public RelOccurrence(SemRel relation, HGHandle [] targetSet)
	// {
	// super(targetSet);
	// this.relation = relation;
	// }
	//
	// public RelOccurrence(SemRel relation, HGHandle [] targetSet,
	// int...position)
	// {
	// super(targetSet);
	// this.relation = relation;
	// this.positions = position;
	// }
	//	
	// public RelOccurrence(SemRel relation, int...position)
	// {
	// this.relation = relation;
	// this.positions = position;
	// }

	public void setRelation(SemRel relation)
	{
		this.relation = relation;
	}

	public SemRel getRelation()
	{
		return relation;
	}

	public void setPositions(int[] positions)
	{
		this.positions = positions;
	}

	public int[] getPositions()
	{
		return positions;
	}

	public int getFirstPosition()
	{
		return positions[0];
	}

	public int getSecondPosition()
	{
		return positions[1];
	}

	public int getPosition(int component)
	{
		return positions[component];
	}

	public List<String> getComponents(HyperGraph graph)
	{	    
		ArrayList<String> components = new ArrayList<String>();
		for (int i = 0; i < getArity(); i++) 
		{
			HGHandle targetAt = getTargetAt(i);
			Object object = (Object) graph.get(targetAt);
			String name = "" + object.toString();
			components.add(name);
		}
		return components;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer(getTargetAt(0) + "(");
		for (int i = 0; i < positions.length; i++)
		{
			sb.append(getTargetAt(i + 1) + ":" + positions[i]);
			if (i < positions.length - 1)
				sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(positions);
		result = prime * result
				+ ((relation == null) ? 0 : relation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RelOccurrence other = (RelOccurrence) obj;
		if (getArity() != other.getArity())
			return false;
		if (!HGUtils.eq(relation, other.relation))
			return false;
		if (!Arrays.equals(positions, other.positions))
			return false;
		for (int i = 0; i < getArity(); i++)
			if (!getTargetAt(i).equals(other.getTargetAt(i)))
				return false;
		return true;
	}

	public static SemRel toDummySemRel(RelOccurrence r)
	{
		final int arity = r.getArity();
		HGHandle[] targets = new HGHandle[arity];
		for (int i = 0; i < arity; i++)
		{
			targets[i] = r.getTargetAt(i);
		}
		return new SemRel(targets);
	}
}
