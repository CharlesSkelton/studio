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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
* Editor UI for the component. All the additional UI features
* like advanced scrolling, info about fonts, abbreviations,
* keyword matching are based on this class.
*
* @author Miloslav Metelka
* @version 1.00
*/
public class EditorUI implements ChangeListener, PropertyChangeListener, SettingsChangeListener {

    public static final String OVERWRITE_MODE_PROPERTY = "overwriteMode"; // NOI18N

    public static final String COMPONENT_PROPERTY = "component"; // NOI18N

    /** Default scrolling type is used for the standard
    * setDot() call. If the area is on the screen, it
    * jumps to it, otherwise it centers the requested area
    * vertically in the middle of the window and it uses
    * smallest covering on the right side.
    */
    public static final int SCROLL_DEFAULT = 0;

    /** Scrolling type used for regular caret moves.
    * The scrollJump is used when the caret requests area outside the screen.
    */
    public static final int SCROLL_MOVE = 1;

    /** Scrolling type where the smallest covering
    * for the requested rectangle is used. It's useful
    * for going to the end of the line for example.
    */
    public static final int SCROLL_SMALLEST = 2;

    /** Scrolling type for find operations, that can
    * request additional configurable area in each
    * direction, so the context around is visible too.
    */
    public static final int SCROLL_FIND = 3;


    private static final Insets NULL_INSETS = new Insets(0, 0, 0, 0);

    private static final Dimension NULL_DIMENSION = new Dimension(0, 0);

    private static final int STYLE_CNT = 4;
    
    private static final boolean debugUpdateLineHeight
    = Boolean.getBoolean("netbeans.debug.editor.updateLineHeight");

    /** Map holding the coloring maps for the different languages.
     * It helps to minimize the amount of the coloring maps
     * and also save the time necessary for their creation.
     */
    private static final HashMap sharedColoringMaps = new HashMap(57);
    private static final SettingsChangeListener clearingListener
        = new SettingsChangeListener() {
            public void settingsChange(SettingsChangeEvent evt) {
                // Fired when the Settings are locked
                sharedColoringMaps.clear();
            }
        };


    static {
        Settings.addSettingsChangeListener( clearingListener );
    }
   
    public void clearSharedColoringMaps()
    {
        sharedColoringMaps.clear();
        coloringMap=null;
    }

    /** Component this extended UI is related to. */
    private JTextComponent component;

    private JComponent extComponent;

    /** Property change support for firing property changes */
    PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /** Document for the case ext ui is constructed without the component */
    private BaseDocument printDoc;

    /** Draw layer chain */
    private DrawLayerList drawLayerList = new DrawLayerList();

    /** Map holding the [name, coloring] pairs */
    private Map coloringMap;

    /** Character (or better line) height. Particular view can use a different
    * character height however most views will probably use this one.
    */
    private int lineHeight = 1; // prevent possible division by zero

    private float lineHeightCorrection = 1.0f;

    /** Ascent of the line which is maximum ascent of all the fonts used. */
    private int lineAscent;

    /** Width of the space in the default coloring's font */
    int defaultSpaceWidth = 1;

    /** Flag to initialize fonts */
    private boolean fontsInited;
    /** First paint after preferenceChanged after fonts were inited. */
    private boolean fontsInitedPreferenceChanged;

    /** Should the search words be colored? */
    boolean highlightSearch;

    /** Enable displaying line numbers. Both this flag and <tt>lineNumberVisibleSetting</tt>
    * must be true to have the line numbers visible in the window. This flag is false
    * by default. It's turned on automatically if the getExtComponent is called.
    */
    boolean lineNumberEnabled;

    /** This flag corresponds to the LINE_NUMBER_VISIBLE setting. */
    boolean lineNumberVisibleSetting;

    /** Whether to show line numbers or not. This flag is obtained using bitwise AND
    * operation on lineNumberEnabled flag and lineNumberVisibleSetting flag.
    */
    boolean lineNumberVisible;

    /** Line number total width with indentation. It includes left and right
    * line-number margins and lineNumberDigitWidth * lineNumberMaxDigitCount.
    */
    int lineNumberWidth;

    /** Width of one digit used for line numbering. It's based
    * on the information from the line coloring.
    */
    int lineNumberDigitWidth;

    /** Current maximum count of digits in line number */
    int lineNumberMaxDigitCount;

    /** Margin on the left and right side of the line number */
    Insets lineNumberMargin;

    /** This is the size of the editor as component while the real size
    * of the lines edited can be lower. The reason why to use this
    * virtual size is that each resizing of the component means
    * revalidating and therefore repainting of the whole component.
    */
    Rectangle virtualSize = new Rectangle();

    //  /** This is the increment by which the size of the component
    //  * is increased.
    //  */
    //  Rectangle virtualSizeIncrement = new Rectangle(); !!!

    /** Margin between the line-number bar and the text. */
    int textLeftMarginWidth;

    /** This is the full margin around the text. The left margin
    * is an addition of component's margin and lineNumberWidth 
    * and textLeftMarginWidth.
    */
    Insets textMargin = NULL_INSETS;

    /** How much columns/lines to add when the scroll is performed
    * so that the component is not scrolled so often.
    * Negative number means portion of the extent width/height
    */
    Insets scrollJumpInsets;

    /** How much columns/lines to add when the scroll is performed
    * so that the component is not scrolled so often.
    * Negative number means portion of the extent width/height
    */
    Insets scrollFindInsets;

    /** Flag saying whether either the width or height in virtualSize
    * were updated.
    */
    boolean virtualSizeUpdated;

    /** Listener to changes in settings */
    private PropertyChangeListener settingsListener;

    /** EditorUI properties */
    Hashtable props = new Hashtable(11);

    boolean textLimitLineVisible;

    Color textLimitLineColor;

    int textLimitWidth;

    private Rectangle lastExtentBounds = new Rectangle();

    private Dimension componentSizeIncrement = new Dimension();

