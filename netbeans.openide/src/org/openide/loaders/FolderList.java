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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import java.io.IOException;
import java.util.*;

import org.openide.ErrorManager;
import org.openide.util.datatransfer.*;
import org.openide.filesystems.*;
import org.openide.util.*;

/** Watches a folder and its children.
 *
 * <p>{@link java.beans.PropertyChangeListener}s
 * may be registered which will be informed about changes in the ordered
 * children list. The {@link java.beans.PropertyChangeEvent}s fired by instances
 * of this class do neither contain information about the old value nor about
 * the new value of the children list.</p>
 *
 * <p>The list of children can be retrieved by calls to
 * the methods {@link #getChildren()} resp. {@link #getChildrenList()}. If you
 * want to filter the children which shall be included into the folder list,
 * call {@link #computeChildrenList(FolderListListener)}. The same is true
 * if you want to trigger children computation asynchronously. In this case
 * the implementation of {@link FolderListListener#finished(List)} shall be 
 * used to get informed about the result of the computation.</p>
 *
 * <p>To retrieve the appropriate instance of this class for a given folder
 *   call {@link #find(FileObject, boolean)}.</p>
*
* @author Jaroslav Tulach
*/
final class FolderList extends Object 
implements FileChangeListener, DataObject.Container {
    
    /* -------------------------------------------------------------------- */
    /* -- Constants ------------------------------------------------------- */
    /* -------------------------------------------------------------------- */
    
    /** serial version UID */
    static final long serialVersionUID = -592616022226761148L;

    /** priority for tasks that can be run later */
    private static final int LATER_PRIORITY = Thread.NORM_PRIORITY;

    /** request processor for recognizing of folders */
    private static final RequestProcessor PROCESSOR = new RequestProcessor (
                "Folder recognizer" // NOI18N
            );
    
    /** map of (FileObject, Reference (FolderList)) */
    private static final Map map = new WeakHashMap (101);
    
    /** refresh time in milliseconds */
    private static int REFRESH_TIME = -1; // will be updated in getRefreshTime

    /* -------------------------------------------------------------------- */
    /* -- Instance attributes --------------------------------------------- */
    /* -------------------------------------------------------------------- */

    /** data folder to work with */
    protected FileObject folder;

    /** The task that computes the content of FolderList. There is also
    * only one computation task in the PROCESSOR for each FolderList.
    * Whenever a new change notification arrives (thru file listener)
    * the previous task is canceled (if not running) and new is created.
    */
    transient private RequestProcessor.Task refreshTask;

    /** Primary files in this folder. Maps (FileObject, Reference (DataObject))
    */
    transient private HashMap primaryFiles = null;

    /** order of primary files (FileObject) */
    transient private List order;

    private static final ErrorManager err = ErrorManager.getDefault().getInstance("org.openide.loaders.FolderList"); // NOI18N
    
    /** property change support */
    transient private PropertyChangeSupport pcs;
    
    /**
     * If true, this folder has been fully created (though it might
     * still be refreshing etc.). Used to avoid e.g. MDO.PROP_FILES
     * firing before the folder is ready.
     */
    transient private boolean folderCreated = false;
    
    /* -------------------------------------------------------------------- */
    /* -- Constructor (private) ------------------------------------------- */
    /* -------------------------------------------------------------------- */

    /**
    * @param df data folder to show
    */
    private FolderList (FileObject folder, boolean attach) {
        this.folder = folder;
        if (attach) {
            // creates object that handles all elements in array and
            // assignes it to the
            folder.addFileChangeListener (WeakListener.fileChange (this, folder));
        }
    }

/*    final void reassign(DataFolder df, FileObject fo) {
        folder = df;
        // reassign is called from DataFolder.handleMove()
        // in this time the folder - df - does not have
        // setup the right primary file
        // so the fo is the new primary file for df
        fo.addFileChangeListener (WeakListener.fileChange (this, fo));
    }
 */
    
    /* -------------------------------------------------------------------- */
    /* -- Factory method (static) ----------------------------------------- */
    /* -------------------------------------------------------------------- */
    
    /** A public method to get the correct list for given file object.
     *
     * @param folder the folder to find FolderList for
     * @param create if true than new FolderList should be created if it does not exists
     * @return the FolderList or null if create was false
     */
    public static FolderList find (FileObject folder, boolean create) {
        FolderList list = null;
        synchronized (FolderList.class) {
            Reference ref = (Reference)map.get (folder);
            list = ref == null ? null : (FolderList)ref.get ();
            if (list == null && create) {
                list = new FolderList (folder, true);
                map.put (folder, new SoftReference (list));
            }
        }
        return list;
    }
    
    /**
     * Has this FolderList finished creation of this list (at least once)?
     * @return true if it has been created (may still be refreshing), false if still in progress
     */
    public boolean isCreated() {
        return folderCreated;
    }
    
    /* -------------------------------------------------------------------- */
    /* -- Static methods -------------------------------------------------- */
    /* -------------------------------------------------------------------- */
    
    /** Checks whether the calling thread is the FolderRecognizer.
     */
    public static boolean isFolderRecognizerThread () {
        return PROCESSOR.isRequestProcessorThread ();
    }
    
    /* -------------------------------------------------------------------- */
    /* -- Static methods to inform FolderList for a given folder ---------- */
    /* -------------------------------------------------------------------- */
    
    /** A method used to notify the FolderList system that order has changed
     * for a given file object. 
     * 
     * @param folder the affected file object
     */
    public static void changedFolderOrder (FileObject folder) {
        FolderList list = find (folder, false);
        if (list != null) {
            list.changeComparator ();
        }
    }
    
    /** Called when a data system changed so much that there is a need for refresh
     * of a content of a folder.
     *
     * @param folder file object that can be affected
     */
    public static void changedDataSystem (FileObject folder) {
        FolderList list = find (folder, false);
        if (list != null) {
            list.refresh ();
        }
    }
    
    /* -------------------------------------------------------------------- */
    /* -- Folder content and content processing --------------------------- */
    /* -------------------------------------------------------------------- */

    /** Computes array of children associated
    * with this folder.
    */
    public DataObject[] getChildren () {
        List res = getChildrenList ();
        DataObject[] arr = new DataObject[res.size ()];
        res.toArray (arr);
        return arr;
    }

    /** List all children.
    * @return array with children
    */
    public List getChildrenList () {
        ListTask lt = getChildrenList (null);
        lt.task.waitFinished ();
        return lt.result;
    }

    /** Blocks if the processing of content of folder is in progress.
    */
    public void waitProcessingFinished () {
        Task t = refreshTask;
        if (t != null) {
            t.waitFinished ();
        }
    }

    /** Starts computation of children list asynchronously.
    */
    public RequestProcessor.Task computeChildrenList (FolderListListener filter) {
        return getChildrenList (filter).task;
    }

    private ListTask getChildrenList (FolderListListener filter) {
        ListTask lt = new ListTask (filter);
        int priority = Thread.currentThread().getPriority();

        // and then post your read task and wait
        lt.task = PROCESSOR.post (lt, 0, priority);
        return lt;
    }

    /** Setter for sort mode.
    */
    private void changeComparator () {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("changeComparator on " + folder);
        PROCESSOR.post (new Runnable () {
                            public void run () {
                                // if has been notified
                                // change mode and regenerated children
                                if (primaryFiles != null) {
                                    // the old children
                                    if (LOG) err.log ("changeComparator on " + folder + ": get old");
                                    List v = getObjects (null);
                                    if (v.size () != 0) {
                                        // the new children - also are stored to be returned next time from getChildrenList ()
                                        order = null;
                                        if (LOG) err.log ("changeComparator: get new");
                                        List r = getObjects (null);
                                        if (LOG) err.log ("changeComparator: fire change");
                                        fireChildrenChange (r, v);
                                    }
                                }
                            }
                        }, 0, Thread.MIN_PRIORITY);
    }
    
    /* -------------------------------------------------------------------- */
    /* -- Refresh --------------------------------------------------------- */
    /* -------------------------------------------------------------------- */

    /** Refreshes the list of children.
     */
    public void refresh () {
        final long now = System.currentTimeMillis();
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("refresh on " + folder + " @" + now);
        synchronized (this) {
            if (refreshTask == null) {
                refreshTask = PROCESSOR.post (new Runnable () {
                    public void run () {
                        if (LOG) err.log ("-- refresh on " + folder + ": now=" + now);
                        if (primaryFiles != null) {
                            // list of children is created, recreate it for new files
                            createBoth (null, true);
                        }
                    }
                }, getRefreshTime(), LATER_PRIORITY);
            } else {
                refreshTask.schedule(getRefreshTime());
            }
        }
    }

    /** Tries to read the value of the refresh time from a system property.
     * If the system property is not present a default value (currently 10)
     * is used.
     */
    private static int getRefreshTime() {
        if (REFRESH_TIME >= 0) {
            return REFRESH_TIME;
        }
        
        String sysProp = System.getProperty("org.openide.loaders.FolderList.refresh.interval"); // NOI18N
        if (sysProp != null) {
            try {
                REFRESH_TIME = Integer.parseInt(sysProp);
            } catch (NumberFormatException nfe) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, nfe);
            }
        }
        if (REFRESH_TIME < 0) {
            REFRESH_TIME = 10;
        }
        return REFRESH_TIME;
    }
    
    /* -------------------------------------------------------------------- */
    /* -- Implementation of FileChangeListener ---------------------------- */
    /* -------------------------------------------------------------------- */

    /** Fired when a file has been changed. Refreshes the list when a 
     *  has be changed which up to now was not a member of the list but
     *  becomes a member as a consequence of the change.
     *
     * @param fe the event describing context where action has taken place
     */
    public void fileChanged (FileEvent fe) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("fileChanged: " + fe);
        
        FileObject fo = fe.getFile ();
        
        /** condition fo.isValid () is hot fix for solving problem (similar to #17328)
         * inside filesystems  and should be reviewed.
         */
        if (fo.isData () && fo.isValid ()) {
            // when a data on the disk has been changed, look whether we
            // should reparse children
            if (primaryFiles != null) {
                // a file has been changed and the list of files is created
                try {
                    DataObject obj = DataObject.find (fo);
                    if (!primaryFiles.containsKey (obj.getPrimaryFile ())) {
                        // BUGFIX: someone who recognized the file and who isn't registered
                        // yet =>
                        // may be still not O.K.

                        // this primary file is not registered yet
                        // so recreate list of children
                        refresh();
                    }
                } catch (DataObjectNotFoundException ex) {
                    ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
                    // file without data object => no changes
                }
            }
        }
    }

    /** Fired when a file has been deleted.
    * @param fe the event describing context where action has taken place
    */
    public void fileDeleted (FileEvent fe) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("fileDeleted: " + fe);
        //    boolean debug = fe.getFile().toString().equals("P"); // NOI18N
        //if (debug) System.out.println ("fileDeleted: " + fe.getFile ()); // NOI18N
        //if (debug) System.out.println ("fileList: " + fileList + " file: " + fileList.get (fe.getFile ())); // NOI18N
        if (primaryFiles == null || primaryFiles.containsKey (fe.getFile ())) {
            // one of main files has been deleted => reparse
            //if (debug) System.out.println ("RecreateChildenList"); // NOI18N
            refresh();
            //if (debug) System.out.println ("Done"); // NOI18N
        }
    }

    /** Fired when a new file has been created. This action can only be
    * listened in folders containing the created file up to the root of
    * file system.
    *
    * @param fe the event describing context where action has taken place
    */
    public void fileDataCreated (FileEvent fe) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("fileDataCreated: " + fe);
        refresh();
    }

    /** Fired when a new file has been created. This action can only be
    * listened in folders containing the created file up to the root of
    * file system.
    *
    * @param fe the event describing context where action has taken place
    */
    public void fileFolderCreated (FileEvent fe) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("fileFolderCreated: " + fe);
        refresh();
    }

    /** Fired when a new file has been renamed.
    *
    * @param fe the event describing context where action has taken place
    */
    public void fileRenamed (FileRenameEvent fe) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("fileRenamed: " + fe);
        refresh();
        // Typically order may change as a result (#13820):
        changeComparator();
    }

    /** Fired when a file attribute has been changed.
    *
    * @param fe the event describing context where action has taken place
    */
    public void fileAttributeChanged(FileAttributeEvent fe) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log("fileAttributeChanged: " + fe);
        // update list when attrs defining order were changed
        if (fe.getFile() == folder) {
            /** Means one of attributes were changed*/
            if (fe.getName() == null) {
                changeComparator();
                return;
            }
            
            if (DataFolder.EA_ORDER.equals(fe.getName()) ||
            DataFolder.EA_SORT_MODE.equals(fe.getName()) ||
            -1 != fe.getName().indexOf("/")) {
                changeComparator();
            }
        }
    }
        
    /* -------------------------------------------------------------------- */
    /* -- Processing methods (only called in PROCESSOR) ------------------- */
    /* -------------------------------------------------------------------- */
    
    /** The comparator for this file objects.
     * @return the comparator to use
     */
    private FolderOrder getComparator () {
        return FolderOrder.findFor (folder);
    }

    /** Getter for list of children.
    * @param f filter to be notified about additions
    * @return List with DataObject types
    */
    private List getObjects (FolderListListener f) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("getObjects on " + folder);
        List res;
        if (primaryFiles == null) {
            res = createBoth (f, false);
        } else {
            if (order != null) {
                res = createObjects (order, primaryFiles, f);
            } else {
                res = createObjects (primaryFiles.keySet (), primaryFiles, f);
                res = carefullySort (res, getComparator ());
                order = createOrder (res);
            }
        }
        return res;
        /* createChildrenAndFiles ();/*
        ArrayList v = (Collection)childrenList.get ();
        //if (debug) System.out.println ("Children list xxxxxxxxxxxxxx");
        if (v == null) {
        //if (debug) System.out.println ("Create them xxxxxxxxxxxx");
          v = createChildrenList (f);
        //if (debug) System.out.println ("result: " + v);
    }
        return v;*/
    }

    /** Sort a list of DataObject's carefully.
     * The supplied comparator should supply a basic ordering,
     * and may also have an associated overriding partial ordering.
     * If the partial ordering is given and is self-contradictory,
     * it will be ignored and a warning issued.
     * @param l the list to sort
     * @param c a comparator and maybe partial comparator to use
     * @return the sorted list (may or may not be the same)
     */
    private /*static*/ List carefullySort (List l, FolderOrder c) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("carefullySort on " + folder);
        // Not quite right: topologicalSort will not guarantee that these are left alone,
        // even if the constraints do not mention files in the existing folder order.
        // Adding constraints between adjacent pairs in the existing folder order is
        // not good either, since that could produce an inconsistency relative to
        // the explicitly specified constraints. E.g. you have:
        // {a, b, c, d, e, x, y} Folder-Order=[a, b, c, d, e] c/x c/y x/d y/d
        // This will currently produce the order: [a, b, c, e, x, y, d]
        // If you had the existing folder order [a, b, d, c, e], then trying to make
        // the sort stabler would just cause it to fail. XXX could try to add in the
        // stabilizing constraints first, and if that fails, try again without them...
        Collections.sort (l, c);
        Map constraints = c.getOrderingConstraints(l);
        if (constraints == null) {
            return l;
        } else {
            if (LOG) err.log ("carefullySort: partial orders");
            
            try {
                return Utilities.topologicalSort(l, constraints);
            } catch (TopologicalSortException ex) {
                List corrected = ex.partialSort();
                if (err.isLoggable(ErrorManager.WARNING)) {
                    err.log (ErrorManager.WARNING, "Note: folder " + folder + " cannot be consistently sorted due to ordering conflicts."); // NOI18N
                    err.notify (ErrorManager.INFORMATIONAL, ex);
                    err.log (ErrorManager.WARNING, "Using partial sort: " + corrected); // NOI18N
                }
                return corrected;
            }
        }
    }

    /** Creates list of primary files from the list of data objects.
    * @param list list of DataObject
    * @return list of FileObject
    */
    private static List createOrder (List list) {
        int size = list.size ();
        List res = new ArrayList (size);

        for (int i = 0; i < size; i++) {
            res.add (((DataObject)list.get (i)).getPrimaryFile ());
        }

        return res;
    }

    /** Creates array of data objects from given order
    * and mapping between files and data objects.
    *
    * @param order list of FileObjects that define the order to use
    * @param map mapping (FileObject, Reference (DataObject)) to create data objects from
    * @param f filter that is notified about additions - only items
    * which are accepted by the filter will be added. Null means no filtering.
    * @return array of data objects
    */
    private /*static*/ List createObjects (
        Collection order, Map map, FolderListListener f
    ) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("createObjects on " + folder);
        int size = order.size ();

        Iterator it = order.iterator ();

        List res = new ArrayList (size);
        for (int i = 0; i < size; i++) {
            FileObject fo = (FileObject)it.next ();

            if (!fo.isValid ()) continue;

            Reference ref = (Reference)map.get (fo);
            DataObject obj = (DataObject)ref.get ();

            if (obj == null) {
                // try to find new data object
                try {
                    obj = DataObject.find (fo);
                    ref = new SoftReference (obj);
                } catch (DataObjectNotFoundException ex) {
                    ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
                }
            }
            // add if accepted
            if (obj != null) {

                // JST: Cannot be avoided otherwise DataObject.files () can be unconsistent
                // avoid to checkFiles(this)
                //obj.recognizedByFolder();

                if (f == null) {
                    // accept all objects
                    res.add (obj);
                } else {
                    // allow the listener f to filter
                    // objects in the array res
                    f.process (obj, res);
                }
            }
        }

        if (f != null) {
            f.finished (res);
        }
        return res;
    }

    /** Scans for files in the folder and creates representation for
     * children. Fires info about changes in the nodes.
     *
     * @param filter listener to addition of nodes or null
     * @param notify true if changes in the children should be fired
     * @return vector of children
     */
    private List createBoth (FolderListListener filter, boolean notify) {
        final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
        if (LOG) err.log ("createBoth on " + folder);
        // map for (FileObject, DataObject)
        final HashMap file = new HashMap ();

        // list of all processed objects
        List all = new ArrayList ();
        // result list to return from the method
        List res = new ArrayList ();

        // map of current objects (FileObject, DataObject)
        final HashMap remove = primaryFiles == null ?
                               new HashMap () : (HashMap)primaryFiles.clone ();

        // list of new objects to add
        final List add = new ArrayList ();

        DataLoaderPool pool = (DataLoaderPool)Lookup.getDefault().lookup(DataLoaderPool.class);

        // hashtable with FileObjects that are marked to be recognized
        // and that is why being out of enumeration
        final HashSet marked = new HashSet ();
        DataLoader.RecognizedFiles recog = new DataLoader.RecognizedFiles () {
                                               /** Adds the file object to the marked hashtable.
                                               * @param fo file object (can be <CODE>null</CODE>)
                                               */
                                               public void markRecognized (FileObject fo) {
                                                   if (fo != null) {
                                                       marked.add (fo);
                                                   }
                                               }
                                           };
        // enumeration of all files in the folder
        Enumeration en = folder.getChildren (false);
        while (en.hasMoreElements ()) {
            FileObject fo = (FileObject)en.nextElement ();
            if (!marked.contains (fo)) {
                // the object fo has not been yet marked as recognized
                // => continue in computation
                DataObject obj;
                try {
                    obj = pool.findDataObject (fo, recog);
                } catch (DataObjectExistsException ex) {
                    // use existing data object
                    obj = ex.getDataObject ();
                } catch (IOException ex) {
                    // data object not recognized or not found
                    obj = null;
                    ErrorManager.getDefault ().notify(ex);
                }

                if (obj != null) {
                    // adds object to data if it is not already there

                    // avoid to checkFiles(this)
                    obj.recognizedByFolder();

                    // primary file
                    FileObject primary = obj.getPrimaryFile ();

                    boolean doNotRemovePrimaryFile = false;
                    if (!file.containsKey (primary)) {
                        // realy added object, test if it is new

                        // if we have not created primaryFiles before, then it is new
                        boolean goIn = primaryFiles == null;
                        if (!goIn) {
                            Reference r = (Reference)primaryFiles.get (primary);
                            // if its primary file is not between original primary files
                            // then data object is new
                            goIn = r == null;
                            if (!goIn) {
                                // if the primary file is there, but the previous data object
                                // exists and is different, then this one is new
                                DataObject obj2 = (DataObject)r.get ();
                                goIn = obj2 == null || obj2 != obj;
                                if (goIn) {
                                    doNotRemovePrimaryFile = true;
                                }
                            }
                        }

                        if (goIn) {
                            // realy new
                            add.add (obj);
                            /* JST: In my opinion it should not be here
                            * so I moved this out of this if. Is it ok?

                            if (filter != null) {
                              // fire info about addition
                              filter.acceptDataObject (obj);
                        }
                            */
                        }
                        // adds the object
                        all.add (obj);
                        if (filter == null) {
                            res.add (obj);
                        } else {
                            filter.process (obj, res);
                        }
                    }

                    if (!doNotRemovePrimaryFile) {
                        // this object exists it should not be removed
                        remove.remove (primary);
                    }

                    // add it to the list of primary files
                    file.put (primary, new WeakReference (obj));
                } else {
                    // 1. nothing to add to data object list
                    // 2. remove this object if it was in list of previous ones
                    // 3. do not put the file into list of know primary files
                    // => do nothing at all
                }
            }
        }

        // !!! section that fires info about changes should be here !!!

        // now file contains newly inserted files
        // data contains data objects
        // remove contains data objects that should be removed
        // add contains data object that were added

        primaryFiles = file;

        all = carefullySort (all, getComparator ());
        order = createOrder (all);
        if (all.size () == res.size ()) {
            // assume no filtering has been done
            res = all;
        } else {
            // sort also content of res
            res = carefullySort (res, getComparator ());
        }
            

        ////if (debug) System.out.println ("Notified: " + notified + " added: " + add.size () + " removed: " + remove.size ()); // NOI18N
        if (notify) {
            fireChildrenChange (add, remove.keySet ());
        }

        // notify the filter
        if (filter != null) {
            filter.finished (res);
        }

        return res;
    }
    
    /* -------------------------------------------------------------------- */
    /* -- PropertyChangeListener management ------------------------------- */
    /* -------------------------------------------------------------------- */

    /** Fires info about change of children to the folder.
    * @param add added data objects
    * @param removed removed data objects
    */
    private void fireChildrenChange (Collection add, Collection removed) {
        if (pcs != null) {
            if (!add.isEmpty() || !removed.isEmpty()) {
                pcs.firePropertyChange (PROP_CHILDREN, null, null);
            }
        }
    }

    /** Removes property change listener.
     * @param l the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (pcs != null) {
            pcs.removePropertyChangeListener (l);
        }
    }
    
    /** Adds a listener.
     * @param l the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (pcs == null) {
            synchronized (this) {
                if (pcs == null) {
                    pcs = new PropertyChangeSupport (this);
                }
            }
        }
        pcs.addPropertyChangeListener (l);
    }
    
    /* -------------------------------------------------------------------- */
    /* -- Inner class ListTask -------------------------------------------- */
    /* -------------------------------------------------------------------- */
    
    /** Task that holds result and also task. Moreover
    * can do the computation.
    */
    private final class ListTask implements Runnable {
        private FolderListListener filter;

        public ListTask (FolderListListener filter) {
            this.filter = filter;
        }

        public List result;
        public RequestProcessor.Task task;

        public void run () {
            final boolean LOG = err.isLoggable(ErrorManager.INFORMATIONAL);
            if (LOG) err.log ("ListTask.run 1 on " + folder);
            // invokes the refresh task before we do anything else
            if (refreshTask != null) {
                refreshTask.waitFinished ();
            }
            err.log ("ListTask.run 2");

            result = getObjects (filter);
            err.log ("ListTask.run 3");
            
            folderCreated = true;
        }
    }
    

}
