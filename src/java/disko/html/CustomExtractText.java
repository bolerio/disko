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
package disko.html;
import java.net.URL;
import java.util.HashSet;

// import au.id.jericho.lib.html.Attribute;
// import au.id.jericho.lib.html.Attributes;
import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.HTMLElementName;
import au.id.jericho.lib.html.HTMLElements;
import au.id.jericho.lib.html.Segment;
import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.StartTag;
import au.id.jericho.lib.html.StartTagType;
import au.id.jericho.lib.html.Tag;
// import au.id.jericho.lib.html.TextExtractor;

public class CustomExtractText {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		String sourceUrlString="test/jericho-html-test.html";
		if (args.length==0)
		  System.err.println("Using default argument of \""+sourceUrlString+'"');
		else
			sourceUrlString=args[0];
		if (sourceUrlString.indexOf(':')==-1) sourceUrlString="file:"+sourceUrlString;
		Source source=new Source(new URL(sourceUrlString));
		source.setLogger(null);

		source.fullSequentialParse();
		// html, head, meta, body, p, ul, ol, li, table, th, tr, td
		final HashSet<String> accepted = new HashSet<String>();
		accepted.add(HTMLElementName.HTML);
		accepted.add(HTMLElementName.HEAD);
		accepted.add(HTMLElementName.META);
		accepted.add(HTMLElementName.BODY);
		accepted.add(HTMLElementName.P);
		accepted.add(HTMLElementName.UL);
		accepted.add(HTMLElementName.OL);
		accepted.add(HTMLElementName.LI);
		accepted.add(HTMLElementName.TABLE);
		accepted.add(HTMLElementName.TH);
		accepted.add(HTMLElementName.TR);
		accepted.add(HTMLElementName.TD);
		accepted.add(HTMLElementName.DIV);
		accepted.add(HTMLElementName.SPAN);
		accepted.add(HTMLElementName.BR);
		
		final boolean excludeNonHTMLElements = true;
		// final boolean includeAttributes = false;
		
		Segment segment = source;
		final StringBuffer sb = new StringBuffer(segment .length());
		int textBegin=segment.getBegin();
		for (Tag tag: (Iterable<Tag>) segment.findAllTags()){
			final int textEnd=tag.getBegin();
			if (textEnd < textBegin) continue;
			
			while (textBegin<textEnd) sb.append(source.charAt(textBegin++));
			
			if (tag.getTagType()==StartTagType.NORMAL) {
				final StartTag startTag=(StartTag)tag;
				
				if (tag.getName()==HTMLElementName.SCRIPT || 
					tag.getName()==HTMLElementName.STYLE || 
					excludeElement(startTag) || 
					(excludeNonHTMLElements && 
							!HTMLElements.getElementNames().contains(tag.getName())
					)
				) {
					textBegin=startTag.getElement().getEnd();
					continue;
				}
			}
			// Treat both start and end tags not belonging to inline-level elements as whitespace:
			if (tag.getName()==HTMLElementName.BR ||
				!HTMLElements.getInlineLevelElementNames().contains(tag.getName())) sb.append('\n');
			textBegin=tag.getEnd();
		}
		while (textBegin<segment.getEnd()) sb.append(source.charAt(textBegin++));
		
//		final String decodedText=CharacterReference.decodeCollapseWhiteSpace(sb,convertNonBreakingSpaces);
		final String decodedText=CharacterReference.decode(sb);
//		System.out.println(decodedText.replaceAll(" +", " ").replaceAll("\n+", "\n"));
		System.out.println(decodedText);
  }

	private static boolean excludeElement(StartTag startTag) {
		final HashSet<String> accepted = new HashSet<String>();
		accepted.add(HTMLElementName.HTML);
		accepted.add(HTMLElementName.HEAD);
		accepted.add(HTMLElementName.META);
		accepted.add(HTMLElementName.BODY);
		accepted.add(HTMLElementName.P);
		accepted.add(HTMLElementName.UL);
		accepted.add(HTMLElementName.OL);
		accepted.add(HTMLElementName.LI);
		accepted.add(HTMLElementName.TABLE);
		accepted.add(HTMLElementName.TH);
		accepted.add(HTMLElementName.TR);
		accepted.add(HTMLElementName.TD);
		accepted.add(HTMLElementName.DIV);
		accepted.add(HTMLElementName.SPAN);
		accepted.add(HTMLElementName.BR);
		return !accepted.contains(startTag.getName());
	}

}
