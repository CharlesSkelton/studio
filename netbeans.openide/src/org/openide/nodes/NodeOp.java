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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Enumeration;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.openide.ErrorManager;
import org.openide.util.enums.ArrayEnumeration;
import org.openide.util.Lookup;

/** Utility class for operations on nodes.
 *
 * @author Jaroslav Tulach, Petr Hamernik, Dafe Simonek
 */
public final class NodeOp extends Object {
    private NodeOp() {}
    
    /** default node actions */
    private static org.openide.util.actions.SystemAction[] defaultActions;
    
    /** Get the default actions for all nodes.
    * @return array of default actions
     * @deprecated Do not use this method. It is useless now.
    */
    public static org.openide.util.actions.SystemAction[] getDefaultActions () {
        if (defaultActions == null) {
            defaultActions = createFromNames (new String [] {
                "Tools", "Properties" // NOI18N 
            });
        }
        return defaultActions;
    }
    
    /** @deprecated Useless. */
    public static void setDefaultActions (org.openide.util.actions.SystemAction[] def) {
        throw new SecurityException ();
    }

    /** Compute common menu for specified nodes.
    * Provides only those actions supplied by all nodes in the list.
    * @param nodes the nodes
    * @return the menu for all nodes
    */
    public static JPopupMenu findContextMenu (Node[] nodes) {
        return findContextMenuImpl (nodes, null);
    }
    
    /** Method for finding popup menu for one or more nodes.
    *
    * @param nodes array of nodes
    * @param actionMap maps keys to actions or null
    * @return popup menu for this array
    */
    static JPopupMenu findContextMenuImpl (Node[] nodes, javax.swing.ActionMap actionMap) {
        Action[] arr = findActions (nodes);
        
        // prepare lookup representing all the selected nodes
        ArrayList allLookups = new ArrayList ();
        for (int i = 0; i < nodes.length; i++) {
            allLookups.add (nodes[i].getLookup ());
        }

        if (actionMap != null) {
            allLookups.add (org.openide.util.lookup.Lookups.singleton(actionMap));
        }

        Lookup lookup = new org.openide.util.lookup.ProxyLookup (
            (Lookup[])allLookups.toArray (new Lookup[0])
        );
        
        return org.openide.util.Utilities.actionsToPopup(arr, lookup);
    }
    
