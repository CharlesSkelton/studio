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

import java.util.LinkedList;
import java.util.EventObject;

import org.openide.nodes.*;

/** Event describing change in a visualizer. Runnable to be added into
* the event queue.
*
* @author Jaroslav Tulach
*/
abstract class VisualizerEvent extends EventObject {
    /** indicies */
    private int[] array;

    public VisualizerEvent (VisualizerChildren ch, int[] array) {
        super (ch);
        this.array = array;
    }

    /** Getter for changed indexes */
    public final int[] getArray () {
        return array;
    }

    /** Getter for the children list.
    */
    public final VisualizerChildren getChildren () {
        return (VisualizerChildren)getSource ();
    }

    /** Getter for the visualizer.
    */
    public final VisualizerNode getVisualizer () {
        return getChildren ().parent;
    }

    /** Class for notification of adding of nodes that can be passed into
    * the event queue and in such case notifies all listeners in Swing Dispatch Thread
    */
    static final class Added extends VisualizerEvent implements Runnable {
        /** array of newly added nodes */
        private Node[] added;

        static final long serialVersionUID =5906423476285962043L;
        /** Constructor for add of nodes notification.
        * @param ch children
        * @param n array of added nodes
        * @param indx indicies of added nodes
        */
        public Added (
            VisualizerChildren ch,
            Node[] n,
            int[] indx
        ) {
            super (ch, indx);
            added = n;
        }

        /** Getter for added nodes.
        */
        public Node[] getAdded () {
            return added;
        }

        /** Process the event
        */
        public void run() {
            super.getChildren ().added (this);
        }
    }

    /** Class for notification of removing of nodes that can be passed into
    * the event queue and in such case notifies all listeners in Swing Dispatch Thread
    */
    static final class Removed extends VisualizerEvent implements Runnable {
        /** linked list of removed nodes, that is filled in getChildren ().removed () method
        */
        public LinkedList removed = new LinkedList ();
        
        private Node[]removedNodes;

        static final long serialVersionUID =5102881916407672392L;
        /** Constructor for add of nodes notification.
        * @param ch children
        * @param n array of added nodes
        * @param indx indicies of added nodes
        */
        public Removed (
            VisualizerChildren ch,
            Node[] removedNodes
        ) {
            super (ch, null);
            this.removedNodes = removedNodes;
        }
        
        public Node[] getRemovedNodes () {
            return removedNodes;
        }
        
        public void setRemovedIndicies (int[] arr) {
            super.array = arr;
        }

        /** Process the event
        */
        public void run() {
            super.getChildren ().removed (this);
        }
    }

    /** Class for notification of reordering of nodes that can be passed into
    * the event queue and in such case notifies all listeners in Swing Dispatch Thread
    */
    static final class Reordered extends VisualizerEvent implements Runnable {
        /** indices */
        private int[] changedIndices;

        static final long serialVersionUID =-4572356079752325870L;
        /** Constructor for add of nodes notification.
        * @param ch children
        * @param n array of added nodes
        * @param indx indicies of added nodes
        */
        public Reordered (
            VisualizerChildren ch,
            int[] indx
        ) {
            super (ch, indx);
        }

//        /** Prepares list of changed indices for use in tree model.
//        */
//        public int[] getChangedIndices () {
//            if (changedIndices == null) {
//                int[] permutation = super.getArray ();
//                int size = permutation.length;
//                int changes = 0;
//                for (int i = 0; i < size; i++) {
//                    if (permutation[i] != i)
//                        changes++;
//                }
//
//                int[] indices = new int[changes];
//
//                int current = 0;
//                for (int i = 0; i < size; i++) {
//                    if (permutation[i] != i) {
//                        indices[current++] = i;
//                    }
//                }
//
//                changedIndices = indices;
//            }
//            return changedIndices;
//        }

        /** Process the event
        */
        public void run() {
            super.getChildren ().reordered (this);
        }
    }

}
