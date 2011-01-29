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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SuffixStemmer implements Stemmer
{
	private String[][] suffixes;

	
	public String[][] getSuffixes()
	{
		return suffixes;
	}


	public void setSuffixes(String[][] suffixes)
	{
		this.suffixes = suffixes;
	}

	public Iterable<String> stemIt(String word)
	{
		ArrayList<String> results = new ArrayList<String>();
		
		if (suffixes == null)
			return results;
		
		for (int i = 0; i < suffixes.length; i++) 
		{
			if (word.endsWith(suffixes[i][0])) 
			{
				results.add(word.substring(
                        0, word.length() - suffixes[i][0].length()) + suffixes[i][1]);				
			}
		} 
		return results;
	}
	
	public static String [][] parseSuffixStemmerConfiguration(String configuration)
	{
		StringTokenizer tokenizer = new StringTokenizer(configuration, "|=", true);
		if (!"|".equals(tokenizer.nextToken())) 
		{
			throw new RuntimeException("Suffix stemmer configuration must starat with |");
		}
		List<String[]> suffixList = new ArrayList<String[]>();
		while (tokenizer.hasMoreTokens()) 
		{
			String next = tokenizer.nextToken();
			String first = "";
			String second = "";
			if (!"=".equals(next)) 
			{
				first = next;
				tokenizer.nextToken();
			}
			next = tokenizer.nextToken();
			if (!"|".equals(next)) 
			{
				second = next;
				tokenizer.nextToken();
			}
			suffixList.add(new String[] {first, second});
		}
		return (String[][]) suffixList.toArray(new String[suffixList.size()][]);
	}
	
	public static SuffixStemmer makeSuffixStemmer(String configuration)
	{
		SuffixStemmer result = new SuffixStemmer();
		result.setSuffixes(parseSuffixStemmerConfiguration(configuration));
		return result;
	}
}
