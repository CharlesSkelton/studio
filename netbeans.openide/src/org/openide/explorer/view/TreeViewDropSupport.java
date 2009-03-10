/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.explorer.view;

import java.awt.Component;
import java.util.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.*;
import java.awt.geom.Line2D;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeCellEditor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.openide.ErrorManager;
import org.openide.nodes.Children;
import org.openide.nodes.Index;

import org.openide.nodes.Node;
import org.openide.util.datatransfer.PasteType;

/** Implementation of drop support for asociated Tree View.
*
* @author Dafe Simonek, Jiri Rechtacek
*/
final class TreeViewDropSupport implements DropTargetListener, Runnable {

    // Attributes

    /** true if support is active, false otherwise */
    boolean active = false;
    
    boolean dropTargetPopupAllowed;

    /** Drop target asociated with the tree */
    DropTarget dropTarget;

    /** Node area which we were during
    * DnD operation. */
    Rectangle lastNodeArea;
    
    private int upperNodeIdx = -1;
    private int lowerNodeIdx = -1;
    
    /** Swing Timer for expand node's parent with delay time. */
    Timer timer;
    
    /** Glass pane for JTree which is associate with this class. */
    DropGlassPane dropPane;
    
    final static protected int FUSSY_POINTING = 3;
    final static private int DELAY_TIME_FOR_EXPAND = 1000;
    final static private int SHIFT_DOWN = -1;
    final static private int SHIFT_RIGHT = 10;
    final static private int SHIFT_LEFT = 15;
    
    private int pointAt = DragDropUtilities.NODE_CENTRAL;

    // Associations

    /** View manager. */
    protected TreeView view;

    /** The component we are supporting with drop support */
    protected JTree tree;

    // Operations
    /** Creates new TreeViewDropSupport */
    public TreeViewDropSupport (TreeView view, JTree tree, boolean dropTargetPopupAllowed) {
        this.view = view;
        this.tree = tree;
        this.dropTargetPopupAllowed = dropTargetPopupAllowed;
    }
	 
    public void setDropTargetPopupAllowed(boolean value) {
        dropTargetPopupAllowed=value;
    }
    
    public boolean isDropTargetPopupAllowed() {
        return dropTargetPopupAllowed;
    }
    
    /** User is starting to drag over us */
    public void dragEnter (DropTargetDragEvent dtde) {

        // remember current glass pane to set back at end of dragging over this compoment
        if (!DropGlassPane.isOriginalPaneStored ()) {
            Component comp = tree.getRootPane ().getGlassPane ();
            DropGlassPane.setOriginalPane (tree, comp, comp.isVisible ());
            
            // set glass pane for paint selection line
            dropPane = DropGlassPane.getDefault (tree);
            tree.getRootPane ().setGlassPane (dropPane);
            dropPane.setOpaque (false);
            dropPane.revalidate();
            dropPane.setVisible (true);
        }
        // set a status and cursor of dnd action
        doDragOver (dtde);
    }
    
    /** User drags over us */
    public void dragOver (DropTargetDragEvent dtde) {
        // set a status and cursor of dnd action
        doDragOver (dtde);
    }

