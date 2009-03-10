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
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.event.ActionEvent;

/**
* This is the parent of majority of the actions. It implements
* the necessary resetting depending of what is required
* by constructor of target action.
* The other thing implemented here is macro recording.
*
* @author Miloslav Metelka
* @version 1.00
*/

public abstract class BaseAction extends TextAction {

    /** Text of the menu item in popup menu for this action */
    public static final String POPUP_MENU_TEXT = "PopupMenuText"; // NOI18N

    /** Prefix for the name of the key for description in locale support */
    public static final String LOCALE_DESC_PREFIX = "desc-"; // NOI18N

    /** Prefix for the name of the key for popup description in locale support */
    public static final String LOCALE_POPUP_PREFIX = "popup-"; // NOI18N

    /** Resource for the icon */
    public static final String ICON_RESOURCE_PROPERTY = "IconResource"; // NOI18N

    /** Remove the selected text at the action begining */
    public static final int SELECTION_REMOVE = 1;

    /** Reset magic caret position */
    public static final int MAGIC_POSITION_RESET = 2;

    /** Reset abbreviation accounting to empty string */
    public static final int ABBREV_RESET = 4;

    /** Prevents adding the new undoable edit to the old one when the next
    * document change occurs.
    */
    public static final int UNDO_MERGE_RESET = 8;

    /** Reset word-match table */
    public static final int WORD_MATCH_RESET = 16;

    /** Clear status bar text */
    public static final int CLEAR_STATUS_TEXT = 32;

    /** The action will not be recorded if in macro recording */
    public static final int NO_RECORDING = 64;

    /** Save current position in the jump list */
    public static final int SAVE_POSITION = 128;


    /** Bit mask of what should be updated when the action is performed before
    * the action's real task is invoked.
    */
    protected int updateMask;
    
    private static boolean recording;
    private static StringBuffer macroBuffer = new StringBuffer();
    private static StringBuffer textBuffer = new StringBuffer();

    static final long serialVersionUID =-4255521122272110786L;

    public BaseAction(String name) {
        this(name, 0);
    }

    public BaseAction(String name, int updateMask) {
        super(name);
        this.updateMask = updateMask;
        // Initialize short description
        String key = LOCALE_DESC_PREFIX + name;
        String desc = LocaleSupport.getString(key);
        if (desc == null) {
            desc = LocaleSupport.getString(name, name);
        }
        putValue(SHORT_DESCRIPTION, desc);

        // Initialize menu description
        key = LOCALE_POPUP_PREFIX + name;
        String popupMenuText = LocaleSupport.getString(key, desc);
        putValue(POPUP_MENU_TEXT, popupMenuText);
    }

    /** This method is called once after the action is constructed
    * and then each time the settings are changed.
    * @param evt event describing the changed setting name. It's null
    *   if it's called after the action construction.
    * @param kitClass class of the kit that created the actions
    */
    protected void settingsChange(SettingsChangeEvent evt, Class kitClass) {
    }

    /** This method is made final here as there's an important
    * processing that must be done before the real action
    * functionality is performed. It can include the following:
    * 1. Updating of the target component depending on the update
    *    mask given in action constructor.
    * 2. Possible macro recoding when the macro recording
    *    is turned on.
    * The real action functionality should be done in
    * the method actionPerformed(ActionEvent evt, JTextComponent target)
    * which must be redefined by the target action.
    */
    public final void actionPerformed(ActionEvent evt) {
        JTextComponent target = getTextComponent(evt);
                              
        if( recording && 0 == (updateMask & NO_RECORDING) ) {
            recordAction( target, evt );
        }
        

        updateComponent(target);

        actionPerformed(evt, target);
    }
    
    private void recordAction( JTextComponent target, ActionEvent evt ) {
        if( this == target.getKeymap().getDefaultAction() ) { // defaultKeyTyped
            textBuffer.append( getFilteredActionCommand(evt.getActionCommand()) );
        } else { // regular action
            if( textBuffer.length() > 0 ) {
                if( macroBuffer.length() > 0 ) macroBuffer.append( ' ' ); 
                macroBuffer.append( encodeText( textBuffer.toString() ) );
                textBuffer.setLength( 0 );
            }
            if( macroBuffer.length() > 0 ) macroBuffer.append( ' ' ); 
            String name = (String)getValue( Action.NAME );
            macroBuffer.append( encodeActionName( name ) );
        }
    }
    
    private String getFilteredActionCommand(String cmd)
    {
        if (cmd == null || cmd.length() == 0)
            return "";
        char ch = cmd.charAt(0);
        if ((ch >= 0x20) && (ch != 0x7F))
            return cmd;
        else
            return "";
    }
    
