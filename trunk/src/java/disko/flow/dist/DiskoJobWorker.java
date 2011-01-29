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

import static org.hypergraphdb.peer.Structs.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HGValueLink;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.DataFlowPeer;
import org.hypergraphdb.app.dataflow.DistUtils;
import org.hypergraphdb.app.dataflow.DistributedException;
import org.hypergraphdb.app.dataflow.Job;
import org.hypergraphdb.app.dataflow.JobDataFlow;
import org.hypergraphdb.app.dataflow.JobListener;
import org.hypergraphdb.app.management.HGManagement;
import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.Message;
import org.hypergraphdb.peer.Messages;
import org.hypergraphdb.peer.Performative;
import org.hypergraphdb.peer.Structs;
import org.hypergraphdb.peer.PeerPresenceListener;

import disko.AnalysisContext;
import disko.DISKOApplication;
import disko.DU;
import disko.TextDocument;

public class DiskoJobWorker
{
    private Map<String, Object> configuration;
    private HyperGraphPeer hgpeer;    
    private volatile DataFlowPeer<AnalysisContext<TextDocument>> dfpeer = null; // initialized after we accept a proposal
    private WorkerJobListener theJobListener = new WorkerJobListener();
    private DiskoManageActivity manageActivity;
    volatile HGPeerIdentity jobMasterPeer;
         
    public DiskoJobWorker(Map<String, Object> configuration)
    {
        this.configuration = configuration;
    }
    
    public void start()
    {        
        try
        {
            hgpeer = new HyperGraphPeer(configuration);
            hgpeer.start(null, null).get();
            hgpeer.addPeerPresenceListener(new MasterPresenceMonitor());
            
            //System.setProperty("relex.morphy.Morphy", "relex.morphy.MapMorphy");
            //System.setProperty("relex.algs.Morphy", "relex.algs.Morphy");            
//            System.setProperty("gate.home", "/home/borislav/gate");
//            System.setProperty("relex.morphy.Morphy", "org.disco.relex.MorphyHGDB");
//            System.setProperty("morphy.hgdb.location", hgpeer.getGraph().getLocation());
//            System.setProperty("wordnet.configfile", "/home/borislav/disco/trunk/data/wordnet/file_properties.xml");
//            System.setProperty("EnglishModelFilename", "/home/borislav/disco/trunk/data/sentence-detector/EnglishSD.bin.gz");

            HGManagement.ensureInstalled(hgpeer.getGraph(), new DISKOApplication());
            
            manageActivity = new DiskoManageActivity(hgpeer);                        
            manageActivity.jobWorker = this;
            hgpeer.getActivityManager().initiateActivity(manageActivity);            
            hgpeer.getExecutorService().execute(new WaitForWork());
            try
            {
            	Thread.sleep(Long.MAX_VALUE);
            }
            catch (InterruptedException ex)
            {	
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    public DataFlowPeer<?> getDataFlowPeer()
    {
        return dfpeer;
    }
    
    public void startNetworkCopy(HGHandle prototypeHandle)
    {
        HGHandle networkHandle = null;
        
        //
        // First see if we already have a local copy of this network.
        //
        HGLink copyLink = hg.getOne(hgpeer.getGraph(), 
                                          hg.and(hg.eq("copy-of"), hg.orderedLink(hg.anyHandle(), prototypeHandle)));
        if (copyLink != null)
            networkHandle = copyLink.getTargetAt(0);
        else if (copyLink == null)
        {
            JobDataFlow<?> networkPrototype = 
                (JobDataFlow<?>)DistUtils.loadNetwork(hgpeer.getGraph(), prototypeHandle);
            JobDataFlow<?> jdf = JobDataFlow.clone(networkPrototype);
            networkHandle = DistUtils.saveDataFlowNetwork(hgpeer.getGraph(), jdf, hgpeer.getIdentity().getId());
            hgpeer.getGraph().add(new HGValueLink("copy-of", networkHandle, prototypeHandle));
        }
        
        dfpeer = new DataFlowPeer<AnalysisContext<TextDocument>>(hgpeer, networkHandle);
        dfpeer.setMaster(true);        
        // make sure it's unique in case network is restarted
        dfpeer.getNetwork().removeJobListener(theJobListener);
        dfpeer.getNetwork().addJobListener(theJobListener);
        dfpeer.getNetwork().setContext(new AnalysisContext<TextDocument>(hgpeer.getGraph(), null));
        dfpeer.start();
    }
    
    private class MasterPresenceMonitor implements PeerPresenceListener
    {
        public void peerJoined(HGPeerIdentity peer)
        {
            
        }
        
        public void peerLeft(HGPeerIdentity peer)
        {
        	DU.log.info("Disko peer left: " + peer);
            if (jobMasterPeer != null && jobMasterPeer.equals(peer))
            {
                dfpeer.shutdown();
                jobMasterPeer = null;
                dfpeer = null;
                manageActivity.getState().setStarted();
                hgpeer.getExecutorService().execute(new WaitForWork());                
            }
        }
    }
    
    private class WaitForWork implements Runnable
    {        
        public void run()
        {
            while (dfpeer == null)
            {
                if (hgpeer.getConnectedPeers().isEmpty())
                    DU.log.info("No peers connected, at this time.");
                else
                {
                    Message msg = Messages.createMessage(Performative.CallForProposal, 
                                                         manageActivity.getType(), 
                                                         manageActivity.getId());
                    DU.log.info("Call for work proposal on peers: " + hgpeer.getConnectedPeers());                
                    hgpeer.getPeerInterface().broadcast(msg);
                }
                try { Thread.sleep(5000); } catch (InterruptedException ex) { break; }
            }       
        }
    }
    
    private class WorkerJobListener implements JobListener<AnalysisContext<TextDocument>>
    {
        public void startJob(Job job, AnalysisContext<TextDocument> ctx, Object source)
        {
        }

        public void endJob(Job job, 
        				   AnalysisContext<TextDocument> ctx, 
        				   Object source, 
        				   List<DistributedException> exL)
        {
            Message msg = Messages.createMessage(Performative.Inform, 
                                                 manageActivity.getType(), 
                                                 manageActivity.getId());
            if (exL.isEmpty())
            {
                Structs.combine(msg, struct("content",
                                            struct("head", "job-completed",
                                                   "job", job,
                                                   "result", 
                                                   ctx.getTopScope())));
            }
            else
            {
            	List<String> L = new ArrayList<String>();
            	for (DistributedException ex:exL) 
            		L.add(DU.printStackTrace(ex.getCause()));
                Structs.combine(msg, struct("content",
                        struct("head", "job-failed",
                               "job", job,
                               "result", L)));
            }
            DU.log.info("Reporting job result to master: " + msg);
            hgpeer.getPeerInterface().send(hgpeer.getNetworkTarget(jobMasterPeer), msg);
        }                	
    }
}
