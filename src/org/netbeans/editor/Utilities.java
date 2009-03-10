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

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
* Various useful editor functions. Some of the methods have
* the same names and signatures like in javax.swing.Utilities but
* there is also many other useful methods.
* All the methods are static so there's no reason to instantiate Utilities.
*
* All the methods working with the document rely on that it is locked against
* modification so they don't acquire document read/write lock by themselves
* to guarantee the full thread safety of the execution.
* It's the user's task to lock the document appropriately
* before using methods described here.
*
* Most of the methods require org.netbeans.editor.BaseDocument instance
* not just the javax.swing.text.Document.
* The reason for that is to mark that the methods work on BaseDocument
* instances only, not on generic documents. To convert the Document
* to BaseDocument the simple conversion (BaseDocument)target.getDocument()
* can be done or the method getDocument(target) can be called.
* There are also other conversion methods like getEditorUI(), getKit()
* or getKitClass().
*
* @author Miloslav Metelka
* @version 0.10
*/

public class Utilities {

    private static final String WRONG_POSITION_LOCALE = "wrong_position"; // NOI18N

    /** Switch the case to capital letters. Used in changeCase() */
    public static final int CASE_UPPER = 0;

    /** Switch the case to small letters. Used in changeCase() */
    public static final int CASE_LOWER = 1;

    /** Switch the case to reverse. Used in changeCase() */
    public static final int CASE_SWITCH = 2;

    private Utilities() {
        // instantiation has no sense
    }

    /** Get the starting position of the row.
    * @param c text component to operate on
    * @param offset position in document where to start searching
    * @return position of the start of the row or -1 for invalid position
    */
    public static int getRowStart(JTextComponent c, int offset)
    throws BadLocationException {
        return getRowStart((BaseDocument)c.getDocument(), offset, 0);
    }

    /** Get the starting position of the row.
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @return position of the start of the row or -1 for invalid position
    */
    public static int getRowStart(BaseDocument doc, int offset)
    throws BadLocationException {
        return getRowStart(doc, offset, 0);
    }

    /** Get the starting position of the row while providing relative count
    * of row how the given position should be shifted. This is the most
    * efficient way how to move by lines in the document based on some
    * position. There is no similair getRowEnd() method that would have
    * shifting parameter.
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @param lineShift shift the given offset forward/back relatively
    *  by some amount of lines
    * @return position of the start of the row or -1 for invalid position
    */
    public static int getRowStart(BaseDocument doc, int offset, int lineShift)
    throws BadLocationException {
        
        checkOffsetValid(doc, offset);

        if (lineShift != 0) {
            Element lineRoot = doc.getParagraphElement(0).getParentElement();
            int line = lineRoot.getElementIndex(offset);
            line += lineShift;
            if (line < 0 || line >= lineRoot.getElementCount()) {
                return -1; // invalid line shift
            }
            return lineRoot.getElement(line).getStartOffset();

        } else { // no shift
            return doc.getParagraphElement(offset).getStartOffset();
        }
    }

    /** Get the first non-white character on the line.
    * The document.isWhitespace() is used to test whether the particular
    * character is white space or not.
    * @param doc document to operate on
    * @param offset position in document anywhere on the line
    * @return position of the first non-white char on the line or -1
    *   if there's no non-white character on that line.
    */
    public static int getRowFirstNonWhite(BaseDocument doc, int offset)
    throws BadLocationException {
        
        checkOffsetValid(doc, offset);

        Element lineElement = doc.getParagraphElement(offset);
        return getFirstNonWhiteFwd(doc,
            lineElement.getStartOffset(),
            lineElement.getEndOffset() - 1
        );
    }

    /** Get the last non-white character on the line.
    * The document.isWhitespace() is used to test whether the particular
    * character is white space or not.
    * @param doc document to operate on
    * @param offset position in document anywhere on the line
    * @return position of the last non-white char on the line or -1
    *   if there's no non-white character on that line.
    */
    public static int getRowLastNonWhite(BaseDocument doc, int offset)
    throws BadLocationException {
        
        checkOffsetValid(doc, offset);

        Element lineElement = doc.getParagraphElement(offset);
        return getFirstNonWhiteBwd(doc,
            lineElement.getEndOffset() - 1,
            lineElement.getStartOffset()
        );
    }

