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

package org.openide.explorer.propertysheet;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;

import org.openide.awt.JPopupMenuPlus;
import org.openide.awt.MouseUtils;
import org.openide.awt.SplittedPanel;

import org.openide.ErrorManager;
import org.openide.nodes.Node;

import org.openide.actions.PopupAction;
import org.openide.util.actions.ActionPerformer;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.actions.SystemAction;

import org.openide.util.NbBundle;
import org.openide.util.WeakListener;
import org.openide.util.Mutex;

/**
 * A JPanel in property sheet showing a set of properties. The set
 * is represented by a Node.PropertySet instance.
 * @author  David Strupl
 */
class PropertySheetTab extends JPanel implements PropertyChangeListener {

    /** Panel with SheetButtons with names of properties. */
    private NamesPanel namesPanel;
    
    /** Panel with PropertyPanels displaying the property values. */
    private NamesPanel valuesPanel;
    
    /** Set of properties in this tab. */
    private Node.PropertySet properties;
    
    /** */
    private Node node;
    
    /**
     * Maps property name (String) --> model from property panel 
     * (PropertyModel)
     */
    private HashMap modelCache;
    
    /** Using this stop stop neverending loops caused
     * by nodes that fire property change inside getXXX methods.
     */
    private boolean changeInProgress;
    
    /** Listens on changes in the global settings for sorter and
     * displayWritableOnly.
     */
    private SettingsListener settingsListener;
    
    /** Comparator for instances of Node.Property */
    private Comparator sorter;
    
    /** Value of this can be one of the constants defined in PropertySheet
     * (UNSORTED, SORTED_BY_NAMES, SORTED_BY_TYPES).
     */
    private int sortingMode;
    
    /** When it's true only writable properties are shown. */
    private PropertySheet mySheet;
    
    /** Comparator which compares types */
    private final static Comparator SORTER_TYPE = new Comparator () {
                public int compare (Object l, Object r) {
                    if (! (l instanceof Node.Property)) {
                        throw new IllegalArgumentException("Can compare only Node.Property instances."); // NOI18N
                    }
                    if (! (r instanceof Node.Property)) {
                        throw new IllegalArgumentException("Can compare only Node.Property instances."); // NOI18N
                    }
                    
                    Class t1 = ((Node.Property)l).getValueType();
                    Class t2 = ((Node.Property)r).getValueType();
                    String s1 = t1 != null ? t1.getName() : "";
                    String s2 = t2 != null ? t2.getName() : "";
                    
                    int s = s1.compareToIgnoreCase (s2);
                    if (s != 0) return s;

                    s1 = ((Node.Property)l).getDisplayName();
                    s2 = ((Node.Property)r).getDisplayName();
                    return s1.compareToIgnoreCase(s2);
                }
            };

    /** Comparator which compares PropertyDeatils names */
    private final static Comparator SORTER_NAME = new Comparator () {
                public int compare (Object l, Object r) {
                    if (! (l instanceof Node.Property)) {
                        throw new IllegalArgumentException("Can compare only Node.Property instances."); // NOI18N
                    }
                    if (! (r instanceof Node.Property)) {
                        throw new IllegalArgumentException("Can compare only Node.Property instances."); // NOI18N
                    }
                    String s1 = ((Node.Property)l).getDisplayName();
                    String s2 = ((Node.Property)r).getDisplayName();
                    return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
                }
            };

    /** Popup menu used for in this sheet. */
    private JPopupMenu popupMenu;

    /** Creates new PropertySheetTab */
    public PropertySheetTab(Node.PropertySet properties, Node node, PropertySheet mySheet) {
        this.properties = properties;
        this.node = node;
        modelCache = new HashMap();
        this.mySheet = mySheet;

        setLayout (new BorderLayout ());
        add (new EmptyPanel (properties.getDisplayName()), BorderLayout.CENTER);
        
        try {
            setSortingMode(mySheet.getSortingMode());
        } catch (PropertyVetoException x) {
            ErrorManager.getDefault ().notify (x);
        }

        settingsListener = new SettingsListener();
        mySheet.addPropertyChangeListener(
            WeakListener.propertyChange(
                settingsListener, 
                mySheet
            )
        );
    }
    