    /** Asks the provided nodes for their actions and those that are common,
     * to all of them returns.
     *
     * @param nodes array of nodes to compose actions for
     * @return array of actions for the nodes or empty array if no actions
     *   were found
     * @since 3.29
     */
    public static Action[] findActions (Node[] nodes) {
        // hashtable: SystemAction -> Integer
        HashMap actions = new HashMap ();
        
        // counts the number of occurences for each action
        for (int n = 0; n < nodes.length; n++) {
            Action[] arr = nodes[n].getActions (false);
            if (arr == null) {
                // use default actions
                arr = defaultActions;
            }

            // keeps actions handled for this node iteration
            HashSet counted = new HashSet ();
            
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null) {
                    // if this action was handled for this node already, skip to next iteration
                    if (counted.contains (arr[i]))
                        continue;
                    
                    counted.add (arr[i]);
                    
                    Integer cntInt = (Integer)actions.get (arr[i]);
                    int cnt = cntInt == null ? 0 : cntInt.intValue ();
                    actions.put (arr[i], new Integer (cnt + 1));
                }
            }
        }
        
        // take all actions that are nodes.length number times
        if (!actions.isEmpty ()) {
            Action[] arr = nodes[0].getActions (false);
            if (arr == null) {
                // use default
                arr = defaultActions;
            }
            
            // keeps actions for which was menu item created already
            ArrayList result = new ArrayList ();
            HashSet counted = new HashSet ();
            for (int i = 0; i < arr.length; i++) {
                Action action = arr[i];
                
                if (action != null) {
                    // if this action has menu item already, skip to next iteration
                    if (counted.contains (action))
                        continue;
                    
                    counted.add (action);
                    Integer cntInt = (Integer)actions.get (action);
                    
                    int cnt = cntInt == null ? 0 : cntInt.intValue ();
                    if (cnt == nodes.length) {
                        result.add (action);
                    }
                } else {
                    // place a separator there
                    result.add (null);
                }
            }
            
            return (Action[])result.toArray(new Action[0]);
        } else {
            // no available actions
            return new Action[0];
        }
    }
    
    /** Test whether the second node is a (direct) child of the first one.
    * @param parent parent node
    * @param son son node
    * @return <code>true</code> if so
    */
    public static boolean isSon (Node parent, Node son) {
        return son.getParentNode () == parent;
    }
    
    /** Find a path (by name) from one node to the root or a parent.
     * @param node the node to start in
     * @param parent parent node to stop in (can be <code>null</code> for the root)
     * @return list of child names--i.e. a path from the parent to the child node
     * @exception IllegalArgumentException if <code>node</code>'s getName()
     * method returns <code>null</code>
     */
    public static String[] createPath (Node node, Node parent) {
        LinkedList ar = new LinkedList ();
        
        while (node != null && node != parent) {
            if (node.getName() == null) {
                boolean isFilter = false;
                
                if(node instanceof FilterNode) {
                    isFilter = true;
                }
                
                throw new IllegalArgumentException("Node:" + node.getClass() // NOI18N
                    + "[" + node.getDisplayName() +"]" // NOI18N
                    + (isFilter ? (" of original:" + ((FilterNode)node).getOriginal().getClass()) : "") // NOI18N
                    + " gets null name!"); // NOI18N
            }
                
            ar.addFirst (node.getName ());
            node = node.getParentNode ();
        }
        
        String[] res = new String [ar.size ()];
        ar.toArray (res);
        return res;
    }
    
    /** Look for a node child of given name.
    * @param node node to search in
    * @param name name of child to look for
    * @return the found child, or <code>null</code> if there is no such child
    */
    public static Node findChild (Node node, String name) {
        return node.getChildren ().findChild (name);
    }
    
    /** Traverse a path from a parent node down, by an enumeration of names.
    * @param start node to start searching at
    * @param names enumeration of <code>String</code>s containing names of nodes
    *   along the path
    * @return the node with such a path from the start node
    * @exception NodeNotFoundException if the node with such name
    *   does not exists; the exception contains additional information
    *   about the failure.
    */
    public static Node findPath (Node start, Enumeration names)
    throws NodeNotFoundException {
        int depth = 0;
        
        while (names.hasMoreElements ()) {
            String name = (String)names.nextElement ();
            Node next = findChild (start, name);
            if (next == null) {
                // no element in list matched the name => fail
                // fire exception with the last accessed node and the
                // name of child that does not exists
                throw new NodeNotFoundException (start, name, depth);
            } else {
                // go on next node
                start = next;
            }
            
            // continue on next depth
            depth++;
        }
        return start;
    }
    
   /** Traverse a path from a parent node down, by an enumeration of names.
    * @param start node to start searching at
    * @param names names of nodes
    *   along the path
    * @return the node with such a path from the start node
    * @exception NodeNotFoundException if the node with such name
    *   does not exists; the exception contains additional information
    *   about the failure.
    */
    public static Node findPath (Node start, String[] names)
    throws NodeNotFoundException {
        return findPath (start, new ArrayEnumeration (names));
    }
    
    /** Find the root for a given node.
    * @param node the node
    * @return its root
    */
    public static Node findRoot (Node node) {
        for (;;) {
            Node parent = node.getParentNode ();
            if (parent == null) return node;
            node = parent;
        }
    }
    
    
    /** Compute a permutation between two arrays of nodes. The arrays
    * must have the same size. The permutation then can be
    * applied to the first array to create the
    * second array.
    *
    * @param arr1 first array
    * @param arr2 second array
    * @return the permutation, or <code>null</code> if the arrays are the same
    * @exception IllegalArgumentException if the arrays cannot be permuted to each other. Either
    *    they have different sizes or they do not contain the same elements.
    */
    public static int[] computePermutation (Node[] arr1, Node[] arr2)
    throws IllegalArgumentException {
        if (arr1.length != arr2.length) {
            int max = Math.max (arr1.length, arr2.length);
            StringBuffer sb = new StringBuffer ();
            for (int i = 0; i < max; i++) {
                sb.append (i + " "); // NOI18N
                if (i < arr1.length) {
                    sb.append (arr1[i].getName ());
                } else {
                    sb.append ("---"); // NOI18N
                }
                sb.append (" = "); // NOI18N
                if (i < arr2.length) {
                    sb.append (arr2[i].getName ());
                } else {
                    sb.append ("---"); // NOI18N
                }
                sb.append ('\n');
            }
            throw new IllegalArgumentException (sb.toString ());
        }
        
        // creates map that assignes to nodes their original
        // position
        HashMap map = new HashMap ();
        for (int i = 0; i < arr2.length; i++) {
            map.put (arr2[i], new Integer (i));
        }
        // takes nodes one by one in the new order and
        // creates permutation array
        int[] perm = new int[arr1.length];
        int diff = 0;
        
        for (int i = 0; i < arr1.length; i++) {
            // get the position of the i-th argument in the second array
            Integer newPos = (Integer)map.get (arr1[i]);
            if (newPos == null) {
                // not permutation i-th element is missing in the array
                throw new IllegalArgumentException ("Missing permutation index " + i); // NOI18N
            }
            // perm must move the object to the newPos
            perm[i] = newPos.intValue ();
            
            if (perm[i] != i) {
                diff++;
            }
        }
        return diff == 0 ? null : perm;
    }
    
    /** Takes array of nodes and creates array of handles. The nodes that do not
    * have handles are not included in the resulting array.
    *
    * @param nodes array of nodes
    * @return array of Node.Handles
    */
    public static Node.Handle[] toHandles (Node[] nodes) {
        LinkedList ll = new LinkedList ();
        
        for (int i = 0; i < nodes.length; i++) {
            Node.Handle h = nodes[i].getHandle();
            if (h != null) {
                ll.add (h);
            }
        }
        
        return (Node.Handle[])ll.toArray (new Node.Handle[ll.size ()]);
    }
    
    /** Takes array of handles and creates array of nodes.
    * @param handles array of handles
    * @return array of nodes
    * @exception IOException if a node cannot be created from the handle
    */
    public static Node[] fromHandles (Node.Handle[] handles)
    throws java.io.IOException {
        Node[] arr = new Node[handles.length];
        for (int i = 0; i < handles.length; i++) {
            arr[i] = handles[i].getNode ();
        }
        
        return arr;
    }

    
    /** Utility method to remove dependency of this package on 
     * org.openide.actions. This method takes names of classes from
     * that package and creates their instances.
     *
     * @param arr the array of names like "Tools", "Properties", etc. can
     *   contain nulls
     */
    static org.openide.util.actions.SystemAction[] createFromNames (String[] arr) {
        ErrorManager err = (ErrorManager)
            org.openide.util.Lookup.getDefault ().lookup (ErrorManager.class);
        
        LinkedList ll = new LinkedList ();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null) {
                ll.add (null);
                continue;
            }
            
            String name = "org.openide.actions." + arr[i] + "Action"; // NOI18N
            try {
                Class c = Class.forName (name);
                ll.add (org.openide.util.actions.SystemAction.get (c));
            } catch (ClassNotFoundException ex) {
                if (err != null) {
                    err.log (err.INFORMATIONAL, "NodeOp.java: Missing class " + name); // NOI18N
                }
                // otherwise it is probably ok, that the class is missing
            }
        }    
        
        return (org.openide.util.actions.SystemAction[])ll.toArray (new org.openide.util.actions.SystemAction[ll.size ()]);
    }
    
    
    /** Notifies an exception to error manager or prints its it to stderr.
     * @param ex exception to notify
     */
    static void exception (Throwable ex) {
        ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
        
    }

    /** Notifies an exception to error manager or prints its it to stderr.
     * @param ex exception to notify
     */
    static void warning (Throwable ex) {
        ErrorManager.getDefault ().notify (ErrorManager.WARNING, ex);
    }
}
