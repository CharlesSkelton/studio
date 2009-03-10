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

package org.openide.explorer.propertysheet;

import java.awt.Color;

import org.openide.options.SystemOption;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import javax.swing.UIManager;

/**
* Settings for the property sheet.
* @see PropertySheet
*
* @author Jan Jancura, Ian Formanek
* @version 0.11, May 16, 1998
*/
public class PropertySheetSettings extends SystemOption {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -3820718202747868830L;

    /** Property variables. */
    static int                             propertyPaintingStyle = PropertySheet.PAINTING_PREFERRED;
    static boolean                         plastic = false;
    /** When it's true only writable properties are showen. */
    static boolean                         displayWritableOnly = false;
    static int                             sortingMode = PropertySheet.SORTED_BY_NAMES;
    static Color                           valueColor = new Color (0, 0, 128);
    private static Color                   disabledColor;
    static PropertySheetSettings           propertySheetSettings = (PropertySheetSettings) PropertySheetSettings.findObject(PropertySheetSettings.class, true);

    static PropertySheetSettings getDefault () {
        return propertySheetSettings;
    }

    public String displayName () {
        return getString ("CTL_Property_sheet_option");
    }

    public HelpCtx getHelpCtx () {
        return new HelpCtx (PropertySheetSettings.class);
    }

    /*
    * Sets property showing mode.
    */
    public void setPropertyPaintingStyle (int style) {
        int oldValue = propertyPaintingStyle;
        propertyPaintingStyle = style;
        firePropertyChange (PropertySheet.PROPERTY_PROPERTY_PAINTING_STYLE, new Integer (oldValue), new Integer (propertyPaintingStyle));
    }

    /*
    * Returns mode of showing properties.
    *
    * @return <CODE>int</CODE> mode of showing properties.
    * @see #setExpert
    */
    public int getPropertyPaintingStyle () {
        return propertyPaintingStyle;
    }

    /*
    * Sets sorting mode.
    *
    * @param sortingMode New sorting mode.
    */
    public void setSortingMode (int sortingMode) {
        int oldValue = this.sortingMode;
        this.sortingMode = sortingMode;
        firePropertyChange (PropertySheet.PROPERTY_SORTING_MODE, new Integer (oldValue), new Integer (sortingMode));
    }

    /*
    * Returns sorting mode.
    *
    * @return Sorting mode.
    */
    public int getSortingMode () {
        return sortingMode;
    }

    /*
    * Sets buttons in sheet to be plastic.
    */
    public void setPlastic (boolean plastic) {
        boolean oldValue = this.plastic;
        this.plastic = plastic;
        firePropertyChange (PropertySheet.PROPERTY_PLASTIC,
                            oldValue ? Boolean.TRUE : Boolean.FALSE,
                            plastic ? Boolean.TRUE : Boolean.FALSE);
    }

    /*
    * Returns true if buttons in sheet are plastic.
    */
    public boolean getPlastic () {
        return plastic;
    }

    /*
    * Sets foreground color of values.
    */
    public void setValueColor (Color color) {
        Color oldValue = valueColor;
        valueColor = color;
        firePropertyChange (PropertySheet.PROPERTY_VALUE_COLOR, oldValue, valueColor);
    }

    /*
    * Gets foreground color of values.
    */
    public Color getValueColor () {
        return valueColor;
    }

    /*
    * Sets foreground color of disabled property.
    */
    public void setDisabledPropertyColor (Color color) {
        Color oldValue = disabledColor;
        disabledColor = color;
        firePropertyChange (
            PropertySheet.PROPERTY_DISABLED_PROPERTY_COLOR,
            oldValue,
            disabledColor
        );
    }

    /*
    * Gets foreground color of values.
    */
    public Color getDisabledPropertyColor () {
        if (disabledColor == null) {
            disabledColor = UIManager.getColor("textInactiveText");
        }
        return disabledColor;
    }

    /*
    * Setter method for visibleWritableOnly property. If is true only writable
    * properties are showen in propertysheet.
    */
    public void setDisplayWritableOnly (boolean b) {
        Boolean oldValue = displayWritableOnly ? Boolean.TRUE : Boolean.FALSE;
        displayWritableOnly = b;
        firePropertyChange (
            PropertySheet.PROPERTY_DISPLAY_WRITABLE_ONLY,
            oldValue,
            displayWritableOnly ? Boolean.TRUE : Boolean.FALSE
        );
    }

    /*
    * Getter method for visibleWritableOnly property. If is true only writable
    * properties are showen in propertysheet.
    */
    public boolean getDisplayWritableOnly () {
        return displayWritableOnly;
    }
    
    private static String getString(String key) {
        return NbBundle.getBundle(PropertySheetSettings.class).getString(key);
    }

}
