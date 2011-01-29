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
package disko.relex;

import java.util.HashMap;
import java.util.Map;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.AdjExcLink;
import org.hypergraphdb.app.wordnet.data.AdjSynsetLink;
import org.hypergraphdb.app.wordnet.data.AdverbExcLink;
import org.hypergraphdb.app.wordnet.data.AdverbSynsetLink;
import org.hypergraphdb.app.wordnet.data.ExcLink;
import org.hypergraphdb.app.wordnet.data.NounExcLink;
import org.hypergraphdb.app.wordnet.data.NounSynsetLink;
import org.hypergraphdb.app.wordnet.data.SynsetLink;
import org.hypergraphdb.app.wordnet.data.VerbExcLink;
import org.hypergraphdb.app.wordnet.data.VerbSynsetLink;
import org.hypergraphdb.app.wordnet.data.Word;

import disko.DU;

import relex.morphy.Morphed;
import relex.morphy.Morphy;
import relex.morphy.MorphyJWNL;
import relex.feature.FeatureNode;

public class MorphyHGDB extends MorphyJWNL
{
	public static final String MORPHY_HGDB_LOCATION = "morphy.hgdb.location";
	
	private String hgdbLocation;
	private HyperGraph graph;
	private Map<String, Stemmer> stemmers = new HashMap<String, Stemmer>();
	
	private void initHGDB()
	{
		if (DU.isEmpty(hgdbLocation))
			hgdbLocation = System.getProperty(MORPHY_HGDB_LOCATION);
		if (!DU.isEmpty(hgdbLocation))
			graph = HGEnvironment.get(hgdbLocation);
		else
			throw new RuntimeException("Please specify the location of the WordNet HGDB database in the " +
					MORPHY_HGDB_LOCATION + " system property");
	}
	
	// hard-coded defaults for now...
	private void initStemmers()
	{
		stemmers.put(NOUN_F, 
					 SuffixStemmer.makeSuffixStemmer("|ses=s|xes=x|zes=z|ches=ch|shes=sh|men=man|ies=y|s=|"));
		stemmers.put(VERB_F, 
					 SuffixStemmer.makeSuffixStemmer("|ies=y|es=e|es=|ed=e|ed=|ing=e|ing=|s=|"));
		stemmers.put(ADJ_F, 
				     SuffixStemmer.makeSuffixStemmer("|er=|est=|er=e|est=e|"));
	}
	
	// Lookup the lemma->Word atom index. 
	private HGHandle findWord(String lemma)
	{
		return hg.findOne(graph, hg.and(hg.type(Word.class), hg.eq("lemma", lemma)));
	}
	
	// Check whether the given Word atom is a part of speech as represented 
	// by the passed synset link type. 
	private boolean checkPos(Class<? extends SynsetLink> linkType, HGHandle handle)
	{
		return hg.findOne(graph, hg.and(hg.type(linkType), hg.incident(handle))) != null;	
	}

	private String findException(Class<? extends ExcLink> linkType, HGHandle handle)
	{
		HGHandle stemmed = hg.findOne(graph, hg.and(hg.type(linkType), hg.incident(handle)));
		if (stemmed != null)
		{
			HGLink exc = graph.get(stemmed);
			Word w = graph.get(exc.getTargetAt(1));
			return w.getLemma();
		}
		else
			return null;
	}
	
	public class LazyMorphed extends Morphed
	{
		private HGHandle originalWord;
		
		LazyMorphed(String original)
		{
			super(original);
			originalWord = findWord(original.toLowerCase());
		}
		
		public FeatureNode getPos(String pos, 
								  Class<? extends SynsetLink> linkType, 
								  Class<? extends ExcLink> excLinkType)
		{
			FeatureNode n = getFeatures().get(pos);
			if (n == null)
			{
				n = new FeatureNode();
				getFeatures().put(pos, n);
				String stem = null;
				if (originalWord != null)
				{
					if (checkPos(linkType, originalWord))
						stem = ((Word)graph.get(originalWord)).getLemma();
					else
						stem = findException(excLinkType, originalWord);
				}
				if (stem == null)
				{
					Stemmer stemmer = stemmers.get(pos);
					if (stemmer != null)
						for (String stemmed : stemmer.stemIt(original))
						{
							HGHandle word = findWord(stemmed.toLowerCase());
							if (word != null)
							{
								if (checkPos(linkType, word))
									stem = ((Word)graph.get(word)).getLemma();
								else
									stem = findException(excLinkType, word);
							}
							if (stem != null)
								break;
						}
				}
				if (stem != null)
				{
					putRoot(pos, maybeChangeFirstLetter(original, stem.replace('_', ' ')));
				}
				// Make one last attempt: some nouns start with uppercase, check for them
				else if (Character.isUpperCase(original.charAt(0)) && pos.equals(Morphy.NOUN_F))
				{
					HGHandle word = findWord(original);
					if (word != null && checkPos(linkType, word))
						putRoot(pos, original.replace('_', ' '));
				}
			}
			return n.get(Morphy.ROOT_F) == null ? null : n;			
		}
		
		public FeatureNode getNoun() 
		{
			return getPos(NOUN_F, NounSynsetLink.class, NounExcLink.class);
		}

		public FeatureNode getVerb() 
		{
			return getPos(Morphy.VERB_F, VerbSynsetLink.class, VerbExcLink.class);
		}

		public FeatureNode getAdj() 
		{
			return getPos(ADJ_F, AdjSynsetLink.class, AdjExcLink.class);			
		}

		public FeatureNode getAdv() 
		{
			return getPos(ADV_F, AdverbSynsetLink.class, AdverbExcLink.class);
		}		
	}
	
	public Morphed morph(String word)
	{
		Morphed m = new LazyMorphed(word);		
		if (!loadPossessive(word, m)) // if it is a common possessive form of personal pronoun
		{
    		word = stripNegativeContraction(word);
    		boolean negativeVerb = !word.equals(m.getOriginal());
    		if (negativeVerb)
    			m.putRootNegative(VERB_F, maybeChangeFirstLetter(m.getOriginal(), word));
		}
//		System.out.println("Morphed " + word + " to " + m);
		return m;
	}
	
	public Map<String, Stemmer> getStemmers()
	{
		return stemmers;
	}
	
	public MorphyHGDB()
	{
	}
	
	public void initialize()
	{		
		initHGDB();
		initStemmers();
	}
	
	public void setGraphLocation(String hgdbLocation)
	{
		this.hgdbLocation = hgdbLocation;
		graph = HGEnvironment.get(hgdbLocation);
	}
	
	public String getGraphLocation()
	{
		return this.hgdbLocation;
	}
}
