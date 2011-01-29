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

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.TextDocument;
import disko.data.relex.RelOccurrence;
import disko.data.relex.SentenceInterpretation;
import relex.entity.EntityMaintainer;

public class ConsoleOutputAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{

    private static Log log = LogFactory.getLog(ConsoleOutputAnalyzer.class);

    public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
    {
        log.debug("ConsoleOutputAnalyzer starts");
        HashSet<InputPort<?>> closedPorts = new HashSet<InputPort<?>>();

        while (closedPorts.size() < ports.getInputCount())
        {
            for (InputPort<?> inputPort : ports.getInputPorts())
            {
                if (closedPorts.contains(inputPort))
                    continue;
                Object data = inputPort.take();
                if (inputPort.isEOS(data))
                {
                    closedPorts.add(inputPort);
                    continue;
                }
                if (data instanceof EntityMaintainer)
                {
                    EntityMaintainer em = (EntityMaintainer) data;
                    System.out.println(inputPort.getChannel() + ": '"
                                       + em.getConvertedSentence() + "'\n" + em);
                }
                if (data instanceof SentenceInterpretation)
                {
                    SentenceInterpretation s = (SentenceInterpretation) data;                    
                    System.out.println(s.getSentence());
                    for (RelOccurrence occ : s.getRelOccs())
                        System.out.println(occ.getRelation().toString(ctx.getGraph()));
                    System.out.println();
                }
                else
                {
                    System.out.println(inputPort.getChannel() + ":\n'" + data
                                       + "'\n");
                }
            }
        }
        log.debug("ConsoleOutputAnalyzer ends");

    }
}
