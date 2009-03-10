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

package org.netbeans.editor.ext;

import org.netbeans.editor.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import org.netbeans.editor.Formatter;
/**
* Unlike the formatter class, the ExtFormatter concentrates
* on providing a support for the real formatting process.
* Each formatter (there's only one per each kit) can contain
* one or more formatting layers. The <tt>FormatLayer</tt>
* operates over the chain of the tokens provided
* by the <tt>FormatWriter</tt>. The formatting consist
* of changing the chain of the tokens until it gets
* the desired look.
* Each formatting requires a separate instance
* of <tt>FormatWriter</tt> but the same set of format-layers
* is used for all the format-writers. Although the base
* implementation is synchronized so that only one
* format-writer at time is processed by each format-writer,
* in general it's not necessary.
* The basic implementation processes all the format-layers
* sequentialy in the order they were added to the formatter
* but this can be redefined.
* The <tt>getSettingValue</tt> enables to get the up-to-date
* value for the particular setting.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ExtFormatter extends Formatter implements FormatLayer {

    /** List holding the format layers */
    private List formatLayerList = new ArrayList();

    /** Use this instead of testing by containsKey() */
    private static final Object NULL_VALUE = new Object();

    /** Map that contains the requested [setting-name, setting-value] pairs */
    private HashMap settingsMap = new HashMap();

    /** Contains the names of the keys that were turned
     * into custom settings and are no longer read from
     * the Settings.
     */
    private HashMap customSettingsNamesMap = new HashMap();

    private Acceptor indentHotCharsAcceptor;
    private boolean reindentWithTextBefore;

    public ExtFormatter(Class kitClass) {
        super(kitClass);

        initFormatLayers();
    }

    /** Add the desired format-layers to the formatter */
    protected void initFormatLayers() {
    }

    /** Return the name of this formatter. By default
     * it's the name of the kit-class for which it's created
     * without the package name.
     */
    public String getName() {
        return getKitClass().getName().substring(
                getKitClass().getName().lastIndexOf('.') + 1);
    }

    public void settingsChange(SettingsChangeEvent evt) {
        super.settingsChange(evt);
        String settingName = (evt != null) ? evt.getSettingName() : null;

        Class kitClass = getKitClass();
        Iterator eit = settingsMap.entrySet().iterator();
        while (eit.hasNext()) {
            Map.Entry e = (Map.Entry)eit.next();
            if (settingName == null || e.getKey().equals(e.getKey())) {
                if (!customSettingsNamesMap.containsKey(e.getKey())) { // not custom
                    e.setValue(Settings.getValue(kitClass, (String)e.getKey()));
                }
            }
        }
        
        indentHotCharsAcceptor = SettingsUtil.getAcceptor(kitClass,
            ExtSettingsNames.INDENT_HOT_CHARS_ACCEPTOR,
            AcceptorFactory.FALSE);

        reindentWithTextBefore = SettingsUtil.getBoolean(kitClass, 
            ExtSettingsNames.REINDENT_WITH_TEXT_BEFORE,
            false);
    }

    /** Get the value of the given setting.
    * @param settingName name of the setting to get.
    */
    public Object getSettingValue(String settingName) {
        synchronized (Settings.class) {
            Object value = settingsMap.get(settingName);
            if (value == null && !customSettingsNamesMap.containsKey(settingName)) {
                value = Settings.getValue(getKitClass(), settingName);
                if (value == null) {
                    value = NULL_VALUE;
                }
                settingsMap.put(settingName, value);
            }
            return (value != NULL_VALUE) ? value : null;
        }
    }

    /** This method allows to set a custom value to a setting thus
     * overriding the value retrieved from the <tt>Settings</tt>.
     * Once done the value is no longer synchronized with the changes
     * in <tt>Settings</tt> for the particular setting.
     * There's a map holding the names of all the custom
     * settings.
     */
    public void setSettingValue(String settingName, Object settingValue) {
        synchronized (Settings.class) {
            customSettingsNamesMap.put(settingName, settingName);
            settingsMap.put(settingName, settingValue);
        }
    }

    /** Add the new format layer to the layer hierarchy.
    */
    public synchronized void addFormatLayer(FormatLayer layer) {
        formatLayerList.add(layer);
    }

    /** Replace the format-layer with the layerName
    * with the the given layer. If there's no such layer with the same
    * name, the layer is not replaced and false is returned.
    */
    public synchronized boolean replaceFormatLayer(String layerName, FormatLayer layer) {
        int cnt = formatLayerList.size();
        for (int i = 0; i < cnt; i++) {
            if (layerName.equals(((FormatLayer)formatLayerList.get(i)).getName())) {
                formatLayerList.set(i, layer);
                return true;
            }
        }
        return false;
    }

    /** Remove the first layer which has the same name as the given one.
    */
    public synchronized void removeFormatLayer(String layerName) {
        Iterator it = formatLayerIterator();
        while (it.hasNext()) {
            if (layerName.equals(((FormatLayer)it.next()).getName())) {
                it.remove();
                return;
            }
        }
    }

    /** Get the iterator over the format layers.
    */
    public Iterator formatLayerIterator() {
        return formatLayerList.iterator();
    }

    /** Whether do no formatting at all. If this method returns true,
     * the FormatWriter will simply write its input into the underlying
     * writer.
     */
    public boolean isSimple() {
        return false;
    }

    /** Called by format-writer to do the format */
    public synchronized void format(FormatWriter fw) {
        boolean done = false;
        int safetyCounter = 0;
        do {
            // Mark the chain as unmodified at the begining
            fw.setChainModified(false);
            fw.setRestartFormat(false);

            Iterator it = formatLayerIterator();
            while (it.hasNext()) {
                ((FormatLayer)it.next()).format(fw);
                if (fw.isRestartFormat()) {
                    break;
                }
            }

            if (!it.hasNext() && !fw.isRestartFormat()) {
                done = true;
            }

            if (safetyCounter > 1000) { // prevent infinite loop
                new Exception("Indentation infinite loop detected").printStackTrace(); // NOI18N
                break;
            }
        } while (!done);
    }

    /** Reformat a block of code.
    * @param doc document to work with
    * @param startOffset position at which the formatting starts
    * @param endOffset position at which the formatting ends
    * @param indentOnly whether just the indentation should be changed
    *  or regular formatting should be performed.
    * @return formatting writer. The text was already reformatted
    *  but the writer can contain useful information.
    */
    public Writer reformat(BaseDocument doc, int startOffset, int endOffset,
    boolean indentOnly) throws BadLocationException, IOException {
        CharArrayWriter cw = new CharArrayWriter();
        Writer w = createWriter(doc, startOffset, cw);
        FormatWriter fw = (w instanceof FormatWriter) ? (FormatWriter)w : null;
        
        boolean fix5620 = true; // whether apply fix for #5620 or not

        if (fw != null) {
            fw.setIndentOnly(indentOnly);
            if (fix5620) {
                fw.setReformatting(true); // #5620
            }
        }

        w.write(doc.getChars(startOffset, endOffset - startOffset));
        w.close();

        if (!fix5620 || fw == null) { // #5620 - for (fw != null) the doc was already modified
            String out = new String(cw.toCharArray());
            doc.remove(startOffset, endOffset - startOffset);
            doc.insertString(startOffset, out, null);
        }
        
        return w;
    }

    /** Fix of #5620 - same method exists in Formatter (predecessor */
    public int reformat(BaseDocument doc, int startOffset, int endOffset)
    throws BadLocationException {
        try {
            javax.swing.text.Position pos = doc.createPosition(endOffset);
            reformat(doc, startOffset, endOffset, false);
            return pos.getOffset() - startOffset;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Get the block to be reformatted after keystroke was pressed.
     * @param target component to which the text was typed. Caaret position
     *  can be checked etc.
     * @param typedText text (usually just one character) that the user has typed.
     * @return block of the code to be reformatted or null if nothing should
     *  reformatted. It can return block containing just one character. The caller
     *  usually expands even one character to the whole line because less than
     *  the whole line usually doesn't provide enough possibilities for formatting.
     * @see ExtKit.ExtDefaultKeyTypedAction.checkIndentHotChars()
     */
    public int[] getReformatBlock(JTextComponent target, String typedText) {
        if (indentHotCharsAcceptor == null) { // init if necessary
            settingsChange(null);
        }

        if (indentHotCharsAcceptor.accept(typedText.charAt(0))) {
            /* This is bugfix 10771. See the issue for problem description.
             * The behaviour before fix was that whenever the lbrace is
             * entered, the line is indented. This make no sense if a text
             * exist on the line before the lbrace. In this case we
             * simply will not indent the line. This is handled by the hasTextBefore 
             * check
             */
            if(!reindentWithTextBefore) {
                if(hasTextBefore(target, typedText)) {
                    return null;
                }
            }
            int dotPos = target.getCaret().getDot();
            return new int[] { Math.max(dotPos - 1, 0), dotPos };
            
        } else {
            return null;
        }
    }

    protected boolean hasTextBefore(JTextComponent target, String typedText) {
        BaseDocument doc = Utilities.getDocument(target);
        int dotPos = target.getCaret().getDot();
        try {
            int fnw = Utilities.getRowFirstNonWhite(doc, dotPos);
            return dotPos != fnw+typedText.length();
        } catch (BadLocationException e) {
            return false;
        }
    }

    /** Create the indentation writer.
    */
    public Writer createWriter(Document doc, int offset, Writer writer) {
        return new FormatWriter(this, doc, offset, writer, false);
    }

    /** Indents the current line. Should not affect any other
    * lines.
    * @param doc the document to work on
    * @param offset the offset of a character on the line
    * @return new offset of the original character
    */
    public int indentLine(Document doc, int offset) {
        if (doc instanceof BaseDocument) {
            try {
                BaseDocument bdoc = (BaseDocument)doc;
                int lineStart = Utilities.getRowStart(bdoc, offset);
                int nextLineStart = Utilities.getRowStart(bdoc, offset, 1);
                if (nextLineStart < 0) { // end of doc
                    nextLineStart = bdoc.getLength();
                }
                reformat(bdoc, lineStart, nextLineStart, false);
                return Utilities.getRowEnd(bdoc, lineStart);
            } catch (GuardedException e) {
                java.awt.Toolkit.getDefaultToolkit().beep();

            } catch (BadLocationException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }


            } catch (IOException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }


            }

            return offset;

        }

        return super.indentLine(doc, offset);
    }
    
    /** Returns offset of EOL for the white line */
    protected int getEOLOffset(BaseDocument bdoc, int offset) throws BadLocationException{
        return Utilities.getRowEnd(bdoc, offset);
    }

    /** Inserts new line at given position and indents the new line with
    * spaces.
    *
    * @param doc the document to work on
    * @param offset the offset of a character on the line
    * @return new offset to place cursor to
    */
    public int indentNewLine(Document doc, int offset) {
        if (doc instanceof BaseDocument) {
            BaseDocument bdoc = (BaseDocument)doc;
            boolean newLineInserted = false;

            bdoc.atomicLock();
            try {
                bdoc.insertString(offset, "\n", null); // NOI18N
                offset++;
                newLineInserted = true;

                int eolOffset = Utilities.getRowEnd(bdoc, offset);

                // Try to change the indent of the new line
                // It may fail when inserting '\n' before the guarded block
                Writer w = reformat(bdoc, offset, eolOffset, true);

                // Find the caret position
                eolOffset = Utilities.getRowFirstNonWhite(bdoc, offset);
                if (eolOffset < 0) { // white line
                    eolOffset = getEOLOffset(bdoc, offset);
                }

                offset = eolOffset;
                
                // Resulting offset (caret position) can be shifted
                if (w instanceof FormatWriter) {
                    offset += ((FormatWriter)w).getIndentShift();
                }

            } catch (GuardedException e) {
                // Possibly couldn't insert additional indentation
                // at the begining of the guarded block
                // but the initial '\n' could be fine
                if (!newLineInserted) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }

            } catch (BadLocationException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }

            } catch (IOException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }

            } finally {
                bdoc.atomicUnlock();
            }

        } else { // not BaseDocument
            try {
                doc.insertString (offset, "\n", null); // NOI18N
                offset++;
            } catch (BadLocationException ex) {
            }
        }

        return offset;
    }

    /** Whether the formatter accepts the given syntax
     * that will be used for parsing the text passed to
     * the FormatWriter.
     * @param syntax syntax to be tested.
     * @return true whether this formatter is able to process
     *  the tokens created by the syntax or false otherwise.
     */
    protected boolean acceptSyntax(Syntax syntax) {
        return true;
    }

    /** Simple formatter */
    public static class Simple extends ExtFormatter {

        public Simple(Class kitClass) {
            super(kitClass);
        }

        public boolean isSimple() {
            return true;
        }
        
        /** Returns offset of EOL for the white line */
        protected int getEOLOffset(BaseDocument bdoc, int offset) throws BadLocationException{
            return offset;
        }
        
    }

}