    /** Get indentation on the current line. If this line is white then
    * return -1.
    * @param doc document to operate on
    * @param offset position in document anywhere on the line
    * @return indentation or -1 if the line is white
    */
    public static int getRowIndent(BaseDocument doc, int offset)
    throws BadLocationException {
        offset = getRowFirstNonWhite(doc, offset);
        if (offset == -1) {
            return -1;
        }
        return doc.getVisColFromPos(offset);
    }

    /** Get indentation on the current line. If this line is white then
    * go either up or down an return indentation of the first non-white row.
    * The <tt>getRowFirstNonWhite()</tt> is used to find the indentation
    * on particular line.
    * @param doc document to operate on
    * @param offset position in document anywhere on the line
    * @param downDir if this flag is set to true then if the row is white
    *   then the indentation of the next first non-white row is returned. If it's
    *   false then the indentation of the previous first non-white row is returned.
    * @return indentation or -1 if there's no non-white line in the specified direction
    */
    public static int getRowIndent(BaseDocument doc, int offset, boolean downDir)
    throws BadLocationException {
        int p = getRowFirstNonWhite(doc, offset);
        if (p == -1) {
            p = getFirstNonWhiteRow(doc, offset, downDir);
            if (p == -1) {
                return -1; // non-white line not found
            }
            p = getRowFirstNonWhite(doc, p);
            if (p == -1) {
                return -1; // non-white line not found
            }
        }
        return doc.getVisColFromPos(p);
    }

    /** Get the end position of the row right before the new-line character.
    * @param c text component to operate on
    * @param offset position in document where to start searching
    * @param relLine shift offset forward/back by some amount of lines
    * @return position of the end of the row or -1 for invalid position
    */
    public static int getRowEnd(JTextComponent c, int offset)
    throws BadLocationException {
        return getRowEnd((BaseDocument)c.getDocument(), offset);
    }

    public static int getRowEnd(BaseDocument doc, int offset)
    throws BadLocationException {
        
        checkOffsetValid(doc, offset);

        return doc.getParagraphElement(offset).getEndOffset() - 1;
    }

    /** Get the position that is one line above and visually at some
    * x-coordinate value.
    * @param doc document to operate on
    * @param offset position in document from which the current line is determined
    * @param x float x-coordinate value
    * @return position of the character that is at the one line above at
    *   the required x-coordinate value
    */
    public static int getPositionAbove(JTextComponent c, int offset, int x)
    throws BadLocationException {
        BaseDocument doc = (BaseDocument)c.getDocument();
        BaseTextUI ui = (BaseTextUI)c.getUI();
        offset = ui.viewToModel(c, x, ui.getYFromPos(offset) - ui.getEditorUI().getLineHeight());
        return offset;
    }

    /** Get the position that is one line above and visually at some
    * x-coordinate value.
    * @param c text component to operate on
    * @param offset position in document from which the current line is determined
    * @param x float x-coordinate value
    * @return position of the character that is at the one line above at
    *   the required x-coordinate value
    */
    public static int getPositionBelow(JTextComponent c, int offset, int x)
    throws BadLocationException {
        BaseDocument doc = (BaseDocument)c.getDocument();
        BaseTextUI ui = (BaseTextUI)c.getUI();
        offset = ui.viewToModel(c, x, ui.getYFromPos(offset) + ui.getEditorUI().getLineHeight());
        return offset;
    }

    /** Get start of the current word. If there are no more words till
    * the begining of the document, this method returns -1.
    * @param c text component to operate on
    * @param offset position in document from which the current line is determined
    */
    public static int getWordStart(JTextComponent c, int offset)
    throws BadLocationException {
        return getWordStart((BaseDocument)c.getDocument(), offset);
    }

    public static int getWordStart(BaseDocument doc, int offset)
    throws BadLocationException {
        int docLen = doc.getLength();
        return doc.find(new FinderFactory.PreviousWordBwdFinder(doc, false, true),
                        offset, 0);
    }

    public static int getWordEnd(JTextComponent c, int offset)
    throws BadLocationException {
        return getWordEnd((BaseDocument)c.getDocument(), offset);
    }

    public static int getWordEnd(BaseDocument doc, int offset)
    throws BadLocationException {
        return doc.find(new FinderFactory.NextWordFwdFinder(doc, false, true),
                        offset, -1);
    }

