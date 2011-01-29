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
package disko.saca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class LoadBalancedSacaClient
{
	private SortedSet<ServerInfo> servers = new TreeSet<ServerInfo>();
	
	private ServerInfo getLeastLoadedServer()
	{
		ServerInfo result = null;
		synchronized (servers)
		{
			while (result == null)
				for (Iterator<ServerInfo> i = servers.iterator(); i.hasNext(); )
				{
					ServerInfo si = i.next();
					if (si.currentRequests < si.maxRequests && si.isActive())
					{
						result = si;
						i.remove();
						break;
					}
				}
			synchronized (result)
			{
				result.currentRequests++;
				servers.add(result);
			}
		}
		return result;
	}
	
	public void addAllServers(Collection<ServerInfo> srvs)
	{
		synchronized (servers) { servers.addAll(srvs); }
	}
	
	public void addServer(ServerInfo si)
	{
		synchronized (servers) { servers.add(si); }
	}
	
	public void addServer(String hostname, int port, int maxRequests)
	{
		ServerInfo info = new ServerInfo();
		info.host = hostname;
		info.port = port;
		info.maxRequests = maxRequests;
		synchronized (servers)
		{
			servers.add(info);
		}
	}
	
	public void removeServer(String hostname, int port)
	{
		ServerInfo si = new ServerInfo();
		si.host = hostname;
		si.port = port;
		synchronized (servers)
		{
			servers.remove(si);
		}
	}

	public ServerInfo getServer(String hostname, int port)
	{
		ServerInfo si = new ServerInfo(hostname, port);
		synchronized (servers)
		{
			for (ServerInfo x : servers)
				if (si.equals(x)) { si = x; break; }
		}
		return si;
	}
	
	public List<ServerInfo> getAllServers()
	{
		ArrayList<ServerInfo> L = new ArrayList<ServerInfo>();
		synchronized (servers) { L.addAll(servers); }		
		return L;
	}
	
	public Object search(String question) throws InterruptedException
	{
		ServerInfo server = getLeastLoadedServer();
		SacaClient client = new SacaClient(server.host, server.port);
		Object answer = null;
		long startTime = System.currentTimeMillis();
		try
		{
			answer = client.search(question);
		}
		finally
		{
			synchronized (server) { server.currentRequests--; }
		}
		long endTime = System.currentTimeMillis();
		synchronized (server)
		{
			server.totalRequests++;
			server.totalProcessingTime += (endTime - startTime);
		}
		return answer;
	}
	
	@SuppressWarnings("unchecked")
	public String ping(ServerInfo server) throws InterruptedException
	{
		SacaClient client = new SacaClient(server.host, server.port);
		Map<String, Object> result = (Map<String, Object>)client.ping();
		return result == null ? null : (String)result.get(SacaService.ACT);
	}
}
