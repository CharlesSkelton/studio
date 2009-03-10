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

package org.openide.text;

import javax.swing.text.*;
import javax.swing.event.*;

import org.openide.util.WeakListener;

/** Position that stays at the same place if someone inserts
* directly to its offset.
*
* @author Jaroslav Tulach
*/
class BackwardPosition extends Object
    implements Position, DocumentListener {
    /** positions current offset */
    private int offset;

    /** Constructor.
    */
    private BackwardPosition(int offset) {
        this.offset = offset;
    }

    /** @param doc document
    * @param offset offset
    * @return new instance of the position
    */
    public static Position create (Document doc, int offset) {
        BackwardPosition p = new BackwardPosition (offset);
        doc.addDocumentListener (WeakListener.document (p, doc));
        return p;
    }

    //
    // Position
    //

    /** @return the offset
    */
    public int getOffset () {
        return offset;
    }

    //
    // document listener
    //


    /** Updates */
    public void insertUpdate(DocumentEvent e) {
        // less, not less and equal
        if (e.getOffset () < offset) {
            offset += e.getLength ();
        }
    }

    /** Updates */
    public void removeUpdate(DocumentEvent e) {
        int o = e.getOffset ();
        if (o < offset) {
            offset -= e.getLength ();
            // was the position in deleted range? => go to its beginning
            if (offset < o) {
                offset = o;
            }
        }
    }

    /** Nothing */
    public void changedUpdate(DocumentEvent e) {
    }
}
