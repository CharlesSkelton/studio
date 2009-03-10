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

package org.openide.filesystems;

import java.io.*;
import java.util.*;

import org.openide.util.actions.SystemAction;
import org.openide.util.enums.*;

/** The base for all filesystems that are build above a top of
* other ones. This system expects at most one filesystem it should write
* to and any number of filesystems to read from.
*
* If there is more versions of one file than the one from writable filesystem 
* is prefered or the read only systems are scanned in the given order.
*
* @author Jaroslav Tulach
*/
public class MultiFileSystem extends FileSystem {
    static final long serialVersionUID =-767493828111559560L;

    /** what extension to add to file that mask another ones */
    static final String MASK = "_hidden"; // NOI18N

    /** array of fs. the filesystem at position 0 can be null, because
    * it is writable filesystem. Others are only for read access
    */
    private FileSystem[] systems;

    /** @see #getPropagateMasks */
    private boolean propagateMasks = false;

    /** root */
    private transient MultiFileObject root;

    /** index of the filesystem with write access */
    private static final int WRITE_SYSTEM_INDEX = 0;

    /** Creates new empty MultiFileSystem. Useful only for
    * subclasses.
    */
    protected MultiFileSystem () {
        this (new FileSystem[1]);
    }

    /** Creates new MultiFileSystem.
    * @param fileSystems array of filesystems (can contain nulls)
    */
    public MultiFileSystem (FileSystem[] fileSystems) {
        this.systems = (FileSystem [])fileSystems.clone ();
    }

    /**
     * Actually implements contract of FileSystem.refresh().
     */
    public void refresh (boolean expected) {
        Enumeration en = getMultiRoot ().existingSubFiles (true);
        while (en.hasMoreElements()) {
            FileObject fo = (FileObject)en.nextElement();
            fo.refresh(expected);
        }        
    }
    
    /** Changes the filesystems that this system delegates to
    * 
    * @param fileSystems array of filesystems
    */
    protected final void setDelegates (FileSystem[] fileSystems) {
        // save for notification
        FileSystem[] oldSystems = systems;
        // set them
        this.systems = fileSystems;

        getMultiRoot ().updateAllAfterSetDelegates(oldSystems);

        List oldList = Arrays.asList (oldSystems);
        List newList = Arrays.asList (systems);

        // notify removed filesystems
        HashSet toRemove = new HashSet(oldList);
        toRemove.removeAll(newList);
        for (Iterator iter = toRemove.iterator(); iter.hasNext(); ) {
            FileSystem fs = ((FileSystem)iter.next());
            if (fs != null)
                fs.removeNotify();
        }

        // notify added filesystems
        HashSet toAdd = new HashSet(newList);
        toAdd.removeAll(oldList);
        for (Iterator iter = toAdd.iterator(); iter.hasNext(); ) {            
            FileSystem fs = ((FileSystem)iter.next());
            if (fs != null)
                fs.addNotify();
        }
    }

    /** All filesystem that this system delegates to.
    * @return the array of delegates
    */
    protected final FileSystem[] getDelegates () {
        return systems;
    }

    /** Will mask files that are not used be listed as children?
     * @return <code>true</code> if so
     */
    public final boolean getPropagateMasks () {
        return propagateMasks;
    }

    /** Set whether unused mask files should be listed as children.
     * @param pm <code>true</code> if so
     */
    protected final void setPropagateMasks (boolean pm) {
        propagateMasks = pm;
    }

    /** This filesystem is readonly if it has not writable system.
    */
    public boolean isReadOnly () {
        return systems[WRITE_SYSTEM_INDEX] == null || systems[WRITE_SYSTEM_INDEX].isReadOnly ();
    }

    /** The name of the filesystem.
    */
    public String getDisplayName () {
        return getString ("CTL_MultiFileSystem");
    }

    /** Root of the filesystem.
    */
    public FileObject getRoot () {
        return getMultiRoot ();
    }

