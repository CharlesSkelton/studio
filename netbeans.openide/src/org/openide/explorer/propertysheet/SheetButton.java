/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */


package org.openide.explorer.propertysheet;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Vector;
import javax.swing.border.EmptyBorder;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.openide.awt.MouseUtils;
import org.openide.util.Utilities;


/**
 * A lightweight component creating a labelled button. 
 * Can be "plastic", which means that when a mouse
 * enters the button, the area of the button is stubbed. 
 * <code>SheetButton</code> can contain one inner component.
 *
 * @author   Jan Jancura
 */
class SheetButton extends JPanel {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -5681433767155127558L;

    /** Action command idicating the right mouse button was clicked. */
    static final String RIGHT_MOUSE_COMMAND = "rightMouseActionCommand"; // NOI18N
    
    // variables .................................................................

    /** There are all listeners stored. */
    private transient Vector listeners = new Vector (1,5);
    /** There are action command string stored. Default value is "click". */
    private String actionCommand = "click"; // NOI18N
    /** Label of this button. */
    private String label;
    /** Preferred size of this button. */
    private Dimension preferredSize;
    /** State of button. */
    private boolean pressed;
    /** True if button can receive focus. */
    private boolean focusTransferable;
    /** True is is in "plastic" mode. */
    private boolean isPlastic;
    /** Listens on focus/key/mouse events. */
    private IL innerListener;
    /** <code>SheetButton</code> can contain one inner component. */
    private JComponent innerComponent;
    /** Is true if mouseEntered and mouseExited actions should be propagated. */
    private boolean plasticNotify;
    /** Active foreground color. */
    private Color foreground;
    /** Inactive foreground color. */
    private Color inForeground;
    /** Flag indicating invalidity. Invalid mark. */
    private boolean invalidMark;
    /** Flag indicating modified status */
    private boolean modifiedMark;
    /** Flag indicating that button is used as renderer. */
    private boolean flat;

    // init .................................................................

    /**
     * Construct a button with empty label.
     */
    public SheetButton() {
        this("", false, false); // NOI18N
    }

    /**
     * Construct a button with label and standard appearance (not plastic).
     *
     * @param aLabel the label
     */
    public SheetButton (String aLabel) {
        this (aLabel, false, false);
    }

    /**
     * Construct a button with label.
     *
     * @param aLabel the label
     * @param boolean <code>true</code> if should use plastic appearance
     */
    public SheetButton (String aLabel, boolean isPlasticButton) {
        this (aLabel, isPlasticButton, false);
    }

    /**
     * Construct a button with label and possibly a plastic listener.
     * @param label the label
     * @param isPlasticButton <code>true</code> if should be plastic
     * @param plasticActionNotify <code>true</code> if mouse enter/exit events should be propagated
     */
    public SheetButton (String aLabel, boolean isPlasticButton, boolean plasticActionNotify) {
        setDoubleBuffered (false);
        setOpaque (true);
        label = aLabel == null ? "" : aLabel; // NOI18N
        isPlastic = isPlasticButton;
        plasticNotify = plasticActionNotify;
        innerListener = new IL ();
        addMouseListener (innerListener);
        addKeyListener (innerListener);
        foreground = UIManager.getColor ("controlText");
        inForeground = UIManager.getColor ("Button.disabledText");
    }


    // variables .................................................................

    /** Identifies whether or not this component can receive the focus.
     * Getter for <code>focusTransferble</code> property.
     * Overrides superclass method. 
     * @see #focusTransferable */
    public boolean isFocusTraversable () {
        return focusTransferable;
    }

    /**
     * Sets whether this component can receive the focus.
     * Setter of <code>focusTransferable</code> property.
     * @param ft whether to be able receive the focus
     * @see #isFocusTraversable
     */
    public void setFocusTraversable(boolean ft) {
        if (isFocusTraversable () == ft) {
            return;
        }
        
        focusTransferable = ft;
        if (ft) {
            addFocusListener (innerListener);
        } else {
            removeFocusListener (innerListener);
        }
    }

