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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.PredefinedNetwork;

import disko.AnalysisContext;
import disko.ParagraphAnn;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.SentenceInterpretation;
import disko.flow.analyzers.ConsoleOutputAnalyzer;
import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.FullRelexAnalyzer;
import disko.flow.analyzers.ParagraphAnalyzer;
import disko.flow.analyzers.ParseSelectAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.ToRelOccAnalyzer;
import disko.flow.analyzers.hgdb.HGDBSaver;
import disko.flow.analyzers.socket.SocketReceiver;
import disko.flow.analyzers.socket.SocketTransmitter;

import relex.concurrent.RelexTaskResult;
import relex.entity.EntityMaintainer;

/**
 * 
 * <p>
 * <em>Information Extraction</em> network. Extracts and store relex relations from a document.
 * </p>
 *
 * <p>
 * This differs from the IENetwork class in that the whole of Relex processing is done
 * in one dataflow node - the link grammar parsing and Relex rules are applied to each
 * sentence as a single unit of work. By contrast, the IENetwork has a separate node
 * for LG parsing and each parse is processed on its own dataflow "tick", which provides
 * much more opportunities for parallelism.  
 * </p>
 * @author Borislav Iordanov
 *
 */
public class IENetworkCoarse extends PredefinedNetwork<AnalysisContext<TextDocument>>
{
	private int entityTransmitPort = -1, entityReceivePort = -1;
	private String entityHost = null;
	private List<String> linkGrammarServers = null;
	private boolean debug = false;
	
	FullRelexAnalyzer lg = null;
	
	public void create()
	{
		addChannel(new Channel<ParagraphAnn>(
				ParagraphAnalyzer.PARAGRAPH_CHANNEL, 
				new ParagraphAnn(0, 0), 30));

/*		addChannel(new Channel<MarkupAnn>(
				ParagraphAnalyzer.ATTRIBUTE_CHANNEL, 
				new MarkupAnn(0, 0, ""), 10)); */
		
		addChannel(new Channel<SentenceAnn>(
				SentenceAnalyzer.SENTENCE_CHANNEL, 
				new SentenceAnn(0, 0), 30));
		
		addChannel(new Channel<EntityMaintainer>(
				EntityAnalyzer.ENTITY_CHANNEL, 
				EntityAnalyzer.EMPTY_EM, 30));
		
		addChannel(new Channel<RelexTaskResult>(
				FullRelexAnalyzer.PARSE_CHANNEL, 
				new RelexTaskResult(-1,null,null,null), 30));

		addChannel(new Channel<SentenceInterpretation>(
				ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL, 
				new SentenceInterpretation(null, null), 30));

		addChannel(new Channel<Set<SentenceInterpretation>>(
				ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS, 
				new HashSet<SentenceInterpretation>(), 30));
		
		addNode(new ParagraphAnalyzer(), 
				new String[] {},
				new String[] { 
					ParagraphAnalyzer.PARAGRAPH_CHANNEL 
				});

		addNode(new SentenceAnalyzer(), 
				new String[] { ParagraphAnalyzer.PARAGRAPH_CHANNEL },
				new String[] { SentenceAnalyzer.SENTENCE_CHANNEL });

		if (entityHost != null)
		{
			if (entityReceivePort < 0 || entityTransmitPort < 0)
				throw new IllegalArgumentException("If entityHost is configured, both the receive and transmit port must be.");

			addNode(new SocketTransmitter<AnalysisContext<TextDocument>>(entityHost, entityTransmitPort), 
					new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
					new String[]{});			
			
			addNode(new SocketReceiver<AnalysisContext<TextDocument>>(entityReceivePort), 
					new String[]{}, 
					new String[]{EntityAnalyzer.ENTITY_CHANNEL});			
		}
		else
			addNode(
					new EntityAnalyzer(), 
					new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
					new String[]{EntityAnalyzer.ENTITY_CHANNEL});
		
		lg = new FullRelexAnalyzer();
		if (linkGrammarServers != null && !linkGrammarServers.isEmpty())
		{
			for (int i = 0; i < linkGrammarServers.size(); i++)
			{
				try
				{
					URL url = new URL(linkGrammarServers.get(i));
					lg.addHost(url.getHost(), url.getPort());
				}
				catch (Exception ex)
				{
					ex.printStackTrace(System.err);
				}
			}
		}			
		lg.setMaxParses(5);
		addNode(lg, 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL}, 
				new String[]{FullRelexAnalyzer.PARSE_CHANNEL});

		addNode(new ToRelOccAnalyzer(), 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL, FullRelexAnalyzer.PARSE_CHANNEL}, 
				new String[]{ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS});

		addNode(new ParseSelectAnalyzer(), 
				new String[]{ToRelOccAnalyzer.SENTENCE_INTERPRETATIONS, EntityAnalyzer.ENTITY_CHANNEL}, 
				new String[]{ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL});

		addNode(new HGDBSaver(),
				new String[] { 
					SentenceAnalyzer.SENTENCE_CHANNEL, 
					ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL 
				}, 
				new String[] {});

		if (debug) 
			addNode(new ConsoleOutputAnalyzer(), 
					new String[]{
						ParagraphAnalyzer.PARAGRAPH_CHANNEL,
						SentenceAnalyzer.SENTENCE_CHANNEL, 
						EntityAnalyzer.ENTITY_CHANNEL,
						ParseSelectAnalyzer.SELECTED_PARSE_CHANNEL
						}, 
						new String[]{});
					
	}	
	
	public IENetworkCoarse()
	{
	}

	public boolean isDebug()
	{
		return debug;
	}

	public void setDebug(boolean debug)
	{
		this.debug = debug;
	}

	public String getEntityHost()
	{
		return entityHost;
	}

	public void setEntityHost(String entityHost)
	{
		this.entityHost = entityHost;
	}

	public int getEntityReceivePort()
	{
		return entityReceivePort;
	}

	public void setEntityReceivePort(int entityReceivePort)
	{
		this.entityReceivePort = entityReceivePort;
	}

	public int getEntityTransmitPort()
	{
		return entityTransmitPort;
	}

	
	public void setEntityTransmitPort(int entityTransmitPort)
	{
		this.entityTransmitPort = entityTransmitPort;
	}

	public List<String> getLinkGrammarServers()
	{
		if (linkGrammarServers == null)
			linkGrammarServers = new ArrayList<String>();
		return linkGrammarServers;
	}

	public void setLinkGrammarServers(List<String> linkGrammarServers)
	{
		this.linkGrammarServers = linkGrammarServers;
	}
	
	public void destroy()
	{
		super.kill();
		lg.destroy();
	}
}
