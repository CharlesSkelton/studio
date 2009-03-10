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

package org.openide.nodes;

import java.util.Arrays;
import java.util.HashSet;

/** Event describing change in the list of a node's children.
*
* @author Jaroslav Tulach
*/
public class NodeMemberEvent extends NodeEvent {
    /** is this add event? */
    private boolean add;
    /** list of changed nodes */
    private Node[] delta;
    /** list of nodes to find indices in, null if one should use current
    * node's list
    */
    private Node[] from;
    /** list of nodes indexes, can be null if it should be computed lazily */
    private int[] indices;

    static final long serialVersionUID =-3973509253579305102L;
    /** Package private constructor to allow construction only
    * @param n node that should fire change
    * @param add true if nodes has been added
    * @param delta array of nodes that have changed
    * @param from nodes to find indices in
    */
    NodeMemberEvent(Node n, boolean add, Node[] delta, Node[] from) {
        super (n);
        this.add = add;
        this.delta = delta;
        this.from = from;
    }

    /** Get the type of action.
    * @return <CODE>true</CODE> if children were added,
    *    <CODE>false</CODE> if removed
    */
    public final boolean isAddEvent () {
        return add;
    }

    /** Get a list of children that changed.
    * @return array of nodes that changed
    */
    public final Node[] getDelta () {
        return delta;
    }

    /** Get an array of indices of the changed nodes.
    * @return array with the same length as {@link #getDelta}
    */
    public synchronized int[] getDeltaIndices () {
        if (indices != null) return indices;

        // compute indices
        if (from == null) {
            // use current node subnodes
            from = getNode ().getChildren ().getNodes ();
        }

        java.util.List list = Arrays.asList (delta);
        HashSet set = new HashSet (list);

        indices = new int[delta.length];

        int j = 0;
        int i = 0;
        while (i < from.length && j < indices.length) {
            if (set.contains (from[i])) {
                indices[j++] = i;
            }
            i++;
        }

        if (j != delta.length) {
            StringBuffer m = new StringBuffer(1000);
            m.append("Some of a set of deleted nodes are not present in the original ones.\n"); // NOI18N
            m.append("See #15478; you may need to check that your Children.Keys keys are safely comparable."); // NOI18N
            m.append("\ni: " + i);  // NOI18N
            m.append("\nj: " + j);  // NOI18N
            m.append("\nThis: " + this); // NOI18N
            m.append("\nCurrent state:\n"); // NOI18N
            m.append(Arrays.asList (from));
            m.append("\nDelta:\n"); // NOI18N
            m.append(list);
            throw new IllegalStateException(m.toString());
        }

        return indices;
    }

    /** Human presentable information about the event */
    public String toString () {
        StringBuffer sb = new StringBuffer ();
        sb.append (getClass ().getName ());
        sb.append ("[node="); // NOI18N
        sb.append (getSource ());
        sb.append (", add="); // NOI18N
        sb.append (isAddEvent ());

        Node[] deltaNodes = getDelta ();
        int[] deltaIndices = getDeltaIndices ();

        for (int i = 0; i < deltaNodes.length; i++) {
            sb.append ("\n  "); // NOI18N
            sb.append (i);
            sb.append (" at "); // NOI18N
            sb.append (deltaIndices[i]);
            sb.append (" = "); // NOI18N
            sb.append (deltaNodes[i]);
        }
        sb.append ("\n]"); // NOI18N

        return sb.toString ();
    }
}
