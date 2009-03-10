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
import java.lang.ref.*;


import org.openide.util.enums.*;

/** Implementation of the file object for abstract file system.
*
* @author Jaroslav Tulach, 
*/
final class AbstractFileObject extends AbstractFolder {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2343651324897646809L;

    /** default extension separator */
    private static final char EXT_SEP = '.';

    /** default path separator */
    private static final char PATH_SEP = '/';

    /** Reference to lock or null */
    private Reference lock;

    /** cache to remember if this object is folder or not */
    private Boolean folder;

    /** the time of last modification */
    private java.util.Date lastModified;
    
    /** Constructor. Takes reference to file system this file belongs to.
    *
    * @param fs the file system
    * @param parent the parent object (folder)
    * @param name name of the object (e.g. <code>filename.ext</code>)
    */
    public AbstractFileObject (AbstractFileSystem fs, AbstractFileObject parent, String name) {
        super (fs, parent, name);
    }

    /** Getter for the right file system */
    private AbstractFileSystem getAbstractFileSystem () {
        return (AbstractFileSystem)getFileSystem ();
    }

    /** Getter for one of children.
    */
    private AbstractFileObject getAbstractChild (String name) {
        return (AbstractFileObject)getChild (name);
    }

    //
    // List
    //
    /** Method that allows subclasses to return its children.
    *
    * @return names (name . ext) of subfiles
    */
    protected final String[] list () {
        return getAbstractFileSystem ().list.children (getPath ());
    }

    /** Method to create a file object for given subfile.
    * @param name of the subfile
    * @return the file object
    */
    protected final AbstractFolder createFile (String name) {
        return getAbstractFileSystem ().createFileObject (this, name);
    }

    //
    // Info
    //

    /* Test whether this object is a folder.
    * @return true if the file object is a folder (i.e., can have children)
    */
    public boolean isFolder () {
        if (folder == null) {
            if (parent == null || getAbstractFileSystem ().info.folder (getPath ())) {
                folder = Boolean.TRUE;
                return true;
            } else {
                folder = Boolean.FALSE;
                return false;
            }
        } else {
            return folder.booleanValue ();
        }
    }

    /* Test whether this object is a data object.
    * This is exclusive with {@link #isFolder}.
    * @return true if the file object represents data (i.e., can be read and written)
    */
    public final boolean isData () {
        return !isFolder ();
    }

    /*
    * Get last modification time.
    * @return the date
    */
    public java.util.Date lastModified() {
        if (lastModified == null || !getAbstractFileSystem ().isLastModifiedCacheEnabled ()) {
            lastModified = getAbstractFileSystem ().info.lastModified (getPath ());
        }

        return lastModified;
    }

    /* Test whether this file can be written to or not.
    * @return <CODE>true</CODE> if file is read-only
    */
    public boolean isReadOnly () {
        AbstractFileSystem fs = getAbstractFileSystem ();
        return fs.isReadOnly () || fs.info.readOnly (getPath ());
    }

    /* Get the MIME type of this file.
    * The MIME type identifies the type of the file's contents and should be used in the same way as in the <B>Java
    * Activation Framework</B> or in the {@link java.awt.datatransfer} package.
    * <P>
    * The default implementation calls {@link FileUtil#getMIMEType}.
    *
    * @return the MIME type textual representation, e.g. <code>"text/plain"</code>
    */
    public String getMIMEType () {
        String retVal = getAbstractFileSystem ().info.mimeType (getPath ());
        if (retVal == null) retVal = FileUtil.getMIMEType (this);        
        
        return (retVal != null) ? retVal : "content/unknown"; // NOI18N
    }

    /* Get the size of the file.
    * @return the size of the file in bytes or zero if the file does not contain data (does not
    *  exist or is a folder).
    */
    public long getSize () {
        return getAbstractFileSystem ().info.size (getPath ());
    }

    /** Get input stream.
    * @return an input stream to read the contents of this file
    * @exception FileNotFoundException if the file does not exists, is a folder 
    * rather than a regular file  or is invalid
    */
    public InputStream getInputStream () throws java.io.FileNotFoundException {
        InputStream is = getAbstractFileSystem ().info.inputStream (getPath ());
        return StreamPool.createInputStream(this, is) ;
    }

    /* Get output stream.
    * @param lock the lock that belongs to this file (obtained by a call to
    *   {@link #lock})
    * @return output stream to overwrite the contents of this file
    * @exception IOException if an error occures (the file is invalid, etc.)
    */
    public OutputStream getOutputStream (FileLock lock)
    throws java.io.IOException {        
        return getOutputStream (lock, true);
    }

