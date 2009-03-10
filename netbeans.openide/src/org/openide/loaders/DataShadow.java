/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.loaders;

import java.awt.datatransfer.Transferable;
import java.awt.Image;
import java.beans.*;
import java.io.*;
import java.text.MessageFormat;
import java.lang.reflect.*;
import java.lang.ref.*;
import java.util.*;

import org.openide.filesystems.*;
import org.openide.filesystems.FileSystem;
import org.openide.util.NbBundle;
import org.openide.nodes.*;
import org.openide.util.HelpCtx;
import org.openide.ErrorManager;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.Mutex;
import org.openide.util.MutexException;

/** Default implementation of a shortcut to another data object.
* Since 1.13 it extends MultiDataObject.
* @author Jan Jancura, Jaroslav Tulach
*/
public class DataShadow extends MultiDataObject implements DataObject.Container {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 6305590675982925167L;

    /** original data object */
    private DataObject original;
    /** Listener attached to original DataObject. */
    private OrigL origL = null;
    /** List of nodes created for the DataShadow. */
    private LinkedList nodes = new LinkedList ();

    /** Extension name. */
    static final String SHADOW_EXTENSION = "shadow"; // NOI18N
    
    /** Set of all DataShadows */
    private static Set allDataShadows;
    /** ReferenceQueue for collected DataShadows */
    private static ReferenceQueue rqueue;
    
    private static Mutex MUTEX = new Mutex ();

    private static final int IDX_FS = 0;
    private static final int IDX_PATH = 1;

    /** Getter for the Set that contains all DataShadows. */
    private static Set getDataShadowsSet() {
        if (allDataShadows == null) {
            synchronized (DataShadow.class) {
                if (allDataShadows == null) {
                    allDataShadows = new HashSet();
                }
            }
        }
        
        return allDataShadows;
    }
    
    /** Getter for the ReferenceQueue that contains WeakReferences
     * for discarded DataShadows
     */
    private static ReferenceQueue getRqueue() {
        if (rqueue == null) {
            synchronized (DataShadow.class) {
                if (rqueue == null) {
                    rqueue = new ReferenceQueue();
                }
            }
        }
        
        return rqueue;
    }
    
    /** Removes WeakReference of collected DataShadows. */
    private static void checkQueue() {
        if (rqueue == null) {
            return;
        }
        
        Reference ref = rqueue.poll();
        while (ref != null) {
           getDataShadowsSet().remove(ref);
           ref = rqueue.poll();
        }
    }
    
    /** Creates WeakReference for given DataShadow */
    static Reference createReference(Object ds, ReferenceQueue q) {
        return new DSWeakReference(ds, q);
    }
    
    private static synchronized void enqueueDataShadow(DataShadow ds) {
        checkQueue();
        getDataShadowsSet().add(createReference(ds, getRqueue()));
    }

    /** @return all active DataShadows or null */
    private static synchronized List getAllDataShadows() {
        Set allShadows = allDataShadows;
        if ((allShadows == null) || allShadows.isEmpty()) {
            return null;
        }
        
        List ret = new ArrayList(allShadows.size());
        Iterator it = allShadows.iterator();
        while (it.hasNext()) {
            Reference ref = (Reference) it.next();
            Object shadow = ref.get();
            if (shadow != null) {
                ret.add(shadow);
            }
        }
        
        return ret;
    }
    
    /** Checks whether a change of the given dataObject
     * does not hurt validity of a DataShadow
     */
    static void checkValidity(EventObject ev) {
        List all = getAllDataShadows();
        if (all == null) {
            return;
        }
        
        boolean moved = false;
        if ((ev instanceof OperationEvent.Rename)
                || (ev instanceof OperationEvent.Move))
            moved = true;
        
        int size = all.size();
        for (int i = 0; i < size; i++) {
            Object obj = all.get(i);
            ((DataShadow) obj).refresh(moved);
        }
    }
    
    /** Constructs new data shadow for given primary file and referenced original.
    * Method to allow subclasses of data shadow.
    *
    * @param fo the primary file
    * @param original original data object
    * @param loader the loader that created the object
    */
    protected DataShadow (
        FileObject fo, DataObject original, MultiFileLoader loader
    ) throws DataObjectExistsException {
        super (fo, loader);
        init(original);
    }

