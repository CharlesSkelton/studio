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

/** Exception signalling that the data object for this file cannot
* be created because there already is an object for the primary file.
*
* @author Jaroslav Tulach
* @version 0.10, Mar 30, 1998
*/
public class DataObjectExistsException extends java.io.IOException {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 4719319528535266801L;
    /** data object */
    private DataObject obj;

    /** Create new exception.
    * @param obj data object which already exists
    */
    public DataObjectExistsException (DataObject obj) {
        this.obj = obj;
    }

    /** Get the object which already exists.
     * @return the data object
    */
    public DataObject getDataObject () {
        //
        // we have to consult the DataObjectPool to check whether
        // the constructor of our DataObject has finished
        //
        DataObjectPool.getPOOL().waitNotified (obj);
        
        // now it should be safe to return the objects
        return obj;
    }

    /** Performance trick */
    public /*final*/ Throwable fillInStackTrace() {
        return this;
    }
}
