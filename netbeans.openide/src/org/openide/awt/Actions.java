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
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

import org.openide.ErrorManager;
import org.openide.util.Mutex;
import org.openide.util.actions.*;
import org.openide.util.HelpCtx;

/** Supporting class for manipulation with menu and toolbar presenters.
*
* @author   Jaroslav Tulach
*/
public class Actions extends Object {
    
    /** Method that finds the keydescription assigned to this action.
    * @param action action to find key for
    * @return the text representing the key or null if  there is no text assigned
    */
    public static String findKey (SystemAction action) {
        return findKey ((Action)action);
    }
        
    /** Same method as above, but works just with plain actions.
     */
    private static String findKey (Action action) {
        if(action == null) {
            return null;
        }
        
        KeyStroke accelerator = (KeyStroke)action.getValue(Action.ACCELERATOR_KEY);
        if(accelerator == null) {
            return null;
        }
        
        int modifiers = accelerator.getModifiers();
        String acceleratorText = ""; // NOI18N
        if (modifiers > 0) {
            acceleratorText = KeyEvent.getKeyModifiersText(modifiers);
            acceleratorText += "+"; // NOI18N
        } else if (accelerator.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            return ""; // NOI18N
        }
        acceleratorText += KeyEvent.getKeyText(accelerator.getKeyCode());
        return acceleratorText;
    }

    /** Attaches menu item to an action.
    * @param item menu item
    * @param action action
    * @param popup create popup or menu item
     * @deprecated Use {@link #connect(JMenuItem, Action, boolean)} instead.
    */
    public static void connect (JMenuItem item, SystemAction action, boolean popup) {
        connect (item, (Action)action, popup);
    }

    /** Attaches menu item to an action.
     * @param item menu item
     * @param action action 
     * @param popup create popup or menu item
     * @since 3.29
     */
    public static void connect (JMenuItem item, Action action, boolean popup) {
        Bridge b = new MenuBridge (item, action, popup);
        // Would make more sense to defer this until addNotify, but for some reason (why?)
        // if you do that, various menus start out compacted and poorly painted.
        b.updateState (null);
    }
    
    /** Attaches checkbox menu item to boolean state action.
    * @param item menu item
    * @param action action
    * @param popup create popup or menu item
    */
    public static void connect (JCheckBoxMenuItem item, BooleanStateAction action, boolean popup) {
        Bridge b = new CheckMenuBridge (item, action, popup);
        b.updateState (null);
    }

    /** Connects buttons to action.
    * @param button the button
    * @param action the action
     * @deprecated Use {@link #connect(AbstractButton, Action)} instead.
    */
    public static void connect (AbstractButton button, SystemAction action) {
        connect (button, (Action)action);
    }

    /** Connects buttons to action.
     * @param button the button
     * @param action the action
     * @since 3.29
     */
    public static void connect (AbstractButton button, Action action) {
        Bridge b = new ButtonBridge (button, action);
        b.updateState (null);
    }

    /** Connects buttons to action.
    * @param button the button
    * @param action the action
    */
    public static void connect (AbstractButton button, BooleanStateAction action) {
        Bridge b = new BooleanButtonBridge (button, action);
        b.updateState (null);
    }

    /** Sets the text for the menu item or other subclass of AbstractButton.
    * Cut from the name '&' char.
    * @param item AbstractButton
    * @param text new label
    * @param useMnemonic if true and '&' char found in new text, next char is used
    *           as Mnemonic.
    * @deprecated Use either {@link AbstractButton#setText} or {@link Mnemonics#setLocalizedText(AbstractButton, String)} as appropriate.
    */
    public static void setMenuText(AbstractButton item, String text, boolean useMnemonic) {
        if (useMnemonic) {
            Mnemonics.setLocalizedText(item, text);
        } else {
            item.setText(cutAmpersand(text));
        }
    }