    /** Process events dragEnter or dragOver. */
    private void doDragOver (DropTargetDragEvent dtde) {
        
        // 1. test if I'm over any node
        TreePath tp = getTreePath(dtde);
        if (tp == null) {
            dtde.rejectDrag();
            removeDropLine ();
            return ;
        }
        
        // 2. find node for drop
        Point p = dtde.getLocation ();
        Node dropNode = getNodeForDrop (p);
        // if I haven't any node for drop then reject drop
        if (dropNode==null) {
            dtde.rejectDrag ();
            removeDropLine ();
            return ;
        }
        
        Rectangle nodeArea = tree.getPathBounds (tp);
        int endPointX = nodeArea.x + nodeArea.width;
        int row = tree.getRowForPath (tp);
        if (nodeArea!=null) {
            pointAt = DragDropUtilities.NODE_CENTRAL;
            if (p.y <= nodeArea.y+FUSSY_POINTING) {
                // don't get line above root
                if (row != 0) {
                    // point above node
                    pointAt = DragDropUtilities.NODE_UP;
                    TreePath upPath = tree.getPathForRow (row-1);
                    if (upPath!=null && !upPath.equals (tp)) {
                        endPointX = Math.max (nodeArea.x + nodeArea.width, 
                                        tree.getPathBounds (upPath).x + tree.getPathBounds (upPath).width);
                    }
                    // drop candidate is parent
                    if (dropNode.getParentNode ()!=null) {
                        dropNode = dropNode.getParentNode ();
                        tp = null;
                    }
                }
            } else if (p.y >= (nodeArea.y+nodeArea.height-FUSSY_POINTING)) {
                // exclude expanded folder
                if (!view.isExpanded (dropNode)) {
                    // point bellow node
                    pointAt = DragDropUtilities.NODE_DOWN;
                    TreePath downPath = tree.getPathForRow (row+1);
                    if (downPath!=null && !downPath.equals (tp)) {
                        endPointX = Math.max (nodeArea.x + nodeArea.width, 
                                        tree.getPathBounds (downPath).x + tree.getPathBounds (downPath).width);
                    }
                    // drop candidate is parent
                    if (dropNode.getParentNode ()!=null) {
                        dropNode = dropNode.getParentNode ();
                        tp = null;
                    }
                }
            }
        }
        endPointX = endPointX + SHIFT_RIGHT;
        
        // 2.b. check index cookie
        Index indexCookie = (Index)dropNode.getCookie (Index.class);
        if (indexCookie!=null) {
            if (pointAt==DragDropUtilities.NODE_UP) {
                lowerNodeIdx = indexCookie.indexOf (getNodeForDrop (p));
                upperNodeIdx = lowerNodeIdx - 1;
            } else if (pointAt==DragDropUtilities.NODE_DOWN) {
                upperNodeIdx = indexCookie.indexOf (getNodeForDrop (p));
                lowerNodeIdx = upperNodeIdx + 1;
            }
        }
        
        // 3. expand with a delay
        if ((timer==null || !timer.isRunning ()) &&
                    dropNode!=null &&
                    !dropNode.isLeaf() &&
                    !view.isExpanded (dropNode)) {
                        
            // ok, let's expand in a while
            // node is candidate for expand
            final Node cn = dropNode;
            
            // remove old timer
            removeTimer ();
            
            // create new timer
            timer = new Timer (DELAY_TIME_FOR_EXPAND, new ActionListener () {
                final public void actionPerformed (ActionEvent e) {
                    view.expandNode (cn);
                }
            });
            timer.setRepeats (false);
            timer.start ();
        }

        // 4. present node for drop

        // prepare selection or line
        if (pointAt==DragDropUtilities.NODE_CENTRAL) {
            // no line
            dropPane.setDropLine (null);
        } else {
            // line and selection of parent if any
            if (pointAt==DragDropUtilities.NODE_UP) {
                Line2D line = new Line2D.Double (nodeArea.x-SHIFT_LEFT, nodeArea.y+SHIFT_DOWN,
                                    endPointX, nodeArea.y+SHIFT_DOWN);
                convertBoundsAndSetDropLine (line);
                // enlagre node area with area for line
                Rectangle lineArea = new Rectangle (nodeArea.x-SHIFT_LEFT, nodeArea.y+SHIFT_DOWN-3,
                                            endPointX-nodeArea.x+SHIFT_LEFT, 5);
                nodeArea = (Rectangle)nodeArea.createUnion (lineArea);
            } else {
                Line2D line = new Line2D.Double (nodeArea.x-SHIFT_LEFT,
                                                 nodeArea.y+nodeArea.height+SHIFT_DOWN,
                                    endPointX, nodeArea.y+nodeArea.height+SHIFT_DOWN);
                convertBoundsAndSetDropLine (line);
                // enlagre node area with area for line
                Rectangle lineArea = new Rectangle (nodeArea.x-SHIFT_LEFT, nodeArea.y+nodeArea.height,
                                            endPointX-nodeArea.x+SHIFT_LEFT, SHIFT_DOWN+3);
                nodeArea = (Rectangle)nodeArea.createUnion (lineArea);
                //System.out.println("OLD + LINE: "+lineArea+" = AREA: "+nodeArea);
            }
            // the parent node won't be selected
            /*// select parent and enlarge paint area
            if (tp.getParentPath ()!=null) {
                tp = tp.getParentPath ();
            }
            nodeArea = (Rectangle)nodeArea.createUnion (tree.getPathBounds (tp));*/
        }
        
        // back normal view w/o any selecetion nor line
        if ((lastNodeArea != null) && (!lastNodeArea.equals (nodeArea))) {
            NodeRenderer.dragExit ();
            repaint (lastNodeArea);
        }
        
        // paint new state
        if (!nodeArea.equals (lastNodeArea)) {
            if (tp!=null)
                NodeRenderer.dragEnter (tp.getLastPathComponent ());
            repaint (nodeArea);
            lastNodeArea = nodeArea;
            removeTimer ();
        }
        
        // 5 show to cursor belong to state
        if (canDrop (dropNode, dtde.getDropAction ())) {
            // ok, can accept
            dtde.acceptDrag (dtde.getDropAction ());
        } else {
            // can only reorder?
            if (canReorder (dropNode, ExplorerDnDManager.getDefault ().getDraggedNodes ())) {
                // ok, can accept only reoder
                dtde.acceptDrag (dtde.getDropAction ());
            } else {
                dtde.rejectDrag ();
            }
        }
    }
    
