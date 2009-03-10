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
import java.beans.*;
import java.util.*;


import org.openide.util.Mutex;
import org.openide.util.enums.ArrayEnumeration;

/** Container for array of nodes.
* Can be {@link Node#Node associated} with a node and then
* all children in the array have that node set as a parent, and this list
* will be returned as the node's children.
*
* <p>Probably you want to subclass {@link Children.Keys}.
* Subclassing <code>Children</code> directly is not recommended.
*
* @author Jaroslav Tulach
*/
public abstract class Children extends Object {
    /** A package internal accessor object to provide priviledged
     * access to children.
     */
    static final Mutex.Privileged PR = new Mutex.Privileged ();
    
    /** Lock for access to hierarchy of all node lists.
    * Anyone who needs to ensure that there will not
    * be shared accesses to hierarchy nodes can use this
    * mutex.
    * <P>
    * All operations on the hierarchy of nodes (add, remove, etc.) are
    * done in the {@link Mutex#writeAccess} method of this lock, so if someone
    * needs for a certain amount of time to forbid modification,
    * he can execute his code in {@link Mutex#readAccess}.
    */
    public static final Mutex MUTEX = new Mutex (PR);

    /** The object representing an empty set of children. Should
    * be used to represent the children of leaf nodes. The same
    * object may be used by all such nodes.
    */
    public static final Children LEAF = new Empty ();

    /** parent node for all nodes in this list (can be null) */
    private Node parent;

    /** mapping from entries to info about them (Entry, Info) */
    private java.util.Map map;
    /** collection of all entries */
    private Collection entries = Collections.EMPTY_LIST;
    /** array of children Reference (ChildrenArray) */
    Reference array = new WeakReference (null);

    /*
      private StringBuffer debug = new StringBuffer ();
      
      private void printStackTrace() {
        Exception e = new Exception ();
        java.io.StringWriter w1 = new java.io.StringWriter ();
        java.io.PrintWriter w = new java.io.PrintWriter (w1);
        e.printStackTrace(w);
        w.close ();
        debug.append (w1.toString ());
        debug.append ('\n');
      }
    */

    /** Constructor.
    */
    public Children () {
    }

    /** Setter of parent node for this list of children. Each children in the list
    * will have this node set as parent. The parent node will return nodes in
    * this list as its children.
    * <P>
    * This method is called from the Node constructor
    *
    * @param n node to attach to
    * @exception IllegalStateException when this object is already used with
    *    different node
    */
    final void attachTo (final Node n) throws IllegalStateException {
        // special treatment for LEAF object.
        if (this == LEAF) {
            // do not attaches the node because the LEAF cannot have children
            // and that is why it need not set parent node for them
            return;
        }

        synchronized (this) {
            if (parent != null) {
                // already used
                throw new IllegalStateException ("An instance of Children may not be used for more than one parent node."); // NOI18N
            }

            // attach itself as a node list for given node
            parent = n;
        }


        // this is the only place where parent is changed,
        // but only under readAccess => double check if
        // it happened correctly
        try {
            PR.enterReadAccess ();
            
            Node[] nodes = testNodes ();
            if (nodes == null) return;


            // fire the change
            for (int i = 0; i < nodes.length; i++) {
                Node node = nodes[i];
                node.assignTo (Children.this, i);
                node.fireParentNodeChange (null, parent);
            }
        } finally {
            PR.exitReadAccess ();
        }
    }

    
    /** Called when the node changes it's children to different nodes.
    *
    * @param n node to detach from
    * @exception IllegalStateException if children were already detached
    */
    final void detachFrom ()  {
        // special treatment for LEAF object.
        if (this == LEAF) {
            // no need to do anything
            return;
        }

        Node oldParent = null;
        
        synchronized (this) {
            if (parent == null) {
                // already detached
                throw new IllegalStateException ("Trying to detach children which do not have parent"); // NOI18N
            }

            // remember old parent 
            oldParent = parent;
            
            // attach itself as a node list for given node
            parent = null;
        }


        // this is the only place where parent is changed,
        // but only under readAccess => double check if
        // it happened correctly
        try {
            PR.enterReadAccess ();
            
            Node[] nodes = testNodes ();
            if (nodes == null) return;


            // fire the change
            for (int i = 0; i < nodes.length; i++) {
                Node node = nodes[i];
                node.deassignFrom (Children.this);
                node.fireParentNodeChange (oldParent, null);
            }
        } finally {
            PR.exitReadAccess ();
        }
    }
    
    /** Get the parent node of these children.
    * @return the node attached to this children object, or <code>null</code> if there is none yet
    */
    protected final Node getNode () {
        return parent;
    }


    /** Allows access to the clone method for Node.
    * @return cloned hierarchy
    * @exception CloneNotSupportedException if not supported
    */
    final Object cloneHierarchy () throws CloneNotSupportedException {
        return clone ();
    }

    /** Handles cloning in the right way, that can be later extended by
    * subclasses. Of course each subclass that wishes to support cloning
    * must implement the <code>Cloneable</code> interface, otherwise this method throws
    * <code>CloneNotSupportedException</code>.
    *
    * @return cloned version of this object, with the same class, uninitialized and without
    *   a parent node
    * *exception CloneNotSupportedException if <code>Cloneable</code> interface is not implemented
    */
    protected Object clone () throws CloneNotSupportedException {
        Children ch = (Children)super.clone ();

        ch.parent = null;
        ch.map = null;
        ch.entries = Collections.EMPTY_LIST;
        ch.array = new WeakReference (null);

        return ch;
    }


    /** Add nodes this container.
    * The parent node of these nodes
    * is changed to the parent node of this list. Each node can be added
    * only once. If there is some reason a node cannot be added, for example
    * if the node expects only a special type of subnodes, the method should
    * do nothing and return <code>false</code> to signal that the addition has not been successful.
    * <P>
    * This method should be implemented by subclasses to filter some nodes, etc.
    *
    * @param nodes set of nodes to add to the list
    * @return <code>true</code> if successfully added
    */
    public abstract boolean add (final Node[] nodes);

    /** Remove nodes from the list. Only nodes that are present are
    * removed.
    *
    * @param nodes nodes to be removed
    * @return <code>true</code> if the nodes could be removed
    */
    public abstract boolean remove (final Node[] nodes);

    /** Get the nodes as an enumeration.
    * @return enumeration of {@link Node}s
    */
    public final Enumeration nodes () {
        return new ArrayEnumeration (getNodes ());
    }

    /** Find a child node by name.
    * This may be overridden in subclasses to provide a more advanced way of finding the 
    * child, but the default implementation simply scans through the list of nodes 
    * to find the first one with the requested name.
    * <p>Normally the list of nodes should have been computed by the time this returns,
    * but see {@link #getNodes()} for an important caveat as to why this may not
    * be doing what you want and what to do instead.
    * @param name (code) name of child node to find or <code>null</code> if any arbitrary child may
    *    be returned
    * @return the node or <code>null</code> if it could not be found
    */
    public Node findChild (String name) {
        Node[] list = getNodes ();
        
        if (list.length == 0) {
            return null;
        }
        
        if (name == null) {
            // return any node
            return list[0];
        }
        
        
        for (int i = 0; i < list.length; i++) {
            if (name.equals (list[i].getName ())) {
                // ok, we have found it
                return list[i];
            }
        }
        return null;
    }

