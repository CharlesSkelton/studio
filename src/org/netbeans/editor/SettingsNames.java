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

/**
* Names of the base settings defined in the editor. The other packages
* related to the editor can define their own extensions to these basic
* names. The extension class can be inherited from this class
* so that the extension class contains all the available names.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class SettingsNames {

    /** Number of spaces to draw when the '\t' character
    * is found in the text. Better said when the drawing-engine
    * finds a '\t' character it computes the next multiple
    * of TAB_SIZE and continues drawing from that position.
    * Values: java.lang.Integer instances
    */
    public static final String TAB_SIZE = "tab-size"; // NOI18N

    /** Whether expand typed tabs to spaces. The number of spaces to substitute
    * per one typed tab is determined by SPACES_PER_TAB setting.
    * Values: java.lang.Boolean instances
    */
    public static final String EXPAND_TABS = "expand-tabs"; // NOI18N

    /** How many spaces substitute per one typed tab. This parameter has
    * effect only when EXPAND_TABS setting is set to true.
    * This parameter has no influence on how
    * the existing tabs are displayed.
    * Values: java.lang.Integer instances
    */
    public static final String SPACES_PER_TAB = "spaces-per-tab"; // NOI18N

    /** Shift-width says how many spaces should the formatter use
    * to indent the more inner level of code. This setting is independent of <tt>TAB_SIZE</tt>
    * and <tt>SPACES_PER_TAB</tt>.
    * Values: java.lang.Integer instances
    */
    public static final String INDENT_SHIFT_WIDTH = "indent-shift-width"; // NOI18N

    /** Acceptor that recognizes the identifier characters.
    * If set it's used instead of the default Syntax.isIdentifierPart() call.
    * Values: org.netbeans.editor.Acceptor instances
    */
    public static final String IDENTIFIER_ACCEPTOR = "identifier-acceptor"; // NOI18N

    /** Acceptor that recognizes the whitespace characters.
    * If set it's used instead of the default Syntax.isWhitespace() call.
    * Values: org.netbeans.editor.Acceptor instances
    */
    public static final String WHITESPACE_ACCEPTOR = "whitespace-acceptor"; // NOI18N

    /** Map of the string abbreviations. The second string (value) means
    * the full version of the first string (key).
    * Values: java.util.Map instances holding
    *      [abbrev-string, expanded-abbrev-string] pairs
    */
    public static final String ABBREV_MAP = "abbrev-map"; // NOI18N

    /** Map of macro definitions. The second string (value) contains
     * encoded functionality of the macro in the following notation:
     * Macro ::= S? Element (S Element)* S?
     * Element ::= Action | '"' Text '"'
     * Action ::= ( ( ( char - ( S | '\' ) ) | '\\' | ( '\' S ) )+
     * Text ::= ( ( char - ( '"' | '\' ) ) | ( '\"' | '\\' ) )*
    */
    public static final String MACRO_MAP = "macro-map"; // NOI18N

    /** Map of the action abbreviations. The second string (value) is
    * the name of the action to execute. The action must be available
    * in the kit actions. It can be added through <tt>CUSTOM_ACTION_LIST</tt>.
    * The original abbreviation string
    * is first removed from the text before the action is executed.
    * If there is the same abbreviation in the <tt>ABBREV_MAP</tt> map
    * it has a precedence over the <tt>ABBREV_ACTION_MAP</tt>.
    * Values: java.util.Map instances holding
    *   [abbrev-string, name-of-the-action-to-execute] pairs
    */
    public static final String ABBREV_ACTION_MAP = "abbrev-action-map"; // NOI18N

    /** Acceptor checking whether abbreviation should be attempted
    * after the appropriate character was typed.
    * Values: org.netbeans.editor.Acceptor instances
    */
    public static final String ABBREV_EXPAND_ACCEPTOR = "abbrev-expand-acceptor"; // NOI18N

    /** Acceptor checking whether typed character that performed
    * abbreviation expansion should be added to the text or not.
    * Values: org.netbeans.editor.Acceptor instances
    */
    public static final String ABBREV_ADD_TYPED_CHAR_ACCEPTOR
    = "abbrev-add-typed-char-acceptor"; // NOI18N

    /** Acceptor checking whether typed character should reset
    * abbreviation accounting. By default all non-letterOrDigit chars
    * reset the abbreviation accounting.
    * Values: org.netbeans.editor.Acceptor instances
    */
    public static final String ABBREV_RESET_ACCEPTOR = "abbrev-reset-acceptor"; // NOI18N

    /** Type of caret for insert mode.
    * Values: java.lang.String instances
    *   Currently supported types are:
    *     org.netbeans.editor.BaseCaret.LINE_CARET - default 2point caret
    *     org.netbeans.editor.BaseCaret.THIN_LINE_CARET - swing like thin caret
    *     org.netbeans.editor.BaseCaret.BLOCK_CARET - block covering whole character
    */
    public static final String CARET_TYPE_INSERT_MODE = "caret-type-insert-mode"; // NOI18N

    /** Type of caret for over write mode.
    * Values: java.lang.String instances
    *   Currently supported types are:
    *     org.netbeans.editor.BaseCaret.LINE_CARET - default 2point caret
    *     org.netbeans.editor.BaseCaret.THIN_LINE_CARET - swing like thin caret
    *     org.netbeans.editor.BaseCaret.BLOCK_CARET - block covering whole character
    */
    public static final String CARET_TYPE_OVERWRITE_MODE = "caret-type-overwrite-mode"; // NOI18N

    /** Will the insert mode caret be italicized if the underlying font
    * is italic?
    * Values: java.lang.Boolean instances
    */
    public static final String CARET_ITALIC_INSERT_MODE = "caret-italic-insert-mode"; // NOI18N

    /** Will the overwrite mode caret be italicized if the underlying font
    * is italic?
    * Values: java.lang.Boolean instances
    */
    public static final String CARET_ITALIC_OVERWRITE_MODE = "caret-italic-overwrite-mode"; // NOI18N

    /** Caret color for insert mode.
    * Values: java.awt.Color instances
    */
    public static final String CARET_COLOR_INSERT_MODE = "caret-color-insert-mode"; // NOI18N

    /** Caret color for overwrite mode.
    * Values: java.awt.Color instances
    */
    public static final String CARET_COLOR_OVERWRITE_MODE = "caret-color-overwrite-mode"; // NOI18N

    /** Caret blink rate in milliseconds.
    * Values: java.lang.Integer
    */
    public static final String CARET_BLINK_RATE = "caret-blink-rate"; // NOI18N

    /** Whether to display line numbers on the left part of the screen.
    * Values: java.lang.Boolean instances
    */
    public static final String LINE_NUMBER_VISIBLE = "line-number-visible"; // NOI18N

    /** Whether to display line numbers when printing to the printer.
    * Values: java.lang.Boolean instances
    */
    public static final String PRINT_LINE_NUMBER_VISIBLE = "print-line-number-visible"; // NOI18N

    /** How much should the view jump when scrolling goes off the screen.
    * Insets are used so that it can be specified for each direction specifically.
    * Each inset value can be positive or negative. The positive value means
    * the number of lines for the top and the bottom and the number of characters
    * for the left and the right. The negative value means percentage of the editor
    * component height for the top and the bottom and percentage of the editor
    * component width for the left and the right.
    * Values: java.awt.Insets instances
    */
    public static final String SCROLL_JUMP_INSETS = "scroll-jump-insets"; // NOI18N

    /** How much space must be reserved in each direction for the find operation.
    * It's here to ensure the found information will be visible in some
    * context around it.
    * Insets are used so that it can be specified for each direction specifically.
    * Each inset value can be positive or negative. The positive value means
    * the number of lines for the top and the bottom and the number of characters
    * for the left and the right. The negative value means percentage of the editor
    * component height for the top and the bottom and percentage of the editor
    * component width for the left and the right.
    * Values: java.awt.Insets instances
    */
    public static final String SCROLL_FIND_INSETS = "scroll-find-insets"; // NOI18N

    /** How much space will be added additionaly when the component needs to be
    * resized.
    * Each dimension value can be positive or negative. The positive value means
    * the number of lines for the height and the number of characters
    * for the width. The negative value means percentage of the editor
    * component height for the height and percentage of the editor
    * component width for the width.
    * Values: java.awt.Dimension instances
    */
    public static final String COMPONENT_SIZE_INCREMENT = "component-size-increment"; // NOI18N

    /** Margin for the editor component
    * Values: java.awt.Insets instances
    */
    public static final String MARGIN = "margin"; // NOI18N

    /** Margin on the left and right side of the line number.
    * It's used only when line numbers are visible. The top and bottom values
    * are ignored.
    * Values: java.awt.Insets instances
    */
    public static final String LINE_NUMBER_MARGIN = "line-number-margin"; // NOI18N

    /** Width of the margin on the left side of the text just after the line-number bar.
    * Values: java.awt.Integer instances
    */
    public static final String TEXT_LEFT_MARGIN_WIDTH = "text-left-margin-width"; // NOI18N

    /** Rendering hints to be used for the painting.
    * Values: java.util.Map instances
    */
    public static final String RENDERING_HINTS = "rendering-hints"; // NOI18N

    /** Key binding list for particular kit.
    * Values: java.util.List instances holding
    *   javax.swing.JTextComponent.KeyBinding instances
    *   or org.netbeans.editor.MultiKeyBinding instances
    */
    public static final String KEY_BINDING_LIST = "key-bindings"; // NOI18N

    /** Whether the input-methods should be enabled.
    * Values: java.lang.Boolean
    */
    public static final String INPUT_METHODS_ENABLED = "input-methods-enabled"; // NOI18N

    /** Float constant by which the height of the character obtained from
    * the font is multiplied. It defaults to 1.
    * Values: java.lang.Float instances
    */
    public static final String LINE_HEIGHT_CORRECTION = "line-height-correction"; // NOI18N

    /* Find properties.
    * They are read by FindSupport when its instance is being initialized.
    * FIND_WHAT: java.lang.String - search expression
    * FIND_REPLACE_BY: java.lang.String - replace string
    * FIND_HIGHLIGHT_SEARCH: java.lang.Boolean - highlight matching strings in text
    * FIND_INC_SEARCH: java.lang.Boolean - show matching strings immediately
    * FIND_BACKWARD_SEARCH: java.lang.Boolean - search in backward direction
    * FIND_WRAP_SEARCH: java.lang.Boolean - if end of doc reached, start from begin
    * FIND_MATCH_CASE: java.lang.Boolean - match case of letters
    * FIND_SMART_CASE: java.lang.Boolean - case insensitive search if FIND_MATCH_CASE
    *   is false and all letters of FIND_WHAT are small, case sensitive otherwise
    * FIND_WHOLE_WORDS: java.lang.Boolean - match only whole words
    * FIND_REG_EXP: java.lang.Boolean - use regular expressions in search expr
    * FIND_HISTORY: java.util.List - History of search expressions
    * FIND_HISTORY_SIZE: java.lang.Integer - Maximum size of the history
    */
    public static final String FIND_WHAT = "find-what"; // NOI18N
    public static final String FIND_REPLACE_WITH = "find-replace-with"; // NOI18N
    public static final String FIND_HIGHLIGHT_SEARCH = "find-highlight-search"; // NOI18N
    public static final String FIND_INC_SEARCH = "find-inc-search"; // NOI18N
    public static final String FIND_INC_SEARCH_DELAY = "find-inc-search-delay"; // NOI18N
    public static final String FIND_BACKWARD_SEARCH = "find-backward-search"; // NOI18N
    public static final String FIND_WRAP_SEARCH = "find-wrap-search"; // NOI18N
    public static final String FIND_MATCH_CASE = "find-match-case"; // NOI18N
    public static final String FIND_SMART_CASE = "find-smart-case"; // NOI18N
    public static final String FIND_WHOLE_WORDS = "find-whole-words"; // NOI18N
    public static final String FIND_REG_EXP = "find-reg-exp"; // NOI18N
    public static final String FIND_HISTORY = "find-history"; // NOI18N
    public static final String FIND_HISTORY_SIZE = "find-history-size"; // NOI18N


    /** Number of characters that can be searched. If the value is larger
    * than the document size, the document is used but the next document
    * will not be used. The zero value disables the word match completely.
    * Specify Integer.MAX_VALUE to search all the documents regardless
    * of the size.
    * Values: java.lang.Integer instances
    */
    public static final String WORD_MATCH_SEARCH_LEN = "word-match-search-len"; // NOI18N

    /** Wrap the word match searching
    * on current document after it reaches the end/begining of
    * current document. All the other documents except the current (first) one
    * are searched from begining in forward direction.
    * Values: java.lang.Boolean instances
    */
    public static final String WORD_MATCH_WRAP_SEARCH = "word-match-wrap-search"; // NOI18N

    /** Word list that is searched as last resort in word matching.
    * It can contain the words that are used often by the user.
    * If this property is set, these words are searched regardless
    * of WORD_MATCH_SEARCH_LEN setting.
    * Values: java.lang.String instances
    */
    public static final String WORD_MATCH_STATIC_WORDS = "word-match-static-words"; // NOI18N

    /** Whether to use case sensitive search or not.
    * Values: java.lang.Boolean instances
    */
    public static final String WORD_MATCH_MATCH_CASE = "word-match-match-case"; // NOI18N

    /** Whether to use case insensitive search if all the letters are small
    * and case sensitive search if at least one letter is capital.
    * Values: java.lang.Boolean instances
    */
    public static final String WORD_MATCH_SMART_CASE = "word-match-smart-case"; // NOI18N

    /** Whether the word matching should return the match even if the matching
    * word has only one char. The WORD_MATCH_MATCH_CASE setting is ignored
    * in case this setting is on.
    * Values: java.lang.Boolean instances
    */
    public static final String WORD_MATCH_MATCH_ONE_CHAR = "word-match-match-one-char"; // NOI18N

    /** List of actions that will be added to the standard list of actions
    * for the particular kit. Using this mechanism, user can add actions
    * and possibly map them to the keys without overriding kit classes.
    * NOTICE!: This option has INCREMENTAL HANDLING, i.e. current kit list but also 
    * all the super kit lists are used. For example if there is a list of actions
    * both for JavaKit and BaseKit classes, both list of actions will be added
    * and BaseKit actions will be added first.
    * Values: java.util.List instances
    */
    public static final String CUSTOM_ACTION_LIST = "custom-action-list"; // NOI18N

    /** List of actions which is executed when
    * editor kit is installed into component. Actions are executed one by one
    * in the order they occur in the list.
    * At the time the kit is installed, the document is not yet assigned.
    * To perform some actions on document, use the DOC_INSTALL_ACTION_LIST.
    * NOTICE!: This option has INCREMENTAL HANDLING, i.e. current kit list but also 
    * all the super kit lists are used. For example if there is a list of actions
    * both for JavaKit and BaseKit classes, both list of actions will be executed
    * and JavaKit actions will be executed first.
    * Values: java.util.List instances
    */
    public static final String KIT_INSTALL_ACTION_NAME_LIST = "kit-install-action-name-list"; // NOI18N

    /** List of actions that are executed when
    * editor kit is being removed from component. Actions are executed one by one
    * in the order they occur in the list.
    * NOTICE!: This option has INCREMENTAL HANDLING, i.e. current kit list but also 
    * all the super kit lists are used. For example if there is a list of actions
    * both for JavaKit and BaseKit classes, both list of actions will be executed
    * and JavaKit actions will be executed first.
    * Values: java.util.List instances
    */
    public static final String KIT_DEINSTALL_ACTION_NAME_LIST = "kit-deinstall-action-name-list"; // NOI18N

    /** List of actions which is executed when
    * the new document is installed into component. Actions are executed one by one
    * in the order they occur in the list.
    * NOTICE!: This option has INCREMENTAL HANDLING, i.e. current kit list but also 
    * all the super kit lists are used. For example if there is a list of actions
    * both for JavaKit and BaseKit classes, both list of actions will be executed
    * and JavaKit actions will be executed first.
    * Values: java.util.List instances
    */
    public static final String DOC_INSTALL_ACTION_NAME_LIST = "doc-install-action-name-list"; // NOI18N

    /** Whether status bar should be visible or not.
    * Values: java.lang.Boolean instances
    */
    public static final String STATUS_BAR_VISIBLE = "status-bar-visible"; // NOI18N

    /** Delay for updating information about caret in the status bar.
    * Values: java.lang.Integer instances
    */
    public static final String STATUS_BAR_CARET_DELAY = "status-bar-caret-delay"; // NOI18N

    /** Whether the line displaying the text limit should be displayed.
    * Values: java.lang.Boolean instances
    */
    public static final String TEXT_LIMIT_LINE_VISIBLE = "text-limit-line-visible"; // NOI18N

    /** Which color should be used for the line showing the text limit.
    * Values: java.awt.Color instances
    */
    public static final String TEXT_LIMIT_LINE_COLOR = "text-limit-line-color"; // NOI18N

    /** After how many characters the text limit line should be displayed.
    * Values: java.awt.Integer instances
    */
    public static final String TEXT_LIMIT_WIDTH = "text-limit-width"; // NOI18N

    /** Whether the home key should go to column 1 or first go to text start
    * on the given line and then to the column 1.
    * Values: java.lang.Boolean
    * @deprecated As of 05/09/2001
    */
    public static final String HOME_KEY_COLUMN_ONE = "home-key-column-one"; // NOI18N

    /** Finder for finding the next word. If it's not set,
    * the <tt>FinderFactory.NextWordFwdFinder</tt> is used.
    * Values: org.netbeans.editor.Finder
    */
    public static final String NEXT_WORD_FINDER = "next-word-finder"; // NOI18N

    /** Finder for finding the previous word. If it's not set,
    * the <tt>FinderFactory.WordStartBwdFinder</tt> is used.
    * Values: org.netbeans.editor.Finder
    */
    public static final String PREVIOUS_WORD_FINDER = "previous-word-finder"; // NOI18N

    /** Whether the word move should stop on the '\n' character. This setting
    * affects both the 
    * Values: java.lang.Boolean
    */
    public static final String WORD_MOVE_NEWLINE_STOP = "word-move-newline-stop"; // NOI18N

    /** Whether to trim the white space characters (except '\n') from
    * the end of the line.
    * Values: java.lang.Boolean instances
    */
    //  public static final String TRIM_SPACES = "trim-spaces"; // NOI18N

    /** Buffer size for reading into the document from input stream or reader.
    * Values: java.lang.Integer
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String READ_BUFFER_SIZE = "read-buffer-size"; // NOI18N

    /** Buffer size for writing from the document to output stream or writer.
    * Values: java.lang.Integer instances
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String WRITE_BUFFER_SIZE = "write-buffer-size"; // NOI18N

    /** Read mark distance is used when performing initial read
    * of the document. It denotes the distance in chars of two adjacent
    * syntax marks inserted into the document.
    * Values: java.lang.Integer instances
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String READ_MARK_DISTANCE = "read-mark-distance"; // NOI18N

    /** Implicit mark distance for inserting to the document.
    * If the insert is made then the distance between nearest syntax
    * marks around insertion point is checked and if it's greater
    * than the max mark distance then another mark(s) are inserted
    * automatically with the distance given by this setting.
    * Values: java.lang.Integer instances instances
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String MARK_DISTANCE = "mark-distance"; // NOI18N

    /** Maximum mark distance. When there is an insertion done in document
    * and the distance between marks gets greater than this setting, another
    * mark will be inserted automatically.
    * Values: java.lang.Integer instances
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String MAX_MARK_DISTANCE = "max-mark-distance"; // NOI18N

    /** Minimum mark distance for removals. When there is a removal done
    * in document and it makes the marks to get closer than this value, then
    * the marks the additional marks that are closer than the distance
    * given by this setting are removed automatically.
    * Values: java.lang.Integer instances
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String MIN_MARK_DISTANCE = "min-mark-distance"; // NOI18N

    /** Size of one batch of characters loaded into syntax segment
    * when updating syntax marks. It prevents checking and loading
    * of syntax segment at every syntax mark. Instead it loads
    * at least the amount of characters given by this setting.
    * This whole process is done only in case the changes in syntax
    * extend the end of current line. If the syntax changes don't
    * extend to the next line, this setting has no effect.
    * Values: java.lang.Integer instances
    * WARNING! This is critical parameter for editor functionality.
    * Please see DefaultSettings.java for values of this setting
    */
    public static final String SYNTAX_UPDATE_BATCH_SIZE = "syntax-update-batch-size"; // NOI18N

    /** How many lines should be processed at once in the various text
    * processing. This is used for example when processing the text
    * by syntax scanner.
    */
    public static final String LINE_BATCH_SIZE = "line-batch-size"; // NOI18N

    /** Ignore the changes made to the settings through the visual property editors
    * working over the methods manipulating the settings. Generally there can be
    * two ways to change the settings in the IDE. One way is to directly
    * call the appropriate methods. The other way can be through some visual
    * property editors. This flag should be checked by the property editors
    * and if set to true, no changes to the settings should be performed.
    * This allows advanced users to completely control the settings through code
    * without any external undesired changes.
    */
    public static final String IGNORE_VISUAL_CHANGES = "ignore-visual-changes";

    /** List of the names of the additional colorings that need to be taken
    * into account for the particular kit. The colorings that come from
    * syntax coloring need not be specified here. Instead
    * the <tt>TOKEN_CONTEXT_LIST</tt> holds the list of token contexts
    * that can be used by the given kit.
    * The coloring names are without the suffix just like the predefined coloring names.
    * Values: java.util.List instances
    */
    public static final String COLORING_NAME_LIST = "coloring-name-list"; // NOI18N

    /** The list of the token contexts that the kit can use.
    * The editor-ui uses this setting to determine all the token-ids
    * and token-categories for the colorings.
    */
    public static final String TOKEN_CONTEXT_LIST = "token-context-list"; // NOI18N

    /** Suffix added to the coloring settings. The resulting name is used
    * as the name of the setting.
    */
    public static final String COLORING_NAME_SUFFIX = "-coloring"; // NOI18N

    /** Suffix added to the print coloring settings. The resulting name is used
    * as the name of the setting.
    */
    public static final String COLORING_NAME_PRINT_SUFFIX = "-print-coloring"; // NOI18N


    /** Default coloring for the drawing. */
    public static final String DEFAULT_COLORING = "default"; // NOI18N

    /** Coloring that will be used for line numbers displayed on the left
    * side on the screen.
    */
    public static final String LINE_NUMBER_COLORING = "line-number"; // NOI18N

    /** Coloring used for guarded blocks */
    public static final String GUARDED_COLORING = "guarded"; // NOI18N

    /** Coloring used for selection */
    public static final String SELECTION_COLORING = "selection"; // NOI18N

    /** Coloring used for highlight search */
    public static final String HIGHLIGHT_SEARCH_COLORING = "highlight-search"; // NOI18N

    /** Coloring used for incremental search */
    public static final String INC_SEARCH_COLORING = "inc-search"; // NOI18N

    /** Coloring used for bookmark lines */
    public static final String BOOKMARK_COLORING = "bookmark"; // NOI18N

    /** Coloring used for the status bar */
    public static final String STATUS_BAR_COLORING = "status-bar"; // NOI18N

    /** Coloring used to mark important text in the status bar */
    public static final String STATUS_BAR_BOLD_COLORING = "status-bar-bold"; // NOI18N

}
