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

import org.netbeans.editor.EditorState;
import org.netbeans.editor.LocaleSupport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GotoDialogPanel is an UI object for entering line numbers to move caret to.
 * It maintains its own history (stored in EditorState).
 * For proper history functionality, it is needed to call
 * <CODE>updateHistory()</CODE> for valid inserts.
 *
 * @author Miloslav Metelka, Petr Nejedly
 * @version 2.0
 */
public class GotoDialogPanel extends JPanel {

    static final long serialVersionUID =-8686958102543713464L;
    private static final String HISTORY_KEY = "GotoDialogPanel.history-goto-line";
    private static final int MAX_ITEMS = 20;

    /** The variable used during updating combo to prevent firing */
    private boolean dontFire = false;

    /** Initializes the UI and fetches the history */
    public GotoDialogPanel() {
        initComponents ();
        getAccessibleContext().setAccessibleName(LocaleSupport.getString("goto-title")); // NOI18N
        getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_goto")); // NOI18N
        gotoCombo.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_goto-line")); // NOI18N
        List history = (List)EditorState.get( HISTORY_KEY );
        if( history == null ) history = new ArrayList();
        updateCombo( history );
    }

    /** Set the content of the history combo
     * @param content The List of items to be shown in the combo
     */
    protected void updateCombo( List content ) {
        dontFire = true;
        gotoCombo.setModel( new DefaultComboBoxModel( content.toArray() ) );
        dontFire = false;
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        gotoLabel = new javax.swing.JLabel();
        gotoCombo = new javax.swing.JComboBox();
        
        setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gridBagConstraints1;
        
        gotoLabel.setText(LocaleSupport.getString("goto-line"));
        gotoLabel.setLabelFor(gotoCombo);
        gotoLabel.setDisplayedMnemonic(LocaleSupport.getChar( "goto-line-mnemonic", 'l' ));
        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.insets = new java.awt.Insets(12, 12, 0, 11);
        gridBagConstraints1.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints1.weighty = 1.0;
        add(gotoLabel, gridBagConstraints1);
        
        gotoCombo.setEditable(true);
        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = new java.awt.Insets(12, 0, 0, 10);
        gridBagConstraints1.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints1.weightx = 1.0;
        gridBagConstraints1.weighty = 1.0;
        add(gotoCombo, gridBagConstraints1);
        
    }//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JLabel gotoLabel;
    protected javax.swing.JComboBox gotoCombo;
    // End of variables declaration//GEN-END:variables


    /** @return the current text from the input field */
    public String getValue() {
        return (String)gotoCombo.getEditor().getItem();
    }
    
    /** This method is to be called when caller wishes to add the current 
     * content of the input filed to the history
     */
    public void updateHistory() {
        List history = (List)EditorState.get( HISTORY_KEY );
        if( history == null ) history = new ArrayList();

        Object value = getValue();

        if( history.contains( value ) ) {
            // move it to top
            history.remove( value );
            history.add( 0, value );
        } else {
            // assure it won't hold more than MAX_ITEMS
            if( history.size() >= MAX_ITEMS )
                history = history.subList(0, MAX_ITEMS-1);
            // add the last entered value to the top
            history.add( 0, getValue() );
        }
        EditorState.put( HISTORY_KEY, history );
        
        updateCombo( history );
    }

    /** the method called to ensure that the input field would be a focused
     * component with the content selected
     */
    public void popupNotify() {
        gotoCombo.getEditor().selectAll();
        gotoCombo.getEditor().getEditorComponent().requestFocus();
    }

    public javax.swing.JComboBox getGotoCombo()
    {
        return gotoCombo;
    }
    
}
