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

import java.beans.PropertyChangeEvent;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;

import org.openide.util.WeakSet;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.WeakListener;

import org.openide.nodes.*;

/** A lookup that represents content of a Node.getCookie and the node itself.
 * 
 *
 * @author  Jaroslav Tulach
 */
final class NodeLookup extends AbstractLookup 
implements NodeListener, Collection {
    /** instance content to add new objects into */
    private InstanceContent ic;
    /** Set of Classes that we have already queried <type>Class</type> */
    private WeakSet queriedCookieClasses = new WeakSet (37);
    
    /** node we are associated with
     */
    private Node node;
    
    /** New flat lookup.
     */
    public NodeLookup (Node n) {
        this (new InstanceContent (), n);
    }

    private NodeLookup (InstanceContent ic, Node n) {
        super (ic);

        this.ic = ic;
        this.node = n;
        this.ic.add (n);
        
        n.addNodeListener (WeakListener.node (this, n));
    }

    /** Notifies subclasses that a query is about to be processed.
     * @param template the template 
     */
    protected final void beforeLookup (Template template) {
        Class type = template.getType ();
        
        if (type == Object.class) {
            type = Node.Cookie.class;
        }

        if (Node.Cookie.class.isAssignableFrom (type)) {
            if (!queriedCookieClasses.contains (type)) {
                synchronized (this) {
                    queriedCookieClasses.add (type);
                }
                
                Object res = node.getCookie (type);
                if (res != null) {
                    ic.add (res);
                }
            }
        }
    }
        
        
    public void propertyChange(PropertyChangeEvent ev) {
        // a change happened in a node
        if (ev.getPropertyName () != Node.PROP_COOKIE) {
            return;
        }

        ArrayList instances = new ArrayList ();
        
        // if it is cookie change, do the rescan
        synchronized (this) {
            Iterator it = queriedCookieClasses.iterator();
            instances.add (node);
            while (it.hasNext()) {
                Class c = (Class)it.next ();
                Object res = node.getCookie (c);
                if (res != null) {
                    instances.add (res);
                }
            }
        }
        
        ic.set (instances, null);
    }
    
    /** Fired when the node is deleted.
     * @param ev event describing the node
     */
    public void nodeDestroyed(NodeEvent ev) {
    }
    
    /** Fired when a set of children is removed.
     * @param ev event describing the action
     */
    public void childrenRemoved(NodeMemberEvent ev) {
    }
    
    /** Fired when the order of children is changed.
     * @param ev event describing the change
     */
    public void childrenReordered(NodeReorderEvent ev) {
    }
    
    /** Fired when a set of new children is added.
     * @param ev event describing the action
     */
    public void childrenAdded(NodeMemberEvent ev) {
    }
    
    // 
    // Not important Collection methods, just add (Object is important)!
    // 
    
    /** Adds value to the lookup
     */
    public boolean add(Object obj) {
        ic.add (obj);
        return true;
    }
    
    public boolean retainAll(java.util.Collection collection) {
        return false;
    }
    
    public boolean contains(Object obj) {
        return false;
    }
    
    public Object[] toArray(Object[] obj) {
        return obj;
    }
    
    public java.util.Iterator iterator() {
        return null;
    }
    
    public boolean removeAll(java.util.Collection collection) {
        return false;
    }
    
    public Object[] toArray() {
        return null;
    }
    
    public boolean remove(Object obj) {
        return false;
    }
    
    public void clear() {
    }
    
    public boolean addAll(java.util.Collection collection) {
        return false;
    }
    
    public int size() {
        return 0;
    }
    
    public boolean containsAll(java.util.Collection collection) {
        return false;
    }
    
    public boolean isEmpty() {
        return false;
    }
    
    
}
