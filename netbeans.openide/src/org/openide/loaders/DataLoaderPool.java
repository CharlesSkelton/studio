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

import java.io.*;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

import org.openide.*;
import org.openide.filesystems.*;
import org.openide.filesystems.FileSystem; // override java.io.FileSystem
import org.openide.nodes.Node;
import org.openide.nodes.FilterNode;
import org.openide.modules.ModuleInfo;
import org.openide.util.RequestProcessor;
import org.openide.util.enums.ArrayEnumeration;
import org.openide.util.enums.FilterEnumeration;
import org.openide.util.enums.SingletonEnumeration;
import org.openide.util.enums.SequenceEnumeration;
import org.openide.util.actions.SystemAction;
import org.openide.util.NbBundle;
import org.openide.util.Lookup;

/** Pool of data loaders.
 * Provides access to set of registered
 * {@link DataLoader loaders} in the system. They are used to find valid data objects
 * for given files.
 * <P>
 * The default instance can be retrieved using lookup.
 *
 * @author Jaroslav Tulach, Petr Hamernik, Dafe Simonek
 */
public abstract class DataLoaderPool extends Object
implements java.io.Serializable {
    /** SUID */
    static final long serialVersionUID=-360141823874889956L;
    /** standard system loaders. Accessed by getSystemLoaders method only */
    private static MultiFileLoader[] systemLoaders;
    /** standard default loaders. Accessed by getDefaultLoaders method only */
    private static MultiFileLoader[] defaultLoaders;
    
    /** Cache of loaders for faster toArray() method. */
    private transient DataLoader[] loaderArray;
    
    private transient EventListenerList listeners;
    
    /** prefered loader */
    private transient DataLoader preferredLoader;
    
    /** Create new loader pool.
     */
    protected DataLoaderPool () {
    }
    
    /** Create new loader pool and set preferred loader.
     * The preferred loader will be asked before any other to recognize files (also before the system
     * loader).
     *
     * @param loader the preferred loader
     */
    protected DataLoaderPool (DataLoader loader) {
        preferredLoader = loader;
    }
    
    /** Get an enumeration of data loaders.
     * Must be overridden in subclasses to provide a list of additional loaders.
     * The list should <em>not</em> include the preferred loader.
     *
     * @return enumeration of {@link DataLoader}s
     */
    protected abstract Enumeration loaders ();
    
    /** Add a new listener to the listener list. A listener is notified of
     * any change which was made to the loader pool (add, remove, or reorder).
     *
     * @param chl new listener
     */
    public final synchronized void addChangeListener (ChangeListener chl) {
        if (listeners == null) listeners = new EventListenerList();
        listeners.add( ChangeListener.class, chl);
    }
    
    /** Remove a listener from the listener list.
     *
     * @param chl listener to remove
     */
    public final synchronized void removeChangeListener (ChangeListener chl) {
        if (listeners != null) {
            listeners.remove( ChangeListener.class, chl);
        }
    }
    
    /** Fire change event to all listeners. Asynchronously.
     * @param che change event
     */
    protected final void fireChangeEvent (final ChangeEvent che) {
        loaderArray = null;
        
	Object[] list;
        synchronized( this ) {
            if (listeners == null) return;            
            list = listeners.getListenerList();
        }
        
        // could fire on given array, modifications will copy it out before
	for (int i = list.length-2; i>=0; i-=2) {
	    if (list[i] == ChangeListener.class) {
                final ChangeListener l = (ChangeListener)list[i+1];
                // separates the task to small pieces not to slow down the
                // rest of the IDE
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run () {
                        l.stateChanged(che);
                    }
                });
            }
        }
    }
    
    /** Add a listener for operations on data objects.
     * @param l the listener
     */
    public synchronized final void addOperationListener (OperationListener l) {
        if (listeners == null) listeners = new EventListenerList();
        listeners.add( OperationListener.class, l);
    }
    
    /** Remove a listener for operations on data objects.
     * @param l the listener
     */
    public synchronized final void removeOperationListener (OperationListener l) {
        if (listeners != null) {
            listeners.remove( OperationListener.class, l);
        }
    }
    
    /** Fires operation event to all listeners.
     * Clears loaderArray before firing a change.
     * @param ev event to fire
     * @param type the type of the event
     */
    final void fireOperationEvent (OperationEvent ev, int type) {
      	Object[] list;
        synchronized( this ) {
            if (listeners == null) return;            
            list = listeners.getListenerList();
        }

        // could fire on given array, modifications will copy it out before
	for (int i = list.length-2; i>=0; i-=2) {
	    if (list[i] == OperationListener.class) {
                OperationListener l = (OperationListener)list[i+1];
                switch (type) {
                    case OperationEvent.COPY:
                        l.operationCopy ((OperationEvent.Copy)ev);
                        break;
                    case OperationEvent.MOVE:
                        l.operationMove ((OperationEvent.Move)ev);
                        break;
                    case OperationEvent.DELETE:
                        l.operationDelete (ev);
                        break;
                    case OperationEvent.RENAME:
                        l.operationRename ((OperationEvent.Rename)ev);
                        break;
                    case OperationEvent.SHADOW:
                        l.operationCreateShadow ((OperationEvent.Copy)ev);
                        break;
                    case OperationEvent.TEMPL:
                        l.operationCreateFromTemplate ((OperationEvent.Copy)ev);
                        break;
                    case OperationEvent.CREATE:
                        l.operationPostCreate (ev);
                        break;
                }
            }
        }
    }
    
    /** Get an enumeration of all loaders, including the preferred and system loaders.
     * This should be the list of loaders as actually used by the system.
     * Typically it will consist of, in this order:
     * <ol>
     * <li>The preferred loader, if any.
     * <li>The system loaders, such as may be used for folders, shadows, etc.
     * <li>Module-specified loaders.
     * <li>The loader for instance data objects.
     * <li>Default loaders, which may handle files not otherwise recognizable.
     * </ol>
     * Applications should not rely on the exact contents of the pool,
     * rather the fact that this contains all the loaders which are
     * capable of recognizing files in the order in which they are
     * called.
     * @return enumeration of {@link DataLoader}s */
    public final Enumeration allLoaders () {
	if (preferredLoader == null) {
            // enumeration of systemloaders followed by normal loaders
	    return new SequenceEnumeration (
		new ArrayEnumeration ( new Enumeration[] {
    		    new ArrayEnumeration (getSystemLoaders ()),
    		    loaders (),
    		    new ArrayEnumeration (getDefaultLoaders ())
		} )
	    );
	} else {
            // enumeration of preferred loader folowed by systemloaders and normal loaders
	    return new SequenceEnumeration (
		new ArrayEnumeration ( new Enumeration[] {
        	    new SingletonEnumeration (preferredLoader),
    		    new ArrayEnumeration (getSystemLoaders ()),
    		    loaders (),
    		    new ArrayEnumeration (getDefaultLoaders ())
		} )
	    );
	}
    }
    
    /** Get an array of loaders that are currently registered.
     * Does not include special system loaders, etc.
     * @return array of loaders
     * @see #loaders
     */
    public DataLoader[] toArray () {
        DataLoader[] localArray = loaderArray;
        if (localArray != null)
            return localArray;
        ArrayList loaders = new ArrayList ();
        Enumeration en = loaders ();
        while (en.hasMoreElements ()) {
            loaders.add(en.nextElement ());
        }
        localArray = new DataLoader[loaders.size()];
        localArray = (DataLoader[])loaders.toArray(localArray);
        loaderArray = localArray;
        return localArray;
    }
    
    /** Finds the first producer of a representation class.
     * Scans through the list of all loaders and returns the first one
     * whose representation class is a superclass of <code>clazz</code>.
     *
     * @param clazz class to find producer for
     * @return data loader or <CODE>null</CODE> if there is no loader that
     *   can produce the class
     */
    public final DataLoader firstProducerOf (Class clazz) {
        Enumeration en = allLoaders ();
        while (en.hasMoreElements ()) {
            DataLoader dl = (DataLoader)en.nextElement ();
            if (dl.getRepresentationClass ().isAssignableFrom (clazz)) {
                // representation class is super class of clazz
                return dl;
            }
        }
        return null;
    }
    
    /** Get an enumeration of all producers of a representation class.
     * @see #firstProducerOf
     *
     * @param clazz class to find producers for
     * @return enumeration of {@link DataLoader}s
     */
    public final Enumeration producersOf (final Class clazz) {
        return new FilterEnumeration (allLoaders ()) {
                   /** Accepts only those loaders that produces superclass of clazz
                    */
            public boolean accept (Object o) {
                DataLoader dl = (DataLoader)o;
                return clazz.isAssignableFrom( dl.getRepresentationClass() );
            }
        };
    }
    
    
    /** private class for next method. Empty implementation of
     * DataLoaderRecognized.
     */
    private static final DataLoader.RecognizedFiles emptyDataLoaderRecognized =
    new DataLoader.RecognizedFiles () {
            /** No op. replacement.
             *
             * @param fo file object to exclude
             */
        public void markRecognized (FileObject fo) {
        }
    };
    
    /** Find a data object for this file object (not for normal users of the APIs).
     * <strong>DO NOT USE THIS</strong> as a normal user of the APIs!
     * Unless you really know what you are doing, use {@link DataObject#find} instead.
     * This call will throw an exception if it already exists, and it is normally
     * only for use by the loader infrastructure.
     * <p>All loaders are asked to recognize it according to their priority.
     * @param fo file object to recognize
     * @return the data object for this object or <CODE>null</CODE> if
     *   no loader recognizes this file
     * @exception DataObjectExistsException if the object for this primary file
     *   already exists
     * @exception IOException if the data object is recognized but
     *   an error occurs during instantiation
     * @see #findDataObject(FileObject, DataLoader.RecognizedFiles)
     */
    public DataObject findDataObject (FileObject fo) throws IOException {
        return findDataObject (fo, emptyDataLoaderRecognized);
    }
    
    /** Find a data object for this file object, considering already-recognized files (not for normal users of the APIs).
     * <strong>DO NOT USE THIS</strong> as a normal user of the APIs!
     * Unless you really know what you are doing, use {@link DataObject#find} instead.
     * This call will throw an exception if it already exists, and it is normally
     * only for use by the loader infrastructure.
     * <p>First of all looks at the
     * file extended attribute <code>NetBeansDataLoader</code>; if it is set and it
     * contains the class name of a valid {@link DataLoader}, that loader is given preference.
     * For all loaders used, the first to return non-<code>null</code> from {@link DataLoader#findDataObject}
     * is used.
     *
     * @param fo file object to recognize
     * @param r recognized files buffer
     * @return the data object for this object
     * @exception DataObjectExistsException if the object for this primary file
     *   already exists
     * @exception IOException if the data object is recognized but
     *   an error occurs during instantiation
     */
    public DataObject findDataObject (
    FileObject fo, DataLoader.RecognizedFiles r
    ) throws IOException {
        // try to find assigned loader
        DataLoader pref = getPreferredLoader (fo);
        if (pref != null) {
            DataObject obj = pref.findDataObject (fo, r);
            if (obj != null) {
                DataObjectPool.getPOOL().notifyCreation (obj);
                // file has been recognized
                return obj;
            }
        }
        
        // scan through loaders
        java.util.Enumeration en = allLoaders ();
        while (en.hasMoreElements ()) {
            DataLoader l = (DataLoader)en.nextElement ();
            DataObject obj = l.findDataObject (fo, r);
            if (obj != null) {
                // the loader recognized the file
                
                // notify it
                DataObjectPool.getPOOL().notifyCreation (obj);
                
                return obj;
            }
        }
        return null;
    }
    
    /** Utility method to mark a file as belonging to a loader.
     * When the file is to be recognized this loader will be used first.
     *
     * @param fo file to mark
     * @param loader the loader to assign to the file or null if previous
     *    association should be cleared
     * @exception IOException if setting the file's attribute failed
     */
    public static void setPreferredLoader (FileObject fo, DataLoader loader)
    throws IOException {
        DataLoader prev = getPreferredLoader (fo);
        
        if (prev == loader) {
            return;
        }
        
        if (loader == null) {
            fo.setAttribute(DataObject.EA_ASSIGNED_LOADER, null);
        } else {
            Class c = loader.getClass();
            // [PENDING] in the future a more efficient API may be introduced
            Iterator modules = Lookup.getDefault().lookup(new Lookup.Template(ModuleInfo.class)).allInstances().iterator();
            String modulename = null;
            while (modules.hasNext()) {
                ModuleInfo module = (ModuleInfo)modules.next();
                if (module.owns(c)) {
                    modulename = module.getCodeNameBase();
                    break;
                }
            }
            fo.setAttribute (DataObject.EA_ASSIGNED_LOADER, c.getName ());
            fo.setAttribute(DataObject.EA_ASSIGNED_LOADER_MODULE, modulename);
        }
        
        DataObject d = DataObjectPool.getPOOL().find (fo);
        if (d != null && d.getLoader() != loader) {
            try {
                d.setValid (false);
            } catch (java.beans.PropertyVetoException e) {
                ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
            }
        }
    }
    
    /** Get the preferred loader for a file.
     * @param fo the file to get loader from
     * @return the loader or null if there is no particular preferred loader
     */
    public static DataLoader getPreferredLoader (FileObject fo) {
        String assignedLoaderName = (String)fo.getAttribute (DataObject.EA_ASSIGNED_LOADER);
        if (assignedLoaderName != null) {
            // First check to see if it comes from an uninstalled module.
            String modulename = (String)fo.getAttribute(DataObject.EA_ASSIGNED_LOADER_MODULE);
            if (modulename != null) {
                // [PENDING] in the future a more efficient API may be introduced
                // (actually currently you can look up with a template giving the name
                // as part of the lookup item ID but this is not an official API)
                Iterator modules = Lookup.getDefault().lookup(new Lookup.Template(ModuleInfo.class)).allInstances().iterator();
                boolean ok = false;
                while (modules.hasNext()) {
                    ModuleInfo module = (ModuleInfo)modules.next();
                    if (module.getCodeNameBase().equals(modulename)) {
                        if (module.isEnabled()) {
                            // Carry on.
                            ok = true;
                            break;
                        } else {
                            // Uninstalled module.
                            return null;
                        }
                    }
                }
                if (! ok) {
                    // Unknown module.
                    return null;
                }
            } // else don't worry about it (compatibility)
            try {
                Class loaderClass = Class.forName (
                assignedLoaderName,
                true,
                (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class)
                );
                return DataLoader.getLoader(loaderClass);
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
        }
        return null;
    }
    
    
    /** Lazy getter for system loaders.
     */
    private static MultiFileLoader[] getSystemLoaders () {
        if (systemLoaders == null) {
            systemLoaders = new MultiFileLoader [] {
                (MultiFileLoader) DataLoader.getLoader(DataLoaderPool$ShadowLoader.class),
                (MultiFileLoader) DataLoader.getLoader(InstanceLoaderSystem.class)
            };
        }
        return systemLoaders;
    }
    
    /** Lazy getter for default loaders.
     */
    private static MultiFileLoader[] getDefaultLoaders () {
        if (defaultLoaders == null) {
            defaultLoaders = new MultiFileLoader [] {
                (MultiFileLoader) DataLoader.getLoader(DataLoaderPool$FolderLoader.class),
                (MultiFileLoader) DataLoader.getLoader(XMLDataObject.Loader.class),
                (MultiFileLoader) DataLoader.getLoader(DataLoaderPool$InstanceLoader.class),
                (MultiFileLoader) DataLoader.getLoader(DataLoaderPool$DefaultLoader.class)
            };
        }
        return defaultLoaders;
    }
    
    /** Getter for default file loader
     * @return the default file loader
     */
    static MultiFileLoader getDefaultFileLoader () {
        return getDefaultLoaders ()[3];
    }
    
    /** Getter for folder loader
     * @return the folder loader
     */
    static MultiFileLoader getFolderLoader () {
        return getDefaultLoaders ()[0];
    }
    
    /** Getter for shadow loader.
     */
    static MultiFileLoader getShadowLoader () {
        return getSystemLoaders ()[0];
    }
    
    //
    // Default loaders
    //

    /* Instance loader recognizing .settings files. It's placed at the beginning
     * of loader pool, .settings files must alwaus be recognized by this loader
     * otherwise IDE settings will not work at all. No module is permitted to use
     * .settings files.
     */
    private static class InstanceLoaderSystem extends DataLoaderPool$InstanceLoader {
        private static final long serialVersionUID = -935749906623354837L;
        
        /* Creates new InstanceLoader */
        public InstanceLoaderSystem() {
            super ();
        }

        protected FileObject findPrimaryFile (FileObject fo) {
            FileSystem fs = null;
            try {
                fs = fo.getFileSystem ();
            } catch (FileStateInvalidException e) {
                return null;
            }
            if (fs != Repository.getDefault ().getDefaultFileSystem ()) {
                return null;
            }
            return super.findPrimaryFile (fo);
        }

        /** @return list of all required extensions for this loader */
        protected String [] getRequiredExt () {
            return new String[] {
                InstanceDataObject.INSTANCE,
                InstanceDataObject.XML_EXT
            };
        }
    }
} // end of DataLoaderPool


