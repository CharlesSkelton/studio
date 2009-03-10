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

import java.io.IOException;
import java.util.*;

import org.openide.*;
import org.openide.filesystems.*;

/** Loader for any kind of <code>MultiDataObject</code>. It provides
* support for recognition of a composite data object and registering
* entries into it.
*
* @author Jaroslav Tulach
*/
public abstract class MultiFileLoader extends DataLoader {
    static final long serialVersionUID=1521919955690157343L;


    /** Creates new multi file loader.
     * @param representationClass the representation class
    */
    protected MultiFileLoader (Class representationClass) {
        super (representationClass);
    }

    /** Creates new multi file loader.
    * @param representationClassName the fully qualified name of the
    * representation class.
    *
    * @since 1.10
    */
    protected MultiFileLoader (String representationClassName) {
        super (representationClassName);
    }

    /*  Provides standard implementation for recognizing files in the
    * loader. First of all the findEntry method is called to allow the
    * subclass to find right entry for the
    *
    * @param fo file object to recognize
    * @param recognized recognized files buffer.
    * @exception DataObjectExistsException if the data object for specific
    *    primary file already exists (thrown by constructor of DataObject)
    * @exception IOException if the object is recognized but cannot be created
    *
    * @return suitable data object or <CODE>null</CODE> if the handler cannot
    *   recognize this object (or its group)
    */
    protected final DataObject handleFindDataObject (
        FileObject fo, RecognizedFiles recognized ) throws IOException {
        // finds primary file for given file
        FileObject primary = findPrimaryFileImpl (fo);

        // if this loader does not recognizes this file => return
        if (primary == null) return null;

        MultiDataObject obj;
        try {
            // create the multi object
            obj = createMultiObject (primary);
        } catch (DataObjectExistsException ex) {
            // object already exists
            DataObject dataObject = ex.getDataObject ();
            
            if (dataObject.getLoader () != this) {
                // try to update the data object by allowing other 
                // loaders to take care of the object
                dataObject = checkCollision (dataObject, fo);
            }
            
            if (!(dataObject instanceof MultiDataObject)) {
                // but if it is not MultiDataObject, propadate the exception
                throw ex;
            }
            obj = (MultiDataObject)dataObject;
        }

        if (obj.getLoader () != this) {
            // this primary file is recognized by a different
            // loader. We should not add entries to it
            return null;
        }

        // mark all secondary entries used
        obj.markSecondaryEntriesRecognized (recognized);

        // if the file is not between
        obj.registerEntry (fo);

        return obj;
    }


    /** For a given file finds the primary file.
    * @param fo the (secondary) file
    *
    * @return the primary file for the file or <code>null</code> if the file is not
    *   recognized by this loader
    */
    protected abstract FileObject findPrimaryFile (FileObject fo);

    /** Creates the right data object for a given primary file.
    * It is guaranteed that the provided file will actually be the primary file
    * returned by {@link #findPrimaryFile}.
    *
    * @param primaryFile the primary file
    * @return the data object for this file
    * @exception DataObjectExistsException if the primary file already has a data object
    */
    protected abstract MultiDataObject createMultiObject (FileObject primaryFile)
    throws DataObjectExistsException, IOException;

    /** Creates the right primary entry for a given primary file.
    *
    * @param obj requesting object
    * @param primaryFile primary file recognized by this loader
    * @return primary entry for that file
    */
    protected abstract MultiDataObject.Entry createPrimaryEntry (MultiDataObject obj, FileObject primaryFile);

    /** Creates a new secondary entry for a given file.
    * Note that separate entries must be created for every secondary
    * file within a given multi-file data object.
    *
    * @param obj requesting object
    * @param secondaryFile a secondary file
    * @return the entry
    */
    protected abstract MultiDataObject.Entry createSecondaryEntry (MultiDataObject obj, FileObject secondaryFile);

    /** Called when there is a collision between a data object that 
    * this loader tries to create and already existing one.
    * 
    * @param obj existing data object
    * @param file the original file that has been recognized by this loader
    *    as bellonging to obj data object
    * @return the data object created for this or null
    */
    DataObject checkCollision (DataObject obj, FileObject file) {
        /* JST: Make protected when necessary. Do not forget to
        * change UniFileDataLoader too.
        */
        FileObject primary = obj.getPrimaryFile ();
        
        /*Set refusing = */DataObjectPool.getPOOL().revalidate (
            new HashSet (Collections.singleton(primary))
        );
            // ok, the obj is discarded
        DataObject result = DataObjectPool.getPOOL().find (primary);
        return result;
    }
    
    /** Called when an entry of the data object is deleted.
    * 
    * @param obj the object to check
    */
    void checkConsistency (MultiDataObject obj) {
        /* JST: Make protected when necessary. Do not forget to
        * change UniFileDataLoader too.
        */
        FileObject primary = obj.getPrimaryFile ();
        if (primary.equals (findPrimaryFileImpl (primary))) {
            // ok recognized
            return;
        }
        
        // something is wrong the loader does not recognize the data 
        // object anymore
        try {
            obj.setValid (false);
        } catch (java.beans.PropertyVetoException ex) {
            // ignore
        }
    }
    
    
    
    /** Called before list of files belonging to a data object
    * is returned from MultiDataObject.files () method. This allows 
    * each loader to perform additional tests and update the set of
    * entries for given data object.
    * <P>
    * Current implementation scans all files in directory.
    * 
    * @param obj the object to test
    */
    void checkFiles (MultiDataObject obj) {
        /* JST: Make protected (and rename) when necessary. Do not forget to
        * change UniFileDataLoader too.
        */


        FileObject primary = obj.getPrimaryFile ();
        FileObject parent = primary.getParent ();

        FileObject[] arr = parent.getChildren ();
        for (int i = 0; i < arr.length; i++) {
            FileObject pf = findPrimaryFileImpl (arr[i]);

            if (pf == primary) {
                // this object could belong to this loader
                try {
                    // this will go thru regular process of looking for
                    // data object and register this file with the right (but not
                    // necessary this one) data object
                    DataObject.find (arr[i]);
                } catch (DataObjectNotFoundException ex) {
                    // ignore
                }
            }
        }
    }

    MultiDataObject.Entry createSecondaryEntryImpl (MultiDataObject obj, FileObject secondaryFile) {
        return createSecondaryEntry (obj, secondaryFile);
    }

    FileObject findPrimaryFileImpl (FileObject fo) {
        return findPrimaryFile (fo);
    }
}
