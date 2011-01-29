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

import relex.corpus.TextInterval;

public class BaseAnn implements Ann 
{
	private static final long serialVersionUID = 5748605342878256148L;
	
	private TextInterval interval;
	
	public BaseAnn()
	{		
		interval = new TextInterval();
	}
	
	public BaseAnn(int start, int end)
	{
		this.interval = new TextInterval(start, end);
	}
	
	public BaseAnn(TextInterval interval)
	{
		this.interval = interval;
	}
	
	public TextInterval getInterval() 
	{
		return interval;
	}
	
	public void setInterval(TextInterval interval)
	{
		this.interval = interval;
	}
	
	public String toString(){
		return interval.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((interval == null) ? 0 : interval.hashCode());
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
		final BaseAnn other = (BaseAnn) obj;
		if (interval == null) {
			if (other.interval != null)
				return false;
		} else if (!interval.equals(other.interval))
			return false;
		return true;
	}
	
	
}
