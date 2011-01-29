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

import java.io.File;
import org.hypergraphdb.*;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.dataflow.DataFlowPeer;
import org.hypergraphdb.app.dataflow.JobDataFlow;
import org.hypergraphdb.handle.UUIDHandleFactory;
import org.hypergraphdb.peer.HyperGraphPeer;

import disko.AnalysisContext;
import disko.HTMLDocument;
import disko.flow.dist.DocAtUrlJob;

public class NetPeerTest
{
    public static void die(String msg)
    {
        System.err.println(msg);
        System.exit(-1);
    }
    
    public static void initSystem(String graphLocation)
    {
        System.setProperty("gate.home", "/home/borislav/gate");
        System.setProperty("relex.morphy.Morphy", "org.disco.relex.MorphyHGDB");
        System.setProperty("morphy.hgdb.location", graphLocation);
        System.setProperty("relex.algpath", "/home/borislav/disco/trunk/data/relex-semantic-algs.txt");
        //System.setProperty("relex.parser.LinkParser.pathname", "D:/work/disco/trunk/data/linkparser"); 
        System.setProperty("relex.parser.LinkParser.pathname", "/home/borislav/disco/trunk/data/linkparser"); 
        relex.RelexProperties.setProperty("relex.parser.LinkParser.pathname", System.getProperty("relex.parser.LinkParser.pathname"));
        System.setProperty("wordnet.configfile", "/home/borislav/disco/trunk/data/wordnet/file_properties.xml");
        System.setProperty("EnglishModelFilename", "/home/borislav/disco/trunk/data/sentence-detector/EnglishSD.bin.gz");        
    }
    
    public static void main(String [] argv)
    {
        boolean master = false;
        String graphLocation = null;
        String peerConfigFile = null;
        HGHandle networkHandle = null;        
        for (int i = 0; i < argv.length; i++)
        {
            String name = argv[i];
            if (++i == argv.length)
                die("Missing value for option " + name);
            String value = argv[i];
            if ("graph".equals(name))
                graphLocation = value;
            else if ("network".equals(name))
                networkHandle = UUIDHandleFactory.I.makeHandle(value);
            else if ("peer".equals(name))
                peerConfigFile = value;
            else if ("master".equals(name))
                master = Boolean.valueOf(value);
            else
                die("Unknown option '" + name + "'");
        }
        if (graphLocation == null)
            die ("Missing graph option");
        else if (peerConfigFile == null)
            die ("Missing 'peer'");
        initSystem(graphLocation);
        HyperGraph graph = HGEnvironment.get(graphLocation);
        if (networkHandle == null)
            networkHandle = hg.findOne(graph, hg.type(JobDataFlow.class));
        HyperGraphPeer hgpeer = new HyperGraphPeer(new File(peerConfigFile));
        hgpeer.getConfiguration().put("localDB", graphLocation);
        try
        {
            hgpeer.start(null, null).get();
            DataFlowPeer<AnalysisContext<HTMLDocument>> dfpeer = 
                new DataFlowPeer<AnalysisContext<HTMLDocument>>(hgpeer, networkHandle);        
            dfpeer.setMaster(master);    
            dfpeer.getNetwork().setContext(new AnalysisContext<HTMLDocument>(graph, null));
            dfpeer.start();
            if (dfpeer.isMaster())
                dfpeer.submitJob(new DocAtUrlJob("http://www.opencog.org/wiki/OpenCog_Core"));            
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
