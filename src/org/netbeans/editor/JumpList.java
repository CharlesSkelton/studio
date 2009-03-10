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
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
* The last several jumps in either the current file
* or across several files is stored here in the list.
* It's possible to track this list.
*
* @author Miloslav Metelka
* @version 1.00
*/
public class JumpList {

    /** Maximum size to which the list will be shrinked
    * if it exceeds the THRESHOLD_SIZE.
    */
    private static final int MAX_SIZE = 50;

    /** Reaching this count means that the size should be checked
    * and possibly shrinked to the MAX_SIZE.
    */
    private static final int CHECK_COUNT = 10;

    /** Current jump list entry */
    private static Entry currentEntry;

    private static int checkCnt;


    public static void checkAddEntry() {
        JTextComponent c = Utilities.getLastActiveComponent();
        if (c != null) {
            checkAddEntry(c, c.getCaret().getDot());
        }
    }

    public static void checkAddEntry(JTextComponent c) {
        checkAddEntry(c, c.getCaret().getDot());
    }

    public static void checkAddEntry(JTextComponent c, int pos) {
        if (currentEntry == null
                || currentEntry.getComponent() != c
                || currentEntry.getPosition() != pos
           ) {
            addEntry(c, pos);
        }
    }

    public static void addEntry(JTextComponent c, int pos) {
        try {
            Entry e = new Entry(c, pos, currentEntry);
            currentEntry = e;
            if (++checkCnt >= CHECK_COUNT) { // perform size check
                sizeCheck();
            }
        } catch (BadLocationException e) {
            // entry not added
        }
    }

    /**
    * @param c current component. It's used to compare the current
    *   component and position with those stored in the entries.
    */
    public static void jumpPrev(JTextComponent c) {
        int dotPos = c.getCaret().getDot();
        if (currentEntry != null) {
            while (true) {
                int entryPos = currentEntry.getPosition();
                JTextComponent entryComp = currentEntry.getComponent();
                if (entryComp != null && (entryComp != c || (entryPos >= 0 && entryPos != dotPos))) {
                    if (currentEntry.setDot()) {
                        break;
                    }
                }
                if (currentEntry.prev != null) { // must check not to end up with null
                    currentEntry = currentEntry.prev;
                } else {
                    break; // break when on the last entry
                }
            }
        }
    }

    /**
    * @param c current component. It's used to compare the current
    *   component and position with those stored in the entries.
    */
    public static void jumpNext(JTextComponent c) {
        int dotPos = c.getCaret().getDot();
        if (currentEntry != null) {
            while (true) {
                int entryPos = currentEntry.getPosition();
                JTextComponent entryComp = currentEntry.getComponent();
                if (entryComp != null && (entryComp != c || (entryPos >= 0 && entryPos != dotPos))) {
                    if (currentEntry.setDot()) {
                        break;
                    }
                }
                if (currentEntry.next != null) { // must check not to end up with null
                    currentEntry = currentEntry.next;
                } else {
                    break; // break when on the last entry
                }
            }
        }
    }

    /**
    * @param c current component. It's used to compare the current
    *   component to those stored in the jump list entries.
    */
    public static void jumpPrevComponent(JTextComponent c) {
        if (currentEntry != null) {
            while (true) {
                JTextComponent entryComp = currentEntry.getComponent();
                if (entryComp != null && entryComp != c) {
                    if (currentEntry.setDot()) {
                        break;
                    }
                }
                if (currentEntry.prev != null) { // must check not to end up with null
                    currentEntry = currentEntry.prev;
                } else {
                    break; // break when on the last entry
                }
            }
        }
    }

    /**
    * @param c current component. It's used to compare the current
    *   component to those stored in the jump list entries.
    */
    public static void jumpNextComponent(JTextComponent c) {
        if (currentEntry != null) {
            while (true) {
                JTextComponent entryComp = currentEntry.getComponent();
                if (entryComp != null && entryComp != c) {
                    if (currentEntry.setDot()) {
                        break;
                    }
                }
                if (currentEntry.next != null) { // must check not to end up with null
                    currentEntry = currentEntry.next;
                } else {
                    break; // break when on the last entry
                }
            }
        }
    }

    public static String dump() {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        Entry e = currentEntry;
        if (e != null) {
            while (true) {
                if (e.prev != null) {
                    e = e.prev;
                    i--;
                } else {
                    break;
                }
            }

            while (e != null) {
                JTextComponent comp = e.getComponent();
                String docStr = (comp != null) ?
                                (String)comp.getDocument().getProperty(Document.TitleProperty)
                                : "<Invalid document>"; // NOI18N
                if (docStr == null) { // no title property
                    docStr = "Untitled"; // NOI18N
                }
                sb.append("[" + i++ + "]=" + docStr + ", " + e.getPosition() + "\n"); // NOI18N
                e = e.next;
            }
        } else { // null current entry
            sb.append("Empty list"); // NOI18N
        }
        return sb.toString();
    }

    private static void sizeCheck() {
        int cnt = MAX_SIZE;
        Entry e = currentEntry;
        while (e != null && cnt > 0) {
            e = e.prev;
            cnt--; // #19429
        }
        if (e != null) { // reached the one that should be the first
            e.makeFirst();
        }
    }

    public static class Entry {

        /** ID of the stored position component */
        private int componentID;

        /** ID of the position stored in the document */
        private int posID;

        /** Previous entry in the linked list */
        Entry prev;

        /** Next entry in the linked list */
        Entry next;

        Entry(JTextComponent component, int offset, Entry last) throws BadLocationException {
            componentID = Registry.getID(component);
            posID = ((BaseDocument)component.getDocument()).storePosition(offset);
            if (last != null) { // apend after the last entry
                last.next = this;
                this.prev = last;
            }
        }

        public int getPosition() {
            JTextComponent c = Registry.getComponent(componentID);
            int pos = -1;
            if (c != null) {
                pos = ((BaseDocument)c.getDocument()).getStoredPosition(posID);
            }
            return pos;
        }

        public JTextComponent getComponent() {
            return Registry.getComponent(componentID);
        }

        /** Set the dot to the component and position
        * stored in the mark.
        * @return true if the caret was successfully moved
        */
        public boolean setDot() {
            JTextComponent c = getComponent();
            if (c != null) {
                if (Utilities.getLastActiveComponent() != c) {
                    Utilities.requestFocus(c); // possibly request for the component
                }

                int pos = getPosition();
                if (pos >= 0 && pos <= c.getDocument().getLength()) {
                    c.getCaret().setDot(pos); // set the dot
                    return true;
                }
            }
            return false;
        }

        void makeLast() {
            if (next != null) {
                next.prev = null;
                next = null;
            }
        }

        void makeFirst() {
            if (prev != null) {
                prev.next = null;
                prev = null;
            }
        }

        protected void finalize() throws Throwable {
            JTextComponent c = Registry.getComponent(componentID);
            if (c != null) {
                ((BaseDocument)c.getDocument()).removeStoredPosition(posID);
            }
            super.finalize();
        }

    }

}