    /** fireFileChange defines if should be fired fileChanged event after close of stream*/
    synchronized OutputStream getOutputStream (FileLock lock, boolean fireFileChanged)
    throws java.io.IOException {        
        FileSystem fs = getAbstractFileSystem ();
        if (fs.isReadOnly()) 
            FSException.io("EXC_FSisRO", fs.getDisplayName ()); // NOI18N
        
        if (isReadOnly()) 
            FSException.io("EXC_FisRO", name, fs.getDisplayName ()); // NOI18N
        
        
        testLock (lock);
        OutputStream os = getAbstractFileSystem ().info.outputStream (getPath ());                
        return StreamPool.createOutputStream(this, os, fireFileChanged);
    }
    
    /* Lock this file.
    * @return lock that can be used to perform various modifications on the file
    * @throws FileAlreadyLockedException if the file is already locked
    */
    public synchronized FileLock lock () throws IOException {
        if (lock != null) {
            FileLock f = (FileLock)lock.get ();
            if (f != null) {
                //        System.out.println ("Already locked: " + this); // NOI18N
                throw new FileAlreadyLockedException();
            }
        }

        getAbstractFileSystem ().info.lock (getPath ());

        FileLock l = new AfLock ();
        lock = new WeakReference (l);
        //    Thread.dumpStack ();
        //    System.out.println ("Locking file: " + this); // NOI18N

        return l;
    }

    /** Unlocks the file. Notifies the underlaying impl.
    */
    synchronized void unlock (FileLock fLock) {
        FileLock currentLock = null;
        if (lock != null) {
            currentLock = (FileLock)lock.get();
        }
        if (currentLock == fLock) {            
            getAbstractFileSystem ().info.unlock (getPath ());
            lastModified = null;
            // clear my lock
            lock = null;
        }
    }

    /** Tests the lock if it is valid, if not throws exception.
    * @param l lock to test
    */
    private void testLock (FileLock l) throws java.io.IOException {
        if (lock == null) 
            FSException.io ("EXC_InvalidLock", l, getPath (), getAbstractFileSystem ().getDisplayName (), lock); // NOI18N
        
        if (lock.get () != l) 
            FSException.io ("EXC_InvalidLock", l, getPath (), getAbstractFileSystem ().getDisplayName (), lock.get ()); // NOI18N                
    }

    // [???] Implicit file state is important.
    /* Indicate whether this file is important from a user perspective.
    * This method allows a file system to distingush between important and
    * unimportant files when this distinction is possible.
    * <P>
    * <em>For example:</em> Java sources have important <code>.java</code> files and
    * unimportant <code>.class</code> files. If the file system provides
    * an "archive" feature it should archive only <code>.java</code> files.
    * @param b true if the file should be considered important
    */
    public void setImportant (boolean b) {
        getAbstractFileSystem ().markImportant(getPath (), b);
    }



    /* Get the file attribute with the specified name.
    * @param attrName name of the attribute
    * @return appropriate (serializable) value or <CODE>null</CODE> if the attribute is unset (or could not be properly restored for some reason)
    */
    public Object getAttribute(String attrName) {
        return getAttribute (attrName, getPath ());
    }

    /** performance hack */
    final Object getAttribute (String attrName, String path) {
        return getAbstractFileSystem ().attr.readAttribute (path, attrName);
    }

    /* Set the file attribute with the specified name.
    * @param attrName name of the attribute
    * @param value new value or <code>null</code> to clear the attribute. Must be serializable, although particular file systems may or may not use serialization to store attribute values.
    * @exception IOException if the attribute cannot be set. If serialization is used to store it, this may in fact be a subclass such as {@link NotSerializableException}.
    */
    public void setAttribute(String attrName, Object value) throws IOException {
        setAttribute (attrName, value, true);
    }

    /* helper method for MFO.setAttribute. MFO can disable firing from underlaying
     * layers. Should be reviewed in 3.4 or later 
     *@see MultiFileObject#setAttribute*/     
    void setAttribute (String attrName, Object value, boolean fire) throws IOException {                
        Object oldValue = null;
        //FileSystem fs = getAbstractFileSystem ();
        //if (fs.isReadOnly()) 
          //  FSException.io("EXC_FSisRO", fs.getDisplayName ()); // NOI18N
                
        if (fire) 
            oldValue = getAttribute (attrName);
        getAbstractFileSystem ().attr.writeAttribute (getPath (), attrName, value);
        if (fire && oldValue != value && hasAtLeastOneListeners () ) 
            fileAttributeChanged0 (new FileAttributeEvent (this,attrName,oldValue,value));        
    }
    
