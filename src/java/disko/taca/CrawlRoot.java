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

import java.util.HashSet;
import java.util.Set;

import org.hypergraphdb.util.HGUtils;

public class CrawlRoot implements Comparable<CrawlRoot>
{
	private boolean active;
	private String url;
	private int depth;
	private Set<String> mimesToFollow = new HashSet<String>();
	private Set<String> mimesToReturn = new HashSet<String>();
	private long crawlingInterval;
	private long lastCrawlTimestamp;
	private boolean domainRestricted; // remain with domain of root url
	private boolean crawlSubdomains; // crawl sub-domains of top-level domain of root url
	
	public boolean isDomainRestricted()
	{
		return domainRestricted;
	}
	public void setDomainRestricted(boolean domainRestricted)
	{
		this.domainRestricted = domainRestricted;
	}
	public boolean isCrawlSubdomains()
	{
		return crawlSubdomains;
	}
	public void setCrawlSubdomains(boolean crawlSubdomains)
	{
		this.crawlSubdomains = crawlSubdomains;
	}
	public boolean isActive()
	{
		return active;
	}
	public void setActive(boolean active)
	{
		this.active = active;
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
	public Set<String> getMimesToFollow()
	{
		if (mimesToFollow == null)
			mimesToFollow = new HashSet<String>();
		return mimesToFollow;
	}
	public void setMimesToFollow(Set<String> mimesToFollow)
	{
		this.mimesToFollow = mimesToFollow;
	}
	public Set<String> getMimesToReturn()
	{
		if (mimesToReturn == null)
			mimesToReturn = new HashSet<String>();		
		return mimesToReturn;
	}
	public void setMimesToReturn(Set<String> mimesToReturn)
	{
		this.mimesToReturn = mimesToReturn;
	}
	public long getCrawlingInterval()
	{
		return crawlingInterval;
	}
	public void setCrawlingInterval(long crawlingInterval)
	{
		this.crawlingInterval = crawlingInterval;
	}
	public long getLastCrawlTimestamp()
	{
		return lastCrawlTimestamp;
	}
	public void setLastCrawlTimestamp(long lastCrawlTimestamp)
	{
		this.lastCrawlTimestamp = lastCrawlTimestamp;
	}

	public int compareTo(CrawlRoot other)
	{
		if (url == null)
			return other.getUrl() == null ? 0 : -1;
		return url.compareTo(other.getUrl());
	}
	
	public int hashCode()
	{
		return url == null ? 0 : url.hashCode();
	}
	
	public boolean equals(Object x)
	{
		if (! (x instanceof CrawlRoot))
			return false;
		CrawlRoot other = (CrawlRoot)x;
		return HGUtils.eq(url, other.url);
	}
}
