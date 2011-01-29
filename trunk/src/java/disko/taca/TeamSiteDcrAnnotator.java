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

import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;

import disko.*;

public class TeamSiteDcrAnnotator implements Danalyzer<AnalysisContext<TextDocument>>
{	
	private static class TagData
	{
		public String name;
		public HashMap attributes;
		public int line, column;
		public long position;
	}
	
	public class TeamSiteHandler extends DefaultHandler
	{
		private ArrayList<TagData> tagData = new ArrayList<TagData>();  
		private Locator locator = null;
		
		public TeamSiteHandler()
		{
		}	

		public void setDocumentLocator(Locator locator)
		{
			this.locator = locator;
		}

		@SuppressWarnings("unchecked")
		public void startElement(String uri, 
								 String localName, 
								 String qName, 
								 Attributes atts) throws SAXException
		{
			TagData data = new TagData();
			data.name  = qName;
			data.attributes = new HashMap();
			for (int i = 0; i < atts.getLength();i++) 
				data.attributes.put(atts.getQName(i), atts.getValue(i));
			
			data.column = locator.getColumnNumber();
			data.line = locator.getLineNumber();
			tagData.add(data);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			TagData data = new TagData();
			data.name  = qName;
			data.column = locator.getColumnNumber();
			data.line = locator.getLineNumber();
			tagData.add(data);			
		}
	}

	private void toAnnotations(AnalysisContext<TextDocument> context, ArrayList<TagData> tagData)
	{
		Collections.sort(tagData, new Comparator<TagData>() {
			public int compare(TagData x, TagData y)
			{
				if (x.line < y.line)
					return -1;
				else if (x.line == y.line)
					return (x.column < y.column ? -1 : x.column == y.column ? 0 : 1);
				else
					return 1; 
			}}
		);

		int pos = 0;
		int line = 1;		
		Stack<MarkupAnn> remaining = new Stack<MarkupAnn>();	
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(context.getDocument().createReader());
		}
		catch (Exception t)
		{
			throw new RuntimeException(t);
		}
		for (TagData data : tagData)
		{
			while (line < data.line)
			{				
				try
				{
					for (int c = reader.read(); c != -1; c = reader.read())
					{
						pos++;
						if (c == '\r')
						{
							line++;
							reader.mark(1);
							c = reader.read();
							if (c == '\n') pos++;
							else reader.reset();
							break;
						}						
					}
				}
				catch (Exception ex)
				{
					throw new RuntimeException(ex);
				}
				line++;
			}
			
			MarkupAnn ann = null;
			if (data.attributes != null)
			{
				ann = new MarkupAnn();
				ann.getInterval().setStart(pos + data.column - 1);
				ann.setTag(data.name);				
				remaining.push(ann);
			}
			else
			{
				ann = remaining.pop();
				// tag closes without a body
				if (pos + data.column - 1 - data.name.length() - 3 <= ann.getInterval().getStart()) 
					ann.getInterval().setEnd(ann.getInterval().getStart());
				else
					ann.getInterval().setEnd(pos + data.column - 1 - data.name.length() - 3);
				context.add(ann);
			}
		}
		try { reader.close(); } catch (Exception ex) {}
	}
	
	public void process(AnalysisContext<TextDocument> context)
	{
		try
		{
	        TeamSiteHandler handler = new TeamSiteHandler();
	        SAXParserFactory factory = SAXParserFactory.newInstance();
	        factory.setValidating(false);
	        SAXParser parser = factory.newSAXParser();
	        parser.setProperty("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	        Reader reader = context.getDocument().createReader();
	        try
	        {
	        	parser.parse(new InputSource(reader), handler);
	        	toAnnotations(context, handler.tagData);
	        }
	        finally
	        {
	        	reader.close();
	        }
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