    /** Method that can be used to test whether the children content has
    * ever been used or it is still not initalized.
    * @return true if children has been used before
    * @see #addNotify
    */
    protected final boolean isInitialized () {
        ChildrenArray arr = (ChildrenArray)array.get ();
        return arr != null && arr.isInitialized ();
    }

    /** Get a (sorted) array of nodes in this list.
     * If the children object is not yet initialized,
     * it will be (using {@link #addNotify}) before
     * the nodes are returned.
     * <p><strong>Warning:</strong> not all children
     * implementations do a complete calculation at
     * this point, see {@link #getNodes(boolean)}
     * @return array of nodes
     */
    //  private static String off = ""; // NOI18N

    public final Node[] getNodes () {
        //Thread.dumpStack();
        //System.err.println(off + "getNodes: " + getNode ());
        boolean[] results = new boolean[2];
        for (;;) {
            results[1] = isInitialized ();
            
            // initializes the ChildrenArray possibly calls 
            // addNotify if this is for the first time
            ChildrenArray array = getArray(results); // fils results[0]
            
            //System.err.println(off + "  initialized: " + initialized);
            //      off = off + "  "; // NOI18N
            // forbid any modifications to this hierarchy
            Node[] nodes;
            try {
                PR.enterReadAccess ();
                
                nodes = array.nodes ();
            } finally {
                PR.exitReadAccess ();
            }
            
            //      off = off.substring (2);
            //System.err.println(off + "  length     : " + nodes.length);
            //System.err.println(off + "  entries    : " + entries);
            //System.err.println(off + "  init now   : " + isInitialized());
            // if not initialized that means that after
            // we computed the nodes, somebody changed them (as a
            // result of addNotify) => we have to compute them
            // again
            if (results[1]) {
                // otherwise it is ok.
                return nodes;
            }
            
            if (results[0]) {
                // looks like the result cannot be computed, just give empty one
                return nodes == null ? new Node[0] : nodes;
            }
        }
    }
    
    /** Get a (sorted) array of nodes in this list.
     *
     * This method is usefull if you need a fully initialized array of nodes
     * for things like MenuView, node navigation from scripts/tests and so on.
     * But in general if you are trying to get useful data by calling
     * this method, you are probably doing something wrong.
     * Usually you should be asking some underlying model
     * for information, not the nodes for children. For example,
     * {@link org.openide.loaders.DataFolder#getChildren DataFolder.getChildren}
     * is a much more appropriate way to get what you want.
     *
     * If you're extending children, you should make sure this method
     * will return a complete list of nodes. The default implementation will do
     * this correctly so long as your subclass implement findChild(null)
     * to initialize all subnodes.
     *
     * <p><strong>Note:</strong>You should not call this method from inside
     * <code>{@link org.openide.nodes.Children#MUTEX Children.MUTEX}.readAccess()</code>.
     * If you do so, the <code>Node</code> will be unable to update its state
     * before you leave the <code>readAccess()</code>.
     *
     * @since OpenAPI version 2.17
     *
     * @param optimalResult whether to try to get a fully initialized array
     * or to simply delegate to {@link #getNodes()}
     * @return array of nodes
     */
    public Node[] getNodes(boolean optimalResult) {
        ChildrenArray arr;
        if (optimalResult) {
            arr = getArray(null);
            findChild(null);
        }
        return getNodes();
    }

    /** Get the number of nodes in the list.
    * @return the count
    */
    public final int getNodesCount () {
        return getNodes ().length;
    }

    //
    // StateNotifications
    //

    /** Called when children are first asked for nodes.
     * Typical implementations at this time calculate
     * their node list (or keys for {@link Children.Keys} etc.).<BR>
     * Notice: call to getNodes() inside of this method will return
     * an empty array of nodes.
     * @see #isInitialized
    */
    protected void addNotify () {
    }

    /** Called when the list of nodes for this children
     * object is no longer needed by the IDE.
     * Typical implementations at this time remove all
     * children to save memory (or clear the keys for
     * {@link Children.Keys} etc.).
    */
    protected void removeNotify () {
    }

    /** Method that can be overriden in subclasses to
    * do additional work and then call addNotify.
    */
    void callAddNotify () {
        addNotify ();
    }

    //
    // ChildrenArray operations call only under lock
    //

    /** @return either nodes associated with this children or null if
    * they are not created
    */
    private Node[] testNodes () {
        ChildrenArray arr = (ChildrenArray)array.get ();
        return arr == null ? null : arr.nodes ();
    }

