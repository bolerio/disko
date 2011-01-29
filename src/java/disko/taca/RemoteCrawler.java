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
package disko.taca;

import java.util.List;

import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;

import disko.utils.ScriptShellActivity;

public class RemoteCrawler extends Crawler
{
	private String remoteObjectName;
	private HyperGraphPeer thisPeer;
	private HGPeerIdentity remotePeer;
	private Object networkTarget;
	
	public String getRemoteObjectName()
	{
		return remoteObjectName;
	}

	public void setRemoteObjectName(String remoteObjectName)
	{
		this.remoteObjectName = remoteObjectName;
	}

	public HyperGraphPeer getThisPeer()
	{
		return thisPeer;
	}

	public void setThisPeer(HyperGraphPeer thisPeer)
	{
		this.thisPeer = thisPeer;
	}

	public HGPeerIdentity getRemotePeer()
	{
		if (remotePeer == null && networkTarget != null)
			remotePeer = ScriptShellActivity.findPeerByNetworkTargetPrefix(
					thisPeer, 
					networkTarget.toString(),
					5,
					2000);		
		return remotePeer;
	}

	public void setRemotePeer(HGPeerIdentity remotePeer)
	{
		this.remotePeer = remotePeer;
	}
	
	public String getRemotePeerHandle()
	{
		return remotePeer == null ? null : remotePeer.getId().toString();
	}

	public void setRemotePeerHandle(String remotePeerHandle)
	{
		if (remotePeerHandle == null)
			remotePeer = null;
		else
		{
			remotePeer = new HGPeerIdentity();
			remotePeer.setId(thisPeer.getGraph().getHandleFactory().makeHandle(remotePeerHandle));
		}
	}
	
	public Object getNetworkTarget()
	{
		return networkTarget;
	}

	public void setNetworkTarget(Object networkTarget)
	{
		this.networkTarget = networkTarget;
	}

	/*
	private Object remoteInvoke(String method, Object...args)
	{
		Map<String, Object> bindings = DU.stringMap(args);
		String scriptArgs = "";
		for (int i = 0; i < args.length; i++)
		{
			scriptArgs += (String)args[i++];
			if (i < args.length - 1)
				scriptArgs += ",";
		}
		if (networkTarget != null && remotePeer == null)
		{
			remotePeer = ScriptShellActivity.findPeerByNetworkTargetPrefix(
													thisPeer, 
													networkTarget.toString(),
													5,
													2000);
			if (remotePeer == null)
				throw new RuntimeException("Unable to find remote peer with net target name: " + networkTarget.toString());
		}
		return ScriptShellActivity.executeScript(thisPeer, 
				  remotePeer, 
				  "beanshell", 
				  "crawler." + method + "(" + scriptArgs + ")", 
				  bindings);
	}
	*/
	@Override
	public void addRoot(CrawlRoot root)
	{
		ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.addRoot", root);
	}

	@Override
	public CrawlRoot findRoot(String url)
	{
		return (CrawlRoot)ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.findRoot", url);		
	}

	@Override
	public int getNThreads()
	{
		return (Integer)ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.getNThreads");		
	}

	@Override
	public List<CrawlRoot> getRoots()
	{
		return (List<CrawlRoot>)ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.getRoots");		
	}

	@Override
	public int getTaskCount()
	{
		return (Integer)ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.getTaskCount");		
	}

	@Override
	public boolean isRunning()
	{
		return (Boolean)ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.isRunning");
	}

	@Override
	public void removeRoot(CrawlRoot root)
	{
		ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.removeRoot", root);		
	}

    @Override
    public void updateRoot(CrawlRoot root)
    {
		ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.updateRoot", root);    	
    }
    
	@Override
	public void setNThreads(int threads)
	{
		ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.setNThreads", threads);		
	}

	@Override
	public synchronized void start()
	{
		ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.start");			
	}

	@Override
	public synchronized void stop()
	{
		ScriptShellActivity.executeMethodCall(thisPeer, getRemotePeer(), "beanshell", "crawler.stop");
	}
}
