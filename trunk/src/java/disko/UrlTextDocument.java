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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.hypergraphdb.annotation.HGIgnore;

import disko.utils.CharSequenceReader;

import relex.corpus.TextInterval;

public class UrlTextDocument implements TextDocument
{
    protected String urlString;
    protected String charSet = null;
    protected String title, summary;    
    @HGIgnore
    protected URL url;
    @HGIgnore
    protected WeakReference<String> fullText = new WeakReference<String>(null);
    @HGIgnore
    protected ArrayList<Ann> annotations = new ArrayList<Ann>();

    public UrlTextDocument()
    {
    }

    public UrlTextDocument(String url)
    {
        this.urlString = url;
        try
        {
            this.url = new URL(url);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public UrlTextDocument(URL url)
    {
        this.url = url;
        if (url != null)
            urlString = url.toExternalForm();
    }

    public UrlTextDocument(File f)
    {
        try
        {
            this.url = f.toURI().toURL();
            this.urlString = url.toExternalForm();
        }
        catch (Exception ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

    protected synchronized String load()
    {    	
        try
        {
        	return (fullText = new WeakReference<String>(
        			ContentDownloader.getInstance().readText(urlString))).get();         	
        }		
        catch (Exception ex)
        {
            throw new RuntimeException("Unable to read resource at URL '" + url
                                       + "'", ex);
        }
    }

    public CharSequence getText(TextInterval interval)
    {
        String s = fullText.get();
        if (s == null)
            s = load();
        return s.subSequence(interval.getStart(), interval.getEnd());
    }

    public synchronized String getFullText()
    {
        String s = fullText.get();
        if (s == null)
            s = load();
        return s;
    }

    public Reader createReader() throws IOException
    {
        String s = fullText.get();
        if (s != null)
            return new CharSequenceReader(s);
        else
            return new InputStreamReader(url.openStream());
    }

    public URL getUrl()
    {
        return url;
    }

    public void setUrlString(String urlString)
    {
        this.urlString = urlString;
        try
        {
            if (urlString != null)
                url = new URL(urlString);
            else
                url = null;
            fullText = new WeakReference<String>(null);
        }
        catch (Exception ex)
        {
            System.err.println("Invalid URL: " + urlString);
            throw new RuntimeException(ex);
        }
    }

    public String getUrlString()
    {
        return urlString;
    }

    public String getCharset()
    {
        return (charSet == null ? "US-ASCII" : charSet);
    }

    public void setCharset(String charset)
    {
        this.charSet = charset;
    }

    public List<Ann> getAnnotations()
    {
        return annotations;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
    
    public String toString()
    {
        return urlString != null ? urlString : "Text[" + this.fullText + "]";
    }
}
