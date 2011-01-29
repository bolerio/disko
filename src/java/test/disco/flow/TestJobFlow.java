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
package test.disco.flow;

import java.util.concurrent.Future;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.app.dataflow.DefaultChannelManager;
import org.hypergraphdb.app.dataflow.JobDataFlow;

import disko.AnalysisContext;
import disko.TextDocument;
import disko.flow.dist.DocAtUrlJob;
import disko.flow.dist.DocJobAdapter;
import disko.flow.networks.IENetworkCoarse;

public class TestJobFlow
{
    public static final String graphLocation = "/home/borislav/data/graphs/wn";
    
    public static void main(String [] argv)
    {
        try
        {
                  
        System.setProperty("relex.morphy.Morphy", "org.disco.relex.MorphyHGDB");
        System.setProperty("morphy.hgdb.location", graphLocation);        
        System.setProperty("gate.home", "/home/borislav/gate");
        IENetworkCoarse ieNetwork = new IENetworkCoarse();
//        ieNetwork.setEntityTransmitPort(9001);
//        ieNetwork.setEntityReceivePort(9002);
//        ieNetwork.setEntityHost("localhost");
        ieNetwork.getLinkGrammarServers().add("http://localhost:9000");        
        ieNetwork.create();
//        ieNetwork.setContext(new AnalysisContext<TextDocument>(HGEnvironment.get(graphLocation), 
//                                    new HTMLDocument(new URL("http://www.opencog.org/wiki/OpenCog_Core"))));
//        ieNetwork.start();
        ieNetwork.setContext(new AnalysisContext<TextDocument>(HGEnvironment.get(graphLocation), null));
        JobDataFlow<AnalysisContext<TextDocument>> taskNetwork = 
            JobDataFlow.make(ieNetwork, new DocJobAdapter(), new DocAtUrlJob(null));
        taskNetwork.setChannelManager(new DefaultChannelManager());
        Future<?> result = taskNetwork.start();
        taskNetwork.submitJob(new DocAtUrlJob("http://www.opencog.org/wiki/OpenCog_Core"));
//        taskNetwork.submitJob(new DocAtUrlJob("http://www.opencog.org/wiki/OpenCog_Framework"));        
//        taskNetwork.submitJob(new DocAtUrlJob("http://en.wikipedia.org/wiki/Software_framework"));
//        taskNetwork.submitJob(new DocAtUrlJob("http://www.opencog.org/wiki/AtomSpace"));
//        taskNetwork.submitJob(new DocAtUrlJob("http://www.opencog.org/wiki/Atom")); 
//        taskNetwork.submitJob(new DocAtUrlJob(null));
        System.out.println("Network result " + result);
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
    }
}
