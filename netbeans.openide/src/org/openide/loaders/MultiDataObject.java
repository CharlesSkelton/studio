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

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;
import java.beans.PropertyVetoException;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.openide.*;
import org.openide.filesystems.*;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;
import org.openide.nodes.Node;
import org.openide.nodes.CookieSet;

/** Provides support for handling of data objects with multiple files.
* One file is represented by one {@link Entry}. Each handler
* has one {@link #getPrimaryEntry primary} entry and zero or more secondary entries.
*
* @author Ales Novak, Jaroslav Tulach, Ian Formanek
*/
public class MultiDataObject extends DataObject {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -7750146802134210308L;

    /** Synchronization object used in getCookieSet and setCookieSet methods.
     */
    private static final Object cookieSetLock = new Object();
    
    /** Lock used for lazy creation of secondary field (in method getSecondary()) */
    private static final Object secondaryCreationLock = new Object();
    
    /** A RequestProceccor used for firing property changes asynchronously */
    private static final RequestProcessor firingProcessor =
		new RequestProcessor( "MDO PropertyChange processor");
    
    /** A RequestProceccor used for waiting for finishing refresh */
    private static final RequestProcessor delayProcessor =
		new RequestProcessor( "MDO Firing delayer");
    /** a task waiting for the FolderList task to finish scanning of the folder */
    private RequestProcessor.Task delayedPropFilesTask;
    /** lock used in firePropFilesAfterFinishing */
    private static final Object delayedPropFilesLock = new Object();
    
    /** getPrimaryEntry() is intended to have all inetligence for copy/move/... */
    private Entry primary;

    /** Map of secondary entries and its files. (FileObject, Entry) */
    private HashMap secondary;

    /** array of cookies for this object */
    private CookieSet cookieSet;

    /** flag when to call checkFiles(this) */
    boolean checked = false;

    /** Create a MultiFileObject.
    * @see DataObject#DataObject(org.openide.filesystems.FileObject,org.openide.loaders.DataLoader)
    * @param fo the primary file object
    * @param loader loader of this data object
    */
    public MultiDataObject(FileObject fo, MultiFileLoader loader) throws DataObjectExistsException {
        super(fo, loader);        
        primary = createPrimaryEntry (this, getPrimaryFile ());
    }

    /** This constructor is added for backward compatibility, MultiDataObject should be
    * properly constructed using the MultiFileLoader.
    * @param fo the primary file object
    * @param loader loader of this data object
    * @deprecated do not use this constructor, it is for backward compatibility of 
    * {@link #DataShadow} and {@link #DataFolder} only
    * @since 1.13
    */
    MultiDataObject(FileObject fo, DataLoader loader) throws DataObjectExistsException {
        super(fo, loader);
        primary = createPrimaryEntry (this, getPrimaryFile ());
    }
    
    /** Getter for the multi file loader that created this
    * object.
    *
    * @return the multi loader for the object
    */
    public final MultiFileLoader getMultiFileLoader () {
        DataLoader loader = getLoader ();
        
        if (!(loader instanceof MultiFileLoader))
            return null;
        
        return (MultiFileLoader)loader;
    }

    /* Method to access all FileObjects used by this DataObject.
    * These file objects should have set the important flag to
    * allow the requester to distingush between important and
    * unimportant files.
    *
    * @return set of FileObjects
    */
    public Set files () {
        synchronized ( synchObjectSecondary() ) {
            removeAllInvalid ();
            return new FilesSet(getPrimaryFile(), getSecondary());
        }
    }

    /* Getter for delete action.
    * @return true if the object can be deleted
    */
    public boolean isDeleteAllowed() {
        return !getPrimaryFile ().isReadOnly () && !existReadOnlySecondary();
    }
    
    private boolean existReadOnlySecondary() {
        synchronized ( synchObjectSecondary() ) {
            Iterator it = getSecondary().keySet().iterator();
            while (it.hasNext())
                if ( ((FileObject)it.next()).isReadOnly() )
                    return true;                
        }
        return false;
    }

