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

/** Cookie for opening an object.
*
* @author Petr Hamernik
* @version 0.10, Apr 03, 1998
*/
public interface OpenCookie extends Node.Cookie {

    /** Invokes the open action. */
    public void open();
}