    /** Repaints TreeView, the given rectangle is enlarged for 5 pixels
     * because some parts was not repainted correctly.
     * @param Rectangle r rectangle which will be repainted.*/
    private void repaint (Rectangle r) {
        tree.repaint (r.x-5, r.y-5, r.width+10, r.height+10);
    }
    
    /** Converts line's bounds by the bounds of the root pane. Drop glass pane
     * is over this root pane. After covert a given line is set to drop glass pane.
     * @param line line for show in drop glass pane */
    private void convertBoundsAndSetDropLine (final Line2D line) {
        int x1 = (int)line.getX1 (), x2 = (int)line.getX2 ();
        int y1 = (int)line.getY1 (), y2 = (int)line.getY2 ();
        Point p1 = SwingUtilities.convertPoint (tree, x1, y1, tree.getRootPane ());
        Point p2 = SwingUtilities.convertPoint (tree, x2, y2, tree.getRootPane ());
        line.setLine (p1, p2);
        dropPane.setDropLine (line);
    }
    
    /** Removes timer and all listeners. */
    private void removeTimer () {
        if (timer!=null) {
            ActionListener[] l = (ActionListener[])timer.getListeners (ActionListener.class);
            for (int i=0; i<l.length; i++) {
                timer.removeActionListener (l[i]);
            }
            timer.stop ();
            timer = null;
        }
    }
    
    public void dropActionChanged (DropTargetDragEvent dtde) {
        // check if the nodes are willing to do selected action
        Node[] nodes = ExplorerDnDManager.getDefault ().getDraggedNodes ();
        int dropAction = dtde.getDropAction ();
        for (int i = 0; i < nodes.length; i++) {
            if (!DragDropUtilities.checkNodeForAction (nodes[i], dropAction)) {
                // this action is not supported
                dtde.rejectDrag ();
                return ;
            }
        }
        
        return ;
    }

    /** User exits the dragging */
    public void dragExit (DropTargetEvent dte) {
        stopDragging ();
    }
    
    private void removeDropLine () {
        dropPane.setDropLine (null);
        if (lastNodeArea != null) {
            NodeRenderer.dragExit();
            repaint (lastNodeArea);
            lastNodeArea = null;
        }
    }

    private void stopDragging() {
        removeDropLine ();
        removeTimer ();
        // set back the remembered glass pane
        if (DropGlassPane.isOriginalPaneStored()) {
            DropGlassPane.putBackOriginal ();
        }
    }
    
    /** Get a node on given point or null if there none*/
    private Node getNodeForDrop (Point p) {
        if (p!=null) {
            TreePath tp = tree.getPathForLocation (p.x, p.y);
            if (tp!=null) {
                return DragDropUtilities.secureFindNode (tp.getLastPathComponent ());
            }
        }
        return null;
    }
    
    private boolean canReorder (Node folder, Node[] dragNodes) {
        if (ExplorerDnDManager.getDefault ().getAllowedDragActions ()!=DnDConstants.ACTION_MOVE) {
            return false;
        }
        if (folder==null||dragNodes.length==0) {
            return false;
        }
        // has folder a index cookie?
        Index ic = (Index)folder.getCookie (Index.class);
        if (ic==null) {
            return false;
        }
        // folder has index cookie
        // check if all dragNodes are from same folder
        for (int i=0; i<dragNodes.length; i++) {
            // bugfix #23988, check if dragNodes[i] isn't null
            if (dragNodes[i]==null) {
                return false;
            }
            if (dragNodes[i].getParentNode ()==null)
                return false;
            if (!dragNodes[i].getParentNode ().equals (folder))
                return false;
        }
        return true;
    }
    