    boolean startRecording( JTextComponent target ) {
        if( recording ) return false;
        recording = true;
        macroBuffer.setLength(0);
        textBuffer.setLength(0);
        Utilities.setStatusText( target, LocaleSupport.getString( "macro-recording" ) );
        return true;
    }
    
    String stopRecording( JTextComponent target ) {
        if( !recording ) return null;

        if( textBuffer.length() > 0 ) {
            if( macroBuffer.length() > 0 ) macroBuffer.append( ' ' ); 
            macroBuffer.append( encodeText( textBuffer.toString() ) );
        }        
        String retVal = macroBuffer.toString();
        recording = false;
        Utilities.setStatusText( target, "" ); // NOI18N
        return retVal;
    }
    
    private String encodeText( String s ) {
        char[] text = s.toCharArray();
        StringBuffer encoded = new StringBuffer( "\""); // NOI18N
        for( int i=0; i < text.length; i++ ) {
            char c = text[i];
            if( c == '"' || c == '\\' ) encoded.append( '\\' );
            encoded.append( c );
        }
        return encoded.append( '"' ).toString();
    }

    private String encodeActionName( String s ) {
        char[] actionName = s.toCharArray();
        StringBuffer encoded = new StringBuffer();
        for( int i=0; i < actionName.length; i++ ) {
            char c = actionName[i];
            if( Character.isWhitespace( c ) || c == '\\' ) encoded.append( '\\' );
            encoded.append( c );
        }
        return encoded.toString();
    }
    
    /** The target method that performs the real action functionality.
    * @param evt action event describing the action that occured
    * @param target target component where the action occured. It's retrieved
    *   by the TextAction.getTextComponent(evt).
    */
    public abstract void actionPerformed(ActionEvent evt, JTextComponent target);

    public JMenuItem getPopupMenuItem(JTextComponent target) {
        return null;
    }

    public String getPopupMenuText(JTextComponent target) {
        String txt = (String)getValue(POPUP_MENU_TEXT);
        if (txt == null) {
            txt = (String)getValue(NAME);
        }
        return txt;
    }

    /** Update the component according to the update mask specified
    * in the constructor of the action.
    * @param target target component to be updated.
    */
    public void updateComponent(JTextComponent target) {
        updateComponent(target, this.updateMask);
    }

    /** Update the component according to the given update mask
    * @param target target component to be updated.
    * @param updateMask mask that specifies what will be updated
    */
    public void updateComponent(JTextComponent target, int updateMask) {
        if (target != null && target.getDocument() instanceof BaseDocument) {
            BaseDocument doc = (BaseDocument)target.getDocument();
            boolean writeLocked = false;

            try {
                // remove selected text
                if ((updateMask & SELECTION_REMOVE) != 0) {
                    writeLocked = true;
                    doc.extWriteLock();
                    Caret caret = target.getCaret();
                    if (caret != null && caret.isSelectionVisible()) {
                        int dot = caret.getDot();
                        int markPos = caret.getMark();
                        if (dot < markPos) { // swap positions
                            int tmpPos = dot;
                            dot = markPos;
                            markPos = tmpPos;
                        }
                        try {
                            target.getDocument().remove(markPos, dot - markPos);
                        } catch (BadLocationException e) {
                            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                                e.printStackTrace();
                            }
                        }
                    }
                }

                // reset magic caret position
                if ((updateMask & MAGIC_POSITION_RESET) != 0) {
                    if (target.getCaret() != null)
                        target.getCaret().setMagicCaretPosition(null);
                }

                // reset abbreviation accounting
                if ((updateMask & ABBREV_RESET) != 0) {
                    ((BaseTextUI)target.getUI()).getEditorUI().getAbbrev().reset();
                }

                // reset merging of undoable edits
                if ((updateMask & UNDO_MERGE_RESET) != 0) {
                    doc.resetUndoMerge();
                }

                // reset word matching
                if ((updateMask & WORD_MATCH_RESET) != 0) {
                    ((BaseTextUI)target.getUI()).getEditorUI().getWordMatch().clear();
                }

                // Clear status bar text
                if ((updateMask & CLEAR_STATUS_TEXT) != 0) {
                    Utilities.clearStatusText(target);
                }

                // Save current caret position in the jump-list
                if ((updateMask & SAVE_POSITION) != 0) {
                    JumpList.checkAddEntry(target);
                }

            } finally {
                if (writeLocked) {
                    doc.extWriteUnlock();
                }
            }
        }
    }

}