/* Instance loader recognizing .ser and .instance files. It's placed at
 * the end of loader pool among default loaders.
 */
class DataLoaderPool$InstanceLoader extends UniFileLoader {
    static final long serialVersionUID =-3462727693843631328L;


    /* Creates new InstanceLoader */
    public DataLoaderPool$InstanceLoader () {
        super ("org.openide.loaders.InstanceDataObject"); // NOI18N
    }

    protected void initialize () {
        super.initialize();
        setExtensions(null);
    }

    /** Get default actions.
    * @return array of default system actions or <CODE>null</CODE> if this loader does not have any
    *   actions
    */
    protected SystemAction[] defaultActions () {
        return new SystemAction[] {
            SystemAction.get (org.openide.actions.CustomizeBeanAction.class),
            SystemAction.get (org.openide.actions.FileSystemAction.class),
            null,
            SystemAction.get(org.openide.actions.CutAction.class),
            SystemAction.get(org.openide.actions.CopyAction.class),
            SystemAction.get(org.openide.actions.PasteAction.class),
            null,
            SystemAction.get(org.openide.actions.DeleteAction.class),
            // #16278: Rename should be there. (It may or may not be enabled...)
            SystemAction.get(org.openide.actions.RenameAction.class),
            null,
            SystemAction.get (org.openide.actions.ToolsAction.class),
            SystemAction.get(org.openide.actions.PropertiesAction.class)
        };
    }

