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

/** Cookie permitting objects to be viewed.
*
* @author Jan Jancura
*/
public interface ViewCookie extends Node.Cookie {
    /** Instructs an viewer to be opened. The operation can
    * return immediately and the viewer be opened later.
    * There can be more than one viewer open, so one of them is
    * arbitrarily chosen and opened.
    */
    public void view ();

}