    /**
     * Removes an ampersand from a text string; commonly used to strip out unneeded mnemonics.
     * Replaces the first occurence of <samp>&amp;?</samp> by <samp>?</samp> or <samp>(&amp;??</samp> by the empty string 
     * where <samp>?</samp> is a wildcard for any character.
     * <samp>&amp;?</samp> is a shortcut in English locale.
     * <samp>(&amp;?)</samp> is a shortcut in Japanese locale.
     * Used to remove shortcuts from workspace names (or similar) when shortcuts are not supported.
     * <p>The current implementation behaves in the same way regardless of locale.
     * In case of a conflict it would be necessary to change the
     * behavior based on the current locale.
     * @param text a localized label that may have mnemonic information in it
     * @return string without first <samp>&amp;</samp> if there was any
     */
    public static String cutAmpersand(String text) {
        // XXX should this also be deprecated by something in Mnemonics?
        int i;
        String result = text;
        /* First check of occurence of '(&'. If not found check 
          * for '&' itself.
          * If '(&' is found then remove '(&??'.
          */
        i = text.indexOf("(&"); // NOI18N
        if (i >= 0 && i + 3 < text.length() && /* #31093 */text.charAt(i + 3) == ')') { // NOI18N
            result = text.substring(0, i) + text.substring(i + 4);
        } else {
            //Sequence '(&?)' not found look for '&' itself
            i = text.indexOf('&');
            if (i < 0) {
                //No ampersand
                result = text;
            } else if (i == (text.length() - 1)) {
                //Ampersand is last character, wrong shortcut but we remove it anyway
                result = text.substring(0, i);
            } else {
                //Remove ampersand from middle of string
                result = text.substring(0, i) + text.substring(i + 1);
            }
        }
        return result;
    }
    
    /** Extracts help from action.
     */
    private static HelpCtx findHelp (Action a) {
        if (a instanceof HelpCtx.Provider) {
            return ((HelpCtx.Provider)a).getHelpCtx();
        } else {
            return HelpCtx.DEFAULT_HELP;
        }
    }

    /** Listener on showing/hiding state of the component.
    * Is attached to menu or toolbar item in prepareXXX methods and
    * method addNotify is called when the item is showing and
    * the method removeNotify is called when the item is hidding.
    * <P>
    * There is a special support listening on changes in the action and
    * if such change occures, updateState method is called to
    * reflect it.
    */
    private static abstract class Bridge extends Object
        implements PropertyChangeListener {
        /** component to work with */
        protected JComponent comp;
        /** action to associate */
        protected Action action;

        /** @param comp component
        * @param action the action
        */
        public Bridge (JComponent comp, Action action) {
            this.comp = comp;
            this.action = action;

            // visibility listener
            Bridge.this.comp.addPropertyChangeListener(new VisL());
            if (Bridge.this.comp.isShowing ()) {
                addNotify ();
            }

            // associate context help, if applicable
            // [PENDING] probably belongs in ButtonBridge.updateState to make it dynamic
            HelpCtx help = findHelp (action);
            if (help != null && ! help.equals (HelpCtx.DEFAULT_HELP) && help.getHelpID () != null)
                HelpCtx.setHelpIDString (comp, help.getHelpID ());
        }

        /** Attaches listener to given action */
        public void addNotify () {
            action.addPropertyChangeListener (this);
            updateState (null);
        }

        /** Remove the listener */
        public void removeNotify () {
            action.removePropertyChangeListener (this);
        }

        /** @param changedProperty the name of property that has changed
        * or null if it is not known
        */
        public abstract void updateState (String changedProperty);

        /** Listener to changes of some properties.
        * Multicast - reacts to keymap changes and ancestor changes
        * together.
        */
        public void propertyChange (final PropertyChangeEvent ev) {
            // do in EventQueue
            Mutex.EVENT.readAccess (new Runnable () {
                public void run () {
                    updateState (ev.getPropertyName ());
                }
            });
        }
        // Must be separate from general PCL, because otherwise
        // SystemAction.PROP_ENABLED -> updateState("enabled") ->
        // button.setEnabled(...) -> JButton.PROP_ENABLED ->
        // updateState("enabled") -> button.setEnabled(same)
        private class VisL implements PropertyChangeListener {
            VisL() {}
            public void propertyChange (final PropertyChangeEvent ev) {
                if ("ancestor".equals(ev.getPropertyName())) {
                    // ancestor change - decide if parent is null or not
                    if (ev.getNewValue() != null) {
                        addNotify();
                    } else {
                        removeNotify();
                    }
                }
            }
        }
    }


