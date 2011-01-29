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

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.SynsetLink;

import disko.AnalysisContext;
import disko.DU;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SemRel;

public class HtmlSenseViewer
{
	private String getScript()
	{
		try
		{
			return "<DIV id=tipDiv style=\"POSITION: absolute; VISIBILITY: hidden; Z-INDEX: 100\"></DIV> " +
				"<script language='JavaScript'>" +			
				DU.readCharacterStream(this.getClass().getResourceAsStream("/org/disco/utils/tooltip.js")) +
			    "</script>";
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}
		
	private String getSenseAnnotation(AnalysisContext<TextDocument> ctx, HGHandle synsetHandle, HGHandle wordHandle)
	{
		StringBuffer result = new StringBuffer();
		SynsetLink s = ctx.getGraph().get(synsetHandle);
		result.append("<p><b>" + s.getGloss() + "</b></p>");
		List<HGHandle> others = hg.findAll(ctx.getGraph(), hg.and(hg.type(s.getClass()), hg.incident(wordHandle)));
		if (others.size() == 1)
			result.append("<p>This is a monosemous word.</p>");
		else
		{
			result.append("<p><b>Alternatives:</b><ul>");
			for (HGHandle other : others)
			{
				if (other.equals(synsetHandle)) continue;
				SynsetLink os = ctx.getGraph().get(other); 
				result.append("<li>" + os.getGloss() + "</li>");
			}
			result.append("</ul></p>");			
		}
		return result.toString().replaceAll("'", "\\\\'").replaceAll("\"", "");
	}
	
	private String formatLinkStart(String text)
	{
		return "<a href=\"#\" onmouseover = \"showtip(event,'" + text +"')\"" + "onmouseout='hidetip()'>";
	}
	
	private int findEndPosition(String text, int start)
	{
		int result = start;
		while (result < text.length())
		{
			char c = text.charAt(result);
			if ( (c >= 'a' && c <= 'z') ||
			     (c >= 'A' && c <= 'Z') ||
			     c == '_' || c == '-') // assume that a dash used as punctuation is preceeded with space
				result++;
			else
				break;
		}
		return result;
	}
	
	public String getSenseAnnotatedHtml(AnalysisContext<TextDocument> ctx)
	{		
		TreeMap<Integer, String> inserts = new TreeMap<Integer, String>();
		String docText = ctx.getDocument().getFullText();		
		Set<RelOccurrence> occs = ctx.find(RelOccurrence.class);
		for (RelOccurrence o : occs)
		{
			SemRel semRel = o.getRelation();
			if (semRel == null)
				continue;
			for (int i = 1; i < o.getArity(); i++)
			{
				String senseAnnotation = getSenseAnnotation(ctx, semRel.getTargetAt(i), o.getTargetAt(i));
				if (senseAnnotation != null)
				{
					inserts.put(o.getPosition(i-1), formatLinkStart(senseAnnotation));
					int endpos = findEndPosition(docText, o.getPosition(i-1));
					inserts.put(endpos, "</a>");
				}
			}
		}
		inserts.put(docText.length(), getScript());
		return DU.insertStuff(docText, inserts);
	}
}
