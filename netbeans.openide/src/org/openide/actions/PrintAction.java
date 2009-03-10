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
import org.openide.util.actions.CookieAction;
import org.openide.nodes.Node;
import org.openide.cookies.PrintCookie;
import org.openide.util.NbBundle;


/** Print the selected object.
* @see PrintCookie
*
* @author Ales Novak
*/
public class PrintAction extends CookieAction {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = 8566654780658436581L;

    /** @return PrintCookie.class */
    protected Class[] cookieClasses() {
        return new Class[] { PrintCookie.class };
    }

    protected void performAction(final Node[] activatedNodes) {
        for (int i = 0; i < activatedNodes.length; i++) {
            PrintCookie pc = (PrintCookie)activatedNodes[i].getCookie (PrintCookie.class);
            if (pc != null) {
                pc.print();
            }
        }
    }

    protected int mode () {
        return MODE_ALL;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(PrintAction.class, "Print");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (PrintAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/print.gif"; // NOI18N
    }
}