    /** Bridge between an action and button.
    */
    private static class ButtonBridge extends Bridge implements ActionListener {
        /** the button */
        protected AbstractButton button;

        public ButtonBridge (AbstractButton button, Action action) {
            super (button, action);
            button.addActionListener (this);
            this.button = button;
        }

        /** @param changedProperty the name of property that has changed
        * or null if it is not known
        */
        public void updateState (String changedProperty) {
            boolean didToolTip = false;
            // note: "enabled" (== SA.PROP_ENABLED) hardcoded in AbstractAction
            if (changedProperty == null || changedProperty.equals (SystemAction.PROP_ENABLED)) {
                button.setEnabled (action.isEnabled ());
            }
            if (changedProperty == null || changedProperty.equals (SystemAction.PROP_ICON) || changedProperty.equals(Action.SMALL_ICON)) {
                if (action instanceof SystemAction) {
                    SystemAction sa = (SystemAction)action;
                    button.setIcon (sa.getIcon (useTextIcons ()));
                } else {
                    Object i = action.getValue (Action.SMALL_ICON);
                    if (i instanceof Icon) {
                        button.setIcon ((Icon)i);
                    }
                }
            }
            if (changedProperty == null || changedProperty.equals (Action.SHORT_DESCRIPTION)) {
                String shortDesc = (String) action.getValue (Action.SHORT_DESCRIPTION);
                if (shortDesc != null && !shortDesc.equals (action.getValue (Action.NAME))) {
                    button.setToolTipText (shortDesc);
                    didToolTip = true;
                }
            }

            if (! didToolTip && (changedProperty == null ||
                                 changedProperty.equals (Action.ACCELERATOR_KEY))) {
                String tip = findKey (action);
                String an = cutAmpersand ((String)action.getValue (Action.NAME));
                if (tip == null || tip.equals("")) { // NOI18N
                    button.setToolTipText(an);
                } else {
                    button.setToolTipText(org.openide.util.NbBundle.getMessage(
			    Actions.class, "FMT_ButtonHint", an, tip));
                }
            }
            
            if (button instanceof javax.accessibility.Accessible
                && (changedProperty == null || changedProperty.equals (Action.NAME))) {
                button.getAccessibleContext().setAccessibleName((String)action.getValue (Action.NAME));
            }
        }

        /** Should textual icons be used when lacking a real icon?
        * In the default implementation, <code>true</code>.
        * @return <code>true</code> if so
        */
        protected boolean useTextIcons () {
            return true;
        }
        
        /**
         */
        public void actionPerformed(final java.awt.event.ActionEvent ev) {
            invokeAction(action, ev);
        }
        
    }
    
