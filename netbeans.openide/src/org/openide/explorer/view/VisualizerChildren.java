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

import java.util.*;

import org.openide.nodes.*;

/** List of Visualizers. This is holded by parent visualizer by a
* weak reference, 
*
* @author Jaroslav Tulach
*/
final class VisualizerChildren extends Object {
    /** parent visualizer */
    public final VisualizerNode parent;
    /** list of all objects here (VisualizerNode) */
    public final List list = new LinkedList ();

    /** Creates new VisualizerChildren.
    * Can be called only from EventQueue.
    */
    public VisualizerChildren (VisualizerNode parent, Node[] nodes) {
        this.parent = parent;
        int s = nodes.length;
        for (int i = 0; i < s; i++) {
            VisualizerNode v = VisualizerNode.getVisualizer (this, nodes[i]);
            list.add (v);
        }
    }

    /** Notification of children addded event. Modifies the list of nodes
    * and fires info to all listeners.
    */
    public void added (VisualizerEvent.Added ev) {
        ListIterator it = list.listIterator ();
        boolean empty = !it.hasNext ();

        int[] indxs = ev.getArray ();
        Node[] nodes = ev.getAdded ();

        int current = 0;
        int inIndxs = 0;
        while (inIndxs < indxs.length) {
            while (current++ < indxs[inIndxs]) {
                it.next ();
            }
            it.add (VisualizerNode.getVisualizer (this, nodes[inIndxs]));
            inIndxs++;
        }


        VisualizerNode parent = this.parent;
        while (parent != null) {
            Object[] listeners = parent.getListenerList ();
            for (int i = listeners.length - 1; i >= 0; i -= 2) {
                ((NodeModel)listeners[i]).added (ev);
            }
            parent = (VisualizerNode)parent.getParent ();
        }

        if (empty) {
            // change of state
            this.parent.notifyVisualizerChildrenChange (list.size (), this);
        }

    }

    /** Notification that children has been removed. Modifies the list of nodes
    * and fires info to all listeners.
    */
    public void removed (VisualizerEvent.Removed ev) {
        List remList = Arrays.asList (ev.getRemovedNodes ());
        
        Iterator it = list.iterator ();
        
        VisualizerNode vis;
        
        int[] indx = new int[remList.size ()];
        int count = 0, remSize = 0;
        while (it.hasNext ()) {
            // take visualizer node
            vis = (VisualizerNode)it.next ();
            
            // check if it will removed
            if (remList.contains (vis.node)) {
                indx[remSize++] = count;

                // remove this VisualizerNode from children
                it.remove ();
            }
            count++;
        }
        
        // notify event about changed indexes
        ev.setRemovedIndicies (indx);
        
        VisualizerNode parent = this.parent;
        while (parent != null) {
            Object[] listeners = parent.getListenerList ();
            for (int i = listeners.length - 1; i >= 0; i -= 2) {
                ((NodeModel)listeners[i]).removed (ev);
            }
            parent = (VisualizerNode)parent.getParent ();
        }

        if (list.isEmpty ()) {
            // now is empty
            this.parent.notifyVisualizerChildrenChange (0, this);
        }
    }
    
    /** Notification that children has been reordered. Modifies the list of nodes
    * and fires info to all listeners.
    */
    public void reordered (VisualizerEvent.Reordered ev) {
        int[] indxs = ev.getArray ();
        Object[] old = list.toArray ();
        Object[] arr = new Object[old.length];


        int s = indxs.length;
        try {
            for (int i = 0; i < s; i++) {
                // arr[indxs[i]] = old[i];
                Object old_i = old[i];
                int indxs_i = indxs[i];
                if (arr[indxs_i] != null) {
                    // this is bad <-- we are rewriting some old value --> there will remain some null somewhere
                    System.err.println("Writing to this index for the second time: " + indxs_i); // NOI18N
                    System.err.println("Length of indxs array: " + indxs.length); // NOI18N
                    System.err.println("Length of actual array: " + old.length); // NOI18N
                    System.err.println("Indices of reorder event:"); // NOI18N
                    for (int j = 0; i < indxs.length; j++)
                        System.err.println("\t" + indxs[j]); // NOI18N
                    Thread.dumpStack();
                    return;
                }
                arr[indxs_i] = old_i;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.err.println("Length of actual array: " + old.length); // NOI18N
            System.err.println("Indices of reorder event:"); // NOI18N
            for (int i = 0; i < indxs.length; i++)
                System.err.println("\t" + indxs[i]); // NOI18N
            return;
        }

        list.clear ();
        list.addAll (Arrays.asList (arr));

        VisualizerNode parent = this.parent;
        while (parent != null) {
            Object[] listeners = parent.getListenerList ();
            for (int i = listeners.length - 1; i >= 0; i -= 2) {
                ((NodeModel)listeners[i]).reordered (ev);
            }
            parent = (VisualizerNode)parent.getParent ();
        }
    }

}
