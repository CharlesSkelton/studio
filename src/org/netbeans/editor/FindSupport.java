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

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
* Find management
*
* @author Miloslav Metelka
* @version 1.00
*/

public class FindSupport {

    private static final String FOUND_LOCALE = "find-found"; // NOI18N
    private static final String NOT_FOUND_LOCALE = "find-not-found"; // NOI18N
    private static final String WRAP_START_LOCALE = "find-wrap-start"; // NOI18N
    private static final String WRAP_END_LOCALE = "find-wrap-end"; // NOI18N
    private static final String ITEMS_REPLACED_LOCALE = "find-items-replaced"; // NOI18N
    public static final String REVERT_MAP = "revert-map"; // NOI18N
    
    
    /** Shared instance of FindSupport class */
    static FindSupport findSupport;

    /** Find properties */
    private Map findProps;

    /** Support for firing change events */
    WeakPropertyChangeSupport changeSupport
    = new WeakPropertyChangeSupport();

    /** Current finder creator.
    * It can be changed by setFinderCreator.
    */
    FinderCreator finderCreator;

    /** Were the find properties already initialized
    * from settings?
    */
    private boolean findPropsInited;


    private FindSupport() {
        // prevent instance creation
    }

    /** Get shared instance of find support */
    public static FindSupport getFindSupport() {
        if (findSupport == null) {
            findSupport = new FindSupport();
        }
        return findSupport;
    }

    /** Get current finder creator */
    public FinderCreator getFinderCreator() {
        if (finderCreator == null) {
            finderCreator = new DefaultFinderCreator();
        }
        return finderCreator;
    }

    /** Set customized finder creator */
    public void setFinderCreator(FinderCreator finderCreator) {
        this.finderCreator = finderCreator;
        firePropertyChange(null, null, null);
    }

    public Map getDefaultFindProperties() {
        HashMap props = new HashMap();
        Class kitClass = BaseKit.class;
        props.put(SettingsNames.FIND_WHAT, Settings.getValue(
                      kitClass, SettingsNames.FIND_WHAT));
        props.put(SettingsNames.FIND_REPLACE_WITH, Settings.getValue(
                      kitClass, SettingsNames.FIND_REPLACE_WITH));
        props.put(SettingsNames.FIND_HIGHLIGHT_SEARCH, Settings.getValue(
                      kitClass, SettingsNames.FIND_HIGHLIGHT_SEARCH));
        props.put(SettingsNames.FIND_INC_SEARCH, Settings.getValue(
                      kitClass, SettingsNames.FIND_INC_SEARCH));
        props.put(SettingsNames.FIND_BACKWARD_SEARCH, Settings.getValue(
                      kitClass, SettingsNames.FIND_BACKWARD_SEARCH));
        props.put(SettingsNames.FIND_WRAP_SEARCH, Settings.getValue(
                      kitClass, SettingsNames.FIND_WRAP_SEARCH));
        props.put(SettingsNames.FIND_MATCH_CASE, Settings.getValue(
                      kitClass, SettingsNames.FIND_MATCH_CASE));
        props.put(SettingsNames.FIND_WHOLE_WORDS, Settings.getValue(
                      kitClass, SettingsNames.FIND_WHOLE_WORDS));
        props.put(SettingsNames.FIND_REG_EXP, Settings.getValue(
                      kitClass, SettingsNames.FIND_REG_EXP));
        props.put(SettingsNames.FIND_HISTORY, Settings.getValue(
                      kitClass, SettingsNames.FIND_HISTORY));

        return props;
    }

    public Map getFindProperties() {
        if (findProps == null) {
            findProps = getDefaultFindProperties();
        }
        return findProps;
    }

    /** Get find property with specified name */
    public Object getFindProperty(String name) {
        return getFindProperties().get(name);
    }

    private Map getValidFindProperties(Map props) {
        return (props != null) ? props : getFindProperties();
    }

