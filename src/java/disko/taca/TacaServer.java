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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.hypergraphdb.HyperGraph;

/**
 * 
 * <p>
 * Provides global context for the execution of TACA, manages thread pools and task
 * scheduling.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class TacaServer
{
	private Executor executor;
	private HyperGraph graph;
	
	protected void initialize()
	{
		executor = Executors.newCachedThreadPool();
	}
	
	/**
	 * Submit a command for later execution.
	 * 
	 * @param command
	 */
	public void submit(Runnable command)
	{
		executor.execute(command);
	}

	public HyperGraph getGraph()
	{
		return graph;
	}
}
