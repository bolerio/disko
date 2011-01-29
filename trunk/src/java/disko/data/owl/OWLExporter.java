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
package disko.data.owl;

import java.io.File;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.atom.HGSubsumes;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;

@SuppressWarnings("unchecked")
public class OWLExporter
{
//	private static Log log = LogFactory.getLog(OWLExporter.class);
	
	private HyperGraph graph;
	private String namespace;
	private File destinationFile;
	
	private Map<HGHandle, Resource> exported = new HashMap<HGHandle, Resource>();
	
	private void addPrimitives()
	{
		HGHandle typeHandle = graph.getTypeSystem().getTypeHandle(String.class);
		exported.put(typeHandle, ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#string"));
		exported.put(graph.getTypeSystem().getTop(), OWL.Thing);
	}
	
	/**
	 * <p>
	 * Return the resource corresponding to the given atom.
	 * </p>
	 * 
	 * @param atom
	 * @return
	 */
	private Resource findResourceFor(HGHandle atom, OntModel m)
	{
		Resource r = (Resource)exported.get(atom);
		if (r != null)
			return r;
		Object x = graph.get(atom);
		if (x instanceof OWLClass)
		{
			exportClass((OWLClass)x, m);
			r = exported.get(atom);
		}
		else if (x instanceof OWLProperty)
		{
			exportProperty((OWLProperty)x, m);
			r = exported.get(atom);
		}
		else if (x instanceof OWLIndividual)
		{
			exportIndividual((OWLIndividual)x, m);
			r = exported.get(atom);
		}		
		if (r == null)
			throw new RuntimeException("Unable to find resource for atom " + graph.getPersistentHandle(atom));
		return r;
	}
	
	private void exportProperty(OWLProperty prop, OntModel m)
	{
		Resource range = findResourceFor(prop.getRangeType(), m);
		OntProperty mp = null;
		if (range instanceof OWLClass || 
			prop.getRangeType().equals(graph.getTypeSystem().getTop()))
			mp = m.createObjectProperty(namespace + prop.getLocalName());
		else
			mp = m.createDatatypeProperty(namespace + prop.getLocalName());
		mp.setRange(range);		
		if (prop.getDomain().size() == 1)
			mp.addDomain(findResourceFor(prop.getDomain().iterator().next(), m));
		else 
		{
			RDFNode [] nodes = new RDFNode[prop.getDomain().size()]; 
			int i = 0;
			for (HGHandle dom : prop.getDomain())
				nodes[i++] = findResourceFor(dom, m);
			mp.addDomain(m.createUnionClass(null, m.createList(nodes)));
		}		
		exported.put(graph.getHandle(prop), mp);
	}
	
	private void exportClass(OWLClass cl, OntModel m)
	{
		HGHandle clHandle = graph.getHandle(cl);
		OntClass mcl = m.createClass(namespace + cl.getLocalName());
		List<HGHandle> superClasses = hg.findAll(graph, 
				hg.apply(hg.targetAt(graph, 0),
						hg.and(hg.type(HGSubsumes.class),
							   hg.orderedLink(hg.anyHandle(), clHandle))));
		for (HGHandle sup : superClasses)
		{
			Object x = graph.get(sup);
			if (! (x instanceof OWLClass))
				continue;
			mcl.addSuperClass(findResourceFor(sup, m));
		}		
		exported.put(clHandle, mcl);
	}
	
	private void exportIndividual(OWLIndividual ind, OntModel m)
	{
		HGHandle indHandle = graph.getHandle(ind);
		HGHandle clHandle = graph.getHandle(ind.getOwlClass());
		Resource clR = findResourceFor(clHandle, m);
		Individual mind = m.createIndividual(namespace + ind.getLocalName(), clR);
		mind.setOntClass(clR);
		exported.put(indHandle, mind); // added here to prevent a circular reference loop
		for (OWLProperty propType : ind.getOwlClass().getProperties().values())
		{
			OntProperty p = (OntProperty)findResourceFor(graph.getHandle(propType), m);			
			Set<Object> all = null;
			Object propValue = ind.getProperties().get(propType.getLocalName());
			if (propValue == null)
				continue;
			if (propValue instanceof Set)
				all = (Set<Object>)propValue;
			else
			{
				all = new HashSet<Object>();
				all.add(propValue);
			}
			if (p.isObjectProperty())
				for (Object x : all)
					mind.addProperty(p, findResourceFor(graph.getHandle(x), m));
			else
				for (Object x : all)
					mind.addProperty(p, x.toString());
		}		
	}
	
	public OWLExporter()
	{
		
	}
	
	public OWLExporter(HyperGraph graph, String namespace, File destinationFile)
	{
		this.graph = graph;
		this.namespace = namespace;
		this.destinationFile = destinationFile;
	}
	
	public HyperGraph getGraph()
	{
		return graph;
	}

	public void setGraph(HyperGraph graph)
	{
		this.graph = graph;
	}
	
	public String getNamespace()
	{
		return namespace;
	}

	public void setNamespace(String namespace)
	{
		this.namespace = namespace;
	}

	public File getDestinationFile()
	{
		return destinationFile;
	}

	public void setDestinationFile(File destinationFile)
	{
		this.destinationFile = destinationFile;
	}

	public void export()
	{
		if (graph == null || namespace == null || destinationFile == null)
			throw new RuntimeException("The HGDB graph, namespace of the ontology and destination file " + 
					" must be set before attempting to export.");
		exported = new HashMap<HGHandle, Resource>();
		addPrimitives();
		HGHandle ontologyHandle = hg.findOne(graph, hg.and(hg.type(OWLOntology.class), 
				  										   hg.eq("namespaceUrl", namespace)));
		if (ontologyHandle == null)
			throw new RuntimeException("Could not find ontology '" + namespace + "' in HGDB " + graph.getLocation());
		List<HGHandle> allObjects = hg.findAll(graph,
				hg.apply(hg.targetAt(graph, 1),hg.and(hg.type(OntologyMember.class),
												      hg.incident(ontologyHandle))));
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		m.setNsPrefix("", namespace);
		for (HGHandle h: allObjects)
		{
			if (exported.containsKey(h))
				continue;
			Object x = graph.get(h);
			if (x instanceof OWLProperty)
			{
				exportProperty((OWLProperty)x, m);
			}
			else if (x instanceof OWLClass)
			{
				exportClass((OWLClass)x, m);
			}
			else if (x instanceof OWLIndividual)
			{
				exportIndividual((OWLIndividual)x, m);
			}
		}

		FileWriter out = null;
		try
		{
			out = new FileWriter(destinationFile);
			m.write(out, "RDF/XML-ABBREV");			
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
		finally
		{
			if (out != null)
				try { out.close(); } catch (Throwable t) { }
		}
	}
}
