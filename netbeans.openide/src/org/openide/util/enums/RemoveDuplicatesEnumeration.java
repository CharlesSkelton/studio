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
import java.util.HashSet;

/** Enumeration that scans through another one and removes duplicates.
* Two objects are duplicate if <CODE>one.equals (another)</CODE>.
*
* @author Jaroslav Tulach
*/
public class RemoveDuplicatesEnumeration extends FilterEnumeration {
    /** hashtable with all returned objects */
    private HashSet all = new HashSet (37);

    /**
    * @param en enumeration to filter
    */
    public RemoveDuplicatesEnumeration (Enumeration en) {
        super (en);
    }

    /** Filters objects. Overwrite this to decide which objects should be
    * included in enumeration and which not.
    * @param o the object to decide on
    * @return true if it should be in enumeration and false if it should not
    */
    protected boolean accept (Object o) {
    	return all.add(o);
    }
}
