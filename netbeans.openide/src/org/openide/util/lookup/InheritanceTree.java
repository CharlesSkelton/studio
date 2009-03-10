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


package org.openide.util.lookup;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

import org.openide.util.Lookup;
import org.openide.util.enums.*;


/** A tree to represent classes with inheritance. Description of the
 * data structure by Petr Nejedly:
 * <P>
 * So pretend I'm Lookup implementation. I've got a bunch of Items (e.g.
 * setPairs() method),
 * didn't do anything on them yet (no startup penalty) so I know nothing
 * about them.
 * Then I'll be asked for all instances implementing given interface or a
 * class. I surely need
 * to check all the Items now, as I don't know anything abou them. I surely
 * don't want to call
 * Item.getClass() as it will dismiss the whole effort. So all I have is
 * Item.instanceOf()
 * and I'll call it on every Item. I'll cache results, so the next time
 * you'll ask me for
 * the same interface/class, I'll answer immediatelly. But what if you ask
 * me for another
 * interface/class? I'll have to scan all Items for it again, unless I can
 * be sure some
 * of them can't implement it. The only source of this knowledge are the
 * previous questions
 * and my rulings on them. Here the algorithm have to be split into two
 * paths. If you
 * previously asked me for interfaces only, I'll have no hint for
 * subsequent queries,
 * but if you asked me for a class in history, and then for another class
 * and these classes
 * are not in inheritance relation (I can check hierarchy of lookup
 * arguments, because
 * they are already resolved/loaded) I can tell that those returned in
 * previous query can't
 * implement the newly asked class (they are in different hierarchy branch)
 * and I need to
 * ask less Items.
 * <P>
 * So if we use mostly classes for asking for services (and it is a trend
 * to use
 * abstract classes for this purpose in IDE anyway), this could be usable.
 * <P>
 * The data structure for separating the Items based on previous queries is
 * simple
 * tree, with every node tagged with one class. The tree's root is,
 * naturally,
 * java.lang.Object, is marked invited and initially contains all the
 * Items.
 * For every class query, the missing part of class hierarchy tree is
 * created,
 * the node of the class looked up is marked as invited and all Items from
 * nearest
 * invited parent (sperclass) are dragged to this node. The result are then
 * all
 * Items from this node and all the nodes deeper in hierarchy. Because it
 * may
 * be too complicated to walk through the children nodes, the results could
 * be
 * cached in the map.
 * For interface lookup, there is a little hint in reality (interfaces
 * and superinterfaces), but it would be harder to exploit it, so we could
 * fall-back
 * to walking through all the Items and cache results.
 * 
 *
 * @author  Jaroslav Tulach
 */
final class InheritanceTree extends Object implements Comparator, Serializable {
    private static final long serialVersionUID = 1L;

    /** the root item (represents Object) */
    private transient Node object;
    /** Map of queried interfaces.
     * <p>Type: <code>Map&lt;Class, (Collection&lt;AbstractLookup.Pair&gt; | AbstractLookup.Pair)&gt;</code>
     */
    private transient Map interfaces;

    /** Constructor
     */
    public InheritanceTree () {
        object = new Node (java.lang.Object.class);
    }

