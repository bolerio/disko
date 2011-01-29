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

import static org.hypergraphdb.peer.Structs.*;

import java.io.IOException;
import java.util.Map;

import org.hypergraphdb.peer.serializer.JSONReader;

import disko.DU;

/**
 * <p>
 * Main program for Disko distributed job processing. The program expects a JSON formatted
 * configuration file defining P2P connectivity and other optional parameters driving
 * the behavior of this particular processing node.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class StartMeUp
{

    @SuppressWarnings("unchecked")
    public static void main(String argv[])
    {
        if (argv.length != 1)
        {
            System.out.println("Syntax: java ... org.disko.flow.dist.StartMeUp <config-file>");
            System.exit(-1);
        }
        
        System.out.println("Starting Disko processing node with configuration at " + argv[0]);
        
        Map<String, Object> configuration = null;
        JSONReader reader = new JSONReader();
        try
        {            
            configuration = (Map<String, Object>)getPart(reader.read(DU.readFile(argv[0])));              
        } 
        catch (IOException e)
        {
            throw new RuntimeException(e);          
        }
        
        Boolean isMaster = getOptPart(configuration, Boolean.FALSE, "isJobMaster"); 
        if (isMaster)
        {
            DiskoJobMaster jobMaster = new DiskoJobMaster(configuration);
            jobMaster.start();
        }
        else
        {
            System.setProperty("disco.pdf.max.pages", "20");
            System.setProperty("gate.home", "c:/tools/gate");
            System.setProperty("relex.morphy.Morphy", "org.disco.relex.MorphyHGDB");
            System.setProperty("morphy.hgdb.location", (String)configuration.get("localDB"));
            System.setProperty("relex.parser.LinkParser.pathname", "D:/work/disco/trunk/data/linkparser"); 
            relex.RelexProperties.setProperty("relex.parser.LinkParser.pathname", System.getProperty("relex.parser.LinkParser.pathname"));
            System.setProperty("wordnet.configfile", "d:/work/disco/trunk/data/wordnet/file_properties.xml");
            System.setProperty("EnglishModelFilename", "d:/work/disco/trunk/data/sentence-detector/EnglishSD.bin.gz");            
            DiskoJobWorker jobWorker = new DiskoJobWorker(configuration);
            jobWorker.start();
        }
    }
}
