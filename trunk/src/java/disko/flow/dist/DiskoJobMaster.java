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
package disko.flow.dist;

import java.util.Collection;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hypergraphdb.peer.Structs.*;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.DistributedException;
import org.hypergraphdb.app.dataflow.Job;
import org.hypergraphdb.app.dataflow.JobFuture;
import org.hypergraphdb.app.dataflow.NetworkMasterPeer;
import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.Message;
import org.hypergraphdb.peer.Messages;
import org.hypergraphdb.peer.PeerPresenceListener;
import org.hypergraphdb.peer.Performative;
import org.hypergraphdb.peer.Structs;
import org.hypergraphdb.peer.workflow.ActivityResult;
import org.hypergraphdb.util.HGUtils;
import org.hypergraphdb.util.Mapping;
import org.hypergraphdb.util.Pair;

import disko.DU;
import disko.data.NamedEntity;
import disko.data.UnknownWord;
import disko.data.relex.SynRel;

/** 
 * <p>
 * Represents a DiskoWorker process connected to a single P2P network. Generally
 * there will be only one such worker per JVM, but care has been taken to allow
 * for more than one if need be.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class DiskoJobMaster
{
    private Map<String, Object> configuration;
    private HyperGraphPeer hgpeer;
    private HGHandle processingNetwork;
    private DiskoManageActivity manageActivity;
    private Future<ActivityResult> manageFuture;
    
    private Map<HGPeerIdentity, NetworkMasterPeer> networkMasters = 
        Collections.synchronizedMap(new HashMap<HGPeerIdentity, NetworkMasterPeer>());
    private BlockingQueue<HGPeerIdentity> availableNetworkMasters = 
        new LinkedBlockingQueue<HGPeerIdentity>();
    private BlockingQueue<JobFuture<Object>> pendingJobs = null; 
    private JobSubmitter jobSubmitter = null;	
    
    private void validateConfiguration()
    {
        String graphLocation = getPart(configuration, "localDB");
        if (DU.isEmpty(graphLocation))
            throw new RuntimeException("Missing HyperGraph configuration 'localDB' parameter.");
        
        HyperGraph graph = HGEnvironment.get(graphLocation);
        
        String netHandle = getPart(configuration, "processingNetwork");        
        if (netHandle == null)
            throw new RuntimeException("Missing 'processingNetwork' configuration for job master worker.");
        processingNetwork = graph.getHandleFactory().makeHandle(netHandle);        
        if (graph.get(processingNetwork) == null)
            throw new RuntimeException("Processing network '" + 
                                       processingNetwork + "' not found in graph at " + graphLocation);
    }
    
    public DiskoJobMaster(Map<String, Object> configuration)
    {
        this.configuration = configuration;
        validateConfiguration();
    }
    
    public void start()
    {
        try
        {
        	pendingJobs = new LinkedBlockingQueue<JobFuture<Object>>(getOptPart(configuration, 100000, "maxPending"));
        	jobSubmitter = new JobSubmitter();
            hgpeer = new HyperGraphPeer(configuration);
            hgpeer.addPeerPresenceListener(new DiskoPresenceListener());
            hgpeer.start(null, null).get();
            hgpeer.getExecutorService().execute(jobSubmitter);
            manageActivity = new DiskoManageActivity(hgpeer);
            manageActivity.jobMaster = this;
            manageActivity.setAtomFinder(new AtomFinder(hgpeer.getGraph()));
            manageFuture = hgpeer.getActivityManager().initiateActivity(manageActivity);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    public void stop()
    {
    	if (jobSubmitter != null)
    	{
    		if (jobSubmitter.running)
    		{
    			jobSubmitter.running = false;
    			jobSubmitter.interrupt();
    		}
    	}
        if (manageActivity != null && !manageActivity.getState().isFinished())
            manageActivity.getState().setCanceled();
        if (hgpeer != null)
            hgpeer.stop();
    }
    
    public boolean isActive()
    {
        return hgpeer != null && hgpeer.getPeerInterface().isConnected() &&
               manageActivity != null && !manageActivity.getState().isFinished();                
    }
    
    public HGHandle getNetworkHandle()
    {
        return processingNetwork;
    }
    
    public DiskoManageActivity getManageActivity()
    {
    	return manageActivity;
    }
    
    public Future<ActivityResult> getManageActivityFuture()
    {
    	return manageFuture;
    }
    
    public Map<HGPeerIdentity, NetworkMasterPeer> getNetworkMasters()
    {
        return networkMasters;
    }
    
    public Queue<HGPeerIdentity> getAvailableNetworkMasters()
    {
        return availableNetworkMasters;
    }
    
    public JobFuture<Object> submitJob(Job job) throws InterruptedException
    {
        JobFuture<Object> future = new JobFuture<Object>(job);
        future.setSubmittedAt(System.currentTimeMillis());
        DU.log.info("Adding job " + job + " to " + pendingJobs.size() + " pending jobs.");
        pendingJobs.put(future);
        return future;
    }
    
    public Collection<JobFuture<Object>> getPendingJobs()
    {
    	return pendingJobs;
    }
    
    public void notifyJobCompleted(HGPeerIdentity from, Job job, HGHandle resultHandle)
    {
        DU.log.info("Completed job " + job + " with result " + resultHandle);
        NetworkMasterPeer netMaster = networkMasters.get(from);
        if (netMaster == null)
        {
        	DU.log.warn("Master for job " + job + " has left the network, but we got complete notification!");
        	return;
        }
        else if (netMaster.getCurrentJob() == null)
        	DU.log.error("Incorrect state: received job complete notification for the netMaster has no job assigned.");
        else
        {
        	netMaster.getCurrentJob().setCompletedAt(System.currentTimeMillis());
        	netMaster.getCurrentJob().complete(resultHandle);
        }
        netMaster.setCurrentJob(null);
        this.availableNetworkMasters.add(from);
    }
    
    public void notifyJobFailed(HGPeerIdentity from, Job job, Object reason)
    {
        DU.log.info("Failed job " + job + " because " + reason);
        NetworkMasterPeer netMaster = networkMasters.get(from);        
        if (netMaster == null)
        {
        	DU.log.warn("Master for job " + job + " has left the network, but we got failure notification!");
        	return;
        }
        else if (netMaster.getCurrentJob() == null)
        	DU.log.error("Incorrect state: received job failure notification for the netMaster has no job assigned.");
        else        
        {
        	netMaster.getCurrentJob().setCompletedAt(System.currentTimeMillis());
        	netMaster.getCurrentJob().complete(new DistributedException(from, reason != null ? reason.toString():null));
        }
        netMaster.setCurrentJob(null);
        this.availableNetworkMasters.add(from);        
    }

    public void notifyPeerFailed(HGPeerIdentity peer)
    {
        DU.log.info("Peer " + peer + " failed on DiskoManagementActivity. Removing until another CallForProposal is received.");
        NetworkMasterPeer netMaster = networkMasters.get(peer);
        if (netMaster == null)
        	return;
        networkMasters.remove(peer);
        availableNetworkMasters.remove(peer);
        JobFuture<Object> currentJob = netMaster.getCurrentJob();
        try
        {
        	currentJob.setScheduledAt(0);
        	pendingJobs.put(currentJob);
        }
        catch (InterruptedException ex)
        {
        	DU.log.warn("DiskoJobMaster.notifyPeerFailed was interrupted, couldn't add job " + currentJob
        			+" to the list of pending jobs.");
        }
    }
    
    private class DiskoPresenceListener implements PeerPresenceListener
    {
        public void peerJoined(HGPeerIdentity peer)
        {
        	// Nothing to do here, since we're actually waiting for a 
        	// CallForProposal
        }

        public void peerLeft(HGPeerIdentity peer)
        {
        	// TODO: should we tolerate a temporary drop out and get back
        	// online within a certain timeout interval?
            NetworkMasterPeer netMaster = networkMasters.remove(peer);
            availableNetworkMasters.remove(peer);            
            if (netMaster != null && netMaster.getCurrentJob() != null)
                try
	            {
	            	pendingJobs.put(netMaster.getCurrentJob());
	            }
	            catch (InterruptedException ex)
	            {
	            	DU.log.warn("DiskoJobMaster.peerLeft  was interrupted, couldn't add job " + 
	            			netMaster.getCurrentJob() + " to the list of pending jobs.");
	            }            	
        }
    }
    
    private class JobSubmitter extends Thread
    {
    	volatile boolean running = true;
    	
    	public void run()
    	{
    	    String threadName = Thread.currentThread().getName();
    	    Thread.currentThread().setName("DISKO JOB SUBMITTER");
    		try {
    		while (running)
    		{
    			JobFuture<Object> future = pendingJobs.take();
    			DU.log.info("Scheduling job " + future.getJob() + 
    					" while " + pendingJobs.size() + " more are pending.");
	            Message msg = Messages.createMessage(Performative.Request, 
	                    manageActivity.getType(), 
	                    manageActivity.getId());
	            Structs.combine(msg, struct("content",
	            							struct("head", "run-job",
	            									"job", future.getJob()))); 
	
	            while (running)
	            {
	                HGPeerIdentity peerId = availableNetworkMasters.take();
	                Object networkTarget = hgpeer.getNetworkTarget(peerId);
	                if (networkTarget == null) // peer droped?
	                {
	                    networkMasters.remove(peerId);
	                }
	                else
	                {
	                    try
	                    {
	                        NetworkMasterPeer netMaster = networkMasters.get(peerId);
	                        netMaster.setCurrentJob(future);
	                        future.setScheduledAt(System.currentTimeMillis());
	                        hgpeer.getPeerInterface().send(hgpeer.getNetworkTarget(peerId), msg);
	                        break;
	                    }
	                    catch (Throwable t)
	                    {
	                        DU.log.error("Failed to submit " + future.getJob() + " to " + peerId, t);
	                        networkMasters.remove(peerId);
	                    }
	                }
	            }
    		}
    		}
    		catch (InterruptedException ex)
    		{
    			running = false;
    			DU.log.info("JOB SUBMITTER thread interrupted, exiting.");
    		}
    		catch (Throwable t)
    		{
    		    DU.log.error("JOB SUBMITTER thread died with exception.", t);
    		}
    		finally
    		{
                Thread.currentThread().setName(threadName);    		    
    		}
    	}
    }
    
    public static class AtomFinder implements Mapping<Pair<HGHandle, Object>, HGHandle>
    {
        HyperGraph graph;
        public AtomFinder(HyperGraph graph) { this.graph = graph; }
        
        public HGHandle eval(Pair<HGHandle, Object> p)
        {
        	if (graph.get(p.getFirst()) != null)
        		return p.getFirst();
            Object x = p.getFirst();
            HGHandle result = null;
            if (x instanceof SynRel)
                result = hg.findOne(graph, hg.and(hg.type(SynRel.class), 
                                             hg.orderedLink(HGUtils.toHandleArray((SynRel)x))));                
            else if (x instanceof NamedEntity)
                result = hg.findOne(graph, hg.eq(x));
            else if (x instanceof UnknownWord)
                result = hg.findOne(graph, 
                                    hg.and(hg.type(UnknownWord.class), 
                                           hg.eq("lemma", ((UnknownWord)x).getLemma())));                
            // TODO: what about OWL entities, we need to make sure those are
            // synchronized (same handle etc.) across the board.
            return result == null ? null : graph.getPersistentHandle(result);        }        
    }
}
