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

public class DocAtUrlJob extends DocJob
{
    private static final long serialVersionUID = -1;    
    private String url;
    
    public DocAtUrlJob()
    {        
    }
    
    public DocAtUrlJob(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
    
    public int hashCode()
    {
        return (url == null) ? 0 : url.hashCode();
    }
    
    public boolean equals(Object x)
    {
        if (! (x instanceof DocAtUrlJob))
            return false;
        DocAtUrlJob y = (DocAtUrlJob)x;
        if (url == null)
            return y.url == null;
        else
            return url.equals(y.url);            
    }
    
    public String toString()
    {
        return super.toString() + "@" + url;
    }
}
