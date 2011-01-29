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

import java.lang.reflect.Constructor;
import java.util.List;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.util.Pair;
import org.hypergraphdb.app.wordnet.data.*;

import disko.AnalysisContext;
import disko.TextDocument;
import relex.feature.FeatureNode;

/**
 * 
 * <p>
 * This  <code>SenseFinder</code> is a real dummy - it will pick the first synset 
 * that has the given word as its sense. If the word has no synset pointing to it, 
 * one will be created for the POS as indicated in the <code>FeatureNode</code>. This
 * creation can be disabled by setting the <code>autoCreate</code> attribute to false
 * (the default is <code>true</code>).
 * When creation is disabled and no synset is found, the finder returns <code>null</code>.
 * If there's only one synset for the given word-POS combination, the confidence is 
 * 0.95. When multiple synsets are found the confidence is 1/#(synsets available). When
 * no sysnet is found and a new one is created, the confidence is 1.0.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class DummySenseFinder implements SenseFinder
{
	private boolean autoCreate = true;
	
	private Class<?> getSynsetType(FeatureNode word)
	{
		String pos = word.featureValue("POS");
		if ("verb".equals(pos))
			return VerbSynsetLink.class;
		else if ("adj".equals(pos))
			return AdjSynsetLink.class;
		else if ("adv".equals(pos))
			return AdverbSynsetLink.class;
		else if ("noun".equals(pos))
			return NounSynsetLink.class;
		else
			return null;
	}
	
	public Pair<HGHandle, Double> findSense(AnalysisContext<TextDocument> ctx,
											FeatureNode sentence, 
											FeatureNode word,
											HGHandle wordHandle)
	{
		Class<?> synsetType = getSynsetType(word);
		if (synsetType == null)
			return null;
		List<HGHandle> all = hg.findAll(ctx.getGraph(), 
					 				    hg.and(hg.type(synsetType), hg.incident(wordHandle)));
		if (all.size() == 0)
		{
			if (autoCreate)
			{
				try
				{
					Constructor<?> c = synsetType.getConstructor(new Class[] { HGHandle[].class });
					System.out.println("No synset found for word " + ctx.getGraph().get(wordHandle) + " creating new.");
					return new Pair<HGHandle, Double>(
						ctx.getGraph().add(c.newInstance(new Object[] { new HGHandle[] { wordHandle }})),
						1.0);
				}
				catch (Throwable t)
				{
					throw new RuntimeException(t);
				}
			}
			else
				return null;
		}
		else if (all.size() == 1)
		{
//			System.out.println("Found synset " + ((SynsetLink)ctx.getGraph().get(all.get(0))).getGloss() +  
//					" for word " + ctx.getGraph().get(wordHandle));			
			return new Pair<HGHandle, Double>(all.get(0), 0.95);
		}
		else
		{
//			System.out.println("Found " + all.size() + " synsets for word " + 
//					ctx.getGraph().get(wordHandle));						
			return new Pair<HGHandle, Double>(all.get(0), 1.0/(double)all.size());
		}
	}

	public boolean isAutoCreate()
	{
		return autoCreate;
	}

	public void setAutoCreate(boolean autoCreate)
	{
		this.autoCreate = autoCreate;
	}	
}
