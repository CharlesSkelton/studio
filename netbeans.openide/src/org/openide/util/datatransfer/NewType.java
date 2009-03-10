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

import java.io.IOException;
import org.openide.util.HelpCtx;

import org.openide.util.NbBundle;

/** Describes a type that can be created anew.
* @see org.openide.nodes.Node#getNewTypes
* @author Jaroslav Tulach
* @version 0.10, Mar 19, 1998
*/
public abstract class NewType extends Object implements HelpCtx.Provider {
    /** Display name for the creation action. This should be
    * presented as an item in a menu.
    *
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getBundle (NewType.class).getString ("Create");
    }

    /** Help context for the creation action.
    * @return the help context
    */
    public HelpCtx getHelpCtx() {
        return org.openide.util.HelpCtx.DEFAULT_HELP;
    }

    /** Create the object.
    * @exception IOException if something fails
    */
    public abstract void create () throws IOException;

    /* JST: Originally designed for dnd and it now uses getDropType () of a node.
    *
    * Create the object at a specific position.
    * The default implementation simply calls {@link #create()}.
    * Subclasses may
    * allow pastes to a specific index in their
    * children list (if the object has children indexed by integer).
    *
    * @param indx index to insert into, can be ignored if not supported
    * @throws IOException if something fails
    *
    public void createAt (int indx) throws IOException {
      create ();
}
    */
}
