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
package disko.service;

/**
 * <p>
 * A service is a background process that you can stop. A service normally 
 * takes up and runs within a separate thread. 
 * </p>
 * 
 * <p>
 * This is a very simply interface that covers a very frequent pattern where
 * a <code>Runnable</code> needs to be also "stoppable". A very common use case
 * is for infinite loops that need to be exited only on demand, or simply for
 * very long running processes that can be interrupted in a graceful manner.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public interface DiskoService extends Runnable
{
	void stop();
	boolean isRunning();
}
