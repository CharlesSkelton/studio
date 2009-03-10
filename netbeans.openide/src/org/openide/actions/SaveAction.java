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

import java.io.IOException;

import org.openide.awt.StatusDisplayer;
import org.openide.ErrorManager;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.cookies.SaveCookie;
import org.openide.util.NbBundle;


/** Save a single object.
* @see SaveCookie
*
* @author   Jan Jancura, Petr Hamernik, Ian Formanek, Dafe Simonek
*/
public class SaveAction extends CookieAction {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = 8726214103323017934L;

    protected Class[] cookieClasses () {
        return new Class[] { SaveCookie.class };
    }

    protected void performAction (final Node[] activatedNodes) {
        if (activatedNodes.length == 0) return;
        SaveCookie sc = (SaveCookie)activatedNodes[0].getCookie (SaveCookie.class);
        if (sc != null) {
            try {
                sc.save();
                StatusDisplayer.getDefault().setStatusText(
                    NbBundle.getMessage(
                        SaveAction.class, 
                        "MSG_saved",
                        getSaveMessage(activatedNodes[0])
                    )
                ); 
            }
            catch (IOException e) {
                ErrorManager err = ErrorManager.getDefault ();
                err.annotate (e, NbBundle.getMessage (
                    SaveAction.class,
                    "EXC_notsaved",
                    getSaveMessage (activatedNodes[0])
                ));
                err.notify (e);
            }
        }
    }
    
    /**
     * Extract a suitable after-save message. If the node contains a
     * cookie for its DataObject, the DataObject's node delegate display name
     * is used to notify the user. In other cases, the current node's display
     * name is returned.
     * @param node that is being saved.
     * @return name that should be printed to the user.
     */
    private String getSaveMessage(Node n) {
        DataObject d = (DataObject)n.getCookie(DataObject.class);
        if (d != null) {
            Node n2 = d.getNodeDelegate();
            if (n2 != null) {
                return n2.getDisplayName();
            }
        }
        return n.getDisplayName();
    }

    protected int mode () {
        return MODE_EXACTLY_ONE;
    }

    public String getName() {
        return NbBundle.getMessage (SaveAction.class, "Save");
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx (SaveAction.class);
    }

    protected String iconResource () {
        return "org/openide/resources/actions/save.gif"; // NOI18N
    }
}
