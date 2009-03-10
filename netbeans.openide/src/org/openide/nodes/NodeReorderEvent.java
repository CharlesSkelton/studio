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

/** Event describing change in the list of a node's children.
*
* @author Jaroslav Tulach
*/
public final class NodeReorderEvent extends NodeEvent {
    /** list of new nodes indexes on the original positions */
    private int[] newIndices;

    static final long serialVersionUID =4479234495493767448L;
    /** Package private constructor to allow construction only
    * @param node the node that has changed
    * @param newIndices new indexes of the nodes
    */
    NodeReorderEvent(Node n, int[] newIndices) {
        super (n);
        this.newIndices = newIndices;
    }

    /** Get the new position of the child that had been at a given position.
    * @param i the original position of the child
    * @return the new position of the child
    */
    public int newIndexOf (int i) {
        return newIndices[i];
    }

    /** Get the permutation used for reordering.
    * @return array of integers used for reordering
    */
    public int[] getPermutation () {
        return newIndices;
    }

    /** Get the number of children reordered.
     * @return size of the permutation array */
    public int getPermutationSize() {
        return newIndices.length;
    }

    /** Human presentable information about the event */
    public String toString () {
        StringBuffer sb = new StringBuffer ();
        sb.append (getClass ().getName ());
        sb.append ("[node="); // NOI18N
        sb.append (getSource ());
        sb.append (", permutation = ("); // NOI18N

        int[] perm = getPermutation ();

        for (int i = 0; i < perm.length; ) {
            sb.append (perm[i]);
            if (++i < perm.length) {
                sb.append (", "); // NOI18N
            }
        }
        sb.append (")]"); // NOI18N

        return sb.toString ();
    }

}
