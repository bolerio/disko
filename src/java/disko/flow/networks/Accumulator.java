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
import java.util.List;
import java.util.Map;

import org.hypergraphdb.app.dataflow.DataFlowNetwork;

import disko.flow.analyzers.AccumulatingNode;

/**
 * <p>
 * This should ideally follow a decorator pattern with this class inheriting
 * from DataFlowNetwork, but it is to tedious to do it this way. Besides, the
 * data capturing is more decoupled from the network with this setup.
 * </p>
 * 
 * @author boris
 *
 * @param <ContextType>
 */
public class Accumulator<ContextType>
{
	private DataFlowNetwork<ContextType> network;
	private Map<String, AccumulatingNode<ContextType, Object>> accNodes = 
		new HashMap<String, AccumulatingNode<ContextType, Object>>();
	
	public Accumulator(DataFlowNetwork<ContextType> network, String...channels)
	{
		this.network = network;
		for (String channel : channels)
			accumulate(channel);
	}	
	
	public DataFlowNetwork<ContextType> getNetwork()
	{
		return network;
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getData(String channelId)
	{
		AccumulatingNode<ContextType, Object> accumulator = accNodes.get(channelId);
		if (accumulator == null)
			throw new RuntimeException("Data on channel " + channelId + " is not being captured.");
		else
			return (List<T>)accumulator.getData();
	}
	
	public void accumulate(String channelId)
	{
		AccumulatingNode<ContextType, Object> accumulator = accNodes.remove(channelId);
		if (accumulator != null)
			network.removeNode(accumulator);
		accumulator = new AccumulatingNode<ContextType, Object>(channelId);
		network.addNode(accumulator, new String[] {channelId}, new String[]{});		
		accNodes.put(channelId, accumulator);		
	}
	
	public void ignore(String channelId)
	{
		accNodes.remove(channelId);
	}
}
