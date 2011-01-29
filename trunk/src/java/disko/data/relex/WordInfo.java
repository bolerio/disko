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

public class WordInfo {
	
	private float countAsSingleArgument; 
	
	private float countAsFirstArgument;

	private float countAsSecondArgument;

	public WordInfo() {
	}

	public float getCountAsFirstArgument() {
		return countAsFirstArgument;
	}

	public void setCountAsFirstArgument(float countAsFirstArgument) {
		this.countAsFirstArgument = countAsFirstArgument;
	}

	public float getCountAsSecondArgument() {
		return countAsSecondArgument;
	}

	public void setCountAsSecondArgument(float countAsSecondArgument) {
		this.countAsSecondArgument = countAsSecondArgument;
	}

	public float getCountAsSingleArgument() {
		return countAsSingleArgument;
	}

	public void setCountAsSingleArgument(float countAsSingleArgument) {
		this.countAsSingleArgument = countAsSingleArgument;
	}

	@Override
	public String toString() {
		return "W(" + countAsFirstArgument + ", " + countAsSecondArgument + ")";
	}
}
