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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.*;
import java.util.*;

import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.LookupEvent;
import org.openide.util.WeakSet;

/** Implementation of the lookup from OpenAPIs that is based on the
 * introduction of Item. This class should provide the default way
 * of how to store (Class, Object) pairs in the lookups. It offers 
 * protected methods for subclasses to register the pairs.
 * <p>Serializable since 3.27.
 * @author  Jaroslav Tulach
 * @since 1.9
 */
public class AbstractLookup extends Lookup implements Serializable {
    static final long serialVersionUID = 5L;
    
    /** lock for initialization of the map */
    private Content treeLock;
    /** the tree that registers all items */
    private InheritanceTree tree;
    /** true - we are modifying the tree */
    private transient boolean usingTree; // do not serialize as true: #32040
    
    /** Map (Class, WeakSet<Result>) of all listeners that are waiting in 
     * changes in class Class
     */
    private transient Map reg;
    /** count of items in to lookup */
    private int count;
    
    /** Constructor to create this lookup and associate it with given 
     * Content. The content than allows the creator to invoke protected
     * methods which are not accessible for any other user of the lookup.
     *
     * @param content the content to assciate with
     *
     * @since 1.25
     */
    public AbstractLookup (Content content) {
        treeLock = content;
        content.attach (this);
    }
    
    public String toString() {
        if (getClass() == AbstractLookup.class && treeLock.getClass() == InstanceContent.class) {
            return "AbstractLookup" + lookup(new Lookup.Template(Object.class)).allInstances(); // NOI18N
        } else {
            return super.toString();
        }
    }
    
    /** Default constructor for subclasses that do not need to provide a content
     */
    protected AbstractLookup () {
        this (new Content ());
    }
    
    /** Getter for the tree object.
     */
    private InheritanceTree getTree () {
        if (tree != null) {
            return tree;
        }

        synchronized (treeLock) {
            if (tree == null) {
                // ok, I am the thread that is going to initialize the map 
                tree = new InheritanceTree ();
                initialize ();
            }
        }
        
        return tree;
    }
    
    /** Should be called under synchronized(this) before
     * setting usingTree to true. It raises exception if
     * usingTree is already set to true - the check is intended to
     * assure that we are not trying to simultaneously modify the tree
     * while traversing it.
     */
    private void checkForTreeModification() {
        if (usingTree) {
            usingTree = false;
            throw new IllegalStateException("You are trying to modify lookup from lookup query!"); // NOI18N
        }
    }
    
    /** Method for subclasses to initialize them selves.
     */
    protected void initialize () {
        treeLock.initialize ();
    }
    
    /** Notifies subclasses that a query is about to be processed.
     * @param template the template 
     */
    protected void beforeLookup (Template template) {
        treeLock.beforeLookup (template);
    }

    /** The method to add instance to the lookup with.
     * @param pair class/instance pair
     */
    protected final void addPair (Pair pair) {
        HashSet toNotify = new HashSet ();
        
        InheritanceTree t = getTree ();
        
        // manipulation with map must be synchronized on this
        synchronized (this) {
            checkForTreeModification();
            try {
                usingTree = true;

                ArrayList affected = new ArrayList ();
                if (t.add (pair, affected)) {
                    // if the pair is newly added and was not there before
                    collectListenersForList (toNotify, affected);

                    pair.index = count++;
                }
            } finally {
                usingTree = false;
            }
        }
        
        notifyListeners (toNotify);
    }
    
    /** Remove instance.
     * @param pair class/instance pair
     */
    protected final void removePair (Pair pair) {
        HashSet toNotify;
        
        synchronized (this) {
            if (tree == null) {
                // nothing needs to be done
                return;
            }

            toNotify = new HashSet ();


            ArrayList affected = new ArrayList ();
            tree.remove (pair, affected);
            collectListenersForList (toNotify, affected);
        }
        
        notifyListeners (toNotify);
    }
    
