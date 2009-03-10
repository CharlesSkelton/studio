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

/** Allows listening to progress of manipulation with ExTransferable.
* So it is notified when the transferable is accepted/rejected by 
* an operation or if it is released from a clipboard.
*
* @author Jaroslav Tulach
*/
public interface TransferListener extends java.util.EventListener {
    /** Accepted by a drop operation.
    * @param action One of java.awt.dnd.DndConstants like ACTION_COPY, ACTION_MOVE,
    * ACTION_LINK.
    */
    public void accepted (int action);

    /** The transfer has been rejected.
    */
    public void rejected ();

    /** Released from a clipboard.
    */
    public void ownershipLost ();
}
