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

/** Event describing change of clipboard content.
*
* @see ExClipboard
*
* @author Jaroslav Tulach
* @version 0.11, May 22, 1997
*/
public final class ClipboardEvent extends java.util.EventObject {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -468077075889138021L;


    /** consumed */
    private boolean consumed = false;

    /**
    * @param c the clipboard
    */
    ClipboardEvent (ExClipboard c) {
        super (c);
    }

    /** Get the clipboard where operation occurred.
    * @return the clipboard
    */
    public ExClipboard getClipboard () {
        return (ExClipboard)getSource ();
    }

    /** Marks this event consumed. Can be
    * used by listeners that are sure that their own reaction to the event
    * is really significant, to inform other listeners that they need not do anything.
    */
    public void consume () {
        consumed = true;
    }

    /** Has this event been consumed?
     * @return <code>true</code> if it has
    */
    public boolean isConsumed () {
        return consumed;
    }
}
