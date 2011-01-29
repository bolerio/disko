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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.DU;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.SentenceInterpretation;
import disko.relex.PrologExporter;

import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;

public class PrologRulesProcessor extends AbstractProcessor<AnalysisContext<TextDocument>>
{
	private static Log log = LogFactory.getLog(PrologRulesProcessor.class);
	
	private String prologResource = "";
	private String prologQuery = "";
	private Prolog prolog = null;
	private Theory resourceTheory = null;
	private PrologExporter exporter = new PrologExporter();		
	
	public PrologRulesProcessor()
	{		
	}
	
	public PrologRulesProcessor(String prologResource, String prologQuery)
	{
		this.prologResource = prologResource;
		this.prologQuery = prologQuery;
	}
	
	@SuppressWarnings("unchecked")
    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
	{
		prolog = exporter.makePrologInstance(ctx.getGraph());
		InputPort<SentenceAnn> sentenceInput = ports.getInput(SentenceAnalyzer.SENTENCE_CHANNEL);
		InputPort<SentenceInterpretation> parseInput = ports.getInput(ToRelOccProcessor.SENTENCE_INTERPRETATIONS);		
		for (SentenceAnn sentence = sentenceInput.take(); 
			 !sentenceInput.isEOS(sentence); 
			 sentence = sentenceInput.take())
		{					
			final SentenceInterpretation si = parseInput.take();
			try
			{
				//prolog.getOperatorManager().
				prolog.setTheory(exporter.getPrologTheory(ctx, si.getRelOccs()));
//				prolog.getOperatorManager().opNew("==>", "xfx", 230);
//				prolog.getOperatorManager().opNew("==>", "xfx", 230);
				prolog.getTheoryManager().consult(getResourceTheory(), true, prologResource);				
				for (SolveInfo solveInfo = prolog.solve(prologQuery); solveInfo != null; 
					 solveInfo = prolog.solveNext())
				{
					System.out.println("Solution: " + solveInfo.getSolution());
					for (Var var : (List<Var>)solveInfo.getBindingVars())
						System.out.println("\t\t" + var + "=" + solveInfo.getVarValue(var.getName()));
				}
			}
			catch (NoMoreSolutionException ex) { }
			catch (NoSolutionException ex) { }
			catch (Exception ex)
			{
				log.error("While processing sentence " + sentence.getSentence(), ex);
				ex.printStackTrace();
			}
		}
	}

	public String getPrologResource()
	{
		return prologResource;
	}

	public void setPrologResource(String prologResource)
	{
		this.prologResource = prologResource;
		resourceTheory = null;
	}

	public Theory getResourceTheory()
	{
		if (resourceTheory == null)
		{
			try
			{
				resourceTheory = new Theory(DU.readResource(prologResource));
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
		return resourceTheory;
	}
	public String getPrologQuery()
	{
		return prologQuery;
	}

	public void setPrologQuery(String prologQuery)
	{
		this.prologQuery = prologQuery;
	}	
}
