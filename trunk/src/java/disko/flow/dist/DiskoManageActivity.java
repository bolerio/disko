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

import java.util.Map;

import java.util.UUID;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPlainLink;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.algorithms.CopyGraphTraversal;
import org.hypergraphdb.algorithms.DefaultALGenerator;
import org.hypergraphdb.algorithms.HGALGenerator;
import org.hypergraphdb.algorithms.HyperTraversal;
import org.hypergraphdb.app.dataflow.ChannelLink;
import org.hypergraphdb.app.dataflow.Job;
import org.hypergraphdb.app.dataflow.NetworkMasterPeer;
import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.Message;
import org.hypergraphdb.peer.Performative;

import static org.hypergraphdb.peer.Messages.*;
import static org.hypergraphdb.peer.Structs.*;

import org.hypergraphdb.peer.cact.TransferGraph;
import org.hypergraphdb.peer.workflow.ActivityListener;
import org.hypergraphdb.peer.workflow.ActivityResult;
import org.hypergraphdb.peer.workflow.FSMActivity;
import org.hypergraphdb.peer.workflow.FromState;
import org.hypergraphdb.peer.workflow.OnMessage;
import org.hypergraphdb.peer.workflow.WorkflowState;
import org.hypergraphdb.peer.workflow.WorkflowStateConstant;
import org.hypergraphdb.util.HGUtils;
import org.hypergraphdb.util.Mapping;
import org.hypergraphdb.util.Pair;

import disko.DU;

