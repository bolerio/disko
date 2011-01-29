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

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.wordnet.data.SynsetLink;
import org.hypergraphdb.app.wordnet.ext.SynsetInfo;
import org.hypergraphdb.util.HGUtils;
import org.hypergraphdb.util.Pair;

import disko.DU;
import disko.relex.OpenNLPEntityMaintainerFactory;

import relex.RelationExtractor;
import relex.Sentence;
import relex.feature.FeatureForeach;
import relex.feature.FeatureNode;
import relex.feature.RelationCallback;

/**
 * 
 * <p>
 * A utility run on a freshly created WordNet database to create an "extended WordNet" 
 * version by using Relex. At the moment, only POS tagging is applied to synset glosses
 * and logical form is not being infered yet. The resulting info is stored as a 
 * <code>org.hypergraphdb.app.wordnet.ext.SynsetInfo</code> bean attached to each
 * synset link that was successfully processed.
 * 
 * </p>
 *
 * <p>This can be run standalone. The 'main' method takes a Java properties file that
 * looks like this:
 * 
 * <pre><code>
 * hgdb.location=c:/tmp/graphs/wordnet
 * gate.home=c:/tools/gate
 * relex.morphy.Morphy=org.disco.relex.MorphyHGDB
 * relex.algpath=d:/work/disco/trunk/data/relex-semantic-algs.txt
 * relex.parser.LinkParser.pathname=D:/work/link-grammar-4.3.4/data
 * EnglishModelFilename=d:/work/disco/trunk/data/sentence-detector/EnglishSD.bin.gz
 * opennlp.model.namedentity=D:/work/disco/trunk/data/namedentity
 * </code></pre>
 * </p>
 *  
 * @author Borislav Iordanov
 *
 */
public class XWordNet
{
	private HyperGraph graph = null;
	private OpenNLPEntityMaintainerFactory emFactory = new OpenNLPEntityMaintainerFactory();	
	private RelationExtractor re = null;
	private PrintStream log = null;
	
	class GlossRelationProcessor implements RelationCallback
	{        
	    String phrase;
	    int offset;
	    Map<Integer, Pair<FeatureNode, String>> inserts = new TreeMap<Integer, Pair<FeatureNode, String>>();

	    private int getStartPos(FeatureNode node)
	    {
	        int p = Integer.parseInt(node.get("start_char").getValue());
	        return p - offset;
	    }

	    private int getEndPos(FeatureNode node)
	    {
	        int p = Integer.parseInt(node.get("end_char").getValue());
	        return p - offset;
	    }

	    private String getName(FeatureNode fn)
	    {
	        if (fn == null) return null;
	        fn = fn.get("ref");
	        if (fn == null) return null;
	        fn = fn.get("name");
	        if (fn == null) return null;
	        return fn.getValue();
	    }
	    
	    public GlossRelationProcessor(String phrase, int offset)
	    {
	        this.phrase = phrase;
	        this.offset = offset;
	    }

	    public String getPhrase() 
	    { 
	        int last = 0;
	        StringBuffer result = new StringBuffer();
	        for (Map.Entry<Integer, Pair<FeatureNode, String>> e : inserts.entrySet())
	        {
	            int endpos = e.getKey();
	            FeatureNode node = e.getValue().getFirst();
	            int startpos = getStartPos(node);
	            String tag = e.getValue().getSecond();
	            result.append(phrase.substring(last, startpos));
	            String morphed = getName(node);
	            if (morphed == null)
	                result.append(phrase.substring(startpos, endpos));
	            else
	                result.append(morphed);
	            result.append(tag);
	            last = endpos;
	        }
	        result.append(phrase.substring(last));
	        return result.toString(); 
	    }

	    public Boolean BinaryHeadCB(FeatureNode node)
	    {
	        return false;
	    }

	    public Boolean BinaryRelationCB(String linkName, FeatureNode srcNode, FeatureNode tgtNode)
	    {
	        return false;
	    }

	    public Boolean UnaryRelationCB(FeatureNode srcNode, String attrName)
	    {
	        // print("processing attribute with name: " + attrName);
	        if (!"pos".equals(attrName))
	            return false;

	        FeatureNode attr = srcNode.get(attrName);
	        if (!attr.isValued()) return false;

	        String value = attr.getValue();
//	        print("pos(" + srcNode.get("nameSource").get("ref").get("name") + ")=" + value);
//	        print("node pos = " + Integer.parseInt(srcNode.get("nameSource").get("end_char").getValue()));

	        FeatureNode nameSrc = srcNode.get("nameSource");
	        int pos = getEndPos(srcNode.get("nameSource"));
	        if (pos <= 0)
	            return false;
	        if ("noun".equals(value))
	            inserts.put(pos, new Pair<FeatureNode, String>(nameSrc, "#n"));
	        else if ("verb".equals(value))
	            inserts.put(pos, new Pair<FeatureNode, String>(nameSrc, "#v"));
	        else if ("adj".equals(value))
	            inserts.put(pos, new Pair<FeatureNode, String>(nameSrc, "#a"));
	        else if ("adv".equals(value))
	            inserts.put(pos, new Pair<FeatureNode, String>(nameSrc, "#d"));
	        return false;
	    }
	}
	
