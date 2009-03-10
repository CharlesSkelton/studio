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

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

/**
* Position in document. This is enhanced version of 
* Swing <CODE>Position</CODE> interface. It supports
* insert after feature. If Position has
* <CODE>insertAfter</CODE> flag set and text is inserted
* right at the mark's position, the position will NOT move.
*
* @author Miloslav Metelka
* @version 1.00
*/

class BasePosition implements Position {

    /** The mark that serves this position */
    private MultiMark mark;

    BasePosition(MultiMark mark) throws BadLocationException {
        this.mark = mark;
    }

    /** Get offset in document for this position */
    public int getOffset() {
        return mark.getOffset();
    }

    /** Remove mark in finalize method */
    protected void finalize() throws Throwable {
        mark.dispose();
        super.finalize();
    }

}
