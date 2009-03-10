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

package org.openide.actions;


import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.*;
import org.openide.nodes.Node;
import org.openide.nodes.Index;

/** Reorder items in a list with a dialog.
* @see Index
*
* @author   Petr Hamernik, Dafe Simonek
*/
public class ReorderAction extends CookieAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2388678563650229890L;

    /* Constructs new reorder action */
    public ReorderAction() {
        super();
    }

    /* Returns false - action should be disabled when a window with no
    * activated nodes is selected.
    *
    * @return false do not survive the change of focus
    */
    protected boolean surviveFocusChange () {
        return false;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getBundle(ReorderAction.class).getString("Reorder");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (ReorderAction.class);
    }

    /* Creates a set of classes that are tested by the cookie.
    *
    * @return list of classes this cookie tests
    */
    protected Class[] cookieClasses () {
        return new Class[] { Index.class };
    }

    /* Overrides abstract method from CookieAction.
    * @return returns the mode of this action.
    */
    protected int mode () {
        return MODE_EXACTLY_ONE;
    }

    protected void performAction (Node[] activatedNodes) {
        Node n = activatedNodes[0]; // we supposed that one node is activated
        Index order = (Index)n.getCookie(Index.class);
        if (order != null)
            order.reorder();
    }

}
