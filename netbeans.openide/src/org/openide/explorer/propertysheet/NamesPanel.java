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


import java.awt.Dimension;
import javax.swing.JPanel;


/**
 * This is continer which manages Components in one column. All components have the same size
 * which is setted by the first component's preferred size or by setter method
 * setItemHeight (int aHeight).
 *
 * @author   Jan Jancura, Jaroslav Tulach
 */
class NamesPanel extends JPanel {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 1620670226589808833L;

    /** Indicates whether the panel has its own focus cycle. */
    private boolean focusCycleRoot;
    

    /**
     * Construct NamesPanel.
     */
    public NamesPanel () {
        setLayout(new ColumnManager());
    }

    /**
     * Construct NamesPanel which size depends on the other NamesPanel size..
     */
    public NamesPanel(NamesPanel namesPanel) {
        setLayout(new ColumnManager(namesPanel.getLayout()));
    }

    // XXX when jdk1.3 will become unsupported revise use of 
    // set/isFocusCycleRoot method. In jdk1.4 this method was 
    // added to java.awt.Container.
    /** Sets focus cycle root. 
     * @see #focusCycleRoot */
    public void setFocusCycleRoot(boolean focusCycleRoot) {
        this.focusCycleRoot = focusCycleRoot;
    }
    
    /** Indicates whether this panel has focus cycle.
     * Overrides superclass method.
     * @see #focusCycleRoot */
    public boolean isFocusCycleRoot() {
        return focusCycleRoot;
    }
    
    /** The preferred size of this panel is the size that is required by the text in the largest button
     */
    public Dimension getPreferredSize () {
        return getLayout ().preferredLayoutSize (this);
    }

    // bugfix of #13152 - makes sure no
    // PropertyPanels are in "write" state
    void reset() {
        // ensure that there is no PropertyPanel
        // in "write state"
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            if(getComponent(i) instanceof PropertyPanel) {
                PropertyPanel p = (PropertyPanel)getComponent(i);
                if (p.isWriteState()) {
                    p.refresh(); // back to the read state
                }
            }
        }
    }
    
    /** Overrides superclass method to ensure that all <code>SheetButton</code>s
    /* added to this panel are depressed when validation occures. */
    public void validate() {
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            if (getComponent(i) instanceof SheetButton) {
                SheetButton b = (SheetButton)getComponent(i);
                b.setPressed(false);
            }
        }
        super.validate();
    }
}
