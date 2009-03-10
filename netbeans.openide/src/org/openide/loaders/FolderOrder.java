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

package org.openide.loaders;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import java.io.IOException;
import java.util.*;

import org.openide.filesystems.*;
import org.openide.util.enums.*;
import org.openide.loaders.DataFolder.SortMode;


/** A support for keeping order of children for folder list.
 *
 * @author  Jaroslav Tulach
 */
final class FolderOrder extends Object implements Comparator {
    /** Separator of names of two files. The first file should be before
     * the second one in partial ordering
     */
    private static final char SEP = '/';
    
    /** a static map with (FileObject, Reference (Folder))
     */
    private static final WeakHashMap map = new WeakHashMap (101);
    /** A static of known folder orders. Though we hold the
     * FolderOrder with a soft reference which can be collected, even
     * if this happens we would like the new FolderOrder to have any
     * previously determined order attribute. Otherwise under obscure
     * circumstances (#15381) it is possible for the IDE to go into an
     * endless loop recalculating folder orders, since they keep
     * getting collected.
     */
    private static final Map knownOrders = Collections.synchronizedMap(new WeakHashMap(50)); // Map<FileObject, Object>
    

    /** map of names of primary files of objects to their index or null */
    private Map order; // Map<String,Integer>
    /** file to store data in */
    private FileObject folder;
    /** if true, partial orderings on disk should be ignored for files in the order */
    private boolean ignorePartials;
    /** a reference to sort mode of this folder order */
    private SortMode sortMode;
    /** previous value of the order */
    private Object previous;

    /** Constructor.
    * @param folder the folder to create order for
    */
    private FolderOrder (FileObject folder) {
        this.folder = folder;
    }
    
    
    /** Changes a sort order for this order
     * @param mode sort mode.
     */
    public void setSortMode (SortMode mode) throws IOException {
        // store the mode to properties
        sortMode = mode;
        mode.write (folder); // writes attribute EA_SORT_MODE -> updates FolderList
        
        // FolderList.changedFolderOrder (folder);
    }
    
    /** Getter for the sort order.
     */
    public SortMode getSortMode () {
        if (sortMode == null) {
            sortMode = SortMode.read (folder);
        }
        return sortMode;
    }
    
    /** Changes the order of data objects.
     */
    public synchronized void setOrder (DataObject[] arr) throws IOException {
        if (arr != null) {
            order = new HashMap (arr.length * 3 / 4);

            // each object only once
            Enumeration en = new RemoveDuplicatesEnumeration (new ArrayEnumeration (arr));

            int i = 0;
            while (en.hasMoreElements ()) {
                DataObject obj = (DataObject)en.nextElement ();
                FileObject fo = obj.getPrimaryFile ();
                if (folder.equals (fo.getParent ())) {
                    // object for my folder
                    order.put (fo.getNameExt (), new Integer (i++));
                }
            }
            // Explicit order has been set, if written please clear affected
            // order markings.
            ignorePartials = true;
        } else {
            order = null;
        }
        
        write (); // writes attribute EA_ORDER -> updates FolderList
        
        
        // FolderList.changedFolderOrder (folder);
    }

    /**
     * Get ordering constraints for this folder.
     * Returns a map from data objects to lists of data objects they should precede.
     * @param objects a collection of data objects known to be in the folder
     * @return a constraint map, or null if there are no constraints
     */
    public synchronized Map getOrderingConstraints(Collection objects) {
        final Set partials = readPartials ();
        if (partials.isEmpty ()) {
            return null;
        } else {
            Map objectsByName = new HashMap();
            Iterator it = objects.iterator();
            while (it.hasNext()) {
                DataObject d = (DataObject)it.next();
                objectsByName.put(d.getPrimaryFile().getNameExt(), d);
            }
            Map m = new HashMap();
            it = partials.iterator();
            while (it.hasNext()) {
                String constraint = (String)it.next();
                int idx = constraint.indexOf(SEP);
                String a = constraint.substring(0, idx);
                String b = constraint.substring(idx + 1);
                if (ignorePartials && (order.containsKey(a) || order.containsKey(b))) {
                    continue;
                }
                DataObject ad = (DataObject)objectsByName.get(a);
                if (ad == null) {
                    continue;
                }
                DataObject bd = (DataObject)objectsByName.get(b);
                if (bd == null) {
                    continue;
                }
                List l = (List)m.get(ad);
                if (l == null) {
                    m.put(ad, l = new LinkedList());
                }
                l.add(bd);
            }
            return m;
        }
    }

    /** Read the list of intended partial orders from disk.
     * Each element is a string of the form "a<b" for a, b filenames
     * with extension, where a should come before b.
     */
    private Set readPartials () { // Set<String>
        Enumeration e = folder.getAttributes ();
        Set s = new HashSet ();
        while (e.hasMoreElements ()) {
            String name = (String) e.nextElement ();
            if (name.indexOf (SEP) != -1) {
                Object value = folder.getAttribute (name);
                if ((value instanceof Boolean) && ((Boolean) value).booleanValue ())
                    s.add (name);
            }
        }
        return s;
    }

