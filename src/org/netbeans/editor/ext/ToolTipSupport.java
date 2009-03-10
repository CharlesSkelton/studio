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

import org.netbeans.editor.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Support for editor tooltips. Once the user stops moving the mouse
 * for the {@link #INITIAL_DELAY} milliseconds the enterTimer fires
 * and the {@link #updateToolTip()} method is called which searches
 * for the action named {@link ExtKit#buildToolTipAction} and if found
 * it executes it. The tooltips can be displayed by either calling
 * {@link #setToolTipText(java.lang.String)}
 * or {@link #setToolTip(javax.swing.JComponent)}.<BR>
 * However only one of the above ways should be used
 * not a combination of both because in such case
 * the text could be propagated in the previously set
 * custom tooltip component. 
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public class ToolTipSupport extends MouseAdapter
implements MouseMotionListener, ActionListener, PropertyChangeListener,
SettingsChangeListener, FocusListener {

    /** Property for the tooltip component change */
    public static final String PROP_TOOL_TIP = "toolTip";

    /** Property for the tooltip text change */
    public static final String PROP_TOOL_TIP_TEXT = "toolTipText";

    /** Property for the visibility status change. */
    public static final String PROP_STATUS = "status";
    
    /** Property for the enabled flag change */
    public static final String PROP_ENABLED = "enabled";

    /** Property for the initial delay change */
    public static final String PROP_INITIAL_DELAY = "initialDelay";

    /** Property for the dismiss delay change */
    public static final String PROP_DISMISS_DELAY = "dismissDelay";

    private static final String UI_PREFIX = "ToolTip"; // NOI18N

    /** Initial delay before the tooltip is shown in milliseconds. */
    public static final int INITIAL_DELAY = 1000;

    /** Delay after which the tooltip will be hidden automatically
     * in milliseconds.
     */
    public static final int DISMISS_DELAY = 60000;
    
    /** Status indicating that  the tooltip is not showing on the screen. */
    public static final int STATUS_HIDDEN = 0;
    /** Status indicating that  the tooltip is not showing on the screen
     * but once either the {@link #setToolTipText(java.lang.String)}
     * or {@link #setToolTip(javax.swing.JComponent)} gets called
     * the tooltip will become visible.
     */
    public static final int STATUS_VISIBILITY_ENABLED = 1;
    /** Status indicating that the tooltip is visible
     * because {@link #setToolTipText(java.lang.String)}
     * was called.
     */
    public static final int STATUS_TEXT_VISIBLE = 2;
    /** Status indicating that the tooltip is visible
     * because {@link #setToolTip(javax.swing.JComponent)}
     * was called.
     */
    public static final int STATUS_COMPONENT_VISIBLE = 3;
    
    /** Extra height added to the rectangle of modelToView() for mouse
     * cursor coordinates.
     */
    private static final int MOUSE_EXTRA_HEIGHT = 5;

    private ExtEditorUI extEditorUI;

    private JComponent toolTip;

    private String toolTipText;
    
    private Timer enterTimer;

    private Timer exitTimer;

    private boolean enabled;
    
    /** Status of the tooltip visibility. */
    private int status;

    private MouseEvent lastMouseEvent;

    private PropertyChangeSupport pcs;


    /** Construct new support for tooltips.
     */
    public ToolTipSupport(ExtEditorUI extEditorUI) {
        this.extEditorUI = extEditorUI;

        enterTimer = new Timer(INITIAL_DELAY, new WeakTimerListener(this));
        enterTimer.setRepeats(false);
        exitTimer = new Timer(DISMISS_DELAY, new WeakTimerListener(this));
        exitTimer.setRepeats(false);

        Settings.addSettingsChangeListener(this);
        extEditorUI.addPropertyChangeListener(this);

        setEnabled(true);
    }

    /** @return the component that either contains the tooltip
     * or is responsible for displaying of text tooltips.
     */
    public final JComponent getToolTip() {
        if (toolTip == null) {
            setToolTip(createDefaultToolTip());
        }

        return toolTip;
    }
    
    /** Set the tooltip component.
     * It can be called either to set the custom component
     * that will display the text tooltips or to display
     * the generic component with the tooltip after
     * the tooltip timer has fired.
     * @param toolTip component that either contains the tooltip
     *  or that will display a text tooltip.
     */
    public void setToolTip(JComponent toolTip) {
        JComponent oldToolTip = this.toolTip;
        this.toolTip = toolTip;

        if (status >= STATUS_VISIBILITY_ENABLED) {
            ensureVisibility();
        }

        firePropertyChange(PROP_TOOL_TIP, oldToolTip, this.toolTip);
    }

    /** Create the default tooltip component.
     */
    protected JComponent createDefaultToolTip() {
        return createTextToolTip();
    }
    
    private JTextArea createTextToolTip() {
        JTextArea tt = new JTextArea() {
            public void setSize(int width, int height) {
                int docLen = getDocument().getLength();
                if (docLen > 0) { // nonzero length
                    setLineWrap(false);
                    Dimension prefSize = getPreferredSize();
                    if (width > prefSize.width) { // given width unnecessarily big
                        width = prefSize.width; // shrink the width to preferred
                        if (height >= prefSize.height) {
                            height = prefSize.height;
                        } else { // height not big enough
                            height = -1;
                        }
                        
                    } else { // available width not enough - wrap lines
                        setLineWrap(true);
                        super.setSize(width, 100000);
                        try {
                            Rectangle r = modelToView(docLen - 1);
                            int prefHeight = r.y + r.height;
                            if (prefHeight < height) {
                                height = prefHeight;
                                
                            } else { // the given height is too small
                                height = -1;
                            }
                        } catch (BadLocationException e) {
                        }
                    }
                }

                if (height >= 0) { // only for valid height
                    super.setSize(width, height);
                } else { // signal that the height is too small to display tooltip
                    putClientProperty(PopupManager.Placement.class, null);
                }
            }
        };

        Font font = UIManager.getFont(UI_PREFIX + ".font"); // NOI18N
        Color backColor = UIManager.getColor(UI_PREFIX + ".background"); // NOI18N
        Color foreColor = UIManager.getColor(UI_PREFIX + ".foreground"); // NOI18N

        if (font != null) {
            tt.setFont(font);
        }
        if (foreColor != null) {
            tt.setForeground(foreColor);
        }
        if (backColor != null) {
            tt.setBackground(backColor);
        }

        tt.setOpaque(true);
        tt.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tt.getForeground()),
            BorderFactory.createEmptyBorder(0, 3, 0, 3)
        ));

        return tt;
    }

    public void settingsChange(SettingsChangeEvent evt) {
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (extEditorUI.COMPONENT_PROPERTY.equals(propName)) {
            JTextComponent component = (JTextComponent)evt.getNewValue();
            if (component != null) { // just installed

                component.addPropertyChangeListener(this);
                
                disableSwingToolTip(component);

                component.addFocusListener(this);
                if (component.hasFocus()) {
                    focusGained(new FocusEvent(component, FocusEvent.FOCUS_GAINED));
                }
                
            } else { // just deinstalled
                component = (JTextComponent)evt.getOldValue();

                component.removeFocusListener(this);
                component.removePropertyChangeListener(this);

            }
        }
        
        if (JComponent.TOOL_TIP_TEXT_KEY.equals(propName)) {
            JComponent component = (JComponent)evt.getSource();
            disableSwingToolTip(component);
            
            componentToolTipTextChanged(evt);
        }
                        
    }

    private void disableSwingToolTip(final JComponent component) {
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    // Prevent default swing tooltip manager
                    javax.swing.ToolTipManager.sharedInstance().unregisterComponent(component);
                    
                    // Also disable the swing tooltip manager on gutter component
                    GlyphGutter gg = extEditorUI.getGlyphGutter();
                    if (gg != null) {
                        javax.swing.ToolTipManager.sharedInstance().unregisterComponent(gg);
                    }
                }
            }
        );
    }
    
    /** Update the tooltip by running corresponding action
     * {@link ExtKit#buildToolTipAction}. This method gets
     * called once the enterTimer fires and it can be overriden
     * by children.
     */
    protected void updateToolTip() {
        ExtEditorUI ui = extEditorUI;
        if (ui == null)
            return;
        JTextComponent comp = ui.getComponent();
        if (comp == null)
            return;
        
        if (isGlyphGutterMouseEvent(lastMouseEvent)) {
            setToolTipText(extEditorUI.getGlyphGutter().getToolTipText(lastMouseEvent));
        } else { // over the text component
            BaseKit kit = Utilities.getKit(comp);
            if (kit != null) {
                Action a = kit.getActionByName(ExtKit.buildToolTipAction);
                if (a != null) {
                    a.actionPerformed(new ActionEvent(comp, 0, "")); // NOI18N
                }
            }
        }
    }

    /** Set the visibility of the tooltip.
     * @param visible whether tooltip should become visible or not.
     *  If true the status is changed
     * to {@link { #STATUS_VISIBILITY_ENABLED}
     * and @link #updateToolTip()}  is called.<BR>
     * It is still possible that the tooltip will not be showing
     * on the screen in case the tooltip or tooltip text are left
     * unchanged.
     */
    protected void setToolTipVisible(boolean visible) {
        if (!visible) { // ensure the timers are stopped
            enterTimer.stop();
            exitTimer.stop();
        }

        if (visible && status < STATUS_VISIBILITY_ENABLED
            || !visible && status >= STATUS_VISIBILITY_ENABLED
        ) {
            if (visible) { // try to show the tooltip
                if (enabled) {
                    setStatus(STATUS_VISIBILITY_ENABLED);

                    updateToolTip();
                }

            } else { // hide tip
                if (toolTip != null) {
                    toolTip.setVisible(false);
                }

                setStatus(STATUS_HIDDEN);
            }
        }
    }
    
    /** @return Whether the tooltip is showing on the screen.
     * {@link #getStatus() } gives the exact visibility state.
     */
    public boolean isToolTipVisible() {
        return status > STATUS_VISIBILITY_ENABLED;
    }
    
    /** @return status of the tooltip visibility. It can
     * be {@link #STATUS_HIDDEN}
     * or {@link #STATUS_VISIBILITY_ENABLED}
     * or {@link #STATUS_TEXT_VISIBLE}
     * or {@link #STATUS_COMPONENT_VISIBLE}.
     */
    public final int getStatus() {
        return status;
    }
    
    private void setStatus(int status) {
        if (this.status != status) {
            int oldStatus = this.status;
            this.status = status;
            firePropertyChange(PROP_STATUS,
                new Integer(oldStatus), new Integer(this.status));
        }
    }

    /** @return the current tooltip text.
     */
    public String getToolTipText() {
        return toolTipText;
    }
    
    /** Set the tooltip text to make the tooltip
     * to be shown on the screen.
     * @param text tooltip text to be displayed.
     */
    public void setToolTipText(String text) {
        String oldText = toolTipText;
        toolTipText = text;

        firePropertyChange(PROP_TOOL_TIP_TEXT,  oldText, toolTipText);
        
        if (toolTipText != null) {
            JTextArea ta = createTextToolTip();
            ta.setText(toolTipText);
            setToolTip(ta);
            
        } else { // null text
            if (status == STATUS_TEXT_VISIBLE) {
                setToolTipVisible(false);
            }
        }
        
    }
    
    private void applyToolTipText() {
        JComponent tt = getToolTip();
        if (tt != null) {
            if (tt instanceof JLabel) {
                ((JLabel)tt).setText(toolTipText);

            } else if (tt instanceof JTextComponent) {
                ((JTextComponent)tt).setText(toolTipText);

            } else if (tt instanceof javax.swing.JToolTip) {
                ((javax.swing.JToolTip)tt).setTipText(toolTipText);

            } else {
                try {
                    java.lang.reflect.Method m = tt.getClass().getMethod("setText",
                        new Class[] { String.class });
                    if (m != null) {
                        m.invoke(toolTip, new Object[] { toolTipText });
                    }
                } catch (NoSuchMethodException e) {
                } catch (IllegalAccessException e) {
                } catch (java.lang.reflect.InvocationTargetException e) {
                }
            }
        }
    }
    
    private boolean isGlyphGutterMouseEvent(MouseEvent evt) {
        return (evt != null && evt.getSource() == extEditorUI.getGlyphGutter());
    }

    private void ensureVisibility() {
        // Find the visual position in the document
        JTextComponent component = extEditorUI.getComponent();
        if (component != null) {
            // Try to display the tooltip above (or below) the line it corresponds to
            int pos = component.viewToModel(getLastMouseEventPoint());
            Rectangle cursorBounds = null;
            if (pos >= 0) {
                try {
                    cursorBounds = component.modelToView(pos);
                    // Enlarge the height slightly to not interfere with mouse cursor
                    cursorBounds.y -= MOUSE_EXTRA_HEIGHT;
                    cursorBounds.height += 2 * MOUSE_EXTRA_HEIGHT; // above and below

                } catch (BadLocationException e) {
                }
            }
            if (cursorBounds == null) { // get mose rect
                cursorBounds = new Rectangle(getLastMouseEventPoint(), new Dimension(1, 1));
            }

            // updateToolTipBounds();
            PopupManager pm = extEditorUI.getPopupManager();
            pm.install(toolTip, cursorBounds, PopupManager.AbovePreferred);
        }
        exitTimer.restart();
    }

    /** Helper method to get the identifier
     * under the mouse cursor.
     * @return string containing identifier under
     * mouse cursor.
     */
    public String getIdentifierUnderCursor() {
        String word = null;
        if (!isGlyphGutterMouseEvent(lastMouseEvent)) {
            try {
                JTextComponent component = extEditorUI.getComponent();
                BaseTextUI ui = (BaseTextUI)component.getUI();
                Point lmePoint = getLastMouseEventPoint();
                int pos = ui.viewToModel(component, lmePoint);
                if (pos >= 0) {
                    BaseDocument doc = (BaseDocument)component.getDocument();
                    int eolPos = Utilities.getRowEnd(doc, pos);
                    Rectangle eolRect = ui.modelToView(component, eolPos);
                    int lineHeight = extEditorUI.getLineHeight();
                    if (lmePoint.x <= eolRect.x && lmePoint.y <= eolRect.y + lineHeight) {
                        word = Utilities.getIdentifier(doc, pos);
                    }
                }
            } catch (BadLocationException e) {
                // word will be null
            }
        }

        return word;
    }

    /** @return whether the tooltip support is enabled. If it's
     * disabled the tooltip does not become visible.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /** Set whether the tooltip support is enabled. If it's
     * disabled the tooltip does not become visible.
     * @param enabled whether the tooltip will be enabled or not.
     */
    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;

            firePropertyChange(PROP_ENABLED,
                enabled ? Boolean.FALSE : Boolean.TRUE,
                enabled ? Boolean.TRUE : Boolean.FALSE
            );

            if (!enabled) {
                setToolTipVisible(false);
            }
        }
    }

    /** @return the delay between stopping
     * mouse movement and displaying
     * of the tooltip in milliseconds.
     */
    public int getInitialDelay() {
        return enterTimer.getDelay();
    }

    /** Set the delay between stopping
     * mouse movement and displaying
     * of the tooltip in milliseconds.
     */
    public void setInitialDelay(int delay) {
        if (enterTimer.getDelay() != delay) {
            int oldDelay = enterTimer.getDelay();
            enterTimer.setDelay(delay);

            firePropertyChange(PROP_INITIAL_DELAY,
                new Integer(oldDelay), new Integer(enterTimer.getDelay()));
        }
    }

    /** @return the delay between displaying
     * of the tooltip and its automatic hiding
     * in milliseconds.
     */
    public int getDismissDelay() {
        return exitTimer.getDelay();
    }

    /** Set the delay between displaying
     * of the tooltip and its automatic hiding
     * in milliseconds.
     */
    public void setDismissDelay(int delay) {
        if (exitTimer.getDelay() != delay) {
            int oldDelay = exitTimer.getDelay();
            exitTimer.setDelay(delay);
            
            firePropertyChange(PROP_DISMISS_DELAY,
                new Integer(oldDelay), new Integer(exitTimer.getDelay()));
        }
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == enterTimer) {
            setToolTipVisible(true);

        } else if (evt.getSource() == exitTimer) {
            setToolTipVisible(false);
        }
    }

    public void mouseClicked(MouseEvent evt) {
        lastMouseEvent = evt;
        setToolTipVisible(false);
    }

    public void mousePressed(MouseEvent evt) {
        lastMouseEvent = evt;
        setToolTipVisible(false);
    }

    public void mouseReleased(MouseEvent evt) {
        lastMouseEvent = evt;
        setToolTipVisible(false);
    }

    public void mouseEntered(MouseEvent evt) {
        lastMouseEvent = evt;
    }

    public void mouseExited(MouseEvent evt) {
        lastMouseEvent = evt;
        setToolTipVisible(false);
    }

    public void mouseDragged(MouseEvent evt) {
        lastMouseEvent = evt;
        setToolTipVisible(false);
    }

    public void mouseMoved(MouseEvent evt) {
        setToolTipVisible(false);
        if (enabled) {
            enterTimer.restart();
            
        }
        lastMouseEvent = evt;
    }

    /** @return last mouse event captured by this support.
     * This method can be used by the action that evaluates
     * the tooltip.
     */
    public final MouseEvent getLastMouseEvent() {
        return lastMouseEvent;
    }
    
    /** Possibly do translation when over the gutter.
     */
    private Point getLastMouseEventPoint() {
        Point p = null;
        MouseEvent lme = lastMouseEvent;
        if (lme != null) {
            p = lme.getPoint();
            if (lme.getSource() == extEditorUI.getGlyphGutter()) {
                // Over glyph gutter - change coords
                JTextComponent c = extEditorUI.getComponent();
                if (c != null) {
                    if (c.getParent() instanceof JViewport) {
                        JViewport vp = (JViewport)c.getParent();
                        p = new Point(vp.getViewPosition().x, p.y);
                    }
                }
            }
        }

        return p;
    }
                
                

    /** Called automatically when the
     * {@link javax.swing.JComponent#TOOL_TIP_TEXT_KEY}
     * property of the corresponding editor component
     * gets changed.<BR>
     * By default it calls {@link #setToolTipText(java.lang.String)}
     * with the new tooltip text of the component.
     */
    protected void componentToolTipTextChanged(PropertyChangeEvent evt) {
        JComponent component = (JComponent)evt.getSource();
        setToolTipText(component.getToolTipText());
    }

    private PropertyChangeSupport getPCS() {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        return pcs;
    }

    /** Add the listener for the property changes. The names
     * of the supported properties are defined
     * as "PROP_" public static string constants.
     * @param listener listener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPCS().addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getPCS().removePropertyChangeListener(listener);
    }
    
    /** Fire the change of the given property.
     * @param propertyName name of the fired property
     * @param oldValue old value of the property
     * @param newValue new value of the property.
     */
    protected void firePropertyChange(String propertyName,
    Object oldValue, Object newValue) {
        getPCS().firePropertyChange(propertyName, oldValue, newValue);
    }
    
    public void focusGained(FocusEvent e) {
        JComponent component = (JComponent)e.getSource();
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        GlyphGutter gg = extEditorUI.getGlyphGutter();
        if (gg != null) {
            gg.addMouseListener(this);
            gg.addMouseMotionListener(this);
        }
    }

    public void focusLost(FocusEvent e) {
        JComponent component = (JComponent)e.getSource();
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        GlyphGutter gg = extEditorUI.getGlyphGutter();
        if (gg != null) {
            gg.removeMouseListener(this);
            gg.removeMouseMotionListener(this);
        }
        setToolTipVisible(false);
    }

}
