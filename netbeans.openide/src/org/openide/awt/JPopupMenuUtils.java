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

import java.util.StringTokenizer;

import java.awt.*;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.openide.util.Utilities;
import org.openide.util.RequestProcessor;

/** A class that contains a set of utility classes and methods
 * around displaying and positioning popup menus.
 * 
 * Popup menus sometimes need to be repositioned so that they
 * don't "fall off" the edges of the screen.
 *
 * Some of the menus have items that are added dynamically, that is,
 * after the menu is displayed.  These menus are often placed correctly
 * for their initial size, but will need to be repositioned as they
 * grow.
 *
 * @author   Evan Adams
 */
public class JPopupMenuUtils extends Object {
    private static Rectangle screenRect;
    
    private static boolean problemTested = false;
    
    private static boolean problem = false;
    
    private static RequestProcessor reqProc;
    private static RequestProcessor.Task task;

    /*
     * Called when a visible menu has dynamically changed.  Ensure that
     * it stays on the screen.  Compute its new location and,
     * if it differs from the current one, move the popup.
     *
     * @param popup the popup menu
     */
    public static void dynamicChange(final JPopupMenu popup, boolean usedToBeContained) {
        if (!popup.isShowing()) {
            return;
        }
        if (isProblemConfig()) {
            callRefreshLater(popup);
            return;
        }

        refreshPopup(popup);
        
        Point p = popup.getLocationOnScreen();
        Point newPt = getPopupMenuOrigin(popup, p);

        boolean willBeContained = willPopupBeContained(popup, newPt);
        if (usedToBeContained != willBeContained) {
            popup.setVisible(false);
        }
        if (!newPt.equals(p)) {
            popup.setLocation(newPt.x, newPt.y);
        }
        if (usedToBeContained != willBeContained) {
            popup.setVisible(true);
        }
    }
     
    /** Mysterious calls to pack(), invalidate() and validate() ;-) */
    private static void refreshPopup(JPopupMenu popup) {
        popup.pack ();
        popup.invalidate ();
        Component c = popup.getParent ();
        if (c != null) {
                c.validate ();
        }
    }
    
