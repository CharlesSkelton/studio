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
import java.util.Arrays;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;

import org.openide.nodes.Node;
import org.openide.explorer.ExplorerPanel;

/** Support for the drag operations in the TreeView.
*
* @author Dafe Simonek, Jiri Rechtacek
*/
final class TreeViewDragSupport extends ExplorerDragSupport {
    // Attributes

    /** True when we are active, false otherwise */
    boolean active = false;

    /** Recognizes default gesture */
    DragGestureRecognizer defaultGesture;

    /** Holds content of the selection in tree.
    * It's here only for workaround of sun's bug */
    TreePath[] curSelection;
    TreePath[] oldSelection;
    
    // Associations

    /** The view that manages viewing the data in a tree. */
    protected TreeView view;

    /** The tree which we are supporting (our client) */
    private JTree tree;

    /** Cell renderer - PENDING - do we need it? */
    //protected DnDTreeViewCellRenderer cellRenderer;

    // Operations
    /** Creates new TreeViewDragSupport, initializes gesture */
    public TreeViewDragSupport(TreeView view, JTree tree) {
        this.view = view;
        this.comp = tree;
        this.tree = tree;
    }

    public void dragGestureRecognized (DragGestureEvent dge) {
        super.dragGestureRecognized (dge);
        // notify tree cell editor that DnD operationm is active
        if (exDnD.isDnDActive ()) {
            TreeCellEditor tce = ((JTree)tree).getCellEditor ();
            if (tce instanceof TreeViewCellEditor)
                ((TreeViewCellEditor)tce).setDnDActive (true);
        }
    }

    
    public void dragDropEnd (DragSourceDropEvent dsde) {
        // get the droped nodes
        Node[] dropedNodes = exDnD.getDraggedNodes ();
        super.dragDropEnd (dsde);

        // if any original glass pane was stored (the DnD was broken e.g. by Esc)
        if (DropGlassPane.isOriginalPaneStored ()) {
            // give back the orig glass pane
            DropGlassPane.putBackOriginal ();
            // DnD is not active
            exDnD.setDnDActive (false);
        }
        
        // select the droped nodes
        try {
            if (dropedNodes!=null) {
                ExplorerPanel panel = (ExplorerPanel)SwingUtilities
                    .getAncestorOfClass(ExplorerPanel.class, view);
                if (panel!=null) {
                    panel.getExplorerManager ().setSelectedNodes (dropedNodes);
                }
            }
        } catch (Exception e) {
            // don't care
        }
        
        // notify tree cell editor that DnD operationm is active
        // no more
        TreeCellEditor tce = tree.getCellEditor();
        if (tce instanceof TreeViewCellEditor)
            ((TreeViewCellEditor)tce).setDnDActive (false);
    }

    /** Utility method. Returns either selected nodes in tree
    * (if cursor hotspot is above some selected node) or the node
    * the cursor points to.
    * @return Node array or null if position of the cursor points
    * to no node.
    */
    Node[] obtainNodes (DragGestureEvent dge) {
        Point dragOrigin = dge.getDragOrigin();
        TreePath tp = tree.getPathForLocation(dragOrigin.x, dragOrigin.y);
        
        if (tp==null)
            return null;
        
        Node n=DragDropUtilities.secureFindNode(tp.getLastPathComponent());
        
        if (n==null)
            return null;
        
        // workaround for Sun's bug #4165577
        // we must repair the selection before dragging
        if ((oldSelection != null) && wasSelected(n)) {
            tree.setSelectionPaths(oldSelection);
            curSelection = null;
        }
        // ---end of workaround
        Node[] result = null;
        if (tree.isPathSelected(tp)) {
            // cursor above selected, so return all selected nodes
            TreePath[] tps = tree.getSelectionPaths();
            result = new Node[tps.length];
            
            for (int i = 0; i < tps.length; i++) {
                result[i]=DragDropUtilities.secureFindNode(tps[i].getLastPathComponent());
                
                if (result[i]==null)
                    return null;
            }
        } else {
            // return only the node the cursor is above
            result = new Node[] { n };
        }
        return result;
    }

    /** Stores last two selections.
    * Workaround for sun's bug */
    public void valueChanged (TreeSelectionEvent tse) {
        TreePath[] newSelection = tree.getSelectionPaths();
        if ((newSelection != null) &&
                (!Arrays.equals(curSelection, newSelection))) {
            oldSelection = (curSelection == null) ? newSelection : curSelection;
            curSelection = newSelection;
        }
    }

    /** @return True if given object was selected in old selection,
    * false otherwise */
    boolean wasSelected (Object obj) {
        if (oldSelection == null)
            return false;
        for (int i = 0; i < oldSelection.length; i++) {
            if (obj.equals(oldSelection[i].getLastPathComponent()))
                return true;
        }
        return false;
    }


} /* end class TreeViewDragSupport */
