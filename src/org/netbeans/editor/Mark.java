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
import javax.swing.text.Element;
import javax.swing.text.Position;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/**
* Marks hold the relative position in the document.
*
* @author Miloslav Metelka
* @version 1.00
*/


/** Class defining basic type of mark. This is a mark used most frequently.
* It's instances are inserted into the leaf plane of the tree.
*/
public class Mark {
    
    private static final MarkComparator MARK_COMPARATOR = new MarkComparator();

    /** Document to which this mark belongs. */
    private BaseDocument doc;
    
    /** MultiMark to which this mark delegates. */
    private MultiMark multiMark;
    
    /** Bias of the mark. It is either
     * {@link javax.swing.text.Position.Bias.Forward}
     * or {@link javax.swing.text.Position.Bias.Backward}
     */
    private Position.Bias bias;
    
    /** Construct new mark with forward bias. */
    public Mark() {
        this(Position.Bias.Forward);
    }

    public Mark(Position.Bias bias) {
        this.bias = bias;
    }
    
    /** Construct new mark.
    * @param backwardBias whether the inserts performed right at the position
    *   of this mark will go after this mark i.e. this mark will not move
    *   forward when inserting right at its position. This flag corresponds
    *   to <tt>Position.Bias.Backward</tt>.
    */
    public Mark(boolean backwardBias) {
        this(backwardBias ? Position.Bias.Backward : Position.Bias.Forward);
    }
    
    void insert(BaseDocument doc, int offset) throws InvalidMarkException, BadLocationException {
        BaseDocument ldoc = this.doc;
        if (ldoc != null) {
            throw new InvalidMarkException("Mark already inserted: mark=" + this
                + ", class=" + this.getClass());
        }

        this.doc = doc;
        ldoc = this.doc;
        Map docMarks = ldoc.marks;
        synchronized (docMarks) {
            if (multiMark != null) {
                throw new IllegalStateException("Mark already inserted: mark=" + this
                + ", class=" + this.getClass());
            }

            if (offset < 0 || offset > ldoc.getLength()) {
                throw new BadLocationException("Invalid offset", offset);
            }

            multiMark = doc.marksStorage.createBiasMark(offset, bias);
            doc.marksStorage.insert(multiMark);
            docMarks.put(multiMark, this);
//            checkMarks(docMarks);
        }
    }
    
    private void checkMarks(Map docMarks) {
        for (Iterator it = docMarks.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry)it.next();
            MultiMark mm = (MultiMark)me.getKey();
            Mark m = (Mark)me.getValue();
            
            if (m.multiMark != mm) {
                throw new IllegalStateException("m.class" + m.getClass() + " mapped to wrong mark=" + mm);
            }
        }            
    }
    
    void move(BaseDocument doc, int newOffset) throws InvalidMarkException, BadLocationException {
        dispose();
        insert(doc, newOffset);
    }
    
    /** Get the position of this mark */
    public final int getOffset() throws InvalidMarkException {
        BaseDocument ldoc = doc;
        if (ldoc != null) {
            Map docMarks = ldoc.marks;
            synchronized (docMarks) {
                if (multiMark != null) {
                    return multiMark.getOffset();
                } else {
                    throw new InvalidMarkException();
                }
            }
        } else {
            throw new InvalidMarkException();
        }
    }

    /** Get the line number of this mark */
    public final int getLine() throws InvalidMarkException {
        BaseDocument ldoc = doc;
        if (ldoc != null) {
            Map docMarks = ldoc.marks;
            synchronized (docMarks) {
                if (multiMark != null) {
                    int offset = multiMark.getOffset();
                    Element lineRoot = ldoc.getParagraphElement(0).getParentElement();
                    return lineRoot.getElementIndex(offset);

                } else {
                    throw new InvalidMarkException();
                }
            }
        } else {
            throw new InvalidMarkException();
        }
    }

    /** Get the insertAfter flag.
     * Replaced by {@link #getBackwardBias()}
     * @deprecated
     */
    public final boolean getInsertAfter() {
        return (bias == Position.Bias.Backward);
    }
    
    /** @return true if the mark has backward bias or false if it has forward bias.
     */
    public final boolean getBackwardBias() {
        return getInsertAfter();
    }
    
    /** @return the bias of this mark. It will be either
     * {@link javax.swing.text.Position.Bias.Forward}
     * or {@link javax.swing.text.Position.Bias.Backward}.
     */
    public final Position.Bias getBias() {
        return bias;
    }
    
    int getBiasAsInt() {
        return (bias == Position.Bias.Backward) ? -1 : +1;
    }
    
    /** Mark will no longer represent a valid place in the document.
     * Although it will not be removed from the structure that holds
     * the marks it will be done later automatically.
     */
    public final void dispose() {
        BaseDocument ldoc = doc;
        if (ldoc != null) {
            Map docMarks = ldoc.marks;
            synchronized (docMarks) {
                if (multiMark != null) {
                    if (docMarks.remove(multiMark) != this) {
                        throw new IllegalStateException("Mark cannot be disposed mark=" + this + ", class=" + getClass());
                    }

                    multiMark.dispose();
                    multiMark = null;

//                    checkMarks(docMarks);

                    this.doc = null;
                    
                    return;
                }
            }
        }

        throw new IllegalStateException("Mark already disposed: mark=" + this
                + ", class=" + this.getClass());
    }
        
    /** Remove mark from the structure holding the marks. The mark can
    * be inserted again into some document.
    */
    public final void remove() throws InvalidMarkException {
        dispose();
    }


    /** Compare this mark to some position.
     * @param pos tested position
     * @return zero - if the marks have the same position
     *         less than zero - if this mark is before the position
     *         greater than zero - if this mark is after the position
     */
    public final int compare(int pos) throws InvalidMarkException {
        return getOffset() - pos;
    }

    /** This function is called from removeUpdater when mark occupies
     * the removal area. The mark can decide what to do next.
     * If it doesn't redefine this method it will be simply moved to
     * the begining of removal area. It is valid to add or remove other mark 
     * from this method. It is even possible (but not very useful)
     * to add the mark to the removal area. However that mark will not be
     * notified about current removal.
     * @deprecated It will not be supported in the future.
     */
    protected void removeUpdateAction(int pos, int len) {
    }


    /** @return true if this mark is currently inserted in the document
     * or false otherwise.
     */
    public final boolean isValid() {
        BaseDocument ldoc = doc;
        if (ldoc != null) {
            Map docMarks = ldoc.marks;
            synchronized (docMarks) {
                return (multiMark != null && multiMark.isValid());
            }
        }
        
        return false;
    }

    /** Get info about <CODE>Mark</CODE>. */
    public String toString() {
        return "offset=" + (isValid() ? Integer.toString(multiMark.getOffset()) : "<invalid>") // NOI18N
               + ", bias=" + bias; // NOI18N
    }

    private static final class MarkComparator implements Comparator {
        
        public int compare(Object o1, Object o2) {
            Mark m1 = ((Mark)o1);
            Mark m2 = ((Mark)o2);
            try {
                int offDiff = m1.getOffset() - m2.getOffset();
                if (offDiff != 0) {
                    return offDiff;
                } else {
                    return m1.getBiasAsInt() - m2.getBiasAsInt();
                }
            } catch (InvalidMarkException e) {
                throw new IllegalStateException(e.toString());
            }
        }

    }

}
