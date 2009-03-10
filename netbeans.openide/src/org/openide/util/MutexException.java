/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.util;

/** Encapsulates other exceptions thrown from a mutex method.
*
* @see Mutex.ExceptionAction
* @see Mutex#readAccess(Mutex.ExceptionAction)
* @see Mutex#writeAccess(Mutex.ExceptionAction)
*
* @author Jaroslav Tulach
*/
public class MutexException extends Exception {
    /** encapsulate exception*/
    private Exception ex;

    static final long serialVersionUID =2806363561939985219L;
    /** Create an encapsulated exception.
    * @param ex the exception
    */
    public MutexException(Exception ex) {
        super(ex.toString());
        this.ex = ex;
    }

    /** Get the encapsulated exception.
    * @return the exception
    */
    public Exception getException () {
        return ex;
    }

}