    /* Get all file attribute names for this file.
    * @return enumeration of keys (as strings)
    */
    public Enumeration getAttributes() {
        return getAttributes (getPath ());
    }

    final Enumeration getAttributes (String path) {
        return getAbstractFileSystem ().attr.attributes (path);
    }

   /** Creates Reference. In FileSystem, which subclasses AbstractFileSystem, you can overload method
   * createReference(FileObject fo) to achieve another type of Reference (weak, strong etc.)
   * @param fo FileObject
   * @return Reference to FileObject
   */
    protected  final Reference createReference(FileObject fo){
        return(getAbstractFileSystem ().createReference(fo));
    }

    /** Create a new folder below this one with the specified name. Fires
    * <code>fileCreated</code> event.
    *
    * @param name the name of folder to create. Periods in name are allowed.
    * @return the new folder
    * @exception IOException if the folder cannot be created (e.g. already exists)
    */
    public  FileObject createFolder (String name) throws IOException {
        AbstractFileObject fo;
        try {
            getFileSystem ().beginAtomicAction ();
            synchronized (this) {
                AbstractFileSystem fs = getAbstractFileSystem ();
                if (fs.isReadOnly()) {
                    FSException.io("EXC_FSisRO", fs.getDisplayName ()); // NOI18N
                }
                if (isReadOnly()) {
                    FSException.io("EXC_FisRO", name, fs.getDisplayName ()); // NOI18N
                }
                if (! isFolder()) {
                    FSException.io ("EXC_FoNotFolder", name, getPath(), fs.getDisplayName ()); // NOI18N
                }

                getAbstractFileSystem ().change.createFolder (getPath () + PATH_SEP + name);
                registerChild (name);
                fo = getAbstractChild (name);

                if (fo == null) {
                    // system error
                    throw new FileStateInvalidException (FileSystem.getString ("EXC_ApplicationCreateError", getPath (), name));
                }

                if (hasListeners ()) {
                    fileCreated0(new FileEvent(this, fo), false);
                }
            }
        } finally {
            getFileSystem ().finishAtomicAction ();
        }

        return fo;
    }

    /* Create new data file in this folder with the specified name. Fires
    * <code>fileCreated</code> event.
    *
    * @param name the name of data object to create (can contain a period)
    * @param ext the extension of the file (or <code>null</code> or <code>""</code>)
    *
    * @return the new data file object
    * @exception IOException if the file cannot be created (e.g. already exists)
    */
    public FileObject createData (String name, String ext) throws IOException {
        if (name.indexOf(PATH_SEP) != -1) throw new IllegalArgumentException("Use FileUtil.createData() instead!");  // NOI18N
        try {
            getFileSystem ().beginAtomicAction ();
            synchronized (this) {
                AbstractFileSystem fs = getAbstractFileSystem ();
                if (fs.isReadOnly()) {
                    FSException.io("EXC_FSisRO", fs.getDisplayName ()); // NOI18N
                }
                if (isReadOnly()) {
                    FSException.io("EXC_FisRO", name, fs.getDisplayName ()); // NOI18N
                }

                String n = (ext == null || "".equals (ext)) ? name : name + EXT_SEP + ext; // NOI18N
                if (! isFolder()) {
                    FSException.io ("EXC_FoNotFolder", n, getPath(), fs.getDisplayName ()); // NOI18N
                }

                getAbstractFileSystem ().change.createData (getPath () + PATH_SEP + n);
                registerChild (n);
                AbstractFileObject fo = getAbstractChild (n);

                if (fo == null) {
                    // system error
                    throw new FileStateInvalidException (FileSystem.getString ("EXC_ApplicationCreateError", getPath (), n));
                }

                if (hasListeners ()) {
                    fileCreated0(new FileEvent(this, fo), true);
                }

                return fo;
            }
        } finally {
            getFileSystem ().finishAtomicAction ();
        }    
    }
    
