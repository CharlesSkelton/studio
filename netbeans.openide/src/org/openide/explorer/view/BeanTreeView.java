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

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.openide.explorer.*;
import org.openide.util.*;
import org.openide.nodes.Node;

/** Functioning tree view class.
*
* @author   Petr Hamernik, Ian Formanek
* @version  1.00, Aug 14, 1998
*/
public class BeanTreeView extends TreeView {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 3841322840231536380L;

    /** Constructor.
    */
    public BeanTreeView() {
    }

    void initializeTree () {
        super.initializeTree();
    }

    /** Create a new model.
    * The default implementation creates a {@link NodeTreeModel}.
    * @return the model
    */
    protected NodeTreeModel createModel() {
        return new NodeTreeModel ();
    }

    /** Can select any nodes.
    */
    protected boolean selectionAccept (Node[] nodes) {
        return true;
    }


    /* Synchronizes selected nodes from the manager of this Explorer.
    */
    protected void showSelection (TreePath[] treePaths) {
        tree.getSelectionModel().setSelectionPaths(treePaths);
        if (treePaths.length == 1)
            showPathWithoutExpansion (treePaths[0]);
    }

    /* Called whenever the value of the selection changes.
    * @param nodes nodes
    * @param em explorer manager
    */
    protected void selectionChanged(Node[] nodes, ExplorerManager em) throws PropertyVetoException {
        if (nodes.length > 0) {
            Node context = nodes[0].getParentNode ();
            for (int i = 1; i < nodes.length; i++) {
                if (context != nodes[i].getParentNode ()) {
                    em.setSelectedNodes (nodes);
                    return;
                }
            }
            // May not set explored context above the root context:
            if (em.getRootContext ().getParentNode () == context) {
                em.setExploredContextAndSelection(null, nodes);
            } else {
                em.setExploredContextAndSelection(context, nodes);
            }
        } else {
            em.setSelectedNodes(nodes);
        }
    }
    
    /** Expand the given path and makes it visible.
    * @param path the path
    */
    protected void showPath (TreePath path) {
        tree.expandPath(path);
        showPathWithoutExpansion (path);
    }

    /** Make a path visible.
    * @param path the path
    */
    private void showPathWithoutExpansion (TreePath path) {
        Rectangle rect = tree.getPathBounds(path);
        if (rect != null) { //PENDING
            Rectangle vis = tree.getVisibleRect();
            if (vis != null) {
                rect.width = vis.width;
                rect.x = vis.x;
            } else {
                rect.width += rect.x;
                rect.x = 0;
            }
            tree.scrollRectToVisible(rect);
        }
    }

    /** Delegate the setEnable method to Jtree
     *  @param enabled whether to enable the tree
     */
    public void setEnabled (boolean enabled){
        this.tree.setEnabled (enabled);
    }

    /** Is the tree enabled
     *  @return boolean
     */
    public boolean isEnabled () {
        return this.tree.isEnabled();
    }
}