    /** Changes all pairs in the lookup to new values.
     * @param collection the collection of (Pair) objects
     */
    protected final void setPairs (Collection collection) {
        HashSet toNotify = new HashSet (27);

        InheritanceTree t = getTree ();
        
        // manipulation with tree must be synchronized on this
        synchronized (this) {
            checkForTreeModification();
            try {
                usingTree = true;

                // map between the Items and their indexes (Integer)
                HashMap shouldBeThere = new HashMap (collection.size () * 2);

                count = 0;

                Iterator it = collection.iterator ();
                ArrayList arr = new ArrayList ();
                while (it.hasNext ()) {
                    Pair item = (Pair)it.next ();
                    if (t.add (item, arr)) {
                        // the item has not been there yet
                        collectListenersForList (toNotify, arr);
                    }

                    // remeber the item, because it should not be removed
                    shouldBeThere.put (item, new Info (count++, arr));

                    arr.clear ();
                }


                ArrayList modified = new ArrayList (27);
                // deletes all objects that should not be there and
                t.retainAll (shouldBeThere, modified);

                // collect listeners
                collectListenersForList (toNotify, modified);

                /*
                // check consistency
                Enumeration en = t.lookup (java.lang.Object.class);
                boolean[] max = new boolean[count];
                int mistake = -1;
                while (en.hasMoreElements ()) {
                    Pair item = (Pair)en.nextElement ();

                    if (max[item.index]) {
                        mistake = item.index;
                    }
                    max[item.index] = true;
                }

                if (mistake != -1) {
                    System.err.println ("Mistake at: " + mistake);
                    tree.print (System.err, true);
                }
                */
            } finally {
                usingTree = false;
            }
        }
        
        notifyListeners (toNotify);
    }
    
    private synchronized final void writeObject(ObjectOutputStream oos) throws IOException {
        // #32040: don't write half-made changes
        oos.defaultWriteObject();
    }
                
    
    /** Lookups an object of given interface. This is the simplest method
     * for the lookuping, if more registered objects implement the given
     * class any of them can be returned. 
     *
     * @param clazz class of the object we are searching for
     * @return the object implementing given class or null if no such 
     *    has been found
     */
    public final Object lookup (Class clazz) {
        Lookup.Item item = lookupItem (new Lookup.Template (clazz));
        return item == null ? null : item.getInstance ();
    }
    
    /** Lookups just one item.
     * @param template a template for what to find
     * @return item or null
     */
    public final Lookup.Item lookupItem (Lookup.Template template) {
        AbstractLookup.this.beforeLookup (template);

        InheritanceTree t = getTree ();

        // manipulation with map must be synchronized
        synchronized (this) {
            checkForTreeModification();
            try {
                usingTree = true;

                Enumeration en = t.lookup (template.getType ());


                int smallest = InheritanceTree.unsorted (en) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                Pair res = null;
                while (en.hasMoreElements ()) {
                    Pair item = (Pair)en.nextElement ();
                    if (matches (template, item)) {
                        if (smallest == Integer.MIN_VALUE) {
                            // ok, sorted enumeration the first that matches is fine
                            return item;
                        } else {
                            // check for the smallest item
                            if (smallest > item.index) {
                                smallest = item.index;
                                res = item;
                            }
                        }
                    }
                }
                return res;
            } finally {
                usingTree = false;
            }
        }
    }
    
    /** The general lookup method.
     * @param template the template describing the services we are looking for
     * @return object containing the results
     */
    public synchronized final Lookup.Result lookup (Lookup.Template template) {
        R result = new R  (template);
        
        if (reg == null) {
            reg = new HashMap (11);
        }
        
        WeakSet ws = (WeakSet)reg.get (template.getType ());
        if (ws == null) {
            ws = new WeakSet(11);
            reg.put (template.getType (), ws);
        }
        ws.add(result);
        
        return result;
    }
    
    /** Collects all listeners that should be interested in change in the set
    * of classes.
    */
    private void collectListenersForList (HashSet allAffectedResults, ArrayList list) {
        if (list.size () == 1) {
            // probably the most common case
            collectListeners (allAffectedResults, (Class)list.get (0));
        } else {
            Iterator it = list.iterator ();
            while (it.hasNext ()) {
                collectListeners (allAffectedResults, (Class)it.next ());
            }
        }
    }

    /** Notifies all listeners that are interested in changes in this class.
     * Should be called from synchronized places.
     * @param allAffectedResults adds Results into this set
     * @param c the class that has changed
     */
    private void collectListeners (HashSet allAffectedResults, Class c) {
        if (reg == null) {
            return;
        }
    
        while (c != null) {
            Set l = (Set)reg.get (c);
            if (l != null && !l.isEmpty ()) {
                Iterator it = l.iterator ();
                while (it.hasNext ()) {
                    R result = (R)it.next();
                    allAffectedResults.add (result);
                }
            }

            c = c.getSuperclass ();
        }
    }   

