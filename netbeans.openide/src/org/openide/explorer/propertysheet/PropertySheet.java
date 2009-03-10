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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.Method;

import javax.swing.*;
import javax.swing.event.*;

import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.awt.SplittedPanel;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Mutex;
import org.openide.util.WeakListener;
import org.openide.util.Utilities;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeOperation;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
* Implements a "property sheet" for a set of selected beans.
*
* <P>
* <TABLE BORDER COLS=3 WIDTH=100%>
* <TR><TH WIDTH=15%>Property<TH WIDTH=15%>Property Type<TH>Description
* <TR><TD> <code>paintingStyle</code> <TD> <code>int</code>     <TD> style of painting properties ({@link #ALWAYS_AS_STRING}, {@link #STRING_PREFERRED}, {@link #PAINTING_PREFERRED})
* <TR><TD> <code>currentPage</code>   <TD> <code>int</code>     <TD> currently showed page (e.g. properties, expert, events)
* <TR><TD> <code>expert</code>        <TD> <code>boolean</code> <TD> expert mode as in the JavaBeans specifications
* </TABLE>
*
* @author   Jan Jancura, Jaroslav Tulach
* @version  1.23, Sep 07, 1998
*/
public class PropertySheet extends JPanel {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -7698351033045864945L;
    

    // public constants ........................................................

    /** Property giving current sorting mode. */
    public static final String    PROPERTY_SORTING_MODE = "sortingMode"; // NOI18N
    /** Property giving current value color. */
    public static final String    PROPERTY_VALUE_COLOR = "valueColor"; // NOI18N
    /** Property giving current disabled property color. */
    public static final String    PROPERTY_DISABLED_PROPERTY_COLOR = "disabledPropertyColor"; // NOI18N
    /** Property with the current page index. */
    public static final String    PROPERTY_CURRENT_PAGE = "currentPage"; // NOI18N
    /** Property for "plastic" mode. */ // NOI18N
    public static final String    PROPERTY_PLASTIC = "plastic"; // NOI18N
    /** Property for the painting style. */
    public static final String    PROPERTY_PROPERTY_PAINTING_STYLE = "propertyPaintingStyle"; // NOI18N
    /** Property for whether only writable properties should be displayed. */
    public static final String    PROPERTY_DISPLAY_WRITABLE_ONLY = "displayWritableOnly"; // NOI18N

    /** Constant for showing properties as a string always. */
    public static final int       ALWAYS_AS_STRING = 1;
    /** Constant for preferably showing properties as string. */
    public static final int       STRING_PREFERRED = 2;
    /** Constant for preferably painting property values. */
    public static final int       PAINTING_PREFERRED = 3;

    /** Constant for unsorted sorting mode. */
    public static final int       UNSORTED = 0;
    /** Constant for by-name sorting mode. */
    public static final int       SORTED_BY_NAMES = 1;
    /** Constant for by-type sorting mode. */
    public static final int       SORTED_BY_TYPES = 2;

    /** Init delay for second change of the selected nodes. */
    private static final int INIT_DELAY = 70;
    
    /** Maximum delay for repeated change of the selected nodes. */
    private static final int MAX_DELAY = 350;
    
    /** Icon for the toolbar.
     * @deprecated Presumably noone uses this variable. If you want to customize
     *  the property sheet look you can change the image files directly (or use your
     *  own).
     */
    static protected Icon         iNoSort;
    /** Icon for the toolbar.
     * @deprecated Presumably noone uses this variable. If you want to customize
     *  the property sheet look you can change the image files directly (or use your
     *  own).
     */
    static protected Icon         iAlphaSort;
    /** Icon for the toolbar.
     * @deprecated Presumably noone uses this variable. If you want to customize
     *  the property sheet look you can change the image files directly (or use your
     *  own).
     */
    static protected Icon         iTypeSort;
    /** Icon for the toolbar.
     * @deprecated Presumably noone uses this variable. If you want to customize
     *  the property sheet look you can change the image files directly (or use your
     *  own).
     */
    static protected Icon         iDisplayWritableOnly;
    /** Icon for the toolbar.
     * @deprecated Presumably noone uses this variable. If you want to customize
     *  the property sheet look you can change the image files directly (or use your
     *  own).
     */
    static protected Icon         iCustomize;
    
    static final String PROP_HAS_CUSTOMIZER = "hasCustomizer"; // NOI18N
    static final String PROP_PAGE_HELP_ID = "pageHelpID"; // NOI18N
    
    private static String getString(String key) {
        return NbBundle.getBundle(PropertySheet.class).getString(key);
    }
    
    /** Remember the position of the splitter. This is used when the property window is re-used for another node  */
    private int savedSplitterPosition = SplittedPanel.FIRST_PREFERRED;

    // private variables for visual controls ...........................................

    private transient JTabbedPane           pages;
    private transient EmptyPanel            emptyPanel;
    
    private transient int                   pageIndex = 0;

    private transient ChangeListener        tabListener =
        new ChangeListener () {
            public void stateChanged (ChangeEvent e) {
                int index = pages.getSelectedIndex ();
                
                setCurrentPage (index);
            }
        };
    private Node activeNode;
    private NodeListener activeNodeListener;
    private boolean displayWritableOnly;
    private int propertyPaintingStyle;
    private int sortingMode;
    private boolean plastic;
    private Color disabledPropertyColor;
    private Color valueColor;

    // init .............................................................................

    public PropertySheet() {
        setLayout (new BorderLayout ());
        
        boolean problem = false;
        try {
            Class c = Class.forName("org.openide.explorer.propertysheet.PropertySheet$PropertySheetSettingsInvoker"); // NOI18N
            Runnable r = (Runnable)c.newInstance();
            current.set(this);
            r.run();
        } catch (Exception e) {
            problem = true;
        } catch (LinkageError le) {
            problem = true;
        }
        if (problem) {
            // set defaults without P ropertySheetSettings
            displayWritableOnly = false;
            propertyPaintingStyle = PAINTING_PREFERRED;
            sortingMode = SORTED_BY_NAMES;
            plastic = false;
            disabledPropertyColor = UIManager.getColor("textInactiveText");
            valueColor = new Color (0, 0, 128);
        }

        pages = new HelpAwareJTabbedPane ();
        pages.getAccessibleContext().setAccessibleName(getString("ACS_PropertySheetTabs"));
        pages.getAccessibleContext().setAccessibleDescription(getString("ACSD_PropertySheetTabs"));
        pages.addChangeListener(tabListener);
        emptyPanel = new EmptyPanel (getString ("CTL_NoProperties"));
        pages.setTabPlacement (JTabbedPane.BOTTOM);
//        add (emptyPanel, BorderLayout.CENTER);

        PropertySheetToolbar p = new PropertySheetToolbar(this);
        p.setBorder (UIManager.getBorder ("Toolbar.border"));
        addPropertyChangeListener(p);
        add (p, BorderLayout.NORTH);

        
//        setNodes(new Node[0]);

        setPreferredSize(new Dimension(280, 300));
    }
    
        /** Overridden to provide a larger preferred size if the default font
         *  is larger, for locales that require this.   */
        public Dimension getPreferredSize() {
            //issue 34157, bad sizing/split location for Chinese locales that require
            //a larger default font size
            Dimension result = super.getPreferredSize();
            int fontsize = 
                javax.swing.UIManager.getFont ("Tree.font").getSize(); //NOI18N
            if (fontsize > 11) {
                int factor = fontsize - 11;
                result.height += 15 * factor;
                result.width += 50 * factor;
                Dimension screen = Utilities.getScreenSize();
                if (result.height > screen.height) {
                    result.height = screen.height -30;
                }
                if (result.width > screen.width) {
                    result.width = screen.width -30;
                }
            } else {
                result.width += 20;
                result.height +=20;
            }
            return result;
        }
    static ThreadLocal current = new ThreadLocal();

    /** Reference to PropertySheetSettings are separated here.*/
    private static class PropertySheetSettingsInvoker implements Runnable {
        // constructor avoid IllegalAccessException during creating new instance
        public PropertySheetSettingsInvoker() {}

        public void run() {
            PropertySheet instance = (PropertySheet)current.get();
            current.set(null);
            if (instance == null) {
                throw new IllegalStateException();
            }
            PropertySheetSettings pss = PropertySheetSettings.getDefault();
            instance.displayWritableOnly = pss.getDisplayWritableOnly();
            instance.propertyPaintingStyle = pss.getPropertyPaintingStyle();
            instance.sortingMode = pss.getSortingMode();
            instance.plastic = pss.getPlastic();
            instance.disabledPropertyColor = pss.getDisabledPropertyColor();
            instance.valueColor = pss.getValueColor();
        }
    }

    /**
     * Set the nodes explored by this property sheet.
     *
     * @param nodes nodes to be explored
     */
    public void setNodes (Node[] nodes) {
        setHelperNodes (nodes);
    }
    
    // delayed setting nodes (partly impl issue 27781)
    private transient Node[] helperNodes;
    
    private synchronized void setHelperNodes (Node[] nodes) {
        RequestProcessor.Task task = getScheduleTask ();
        helperNodes = nodes;
        if (task.equals (initTask)) {
            //if task is only init task then set nodes immediatelly
            scheduleTask.schedule (0);
            task.schedule (INIT_DELAY);
        } else {
            // in a task run then increase delay and reschedule task
            int delay = task.getDelay () * 2;
            if (delay > MAX_DELAY) delay = MAX_DELAY;
            if (delay < INIT_DELAY) delay = INIT_DELAY;
            task.schedule (delay);
        }
    }
    
    private synchronized Node[] getHelperNodes () {
        return helperNodes;
    }
    
    private transient RequestProcessor.Task scheduleTask;
    private transient RequestProcessor.Task initTask;

    private synchronized RequestProcessor.Task getScheduleTask () {
        if (scheduleTask == null) {
            scheduleTask = RequestProcessor.getDefault ().post (new Runnable () {
                public void run () {
                    Node[] nodes = getHelperNodes ();
                    final Node n = (nodes.length == 1) ? nodes[0] : 
                        (nodes.length==0 ? null : new ProxyNode (nodes));
                    SwingUtilities.invokeLater (new Runnable () {
                        public void run () {
                            setCurrentNode (n);
                        }
                    });
                }
            });
            initTask = RequestProcessor.getDefault ().post (new Runnable () {
                public void run () {
                }
            });
            
        }
        // if none task runs then return initTask to wait for next changes
        if (initTask.isFinished () && scheduleTask.isFinished ()) {
            return initTask;
        }
        // if some task runs then return schedule task which will set nodes
        return scheduleTask;
    }
    
    // end of delayed
    
    
    /**
     * Set property paint mode.
     * @param style one of {@link #ALWAYS_AS_STRING}, {@link #STRING_PREFERRED}, or {@link #PAINTING_PREFERRED}
     */
    public void setPropertyPaintingStyle (int style) {
        if (style == propertyPaintingStyle) return;
        
        int oldVal = propertyPaintingStyle;
        propertyPaintingStyle = style;
        firePropertyChange(PROPERTY_PROPERTY_PAINTING_STYLE, new Integer(oldVal), new Integer(style));
    }

    /**
     * Get property paint mode.
     *
     * @return the mode
     * @see #setPropertyPaintingStyle
     */
    public int getPropertyPaintingStyle () {
        return propertyPaintingStyle;
    }

    /**
     * Set the sorting mode.
     *
     * @param sortingMode one of {@link #UNSORTED}, {@link #SORTED_BY_NAMES}, {@link #SORTED_BY_TYPES}
     */
    public void setSortingMode (int sortingMode) throws PropertyVetoException {
        if (this.sortingMode == sortingMode) return;
        
        int oldVal = this.sortingMode;
        this.sortingMode = sortingMode;
        firePropertyChange(PROPERTY_SORTING_MODE, new Integer(oldVal), new Integer(sortingMode));
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

    /**
     * Set the currently selected page.
     *
     * @param index index of the page to select
     */
    public void setCurrentPage (int index) {
        if (pageIndex == index)
            return;
        pageIndex = index;
        if (index < 0)
            return;
        
        if (index != pages.getSelectedIndex ()) {
            pages.setSelectedIndex (index);
        }
        
        int selected = pages.getSelectedIndex();
        if (selected >= 0) {
            Component comp = pages.getComponentAt(selected);
            if (comp instanceof PropertySheetTab) {
                ((PropertySheetTab)comp).ensurePaneCreated();
            }
        }
        firePropertyChange(PROP_PAGE_HELP_ID, null, null);
    }

    /**
    * Set the currently selected page.
    *
    * @param str name of the tab to select
    */
    public boolean setCurrentPage (String str) {
        int index = pages.indexOfTab (str);
        if (index < 0) return false;
        setCurrentPage (index);
        return true;
    }

    /**
    * Get the currently selected page.
    * @return index of currently selected page
    */
    public int getCurrentPage () {
        return pages.getSelectedIndex ();
    }
    
    String getPageHelpID() {
        if (isAncestorOf(pages)) {
            Component comp = pages.getSelectedComponent();
            if (comp instanceof PropertySheetTab) {
                String helpID = ((PropertySheetTab)comp).getHelpID();
                if (helpID != null) {
                    return helpID;
                }
            }
        }
        return null;
    }

    /**
    * Set whether buttons in sheet should be plastic.
    * @param plastic true if so
    */
    public void setPlastic (boolean plastic) {
        if (this.plastic == plastic) return;
        this.plastic = plastic;
        firePropertyChange(PROPERTY_PLASTIC,
            plastic ? Boolean.FALSE:Boolean.TRUE, 
            plastic ? Boolean.TRUE:Boolean.FALSE);
    }

    /**
    * Test whether buttons in sheet are plastic.
    * @return <code>true</code> if so
    */
    public boolean getPlastic () {
        return plastic;
    }

    /**
    * Set the foreground color of values.
    * @param color the new color
    */
    public void setValueColor (Color color) {
        if (valueColor.equals(color)) return;
        
        Color oldVal = valueColor;
        valueColor = color;
        firePropertyChange(PROPERTY_VALUE_COLOR, oldVal, color);
    }

    /**
    * Get the foreground color of values.
    * @return the color
    */
    public Color getValueColor() {
        return valueColor;
    }

    /**
    * Set the foreground color of disabled properties.
    * @param color the new color
    */
    public void setDisabledPropertyColor (Color color) {
        if (disabledPropertyColor.equals(color)) return;
        
        Color oldVal = disabledPropertyColor;
        disabledPropertyColor = color;
        
        firePropertyChange(PROPERTY_DISABLED_PROPERTY_COLOR, oldVal, color);
    }

    /**
    * Get the foreground color of disabled properties.
    * @return the color
    */
    public Color getDisabledPropertyColor () {
        return disabledPropertyColor;
    }

    /**
    * Set whether only writable properties are displayed.
    * @param b <code>true</code> if this is desired
    */
    public void setDisplayWritableOnly (boolean b) {
        if (displayWritableOnly == b) return;
        displayWritableOnly = b;
        firePropertyChange(PROPERTY_DISPLAY_WRITABLE_ONLY,
            b ? Boolean.FALSE:Boolean.TRUE, 
            b ? Boolean.TRUE:Boolean.FALSE);
    }

    /**
    * Test whether only writable properties are currently displayed.
    * @return <code>true</code> if so
    */
    public boolean getDisplayWritableOnly () {
        return displayWritableOnly;
    }
    
    void setSavedPosition (int savedPostion) {
        savedSplitterPosition = savedPostion;
    }
    
    int getSavedPosition () {
        return savedSplitterPosition;
    }

    // private helper methods ....................................................................
    
    private final String detachFromNode () {
        String result = null;
        if (activeNode != null) {
            activeNode.removeNodeListener( activeNodeListener );
            attached = false;
            Node.PropertySet [] oldP = activeNode.getPropertySets();
            if (oldP == null) {
                // illegal node behavior => log warning about it
                ErrorManager.getDefault ().log (ErrorManager.WARNING,
                    "Node "+activeNode+": getPropertySets() returns null!"); // NOI18N
                oldP = new Node.PropertySet[] {};
            }
            if ((pageIndex >= 0) && (pageIndex < oldP.length)) {
                result = oldP[pageIndex].getDisplayName();
            }

            for (int i = 0, tabCount = pages.getTabCount(); i < tabCount; i++) {
                ((PropertySheetTab)pages.getComponentAt(i)).detachPropertyChangeListener();
            }
            pages.removeAll();
        }
        return result;
    }
    
    public void addNotify () {
        super.addNotify();
        if (activeNode != null) {
            if (!attached) {
                attachToNode (activeNode);
                createPages();
                if (storedTab != null) {
                    navToCorrectPage (storedTab);
                    storedTab = null;
                } else {
                    if (pages.getTabCount() > 0) {
                        String first = pages.getTitleAt(0);
                        navToCorrectPage (first);
                    } else {
                        add (emptyPanel, BorderLayout.CENTER);
                    }
                }
            }
        } else {
            remove (pages);
            add (emptyPanel, BorderLayout.CENTER);
        }
    }
    
    private String storedTab = null;
    public void removeNotify() {
        if (attached) storedTab = detachFromNode();
        super.removeNotify();
    }

    private boolean attached=false;
    private final void attachToNode (Node node) {
        //XXX There is probably no reason to be using WeakListener here -
        //attach and detach occasions are well-defined unless two threads
        //enter setCurrentNode concurrently.  Test for this added to
        //setCurrentNode to determine if there are really cases of this
//        activeNodeListener = WeakListener.node (new ActiveNodeListener(), 
//            activeNode);
        if (activeNodeListener == null) {
            activeNodeListener = new ActiveNodeListener();
        }
        activeNode.addNodeListener(activeNodeListener);
        attached = true;
    }
    
    private final void createPages () {
        Node.PropertySet [] propsets = activeNode.getPropertySets();
        if (propsets == null) {
            // illegal node behavior => log warning about it
            ErrorManager.getDefault ().log (ErrorManager.WARNING, 
                "Node "+activeNode+": getPropertySets() returns null!"); // NOI18N
            propsets = new Node.PropertySet[] {};
        }

        for (int i = 0, n = propsets.length; i < n; i++) {
            Node.PropertySet set = propsets[i];

            if (set.isHidden())
                continue;

            pages.addTab(set.getDisplayName(),
                         null, 
                         new PropertySheetTab(set, activeNode, this),
                         set.getShortDescription()
                         );
        }
    }
    
    private final void navToCorrectPage (String selectedTabName) {
        if (isAncestorOf(emptyPanel)) {
                remove(emptyPanel);
            }
            add(pages, BorderLayout.CENTER);
            if (selectedTabName != null) {
                setCurrentPage(selectedTabName);
            }

            int selected = pages.getSelectedIndex();
            if (selected >= 0) {
                Component comp = pages.getComponentAt(selected);
                if (comp instanceof PropertySheetTab) {
                    ((PropertySheetTab)comp).ensurePaneCreated();
                }
            }
    }
    
    /** This has to be called from the AWT thread. */
    private void setCurrentNode(Node node) {
        if (activeNode == node)
            return;

        //if this should only be called from the AWT thread, enforce it
        if (!(SwingUtilities.isEventDispatchThread())) {
            throw new IllegalStateException 
                ("Current node for propertysheet set from off the AWT thread: " //NOI18N
                + Thread.currentThread());
        }

        String selectedTabName = detachFromNode();
        
        activeNode = node;
        if (getParent() == null) {
            return;
        }
        
        if (activeNode != null) {
            attachToNode (activeNode);
            createPages();
            if (pages.getTabCount() > 0) {
                navToCorrectPage(selectedTabName); 
            } else {
                if (isAncestorOf(pages)) {
                    remove(pages);
                    add(emptyPanel, BorderLayout.CENTER);
                }
            }
        } else {
            if (isAncestorOf(pages)) {
                remove(pages);
                add(emptyPanel, BorderLayout.CENTER);
            }
        }
        revalidate();
        repaint();
        
        if (activeNode != null && activeNode.hasCustomizer()) {
            firePropertyChange(PROP_HAS_CUSTOMIZER, null, Boolean.TRUE);
        } else {
            firePropertyChange(PROP_HAS_CUSTOMIZER, null, Boolean.FALSE);
        }
        firePropertyChange(PROP_PAGE_HELP_ID, null, null);
    }

    /**
     * Invokes the customization on the currently selected Node (JavaBean).
     */
    void invokeCustomization () {
        NodeOperation.getDefault().customize(activeNode);
    }
    
    /** Show help on the selected tab.
     */
    void invokeHelp() {
        HelpCtx h = new HelpCtx(getPageHelpID());
        // Awkward but should work. Copied from NbTopManager.showHelp.
        try {
            Class c = ((ClassLoader)Lookup.getDefault().lookup(ClassLoader.class)).loadClass("org.netbeans.api.javahelp.Help"); // NOI18N
            Object o = Lookup.getDefault().lookup(c);
            if (o != null) {
                Method m = c.getMethod("showHelp", new Class[] {HelpCtx.class}); // NOI18N
                m.invoke(o, new Object[] {h});
                return;
            }
        } catch (ClassNotFoundException cnfe) {
            // ignore - maybe javahelp module is not installed, not so strange
        } catch (Exception e) {
            // potentially more serious
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
        }
        // Did not work.
        Toolkit.getDefaultToolkit().beep();
    }
    
    private class ActiveNodeListener extends NodeAdapter {
        int id;
        ActiveNodeListener() {}
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() != activeNode) {
                return;
            }
            if (evt.getPropertyName() == null
                || Node.PROP_PROPERTY_SETS.equals(evt.getPropertyName()))
            {
                // force refresh of the whole sheet, must be done in AWT event
                // thread

                Mutex.EVENT.readAccess(new Runnable() {
                    public void run() {
                        String selectedTabName = null;
                        if (activeNode != null) {
                            Node.PropertySet [] oldP = activeNode.getPropertySets();
                            if (oldP == null) {
                                // illegal node behavior => log warning about it
                                ErrorManager.getDefault ().log (ErrorManager.WARNING,
                                    "Node "+activeNode+": getPropertySets() returns null!"); // NOI18N
                                oldP = new Node.PropertySet[] {};
                            }
                            if ((pageIndex >= 0) && (pageIndex < oldP.length)) {
                                selectedTabName = oldP[pageIndex].getDisplayName();
                            }
                        }
                        
                        Node old = activeNode;
                        setCurrentNode(null);
                        setCurrentNode(old);

                        if (selectedTabName != null) {
                            setCurrentPage(selectedTabName);
                        }
                    }
                });
            }
        }
    }
    
    
    /** JTabbedPane subclass which has a getHelpCtx method that will be
     * understood by HelpCtx.findHelp.
     */
    private static final class HelpAwareJTabbedPane extends JTabbedPane implements HelpCtx.Provider {

        public HelpAwareJTabbedPane () {
            // XXX(-ttran) experimental code, needs cleanup before release
            
            if (Boolean.getBoolean("netbeans.scrolling.tabs")) {
                boolean jdk14 = org.openide.modules.Dependency.JAVA_SPEC.compareTo(new org.openide.modules.SpecificationVersion("1.4")) >= 0;
                if (jdk14) {
                    try {
                        java.lang.reflect.Method method = getClass().getMethod("setTabLayoutPolicy", new Class[] {Integer.TYPE});
                        method.invoke(this, new Object[] {new Integer(1)});
                    } catch(NoSuchMethodException nme) {
                    } catch(SecurityException se) {
                    } catch(IllegalAccessException iae) {
                    } catch(IllegalArgumentException iare) {
                    } catch(java.lang.reflect.InvocationTargetException ite) {
                    }
                }
            }
        }

        /** Gets HelpCtx for currently selected tab, retrieved from 
         * <code>Node.PropertySet</code> <em>getValue("helpID")</em> method.
         * @see PropertySheetTab#getHelpID */
        public HelpCtx getHelpCtx () {
            Component comp = getSelectedComponent();
            if(comp instanceof PropertySheetTab) {
                String helpID = ((PropertySheetTab)comp).getHelpID();
                if(helpID != null) {
                    return new HelpCtx(helpID);
                }
            }
            
            return HelpCtx.findHelp(getParent());
        }
    }
}
