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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import disko.utils.IntervalTree;

import relex.corpus.TextInterval;

import au.id.jericho.lib.html.Attribute;
import au.id.jericho.lib.html.Attributes;
import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.HTMLElementName;
import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.StartTagType;
import au.id.jericho.lib.html.Tag;
import au.id.jericho.lib.html.TextExtractor;

@SuppressWarnings("unchecked")
public class HTMLDocument extends UrlTextDocument
{
    public static final String PAGE_ANN = "page";
    private static Log log = LogFactory.getLog(HTMLDocument.class);

    public static final Set<String> DEFAULT_TAGS;
    public static final Set<String> DEFAULT_ATTRIBUTES;
    public static final Set<String> DEFAULT_PARAGRAPH_DELIMITING_TAGS;

    private String htmlText = null;
    private boolean ignoreEnclosingParagraphs = true;
    private Set<String> desiredTags = new HashSet<String>(DEFAULT_TAGS);;
    private Set<String> desiredAttributes = new HashSet<String>(DEFAULT_ATTRIBUTES);;
    private Set<String> paragraphDelimitingTags = new HashSet<String>(DEFAULT_PARAGRAPH_DELIMITING_TAGS); 
    
    static
    {
        DEFAULT_TAGS = new HashSet<String>();
        DEFAULT_TAGS.add(HTMLElementName.HTML);
        DEFAULT_TAGS.add(HTMLElementName.HEAD);
        DEFAULT_TAGS.add(HTMLElementName.TITLE);
        DEFAULT_TAGS.add(HTMLElementName.META);
        DEFAULT_TAGS.add(HTMLElementName.BODY);
        DEFAULT_TAGS.add(HTMLElementName.UL);
        DEFAULT_TAGS.add(HTMLElementName.OL);
        DEFAULT_TAGS.add(HTMLElementName.LI);
        DEFAULT_TAGS.add(HTMLElementName.TABLE);
        DEFAULT_TAGS.add(HTMLElementName.TH);
        DEFAULT_TAGS.add(HTMLElementName.TR);
        DEFAULT_TAGS.add(HTMLElementName.TD);
        DEFAULT_TAGS.add(HTMLElementName.DIV);
        DEFAULT_TAGS.add(HTMLElementName.SPAN);
        DEFAULT_TAGS.add(HTMLElementName.P);

        DEFAULT_ATTRIBUTES = new HashSet<String>();
        DEFAULT_ATTRIBUTES.add("class");

        DEFAULT_PARAGRAPH_DELIMITING_TAGS = new HashSet<String>();
        DEFAULT_PARAGRAPH_DELIMITING_TAGS.add(HTMLElementName.P);
        DEFAULT_PARAGRAPH_DELIMITING_TAGS.add(HTMLElementName.LI);
        DEFAULT_PARAGRAPH_DELIMITING_TAGS.add(HTMLElementName.TD);
    }

    private Ann getParagraphAnn(Element element)
    {
        String clean = CharacterReference.decode(new TextExtractor(element).toString()).trim();
        if (clean.length() == 0)
            return null;
        // This is because link parser's inability to deal with those characters
        // yet...
        return new ParagraphAnn(element.getBegin(), 
                                element.getEnd(), 
                                DU.replaceUnicodePunctuation(clean));
    }

    public List<Ann> htmlAnnotate(String htmlText)
    {
        Source source = new Source(htmlText);
        source.setLogger(null);
        List allTags = source.findAllTags();
        ArrayList<Ann> annotations = new ArrayList<Ann>();

        if (allTags.isEmpty()) // this is not HTML text, treat it as a single
                                // paragraph
        {
            ParagraphAnn p = new ParagraphAnn(0, htmlText.length(), htmlText);
            annotations.add(p);
            return annotations;
        }

        IntervalTree<Element> paragraphTree = new IntervalTree<Element>(new TextInterval(0,
                                                                                         htmlText.length()));

        for (Tag tag : (Iterable<Tag>) allTags)
        {
            if (tag.getTagType() != StartTagType.NORMAL || 
                !desiredTags.contains(tag.getName()))
                continue;
            Element element = tag.getElement();
            int begin = element.getBegin();
            int end = element.getEnd();
            MarkupAnn markupAnn = new MarkupAnn(begin, end, tag.getName());
            Attributes attributes = element.getAttributes();
            for (Iterator it = attributes.iterator(); it.hasNext();)
            {
                Attribute attribute = (Attribute) it.next();
                String name = attribute.getName().toLowerCase();
                String value = attribute.getValue();
                if (desiredAttributes.contains(name))
                {
                    log.debug("Found attribute " + name + ":" + value);
                    markupAnn.getAttributes().put(name, value);
                }
            }

            annotations.add(markupAnn);

            if (paragraphDelimitingTags.contains(tag.getName()))
                if (ignoreEnclosingParagraphs)
                    paragraphTree.add(new TextInterval(begin, end), element);
                else
                {
                    Ann a = getParagraphAnn(element);
                    if (a != null)
                        annotations.add(a);
                }
        }

        if (ignoreEnclosingParagraphs)
        {
            Set<TextInterval> ignore = new HashSet<TextInterval>();
            for (Iterator<TextInterval> ti = paragraphTree.leafs(); ti.hasNext();)
            {
                TextInterval current = ti.next();
                if (ignore.contains(current))
                    continue;
                else
                    ignore.add(current);
                List<Element> elements = paragraphTree.get(current);
                if (elements.size() == 1)
                {
                    Ann a = getParagraphAnn(elements.get(0));
                    if (a != null)
                        annotations.add(a);
                }
                else if (elements.isEmpty())
                    continue;
                else
                    log.error("Paragraph overlap at " + current
                              + " in document " + getUrl());
            }
        }
        return annotations;
    }

    public synchronized String load()
    {
        String rawText = htmlText == null ? super.load() : htmlText;
        annotations.addAll(htmlAnnotate(rawText));
        for (Ann a : annotations)
            if (a instanceof MarkupAnn
                && ((MarkupAnn) a).getTag().equals(HTMLElementName.TITLE))
                setTitle(rawText.substring(a.getInterval().getStart(),
                                           a.getInterval().getEnd()).trim());
        return (fullText = new WeakReference<String>(rawText)).get();
    }

    /**
     * 
     * <p>
     * Extract all plain text from this document's body.
     * </p>
     * 
     * @return
     */
    public String getPlainText()
    {
        Source source = new Source(this.getFullText());
        source.setLogger(null);
        List<Tag> allTags = source.findAllTags();
        for (Tag tag : allTags)
        {
            if (tag.getTagType() != StartTagType.NORMAL)
            {
                continue;
            }
            if (tag.getName() == HTMLElementName.TITLE)
                setTitle(tag.getElement().getTextExtractor().toString());
            if (tag.getName() == HTMLElementName.BODY)
                return tag.getElement().getTextExtractor().toString();
        }
        return source.getTextExtractor().toString();
    }

    public HTMLDocument()
    {
    }

    public HTMLDocument(String htmlText)
    {
        this.htmlText = htmlText;
    }

    public HTMLDocument(URL url)
    {
        super(url);
    }

    public HTMLDocument(File f)
    {
        super(f);
    }

    public Set<String> getDesiredTags()
    {
        return desiredTags;
    }
    
    public Set<String> getDesiredAttributes()
    {
        return desiredAttributes;
    }
    
    public Set<String> getParagraphDelimitingTags()
    {
        return paragraphDelimitingTags;
    }
}
