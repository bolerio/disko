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

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import org.hypergraphdb.util.Pair;

/**
 * 
 * <p>
 * Implements a Spread Activation Network. Construct the network by adding nodes
 * and edges between them. Make sure the type of nodes behaves well as a key
 * in hash tables (i.e. has properly defined and efficient hashCode and equals
 * methods). Once the network is constructed, call the <code>run</code> and then
 * read the resulting weights from <code>getNodeWeights</code> and <code>getTimeOfDeathMap</code>
 * methods. 
 * </p>
 *
 * <p>
 * Activation is spread following Berger et al. (google their paper called 
 * "An Adaptive Information Retrieval System based on Associative Networks", 2004). The network
 * doesn't have to be fully connected and there are no designed initial nodes (this
 * effect can be achieved by simply setting the weights of non-initial nodes to 0). Edges
 * can have positive or negative weights. In the latter case, they are called "inhibitory".
 * </p>
 * 
 * @author Borislav Iordanov
 *
 * @param <NodeType>
 */
public class SAN<NodeType>
{
	class Edge
	{
		NodeType src, dest; // dest is not really needed, added for completeness but may be removed
		double weight;
	}
	
	double threshold;
	
	Map<NodeType, Double> nodeWeights = new HashMap<NodeType, Double>();
	Map<NodeType, List<Edge>> srcToDest = new HashMap<NodeType, List<Edge>>();
	Map<NodeType, List<Edge>> destToSrc = new HashMap<NodeType, List<Edge>>();
	HashMap<NodeType, Double> sources = new HashMap<NodeType, Double>();
	Map<NodeType, Pair<Integer, Double>> timeOfDeath = new HashMap<NodeType, Pair<Integer, Double>>();
	
	private double activationOutput(Map<NodeType, Double> weights, int iteration, NodeType n)
	{
		double weight = weights.get(n);
		double fanoutFactor = 1.0 - (double)srcToDest.get(n).size()/(double)srcToDest.size();
		double result = fanoutFactor * weight / (double)(iteration + 1);
		return result;
	}
	
	public SAN(double threshold)
	{
		this.threshold = threshold;
	}

	public void clear()
	{
		nodeWeights.clear();
		srcToDest.clear();
		destToSrc.clear();
		timeOfDeath.clear();
	}
	
	/**
	 * <p>
	 * Add a node with a specified initial weight.
	 * </p>
	 * 
	 * @param node
	 * @param initialWeight
	 */
 	public void addNode(NodeType node, double initialWeight)
	{
		nodeWeights.put(node, initialWeight);
	}
	
 	/**
 	 * <p>
 	 * Add a directed edge with a given weight.
 	 * </p>
 	 * 
 	 * @param source
 	 * @param destination
 	 * @param weight
 	 */
	public void addEdge(NodeType source, NodeType destination, double weight)
	{
		if (!nodeWeights.containsKey(source))
			throw new RuntimeException("Source node has not been previously added: " + source);
		else if (!nodeWeights.containsKey(destination))
			throw new RuntimeException("Destination node has not been previously added: " + destination);
		Edge e = new Edge();
		e.src = source;
		e.dest = destination;
		e.weight = weight;
		List<Edge> L = srcToDest.get(source);
		if (L == null)
		{
			L = new ArrayList<Edge>();
			srcToDest.put(source, L);
		}
		L.add(e);
		L = destToSrc.get(destination);
		if (L == null)
		{
			L = new ArrayList<Edge>();
			destToSrc.put(destination, L);
		}
		L.add(e);
	}
	
	/**
	 * <p>
	 * Add a bi-directional edge with the same weight in both directions.
	 * </p>
	 * 
	 * @param n1
	 * @param n2
	 * @param weight
	 */
	public void addBiEdge(NodeType n1, NodeType n2, double weight)
	{
		addEdge(n1, n2, weight);
		addEdge(n2, n1, weight);
	}
	
	public void run()
	{
		boolean done = false;
		sources.clear();
		for (Map.Entry<NodeType, Double> e : nodeWeights.entrySet())
			if (!destToSrc.containsKey(e.getKey()))
				sources.put(e.getKey(), e.getValue());
		Map<NodeType, Double> weights = nodeWeights;
		for (int iteration = 1; !done; iteration++)
		{
			Map<NodeType, Double> nextWeights = new HashMap<NodeType, Double>();
			done = true;
			for (Map.Entry<NodeType, List<Edge>> n : destToSrc.entrySet())
			{
				if (timeOfDeath.containsKey(n.getKey()))
					continue;
				double nextWeight = 0.0;
				for (Edge e : n.getValue())					
					if (!timeOfDeath.containsKey(e.src))
					{
						done = false;
						nextWeight += activationOutput(weights, iteration, e.src) * e.weight;
					}
				if (nextWeight < threshold) // node becomes inactive
					timeOfDeath.put(n.getKey(),new Pair<Integer, Double>(iteration, nextWeight));
				nextWeights.put(n.getKey(), nextWeight);
			}
			weights = nextWeights;
			weights.putAll(sources);
		}
	}
	
	public Map<NodeType, Double> getNodeWeights()
	{
		return nodeWeights;
	}

	HashMap<NodeType, Double> getSources()
	{
		return sources;
	}
	
	public Map<NodeType, Pair<Integer, Double>> getTimeOfDeathMap()
	{
		return timeOfDeath;
	}
	
	public Collection<NodeType> getNodes()
	{
		return srcToDest.keySet();
	}

	public void print(PrintStream out) throws IOException
	{
		
	}
	
	public String toString()
	{
		int edgeCount = 0;
		for (List<Edge> L : srcToDest.values()) edgeCount += L.size();
		for (List<Edge> L : destToSrc.values()) edgeCount += L.size();
		return "SAN(" + nodeWeights.size() + " nodes, " + edgeCount + " edges)";
	}
}
