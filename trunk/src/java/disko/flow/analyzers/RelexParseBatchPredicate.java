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
package disko.flow.analyzers;

import java.util.UUID;

import org.hypergraphdb.util.Mapping;

import disko.data.relex.RelexParse;

public class RelexParseBatchPredicate implements Mapping<Object, Boolean>
{
    private transient UUID lastSentenceId = null;
    
    /**
     * Return <code>true</code> if the parameter 'x' (a {@see RelexParse}) is still
     * about the same sentence as the last 'x' this predicate was invoked with.
     */
    public Boolean eval(Object x)
    {
        UUID curr = lastSentenceId;
        lastSentenceId = ((RelexParse)x).getSentenceId();
        return curr == null || curr.equals(lastSentenceId);
    }
}
