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
package disko.saca.trie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * An implementation of a trie that dynamically grows and shrinks with word
 * insertions and removals and supports approximate matching of words using edit
 * distance and word frequency.
 * 
 * This trie implementation is not thread-safe because I didn't need it to be,
 * but it would be easy to change.
 * 
 * @author Brian Harris (brian_bcharris_net)
 */
public class Trie implements java.io.Serializable
{
    private static final long serialVersionUID = -7984682067008153942L;

    public static final Character NIL = new Character((char)0);
    
    // dummy node for root of trie
    final InternalNode root;

    // current number of unique words in trie
    private int size;

    // if this is a case sensitive trie
    protected boolean caseSensitive;

    /**
     * @param caseSensitive
     *            If this Trie should be case-insensitive to the words it
     *            encounters. Case-insensitivity is accomplished by converting
     *            String arguments to all functions to lower-case before
     *            proceeding.
     */
    public Trie(boolean caseSensitive)
    {
        root = new InternalNode();
        size = 0;
        this.caseSensitive = caseSensitive;
    }

    public List<String> topN(String prefix, int N)
    {
        ArrayList<String> L = new ArrayList<String>();
        InternalNode node = root;
        for (int i = 0; i < prefix.length() && node != null; i++)
            node = (InternalNode)node.children.get(prefix.charAt(i));
        if (node == null) // no match
            return L;
        PriorityQueue<Node> toexplore = new PriorityQueue<Node>();        
        toexplore.add(node);
        while (L.size() < N  && !toexplore.isEmpty())
        {
            Node x = toexplore.remove();
//            System.out.println("Top is " + x.weight + " " + x.getClass());
            if (x instanceof LeafNode)
                L.add(((LeafNode)x).data);
            else 
                for (Node child : ((InternalNode)x).children.values())
                    toexplore.add(child);
        }
        return L;
    }
    
    public void insert(String word, double weight)
    {
        if (word == null || word.length() == 0)
            return;
        root.insert(caseSensitive ? word : word.toLowerCase(), weight, 0);
        size++;
    }

    public double weight(String word)
    {
        if (word == null)
            return 0;

        Node n = root.lookup(caseSensitive ? word : word.toLowerCase(), 0);
        return n == null ? 0.0 : n.weight;
    }
    
    public int size()
    {
        return size;
    }

    @Override
    public String toString()
    {
        return root.toString();
    }

    public String bestMatch(String word, long max_time)
    {

        if (!caseSensitive)
            word = word.toLowerCase();

        // we store candidate nodes in a pqueue in an attempt to find the
        // optimal
        // match ASAP which can be useful for a necessary early exit
        PriorityQueue<DYMNode> pq = new PriorityQueue<DYMNode>();

        DYMNode best = new DYMNode(root, Distance.LD("", word), "", false);
        DYMNode cur = best;
        Node tmp;
        int count = 0;
        long start_time = System.currentTimeMillis();

        while (true)
        {
            if (count++ % 1000 == 0
                    && (System.currentTimeMillis() - start_time) > max_time)
                break;

            if (cur.node instanceof InternalNode)
            {
                InternalNode icur = (InternalNode)cur.node; 
                for (char c : icur.children.keySet())
                {
                    tmp = icur.children.get(c);
                    String tWord = cur.word + c;
                    int distance = Distance.LD(tWord, word);

                    // only add possibly better matches to the pqueue
                    if (distance <= cur.distance)
                    {
                        if (tmp instanceof InternalNode)
                            pq.add(new DYMNode(tmp, distance, tWord, false));
                        else
                            pq.add(new DYMNode(tmp, distance, tWord, true));
                    }
                }
            }

            DYMNode n = pq.poll();

            if (n == null)
                break;

            cur = n;
            if (n.wordExists)
                // if n is more optimal, set it as best
                if (n.distance < best.distance
                        || (n.distance == best.distance && n.node.weight > best.node.weight))
                    best = n;
        }
        return best.word;
    }
}

class Node implements Comparable<Node>, java.io.Serializable
{
    private static final long serialVersionUID = 1872955823127195350L;
    
    double weight = 0.0;

    public int compareTo(Node o)
    {
        return -Double.compare(weight, o.weight);
    }    
}

class LeafNode extends Node
{
    private static final long serialVersionUID = -6855349641641196938L;
    String data;
    LeafNode(String data, double weight) { this.data = data; this.weight = weight; }
}
    
class InternalNode extends Node
{
    private static final long serialVersionUID = -7596138719551893096L;
    Map<Character, Node> children = new HashMap<Character, Node>();    

    double insert(String s, double weight, int pos)
    {
        char c = s.charAt(pos);
        InternalNode n = (InternalNode)children.get(c);

        // make sure we have a child with char c
        if (n == null)
        {
            n = new InternalNode();
            n.weight = weight;
            children.put(c, n);
        }

        // if we are the last node in the sequence of chars
        // that make up the string
        if (pos == s.length() - 1)
        {
            LeafNode leaf = (LeafNode)n.children.get(Trie.NIL);
            if (leaf != null) // if already insert, just update the weight...
                leaf.weight = weight;
            else
            {
                leaf = new LeafNode(s, weight);
                n.children.put(Trie.NIL, leaf);
            }
            return leaf.weight;
        }
        else
            return this.weight = Math.max(n.insert(s, weight, pos + 1), this.weight);
    }    
    
    Node lookup(String s, int pos)
    {
        char c = s.charAt(pos);
        InternalNode n = (InternalNode)children.get(c);
        if (n == null)
            return null;
        if (pos == s.length() - 1)
            return n.children.get(Trie.NIL);
        else
            return n.lookup(s, pos + 1);
    }
}

/**
 * Utility class for finding a best match to a word.
 * 
 * @author Brian Harris (brian_bcharris_net)
 */
class DYMNode implements Comparable<DYMNode>
{
    Node node;
    String word;
    int distance;
    boolean wordExists;

    DYMNode(Node node, int distance, String word, boolean wordExists)
    {
        this.node = node;
        this.distance = distance;
        this.word = word;
        this.wordExists = wordExists;
    }

    // break ties of distance with frequency
    public int compareTo(DYMNode n)
    {
        if (distance == n.distance)
            return Double.compare(n.node.weight, node.weight);
        return distance - n.distance;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null)
            return false;

        if (!(o instanceof DYMNode))
            return false;

        DYMNode n = (DYMNode) o;
        return distance == n.distance && n.node.weight == node.weight;
    }

    @Override
    public int hashCode()
    {
        int hash = 31;
        hash += distance * 31;
        return hash;
    }

    @Override
    public String toString()
    {
        return word + ":" + distance;
    }
}
