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

package org.netbeans.editor;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
* Actions that are not considered basic and therefore
* they are not included directly in BaseKit, but here.
* Their names however are still part of BaseKit.
*
* @author Miloslav Metelka
* @version 1.00
*
* TODO: I18N in RunMacroAction errors
*/

public class ActionFactory {

    private ActionFactory() {
        // no instantiation
    }
    
    public static class RemoveTabAction extends BaseAction {

        static final long serialVersionUID =-1537748600593395706L;

        public RemoveTabAction() {
            super(BaseKit.removeTabAction, MAGIC_POSITION_RESET | ABBREV_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                Caret caret = target.getCaret();
                BaseDocument doc = (BaseDocument)target.getDocument();
                if (caret.isSelectionVisible()) { // block selected
                    try {
                        doc.getFormatter().changeBlockIndent(doc,
                                target.getSelectionStart(), target.getSelectionEnd(), -1);
                    } catch (GuardedException e) {
                        target.getToolkit().beep();
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                } else { // no selected text
                    try {
                        int startOffset = Utilities.getRowStart(doc, caret.getDot());
                        int firstNW = Utilities.getRowFirstNonWhite(doc, caret.getDot());
                        int endOffset = Utilities.getRowEnd(doc, caret.getDot());
                        if (firstNW == -1 || (firstNW >= caret.getDot()))
                            doc.getFormatter().changeBlockIndent(doc, startOffset, endOffset, -1);
                        else {
                            // TODO:
                            // after we will have action which will do opposite to "tab" action
                            // means it check whether before the caret is whole tab which can
                            // be removed, this action will be called here
                        }
                    } catch (GuardedException e) {
                        target.getToolkit().beep();
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    public static class RemoveWordAction extends BaseAction {

        static final long serialVersionUID =9193117196412195554L;

        public RemoveWordAction() {
            super(BaseKit.removeWordAction, MAGIC_POSITION_RESET
                  | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                Caret caret = target.getCaret();
                try {
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    int dotPos = caret.getDot();
                    int bolPos = Utilities.getRowStart(doc, dotPos);
                    int wsPos = Utilities.getPreviousWord(target, dotPos);
                    wsPos = (dotPos == bolPos) ? wsPos : Math.max(bolPos, wsPos);
                    doc.remove(wsPos, dotPos - wsPos);
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class RemoveLineBeginAction extends BaseAction {

        static final long serialVersionUID =9193117196412195554L;

        public RemoveLineBeginAction() {
            super(BaseKit.removeLineBeginAction, MAGIC_POSITION_RESET
                  | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                Caret caret = target.getCaret();
                try {
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    int dotPos = caret.getDot();
                    int bolPos = Utilities.getRowStart(doc, dotPos);
                    if (dotPos == bolPos) { // at begining of the line
                        if (dotPos > 0) {
                            doc.remove(dotPos - 1, 1); // remove previous new-line
                        }
                    } else { // not at the line begining
                        char[] chars = doc.getChars(bolPos, dotPos - bolPos);
                        if (Analyzer.isWhitespace(chars, 0, chars.length)) {
                            doc.remove(bolPos, dotPos - bolPos); // remove whitespace
                        } else {
                            int firstNW = Utilities.getRowFirstNonWhite(doc, bolPos);
                            if (firstNW >= 0 && firstNW < dotPos) {
                                doc.remove(firstNW, dotPos - firstNW);
                            }
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class RemoveLineAction extends BaseAction {

        static final long serialVersionUID =-536315497241419877L;

        public RemoveLineAction() {
            super(BaseKit.removeLineAction, MAGIC_POSITION_RESET
                  | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                Caret caret = target.getCaret();
                try {
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    int dotPos = caret.getDot();
                    int bolPos = Utilities.getRowStart(target, dotPos);
                    int eolPos = Utilities.getRowEnd(target, dotPos);
                    eolPos = Math.min(eolPos + 1, doc.getLength()); // include '\n'
                    doc.remove(bolPos, eolPos - bolPos);
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    /* Useful for popup menu - remove selected block or do nothing */
    public static class RemoveSelectionAction extends BaseAction {

        static final long serialVersionUID =-1419424594746686573L;

        public RemoveSelectionAction() {
            super(BaseKit.removeSelectionAction, MAGIC_POSITION_RESET
                  | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            putValue ("helpID", RemoveSelectionAction.class.getName ());
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                target.replaceSelection(null);
            }
        }
    }

    /** Switch to overwrite mode or back to insert mode */
    public static class ToggleTypingModeAction extends BaseAction {

        static final long serialVersionUID =-2431132686507799723L;

        public ToggleTypingModeAction() {
            super(BaseKit.toggleTypingModeAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI editorUI = Utilities.getEditorUI(target);
                Boolean overwriteMode = (Boolean)editorUI.getProperty(EditorUI.OVERWRITE_MODE_PROPERTY);
                // Now toggle
                overwriteMode = (overwriteMode == null || !overwriteMode.booleanValue())
                                ? Boolean.TRUE : Boolean.FALSE;
                editorUI.putProperty(EditorUI.OVERWRITE_MODE_PROPERTY, overwriteMode);
            }
        }
    }

    public static class ToggleBookmarkAction extends BaseAction {

        static final long serialVersionUID =-8438899482709646741L;

        public ToggleBookmarkAction() {
            super(BaseKit.toggleBookmarkAction);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/toggle_bookmark");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    doc.toggleBookmark(caret.getDot());
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class GotoNextBookmarkAction extends BaseAction {

        boolean select;

        static final long serialVersionUID =-5169554640178645108L;

        public GotoNextBookmarkAction(String nm, boolean select) {
            super(BaseKit.gotoNextBookmarkAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            this.select = select;
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/next_bookmark");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    int dotPos = doc.getNextBookmark(caret.getDot(), true); // wrap
                    if (dotPos >= 0) {
                        if (select) {
                            caret.moveDot(dotPos);
                        } else {
                            caret.setDot(dotPos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }



    public static class RunMacroAction extends BaseAction {

        static final long serialVersionUID =1L;

        static HashSet runningActions = new HashSet();
        private String macroName;

        public RunMacroAction( String name ) {
            super( BaseKit.macroActionPrefix + name);
            this.macroName = name;
        }

        protected void error( JTextComponent target, String messageKey ) {
            Utilities.setStatusText( target, LocaleSupport.getString(
                messageKey, "Error in macro: " + messageKey )
            );
            Toolkit.getDefaultToolkit().beep();
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if( !runningActions.add( macroName ) ) { // this macro is already running, beware of loops
                error( target, "loop" ); 
                return;
            }

            if( target == null ) return;
           
            BaseKit kit = Utilities.getKit(target);
            if( kit == null ) return;
            
            Map macroMap = (Map)Settings.getValue( kit.getClass(), SettingsNames.MACRO_MAP);
            
            String commandString = (String)macroMap.get( macroName );

            if( commandString == null ) {
                error( target, "macro-not-found" );
                runningActions.remove( macroName );
                return;
            }

            StringBuffer actionName = new StringBuffer();
            char[] command = commandString.toCharArray();
            int len = command.length;

            BaseDocument doc = (BaseDocument)target.getDocument();
            try {
                doc.atomicLock();
                
                for( int i = 0; i < len; i++ ) {
                    if( Character.isWhitespace( command[i] ) ) continue;
                    if( command[i] == '"' ) {
                        while( ++i < len && command[i] != '"' ) {
                            char ch = command[i];
                            if( ch == '\\' ) {
                                if( ++i >= len ) { // '\' at the end
                                    error( target, "macro-malformed" );
                                    return;
                                }
                                ch = command[i];
                                if( ch != '"' && ch != '\\' ) { // neither \\ nor \"
                                    error( target, "macro-malformed" );
                                    return;
                                } // else fall through
                            }
                            Action a = target.getKeymap().getDefaultAction();

                            if (a != null) {
                                ActionEvent newEvt = new ActionEvent( target, 0, new String( new char[] { ch } ) );
                                if( a instanceof BaseAction ) {
                                    ((BaseAction)a).updateComponent(target);
                                    ((BaseAction)a).actionPerformed( newEvt, target );
                                } else {
                                    a.actionPerformed( newEvt );
                                }
                            }
                        }
                    } else { // parse the action name
                        actionName.setLength( 0 );
                        while( i < len && ! Character.isWhitespace( command[i] ) ) {
                            char ch = command[i++];
                            if( ch == '\\' ) {
                                if( i >= len ) { // macro ending with single '\'
                                    error( target, "macro-malformed" );
                                    return;
                                }; 
                                ch = command[i++];
                                if( ch != '\\' && ! Character.isWhitespace( ch ) ) {//
                                    error( target, "macro-malformed" ); // neither "\\" nor "\ "
                                    return;
                                } // else fall through
                            }
                            actionName.append( ch );
                        }
                        // execute the action
                        Action a = kit.getActionByName( actionName.toString() );
                        if (a != null) {
                            ActionEvent fakeEvt = new ActionEvent( target, 0, "" );
                            if( a instanceof BaseAction ) {
                                ((BaseAction)a).updateComponent(target);
                                ((BaseAction)a).actionPerformed( fakeEvt, target );
                            } else {
                                a.actionPerformed( fakeEvt );
                            }
                        } else {
                            error( target, "macro-unknown-action" );
                            return;
                        }
                    }
                }
            } finally {
                doc.atomicUnlock();
                runningActions.remove( macroName );
            }
        }
    }
    
    
    public static class StartMacroRecordingAction extends BaseAction {

        static final long serialVersionUID =1L;

        public StartMacroRecordingAction() {
            super( BaseKit.startMacroRecordingAction, NO_RECORDING );
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/macro_start");
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if( !startRecording(target) ) target.getToolkit().beep();
            }
        }
    }

    public static class StopMacroRecordingAction extends BaseAction {

        static final long serialVersionUID =1L;

        public StopMacroRecordingAction() {
            super( BaseKit.stopMacroRecordingAction, NO_RECORDING );
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/macro_stop");
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                String macro = stopRecording(target);
                if( macro == null ) { // not recording
                    target.getToolkit().beep();
                } else {
                    // popup a macro dialog
                    BaseKit kit = Utilities.getKit(target);
                    MacroDialogSupport support = new MacroDialogSupport( kit.getClass() );
                    support.setBody( macro );
                    support.showMacroDialog();
                }
            }
        }
    }

    
    public static class AbbrevExpandAction extends BaseAction {

        static final long serialVersionUID =-2124569510083544403L;

        public AbbrevExpandAction() {
            super(BaseKit.abbrevExpandAction,
                  MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                EditorUI editorUI = ((BaseTextUI)target.getUI()).getEditorUI();
                try {
                    editorUI.getAbbrev().checkAndExpand(evt);
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class AbbrevResetAction extends BaseAction {

        static final long serialVersionUID =-2807497346060448395L;

        public AbbrevResetAction() {
            super(BaseKit.abbrevResetAction, ABBREV_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
        }

    }

    public static class ChangeCaseAction extends BaseAction {

        int changeCaseMode;

        static final long serialVersionUID =5680212865619897402L;

        public ChangeCaseAction(String name, int changeCaseMode) {
            super(name, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            this.changeCaseMode = changeCaseMode;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    if (caret.isSelectionVisible()) { // valid selection
                        int startPos = target.getSelectionStart();
                        int endPos = target.getSelectionEnd();
                        Utilities.changeCase(doc, startPos, endPos - startPos, changeCaseMode);
                        caret.setSelectionVisible(false);
                        caret.setDot(endPos);
                    } else { // no selection - change current char
                        int dotPos = caret.getDot();
                        Utilities.changeCase(doc, dotPos, 1, changeCaseMode);
                        caret.setDot(dotPos + 1);
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }


    public static class FindNextAction extends BaseAction {

        static final long serialVersionUID =6878814427731642684L;

        public FindNextAction() {
            super(BaseKit.findNextAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/find_next");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                FindSupport.getFindSupport().find(null, false);
            }
        }
    }

    public static class FindPreviousAction extends BaseAction {

        static final long serialVersionUID =-43746947902694926L;

        public FindPreviousAction() {
            super(BaseKit.findPreviousAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/find_previous");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                FindSupport.getFindSupport().find(null, true);
            }
        }
    }

    /** Finds either selection or if there's no selection it finds
    * the word where the cursor is standing.
    */
    public static class FindSelectionAction extends BaseAction {

        static final long serialVersionUID =-5601618936504699565L;

        public FindSelectionAction() {
            super(BaseKit.findSelectionAction);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/find_selection");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                FindSupport findSupport = FindSupport.getFindSupport();
                Caret caret = target.getCaret();
                int dotPos = caret.getDot();
                HashMap props = new HashMap(findSupport.getFindProperties());
                String searchWord = null;
                boolean revert = false;
                
                if (caret.isSelectionVisible()) { // valid selection
                    searchWord = target.getSelectedText();
                    revert = !Boolean.FALSE.equals(props.put(SettingsNames.FIND_WHOLE_WORDS, Boolean.FALSE));
                } else { // no selection, get current word
                    try {
                        searchWord = Utilities.getIdentifier((BaseDocument)target.getDocument(),
                                                             dotPos);
                        revert =  !Boolean.TRUE.equals(props.put(SettingsNames.FIND_WHOLE_WORDS, Boolean.TRUE));
                        
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }

                if (searchWord != null) {
                    int n = searchWord.indexOf( '\n' );
                    if (n >= 0 ) 
                        searchWord = searchWord.substring(0, n);
                    props.put(SettingsNames.FIND_WHAT, searchWord);
                
                    if (revert){
                        HashMap revertMap = new HashMap();                                            
                        revertMap.put(SettingsNames.FIND_WHOLE_WORDS, Boolean.TRUE);
                        props.put(FindSupport.REVERT_MAP, revertMap);
                    }

                    findSupport.putFindProperties(props);
                    findSupport.find(null, false);
                }
            }
        }
    }

    public static class ToggleHighlightSearchAction extends BaseAction {

        static final long serialVersionUID =4603809175771743200L;

        public ToggleHighlightSearchAction() {
            super(BaseKit.toggleHighlightSearchAction, CLEAR_STATUS_TEXT);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/toggle_search");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Boolean cur = (Boolean)FindSupport.getFindSupport().getFindProperty(
                                  SettingsNames.FIND_HIGHLIGHT_SEARCH);
                if (cur == null || cur.booleanValue() == false) {
                    cur = Boolean.TRUE;
                } else {
                    cur = Boolean.FALSE;
                }
                FindSupport.getFindSupport().putFindProperty(
                    SettingsNames.FIND_HIGHLIGHT_SEARCH, cur);
            }
        }
    }

    public static class UndoAction extends BaseAction {

        static final long serialVersionUID =8628586205035497612L;

        public UndoAction() {
            super(BaseKit.undoAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (!target.isEditable() || !target.isEnabled()) {
                target.getToolkit().beep();
                return;
            }

            Document doc = target.getDocument();
            UndoableEdit undoMgr = (UndoableEdit)doc.getProperty(
                                       BaseDocument.UNDO_MANAGER_PROP);
            if (target != null && undoMgr != null) {
                try {
                    undoMgr.undo();
                } catch (CannotUndoException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class RedoAction extends BaseAction {

        static final long serialVersionUID =6048125996333769202L;

        public RedoAction() {
            super(BaseKit.redoAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (!target.isEditable() || !target.isEnabled()) {
                target.getToolkit().beep();
                return;
            }

            Document doc = target.getDocument();
            UndoableEdit undoMgr = (UndoableEdit)doc.getProperty(
                                       BaseDocument.UNDO_MANAGER_PROP);
            if (target != null && undoMgr != null) {
                try {
                    undoMgr.redo();
                } catch (CannotRedoException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class WordMatchAction extends BaseAction {

        private boolean direction;

        static final long serialVersionUID =595571114685133170L;

        public WordMatchAction(String name, boolean direction) {
            super(name, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            this.direction = direction;
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                direction
                    ? "org/netbeans/modules/editor/resources/match_next"
                    : "org/netbeans/modules/editor/resources/match_previous"
            );
                        
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                EditorUI editorUI = Utilities.getEditorUI(target);
                Caret caret = target.getCaret();
                final BaseDocument doc = Utilities.getDocument(target);

                // Possibly remove selection
                if (caret.isSelectionVisible()) {
                    target.replaceSelection(null);
                }

                int dotPos = caret.getDot();
                String s = editorUI.getWordMatch().getMatchWord(dotPos, direction);
                String prevWord = editorUI.getWordMatch().getPreviousWord();
                if (s != null) {
                    doc.atomicLock();
                    try {
                        int pos = dotPos;
                        if (prevWord != null && prevWord.length() > 0) {
                            pos -= prevWord.length();
                            doc.remove(pos, prevWord.length());
                        }
                        doc.insertString(pos, s, null);
                    } catch (BadLocationException e) {
                        target.getToolkit().beep();
                    } finally {
                        doc.atomicUnlock();
                    }
                }
            }
        }
    }


    public static class ShiftLineAction extends BaseAction {

        boolean right;

        static final long serialVersionUID =-5124732597493699582L;

        public ShiftLineAction(String name, boolean right) {
            super(name, MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            this.right = right;
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                right
                    ? "org/netbeans/modules/editor/resources/shift_right"
                    : "org/netbeans/modules/editor/resources/shift_left"
            );

        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = Utilities.getDocument(target);
                    if (caret.isSelectionVisible()) {
                        doc.getFormatter().changeBlockIndent(doc,
                        target.getSelectionStart(), target.getSelectionEnd(),
                        right ? +1 : -1);
                    } else {
                        doc.getFormatter().shiftLine(doc, caret.getDot(), right);
                    }
                } catch (GuardedException e) {
                    target.getToolkit().beep();
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class AdjustWindowAction extends BaseAction {

        int percentFromWindowTop;

        static final long serialVersionUID =8864278998999643292L;

        public AdjustWindowAction(String name, int percentFromWindowTop) {
            super(name);
            this.percentFromWindowTop = percentFromWindowTop;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Utilities.getEditorUI(target).adjustWindow(percentFromWindowTop);
            }
        }
    }

    public static class AdjustCaretAction extends BaseAction {

        int percentFromWindowTop;

        static final long serialVersionUID =3223383913531191066L;

        public AdjustCaretAction(String name, int percentFromWindowTop) {
            super(name);
            this.percentFromWindowTop = percentFromWindowTop;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Utilities.getEditorUI(target).adjustCaret(percentFromWindowTop);
            }
        }
    }

    public static class FormatAction extends BaseAction {

        static final long serialVersionUID =-7666172828961171865L;

        public FormatAction() {
            super(BaseKit.formatAction,
                  ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            putValue ("helpID", FormatAction.class.getName ());
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                Caret caret = target.getCaret();
                BaseDocument doc = (BaseDocument)target.getDocument();
                GuardedDocument gdoc = (doc instanceof GuardedDocument)
                                       ? (GuardedDocument)doc : null;

                doc.atomicLock();
                try {

                    int caretLine = Utilities.getLineOffset(doc, caret.getDot());
                    int startPos;
                    Position endPosition;
                    if (caret.isSelectionVisible()) {
                        startPos = target.getSelectionStart();
                        endPosition = doc.createPosition(target.getSelectionEnd());
                    } else {
                        startPos = 0;
                        endPosition = doc.createPosition(doc.getLength());
                    }

                    int pos = startPos;
                    if (gdoc != null) {
                        pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
                    }

                    while (pos < endPosition.getOffset()) {
                        int stopPos = endPosition.getOffset();
                        if (gdoc != null) { // adjust to start of the next guarded block
                            stopPos = gdoc.getGuardedBlockChain().adjustToNextBlockStart(pos);
                            if (stopPos == -1) {
                                stopPos = endPosition.getOffset();
                            }
                        }

                        int reformattedLen = doc.getFormatter().reformat(doc, pos, stopPos);
                        pos = pos + reformattedLen;

                        if (gdoc != null) { // adjust to end of current block
                            pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
                        }
                    }

                    // Restore the line
                    pos = Utilities.getRowStartFromLineOffset(doc, caretLine);
                    if (pos >= 0) {
                        caret.setDot(pos);
                    }
                } catch (GuardedException e) {
                    target.getToolkit().beep();
                } catch (BadLocationException e) {
                    if (System.getProperty("netbeans.debug.exceptions") != null) { // NOI18N
                        e.printStackTrace();
                    }
                } finally {
                    doc.atomicUnlock();
                }

            }
        }
        
    }

    public static class FirstNonWhiteAction extends BaseAction {

        boolean select;

        static final long serialVersionUID =-5888439539790901158L;

        public FirstNonWhiteAction(String nm, boolean select) {
            super(nm, MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET
                  | WORD_MATCH_RESET);
            this.select = select;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    int pos = Utilities.getRowFirstNonWhite((BaseDocument)target.getDocument(),
                                                            caret.getDot());
                    if (pos >= 0) {
                        if (select) {
                            caret.moveDot(pos);
                        } else {
                            caret.setDot(pos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class LastNonWhiteAction extends BaseAction {

        boolean select;

        static final long serialVersionUID =4503533041729712917L;

        public LastNonWhiteAction(String nm, boolean select) {
            super(nm, MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET
                  | WORD_MATCH_RESET);
            this.select = select;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    int pos = Utilities.getRowLastNonWhite((BaseDocument)target.getDocument(),
                                                           caret.getDot());
                    if (pos >= 0) {
                        if (select) {
                            caret.moveDot(pos);
                        } else {
                            caret.setDot(pos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class SelectIdentifierAction extends BaseAction {

        static final long serialVersionUID =-7288216961333147873L;

        public SelectIdentifierAction() {
            super(BaseKit.selectIdentifierAction, MAGIC_POSITION_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    if (caret.isSelectionVisible()) {
                        caret.setSelectionVisible(false); // unselect if anything selected
                    } else { // selection not visible
                        int block[] = Utilities.getIdentifierBlock((BaseDocument)target.getDocument(),
                                      caret.getDot());
                        if (block != null) {
                            caret.setDot(block[0]);
                            caret.moveDot(block[1]);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class SelectNextParameterAction extends BaseAction {

        static final long serialVersionUID =8045372985336370934L;

        public SelectNextParameterAction() {
            super(BaseKit.selectNextParameterAction, MAGIC_POSITION_RESET | CLEAR_STATUS_TEXT);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                BaseDocument doc = (BaseDocument)target.getDocument();
                int dotPos = caret.getDot();
                int selectStartPos = -1;
                try {
                    if (dotPos > 0) {
                        if (doc.getChars(dotPos - 1, 1)[0] == ',') { // right after the comma
                            selectStartPos = dotPos;
                        }
                    }
                    if (dotPos < doc.getLength()) {
                        char dotChar = doc.getChars(dotPos, 1)[0];
                        if (dotChar == ',') {
                            selectStartPos = dotPos + 1;
                        } else if (dotChar == ')') {
                            caret.setDot(dotPos + 1);
                        }
                    }
                    if (selectStartPos >= 0) {
                        int selectEndPos = doc.find(
                                               new FinderFactory.CharArrayFwdFinder( new char[] { ',', ')' }),
                                               selectStartPos, -1
                                           );
                        if (selectEndPos >= 0) {
                            target.select(selectStartPos, selectEndPos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class JumpListNextAction extends BaseAction {

        static final long serialVersionUID =6891721278404990446L;

        public JumpListNextAction() {
            super(BaseKit.jumpListNextAction);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/edit_next");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpNext(target);
            }
        }
    }

    public static class JumpListPrevAction extends BaseAction {

        static final long serialVersionUID =7174907031986424265L;

        public JumpListPrevAction() {
            super(BaseKit.jumpListPrevAction);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/edit_previous");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpPrev(target);
            }
        }
    }

    public static class JumpListNextComponentAction extends BaseAction {

        static final long serialVersionUID =-2059070050865876892L;

        public JumpListNextComponentAction() {
            super(BaseKit.jumpListNextComponentAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpNextComponent(target);
            }
        }
    }

    public static class JumpListPrevComponentAction extends BaseAction {

        static final long serialVersionUID =2032230534727849525L;

        public JumpListPrevComponentAction() {
            super(BaseKit.jumpListPrevComponentAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpPrevComponent(target);
            }
        }
    }

    public static class ScrollUpAction extends BaseAction {

        public ScrollUpAction() {
            super(BaseKit.scrollUpAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI editorUI = Utilities.getEditorUI(target);
                Rectangle bounds = editorUI.getExtentBounds();
                bounds.y += editorUI.getLineHeight();
                editorUI.scrollRectToVisible(bounds, EditorUI.SCROLL_SMALLEST);
            }
        }

    }

    public static class ScrollDownAction extends BaseAction {

        public ScrollDownAction() {
            super(BaseKit.scrollDownAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI editorUI = Utilities.getEditorUI(target);
                Rectangle bounds = editorUI.getExtentBounds();
                bounds.y -= editorUI.getLineHeight();
                editorUI.scrollRectToVisible(bounds, EditorUI.SCROLL_SMALLEST);
            }
        }

    }

    public static class InsertDateTimeAction extends BaseAction {
        
        static final long serialVersionUID =2865619897402L;
        
        public InsertDateTimeAction() {
            super(BaseKit.insertDateTimeAction,
            ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                
                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    
                    // Format the current time.
                    SimpleDateFormat formatter = new SimpleDateFormat();
                    Date currentTime = new Date();
                    String dateString = formatter.format(currentTime);
                    
                    doc.insertString(caret.getDot(), dateString, null);
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }
    
    /** Select text of whole document */
    public static class GenerateGutterPopupAction extends BaseAction {

        static final long serialVersionUID =-3502499718130556525L;

        public GenerateGutterPopupAction() {
            super(BaseKit.generateGutterPopupAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
        }

        public JMenuItem getPopupMenuItem(JTextComponent target) {
            EditorUI ui = Utilities.getEditorUI(target);
            try {
                return ui.getDocument().getAnnotations().createMenu(Utilities.getKit(target), Utilities.getLineOffset(ui.getDocument(),target.getCaret().getDot()));
            } catch (BadLocationException ex) {
                return null;
            }
        }
    
    }

    /** Switch visibility of line numbers in editor */
    public static class ToggleLineNumbersAction extends BaseAction {

        static final long serialVersionUID =-3502499718130556526L;
        
        private JCheckBoxMenuItem item = null;

        public ToggleLineNumbersAction() {
            super(BaseKit.toggleLineNumbersAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            toggleLineNumbers();
        }
        
        public JMenuItem getPopupMenuItem(JTextComponent target) {
            if (item != null) {
                return item;
            }
            
            item = new JCheckBoxMenuItem(LocaleSupport.getString("line-numbers-menuitem"), isLineNumbersVisible());
            item.addItemListener( new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    actionPerformed(null,null);
                }
            });
            return item;
        }
        
        protected boolean isLineNumbersVisible() {
            return false;
        }
        
        protected void toggleLineNumbers() {
        }
        
    }
    
    /** Cycle through annotations on the current line */
    public static class AnnotationsCyclingAction extends BaseAction {
        
        public AnnotationsCyclingAction() {
            super(BaseKit.annotationsCyclingAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = Utilities.getDocument(target);
                    int caretLine = Utilities.getLineOffset(doc, caret.getDot());
                    AnnotationDesc aDesc = doc.getAnnotations().activateNextAnnotation(caretLine);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
