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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/** The component for displaying and editing just recorded macro.
 * It allows you to define a name for the macro and bound keystrokes to it.
 * 
 * @author Petr Nejedly
 * @version 1.0
 */
public class MacroSavePanel extends javax.swing.JPanel {
    
    private Vector bindings = new Vector();
    private Class kitClass;
    
    /** Creates new form SaveMacroPanel */
    public MacroSavePanel( Class kitClass ) {
        this.kitClass = kitClass;
        initComponents ();
        
        nameLabel.setDisplayedMnemonic(LocaleSupport.getString("MSP_Name_Mnemonic").charAt(0)); // NOI18N
        macroLabel.setDisplayedMnemonic(LocaleSupport.getString("MSP_Macro_Mnemonic").charAt(0)); // NOI18N
        bindingLabel.setDisplayedMnemonic(LocaleSupport.getString("MSP_Keys_Mnemonic").charAt(0)); // NOI18N
        nameField.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP_Name")); // NOI18N
        macroField.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP_Macro")); // NOI18N
        bindingList.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP_Keys")); // NOI18N
        getAccessibleContext().setAccessibleName(LocaleSupport.getString("MDS_Title")); // NOI18N
        getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP")); // NOI18N
        
        // temporary loss of function
        setMaximumSize( new Dimension( 400, 200 ) );
    }

    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        Dimension max = getMaximumSize();
        if( pref.width > max.width ) pref.width = max.width;
        if( pref.height > max.height ) pref.height = max.height;
	return pref;
    }
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        macroPanel = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        macroLabel = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        macroField = new javax.swing.JTextField();
        bindingPanel = new javax.swing.JPanel();
        bindingLabel = new javax.swing.JLabel();
        bindingScrollPane = new javax.swing.JScrollPane();
        bindingList = new javax.swing.JList();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(12, 12, 11, 11)));
        macroPanel.setLayout(new java.awt.GridBagLayout());

        nameLabel.setText(LocaleSupport.getString( "MSP_Name", "Name" ));
        nameLabel.setLabelFor(nameField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        macroPanel.add(nameLabel, gridBagConstraints);

        macroLabel.setText(LocaleSupport.getString( "MSP_Macro", "Macro" ));
        macroLabel.setLabelFor(macroField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 12);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        macroPanel.add(macroLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        macroPanel.add(nameField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        macroPanel.add(macroField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(macroPanel, gridBagConstraints);

        bindingPanel.setLayout(new java.awt.GridBagLayout());

        bindingLabel.setText(LocaleSupport.getString("MSP_Keys", "Keybindings:"));
        bindingLabel.setLabelFor(bindingList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bindingPanel.add(bindingLabel, gridBagConstraints);

        bindingList.setCellRenderer(new KeySequenceCellRenderer());
        bindingScrollPane.setViewportView(bindingList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bindingPanel.add(bindingScrollPane, gridBagConstraints);

        addButton.setToolTipText(LocaleSupport.getString( "MSP_AddToolTip", "Add a keybinding for this macro." ));
        addButton.setMnemonic(LocaleSupport.getString("MSP_Add_Mnemonic").charAt(0));
        addButton.setText(LocaleSupport.getString( "MSP_Add", "Add..."));
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBindingActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        bindingPanel.add(addButton, gridBagConstraints);

        removeButton.setToolTipText(LocaleSupport.getString( "MSP_RemoveToolTip", "Remove a keybinding from this macro." ));
        removeButton.setMnemonic(LocaleSupport.getString("MSP_Remove_Mnemonic").charAt(0));
        removeButton.setText(LocaleSupport.getString( "MSP_Remove", "Remove" ));
        removeButton.setEnabled(false);
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeBindingActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        bindingPanel.add(removeButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(bindingPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void removeBindingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBindingActionPerformed
        int index = bindingList.getSelectedIndex();
        if( index >= 0 ) {
            bindings.remove(index);
            bindingList.setListData(bindings);
        }
        if (bindingList.getModel().getSize() <= 0)
            removeButton.setEnabled(false);
        else
            bindingList.setSelectedIndex(0);
    }//GEN-LAST:event_removeBindingActionPerformed

    private void addBindingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBindingActionPerformed
        KeyStroke[] newKeyStrokes = new KeySequenceRequester().getKeySequence();
        
        if (newKeyStrokes != null)
        {
            bindings.add(newKeyStrokes);
            bindingList.setListData(bindings);
            bindingList.setSelectedIndex(0);
            removeButton.setEnabled(true);
        }
    }//GEN-LAST:event_addBindingActionPerformed
    
    public String getName() {
        return nameField.getText();
    }

    public void setName( String name ) {
        nameField.setText( name );
    }

    public String getBody() {
        return macroField.getText();
    }

    public void setBody( String body ) {
        macroField.setText( body );
    }

    /** @return List of KeyStroke[] */
    public List getKeySequences() {
        return new ArrayList( bindings );
    }

    /** @param sequences List of KeyStroke[] bounds to this macro */
    public void setKeySequences( List sequences ) {
        bindings = new Vector( sequences );
        bindingList.setListData( bindings );
    }    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameField;
    private javax.swing.JScrollPane bindingScrollPane;
    private javax.swing.JPanel macroPanel;
    private javax.swing.JButton addButton;
    private javax.swing.JList bindingList;
    private javax.swing.JPanel bindingPanel;
    private javax.swing.JLabel macroLabel;
    private javax.swing.JTextField macroField;
    private javax.swing.JLabel bindingLabel;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables

    
    public void popupNotify() {
        nameField.requestFocus();
    }

    
    private class KeySequenceCellRenderer extends JLabel implements ListCellRenderer {
        public KeySequenceCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setText( Utilities.keySequenceToString( (KeyStroke[])value ) );
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground() );
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground() );
            return this;
        }        
    }
    

    /**
     * Encapsulation for components of dialog asking for new KeySequence
     */
    class KeySequenceRequester {

        KeySequenceInputPanel panel;
        Dialog dial;

        JButton[] buttons = { new JButton(LocaleSupport.getString("MSP_ok")),  // NOI18N
                              new JButton(LocaleSupport.getString("MSP_clear")), // NOI18N
                              new JButton(LocaleSupport.getString("MSP_cancel"))}; // NOI18N

        KeyStroke[] retVal = null;


        KeySequenceRequester() {
            ((JButton)buttons[0]).getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP_ok")); // NOI18N
            ((JButton)buttons[1]).getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP_clear")); // NOI18N
            ((JButton)buttons[2]).getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MSP_cancel")); // NOI18N
            ((JButton)buttons[1]).setMnemonic(LocaleSupport.getString("MSP_clear_Mnemonic").charAt (0)); // NOI18N
            ((JButton)buttons[0]).setEnabled( false ); // default initial state

            // Prepare KeySequence input dialog
            panel = new KeySequenceInputPanel();
            panel.addPropertyChangeListener( new PropertyChangeListener() {
                                                 public void propertyChange( PropertyChangeEvent evt ) {
                                                     if( KeySequenceInputPanel.PROP_KEYSEQUENCE != evt.getPropertyName() ) return;
                                                     KeyStroke[] seq = panel.getKeySequence();
                                                     String warn = isAlreadyBounded( seq );
                                                     if (warn == null)
                                                        warn = getCollisionString( seq );
                                                     ((JButton)buttons[0]).setEnabled( seq.length > 0 && warn == null );
                                                     panel.setInfoText( warn == null ? "" : warn );  // NOI18N
                                                 }
                                             } );

            dial = DialogSupport.createDialog(
                LocaleSupport.getString("MSP_AddTitle"), // NOI18N
                panel, true, buttons, false, -1, 2, new ActionListener(){
                                            public void actionPerformed( ActionEvent evt ) {
                                                if( evt.getSource() == buttons[1] ) { // Clear pressed
                                                    panel.clear();          // Clear entered KeyStrokes, start again
                                                    panel.requestFocus();   // Make user imediately able to enter new strokes
                                                } else if( evt.getSource() == buttons[0] ) { // OK pressed
                                                    retVal = panel.getKeySequence();
                                                    dial.dispose();  // Done
                                                } else if( evt.getSource() == buttons[2] ) { // OK pressed
                                                    retVal = null;
                                                    dial.dispose();  // Done
                                                }
                                            }
                                        });

        }

        KeyStroke[] getKeySequence() {
            dial.pack();
            panel.requestFocus();
            dial.show();
            return retVal;
        }

        /** Check whether this KeyStroke is already bounded to this macro or not.
         * Disallow to duplicate the KeyStroke.
         */
        String isAlreadyBounded( KeyStroke[] seq ) {
            if( seq.length == 0 ) return null; // NOI18N   not valid sequence, but don't alert user

            Iterator it = bindings.iterator();
            while( it.hasNext() ) {
                if( isOverlapingSequence( (KeyStroke[])it.next(), seq ) ) {
                    return LocaleSupport.getString( "MSP_Collision" ); // NOI18N
                }
            }
            return null;  // no colliding sequence
        }

        String getCollisionString( KeyStroke[] seq ) {
            if( seq.length == 0 ) return null; // NOI18N   not valid sequence, but don't alert user

            Settings.KitAndValue[] kv = Settings.getValueHierarchy( kitClass, SettingsNames.KEY_BINDING_LIST );
            for (int i = 0; i < kv.length; i++)
            {
                Iterator iter = ((List)kv[i].value).iterator();
                while( iter.hasNext() ) {
                    MultiKeyBinding b = (MultiKeyBinding)iter.next();
                    KeyStroke[] ks = b.keys;
                    if (ks == null && b.key != null)
                    {
                        ks = new KeyStroke[1];
                        ks[0] = b.key;
                    }
                    if( ks !=  null && isOverlapingSequence( ks, seq ) ) {
                        Object[] values = { Utilities.keySequenceToString( ks ), b.actionName };
                        return MessageFormat.format( LocaleSupport.getString( "MSP_FMT_Collision" ), values ); // NOI18N
                    }
                }
            }
            return null;  // no colliding sequence
        }
        
        private boolean isOverlapingSequence( KeyStroke[] s1, KeyStroke[] s2 ) {
            int l = Math.min( s1.length, s2.length );
            if (l == 0)
                return false;
            while( l-- > 0 ) if( !s1[l].equals( s2[l] ) ) return false;
            return true;
        }
    }
    
}
