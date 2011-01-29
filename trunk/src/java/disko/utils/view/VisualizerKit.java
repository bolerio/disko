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
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.text.StyledEditorKit;

import org.hypergraphdb.util.Pair;

public class VisualizerKit extends StyledEditorKit {
	private static final long serialVersionUID = -7058461083710182275L;

    // VisualizerViewFactory factory = new VisualizerViewFactory();
   

	public static JFrame getFrame() {
        JFrame frame = new JFrame("Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final VisualizerPane editor = new VisualizerPane();
        try{
        	for(int i = 0; i< 50; i++)
              editor.getDoc().insertString(0, "Mistery" + i + "\n", null);
        	List<Pair<Integer, Integer>> spanList = 
        		new LinkedList<Pair<Integer, Integer>>();
        	spanList.add(new Pair<Integer, Integer>(5,10));
        	spanList.add(new Pair<Integer, Integer>(20,30));
        	editor.setColor(spanList, Color.red);
        	List<Pair<Integer, Integer>> hideList = 
        		new LinkedList<Pair<Integer, Integer>>();
        	hideList.add(new Pair<Integer, Integer>(0,5));
        	hideList.add(new Pair<Integer, Integer>(10,20));
        	hideList.add(new Pair<Integer, Integer>(50,120));
        	editor.hideText(hideList);
        	//editor.resetColors();
        	//editor.unhideText();
        	spanList = 
        		new LinkedList<Pair<Integer, Integer>>();
        	spanList.add(new Pair<Integer, Integer>(33,34));
        	spanList.add(new Pair<Integer, Integer>(50,100));
        	editor.setColor(spanList, Color.yellow);
        }
        catch(Exception ex){
        	ex.printStackTrace();
        }
        JScrollPane scroll = new JScrollPane(editor);
        frame.getContentPane().add(scroll);
        frame.setSize(200, 100);
        frame.setLocationRelativeTo(null);
        System.out.println("Frame: " + frame);
        return frame;
    }

    public static void main(String[] args) throws Exception {
    	final JFrame frame = VisualizerKit.getFrame();
        java.awt.EventQueue.invokeLater(new Runnable() {
			public void run()
			{
				frame.setVisible(true);
			}
		});
    }

/*
    public ViewFactory getViewFactory() {
        return factory;
    }

     class VisualizerViewFactory implements ViewFactory 
     {

        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new VisualizerLabelView(elem);
                }
                else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new VisualizerParagraphView(elem);
                }
                else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                }
                else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                }
                else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            // default to text display
            return new LabelView(elem);
        }
    }

    class VisualizerParagraphView extends ParagraphView {
        
        protected boolean visible = true;
        public VisualizerParagraphView(Element elem) {
            super(elem);
        }
    }

    class VisualizerLabelView extends LabelView {
    	protected boolean visible = true;
    	
        public VisualizerLabelView(Element elem) {
            super(elem);
        }

        protected void setPropertiesFromAttributes()
		{
        	AttributeSet attr = getAttributes();
			if (attr != null) {
				Boolean b = (Boolean)attr.getAttribute(VisualizerPane.ATTR_VISIBLE);
				if(b != null)
					visible = b.booleanValue();
			}
			super.setPropertiesFromAttributes();
		}
        
        public float getMaximumSpan(int axis)
		{
			if(!visible) return 0;
			return super.getMaximumSpan(axis);
		}
        
        public float getMinimumSpan(int axis)
		{
			if(!visible) return 0;
			return super.getMinimumSpan(axis);
		}

        public float getPreferredSpan(int axis) {
        	if(!visible) return 0;
              return super.getPreferredSpan(axis);
        }

        public void paint(Graphics g, Shape a) {
        	if(!visible) return;
            super.paint(g, a);
        }
    } */
}