    /** Performs checks by calling checkFiles
     * @return getSecondary() method result
     */
    private HashMap checkSecondary () {
        // enumeration of all files
        if (! checked) {
            checkFiles (this);
            checked = true;
        }
        return getSecondary();
    }
        
    /** Lazy getter for secondary property
     * @return secondary object
     */
    private HashMap getSecondary() {
        synchronized (secondaryCreationLock) {
            if (secondary == null) {
                secondary = new HashMap (11);
            }
            return secondary;
        }
    }
    
    /* Getter for copy action.
    * @return true if the object can be copied
    */
    public boolean isCopyAllowed() {
        return true;
    }

    /* Getter for move action.
    * @return true if the object can be moved
    */
    public boolean isMoveAllowed() {
        return !getPrimaryFile ().isReadOnly () && !existReadOnlySecondary();
    }

    /* Getter for rename action.
    * @return true if the object can be renamed
    */
    public boolean isRenameAllowed () {
        return !getPrimaryFile ().isReadOnly () && !existReadOnlySecondary();
    }

    /* Help context for this object.
    * @return help context
    */
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    /** Provide object used for synchronization of methods working with 
     * Secondaries.
     * @return The private field <CODE>secondary</CODE>.
     */
    Object synchObjectSecondary() {
        Object lock = checkSecondary();
        if (lock == null) throw new IllegalStateException("checkSecondary was null from " + this); // NOI18N
        return checkSecondary();
    }
    
    /** Provides node that should represent this data object.
    *
    * @return the node representation
    * @see DataNode
    */
    protected Node createNodeDelegate () {
        DataNode dataNode = (DataNode) super.createNodeDelegate ();
        return dataNode;
    }

    /** Add a new secondary entry to the list.
    * @param fe the entry to add
    */
    protected final void addSecondaryEntry (Entry fe) {
        synchronized ( getSecondary() ) {
            getSecondary().put (fe.getFile (), fe);            
        }

        // Fire PROP_FILES only if we have actually finished making the folder.
        // It is dumb to fire this if we do not yet even know what all of our
        // initial secondary files are going to be.
        FolderList l = getFolderList();
        if (l == null) {
            firePropertyChangeLater (PROP_FILES, null, null);
        } else { // l != null
            if (l.isCreated()) {
                firePropertyChangeLater (PROP_FILES, null, null);
            } else {
                firePropFilesAfterFinishing();
            }
        }
    }

    /** Finds FolderList object for the primary file's parent folder
     * @return FolderList object or <code>null</code>
     */
    private FolderList getFolderList() {
        FileObject parent = primary.file.getParent();
        if (parent != null) {
            return FolderList.find(parent, false);
        }
        return null;
    }
    
    /** Remove a secondary entry from the list.
     * @param fe the entry to remove
    */
    protected final void removeSecondaryEntry (Entry fe) {
        synchronized (getSecondary()) {
            getSecondary().remove (fe.getFile ());
        }
        
        firePropertyChangeLater (PROP_FILES, null, null);

        if (fe.isImportant ()) {
            checkConsistency(this);
        }
    }

    /** All secondary entries are recognized. Called from multi file object.
    * @param recognized object to mark recognized file to
    */
    final void markSecondaryEntriesRecognized (DataLoader.RecognizedFiles recognized) {
        synchronized (getSecondary()) {
            Iterator it = getSecondary().keySet ().iterator ();
            while (it.hasNext ()) {
                FileObject fo=(FileObject)it.next ();
                recognized.markRecognized (fo);
            }
        }
    }


    /** Tests whether this file is between entries and if not,
    * creates a secondary entry for it and adds it into set of
    * secondary entries.
    * <P>
    * This method should be used in constructor of MultiDataObject to
    * register all the important files, that could belong to this data object.
    * As example, our XMLDataObject, tries to locate its <CODE>xmlinfo</CODE>
    * file and then do register it
    *
    * @param fo the file to register (can be null, then the action is ignored)
    * @return the entry associated to this file object (returns primary entry if the fo is null)
    */
    protected final Entry registerEntry (FileObject fo) {
        synchronized (getSecondary()) {
            if (fo == null) {
                // is it ok, to do this or somebody would like to see different behavour?
                return primary;
            }
            if (fo.equals (getPrimaryFile ())) {
                return primary;
            }

            Entry e = (Entry)getSecondary().get (fo);
            if (e != null) {
                return e;
            }

            // add it into set of entries
            e = createSecondaryEntry (this, fo);
            addSecondaryEntry (e);

            return e;
        }
    }

