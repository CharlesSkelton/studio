/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

/** Set which holds its members by using of WeakReferences.
* MT level: unsafe.
*
* @author Ales Novak
*/
public class WeakSet extends AbstractSet implements Cloneable, Serializable {

    /** load factor */
    private float loadFactor;
    /** Number of items. */
    private int size;
    /** Modification count */
    private long modcount;
    /** Reference queue of collected weak refs */
    private transient ReferenceQueue refq;
    /** Count of <tt>null</tt> in this set */
    long nullCount;

    /** An array of Entries */
    private transient Entry[] entries;

    transient Entry iterChain;

    static final long serialVersionUID =3062376055928236721L;
    /** Constructs a new set. */
    public WeakSet() {
        this (101, 0.75f);
    }

    /** Constructs a new set containing the elements in the specified collection.
    * @param c a collection to add
    */
    public WeakSet(Collection c) {
        this ();
        addAll(c);
    }

    /** Constructs a new, empty set;
    * @param initialCapacity initial capacity
    */
    public WeakSet(int initialCapacity) {
        this (initialCapacity, 0.75f);
    }

    /** Constructs a new, empty set;
    *
    * @param initialCapacity initial capacity
    * @param loadFactor load factor
    */
    public WeakSet(int initialCapacity, float loadFactor) {
        if (initialCapacity <= 0 || loadFactor <= 0) {
            throw new IllegalArgumentException();
        }
        size = 0;
        modcount = 0;
        this.loadFactor = loadFactor;
        nullCount = 0;
        refq = new ReferenceQueue();
        entries = new Entry[initialCapacity];
        iterChain = null;
    }

