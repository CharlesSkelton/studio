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

package org.openide.awt;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/**
 * This class implements unavailable appearance for icons.
 *
 * TODO: rewrite for Java2D when available...
 *
 * @author David Peroutka
 * @version 0.2 06/05/1998
 */
class DisabledIcon extends ImageIcon {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 5126672233442706110L;

    private Image borderImage;
    protected boolean borderPainted = true;
    class Filter extends RGBImageFilter
    {
        Filter()
        {
            canFilterIndexColorModel = true;
        }

        public int filterRGB(int x, int y, int rgb)
        {
            return (rgb & 0xff000000) | (0xff << 16) | (0xff << 8) | (0xff << 0);
        }
    }

    /**
     * Creates a disabled image icon.
     * @param image disabled image.
     */
    public DisabledIcon(Image image)
    {
        super(image);
        // create unavailable appearance border
        borderImage = Toolkit.getDefaultToolkit().createImage((ImageProducer)new FilteredImageSource(image.getSource(),
                      new Filter()));
    }

    /**
     * Paints the icon.
     */
    public synchronized void paintIcon(Component component, Graphics graphics, int x, int y)
    {
        if (borderPainted) {
            graphics.drawImage(borderImage, x + 1, y + 1, null);
        }
        super.paintIcon(component, graphics, x, y);
    }

    /**
     * Sets whether the unavailable appearance border should be painted.
     * @param b if true, the border is painted.
     * @see isBorderPainted
     */
    public void setBorderPainted(boolean b)
    {
        borderPainted = b;
    }
}