    /** Removes the entry from the set of secondary entries.
     * Called from the notifyFileDeleted
     */
    final void removeFile (FileObject fo) {
        synchronized (getSecondary()) {
            Entry e = (Entry)getSecondary().get (fo);
            if (e != null) {
                removeSecondaryEntry (e);
            }
        }
    }

    /** Get the primary entry.
    * @return the entry
    */
    public final Entry getPrimaryEntry () {
        return primary;
    }

    /** Get secondary entries.
    * @return immutable set of {@link Entry}s
    */
    public final Set secondaryEntries () {
        synchronized ( synchObjectSecondary() ) {
            removeAllInvalid ();

            return new HashSet (getSecondary().values ());
        }
    }

    /** For a given file, find the associated secondary entry.
    * @param fo file object
    * @return the entry associated with the file object, or <code>null</code> if there is no
    *    such entry
    */
    public final Entry findSecondaryEntry (FileObject fo) {
        Entry e;
        synchronized ( synchObjectSecondary() ) {
            e = (Entry)getSecondary().get (fo);
        }
        return e;
    }
    
    /** Removes all FileObjects that are not isValid from the
     * set of objects.
     */
    private void removeAllInvalid () {
        Iterator it = checkSecondary ().entrySet ().iterator ();
        while (it.hasNext ()) {
            Map.Entry e = (Map.Entry)it.next ();
            FileObject fo = (FileObject)e.getKey ();
            if (!fo.isValid ()) {
                it.remove ();
                firePropertyChangeLater (PROP_FILES, null, null);
            }
        }
    }


    //methods overriding DataObjectHandler's abstract methods

    /* Obtains lock for primary file by asking getPrimaryEntry() entry.
    *
    * @return the lock for primary file
    * @exception IOException if it is not possible to set the template
    *   state.
    */
    protected FileLock takePrimaryFileLock () throws IOException {
        return getPrimaryEntry ().takeLock ();
    }

    // XXX does nothing of the sort --jglick
    /** Check if in specific folder exists fileobject with the same name.
    * If it exists user is asked for confirmation to rewrite, rename or cancel operation.
    * @param folder destination folder
    * @return the suffix which should be added to the name or null if operation is cancelled
    */
    private String existInFolder(FileObject fo, FileObject folder) {
        // merge folders when neccessary
        if (fo.isFolder () && isMergingFolders ())
            return ""; // NOI18N
        
        String orig = fo.getName ();
        String name = FileUtil.findFreeFileName(
                          folder, orig, fo.getExt ()
                      );
        if (name.length () <= orig.length ()) {
            return ""; // NOI18N
        } else {
            return name.substring (orig.length ());
        }
    }

    /** Override to change default handling of name collisions detected during the
     * copy, move operations. Reasonable for MultiDataObjects having folder their
     * primary file (e.g. DataFolder, CompoundDataObject).
     * @return <code>false</code> means, that new folder name should be synthetized when
     * the same folder already exists in the target location of copy, move operation, otherwise
     * existing falder will be used. Default implementation returns <code>false</code>.
     */
    boolean isMergingFolders() {
        return false;
    }
    
