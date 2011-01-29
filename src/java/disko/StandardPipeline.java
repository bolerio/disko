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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;

import disko.data.relex.SentenceInterpretation;
import disko.flow.analyzers.AnaphoraAnalyzer;
import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.FullRelexAnalyzer;
import disko.flow.analyzers.ParseSelectAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.ToRelOccAnalyzer;
import disko.flow.analyzers.hgdb.HGDBSaver;
import disko.flow.analyzers.socket.SocketTransmitter;

import relex.concurrent.RelexTaskResult;
import relex.entity.EntityMaintainer;

public class StandardPipeline
{	
	private HyperGraph graph;
	private HyperGraph countGraph; 
	
	private DataFlowNetwork<AnalysisContext<TextDocument>> network = null;
	
	public void shutdown(){
		network.shutdown();
	}
	
	private void buildNetwork()
	{
		network = new DataFlowNetwork<AnalysisContext<TextDocument>>();

		network.addChannel(new Channel<SentenceAnn>(
				SentenceAnalyzer.SENTENCE_CHANNEL, 
				new SentenceAnn(0, 0)));
		
		network.addChannel(new Channel<EntityMaintainer>(
				EntityAnalyzer.ENTITY_CHANNEL, 
				EntityAnalyzer.EMPTY_EM ));
		
		network.addChannel(new Channel<RelexTaskResult>(
				FullRelexAnalyzer.PARSE_CHANNEL, 
				new RelexTaskResult(-1,null,null,null)));

		network.addChannel(new Channel<SentenceInterpretation>(
				ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL, 
				new SentenceInterpretation(null, null)));

		network.addChannel(new Channel<Set<SentenceInterpretation>>(
				ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS, 
				new HashSet<SentenceInterpretation>()));
		
//		network.addChannel(new Channel<SentenceInterpretation>(
//				AnaphoraAnalyzer.ANAPHORA_CHANNEL, 
//				new SentenceInterpretation(null, null)));

		network.addNode(new SentenceAnalyzer(), 
				new String[] {},
				new String[] { SentenceAnalyzer.SENTENCE_CHANNEL });

		network.addNode(
				new EntityAnalyzer(), 
				new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL});
		
		network.addNode(
				new FullRelexAnalyzer("localhost", 9000 /*, "localhost", 9001 */),
				new String[]{EntityAnalyzer.ENTITY_CHANNEL}, 
				new String[]{FullRelexAnalyzer.PARSE_CHANNEL});

		network.addNode(
				new ToRelOccAnalyzer(), 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL, FullRelexAnalyzer.PARSE_CHANNEL}, 
				new String[]{ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS});

		network.addNode(
				new ParseSelectAnalyzer(countGraph), 
				new String[]{ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS, EntityAnalyzer.ENTITY_CHANNEL}, 
				new String[]{ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL});
		
//		network.addNode(
//				new AnaphoraAnalyzer(), 
//				new String[]{EntityAnalyzer.ENTITY_CHANNEL, ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL}, 
//				new String[]{AnaphoraAnalyzer.ANAPHORA_CHANNEL});
//
//		network.addNode(new HGDBSaver<SentenceAnn>(),
//				new String[] { SentenceAnalyzer.SENTENCE_CHANNEL,							   
//							   AnaphoraAnalyzer.ANAPHORA_CHANNEL }, 
//							   new String[] {});

		network.addNode(new HGDBSaver(),
				new String[] { SentenceAnalyzer.SENTENCE_CHANNEL, ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL }, 
				new String[] {});

		
// Uncomment with Ctrl+Shift+C
//		network.addNode(new ConsoleOutputAnalyzer(), 
//				new String[]{
//					SentenceAnalyzer.SENTENCE_CHANNEL, 
//					EntityAnalyzer.ENTITY_CHANNEL,
//					LinkGrammarAnalyzer.PARSE_CHANNEL,
//					AnaphoraAnalyzer.ANAPHORA_CHANNEL}, 
//				new String[]{}); 

		boolean TRANSMIT = false;
		if (TRANSMIT) {
			network.addNode(new SocketTransmitter<AnalysisContext<TextDocument>>("localhost", 10000), 
					new String[] { 
						SentenceAnalyzer.SENTENCE_CHANNEL, 
						EntityAnalyzer.ENTITY_CHANNEL,
						FullRelexAnalyzer.PARSE_CHANNEL,
						AnaphoraAnalyzer.ANAPHORA_CHANNEL },
					new String[] {});
		}		
	}
	
	public StandardPipeline(HyperGraph graph, HyperGraph countGraph)
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
	
	public static void main(String[] args) throws InterruptedException, ExecutionException{
		HyperGraph graph = HGEnvironment.get("../hgdb_main");
		HyperGraph countGraph = HGEnvironment.get("../hgdb_counts");
		
		DefaultTextDocument doc = new DefaultTextDocument(new File("test/headstart.txt"));
		
		StandardPipeline pipeline = new StandardPipeline(graph, countGraph);
		pipeline.processDocument(doc).get();
		pipeline.shutdown();
		graph.close();
		countGraph.close();
	}
}
