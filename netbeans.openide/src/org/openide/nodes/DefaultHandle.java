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

import org.openide.ErrorManager;

/** Simple implementation of <code>Node.Handle</code>.
* When created by {@link #createHandle} it
* looks for the parent of the node and stores the node's name.
* When {@link #getNode} is then called, it tries to restore the
* parent and then to walk down to the child.
* <p>Note that if most nodes use <code>DefaultHandle</code>, this
* may walk up to the root recursively. Otherwise, some other sort
* of handle may provide the termination case.
*
* @author Jaroslav Tulach, Jesse Glick
*/
public final class DefaultHandle extends Object implements Node.Handle {
    /** parent handle */
    private Node.Handle parent;
    /** path to the node (just one hop, really) */
    private String path;

    private static final long serialVersionUID = -8739127064355983273L;
    /* Create a new handle.
    * @param parent handle for the parent node
    * @param path desired name of child
    */
    DefaultHandle (Node.Handle parent, String path) {
        this.parent = parent;
        this.path = path;
    }

    /** Find the node.
    * @return the found node
    * @exception IOException if the parent cannot be recreated
    * @exception NodeNotFoundException if the path is not valid (exception may be examined for details)
    */
    public Node getNode () throws java.io.IOException {
        Node parentNode = parent.getNode ();
        Node child = parentNode.getChildren ().findChild (path);
        if (child != null)
            return child;
        else
            throw new NodeNotFoundException (parentNode, path, 0);
    }

    /** Create a handle for a given node.
    * A handle cannot be created under these circumstances:
    * <ol>
    * <li>The node has no name.
    * <li>The node has no parent.
    * <li>The parent has no handle.
    * <li>The parent is incapable of finding its child by the supplied name.
    * </ol>
    * @param node the node to create a handler for
    * @return the handler, or <code>null</code> if a handle cannot be created
    */
    public static DefaultHandle createHandle (final Node node) {
        try {
            Children.PR.enterReadAccess ();
            String childPath = node.getName ();
            if (childPath == null) return null;
            Node parentNode = node.getParentNode ();
            if (parentNode == null) return null;
            if (parentNode.getChildren ().findChild (childPath) != node) {
                if (ErrorManager.getDefault().isLoggable(ErrorManager.WARNING)) {
                    ErrorManager.getDefault().log(ErrorManager.WARNING, "WARNING: parent could not find own child in DefaultHandle.createHandle\n\tnode=" + node + "\n\tparentNode=" + parentNode + "\n\tchildPath=" + childPath + "\n\tfound child=" + parentNode.getChildren().findChild(childPath));
                }
                return null;
            }
            Node.Handle parentHandle = parentNode.getHandle ();
            if (parentHandle == null) return null;
            return new DefaultHandle (parentHandle, childPath);
        } finally {
            Children.PR.exitReadAccess ();
        }
    }

    public String toString () {
        return "DefaultHandle[" + parent + "|" + path + "]"; // NOI18N
    }

}
