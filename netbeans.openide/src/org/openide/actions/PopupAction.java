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

import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/** Open a popup menu.
 * For example, may be bound to the context menu in the Explorer.
 */
public final class PopupAction extends CallbackSystemAction {
    
    private static final long serialVersionUID = -7852158840533508176L;
    
    protected void initialize() {
        super.initialize();
        // XXX revisit:
        setSurviveFocusChange(true);
        // Cf. org.netbeans.core.windows.frames.NbFocusManager:
        putProperty ("OpenIDE-Transmodal-Action", Boolean.TRUE); // NOI18N
    }

    /** @return name of the Action
     */
    public String getName() {
        return NbBundle.getMessage(PopupAction.class, "Popup");
    }
    
    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (PopupAction.class);
    }
}