    /** Called from dynamicChange. Performs refresh of the popup
     * in task in reqProc.
     */
    private static void callRefreshLater(final JPopupMenu popup) {
            // this may cause the popup to flicker
            if (reqProc == null) {
                reqProc = new RequestProcessor();
            }
            if (task == null) {
                task = reqProc.create(new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                task = null;
                                // after the action is performed new task should be created
                                // (probably for another instance of a popup)
                                if (!popup.isShowing()) {
                                    return;
                                }
                                Point p = popup.getLocationOnScreen();
                                Point newPt = getPopupMenuOrigin(popup, p);
                                popup.setVisible(false);
                                refreshPopup(popup);
                                if (!newPt.equals(p)) {
                                    popup.setLocation(newPt.x, newPt.y);
                                }
                                popup.setVisible(true);
                            }
                        });
                    }
                });
            }
            task.schedule(100);
    }
    
    /** Returns true when the popup has to be unconditioanlly
     * redisplayed when adding new items. Currently
     * this is true with JDK1.3 on Linux/Gnome. This
     * method checks for presence of system property
     * "netbeans.popup.linuxhack".
     */
    private static boolean isProblemConfig() {
        // we have already tested the need for the hack
        if (problemTested) {
            return problem;
        }
        problem = false;
        String needHack = System.getProperty("netbeans.popup.linuxhack");
        if (needHack != null) {
            problem = true;
        }
        return problem;
    }

    /*
     * Called when a visible submenu (pullright) has dynamically changed.
     * Ensure that it stays on the screen.  If it doesn't fit, then hide
     * the popup and redisplay it.  This causes JMenu's placement code
     * to get executed again which may change the submens to go up rather
     * than down.
     *
     * @param popup the popup menu
     */
    public static void dynamicChangeToSubmenu(JPopupMenu popup, boolean usedToBeContained) {
        Object invoker = popup.getInvoker();
        if (!(invoker instanceof JMenu)) {
            return;
        }
        JMenu menu = (JMenu) invoker;

        if (!popup.isShowing()) {
            return;
        }
        if (isProblemConfig()) {
            callRefreshLater2(popup, menu);
            return;
        }

        refreshPopup(popup);
        
        Point p = popup.getLocationOnScreen();
        Dimension popupSize = popup.getPreferredSize ();
        Rectangle popupRect = new Rectangle(p, popupSize);
        Rectangle screenRect = getScreenRect();
        boolean willBeContained = isPopupContained(popup);
        if (!screenRect.contains(popupRect)) {
            /*
             * The menu grew off the edge of the screen.
             */
            menu.setPopupMenuVisible(false);
            menu.setPopupMenuVisible(true);
        } else if (usedToBeContained != willBeContained) {
            /*
             * The menu grew off the edge of the containing window.
             * Use the setVisible() hack to change the menu from
             * lightweight to heavyweight.
             */
            popup.setVisible(false);
            popup.setVisible(true);
        }
    }

    /** Called from dynamicChangeToSubmenu. Calls the popup refresh
     * in a task in the reqProc.
     */
    private static void callRefreshLater2(final JPopupMenu popup, final JMenu menu) {
            // this may cause the popup to flicker
            if (reqProc == null) {
                reqProc = new RequestProcessor();
            }
            if (task == null) {
                task = reqProc.create(new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                task = null;
                                // after the action is performed new task should be created
                                // (probably for another instance of a popup)
                                if (!popup.isShowing()) {
                                    return;
                                }
                                popup.setVisible(false);
                                refreshPopup(popup);
                                popup.setVisible(true);
                                Point p = popup.getLocationOnScreen();
                                Dimension popupSize = popup.getPreferredSize ();
                                Rectangle popupRect = new Rectangle(p, popupSize);
                                Rectangle screenRect = getScreenRect();
                                if (!screenRect.contains(popupRect)) {
                                    menu.setPopupMenuVisible(false);
                                    menu.setPopupMenuVisible(true);
                                }
                            }
                        });
                    }
                });
            }
            task.schedule(100);
    }

    /*
     * Return the point for the origin of this popup.
     * This is where the adjustments are made to ensure the
     * popup stays on the screen.
     *
     * @param popup the popup menu
     * @param p the popup menu's origin
     * @return the popup menu's new origin
     */
    static Point getPopupMenuOrigin(JPopupMenu popup, Point p) {
        Point newPt = new Point(p);
        Dimension popupSize = popup.getPreferredSize ();
        Rectangle screenRect = getScreenRect();
        int popupRight = newPt.x + popupSize.width;
        int popupBottom = newPt.y + popupSize.height;
        int screenRight = screenRect.x + screenRect.width;
        int screenBottom = screenRect.y + screenRect.height;
        if (popupRight > screenRight) {     // Are we off the right edge?
            newPt.x = screenRight - popupSize.width;
        }
        if (newPt.x < screenRect.x) {       // Are we off the left edge?
            newPt.x = screenRect.x;
        }
        if (popupBottom > screenBottom) {   // Are we off the bottom edge?
            newPt.y = screenBottom - popupSize.height;
        }
        if (newPt.y < screenRect.y) {       // Are we off the top edge?
            newPt.y = screenRect.y;
        }
        return newPt;
    }

    /*
     * Return whether or not the given popup is contained by its
     * parent window.  Uses the current location and size of the popup.
     *
     * @return boolean indicating if the popup is contained
     */
    public static boolean isPopupContained(JPopupMenu popup) {
        if (!popup.isShowing()) {
            return false;
        }
        return willPopupBeContained(popup, popup.getLocationOnScreen());
    }

    /*
     * Return whether or not the given popup will be contained by
     * its parent window if it is moved to <code>origin</origin>.
     * Use its current size.
     *
     * @param <code>popup</code> the popup to be tested
     * @param <code>origin</code> location of the popup to be tested
     * @return boolean indicating if the popup will be contained
     */
    private static boolean willPopupBeContained(JPopupMenu popup, Point origin) {
        if (!popup.isShowing()) {
            return false;
        }
        Window w = SwingUtilities.windowForComponent (popup.getInvoker());
        Rectangle r = new Rectangle (origin, popup.getSize ());
        return w != null && w.getBounds ().contains (r);
    }

    /*
     * Return a rectange defining the usable portion of the screen.  
     * Designed to provide a way to account for the taskbar in Windows.
     * Ultimately, we should make a native call when running on Windows
     * to determine screen rectangle.  For now we assume the taskbar is
     * at the bottom and that it is just one row tall.
     *
     * @return a rectangle defining the usable area.
     */
    public static Rectangle getScreenRect() {
        if (screenRect != null) {
            return screenRect;
        }
        screenRect = getRectFromProperty();
        if (screenRect != null) {
            return screenRect;
        }
        Dimension screen = Utilities.getScreenSize();
        return new Rectangle(0, 0, screen.width, screen.height);
    }

    /*
     * If the property "netbeans.screen.rect" contains the description
     * of a rectangle, then return that rectangle.  Used to allow the
     * user to specify the usable portion of the screen.  Intended to
     * describe where and how big the taskbar is on Windows.
     */
    private static Rectangle getRectFromProperty() {
        String prop = System.getProperty("netbeans.screen.rect");
        if (prop == null) {
            return null;
        }
        StringTokenizer strtok = new StringTokenizer(prop, ",", false); // NOI18N
        if (strtok.countTokens() != 4) {
            return null;
        }
        try {
            int x = Integer.parseInt(strtok.nextToken());
            int y = Integer.parseInt(strtok.nextToken());
            int width = Integer.parseInt(strtok.nextToken());
            int height = Integer.parseInt(strtok.nextToken());
            return new Rectangle(x, y, width, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
