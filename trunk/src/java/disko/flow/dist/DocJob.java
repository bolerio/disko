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

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.app.dataflow.Job;

public class DocJob implements Job, java.io.Serializable
{
    private static final long serialVersionUID = -1;
    
    private HGHandle handle;
    private HGHandle scopeHandle;
    private Object scope;
    
    public HGHandle getHandle()
    {
        return handle;
    }

    public void setHandle(HGHandle handle)
    {
        this.handle = handle;
    }

    public HGHandle getScopeHandle()
    {
        return scopeHandle;
    }

    public void setScopeHandle(HGHandle scopeHandle)
    {
        this.scopeHandle = scopeHandle;
    }

	public Object getScope()
	{
		return scope;
	}

	public void setScope(Object scope)
	{
		this.scope = scope;
	}    
	
	public String toString()
	{
		return "DocJob[handle:" + handle + ", scopeHandle=" + scopeHandle + ", scope=" + scope + "]";
	}
}