    private Abbrev abbrev;

    private WordMatch wordMatch;

    private Object componentLock;

    /** Status bar */
    StatusBar statusBar;

    private FocusAdapter focusL;

    Map renderingHints;

    /** Glyph gutter used for drawing of annotation glyph icons. */
    private GlyphGutter glyphGutter = null;

    /** The line numbers can be shown in glyph gutter and therefore it is necessary 
     * to disable drawing of lines here. During the printing on the the other hand, line 
     * numbers must be visible. */
    private boolean disableLineNumbers = true;

    /** Left right corner of the JScrollPane */
    private JPanel glyphCorner;
    
    /** Construct extended UI for the use with a text component */
    public EditorUI() {
        Settings.addSettingsChangeListener(this);

        focusL = new FocusAdapter() {
                     public void focusGained(FocusEvent evt) {
                         Registry.activate(getComponent());
                         /* Fix of #25475 - copyAction's enabled flag
                          * must be updated on focus change
                          */
                         stateChanged(null);
                     }
                 };

    }

    /** Construct extended UI for printing the given document */
    public EditorUI(BaseDocument printDoc) {
        this.printDoc = printDoc;

        settingsChange(null);

        setLineNumberEnabled(true);

        updateLineNumberWidth(0);

        drawLayerList.add(printDoc.getDrawLayerList());
    }

    /** Gets the coloring map that can be shared by the components
      * with the same kit. Only the component coloring map is provided.
      */
    protected static Map getSharedColoringMap(Class kitClass) {
        synchronized (Settings.class) { // must sync like this against dedloks
            Map cm = (Map)sharedColoringMaps.get(kitClass);
            if (cm == null) {
                cm = SettingsUtil.getColoringMap(kitClass, false, true);
                // Test if there's a default coloring
             //   if (cm.get(SettingsNames.DEFAULT_COLORING) == null) {
                    cm.put(SettingsNames.DEFAULT_COLORING, SettingsDefaults.defaultColoring);
             //   }

                sharedColoringMaps.put(kitClass, cm);
            }

            return cm;
        }
    }


    /** Called when the <tt>BaseTextUI</tt> is being installed
    * into the component.
    */
    public void installUI(JTextComponent c) {
        synchronized (getComponentLock()) {
            this.component = c;
            putProperty(COMPONENT_PROPERTY, c);

            // listen on component
            component.addPropertyChangeListener(this);
            component.addFocusListener(focusL);

            // listen on caret
            Caret caret = component.getCaret();
            if (caret != null) {
                caret.addChangeListener(this);
            }

            BaseDocument doc = getDocument();
            if (doc != null) {
                modelChanged(null, doc);
            }
        }

        // Make sure all the things depending on non-null component will be updated
        settingsChange(null);
        
        // fix for issue #16352
        getDefaultColoring().apply(component);
    }

    /** Called when the <tt>BaseTextUI</tt> is being uninstalled
    * from the component.
    */
    public void uninstallUI(JTextComponent c) {
        synchronized (getComponentLock()) {
            
            // fix for issue 12996
            if (component != null) {
                
            // stop listening on caret
            Caret caret = component.getCaret();
            if (caret != null) {
                caret.removeChangeListener(this);
            }

            // stop listening on component
            component.removePropertyChangeListener(this);
            component.removeFocusListener(focusL);
            
            }

            BaseDocument doc = getDocument();
            if (doc != null) {
                modelChanged(doc, null);
            }

            component = null;
            putProperty(COMPONENT_PROPERTY, null);

            // Clear the font-metrics cache
            FontMetricsCache.clear();
        }
    }

