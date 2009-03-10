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

import javax.swing.text.Document;
/**
 * Document that supports atomic locking allows
 * for transactional modifications.
 * The document is write-locked during the whole atomic
 * operation. All the operations since
 * the begining of the atomic operation
 * can be undone by using atomicUndo().
 * Typical scenario of the operation
 * is the following: <PRE>
 *   doc.atomicLock();
 *   try {
 *     ...
 *     modification1
 *     modification2
 *     ...
 *   } catch (BadLocationException e) {
 *     // something went wrong - undo till begining
 *     doc.atomicUndo();
 *   } finally {
 *     doc.atomicUnlock();
 *   }
 *   <PRE>
 *   <P>The external clients can watch for atomic operations
 *   by registering an listener through
 *   {@link addAtomicLockListener(AtomicLockListener)}
 */
public interface AtomicLockDocument extends Document {

    public void atomicLock();
    
    public void atomicUnlock();
    
    public void atomicUndo();
    
    public void addAtomicLockListener(AtomicLockListener l);
    
    public void removeAtomicLockListener(AtomicLockListener l);

}
