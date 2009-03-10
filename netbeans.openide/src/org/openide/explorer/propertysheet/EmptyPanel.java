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

import java.awt.*;

/**
* Empty panel with given text in the center of them.
*
* @author   Jan Jancura
*/
final class EmptyPanel extends javax.swing.JPanel {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -5681425006155127558L;

    private String text = org.openide.util.NbBundle.getBundle (EmptyPanel.class).getString ("CTL_No_properties");

    /*
    * Creates new panel vith given message.
    */
    EmptyPanel (String text) {
        this.text = text;
    }

    /*
    * Standart painting method.
    */
    public void paintBorder (Graphics g) {
        super.paintBorder (g);
        Dimension   size = getSize ();
        Color       c = g.getColor ();
        Color       bc = getBackground ();
        FontMetrics fontMetrics = g.getFontMetrics();
        g.setColor (bc.brighter ().brighter ());
        g.drawString (
            text,
            (size.width - fontMetrics.stringWidth (text)) / 2,
            10 + fontMetrics.getMaxAscent ()
        );
        g.setColor (bc.darker ());
        g.drawString (
            text,
            (size.width - fontMetrics.stringWidth (text)) / 2 - 1,
            10 + fontMetrics.getMaxAscent () - 1
        );
        g.setColor (c);
    }
}
