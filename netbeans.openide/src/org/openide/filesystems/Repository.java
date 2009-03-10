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
import java.util.*;

import org.openide.util.NbBundle;
import org.openide.util.io.NbMarshalledObject;

/** This singleton object contains all {@link FileSystem}s in the IDE.
* It corresponds to the <b>Filesystems</b> tab in the Explorer, or more precisely
* to <b>Filesystems&nbsp;Settings</b> in Project Settings.
* <P>
* At any given time, no two filesystems in the pool may share the same {@link FileSystem#getSystemName name}
* (unless all but one are {@link FileSystem#isValid invalid}).
*
* <p>Use {@link #getDefault} to retrieve the default instance.
*
* <p>Note that you may construct additional instances of the Repository if you
* wish. The serialization replacer of this class specifically deals with the
* default instance as stored by the top manager; however you may safely call
* the {@link #readExternal} and {@link #writeExternal} methods directly to implement
* persistence of a non-default instance.
*
* @author Jaroslav Tulach, Petr Hamernik
*/
public class Repository extends Object implements java.io.Serializable 
{
    /** list of filesystems (FileSystem) */
    private ArrayList fileSystems;
    private transient ArrayList fileSystemsClone;

    /** the system filesystem */
    private FileSystem system;

    /** hashtable that maps system names to FileSystems */
    private Hashtable names;
    
    private transient FCLSupport fclSupport;
    

    // [PENDING] access to this hashtable is apparently not propertly synched
    // should use e.g. Collections.synchronizedSet, or just synch methods using it
    /** hashtable for listeners on changes in the filesystem.
    * Its elements are of type (RepositoryListener, RepositoryListener)
    */
    private Hashtable listeners = new Hashtable ();

    /** vetoable listener on systemName property of filesystem */
    private VetoableChangeListener vetoListener = new VetoableChangeListener () {
                /** @param ev event with changes */
                public void vetoableChange (PropertyChangeEvent ev)
                throws PropertyVetoException {
                    if (ev.getPropertyName ().equals ("systemName")) {
                        final String ov = (String)ev.getOldValue ();
                        final String nv = (String)ev.getNewValue ();
                        if (names.get (nv) != null) {
                            throw new PropertyVetoException ("system name already exists", ev) { // NOI18N
                                public String getLocalizedMessage () {
                                    return NbBundle.getMessage (Repository.class, "EXC_duplicate_system_name", ov, nv);
                                }
                            };
                        }
                    }
                }
            };

    /** property listener on systemName property of filesystem */
    private PropertyChangeListener propListener = new PropertyChangeListener () {
                /** @param ev event with changes */
                public void propertyChange (PropertyChangeEvent ev) {
                    if (ev.getPropertyName ().equals ("systemName")) {
                        // assign the property to new name
                        String ov = (String)ev.getOldValue ();
                        String nv = (String)ev.getNewValue ();
                        FileSystem fs = (FileSystem)ev.getSource ();
                        if (fs.isValid ()) {
                            // when a filesystem is valid then it is attached to a name
                            names.remove (ov);
                        }
                        // register name of the filesystem
                        names.put (nv, fs);
                        // the filesystem becomes valid
                        fs.setValid (true);
                    }
                }
            };

    static final long serialVersionUID =-6344768369160069704L;
    /** Creates new instance of filesystem pool and
    * registers it as the default one. Also registers the default filesystem.
    *
    * @param def the default filesystem
    */
    public Repository (FileSystem def) {
        this.system = def;
        init ();
    }
    
    /** Access method to get default instance of repository in the system.
     * The instance is either taken as a result of 
     * <CODE>org.openide.util.Lookup.getDefault ().lookup (Repository.class)</CODE>
     * or (if the lookup query returns null) a default instance is created.
     *
     * @return default repository for the system
     */
    public static Repository getDefault () {
        return ExternalUtil.getRepository ();
    }

    /** Initialazes the pool.
    */
    private void init () {
        // empties the pool
        fileSystems = new ArrayList ();
        names = new Hashtable ();
        addFileSystem (system);
    }

    /** Gets the default filesystem of the IDE.
    * @return the default filesystem
    */
    public final FileSystem getDefaultFileSystem () {
        return system;
    }

