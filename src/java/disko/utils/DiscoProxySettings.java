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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * <p>
 * Configure proxy settings for URL access within Disco. This is a simple
 * class holding global proxy settings that must be configured by whatever 
 * main program is calling the framework.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class DiscoProxySettings
{	
	public static String proxyHost = null;
    public static int  proxyPort = 80;
	public static String proxyUser = null;
	public static String proxyPassword = null;
	public static Proxy.Type proxyType = Proxy.Type.HTTP;	 
	
	/**
	 * 
	 * <p>
	 * Convenience method to open a URL connection by using the
	 * proxy settings. If proxyHost is null, the default is to  just
	 * call <code>url.openConnection</code>.
	 * </p>
	 *
	 * @param url
	 * @return
	 */
	public static URLConnection newConnection(URL url) throws IOException
	{
		URLConnection connection;
		String externalForm = url.toExternalForm();
		url = new URL(externalForm.replace(" ", "%20"));
		if (DiscoProxySettings.proxyHost != null)
		{
			connection = url.openConnection(new Proxy(Proxy.Type.HTTP, 
													 new InetSocketAddress(DiscoProxySettings.proxyHost, 
															 			   DiscoProxySettings.proxyPort)));
            if (DiscoProxySettings.proxyUser != null)
            {
                String enc= new String(Base64.encodeBase64(
                        new String(DiscoProxySettings.proxyUser + ":" + 
                        		   DiscoProxySettings.proxyPassword).getBytes()));
                connection.setRequestProperty("Proxy-Authorization", "Basic " + enc);
            }
		}
		else
			connection = url.openConnection();		
		return connection;
	}
}