    /** Get finder depending on find properties */
    public FinderFactory.StringFinder getStringFinder(BaseDocument doc, Map props) {
        FinderFactory.StringFinder stringFinder
        = (FinderFactory.StringFinder)doc.getProperty(BaseDocument.STRING_FINDER_PROP);

        if (stringFinder == null) {
            stringFinder = getFinderCreator().createFinder(doc,
                           getValidFindProperties(props));
            doc.putProperty(BaseDocument.STRING_FINDER_PROP, stringFinder);
        }

        return stringFinder;
    }

    /** Get opposite direction finder depending on find properties */
    public FinderFactory.StringFinder getStringBwdFinder(BaseDocument doc, Map props) {
        FinderFactory.StringFinder stringBwdFinder
        = (FinderFactory.StringFinder)doc.getProperty(BaseDocument.STRING_BWD_FINDER_PROP);

        if (stringBwdFinder == null) {
            stringBwdFinder = getFinderCreator().createBwdFinder(doc,
                              getValidFindProperties(props));
            doc.putProperty(BaseDocument.STRING_BWD_FINDER_PROP, stringBwdFinder);
        }

        return stringBwdFinder;
    }

    /** Get position pairs finder depending on find properties */
    public FinderFactory.BlocksFinder getBlocksFinder(BaseDocument doc, Map props) {
        FinderFactory.BlocksFinder blocksFinder
        = (FinderFactory.BlocksFinder)doc.getProperty(BaseDocument.BLOCKS_FINDER_PROP);

        if (blocksFinder == null) {
            blocksFinder = getFinderCreator().createBlocksFinder(doc,
                           getValidFindProperties(props));
        }
        return blocksFinder;
    }

    int[] getBlocks(int[] blocks, BaseDocument doc,
                    int startPos, int endPos) throws BadLocationException {
        FinderFactory.BlocksFinder ppf = getBlocksFinder(doc, null);
        if (ppf == null) { // not yet assigned
            return blocks;
        }
        synchronized (ppf) {
            ppf.setBlocks(blocks);
            doc.find(ppf, startPos, endPos);
            return ppf.getBlocks();
        }
    }

    /** Get find property without performing initialization
    * of find properties. This is useful for example for base document
    * when it wants to query whether it should do highlight search.
    */
    Object getPropertyNoInit(String name) {
        if (findProps == null) {
            return null;
        } else {
            return getFindProperty(name);
        }
    }

    /** Set find property with specified name and fire change.
    */
    public void putFindProperty(String name, Object newValue) {
        Object oldValue = getFindProperty(name);
        if ((oldValue == null && newValue == null)
                || (oldValue != null && oldValue.equals(newValue))
           ) {
            return;
        }
        if (newValue != null) {
            getFindProperties().put(name, newValue);
        } else {
            getFindProperties().remove(name);
        }
        firePropertyChange(name, oldValue, newValue);
    }

    /** Add/replace properties from some other map
    * to current find properties. If the added properties
    * are different than the original ones,
    * the property change is fired.
    */
    public void putFindProperties(Map propsToAdd) {
        if (!getFindProperties().equals(propsToAdd)) {
            getFindProperties().putAll(propsToAdd);
            firePropertyChange(null, null, null);
        }
    }