    /** Copies primary and secondary files to new folder.
     * May ask for user confirmation before overwriting.
     * @param df the new folder
     * @return data object for the new primary
     * @throws IOException if there was a problem copying
     * @throws UserCancelException if the user cancelled the copy
    */
    protected DataObject handleCopy (DataFolder df) throws IOException {
        FileObject fo;

        String suffix = existInFolder(
                            getPrimaryEntry().getFile(),
                            df.getPrimaryFile ()
                        );
        if (suffix == null)
            throw new org.openide.util.UserCancelException();

        fo = getPrimaryEntry ().copy (df.getPrimaryFile (), suffix);
        Iterator it = secondaryEntries().iterator();
        while (it.hasNext ()) {
            ((Entry)it.next()).copy (df.getPrimaryFile (), suffix);
        }
        
        try {
            return createMultiObject (fo);
        } catch (DataObjectExistsException ex) {
            return ex.getDataObject ();
        }
    }

    /* Deletes all secondary entries, removes them from the set of
    * secondary entries and then deletes the getPrimaryEntry() entry.
    */
    protected void handleDelete() throws IOException {
        ArrayList toRemove = new ArrayList();
        Iterator it;
        synchronized ( synchObjectSecondary() ) {
            it = new ArrayList(getSecondary().entrySet ()).iterator();
        }
        
        while (it.hasNext ()) {
            Map.Entry e = (Map.Entry)it.next ();
            ((Entry)e.getValue ()).delete ();
            toRemove.add(e.getKey());
        }
        
        synchronized ( synchObjectSecondary() ) {
            Object[] objects = toRemove.toArray();
            for (int i = 0; i < objects.length; i++)
                getSecondary().remove(objects[i]);
        }
        
        getPrimaryEntry().delete();
    }

    /* Renames all entries and changes their files to new ones.
    */
    protected FileObject handleRename (String name) throws IOException {
        getPrimaryEntry ().changeFile (getPrimaryEntry().rename (name));

        HashMap add = null;

        ArrayList toRemove = new ArrayList();
        
        Iterator it;
        synchronized ( synchObjectSecondary() ) {
            it = new ArrayList(getSecondary().entrySet ()).iterator();
        }
        
        while (it.hasNext ()) {
            Map.Entry e = (Map.Entry)it.next ();
            FileObject fo = ((Entry)e.getValue ()).rename (name);
            if (fo == null) {
                // remove the entry
                toRemove.add (e.getKey());
            } else {
                if (!fo.equals (e.getKey ())) {
                    // put the new one into change table
                    if (add == null) add = new HashMap ();
                    Entry entry = (Entry)e.getValue ();
                    entry.changeFile (fo);
                    // using getFile to let the entry correctly annotate
                    // the file by isImportant flag
                    add.put (entry.getFile (), entry);

                    // changed the file => remove the file
                    toRemove.add(e.getKey());
                }
            }
        }

        // if there has been a change in files, apply it
        if ((add != null) || (!toRemove.isEmpty())) {
            synchronized ( synchObjectSecondary() ) {
                // remove entries
                if (!toRemove.isEmpty()) {
                    Object[] objects = toRemove.toArray();
                    for (int i = 0; i < objects.length; i++)
                        getSecondary().remove(objects[i]);
                }
                // add entries
                if (add != null) {
                    getSecondary().putAll (add);
                }
            }
            firePropertyChangeLater (PROP_FILES, null, null);
        }

        return getPrimaryEntry ().getFile ();
    }

