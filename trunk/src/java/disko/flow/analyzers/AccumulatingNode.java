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
package disko.flow.analyzers;

import java.util.ArrayList;
import java.util.List;

import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;

/**
 * 
 * <p>
 * An <code>AccumulatingNode</code> stores all data that goes into a Channel.
 * There is no bound of the accumulated data. It's up to a user of this class
 * to manage the amount stored, remove from it etc. In particular, repeated
 * calls to the <code>process</code> method will just add to the data by default. To
 * clear the data at the beginning of the <code>process</code>, set the 
 * <code>autoClear</code> flag to true.
 * </p>
 *
 * @author Borislav Iordanov
 *
 * @param <T>
 */
public class AccumulatingNode<ContextType, T> extends AbstractProcessor<ContextType>
{
	private String channelId;
	private boolean autoClear = false;
	private transient List<T> data = new ArrayList<T>();
	
	public AccumulatingNode(String channelId)
	{
		this.channelId = channelId;
	}
	
	public void process(ContextType ctx, Ports ports) throws InterruptedException
	{
		if (autoClear)
			data.clear();
		InputPort<T> in = ports.getInput(channelId);
		for (T t = in.take(); !in.isEOS(t); t = in.take())
			data.add(t);
	}
	
	public String getChannelId()
	{
		return channelId;
	}
	
	public List<T> getData()
	{
		return data;
	}

	public boolean isAutoClear()
	{
		return autoClear;
	}

	public void setAutoClear(boolean autoClear)
	{
		this.autoClear = autoClear;
	}	
}
