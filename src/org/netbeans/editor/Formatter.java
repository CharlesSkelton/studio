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
import javax.swing.text.Document;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
* Various services related to indentation and text formatting
* are located here. Each kit can have different formatter
* so the first action should be getting the right formatter
* for the given kit by calling Formatter.getFormatter(kitClass).
*
* @author Miloslav Metelka
* @version 1.00
*/

public class Formatter implements SettingsChangeListener {

    private static Map kitClass2Formatter = new HashMap();

    /** Get the formatter for the given kit-class */
    public static synchronized Formatter getFormatter(Class kitClass) {
        Formatter f = (Formatter)kitClass2Formatter.get(kitClass);
        if (f == null) {
            f = BaseKit.getKit(kitClass).createFormatter();
            kitClass2Formatter.put(kitClass, f);
        }
        return f;
    }
    
    /** Set the formatter for the given kit-class.
     * @param kitClass class of the kit for which the formatter
     *  is being assigned.
     * @param formatter new formatter for the given kit
     */
    public static synchronized void setFormatter(Class kitClass, Formatter formatter) {
        kitClass2Formatter.put(kitClass, formatter);
    }


    /** Maximum tab size for which the indent strings will be cached. */
    private static final int ISC_MAX_TAB_SIZE = 16;
    
    /** Cache the indentation strings up to this size */
    private static final int ISC_MAX_INDENT_SIZE = 32;
    
    /** Cache holding the indentation strings for various tab-sizes. */
    private static final String[][] indentStringCache
        = new String[ISC_MAX_TAB_SIZE][];


    private final Class kitClass;

    /** Whether values were already inited from the cache */
    private boolean inited;

    private int tabSize;

    private boolean customTabSize;

    private Integer shiftWidth;

    private boolean customShiftWidth;

    private boolean expandTabs;

    private boolean customExpandTabs;

    private int spacesPerTab;

    private boolean customSpacesPerTab;

    /** Construct new formatter.
    * @param kitClass the class of the kit for which this formatter is being
    *  constructed.
    */
    public Formatter(Class kitClass) {
        this.kitClass = kitClass;
        Settings.addSettingsChangeListener(this);
    }

    /** Get the kit-class for which this formatter is constructed. */
    public Class getKitClass() {
        return kitClass;
    }

    public void settingsChange(SettingsChangeEvent evt) {
        String settingName = (evt != null) ? evt.getSettingName() : null;
        if (!inited || settingName == null || SettingsNames.TAB_SIZE.equals(settingName)) {
            if (!customTabSize) {
                tabSize = SettingsUtil.getInteger(kitClass, SettingsNames.TAB_SIZE,
                                                  SettingsDefaults.defaultTabSize);
            }
        }

        // Shift-width often depends on the rest of parameters
        if (!customShiftWidth) {
            Object shw = Settings.getValue(kitClass, SettingsNames.INDENT_SHIFT_WIDTH);
            if (shw instanceof Integer) {
                shiftWidth = (Integer)shw;
            }
        }

        if (!inited || settingName == null || SettingsNames.EXPAND_TABS.equals(settingName)) {
            if (!customExpandTabs) {
                expandTabs = SettingsUtil.getBoolean(kitClass, SettingsNames.EXPAND_TABS,
                                                     SettingsDefaults.defaultExpandTabs);
            }
        }
        if (!inited || settingName == null || SettingsNames.SPACES_PER_TAB.equals(settingName)) {
            if (!customSpacesPerTab) {
                spacesPerTab = SettingsUtil.getInteger(kitClass, SettingsNames.SPACES_PER_TAB,
                                                       SettingsDefaults.defaultSpacesPerTab);
            }
        }

        inited = true;
    }

    /** Get the number of spaces the TAB character ('\t') visually represents
     * for non-BaseDocument documents. It shouldn't be used for BaseDocument
     * based documents. The reason for that is that the returned value 
     * reflects the value of the setting for the kit class over which
     * this formatter was constructed. However it's possible that the kit class of
     * the document being formatted is different than the kit of the formatter.
     * For example java document could be formatted by html formatter.
     * Therefore <code>BaseDocument.getTabSize()</code> must be used
     * for BaseDocuments to reflect the document's own tabsize.
     * @see BaseDocument.getTabSize()
     */
    public int getTabSize() {
        if (!customTabSize && !inited) {
            settingsChange(null);
        }

        return tabSize;
    }

    /** Set the number of spaces the TAB character ('\t') visually represents
     * for non-BaseDocument documents. It doesn't affect BaseDocument
     * based documents.
     *
     * @see getTabSize()
     * @see BaseDocument.setTabSize()
     */
    public void setTabSize(int tabSize) {
        customTabSize = true;
        this.tabSize = tabSize;
    }


