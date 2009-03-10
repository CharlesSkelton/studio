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
import java.util.Arrays;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.openide.ErrorManager;
import org.openide.nodes.Index;
import org.openide.util.HelpCtx;
import org.openide.util.actions.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/** Move an item up in a list.
* This action is final only for performance reasons.
*
* @see Index
* @author Ian Formanek, Jan Jancura, Dafe Simonek
*/
public final class MoveUpAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -8201315242813084212L;
    /** the key to listener to reorder of selected nodes */
    private static final String PROP_ORDER_LISTENER = "sellistener"; // NOI18N
    /** Holds index cookie on which we are listening */
    private Reference curIndexCookie;
    private static ErrorManager err = null;
    private static boolean errInited = false;
    private static final void initErr () {
        if (! errInited) {
            errInited = true;
            ErrorManager master = ErrorManager.getDefault();
            ErrorManager tmp = master.getInstance("org.openide.actions.MoveUpAction"); // NOI18N
            if (tmp.isLoggable (ErrorManager.UNKNOWN)) {
                err = tmp;
            }
        }
    }

    /* Initilizes the set of properties.
    */
    protected void initialize () {
        initErr ();
        if (err != null) {
            err.log (ErrorManager.UNKNOWN, "initialize");
        }
        super.initialize();
        // initializes the listener
        OrderingListener sl = new OrderingListener();
        putProperty(PROP_ORDER_LISTENER, sl);
    }
    
    /** Getter for curIndexCookie */
    private Index getCurIndexCookie() {
        return (curIndexCookie == null ? null : (Index) curIndexCookie.get());
    }

    /* Actually performs the action of moving up
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
        if (nodeIndex > 0) {
            cookie.moveUp(nodeIndex);
        }
    }

    /* Manages enable - disable logic of this action */
    protected boolean enable (Node[] activatedNodes) {
        initErr ();
        if (err != null) {
            err.log (ErrorManager.UNKNOWN, "enable; activatedNodes=" + (activatedNodes == null ? null : Arrays.asList (activatedNodes)));
        }
        // remove old listener, if any
        Index idx = getCurIndexCookie();
        if (idx != null) {
            idx.removeChangeListener(
                (ChangeListener) getProperty(PROP_ORDER_LISTENER));
        }
        Index cookie = getIndexCookie(activatedNodes);
        if (err != null) {
            err.log (ErrorManager.UNKNOWN, "enable; cookie=" + cookie);
        }
        if (cookie == null) return false;
        // now start listening to reordering changes
        cookie.addChangeListener(
            (OrderingListener)getProperty(PROP_ORDER_LISTENER));
        curIndexCookie = new WeakReference(cookie);
        int index = cookie.indexOf (activatedNodes[0]);
        if (err != null) {
            err.log (ErrorManager.UNKNOWN, "enable; index=" + index);
            if (index == -1) {
                Node parent = activatedNodes[0].getParentNode ();
                err.log (ErrorManager.UNKNOWN, "enable; parent=" + parent + "; parent.children=" + Arrays.asList (parent.getChildren ().getNodes ()));
            }
        }
        return index > 0;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(MoveUpAction.class, "MoveUp");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (MoveUpAction.class);
    }

    /** Helper method. Returns index cookie or null, if some
    * conditions weren't satisfied */
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
            initErr ();
            Node[] activatedNodes = getActivatedNodes();
            if (err != null) {
                err.log (ErrorManager.UNKNOWN, "stateChanged; activatedNodes=" + (activatedNodes == null ? null : Arrays.asList (activatedNodes)));
            }
            Index cookie = getIndexCookie(activatedNodes);
            if (err != null) {
                err.log (ErrorManager.UNKNOWN, "stateChanged; cookie=" + cookie);
            }
            if (cookie == null) {
                setEnabled (false);
            } else {
                int index = cookie.indexOf (activatedNodes[0]);
                if (err != null) {
                    err.log (ErrorManager.UNKNOWN, "stateChanged; index=" + index);
                }
                setEnabled (index > 0);
            }
        }
    }

}
