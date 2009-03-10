/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.editor;

import java.util.ArrayList;

/** List of all bookmarks attached to document. This is helper class
 * for storage of all bookmarks attached to the document.
 *
 * @author David Konecny
 * @since 07/2001
 */

// TODO: this is first primitive version of storage of bookmark
// it must be rewritten and also persistence must be added

public class Bookmarks {

    /** List of bookmarks*/
    private ArrayList bookmarks;

    /** Creates new Bookmarks */
    public Bookmarks() {
        bookmarks = new ArrayList(15);
    }

    /** Find bookmark for the given line. */
    private Bookmark getInternalBookmark(int line) {
        for (int i=0; i < bookmarks.size() && ((Bookmark)bookmarks.get(i)).getLine() <= line; i++) {
            if (((Bookmark)bookmarks.get(i)).getLine() == line)
                return (Bookmark)bookmarks.get(i);
        }
        return null;
    }
    
    /** Find bookmark for the given line. */
    public Bookmark getBookmark(int line) {
        return getInternalBookmark(line);
    }

    /** Find next bookmark */
    public Bookmark getNextLineBookmark(int line) {
        for (int i=0; i < bookmarks.size(); i++) {
            if (((Bookmark)bookmarks.get(i)).getLine() >= line)
                return (Bookmark)bookmarks.get(i);
        }
        return null;
    }

    /** Add new bookmark into array */
    public void putBookmark(Bookmark bookmark) {
        int line = bookmark.getLine();
        boolean inserted = false;
        for (int i=0; i < bookmarks.size(); i++) {
            if (((Bookmark)bookmarks.get(i)).getLine() > line) {
                bookmarks.add(i, bookmark);
                inserted = true;
                break;
            }
        }
        if (!inserted)
                bookmarks.add(bookmark);
    }

    /** Remove bookmark from the line */
    public Bookmark removeBookmark(int line) {
        Bookmark bookmark;
        bookmark = getInternalBookmark(line);
        if (bookmark == null)
            return null;
        bookmarks.remove(bookmark);
        return bookmark;
    }

    /** Remove given bookmark */
    public void removeBookmark(Bookmark bookmark) {
        bookmarks.remove(bookmark);
    }

    /** Remove all bookmarks. Usually called when document is going to be closed. */
    public void removeAll() {
        for (int i=0; i < bookmarks.size(); i++) {
            ((Bookmark)bookmarks.get(i)).remove();
        }
        bookmarks = new ArrayList();
    }
    
    /** Interface defining bookmark. */
    public interface Bookmark {
        
        /** Line number of the bookmark */
        public int getLine();

        /** Method which is called when document is going to be closed.
         * Bookmark can handle this change of state if necessary. */
        public void remove();
    }
    
}
