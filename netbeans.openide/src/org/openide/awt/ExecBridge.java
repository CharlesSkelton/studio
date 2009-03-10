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

package org.openide.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.*;
import java.lang.reflect.Method;

import javax.swing.*;
import org.openide.ErrorManager;

import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.WeakListener;

/** Menu item associated with data object. When
* pressed it executes the data object.
*
* @author Jaroslav Tulach
*/
class ExecBridge extends Object implements ActionListener, PropertyChangeListener {
    /** object to execute */
    private Node node;
    /** associated button */
    private AbstractButton button;
    
    private static Class execCookieClass = null;
    private static synchronized Class getExecCookieClass() {
        if (execCookieClass == null) {
            try {
                execCookieClass = Class.forName("org.openide.cookies.ExecCookie", true, (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class)); // NOI18N
            } catch (ClassNotFoundException cnfe) {
                execCookieClass = ExecBridge.class;
            }
        }
        if (execCookieClass == ExecBridge.class) {
            return null;
        } else {
            return execCookieClass;
        }
    }

    /** Creates new ExecMenuItem */
    private ExecBridge (Node node, AbstractButton button) {
        this.node = node;
        this.button = button;

        button.addActionListener (this);
        node.addPropertyChangeListener (WeakListener.propertyChange (this, node));

        updateState ();
    }

    /** Executes the object.
    */
    public void actionPerformed (ActionEvent ev) {
        Class c = getExecCookieClass();
        if (c == null) {
            return;
        }
        Node.Cookie ec = node.getCookie(c);
        if (ec != null) {
            try {
                Method m = getExecCookieClass().getMethod("start", new Class[] {});
                m.invoke(ec, new Object[] {});
            } catch (Exception e) {
                ErrorManager.getDefault().notify(e);
            }
        }
    }

    /** Listen to changes of exec cookie on the data object.
    */
    public void propertyChange (PropertyChangeEvent ev) {
        if (Node.PROP_COOKIE.equals (ev.getPropertyName ())) {
            updateState ();
        }
        if (Node.PROP_DISPLAY_NAME.equals (ev.getPropertyName ())) {
            updateState ();
        }
        if (Node.PROP_ICON.equals (ev.getPropertyName ())) {
            updateState ();
        }
    }

    /** Updates state
    */
    private void updateState () {
        button.setText (node.getDisplayName ());
        Icon icon = new ImageIcon (node.getIcon (BeanInfo.ICON_COLOR_16x16));
        button.setIcon (icon);

        Class c = getExecCookieClass();
        button.setEnabled(c != null && node.getCookie(c) != null);
    }

    /** Creates menu item associated with the data object.
     * May be null if there is no ExecCookie.
    */
    public static JMenuItem createMenuItem (DataObject obj) {
        if (!obj.isValid()) return null;
        Node n = obj.getNodeDelegate ();
        Class c = getExecCookieClass();
        if (c == null || n.getCookie(c) == null) return null;
        JMenuItem item = new JMenuItem ();
        new ExecBridge (n, item);
        return item;
    }

    /** Creates toolbar component associated with the object.
     * May be null if there is no ExecCookie.
    */
    public static JButton createButton (DataObject obj) {
        if (!obj.isValid()) return null;
        Node n = obj.getNodeDelegate ();
        Class c = getExecCookieClass();
        if (c == null || n.getCookie(c) == null) return null;
        JButton item = new JButton ();
        new ExecBridge (n, item);
        return item;
    }

}