    /* Renames this file (or folder).
    * Both the new basename and new extension should be specified.
    * <p>
    * Note that using this call, it is currently only possible to rename <em>within</em>
    * a parent folder, and not to do moves <em>across</em> folders.
    * Conversely, implementing file systems need only implement "simple" renames.
    * If you wish to move a file across folders, you should call {@link FileUtil#moveFile}.
    * @param lock File must be locked before renaming.
    * @param name new basename of file
    * @param ext new extension of file or null if no extension requested (ignored for folders). 
    */
    public void rename(FileLock lock, String name, String ext) throws IOException {
        if (parent == null) {
            FSException.io ("EXC_CannotRenameRoot", getAbstractFileSystem ().getDisplayName ()); // NOI18N
        }
        if (name.indexOf ('/') != -1 || (ext != null && ext.indexOf ('/') != -1) ||
            name.indexOf ('\\') != -1 || (ext != null && ext.indexOf ('\\') != -1)) {
            FSException.io ("EXC_CannotRename", getPath (), getAbstractFileSystem ().getDisplayName (), name + "." + ext); // NOI18N
        }        
        try {
            getFileSystem ().beginAtomicAction ();
            String newFullName = null;
            String oldFullName = null;
            synchronized (parent) {
                // synchronize on your folder
                testLock (lock);

                if (isData ()) {
                    if (ext != null) 
                        name = name + EXT_SEP + ext;
                }
                newFullName =  parent.getPath () + PATH_SEP + name;
                oldFullName = getPath ();

                if (isReadOnly ()) {
                    FSException.io ("EXC_CannotRename", getPath (), getAbstractFileSystem ().getDisplayName (), newFullName); // NOI18N
                }
                if (getFileSystem ().isReadOnly()) {
                    FSException.io ("EXC_FSisRO", getAbstractFileSystem ().getDisplayName ()); // NOI18N
                }

                String on = getName ();
                String oe = getExt ();

                getAbstractFileSystem ().change.rename (oldFullName, newFullName);

                String oldName = this.name;
                this.name = name;
                /*
                      System.out.println ("Resulting file is: " + getPath ());
                      System.out.println ("Bedw      file is: " + newFullName);
                      System.out.println ("Name: " + name);
                      System.out.println ("Old : " + oldName);
                */

                parent.refresh (name, oldName);

                if (hasAtLeastOneListeners ()) {
                    fileRenamed0 (new FileRenameEvent(this, on, oe));
                }
            }
            getAbstractFileSystem ().attr.renameAttributes (oldFullName, newFullName);            
        } finally {
            getFileSystem ().finishAtomicAction ();   
        }
    }

    /* Delete this file. If the file is a folder and it is not empty then
    * all of its contents are also recursively deleted.
    *
    * @param lock the lock obtained by a call to {@link #lock}
    * @exception IOException if the file could not be deleted
    */
    public void delete (FileLock lock) throws IOException {
        if (parent == null) {
            FSException.io ("EXC_CannotDeleteRoot", getAbstractFileSystem ().getDisplayName ()); // NOI18N
        }
        
        String fullName;
        try {
            getFileSystem ().beginAtomicAction ();
            synchronized (parent) {
                testLock (lock);

                fullName = getPath ();
                try {
                    getAbstractFileSystem ().change.delete (fullName);
                } catch (IOException ex) {
                    StreamPool p = StreamPool.find (this);
                    if (p != null) {
                        p.annotate (ex);
                    }
                    throw ex;
                }

                String n = name;
                validFlag = false;

                parent.refresh (null, n, true);
            }
            getAbstractFileSystem ().attr.deleteAttributes (fullName);

            if (hasAtLeastOneListeners ()) {
                fileDeleted0(new FileEvent(this));
            }
            
        } finally {
            getFileSystem ().finishAtomicAction ();   
        }
    }

    //
    // Transfer
    //

    /** Copies this file. This allows the filesystem to perform any additional
    * operation associated with the copy. But the default implementation is simple
    * copy of the file and its attributes
    * 
    * @param target target folder to move this file to
    * @param name new basename of file
    * @param ext new extension of file (ignored for folders)
    * @return the newly created file object representing the moved file
    */
    public FileObject copy (FileObject target, String name, String ext)
    throws IOException {
        AbstractFileSystem.Transfer from = getAbstractFileSystem ().transfer;

        if (from == null || !(target instanceof AbstractFileObject)) {
            return super.copy (target, name, ext);
        }

        AbstractFileObject abstractTarget = (AbstractFileObject)target;
        AbstractFileSystem abstractFS = abstractTarget.getAbstractFileSystem ();
        AbstractFileSystem.Transfer to = abstractFS.transfer;

        if (to != null) {
            try {
                getFileSystem ().beginAtomicAction ();
                synchronized (abstractTarget) {
                    // try copying thru the transfer
                    if (abstractFS.isReadOnly()) {
                        FSException.io ("EXC_FSisRO", abstractFS.getDisplayName ()); // NOI18N
                    }

                    if (target.isReadOnly()) {
                        FSException.io ("EXC_FisRO", target.getPath (), abstractFS.getDisplayName ()); // NOI18N
                    }

                    String n = "".equals (ext) ? name : name + EXT_SEP + ext; // NOI18N

                    if (from.copy (getPath (), to, target.getPath () + PATH_SEP + n)) {
                        // the transfer implementation thinks that the copy succeeded
                        abstractTarget.registerChild (n);
                        AbstractFileObject fo = abstractTarget.getAbstractChild (n);

                        if (fo == null) {
                            // system error
                            throw new FileStateInvalidException (FileSystem.getString ("EXC_ApplicationCreateError", abstractTarget.getPath (), n));
                        }

                        if (abstractTarget.hasListeners ()) {
                            abstractTarget.fileCreated0(new FileEvent(abstractTarget, fo), true);
                        }

                        return fo;
                    }
                }
            } finally {
                getFileSystem ().finishAtomicAction ();
            }
        }
    
        return super.copy (target, name, ext);
    }


