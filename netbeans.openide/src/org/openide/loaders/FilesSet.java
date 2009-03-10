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

package org.openide.loaders;

import java.util.*;

import org.openide.filesystems.FileObject;

/** This class is a lazy initialized set of FileObjects representing entries
 * in MultiDataObject class. Primary file object is returned as the first from 
 * this set, the secondary fileobjects are sorted by <code>getNameExt()</code>
 * method result alphabetically.
 * <p>
 * This class is an implementation of performance enhancement #16396.
 *
 * @see <a href="http://www.netbeans.org/issues/show_bug.cgi?id=16396">Issue #16396</a>
 *
 * @author  Petr Hamernik
 */
final class FilesSet implements Set {
    /** Primary file. It is returned first. */
    private Object primaryFile;
    
    /** The link to secondary variable of MultiDataObject. Reading of the content
     * must be synchronized on this map.
     */
    private HashMap secondary;

    /** The set containing all files. It is <code>null</code> and is lazy initialized
     * when necessary.
     */
    private TreeSet delegate;
    
    /** Creates a new instance of FilesSet for the MultiDataObject.
     * @param primaryFile The primary file - this object is returned first.
     * @param secondary the map of secondary file objects. It is used 
     *     for initialization <code>delegate</code> variable when necessary.
     */
    public FilesSet(Object primaryFile, HashMap secondary) {
        this.primaryFile = primaryFile;
        this.secondary = secondary;
    }

    /** Perform lazy initialization of delegate TreeSet.
     */
    private Set getDelegate() {
        // This synchronized block was moved from MultiDataObject.files() method,
        // because of lazy initialization of delegate TreeSet.
        // Hopefully won't cause threading problems.
        synchronized (secondary) {
            if (delegate == null) {
                delegate = new TreeSet(new FilesComparator());
                delegate.add(primaryFile);
                delegate.addAll(secondary.keySet());
            }
        }
        return delegate;
    }

    // =====================================================================
    //   Implementation of Set interface methods
    // =====================================================================
    
    public boolean add(Object obj) {
        return getDelegate().add(obj);
    }
    
    public boolean addAll(java.util.Collection collection) {
        return getDelegate().addAll(collection);
    }
    
    public void clear() {
        getDelegate().clear();
    }
    
    public boolean contains(Object obj) {
        return getDelegate().contains(obj);
    }
    
    public boolean containsAll(java.util.Collection collection) {
        return getDelegate().containsAll(collection);
    }
    
    public boolean isEmpty() {
        synchronized (secondary) {
            return (delegate == null) ? false : delegate.isEmpty();
        }
    }
    
    public java.util.Iterator iterator() {
        synchronized (secondary) {
            return (delegate == null) ? new FilesIterator() : delegate.iterator();
        }
    }
    
    public boolean remove(Object obj) {
        return getDelegate().remove(obj);
    }
    
    public boolean removeAll(java.util.Collection collection) {
        return getDelegate().removeAll(collection);
    }
    
    public boolean retainAll(java.util.Collection collection) {
        return getDelegate().retainAll(collection);
    }
    
    public int size() {
        synchronized (secondary) {
            return (delegate == null) ? (secondary.size() + 1) : delegate.size();
        }
    }
    
    public Object[] toArray() {
        return getDelegate().toArray();
    }
    
    public Object[] toArray(Object[] obj) {
        return getDelegate().toArray(obj);
    }

    /** Iterator for FilesSet. It returns the primaryFile first and 
     * then initialize the delegate iterator for secondary files.
     */
    private final class FilesIterator implements Iterator {
        /** Was the first element (primary file) already returned?
         */
        private boolean first = true;
        
        /** Delegation iterator for secondary files. It is lazy initialized after
         * the first element is returned.
         */
        private Iterator itDelegate = null;
        
        FilesIterator() {}
        
        public boolean hasNext() {
            return first ? true : getIteratorDelegate().hasNext();
        }
        
        public Object next() {
            if (first) {
                first = false;
                return FilesSet.this.primaryFile;
            }
            else {
                return getIteratorDelegate().next();
            }
        }

        public void remove() {
            getIteratorDelegate().remove();
        }

        /** Initialize the delegation iterator.
         */
        private Iterator getIteratorDelegate() {
            if (itDelegate == null) {
                // this should return iterator of all files of the MultiDataObject...
                itDelegate = FilesSet.this.getDelegate().iterator();
                // ..., so it is necessary to skip the primary file
                itDelegate.next();
            }
            return itDelegate;
        }
    }
    
    /** Comparator for file objects. The primary file is less than any other file,
     * so it is returned first. Other files are compared by getNameExt() method
     * result.
     */
    private final class FilesComparator implements Comparator {
        FilesComparator() {}
        public int compare(Object obj1, Object obj2) {
            if (obj1 == obj2)
                return 0;
            
            if (obj1 == primaryFile)
                return -1;
            
            if (obj2 == primaryFile)
                return 1;
            
            FileObject f1 = (FileObject) obj1;
            FileObject f2 = (FileObject) obj2;
            int res = f1.getNameExt().compareTo(f2.getNameExt());
            
            if (res == 0) {
                // check whether they both live on the same fs
                try {
                    if (f1.getFileSystem() == f2.getFileSystem()) {
                        return 0;
                    }
                    // different fs --> compare the fs names
                    return f1.getFileSystem().getSystemName().compareTo(
                        f2.getFileSystem().getSystemName()); 
                } catch (org.openide.filesystems.FileStateInvalidException fsie) {
                    // should not happen - but the names were the same
                    // so we declare they are the same (even if the filesystems
                    // crashed meanwhile)
                    return 0;
                }
            }
            
            return res;
        }
    }
}
