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

import org.openide.text.Line;
import org.openide.nodes.Node;

/** Cookie for data objects that want to provide support for accessing
* lines in a document.
* Lines may change absolute position as changes are made around them in a document.
*
* @see Line
* @see org.openide.text.Line.Set
*
* @author Jaroslav Tulach
*/
public interface LineCookie extends Node.Cookie {
    /** Creates new line set.
    *
    * @return line set for current state of the node
    */
    public Line.Set getLineSet ();
}
