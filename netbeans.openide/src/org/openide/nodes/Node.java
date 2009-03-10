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

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.FeatureDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;

import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.event.EventListenerList;

import org.openide.ErrorManager;
import org.openide.util.datatransfer.NewType;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;


/** A <em>node</em> represents one element in a hierarchy of objects (beans).
* It provides all methods that are needed for communication between
* the IDE and the bean.
* <P>
* The node has three purposes:
* <OL>
*   <LI>visually represent the object in the tree hierarchy (i.e. Explorer)
*   <LI>provide sets of properties for that object (i.e. Component Inspector, Property Sheet)
*   <LI>offer actions to perform on itself
* </OL>
* <P>
* Frequently nodes are created to represent {@link org.openide.loaders.DataObject data objects}.
* But they may also represent anything to be displayed to the user or manipulated programmatically,
* even if they have no data directly stored behind them; for example, a control panel or debugger
* breakpoint.
* <P>
* There are two listeners in this class: {@link PropertyChangeListener}
* and {@link NodeListener} (which extends <code>PropertyChangeListener</code>). The first
* is designed to listen on properties that can be returned from
* {@link #getPropertySets}, the later for listening on changes in the
* node itself (including the name, children, parent, set of properties,
* icons, etc.). Be sure to distinguish between these two.
* <P>
* The node is cloneable. When a node is cloned, it is initialized
* with an empty set of listeners and no parent. The display name and short description
* are copied to the new node. The set of properties is <em>shared</em>.
* <P>
* Implements {@link org.openide.util.Lookup.Provider} since 3.11.
*
* @author Jaroslav Tulach,
*/
public abstract class Node extends FeatureDescriptor implements Lookup.Provider, HelpCtx.Provider {
    /** An empty leaf node. */
    public static final Node EMPTY = new AbstractNode (Children.LEAF);

    /* here is list of property names that can be changed in the Node object.
    * These properties can be notified to the <CODE>NodeListener</CODE>.
    */
    /** Property for node display name. */
    public static final String PROP_DISPLAY_NAME = "displayName"; // NOI18N
    /** Property for internal (not displayable) name of a node. This name is often used to
    * look up a node in the hierarchy, so it should be chosen to be simple.
    */
    public static final String PROP_NAME = "name"; // NOI18N
    /** Property for short description of a node. */
    public static final String PROP_SHORT_DESCRIPTION = "shortDescription"; // NOI18N
    /** Property for the normal (closed) icon of a node. */
    public static final String PROP_ICON = "icon"; // NOI18N
    /** Property for the opened icon of a node. */
    public static final String PROP_OPENED_ICON = "openedIcon"; // NOI18N
    /** Property for a node's parent. */
    public static final String PROP_PARENT_NODE = "parentNode"; // NOI18N
    /** Property for a node's list of property sets. */
    public static final String PROP_PROPERTY_SETS = "propertySets"; // NOI18N
    /** Property for a node's cookie set. */
    public static final String PROP_COOKIE = "cookie"; // NOI18N
    /** Property saying whether the Node is Leaf 
     *@since 3.1
     */
    public static final String PROP_LEAF = "leaf"; // NOI18N


    /** Error manager for used for logging */
    transient static ErrorManager err;
    
    /** cache of all created lookups */
    private static WeakHashMap lookups = new WeakHashMap (37);
    
    /** children representing parent node (Children or ChildrenArray), 
    * for synchronization reasons must be changed only
    * under the Children.MUTEX lock
    */
    private Object parent;
    
    /** children list, for synch. reasons change only under Children.MUTEX
    * lock
    */
    Children hierarchy;

    /** listeners for changes in hierarchy.
    */
    private transient EventListenerList listeners;

    /** Creates a new node with a given hierarchy of children.
    * @param h the children to use for this node
    * @exception IllegalStateException if the children object is already in use by
    *   a different node
    */
    protected Node(Children h) throws IllegalStateException {
        this (h, null);
    }
    
    /** Creates a new node with a given hierarchy of children and a lookup 
    * providing content for {@link #getCookie} and {@link #getLookup} methods.
    *
    * @since 3.11
    * @param h the children to use for this node
    * @param lookup the lookup to provide content of {@link #getLookup}
    *   and also {@link #getCookie}
    * @exception IllegalStateException if the children object is already in use by
    *   a different node
    */
    protected Node(Children h, Lookup lookup) throws IllegalStateException {
        this.hierarchy = h;
        
        // allow subclasses (FilterNode) to update the lookup
        lookup = replaceProvidedLookup (lookup);
        
        if (lookup != null) {
            this.listeners = new LookupEventList (lookup);
        } else {
            this.listeners = new EventListenerList ();
        }
        
        // attaches to this node
        h.attachTo (this);
        
        if (err == null) {
            err = ErrorManager.getDefault().getInstance("org.openide.nodes.Node"); //NOI18N
        }
    }
    
