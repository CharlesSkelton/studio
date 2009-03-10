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

package org.netbeans.editor;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

/**
* Support class for chain of MarkBlocks
*
* @author Miloslav Metelka
* @version 1.00
*/

public class MarkChain {

    /** Chain of all marks */
    protected MarkFactory.ChainDrawMark chain;

    /** Current mark to make checks faster */
    protected MarkFactory.ChainDrawMark curMark;

    /** Document for this mark */
    protected BaseDocument doc;

    /** If this chain uses draw marks, then this is the name for the draw layer
    * that will be used for the marks
    */
    protected String layerName;

    /** The mark created by addMark() method is stored in this variable. In case
     * the mark was not created, because there already was some on this position,
     * the already existing mark is returned. */
    private MarkFactory.ChainDrawMark recentlyAddedMark;
    
    /** Construct chain using draw marks */
    public MarkChain(BaseDocument doc, String layerName) {
        this.doc = doc;
        this.layerName = layerName;
    }

    public final MarkFactory.ChainDrawMark getChain() {
        return chain;
    }

    public final MarkFactory.ChainDrawMark getCurMark() {
        return curMark;
    }

    /** Tests whether the position range is partly or fully inside
    * some mark block from the chain.
    * @param pos compared position
    * @return relation of curMark to the given position
    */
    public int compareMark(int pos) {
        try {
            if (curMark == null) {
                curMark = chain;
                if (curMark == null) {
                    return -1; // no marks yet
                }
            }

            int rel;
            boolean after = false;
            boolean before = false;
            while ((rel = curMark.compare(pos)) != 0) { // just match
                if (rel > 0) { // this mark after pos
                    if (before) {
                        return rel;
                    }
                    if (curMark.prev != null) {
                        after = true;
                        curMark = curMark.prev;
                    } else { // end of chain
                        return rel;
                    }
                } else { // this mark before pos
                    if (after) {
                        return rel;
                    }
                    if (curMark.next != null) {
                        before = true;
                        curMark = curMark.next;
                    } else { // start of chain
                        return rel;
                    }
                }
            }
            return 0; // match
        } catch (InvalidMarkException e) {
            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                e.printStackTrace();
            }
            return -1; // don't match, but what to return?
        }
    }

    protected MarkFactory.ChainDrawMark createAndInsertNewMark(int pos)
    throws BadLocationException {
        MarkFactory.ChainDrawMark mark = createMark();
        try {
            mark.insert(doc, pos);
        } catch (InvalidMarkException e) {
            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                e.printStackTrace();
            }
        }
        return mark;
    }

    protected MarkFactory.ChainDrawMark createMark() {
        MarkFactory.ChainDrawMark mark = new MarkFactory.ChainDrawMark(layerName, null, Position.Bias.Backward);
        mark.activateLayer = true;
        return mark;
    }

    /** Add mark to the chain
    * @return true if the mark was added
    *         false if there's already mark at that pos
    */
    public boolean addMark(int pos) throws BadLocationException {
        int rel = compareMark(pos);
        if (rel == 0) {
            recentlyAddedMark = curMark;
            return false; // already exists
        } else if (rel > 0) { // curMark after pos
            MarkFactory.ChainDrawMark mark = createAndInsertNewMark(pos);
            recentlyAddedMark = mark;
            if (curMark != null) {
                if (curMark == chain) { // curMark is first mark
                    chain = curMark.insertChain(mark);
                } else { // curMark is not first mark
                    curMark.insertChain(mark);
                }
            } else { // no marks in chain
                chain = mark;
            }
        } else { // curMark before pos
            MarkFactory.ChainDrawMark mark = createAndInsertNewMark(pos);
            recentlyAddedMark = mark;
            if (curMark != null) {
                if (curMark.next != null) {
                    curMark.next.insertChain(mark);
                } else { // last mark in chain
                    curMark.setNextChain(mark);
                }
            } else { // no marks in chain
                chain = mark;
            }
        }
        return true;
    }

    /** The mark created by addMark() method is returned by this method. In case
     * the mark was not created, because there already was some on requested position,
     * the already existing mark is returned. */
    public MarkFactory.ChainDrawMark getAddedMark() {
        return recentlyAddedMark;
    }
    
    /** Remove non-empty block from area covered by blocks from chain */
    public boolean removeMark(int pos) {
        int rel = compareMark(pos);
        if (rel == 0) {
            boolean first = (curMark == chain);
            curMark = curMark.removeChain();
            if (first) {
                chain = curMark;
            }
            return true;
        } else { // not found
            return false;
        }
    }

    /** Is there mark at given position? */
    public boolean isMark(int pos) {
        return (compareMark(pos) == 0);
    }

    /** Toggle the mark so that if it didn't exist it is created
    * and if it existed it's removed
    * @return true if the new mark was added
    *         false if the existing mark was removed
    */
    public boolean toggleMark(int pos) throws BadLocationException {
        int rel = compareMark(pos);
        if (rel == 0) { // exists
            removeMark(pos);
            return false;
        } else { // didn't exist
            addMark(pos);
            return true;
        }
    }


    public String toString() {
        return "MarkChain: curMark=" + curMark + ", mark chain: " // NOI18N
               + (chain != null ? ("\n" + chain.toStringChain()) : "Empty"); // NOI18N
    }

}
