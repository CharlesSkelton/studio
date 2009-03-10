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

/** Exception that is thrown when the process is about to perform some 
* action that requires user confirmation. It can be useful when there 
* is a call to a method which cannot open a dialog, but still would like
* to ask the user a question. It can raise this exception and higher level
* parts of the system can/should catch it and present a dialog to the user
* and if the user agrees reinvoke the action again.
* <P>
* The <code>getLocalizedMessage</code> method should return the user question,
* which will be shown to the user in a dialog with OK, Cancel options and 
* if the user chooses OK, method <code>ex.confirmed ()</code> will be called.
*
* @author Jaroslav Tulach
*/
public abstract class UserQuestionException extends java.io.IOException {
    static final long serialVersionUID =-654358275349813705L;
    
    /** Creates new exception UserQuestionException
    */
    public UserQuestionException() {
        super ();
    }

    /** Creates new exception UserQuestionException with text specified
    * string s.
    * @param s the text describing the exception
    */
    public UserQuestionException(String s) {
        super (s);
    }
    
    /** Invoke the action if the user confirms the action.
     * @exception IOException if another I/O problem exists
     */
    public abstract void confirmed () throws java.io.IOException;
}