    /** Method for subclasses to modify provided lookup before its use.
     * This implementation does nothing.
     */
    Lookup replaceProvidedLookup (Lookup l) {
        return l;
    }
    
    /** Method that gives access to internal lookup. 
     * @param init should it be really initialized (ready for queries) or need not be
     * @return lookup or null
     */
    final Lookup internalLookup (boolean init) {
        if (listeners instanceof LookupEventList) {
            return ((LookupEventList)listeners).init (init);
        } else {
            return null;
        }
    }

    /** Implements {@link Object#clone} to behave correctly if cloning is desired.
    * But {@link Cloneable} is <em>not</em> declared by default.
    * <P>
    * The default implementation checks whether the child list implements
    * <code>Cloneable</code>, and if so, it clones the children.
    * If the child list does not support cloning, {@link Children#LEAF} is used
    * instead for the children. The parent of this node is set to <code>null</code> and an empty set
    * of listeners is attached to the node.
    *
    * @return the cloned version of the node
    * @exception CloneNotSupportedException if the children cannot be cloned
    *    in spite of implementing <code>Cloneable</code>
    */
    protected Object clone () throws CloneNotSupportedException {
        Node n = (Node)super.clone ();
        if (hierarchy instanceof Cloneable) {
            hierarchy = (Children)hierarchy.cloneHierarchy ();
        } else {
            hierarchy = Children.LEAF;
        }

        // attach the hierarchy
        hierarchy.attachTo (n);

        // no parent
        n.parent = null;
        // empty set of listeners
        
        if (listeners instanceof LookupEventList) {
            n.listeners = new LookupEventList (internalLookup (false));
        } else {
            n.listeners = new EventListenerList ();
        }

        return n;
    }

    /** Clone the node. The newly created node should reference the same
    * object is this node does, but it should not be inserted as a child
    * to any other node. Also it should have an empty set of listeners.
    * In all other respects the node should behave exactly as the
    * original one does.
    *
    * @return copy of this node
    */
    public abstract Node cloneNode ();

    
    /** Finds the children we are attached to.
     * @return children
     */
    private Children getParentChildren () {
        return this.parent instanceof ChildrenArray ? ((ChildrenArray)this.parent).getChildren () : (Children)this.parent;
    }

    /** Method that allows Children to change the parent children of
    * the node when the node is add to a children.
    *
    * @param parent the children that wants to contain this node
    * @param index index that will be assigned to this node
    * @exception IllegalStateException if this node already belongs to a children
    */
    final synchronized void assignTo (Children parent, int index) {
        Children ch = getParentChildren ();
        if (ch != null && ch != parent) {
            throw new IllegalStateException ("Cannot initialize " + index + "th child of node " + parent.getNode () + "; it already belongs to node " + ch.getNode ()); // NOI18N
        }
        if ( ! ( this.parent instanceof ChildrenArray ) ) { 
            this.parent = parent;
        }
    }
    
    /** Code that reasignes the reference from to parent from its
     * Children to its ChildrenArray.
     */
    final synchronized void reassignTo (Children currentParent, ChildrenArray itsArray) {
        if (this.parent != currentParent && this.parent != itsArray ) {
            throw new IllegalStateException ("Unauthorized call to change parent: " + this.parent + " and should be: " + currentParent);
        }
        this.parent = itsArray;
    }

    /** Deassignes the node from a children, when it is removed from
    * a children.
    */
    final synchronized void deassignFrom (Children parent) {
        Children p = getParentChildren ();
        if (parent != p) {
            throw new IllegalArgumentException ("Deassign from wrong parent. Old: " + p + " Caller: " + parent); //NOI18N
        }
        this.parent = null;
    }

    /** Set the system name. Fires a property change event.
    * @param s the new name
    * @exception IllegalArgumentException if the new name cannot represent
    *    a valid node name
    */
    public void setName (String s) {
        String name = super.getName ();
        if (name == null || !name.equals (s)) {
            super.setName (s);
            fireNameChange (name, s);
        }
    }

    /** Set the display name. Fires a property change event.
    * @param s the new name
    */
    public void setDisplayName (String s) {
        String displayName = super.getDisplayName ();
        if (displayName == null || !displayName.equals (s)) {
            super.setDisplayName (s);
            fireDisplayNameChange (displayName, s);
        }
    }

    /** Set the short description of the node. Fires a property change event.
    * <p>This description may be used for tool tips, etc.
    * @param s the new description
    */
    public void setShortDescription (String s) {
        String descr = super.getShortDescription ();
        if (descr == null || !descr.equals (s)) {
            super.setShortDescription (s);
            fireShortDescriptionChange (descr, s);
        }
    }

    /** Find an icon for this node (in the closed state).
    * @param type constant from {@link java.beans.BeanInfo}
    * @return icon to use to represent the node
    */
    public abstract Image getIcon (int type);