    /** Get the default display name of this loader.
    * @return default display name
    */
    protected String defaultDisplayName () {
        return NbBundle.getMessage (DataLoaderPool.class, "LBL_instance_loader_display_name");
    }

    /* Creates the right data object for given primary file.
     * It is guaranteed that the provided file is realy primary file
     * returned from the method findPrimaryFile.
     *
     * @param primaryFile the primary file
     * @return the data object for this file
     * @exception DataObjectExistsException if the primary file already has data object
     */
    protected MultiDataObject createMultiObject (FileObject primaryFile)
    throws DataObjectExistsException, java.io.IOException {
        InstanceDataObject obj = new InstanceDataObject(primaryFile, this);
        return obj;
    }

    public void writeExternal (ObjectOutput oo) throws IOException {
        // does not use super serialization of extensions
        oo.writeObject (this);

        super.writeExternal (oo);
    }

    public void readExternal (ObjectInput oi) throws IOException, ClassNotFoundException {
        // the result of following code is either ExtensionList (original version)
        // or this (current version).
        Object o = oi.readObject ();
        if (o instanceof SystemAction[]) {
            //added for compatibility with FFJ2.0
            setActions ((SystemAction[]) o);            
            setExtensions(getExtensions());
        } else if (o instanceof ExtensionList) {
            // old serialization, add new extension
            ExtensionList list = (ExtensionList)o;
            setExtensions(list);
        } else {
            // newer serialization, everything should be ok, just read
            // the original value
            super.readExternal (oi);
            setExtensions(getExtensions());
        }
    }

