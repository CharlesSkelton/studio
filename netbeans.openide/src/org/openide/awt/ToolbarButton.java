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

package org.openide.awt;

import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.event.MouseEvent;
/**
 * An implementation of a toolbar button.
 */
public class ToolbarButton extends JButton {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 6564434578524381134L;
    /** true if the disabled icon is already created */
    boolean disabled;

    /** Creates a button with an icon.
    *
    * @param icon  the Icon image to display on the button
    */
    public ToolbarButton (Icon icon) {
        super (null, icon);
        setModel (new EnabledButtonModel());
        setMargin (new java.awt.Insets(2, 1, 0, 1));
        setBorderPainted (false);
        this.setRolloverEnabled(true);
    }
    
    public void processMouseEvent (MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            setBorderPainted(true);
        }
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            setBorderPainted(false);
        }
        super.processMouseEvent (e);
    }    
    
    public Icon getDisabledIcon() {
        if (! disabled) {
            setDisabledIcon(ToolbarButton.createDisabledIcon(getIcon()));
            disabled = true;
        }
        return super.getDisabledIcon();
    }

    /** Identifies whether or not this component can receive the focus.
    * A disabled button, for example, would return false.
    * @return true if this component can receive the focus
    */
    public boolean isFocusTraversable() {
        return super.isFocusTraversable();
    }

    /** Creates shadowed icon */
    static Icon createDisabledIcon(Icon icon) {
        if (! (icon instanceof ImageIcon)) {
            return null;
        }
        ImageIcon imgIcon = (ImageIcon) icon;
        Image img = imgIcon.getImage();
        if (! (img instanceof BufferedImage)) {
            return null;
        }

        BufferedImage srcImg = (BufferedImage) img;
        int w = srcImg.getWidth();
        int h = srcImg.getHeight();
        int wh = w * h;
        int[] rgbArray = srcImg.getRGB(0, 0, w, h, new int[wh], 0, w);
        GrayFilter grayFilter = new GrayFilter(true, 50);
        for (int i = 0; i < wh; i++) {
            rgbArray[i] = grayFilter.filterRGB(0, 0, rgbArray[i]);
        }
        BufferedImage destImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        destImg.setRGB(0, 0, w, h, rgbArray, 0, w);

        return new ImageIcon(destImg);
    }
}