    /** Constructs new data shadow for given primary file and referenced original.
    * Method to allow subclasses of data shadow.
    *
    * @param fo the primary file
    * @param original original data object
    * @param loader the loader that created the object
    * @deprecated Since 1.13 do not use this constructor, it is for backward compatibility only
    */
    protected DataShadow (
        FileObject fo, DataObject original, DataLoader loader
    ) throws DataObjectExistsException {
        super (fo, loader);
        init(original);
    }
    
    /** Perform initialization after construction.
    * @param original original data object
    */
    private void init(DataObject original) {
        if (original == null)
            throw new IllegalArgumentException();
        setOriginal (original);
        enqueueDataShadow(this);
    }
    
    /** Constructs new data shadow for given primary file and referenced original.
    * @param fo the primary file
    * @param original original data object
    */
    private DataShadow (FileObject fo, DataObject original) throws DataObjectExistsException {
        this (fo, original, (MultiFileLoader)DataLoaderPool.getShadowLoader ());
    }

    /** Method that creates new data shadow in a folder. The name chosen is based
    * on the name of the original object.
    *
    * @param folder target folder to create data in
    * @param original orignal object that should be represented by the shadow
    */
    public static DataShadow create (DataFolder folder, DataObject original)
    throws IOException {
        return create (folder, null, original, SHADOW_EXTENSION);
    }

    /** Method that creates new data shadow in a folder. The default extension
    * is used.
    *
    * @param folder target folder to create data in
    * @param name name to give to the shadow
    * @param original object that should be represented by the shadow
    */
    public static DataShadow create (
        DataFolder folder,
        final String name,
        final DataObject original
    ) throws IOException {
        return create (folder, name, original, SHADOW_EXTENSION);
    }
    
    /** Method that creates new data shadow in a folder. All modifications are
    * done atomicly using {@link FileSystem#runAtomicAction}.
    *
    * @param folder target folder to create data in
    * @param name name to give to the shadow
    * @param original orignal object that should be represented by the shadow
    */
    public static DataShadow create (
        DataFolder folder,
        final String name,
        final DataObject original,
        final String ext
    ) throws IOException {
        final FileObject fo = folder.getPrimaryFile ();
        final DataShadow[] arr = new DataShadow[1];

        fo.getFileSystem ().runAtomicAction (new FileSystem.AtomicAction () {
                                                 public void run () throws IOException {
                                                     FileObject file = writeOriginal (name, ext, fo, original);
                                                     DataObject obj = DataObject.find (file);
                                                     if (obj instanceof DataShadow) {
                                                         arr[0] = (DataShadow)obj;
                                                     } else {
                                                         // wrong instance => shadow was not found
                                                         DataObjectNotFoundException dnfe = 
                                                             new DataObjectNotFoundException (obj.getPrimaryFile ());
                                                         ErrorManager errMan = ErrorManager.getDefault ();
                                                         errMan.annotate( dnfe, obj == null ? null : obj.getClass().toString());
                                                         errMan.annotate( dnfe, file == null ? null : file.getPath());
                                                         throw dnfe;
                                                     }
                                                 }
                                             });

        return arr[0];
    }
    
    /** Writes the original DataObject into file of given name and extension.
     * Both parameters {@link name} and {@link ext} are ignored when the data file
     * is passed in as a {@link trg} parameter, in that case name and link can be <code>null</code>.
     * @param name name of the file to write original DataObject in
     * @param ext extension of the file to write original DataObject in
     * @param trg folder where FileObject of given name and ext will be created or
     * file which content is replaced
     * @param obj DataObject which link is stored into
     * @return the file with link
     * @exception IOException on I/O error
     */
    private static FileObject writeOriginal (
        final String name, final String ext, final FileObject trg, final DataObject obj
    ) throws IOException {
        try {
            return (FileObject) MUTEX.writeAccess (new Mutex.ExceptionAction () {
                public Object run () throws IOException {
                    FileObject fo;
                    if (trg.isData ()) {
                        fo = trg;
                    } else {
                         String n;
                         if (name == null) {
                             n = FileUtil.findFreeFileName (trg, obj.getName (), ext);
                         } else {
                             n = name;
                         }
                         fo = trg.createData (n, ext);
                    }

                    FileLock lock = fo.lock ();
                    Writer os = new OutputStreamWriter (fo.getOutputStream (lock), "UTF-8");
                    try {
                        FileObject pf = obj.getPrimaryFile ();
                        os.write (pf.getPath());
                        os.write ('\n');
                        os.write (pf.getFileSystem ().getSystemName ());
                        os.write ('\n');
                    } finally {
                        os.close ();
                        lock.releaseLock ();
                    }
                    return fo;
                }
            });
        } catch (MutexException e) {
            throw (IOException) e.getException ();
        }
    }