    /** Moves primary and secondary files to a new folder.
     * May ask for user confirmation before overwriting.
     * @param df the new folder
     * @return the moved primary file object
     * @throws IOException if there was a problem moving
     * @throws UserCancelException if the user cancelled the move
    */
    protected FileObject handleMove (DataFolder df) throws IOException {
        String suffix = existInFolder(getPrimaryEntry().getFile(), df.getPrimaryFile ());
        if (suffix == null)
            throw new org.openide.util.UserCancelException();

        List backup = saveEntries();

        try {
            getPrimaryEntry ().changeFile (getPrimaryEntry ().move (df.getPrimaryFile (), suffix));

            HashMap add = null;

            ArrayList toRemove = new ArrayList();
            Iterator it;
            synchronized ( synchObjectSecondary() ) {
                it = new ArrayList(getSecondary().entrySet ()).iterator();
            }
            
            while (it.hasNext ()) {
                Map.Entry e = (Map.Entry)it.next ();
                FileObject fo = ((Entry)e.getValue ()).move (df.getPrimaryFile (), suffix);
                if (fo == null) {
                    // remove the entry
                    toRemove.add(e.getKey());
                } else {
                    if (!fo.equals (e.getKey ())) {
                        // put the new one into change table
                        if (add == null) add = new HashMap ();
                        Entry entry = (Entry)e.getValue ();
                        entry.changeFile (fo);
                        // using entry.getFile, so the file has correctly
                        // associated its isImportant flag
                        add.put (entry.getFile (), entry);

                        // changed the file => remove the file
                        toRemove.add(e.getKey());
                    }
                }
            }

            // if there has been a change in files, apply it
            if ((add != null) || (!toRemove.isEmpty())) {
                synchronized ( synchObjectSecondary() ) {
                    // remove entries
                    if (!toRemove.isEmpty()) {
                        Object[] objects = toRemove.toArray();
                        for (int i = 0; i < objects.length; i++)
                            getSecondary().remove(objects[i]);
                    }
                    // add entries
                    if (add != null) {
                        getSecondary().putAll (add);
                    }
                }
                firePropertyChangeLater (PROP_FILES, null, null);
            }

            return getPrimaryEntry ().getFile ();
        } catch (IOException e) {
            restoreEntries(backup);
            throw e;
        }
    }

    /* Creates new object from template.
    * @exception IOException
    */
    protected DataObject handleCreateFromTemplate (
        DataFolder df, String name
    ) throws IOException {
        FileObject fo;


        if (name == null) {
            name = FileUtil.findFreeFileName(
                       df.getPrimaryFile (), getPrimaryFile ().getName (), getPrimaryFile ().getExt ()
                   );
        }

        fo = getPrimaryEntry().createFromTemplate (df.getPrimaryFile (), name);
        Iterator it = secondaryEntries().iterator();
        while (it.hasNext ()) {
            ((Entry)it.next()).createFromTemplate (df.getPrimaryFile (), name);
        }
        
        try {
            return createMultiObject (fo);
        } catch (DataObjectExistsException ex) {
            return ex.getDataObject ();
        }
    }

    /** Set the set of cookies.
     * To the provided cookie set a listener is attached,
    * and any change to the set is propagated by
    * firing a change on {@link #PROP_COOKIE}.
    *
    * @param s the cookie set to use
    * @deprecated just use getCookieSet().add(...) instead
    */
    protected final void setCookieSet (CookieSet s) {
        setCookieSet(s, true);
    }

    /** Set the set of cookies.
     *
     * @param s the cookie set to use
     * @param fireChange used when called from getter. In this case event shouldn't
     * be fired.
     */
    private void setCookieSet (CookieSet s, boolean fireChange) {
        synchronized (cookieSetLock) {
            ChangeListener ch = getChangeListener();

            if (cookieSet != null) {
                cookieSet.removeChangeListener (ch);
            }

            s.addChangeListener (ch);
            cookieSet = s;
        }
        
        if (fireChange) {
            fireCookieChange ();
        }
    }
    
    /** Get the set of cookies.
     * If the set had been
    * previously set by {@link #setCookieSet}, that set
    * is returned. Otherwise an empty set is
    * returned.
    *
    * @return the cookie set (never <code>null</code>)
    */
    protected final CookieSet getCookieSet () {
        CookieSet s = cookieSet;
        if (s != null) return s;
        synchronized (cookieSetLock) {
            if (cookieSet != null) return cookieSet;

            // sets empty sheet and adds a listener to it
            setCookieSet (new CookieSet (), false);
            return cookieSet;
        }
    }

    /** Look for a cookie in the current cookie set matching the requested class.
    *
    * @param type the class to look for
    * @return an instance of that class, or <code>null</code> if this class of cookie
    *    is not supported
    */
    public Node.Cookie getCookie (Class type) {
        CookieSet c = cookieSet;
        if (c != null) {
            Node.Cookie cookie = c.getCookie (type);
            if (cookie != null) return cookie;
        }
        return super.getCookie (type);
    }