    /** Root of the filesystem.
    */
    private MultiFileObject getMultiRoot () {
        synchronized (MultiFileSystem.class) {
            if (root == null) {
                root = new MultiFileObject (this);
            }
            return root;
        }
    }

    /** Merge actions from all delegates.
    */
    public SystemAction[] getActions () {
	ArrayList al = new ArrayList(101); // randomly choosen constant
	HashSet uniq = new HashSet(101); // not that randommly choosen
	
	FileSystem[] del = this.getDelegates();
	for (int i=0; i<del.length; i++) {
	    if (del[i] == null) continue;
	    SystemAction[] acts = del[i].getActions();
	    for (int j=0; j<acts.length; j++) {
		if (uniq.add(acts[j])) al.add(acts[j]);
	    }
	}
	
        return (SystemAction[])al.toArray(new SystemAction[al.size ()]);
    }

    public SystemAction[] getActions (final Set foSet) {
	final ArrayList al = new ArrayList(101); // randomly choosen constant
	final HashSet uniq = new HashSet(101); // not that randommly choosen
	
	final FileSystem[] del = this.getDelegates();
	for (int i=0; i<del.length; i++) {
	    if (del[i] == null) continue;
	    final SystemAction[] acts = del[i].getActions(foSet);
	    for (int j=0; j<acts.length; j++) {
		if (uniq.add(acts[j])) al.add(acts[j]);
	    }
	}
	
        return (SystemAction[])al.toArray(new SystemAction[al.size ()]);
    }
    
    /* Finds file when its name is provided.
    *
    * @param aPackage package name where each package is separated by a dot
    * @param name name of the file (without dots) or <CODE>null</CODE> if
    *    one want to obtain name of package and not file in it
    * @param ext extension of the file or <CODE>null</CODE> if one needs
    *    package and not file name
    *
    * @warning when one of name or ext is <CODE>null</CODE> then name and
    *    ext should be ignored and scan should look only for a package
    *
    * @return FileObject that represents file with given name or
    *   <CODE>null</CODE> if the file does not exist
    */
    public FileObject find (String aPackage, String name, String ext) {
        // create enumeration of name to look for
        StringTokenizer st = new StringTokenizer (aPackage, "."); // NOI18N
        Enumeration en;
        if (name == null || ext == null) {
            en = st;
        } else {
            en = new SequenceEnumeration (
                     st,
                     new SingletonEnumeration (name + '.' + ext)
                 );
        }
        // tries to find it (can return null)
        return getMultiRoot ().find (en);
    }

    /* Finds file when its resource name is given.
    * The name has the usual format for the {@link ClassLoader#getResource(String)}
    * method. So it may consist of "package1/package2/filename.ext".
    * If there is no package, it may consist only of "filename.ext".
    *
    * @param name resource name
    *
    * @return FileObject that represents file with given name or
    *   <CODE>null</CODE> if the file does not exist
    */
    public FileObject findResource (String name) {
        if (name.length () == 0) {
            return getMultiRoot ();
        } else {
            StringTokenizer tok = new StringTokenizer (name, "/"); // NOI18N
            return getMultiRoot ().find (tok);
        }
    }

    //
    // Helper methods for subclasses
    //

    /** For given file object finds the filesystem that the object is placed on.
    * The object must be created by this filesystem orherwise IllegalArgumentException
    * is thrown.
    *
    * @param fo file object
    * @return the filesystem (from the list we delegate to) the object has file on
    * @exception IllegalArgumentException if the file object is not represented in this filesystem
    */
    protected final FileSystem findSystem (FileObject fo) throws IllegalArgumentException {
        try {
            if (fo instanceof MultiFileObject) {
                MultiFileObject mfo = (MultiFileObject)fo;
                return mfo.getLeaderFileSystem ();
            }
        } catch (FileStateInvalidException ex) {
            // can happen if there is no delegate, I do not know what to return
            // better, but we should not throw the exception
            return this;
        }

        throw new IllegalArgumentException (fo.getPath());
    }

