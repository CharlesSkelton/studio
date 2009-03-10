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

import org.netbeans.editor.*;
import org.netbeans.editor.Utilities;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * Extended kit offering advanced functionality
 *
 * @author Miloslav Metelka
 * @version 1.00
 */
public class ExtKit extends BaseKit
{

    /** This action is searched and executed when the popup menu should
     * be displayed to build the popup menu.
     */
    public static final String buildPopupMenuAction = "build-popup-menu"; // NOI18N

    /** Show popup menu.
     */
    public static final String showPopupMenuAction = "show-popup-menu"; // NOI18N

    /** This action is searched and executed when the tool-tip should
     * be displayed by tool-tip support to build the tool-tip.
     */
    public static final String buildToolTipAction = "build-tool-tip"; // NOI18N

    /** Open find dialog action - this action is defined in view package, but
     * its name is defined here for clarity
     */
    public static final String findAction = "find"; // NOI18N

    /** Open replace dialog action - this action is defined in view package, but
     * its name is defined here for clarity
     */
    public static final String replaceAction = "replace"; // NOI18N

    /** Open goto dialog action - this action is defined in view package, but
     * its name is defined here for clarity
     */
    public static final String gotoAction = "goto"; // NOI18N

    /** Goto declaration depending on the context under the caret */
    public static final String gotoDeclarationAction = "goto-declaration"; // NOI18N

    /** Goto source depending on the context under the caret */
    public static final String gotoSourceAction = "goto-source"; // NOI18N

    /** Goto help depending on the context under the caret */
    public static final String gotoHelpAction = "goto-help"; // NOI18N

    /** Match brace */
    public static final String matchBraceAction = "match-brace"; // NOI18N

    /** Select the text to the matching bracket */
    public static final String selectionMatchBraceAction = "selection-match-brace"; // NOI18N

    /** Toggle the case for the first character of the word under caret */
    public static final String toggleCaseIdentifierBeginAction = "toggle-case-identifier-begin"; // NOI18N

    /** Advanced code selection technique */
    public static final String codeSelectAction = "code-select"; // NOI18N

    /** Action used when escape is pressed. By default it hides popup-menu */
    public static final String escapeAction = "escape"; // NOI18N

    /** Find the completion help and show it in the completion pane. */
    public static final String completionShowAction = "completion-show"; // NOI18N

    /** Hide completion pane. */
    public static final String completionHideAction = "completion-hide"; // NOI18N

    /** Hide javaDoc and completion popup panel */
    public static final String jdcPopupPanelHideAction = "jdc-popup-hide"; // NOI18N

    /** Show javaDoc and completion popup panel */
    public static final String jdcPopupPanelShowAction = "jdc-popup-show"; // NOI18N

    /** Used by completion to provide alternate item inserting way. */
    public static final String shiftInsertBreakAction = "shift-insert-break"; // NOI18N

    /** Comment out the selected block */
    public static final String commentAction = "comment"; // NOI18N

    /** Uncomment the selected block */
    public static final String uncommentAction = "uncomment"; // NOI18N

    /** Toggle the toolbar */
    public static final String toggleToolbarAction = "toggle-toolbar"; // NOI18N


    /** Shared suport for find and replace dialogs */
    private static FindDialogSupport findDialogSupport;


    public ExtKit()
    {
    }

    /** Create caret to navigate through document */
    public Caret createCaret()
    {
        return new ExtCaret();
    }

    public SyntaxSupport createSyntaxSupport(BaseDocument doc)
    {
        return new ExtSyntaxSupport(doc);
    }

    public Completion createCompletion(ExtEditorUI extEditorUI)
    {
        return null;
    }

/*    public CompletionJavaDoc createCompletionJavaDoc(ExtEditorUI extEditorUI) {
        return null;
    }
  */
    protected EditorUI createEditorUI()
    {
        return new ExtEditorUI();
    }