    /** Loads proper dataShadow from the file fileObject.
    *
    * @param fileObject The file to deserialize shadow from.
    * @return the original <code>DataObject</code> referenced by the shadow
    * @exception IOException error during load
    */
    protected static DataObject deserialize (FileObject fileObject) throws java.io.IOException {
        String result [] = read (fileObject);
        FileObject fo = checkOriginal (result [IDX_PATH], result [IDX_FS], fileObject.getFileSystem());
        return DataObject.find (fo);
    }
    
    private static String [] read (final FileObject f) throws IOException {
        if ( f.getSize() == 0 ) {
            Object fileName = f.getAttribute ("originalFile"); // NOI18N
            if ( fileName instanceof String ) {
                
                Object fileSystemName = f.getAttribute( "originalFileSystem" ); // NOI18N
                
                if (!(fileSystemName instanceof String )) {
                    /*
                    fileSystemName = f.getFileSystem().getSystemName();
                     */
                    fileSystemName = null;
                }
                
                return new String [] { (String)fileSystemName, (String)fileName };
            }
            else {
                throw new java.io.FileNotFoundException (f.getPath());
            }
        }
        try {
            return (String []) MUTEX.readAccess (new Mutex.ExceptionAction () {
                public Object run () throws IOException {
                    BufferedReader ois = new BufferedReader (new InputStreamReader (f.getInputStream (), "UTF-8"));

                    try {
                        String s = ois.readLine ();
                        String fs = ois.readLine ();

                        if (s == null) {
                            // not found
                            throw new java.io.FileNotFoundException (f.getPath());
                        }

                        return new String [] { fs, s };
                    } finally {
                        ois.close ();
                    }
                }
            });
        } catch (MutexException e) {
            throw (IOException) e.getException ();
        }
    }

    private FileObject checkOriginal (DataObject orig) throws java.io.IOException {                
        if (orig == null)
            return null;
        return deserialize(getPrimaryFile()).getPrimaryFile();
    }
        
    /*
    static FileObject checkOriginal (String strFile, String strFS) throws java.io.IOException {                
        return checkOriginal(strFile, strFS, null);
    }
    */
    
    static FileObject checkOriginal (String strFile, String strFS, FileSystem origSystem) throws java.io.IOException {                

        Repository rep = Repository.getDefault();
        FileSystem fileSystem;
        if (strFS != null) {
            // try to locate the fs
            fileSystem = rep.findFileSystem (strFS);
        } else {
            fileSystem = origSystem;
        }

        FileObject fo;

        if (fileSystem != null) {
            // first of all try to locate the shadow by filesystem
            fo = fileSystem.findResource (strFile);
        } else {
            fo = null;
        }

        /*
        if (fo == null) {
            fo = rep.findResource (s);
        }
         */

        if (fo == null) {
            throw new java.io.FileNotFoundException (strFile);
        }

        return fo;

    }


    /** Return the original shadowed object.
    * @return the data object
    */
    public DataObject getOriginal () {
        return original;
    }
    
    /** Implementation of Container interface.
     * @return array of one element, the original
     */
    public DataObject[] getChildren () {
        return new DataObject[] { getOriginal () };
    }

    /* Creates node delegate.
    */
    protected Node createNodeDelegate () {
        return new ShadowNode (this);
    }

    /* Getter for delete action.
    * @return true if the object can be deleted
    */
    public boolean isDeleteAllowed () {
        return !getPrimaryFile ().isReadOnly ();
    }

    /* Getter for copy action.
    * @return true if the object can be copied
    */
    public boolean isCopyAllowed ()  {
        return true;
    }

    /* Getter for move action.
    * @return true if the object can be moved
    */
    public boolean isMoveAllowed ()  {
        return !getPrimaryFile ().isReadOnly ();
    }

