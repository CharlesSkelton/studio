/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.explorer.propertysheet;

import java.awt.Image;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.ImageIcon;

import org.openide.awt.Toolbar;
import org.openide.awt.ToolbarButton;
import org.openide.awt.ToolbarToggleButton;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Toolbar panel for the PropertySheet.
 * @author  David Strupl
 */
class PropertySheetToolbar extends javax.swing.JPanel
implements ActionListener, PropertyChangeListener {
    
    /** References back the "parent" property sheet. */
    private PropertySheet mySheet;
    /** Set of buttons. */
    private ToolbarToggleButton   bNoSort, bAlphaSort, bTypeSort, bDisplayWritableOnly;
    private ToolbarButton         customizer;
    /** Show help on the active property sheet tab (Node.PropertySet) if applicable.
     * Does not show node- nor property-level help.
     * @see "#20794"
     */
    private ToolbarButton         help;
    
    /** When firing back to the PropertySheet we should not react to changes -
     * - this should prevent the loop.
     */
    private boolean ignorePropertyChange = false;
    
    /** Creates new PropertySheetToolbar */
    public PropertySheetToolbar(PropertySheet p) {
        mySheet = p;
        
        mySheet.addPropertyChangeListener(this);
        
        // Toolbar
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(bNoSort = new ToolbarToggleButton(new ImageIcon(Utilities.loadImage(
            "org/openide/resources/propertysheet/unsorted.gif")))); // NOI18N
        
        bNoSort.getAccessibleContext().setAccessibleName(getString("ACS_CTL_NoSort"));
        bNoSort.setToolTipText(getString("CTL_NoSort"));
        bNoSort.setSelected(true);
        bNoSort.addActionListener(this);
        
        add(bAlphaSort = new ToolbarToggleButton(new ImageIcon(Utilities.loadImage(
            "org/openide/resources/propertysheet/sortedByNames.gif")))); // NOI18N
        
        bAlphaSort.getAccessibleContext().setAccessibleName(getString("ACS_CTL_AlphaSort"));
        bAlphaSort.setToolTipText(getString("CTL_AlphaSort"));
        bAlphaSort.addActionListener(this);
        
        add(bTypeSort = new ToolbarToggleButton(new ImageIcon(Utilities.loadImage(
            "org/openide/resources/propertysheet/sortedByTypes.gif")))); // NOI18N
        
        bTypeSort.getAccessibleContext().setAccessibleName(getString("ACS_CTL_TypeSort"));
        bTypeSort.setToolTipText(getString("CTL_TypeSort"));
        bTypeSort.addActionListener(this);
        
        setSortingMode(mySheet.getSortingMode());
        
        Toolbar.Separator ts = new Toolbar.Separator();
        add(ts);
        ts.updateUI();
        
        bDisplayWritableOnly = new ToolbarToggleButton(
            new ImageIcon(Utilities.loadImage(
                "org/openide/resources/propertysheet/showWritableOnly.gif")), // NOI18N
            mySheet.getDisplayWritableOnly()
        );
        bDisplayWritableOnly.getAccessibleContext().setAccessibleName(getString("ACS_CTL_VisibleWritableOnly"));
        bDisplayWritableOnly.setToolTipText(getString("CTL_VisibleWritableOnly"));
        bDisplayWritableOnly.addActionListener(this);
        
        add(bDisplayWritableOnly);
        
        ts = new Toolbar.Separator();
        add(ts);
        ts.updateUI();
        
        add(customizer = new ToolbarButton(new ImageIcon(Utilities.loadImage(
            "org/openide/resources/propertysheet/customize.gif")))); // NOI18N
        
        customizer.getAccessibleContext().setAccessibleName(getString("ACS_CTL_Customize"));
        customizer.setToolTipText(getString("CTL_Customize"));
        customizer.setEnabled(false);
        customizer.addActionListener(this);
        
        ts = new Toolbar.Separator();
        add(ts);
        ts.updateUI();
        
        add(help = new ToolbarButton(new ImageIcon(Utilities.loadImage(
            "org/openide/resources/propertysheet/propertySheetHelp.gif")))); // NOI18N
        
        help.getAccessibleContext().setAccessibleName(getString("ACS_CTL_Help"));
        help.setToolTipText(getString("CTL_Help"));
        help.setEnabled(false);
        help.addActionListener(this);
    }

    
    /** Implements <code>ActionListener</code> interface.
     * Listens all toolbar buttons. */
    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        
        if(source == bNoSort) {
            setSortingMode(PropertySheet.UNSORTED);
        } else if(source == bAlphaSort) {
            setSortingMode(PropertySheet.SORTED_BY_NAMES);
        } else if(source == bTypeSort) {
            setSortingMode(PropertySheet.SORTED_BY_TYPES);
        } else if(source == customizer) {
            mySheet.invokeCustomization();
        } else if (source == help) {
            mySheet.invokeHelp();
        } else if(source == bDisplayWritableOnly) {
            ignorePropertyChange = true;
            try {
                mySheet.setDisplayWritableOnly(bDisplayWritableOnly.isSelected());
            } finally {
                ignorePropertyChange = false;
            }
        }
    }
    
    /** This setter calls it's counterpart in the master PropertySheet instance.
     */
    private void setSortingMode(int sortingMode) {
        ignorePropertyChange = true;
        try {
            mySheet.setSortingMode(sortingMode);
        } catch (PropertyVetoException pve) {
            PropertyDialogManager.notify(pve);
        } finally {
            ignorePropertyChange = false;
            bNoSort.setSelected (sortingMode == PropertySheet.UNSORTED);
            bAlphaSort.setSelected (sortingMode == PropertySheet.SORTED_BY_NAMES);
            bTypeSort.setSelected (sortingMode == PropertySheet.SORTED_BY_TYPES);
        }
    }
    
    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *  	and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (ignorePropertyChange) {
            return;
        }
        if (evt.getPropertyName() == null) {
            return;
        }
        if (evt.getPropertyName().equals(PropertySheet.PROPERTY_SORTING_MODE)) {
            setSortingMode(((Integer)evt.getNewValue()).intValue());
        }
        if (evt.getPropertyName().equals(PropertySheet.PROPERTY_DISPLAY_WRITABLE_ONLY)) {
          bDisplayWritableOnly.setSelected (((Boolean)evt.getNewValue()).booleanValue());
        }
        if (evt.getPropertyName().equals(PropertySheet.PROP_HAS_CUSTOMIZER)) {
            customizer.setEnabled(((Boolean)evt.getNewValue()).booleanValue());
        }
        if (evt.getPropertyName().equals(PropertySheet.PROP_PAGE_HELP_ID)) {
            help.setEnabled(mySheet.getPageHelpID() != null);
        }
    }
    
    /** Forces the icon to use BufferedImage */
    private static void toBufferedImage(ImageIcon icon) {
        Image img = createImage();
        Graphics g = img.getGraphics();
        g.drawImage(icon.getImage(), 0, 0, null);
        g.dispose();
        icon.setImage(img);
    }

    /** Creates BufferedImage 16x16 and Transparency.BITMASK */
    private static BufferedImage createImage() {
        ColorModel model = GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().getDefaultConfiguration().getColorModel(Transparency.BITMASK);
        BufferedImage buffImage = new BufferedImage(model, 
            model.createCompatibleWritableRaster(16, 16), model.isAlphaPremultiplied(), null);
        return buffImage;
    }
    
    private static String getString(String key) {
        return NbBundle.getBundle(PropertySheetToolbar.class).getString(key);
    }
}
