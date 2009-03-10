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
import org.openide.util.actions.CallbackSystemAction;

/** Go to the previous tab (in a window).
*
* @author Petr Hamernik
*/
public class PreviousTabAction extends CallbackSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 3895815783852934463L;

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/previousTab.gif"; // NOI18N
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx (PreviousTabAction.class);
    }

    public String getName() {
        return NbBundle.getMessage(PreviousTabAction.class, "PreviousTab");
    }
}