    /* Getter for rename action.
    * @return true if the object can be renamed
    */
    public boolean isRenameAllowed () {
        return !getPrimaryFile ().isReadOnly ();
    }

    /* Help context for this object.
    * @return help context
    */
    public HelpCtx getHelpCtx () {
        return getOriginal ().getHelpCtx ();
    }

    /* Creates shadow for this object in specified folder. The current
    * implementation creates reference data shadow and pastes it into
    * specified folder.
    *
    * @param f the folder to create shortcut in
    * @return the shadow
    */
    protected DataShadow handleCreateShadow (DataFolder f) throws IOException {
        return original.handleCreateShadow (f);
    }

    /* Scans the orginal bundle */
    public Node.Cookie getCookie (Class c) {
        if (c.isInstance (this)) {
            return this;
        }
        return original.getCookie (this, c);
    }

    /* Try to refresh link to original file */
    public void refresh() {
        refresh(false);
    }
    
    private void refresh(boolean moved) {        
        try {
            /* Link isn't broken */            
            if (moved)
                tryUpdate();
            if (checkOriginal(original) != null)            
                return;                
        } catch (IOException e) {            
        }
        try {            
            /* Link is broken */
            this.setValid(false);            
        } catch (java.beans.PropertyVetoException e) {                        
        }         
    }
    
    private void tryUpdate() throws IOException {
        String result [] = read (getPrimaryFile ());
        FileObject pf = original.getPrimaryFile ();
        if (result [IDX_PATH].equals (pf.getPath())) {
            if (result[IDX_FS] == null) {
                if (getPrimaryFile().getFileSystem() == pf.getFileSystem ())
            return;
            } else {
                if (result [IDX_FS].equals (pf.getFileSystem ().getSystemName ()))
                    return;
            }
        }
        writeOriginal (null, null, getPrimaryFile (), original);
    }
    
    private void setOriginal (DataObject o) {
        if (origL == null) {
            origL = new OrigL (this);
        }

        // set new original
        if (original != null) {
            original.removePropertyChangeListener (origL);
        }

        DataObject oldOriginal = original;
        
        o.addPropertyChangeListener (origL);
        original = o;

        // update nodes
        ShadowNode n [] = null;
        synchronized (nodes) {
            n = (ShadowNode [])nodes.toArray (new ShadowNode [nodes.size ()]);
        }
        
        try {
            for (int i = 0; i < n.length; i++) {
                n[i].originalChanged ();
            }
        }
        catch (IllegalStateException e) {
            System.out.println("Please reopen the bug #18998 if you see this message."); // NOI18N
            System.out.println("Old:"+oldOriginal + // NOI18N
                ((oldOriginal == null) ? "" : (" / " + oldOriginal.isValid() + " / " + System.identityHashCode(oldOriginal)))); // NOI18N
            System.out.println("New:"+original + // NOI18N
                ((original == null) ? "" : (" / " + original.isValid() + " / " + System.identityHashCode(original)))); // NOI18N
            throw e;
        }
    }

    private static void updateShadowOriginal(final DataShadow shadow) {
        final FileObject primary = shadow.original.getPrimaryFile ();

        org.openide.util.RequestProcessor.postRequest (new Runnable () {
            public void run () {
                DataObject newOrig;

                try {
                    newOrig = DataObject.find (primary);
                } catch (DataObjectNotFoundException e) {
                    newOrig = null;
                }

                if (newOrig != null) {
                    shadow.setOriginal (newOrig);
                }
            }
        }, 100);
    }
    
    private static class OrigL implements PropertyChangeListener {
        WeakReference shadow = null;
        public OrigL (DataShadow shadow) {
            this.shadow = new WeakReference (shadow);
        }
        public void propertyChange (PropertyChangeEvent evt) {
            final DataShadow shadow = (DataShadow) this.shadow.get ();

            if (shadow != null && DataObject.PROP_VALID.equals (evt.getPropertyName ())) {
                updateShadowOriginal(shadow);
            }
        }
    }

