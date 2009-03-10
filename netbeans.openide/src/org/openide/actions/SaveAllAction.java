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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.LifecycleManager;

import org.openide.loaders.DataObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.WeakListener;
import org.openide.util.actions.CallableSystemAction;

/** Save all open objects.
* This class is final only for performance reasons.
* @see DataObject#getRegistry
* @see LifecycleManager#saveAll
*
* @author   Jan Jancura, Ian Formanek
*/
public final class SaveAllAction extends CallableSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 7654588299199250931L;

    /** Reference to the change listener
    * (we treat it weakly, so we have to to prevent it from
    * being finalized before finalization of this action) */
    private ChangeListener chl;

    /* Creates new HashMap and inserts some properties to it.
    * @return the hash map
    */
    protected void initialize () {
        super.initialize ();
        // false by default
        putProperty (PROP_ENABLED, Boolean.FALSE);
        // listen to the changes
        chl = new ModifiedListL();
        DataObject.getRegistry().addChangeListener(
            (ChangeListener)(WeakListener.change(chl, DataObject.getRegistry ())));
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(SaveAllAction.class, "SaveAll");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (SaveAllAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/saveAll.gif"; // NOI18N
    }

    /* Saves all. */
    public void performAction() {
        LifecycleManager.getDefault().saveAll();
    }

    /* Listens to the chnages in list of modified data objects
    * and enables / disables this action appropriately */
    final class ModifiedListL implements ChangeListener {
        public void stateChanged(final ChangeEvent evt) {
            setEnabled(((java.util.Set)evt.getSource()).size() > 0);
        }
    } // end of ModifiedListL inner class
}
