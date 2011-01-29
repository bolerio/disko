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

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;

import disko.flow.analyzers.XQueryAnalyzer;

public class XQueryHTMLPipeline
{	
	private HyperGraph graph;
	@SuppressWarnings("unused")
	private HyperGraph countGraph; 
	
	private DataFlowNetwork<AnalysisContext<TextDocument>> network = null;
	
	public void shutdown(){
		network.shutdown();
	}
	
	private void buildNetwork()
	{
		network = new DataFlowNetwork<AnalysisContext<TextDocument>>();

		network.addNode(new XQueryAnalyzer(), 
				new String[] {},
				new String[] {});
	}
	
	public XQueryHTMLPipeline(HyperGraph graph, HyperGraph countGraph)
	{
		this.graph = graph;
		this.countGraph = countGraph; 
		buildNetwork();
	}
	
	public Future<?> processDocument(TextDocument doc)
	{
		AnalysisContext<TextDocument> ctx = new AnalysisContext<TextDocument>(graph, doc);
		ctx.pushScoping(graph.add(doc));
		return network.start(ctx);
	}

	public static void processDir(HyperGraph graph, HyperGraph countGraph,
			File htmlDir) throws InterruptedException, ExecutionException {
		final File[] xqFiles = htmlDir.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".html");
			}});
		for (File file: xqFiles){
			process(graph, countGraph, file);
		}
	}

	public static void process(HyperGraph graph, HyperGraph countGraph,
			File file) throws InterruptedException, ExecutionException {
		HTMLDocument doc = new HTMLDocument(file);
		XQueryHTMLPipeline pipeline = new XQueryHTMLPipeline(graph, countGraph);
		pipeline.processDocument(doc).get();
		pipeline.shutdown();
	}
	
	
	public static void main(String[] args) throws InterruptedException, ExecutionException{
		HyperGraph graph = HGEnvironment.get("../hgdb_main");
		HyperGraph countGraph = HGEnvironment.get("../hgdb_counts");
		
		processDir(graph, countGraph, new File("test/xquery"));
		
//		process(graph, countGraph, new File("test/xquery/county_departments_and_agencies.html"));
//		process(graph, countGraph, new File("test/xquery/taxcollector_contact.html"));
//		process(graph, countGraph, new File("test/xquery/animals_contact.html"));
//		process(graph, countGraph, new File("test/xquery/csd_contact.html"));
//		process(graph, countGraph, new File("test/xquery/library_al.html"));

		graph.close();
		countGraph.close();
	}
}
