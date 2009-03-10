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

import java.beans.*;
import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.util.*;


import org.openide.util.actions.SystemAction;
import org.openide.util.SharedClassObject;
import org.openide.util.enums.*;

/** Implementation of <code>FileSystem</code> that simplifies the most
* common tasks. Caches information about the filesystem in
* memory and periodically refreshes its content.
* Many other operations are performed in a safer manner so as to reuse
* known experience; should be substantially simpler to subclass.
*
* @author Jaroslav Tulach
*/
public abstract class AbstractFileSystem extends FileSystem {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -3345098214331282438L;

    /** system actions for this FS if it has refreshTime != 0 */
    private static SystemAction[] SYSTEM_ACTIONS;

    /** system actions for this FS */
    private static final SystemAction[] NO_SYSTEM_ACTIONS = new SystemAction[] {
            };

    /** root object for the filesystem */
    private transient AbstractFileObject root;

    /** refresher */
    private transient RefreshRequest refresher;
    
    /** cached last value of Enumeration which holds resource name (enumeration like StringTokenizer)*/
    static transient private PathElements lastEnum;

    /** Provider of hierarchy of files. */
    protected List list;

    /** Methods for modification of files. */
    protected Change change;

    /** Methods for moving of files. This field can be left null if the filesystem
    * does not require special handling handling of FileObject.move and is satified
    * with the default implementation.
    */
    protected Transfer transfer;

    /** Methods for obtaining information about files. */
    protected Info info;

    /** Handling of attributes for files. */
    protected Attr attr;

    /**
     * Actually implements contract of FileSystem.refresh().
     */
    public void refresh (boolean expected) {
        Enumeration en = getAbstractRoot ().existingSubFiles (true);
        while (en.hasMoreElements()) {
            FileObject fo = (FileObject)en.nextElement();
            fo.refresh(expected);
        }        
    }
    
    /* Provides a name for the system that can be presented to the user.
    * @return user presentable name of the filesystem
    */
    public abstract String getDisplayName ();

    /* Getter for root folder in the filesystem.
    *
    * @return root folder of whole filesystem
    */
    public FileObject getRoot () {
        return getAbstractRoot ();
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
        if (name == null || ext == null) {
            // search for folder, return the object only if it is folder
            FileObject fo = getAbstractRoot ().find (st);
            return fo != null && fo.isFolder() ? fo : null;
        } else {
            Enumeration en = new SequenceEnumeration (
                     st,
                     new SingletonEnumeration (name + '.' + ext)
                 );
            // tries to find it (can return null)
            return getAbstractRoot ().find (en);
        }
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
            return getAbstractRoot ();
        } 
        /**Next piece of code is preformance enhancement; lastEnum = last value cache;
         PathElements is StringTokenizer wrapper that caches individual elements*/
        PathElements local = lastEnum;
        if (local == null || !local.getOriginalName().equals(name)) {
            local = new PathElements (name);
            lastEnum = local;
        }
            
