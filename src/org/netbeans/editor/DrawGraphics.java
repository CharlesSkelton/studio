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

import javax.swing.text.JTextComponent;
import java.awt.*;

/** Draw graphics functions as abstraction over various kinds of drawing. It's used
* for drawing into classic graphics, printing and measuring.
* Generally there are only the setters for some properties because 
* the draw-engine doesn't retrieve the values that it previously
* set.
*
* @author Miloslav Metelka
* @version 1.00
*/
interface DrawGraphics {

    /** Set foreground color */
    public void setForeColor(Color foreColor);

    /** Set background color */
    public void setBackColor(Color backColor);

    /** Inform the draw-graphics about the current
    * background color of the component.
    */
    public void setDefaultBackColor(Color defaultBackColor);
    
    public void setStrikeThroughColor(Color strikeThroughColor);
    
    public void setUnderlineColor(Color underlineColor);
    
    public void setWaveUnderlineColor(Color waveUnderlineColor);

    /** Set current font */
    public void setFont(Font font);

    /** Set the current x-coordinate */
    public void setX(int x);

    /** Set the current y-coordinate */
    public void setY(int y);

    /** Set the height of the line. */
    public void setLineHeight(int lineHeight);

    /** Set the ascent of the line. */
    public void setLineAscent(int lineAscent);

    /** Get the AWT-graphics to determine whether this draws to a graphics.
    * This is useful for fast line numbering and others.
    */
    public Graphics getGraphics();

    /** Whether draw graphics supports displaying of line numbers.
    * If not line number displaying is not done.
    */
    public boolean supportsLineNumbers();

    /** Initialize this draw graphics before drawing */
    public void init(DrawContext ctx);

    /** Called when whole drawing ends. Can be used to deallocate
    * some resources etc.
    */
    public void finish();

    /** Fill rectangle at the current [x, y] with the current
    * background color.
    * @param width width of the rectangle to fill in points. The current x-coordinate
    *  must be increased by width automatically.
    */
    public void fillRect(int width);

    /** Draw characters from the specified offset in the buffer
    * @param offset offset in the buffer for drawn text; if the text contains
    *   tabs, then offset is set to -1 and length contains the count
    *   of the space characters that correspond to the expanded tabs
    * @param length length of the text being drawn
    * @param width width of the text being drawn in points. The current
    *   x-coordinate must be increased by width automatically.
    */
    public void drawChars(int offset, int length, int width);

    /** Draw the expanded tab characters.
    * @param offset offset in the buffer where the tab characters start.
    * @param length number of the tab characters
    * @param spaceCount number of spaces that replace the tabs
    * @param width width of the spaces in points. The current x-coordinate
    *   must be increased by width automatically.
    */
    public void drawTabs(int offset, int length, int spaceCount, int width);

    /** Set character buffer from which the characters are drawn. */
    public void setBuffer(char[] buffer);

    /** This method is called to notify this draw graphics in response
    * from targetPos parameter passed to draw().
    * @param offset position that was reached during the drawing.
    * @param ch character at offset
    * @param charWidth visual width of the character ch
    * @param ctx current draw context containing 
    * @return whether the drawing should continue or not. If it returns
    *   false it's guaranteed that this method will not be called again
    *   and the whole draw() method will be stopped. <BR>The only
    *   exception is when the -1 is used as the target offset
    *   when draw() is called which means that every offset
    *   is a potential target offset and must be checked.
    *   In this case the binary search is used when finding
    *   the target offset inside painted fragment. That greatly
    *   improves performance for long fragments because
    *   the font metrics measurements are relatively expensive.
    */
    public boolean targetOffsetReached(int offset, char ch, int x,
                                       int charWidth, DrawContext ctx);

    /** EOL encountered and should be handled. */
    public void eol();


    /** Abstract draw-graphics that maintains a fg and bg color, font,
    * current x and y coordinates.
    */
    static abstract class AbstractDG implements DrawGraphics {

        /** Current foreground color */
        Color foreColor;

        /** Current background color */
        Color backColor;

        /** Default background color */
        Color defaultBackColor;

        /** Current font */
        Font font;

        /** Character buffer from which the data are drawn */
        char[] buffer;

        /** Current x-coordinate */
        int x;

        /** Current y-coordinate */
        int y;

        /** Height of the line being drawn */
        int lineHeight;

        /** Ascent of the line being drawn */
        int lineAscent;

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

        public Color getDefaultBackColor() {
            return defaultBackColor;
        }

        public void setDefaultBackColor(Color defaultBackColor) {
            this.defaultBackColor = defaultBackColor;
        }

        public Font getFont() {
            return font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getLineHeight() {
            return lineHeight;
        }

        public void setLineHeight(int lineHeight) {
            this.lineHeight = lineHeight;
        }

        public int getLineAscent() {
            return lineAscent;
        }

        public void setLineAscent(int lineAscent) {
            this.lineAscent = lineAscent;
        }

        public char[] getBuffer() {
            return buffer;
        }

        public void setBuffer(char[] buffer) {
            this.buffer = buffer;
        }

        public void drawChars(int offset, int length, int width) {
            x += width;
        }

        public void drawTabs(int offset, int length, int spaceCount, int width) {
            x += width;
        }

        public void setStrikeThroughColor(Color strikeThroughColor) {
        }
        
        public void setUnderlineColor(Color underlineColor) {
        }
        
        public void setWaveUnderlineColor(Color waveUnderlineColor) {
        }
        
    }