    /**
     * Sets tooltip test.
     */
    public void setToolTipText (String str) {
        // bugfix #26252, mark invalid value in a tool tip
        if (invalidMark)
            str = org.openide.util.NbBundle.getMessage (SheetButton.class,
                    "CTL_InvalidValue", str); // NOI18N
        if (innerComponent != null) innerComponent.setToolTipText (str);
        super.setToolTipText (str);
    }

    /**
     * Gets preferred size. Overrides superclass method.
     * @return Standart method returned preferredSize (depends on font size only).
     */
    public Dimension getPreferredSize () {
        if(preferredSize == null) {
            updatePreferredSize ();
        }
        
        return preferredSize == null ? new Dimension (10,10) : preferredSize;
    }

    /**
     * Sets the font of this component. Overrides superclass method. Adds updating
     * of preferred size.
     * @param aFont The font to become this component's font.
     */
    public void setFont (Font aFont) {
        super.setFont (aFont);
        updatePreferredSize ();
    }

    /**
     * Getter for <code>foreground</code> property.
     * @return the color
     */
    public Color getActiveForeground () {
        return foreground;
    }

    /**
     * Setter for <code>foreground</code> property.
     * @param color the color to set
     */
    public void setActiveForeground (Color color) {
        foreground = color;
        repaint();
    }

    /**
     * Getter for <code>inForeColor</code> property.
     * @return the color of inactive property
     */
    public Color getInactiveForeground () {
        return inForeground;
    }

    /**
     * Sets the inactive foreground color.
     * @param color the color
     */
    public void setInactiveForeground (Color color) {
        inForeground = color;
        repaint();
    }

    /**
     * Gets the label of this button.
     * @return the button's label.
     */
    public String getLabel () {
        return label;
    }

    /**
     * Sets the button's label to be the specified string.
     * @param label   the new label.
     */
    public void setLabel (String aLabel) {
        label = aLabel;
        if (isShowing()) {
            updatePreferredSize ();
            repaint ();
        }
    }

    /**
     * Set whether button should appear stubbed.
     * @param aPressed <code>true</code> if so
     */
    public void setPressed (boolean aPressed) {
        if (pressed == aPressed) return;
        pressed = aPressed;
        if (pressed)
            setBorder (new EmptyBorder (2, 2, 0, 0));
        else
            setBorder (new EmptyBorder (1, 1, 1, 1));
        repaint ();
    }

    /**
     * Test whether button is pressed.
     * @return <code>true</code> if so
     */
    public boolean isPressed () {
        return pressed;
    }

    /**
     * Set whether button is plastic.
     * @param plastic <code>true</code> if so
     */
    public void setPlastic (boolean plastic) {
        this.isPlastic = plastic;
        repaint ();
    }

    /**
     * Test whether button is plastic.
     * @return <code>true</code> if so
     */
    public boolean isPlastic () {
        return isPlastic;
    }

    /**
     * Set the action command.
     * @param command the new command, default is set to "click"
     */
    public void setActionCommand (String command) {
        actionCommand = command;
    }

    /**
     * Attaches component for this button.
     */
    public Component add (Component innerComponent) {
        setLayout (new BorderLayout ());
        setBorder (new EmptyBorder (1, 1, 1, 1));
        add (innerComponent, BorderLayout.CENTER);
        removeMouseListener (innerListener);
        innerComponent.addMouseListener (innerListener);
        return this.innerComponent = (JComponent) innerComponent;
    }

    /** Sets invalid. */
    void setInvalidMark(boolean invalid) {
        invalidMark = invalid;
        repaint();
    }
    
    void setModifiedMark(boolean modified) {
        modifiedMark = modified;
        repaint();
    }
    
    /** Sets flat flag. */
    void setFlat(boolean f) {
        flat = f;
    }

    /** Gets invalid image. */
    private Image getInvalidImage() {
        return Utilities.loadImage("org/openide/resources/propertysheet/invalid.gif"); //NOI18N
    }
    
    /** Gets modified image. */
    private Image getModifiedImage() {
        return Utilities.loadImage("org/openide/resources/propertysheet/modified.gif"); //NOI18N
    }
    
    /**
     * Recalculates preferred size.
     */
    private void updatePreferredSize () {
        Graphics g = getGraphics ();
        if (g == null) return;
        Font font = null;
        if ((font = getFont ()) == null) return;
        FontMetrics fontMetrics = g.getFontMetrics (font);
        preferredSize = new Dimension (
            fontMetrics.stringWidth (label) + 10,
            fontMetrics.getHeight () + 6
        );
    }

