/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.loaders;

import java.beans.*;
import java.io.*;
import java.util.*;

import org.openide.ErrorManager;
import org.openide.filesystems.*;
import org.openide.nodes.NodeOp;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.SharedClassObject;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import org.openide.util.io.SafeException;

/** A data loader recognizes {@link FileObject}s and creates appropriate
* {@link DataObject}s to represent them.
* The created data object must be a subclass
* of the <EM>representation class</EM> provided in the constructor.
* <P>
* Subclasses of <code>DataLoader</code> should be made <EM>JavaBeans</EM> with
* additional parameters, so a user may configure the loaders in the loader pool.
*
* @author Jaroslav Tulach
*/
public abstract class DataLoader extends SharedClassObject {

    // XXX why is this necessary? otherwise reading loader pool now throws heavy
    // InvalidClassException's reading (abstract!) DataLoader...? --jglick
    private static final long serialVersionUID = 1986614061378346169L;

    /** property name of display name */
    public static final String PROP_DISPLAY_NAME = "displayName"; // NOI18N
    /** property name of list of actions */
    public static final String PROP_ACTIONS = "actions"; // NOI18N
    /** property name of list of default actions */
    private static final String PROP_DEF_ACTIONS = "defaultActions"; // NOI18N
    /** representation class, not public property */
    private static final Object PROP_REPRESENTATION_CLASS = new Object ();
    /** representation class name, not public property */
    private static final Object PROP_REPRESENTATION_CLASS_NAME = new Object ();

    private static final int LOADER_VERSION = 1;
    
    /** Create a new data loader.
    * Pass its representation class as a parameter to the constructor. 
    * It is recommended that representation class is superclass of all
    * DataObjects produced by the loaded, but it is not required any more.
    *
    * @param representationClass the superclass (not necessarily) of all objects 
    *    returned from {@link #findDataObject}. The class may be anything but
    *    should be chosen to be as close as possible to the actual class of objects returned from the loader,
    *    to best identify the loader's data objects to listeners.
    * @deprecated Use {@link #DataLoader(String)} instead.
    */
    protected DataLoader (Class representationClass) {
        putProperty (PROP_REPRESENTATION_CLASS, representationClass);
        putProperty (PROP_REPRESENTATION_CLASS_NAME, representationClass.getName());
        if (representationClass.getClassLoader() == getClass().getClassLoader()) {
            ErrorManager.getDefault().log(ErrorManager.WARNING, "Use of super(" + representationClass.getName() + ".class) in " + getClass().getName() + "() should be replaced with super(\"" + representationClass.getName() + "\") to reduce unnecessary class loading");
        }
    }

    /** Create a new data loader.
     * Pass its representation class name
    * as a parameter to the constructor. The constructor is then allowed
    * to return only subclasses of the representation class as the result of
    * {@link #findDataObject}.
    *
    * @param representationClassName the name of the superclass for all objects
     *   returned from
    *    {@link #findDataObject}. The class may be anything but
    *    should be chosen to be as close as possible to the actual class of objects returned from the loader,
    *    to best identify the loader's data objects to listeners.
    *
    * @since 1.10
    */
    protected DataLoader( String representationClassName ) {
        putProperty (PROP_REPRESENTATION_CLASS_NAME, representationClassName);
    }
    
    /**
     * Get the representation class for this data loader, as passed to the constructor.
     * @return the representation class
     */
    public final Class getRepresentationClass() {
        Class cls = (Class)getProperty (PROP_REPRESENTATION_CLASS);
        if (cls != null) return cls;

        String clsName = (String)getProperty (PROP_REPRESENTATION_CLASS_NAME);
        try {
            cls = Class.forName (clsName, false, getClass().getClassLoader ());
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException (cnfe.toString ());
        }
	
        putProperty (PROP_REPRESENTATION_CLASS, cls);
        return cls;
    }
    
    /**
     * Get the name of the representation class for this data loader.
     * Might avoid actually loading the class.
     * @return the class name
     * @see #getRepresentationClass
     * @since 3.25
     */
    public final String getRepresentationClassName() {
        return (String)getProperty (PROP_REPRESENTATION_CLASS_NAME);
    }

    /** Get actions.
     * These actions are used to compose
    * a popup menu for the data object. Also these actions should
    * be customizable by the user, so he can modify the popup menu on a
    * data object.
    *
    * @return array of system actions or <CODE>null</CODE> if this loader does not have any
    *   actions
    */
    public final SystemAction[] getActions () {
        SystemAction[] actions = (SystemAction[])getProperty (PROP_ACTIONS);
        if ( actions == null ) {
            actions = (SystemAction[])getProperty (PROP_DEF_ACTIONS);
            if ( actions == null ) {
                actions = defaultActions();
                putProperty (PROP_DEF_ACTIONS, actions, false);
            }        
        }
        return actions;
    }