    /** 
     * Call resultChanged on all listeners.
     * @param listeners array of listeners in the format used by
     *        javax.swing.EventListenerList. It means that there are Class
     *        objects on even positions and the listeners on odd positions
     * @param ev the event to fire
     */
    static void notifyListeners(final Object []listeners, final LookupEvent ev) {
        for (int i = listeners.length - 1; i >= 0; i -= 2) {
            LookupListener ll = (LookupListener)listeners[i];
            try {
                ll.resultChanged(ev);
            } catch (RuntimeException e) {
                // Such as e.g. occurred in #32040. Do not halt other things.
                e.printStackTrace();
            }
        }
    }
    
    /** Notify change to all Results in the set.
     */
    private static void notifyListeners (HashSet allAffectedResults) {
        if (allAffectedResults.isEmpty ()) {
            return;
        }
        
        Iterator it = allAffectedResults.iterator ();
        while (it.hasNext ()) {
            R result = (R)it.next ();
            result.fireStateChanged ();
        }
    }
    
        
    /** A method that defines matching between Item and Template.
     * @param item the item to match
     * @return true if item matches the template requirements, false if not
     */
    static boolean matches (Template t, Pair item) {
        String id = t.getId ();
        if (id != null && !item.getId ().equals (id)) {
            return false;
        }

        Object instance = t.getInstance ();
        if (instance != null && !item.creatorOf (instance)) {
            return false;
        }

        return true;
    }