    /** Moves this file. This allows the filesystem to perform any additional
    * operation associated with the move. But the default implementation is encapsulated
    * as copy and delete.
    * 
    * @param lock File must be locked before renaming.
    * @param target target folder to move this file to
    * @param name new basename of file
    * @param ext new extension of file (ignored for folders)
    * @return the newly created file object representing the moved file
    */
    public FileObject move (FileLock lock, FileObject target, String name, String ext)
    throws IOException {
        AbstractFileSystem fs = getAbstractFileSystem ();

        if (parent == null) {
            FSException.io ("EXC_CannotDeleteRoot", fs.getDisplayName ()); // NOI18N
        }

        AbstractFileSystem.Transfer from = getAbstractFileSystem ().transfer;

        if (from == null || !(target instanceof AbstractFileObject)) {
            return super.move (lock, target, name, ext);
        }

        AbstractFileObject abstractTarget = (AbstractFileObject)target;
        AbstractFileSystem abstractFS = abstractTarget.getAbstractFileSystem ();
        AbstractFileSystem.Transfer to = abstractFS.transfer;

        if (to != null) {
            try {
                getFileSystem ().beginAtomicAction ();
                synchronized (parent) {
                    testLock (lock);

                    if (abstractFS.isReadOnly()) {
                        FSException.io ("EXC_FSisRO", abstractFS.getDisplayName ()); // NOI18N
                    }

                    if (target.isReadOnly()) {
                        FSException.io ("EXC_FisRO", target.getPath (), abstractFS.getDisplayName ()); // NOI18N
                    }

                    String n = "".equals (ext) ? name : name + EXT_SEP + ext; // NOI18N
                    String fullName = getPath ();

                    if (from.move (fullName, to, target.getPath () + PATH_SEP + n)) {
                        // the transfer implementation thinks that the move succeeded
                        String oldN = name;
                        validFlag = false;

                        // refresh the parent because this file has been deleted
                        parent.refresh (null, oldN);

                        // deletes all attributes asssociated with the moved file
                        // JST: I am not sure if this is the right behaviour, maybe this
                        //      should be the reposibility of from.move?
                        // fs.attr.deleteAttributes (fullName);

                        // refresh the target so new file appears there
                        abstractTarget.registerChild (n);
                        AbstractFileObject fo = abstractTarget.getAbstractChild (n);

                        if (fo == null) {
                            // system error
                            throw new FileStateInvalidException (FileSystem.getString ("EXC_ApplicationCreateError", abstractTarget.getPath (), n));
                        }

                        if (hasAtLeastOneListeners ()) {
                            fileDeleted0(new FileEvent(this));
                        }

                        if (abstractTarget.hasListeners ()) {
                            abstractTarget.fileCreated0(new FileEvent(abstractTarget, fo), true);
                        }                    

                        return fo;
                    }
                }
            } finally {
                getFileSystem ().finishAtomicAction ();   
            }
        }
        return super.move (lock, target, name, ext);
    }
    
    /**
     * Tests if file really exists or is missing. Some operation on it may be restricted.
     * @return true indicates that the file is missing. 
     * @since 1.9
     */    
    public boolean isVirtual() {
        return getAbstractFileSystem ().checkVirtual (getPath ());
    }
    

    /** Refresh the content of file. Ignores changes to the files provided,
    * instead returns its file object.
    * @param added do not notify addition of this file
    * @param removed do not notify removing of this file
    * @param fire true if we should fire changes
    */
    protected void refresh (
        String added, String removed, boolean fire, boolean expected
    ) {
        this.refresh(added, removed, fire, expected, null);
    }