    public void addNotify () {
        super.addNotify();
        if (node != null) node.addPropertyChangeListener (this);
    }
    
    public void removeNotify () {
        if (node != null) node.removePropertyChangeListener (this);
        super.removeNotify();
    }

    void detachPropertyChangeListener() {
        node.removePropertyChangeListener( this );
    }
    
    void setActions (final Node.Property pd) {
        final CallbackSystemAction setDefault = (CallbackSystemAction)SystemAction
            .get(SetDefaultValueAction.class);
        
        // Enable / Disable DefaultValueAction
        if (pd.supportsDefaultValue () && pd.canWrite ()) {
            setDefault.setActionPerformer (new ActionPerformer () {
                public void performAction (SystemAction a) {
                    try {
                        pd.restoreDefaultValue ();

                        // workaround of bug #21182: forces a node's property change
                        propertyChange (
                               new PropertyChangeEvent (this, pd.getName (), null, null));
                    } catch (Exception e) {
                        setDefault.setActionPerformer (null);
                    }
                }
            });
        } else {
            setDefault.setActionPerformer (null);
        }
    }

    private boolean paneCreated;
    
    void ensurePaneCreated() {
        if (!paneCreated) {
            createPane();
        }
    }
    
    /**
     * Displays either empty panel or namesPanel and valuesPanel.
     */
    private void createPane () {
        paneCreated = true;
        
        Component c = getComponent(0);
        if (properties.getProperties().length == 0) {
            if ((c != null) && (c instanceof EmptyPanel)) {
                // empty panel already there
                return;
            }
            removeAll ();
            add (new EmptyPanel (properties.getDisplayName()), BorderLayout.CENTER);
            invalidate();
            validate();
            repaint();
            return;
        }

        if (namesPanel == null) {
            namesPanel = new NamesPanel ();
            valuesPanel = new NamesPanel (namesPanel);
        } else {
            namesPanel.removeAll ();
            valuesPanel.removeAll ();
        }
        if ((c == null) || !(c instanceof JScrollPane)) {
            removeAll ();
            JScrollPane scrollPane = new JScrollPane ();
            scrollPane.setBorder (null);
            SplittedPanel splittedPanel = new ScrollableSplittedPanel (scrollPane, namesPanel);
            splittedPanel.add (namesPanel, SplittedPanel.ADD_LEFT);
            splittedPanel.add (valuesPanel, SplittedPanel.ADD_RIGHT);
            splittedPanel.setSplitAbsolute (true);

            scrollPane.setViewportView (splittedPanel);
            scrollPane.setHorizontalScrollBarPolicy (JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.getVerticalScrollBar ().setUnitIncrement (25);
            add (scrollPane, BorderLayout.CENTER);
        }
        fillProperties();
    }
    
    /**
     * Sorts the properties with a sorter. Creates a SheetButtons and
     * PropertyPanels for the display and adds them to namesPanel and
     * valuesPanel.
     */
    private void fillProperties() {
        Node.Property[]p = properties.getProperties();
        
        ArrayList a = new ArrayList(p.length);
        for (int i = 0; i < p.length; i++) {
            if (mySheet.getDisplayWritableOnly() && !p[i].canWrite()) {
                continue;
            }
            a.add(p[i]);
        }
        if (sorter != null) {
            Collections.sort(a, sorter);
        }
        
        Object [] beans = new Object[] { node };
        if (node instanceof ProxyNode) {
            beans = ((ProxyNode)node).getOriginalNodes();
        }
        
        for (Iterator i = a.iterator(); i.hasNext(); ) {
            final Node.Property prop = (Node.Property)i.next();

            class LazyToolTipSheetButton extends SheetButton {
                /** cache it, and do not compute it until requested */
                private String toolTipText = null;
                Node.Property pr;
                
                public LazyToolTipSheetButton(Node.Property pr) {
                    super(pr.getDisplayName(), false, true);
                    // Cause it to be registered with manager:
                    this.setToolTipText("dummy"); // NOI18N
                    this.pr = pr;
                }
                public String getToolTipText(MouseEvent event) {
                    if (toolTipText == null) {
                        toolTipText = getToolTipTextForProperty(pr);
                    }
                    return toolTipText;
                }
            }
            final SheetButton leftButton = new LazyToolTipSheetButton(prop);
            leftButton.setFocusTraversable(false);
            
            namesPanel.add(leftButton);
            PropertyPanel rightPanel = new PropertyPanel(prop, beans);
            modelCache.put(prop.getName(), rightPanel.getModel());
            valuesPanel.add(rightPanel);
            ButtonListener listener = new ButtonListener(leftButton, rightPanel);
            rightPanel.addSheetButtonListener(listener);
            leftButton.addSheetButtonListener(listener);
            leftButton.setPlastic(rightPanel.getPlastic());
            if (prop.canWrite()) {
                leftButton.setActiveForeground(mySheet.getValueColor());
            } else {
                leftButton.setActiveForeground(mySheet.getDisabledPropertyColor());
            }

            leftButton.addMouseListener (
                new MouseUtils.PopupMouseAdapter () {
                    public void showPopup (MouseEvent ev) {
                        setActions(prop);
                        createPopup();
                        popupMenu.show (leftButton, ev.getX(), ev.getY ());
                    }
                }
            );
            rightPanel.addSheetButtonListener(
                new InstallPerformerListener(rightPanel));
        }
        invalidate();
        validate();
        repaint();
    }

    /**
     * Set whether buttons in sheet should be plastic.
     * @param plastic true if so
     */
    void setPlastic (boolean plastic) {
        int count = namesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (namesPanel.getComponent(i) instanceof SheetButton) {
                ((SheetButton)namesPanel.getComponent(i)).setPlastic(plastic);
            }
        }
        count = valuesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (valuesPanel.getComponent(i) instanceof PropertyPanel) {
                ((PropertyPanel)valuesPanel.getComponent(i)).setPlastic(plastic);
            }
        }
    }

    /**
     * Set the foreground color of values.
     * @param color the new color
     */
    void setForegroundColor (Color color) {
        int count = namesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (namesPanel.getComponent(i) instanceof SheetButton) {
                ((SheetButton)namesPanel.getComponent(i)).setActiveForeground(color);
            }
        }
        count = valuesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (valuesPanel.getComponent(i) instanceof PropertyPanel) {
                ((PropertyPanel)valuesPanel.getComponent(i)).setForegroundColor(color);
            }
        }
    }

    /**
     * Set the foreground color of disabled properties.
     * @param color the new color
     */
    void setDisabledColor (Color color) {
        int count = namesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (namesPanel.getComponent(i) instanceof SheetButton) {
                ((SheetButton)namesPanel.getComponent(i)).setInactiveForeground(color);
            }
        }
        count = valuesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (valuesPanel.getComponent(i) instanceof PropertyPanel) {
                ((PropertyPanel)valuesPanel.getComponent(i)).setDisabledColor(color);
            }
        }
    }

    void setPaintingStyle(int style) {
        int count = valuesPanel.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (valuesPanel.getComponent(i) instanceof PropertyPanel) {
                ((PropertyPanel)valuesPanel.getComponent(i)).setPaintingStyle(style);
            }
        }
    }
    
    /**
     * Set the sorting mode.
     *
     * @param sortingMode one of {@link #UNSORTED}, {@link #SORTED_BY_NAMES}, {@link #SORTED_BY_TYPES}
     */
    public void setSortingMode (int sortingMode) throws PropertyVetoException {
        switch (sortingMode) {
        case PropertySheet.UNSORTED:
            sorter = null;
            break;
        case PropertySheet.SORTED_BY_NAMES:
            sorter = SORTER_NAME;
            break;
        case PropertySheet.SORTED_BY_TYPES:
            sorter = SORTER_TYPE;
            break;
        default:
            throw new PropertyVetoException (
                getString ("EXC_Unknown_sorting_mode"),
                new PropertyChangeEvent (this, PropertySheet.PROPERTY_SORTING_MODE,
                new Integer (this.sortingMode),
                new Integer (sortingMode))
            );
        }
        
        int oldSortingMode = this.sortingMode;
        this.sortingMode = sortingMode;
        firePropertyChange(PropertySheet.PROPERTY_SORTING_MODE, oldSortingMode, this.sortingMode);
    }

    /**
     * Get the sorting mode.
     *
     * @return the mode
     * @see #setSortingMode
     */
    public int getSortingMode () {
        return sortingMode;
    }

    /** Gets help ID for this property sheet tab.
     * @see PropertySheet.HelpAwareJTabbedPane#getHelpCtx */
    String getHelpID() {
        return (String)properties.getValue("helpID"); // NOI18N
    }
    
    private static String getString(String key) {
        return NbBundle.getBundle(PropertySheetTab.class).getString(key);
    }
    
    /** Constructs tooltip for <code>Node.Property</code>. Helper method. */
    private static String getToolTipTextForProperty(Node.Property prop) {
        StringBuffer buff = new StringBuffer(); // NOI18N
        buff.append(prop.canRead() 
            ? getString("CTL_Property_Read_Yes") 
            : getString("CTL_Property_Read_No"));

        buff.append(prop.canWrite() 
            ? getString("CTL_Property_Write_Yes")
            : getString("CTL_Property_Write_No"));
            
        buff.append(' ');
        String shortDesc = prop.getShortDescription();
        buff.append(shortDesc != null ? shortDesc : prop.getDisplayName());
        
        return buff.toString();
    }
    
    /** Fires a value change in property's model.
     * @param propertyName property name */
    private void doPropertyChange (String propertyName) {
        if (changeInProgress) {
            // this is here to assure that if a node would
            // refire back our property change event we
            // should not end up in infinite loop
            return;
        }
        PropertyModel m = (PropertyModel)modelCache.get (propertyName);
        if (m == null) {
            // the model is not in our cache, probably we are not displaying
            // this property --> do nothing in such case
            return;
        }
        if (m instanceof PropertyPanel.SimpleModel) {
            PropertyPanel.SimpleModel sm = (PropertyPanel.SimpleModel)m;
            try {
                changeInProgress = true;
                sm.fireValueChanged();
            } finally {
                changeInProgress = false;
            }
        }
    }
    
    private static final HashSet propsToIgnore = new HashSet();
    static {
        propsToIgnore.addAll (Arrays.asList (new String[] {
            Node.PROP_COOKIE, Node.PROP_ICON, Node.PROP_OPENED_ICON,
            Node.PROP_PARENT_NODE
        }));
    };

    /**
     * This is attached to the node we are taking properties from.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (propsToIgnore.contains (evt.getPropertyName())) {
            // ignore cookie changes and such, they're noise and 
            // often fired from settings nodes
            return;
        }
        
        if ((evt.getPropertyName () != null) && (evt.getSource ().equals (node))) {
            doPropertyChange (evt.getPropertyName ());
        } else {
            // bugfix #20427 if was firePropertyChange(null, null, null)
            // then a property's value change is fired on all node's properties
            Node.Property []prop = properties.getProperties ();

            for (int i = 0; i < prop.length; i++) {
                doPropertyChange (prop[i].getName ());
            }
        }
    }
    
    /**
     * Lazy creation of the popup menu. Adds SetDeafulValuetAction
     * to the menu.
     */
    private void createPopup() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenuPlus();
            //    popupMenu.add (new CopyAction ().getPopupPresenter ());
            //    popupMenu.add (new PasteAction ().getPopupPresenter ());
            //    popupMenu.addSeparator ();
            CallbackSystemAction setDefault = (CallbackSystemAction)SystemAction.get(SetDefaultValueAction.class);
            popupMenu.add(setDefault.getPopupPresenter());
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Shows the popup (in AWT thread).
     */
    private final class PopupPerformer implements org.openide.util.actions.ActionPerformer {
        private PropertyPanel panel;
        
        public PopupPerformer(PropertyPanel p) {
            panel = p;
        }
        
        public void performAction(SystemAction act) {
            Mutex.EVENT.readAccess(new Runnable() {
                public void run() {
                    PropertyModel pm = panel.getModel();
                    if (pm instanceof ExPropertyModel) {
                        ExPropertyModel epm = (ExPropertyModel)pm;
                        FeatureDescriptor fd = epm.getFeatureDescriptor();
                        if (fd instanceof Node.Property) {
                            Node.Property np = (Node.Property)fd;
                            setActions(np);
                            createPopup();
                            popupMenu.show(panel, 0, 0);
                        }
                    }
                }
            });
        }
    }

    /** 
     * Listens on the property panel and installs and uninstalls
     * appropriate PopupPerformer (for the supplied PropertyPanel).
     */
    private final class InstallPerformerListener implements SheetButtonListener {
        
        private CallbackSystemAction csa;
        private PopupPerformer performer;
        private PropertyPanel panel;
        
        public InstallPerformerListener(PropertyPanel p) {
            panel = p;
        }

        public void sheetButtonClicked(ActionEvent e) { }
        
        public void sheetButtonEntered(ActionEvent e) {
            if (csa == null) {
                csa = (CallbackSystemAction) SystemAction.get (PopupAction.class);
                performer = new PopupPerformer(panel);
            }
            csa.setActionPerformer(performer);
        }
        
        public void sheetButtonExited(ActionEvent e) {
            if (csa != null && (csa.getActionPerformer() instanceof PopupPerformer)) {
                csa.setActionPerformer(null);
            }
        }
    }    
    
    /** Listener updating the two adjacent buttons */
    private final class ButtonListener implements SheetButtonListener {
        
        private SheetButton b;
        private PropertyPanel p;
        
        public ButtonListener(SheetButton b, PropertyPanel p) {
            this.b = b;
            this.p = p;
        }
        
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void sheetButtonClicked(ActionEvent e) {
            // Fix #15885. Avoid setting 'writeState' if there was
            // right mouse button clicked -> popup is about to show.
            if(SheetButton.RIGHT_MOUSE_COMMAND.equals(e.getActionCommand())) {
                if(p.isWriteState()) {
                    p.setReadState();
                }
                
                return;
            }
            
            if (e.getSource() == b) {
                // Is double click?
                if(e.getID() == ActionEvent.ACTION_FIRST + 2) {
                    p.tryToSelectNextTag();
                } else if(p.isWriteState()) {
                    p.setReadState();
                    p.requestDefaultFocus();
                } else {
                    p.setWriteState();
                }
            }
        }
        
        /**
         * Invoked when the mouse enters a component.
         */
        public void sheetButtonEntered(ActionEvent e) {
            if (e.getSource() == b) {
                if (p.getReadComponent() != null) {
                    p.getReadComponent().setPressed(true);
                }
            } else {
                b.setPressed(true);
            }
        }
        
        /**
         * Invoked when the mouse exits a component.
         */
        public void sheetButtonExited(ActionEvent e) {
            if (e.getSource() == b) {
                if (p.getReadComponent() != null) {
                    p.getReadComponent().setPressed(false);
                }
            } else {
                b.setPressed(false);
            }
        }
    }
    
    // Settings listener
    final class SettingsListener implements PropertyChangeListener {
        public void propertyChange (PropertyChangeEvent e) {
            String name = e.getPropertyName ();

            if (name == null) return;

            if (name.equals (PropertySheet.PROPERTY_SORTING_MODE)) {
                try {
                    setSortingMode (((Integer)e.getNewValue ()).intValue ());
                    if (paneCreated)
                        createPane();
                } catch (PropertyVetoException ee) {
                    PropertyDialogManager.notify(ee);
                }
            } else if (name.equals (PropertySheet.PROPERTY_DISPLAY_WRITABLE_ONLY)) {
                if (paneCreated)
                    createPane();
            } else if (name.equals (PropertySheet.PROPERTY_VALUE_COLOR)) {
                setForegroundColor ((Color)e.getNewValue ());
            } else if (name.equals (PropertySheet.PROPERTY_DISABLED_PROPERTY_COLOR)) {
                setDisabledColor ((Color)e.getNewValue ());
            } else if (name.equals (PropertySheet.PROPERTY_PLASTIC)) {
                setPlastic (((Boolean)e.getNewValue ()).booleanValue ());
            } else if (name.equals (PropertySheet.PROPERTY_PROPERTY_PAINTING_STYLE)) {
                setPaintingStyle (((Integer)e.getNewValue ()).intValue ());
            }
        }
    }

    /**
     * Scrollable enhancement of SplittedPanel.
     */
    private class ScrollableSplittedPanel extends SplittedPanel implements Scrollable {
        private Component scroll;
        private Container element;

        ScrollableSplittedPanel (Component scroll, Container element) {
            this.scroll = scroll;
            this.element = element;
            setSplitPosition (mySheet.getSavedPosition ());
            JComponent c = new JPanel();
            c.setPreferredSize (new Dimension(2,2));
            setSplitterComponent(c);
            //make borders consistent
            javax.swing.border.Border b = 
                UIManager.getBorder ("nb.splitChildBorder"); //NOI18N
            if (b != null) {
                setBorder (b); 
            }
        }

        /**
        * Returns the preferred size of the viewport for a view component.
        *
        * @return The preferredSize of a JViewport whose view is this Scrollable.
        */
        public Dimension getPreferredScrollableViewportSize () {
            return super.getPreferredSize ();
        }

        /**
        * @param visibleRect The view area visible within the viewport
        * @param orientation Either SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
        * @param direction Less than zero to scroll up/left, greater than zero for down/right.
        * @return The "unit" increment for scrolling in the specified direction
        */
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            Component[] c = element.getComponents ();
            if (c.length < 1) return 1;
            Dimension d = c [0].getSize ();
            if (orientation == SwingConstants.VERTICAL) return d.height;
            else return d.width;
        }

        /**
        * @param visibleRect The view area visible within the viewport
        * @param orientation Either SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
        * @param direction Less than zero to scroll up/left, greater than zero for down/right.
        * @return The "block" increment for scrolling in the specified direction.
        */
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            if (orientation == SwingConstants.VERTICAL) return scroll.getSize ().height;
            else return scroll.getSize ().width;
        }


        /**
        * Return true if a viewport should always force the width of this
        * Scrollable to match the width of the viewport.
        *
        * @return True if a viewport should force the Scrollables width to match its own.
        */
        public boolean getScrollableTracksViewportWidth () {
            return true;
        }

        /**
        * Return true if a viewport should always force the height of this
        * Scrollable to match the height of the viewport.
        *
        * @return True if a viewport should force the Scrollables height to match its own.
        */
        public boolean getScrollableTracksViewportHeight () {
            return false;
        }
        
        /**
         * Overriden to remember the split position.
         */
        public void setSplitPosition(int value) {
            super.setSplitPosition(value);
            mySheet.setSavedPosition (value);
        }
    } // End of class ScrollableSplittedPanel.
    
}
