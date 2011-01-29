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

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

/**
 * A wrapper for a Trie which provides a TreeModel to display the underlying
 * Trie.
 * 
 * @author Brian Harris (brian_bcharris_net)
 */
public class ViewableTrie extends Trie
{
    private final TrieTreeModel treeModel;

    public ViewableTrie(boolean caseSensitive)
    {
        super(caseSensitive);
        treeModel = new TrieTreeModel(this);
    }

    public void insert(String word, double weight)
    {
        super.insert(word, weight);
        treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());
    }

    /**
     * Get a TreeModel representing this Trie.
     * 
     * @return A TreeModel which will be updated with changes to this Trie.
     */
    public TreeModel getTreeModel()
    {
        return treeModel;
    }
}
