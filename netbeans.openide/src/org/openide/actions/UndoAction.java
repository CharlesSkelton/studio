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

import java.beans.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import javax.swing.SwingUtilities;

import org.openide.ErrorManager;
import org.openide.awt.UndoRedo;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.WeakListener;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponent.Registry;
import org.openide.windows.WindowManager;

/** Undo an edit.
*
* @see UndoRedo
* @author   Ian Formanek, Jaroslav Tulach
*/
public class UndoAction extends CallableSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2762036372802427913L;

    /** initialized listener */
    private static Listener listener;

    /** last edit */
    private static UndoRedo last = UndoRedo.NONE;
    
    public boolean isEnabled() {
        initializeUndoRedo();
        return super.isEnabled();
    }

    /** Initializes the object.
    */
    static synchronized void initializeUndoRedo () {
        if (listener != null) return;

        listener = new Listener ();

        Registry r = WindowManager.getDefault ().getRegistry ();

        r.addPropertyChangeListener (
            WeakListener.propertyChange (listener, r)
        );
        last = getUndoRedo ();
        last.addChangeListener (listener);

        updateStatus ();
    }

    /** Update status of action.
    */
    static synchronized void updateStatus() {
        if (undoAction == null) undoAction = (UndoAction)findObject(UndoAction.class, false);
        if (redoAction == null) redoAction = (RedoAction)findObject(RedoAction.class, false);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UndoRedo ur = getUndoRedo();
                if (undoAction != null) undoAction.setEnabled(ur.canUndo());
                if (redoAction != null) redoAction.setEnabled(ur.canRedo());
            }
        });
    }
    private static UndoAction undoAction = null;
    private static RedoAction redoAction = null;

    /** Finds current undo/redo.
    */
    static UndoRedo getUndoRedo (){
        TopComponent el = WindowManager.getDefault ().getRegistry ().getActivated ();
        return el == null ? UndoRedo.NONE : el.getUndoRedo ();
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(UndoAction.class, "Undo", getUndoRedo().getUndoPresentationName());
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (UndoAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/undo.gif"; // NOI18N
    }

    /* Perform action. Try to do undo operation.
    */
    public void performAction() {
        try {
            UndoRedo undoRedo = getUndoRedo ();
            if (undoRedo.canUndo ()) {
                undoRedo.undo ();
            }
        } catch (CannotUndoException ex) {
            ErrorManager.getDefault ().notify (ex);
        }
        updateStatus();
    }

    /** Listener on changes of selected workspace element and
    * its changes.
    */
    private static final class Listener implements PropertyChangeListener, ChangeListener {
        Listener() {}
        public void propertyChange (PropertyChangeEvent ev) {
            updateStatus ();
            last.removeChangeListener (this);
            last = getUndoRedo ();
            last.addChangeListener (this);
        }

        public void stateChanged (ChangeEvent ev) {
            updateStatus ();
        }
    }

}
