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
package disko.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Set;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;

import disko.data.owl.*;

/**
 * 
 * <p>
 *
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
@SuppressWarnings("unchecked")
public class OWLIndividualsToGate
{
	static void updateListDef(File gateHome) throws Exception
	{
		File listDefFile = new File(gateHome, "lists.def");
		if (!listDefFile.exists())
			throw new RuntimeException(listDefFile.getAbsolutePath() + 
					" is missing - this is either an unsupported or a corrupted GATE installation");
		File tmpFile = new File(gateHome, "lists.def.discotmp");
		BufferedReader reader = new BufferedReader(new FileReader(listDefFile));
		FileWriter writer = new FileWriter(tmpFile);
		writer.write("disco.lst:disco\n");
		for (String line = reader.readLine(); line != null; line = reader.readLine())
		{
			if (line.startsWith("disco.lst"))
				continue;
			writer.write(line + "\n");
		}
		writer.close();
		reader.close();
		listDefFile.delete();
		tmpFile.renameTo(listDefFile);
	}
	
	static void generateDiscoList(File gateHome, HyperGraph graph) throws Exception
	{
		File discoFile = new File(gateHome, "disco.lst");
		FileWriter writer = new FileWriter(discoFile);
		String [] classes = new String []
		{
			"Organization",
			"Person",
			"Landmark"
		};
		OWLProperty nameProperty = hg.getOne(graph, hg.and(hg.type(OWLProperty.class), 
														   hg.eq("localName", "Name")));
		OWLProperty aliasProperty = hg.getOne(graph, hg.and(hg.type(OWLProperty.class), 
															hg.eq("localName", "Alias")));
		for (String localName : classes)
		{
			List<HGHandle> L = hg.findAll(graph, hg.and(hg.type(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE),
													    hg.eq("localName", localName)));
			for (HGHandle classHandle : L)
			{
				
				List<OWLIndividual> individuals = hg.getAll(graph, hg.typePlus(classHandle));
				for (OWLIndividual ind : individuals)
				{
					String name = (String)ind.getProperties().get(nameProperty.getLocalName());
					if (name != null)
						writer.write(name + "&HGHANDLE=" + graph.getPersistentHandle(graph.getHandle(ind)) + "\n");
					Object aliases = ind.getProperties().get(aliasProperty.getLocalName());
					if (aliases != null)
					{
						if (aliases instanceof String)
							writer.write((String)aliases + "&HGHANDLE=" + graph.getPersistentHandle(graph.getHandle(ind)) + "\n");
						else for (String alias : (Set<String>)aliases)
							writer.write(alias + "&HGHANDLE=" + graph.getPersistentHandle(graph.getHandle(ind)) + "\n");
					}
				}
			}
		}
		writer.close();
	}
	
	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.out.println("Syntax: OWLIndividualsToGate graphLocation gateHome");
			System.exit(0);
		}
		try
		{
			if (!HGEnvironment.exists(args[0]))
				throw new Exception("There doesn't seem to be a HyperGraph database at " + args[0]);
			File annieDir = new File(args[1], "plugins/ANNIE/resources/gazetteer");
			if (!annieDir.exists())
				throw new Exception("There doesn't seem to be a GATE installation with ANNIE plugin at " + args[1]);
			generateDiscoList(annieDir, HGEnvironment.get(args[0]));
			updateListDef(annieDir);
			System.exit(0);
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			System.exit(-1);			
		}
	}
}
