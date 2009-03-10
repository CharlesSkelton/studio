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

package org.openide.util.actions;

import java.beans.*;
import java.lang.ref.*;
import java.util.*;
import javax.swing.Action;
import org.openide.awt.Actions;

import org.openide.util.ContextAwareAction;
import org.openide.nodes.Node;
import org.openide.explorer.ExplorerManager;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent.Registry;
import org.openide.util.Lookup;
import org.openide.util.WeakSet;

/** An action which can listen to the activated node selection.
* This means that the set of nodes active in a window
* may change the enabled state of the action according to {@link #enable}.
* <p><strong>Note:</strong> if your action involves getting cookies
* from nodes, which in many cases is the correct design, please use
* {@link CookieAction} instead, as that permits sensitivity to cookies
* and also listens to changes in supplied cookies.
* @author   Jan Jancura, Ian Formanek, Jaroslav Tulach
*/
public abstract class NodeAction extends CallableSystemAction
implements ContextAwareAction {
    private static final long serialVersionUID = -5672895970450115226L;

    /** whether or not anyone is listening to PROP_ENABLED */
    private static final String PROP_HAS_LISTENERS = "hasListeners"; // NOI18N
    /** last-used nodes, as a Reference<Node[]> */
    private static final String PROP_LAST_NODES = "lastNodes"; // NOI18N
    /** last-computed enablement (Boolean) */
    private static final String PROP_LAST_ENABLED = "lastEnabled"; // NOI18N
    /** the selection listener, if any */
    private static NodesL l;
    /** set of actions with listeners */
    private static final Set listeningActions = new WeakSet(100); // Set<NodeAction>

    /* Initialize the listener.
    */
    protected void initialize () {
        super.initialize ();
        putProperty(PROP_HAS_LISTENERS, Boolean.FALSE);
        // Not yet determined:
        putProperty(PROP_ENABLED, null);
    }

    /** Initializes selection listener.
     * If you override this method, you must always call the super method first.
     */
    protected void addNotify () {
        super.addNotify();
        // initializes the listener
        putProperty(PROP_HAS_LISTENERS, Boolean.TRUE);
        synchronized (listeningActions) {
            if (l == null) {
                l = new NodesL();
            }
            if (listeningActions.isEmpty()) {
                l.setActive(true);
            }
            listeningActions.add(this);
        }
    }

    /** Shuts down the selection listener.
     * If you override this method, you must always call the super method last.
     */
    protected void removeNotify () {
        synchronized (listeningActions) {
            listeningActions.remove(this);
            if (listeningActions.isEmpty()) {
                l.setActive(false);
            }
        }
        putProperty(PROP_HAS_LISTENERS, Boolean.FALSE);
        // Previous results should no longer be cached:
        putProperty(PROP_ENABLED, null);
        super.removeNotify();
    }

    /** Test for enablement based on {@link #enable}.
    * You probably ought not ever override this.
    * @return <code>true</code> to enable
    */
    public boolean isEnabled () {
        Node[] ns = null;
        Boolean b = null;
        synchronized (getLock()) {
            b = (Boolean)getProperty(PROP_ENABLED);
            if (b == null) {
                ns = getActivatedNodes(surviveFocusChange());
                Reference r = (Reference)getProperty(PROP_LAST_NODES);
                if (r != null && r.get() == ns) {
                    // Still using the same Node[] we did last time. Remember the result.
                    b = (Boolean)getProperty(PROP_LAST_ENABLED);
                    if (((Boolean)getProperty(PROP_HAS_LISTENERS)).booleanValue()) {
                        putProperty(PROP_ENABLED, b);
                    }
                } else {
                    // Really need to compute it.
                    // #17433: do this outside the lock!
                }
                // if inactive, we cannot safely cache results because node selection might change
            }
        }
        if (b == null) {
            b = ((ns != null && enable(ns)) ? Boolean.TRUE : Boolean.FALSE);
            synchronized (getLock()) {
                putProperty(PROP_LAST_NODES, new WeakReference(ns));
                putProperty(PROP_LAST_ENABLED, b);
                if (((Boolean)getProperty(PROP_HAS_LISTENERS)).booleanValue()) {
                    putProperty(PROP_ENABLED, b);
                }
            }
        }
        return b.booleanValue();
    }
    
    /* Change enablement state.
     * Clears our previous cache.
     * Some NodeAction subclasses (CookieAction, MoveUpAction, ...) may call this
     * when some aspect of the node selection other than the selection itself
     * changes, so we should clear the cache to ensure that the enablement status
     * is respected.
     */
    public void setEnabled(boolean e) {
        putProperty(PROP_LAST_ENABLED, null);
        putProperty(PROP_LAST_NODES, null);
        if (((Boolean)getProperty(PROP_HAS_LISTENERS)).booleanValue()) {
            // Just set it; the next time selection chamges, we will recompute.
            super.setEnabled(e);
        } else {
            // Problematic. If we just set PROP_ENABLED then the next time isEnabled()
            // is called, even if the node selection is now different, we will be
            // in trouble; it will not bother to call enable() again.
            putProperty(PROP_ENABLED, null, true);
        }
    }

    /** Perform the action with a specific action event.
     * Normally this simply calls {@link #performAction()}, that is using
     * the global node selection.
     * However you may call this directly, with an action event whose
     * source is either a node or an array of nodes, to invoke the action
     * directly on that nodes or nodes. If you do this, the action must
     * be such that it would be enabled on that node selection, otherwise
     * the action is not required to behave correctly (that is, it can
     * be written to assume that it is never called with a node selection
     * it is not enabled on).
     * @param ev action event
     */
    public void actionPerformed (java.awt.event.ActionEvent ev) {
        Object s = ev == null ? null : ev.getSource ();
        if (s instanceof Node) {
            performAction (new Node[] { (Node) s });
        } else if (s instanceof Node[]) {
            performAction ((Node[])s);
        } else {
            performAction ();
        }
    }

    /** Performs the action.
    * In the default implementation, calls {@link #performAction(Node[])}.
    * In general you need not override this.
    */
    public void performAction() {
        performAction (getActivatedNodes ());
    }

    /** Get the currently activated nodes.
    * @return the nodes (may be empty but not <code>null</code>)
    */
    public final Node[] getActivatedNodes () {
        Registry r = CallbackSystemAction.getRegistry ();
        if (r != null) {
            return r.getActivatedNodes ();
        } else {
            ExplorerManager em = CallbackSystemAction.getExplorerManager();
            if (em != null) {
                return em.getSelectedNodes();
            }
        }
        return new Node[0];
    }

    /** Specify the behavior of the action when a window with no
    * activated nodes is selected.
    * If the action should then be disabled,
    * return <code>false</code> here; if the action should stay in the previous state,
    * return <code>true</code>.
    * <p>Note that {@link #getActivatedNodes} and {@link #performAction} are still
    * passed the set of selected nodes from the old window, if you keep this feature on.
    * This is useful, e.g., for an action like Compilation which should remain active
    * even if the user switches to a window like the Output Window that has no associated nodes;
    * then running the action will still use the last selection from e.g. an Explorer window
    * or the Editor, if there was one to begin with.
    *
    * @return <code>true</code> in the default implementation
    */
    protected boolean surviveFocusChange() {
        return true;
    }

    /**
    * Perform the action based on the currently activated nodes.
    * Note that if the source of the event triggering this action was itself
    * a node, that node will be the sole argument to this method, rather
    * than the activated nodes.
    *
    * @param activatedNodes current activated nodes, may be empty but not <code>null</code>
    */
    protected abstract void performAction (Node[] activatedNodes);

    /**
    * Test whether the action should be enabled based
    * on the currently activated nodes.
    *
    * @param activatedNodes current activated nodes, may be empty but not <code>null</code>
    * @return <code>true</code> to be enabled, <code>false</code> to be disabled
    */
    protected abstract boolean enable (Node[] activatedNodes);
    
    /** Implements <code>ContextAwareAction</code> interface method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }
    
    
    /** Fire PROP_ENABLE if the value is currently known (and clear that value).
     */
    void maybeFireEnabledChange() {
        boolean fire = false;
        synchronized (getLock()) {
            if (getProperty(PROP_ENABLED) != null) {
                putProperty(PROP_ENABLED, null);
                fire = true;
            }
        }
        if (fire) {
            firePropertyChange(PROP_ENABLED, null, null);
        }
    }


    /** Getter for activated nodes
    * @return array
    */
    private static Node[] getActivatedNodes (boolean survive) {
        Registry r = CallbackSystemAction.getRegistry ();
        if (r != null) {
            if (survive) {
                return r.getActivatedNodes ();
            } else {
                return r.getCurrentNodes ();
            }
        } else {
            ExplorerManager em = CallbackSystemAction.getExplorerManager();
            if (em != null) {
                return em.getSelectedNodes();
            }
        }
        return new Node[0];
    }

    /** Node listener to check whether the action is enabled or not
    */
    private static final class NodesL
        implements PropertyChangeListener {
        /** whether to change enablement of nodes marked to survive focus change */
        private boolean chgSFC = false;
        /** and those marked to not survive */
        private boolean chgNSFC = false;

        /** Constructor that checks the current state
        */
        public NodesL() {
        }

        /** Activates/passivates the listener.
        */
        void setActive (boolean active) {
                Registry r = CallbackSystemAction.getRegistry ();
                if (r != null) {
                    if (active) {
                        r.addPropertyChangeListener (this);
                    } else {
                        r.removePropertyChangeListener(this);
                        // Any saved PROP_ENABLED will be bogus now:
                        forget(true);
                        forget(false);
                    }
                } else {
                    ExplorerManager em = CallbackSystemAction.getExplorerManager();
                    if (em != null) {
                        if (active) {
                            em.addPropertyChangeListener(this);
                        } else {
                            em.removePropertyChangeListener(this);
                            forget(true);
                            forget(false);
                        }
                    }
                }
        }

        /** Property change listener.
        */
        public void propertyChange (PropertyChangeEvent ev) {
            String p = ev.getPropertyName();
            boolean schedule = false;
            if (p == null || p.equals(Registry.PROP_ACTIVATED_NODES)) {
                chgSFC = true;
                schedule = true;
            }
            if (p == null || p.equals(Registry.PROP_CURRENT_NODES)) {
                chgNSFC = true;
                schedule = true;
            }
            if (p == null || p.equals(ExplorerManager.PROP_SELECTED_NODES)) {
                chgSFC = true;
                chgNSFC = true;
                schedule = true;
            }
            if (schedule) {
                update();
            }
        }

        /** Updates the state of the action.
        */
        public void update() {
            if (chgSFC) {
                forget(true);
                chgSFC = false;
            }
            if (chgNSFC) {
                forget(false);
                chgNSFC = false;
            }
        }

        /** Checks the state of the action.
         * Or rather, it just forgets it ever knew.
         * @param sfc if true, only survive-focus-change actions affected, else only not-s-f-c
        */
        private void forget(boolean sfc) {
            List as;
            synchronized (listeningActions) {
                as = new ArrayList(listeningActions.size());
                for (Iterator it = listeningActions.iterator(); it.hasNext(); ) {
                    as.add(it.next());
                }
            }
            Iterator it = as.iterator();
            while (it.hasNext()) {
                NodeAction a = (NodeAction)it.next();
                if (a.surviveFocusChange() == sfc) {
                    a.maybeFireEnabledChange();
                }
            }
        }
    } // end of NodesL
    
    
    /** A delegate action that is usually associated with a specific lookup and
     * extract the nodes it operates on from it. Otherwise it delegates to the
     * regular NodeAction.
     */
    private static final class DelegateAction extends Object
    implements javax.swing.Action, org.openide.util.LookupListener,
    Presenter.Menu, Presenter.Popup, Presenter.Toolbar {
        /** action to delegate too */
        private NodeAction delegate;
        /** lookup we are associated with (or null) */
        private org.openide.util.Lookup.Result result;
        /** previous state of enabled */
        private boolean enabled = true;
        /** support for listeners */
        private PropertyChangeSupport support = new PropertyChangeSupport (this);
        
        public DelegateAction (NodeAction a, Lookup actionContext) {
            this.delegate = a;
            
            this.result = actionContext.lookup (new org.openide.util.Lookup.Template (
                Node.class
            ));
            this.result.addLookupListener ((LookupListener)org.openide.util.WeakListener.create (
                LookupListener.class, this, this.result
            ));
            resultChanged (null);
        }
        
        
        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }
        
        /** Nodes are taken from the lookup if any.
         */
        private synchronized Node[] nodes () {
            Node[] empty = new Node[0];
            
            if (result != null) {
                return (Node[])result.allInstances ().toArray (empty);
            } else {
                return empty;
            }
        }
        
        /** Invoked when an action occurs.
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            delegate.performAction (nodes ());
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener (listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener (listener);
        }

        public void putValue(String key, Object o) {}
        
        public Object getValue(String key) {
            return delegate.getValue(key);
        }
        
        public boolean isEnabled() {
            return delegate.enable(nodes ());
        }
        
        public void setEnabled(boolean b) {
        }

        public void resultChanged(org.openide.util.LookupEvent ev) {
            boolean newEnabled = delegate.enable(nodes ());
            if (newEnabled != enabled) {
                support.firePropertyChange (PROP_ENABLED, enabled, newEnabled);
                enabled = newEnabled;
            }
        }
        
        public javax.swing.JMenuItem getMenuPresenter() {
            if (isMethodOverriden (delegate, "getMenuPresenter")) { // NOI18N
                return delegate.getMenuPresenter ();
            } else {
                return new Actions.MenuItem(this, true);
            }
        }
        
        public javax.swing.JMenuItem getPopupPresenter() {
            if (isMethodOverriden (delegate, "getPopupPresenter")) { // NOI18N
                return delegate.getPopupPresenter ();
            } else {
                return new Actions.MenuItem(this, false);
            }
        }
        
        public java.awt.Component getToolbarPresenter() {
            if (isMethodOverriden (delegate, "getToolbarPresenter")) { // NOI18N
                return delegate.getToolbarPresenter ();
            } else {
                return new Actions.ToolbarButton (this);
            }
        }
        
        private boolean isMethodOverriden (NodeAction d, String name) {
            try {
                java.lang.reflect.Method m = d.getClass ().getMethod(name, new Class[0]);
                return m.getDeclaringClass() != CallableSystemAction.class;
            } catch (java.lang.NoSuchMethodException ex) {
                ex.printStackTrace();
                throw new IllegalStateException ("Error searching for method " + name + " in " + d); // NOI18N
            }
        }
    } // end of DelegateAction
}
        
