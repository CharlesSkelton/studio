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

import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Content of the document.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

final class DocumentContent implements AbstractDocument.Content, CharSeq, GapStart {
    
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    /**
     * Invalid undoable edit being used to mark that the line undo was already
     * processed. It must never be undone/redone as it's used in a flyweight
     * way but the undomanager's operation changes states of undoable edits
     * being undone/redone.
     */
    private static final UndoableEdit INVALID_EDIT = new AbstractUndoableEdit();
        
    /** Vector holding the marks for the document */
    private final MarkVector markVector;
    
    /** Array with gap holding the text of the document */
    private char[] charArray;

    /** Start index of the gap */
    private int gapStart;

    /** Length of the gap */
    private int gapLength;
    
    // Unfortunately needed for syntax updates
    private BaseDocument doc;
    
    DocumentContent() {
        charArray = EMPTY_CHAR_ARRAY;
        markVector = new MarkVector();
        
        // Insert implied '\n'
        insertText(0, "\n");
    }
    
    public final int getGapStart() { // to implement GapStart
        return gapStart;
    }

    public UndoableEdit insertString(int offset, String text)
    throws BadLocationException {

        checkBounds(offset, 0, length() - 1);
        return new Edit(offset, text);
    }
    
    public UndoableEdit remove(int offset, int length)
    throws BadLocationException {

        checkBounds(offset, length, length() - 1);
        return new Edit(offset, length);
    }

    public Position createPosition(int offset) throws BadLocationException {
        return new BasePosition(createMark(offset));
    }

    public Position createBiasPosition(int offset, Position.Bias bias)
    throws BadLocationException {
        return new BasePosition(createBiasMark(offset, bias));
    }

    MultiMark createBiasMark(int offset, Position.Bias bias) {
        return markVector.insert(markVector.createBiasMark(offset, bias));
    }

    MultiMark createMark(int offset) {
        return markVector.insert(markVector.createMark(offset));
    }

    public int length() {
        return charArray.length - gapLength;
    }
    
    public void getChars(int offset, int length, Segment chars)
    throws BadLocationException {

        checkBounds(offset, length, length());
        
        if ((offset + length) <= gapStart) { // completely below gap
            chars.array = charArray;
            chars.offset = offset;
            
        } else if (offset >= gapStart) { // completely above gap
            chars.array = charArray;
            chars.offset = offset + gapLength;
            
        } else { // spans the gap, must copy
            chars.array = copySpanChars(offset, length);
            chars.offset = 0;
        }
        
        chars.count = length;
    }

    public String getString(int offset, int length)
    throws BadLocationException {

        checkBounds(offset, length, length());
        return getText(offset, length);
    }
    
    String getText(int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalStateException("offset=" + offset + ", length=" + length);
        }

        String ret;
        if ((offset + length) <= gapStart) { // completely below gap
            ret = new String(charArray, offset, length);
            
        } else if (offset >= gapStart) { // completely above gap
            ret = new String(charArray, offset + gapLength, length);
            
        } else { // spans the gap, must copy
            ret = new String(copySpanChars(offset, length));
        }
        