    private void performReorder (final Node folder, Node[] dragNodes, int lNode, int uNode) {
        try {
            Index indexCookie = (Index) folder.getCookie (Index.class);
            if (indexCookie != null) {
                int perm [] = new int [indexCookie.getNodesCount()];
                int indexes [] = new int [dragNodes.length];
                int indexesLength = 0;
                for (int i = 0; i < dragNodes.length; i++) {
                    int idx = indexCookie.indexOf(dragNodes[i]);
                    if (idx >= 0 && idx < perm.length) {
                        indexes[indexesLength++] = idx;
                    }
                }

                // XXX: normally indexes of dragged nodes should be in ascending order, but
                // it seems that Tree.getSelectionPaths doesn't keep this order
                Arrays.sort(indexes);

                if (lNode < 0 || uNode >= perm.length || indexesLength == 0) {
                    return;
                }

                int k = 0;
                for (int i = 0; i < perm.length; i++) {
                    if (i <= uNode) {
                        if (!containsNumber(indexes, indexesLength, i)) {
                            perm[i] = k++;
                        }

                        if (i == uNode) {
                            for (int j = 0; j < indexesLength; j++) {
                                if (indexes[j] <= uNode) {
                                    perm[indexes[j]] = k++;
                                }
                            }
                        }
                    } else {
                        if (i == lNode) {
                            for (int j = 0; j < indexesLength; j++) {
                                if (indexes[j] >= lNode) {
                                    perm[indexes[j]] = k++;
                                }
                            }
                        }

                        if (!containsNumber(indexes, indexesLength, i)) {
                            perm[i] = k++;
                        }
                    }
                }
                
                // check for identity permutation
                for (int i = 0; i < perm.length; i++) {
                    if (perm[i] != i) {
                        indexCookie.reorder(perm);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Pending: add annotation or remove try/catch block
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
        }
    }
    
    private boolean containsNumber(int [] arr, int arrLength, int n) {
        for (int i = 0; i < arrLength; i++) {
            if (arr[i] == n) {
                return true;
            }
        }
        return false;
    }
    
    private Node[] findDropedNodes (Node folder, Node[] dragNodes) {
        if (folder==null||dragNodes.length==0) {
            return null;
        }
        Node[] dropNodes = new Node[dragNodes.length];
        Children children = folder.getChildren ();
        for (int i=0; i<dragNodes.length; i++) {
            dropNodes[i] = children.findChild (dragNodes[i].getName ());
        }
        return dropNodes;
    }
    
    /** Can node recieve given drop action? */
    // XXX canditate for more general support
    private boolean canDrop (Node n, int dropAction) {
        if (n == null) {
            return false;
        }
        
        // test if a parent of the dragged nodes isn't the node over
        // only for MOVE action
        if (DnDConstants.ACTION_MOVE==dropAction) {
            Node[] nodes = ExplorerDnDManager.getDefault ().getDraggedNodes();
            if (nodes == null)
                return false;
            for (int i=0; i<nodes.length; i++) {
                if (n.equals(nodes[i].getParentNode ()))
                    return false;
            }
        }
        
        Transferable trans = ExplorerDnDManager.getDefault ().getDraggedTransferable (DnDConstants.ACTION_MOVE==dropAction);
        if (trans==null) {
            return false;
        }
        
        // get paste types for given transferred transferable
        PasteType[] pt = DragDropUtilities.getPasteTypes(n, trans);
        
        return ((pt!=null) && (pt.length!=0));
    }
    
    /** Performs the drop action, if we are dropping on
    * right node and target node agrees.
    */
    public void drop (DropTargetDropEvent dtde) {
        stopDragging ();
        
        // find node for the drop perform
        Node dropNode = getNodeForDrop (dtde.getLocation ());
        Node [] dragNodes = ExplorerDnDManager.getDefault ().getDraggedNodes ();
        TreePath tp = tree.getPathForLocation (dtde.getLocation ().x, dtde.getLocation ().y);
        if (pointAt!=DragDropUtilities.NODE_CENTRAL) {
            dropNode = dropNode.getParentNode();
        }
        
        if (!canDrop (dropNode, dtde.getDropAction ())) {
            if (canReorder (dropNode, dragNodes)) {
                performReorder (dropNode, dragNodes, lowerNodeIdx, upperNodeIdx);
                dtde.acceptDrop (dtde.getDropAction ());
            } else {
                dtde.rejectDrop ();
            }
            dtde.dropComplete (true);
            return;
        }

        // finally perform the drop
        int dropAction = dtde.getDropAction ();
        dtde.acceptDrop (dropAction);

        if (DnDConstants.ACTION_LINK == dropAction) {
            // construct all paste types
            PasteType[] ptCut = new PasteType[] {}, ptCopy = new PasteType[] {};
            // do not try get paste types for move if MOVE is not allowed
            if (ExplorerDnDManager.getDefault ().getAllowedDragActions ()==DnDConstants.ACTION_MOVE) {
                ptCut = DragDropUtilities.getPasteTypes (dropNode,
                        ExplorerDnDManager.getDefault ().getDraggedTransferable (true));
            }
            // do not try get paste types for copy if COPY is not allowed
            if (ExplorerDnDManager.getDefault ().getAllowedDragActions ()>=DnDConstants.ACTION_COPY) {
                ptCopy = DragDropUtilities.getPasteTypes (dropNode,
                        ExplorerDnDManager.getDefault ().getDraggedTransferable (false));
            }
            TreeSet setPasteTypes = new TreeSet (new Comparator () {
                public int compare (Object obj1, Object obj2) {
                    return ((PasteType)obj1).getName ().compareTo (((PasteType)obj2).getName ());
                    
                   // have to fix: the different actions can have same name!!!
                    /*int res = ((PasteType)obj1).getName ().compareTo (((PasteType)obj2).getName ());
                    System.out.println("res1: "+res);
                    if (res == 0) {
                        res = System.identityHashCode(obj1)-System.identityHashCode(obj2);
                    }
                    System.out.println("res2: "+res);
                    return res;*/
                }}
            );
            for (int i=0; i<ptCut.length; i++) {
                //System.out.println(ptCut[i].getName()+", "+System.identityHashCode(ptCut[i]));
                setPasteTypes.add (ptCut[i]);
            }
            for (int i=0; i<ptCopy.length; i++) {
                //System.out.println(ptCopy[i].getName()+", "+System.identityHashCode(ptCopy[i]));
                setPasteTypes.add (ptCopy[i]);
            }
            DragDropUtilities.createDropFinishPopup (setPasteTypes).show(tree,Math.max(dtde.getLocation ().x-5,0),Math.max(dtde.getLocation ().y-5,0));
            
            // reorder have to be perform
            if (canReorder (dropNode, dragNodes)) {
                final Node tempDropNode = dropNode;
                final int tmpUpper = upperNodeIdx;
                final int tmpLower = lowerNodeIdx;
                final Node[] tempDragNodes = dragNodes;
                DragDropUtilities.setPostDropRun (new Runnable () {
                    public void run () {
                        performReorder (tempDropNode,
                            findDropedNodes (tempDropNode, tempDragNodes), tmpLower, tmpUpper);
                    }
                });
            }
        } else {
            // get correct paste type
            PasteType[] pt = DragDropUtilities.getPasteTypes (dropNode,
                    ExplorerDnDManager.getDefault ().getDraggedTransferable (DnDConstants.ACTION_MOVE==dropAction));
            /*// help loop for all paste actions
            System.out.println("PASTE TYPES FOR "+dropAction);
            for (int i=0; i<pt.length; i++) {
                System.out.println(i+". "+pt [i].getName ());
            }*/
            DragDropUtilities.performDrop (pt[0]);
            
            // check canReorder or optionally perform it
            // before find new nodes in dropNode
            if (canReorder (dropNode, findDropedNodes (dropNode, dragNodes))) {
                performReorder (dropNode, findDropedNodes (dropNode, dragNodes), lowerNodeIdx, upperNodeIdx);
            }

        }
        
        TreeCellEditor tce = tree.getCellEditor ();
        if (tce instanceof TreeViewCellEditor)
            ((TreeViewCellEditor)tce).setDnDActive (false);
        
        // finished
        dtde.dropComplete (true);
   }
   
    /** Activates or deactivates Drag support on asociated JTree
    * component
    * @param active true if the support should be active, false
    * otherwise
    */
    public void activate (boolean active) {
        if (this.active == active)
            return;
        this.active = active;
        getDropTarget().setActive(active);
    }

    /** Implementation of the runnable interface.
    * Notifies user in AWT thread. */
    public void run () {
	if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater (this);
	    return;
	}
        DragDropUtilities.dropNotSuccesfull();
    }

    /** @return The tree path to the node the cursor is above now or
    * null if no such node currently exists or if conditions were not
    * satisfied to continue with DnD operation.
    */    
    TreePath getTreePath (DropTargetDragEvent dtde) {
        int dropAction = dtde.getDropAction();
        // check location
        Point location = dtde.getLocation();
        TreePath tp = tree.getPathForLocation(location.x, location.y);
        
        return tp != null && DragDropUtilities.secureFindNode(tp.getLastPathComponent())!=null ? tp : null;
    }

    /** Safe accessor to the drop target which is asociated
    * with the tree */
    DropTarget getDropTarget () {
        if (dropTarget == null) {
            dropTarget =
                new DropTarget(tree, view.getAllowedDropActions(),
                               this, false);
        }
        return dropTarget;
    }

} /* end class TreeViewDropSupport */
