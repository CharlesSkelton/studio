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

package org.openide.util;

import java.awt.event.*;
import java.awt.*;
import java.beans.*;
import java.io.*;
import javax.swing.*;

import org.openide.explorer.*;
import org.openide.nodes.*;

/** ActionMap that is composed from all Components up to the ExplorerManager.Provider
*
* @author   Jaroslav Tulach
*/
final class UtilitiesCompositeActionMap extends ActionMap {
    private Component component;

    public UtilitiesCompositeActionMap(Component c) {
        this.component = c;
    }

    public int size() {
        return keys ().length;
    }

    public Action get(Object key) {
        Component c = component;
        for (;;) {
            if (c instanceof JComponent) {
                javax.swing.ActionMap m = ((JComponent)c).getActionMap ();
                if (m != null) {
                    Action a = m.get (key);
                    if (a != null) {
                        return a;
                    }
                }
            }

            if (c instanceof Lookup.Provider) {
                break;
            }

            c = c.getParent();

            if (c == null) {
                break;
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
        java.util.HashSet keys = new java.util.HashSet ();

        Component c = component;
        for (;;) {
            if (c instanceof JComponent) {
                javax.swing.ActionMap m = ((JComponent)c).getActionMap ();
                if (m != null) {
                    java.util.List l;

                    if (all) {
                        l = java.util.Arrays.asList (m.allKeys ());
                    } else {
                        l = java.util.Arrays.asList (m.keys ());
                    }

                    keys.addAll (l);
                }
            }

            if (c instanceof Lookup.Provider) {
                break;
            }

            c = c.getParent();

            if (c == null) {
                break;
            }
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