        return ret;
    }

    public char charAt(int index) {
        return charArray[getRawIndex(index)];
    }
    
    void compact() {
        if (gapLength > 0) {
            int newLength = charArray.length - gapLength;
            char[] newCharArray = new char[newLength];
            int gapEnd = gapStart + gapLength;
            System.arraycopy(charArray, 0, newCharArray, 0, gapStart);
            System.arraycopy(charArray, gapEnd, newCharArray, gapStart, 
                charArray.length - gapEnd);
            charArray = newCharArray;
            gapStart = charArray.length;
            gapLength = 0;
        }
        
        markVector.compact();
    }
    
    private int getRawIndex(int index) {
        return (index < gapStart) ? index : (index + gapLength);
    }
    
    private void moveGap(int index) {
        if (index <= gapStart) { // move gap down
            int moveSize = gapStart - index;
            System.arraycopy(charArray, index, charArray,
                gapStart + gapLength - moveSize, moveSize);
            gapStart = index;

        } else { // above gap
            int gapEnd = gapStart + gapLength;
            int moveSize = index - gapStart;
            System.arraycopy(charArray, gapEnd, charArray, gapStart, moveSize);
            gapStart += moveSize;
        }
    }
    
    private void enlargeGap(int extraLength) {
        int newLength = Math.max(10, charArray.length * 3 / 2 + extraLength);
        int gapEnd = gapStart + gapLength;
        int afterGapLength = (charArray.length - gapEnd);
        int newGapEnd = newLength - afterGapLength;
        char[] newCharArray = new char[newLength];
        System.arraycopy(charArray, 0, newCharArray, 0, gapStart);
        System.arraycopy(charArray, gapEnd, newCharArray, newGapEnd, afterGapLength);
        charArray = newCharArray;
        gapLength = newGapEnd - gapStart;
    }

    private char[] copyChars(int offset, int length) {
        char[] ret;
        if ((offset + length) <= gapStart) { // completely below gap
            ret = new char[length];
            System.arraycopy(charArray, offset, ret, 0, length);
            
        } else if (offset >= gapStart) { // completely above gap
            ret = new char[length];
            System.arraycopy(charArray, offset + gapLength, ret, 0, length);
            
        } else { // spans the gap, must copy
            ret = copySpanChars(offset, length);
        }
        
        return ret;
    }

    private char[] copySpanChars(int offset, int length) {
        char[] ret = new char[length];
        int belowGap = gapStart - offset;
        System.arraycopy(charArray, offset, ret, 0, belowGap);
        System.arraycopy(charArray, gapStart + gapLength,
            ret, belowGap, length - belowGap);
        return ret;
    }

    void insertText(int offset, String text) {
        int textLength = text.length();
        int extraLength = textLength - gapLength;
        if (extraLength > 0) {
            enlargeGap(extraLength);
        }
        if (offset != gapStart) {
            moveGap(offset);
        }
        text.getChars(0, textLength, charArray, gapStart);
        gapStart += textLength;
        gapLength -= textLength;
    }
    
    void removeText(int offset, int length) {
        if (offset >= gapStart) { // completely over gap
            if (offset > gapStart) {
                moveGap(offset);
            }

        } else { // completely below gap or spans the gap
            int endOffset = offset + length;
            if (endOffset <= gapStart) {
                if (endOffset < gapStart) {
                    moveGap(endOffset);
                }
                gapStart -= length;
                
            } else { // spans gap
                gapStart = offset;
            }
        }

        gapLength += length;
    }

    private void checkBounds(int offset, int length, int limitOffset)
    throws BadLocationException {

	if (offset < 0) {
	    throw new BadLocationException("Invalid offset=" + offset, offset);
	}
        if (length < 0) {
            throw new BadLocationException("Invalid length" + length, length);
        }
	if (offset + length > limitOffset) {
	    throw new BadLocationException(
                "docLength=" + (length() - 1)
                + ":  Invalid offset"
                + ((length != 0) ? "+length" : "")
                + "=" + (offset + length),
                (offset + length)
            );
	}
    }
    
    void setBaseDocument(BaseDocument doc) {
        this.doc = doc;
    }
    
    class Edit extends AbstractUndoableEdit {
        
        /** Constructor used for insert.
         * @param offset offset of insert.
         * @param text inserted text.
         */
        Edit(int offset, String text) {
            this.offset = offset;
            this.length = text.length();
            this.text = text;

            undoOrRedo(length, false); // pretend redo
            
        }
        
        /** Constructor used for remove.
         * @param offset offset of remove.
         * @param length length of the removed text.
         */
        Edit(int offset, int length) {
            this.offset = offset;
            this.length = -length;
            
            // Added to make sure the text is not inited later at unappropriate time
            this.text = getText(offset, length);

            undoOrRedo(-length, false); // pretend redo
            
        }
        
        private int offset;
        
        private int length;
        
        private String text;
        
        private MarkVector.Undo markVectorUndo;
        
        private UndoableEdit lineUndo;
        
        private int syntaxUpdateOffset;
        
        public void undo() throws CannotUndoException {
            super.undo();

            undoOrRedo(-length, true);
        }
        
        public void redo() throws CannotRedoException {
            super.redo();

            undoOrRedo(length, false);
        }
        
        private LineRootElement getLineRoot() {
            return (LineRootElement)doc.getParagraphElement(0).getParentElement();
        }
        
        private void undoOrRedo(int len, boolean undo) {
            
            boolean lineUndoAssigned = false; // whether lineUndo was assigned in this body
            
            // Line undo information must be obtained before doing the actual remove
            if (lineUndo == null && len < 0) {
                lineUndo = getLineRoot().removeUpdate(offset, -len);

                if (lineUndo == null) { // must be valid value
                    lineUndo = INVALID_EDIT; // must not be undone - can't be flyweight!!!
                }
                
                lineUndoAssigned = true;
            }

            // Fix text content
            if (len < 0) { // do remove
                removeText(offset, -len);
            } else { // do insert
                insertText(offset, text);
            }
            
            // Update marks
            markVectorUndo = markVector.update(offset, len, markVectorUndo);
            // Update document's compatible marks storage - no undo
            doc.marksStorage.update(offset, len, null);

            // Fix line elements structure
            if (lineUndo != null) {
                if (!lineUndoAssigned && lineUndo != INVALID_EDIT) {
                    if (undo) {
                        lineUndo.undo();
                    } else {
                        lineUndo.redo();
                    }
                }

            } else { // line undo not yet assigned
                if (len < 0) { // had to be assigned before
                    throw new IllegalStateException();
                }
                lineUndo = getLineRoot().insertUpdate(offset, length);
                    
                if (lineUndo == null) { // must be valid value
                    lineUndo = INVALID_EDIT; // must not be undone - can't be flyweight!!!
                }
                
                lineUndoAssigned = true;
            }
                
            // Update syntax state infos (based on updated text and line structures
            syntaxUpdateOffset = getLineRoot().fixSyntaxStateInfos(offset, len);
        }

        /**
         * @return text of the modification.
         */
        final String getUndoRedoText() {
            return text;
        }
        
        final int getSyntaxUpdateOffset() {
            return syntaxUpdateOffset;
        }

    }
}
