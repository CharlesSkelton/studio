/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.explorer.view;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.HashMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;

/**
 * Glass pane which is used for paint of a drop line over <code>JComponent</code>.
 *
 * @author  Jiri Rechtacek
 *
 * @see java.awt.dnd.DropTarget
 * @see org.openide.explorer.view.TreeViewDropSupport
 */
final class DropGlassPane extends JPanel {

    Line2D line = null;
    static private HashMap map = new HashMap ();
    final static private int MIN_X = 5;
    final static private int MIN_Y = 3;
    final static private int MIN_WIDTH = 10;
    final static private int MIN_HEIGTH = 3;
    
    transient static private Component oldPane;
    transient static private JTree originalSource;
    transient static private boolean wasVisible;

    private DropGlassPane () {
    }

    /** Check the bounds of given line with the bounds of this pane. Optionally
     * calculate the new bounds in current pane's boundary.
     * @param comp
     * @return  */    
    synchronized static public DropGlassPane getDefault (JComponent comp) {
        Integer id = new Integer (System.identityHashCode (comp));
        if ((map.get (id))==null) {
            map.put (id, new DropGlassPane ());
        }
        return (DropGlassPane)map.get (id);
    }
    
    /** Stores the original glass pane on given tree.
     * @param source the active container
     * @param pane the original glass
     * @param visible was glass pane visible
     */    
    static void setOriginalPane (JTree source, Component pane, boolean visible) {
        // pending, should throw an exception that original is set already
        oldPane = pane;
        originalSource = source;
        wasVisible = visible;
    }
    
    /** Is any original glass pane stored?
     * @return true if true; false otherwise
     */    
    static boolean isOriginalPaneStored () {
        return oldPane != null;
    }
    
    /** Sets the original glass pane to the root pane of stored container.
     */    
    static void putBackOriginal () {
        if (oldPane == null)
            // pending, should throw an exception
            return ;
        originalSource.getRootPane ().setGlassPane (oldPane);
        oldPane.setVisible (wasVisible);
        oldPane = null;
    }
    
    /** Unset drop line if setVisible to false.
     * @param boolean aFlag new state */    
    public void setVisible (boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag)
            setDropLine (null);
    }
    
    /** Set drop line. Given line is used by paint method.
     * @param line drop line */    
    public void setDropLine (Line2D line) {
        this.line = line;
        //repaint ();
    }
    
    /** Check the bounds of given line with the bounds of this pane. Optionally
     * calculate the new bounds in current pane's boundary.
     * @param line a line for check
     * @return  a line with bounds inside the pane's boundary */    
    private Line2D checkLineBounds (Line2D line) {
        Rectangle bounds = getBounds();
        double startPointX, startPointY, endPointX, endPointY;
        // check start point
        startPointX = Math.max (line.getX1 (), bounds.x+MIN_X);
        startPointY = Math.max (line.getY1 (), bounds.y+MIN_Y);
        
        // check end point
        endPointX = Math.min (line.getX2 (), (bounds.x+bounds.width)-MIN_WIDTH);
        endPointY = Math.min (line.getY2 (), (bounds.y+bounds.height)-MIN_HEIGTH);
        
        // set new bounds
        line.setLine(startPointX, startPointY, endPointX, endPointY);
        
        return line;
    }

    /** Paint drop line on glass pane.
     * @param Graphics g Obtained graphics */
    public void paint(Graphics g) {
        if (line!=null) {
            // check bounds
            line = checkLineBounds (line);
            
            // LINE
            g.drawLine ((int)line.getX1 (), (int)line.getY1 (), (int)line.getX2 (), (int)line.getY1 ());
            g.drawLine ((int)line.getX1 (), (int)line.getY1 ()+1, (int)line.getX2 (), (int)line.getY1 ()+1);
            // RIGHT
            g.drawLine ((int)line.getX1 (), (int)line.getY1 ()-2, (int)line.getX1 (), (int)line.getY1 ()+3);
            g.drawLine ((int)line.getX1 ()+1, (int)line.getY1 ()-1, (int)line.getX1 ()+1, (int)line.getY1 ()+2);
            // LEFT
            g.drawLine ((int)line.getX2 (), (int)line.getY1 ()-2, (int)line.getX2 (), (int)line.getY1 ()+3);
            g.drawLine ((int)line.getX2 ()-1, (int)line.getY1 ()-1, (int)line.getX2 ()-1, (int)line.getY1 ()+2);
        }
        // help indication of glass pane for debugging
        /*g.drawLine (0, getBounds ().height / 2, getBounds ().width, getBounds ().height / 2);
        g.drawLine (0, getBounds ().height / 2+1, getBounds ().width, getBounds ().height / 2+1);
        g.drawLine (getBounds ().width / 2, 0, getBounds ().width / 2, getBounds ().height);
        g.drawLine (getBounds ().width / 2+1, 0, getBounds ().width / 2+1, getBounds ().height);
         */
    }
}
