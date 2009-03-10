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

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;

/** GlyphGutter is component for displaying line numbers and annotation
 * glyph icons. Component also allow to "cycle" through the annotations. It
 * means that if there is more than one annotation on the line, only one of them
 * might be visible. And clicking the special cycling button in the gutter the user
 * can cycle through the annotations.
 *
 * @author  David Konecny
 * @since 07/2001
 */

public class GlyphGutter extends JComponent implements Annotations.AnnotationsListener, Accessible {

    /** EditorUI which part this gutter is */
    private EditorUI editorUI;
    
    /** Document to which this gutter is attached*/
    private BaseDocument doc;
    
    /** Annotations manager responsible for annotations for this line */
    private Annotations annos;
    
    /** Cycling button image */
    private Image gutterButton;
    
    /** Backroung color of the gutter */
    private Color backgroundColor;
    
    /** Foreground color of the gutter. Used for drawing line numbers. */
    private Color foreColor;
    
    /** Font used for drawing line numbers */
    private Font font;
    
    /** Height of the line as it was calculated in EditorUI. */
    private int lineHeight;

    /** Flag whther the gutter was initialized or not. The painting is disabled till the
     * gutter is not initialized */
    private boolean init;
    
    /** Width of the column used for drawing line numbers. The value contains
     * also line number margins. */
    private int numberWidth;

    /** Predefined width of the glyph icons */
    private final static int glyphWidth = 16;

    /** Preddefined width of the cycling button */
    private final static int glyphButtonWidth = 9;
    
    /** Whether the line numbers are shown or not */
    private boolean showLineNumbers = true;
    
    /** Image observer used for glyph icons */
    private ImageObserver imgObserver = null;

    /** The gutter height is enlarged by number of lines which specifies this constant */
    private static final int ENLARGE_GUTTER_HEIGHT = 300;
    
    /** The hightest line number. This value is used for calculating width of the gutter */
    private int highestLineNumber = 0;
    
    /** Whether the annotation glyph can be drawn over the line numbers */
    private boolean drawOverLineNumbers = false;

    /* These two variables are used for caching of count of line annos 
     * on the line over which is the mouse caret. Just for sake of optimalization. */
    private int cachedCountOfAnnos = -1;
    private int cachedCountOfAnnosForLine = -1;

    /** Property change listener on AnnotationTypes changes */
    private PropertyChangeListener annoTypesListener;
    
    public GlyphGutter(EditorUI editorUI) {
        super();
        this.editorUI = editorUI;
        init = false;
        doc = editorUI.getDocument();
        annos = doc.getAnnotations();
        
        // Annotations class is model for this view, so the listener on changes in
        // Annotations must be added here
        annos.addAnnotationsListener(this);

        // do initialization
        init();
        update();
    }

