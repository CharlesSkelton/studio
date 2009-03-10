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

import java.io.IOException;
import java.awt.*;

import javax.swing.*;

import org.openide.*;
import org.openide.loaders.*;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import org.openide.util.UserCancelException;

/** Instantiate a template.
* Enabled only when there is one selected node and
* it represents a data object satisfying {@link DataObject#isTemplate}.
*
* @author   Jaroslav Tulach
*
* @deprecated Deprecated since 3.42. The use of this action should be avoided.
*/
public class InstantiateAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 1482795804240508824L;

    protected boolean enable (Node[] activatedNodes) {
        if (activatedNodes.length != 1) return false;
        DataObject obj = (DataObject)activatedNodes[0].getCookie (DataObject.class);
        return obj != null && obj.isTemplate ();
    }

    protected void performAction (Node[] activatedNodes) {
        DataObject obj = (DataObject)activatedNodes[0].getCookie (DataObject.class);
        if (obj != null && obj.isTemplate ()) {
            try {
                instantiateTemplate (obj);
            } catch (UserCancelException ex) {
                // canceled by user
                // do not notify the exception
            } catch (IOException ex) {
                ErrorManager.getDefault ().notify (ex);
            }
        }
    }

    /* @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(InstantiateAction.class, "Instantiate");
    }

    public HelpCtx getHelpCtx () {
        return new HelpCtx (InstantiateAction.class);
    }

    /** Instantiate a template object.
    * Asks user for the target file's folder and creates the file.
    * Then runs the node delegate's {@link org.openide.nodes.NodeOperation#customize customizer} (if there is one).
    * Also the node's {@link Node#getDefaultAction default action}, if any, is run.
    * @param obj the template to use
    * @return set of created objects or null if user canceled the action
    * @exception IOException on I/O error
    * @see DataObject#createFromTemplate
    */
    public static java.util.Set instantiateTemplate (DataObject obj)
    throws IOException {
        // Create component for for file name input
        return NewTemplateAction.getWizard (null).instantiate (obj);
    }
}
