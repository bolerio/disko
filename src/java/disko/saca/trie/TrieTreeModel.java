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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

// A TreeModel for a Trie
class TrieTreeModel extends DefaultTreeModel
{
    private static final long serialVersionUID = 5102280314120333696L;

    TrieTreeModel(Trie trie)
    {
        super(TrieTreeNode.newInstance(null, trie.root, ""));
    }
}

// A TreeNode for a TrieNode
class TrieTreeNode implements TreeNode, Comparable<TrieTreeNode>, java.io.Serializable
{
    private static final long serialVersionUID = -4406312561494684134L;
    
    final TrieTreeNode parent;
    final Node trieNode;
    final String word;
    
    // flyweight design pattern
    private static final Map<Node, TrieTreeNode> tNodeMapping = new HashMap<Node, TrieTreeNode>();

    private TrieTreeNode(TrieTreeNode parent, Node trieNode, String word)
    {
        this.parent = parent;
        this.trieNode = trieNode;
        this.word = word;
    }

    public static TrieTreeNode newInstance(TrieTreeNode parent,
                                           Node trieNode,
                                           String word)
    {
        if (tNodeMapping.containsKey(trieNode))
            return tNodeMapping.get(trieNode);
        else
        {
            TrieTreeNode trieTreeNode = new TrieTreeNode(parent, trieNode, word);
            tNodeMapping.put(trieNode, trieTreeNode);
            return trieTreeNode;
        }
    }

    public int compareTo(TrieTreeNode o)
    {
        return Double.compare(trieNode.weight, o.trieNode.weight);
    }

    public Enumeration children()
    {
        if (trieNode instanceof LeafNode)
            return Collections.enumeration(Collections.EMPTY_SET);
        InternalNode inode = (InternalNode)trieNode;
        Set<Entry<Character, Node>> entries = inode.children.entrySet();
        List<TrieTreeNode> trieTreeNodes = new ArrayList<TrieTreeNode>(entries.size()); 
        for (Entry<Character, Node> entry : entries)
        {
            trieTreeNodes.add(newInstance(this, entry.getValue(), word + entry.getKey()));
        }

        Collections.sort(trieTreeNodes);

        return Collections.enumeration(trieTreeNodes);
    }

    public boolean getAllowsChildren()
    {
        return true;
    }

    public TreeNode getChildAt(int childIndex)
    {
        if (trieNode instanceof LeafNode)
            return null;
        InternalNode inode = (InternalNode)trieNode;
        Enumeration e = children();
        int i = 0;
        while (e.hasMoreElements())
        {
            if (i++ == childIndex)
                return (TreeNode) e.nextElement();
            else
                e.nextElement();
        }

        return null;
    }

    public int getChildCount()
    {
        if (trieNode instanceof LeafNode)
            return 0;
        InternalNode inode = (InternalNode)trieNode;
        return inode.children.size();
    }

    public int getIndex(TreeNode node)
    {
        Enumeration e = children();
        int i = 0;
        while (e.hasMoreElements())
        {
            TreeNode treeNode = (TreeNode) e.nextElement();
            if (treeNode.equals(node))
                return i;

            i++;
        }
        return -1;
    }

    public TreeNode getParent()
    {
        return parent;
    }

    public boolean isLeaf()
    {
        return getChildCount() == 0;
    }

/*    private String getWord()
    {
        if (parent == null)
            return "";

        return parent.getWord() + trieNode.c;
    } */

    @Override
    public String toString()
    {
        return word + "-" + trieNode.weight; // getWord();
    }
}
