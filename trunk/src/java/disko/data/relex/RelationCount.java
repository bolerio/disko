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
package disko.data.relex;

import java.io.Serializable;

public class RelationCount implements Serializable {
	private static final long serialVersionUID = 2548290654136406063L;
	public String predicate;
	public String arg0;
	public String arg1;
	public String pos0;
	public String pos1;
	public double count;

	public RelationCount() {
	}

	public RelationCount(String predicate, String arg0, String arg1,
			String pos0, String pos1) {
		super();
		this.predicate = predicate;
		this.arg0 = arg0;
		this.arg1 = arg1;
		this.pos0 = pos0;
		this.pos1 = pos1;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getArg0() {
		return arg0;
	}

	public void setArg0(String arg0) {
		this.arg0 = arg0;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getPos0() {
		return pos0;
	}

	public void setPos0(String pos0) {
		this.pos0 = pos0;
	}

	public String getPos1() {
		return pos1;
	}

	public void setPos1(String pos1) {
		this.pos1 = pos1;
	}

	public double getCount() {
		return count;
	}

	public void setCount(double count) {
		this.count = count;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(predicate + "[" + pos0 + ", " + pos1 + "]");
		sb.append("(" + arg0 + ", "+ arg1 + ")");
		sb.append(" = " + count);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arg0 == null) ? 0 : arg0.hashCode());
		result = prime * result + ((arg1 == null) ? 0 : arg1.hashCode());
		result = prime * result + ((pos0 == null) ? 0 : pos0.hashCode());
		result = prime * result + ((pos1 == null) ? 0 : pos1.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
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
		final RelationCount other = (RelationCount) obj;
		if (arg0 == null) {
			if (other.arg0 != null)
				return false;
		} else if (!arg0.equals(other.arg0))
			return false;
		if (arg1 == null) {
			if (other.arg1 != null)
				return false;
		} else if (!arg1.equals(other.arg1))
			return false;
		if (pos0 == null) {
			if (other.pos0 != null)
				return false;
		} else if (!pos0.equals(other.pos0))
			return false;
		if (pos1 == null) {
			if (other.pos1 != null)
				return false;
		} else if (!pos1.equals(other.pos1))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		return true;
	}
}
