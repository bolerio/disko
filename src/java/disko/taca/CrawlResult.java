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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CrawlResult
{
    public static final String MIME = "mime";
    public static final String ENCODING = "encoding";
    public static final String LAST_MODIFIED = "last-modified";
    
	private String url;
	private int depth;
	private CrawlRoot from;
	private Map<String, Object> metaData = new HashMap<String, Object>();
	
	public CrawlResult()
	{	    
	}
	public CrawlResult(String url, int depth, CrawlRoot from)
	{
	    this.url = url;
	    this.depth = depth;
	    this.from = from;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public int getDepth()
	{
		return depth;
	}
	public void setDepth(int depth)
	{
		this.depth = depth;
	}
	public CrawlRoot getFrom()
	{
		return from;
	}
	public void setFrom(CrawlRoot from)
	{
		this.from = from;
	}
    public Map<String, Object> getMetaData()
    {
        return metaData;
    }
    public void setMetaData(Map<String, Object> metaData)
    {
        this.metaData = metaData;
    }
    public String getMime()
    {
        return (String)metaData.get(MIME);
    }
    public Date getLastModified()
    {
        return (Date)metaData.get(LAST_MODIFIED);
    }
}