    private static final Object LOCK = new Object ();
    /** Obtains references to array holder. If it does not exist, it is
    * created.
     *
     * @param cannotWorkBetter array of size 1 or null, will contain true, if 
     *    the getArray cannot be initialized (we are underread access
     *    and nother thread is responbile for initialization, in such case
     *    give up on computation of best result
    */
    private Thread initThread;
    private ChildrenArray getArray (boolean[] cannotWorkBetter) {
        ChildrenArray arr;
        boolean doInitialize = false;
        synchronized (LOCK) {
             arr = (ChildrenArray)array.get ();
             if (arr == null) {
                 arr = new ChildrenArray ();
                 // register the array with the children
                 registerChildrenArray (arr, true);
                 doInitialize = true;
                 initThread = Thread.currentThread();
             }
        }
        
        if (doInitialize) {
            // this call can cause a lot of callbacks => be prepared
            // to handle them as clean as possible
            try {
                this.callAddNotify ();
            }
            finally {    
                synchronized (LOCK) {
                    // now attach to children, so when children == null => we are
                    // not fully initialized!!!!
                    arr.children = this;
                    initThread = null;
                    LOCK.notifyAll ();                    
                }
            }
        } else {
            // otherwise, if not initialize yet (arr.children) wait 
            // for the initialization to finish, but only if we can wait
            
            // we are not in ReadAccess
            // Children.MUTEX.isReadAccess, if such call would be added to Mutex
            class MutexChecker implements Runnable {
                public boolean inReadAccess = true;
                public void run () {
                    inReadAccess = false;
                }
            }
            MutexChecker test = new MutexChecker();
            // the code will run either immediatally or after we leave readAccess
            // section
            Children.MUTEX.postWriteRequest(test);

            if (test.inReadAccess || initThread == Thread.currentThread()) {
                // fail, we are in read access
                if (cannotWorkBetter != null) {
                    cannotWorkBetter[0] = true;
                }
                return arr;
            }
            
            // otherwise we can wait
            synchronized (LOCK) {
                while (arr.children == null) {
                    try {
                        LOCK.wait ();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
                    
        return arr;
    }

    /** Clears the nodes
    */
    private void clearNodes () {
        ChildrenArray arr = (ChildrenArray)array.get ();
        //System.err.println(off + "  clearNodes: " + getNode ());
        if (arr != null) {
            // clear the array
            arr.clear ();
        }
    }

    /** Forces finalization of nodes for given info.
    * Called from finalizer of Info.
    */
    final void finalizeNodes () {
        ChildrenArray arr = (ChildrenArray)array.get ();
        if (arr != null) {
            arr.finalizeNodes ();
        }
    }


    /** Registration of ChildrenArray.
    * @param array use weak or hard references
    * @param weak use weak or hard reference
    */
    final void registerChildrenArray (final ChildrenArray array, boolean weak) {
        if (weak) {
            this.array = new WeakReference (array);
        } else {
            // hold the children hard
            this.array = new WeakReference (array) {
                             public Object get () {
                                 return array;
                             }
                         };
        }
    }

    /** Finalized.
    */
    final void finalizedChildrenArray () {
        // usually in removeNotify setKeys is called => better require write access
        try {
            PR.enterWriteAccess ();
            
            if (array.get () == null) {
                // really finalized and not reconstructed
                removeNotify ();
            }
        } finally {
            PR.exitWriteAccess ();
        }
    }

    /** Computes the nodes now.
    */
    final Node[] justComputeNodes () {
        if (map == null) {
            map = new HashMap (17);
            //      debug.append ("Map initialized\n"); // NOI18N
            //      printStackTrace();
        }

        LinkedList l = new LinkedList ();
        Iterator it = entries.iterator ();
        while (it.hasNext ()) {
            Entry entry = (Entry)it.next ();
            Info info = findInfo (entry);
            try {
                l.addAll (info.nodes ());
            } catch (RuntimeException ex) {
                NodeOp.warning (ex);
            }
        }

        Node[] arr = (Node[])l.toArray (new Node[l.size ()]);

        // initialize parent nodes
        for (int i = 0; i < arr.length; i++) {
            Node n = arr[i];
            n.assignTo (this, i);
            n.fireParentNodeChange (null, parent);
        }

        return arr;
    }

    /** Finds info for given entry, or registers
    * it, if not registered yet.
    */
    private Info findInfo (Entry entry) {
        Info info = (Info)map.get (entry);
        if (info == null) {
            info = new Info (entry);
            map.put (entry, info);
            //      debug.append ("Put: " + entry + " info: " + info); // NOI18N
            //      debug.append ('\n');
            //      printStackTrace();
        }
        return info;
    }

    //
    // Entries
    //

    /** Access to copy of current entries.
    * @return copy of entries in the objects
    */
    final ArrayList getEntries () {
        return new ArrayList (this.entries);
    }

    final void setEntries (Collection entries) {
        // current list of nodes
        ChildrenArray holder = (ChildrenArray)array.get ();
        if (holder == null) {
            //      debug.append ("Set1: " + entries); // NOI18N
            //      printStackTrace();
            this.entries = entries;
            if (map != null) map.keySet ().retainAll ( new HashSet( entries ) );
            
            return;
        }
        Node[] current = holder.nodes ();
        if (current == null) {
            // the initialization is not finished yet =>
            //      debug.append ("Set2: " + entries); // NOI18N
            //      printStackTrace();
            this.entries = entries;
            if (map != null) map.keySet ().retainAll ( new HashSet( entries ) );
            return;
        }

        // if there are old items in the map, remove them to
        // reflect current state
        map.keySet ().retainAll ( new HashSet( this.entries ) );

        // what should be removed
        HashSet toRemove = new HashSet (map.keySet ());
        HashSet entriesSet = new HashSet (entries);
        toRemove.removeAll (entriesSet);

        if (!toRemove.isEmpty ()) {
            // notify removing, the set must be ready for
            // callbacks with questions
            updateRemove (current, toRemove);
            current = holder.nodes ();
        }

        // change the order of entries, notifies
        // it and again brings children to up-to-date state
        Collection toAdd = updateOrder (current, entries);

        if (!toAdd.isEmpty ()) {

            // toAdd contains Info objects that should
            // be added
            updateAdd (toAdd, entries);
        }
    }

    /** Removes the objects from the children.
    */
    private void updateRemove (Node[] current, Set toRemove) {
        LinkedList nodes = new LinkedList ();

        Iterator it = toRemove.iterator ();
        while (it.hasNext ()) {
            Entry en = (Entry)it.next ();
            Info info = (Info)map.remove (en);
            //debug.append ("Removed: " + en + " info: " + info); // NOI18N
            //debug.append ('\n');
            //printStackTrace();
            nodes.addAll (info.nodes ());  // Has a NullPointerException been thrown?
                                            // In such case consider a key implementation.
                                            // The key hashCode() and equals() methods
                                            // must behave as for an IMMUTABLE object and
                                            // the hashCode() must return the same value
                                            // for equals() keys.  Ccc
        }

        // modify the current set of entries and empty the list of nodes
        // so it has to be recreated again
        //debug.append ("Current : " + this.entries + '\n'); // NOI18N
        this.entries.removeAll (toRemove);
        //debug.append ("Removing: " + toRemove + '\n'); // NOI18N
        //debug.append ("New     : " + this.entries + '\n'); // NOI18N
        //printStackTrace();

        clearNodes ();

        notifyRemove (nodes, current);
    }

    /** Notifies that a set of nodes has been removed from
    * children. It is necessary that the system is already 
    * in consistent state, so any callbacks will return 
    * valid values.
    *
    * @param nodes list of removed nodes
    * @param current state of nodes
    * @return array of nodes that were deleted
    */
    Node[] notifyRemove (Collection nodes, Node[] current) {
        //System.err.println("notifyRemove from: " + getNode ());
        //System.err.println("notifyRemove: " + nodes);
        //System.err.println("Current     : " + Arrays.asList (current));
        //Thread.dumpStack();
        //Keys.last.printStackTrace();


        // [TODO] Children do not have always a parent
        // see Services->FIRST ($SubLevel.class)
        // during a deserialization it may have parent == null

        Node[] arr = (Node[])nodes.toArray (new Node[nodes.size ()]);

        if (parent == null) {
            return arr;
        }

        // fire change of nodes
        parent.fireSubNodesChange (
            false,  // remove
            arr,
            current
        );

        // fire change of parent
        Iterator it = nodes.iterator ();
        while (it.hasNext ()) {
            Node n = (Node)it.next ();
            n.deassignFrom (this);
            n.fireParentNodeChange (parent, null);
        }

        return arr;
    }

    /** Updates the order of entries.
    * @param current current state of nodes
    * @param entries new set of entries
    * @return list of infos that should be added
    */
    private List updateOrder (Node[] current, Collection entries) {
        LinkedList toAdd = new LinkedList ();

        // that assignes entries their begining position in the array
        // of nodes
        HashMap offsets = new HashMap ();
        {
            int previousPos = 0;

            Iterator it = this.entries.iterator ();
            while (it.hasNext ()) {
                Entry entry = (Entry)it.next ();

                Info info = (Info)map.get (entry);

                if (info == null) {
                    throw new IllegalStateException(
                        "Error in " + getClass().getName() + " with entry " + entry + // NOI18N
                        " probably caused by faulty key implementation." + // NOI18N
                        " The key hashCode() and equals() methods must behave as for an IMMUTABLE object" + // NOI18N
                        " and the hashCode() must return the same value for equals() keys."); // NOI18N
                }
                
                offsets.put (info, new Integer (previousPos));

                previousPos += info.length ();
            }
        }

        // because map can contain some additional items,
        // that has not been garbage collected yet,
        // retain only those that are in current list of
        // entries
        map.keySet ().retainAll ( new HashSet( this.entries ) );

        int[] perm = new int[current.length];
        int currentPos = 0;
        int permSize = 0;
        LinkedList reorderedEntries = null;

        Iterator it = entries.iterator ();
        while (it.hasNext ()) {
            Entry entry = (Entry)it.next ();

            Info info = (Info)map.get (entry);
            if (info == null) {
                // this info has to be added
                info = new Info (entry);
                toAdd.add (info);
            } else {
                int len = info.length ();
                if (reorderedEntries == null) {
                    reorderedEntries = new LinkedList ();
                }
                reorderedEntries.add (entry);

                // already there => test if it should not be reordered
                Integer previousInt = (Integer)offsets.get (info);
                /*
                        if (previousInt == null) {
                          System.err.println("Offsets: " + offsets);
                          System.err.println("Info: " + info);
                          System.err.println("Entry: " + info.entry);
                          System.err.println("This entries: " + this.entries);
                          System.err.println("Entries: " + entries);
                          System.err.println("Map: " + map);
                          
                          System.err.println("---------vvvvv");
                          System.err.println(debug);
                          System.err.println("---------^^^^^");
                          
                        }
                */        
                int previousPos = previousInt.intValue ();
                if (currentPos != previousPos) {
                    for (int i = 0; i < len; i++) {
                        perm[previousPos + i] = 1 + currentPos + i;
                    }
                    permSize += len;
                }
            }

            currentPos += info.length ();
        }

        if (permSize > 0) {
            // now the perm array contains numbers 1 to ... and
            // 0 one places where no permutation occures =>
            // decrease numbers, replace zeros

            for (int i = 0; i < perm.length; i++) {
                if (perm[i] == 0) {
                    // fixed point
                    perm[i] = i;
                } else {
                    // decrease
                    perm[i]--;
                }
            }

            // reorderedEntries are not null
            this.entries = reorderedEntries;
            //      debug.append ("Set3: " + this.entries); // NOI18N
            //      printStackTrace();

            // notify the permutation to the parent
            clearNodes ();
            //System.err.println("Paremutaiton! " + getNode ());
            Node p = parent;
            if (p != null) {
                p.fireReorderChange (perm);
            }
        }

        return toAdd;
    }

    /** Updates the state of children by adding given Infos.
    * @param infos list of Info objects to add
    * @param entries the final state of entries that should occur
    */
    private void updateAdd (Collection infos, Collection entries) {
        LinkedList nodes = new LinkedList ();
        Iterator it = infos.iterator ();
        while (it.hasNext ()) {
            Info info = (Info)it.next ();
            nodes.addAll (info.nodes ());
            map.put (info.entry, info);
            //      debug.append ("updateadd: " + info.entry + " info: " + info + '\n'); // NOI18N
            //      printStackTrace();
        }

        this.entries = entries;
        //      debug.append ("Set4: " + entries); // NOI18N
        //      printStackTrace();

        clearNodes ();

        notifyAdd (nodes);
    }

    /** Notifies that a set of nodes has been add to
    * children. It is necessary that the system is already 
    * in consistent state, so any callbacks will return 
    * valid values.
    *
    * @param nodes list of removed nodes
    */
    private void notifyAdd (Collection nodes) {
        // notify about parent change
        Iterator it = nodes.iterator ();
        while (it.hasNext ()) {
            Node n = (Node)it.next ();

            n.assignTo (this, -1);
            n.fireParentNodeChange (null, parent);
        }
        Node[] arr = (Node[])nodes.toArray (new Node[nodes.size ()]);

        Node n = parent;
        if (n != null) {
            n.fireSubNodesChange (
                true, arr, null
            );
        }
    }


    /** Refreshes content of one entry. Updates the state of children
    * appropriatelly.
    */
    final void refreshEntry (Entry entry) {
        // current list of nodes
        ChildrenArray holder = (ChildrenArray)array.get ();
        if (holder == null) {
            return;
        }

        Node[] current = holder.nodes ();
        if (current == null) {
            // the initialization is not finished yet =>
            return;
        }

        // because map can contain some additional items,
        // that has not been garbage collected yet,
        // retain only those that are in current list of
        // entries
        map.keySet ().retainAll ( new HashSet( this.entries ) );

        Info info = (Info)map.get (entry);
        if (info == null) {
            // refresh of entry that is not present =>
            return;
        }


        Collection oldNodes = info.nodes ();
        Collection newNodes = info.entry.nodes ();

        if (oldNodes.equals (newNodes)) {
            // nodes are the same =>
            return;
        }

        HashSet toRemove = new HashSet (oldNodes);
        toRemove.removeAll (newNodes);

        if (!toRemove.isEmpty ()) {
            // notify removing, the set must be ready for
            // callbacks with questions

            // modifies the list associated with the info
            oldNodes.removeAll (toRemove);
            clearNodes ();

            // now everything should be consistent => notify the remove
            notifyRemove (toRemove, current);

            current = holder.nodes ();
        }

        List toAdd = refreshOrder (entry, oldNodes, newNodes);
        info.useNodes (newNodes);

        if (!toAdd.isEmpty ()) {
            // modifies the list associated with the info
            clearNodes ();
            notifyAdd (toAdd);
        }
    }

    /** Updates the order of nodes after a refresh.
    * @param entry the refreshed entry
    * @param oldNodes nodes that are currently in the list
    * @param newNodes new nodes (defining the order of oldNodes and some more)
    * @return list of infos that should be added
    */
    private List refreshOrder (Entry entry, Collection oldNodes, Collection newNodes) {
        LinkedList toAdd = new LinkedList ();

        int currentPos = 0;

        // cycle thru all entries to find index of the entry
        Iterator it = this.entries.iterator ();
        for (;;) {
            Entry e = (Entry)it.next ();
            if (e.equals (entry)) {
                break;
            }
            Info info = findInfo (e);
            currentPos += info.length ();
        }

        HashSet oldNodesSet = new HashSet (oldNodes);
        HashSet toProcess = (HashSet)oldNodesSet.clone ();

        Node[] permArray = new Node[oldNodes.size ()];
        it = newNodes.iterator ();
        int pos = 0;
        while (it.hasNext ()) {
            Node n = (Node)it.next ();
            if (oldNodesSet.remove (n)) {
                // the node is in the old set => test for permuation
                permArray[pos++] = n;
            } else {
                if (!toProcess.contains (n)) {
                    // if the node has not been processed yet
                    toAdd.add (n);
                } else {
                    it.remove ();
                }
            }
        }

        // JST: If you get IllegalArgumentException in following code
        // then it can be cause by wrong synchronization between
        // equals and hashCode methods. First of all check them!
        int[] perm = NodeOp.computePermutation (
                         (Node[])oldNodes.toArray (new Node[oldNodes.size ()]),
                         permArray
                     );
        if (perm != null) {
            // apply the permutation
            clearNodes ();
            // temporarily change the nodes the entry should use
            findInfo (entry).useNodes (Arrays.asList (permArray));
            Node p = parent;
            if (p != null) {
                p.fireReorderChange (perm);
            }
        }

        return toAdd;
    }


    /** Information about an entry. Contains number of nodes,
    * position in the array of nodes, etc.
    */
    final class Info extends Object {
        int length;
        Entry entry;

        public Info (Entry entry) {
            this.entry = entry;
        }

        /** Finalizes the content of ChildrenArray.
        */
        protected void finalize () {
            finalizeNodes ();
        }

        public Collection nodes () {
            // forces creation of the array
            ChildrenArray arr = getArray (null);

            return arr.nodesFor (this);
        }

        public void useNodes (Collection nodes) {
            // forces creation of the array
            ChildrenArray arr = getArray (null);

            arr.useNodes (this, nodes);

            // assign all there nodes the new children
            Iterator it = nodes.iterator ();
            while (it.hasNext ()) {
                Node n = (Node)it.next ();
                n.assignTo (Children.this, -1);
                n.fireParentNodeChange (null, parent);
            }
        }

        public int length () {
            return length;
        }
    }

    /** Interface that provides a set of nodes.
    */
    static interface Entry {
        /** Set of nodes associated with this entry.
        * @return list of Node objects
        */
        public Collection nodes ();
    }

    /** Empty list of children. Does not allow anybody to insert a node.
    * Treated especially in the attachTo method.
    */
    private static final class Empty extends Children {
        Empty() {}
        /** @return false, does no action */
        public boolean add (Node[] nodes) {
            return false;
        }

        /** @return false, does no action */
        public boolean remove (Node[] nodes) {
            return false;
        }
    }

    /** Implements the storage of node children by an array.
    * Each new child is added at the end of the array. The nodes are
    * returned in the order they were inserted.
    *
    * <p>Normally you will simply create an instance of
    * <code>Children.Array</code> and add some nodes to it.
    * If you expect the child nodes to change dynamically,
    * {@link Children.Keys} is preferable.
    */
    public static class Array extends Children implements Cloneable {
        /** the entry used for all nodes in the following collection 
        * this object is used for synchronization of operations that
        * need to be synchronized on this instance of Children, but
        * we cannot synchronize on this instance because it is public
        * and somebody else could synchronize too.
        */
        private Entry nodesEntry;
        /** vector of added children */
        protected Collection nodes;

        /** Constructs a new list and allows a subclass to
        * provide its own implementation of <code>Collection</code> to store
        * data in. The collection should be empty and should not
        * be directly accessed in any way after creation.
        *
        * @param c collection to store data in
        */
        protected Array (Collection c) {
            this ();
            nodes = c;
        }

        /** Constructs a new array children without any assigned collection.
        * The collection will be created by a call to method initCollection the
        * first time, children will be used.
        */
        public Array () {
            nodesEntry = createNodesEntry ();
            this.setEntries (Collections.singleton (getNodesEntry ()));
        }

        /** Clones all nodes that are contained in the children list.
        *
        * @return the cloned array for this children
        */
        public Object clone () {
            try {
                final Children.Array ar = (Array)super.clone ();

                try {
                    PR.enterReadAccess ();
                    if (nodes != null) {
                        // nodes already initilized

                        // used to create the right type of collection
                        // clears the content of the collection
                        // JST: hack, but I have no better idea how to write this
                        //     pls. notice that in initCollection you can test
                        //     whether nodes == null => real initialization
                        //             nodes != null => only create new empty collection
                        ar.nodes = ar.initCollection ();
                        ar.nodes.clear ();


                        // insert copies of the nodes
                        Iterator it = nodes.iterator ();
                        while (it.hasNext ()) {
                            Node n = (Node)it.next ();
                            ar.nodes.add (n.cloneNode ());
                        }
                    }
                } finally {
                    PR.exitReadAccess ();
                }
                return ar;
            } catch (CloneNotSupportedException e) {
                // this cannot happen
                throw new InternalError ();
            }
        }

        /** Allow subclasses to create a collection, the first time the
        * children are used. It is called only if the collection has not
        * been passed in the constructor.
        * <P>
        * The current implementation returns ArrayList.
        *
        * @return empty or initialized collection to use
        */
        protected Collection initCollection () {
            return new ArrayList ();
        }

        /** This method can be called by subclasses that
        * directly modify the nodes collection to update the 
        * state of the nodes appropriatelly.
        * This method should be called under 
        * MUTEX.writeAccess.
        */
        final void refreshImpl () {
            if ( isInitialized() ) {
                Array.this.refreshEntry (getNodesEntry ());
                super.getArray (null).nodes ();
            }
            else if ( nodes != null ) {
                for( Iterator it = nodes.iterator(); it.hasNext(); ) {
                    Node n = (Node)it.next();
                    n.assignTo( this, -1 );
                }
            }
        }

        /** This method can be called by subclasses that
        * directly modify the nodes collection to update the 
        * state of the nodes appropriatelly.
        */
        protected final void refresh () {
            MUTEX.postWriteRequest (new Runnable () {
                                   public void run () {
                                       refreshImpl ();
                                   }
                               });
        }

        /** Getter for the entry.
        */
        final Entry getNodesEntry () {
            return nodesEntry;
        }

        /** This method allows subclasses (only in this package) to
        * provide own version of entry. Usefull for SortedArray.
        */
        Entry createNodesEntry () {
            return new AE ();
        }

        /** Getter for nodes.
        */
        final Collection getCollection () {
            if (nodes == null) {
                synchronized (getNodesEntry ()) {
                    if (nodes == null) {
                        nodes = initCollection ();
                    }
                }
            }
            return nodes;
        }

        /*
        * @param arr nodes to add
        * @return true if changed false if not
        */
        public boolean add (final Node[] arr) {
            synchronized (getNodesEntry ()) {
                if (!getCollection ().addAll (Arrays.asList (arr))) {
                    // no change to the collection
                    return false;
                };
            }
            refresh ();
            return true;
        }

        /*
        * @param arr nodes to remove
        * @return true if changed false if not
        */
        public boolean remove (final Node[] arr) {
            synchronized (getNodesEntry ()) {
                if (!getCollection ().removeAll (Arrays.asList (arr))) {
                    // the collection was not changed
                    return false;
                }
            }
            refresh ();
            return true;
        }

        /** One entry that holds all the nodes in the collection
        * member called nodes.
        */
        private final class AE extends Object implements Entry {
            AE() {}
            /** List of elements.
            */
            public Collection nodes () {
                Collection c = getCollection ();
                if (c.isEmpty ()) {
                    return Collections.EMPTY_LIST;
                } else {
                    return new ArrayList (c);
                }
            }
        }
    }

    /** Implements the storage of node children by a map.
    * This class also permits
    * association of a key with any node and to remove nodes by key.
    * Subclasses should reasonably
    * implement {@link #add} and {@link #remove}.
    */
    public static class Map extends Children {
        /** A map to use to store children in.
        * Keys are <code>Object</code>s, values are {@link Node}s.
        * Do <em>not</em> modify elements in the map! Use it only for read access.
        */
        protected java.util.Map nodes;

        /** Constructs a new list with a supplied map object.
        * Should be used by subclasses desiring an alternate storage method.
        * The map must not be explicitly modified after creation.
        *
        * @param m the map to use for this list
        */
        protected Map (java.util.Map m) {
            nodes = m;
        }

        /** Constructs a new list using {@link HashMap}.
        */
        public Map () {
        }

        /** Getter for the map.
        * Ensures that the map has been initialized.
        */
        final java.util.Map getMap () {
            // package private only to simplify access from inner classes

            if (nodes == null) {
                nodes = initMap ();
            }
            return nodes;
        }

        /** Called on first use.
        */
        final void callAddNotify () {
            this.setEntries (createEntries (getMap ()));

            super.callAddNotify ();
        }

        /** Method that allows subclasses (SortedMap) to redefine
        * order of entries.
        * @param map the map (Object, Node)
        * @return collection of (Entry)
        */
        Collection createEntries (java.util.Map map) {
            LinkedList l = new LinkedList ();
            Iterator it = map.entrySet ().iterator ();
            while (it.hasNext ()) {
                java.util.Map.Entry e = (java.util.Map.Entry)it.next ();
                l.add (new ME (
                           e.getKey (),
                           (Node)e.getValue ()
                       ));
            }
            return l;
        }

        /** Allows subclasses that directly modifies the
        * map with nodes to synchronize the state of the children.
        * This method should be called under 
        * MUTEX.writeAccess.
        */
        final void refreshImpl () {
            this.setEntries (createEntries (getMap ()));
        }

        /** Allows subclasses that directly modifies the
        * map with nodes to synchronize the state of the children.
        */
        protected final void refresh () {
            try {
                PR.enterWriteAccess ();
                refreshImpl ();
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Allows subclasses that directly modifies the
        * map with nodes to synchronize the state of the children.
        * This method should be called under 
        * MUTEX.writeAccess.
        *
        * @param key the key that should be refreshed
        */
        final void refreshKeyImpl (Object key) {
            this.refreshEntry (new ME (key, null));
        }

        /** Allows subclasses that directly modifies the
        * map with nodes to synchronize the state of the children.
        *
        * @param key the key that should be refreshed
        */
        protected final void refreshKey (final Object key) {
            try {
                PR.enterWriteAccess ();
                refreshKeyImpl (key);
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Add a collection of new key/value pairs into the map.
        * The supplied map may contain any keys, but the values must be {@link Node}s.
        *
        * @param map the map with pairs to add
        */
        protected final void putAll (final java.util.Map map) {
            try {
                PR.enterWriteAccess ();
                nodes.putAll (map);
                refreshImpl ();
                // PENDING sometime we should also call refreshKey...
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Add one key and one node to the list.
        * @param key the key
        * @param node the node
        */
        protected final void put (final Object key, final Node node) {
            try {
                PR.enterWriteAccess ();
                if (nodes.put (key, node) != null) {
                    refreshKeyImpl (key);
                } else {
                    refreshImpl ();
                }
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Remove some children from the list by key.
        * @param keys collection of keys to remove
        */
        protected final void removeAll (final Collection keys) {
            try {
                PR.enterWriteAccess ();
                nodes.keySet ().removeAll (keys);
                refreshImpl ();
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Remove a given child node from the list by its key.
        * @param key key to remove
        */
        protected void remove (final Object key) {
            try {
                PR.enterWriteAccess ();
                if (nodes.remove (key) != null) {
                    refreshImpl ();
                }
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Initialize some nodes. Allows a subclass to
        * provide a default map to initialize the map with.
        * Called only if the map has not been provided in the constructor.
        *
        * <P>
        * The default implementation returns <code>new HashMap (7)</code>.
        *
        * @return a map from <code>Object</code>s to {@link Node}s
        */
        protected java.util.Map initMap () {
            return new HashMap (7);
        }

        /** Does nothing. Should be reimplemented in a subclass wishing
        * to support external addition of nodes.
        *
        * @param arr nodes to add
        * @return <code>false</code> in the default implementation
        */
        public boolean add (Node[] arr) {
            return false;
        }

        /** Does nothing. Should be reimplemented in a subclass wishing
        * to support external removal of nodes.
        * @param arr nodes to remove
        * @return <code>false</code> in the default implementation
        */
        public boolean remove (Node[] arr) {
            return false;
        }

        /** Entry mapping one key to the node.
        */
        final static class ME extends Object implements Entry {
            /** key */
            public Object key;
            /** node set */
            public Node node;

            /** Constructor.
            */
            public ME (Object key, Node node) {
                this.key = key;
                this.node = node;
            }

            /** Nodes */
            public Collection nodes () {
                return Collections.singleton (node);
            }

            /** Hash code.
            */
            public int hashCode () {
                return key.hashCode ();
            }

            /** Equals.
            */
            public boolean equals (Object o) {
                if (o instanceof ME) {
                    ME me = (ME)o;
                    return key.equals (me.key);
                }
                return false;
            }

            public String toString () {
                return "Key (" + key + ")"; // NOI18N
            }
        }
    }

    /** Maintains a list of children sorted by the provided comparator in an array.
    * The comparator can change during the lifetime of the children, in which case
    * the children are resorted.
    */
    public static class SortedArray extends Children.Array {
        /** comparator to use */
        private Comparator comp;

        /** Create an empty list of children. */
        public SortedArray() {
        }

        /** Create an empty list with a specified storage method.
        *
        * @param c collection to store data in
        * @see Children.Array#Children.Array(Collection)
        */
        protected SortedArray (Collection c) {
            super(c);
        }

        /** Set the comparator. The children will be resorted.
        * The comparator is used to compare Nodes, if no
        * comparator is used then nodes will be compared by
        * the use of natural ordering.
        *
        * @param c the new comparator
        */
        public void setComparator (final Comparator c) {
            try {
                PR.enterWriteAccess ();
                comp = c;
                refresh ();
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Get the current comparator.
        * @return the comparator
        */
        public Comparator getComparator () {
            return comp;
        }

        /** This method allows subclasses (only in this package) to
        * provide own version of entry. Usefull for SortedArray.
        */
        Entry createNodesEntry () {
            return new SAE ();
        }

        /** One entry that holds all the nodes in the collection
        * member called nodes.
        */
        private final class SAE extends Object implements Entry {
            /** Constructor that provides the original comparator.
            */
            public SAE () {
            }

            /** List of elements.
            */
            public Collection nodes () {
                ArrayList al = new ArrayList (getCollection ());
                Collections.sort (al, comp);
                return al;
            }
        }

    } // end of SortedArray

    /** Maintains a list of children sorted by the provided comparator in a map.
    * Similar to {@link Children.SortedArray}.
    */
    public static class SortedMap extends Children.Map {
        /** comparator to use */
        private Comparator comp;

        /** Create an empty list. */
        public SortedMap () {
        }

        /** Create an empty list with a specific storage method.
        *
        * @param map the map to use with this object
        * @see Children.Map#Children.Map(java.util.Map)
        */
        protected SortedMap (java.util.Map map) {
            super(map);
        }


        /** Set the comparator. The children will be resorted.
        * The comparator is used to compare Nodes, if no
        * comparator is used then values will be compared by
        * the use of natural ordering.
        *
        * @param c the new comparator that should compare nodes
        */
        public void setComparator (final Comparator c) {
            try {
                PR.enterWriteAccess ();
                comp = c;
                refresh ();
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Get the current comparator.
        * @return the comparator
        */
        public Comparator getComparator () {
            return comp;
        }

        /** Method that allows subclasses (SortedMap) to redefine
        * order of entries.
        * @param map the map (Object, Node)
        * @return collection of (Entry)
        */
        Collection createEntries (java.util.Map map) {
            // SME objects use natural ordering
            TreeSet l = new TreeSet (new SMComparator ());

            Iterator it = map.entrySet ().iterator ();
            while (it.hasNext ()) {
                java.util.Map.Entry e = (java.util.Map.Entry)it.next ();
                l.add (new ME (
                           e.getKey (),
                           (Node)e.getValue ()
                       ));
            }

            return l;
        }

        /** Sorted map entry can be used for comparing.
        */
        final class SMComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                ME me1 = (ME)o1;
                ME me2 = (ME)o2;

                Comparator c = comp;
                if (c == null) {
                    // compare keys
                    return ((Comparable)me1.key).compareTo (me2.key);
                } else {
                    return c.compare (me1.node, me2.node);
                }
            }
        }

    } // end of SortedMap

    /** Implements an array of child nodes associated nonuniquely with keys and sorted by these keys.
    * There is a {@link #createNodes(Object) method} that should for each
    * key create an array of nodes that represents the key.
    *
    * <p>Typical usage:
    * <ol>
    * <li>Subclass.
    * <li>Decide what type your key should be.
    * <li>Implement {@link #createNodes} to create some nodes
    * (usually exactly one) per key.
    * <li>Override {@link Children#addNotify} to compute a set of keys
    * and set it using {@link #setKeys(Collection)}.
    * The collection may be ordered.
    * <li>Override {@link Children#removeNotify} to just call
    * <code>setKeys</code> on {@link Collections#EMPTY_SET}.
    * <li>When your model changes, call <code>setKeys</code>
    * with the new set of keys. <code>Children.Keys</code> will
    * be smart and calculate exactly what it needs to do effficiently.
    * <li><i>(Optional)</i> if your notion of what the node for a
    * given key changes (but the key stays the same), you can
    * call {@link #refreshKey}. Usually this is not necessary.
    * </ol>
    */
    public static abstract class Keys extends Children.Array {
        /** add array children before or after keys ones */
        private boolean before;
        /** the last runnable (created in method setKeys) for each children object.
         */
        private static HashMap lastRuns = new HashMap (11);

        /** Special handling for clonning.
        */
        public Object clone () {
            Keys k = (Keys)super.clone ();
            return k;
        }

        /* Adds additional nodes to the children list.
        * Works same like Children.Array. 
        *
        * @param arr nodes to add
        * @return true
        */
        public boolean add (Node[] arr) {
            return super.add (arr);
        }

        /* Removes nodes added by add from the list.
        * @param arr nodes to remove
        * @return if nodes has been removed (they need not necessary be,
        *   because only nodes added by add can be removed, not those
        *   created for key objects)
        */
        public boolean remove (final Node[] arr) {
            try {
                PR.enterWriteAccess ();
                if (nodes != null) {
                    // removing from array, just if the array nodes are 
                    // really created

                    // expecting arr.length == 1, which is the usual case
                    for (int i = 0; i < arr.length; i++) {
                        if (!nodes.contains (arr[i])) {
                            arr[i] = null;
                        }
                    }

                    superRemove (arr);
                }
            } finally {
                PR.exitWriteAccess ();
            }
            
            return true;
        }

        /** Access method to super impl of remove.
        */
        final void superRemove (Node[] arr) {
            super.remove (arr);
        }

        /** Refresh the child nodes for a given key.
        *
        * @param key the key to refresh
        */
        protected final void refreshKey (final Object key) {
            try {
                PR.enterWriteAccess ();
                Keys.this.refreshEntry (new KE (key));
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Set new keys for this children object. Setting of keys
        * does not necessarily lead to the creation of nodes. It happens only
        * when the list has already been initialized.
        *
        * @param keysSet the keys for the nodes (collection of any objects)
        */
        protected final void setKeys (Collection keysSet) {
            final ArrayList l = new ArrayList (keysSet.size () + 1);

            if (before) {
                l.add (getNodesEntry ());
            }
            
            KE updator = new KE ();
            updator.updateList (keysSet, l);
            
            if (!before) {
                l.add (getNodesEntry ());
            }
            
            applyKeys (l);
        }

        /** Set keys for this list.
        *
        * @param keys the keys for the nodes
        * @see #setKeys(Collection)
        */
        protected final void setKeys (final Object[] keys) {
            final ArrayList l = new ArrayList (keys.length + 1);

            KE updator = new KE ();
            
            if (before) {
                l.add (getNodesEntry ());
            }
            
            updator.updateList (keys, l);
            
            if (!before) {
                l.add (getNodesEntry ());
            }
            
            applyKeys (l);
        }
        
        /** Applies the keys.
         */
        private void applyKeys (final ArrayList l) {
            Runnable invoke = new Runnable () {
                public void run () {
                    if (keysCheck (Keys.this, this)) {
                        // no next request after me
                        Keys.this.setEntries (l);
                        // clear this runnable
                        keysExit (Keys.this, this);
                    }
                }
            };
            
            keysEnter (this, invoke);
            MUTEX.postWriteRequest (invoke);
        }


        /** Set whether new nodes should be added to the beginning or end of sublists for a given key.
        *
        * @param b <code>true</code> if the children should be added before
        */
        protected final void setBefore (final boolean b) {
            try {
                PR.enterWriteAccess ();
                if (before != b) {
                    ArrayList l = Keys.this.getEntries ();
                    l.remove (getNodesEntry ());
                    before = b;
                    if (b) {
                        l.add (0, getNodesEntry ());
                    } else {
                        l.add (getNodesEntry ());
                    }
                    Keys.this.setEntries (l);
                }
            } finally {
                PR.exitWriteAccess ();
            }
        }

        /** Create nodes for a given key.
        * @param key the key
        * @return child nodes for this key or null if there should be no 
        *    nodes for this key
        */
        protected abstract Node[] createNodes (Object key);

        /** Called when the nodes have been removed from the children.
        * This method should allow subclasses to clean the nodes somehow.
        * <p>
        * Current implementation notifies all listeners on the nodes
        * that nodes have been deleted.
        *
        * @param arr array of deleted nodes
        */
        protected void destroyNodes (Node[] arr) {
            for (int i = 0; i < arr.length; i++) {
                arr[i].fireNodeDestroyed ();
            }
        }

        /** Notifies the children class that nodes has been released.
        */
        Node[] notifyRemove (Collection nodes, Node[] current) {
            Node[] arr = super.notifyRemove (nodes, current);
            destroyNodes (arr);
            return arr;
        }

        /** Enter of setKeys.
         * @param ch the children
         * @param run runnable 
         */
        private static synchronized void keysEnter (Keys ch, Runnable run) {
            lastRuns.put (ch, run);
        }
        
        /** Clears the entry for the children
         * @param ch children
         */
        private static synchronized void keysExit (Keys ch, Runnable r) {
            Runnable reg = (Runnable) lastRuns.remove (ch);
	    if (reg != null && !reg.equals (r))
		lastRuns.put (ch,reg);
        }
        
        /** Check whether the runnable is "the current" for given children.
         * @param ch children
         * @param run runnable
         * @return true if the runnable shoul run
         */
        private static synchronized boolean keysCheck (Keys ch, Runnable run) {
            return run == lastRuns.get (ch);
        }
            

        /** Entry for a key
        */
        private final class KE extends Dupl implements Entry {
            /** Has default constructor that allows to create a factory
            * of KE objects for use with updateList
            */
            public KE () {
            }

            /** Creates directly an instance of the KE object.
            */
            public KE (Object key) {
                this.key = key;
            }

            /** Nodes are taken from the create nodes.
            */
            public Collection nodes () {
                Node[] arr = createNodes (getKey ());
                if (arr == null) {
                    return Collections.EMPTY_LIST;
                } else {
                    return new LinkedList (Arrays.asList (arr));
                }
            }
        }
    } // end of Keys

    /** Supporting class that provides support for duplicated
    * objects that still should not be equal.
    * <P>
    * It counts the number of times an object has been
    * added to the collection and if the same object is added
    * more than once it is indexed by a number.
    */
    private static class Dupl implements Cloneable {
        /** the key either real value or Dupl (Dupl (Dupl (... value ...)))*/
        protected Object key;
        
        Dupl() {}

        /** Updates the second collection with values from the first one.
        * If there is a multiple occurence of an object in the first collection
        * a Dupl for the object is created to encapsulate it.
        */
        public final void updateList (
            java.util.Collection src, java.util.Collection target
        ) {
            java.util.Map map = new java.util.HashMap (src.size () * 2);

            Iterator it = src.iterator ();
            while (it.hasNext ()) {
                Object o = it.next ();
                updateListAndMap (o, target, map);
            }
        }

        /** Updates the second collection with values from the first array.
        * If there is a multiple occurence of an object in the first array
        * a Dupl for the object is created to encapsulate it.
        */
        public final void updateList (
            Object[] arr, java.util.Collection target
        ) {
            java.util.Map map = new java.util.HashMap (arr.length * 2);

            for (int i = 0; i < arr.length; i++) {
                updateListAndMap (arr[i], target, map);
            }
        }

        /** Updates the linked list and the map with right
        * values. The map is used to count the number of times
        * a element occures in the list.
        *
        * @param obj object to add
        * @param list the list to add obj to
        * @param map to track number of occurences in the array (obj, Integer)
        */
        public final void updateListAndMap (
            Object obj,
            java.util.Collection list,
            java.util.Map map
        ) {
            // optimized for first occurence
            // of each object because often occurences should be rare
            Object prev = map.put (obj, this);
            if (prev == null) {
                // first occurance of object obj
                list.add (createInstance (obj, 0));
                return;
            } else {
                if (prev == this) {
                    // second occurence of the object
                    map.put (obj, new Integer (1));
                    list.add (createInstance (obj, 1));
                    return;
                } else {
                    int cnt = ((Integer)prev).intValue () + 1;
                    map.put (obj, new Integer (cnt));
                    list.add (createInstance (obj, cnt));
                    return;
                }
            }
        }

        /** Gets the key represented by this object.
        * @return the key 
        */
        public final Object getKey () {
            if (key instanceof Dupl) {
                return ((Dupl)key).getKey ();
            } else {
                return key;
            }
        }

        /** Counts the index of this key.
        * @return integer
        */
        public final int getCnt () {
            int cnt = 0;
            Dupl d = this;
            while (d.key instanceof Dupl) {
                d = (Dupl)d.key;
                cnt++;
            }
            return cnt;
        }

        /** Create instance of Dupl (uses cloning of the class)
        */
        private final Dupl createInstance (Object obj, int cnt) {
            try {
                // creates the chain of Dupl (Dupl (Dupl (obj))) where
                // for cnt = 0 the it would look like Dupl (obj)
                // for cnt = 1 like Dupl (Dupl (obj))
                Dupl d = (Dupl)this.clone ();
                Dupl first = d;
                while (cnt-- > 0) {
                    Dupl n = (Dupl)this.clone ();
                    d.key = n;
                    d = n;
                }
                d.key = obj;
                return first;
            } catch (CloneNotSupportedException ex) {
                throw new InternalError ();
            }
        }

        public int hashCode () {
            return getKey ().hashCode ();
        }

        public boolean equals (Object o) {
            if (o instanceof Dupl) {
                Dupl d = (Dupl)o;
                return getKey ().equals (d.getKey ()) && getCnt () == d.getCnt ();
            }
            return false;
        }

        public String toString () {
            String s = getKey ().toString ();
            if (s.length () > 80) {
                s = s.substring (0, 80);
            }
            return "Key (" + s + ", " + getCnt () + ")"; // NOI18N
        }

    }

    /*
      static void printNodes (Node[] n) {
        for (int i = 0; i < n.length; i++) {
          System.out.println ("  " + i + ". " + n[i].getName () + " number: " + System.identityHashCode (n[i]));
        }
        }
        */

    /* JST: Useful test routine ;-) *
    static {
      final TopComponent.Registry r = TopComponent.getRegistry ();
      r.addPropertyChangeListener (new PropertyChangeListener () {
        Node last = new AbstractNode (LEAF);
        
        public void propertyChange (PropertyChangeEvent ev) {
          Node[] arr = r.getCurrentNodes ();
          if (arr != null && arr.length == 1) {
            last = arr[0];
          }
          System.out.println (
            "Activated node: " + last + " \nparent: " + last.getParentNode ()
          );
        }
      });
    }
    */
}
