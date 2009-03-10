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


import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;


// This is almost copy of org.openide.util.UtilitiesCompositeActionMap.
/** ActionMap that delegates to current action map of provided component.
 * Used in <code>TopComopnent</code> lookup.
 * <p><b>Note: This action map is 'passive', i.e putting new mappings
 * into it makes no effect. Could be changed later.</b>
 *
 * @author Peter Zavadsky
 */
final class DelegateActionMap extends ActionMap {
    private JComponent component;

    public DelegateActionMap(JComponent c) {
        this.component = c;
    }

    public int size() {
        return keys ().length;
    }

    public Action get(Object key) {
        javax.swing.ActionMap m = component.getActionMap ();
        if (m != null) {
            Action a = m.get (key);
            if (a != null) {
                return a;
            }
        }
        
        return null;
    }

    public Object[] allKeys() {
        return keys (true);
    }

    public Object[] keys() {
        return keys (false);
    }


    private Object[] keys(boolean all) {
        java.util.Set keys = new java.util.HashSet ();

        javax.swing.ActionMap m = component.getActionMap ();
        if (m != null) {
            java.util.List l;

            if (all) {
                l = java.util.Arrays.asList (m.allKeys ());
            } else {
                l = java.util.Arrays.asList (m.keys ());
            }

            keys.addAll (l);
        }

        return keys.toArray ();
    }

    // 
    // Not implemented
    //
    public void remove(Object key) {
    }        

    public void setParent(ActionMap map) {
    }

    public void clear() {
    }

    public void put(Object key, Action action) {
    }

    public ActionMap getParent() {
        return null;
    }

}    
