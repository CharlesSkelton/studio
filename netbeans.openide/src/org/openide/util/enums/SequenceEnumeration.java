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

/** Composes more enumerations into one.
*
* @author Jaroslav Tulach
* @author Petr Nejedly
* @version 0.20
*/
public class SequenceEnumeration extends Object implements Enumeration {
    /** enumeration of Enumerations */
    private Enumeration en;
    /** current enumeration */
    private Enumeration current;

    /** is {@link #current} up-to-date and has more elements?
    * The combination <CODE>current == null</CODE> and
    * <CODE>checked == true means there are no more elements
    * in this enumeration.
    */
    private boolean checked = false;

    /** Constructs new enumeration from already existing. The elements
    * of <CODE>en</CODE> should be also enumerations. The resulting
    * enumeration contains elements of such enumerations.
    *
    * @param en enumeration of Enumerations that should be sequenced
    */
    public SequenceEnumeration (Enumeration en) {
        this.en = en;
    }

    /** Composes two enumerations into one.
    * @param first first enumeration
    * @param second second enumeration
    */
    public SequenceEnumeration (Enumeration first, Enumeration second) {
        this (new ArrayEnumeration (new Enumeration[] { first, second }));
    }

    /** Ensures that current enumeration is set. If there aren't more
    * elements in the Enumerations, sets the field <CODE>current</CODE> to null.
    */
    private void ensureCurrent () {
        while (current == null || !current.hasMoreElements ()) {
            if (en.hasMoreElements ()) {
                current = (Enumeration)en.nextElement ();
            } else {
                // no next valid enumeration
		current = null;
                return;
            }
        }
    }

    /** @return true if we have more elements */
    public boolean hasMoreElements () {
	if( !checked ) {
	    ensureCurrent ();
	    checked = true;
	}
	return current != null;
    }

    /** @return next element
    * @exception NoSuchElementException if there is no next element
    */
    public synchronized Object nextElement () {
	if( !checked ) {
	    ensureCurrent ();
	}
	if( current != null ) {
	    checked = false;
            return current.nextElement ();
        } else {
	    checked = true;
            throw new java.util.NoSuchElementException ();
        }
    }
}
