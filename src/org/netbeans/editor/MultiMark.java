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

import javax.swing.text.Position;


/**
 * Multipurpose mark that can be used
 * both as the traditional swing mark
 * or the bias mark.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

final class MultiMark {

    /** Whether mark has a backward (or forward) bias */
    static final int BACKWARD_BIAS = 1;
    
    /** Whether mark was disposed and can no longer be used */
    static final int VALID = 2;
    
    /** Storage of the marks uses this flag
     * to indicate that the diposed mark was physically removed
     * from the underlying array.
     */
    static final int REMOVED = 4;
    
    /** Whether mark behaves so that it conforms to the behavior
     * of the swing positions. This behavior requires the mark
     * to keep its offset to be zero once the mark
     * reaches the zero offset (by removal in the document).
     */
    static final int COMPATIBLE = 8;
    
    /** Whether the compatible mark has zero offset regardless of
     * what MarkVector.getOffset() would return.
     */
    static final int ZERO = 16;
    
    /** Offset at which the mark is located in the document. */
    int rawOffset;

    /** Composition of the flags */
    int flags;
    
    /** Mark vector that hosts this mark. */
    MarkVector markVector;
    
    /** Construct compatible mark */
    MultiMark(MarkVector markVector, int offset) {
        this(markVector, offset,
            (offset != 0) ? COMPATIBLE : (COMPATIBLE | ZERO | BACKWARD_BIAS));
    }
    
    /** Construct bias mark */
    MultiMark(MarkVector markVector, int offset, Position.Bias bias) {
        this(markVector, offset,
            (bias == Position.Bias.Backward) ? BACKWARD_BIAS : 0);
    }
        
    /** Construct new mark. The mark is invalid by default.
     */
    private MultiMark(MarkVector markVector, int offset, int flags) {
        this.markVector = markVector;
        this.rawOffset = offset; // will be corrected once the mark is inserted
        this.flags = flags;
    }

    /** @return the bias of this mark. It will be either
     * {@link javax.swing.text.Position.Bias.Forward}
     * or {@link javax.swing.text.Position.Bias.Backward}.
     */
    public Position.Bias getBias() {
        return ((flags & BACKWARD_BIAS) != 0)
            ? Position.Bias.Backward
            : Position.Bias.Forward;
    }
    
    /** Get the position of this mark */
    public int getOffset() {
        synchronized (markVector) {
            if ((flags & VALID) != 0) {
                return ((flags & ZERO) == 0)
                    ? markVector.getOffset(rawOffset)
                    : 0;
            } else { // already disposed
                throw new IllegalStateException();
            }
        }
    }

    /** Mark will no longer represent a valid place in the document.
     * Attempts to use the mark will result into throwing of
     * {@link java.lang.IllegalStateException}.
     * @throws IllegalStateException if the mark was already disposed before.
     */
    public void dispose() {
        synchronized (markVector) {
            if ((flags & VALID) != 0) {
                flags &= ~VALID;
                markVector.notifyMarkDisposed();
            } else { // already disposed before
                throw new IllegalStateException();
            }
        }
    }
    
    /** @return true if this mark was not disposed yet.
     */
    public boolean isValid() {
        synchronized(markVector) {
            return ((flags & VALID) != 0);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        synchronized(markVector) {
            if ((flags & VALID) != 0) {
                sb.append("offset=" + getOffset());
            } else {
                sb.append("removed");
            }
            sb.append(", bias=");
            sb.append(getBias());
            
            return sb.toString();
        }
    }

    public String toStringDetail() {
        StringBuffer sb = new StringBuffer();
        synchronized(markVector) {
            sb.append(System.identityHashCode(this));
            sb.append(" (");
            sb.append(rawOffset);
            sb.append(" -> ");
            if ((flags & VALID) != 0) {
                sb.append(getOffset());
            } else {
                sb.append('X');
                sb.append(markVector.getOffset(rawOffset));
                sb.append('X');
            }
            sb.append(", ");
            sb.append(((flags & BACKWARD_BIAS) != 0) ? 'B' : 'F');
            if ((flags & VALID) != 0) {
                sb.append('V');
            }
            if ((flags & REMOVED) != 0) {
                sb.append('R');
            }
            if ((flags & COMPATIBLE) != 0) {
                sb.append('C');
            }
            if ((flags & ZERO) != 0) {
                sb.append('Z');
            }
            sb.append(')');

            return sb.toString();
        }
    }


}
