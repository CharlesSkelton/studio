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
import java.awt.Component;
import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.lang.ref.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.Action;

import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.explorer.ExplorerManager;
import org.openide.windows.TopComponent.Registry;
import org.openide.util.WeakSet;
import org.openide.awt.Actions;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;

/* enabled is old; action perf prop is not public --jglick
*
* <P>
* <TABLE BORDER COLS=3 WIDTH=100%>
* <TR><TH WIDTH=15%>Property<TH WIDTH=15%>Property Type<TH>Description
* <TR><TD> Enabled  <TD> boolean   <TD> The explicite enabled/disabled
*                                       state of the action.
* <TR><TD> ActionPerformer  <TD> ActionPerformer  <TD> The class that performs the action
* </TABLE>
*/
/** Action that can have a performer of the action attached to it at any time,
* or changed.
* The action will be automatically disabled
* when it has no performer.
* <p>Also may be made sensitive to changes in window focus.
* @author   Ian Formanek, Jaroslav Tulach, Petr Hamernik
*/
public abstract class CallbackSystemAction extends CallableSystemAction
implements ContextAwareAction {
    /** action performer */
    private static final String PROP_ACTION_PERFORMER = "actionPerformer"; // NOI18N
    
    /** a list of all actions that has survive focus change set to false */
    private static final WeakSet notSurviving = new WeakSet (37);
    /** a list of CallableSystemAction actions surviving focus change */
    private static final WeakSet surviving = new WeakSet (37);
    
    
    /** key to access listener */
    private static final Object LISTENER = new Object ();

    /** If the IDE is running (with the window system) we remember
     * a reference to the registry.
     */
    private static Registry registry;

    // try to initialize as soon as this class is loaded,
    // getRegistry adds a listener to java.awt.Toolkit
    // we have to ensure that if the IDE is running (more precisely
    // window system) the next calls to getRegistry remove this
    // ugly listener from Toolkit.
    static {
        getRegistry();
    }
    
    /** If there is no registry we keep reference to an ExplorerManager.
     */
    private static ExplorerManager explorerManager;
    private static FocusTracker ft;
    
    static final long serialVersionUID =-6305817805474624653L;
    /** Initialize the action to have no performer.
    */
    protected void initialize () {
        super.initialize ();
        updateEnabled ();
        setSurviveFocusChange(false);
    }


    /** Get the current action performer.
    * @return the current action performer, or <code>null</code> if there is currently no performer
    */
    public ActionPerformer getActionPerformer() {
        return (ActionPerformer)getProperty (PROP_ACTION_PERFORMER);
    }

    /** Set the action performer.
    * The specified value can be <code>null</code>, which means that the action will have no performer
    * and is disabled. ({@link #isEnabled} will return <code>false</code> regardless its previous state.)
    * <P>
    * This method is <em>too dynamic</em> it depends on the actuall order of callers and
    * is for example very fragile with respect to focus switching and correct delivering of
    * focus change events. That is why an alternative based on 
    * <a href="http://openide.netbeans.org/proposals/actions/design.html#callback">ActionMap proposal</a> 
    * has been developed.
    * <P>
    * So if you are providing a {@link TopComponent} and want to provide
    * your own handling of {@link org.openide.actions.CopyAction} use following code:
    * <PRE>
    * TopComponent tc = ...;
    * javax.swing.Action yourCopyAction = ...; // the action to invoke instead of Copy
    *
    * CopyAction globalCopyAction = SystemAction.get (CopyAction.class);
    * Object key = globalCopyAction.getActionMapKey(); // key is a special value defined by all CallbackSystemActions
    *
    * // and finally:
    * tc.getActionMap ().put (key, yourCopyAction);
    * </PRE>
    * This code registers <code>yourCopyAction</code> with <code>tc</code>
    * top component, so whenever a <code>globalCopyAction</code> is invoked,
    * your action is being delegated to.
    *
    * @param performer the new action performer or <code>null</code> to disable
    *
    * @deprecated use TopComponent.getActionMap() as described in the javadoc 
    */
    public void setActionPerformer(ActionPerformer performer) {
        putProperty (
                                       PROP_ACTION_PERFORMER, performer
                                   );
        updateEnabled ();
    }
    
    /** Updates the enabled state by checking performer and ActionMap
     */
    private void updateEnabled () {
        javax.swing.Action action = findGlobalContextAction ();
        if(action != null) {
            setEnabled (action.isEnabled());

            synchronized (LISTENER) {
                ActionDelegateListener l = (ActionDelegateListener)getProperty(LISTENER);
                if (l == null) {
                    l = new ActionDelegateListener (this, action);
                    putProperty (LISTENER, l);
                } else {
                    l.attach (action);
                }
            }
        } else {
            if (getActionPerformer () != null) {
                // we have performer
                setEnabled (true);
            } else {
                setEnabled (false);
            }
            clearListener ();
        }
    }
    
    /** Clears the listener.
     */
    private void clearListener () {
        synchronized (LISTENER) {
            // remove listener on any action
            ActionDelegateListener l = (ActionDelegateListener)getProperty(LISTENER);
            if (l != null) {
                l.clear ();
                putProperty (LISTENER, null);
            }
        }
    }
    
    /** Perform the action. Tries the performer and then scans the ActionMap
     * of selected topcomponent.
     */
    public void actionPerformed(java.awt.event.ActionEvent ev) {
        // First try global context action.
        javax.swing.Action action = findGlobalContextAction ();
        if (action != null) {
            action.actionPerformed(ev);
            return;
        }

        ActionPerformer ap = getActionPerformer ();
        if (ap != null) {
            ap.performAction (this);
            return;
        }
    }
    
    /** Finds global context action (the one from activated component).
     * @return action for key of activated component or null
     */
    private javax.swing.Action findGlobalContextAction () {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        if (tc != null) {
            javax.swing.ActionMap map = tc.getActionMap();
            if (map != null) {
                Object key = getActionMapKey();
                return map.get (key);
            }
        }
        
        return null;
    }

    /** Perform the action.
    * This default implementation calls the assigned action performer if it
    * exists, otherwise does nothing.
    */
    public void performAction() {
        ActionPerformer ap = getActionPerformer ();
        if (ap != null) ap.performAction (this);
    }
    
    /** Getter for action map key, which is used to find action from provided
     * context (i.e. <codeActionMap</code> provided by the context),
     * which acts as a callback.
     * Override this method in subclasses to provide 'nice' key.
     * @return key which is used to find the action which performs callback,
     *      default returned key is a class name.
     * @since 3.29 */
    public Object getActionMapKey () {
        return getClass ().getName ();
    }

    /** Test whether the action will survive a change in focus.
    * By default, it will not.
    * @return <code>true</code> if the enabled state of the action survives focus changes
    */
    public boolean getSurviveFocusChange () {
        getProperty(null); // force initialization
        return !notSurviving.contains (getClass ());
    }
    
    /** Implements <code>ContextAwareAction</code> interface method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }
    
    

    /** Set whether the action will survive a change in focus.
    * If <code>false</code>, then the action will be automatically
    * disabled (using {@link #setActionPerformer}) when the window
    * focus changes.
    *
    * @param b <code>true</code> to survive focus changes, <code>false</code> to be sensitive to them
    */
    public void setSurviveFocusChange (boolean b) {
        synchronized (notSurviving) {
            if (b) {
                notSurviving.remove (getClass ());
                surviving.add (getClass ());
            } else {
                notSurviving.add (getClass ());
                surviving.remove (getClass ());
            }
        }
    }

    /** Getter for component registry.
     * @returns active registry or null if there is none
     */
    static Registry getRegistry() {
        if (registry != null) {
            return registry;
        }
        
        try {
            Class c = Class.forName("org.openide.windows.TopComponent$Registry"); // NOI18N
            registry = (Registry)Lookup.getDefault().lookup(c);
        } catch (Exception x) {
            // ignore any exception and return null
        }
        if ((registry == null) && (ft == null)) {
            startTrackingFocus();
        }
        if (registry != null) {
            // lookup succesfull --> no need to globally listen on focus changes
            stopTrackingFocus();
            registry.addPropertyChangeListener (new FocusTracker ());
        }
        
        return registry; // if the IDE is present we should return non null
    }

    /** If there is no TopComponent.Registry use this
     * instance of explorerManager to get selected nodes.
     */
    static ExplorerManager getExplorerManager() {
        return explorerManager;
    }
    
    /** Setter for the current explorer manager.*/
    static void setExplorerManager(ExplorerManager e) {
        explorerManager = e;
    }
    
    /** Attach a listener (FocusTracker) to global event delivery
     * mechanism (Toolkit).
     */
    private static void startTrackingFocus() {
        ft = new FocusTracker();
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
            ft, AWTEvent.FOCUS_EVENT_MASK);
    }
    
    /** Unregisters FocusTracker from Toolkit. */
    private static void stopTrackingFocus() {
        if (ft != null) {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(ft);
            ft = null;
        }
    }
    
    /** Array of actions from a set of classes.
     */
    private static ArrayList toInstances (java.util.Set s) {
        ArrayList actions;
        
        synchronized (notSurviving) {
            actions = new ArrayList (s.size ());
            
            Iterator it = s.iterator ();
            while (it.hasNext ()) {
                Class c = (Class)it.next ();

                Object a = SystemAction.findObject (c, false);
                if (a != null) {
                    actions.add (a);
                }
            }
        }
        
        return actions;
    }

    /** Clears all action performers for those that has setSurviveFocusChange
     * on true.
     */
    private static void clearActionPerformers () {
        ArrayList actions = toInstances (notSurviving);
        
        // clear the performers out of any loop
        Iterator it = actions.iterator ();
        while (it.hasNext ()) {
            CallbackSystemAction a = (CallbackSystemAction)it.next ();
            a.setActionPerformer (null);
        }
        
        actions = toInstances (surviving);
        
        // clear the performers out of any loop
        it = actions.iterator ();
        while (it.hasNext ()) {
            CallbackSystemAction a = (CallbackSystemAction)it.next ();
            a.updateEnabled ();
        }
    }
    
    /** This listener is attached to java.awt.Toolkit. If window
     * manager is present (if it registers TopComponent.Registry
     * in global lookup (Lookup.getDefault()) this listener should
     * be immediatelly detached from the Toolkit.
     */
    private static class FocusTracker implements AWTEventListener, PropertyChangeListener {
        FocusTracker() {}
	
        /**
         * Invoked when an event is dispatched in the AWT.
         */
        public void eventDispatched(AWTEvent event) {
            if (event instanceof FocusEvent) {
                FocusEvent fe = (FocusEvent)event;
                if ((fe.getID() & FocusEvent.FOCUS_GAINED) == 0) {
                    // if it is not focus gained --> return
                    return;
                }
            }
            Object source = event.getSource();
            if (source instanceof Component) {
                Component c = (Component)source;
                while ((c != null) && (! (c instanceof ExplorerManager.Provider))) {
                    c = c.getParent();
                }
                if (c instanceof ExplorerManager.Provider) {
                    setExplorerManager(((ExplorerManager.Provider)c).getExplorerManager());
                }
            }
        }
        
        public void propertyChange(java.beans.PropertyChangeEvent ev) {
            if (Registry.PROP_ACTIVATED.equals(ev.getPropertyName())) {
                // deletes the performer
                clearActionPerformers ();
            }
        }
    } // end of FocusTracker
    
    /** A class that listens on changes in enabled state of an action
     * and updates the state of the action according to it.
     */
    private static final class ActionDelegateListener extends Object
    implements PropertyChangeListener {
        private CallbackSystemAction action;
        private javax.swing.Action delegate;
        
        public ActionDelegateListener (CallbackSystemAction c, javax.swing.Action delegate) {
            this.action = c;
            this.delegate = delegate;
            
            
            delegate.addPropertyChangeListener(this);
        }
        
        public void clear () {
            javax.swing.Action a;
            
            a = delegate;
            if (a == null) return;
            delegate = null;
            
            a.removePropertyChangeListener (this);
        }
        
        public void attach (javax.swing.Action action) {
            if (delegate == action) {
                return;
            }
            
            // reattaches to different action
            if (this.delegate != null) {
                this.delegate.removePropertyChangeListener(this);
            }
            
            this.delegate = action;
            action.addPropertyChangeListener(this);
        }
            
        
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            synchronized (LISTENER) {
                if (delegate == null) return;
            }
            
            action.updateEnabled();
        }
    }
    
    
    /** A delegate action that is usually associated with a specific lookup and
     * extract the nodes it operates on from it. Otherwise it delegates to the
     * regular NodeAction.
     */
    private static final class DelegateAction extends Object
    implements javax.swing.Action, org.openide.util.LookupListener,
    Presenter.Menu, Presenter.Popup, Presenter.Toolbar, PropertyChangeListener {
        /** action to delegate too */
        private CallbackSystemAction delegate;
        /** lookup we are associated with (or null) */
        private org.openide.util.Lookup.Result result;
        /** previous state of enabled */
        private boolean enabled;
        /** support for listeners */
        private PropertyChangeSupport support = new PropertyChangeSupport (this);
        /** listener to check listen on state of action(s) we delegate to */
        private PropertyChangeListener weakL;
        /** last action we were listening to */
        private javax.swing.Action last;
        
        public DelegateAction (CallbackSystemAction a, Lookup actionContext) {
            this.delegate = a;
            this.weakL = org.openide.util.WeakListener.propertyChange (this, null);
            this.enabled = a.getActionPerformer () != null;
            
            this.result = actionContext.lookup (new org.openide.util.Lookup.Template (
                javax.swing.ActionMap.class
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
        
        /** Invoked when an action occurs.
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            javax.swing.Action a = findAction ();
            if (a != null) {
                a.actionPerformed(e);
            } else {
                // XXX #30303 if the action falls back to the old behaviour
                // it may not be performed in case it is in dialog and
                // is not transmodal. 
                // This is just a hack, see TopComponent.processKeyBinding.
                Object source = e.getSource();
                if(source instanceof Component
                && javax.swing.SwingUtilities.getWindowAncestor((Component)source)
                instanceof java.awt.Dialog) {
                    Object value = delegate.getValue(
                        "OpenIDE-Transmodal-Action"); // NOI18N
                    if(!Boolean.TRUE.equals(value)) {
                        return;
                    }
                }
                
                delegate.actionPerformed (e);
            }
        }
        
        public boolean isEnabled() {
            javax.swing.Action a = findAction ();
            if (a == null) {
                a = delegate;
            }
            
            if (a != last) {
                if (last != null) {
                    last.removePropertyChangeListener (weakL);
                }
                last = a;
                last.addPropertyChangeListener (weakL);
            }
            
            return a.isEnabled();
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
        
        public void setEnabled(boolean b) {
        }

        public void resultChanged(org.openide.util.LookupEvent ev) {
            boolean newEnabled = isEnabled ();
            
            if (newEnabled != enabled) {
                support.firePropertyChange (PROP_ENABLED, enabled, newEnabled);
                enabled = newEnabled;
            }
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            resultChanged (null);
        }
            
        /*** Finds an action that we should delegate to
         * @return the action or null
         */
        private javax.swing.Action findAction () {
            java.util.Collection c = result != null ? 
                result.allInstances() : java.util.Collections.EMPTY_LIST;
            if (!c.isEmpty()) {
                Object key = delegate.getActionMapKey();

                for(Iterator it = c.iterator(); it.hasNext(); ) {
                    javax.swing.ActionMap map = (javax.swing.ActionMap)it.next();
                    javax.swing.Action action = map.get (key); 
                    if(action != null) {
                        return action;
                    }
                }
            }
            
            return null;
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
        
        private boolean isMethodOverriden (CallableSystemAction d, String name) {
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
