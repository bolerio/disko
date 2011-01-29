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
package disko.relex;

/**
 * 
 * <p>
 * A stemmer will produce one or more variants of possible root forms of a 
 * given word.
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public interface Stemmer
{
	Iterable<String> stemIt(String word);
}
