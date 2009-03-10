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

import org.netbeans.editor.Coloring;
import org.netbeans.editor.MultiKeyBinding;
import org.netbeans.editor.SettingsDefaults;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
* Initializer for the extended editor settings.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ExtSettingsDefaults extends SettingsDefaults {

    // Highlight row with caret coloring
    public static final Color defaultHighlightCaretRowBackColor = new Color(255, 255, 220);
    public static final Coloring defaultHighlightCaretRowColoring
    = new Coloring(null, null, defaultHighlightCaretRowBackColor);
    // Highlight matching brace coloring
    public static final Color defaultHighlightMatchBraceForeColor = Color.white;
    public static final Color defaultHighlightMatchBraceBackColor = new Color(255, 50, 210);
    public static final Coloring defaultHighlightMatchBraceColoring
    = new Coloring(null, defaultHighlightMatchBraceForeColor, defaultHighlightMatchBraceBackColor);

    public static final Boolean defaultHighlightCaretRow = Boolean.FALSE;
    public static final Boolean defaultHighlightMatchBrace = Boolean.TRUE;
    public static final Integer defaultHighlightMatchBraceDelay = new Integer(100);
    public static final Boolean defaultCaretSimpleMatchBrace = Boolean.TRUE;

    public static final Boolean defaultCompletionAutoPopup = Boolean.TRUE;
    public static final Boolean defaultCompletionCaseSensitive = Boolean.TRUE;
    public static final Boolean defaultCompletionNaturalSort = Boolean.FALSE;    
    public static final Integer defaultCompletionAutoPopupDelay = new Integer(500);
    public static final Integer defaultCompletionRefreshDelay = new Integer(200);
    public static final Dimension defaultCompletionPaneMaxSize = new Dimension(400, 300);
    public static final Dimension defaultCompletionPaneMinSize = new Dimension(60, 34);
    public static final Boolean defaultDisplayGoToClassInfo = Boolean.TRUE;
    public static final Boolean defaultFastImportPackage = Boolean.FALSE;    
    public static final Integer defaultFastImportSelection = new Integer(0);
    public static final Boolean defaultShowDeprecatedMembers = Boolean.TRUE;
    public static final Boolean defaultCompletionInstantSubstitution = Boolean.TRUE;    
    
    public static final Color defaultJavaDocBGColor = new Color(247, 247, 255);
    public static final Integer defaultJavaDocAutoPopupDelay = new Integer(200);
    public static final Dimension defaultJavaDocPreferredSize = new Dimension(500, 300);    
    public static final Boolean defaultJavaDocAutoPopup = Boolean.TRUE;
    public static final String defaultUpdatePDAfterMounting = ExtSettingsNames.ALWAYS;
    
    public static final MultiKeyBinding[] defaultExtKeyBindings
    = new MultiKeyBinding[] {
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.ALT_MASK),
              ExtKit.gotoDeclarationAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK),
              ExtKit.findAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_FIND, 0),
              ExtKit.findAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK),
              ExtKit.replaceAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK),
              ExtKit.gotoAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_MASK),
              ExtKit.completionShowAction
          ),
          new MultiKeyBinding( // Japanese Solaris uses CTRL+SPACE for IM
              KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, InputEvent.CTRL_MASK),
              ExtKit.completionShowAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              ExtKit.escapeAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_MASK),
              ExtKit.matchBraceAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
              ExtKit.selectionMatchBraceAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK),
              ExtKit.shiftInsertBreakAction
          ),
          new MultiKeyBinding(
              KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_MASK),
              ExtKit.showPopupMenuAction
          ),
          /*      new MultiKeyBinding(
                    KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK),
          //          KeyStroke.getKeyStroke(KeyEvent.VK_BRACELEFT, InputEvent.CTRL_MASK),
                    ExtKit.braceCodeSelectAction
                ),
          */

      };

}