    /** Get the width of one indentation level for non-BaseDocument documents.
     * The algorithm first checks whether there's a value for the INDENT_SHIFT_WIDTH
     * setting. If so it uses it, otherwise it uses <code>getSpacesPerTab()</code>
     * 
     * @see setShiftWidth()
     * @see getSpacesPerTab()
     */
    public int getShiftWidth() {
        if (!customShiftWidth && !inited) {
            settingsChange(null);
        }

        return (shiftWidth != null) ? shiftWidth.intValue() : getSpacesPerTab();
    }

    /** Set the width of one indentation level for non-BaseDocument documents.
     * It doesn't affect BaseDocument based documents.
     *
     * @see getShiftWidth()
     */
    public void setShiftWidth(int shiftWidth) {
        customShiftWidth = true;
        if (this.shiftWidth == null || this.shiftWidth.intValue() != shiftWidth) {
            this.shiftWidth = new Integer(shiftWidth);
        }
    }

    /** Should the typed tabs be expanded to the spaces? */
    public boolean expandTabs() {
        if (!customExpandTabs && !inited) {
            settingsChange(null);
        }

        return expandTabs;
    }

    public void setExpandTabs(boolean expandTabs) {
        customExpandTabs = true;
        this.expandTabs = expandTabs;
    }

    /** Get the number of spaces that should be inserted into the document
    * instead of one typed tab.
    */
    public int getSpacesPerTab() {
        if (!customSpacesPerTab && !inited) {
            settingsChange(null);
        }

        return spacesPerTab;
    }

    public void setSpacesPerTab(int spacesPerTab) {
        customSpacesPerTab = true;
        this.spacesPerTab = spacesPerTab;
    }

    static String getIndentString(int indent, boolean expandTabs, int tabSize) {
        if (indent <= 0) {
            return "";
        }

        if (expandTabs) { // store in 0th slot
            tabSize = 0;
        }

        synchronized (Settings.class) {
            boolean large = (tabSize >= indentStringCache.length)
                || (indent > ISC_MAX_INDENT_SIZE); // indexed by (indent - 1)
            String indentString = null;
            String[] tabCache = null;
            if (!large) {
                tabCache = indentStringCache[tabSize]; // cache for this tab
                if (tabCache == null) {
                    tabCache = new String[ISC_MAX_INDENT_SIZE];
                    indentStringCache[tabSize] = tabCache;
                }
                indentString = tabCache[indent - 1];
            }

            if (indentString == null) {
                indentString = Analyzer.getIndentString(indent, expandTabs, tabSize);

                if (!large) {
                    tabCache[indent - 1] = indentString;
                }
            }

            return indentString;
        }
    }

    public String getIndentString(BaseDocument doc, int indent) {
        return getIndentString(indent, expandTabs(), doc.getTabSize());
    }
        
        
    /** Get the string that is appropriate for the requested indentation.
    * The returned string respects the <tt>expandTabs()</tt> and
    * the <tt>getTabSize()</tt> and will contain either spaces only
    * or fully or partially tabs as necessary.
    */
    public String getIndentString(int indent) {
        return getIndentString(indent, expandTabs(), getTabSize());
    }

    /** Modify the line to move the text starting at dotPos one tab
     * column to the right.  Whitespace preceeding dotPos may be
     * replaced by a TAB character if tabs expanding is on.
     * @param doc document to operate on
     * @param dotPos insertion point
     */
    public void insertTabString(BaseDocument doc, int dotPos)
    throws BadLocationException {
        doc.atomicLock();
        try {
            // Determine first white char before dotPos
            int rsPos = Utilities.getRowStart(doc, dotPos);
            int startPos = Utilities.getFirstNonWhiteBwd(doc, dotPos, rsPos);
            startPos = (startPos >= 0) ? (startPos + 1) : rsPos;
            
            int startCol = Utilities.getVisualColumn(doc, startPos);
            int endCol = Utilities.getNextTabColumn(doc, dotPos);
            String tabStr = Analyzer.getWhitespaceString(startCol, endCol, expandTabs(), doc.getTabSize());
            
            // Search for the first non-common char
            char[] removeChars = doc.getChars(startPos, dotPos - startPos);
            int ind = 0;
            while (ind < removeChars.length && removeChars[ind] == tabStr.charAt(ind)) {
                ind++;
            }
            
            startPos += ind;
            doc.remove(startPos, dotPos - startPos);
            doc.insertString(startPos, tabStr.substring(ind), null);
            
        } finally {
            doc.atomicUnlock();
        }
    }

    /** Change the indent of the given row. Document is atomically locked
    * during this operation.
    */
    public void changeRowIndent(BaseDocument doc, int pos, int newIndent)
    throws BadLocationException {
        doc.atomicLock();
        try {
            if (newIndent < 0) {
                newIndent = 0;
            }
            int firstNW = Utilities.getRowFirstNonWhite(doc, pos);
            if (firstNW == -1) { // valid first non-blank
                firstNW = Utilities.getRowEnd(doc, pos);
            }
            int replacePos = Utilities.getRowStart(doc, pos);
            int removeLen = firstNW - replacePos;
            String removeText = doc.getText(replacePos, removeLen);
            String newIndentText = getIndentString(doc, newIndent);
            if (newIndentText.startsWith(removeText)) {
                // Skip removeLen chars at start
                newIndentText = newIndentText.substring(removeLen);
                replacePos += removeLen;
                removeLen = 0;
            } else if (newIndentText.endsWith(removeText)) {
                // Skip removeLen chars at the end
                newIndentText = newIndentText.substring(0, newIndentText.length() - removeLen);
                removeLen = 0;
            }
            
            if (removeLen != 0) {
                doc.remove(replacePos, removeLen);
            }

            doc.insertString(replacePos, newIndentText, null);
        } finally {
            doc.atomicUnlock();
        }
    }

