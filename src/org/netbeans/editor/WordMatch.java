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
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;

/** Word matching support enables to fill in the rest of the word
* when knowing the begining of the word. It is capable to search either
* only in current file or also in several or all open files.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class WordMatch extends FinderFactory.AbstractFinder
    implements SettingsChangeListener, PropertyChangeListener {

    private static final Object NULL_DOC = new Object();

    /** Mapping of kit class to document with the static word */
    private static final HashMap staticWordsDocs = new HashMap();

    /** First part of matching word expressed as char[].
    * Status of word matching support
    * can be tested by looking if this variable is null. If it is,
    * word matching was reset and it's not initialized yet.
    */
    char[] baseWord;

    /** Found characters are accumulated here */
    char[] word = new char[20];

    /** Last word returned */
    String lastWord;

    /** Previous word returned */
    String previousWord;

    /** Current index in word */
    int wordLen;

    /** HashMap for already matched words */
    StringMap wordsMap = new StringMap();

    /** ArrayList holding already found words and their positions. */
    ArrayList wordInfoList = new ArrayList();

    /** Current index in word match vector. Reaching either first or last
    * index of vector means searching backward or forward respectively from
    * position stored in previous vector's element.
    */
    int wordsIndex;

    /** Current search direction */
    boolean forwardSearch;

    /** Pointer to editorUI instance */
    EditorUI editorUI;

    /** Whether the search should be wrapped */
    boolean wrapSearch;

    /** Search with case matching */
    boolean matchCase;

    /** Search using smart case */
    boolean smartCase;

    /** This is the flag that really says whether the search is matching case
    * or not. The value is (smartCase ? (is-there-capital-in-base-word?) : matchCase).
    */
    boolean realMatchCase;

    /** Whether the match should be reported when word is found
    * which is only one char long.
    */
    boolean matchOneChar;

    /** Maximum lenght in chars of the search area.
    * If the number is zero, no search is performed except the static words.
    */
    int maxSearchLen;

    /** Current count of documents where the search was performed */
    int searchLen;

    /** Document where to start from */
    BaseDocument startDoc;

    /** Construct new word match over given view manager */
    public WordMatch(EditorUI editorUI) {
        this.editorUI = editorUI;

        Settings.addSettingsChangeListener(this);

        synchronized (editorUI.getComponentLock()) {
            // if component already installed in EditorUI simulate installation
            JTextComponent component = editorUI.getComponent();
            if (component != null) {
                propertyChange(new PropertyChangeEvent(editorUI,
                                                       EditorUI.COMPONENT_PROPERTY, null, component));
            }

            editorUI.addPropertyChangeListener(this);
        }
    }

    /** Called when settings were changed. The method is called
    * by editorUI when settings were changed and from constructor.
    */
    public void settingsChange(SettingsChangeEvent evt) {
        if (evt != null) { // real change event
            staticWordsDocs.clear();
        }

        Class kitClass = Utilities.getKitClass(editorUI.getComponent());
        if (kitClass != null) {
            maxSearchLen = SettingsUtil.getInteger(kitClass, SettingsNames.WORD_MATCH_SEARCH_LEN,
                                                   Integer.MAX_VALUE);
            wrapSearch = SettingsUtil.getBoolean(kitClass, SettingsNames.WORD_MATCH_WRAP_SEARCH,
                                                 true);
            matchOneChar = SettingsUtil.getBoolean(kitClass, SettingsNames.WORD_MATCH_MATCH_ONE_CHAR,
                                                   true);
            matchCase = SettingsUtil.getBoolean(kitClass, SettingsNames.WORD_MATCH_MATCH_CASE,
                                                false);
            smartCase = SettingsUtil.getBoolean(kitClass, SettingsNames.WORD_MATCH_SMART_CASE,
                                                false);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (EditorUI.COMPONENT_PROPERTY.equals(propName)) {
            JTextComponent component = (JTextComponent)evt.getNewValue();
            if (component != null) { // just installed

                settingsChange(null);

            } else { // just deinstalled
                //        component = (JTextComponent)evt.getOldValue();

            }

        }
    }

    /** Clear word matching, so that it forgots the remembered
    * matching words.
    */
    public synchronized void clear() {
        if (baseWord != null) {
            baseWord = null;
            wordsMap.clear();
            wordInfoList.clear();
            wordsIndex = 0;
            searchLen = maxSearchLen;
        }
    }

    /** Reset this finder before each search */
    public void reset() {
        super.reset();
        wordLen = 0;
    }

    /** Find next matching word and replace it on current cursor position
    * @param forward in which direction should the search be done
    */
    public synchronized String getMatchWord(int startPos, boolean forward) {
        int listSize = wordInfoList.size();
        boolean searchNext = (listSize == 0)
                             || (wordsIndex == (forward ? (listSize - 1) : 0));
        startDoc = (BaseDocument)editorUI.getComponent().getDocument();
        String ret = null;

        // initialize base word if necessary
        if (baseWord == null) {
            try {
                String baseWordString = Utilities.getIdentifierBefore(startDoc, startPos);
                if (baseWordString == null) {
                    baseWordString = ""; // NOI18N
                }
                lastWord = baseWordString;
                baseWord = baseWordString.toCharArray();

                WordInfo info = new WordInfo(baseWordString,
                                             startDoc.createPosition(startPos - baseWord.length), startDoc);
                wordsMap.put(info.word, info);
                wordInfoList.add(info);
            } catch (BadLocationException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }
            }
            if (smartCase && !matchCase) {
                realMatchCase = false;
                for (int i = 0; i < baseWord.length; i++) {
                    if (Character.isUpperCase(baseWord[i])) {
                        realMatchCase = true;
                    }
                }
            } else {
                realMatchCase = matchCase;
            }
            // make lowercase if not matching case
            if (!realMatchCase) {
                for (int i = 0; i < baseWord.length; i++) {
                    baseWord[i] = Character.toLowerCase(baseWord[i]);
                }
            }
        }

        // possibly search next word
        if (searchNext) {
            try {
                // determine start document and position
                BaseDocument doc; // actual document
                int pos; // actual position
                if (listSize > 0) {
                    WordInfo info = (WordInfo)wordInfoList.get(wordsIndex);
                    doc = info.doc;
                    pos = info.pos.getOffset();
                    if (forward) {
                        pos += info.word.length();
                    }
                } else {
                    doc = startDoc;
                    pos = startPos;
                }

                // search for next occurence
                while (doc != null) {
                    if (doc.getLength() > 0) {
                        int endPos;
                        if (doc == startDoc) {
                            if (forward) {
                                endPos = (pos >= startPos) ? -1 : startPos;
                            } else { // bwd
                                endPos = (pos == -1 || pos > startPos) ? startPos : 0;
                            }
                        } else { // not starting doc
                            endPos = -1;
                        }

                        this.forwardSearch = !(!forward && (doc == startDoc));
                        int foundPos = doc.find(this, pos, endPos);
                        if (foundPos != -1) { // found
                            if (forward) {
                                wordsIndex++;
                            }
                            WordInfo info = new WordInfo(new String(word, 0, wordLen),
                                                         doc.createPosition(foundPos), doc);
                            wordsMap.put(info.word, info);
                            wordInfoList.add(wordsIndex, info);
                            previousWord = lastWord;
                            lastWord = info.word;
                            return lastWord;
                        }
                        if (doc == startDoc) {
                            if (forward) {
                                pos = 0;
                                if (endPos != -1 || !wrapSearch) {
                                    doc = getNextDoc(doc);
                                }
                            } else { // bwd
                                if (pos == -1 || !wrapSearch) {
                                    doc = getNextDoc(doc);
                                    pos = 0;
                                } else {
                                    pos = -1; // stay on the same document
                                }
                            }
                        } else { // not starting doc
                            doc = getNextDoc(doc);
                            pos = 0;
                        }
                    } else { // empty document
                        doc = getNextDoc(doc);
                        pos = 0; // should be anyway
                    }
                }
                // Return null in this case
            } catch (BadLocationException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }
            }
        } else { // use word from the list
            wordsIndex += (forward ? 1 : -1);
            previousWord = lastWord;
            lastWord = ((WordInfo)wordInfoList.get(wordsIndex)).word;
            ret = lastWord;
        }

        startDoc = null;
        return ret;
    }

    public String getPreviousWord() {
        return previousWord;
    }

    private void doubleWordSize() {
        char[] tmp = new char[word.length * 2];
        System.arraycopy(word, 0, tmp, 0, word.length);
        word = tmp;
    }

    private boolean checkWord() {
        // check matching of one-char string
        if (!matchOneChar && wordLen == 1) {
            return false;
        }

        // check word start
        if (baseWord.length > 0) {
            if (wordLen < baseWord.length) {
                return false;
            }
            for (int i = 0; i < baseWord.length; i++) {
                if (realMatchCase) {
                    if (word[i] != baseWord[i]) {
                        return false;
                    }
                } else { // case-insensitive
                    if (Character.toLowerCase(word[i]) != baseWord[i]) {
                        return false;
                    }
                }
            }
        }

        // check existing words
        if (wordsMap.containsKey(word, 0, wordLen)) {
            return false;
        }
        return true; // new word found
    }


    public int find(int bufferStartPos, char buffer[],
                    int offset1, int offset2, int reqPos, int limitPos) {
        int offset = reqPos - bufferStartPos;
        if (forwardSearch) {
            int limitOffset = limitPos - bufferStartPos - 1;
            while (offset < offset2) {
                char ch = buffer[offset];
                boolean wp = startDoc.isIdentifierPart(ch);
                if (wp) { // append the char
                    if (wordLen == word.length) {
                        doubleWordSize();
                    }
                    word[wordLen++] = ch;
                }

                if (!wp) {
                    if (wordLen > 0) {
                        if (checkWord()) {
                            found = true;
                            return bufferStartPos + offset - wordLen;
                            
                        } else {
                            wordLen = 0;
                        }
                    }

                } else { // current char is word part
                    if (limitOffset == offset) {
                        if (checkWord()) {
                            found = true;
                            // differs in one char because current is part of word
                            return bufferStartPos + offset - wordLen + 1;

                        } else {
                            wordLen = 0;
                        }
                    }
                }

                offset++;
            }
        } else { // bwd search
            int limitOffset = limitPos - bufferStartPos;
            while (offset >= offset1) {
                char ch = buffer[offset];
                boolean wp = startDoc.isIdentifierPart(ch);
                if (wp) {
                    if (wordLen == word.length) {
                        doubleWordSize();
                    }
                    word[wordLen++] = ch;
                }
                if (!wp || (limitOffset == offset)) {
                    if (wordLen > 0) {
                        Analyzer.reverse(word, wordLen); // reverse word chars
                        if (checkWord()) {
                            found = true;
                            return (wp) ? bufferStartPos + offset + 1
                                : bufferStartPos + offset;
                        } else {
                            wordLen = 0;
                        }
                    }
                }
                offset--;
            }
        }
        return bufferStartPos + offset;
    }


    private BaseDocument getNextDoc(BaseDocument doc) {
        if (doc == getStaticWordsDoc()) {
            return null;
        }
        BaseDocument nextDoc = Registry.getLessActiveDocument(doc);
        if (nextDoc == null) {
            nextDoc = getStaticWordsDoc();
        }
        return nextDoc;
    }

    private BaseDocument getStaticWordsDoc() {
        Class kitClass = Utilities.getKitClass(editorUI.getComponent());
        Object val = staticWordsDocs.get(kitClass);
        if (val == NULL_DOC) {
            return null;
        }
        BaseDocument doc = (BaseDocument)val;
        if (doc == null) {
            String staticWords = (String)Settings.getValue(kitClass,
                                 SettingsNames.WORD_MATCH_STATIC_WORDS);
            if (staticWords != null) {
                doc = new BaseDocument(BaseKit.class, false); // don't add to registry
                try {
                    doc.insertString(0, staticWords, null);
                } catch (BadLocationException e) {
                    if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                        e.printStackTrace();
                    }
                }
                staticWordsDocs.put(kitClass, doc);
            } else { // null static words
                staticWordsDocs.put(kitClass, NULL_DOC);
            }
        }
        return doc;
    }

    /** Word match info - used in previous/next word matching.
    * It contains info found word and next matching position.
    */
    private static final class WordInfo {

        public WordInfo(String word, Position pos, BaseDocument doc) {
            this.word = word;
            this.pos = pos;
            this.doc = doc;
        }

        /** Found word */
        String word;

        /** Position of the word in document.
        * Positions are used so that the marks are removed
        * when they are no longer necessary.
        */
        Position pos;

        /** Document where the word resides */
        BaseDocument doc;

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof WordMatch) {
                WordMatch wm = (WordMatch)o;
                return Analyzer.equals(word, wm.word, 0, wm.wordLen);
            }
            if (o instanceof WordInfo) {
                return word.equals(((WordInfo)o).word);
            }
            if (o instanceof String) {
                return word.equals(o);
            }
            return false;
        }

        public int hashCode() {
            return word.hashCode();
        }

        public String toString() {
            return "{word='" + word + "', pos=" + pos.getOffset() // NOI18N
                   + ", doc=" + Registry.getID(doc) + "}"; // NOI18N
        }

    }

    public String toString() {
        return "baseWord=" + ((baseWord != null) ? ("'" + baseWord.toString() + "'") // NOI18N
                              : "null") + ", wrapSearch=" + wrapSearch // NOI18N
               + ", matchCase=" + matchCase + ", smartCase=" + smartCase // NOI18N
               + ", matchOneChar=" + matchOneChar + ", maxSearchLen=" + maxSearchLen // NOI18N
               + ", wordsMap=" + wordsMap + "\nwordInfoList=" + wordInfoList // NOI18N
               + "\nwordsIndex=" + wordsIndex; // NOI18N
    }

}
