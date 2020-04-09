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

import studio.kdb.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
* Default values for the settings. They are used
* by <tt>BaseSettingsInitializer</tt> to initialize
* the settings with the default values. They can be also used
* for substitution if the value of the particular setting
* is unacceptable.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class SettingsDefaults {

    private static final Integer INTEGER_MAX_VALUE = new Integer(Integer.MAX_VALUE);

    // Caret color
    public static final Color defaultCaretColor = Color.black;

    // Empty coloring - it doesn't change font or colors
    public static final Coloring emptyColoring = new Coloring(null, null, null);

    // Default coloring
    private static int defaultFontSize ; // =24; // Fix of #33249
    public static Font defaultFont;

    static {
        //Font systemDefaultFont = Config.getInstance().getFont();
        defaultFont = Config.getInstance().getFont();
        defaultFontSize = defaultFont.getSize();
    }

    // public static Font defaultFont = new Font( "Monospaced", Font.PLAIN, defaultFontSize); // NOI18N
   // public static Font defaultFont = new Font( "Monospaced", Font.PLAIN, defaultFontSize); // NOI18N    
    public static final Color defaultForeColor = Color.black;
    public static Color defaultBackColor;
    public static Coloring defaultColoring;
    public static final void init()
    {
    defaultBackColor= Config.getInstance().getDefaultBackgroundColor();
        defaultColoring= new Coloring(defaultFont, defaultForeColor, defaultBackColor);
    }
    static{init();}
    // line number coloring
    public static final Color defaultLineNumberForeColor = new Color(128, 64, 64);
    public static final Color defaultLineNumberBackColor = new Color(224, 224, 224);
    public static final Coloring defaultLineNumberColoring
    = new Coloring(null, defaultLineNumberForeColor, defaultLineNumberBackColor);
    // caret selection coloring
    public static final Color defaultSelectionForeColor = Color.white;
    public static final Color defaultSelectionBackColor = Color.lightGray;
    public static final Coloring defaultSelectionColoring
    = new Coloring(null, defaultSelectionForeColor, defaultSelectionBackColor);
    // Highlight search coloring
    public static final Color defaultHighlightSearchForeColor = Color.black;
    public static final Color defaultHighlightSearchBackColor = new Color(255, 255, 128);
    public static final Coloring defaultHighlightSearchColoring
    = new Coloring(null, defaultHighlightSearchForeColor, defaultHighlightSearchBackColor);
    // Incremental search coloring
    public static final Color defaultIncSearchForeColor = Color.black;
    public static final Color defaultIncSearchBackColor = new Color(255, 107, 138);
    public static final Coloring defaultIncSearchColoring
    = new Coloring(null, defaultIncSearchForeColor, defaultIncSearchBackColor);
    /*
    // Bookmark coloring
    public static final Color defaultBookmarkForeColor = Color.black;
    public static final Color defaultBookmarkBackColor = new Color(184, 230, 230);
    public static final Coloring defaultBookmarkColoring
    = new Coloring(null, defaultBookmarkForeColor, defaultBookmarkBackColor);
     */
    // Guarded blocks coloring
    public static final Color defaultGuardedForeColor = null;
    public static final Color defaultGuardedBackColor = new Color(225, 236, 247);
    public static final Coloring defaultGuardedColoring
    = new Coloring(null, defaultGuardedForeColor, defaultGuardedBackColor);

    public static final Color defaultStatusBarForeColor = null;
    public static final Color defaultStatusBarBackColor
    = UIManager.getColor("ScrollPane.background"); // NOI18N
    public static final Coloring defaultStatusBarColoring
    = new Coloring(null, defaultStatusBarForeColor, defaultStatusBarBackColor);

    public static final Color defaultStatusBarBoldForeColor = Color.white;
    public static final Color defaultStatusBarBoldBackColor = Color.red;
    public static final Coloring defaultStatusBarBoldColoring
    = new Coloring(null, defaultStatusBarBoldForeColor, defaultStatusBarBoldBackColor);

    public static final Integer defaultCaretBlinkRate = new Integer(300);
    public static final Integer defaultTabSize = new Integer(4);
    public static final Integer defaultSpacesPerTab = new Integer(4);
    public static final Integer defaultShiftWidth = new Integer(4); // usually
    // not used as there's a Evaluator for shift width

    public static final Integer defaultStatusBarCaretDelay = new Integer(200);

    public static final Color defaultTextLimitLineColor = new Color(255, 235, 235);
    public static final Integer defaultTextLimitWidth = new Integer(80);

    public static final Acceptor defaultIdentifierAcceptor = AcceptorFactory.LETTER_DIGIT;
    public static final Acceptor defaultWhitespaceAcceptor = AcceptorFactory.WHITESPACE;

    public static final Float defaultLineHeightCorrection = new Float(1.0f);

    public static final Insets defaultLineNumberMargin = new Insets(0, 3, 0, 3);
    public static final Integer defaultTextLeftMarginWidth = new Integer(2);
    public static final Insets defaultScrollJumpInsets = new Insets(-5, -10, -5, -30);
    public static final Insets defaultScrollFindInsets = new Insets(0, -0, -10, -0);
    public static final Dimension defaultComponentSizeIncrement = new Dimension(-5, -30);

    public static final Integer defaultReadBufferSize = new Integer(16384);
    public static final Integer defaultWriteBufferSize = new Integer(16384);
    public static final Integer defaultReadMarkDistance = new Integer(180);
    public static final Integer defaultMarkDistance = new Integer(100);
    public static final Integer defaultMaxMarkDistance = new Integer(150);
    public static final Integer defaultMinMarkDistance = new Integer(50);
    public static final Integer defaultSyntaxUpdateBatchSize
    = new Integer(defaultMarkDistance.intValue() * 7);
    public static final Integer defaultLineBatchSize = new Integer(2);

    public static final Boolean defaultExpandTabs = Boolean.TRUE;

    public static final String defaultCaretTypeInsertMode = BaseCaret.LINE_CARET;
    public static final String defaultCaretTypeOverwriteMode = BaseCaret.BLOCK_CARET;
    public static final Color defaultCaretColorInsertMode = Color.black;
    public static final Color defaultCaretColorOvwerwriteMode = Color.black;
    public static final Boolean defaultCaretItalicInsertMode = Boolean.FALSE;
    public static final Boolean defaultCaretItalicOverwriteMode = Boolean.FALSE;
    public static final Acceptor defaultAbbrevExpandAcceptor = AcceptorFactory.WHITESPACE;
    public static final Acceptor defaultAbbrevAddTypedCharAcceptor = AcceptorFactory.NL;
    public static final Acceptor defaultAbbrevResetAcceptor = AcceptorFactory.NON_JAVA_IDENTIFIER;
    public static final Map defaultAbbrevMap = new HashMap();

    public static final Map defaultMacroMap = new HashMap();
    
    public static final Boolean defaultStatusBarVisible = Boolean.TRUE;

    public static final Boolean defaultLineNumberVisible = Boolean.TRUE;
    public static final Boolean defaultPrintLineNumberVisible = Boolean.TRUE;
    public static final Boolean defaultTextLimitLineVisible = Boolean.TRUE;
    public static final Boolean defaultHomeKeyColumnOne = Boolean.TRUE;
    public static final Boolean defaultWordMoveNewlineStop = Boolean.TRUE;
    public static final Boolean defaultInputMethodsEnabled = Boolean.TRUE;
    public static final Boolean defaultFindHighlightSearch = Boolean.TRUE;
    public static final Boolean defaultFindIncSearch = Boolean.TRUE;
    public static final Boolean defaultFindBackwardSearch = Boolean.FALSE;
    public static final Boolean defaultFindWrapSearch = Boolean.TRUE;
    public static final Boolean defaultFindMatchCase = Boolean.FALSE;
    public static final Boolean defaultFindWholeWords = Boolean.FALSE;
    public static final Boolean defaultFindRegExp = Boolean.FALSE;
    public static final Integer defaultFindHistorySize = new Integer(30);
    public static final Integer defaultWordMatchSearchLen = INTEGER_MAX_VALUE;
    public static final Boolean defaultWordMatchWrapSearch = Boolean.TRUE;
    public static final Boolean defaultWordMatchMatchOneChar = Boolean.TRUE;
    public static final Boolean defaultWordMatchMatchCase = Boolean.FALSE;
    public static final Boolean defaultWordMatchSmartCase = Boolean.FALSE;

    public static final String[] defaultColoringNames
    = new String[] {
          SettingsNames.DEFAULT_COLORING,
          SettingsNames.LINE_NUMBER_COLORING,
          SettingsNames.GUARDED_COLORING,
          SettingsNames.SELECTION_COLORING,
          SettingsNames.HIGHLIGHT_SEARCH_COLORING,
          SettingsNames.INC_SEARCH_COLORING,
//          SettingsNames.BOOKMARK_COLORING,
          SettingsNames.STATUS_BAR_COLORING,
          SettingsNames.STATUS_BAR_BOLD_COLORING
      };

        private final static int menuShortcutKeyMask= java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    public static final MultiKeyBinding[] defaultKeyBindings
    = new MultiKeyBinding[] {
          new MultiKeyBinding(
              (KeyStroke)null, // this assigns the default action to keymap
              BaseKit.defaultKeyTypedAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
              BaseKit.insertBreakAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
              BaseKit.insertTabAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK),
              BaseKit.removeTabAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
              BaseKit.deletePrevCharAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.SHIFT_MASK),
              BaseKit.deletePrevCharAction
          ),
