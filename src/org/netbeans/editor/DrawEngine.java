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
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* Class responsible for drawing the editor component.
*
* @author Miloslav Metelka
* @version 1.00
*/
class DrawEngine {

    /** Whether debug messages should be displayed */
    private static final boolean debug
        = Boolean.getBoolean("netbeans.debug.editor.draw"); // NOI18N
    /** Whether debug messages for each token fragment should be displayed */
    private static final boolean debugFragment
        = Boolean.getBoolean("netbeans.debug.editor.draw.fragment"); // NOI18N


    /** Only one instance of draw-engine */
    private static DrawEngine drawEngine;
    
    private static final char[] SPACE = new char[] { ' ' };

    /** Prevent creation */
    private DrawEngine() {
    }

    /** Get the static instance of draw-engine */
    public static DrawEngine getDrawEngine() {
        if (drawEngine == null) {
            drawEngine = new DrawEngine();
        }
        return drawEngine;
    }

    private void initLineNumbering(DrawInfo ctx) {
        // Resolve whether line numbers will be painted
        ctx.lineNumbering = ctx.editorUI.lineNumberVisible
                            && ctx.drawGraphics.supportsLineNumbers();

        // create buffer for showing line numbers
        if (ctx.lineNumbering) {
            try {
                ctx.startLineNumber = Utilities.getLineOffset(ctx.doc, ctx.startOffset) + 1;
            } catch (BadLocationException e) {
                if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                    e.printStackTrace();
                }
            }

            ctx.lineNumberColoring = ctx.editorUI.getColoring(SettingsNames.LINE_NUMBER_COLORING);
            if (ctx.lineNumberColoring == null) {
                ctx.lineNumberColoring = ctx.defaultColoring; // no number coloring found

            } else { // lineNumberColoring not null
                ctx.lineNumberColoring = ctx.lineNumberColoring.apply(ctx.defaultColoring);
            }

            Font lnFont = ctx.lineNumberColoring.getFont();
            if (lnFont == null) {
                lnFont = ctx.defaultColoring.getFont();
            }

            Color lnBackColor = ctx.lineNumberColoring.getBackColor();
            if (lnBackColor == null) {
                lnBackColor = ctx.defaultColoring.getBackColor();
            }

            Color lnForeColor = ctx.lineNumberColoring.getForeColor();
            if (lnForeColor == null) {
                lnForeColor = ctx.defaultColoring.getForeColor();
            }

            ctx.lineNumberChars = new char[Math.max(ctx.editorUI.lineNumberMaxDigitCount, 1)];
            if (ctx.graphics == null) {
                ctx.syncedLineNumbering = true;

            } else { // non-synced line numbering - need to remember line start offsets
                try {
                    int endLineNumber = Utilities.getLineOffset(ctx.doc, ctx.endOffset) + 1;
                    ctx.lineStartOffsets = new int[endLineNumber - ctx.startLineNumber + 2]; // reserve more
                } catch (BadLocationException e) {
                    if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void initInfo(DrawInfo ctx) throws BadLocationException {
        ctx.x = ctx.startX;
        ctx.y = ctx.startY;
        ctx.lineHeight = ctx.editorUI.getLineHeight();
        ctx.defaultColoring = ctx.editorUI.getDefaultColoring();
        ctx.tabSize = ctx.doc.getTabSize();
        ctx.fragmentOffset = ctx.startOffset; // actual painting position
        ctx.graphics = ctx.drawGraphics.getGraphics();

        if (ctx.graphics != null) {
            if (ctx.editorUI.renderingHints != null) {
                ((Graphics2D)ctx.graphics).setRenderingHints(ctx.editorUI.renderingHints);
            }

            if (ctx.editorUI.textLimitLineVisible) { // draw limit line
                int lineX = ctx.startX + ctx.editorUI.textLimitWidth * ctx.editorUI.defaultSpaceWidth;
                ctx.graphics.setColor(ctx.editorUI.textLimitLineColor);
                Rectangle clip = ctx.graphics.getClipBounds();
                //            ctx.graphics.drawRect(lineX, clip.y, 1, clip.height);
                ctx.graphics.drawLine(lineX, clip.y, lineX, clip.y + clip.height);
            }
        }

        initLineNumbering(ctx);

        // Initialize draw context
        ctx.foreColor = ctx.defaultColoring.getForeColor();
        ctx.backColor = ctx.defaultColoring.getBackColor();
        ctx.font = ctx.defaultColoring.getFont();
        ctx.bol = true; // draw must always start at line begin

        // Init draw graphics
        ctx.drawGraphics.init(ctx);
        ctx.drawGraphics.setDefaultBackColor(ctx.defaultColoring.getBackColor());
        ctx.drawGraphics.setLineHeight(ctx.lineHeight);
        ctx.drawGraphics.setLineAscent(ctx.editorUI.getLineAscent());
        ctx.drawGraphics.setX(ctx.x);
        ctx.drawGraphics.setY(ctx.y);

        // Init all draw-layers
        ctx.layers = ctx.editorUI.getDrawLayerList().currentLayers();
        int layersLength = ctx.layers.length;
        ctx.layerActives = new boolean[layersLength];
        ctx.layerActivityChangeOffsets = new int[layersLength];

        for (int i = 0; i < layersLength; i++) {
            ctx.layers[i].init(ctx); // init all layers
        }

        ctx.drawMarkList = new ArrayList();
        Map docMarks = ctx.doc.marks;
        MarkVector docMarksStorage = ctx.doc.marksStorage;
        synchronized (docMarks) {
            int offset = ctx.startOffset;
            int low = 0;
            int markCount = docMarksStorage.getMarkCount();
            int high = markCount - 1;

            while (low <= high) {
                int mid = (low + high) >> 1;
                int cmp = docMarksStorage.getMarkOffsetInternal(mid) - offset;

                if (cmp < 0)
                    low = mid + 1;
                else if (cmp > 0)
                    high = mid - 1;
                else { // found
                    while (--mid >= 0
                        && docMarksStorage.getMarkOffsetInternal(mid) == offset
                    ) { }
                    low = mid + 1;
                    break;
                }

            }

            // the low contains first mark to consider
            offset = ctx.endOffset;
            while (low < markCount) {
                MultiMark m = (MultiMark)docMarksStorage.getMark(low);
                if (m.isValid()) {
                    if (m.getOffset() > offset) {
                        break;
                    }

                    Mark mark = (Mark)docMarks.get(m);
                    if (mark == null) {
                        throw new IllegalStateException("No mark for m=" + m);
                    }
                    if (mark instanceof MarkFactory.DrawMark) {
                        ctx.drawMarkList.add(mark);
                    }
                }

                low++;
            }
        }
        
        
        // Get current draw mark
        ctx.drawMarkIndex = 0;
        ctx.drawMarkOffset = Integer.MAX_VALUE;
        if (ctx.drawMarkList.size() > 0) {
            ctx.drawMark = (MarkFactory.DrawMark)ctx.drawMarkList.get(ctx.drawMarkIndex++);
            try {
                ctx.drawMarkOffset = ctx.drawMark.getOffset();
            } catch (InvalidMarkException e) {
                throw new IllegalStateException(e.toString());
            }
            if (ctx.drawMarkOffset < ctx.updateOffset) {
                ctx.updateOffset = ctx.drawMarkOffset;
                ctx.drawMarkUpdate = true;
            }
        }

        // Prepare syntax scanner and then cycle through all the syntax segments
        int endTokenSafeOffset = ctx.doc.getTokenSafeOffset(ctx.endOffset);
        ctx.doc.prepareSyntax(ctx.text, ctx.syntax,
            ctx.startOffset, endTokenSafeOffset - ctx.startOffset,
            false, false
        );

        ctx.textArray = ctx.text.array;
        ctx.buffer = ctx.textArray;
        ctx.bufferStartOffset = ctx.startOffset - ctx.syntax.getOffset();
        ctx.drawGraphics.setBuffer(ctx.textArray);

        ctx.continueDraw = true;
    }

    private void handleBOL(DrawInfo ctx) {
        if (ctx.lineNumbering) {
            if (ctx.syncedLineNumbering) {
                // Draw line numbers synchronously at begining of each line

                // Init context
                ctx.foreColor = ctx.lineNumberColoring.getForeColor();
                ctx.backColor = ctx.lineNumberColoring.getBackColor();
                ctx.font = ctx.lineNumberColoring.getFont();
                ctx.strikeThroughColor = null;
                ctx.underlineColor = null;
                ctx.waveUnderlineColor = null;

                int lineNumber = ctx.startLineNumber + ctx.lineIndex;
                // Update line-number by layers
                int layersLength = ctx.layers.length;
                for (int i = 0; i < layersLength; i++) {
                    lineNumber = ctx.layers[i].updateLineNumberContext(lineNumber, ctx);
                }

                // Fill the buffer with digit chars
                int i = Math.max(ctx.lineNumberChars.length - 1, 0);
                do {
                    ctx.lineNumberChars[i--] = (char)('0' + (lineNumber % 10));
                    lineNumber /= 10;
                } while (lineNumber != 0 && i >= 0);

                // Fill the rest with spaces
                while (i >= 0) {
                    ctx.lineNumberChars[i--] = ' ';
                }

                // Fill the DG's attributes and draw
                int numX = ctx.x - ctx.editorUI.lineNumberWidth;
                if (ctx.editorUI.lineNumberMargin != null) {
                    numX += ctx.editorUI.lineNumberMargin.left;
                }
                ctx.drawGraphics.setX(numX);

                ctx.drawGraphics.setBuffer(ctx.lineNumberChars);
                ctx.drawGraphics.setForeColor(ctx.foreColor);
                ctx.drawGraphics.setBackColor(ctx.backColor);
                ctx.drawGraphics.setStrikeThroughColor(ctx.strikeThroughColor);
                ctx.drawGraphics.setUnderlineColor(ctx.underlineColor);
                ctx.drawGraphics.setWaveUnderlineColor(ctx.waveUnderlineColor);
                ctx.drawGraphics.setFont(ctx.font);
                ctx.drawGraphics.drawChars(0, ctx.lineNumberChars.length,
                    ctx.editorUI.lineNumberWidth);

                // When printing there should be an additional space between
                // line number and the text
                if (ctx.drawGraphics.getGraphics() == null) {
                    ctx.drawGraphics.setBuffer(SPACE);
                    ctx.drawGraphics.drawChars(0, 1,
                        ctx.editorUI.lineNumberDigitWidth);
                }

                ctx.drawGraphics.setX(ctx.x);
                ctx.drawGraphics.setBuffer(ctx.textArray);

            } else { // non-synced line numbering
                ctx.lineStartOffsets[ctx.lineIndex] = ctx.fragmentOffset; // store the line number
            }
        }

        ctx.lineIndex++;
    }


    /** Handle the end-of-line */
    private void handleEOL(DrawInfo ctx) {
        ctx.drawGraphics.eol(); // sign EOL to DG
        ctx.widestWidth = Math.max(ctx.widestWidth, ctx.x); // update widest width
        ctx.visualColumn = 0;
        ctx.x = ctx.startX;
        ctx.y += ctx.lineHeight;

        ctx.drawGraphics.setX(ctx.x);
        ctx.drawGraphics.setY(ctx.y);

    }


    /** Called when the current fragment starts at the offset that corresponds
    * to the update-offset.
    */
    private void updateOffsetReached(DrawInfo ctx) {
        if (ctx.drawMarkUpdate) { // update because of draw mark
            // means no-mark update yet performed
            int layersLength = ctx.layers.length;
            for (int i = 0; i < layersLength; i++) {
                DrawLayer l = ctx.layers[i];
                if (l.getName().equals(ctx.drawMark.layerName)
                        && (ctx.drawMark.isDocumentMark()
                            || ctx.editorUI == ctx.drawMark.getEditorUI())
                   ) {
                    ctx.layerActives[i] = l.isActive(ctx, ctx.drawMark);
                    int naco = l.getNextActivityChangeOffset(ctx);
                    ctx.layerActivityChangeOffsets[i] = naco;
                    if (naco > ctx.fragmentOffset && naco < ctx.layerUpdateOffset) {
                        ctx.layerUpdateOffset = naco;
                    }
                }
            }

            // Get next mark
            if (ctx.drawMarkIndex < ctx.drawMarkList.size()) {
                ctx.drawMark = (MarkFactory.DrawMark)ctx.drawMarkList.get(ctx.drawMarkIndex++);
                try {
                    ctx.drawMarkOffset = ctx.drawMark.getOffset();
                } catch (InvalidMarkException e) {
                    throw new IllegalStateException(e.toString());
                }

            } else { // no more draw marks
                ctx.drawMark = null;
                ctx.drawMarkOffset = Integer.MAX_VALUE;
            }

        } else { // update because activity-change-offset set in some layer
            ctx.layerUpdateOffset = Integer.MAX_VALUE;
            int layersLength = ctx.layers.length;
            for (int i = 0; i < layersLength; i++) {
                // Update only layers with the same offset as fragmentOffset
                int naco = ctx.layerActivityChangeOffsets[i];
                if (naco == ctx.fragmentOffset) {
                    DrawLayer l = ctx.layers[i];
                    ctx.layerActives[i] = l.isActive(ctx, null);
                    naco = l.getNextActivityChangeOffset(ctx);
                    ctx.layerActivityChangeOffsets[i] = naco;
                }

                if (naco > ctx.fragmentOffset && naco < ctx.layerUpdateOffset) {
                    ctx.layerUpdateOffset = naco;
                }
            }
        }

        // Check next update position
        if (ctx.drawMarkOffset < ctx.layerUpdateOffset) {
            ctx.drawMarkUpdate = true;
            ctx.updateOffset = ctx.drawMarkOffset;

        } else {
            ctx.drawMarkUpdate = false;
            ctx.updateOffset = ctx.layerUpdateOffset;
        }

    }

    /** Compute the length of the fragment. */
    private void computeFragmentLength(DrawInfo ctx) {
        // Compute initial fragment (of token) length
        ctx.fragmentStartIndex = ctx.fragmentOffset - ctx.bufferStartOffset;
        ctx.fragmentLength = Math.min(ctx.updateOffset - ctx.fragmentOffset,
                                      ctx.tokenLength - ctx.drawnLength);

        // Find first TAB or LF
        int stopIndex = Analyzer.findFirstTabOrLF(ctx.textArray,
                        ctx.fragmentStartIndex, ctx.fragmentLength);

        // There must be extra EOL at the end of the document
        ctx.eol = (ctx.fragmentOffset == ctx.docLen);
        ctx.tabsFragment = false;

        // Check whether there are no tabs in the fragment and possibly shrink
        // Get the first offset of the tab character or -1 if no tabs in fragment
        if (stopIndex >= 0) { // either '\t' or '\n' found
            if (stopIndex == ctx.fragmentStartIndex) { // since fragment start
                if (ctx.textArray[stopIndex] == '\t') { //
                    ctx.tabsFragment = true;
                    // Find first non-tab char
                    int ntInd = Analyzer.findFirstNonTab(ctx.textArray, ctx.fragmentStartIndex,
                                                         ctx.fragmentLength);

                    if (ntInd != -1) { // not whole fragment are tabs
                        ctx.fragmentLength = ntInd - ctx.fragmentStartIndex;
                    }


                } else { // '\n' found
                    ctx.eol = true;
                    ctx.fragmentLength = 1; // only one EOL in fragment
                }

            } else { // inside fragment start
                ctx.fragmentLength = stopIndex - ctx.fragmentStartIndex; // shrink fragment size
            }
        }
    }

    /** Compute the display width of the fragment */
    private void computeFragmentDisplayWidth(DrawInfo ctx) {
        // First go through all layers to update draw context
        // to get up-to-date fonts and colors
        if (!ctx.eol) { // handled later
            int layersLength = ctx.layers.length;
            for (int i = 0; i < layersLength; i++) {
                DrawLayer l = ctx.layers[i];
                if (ctx.layerActives[i]) {
                    ctx.layers[i].updateContext(ctx); // Let the layer to update the context
                }
            }
        }

        // Handle possible white space expansion and compute display width
        FontMetricsCache.Info fmcInfo = FontMetricsCache.getInfo(ctx.font);
        ctx.spaceWidth = (ctx.component != null)
            ? fmcInfo.getSpaceWidth(ctx.component) : ctx.editorUI.defaultSpaceWidth;

        // Compute real count of chars in fragment - can differ if tabs
        ctx.fragmentCharCount = ctx.fragmentLength;
        if (ctx.tabsFragment) { // tabs in fragment
            ctx.fragmentCharCount = Analyzer.getColumn(ctx.textArray,
                                    ctx.fragmentStartIndex, ctx.fragmentLength, ctx.tabSize, ctx.visualColumn) - ctx.visualColumn;
            ctx.fragmentWidth = ctx.fragmentCharCount * ctx.spaceWidth;

        } else if (ctx.eol) { // EOL will have the spaceWidth
            ctx.fragmentWidth = ctx.spaceWidth;

        } else { // regular fragment
            if (ctx.fragmentLength > 0) {
                if (ctx.component != null) {
                    ctx.fragmentWidth = FontMetricsCache.getFontMetrics(ctx.font, ctx.component).charsWidth(
                                            ctx.textArray, ctx.fragmentStartIndex, ctx.fragmentLength);

                } else { // non-valid component
                    ctx.fragmentWidth = ctx.fragmentLength * ctx.spaceWidth;
                }

            } else {
                ctx.fragmentWidth = 0; // empty fragment
            }
        }

    }


    /** Draw the fragment. Handle the EOL in special way
    * as it needs to care about the empty-lines.
    */
    private void drawFragment(DrawInfo ctx) {
        if (ctx.eol) { // special handling for EOL
            int layersLength = ctx.layers.length;
            boolean emptyLine = false;
            int blankWidth = ctx.fragmentWidth;

            /** Need to do one or two cycles.
            * In the first pass
            * the check is performed whether the line is empty.
            * If so all the layers that extend the empty line are
            * called to update the context and the resulting half-space
            * is drawn.
            * In the second pass all the layers that extend EOL
            * are called to update the context and the resulting
            * whitespace is drawn.
            */
            do {
                blankWidth = 0;
                if (ctx.bol) { // empty line found
                    if (!emptyLine) { // not yet processed
                        for (int i = 0; i < layersLength; i++) {
                            if (ctx.layerActives[i]) {
                                DrawLayer l = ctx.layers[i];
                                if (l.extendsEmptyLine()) {
                                    emptyLine = true; // for at least one layer
                                    l.updateContext(ctx);
                                }
                            }
                        }

                        if (emptyLine) { // count only if necessary
                            blankWidth = ctx.spaceWidth / 2; // display half of char
                        }
                    } else { // already went through the cycle once for empty line
                        emptyLine = false;
                    }
                }

                if (!emptyLine) { // EOL and currently not servicing empty line
                    boolean extendEOL = false;
                    for (int i = 0; i < layersLength; i++) {
                        if (ctx.layerActives[i]) {
                            DrawLayer l = ctx.layers[i];
                            if (l.extendsEOL()) {
                                extendEOL = true; // for at least one layer
                                l.updateContext(ctx);
                            }
                        }
                    }
                    if (extendEOL && ctx.component != null) {
                        // Disable the underlines and strikethrough for EOL extension
                        ctx.drawGraphics.setStrikeThroughColor(null);
                        ctx.drawGraphics.setUnderlineColor(null);
                        ctx.drawGraphics.setWaveUnderlineColor(null);
                        blankWidth = ctx.component.getWidth();
                    }
                }

                if (blankWidth > 0) {
                    ctx.drawGraphics.setBackColor(ctx.backColor);
                    ctx.drawGraphics.fillRect(blankWidth);
                    if (emptyLine) {
                        ctx.x += blankWidth;
                    }
                }
            } while (emptyLine);

        } else { // Draw regular fragment

            ctx.drawGraphics.setBackColor(ctx.backColor);
            ctx.drawGraphics.setForeColor(ctx.foreColor);
            ctx.drawGraphics.setStrikeThroughColor(ctx.strikeThroughColor);
            ctx.drawGraphics.setUnderlineColor(ctx.underlineColor);
            ctx.drawGraphics.setWaveUnderlineColor(ctx.waveUnderlineColor);
            ctx.drawGraphics.setFont(ctx.font);

            if (ctx.tabsFragment) {
                ctx.drawGraphics.drawTabs(ctx.fragmentStartIndex,
                    ctx.fragmentLength, ctx.fragmentCharCount, ctx.fragmentWidth);

            } else { // non-tabs
                ctx.drawGraphics.drawChars(ctx.fragmentStartIndex,
                    ctx.fragmentLength, ctx.fragmentWidth);
            }
        }
    }


    /** Check whether the target offset was reached. */
    private void checkTargetOffsetReached(DrawInfo ctx) {
        ctx.continueDraw = true;

        // Check whether at the end of the line
        if (ctx.eol
            && (ctx.targetOffset == ctx.fragmentOffset || (ctx.targetOffset == -1))
        ) {
            /** Special case for the emulating the EOL at the end of the document
            * The EOL is emulated to process the layers that extend empty-line or EOL
            */
            char ch = '\n';
            ctx.continueDraw = ctx.drawGraphics.targetOffsetReached(
                    ctx.fragmentOffset, ch, ctx.x, ctx.spaceWidth, ctx);

        // Check whether targeting all characters
        } else if (ctx.targetOffset == -1
          && ctx.fragmentLength > 0 // Not sure whether it's necessary
        ) { // When targeting all chars
            FontMetrics fm = FontMetricsCache.getFontMetrics(ctx.font, ctx.component);
            
            // Use binary search to find the right offset
            int low = -1;
            int high = ctx.fragmentLength - 1;
            
            // Cache the widths and first check whether past the end of fragment
            int lastMid = high;
            int lastWidth; // cache 
            if (ctx.tabsFragment) { // fragment contains tabs
                int spaceCount = Analyzer.getColumn(ctx.textArray,
                    ctx.fragmentStartIndex, high, ctx.tabSize, ctx.visualColumn)
                        - ctx.visualColumn;
                lastWidth = spaceCount * ctx.spaceWidth;

            } else { // no tabs inside fragment
                lastWidth = fm.charsWidth(ctx.textArray, ctx.fragmentStartIndex,  high);
            }

            int lastWidthP1; // plus one char
            if (ctx.tabsFragment) { // fragment contains tabs
                int spaceCount = Analyzer.getColumn(ctx.textArray,
                    ctx.fragmentStartIndex, ctx.fragmentLength, ctx.tabSize, ctx.visualColumn)
                        - ctx.visualColumn;
                lastWidthP1 = spaceCount * ctx.spaceWidth;

            } else { // no tabs inside fragment
                lastWidthP1 = fm.charsWidth(ctx.textArray, ctx.fragmentStartIndex,  ctx.fragmentLength);
            }

            // Test whether the end of the fragment is accepted
            ctx.continueDraw = ctx.drawGraphics.targetOffsetReached(
                ctx.fragmentOffset + high, ctx.textArray[ctx.fragmentStartIndex + high],
                ctx.x + lastWidth, lastWidthP1 - lastWidth, ctx
            );
            
            if (!ctx.continueDraw) {
                // Binary search of the first offset that returns false follows
                while (low <= high) {
                    int mid =(low + high) / 2;

                    // Compute width that will be passed as x coordinate
                    int width = 0;
                    if (mid == lastMid + 1) { // try to use cached value
                        width = lastWidthP1;

                    } else {
                        if (ctx.tabsFragment) { // fragment contains tabs
                            int spaceCount = Analyzer.getColumn(ctx.textArray,
                                ctx.fragmentStartIndex, mid, ctx.tabSize, ctx.visualColumn)
                                - ctx.visualColumn;
                            width = spaceCount * ctx.spaceWidth;

                        } else { // no tabs inside fragment
                            width = fm.charsWidth(ctx.textArray, ctx.fragmentStartIndex,  mid);
                        }
                    }

                    // Compute width plus one char and substract the previous width
                    // to get the width of the char
                    int widthP1 = 0;
                    if (mid == lastMid - 1) { // try to use cached value
                        widthP1 = lastWidth;

                    } else {
                        if (ctx.tabsFragment) { // fragment contains tabs
                            int spaceCount = Analyzer.getColumn(ctx.textArray,
                                ctx.fragmentStartIndex, mid + 1, ctx.tabSize, ctx.visualColumn)
                                - ctx.visualColumn;
                            widthP1 = spaceCount * ctx.spaceWidth;

                        } else { // no tabs inside fragment
                            widthP1 = fm.charsWidth(ctx.textArray, ctx.fragmentStartIndex,  mid + 1);
                        }
                    }

                    lastWidth = width;
                    lastWidthP1 = widthP1;
                    lastMid = mid;

                    ctx.continueDraw = ctx.drawGraphics.targetOffsetReached(
                        ctx.fragmentOffset + mid, ctx.textArray[ctx.fragmentStartIndex + mid],
                        ctx.x + width, widthP1 - width, ctx
                    );

                    if (ctx.continueDraw) {
                        low = mid + 1;
                    } else {
                        if (mid > low + 1) {
                            high = mid; // last that rejected

                        } else { // mid = low + 1 -> mid is first rejected
                            break;
                        }
                    }
                }
            }
            


        // Check whether target offset is inside current fragment
        } else if (ctx.targetOffset < ctx.fragmentOffset + ctx.fragmentLength
                   && ctx.fragmentOffset <= ctx.targetOffset
        ) {
            int curWidth;
            int prevWidth = 0;
            int i = (ctx.targetOffset - ctx.fragmentOffset);

            if (i > 0) {
                if (ctx.tabsFragment) { // fragment contains tabs
                    int spaceCount = Analyzer.getColumn(ctx.textArray,
                        ctx.fragmentStartIndex, i, ctx.tabSize, ctx.visualColumn)
                            - ctx.visualColumn;
                    prevWidth = spaceCount * ctx.spaceWidth;

                } else { // no tabs inside fragment
                    prevWidth = FontMetricsCache.getFontMetrics(ctx.font,
                        ctx.component).charsWidth(ctx.textArray, ctx.fragmentStartIndex, i);
                }
            }

            if (ctx.tabsFragment) { // fragment contains tabs
                int spaceCount = Analyzer.getColumn(ctx.textArray,
                    ctx.fragmentStartIndex, i + 1, ctx.tabSize, ctx.visualColumn)
                        - ctx.visualColumn;
                curWidth = spaceCount * ctx.spaceWidth;

            } else { // no tabs inside fragment
                curWidth = FontMetricsCache.getFontMetrics(ctx.font,
                           ctx.component).charsWidth(ctx.textArray, ctx.fragmentStartIndex, i + 1);
            }

            ctx.continueDraw = ctx.drawGraphics.targetOffsetReached(
                ctx.fragmentOffset + i, ctx.textArray[ctx.fragmentStartIndex + i],
                ctx.x + prevWidth, curWidth - prevWidth, ctx);
        }
    }

    /** Draw current fragment of the token. */
    private void drawCurrentTokenFragment(DrawInfo ctx) {
        // Fill in the draw context
        ctx.foreColor = ctx.defaultColoring.getForeColor();
        ctx.backColor = ctx.defaultColoring.getBackColor();
        ctx.font = ctx.defaultColoring.getFont();
        ctx.strikeThroughColor = null;
        ctx.underlineColor = null;
        ctx.waveUnderlineColor = null;

        if (ctx.bol) { // if we are on the line begining
            handleBOL(ctx);
        }

        // Check for status updates in planes at the begining of this fragment
        while (ctx.fragmentOffset == ctx.updateOffset) {
            updateOffsetReached(ctx); // while() because can be more marks at same pos
        }

        // Compute the length of the fragment
        computeFragmentLength(ctx);

        // Compute the display width of the fragment
        computeFragmentDisplayWidth(ctx);

        // Draw the fragment
        drawFragment(ctx);

        if (debugFragment) {
            System.err.println("DrawEngine:   FRAGMENT='" // NOI18N
                    + EditorDebug.debugChars(ctx.buffer, ctx.fragmentStartIndex, ctx.fragmentLength)
                    + "', at pos=" + ctx.fragmentOffset // NOI18N
                    + ", bol=" + ctx.bol + ", eol=" + ctx.eol // NOI18N
            );
        }

        // Check whether target-offset was reached
        if (ctx.component != null) {
            checkTargetOffsetReached(ctx);
        }

        // Move the variables to the next fragment in token
        ctx.fragmentOffset += ctx.fragmentLength;
        ctx.drawnLength += ctx.fragmentLength;
        ctx.visualColumn += ctx.fragmentCharCount;
        ctx.x += ctx.fragmentWidth;
        ctx.bol = false;

        // Update coordinates at the end of each line
        if (ctx.eol) {
            handleEOL(ctx);
            ctx.bol = true; // now at BOL
        }

        if (ctx.fragmentOffset >= ctx.endOffset && ctx.endOffset < ctx.docLen) {
            ctx.continueDraw = false;
        }
    }


    /** Draw one token. This is repeatedly called until all the tokens
    * were drawn.
    * @return true when the drawing of the next token should be done
    *   or false when the drawing should stop.
    */
    private void drawCurrentToken(DrawInfo ctx) {
        // Get the token
        if (ctx.tokenID != null) {
            ctx.tokenContextPath = ctx.syntax.getTokenContextPath();
            ctx.tokenOffset = ctx.syntax.getTokenOffset() + ctx.bufferStartOffset;
            ctx.tokenLength = ctx.syntax.getTokenLength();

            /* Check whether the token isn't totally before the area to be drawn.
             * It must cover it at least by one character. For the more complicated lexical
             * analyzers it's possible that they return several tokens that
             * will cover only the prescan area.
             */
            if (ctx.tokenOffset + ctx.tokenLength <= ctx.startOffset) {
                return;
            }

        } else { // end of drawing area
            ctx.tokenContextPath = null;
            ctx.tokenOffset = ctx.fragmentOffset;
            ctx.tokenLength = 0;
        }

        // Ask all the contexts first to possibly find out
        // the first fragment length
        if (ctx.tokenOffset <= ctx.startOffset) {
            ctx.layerUpdateOffset = Integer.MAX_VALUE;
            int layersLength = ctx.layers.length;
            for (int i = 0; i < layersLength; i++) { // update status of all layers
                DrawLayer l = ctx.layers[i];
                ctx.layerActives[i] = l.isActive(ctx, null);

                int naco = l.getNextActivityChangeOffset(ctx);
                ctx.layerActivityChangeOffsets[i] = naco;
                if (naco > ctx.fragmentOffset && naco < ctx.layerUpdateOffset) {
                    ctx.layerUpdateOffset = naco;
                }
            }
            ctx.updateOffset = Math.min(ctx.layerUpdateOffset, ctx.drawMarkOffset);
        }

        ctx.drawnLength = ctx.fragmentOffset - ctx.tokenOffset;
        ctx.fragmentLength = 0; // length of current token fragment

        if (debug) {
            System.err.println("DrawEngine: TOKEN='" // NOI18N
                    + EditorDebug.debugChars(ctx.getBuffer(), ctx.getTokenOffset()
                        - ctx.getBufferStartOffset(), ctx.getTokenLength())
                    + "', tokenID=<" + (ctx.getTokenID() == null ? "null" : ctx.tokenID.getName()) // NOI18N
                    + ">, tcp=" + ctx.getTokenContextPath() // NOI18N
                    + ", pos=" + ctx.getTokenOffset() // NOI18N
            );
        }

        // Process all the fragments of one token
        do {
            drawCurrentTokenFragment(ctx);
        } while (ctx.continueDraw && ctx.drawnLength < ctx.tokenLength);

    }

    private void graphicsSpecificUpdates(DrawInfo ctx) {
        Rectangle bounds = ctx.editorUI.getExtentBounds();
        Rectangle clip = ctx.graphics.getClipBounds();
        Insets textMargin = ctx.editorUI.getTextMargin();
        int leftMarginWidth = textMargin.left - ctx.editorUI.lineNumberWidth
                              - ctx.editorUI.textLeftMarginWidth;

        // Draw line numbers bar and all the line nummbers
        if (ctx.lineNumbering && !ctx.syncedLineNumbering) {
            Color lnBackColor = ctx.lineNumberColoring.getBackColor();
            int numY = ctx.startY;
            int lnBarX = bounds.x + leftMarginWidth;
            if (!lnBackColor.equals(ctx.defaultColoring.getBackColor()) || bounds.x > 0) {
                ctx.graphics.setColor(lnBackColor);
                ctx.graphics.fillRect(lnBarX, numY, ctx.editorUI.lineNumberWidth,
                                      ctx.lineIndex * ctx.lineHeight); // can't use dg because of height
            }

            ctx.drawGraphics.setDefaultBackColor(lnBackColor); // will paint into bar

            int lastDigitInd = Math.max(ctx.lineNumberChars.length - 1, 0);
            int numX = lnBarX;
            if (ctx.editorUI.lineNumberMargin != null) {
                numX += ctx.editorUI.lineNumberMargin.left;
            }

            ctx.bol = true; //
            for (int j = 0; j < ctx.lineIndex; j++) { // draw all line numbers

                // Init the context
                ctx.fragmentOffset = ctx.lineStartOffsets[j];
                ctx.foreColor = ctx.lineNumberColoring.getForeColor();
                ctx.backColor = lnBackColor;
                ctx.font = ctx.lineNumberColoring.getFont();
                ctx.strikeThroughColor = null;
                ctx.underlineColor = null;
                ctx.waveUnderlineColor = null;

                int lineNumber = ctx.startLineNumber + j;
                // Update line-number by layers
                int layersLength = ctx.layers.length;
                for (int i = 0; i < layersLength; i++) {
                    lineNumber = ctx.layers[i].updateLineNumberContext(lineNumber, ctx);
                }

                int i = lastDigitInd;
                // Fill in the digit chars
                do {
                    ctx.lineNumberChars[i--] = (char)('0' + (lineNumber % 10));
                    lineNumber /= 10;
                } while (lineNumber != 0 && i >= 0);
                // Fill in the spaces
                while (i >= 0) {
                    ctx.lineNumberChars[i--] = ' ';
                }

                ctx.drawGraphics.setY(numY);
                ctx.drawGraphics.setBuffer(ctx.lineNumberChars);
                ctx.drawGraphics.setForeColor(ctx.foreColor);
                ctx.drawGraphics.setBackColor(ctx.backColor);
                ctx.drawGraphics.setStrikeThroughColor(ctx.strikeThroughColor);
                ctx.drawGraphics.setUnderlineColor(ctx.underlineColor);
                ctx.drawGraphics.setWaveUnderlineColor(ctx.waveUnderlineColor);
                ctx.drawGraphics.setFont(ctx.font);

                ctx.drawGraphics.setX(lnBarX);
                ctx.drawGraphics.fillRect(ctx.editorUI.lineNumberWidth);
                ctx.drawGraphics.setX(numX);

                ctx.drawGraphics.drawChars(0, ctx.lineNumberChars.length,
                    ctx.lineNumberChars.length * ctx.editorUI.lineNumberDigitWidth);
                
                ctx.drawGraphics.setBuffer(null); // will do changes in buffer

                numY += ctx.lineHeight;
            }
        }

        // Clear margins
        ctx.graphics.setColor(ctx.defaultColoring.getBackColor());

        /** The margin is cleared only in case the line is scrolled
        * horizontally and therefore the margin is dirty because
        * of the previous text on the line. Otherwise the margin
        * is not cleared. The condition (bounds.x gt 0) gives the answer.
        */
        // Left margin
        if (leftMarginWidth > 0 && bounds.x > 0) {
            ctx.graphics.fillRect(bounds.x, ctx.startY, leftMarginWidth, ctx.lineIndex * ctx.lineHeight);
        }

        // Text left margin
        if (ctx.editorUI.textLeftMarginWidth > 0 && bounds.x > 0) {
            ctx.graphics.fillRect(bounds.x + textMargin.left - ctx.editorUI.textLeftMarginWidth,
                                  ctx.startY, ctx.editorUI.textLeftMarginWidth, ctx.lineIndex * ctx.lineHeight);
        }

        // Right margin
        if (textMargin.right > 0) {
            ctx.graphics.fillRect(bounds.x + bounds.width - textMargin.right,
                                  ctx.startY, textMargin.right, ctx.lineIndex * ctx.lineHeight);
        }

        // Top margin
        /** The margin is cleared only in case the extent bounds
        * are high enough so that the margin could be dirty
        * because of drawing too much to the right.
        */
        if (textMargin.top > 0 && clip.y < bounds.y + textMargin.top) {
            ctx.graphics.fillRect(bounds.x, bounds.y,
                                  bounds.width, textMargin.top);
        }

        // Bottom margin
        int bY = bounds.y + bounds.height - textMargin.bottom;
        if (textMargin.bottom > 0 && clip.y + clip.height > bY) {
            ctx.graphics.fillRect(bounds.x, bY,
                                  bounds.width, textMargin.bottom);
        }
    }

    /** Draw on the specified area.
     * @param drawGraphics draw graphics through which the drawing is done
     * @param editorUI extended UI to use
     * @param startOffset position from which the drawing starts.
     *  It must BOL of the first line to be drawn.
     * @param endOffset position where the drawing stops.
     *  It must be EOL of the last line to be drawn.
     * @param startX x-coordinate at which the drawing starts
     * @param startY x-coordinate at which the drawing starts
     * @param targetOffset position where the targetOffsetReached() method
     *   of drawGraphics is called. This is useful for caret update or modelToView.
     *   The Integer.MAX_VALUE can be passed to ignore that behavior. The -1 value
     *   has special meaning there so that it calls targetOffsetReached() after each
     *   character processed. This is used by viewToModel to find the position
     *   for some point.
     */
    void draw(DrawGraphics drawGraphics, EditorUI editorUI, int startOffset, int endOffset,
              int startX, int startY, int targetOffset) throws BadLocationException {
        // Some correctness tests at the begining
        if (startOffset < 0 || endOffset < 0 || startOffset > endOffset
                || startX < 0 || startY < 0
           ) {
            return;
        }

        if (debug) {
            System.err.println("DrawEngine:---------- DRAWING startOffset="
                + startOffset + ", endOffset=" + endOffset
                + "  ------------------"); // NOI18N
        }

        // Draw-context and other info
        DrawInfo ctx = new DrawInfo();
        ctx.drawGraphics = drawGraphics;
        ctx.editorUI = editorUI;
        ctx.startOffset = startOffset;
        ctx.endOffset = endOffset;
        ctx.startX = startX;
        ctx.startY = startY;
        ctx.targetOffset = targetOffset;

        synchronized (editorUI) { // lock operations manipulating draw layer chain
            ctx.doc = editorUI.getDocument();
            if (ctx.doc == null) { // no base-document available
                return;
            }

            ctx.text = DocumentUtilities.SEGMENT_CACHE.getSegment();
            ctx.syntax = ctx.doc.getFreeSyntax();
            ctx.doc.readLock();

            try {
                ctx.component = editorUI.getComponent();
                ctx.docLen = ctx.doc.getLength();

                if (ctx.startOffset > ctx.docLen || ctx.endOffset > ctx.docLen) {
                    return;
                }

                /* Correct the ending position to be at the begining
                 * of the next line. The only exception is when the
                 * endOffset is equal to docLen.
                 */
                if (ctx.endOffset < ctx.docLen) {
                    ctx.endOffset++;
                }

                // Initialize the draw-info
                initInfo(ctx);

                // Cycle through all the tokens found in the buffer
                do {
                    ctx.tokenID = ctx.syntax.nextToken();

                    // Draw the current token
                    drawCurrentToken(ctx);

                } while (ctx.continueDraw && ctx.tokenID != null);

                if (ctx.endOffset == ctx.docLen) {
                    handleEOL(ctx);
                }

                ctx.editorUI.updateVirtualWidth(ctx.widestWidth
                                                + ctx.editorUI.lineNumberWidth + 2 * ctx.editorUI.defaultSpaceWidth);

                // When drawing to graphics, the line numbers and insets will be drawn now
                if (ctx.graphics != null) {
                    graphicsSpecificUpdates(ctx);
                }

            } finally {
                ctx.drawGraphics.setBuffer(null);
                ctx.drawGraphics.finish();

                if (ctx.syntax != null) {
                    ctx.doc.releaseSyntax(ctx.syntax);
                }
                DocumentUtilities.SEGMENT_CACHE.releaseSegment(ctx.text);
                ctx.doc.readUnlock();

            }
        } // synchronized on editorUI
    }


    class DrawInfo implements DrawContext {

        // DrawContext -----------------------------------------------
        /** Current foreground color. */
        Color foreColor;

        /** Current background color. */
        Color backColor;

        /** Current background color. */
        Color underlineColor;

        /** Current background color. */
        Color waveUnderlineColor;

        /** Color of the strike-through line or null. */
        Color strikeThroughColor;

        /** Current font. */
        Font font;

        /** Starting position of the drawing. */
        int startOffset;

        /** Ending position of the drawing. */
        int endOffset;

        /** Whether we are currently at the line begining. */
        boolean bol;

        /** Whether we are currently at the line end. */
        boolean eol;

        /** Editor-UI of the component for which we are drawing. */
        EditorUI editorUI;

        /** Buffer from which the chars are being drawn. */
        char[] buffer;

        /** Starting poisition of the buffer inside the document. */
        int bufferStartOffset;

        /** Token-id of the token being drawn. */
        TokenID tokenID;

        /** Token-context-path of the token being drawn. */
        TokenContextPath tokenContextPath;

        /** Position of the token in the document */
        int tokenOffset;

        /** Length of the token's text. */
        int tokenLength;

        /** Position of the fragment of the token being drawn in the document. */
        int fragmentOffset;

        /** Length of the fragment of the token in the document. */
        int fragmentLength;

        // Other variables ---------------------------------------------

        /** Draw graphics */
        DrawGraphics drawGraphics;

        /** Target document position for the drawing or -1 if all the positions
        * are the potential targets.
        */
        int targetOffset;

        /** Text of the document being used by drawing. */
        Segment text;

        /** Char array in the text segment. */
        char[] textArray;

        /** Syntax scanning the input. */
        Syntax syntax;

        /** Component being painted */
        JTextComponent component;

        /** Document of the component. */
        BaseDocument doc;

        /** Current length of the document */
        int docLen;

        /** Current visual column. */
        int visualColumn;

        /** Current x-coordinate. */
        int x;

        /** Current y-coordinate. */
        int y;

        /** Starting x-coordinate. */
        int startX;

        /** Starting y-coordinate. */
        int startY;

        /** Height of the line being drawn. */
        int lineHeight;

        /** Default coloring of the component. */
        Coloring defaultColoring;

        /** Size of the TAB character being drawn. */
        int tabSize;

        /** Widest width of the line that was painted. */
        int widestWidth;

        /** Whether the draw should continue or not. */
        boolean continueDraw;

        /** Line number of the first painted line. */
        int startLineNumber;

        /** Index of the line being drawn. It is added
        * to the startLineNumber to form the resulting line number.
        */
        int lineIndex;

        /** Array of the start positions of all the lines drawn. */
        int[] lineStartOffsets;

        /** Characters forming the line-number. It is reused for drawing
        * all the lines.
        */
        char[] lineNumberChars;

        /** Coloring for the line-number. */
        Coloring lineNumberColoring;

        /** Graphics object. It can be null when not drawing to the component. */
        Graphics graphics;

        /** Whether line-numbers are visible and allowed by the draw-graphics. */
        boolean lineNumbering;

        /** Whether the line-numbers should be painted after each is painted.
        * By default the line-numbers are drawn as one block at the end
        * of the drawing.
        */
        boolean syncedLineNumbering;

        /** Array of the draw-layers to be used in the painting */
        DrawLayer[] layers;

        /** Whether the particular layer is currently active or not. */
        boolean[] layerActives;

        /** Next position where the layer will be asked whether it's active
        * or not.
        */
        int[] layerActivityChangeOffsets;

        /** Position where either the next draw-mark is
        * or which matches the activity-change-offset of one or more layers.
        */
        int updateOffset;

        /** Next activity-change-offset of one or more layers. */
        int layerUpdateOffset;

        /** Update of the layers because of the draw-mark is at the given position.
        * False means the update is because the activity-change-offset
        * was reached.
        */
        boolean drawMarkUpdate;

        /** List of draw marks in the painted area. */
        List drawMarkList;
        
        /** Current index in the drawMarkList */
        int drawMarkIndex;

        /** Current draw-mark */
        MarkFactory.DrawMark drawMark;

        /** Position of the current draw-mark */
        int drawMarkOffset;

        /** Length of the current token that was already drawn. */
        int drawnLength;

        /** Offset of the fragment starting character in the buffer */
        int fragmentStartIndex;

        /** Whether the fragment contains TABs only. */
        boolean tabsFragment;

        /** Width of one space character for the current context font. */
        int spaceWidth;

        /** Display width of the fragment */
        int fragmentWidth;

        /** Number of characters that the fragment stands for. It can differ
        * from fragmentLength for tabsFragment.
        */
        int fragmentCharCount;

        public Color getForeColor() {
            return foreColor;
        }

        public void setForeColor(Color foreColor) {
            this.foreColor = foreColor;
        }

        public Color getBackColor() {
            return backColor;
        }

        public void setBackColor(Color backColor) {
            this.backColor = backColor;
        }

        public Color getUnderlineColor() {
            return underlineColor;
        }

        public void setUnderlineColor(Color underlineColor) {
            this.underlineColor = underlineColor;
        }

        public Color getWaveUnderlineColor() {
            return waveUnderlineColor;
        }

        public void setWaveUnderlineColor(Color waveUnderlineColor) {
            this.waveUnderlineColor = waveUnderlineColor;
        }

        public Color getStrikeThroughColor() {
            return strikeThroughColor;
        }

        public void setStrikeThroughColor(Color strikeThroughColor) {
            this.strikeThroughColor = strikeThroughColor;
        }

        public Font getFont() {
            return font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public boolean isBOL() {
            return bol;
        }

        public boolean isEOL() {
            return eol;
        }

        public EditorUI getEditorUI() {
            return editorUI;
        }

        public char[] getBuffer() {
            return buffer;
        }

        public int getBufferStartOffset() {
            return bufferStartOffset;
        }

        public TokenID getTokenID() {
            return tokenID;
        }

        public TokenContextPath getTokenContextPath() {
            return tokenContextPath;
        }

        public int getTokenOffset() {
            return tokenOffset;
        }

        public int getTokenLength() {
            return tokenLength;
        }

        public int getFragmentOffset() {
            return fragmentOffset;
        }

        public int getFragmentLength() {
            return fragmentLength;
        }

    }

}
