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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.atom.HGSubsumes;
import org.hypergraphdb.HGTypeSystem;
import org.hypergraphdb.HyperGraph;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

import disko.flow.analyzers.hgdb.HGDBSaver;

/**
 * 
 * <p>
 * Use the Jena Semantic Framework to import a OWL file in HGDB.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class OWLImporter
{
	private static Log log = LogFactory.getLog(HGDBSaver.class);
	
	private HyperGraph graph;
	private String namespace;
	HGHandle ontologyHandle = null;	
	
	private static final String TYPE_PREDICATE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	
	private void prepare()
	{		
		log.debug("Preparing OWL import, creating types etc.");
		OWLClassConstructor owlClassConstructor = graph.get(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE);
		if (owlClassConstructor == null)
		{
			owlClassConstructor = new OWLClassConstructor();
			owlClassConstructor.setHyperGraph(graph);
			graph.getTypeSystem().addPredefinedType(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE, 
												    owlClassConstructor, 
												    OWLClass.class);
		}
		graph.getTypeSystem().getTypeHandle(OWLOntology.class);
		graph.getTypeSystem().getTypeHandle(OntologyMember.class);
		graph.getTypeSystem().getTypeHandle(OWLDomainLink.class);
		graph.getTypeSystem().getTypeHandle(OWLProperty.class);		
		log.debug("Done with OWL import preparation.");
	}
	
	private HGHandle getPrimitiveType(String url)
	{
		HGTypeSystem ts = graph.getTypeSystem();
		if (url.indexOf("XMLSchema") < 0)
			return null;
		else if (url.endsWith("#string"))
			return ts.getTypeHandle(String.class);
		else if (url.endsWith("#int"))
			return ts.getTypeHandle(Integer.class);
		else if (url.endsWith("#boolean"))
			return ts.getTypeHandle(Boolean.class);
		else if (url.endsWith("#float"))
			return ts.getTypeHandle(Float.class);
		else if (url.endsWith("#double"))
			return ts.getTypeHandle(Double.class);
		else if (url.endsWith("#date") || url.endsWith("#dateTime") || url.endsWith("#time"))
			return ts.getTypeHandle(java.util.Date.class);
		else if (url.endsWith("#anyURI"))
			return ts.getTypeHandle(java.net.URI.class);		
		else
			throw new IllegalArgumentException("Unkown primitive XML Schema type: " + url);
	}
	
	private java.util.Date parseDate(String s) throws Exception
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.parse(s);
	}
	
	private HGHandle getLiteralAtom(String literal)
	{
		String [] parts = literal.split("\\^\\^");		
		HGHandle type = getPrimitiveType(parts[1]);
		if (type == null)
			return null;		
		
		Object value = null;
		if (parts[1].endsWith("#string"))
			value = parts[0];
		else if (parts[1].endsWith("#int"))
			try { value = Integer.parseInt(parts[0]); } catch (Exception ex) { throw new RuntimeException(ex); }
		else if (parts[1].endsWith("#boolean"))
			value = Boolean.valueOf(parts[0]);
		else if (parts[1].endsWith("#float"))
			try { value = Float.parseFloat(parts[0]); } catch (Exception ex) { throw new RuntimeException(ex); }
		else if (parts[1].endsWith("#double"))
			try { value = Double.parseDouble(parts[0]); } catch (Exception ex) { throw new RuntimeException(ex); }
		else if (parts[1].endsWith("#date") || parts[1].endsWith("#dateTime") || parts[1].endsWith("#time"))
			try { value = parseDate(parts[0]); } catch (Exception ex) { throw new RuntimeException(ex); }
		else if (parts[1].endsWith("#anyURI"))
			try { value = new java.net.URI(parts[0]); } catch (Exception ex) { throw new RuntimeException(ex); }		
		
		if (value == null)
			return null;
		
		HGHandle result = null;		
		result = hg.findOne(graph, hg.and(hg.type(type), hg.eq(value)));
		if (result == null)
			result = graph.add(value);		
		return result;
	}
	
	private void getSimpleOntClassSet(Iterator<?> ontClassIterator, Set<HGHandle> set)
	{
		while (ontClassIterator.hasNext())
		{
			OntClass dom = (OntClass)ontClassIterator.next();			
			if (dom.isUnionClass())
				getSimpleOntClassSet(dom.asUnionClass().listOperands(), set);
			else if (!dom.isAnon())
			{
				HGHandle clHandle = hg.findOne(graph, hg.and(hg.type(OWLClass.class), 
						 hg.eq("localName", dom.getLocalName())));
				if (clHandle == null)
					throw new RuntimeException("Could not find handle for domain " + dom.getLocalName());
				log.debug("Found non-anonymous domain : " + dom);
				set.add(clHandle);				
			}				
		}
	}
	
	private void importProperty(OntProperty p)
	{
		log.debug("Importing property " + p);
		if (p.getRange() == null && p.isDatatypeProperty())
			throw new RuntimeException("WTF? datatype property '" + p.getLocalName() +  "' has no range!");
		Set<HGHandle> domain = new HashSet<HGHandle>();
		getSimpleOntClassSet(p.listDomain(), domain);
		if (domain.isEmpty())
			domain.add(graph.getTypeSystem().getTop());
		HGHandle rangeHandle = null;
		if (p.isObjectProperty())
			if (p.getRange() != null && !p.getRange().isAnon())
			{
				rangeHandle = hg.findOne(graph, hg.and(hg.type(OWLClass.class), 
													   hg.eq("localName", p.getRange().getLocalName())));
				log.debug("Property range is an OWL object: " + p.getRange().getLocalName());
			}
			else
			{
				log.debug("Object property range is top level OWL::Thing");
				rangeHandle = graph.getTypeSystem().getTop();
			}
		else
		{
			rangeHandle = getPrimitiveType(p.getRange().toString());
			log.debug("Property range is a primitive " + p.getRange());
		}
		if (rangeHandle == null)
			throw new RuntimeException("Could not find handle for range " + p.getRange() + " of property " + p);
		OWLProperty owlProp = new OWLProperty();
		owlProp.setLocalName(p.getLocalName());
		owlProp.setHyperGraph(graph);
		owlProp.setRangeType(rangeHandle);
		HGHandle propHandle = graph.add(owlProp);		
		graph.getTypeSystem().addAlias(propHandle, p.getURI());
		graph.add(new OntologyMember(ontologyHandle, propHandle));
		log.debug("Added property " + p.getURI() + " at handle " + propHandle);		
		for (HGHandle dh : domain)
		{			
			graph.add(new OWLDomainLink(propHandle, dh));
			log.debug("Domain linked property " + propHandle + " and " + dh);
		}
	}
	
	private HGHandle importClass(OntClass ontcl, Set<OntClass> occurs)
	{
		occurs.add(ontcl);
		log.debug("Importing class " + ontcl);
		OWLClass cl = new OWLClass();
		cl.setLocalName(ontcl.getLocalName());
		HGHandle classHandle = graph.add(cl);
		cl.setHyperGraph(graph);
		graph.getTypeSystem().addAlias(classHandle, ontcl.getURI());
		graph.add(new OntologyMember(ontologyHandle, classHandle));
		log.debug("Class imported.");
		for (Iterator<?> i = ontcl.listSubClasses(true); i.hasNext(); )
		{
			OntClass sub = (OntClass)i.next();
			if (!sub.isAnon() && !occurs.contains(sub))
			{
				log.debug("Non-anonymous and not already imported sub-class found: " + sub);
				HGHandle subHandle = importClass(sub, occurs);
				graph.add(new HGSubsumes(classHandle, subHandle));
			}
		}
		return classHandle;
	}
	
	private void importIndividuals(OntModel m)
	{
		log.debug("Importing individuals...");
		Map<String, HGHandle> handles = new HashMap<String, HGHandle>();
		for (Iterator<?> i = m.listIndividuals(); i.hasNext(); )
		{
			Individual ind = (Individual)i.next();
			Iterator<?> types = ind.listOntClasses(true);
			if (!types.hasNext()) 
				throw new RuntimeException("WTF? Individual " + ind + " has not type.");
			OntClass type = (OntClass)types.next();
			if (types.hasNext())
			{
				System.err.println("WARN: individuals with more than one class not supported yet, using only first -- " + ind);
	//			throw new RuntimeException("Only one class can be the type of an individual: " + ind);
			}
			OWLClass owlClass = (OWLClass)graph.getTypeSystem().getType(type.getURI());
			if (owlClass == null)
				throw new RuntimeException("Could not find OWLClass for type " + type.getURI());
			OWLIndividual owlInd = new OWLIndividual(owlClass);
			owlInd.setLocalName(ind.getLocalName());
			HGHandle indHandle = graph.add(owlInd, graph.getHandle(owlClass));
			graph.add(new OntologyMember(ontologyHandle, indHandle));
			handles.put(ind.getURI(), indHandle);
			log.debug("Added individual " + ind.getURI() + " with handle " + indHandle);
		}		
		for (Iterator<?> i = m.listIndividuals(); i.hasNext(); )
		{
			Individual ind = (Individual)i.next();
			log.debug("Examining properties of individual " + ind);
			HGHandle indHandle = handles.get(ind.getURI());
			for (Iterator<?> j = ind.listProperties(); j.hasNext(); )
			{
				Statement stmt = (Statement)j.next();
				HGHandle propHandle = graph.getTypeSystem().getTypeHandle(stmt.getPredicate().getURI());
				if (propHandle == null)					
				{
					if (!stmt.getPredicate().getURI().equals(TYPE_PREDICATE))
						log.warn("WARN: no property handle for " + stmt.getPredicate());
					continue;
				}
				log.debug("Found property " + stmt.getPredicate().getURI());
				HGHandle valueAtom = null;
				if (stmt.getObject().isLiteral())
					valueAtom = getLiteralAtom(stmt.getObject().toString());
				else
				{
					valueAtom = handles.get(stmt.getObject().toString());
					if (valueAtom == null)
						// try classes...
						valueAtom = graph.getTypeSystem().getTypeHandle(stmt.getObject().toString());
				}
				if (valueAtom == null)
					throw new RuntimeException("Could not get property value atom for individual " + ind + " and property stmt "+ stmt);
				graph.add(new OWLPropertyInstance(indHandle, valueAtom), propHandle);
				log.debug("Added property with value " + valueAtom + " -- " + stmt.getObject());
			}
		}
	}
	
	private void importAll(OntModel m)
	{		
		if (namespace == null || namespace.equals(""))
			namespace = (String)m.getNsPrefixMap().get("");
		if (namespace == null || namespace.equals(""))
			throw new RuntimeException("Either set the namespace property " + 
					"of OWLImporter or make sure there's a default namespace in the OWL file.");
		
		prepare();
		
		ontologyHandle = hg.findOne(graph, hg.and(hg.type(OWLOntology.class), 
												  hg.eq("namespaceUrl", namespace)));
		
		if (ontologyHandle != null)			
			throw new RuntimeException("An ontology with namespace '" + 
					namespace + "' has already been uploaded. Please remove it first before reuploading.");
		
		ontologyHandle = graph.add(new OWLOntology(namespace));
		
		for (Iterator<?> i = m.listHierarchyRootClasses(); i.hasNext(); )
		{
			OntClass cl = (OntClass)i.next();
			if (!cl.isAnon())
				importClass(cl, new HashSet<OntClass>());
		}
		for (Iterator<?> i = m.listAllOntProperties(); i.hasNext(); )
		{
			OntProperty p = (OntProperty)i.next();
			if (p.isDatatypeProperty() || p.isObjectProperty())
				importProperty(p);
		}
		importIndividuals(m);
	}
	
	public void removeClass(HGHandle clHandle)
	{
		List<HGHandle> individuals = hg.findAll(graph, hg.type(clHandle));
		for (HGHandle h : individuals)
		{
			for (HGHandle incident : graph.getIncidenceSet(h))
				graph.remove(incident);
			graph.remove(h);
		}
		for (HGHandle incident : graph.getIncidenceSet(clHandle))
			graph.remove(incident);
		graph.remove(clHandle);
	}
	
	public void removeExisting(String url)
	{
		List<HGHandle> classes = hg.findAll(graph, hg.type(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE));
		for (HGHandle h:classes)
		{
			Set<String> aliases = graph.getTypeSystem().findAliases(h);			
			OWLClass cl = graph.get(h);
			for (String alias : aliases)
				graph.getTypeSystem().removeAlias(alias);						
			if (aliases.contains(url + "#" + cl.getLocalName()))
				removeClass(h);
		}
		List<HGHandle> properties = hg.findAll(graph, hg.type(OWLProperty.class));
		for (HGHandle h:properties)
		{
			Set<String> aliases = graph.getTypeSystem().findAliases(h);
			for (String alias : aliases)
				graph.getTypeSystem().removeAlias(alias);
			graph.remove(h);
		}
		HGHandle ontologyHandle = hg.findOne(graph, hg.and(hg.type(OWLOntology.class), 
				  hg.eq("namespaceUrl", namespace)));
		if (ontologyHandle != null)			
			graph.remove(ontologyHandle);
	}
	
	public void importOntology(File location)
	{
		try
		{
			importOntology(namespace, location);
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public void importOntology(String url)
	{
		OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		m.read(url);
		importAll(m);		
	}
	
	public void importOntology(String url, File location)
	{
		try
		{
			OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null );
			m.getDocumentManager().addAltEntry(url.toString(), location.toURI().toURL().toString());
			m.read(url);
			importAll(m);
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
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
}
