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


import javax.swing.undo.CannotRedoException;

import org.openide.ErrorManager;
import org.openide.awt.UndoRedo;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

/** Redo an edit.
*
* @see UndoAction
* @author   Ian Formanek, Jaroslav Tulach
*/
public class RedoAction extends CallableSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -7791957449503504810L;

    public boolean isEnabled() {
        UndoAction.initializeUndoRedo();
        return super.isEnabled();
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(RedoAction.class, "Redo", UndoAction.getUndoRedo().getRedoPresentationName());
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (RedoAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/redo.gif"; // NOI18N
    }

    public void performAction() {
        try {
            UndoRedo undoRedo = UndoAction.getUndoRedo ();
            if (undoRedo.canRedo ()) {
                undoRedo.redo ();
            }
        } catch (CannotRedoException ex) {
            ErrorManager.getDefault ().notify (ex);
        }
        UndoAction.updateStatus();
    }
}
