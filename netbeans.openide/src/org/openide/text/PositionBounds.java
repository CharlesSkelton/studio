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

import java.io.*;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;

import org.openide.util.NbBundle;

/** A range bounded by two {@link PositionRef}s.
*
* @author Petr Hamernik
*/
public final class PositionBounds extends Object implements Serializable {
    static final long serialVersionUID = 3338509625548836633L;

    /** Begin */
    private PositionRef begin;

    /** End */
    private PositionRef end;

    /** Creates new <code>PositionBounds</code>.
     * @param begin the start position of the range
     * @param end the end position of the range
    */
    public PositionBounds(PositionRef begin, PositionRef end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * Get the starting position of this range.
     * @return the begin position
     */
    public PositionRef getBegin() {
        return begin;
    }

    /**
     * Get the ending position of this range.
     * @return the end position
     */
    public PositionRef getEnd() {
        return end;
    }

    /** Replaces the text contained in this range.
    * This replacement is done atomically, and so is preferable to manual inserts & removes.
    * <p>If you are running this from user-oriented code, you may want to wrap it in {@link NbDocument#runAtomicAsUser}.
    * @param text new text to insert over existing text
    * @exception IOException if any problem occurred during document loading (if that was necessary)
    * @exception BadLocationException if the positions are out of the bounds of the document
    */
    public void setText(final String text) throws IOException, BadLocationException {
        final CloneableEditorSupport editor = begin.getCloneableEditorSupport();
        final StyledDocument doc = editor.openDocument();
        final BadLocationException[] hold = new BadLocationException[] { null };
        Runnable run = new Runnable() {
                           public void run() {
                               try {
                                   int p1 = begin.getOffset();
                                   int p2 = end.getOffset();
                                   int len = text.length();
                                   if (len == 0) {  // 1) set empty string
                                       if (p2 > p1)
                                           doc.remove(p1, p2 - p1);
                                   }
                                   else {                     // 2) set non empty string
                                       if (p2 - p1 >= 2) {
                                           doc.insertString(p1 + 1, text, null);
                                           doc.remove(p1 + 1 + len, p2 - p1 - 1);
                                           doc.remove(p1, 1);
                                       } else {
                                           // zero or exactly one character:
                                           // adjust the positions if they are
                                           // biased to not absorb the text inserted at the start/end
                                           // it would be ridiculous not to have text set by setText
                                           // be part of the bounds.
                                           doc.insertString(p1, text, null);
                                           if (p2 > p1)
                                               doc.remove(p1 + len, p2 - p1);
                                           if (begin.getOffset() != p1) 
                                               begin = editor.createPositionRef(p1, begin.getPositionBias());
                                           if (end.getOffset() - p1 != len) 
                                               end = editor.createPositionRef(p1 + len, end.getPositionBias());
				       }
                                   }
                               }
                               catch (BadLocationException e) {
                                   hold[0] = e;
                               }
                           }
                       };
        NbDocument.runAtomic (doc, run);
        if (hold[0] != null)
            throw hold[0];
    }

    /** Inserts the text after this PositionBounds.
    * @param text The text to insert. The text must not be empty.
    * @return the range of inserted text.
    * @exception IOException if any problem occurred during document loading (if that was necessary)
    * @exception BadLocationException if the positions are out of the bounds of the document
    */
    public PositionBounds insertAfter(final String text) throws IOException, BadLocationException {
        if (text.length() == 0)
            throw new BadLocationException(NbBundle.getBundle(PositionBounds.class).getString("MSG_Empty_string"),
                                           begin.getOffset());

        final CloneableEditorSupport editor = begin.getCloneableEditorSupport();
        final StyledDocument doc = editor.openDocument();
        final Object[] hold = new Object[] { null, null };

        Runnable run = new Runnable() {
                           public void run() {
                               synchronized (editor.getLock()) {
                                   /* editor.getLock(): fixes deadlock - this lock is acquired later anyhow,
                                   so we are changing the order in which the locks are acquired */
                                   try {
                                       int p1 = end.getOffset();
                                       int p2 = p1 + text.length();
                                       doc.insertString (p1, text, null);
                                       end = editor.createPositionRef(p1, end.getPositionBias());
                                       PositionRef posBegin = editor.createPositionRef(p1, Position.Bias.Forward);
                                       PositionRef posEnd = editor.createPositionRef(p2, Position.Bias.Backward);
                                       hold[1] = new PositionBounds(posBegin, posEnd);
                                   }
                                   catch (BadLocationException e) {
                                       hold[0] = e;
                                   }
                               }
                           }
                       };

        NbDocument.runAtomic (doc, run);
        if (hold[0] != null)
            throw (BadLocationException) hold[0];
        else
            return (PositionBounds) hold[1];
    }

    /** Finds the text contained in this range.
    * @return the text
    * @exception IOException if any I/O problem occurred during document loading (if that was necessary)
    * @exception BadLocationException if the positions are out of the bounds of the document
    */
    public String getText() throws BadLocationException, IOException {
        StyledDocument doc = begin.getCloneableEditorSupport().openDocument();
        int p1 = begin.getOffset();
        int p2 = end.getOffset();
        return doc.getText(p1, p2 - p1);
    }

    /* @return the bounds as the string. */
    public String toString() {
        StringBuffer buf = new StringBuffer("Position bounds["); // NOI18N
        try {
            String content = getText();
            buf.append(begin);
            buf.append(","); // NOI18N
            buf.append(end);
            buf.append(",\""); // NOI18N
            buf.append(content);
            buf.append("\""); // NOI18N
        }
        catch (IOException e) {
            buf.append("Invalid: "); // NOI18N
            buf.append(e.getMessage());
        }
        catch (BadLocationException e) {
            buf.append("Invalid: "); // NOI18N
            buf.append(e.getMessage());
        }
        buf.append("]"); // NOI18N
        return buf.toString();
    }
}
