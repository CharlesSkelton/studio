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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Menu element that can contain other menu items. These items are then
 * displayed "inline". The JInlineMenu can be used to componse more menu items
 * into one that can be added/removed at once.
 * 
 * @deprecated JInlineMenu is resizing itself after being displayed, this
 * behavior causes a lot of troubles for Swing/AWT on various platforms.  Very
 * hard to be fixed.  Module developers should stop using this class.
 * 
 * @author Jan Jancura
 */
public class JInlineMenu extends JMenuItem {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2310488127953523571L;

    /** north separator */
    private JSeparator north = new JSeparator ();
    /** south separator */
    private JSeparator south = new JSeparator ();
    /** Stores inner MenuItems added to outer menu. */
    private JComponent[] items = new JComponent[0];
    
    /** true iff items of this menu are up to date */
    boolean upToDate;
    
    /** private List of the items previously added to the parent menu */
    private List addedItems;
    
    /**
    * Creates new JInlineMenu.
    */
    public JInlineMenu () {
        setEnabled (false);
        upToDate = true;
    }

    /** Overriden to eliminate big gap at top of JInline popup painting.
     * @return cleared instets (0, 0, 0, 0) */
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Setter for array of items to display. Can be called only from event queue
     * thread.
     *
     * @param newItems array of menu items to display
     */
    public void setMenuItems (final JMenuItem[] newItems) {
//        if(!SwingUtilities.isEventDispatchThread()) {
//System.err.println("JInlineMenu.setMenuItems called outside of event queue !!!");
//Thread.dumpStack();
//        }
            
        // make a tuned private copy
        JComponent[] local = new JComponent[newItems.length];
        for(int i = 0; i < newItems.length; i++) {
            local[i] = newItems[i] != null ? (JComponent)newItems[i] : new JSeparator();
        }
        items = local;
        upToDate = false;
        
        // tell the parent it is not up-to-date as well
        Container parent = getParent();
        while (parent instanceof JInlineMenu) {
            ((JInlineMenu)parent).upToDate = false;
            parent = parent.getParent();
        }
        
        if (isShowing()) { // Ugly thing have happened - we're already visible
//System.err.println("JInlineMenu.setMenuItems on visible is deprecated !!!");
//Thread.dumpStack();
            SwingUtilities.invokeLater(new Updater());
        }
    }
    
    /* This method is overriden so that this class now allow following
     * pattern to be used:
     *
     * (1) nm = new JInlineMenu();
     * (2) nm.setMenuItems( ... some items ... );
     * (3) myJPopupMenu.add(nm);
     *
     * While the old source required (1) (3) and (2)
     */
    public void addNotify() { // addNotify it quite late to do anything, but we'll try
        super.addNotify();
        if (!upToDate) {
//System.err.println("InvokeLater-ing from addNotify()");
            SwingUtilities.invokeLater(new Updater());
        }
    }

    static void prepareItemsInContainer(Container c) {
        Component[] comps = c.getComponents();
        for (int i=0; i<comps.length; i++) {
            if (comps[i] instanceof JInlineMenu) {
                ((JInlineMenu)comps[i]).doUpdate();
            }
        }

    }
    
    private void doUpdate() {
        // Let the subclasses add their own update logic, done this
        // way because we want to go w/o API change for 3.4 on this topic
        // when we're going to _solve_ this for 4.0
        Object prop = getClientProperty("hack.preShowUpdater");
        if (prop instanceof Runnable) ((Runnable)prop).run();
        
	updateContents();
    }
    
    /** This method is called only when in AWT, never in addNotify
     * so it is safe to operate with the parent menu content */
    private void updateContents() {
        Container parent = getParent();
        if (!upToDate && parent != null) {
            if(! (parent instanceof JInlineMenu)) { 
                // we're the highest JInlineMenu, do the update ourself
                
                //call around all our subitems to prepare them
                for (int i=0; i<items.length; i++) {
                    Object prop = items[i].getClientProperty("hack.preShowUpdater");
                    if (prop instanceof Runnable) {
                        ((Runnable)prop).run();
                    }
                }

                removeItems();
                addItems();
            }
            upToDate = true; // we've been processed
        }
    }