    /** Find an icon for this node (in the open state).
    * This icon is used when the node may have children and is expanded.
    *
    * @param type constant from {@link java.beans.BeanInfo}
    * @return icon to use to represent the node when open
    */
    public abstract Image getOpenedIcon (int type);

    /** Get context help associated with this node.
    * @return the context help object (could be <code>null</code> or {@link HelpCtx#DEFAULT_HELP})
    */
    public abstract HelpCtx getHelpCtx ();

    /** Get the list of children.
    * @return the children
    */
    public final Children getChildren () {
        return hierarchy;
    }
    
    /** Allows to change Children of the node. Call to this method aquires 
     * write lock on the nodes hierarchy. Take care not to call this method
     * under read lock.<BR>
     *
     * @param ch New children to be set on the node.
     * @since 3.1
     */
    protected final void setChildren( Children ch ) {
        try {
            Children.PR.enterWriteAccess ();
            
            Node[] oldNodes = null;
            
            if (hierarchy.isInitialized ()) {
                oldNodes = hierarchy.getNodes();
                hierarchy.detachFrom();
            }
            
            
            boolean wasLeaf = hierarchy == Children.LEAF;
            
            hierarchy = ch;
            hierarchy.attachTo( this );
            
            if ( wasLeaf != ( hierarchy == Children.LEAF ) ) {
                fireOwnPropertyChange (PROP_LEAF, 
                                        wasLeaf ? Boolean.TRUE : Boolean.FALSE, 
                                        hierarchy == Children.LEAF ? Boolean.TRUE : Boolean.FALSE);
            }
            
            if ( oldNodes != null && !wasLeaf ) {
                fireSubNodesChange (false, oldNodes, oldNodes);
                fireSubNodesChange (true, hierarchy.getNodes(), null);
            }
            
        } 
        finally {
            Children.PR.exitWriteAccess ();
        }
    }

    /** Test whether the node is a leaf, or may contain children.
    * @return <code>true</code> if the children list is actually {@link Children#LEAF}
    */
    public final boolean isLeaf () {
        return hierarchy == Children.LEAF;
    }

    /** Get the parent node.
    * @return the parent node, or <CODE>null</CODE> if this node is the root of a hierarchy
    */
    public final Node getParentNode () {
        // if contained in a list return its parent node
        Children ch = getParentChildren ();
        return ch == null ? null : ch.getNode ();
    }

    /** Test whether this node can be renamed.
    * If true, one can use {@link #getName} to obtain the current name and
    * {@link #setName} to change it.
    *
    * @return <code>true</code> if the node can be renamed
    */
    public abstract boolean canRename ();

    /** Test whether this node can be deleted.
    * @return <CODE>true</CODE> if can
    */
    public abstract boolean canDestroy ();

    // [PENDING] "valid" property?  --jglick // NOI18N
    /** Remove the node from its parent and deletes it.
    * The default
    * implementation obtains write access to
    * the {@link Children#MUTEX children's lock}, and removes
    * the node from its parent (if any). Also fires a property change.
    * <P>
    * This may be overridden by subclasses to do any additional
    * cleanup.
    *
    * @exception IOException if something fails
    */
    public void destroy () throws IOException {
        Children.MUTEX.postWriteRequest (new Runnable () {
                                             public void run () {
                                                 Children p = getParentChildren ();
                                                 if (p != null) {
                                                     // remove itself from parent
                                                     p.remove (new Node[] {Node.this} );
                                                 }
                                                 // sets the valid flag to false and fires prop. change
                                                 fireNodeDestroyed ();
                                             }
                                         });
    }


    /** Get the list of property sets for this node.
    * E.g. typically there may be one for normal Bean properties, one for expert
    * properties, and one for hidden properties.
    *
    * @return the property sets
    */
    public abstract PropertySet[] getPropertySets ();

    /** Called when a node is to be copied to the clipboard.
    * @return the transferable object representing the
    *    content of the clipboard
    * @exception IOException when the
    *    copy cannot be performed
    */
    public abstract Transferable clipboardCopy () throws IOException;

    /** Called when a node is to be cut to the clipboard.
    * @return the transferable object representing the
    *    content of the clipboard
    * @exception IOException when the
    *    cut cannot be performed
    */
    public abstract Transferable clipboardCut () throws IOException;

    /** Called when a drag is started with this node.
    * The node can attach a transfer listener to ExTransferable and
    * will be then notified about progress of the drag (accept/reject).
    *
    * @return transferable to represent this node during a drag
    * @exception IOException if a drag cannot be started
    */
    public abstract Transferable drag () throws IOException;

    /** Test whether this node permits copying.
    * @return <code>true</code> if so
    */
    public abstract boolean canCopy ();