/**
 * <p>
 * Activity fired at startup to bootstrap the participation of a given
 * peer within a global distributed processing. The newly started peer
 * should keep issuing a CallForProposal asking for work to do, independent
 * of this activity. A master peer
 * will issue a proposal to either join an existing DFN or fork a new
 * copy. The peer will then accept on a first-come first-served basis.  
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class DiskoManageActivity extends FSMActivity
{
    /**
     * Unique, global, per HGDB peer ID of this activity.
     */
    public static final UUID ID = UUID.fromString("31e198a9-aa85-4ec6-a413-37d5f214350c");
    
    public static final String TYPENAME = "disko-management";
    
    public static final WorkflowStateConstant WorkingOnProposal = WorkflowState.makeStateConstant("WorkingOnProposal");
    
    // Either or both of variables below will be set depending on whether
    // this activity implements a job master, a job worker or both. The latter
    // is not an intended usage.
    DiskoJobMaster jobMaster = null;
    DiskoJobWorker jobWorker = null;

    private static int createCount = 0;
    
    private Mapping<Pair<HGHandle, Object>, HGHandle> atomFinder = null;
    
    public DiskoManageActivity(HyperGraphPeer thisPeer)
    {
        super(thisPeer, ID);
        if (thisPeer != null && ++createCount > 1)
        	throw new RuntimeException("Oops: creating DiskoManageActivity more than once!");
    }
    
    @FromState({"Started", "WorkingOnProposal"})
    @OnMessage(performative="CallForProposal")
    public WorkflowState onCallForProposal(Message msg)
    {
        if (jobMaster == null)
        {
        	DU.log.info("Ignoring CallForProposal on a non-job-master peer.");
            return null;
        }
        DU.log.info("Job master called for proposal by " + getSender(msg));
        HGPeerIdentity peer = getThisPeer().getIdentity(getSender(msg));
        if (peer == null)
        	return null; // send no reply to an unknown peer
        reply(msg, Performative.Propose, struct("dfn", jobMaster.getNetworkHandle()));
        DU.log.info("Job master proposed network " + jobMaster.getNetworkHandle());
        return null;
    }

    @FromState("WorkingOnProposal")
    @OnMessage(performative="Propose")
    public WorkflowState onProposeWhileWorking(final Message msg)
    {
        reply(msg, Performative.RejectProposal, null);
        return null;
    }
    
    @FromState("Started")
    @OnMessage(performative="Propose")
    public WorkflowState onPropose(final Message msg)
    {        
    	DU.log.info("Got proposal from " + getSender(msg));
        final HGPeerIdentity tentativeMaster = getThisPeer().getIdentity(getSender(msg));        
        final HGHandle network = getPart(msg, "content", "dfn");
        if (getThisPeer().getGraph().get(network) == null)
        {
            //
            // Need to get a copy of this network.
            //
            DefaultALGenerator gen = new DefaultALGenerator(getThisPeer().getGraph(), 
                                                           hg.or(hg.type(ChannelLink.class),
                                                                 hg.type(HGPlainLink.class)),
                                                           null); 
            TransferGraph transferGraphActivity = new TransferGraph(getThisPeer(), 
                                                                    new HyperTraversal(getThisPeer().getGraph(),
                                                                                       new CopyGraphTraversal(network, gen)), 
                                                                    getThisPeer().getIdentity(getSender(msg)));
            ActivityListener L = new ActivityListener()
            {
                public void activityFinished(ActivityResult result)
                {
                    if (result.getActivity().getState().isCompleted())
                    {
                        try
                        {
                            jobWorker.startNetworkCopy(network);
                            DU.log.info("Successfully started network " + network);
                        }
                        catch (Throwable t)
                        {
                            DU.log.error("While starting network prototype : " + network, t);
                            reply(msg,Performative.RejectProposal, null);
                            getState().setStarted();
                        }
                        reply(msg, Performative.AcceptProposal, null);
                        jobWorker.jobMasterPeer = tentativeMaster;
                    }
                    else
                    {
                        if (result.getException() != null)
                            DU.log.error("Remote exception at " + jobWorker.jobMasterPeer, 
                                         result.getException());
                        else
                        	DU.log.error("Failed to fetch network " + network + ", but no exception reported.");
                        reply(msg, Performative.RejectProposal, null);
                        getState().setStarted(); 
                    }
                }
            };
        	DU.log.info("Fetching network " + network + " from job master.");            
            this.getThisPeer().getActivityManager().initiateActivity(transferGraphActivity, 
                                                                     this, 
                                                                     L);
        }
        else
        {
        	DU.log.info("Network already stored locally.");
            try
            {
                jobWorker.startNetworkCopy(network);
                reply(msg, Performative.AcceptProposal, null);
                jobWorker.jobMasterPeer = tentativeMaster;
                DU.log.info("Network " + network + " started successfully.");
            }
            catch (Throwable t)
            {
                DU.log.error("While starting network prototype : " + network, t);
                reply(msg,Performative.RejectProposal, null);
                return null;
            }
        }
        return WorkingOnProposal;
    }
    
    @FromState("Started")
    @OnMessage(performative="AcceptProposal")
    public WorkflowState onAcceptProposal(final Message msg)
    {                
        HGPeerIdentity peerId = getThisPeer().getIdentity(getSender(msg));
        jobMaster.getNetworkMasters().put(peerId, new NetworkMasterPeer());
        jobMaster.getAvailableNetworkMasters().add(peerId);
        return null;
    }

    @FromState("Started")
    @OnMessage(performative="RejectProposal")
    public WorkflowState onRejectProposal(final Message msg)
    {
        return null;
    }
    
    @FromState("WorkingOnProposal")
    @OnMessage(performative="Request")
    public WorkflowState onRequest(final Message msg)
    {
        Map<String, Object> content = getPart(msg, CONTENT);
        String head = getPart(content, "head");        
        if ("run-job".equals(head))
        {   
            Job job = getPart(msg, "content", "job");
            DU.log.info("Got job request for job " + job);
            if (job != null)
                reply(msg, Performative.Agree, content);
            else
                reply(msg, 
                      Performative.Refuse, 
                      combine(content, struct("reason", "missing job description")));
            try
            {
                jobWorker.getDataFlowPeer().submitJob(job);
                DU.log.info("Job " + job + " submitted to locally running network.");
            }
            catch (InterruptedException e)
            {
            	DU.log.warn("Interrupted while submitted job " + job);
                reply(msg, 
                	  Performative.Refuse, 
                	  combine(content, struct("reason", "interrupted unexpectedly")));
            }
            catch (Throwable t)
            {
            	DU.log.error("While submitting job " + job, t);
                reply(msg, 
                  	  Performative.Refuse, 
                  	  combine(content, struct("reason", HGUtils.printStackTrace(t))));            	
            }
        }
        else
            reply(msg, Performative.Refuse, "unknown request kind");
        return null;
    }

    @FromState("Started")
    @OnMessage(performative="Agree")
    public WorkflowState onJobAgree(final Message msg)
    {
    	DU.log.info("Peer " + getThisPeer().getIdentity(getSender(msg)) + " agreed to job " +
    			getPart(msg, "content", "job"));
    	return null;
    }
    
    @FromState("Started")
    @OnMessage(performative="Refuse")
    public WorkflowState onWorkRefuse(final Message msg)
    {
    	Job job = getPart(msg, "content", "job");
    	Object reason = getPart(msg, "content", "reason");
    	jobMaster.notifyJobFailed(getThisPeer().getIdentity(getSender(msg)), job, reason);    	
    	return null;
    }
    
    @FromState("Started")
    @OnMessage(performative="Inform")
    public WorkflowState onInform(final Message msg)
    {
        String head = getPart(msg, "content", "head");
        final HGPeerIdentity worker = getThisPeer().getIdentity(getSender(msg));
        final Job job = getPart(msg, "content", "job");        
        if ("job-completed".equals(head))
        {
            final HGHandle resultHandle = getPart(msg, "content", "result");
            //
            // Get the results locally.
            //
            HGALGenerator gen = new DefaultALGenerator(getThisPeer().getGraph(), 
                                                       hg.type(disko.ScopeLink.class), 
                                                       null, // sibling predicate
                                                       false, // return preceeding
                                                       true, // return succeeding
                                                       false); // reverse order
            TransferGraph transferGraphActivity = new TransferGraph(getThisPeer(), 
                                                                    new HyperTraversal(getThisPeer().getGraph(),
                                                                                       new CopyGraphTraversal(resultHandle, gen)), 
                                                                    getThisPeer().getIdentity(getSender(msg)),
                                                                    atomFinder);
            DU.log.info("Job " + job + " completed at " + worker + ", starting result transfer.");
            ActivityListener L = new ActivityListener()
            {
                public void activityFinished(ActivityResult result)
                {
                    if (result.getActivity().getState().isCompleted())
                        jobMaster.notifyJobCompleted(worker, job, resultHandle);
                    else
                        jobMaster.notifyJobFailed(worker, job, result.getException());
                }
            };            
            getThisPeer().getActivityManager().initiateActivity(transferGraphActivity, this, L);
        }
        else if ("job-failed".equals(head))
        {
        	jobMaster.notifyJobFailed(worker, job, getPart(msg, "content", "result"));
        }
        return null;
    }

    protected void onPeerFailure(Message msg)
    {
    	if (jobMaster != null) // is this a job master peer
    	{
    		// Remove the peer for list of available
    		jobMaster.notifyPeerFailed(getThisPeer().getIdentity(getSender(msg)));
    	}
    	else
    		super.onPeerFailure(msg);
    }
    
    protected void onPeerNotUnderstand(Message msg)
    {
    	DU.log.error("We were NotUnderstood by " + getSender(msg) + " on " + msg);
    	if (jobMaster != null) // is this a job master peer
    	{
    		// Remove the peer for list of available
    		jobMaster.notifyPeerFailed(getThisPeer().getIdentity(getSender(msg)));
    		// Cause it to fail
    		reply(msg, Performative.Failure, null);
    	}
    	// we should not be failing this peer because somebody 
    	// did not understand us...
//    	else
//    		super.onPeerNotUnderstand(msg);
    }
    
    public void setAtomFinder(Mapping<Pair<HGHandle, Object>, HGHandle> atomFinder)
    {
        this.atomFinder = atomFinder;
    }
    
    public Mapping<Pair<HGHandle, Object>, HGHandle> getAtomFinder()
    {
        return this.atomFinder;
    }
    
    public String getType()
    {
        return TYPENAME;
    }
}
