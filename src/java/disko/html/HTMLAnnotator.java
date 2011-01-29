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

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;

import disko.AnalysisContext;
import disko.Danalyzer;
import disko.DefaultTextDocument;
import disko.MarkupAnn;
import disko.ParagraphAnn;
import disko.StringTextDocument;
import disko.TextDocument;
import disko.data.relex.SyntacticPredicate;

import au.id.jericho.lib.html.Attribute;
import au.id.jericho.lib.html.Attributes;
import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.HTMLElementName;
import au.id.jericho.lib.html.HTMLElements;
import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.StartTagType;
import au.id.jericho.lib.html.Tag;
import au.id.jericho.lib.html.TextExtractor;

public class HTMLAnnotator implements Danalyzer<AnalysisContext<TextDocument>>
{
	public static final int DEBUG = 0;
	private HashSet<String> tagged;
	private HashSet<String> desiredAttributes;
	
	public void initialize()
	{
		tagged = new HashSet<String>();
		tagged.add(HTMLElementName.HTML);
		tagged.add(HTMLElementName.HEAD);
		tagged.add(HTMLElementName.META);
		tagged.add(HTMLElementName.BODY);
		tagged.add(HTMLElementName.UL);
		tagged.add(HTMLElementName.OL);
		tagged.add(HTMLElementName.LI);
		tagged.add(HTMLElementName.TABLE);
		tagged.add(HTMLElementName.TH);
		tagged.add(HTMLElementName.TR);
		tagged.add(HTMLElementName.TD);
		tagged.add(HTMLElementName.DIV);
		tagged.add(HTMLElementName.SPAN);
		tagged.add(HTMLElementName.P);
		
		desiredAttributes = new HashSet<String>();
		desiredAttributes.add("class");
	}

	@SuppressWarnings("unchecked")
	public void process(AnalysisContext<TextDocument> context)
	{
		String docText = context.getDocument().getFullText();
		Source source=new Source(docText);
		source.setLogger(null);
		for (Tag tag: (Iterable<Tag>) source.findAllTags()){
			if (tag.getTagType() != StartTagType.NORMAL) {
				continue;
			}
			if (tag.getName()==HTMLElementName.SCRIPT || 
				tag.getName()==HTMLElementName.STYLE || 
				!HTMLElements.getElementNames().contains(tag.getName()) ||				
				!tagged.contains(tag.getName()) ) {
				continue;
			}
			Element element = tag.getElement();
			int begin = element.getBegin();
			int end = element.getEnd();
			MarkupAnn markupAnn = new MarkupAnn(begin, end, tag.getName());
			Attributes attributes = element.getAttributes();
			for (Iterator it = attributes.iterator(); it.hasNext();){
				Attribute attribute = (Attribute) it.next();
				String name = attribute.getName().toLowerCase();
				String value = attribute.getValue();
				if (desiredAttributes.contains(name)){
					if (DEBUG > 0) System.out.println("Found attribute "+name+":"+value);
					markupAnn.getAttributes().put(name, value);
				}
			}
			
			context.add(markupAnn);
			if ( tag.getName().equals(HTMLElementName.P) ||
				 tag.getName().equals(HTMLElementName.LI) ){
				String clean = CharacterReference.decode(new TextExtractor(element).toString()).trim();
				if (clean.length() == 0) continue;
				ParagraphAnn p = new ParagraphAnn(begin, end, clean);
				context.add(p);
				if (DEBUG > 0) System.out.println(tag.getName()+" "+begin+","+end+": "+clean);
			} else {
				if (DEBUG > 0) System.out.println(tag.getName()+" "+begin+","+end);
			}
		}
		
	}

	public static AnalysisContext<TextDocument> initAnalysisContext(
			String[] args) {
		if (args.length==0){
			System.err.println("Usage:\njava org.disco.html.HTMLAnnotator <text file|sentence>");
			System.exit(1);
		}
		TextDocument doc = null;
		File file = new File(args[0]);
		if (file.exists()){
			doc = new DefaultTextDocument(file);
		} else {
			doc = new StringTextDocument(args[0]);
		}
		String home = ".";
		String hgdbURL = "../wordnet_hgdb";
		
		if (!home.endsWith("/")) home += "/";
		String gateHome = System.getProperty("gate.home"); 
		if (gateHome == null || gateHome.length() == 0)			
			gateHome = System.getenv().get("GATE_HOME");		
		if (gateHome == null || gateHome.length() == 0)
			gateHome = "/opt/GATE-4.0";
		System.setProperty("gate.home", gateHome);
		System.setProperty("wordnet.configfile", home+"data/wordnet/file_properties-linux.xml");
		System.setProperty("EnglishModelFilename", home+"data/sentence-detector/EnglishSD.bin.gz");
		System.setProperty("relex.algpath", home+"data/relex-semantic-algs.txt");
		System.setProperty("relex.linkparserpath", home+"/data/linkparser");
		HyperGraph graph = null;
		if (graph == null)
		{
			graph = HGEnvironment.get(hgdbURL);
			SyntacticPredicate.loadAll(graph);
		}
		HGHandle docHandle = graph.add(doc);
		AnalysisContext<TextDocument> ctx = new AnalysisContext<TextDocument>(graph, doc);
		ctx.pushScoping(docHandle);
		return ctx;
	}
	
	
	public static void main(String[] args)
	{
		AnalysisContext<TextDocument> ctx = initAnalysisContext(args);
		
		HTMLAnnotator htmlAnnotator = new HTMLAnnotator();
		htmlAnnotator.initialize();
		htmlAnnotator.process(ctx);
		
		ctx.getGraph().close();
	}
}