    /** Fires cookie change.
    */
    final void fireCookieChange () {
        firePropertyChange (PROP_COOKIE, null, null);
    }

    /** Fires property change but in event thread.
    */
    private void firePropertyChangeLater (
        final String name, final Object oldV, final Object newV
    ) {
        firingProcessor.post(new Runnable () {
    	    public void run () {
        	firePropertyChange (name, oldV, newV);
            }
        });
    }

    /**
     * Posts a task to delayProcessor such that task
     *   1. waits for the FolderList to finish
     *   2. calls firePropertyChangeLater with PROP_FILES
     * Second time this method is called (delayedPropFilesTask is not null)
     * the new task is not created - the old one is rescheduled to run again.
     *
     * NOTE: this method should be improved not to fire twice in some cases.
     */
    private void firePropFilesAfterFinishing() {
        synchronized (delayedPropFilesLock) {
            if (delayedPropFilesTask == null) {
                delayedPropFilesTask = delayProcessor.post(new Runnable() {
                    public void run() {
                        FolderList l = getFolderList();
                        if (l != null) {
                            l.waitProcessingFinished();
                        }
                        firePropertyChangeLater(PROP_FILES, null, null);
                    }
                });
            } else {
                delayedPropFilesTask.schedule(0);
            }
        }
    }
    
    /** sets checked to true */
    final void recognizedByFolder() {
        checked = true;
    }

    private ChangeListener chLis;

    final ChangeListener getChangeListener() {
	if (chLis == null) {
	    chLis = new ChangeListener() {
    		/** State changed */
    		public void stateChanged (ChangeEvent ev) {
        	    fireCookieChange ();
    		}
	    };
	}
        return chLis;
    }

    // -- Following methods were added in order to wrap calls to MultiFileLoader
    // and check if the loader is really of this type. This hack was added to
    // keep backward compatibility of DataFolder and DataShadow classes, which
    // were originally subclassing DataObject, but was changed to subclass
    // MultiDataObject. Methods can be removed as the deprecated constructor
    // MultiDataObject(FileObject, DataLoader) disappears.
    
    private final MultiDataObject.Entry createPrimaryEntry(MultiDataObject obj, FileObject fo) {
        MultiFileLoader loader = getMultiFileLoader ();
        
        if (loader != null)
            return loader.createPrimaryEntry (obj, fo);
        
        Entry e;
        if (fo.isFolder ())
            e = new FileEntry.Folder(obj, fo);
        else
            e = new FileEntry (obj, fo);
        
        return e;
    }

    private final MultiDataObject.Entry createSecondaryEntry(MultiDataObject obj, FileObject fo) {
        MultiFileLoader loader = getMultiFileLoader ();
        
        if (loader != null)
            return loader.createSecondaryEntryImpl (obj, fo);
        
        Entry e;
        if (fo.isFolder ())
            e = new FileEntry.Folder(obj, fo);
        else
            e = new FileEntry (obj, fo);
        
        return e;
    }

    private final MultiDataObject createMultiObject(FileObject fo) throws DataObjectExistsException, IOException {
        MultiFileLoader loader = getMultiFileLoader ();

        MultiDataObject obj;

        if (loader != null) {
            obj = loader.createMultiObject (fo);
        } else {
            obj = (MultiDataObject)getLoader ().findDataObject (fo, RECOGNIZER);
        }

        // notifies that the object has been created
        DataObjectPool.getPOOL ().notifyCreation (obj);
        
        return obj;
    }

    private final void checkConsistency (MultiDataObject obj) {
        MultiFileLoader loader = getMultiFileLoader ();

        if (loader != null)
            loader.checkConsistency (obj);
    }

    private final void checkFiles (MultiDataObject obj) {
        MultiFileLoader loader = getMultiFileLoader ();

        if (loader != null)
            loader.checkFiles (obj);
    }

    private static EmptyRecognizer RECOGNIZER = new EmptyRecognizer();
    