    protected Action[] createActions()
    {
        Action[] extActions = new Action[]{
            new BuildPopupMenuAction(),
            new ShowPopupMenuAction(),
            new BuildToolTipAction(),
            new FindAction(),
            new ReplaceAction(),
            new GotoAction(),
            new GotoDeclarationAction(),
            new ToggleCaseIdentifierBeginAction(),
            new MatchBraceAction(matchBraceAction, false),
            new MatchBraceAction(selectionMatchBraceAction, true),
//            new CodeSelectAction(),   // the actionPerformed is empty and so I'm removing the action from the list
            new EscapeAction(),
            new ExtDefaultKeyTypedAction(),
            new ExtInsertTabAction(),
            new ExtInsertBreakAction(),
            new CompletionShowAction(),
            new CompletionHideAction(),
            new JDCPopupPanelHideAction(),
            new JDCPopupPanelShowAction(),
            new ShiftInsertBreakAction()
        };
        return TextAction.augmentList(super.createActions(), extActions);
    }

    /** Called before the popup menu is shown to possibly rebuild
     * the popup menu.
     */
    public static class BuildPopupMenuAction extends BaseAction
    {

        static final long serialVersionUID = 4257043398248915291L;

        public BuildPopupMenuAction()
        {
            super(buildPopupMenuAction, NO_RECORDING);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                JPopupMenu pm = buildPopupMenu(target);
                ExtUtilities.getExtEditorUI(target).setPopupMenu(pm);
            }
        }

        protected JPopupMenu buildPopupMenu(JTextComponent target)
        {
            JPopupMenu pm = new JPopupMenu();
            EditorUI ui = Utilities.getEditorUI(target);
            List l = (List) Settings.getValue(Utilities.getKitClass(target),
                                              (ui == null || ui.hasExtComponent())
                                              ? ExtSettingsNames.POPUP_MENU_ACTION_NAME_LIST
                                              : ExtSettingsNames.DIALOG_POPUP_MENU_ACTION_NAME_LIST
            );

            if (l != null)
            {
                Iterator i = l.iterator();
                while (i.hasNext())
                {
                    String an = (String) i.next();
                    addAction(target, pm, an);
                }
            }
            return pm;
        }

        /** Add the action to the popup menu. This method is called
         * for each action-name found in the action-name-list. It should
         * add the appopriate menu item to the popup menu.
         * @param target target component for which the menu is being
         *  constructed.
         * @param popupMenu popup menu to which this method should add
         *  the item corresponding to the action-name.
         * @param actionName name of the action to add. The real action
         *  can be retrieved from the kit by calling <tt>getActionByName()</tt>.
         */
        protected void addAction(JTextComponent target, JPopupMenu popupMenu,
                                 String actionName)
        {
            Action a = Utilities.getKit(target).getActionByName(actionName);
            if (a != null)
            {
                JMenuItem item = null;
                if (a instanceof BaseAction)
                {
                    item = ((BaseAction) a).getPopupMenuItem(target);
                }
                if (item == null)
                {
                    String itemText = getItemText(target, actionName, a);
                    if (itemText != null)
                    {
                        item = new JMenuItem(itemText);
                        item.addActionListener(a);
                        // Try to get the accelerator
                        Keymap km = target.getKeymap();
                        if (km != null)
                        {
                            KeyStroke[] keys = km.getKeyStrokesForAction(a);
                            if (keys != null && keys.length > 0)
                            {
                                item.setAccelerator(keys[0]);
                            }
                        }
                        item.setEnabled(a.isEnabled());
                        Object helpID = a.getValue("helpID");
                        if (helpID != null && (helpID instanceof String))
                            item.putClientProperty("HelpID", helpID);
                    }
                }

                if (item != null)
                {
                    popupMenu.add(item);
                }

            }
            else
            { // action-name is null, add the separator
                popupMenu.addSeparator();
            }
        }

