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
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * Various document-related utilities.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public class DocumentUtilities {
    
    static final SegmentCache SEGMENT_CACHE = new SegmentCache();
    
    private DocumentUtilities() {
        // no instances
    }

    /**
     * @return &gt;=0 offset of the gap start in the document's content.
     *         -1 if the document does not export <CODE>GapStart</CODE> interface.
     */
    public static int getGapStart(Document doc) {
        GapStart gs = (GapStart)doc.getProperty(GapStart.class);
        return (gs != null) ? gs.getGapStart() : -1;
    }
    
    /**
     * Copy portion of the document into target character array.
     * @param srcDoc document from which to copy.
     * @param srcStartOffset offset of the first character to copy.
     * @param srcEndOffset offset that follows the last character to copy.
     * @param dst destination character array into which the data will be copied.
     * @param dstOffset offset in the destination array at which the putting
     *  of the characters starts.
     */
    public static void copyText(Document srcDoc, int srcStartOffset,
    int srcEndOffset, char[] dst, int dstOffset) throws BadLocationException {
        Segment text = SEGMENT_CACHE.getSegment();
        try {
            int gapStart = getGapStart(srcDoc);
            if (gapStart != -1 && srcStartOffset < gapStart && gapStart < srcEndOffset) {
                // Get part below gap
                srcDoc.getText(srcStartOffset, gapStart - srcStartOffset, text);
                System.arraycopy(text.array, text.offset, dst, dstOffset, text.count);
                dstOffset += text.count;
                srcStartOffset = gapStart;
            }

            srcDoc.getText(srcStartOffset, srcEndOffset - srcStartOffset, text);
            System.arraycopy(text.array, text.offset, dst, dstOffset, srcEndOffset - srcStartOffset);
            
        } finally {
            SEGMENT_CACHE.releaseSegment(text);
        }
    }

}
