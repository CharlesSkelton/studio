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


import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants; // TEMP
import java.util.Iterator;
import javax.swing.JScrollPane;

import org.openide.nodes.Node;
import org.openide.util.WeakSet;

/**
 * Manager for explorer DnD. 
 *
 *
 * @author  Jiri Rechtacek
 *
 * @see TreeViewDragSupport
 * @see TreeViewDropSupport
 */
final class ExplorerDnDManager {

    /** Singleton instance of explorer dnd manager. */
    private static ExplorerDnDManager defaultDnDManager;

    /** Creates a new instance of <code>WindowsDnDManager</code>. */
    private ExplorerDnDManager () {
    }
    
    private Node[] draggedNodes;
    
    private Transferable draggedTransForCut;
    private Transferable draggedTransForCopy;
    
    private boolean isDnDActive = false;
    
    private int allowedActions = 0;

    private int supportedActions = DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY |
               DnDConstants.ACTION_LINK;
    
    private transient WeakSet setOfTargets;
    
    /** Gets the singleton instance of this window dnd manager. */
    static synchronized ExplorerDnDManager getDefault () {
        if (defaultDnDManager == null) {
            defaultDnDManager = new ExplorerDnDManager ();
        }
        
        return defaultDnDManager;
    }
    
    void setDraggedNodes (Node[] n) {
        draggedNodes = n;
    }
    
    Node[] getDraggedNodes () {
        return draggedNodes;
    }
    
    void setDraggedTransferable (Transferable trans, boolean isCut) {
        if (isCut) {
            draggedTransForCut = trans;
        } else {
            draggedTransForCopy = trans;
        }
    }
    
    Transferable getDraggedTransferable (boolean isCut) {
        if (isCut) {
            return draggedTransForCut;
        }
        // only for copy
        return draggedTransForCopy;
        
    }

    void setAllowedDragActions (int actions) {
        allowedActions= actions;
    }

    int getAllowedDragActions () {
        if (allowedActions==0) {
            return DnDConstants.ACTION_NONE;
        }
        return allowedActions;
    }
    
    void setSupportedDragActions (int actions) {
        supportedActions= actions;
    }

    int getSupportedDragActions () {
        if (supportedActions==0) {
            return DnDConstants.ACTION_NONE;
        }
        return supportedActions;
    }

    void setDnDActive (boolean state) {
        isDnDActive = state;
        if (setOfTargets != null && !setOfTargets.isEmpty ()) {
            Iterator it = setOfTargets.iterator();
            while (it.hasNext ()) {
                JScrollPane pane = (JScrollPane)it.next ();
                if (pane.isEnabled ()) {
                    if (pane instanceof TreeView) {
                        ((TreeView)pane).setDropTarget (state);
                    } else if (pane instanceof ListView) {
                        ((ListView)pane).setDropTarget (state);
                    }
                }
            }
        }
    }
    
    boolean isDnDActive () {
        return isDnDActive;
    }
    
    void addFutureDropTarget (JScrollPane view) {
        if (setOfTargets == null)
            setOfTargets = new WeakSet ();
        setOfTargets.add (view);
    }
}