    /** Get the lock assuring the component will not be changed
    * by <tt>installUI()</tt> or <tt>uninstallUI()</tt>.
    * It's useful for the classes that want to listen for the
    * component change in <tt>EditorUI</tt>.
    */
    public Object getComponentLock() {
        if (componentLock == null) {
            componentLock = new ComponentLock();
        }
        return componentLock;
    }
    static class ComponentLock {};

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, l);
    }

    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void settingsChange(SettingsChangeEvent evt) {
        if (component != null) {
            if (Utilities.getKit(component) == null) {
                return; // prevent problems if not garbage collected and settings changed
            }
        }

        Class kitClass = getKitClass();
        String settingName = (evt != null) ? evt.getSettingName() : null;

        if (settingName == null || SettingsNames.LINE_NUMBER_VISIBLE.equals(settingName)
                || SettingsNames.PRINT_LINE_NUMBER_VISIBLE.equals(settingName)
           ) {
            lineNumberVisibleSetting = SettingsUtil.getBoolean(kitClass, (component != null)
                                       ? SettingsNames.LINE_NUMBER_VISIBLE : SettingsNames.PRINT_LINE_NUMBER_VISIBLE,
                                       (component != null) ? SettingsDefaults.defaultLineNumberVisible
                                       : SettingsDefaults.defaultPrintLineNumberVisible
                                                              );
            lineNumberVisible = lineNumberEnabled && lineNumberVisibleSetting;
            
            // if this is printing, the drawing of original line numbers must be enabled
            if (component == null)
                disableLineNumbers = false;
            
            if (disableLineNumbers)
                lineNumberVisible = false;
        }

        BaseDocument doc = getDocument();
        if (doc != null) {

            if (settingName == null || SettingsNames.LINE_NUMBER_MARGIN.equals(settingName)) {
                Object value = Settings.getValue(kitClass, SettingsNames.LINE_NUMBER_MARGIN);
                lineNumberMargin = (value instanceof Insets) ? (Insets)value : NULL_INSETS;
            }

            if (settingName == null || SettingsNames.TEXT_LEFT_MARGIN_WIDTH.equals(settingName)) {
                textLeftMarginWidth = SettingsUtil.getInteger(kitClass,
                                      SettingsNames.TEXT_LEFT_MARGIN_WIDTH,
                                      SettingsDefaults.defaultTextLeftMarginWidth);
            }

            if (settingName == null || SettingsNames.LINE_HEIGHT_CORRECTION.equals(settingName)) {
                Object value = Settings.getValue(kitClass, SettingsNames.LINE_HEIGHT_CORRECTION);
                if (!(value instanceof Float) || ((Float)value).floatValue() < 0) {
                    value = SettingsDefaults.defaultLineHeightCorrection;
                }
                lineHeightCorrection = ((Float)value).floatValue();
            }

            if (settingName == null || SettingsNames.TEXT_LIMIT_LINE_VISIBLE.equals(settingName)) {
                textLimitLineVisible = SettingsUtil.getBoolean(kitClass,
                                       SettingsNames.TEXT_LIMIT_LINE_VISIBLE, SettingsDefaults.defaultTextLimitLineVisible);
            }

            if (settingName == null || SettingsNames.TEXT_LIMIT_LINE_COLOR.equals(settingName)) {
                Object value = Settings.getValue(kitClass, SettingsNames.TEXT_LIMIT_LINE_COLOR);
                textLimitLineColor = (value instanceof Color) ? (Color)value
                                     : SettingsDefaults.defaultTextLimitLineColor;
            }

            if (settingName == null || SettingsNames.TEXT_LIMIT_WIDTH.equals(settingName)) {
                textLimitWidth = SettingsUtil.getPositiveInteger(kitClass,
                                 SettingsNames.TEXT_LIMIT_WIDTH, SettingsDefaults.defaultTextLimitWidth);
            }

            // component only properties
            if (component != null) {
                if (settingName == null || SettingsNames.SCROLL_JUMP_INSETS.equals(settingName)) {
                    Object value = Settings.getValue(kitClass, SettingsNames.SCROLL_JUMP_INSETS);
                    scrollJumpInsets = (value instanceof Insets) ? (Insets)value : NULL_INSETS;
                }

                if (settingName == null || SettingsNames.SCROLL_FIND_INSETS.equals(settingName)) {
                    Object value = Settings.getValue(kitClass, SettingsNames.SCROLL_FIND_INSETS);
                    scrollFindInsets = (value instanceof Insets) ? (Insets)value : NULL_INSETS;
                }

                if (settingName == null || SettingsNames.COMPONENT_SIZE_INCREMENT.equals(settingName)) {
                    Object value = Settings.getValue(kitClass, SettingsNames.COMPONENT_SIZE_INCREMENT);
                    componentSizeIncrement = (value instanceof Dimension) ? (Dimension)value : NULL_DIMENSION;
                }

                if (settingName == null || SettingsNames.RENDERING_HINTS.equals(settingName)) {
                    Object value = Settings.getValue(kitClass, SettingsNames.RENDERING_HINTS);
                    renderingHints = (value instanceof Map) ? (Map)value : null;
                }

                if (settingName == null || SettingsNames.CARET_COLOR_INSERT_MODE.equals(settingName)
                        || SettingsNames.CARET_COLOR_OVERWRITE_MODE.equals(settingName)
                   ) {
                    Boolean b = (Boolean)getProperty(OVERWRITE_MODE_PROPERTY);
                    Color caretColor;
                    if (b == null || !b.booleanValue()) {
                        Object value = Settings.getValue(kitClass, SettingsNames.CARET_COLOR_INSERT_MODE);
                        caretColor = (value instanceof Color) ? (Color)value
                                     : SettingsDefaults.defaultCaretColorInsertMode;

                    } else {
                        Object value = Settings.getValue(kitClass, SettingsNames.CARET_COLOR_OVERWRITE_MODE);
                        caretColor = (value instanceof Color) ? (Color)value
                                     : SettingsDefaults.defaultCaretColorOvwerwriteMode;
                    }

                    if (caretColor != null) {
                        component.setCaretColor(caretColor);
                    }
                }



                // fix for issues 13842, 14003
                if (SwingUtilities.isEventDispatchThread()) {
                    component.setKeymap(Utilities.getKit(component).getKeymap());                    
                    BaseTextUI ui = (BaseTextUI)component.getUI();
                    ui.updateHeight();
                    component.repaint();
                } else {
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                JTextComponent c = component;
                                if (c != null) {
                                    BaseKit kit = Utilities.getKit(c);
                                    if (kit != null) {
                                        c.setKeymap(kit.getKeymap());                                    
                                        BaseTextUI ui = (BaseTextUI)c.getUI();
                                        if (ui != null) {
                                            ui.updateHeight();
                                            c.repaint();
                                        }
                                    }
                                }
                            }
                        }
                    );
                }
            }
        }

        coloringMap = null; // reset coloring map so it's lazily rebuilt
         /* make sure there's no pending preferenceChanged() request
          * because if it would be then the fontsInited = false
          * would have no effect.
          */
        fontsInitedPreferenceChanged = false;
        fontsInited = false;

    }

    public void stateChanged(ChangeEvent evt) {
        SwingUtilities.invokeLater(
            new Runnable() {
                
                /** @return true if the document supports guarded sections
                 * and when either the caret is in guarded block
                 * or when selection spans any guarded block(s).
                 */
                private boolean isCaretGuarded(){
                    JTextComponent c = component;
                    BaseDocument bdoc = getDocument();
                    boolean inGuardedBlock = false;
                    if (bdoc instanceof GuardedDocument){
                        GuardedDocument gdoc = (GuardedDocument)bdoc;

                        boolean selectionSpansGuardedSection = false;
                        for (int i=c.getSelectionStart(); i<c.getSelectionEnd(); i++){
                            if (gdoc.isPosGuarded(i)){
                                selectionSpansGuardedSection = true;
                                break;
                            }
                        }
                        
                        inGuardedBlock = (gdoc.isPosGuarded(c.getCaretPosition()) ||
                            selectionSpansGuardedSection);
                    }
                    return inGuardedBlock;
                }
                
                public void run() {
                    JTextComponent c = component;
                    if (c != null) {
                        BaseKit kit = Utilities.getKit(c);
                        if (kit != null) {
                            boolean isEditable = c.isEditable();
                            boolean selectionVisible = c.getCaret().isSelectionVisible();
                            boolean caretGuarded = isCaretGuarded();

                            Action a = kit.getActionByName(BaseKit.copyAction);
                            if (a != null) {
                                a.setEnabled(selectionVisible);
                            }

                            a = kit.getActionByName(BaseKit.cutAction);
                            if (a != null) {
                                a.setEnabled(selectionVisible && !caretGuarded && isEditable);
                            }

                            a = kit.getActionByName(BaseKit.removeSelectionAction);
                            if (a != null) {
                                a.setEnabled(selectionVisible && !caretGuarded && isEditable);
                            }
                            
                            a = kit.getActionByName(BaseKit.pasteAction);
                            if (a != null) {
                                a.setEnabled(!caretGuarded && isEditable);
                            }
                        }
                    }
                }
            }
        );
    }

    protected void modelChanged(BaseDocument oldDoc, BaseDocument newDoc) {
        if (oldDoc != null) {
            // remove all document layers
            drawLayerList.remove(oldDoc.getDrawLayerList());
        }

        if (newDoc != null) {
            settingsChange(null);

            // add all document layers
            drawLayerList.add(newDoc.getDrawLayerList());
        }

        if (oldDoc != null)
            oldDoc.getBookmarks().removeAll();            
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if ("document".equals(propName)) {
            BaseDocument oldDoc = (evt.getOldValue() instanceof BaseDocument)
                                  ? (BaseDocument)evt.getOldValue() : null;
            BaseDocument newDoc = (evt.getNewValue() instanceof BaseDocument)
                                  ? (BaseDocument)evt.getNewValue() : null;
            modelChanged(oldDoc, newDoc);

        } else if ("margin".equals(propName)) { // NOI18N
            updateTextMargin();

        } else if ("caret".equals(propName)) { // NOI18N
            if (evt.getOldValue() instanceof Caret) {
                ((Caret)evt.getOldValue()).removeChangeListener(this);
            }
            if (evt.getNewValue() instanceof Caret) {
                ((Caret)evt.getNewValue()).addChangeListener(this);
            }

        } else if ("enabled".equals(propName)) { // NOI18N
            if (!component.isEnabled()) {
                component.getCaret().setVisible(false);
            }
        }
    }

    protected Map createColoringMap() {
        Map cm;

        if (component != null) {
            // Use the shared coloring-map to save space and time
            cm = getSharedColoringMap(getKitClass());

        } else { // print coloring-map must be created
            cm = SettingsUtil.getColoringMap(getKitClass(), (component == null), true);
            // Test if there's a default coloring
            if (cm.get(SettingsNames.DEFAULT_COLORING) == null) {
                cm.put(SettingsNames.DEFAULT_COLORING, SettingsDefaults.defaultColoring);
            }
        }

        return cm;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public int getLineAscent() {
        return lineAscent;
    }

    public Map getColoringMap() {
        if (coloringMap == null) {
            coloringMap = createColoringMap();
        }
        return coloringMap;
    }

    public Coloring getDefaultColoring() {
        return (Coloring)getColoringMap().get(SettingsNames.DEFAULT_COLORING);
    }

    public Coloring getColoring(String coloringName) {
        return (Coloring)getColoringMap().get(coloringName);
    }

    private void updateLineHeight(Graphics g) {
        if (debugUpdateLineHeight) {
            System.err.println("EditorUI.updateLineHeight(): Computing lineHeight ...");
        }

        Map cm = getColoringMap();
        Iterator i = cm.entrySet().iterator();
        int maxHeight = 1;
        int maxAscent = 0;
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            String coloringName = (String)me.getKey();
            Coloring c = (Coloring)me.getValue();
            if (c != null) {
                Font font = c.getFont();
                if (font != null && (c.getFontMode() & Coloring.FONT_MODE_APPLY_SIZE) != 0) {
                    FontMetrics fm = g.getFontMetrics(font);
                    if (fm != null) {
                        if (debugUpdateLineHeight) {
                            if (maxHeight < fm.getHeight()) {
                                System.err.println("Updating maxHeight from "
                                    + maxHeight + " to " + fm.getHeight()
                                    + ", coloringName=" + coloringName
                                    + ", font=" + font
                                );
                            }

                            if (maxHeight < fm.getHeight()) {
                                System.err.println("Updating maxAscent from "
                                    + maxAscent + " to " + fm.getAscent()
                                    + ", coloringName=" + coloringName
                                    + ", font=" + font
                                );
                            }
                        }

                        maxHeight = Math.max(maxHeight, fm.getHeight());
                        maxAscent = Math.max(maxAscent, fm.getAscent());
                    }
                }
            }
        }

        // Apply lineHeightCorrection
        lineHeight = (int)(maxHeight * lineHeightCorrection);
        lineAscent = (int)(maxAscent * lineHeightCorrection);

    }
    
    /** Return whether the fonts are already initialized or not.
     */
    boolean isFontsInited() {
        return fontsInited;
    }

    protected void update(Graphics g) {
        Class kitClass = Utilities.getKitClass(component);

        // Set the margin
        if (kitClass != null) {
            Object value = Settings.getValue(kitClass, SettingsNames.MARGIN);
            Insets margin = (value instanceof Insets) ? (Insets)value : null;
            component.setMargin(margin);
        }

        // Apply the default coloring to the component
  //      getDefaultColoring().apply(component);

        // Possibly apply the rendering hints
        if (renderingHints != null) {
            ((Graphics2D)g).setRenderingHints(renderingHints);
        }

        Coloring dc = getDefaultColoring();

        // Handle line number fonts and widths
        Coloring lnc = (Coloring)getColoringMap().get(SettingsNames.LINE_NUMBER_COLORING);
        if (lnc != null) {
            Font lnFont = lnc.getFont();
            if (lnFont == null) {
                lnFont = dc.getFont();
            }
            FontMetrics lnFM = g.getFontMetrics(lnFont);
            int maxWidth = 1;
            char[] digit = new char[1]; // will be used for '0' - '9'
            for (int i = 0; i <= 9; i++) {
                digit[0] = (char)('0' + i);
                maxWidth = Math.max(maxWidth, lnFM.charsWidth(digit, 0, 1));
            }
            lineNumberDigitWidth = maxWidth;
        }

        // Update line height
        updateLineHeight(g);

        // Update space width of the default coloring's font
        FontMetricsCache.Info fmcInfo = FontMetricsCache.getInfo(getDefaultColoring().getFont());
        defaultSpaceWidth = fmcInfo.getSpaceWidth(g);

        // Update total height
        if (component != null) {
            ((BaseTextUI)component.getUI()).updateHeight();
            updateLineNumberWidth(0);
            checkLineLimit();
        }

        /* JDK1.3 patch for the behavior that occurs when the line is wider
        * than the screen and the user first clicks End key to go to the end
        * and then goes back by (Ctrl+)Left. As the non-simple scrolling mode 
        * is used in JViewport in 1.3 the line number block appears shifted
        * to the right and gets repainted after 300ms which looks ugly.
        * The patch is to set the simple scrolling mode into JViewport.
        *
        * getParentViewport().setScrollMode(0); // 2 stands for SIMPLE_SCROLL_MODE
        *
        */
        try {
            JViewport vp = getParentViewport();
            if (vp != null) {
                java.lang.reflect.Method setScrollModeMethod = JViewport.class.getDeclaredMethod(
                            "setScrollMode", new Class[] { Integer.TYPE }); // NOI18N
                setScrollModeMethod.invoke(vp, new Object[] { new Integer(0) });
            }
        } catch (Throwable t) {
        }

        // update glyph gutter colors and fonts
        if (isGlyphGutterVisible()) {
            glyphGutter.update();
            updateScrollPaneCornerColor();
        }
        
        // FIx of #14295
        updateVirtualHeight(0);

        /* Fix of #8123 - the caret is physically set to the end of the file
         * but the window is not scrolled there.
         * The problem is that the caret has the right position
         * but the editor pane has not yet the right size. Although
         * at such time the TextUI.preferenceChanged() was already
         * called, the request for revalidation is waiting
         * in the queue to be done.
         * The fix adds a flag that determines whether the fonts
         * were reinited already but there is a pending preferenceChanged()
         * request that was not finished yet. Once it's finished
         * the fontsInited flag is set.
         * The other part of the fix is in BaseCaret.
         */
        fontsInitedPreferenceChanged = true;

        if (component != null) {
            // revalidate the component
            ((BaseTextUI)component.getUI()).preferenceChanged(true, true);
        }
 
    }

    public final JTextComponent getComponent() {
        return component;
    }

    /** Get the document to work on. Either component's document or printed document
    * is returned. It can return null in case the component's document is not instance
    * of BaseDocument.
    */
    public final BaseDocument getDocument() {
        return (component != null) ? Utilities.getDocument(component) : printDoc;
    }

    private Class getKitClass() {
        return (component != null) ? Utilities.getKitClass(component)
               : ((printDoc != null) ? printDoc.getKitClass() : null);
    }

    public Object getProperty(Object key) {
        return props.get(key);
    }

    public void putProperty(Object key, Object value) {
        Object oldValue;
        if (value != null) {
            oldValue = props.put(key, value);
        } else {
            oldValue = props.remove(key);
        }
        firePropertyChange(key.toString(), oldValue, value);
    }

    /** Get extended editor component.
     * The extended component should normally be used
     * for editing files instead of just the JEditorPane
     * because it offers status bar and possibly
     * other useful components.
     * The getExtComponent() should not be used when
     * the JEditorPane is included in dialog.
     * @see #hasExtComponent()
     */
    public JComponent getExtComponent() {
        if (extComponent == null) {
            if (component != null) {
                extComponent = createExtComponent();
            }
        }
        return extComponent;
    }

    protected JComponent createExtComponent() {
        setLineNumberEnabled(true); // enable line numbering

        // extComponent will be a panel
        JComponent ec = new JPanel(new BorderLayout());
        ec.putClientProperty(JTextComponent.class, component);

        // Add the scroll-pane with the component to the center
        JScrollPane scroller = new JScrollPane(component);
        scroller.getViewport().setMinimumSize(new Dimension(4,4));

        // glyph gutter must be created here
        glyphGutter = new GlyphGutter(this);
        scroller.setRowHeaderView(glyphGutter);

        glyphCorner = new JPanel();
        updateScrollPaneCornerColor();
        scroller.setCorner(JScrollPane.LOWER_LEFT_CORNER, glyphCorner);

        ec.add(scroller);

        // Install the status-bar panel to the bottom
        ec.add(getStatusBar().getPanel(), BorderLayout.SOUTH);
        
        return ec;
    }
    
    /** Whether this ui uses extComponent or not.
     * @see #getExtComponent()
     */
    public boolean hasExtComponent() {
        return (extComponent != null);
    }

    public Abbrev getAbbrev() {
        if (abbrev == null) {
            abbrev = new Abbrev(this, true, true);
        }
        return abbrev;
    }

    public WordMatch getWordMatch() {
        if (wordMatch == null) {
            wordMatch = new WordMatch(this);
        }
        return wordMatch;
    }

    public StatusBar getStatusBar() {
        if (statusBar == null) {
            statusBar = new StatusBar(this);
        }
        return statusBar;
    }

    final DrawLayerList getDrawLayerList() {
        return drawLayerList;
    }

    /** Find the layer with some layer name in the layer hierarchy */
    public DrawLayer findLayer(String layerName) {
        return drawLayerList.findLayer(layerName);
    }

    /** Add new layer and use its priority to position it in the chain.
    * If there's the layer with same visibility then the inserted layer
    * will be placed after it.
    *
    * @param layer layer to insert into the chain
    */
    public boolean addLayer(DrawLayer layer, int visibility) {
        return drawLayerList.add(layer, visibility);
    }

    public DrawLayer removeLayer(String layerName) {
        return drawLayerList.remove(layerName);
    }

    public void repaint(int startY) {
        repaint(startY, component.getHeight());
    }

    public void repaint(int startY, int height) {
        if (height <= 0) {
            return;
        }
        int width = Math.max(component.getWidth(), 0);
        startY = Math.max(startY, 0);
        component.repaint(0, startY, width, height);
    }

    public void repaintOffset(int pos) throws BadLocationException {
        repaintBlock(pos, pos);
    }

    /** Repaint the block between the given positions. */
    public void repaintBlock(int startPos, int endPos)
    throws BadLocationException {
        BaseTextUI ui = (BaseTextUI)component.getUI();
        if (startPos > endPos) { // swap
            int tmpPos = startPos;
            startPos = endPos;
            endPos = tmpPos;
        }
        try {
            int yFrom = ui.getYFromPos(startPos);
            int yTo = ui.getYFromPos(endPos);
            repaint(yFrom, (yTo - yFrom) + lineHeight);
        } catch (BadLocationException e) {
            if (Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                e.printStackTrace();
            }
        }
    }

    /** Is the parent of some editor component a viewport */
    private JViewport getParentViewport() {
        Component pc = component.getParent();
        return (pc instanceof JViewport) ? (JViewport)pc : null;
    }

    /** Finds the frame - parent of editor component */
    public static Frame getParentFrame(Component c) {
        do {
            c = c.getParent();
            if (c instanceof Frame) {
                return (Frame)c;
            }
        } while (c != null);
        return null;
    }

    /** Possibly update virtual width. If the width
    * is really updated, the method returns true.
    */
    public boolean updateVirtualWidth(int width) {
        boolean updated = false;
        if (width > virtualSize.width) {
            int widthInc = componentSizeIncrement.width;
            widthInc = (widthInc < 0) ? (lastExtentBounds.width * (-widthInc) / 100)
                       : widthInc * defaultSpaceWidth;

            virtualSize.width = width + widthInc;
            virtualSizeUpdated = true;
            updated = true;
        }

        return updated;
    }

    /** Possibly update virtual height. If the height
    * is really updated, the method returns true. There is
    * a slight difference against virtual width in that
    * if the height is shrinked too much the virtual height
    * is shrinked too.
    * 0 can be used to update to the real height.
    */
    public boolean updateVirtualHeight(int height) {
        boolean updated = false;
        updateLineNumberWidth(0);

         // changed to fix #18648
        if (height <= 0) { //compute real height - fix of #14295
            height = (int)((TextUI)component.getUI()).getRootView(component).getPreferredSpan(View.Y_AXIS);
        }
        
        if (height != virtualSize.height) {
            virtualSize.height = height;
            virtualSizeUpdated = true;
            updated = true;
        }

        return updated;
    }

    public boolean isLineNumberEnabled() {
        return lineNumberEnabled;
    }

    public void setLineNumberEnabled(boolean lineNumberEnabled) {
        this.lineNumberEnabled = lineNumberEnabled;
        lineNumberVisible = lineNumberEnabled && lineNumberVisibleSetting;
        if (disableLineNumbers)
            lineNumberVisible = false;
    }

    /** Update the width that will be occupied by the line number.
    * @param maxDigitCount maximum digit count that can the line number have.
    *  if it's lower or equal to zero it will be computed automatically.
    */
    public void updateLineNumberWidth(int maxDigitCount) {
        int oldWidth = lineNumberWidth;

        if (lineNumberVisible) {
            try {
                if (maxDigitCount <= 0) {
                    BaseDocument doc = getDocument();
                    int lineCnt = Utilities.getLineOffset(doc, doc.getLength()) + 1;
                    maxDigitCount = Integer.toString(lineCnt).length();
                }

                if (maxDigitCount > lineNumberMaxDigitCount) {
                    lineNumberMaxDigitCount = maxDigitCount;
                }

            } catch (BadLocationException e) {
                lineNumberMaxDigitCount = 1;
            }
            lineNumberWidth = lineNumberMaxDigitCount * lineNumberDigitWidth;
            if (lineNumberMargin != null) {
                lineNumberWidth += lineNumberMargin.left + lineNumberMargin.right;
            }

        } else {
            lineNumberWidth = 0;
        }

        updateTextMargin();
        if (oldWidth != lineNumberWidth) { // changed
            if (component != null) {
                component.repaint();
            }
        }
    }

    void checkLineLimit() {
        BaseDocument doc = getDocument();
        if (doc != null) {
            Integer lineLimit = (Integer)doc.getProperty(BaseDocument.LINE_LIMIT_PROP);
            if (lineLimit != null) {
                if (component != null) {
                    // Not using FM cache - could be called too early
                    FontMetrics fm = component.getFontMetrics(getDefaultColoring().getFont());
                    if (fm != null) {
                        int charWidth = fm.stringWidth("A");
                        updateVirtualWidth(charWidth * lineLimit.intValue() + lineNumberWidth);
                    }
                }
            }
        }
    }

    public void updateTextMargin() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        updateTextMargin();
                    }
                }
            );
        }

        Insets orig = textMargin;
        Insets cm = (component != null) ? component.getMargin() : null;
        int leftWidth = lineNumberWidth + textLeftMarginWidth;
        if (cm != null) {
            textMargin = new Insets(cm.top, cm.left + leftWidth,
                                    cm.bottom, cm.right);
        } else {
            textMargin = new Insets(0, leftWidth, 0, 0);
        }
        if (orig.top != textMargin.top || orig.bottom != textMargin.bottom) {
            ((BaseTextUI)component.getUI()).invalidateStartY();
        }
    }

    public Rectangle getExtentBounds() {
        return getExtentBounds(null);
    }

    /** Get position of the component extent. The (x, y) are set to (0, 0) if there's
    * no viewport or (-x, -y) if there's one.
    */
    public Rectangle getExtentBounds(Rectangle r) {
        if (r == null) {
            r = new Rectangle();
        }
        if (component != null) {
            JViewport port = getParentViewport();
            if (port != null) {
                Point p = port.getViewPosition();
                r.width = port.getWidth();
                r.height = port.getHeight();
                r.x = p.x;
                r.y = p.y;
            } else { // no viewport
                r.setBounds(component.getVisibleRect());
            }
        }
        return r;
    }

    /** Get the begining of the area covered by text */
    public Insets getTextMargin() {
        return textMargin;
    }

    public void scrollRectToVisible(final Rectangle r, final int scrollPolicy) {
        Utilities.runInEventDispatchThread(
            new Runnable() {
                public void run() {
                    scrollRectToVisibleFragile(r, scrollPolicy);
                }
            }
        );
    }

    /** Must be called with EventDispatchThread */
    boolean scrollRectToVisibleFragile(Rectangle r, int scrollPolicy) {
        Insets margin = getTextMargin();
        Rectangle bounds = getExtentBounds();
        r = new Rectangle(r); // make copy of orig rect
        r.x -= margin.left;
        r.y -= margin.top;
        bounds.width -= margin.left + margin.right;
        bounds.height -= margin.top + margin.bottom;
        return scrollRectToVisibleImpl(r, scrollPolicy, bounds);
    }

    /** Scroll the view so that requested rectangle is best visible.
     * There are different scroll policies available.
     * @return whether the extent has to be scrolled in any direction.
     */
    private boolean scrollRectToVisibleImpl(Rectangle r, int scrollPolicy,
                                            Rectangle bounds) {
        if (bounds.width <= 0 || bounds.height <= 0) {
            return false;
        }

        // handle find scrolling specifically
        if (scrollPolicy == SCROLL_FIND) {
            // converted inset
            int cnvFI = (scrollFindInsets.left < 0)
                ? (- bounds.width * scrollFindInsets.left / 100)
                : scrollFindInsets.left * defaultSpaceWidth;

            int nx = Math.max(r.x - cnvFI, 0);
            
            cnvFI = (scrollFindInsets.right < 0)
                ? (- bounds.width * scrollFindInsets.right / 100)
                : scrollFindInsets.right * defaultSpaceWidth;

            r.width += (r.x - nx) + cnvFI;
            r.x = nx;

            cnvFI = (scrollFindInsets.top < 0)
                ? (- bounds.height * scrollFindInsets.top / 100)
                : scrollFindInsets.top * lineHeight;

            int ny = Math.max(r.y - cnvFI, 0);

            cnvFI = (scrollFindInsets.bottom < 0)
                ? (- bounds.height * scrollFindInsets.bottom / 100)
                : scrollFindInsets.bottom * lineHeight;

            r.height += (r.y - ny) + cnvFI;
            r.y = ny;

            return scrollRectToVisibleImpl(r, SCROLL_SMALLEST, bounds); // recall
        }
        // r must be within virtualSize's width
        if (r.x + r.width > virtualSize.width) {
            r.x = virtualSize.width - r.width;
            if (r.x < 0) {
                r.x = 0;
                r.width = virtualSize.width;
            }
            return scrollRectToVisibleImpl(r, scrollPolicy, bounds); // recall
        }
        // r must be within virtualSize's height
        if (r.y + r.height > virtualSize.height) {
            r.y = virtualSize.height - r.height;
            if (r.y < 0) {
                r.y = 0;
                r.height = virtualSize.height;
            }
            return scrollRectToVisibleImpl(r, scrollPolicy, bounds);
        }

        // if r extends bounds dimension it must be corrected now
        if (r.width > bounds.width || r.height > bounds.height) {
            Rectangle caretRect = new Rectangle((Rectangle)component.getCaret());
            if (caretRect.x >= r.x
                    && caretRect.x + caretRect.width <= r.x + r.width
                    && caretRect.y >= r.y
                    && caretRect.y + caretRect.height <= r.y + r.height
               ) { // caret inside requested rect
                // move scroll rect for best caret visibility
                int overX = r.width - bounds.width;
                int overY = r.height - bounds.height;
                if (overX > 0) {
                    r.x -= overX * (caretRect.x - r.x) / r.width;
                }
                if (overY > 0) {
                    r.y -= overY * (caretRect.y - r.y) / r.height;
                }
            }
            r.height = bounds.height;
            r.width = bounds.width; // could be different algorithm
            return scrollRectToVisibleImpl(r, scrollPolicy, bounds);
        }

        int newX = bounds.x;
        int newY = bounds.y;
        boolean move = false;
        // now the scroll rect is within bounds of the component
        // and can have size of the extent at maximum
        if (r.x < bounds.x) {
            move = true;
            switch (scrollPolicy) {
            case SCROLL_MOVE:
                newX = (scrollJumpInsets.left < 0)
                       ? (bounds.width * (-scrollJumpInsets.left) / 100)
                       : scrollJumpInsets.left * defaultSpaceWidth;
                newX = Math.min(newX, bounds.x + bounds.width - (r.x + r.width));
                newX = Math.max(r.x - newX, 0); // new bounds.x
                break;
            case SCROLL_DEFAULT:
            case SCROLL_SMALLEST:
            default:
                newX = r.x;
                break;
            }
            updateVirtualWidth(newX + bounds.width);
        } else if (r.x + r.width > bounds.x + bounds.width) {
            move = true;
            switch (scrollPolicy) {
            case SCROLL_SMALLEST:
                newX = r.x + r.width - bounds.width;
                break;
            default:
                newX = (scrollJumpInsets.right < 0)
                       ? (bounds.width * (-scrollJumpInsets.right) / 100 )
                       : scrollJumpInsets.right * defaultSpaceWidth;
                newX = Math.min(newX, bounds.width - r.width);
                newX = (r.x + r.width) + newX - bounds.width;
                break;
            }
            updateVirtualWidth(newX + bounds.width);
        }

        if (r.y < bounds.y) {
            move = true;
            switch (scrollPolicy) {
            case SCROLL_MOVE:
                newY = r.y;
                newY -= (scrollJumpInsets.top < 0)
                        ? (bounds.height * (-scrollJumpInsets.top) / 100 )
                        : scrollJumpInsets.top * lineHeight;
                break;
            case SCROLL_SMALLEST:
                newY = r.y;
                break;
            case SCROLL_DEFAULT:
            default:
                newY = r.y - (bounds.height - r.height) / 2; // center
                break;
            }
            newY = Math.max(newY, 0);
        } else if (r.y + r.height > bounds.y + bounds.height) {
            move = true;
            switch (scrollPolicy) {
            case SCROLL_MOVE:
                newY = (r.y + r.height) - bounds.height;
                newY += (scrollJumpInsets.bottom < 0)
                        ? (bounds.height * (-scrollJumpInsets.bottom) / 100 )
                        : scrollJumpInsets.bottom * lineHeight;
                break;
            case SCROLL_SMALLEST:
                newY = (r.y + r.height) - bounds.height;
                break;
            case SCROLL_DEFAULT:
            default:
                newY = r.y - (bounds.height - r.height) / 2; // center
                break;
            }
            newY = Math.max(newY, 0);
        }

        if (move) {
            setExtentPosition(newX, newY);
        }
        return move;
    }

    void setExtentPosition(int x, int y) {
        JViewport port = getParentViewport();
        if (port != null) {
            Point p = new Point(Math.max(x, 0), Math.max(y, 0));
            port.setViewPosition(p);
        }
    }

    public void adjustWindow(int caretPercentFromWindowTop) {
        final Rectangle bounds = getExtentBounds();
        if (component != null && (component.getCaret() instanceof Rectangle)) {
            Rectangle caretRect = (Rectangle)component.getCaret();
            bounds.y = caretRect.y - (caretPercentFromWindowTop * bounds.height) / 100
                       + (caretPercentFromWindowTop * lineHeight) / 100;
            Utilities.runInEventDispatchThread(
                new Runnable() {
                    public void run() {
                        scrollRectToVisible(bounds, SCROLL_SMALLEST);
                    }
                }
            );
        }
    }

    /** Set the dot according to the currently visible screen window.
    * #param percentFromWindowTop percentage giving the distance of the caret
    *  from the top of the currently visible window.
    */
    public void adjustCaret(int percentFromWindowTop) {
        JTextComponent c = component;
        if (c != null) {
            Rectangle bounds = getExtentBounds();
            bounds.y += (percentFromWindowTop * bounds.height) / 100
                        - (percentFromWindowTop * lineHeight) / 100;
            try {
                int offset = ((BaseTextUI)c.getUI()).getPosFromY(bounds.y);
                if (offset >= 0) {
                    caretSetDot(offset, null, SCROLL_SMALLEST);
                }
            } catch (BadLocationException e) {
            }
        }
    }

    /** Set the position of the caret and scroll the extent if necessary.
     * @param offset position where the caret should be placed
     * @param scrollRect rectangle that should become visible. It can be null
     *   when no scrolling should be done.
     * @param scrollPolicy policy to be used when scrolling.
     * @deprecated
     */
    public void caretSetDot(int offset, Rectangle scrollRect, int scrollPolicy) {
        if (component != null) {
            Caret caret = component.getCaret();
            if (caret instanceof BaseCaret) {
                ((BaseCaret)caret).setDot(offset, scrollRect, scrollPolicy);
            } else {
                caret.setDot(offset);
            }
        }
    }

    /** Set the position of the caret and scroll the extent if necessary.
     * @param offset position where the caret should be placed
     * @param scrollRect rectangle that should become visible. It can be null
     *   when no scrolling should be done.
     * @param scrollPolicy policy to be used when scrolling.
     * @deprecated
     */
    public void caretMoveDot(int offset, Rectangle scrollRect, int scrollPolicy) {
        if (component != null) {
            Caret caret = component.getCaret();
            if (caret instanceof BaseCaret) {
                ((BaseCaret)caret).moveDot(offset, scrollRect, scrollPolicy);
            } else {
                caret.moveDot(offset);
            }
        }
    }

    /** This method is called by textui to do the paint.
    * It is forwarded either to paint through the image
    * and then copy the image area to the screen or to
    * paint directly to this graphics. The real work occurs
    * in draw-engine.
    */
    protected void paint(Graphics g) {
        if (component != null) { // component must be installed
            if (fontsInitedPreferenceChanged) {
                fontsInitedPreferenceChanged = false;
                fontsInited = true;
                getExtentBounds(lastExtentBounds);
            }

            if (!fontsInited && g != null) {
                update(g);
            }
            ((BaseTextUI)component.getUI()).paintRegion(g);
        }
    }

    /** Returns the line number margin */
    public Insets getLineNumberMargin() {
        return lineNumberMargin;
    }

    /** Returns width of the one digit */
    public int getLineNumberDigitWidth() {
        return lineNumberDigitWidth;
    }

    /** Is glyph gutter created and visible for the document or not */
    public boolean isGlyphGutterVisible() {
        return glyphGutter != null;
    }
    
    public GlyphGutter getGlyphGutter() {
        return glyphGutter;
    }

    protected void updateScrollPaneCornerColor() {
        Coloring lineColoring = (Coloring)getColoringMap().get(SettingsNames.LINE_NUMBER_COLORING);
        Coloring defaultColoring = (Coloring)getDefaultColoring();
        
        Color backgroundColor;
        if (lineColoring.getBackColor() != null)
            backgroundColor = lineColoring.getBackColor();
        else
            backgroundColor = defaultColoring.getBackColor();
        
        glyphCorner.setBackground(backgroundColor);
    }
}
