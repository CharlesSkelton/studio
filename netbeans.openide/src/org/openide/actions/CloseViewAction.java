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

/** Close the current top component.
* @see org.openide.windows.TopComponent#close
*
* @author Petr Hamernik
*/
public class CloseViewAction extends CallbackSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2779504032537558754L;

    public HelpCtx getHelpCtx() {
        return new HelpCtx (CloseViewAction.class);
    }

    public String getName() {
        return NbBundle.getMessage(CloseViewAction.class, "CloseView");
    }
}
