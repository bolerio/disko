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
package disko.flow.analyzers.socket;

import java.io.Serializable;

public class Message implements Serializable
{
	private static final long serialVersionUID = -5096296939017990494L;

	private String channelId;
	private Serializable data;

	public Message(String channelId, Serializable data)
	{
		super();
		this.channelId = channelId;
		this.data = data;
	}

	public String getChannelId()
	{
		return channelId;
	}

	public void setChannelId(String channelId)
	{
		this.channelId = channelId;
	}

	public Object getData()
	{
		return data;
	}

	public void setData(Serializable data)
	{
		this.data = data;
	}

	public String toString()
	{
		return channelId + ": " + data;
	}
}