    private static class EmptyRecognizer implements DataLoader.RecognizedFiles {
        EmptyRecognizer() {}
        public void markRecognized (FileObject fo) {
        }
    }
    
    // End of compatibility hack. --^

    /** Save pairs Entry <-> Entry.getFile () in the list
     *  @return list of saved pairs
     */
    final List saveEntries() {
        synchronized ( synchObjectSecondary() ) {
            LinkedList ll = new LinkedList();
            Iterator it = secondaryEntries ().iterator ();

            ll.add (new Pair(getPrimaryEntry ()));
            while (it.hasNext ()) {
                MultiDataObject.Entry en = (MultiDataObject.Entry)it.next ();
                ll.add (new Pair(en));
            }
            return ll;
        }
    }
    
    /** Restore entries from the list. If Entry.getFile () has changed from
     * time when backup list was created, original file is restored and
     * Entry is re-assigned to it.
     * @param backup list obtained from {@link #saveEntries ()} function
     */
    final void restoreEntries(List backup) {
        Iterator it = backup.iterator ();
        while (it.hasNext ()) {
            Pair p = (Pair)it.next ();
            if (p.entry.getFile ().equals (p.file))
                continue;
            if (p.file.isValid()) {
                p.entry.changeFile (p.file);
            } else {
                // copy back
                try {
                    if (p.entry.getFile ().isData ())
                        p.entry.changeFile (p.entry.getFile ().copy (p.file.getParent (), p.file.getName (), p.file.getExt ()));
                    else {
                        FileObject fo = p.file.getParent ().createFolder (p.file.getName ());
                        FileUtil.copyAttributes (p.entry.getFile (), fo);
                        p.entry.changeFile (fo);
                    }
                } catch (IOException e) {
                    // should not occure
                }
            }
        }
    }
    
    private final static class Pair {
        MultiDataObject.Entry entry;
        FileObject file;

        Pair(MultiDataObject.Entry e) {
            entry = e;
            file = e.getFile ();
        }
    }
    
    /** Represents one file in a {@link MultiDataObject group data object}. */
    public abstract class Entry implements java.io.Serializable {
        /** generated Serialized Version UID */
        static final long serialVersionUID = 6024795908818133571L;

        /** modified from MultiDataObject operations, that is why it is package
        * private. Do not assign anything to this object, use changeFile method
        */
        private FileObject file;

        /** This factory is used for creating new clones of the holding lock for internal
        * use of this DataObject. It factory is null it means that the file entry is not
        */
        private transient WeakReference lock;

        protected Entry (FileObject file) {
            this.file = file;
            file.setImportant(isImportant());
        }

        /** A method to change the entry file to some else.
        * @param newFile
        */
        final void changeFile (FileObject newFile) {
            if (newFile.equals (file)) {
                return;
            }

            newFile.setImportant (isImportant ());
            this.file = newFile;
            
            // release lock for old file
            FileLock l = lock == null ? null : (FileLock)lock.get ();
            if (l != null && l.isValid ()) {
                l.releaseLock ();
            }
            lock = null;
        }

        /** Get the file this entry works with.
        */
        public final FileObject getFile () {
            return file;
        }
        
        /** Get the multi data object this entry is assigned to.
         * @return the data object
        */
        public final MultiDataObject getDataObject () {
            return MultiDataObject.this;
        }
        
        /** Method that allows to check whether an entry is important or is not.
        * Should be overriden by subclasses, the current implementation returns 
        * true.
        *
        * @return true if this entry is important or false if not
        */
        public boolean isImportant () {
            return true;
        }

        /** Called when the entry is to be copied.
        * Depending on the entry type, it should either copy the underlying <code>FileObject</code>,
        * or do nothing (if it cannot be copied).
        * @param f the folder to create this entry in
        * @param suffix the suffix to add to the name of original file
        * @return the copied <code>FileObject</code> or <code>null</code> if it cannot be copied
        * @exception IOException when the operation fails
        */
        public abstract FileObject copy (FileObject f, String suffix) throws IOException;

