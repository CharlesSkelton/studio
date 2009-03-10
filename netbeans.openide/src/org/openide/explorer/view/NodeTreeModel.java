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

import java.beans.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;

import org.openide.util.*;
import org.openide.nodes.Node;

/** Model for displaying the nodes in tree.
*
* @author Jaroslav Tulach
*/
public class NodeTreeModel extends DefaultTreeModel {
    /** listener used to listen to changes in trees */
    private transient Listener listener;

    static final long serialVersionUID =1900670294524747212L;
    /** Creates new NodeTreeModel
    */
    public NodeTreeModel () {
        super (VisualizerNode.EMPTY, true);
    }

    /** Creates new NodeTreeModel
    * @param root the root of the model
    */
    public NodeTreeModel (Node root) {
        super (VisualizerNode.EMPTY, true);
        setNode (root);
    }

    /** Changes the root of the model. This is thread safe method.
    * @param root the root of the model
    */
    public void setNode (final Node root) {
        Mutex.EVENT.readAccess (new Runnable () {
                                    public void run () {
                                        VisualizerNode v = (VisualizerNode)getRoot ();
                                        VisualizerNode nr = VisualizerNode.getVisualizer (null, root);

                                        if (v == nr) {
                                            // no change
                                            return;
                                        }

                                        v.removeNodeModel (listener ());

                                        nr.addNodeModel (listener ());
                                        setRoot (nr);
                                    }
                                });
    }

    /** Getter for the listener. Only from AWT-QUEUE.
    */
    private Listener listener () {
        if (listener == null) {
            listener = new Listener (this);
        }
        return listener;
    }

    /**
    * This sets the user object of the TreeNode identified by path
    * and posts a node changed.  If you use custom user objects in
    * the TreeModel you'returngoing to need to subclass this and
    * set the user object of the changed node to something meaningful.
    */
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (path == null)
            return;
        
        Object o = path.getLastPathComponent();
        if (o instanceof VisualizerNode) {
            nodeChanged ((VisualizerNode)o);
            return;
        }
        MutableTreeNode   aNode = (MutableTreeNode)o;

        aNode.setUserObject(newValue);
        nodeChanged(aNode);
    }


    /** The listener */
    private static final class Listener implements NodeModel {
        /** weak reference to the model */
        private Reference model;

        /** Constructor.
        */
        public Listener (NodeTreeModel m) {
            model = new WeakReference (m);
        }

        /** Getter for the model or null.
        */
        private NodeTreeModel get (VisualizerEvent ev) {
            NodeTreeModel m = (NodeTreeModel)model.get ();
            if (m == null && ev != null) {
                ev.getVisualizer ().removeNodeModel (this);
                return null;
            }
            return m;
        }

        /** Notification of children addded event. Modifies the list of nodes
        * and fires info to all listeners.
        */
        public void added (VisualizerEvent.Added ev) {
            NodeTreeModel m = get (ev);
            if (m == null) return;

            m.nodesWereInserted (ev.getVisualizer (), ev.getArray ());
        }

        /** Notification that children has been removed. Modifies the list of nodes
        * and fires info to all listeners.
        */
        public void removed (VisualizerEvent.Removed ev) {
            NodeTreeModel m = get (ev);
            if (m == null) return;

            m.nodesWereRemoved (ev.getVisualizer (), ev.getArray (), ev.removed.toArray ());
        }

        /** Notification that children has been reordered. Modifies the list of nodes
        * and fires info to all listeners.
        */
        public void reordered (VisualizerEvent.Reordered ev) {
            NodeTreeModel m = get (ev);
            if (m == null) return;
            
            m.nodeStructureChanged (ev.getVisualizer ());            
        }

        /** Update a visualizer (change of name, icon, description, etc.)
        */
        public void update(VisualizerNode v) {
            NodeTreeModel m = get (null);
            if (m == null) return;
            m.nodeChanged (v);
        }
        
        /** Notification about large change in the sub tree
         */
        public void structuralChange(VisualizerNode v) {
            NodeTreeModel m = get (null);
            if (m == null) return;
            m.nodeStructureChanged(v);
        }        
        
    }
}
