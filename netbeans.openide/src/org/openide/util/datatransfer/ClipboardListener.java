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

/** Listener to changes in the clipboard.
*
* @author Jaroslav Tulach
* @version 0.10, Dec 12, 1997
*/
public interface ClipboardListener extends java.util.EventListener {
    /** Called when the content of the clipboard is changed.
    * @param ev event describing the action
    */
    public void clipboardChanged (ClipboardEvent ev);
}