/*          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
              BaseKit.deletePrevCharAction
          ),
*/          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
              BaseKit.deleteNextCharAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
              BaseKit.forwardAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), // keypad right
              BaseKit.forwardAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK),
              BaseKit.selectionForwardAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menuShortcutKeyMask),
              BaseKit.nextWordAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK | menuShortcutKeyMask),
              BaseKit.selectionNextWordAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
              BaseKit.backwardAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), // keypad left
              BaseKit.backwardAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK),
              BaseKit.selectionBackwardAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menuShortcutKeyMask),
              BaseKit.previousWordAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK | menuShortcutKeyMask),
              BaseKit.selectionPreviousWordAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
              BaseKit.downAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), // keypad down
              BaseKit.downAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK),
              BaseKit.selectionDownAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menuShortcutKeyMask),
              BaseKit.scrollUpAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
              BaseKit.upAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), // keypad up
              BaseKit.upAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK),
              BaseKit.selectionUpAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_UP, menuShortcutKeyMask),
              BaseKit.scrollDownAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
              BaseKit.pageDownAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_MASK),
              BaseKit.selectionPageDownAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
              BaseKit.pageUpAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK),
              BaseKit.selectionPageUpAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
              BaseKit.beginLineAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.SHIFT_MASK),
              BaseKit.selectionBeginLineAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_HOME, menuShortcutKeyMask),
              BaseKit.beginAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.SHIFT_MASK | menuShortcutKeyMask),
              BaseKit.selectionBeginAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
              BaseKit.endLineAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.SHIFT_MASK),
              BaseKit.selectionEndLineAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_END, menuShortcutKeyMask),
              BaseKit.endAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.SHIFT_MASK | menuShortcutKeyMask),
              BaseKit.selectionEndAction
          ),

          // clipboard bindings
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask),
              BaseKit.copyAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, menuShortcutKeyMask),
              BaseKit.copyAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_COPY, 0),
              BaseKit.copyAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK),
              BaseKit.cutAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask),
              BaseKit.cutAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_CUT, 0),
              BaseKit.cutAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_V,menuShortcutKeyMask),
              BaseKit.pasteAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK),
              BaseKit.pasteAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_PASTE, 0),
              BaseKit.pasteAction
          ),

          // undo and redo bindings - handled at system level
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcutKeyMask),
              BaseKit.undoAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0),
              BaseKit.undoAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutKeyMask),
              BaseKit.redoAction
          ),

          // other bindings
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask),
              BaseKit.selectAllAction
          ),
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_E, 0),
              },
              BaseKit.endWordAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutKeyMask),
              BaseKit.removeWordAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_U, menuShortcutKeyMask),
              BaseKit.removeLineBeginAction
          ),
