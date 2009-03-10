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

import java.awt.dnd.*;
import java.awt.Point;

import javax.swing.JList;

import org.openide.nodes.Node;

/**
*
* @author Dafe Simonek, Jiri Rechtacek
*/
class ListViewDragSupport extends ExplorerDragSupport {
    // Attributes

    /** Holds selected indices - it's here only
    * as a workaround for sun's bug */
    /*int[] oldSelection;
    int[] curSelection;*/

    // Associations

    /** The view that manages viewing the data in a tree. */
    protected ListView view;

    /** The tree which we are supporting (our client) */
    protected JList list;


    // Operations

    /** Creates new TreeViewDragSupport, initializes gesture */
    public ListViewDragSupport (ListView view, JList list) {
        this.comp = list;
        this.view = view;
        this.list = list;
    }

    /** Initiating the drag */
    public void dragGestureRecognized (DragGestureEvent dge) {
        super.dragGestureRecognized (dge);
    }

    /** Utility method. Returns either selected nodes in the list
    * (if cursor hotspot is above some selected node) or the node
    * the cursor points to.
    * @return Node array or null if position of the cursor points
    * to no node.
    */
    Node[] obtainNodes (DragGestureEvent dge) {
        Point dragOrigin = dge.getDragOrigin();
        int index = list.locationToIndex(dge.getDragOrigin());
        Object obj = list.getModel().getElementAt(index);
        if( obj instanceof VisualizerNode )
            obj = ((VisualizerNode) obj).node;
        // check conditions
        if( (index < 0) )
            return null;
        if( !( obj instanceof Node) )
            return null;
        Node[] result = null;
        if (list.isSelectedIndex(index)) {
            // cursor is above selection, so return all selected indices
            Object[] selected = list.getSelectedValues();
            result = new Node[selected.length];
            for (int i = 0; i < selected.length; i++) 
            {
                if( selected[i] instanceof VisualizerNode )
                    result[i] = ((VisualizerNode) selected[i]).node;
                else
                {
                    if (!( selected[i] instanceof Node))
                        return null;
                    result[i] = (Node)selected[i];
                }
            }
        } else {
            // return only the node the cursor is above
            result = new Node[] { (Node)obj };
        }
        return result;
    }

} // end of ListViewDragSupport