    /** Increase/decrease indentation of the block of the code. Document
    * is atomically locked during the operation.
    * @param doc document to operate on
    * @param startPos starting line position
    * @param endPos ending line position
    * @param shiftCnt positive/negative count of shiftwidths by which indentation
    *   should be shifted right/left
    */
    public void changeBlockIndent(BaseDocument doc, int startPos, int endPos,
                                  int shiftCnt) throws BadLocationException {

       
        GuardedDocument gdoc = (doc instanceof GuardedDocument)
                               ? (GuardedDocument)doc : null;
        if (gdoc != null){
            for (int i = startPos; i<endPos; i++){
                if (gdoc.isPosGuarded(i)){
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    return;
                }
            }
        }
        
        doc.atomicLock();
        try {

            int indentDelta = shiftCnt * doc.getShiftWidth();
            if (endPos > 0 && Utilities.getRowStart(doc, endPos) == endPos) {
                endPos--;
            }

            int pos = Utilities.getRowStart(doc, startPos );
            for (int lineCnt = Utilities.getRowCount(doc, startPos, endPos);
                    lineCnt > 0; lineCnt--
                ) {
                int indent = Utilities.getRowIndent(doc, pos);
                if (Utilities.isRowWhite(doc, pos)) {
                    indent = -indentDelta; // zero indentation for white line
                }
                changeRowIndent(doc, pos, Math.max(indent + indentDelta, 0));
                pos = Utilities.getRowStart(doc, pos, +1);
            }

        } finally {
            doc.atomicUnlock();
        }
    }

    /** Shift line either left or right */
    public void shiftLine(BaseDocument doc, int dotPos, boolean right)
    throws BadLocationException {
        int ind = doc.getShiftWidth();
        if (!right) {
            ind = -ind;
        }

        if (Utilities.isRowWhite(doc, dotPos)) {
            ind += Utilities.getVisualColumn(doc, dotPos);
        } else {
            ind += Utilities.getRowIndent(doc, dotPos);
        }
        ind = Math.max(ind, 0);
        changeRowIndent(doc, dotPos, ind);
    }

    /** Reformat a block of code.
    * @param doc document to work with
    * @param startOffset offset at which the formatting starts
    * @param endOffset offset at which the formatting ends
    * @return length of the reformatted code
    */
    public int reformat(BaseDocument doc, int startOffset, int endOffset)
    throws BadLocationException {
        try {
            CharArrayWriter cw = new CharArrayWriter();
            Writer w = createWriter(doc, startOffset, cw);
            w.write(doc.getChars(startOffset, endOffset - startOffset));
            w.close();
            String out = new String(cw.toCharArray());
            doc.remove(startOffset, endOffset - startOffset);
            doc.insertString(startOffset, out, null);
            return out.length();
        } catch (IOException e) {
            if (System.getProperty("netbeans.debug.exceptions") != null) { // NOI18N
                e.printStackTrace();
            }
            return 0;
        }
    }

    /** Indents the current line. Should not affect any other
    * lines.
    * @param doc the document to work on
    * @param offset the offset of a character on the line
    * @return new offset of the original character
    */
    public int indentLine(Document doc, int offset) {
        return offset;
    }

    /** Inserts new line at given position and indents the new line with
    * spaces.
    *
    * @param doc the document to work on
    * @param offset the offset of a character on the line
    * @return new offset to place cursor to
    */
    public int indentNewLine(Document doc, int offset) {
        try {
            doc.insertString(offset, "\n", null); // NOI18N
            offset++;

        } catch (GuardedException e) {
            java.awt.Toolkit.getDefaultToolkit().beep();

        } catch (BadLocationException e) {
            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                e.printStackTrace();
            }
        }

        return offset;
    }

    /** Creates a writer that formats text that is inserted into it.
    * The writer should not modify the document but use the 
    * provided writer to write to. Usually the underlaying writer
    * will modify the document itself and optionally it can remember
    * the current position in document. That is why the newly created
    * writer should do no buffering.
    * <P>
    * The provided document and pos are only informational,
    * should not be modified but only used to find correct indentation
    * strategy.
    *
    * @param doc document 
    * @param offset position to begin inserts at
    * @param writer writer to write to
    * @return new writer that will format written text and pass it
    *   into the writer
    */
    public Writer createWriter(Document doc, int offset, Writer writer) {
        return writer;
    }

}
