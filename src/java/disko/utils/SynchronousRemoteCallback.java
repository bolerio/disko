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

public class SynchronousRemoteCallback<T> implements RemoteMethodCallback<T>
{
	private boolean done;
	private T result;
	private Throwable ex;
	
	public synchronized void onException(Throwable t)
	{
		ex = t;
		done = true;
		this.notifyAll();
	}

	public synchronized void onSuccess(T result)
	{
		this.result = result;
		done = true;
		this.notifyAll();
	}
	
	public boolean isDone() { return done; }
	public boolean isFailed() { return ex != null; }
	public Throwable getException() { return ex; }
	public T getResult() { return result; }
}
