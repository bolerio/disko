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

/**
 * 
 * <p>
 * Represents a semantic relationship between things. The things are
 * called the "components" of the relationship and are usually represented
 * as entries in some lexical database (WordNet), but could be anything
 * that can be assigned a lexical/syntactic role in text. The <em>head</code>
 * of the relationship is the predicate, and it comes either from a lexical
 * database as well or it represents some syntactic predicate. 
 * </p>
 * 
 * <p>
 * There are no constraints on the arity of the relationship except that it be
 * > 0. Thus unary, binary, ternary etc. relationships are supported. 
 * </p>
 * 
 * <p>
 * A semantic relationships is an abstract entity and it is not itself associated
 * with a particular piece of text. It is <em>occurrences</code> of semantic
 * relationships that are associated/scoped with pieces of text. Each semantic 
 * relationship has occurrences in the analyzed corpus, recorded as instances of
 * <code>RelOccurrence</code>.
 * </p>
 *
 * <p>
 * In essence, a semantic relationship can be thought of as a type and occurrences
 * as its instances. It must be noted that the head of a relationship defines its
 * type in a way, but it is not represented as a hypergraph type. 
 * </p>
 * 
 * <p>
 * The arguments of a <code>SemRel</code>, that is - its target set, generally consists
 * of word senses (synsets in WordNet) while the head is not endowed with a sense because
 * it is not expected to exhibit any sort of ambiguity: it'll always be a preposition or
 * grammatical relation that has a well-understood meaning. 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class SemRel extends HGPlainLink
{
	public SemRel(HGHandle [] targets)
	{
		super(targets);
	}
	
	public HGHandle getHead() { return getTargetAt(0); }
	public HGHandle getFirst() { return getTargetAt(1); }
	public HGHandle getSecond() { return getTargetAt(2); }
	
	public String toString(HyperGraph graph)
	{
		StringBuffer sb = new StringBuffer();
		HGHandle head = getHead();
		Object object = graph.get(head);
		String id = object.toString();
		String relationName = SyntacticPredicate.idFriendlyToRelex(id);
		sb.append(relationName).append("(");
		for (int i = 1; i < getArity(); i++)
		{
			HGHandle targetAt = getTargetAt(i);
			Object object2 = graph.get(targetAt);
			sb.append(object2.toString());
			if (i < getArity() - 1) 
				sb.append(", ");
		}
		sb.append(")");
		String rel = sb.toString();
		return rel;
	}
	
	/** 
	 * @param graph
	 * @return a list with the string representation of the relation components.
	 */
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
	
	public HGHandle[] getElements() 
	{
		HGHandle[] elements = new HGHandle[getArity()];
		for (int i = 1; i < getArity(); i++)
			elements[i]=getTargetAt(i);
		return elements;
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof SemRel == false) return false;
		SemRel that = (SemRel) o; 
		return Arrays.equals(this.getElements(), that.getElements());
	}
	
	public int hashCode()
	{
		return Arrays.hashCode(this.getElements());
	}
}
