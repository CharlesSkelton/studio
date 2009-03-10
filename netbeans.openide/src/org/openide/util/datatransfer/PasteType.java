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

package org.openide.util.datatransfer;

import java.awt.datatransfer.*;
import java.io.IOException;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/** Clipboard operation providing one kind of paste action.
* @see org.openide.nodes.Node#getPasteTypes
* @author Petr Hamernik
* @version 0.11, Jan 16, 1998
*/
public abstract class PasteType extends Object implements HelpCtx.Provider {
    /** Display name for the paste action. This should be
    * presented as an item in a menu.
    *
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getBundle (PasteType.class).getString ("Paste");
    }

    /** Help content for the action.
    * @return the help context
    */
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    /** Perform the paste action.
    * @return transferable which should be inserted into the clipboard after the
    *         paste action. It can be <code>null</code>, meaning that the clipboard content
    *         is not affected. Use e.g. {@link ExTransferable#EMPTY} to clear it.
    * @throws IOException if something fails
    */
    public abstract Transferable paste() throws IOException;

    /* JST: Originally designed for dnd and it now uses getDropType () of a node.
    *
    * Perform the paste action at an index.
    * @see NewType#createAt(int)
    * @param indx index to insert into, can be ignored if not supported
    * @return new transferable to be inserted into the clipboard
    *  public Transferable pasteAt (int indx) throws IOException {
      return paste ();
}
    */
}