    public static int getNextWord(JTextComponent c, int offset)
    throws BadLocationException {
        return getNextWord((BaseDocument)c.getDocument(), offset);
    }

    public static int getNextWord(BaseDocument doc, int offset)
    throws BadLocationException {
        Finder nextWordFinder = (Finder)doc.getProperty(SettingsNames.NEXT_WORD_FINDER);
        offset = doc.find(nextWordFinder, offset, -1);
        if (offset < 0) {
            offset = doc.getLength();
        }
        return offset;
    }

    public static int getPreviousWord(JTextComponent c, int offset)
    throws BadLocationException {
        return getPreviousWord((BaseDocument)c.getDocument(), offset);
    }

    public static int getPreviousWord(BaseDocument doc, int offset)
    throws BadLocationException {
        Finder prevWordFinder = (Finder)doc.getProperty(SettingsNames.PREVIOUS_WORD_FINDER);
        offset = doc.find(prevWordFinder, offset, 0);
        if (offset < 0) {
            offset = 0;
        }
        return offset;
    }

    /** Get first white character in document in forward direction
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @return position of the first white character or -1
    */
    public static int getFirstWhiteFwd(BaseDocument doc, int offset)
    throws BadLocationException {
        return getFirstWhiteFwd(doc, offset, -1);
    }

    /** Get first white character in document in forward direction
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @param limitPos position in document (greater or equal than offset) where
    *   the search will stop reporting unsuccessful search by returning -1
    * @return position of the first non-white character or -1
    */
    public static int getFirstWhiteFwd(BaseDocument doc, int offset, int limitPos)
    throws BadLocationException {
        return doc.find(new FinderFactory.WhiteFwdFinder(doc), offset, limitPos);
    }

    /** Get first non-white character in document in forward direction
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @return position of the first non-white character or -1
    */
    public static int getFirstNonWhiteFwd(BaseDocument doc, int offset)
    throws BadLocationException {
        return getFirstNonWhiteFwd(doc, offset, -1);
    }

    /** Get first non-white character in document in forward direction
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @param limitPos position in document (greater or equal than offset) where
    *   the search will stop reporting unsuccessful search by returning -1
    * @return position of the first non-white character or -1
    */
    public static int getFirstNonWhiteFwd(BaseDocument doc, int offset, int limitPos)
    throws BadLocationException {
        return doc.find(new FinderFactory.NonWhiteFwdFinder(doc), offset, limitPos);
    }

    /** Get first white character in document in backward direction.
    * The character right before the character at position offset will
    * be searched as first.
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @return position of the first white character or -1
    */
    public static int getFirstWhiteBwd(BaseDocument doc, int offset)
    throws BadLocationException {
        return getFirstWhiteBwd(doc, offset, 0);
    }

    /** Get first white character in document in backward direction.
    * The character right before the character at position offset will
    * be searched as first.
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @param limitPos position in document (lower or equal than offset) where
    *   the search will stop reporting unsuccessful search by returning -1
    * @return position of the first white character or -1
    */
    public static int getFirstWhiteBwd(BaseDocument doc, int offset, int limitPos)
    throws BadLocationException {
        return doc.find(new FinderFactory.WhiteBwdFinder(doc), offset, limitPos);
    }

    /** Get first non-white character in document in backward direction.
    * The character right before the character at position offset will
    * be searched as first.
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @return position of the first non-white character or -1
    */
    public static int getFirstNonWhiteBwd(BaseDocument doc, int offset)
    throws BadLocationException {
        return getFirstNonWhiteBwd(doc, offset, 0);
    }

    /** Get first non-white character in document in backward direction.
    * The character right before the character at position offset will
    * be searched as first.
    * @param doc document to operate on
    * @param offset position in document where to start searching
    * @param limitPos position in document (lower or equal than offset) where
    *   the search will stop reporting unsuccessful search by returning -1
    * @return position of the first non-white character or -1
    */
    public static int getFirstNonWhiteBwd(BaseDocument doc, int offset, int limitPos)
    throws BadLocationException {
        return doc.find(new FinderFactory.NonWhiteBwdFinder(doc), offset, limitPos);
    }

    /** Return line offset (line number - 1) for some position in the document
    * @param doc document to operate on
    * @param offset position in document where to start searching
    */
    public static int getLineOffset(BaseDocument doc, int offset)
    throws BadLocationException {
        
        checkOffsetValid(doc, offset);

        Element lineRoot = doc.getParagraphElement(0).getParentElement();
        return lineRoot.getElementIndex(offset);
    }

