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

package org.openide.nodes;

import java.util.*;

/** Holder of nodes for a children object. Communicates
* with children to notify when created/finalized.
*
* @author Jaroslav Tulach
*/
final class ChildrenArray extends NodeAdapter {
    /** children */
    public  Children children;
    /** nodes associated */
    private Node[] nodes;
    /** mapping from the (Children.Info, Collection (Node)) */
    private WeakHashMap map;

    /** Creates new ChildrenArray */
    public ChildrenArray () {
    }
    
    public Children getChildren () {
        return children;
    }

    /** When finalized notify the children.
    */
    protected void finalize () {
        children.finalizedChildrenArray ();
    }

    /** Getter method to receive a set of computed nodes.
    */
    public Node[] nodes () {
        if (children == null) {
            // not fully initialize
            return null;
        }

        if (nodes == null) {
            nodes = children.justComputeNodes ();
            for (int i = 0; i < nodes.length; i++) {
                // keeps a hard reference from the children node to this
                // so we can be GCed only when child nodes are gone
                nodes[i].reassignTo (children, this);
            }
            // if at least one node => be weak
            children.registerChildrenArray (this, nodes.length > 0);
        }

        return nodes;
    }

    /** Clears the array of nodes.
    */
    public void clear () {
        if (nodes != null) {
            nodes = null;
            // register in the childrens to be hold by hard reference
            // because we keep no reference to nodes, we can be
            // hard holded by children
            children.registerChildrenArray (this, false);
        }
    }

    /** Finalizes nodes by calling get on weak hash map,
    * all references stored in the map, that are finalized
    * will be cleared.
    */
    public void finalizeNodes () {
        WeakHashMap m = map;
        if (m != null) {
            // processes the queue of garbage
            // collected keys
            m.remove (null);
        }
    }

    /** Initilized if has some nodes.
    */
    public boolean isInitialized () {
        return nodes != null;
    }

    /** Gets the nodes for given info.
    * @param info the info
    * @return the nodes
    */
    public Collection nodesFor (Children.Info info) {
        if (map == null) {
            map = new WeakHashMap (7);
        }

        Collection nodes = (Collection)map.get (info);
        if (nodes == null) {
            nodes = info.entry.nodes ();
            info.length = nodes.size ();
            map.put (info, nodes);
        }
        return nodes;
    }

    /** Refreshes the nodes for given info.
    * @param info the info
    * @return the nodes
    */
    public void useNodes (Children.Info info, Collection list) {
        if (map == null) {
            map = new WeakHashMap (7);
        }

        info.length = list.size ();

        map.put (info, list);
    }
    
    
    public String toString() {
        return super.toString() + "  " + getChildren(); //NOI18N
    }

}