    /** Utility method for invoking actions in separate thread. Note:
     * it uses reflection because it should work without
     * the rest of the IDE classes.
     */
    static void invokeAction(Action a, ActionEvent ev) {
        Throwable t = null;
        try {
            Class c = Class.forName("org.openide.actions.ActionManager"); // NOI18N
            Object o = org.openide.util.Lookup.getDefault ().lookup(c);
            if (o != null) {
                // lookup has found the instance
                // use reflection now
                java.lang.reflect.Method m = c.getMethod("invokeAction", // NOI18N
                    new Class[] {
                        javax.swing.Action.class,
                        java.awt.event.ActionEvent.class });
                m.invoke(o, new Object[] { a, ev } );
                // everything went ok -->
                return;
            }
        }
        // exceptions from forName:
        catch (ClassNotFoundException x) { }
        catch (ExceptionInInitializerError x) { }
        catch (LinkageError x) {  }
        // exceptions from getMethod:
        catch (SecurityException x) { t = x; } 
        catch (NoSuchMethodException x) { t = x;}
        // exceptions from invoke
        catch (IllegalAccessException x) { t = x;} 
        catch (IllegalArgumentException x) { t = x;} 
        catch (java.lang.reflect.InvocationTargetException x) {
            t = x;
        }

        if (t != null) {
            ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, t);
        }
        // something went wrong --> invoke the action directly
        a.actionPerformed(ev);
    }

    /** Bridge for button and boolean action.
    */
    private static class BooleanButtonBridge extends ButtonBridge {

        public BooleanButtonBridge (AbstractButton button, BooleanStateAction action) {
            super (button, action);
        }

        /** @param changedProperty the name of property that has changed
        * or null if it is not known
        */
        public void updateState (String changedProperty) {
            super.updateState (changedProperty);
            if (changedProperty == null || changedProperty.equals (BooleanStateAction.PROP_BOOLEAN_STATE)) {
                button.setSelected (((BooleanStateAction)action).getBooleanState ());
            }
        }

    }

    /** Menu item bridge.
    */
    private static class MenuBridge extends ButtonBridge {
        /** behave like menu or popup */
        private boolean popup;

        /** Constructor.
        * @param popup pop-up menu
        */
        public MenuBridge (JMenuItem item, Action action, boolean popup) {
            super (item, action);
            this.popup = popup;

            if (popup) {
                prepareMargins (item, action);
            }
        }

        /** @param changedProperty the name of property that has changed
        * or null if it is not known
        */
        public void updateState (String changedProperty) {
            if (changedProperty == null || changedProperty.equals (SystemAction.PROP_ENABLED)) {
                button.setEnabled (action.isEnabled ());
            }

            if (changedProperty == null || !changedProperty.equals (Action.ACCELERATOR_KEY)) {
                updateKey ((JMenuItem)comp, action);
            }

            if (!popup) {

                if (changedProperty == null || changedProperty.equals (SystemAction.PROP_ICON) || changedProperty.equals(Action.SMALL_ICON)) {
                    if (action instanceof SystemAction) {
                        SystemAction sa = (SystemAction)action;
                        button.setIcon (sa.getIcon (useTextIcons ()));
                    } else {
                        Object i = action.getValue (Action.SMALL_ICON);
                        if (i instanceof Icon) {
                            button.setIcon ((Icon)i);
                        }
                    }
                }
            }

            if (changedProperty == null || changedProperty.equals (Action.NAME)) {
                Object s = action.getValue (Action.NAME);
                if (s instanceof String) {
                    setMenuText (((JMenuItem)comp), (String)s, !popup);
                }
            }
        }

        // Not actually used:
        protected boolean useTextIcons () {
            return false;
        }

    }

    /** Check menu item bridge.
    */
    private static final class CheckMenuBridge extends BooleanButtonBridge {
        /** is popup or menu */
        private boolean popup;

        /** Popup menu */
        public CheckMenuBridge (JCheckBoxMenuItem item, BooleanStateAction action, boolean popup) {
            super (item, action);
            this.popup = popup;

            if (popup) {
                prepareMargins (item, action);
            }
        }

        /** @param changedProperty the name of property that has changed
        * or null if it is not known
        */
        public void updateState (String changedProperty) {
            super.updateState (changedProperty);

            if (changedProperty == null || !changedProperty.equals (Action.ACCELERATOR_KEY)) {
		updateKey ((JMenuItem)comp, action);
	    }

            if (changedProperty == null || changedProperty.equals (Action.NAME)) {
                Object s = action.getValue (Action.NAME);
                if (s instanceof String) {
                    setMenuText (((JMenuItem)comp), (String)s, !popup);
                }
            }
        }

        protected boolean useTextIcons () {
            return false;
        }
    }

    /** Sub menu bridge.
    */
    private static final class SubMenuBridge extends MenuBridge
    implements ChangeListener, Runnable {
        /** model to obtain subitems from */
        private SubMenuModel model;
        /** submenu */
        private SubMenu menu;

        /** Constructor.
        */
        public SubMenuBridge (SubMenu item, Action action, SubMenuModel model, boolean popup) {
            super (item, action, popup);
            prepareMargins (item, action);


            menu = item;
            this.model = model;
        }

        public void addNotify () {
            super.addNotify ();
            model.addChangeListener (this);
            generateSubMenu ();
        }

        public void removeNotify () {
            model.removeChangeListener (this);
            super.removeNotify ();
        }

        /** Called when model changes. Regenerates the model.
        */
        public void stateChanged (ChangeEvent ev) {
            // transfers the execution into AWT event thread
            org.openide.util.Mutex.EVENT.readAccess (this);
        }
        
        /** Called only in AWT event thread. Safe to work with component hiearachy.
         */
        public void run () {
            // change in keys or in submenu model
            generateSubMenu ();
        }            

        /** Regenerates the menu
        */
        private void generateSubMenu() {
            boolean shouldUpdate = false;
            try {
                menu.removeAll ();

                int cnt = model.getCount ();

                if (cnt != menu.previousCount) {
                    // update UI
                    shouldUpdate = true;
                }
                // in all cases remeber the previous
                menu.previousCount = cnt;

                // remove if there is an previous listener
                if (menu.oneItemListener != null) {
                    menu.removeActionListener(menu.oneItemListener);
                }
                if (cnt == 0) {
                    // menu disabled
                    menu.setEnabled (false);
                    if (menu.oneItemListener != null) {
                        menu.removeActionListener (menu.oneItemListener);
                        menu.oneItemListener = null;
                    }
                    return;
                } else {
                    menu.setEnabled (true);
                    // go on
                }

                if (cnt == 1) {
                    // generate without submenu
                    menu.addActionListener(menu.oneItemListener = new ISubActionListener(0, model));
                    HelpCtx help = model.getHelpCtx (0);
                    associateHelp (menu, help == null ? findHelp (action) : help);
                } else {
                    boolean addSeparator = false;
                    int count = model.getCount();
                    for (int i = 0; i < count; i++) {
                        String label = model.getLabel(i);
                        //          MenuShortcut shortcut = support.getMenuShortcut(i);
                        if (label == null) {
                            addSeparator =  menu.getComponentCount() > 0;
                        } else {
                            if(addSeparator) {
                                menu.addSeparator();
                                addSeparator = false;
                            }
                            //       if (shortcut == null)
                            // (Dafe) changed to support mnemonics in item labels
                            JMenuItem item = new JMenuItem();
                            Mnemonics.setLocalizedText(item, label);
                            // attach the shortcut to the first item
                            if (i == 0) updateKey(item, action);
                            item.addActionListener(new ISubActionListener(i, model));
                            HelpCtx help = model.getHelpCtx (i);
                            associateHelp (item, help == null ? findHelp (action) : help);
                            menu.add(item);
                        }
                    }
                    associateHelp (menu, findHelp (action));
                }
            } finally {
                if (shouldUpdate) {
                    menu.updateUI ();
                }
            }
        }
        private void associateHelp (JComponent comp, HelpCtx help) {
            if (help != null && ! help.equals (HelpCtx.DEFAULT_HELP) && help.getHelpID () != null)
                HelpCtx.setHelpIDString (comp, help.getHelpID ());
            else
                HelpCtx.setHelpIDString (comp, null);
        }

        /** The class that listens to the menu item selections and forwards it to the
        * action class via the performAction() method.
        */
        private static class ISubActionListener implements java.awt.event.ActionListener {
            int index;
            SubMenuModel support;

            public ISubActionListener(int index, SubMenuModel support) {
                this.index = index;
                this.support = support;
            }

            /** called when a user clicks on this menu item */
            public void actionPerformed(ActionEvent e) {
                support.performActionAt(index);
            }
        }

    }


    //
    // Methods for configuration of MenuItems
    //


    /** Method to prepare the margins and text positions.
    */
    static void prepareMargins (JMenuItem item, Action action) {
        Insets margin = item.getMargin ();
        margin.left = 0;
        item.setMargin(margin);
        item.setHorizontalTextPosition(JMenuItem.RIGHT);
        item.setHorizontalAlignment(JMenuItem.LEFT);
    }

    /** Updates value of the key
    * @param item item to update
    * @param action the action to update
    */
    static void updateKey (JMenuItem item, Action action) {
        if (item instanceof SubMenu || !(item instanceof JMenu)) {
            if (item instanceof SubMenu && !((SubMenu)item).useAccel()) {
                item.setAccelerator (null);
            } else {
                item.setAccelerator((KeyStroke)action.getValue(Action.ACCELERATOR_KEY));
            }
        }            
    }



    //
    //
    // The presenter classes
    //
    //

    /**
     * Extension of Swing menu item with connection to
     * system actions.
     */
    public static class MenuItem extends javax.swing.JMenuItem {
        static final long serialVersionUID =-21757335363267194L;
        /** Constructs a new menu item with the specified label
        * and no keyboard shortcut and connects it to the given SystemAction.
        * @param aAction the action to which this menu item should be connected
        * @param useMnemonic if true, the menu try to find mnemonic in action label
        */
        public MenuItem (SystemAction aAction, boolean useMnemonic) {
            Actions.connect (this, aAction, !useMnemonic);
        }
        
        /** Constructs a new menu item with the specified label
        * and no keyboard shortcut and connects it to the given SystemAction.
        * @param aAction the action to which this menu item should be connected
        * @param useMnemonic if true, the menu try to find mnemonic in action label
        */
        public MenuItem (Action aAction, boolean useMnemonic) {
            Actions.connect (this, aAction, !useMnemonic);
        }
    }

    /** CheckboxMenuItem extends the java.awt.CheckboxMenuItem and adds
    * a connection to boolean state actions. The ActCheckboxMenuItem
    * processes the ItemEvents itself and calls the action.seBooleanState() method.
    * It also tracks the enabled and boolean state of the action and reflects it
    * as its visual enabled/check state.
    *
    * @author   Ian Formanek, Jan Jancura
    */
    public static class CheckboxMenuItem extends javax.swing.JCheckBoxMenuItem {
        static final long serialVersionUID =6190621106981774043L;
        /** Constructs a new ActCheckboxMenuItem with the specified label
        *  and connects it to the given BooleanStateAction.
        * @param aAction the action to which this menu item should be connected
        * @param useMnemonic if true, the menu try to find mnemonic in action label
        */
        public CheckboxMenuItem (BooleanStateAction aAction, boolean useMnemonic) {
            Actions.connect (this, aAction, !useMnemonic);
        }
    }

    /** Component shown in toolbar, representing an action.
    *
    */
    public static class ToolbarButton extends org.openide.awt.ToolbarButton {
        static final long serialVersionUID =6564434578524381134L;
        public ToolbarButton (SystemAction aAction) {
            super (null);
            Actions.connect (this, aAction);
        }

        public ToolbarButton (Action aAction) {
            super (null);
            Actions.connect (this, aAction);
        }
        
        /**
         * Gets the maximum size of this component.
         * @return A dimension object indicating this component's maximum size.
         * @see #getMinimumSize
         * @see #getPreferredSize
         * @see LayoutManager
         */
        public Dimension getMaximumSize() {
            return this.getPreferredSize ();
        }

        public Dimension getMinimumSize() {
            return this.getPreferredSize ();
        }
    }


    /** The Component for BooleeanState action that is to be shown
    * in a toolbar.
    *
    */
    public static class ToolbarToggleButton extends org.openide.awt.ToolbarToggleButton {
        static final long serialVersionUID =-4783163952526348942L;
        /** Constructs a new ActToolbarToggleButton for specified action */
        public ToolbarToggleButton (BooleanStateAction aAction) {
            super(null, false);
            Actions.connect (this, aAction);
        }

        /**
         * Gets the maximum size of this component.
         * @return A dimension object indicating this component's maximum size.
         * @see #getMinimumSize
         * @see #getPreferredSize
         * @see LayoutManager
         */
        public Dimension getMaximumSize() {
            return this.getPreferredSize ();
        }

        public Dimension getMinimumSize() {
            return this.getPreferredSize ();
        }
    }


    /** Interface for the creating Actions.SubMenu. It provides the methods for
    * all items in submenu: name shortcut and perform method. Also has methods
    * for notification of changes of the model.
    */
    public static interface SubMenuModel {
        /** @return count of the submenu items. */
        public int getCount();

        /** Gets label for specific index
        * @param index of the submenu item
        * @return label for this menu item (or <code>null</code> for a separator)
        */
        public String getLabel(int index);

        /** Gets shortcut for specific index
        * @index of the submenu item
        * @return menushortcut for this menu item
        */
        //    public MenuShortcut getMenuShortcut(int index);

        /** Get context help for the specified item.
        * This can be used to associate help with individual items.
        * You may return <code>null</code> to just use the context help for
        * the associated system action (if any).
        * Note that only help IDs will work, not URLs.
        * @return the context help, or <code>null</code>
        */
        public HelpCtx getHelpCtx (int index);

        /** Perform the action on the specific index
        * @param index of the submenu item which should be performed
        */
        public void performActionAt(int index);

        /** Adds change listener for changes of the model.
        */
        public void addChangeListener (ChangeListener l);

        /** Removes change listener for changes of the model.
        */
        public void removeChangeListener (ChangeListener l);

    }

    /** SubMenu provides easy way of displaying submenu items based on
    * SubMenuModel.
    */
    public static class SubMenu extends org.openide.awt.JMenuPlus {
        /** number of previous sub items */
        int previousCount = -1;
        /** listener to remove from this menu or <CODE>null</CODE> */
        ActionListener oneItemListener;
        /** The keystroke which acts as the menu's accelerator.
         * This menu can have an accelerator! */
        private KeyStroke accelerator;
        
	/** The model of the submenu used in menuitem generation */
        private SubMenuModel subModel;
        private SubMenuBridge bridge;

        /** Constructs a new ActMenuItem with the specified label
        * and no keyboard shortcut and connects it to the given SystemAction.
        * No icon is used by default.
        * @param aAction the action to which this menu item should be connected
        * @param model the support for the menu items
        */
        public SubMenu(SystemAction aAction, SubMenuModel model) {
            this (aAction, model, true);
        }

        static final long serialVersionUID =-4446966671302959091L;
        /** Constructs a new ActMenuItem with the specified label
        * and no keyboard shortcut and connects it to the given SystemAction.
        * No icon is used by default.
        * @param aAction the action to which this menu item should be connected
        * @param model the support for the menu items
        * @param popup whether this is a popup menu
        */
        public SubMenu(SystemAction aAction, SubMenuModel model, boolean popup) {
            this ((Action)aAction, model, popup);
        }
            
        /** Constructs a new ActMenuItem with the specified label
        * and no keyboard shortcut and connects it to the given SystemAction.
        * No icon is used by default.
        * @param aAction the action to which this menu item should be connected
        * @param model the support for the menu items
        * @param popup whether this is a popup menu
        */
        public SubMenu(Action aAction, SubMenuModel model, boolean popup) {
            subModel = model;
            bridge = new SubMenuBridge (this, aAction, model, popup);
            // set at least the name to have reasonable bounds
            bridge.updateState (Action.NAME);
        }

        // Fixes #26619
        /** Overriden to finish initialization of the bridge on demand
         */
        public void addNotify() {
            super.addNotify();
            bridge.updateState (null);
            // Empty SubMenu -> disable
	    if (subModel.getCount() == 0) setEnabled(false);
        }

        // XXX Overriding processKeyBinding is not a nice solution, used as
        // a last resort here to fix the bug.
        // #9331. Missed accelerator for Paste action.
        /** Overrides superclass method.
         * If it has accelerator delegates processing of it to the first item. */
        protected boolean processKeyBinding(
        KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            // If it is as accelerator process the doClick binding to the
            // first sub-item.
            if(ks.equals(accelerator)) {
                // Use first item if there is one.
                Component[] cs = getMenuComponents();
                if(cs.length > 0 && cs[0] instanceof JComponent) {
                    JComponent comp = (JComponent)cs[0];
                    
                    ActionMap am = comp.getActionMap();
                    if(am != null && comp.isEnabled()) {
                        Action action = am.get("doClick"); // NOI18N
                        
                        if (action != null) {
                            return SwingUtilities.notifyAction(
                                action, ks, e, comp, e.getModifiers());
                        }
                    }
                    return false;
                }
            }
            
            return super.processKeyBinding(ks, e, condition, pressed);
        }
        
        // XXX #11048. Ugly patch.
        // This works for the cases when this menu is in 'menu'
        // (not popup), the popup is handled by NbPopupMenuUI hack. This same
        // method for popup wouldn't work since NbPopupMenuUI automatically
        // passes focus to sub-menu.
        /** Overrides superclass method. Adds a hack for KB menu invokation 
         * when this <code>JMenu</code> needs to act like <code>JMenuItem</code>. */
        public void processKeyEvent(KeyEvent e, MenuElement[] path, MenuSelectionManager m) {
            if(getMenuComponentCount() <= 1
            && java.util.Arrays.equals(
                path, MenuSelectionManager.defaultManager().getSelectedPath())
            && (e.getKeyCode() == KeyEvent.VK_ENTER
                || e.getKeyCode() == KeyEvent.VK_SPACE)
            ) {
                ActionListener ac = oneItemListener;
                if(ac != null) {
                    m.setSelectedPath(new MenuElement[0]);
                    ac.actionPerformed(new ActionEvent(e.getSource(), 0, null));
                    
                    return;
                }
            }
            
            super.processKeyEvent(e, path, m);
        }
        
        /** Request for either MenuUI or MenuItemUI if the only one subitem should not
        * use submenu.
        */
        public String getUIClassID () {
            if (previousCount == 0) {
                return "MenuItemUI"; // NOI18N
            }
            return previousCount == 1 ? "MenuItemUI" : "MenuUI"; // NOI18N
        }

        boolean useAccel() {
            return subModel.getCount() <= 1;
        }
        
        /** Overrides superclass method to be able to have an accelerator. */
        public void setAccelerator(KeyStroke keyStroke) {
            KeyStroke oldAccelerator = accelerator;
            this.accelerator = keyStroke;
            // Note: "accelerator" for the bean prop, not Action.ACCELERATOR_KEY == "AcceleratorKey"
            firePropertyChange("accelerator", oldAccelerator, accelerator); // NOI18N
        }

        /** Overrides superclass method to be able to have an accelerator. */
        public KeyStroke getAccelerator() {
            return this.accelerator;
        }

        public void menuSelectionChanged(boolean isIncluded) {
            if (previousCount <= 1)
                setArmed(isIncluded); // JMenuItem behaviour
            else
                super.menuSelectionChanged(isIncluded);
        }

        /** Menu cannot be selected when it represents MenuItem.
        */
        public void setSelected (boolean s) {
            // disabled menu cannot be selected
            if (isEnabled () || !s) {
                super.setSelected (s);
            }
        }

        /** Seting menu to disabled also sets the item as not selected
        */
        public void setEnabled (boolean e) {
            super.setEnabled (e);
            if (!e) {
                super.setSelected (false);
            }

        }

        public void doClick(int pressTime) {
            if (!isEnabled ()) {
                // do nothing if not enabled
                return;
            }

            if (oneItemListener != null) {
                oneItemListener.actionPerformed (null);
            } else {
                super.doClick (pressTime);
            }
        }
    }

}