    /** Set the extension list for this data loader.
    * Checks if all required extensions are in new list of extensions.
    * @param ext new list of extensions
    */
    public void setExtensions(ExtensionList ext) {
        super.setExtensions(initExtensions(ext));
    }

    /** fill in instance file's extension list; if ext == null new list is created */
    private ExtensionList initExtensions(ExtensionList ext) {
        String rqext [] = getRequiredExt ();
        if (ext == null) ext = new ExtensionList();
        for (int i = 0; i < rqext.length; i++)
            ext.addExtension(rqext[i]);
        return ext;
    }
    
    /**
     * Just avoids loaders.ser, which is not a well-formed ser file and causes confusing
     * exceptions when browsing system file system.
     * Anyway reading the contents would mutate loader singletons! Evil.
     */
    protected FileObject findPrimaryFile(FileObject fo) {
        FileObject r = super.findPrimaryFile(fo);
        if (r != null && r.getPath().equals("loaders.ser")) { // NOI18N
            try {
                if (r.getFileSystem().isDefault()) {
                    // Skip it.
                    return null;
                }
            } catch (FileStateInvalidException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        return r;
    }

    /** @return list of all required extensions for this loader */
    protected String [] getRequiredExt () {
        return new String[] {
            InstanceDataObject.INSTANCE,
            InstanceDataObject.SER_EXT,
            InstanceDataObject.XML_EXT
        };
    }
} // end of DataLoaderPool$InstanceLoader



    

/** Loader for file objects not recognized by any other loader */
class DataLoaderPool$DefaultLoader extends MultiFileLoader {
    static final long serialVersionUID =-6761887227412396555L;

    /* Representation class is DefaultDataObject */
    public DataLoaderPool$DefaultLoader () {
        super ("org.openide.loaders.DefaultDataObject"); // NOI18N
        //super (DefaultDataObject.class);
    }

    /** Get default actions.
    * @return array of default system actions or <CODE>null</CODE> if this loader does not have any
    *   actions
    */
    protected SystemAction[] defaultActions () {
        return new SystemAction[] {
                SystemAction.get (org.openide.actions.FileSystemAction.class),
                null,
                SystemAction.get (org.openide.actions.CutAction.class),
                SystemAction.get (org.openide.actions.CopyAction.class),
                SystemAction.get (org.openide.actions.PasteAction.class),
                null,
                SystemAction.get (org.openide.actions.DeleteAction.class),
                SystemAction.get (org.openide.actions.RenameAction.class),
                null,
                SystemAction.get (org.openide.actions.ToolsAction.class),
                SystemAction.get (org.openide.actions.PropertiesAction.class)
            };
    }

    /** Get the default display name of this loader.
    * @return default display name
    */
    protected String defaultDisplayName () {
        return NbBundle.getMessage (DataLoaderPool.class, "LBL_default_loader_display_name");
    }

    /** Get the primary file.
     * @param fo the file to find the primary file for
     *
     * @return the primary file
     */
    protected FileObject findPrimaryFile (FileObject fo) {
        // never recognize folders
        if (fo.isFolder()) return null;
        return fo;
    }

    /* Creates the right data object for given primary file.
     * It is guaranteed that the provided file is realy primary file
     * returned from the method findPrimaryFile.
     *
     * @param primaryFile the primary file
     * @return the data object for this file
     * @exception DataObjectExistsException if the primary file already has data object
     */
    protected MultiDataObject createMultiObject (FileObject primaryFile)
    throws DataObjectExistsException, java.io.IOException {
        return new DefaultDataObject(primaryFile, this);
    }

    /* Creates the right primary entry for given primary file.
     *
     * @param obj requesting object
     * @param primaryFile primary file recognized by this loader
     * @return primary entry for that file
     */
    protected MultiDataObject.Entry createPrimaryEntry (MultiDataObject obj, FileObject primaryFile) {
        return new FileEntry (obj, primaryFile);
    }

    /** Do not create a seconday entry.
     *
     * @param obj ignored
     * @param secondaryFile ignored
     * @return never returns
     * @exception UnsupportedOperationException because this loader supports only a primary file object
     */
    protected MultiDataObject.Entry createSecondaryEntry (MultiDataObject obj, FileObject secondaryFile) {
        throw new UnsupportedOperationException ();
    }

    /** Does nothing because this loader works only with objects
     * with one file => primary file so it is not necessary to search
     * for anything else.
     *
     * @param obj the object to test
     */
    void checkFiles (MultiDataObject obj) {
    }
} // end of DataLoaderPool$DefaultLoader





/** Loader for shadows, since 1.13 changed to UniFileLoader. */
class DataLoaderPool$ShadowLoader extends UniFileLoader {
    static final long serialVersionUID =-11013405787959120L;

    /* DO NOT REMOVE THIS, the ShadowChangeAdapter must be constructed, it listens
     * on filesystems changes and converts DataShadows to BrokenDataShadows and vice versa.
     */
    private static ShadowChangeAdapter changeAdapter = new ShadowChangeAdapter();

    /* Representation class is DataShadow */
    public DataLoaderPool$ShadowLoader () {
        super ("org.openide.loaders.DataShadow"); // NOI18N
        //super (DataShadow.class);
    }

    /** Get the default display name of this loader.
    * @return default display name
    */
    protected String defaultDisplayName () {
        return NbBundle.getMessage (DataLoaderPool.class, "LBL_shadow_loader_display_name");
    }

    /** For a given file finds the primary file.
     * @param fo the (secondary) file
     *
     * @return the primary file for the file or <code>null</code> if the file is not
     *  recognized by this loader
     */
    protected FileObject findPrimaryFile(FileObject fo) {
        if (fo.hasExt (DataShadow.SHADOW_EXTENSION)) {
            return fo;
        }
        return null;
    }

    /** Creates the right primary entry for a given primary file.
     *
     * @param obj requesting object
     * @param primaryFile primary file recognized by this loader
     * @return primary entry for that file
     */
    protected MultiDataObject.Entry createPrimaryEntry(MultiDataObject obj, FileObject primaryFile) {
        return new FileEntry(obj, primaryFile);
    }

    /** Creates the right data object for a given primary file.
     * It is guaranteed that the provided file will actually be the primary file
     * returned by {@link #findPrimaryFile}.
     *
     * @param primaryFile the primary file
     * @return the data object for this file
     * @exception DataObjectExistsException if the primary file already has a data object
     */
    protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
        try {
            DataObject d = DataShadow.deserialize (primaryFile);
            if (d != null) return new DataShadow (primaryFile, d, this);
        } catch (IOException ex) {
            // broken link or damaged shadow file
        }
        /* Link is broken, create BrokenDataShadow */
        return new BrokenDataShadow (primaryFile, this);
    }
    public void writeExternal(ObjectOutput oo) throws IOException {
    }
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
    }
} // end of DataLoaderPool$ShadowLoader

