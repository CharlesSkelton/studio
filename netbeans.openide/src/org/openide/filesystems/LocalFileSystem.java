/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.filesystems;

import java.beans.PropertyVetoException;
import java.io.*;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.util.NbBundle;

/** Local filesystem. Provides access to files on local disk.
* <p>For historical reasons many AbstractFileSystem.* methods are implemented
* as protected in this class. Do not call them! Subclasses might override
* them, or (better) use delegation.
*/
public class LocalFileSystem extends AbstractFileSystem {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = -5355566113542272442L;
    
    /** Controlls the LocalFileSystem's automatic refresh.
    * If the refresh time interval is set from the System.property, than this value is used.
    * Otherwise, the refresh time interval is set to 0 which means the refresh
    * is disabled. */    
    private static final int REFRESH_TIME = Integer.getInteger
    ("org.openide.filesystems.LocalFileSystem.REFRESH_TIME",0).intValue(); // NOI18N

    /** root file */
    private File rootFile = new File ("."); // NOI18N

    /** is read only */
    private boolean readOnly;

    /** Constructor.
    */
    public LocalFileSystem () {
        Impl impl = new Impl (this);

        info = impl;
        change = impl;
        DefaultAttributes a = new InnerAttrs (this,info, change, impl);
        attr = a;
        list = a;
        setRefreshTime (REFRESH_TIME);
    }
    
    /** Constructor. Allows user to provide own capabilities
    * for this filesystem.
    * @param cap capabilities for this filesystem
    */
    public LocalFileSystem (FileSystemCapability cap) {
        this ();
        setCapability (cap);
    }

    /* Human presentable name */
    public String getDisplayName() {
        
        boolean validValue =  (isValid() && rootFile != null && 
        rootFile.isDirectory() && rootFile.exists ());
        
        if(validValue)
            return getString ("LAB_FileSystemValid", rootFile.getAbsolutePath ());            
        else
            return getString("LAB_FileSystemInvalid", rootFile.getAbsolutePath ());
    }
    
    /** Set the root directory of the filesystem.
    * @param r file to set root to
    * @exception PropertyVetoException if the value if vetoed by someone else (usually
    *    by the {@link org.openide.filesystems.Repository Repository})
    * @exception IOException if the root does not exists or some other error occured
    */
    public synchronized void setRootDirectory (File r) throws PropertyVetoException, IOException {
        if (!r.exists() || r.isFile ()) {
            FSException.io ("EXC_RootNotExist", r.getAbsolutePath ()); // NOI18N
        }

        String oldDisplayName = getDisplayName ();
        setSystemName(computeSystemName (r));

        rootFile = r;

        firePropertyChange(PROP_ROOT, null, refreshRoot ());
        firePropertyChange (PROP_DISPLAY_NAME, oldDisplayName, getDisplayName ());                    
    }

    /** Get the root directory of the filesystem.
     * @return root directory
    */
    public File getRootDirectory () {
        return rootFile;
    }

