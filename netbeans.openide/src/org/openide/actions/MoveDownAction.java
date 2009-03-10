/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.actions;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.openide.nodes.Index;
import org.openide.util.HelpCtx;
import org.openide.util.actions.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/** Move an item down in a list.
* This action is final only for performance reasons.
* @see Index
*
* @author   Ian Formanek, Dafe Simonek
*/
public final class MoveDownAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -6895014137711668193L;
    /** the key to listener to reorder of selected nodes */
    private static final String PROP_ORDER_LISTENER = "sellistener"; // NOI18N
    /** Holds index cookie on which we are listening */
    private Reference curIndexCookie;

    /* Initilizes the set of properties.
    */
    protected void initialize () {
        super.initialize();
        // initializes the listener
        OrderingListener sl = new OrderingListener();
        putProperty(PROP_ORDER_LISTENER, sl);
    }

    /** Getter for curIndexCookie */
    private Index getCurIndexCookie() {
        return (curIndexCookie == null ? null : (Index) curIndexCookie.get());
    }
    
    /* Actually performs the action of moving the node down
    * in the order.
    * @param activatedNodes The nodes on which to perform the action.
    */
    protected void performAction (Node[] activatedNodes) {
        // we need to check activatedNodes, because there's no
        // guarantee that they not changed between enable() and
        // performAction calls
        Index cookie = getIndexCookie(activatedNodes);
        if (cookie == null) return;
        int nodeIndex = cookie.indexOf(activatedNodes[0]);
        if ((nodeIndex >= 0) && (nodeIndex < (cookie.getNodesCount() - 1))) {
            cookie.moveDown(nodeIndex);
        }
    }

    /* Manages enable - disable logic of this action */
    protected boolean enable (Node[] activatedNodes) {
        // remove old listener, if any
        Index idx = getCurIndexCookie();
        if (idx != null) {
            idx.removeChangeListener(
                (ChangeListener) getProperty(PROP_ORDER_LISTENER));
            idx = null;
        }
        Index cookie = getIndexCookie(activatedNodes);
        if (cookie == null) return false;
        int nodeIndex = cookie.indexOf(activatedNodes[0]);
        // now start listening to reordering changes
        cookie.addChangeListener(
            (OrderingListener)getProperty(PROP_ORDER_LISTENER));
        curIndexCookie = new WeakReference(cookie);
        return (nodeIndex >= 0) && (nodeIndex < (cookie.getNodesCount() - 1));
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(MoveDownAction.class, "MoveDown");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (MoveDownAction.class);
    }

    /** Helper method. Returns index cookie or null, if some
    * conditions aren't satisfied */
    private Index getIndexCookie (Node[] activatedNodes) {
        if ((activatedNodes == null) || (activatedNodes.length != 1))
            return null;
        Node parent = activatedNodes[0].getParentNode();
        if (parent == null) return null;
        return (Index)parent.getCookie(Index.class);
    }

    /** Listens to the ordering changes and enables/disables the
    * action if appropriate */
    private final class OrderingListener implements ChangeListener {
        OrderingListener() {}
        public void stateChanged (ChangeEvent e) {
            Node[] activatedNodes = getActivatedNodes();
            Index cookie = getIndexCookie(activatedNodes);
            if (cookie == null)
                setEnabled(false);
            else {
                int nodeIndex = cookie.indexOf(activatedNodes[0]);
                setEnabled((nodeIndex >= 0) &&
                           (nodeIndex < (cookie.getNodesCount() - 1)));
            }
        }
    }

}