    /* Read accessible context
     * @return - accessible context
     */
    public AccessibleContext getAccessibleContext () {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PANEL;
                }
            };
        }
        return accessibleContext;
    }

    /** Do initialization of the glyph gutter*/
    protected void init() {
        URL imageURL = null;

        try {
            // cycling button
            imageURL = new URL("nbresloc:/org/netbeans/editor/resources/glyphbutton.gif"); // NOI18N
        } catch (MalformedURLException ex) {
            if( Boolean.getBoolean( "netbeans.debug.exceptions" ) ) // NOI18N
                ex.printStackTrace();
            return;
        }
        
        if (imageURL != null)
            gutterButton = Toolkit.getDefaultToolkit().getImage(imageURL);

        setToolTipText ("");
        getAccessibleContext().setAccessibleName(LocaleSupport.getString("ACSN_Glyph_Gutter")); // NOI18N
        getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_Glyph_Gutter")); // NOI18N

        // add mouse listener for cycling button
        // TODO: clicking the line number should select whole line
        // TODO: clicking the line number abd dragging the mouse should select block of lines
        GutterMouseListener gutterMouseListener = new GutterMouseListener ();
        addMouseListener (gutterMouseListener);
        addMouseMotionListener (gutterMouseListener);

        // after the glyph icons are loaded it is necessary to repaint the gutter
        imgObserver = new ImageObserver() {
            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                if ((infoflags & ImageObserver.ALLBITS) == ImageObserver.ALLBITS) {
                    repaint();
                    return true;
                }
                return true;
            }
        };
        
        AnnotationTypes.getTypes().addPropertyChangeListener( annoTypesListener = new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent evt) {
                if (evt.getPropertyName() == AnnotationTypes.PROP_GLYPHS_OVER_LINE_NUMBERS ||
                    evt.getPropertyName() == AnnotationTypes.PROP_SHOW_GLYPH_GUTTER) {
                    update();
                }
            }
        });
        
    }
    
    /** Update colors, fonts, sizes and invalidate itself. This method is
     * called from EditorUI.update() */
    public void update() {
        Coloring lineColoring = (Coloring)editorUI.getColoringMap().get(SettingsNames.LINE_NUMBER_COLORING);
        Coloring defaultColoring = (Coloring)editorUI.getDefaultColoring();
        
        // fix for issue #16940
        // the real cause of this problem is that closed document is not garbage collected, 
        // because of *some* references (see #16072) and so any change in AnnotationTypes.PROP_*
        // properties is fired which must update this component although it is not visible anymore
        if (lineColoring == null)
            return;
        
        if (lineColoring.getBackColor() != null)
            backgroundColor = lineColoring.getBackColor();
        else
            backgroundColor = defaultColoring.getBackColor();

        if (lineColoring.getForeColor() != null)
            foreColor = lineColoring.getForeColor();
        else
            foreColor = defaultColoring.getForeColor();
        
        if (lineColoring.getFont() != null)
            font = lineColoring.getFont();
        else
            font = defaultColoring.getFont();

        lineHeight = editorUI.getLineHeight();

        showLineNumbers = editorUI.lineNumberVisibleSetting;

        drawOverLineNumbers = AnnotationTypes.getTypes().isGlyphsOverLineNumbers().booleanValue();
        
        init = true;

        // initialize the value with current number of lines
        highestLineNumber = getLineCount();
        
        repaint();
        resize();
    }
    
    protected void resize() {
        Dimension dim = new Dimension();
        dim.width = getWidthDimension();
        dim.height = getHeightDimension();
        
        // enlarge the gutter so that inserting new lines into 
        // document does not cause resizing too often
        dim.height += ENLARGE_GUTTER_HEIGHT * lineHeight;
        
        numberWidth = getLineNumberWidth();
        if (!showLineNumbers)
            numberWidth = 0;
        
        setPreferredSize(dim);

        revalidate();
    }

    /** Return number of lines in the document */
    protected int getLineCount() {
        int lineCnt;
        try {
            lineCnt = Utilities.getLineOffset(doc, doc.getLength()) + 1;
        } catch (BadLocationException e) {
            lineCnt = 1;
        }
        return lineCnt;
    }

    /** Gets number of digits in the number */
    protected int getDigitCount(int number) {
        return Integer.toString(number).length();
    }

    protected int getLineNumberWidth() {
        int newWidth = 0;
        Insets insets = editorUI.getLineNumberMargin();
        if (insets != null) {
            newWidth += insets.left + insets.right;
        }
        newWidth += getDigitCount(highestLineNumber) * editorUI.getLineNumberDigitWidth();
        return newWidth;
    }

    protected int getWidthDimension() {
        int newWidth = 0;
        
        if (annos.isGlyphColumn() || AnnotationTypes.getTypes().isShowGlyphGutter().booleanValue())
            newWidth += glyphWidth;
        
        if (annos.isGlyphButtonColumn())
            newWidth += glyphButtonWidth;

        if (showLineNumbers) {
            int lineNumberWidth = getLineNumberWidth();
            if (drawOverLineNumbers) {
                if (lineNumberWidth > newWidth)
                    newWidth = lineNumberWidth;
            } else
                newWidth += lineNumberWidth;
        }
        
        return newWidth;
    }
    
    protected int getHeightDimension() {
        JComponent comp = editorUI.getComponent();
        if (comp == null)
            return 0;
        return highestLineNumber * lineHeight + (int)comp.getSize().getHeight();
    }
    
    /** Paint the gutter itself */
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        
        // if the gutter was not initialized yet, skip the painting
        if (!init)
            return;
        
        Rectangle drawHere = g.getClipBounds();

        // Fill clipping area with dirty brown/orange.
        g.setColor(backgroundColor);
        g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

        g.setFont(font); 
        g.setColor(foreColor);

        FontMetrics fm = FontMetricsCache.getFontMetrics(font, this);
        int rightMargin = 0;
        Insets margin = editorUI.getLineNumberMargin();
        if (margin != null)
            rightMargin = margin.right;
        
        // calculate the first line which must be drawn
        int line = (int)( (float)drawHere.y / (float)lineHeight );
        if (line > 0)
            line--;

        // calculate the Y of the first line
        int y = line * lineHeight;

        int lineCount = Integer.MAX_VALUE;

        if (showLineNumbers) {
            lineCount = getLineCount();
            int lastLine = (int)( (float)(drawHere.y+drawHere.height) / (float)lineHeight )+1;
            if (lastLine > highestLineNumber) {
                int prevHighest = highestLineNumber;
                highestLineNumber = lastLine;
                if (getDigitCount(highestLineNumber) > getDigitCount(prevHighest)) {
                    resize();
                    return;
                }
            }
        }
        
        // find the nearest visible line with an annotation
        int lineWithAnno = annos.getNextLineWithAnnotation(line);
        
        // draw liune numbers and annotations while we are in visible area
        // "+(lineHeight/2)" means to don't draw less than half of the line number
        while ( (y+(lineHeight/2)) <= (drawHere.y + drawHere.height) )
        {
            // draw line numbers if they are turned on
            if (showLineNumbers && ( (!drawOverLineNumbers) || (drawOverLineNumbers && line != lineWithAnno) ) ) {
                if (line < lineCount) {
                    int lineNumberWidth = fm.stringWidth(""+(line+1));
                    g.drawString(""+(line+1), numberWidth-lineNumberWidth-rightMargin, y+editorUI.getLineAscent());
                }
            }
            
            // draw anotation if we get to the line with some annotation
            if (line == lineWithAnno) {
                
                int count = annos.getNumberOfAnnotations(line);
                AnnotationDesc anno = annos.getActiveAnnotation(line);

                int xPos = numberWidth;
                if (drawOverLineNumbers) {
                    xPos = getWidth() - glyphWidth;
                    if (count > 1)
                        xPos -= glyphButtonWidth;
                }
                
                if (anno != null) {
                    // draw the glyph only when the annotation type has its own icon (no the default one)
                    // or in case there is more than one annotations on the line
                    if ( ! (count == 1 && anno.isDefaultGlyph()) ) {
                        if (anno.getGlyph() != null && prepareImage(anno.getGlyph(), imgObserver))
                            g.drawImage(anno.getGlyph(), xPos, y + (lineHeight-anno.getGlyph().getHeight(null)) / 2 + 1, null);
                    }
                }
                
                // draw cycling button if there is more than one annotations on the line
                if (count > 1)
                    if (anno.getGlyph() != null && prepareImage(gutterButton, imgObserver) && prepareImage(anno.getGlyph(), imgObserver))
                        g.drawImage(gutterButton, xPos+glyphWidth, y + (lineHeight-anno.getGlyph().getHeight(null)) / 2, null);

                // update the value with next line with some anntoation
                lineWithAnno = annos.getNextLineWithAnnotation(line+1);
            }
            
            y += lineHeight;
            line++;
        }
    }

    /** Data for the line has changed and the line must be redraw. */
    public void changedLine(int line) {
        
        if (!init)
            return;

        // reset cache if there was some change
        cachedCountOfAnnos = -1;
        
        // redraw also lines around - three lines will be redrawn
        if (line > 0)
            line--;
        int y = line * lineHeight;
        
        repaint(0, y, (int)getSize().getWidth(), 3*lineHeight);
        checkSize();
    }

    /** Repaint whole gutter.*/
    public void changedAll() {

        if (!init)
            return;

        // reset cache if there was some change
        cachedCountOfAnnos = -1;
        
        int lineCnt;
        try {
            lineCnt = Utilities.getLineOffset(doc, doc.getLength()) + 1;
        } catch (BadLocationException e) {
            lineCnt = 1;
        }

        repaint();
        checkSize();
    }

    /** Check whether it is not necessary to resize the gutter */
    protected void checkSize() {
        int count = getLineCount();
        if (count > highestLineNumber) {
            highestLineNumber = count;
        }
        Dimension dim = getPreferredSize();
        if (getWidthDimension() > dim.width ||
            getHeightDimension() > dim.height) {
            resize();
        }
        
    }

    /** Get tooltip text for the mouse position */
    // TODO: does not work for asynchronous tooltip texts
    public String getToolTipText (MouseEvent e) {
        int line = (int)( (float)e.getY() / (float)lineHeight );
        if (annos.getNumberOfAnnotations(line) == 0)
            return null;
        if (isMouseOverCycleButton(e) && annos.getNumberOfAnnotations(line) > 1) {
            return java.text.MessageFormat.format (
                LocaleSupport.getString ("cycling-glyph_tooltip"), //NOI18N
                new Object[] { new Integer (annos.getNumberOfAnnotations(line)) });
        }
        else if (isMouseOverGlyph(e)) {
            return annos.getActiveAnnotation(line).getShortDescription();
        }
        else
            return null;
    }

    /** Count the X position of the glyph on the line. */
    private int getXPosOfGlyph(int line) {
        int xPos = numberWidth;
        if (drawOverLineNumbers) {
            xPos = getWidth() - glyphWidth;
            if (cachedCountOfAnnos == -1 || cachedCountOfAnnosForLine != line) {
                cachedCountOfAnnos = annos.getNumberOfAnnotations(line);
                cachedCountOfAnnosForLine = line;
            }
            if (cachedCountOfAnnos > 1)
                xPos -= glyphButtonWidth;
        }
        return xPos;
    }

    /** Check whether the mouse is over some glyph icon or not */
    private boolean isMouseOverGlyph(MouseEvent e) {
        int line = (int)( (float)e.getY() / (float)lineHeight );
        if (e.getX() >= getXPosOfGlyph(line) && e.getX() <= getXPosOfGlyph(line)+glyphWidth)
            return true;
        else
            return false;
    }
    
    /** Check whether the mouse is over the cycling button or not */
    private boolean isMouseOverCycleButton(MouseEvent e) {
        int line = (int)( (float)e.getY() / (float)lineHeight );
        if (e.getX() >= getXPosOfGlyph(line)+glyphWidth && e.getX() <= getXPosOfGlyph(line)+glyphWidth+glyphButtonWidth)
            return true;
        else
            return false;
    }

    class GutterMouseListener extends MouseAdapter implements MouseMotionListener {
        
        /** start line of the dragging. */
        private int dragStartLine;
        /** end line of the dragging. */
        private int dragEndLine;
        /** end line of last selection. */
        private int currentEndLine;
        /** If true, the selection goes forwards. */
        private boolean selectForward;

        public void mouseClicked(MouseEvent e) {
            // cycling button was clicked by left mouse button
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                if (isMouseOverCycleButton(e)) {
                    int line = (int)( (float)e.getY() / (float)lineHeight );
                    annos.activateNextAnnotation(line);
                } else {
                    Action a = ImplementationProvider.getDefault().getToggleBreakpointAction();
                    if (a != null && a.isEnabled()) {
                        int line = (int)( (float)e.getY() / (float)lineHeight );
                        int currentLine = -1;
                        try {
                            currentLine = Utilities.getLineOffset(doc, editorUI.getComponent().getCaret().getDot());
                        } catch (BadLocationException ex) {
                            // XXX what should this do?
                        }
                        if (line != currentLine) {
                            int offset = Utilities.getRowStartFromLineOffset(doc, line);
                            JumpList.checkAddEntry();
                            editorUI.getComponent().getCaret().setDot(offset);
                        }
                        a.actionPerformed(new ActionEvent(editorUI.getComponent(), 0, ""));
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
            }
        }

        private void showPopup(MouseEvent e) {
            // annotation glyph was clicked by right mouse button
            if (e.isPopupTrigger()) {
                int line = (int)( (float)e.getY() / (float)lineHeight );
                int offset;
                if (annos.getActiveAnnotation(line) != null)
                    offset = annos.getActiveAnnotation(line).getOffset();
                else
                    offset = Utilities.getRowStartFromLineOffset(doc, line);
                if (editorUI.getComponent().getCaret().getDot() != offset)
                    JumpList.checkAddEntry();
                editorUI.getComponent().getCaret().setDot(offset);
                JPopupMenu pm = annos.createPopupMenu(Utilities.getKit(editorUI.getComponent()), line);
                if (pm != null) {
                    pm.show(GlyphGutter.this, e.getX(), e.getY());
                }
                pm.addPopupMenuListener( new PopupMenuListener() {
                        public void popupMenuCanceled(PopupMenuEvent e2) {
                            editorUI.getComponent().requestFocus();
                        }
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e2) {
                            editorUI.getComponent().requestFocus();
                        }
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e2) {
                        }
                    });
            }
        }
        
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        
        public void mousePressed (MouseEvent e) {
            showPopup(e);
            // "click gutter selects line" functionality was disabled
//            // only react when it is not a cycling button
//            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
//                if (! isMouseOverCycleButton(e)) {
//                    dragStartLine = (int)( (float)e.getY() / (float)lineHeight );
//                    updateSelection (true);
//                }
//            }
        }
        
        public void mouseDragged(MouseEvent e) {
            // "click gutter selects line" functionality was disabled
//            dragEndLine = (int)( (float)e.getY() / (float)lineHeight );
//            updateSelection (false);
        }
        
        public void mouseMoved(MouseEvent e) {}
        
        /** Updates the selection */
        private void updateSelection (boolean newSelection) {
            javax.swing.text.JTextComponent comp = Utilities.getLastActiveComponent ();
            try {
                if (newSelection) {
                    selectForward = true;
                    // try to get the startOffset. In case of -1 it is most
                    // likely the end of the document
                    int rowStart = Utilities.getRowStartFromLineOffset (doc, dragStartLine);
                    if (rowStart < 0) {
                        rowStart = Utilities.getRowStart (doc, doc.getLength ());
                        dragStartLine = Utilities.getLineOffset (doc, rowStart);
                    }
                    comp.setCaretPosition (rowStart);
                    int offSet = Utilities.getRowEnd (doc, rowStart);
                    if (offSet < doc.getLength()) {
                        offSet = offSet + 1;
                    }
                    comp.moveCaretPosition (offSet);
                    currentEndLine = dragEndLine = dragStartLine;
                } else {
                    if (currentEndLine == dragEndLine) return;
                    // select backwards
                    if (dragEndLine < dragStartLine) {
                        if (selectForward) {
                            // selection start should be at start of (dragLine + 1)
                            int offSet = Utilities.getRowStartFromLineOffset (doc, dragStartLine + 1);
                            if (offSet < 0) {
                                offSet = Utilities.getRowEnd (doc, Utilities.getRowStartFromLineOffset (doc, dragStartLine));
                            }
                            comp.setCaretPosition (offSet);
                            selectForward = false;
                        }
                        int rowStart = Utilities.getRowStartFromLineOffset (doc, dragEndLine);
                        if (rowStart < 0) rowStart = 0;
                        comp.moveCaretPosition (rowStart);
                    }
                    // select forwards
                    else {
                        if (! selectForward) {
                            // select start should be at dragStartLine
                            comp.setCaretPosition (Utilities.getRowStartFromLineOffset (doc, dragStartLine));
                            selectForward = true;
                        }
                        // try to get the begin of (endLine + 1)
                        int offSet = Utilities.getRowStartFromLineOffset (doc, dragEndLine + 1);;
                        // for last line or more -1 is returned, so set to docLength...
                        if (offSet < 0) {
                            offSet = doc.getLength ();
                        }
                        comp.moveCaretPosition (offSet);
                    }
                }
                currentEndLine = dragEndLine;
            } catch (BadLocationException ble) {
                System.err.println(ble);
            }
        }
    }
    
}