        /** Called when the entry is to be renamed.
        * Depending on the entry type, it should either rename the underlying <code>FileObject</code>,
        * or delete it (if it cannot be renamed).
        * @param name the new name
        * @return the renamed <code>FileObject</code> or <code>null</code> if it has been deleted
        * @exception IOException when the operation fails
        */
        public abstract FileObject rename (String name) throws IOException;

        /** Called when the entry is to be moved.
        * Depending on the entry type, it should either move the underlying <code>FileObject</code>,
        * or delete it (if it cannot be moved).
        * @param f the folder to move this entry to
        * @param suffix the suffix to use
        * @return the moved <code>FileObject</code> or <code>null</code> if it has been deleted
        * @exception IOException when the operation fails
        */
        public abstract FileObject move (FileObject f, String suffix) throws IOException;

        /** Called when the entry is to be deleted.
        * @exception IOException when the operation fails
        */
        public abstract void delete () throws IOException;

        /** Called when the entry is to be created from a template.
        * Depending on the entry type, it should either copy the underlying <code>FileObject</code>,
        * or do nothing (if it cannot be copied).
        * @param f the folder to create this entry in
        * @param name the new name to use
        * @return the copied <code>FileObject</code> or <code>null</code> if it cannot be copied
        * @exception IOException when the operation fails
        */
        public abstract FileObject createFromTemplate (FileObject f, String name) throws IOException;

        /** Try to lock this file entry.
        * @return the lock if the operation was successful; otherwise <code>null</code>
        * @throws IOException if the lock could not be taken
        */
        public FileLock takeLock() throws IOException {
            FileLock l = lock == null ? null : (FileLock)lock.get ();
            if (l == null || !l.isValid ()){
                l = getFile ().lock ();
                lock = new WeakReference (l);
            }
            return l;
        }

        /** Tests whether the entry is locked.
         * @return <code>true</code> if so
         */
        public boolean isLocked() {
            FileLock l = lock == null ? null : (FileLock)lock.get ();
            return l != null && l.isValid ();
        }

        public boolean equals(Object o) {
            if (! (o instanceof Entry)) return false;
            return getFile ().equals(((Entry) o).getFile ());
        }

        public int hashCode() {
            return getFile ().hashCode();
        }

        /** Make a Serialization replacement.
         * The entry is identified by the
        * file object is holds. When serialized, it stores the
        * file object and the data object. On deserialization
        * it finds the data object and creates the right entry
        * for it.
        */
        protected Object writeReplace () {
            return new EntryReplace (getFile ());
        }
    }    

    void notifyFileDeleted (FileEvent fe) {
        removeFile (fe.getFile ());
        if (fe.getFile ().equals (getPrimaryFile ())) {
            try {
                MultiDataObject.this.setValid (false);
            } catch (PropertyVetoException ex) {
                // silently ignore?
                ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
            }
        }
    }

    /** Fired when a file has been added to the same folder
     * @param fe the event describing context where action has taken place
     */
    void notifyFileDataCreated(FileEvent fe) {
        checked = false;
    }

    /** Entry replace.
    */
    private static final class EntryReplace extends Object implements java.io.Serializable {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -1498798537289529182L;

        /** file object of the entry */
        private FileObject file;
        /** entry to be used during read */
        private transient Entry entry;

        public EntryReplace (FileObject fo) {
            file = fo;
        }

        private void readObject (ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject ();
            try {
                DataObject obj = DataObject.find (file);
                if (obj instanceof MultiDataObject) {
                    MultiDataObject m = (MultiDataObject)obj;

                    if (file.equals (m.getPrimaryFile ())) {
                        // primary entry
                        entry = m.getPrimaryEntry ();
                    } else {
                        // secondary entry
                        Entry e = (Entry)m.findSecondaryEntry (file);
                        if (e == null) {
                            throw new InvalidObjectException (obj.toString ());
                        }
                        // remember the entry
                        entry = e;
                    }
                }
            } catch (DataObjectNotFoundException ex) {
                throw new InvalidObjectException (ex.getMessage ());
            }
        }

        public Object readResolve () {
            return entry;
        }
    }
}