    /** Return start offset of the line
    * @param lineIndex line index starting from 0
    * @return start position of the line or -1 if lineIndex was invalid
    */
    public static int getRowStartFromLineOffset(BaseDocument doc, int lineIndex) {
        Element lineRoot = doc.getParagraphElement(0).getParentElement();
        if (lineIndex < 0 || lineIndex >= lineRoot.getElementCount()) {
            return -1; // invalid line number

        } else {
            return lineRoot.getElement(lineIndex).getStartOffset();
        }
    }

    /** Return visual column (with expanded tabs) on the line.
    * @param doc document to operate on
    * @param offset position in document for which the visual column should be found
    * @return visual column on the line determined by position
    */
    public static int getVisualColumn(BaseDocument doc, int offset)
    throws BadLocationException {
        return doc.getVisColFromPos(offset);
    }

    /** Get the identifier around the given position or null if there's no identifier
    * @see getIdentifierBlock()
    */
    public static String getIdentifier(BaseDocument doc, int offset)
    throws BadLocationException {
        int[] blk = getIdentifierBlock(doc, offset);
        return (blk != null) ? doc.getText(blk[0], blk[1] - blk[0]) : null;
    }


    /** Get the identifier around the given position or null if there's no identifier
     * around the given position. The identifier is not verified against SyntaxSupport.isIdentifier().
     * @param c JTextComponent to work on
     * @param offset position in document - usually the caret.getDot()
     * @return the block (starting and ending position) enclosing the identifier
     * or null if no identifier was found
     */
    public static int[] getIdentifierBlock(JTextComponent c, int offset)
    throws BadLocationException {
        String id = null;
        int[] ret = null;
        Document doc = c.getDocument();
        int idStart = javax.swing.text.Utilities.getWordStart(c, offset);
        if (idStart >= 0) {
            int idEnd = javax.swing.text.Utilities.getWordEnd(c, idStart);
            if (idEnd >= 0) {
                id = doc.getText(idStart, idEnd - idStart);
                ret = new int[] { idStart, idEnd };
                String trim = id.trim();
                if (trim.length() == 0 || (trim.length() == 1 && !Character.isJavaIdentifierPart(trim.charAt(0)))) {
                    int prevWordStart = javax.swing.text.Utilities.getPreviousWord(c, offset);
                    if (offset == javax.swing.text.Utilities.getWordEnd(c,prevWordStart )){
                        ret = new int[] { prevWordStart, offset };
                    }
                } else if ((id != null) && (id.length() != 0)  && (id.indexOf(".") != -1)){ //NOI18N
                    int index = offset - idStart;
                    int begin = id.substring(0, index).lastIndexOf("."); //NOI18N
                    begin = (begin == -1) ? 0 : begin + 1; //first index after the dot, if exists
                    int end = id.indexOf(".", index); //NOI18N
                    end = (end == -1) ? id.length() : end;
                    ret = new int[] { idStart+begin, idStart+end };
                }
            }
        }
        return ret;
    }
    
    
    
    /** Get the identifier around the given position or null if there's no identifier
    * around the given position. The identifier must be
    * accepted by SyntaxSupport.isIdnetifier() otherwise null is returned.
    * @param doc document to work on
    * @param offset position in document - usually the caret.getDot()
    * @return the block (starting and ending position) enclosing the identifier
    *   or null if no identifier was found
    */
    public static int[] getIdentifierBlock(BaseDocument doc, int offset)
    throws BadLocationException {
        int[] ret = null;
        int idStart = getWordStart(doc, offset);
        if (idStart >= 0) {
            int idEnd = getWordEnd(doc, idStart);
            if (idEnd >= 0) {
                String id = doc.getText(idStart, idEnd - idStart);
                if (doc.getSyntaxSupport().isIdentifier(id)) {
                    ret = new int[] { idStart, idEnd };
                } else { // not identifier by syntax support
                    id = getWord(doc, offset); // try right at offset
                    if (doc.getSyntaxSupport().isIdentifier(id)) {
                        ret = new int[] { offset, offset + id.length() };
                    }
                }
            }
        }
        return ret;
    }

    
    /** Get the word around the given position .
     * @param c component to work with
     * @param offset position in document - usually the caret.getDot()
     * @return the word.
     */
    public static String getWord(JTextComponent c, int offset)
    throws BadLocationException {
        int[] blk = getIdentifierBlock(c, offset);
        Document doc = c.getDocument();
        return (blk != null) ? doc.getText(blk[0], blk[1] - blk[0]) : null;
    }
    
    
    /** Get the selection if there's any or get the identifier around
    * the position if there's no selection.
    * @param c component to work with
    * @param offset position in document - usually the caret.getDot()
    * @return the block (starting and ending position) enclosing the identifier
    *   or null if no identifier was found
    */
    public static int[] getSelectionOrIdentifierBlock(JTextComponent c, int offset)
    throws BadLocationException {
        Document doc = c.getDocument();
        Caret caret = c.getCaret();
        int[] ret;
        if (caret.isSelectionVisible()) {
            ret = new int[] { c.getSelectionStart(), c.getSelectionEnd() }; 
        } else if (doc instanceof BaseDocument){
            ret = getIdentifierBlock((BaseDocument)doc, caret.getDot());
        } else {
            ret = getIdentifierBlock(c, offset);
        }
        return ret;
    }

