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
/**
 * 
 */
package disko.saca;

import org.hypergraphdb.util.HGUtils;

public class ServerInfo implements Comparable<ServerInfo>
{
	String host;
	int port;
	int maxRequests;
	boolean active;
	
	int totalRequests = 0;
	long totalProcessingTime = 0;
	int currentRequests = 0;
		
	public ServerInfo()
	{		
	}
	
	public ServerInfo(String hostname, int port)
	{
		this.host = hostname;
		this.port = port;
	}
	
	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}

	public int getMaxRequests()
	{
		return maxRequests;
	}

	public void setMaxRequests(int maxRequests)
	{
		this.maxRequests = maxRequests;
	}

	public int getTotalRequests()
	{
		return totalRequests;
	}

	public void setTotalRequests(int totalRequests)
	{
		this.totalRequests = totalRequests;
	}

	public long getTotalProcessingTime()
	{
		return totalProcessingTime;
	}

	public void setTotalProcessingTime(long totalProcessingTime)
	{
		this.totalProcessingTime = totalProcessingTime;
	}

	public int getCurrentRequests()
	{
		return currentRequests;
	}

	public void setCurrentRequests(int currentRequests)
	{
		this.currentRequests = currentRequests;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public boolean isIddle()
	{
		return !active;
	}

	public void setIddle(boolean iddle)
	{
		this.active = !iddle;
	}
	
	public double getAvgTime()
	{
		return totalRequests == 0 ? 0 : (double)totalProcessingTime / (double)totalRequests;	
	}
	
	public int compareTo(ServerInfo si)
	{
		// TODO - perhaps maxRequests should be incorporated into this formula?
		// because different values of maxRequests may reflect different capacities of the 
		// corresponding servers...
		double w1 = getAvgTime()*currentRequests;
		double w2 = si.getAvgTime()*si.currentRequests;
		return Double.compare(w1, w2);
	}
	
	public int hashCode() { return HGUtils.hashThem(host, port); }
	public boolean equals(Object other)
	{
		if (! (other instanceof ServerInfo)) return false;
		ServerInfo si = (ServerInfo)other;
		return HGUtils.eq(host, si.host) && port == si.port;
	}
		
	public String toString()
	{
		return "DiscoServer[" + host + ":" + port + "]";
	}
}
