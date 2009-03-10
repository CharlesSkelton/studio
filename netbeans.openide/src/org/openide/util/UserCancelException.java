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

package org.openide.util;

/** Exception that is thrown when user cancels interaction so the
* requested result cannot be produced.
*
* @author Jaroslav Tulach
* @version 0.10, Jan 26, 1998
*/
public class UserCancelException extends java.io.IOException {
    static final long serialVersionUID =-935122105568373266L;
    /** Creates new exception UserCancelException
    */
    public UserCancelException () {
        super ();
    }

    /** Creates new exception UserCancelException with text specified
    * string s.
    * @param s the text describing the exception
    */
    public UserCancelException (String s) {
        super (s);
    }
}
