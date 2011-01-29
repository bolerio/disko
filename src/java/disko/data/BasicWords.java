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
package disko.data;

import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import org.hypergraphdb.HGException;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.Word;
import org.hypergraphdb.HyperGraph;

import disko.DU;

/**
 * 
 * <p>
 * Lists some basic English words that are not part of WordNet, like pronouns
 * and prepositions.
 * </p>
 * 
 * @author Borislav Iordanov
 * 
 */
public class BasicWords
{
    public static final String RESOURCE = "/org/disco/data/basicwords.lst";

    public static final String [] PREPOSITIONS = new String[] 
    {
         "of", "in", "for", "to", "with", "on", "at", "by", "from", "about",
         "than", "over", "through", "after", "between", "under", "per", "among",
         "within", "towards", "above", "near", "off", "past", "worth", "toward", 
         "plus", "till", "amongst", "via", "amid", "underneath", "versus", "amidst",
         "sans", "circa", "pace", "nigh", "re", "mid", "o'er", "but", "ere", "less", 
         "midst", "o'", "thru", "vice"
    };
    
    private static final Set<String> prepositionSet = new HashSet<String>();
    static { for (String s : PREPOSITIONS) prepositionSet.add(s); }
    
    public static final String [] PARTICLES = new String[]
    {                                                         
         "aboard", "about", "above", "across", "ahead", "alongside", "apart",
         "around", "aside", "astray", "away", "back", "before", "behind", "below",
         "beneath", "besides", "between", "beyond", "by", "close", "down", "east",
         "west", "south", "north", "eastward", "westward", "southward", "northward",
         "eastwards", "westwards", "southwards", "northwards", "forward", "forwards",
         "home", "in", "inside", "instead", "near", "off", "on", "opposite", 
         "out", "outside", "over", "overhead", "past", "round", "since", "through",
         "throughout", "together", "under", "underneath", "up", "within", "without"
    };
    
    private static final Set<String> particleSet = new HashSet<String>();
    static { for (String s : PARTICLES) particleSet.add(s); }
    
    public static final String [] CONJUNCTIONS = new String[]
    {
         "and", "that", "but", "or", "as", "if", "when", "because", "so", "before",
         "though", "than", "while", "after", "whether", "for", "altough", "until", 
         "yet", "since", "where", "nor", "once", "unless", "why", "now", "neither",
         "whenever", "whereas", "except", "till", "provided", "whilst", "suppose", 
         "cos", "supposing", "considering", "lest", "albeit", "providing", "whereupon",
         "seeing", "directly", "ere", "notwithstanding", "according_as", "as_if", 
         "as_long_as", "as_though", "but_that", "but_then", "but_then_again", "forasmuch_as",
         "however", "immediately", "in_as_far_as", "in_so_far_as", "inasmuch_as", "insomuch_as",
         "insomuch_that", "like", "now_that", "only", "provided_that", "providing_that",
         "seeing_as", "seeing_as_how", "seeing_that", "without"
    };
    
    private static final Set<String> conjunctionSet = new HashSet<String>();
    static { for (String s : CONJUNCTIONS) conjunctionSet.add(s); }
    
    public static String [] PRONOUNS = new String []
    {
         "it", "I", "he", "you", "his", "they", "this", "that", "she", "her", "we",
         "all", "which", "their", "what", "my", "him", "me", "who", "them", "no",
         "some", "other", "your", "its", "our", "these", "any", "more", "many", "such",
         "those", "own", "us", "how", "another", "where", "same", "something", "each",
         "both", "last", "every", "himself", "nothing", "when", "one", "much", "anything", 
         "next", "themselves", "most", "itself", "myself", "everything", "several", 
         "less", "herself", "whose", "someone", "certain", "anyone", "whom", "enough",
         "half", "few", "everyone", "whatever", "yourself", "why", "little", "none",
         "nobody", "further", "everybody", "ourselves", "mine", "somebody", "former",
         "past", "plenty", "either", "yours", "neither", "fewer", "hers", "ours", "whoever",
         "least", "twice", "theirs", "wherever", "oneself", "thou", "'un", "ye", "thy",
         "whereby", "thee", "yourselves", "latter", "whichever", "no_one", "wherein", 
         "double", "thine", "summat", "suchlike", "fewest", "thyself", "whomever", "whosoever",
         "whomsoever", "wherefore", "whereat", "whatsoever", "whereon", "whoso", "aught",
         "howsoever", "thrice", "wheresoever", "you-all", "additional", "anybody", "each_other",
         "once", "one_another", "overmuch", "such_and_such", "whate'er", "whenever", "whereof",
         "whereto", "whereunto", "whichsoever"
    };
    