    /** Get default actions.
    * @return array of default system actions or <CODE>null</CODE> if this loader
    * does not have any actions.
    * Typical example of usage:
    * <pre>
    * return new SystemAction[] {
    *                    SystemAction.get (OpenAction.class), ...
    *                    SystemAction.get (PropertiesAction.class)
    *                };
    * </pre>
    */
    protected SystemAction[] defaultActions () {
        SystemAction[] actions = NodeOp.getDefaultActions();
        return actions;
    }
    
    /** Set actions.
    * <p>Note that this method is public, not protected, so it is possible for anyone
    * to modify the loader's popup actions externally (after finding the loader
    * using {@link DataLoaderPool#firstProducerOf}).
    * While this is possible, anyone doing so must take care to place new actions
    * into sensible positions, including consideration of separators.
    * This may also adversely affect the intended feel of the data objects.
    * A preferable solution is generally to use {@link org.openide.actions.ToolsAction service actions}.
    * @param actions actions for this loader or <CODE>null</CODE> if it should not have any
    * @see #getActions
    */
    public final void setActions (SystemAction[] actions) {
        putProperty (PROP_ACTIONS, actions, true);
    }

    /** Get the current display name of this loader.
    * @return display name
    */
    public final String getDisplayName () {
        String dn = (String) getProperty (PROP_DISPLAY_NAME);
        if (dn != null) {
            return dn;
        } else {
            dn = defaultDisplayName();            
            if (dn != null) {
                return dn;
            } else {
                return getRepresentationClassName();
            }
        }
    }

    /** Set the display name for this loader. Only subclasses should set the name.
    * @param displayName new name
    */
    protected final void setDisplayName (final String displayName) {
        putProperty (PROP_DISPLAY_NAME, displayName, true);
    }

    /** Get the default display name of this loader.
    * @return default display name
    */
    protected String defaultDisplayName () {
        return NbBundle.getBundle(DataLoader.class).getString ("LBL_loader_display_name");
    }

    /** Find a data object appropriate to the given file object--the meat of this class.
     * <p>
    * For example: for files with the same basename but extensions <EM>.java</EM> and <EM>.class</EM>, the handler
    * should return the same <code>DataObject</code>.
    * <P>
    * The loader can add all files it has recognized into the <CODE>recognized</CODE>
    * buffer. Then all these files will be excluded from further processing.
    *
    * @param fo file object to recognize
    * @param recognized recognized file buffer
    * @exception DataObjectExistsException if the data object for the
    *    primary file already exists
    * @exception IOException if the object is recognized but cannot be created
    * @exception InvalidClassException if the class is not instance of
    *    {@link #getRepresentationClass}
    *
    * @return suitable data object or <CODE>null</CODE> if the handler cannot
    *   recognize this object (or its group)
    * @see #handleFindDataObject
    */
    public final DataObject findDataObject (
        FileObject fo, RecognizedFiles recognized
    ) throws IOException {
	try {
	    return handleFindDataObject( fo, recognized );
	} catch (IOException ioe) {
	    throw ioe;
	} catch (ThreadDeath td) {
	    throw td;
	} catch (RuntimeException e) {
	    // Some strange error, perhaps an unexpected exception in
	    // MultiFileLoader.findPrimaryFile. Such an error ought
	    // not cause whole folder recognizer to die! Assume that
	    // file/loader is kaput and continue.
	    IOException ioe = new IOException (e.toString());
	    ErrorManager.getDefault ().annotate (ioe, e);
	    throw ioe;
	}
	
	/*
	if (obj != null && !getRepresentationClass ().isInstance (obj)) {
	    // does not fullfil representation class
	    throw new java.io.InvalidClassException (obj.getClass ().toString ());
	}
	
	return obj;
	*/
    }

    /** Find a data object appropriate to the given file object (as implemented in subclasses).
     * @see #findDataObject
    * @param fo file object to recognize
    * @param recognized recognized file buffer
    * @exception DataObjectExistsException as in <code>#findDataObject</code>
    * @exception IOException as in <code>#findDataObject</code>
    *
    * @return the data object or <code>null</code>
    */
    protected abstract DataObject handleFindDataObject (
        FileObject fo, RecognizedFiles recognized
    ) throws IOException;

