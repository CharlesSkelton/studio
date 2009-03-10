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

/** Special IOException that is used to signal that the write operation
* failed but the underlaying stream is not corrupted and can be used
* for next operations.
*
*
* @author Jaroslav Tulach
*/
public class OperationException extends IOException {
    /** the exception encapsulated */
    private Exception ex;

    static final long serialVersionUID =8389141975137998729L;
    /** Default constructor.
    */
    public OperationException (Exception ex) {
        this.ex = ex;
    }

    /** @return the encapsulated exception.
    */
    public Exception getException () {
        return ex;
    }

    /** Description taken from previous message
    */
    public String getMessage () {
        return ex.getMessage ();
    }

}
