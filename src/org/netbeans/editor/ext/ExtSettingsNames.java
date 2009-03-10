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

package org.netbeans.editor.ext;

import org.netbeans.editor.SettingsNames;

/**
* Names of the extended editor settings.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ExtSettingsNames extends SettingsNames {

    /** List of the action names that should be shown in the popup menu.
    * Null name means separator.
    * Values: java.util.List containing java.lang.String instances
    */
    public static final String POPUP_MENU_ACTION_NAME_LIST = "popup-menu-action-name-list"; // NOI18N

    /** List of the action names that should be shown in the popup menu
     * when JEditorPane is shown in the dialogs. It corresponds
     * Null name means separator.
     * Values: java.util.List containing java.lang.String instances
     */
    public static final String DIALOG_POPUP_MENU_ACTION_NAME_LIST
    = "dialog-popup-menu-action-name-list"; // NOI18N

    /** Whether popup menu will be displayed on mouse right-click or not.
     * It's set to true by default.
     * Values: java.lang.Boolean
     */
    public static final String POPUP_MENU_ENABLED = "popup-menu-enabled"; // NOI18N

    /** Highlight the row where the caret currently is. The ExtCaret must be used.
    * Values: java.lang.Boolean 
    */
    public static final String HIGHLIGHT_CARET_ROW = "highlight-caret-row"; // NOI18N

    /** Highlight the matching brace (if the caret currently stands after the brace).
    * The ExtCaret must be used.
    * Values: java.lang.Boolean 
    */
    public static final String HIGHLIGHT_MATCH_BRACE = "highlight-match-brace"; // NOI18N

    /** Coloring used to highlight the row where the caret resides */
    public static final String HIGHLIGHT_CARET_ROW_COLORING = "highlight-caret-row"; // NOI18N

    /** Coloring used to highlight the matching brace */
    public static final String HIGHLIGHT_MATCH_BRACE_COLORING = "highlight-match-brace"; // NOI18N

    /** Delay (milliseconds) after which the matching brace
    * will be updated. This is intended to eliminate flicker
    * if the user holds the arrow key pressed.
    */
    public static final String HIGHLIGHT_MATCH_BRACE_DELAY = "highlight-match-brace-delay"; // NOI18N

    /** Whether the fast and simple matching should be used for higlighting
    * the matching brace. Its disadvantage is that it doesn't ignore the comments
    * and string and character constants in the search.
    */
    public static final String CARET_SIMPLE_MATCH_BRACE = "caret-simple-match-brace"; // NOI18N

    /** Whether the code completion window should popup automatically.
    * Values: java.lang.Boolean
    */
    public static final String COMPLETION_AUTO_POPUP = "completion-auto-popup"; // NOI18N

    /** Whether the code completion query search will be case  sensitive
    * Values: java.lang.Boolean
    */
    public static final String COMPLETION_CASE_SENSITIVE = "completion-case-sensitive"; // NOI18N

    /** Whether the code completion sorting will be natural
    * Values: java.lang.Boolean
    */
    public static final String COMPLETION_NATURAL_SORT = "completion-natural-sort"; // NOI18N
    
    /** Whether perform instant substitution, if the search result contains only one item
    * Values: java.lang.Boolean
    */
    public static final String COMPLETION_INSTANT_SUBSTITUTION = "completion-instant-substitution"; // NOI18N

    /** The delay after which the completion window is shown automatically.
    * Values: java.lang.Integer
    */
    public static final String COMPLETION_AUTO_POPUP_DELAY = "completion-auto-popup-delay"; // NOI18N

    /** The delay after which the completion window is refreshed.
    * Values: java.lang.Integer
    */
    public static final String COMPLETION_REFRESH_DELAY = "completion-refresh-delay"; // NOI18N

    /** The minimum size of the completion pane component.
    * Values: java.awt.Dimension
    */
    public static final String COMPLETION_PANE_MIN_SIZE = "completion-pane-min-size"; // NOI18N

    /** The maximum size of the completion pane component.
    * Values: java.awt.Dimension
    */
    public static final String COMPLETION_PANE_MAX_SIZE = "completion-pane-max-size"; // NOI18N

    /** Acceptor sensitive to characters that cause that
     * that the current line will be reformatted immediately.
     */
    public static final String INDENT_HOT_CHARS_ACCEPTOR = "indent-hot-chars-acceptor"; // NOI18N

    /** Whether lines should be indented on an indent hot key if there is non whitespace before
     * the typed hot key. See editor issue #10771.
     * Values: java.lang.Boolean
     */
    public static final String REINDENT_WITH_TEXT_BEFORE = "reindent-with-text-before"; // NOI18N

    /** Whether the fast import should offer packages instead of classes
     * Values: java.lang.Integer
     */
    public static final String FAST_IMPORT_SELECTION = "fast-import-selection"; // NOI18N
    
    /** Whether the fast import should offer packages instead of classes
     * Values: java.lang.Boolean
     * @deprecated replaced by FAST_IMPORT_SELECTION
     */
    public static final String FAST_IMPORT_PACKAGE = "fast-import-package"; // NOI18N
    
    
    /** Display info before Go To Class feature 
     *  Values: java.lang.Boolean
     */
    public static final String DISPLAY_GO_TO_CLASS_INFO = "display-go-to-class-info"; // NOI18N    
    
    /** Background color of javaDoc popup window 
     *  Values: java.awt.Color
     */
    public static final String JAVADOC_BG_COLOR = "javadoc-bg-color"; //NOI18N
    
    /** The delay after which the javaDoc window is shown automatically.
    *   Values: java.lang.Integer
    */
    public static final String JAVADOC_AUTO_POPUP_DELAY = "javadoc-auto-popup-delay"; //NOI18N
    
    /** The preferred size of javaDoc popup window
    *   Values: java.awt.Dimension
    */ 
    public static final String JAVADOC_PREFERRED_SIZE = "javadoc-preferred-size"; //NOI18N

    /** Whether the javaDoc window should popup automatically.
    * Values: java.lang.Boolean
    */
    public static final String JAVADOC_AUTO_POPUP = "javadoc-auto-popup"; // NOI18N
    
    /** Whether show deprecated members in code completion popup window
    * Values: java.lang.Boolean
    */
    public static final String SHOW_DEPRECATED_MEMBERS = "show-deprecated-members"; // NOI18N
    
    /** Whether update Code Completion DB after mounting new FS
    * Values: java.lang.String
    */
    public static final String UPDATE_PD_AFTER_MOUNTING = "update_pd_after_mounting"; // NOI18N
    
    /** Two levels of performing auto update of Code Completion DB 
     *  ALWAYS - PD is updated automatically after mounting a new filesystem
     *  NEVER - PD is never updated after mounting a new filesystem
     */
    public static final String ALWAYS = "pd_always";//NOI18N
    public static final String NEVER = "pd_never";//NOI18N
    
}