    /** Compares two data object or two nodes.
    */
    public int compare (Object o1, Object o2) {
        DataObject obj1 = (DataObject) o1;
        DataObject obj2 = (DataObject) o2;
        
        Integer i1 = (order == null) ? null : (Integer)order.get (obj1.getPrimaryFile ().getNameExt ());
        Integer i2 = (order == null) ? null : (Integer)order.get (obj2.getPrimaryFile ().getNameExt ());

        if (i1 == null) {
            if (i2 != null) return 1;

            // compare by the provided comparator
            return getSortMode ().compare (obj1, obj2);
        } else {
            if (i2 == null) return -1;
            // compare integers
            if (i1.intValue () == i2.intValue ()) return 0;
            if (i1.intValue () < i2.intValue ()) return -1;
            return 1;
        }
    }

    /** Stores the order to files.
    */
    public void write () throws IOException {
        // Let it throw the IOException:
        //if (folder.getFileSystem ().isReadOnly ()) return; // cannot write to read-only FS
        if (order == null) {
            // if we should clear the order
            folder.setAttribute (DataFolder.EA_ORDER, null);
        } else {
            // Stores list of file names separated by /
            java.util.Iterator it = order.entrySet ().iterator ();
            String[] filenames = new String[order.size ()];
            while (it.hasNext ()) {
                Map.Entry en = (Map.Entry)it.next ();
                String fo = (String)en.getKey ();
                int indx = ((Integer)en.getValue ()).intValue ();
                filenames[indx] = fo;
            }
            StringBuffer buf = new StringBuffer (255);
            for (int i = 0; i < filenames.length; i++) {
                if (i > 0) {
                    buf.append ('/');
                }
                buf.append (filenames[i]);
            }
            folder.setAttribute (DataFolder.EA_ORDER, buf.toString ());

            if (ignorePartials) {
                // Reverse any existing partial orders among files explicitly
                // mentioned in the order.
                Set p = readPartials ();
                if (! p.isEmpty ()) {
                    Set f = new HashSet (); // Set<String> for filenames
                    it = order.keySet ().iterator ();
                    while (it.hasNext ()) {
                        String fo = (String) it.next ();
                        f.add (fo);
                    }
                    it = p.iterator ();
                    while (it.hasNext ()) {
                        String s = (String) it.next ();
                        int idx = s.indexOf (SEP);
                        if (f.contains (s.substring (0, idx)) &&
                            f.contains (s.substring (idx + 1))) {
                            folder.setAttribute (s, null);
                        }
                    }
                }
                // Need not do this again for this order:
                ignorePartials = false;
            }
        }
    }
    
    /** Reads the order from disk.
     */
    private void read () {
        Object o = folder.getAttribute (DataFolder.EA_ORDER);
        
        if ((previous == null && o == null) ||
            (previous != null && previous.equals (o))) {
            // no change in order
            return;
        }
        
        if ((o instanceof Object[]) && (previous instanceof Object[])) {
            if (compare((Object[]) o, (Object[]) previous)) {
                return;
            }
        }
        
        doRead (o);
        
        previous = o;
        if (previous != null) {
            knownOrders.put(folder, previous);
        }
        
        FolderList.changedFolderOrder (folder);
    }

    /** Compares two arrays */
    private static boolean compare(Object[] a, Object[] b) {
        if (a == b) {
            return true;
        }
        
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            if (a[i] != b[i]) {
                if (a[i] == null) {
                    return false;
                }
                
                if (a[i].equals(b[i])) {
                    continue;
                }
                
                if ((a[i] instanceof Object[]) && (b[i] instanceof Object[])) {
                    if (compare((Object[]) a[i], (Object[]) b[i])) {
                        continue;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        
        Object[] arr = (a.length > b.length) ? a : b;
        if (checkNonNull(arr, len)) {
            return false;
        }
        
        return true;
    }
    
    private static boolean checkNonNull(Object[] a, int from) {
        for (int i = from; i < a.length; i++) {
            if (a[i] != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /** Reads the values from the object o
     * @param o value of attribute EA_ORDER
     */
    private void doRead (Object o) {
        if (o == null) {
            order = null;
            return;
        } else if (o instanceof String[][]) {
            // Compatibility:
            String[][] namesExts = (String[][]) o;

            if (namesExts.length != 2) {
                order = null;
                return;
            }
            String[] names = namesExts[0];
            String[] exts = namesExts[1];

            if (names == null || exts == null || names.length != exts.length) {
                // empty order
                order = null;
                return;
            }


            HashMap set = new HashMap (names.length);

            for (int i = 0; i < names.length; i++) {
                set.put (names[i], new Integer (i));
            }
            order = set;
            return;
            
        } else if (o instanceof String) {
            // Current format:
            String sepnames = (String) o;
            HashMap set = new HashMap ();
            StringTokenizer tok = new StringTokenizer (sepnames, "/"); // NOI18N
            int i = 0;
            while (tok.hasMoreTokens ()) {
                String file = tok.nextToken ();
                set.put (file, new Integer (i));
                i++;
            }
            
            order = set;
            return;
        } else {
            // Unknown format:
            order = null;
            return;
        }
    }
    

    /** Creates order for given folder object.
    * @param f the folder
    * @return the order
    */
    public static FolderOrder findFor (FileObject folder) {
        FolderOrder order = null;
        synchronized (map) {
            Reference ref = (Reference)map.get (folder);
            order = ref == null ? null : (FolderOrder)ref.get ();
            if (order == null) {
                order = new FolderOrder (folder);
                order.previous = knownOrders.get(folder);
                order.doRead(order.previous);
                
                map.put (folder, new SoftReference (order));
            }
        }
        // always reread the order from disk, so it is uptodate
        synchronized (order) {
            order.read ();
            return order;            
        }        
    }
        
     
    
}
