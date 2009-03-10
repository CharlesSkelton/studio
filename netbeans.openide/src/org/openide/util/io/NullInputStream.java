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
import java.io.InputStream;

/** null InputStream utility
*
* @author Ales Novak
* @version 0.10 Apr 24, 1998
*/
public class NullInputStream extends InputStream {

    /** is an exception be thrown while read? */
    public boolean throwException;

    /** read method */
    public int read() throws IOException {
        if (throwException) throw new IOException();
        return -1;
    }
}
