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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.hypergraphdb.peer.HGPeerIdentity;
import org.hypergraphdb.peer.HyperGraphPeer;
import org.hypergraphdb.peer.Message;
import org.hypergraphdb.peer.Messages;
import org.hypergraphdb.peer.Performative;
import org.hypergraphdb.peer.workflow.Activity;

import disko.DU;
import static org.hypergraphdb.peer.Structs.*;
import static org.hypergraphdb.peer.Messages.*;

/**
 * 
 * <p>
 * This activity is used to perform execution of scripts between peers.
 * The scripts can be written in any language executing on the JVM platform
 * as long as an implementation of the <code>javax.script.ScriptEngine</code>
 * is provided in the {@link DiskoBootstrap} configuration. Several such 
 * scripting engines can be configured.    
 * </p>
 *
 * <p>
 * The <code>ScriptContext</code>, that all engines registered with this activity
 * share, has only one global scope which is bound to the <code>objectContext</code>
 * of the {@link HyperGraphPeer}. Thus, to make an object available to scripting
 * engines, put it in the map returned by <code>HyperGraphPeer.getObjectContext</code>.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class ScriptShellActivity extends Activity
{
    /**
     * This is a singleton activity (only one per peer) and this is its ID.
     */
    public static final UUID ID = UUID.fromString("9b0c97d7-6a64-4a84-af24-25e67cfc5e7d");
    
    public static final String TYPENAME = "script-shell";
    
    public static final String SCRIPT_ENGINE = "script-engine";
    public static final String SCRIPT_CONTENT = "script-content";

    static class EvalTask
    {
        Integer id = null;
        RemoteMethodCallback<Object> callback = null;
    }
    
    private Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
    private ScriptContext scriptContext = new SimpleScriptContext();
    private AtomicInteger expressionId = new AtomicInteger(0);

    private Map<Integer, EvalTask> pendingEvaluations  = 
        Collections.synchronizedMap(new HashMap<Integer, EvalTask>());
    
    public ScriptShellActivity(HyperGraphPeer thisPeer)
    {
        super(thisPeer, ID);
    }
    
    @Override
    public void handleMessage(Message msg)
    {
        Performative action = Performative.makeConstant((String)getPart(msg, PERFORMATIVE));
        if (Performative.Request.equals(action))
        {
            String engineName = getPart(msg, LANGUAGE);
            String script = getPart(msg, CONTENT, "script");
//            DU.log.info("SSACT: Request to run script <" + script + "> on engine " + engineName);
            Map<String, Object> bindings = getPart(msg, CONTENT, "bindings");
            ScriptEngine engine = engines.get(engineName);
            if (engine == null)
                reply(msg, Performative.NotUnderstood, "unknown script engine '" + engineName + "'");
            try
            {
            	ScriptContext ctx = new SimpleScriptContext();
            	ctx.setBindings(scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE), 
            					ScriptContext.GLOBAL_SCOPE);
            	if (bindings != null)
            	{
            	    Set<String> variables = new HashSet<String>();
            	    variables.addAll(bindings.keySet());
            	    for (String var: variables)
            	    {
            	        Object value =  getPart(bindings, var); // perform JSON deserialization
            	        bindings.put(var, value);
            	    }
            		ctx.setBindings(new SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE);
            	}
//            	DU.log.info("SSACT: Running script and returning result");
                reply(msg, Performative.Agree, engine.eval(script, ctx));
//                DU.log.info("SSACT: Script result sent back.");
            }
            catch (Throwable t)
            {
                t.printStackTrace(System.err);
                reply(msg, Performative.Failure, DU.printStackTrace(t));
            }
        }
        else if (Performative.Agree.equals(action))
        {
            String taskId = getPart(msg, IN_REPLY_TO);
//            DU.log.info("SSACT: Received response for task " + taskId);
            if (taskId != null)
            {    
                EvalTask task = pendingEvaluations.remove(Integer.parseInt(taskId));
//                DU.log.info("SSACT: Task removed, notifying of success.");
                if (task != null)
                    try 
                	{ 
                    	task.callback.onSuccess(getPart(msg, CONTENT));
//                    	DU.log.info("SSACT: Onsuccess called back on task " + taskId);
                    }
                    catch (Throwable t) { DU.log.error(t); }
            }
        }
        else if (Performative.NotUnderstood.equals(action))
        {
        	DU.log.info("SSACT: peer didn't understand " + msg);
        }
        else if (Performative.Failure.equals(action))
        {
        	DU.log.warn("SSACT: peer failed with message " + msg);
            String taskId = getPart(msg, IN_REPLY_TO);
            if (taskId != null)
            {    
//            	DU.log.info("SSACT: Failure is on task ID " + taskId + " reporting to callback.");
                EvalTask task = pendingEvaluations.remove(Integer.parseInt(taskId));
                if (task != null) try 
                {
                    Object ex = getPart(msg, CONTENT);
                    if (ex == null) ex = "null";
                    if (ex instanceof Throwable)
                        task.callback.onException((Throwable)ex);
                    else
                        task.callback.onException(new RemoteException(ex.toString()));
                }
                catch (Throwable t) 
                { 
                    task.callback.onException(t);
                    DU.log.error(t); 
                }
            }            
        }
        else
        {
        	DU.log.info("SSACT: unknown performative: " + action);
            reply(msg, Performative.NotUnderstood, "irrelevant performative '" + action + "'");
        }
    }

    @Override
    public void initiate()
    {
        scriptContext.setBindings(new SimpleBindings(getThisPeer().getObjectContext()), 
                                  ScriptContext.GLOBAL_SCOPE);
    }
    
    public Map<String, ScriptEngine> getEngines()
    {
        return engines;
    }
 
    public ScriptContext getScriptContext()
    {
        
        return scriptContext;
    }
    
	public static <T> T executeScript(HyperGraphPeer thisPeer, 
                                   	  HGPeerIdentity target, 
                                      String engine, 
                                      String script,
                                      Map<String, Object> bindings)
    {
    	SynchronousRemoteCallback<T> callback = new SynchronousRemoteCallback<T>();
    	executeScript(thisPeer, target, engine, script, bindings, callback);
        synchronized (callback)
		{
			try
			{
				if (!callback.isDone())
					callback.wait();
			}
			catch (InterruptedException ex)
			{
				throw new RuntimeException("Remote call interrupted.");
			}
		}        
    	
    	if (callback.isFailed())
    		throw new RuntimeException(" while calling script " + script + " on target " + target, callback.getException());
    	else
    		return callback.getResult();
    }

	/**
	 * <p>
	 * This assumes a Java-like method call: <code>obj.m(arg1, arg2, ..., argn)</code>.
	 * </p>
	 * 
	 * @param <T>
	 * @param thisPeer
	 * @param target
	 * @param engine
	 * @param methodRef
	 * @param args
	 */
	public static <T> T executeMethodCall(HyperGraphPeer thisPeer, 
                                     	  HGPeerIdentity target, 
                                     	  String engine,
                                     	  String methodRef,
                                     	  Object...args)
	{
		Map<String, Object> bindings = new HashMap<String, Object>();
		String scriptArgs = "";
		for (int i = 0; i < args.length; i++)
		{
			String argName = "remotearg" + i;
			bindings.put(argName, args[i]);
			scriptArgs += argName;
			if (i < args.length - 1)
				scriptArgs += ",";
		}
		return ScriptShellActivity.executeScript(thisPeer, 
												 target, 
												 engine, 
												 methodRef + "(" + scriptArgs + ")", 
												 bindings);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> void executeScript(HyperGraphPeer thisPeer, 
                                     	 HGPeerIdentity target, 
                                     	 String engine, 
                                     	 String script,
                                     	 Map<String, Object> bindings,
                                     	 RemoteMethodCallback<T> callback)
    {        
        ScriptShellActivity act = (ScriptShellActivity)thisPeer.getActivityManager().getActivity(ID);
        if (act == null)
            throw new RuntimeException("Cannot execute remote script since there's no local ScriptShell activity running.");
        Message msg = Messages.createMessage(Performative.Request, act);
        Integer evalId = act.expressionId.incrementAndGet();
        if (bindings != null)
        {
            Set<String> variables = new HashSet<String>();
            variables.addAll(bindings.keySet());
            for (String var: variables)
            {
                Object value =  svalue(bindings.get(var));
                bindings.put(var, value);
            }
        }        
        combine(msg, struct(REPLY_WITH, evalId.toString(),
                            LANGUAGE, engine,
                            CONTENT, struct("script", script, "bindings", bindings)));
        EvalTask task = new EvalTask();
        task.id = evalId;
        task.callback = (RemoteMethodCallback<Object>)callback;
        act.pendingEvaluations.put(evalId, task);        
//        DU.log.info("SSACT: Requesting new task eval " + task.id + " of script " + script);
        act.send(target, msg);
    }
    
    public static HGPeerIdentity findPeerByNetworkTarget(HyperGraphPeer thisPeer,
			 											 Object networkTarget,
			 											 int retryCount,
			 											 long sleepTime)
    {
    	for (int i = 0; i < retryCount; i++)
    	{
    		HGPeerIdentity id = findPeerByNetworkTarget(thisPeer, networkTarget);
    		if (id != null)
    			return id;
    		try { Thread.sleep(sleepTime); }
    		catch (InterruptedException ex) { return null; }
    	}
    	return null;
    }
    
    public static HGPeerIdentity findPeerByNetworkTarget(HyperGraphPeer thisPeer,
    													 Object networkTarget)
    {
		for (HGPeerIdentity peer : thisPeer.getConnectedPeers())
		{
			Object nt = thisPeer.getNetworkTarget(peer);
			if (nt == null)
				continue;
			if (nt.equals(networkTarget))
			{
				return peer;
			}
		}
		return null;
    }

	public static HGPeerIdentity findPeerByNetworkTargetPrefix(HyperGraphPeer thisPeer,
			 	   											   String netTargetPrefix,
															   int retryCount,
															   long sleepTime)
	{
		for (int i = 0; i < retryCount; i++)
		{
			HGPeerIdentity id = findPeerByNetworkTargetPrefix(thisPeer, netTargetPrefix);
			if (id != null)
				return id;
			try { Thread.sleep(sleepTime); }
			catch (InterruptedException ex) { return null; }
		}
		return null;
	}

	public static HGPeerIdentity findPeerByNetworkTargetPrefix(HyperGraphPeer thisPeer,
			 								   			 	   String netTargetPrefix)
	{
		for (HGPeerIdentity peer : thisPeer.getConnectedPeers())
		{
			Object nt = thisPeer.getNetworkTarget(peer);
			if (nt == null)
				continue;
			if (nt.toString().startsWith(netTargetPrefix))
				return peer;
		}
		return null;
	}
	
    
    
    public String getType()
    {
        return TYPENAME;
    }
}
