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
package disko;

import java.io.IOException;
import java.io.InputStream;

public class ContentDownloader implements DownloaderInterface
{
	private DownloaderInterface implementation = null;
		
	private static ContentDownloader instance = null;

	@SuppressWarnings("unchecked")
	private ContentDownloader()
	{
		String implClassName = System.getProperty("org.disco.ContentDownloader");
		if (implClassName == null)
			implementation = new DefaultContentDownloader(); 
		else try
		{
			Class<DownloaderInterface> cl = (Class<DownloaderInterface>)
				Class.forName(implClassName);
			implementation = cl.newInstance();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}	
	
	public synchronized static ContentDownloader getInstance()
	{
		if (instance == null)
			instance = new ContentDownloader();
		return instance;
	}
	
	public ContentDownloader(DownloaderInterface implementation)
	{
		this.implementation = implementation;
	}

	public InputStream getInputStream(String location) throws IOException
	{
		DU.log.info("CONTENTDOWNLOAD InputStream from " + location);
		InputStream in = implementation.getInputStream(location);
		DU.log.info("CONTENT DOWNLOAD done.");
		return in;
	}

	public byte[] readRawData(String location)
	{
		DU.log.info("CONTENTDOWNLOAD readRawData from " + location);
		byte [] A = implementation.readRawData(location);
		DU.log.info("CONTENT DOWNLOAD done.");
		return A;
	}

	public String readText(String location)
	{
		DU.log.info("CONTENTDOWNLOAD readText from " + location);
		String text = implementation.readText(location);
		DU.log.info("CONTENT DOWNLOAD done.");
		return text;

	}

	public DownloaderInterface getImplementation()
	{
		return implementation;
	}

	public void setImplementation(DownloaderInterface implementation)
	{
		if (implementation == null)
			throw new NullPointerException("ContentDownloader implementation can't be null.");
		this.implementation = implementation;
	}		
}