    private static final Set<String> pronounSet = new HashSet<String>();
    static { for (String s : PRONOUNS) pronounSet.add(s); }
    
    public static String [] RELEX_SPECIAL = new String []
	{
		"_%copula", "_$qvar", "_$cvar", "_$crvar"
	};
    
    private static final Set<String> relexSet = new HashSet<String>();
    static { for (String s : RELEX_SPECIAL) relexSet.add(s); }
    
    private static final Set<String> basicWords = new HashSet<String>();
    
    static
    {
    	basicWords.addAll(prepositionSet);
    	basicWords.addAll(particleSet);
    	basicWords.addAll(pronounSet);
    	basicWords.addAll(conjunctionSet);
    	basicWords.addAll(relexSet);
    }
        
    public static void unloadAll(final HyperGraph graph)
    {
        // since some words overlap in parts of speech, we don't know which
        // ones to erase for sure, so we do nothing...
        DU.log.warn("BasicWords.unloadAll is a NOP since some of those function words belong to other parts of speech.");
    }
    
    public static void unloadAll_xxxx(final HyperGraph graph)
    {
        final List<HGHandle> L1 = hg.findAll(graph, hg.type(Pronoun.class));
        graph.getTransactionManager().transact(new Callable<Object>() {
            public Object call()
            {
                for (HGHandle h : L1) graph.remove(h);
                return null;
            }
        });
        final List<HGHandle> L2 = hg.findAll(graph, hg.type(Preposition.class));
        graph.getTransactionManager().transact(new Callable<Object>() {
            public Object call()
            {
                for (HGHandle h : L2) graph.remove(h);
                return null;
            }
        });        
    }
    
    private static void load(String [] A, HyperGraph graph)
    {
        for (String s : A)
        {
            if (hg.findOne(graph, hg.and(hg.type(Word.class), hg.eq("lemma", s))) == null)
                graph.add(new Word(s));
        }
    }
    
    public static void loadAll(HyperGraph graph)
    {
        load(PREPOSITIONS, graph);
        load(PARTICLES, graph);
        load(CONJUNCTIONS, graph);
        load(PRONOUNS, graph);
        load(RELEX_SPECIAL, graph);
    }
    
    public static void loadAll_xxxx(HyperGraph graph)
    {
        InputStream resourceIn = BasicWords.class.getResourceAsStream(RESOURCE);
        if (resourceIn == null)
            throw new HGException("Fatal error: could not Disko basic words from "
                                          + RESOURCE
                                          + ", this resource could not be found!");
        BufferedReader reader = new BufferedReader(
                                                   new InputStreamReader(
                                                                         resourceIn));
        try
        {
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
                line = line.trim();
                if (line.length() == 0)
                    continue;

                if (line.startsWith("#"))
                    continue;

                StringTokenizer tok = new StringTokenizer(line, " ");
                if (tok.countTokens() < 3)
                    throw new HGException("Fatal error: could not basic words from "
                                                  + RESOURCE
                                                  + ", the line "
                                                  + line
                                                  + " is ill formed - expecting 3 tokens.");
                String pHandleStr = tok.nextToken().trim();
                String typeClassName = tok.nextToken().trim();
                String lemma = tok.nextToken().trim();
                Object word = null;
                if ("pronoun".equals(typeClassName))
                    word = new Pronoun(lemma);
                else if ("preposition".equals(typeClassName))
                    word = new Preposition(lemma);
                else
                    throw new Exception("Unknown basic word type " + typeClassName);
                graph.define(graph.getHandleFactory().makeHandle(pHandleStr), word);
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (Throwable t)
            {
            }
        }
    }
    
    public static boolean isBasicWord(String s) { return basicWords.contains(s); }    
    public static boolean isPreposition(String s) { return prepositionSet.contains(s); } 
    public static boolean isParticle(String s) { return particleSet.contains(s); }
    public static boolean isPronoun(String s) { return pronounSet.contains(s); }
    public static boolean isConjunction(String s) { return conjunctionSet.contains(s); }
    public static boolean isRelexSpecial(String s) { return relexSet.contains(s); }
}
