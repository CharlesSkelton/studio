/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.actions;

import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOperation;
import org.openide.util.NbBundle;


/** Open an Explorer window with a particular root node.
* Final only for better performance.
* @see NodeOperation#explore
* @author   Ian Formanek
*/
public final class OpenLocalExplorerAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2703311250025273778L;

    protected void performAction (Node[] activatedNodes) {
        //bugfix #7579 test if nodes array is empty and node is enabled
        if ((activatedNodes != null) && 
              (activatedNodes.length == 1) &&
              (activatedNodes[0].isLeaf() == false) ) {
           NodeOperation.getDefault().explore(activatedNodes[0]);
        }
    }

    protected boolean enable (Node[] activatedNodes) {
        if ((activatedNodes == null) || (activatedNodes.length != 1) ||
                (activatedNodes[0].isLeaf()))
            return false;
        return true;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(OpenLocalExplorerAction.class, "OpenLocalExplorer");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (OpenLocalExplorerAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/openLocalExplorer.gif"; // NOI18N
    }
}
