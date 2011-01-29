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

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.util.Pair;

import disko.AnalysisContext;
import disko.TextDocument;

import relex.feature.FeatureNode;

/**
 * 
 * <p>
 * Define the interface for sense disambiguation algorithms within Disco. An
 * algorithm is required to find the appropriate WordNet synset for a lemma and 
 * return its HGDB handle together with a confidence score. 
 * </p>
 *
 * NOTE: this interface is tentative...it will be probably refined and some algos get
 * implemented and requirements evolve.
 * 
 * @author Borislav Iordanov
 *
 */
public interface SenseFinder
{
	/**
	 * <p>
	 * Return the WordNet synset representing the sense of <code>word</code>
	 * within the sentence <code>sentence</code>.
	 * </p>
	 * 
	 * @param ctx The current analysis context.
	 * @param sentence The whole sentence where the word appears. This is usually
	 * obtained from the left wall of a parsed sentence processed by the relation 
	 * extractor.
	 * @param word The node of the word itself. It should contain its assigned 
	 * part-of-speech by the parsing and relex algorithms.
	 * @param wordHandle The WordNet-HGDB handle of the actual word lemma.
	 * 
	 */
	Pair<HGHandle, Double> findSense(AnalysisContext<TextDocument> ctx, 
									 FeatureNode sentence, 
									 FeatureNode word,
									 HGHandle wordHandle);
}
