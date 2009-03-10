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

package org.openide.filesystems;



import org.openide.loaders.DataFolder;
import org.openide.util.actions.CookieAction;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/** Action for refresh of file systm
*
* @author Jaroslav Tulach
*/
final class RefreshAction extends CookieAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -6022165630798612727L;

    /** @return DataFolder class */
    protected Class[] cookieClasses () {
        return new Class[] { DataFolder.class };
    }

    protected void performAction (Node[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            DataFolder df = (DataFolder)nodes[i].getCookie (DataFolder.class);
            if (df != null) {
                FileObject fo = df.getPrimaryFile ();
                fo.refresh ();
            }
        }
    }

    protected int mode () {
        return MODE_ALL;
    }

    public String getName () {
        return NbBundle.getBundle(RefreshAction.class).getString ("LAB_Refresh");
    }

    public HelpCtx getHelpCtx () {
        return new HelpCtx (RefreshAction.class);
    }

}