        protected String getItemText(JTextComponent target, String actionName, Action a)
        {
            String itemText;
            if (a instanceof BaseAction)
            {
                itemText = ((BaseAction) a).getPopupMenuText(target);
            }
            else
            {
                itemText = actionName;
            }
            return itemText;
        }

    }

    /** Show the popup menu.
     */
    public static class ShowPopupMenuAction extends BaseAction
    {

        static final long serialVersionUID = 4257043398248915291L;

        public ShowPopupMenuAction()
        {
            super(showPopupMenuAction, NO_RECORDING);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                try
                {
                    int dotPos = target.getCaret().getDot();
                    Rectangle r = target.getUI().modelToView(target, dotPos);
                    if (r != null)
                    {
                        ExtUtilities.getExtEditorUI(target).showPopupMenu(r.x, r.y + r.height);
                    }
                }
                catch (BadLocationException e)
                {
                    target.getToolkit().beep();
                }
            }
        }

    }

    public static class BuildToolTipAction extends BaseAction
    {

        static final long serialVersionUID = -2701131863705941250L;

        public BuildToolTipAction()
        {
            super(buildToolTipAction, NO_RECORDING);
        }

        protected String buildText(JTextComponent target)
        {
            ToolTipSupport tts = ExtUtilities.getExtEditorUI(target).getToolTipSupport();
            return (tts != null)
                    ? target.getToolTipText(tts.getLastMouseEvent())
                    : target.getToolTipText();
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                ToolTipSupport tts = ExtUtilities.getExtEditorUI(target).getToolTipSupport();
                if (tts != null)
                {
                    tts.setToolTipText(buildText(target));
                    //tts.setToolTipText("Testing 123");                    
                }
            }
        }

    }

    public static class FindAction extends BaseAction
    {

        static final long serialVersionUID = 719554648887497427L;

        public FindAction()
        {
            super(findAction, ABBREV_RESET
                              | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | NO_RECORDING);
        }

        public FindDialogSupport getSupport()
        {
            if (findDialogSupport == null)
            {
                findDialogSupport = new FindDialogSupport();
            }
            return findDialogSupport;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                getSupport().showFindDialog();
            }
        }

    }

    public static class ReplaceAction extends BaseAction
    {

        static final long serialVersionUID = 1828017436079834384L;

        public ReplaceAction()
        {
            super(replaceAction, ABBREV_RESET
                                 | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | NO_RECORDING);
        }

        public FindDialogSupport getSupport()
        {
            if (findDialogSupport == null)
            {
                findDialogSupport = new FindDialogSupport();
            }
            return findDialogSupport;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                getSupport().showReplaceDialog();
            }
        }

    }

    public static class GotoAction extends BaseAction
    {

        static final long serialVersionUID = 8425585413146373256L;

        public GotoAction()
        {
            super(gotoAction, ABBREV_RESET
                              | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
        }


        /** This method is called by the dialog support
         * to translate the line offset to the document position. This
         * can be changed for example for the diff operations.
         * @param doc document to operate over
         * @param lineOffset the line offset to convert to position
         * @return document offset that corresponds to the row-start
         *  of the line with the line-number equal to (lineOffset + 1).
         */
        protected int getOffsetFromLine(BaseDocument doc, int lineOffset)
        {
            return Utilities.getRowStartFromLineOffset(doc, lineOffset);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                new GotoDialogSupport().showGotoDialog();
            }
        }

    }

    /** Action to go to the declaration of the variable under the caret.
     */
    public static class GotoDeclarationAction extends BaseAction
    {

        static final long serialVersionUID = -6440495023918097760L;

        public GotoDeclarationAction()
        {
            super(gotoDeclarationAction,
                  ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET
                  | SAVE_POSITION
            );
        }

        public boolean gotoDeclaration(JTextComponent target)
        {
            try
            {
                Caret caret = target.getCaret();
                int dotPos = caret.getDot();
                BaseDocument doc = (BaseDocument) target.getDocument();
                int[] idBlk = Utilities.getIdentifierBlock(doc, dotPos);
                ExtSyntaxSupport extSup = (ExtSyntaxSupport) doc.getSyntaxSupport();
                if (idBlk != null)
                {
                    int decPos = extSup.findDeclarationPosition(doc.getText(idBlk), idBlk[1]);
                    if (decPos >= 0)
                    {
                        caret.setDot(decPos);
                        return true;
                    }
                }
            }
            catch (BadLocationException e)
            {
            }
            return false;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                gotoDeclaration(target); // try to go to the declaration position
            }
        }
    }

    public static class ToggleCaseIdentifierBeginAction extends BaseAction
    {

        static final long serialVersionUID = 584392193824931979L;

        ToggleCaseIdentifierBeginAction()
        {
            super(toggleCaseIdentifierBeginAction, ABBREV_RESET
                                                   | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                if (!target.isEditable() || !target.isEnabled())
                {
                    target.getToolkit().beep();
                    return;
                }

                try
                {
                    Caret caret = target.getCaret();
                    BaseDocument doc = (BaseDocument) target.getDocument();
                    int[] idBlk = Utilities.getIdentifierBlock(doc, caret.getDot());
                    if (idBlk != null)
                    {
                        Utilities.changeCase(doc, idBlk[0], 1, Utilities.CASE_SWITCH);
                    }
                }
                catch (BadLocationException e)
                {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class MatchBraceAction extends BaseAction
    {

        boolean select;

        static final long serialVersionUID = -184887499045886231L;

        public MatchBraceAction(String name, boolean select)
        {
            super(name, ABBREV_RESET
                        | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            this.select = select;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                try
                {
                    Caret caret = target.getCaret();
                    BaseDocument doc = Utilities.getDocument(target);
                    int dotPos = caret.getDot();
                    ExtSyntaxSupport sup = (ExtSyntaxSupport) doc.getSyntaxSupport();
                    if (dotPos > 0)
                    {
                        int[] matchBlk = sup.findMatchingBlock(dotPos - 1, false);
                        if (matchBlk != null)
                        {
                            if (select)
                            {
                                caret.moveDot(matchBlk[1]);
                            }
                            else
                            {
                                caret.setDot(matchBlk[1]);
                            }
                        }
                    }
                }
                catch (BadLocationException e)
                {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class CodeSelectAction extends BaseAction
    {

        static final long serialVersionUID = 4033474080778585860L;

        public CodeSelectAction()
        {
            super(codeSelectAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
/*            if (target != null) {
                BaseDocument doc = (BaseDocument)target.getDocument();
                SyntaxSupport sup = doc.getSyntaxSupport();
                Caret caret = target.getCaret();
                try {
                    int bracketPos = sup.findUnmatchedBracket(caret.getDot(), sup.getRightBrackets());
                    if (bracketPos >= 0) {
                        caret.setDot(bracketPos);
                        while (true) {
                          int bolPos = Utilities.getRowStart(doc, bracketPos);
                          boolean isWSC = sup.isCommentOrWhitespace(bolPos, bracketPos);
                          if (isWSC) { // get previous line end

                          }
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
*/
        }
    }

    /** Prefix maker adds the prefix before the identifier under cursor.
     * The prefix is not added if it's already present. The prefix to be
     * added is specified in the constructor of the action together
     * with the prefix group. If there's already any prefix from the prefix
     * group at the begining of the identifier, that prefix is replaced
     * by the actual prefix.
     */
    public static class PrefixMakerAction extends BaseAction
    {

        static final long serialVersionUID = -2305157963664484920L;

        private String prefix;

        private String[] prefixGroup;

        public PrefixMakerAction(String name, String prefix, String[] prefixGroup)
        {
            super(name);
            this.prefix = prefix;
            this.prefixGroup = prefixGroup;

            // [PENDING] This should be done in a better way
            String iconRes = null;
            if ("get".equals(prefix))
            {
                iconRes = "org/netbeans/modules/editor/resources/var_get";
            }
            else if ("set".equals(prefix))
            {
                iconRes = "org/netbeans/modules/editor/resources/var_set";
            }
            else if ("is".equals(prefix))
            {
                iconRes = "org/netbeans/modules/editor/resources/var_is";
            }
            if (iconRes != null)
            {
                putValue(BaseAction.ICON_RESOURCE_PROPERTY, iconRes);
            }
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                if (!target.isEditable() || !target.isEnabled())
                {
                    target.getToolkit().beep();
                    return;
                }

                BaseDocument doc = (BaseDocument) target.getDocument();
                int dotPos = target.getCaret().getDot();
                try
                {
                    // look for identifier around caret
                    int[] block = org.netbeans.editor.Utilities.getIdentifierBlock(doc, dotPos);

                    // If there is no identifier around, warn user
                    if (block == null)
                    {
                        target.getToolkit().beep();
                        return;
                    }

                    // Get the identifier to operate on
                    String identifier = doc.getText(block[0], block[1] - block[0]);

                    // Handle the case we already have the work done - e.g. if we got called over 'getValue'
                    if (identifier.startsWith(prefix) && Character.isUpperCase(identifier.charAt(prefix.length()))) return;

                    // Handle the case we have other type of known xEr: eg isRunning -> getRunning
                    for (int i = 0; i < prefixGroup.length; i++)
                    {
                        String actPref = prefixGroup[i];
                        if (identifier.startsWith(actPref)
                                && identifier.length() > actPref.length()
                                && Character.isUpperCase(identifier.charAt(actPref.length()))
                        )
                        {
                            doc.remove(block[0], actPref.length());
                            doc.insertString(block[0], prefix, null);
                            return;
                        }
                    }

                    // Upcase the first letter
                    Utilities.changeCase(doc, block[0], 1, Utilities.CASE_UPPER);
                    // Prepend the prefix before it
                    doc.insertString(block[0], prefix, null);
                }
                catch (BadLocationException e)
                {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class CommentAction extends BaseAction
    {

        static final long serialVersionUID = -1422954906554289179L;

        private String lineCommentString;

        public CommentAction(String lineCommentString)
        {
            super(commentAction);
            this.lineCommentString = lineCommentString;
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                     "org/netbeans/modules/editor/resources/comment");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                if (!target.isEditable() || !target.isEnabled())
                {
                    target.getToolkit().beep();
                    return;
                }
                Caret caret = target.getCaret();
                BaseDocument doc = (BaseDocument) target.getDocument();
                try
                {
                    if (caret.isSelectionVisible())
                    {
                        int startPos = Utilities.getRowStart(doc, target.getSelectionStart());
                        int endPos = target.getSelectionEnd();
                        doc.atomicLock();
                        try
                        {

                            if (endPos > 0 && Utilities.getRowStart(doc, endPos) == endPos)
                            {
                                endPos--;
                            }

                            int pos = startPos;
                            for (int lineCnt = Utilities.getRowCount(doc, startPos, endPos);
                                 lineCnt > 0; lineCnt--
                                    )
                            {
                                doc.insertString(pos, lineCommentString, null); // NOI18N
                                pos = Utilities.getRowStart(doc, pos, +1);
                            }

                        }
                        finally
                        {
                            doc.atomicUnlock();
                        }
                    }
                    else
                    { // selection not visible
                        doc.insertString(Utilities.getRowStart(doc, target.getSelectionStart()),
                                         lineCommentString, null); // NOI18N
                    }
                }
                catch (BadLocationException e)
                {
                    target.getToolkit().beep();
                }
            }
        }

    }

    public static class UncommentAction extends BaseAction
    {

        static final long serialVersionUID = -7005758666529862034L;

        private String lineCommentString;

        public UncommentAction(String lineCommentString)
        {
            super(uncommentAction);
            this.lineCommentString = lineCommentString;
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                     "org/netbeans/modules/editor/resources/uncomment");
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                if (!target.isEditable() || !target.isEnabled())
                {
                    target.getToolkit().beep();
                    return;
                }
                Caret caret = target.getCaret();
                BaseDocument doc = (BaseDocument) target.getDocument();
                try
                {
                    if (caret.isSelectionVisible())
                    {
                        int startPos = Utilities.getRowStart(doc, target.getSelectionStart());
                        int endPos = target.getSelectionEnd();
                        doc.atomicLock();
                        try
                        {

                            if (endPos > 0 && Utilities.getRowStart(doc, endPos) == endPos)
                            {
                                endPos--;
                            }

                            int pos = startPos;
                            for (int lineCnt = Utilities.getRowCount(doc, startPos, endPos);
                                 lineCnt > 0; lineCnt--
                                    )
                            {
                                if (Utilities.getRowEnd(doc, pos) - pos >= 2
                                        && doc.getText(pos, 2).equals(lineCommentString)
                                )
                                {
                                    doc.remove(pos, 2);
                                }
                                pos = Utilities.getRowStart(doc, pos, +1);
                            }

                        }
                        finally
                        {
                            doc.atomicUnlock();
                        }
                    }
                    else
                    { // selection not visible
                        int pos = Utilities.getRowStart(doc, caret.getDot());
                        if (Utilities.getRowEnd(doc, pos) - pos >= 2
                                && doc.getText(pos, 2).equals(lineCommentString) // NOI18N
                        )
                        {
                            doc.remove(pos, 2);
                        }
                    }
                }
                catch (BadLocationException e)
                {
                    target.getToolkit().beep();
                }
            }
        }

    }


    /** Executed when the Escape key is pressed. By default it hides
     * the popup menu if visible.
     */
    public static class EscapeAction extends BaseAction
    {

        public EscapeAction()
        {
            super(escapeAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                ExtUtilities.getExtEditorUI(target).hidePopupMenu();
            }
        }
    }


    // Completion customized actions
    public static class ExtDefaultKeyTypedAction extends DefaultKeyTypedAction
    {

        static final long serialVersionUID = 5273032708909044812L;

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            String cmd = evt.getActionCommand();
            int mod = evt.getModifiers();

            // Dirty fix for Completion shortcut on Unix !!!
            if (cmd != null && cmd.equals(" ") && (mod == ActionEvent.CTRL_MASK))
            { // NOI18N
                // Ctrl + SPACE
            }
            else
            {
                Caret caret = target.getCaret();
                if (caret instanceof ExtCaret)
                {
                    ((ExtCaret) caret).requestMatchBraceUpdateSync(); // synced bracket update
                }
                super.actionPerformed(evt, target);
            }

            if ((target != null) && (evt != null))
            {
                if ((cmd != null) && (cmd.length() == 1) &&
                        ((mod & ActionEvent.ALT_MASK) == 0
                        && (mod & ActionEvent.CTRL_MASK) == 0)
                )
                {
                    // Check whether char that should reindent the line was inserted
                    checkIndentHotChars(target, cmd);

                    // Check the completion
                    checkCompletion(target, cmd);
                }
            }
        }

        /** Check the characters that should cause reindenting the line. */
        protected void checkIndentHotChars(JTextComponent target, String typedText)
        {
            BaseDocument doc = Utilities.getDocument(target);
            if (doc != null)
            {
                Caret caret = target.getCaret();
                Formatter f = doc.getFormatter();
                if (f instanceof ExtFormatter)
                {
                    ExtFormatter ef = (ExtFormatter) f;
                    int[] fmtBlk = ef.getReformatBlock(target, typedText);

                    if (fmtBlk != null)
                    {
                        try
                        {
                            fmtBlk[0] = Utilities.getRowStart(doc, fmtBlk[0]);
                            fmtBlk[1] = Utilities.getRowEnd(doc, fmtBlk[1]);

                            //this was the of #18922, that causes the bug #20198
                            //ef.reformat(doc, fmtBlk[0], fmtBlk[1]);

                            //bugfix of the bug #20198. Bug #18922 is fixed too as well as #6968
                            ef.reformat(doc, fmtBlk[0], fmtBlk[1], true);

                        }
                        catch (BadLocationException e)
                        {
                        }
                        catch (IOException e)
                        {
                        }
                    }
                }
            }
        }


        /** Check and possibly popup, hide or refresh the completion */
        protected void checkCompletion(JTextComponent target, String typedText)
        {
            Completion completion = ExtUtilities.getCompletion(target);

            BaseDocument doc = (BaseDocument) target.getDocument();
            ExtSyntaxSupport extSup = (ExtSyntaxSupport) doc.getSyntaxSupport();

            if (completion != null && typedText.length() > 0)
            {
                if (!completion.isPaneVisible())
                {
                    if (completion.isAutoPopupEnabled())
                    {
                        int result = extSup.checkCompletion(target, typedText, false);
                        if (result == ExtSyntaxSupport.COMPLETION_POPUP)
                        {
                            completion.popup(true);
                        }
                        else if (result == ExtSyntaxSupport.COMPLETION_CANCEL)
                        {
                            completion.cancelRequest();
                        }
                    }
                }
                else
                {
                    int result = extSup.checkCompletion(target, typedText, true);
                    switch (result)
                    {
                        case ExtSyntaxSupport.COMPLETION_HIDE:
                            completion.setPaneVisible(false);
                            break;
                        case ExtSyntaxSupport.COMPLETION_REFRESH:
                            completion.refresh(false);
                            break;
                        case ExtSyntaxSupport.COMPLETION_POST_REFRESH:
                            completion.refresh(true);
                            break;
                    }
                }
            }
        }
    }

    public static class ExtInsertBreakAction extends InsertBreakAction
    {

        static final long serialVersionUID = 4004043376345356060L;

        public ExtInsertBreakAction()
        {
            super();
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                Completion completion = ExtUtilities.getCompletion(target);
                if (completion != null && completion.isPaneVisible())
                {
                    if (completion.substituteText(false))
                    {
                        completion.setPaneVisible(false);
                    }
                    else
                    {
                        completion.refresh(false);
                    }
                }
                else
                {
                    super.actionPerformed(evt, target);
                }
            }
        }

    }

    public static class ExtInsertTabAction extends InsertTabAction
    {

        static final long serialVersionUID = 2711045528538714986L;

        public ExtInsertTabAction()
        {
            super();
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                Completion completion = ExtUtilities.getCompletion(target);
                if (completion != null && completion.isPaneVisible())
                {
                    completion.refresh(false);
                    completion.substituteCommonText();
                }
                else
                {
                    super.actionPerformed(evt, target);
                }
            }
        }

    }


    public static class CompletionShowAction extends BaseAction
    {

        static final long serialVersionUID = 1050644925893851146L;

        public CompletionShowAction()
        {
            super(completionShowAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                Completion completion = ExtUtilities.getCompletion(target);
                if (completion != null)
                {
                    completion.setPaneVisible(true);
                }
            }
        }

    }

    public static class CompletionHideAction extends BaseAction
    {

        static final long serialVersionUID = -9162014350666711948L;

        public CompletionHideAction()
        {
            super(completionHideAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                Completion completion = ExtUtilities.getCompletion(target);
                if (completion != null)
                {
                    completion.setPaneVisible(false);
                }
            }
        }

    }


    public static class JDCPopupPanelHideAction extends BaseAction
    {

        //      static final long serialVersionUID =-9162014350666711948L;

        public JDCPopupPanelHideAction()
        {
            super(jdcPopupPanelHideAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                JDCPopupPanel jdc = ExtUtilities.getJDCPopupPanel(target);
                if (jdc != null)
                {
                    jdc.setVisible(false);
                }
            }
        }

    }

    public static class JDCPopupPanelShowAction extends BaseAction
    {

//        static final long serialVersionUID =1050644925893851146L;

        public JDCPopupPanelShowAction()
        {
            super(jdcPopupPanelShowAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                JDCPopupPanel jdc = ExtUtilities.getJDCPopupPanel(target);
                if (jdc != null)
                {
                    jdc.setVisible(true);
                }
            }
        }

    }


    public static class ShiftInsertBreakAction extends BaseAction
    {

        public ShiftInsertBreakAction()
        {
            super(shiftInsertBreakAction, ABBREV_RESET
                                          | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target)
        {
            if (target != null)
            {
                Completion completion = ExtUtilities.getCompletion(target);
                if (completion != null && completion.isPaneVisible())
                {
                    if (completion.substituteText(true))
                    {
//                        completion.setPaneVisible(false);
                    }
                    else
                    {
                        completion.refresh(false);
                    }
                }
            }
        }

    }

}
