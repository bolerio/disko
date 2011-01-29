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


import java.util.ArrayList;
import java.util.List;

import org.hypergraphdb.app.dataflow.AbstractProcessor;
import org.hypergraphdb.app.dataflow.InputPort;
import org.hypergraphdb.app.dataflow.OutputPort;
import org.hypergraphdb.app.dataflow.Ports;

import disko.AnalysisContext;
import disko.DU;
import disko.TextDocument;
import relex.entity.EntityInfo;
import relex.entity.EntityMaintainer;

/**
 * 
 * <p>
 * Combine entity analysis from GATE and OpenNLP giving priority to GATE
 * in case of conflict (because GATE's detected entities come from predefined
 * lists and are presumably more accurate).
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class CombinedEntityAnalyzer extends AbstractProcessor<AnalysisContext<TextDocument>>
{
	public static final String GATE_ENTITY_CHANNEL = "GATE_ENTITY_CHANNEL";
	public static final String OPENNLP_ENTITY_CHANNEL = "OPENNLP_ENTITY_CHANNEL";
	
	private boolean overlap(EntityInfo x, EntityInfo y)
	{
		return x.getFirstCharIndex() >= y.getFirstCharIndex() &&
			   x.getFirstCharIndex() <= y.getLastCharIndex() ||
			   y.getFirstCharIndex() >= x.getFirstCharIndex() &&
			   y.getFirstCharIndex() <= x.getLastCharIndex();
	}	
	
	public void process(AnalysisContext<TextDocument> ctx, Ports ports) throws InterruptedException
	{
		InputPort<EntityMaintainer> gateIn = ports.getInput(GATE_ENTITY_CHANNEL);
		InputPort<EntityMaintainer> openNlpIn = ports.getInput(OPENNLP_ENTITY_CHANNEL);
		OutputPort<EntityMaintainer> out = ports.getSingleOutput();
		
		DU.log.info("Starting Combined Entity Processor.");
		
		while (true)
		{
			EntityMaintainer fromGate = gateIn.take();
			EntityMaintainer fromOpenNlp = openNlpIn.take();
			
			DU.log.debug("From gate: '" + fromGate.getOriginalSentence() + "'");
			DU.log.debug("From openNLP: '" + fromOpenNlp.getOriginalSentence() + "'");
			if (gateIn.isEOS(fromGate) || openNlpIn.isEOS(fromOpenNlp))
			{
				if (gateIn.isEOS(fromGate) ^ openNlpIn.isEOS(fromOpenNlp))
					throw new RuntimeException("EOF b/w Gate and OpenNLP entity maintainers out of synch.");
				break;
			}
			
			// Merge to two sets of entities found. Entities detected at independent positions
			// are accumulated in order. Whenever entities coming from the two detectors overlap,
			// a new one is constructed that spans both of them and that includes the extended
			// information (i.e. EntityInfo attributes) from the GATE detector.
			ArrayList<EntityInfo> merged = new ArrayList<EntityInfo>();
			int gi = 0, oi = 0;			
			List<EntityInfo> G = fromGate.getEntities();
			List<EntityInfo> O = fromOpenNlp.getEntities();
			DU.log.debug("Merging GATE result " + fromGate + " to OpenNLP result " + fromOpenNlp);
			while (gi < G.size() && oi < O.size())
			{
				EntityInfo gentity = G.get(gi);
				EntityInfo oentity = O.get(oi);				
				if (overlap(gentity, oentity))
				{
					EntityInfo info = new EntityInfo(gentity.getOriginalSentence(),
													 Math.min(gentity.getFirstCharIndex(), oentity.getFirstCharIndex()),
													 Math.max(gentity.getLastCharIndex(), oentity.getLastCharIndex()),
													 gentity.getType());
					info.getAttributes().putAll(gentity.getAttributes());
					info.getNodePropertyNames().addAll(gentity.getNodePropertyNames());
					merged.add(info);
					gi++;
					oi++;
				}
				else if (gentity.getFirstCharIndex() < oentity.getFirstCharIndex())
				{
					merged.add(gentity);
					gi++;
				}
				else
				{
					merged.add(oentity);
					oi++;
				}	
			}
			merged.addAll(G.subList(gi, G.size()));
			merged.addAll(O.subList(oi, O.size()));
			out.put(new EntityMaintainer(fromGate.getOriginalSentence(), merged));
			DU.log.debug("Merge completed, result sent to output.");
		}
		DU.log.info("Finished Combined Entity Processor.");
	}
}
