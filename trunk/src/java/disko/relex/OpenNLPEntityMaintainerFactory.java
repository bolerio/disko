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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import disko.DU;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.namefind.NameFinder;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;
import relex.corpus.EntityMaintainerFactory;
import relex.entity.EntityInfo;
import relex.entity.EntityMaintainer;
import relex.entity.EntityType;

/**
 * 
 * <p>
 * Similar to the GateEntityMaintainer, but uses OpenNLP instead.
 * </p>
 *
 * TODO: OpenNLP allows one to provide a map of already found entities in previous
 * sentences. This map can be maintained on a per document basis. Not sure how that
 * improves OpenNLP alone (perhaps just speed), but when combined with entities found
 * by GATE, it could also improve accuray. Need to play with and test this... 
 * 
 * @author Borislav Iordanov
 *
 */
public class OpenNLPEntityMaintainerFactory extends EntityMaintainerFactory
{
	private String modelsPath;
	private boolean initialized = false;
	private HashMap<String, NameFinder> finders = new HashMap<String, NameFinder>();

	private synchronized void init()
	{
		if (initialized)
			return;
		if (getModelsPath() == null)
			throw new RuntimeException("Please specify the path to OpenNLP named entity models.\n" +
					"Either use the opennlp.model.namedentity system property or make sure they are located \n" +
					"under [disco.home]/data/namedentity where the disco.home system property points to the Disco installation.");
		File dir = new File(getModelsPath());
		DU.log.info("Using OpenNLP named entity files: " + dir.getAbsolutePath());
		try
		{
			for (File f : dir.listFiles())
				if (f.getAbsolutePath().endsWith(".gz"))
				{
					String kind = f.getName().substring(0, f.getName().indexOf('.'));
					finders.put(kind, new opennlp.tools.lang.english.NameFinder(
										new SuffixSensitiveGISModelReader(f).getModel()));
				}
		}
		catch (Exception ex)
		{
			throw new RuntimeException("Failed to load OpenNLP named entity models.", ex);
		}
		initialized = true;
	}
	
	private EntityInfo getEntityInfo(String kind, String sentence, Span start, Span end)
	{
		if ("date".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.DATE);
		else if ("location".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.LOCATION);
		else if ("money".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.MONEY);			
		else if ("organization".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.ORGANIZATION);			
		else if ("percentage".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.PUNCTUATION);			
		else if ("person".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.PERSON);			
		else if ("time".equals(kind))
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.DATE);			
		else
			return new EntityInfo(sentence, start.getStart(), end.getEnd(), EntityType.GENERIC);
	}
	
	public EntityMaintainer makeEntityMaintainer(String sentence)
	{
		init();
		ArrayList<EntityInfo> entities = new ArrayList<EntityInfo>();		
	    Span[] spans = opennlp.tools.lang.english.NameFinder.tokenizeToSpans(sentence);
	    String[] tokens = opennlp.tools.lang.english.NameFinder.spansToStrings(spans,sentence);
	    for (Map.Entry<String, NameFinder> finder : finders.entrySet())
	    {
	        String [] L = finder.getValue().find(tokens, new HashMap<String, String>());
	        String prev = NameFinderME.OTHER;
	        int startToken = 0;
	        for (int j = 0; j < tokens.length; j++)
	        {
	            String curr = L[j];
	            if (!prev.equals(NameFinderME.OTHER) && !curr.equals(NameFinderME.CONTINUE))
	            	entities.add(getEntityInfo(finder.getKey(), 
	            							   sentence, 
	            							   spans[startToken], 
	            							   spans[j - 1]));
	            if (curr.equals(NameFinderME.START))
	            	startToken = j;
	            prev = curr;
	        }
	    }		
		return new EntityMaintainer(sentence, entities);
	}

	public String getModelsPath()
	{
		if (modelsPath == null)
		{
			modelsPath = System.getProperty("opennlp.model.namedentity");
			if (modelsPath == null)
			{
				modelsPath = System.getProperty("disco.home");
				if (modelsPath != null)
					modelsPath += "data/namedentity";
			}
		}
		return modelsPath;
	}

	public void setModelsPath(String modelsPath)
	{
		this.modelsPath = modelsPath;
	}
}