    private void writeObject (ObjectOutputStream oos) throws IOException {
        oos.writeObject(object);
        Iterator it = interfaces.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry)it.next();
            Class c = (Class)e.getKey();
            oos.writeObject(c.getName());
            Object o = e.getValue();
            if (!(o instanceof Collection) && !(o instanceof AbstractLookup.Pair)) throw new ClassCastException(String.valueOf(o));
            oos.writeObject(o);
        }
        oos.writeObject(null);
    }

    private void readObject (ObjectInputStream ois) throws IOException, ClassNotFoundException {
        object = (Node)ois.readObject();
        interfaces = new WeakHashMap();
        String clazz;
        ClassLoader l = (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class);
        while ((clazz = (String)ois.readObject()) != null) {
            Object o = ois.readObject();
            if (!(o instanceof Collection) && !(o instanceof AbstractLookup.Pair)) throw new ClassCastException(String.valueOf(o));
            Class c = Class.forName(clazz, false, l);
            interfaces.put(c, o);
        }
    }
        

    /** Adds an item into the tree.
    * @param item to add
    * @return true if the Item has been added for the first time or false if some other
    *    item equal to this one already existed in the lookup
    */
    public boolean add (AbstractLookup.Pair item, Collection affected) {
        Node node = registerClass (object, item);

        affected.add (node.getType ());
        
        if (node.assignItem (item)) {
            // this is the first item added to n.items
            // ok, we have to test interfaces too
        } else {
            // equal item is already there => stop processing
            return false;
        }

        boolean registeredAsInterface = registerInterface (item, affected);
        return registeredAsInterface;
    }

    /** Removes an item.
    */
    public void remove (AbstractLookup.Pair item, Collection affected) {
        Node n = removeClass (object, item);
        if (n != null) {
            affected.add (n.getType ());
        }
        removeInterface (item, affected);
    }

    /** Removes all items that are not present in the provided collection.
    * @param retain collection of Pairs to keep them in
    * @param notify set of Classes that has possibly changed
    */
    public void retainAll (Map retain, Collection notify) {
        retainAllInterface (retain, notify);
        retainAllClasses(object, retain, notify);
    }


    /** Queries for instances of given class.
    * @param clazz the class to check
    * @return enumeration of Item
    * @see #unsorted
    */
    public Enumeration lookup (Class clazz) {
        if (clazz.isInterface ()) {
            return searchInterface (clazz);
        } else {
            return searchClass (object, clazz);
        }
    }
    
    /** A method to check whether the enumeration returned from
     * lookup method is sorted or is not
     * @param en enumeration to check
     * @return true if it is unsorted and needs to be sorted to find
     *   pair with smallest index
     */
    public static boolean unsorted (Enumeration en) {
        return en instanceof SequenceEnumeration;
    }
    
    /** Prints debug messages.
     * @param out stream to output to
     * @param instances print also instances of the 
     */
    public void print (java.io.PrintStream out, boolean instances) {
        printNode (object, "", out, instances); // NOI18N
    }

    //
    // methods to work on classes which are not interfaces
    //

    /** Searches the subtree and register the item where necessary.
    * @return the node that should contain the item
    */
    private static Node registerClass (Node n, AbstractLookup.Pair item) {
        if (!n.accepts (item)) {
            return null;
        }

        if (n.children != null) {
            Iterator it = n.children.iterator ();
            for (;;) {
                Node ch = extractNode (it);
                if (ch == null) break;
                
                Node result = registerClass (ch, item);
                if (result != null) {
                    // it is in subclass, in case of classes, it cannot
                    // be any other class
                    return result;
                }
            }
        }

        // ok, nobody of our subclasses wants the class, I'll take it
        return n;
    }

    /** Removes the item from the tree of objects.
    * @return most narrow class that this item was removed from
    */
    private static Node removeClass (Node n, AbstractLookup.Pair item) {
        if (!n.accepts (item)) {
            return null;
        }

        if (n.items != null && n.items.remove (item)) {
            // this node really contains the item
            return n;
        }

        if (n.children != null) {
            Iterator it = n.children.iterator ();
            for (;;) {
                Node ch = extractNode (it);
                if (ch == null) break;
                
                Node result = removeClass (ch, item);

                // If the children node was emptied, remove it if possible.
                if( (ch.items == null || ch.items.isEmpty()) 
                    && (ch.children == null || ch.children.isEmpty()) ) {
                        it.remove();
                }
                
                if (result != null) {
                    
                    // it is in subclass, in case of classes, it cannot
                    // be any other class
                    return result;
                }
            }
        }

        // nobody found
        return null;
    }

    /** Finds a node that represents a class.
    * @param n node to search from
    * @param clazz the clazz to find
    * @return node that represents clazz in the tree or null if the clazz is not 
    *    represented under the node n
    */
    private static Node classToNode (Node n, Class clazz) {
        if (!n.accepts (clazz)) {
            // nothing from us
            return null;
        }

        if (n.getType () == clazz) {
            // we have found what we need
            return n;
        }

        if (n.children != null) {
            // have to proceed to children
            Iterator it = n.children.iterator ();
            for (;;) {
                Node ch = extractNode (it);
                if (ch == null) break;
                
                Node found = classToNode (ch, clazz);
                if (found != null) {
                    // class found in one of subnodes
                    return found;
                }
            }
        }
         
        // have to create new subnode and possibly reparent one of my own
        ArrayList reparent = null;
        if (n.children == null) {
            n.children = new ArrayList ();
        } else {
            // scan thru all my nodes if some of them are not a subclass
            // of clazz => then they would need to become child of newNode
            Iterator it = n.children.iterator ();
            for (;;) {
                Node r = extractNode (it);
                if (r == null) break;
                
                if (clazz.isAssignableFrom (r.getType ())) {
                    if (reparent == null) {
                        reparent = new ArrayList ();
                    }
                    reparent.add (r);
                    it.remove ();
                }
            }
        }

        Node newNode = new Node (clazz);
        n.children.add (newNode);

        if (reparent != null) {
            // reassing reparent node as a child of newNode
            newNode.children = reparent;
        }

        // now take all my items that are instances of that class and
        // reasign them
        if (n.items != null) {
            Iterator it = n.items.iterator (); 
            while (it.hasNext ()) {
                AbstractLookup.Pair item = (AbstractLookup.Pair)it.next ();
                if (item.instanceOf (clazz)) {
                    it.remove ();
                    newNode.assignItem (item);
                }
            }
        }

        // newNode represnts my clazz
        return newNode;

    }

    /** Search for a requested class. 
    * @return enumeration of Pair 
    */
    private static Enumeration searchClass (Node n, Class clazz) {
        n = classToNode (n, clazz);
        if (n == null) {
            // not for us
            return EmptyEnumeration.EMPTY;
        } else {
            return nodeToEnum(n);
        }
    }


    /** Retains all classes. Removes nodes which items and children are emptied, works
     * recursivelly from specified root node.
     * @param node root node from which to start to process the tree
     * @param retain a map from (Item, AbstractLookup.Info) that describes which items to retain
     *    and witch integer to assign them
     * @param notify collection of classes will be changed
     * @return <code>true<code> if some items were changed and node items and children are emptied,
     * those nodes, excluding root, will be removed from tree */
    private static boolean retainAllClasses(Node node, Map retain, Collection notify) {
        boolean retained = false;
        
        if(node.items != null && retain != null) {
            Iterator it = node.items.iterator ();
            while (it.hasNext ()) {
                AbstractLookup.Pair item = (AbstractLookup.Pair)it.next ();
                AbstractLookup.Info n = (AbstractLookup.Info)retain.get (item);
                
                if (n == null) {
                    // remove this item, it should not be there
                    it.remove ();
                    retained = true;
                } else {
                    // change the index
                    if (item.index != n.index) {
                        item.index = n.index;
                        notify.addAll (n.modified);
                    }
                }
            }
                
            if (retained && notify != null) {
                // type of this node has been changed
                notify.add(node.getType());
            }
        }

        if(node.children != null) {
            for(Iterator it = node.children.iterator(); ;) {
                Node ch = extractNode(it);
                if(ch == null) {
                    break;
                }
                
                boolean result = retainAllClasses(ch, retain, notify);
                
                if(result) {
                    // The children node was emptied and has no children -> remove it.
                    it.remove();
                }
            }
        }
        
        return retained && node.items.isEmpty() && (node.children == null || node.children.isEmpty());
    }
    
    /** A method that creates enumeration of all items under given node.
     *
     * @param n node to create enumeration for
     * @return enumeration of Pairs
     */
    private static Enumeration nodeToEnum(Node n) {
        if (n.children == null) {
            // create a simple enumeration because we do not have children
            return n.items == null ? EmptyEnumeration.EMPTY : Collections.enumeration (n.items);
        }
        
        // we have found what we need
        // now we have to just build the enumeration
        QueueEnumeration en = new QueueEnumeration () {
            protected void process (Object obj) {
                Node n2 = (Node)obj;
                if (n2.children != null) {
                    Object[] nodes = n2.children.toArray ();
                    put (nodes);
                }
            }
        };
        // initial node is our current one
        en.put (n);

        // convert Node into enumeration of Enumerations of Items
        AlterEnumeration alt = new AlterEnumeration (en) {
            protected Object alter (Object obj) {
                Node n2 = (Node)obj;
                if (n2.items == null || n2.items.isEmpty ()) {
                    return EmptyEnumeration.EMPTY;
                } else {
                    return Collections.enumeration (n2.items);
                }
            }
        };

        // create enumeration of Items
        return new SequenceEnumeration (alt);
    }

    //
    // Methods to work on interfaces
    // 

    /** Registers an item with interfaces.
    * @param item item to register
    * @param affected list of classes that were affected
    * @return false if similar item has already been registered
    */
    private boolean registerInterface (AbstractLookup.Pair item, Collection affected) {
        if (interfaces == null) {
            return true;
        }

        Iterator it = interfaces.entrySet ().iterator ();
        while (it.hasNext ()) {
            Map.Entry entry = (Map.Entry)it.next ();
            Class iface = (Class)entry.getKey ();
            if (item.instanceOf (iface)) {
                Object value = entry.getValue ();
                if (value instanceof Collection) {
                    Collection set = (Collection)value;
                    if (! set.add (item)) {
                        // item is already there, probably (if everything is correct) is registered in 
                        // all other ifaces too, so stop additional testing
                        return false;
                    }
                } else {
                    // there is just one pair right now
                    if (value.equals (item)) {
                        // item is there => stop processing (same as above)
                        return false;
                    }
                    
                    // otherwise replace the single item with ArrayList
                    ArrayList ll = new ArrayList (3);
                    ll.add (value);
                    ll.add (item);
                    entry.setValue (ll);
                }

                affected.add (iface);
            }
        }

        return true;
    }

    /** Removes interface.
    * @param item item to register
    * @param affected list of classes that were affected
    */
    private void removeInterface (AbstractLookup.Pair item, Collection affected) {
        if (interfaces == null) {
            return;
        }

        Iterator it = interfaces.entrySet ().iterator ();
        while (it.hasNext ()) {
            Map.Entry entry = (Map.Entry)it.next ();
            Object value = entry.getValue ();
            if (value instanceof Collection) {
                Collection set = (Collection)value;
                if (set.remove (item)) {
                    if (set.size () == 1) {
                        // if there is just one item remaining change to single item mode
                        entry.setValue (set.iterator().next());
                    }

                    // adds the Class the item was register to into affected
                    affected.add (entry.getKey ());
                }
            } else {
                // single item value
                if (value.equals (item)) {
                    // Emptied -> remove.
                    it.remove();
                    
                    affected.add (entry.getKey ());
                }
            }
        }
    }

    /** Retains some items.
    * @param retain items to retain and their mapping to index numbers
    *    (AbstractLookup.Pair -> AbstractLookup.Info)
    * @param affected list of classes that were affected
    */
    private void retainAllInterface (Map retain, Collection affected) {
        if (interfaces == null) {
            return;
        }

        Iterator it = interfaces.entrySet ().iterator ();
        while (it.hasNext ()) {
            Map.Entry entry = (Map.Entry)it.next ();
            Object value = entry.getValue ();
            
            Iterator elems;
            boolean multi = value instanceof Collection;
            if (multi) {
                // collection mode
                elems = ((Collection)value).iterator ();
                
            } else {
                // single item mode
                elems = Collections.singleton (value).iterator();
            }
            boolean changed = false;
            boolean reordered = false;
            while (elems.hasNext ()) {
                AbstractLookup.Pair p = (AbstractLookup.Pair)elems.next ();
                
                AbstractLookup.Info n = (AbstractLookup.Info)retain.get (p);
                if (n == null) {
                    if (multi) {
                        // remove it
                        elems.remove ();
                    }
                    changed = true;
                } else {
                    if (p.index != n.index) {
                        // improve the index
                        p.index = n.index;
                        affected.addAll (n.modified);
                        reordered = true;
                    }
                }
            }
            
            if (reordered && value instanceof List) {
                // if reordered, than update the order in the collection
                List l = (List)value;
                Collections.sort (l, this);
            }
                
            if (changed) {
                if (multi) {
                    Collection c = (Collection)value;
                    if (c.size () == 1) {
                        // back to single item mode
                        entry.setValue (c.iterator ().next ());
                    }
                } else {
                    // remove in single mode => remove completely
                    it.remove();
                }
                
                // adds the Class the item was register to into affected
                affected.add (entry.getKey ());
            }
        }
    }

    /** Searches for a clazz between interfaces.
    * @param clazz class to search for
    * @return enumeration of Items
    */
    private Enumeration searchInterface (final Class clazz) {
        if (interfaces == null) {
            // first call for interface, only initialize
            interfaces = new WeakHashMap ();
        }

        Object obj = interfaces.get (clazz);
        if (obj == null) {
            // set of items
            AbstractLookup.Pair one = null;
            ArrayList items = null;

            Enumeration en = lookup (Object.class);
            while (en.hasMoreElements ()) {
                AbstractLookup.Pair it = (AbstractLookup.Pair)en.nextElement ();
                if (it.instanceOf (clazz)) {
                    // ok, this item implements given clazz
                    if (one == null) {
                        one = it;
                    } else {
                        if (items == null) {
                            items = new ArrayList (3);
                            items.add (one);
                        }
                        items.add (it);
                    }
                }
            }
            
            if (items == null && one != null) {
                // single item mode
                interfaces.put (clazz, one);
                return new SingletonEnumeration (one);
            } else {
                if (items == null) {
                    items = new ArrayList (2);
                }
                interfaces.put (clazz, items);
                return Collections.enumeration (items);
            }
        } else {
            if (obj instanceof Collection) {
                return Collections.enumeration ((Collection)obj);
            } else {
                // single item mode
                return new SingletonEnumeration (obj);
            }
        }
    }

    /** Extracts a node from an iterator, returning null if no next element found
     */
    private static Node extractNode (Iterator it) {
        while (it.hasNext ()) {
            Node n = (Node)it.next ();
            if (n.get () == null) {
                it.remove ();
            } else {
                return n;
            }
        }
        return null;
    }
    
    /** Prints debug info about the node.
     * @param n node to print
     * @param sp spaces to add
     * @param out where
     * @param instances print also instances
     */
    private static void printNode (Node n, String sp, java.io.PrintStream out, boolean instances) {
        int i;
        Iterator it;

        Class type = n.getType();
        
        out.print (sp); out.println ("Node for: " + type + "\t"+ (type==null ? null : type.getClassLoader() ) ); // NOI18N
        

        if (n.items != null) {
            i = 0; 
            it = new ArrayList (n.items).iterator ();
            while (it.hasNext ()) {
                AbstractLookup.Pair p = (AbstractLookup.Pair)it.next ();
                out.print (sp); out.print ("  item (" + i++ + "): "); out.print (p); // NOI18N
                out.print (" id: " + Integer.toHexString (System.identityHashCode (p))); // NOI18N
                out.print (" index: "); // NOI18N
                out.print (p.index); 
                if (instances) {
                    out.print (" I: " + p.getInstance ());
                }
                out.println ();
            }
        }
        
        if (n.children != null) {
            i = 0; it = n.children.iterator ();
            while (it.hasNext ()) {
                Node ch = (Node)it.next ();
                printNode (ch, sp + "  ", out, instances); // NOI18N
            }
        }
    }

    //
    // Implementation of comparator for AbstractLookup.Pair
    //
    
    
    /** Compares two items.
    */
    public int compare(Object obj, Object obj1) {
        AbstractLookup.Pair i1 = (AbstractLookup.Pair)obj;
        AbstractLookup.Pair i2 = (AbstractLookup.Pair)obj1;

        int result = i1.index - i2.index;
        if (result == 0) {
            if (i1 != i2) {
                java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream ();
                java.io.PrintStream ps = new java.io.PrintStream (bs);

                ps.println (
                    "Please report this exception as issue http://www.netbeans.org/issues/show_bug.cgi?id=13779 " + // NOI18N
                    "Pair1: " + i1 + " pair2: " + i2 + " index1: " + i1.index + " index2: " + i2.index // NOI18N
                    + " item1: " + i1.getInstance () + " item2: " + i2.getInstance () // NOI18N
                    + " id1: " + Integer.toHexString (System.identityHashCode (i1)) // NOI18N
                    + " id2: " + Integer.toHexString (System.identityHashCode (i2)) // NOI18N
                );

                print (ps, false);
                ps.close ();

                throw new IllegalStateException (bs.toString ());
            }
            return 0;
        }

        return result;
    }


    /** Node in the tree.
    */
    static final class Node extends WeakReference implements Serializable {
        static final long serialVersionUID = 3L;
        /** children nodes */
        public ArrayList children; // List<Node>

        /** list of items assigned to this node (suspect to be subclasses) */
        public ArrayList items; // List<AbstractLookup.Pair>

        /** Constructor.
        */
        public Node (Class clazz) {
            super (clazz);
        }
        
        /** Getter for the type associated with this node.
         */
        public Class getType () {
            Class c = (Class)get ();
            // if  garbage collected, then return a garbage
            return c == null ? Void.TYPE : c;
        }
        
        /** Checks whether a node can represent an class.
        */
        public boolean accepts (Class clazz) {
            if (getType () == Object.class) {
                return true;
            }

            return getType ().isAssignableFrom (clazz);
        }
            
        /** Checks whether item is instance of this node.
        */
        public boolean accepts (AbstractLookup.Pair item) {
            if (getType () == Object.class) {
                // Object.class
                return true;
            }
            return item.instanceOf (getType ());
        }

        /** Assings an item to this node.
        * @param item the item
        * @return true if item has been added as new
        */
        public boolean assignItem (AbstractLookup.Pair item) {
            if (items == null) {
                items = new ArrayList ();
                items.add (item);
                return true;
            } 
            if (items.contains(item)) {
                int i = items.indexOf(item);
                AbstractLookup.Pair old = (AbstractLookup.Pair)items.get(i);
                item.index = old.index;
                items.remove(old);
                items.add(item);
                return false;
            }
            items.add (item);
            return true;
        }
        
        private Object writeReplace () {
            return new R (this);
        }
        
    } // End of class Node.
    
    private static final class R implements Serializable {
        static final long serialVersionUID = 1L;
        
        private static ClassLoader l;
        
        private String clazzName;
        private transient Class clazz;
        private ArrayList children;
        private ArrayList items;
        
        public R (Node n) {
            this.clazzName = n.getType ().getName();
            this.children = n.children;
            this.items = n.items;
        }
        
        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            if (l == null) {
                l = (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class);
            }
            clazz = Class.forName(clazzName, false, l);
        }
        
        private Object readResolve () throws ObjectStreamException {
            Node n = new Node (clazz);
            n.children = children;
            n.items = items;
            
            return n;
        }
            
    } // end of R
}
