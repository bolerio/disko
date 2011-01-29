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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import disko.utils.DiscoProxySettings;

public class DefaultContentDownloader implements DownloaderInterface
{
	public InputStream getInputStream(String location) throws IOException
	{
		return DiscoProxySettings.newConnection(new URL(location)).getInputStream();
	}

	public byte[] readRawData(String location)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = null;
		try
		{
			URL url = new URL(location);
			URLConnection connection = DiscoProxySettings.newConnection(url);
			connection.setConnectTimeout(60000);
			connection.setReadTimeout(60000);
			in = DiscoProxySettings.newConnection(url).getInputStream();
			int len = 4096;
			byte [] buf = new byte[len];			
			for (int read = in.read(buf, 0, len); read > -1; read = in.read(buf, 0, len))
				out.write(buf, 0, read);
			return out.toByteArray();
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex); 
		}		
		finally
		{
			if (in != null) try { in.close(); } catch (Throwable t) { }
		}		
	}

	public String readText(String location)
	{
		StringBuffer buffer = new StringBuffer();
		InputStream in = null;
		try
		{
			URL url = new URL(location);
			URLConnection connection = DiscoProxySettings.newConnection(url);
			connection.setConnectTimeout(60000);
			connection.setReadTimeout(60000);
			in = DiscoProxySettings.newConnection(url).getInputStream();
			InputStreamReader reader = new InputStreamReader(in);
			int len = 4096;
			char [] A = new char[len];			
			for (int read = reader.read(A, 0, len); read > -1; read = reader.read(A, 0, len))
				buffer.append(A, 0, read);
			return buffer.toString();
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex); 
		}		
		finally
		{
			if (in != null) try { in.close(); } catch (Throwable t) { }
		}	
	}
}
