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

package org.netbeans.editor;

/**
* This exception is thrown either if the mark is invalid and it should
* be valid (<CODE>getOffset(), getLine(), remove()</CODE>) or on
* the oposite side if the mark is valid and it shouldn't be
* i.e. <CODE>insertMark()</CODE>
*
* @author Miloslav Metelka
* @version 1.00
*/

public class InvalidMarkException extends Exception {

    static final long serialVersionUID =-7408566695283816594L;
    InvalidMarkException() {
        super();
    }

    InvalidMarkException(String s) {
        super(s);
    }

}
