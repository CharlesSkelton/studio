/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.explorer;

import java.awt.Component;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.text.MessageFormat;
import javax.swing.SwingUtilities;

import org.openide.nodes.*;
import org.openide.ErrorManager;
import org.openide.util.*;
import org.openide.util.datatransfer.*;
import org.openide.util.io.SafeException;

/**
 * Manages a selection and root context for a (set of) Explorer view(s).  The
 * views should register their {@link java.beans.VetoableChangeListener}s and
 * {@link java.beans.PropertyChangeListener}s at the
 * <code>ExplorerManager</code> of the Explorer they belong to. The manager
 * listens on changes to the node hierarchy and updates the selection and root
 * node.
 *
 * <P>Deserialization may throw {@link SafeException} if the contexts cannot be
 * restored correctly, but the stream is uncorrupted.
 *
 * @author Ian Formanek, Petr Hamernik, Jaroslav Tulach, Jan Jancura,
 *         Jesse Glick
 */
public final class ExplorerManager extends Object
    implements Serializable, Cloneable {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -4330330689803575792L;

    /** Name of property for the root context. */
    public static final String PROP_ROOT_CONTEXT = "rootContext"; // NOI18N
    /** Name of property for the explored context. */
    public static final String PROP_EXPLORED_CONTEXT = "exploredContext"; // NOI18N
    /** Name of property for the node selection. */
    public static final String PROP_SELECTED_NODES = "selectedNodes"; // NOI18N
    /** Name of property for change in a node. */
    public static final String PROP_NODE_CHANGE = "nodeChange"; // NOI18N

    /** The support for VetoableChangeEvent */
    private transient VetoableChangeSupport vetoableSupport;
    /** The support for PropertyChangeEvent */
    private transient PropertyChangeSupport propertySupport;

    /** The current root context */
    private Node rootContext;
    /** The current explored context */
    private Node exploredContext;
    /** The currently selected beans */
    private Node[] selectedNodes;
    /** listener to destroy of root node */
    private transient Listener listener;
    /** weak listener */
    private transient NodeListener weakListener;

    /** Request processor for managing selections.
    */
    static RequestProcessor selectionProcessor;

    /** Delay for coalescing events before removing destroyed nodes from 
        the selection.
    */
    private static final int SELECTION_SYNC_DELAY = 200;

    /** Task that removes manages node selection issues.
    */
    private RequestProcessor.Task selectionSyncTask;

    // this static loading of CallbackSystemAction is here because
    // when we are in standalone library we need the CallbackSystemAction
    // be loaded (its static initializers performed) in order
    // to any ExplorerManager to work.
    static {
        try {
            Class.forName("org.openide.util.actions.CallbackSystemAction"); // NOI18N
        } catch (Exception y) {
        }
    }
    
    /** Construct a new manager. */
    public ExplorerManager () {
        init ();
    }

    /** Initializes the nodes.
    */
    private void init () {
        exploredContext = rootContext = Node.EMPTY;
        selectedNodes = new Node[0];
        listener = new Listener ();
        weakListener = WeakListener.node (listener, null);
    }

    /** Clones the manager.
    * @return manager with the same settings like this one
    */
    public Object clone () {
        ExplorerManager em = new ExplorerManager ();
        em.rootContext = rootContext;
        em.exploredContext = exploredContext;
        em.selectedNodes = selectedNodes;
        return em;
    }

    /** Get the set of selected nodes.
    * @return the selected nodes; empty (not <code>null</code>) if none are selected
    */
    public Node[] getSelectedNodes () {
        return selectedNodes;
    }

    /** Set the set of selected nodes.
    * @param value the nodes to select; empty (not <code>null</code>) if none are to be selected
    * @exception PropertyVetoException when the given nodes cannot be selected
    * @throws IllegalArgumentException if <code>null</code> is given, or if any elements
    *                                  of the selection are not within the current root context
    */
    public final void setSelectedNodes (Node[] value) throws PropertyVetoException {
        Node[] oldValue;
        
        synchronized (this) {
            if (value == null) throw new IllegalArgumentException(getString("EXC_NodeCannotBeNull"));

            if (Arrays.equals (value, selectedNodes)) {
                return;
            }

            for (int i = 0; i < value.length; i++) {
                if (value[i] == null) {
                    throw new IllegalArgumentException(getString("EXC_NoElementOfNodeSelectionMayBeNull"));
                }
                if (! isUnderRoot (value[i])) {
                    throw new IllegalArgumentException(
                        MessageFormat.format(
                            getString("EXC_NodeSelectionCannotContainNodes"),
                            new Object[] {
                                value[i].getDisplayName(),
                                rootContext.getDisplayName(),
                            }
                        )
                    );
                }
            }

            if (value.length != 0 && vetoableSupport != null) {
                // we send the vetoable change event only for non-empty selections
                vetoableSupport.fireVetoableChange(PROP_SELECTED_NODES, selectedNodes, value);
            }

            oldValue = selectedNodes;

            Collection nodesToRemove;
            Collection nodesToAdd;
            Collection newSelection = new LinkedList();
            Collection currentNodes = Arrays.asList(oldValue);
	    
	    for (int i = 0; i < value.length; i++) {
		newSelection.add(value[i]);
	    }
	    
            nodesToAdd = new LinkedList(newSelection);
            nodesToAdd.removeAll(currentNodes);

            nodesToRemove = new LinkedList(currentNodes);
            nodesToRemove.removeAll(newSelection);

            // PENDING: filter out duplicities from the selection
            if (newSelection.size() == value.length) {
                selectedNodes = value;
            } else {
                selectedNodes = new Node[value.length];
                newSelection.toArray(selectedNodes);
            }

            Iterator it;
            // remove listeners from nodes that are being deselected
            for (it = nodesToRemove.iterator(); it.hasNext(); ) {
                Node n = (Node)it.next();
                n.removeNodeListener(weakListener);
            }
            // and add listeners to nodes that become selected
            for (it = nodesToAdd.iterator(); it.hasNext(); ) {
                Node n = (Node)it.next();
                n.removeNodeListener(weakListener);
                n.addNodeListener(weakListener);
            }
        }  // synchronized (this)
        if (propertySupport != null) {
            // replan fire of prop event to AWT-queue to get correctly visual-reactions
            if (SwingUtilities.isEventDispatchThread ()) {
                propertySupport.firePropertyChange (PROP_SELECTED_NODES, oldValue, selectedNodes);
            } else {
                final Node[] tempOldValue = oldValue;
                SwingUtilities.invokeLater(new Runnable () {
                    public void run () {
                        propertySupport.firePropertyChange (PROP_SELECTED_NODES, tempOldValue, selectedNodes);
                    }
                });
            }
        }
    }

    /** Get the explored context.
     * <p>The "explored context" is not as frequently used as the node selection;
     * generally it refers to a parent node which contains all of the things
     * being displayed at this moment. For <code>BeanTreeView</code> this is
     * irrelevant, but <code>ContextTreeView</code> uses it (in lieu of the node
     * selection) and for <code>IconView</code> it is important (the node
     * whose children are visible, i.e. the "background" of the icon view).
     * @return the node being explored, or <code>null</code>
     */
    public final Node getExploredContext() {
        return exploredContext;
    }

    /** Set the explored context.
     * The node selection will be cleared as well.
     * @param value the new node to explore, or <code>null</code> if none should be explored.
     * @throws IllegalArgumentException if the node is not within the current root context in the node hierarchy
     */
    public final void setExploredContext(Node value) {
        setExploredContext(value, new Node[0]);
    }
    
    /** Set the explored context.
     * The node selection will be changed as well. Note: node selection cannot be
     * vetoed if calling this method. It is generally better to call setExploredContextAndSelection.
     * @param value the new node to explore, or <code>null</code> if none should be explored.
     * @throws IllegalArgumentException if the node is not within the current root context in the node hierarchy
     */
    public final void setExploredContext(Node value, Node[] selection) {
        // handles nulls correctly:
        if (Utilities.compareObjects (value, exploredContext)) {
            setSelectedNodes0(selection);
            return;
        }

        if (value != null && ! isUnderRoot (value)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    getString("EXC_ContextMustBeWithinRootContext"),
                    new Object[] {
                        value.getDisplayName (),
                        rootContext.getDisplayName ()
                    }
                )
            );
        }
        setSelectedNodes0(selection);
        final Node oldValue = exploredContext;
        exploredContext = value;

        if (propertySupport != null) {
            // replan fire of prop event to AWT-queue to get correctly visual-reactions
            if (SwingUtilities.isEventDispatchThread ()) {
                propertySupport.firePropertyChange (PROP_EXPLORED_CONTEXT, oldValue, exploredContext);
            } else {
                SwingUtilities.invokeLater(new Runnable () {
                    public void run () {
                        propertySupport.firePropertyChange (PROP_EXPLORED_CONTEXT, oldValue, exploredContext);
                    }
                });
            }
        }
    }
    
    /** Set the explored context and selected nodes. If the change in selected nodes is vetoed,
     * PropertyVetoException is rethrown from here.
     * @param value the new node to explore, or <code>null</code> if none should be explored.
     * @param selection the new nodes to be selected
     * @throws IllegalArgumentException if the node is not within the current root context in the node hierarchy
     * @throws PropertyVetoExcepion if listeners attached to this explorer manager do so
     */
    public final void setExploredContextAndSelection(Node value, Node[] selection) throws PropertyVetoException {
        // handles nulls correctly:
        if (Utilities.compareObjects (value, exploredContext)) {
            setSelectedNodes1(selection);
            return;
        }

        if (value != null && ! isUnderRoot (value)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    getString("EXC_ContextMustBeWithinRootContext"),
                    new Object[] {
                        value.getDisplayName (),
                        rootContext.getDisplayName ()
                    }
                )
            );
        }
        setSelectedNodes1(selection);
        final Node oldValue = exploredContext;
        exploredContext = value;

        if (propertySupport != null) {
            // replan fire of prop event to AWT-queue to get correctly visual-reactions
            if (SwingUtilities.isEventDispatchThread ()) {
                propertySupport.firePropertyChange (PROP_EXPLORED_CONTEXT, oldValue, exploredContext);
            } else {
                SwingUtilities.invokeLater(new Runnable () {
                    public void run () {
                        propertySupport.firePropertyChange (PROP_EXPLORED_CONTEXT, oldValue, exploredContext);
                    }
                });
            }
        }
    }
    
    /** Sets selected nodes and handles PropertyVetoException */
    final void setSelectedNodes0(Node[] nodes) {
        try {
            setSelectedNodes(nodes);
        } catch (PropertyVetoException e) {
            if (nodes.length == 0) {
                IllegalStateException err = new IllegalStateException(getString("EXC_MustNotVetoEmptySelection"));
                ErrorManager.getDefault ().annotate(err, e);
                // Should be impossible regardless of view:
                throw err;
            }
        }
    }

    /** Sets selected nodes and handles PropertyVetoException */
    private void setSelectedNodes1(Node[] nodes) throws PropertyVetoException {
        try {
            setSelectedNodes(nodes);
        } catch (PropertyVetoException e) {
            if (nodes.length == 0) {
                IllegalStateException err = new IllegalStateException(getString("EXC_MustNotVetoEmptySelection"));
                ErrorManager.getDefault ().annotate(err, e);
                // Should be impossible regardless of view:
                throw err;
            }
            throw e; // this is the difference to setSelectedNodes0
        }
    }
    /** Get the root context.
    * <p>The "root context" is simply the topmost node that this explorer can
    * display or manipulate. For <code>BeanTreeView</code>, this would mean
    * the root node of the tree. For e.g. <code>IconView</code>, this would
    * mean the uppermost possible node that that icon view could display;
    * while the explored context would change at user prompting via the
    * up button and clicking on subfolders, the root context would be fixed
    * by the code displaying the explorer.
    * @return the root context node
    */
    public final Node getRootContext() {
        return rootContext;
    }

    /** Set the root context.
    * The explored context will be set to the new root context as well.
    * If any of the selected nodes are not inside it, the selection will be cleared.
    * @param value the new node to serve as a root
    * @throws IllegalArgumentException if it is <code>null</code>
    */
    public final void setRootContext(Node value) {
        if (value == null) throw new IllegalArgumentException(getString("EXC_CannotHaveNullRootContext"));
        if (rootContext.equals (value)) return;
        final Node oldValue = rootContext;
        rootContext = value;

        oldValue.removeNodeListener (weakListener);
        rootContext.addNodeListener (weakListener);

        if (propertySupport != null) {
            // replan fire of prop event to AWT-queue to get correctly visual-reactions
            if (SwingUtilities.isEventDispatchThread ()) {
                propertySupport.firePropertyChange (PROP_ROOT_CONTEXT, oldValue, rootContext);
            } else {
                SwingUtilities.invokeLater(new Runnable () {
                    public void run () {
                        propertySupport.firePropertyChange (PROP_ROOT_CONTEXT, oldValue, rootContext);
                    }
                });
            }
        }
        Node[] newselection = getSelectedNodes();
        if (!areUnderTarget(newselection, rootContext)) {
            newselection = new Node[0];
        }
        setExploredContext(rootContext, newselection);
    }
    
    /** @return true iff all nodes are under the target node */
    private boolean areUnderTarget(Node[] nodes, Node target) {
        bigloop: for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            while (node != null) {
                if (node.equals(target)) {
                    continue bigloop;
                }
                node = node.getParentNode();
            }
            return false;
        }
        return true;
    }

    /** Add a <code>PropertyChangeListener</code> to the listener list.
    * @param l the listener to add
    */
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        if (propertySupport == null)
            propertySupport = new PropertyChangeSupport(this);
        propertySupport.addPropertyChangeListener(l);
    }

    /** Remove a <code>PropertyChangeListener</code> from the listener list.
    * @param l the listener to remove
    */
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        if (propertySupport != null)
            propertySupport.removePropertyChangeListener(l);
    }

    /** Add a <code>VetoableListener</code> to the listener list.
    * @param l the listener to add
    */
    public synchronized void addVetoableChangeListener(VetoableChangeListener l) {
        if (vetoableSupport == null)
            vetoableSupport = new VetoableChangeSupport(this);
        vetoableSupport.addVetoableChangeListener(l);
    }

    /** Remove a <code>VetoableChangeListener</code> from the listener list.
    * @param l the listener to remove
    */
    public synchronized void removeVetoableChangeListener(VetoableChangeListener l) {
        if (vetoableSupport != null)
            vetoableSupport.removeVetoableChangeListener(l);
    }

    /** Checks whether given Node is a subnode of rootContext.
    * @return true if specified Node is under current rootContext
    */
    private boolean isUnderRoot(Node node) {
        while (node != null) {
            if (node.equals(rootContext)) return true;
            node = node.getParentNode();
        }
        return false;
    }
    
    /** defines serialized fields for the manager.
    */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField ("root", Node.Handle.class), // NOI18N
        new ObjectStreamField ("rootName", String.class), // NOI18N
        new ObjectStreamField ("explored", String[].class), // NOI18N

        // XXX(-ttran) this should be String[][].class, but cannot be changed
        // now because of backward compatibility
        new ObjectStreamField ("selected", Object[].class) // NOI18N
    };

    /** serializes object
    * @serialData the following objects are written in sequence:
    * <ol>
    * <li> a Node.Handle for the root context; may be null if root context
    *      is not persistable
    * <li> the display name of the root context (to give nicer error messages
    *      later on)
    * <li> the path from root context to explored context; null if no explored
    *      context or no such path
    * <li> for every element of node selection, path from root context to that node;
    *      null if no such path
    * <li> null to terminate
    * </ol>
    * Note that if the root context handle is null, the display name is still written
    * but the paths to explored context and node selection are not written, the stream
    * ends there.
    */
    private void writeObject (ObjectOutputStream os) throws IOException {
        // indication that we gonna use put fields and not the old method.
        os.writeObject (this);
        
        ObjectOutputStream.PutField fields = os.putFields ();
        
        // [PENDING] is this method (and readObject) always called from within
        // the Nodes mutex? It should be!
        //System.err.println("rootContext: " + rootContext);
      
      
        Node.Handle rCH = rootContext.getHandle ();
        fields.put ("root", rCH); // NOI18N
        //System.err.println("writing: " + rCH);
        fields.put ("rootName", rootContext.getDisplayName ()); // NOI18N
        
        //System.err.println("writing: " + rootContext.getDisplayName ());
        // If root cannot be stored, we just stop right there, of course.
        if (rCH != null) {
            // Note that explored context may be null (this is valid).
            // Also, it may have happened that the hierarchy changed so that
            // the explored context is *no longer* under the root (though it was at
            // the time these things were set up). In this case, we cannot store the
            // path. Caution: NodeOp.createPath will create a path to a root (parentless)
            // node even if you specify a non-null root, if the first arg is not a child!
            String[] explored;
            if (exploredContext == null)
                explored = null;
            else if (isUnderRoot (exploredContext))
                explored = NodeOp.createPath (exploredContext, rootContext);
            else
                explored = null;
            
            fields.put ("explored", explored); // NOI18N
            
            // Now do the same for each node selection, with the same caveats.
            // Null terminates, so bad elements are simply skipped.
            LinkedList selected = new LinkedList ();
            for (int i = 0; i < selectedNodes.length; i++) {
                if (isUnderRoot (selectedNodes[i])) {
                    selected.add (NodeOp.createPath (selectedNodes[i], rootContext));
                }
            }
            
            fields.put ("selected", selected.toArray ()); // NOI18N
        }
        
        os.writeFields();
    }

    /** Deserializes the view and initializes it
     * @serialData see writeObject
     */
    private void readObject (ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // perform initialization
        init();

        // read the first object in the stream
        Object firstObject = ois.readObject ();
        if (firstObject != this) {
            // use old version of deserialization
            readObjectOld ((Node.Handle)firstObject, ois);
            return;
        }
        
        // work with get fields
        ObjectInputStream.GetField fields = ois.readFields();
        
        // read root handle
        Node.Handle h = (Node.Handle)fields.get ("root", null); // NOI18N
        //System.err.println("reading: " + h);
        final String rootName = (String)fields.get ("rootName", null); // NOI18N
        //System.err.println("reading: " + rootName);

        if (h == null) {
            // Cancel deserialization (e.g. of the ExplorerPanel window) in case the
            // root handle was not persistent:
            throw new SafeException(
                new IOException(NbBundle.getMessage(ExplorerManager.class, "EXC_cannot_deser_null_handle", rootName))
            );
        } else {
            String[] exploredCtx = (String[]) fields.get ("explored", null); // NOI18N
            Object[] selPaths = (Object[]) fields.get ("selected", null); // NOI18N
            
            try {
                Node root = h.getNode ();
                if (root == null)
                    throw new IOException("Node.Handle.getNode (for " + rootName + ") should not return null"); // NOI18N
                restoreSelection(root, exploredCtx, Arrays.asList(selPaths));
            } catch (IOException ioe) {
                if (! Utilities.compareObjects (ioe.getMessage (), ioe.getLocalizedMessage ())) {
                    // already localized
                    throw new SafeException (ioe);
                } else {
                    throw new SafeException (ioe) {
                        public String getLocalizedMessage () {
                            return NbBundle.getMessage (ExplorerManager.class, "EXC_handle_failed", rootName);
                        }
                    };
                }
            }
        }
    }

    private void readObjectOld (Node.Handle h, ObjectInputStream ois)
        throws java.io.IOException, ClassNotFoundException
    {
     
        if (h == null) {
            // do nothing => should not occur to often and moreover this is also
            // dead code replaced by new version
            return;
        }
        else {
            String[] rootCtx = (String[]) ois.readObject();
            String[] exploredCtx = (String[]) ois.readObject ();
            LinkedList ll = new LinkedList ();
            for (;;) {
                String[] path = (String[]) ois.readObject();
                if (path == null)
                    break;
                ll.add(path);
            }
            Node root = findPath (h.getNode (), rootCtx);
            restoreSelection(root, exploredCtx, ll);
        }
    }
 
    private void restoreSelection(final Node root,
                                  final String[] exploredCtx,
                                  final List selectedPaths /* of String[] */) {
        setRootContext(root);

        // XXX(-ttran) findPath() can take a long time and employs DataSystems
        // and others.  We cannot call it synchrorously, in the past deadlocks
        // have happened because of this.  OTOH as we call setSelectedNodes
        // asynchonously someone else can change the root context or the Node
        // hierarchy in between, which causes setSelectedNodes to throw
        // IllegalArgumentException.  There seems to be no simple good
        // solution.  For now we just catch IllegalArgumentException and be
        // decently silent about the fact.
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                // convert paths to Nodes
        
                List selNodes = new ArrayList(selectedPaths.size());
                
                for (Iterator iter = selectedPaths.iterator(); iter.hasNext(); ) {
                    String[] path = (String[]) iter.next();
                    selNodes.add(findPath(root, path));
                }
                
                // set the selection

                try {
                    Node[] newSelection = (Node[]) selNodes.toArray(new Node[selNodes.size ()]);
                    if (exploredCtx != null) {
                        setExploredContext(findPath(root, exploredCtx), newSelection);
                    }
                    else {
                        setSelectedNodes0(newSelection);
                    }
                }
                catch (IllegalArgumentException ignore) {}
            }
        });
    }

    /**
     * Finds the proper Explorer manager for a given component.  This is done
     * by traversing the component hierarchy and finding the first ancestor
     * that implements {@link Provider}.  <P> This method should be used in
     * {@link Component#addNotify} of each component that works with the
     * Explorer manager, e.g.:
     * <p><pre>
     * private transient ExplorerManager explorer;
     * 
     * public void addNotify () {
     *   super.addNotify ();
     *   explorer = ExplorerManager.find (this);
     * }
     * </pre>
     *
     * @param comp component to find the manager for
     * @return the manager, or a new empty manager if no ancestor implements
     * <code>Provider</code>
     *
     * @see Provider
     */
    public static ExplorerManager find (Component comp) {
        // start looking for manager from parent, not the component itself
        for (;;) {
            comp = comp.getParent ();
            if (comp == null) {
                // create new explorer because nothing has been found
                return new ExplorerManager ();
            }
            if (comp instanceof Provider) {
                // ok, found a provider, return its manager
                return ((Provider)comp).getExplorerManager ();
            }
        }
    }

    /** Finds node by given path */
    static Node findPath(Node r, String[] path) {
        try {
            return NodeOp.findPath(r, path);
        } catch (NodeNotFoundException ex) {
            return ex.getClosestNode();
        }
    }

    /** Creates or retrieves RequestProcessor for selection updates. */
    static synchronized RequestProcessor getSelectionProcessor() {
        if (selectionProcessor == null) {
            selectionProcessor = new RequestProcessor("ExplorerManager-selection"); //NOI18N
        }
        return selectionProcessor;
    }
    
    //
    // inner classes
    //

    /** Interface for components wishing to provide their own <code>ExplorerManager</code>.
    * @see ExplorerManager#find
    */
    public static interface Provider {
        /** Get the explorer manager.
        * @return the manager
        */
        public ExplorerManager getExplorerManager ();
    }
    
    /** Listener to be notified when root node has been destroyed.
    * Then the root node is changed to Node.EMPTY
    */
    private class Listener extends NodeAdapter implements Runnable {
        Collection  removeList = new HashSet();

	Listener() {}

        /** Fired when the node is deleted.
         * @param ev event describing the node
         */
        public void nodeDestroyed(NodeEvent ev) {
            if (ev.getNode ().equals (getRootContext ())) {
                // node has been deleted
                // [PENDING] better to show a node with a label such as "<deleted>"
                // and a tool tip explaining the situation
                setRootContext (Node.EMPTY);
            } else {
                // assume that the node is among currently selected nodes
                scheduleRemove(ev.getNode());
            }
        }
        
        /* Change in a node.
         * @param ev the event
         */
        public void propertyChange (java.beans.PropertyChangeEvent ev) {
            if (propertySupport != null) {
                // replan fire of prop event to AWT-queue to get correctly visual-reactions
                if (SwingUtilities.isEventDispatchThread ()) {
                    propertySupport.firePropertyChange (PROP_NODE_CHANGE, null, null);
                } else {
                    SwingUtilities.invokeLater(new Runnable () {
                        public void run () {
                            propertySupport.firePropertyChange (PROP_NODE_CHANGE, null, null);
                        }
                    });
                }
            }
        }

        /** Schedules removal of a node
        */        
        private void scheduleRemove(Node n) {
            synchronized (ExplorerManager.this) {
                if (selectionSyncTask == null) {
                    selectionSyncTask = getSelectionProcessor().create(this);
                } else {
                    selectionSyncTask.cancel();
                }
            }
            
            synchronized (this) {
                removeList.add(n);
            }
            // invariant: selectionSyncTask != null && is not running yet.
            selectionSyncTask.schedule(SELECTION_SYNC_DELAY);
        }
        
        public void run() {
            Collection remove;
            
            synchronized (this) {
                // atomically clears the list while keeping a copy.
                // if another node is removed after this point, the selection
                // will be updated later.
                remove = removeList;
                removeList = new HashSet();
            }
            Collection newSel = new LinkedList(Arrays.asList(getSelectedNodes()));
            newSel.removeAll(remove);
            Node[] selNodes = (Node[]) newSel.toArray(new Node[newSel.size()]);
            setSelectedNodes0(selNodes);
        }
    }

    private static String getString(String key) {
        return NbBundle.getMessage(ExplorerManager.class,key);
    }
}
