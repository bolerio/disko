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
package disko.utils.view;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.hypergraphdb.util.Pair;


public class VisualizerPane extends JEditorPane
{
	private static final long serialVersionUID = 5384305459559762059L;
	
	static final String ATTR_VISIBLE = "attr_visible";
	private static MutableAttributeSet invisibleSet = new SimpleAttributeSet();
	private static MutableAttributeSet visibleSet = new SimpleAttributeSet();
	private static MutableAttributeSet colorSet = new SimpleAttributeSet();
	static
	{
		invisibleSet.addAttribute(ATTR_VISIBLE, false);
		visibleSet.addAttribute(ATTR_VISIBLE, true);
	}
		
	protected List<Pair<Integer, Integer>> colorList = new LinkedList<Pair<Integer, Integer>>();
	protected List<Pair<Integer, Integer>> hiddenList = new LinkedList<Pair<Integer, Integer>>();;
	protected MutableAttributeSet bgSet = new SimpleAttributeSet();
	
	public VisualizerPane() 
	{
		super();
		setEditable(false);
		bgSet.addAttribute(StyleConstants.Background, getBackground());
	}
	
	@Override
	public void setBackground(Color bg)
	{
		super.setBackground(bg);
		if (bgSet != null)
			bgSet.addAttribute(StyleConstants.Background, bg);
	}

	protected EditorKit createDefaultEditorKit()
	{
		return new VisualizerKit();
	}
	
	public StyledDocument getDoc(){
		return (StyledDocument) getDocument();
	}
	
	public void reset(){
		resetColors();
		unhideText();
	}
	
	public void setColor(List<Pair<Integer, Integer>> spanList, Color color)
	{
		StyledDocument doc = getDoc();
		for(Iterator<Pair<Integer, Integer>> it = spanList.iterator(); it.hasNext();)
		{
			Pair<Integer, Integer> p = it.next();
			StyleConstants.setBackground(colorSet, color);
			doc.setCharacterAttributes(p.getFirst().intValue(), 
					p.getSecond() - p.getFirst(), colorSet, false);
			colorList.add(p);
		}
	}
	
	void resetColors(){
		apply_attr_set(colorList, bgSet);
		colorList.clear();
	}
	
	public void hideText(List<Pair<Integer, Integer>> spanList)
	{
		StyledDocument doc = getDoc();
		for(Iterator<Pair<Integer, Integer>> it = spanList.iterator(); it.hasNext();)
		{
			Pair<Integer, Integer> p = it.next();
			doc.setCharacterAttributes(p.getFirst().intValue(), 
					p.getSecond() - p.getFirst(), invisibleSet, false);
			hiddenList.add(p);
		}
	}
	
	void unhideText()
	{
		apply_attr_set(hiddenList, visibleSet);
		hiddenList.clear();
	}
	
	private void apply_attr_set(List<Pair<Integer, Integer>> list, AttributeSet s){
		StyledDocument doc = getDoc();
		for(Iterator<Pair<Integer, Integer>> it = list.iterator(); it.hasNext();)
		{
			Pair<Integer, Integer> p = it.next();
			doc.setCharacterAttributes(p.getFirst().intValue(), 
					p.getSecond() - p.getFirst(), s, false);
		}
	}

}
