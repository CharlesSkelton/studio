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

/** The class that presents specifiED (in constructor) array
* as an Enumeration.
*
* @author   Ian Formanek
* @version  0.10, Feb 17, 1998
*/
public class ArrayEnumeration implements Enumeration {
    /** The array */
    private Object[] array;
    /** Current index in the array */
    private int index = 0;

    /** Constructs a new ArrayEnumeration for specified array */
    public ArrayEnumeration (Object[] array) {
        this.array = array;
    }

    /** Tests if this enumeration contains more elements.
    * @return  <code>true</code> if this enumeration contains more elements;
    *          <code>false</code> otherwise.
    */
    public boolean hasMoreElements() {
        return (index < array.length);
    }

    /** Returns the next element of this enumeration.
    * @return     the next element of this enumeration.
    * @exception  NoSuchElementException  if no more elements exist.
    */
    public Object nextElement() {
        try {
            return array[index++];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

}