    /** Node for a shadow object. */
    protected static class ShadowNode extends FilterNode {
        /** message to create name of node */
        private static MessageFormat format;
        /** message to create short description of node */
        private static MessageFormat descriptionFormat;
        /** if true, the DataShadow name is used instead of original's name, 
         * affects DataShadows of filesystem roots only
         */
        private static final String ATTR_USEOWNNAME = "UseOwnName"; //NOI18N

        /** shadow */
        private DataShadow obj;

        /** the sheet computed for this node or null */
        private Sheet sheet;

        /** filesystem name property of original */
        private String originalFS;
        
        /** Create a shadowing node.
         * @param shadow the shadow
         */
        public ShadowNode (DataShadow shadow) {
            this (shadow, shadow.getOriginal ().getNodeDelegate ());
        }

        /** Initializes it */
        private ShadowNode (DataShadow shadow, Node node) {
            super (node);
            this.obj = shadow;
            synchronized (this.obj.nodes) {
                this.obj.nodes.add (this);
            }
        }

        /* Clones the node
        */
        public Node cloneNode () {
            ShadowNode sn = new ShadowNode (obj);
            return sn;
        }

        /* Renames the shadow data object.
        * @param name new name for the object
        * @exception IllegalArgumentException if the rename failed
        */
        public void setName (String name) {
            try {
                if (!name.equals (obj.getName ())) {
                    obj.rename (name);
                    if (obj.original.getPrimaryFile ().isRoot ()) {
                        obj.getPrimaryFile ().setAttribute (ATTR_USEOWNNAME, Boolean.TRUE);
                    }
                    fireDisplayNameChange (null, null);
                    fireNameChange (null, null);
                }
            } catch (IOException ex) {
                throw new IllegalArgumentException (ex.getMessage ());
            }
        }

        /** The name of the shadow.
        * @return the name
        */
        public String getName () {
            return obj.getName ();
        }

        /** Lazy getter for filesystem name property of original data object
        * @return the filesystem display name of original
        */
        private String getOriginalFileSystemName () {
            if ( originalFS != null )
                return originalFS;
            else {
                try {
                    originalFS = obj.getOriginal().getPrimaryFile().getFileSystem().getDisplayName();
                } catch (FileStateInvalidException ex) {
                    originalFS = ""; // NOI18N
                }
            }
            return originalFS;
        }
        
        /* Creates name based on the original one.
        */
        public String getDisplayName () {
            if (format == null) {
                format = new MessageFormat (NbBundle.getBundle (DataShadow.class).getString ("FMT_shadowName"));
            }
            String n = format.format (createArguments ());
            try {
                obj.getPrimaryFile().getFileSystem().getStatus().annotateName(n, obj.files());
            } catch (FileStateInvalidException fsie) {
                // ignore
            }
            return n;
        }

        /** Creates arguments for given shadow node */
        private Object[] createArguments () {
            String origDisp;
            String shadowName = obj.getName ();
            if (obj.original.isValid()) {
                origDisp = obj.original.getNodeDelegate().getDisplayName();
            } else {
                // We will soon be a broken data shadow, in the meantime...
                origDisp = ""; // NOI18N
            }
            Boolean useOwnName = (Boolean)obj.getPrimaryFile ().getAttribute (ATTR_USEOWNNAME);
            if (obj.original.getPrimaryFile ().isRoot () && 
                (useOwnName == null || !useOwnName.booleanValue ())) {
                try {
                    shadowName = obj.original.getPrimaryFile ().getFileSystem ().getDisplayName ();
                } catch (FileStateInvalidException e) {
                    // ignore
                }
            }
            return new Object[] {
                       shadowName, // name of the shadow
                       super.getDisplayName (), // name of original
                       systemNameOrFileName (obj.getPrimaryFile ()), // full name of file for shadow
                       systemNameOrFileName (obj.getOriginal ().getPrimaryFile ()), // full name of original file
                       origDisp, // display name of original
                   };
        }

        /** System name of file name
        */
        private static String systemNameOrFileName (FileObject fo) {
            if (fo.isRoot ()) {
                try {
                    return fo.getFileSystem ().getDisplayName ();
                } catch (FileStateInvalidException ex) {
                }
            }
            return fo.getPath();
        }

        /* Creates description based on the original one.
        */
        public String getShortDescription () {
            if (descriptionFormat == null) {
                descriptionFormat = new MessageFormat (
                                        NbBundle.getBundle (DataShadow.class).getString ("FMT_shadowHint")
                                    );
            }
            return descriptionFormat.format (createArguments ());
        }
        