    public boolean incSearch(Map props) {
        props = getValidFindProperties(props);
        Boolean b = (Boolean)props.get(SettingsNames.FIND_INC_SEARCH);
        if (b != null && b.booleanValue()) { // inc search enabled
            JTextComponent c = Utilities.getLastActiveComponent();
            if (c != null) {
                Caret caret = c.getCaret();
                BaseDocument doc = (BaseDocument)c.getDocument();
                int dot = caret.getDot();
                Finder finder = getFinderCreator().createFinder(doc, props);
                int pos;
                try {
                    b = (Boolean)props.get(SettingsNames.FIND_BACKWARD_SEARCH);
                    boolean back = (b != null && b.booleanValue());
                    if (back) {
                        pos = doc.find(finder, dot, 0); // !!! handle wrap search
                    } else {
                        pos = doc.find(finder, dot, -1); // !!! handle wrap search
                    }
                } catch (BadLocationException e) {
                    if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                        e.printStackTrace();
                    }
                    return false;
                }

                // possibly create incSearch layer
                BaseTextUI ui = (BaseTextUI)c.getUI();
                EditorUI editorUI = ui.getEditorUI();
                DrawLayerFactory.IncSearchLayer incLayer
                = (DrawLayerFactory.IncSearchLayer)editorUI.findLayer(
                      DrawLayerFactory.INC_SEARCH_LAYER_NAME);
                if (incLayer == null) {
                    incLayer = new DrawLayerFactory.IncSearchLayer();
                    if (!editorUI.addLayer(incLayer,
                        DrawLayerFactory.INC_SEARCH_LAYER_VISIBILITY)
                    ) {
                        return false; // couldn't add layer
                    }
                } else {
                    if (incLayer.isEnabled()) {
                        incLayer.setEnabled(false);
                        try {
                            editorUI.repaintOffset(incLayer.getOffset());
                        } catch (BadLocationException e) {
                            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if (pos >= 0) {
                    String s = (String)props.get(SettingsNames.FIND_WHAT);
                    int len = (s != null) ? s.length() : 0;
                    if (len > 0) {
                        incLayer.setEnabled(true);
                        incLayer.setArea(pos, len);
                        try {
                            Rectangle r0 = (Rectangle)ui.modelToView(c, pos, null);
                            Rectangle r = (Rectangle)ui.modelToView(c, pos + len, null);
                            r.add(r0);
                            editorUI.repaintOffset(pos);
                            editorUI.scrollRectToVisible(r, EditorUI.SCROLL_DEFAULT);
                        } catch (BadLocationException e) {
                            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                                e.printStackTrace();
                                return false;
                            }
                        }
                        return true;
                    }
                } else { // string not found
                    // !!!          ((BaseCaret)c.getCaret()).dispatchUpdate();
                }
            }
        } else { // inc search not enabled
            incSearchReset();
        }
        return false;
    }

    public void incSearchReset() {
        JTextComponent c = Utilities.getLastActiveComponent();
        if (c==null) return; //  #19558 bugfix. Editor window has been closed => c==null
        EditorUI editorUI = ((BaseTextUI)c.getUI()).getEditorUI();
        DrawLayerFactory.IncSearchLayer incLayer
        = (DrawLayerFactory.IncSearchLayer)editorUI.findLayer(
              DrawLayerFactory.INC_SEARCH_LAYER_NAME);
        if (incLayer != null) {
            if (incLayer.isEnabled()) {
                incLayer.setEnabled(false);
                try {
                    editorUI.repaintOffset(incLayer.getOffset());
                } catch (BadLocationException e) {
                    if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean isBackSearch(Map props, boolean oppositeDir) {
        Boolean b = (Boolean)props.get(SettingsNames.FIND_BACKWARD_SEARCH);
        boolean back = (b != null && b.booleanValue());
        if (oppositeDir) {
            back = !back;
        }
        return back;
    }

    /** Find the text from the caret position.
    * @param props search properties
    * @param oppositeDir whether search in opposite direction
    */
    public boolean find(Map props, boolean oppositeDir) {
        incSearchReset();
        props = getValidFindProperties(props);
        boolean back = isBackSearch(props, oppositeDir);
        JTextComponent c = Utilities.getLastActiveComponent();
        Object findWhat = props.get(SettingsNames.FIND_WHAT);
        if (findWhat == null) { // nothing to search for
            return true;
        }

        String exp = "'" + findWhat + "' "; // NOI18N
        if (c != null) {
            Utilities.clearStatusText(c);
            Caret caret = c.getCaret();
            int dotPos = caret.getDot();
            try {
                int[] blk = findInBlock(c, dotPos, 0, -1, props, oppositeDir);
                if (blk != null) {
                    if (caret instanceof BaseCaret) { // support extended scroll mode
                        BaseCaret bCaret = (BaseCaret)caret;
                        if (back) { // back direction
                            bCaret.setDot(blk[1], bCaret, EditorUI.SCROLL_FIND);
                            bCaret.moveDot(blk[0], bCaret, EditorUI.SCROLL_FIND);

                        } else { // forward direction
                            bCaret.setDot(blk[0], bCaret, EditorUI.SCROLL_FIND);
                            bCaret.moveDot(blk[1], bCaret, EditorUI.SCROLL_FIND);
                        }
                            
                            
                    } else { // regular caret
                        if (back) {
                            caret.setDot(blk[1]);
                            caret.moveDot(blk[0]);
                        } else { // forward direction
                            caret.setDot(blk[0]);
                            caret.moveDot(blk[1]);
                        }
                    }

                    JumpList.checkAddEntry();
                    String msg = exp + LocaleSupport.getString(FOUND_LOCALE)
                                 + ' ' + Utilities.debugPosition((BaseDocument)c.getDocument(), blk[0]);
                    if (blk[2] == 1) { // wrap was done
                        msg += "; "; // NOI18N
                        msg += back ? LocaleSupport.getString(WRAP_END_LOCALE)
                               : LocaleSupport.getString(WRAP_START_LOCALE);

                        Utilities.setStatusBoldText(c, msg);
                    } else {
                        Utilities.setStatusText(c, msg);
                    }
                    return true;
                } else { // not found
                    Utilities.setStatusBoldText(c, exp + LocaleSupport.getString(
                                                    NOT_FOUND_LOCALE));
                    // issue 14189 - selection was not removed
                    c.getCaret().setDot(c.getCaret().getDot());
                }
            } catch (BadLocationException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /** Find the searched expression
    * @param startPos position from which to search. It must be inside the block.
    * @param blockStartPos starting position of the block. It must
    *   be valid position greater or equal than zero. It must be lower than
    *   or equal to blockEndPos (except blockEndPos=-1).
    * @param blockEndPos ending position of the block. It can be -1 for the end
    *   of document. It must be greater or equal than blockStartPos (except blockEndPos=-1).
    * @param props search properties
    * @param oppositeDir whether search in opposite direction
    * @param displayWrap whether display messages about the wrapping
    * @return either null when nothing was found or integer array with three members
    *    ret[0] - starting position of the found string
    *    ret[1] - ending position of the found string
    *    ret[2] - 1 or 0 when wrap was or wasn't performed in order to find the string 
    */
    public int[] findInBlock(JTextComponent c, int startPos, int blockStartPos,
                             int blockEndPos, Map props, boolean oppositeDir) throws BadLocationException {
        if (c != null) {
            props = getValidFindProperties(props);
            BaseDocument doc = (BaseDocument)c.getDocument();
            FinderFactory.StringFinder sf
            = oppositeDir ? getStringBwdFinder(doc, props)
              : getStringFinder(doc, props);
            int pos = -1;
            boolean wrapDone = false;

            boolean back = isBackSearch(props, oppositeDir);
            Boolean b = (Boolean)props.get(SettingsNames.FIND_WRAP_SEARCH);
            boolean wrap = (b != null && b.booleanValue());
            int docLen = doc.getLength();
            if (blockEndPos == -1) {
                blockEndPos = docLen;
            }

            while (true) {
                pos = doc.find(sf, startPos, back ? blockStartPos : blockEndPos);
                if (pos != -1) {
                    break;
                }

                if (wrap) {
                    if (back) {
                        //Bug #20552 the wrap search check whole document
                        //instead of just the remaining not-searched part to be
                        //able to find expressions with the cursor in it

                        //blockStartPos = startPos;
                        startPos = blockEndPos;
                    } else {
                        //blockEndPos = startPos;
                        startPos = blockStartPos;
                    }
                    wrapDone = true;
                    wrap = false; // only one loop
                } else { // no wrap set
                    break;
                }

            }

            if (pos != -1) {
                int[] ret = new int[3];
                ret[0] = pos;
                ret[1] = pos + sf.getFoundLength();
                ret[2] = wrapDone ? 1 : 0;
                return ret;
            }
        }
        return null;
    }

    public boolean replace(Map props, boolean oppositeDir)
    throws BadLocationException {
        incSearchReset();
        props = getValidFindProperties(props);
        Boolean b = (Boolean)props.get(SettingsNames.FIND_BACKWARD_SEARCH);
        boolean back = (b != null && b.booleanValue());
        if (oppositeDir) {
            back = !back;
        }

        JTextComponent c = Utilities.getLastActiveComponent();
        if (c != null) {
            Caret caret = c.getCaret();
            if (!caret.isSelectionVisible()) {
                if (!find(props, oppositeDir)) { // nothing found
                    return false;
                }
            }
            // now there's selected text to be replaced
            BaseDocument doc = (BaseDocument)c.getDocument();
            int startPos = c.getSelectionStart();
            int len = c.getSelectionEnd() - startPos;
            doc.atomicLock();
            try {
                if (len > 0) {
                    doc.remove(startPos, len);
                }
                String s = (String)props.get(SettingsNames.FIND_REPLACE_WITH);
                if (s != null && s.length() > 0) {
                    doc.insertString(startPos, s, null);
                }
            } finally {
                doc.atomicUnlock();
            }
            if (back) {
                // Handle repetitive search ('a' replaced by 'aa') by moving the caret
                if (caret instanceof BaseCaret) {
                    ((BaseCaret)caret).setDot(startPos, (BaseCaret)caret, EditorUI.SCROLL_FIND);
                    
                } else { // regular caret
                    caret.setDot(startPos);
                }
            }
        }
        return true;
    }

    public void replaceAll(Map props) {
        incSearchReset();
        JTextComponent c = Utilities.getLastActiveComponent();
        BaseDocument doc = (BaseDocument)c.getDocument();
        int maxCnt = doc.getLength();
        int replacedCnt = 0;
        int totalCnt = 0;

        props = getValidFindProperties(props);
        props = new HashMap(props);
        props.put(SettingsNames.FIND_WRAP_SEARCH, Boolean.FALSE);

        // bugfix of #20476
        // if replaceAll, then set backward search to false.
        // Also reset FinderFactory.StringFinder, that is stored as property  BaseDocument.STRING_FINDER_PROP
        // via firePropertyChange ...
        props.put(SettingsNames.FIND_BACKWARD_SEARCH, Boolean.FALSE);
        firePropertyChange(null, null, null); 
        
        String replaceWith = (String)props.get(SettingsNames.FIND_REPLACE_WITH);

        if (c != null) {
            doc.atomicLock();
            try {
                int pos = 0; // actual position
                while (true) {
                    int[] blk = findInBlock(c, pos, 0, -1, props, false);
                    if (blk == null) {
                        break;
                    }
                    totalCnt++;
                    int len = blk[1] - blk[0];
                    boolean skip = false; // cannot remove (because of guarded block)?
                    try {
                        doc.remove(blk[0], len);
                    } catch (GuardedException e) {
                        // replace in guarded block
                        skip = true;
                    }
                    if (skip) {
                        pos = blk[0] + len;

                    } else { // can and will insert the new string
                        if (replaceWith != null && replaceWith.length() > 0) {
                            doc.insertString(blk[0], replaceWith, null);
                        }
                        pos = blk[0] + ((replaceWith != null) ? replaceWith.length() : 0);
                        replacedCnt++;
                    }
                }
                
                // Display message about replacement
                MessageFormat fmt = new MessageFormat(
                                        LocaleSupport.getString(ITEMS_REPLACED_LOCALE));
                String msg = fmt.format(new Object[] { new Integer(replacedCnt), new Integer(totalCnt) });
                Utilities.setStatusText(c, msg);

            } catch (BadLocationException e) {
                e.printStackTrace();
            } finally {
                doc.atomicUnlock();
            }

        }
    }

    /** Get position of wrap mark for some document */
    public int getWrapSearchMarkPos(BaseDocument doc) {
        Mark mark = (Mark)doc.getProperty(BaseDocument.WRAP_SEARCH_MARK_PROP);
        try {
            return (mark != null) ? mark.getOffset() : doc.getLength();
        } catch (InvalidMarkException e) {
            throw new RuntimeException(); // shouldn't happen
        }
    }

    /** Set new position of wrap mark for some document */
    public void setWrapSearchMarkPos(BaseDocument doc, int pos) {
        //!!!
    }

    /** Add weak listener to listen to change of any property. The caller must
    * hold the listener object in some instance variable to prevent it
    * from being garbage collected.
    */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(l);
    }

    public synchronized void addPropertyChangeListener(String findPropertyName,
            PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(findPropertyName, l);
    }

    /** Remove listener for changes in properties */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(l);
    }

    void firePropertyChange(String settingName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(this, settingName, oldValue, newValue);
    }

    public interface FinderCreator {

        /** Create finder for regular finding */
        public FinderFactory.StringFinder createFinder(BaseDocument doc, Map searchProps);

        /** Create finder for finding in opposite direction than regular finder */
        public FinderFactory.StringFinder createBwdFinder(BaseDocument doc, Map searchProps);

        /** Create finder that returns position pairs useful for highlight search */
        public FinderFactory.BlocksFinder createBlocksFinder(BaseDocument doc, Map searchProps);

    }

    public static class DefaultFinderCreator implements FinderCreator {

        protected Finder createFinder(BaseDocument doc,
                                      Map searchProps, boolean oppositeDir, boolean blocksFinder) {

            String text = (String)searchProps.get(SettingsNames.FIND_WHAT);
            if (text == null || text.length() == 0) {
                if (blocksFinder) {
                    return new FinderFactory.FalseBlocksFinder();
                } else {
                    return new FinderFactory.FalseFinder();
                }
            }

            Boolean b = (Boolean)searchProps.get(SettingsNames.FIND_BACKWARD_SEARCH);
            boolean bwdSearch = (b != null && b.booleanValue());
            if (oppositeDir) { // negate for opposite direction search
                bwdSearch = !bwdSearch;
            }

            b = (Boolean)searchProps.get(SettingsNames.FIND_MATCH_CASE);
            boolean matchCase = (b != null && b.booleanValue());
            b = (Boolean)searchProps.get(SettingsNames.FIND_SMART_CASE);
            boolean smartCase = (b != null && b.booleanValue());
            b = (Boolean)searchProps.get(SettingsNames.FIND_WHOLE_WORDS);
            boolean wholeWords = (b != null && b.booleanValue());

            if (smartCase && !matchCase) {
                int cnt = text.length();
                for (int i = 0; i < cnt; i++) {
                    if (Character.isUpperCase(text.charAt(i))) {
                        matchCase = true;
                    }
                }
            }

            //      searchProps.get(SettingsNames.FIND_REG_EXP);
            if (blocksFinder) {
                if (wholeWords) {
                    return new FinderFactory.WholeWordsBlocksFinder(doc, text, matchCase);
                } else {
                    return new FinderFactory.StringBlocksFinder(text, matchCase);
                }
            } else {
                if (wholeWords) {
                    if (bwdSearch) {
                        return new FinderFactory.WholeWordsBwdFinder(doc, text, matchCase);
                    } else {
                        return new FinderFactory.WholeWordsFwdFinder(doc, text, matchCase);
                    }
                } else {
                    if (bwdSearch) {
                        return new FinderFactory.StringBwdFinder(text, matchCase);
                    } else {
                        return new FinderFactory.StringFwdFinder(text, matchCase);
                    }
                }
            }
        }

        public FinderFactory.StringFinder createFinder(BaseDocument doc, Map searchProps) {
            return (FinderFactory.StringFinder)createFinder(doc, searchProps, false, false);
        }

        public FinderFactory.StringFinder createBwdFinder(BaseDocument doc, Map searchProps) {
            return (FinderFactory.StringFinder)createFinder(doc, searchProps, true, false);
        }

        /** Creates finder that makes position pairs of begining
        * and end of the matched word.
        */
        public FinderFactory.BlocksFinder createBlocksFinder(BaseDocument doc, Map searchProps) {
            return (FinderFactory.BlocksFinder)createFinder(doc, searchProps, false, true);
        }

    }

}
