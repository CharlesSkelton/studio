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

import org.netbeans.editor.Analyzer;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.FinderFactory;
import org.netbeans.editor.Utilities;

import javax.swing.text.BadLocationException;

/**
* Various finders are located here.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ExtFinderFactory {

    /** Finder that collects the whole lines and calls
    * the <tt>lineFound()</tt> method that can do a local find.
    * !!! Udelat to poradne i s vice bufferama
    */
    public static abstract class LineFwdFinder extends FinderFactory.AbstractFinder {

        private char[] lineBuffer = Analyzer.EMPTY_CHAR_ARRAY;

        private int lineLen;

        private int origStartPos;

        private int origLimitPos;

        public LineFwdFinder() {
        }

        public int adjustStartPos(BaseDocument doc, int startPos) {
            origStartPos = startPos;
            try {
                return Utilities.getRowStart(doc, startPos);
            } catch (BadLocationException e) {
                return startPos;
            }
        }

        public int adjustLimitPos(BaseDocument doc, int limitPos) {
            origLimitPos = limitPos;
            try {
                return Utilities.getRowEnd(doc, limitPos);
            } catch (BadLocationException e) {
                return limitPos;
            }
        }

        /** find function that must be defined by descendant */
        public int find(int bufferStartPos, char buffer[],
                        int offset1, int offset2, int reqPos, int limitPos) {
            int offset = reqPos - bufferStartPos; // !!! Udelat poradne s moznosti vice bufferu
            while (true) {
                int lfOffset = Analyzer.findFirstLFOffset(buffer, offset, offset2 - offset);
                boolean lfFound = (lfOffset >= 0);
                if (!lfFound) {
                    lfOffset = offset2;
                }

                int lineOffset = lineFound(buffer, offset, lfOffset,
                                           Math.max(origStartPos - bufferStartPos, offset),
                                           Math.min(origLimitPos - bufferStartPos, lfOffset));
                if (lineOffset >= 0) {
                    found = true;
                    return bufferStartPos + offset + lineOffset;
                }

                if (lfFound) {
                    offset = lfOffset + 1; // skip '\n'
                } else {
                    break;
                }
            }
            return bufferStartPos + offset2;
        }

        /** Line was found and is present in the given buffer. The given
        * buffer is either the original buffer passed to the <tt>find()</tt>
        * or constructed buffer if the line is at the border of the previous
        * and next buffer.
        * @return non-negative number means the target string was found and
        *   the returned number is offset on the line where the string was found.
        *   Negative number means the target string was not found on the line
        *   and the search will continue with the next line.
        */
        protected abstract int lineFound(char[] buffer, int lineStartOffset, int lineEndOffset,
                                         int startOffset, int endOffset);

    }

    /** Finder that collects the whole lines and calls
    * the <tt>lineFound()</tt> method that can do a local find.
    * !!! Udelat to poradne i s vice bufferama
    */
    public static abstract class LineBwdFinder extends FinderFactory.AbstractFinder {

        private char[] lineBuffer = Analyzer.EMPTY_CHAR_ARRAY;

        private int lineLen;

        private int origStartPos;

        private int origLimitPos;

        public LineBwdFinder() {
        }

        public int adjustStartPos(BaseDocument doc, int startPos) {
            origStartPos = startPos;
            try {
                return Utilities.getRowEnd(doc, startPos);
            } catch (BadLocationException e) {
                return startPos;
            }
        }

        public int adjustLimitPos(BaseDocument doc, int limitPos) {
            origLimitPos = limitPos;
            try {
                return Utilities.getRowStart(doc, limitPos);
            } catch (BadLocationException e) {
                return limitPos;
            }
        }

        /** find function that must be defined by descendant */
        public int find(int bufferStartPos, char buffer[],
                        int offset1, int offset2, int reqPos, int limitPos) {
            int offset = reqPos - bufferStartPos + 1; // !!! Udelat poradne s moznosti vice bufferu
            while (true) {
                boolean lfFound = false;
                int lfOffsetP1 = offset;
                while (lfOffsetP1 > offset1) {
                    if (buffer[--lfOffsetP1] == '\n') {
                        lfFound = true;
                        lfOffsetP1++; // past '\n'
                        break;
                    }
                }
                if (!lfFound) {
                    lfOffsetP1 = offset1;
                }

                int lineOffset = lineFound(buffer, lfOffsetP1, offset,
                                           Math.max(origLimitPos - bufferStartPos, lfOffsetP1),
                                           Math.min(origStartPos - bufferStartPos, offset));
                if (lineOffset >= 0) {
                    found = true;
                    return bufferStartPos + offset + lineOffset;
                }

                if (lfFound) {
                    offset = lfOffsetP1 - 1; // skip '\n'
                } else {
                    break;
                }
            }
            return bufferStartPos + offset1 - 1;
        }

        /** Line was found and is present in the given buffer. The given
        * buffer is either the original buffer passed to the <tt>find()</tt>
        * or constructed buffer if the line is at the border of the previous
        * and next buffer.
        * @return non-negative number means the target string was found and
        *   the returned number is offset on the line where the string was found.
        *   Negative number means the target string was not found on the line
        *   and the search will continue with the next line.
        */
        protected abstract int lineFound(char[] buffer, int lineStartOffset, int lineEndOffset,
                                         int startOffset, int endOffset);

    }

    /** Finder that collects the whole lines and calls
    * the <tt>lineFound()</tt> method that can do a local find.
    * !!! Udelat to poradne i s vice bufferama
    */
    public static abstract class LineBlocksFinder extends FinderFactory.AbstractBlocksFinder {

        private char[] lineBuffer = Analyzer.EMPTY_CHAR_ARRAY;

        private int lineLen;

        private int origStartPos;

        private int origLimitPos;

        public LineBlocksFinder() {
        }

        public int adjustStartPos(BaseDocument doc, int startPos) {
            origStartPos = startPos;
            try {
                return Utilities.getRowStart(doc, startPos);
            } catch (BadLocationException e) {
                return startPos;
            }
        }

        public int adjustLimitPos(BaseDocument doc, int limitPos) {
            origLimitPos = limitPos;
            try {
                return Utilities.getRowEnd(doc, limitPos);
            } catch (BadLocationException e) {
                return limitPos;
            }
        }

        /** find function that must be defined by descendant */
        public int find(int bufferStartPos, char buffer[],
                        int offset1, int offset2, int reqPos, int limitPos) {
            int offset = reqPos - bufferStartPos; // !!! Udelat poradne s moznosti vice bufferu
            while (true) {
                int lfOffset = Analyzer.findFirstLFOffset(buffer, offset, offset2 - offset);
                boolean lfFound = (lfOffset >= 0);
                if (!lfFound) {
                    lfOffset = offset2;
                }

                int lineOffset = lineFound(buffer, offset, lfOffset,
                                           Math.max(origStartPos - bufferStartPos, offset),
                                           Math.min(origLimitPos - bufferStartPos, lfOffset));
                if (lineOffset >= 0) {
                    found = true;
                    return bufferStartPos + offset + lineOffset;
                }

                if (lfFound) {
                    offset = lfOffset + 1; // skip '\n'
                } else {
                    break;
                }
            }
            return bufferStartPos + offset2;
        }

        /** Line was found and is present in the given buffer. The given
        * buffer is either the original buffer passed to the <tt>find()</tt>
        * or constructed buffer if the line is at the border of the previous
        * and next buffer.
        * @return non-negative number means the target string was found and
        *   the returned number is offset on the line where the string was found.
        *   Negative number means the target string was not found on the line
        *   and the search will continue with the next line.
        */
        protected abstract int lineFound(char[] buffer, int lineStartOffset, int lineEndOffset,
                                         int startOffset, int endOffset);

    }



}