/*          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK),
              BaseKit.removeLineAction
          ),
          */
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),
              BaseKit.toggleTypingModeAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F2, menuShortcutKeyMask),
              BaseKit.toggleBookmarkAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
              BaseKit.gotoNextBookmarkAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
              BaseKit.findNextAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK),
              BaseKit.findPreviousAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F3, menuShortcutKeyMask),
              BaseKit.findSelectionAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK),
              BaseKit.toggleHighlightSearchAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcutKeyMask),
              BaseKit.wordMatchNextAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_K, menuShortcutKeyMask),
              BaseKit.wordMatchPrevAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_T, menuShortcutKeyMask),
              BaseKit.shiftLineRightAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_D, menuShortcutKeyMask),
              BaseKit.shiftLineLeftAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK),
              BaseKit.abbrevResetAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
              BaseKit.annotationsCyclingAction
          ),

          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_T, 0),
              },
              BaseKit.adjustWindowTopAction
          ),
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_M, 0),
              },
              BaseKit.adjustWindowCenterAction
          ),
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_B, 0),
              },
              BaseKit.adjustWindowBottomAction
          ),

          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK),
              BaseKit.adjustCaretTopAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK),
              BaseKit.adjustCaretCenterAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK),
              BaseKit.adjustCaretBottomAction
          ),

          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutKeyMask | InputEvent.SHIFT_MASK ),
              BaseKit.formatAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.ALT_MASK),
              BaseKit.selectIdentifierAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_MASK),
              BaseKit.jumpListPrevAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK),
              BaseKit.jumpListNextAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK),
              BaseKit.jumpListPrevComponentAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.SHIFT_MASK | InputEvent.ALT_MASK),
              BaseKit.jumpListNextComponentAction
          ),
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, 0),
              },
              BaseKit.toUpperCaseAction
          ),
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_L, 0),
              },
              BaseKit.toLowerCaseAction
          ),
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK),
                  KeyStroke.getKeyStroke(KeyEvent.VK_R, 0),
              },
              BaseKit.switchCaseAction
          ),
/*
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_M, menuShortcutKeyMask),
              BaseKit.selectNextParameterAction
          ),
*/
          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_J, menuShortcutKeyMask),
                  KeyStroke.getKeyStroke(KeyEvent.VK_S, 0),
              },
              BaseKit.startMacroRecordingAction
          ),

          new MultiKeyBinding(
              new KeyStroke[] {
                  KeyStroke.getKeyStroke(KeyEvent.VK_J, menuShortcutKeyMask),
                  KeyStroke.getKeyStroke(KeyEvent.VK_E, 0),
              },
              BaseKit.stopMacroRecordingAction
          ),
      };
}
