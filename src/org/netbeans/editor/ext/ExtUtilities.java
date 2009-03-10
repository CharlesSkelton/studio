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

import org.netbeans.editor.BaseTextUI;

import javax.swing.plaf.TextUI;
import javax.swing.text.JTextComponent;

/**
* Extended kit offering advanced functionality
*
* @author Miloslav Metelka
* @version 1.00
*/
public class ExtUtilities {
    

    
    private ExtUtilities() {
    }

    public static ExtEditorUI getExtEditorUI(JTextComponent target) {
        TextUI ui = target.getUI();
        return (ui instanceof BaseTextUI)
            ? (ExtEditorUI)((BaseTextUI)ui).getEditorUI()
            : null;
    }

    public static Completion getCompletion(JTextComponent target) {
        ExtEditorUI extEditorUI = getExtEditorUI(target);
        if (extEditorUI != null) {
            return extEditorUI.getCompletion();
        }
        return null;
    }

/*    public static CompletionJavaDoc getCompletionJavaDoc(JTextComponent target) {
        ExtEditorUI extEditorUI = getExtEditorUI(target);
        if (extEditorUI != null) {
            return extEditorUI.getCompletionJavaDoc();
        }
        return null;
    }
  */
    public static JDCPopupPanel getJDCPopupPanel(JTextComponent target) {
        ExtEditorUI extEditorUI = getExtEditorUI(target);
        if (extEditorUI != null) {
            Completion c = extEditorUI.getCompletion();
            if (c!=null) {
                return c.getJDCPopupPanel();
            }
        }
        return null;
    }
    

}
