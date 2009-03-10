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


package org.openide.explorer.propertysheet;


import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.event.*;
import java.beans.PropertyEditor;
import java.util.Vector;


/**
 * This lightweight component encapsulates calling of propertyEditor.paintValue
 * (Graphics g, Rectangle r) method in special Component.
 *
 * @author   Jan Jancura
 * @version  0.16
 */
class PropertyShow extends javax.swing.JPanel {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -8663903931982719530L;

    /** Link to the property editor */
    private PropertyEditor propertyEditor;

    /** Standart helper variable. */
    private Vector listeners = new Vector (1,5);

    /**
     * Constructs new PropertyShow for specified PropertyEditor.
     *
     * @param PropertyEditor aPropertyEditor proper property editor
     */
    public PropertyShow (PropertyEditor aPropertyEditor) {
        propertyEditor = aPropertyEditor;
        setDoubleBuffered (false);
        setOpaque (true);
    }

    /**
     * Sets value of showen property.
     */
    public void setValue (Object newValue) {
        propertyEditor.setValue (newValue);
    }

    /**
     * Standart method for painting component.
     */
    public void paintComponent (Graphics g) {
        Dimension sz = getSize();
        Color color = g.getColor();
        g.setColor(getBackground());
        g.fillRect(0, 0, sz.width, sz.height);

        Rectangle r = new Rectangle();
        r.setBounds(4, 0, sz.width-4, sz.height);

        // XXX Use foreground color of PropertyShow to reflect r/w v. r/o
        // property, see PropertyPanel -> use of canWrite property. 
        g.setColor(getForeground());
        propertyEditor.paintValue (g, r);
        
        g.setColor(color);
        
        if(hasFocus()) {
            g.setColor(javax.swing.UIManager.getColor("Button.focus")); // NOI18N
            g.drawRect(1, 1, sz.width - 3, sz.height - 3);
        }
    }

    /** Sets <code>propertyEditor</code>. */
    public void setEditor(PropertyEditor aEditor) {
        propertyEditor = aEditor;
    }
    
    /** Fires sheet button was cliced. */
    void fireSheetButtonClicked (ActionEvent e) {
        Vector l = (Vector)listeners.clone ();
        int i, k = l.size ();
        for (i = 0; i < k; i++)
            ((SheetButtonListener)l.elementAt (i)).sheetButtonClicked (e);
    }

    /** Adds <code>SheetButtonListener</code>. */
    public void addSheetButtonListener (SheetButtonListener sheetListener) {
        listeners.addElement (sheetListener);
    }

    /** Removes <code>SheetButtonListener</code>. */
    public void removeSheetButtonListener (SheetButtonListener sheetListener) {
        listeners.removeElement (sheetListener);
    }
    
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessiblePropertyShow();
        }
        return accessibleContext;
    }

    private class AccessiblePropertyShow extends AccessibleJPanel {
        AccessiblePropertyShow() {}
        public String getAccessibleName() {
            return propertyEditor.getAsText();
        }
    }
}
