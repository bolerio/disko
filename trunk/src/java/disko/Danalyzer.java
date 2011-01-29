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


/**
 * 
 * <p>
 * This interface defines a "document analyzer". A document analyzer is something
 * that will perform some analysis on some document in some context.
 * </p>
 * 
 * @author Borislav Iordanov
 *
 * @param <ContextType> The context of analysis. This will be some global data 
 * structure where extracted information is recorded and provided contextual
 * input to the analyzer itself as well.
 * @param <DocumentType> The type of document being analyzed.
 */
public interface Danalyzer<ContextType>
{
	void process(ContextType context);
}
