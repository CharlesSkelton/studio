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

package org.openide.nodes;

/** Discriminator accepting only certain sets of nodes.
* <P>
* Currently used in {@link NodeOperation#select}
* to find out if the currently selected beans are valid or not.
*
* @author Jaroslav Tulach
*/
public interface NodeAcceptor {
    /** Is the set of nodes acceptable?
    * @param nodes the nodes to consider
    * @return <CODE>true</CODE> if so
    */
    public boolean acceptNodes (Node[] nodes);
}
