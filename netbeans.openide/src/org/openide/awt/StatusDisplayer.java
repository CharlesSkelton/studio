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

package org.openide.awt;

import javax.swing.event.ChangeListener;
import org.openide.util.Lookup;

/** Permits control of a status line.
 * The default instance may correspond to the NetBeans status line in the main window.
 * @author Jesse Glick
 * @since 3.14
 */
public abstract class StatusDisplayer {

    /** Get the default status displayer.
     * @return the default instance from lookup
     */
    public static StatusDisplayer getDefault() {
        return (StatusDisplayer)Lookup.getDefault().lookup(StatusDisplayer.class);
    }

    /** Subclass constructor. */
    protected StatusDisplayer() {}

    /** Get the currently displayed text.
     * <p>Modules should <strong>not</strong> need to call this method.
     * If you think you really do, please explain why on nbdev.
     * The implementation of the GUI component (if any) which displays
     * the text naturally needs to call it.
     * @return some text
     */
    public abstract String getStatusText();

    /** Show text in the status line.
     * Can be called at any time, but remember the text may not be updated
     * until the AWT event queue is ready for it - so if you are hogging
     * the event queue the text will not appear until you release it
     * (finish your work or display a modal dialog, for example).
     * @param text the text to be shown
     */
    public abstract void setStatusText(String text);
    
    /** Add a listener for when the text changes.
     * @param l a listener
     */
    public abstract void addChangeListener(ChangeListener l);
    
    /** Remove a listener for the text.
     * @param l a listener
     */
    public abstract void removeChangeListener(ChangeListener l);

}
