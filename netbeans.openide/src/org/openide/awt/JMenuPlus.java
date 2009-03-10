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

import javax.swing.*;
import java.awt.*;

/** A subclass of JMenu which provides workaround for pre-JDK 1.2.2 JMenu positioning problem.
 * It assures, that the popup menu gets placed inside visible screen area.
 * It also improves placement of popups in the case the subclass lazily changes
 * the content from getPopupMenu(). 
 */
public class JMenuPlus extends JMenu {
    static final long serialVersionUID =-7700146216422707913L;

    public JMenuPlus() {
        this (""); // NOI18N
    }

    public JMenuPlus(String label) {
        super(label);

        enableInputMethods(false);

        getAccessibleContext().setAccessibleDescription(label);
    }

    /** Overriden to provide better strategy for placing the JMenu on the screen.
    * @param b a boolean value -- true to make the menu visible, false to hide it
    */
    public void setPopupMenuVisible(boolean b) {
        boolean isVisible = isPopupMenuVisible();
        if (b != isVisible) {
            if ((b==true) && isShowing()) {
                // The order of calls is a provision for subclassers that 
                // change the content of the menu during getPopupMenu()
                // We compute the origin later with properly filled popup
                JPopupMenu popup = getPopupMenu();
                
                // HACK[pnejedly]: Notify all the items in the menu we're going to show
                JInlineMenu.prepareItemsInContainer(popup);
                // End of HACK
                
                Point p = getPopupMenuOrigin();
                popup.show(this, p.x, p.y);
            } else {
                getPopupMenu().setVisible(false);
            }
        }
    }

    /** Overriden to provide better strategy for placing the JMenu on the screen.
    *
    * @return a Point in the coordinate space of the menu instance
    * which should be used as the origin of the JMenu's popup menu.
    */
    protected Point getPopupMenuOrigin() {
        int x = 0;
        int y = 0;
        JPopupMenu pm = getPopupMenu();
        Rectangle screenRect = JPopupMenuUtils.getScreenRect();
        Dimension s = getSize();
        Dimension pmSize = pm.getSize();
        int screenRight = screenRect.x + screenRect.width;
        int screenBottom = screenRect.y + screenRect.height;
        // For the first time the menu is popped up,
        // the size has not yet been initiated
        if (pmSize.width==0) {
            pmSize = pm.getPreferredSize();
        }
        Point position = getLocationOnScreen();

        Container parent = getParent();
        if (parent instanceof JPopupMenu) {
            // We are a submenu (pull-right)

            // First determine x:
            if (position.x+s.width + pmSize.width < screenRight) {
                x = s.width;         // Prefer placement to the right
            } else {
                x = 0-pmSize.width;  // Otherwise place to the left
            }
            // Then the y:
            if (position.y+pmSize.height < screenBottom) {
                y = 0;                       // Prefer dropping down
            } else {
                y = s.height-pmSize.height;  // Otherwise drop 'up'
            }
        } else {
            // We are a toplevel menu (pull-down)

            // First determine the x:
            if (position.x+pmSize.width < screenRight) {
                x = 0;                     // Prefer extending to right
            } else {
                x = s.width-pmSize.width;  // Otherwise extend to left
            }
            // Then the y:
            if (position.y+s.height+pmSize.height < screenBottom) {
                y = s.height;          // Prefer dropping down
            } else {
                y = 0-pmSize.height;   // Otherwise drop 'up'
            }
        }
        if (y < -position.y) y = -position.y;
        if (x < -position.x) x = -position.x;
        return new Point(x,y);
    }
}
