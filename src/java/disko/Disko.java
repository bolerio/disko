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
import java.util.List;
import java.util.concurrent.Future;
import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.management.HGManagement;
import disko.flow.networks.IENetworkCoarse;

public class Disko
{

    /**
     * The default GATE installation directory. It's not necessary to change
     * this value; if you put GATE somewhere else define it as a Java system
     * property (e.g. java -Dgate.home=c:/tools/gate) or in a environment
     * variable (e.g. GATE_HOME="c:/Program Files/GATE-4.0").
     */
    // public static String DEFAULT_GATE_HOME = "/opt/GATE-4.0";
    public static String DEFAULT_HGDB = "../wordnet_hgdb";
    public static String DEFAULT_HOME = ".";

    public static String DEFAULT_GATE_HOME = "c:/tools/gate";

    public String home = null;
    public String hgdbURL = DEFAULT_HGDB;

    private HyperGraph graph;

    public static final boolean DETECT_SENTENCES = true;

    public Disko()
    {
    }

    public Disko(String home, String hgdbURL)
    {
        this.home = home;
        this.hgdbURL = hgdbURL;
    }

    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.err.println("Usage:\n disko.Disko <config file> <text file|sentence>");
            System.err.println("Got " + args.length + " arguments, 3d is " + args[2]);
            System.exit(1);
        }
        
        Disko disko = new Disko();
        disko.init(args[0]);
        
        TextDocument doc = null;
        File file = new File(args[1]);
        if (file.exists())
        {
            doc = new DefaultTextDocument(file);
        }
        else
        {
            doc = new StringTextDocument(args[1]);
        }

        AnalysisContext<TextDocument> ctx = disko.pushDocument(doc);
        disko.process(ctx);
        disko.printScope(disko.graph.getHandle(doc), "");
        disko.destroy();
    }

    public void printScope(HGHandle scope, String indent)
    {
        System.out.println(indent + graph.get(scope));
        List<HGHandle> rels = hg.findAll(graph, hg.apply(hg.targetAt(graph, 1), 
                                                hg.and(hg.type(ScopeLink.class),
                                                       hg.orderedLink(scope, hg.anyHandle()))));
        for (HGHandle h: rels)
            printScope(h, indent + "    ");
    }
    
    public void init(String configFile)
    {
        DU.loadSystemProperties(configFile);
        String gateHome = System.getProperty("gate.home");
        if (gateHome == null || gateHome.length() == 0)
            gateHome = System.getenv().get("GATE_HOME");
        if (gateHome == null || gateHome.length() == 0)
            gateHome = DEFAULT_GATE_HOME;

        String hgdbLocation = System.getProperty("hgdb.location");
        if (hgdbLocation != null && hgdbLocation.length() > 0)
            hgdbURL = hgdbLocation;

        if (home == null)
        {
            home = System.getProperty("disko.home");
            if (home == null || home.length() == 0)
                home = DEFAULT_HOME;
        }
        if (!home.endsWith("/"))
            home += "/";

        System.setProperty("gate.home", gateHome);
        System.setProperty("EnglishModelFilename", home
                + "data/sentence-detector/EnglishSD.bin.gz");

        if (System.getProperty("relex.parser.LinkParser.pathname") != null)
            relex.RelexProperties.setProperty("relex.parser.LinkParser.pathname",
                                              System.getProperty("relex.parser.LinkParser.pathname"));

        initHGDB();

    }

    private void initHGDB()
    {
        // deleteDirectory(new File(hgdbURL));
        if (graph == null)
        {
            graph = HGEnvironment.get(hgdbURL);
            HGManagement.ensureInstalled(graph, new DISKOApplication());
        }
    }

    public void destroy()
    {
        if (graph != null)
            graph.close();
    }

    public AnalysisContext<TextDocument> pushDocument(TextDocument doc)
    {
        HGHandle docHandle = graph.add(doc);
        AnalysisContext<TextDocument> ctx = new AnalysisContext<TextDocument>(
                graph, doc);
        ctx.pushScoping(docHandle);
        return ctx;
    }

    public AnalysisContext<TextDocument> pushDocument(HGHandle topScope,
                                                      TextDocument doc)
    {
        AnalysisContext<TextDocument> ctx = new AnalysisContext<TextDocument>(
                graph, doc);
        ctx.pushScoping(topScope);
        return ctx;
    }

    public void process(AnalysisContext<TextDocument> ctx)
    {
        IENetworkCoarse net = new IENetworkCoarse();
        net.create();
        net.setDaemon(true);
        Future<Boolean> f = (Future<Boolean>)net.start(ctx);
        try
        {
            System.out.println("Processing completed: " + f.get());
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
        // Detect sentences
//        new Danalyzer<AnalysisContext<TextDocument>>() {
//            public void process(AnalysisContext<TextDocument> context)
//            {
//                String docText = context.getDocument().getFullText();
//                System.out.println("Original sentence: " + docText);
//                SentenceAnn ann = new SentenceAnn(0, docText.length(), docText);
//                context.add(ann);
//            }
//        }.process(ctx);
    }

    public static boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    deleteDirectory(files[i]);
                }
                else
                {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }
}