    /**
     * Compares the array elements for equality.
     * @return true if all elements in the arrays are equal 
     *  (by calling equals(Object x) method)
     */ 
    private static boolean compareArrays(Object[]a, Object []b) {
        // handle null values
        if (a == null) {
            return (b == null);
        } else {
            if (b == null) {
                return false;
            }
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            // handle null values for individual elements
            if (a[i] == null) {
                if (b[i] != null) {
                    return false;
                }
                // both are null --> ok, take next
                continue;
            } else {
                if (b[i] == null) {
                    return false;
                }
            }
            // perform the comparison
            if (! a[i].equals(b[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    /** Extension to the default lookup item that offers additional information
     * for the data structures use in AbstractLookup
     */
    public static abstract class Pair extends Lookup.Item implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** possition of this item in the lookup, manipulated in addPair, removePair, setPairs methods */
        int index = -1;

        /** For use by subclasses. */
        protected Pair () {}

        /** Tests whether this item can produce object
        * of class c.
        */
        protected abstract boolean instanceOf (Class c);

        /** Method that can test whether an instance of a class has been created
         * by this item.
         *
         * @param obj the instance
         * @return if the item has already create an instance and it is the same
         *   as obj.
         */
        protected abstract boolean creatorOf (Object obj);
    }

    
    /** Result based on one instance returned.
     */
    private final class R extends WaitableResult {
        private Template template;
        
        /** temporary caches */
        private Set classesCache;
        private Collection instancesCache;
        private Collection itemsCache;
        
        /** listeners on the results */
        private ArrayList listeners;
        
        R (Template template) {
            this.template = template;
        }
        
        /** Delete all cached values, the template changed.
         */
        public void fireStateChanged () {
            Collection previousItems = itemsCache;

            classesCache = null;
            instancesCache = null;
            itemsCache = null;

            if (previousItems != null) {
                Object[] previousArray = previousItems.toArray ();
                Object[] newArray = allItems ().toArray ();

                if (compareArrays (
                    previousArray, newArray
                )) {
                    // do not fire any change if nothing has been changed
                    return;
                }
            }
            
            LookupListener[] arr;
            synchronized (this) {
                if (listeners == null) return;
                
                arr = (LookupListener[])listeners.toArray (
                    new LookupListener[listeners.size ()]
                );
            }

            final LookupListener[] ll = arr;
            final LookupEvent ev = new LookupEvent (this);
            for (int i = 0; i < ll.length; i++) {
                ll[i].resultChanged(ev);
            }
        }
        
        /** Ok, register listeners to all classes and super classes.
         */
        public synchronized void addLookupListener (LookupListener l) {
            if (listeners == null) {
                listeners = new ArrayList ();
            }
            listeners.add (l);
        }    
        
        /** Ok, register listeners to all classes and super classes.
         */
        public synchronized void removeLookupListener (LookupListener l) {
            listeners.remove (l);
        }    
        

        public Collection allInstances () {
            Collection s = instancesCache;
            if (s != null) {
                return s;
            }
            
            s = new ArrayList (allItems ().size ());
            Iterator it = allItems ().iterator ();
            while (it.hasNext ()) {
                Item item = (Item)it.next ();
                Object obj = item.getInstance ();
                if (obj != null) {
                    s.add (obj);
                }
            }
            instancesCache = s;
            
            return s;
        }
        
        /** Set of all classes.
         *
         */
        public Set allClasses () {
            Set s = classesCache;
            if (s != null) {
                return s;
            }
            
            s = new HashSet ();
            Iterator it = allItems ().iterator ();
            while (it.hasNext ()) {
                Item item = (Item)it.next ();
                Class clazz = item.getType ();
                if (clazz != null) {
                    s.add (clazz);
                }
            }
            classesCache = s;
            
            return s;
        }
        
        /** Items are stored directly in the allItems.
         */
        public Collection allItems () {
            AbstractLookup.this.beforeLookup (template);
            
            if (itemsCache != null) {
                return itemsCache;
            }

            InheritanceTree t = getTree ();

            synchronized (AbstractLookup.this) {
                checkForTreeModification();
                try {
                    usingTree = true;

                    // manipulation with the tree must be synchronized
                    Enumeration en = t.lookup (template.getType ());

                    // InheritanceTree is comparator for AbstractLookup.Pairs
                    TreeSet items = new TreeSet (t);
                    while (en.hasMoreElements ()) {
                        Pair i = (Pair)en.nextElement ();
                        if (matches (template, i)) {
                            items.add (i);
                        }
                    }

                    // create a correctly sorted copy using the tree as the comparator
                    itemsCache = Collections.unmodifiableList(new ArrayList(items));
                    return itemsCache;
                } finally {
                    usingTree = false;
                }
            }
        }

        /** Used by proxy results to synchronize before lookup.
         */
        protected void beforeLookup(Lookup.Template t) {
            if (t.getType () == template.getType ()) {
                AbstractLookup.this.beforeLookup (t);
            }
        }        
        
        /* Do not need to implement it, the default way is ok.
        public boolean equals(java.lang.Object obj) {
            return obj == this;
        }
        */

    }
    
    /** A class that can be used by the creator of the AbstractLookup to
     * control its content. It can be passed to AbstractLookup constructor
     * and used to add and remove pairs.
     *
     * @since 1.25
     */
    public static class Content extends Object implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        // one of them is always null (except attach stage)
        /** abstract lookup we are connected to */
        private AbstractLookup al = null;
        private transient ArrayList earlyPairs = new ArrayList(3);
        
        /** A lookup attaches to this object.
         */
        final synchronized void attach (AbstractLookup al) {
            if (this.al == null) {
                this.al = al;
                // we must just add no override!
                Pair[] p = (Pair[]) earlyPairs.toArray(new Pair[earlyPairs.size()]);
                for (int i = 0; i<p.length; i++) {
                    addPair(p[i]);
                }
                earlyPairs = null;
            } else {
                throw new IllegalStateException ("Trying to use content for " + al + " but it is already used for " + this.al); // NOI18N
            }
        }
        
        /** The method to add instance to the lookup with.
         * @param pair class/instance pair
         */
        public final void addPair (Pair pair) {
            AbstractLookup a = al;
            if (a != null) {
                a.addPair (pair);
            } else {
                earlyPairs.add(pair);
            }
        }
        
        /** Remove instance.
         * @param pair class/instance pair
         */
        public final void removePair (Pair pair) {
            AbstractLookup a = al;
            if (a != null) {
                a.removePair (pair);
            } else {
                earlyPairs.remove(pair);
            }
        }
        
        /** Changes all pairs in the lookup to new values.
         * @param c the collection of (Pair) objects
         */
        public final void setPairs (Collection c) {
            AbstractLookup a = al;
            if (a != null) {
                a.setPairs (c);
            } else {
                earlyPairs.clear();
                earlyPairs.addAll(c);
            }
        }
        
        // make protected if needed...
        
        /** Called when the lookup if first used.
         */
        /*protected*/ void initialize () {
        }
        
        /** Notifies subclasses that a query is about to be processed.
         * @param template the template 
         */
        /*protected*/ void beforeLookup (Template template) {
        }
        
    } // end of R (result)

    /** Just a holder for index & modified values.
     */
    final static class Info extends Object {
        public int index;
        public ArrayList modified;

        public Info (int i, ArrayList m) {
            index = i;
            modified = (ArrayList)m.clone ();
        }
    }
}