    static class SimpleDG extends AbstractDG {

        public Graphics getGraphics() {
            return null;
        }

        public boolean supportsLineNumbers() {
            return false;
        }

        public void init(DrawContext ctx) {
        }

        public void finish() {
        }

        public void fillRect(int width) {
        }

        public boolean targetOffsetReached(int offset, char ch, int x,
                                           int charWidth, DrawContext ctx) {
            return true; // shouldn't reach this place
        }

        public void eol() {
        }

    }

    /** Implementation of DrawGraphics to delegate to some Graphics.
    * It optimizes the drawing by joining together the pieces of
    * the text drawn with the same font and fg/bg color.
    */
    static final class GraphicsDG extends SimpleDG {

        private Graphics graphics;

        /** Current graphics color */
        private Color gColor;

        /** Current graphics font */
        private Font gFont;

        /** Start of the chars that were not drawn yet. It can be -1
        * to indicate the buffered characters were just flushed.
        */
        private int startOffset = -1;

        /** End of the chars that were not drawn yet */
        private int endOffset;

        /** X coordinate where the drawing of chars should occur */
        private int startX;

        /** Y coordinate where the drawing of chars should occur */
        private int startY;

        private int width;

        private Color strikeThroughColor;

        private Color underlineColor;
        
        private Color waveUnderlineColor;

        /** Alpha used for drawing the glyphs on the background */
        private AlphaComposite alpha = null;

        /** Access to annotations for this document which will be
         * drawn on the background */
        private Annotations annos = null;
        

        GraphicsDG(Graphics graphics) {
            this.graphics = graphics;
        }

        public void setForeColor(Color foreColor) {
            if (!foreColor.equals(this.foreColor)) {
                flush();
                this.foreColor = foreColor;
            }
        }

        public void setBackColor(Color backColor) {
            if (!backColor.equals(this.backColor)) {
                flush();
                this.backColor = backColor;
            }
        }

        public void setStrikeThroughColor(Color strikeThroughColor) {
            if ((strikeThroughColor != this.strikeThroughColor)
                && (strikeThroughColor == null
                    || !strikeThroughColor.equals(this.strikeThroughColor))
            ) {
                flush();
                this.strikeThroughColor = strikeThroughColor;
            }
        }

        public void setUnderlineColor(Color underlineColor) {
            if ((underlineColor != this.underlineColor)
                && (underlineColor == null
                    || !underlineColor.equals(this.underlineColor))
            ) {
                flush();
                this.underlineColor = underlineColor;
            }
        }

        public void setWaveUnderlineColor(Color waveUnderlineColor) {
            if ((waveUnderlineColor != this.waveUnderlineColor)
                && (waveUnderlineColor == null
                    || !waveUnderlineColor.equals(this.waveUnderlineColor))
            ) {
                flush();
                this.waveUnderlineColor = waveUnderlineColor;
            }
        }

        public void setFont(Font font) {
            if (!font.equals(this.font)) {
                flush();
                this.font = font;
            }
        }

        public void setX(int x) {
            if (x != this.x) {
                flush();
                this.x = x;
            }
        }

        public void setY(int y) {
            if (y != this.y) {
                flush();
                this.y = y;
            }
        }

        public void init(DrawContext ctx) {
            JTextComponent c = ctx.getEditorUI().getComponent();
            gColor = graphics.getColor();
            gFont = graphics.getFont();
            // initialize reference to annotations
            annos = ctx.getEditorUI().getDocument().getAnnotations();
        }

        public void finish() {
            flush();
        }

        private void flush() {
            if (startOffset < 0) {
                return;
            }

            if (startOffset == endOffset) {
                startOffset = -1;
                return;
            }

            // First possibly fill the rectangle
            fillRectImpl(startX, startY, x - startX);

            if (AnnotationTypes.getTypes().isBackgroundDrawing().booleanValue()) {
                
                if (alpha == null)
                    alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, AnnotationTypes.getTypes().getBackgroundGlyphAlpha().intValue() / 100f);
                
                AnnotationDesc[] annosArray = annos.getPasiveAnnotations( (int)( (float)startY / (float)lineHeight ));
                int glyphX=2;
                if (annosArray != null) {
                    Graphics2D g2d = (Graphics2D) graphics;

                    Shape shape = graphics.getClip();

                    // set alpha composite
                    Composite origin = g2d.getComposite();
                    g2d.setComposite(alpha);

                    // clip the drawing area
                    Rectangle r = new Rectangle(startX, startY, x - startX, lineHeight);
                    r = r.intersection(shape.getBounds());
                    graphics.setClip(r);

                    for (int i=0; i < annosArray.length; i++) {
                        g2d.drawImage(annosArray[i].getGlyph(), glyphX, startY, null);
                        glyphX += annosArray[i].getGlyph().getWidth(null)+1;
                    }

                    // restore original clip region
                    graphics.setClip(shape);

                    // restore original ocmposite
                    g2d.setComposite(origin);
                }
            }
            
