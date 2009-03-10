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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.beans.*;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Externalizable;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.Keymap;

import org.openide.ErrorManager;
import org.openide.awt.UndoRedo;
import org.openide.loaders.*;
import org.openide.actions.*;
import org.openide.util.actions.SystemAction;
import org.openide.nodes.*;
import org.openide.util.ContextAwareAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;

import org.openide.modules.Dependency;
import org.openide.modules.SpecificationVersion;

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
 * @author Jaroslav Tulach, Petr Hamernik, Jan Jancura
 */
public class TopComponent extends JComponent 
implements Externalizable, Accessible, HelpCtx.Provider, Lookup.Provider {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -3022538025284122942L;

    /** Behavior in which a top component closed (by the user) in one workspace
    * will be removed from <em>every</em> workspace.
    * Also, {@link #close} is called.
    * This is appropriate for top components such as Editor panes which
    * the user expects to really close (and prompt to save) when closed
    * in any workspace.
    */
    public static final int CLOSE_EACH = 0;
    /** Behavior in which a top component closed (by the user) in one workspace
    * may be left in other workspaces.
    * Only when the last remaining manifestation in any workspace is closed
    * will the object be deleted using {@link #close}.
    * Appropriate for components containing no user data, for which closing
    * the component is only likely to result from the user's wanting to remove
    * it from active view (on the current workspace).
    */
    public static final int CLOSE_LAST = 1;

    /** a set of actions of this component */
    private static SystemAction[] DEFAULT_ACTIONS;
    
    /** a lock for operations in default impl of getLookup */
    private static Object defaultLookupLock = new Object ();
    
    /** reference to Lookup with default implementation for the 
     * component
     */
    private java.lang.ref.Reference defaultLookupRef = new WeakReference(null);

    /** Listener to the data object's node or null */
    private NodeName nodeName;

    /** manager for the component */
    private final WindowManager.Component manager;

    /** constant for desired close operation */
    private int closeOperation = CLOSE_LAST;
    
    /** identification of serialization version
    * Used in CloneableTopComponent readObject method.
    */
    short serialVersion = 1;

    /** Create a top component.
    */
    public TopComponent () {
        enableEvents (java.awt.AWTEvent.KEY_EVENT_MASK);
        
        // there is no reason why a top component should have a focus
        // => let's disable it
        if (Dependency.JAVA_SPEC.compareTo(new SpecificationVersion("1.4")) >= 0) {
            try {
                Method method = getClass().getMethod("setFocusable", new Class[] { Boolean.TYPE });
                method.invoke(this, new Object[] { new Boolean(false) });
            }
            catch (Exception ex) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
        } else {
            setRequestFocusEnabled (false);
        }
        // request creating of our manager - it's here to avoid
        // problems with recreating the connections between top components
        // and their managers during deserialization
        manager = WindowManager.getDefault().createTopComponentManager(this);
    }
    
    /** Create a top component associated with a data object.
    * Currently the data object is used to set the component's name
    * (which will be updated according to the object's node delegate) by
    * installing NodeName inner class and attaching it to the node delegate.
    * 
    * @param obj the data object
    */
    public TopComponent (DataObject obj) {
        this ();
        Node n = obj.getNodeDelegate ();

        nodeName = new NodeName (this);
        nodeName.attach (n);

        getAccessibleContext().setAccessibleDescription(n.getDisplayName());
    }

    /** Getter for class that allows obtaining of information about components.
    * It allows to find out which component is selected, which nodes are 
    * currently or has been activated and list of all components.
    * 
    * @return the registry of components
    */
    public static final Registry getRegistry () {
        return WindowManager.getDefault().getRegistry();
    }

    /** Get the set of activated nodes in this component.
    * @return the activated nodes for this component
    */
    public final Node[] getActivatedNodes () {
        return getManager ().getActivatedNodes ();
    }

    /** Set the set of activated nodes in this component.
    * @param nodes activated nodes for this component
    */
    public final void setActivatedNodes (Node[] nodes) {
        getManager ().setActivatedNodes (nodes);
        firePropertyChange ("activatedNodes", null, null); // NOI18N
    }

    /** Get the undo/redo support for this component.
    * The default implementation returns a dummy support that cannot
    * undo anything.
    *
    * @return undoable edit for this component
    */
    public UndoRedo getUndoRedo () {
        return UndoRedo.NONE;
    }

    /** Show the component on current workspace.
    * Note that this method only makes it visible, but does not
    * give it focus. Implemented via call to open(null).
    * @see #requestFocus
    */
    public void open () {
        open(null);
    }

    /** Show the component on given workspace. If given workspace is
    * not active, component will be shown only after given workspace
    * will become visible.
    * Note that this method only makes it visible, but does not
    * give it focus.
    * @param workspace Workspace on which component should be opened.
    * Parameter can be null -> means current workspace.
    * @see #requestFocus
    */
    public void open (Workspace workspace) {
        getManager().open(workspace);
    }

    /** Finds out if this top component is opened at least on one workspace.
    * @return true if given top component is opened on at least
    * one workspace, false otherwise */
    public final boolean isOpened () {
        return getManager().whereOpened().size() > 0;
    }

    /** Finds out whether this top component is opened or not on specified
    * workspace.
    * @return true if given top component is opened on given workspace,
    * false otherwise */
    public final boolean isOpened (Workspace workspace) {
        return getManager().whereOpened().contains(workspace);
    }
    
    public void addNotify() {
        super.addNotify();
        if (!(getParent() instanceof TopComponent)) {
            //(TDB) Note: The constant below is defined for reference in NbTheme, but
            //since NbTheme is in core, cannot create a dependancy.
            setBorder (javax.swing.UIManager.getBorder ("nb.TopComponent.border"));
        }
    }

    /** Closes the top component on current workspace.
    * First asks canClose() method to see if it is
    * possible to close now. If canClose() returns false, component will not
    * be closed.
    * Semantics of this method depends on top component's closeOperation
    * state. If closeOperation is set to CLOSE_LAST (default), top component
    * will be closed only on current workspace. If it is set to
    * CLOSE_EACH, if will be closed on all workspaces at once.
    *
    * @return true if top component was succesfully closed, false if 
    * top component for some reason refused to close.
    */
    public final boolean close () {
        return close(WindowManager.getDefault().getCurrentWorkspace());
    }

    /** Closes the top component on given workspace, if closeOperation
    * is set to CLOSE_LAST. If it is set to CLOSE_EACH, given parameter 
    * will be ignored and component will be closed on all workspaces
    * at once.
    *
    * @param workspace Workspace on which component should be closed.
    * @return true if top component was succesfully closed, false if 
    * top component for some reason refused to close.
    */
    public final boolean close (Workspace workspace) {
        Set whereOpened = getManager().whereOpened();
        // don't close multiple times
        if ((closeOperation != CLOSE_EACH) && !whereOpened.contains(workspace))
            return true;
        boolean result;
        switch (closeOperation) {
        case CLOSE_LAST:
            result = canClose(workspace, whereOpened.size() == 1);
            break;
        case CLOSE_EACH:
            result = canClose(null, true);
            break;
        default:
            throw new IllegalStateException ("closeOperation=" + closeOperation); // NOI18N
        }
        if (result)
            getManager().close(workspace);
        return result;
    }


    /** This method is called when top component is about to close.
    * Allows subclasses to decide if top component is ready for closing
    * or not.<br>
    * Default implementation always return true.
    * 
    * @param workspace the workspace on which we are about to close or
    *                  null which means that component will be closed
    *                  on all workspaces where it is opened (CLOSE_EACH mode)
    * @param last true if this is last workspace where top component is
    *             opened, false otherwise. If close operation is set to
    *             CLOSE_EACH, then this param is always true
    * @return true if top component is ready to close, false otherwise.
    */
    public boolean canClose (Workspace workspace, boolean last) {
        return true;
    }
    
    /** Called only when top component was closed on all workspaces before and
     * now is opened for the first time on some workspace. The intent is to
     * provide subclasses information about TopComponent's life cycle across
     * all existing workspaces.
     * Subclasses will usually perform initializing tasks here.
     * @deprecated Use {@link #componentOpened} instead. */
    protected void openNotify () {
    }
    
    /** Called only when top component was closed so that now it is closed
     * on all workspaces in the system. The intent is to provide subclasses
     * information about TopComponent's life cycle across workspaces.
     * Subclasses will usually perform cleaning tasks here.
     * @deprecated Use {@link #componentClosed} instead.
     */
    protected void closeNotify () {
    }
    
    /** Gets the system actions which will appear in the popup menu of this component.
     * @return array of system actions for this component
     * @deprecated Use {@link #getActions()} instead.
     */
    public SystemAction[] getSystemActions () {
        // lazy inicialization
        synchronized(TopComponent.class) {
            if (DEFAULT_ACTIONS == null) {
                DEFAULT_ACTIONS = new SystemAction[] {
                    SystemAction.get(SaveAction.class),
                    SystemAction.get(CloneViewAction.class),
                    null,
                    SystemAction.get(CloseViewAction.class)
                };
            }
        }
        return DEFAULT_ACTIONS;
    }
    
    /** Gets the actions which will appear in the popup menu of this component.
     * <p>Subclasses are encouraged to override this method to specify
     * their own sets of actions.
     * <p>Remember to call the super method when overriding and add your actions
     * to the superclass' ones (in some order),
     * because the default implementation provides support for standard
     * component actions like save, close, and clone.
     * @return array of actions for this component
     * @since 3.32
     */
    public javax.swing.Action[] getActions() {
        return getSystemActions();
    }

    /** Set the close mode for the component.
    * @param closeOperation one of {@link #CLOSE_EACH} or {@link #CLOSE_LAST}
    * @throws IllegalArgumentException if an unrecognized close mode was supplied
    * @see #close()
    */
    public final void setCloseOperation (final int closeOperation) {
        if ((closeOperation != CLOSE_EACH) && (closeOperation != CLOSE_LAST))
            throw new IllegalArgumentException(
                NbBundle.getBundle(TopComponent.class).getString("EXC_UnknownOperation")
            );
        if (this.closeOperation == closeOperation) return;
        this.closeOperation = closeOperation;
        firePropertyChange ("closeOperation", null, null); // NOI18N
    }

    /** Get the current close mode for this component.
    * @return one of {@link #CLOSE_EACH} or {@link #CLOSE_LAST}
    */
    public final int getCloseOperation () {
        return closeOperation;
    }

    
    /** Called only when top component was closed on all workspaces before and
     * now is opened for the first time on some workspace. The intent is to
     * provide subclasses information about TopComponent's life cycle across
     * all existing workspaces.
     * Subclasses will usually perform initializing tasks here.
     * @since 2.18 */
    protected void componentOpened() {
        openNotify();
    }
    
    /** Called only when top component was closed so that now it is closed
     * on all workspaces in the system. The intent is to provide subclasses
     * information about TopComponent's life cycle across workspaces.
     * Subclasses will usually perform cleaning tasks here.
     * @since 2.18 */
    protected void componentClosed() {
        closeNotify();
    }

    /** Called when <code>TopComponent</code> is about to be shown.
     * Shown here means the component is selected or resides in it own cell
     * in container in its <code>Mode</code>. The container is visible and not minimized.
     * <p><em>Note:</em> component
     * is considered to be shown, even its container window
     * is overlapped by another window.</p>
     * @since 2.18 */
    protected void componentShowing() {
    }
    
    /** Called when <code>TopComponent</code> was hidden. <em>Nore</em>:
     * <p><em>Note:</em> Beside typical situations when component is hidden,
     * it is considered to be hidden even in that case
     * the component is in <code>Mode</code> container hierarchy,
     * the cointainer is visible, not minimized,
     * but the component is neither selected nor in its own cell,
     * i.e. it has it's own tab, but is not the selected one.
     * @since 2.18 */
    protected void componentHidden() {
    }
    
    /** Called when this component is activated.
    * This happens when the parent window of this component gets focus
    * (and this component is the preferred one in it), <em>or</em> when
    * this component is selected in its window (and its window was already focussed).
    * Remember to call the super method.
    * The default implementation does nothing.
    */
    protected void componentActivated () {
    }

    /** Called when this component is deactivated.
    * This happens when the parent window of this component loses focus
    * (and this component is the preferred one in the parent),
    * <em>or</em> when this component loses preference in the parent window
    * (and the parent window is focussed).
    * Remember to call the super method.
    * The default implementation does nothing.
    */
    protected void componentDeactivated () {
    }
    
    /** Request focus for the window holding this top component.
    * Also makes the component preferred in that window.
    * The component will <em>not</em> be automatically {@link #open opened} first
    * if it is not already.
    * <p>Subclasses should override this method to transfer focus to desired
    * focusable component. <code>TopComponent</code> itself is not focusable.
    * See for example {@link org.openide.text.CloneableEditor#requestFocus}.
    */
    public void requestFocus () {
        getManager().requestFocus();
        super.requestFocus();
    }
    
    /** Set this component visible but not selected or focused if possible.
    * If focus is in other container (multitab) or other pane (split) in 
    * the same container it makes this component only visible eg. it selects 
    * tab with this component.
    * If focus is in the same container (multitab) or in the same pane (split)
    * it has the same effect as requestFocus().
    */
    public void requestVisible () {
        getManager().requestVisible();
    }

    /** Set the name of this top component.
    * The default implementation just notifies the window manager.
    * @param name the new display name
    */
    public void setName (final String name) {
        String old = getName();
        if ((name != null) && (name.equals(old)))
            return;
        super.setName(name);
        firePropertyChange("name", old, name);

        getManager().nameChanged();
    }

    /** Sets toolTip for this <code>TopComponent</code>, adds notification
     * about the change to its <code>WindowManager.TopComponentManager</code>. */
    public void setToolTipText(String toolTip) {
        if(toolTip != null && toolTip.equals(getToolTipText())) {
            return;
        }
        
        super.setToolTipText(toolTip);
        // XXX #19428. Container updates name and tooltip in the same handler.
        getManager().nameChanged();
    }

    /** Set the icon of this top component.
    * The icon will be used for
    * the component's representation on the screen, e.g. in a multiwindow's tab.
    * The default implementation just notifies the window manager.
    * @param icon New components' icon.
    */
    public void setIcon (final Image icon) {
        getManager().setIcon(icon);
        firePropertyChange ("icon", null, null); // NOI18N
    }

    /** @return The icon of the top component */
    public Image getIcon () {
        return getManager().getIcon();
    }

    /** Get the help context for this component.
    * Subclasses should generally override this to return specific help.
    * @return the help context
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (TopComponent.class);
    }
    
    /** Allows top component to specify list of modes into which can be docked
     * by end user. Subclasses should override this method if they want to 
     * alter docking policy of top component. <p>
     * So for example, by returning empty list, top component refuses
     * to be docked anywhere. <p>
     * Default implementation allows docking anywhere by returning
     * input list unchanged.
     *
     * @param modes list of {@link Mode} which represent all modes of current
     * workspace, can contain nulls. Items are structured in logical groups
     * separated by null entries. <p>
     * Input array also contains special constant modes for docking
     * into newly created frames. Their names are "SingleNewMode", 
     * "MultiNewMode", "SplitNewMode", can be used for their
     * recognition. Please note that names and existence of special modes
     * can change in future releases.
     *
     * @return list of {@link Mode} which are available for dock, can contain nulls 
     * @since 2.14
     */
    public List availableModes (List modes) {
        return modes;
    }

    /** Overrides superclass method, adds possible additional handling of global keystrokes
     * in case this <code>TopComoponent</code> is ancestor of focused component. */
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
    int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);
        
        // XXX #30189 Reason of overriding: to process global shortcut.
        if(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT == condition
        && ret == false && !e.isConsumed()
        && (Dependency.JAVA_SPEC.compareTo(new SpecificationVersion("1.4")) >= 0)) { // NOI18N
            Keymap km = (Keymap)Lookup.getDefault().lookup(Keymap.class);
            Action action = km.getAction(ks);
            
            if(action == null) {
                return false;
            }
            
            // If necessary create context aware instance.
            if(action instanceof ContextAwareAction) {
                action = ((ContextAwareAction)action).createContextAwareInstance(getLookup());
            } else if(SwingUtilities.getWindowAncestor(e.getComponent())
            instanceof java.awt.Dialog) {
                // #30303 For 'old type' actions check the transmodal flag,
                // if invoked in dialog. See ShorcutAndMenuKeyEventProcessor in core.
                Object value = action.getValue("OpenIDE-Transmodal-Action"); // NOI18N
                if(!Boolean.TRUE.equals(value)) {
                    return false;
                }
            }
                
            // #30600 We have to use ActionManager, which replanes performation
            // of action to another thread, since our actions rely on that.
//            return SwingUtilities.notifyAction(
//                        action, ks, e, this, e.getModifiers());
            ActionManager am = (ActionManager)Lookup.getDefault().lookup(ActionManager.class);
            am.invokeAction(
                action,
                new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Utilities.keyToString(ks))
            );
            
            return true;
        } else {
            return ret;
        }
    }

    /** Getter for manager for this component. This manager allows to
    * control where is the component shown can be used to destroy and show the
    * component, etc.
    */
    final WindowManager.Component getManager () {
        return manager;
    }

    /** Serialize this top component.
    * Subclasses wishing to store state must call the super method, then write to the stream.
    * @param out the stream to serialize to
    */
    public void writeExternal (ObjectOutput out)
    throws IOException {
        out.writeObject(new Short (serialVersion));

        out.writeInt (closeOperation);
        out.writeObject (getName());
        out.writeObject (getToolTipText());

        Node.Handle h = nodeName == null ? null : nodeName.node.getHandle ();
        out.writeObject(h);
    }

    /** Deserialize this top component.
    * Subclasses wishing to store state must call the super method, then read from the stream.
    * @param in the stream to deserialize from
    */
    public void readExternal (ObjectInput in)
    throws IOException, ClassNotFoundException {
        Object firstObject = in.readObject ();
        if (firstObject instanceof Integer) {
            // backward compatibility read
            serialVersion = 0;

            closeOperation = ((Integer)firstObject).intValue();
            DataObject obj = (DataObject)in.readObject();

            super.setName((String)in.readObject());
            setToolTipText((String)in.readObject());

            // initialize the connection to a data object
            if (obj != null) {
                nodeName = new NodeName (this);
                nodeName.attach (obj.getNodeDelegate ());
            }
        } else {
            // new serialization
            serialVersion = ((Short)firstObject).shortValue ();

            closeOperation = in.readInt ();
            super.setName ((String)in.readObject ());
            setToolTipText ((String)in.readObject ());

            Node.Handle h = (Node.Handle)in.readObject ();
            if (h != null) {
                Node n = h.getNode ();
                nodeName = new NodeName (this);
                nodeName.attach (n);
            }
        }
        if (closeOperation != CLOSE_EACH && closeOperation != CLOSE_LAST) {
            throw new IOException ("invalid closeOperation: " + closeOperation); // NOI18N
        }
    }
    
    /** Delegates instance of replacer class to be serialized instead
    * of top component itself. Replacer class calls writeExternal and
    * constructor, readExternal and readResolve methods properly, so
    8 any top component can behave like any other externalizable object.
    * Subclasses can override this method to perform their
    * serialization differentrly */
    protected Object writeReplace () throws ObjectStreamException {
        return new Replacer(this);
    }
    
    /** Each top component that wishes to be cloned should implement
    * this interface, so CloneAction can check it and call the cloneComponent
    * method.
    */
    public static interface Cloneable {
        /** Creates a clone of this component
        * @return cloned component.
        */
        public TopComponent cloneComponent ();
    }

    /* Read accessible context
     * @return - accessible context
     */
    public AccessibleContext getAccessibleContext () {
        if(accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PANEL;
                }
                public String getAccessibleName() {
                    if (accessibleName != null) {
                        return accessibleName;
                    }
                    return getName();
                }
                /* Fix for 19344: Null accessible decription of all TopComponents on JDK1.4 */
                public String getToolTipText() {
                    return TopComponent.this.getToolTipText();
                }
            };
        }
        return accessibleContext;
    }

    /** Gets lookup which represents context of this component. By default
     * the lookup delegates to result of <code>getActivatedNodes</code>
     * method and result of this component <code>ActionMap</code> delegate.
     *
     * @return a lookup with designates context of this component
     * @see org.openide.util.ContextAwareAction
     * @see org.openide.util.Utilities#actionsToPopup(Action[], Lookup)
     * @since 3.29
     */
    public Lookup getLookup() {
        synchronized (defaultLookupLock) {
            Object l = defaultLookupRef.get ();
            if (l instanceof Lookup) {
                return (Lookup)l;
            }
            
            Lookup lookup = new ProxyLookup(new Lookup[] {
                new DefaultTopComponentLookup (this), // Lookup of activated nodes.
                Lookups.singleton(new DelegateActionMap(this)) // Action map lookup.
            });
                
            defaultLookupRef = new java.lang.ref.WeakReference (lookup);
            return lookup;
        }
    }
    
    /** This class provides the connection between the node name and
    * a name of the component.
    */
    public static class NodeName extends NodeAdapter {
        /** weak reference to the top component */
        private transient Reference top;
        /** node we are attached to or null */
        private transient Node node;

        /** Constructs new name adapter that
        * can be attached to any node and will listen on changes 
        * of its display name and modify the name of the component.
        *
        * @param top top compoonent to modify its name
        */
        public NodeName (TopComponent top) {
            this.top = new WeakReference (top);
        }

        /** Attaches itself to a given node.
        */
        final void attach (Node n) {
            TopComponent top = (TopComponent)this.top.get ();
            if (top != null) {
                synchronized (top) {
                    // ok no change
                    if (n == node) return;

                    // change the node we are attached to
                    if (node != null) {
                        node.removeNodeListener (this);
                    }
                    node = n;

                    if (n != null) {
                        n.addNodeListener (this);
                        top.setActivatedNodes (new Node[] { n });
                        top.setName (n.getDisplayName ());
                    }
                }
            }
        }


        /** Listens to Node.PROP_DISPLAY_NAME.
        */
        public void propertyChange(PropertyChangeEvent ev) {
            TopComponent top = (TopComponent)this.top.get ();
            if (top == null) {
                // stop listening if top component no longer exists
                if (ev.getSource () instanceof Node) {
                    Node n = (Node)ev.getSource ();
                    n.removeNodeListener (this);
                }
                return;
            }

            // ensure we are attached
            attach (node);

            if (ev.getPropertyName ().equals (Node.PROP_DISPLAY_NAME)) {
                top.setName (node.getDisplayName());
            }
        }
    } // end of NodeName

    /** Registry of all top components.
    * There is one instance that can be obtained via {@link TopComponent#getRegistry}
    * and it permits listening to the currently selected element, and to
    * the activated nodes assigned to it.
    */
    public static interface Registry {
        /** Name of property for the set of opened components. */
        public static final String PROP_OPENED = "opened"; // NOI18N
        /** Name of property for the selected top component. */
        public static final String PROP_ACTIVATED = "activated"; // NOI18N
        /** Name of property for currently selected nodes. */
        public static final String PROP_CURRENT_NODES = "currentNodes"; // NOI18N
        /** Name of property for lastly activated nodes nodes. */
        public static final String PROP_ACTIVATED_NODES = "activatedNodes"; // NOI18N

        /** Get reference to a set of all opened componets in the system.
        *
        * @return live read-only set of {@link TopComponent}s
        */
        public Set getOpened ();

        /** Get the currently selected element.
        * @return the selected top component, or <CODE>null</CODE> if there is none
        */
        public TopComponent getActivated ();

        /** Getter for the currently selected nodes.
        * @return array of nodes or null if no component activated or it returns
        *   null from getActivatedNodes ().
        */
        public Node[] getCurrentNodes ();

        /** Getter for the lastly activated nodes. Comparing
        * to previous method it always remembers the selected nodes
        * of the last component that had ones.
        *
        * @return array of nodes (not null)
        */
        public Node[] getActivatedNodes();

        /** Add a property change listener.
        * @param l the listener to add
        */
        public void addPropertyChangeListener (PropertyChangeListener l);

        /** Remove a property change listener.
        * @param l the listener to remove
        */
        public void removePropertyChangeListener (PropertyChangeListener l);
    }

    /** Instance of this class is serialized instead of TopComponent itself.
    * Emulates behaviour of serialization of externalizable objects
    * to keep TopComponent serialization compatible with previous versions. */
    private static final class Replacer implements Serializable {
        /** SUID */
        static final long serialVersionUID=-8897067133215740572L;

        /** Asociation with top component which is to be serialized using
        * this replacer */
        transient TopComponent tc;

        public Replacer (TopComponent tc) {
            this.tc = tc;
        }

        private void writeObject (ObjectOutputStream oos)
        throws IOException, ClassNotFoundException {
            // write the name of the top component first
            oos.writeObject(tc.getClass().getName());
            // and now let top component to serialize itself
            tc.writeExternal(oos);
        }

        private void readObject (ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
            // read the name of top component's class, instantiate it
            // and read its attributes from the stream
            String name = (String)ois.readObject();
            name = org.openide.util.Utilities.translate(name);
            try {
                Class tcClass = Class.forName(
                                    name,
                                    true,
                                    (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class)
                                );
                // instantiate class event if it has protected or private
                // default constructor
                java.lang.reflect.Constructor con = tcClass.getDeclaredConstructor(new Class[0]);
                con.setAccessible(true);
                try {
                    tc = (TopComponent)con.newInstance(new Object[0]);
                } finally {
                    con.setAccessible(false);
                }
                tc.readExternal(ois);
                // call readResolve() if present and use resolved value
                Method resolveMethod = findReadResolveMethod(tcClass);
                if (resolveMethod != null) {
                    // check exceptions clause
                    Class[] result = resolveMethod.getExceptionTypes();
                    if ((result.length == 1) &&
                            ObjectStreamException.class.equals(result[0])) {
                        // returned value type
                        if (Object.class.equals(resolveMethod.getReturnType())) {
                            // make readResolve accessible (it can have any access modifier)
                            resolveMethod.setAccessible(true);
                            // invoke resolve method and accept its result
                            try {
                                TopComponent unresolvedTc = tc;
                                tc = (TopComponent)resolveMethod.invoke(tc, new Class[0]);
                                if (tc == null) {
                                    throw new java.io.InvalidObjectException(
                                        "TopComponent.readResolve() cannot return null." // NOI18N
                                        + " See http://www.netbeans.org/issues/show_bug.cgi?id=27849 for more info." // NOI18N
                                        + " TopComponent:" + unresolvedTc); // NOI18N
                                }
                            } finally {
                                resolveMethod.setAccessible(false);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException exc) {
                //Bugfix #16408: Ignore missing classes from objectbrowser and icebrowser module
                if ((exc.getMessage().indexOf("org.netbeans.modules.objectbrowser") != -1) ||
                    (exc.getMessage().indexOf("org.netbeans.modules.icebrowser") != -1)) {
                    tc = null;
                } else {
                    // turn all troubles into IOException
                    IOException newEx = new IOException(exc.getMessage());
                    ErrorManager.getDefault().annotate(newEx, exc);
                    throw newEx;
                }
            } catch (Exception exc) {
                Throwable th = exc;
                // Extract target exception.
                if(th instanceof InvocationTargetException) {
                    th = ((InvocationTargetException)th).getTargetException();
                }
                // IOException throw directly.
                if(th instanceof IOException) {
                    throw (IOException)th;
                }
                // All others wrap into IOException.
                IOException newEx = new IOException(th.getMessage());
                ErrorManager.getDefault().annotate(newEx, th);
                throw newEx;
            }
        }
        
        /** Resolve to original top component instance */
        private Object readResolve () throws ObjectStreamException {
            return tc;
        }

        /** Tries to find readResolve method in given class. Finds
        * both public and non-public occurences of the method and
        * searches also in superclasses */
        private static Method findReadResolveMethod (Class clazz) {
            Method result = null;
            // first try public occurences
            try {
                result = clazz.getMethod("readResolve", new Class[0]); // NOI18N
            } catch (NoSuchMethodException exc) {
                // public readResolve does not exist
            }
            // now try non-public occurences; search also in superclasses
            for (Class i = clazz; i != null; i = i.getSuperclass()) {
                try {
                    result = i.getDeclaredMethod("readResolve", new Class[0]); // NOI18N
                    // get out of cycle if method found
                    break;
                } catch (NoSuchMethodException exc) {
                    // readResolve does not exist in current class
                }
            }
            return result;
        }

    } // end of Replacer inner class

}
