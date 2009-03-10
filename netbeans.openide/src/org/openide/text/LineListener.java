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

import org.openide.util.WeakListener;

/** Listener to changes in the document.
*
* @author Jaroslav Tulach
*/
final class LineListener extends Object
    implements javax.swing.event.DocumentListener {
    /** original count of lines */
    private int orig;
    /** document to work with */
    public final StyledDocument doc;
    /** root element of all lines */
    private Element root;
    /** last tested amount of lines */
    private int lines;
    /** operations on lines */
    private LineStruct struct;

    
    /** Support necessary for getting Set of lines*/
    CloneableEditorSupport support;
    
    /** Creates new LineListener */
    public LineListener (StyledDocument doc, CloneableEditorSupport support) {
        this.doc = doc;
        this.struct = new LineStruct ();
        root = NbDocument.findLineRootElement (doc);
        orig = lines = root.getElementCount ();
        this.support = support;
        
        doc.addDocumentListener(WeakListener.document (this, doc));
    }

    /** Getter for amount of lines */
    public int getOriginalLineCount () {
        return orig;
    }

    /** Convertor between old and new line sets */
    public int getLine (int i) {
        return struct.originalToCurrent (i);
    }

    public void removeUpdate(javax.swing.event.DocumentEvent p0) {
        int elem = root.getElementCount ();
        int delta = lines - elem;
        lines = elem;

        int lineNumber = NbDocument.findLineNumber (doc, p0.getOffset ());
        
        if (delta > 0) {
            struct.deleteLines (
                lineNumber,
                delta
            );
        }        
        if ( support == null)
            return;
        Line.Set set = support.getLineSet ();
        if (!(set instanceof DocumentLine.Set))
            return;

        // Notify lineSet there was changed range of lines.
        ((DocumentLine.Set)set).linesChanged(lineNumber, lineNumber+delta, p0);
        
        if (delta > 0) {
            // Notify Line.Set there was moved range of lines.
            ((DocumentLine.Set)set).linesMoved(lineNumber, elem);
        }
    }

    public void changedUpdate(javax.swing.event.DocumentEvent p0) {
    }

    public void insertUpdate(javax.swing.event.DocumentEvent p0) {
        int elem = root.getElementCount ();
        
        int delta = elem - lines;
        lines = elem;

        int lineNumber = NbDocument.findLineNumber (doc, p0.getOffset ());
        
        if (delta > 0) {
            struct.insertLines (
                lineNumber,
                delta
            );
        }
        
        if ( support == null)
            return;
        Line.Set set = support.getLineSet ();
        if (!(set instanceof DocumentLine.Set))
            return;
        
        // Nptify Line.Set there was changed range of lines.
        ((DocumentLine.Set)set).linesChanged(lineNumber, lineNumber, p0);
        
        if (delta > 0) {
            // Notify Line.Set there was moved range of lines.
            ((DocumentLine.Set)set).linesMoved(lineNumber, elem);
        }
    }
    
}
