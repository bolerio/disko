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
import java.util.List;

import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.LoadBalancer;
import org.hypergraphdb.app.dataflow.PredefinedNetwork;

import disko.AnalysisContext;
import disko.ParagraphAnn;
import disko.SentenceAnn;
import disko.TextDocument;

import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.FullRelexAnalyzer;
import disko.flow.analyzers.ParagraphAnalyzer;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.hgdb.RelationCounterAnalyzer;
import disko.flow.analyzers.socket.SocketReceiver;
import disko.flow.analyzers.socket.SocketTransmitter;

import relex.concurrent.RelexTaskResult;
import relex.entity.EntityMaintainer;

/**
 * 
 * <p>
 * A network used to pre-process a whole corpus by accumulating statistics
 * about relex relation counts to be used in parse ranking. 
 * </p>
 *
 * <p>
 * Entity detection can be in a separate process (e.g. by using the
 * <code>EntityDetectionNetwork</code>), in which case this network must be
 * configured with:
 * 
 * <ul>
 * <li>entityHost - the computer on which the entity detection network resides.</li>
 * <li>entityTransmitPort - the port to which sentences are sent to the entity detection network.</li>
 * <li>entityReceivePort - the port on which <code>EntityMaintainer</code> objects arrive from 
 * the entity detection network.</li>
 * </ul>
 * 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class RelationCounterNetwork extends	PredefinedNetwork<AnalysisContext<TextDocument>>
{
	private int entityTransmitPort = -1, entityReceivePort = -1;
	private String entityHost = null;
	private List<String> linkGrammarServers = null;
	private int lgLoadBalance = 0;
	
	public RelationCounterNetwork()
	{		
	}
	
	public RelationCounterNetwork(AnalysisContext<TextDocument> ctx)
	{
		super(ctx);
	}

	public int getEntityTransmitPort()
	{
		return entityTransmitPort;
	}

	public void setEntityTransmitPort(int entityTransmitPort)
	{
		this.entityTransmitPort = entityTransmitPort;
	}

	public int getEntityReceivePort()
	{
		return entityReceivePort;
	}

	public void setEntityReceivePort(int entityReceivePort)
	{
		this.entityReceivePort = entityReceivePort;
	}

	public String getEntityHost()
	{
		return entityHost;
	}

	public void setEntityHost(String entityHost)
	{
		this.entityHost = entityHost;
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

	public void create()
	{				
		addChannel(new Channel<ParagraphAnn>(
				ParagraphAnalyzer.PARAGRAPH_CHANNEL, 
				new ParagraphAnn(0, 0)));
		
		addChannel(new Channel<SentenceAnn>(
				SentenceAnalyzer.SENTENCE_CHANNEL, 
				new SentenceAnn(0, 0)));
		
		addChannel(new Channel<EntityMaintainer>(
				EntityAnalyzer.ENTITY_CHANNEL, 
				EntityAnalyzer.EMPTY_EM ));
		
		addChannel(new Channel<RelexTaskResult>(
				FullRelexAnalyzer.PARSE_CHANNEL, 
				new RelexTaskResult(-1,null,null,null)));
		
		addNode(new ParagraphAnalyzer(), 
				new String[] {},
				new String[] { ParagraphAnalyzer.PARAGRAPH_CHANNEL });
		
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
		
		FullRelexAnalyzer lg = null;
		if (linkGrammarServers != null && !linkGrammarServers.isEmpty())
		{
			Object [] A = new Object[linkGrammarServers.size()*2];
			for (int i = 0; i < linkGrammarServers.size(); i++)
			{
				try
				{
					URL url = new URL(linkGrammarServers.get(i));
					A[2*i] = url.getHost();
					A[2*i+1] = new Integer(url.getPort());
				}
				catch (Exception ex)
				{
					ex.printStackTrace(System.err);
				}
			}
			lg = new FullRelexAnalyzer(A);
		}
		else
			lg = new FullRelexAnalyzer();
		addNode(lg, 
				new String[]{EntityAnalyzer.ENTITY_CHANNEL}, 
				new String[]{FullRelexAnalyzer.PARSE_CHANNEL});

		addNode(new RelationCounterAnalyzer<SentenceAnn>(),
				new String[] {EntityAnalyzer.ENTITY_CHANNEL,
							  FullRelexAnalyzer.PARSE_CHANNEL}, 
				new String[] {});
		
		if (lgLoadBalance > 0)
			try
			{
				LoadBalancer.make(this, lg, lgLoadBalance);
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
	}

	public int getLgLoadBalance()
	{
		return lgLoadBalance;
	}

	public void setLgLoadBalance(int lgLoadBalance)
	{
		this.lgLoadBalance = lgLoadBalance;
	}
}