    /** Adds the specified element to this set if it is not already present.
    *
    * @param o an Object to add
    */
    public boolean add(Object o) {
        if (o == null) {
            size++;
            nullCount++;
            modcount++;
            return true;
        }
        Entry e = object2Entry(o);
        if (e != null) {
            return false;
        }
        modcount++;
        size++;
        int hash = hashIt(o);
        Entry next = entries[hash];
        iterChain = entries[hash] = new Entry(o, refq, next, iterChain);
        rehash();
        return true;
    }
    /** Removes all of the elements from this set. */
    public void clear() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = null;
        }
        nullCount = 0;
        modcount++;
        size = 0;
        iterChain = null;
    }
    /** Returns a shallow copy of this WeakSet instance: the elements themselves are not cloned. */
    public Object clone() {
        WeakSet nws = new WeakSet(1, loadFactor);
        nws.size = size;
        nws.nullCount = nullCount;

        Entry[] cloned = new Entry[entries.length];
        nws.entries = cloned;

        for (int i = 0; i < cloned.length; i++) {

            Object ref;
            if ((entries[i] == null) ||
                    ((ref = entries[i].get()) == null)) {
                cloned[i] = null;
            } else {
                cloned[i] = (entries[i] == null ? null : entries[i].clone(nws.refq));
                ref = null;
            }

            // chains into nws iterator chain
            Entry entry = cloned[i];
            while (entry != null) {
                entry.chainIntoIter(nws.iterChain);
                nws.iterChain = entry;
                entry = entry.next;
            }
        }
        return nws;
    }
    /** Returns true if this set contains the specified element.
    *
    * @param o an Object to examine
    */
    public boolean contains(Object o) {
        if (o == null) {
            return nullCount > 0;
        }
        return object2Entry(o) != null;
    }
    /** Returns true if this set contains no elements.
    */
    public boolean isEmpty() {
        return ((nullCount == 0) &&
                (size() == 0));
    }
    /** Returns an iterator over the elements in this set. */
    public Iterator iterator() {
        return new WeakSetIterator();
    }

    class WeakSetIterator implements Iterator {
        Entry current;
        Entry next;
        Object currentObj;
        Object nextObj;
        final long myModcount;
        long myNullCount;

        WeakSetIterator() {
            myModcount = modCount();
            myNullCount = nullCount;
            current = null;
            next = null;
            Entry ee = iterChain;

            if (ee == null) {
                return;
            }

            Object o = ee.get();
            while (ee.isEnqueued()) {
                ee = ee.iterChainNext;
                if (ee == null) {
                    return;
                }
                o = ee.get();
            }

            nextObj = o;
            next = ee;
        }

        public boolean hasNext() {
            checkModcount();
            return (myNullCount > 0 || next != null);
        }

        public Object next() {
            checkModcount();
            checkRefQueue();
            if (myNullCount > 0) {
                myNullCount--;
                return null;
            } else {
                if (next == null) {
                    throw new java.util.NoSuchElementException();
                }
                current = next;
                currentObj = nextObj;

                // move to next requested
                do {
                    next = next.iterChainNext;
                    if (next == null) {
                        break;
                    }
                    nextObj = next.get();
                } while (next.isEnqueued());

                return currentObj;
            }
        }

        public void remove() {
            checkModcount();
            if (current == null) {
                throw new IllegalStateException();
            }
            current.remove();
            size--;
        }

        void checkModcount() {
            if (myModcount != modCount()) {
                throw new java.util.ConcurrentModificationException();
            }
        }
    }


    /** Removes the given element from this set if it is present.
    *
    * @param o an Object to remove
    * @return <tt>true</tt> if and only if the Object was successfuly removed.
    */
    public boolean remove(Object o) {
        if (o == null) {
            if (nullCount > 0) {
                nullCount--;
                modcount++;
                size--;
            }
            return true;
        }
        Entry e = object2Entry(o);
        if (e != null) {
            modcount++;
            size--;
            e.remove();
            rehash();
            return true;
        }
        return false;
    }
    
    /** @return the number of elements in this set (its cardinality). */
    public int size() {
        checkRefQueue ();
        return size;
    }
    
    public Object[] toArray(Object[] array) {
        ArrayList list = new ArrayList(array.length);
        Iterator it = iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        
        return list.toArray(array);
    }
    
    // #14772
    public String toString() {
	StringBuffer buf = new StringBuffer();
	Iterator e = iterator();
	buf.append("[");
        while (e.hasNext()) {
	    buf.append(String.valueOf(e.next()));
	    if (e.hasNext())
		buf.append(", ");
	}
	buf.append("]");
	return buf.toString();
    }

    /** Checks if the queue is empty if not pending weak refs are removed. */
    void checkRefQueue() {
        for (;;) {
            Entry entry = (Entry) refq.poll();
            if (entry == null) {
                break;
            }
            entry.remove();
            size--;
        }
    }

    /** @return modcount */
    long modCount() {
        return modcount;
    }

    /** @return an index to entries array */
    int hashIt(Object o) {
        return (o.hashCode() & 0x7fffffff) % entries.length;
    }

    /** rehashes this Set */
    void rehash() {
        /*
        float currentLF = ((float) size) / ((float) entries.length);
        if (currentLF < loadFactor) {
          return;
    }
        */
    }

    /** @return an Entry with given object */
    private Entry object2Entry(Object o) {

        checkRefQueue(); // clear ref q

        int hash = hashIt(o);
        Entry e = entries[hash];
        if (e == null) {
            return null;
        }
        while ((e != null) && !e.equals(o)) {
            e = e.next;
        }
        return e;
    }

    private void writeObject(ObjectOutputStream obtos) throws IOException {
        obtos.defaultWriteObject();
        obtos.writeObject(toArray());
    }

    private void readObject(ObjectInputStream obtis) throws IOException,
        ClassNotFoundException {
        obtis.defaultReadObject();
        Object[] arr = (Object[]) obtis.readObject();
        entries = new Entry[(int) (size * 1.5)];
        refq = new ReferenceQueue();
        for (int i = 0; i < arr.length; i++) {
            add(arr[i]);
        }
    }

    /** Entries of this set */
    class Entry extends WeakReference {
        // double linked list
        Entry prev;
        Entry next;
        private final int hashcode;

        Entry iterChainNext;
        Entry iterChainPrev;

        Entry(Object referenced, ReferenceQueue q, Entry next, Entry nextInIter) {
            super (referenced, q);
            this.next = next;
            this.prev = null;

            if (next != null) {
                next.prev = this;
            }

            if (referenced != null) {
                hashcode = hashIt(referenced);
            } else {
                hashcode = 0;
            }

            chainIntoIter(nextInIter);
        }

        void chainIntoIter(Entry nextInIter) {
            iterChainNext = nextInIter;
            if (nextInIter != null) {
                nextInIter.iterChainPrev = this;
                Object ref = nextInIter.get();
                if (ref == null) {
                    nextInIter.remove();
                }
            }
        }

        /** deques itself */
        void remove() {
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }

            if (iterChainNext != null) {
                iterChainNext.iterChainPrev = iterChainPrev;
            }
            if (iterChainPrev != null) {
                iterChainPrev.iterChainNext = iterChainNext;
            } else { // root
                iterChain = iterChainNext;
            }
            
            if (entries[hashcode] == this) {
                entries[hashcode] = next;
            }
            
            prev = null;
            next = null;
            iterChainNext = null;
            iterChainPrev = null;
        }

        public int hashCode() {
            return hashcode;
        }

        public boolean equals(Object o) {
            Object oo = get();
            if (oo == null) {
                return false;
            } else {
                return oo.equals(o);
            }
        }

        public Entry clone(ReferenceQueue q) {
            return new Entry(get(), q, (next != null ? (Entry) next.clone(q) : null), null);
        }
    }
}