    /** Adds new filesystem to the pool.
    * <em>Note</em> that a filesystem cannot be assigned to more than one file
    *   system pool at one time (though currently there is only one pool anyway).
    * At any given time, no two filesystems in the pool may share the same {@link FileSystem#getSystemName name}
    * (unless all but one are {@link FileSystem#isValid invalid}). To be sure, that 
    * filesystem was really added in Repository, then test that <code>FileSystem</code>
    * is valid.
    * @param fs filesystem to add
    */
    public final void addFileSystem (FileSystem fs) {
        boolean fireIt = false;
        synchronized (this) {        
            // if the filesystem is not assigned yet
            if (!fs.assigned && !fileSystems.contains(fs)) {
                // new filesystem
                fs.setRepository (this);               
                fileSystems.add(fs);
                fileSystemsClone = (ArrayList)fileSystems.clone();
                String systemName = fs.getSystemName ();

                boolean isReg = names.get (systemName) == null;
                if (isReg && !systemName.equals ("")) { // NOI18N
                    // filesystem with the same name is not there => then it is valid
                    names.put (systemName, fs);
                    fs.setValid (true);
                } else {
                    // there is another filesystem with the same name => it is invalid
                    fs.setValid (false);
                }
                // mark the filesystem as being assigned
                fs.assigned = true;
                // mark as a listener on changes in the filesystem
                fs.addPropertyChangeListener (propListener);
                fs.addVetoableChangeListener (vetoListener);

                // notify filesystem itself that it has been added
                fs.addNotify(); 

                // fire info about new filesystem
                fireIt = true;                     
            }
        }
        // postponed firing after synchronized  block to prevent deadlock
        if (fireIt) 
            fireFileSystem (fs, true);         
    }

    /** Removes a filesystem from the pool.
    * @param fs filesystem to remove
    */
    public final void removeFileSystem (FileSystem fs) {
        boolean fireIt = false;
        synchronized (this) {        
            if (fs.isDefault()) return;
            if (fireIt = fileSystems.remove(fs)) {
                fs.setRepository (null);               

                fileSystemsClone = (ArrayList)fileSystems.clone();
                // the filesystem realy was here
                if (fs.isValid ()) {
                    // if the filesystem is valid then is in names hashtable
                    names.remove (fs.getSystemName ());
                    fs.setValid (false);
                }
                // in all cases remove it from listeners
                fs.removePropertyChangeListener (propListener);
                fs.removeVetoableChangeListener (vetoListener);

                // notify filesystem itself that it has been removed
                fs.removeNotify(); 
            }
            // unassign the filesystem
            fs.assigned = false;
        }
        // postponed firing after synchronized  block to prevent deadlock
        if (fireIt) 
            fireFileSystem (fs, false);        
    }

    /** Reorders {@link FileSystem}s by given permutation.
     * For example, if there are three filesystems, <code>new int[] {2, 0, 1}</code> cycles the filesystems forwards.
    * @param perm an array of integers
    * @throws IllegalArgumentException if the array is not a permutation, or is not the same length as the current number of filesystems in the pool
    */
    public final void reorder(int[] perm) {
        synchronized (this) {
            if (perm == null) {
                throw new IllegalArgumentException ("null permutation"); // NOI18N
            } else if (perm.length != fileSystems.size ()) {
                throw new IllegalArgumentException ("permutation is wrong size: " + perm.length + " elements but should be " + fileSystems.size ()); // NOI18N
            } else if (! isPermutation (perm)) {
                StringBuffer message = new StringBuffer ("permutation is not really a permutation:"); // NOI18N
                for (int i = 0; i < perm.length; i++) {
                    message.append (' ');
                    message.append (perm[i]);
                }
                throw new IllegalArgumentException (message.toString ());
            }

            ArrayList newList = new ArrayList (fileSystems.size ());
            int len = perm.length;
            for (int i = 0; i < len; i++) {
                newList.add (fileSystems.get (perm[i]));
            }
            fileSystems = newList;
            fileSystemsClone = (ArrayList)fileSystems.clone();            
        }
        fireFileSystemReordered(perm);
    }