    /** Set whether the filesystem should be read only.
     * @param flag <code>true</code> if it should
    */
    public void setReadOnly(boolean flag) {
        if (flag != readOnly) {
            readOnly = flag;
            firePropertyChange (PROP_READ_ONLY,
                                !flag ? Boolean.TRUE : Boolean.FALSE,
                                flag ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    /* Test whether filesystem is read only.
     * @return <true> if filesystem is read only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** Prepare environment by adding the root directory of the filesystem to the class path.
    * @param environment the environment to add to
    */
    public void prepareEnvironment(FileSystem.Environment environment) {
        environment.addClassPath(rootFile.getAbsolutePath ());
    }

    /** Compute the system name of this filesystem for a given root directory.
    * <P>
    * The default implementation simply returns the filename separated by slashes.
    * @see FileSystem#setSystemName
    * @param rootFile root directory for the filesystem
    * @return system name for the filesystem
    */
    protected String computeSystemName (File rootFile) {
        return rootFile.getAbsolutePath ().replace(File.separatorChar, '/');
    }

    //
    // List
    //

    protected String[] children (String name) {
        File f = getFile (name);
        if (f.isDirectory ()) {
            return f.list ();
        } else {
            return null;
        }
    }

    //
    // Change
    //

    protected void createFolder (String name) throws java.io.IOException {
        File f = getFile (name);

        if (name.equals ("")) { // NOI18N
            FSException.io ("EXC_CannotCreateF", new Object[] { f.getName (), getDisplayName (), f.getAbsolutePath ()}); // NOI18N
        }

        if (f.exists()) {
            FSException.io ("EXC_FolderAlreadyExist", new Object[] { f.getName (), getDisplayName (), f.getAbsolutePath ()}); // NOI18N
        }

        boolean b = createRecursiveFolder(f);
        if (!b) {
            FSException.io ("EXC_CannotCreateF", new Object[] { f.getName (), getDisplayName (), f.getAbsolutePath ()}); // NOI18N
        }
    }

    /* 
    * @return true if RefreshAction should be enabled
    */        
    boolean isEnabledRefreshFolder () {
        return true;
    }

    
    /** Creates new folder and all necessary subfolders
    *  @param f folder to create
    *  @return <code>true</code> if the file exists when returning from this method
    */
    private static boolean createRecursiveFolder(File f) {
        if (f.exists()) return true;
        if (!f.isAbsolute())
            f = f.getAbsoluteFile();
        String par = f.getParent();
        if (par == null) return false;
        if (!createRecursiveFolder(new File(par))) return false;
        f.mkdir();
        return f.exists();
    }


    protected void createData (String name) throws IOException {
        File f = getFile (name);

        if (!f.createNewFile ()) {
            String msg = NbBundle.getMessage (
                LocalFileSystem.class, 
                "EXC_DataAlreadyExist", 
                new Object[] { f.getName (), getDisplayName (), f.getAbsolutePath () }
            );
            
            SyncFailedException e = new SyncFailedException (msg);
            
            ExternalUtil.annotate (e, msg);
            throw e;
        }
        /* JST: Maybe handled by createNewFile, but probably
            if (!tmp.exists())
              throw new IOException(MessageFormat.format (LocalFileSystem.getString("EXC_CannotCreateD"), errorParams));
        */
    }

    protected void rename(String oldName, String newName) throws IOException {
        File of = getFile (oldName);
        File nf = getFile (newName);
        
        // #7086 - (nf.exists() && !nf.equals(of)) instead of nf.exists() - fix for Win32
        if ((nf.exists() && !nf.equals(of)) || !of.renameTo (nf)) {
            FSException.io ("EXC_CannotRename", oldName, getDisplayName (), newName); // NOI18N
        }
    }

    protected void delete(String name) throws IOException {
        File file = getFile(name);
        if (deleteFile(file) != SUCCESS) {
            if (file.exists())
                FSException.io("EXC_CannotDelete", name, getDisplayName(), file.getAbsolutePath()); // NOI18N
            else {
                /** When file externaly deleted and fo.delete () is called before 
                 periodical refresh */
                FileObject thisFo = findResource(name);
                if (thisFo != null) {
                    if (thisFo.getParent() != null)
                        thisFo.getParent().refresh();
                    
                    thisFo.refresh();
                    
                    if (thisFo.isValid())
                        FSException.io("EXC_CannotDelete", name, getDisplayName(), file.getAbsolutePath()); // NOI18N
                }
            }
        }
    }
    
    private static final int SUCCESS = 0;
    private static final int FAILURE = 1;
    private static final int NOT_EXISTS = 3;

    /** Method that recursivelly deletes all files in a folder.
    * @return true if successful
    */
    private static int deleteFile (File file) {
        boolean ret = file.delete();
        
        if (ret) {
            return SUCCESS;
        }
        
        if (! file.exists()) {
            return NOT_EXISTS;
        }
        
        if (file.isDirectory()) {
            // first of all delete whole content
            File[] arr = file.listFiles();
            for (int i = 0; i < arr.length; i++) {
                if (deleteFile (arr[i]) != SUCCESS) {
                    return FAILURE;
                }
            }
        }
        
        // delete the file itself
        return (file.delete() ? SUCCESS : FAILURE);
    }

    //
    // Info
    //

    protected java.util.Date lastModified(String name) {
        return new java.util.Date (getFile (name).lastModified ());
    }

    protected boolean folder (String name) {
        return getFile (name).isDirectory ();
    }

    protected boolean readOnly (String name) {
        File f = getFile (name);
        return !f.canWrite () && f.exists ();
    }

    protected String mimeType (String name) {
        return null;
    }

    protected long size (String name) {
        return getFile (name).length ();
    }

    // ===============================================================================
    //  This part of code could be used for monitoring of closing file streams.
    /*  public static java.util.HashMap openedIS = new java.util.HashMap();
      public static java.util.HashMap openedOS = new java.util.HashMap();

      static class DebugIS extends FileInputStream {
        public DebugIS(File f) throws java.io.FileNotFoundException { super(f); }
        public void close() throws IOException { openedIS.remove(this); super.close(); }
      };

      static class DebugOS extends FileOutputStream {
        public DebugOS(File f) throws java.io.IOException { super(f); }
        public void close() throws IOException { openedOS.remove(this); super.close(); }
      };

      public InputStream inputStream (String name) throws java.io.FileNotFoundException {
        DebugIS is = new DebugIS(getFile(name));
        openedIS.put(is, new Exception());
        return is;
      }

      public OutputStream outputStream (String name) throws java.io.IOException {
        DebugOS os = new DebugOS(getFile(name));
        openedOS.put(os, new Exception());
        return os;
      }*/
    //  End of the debug part
    // ============================================================================
    //  Begin of the original part

    protected InputStream inputStream (String name) throws java.io.FileNotFoundException {
        FileInputStream fis;
        File    file = null;
        
        try {
            fis = new FileInputStream(file=getFile(name));
        } catch(FileNotFoundException exc) {
            if (file == null || !file.exists()) 
                ExternalUtil.annotate (exc, NbBundle.getMessage(LocalFileSystem.class,"EXC_FileOutsideModified"));
            
            throw exc;
        }        
        return fis;
    }

    protected OutputStream outputStream (String name) throws java.io.IOException {
        return new FileOutputStream (getFile (name));
    }

    //  End of the original part
    // ============================================================================

    protected void lock (String name) throws IOException {
        File file = getFile (name);
        if ((!file.canWrite () && file.exists ()) ||
            isReadOnly()) {
            FSException.io ("EXC_CannotLock", name, getDisplayName (), file.getAbsolutePath ()); // NOI18N
        }
    }

    protected void unlock (String name) {
    }

    protected void markUnimportant (String name) {
    }

    /** Creates file for given string name.
    * @param name the name
    * @return the file
    */
    private File getFile (String name) {
        // XXX should this be name.replace('/', File.separatorChar)? Cf. BT #4745638.
        return new File (rootFile, name);
    }


    /** 
    * @param in the input stream to read from
    * @exception IOException error during read
    * @exception ClassNotFoundException when class not found
    */
    private void readObject (java.io.ObjectInputStream in)
    throws java.io.IOException, java.lang.ClassNotFoundException {
        in.defaultReadObject ();

        ((ObjectInputStream)in).registerValidation(new ObjectInputValidation() {
            public void validateObject() {
                if (attr.getClass() == DefaultAttributes.class) {
                    Impl impl = new Impl(LocalFileSystem.this);
                    attr = new InnerAttrs(LocalFileSystem.this,impl, impl, impl);
                }
            }
        }, 0);
    }
    
    /** The implementation class that implements List, Info
    * and Change interfaces and delegates all the methods 
    * to appropriate methods of LocalFileSystem.
    */
    public static class Impl extends Object
                implements AbstractFileSystem.List, AbstractFileSystem.Info,
        AbstractFileSystem.Change {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -8432015909317698511L;

        /** pointer to local filesystem */
        private LocalFileSystem fs;

        /** Pointer to local filesystem
        * @param fs the filesystem this impl is connected to 
        */
        public Impl (LocalFileSystem fs) {
            this.fs = fs;
        }


        /*
        *
        * Scans children for given name
        */
        public String[] children (String name) {
            return fs.children (name);
        }

        //
        // Change
        //

        /*
        * Creates new folder named name.
        * @param name name of folder
        * @throws IOException if operation fails
        */
        public void createFolder (String name) throws java.io.IOException {
            fs.createFolder (name);
        }


        /*
        * Create new data file.
        *
        * @param name name of the file
        *
        * @return the new data file object
        * @exception IOException if the file cannot be created (e.g. already exists)
        */
        public void createData (String name) throws IOException {
            fs.createData (name);
        }

        /*
        * Renames a file.
        *
        * @param oldName old name of the file
        * @param newName new name of the file
        */
        public void rename(String oldName, String newName) throws IOException {
            fs.rename (oldName, newName);
        }

        /*
        * Delete the file. 
        *
        * @param name name of file
        * @exception IOException if the file could not be deleted
        */
        public void delete (String name) throws IOException {
            fs.delete (name);
        }

        //
        // Info
        //

        /*
        *
        * Get last modification time.
        * @param name the file to test
        * @return the date
        */
        public java.util.Date lastModified(String name) {
            return fs.lastModified (name);
        }

        /*
        * Test if the file is folder or contains data.
        * @param name name of the file
        * @return true if the file is folder, false otherwise
        */
        public boolean folder (String name) {
            return fs.folder (name);
        }

        /*
        * Test whether this file can be written to or not.
        * @param name the file to test
        * @return <CODE>true</CODE> if file is read-only
        */
        public boolean readOnly (String name) {
            return fs.readOnly (name);
        }

        /*
        * Get the MIME type of the file.
        * Uses {@link FileUtil#getMIMEType}.
        *
        * @param name the file to test
        * @return the MIME type textual representation, e.g. <code>"text/plain"</code>
        */
        public String mimeType (String name) {
            return fs.mimeType (name);
        }

        /*
        * Get the size of the file.
        *
        * @param name the file to test
        * @return the size of the file in bytes or zero if the file does not contain data (does not
        *  exist or is a folder).
        */
        public long size (String name) {
            return fs.size (name);
        }

        /*
        * Get input stream.
        *
        * @param name the file to test
        * @return an input stream to read the contents of this file
        * @exception FileNotFoundException if the file does not exists or is invalid
        */
        public InputStream inputStream (String name) throws java.io.FileNotFoundException {
            return fs.inputStream (name);
        }

        /*
        * Get output stream.
        *
        * @param name the file to test
        * @return output stream to overwrite the contents of this file
        * @exception IOException if an error occures (the file is invalid, etc.)
        */
        public OutputStream outputStream (String name) throws java.io.IOException {
            return fs.outputStream (name);
        }

        /*
        * Does nothing to lock the file.
        *
        * @param name name of the file
        */
        public void lock (String name) throws IOException {
            fs.lock (name);
        }

        /*
        * Does nothing to unlock the file.
        *
        * @param name name of the file
        */
        public void unlock (String name) {
            fs.unlock (name);
        }

        /*
        * Does nothing to mark the file as unimportant.
        *
        * @param name the file to mark
        */
        public void markUnimportant (String name) {
            fs.markUnimportant (name);
        }
    }
    
    /** This class adds new virtual attribute "java.io.File".
     * Because of the fact that FileObjects of LocalFileSystem are convertable 
     * to java.io.File by means of attributes. */
    private static class InnerAttrs extends DefaultAttributes {
        static final long serialVersionUID = 1257351369229921993L;
        LocalFileSystem lfs;
        public InnerAttrs(LocalFileSystem lfs, AbstractFileSystem.Info info,
        AbstractFileSystem.Change change,AbstractFileSystem.List list ) {            
            super (info, change, list);
            this.lfs = lfs;
        }
        public Object readAttribute(String name, String attrName) {
            if (attrName.equals("java.io.File"))  // NOI18N
                return lfs.getFile(name);
            
            return super.readAttribute(name, attrName);
        }        
    }

}
