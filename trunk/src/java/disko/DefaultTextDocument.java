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
import java.lang.ref.WeakReference;
import java.net.URL;


/**
 * Standard text document, with paragraph detection.
 * @author muriloq
 *
 */
public class DefaultTextDocument extends UrlTextDocument 
{	
	protected synchronized String load() 
	{
		String s = super.load();
		ParagraphDetector.detectParagraphs(s, this.annotations);
		return (fullText = new WeakReference<String>(s)).get();
	}

	public DefaultTextDocument()
	{		
	}
	
	public DefaultTextDocument(URL url)
	{		
		super(url);
	}
	
	public DefaultTextDocument(File f)
	{
		super(f);
	}
}
