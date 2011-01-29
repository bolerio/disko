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

import java.util.HashMap;
import java.util.Map;

public class DocumentAnn extends BaseAnn 
{
	private static final long serialVersionUID = 669692584211044288L;
	
	private String tag;
	
	private Map<String, String> attributes = new HashMap<String, String>();
	
	public DocumentAnn()
	{		
	}
	
	public DocumentAnn(int start, int end, String tag)
	{
		super(start, end);
		this.tag = tag;
	}
	
	public void setTag(String tag)
	{
		this.tag = tag;
	}
	
	public String getTag()
	{		
		return tag;
	}
	
	public Map<String, String> getAttributes()
	{
		return attributes;
	}
	
	public String toString()
	{
		return tag + super.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DocumentAnn other = (DocumentAnn) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		return true;
	}	
	
	
}
