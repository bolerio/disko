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
package disko.flow.analyzers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.dom.DocumentWrapper;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import disko.AnalysisContext;
import disko.ParagraphAnn;
import disko.TextDocument;


public class XQueryAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
    private static Log log = LogFactory.getLog(XQueryAnalyzer.class);

    public static final String DEFAULT_XQUERIES_HOME = "data/xquery";

    private transient TreeMap<String, XQueryExecutable> xqueries;
    private transient Processor xqProcessor;
    private transient XQueryCompiler xqCompiler;

    public XQueryAnalyzer()
    {
        addDefaultXQueries();
    }

    /**
     * Adds all the queries found in the directory defined by system property
     * xqueries.home, or if it's not found, the default location
     * DEFAULT_XQUERIES_HOME
     */
    private void addDefaultXQueries()
    {
        String xqueriesHomeProperty = System.getProperty("xqueries.home");
        if (xqueriesHomeProperty == null)
            xqueriesHomeProperty = DEFAULT_XQUERIES_HOME;
        File xqueriesHome = new File(xqueriesHomeProperty);

        xqProcessor = new Processor(false);
        xqCompiler = xqProcessor.newXQueryCompiler();
        // Doesn't work; seems to be a bug
        // xqCompiler.declareNamespace("", "http://www.w3.org/1999/xhtml"); //
        // default namespace
        // declare default element namespace "http://www.w3.org/1999/xhtml";

        // Works; it can be used to simplify the xqueries
        // declare namespace
        // java="java:org.disco.flow.analyzers.XQueryAnalyzer";
        // xqCompiler.declareNamespace("java",
        // "java:org.disco.flow.analyzers.XQueryAnalyzer");

        xqueries = new TreeMap<String, XQueryExecutable>();
        final File[] xqFiles = xqueriesHome.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".xq");
            }
        });
        for (File xqueryFile : xqFiles)
        {
            try
            {
                xqueries.put(xqueryFile.getName(),
                             xqCompiler.compile(xqueryFile));
                log.debug("Loaded " + xqueryFile.getCanonicalPath());
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
            catch (SaxonApiException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void addXQuery(String name, String xquery) throws SaxonApiException
    {
        xqueries.put(name, xqCompiler.compile(xquery));
    }

    public TreeMap<String, XQueryExecutable> getXqueries()
    {
        return xqueries;
    }

    public void process(AnalysisContext<TextDocument> context, Ports ports) throws InterruptedException
    {
        log.debug("XQuery Analyzer started");

        try
        {
            if (ports.getInputCount() == 0)
            {
                log.debug("RegexpAnalyzer doesn't have input ports, reading document from context");
                String text = context.getDocument().getFullText();
                processText(context, ports, text, 0);
            }
            else
            {
                InputPort<ParagraphAnn> paragraphInput = ports.getInput(ParagraphAnalyzer.PARAGRAPH_CHANNEL);
                if (paragraphInput != null)
                {
                    log.debug("RegexpAnalyzer reading from PARAGRAPH_CHANNEL");
                    for (ParagraphAnn paragraph = paragraphInput.take(); !paragraphInput.isEOS(paragraph); paragraph = paragraphInput.take())
                    {
                        processText(context, ports, paragraph.getParagraph(),
                                    paragraph.getInterval().getStart());
                    }
                }
                else
                {
                    log.debug("RegexpAnalyzer reading from TEXT_CHANNEL");
                    InputPort<String> textInput = ports.getInput(SentenceAnalyzer.TEXT_CHANNEL);
                    String text = textInput.take();
                    processText(context, ports, text, 0);
                }
            }
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
        }

        log.debug("XQuery Analyzer ended");
    }

    private void processText(AnalysisContext<TextDocument> context,
                             Ports ports, String docText, int offset)
                                                                     throws UnsupportedEncodingException,
                                                                     SaxonApiException
    {
        XdmNode xmlContext = getXMLContext(docText);

        for (Map.Entry<String, XQueryExecutable> entry : xqueries.entrySet())
        {
            String name = entry.getKey();
            XQueryExecutable exp = entry.getValue();

            log.debug("\nRunning " + name);
            XQueryEvaluator qe = exp.load();
            qe.setContextItem(xmlContext);

            // TODO save the results as relations in HGDB
            StringWriter outputWriter = new StringWriter();
            Serializer out = new Serializer();
            out.setOutputProperty(Serializer.Property.INDENT, "yes");
            out.setOutputWriter(outputWriter);
            qe.run(out);
            log.info(outputWriter.toString());
        }
    }

    /**
     * Transforms the raw HTML source into a XDM node to be processed by XQuery.
     * This method will call parseDOM to clean ill-formed HTML and transform it
     * into XHTML compatible with XQuery.
     * 
     * @param docText
     *            The raw HTML source
     * @return The XdmNode corresponding to the given HTML document
     */
    private XdmNode getXMLContext(String docText)
    {
        XdmNode xmlContext = null;
        try
        {
            System.setProperty("javax.xml.transform.TransformerFactory",
                               "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory tfactory = TransformerFactory.newInstance();
            Configuration config = ((TransformerFactoryImpl) tfactory).getConfiguration();
            DocumentWrapper source = new DocumentWrapper(parseDOM(docText), "",
                                                         config);
            xmlContext = xqProcessor.newDocumentBuilder().build(source);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return xmlContext;
    }

    /**
     * Uses JTidy to parse probably ill-formed HTML and generate a valid DOM,
     * which is converted to XHTML to be processed by the XQueries.
     * 
     * Jericho seems to be even more tolerant with HTML, but it's not as easy to
     * integrate with Saxon (the XQuery library) as JTidy. Furthermore, it seems
     * that JTidy is the de facto standard for this task.
     * 
     * @param docText
     *            The HTML source
     * @return a Document Object Model of the source
     * @throws UnsupportedEncodingException
     */
    public Document parseDOM(String docText)
                                            throws UnsupportedEncodingException
    {
        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        Document tidyDOM = null;
        tidyDOM = tidy.parseDOM(
                                new ByteArrayInputStream(
                                                         docText.getBytes("UTF-8")),
                                null);
        return tidyDOM;
    }

    // TODO implement this (called from XQuery)
    public static boolean isNamedEntity(String text)
    {
        return true;
    }

    // TODO implement this (called from XQuery)
    public static final Pattern DEFAULT_ADDRESS_PATTERN = Pattern.compile(
          "((PO|P\\.O|P\\.O\\.|po|p\\.o|p\\.o\\.)? *(box|BOX|Box)|[0-9]+){0,1}"
                  + "[0-9A-Za-z\\-\\. #,]{3,50}[, \\*]+"
                  + "(A[LKSZRAP]|C[AOT]|D[EC]|F[LM]|G[AU]|HI|I[ADLN]|"
                  + "K[SY]|LA|M[ADEHINOPST]|N[CDEHJMVY]|O[HKR]|P[ARW]|"
                  + "RI|S[CD]|T[NX]|UT|V[AIT]|W[AIVY]) "
                  + "[0-9]{5}(-?[0-9]{4})?"
          /*
             * +"[
             * ,\\.$\\r]"
             */,
          Pattern.MULTILINE);

    public static boolean isAddress(String text)
    {
        Matcher matcher = DEFAULT_ADDRESS_PATTERN.matcher(text);
        final boolean matches = matcher.find();
        log.debug("isAddress(\"" + text + "\") = " + matches);
        return matches;
    }

    // TODO implement this (called from XQuery)
    public static boolean isPhone(String text)
    {
        final boolean matches = text.matches(".*\\({0,1}[0-9]{1,3}[\\)-]{0,1} {0,1}[0-9]{1,3}-[0-9A-Z]{1,4}.*");
        // log.debug("isPhone(\""+text+"\") = "+matches);
        return matches;
    }
}
