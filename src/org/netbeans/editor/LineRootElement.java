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

import javax.swing.text.*;
import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;

/**
 * Line root element implementation.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

final class LineRootElement extends GapBranchElement {
    
    private static final String NAME
        = AbstractDocument.ParagraphElementName + "Root";
    
    private BaseDocument doc;
    
    private ArrayList addedLines = new ArrayList();
    
    LineRootElement(BaseDocument doc) {
        this.doc = doc;

        replace(0, 0, new Element[]{ new LineElement(this, 0, null) });
    }
    
    public Element getElement(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Invalid line index=" + index + " < 0");
        }
        int elementCount = getElementCount();
        if (index >= elementCount) {
            throw new IndexOutOfBoundsException("Invalid line index=" + index
                + " >= lineCount=" + elementCount);
        }
        
        return super.getElement(index);
    }
    
    UndoableEdit insertUpdate(int offset, int length) {
        int startOffset = offset;
        offset += length;
        LineElement lastCreatedLine = null;
        CharSeq docText = doc.getText();

        while (--offset >= startOffset) {
            if (docText.charAt(offset) == '\n') { // line break at offset
                lastCreatedLine = new LineElement(this, offset + 1, lastCreatedLine);
                addedLines.add(lastCreatedLine);
            }
        }
        
        if (lastCreatedLine != null) {
            Element[] linesAdded = new Element[addedLines.size()];
            addedLines.toArray(linesAdded);
            addedLines.clear();
            ObjectArrayUtilities.reverse(linesAdded);
            int index = getElementIndex(startOffset) + 1;
            replace(index, 0, linesAdded);
            return new Undo(index, EMPTY_ELEMENT_ARRAY, linesAdded);
        }
        
        return null;
    }

    UndoableEdit removeUpdate(int offset, int length) {
        int endOffset = offset + length;
        CharSeq docText = doc.getText();
        
        while (offset < endOffset) {
            if (docText.charAt(offset) == '\n') { // at least one line removed
                int index = getElementIndex(offset + 1);
                LineElement lineElem = getLineElement(index);
                int removeCount = 0;
                do {
                    removeCount++;
                    lineElem = lineElem.getNext();
                } while (lineElem != null
                    && lineElem.getStartOffset() <= endOffset
                );
                Element[] linesRemoved = new Element[removeCount];
                copyElements(index, index + removeCount, linesRemoved, 0);
                replace(index, linesRemoved.length, EMPTY_ELEMENT_ARRAY);
                return new Undo(index, linesRemoved, EMPTY_ELEMENT_ARRAY);
            }
            offset++;
        }
        
        return null;
    }

    protected void replace(int index, int removeCount, Element[] addedElems) {
        if (removeCount > 0) { // Unlink the line elements
            // allow possible garbage collection
            LineElement lastRemoved = getLineElement(index + removeCount - 1);
            if (index > 0) { // not removing the first one
                getLineElement(index - 1).setNext(lastRemoved.getNext());
            }
            lastRemoved.setNext(null);
        }

        super.replace(index, removeCount, addedElems);

        if (addedElems.length > 0) { // Link the new line elements
            // Relink the "next" field to first inserted element
            if (index > 0) {
                LineElement firstAdded = (LineElement)(addedElems[0]);
                ((LineElement)getElement(index - 1)).setNext(firstAdded);
            }

            int firstAfterAddedIndex = index + addedElems.length - removeCount;
            ((LineElement)addedElems[addedElems.length - 1]).setNext(
                (firstAfterAddedIndex < getElementCount())
                    ? getLineElement(firstAfterAddedIndex)
                    : null
            );
        }
    }
    
    public Document getDocument() {
        return doc;
    }

    public Element getParentElement() {
        return null;
    }

    public String getName() {
        return NAME;
    }

    public AttributeSet getAttributes() {
        return StyleContext.getDefaultStyleContext().getEmptySet();
    }

    public int getStartOffset() {
        return 0;
    }

    public int getEndOffset() {
        return doc.getLength() + 1;
    }

    public int getElementIndex(int offset) {
        if (offset == 0) { // NB uses this frequently to just get the parent
            return 0;
        }

        return super.getElementIndex(offset);
    }

    void invalidateAllSyntaxStateInfos() {
        int elemCount = getElementCount();
        for (int i = elemCount - 1; i >= 0; i--) {
            LineElement line = getValidLineElement(i);
            line.clearSyntaxStateInfo();
        }
    }
    
    /** Prepare syntax scanner so that it's ready to scan from requested
     * position.
     * @param text text segment to be used. Method ensures it will
     *  be filled so that <CODE>text.array</CODE> contains the character data
     *  <BR><CODE>text.offset</CODE> logically points to <CODE>reqPos</CODE>
     *  <BR><CODE>text.count</CODE> equals to <CODE>reqLen</CODE>.
     * @param syntax syntax scanner to be used
     * @param reqPos position to which the syntax should be prepared
     * @param reqLen length that will be scanned by the caller after the syntax 
     *   is prepared. The prepareSyntax() automatically preloads this area
     *   into the given text segment.
     * @param forceLastBuffer force the syntax to think that the scanned area is the last
     *  in the document. This is useful for forcing the syntax to process all the characters
     *  in the given area.
     * @param forceNotLastBuffer force the syntax to think that the scanned area is NOT
     *  the last buffer in the document. This is useful when the syntax will continue
     *  scanning on another buffer.
     */
    void prepareSyntax(Segment text, Syntax syntax, int reqPos, int reqLen,
    boolean forceLastBuffer, boolean forceNotLastBuffer) throws BadLocationException {

        if (reqPos < 0 || reqLen < 0 || reqPos + reqLen > doc.getLength()) {
            throw new BadLocationException("reqPos=" + reqPos
                + ", reqLen=" + reqLen + ", doc.getLength()=" + doc.getLength(),
                -1 // getting rid of it
            );
        }

        // Find line element that covers the reqPos
        int reqPosLineIndex = getElementIndex(reqPos);
        LineElement reqPosLineElem = getValidSyntaxStateInfoLineElement(reqPosLineIndex);

        // Get nearest previous syntax mark
        int lineStartOffset = reqPosLineElem.getStartOffset();
        int preScan;
        Syntax.StateInfo stateInfo = reqPosLineElem.getSyntaxStateInfo();
        if (reqPosLineIndex > 0) {
            preScan = stateInfo.getPreScan();
        } else { // index is 0
            preScan = 0;
            if (stateInfo != null) {
                throw new IllegalStateException("stateInfo=" + stateInfo);
            }
        }

        // load syntax segment
        int intraLineLength = reqPos - lineStartOffset;
        doc.getText(lineStartOffset - preScan, preScan + intraLineLength + reqLen, text);
        text.offset += preScan;
        text.count -= preScan;

        // load state into syntax scanner - will scan from mark up to reqPos
        syntax.load(stateInfo, text.array, text.offset, intraLineLength, false, reqPos);
// [CAUTION] instead of false used to be forceNotLastBuffer ? false : (reqPos >= docLen)

        // ignore tokens until reqPos is reached
        while (syntax.nextToken() != null) { }

        text.offset += intraLineLength;
        text.count -= intraLineLength;
        boolean forceLB = forceNotLastBuffer
            ? false
            : (forceLastBuffer || (reqPos + reqLen >= getDocument().getLength()));

        syntax.relocate(text.array, text.offset, text.count, forceLB, reqPos + reqLen);
    }

    private LineElement getValidSyntaxStateInfoLineElement(int lineIndex) throws BadLocationException {
        LineElement lineElem = getValidLineElement(lineIndex);
        Syntax.StateInfo stateInfo = lineElem.getSyntaxStateInfo();

        if (lineIndex > 0 && stateInfo == null) { // need to update
            // Find the last line with the valid state info
            int validLineIndex = lineIndex - 1; // is >= 0
            LineElement validLineElem = null;
            while (validLineIndex > 0) {
                validLineElem = getValidLineElement(validLineIndex);
                stateInfo = validLineElem.getSyntaxStateInfo() ;
                if (stateInfo != null) {
                    break;
                }
                validLineIndex--;
            }

            /* validLineIndex now contains index of last line
             * that has valid syntax state info. Or it's zero (always valid).
             * stateInfo contains state info of last valid line
             * or undefined value if validLineIndex == 0.
             * validLineElem contains valid line element
             * or undefined value if validLineIndex == 0.
             */

            Segment text = DocumentUtilities.SEGMENT_CACHE.getSegment();
            try {
                Syntax syntax = doc.getFreeSyntax();
                try {
                    int lineElemOffset = lineElem.getStartOffset();
                    int validLineOffset = 0;
                    int preScan = 0;
                    if (validLineIndex > 0) {
                        validLineOffset = validLineElem.getStartOffset();
                        preScan = stateInfo.getPreScan();
                    } else { // validLineIndex == 0
                        stateInfo = null;
                    }

                    doc.getText(validLineOffset - preScan,
                        (lineElemOffset - validLineOffset) + preScan,
                        text
                    );

                    text.offset += preScan;
                    text.count -= preScan;
                    /* text segment contains all the required data including preScan
                     * but "officially" it points to validLineOffset offset.
                     */
                    
                    syntax.load(stateInfo, text.array, text.offset,
                        text.count, false, lineElemOffset);

                    int textEndOffset = text.offset + text.count;
                    do {
                        validLineIndex++;
                        validLineElem = getValidLineElement(validLineIndex);
                        validLineOffset = validLineElem.getStartOffset();
                        syntax.relocate(text.array, syntax.getOffset(),
                            textEndOffset - syntax.getOffset(),
                            false, validLineOffset
                        );
                        
                        while (syntax.nextToken() != null) {
                            // ignore returned tokens
                        }
                        
                        validLineElem.updateSyntaxStateInfo(syntax);
                        
                    } while (validLineIndex != lineIndex);
                    
                } finally {
                    doc.releaseSyntax(syntax);
                }
            } finally {
                DocumentUtilities.SEGMENT_CACHE.releaseSegment(text);
            }
        }
        
        return lineElem;
    }

    /**
     * Fix state infos after insertion/removal.
     * @param offset offset of the modification
     * @param length length of the modification. It's lower than zero for removals.
     * @return offset of the last line where the syntax stateinfo was modified.
     */
    int fixSyntaxStateInfos(int offset, int length) {
        if (offset < 0) {
            throw new IllegalStateException("offset=" + offset);
        }

        int lineIndex = getElementIndex(offset);
        int addedLinesCount = (length > 0)
            ? getElementIndex(offset + length) - lineIndex
            : 0;

        LineElement lineElem = getValidLineElement(lineIndex);
//        System.out.println("Fixing lineIndex=" + lineIndex + ", addedLinesCount=" + addedLinesCount);
        Segment text = DocumentUtilities.SEGMENT_CACHE.getSegment();
        try {
            Syntax syntax = doc.getFreeSyntax();
            try {
                int docLastLineIndex = getElementCount() - 1;
                if (lineIndex == docLastLineIndex) { // modification in last line
                    if (lineIndex == 0 && lineElem.getSyntaxStateInfo() != null) {
//                        System.out.println("CCCCCCCCCClearing syntax state info");
                        lineElem.clearSyntaxStateInfo();
                    }

                    return doc.getLength();
                }
                
                int maybeMatchLineIndex = Math.min(
                    lineIndex + addedLinesCount + 1, docLastLineIndex);
                
                Syntax.StateInfo stateInfo = null;
                int lineStartOffset = 0;
                int preScan = 0;
                if (lineIndex > 0) {
                    stateInfo = lineElem.getSyntaxStateInfo();
                    preScan = stateInfo.getPreScan();
                    lineStartOffset = lineElem.getStartOffset();
                }

                lineIndex++; // line index now points to line that follows the modified one
                LineElement nextLineElem = getValidLineElement(lineIndex); // should be valid
                int nextLineStartOffset = nextLineElem.getStartOffset();

                doc.getText(lineStartOffset - preScan,
                    (nextLineStartOffset - lineStartOffset) + preScan, text);

                text.offset += preScan;
                text.count -= preScan;

                syntax.load(stateInfo, text.array, text.offset, text.count,
                    false, nextLineStartOffset);

                int lineCount = getElementCount();
                while (true) {
                    while (syntax.nextToken() != null) {
                        // go through the tokens
                    }

                    if (lineIndex >= maybeMatchLineIndex) {
                        stateInfo = nextLineElem.getSyntaxStateInfo();
                        if (stateInfo != null 
                            && syntax.compareState(stateInfo) == Syntax.EQUAL_STATE
                        ) {
//                            System.out.println("SAME-INFO lineIndex=" + lineIndex + ", stateInfo=" + ((Syntax.BaseStateInfo)nextLineElem.getSyntaxStateInfo()).toString(syntax));
                            break;
                        }
                    }
                    
                    nextLineElem.updateSyntaxStateInfo(syntax);
//                    System.out.println("FFFFixed lineIndex=" + lineIndex + ", offset=" + nextLineElem.getStartOffset() + ", stateInfo=" + ((Syntax.BaseStateInfo)nextLineElem.getSyntaxStateInfo()).toString(syntax));
                    
                    lineIndex++;
                    if (lineIndex >= lineCount) { // still not match at begining of last line
                        return doc.getLength();
                    }

                    lineElem = nextLineElem;
                    lineStartOffset = nextLineStartOffset;

                    nextLineElem = getValidLineElement(lineIndex);
                    nextLineStartOffset = nextLineElem.getStartOffset();
                    
                    preScan = syntax.getPreScan();
                    doc.getText(lineStartOffset - preScan,
                        (nextLineStartOffset - lineStartOffset) + preScan, text);

                    text.offset += preScan;
                    text.count -= preScan;
                    
                    syntax.relocate(text.array, text.offset, text.count,
                        false, nextLineStartOffset);
                }
                
                return lineStartOffset;
                
            } finally {
                doc.releaseSyntax(syntax);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.toString());
        } finally {
            DocumentUtilities.SEGMENT_CACHE.releaseSegment(text);
        }
    }

    private LineElement getLineElement(int index) {
        return (LineElement)getElement(index);
    }
    
    private LineElement getValidLineElement(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= getElementCount()) {
            throw new IllegalArgumentException("lineIndex=" + lineIndex
                + ", lineCount=" + getElementCount());
        }
        
        return getLineElement(lineIndex);
    }


    /**
     * @param offset to be examined.
     * @return offset that will be high enough to ensure that the given offset
     *  will be covered by token that can be returned from the syntax.nextToken()
     *  assuming that the syntax will be prepared with the returned token.
     *  <BR>It's not guaranteed how much bigger the returned offset will be.
     */
    int getTokenSafeOffset(int offset) {
        if (offset == 0) { // no valid state-info at offset 0
            return offset;
        }

        try {
            int lineIndex = getElementIndex(offset);
            LineElement lineElem = getValidSyntaxStateInfoLineElement(lineIndex);
            int lineStartOffset = lineElem.getStartOffset();
            Syntax.StateInfo stateInfo = lineElem.getSyntaxStateInfo();
            if (offset == lineStartOffset && stateInfo.getPreScan() == 0) {
                // can be done with the given offset
//            System.out.println("getTokenSafeOffset() offset=" + offset + " unchanged");
                return offset;
            }

            // go to next line and maybe further for tokens
            // crossing several lines
            int lineCount = getElementCount();
            while (++lineIndex < lineCount) {
                lineElem = getValidSyntaxStateInfoLineElement(lineIndex);
                lineStartOffset = lineElem.getStartOffset();
                stateInfo = lineElem.getSyntaxStateInfo();
                if (lineStartOffset - stateInfo.getPreScan() >= offset) {
//        System.out.println("getTokenSafeOffset() offset=" + offset + " to safe offset=" + lineStartOffset + ", preScan=" + stateInfo.getPreScan() + ", lineIndex=" + lineIndex);

                    return lineStartOffset;
                }
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.toString());
        }

//        System.out.println("getTokenSafeOffset() offset=" + offset + " to DOCLEN=" + doc.getLength());
        return doc.getLength();
    }
    
}
