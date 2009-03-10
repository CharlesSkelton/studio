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

/** The class that encapsulates one object into one element enumeration.
*
* @author   Jaroslav Tulach
* @version  0.10, Apr 10, 1998
*/
public class SingletonEnumeration implements Enumeration {
    /** object to return */
    private Object object;

    /** @param object object to be put into the enumeration
    */
    public SingletonEnumeration (Object object) {
        this.object = object;
    }

    /** Tests if this enumeration contains next element.
    * @return  <code>true</code> if this enumeration contains it
    *          <code>false</code> otherwise.
    */
    public boolean hasMoreElements() {
        return object != null;
    }

    /** Returns the next element of this enumeration.
    * @return     the next element of this enumeration.
    * @exception  NoSuchElementException  if no more elements exist.
    */
    public synchronized Object nextElement() {
        if (object == null) {
            throw new NoSuchElementException();
        } else {
            Object o = object;
            object = null;
            return o;
        }
    }
}
