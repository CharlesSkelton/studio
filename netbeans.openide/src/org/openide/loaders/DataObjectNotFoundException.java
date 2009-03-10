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

import org.openide.filesystems.FileObject;

/** Exception signalling that the data object for a given file object could not
* be found in {@link DataObject#find}.
*
* @author Jaroslav Tulach
*/
public class DataObjectNotFoundException extends java.io.IOException {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 1646623156535839081L;
    /** data object */
    private FileObject obj;

    /** Create a new exception.
    * @param obj the file that does not have a data object
    */
    public DataObjectNotFoundException (FileObject obj) {
        super (obj.toString ());
        this.obj = obj;
    }

    /** Get the file which does not have a data object.
     * @return the file
    */
    public FileObject getFileObject () {
        return obj;
    }
}