/** Loader for folders, since 1.13 changed to UniFileLoader. Will be made public by patching
 */
class DataLoaderPool$FolderLoader extends UniFileLoader {
    static final long serialVersionUID =-8325525104047820255L;

    /* Representation class is DataFolder */
    public DataLoaderPool$FolderLoader () {
        super ("org.openide.loaders.DataFolder"); // NOI18N
        // super (DataFolder.class);
    }

    /** Get default actions.
    * @return array of default system actions or <CODE>null</CODE> if this loader does not have any
    *   actions
    */
    protected SystemAction[] defaultActions () {
        // #30138: try to add compiler-related actions, if they exist.
        // When Looks & new Datasystems are ready, this will no longer
        // be necessary.
        ClassLoader l = (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class);
        try {
            Class c1 = Class.forName("org.openide.actions.CompileAction", true, l);
            Class c2 = Class.forName("org.openide.actions.CompileAllAction", true, l);
            Class c3 = Class.forName("org.openide.actions.BuildAction", true, l);
            Class c4 = Class.forName("org.openide.actions.BuildAllAction", true, l);
            return new SystemAction[] {
                    SystemAction.get (org.openide.actions.OpenLocalExplorerAction.class),
                    SystemAction.get (org.openide.actions.FindAction.class),
                    SystemAction.get (org.openide.actions.FileSystemAction.class),
                    null,
                    SystemAction.get(c1),
                    SystemAction.get(c2),
                    null,
                    SystemAction.get(c3),
                    SystemAction.get(c4),
                    null,
                    SystemAction.get (org.openide.actions.CutAction.class),
                    SystemAction.get (org.openide.actions.CopyAction.class),
                    SystemAction.get (org.openide.actions.PasteAction.class),
                    null,
                    SystemAction.get (org.openide.actions.DeleteAction.class),
                    SystemAction.get (org.openide.actions.RenameAction.class),
                    null,
                    SystemAction.get (org.openide.actions.NewTemplateAction.class),
                    null,
                    SystemAction.get (org.openide.actions.ToolsAction.class),
                    SystemAction.get (org.openide.actions.PropertiesAction.class)
                };
        } catch (ClassNotFoundException cnfe) {
            // OK, platform - don't include them.
            return new SystemAction[] {
                    SystemAction.get (org.openide.actions.OpenLocalExplorerAction.class),
                    SystemAction.get (org.openide.actions.FindAction.class),
                    SystemAction.get (org.openide.actions.FileSystemAction.class),
                    null,
                    SystemAction.get (org.openide.actions.CutAction.class),
                    SystemAction.get (org.openide.actions.CopyAction.class),
                    SystemAction.get (org.openide.actions.PasteAction.class),
                    null,
                    SystemAction.get (org.openide.actions.DeleteAction.class),
                    SystemAction.get (org.openide.actions.RenameAction.class),
                    null,
                    SystemAction.get (org.openide.actions.NewTemplateAction.class),
                    null,
                    SystemAction.get (org.openide.actions.ToolsAction.class),
                    SystemAction.get (org.openide.actions.PropertiesAction.class)
                };
        }
    }