    /** Test whether this node permits cutting.
    * @return <code>true</code> if so
    */
    public abstract boolean canCut ();

    /** Determine which paste operations are allowed when a given transferable is in the clipboard.
    * For example, a node representing a Java package will permit classes to be pasted into it.
    * @param t the transferable in the clipboard
    * @return array of operations that are allowed
    */
    public abstract PasteType[] getPasteTypes (Transferable t);

    /** Determine if there is a paste operation that can be performed
    * on provided transferable. Used by drag'n'drop code to check
    * whether the drop is possible.
    *
    * @param t the transferable 
    * @param action the drag'n'drop action to do DnDConstants.ACTION_MOVE, ACTION_COPY, ACTION_LINK
    * @param index index between children the drop occured at or -1 if not specified
    * @return null if the transferable cannot be accepted or the paste type
    *    to execute when the drop occures
    */
    public abstract PasteType getDropType (Transferable t, int action, int index);

    /** Get the new types that can be created in this node.
    * For example, a node representing a Java package will permit classes to be added.
    * @return array of new type operations that are allowed
    */
    public abstract NewType[] getNewTypes ();
    
    /** Get the set of actions that are associated with this node.
     * This set is used to construct the popup menu for the node.
     *
     * <P>
     * By default this method delegates to the deprecated getActions or getContextActions
     * method depending on the value of suplied argument.
     * <P>
     * It is supposed to be overridden by subclasses accordingly.
     *
     * @param context whether to find actions for context meaning or for the
     *   node itself
     * @since 3.29
     */
    public Action[] getActions (boolean context) {
        return context ? getContextActions () : getActions ();
    }

    /** Get the set of actions associated with this node.
    * This may be used e.g. in constructing a {@link #getContextMenu context menu}.
    *
    * <P>
    * By default returns the actions in {@link NodeOp#getDefaultActions}.
    *
    * @return system actions appropriate to the node
    * @deprecated Use getActions (false) instead.
    */
    public SystemAction[] getActions () {
        return NodeOp.getDefaultActions ();
    }
    
    

    /** Get a special set of actions
    * for situations when this node is displayed as a context.
    * <p>For example, right-clicking on a parent node in a hierarchical view (such as
    * the normal Explorer) should use <code>getActions</code>. However, if this node
    * is serving as the parent of a (say) a window tab full of icons (e.g., in
    * {@link org.openide.explorer.view.IconView}), and the users right-clicks on
    * the empty space in this pane, then this method should be used to get
    * the appropriate actions for a popup menu.
    * <p>Note that in the Windows UI system, e.g., these action sets are quite different.
    *
    * @return actions for a context. In the default implementation, same as {@link #getActions}.
    * @deprecated Use getActions (true) instead.
    */
    public SystemAction[] getContextActions () {
        return getActions ();
    }

    /** Gets the default action for this node.
     * @return <code>null</code> indicating there should be none default action
     * @deprecated Use {@link #getPreferredAction} instead.
     */
    public SystemAction getDefaultAction () {
        return null;
    };

    /** Gets the preferred action for this node.
     * This action can but need not to be one from the action array returned
     * from {@link #getActions(boolean)}.
     * In case it is, the popup menu created from those actions
     * is encouraged to highlight the preferred action.
     * Override in subclasses accordingly.
     *
     * @return the preferred action, or <code>null</code> if there is none
     * @since 3.29
     */
    public Action getPreferredAction() {
        return getDefaultAction();
    }


    /** Make a context menu for this node.
    * The menu is constructed from the set of actions returned by {@link #getActions}.
    *
    * @return the popup menu
    */
    public final JPopupMenu getContextMenu () {
        return NodeOp.findContextMenuImpl (new Node[] { this }, null);
    }

    /** Test whether there is a customizer for this node. If true,
    * the customizer can be obtained via {@link #getCustomizer}.
    *
    * @return <CODE>true</CODE> if there is a customizer
    */
    public abstract boolean hasCustomizer ();

    /** Get the customizer component.
    * @return the component, or <CODE>null</CODE> if there is no customizer
    */
    public abstract java.awt.Component getCustomizer ();

    /** Get a cookie for this node.
    * <P>
    * The set of cookies can change. If a node changes its set of
    * cookies, it fires a property change event with {@link #PROP_COOKIE}.
    * <P>
    * If the Node was constructed with a <code>Lookup</code> in constructor
    * than this method delegates to the provided lookup object.
    *
    * @param type the representation class of the cookie
    * @return a cookie assignable to that class, or <code>null</code> if this node has no such cookie
    * @see Lookup
    */
    public Node.Cookie getCookie (Class type) {
        Lookup l = internalLookup (true);
        if (l != null) {
            Object o = l.lookup (type);
            if (o instanceof Node.Cookie) {
                return (Node.Cookie)o;
            }
        }
        return null;
    }
    
