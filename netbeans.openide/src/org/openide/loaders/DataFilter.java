/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.loaders;


/** Allows certain data objects to be excluded from being displayed.
* @see RepositoryNodeFactory
* @author Jaroslav Tulach
*/
public interface DataFilter extends java.io.Serializable {
    /** Should the data object be displayed or not?
    * @param obj the data object
    * @return <CODE>true</CODE> if the object should be displayed,
    *    <CODE>false</CODE> otherwise
    */
    public boolean acceptDataObject (DataObject obj);

    /** Default filter that accepts everything.
    */
    /*public static final*/ DataFilter ALL = new DataFilterAll ();

    /** @deprecated Only public by accident. */
    /* public static final */ long serialVersionUID = 0L;
}
