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
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Vector;

/**
 * This class could be used as input of sequence of KeyStrokes.
 * {@link #getKeySequence}
 * One instance could be reused.
 * {@link #clear}
 * When actual keySequence changes, it fires PropertyChangeEvent
 * of property {@link #PROP_KEYSEQUENCE}.
 * There is additional label on the bottom, which could be set
 * with {@link #setInfoText} to pass some information to user.
 *
 * @author  David Konecny
 */

public class KeySequenceInputPanel extends javax.swing.JPanel {

    public static String PROP_KEYSEQUENCE = "keySequence"; // NOI18N
    private Vector strokes = new Vector();
    private StringBuffer text = new StringBuffer();

    /** Creates new form KeySequenceInputPanel with empty sequence*/
    public KeySequenceInputPanel() {
        initComponents ();
        
        keySequenceLabel.setDisplayedMnemonic(LocaleSupport.getString("LBL_KSIP_Sequence_Mnemonic").charAt(0)); // NOI18N
        keySequenceInputField.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_LBL_KSIP_Sequence")); // NOI18N
        getAccessibleContext().setAccessibleName(LocaleSupport.getString("MSP_AddTitle")); // NOI18N
        getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_KSIP")); // NOI18N
    }

    /**
     * Clears actual sequence of KeyStrokes
     */
    public void clear() {
        strokes.clear();
        text.setLength( 0 );
        keySequenceInputField.setText( text.toString() );
        firePropertyChange( PROP_KEYSEQUENCE, null, null );
    }

    /*
     * Sets the text of JLabel locaten on the bottom of this panel
     */
    public void setInfoText( String s ) {
        collisionLabel.setText( s + ' ' ); // NOI18N
    }

    /**
     * Returns sequence of completed KeyStrokes as KeyStroke[]
     */
    public KeyStroke[] getKeySequence() {
        return (KeyStroke[])strokes.toArray( new KeyStroke[0] );
    }

    /**
     * Makes it trying to be bigger
     */
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        
        if (dim.width < 400)
            dim.width = 400;
        
        return dim;
    }

    /**
     * We're redirecting our focus to proper component.
     */
    public void requestFocus() {
        keySequenceInputField.requestFocus();
    }

    /**
     * Visual part and event handling:
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        keySequenceLabel = new javax.swing.JLabel();
        keySequenceInputField = new javax.swing.JTextField();
        collisionLabel = new javax.swing.JTextArea();

        setLayout(new java.awt.GridBagLayout());

        setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(12, 12, 11, 11)));
        keySequenceLabel.setText(LocaleSupport.getString( "LBL_KSIP_Sequence" ));
        keySequenceLabel.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(0, 0, 0, 8)));
        keySequenceLabel.setLabelFor(keySequenceInputField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        add(keySequenceLabel, gridBagConstraints);

        keySequenceInputField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                keySequenceInputFieldKeyTyped(evt);
            }
            public void keyPressed(java.awt.event.KeyEvent evt) {
                keySequenceInputFieldKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keySequenceInputFieldKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(keySequenceInputField, gridBagConstraints);

        collisionLabel.setLineWrap(true);
        collisionLabel.setEditable(false);
        collisionLabel.setRows(2);
        collisionLabel.setForeground(java.awt.Color.red);
        collisionLabel.setBackground(getBackground());
        collisionLabel.setDisabledTextColor(java.awt.Color.red);
        collisionLabel.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(collisionLabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void keySequenceInputFieldKeyTyped (java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keySequenceInputFieldKeyTyped
        evt.consume();
    }//GEN-LAST:event_keySequenceInputFieldKeyTyped

    private void keySequenceInputFieldKeyReleased (java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keySequenceInputFieldKeyReleased
        evt.consume();
        keySequenceInputField.setText( text.toString() );
    }//GEN-LAST:event_keySequenceInputFieldKeyReleased

    private void keySequenceInputFieldKeyPressed (java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keySequenceInputFieldKeyPressed
        evt.consume();

        String modif = KeyEvent.getKeyModifiersText( evt.getModifiers() );
        if( isModifier( evt.getKeyCode() ) ) {
            keySequenceInputField.setText( text.toString() + modif + '+' ); //NOI18N
        } else {
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent( evt );
            strokes.add( stroke );
            text.append( Utilities.keyStrokeToString( stroke ) );
            text.append( ' ' );
            keySequenceInputField.setText( text.toString() );
            firePropertyChange( PROP_KEYSEQUENCE, null, null );
        }
    }//GEN-LAST:event_keySequenceInputFieldKeyPressed

    private boolean isModifier( int keyCode ) {
        return (keyCode == KeyEvent.VK_ALT) ||
               (keyCode == KeyEvent.VK_ALT_GRAPH) ||
               (keyCode == KeyEvent.VK_CONTROL) ||
               (keyCode == KeyEvent.VK_SHIFT) ||
               (keyCode == KeyEvent.VK_META);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel keySequenceLabel;
    private javax.swing.JTextArea collisionLabel;
    private javax.swing.JTextField keySequenceInputField;
    // End of variables declaration//GEN-END:variables
}
