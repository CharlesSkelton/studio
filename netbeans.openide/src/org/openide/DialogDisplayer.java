/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide;

import java.awt.Dialog;

import org.openide.util.Lookup;

/** Permits dialogs to be displayed.
 * @author Jesse Glick
 * @since 3.14
 */
public abstract class DialogDisplayer {

    /** Get the default dialog displayer.
     * @return the default instance from lookup
     */
    public static DialogDisplayer getDefault() {
        return (DialogDisplayer)Lookup.getDefault().lookup(DialogDisplayer.class);
    }

    /** Subclass constructor. */
    protected DialogDisplayer() {}

    /** Notify the user of something in a message box, possibly with feedback.
     * <p>To support both GUI and non-GUI use, this method may be called
     * from any thread (providing you are not holding any locks), and
     * will block the caller's thread. In GUI mode, it will be run in the AWT
     * event thread automatically. If you wish to hold locks, or do not
     * need the result object immediately or at all, please make this call
     * asynchronously (e.g. from the request processor).
     * @param descriptor description of the notification
     * @return the option that caused the message box to be closed
     */
    public abstract Object notify(NotifyDescriptor descriptor);
    
    /** Get a new standard dialog.
     * The dialog is designed and created as specified in the parameter.
     * Anyone who wants a dialog with standard buttons and
     * standard behavior should use this method.
     * <p><strong>Do not cache</strong> the resulting dialog if it
     * is modal and try to reuse it! Always create a new dialog
     * using this method if you need to show a dialog again.
     * Otherwise previously closed windows can reappear.
     * @param descriptor general description of the dialog
     * @return the new dialog
     */
    public abstract Dialog createDialog(DialogDescriptor descriptor);

}
