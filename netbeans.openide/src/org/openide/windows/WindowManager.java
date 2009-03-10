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

import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Set;

import org.openide.nodes.Node;
import org.openide.util.Lookup;

/** Manager of windows in the IDE.
* Handles the work with workspaces, allows to listen to
* workspace changes.
*
* @author Jaroslav Tulach
*/
public abstract class WindowManager extends Object implements Serializable {
    /** property change of workspaces */
    public static final String PROP_WORKSPACES = "workspaces"; // NOI18N
    /** property change of current workspace */
    public static final String PROP_CURRENT_WORKSPACE = "currentWorkspace"; // NOI18N
    /** The top component which is currently active */
    private TopComponent activeComponent;
    /** the registry */
    private TopComponent.Registry registry;
    /** instance of window manager installed in the system */
    private static WindowManager wmInstance;

    static final long serialVersionUID =-4133918059009277602L;
    
    /** Singleton instance accessor method for window manager. Provides entry
     * point for further work with window system API of the system. 
     *
     * @return instance of window manager installed in the system
     * @since 2.10
     */
    public static final WindowManager getDefault () {
        synchronized (WindowManager.class) {
            if (wmInstance == null) {
                wmInstance = (WindowManager)Lookup.getDefault().lookup(WindowManager.class);
                if (wmInstance == null) {
                    wmInstance = new DummyWindowManager();
                }
            }
            return wmInstance;
        }
    }
    
    /** Get the Main Window of the IDE.
    * This should ONLY be used for:
    * <UL>
    *   <LI>using the Main Window as the parent for dialogs</LI>
    *   <LI>using the Main Window's position for preplacement of windows</LI>
    * </UL>
    * @return the Main Window of the IDE
    */
    public abstract java.awt.Frame getMainWindow ();

    /** Called after a Look&amp;Feel change to update the IDE's UI.
    * Should call {@link javax.swing.JComponent#updateUI} on all opened windows.
    */
    public abstract void updateUI ();

    /** Create a component manager for the given top component.
    * @param c the component
    * @return the manager to handle opening, closing and selecting the component
    */
    protected abstract WindowManager.Component createTopComponentManager (TopComponent c);

    /** Access method for registry of all components in the system.
    * @return the registry 
    */
    protected TopComponent.Registry componentRegistry () {
        return (TopComponent.Registry)org.openide.util.Lookup.getDefault ().lookup (TopComponent.Registry.class);
    }

    /** Getter for component registry.
    * @return the registry
    */
    public TopComponent.Registry getRegistry () {
        if (registry != null) {
            return registry;
        }

        synchronized (this) {
            if (registry == null) {
                registry = componentRegistry ();
            }
        }

        return registry;
    }

    /** Creates new workspace.
    * @deprecated please use method createWorkspace(String name, String displayName) instead
    * @param name the name of the workspace
    * @return new workspace
    */
    public final Workspace createWorkspace (String name) {
        return createWorkspace(name, name);
    }

    /** Creates new workspace.
    * Note that it will not be displayed until {@link #setWorkspaces} is called
    * with an array containing the new workspace.
    * @param name the code name (used for lookup)
    * @param displayName the display name
    */
    public abstract Workspace createWorkspace (String name, String displayName);

    /** Finds workspace given its name.
    * @param name the name of workspace to find
    * @return workspace or null if not found
    */
    public abstract Workspace findWorkspace (String name);

    /** List of all workspaces.
    */
    public abstract Workspace[] getWorkspaces ();

    /** Sets new array of workspaces.
    * In conjunction with {@link #getWorkspaces}, this may be used to reorder
    * workspaces, or add or remove workspaces.
    * @param workspaces An array consisting of new workspaces.
    */
    public abstract void setWorkspaces (Workspace[] workspaces);

    /** Current workspace. Can be changed by calling Workspace.activate ()
    */
    public abstract Workspace getCurrentWorkspace ();

    //
    // You can add implementation to this class (+firePropertyChange), or implement it in subclass
    // Do as you want.
    //

    /** Attaches listener for changes in workspaces
    */
    public abstract void addPropertyChangeListener (PropertyChangeListener l);

    /** Removes listener.
    */
    public abstract void removePropertyChangeListener (PropertyChangeListener l);

    /** Finds top component manager for given top component.
    * @param tc top component to find manager for.
    * @return component manager for given top component.
    */
    protected static final Component findComponentManager (TopComponent tc) {
        return tc.getManager();
    }

    /** Activate a component. The top component containers should inform
    * the top component that it is active via a call to this method through
    * derived window manager implementation.
    * @param tc the top component to activate;
    * or <code>null</code> to deactivate all top components
    */
    protected void activateComponent (TopComponent tc) {
        // check
        if (activeComponent == tc) return;
        // deactivate old if possible
        if (activeComponent != null)
            activeComponent.componentDeactivated();
        activeComponent = tc;
        if (activeComponent != null)
            activeComponent.componentActivated();
    }

    /** Notifies component that it was opened (and wasn't opened on any
     * workspace before). Top component manager that implements Component
     * inner interface of this class should send open notifications via
     * calling this method
     * @param tc the top component to be notified
     */
    protected void componentOpenNotify (TopComponent tc) {
        tc.componentOpened();
    }
    
    /** Notifies component that it was closed (and is not opened on any
     * workspace anymore). Top component manager that implements Component
     * inner interface of this class should send close notifications via
     * calling this method
     * @param tc the top component to be notified
     */
    protected void componentCloseNotify (TopComponent tc) {
        tc.componentClosed();
    }
    
    /** Notifies <code>TopComponent</code> it is about to be shown.
     * @param tc <code>TopComponent</code> to be notified
     * @see TopComponent#componentShowing
     * @since 2.18 */
    protected void componentShowing(TopComponent tc) {
        tc.componentShowing();
    }
    
    /** Notifies <code>TopComponent</code> it was hidden.
     * @param tc <code>TopComponent</code> to be notified
     * @see TopComponent#componentHidden
     * @since 2.18 */
    protected void componentHidden(TopComponent tc) {
        tc.componentHidden();
    }
    

    /** A manager that handles operations on top components.
    * It is always attached to a {@link TopComponent}.
    */
    protected interface Component extends java.io.Serializable {

        /** Open the component on current workspace */
        public void open ();

        /** Open the component on given workspace in the right mode.
        */
        public void open (Workspace workspace);

        /** Close the component on given workspace.
        */
        public void close (Workspace workspace);

        /** Called when the component requests focus. Moves it to be visible.
        */
        public void requestFocus ();
        
        /** Set this component visible but not selected or focused if possible.
        * If focus is in other container (multitab) or other pane (split) in 
        * the same container it makes this component only visible eg. it selects 
        * tab with this component.
        * If focus is in the same container (multitab) or in the same pane (split)
        * it has the same effect as requestFocus().
        */
        public void requestVisible ();

        /** Get the set of activated nodes.
        * @return currently activated nodes for this component
        */
        public Node[] getActivatedNodes ();

        /** Set the set of activated nodes for this component.
        * @param nodes new set of activated nodes
        */
        public void setActivatedNodes (Node[] nodes);

        /** Called when the name of the top component changes.
        */
        public void nameChanged ();

        /** Set the icon of the top component.
        * @param icon the new icon
        */
        public void setIcon (final Image icon);

        /** @return the icon of the top component */
        public Image getIcon ();

        /** @return the set of workspaces where managed component is open */
        public Set whereOpened ();

        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = 0L;

    }
}
