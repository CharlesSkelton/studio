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

import org.openide.cookies.OpenCookie;
import org.openide.util.HelpCtx;
import org.openide.util.actions.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/** Opens a node (for example, in a web browser, or in the Editor).
* @see OpenCookie
*
* @author   Petr Hamernik
*/
public class OpenAction extends CookieAction {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = -5847763658433081444L;

    /* @return set of needed cookies */
    protected Class[] cookieClasses () {
        return new Class[] { OpenCookie.class };
    }

    /* @return false */
    protected boolean surviveFocusChange () {
        return false;
    }

    /* @return any */
    protected int mode () {
        return MODE_ANY;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(OpenAction.class, "Open");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (OpenAction.class);
    }

    /*
    * Standart perform action extended by actually activated nodes.
    * @see CallableSystemAction#performAction
    *
    * @param activatedNodes gives array of actually activated nodes.
    */
    protected void performAction (final Node[] activatedNodes) {
        for (int i = 0; i < activatedNodes.length; i++) {
            OpenCookie oc =
                (OpenCookie)activatedNodes[i].getCookie(OpenCookie.class);
            if (oc != null) {
                oc.open();
            }
        }
    }
}
