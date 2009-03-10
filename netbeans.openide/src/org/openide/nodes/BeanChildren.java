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

import java.lang.ref.*;
import java.beans.beancontext.*;
import java.beans.IntrospectionException;
import java.util.*;

import org.openide.ErrorManager;

/** Class that represents bean children of a JavaBeans context.
* It listens on the bean context changes and creates nodes for
* child beans. By default {@link BeanNode}s are created for all
* child beans, but this behaviour can be changed by
* providing a different factory to the constructor.
*
* @author Jaroslav Tulach, Jesse Glick
*/
public class BeanChildren extends Children.Keys {
    /** default factory for creation of children */
    private static final Factory DEFAULT_FACTORY = new BeanFactory ();

    /** bean context to work on */
    private BeanContext bean;

    /** factory for creation of subnodes */
    private Factory factory;

    /** context listener */
    private ContextL contextL;
    
    /** Map from nodes some BeanChildren have created, to the beans
     * they were intended to represent. If a node is deleted, we remove
     * the bean from its context. The nodes are weakly held, and each
     * value is a 2-element array of weak references to the bean context
     * and child, resp.
     * See #7925.
     */
    private static final java.util.Map nodes2Beans = new WeakHashMap(); // Map<Node,Reference<Object>[2]>

    /** Create {@link BeanNode} children based on a Bean context.
    * @param bean the context
    */
    public BeanChildren(BeanContext bean) {
        this (bean, DEFAULT_FACTORY);
    }

    /** Create children based on a Bean context.
    * @param bean the context
    * @param factory a factory to use for creation of child nodes
    */
    public BeanChildren (BeanContext bean, Factory factory) {
        this.bean = bean;
        this.factory = factory;
    }

    /** Updates the keys from the bean context.
    */
    final void updateKeys () {
        setKeys (bean.toArray ());
    }
    
    /** Creates a node representant for given bean. Uses factory 
    * to get the node.
    * @param subbean the bean from bean context
    * @return node created by the factory
    */
    protected Node[] createNodes (Object subbean) {
        try {
            if (subbean instanceof BeanContextSupport) {
                BeanContextSupport bcs = (BeanContextSupport)subbean;

                if (bean.contains (bcs.getBeanContextPeer()) && bcs != bcs.getBeanContextPeer() ) {
                    // sometimes a BeanContextSupport occures in the list of
                    // beans children even there is its peer. we think that
                    // it is desirable to hide the context if the peer is
                    // also present
                    return new Node[0];
                }
            }

            Node n = factory.createNode(subbean);
            // #7925: deleting from BeanChildren has no effect
            synchronized (nodes2Beans) {
                nodes2Beans.put(n, new Reference[] {new WeakReference(bean), new WeakReference(subbean)});
            }
            n.addNodeListener(contextL);
            return new Node[] {n};
        } catch (IntrospectionException ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            return new Node[0];
        }
    }

    /* Initializes children and attaches listener to bean context.
    */
    protected void addNotify () {
        // attaches a listener to the bean
        contextL = new ContextL (this);
        bean.addBeanContextMembershipListener (contextL);

        updateKeys ();
    }
    
    /** Removes the listener and does some necessary clean up.
    */
    protected void removeNotify () {
        if (contextL != null)
            bean.removeBeanContextMembershipListener (contextL);
        contextL = null;
        
        setKeys (java.util.Collections.EMPTY_SET);
    }

    /** Controls which nodes
    * are created for a child bean.
    * @see BeanChildren#BeanChildren(BeanContext, BeanChildren.Factory)
    */
    public static interface Factory {
        /** Create a node for a child bean.
        * @param bean the bean
        * @return the node for the bean 
        * @exception IntrospectionException if the node cannot be created
        */
        public Node createNode (Object bean) throws IntrospectionException;
    }

    /** Default factory. Creates BeanNode for each bean
    */
    private static class BeanFactory extends Object implements Factory {
	BeanFactory() {}
	
        /** @return bean node */
        public Node createNode (Object bean) throws IntrospectionException {
            return new BeanNode (bean);
        }
    }

    /** Context listener.
    */
    private static final class ContextL extends NodeAdapter implements BeanContextMembershipListener {
	ContextL() {}
	
        /** weak reference to the BeanChildren object */
        private WeakReference ref;

        /** Constructor */
        ContextL (BeanChildren bc) {
            ref = new WeakReference (bc);
        }

        /** Listener method that is called when a bean is added to
        * the bean context.
        * @param bcme event describing the action
        */
        public void childrenAdded (BeanContextMembershipEvent bcme) {
            BeanChildren bc = (BeanChildren)ref.get ();
            if (bc != null) {
                bc.updateKeys();
            }
        }

        /** Listener method that is called when a bean is removed to
        * the bean context.
        * @param bcme event describing the action
        */
        public void childrenRemoved (BeanContextMembershipEvent bcme) {
            BeanChildren bc = (BeanChildren)ref.get ();
            if (bc != null) {
                bc.updateKeys ();
            }
        }
        
        public void nodeDestroyed(NodeEvent ev) {
            Node n = ev.getNode();
            Reference[] refs;
            synchronized (nodes2Beans) {
                refs = (Reference[])nodes2Beans.get(n);
            }
            if (refs != null) {
                BeanContext bean = (BeanContext)refs[0].get();
                if (bean != null) {
                    Object subbean = refs[1].get();
                    if (subbean != null) {
                        // This should in turn cause childrenRemoved to be called...
                        // and the node not to be recreated in the next keys update.
                        try {
                            bean.remove(subbean);
                        } catch (RuntimeException re) {
                            // BeanContext does not document what might be thrown
                            // from this method, but in fact BeanContextSupport
                            // can throw IllegalStateException if either child or
                            // parent refuses the deletion. So better deal with it.
                            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, re);
                        }
                    }
                }
            }
        }
        
    }

}