    /** Refresh the content of file. Ignores changes to the files provided,
    * instead returns its file object.
    * @param added do not notify addition of this file
    * @param removed do not notify removing of this file
    * @param fire true if we should fire changes
    * @param list a list of children
    */
    protected void refresh (
        String added, String removed, boolean fire, boolean expected, String[] list
    ) {
        FileEvent ev = null;
        boolean refreshParent = false;
        try {
            getFileSystem ().beginAtomicAction ();
            synchronized (this) {
                if (isFolder ()) {
                    super.refresh (added, removed, fire, expected, list);
                } else {
                    StreamPool strPool = StreamPool.find (this);
                    /** Events should not be fired until stream was closed. */
                    if (strPool != null && strPool.isOutputStreamOpen ())
                        return;
                    // check the time of a file last modification                    
                    java.util.Date l = null;

                    if (lastModified == null) {
                        lastModified = l = getAbstractFileSystem ().info.lastModified (getPath ());
                        return;
                    } else
                        l = getAbstractFileSystem ().info.lastModified (getPath ());

                    /*If file was already deleted then new java.io.File("...").lastModified() returns 0.
                     This value is converted to java.util.Date (new java.io.File("...").lastModified())
                     Such java.util.Date is then evaluated as if file was modified. But the file was 
                     actually deleted.
                     */
                    // JST: Seems like the lastModified () time can vary a bit on NT (up to 500ms)
                    //        if (!l.equals (lastModified)) {                        
                    // Solution: if (Math.abs(lastModified.getTime() - l.getTime ()) >= 5000) {
                    // I think that above mentioned solution is not necessary. Because 
                    // events should not be fired until stream was closed. 
                    if (Math.abs(lastModified.getTime() - l.getTime ()) != 0) {
                        /*
                        System.out.println("file     : " + getPath ());          
                        System.out.println("prev date: " + lastModified.getTime ());
                        System.out.println("now  date: " + l.getTime());
                        System.out.println("diff     : " + (lastModified.getTime () - l.getTime()));
                        */
                        lastModified = l;
                        if (fire && hasAtLeastOneListeners())
                            ev = new FileEvent (this, this, expected);
                        
                        if (l.getTime() == 0) {
                            validFlag = false;
                            if (ev != null) fileDeleted0(ev);                                                        
                            refreshParent = true;
                        } else if (ev != null) fileChanged0(ev);                        
                    }                    
                }
            }
            if (refreshParent && parent.getFileObject(getName (),getExt ()) != null) 
                parent.refreshFolder(null,this.getNameExt(),fire,expected,null);            
        } finally {
             getFileSystem ().finishAtomicAction ();   
        }                
            
        //    System.out.println ("Refresh of " + this + " ended"); // NOI18N
        return;
    }
    
    /** Notification that the output stream has been closed.
     * @fireFileChange defines if should be fired fileChanged event after close of stream
     */
    protected void outputStreamClosed (boolean fireFileChanged) {
        synchronized (this) {
            lastModified = null;        
        }
        super.outputStreamClosed (fireFileChanged);        
    }
    
     // Implements FileObject.canWrite() 
    public boolean canWrite() {
        AbstractFileSystem fs = getAbstractFileSystem ();        
        return fs.canWrite (getPath ());
    }    

    // Implements FileObject.canRead() 
    public boolean canRead() {
        AbstractFileSystem fs = getAbstractFileSystem ();                
        return fs.canRead (getPath ());
    }
    
    // Helper method used in AbstractFileSystem.canWrite() 
    final boolean superCanWrite() {        
        return super.canWrite();
    }    

    // Helper method used in AbstractFileSystem.canRead()     
    final boolean superCanRead() {        
        return super.canRead();
    }
    
    /** Implementation of lock for abstract files.
    */
    private class AfLock extends FileLock {
	AfLock() {}
	
        public void releaseLock () {
            if (this.isValid()) {
                super.releaseLock();
                unlock (this);                
            }
        }
    }

    //
    // Invalid object that can be created after deserialization
    //

    static final class Invalid extends FileObject {
        static final long serialVersionUID =-4558997829579415276L;

        /** name */
        private String name;
        private String fullName;
	private String fileSystemName;

        /** special instance that represent root */
        private static final Invalid ROOT = new Invalid (""); // NOI18N

        /** Constructor. 
        * @param name name of the object
        */
        public Invalid (String name) {
            fullName = name;
            int i = name.lastIndexOf ('/') + 1;
            this.name = i == 0 || i == name.length () ? name : name.substring (i);
        }

        /** Constructor. Takes reference to file system this file belongs to.
        * @param fs file system name
        * @param name name of the object
        */
        public Invalid (String fs, String name) {
	    this(name);
	    fileSystemName = fs;
        }

