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

import org.hypergraphdb.HGGraphHolder;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;
import org.hypergraphdb.HyperGraph;

public class SynRel extends HGPlainLink implements HGGraphHolder
{
    private HyperGraph graph;
    
    public SynRel(HGHandle[] targets)
    {
        super(targets);
    }

    public void setHyperGraph(HyperGraph graph)
    {
        this.graph = graph;
    }

    public HGHandle getHead()
    {
        return getTargetAt(0);
    }

    public HGHandle getFirst()
    {
        return getTargetAt(1);
    }

    public HGHandle getSecond()
    {
        return getTargetAt(2);
    }

    public String toString()
    {
        return toString(graph);
    }
    
    public String toString(HyperGraph graph)
    {
        StringBuffer sb = new StringBuffer();
        HGHandle head = getHead();
        Object object = graph.get(head);
        String id = object == null ? "null" : object.toString();
        String relationName = id; // SyntacticPredicate.idFriendlyToRelex(id);
        sb.append(relationName).append("(");
        for (int i = 1; i < getArity(); i++)
        {
            HGHandle targetAt = getTargetAt(i);
            Object object2 = graph.get(targetAt);
            sb.append(object2 == null ? "null" : object2.toString());
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
    public ArrayList<String> getComponents(HyperGraph graph)
    {
        ArrayList<String> components = new ArrayList<String>();
        for (int i = 0; i < getArity(); i++)
        {
            HGHandle targetAt = getTargetAt(i);
            Object object = (Object) graph.get(targetAt);
            String name = object.toString() + "";
            components.add(name);
        }
        return components;
    }

    public HGHandle[] getElements()
    {
        HGHandle[] elements = new HGHandle[getArity()];
        for (int i = 1; i < getArity(); i++)
        {
            elements[i] = getTargetAt(i);
        }
        return elements;
    }

    public boolean equals(Object o)
    {
        if (o instanceof SemRel == false)
            return false;
        SemRel that = (SemRel) o;
        return Arrays.equals(this.getElements(), that.getElements());
    }

    public int hashCode()
    {
        return Arrays.hashCode(this.getElements());
    }
}
