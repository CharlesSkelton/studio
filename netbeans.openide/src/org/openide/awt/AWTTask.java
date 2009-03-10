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

package org.openide.awt;

/** A special task designed to run in AWT thread.
 * It will fire itself immediatelly.
 */
final class AWTTask extends org.openide.util.Task {
    private boolean executed;

    public AWTTask (Runnable r) {
        super (r);
        org.openide.util.Mutex.EVENT.readAccess (this);
    }

    public void run () {
        if (!executed) {
            super.run ();
            executed = true;
        }
    }

    public void waitFinished () {
        if (javax.swing.SwingUtilities.isEventDispatchThread ()) {
            run ();
        } else {
            super.waitFinished ();
        }
    }
}