    /** Get the selection or identifier at the current caret position
     * @see getSelectionOrIdentifierBlock(JTextComponent, int)
     */
    public static int[] getSelectionOrIdentifierBlock(JTextComponent c) {
        try {
            return getSelectionOrIdentifierBlock(c, c.getCaret().getDot());
        } catch (BadLocationException e) {
            return null;
        }
    }

    /** Get the identifier before the given position (ending at given offset)
    * or null if there's no identifier
    */
    public static String getIdentifierBefore(BaseDocument doc, int offset)
    throws BadLocationException {
        int wordStart = getWordStart(doc, offset);
        if (wordStart != -1) {
            String word = new String(doc.getChars(wordStart,
                                                  offset - wordStart), 0, offset - wordStart);
            if (doc.getSyntaxSupport().isIdentifier(word)) {
                return word;
            }
        }
        return null;
    }

    /** Get the selection if there's any or get the identifier around
    * the position if there's no selection.
    */
    public static String getSelectionOrIdentifier(JTextComponent c, int offset)
    throws BadLocationException {
        Document doc = c.getDocument();
        Caret caret = c.getCaret();
        String ret;
        if (caret.isSelectionVisible()) {
            ret = c.getSelectedText();
	    if (ret != null) return ret;
        } 
	if (doc instanceof BaseDocument){
	    ret = getIdentifier((BaseDocument) doc, caret.getDot());
        } else {
	    ret = getWord(c, offset);
	}
        return ret;
    }

    /** Get the selection or identifier at the current caret position */
    public static String getSelectionOrIdentifier(JTextComponent c) {
        try {
            return getSelectionOrIdentifier(c, c.getCaret().getDot());
        } catch (BadLocationException e) {
            return null;
        }
    }

    /** Get the word at given position.
    */
    public static String getWord(BaseDocument doc, int offset)
    throws BadLocationException {
        int wordEnd = getWordEnd(doc, offset);
        if (wordEnd != -1) {
            return new String(doc.getChars(offset, wordEnd - offset), 0,
                              wordEnd - offset);
        }
        return null;
    }


    /** Change the case for specified part of document
    * @param doc document to operate on
    * @param offset position in document determines the changed area begining
    * @param len number of chars to change
    * @param type either CASE_CAPITAL, CASE_SMALL or CASE_SWITCH
    */
    public static boolean changeCase(BaseDocument doc, int offset, int len, int type)
    throws BadLocationException {
        char[] orig = doc.getChars(offset, len);
        char[] changed = (char[])orig.clone();
        for (int i = 0; i < orig.length; i++) {
            switch (type) {
            case CASE_UPPER:
                changed[i] = Character.toUpperCase(orig[i]);
                break;
            case CASE_LOWER:
                changed[i] = Character.toLowerCase(orig[i]);
                break;
            case CASE_SWITCH:
                if (Character.isUpperCase(orig[i])) {
                    changed[i] = Character.toLowerCase(orig[i]);
                } else if (Character.isLowerCase(orig[i])) {
                    changed[i] = Character.toUpperCase(orig[i]);
                }
                break;
            }
        }
        // check chars for difference and possibly change document
        for (int i = 0; i < orig.length; i++) {
            if (orig[i] != changed[i]) {
                doc.atomicLock();
                try {
                    doc.remove(offset, orig.length);
                    doc.insertString(offset, new String(changed), null);
                } finally {
                    doc.atomicUnlock();
                }
                return true; // changed
            }
        }
        return false;
    }