        return getAbstractRoot ().find (local.getEnumeration());            
    }
    
    /** Creates Reference. In FileSystem, which subclasses AbstractFileSystem, you can overload method
    * createReference(FileObject fo) to achieve another type of Reference (weak, strong etc.)
    * @param fo is FileObject. It`s reference yourequire to get.
    * @return Reference to FileObject
    */
    protected  Reference createReference(FileObject fo){
        return(new WeakReference (fo));
    }


    /** This method allows to find Reference to resourceName
    * @param resourceName is name of resource
    * @return Reference to resourceName
    */
    protected final Reference findReference(String resourceName){
        if (resourceName.length () == 0) {
            return null;
        } else {
            StringTokenizer tok = new StringTokenizer (resourceName, "/"); // NOI18N
            return getAbstractRoot ().findRefIfExists (tok);
        }
    }

    /* 
    * @return true if RefreshAction should be enabled
    */    
    boolean isEnabledRefreshFolder () {
        return (refresher != null);
    }
    
    /* Action for this filesystem.
    *
    * @return refresh action
    */
    public SystemAction[] getActions () {
        if (!isEnabledRefreshFolder ()) {
            return NO_SYSTEM_ACTIONS;
        } else {
            if (SYSTEM_ACTIONS == null) {
                try {
                    Class c = Class.forName ("org.openide.filesystems.RefreshAction"); // NOI18N
                    RefreshAction ra = (RefreshAction) SharedClassObject.findObject (c, true);
                    // initialize the SYSTEM_ACTIONS
                    SYSTEM_ACTIONS = new SystemAction[] { ra };
                } catch (Exception ex) {
                    // ok, we are probably running in standalone mode and
                    // classes needed to initialize the RefreshAction are
                    // not available
                    SYSTEM_ACTIONS = NO_SYSTEM_ACTIONS;
                }
            }
            return SYSTEM_ACTIONS;
        }
    }

    /** Set the number of milliseconds between automatic
    * refreshes of the directory structure.
    *
    * @param ms number of milliseconds between two refreshes; if <code><= 0</code> then refreshing is disabled
    */
    protected synchronized final void setRefreshTime (int ms) {
        if (refresher != null) {
            refresher.stop ();
        }


        if (ms <= 0 || System.getProperty ("netbeans.debug.heap") != null) {
            refresher = null;
        } else {
            refresher = new RefreshRequest (this, ms);
        }
    }

    /** Get the number of milliseconds between automatic
    * refreshes of the directory structure.
    * By default, automatic refreshing is disabled.
    * @return the number of milliseconds, or <code>0</code> if refreshing is disabled
    */
    protected final int getRefreshTime () {
        RefreshRequest r = refresher;
        return r == null ? 0 : r.getRefreshTime ();
    }

    /** Instruct the filesystem
    * that the root should change.
    * A fresh root is created. Subclasses that support root changes should use this.
    *
    * @return the new root
    */
    final synchronized AbstractFileObject refreshRootImpl () {
        root = createFileObject (null, ""); // NOI18N
        return root;
    }

    /** Instruct the filesystem
    * that the root should change.
    * A fresh root is created. Subclasses that support root changes should use this.
    *
    * @return the new root
    */
    protected final FileObject refreshRoot () {
        return refreshRootImpl ();
    }
    
    /** @deprecated Just for backward compatibility, renamed during runtime to refreshRoot
     * and made available publicly
     */
    private AbstractFileObject r3fr3shRoot () {
        return refreshRootImpl ();
    }

    /** Allows subclasses to fire that a change occured in a
    * file or folder. The change can be "expected" when it is 
    * a result of an user action and the user knows that such
    * change should occur. 
    *
    * @param name resource name of the file where the change occured
    * @param expected true if the user initiated change and expects it
    */
    protected final void refreshResource (String name, boolean expected) {
        AbstractFileObject fo = (AbstractFileObject)findResourceIfExists (name);
        if (fo != null) {
            // refresh and behave like the changes is expected
            fo.refresh (null, null, true, expected);
        }
    }
    /** 
     * For the FileObject specified as parameter, returns the recursive enumeration 
     * of existing children fileobjects (both folders and data). It doesn't create
     * any new FileObject instances. Direct children are at the begining of the enumeration.
     * @param fo the starting point for the recursive fileobject search
     * @return enumeration of currently existing fileobjects.
     */
    protected final Enumeration existingFileObjects (FileObject fo) {
        AlterEnumeration en = new AlterEnumeration(existingFileObjectsWeak(fo)) {
            protected Object alter(Object obj) {
                return ((Reference) obj).get();
            }
        };
        FilterEnumeration fen = new FilterEnumeration(en) {
            protected boolean accept(Object obj) {
                return (obj != null && ((FileObject) obj).isValid ());
            }
        };
        return fen;
    }
    
    /**
     * Return Enumeration of references to FileObjects.
     */
    final Enumeration existingFileObjectsWeak (FileObject fo) {
        QueueEnumeration en = new QueueEnumeration () {
                                  public void process (Object o) {
                                      Reference ref = (Reference)o;
                                      AbstractFileObject file = (AbstractFileObject)ref.get ();
                                      if (file != null) {
                                          FileObject[] arr = file.subfiles ();
                                          Reference[] to = new Reference[arr.length];

                                          // make the array weak
                                          for (int i = 0; i < arr.length; i++) {
                                              to[i] = new WeakReference (arr[i]);
                                          }

                                          // put it into the enumeration
                                          put (to);
                                      }
                                  }
                              };
        // weak reference to root
        en.put (new WeakReference (fo));
        
        return en;
    }

    /**
    * @return if value of lastModified should be cached
    */    
    boolean isLastModifiedCacheEnabled () {
        return true;
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
    private FileObject findResourceIfExists (String name) {
        if (name.length () == 0) {
            return getAbstractRoot ();
        } else {
            StringTokenizer tok = new StringTokenizer (name, "/"); // NOI18N
            return getAbstractRoot ().findIfExists (tok);
        }
    }

    /** Hooking method to allow MultiFileSystem to be informed when a new
    * file object is created. This is the only method that creates AbstractFileObjects.
    * 
    * @param parent parent object
    * @param name of the object
    */
    AbstractFileObject createFileObject (AbstractFileObject parent, String name) {
        return new AbstractFileObject (this, parent, name);
    }

    /** Creates root object for the fs.
    */
    final AbstractFileObject getAbstractRoot () {
        if (root == null) {
            synchronized (this) {
                if (root == null) {
                    return refreshRootImpl ();
                }
            }
        }
        return root;
    }

    /** Writes the common fields and the state of refresher.
    */
    private void writeObject (ObjectOutputStream oos) throws IOException {
        ObjectOutputStream.PutField fields = oos.putFields(); 
        
        fields.put ("change", change); // NOI18N
        fields.put ("info", info); // NOI18N
        fields.put ("attr", attr); // NOI18N
        fields.put ("list", list); // NOI18N
        fields.put ("transfer", transfer); // NOI18N
        oos.writeFields();

        oos.writeInt (getRefreshTime ());
    }

    /** Reads common fields and state of refresher.
    */
    private void readObject (ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = ois.readFields();  
      
        Object o1 = readImpl ("change", fields); // change // NOI18N
        Object o2 = readImpl ("info", fields); // info // NOI18N
        Object o3 = readImpl ("attr", fields); // attr // NOI18N
        Object o4 = readImpl ("list", fields); // list // NOI18N
        Object o5 = readImpl ("transfer", fields); // transfer // NOI18N

        change = (Change)o1;
        info = (Info)o2;
        attr = (Attr)o3;
        list = (List)o4;
        transfer = (Transfer)o5;

        setRefreshTime (ois.readInt ());
    }

    //
    // Backward compatibility methods
    //

    /** Reads object from input stream, if it is
    * LocalFileSystem or JarFileSystem then replaces the object
    * by its Impl.
    */
    static Object readImpl (String name, ObjectInputStream.GetField fields)
    throws ClassNotFoundException, IOException {
        Object o = fields.get (name, null);
        if (o instanceof LocalFileSystem) {
            return new LocalFileSystem.Impl ((LocalFileSystem)o);
        } else if (o instanceof JarFileSystem) {
            return new JarFileSystem.Impl ((JarFileSystem)o);
        }
        return o;
    }

    /**
     * This method is called from AbstractFileObject.isVirtual. Tests if file 
     * really exists or is missing. Some operation on it may be restricted if returns true.
     * @param name of the file 
     * @return  true indicates that the file is missing. 
     * @since 1.9
     */    
    protected boolean checkVirtual(String name) {
        return false;
    }    

    /** Tests if this file can be written to.
     * @param name resource name
     * @return true if this file can be written, false if not.
     * @since 3.31
     */
    protected boolean canWrite(String name) {
        AbstractFileObject afo = (AbstractFileObject)this.findResource(name);
        return (afo != null) ? afo.superCanWrite () : false;
    }    

    /** Tests if this file can be read.
     * @param name resource name
     * @return true if this file can be read, false if not.
     * @since 3.31
     */
    protected boolean canRead(String name) {
        AbstractFileObject afo = (AbstractFileObject)this.findResource(name);
        return (afo != null) ? afo.superCanRead () : false;        
    }
    
    /** Mark the file as being important or unimportant.
    * @param name the file to mark
    * @param important true indicates that file is important, false conversely 
    * file is unimportant.
    * @since 1.9
    */
    protected void markImportant (String name, boolean important) {
        if (!important && info != null)
            info.markUnimportant(name);            
    }
    

    /** Provides access to the hierarchy of resources.
    */
    public interface List extends java.io.Serializable {
        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = -6242105832891012528L;

        /** Get a list of children files for a given folder.
        *
        * @param f the folder, by name; e.g. <code>top/next/afterthat</code>
        * @return a list of children of the folder, as <code>file.ext</code> (no path)
        *   the array can contain <code>null</code> values that will be ignored
        */
        public String[] children (String f);
    }

    /** Controls modification of files.
    */
    public interface Change extends java.io.Serializable {
        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = -5841597109944924596L;

        /** Create new folder.
        * @param name full name of new folder, e.g. <code>topfolder/newfolder</code>
        * @throws IOException if the operation fails
        */
        public void createFolder (String name) throws java.io.IOException;

        /** Create new data file.
        *
        * @param name full name of the file, e.g. <code>path/from/root/filename.ext</code>
        *
        * @exception IOException if the file cannot be created (e.g. already exists)
        */
        public void createData (String name) throws IOException;

        /** Rename a file.
        *
        * @param oldName old name of the file; fully qualified
        * @param newName new name of the file; fully qualified
        * @throws IOException if it could not be renamed
        */
        public void rename(String oldName, String newName) throws IOException;

        /** Delete a file.
        *
        * @param name name of file; fully qualified
        * @exception IOException if the file could not be deleted
        */
        public void delete (String name) throws IOException;
    }

    /** Controls on moving of files. This is additional interface to
    * allow filesystem that require special handling of move to implement 
    * it in different way then is the default one.
    */
    public interface Transfer extends java.io.Serializable {
        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = -8945397853892302838L;

        /** Move a file.
        *
        * @param name of the file on current filesystem
        * @param target move implementation
        * @param targetName of target file
        * @exception IOException if the move fails
        * @return false if the method is not able to handle the request and
        *    default implementation should be used instead
        */
        public boolean move (String name, Transfer target, String targetName) throws IOException;

        /** Copy a file.
        *
        * @param name of the file on current filesystem
        * @param target target transfer implementation
        * @param targetName name of target file
        * @exception IOException if the copy fails
        * @return false if the method is not able to handle the request and
        *    default implementation should be used instead
        */
        public boolean copy (String name, Transfer target, String targetName) throws IOException;
    }


    /** Information about files.
    */
    public interface Info extends java.io.Serializable {
        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = -2438286177948307985L;

        /**
        * Get last modification time.
        * @param name the file to test
        * @return the date of last modification
        */
        public java.util.Date lastModified(String name);

        /** Test if the file is a folder or contains data.
        * @param name name of the file
        * @return <code>true</code> if the file is folder, <code>false</code> if it is data
        */
        public boolean folder (String name);

        /** Test whether this file can be written to or not.
        * @param name the file to test
        * @return <CODE>true</CODE> if the file is read-only
        */
        public boolean readOnly (String name);

        /** Get the MIME type of the file. If filesystem has no special support 
        * for MIME types then can simply return null. FileSystem can register 
        * MIME types for a well-known extensions: FileUtil.setMIMEType(String ext, String mimeType)
        * or together with filesystem supply some resolvers subclassed from MIMEResolver.
        *
        * @param name the file to test
        * @return the MIME type textual representation (e.g. <code>"text/plain"</code>)
        * or null if no special support for recognizing MIME is implemented.
         */
        public String mimeType (String name);

        /** Get the size of the file.
        *
        * @param name the file to test
        * @return the size of the file in bytes, or zero if the file does not contain data (does not
        *  exist or is a folder).
        */
        public long size (String name);

        /** Get input stream.
        *
        * @param name the file to test
        * @return an input stream to read the contents of this file
        * @exception FileNotFoundException if the file does not exist or is invalid
        */
        public InputStream inputStream (String name) throws java.io.FileNotFoundException;

        /** Get output stream.
        *
        * @param name the file to test
        * @return output stream to overwrite the contents of this file
        * @exception IOException if an error occurs (the file is invalid, etc.)
        */
        public OutputStream outputStream (String name) throws java.io.IOException;

        /** Lock the file.
        * May do nothing if the underlying storage does not support locking.
        * This does not affect locking using {@link FileLock} within the IDE, however.
        * @param name name of the file
        * @throws FileAlreadyLockedException if the file is already locked
        */
        public void lock (String name) throws IOException;

        /** Unlock the file.
        * @param name name of the file
        */
        public void unlock (String name);

        /** Mark the file as being unimportant.
         * If not called, the file is assumed to be important.
         *
         * @param name the file to mark
         */
        public void markUnimportant (String name);
    }

    /** Handle attributes of files.
    */
    public interface Attr extends java.io.Serializable {
        /** @deprecated Only public by accident. */
        /* public static final */ long serialVersionUID = 5978845941846736946L;
        /** Get the file attribute with the specified name.
        * @param name the file
        * @param attrName name of the attribute
        * @return appropriate (serializable) value or <CODE>null</CODE> if the attribute is unset (or could not be properly restored for some reason)
        */
        public Object readAttribute(String name, String attrName);

        /** Set the file attribute with the specified name.
        * @param name the file
        * @param attrName name of the attribute
        * @param value new value or <code>null</code> to clear the attribute. Must be serializable, although particular filesystems may or may not use serialization to store attribute values.
        * @exception IOException if the attribute cannot be set. If serialization is used to store it, this may in fact be a subclass such as {@link NotSerializableException}.
        */
        public void writeAttribute(String name, String attrName, Object value) throws IOException;

        /** Get all file attribute names for the file.
        * @param name the file
        * @return enumeration of keys (as strings)
        */
        public Enumeration attributes(String name);

        /** Called when a file is renamed, to appropriately update its attributes.
        * @param oldName old name of the file
        * @param newName new name of the file
        */
        public void renameAttributes (String oldName, String newName);

        /** Called when a file is deleted, to also delete its attributes.
        *
        * @param name name of the file
        */
        public void deleteAttributes (String name);
    }

}
