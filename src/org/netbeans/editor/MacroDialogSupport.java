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
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** The support for creating macros.
 *
 * @author  Petr Nejedly
 * @version 1.0
 */
public class MacroDialogSupport implements ActionListener {

    JButton okButton;
    JButton cancelButton;

    MacroSavePanel panel;
    Dialog macroDialog;
    Class kitClass;
    
    /** Creates new MacroDialogSupport */
    public MacroDialogSupport( Class kitClass ) {
        this.kitClass = kitClass;
        panel = new MacroSavePanel(kitClass);
        okButton = new JButton(LocaleSupport.getString("MDS_ok")); // NOI18N
        cancelButton = new JButton(LocaleSupport.getString("MDS_cancel")); // NOI18N
        okButton.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MDS_ok")); // NOI18N
        cancelButton.getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_MDS_cancel")); // NOI18N
    }

    public void setBody( String body ) {
        panel.setBody( body );
    }
    
    public void showMacroDialog() {
        macroDialog = DialogSupport.createDialog(
                LocaleSupport.getString("MDS_title"), // NOI18N
                panel, true, new JButton[] { okButton, cancelButton }, false, 0, 1, this );

        macroDialog.pack();
        panel.popupNotify();
        macroDialog.requestFocus();
        macroDialog.show();
    }
    
    private List getKBList(){
        Settings.KitAndValue[] kav = Settings.getValueHierarchy(kitClass, SettingsNames.KEY_BINDING_LIST);
        List kbList = null;
        for (int i = 0; i < kav.length; i++) {
            if (kav[i].kitClass == kitClass) {
                kbList = (List)kav[i].value;
            }
        }
        if (kbList == null) {
            kbList = new ArrayList();
        }
        
        // must convert all members to serializable MultiKeyBinding
        int cnt = kbList.size();
        for (int i = 0; i < cnt; i++) {
            Object o = kbList.get(i);
            if (!(o instanceof MultiKeyBinding) && o != null) {
                JTextComponent.KeyBinding b = (JTextComponent.KeyBinding)o;
                kbList.set(i, new MultiKeyBinding(b.key, b.actionName));
            }
        }
        return new ArrayList( kbList );
    }
    
    public void actionPerformed(java.awt.event.ActionEvent evt ) {
        Object source = evt.getSource();
        if( source == okButton ) {
            Map macroMap = (Map)Settings.getValue( kitClass, SettingsNames.MACRO_MAP);
            Map newMap = new HashMap( macroMap );
            newMap.put( panel.getName(), panel.getBody() );
            Settings.setValue( kitClass, SettingsNames.MACRO_MAP, newMap );
            
            List listBindings = panel.getKeySequences();

              // insert listBindings into keybindings
            if (listBindings.size() > 0)
            {
                List keybindings = getKBList();
                String actionName = new String(BaseKit.macroActionPrefix + panel.getName());
                for (int i = 0; i < listBindings.size(); i++)
                {
                    KeyStroke[] keyStrokes = (KeyStroke[])listBindings.get(i);
                    MultiKeyBinding multiKey = new MultiKeyBinding(keyStrokes, actionName);
                    keybindings.add(multiKey);
                }

                // set new KEY_BINDING_LIST
                Settings.setValue( kitClass, SettingsNames.KEY_BINDING_LIST, keybindings);
            }
        }
        macroDialog.setVisible( false );
        macroDialog.dispose();        
    }
    
}
