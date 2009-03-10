/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.editor;

import java.util.EventListener;

/**
 * Listener for begining and end of the atomic
 * locking. It can be used to optimize the document
 * listeners if a large amounts of edits are performed
 * in an atomic change. For example if there's
 * a timer restarted after each document modification
 * to update an external pane showing the document structure
 * after 2000ms past the last modification occurred
 * then there could be a following listener used:<PRE>
 *  class MultiListener implements DocumentListener, AtomicLockListener {
 *
 *    private boolean atomic; // whether in atomic change
 *
 *    public void insertUpdate(DocumentEvent evt) {
 *      modified(evt);
 *    }
 *
 *    public void removeUpdate(DocumentEvent evt) {
 *      modified(evt);
 *    }
 *
 *    public void changedUpdate(DocumentEvent evt) {
 *    }
 *
 *    private void modified(DocumentEvent evt) {
 *      if (!atomic) {
 *        restartTimer(); // restart the timer
 *      }
 *    }
 *
 *    public void atomicLock(AtomicLockEvent evt) {
 *      atomic = true;
 *    }
 *
 *    public void atomicUnlock(AtomicLockEvent evt) {
 *      atomic = false;
 *    }
 *
 *  }
 *  <PRE>
 */
public interface AtomicLockListener extends EventListener {

    public void atomicLock(AtomicLockEvent evt);
    
    public void atomicUnlock(AtomicLockEvent evt);
    
}