    /** Tests whether the line contains no characters except the ending new-line.
    * @param doc document to operate on
    * @param offset position anywhere on the tested line
    * @return whether the line is empty or not
    */
    public static boolean isRowEmpty(BaseDocument doc, int offset)
    throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        return (lineElement.getStartOffset() + 1 == lineElement.getEndOffset());
    }

    public static int getFirstNonEmptyRow(BaseDocument doc, int offset, boolean downDir)
    throws BadLocationException {
        while (offset != -1 && isRowEmpty(doc, offset)) {
            offset = getRowStart(doc, offset, downDir ? +1 : -1);
        }
        return offset;
    }

    /** Tests whether the line contains only whitespace characters.
    * @param doc document to operate on
    * @param offset position anywhere on the tested line
    * @return whether the line is empty or not
    */
    public static boolean isRowWhite(BaseDocument doc, int offset)
    throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        offset = doc.find(new FinderFactory.NonWhiteFwdFinder(doc),
              lineElement.getStartOffset(), lineElement.getEndOffset() - 1);
        return (offset == -1);
    }

    public static int getFirstNonWhiteRow(BaseDocument doc, int offset, boolean downDir)
    throws BadLocationException {
        if (isRowWhite(doc, offset)) {
            if (downDir) { // search down for non-white line
                offset = getFirstNonWhiteFwd(doc, offset);
            } else { // search up for non-white line
                offset = getFirstNonWhiteBwd(doc, offset);
            }
        }
        return offset;
    }

    /** Reformat a block of code.
    * @param doc document to work with
    * @param startOffset offset at which the formatting starts
    * @param endOffset offset at which the formatting ends
    * @return length of the reformatted code
    */
    public static int reformat(BaseDocument doc, int startOffset, int endOffset)
    throws BadLocationException {
        return doc.getFormatter().reformat(doc, startOffset, endOffset);
    }

    /** Reformat the line around the given position. */
    public static void reformatLine(BaseDocument doc, int pos)
    throws BadLocationException {
        int lineStart = getRowStart(doc, pos);
        int lineEnd = getRowEnd(doc, pos);
        reformat(doc, lineStart, lineEnd);
    }

    /** Count of rows between these two positions */
    public static int getRowCount(BaseDocument doc, int startPos, int endPos)
    throws BadLocationException {
        if (startPos > endPos) {
            return 0;
        }
        Element lineRoot = doc.getParagraphElement(0).getParentElement();
        return lineRoot.getElementIndex(endPos) - lineRoot.getElementIndex(startPos) + 1;
    }

    /** Get the total count of lines in the document */
    public static int getRowCount(BaseDocument doc) {
        return doc.getParagraphElement(0).getParentElement().getElementCount();
    }

    /** @deprecated
     * @see Formatter.insertTabString()
     */
    public static String getTabInsertString(BaseDocument doc, int offset)
    throws BadLocationException {
        int col = getVisualColumn(doc, offset);
        Formatter f = doc.getFormatter();
        boolean expandTabs = f.expandTabs();
        if (expandTabs) {
            int spacesPerTab = f.getSpacesPerTab();
            int len = (col + spacesPerTab) / spacesPerTab * spacesPerTab - col;
            return new String(Analyzer.getSpacesBuffer(len), 0, len);
        } else { // insert pure tab
            return "\t"; // NOI18N
        }
    }

    /** Get the visual column corresponding to the position after pressing
     * the TAB key.
     * @param doc document to work with
     * @param offset position at which the TAB was pressed
     */
    public static int getNextTabColumn(BaseDocument doc, int offset)
    throws BadLocationException {
        int col = getVisualColumn(doc, offset);
        int tabSize = doc.getFormatter().getSpacesPerTab();
        return (col + tabSize) / tabSize * tabSize;
    }

    public static void setStatusText(JTextComponent c, String text) {
        StatusBar sb = getEditorUI(c).getStatusBar();
        if (sb != null) {
            sb.setText(StatusBar.CELL_MAIN, text);
        }
    }

    public static void setStatusText(JTextComponent c, String text,
                                     Coloring extraColoring) {
        StatusBar sb = getEditorUI(c).getStatusBar();
        if (sb != null) {
            sb.setText(StatusBar.CELL_MAIN, text, extraColoring);
        }
    }

    public static void setStatusBoldText(JTextComponent c, String text) {
        StatusBar sb = getEditorUI(c).getStatusBar();
        if (sb != null) {
            sb.setBoldText(StatusBar.CELL_MAIN, text);
        }
    }

    public static String getStatusText(JTextComponent c) {
        StatusBar sb = getEditorUI(c).getStatusBar();
        return (sb != null) ? sb.getText(StatusBar.CELL_MAIN) : null;
    }

    public static void clearStatusText(JTextComponent c) {
        setStatusText(c, ""); // NOI18N
    }

    public static void insertMark(BaseDocument doc, Mark mark, int offset)
    throws BadLocationException, InvalidMarkException {
        mark.insert(doc, offset);
    }

    public static void moveMark(BaseDocument doc, Mark mark, int newOffset)
    throws BadLocationException, InvalidMarkException {
        mark.move(doc, newOffset);
    }

    public static void returnFocus() {
         JTextComponent c = getLastActiveComponent();
         if (c != null) {
             requestFocus(c);
         }
    }

    public static void requestFocus(JTextComponent c) {
        if (c != null) {
            boolean ok = false;
            BaseKit kit = getKit(c);
            if (kit != null) {
                Class fcc = kit.getFocusableComponentClass(c);
                if (fcc != null) {
                    Container container = SwingUtilities.getAncestorOfClass(fcc, c);
                    if (container != null) {
                        container.requestFocus();
                        ok = true;
                    }
                }
            }

            if (!ok) {
                Frame f = EditorUI.getParentFrame(c);
                if (f != null) {
                    f.requestFocus();
                }
                c.requestFocus();
            }
        }
    }

    public static void runInEventDispatchThread(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public static String debugPosition(BaseDocument doc, int offset) {
        String ret;

        if (offset >= 0) {
            try {
                int line = getLineOffset(doc, offset) + 1;
                int col = getVisualColumn(doc, offset) + 1;
                ret = String.valueOf(line) + ":" + String.valueOf(col); // NOI18N
            } catch (BadLocationException e) {
                ret = LocaleSupport.getString( WRONG_POSITION_LOCALE )
                      + ' ' + offset + " > " + doc.getLength(); // NOI18N
            }
        } else {
            ret = String.valueOf(offset);
        }

        return ret;
    }

    /** Display the identity of the document together with the title property
     * and stream-description property.
     */
    public static String debugDocument(Document doc) {
        return "<" + System.identityHashCode(doc)
            + ", title='" + doc.getProperty(Document.TitleProperty)
            + "', stream='" + doc.getProperty(Document.StreamDescriptionProperty)
            + ", " + doc.toString() + ">";
    }

    public static void performAction(Action a, ActionEvent evt, JTextComponent target) {
        if (a instanceof BaseAction) {
            ((BaseAction)a).actionPerformed(evt, target);
        } else {
            a.actionPerformed(evt);
        }
    }

    public static JTextComponent getLastActiveComponent() {
        return Registry.getMostActiveComponent();
    }

    /** Helper method to obtain instance of EditorUI (extended UI)
     * from the existing JTextComponent.
     * It doesn't require any document locking.
     * @param target JTextComponent for which the extended UI should be obtained
     * @return extended ui instance or null if the component.getUI()
     *   does not return BaseTextUI instance.
     */
    public static EditorUI getEditorUI(JTextComponent target) {
        TextUI ui = target.getUI();
        return (ui instanceof BaseTextUI) 
            ? ((BaseTextUI)ui).getEditorUI()
            : null;
    }

    /** Helper method to obtain instance of editor kit from existing JTextComponent.
    * If the kit of the component is not an instance
    * of the <tt>org.netbeans.editor.BaseKit</tt> the method returns null.
    * The method doesn't require any document locking.
    * @param target JTextComponent for which the editor kit should be obtained
    * @return BaseKit instance or null
    */
    public static BaseKit getKit(JTextComponent target) {
        if (target == null) return null; // #19574
        EditorKit ekit = target.getUI().getEditorKit(target);
        return (ekit instanceof BaseKit) ? (BaseKit)ekit : null;
    }

    /** Helper method to obtain editor kit class from existing JTextComponent.
    * This method is useful for example when dealing with Settings.
    * The method doesn't require any document locking.
    * @param target JTextComponent for which the editor kit should be obtained
    * @return editor kit class
    */
    public static Class getKitClass(JTextComponent target) {
        return (target != null) ? target.getUI().getEditorKit(target).getClass() : null;
    }

    /** Helper method to obtain instance of BaseDocument from JTextComponent.
    * If the document of the component is not an instance
    * of the <tt>org.netbeans.editor.BaseDocument</tt> the method returns null.
    * The method doesn't require any document locking.
    * @param target JTextComponent for which the document should be obtained
    * @return BaseDocument instance or null
    */
    public static BaseDocument getDocument(JTextComponent target) {
        Document doc = target.getDocument();
        return (doc instanceof BaseDocument) ? (BaseDocument)doc : null;
    }

    /** Get the syntax-support class that belongs to the document of the given
    * component. Besides using directly this method, the <tt>SyntaxSupport</tt>
    * can be obtained by calling <tt>doc.getSyntaxSupport()</tt>.
    * The method can return null in case the document is not
    * an instance of the BaseDocument.
    * The method doesn't require any document locking.
    * @param target JTextComponent for which the syntax-support should be obtained
    * @return SyntaxSupport instance or null
    */
    public static SyntaxSupport getSyntaxSupport(JTextComponent target) {
        Document doc = target.getDocument();
        return (doc instanceof BaseDocument) ? ((BaseDocument)doc).getSyntaxSupport() : null;
    }


    /**
     * Creates nice textual description of sequence of KeyStrokes. Usable for
     * displaying MultiKeyBindings. The keyStrokes are delimited by space.
     * @param Array of KeyStrokes representing the actual sequence.
     * @return String describing the KeyStroke sequence.
     */
    public static String keySequenceToString( KeyStroke[] seq ) {
        StringBuffer sb = new StringBuffer();
        for( int i=0; i<seq.length; i++ ) {
            if( i>0 ) sb.append( ' ' );  // NOI18N
            sb.append( keyStrokeToString( seq[i] ) );
        }
        return sb.toString();
    }

    /**
     * Creates nice textual representation of KeyStroke.
     * Modifiers and an actual key label are concated by plus signs
     * @param the KeyStroke to get description of
     * @return String describing the KeyStroke
     */
    public static String keyStrokeToString( KeyStroke stroke ) {
        String modifText = KeyEvent.getKeyModifiersText( stroke.getModifiers() );
        String keyText = (stroke.getKeyCode() == KeyEvent.VK_UNDEFINED) ? 
            String.valueOf(stroke.getKeyChar()) : getKeyText(stroke.getKeyCode());
        if( modifText.length() > 0 ) return modifText + '+' + keyText;
        else return keyText;
    }
    
    /** @return slight modification of what KeyEvent.getKeyText() returns.
     *  The numpad Left, Right, Down, Up get extra result.
     */
    private static String getKeyText(int keyCode) {
        String ret = KeyEvent.getKeyText(keyCode);
        if (ret != null) {
            switch (keyCode) {
                case KeyEvent.VK_KP_DOWN:
                    ret = prefixNumpad(ret, KeyEvent.VK_DOWN);
                    break;
                case KeyEvent.VK_KP_LEFT:
                    ret = prefixNumpad(ret, KeyEvent.VK_LEFT);
                    break;
                case KeyEvent.VK_KP_RIGHT:
                    ret = prefixNumpad(ret, KeyEvent.VK_RIGHT);
                    break;
                case KeyEvent.VK_KP_UP:
                    ret = prefixNumpad(ret, KeyEvent.VK_UP);
                    break;
            }
        }
        return ret;
    }
    
    private static String prefixNumpad(String key, int testKeyCode) {
        if (key.equals(KeyEvent.getKeyText(testKeyCode))) {
            key = LocaleSupport.getString("key-prefix-numpad") + key;
        }
        return key;
    }

    private static void checkOffsetValid(Document doc, int offset) throws BadLocationException {
        int docLen = doc.getLength();
        if (offset < 0 || offset > docLen) { 
            throw new BadLocationException("Invalid offset, document length=" + docLen,
                offset);
        }
    }

}
