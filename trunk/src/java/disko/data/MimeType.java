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
package disko.data;

public class MimeType
{
	public static final MimeType CSS = new MimeType("text/css", "Cascading Style Sheet", ".css");
	public static final MimeType HTML = new MimeType("text/html", 
			"HTML Document", ".html", ".htm", ".asp", ".jsp", ".php", ".aspx", ".htmls"); 
	public static final MimeType PLAIN = new MimeType("text/plain", "Plain Text", 
			".txt", ".conf", ".def", ".log", ".lst", ".text");
	public static final MimeType PDF = new MimeType("application/pdf", "PDF Document", ".pdf");
	public static final MimeType JPEG = new MimeType("image/jpeg", "JPEG Image", ".jpg", ".jpeg", ".jpe");
	public static final MimeType GIF = new MimeType("image/gif", "GIF Image", ".gif");
	public static final MimeType PNG = new MimeType("image/png", "PNG Image", ".png");
	public static final MimeType TIFF = new MimeType("image/tiff", "TIFF Image", ".tif", ".tiff");
	public static final MimeType EXCEL = new MimeType("application/excel", "MS Excel Document", ".xls");
	public static final MimeType WORD = new MimeType("application/msword", "MS Word Document", ".doc", ".dot");
		
	private String type;
	private String subType;
	private String label;
	private String [] fileExtension;

	public static final MimeType [] ALL = new MimeType[]
    { CSS, HTML, PLAIN, PDF, JPEG, GIF, PNG, TIFF, EXCEL, WORD };
	
	public MimeType()
	{		
	}
	
	public MimeType(String fullType)
	{
		String [] split = fullType.split("/");
		type = split[0];
		subType = split[1];
		label = fullType;
	}
	
	public MimeType(String fullType, String label)
	{
		this(fullType);
		this.label = label;
	}
	
	public MimeType(String fullType, String label, String...fileExtension)
	{
		this(fullType, label);
		this.fileExtension = fileExtension;
	}
	
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public String getSubType()
	{
		return subType;
	}
	public void setSubType(String subType)
	{
		this.subType = subType;
	}
	public String getLabel()
	{
		return label;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public String [] getFileExtension()
	{
		return fileExtension;
	}
	public void setFileExtension(String [] fileExtension)
	{
		this.fileExtension = fileExtension;
	}

	public String toString()
	{
		return type + "/" + subType;
	}
	
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	public boolean equals(Object x)
	{
		if (! (x instanceof MimeType))
			return false;
		else
			return toString().equals(x.toString());
	}
}
