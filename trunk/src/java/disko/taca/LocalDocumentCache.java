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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;

import disko.DU;
import disko.utils.DiscoProxySettings;

public class LocalDocumentCache
{
    private File directory;
    
    private File getFileForUrl(String url)
    {
        URL uu = null;
        try { uu = new URL(url); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        String subdir = uu.getPort() == -1 ? uu.getHost() : uu.getHost() + "_" + uu.getPort();
        File result = new File(directory, uu.getProtocol() + File.separator + subdir);
        if (!DU.isEmpty(uu.getPath()))
            result = new File(result, uu.getPath());
        if (!DU.isEmpty(uu.getQuery()))
        {
            String query = URLEncoder.encode(uu.getQuery());
            for (int i = 0; i < query.length(); )
            {
                String part = query.substring(i, Math.min(query.length(), i+200));
                result = new File(result, part);
                i+=200;
            }
        }        
        return new File(result.getParentFile(), result.getName() + ".cache");
    }
    
    public void cacheUrl(String url)
    {
        InputStream in = null; 
        try
        {       
            in = DiscoProxySettings.newConnection(new URL(url)).getInputStream();
            cacheUrl(url, in);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            if (in != null) try { in.close(); } catch (Throwable t) { }
        }         
    }
    
    public void cacheUrl(String url, InputStream in)
    {
        File f = getFileForUrl(url);
        OutputStream out = null;
        try
        {       
            f.getParentFile().mkdirs();
            out = new FileOutputStream(f);
            int len = 4096;
            byte [] A = new byte [len];
            for (int read = in.read(A, 0, len); read > -1; read = in.read(A, 0, len))
                out.write(A, 0, read);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            if (out != null) try { out.close(); } catch (Throwable t) { }
        }           
    }
    
    public void cacheUrl(String url, String data)
    {
        cacheUrl(url, data.getBytes());
    }
    
    public void cacheUrl(String url, byte [] data)
    {
        cacheUrl(url, new ByteArrayInputStream(data));
    }
    
    public File getCachedUrl(String url)
    {
        File f = this.getFileForUrl(url);
        if (f.exists())
            return f;
        else
            return null;
    }

    public File getDirectory()
    {
        return directory;
    }

    public void setDirectory(File directory)
    {
        this.directory = directory;
    }    
}