    /** @return true if the parameter describes a permutation */
    private static boolean isPermutation(int[] perm) {
        final int len = perm.length;
        boolean[] bool = new boolean[len];
        try {
            for (int i = 0; i < len; i++) {
                if (bool[perm[i]]) return false;
                else bool[perm[i]] = true;
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /** Returns enumeration of all filesystems.
    * @return enumeration of type {@link FileSystem}
    */
    public final Enumeration getFileSystems () {
        ArrayList tempFileSystems = fileSystemsClone;
        return java.util.Collections.enumeration (tempFileSystems);
    }

    /** Returns enumeration of all filesystems.
    * @return enumeration of type {@link FileSystem}
    */
    public final Enumeration fileSystems () {
        return getFileSystems ();
    }


    /** Returns a sorted array of filesystems. */
    public final FileSystem[] toArray() {
        ArrayList tempFileSystems = fileSystemsClone;        

        FileSystem[] fss = new FileSystem[tempFileSystems.size()];
        tempFileSystems.toArray(fss);
        return fss;
    }

    /** Finds filesystem when only its system name is known.
    * @param systemName {@link FileSystem#getSystemName name} of the filesystem
    * @return the filesystem or <CODE>null</CODE> if there is no such
    *   filesystem
    */
    public final FileSystem findFileSystem (String systemName) {
        FileSystem fs = (FileSystem)names.get (systemName);
        return fs;
    }

    /** Saves pool to stream by saving all filesystems.
    * The default (system) filesystem, or any persistent filesystems, are skipped.
    *
    * @param oos object output stream
    * @exception IOException if an error occures
    * @deprecated Unused.
    */
    public final synchronized void writeExternal (ObjectOutput oos) throws IOException {
        Iterator iter = fileSystems.iterator();
        while (iter.hasNext()) {
            FileSystem fs = (FileSystem)iter.next();

            if (!fs.isDefault () && !fs.isPersistent ()) {
                oos.writeObject (new NbMarshalledObject (fs));
            }
        }
        oos.writeObject (null);
    }

    /** Reads object from stream.
    * Reads all filesystems. Persistent and system filesystems are untouched; all others are removed and possibly reread.
    * @param ois object input stream
    * @exception IOException if an error occures
    * @exception ClassNotFoundException if read class is not found
    * @deprecated Unused.
    */
    public final synchronized void readExternal (ObjectInput ois)
    throws IOException, ClassNotFoundException {
        ArrayList temp = new ArrayList(10);

        for (;;) {
            Object obj = ois.readObject ();
            if (obj == null) {
                // all system has been read in
                break;
            }
            FileSystem fs;
            if (obj instanceof FileSystem) {
                fs = (FileSystem)obj;
            } else {
                try {
                    NbMarshalledObject mar = (NbMarshalledObject)obj;
                    fs = (FileSystem)mar.get ();
                } catch (IOException ex) {
                    ExternalUtil.exception (ex);
                    fs = null;
                } catch (ClassNotFoundException ex) {
                    ExternalUtil.exception (ex);
                    fs = null;
                }
            }
            if (fs != null) {
                // add the new filesystem
                temp.add(fs);
            }
        }

        Enumeration ee = getFileSystems();
        FileSystem fs;
        while (ee.hasMoreElements()) {
            fs = (FileSystem) ee.nextElement();
            if (!fs.isPersistent ()) {
                removeFileSystem (fs);
            }
        }
        // in init assigned is checked and we force 'system' to be added again
        system.assigned = false;
        init ();

        // all is successfuly read
        for (Iterator iter = temp.iterator(); iter.hasNext();)
            addFileSystem ((FileSystem) iter.next());
    }



    /** Finds file when its name is provided. It scans in the list of
    * filesystems and asks them for the specified file by a call to
    * {@link FileSystem#find find}. The first object that is found is returned or <CODE>null</CODE>
    * if none of the filesystems contain such a file.
    *
    * @param aPackage package name where each package is separated by a dot
    * @param name name of the file (without dots) or <CODE>null</CODE> if
    *    one wants to obtain the name of a package and not a file in it
    * @param ext extension of the file or <CODE>null</CODE> if one needs
    *    a package and not a file name
    *
    * @return {@link FileObject} that represents file with given name or
    *   <CODE>null</CODE> if the file does not exist
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public final FileObject find (String aPackage, String name, String ext) {
        Enumeration en = getFileSystems ();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.find (aPackage, name, ext);
            if (fo != null) {
                // object found
                return fo;
            }
        }
        return null;
    }


    /** Searches for the given resource among all filesystems.
     * <p><em>Note: Do not use this method for finding classes!
     * It is a wrong usage.</em>
    * @see FileSystem#findResource
    * @param name a name of the resource
    * @return file object or <code>null</code> if the resource can not be found
    */
    public final FileObject findResource(String name) {
        Enumeration en = getFileSystems ();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.findResource(name);
            if (fo != null) {
                // object found
                return fo;
            }
        }
        return null;
    }

    /** Searches for the given resource among all filesystems, returning all matches.
     * <p><em>Note: Do not use this method for finding classes!.
     * It is a wrong usage.</em>
    * @param name name of the resource
    * @return enumeration of {@link FileObject}s
    */
    public final Enumeration findAllResources(String name) {
        Vector v = new Vector(8);
        Enumeration en = getFileSystems ();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.findResource(name);
            if (fo != null) {
                v.addElement(fo);
            }
        }
        return v.elements();
    }

    /** Finds all files among all filesystems matching a given name, returning all matches.
    * All filesystems are queried with {@link FileSystem#find}.
    *
    * @param aPackage package name where each package is separated by a dot
    * @param name name of the file (without dots) or <CODE>null</CODE> if
    *    one wants to obtain the name of a package and not a file in it
    * @param ext extension of the file or <CODE>null</CODE> if one needs
    *    a package and not a file name
    *
    * @return enumeration of {@link FileObject}s
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public final Enumeration findAll (String aPackage, String name, String ext) {
        Enumeration en = getFileSystems ();
        Vector ret = new Vector();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.find (aPackage, name, ext);
            if (fo != null) {
                ret.addElement(fo);
            }
        }
        return ret.elements();
    }

    /** Fire info about changes in the filesystem pool.
    * @param fs filesystem
    * @param add <CODE>true</CODE> if the filesystem is added,
    *   <CODE>false</CODE> if it is removed
    */
    private void fireFileSystem (FileSystem fs, boolean add) {
        Enumeration en = ((Hashtable)listeners.clone ()).elements ();
        RepositoryEvent ev = new RepositoryEvent (this, fs, add);
        while (en.hasMoreElements ()) {
            RepositoryListener list = (RepositoryListener)en.nextElement ();
            if (add) {
                list.fileSystemAdded (ev);
            } else {
                list.fileSystemRemoved (ev);
            }
        }
    }

    /** Fires info about reodering
    * @param perm
    */
    private void fireFileSystemReordered(int[] perm) {
        Enumeration en = ((Hashtable)listeners.clone ()).elements ();
        RepositoryReorderedEvent ev = new RepositoryReorderedEvent(this, perm);
        while (en.hasMoreElements ()) {
            RepositoryListener list = (RepositoryListener)en.nextElement ();
            list.fileSystemPoolReordered(ev);
        }
    }

    /** Adds new listener.
    * @param list the listener
    */
    public final void addRepositoryListener (RepositoryListener list) {
        listeners.put (list, list);
    }

    /** Removes listener.
    * @param list the listener
    */
    public final void removeRepositoryListener (RepositoryListener list) {
        listeners.remove (list);
    }

    /** Writes the object to the stream.
    */
    private Object writeReplace () {
        return new Replacer ();
    }

    final FCLSupport getFCLSupport() {
        synchronized (FCLSupport.class) {
            if (fclSupport == null)
                fclSupport = new FCLSupport ();
        }        
        return fclSupport;
    }
    
    /** Add new listener to this object.
    * @param fcl the listener
    * @since 2.8    
    */        
    public final void addFileChangeListener(FileChangeListener fcl) {
        getFCLSupport ().addFileChangeListener(fcl);
    }

    /** Remove listener from this object.
    * @param fcl the listener
    * @since 2.8    
    */        
    public final void removeFileChangeListener(FileChangeListener fcl) {
        getFCLSupport ().removeFileChangeListener(fcl);        
    }
    
    private static class Replacer implements java.io.Serializable {
        /** serial version UID */
        static final long serialVersionUID=-3814531276726840241L;
        
        Replacer() {}

        private void writeObject (ObjectOutputStream oos) throws IOException {
            ExternalUtil.getRepository ().writeExternal (oos);
        }

        private void readObject (ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
            ExternalUtil.getRepository ().readExternal (ois);
        }

        /** @return the default pool */
        public Object readResolve () {
            return ExternalUtil.getRepository ();
        }
    }
}