	private String annotatePart(String part)
	{
		if (log != null)
			log.println("processing '" + part + "'");	     
	    // remove everything between parenthesis since we don't know 
	    // what to do with it at this point.
	    part = part.replaceAll("\\([^\\(\\)]*\\)", ""); 
	    if (part.trim().length() == 0)
	        return "";
	    GlossRelationProcessor visitor = null;
	    try
	    {
	        Sentence rInfo = re.processSentence("It means " + part, 
	        						emFactory.makeEntityMaintainer("It means " + part));
	        if (rInfo.getParses() == null || rInfo.getParses().size() == 0)
	            throw new RuntimeException("Failed to parse part: " + part);
	        visitor = new GlossRelationProcessor(part, "It means ".length());
	        FeatureForeach.foreach(rInfo.getParses().get(0).getLeft(), visitor);
	    }
	    finally
	    {
	        re.clear();
	    }
	    return visitor.getPhrase();
	}

	private SynsetInfo getSynsetInfo(HGHandle synsetHandle)
	{
	    SynsetInfo info = null;
	    HGHandle infoHandle = hg.findOne(graph, hg.and(hg.type(SynsetInfo.class), 
	                                                   hg.incident(synsetHandle)));
	    if (infoHandle != null)
	        info = graph.get(infoHandle);
	    else
	    {
	        info = new SynsetInfo(synsetHandle);
	        graph.add(info);
	    }
	    return info;
	}

	private void annotateSynset(HGHandle synsetHandle)
	{
	    SynsetLink synset = graph.get(synsetHandle);
	    String gloss = synset.getGloss();
	    log.println("Processing gloss: " + gloss);
	    String extgloss = "";
	    int start_pos = 0;
	    int end_pos = gloss.indexOf(';');
	    if (end_pos == -1)
	        end_pos = gloss.length();
	    while (true)
	    {
	        String part = gloss.substring(start_pos, end_pos);
	        part = part.trim();
	        boolean isexample = part.charAt(0) == '"';
	        if (isexample)
	            part = part.substring(1, part.length() - 1);
	        extgloss += (isexample ? "\"" : "") + annotatePart(part) + (isexample ? "\"" : "");
	        start_pos = end_pos + 1;
	        end_pos = gloss.indexOf(';', start_pos);
	        if (end_pos > -1)
	            extgloss += ';';
	        else
	            break;    
	    }
	    SynsetInfo info = getSynsetInfo(synsetHandle);
	    info.getAttributes().put("taggedGloss", extgloss);
	    graph.update(info);    
	    log.println("Stored tagged gloss as: " + extgloss);
	}
	
	public void processAllGlosses(boolean update)
	{	    
	    List<HGHandle> L = hg.findAll(graph, hg.typePlus(SynsetLink.class));
        for (HGHandle current : L)
        {
            if (update || HGQuery.hg.findOne(graph, hg.and(hg.type(SynsetInfo.class), 
                                                 		   hg.incident(current))) == null)
            {
                try { annotateSynset(current); }
                catch (Throwable t) { log.println("Failed on " + current); t.printStackTrace(log); }
            }
        }
	}
	
	public static void main(String [] argv)
	{
		if (argv.length < 1)
		{
			System.out.println("Usage: java XWordNet properties_file");
			System.exit(-1);
		}
		String propsFilename = argv[0];
		System.out.println("Using properties file " + propsFilename);
		try
		{
			DU.loadSystemProperties(propsFilename);
			String hgLocation = System.getProperty("hgdb.location");
			if (HGUtils.isEmpty(hgLocation))
				throw new Exception("No HGDB location specified.");
			else
				System.out.println("Using HGDB: " + hgLocation);			
			System.setProperty("morphy.hgdb.location", hgLocation);
			XWordNet xwordnet = new XWordNet();			
			xwordnet.graph = HGEnvironment.get(hgLocation);
			xwordnet.log = System.out;
			xwordnet.re = new RelationExtractor(true);
			xwordnet.re.setAllowSkippedWords(true);
			xwordnet.processAllGlosses(false);
			System.exit(0);
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			System.exit(-1);
		}
	}
}
