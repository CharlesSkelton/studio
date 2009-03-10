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
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * DialogSupport is factory based class for creating dialogs of certain
 * behaviour. It is intended to be used whenever editor needs to popup a dialog.
 * It presents a way for changing the implementation of the dialog depending
 * on the enviroment the Editor is embeded in.
 *
 * @author  pnejedly
 * @version 1.0
 */
public class DialogSupport {

    private static DialogFactory factory;
    
    /** Noone needs to instantiate the dialog support */
    private DialogSupport() {
    }

    /** 
     * The method for creating a dialog with specified properties.
     * @param title The title of created dialog.
     * @param panel The content of the dialog to be displayed.
     * @param modal Whether the dialog should be modal.
     * @param buttons The array of JButtons to be added to the dialog.
     * @param sidebuttons The buttons could be placed under the panel (false),
     *      or on the right side of the panel (true).
     * @param defaultIndex The index of default button in the buttons array,
     *    if <CODE>index < 0</CODE>, no default button is set.
     * @param cancelIndex The index about cancel button - the button that will
     *    be <I>pressed</I> when closing the dialog.
     * @param listener The listener which will be notified of all button
     *    events.
     * @return newly created <CODE>Dialog</CODE>
     */
    public static Dialog createDialog( String title, JPanel panel, boolean modal,
                JButton[] buttons, boolean sidebuttons, int defaultIndex, int cancelIndex,
                ActionListener listener
    ) {
        if( factory == null ) {
            factory = new DefaultDialogFactory();
        }
        return factory.createDialog(title, panel, modal, buttons, sidebuttons,
                defaultIndex, cancelIndex, listener );
    }
    
    /** The method for setting custom factory for creating dialogs via
     * the {@link #createDialog(java.lang.String, javax.swing.JPanel, boolean, javax.swing.JButton[], boolean, int, int, java.awt.event.ActionListener) createDialog} method.
     * If no factory is set, the {@link DialogSupport.DefaultDialogFactory DefaultDialogFactory}
     * will be used.
     * @param factory the {@link DialogSupport.DialogFactory DialogFactory}
     * implementation that will be responsible for providing dialogs.
     *
     * @see DialogSupport.DialogFactory
     * @see DialogSupport.DefaultDialogFactory
     */
    public static void setDialogFactory( DialogFactory factory ) {
        DialogSupport.factory = factory;
    }
    
    
    /**
     * DialogFactory implementation is a class responsible for providing
     * proper implementation of Dialog containing required widgets.
     * It can provide the dialog itself or delegate the functionality
     * to another piece of code, e.g some windowing system. 
     */
    public static interface DialogFactory {
        
        /** 
         * The method for creating a dialog with specified properties.
         * @param title The title of created dialog.
         * @param panel The content of the dialog to be displayed.
         * @param modal Whether the dialog should be modal.
         * @param buttons The array of JButtons to be added to the dialog.
         * @param sidebuttons The buttons could be placed under the panel (false),
         *      or on the right side of the panel (true).
         * @param defaultIndex The index of default button in the buttons array,
         *    if <CODE>index < 0</CODE>, no default button is set.
         * @param cancelIndex The index of cancel button - the button that will
         *    be <I>pressed</I> when closing the dialog.
         * @param listener The listener which will be notified of all button
         *    events.
         * @return newly created <CODE>Dialog</CODE>
         */
        public Dialog createDialog( String title, JPanel panel, boolean modal,
                JButton[] buttons, boolean sidebuttons, int defaultIndex,
                int cancelIndex, ActionListener listener );
    }
    
    
    /** The DialogFactory that will be used to create Dialogs if no other
     * DialogFactory is set to DialogSupport.
     */
    private static class DefaultDialogFactory extends WindowAdapter implements DialogFactory, ActionListener {
        
        private JButton cancelButton;
        
        /** Create a panel with buttons that will be placed according
         * to the required alignment */
        JPanel createButtonPanel( JButton[] buttons, boolean sidebuttons ) {
            int count = buttons.length;
            
            JPanel outerPanel = new JPanel( new BorderLayout() );
            outerPanel.setBorder( new EmptyBorder( new Insets(
                    sidebuttons ? 5 : 0, sidebuttons ? 0 : 5, 5, 5 ) ) );

            LayoutManager lm = new GridLayout( // GridLayout makes equal cells
                    sidebuttons ? count : 1, sidebuttons ? 1 : count,  5, 5 );
                
            JPanel innerPanel = new JPanel( lm );
            
            for( int i = 0; i < count; i++ ) innerPanel.add( buttons[i] );
            
            outerPanel.add( innerPanel,
                sidebuttons ? BorderLayout.NORTH : BorderLayout.EAST ) ;
            return outerPanel;
        }
        
        public Dialog createDialog( String title, JPanel panel, boolean modal,
                JButton[] buttons, boolean sidebuttons, int defaultIndex,
                int cancelIndex, ActionListener listener ) {

            // create the dialog with given content
            JDialog d = new JDialog( (javax.swing.JFrame)null, title, modal );
            d.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
            d.getContentPane().add( panel, BorderLayout.CENTER);
            
            // Add the buttons to it
            JPanel buttonPanel = createButtonPanel( buttons, sidebuttons );
            String buttonAlign = sidebuttons ? BorderLayout.EAST : BorderLayout.SOUTH;
            d.getContentPane().add( buttonPanel, buttonAlign );

            // add listener to buttons
            if( listener != null ) {
                for( int i = 0; i < buttons.length; i++ ) {
                    buttons[i].addActionListener( listener );
                }
            }

            // register the default button, if available
            if( defaultIndex >= 0 ) {
                d.getRootPane().setDefaultButton( buttons[defaultIndex] );
            }
            
            // register the cancel button helpers, if available
            if( cancelIndex >= 0 ) {
                cancelButton = buttons[cancelIndex];
                // redirect the Esc key to Cancel button
                d.getRootPane().registerKeyboardAction(
                    this,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                // listen on windowClosing and redirect it to Cancel button
                d.addWindowListener( this );
            }

            d.pack();
            return d;
        }
        
        public void actionPerformed(ActionEvent evt) {
            cancelButton.doClick( 10 );
        }

        public void windowClosing( WindowEvent evt ) {
            cancelButton.doClick( 10 );
        }
    }
    
}
