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

import org.netbeans.editor.BaseKit;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.PopupManager;
import org.netbeans.editor.Utilities;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;


/**
* Editor UI for the component. All the additional UI features
* like advanced scrolling, info about fonts, abbreviations,
* keyword matching are based on this class.
*
* @author Miloslav Metelka
* @version 1.00
*/
public class ExtEditorUI extends EditorUI {

    private ToolTipSupport toolTipSupport;

    private JPopupMenu popupMenu;

    private Completion completion;
    
    private PopupManager popupManager;

//    private CompletionJavaDoc completionJavaDoc;
    
    private boolean noCompletion; // no completion available

    private boolean noCompletionJavaDoc; // no completion available
    

    
    public ExtEditorUI() {

        getToolTipSupport();
        getCompletion();
//        getCompletionJavaDoc();
    }

    public ToolTipSupport getToolTipSupport() {
        if (toolTipSupport == null) {
            toolTipSupport = new ToolTipSupport(this);
        }
        return toolTipSupport;
    }

    public Completion getCompletion() {
        
        if (completion == null) {
            if (noCompletion) {
                return null;
            }

            synchronized (getComponentLock()) {
                JTextComponent component = getComponent();
                if (component != null) {
                    BaseKit kit = Utilities.getKit(component);
                    if (kit != null && kit instanceof ExtKit) {
                        completion = ((ExtKit)kit).createCompletion(this);
                        if (completion == null) {
                            noCompletion = true;
                        }
                    }
                }
            }
        }

        return completion;
    }

    
/*    public CompletionJavaDoc getCompletionJavaDoc() {
        if (completionJavaDoc == null) {
            if (noCompletionJavaDoc) {
                return null;
            }

            synchronized (getComponentLock()) {
                JTextComponent component = getComponent();
                if (component != null) {
                    BaseKit kit = Utilities.getKit(component);
                    if (kit != null && kit instanceof ExtKit) {
                        completionJavaDoc = ((ExtKit)kit).createCompletionJavaDoc(this);
                        if (completionJavaDoc == null) {
                            noCompletionJavaDoc = true;
                        }
                    }
                }
            }
        }

        return completionJavaDoc;
    }
  */
    
    public PopupManager getPopupManager() {
        if (popupManager == null) {

            synchronized (getComponentLock()) {
                JTextComponent component = getComponent();
                if (component != null) {
                    popupManager = new PopupManager(component);
                }
            }
        }

        return popupManager;
    }
    

    
    public void showPopupMenu(int x, int y) {
        // First call the build-popup-menu action to possibly rebuild the popup menu
        JTextComponent component = getComponent();
        if (component != null) {
            BaseKit kit = Utilities.getKit(component);
            if (kit != null) {
                Action a = kit.getActionByName(ExtKit.buildPopupMenuAction);
                if (a != null) {
                    a.actionPerformed(new ActionEvent(component, 0, "")); // NOI18N
                }
            }

            JPopupMenu pm = getPopupMenu();
            if (pm != null) {
                if (component.isShowing()) { // fix of #18808
                    pm.show(component, x, y);
                }
            }
        }
    }

    public void hidePopupMenu() {
        JPopupMenu pm = getPopupMenu();
        if (pm != null) {
            pm.setVisible(false);
        }
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void setPopupMenu(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

}
