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

package org.openide.cookies;

import org.openide.nodes.Node;

/** Permits an object which was {@link OpenCookie opened} to be closed.
*/
public interface CloseCookie extends Node.Cookie {

    /** Closes the object if it is open.
    * @return <code>true</code> if it was really closed; <code>false</code> if the user cancelled the request
    */
    public boolean close ();
}
