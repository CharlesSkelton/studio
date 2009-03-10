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
import java.util.NoSuchElementException;

/** Abstract class that takes an enumeration and filters its elements.
* To get this class fully work one must override <CODE>accept</CODE> method.
* Objects in the enumeration must not be <CODE>null</CODE>.
*
* @author Jaroslav Tulach
*/
public class FilterEnumeration extends Object implements Enumeration {
    /** marker object stating there is no nexte element prepared */
    private static final Object EMPTY = new Object();

    /** enumeration to filter */
    private Enumeration en;

    /** element to be returned next time or {@link #EMPTY} if there is
    * no such element prepared */
    private Object next = EMPTY;
    
    /**
    * @param en enumeration to filter
    */
    public FilterEnumeration (Enumeration en) {
        this.en = en;
    }

    /** Filters objects. Overwrite this to decide which objects should be
    * included in enumeration and which not.
    * <P>
    * Default implementation accepts all non-null objects
    *
    * @param o the object to decide on
    * @return true if it should be in enumeration and false if it should not
    */
    protected boolean accept (Object o) {
        return o != null;
    }

    /** @return true if there is more elements in the enumeration
    */
    public boolean hasMoreElements () {
        if (next != EMPTY) {
            // there is a object already prepared
            return true;
        }
        while (en.hasMoreElements ()) {
            // read next
            next = en.nextElement ();
            if (accept (next)) {
                // if the object is accepted
                return true;
            };
        }
        next = EMPTY;
        return false;
    }

    /** @return next object in the enumeration
    * @exception NoSuchElementException can be thrown if there is no next object
    *   in the enumeration
    */
    public Object nextElement () {
	if( next == EMPTY && !hasMoreElements() ) {
            throw new NoSuchElementException ();
        }
        Object res = next;
        next = EMPTY;
        return res;
    }
}
