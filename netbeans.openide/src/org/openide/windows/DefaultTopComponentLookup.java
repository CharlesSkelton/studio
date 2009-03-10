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

package org.openide.windows;

import java.beans.*;
import java.util.*;

import org.openide.actions.*;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

/** Embeddable visual component to be displayed in the IDE.
 * This is the basic unit of display in the IDE--windows should not be
 * created directly, but rather use this class.
 * A top component may correspond to a single window, but may also
 * be a tab (e.g.) in a window. It may be docked or undocked,
 * have selected nodes, supply actions, etc.
 *
 * Important serialization note: Serialization of this TopComponent is designed
 * in a way that it's not desired to override writeReplace method. If you would
 * like to resolve to something, please implement readResolve() method directly
 * on your top component.
 *
 * @author Jaroslav Tulach
 */
final class DefaultTopComponentLookup extends ProxyLookup 
implements java.beans.PropertyChangeListener, InstanceContent.Convertor, LookupListener {
    /** component to work with */
    private TopComponent tc;
    /** lookup listener that is attached to all subnodes */
    private LookupListener listener;
    /** Map of (Node -> node Lookup.Result) the above lookup listener is attached to */
    private Map attachedTo;
    
    /** Creates the lookup.
     * @param tc component to work on
    */
    public DefaultTopComponentLookup(TopComponent tc) {
        super ();
        
        this.tc = tc;
        this.listener = (LookupListener)org.openide.util.WeakListener.create (LookupListener.class, this, null);
        
        updateLookups ();
        
        tc.addPropertyChangeListener(
            "activatedNodes", // NOI18N
            org.openide.util.WeakListener.propertyChange(this, tc)
        );
        
    }
    
    private static Lookup[] EMPTY_ARRAY = new Lookup[0];
    /** Extracts activated nodes from a top component and
     * returns their lookups.
     */
    private void updateLookups () {
        Node[] arr = tc.getActivatedNodes();
        if (arr == null) {
            setLookups (EMPTY_ARRAY);
            return;
        }
        
        Lookup[] lookups = new Lookup[arr.length + 1];
        
        Map copy;
        synchronized (this) {
            if (attachedTo == null) {
                copy = java.util.Collections.EMPTY_MAP;
            } else {
                copy = new HashMap (attachedTo);
            }
        }
        
        for (int i = 0; i < arr.length; i++) {
            lookups[i] = arr[i].getLookup ();
            if (copy != null) {
                // node arr[i] remains there, so do not remove it
                copy.remove (arr[i]);
            }
        }
        lookups[arr.length] = org.openide.util.lookup.Lookups.fixed (arr, this);

        for (Iterator it = copy.values().iterator(); it.hasNext(); ) {
            Lookup.Result res = (Lookup.Result)it.next ();
            res.removeLookupListener (listener);
        }
        
        synchronized (this) {
            attachedTo = null;
        }
        
        setLookups (lookups);
    }

    /** Updates the lookup if necessary */
    public void propertyChange(java.beans.PropertyChangeEvent ev) {
        updateLookups ();
    }    

    
    /** Change in one of the lookups we delegate to */
    public void resultChanged(org.openide.util.LookupEvent ev) {
        updateLookups ();
    }
    
    /** Finds out whether a query for a class can be influenced 
     * by a state of the "nodes" lookup and whether we should 
     * initialize listening
     */
    private static boolean isNodeQuery (Class c) {
        return Node.class.isAssignableFrom (c) || c.isAssignableFrom (Node.class);
    }
    
    protected synchronized void beforeLookup (Template t) {
        if (attachedTo == null && isNodeQuery (t.getType ())) {
            Lookup[] arr = getLookups();
            
            attachedTo = new WeakHashMap (arr.length * 2);
            for (int i = 0; i < arr.length - 1; i++) {
                Lookup.Result res = arr[i].lookup (t);
                res.addLookupListener(listener);
                attachedTo.put (arr[i], res);
            }
        }
    }
    
    // 
    // InstanceContent.Convertor
    // 

    /** Create something only if the Node does not provide itself 
     * from its lookup */
    public Object convert(Object obj) {
        Lookup[] arr = getLookups ();
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i].lookup (Node.class) == obj) {
                return null;
            }
        }
        
        return obj;
    }
    
    public String displayName(Object obj) {
        return ((Node)obj).getDisplayName();
    }
    
    public String id(Object obj) {
        return ((Node)obj).getName ();
    }
    
    public Class type(Object obj) {
        return convert (obj) == null ? Object.class : obj.getClass ();
    }
    
}
