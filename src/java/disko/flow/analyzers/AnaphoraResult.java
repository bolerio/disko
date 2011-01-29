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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import relex.feature.FeatureNode;

public class AnaphoraResult implements Serializable {
	
	private static final long serialVersionUID = -8159965317811091777L;

	protected static Log log = LogFactory.getLog(AnaphoraResult.class);
	
	protected HashMap<FeatureNode,ArrayList<FeatureNode>> amap;

	public HashMap<FeatureNode, ArrayList<FeatureNode>> getAmap() {
		return amap;
	}

	public void setAmap(HashMap<FeatureNode, ArrayList<FeatureNode>> amap) {
		this.amap = amap;
	}

	public AnaphoraResult(HashMap<FeatureNode, ArrayList<FeatureNode>> amap) {
		super();
		this.amap = amap;
	}
	
	public ArrayList<ArrayList<String>> dumpAntecedents() {
		ArrayList<ArrayList<String>> r = new ArrayList<ArrayList<String>>();
		
		for (FeatureNode prn: amap.keySet()) {	
			// Get the anaphore (pronoun)
			FeatureNode srcNSource = prn;
			String srcName = getName(srcNSource);
			if (srcName == null) continue;
			
			final String prnName = getName(prn);
			
			// Loop over list of antecedents
			ArrayList<FeatureNode> ante_list = amap.get(prn);
			for (Integer i =0; i<ante_list.size(); i++)
			{
				// The larger the "i", the less likely this is the correct antecedent.
				String linkName = "_ante_" + i;
				
				FeatureNode targetNSource = ante_list.get(i);
				String targetName = getName(targetNSource);
				
				ArrayList<String> a = new ArrayList<String>();
				a.add(linkName);
				a.add(prnName);
				a.add(targetName);
				
				r.add(a);
				
				log.debug(linkName+"("+prnName+", "+targetName+")");
			}
		}
		
		return r; 
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
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (ArrayList<String> a: dumpAntecedents()){
			sb.append(a.get(0)+"("+a.get(1)+", "+a.get(2)+")\n");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((amap == null) ? 0 : amap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AnaphoraResult other = (AnaphoraResult) obj;
		if (amap == null) {
			if (other.amap != null)
				return false;
		} else if (!amap.equals(other.amap))
			return false;
		return true;
	}	
	
	
}
