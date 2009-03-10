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

package org.netbeans.editor.ext;

import org.netbeans.editor.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

/**
* Extended caret implementation
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ExtCaret extends BaseCaret {

    /** Highlight row draw layer name */
    public static final String HIGHLIGHT_ROW_LAYER_NAME = "highlight-row-layer"; // NOI18N

    /** Highlight row draw layer visibility */
    public static final int HIGHLIGHT_ROW_LAYER_VISIBILITY = 1600;

    /** Highlight matching brace draw layer name */
    public static final String HIGHLIGHT_BRACE_LAYER_NAME = "highlight-brace-layer"; // NOI18N

    /** Highlight matching brace draw layer visibility */
    public static final int HIGHLIGHT_BRACE_LAYER_VISIBILITY = 11000;

    /** Whether to highlight the background of the row
    * where the caret is.
    */
    boolean highlightRow;

    /** Whether to hightlight the matching brace */
    boolean highlightBrace;

    /** Coloring used for highlighting the row where the caret is. */
    Coloring highlightRowColoring;

    /** Coloring used for highlighting the matching brace */
    Coloring highlightBraceColoring;

    /** Mark holding the start of the line where the caret currently is. */
    MarkFactory.DrawMark highlightRowMark;

    /** Mark holding the starting position of the matching brace. */
    MarkFactory.DrawMark highlightBraceStartMark;

    /** Mark holding the ending position of the matching brace. */
    MarkFactory.DrawMark highlightBraceEndMark;

    /** Timer that fires when the matching brace should be displayed */
    private Timer braceTimer;
    private ActionListener braceTimerListener; // because of unwanted GC

    /** Signal that the next matching brace update
    * will be immediate without waiting for the brace
    * timer to fire the action.
    */
    private boolean matchBraceUpdateSync;

    /** Whether the brace starting and ending marks are currently valid or not.
     * If they are not valid the block they delimit is not highlighted.
     */
    boolean braceMarksValid;

    boolean simpleMatchBrace;
    
    private boolean popupMenuEnabled;

    static final long serialVersionUID =-4292670043122577690L;

    
    protected void modelChanged(BaseDocument oldDoc, BaseDocument newDoc) {
        // Fix for #7108
        braceMarksValid = false; // brace marks are out of date - new document
        if (highlightRowMark != null) {
            try {
                highlightRowMark.remove();
            } catch (InvalidMarkException e) {
            }
            highlightRowMark = null;
        }
        
        if (highlightBraceStartMark != null) {
            try {
                highlightBraceStartMark.remove();
            } catch (InvalidMarkException e) {
            }
            highlightBraceStartMark = null;
        }

        if (highlightBraceEndMark != null) {
            try {
                highlightBraceEndMark.remove();
            } catch (InvalidMarkException e) {
            }
            highlightBraceEndMark = null;
        }

        super.modelChanged( oldDoc, newDoc );
    }
    
    /** Called when settings were changed. The method is called
    * also in constructor, so the code must count with the evt being null.
    */
    public void settingsChange(SettingsChangeEvent evt) {
        super.settingsChange(evt);
        JTextComponent c = component;
        if (c != null) {

            EditorUI editorUI = Utilities.getEditorUI(c);
            Class kitClass = Utilities.getKitClass(c);
            highlightRowColoring = editorUI.getColoring(
                                       ExtSettingsNames.HIGHLIGHT_CARET_ROW_COLORING);
            highlightBraceColoring = editorUI.getColoring(
                                           ExtSettingsNames.HIGHLIGHT_MATCH_BRACE_COLORING);

            // Handle highlight row
            boolean oldHighlightRow = highlightRow;
            highlightRow = SettingsUtil.getBoolean(kitClass,
                                                   ExtSettingsNames.HIGHLIGHT_CARET_ROW,
                                                   ExtSettingsDefaults.defaultHighlightCaretRow);

            if (oldHighlightRow && !highlightRow && highlightRowMark != null) {
                try {
                    highlightRowMark.remove();
                } catch (InvalidMarkException e) {
                }

                highlightRowMark = null;
            }

            highlightBrace = SettingsUtil.getBoolean(kitClass,
                               ExtSettingsNames.HIGHLIGHT_MATCH_BRACE,
                               ExtSettingsDefaults.defaultHighlightMatchBrace);
            int highlightBraceDelay = SettingsUtil.getInteger(kitClass,
                                        ExtSettingsNames.HIGHLIGHT_MATCH_BRACE_DELAY,
                                        ExtSettingsDefaults.defaultHighlightMatchBraceDelay);

            if (highlightBrace) {
                if (highlightBraceDelay > 0) {
                    // jdk12 compiler doesn't allow inside run()
                    final JTextComponent c2 = component;

                    braceTimer = new Timer(highlightBraceDelay, null);
                    braceTimerListener = 
                         new ActionListener() {
                             public void actionPerformed(ActionEvent evt2) {
                                 SwingUtilities.invokeLater(
                                     new Runnable() {
                                         public void run() {
                                             if (c2 != null) {
                                                 BaseDocument doc = Utilities.getDocument(c2);
                                                 if( doc != null ) {
                                                     doc.readLock();
                                                     try {
                                                         updateMatchBrace();
                                                     } finally {
                                                         doc.readUnlock();
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 );
                             }
                         };
                         
                    braceTimer.addActionListener(new WeakTimerListener(braceTimerListener));
                    braceTimer.setRepeats(false);
                } else {
                    braceTimer = null; // signal no delay
                }
            }

            simpleMatchBrace = SettingsUtil.getBoolean(kitClass,
                                    ExtSettingsNames.CARET_SIMPLE_MATCH_BRACE,
                                    ExtSettingsDefaults.defaultCaretSimpleMatchBrace);
            
            popupMenuEnabled = SettingsUtil.getBoolean(kitClass,
                ExtSettingsNames.POPUP_MENU_ENABLED, true);
        }
    }

    public void install(JTextComponent c) {
        EditorUI editorUI = Utilities.getEditorUI(c);
        editorUI.addLayer(new HighlightRowLayer(), HIGHLIGHT_ROW_LAYER_VISIBILITY);
        editorUI.addLayer(new HighlightBraceLayer(), HIGHLIGHT_BRACE_LAYER_VISIBILITY);
        super.install(c);
    }

    public void deinstall(JTextComponent c) {
        EditorUI editorUI = Utilities.getEditorUI(c);
        editorUI.removeLayer(HIGHLIGHT_ROW_LAYER_NAME);
        editorUI.removeLayer(HIGHLIGHT_BRACE_LAYER_NAME);
        super.deinstall(c);
    }

    /** Update the matching brace of the caret. The document is read-locked
     * while this method is called.
     */
    protected void updateMatchBrace() {
        JTextComponent c = component;
        if (c != null && highlightBrace) {
            try {
                EditorUI editorUI = Utilities.getEditorUI(c);
                BaseDocument doc = (BaseDocument)c.getDocument();
                int dotPos = getDot();
                boolean madeValid = false; // whether brace marks display were validated
                if (dotPos > 0) {
                    int[] matchBlk = ((ExtSyntaxSupport)doc.getSyntaxSupport())
                        .findMatchingBlock(dotPos - 1, simpleMatchBrace);
                    if (matchBlk != null) {
                        if (highlightBraceStartMark != null) {
                            int markStartPos = highlightBraceStartMark.getOffset();
                            int markEndPos = highlightBraceEndMark.getOffset();
                            if (markStartPos != matchBlk[0] || markEndPos != matchBlk[1]) {
                                editorUI.repaintBlock(markStartPos, markEndPos);
                                Utilities.moveMark(doc, highlightBraceStartMark, matchBlk[0]);
                                Utilities.moveMark(doc, highlightBraceEndMark, matchBlk[1]);
                                editorUI.repaintBlock(matchBlk[0], matchBlk[1]);
                            } else { // on the same position
                                if (!braceMarksValid) { // was not valid, must repaint
                                    editorUI.repaintBlock(matchBlk[0], matchBlk[1]);
                                }
                            }
                        } else { // highlight mark is null
                            highlightBraceStartMark = new MarkFactory.DrawMark(
                                                       HIGHLIGHT_BRACE_LAYER_NAME, editorUI);
                            highlightBraceEndMark = new MarkFactory.DrawMark(
                                                       HIGHLIGHT_BRACE_LAYER_NAME, editorUI);
                            highlightBraceStartMark.setActivateLayer(true);
                            Utilities.insertMark(doc, highlightBraceStartMark, matchBlk[0]);
                            Utilities.insertMark(doc, highlightBraceEndMark, matchBlk[1]);
                            editorUI.repaintBlock(matchBlk[0], matchBlk[1]);
                        }
                        braceMarksValid = true;
                        madeValid = true;
                    }
                }

                if (!madeValid) {
                    if (braceMarksValid) {
                        braceMarksValid = false;
                        editorUI.repaintBlock(highlightBraceStartMark.getOffset(),
                                highlightBraceEndMark.getOffset());
                    }
                }
            } catch (BadLocationException e) {
                if (System.getProperty("netbeans.debug.exceptions") != null) { // NOI18N
                    e.printStackTrace();
                }
                highlightBrace = false;
            } catch (InvalidMarkException e) {
                if (System.getProperty("netbeans.debug.exceptions") != null) { // NOI18N
                    e.printStackTrace();
                }
                highlightBrace = false;
            }
        }
    }

    protected void update(Rectangle scrollRect, int scrollPolicy) {
        if (highlightRow) { // highlight row with the caret
            JTextComponent c = component;
            if (c != null) {
                EditorUI editorUI = Utilities.getEditorUI(c);
                BaseDocument doc = (BaseDocument)c.getDocument();
                int dotPos = getDot();
                try {
                    int bolPos = Utilities.getRowStart(doc, dotPos);
                    if (highlightRowMark != null) {
                        int markPos = highlightRowMark.getOffset();
                        if (bolPos != markPos) {
                            editorUI.repaintOffset(markPos);
                            Utilities.moveMark(doc, highlightRowMark, bolPos);
                            editorUI.repaintOffset(bolPos);
                        }
                    } else { // highlight mark is null
                        highlightRowMark = new MarkFactory.DrawMark(HIGHLIGHT_ROW_LAYER_NAME, editorUI);
                        highlightRowMark.setActivateLayer(true);
                        Utilities.insertMark(doc, highlightRowMark, bolPos);
                        editorUI.repaintOffset(bolPos);
                    }
                } catch (BadLocationException e) {
                    highlightRow = false;
                } catch (InvalidMarkException e) {
                    highlightRow = false;
                }
            }
        }

        if (highlightBrace) {
            if (matchBraceUpdateSync || braceTimer == null) {
                updateMatchBrace();
                matchBraceUpdateSync = false;

            } else { // delay the brace update
                braceTimer.restart();
            }
        }

        super.update(scrollRect, scrollPolicy);
    }

    /** Signal that the next matching brace update
    * will be immediate without waiting for the brace
    * timer to fire the action. This is usually done
    * for the key-typed action.
    */
    public void requestMatchBraceUpdateSync() {
        matchBraceUpdateSync = true;
    }
    
    public void mousePressed(MouseEvent evt) {
        Completion completion = ExtUtilities.getCompletion(component);
        if (completion != null && completion.isPaneVisible()) {
            // Hide completion if visible
            completion.setPaneVisible(false);
        }
	showPopup(evt);
        super.mousePressed(evt);
    }
    
    private boolean showPopup (MouseEvent evt) {
        // Show popup menu for right click
        if (component != null && evt.isPopupTrigger() && popupMenuEnabled) {
            ExtUtilities.getExtEditorUI(component).showPopupMenu(evt.getX(), evt.getY());
            return true;
        }
        return false;
    }
    
    public void mouseReleased(MouseEvent evt) {
        if (!showPopup(evt)) {
            super.mouseReleased(evt);
        }
    }

    /** Draw layer to highlight the row where the caret currently resides */
    class HighlightRowLayer extends DrawLayerFactory.ColorLineLayer {

        public HighlightRowLayer() {
            super(HIGHLIGHT_ROW_LAYER_NAME);
        }

        protected Coloring getColoring(DrawContext ctx) {
            return highlightRowColoring;
        }

    }

    /** Draw layer to highlight the matching brace */
    class HighlightBraceLayer extends DrawLayer.AbstractLayer {

        public HighlightBraceLayer() {
            super(HIGHLIGHT_BRACE_LAYER_NAME);
        }

        public void init(DrawContext ctx) {
        }

        public boolean isActive(DrawContext ctx, MarkFactory.DrawMark mark) {
            boolean active = false;

            if (mark != null) {
                if (braceMarksValid) {
                    active = mark.getActivateLayer();
                }
            }

            return active;
        }

        public void updateContext(DrawContext ctx) {
            if (highlightBraceColoring != null) {
                highlightBraceColoring.apply(ctx);
            }
        }

    }

}
