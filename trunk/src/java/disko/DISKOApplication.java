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
package disko;

//import org.disco.data.relex.RelOccurrence;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.management.HGApplication;
import org.hypergraphdb.indexing.ByPartIndexer;
import org.hypergraphdb.indexing.TargetToTargetIndexer;
import org.hypergraphdb.type.TypeUtils;

import disko.data.BasicWords;
import disko.data.NamedEntity;
import disko.data.UnknownWord;
import disko.data.owl.OWLClass;
import disko.data.owl.OWLClassConstructor;
import disko.data.relex.RelOccurrence;
import disko.data.relex.RelationCount;
import disko.data.relex.RelationInfo;
import disko.data.relex.RelexWord;
import disko.data.relex.SemRel;
import disko.data.relex.SynRel;
import disko.data.relex.SyntacticPredicate;
import disko.data.relex.WordInfo;

public class DISKOApplication extends HGApplication
{
	private void createIndices(HyperGraph graph)
	{
		HGHandle typeH = graph.getTypeSystem().getTypeHandle(UrlTextDocument.class);
		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "urlString" }));
		typeH = graph.getTypeSystem().getTypeHandle(NamedEntity.class);
		graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "name" }));
		graph.getIndexManager().register(new ByPartIndexer(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE, new String[] { "localName" }));		
		typeH = graph.getTypeSystem().getTypeHandle(ScopeLink.class);
		graph.getIndexManager().register(new TargetToTargetIndexer(typeH, 1, 0));
        typeH = graph.getTypeSystem().getTypeHandle(UnknownWord.class); // since sub-types don't get indexed because of a super-type
        graph.getIndexManager().register(new ByPartIndexer(typeH, new String[] { "lemma" }));		
	}
	
	private void removeIndices(HyperGraph graph)
	{
		HGHandle typeH = graph.getTypeSystem().getTypeHandle(DefaultTextDocument.class);
		graph.getIndexManager().unregister(new ByPartIndexer(typeH, new String[] { "urlString" }));
		typeH = graph.getTypeSystem().getTypeHandle(NamedEntity.class);
		graph.getIndexManager().unregister(new ByPartIndexer(typeH, new String[] { "name" }));
		graph.getIndexManager().unregister(new ByPartIndexer(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE, new String[] { "localName" }));		
		typeH = graph.getTypeSystem().getTypeHandle(ScopeLink.class);
		graph.getIndexManager().unregister(new TargetToTargetIndexer(typeH, 1, 0));
        typeH = graph.getTypeSystem().getTypeHandle(UnknownWord.class); // since sub-types don't get indexed because of a super-type
        graph.getIndexManager().unregister(new ByPartIndexer(typeH, new String[] { "lemma" }));    
	}
	
	public void install(HyperGraph graph)
	{
		SyntacticPredicate.loadAll(graph);
		BasicWords.loadAll(graph);
		OWLClassConstructor owlClassConstructor = new OWLClassConstructor();
        owlClassConstructor.setHyperGraph(graph);
        graph.getTypeSystem().addPredefinedType(OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE, 
                                                owlClassConstructor, 
                                                OWLClass.class);
		
		createIndices(graph);
	}

	public void reset(HyperGraph graph)
	{
		// TODO Auto-generated method stub
		
	}

	public void uninstall(HyperGraph graph)
	{		
		TypeUtils.deleteType(graph, RelationCount.class, true);
		TypeUtils.deleteType(graph, RelationInfo.class, true);
		TypeUtils.deleteType(graph, RelexWord.class, true);
		TypeUtils.deleteType(graph, RelOccurrence.class, true);
		TypeUtils.deleteType(graph, SemRel.class, true);
		TypeUtils.deleteType(graph, SynRel.class, true);
		TypeUtils.deleteType(graph, WordInfo.class, true);
		TypeUtils.deleteType(graph, NamedEntity.class, true);
		TypeUtils.deleteType(graph, TextDocument.class, true);
		TypeUtils.deleteType(graph, Ann.class, true);
        BasicWords.unloadAll(graph);		
		SyntacticPredicate.deleteAll(graph);
		removeIndices(graph);
		TypeUtils.deleteType(graph, OWLClassConstructor.OWL_CLASS_CONSTRUCTOR_HANDLE, true);
	}

	public void update(HyperGraph graph)
	{
		// TODO Auto-generated method stub
		
	}
	
	public DISKOApplication()
	{
		setName("discoApp");
	}
}
