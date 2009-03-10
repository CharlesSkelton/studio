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

package org.openide.util.enums;

import java.util.Enumeration;

/** Abstract class that takes an enumeration and alter their elements
* to new objects.
* To get this class fully work one must override <CODE>alter</CODE> method.
* Objects in the input and resulting enumeration must not be <CODE>null</CODE>.
*
* @author Jaroslav Tulach
* @version 0.10 Apr 10, 1998
*/
public abstract class AlterEnumeration extends Object implements Enumeration {
    /** enumeration to filter */
    private Enumeration en;

    /**
    * @param en enumeration to filter
    */
    public AlterEnumeration (Enumeration en) {
        this.en = en;
    }

    /** Alters objects. Overwrite this to alter the object in the
    * enumeration by another.
    * @param o the object to decide on
    * @return new object to be placed into the output enumeration
    */
    protected abstract Object alter (Object o);

    /** @return true if there is more elements in the enumeration
    */
    public boolean hasMoreElements () {
        return en.hasMoreElements ();
    }

    /** @return next object in the enumeration
    * @exception NoSuchElementException can be thrown if there is no next object
    *   in the enumeration
    */
    public Object nextElement () {
        return alter (en.nextElement ());
    }
}
