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

import java.util.TooManyListenersException;
import java.awt.dnd.*;
import java.awt.Point;
import java.awt.Dialog;
import java.awt.datatransfer.Transferable;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.tree.*;

import org.openide.nodes.Node;
import org.openide.ErrorManager;
import org.openide.util.Utilities;

/** Support for the drag operations in explorer.
*
* @author Jiri Rechtacek
*/
abstract class ExplorerDragSupport implements DragSourceListener, DragGestureListener {
    // Attributes

    /** True when we are active, false otherwise */
    boolean active = false;

    /** Recognizes default gesture */
    DragGestureRecognizer defaultGesture;

    /** The component which we are supporting (our client) */
    protected JComponent comp;
    
    ExplorerDnDManager exDnD = ExplorerDnDManager.getDefault ();

    /** Initiating the drag */
    public void dragGestureRecognized (DragGestureEvent dge) {
        // 1. get seleced dragged nodes
        Node[] nodes = obtainNodes(dge);

        // check nodes
        if ((nodes == null) || (nodes.length == 0)) {
            return;
        }
        
        
        // 2. detect highest common action
        int possibleAction = DnDConstants.ACTION_MOVE;
        for (int i = 0; i < nodes.length; i++) {
            if  (possibleAction == DnDConstants.ACTION_MOVE) {
                if (!nodes[i].canCut ()) {
                    possibleAction = DnDConstants.ACTION_COPY;
                }
            }
            if  (possibleAction == DnDConstants.ACTION_COPY) {
                if (!nodes[i].canCopy ()) {
                    possibleAction = DnDConstants.ACTION_NONE;
                }
            }
        }
        exDnD = ExplorerDnDManager.getDefault ();
        exDnD.setAllowedDragActions (possibleAction);
    
        int dragAction = dge.getDragAction();
        boolean dragStatus = canDrag (dragAction, possibleAction);
        
        // 3. get transferable and start the drag
        try {
            // for MOVE
            Transferable transferable;
            if (possibleAction == DnDConstants.ACTION_MOVE) {
                // for MOVE
                transferable =
                    DragDropUtilities.getNodeTransferable(nodes, DnDConstants.ACTION_MOVE);
                exDnD.setDraggedTransferable (transferable, true);
                // for COPY too
                transferable =
                    DragDropUtilities.getNodeTransferable(nodes, DnDConstants.ACTION_COPY);
                exDnD.setDraggedTransferable (transferable, false);
            } else if (possibleAction == DnDConstants.ACTION_COPY) {
                // for COPY
                transferable =
                    DragDropUtilities.getNodeTransferable(nodes, DnDConstants.ACTION_COPY);
                exDnD.setDraggedTransferable (transferable, false);
            } else {
                // transferable for NONE
                transferable = Node.EMPTY.drag ();
                exDnD.setDraggedTransferable (transferable, false);
            }
            
            //System.out.println("Transferable: " + transferable); // NOI18N
            exDnD.setDraggedNodes (nodes);
            Dialog d = (Dialog)SwingUtilities.getAncestorOfClass (Dialog.class, comp);
            if (d != null && d.isModal ()) {
                exDnD.setDnDActive (false);
                return ;
            } else {
                exDnD.setDnDActive (true);
                dge.startDrag(
                    DragDropUtilities.chooseCursor (dragAction, dragStatus),
                    Utilities.loadImage(
                        "org/openide/resources/cursorscopysingle.gif"), // NOI18N
                    new Point (16, 16),
                    transferable,
                    this
                );
            }
            
        } catch (InvalidDnDOperationException exc) {
            // cannot start the drag, notify as informational
            ErrorManager em = ErrorManager.getDefault ();
            em.notify (ErrorManager.INFORMATIONAL, exc);
            exDnD.setDnDActive (false);
        } catch (IOException exc) {
            // cannot start the drag, notify user
            ErrorManager.getDefault ().notify (exc);
            exDnD.setDnDActive (false);
        }
    }
    
    private boolean canDrag (int targetAction, int possibleAction) {
        switch (possibleAction) {
            case DnDConstants.ACTION_MOVE:
                return true;
            case DnDConstants.ACTION_COPY:
            case DnDConstants.ACTION_COPY_OR_MOVE:
            case DnDConstants.ACTION_LINK:
                return (targetAction!=DnDConstants.ACTION_MOVE);
        default:
            return (possibleAction!=DnDConstants.ACTION_NONE);
        }
    }
    
    public void dragEnter (DragSourceDragEvent dsde) {
        dsde.getDragSourceContext().setCursor(DragDropUtilities.chooseCursor(
            dsde.getDropAction (),
            canDrag(dsde.getUserAction (), exDnD.getAllowedDragActions ())));
    }

    public void dragOver (DragSourceDragEvent dsde) {
        dsde.getDragSourceContext().setCursor(DragDropUtilities.chooseCursor(
            dsde.getDropAction (),
            canDrag(dsde.getUserAction (), exDnD.getAllowedDragActions ())));
    }

    public void dropActionChanged (DragSourceDragEvent dsde) {
    }

    public void dragExit (DragSourceEvent dse) {
        dse.getDragSourceContext().setCursor(
            DragDropUtilities.chooseCursor(
                dse.getDragSourceContext ().getSourceActions (),
                false));
    }

    public void dragDropEnd (DragSourceDropEvent dsde) {
        // not transferable for MOVE nor COPY
        exDnD.setDraggedTransferable (null, true);
        exDnD.setDraggedTransferable (null, false);
        // no nodes are dragged
        exDnD.setDraggedNodes (null);
        // no drop candidate
        NodeRenderer.dragExit ();
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
        DragGestureRecognizer dgr = getDefaultGestureRecognizer();
        if (active) {
            dgr.setSourceActions (exDnD.getSupportedDragActions ());
            try {
                dgr.removeDragGestureListener(this);
                dgr.addDragGestureListener(this);
            } catch (TooManyListenersException exc) {
                throw new IllegalStateException("Too many listeners for drag gesture."); // NOI18N
            }
        } else {
            dgr.removeDragGestureListener(this);
        }
    }

    /** Safe getter for default gesture<br>
    * (creates the gesture when called for the first time)
    */
    DragGestureRecognizer getDefaultGestureRecognizer () {
        if (defaultGesture == null) {
            DragSource ds = DragSource.getDefaultDragSource();
            defaultGesture = ds.createDefaultDragGestureRecognizer(
                                 comp, exDnD.getSupportedDragActions (), this);
        }
        return defaultGesture;
    }

    /** Utility method. Returns either selected nodes in tree
    * (if cursor hotspot is above some selected node) or the node
    * the cursor points to.
    * @return Node array or null if position of the cursor points
    * to no node.
    */
    abstract Node[] obtainNodes (DragGestureEvent dge);

} /* end class ExplorerDragSupport */
