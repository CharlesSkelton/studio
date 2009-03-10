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

package org.openide.explorer.view;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.tree.TreeNode;

import org.openide.ErrorManager;
import org.openide.nodes.*;
import org.openide.util.Mutex;
import org.openide.util.WeakListener;
import org.openide.util.enums.QueueEnumeration;

/** Visual representation of one node. Holds necessary information about nodes
* like icon, name, description and also list of its children. 
* <P>
* There is at most one VisualizerNode for one node. All of them are hold in a cache.
* <P>
* The VisualizerNode level provides secure layer between Nodes and Swing AWT dispatch 
* thread.
*
* @author Jaroslav Tulach
*/
final class VisualizerNode extends EventListenerList
    implements NodeListener, TreeNode, Runnable {
    /** one template to use for searching for visualizers */
    private static final VisualizerNode TEMPLATE = new VisualizerNode (0);

    /** constant holding empty reference to children */
    private static final Reference NO_REF = new WeakReference (null);

    /** cache of visializers (VisualizerNode, Reference (VisualizerNode)) */
    private static WeakHashMap cache = new WeakHashMap ();

    /** empty visualizer */
    public static final VisualizerNode EMPTY = getVisualizer (null, Node.EMPTY);
    
    /** queue processor to transfer requests to event queue */
    private static final QP QUEUE = new QP ();
    
    private static final ErrorManager err = ErrorManager.getDefault().getInstance("org.openide.explorer.view.VisualizerNode"); // NOI18N

    // bugfix #29435, getVisualizer is synchronized in place of be called only from EventQueue
    /** Finds VisualizerNode for given node.
    * @param ch the children this visualizer should belong to
    * @param n the node
    * @return the visualizer
    */
    public static synchronized VisualizerNode getVisualizer (VisualizerChildren ch, Node n) {
        TEMPLATE.hashCode = System.identityHashCode (n);
        TEMPLATE.node = n;

        Reference r = (Reference)cache.get (TEMPLATE);

        TEMPLATE.hashCode = 0;
        TEMPLATE.node = null;

        VisualizerNode v = r == null ? null : (VisualizerNode)r.get ();
        if (v == null) {
            v = new VisualizerNode (n);
            cache.put (v, new WeakReference (v));
        }
        if (ch != null) {
            v.parent = ch;
        }
        return v;
    }


    /** node. Do not modify!!! */
    Node node;
    /** system hashcode of the node */
    private int hashCode;
    /** visualizer children attached thru weak references Reference (VisualizerChildren) */
    private Reference children = NO_REF;
    /** the VisualizerChildren that contains this VisualizerNode or null */
    private VisualizerChildren parent;

    /** cached name */
    private String name;
    /** cached display name */
    private String displayName;

    private static final String UNKNOWN = new String();

    /** cached short description */
    private String shortDescription;

    static final long serialVersionUID =3726728244698316872L;
    /** Constructor that creates template for the node.
    */
    private VisualizerNode (int hashCode) {
        this.hashCode = hashCode;
        this.node = null;
    }

    /** Creates new VisualizerNode
    * @param n node to refer to
    */
    private VisualizerNode(Node n) {
        node = n;
        hashCode = System.identityHashCode (node);

        // attach as a listener
        node.addNodeListener (WeakListener.node (this, node));
        // uiListener = WeakListener.propertyChange (this, null);
        // UIManager.addPropertyChangeListener (uiListener);

	name = UNKNOWN;
        displayName = UNKNOWN;
        shortDescription = UNKNOWN;
    }


    /** Returns cached short description.
     * @return short description of represented node
     */    
    public String getShortDescription() {
	String desc = shortDescription;
	if( desc == UNKNOWN ) {
            shortDescription = desc = node.getShortDescription ();
	}
	return desc;
    }

    /** Returns cached display name.
     * @return display name of represented node
     */    
    public String getDisplayName () {
        if (displayName == UNKNOWN) {
            displayName = node == null ? null : node.getDisplayName ();
        }
        return displayName;
    }
    
    /** Returns cached name.
     * @return name of represented node
     */    
    public String getName () {
        if (name == UNKNOWN) {
            name = node == null ? null : node.getName ();
        }
        return name;
    }

    /** Getter for list of children of this visualizer.
    * @return list of VisualizerNode objects
    */
    public List getChildren () {
        VisualizerChildren ch = (VisualizerChildren)children.get ();
        if (ch == null && !node.isLeaf ()) {
            // initialize the nodes children before we enter
            // the readAccess section
            Node[] tmpInit = node.getChildren ().getNodes ();
            // go into lock to ensure that no childrenAdded, childrenRemoved,
            // childrenReordered notifications occures and that is why we do
            // not loose any changes
            ch = (VisualizerChildren)Children.MUTEX.readAccess (new Mutex.Action () {
                        public Object run () {
                            Node[] nodes = node.getChildren ().getNodes ();
                            VisualizerChildren vc = new VisualizerChildren (
                                                        VisualizerNode.this, nodes
                                                    );
                            notifyVisualizerChildrenChange (nodes.length, vc);
                            return vc;
                        }
                    });
        }
        return ch == null ? Collections.EMPTY_LIST : ch.list;
    }

    //
    // TreeNode interface (callable only from AWT-Event-Queue)
    //

    public int getIndex(final javax.swing.tree.TreeNode p1) {
        return getChildren ().indexOf (p1);
    }

    public boolean getAllowsChildren() {
        return !isLeaf ();
    }

    public javax.swing.tree.TreeNode getChildAt(int p1) {
        List ch = getChildren();
        VisualizerNode vn = (VisualizerNode)ch.get (p1);
        if (vn == null) {
            System.out.println("Children are: "); // NOI18N
            for (Iterator it = ch.iterator(); it.hasNext(); ) {
                System.out.println("    " + it.next()); // NOI18N
            }
            throw new IllegalStateException("VisualizerNode.getChildAt() returning null!"); // NOI18N
        }
        return vn;
    }

    public int getChildCount() {
        return getChildren ().size ();
    }

    public java.util.Enumeration children() {
        if (err.isLoggable(ErrorManager.INFORMATIONAL)) {
            List l = getChildren();
            if (l.contains(null)) {
                err.log("Children are: "); // NOI18N
                for (Iterator it = l.iterator(); it.hasNext(); ) {
                    err.log("    " + it.next()); // NOI18N
                }
                throw new IllegalStateException("VisualizerNode.children() contains null!"); // NOI18N
            }
            return java.util.Collections.enumeration(l);
        }
        return java.util.Collections.enumeration (getChildren ());
    }

    public boolean isLeaf() {
        return node.isLeaf ();
    }

    public javax.swing.tree.TreeNode getParent() {
        Node parent = node.getParentNode ();
        return parent == null ? null : getVisualizer (null, parent);
    }

    // **********************************************
    // Can be called under Children.MUTEX.writeAccess
    // **********************************************

    /** Fired when a set of new children is added.
    * @param ev event describing the action
    */
    public void childrenAdded(NodeMemberEvent ev) {
        VisualizerChildren ch = (VisualizerChildren)children.get ();
        if (ch == null) return;

        QUEUE.runSafe (new VisualizerEvent.Added (
            ch, ev.getDelta (), ev.getDeltaIndices ()
        ));
    }
    
    /** Fired when a set of children is removed.
    * @param ev event describing the action
    */
    public void childrenRemoved(NodeMemberEvent ev) {
        VisualizerChildren ch = (VisualizerChildren)children.get ();
        if (ch == null) return;
        
        QUEUE.runSafe (new VisualizerEvent.Removed (ch,  ev.getDelta ()) );
    }

    /** Fired when the order of children is changed.
    * @param ev event describing the change
    */
    public void childrenReordered(NodeReorderEvent ev) {
        doChildrenReordered (ev.getPermutation ());
    }
    
    // helper method (called from TreeTableView.sort)
    void doChildrenReordered (int[] perm) {
        VisualizerChildren ch = (VisualizerChildren)children.get ();
        if (ch == null) return;

        QUEUE.runSafe (new VisualizerEvent.Reordered (ch, perm));
    }

    /** Fired when the node is deleted.
    * @param ev event describing the node
    */
    public void nodeDestroyed(NodeEvent ev) {
        // ignore for now
    }

    /** Change in the node properties (icon, etc.)
    */
    public void propertyChange(final java.beans.PropertyChangeEvent evt) {
        String name = evt.getPropertyName ();
        if (
            Node.PROP_NAME.equals (name) ||
            Node.PROP_DISPLAY_NAME.equals (name) ||
            Node.PROP_SHORT_DESCRIPTION.equals (name) ||
            Node.PROP_ICON.equals (name) ||
            Node.PROP_OPENED_ICON.equals (name)
        ) {
            SwingUtilities.invokeLater (this);
            return;
        }
        if ( Node.PROP_LEAF.equals( name ) ) {
            SwingUtilities.invokeLater( new Runnable() {
                
                public void run() {
                    children = NO_REF;
                    // notify models               
                    VisualizerNode parent = VisualizerNode.this;
                    while (parent != null) {
                        Object[] listeners = parent.getListenerList ();
                        for (int i = listeners.length - 1; i >= 0; i -= 2) {
                            ((NodeModel)listeners[i]).structuralChange (VisualizerNode.this);
                        }
                        parent = (VisualizerNode)parent.getParent ();
                    }
                } 
            } );
        }
        /*
        if (
            "lookAndFeel".equals (name) // NOI18N
        ) {
            SwingUtilities.invokeLater (this);
        }
        */
    }

    /** Update the state of this class by retrieving new name, etc.
    * And fire change to all listeners. Only by AWT-Event-Queue
    */
    
    public void run () {
        name = node.getName ();
        displayName = node.getDisplayName ();
        shortDescription = UNKNOWN;

        //
        // notify models
        //
        VisualizerNode parent = this;
        while (parent != null) {
            Object[] listeners = parent.getListenerList ();
            for (int i = listeners.length - 1; i >= 0; i -= 2) {
                ((NodeModel)listeners[i]).update (this);
            }
            parent = (VisualizerNode)parent.getParent ();
        }
    }
    

    //
    // Access to VisualizerChildren
    //

    /** Notifies change in the amount of children. This is used to distinguish between
    * weak and hard reference. Called from VisualizerChildren
    * @param size amount of children
    * @param ch the children
    */
    void notifyVisualizerChildrenChange (int size, VisualizerChildren ch) {
        if (size == 0) {
            // hold the children hard
            children = new StrongReference (ch);
        } else {
            children = new WeakReference (ch);
        }
    }

    // ********************************
    // This can be called from anywhere
    // ********************************

    /** Adds visualizer listener.
    */
    public synchronized void addNodeModel (NodeModel l) {
        add (NodeModel.class, l);
    }

    /** Removes visualizer listener.
    */
    public synchronized void removeNodeModel (NodeModel l) {
        remove (NodeModel.class, l);
    }

    /** Hash code
    */
    public int hashCode () {
        return hashCode;
    }

    /** Equals two objects are equal if they have the same hash code
    */
    public boolean equals (Object o) {
        if (!(o instanceof VisualizerNode)) return false;
        VisualizerNode v = (VisualizerNode)o;
        return v.node == node;
    }
    
    /** String name is taken from the node.
    */
    public String toString () {
        return getDisplayName ();
    }

    /** Strong reference.
    */
    private static final class StrongReference extends WeakReference {
        private Object o;
        public StrongReference (Object o) {
            super (null);
            this.o = o;
        }

        public Object get () {
            return o;
        }
    }
    
    
    /** Class that processes runnables in event queue. It guarantees that
    * the order of processed objects will be exactly the same as they
    * arrived.
    */
    private static final class QP extends Object implements Runnable {
        QP() {}
        /** queue of all requests (Runnable) that should be processed
         * AWT-Event queue.
         */
        private QueueEnumeration queue = null;

        /** Runs the runnable in event thread.
         * @param run what should run
         */
        public void runSafe (Runnable run) {
            boolean isNew = false;
            
            synchronized (this) {
                // access to queue variable is synchronized
                if (queue == null) {
                    queue = new QueueEnumeration ();
                    isNew = true;
                }
                queue.put (run);
            }
            
            if (isNew) {
                // either starts the processing of the queue immediatelly
                // (if we are in AWT-Event thread) or uses 
                // SwingUtilities.invokeLater to do so
                Mutex.EVENT.writeAccess (this);
            }
        }
        
        /** Processes the queue.
         */ 
        public void run () {
            QueueEnumeration en;
            synchronized (this) {
                // access to queue variable is synchronized
                en = queue;
                queue = null;
            }
  
            while (en.hasMoreElements ()) {
                Runnable r = (Runnable)en.nextElement ();
                r.run ();
            }
        }
    }
}
