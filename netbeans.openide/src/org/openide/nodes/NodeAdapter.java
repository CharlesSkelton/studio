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

/** Empty adapter for <code>NodeListener</code>.
*
* @author Jaroslav Tulach
* @version 0.10, Jan 16, 1998
*/
public class NodeAdapter extends Object implements NodeListener {
    /* Change in some own node's property.
    * @param ev the event
    */
    public void propertyChange (java.beans.PropertyChangeEvent ev) {
    }

    /* Informs that a set of new children has been added.
    * @param ev event describing the action
    */
    public void childrenAdded (NodeMemberEvent ev) {
    }

    /* Informs that a set of children has been removed.
    * @param ev event describing the action
    */
    public void childrenRemoved (NodeMemberEvent ev) {
    }

    /* Fired when the order of children has changed.
    * @param ev event describing the change
    */
    public void childrenReordered(NodeReorderEvent ev) {
    }

    /* Informs that the node has been deleted.
    * @param ev event describing the node
    */
    public void nodeDestroyed (NodeEvent ev) {
    }
}
