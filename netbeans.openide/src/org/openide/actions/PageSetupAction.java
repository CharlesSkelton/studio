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

import java.awt.print.PrinterJob;

import org.openide.text.PrintSettings;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

/** Sets up page for printing.
*/
public final class PageSetupAction extends CallableSystemAction {

    private static final long serialVersionUID = 659118349598130353L;
    
    /* performs page setup */
    public synchronized void performAction() {
        PrintSettings ps = (PrintSettings) PrintSettings.findObject(PrintSettings.class, true);
        PrinterJob pj = PrinterJob.getPrinterJob();
        ps.setPageFormat(pj.pageDialog(PrintSettings.getPageFormat(pj)));
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(PageSetupAction.class, "PageSetup");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx(PageSetupAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/pageSetup.gif"; // NOI18N
    }
}