    /** Marks a resource as hidden. It will not be listed in the list of files.
    * Uses createMaskOn method to determine on which filesystem to mark the file.
    *
    * @param res resource name of file to hide or show
    * @param hide true if we should hide the file/false otherwise
    * @exception IOException if it is not possible
    */
    protected final void hideResource (String res, boolean hide) throws IOException {
        if (hide) {
            // mask file
            maskFile (createWritableOn (res), res);
        } else {
            unmaskFile (createWritableOn (res), res);
        }
    }

    /** Finds all hidden files on given filesystem. The methods scans all files for
    * ones with hidden extension and returns enumeration of names of files
    * that are hidden.
    *
    * @param folder folder to start at
    * @param rec proceed recursivelly
    * @return enumeration of String with names of hidden files
    */
    protected static Enumeration hiddenFiles (FileObject folder, boolean rec) {
        Enumeration allFiles = folder.getChildren (rec);
        Enumeration allNull = new AlterEnumeration (allFiles) {
                                  public Object alter (Object fo) {
                                      String sf = ((FileObject)fo).getPath();
                                      if (sf.endsWith (MASK)) {
                                          return sf.substring (0, sf.length () - MASK.length ());
                                      } else {
                                          return null;
                                      }
                                  }
                              };
        return new FilterEnumeration (allNull);
    }

    //
    // methods for subclass customization
    //

    /** Finds a resource on given filesystem. The default
    * implementation simply uses FileSystem.findResource, but
    * subclasses may override this method to hide/show some
    * resources.
    *
    * @param fs the filesystem to scan on
    * @param res the resource name to look for
    * @return the file object or null 
    */
    protected FileObject findResourceOn (FileSystem fs, String res) {
        return fs.findResource (res);
    }

    /** Finds the system to create writable version of the file on.
    *
    * @param name name of the file (full)
    * @return the first one
    * @exception IOException if the filesystem is readonly
    */
    protected FileSystem createWritableOn (String name) throws IOException {
        if (systems[WRITE_SYSTEM_INDEX] == null || systems[WRITE_SYSTEM_INDEX].isReadOnly ()) {
            FSException.io ("EXC_FSisRO", getDisplayName ()); // NOI18N
        }
        return systems[WRITE_SYSTEM_INDEX];
    }

    /** Special case of createWritableOn (@see #createWritableOn). 
    *
    * @param oldName original name of the file (full) 
    * @param newName name new of the file (full) 
    * @return the first one
    * @exception IOException if the filesystem is readonly
    * @since 1.34
    */
    protected FileSystem createWritableOnForRename (String oldName, String newName) throws IOException {        
        return createWritableOn (newName);
    }
    
    /** When a file is about to be locked this method is consulted to
    * choose which delegates should be locked. By default this method
    * returns only one filesystem; the same returned by createWritableOn.
    * <P>
    * If an delegate resides on a filesystem returned in the resulting
    * set, it will be locked. All others will remain unlocked.
    *
    * @param name the resource name to lock
    * @return set of filesystems
    * @exception IOException if the resource cannot be locked
    */
    protected java.util.Set createLocksOn (String name) throws IOException {
        FileSystem writable = createWritableOn (name);
        return java.util.Collections.singleton(writable);
    }

    /** Notification that a file has migrated from one filesystem
    * to another. Usually when somebody writes to file on readonly file
    * system and the file has to be copied to write one. 
    * <P>
    * This method allows subclasses to fire for example FileSystem.PROP_STATUS
    * change to notify that annotation of this file should change.
    *
    * @param fo file object that change its actual filesystem
    */
    protected void notifyMigration (FileObject fo) {

    }

    /** Notification that a file has been marked unimportant.
    * 
    *
    * @param fo file object that change its actual filesystem
    */
    protected void markUnimportant (FileObject fo) {
    }

