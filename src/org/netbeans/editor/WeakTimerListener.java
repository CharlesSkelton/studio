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

package org.netbeans.editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

/**
 * Action listener that has a weak reference
 * to the source action listener so it doesn't prevent
 * it to be garbage collected.
 * The calls to the <code>actionPerformed</code> are automatically
 * propagated to the source action listener.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public class WeakTimerListener implements ActionListener {

    private WeakReference ref;

    private boolean stopTimer;

    /** Construct new listener with automatic timer stopping.
     */
    public WeakTimerListener(ActionListener source) {
        this(source, true);
    }

    /** Construct new listener.
     * @param source source action listener to which this listener delegates.
     * @param stopTimer whether the timer should be stopped automatically when
     *  the timer fires and the source listener was garbage collected.
     */
    public WeakTimerListener(ActionListener source, boolean stopTimer) {
        this.ref = new WeakReference(source);
        this.stopTimer = stopTimer;
    }

    public void actionPerformed(ActionEvent evt) {
        ActionListener src = (ActionListener)ref.get();
        if (src != null) {
            src.actionPerformed(evt);

        } else { // source listener was garbage collected
            if (evt.getSource() instanceof Timer) {
                Timer timer = (Timer)evt.getSource();
                timer.removeActionListener(this);

                if (stopTimer) {
                    timer.stop();
                }
            }
        }
    }

}
