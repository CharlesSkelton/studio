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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.ChangeListener;

import org.openide.ErrorManager;
import org.openide.filesystems.*;
import org.openide.util.RequestProcessor;
import java.lang.ref.*;
import org.openide.util.WeakSet;
import org.openide.util.Lookup;

/** Registraction list of all data objects in the system.
* Maps data objects to its handlers.
*
* @author Jaroslav Tulach
*/
final class DataObjectPool extends Object
implements ChangeListener, RepositoryListener, PropertyChangeListener, Runnable {
    /** validator */
    private static final Validator VALIDATOR = new Validator ();
    
    /** hashtable that maps FileObject to DataObjectPool.Item */
    private HashMap map = new HashMap ();
    
    /** Set<FileSystem> covering all FileSystems we're listening on */
    private WeakSet knownFileSystems = new WeakSet();
    
    /** the pool for all objects. Use getPOOL method instead of direct referencing
     * this field.
     */
    private static DataObjectPool POOL;

    /** Lock for creating POOL instance */
    private static Object lockPOOL = new Object();
    
    /** Get the instance of DataObjectPool - value of static field 'POOL'.
     * Initialize the field if necessary.
     *
     * @return The DataObjectPool.
     */
    static DataObjectPool getPOOL() {
        synchronized (lockPOOL) {
            if (POOL != null)
                return POOL;
            POOL = new DataObjectPool ();
        }
        
        ((DataLoaderPool)Lookup.getDefault ().lookup (DataLoaderPool.class)).addChangeListener (POOL);
        Repository.getDefault().addRepositoryListener (POOL);

        return POOL;
    }
    
    /** Collection of all objects that has been created but their
    * creation has not been yet notified to OperationListener.postCreate
    * method.
    *
    * Set<Item>
    */
    private HashSet toNotify = new HashSet();
    
    /** A special hack to work around code like:
     * <pre>
     *  MyDataObject (FileObject fo) { // constructor of a data object
     *      super (fo);
     *
     *      DataObject.find (fo);
     * </pre>
     * which is very common (MultiDataObject.secondaryEntries ()) and which 
     * waits SAFE_NOTIFY_DELAY in waitNotified method.
     *
     *
     * <P>
     * This variable holds the reference to DataObject that was created by each
     * thread, so if the same thread calls back, it will not wait in waitNotified
     * method.
     * <P>
     * Contais object of value Item
     */
    private ThreadLocal last = new ThreadLocal ();
    
    /** Time when the toNotify set has been modified.
     */
    private long toNotifyModified;
    /** A delay to check the notify modified content. It is expected that
     * in 500ms each constructor can finish, so 500ms after the registration
     * of object in a toNotify map, it should be ready and initialized.
     */
    private static final int SAFE_NOTIFY_DELAY = 500;
    
    private static final Integer ONE = new Integer(1);
    
    /** A task to check toNotify content and notify that objects were created.
     */
    private RequestProcessor.Task task;
    
    /** Constructor.
     */
    private DataObjectPool () {
        task = RequestProcessor.createRequest (this);
        task.setPriority (Thread.MIN_PRIORITY);
    }



    /** Checks whether there is a data object with primary file
    * passed thru the parameter.
    *
    * @param fo the file to check
    * @return data object with fo as primary file or null
    */
    public DataObject find (FileObject fo) {
        synchronized (this) {
            Item doh = (Item)map.get (fo);
            if (doh == null) {
                return null;
            }
            
            // do not return DOs before their creation were notified to OperationListeners
            if (toNotify.contains (doh)) {
                // special test for data objects calling this method from 
                // their own constructor, those are ok to be returned if
                // they exist
                if (last.get () != doh) {
                    return null;
                }
            }

            return doh.getDataObjectOrNull ();
        }
    }
    
    /** mapping of files to registration count */
    private final Map registrationCounts = new WeakHashMap(); // Map<FileObject,int>
    void countRegistration(FileObject fo) {
        Integer i = (Integer)registrationCounts.get(fo);
        Integer i2;
        if (i == null) {
            i2 = ONE;
        } else {
            i2 = new Integer(i.intValue() + 1);
        }
        registrationCounts.put(fo, i2);
    }
    /** For use from FolderChildren. @see "#20699" */
    int registrationCount(FileObject fo) {
        Integer i = (Integer)registrationCounts.get(fo);
        if (i == null) {
            return 0;
        } else {
            return i.intValue();
        }
    }
    
    /** Refresh of all folders.
    */
    private void refreshAllFolders () {
        Set files;
        synchronized (this) {
            files = new HashSet (map.keySet ());
        }

        Iterator it = files.iterator ();
        while (it.hasNext ()) {
            FileObject fo = (FileObject)it.next ();
            if (fo.isFolder ()) {
                DataObject obj = find (fo);
                if (obj instanceof DataFolder) {
                    DataFolder df = (DataFolder)obj;
                    FileObject file = df.getPrimaryFile ();
                    synchronized (this) {
                        if (toNotify.isEmpty() || !toNotify.contains((Item)map.get(file))) {
                            FolderList.changedDataSystem (file);
                        }
                    }
                }
            }
        }
    }

    /** Rescans all fileobjects in given set.
    * @param s mutable set of FileObjects
    * @return set of DataObjects that refused to be revalidated
    */
    public Set revalidate (Set s) {
        return VALIDATOR.revalidate (s);
    }

    /** Rescan all primary files of currently existing data
    * objects.
    *
    * @return set of DataObjects that refused to be revalidated
    */
    public Set revalidate () {
        Set files;
        synchronized (this) {
            files = createSetOfAllFiles (map.values ());
        }

        return revalidate (files);
    }

    /** Notifies that an object has been created.
     * @param obj the object that was created
    */
    public void notifyCreation (DataObject obj) {
        synchronized (this) {
            if (toNotify.isEmpty()) {
                return;
            }
            
            if (!toNotify.remove (obj.item)) {
                return;
            }
            
            if (toNotify.isEmpty ()) {
                // ok, we do not need the task
                task.cancel ();
            }
            
            // if somebody is caught in waitNotified then wake him up
            notifyAll ();
        }

        DataLoaderPool pool = (DataLoaderPool)Lookup.getDefault().lookup(DataLoaderPool.class);
        pool.fireOperationEvent (
            new OperationEvent (obj), OperationEvent.CREATE
        );
    }
    
    /** Wait till the data object will be notified. But wait limited amount
     * of time so we will not deadlock
     *
     * @param obj data object to check
     */
    public void waitNotified (DataObject obj) {
        try {
            synchronized (this) {
                if (toNotify.isEmpty()) {
                    return;
                }
                
                if (obj.item == last.get ()) {
                    return;
                }

                if (!toNotify.contains (obj.item)) {
                    return;
                }

                wait (SAFE_NOTIFY_DELAY);
            }
        } catch (InterruptedException ex) {
        }
    }
        
    
    /** Invoked to periodicaly check whether some data objects are not notified
     * to be created. In such case it notifies about their creation.
     */
    public void run () {
        Item arr []; 

        synchronized (this) {
            if (toNotify.isEmpty()) {
                return;
            }
            
            if (System.currentTimeMillis () < toNotifyModified + SAFE_NOTIFY_DELAY) {
                task.schedule (SAFE_NOTIFY_DELAY);
                return;
            }
            arr = (Item [])toNotify.toArray (new Item [toNotify.size ()]);
        }

        // notify each created object
        for (int i = 0; i < arr.length; i++) {
            DataObject obj = arr[i].getDataObjectOrNull ();
            
            // notifyCreation removes object from toNotify queue,
            // if object was already invalidated then remove it as well
            if (obj != null) {
                notifyCreation (obj);
            } else {
                synchronized (this) {
                    toNotify.remove (arr[i]);
                }
            }
        }
    }

    /** Add to list of created objects.
     */
    private void notifyAdd (Item item) {
        if (toNotify.isEmpty()) {
            task.schedule (SAFE_NOTIFY_DELAY);
        }
        toNotify.add (item);
        last.set (item);
        toNotifyModified = System.currentTimeMillis ();
    }

    
    /** Listener used to distribute the File events to their DOs.
     * [pnejedly] A little bit about its internals/motivation:
     * Originally, every created DO have hooked its onw listener to the primary
     * FO's parent folder for listening on primary FO changes. The listener
     * was enhanced in MDO to also cover secondaries.
     * Now there is one FSListener per FileSystem which have to distribute
     * the events to the DOs using limited DOPool's knowledge about FO->DO
     * mapping. Because the mapping knowledge is limited to primary FOs only,
     * it have to resort to notifying all known DOs for given folder
     * if the changed file is not known. Although it is not as good as direct
     * notification used for known primaries, it is still no worse than
     * all DOs listening on their folder themselves as it spares at least
     * the zillions of WeakListener instances.
     */
    private final class FSListener extends FileChangeAdapter {
        FSListener() {}
        /**
         * @return Iterator<Item>
         */
        private Iterator getTargets(FileEvent fe) {
            FileObject fo = fe.getFile();
            List toNotify = new LinkedList();
            // The FileSystem notifying us about the changes should
            // not hold any lock so we're safe here
            synchronized (DataObjectPool.this) {
                Item itm = (Item)map.get (fo);
                if (itm != null) { // the file was someones' primary
                    toNotify.add(itm); // so notify only owner
                } else { // unknown file or someone secondary
                    FileObject parent = fo.getParent();
                    if (parent != null) { // the fo is not root
                        FileObject[] siblings = parent.getChildren();
                        // notify all in folder
                        for (int i=0; i<siblings.length; i++) { 
                            itm = (Item)map.get (siblings[i]);
                            if (itm != null) toNotify.add(itm);
                        }
                    }
                }
            }
            return toNotify.iterator();
        }

        public void fileRenamed (FileRenameEvent fe) {
            for( Iterator it = getTargets(fe); it.hasNext(); ) {
                DataObject dobj = ((Item)it.next()).getDataObjectOrNull();
                if (dobj != null) dobj.notifyFileRenamed(fe);
            }
        }

        public void fileDeleted (FileEvent fe) {
            for( Iterator it = getTargets(fe); it.hasNext(); ) {
                DataObject dobj = ((Item)it.next()).getDataObjectOrNull();
                if (dobj != null) dobj.notifyFileDeleted(fe);
            }
        }

        public void fileDataCreated (FileEvent fe) {
            for( Iterator it = getTargets(fe); it.hasNext(); ) {
                DataObject dobj = ((Item)it.next()).getDataObjectOrNull();
                if (dobj != null) dobj.notifyFileDataCreated(fe);
            }
        }
        
        public void fileAttributeChanged (FileAttributeEvent fe) {
            for( Iterator it = getTargets(fe); it.hasNext(); ) {
                DataObject dobj = ((Item)it.next()).getDataObjectOrNull();
                if (dobj != null) dobj.notifyAttributeChanged(fe);
            }
        }
    }
    
    /** Registers new DataObject instance.
    * @param fo primary file for obj
    * @param loader the loader of the object to be created
    *
    * @return object with common information for this <CODE>DataObject</CODE>
    * @exception DataObjectExistsException if the file object is already registered
    */
    public Item register (FileObject fo, DataLoader loader) throws DataObjectExistsException {
        // here we're registering a listener on fo's FileSystem so we can deliver
        // fo changes to DO without lots of tiny listeners on folders
        // The new DS bound to a repository can simply place a single listener
        // on its repository instead of registering listeners on FileSystems. 
        try { // to register a listener of fo's FileSystem
            FileSystem fs = fo.getFileSystem();
            synchronized (knownFileSystems) {
                if (! knownFileSystems.contains(fs)) {
                    fs.addFileChangeListener (new FSListener());
                    knownFileSystems.add(fs);
                }
            }
        } catch (FileStateInvalidException e ) {
            // no need to listen then
        }
        
        Item doh;
        DataObject obj;
        synchronized (this) {
            doh = (Item)map.get (fo);
            // if Item for this file has not been created yet
            if (doh == null) {
                doh = new Item (fo);
                map.put (fo, doh);
                countRegistration(fo);
                notifyAdd (doh);

                VALIDATOR.notifyRegistered (fo);

                return doh;
            }
            
            obj = doh.getDataObjectOrNull ();

            if (obj == null) {
                // the item is to be finalize => create new
                doh = new Item (fo);
                map.put (fo, doh);
                countRegistration(fo);
                notifyAdd (doh);

                return doh;
            }
            
            if (!VALIDATOR.reregister (obj, loader)) {
                throw new DataObjectExistsException (obj);
            }
        }
        
        try {
            obj.setValid (false);
            synchronized (this) {
                // check if there isn't any new data object registered 
                // when this thread left synchronization block.
                Item doh2 = (Item)map.get (fo);
                if (doh2 == null) {
                    doh = new Item (fo);
                    map.put (fo, doh);
                    countRegistration(fo);
                    notifyAdd (doh);

                    return doh;
                }
            }
        } catch (java.beans.PropertyVetoException ex) {
            VALIDATOR.refusingObjects.add (obj);
        }
        throw new DataObjectExistsException (obj);
    }

    /** Notifies all newly created objects to

    /** Deregister.
    * @param item the item with common information to deregister
    * @param refresh true if the parent folder should be refreshed
    */
    private synchronized void deregister (Item item, boolean refresh) {
        FileObject fo = item.primaryFile;

        Item previous = (Item)map.remove (fo);

        if (previous != null && previous != item) {
            // ops, mistake,
            // return back the original
            map.put (fo, previous);
            countRegistration(fo);
            // Furthermore, item is probably in toNotify by mistake.
            // Observed in DataFolderTest.testMove: after vetoing the move
            // of a data folder, the bogus item for the temporary new folder
            // (e.g. BB/AAA/A1) is left in the toNotify pool forever. This
            // point is reached; remove it now. -jglick
            if (toNotify.remove(item)) {
                if (toNotify.isEmpty()) {
                    task.cancel();
                }
                notifyAll();
            }
            return;
        }

        // refresh of parent folder
        if (refresh) {
            fo = fo.getParent ();
            if (fo != null) {
                Item item2 = (Item)map.get (fo);
                if (item2 != null) {
                    DataFolder df = (DataFolder) item2.getDataObjectOrNull();
                    if (df != null) {
                        VALIDATOR.refreshFolderOf (df);
                    }
                }
            }
        }
    }

    /** Changes the primary file to new one.
    * @param item the item to change
    * @param newFile new primary file to set
    */
    private synchronized void changePrimaryFile (
        Item item, FileObject newFile
    ) {
        map.remove (item.primaryFile);
        item.primaryFile = newFile;
        map.put (newFile, item);
        countRegistration(newFile);
    }

    /** When the loader pool is changed, then all objects are rescanned.
    */
    public void stateChanged (javax.swing.event.ChangeEvent ev) {
        Set set;
        synchronized (this) {
            // copy the values synchronously
            set = new HashSet (map.values ());
        }
        set = createSetOfAllFiles (set);
        revalidate (set);
    }
    
    /** Create list of all files for given collection of data objects.
    * @param c collection of DataObjectPool.Item
    * @return set of files
    */
    private static Set createSetOfAllFiles (Collection c) {
        HashSet set = new HashSet (c.size () * 7);
        
        Iterator it = c.iterator();
        while (it.hasNext()) {
            Item item = (Item)it.next ();
            DataObject obj = item.getDataObjectOrNull ();
            if (obj != null) {
                set.addAll (obj.files ());
            }
        }
        return set;
    }
    
    /** Remove DataObjects which became invalid thanks
     * to unmounting a FileSystem
     */
    private void removeInvalidObjects() {
        Set files;
        synchronized (this) {
            files = new HashSet (map.values ());
        }
        files = createSetOfAllFiles (files);
        
        VALIDATOR.removeInvalidObject(files);
    }

    //
    // Repository listener changes
    //
    /** Called when new file system is added to the pool.
     * @param ev event describing the action
     */
    public void fileSystemAdded(RepositoryEvent ev) {
        ev.getFileSystem().addPropertyChangeListener( getPOOL() );
    }
    /** Called when a file system is removed from the pool.
     * @param ev event describing the action
     */
    public void fileSystemRemoved(RepositoryEvent ev) {
        ev.getFileSystem().removePropertyChangeListener( getPOOL() );
        removeInvalidObjects();
    }

    /** Called when a file system pool is reordered. */
    public void fileSystemPoolReordered(RepositoryReorderedEvent ev) {
    }
    
    /** Called when a file system property changed.
     * If it's property root, check validity.
     * @param ev event describing the action     
    */
    public void propertyChange (final PropertyChangeEvent ev) {
        if (FileSystem.PROP_SYSTEM_NAME.equals (ev.getPropertyName ())) {
            removeInvalidObjects();
        }        
        if (FileSystem.PROP_ROOT.equals (ev.getPropertyName ())) {
            removeInvalidObjects();
        }        
    }

    /** Returns all currently existing data
    * objects.
    *
    * @return iterator of DataObjectPool.Item
    */    
    Iterator getActiveDataObjects () {
        synchronized (this) {
            ArrayList alist = new ArrayList(map.values());
            return alist.iterator();
        }
    }

    /** One item in object pool.
    */
    static final class Item extends Object {
        /** initial value of obj field. */
        private static final Reference REFERENCE_NOT_SET = new WeakReference(null);

        /** weak reference data object with this primary file */
        private Reference obj = REFERENCE_NOT_SET;
        
        /** primary file */
        FileObject primaryFile;
        
        // [PENDING] hack to check the stack when the DataObject has been created
        //    private Exception stack;

        /** @param fo primary file
        * @param pool object pool
        */
        public Item (FileObject fo) {
            this.primaryFile = fo;

            // [PENDING] // stores stack
            /*      java.io.StringWriter sw = new java.io.StringWriter ();
                  stack = new Exception ();
                }

                // [PENDING] toString returns original stack
                public String toString () {
                  return stack.toString ();*/
        }

        /** Setter for the data object. Called immediatelly as possible.
        * @param obj the data object for this item
        */
        public void setDataObject (DataObject obj) {
            this.obj = new ItemReference (obj, this);
            
            if (obj != null && !obj.getPrimaryFile ().isValid ()) {
                // if the primary file is already invalid =>
                // mark the object as invalid
                deregister (false);
            }
            
            synchronized (DataObjectPool.getPOOL()) {
                DataObjectPool.getPOOL().notifyAll();
            }
        }

        /** Getter for the data object.
        * @return the data object or null
        */
        DataObject getDataObjectOrNull () {
            synchronized (DataObjectPool.getPOOL()) {
                while (this.obj == REFERENCE_NOT_SET) {
                    try {
                        DataObjectPool.getPOOL().wait ();
                    }
                    catch (InterruptedException exc) {
                    }
                }
            }
            
            return this.obj == null ? null : (DataObject)this.obj.get ();
        }
        
        /** Getter for the data object.
        * @return the data object
        * @exception IllegalStateException if the data object has been lost
        *   due to weak references (should not happen)
        */
        public DataObject getDataObject () {
            DataObject obj = getDataObjectOrNull ();
            if (obj == null) {
                throw new IllegalStateException ();
            }
            return obj;
        }

        /** Deregister one reference.
        * @param refresh true if the parent folder should be refreshed
        */
        public void deregister (boolean refresh) {
            getPOOL().deregister (this, refresh);
        }

        /** Changes the primary file to new one.
        * @param newFile new primary file to set
        */
        public void changePrimaryFile (FileObject newFile) {
            getPOOL().changePrimaryFile (this, newFile);
        }

        /** Is the item valid?
        */
        public boolean isValid () {
            if (getPOOL().map.get (primaryFile) == this) {
                return primaryFile.isValid();
            } else {
                return false;
            }
            
        }
        
        public String toString () {
            DataObject obj = (DataObject)this.obj.get ();
            if (obj == null) {
                return "nothing[" + primaryFile + "]"; // NOI18N
            }
            return obj.toString ();
        }
    }

    /** WeakReference - references a DataObject, strongly references an Item */
    static final class ItemReference extends WeakReference 
    implements Runnable {
        /** Reference to an Item */
        private Item item;
        
        ItemReference(DataObject dobject, Item item) {
            super(dobject, org.openide.util.Utilities.activeReferenceQueue());
            this.item = item;
        }

        /** Does the cleanup of the reference */
        public void run () {
            item.deregister(false);
            item = null;
        }
        
    }
    
    /** Validator to allow rescan of files.
    */
    private static final class Validator extends Object
        implements DataLoader.RecognizedFiles {
        /** set of all files that should be revalidated (FileObject) */
        private Set files;
        /** current thread that is in the validator */
        private Thread current;
        /** number of threads waiting to enter the validation */
        private int waiters;
        /** Number of calls to enter by current thread minus 1 */
        private int reenterCount;
        /** set of files that has been marked recognized (FileObject) */
        private HashSet recognizedFiles;
        /** set with all objects that refused to be discarded (DataObject) */
        private HashSet refusingObjects;
        /** set of files that has been registered during revalidation */
        private HashSet createdFiles;

	Validator() {}

        /** Enters the section.
        * @param set mutable set of files that should be processed
        * @return the set of files concatenated with any previous sets
        */
        private synchronized Set enter (Set set) {
            if (current == Thread.currentThread ()) {
                reenterCount++;
            } else {
                waiters++;
                while (current != null) {
                    try {
                        wait ();
                    } catch (InterruptedException ex) {
                    }
                }
                current = Thread.currentThread ();
                waiters--;
            }
            
            if (files == null) {
                files = set;
            } else {
                files.addAll (set);
            }

            return files;
        }

        /** Leaves the critical section.
        */
        private synchronized void exit () {
            if (reenterCount == 0) {
                current = null;
                if (waiters == 0) {
                    files = null;
                }
                notify ();
            } else {
                reenterCount--;
            }
        }

        /** If there is another waiting thread, then I can
        * cancel my computation.
        */
        private synchronized boolean goOn () {
            return waiters == 0;
        }

        /** Called to either refresh folder, or register the folder to be
        * refreshed later is validation is in progress.
        */
        public void refreshFolderOf (DataFolder df) {
            if (createdFiles == null) {
                // no validator in progress
                FolderList.changedDataSystem (df.getPrimaryFile ());
            }
        }

        /** Mark this file as being recognized. It will be excluded
        * from further processing.
        *
        * @param fo file object to exclude
        */
        public void markRecognized (FileObject fo) {
            recognizedFiles.add (fo);
        }

        public void notifyRegistered (FileObject fo) {
            if (createdFiles != null) {
                createdFiles.add (fo);
            }
        }

        /** Reregister new object for already existing file object.
        * @param obj old object existing
        * @param loader loader of new object to create
        * @return true if the old object has been discarded and new one can
        *    be created
        */
        public boolean reregister (DataObject obj, DataLoader loader) {
            if (recognizedFiles == null) {
                // revalidation not in progress
                return false;
            }

            if (obj.getLoader () == loader) {
                // no change in loader =>
                return false;
            }

            if (createdFiles.contains (obj.getPrimaryFile ())) {
                // if the file already has been created
                return false;
            }

            if (refusingObjects.contains (obj)) {
                // the object has been refused before
                return false;
            }

            return true;
        }

        /** Rescans all fileobjects in given set.
        * @param s mutable set of FileObjects
        * @return set of objects that refused to be revalidated
        */
        public Set revalidate (Set s) {
            
            // ----------------- fix of #30559 START
            if ((s.size() == 1) && (current == Thread.currentThread ())) {
                if (files != null && files.contains(s.iterator().next())) {
                    return new HashSet();
                }
            }
            // ----------------- fix of #30559 END
            
            // holds all created object, so they are not garbage
            // collected till this method ends
            LinkedList createObjects = new LinkedList ();
            try {
                
                s = enter (s);
                
                recognizedFiles = new HashSet ();
                refusingObjects = new HashSet ();
                createdFiles = new HashSet ();

                HashSet allFS = new HashSet (java.util.Arrays.asList(
                                                 Repository.getDefault().toArray()
                                             ));

                DataLoaderPool pool = (DataLoaderPool)Lookup.getDefault().lookup (DataLoaderPool.class);
                Iterator it = s.iterator ();
                while (it.hasNext () && goOn ()) {
                    try {
                        FileObject fo = (FileObject)it.next ();
                        if (!recognizedFiles.contains (fo)) {
                            // first of all test if the file is on a valid filesystem
                            boolean invalidate;
                            try {
                                FileSystem fs = fo.getFileSystem ();
                                invalidate = !allFS.contains (fs);
                            } catch (FileStateInvalidException ex) {
                                invalidate = true;
                            }

                            // the previous data object should be canceled
                            DataObject orig = getPOOL().find (fo);
                            if (orig == null) {
                                // go on
                                continue;
                            }

                            if (!invalidate) {
                                // findDataObject
                                // is not using method DataObjectPool.find to locate data object
                                // directly for primary file, that is good
                                DataObject obj = pool.findDataObject (fo, this);
                                createObjects.add (obj);

                                invalidate = obj != orig;
                            }

                            if (invalidate) {
                                it.remove();                                
                                try {
                                    orig.setValid (false);
                                } catch (java.beans.PropertyVetoException ex) {
                                    refusingObjects.add (orig);
                                }
                            }
                        }
                    } catch (DataObjectExistsException ex) {
                        // this should be no problem here
                    } catch (java.io.IOException ioe) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ioe);
                    } catch (ConcurrentModificationException cme) {
                        // not very nice but the only way I could come up to handle this:
                        // java.util.ConcurrentModificationException
                        //   at java.util.HashMap$HashIterator.remove(HashMap.java:755)
                        //   at org.openide.loaders.DataObjectPool$Validator.revalidate(DataObjectPool.java:916)
                        //   at org.openide.loaders.DataObjectPool.revalidate(DataObjectPool.java:203)
                        //   at org.openide.loaders.DataObjectPool.stateChanged(DataObjectPool.java:527)
                        //   at org.openide.loaders.DataLoaderPool$1.run(DataLoaderPool.java:128)
                        //   at org.openide.util.Task.run(Task.java:136)
                        //[catch] at org.openide.util.RequestProcessor$Processor.run(RequestProcessor.java:635)
                        // is to ignore the exception and continue
                        it = s.iterator();
                    }
                }
                return refusingObjects;
            } finally {
                recognizedFiles = null;
                refusingObjects = null;
                createdFiles = null;

                exit ();

                if ( s.size() > 1 )
                    getPOOL().refreshAllFolders ();
            }
        }
        
        /** Remove DataObjects which became invalid thanks
         * to unmounting a FileSystem
         */
        void removeInvalidObject(Set files) {
            try {
                files = enter(files);
                HashSet allFS = new HashSet();
                FileSystem[] fss = Repository.getDefault().toArray();
                for (int i = 0; i < fss.length; i++) {
                    allFS.add(fss[i]);
                }
                
                Iterator it = files.iterator();
                while (it.hasNext() && goOn()) {
                    FileObject fo = (FileObject) it.next();
                    
                    boolean invalidate = !fo.isValid();
                    if ( !invalidate ) {
                        try {
                            FileSystem fs = fo.getFileSystem ();
                            invalidate = !allFS.contains (fs);
                        } catch (FileStateInvalidException ex) {
                            invalidate = true;
                        }
                    }

                    DataObject orig = null;
                    synchronized (getPOOL()) {
                        Item itm = (Item)getPOOL().map.get (fo);
                        if (itm == null) {
                            continue;
                        }
                        orig = itm.getDataObjectOrNull ();
                    }

                    if (invalidate && orig != null) {
                        it.remove();
                        try {                            
                            orig.setValid (false);
                        } catch (java.beans.PropertyVetoException ex) {
                            // silently ignore?
                            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                        }
                    }
                }
            } finally {
                exit();
            }
        }
    } // end of Validator
}
