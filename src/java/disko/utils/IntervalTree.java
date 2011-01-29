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
package disko.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.hypergraphdb.util.ArrayBasedSet;
import org.hypergraphdb.util.Pair;

import relex.corpus.TextInterval;

/**
 * 
 * <p>
 * Maintain a tree of nested <code>TextInterval</code>s. Each interval has a value associated
 * with it. That value may be null. The intended use is for strictly nested intervals as in
 * well-formed XML/HTML document. However, this  class will automatically split any newly
 * added interval that overlaps with previously added ones and would thus break the strict
 * nesting. When such splitting is performed, the resulting "pieces" accumulate the values
 * of all their original intervals in a <code>java.util.List</code>. 
 * </p>
 *
 * @author Borislav Iordanov
 *
 * @param <T> The type of the values associated with text intervals.
 */
@SuppressWarnings("unchecked")
public class IntervalTree<T>
{
	static class Node<T>
	{
		ArrayList<T> values = new ArrayList<T>();
		TextInterval I;
		ArrayBasedSet<Node<T>> children = new ArrayBasedSet<Node<T>>(new Node[0], 
				new Comparator<Node<T>>()
				{
					public int compare(Node<T> left, Node<T> right)
					{
						return left.I.getStart() - right.I.getStart();
					}
				}
		);
		public Node(TextInterval I)
		{
			this.I = I;
		}
		
		public Node(TextInterval I, T value) 
		{ 
			this.I = I;			
			values.add(value); 
		}
	}
	
	private Node<T> root;
	
	private Node<T> findParent(Node<T> n, TextInterval I)
	{
		if (n.I.getStart() > I.getStart() || n.I.getEnd() < I.getEnd())
			return null;
		Node<T> next = n;
		do
		{
			n = next;
			for (Node<T> child : n.children)
				if (child.I.getStart() <= I.getStart() && child.I.getEnd() >= I.getEnd())
				{
					next = child;
					break;
				}
		} while (n != next);
		return n;
	}
	
	Pair<Node<T>, Node<T>> splitNode(Node<T> n, int pos)
	{
		Node<T> x = new Node<T>(new TextInterval(n.I.getStart(), pos));
		Node<T> y = new Node<T>(new TextInterval(pos, n.I.getEnd()));
		x.values.addAll(n.values);
		y.values.addAll(n.values);
		return new Pair<Node<T>, Node<T>>(x,y);
	}
	
	public IntervalTree(TextInterval topInterval)
	{
		root = new Node<T>(topInterval);
	}
	
	public IntervalTree(TextInterval topInterval, T topValue)
	{
		root = new Node<T>(topInterval, topValue);
	}
	
	public void add(TextInterval I, T value)
	{
		Node<T> parent = findParent(root, I);
		if (parent == null)
			throw new RuntimeException("Attempt to add interval " + I + " larger than the root " + root.I);
		else if (parent.I.equals(I))
		{
			parent.values.add(value);
			return;
		}
		int firstChild = -1;
		int lastChild = -1;
		for (int i = 0; i < parent.children.size(); i++)
		{
			Node<T> child = parent.children.getAt(i);
			if (child.I.getStart() >= I.getEnd())
				break;			
			if (firstChild == -1 && child.I.getEnd() > I.getStart())
				firstChild = i;
			if (child.I.getStart() < I.getEnd())
				lastChild = i;
		}
		Node<T> newNode = new Node<T>(I, value);
		if (firstChild > -1)
		{
			Node<T> first = parent.children.removeAt(firstChild);
			lastChild--;
			if (first.I.getStart() < I.getStart()) // need to split
			{
				Pair<Node<T>, Node<T>> p = splitNode(first, I.getStart());
				p.getSecond().values.add(value);
				parent.children.add(p.getFirst());
				lastChild++;
				firstChild++;
				newNode.children.add(p.getSecond());				
			}
			else
				newNode.children.add(first);
			for (int i = firstChild; i < lastChild;)
			{
				newNode.children.add(parent.children.removeAt(i));
				lastChild--;
			}
			if (lastChild >= firstChild)
			{
				Node<T> last = parent.children.removeAt(lastChild);
				if (last.I.getEnd() > I.getEnd()) // need to split
				{
					Pair<Node<T>, Node<T>> p = splitNode(last, I.getEnd());
					p.getFirst().values.add(value);
					parent.children.add(p.getSecond());
					newNode.children.add(p.getFirst());	
				}
				else
					newNode.children.add(last);					
			}
		}
		parent.children.add(newNode);
	}
	
