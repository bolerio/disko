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

import java.io.File;

import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.util.PDFTextStripper;

public class PDFDocument extends DefaultTextDocument 
{
	public static final String PAGE_ANN = "page";
	
	private static Log log = LogFactory.getLog(PDFDocument.class);
	
	protected synchronized String load()
	{
		final StringWriter stringOutput = new StringWriter();
        PDDocument document = null;
        try 
        {
        	document = PDDocument.load(ContentDownloader.getInstance().getInputStream(getUrlString()));
        	title = document.getDocumentInformation().getTitle();
            PDFTextStripper stripper = new PDFTextStripper()
            {
            	int startPage=0; 
            	
				private int getIndex() 
				{
					return stringOutput.getBuffer().length();
				}

				@Override
				protected void startPage(PDPage page) throws IOException 
				{
					startPage = getIndex();
					log.debug("START PAGE "+getCurrentPageNo()+" AT "+startPage);
				}

				@Override
				protected void endPage(PDPage page) throws IOException 
				{
					int endPage = getIndex();
					DocumentAnn ann = new DocumentAnn(startPage, endPage, PAGE_ANN);
					annotations.add(ann);
					log.debug("END PAGE "+getCurrentPageNo()+" AT "+endPage);
				}
				
            };
//            stripper.setSortByPosition(false);
//            String maxPageSetting = System.getProperty("disco.pdf.max.pages", 
//            										   Integer.toString(Integer.MAX_VALUE));
//            stripper.setEndPage(Integer.parseInt(maxPageSetting));
            stripper.writeText( document, stringOutput );
        }
        catch (Throwable t){
        	throw new RuntimeException("Unable to read resource at URL '" + url + "'", t);
        }
        finally {
            if( stringOutput != null ) {
            	try {
            		stringOutput.close();
            	} catch (IOException e) {
				}
            }
            if( document != null ) {
            	try {
					document.close();
				} catch (IOException e) {
				}
            }
        }
        
		String s = DU.replaceUnicodePunctuation(stringOutput.toString());
		ParagraphDetector.detectParagraphs(s, this.annotations);
		
		return (fullText = new WeakReference<String>(s)).get();
	}
	
	public PDFDocument()
	{		
	}
	
	public PDFDocument(URL url)
	{
		super(url);
	}
	
	public PDFDocument(File f)
	{
		super(f);
	}
	
	public static void main(String[] args){
		File dir = new File("/var/tmp/muriloq/mdc/selected");
		
		File[] pdfFiles = dir.listFiles(new FilenameFilter(){
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".pdf");
			}});
		
		for (File f:pdfFiles){
			try {
				System.out.println(f.getName());
				PDFDocument pdf = new PDFDocument(f);
				pdf.getFullText();
				System.out.println(pdf.getFullText());
			} catch (Throwable t){
				t.printStackTrace();
			}
		}
	}
	
}