    /** Obtains a Lookup represeting additional content of this Node.
     * If the lookup was provided in a constructor, it is returned here,
     * if not, a lookup based on the content of <link>getCookie</link> 
     * method is provided.
     *
     * @return lookup for this node
     * @since 3.11
     */
    public final Lookup getLookup () {
        synchronized (listeners) {
            Lookup l = internalLookup (true);
            if (l != null) {
                return l;
            }
            
            l = findDelegatingLookup ();
            if (l != null) {
                return l;
            }

            // create new lookup and use it
            NodeLookup nl = new NodeLookup (this);
            registerDelegatingLookup (nl);
            return nl;
        }
    }
    
    /** Register delegating lookup so it can always be found.
     */
    final void registerDelegatingLookup (NodeLookup l) {
        // to have just one thread accessing the static lookups variable
        synchronized (lookups) {
            lookups.put (listeners, new WeakReference (l));
        }
    }
    
    /** Finds delegating lookup that was previously registered
     * @return the lookup or null if nothing was registed or the
     *    lookup was GCed.
     */
    final Lookup findDelegatingLookup () {
        WeakReference ref = (WeakReference)lookups.get (listeners);
        return ref == null ? null : (Lookup)ref.get ();
    }

    /** Obtain handle for this node (for serialization).
    * The handle can be serialized and {@link Handle#getNode} used after
    * deserialization to obtain the original node.
    *
    * @return the handle, or <code>null</code> if this node is not persistable
    */
    public abstract Node.Handle getHandle ();

    /** Add a listener to changes in the node's intrinsic properties (name, cookies, etc.).
     * <P>
     * The listener is not notified about changes in subnodes until the
     * method <CODE>getChildren().getNodes()</CODE> is called.
     * @param l the listener to add
     */
    public final void addNodeListener (NodeListener l) {
        listeners.add (NodeListener.class, l);
        listenerAdded ();
    }
    
    /** A method to notify FilterNode that a listenerAdded has been added */
    void listenerAdded () {
    }

    /** Remove a node listener.
     * @param l the listener
    */
    public final void removeNodeListener (NodeListener l) {
        listeners.remove (NodeListener.class, l);
    }

    /** Add a listener to the node's computed Bean properties.
     * @param l the listener
    */
    public final void addPropertyChangeListener (PropertyChangeListener l) {
        int count = -1;
        
        boolean log = err != null && err.isLoggable(ErrorManager.INFORMATIONAL);
        
        if (log) {
            count = getPropertyChangeListenersCount();
        }
        listeners.add (PropertyChangeListener.class, l);
        
        if (log) {
            err.log( ErrorManager.INFORMATIONAL, 
                "ADD - " + getName() + " [" + count + "]->[" + 
                getPropertyChangeListenersCount() + "] " + l );
        }
        notifyPropertyChangeListenerAdded( l );
    }

    /** Called to notify subclasses (FilterNode) about addition of
     * PropertyChangeListener.
     */
    void notifyPropertyChangeListenerAdded( PropertyChangeListener l ) {
    }
    
    /** Returns the number of property change listeners attached to this node
     */
    int getPropertyChangeListenersCount() {
        return listeners.getListenerCount( PropertyChangeListener.class );
    }
    
    /** Allows to figure out, whether the node has any 
     * PropertyChangeListeners attached.
     * @return True if node has one or more PropertyChangeListeners attached.
     * @since 1.36
     */
    protected final boolean hasPropertyChangeListener() {
        return getPropertyChangeListenersCount() > 0;
    }
    
    /** Remove a Bean property change listener.
     * @param l the listener
    */
    public final void removePropertyChangeListener (PropertyChangeListener l) {
        int count = -1;
        if ( err.isLoggable(ErrorManager.INFORMATIONAL) ) {
            count = getPropertyChangeListenersCount();
        }
        listeners.remove (PropertyChangeListener.class, l);
        if ( err.isLoggable(ErrorManager.INFORMATIONAL) ) {
            err.log( ErrorManager.INFORMATIONAL, 
                "RMV - " + getName() + " [" + count + "]->[" + 
                getPropertyChangeListenersCount() + "] " + l );
        }
        
        notifyPropertyChangeListenerRemoved( l );
    }
    
    /** Called to notify subclasses (FilterNode) about removal of
     * PropertyChangeListener.
     */
    void notifyPropertyChangeListenerRemoved( PropertyChangeListener l ) {
    }