	public List<T> get(TextInterval I)
	{		
		if (root.I.equals(I))
			return root.values;
		Node<T> next = null;
		for (Node<T> n = root; n != null; n = next)
		{			
			next = null;
			for (Node<T> child : n.children)
			{
				if (child.I.equals(I))
					return child.values;
				else if (child.I.getStart() <= I.getStart() && child.I.getEnd() >= I.getEnd())
				{
					next = child;
					break;
				}
			}			
		}
		return null;
	}
	
	public TextInterval findEnclosing(TextInterval I)
	{
		return findParent(root, I).I;
	}
	
	private void printIntervals(PrintStream out, Node<T> n, String indent)
	{
		out.println(indent + n.I);
		String newIndent = indent + "  ";
		for (Node<T> child : n.children)
			printIntervals(out, child, newIndent);
	}
	
	public void printIntervalsOnly(PrintStream out)
	{
		printIntervals(out, root, "");
	}
	
	private boolean isValid(Node<T> n)
	{
		for (Node<T> child : n.children)
			if (n.I.getStart() > child.I.getStart() || 
				n.I.getEnd() < child.I.getEnd() || 
				!isValid(child))
				return false;
		return true;
	}
	
	public boolean isValid()
	{
		return isValid(root);
	}
		
	public Iterator<TextInterval> leafs()
	{
		final Stack<Pair<Node<T>, Integer>> stack = new Stack<Pair<Node<T>, Integer>>();		
		stack.push(new Pair<Node<T>, Integer>(root, root.children.isEmpty() ? -1 : 0));		
		return new Iterator<TextInterval>()
		{
			public void remove() { throw new UnsupportedOperationException(); }
			public boolean hasNext() { return !stack.isEmpty(); }
			public TextInterval next()
			{
				Pair<Node<T>, Integer> p = stack.pop();				
				if (p.getSecond() == -1) // is it a leaf node?
					return p.getFirst().I;
				if (p.getFirst().children.size() -1 > p.getSecond())
					stack.push(new Pair<Node<T>, Integer>(p.getFirst(), p.getSecond() + 1));
				Node<T> nextNode = p.getFirst().children.getAt(p.getSecond());
				stack.push(new Pair<Node<T>, Integer>(nextNode, nextNode.children.isEmpty() ? -1 : 0));
				return next();
			}
		};
	}
	
	public static void main(String [] args)
	{
		IntervalTree tree = new IntervalTree(new TextInterval(0, 2000), null);
		tree.add(new TextInterval(10, 20), "A");
		tree.add(new TextInterval(40, 50), "B");
		tree.add(new TextInterval(100, 150), "C");
		tree.add(new TextInterval(30, 200), "D");
		tree.add(new TextInterval(30, 33), "E");
		tree.add(new TextInterval(2, 10), "F");
		tree.add(new TextInterval(5, 15), "split");
		tree.add(new TextInterval(10, 15), "duplicate just adds value");  
//		tree.add(new TextInterval(0, 3000), "Z"); // should throw exception
		tree.printIntervalsOnly(System.out);
		System.out.println(tree.get(new TextInterval(10,15)));
		for (Iterator<TextInterval> ti = tree.leafs(); ti.hasNext(); )
			System.out.print(ti.next() + " ");
	}
}
