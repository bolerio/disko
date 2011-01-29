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
import java.util.List;
import java.util.UUID;

import org.hypergraphdb.util.HGUtils;

import relex.ParsedSentence;
import relex.entity.EntityMaintainer;
import relex.feature.FeatureNode;
import relex.feature.LinkableView;

/**
 * <p>
 * A lighter-weight version of Relex's <code>relex.ParsedSentence</code> more
 * suitable for wire transmission.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class RelexParse
{
    /**
     * Unique runtime ID of the sentence this parse belongs to. This is
     * simply because the 'originalSentence' cannot by itself identify
     * the input uniquely - there may be more than two identical 
     * "original sentence" that appear consecutive in a single document. While
     * this is unlikely in conventional prose, it is more likely in something
     * extracted from an HTML table for example. 
     */
    private UUID sentenceId = null;
    
    private EntityMaintainer entityMaintainer = new EntityMaintainer();
    
    // Metadata about the sentence; primarily, this consists of diagnostic
    // info returned by the link grammar parser.
    private FeatureNode metaData = null;

    // An ArrayList of FeatureNodes, each one representing a word in the
    // sentence.  If there are no "link islands", each can be reached by
    // following arcs from the others.
    private List<FeatureNode> leafConstituents = new ArrayList<FeatureNode>();
        
    public static RelexParse make(ParsedSentence parse)
    {
        RelexParse rparse = new RelexParse();
        rparse.setMetaData(parse.getMetaData());
        for (int i = 0; i < parse.getNumWords(); i++)
            rparse.leafConstituents.add(parse.getWordAsNode(i));
        return rparse;
    }
    
    /**
     * <p>Return a <code>relex.ParsedSentence</code> representation which won't contain
     * extras such as a back pointer to the original <code>relex.Sentence</code> etc. 
     */
    public ParsedSentence toParsedSentence()
    {
        ParsedSentence ps = new ParsedSentence(entityMaintainer.getOriginalSentence());
        ps.setMetaData(metaData);
        for (FeatureNode fn : leafConstituents)
            ps.addWord(fn);
        return ps;
    }
        
    public UUID getSentenceId()
    {
        return sentenceId;
    }

    public void setSentenceId(UUID sentenceId)
    {
        this.sentenceId = sentenceId;
    }

    public EntityMaintainer getEntityMaintainer()
    {
        return entityMaintainer;
    }

    public void setEntityMaintainer(EntityMaintainer entityMaintainer)
    {
        this.entityMaintainer = entityMaintainer;
    }

    public FeatureNode getMetaData()
    {
        return metaData;
    }

    public void setMetaData(FeatureNode metaData)
    {
        this.metaData = metaData;
    }

    public List<FeatureNode> getLeafConstituents()
    {
        return leafConstituents;
    }

    public void setLeafConstituents(List<FeatureNode> leafConstituents)
    {
        this.leafConstituents = leafConstituents;
    }

    public int getNumWords()
    {
        return leafConstituents.size();
    }

    /**
     * Return the i'th word in the sentence, as a feature node
     */
    public FeatureNode getWordAsNode(int i)
    {
        return leafConstituents.get(i);
    }

    /**
     * Return the i'th lemmatized word in the sentence, as a string.
     * This is the "root form" of the word, and not the original word.
     */
    public String getWord(int i)
    {
        return LinkableView.getWordString(getWordAsNode(i));
    }

    /**
     * Return the i'th word in the sentence, as a string
     * This is the original form of the word, and not its lemma.
     */
    public String getOrigWord(int i)
    {
        return LinkableView.getWordString(getWordAsNode(i));
    }

    /**
     * Return the part-of-speech of the i'th word in the sentence
     */
    public String getPOS(int i)
    {
        return LinkableView.getPOS(getWordAsNode(i));
    }

    /**
     * Return the offset, in the original sentence, to the first
     * character of the i'th word in the sentence.
     */
    public int getStartChar(int i)
    {
        return LinkableView.getStartChar(getWordAsNode(i));
    }

    public void addWord(FeatureNode w)
    {
        leafConstituents.add(w);
    }

    public int getAndCost()
    {
        return getMeta("and_cost");
    }

    public int getDisjunctCost()
    {
        return getMeta("disjunct_cost");
    }

    public int getLinkCost()
    {
        return getMeta("link_cost");
    }

    public int getNumSkippedWords()
    {
        return getMeta("num_skipped_words");
    }

    private int getMeta(String str)
    {
        FeatureNode fn = metaData.get(str);
        if (fn == null) return -1;
        String val = fn.getValue();
        return Integer.parseInt(val);
    }
    
    public String toString()
    {
        return "RelexParse[" + entityMaintainer.getOriginalSentence() + "]" + hashCode();
    }
    
    public int hashCode()
    {
        return entityMaintainer == null ? 0 : entityMaintainer.hashCode();
    }
 
    public boolean equals(Object x)
    {
        if ( ! (x instanceof RelexParse))
            return false;
        
        RelexParse parse = (RelexParse)x;
        return HGUtils.eq(parse.entityMaintainer, entityMaintainer) &&
               HGUtils.eq(sentenceId, parse.sentenceId);
    }
}
