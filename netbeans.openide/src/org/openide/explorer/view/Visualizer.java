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

package org.openide.explorer.view;

import javax.swing.tree.*;

import org.openide.nodes.Node;

/** This class provide access to thread safe layer that
* reflects the hierarchy of Nodes, but is updated only in
* event dispatch thread (in contrast to nodes that can be updated from any thread).
* That is why this class is useful for writers of explorer views,
* because it guarantees that all changes will be done safely.
* <P>
* NodeTreeModel, NodeListModel, etc. use these objects as its
* model values.
*
* @author Jaroslav Tulach
*/
public class Visualizer extends Object {

    /** No constructor. */
    private Visualizer () {
    }

    /** Methods that create a tree node for given node.
    * The tree node reflects the state of the associated node as close
    * as possible, but is updated asynchronously in event dispatch thread.
    * <P>
    * This method can be called only from AWT-Event dispatch thread.
    *
    * @param node node to create safe representant for
    * @return tree node that represents the node
    */
    public static TreeNode findVisualizer (Node node) {
        return VisualizerNode.getVisualizer (null, node);
    }

    /** Converts visualizer object back to its node representant.
    * 
    * @param visualizer visualizer create by findVisualizer method
    * @return node associated with the visualizer
    * @exception ClassCastException if the parameter is invalid
    */
    public static Node findNode (Object visualizer) {
        if (visualizer instanceof Node) {
            return (Node)visualizer;
        } else {
            return ((VisualizerNode)visualizer).node;
        }
    }
}
