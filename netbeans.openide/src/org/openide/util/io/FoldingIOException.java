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

package org.openide.util.io;

import java.io.IOException;

/** Encapsulates an exception.
*
* @author Ales Novak
* @deprecated Better to create a new <code>IOException</code> and {@link org.openide.ErrorManager#annotate annotate} it with the throwable.
*/
public class FoldingIOException extends IOException {

    /** Foreign exception */
    private Throwable t;

    static final long serialVersionUID =1079829841541926901L;
    /**
    * @param t a foreign folded Throwable
    */
    public FoldingIOException(Throwable t) {
        super(t.getMessage());
        this.t = t;
    }
    /** Prints stack trace of the foreign exception */
    public void printStackTrace() {
        t.printStackTrace();
    }
    /** Prints stack trace of the foreign exception */
    public void printStackTrace(java.io.PrintStream s) {
        t.printStackTrace(s);
    }
    /** Prints stack trace of the foreign exception */
    public void printStackTrace(java.io.PrintWriter s) {
        t.printStackTrace(s);
    }
    /**
    * @return toString of the foreign exception
    */
    public String toString() {
        return t.toString();
    }
    /**
    * @return getLocalizedMessage of the foreign exception 
    */
    public String getLocalizedMessage() {
        return t.getLocalizedMessage();
    }
}
