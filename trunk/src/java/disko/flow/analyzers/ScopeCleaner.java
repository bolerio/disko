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

import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.Ports;
import org.hypergraphdb.query.HGAtomPredicate;

import disko.AnalysisContext;
import disko.DU;
import disko.ParagraphAnn;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SynRel;

/**
 * 
 * <p>
 * This processor is performs a cleanup activity intended to be run at
 * the beginning of an IE (information extraction) network. It deletes
 * all scoped data within a given document. It assumes that the current
 * scope of the {@link AnalysisContext} is the document to be cleaned. 
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class ScopeCleaner extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    public static final String SIGNAL_DB_CLEAN = "db_clean";
    
    public static final HGAtomPredicate DEFAULT_REMOVE_SCOPE_PREDICATE = 
        hg.or(hg.type(RelOccurrence.class),
              hg.type(ParagraphAnn.class),
              hg.type(SentenceAnn.class),
              hg.and(hg.type(SynRel.class),
                     hg.disconnected()));        
    public static final HGAtomPredicate DEFAULT_STOP_RECURSION_PREDICATE = 
    	hg.or(hg.type(RelOccurrence.class),
    		  hg.type(SynRel.class));
        
    private HGAtomPredicate removeScopePredicate = DEFAULT_REMOVE_SCOPE_PREDICATE;
    private HGAtomPredicate stopRecursionPredicate = DEFAULT_STOP_RECURSION_PREDICATE;
    
    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
    {        
        DU.deleteScope(ctx.getGraph(), 
                       ctx.getTopScope(), 
                       getRemoveScopePredicate(), 
                       getStopRecursionPredicate());
        ports.getOutput(SIGNAL_DB_CLEAN).put("done");
    }

    public HGAtomPredicate getRemoveScopePredicate()
    {
        return removeScopePredicate;
    }

    public void setRemoveScopePredicate(HGAtomPredicate removeScopePredicate)
    {
        this.removeScopePredicate = removeScopePredicate;
    }

    public HGAtomPredicate getStopRecursionPredicate()
    {
        return stopRecursionPredicate;
    }

    public void setStopRecursionPredicate(HGAtomPredicate stopRecursionPredicate)
    {
        this.stopRecursionPredicate = stopRecursionPredicate;
    }
    
    
}
