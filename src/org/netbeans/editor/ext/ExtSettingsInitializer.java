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
import org.netbeans.editor.Settings;
import org.netbeans.editor.SettingsNames;
import org.netbeans.editor.SettingsUtil;

import java.util.List;
import java.util.Map;

/**
* Initializer for the extended editor settings.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ExtSettingsInitializer extends Settings.AbstractInitializer {

    public static final String NAME = "ext-settings-initializer";

    public ExtSettingsInitializer() {
        super(NAME);
    }

    /** Update map filled with the settings.
    * @param kitClass kit class for which the settings are being updated.
    *   It is always non-null value.
    * @param settingsMap map holding [setting-name, setting-value] pairs.
    *   The map can be empty if this is the first initializer
    *   that updates it or if no previous initializers updated it.
    */
    public void updateSettingsMap(Class kitClass, Map settingsMap) {

        // ------------------------ BaseKit Settings --------------------------------------
        if (kitClass == BaseKit.class) {
            // Add key-bindings
            SettingsUtil.updateListSetting(settingsMap, SettingsNames.KEY_BINDING_LIST,
                                           ExtSettingsDefaults.defaultExtKeyBindings);
        }

        // ------------------------ ExtKit Settings --------------------------------------
        if (kitClass == ExtKit.class) {

            // List of the additional colorings
            SettingsUtil.updateListSetting(settingsMap, SettingsNames.COLORING_NAME_LIST,
                                           new String[] {
                                               ExtSettingsNames.HIGHLIGHT_CARET_ROW_COLORING,
                                               ExtSettingsNames.HIGHLIGHT_MATCH_BRACE_COLORING,
                                           }
                                          );

            // ExtCaret highlighting options
            settingsMap.put(ExtSettingsNames.HIGHLIGHT_CARET_ROW,
                            ExtSettingsDefaults.defaultHighlightCaretRow);
            settingsMap.put(ExtSettingsNames.HIGHLIGHT_MATCH_BRACE,
                            ExtSettingsDefaults.defaultHighlightMatchBrace);

            // ExtCaret highlighting colorings
            SettingsUtil.setColoring(settingsMap, ExtSettingsNames.HIGHLIGHT_CARET_ROW_COLORING,
                                     ExtSettingsDefaults.defaultHighlightCaretRowColoring);
            SettingsUtil.setColoring(settingsMap, ExtSettingsNames.HIGHLIGHT_MATCH_BRACE_COLORING,
                                     ExtSettingsDefaults.defaultHighlightMatchBraceColoring);

            // Popup menu default action names
            String[] popupMenuActionNames
                = new String[] {
                    BaseKit.cutAction,
                    BaseKit.copyAction,
                    BaseKit.pasteAction,
                    null,
                    BaseKit.removeSelectionAction
                };

            
            List pml = (List)settingsMap.get(ExtSettingsNames.POPUP_MENU_ACTION_NAME_LIST);
            if (pml == null || pml.indexOf(BaseKit.cutAction) == -1) {
                SettingsUtil.updateListSetting(settingsMap,
                    ExtSettingsNames.POPUP_MENU_ACTION_NAME_LIST, popupMenuActionNames);

                SettingsUtil.updateListSetting(settingsMap,
                    ExtSettingsNames.DIALOG_POPUP_MENU_ACTION_NAME_LIST, popupMenuActionNames);
            }
                                          
            settingsMap.put(ExtSettingsNames.POPUP_MENU_ENABLED, Boolean.TRUE);

            settingsMap.put(ExtSettingsNames.FAST_IMPORT_PACKAGE,
                            ExtSettingsDefaults.defaultFastImportPackage);
            
            // Completion settings
            settingsMap.put(ExtSettingsNames.COMPLETION_AUTO_POPUP,
                            ExtSettingsDefaults.defaultCompletionAutoPopup);

            settingsMap.put(ExtSettingsNames.COMPLETION_CASE_SENSITIVE,
                            ExtSettingsDefaults.defaultCompletionCaseSensitive);

            settingsMap.put(ExtSettingsNames.COMPLETION_NATURAL_SORT,
                            ExtSettingsDefaults.defaultCompletionNaturalSort);
            
            settingsMap.put(ExtSettingsNames.COMPLETION_INSTANT_SUBSTITUTION,
                            ExtSettingsDefaults.defaultCompletionInstantSubstitution);

            settingsMap.put(ExtSettingsNames.COMPLETION_AUTO_POPUP_DELAY,
                            ExtSettingsDefaults.defaultCompletionAutoPopupDelay);

            settingsMap.put(ExtSettingsNames.COMPLETION_REFRESH_DELAY,
                            ExtSettingsDefaults.defaultCompletionRefreshDelay);

            settingsMap.put(ExtSettingsNames.COMPLETION_PANE_MIN_SIZE,
                            ExtSettingsDefaults.defaultCompletionPaneMinSize);

            settingsMap.put(ExtSettingsNames.COMPLETION_PANE_MAX_SIZE,
                            ExtSettingsDefaults.defaultCompletionPaneMaxSize);

            // re-indentation settings
            settingsMap.put(ExtSettingsNames.REINDENT_WITH_TEXT_BEFORE,
                            Boolean.TRUE);
            
            settingsMap.put(ExtSettingsNames.JAVADOC_BG_COLOR,
                            ExtSettingsDefaults.defaultJavaDocBGColor);
            
            settingsMap.put(ExtSettingsNames.JAVADOC_AUTO_POPUP_DELAY,
                            ExtSettingsDefaults.defaultJavaDocAutoPopupDelay);
            
            settingsMap.put(ExtSettingsNames.JAVADOC_PREFERRED_SIZE,
                            ExtSettingsDefaults.defaultJavaDocPreferredSize);
            
            settingsMap.put(ExtSettingsNames.JAVADOC_AUTO_POPUP,
                            ExtSettingsDefaults.defaultJavaDocAutoPopup);
            
        }

    }

}
