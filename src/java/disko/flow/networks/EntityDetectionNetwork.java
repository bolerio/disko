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
package disko.flow.networks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.PredefinedNetwork;

import disko.AnalysisContext;
import disko.DU;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.flow.analyzers.CombinedEntityAnalyzer;
import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.socket.SocketReceiver;
import disko.flow.analyzers.socket.SocketTransmitter;
import disko.relex.OpenNLPEntityMaintainerFactory;

import relex.corpus.GateEntityDetector;
import relex.entity.EntityMaintainer;

public class EntityDetectionNetwork extends PredefinedNetwork<AnalysisContext<TextDocument>>
{
	public static final String COMBENTITIES_CHANNEL = "COMBINED_ENTITIES";
	
	private int receivePort = -1, transmitPort = -1;
	private String destinationHost;
	
	
	public EntityDetectionNetwork()
	{		
	}
	
	public EntityDetectionNetwork(int receivePort, int transmitPort, String destinationHost)
	{
		this.receivePort = receivePort;
		this.transmitPort = transmitPort;
		this.destinationHost = destinationHost;
	}
	
	public void create()
	{
		addChannel(new Channel<SentenceAnn>(
				SentenceAnalyzer.SENTENCE_CHANNEL, 
				new SentenceAnn(0, 0)));		
		addChannel(new Channel<EntityMaintainer>(
				CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL, 
				EntityAnalyzer.EMPTY_EM ));
		addChannel(new Channel<EntityMaintainer>(
				CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL, 
				EntityAnalyzer.EMPTY_EM ));
		addChannel(new Channel<EntityMaintainer>(
				EntityAnalyzer.ENTITY_CHANNEL, 
				EntityAnalyzer.EMPTY_EM ));
		
		GateEntityDetector gateDetector = new GateEntityDetector();
		Map<Object, Object> gazParams = new HashMap<Object, Object>();
		gazParams.put("caseSensitive", Boolean.TRUE);
		gazParams.put("gazetteerFeatureSeparator", "&");
		gateDetector.getAnnieParams().put("gate.creole.gazetteer.DefaultGazetteer", gazParams);
		
		addNode(new SocketReceiver<AnalysisContext<TextDocument>>(receivePort, false), 
				new String[]{}, 
				new String[]{SentenceAnalyzer.SENTENCE_CHANNEL});
				
		addNode(new EntityAnalyzer(gateDetector, null, CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL), 
				new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
				new String[]{CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL});
		
		addNode(new EntityAnalyzer(new OpenNLPEntityMaintainerFactory(), null, CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL), 
				new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
				new String[]{CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL});

		addNode(new CombinedEntityAnalyzer(), 
				new String[]{CombinedEntityAnalyzer.GATE_ENTITY_CHANNEL, CombinedEntityAnalyzer.OPENNLP_ENTITY_CHANNEL}, 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL});
		
		addNode(new SocketTransmitter<AnalysisContext<TextDocument>>(destinationHost, transmitPort), 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL}, 
				new String[]{});		
	}
	
	public static void main(String [] argv)
	{
		if (argv.length < 3)
		{
			System.out.println("Syntax: EntityDetectionNetwork receivePort transmitPort transmitHost.");
			System.exit(-1);
		}
		
		DU.loadSystemProperties();
		
		EntityDetectionNetwork network = new EntityDetectionNetwork();
		network.setReceivePort(Integer.parseInt(argv[0]));
		network.setTransmitPort(Integer.parseInt(argv[1]));
		network.setDestinationHost(argv[2]);
		
		network.create();
		
		while (true)
		{
			Future<?> f = network.start();
			try
			{
				Boolean success = (Boolean)f.get();
				System.out.println("Network run completed, success=" + success);
			}
			catch (Throwable t)
			{
				t.printStackTrace(System.err);
			}
		}
	}

	public String getDestinationHost()
	{
		return destinationHost;
	}

	public void setDestinationHost(String destinationHost)
	{
		this.destinationHost = destinationHost;
	}

	public int getReceivePort()
	{
		return receivePort;
	}

	public void setReceivePort(int receivePort)
	{
		this.receivePort = receivePort;
	}

	public int getTransmitPort()
	{
		return transmitPort;
	}

	public void setTransmitPort(int transmitPort)
	{
		this.transmitPort = transmitPort;
	}	
}
