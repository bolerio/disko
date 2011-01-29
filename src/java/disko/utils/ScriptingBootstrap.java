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
package disko.utils;

import java.util.Map;

import javax.script.ScriptEngine;
import static org.hypergraphdb.peer.Structs.*;
import org.hypergraphdb.peer.BootstrapPeer;
import org.hypergraphdb.peer.HyperGraphPeer;

import disko.DU;

public class ScriptingBootstrap implements BootstrapPeer
{
	public void bootstrap(HyperGraphPeer peer, Map<String, Object> config)
	{
        ScriptShellActivity scriptShell = new ScriptShellActivity(peer);        
        Map<String, Object> engines = getPart(config, "engines");
        if (engines != null) for (String E : engines.keySet())
        {
            String classname = (String)engines.get(E);                
            try
            {
                Class<?> clazz = Class.forName(classname);                    
                scriptShell.getEngines().put(E, (ScriptEngine)clazz.newInstance());   
            }
            catch (Throwable t)
            {
                DU.log.error("Unable to instantiate engine '" + E + "'", t);                    
            }
        }
        else
            DU.log.warn("Scriting activity configured, but without any scriping engines.");
        peer.getActivityManager().initiateActivity(scriptShell);
	}
}
