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

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import relex.corpus.TextInterval;

public interface TextDocument 
{
	/**
	 * <p>
	 * Get the piece of the document within the specified interval.
	 * </p>
	 * 
	 * @param interval
	 * @return
	 */
	CharSequence getText(TextInterval interval);
	
	/**
	 * <p>
	 * Create and return a new character-based input reader for this document.
	 * It is the responsibility of the caller to close the reader. Some implementations
	 * may represent the document as an in-memory buffer so closing might not be 
	 * important in those cases. However, callers should always close the result
	 * returned by this method.
	 * </p>
	 * 
	 * @return
	 */
	Reader createReader()  throws IOException;

	/**
	 * <p>
	 * Return the complete text of the document as a string.
	 * </p>
	 */
	String getFullText();	
	
	/**
	 * <p>Return the annotations found in this document (e.g. ParagraphAnn, tags, etc.).</p> 
	 */
	List<Ann> getAnnotations();
}