    /** Utility method to mark a file as belonging to this loader.
    * When the file is to be recognized this loader will be used first.
    * <P>
    * This method is used by {@link DataObject#markFiles}.
    *
    * @param fo file to mark
    * @exception IOException if setting the file's attribute failed
    */
    public final void markFile (FileObject fo) throws IOException {
        DataLoaderPool.setPreferredLoader(fo, this);
    }
    
    
    

    /** Writes nothing to the stream.
    * @param oo ignored
    */
    public void writeExternal (ObjectOutput oo) throws IOException {
        oo.writeObject( new Integer(LOADER_VERSION) );
        
        SystemAction[] arr = (SystemAction[])getProperty (PROP_ACTIONS);
        if (arr == null) {
            oo.writeObject (null);
        } else {
            // convert actions to class names
            LinkedList names = new LinkedList ();
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    names.add (null);
                } else {
                    names.add (arr[i].getClass ().getName ());
                }
            }
            oo.writeObject (names.toArray ());
        }
        
        String dn = (String) getProperty (PROP_DISPLAY_NAME);
        if ( dn == null )
            dn = ""; // NOI18N
        oo.writeUTF ( dn );        
    }

    /** Reads actions and display name from the stream.
    * @param oi input source to read from
    * @exception SafeException if some of the actions is not found in the 
    *    stream, but all the content has been read ok. Subclasses can
    *    catch this exception and continue reading from the stream
    */
    public void readExternal (ObjectInput oi)
    throws IOException, ClassNotFoundException {
        Exception main = null;
        int version = 0;        
        
        Object first = oi.readObject ();
        if ( first instanceof Integer ) {            
            version = ((Integer)first).intValue();
            first = oi.readObject ();
        }
        if (first == null || first instanceof SystemAction[]) {
            // boston version, do nothing
            // setActions ((SystemAction[])first);
        } else {
            // new version that reads the names of the actions - NB3.1
            Object[] arr = (Object[])first;
            boolean isdefault = true;
            
            SystemAction[] defactions = getActions ();

            if ( version > 0 || ( version == 0 && arr.length != defactions.length ))
                isdefault = false;
            LinkedList ll = new LinkedList ();
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    ll.add (null);
                    if ( version == 0 && isdefault && defactions[i] != null)
                        isdefault = false;
                    continue;
                }
                
                try {
                    Class c = Class.forName (
                        Utilities.translate((String)arr[i]),
                        false, // why resolve?? --jglick
                        (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class)
                    );
                    SystemAction ac = SystemAction.get (c);
                    
                    ll.add (ac);
                    if ( version == 0 && isdefault && !defactions[i].equals(ac))
                        isdefault = false;
                } catch (ClassNotFoundException ex) {
                    ErrorManager.getDefault ().annotate (
                        ex, org.openide.ErrorManager.INFORMATIONAL, 
                        null, null, null, null
                    );
                    if (main == null) {
                        main = ex;
                    } else {
                        ErrorManager.getDefault ().annotate (main, ex);
                    }
                }
            }
            if (main == null && !isdefault) {
                // Whole action list was successfully read.
                setActions ((SystemAction[])ll.toArray(new SystemAction[0]));
            } // Else do not try to override the default action list if it is incomplete anyway.
        }
        
        String displayName = oi.readUTF ();
        if ( displayName.equals("") || ( version == 0 && displayName.equals(defaultDisplayName()))) // NOI18N
            displayName = null;
        setDisplayName( displayName );
        
        if (main != null) {
            // exception occured during reading 
            SafeException se = new SafeException (main);
            // Provide a localized message explaining that there is no big problem.
            String message = NbBundle.getMessage (DataLoader.class, "EXC_missing_actions_in_loader", getDisplayName ());
            ErrorManager.getDefault ().annotate (se, message);
            throw se;
        }
    }

    protected boolean clearSharedData () {
        return false;
    }

    /** Get a registered loader from the pool.
     * @param loaderClass exact class of the loader (<em>not</em> its data object representation class)
     * @return the loader instance, or <code>null</code> if there is no such loader registered
     * @see DataLoaderPool#allLoaders
     */
    public static DataLoader getLoader (Class loaderClass) {
        return (DataLoader)findObject (loaderClass, true);
    }

    // XXX huh? --jglick
    // The parameter can be <CODE>null</CODE> to
    // simplify testing whether the file object fo is valid or not
    /** Buffer holding a list of primary and secondary files marked as already recognized, to prevent further scanning.
    */
    public interface RecognizedFiles {
        /** Mark this file as being recognized. It will be excluded
        * from further processing.
        *
        * @param fo file object to exclude
        */
        public void markRecognized (FileObject fo);
    }

}