        /** Get the name without extension of this file or folder.
        * Period at first position is not considered as extension-separator     
        *
        * @return name of the file or folder(in its enclosing folder)
        */
        public String getName () {
            int i = name.lastIndexOf ('.');
            return i <= 0 ? name : name.substring (0, i);
        }

        /** Get the extension of this file or folder.
        * Period at first position is not considered as extension-separator     
        * This is the string after the last dot of the full name, if any.
        *
        * @return extension of the file or folder(if any) or empty string if there is none
        */
        public String getExt () {
            int i = name.lastIndexOf ('.') + 1;
            return i <= 1 || i == name.length () ? "" : name.substring (i); // NOI18N
        }

        /** @exception FileStateInvalidException always
        */
        public FileSystem getFileSystem () throws FileStateInvalidException {
            throw new FileStateInvalidException (null, name+"["+fileSystemName+"]");// NOI18N
        }

        //
        // Info
        //

        /** Test whether this object is the root folder.
        * The root should always be a folder.
        * @return true if the object is the root of a file system
        */
        public boolean isRoot () {
            return this == ROOT;
        }

        /** Test whether this object is a folder.
        * @return true if the file object is a folder (i.e., can have children)
        */
        public boolean isFolder () {
            return this == ROOT;
        }

        /**
        * Get last modification time.
        * @return the date
        */
        public java.util.Date lastModified() {
            return new java.util.Date ();
        }

        /** Test whether this object is a data object.
        * This is exclusive with {@link #isFolder}.
        * @return true if the file object represents data (i.e., can be read and written)
        */
        public boolean isData () {
            return false;
        }

        /** Test whether this file can be written to or not.
        * @return <CODE>true</CODE> if file is read-only
        */
        public boolean isReadOnly () {
            return false;
        }

        /** Test whether the file is valid. The file can be invalid if it has been deserialized
        * and the file no longer exists on disk; or if the file has been deleted.
        *
        * @return true if the file object is valid
        */
        public boolean isValid () {
            return false;
        }


        /** Get the MIME type of this file.
        * The MIME type identifies the type of the file's contents and should be used in the same way as in the <B>Java
        * Activation Framework</B> or in the {@link java.awt.datatransfer} package.
        * <P>
        * The default implementation calls {@link FileUtil#getMIMEType}.
        *
        * @return the MIME type textual representation, e.g. <code>"text/plain"</code>
        */
        public String getMIMEType () {
            return "content/unknown"; // NOI18N
        }

        /** Get the size of the file.
        * @return the size of the file in bytes or zero if the file does not contain data (does not
        *  exist or is a folder).
        */
        public long getSize () {
            return 0;
        }

        /** Get input stream.
        * @return an input stream to read the contents of this file
        * @exception FileNotFoundException if the file does not exists or is invalid
        */
        public InputStream getInputStream () throws java.io.FileNotFoundException {
            throw new java.io.FileNotFoundException ();
        }

        /** Get output stream.
        * @param lock the lock that belongs to this file (obtained by a call to
        *   {@link #lock})
        * @return output stream to overwrite the contents of this file
        * @exception IOException if an error occures (the file is invalid, etc.)
        */
        public synchronized OutputStream getOutputStream (FileLock lock)
        throws java.io.IOException {
            throw new java.io.IOException ();
        }

        /** Lock this file.
        * @return lock that can be used to perform various modifications on the file
        * @throws FileAlreadyLockedException if the file is already locked
        */
        public synchronized FileLock lock () throws IOException {
            throw new java.io.IOException ();
        }

        /** Indicate whether this file is important from a user perspective.
        * This method allows a file system to distingush between important and
        * unimportant files when this distinction is possible.
        * <P>
        * <em>For example:</em> Java sources have important <code>.java</code> files and
        * unimportant <code>.class</code> files. If the file system provides
        * an "archive" feature it should archive only <code>.java</code> files.
        * @param b true if the file should be considered important
        */
        public void setImportant (boolean b) {
        }



        /** Get the file attribute with the specified name.
        * @param attrName name of the attribute
        * @return appropriate (serializable) value or <CODE>null</CODE> if the attribute is unset (or could not be properly restored for some reason)
        */
        public Object getAttribute(String attrName) {
            return null;
        }

        /** Set the file attribute with the specified name.
        * @param attrName name of the attribute
        * @param value new value or <code>null</code> to clear the attribute. Must be serializable, although particular file systems may or may not use serialization to store attribute values.
        * @exception IOException if the attribute cannot be set. If serialization is used to store it, this may in fact be a subclass such as {@link NotSerializableException}.
        */
        public void setAttribute(String attrName, Object value) throws IOException {
            throw new java.io.IOException ();
        }