            // Check whether the graphics uses right color
            if (foreColor != gColor) {
                graphics.setColor(foreColor);
                gColor = foreColor;
            }
            // Check whether the graphics uses right font
            if (font != gFont) {
                graphics.setFont(font);
                gFont = font;
            }

            graphics.drawChars(buffer, startOffset, endOffset - startOffset,
                               startX, startY + lineAscent);

            if (strikeThroughColor != null) { // draw strike-through
                FontMetricsCache.Info fmcInfo = FontMetricsCache.getInfo(font);
                if (strikeThroughColor != gColor) {
                    graphics.setColor(strikeThroughColor);
                    gColor = strikeThroughColor;
                }
                graphics.fillRect(startX,
                                  startY + (int)(fmcInfo.getStrikethroughOffset(graphics) + lineAscent + 0.5),
                                  x - startX,
                                  (int)(fmcInfo.getStrikethroughThickness(graphics) + 0.5)
                                 );
            }

            if (waveUnderlineColor != null) { // draw wave underline
                FontMetricsCache.Info fmcInfo = FontMetricsCache.getInfo(font);
                if (waveUnderlineColor != gColor) {
                    graphics.setColor(waveUnderlineColor);
                    gColor = waveUnderlineColor;
                }

                int[] wf = {0, +1, 0, -1};
                int x0 = startX;
                // Add one to be right below the baseline
                int y0 = (int)(startY + fmcInfo.getUnderlineOffset(graphics) + lineAscent + 1 + 0.5);
                while (x0 <= x) {
                    graphics.drawLine(x0, y0 + wf[x0 % 4],
                                      x0 + 1, y0 + wf[(x0 + 1) % 4]);
                    x0++;
                }
            }

            if (underlineColor != null) { // draw underline
                FontMetricsCache.Info fmcInfo = FontMetricsCache.getInfo(font);
                if (underlineColor != gColor) {
                    graphics.setColor(underlineColor);
                    gColor = underlineColor;
                }
                // Add one pixel to the underline offset
                graphics.fillRect(startX,
                                  startY + (int)(fmcInfo.getUnderlineOffset(graphics) + lineAscent + 1.5),
                                  x - startX,
                                  (int)(fmcInfo.getUnderlineThickness(graphics) + 0.5)
                                 );

            }

            startOffset = -1; // signal no characters to draw
        }

        public Graphics getGraphics() {
            return graphics;
        }

        public boolean supportsLineNumbers() {
            return true;
        }

        public void fillRect(int width) {
            fillRectImpl(x, y, width);
            x += width;
        }

        private void fillRectImpl(int rx, int ry, int width) {
            if (width > 0) { // only for non-zero width
                // only fill for different color than current background
                if (!backColor.equals(defaultBackColor)) {
                    if (backColor != gColor) {
                        graphics.setColor(backColor);
                        gColor = backColor;
                    }

                    graphics.fillRect(rx, ry, width, lineHeight);
                }

            }
        }


        public void drawChars(int offset, int length, int width) {
            if (length >= 0) {
                if (startOffset < 0) { // no token yet
                    startOffset = offset;
                    endOffset = offset + length;
                    this.startX = x;
                    this.startY = y;
                    this.width = width;

                } else { // already token before
                    endOffset += length;
                }
            }

            x += width;
        }

        public void drawTabs(int offset, int length, int spaceCount, int width) {
            if (width > 0) {
                flush();
                fillRectImpl(x, y, width);
                x += width;
            }
        }

        public void setBuffer(char[] buffer) {
            flush();
            this.buffer = buffer;
            startOffset = -1;
        }

        public void eol() {
            flush();
        }

    }

    static final class PrintDG extends SimpleDG {

        PrintContainer container;

        /** Whether there were some paints already on the line */
        boolean lineInited;

        /** Construct the new print graphics
        * @param container print container to which the tokens
        *   are added.
        */
        public PrintDG(PrintContainer container) {
            this.container = container;
        }

        public boolean supportsLineNumbers() {
            return true;
        }

        public void drawChars(int offset, int length, int width) {
            if (length > 0) {
                char[] chars = new char[length];
                System.arraycopy(buffer, offset, chars, 0, length);
                container.add(chars, font, foreColor, backColor);
            }
        }

        private void printSpaces(int spaceCount) {
            char[] chars = new char[spaceCount];
            System.arraycopy(Analyzer.getSpacesBuffer(spaceCount), 0, chars, 0, spaceCount);
            container.add(chars, font, foreColor, backColor);
        }

        public void drawTabs(int offset, int length, int spaceCount, int width) {
            printSpaces(spaceCount);
        }

        public void eol() {
            if (!lineInited && container.initEmptyLines()) {
                printSpaces(1);
            }
            container.eol();
            lineInited = false; // signal that the next line is not inited yet
        }

    }

}
