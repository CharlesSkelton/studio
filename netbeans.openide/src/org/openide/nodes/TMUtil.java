/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.nodes;

import java.util.*;

import org.openide.util.HelpCtx;
import org.openide.util.Mutex;
import org.openide.windows.WindowManager;

/** Class that serves as interface to various parts of OpenAPIs that need 
 * not be present.
 *
 * @author  Jaroslav Tulach
 */
abstract class TMUtil extends Object {
    /** variable that will contain the argument to a call and then a result.
     */
    private static final ThreadLocal TALK = new ThreadLocal ();
    
    /** maps names of algorithms (that use the ARGUMENT and the RESULT)
     * and runnables that has to be executed to compute the algorithm
     * (String, Runnable) or (String, Exception)
     */
    private static Hashtable algorithms = new Hashtable (10);
    
    /** Creates InstanceCookie, if available.
     * @param bean the object to create cookie for
     * @return Node.Cookie or null
     */
    static Node.Cookie createInstanceCookie (Object bean) {
        try {
            TALK.set (bean);
            return exec ("Bean") ? (Node.Cookie)TALK.get () : null; // NOI18N
        } finally {
            TALK.set(null);
        }
    }
    
    /** Tries to find a help for given bean node.
     * @param n the bean node
     * @return help ctx or null
     */
    static HelpCtx findHelp (BeanNode n) {
        try {
            TALK.set (n);
            return exec ("Help") ? (HelpCtx)TALK.get () : null; // NOI18N
        } 
        finally {
            TALK.set(null);
        }                
    }
    
    /** Checks whether an object is instance of DialogDescriptor and if 
     * so it used top manager to create its instance.
     * @param maybeDialogDescriptor an object
     * @return a dialog or null
     */
    static java.awt.Dialog createDialog (Object maybeDialogDescriptor) {
        try {
            TALK.set (maybeDialogDescriptor);
            return exec ("Dial") ? (java.awt.Dialog)TALK.get () : null; // NOI18N
        } 
        finally {
            TALK.set(null);
        }                
    }
    
    /** Attaches a customizer to given node.
     * @param node the bean node 
     * @param cust customizer to attach
     */
    static void attachCustomizer (Node node, java.beans.Customizer cust) {
        try {
            TALK.set (new Object[] { node, cust });
            exec ("Cust"); // NOI18N
        } 
        finally {
            TALK.set(null);
        }                
    }
    
    /** Finds main window.
     * @return main window or null
     */
    static java.awt.Frame mainWindow () {
        try {
            if (exec ("Win")) { // NOI18N
                return (java.awt.Frame)TALK.get ();
            } else {
                // default owner for JDialog
                if (owner == null) {
                    owner = (java.awt.Frame)new javax.swing.JDialog ().getOwner ();
                }
                return owner;
            }
        } 
        finally {
            TALK.set(null);
        }
    }
    private static java.awt.Frame owner;
    
    /** Finds usable list cell renderer.
     */
    static javax.swing.ListCellRenderer findListCellRenderer () {
        try {
            if (exec ("Rend")) { // NOI18N
                return (javax.swing.ListCellRenderer)TALK.get ();
            } else {
                return new javax.swing.DefaultListCellRenderer ();
            }
        } 
        finally {
            TALK.set(null);
        }                    
    }
    
    /** Invoke an indexed customizer. */
    static void showIndexedCustomizer(Index idx) {
        try {
            TALK.set(idx);
            if (! exec("IndexC")) { // NOI18N
                // Fallback to simple method.
                final IndexedCustomizer ic = new IndexedCustomizer();
                ic.setObject(idx);
                ic.setImmediateReorder(false);
                Mutex.EVENT.readAccess(new Mutex.Action() {
                    public Object run() {
                        ic.show();
                        return null;
                    }
                });
            }
        } 
        finally {
            TALK.set(null);
        }        
            
    }