        /* Show filesystem icon if it is a root.
         */
        public Image getIcon(int type) {
            Image i = rootIcon(type);
            if (i != null) {
                return i;
            } else {
                return super.getIcon(type);
            }
        }
        public Image getOpenedIcon(int type) {
            Image i = rootIcon(type);
            if (i != null) {
                return i;
            } else {
                return super.getOpenedIcon(type);
            }
        }
        private Image rootIcon(int type) {
            FileObject orig = obj.getOriginal().getPrimaryFile();
            if (orig.isRoot()) {
                try {
                    FileSystem fs = orig.getFileSystem();
                    try {
                        Image i = Introspector.getBeanInfo(fs.getClass()).getIcon(type);
                        return fs.getStatus().annotateIcon(i, type, obj.files());
                    } catch (IntrospectionException ie) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ie);
                        // ignore
                    }
                } catch (FileStateInvalidException fsie) {
                    // ignore
                }
            }
            return null;
        }

        /* @return obj.isDeleteAllowed () */
        public boolean canDestroy () {
            return obj.isDeleteAllowed ();
        }

        /* Destroyes the node
        */
        public void destroy () throws IOException {
            synchronized (obj.nodes) {
                obj.nodes.remove (this);
            }
            obj.delete ();
            //      super.destroy ();
        }

        /** @return true if shadow can be renamed
        */
        public final boolean canRename () {
            return obj.isRenameAllowed ();
        }

        /* Returns true if this object allows copying.
        * @returns true if so
        */
        public final boolean canCopy () {
            return obj.isCopyAllowed ();
        }

        /* Returns true if this object allows cutting.
        * @returns true if so
        */
        public final boolean canCut () {
            return obj.isMoveAllowed ();
        }

        /* First of all the DataObject.getCookie method is
        * called. If it produces non-null result, it is returned.
        * Otherwise the value returned from super.getCookie
        * method is returned.
        *
        * @return the cookie or null
        */
        public Node.Cookie getCookie (Class cl) {
            Node.Cookie c = obj.getCookie (cl);
            if (c != null) {
                return c;
            } else {
                return super.getCookie (cl);
            }
        }

        /** Returns modified properties of the original node.
        * @return property sets 
        */
        public PropertySet[] getPropertySets () {
            Sheet s = sheet;
            if (s == null) {
                s = sheet = cloneSheet ();
            }
            return s.toArray ();
        }

        /** Copy this node to the clipboard.
        *
        * @return {@link org.openide.util.datatransfer.ExTransferable.Single} with one copy flavor
        * @throws IOException if it could not copy
        * @see NodeTransfer
        */
        public Transferable clipboardCopy () throws IOException {
            ExTransferable t = ExTransferable.create (super.clipboardCopy ());
            t.put (LoaderTransfer.transferable (
                obj, 
                LoaderTransfer.CLIPBOARD_COPY)
            );
            return t;
        }

        /** Cut this node to the clipboard.
        *
        * @return {@link org.openide.util.datatransfer.ExTransferable.Single} with one cut flavor
        * @throws IOException if it could not cut
        * @see NodeTransfer
        */
        public Transferable clipboardCut () throws IOException {
            ExTransferable t = ExTransferable.create (super.clipboardCut ());
            t.put (LoaderTransfer.transferable (
                obj, 
                LoaderTransfer.CLIPBOARD_CUT)
            );
            return t;
        }
        /**
        * This implementation only calls clipboardCopy supposing that 
        * copy to clipboard and copy by d'n'd are similar.
        *
        * @return transferable to represent this node during a drag
        * @exception IOException when the
        *    cut cannot be performed
        */
        public Transferable drag () throws IOException {
            return clipboardCopy ();
        }

        /** Creates a node listener that allows listening on the
        * original node and propagating events to the proxy.
        * <p>Intended for overriding by subclasses, as with {@link #createPropertyChangeListener}.
        *
        * @return a {@link org.openide.nodes.FilterNode.NodeAdapter} in the default implementation
        */
        protected org.openide.nodes.NodeListener createNodeListener () {
            return new PropL (this);
        }

        /** Equal if the o is ShadowNode to the same shadow object.
        */
        public boolean equals (Object o) {
            if (o instanceof ShadowNode) {
                ShadowNode sn = (ShadowNode)o;
                return sn.obj == obj;
            }
            return false;
        }

        /** Hashcode is computed by the represented shadow.
        */
        public int hashCode () {
            return obj.hashCode ();
        }


        /** Clones the property sheet of original node.
        */
        private Sheet cloneSheet () {
            PropertySet[] sets = this.getOriginal ().getPropertySets ();

            Sheet s = new Sheet ();
            for (int i = 0; i < sets.length; i++) {
                Sheet.Set ss = new Sheet.Set ();
                ss.put (sets[i].getProperties ());
                ss.setName (sets[i].getName ());
                ss.setDisplayName (sets[i].getDisplayName ());
                ss.setShortDescription (sets[i].getShortDescription ());

                // modifies the set if it contains name of object property
                modifySheetSet (ss);

                s.put (ss);
            }

            return s;
        }

        /** Modifies the sheet set to contain name of property and name of
        * original object.
        */
        private void modifySheetSet (Sheet.Set ss) {
            Property p = ss.remove (DataObject.PROP_NAME);
            if (p != null) {
                p = new PropertySupport.Name (this);
                ss.put (p);

                p = new Name ();
                ss.put (p);
                
                p = new FileSystemProperty ();
                ss.put (p);
            }
        }

        private void originalChanged () {
            DataObject ori = obj.getOriginal();
            if (ori.isValid()) {
                changeOriginal (ori.getNodeDelegate(), true);
            } else {
                updateShadowOriginal(obj);
            }
        }

        /** Class that renames the orginal object and also updates
        * the link
        */
        private final class Name extends PropertySupport.ReadWrite {
            public Name () {
                super (
                    "OriginalName", // NOI18N
                    String.class,
                    DataObject.getString ("PROP_ShadowOriginalName"),
                    DataObject.getString ("HINT_ShadowOriginalName")
                );
            }

            public Object getValue () {
                return obj.getOriginal ().getName();
            }

            public void setValue (Object val) throws IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
                if (!canWrite())
                    throw new IllegalAccessException();
                if (!(val instanceof String))
                    throw new IllegalArgumentException();

                try {
                    DataObject orig = obj.getOriginal ();
                    orig.rename ((String)val);
                    writeOriginal (null, null, obj.getPrimaryFile (), orig);
                } catch (IOException ex) {
                    throw new InvocationTargetException (ex);
                }
            }

            public boolean canWrite () {
                return obj.getOriginal ().isRenameAllowed();
            }
        }
        
        /** Class for original filesystem name property of broken link
        */
        private final class FileSystemProperty extends PropertySupport.ReadOnly {
            
            public FileSystemProperty () {
                super (
                    "OriginalFileSystem", // NOI18N
                    String.class,
                    DataObject.getString ("PROP_ShadowOriginalFileSystem"),
                    DataObject.getString ("HINT_ShadowOriginalFileSystem")
                );
            }

            /* Getter */
            public Object getValue () {                
                return getOriginalFileSystemName();                                
            }                        
        }

        /** Property listener on data object that delegates all changes of
        * properties to this node.
        */
        private static class PropL extends FilterNode.NodeAdapter {
            public PropL (ShadowNode sn) {
                super (sn);
            }

            protected void propertyChange (FilterNode fn, PropertyChangeEvent ev) {
              if (Node.PROP_PROPERTY_SETS.equals(ev.getPropertyName ())) {
                // clear the sheet
                ShadowNode sn = (ShadowNode)fn;
                sn.sheet = null;
              }
              
              super.propertyChange (fn, ev);
              }
        }
    }
    
    static final class DSWeakReference extends WeakReference {
        private int hash;
        
        DSWeakReference(Object o, ReferenceQueue rqueue) {
            super(o, rqueue);
            hash = o.hashCode();
        }
        
        public int hashCode() {
            return hash;
        }
        
        public boolean equals(Object o) {
            Object mine = get();
            if (mine == null) {
                return false;
            }
            
            if (o instanceof DSWeakReference) {
                DSWeakReference him = (DSWeakReference) o;
                return mine.equals(him.get());
            }
            
            return false;
        }
    }
}