    /**
     * Standart methods painting SheetButton. Overrides superclass method.
     */
    public void paint (Graphics g) {
        super.paint (g);

        Dimension size = getSize ();
        Color color = g.getColor ();
        Font theFont = g.getFont ();

        int xShift = 0;
        if (invalidMark) {
            xShift += getInvalidImage().getWidth(null) + 2;
        }
        if (modifiedMark) {
            xShift += getModifiedImage().getWidth(null) + 2;
        }
        
        g.setFont (getFont ());
        FontMetrics fontMetrics = g.getFontMetrics ();

        if (flat || pressed) {
            if (innerComponent == null) {
                g.setColor (isEnabled () ? getActiveForeground () : getInactiveForeground ());
                g.drawString (
                    label,
                    6 + xShift,
                    (size.height - fontMetrics.getHeight ()) / 2 + 1 + fontMetrics.getMaxAscent ()
                );
                if (invalidMark) {
                    g.drawImage(
                        getInvalidImage(), 5,
                        (size.height - getInvalidImage ().getHeight (null)) / 2 + 1, null);
                }
                if (modifiedMark) {
                    g.drawImage(
                        getModifiedImage(), invalidMark ? getInvalidImage().getWidth(null) + 7 : 5,
                        (size.height - getModifiedImage ().getHeight (null)) / 2 + 1, null);
                }
            }
        } else {
            g.setColor (UIManager.getColor ("controlLtHighlight"));
            g.drawLine (0, 0, size.width - 1, 0);
            g.drawLine (0, 0, 0, size.height - 1);

            g.setColor (UIManager.getColor("controlDkShadow"));
            g.drawLine (size.width - 1, 0, size.width - 1, size.height - 1);
            g.drawLine (0, size.height - 1, size.width - 1, size.height - 1);

            if (innerComponent == null) {
                g.setColor (isEnabled () ? getActiveForeground () : getInactiveForeground ());
                g.drawString (
                    label,
                    5 + xShift,
                    (size.height - fontMetrics.getHeight ()) / 2 + fontMetrics.getMaxAscent ()
                );
                if (invalidMark) {
                    g.drawImage(
                        getInvalidImage(), 4, 
                        (size.height - getInvalidImage().getHeight(null)) / 2, null);
                }
                if (modifiedMark) {
                    g.drawImage(
                        getModifiedImage(), invalidMark ? getInvalidImage().getWidth(null) + 6 : 4, 
                        (size.height - getModifiedImage().getHeight(null)) / 2, null);
                }
            }
        }

        if(hasFocus()) {
            g.setColor (UIManager.getColor("Button.focus")); // NOI18N
            g.drawRect(2, 1, size.width - 5, size.height - 4);
        }                
        g.setFont (theFont);
        g.setColor (color);
    }

    /**
     * Returns string representation of this class.
     * @return <code>String</code> Representation of this class.
     */
    public String toString () {
        return getClass ().getName () + "[ \"" + label + "\" ]"; // NOI18N
    }


    // SheetButtonListener support ......................................................

    /**
     * Adds <code>SheetButtonListener</code>.
     */
    void addSheetButtonListener (SheetButtonListener sheetButtonListener) {
        if (!listeners.contains(sheetButtonListener)) {
            listeners.addElement (sheetButtonListener);
        }
    }

    /**
     * Removes <code>SheetButtonListener</code>.
     */
    void removeSheetButtonListener (SheetButtonListener sheetButtonListener) {
        listeners.removeElement (sheetButtonListener);
    }

    /** Notifies listeners button was clicked. */
    public void notifySheetButtonListenersAboutClick (ActionEvent e) {
        Vector l = (Vector) listeners.clone ();
        int i, k = l.size ();
        for (i = 0; i < k; i++)
            ((SheetButtonListener) l.elementAt (i)).sheetButtonClicked (e);
    }

    /** Notifies listeners button was entered. */
    public void notifySheetButtonListenersAboutEntered (ActionEvent e) {
        Vector l = (Vector) listeners.clone ();
        int i, k = l.size ();
        for (i = 0; i < k; i++)
            ((SheetButtonListener) l.elementAt (i)).sheetButtonEntered (e);
    }