    /** Get the default display name of this loader.
    * @return default display name
    */
    protected String defaultDisplayName () {
        return NbBundle.getMessage (DataLoaderPool.class, "LBL_folder_loader_display_name");
    }

    protected FileObject findPrimaryFile(FileObject fo) {
        if (fo.isFolder()) {
            return fo;
        }
        return null;
    }

    protected MultiDataObject.Entry createPrimaryEntry(MultiDataObject obj, FileObject primaryFile) {
        return new FileEntry.Folder(obj, primaryFile);
    }

    protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
        return new DataFolder (primaryFile);
    }

    /** This method is used only in DataFolder.handleMove method.
     * For more comments see {@link org.openide.loaders.DataFolder#handleMove}.
     *
     * @param primaryFile the primary file of the datafolder to be created
     * @param original The original DataFolder. The returned MultiDataObject 
     *      delegates createNodeDelegate and getClonedNodeDelegate methods calls
     *      to the original DataFolder.
     * @return The DataFolder that shares the nodes with the original DataFolder.
     */
    MultiDataObject createMultiObject(FileObject primaryFile, final DataFolder original) throws DataObjectExistsException, IOException {
        class NodeSharingDataFolder extends DataFolder {
            public NodeSharingDataFolder(FileObject fo) throws DataObjectExistsException, IllegalArgumentException {
                super(fo);
            }
            protected Node createNodeDelegate() {
                return new FilterNode(original.getNodeDelegate());
            }
            Node getClonedNodeDelegate (DataFilter filter) {
                return new FilterNode(original.getClonedNodeDelegate(filter));
            }
        }
        return new NodeSharingDataFolder(primaryFile);
    }

    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        try {
            super.readExternal(oi);
        } catch (OptionalDataException ode) {
            // older ser of FolderLoader which did not store actions - ignore
        }
    }

} // end of DataLoaderPool$FolderLoader