        /** Get all file attribute names for this file.
        * @return enumeration of keys (as strings)
        */
        public Enumeration getAttributes() {
            return EmptyEnumeration.EMPTY;
        }


        /** Create a new folder below this one with the specified name. Fires
        * <code>fileCreated</code> event.
        *
        * @param name the name of folder to create (without extension)
        * @return the new folder
        * @exception IOException if the folder cannot be created (e.g. already exists)
        */
        public synchronized FileObject createFolder (String name) throws IOException {
            throw new java.io.IOException ();
        }

        /** Create new data file in this folder with the specified name. Fires
        * <code>fileCreated</code> event.
        *
        * @param name the name of data object to create (should not contain a period)
        * @param ext the extension of the file (or <code>null</code> or <code>""</code>)
        *
        * @return the new data file object
        * @exception IOException if the file cannot be created (e.g. already exists)
        */
        public synchronized FileObject createData (String name, String ext) throws IOException {
            throw new java.io.IOException ();
        }

        /** Renames this file (or folder).
        * Both the new basename and new extension should be specified.
        * <p>
        * Note that using this call, it is currently only possible to rename <em>within</em>
        * a parent folder, and not to do moves <em>across</em> folders.
        * Conversely, implementing file systems need only implement "simple" renames.
        * If you wish to move a file across folders, you should call {@link FileUtil#moveFile}.
        * @param lock File must be locked before renaming.
        * @param name new basename of file
        * @param ext new extension of file (ignored for folders)
        */
        public void rename(FileLock lock, String name, String ext) throws IOException {
            throw new java.io.IOException ();
        }

        /** Delete this file. If the file is a folder and it is not empty then
        * all of its contents are also recursively deleted.
        *
        * @param lock the lock obtained by a call to {@link #lock}
        * @exception IOException if the file could not be deleted
        */
        public void delete (FileLock lock) throws IOException {
            throw new java.io.IOException ();
        }

        //
        // List
        //

        /** Get parent folder.
        * The returned object will satisfy {@link #isFolder}.
        *
        * @return common root for all invalid objects
        */
        public FileObject getParent () {
            return this == ROOT ? null : ROOT;
        }


        /** Get all children of this folder (files and subfolders). If the file does not have children
        * (does not exist or is not a folder) then an empty array should be returned. No particular order is assumed.
        *
        * @return array of direct children
        * @see #getChildren(boolean)
        * @see #getFolders
        * @see #getData
        */
        public synchronized FileObject[] getChildren () {
            return new FileObject[0];
        }

        /** Retrieve file or folder contained in this folder by name.
        * <em>Note</em> that neither file nor folder is created on disk.
        * @param name basename of the file or folder (in this folder)
        * @param ext extension of the file; <CODE>null</CODE> or <code>""</code>
        *    if the file should have no extension or if folder is requested
        * @return the object representing this file or <CODE>null</CODE> if the file
        *   or folder does not exist
        * @exception IllegalArgumentException if <code>this</code> is not a folder
        */
        public synchronized FileObject getFileObject (String name, String ext) {
            return null;
        }

        /** Refresh the contents of a folder. Rescans the list of children names.
        */
        public void refresh() {
        }


        //
        // Listeners section
        //

        /** Add new listener to this object.
        * @param l the listener
        */
        public void addFileChangeListener (FileChangeListener fcl) {
        }


        /** Remove listener from this object.
        * @param l the listener
        */
        public void removeFileChangeListener (FileChangeListener fcl) {
        }
        
        
    } // end of Invalid

    /** Replace that stores name of fs and file.
    */
    static final class Replace extends Object implements java.io.Serializable {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -8543432135435542113L;

        private String fsName;
        private String fileName;

        /** Constructor
        */
        public Replace (String fsName, String fileName) {
            this.fsName = fsName;
            this.fileName = fileName;
        }

        /** Finds the right file.
        */
        public Object readResolve () {
            Repository rep = ExternalUtil.getRepository ();
            FileSystem fs = rep.findFileSystem (fsName);
            FileObject fo = null;
            if (fs != null) {
                // scan desired system
                fo = fs.findResource (fileName);
            }

            if (fo == null) {
                // scan all systems
                fo = rep.findResource (fileName);
            }

            if (fo == null) {
                // create invalid file instead
                return new Invalid (fsName, fileName);
            }

            return fo;
        }

    } // end of Replace

    
}
