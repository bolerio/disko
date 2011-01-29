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
package test.disco.relex;

import java.io.*;

public class RelexTestDriver
{
	public static void testCasesToClauses(String inFilename, String outFilename)
	{
		BufferedReader in = null;
		FileWriter out = null;
		try
		{
			in = new BufferedReader(new FileReader(inFilename));
			out = new FileWriter(outFilename);
			for (String line = in.readLine(); line != null; line = in.readLine())
			{
				if (!line.startsWith("<<<"))
					out.write(line + "\n");
				else
				{
					int p = line.indexOf(">>>");
					String sentence = line.substring(3, p);
					String parse = line.substring(p + 4);
					out.write("relexParse('" + sentence + "'," + parse + ").\n");
				}
			}
		}
		catch (Exception ex)
		{
			throw new RuntimeException (ex);
		}
		finally
		{
			if (in != null) try { in.close(); } catch (Throwable t) { }
			if (out != null) try { out.close(); } catch (Throwable t) { }
		}
	}
	
	public static void main(String [] argv)
	{
		if (argv.length != 1)
		{
			System.err.println("Syntax: RelexTestDriver testCaseFileName.");
			System.exit(-1);
		}
		
		String filename = argv[0];
		
	}
}