    /** Lets any sub filesystems prepare the environment.
     * If they do not support it, it does not care.
     */
    public void prepareEnvironment (FileSystem.Environment env)
    throws EnvironmentNotSupportedException {
        FileSystem[] layers = getDelegates ();
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] != null) {
                try {
                    layers[i].prepareEnvironment (env);
                } catch (EnvironmentNotSupportedException ense) {
                    // Fine.
                }
            }
        }
    }

    /** Notifies all encapsulated filesystems in advance
    * to superclass behaviour. */
    public void addNotify () {
        super.addNotify();
        for (int i = 0; i < systems.length; i++) {
            if (systems[i] != null) {
                systems[i].addNotify();
            }
        }
    }

    /** Notifies all encapsulated filesystems in advance
    * to superclass behaviour. */
    public void removeNotify () {
        super.removeNotify();
        for (int i = 0; i < systems.length; i++) {
            if (systems[i] != null) {
                systems[i].removeNotify();
            }
        }
    }
    

    //
    // Private methods
    //

    /** Receives name of a resource and array of three elements and
    * splits the name into folder, name and extension.
    *
    * @param res resource name
    * @param store array to store data to
    */
    private static String[] split (String res, String[] store) {
        if (store == null) {
            store = new String[3];
        }

        int file = res.lastIndexOf ('/');
        int dot = res.lastIndexOf ('.');

        if (file == -1) {
            store[0] = ""; // NOI18N
        } else {
            store[0] = res.substring (0, file);
        }

        file++;

        if (dot == -1) {
            store[1] = res.substring (file);
            store[2] = ""; // NOI18N
        } else {
            store[1] = res.substring (file, dot);
            store[2] = res.substring (dot + 1);
        }

        return store;
    }


    /** Computes a list of FileObjects in the right order
    * that can represent this instance.
    *
    * @param name of resource to find
    * @return enumeration of FileObject
    */
    Enumeration delegates (final String name) {
        Enumeration en = new ArrayEnumeration (systems);

        Enumeration objsAndNulls = new AlterEnumeration (en) {
                                       public Object alter (Object o) {
                                           FileSystem fs = (FileSystem)o;
                                           if (fs == null) {
                                               return null;
                                           } else {
                                               return findResourceOn (fs, name);
                                           }
                                       }
                                   };

        return new FilterEnumeration (objsAndNulls);
    }

    /** Creates a file object that will mask the given file.
    * @param fs filesystem to work on
    * @param res resource name of the file
    * @exception IOException if it fails
    */
    void maskFile (FileSystem fs, String res) throws IOException {
        FileObject where = findResourceOn (fs,fs.getRoot().getPath ());
        FileUtil.createData (where, res + MASK);        
    }

    /** Deletes a file object that will mask the given file.
    * @param fs filesystem to work on
    * @param res resource name of the file
    * @exception IOException if it fails
    */
    void unmaskFile (FileSystem fs, String res) throws IOException {
        FileObject fo = findResourceOn (fs,res + MASK);        

        if (fo != null) {
            FileLock lock = fo.lock ();
            try {
                fo.delete (lock);
            } finally {
                lock.releaseLock ();
            }
        }
    }

    /** Deletes a all mask files  that  mask the given file. All
    * higher levels then fs are checked and mask is deleted if necessary
    * @param fs filesystem where res is placed
    * @param res resource name of the file that should be unmasked
    * @exception IOException if it fails
    */
    void unmaskFileOnAll (FileSystem fs,String res) throws IOException {
        FileSystem[] fss = this.getDelegates();
        for (int i = 0; i < fss.length ; i++) {
            if (fss[i] == null || fss[i].isReadOnly())
                continue;
            
            unmaskFile (fss[i], res);
            /** unamsk on all higher levels, which mask files on fs-layer */
            if (fss[i] == fs) return;
        }
    }
    
    static boolean isMaskFile(FileObject fo) {
        return fo.getExt().endsWith(MASK);
    }
}