    /** Fire a property change event.
    *
    * @param name name of changed property (from {@link #getPropertySets})
    * @param o old value
    * @param n new value
    */
    protected final void firePropertyChange (String name, Object o, Object n) {
        // do not fire if the values are the same
        if (o != null && n != null && (o == n || o.equals (n))) {
            return;
        }

        PropertyChangeEvent ev = null;

        Object[] listeners = this.listeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == PropertyChangeListener.class) {
                // Lazily create the event:
                if (ev == null) {
                    ev = new PropertyChangeEvent (this, name, o, n);
                }
                ((PropertyChangeListener)listeners[i+1]).propertyChange (ev);
            }
        }
    }

    /** Allow subclasses that override the getName method to fire
    * the changes of the name by itself. Please notice that default 
    * implementation of setName will fire the change by itself.
    */
    protected final void fireNameChange (String o, String n) {
        fireOwnPropertyChange (PROP_NAME, o, n);
    }

    /** Allow subclasses that override the getDisplayName method to fire
    * the changes of the name by itself. Please notice that default 
    * implementation of setDisplayName will fire the change by itself.
    */
    protected final void fireDisplayNameChange (String o, String n) {
        fireOwnPropertyChange (PROP_DISPLAY_NAME, o, n);
    }

    /** Allow subclasses that override the getShortDescription method to fire
    * the changes of the description by itself. Please notice that default 
    * implementation of setShortDescription will fire the change by itself.
    */
    protected final void fireShortDescriptionChange (String o, String n) {
        fireOwnPropertyChange (PROP_SHORT_DESCRIPTION, o, n);
    }

    /** Fire a change event for {@link #PROP_ICON}.
    */
    protected final void fireIconChange () {
        fireOwnPropertyChange (PROP_ICON, null, null);
    }

    /** Fire a change event for {@link #PROP_OPENED_ICON}.
    */
    protected final void fireOpenedIconChange () {
        fireOwnPropertyChange (PROP_OPENED_ICON, null, null);
    }

    /** Fires info about some structural change in children. Providing
    * type of operation and set of children changed generates event describing
    * the change.
    *
    *
    * @param addAction <CODE>true</CODE> if the set of children has been added,
    *   false if it has been removed
    * @param delta the array with changed children
    * @param from the array of nodes to take indices from.
    *   Can be null if one should find indices from current set of nodes
    */
    final void fireSubNodesChange (boolean addAction, Node[] delta, Node[] from) {
        NodeMemberEvent ev = null;

        Object[] listeners = this.listeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeListener.class) {
                // Lazily create the event:
                if (ev == null) {
                    ev = new NodeMemberEvent (this, addAction, delta, from);
                }
                if (addAction) {
                    ((NodeListener)listeners[i+1]).childrenAdded(ev);
                } else {
                    ((NodeListener)listeners[i+1]).childrenRemoved(ev);
                }
            }
        }
    }

    /** Fires info about reordering of some children.
    *
    * @param indices array of integers describing the permutation
    */
    final void fireReorderChange (int[] indices) {
        NodeReorderEvent ev = null;

        Object[] listeners = this.listeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeListener.class) {
                // Lazily create the event:
                if (ev == null) {
                    ev = new NodeReorderEvent (this, indices);
                }
                ((NodeListener)listeners[i+1]).childrenReordered(ev);
            }
        }
    }

    /** To all node listeners fire node destroyed notification.
    */
    protected final void fireNodeDestroyed () {
        NodeEvent ev = null;

        Object[] listeners = this.listeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeListener.class) {
                // Lazily create the event:
                if (ev == null) {
                    ev = new NodeEvent (this);
                }
                ((NodeListener)listeners[i+1]).nodeDestroyed (ev);
            }
        }
    }

    /** Fires info about change of parent node.
    * @param o old node
    * @param n new parent
    */
    final void fireParentNodeChange (Node o, Node n) {
        fireOwnPropertyChange (PROP_PARENT_NODE, o, n);
    }

    /** Fires a (Bean) property change event (for {@link #PROP_PROPERTY_SETS}).
    * @param o the old set
    * @param n the new set
    */
    protected final void firePropertySetsChange (PropertySet[] o, PropertySet[] n) {
        fireOwnPropertyChange (PROP_PROPERTY_SETS, o, n);
    }

    /** Fires a change event for {@link #PROP_COOKIE}.
    * The old and new values are set to null.
    */
    protected final void fireCookieChange () {
        fireOwnPropertyChange (PROP_COOKIE, null, null);
    }

    /** Fires info about change of own property.
    * @param name name of property
    * @param o old value
    * @param n new value
    */
    final void fireOwnPropertyChange (String name, Object o, Object n) {
        // do not fire if the values are the same
        if (o != null && n != null && (o == n || o.equals (n))) {
            return;
        }

        PropertyChangeEvent ev = null;

        Object[] listeners = this.listeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeListener.class) {
                // Lazily create the event:
                if (ev == null) {
                    ev = new PropertyChangeEvent (this, name, o, n);
                }
                ((NodeListener)listeners[i+1]).propertyChange (ev);
            }
        }
    }


    /** Class that represents one set of properties. A usual bean has three
    * sets of properties: normal, expert, and events.
    * <p>You may associate context help with this object, if desired, by setting
    * a {@link FeatureDescriptor#setValue custom property} with the name <code>helpID</code>
    * and value of type <code>String</code> giving a help ID.
    * Normally this is unnecessary as help for the whole {@link Node} will be used by default.
    */
    public static abstract class PropertySet extends FeatureDescriptor {
        /** Default constructor. */
        public PropertySet () {
        }

        /** Create a property set.
         * @param name system name of the property set
        * @param displayName human presentable name
        * @param shortDescription description for the set
        */
        public PropertySet (
            String name,
            String displayName,
            String shortDescription
        ) {
            super.setName (name);
            super.setDisplayName (displayName);
            super.setShortDescription (shortDescription);
        }

        /** Get the list of contained properties.
        * This list can contain both {@link Node.Property} and {@link Node.IndexedProperty} elements.
        *
        * @return the properties
        */
        public abstract Property[] getProperties ();
        
        /* Compares just the names.
         * @param propertySet The object to compare to
         */
        public boolean equals (Object propertySet) {
            if (! (propertySet instanceof PropertySet)) {
                return false;
            }
            return ((PropertySet)propertySet).getName ().equals(getName ());
        }
        
        /* Returns a hash code value for the object.
         *
         * @return int hashcode
         */
        public int hashCode () {
            return getName().hashCode ();
        }
    }

    /** Description of a Bean property on a node, and operations on it.
    * <p>You may associate context help with this object, if desired, by setting
    * a {@link FeatureDescriptor#setValue custom property} with the name <code>helpID</code>
    * and value of type <code>String</code> giving a help ID.
    * Normally this is unnecessary as help for the whole {@link Node} will be used by default.
    */
    public static abstract class Property extends FeatureDescriptor {
        /** type that this property works with */
        private Class type;

        /** Constructor.
        * @param valueType type of the property
        */
        public Property (Class valueType) {
            this.type = valueType;
            super.setName(""); // NOI18N
        }

        /** Get the value type. This is the representation class of the property.
        * Remember that e.g. {@link Boolean <code>Boolean.class</code>} means that values are <code>Boolean</code>
        * objects; to specify the primitive type, use e.g. {@link Boolean#TYPE}.
        * In the latter case, {@link #getValue} and {@link #setValue} will still operate on the wrapper object.
        * @return the type
        */
        public Class getValueType () {
            return type;
        }

        /** Test whether the property is readable.
        * @return <CODE>true</CODE> if it is
        */
        public abstract boolean canRead ();

        /** Get the value.
        * @return the value of the property
        * @exception IllegalAccessException cannot access the called method
        * @exception InvocationTargetException an exception during invocation
        */
        public abstract Object getValue () throws
                    IllegalAccessException, InvocationTargetException;

        /** Test whether the property is writable.
        * @return <CODE>true</CODE> if the read of the value is supported
        */
        public abstract boolean canWrite ();

        /** Set the value.
        * @param val the new value of the property
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public abstract void setValue (Object val) throws IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException;

        /** Test whether the property had a default value.
        * @return <code>true</code> if it does (<code>false</code> by default)
        */
        public boolean supportsDefaultValue () {
            return false;
        }

        /** Restore this property to its default value, if supported.
        * In the default implementation, does nothing.
        * Typically you would just call e.g. <code>setValue(default)</code>.
        * Note that it is not permitted for this call to throw {@link IllegalArgumentException},
        * though the other two exceptions from {@link #setValue} may be passed through.
        * @exception IllegalAccessException cannot access the called method
        * @exception InvocationTargetException an exception during invocation
        */
        public void restoreDefaultValue () throws IllegalAccessException, InvocationTargetException {
        }
        
        /**
         * This method indicates whether the current value is the same as
         * the value that would otherwise be restored by calling
         * <code>restoreDefaultValue()</code> (if <code>supportsDefaultValue()</code>
         * returns true). The default implementation returns true and
         * it is recommended to also return true when <code>supportsDefaultValue()</code>
         * returns false (if we do not support default value any value can
         * be considered as the default). If <code>supportsDefaultValue()</code>
         * returns false this method will not be called by the default
         * implementation of property sheet.
         * @since 3.19
         */
        public boolean isDefaultValue() {
            return true;
        }

        /** Get a property editor for this property.
        * The default implementation tries to use {@link java.beans.PropertyEditorManager}.
        * @return the property editor, or <CODE>null</CODE> if there is no editor  */
        public PropertyEditor getPropertyEditor () {
            if (type == null) return null;
            return java.beans.PropertyEditorManager.findEditor(type);
        }

        /* Standard equals implementation for all property
        * classes.
        * @param property The object to compare to
        */
        public boolean equals (Object property) {
            try {
                Class propValueType = ((Property)property).getValueType ();
                Class valueType = getValueType();
                
                if ( ( propValueType == null && valueType != null ) ||
                     ( propValueType != null && valueType == null ) ) {
                    return false;
                }
                
                return ((Property)property).getName ().equals (getName ()) &&
                        (( propValueType == null && valueType == null ) ||
                        propValueType.equals (valueType));
            } catch (ClassCastException e) {
                return false;
            }
        }

        /* Returns a hash code value for the object.
        *
        * @return int hashcode
        */
        public int hashCode () {
            
            Class valueType = getValueType ();
            
            return getName ().hashCode () * 
                   ( valueType == null ? 1 : valueType.hashCode () );
        }
    }

    /** Description of an indexed property and operations on it.
    */
    public static abstract class IndexedProperty extends Node.Property {
        /** type of element that this property works with */
        private Class elementType;

        /** Constructor.
        * @param valueType type of the property
        */
        public IndexedProperty (Class valueType, Class elementType) {
            super (valueType);
            this.elementType = elementType;
        }

        /** Test whether the property is readable by index.
        * @return <CODE>true</CODE> if so
        */
        public abstract boolean canIndexedRead ();

        /** Get the element type of the property (not the type of the whole property).
        * @return the type
        */
        public Class getElementType () {
            return elementType;
        }

        /** Get the value of the property at an index.
        *
        * @param index the index
        * @return the value at that index
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public abstract Object getIndexedValue (int index) throws
                    IllegalAccessException, IllegalArgumentException, InvocationTargetException;

        /** Test whether the property is writable by index.
        * @return <CODE>true</CODE> if so
        */
        public abstract boolean canIndexedWrite ();

        /** Set the value of the property at an index.
        *
        * @param indx the index
        * @param val the value to set
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public abstract void setIndexedValue (int indx, Object val) throws
                    IllegalAccessException, IllegalArgumentException, InvocationTargetException;

        /** Get a property editor for individual elements in this property.
        * @return the property editor for elements
        */
        public PropertyEditor getIndexedPropertyEditor () {
            return java.beans.PropertyEditorManager.findEditor (elementType);
        }
        
        /* Standard equals implementation for all property
        * classes.
        * @param property The object to compare to
        */
        public boolean equals (Object property) {
            try {
                if ( ! super.equals( property ) ) {
                    return false;
                }
                
                Class propElementType = ((IndexedProperty)property).getElementType ();
                Class elementType = getElementType();
                
                if ( ( propElementType == null && elementType != null ) ||
                     ( propElementType != null && elementType == null ) ) {
                    return false;
                }
                
                return (( propElementType == null && elementType == null ) ||
                        propElementType.equals (elementType));
                
            } catch (ClassCastException e) {
                return false;
            }
        }

        /* Returns a hash code value for the object.
        *
        * @return int hashcode
        */
        public int hashCode () {
            
            Class ementType = getElementType ();
            
            return super.hashCode () * 
                   ( elementType == null ? 1 : elementType.hashCode() );       
        }
    }

    /** Marker interface for all cookies.
    * <P>
    * Most examples are present in {@link org.openide.cookies}.
    */
    public static interface Cookie {
    }

    /** Serializable node reference. The node should not
    * be serialized directly but via this handle. One can obtain a handle
    * by a call to {@link Node#getHandle}.
    * <P>
    * If that methods returns a non-<code>null</code> value, one can serialize it,
    * and after deserialization
    * use {@link #getNode} to obtain the original node.
    */
    public static interface Handle extends java.io.Serializable {
        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = -4518262478987434353L;
        /** Reconstitute the node for this handle.
        *
        * @return the node for this handle
        * @exception IOException if the node cannot be created
        */
        public Node getNode () throws java.io.IOException;
    }

    /** Obtains a resource string from bundle.
    * @param resName resource name
    * @return the string
    */
    static String getString (final String resName) {
        return NbBundle.getBundle(Node.class).getString (resName);
    }

    public String toString () {
        return super.toString () + "[Name="+getName ()+", displayName="+getDisplayName ()+"]"; // NOI18N
    }
    
    
    /** template for changes in cookies */
    private static final Lookup.Template TEMPL_COOKIE = new Lookup.Template (Node.Cookie.class);

    /** Special subclass of EventListenerList that can also listen on changes in 
     * a lookup.
     */
    private final class LookupEventList extends javax.swing.event.EventListenerList 
    implements LookupListener {
        
        public final Lookup lookup;
        private Lookup.Result result;

        public LookupEventList (Lookup l) {
            this.lookup = l;
        }
        
        public synchronized Lookup init (boolean init) {
            if (init && result == null) {
                result = lookup.lookup (TEMPL_COOKIE);
                result.addLookupListener (this);
                result.allItems();
            }
            return lookup;
        }
        
        public void resultChanged(org.openide.util.LookupEvent ev) {
            fireCookieChange();
        }
    }
}
