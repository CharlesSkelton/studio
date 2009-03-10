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

/** Listener to special changes in <code>Node</code>s. Is a property
* change listener so that all changes in properties in the {@link Node node} can be fired
* in the usual way.
* <P>
* Methods childrenAdded, childrenRemoved and childrenReordered are called
* with Children.MUTEX.writeAccess which guarantees that no other thread
* can change the hierarchy during that time, but also requires proper
* implementation of all NodeListeners which should avoid calls  
* to other threads which might require access
* to Children.MUTEX due to changes nodes hierarchy or do any other kind of 
* starvation.
* 
*
* @author Jaroslav Tulach
*/
public interface NodeListener extends java.beans.PropertyChangeListener {

    /** Fired when a set of new children is added.
    * @param ev event describing the action
    */
    public void childrenAdded (NodeMemberEvent ev);

    /** Fired when a set of children is removed.
    * @param ev event describing the action
    */
    public void childrenRemoved (NodeMemberEvent ev);

    /** Fired when the order of children is changed.
    * @param ev event describing the change
    */
    public void childrenReordered(NodeReorderEvent ev);

    /** Fired when the node is deleted.
    * @param ev event describing the node
    */
    public void nodeDestroyed (NodeEvent ev);
}
