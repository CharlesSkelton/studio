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

import java.util.EventObject;

import org.openide.util.Lookup;
import org.openide.filesystems.FileObject;

/** Event that describes operations taken on
* a data object.
*
* @author Jaroslav Tulach
*/
public class OperationEvent extends EventObject {
    /** package private numbering of methods */
    static final int COPY = 1, MOVE = 2, DELETE = 3, RENAME = 4, SHADOW = 5, TEMPL = 6, CREATE = 7;

    /** data object */
    private DataObject obj;

    /*
    */
    static final long serialVersionUID =-3884037468317843808L;
    OperationEvent(DataObject obj) {
        super ((DataLoaderPool)Lookup.getDefault().lookup(DataLoaderPool.class));
        this.obj = obj;
    }

    /** Get the data object that has been modified.
    * @return the data object
    */
    public DataObject getObject () {
        return obj;
    }

    /** Notification of a rename of a data object.
    */
    public static final class Rename extends OperationEvent {
        /** name */
        private String name;

        static final long serialVersionUID =-1584168503454848519L;
        /** @param obj renamed object
        * @param name original name
        */
        Rename (DataObject obj, String name) {
            super (obj);
            this.name = name;
        }

        /** Get the old name of the object.
         * @return the old name
        */
        public String getOriginalName () {
            return name;
        }
    }

    /** Notification of a move of a data object.
    */
    public static final class Move extends OperationEvent {
        /** original file */
        private FileObject file;

        static final long serialVersionUID =-7753279728025703632L;
        /** @param obj renamed object
        * @param file original primary file
        */
        Move (DataObject obj, FileObject file) {
            super (obj);
            this.file = file;
        }

        /** Get the original primary file.
        * @return the file
        */
        public FileObject getOriginalPrimaryFile () {
            return file;
        }
    }

    /** Notification of a copy action of a data object, creation of a shadow,
    * or creation from a template.
    */
    public static final class Copy extends OperationEvent {
        /** original data object */
        private DataObject orig;

        static final long serialVersionUID =-2768331988864546290L;
        /** @param obj renamed object
        * @param orig original object
        */
        Copy (DataObject obj, DataObject orig) {
            super (obj);
            this.orig = orig;
        }


        /** Get the original data object.
        * @return the data object
        */
        public DataObject getOriginalDataObject () {
            return orig;
        }
    }
}
