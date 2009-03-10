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

import javax.swing.*;
import java.awt.event.MouseEvent;
/**
 * An implementation of a toggle toolbar button.
 */
public class ToolbarToggleButton extends JToggleButton {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -4783163952526348942L;

    /** Creates a button with an icon.
    *
    * @param icon  the Icon image to display on the button
    */
    public ToolbarToggleButton(Icon icon) {
        super(null, icon);
        setModel(new EnabledButtonModel());
        setMargin(new java.awt.Insets(2, 1, 0, 1));
        setBorderPainted (false);
    }

    protected void processMouseEvent (MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            this.setBorderPainted(true);
        }
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            this.setBorderPainted(false);
        }
        super.processMouseEvent (e);
    }
    
    /**
    * Creates a toggle button with the specified image and selection state.
    *
    * @param icon  the image that the button should display
    * @param selected  if true, the button is initially selected;
    *                  otherwise, the button is initially unselected
    */
    public ToolbarToggleButton(Icon icon, boolean selected) {
        super(null, icon, selected);
        setMargin (new java.awt.Insets(2, 1, 0, 1));
    }

    /** Identifies whether or not this component can receive the focus.
    * A disabled button, for example, would return false.
    * @return true if this component can receive the focus
    */
    public boolean isFocusTraversable() {
        return super.isFocusTraversable();
    }
}
