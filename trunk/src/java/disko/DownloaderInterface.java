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
import java.io.InputStream;

/**
 * <p>
 * This interface abstracts Disko's needs for reading content from arbitrary
 * locations. Locations are usually HTTP-based URLs and the default implementation
 * of this interface handles this case and is usually enough 
 * {@link DefaultContentDownloader}. However, if reading from a file system or ftp,
 * with a login sequence, or behind firewall etc. is needed, a different implementation
 * must be provided.
 * </p>
 * 
 * @author boris
 *
 */
public interface DownloaderInterface
{
	byte [] readRawData(String location);
	String readText(String location);
	InputStream getInputStream(String location) throws IOException;
}