    /** Remove all current items.
    */
    private void removeItems () {
        JComponent m = (JComponent) getParent ();
        if (m == null) return; // Can't happen

        if(m instanceof JInlineMenu) {
            // Delegate removing to parent JInlineMenu.
            ((JInlineMenu)m).removeItems();
            return;
        }

        // Remove all the items we've previously added.
        if (addedItems != null) {
            java.util.Iterator it = addedItems.iterator();
            while (it.hasNext()) m.remove((Component)it.next());
        }
        
        // Remove also our separators
        m.remove (north);
        m.remove (south);
        
        addedItems = null;
    }


    /** Gets all inline items including inline items from
     * sub-<code>JInlineMenu</code>'s. Used only by parent
     * <code>JInlineMenu</code>. */
    private void getAllInlineItems(List its) {
        for(int i = 0; i < items.length; i++) {
            Object item = items[i];
            
            if(item instanceof JInlineMenu) {
                ((JInlineMenu)item).getAllInlineItems(its);
            } else {
                its.add(item);
            }
        }
    }

    /** Finds the index of a component in array of components.
     * @return index or -1
     */
    private static int findIndex (Object of, Object[] arr) {
        int menuLength = arr.length;
        for (int i = 0; i < menuLength; i++) {
            if (of == arr[i]) {
                return i;
            }
        }
        return -1;
    }

    void addItems () {
        JComponent m = (JComponent) getParent ();
        if (m == null) return; // Can't happen

        boolean usedToBeContained = false;
        if (m instanceof JPopupMenu) {
            usedToBeContained = JPopupMenuUtils.isPopupContained ((JPopupMenu) m);
        }

        // Get all items, including those ones from sub-JInlineMenu's.
        List its = new java.util.ArrayList(items.length);
        getAllInlineItems(its);
        JComponent[] items = (JComponent[])its.toArray(new JComponent[its.size()]);
        addedItems = its;
        
        // Find me please!
        Component[] array = m.getComponents ();

        int menuPos = findIndex (this, array);
        if (menuPos == -1) return; // not found? strange!

        if (
            menuPos > 0 &&
            array.length > 0 && /* should be always true */
            !(array[menuPos - 1] instanceof JSeparator) &&
            !(array[menuPos - 1] instanceof JInlineMenu)
        ) { // not first and not after separator or another inline menu ==>> add separator before
            m.add (north, menuPos++);
            array = m.getComponents ();
        }

        if (menuPos < array.length - 1) {
            // not last
            if (
                items.length > 0 &&
                !(array[menuPos + 1] instanceof JPopupMenu.Separator) &&
                !(array[menuPos + 1] instanceof JSeparator)
            ) {
                // adding non-zero items and not before separator
                m.add (south, menuPos + 1);
            } else if (
                items.length == 0 &&
                (array[menuPos + 1] instanceof JPopupMenu.Separator
                 || array[menuPos + 1] instanceof JSeparator)
            ) {
                // adding zero items and there is an extra separator after the JInlineMenu item ==>> remove it
                m.remove (menuPos + 1);
                array = m.getComponents();
            }
        }

        // Add components to outer menu.
        if (menuPos > array.length) {
            int menuLength = items.length;
            for (int i = 0; i < menuLength; i++) {
                m.add (items[i]);
            }
        } else {
            int menuLength = items.length;
            for (int i = 0; i < menuLength; i++) {
                m.add (items[i], ++menuPos);
                // advance menuPos for JInlineMenu
                // otherwise the next item will be
                // actually placed before expanded
                // items of this JInlineMenu
                if (items[i] instanceof JInlineMenu) {
                    JInlineMenu him = (JInlineMenu) items[i];
                    menuPos += him.items.length;
                }
            }
        }
        
        if(m instanceof JPopupMenu && m.isShowing()) {
            // This can happen when somebody call setMenuItems on visible
            JPopupMenuUtils.dynamicChange((JPopupMenu)m, usedToBeContained);
        } else {
            // ensure correct preferred size computation
            m.invalidate();
        }
    }

    private class Updater implements Runnable {
        Updater() {}
        public void run() {
            updateContents();
        }
    }        

}
