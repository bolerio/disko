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
package disko.taca;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;

import relex.corpus.DocSplitter;
import relex.corpus.TextInterval;

public class OpenNLPSentenceDetector implements DocSplitter
{
	private static final int DEBUG = 0;
	private static final String DEFAULT_ENGLISH_FILENAME =
	        "data/sentence-detector/EnglishSD.bin.gz";

	private static HashSet<String> capitalizedUnacceptableSentenceEnds;

	static
	{
		// The NLP toolkit seems to work fine with Dr., Mrs. etc.
		// but chokes on Ms.
		capitalizedUnacceptableSentenceEnds = new HashSet<String>();
		capitalizedUnacceptableSentenceEnds.add("MS.");
		capitalizedUnacceptableSentenceEnds.add("MR.");
	}

	private static SentenceDetector detector;

	// Returned values
	private ArrayList<TextInterval> lst;
	private ArrayList<String> snl;

	// Buffered text, for FIFO mode.
	private String buffer;

	// parameters
	private String englishModelFilename;

	/* --------------------------------------------------------------- */
	public OpenNLPSentenceDetector()
	{
		buffer = "";
	}

	private void initialize()
	{
		if (detector == null)
		{
			try
			{
				if (englishModelFilename == null)
					englishModelFilename = System.getProperty("EnglishModelFilename");
				if (englishModelFilename == null || englishModelFilename.length() == 0)
				{
				    String home = System.getProperty("disko.home");
				    if (home == null)
				        home = "";
					englishModelFilename = home + DEFAULT_ENGLISH_FILENAME;
				}
				detector = new SentenceDetectorME(new SuffixSensitiveGISModelReader(new File(englishModelFilename)).getModel());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/* --------------------------------------------------------------- */
	public void setEnglishModelFilename(String emf)
	{
		englishModelFilename = emf;
	}

	public String getEnglishModelFilename()
	{
		return englishModelFilename;
	}

	/* --------------------------------------------------------------- */
	/**
	 * Returns false if break is unacceptable. Used to prevent overzelous
	 * sentence detectors which have recognizable idiosyncracies
	 */
	public boolean acceptableBreak(String s, int start, int end)
	{
		// if the string ends with "Ms." preceeded by whitespace
		for (String endString : capitalizedUnacceptableSentenceEnds)
		{
			int len = endString.length();
			if (end >= start + len && s.substring(end - len, end).toUpperCase().equals(endString)
			    && (end == start + len || Character.isWhitespace(s.charAt(end - len - 1)))) 
			{
				return false;
			}
		}
		return true;
	}

	/* --------------------------------------------------------------- */
	/**
	 * Add more text to the buffer.
	 * This allows this class to be used in FIFO mode: text is added with
	 * this call, and sentences are extracted with the getNextSentence() call.
	 */
	public void addText(String newText)
	{
		buffer += newText;
	}

	/**
	 * Clear out the sentence buffer.
	 */
	public void clearBuffer()
	{
		buffer = "";
	}

	/**
	 * Get the next sentence out of the buffered text.
	 * Return null if there are no complete sentences in the buffer.
	 */
	public String getNextSentence()
	{
        initialize();
        
		// punt if no sentence detector
		if (detector == null)
		{
			String rc = buffer;
			buffer = "";
			return rc;
		}

		int[] sentenceEnds = detector.sentPosDetect(buffer);
		if (0 == sentenceEnds.length) return null;

		start = 0;
		end = sentenceEnds[0];
		if (!foundSentence(buffer)) return null;

		buffer = buffer.substring(trimmedEnd);
		return trimmedSentence;
	}

	/* --------------------------------------------------------------- */
	int start;
	int end;
	int trimmedStart;
	int trimmedEnd;
	String trimmedSentence;

	private Boolean foundSentence(String docText)
	{
		trimmedSentence = docText.substring(start, end).trim();
		if (trimmedSentence == null || trimmedSentence.length() == 0)
			return false;

		trimmedStart = docText.indexOf(trimmedSentence.charAt(0), start);
		trimmedEnd = trimmedStart + trimmedSentence.length();
		return acceptableBreak(docText, trimmedStart, trimmedEnd);
	}

	/* --------------------------------------------------------------- */
	/**
	 * Split a document text string into sentences.
	 * Returns a list of sentence start and end-points.
	 */
	public ArrayList<TextInterval> process(String docText)
	{
		_process(docText);
		return lst;
	}

	/**
	 * Split a document text string into sentences.
	 * Returns a list of sentence strings.
	 */
	public ArrayList<String> split(String docText)
	{
		_process(docText);
		return snl;
	}

	private void _process(String docText)
	{
        initialize();
        
		lst = new ArrayList<TextInterval>();
		snl = new ArrayList<String>();
		if (docText == null) return;

		int[] sentenceEnds = detector.sentPosDetect(docText);

		// The detector chokes on single sentences for some reason.
		if (sentenceEnds.length == 0)
		{
	        trimmedSentence = docText.trim();
	        if (trimmedSentence != null && trimmedSentence.length() > 0)
	        {
    	        trimmedStart = docText.indexOf(trimmedSentence.charAt(0), start);
    	        trimmedEnd = trimmedStart + trimmedSentence.length();
                lst.add(new TextInterval(trimmedStart, trimmedEnd));
                snl.add(trimmedSentence);
	        }
            return;
		}
		
		start = 0;
		end = 0;
		for (int sentenceEnd : sentenceEnds)
		{
			int prevstart = start;
			start = end; // from previous loop iteration
			end = sentenceEnd;

			if (!foundSentence(docText))
			{
				// go back to previous start
				start = prevstart;
				end = prevstart;
				continue;
			}

			if (DEBUG > 0) System.out.println(start + "," + end + ": " + trimmedSentence);
			lst.add(new TextInterval(trimmedStart, trimmedEnd));
			snl.add(trimmedSentence);
		}
	}
	
    public boolean operational()
    {
        if (detector == null) return false;
        return true;
    }	
}