    /** Executes algorithm of given name. 
     * @param name the name of algorithm
     * @return true iff successfule
     */
    private static boolean exec (String name) {
        Object  obj = algorithms.get (name);
        
        if (obj == null) {
            try {
                Class c = Class.forName ("org.openide.nodes.TMUtil$" + name); // NOI18N
                obj = c.newInstance ();
            } catch (ClassNotFoundException ex) {
                obj = ex;
                NodeOp.exception (ex);
            } catch (InstantiationException ex) {
                // that is ok, we should not be able to create an
                // instance if some classes are missing
                obj = ex;
            } catch (IllegalAccessException ex) {
                obj = ex;
                NodeOp.exception (ex);
            } catch (NoClassDefFoundError ex) {
                // that is ok, some classes need not be found
                obj = ex;
            }

            algorithms.put (name, obj);
        }

        try {
            if (obj instanceof Runnable) {
                ((Runnable)obj).run ();
                return true;
            } 
        } catch (NoClassDefFoundError ex) {
            // in case of late linking the error can be thrown
            // just when the runnable is executed
            algorithms.put (name, ex);
        }
        return false;
    }
    
    /** Creates instance of InstanceCookie for given object.
     * ARGUMENT contains the bean to create instance for.
     */
    static final class Bean 
    implements Runnable, org.openide.cookies.InstanceCookie {
        private Object bean;
        
        public void run () {
            Bean n = new Bean ();
            n.bean = TALK.get ();
            TALK.set (n);
        }
        
        public String instanceName() {
            return bean.getClass ().getName ();
        }
        
        public Class instanceClass() {
            return bean.getClass ();
        }
        
        public Object instanceCreate() {
            return bean;
        }
    }
    
    /** Finds help for given node.
     * ARGUMENT contains the bean to create instance for.
     */
    static final class Help implements Runnable {
        public void run () {
            BeanNode node = (BeanNode)TALK.get ();
            HelpCtx h = org.openide.loaders.InstanceSupport.findHelp (
                (org.openide.cookies.InstanceCookie) node.getCookie (
                    org.openide.cookies.InstanceCookie.class
                )
            );
            TALK.set (h);
        }
    }
    
    /** Creates dialog from DialogDescriptor
     * ARGUMENT contains the descriptor.
     */
    static final class Dial implements Runnable {
        public void run () {
            Object obj = TALK.get ();
            if (obj instanceof org.openide.DialogDescriptor) {
                TALK.set (org.openide.DialogDisplayer.getDefault ().createDialog(
                    (org.openide.DialogDescriptor)obj
                ));
            } else {
                TALK.set (null);
            }
        }
    }
    
    /** Attaches the node to a customizer if it implements NodeCustomizer.
     * ARGUMENT contains array of node and customizer
     */
    static final class Cust implements Runnable {
        public void run () {
            Object[] arr = (Object[])TALK.get ();
            
            Node n = (Node)arr[0];
            Object cust = arr[1];
            
            if (cust instanceof org.openide.explorer.propertysheet.editors.NodeCustomizer) {
                ((org.openide.explorer.propertysheet.editors.NodeCustomizer)cust).attach (n);
            }
        }
    }
    
    /** Finds the main window.
     */
    static final class Win implements Runnable {
        public void run () {
            TALK.set (
                WindowManager.getDefault().getMainWindow ()
            );
        }
    }
    
    /** Finds renderer.
     */
    static final class Rend implements Runnable {
        public void run () {
            TALK.set (
                new org.openide.explorer.view.NodeRenderer ()
            );
        }
    }
    
    static final class IndexC implements Runnable {
        public void run() {
            Index idx = (Index)TALK.get();
            java.awt.Container p = new javax.swing.JPanel();
            IndexedCustomizer ic = new IndexedCustomizer(p, false);
            ic.setObject(idx);
            ic.setImmediateReorder(false);
            org.openide.DialogDescriptor dd = new org.openide.DialogDescriptor(p, Node.getString("LAB_order"));
            dd.setModal(true);
            dd.setOptionType(org.openide.DialogDescriptor.DEFAULT_OPTION);
            Object result = org.openide.DialogDisplayer.getDefault().notify(dd);
            if (result == org.openide.DialogDescriptor.OK_OPTION) {
                ic.doClose();
            }
        }
    }
    
}