    /** Notifies listeners buttoin was exited. */
    public void notifySheetButtonListenersAboutExited (ActionEvent e) {
        Vector l = (Vector) listeners.clone ();
        int i, k = l.size ();
        for (i = 0; i < k; i++)
            ((SheetButtonListener) l.elementAt (i)).sheetButtonExited (e);
    }


    // innerclasses ..........................................................................

    /** Mouse, key and focus listener. */
    private class IL extends MouseMotionAdapter implements
        MouseListener, KeyListener, FocusListener {
	
	IL() {}

        /** Dummy implementation of <code>MouseListener</code> interface method. */
        public void mouseClicked (MouseEvent e) {}

        /** Implements <code>MouseListener</code> interface method. */
        public void mousePressed (MouseEvent e) {
            if (!isPlastic) {
                pressed = true;
                setBorder (new EmptyBorder (2, 2, 0, 0));
                notifySheetButtonListenersAboutEntered (
                    new ActionEvent (
                        SheetButton.this,
                        ActionEvent.ACTION_FIRST,
                        actionCommand
                    )
                );
                if (innerComponent != null) {
                    // setBorder (new EmptyBorder (2, 2, 0, 0));
                    invalidate ();
                    if (getParent() != null) {
                        getParent ().validate ();
                    }
                    innerComponent.addMouseMotionListener (innerListener);
                } else
                    addMouseMotionListener (innerListener);
                repaint ();
            }
        }

        /** Implements <code>MouseListener</code> interface method. */
        public void mouseReleased (MouseEvent e) {
            boolean isRightClick = MouseUtils.isRightMouseButton(e);
            
            if (innerComponent != null) {
                innerComponent.removeMouseMotionListener (innerListener);
                // setBorder (new EmptyBorder (1, 1, 1, 1));
                invalidate ();
                if (getParent() != null) {
                    getParent ().validate ();
                }
            } else
                removeMouseMotionListener (innerListener);
            repaint ();
            if(isEnabled ()) {
                notifySheetButtonListenersAboutClick (
                    new ActionEvent (
                        SheetButton.this,
                        ((e.getClickCount () % 2) == 1) ?
                        (ActionEvent.ACTION_FIRST + 1) :
                        ActionEvent.ACTION_FIRST + 2,
                        isRightClick ? RIGHT_MOUSE_COMMAND : actionCommand
                    )
                );
                notifySheetButtonListenersAboutExited (
                    new ActionEvent (
                        SheetButton.this,
                        ActionEvent.ACTION_FIRST,
                        actionCommand
                    )
                );
            }
            pressed = false;
            setBorder (new EmptyBorder (1, 1, 1, 1));
        }

        /** Implements <code>MouseListener</code> interface method. */
        public void mouseEntered (MouseEvent e) {
            if (isPlastic) {
                pressed = true;
                setBorder (new EmptyBorder (2, 2, 0, 0));
                repaint ();
                if (plasticNotify)
                    notifySheetButtonListenersAboutEntered (
                        new ActionEvent (
                            SheetButton.this,
                            ActionEvent.ACTION_FIRST,
                            actionCommand
                        )
                    );
            }
        }

        /** Implements <code>MouseExited</code> interface method. */
        public void mouseExited (MouseEvent e) {
            if (isPlastic) {
                pressed = false;
                setBorder (new EmptyBorder (1, 1, 1, 1));
                repaint ();
                if (plasticNotify)
                    notifySheetButtonListenersAboutExited (
                        new ActionEvent (
                            SheetButton.this,
                            ActionEvent.ACTION_FIRST,
                            actionCommand
                        )
                    );
            }
        }

        /** Overrides superclass method. */
        public void mouseDragged (MouseEvent e) {
            if(new Rectangle(SheetButton.this.getSize())
                .contains(e.getPoint()) == pressed) {
                    return;
            }
            
            if (pressed) {
                notifySheetButtonListenersAboutExited (
                    new ActionEvent (
                        SheetButton.this,
                        ActionEvent.ACTION_FIRST,
                        actionCommand
                    ));
            } else {
                notifySheetButtonListenersAboutEntered (
                    new ActionEvent (
                        SheetButton.this,
                        ActionEvent.ACTION_FIRST,
                        actionCommand
                    ));
            }
            
            pressed = !pressed;
            if (pressed) {
                setBorder (new EmptyBorder (2, 2, 0, 0));
            } else {
                setBorder (new EmptyBorder (1, 1, 1, 1));
            }
            
            if (innerComponent != null) {
                invalidate ();
                if (getParent() != null) {
                    getParent ().validate ();
                }
            }
            
            repaint ();
        }

        /** Implements <code>KeyListener</code> interface method. */
        public void keyTyped (KeyEvent e) {
        }

        /** Implements <code>KeyListener</code> interface method. */
        public void keyPressed (KeyEvent e) {
            if (e.isControlDown())
                return;

            if ((e.getKeyCode() == KeyEvent.VK_SPACE) ||
                (e.getKeyCode() == KeyEvent.VK_ENTER)) {
                notifySheetButtonListenersAboutClick (
                    new ActionEvent (
                        SheetButton.this,
                        ActionEvent.ACTION_FIRST + 1,
                        actionCommand
                    )
                );
            }
            
            if(e.getKeyCode () == KeyEvent.VK_DOWN) {
                FocusManager fm = FocusManager.getCurrentManager ();
                fm.focusNextComponent(SheetButton.this);
                e.consume();
            } else {
                if(e.getKeyCode () == KeyEvent.VK_UP) {
                    FocusManager fm = FocusManager.getCurrentManager ();
                    fm.focusPreviousComponent(SheetButton.this);
                    e.consume();
                }
            }
        }

        /** Dummy implementation of <code>KeyListener</code> interface method. */
        public void keyReleased (KeyEvent e) {
        }

        /** Implements <code>FocusListener</code> interface method. */
        public void focusGained (FocusEvent fe) {
            notifySheetButtonListenersAboutEntered (
                new ActionEvent (
                    SheetButton.this,
                    ActionEvent.ACTION_FIRST + 1,
                    actionCommand
                )
            );
            
            Component c = SheetButton.this;
            Rectangle r = SheetButton.this.getBounds();
            while ((c != null) && (!(c instanceof javax.swing.JViewport))) {
                c = c.getParent();
                if (c != null) {
                    r.x += c.getX();
                    r.y += c.getY();
                }
            }
            if (c != null) {
                javax.swing.JViewport jvp = (javax.swing.JViewport)c;
                jvp.scrollRectToVisible(r);
            }
            
            repaint ();
        }

        /** Implements <code>FocusListener</code> interface method. */
        public void focusLost (FocusEvent fe) {
            if(isPlastic) {
                pressed = false;
                setBorder(new EmptyBorder (1, 1, 1, 1));
            }
            
            notifySheetButtonListenersAboutExited (
                new ActionEvent (
                    SheetButton.this,
                    ActionEvent.ACTION_FIRST + 1,
                    actionCommand
                )
            );
            
            repaint ();
        }
    } // End of class IL.

    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleSheetButton();
        }
        return accessibleContext;
    }

    class AccessibleSheetButton extends AccessibleJComponent {

        public javax.accessibility.AccessibleRole getAccessibleRole() {
            return javax.accessibility.AccessibleRole.PUSH_BUTTON;
        }
        
        public String getAccessibleName() {
            String name = super.getAccessibleName();
            
            if (name == null && innerComponent != null) {
                name = innerComponent.getAccessibleContext().getAccessibleName();
            }
            
            if (name == null && label != null) {
                name = label;
            }
            
            return org.openide.util.NbBundle.getMessage(SheetButton.class,
		    "ACS_SheetButton", new Integer(invalidMark ? 0 : 1), name); // NOI18N
        }
        
        public String getAccessibleDescription() {
            String name = super.getAccessibleDescription();
            
            if (name == null && innerComponent != null) {
                name = innerComponent.getAccessibleContext().getAccessibleDescription();
            }
            
            if (name == null && label != null) {
                name = label;
            }
            
            return org.openide.util.NbBundle.getMessage(SheetButton.class,
		    "ACS_SheetButton", new Integer(invalidMark ? 0 : 1), name); // NOI18N
        }
    }
}
