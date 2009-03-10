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

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.*;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.border.*;

import org.openide.*;
import org.openide.loaders.*;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.util.Task;
import org.openide.util.TaskListener;

/**
 * This class keeps track of the current toolbars and their names.
 * @author David Peroutka, Libor Kramolis
 */
public final class ToolbarPool extends JComponent implements Accessible {
    /** Default ToolbarPool */
    private static ToolbarPool defaultPool;

    /** objects responsible for creation of the window */
    private Folder instance;

    /** DataFolder from which the pool was created */
    private DataFolder folder;

    /** Maps name to <code>Toolbar</code>s */
    private Map toolbars;
    /** Maps name to <code>ToolbarPool.Configuration</code>s */
    private Map toolbarConfigs;

    /** Current name of selected configuration */
    private String name = ""; // NOI18N

    /** Center component */
    private Component center;

    /** Popup menu listener */
    private PopupListener listener;

    /** Accessible context */
    private AccessibleContext accessibleContext;

    /** Name of default toolbar configuration. */
    public static final String DEFAULT_CONFIGURATION = "Standard"; // NOI18N
    
    private TPTaskListener taskListener;
    
    /**
     * Returns default toolbar pool.
     * @return default system pool
     */
    public static synchronized ToolbarPool getDefault () {
        if (defaultPool == null) {
            FileObject fo = Repository.getDefault().getDefaultFileSystem().findResource("Toolbars"); // NOI18N
            if (fo == null) throw new IllegalStateException("No Toolbars/"); // NOI18N
            DataFolder folder = DataFolder.findFolder(fo);
            defaultPool = new ToolbarPool(folder);
            // we mustn't do this in constructor to prevent from
            // nevereding recursive calls to this method.
            defaultPool.instance.recreate();
        }
        return defaultPool;
    }

    static final long serialVersionUID =3420915387298484008L;


    /**
     * Creates a new <code>ToolbarPool</code>. Useful for modules that need they
     * own toolbars.
     *
     * @param df the data folder to read toolbar definitions and configurations from
     * @since 1.5
     */
    public ToolbarPool (DataFolder df) {
        accessibleContext = null;

        folder = df;

        setLayout (new BorderLayout ());
        listener = new PopupListener();
        toolbars = new TreeMap();
        toolbarConfigs = new TreeMap();

        instance = new Folder (df);

        getAccessibleContext().setAccessibleName(instance.instanceName());
        getAccessibleContext().setAccessibleDescription(instance.instanceName());
    }

    /** Allows to wait till the content of the pool is initialized. */
    public final void waitFinished () {
        instance.instanceFinished ();
    }

    /** Initialization of new values.
     * @param toolbars map (String, Toolbar) of toolbars
     * @param conf map (String, Configuration) of configs
     */
    void update (Map toolbars, Map conf) {
        this.toolbars = toolbars;
        this.toolbarConfigs = conf;

        if (!"".equals(name)) {
            setConfiguration (name);
        }
    }

    /** Updates the default configuration. */
    private synchronized void updateDefault () {
        JPanel tp = new JPanel (new FlowLayout (FlowLayout.LEFT));
        Toolbar[] list = getToolbars ();
        for (int i = 0; i < list.length; i++) {
            tp.add (list[i]);
        }
        name = ""; // NOI18N
        revalidate (tp);
    }

    /** Activates a configuration.
     * @param c configuration
     */
    private synchronized void activate (Configuration c) {
        Component comp = c.activate ();
        name = c.getName();
        revalidate (comp);
    }

    /** Sets DnDListener to all Toolbars. */
    public void setToolbarsListener (Toolbar.DnDListener l) {
        Iterator it = toolbars.values().iterator();
        while (it.hasNext())
            ((Toolbar)it.next()).setDnDListener (l);
    }

    /** Uses new component as a cental one. */
    private void revalidate (Component c) {
        if (c != center) {
            // exchange
            if (center != null) {
                remove (center);
                center.removeMouseListener (listener);
            }
            add (center = c, BorderLayout.CENTER);
            center.addMouseListener (listener);

            java.awt.Window w = javax.swing.SwingUtilities.windowForComponent (this);
            if (w != null) {
                w.validate();
            }
        }
    }

    /**
     * Returns a <code>Toolbar</code> to which this pool maps the given name.
     * @param name a <code>String</code> that is to be a toolbar's name
     * @return a <code>Toolbar</code> to which this pool maps the name
     */
    public final Toolbar findToolbar (String name) {
        return (Toolbar)toolbars.get (name);
    }

    /**
     * Getter for the name of current configuration.
     * @return the name of current configuration
     */
    public final String getConfiguration () {
        return name;
    }

    /**
     * Switch to toolbar configuration by specific config name
     * @param n toolbar configuration name
     */
    public final void setConfiguration (String n) {
        String old = name;
        
        // should be 'instance.waitFinished();' but some bug in isFinished ...
        if (!instance.isFinished()) {
            if (taskListener == null) {
                taskListener = new TPTaskListener();
                instance.addTaskListener(taskListener);
            }
            taskListener.setConfiguration(n);
            return;
        }
        if (taskListener != null) {
            instance.removeTaskListener(taskListener);
            taskListener = null;
        }

        Configuration config = null;
        if (n != null) {
            config = (Configuration)toolbarConfigs.get (n);
        }
        if (config != null) { // if configuration found
            activate (config);
        } else if (toolbarConfigs.isEmpty()) { // if no toolbar configuration
            updateDefault ();
        } else {
            // line below commented - bugfix, we need default configuration always when unknown config name is used:
            // if (center == null) { // bad config name (n) and no configuration activated yet
            config = (Configuration)toolbarConfigs.get (DEFAULT_CONFIGURATION);
            if (config == null) {
                config = (Configuration)toolbarConfigs.values().iterator().next();
            }
            activate (config);
        }
        
        firePropertyChange("configuration", old, name);
    }

    /**
     * @return the <code>DataFolder</code> from which the pool was created.
     */
    public final DataFolder getFolder() {
        return folder;
    }

    /**
     * Returns the toolbars contained in this pool.
     * @return the toolbars contained in this pool
     */
    public final synchronized Toolbar[] getToolbars() {
        Toolbar[] arr = new Toolbar[toolbars.size ()];
        return (Toolbar[])toolbars.values ().toArray (arr);
    }

    /**
     * @return the names of toolbar configurations contained in this pool
     */
    public final synchronized String[] getConfigurations () {
        String[] arr = new String[toolbarConfigs.size ()];
        return (String[])toolbarConfigs.keySet ().toArray (arr);
    }

        /** Read accessible context
     * @return - accessible context
     */
    public AccessibleContext getAccessibleContext () {
        if(accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.TOOL_BAR;
                }
            };
        }
        return accessibleContext;
    }

    /**
     * This class is used for delayed setting of configuration after instance
     * creation is finished. It may happen during IDE start that 
     * ToolbarPool.setConfiguration is called before instance is created.
     */
    private class TPTaskListener implements TaskListener {
        private String conf;
        
        TPTaskListener() {}
        
        public void taskFinished(Task task) {
            ToolbarPool.this.setConfiguration(conf);
            conf = null;
        }
        
        void setConfiguration(String conf) {
            // #23619: Don't reset already pending configuration to be set.
            if(this.conf == null) {
                this.conf = conf;
            }
        }
    }

    /**
     * This class can be used to produce a <code>ToolbarPool</code> instance
     * from the given <code>DataFolder</code>.
     */
    private class Folder extends FolderInstance {
        private WeakHashMap foldersCache = new WeakHashMap (15);

        public Folder (DataFolder f) {
            super (f);
        }

        /**
         * Full name of the data folder's primary file separated by dots.
         * @return the name
         */
        public String instanceName () {
            return instanceClass().getName();
        }

        /**
         * Returns the root class of all objects.
         * @return Object.class
         */
        public Class instanceClass () {
            return ToolbarPool.class;
        }

        /**
         * Accepts only cookies that can provide <code>Configuration</code>.
         * @param cookie the instance cookie to test
         * @return true if the cookie can provide <code>Configuration</code>
         */
        protected InstanceCookie acceptCookie (InstanceCookie cookie)
        throws java.io.IOException, ClassNotFoundException {
            Class cls = cookie.instanceClass();
            if (ToolbarPool.Configuration.class.isAssignableFrom (cls)) {
                return cookie;
            }
            if (Component.class.isAssignableFrom (cls)) {
                return cookie;
            }
            return null;
        }

        /**
         * Returns a <code>Toolbar.Folder</code> cookie for the specified
         * <code>DataFolder</code>.
         * @param df a <code>DataFolder</code> to create the cookie for
         * @return a <code>Toolbar.Folder</code> for the specified folder
         */
        protected InstanceCookie acceptFolder (DataFolder df) {
            InstanceCookie ic = (InstanceCookie)foldersCache.get (df);
            if (ic == null) {
                ic = new InstanceSupport.Instance(new Toolbar(df,true));
                foldersCache.put (df, ic);
            }
            return ic;
        }

        /**
         * Updates the <code>ToolbarPool</code> represented by this folder.
         *
         * @param cookies array of instance cookies for the folder
         * @return the updated <code>ToolbarPool</code> representee
         */
        protected Object createInstance (InstanceCookie[] cookies)
        throws java.io.IOException, ClassNotFoundException {
            final int length = cookies.length;

            final Map toolbars = new TreeMap ();
            final Map conf = new TreeMap ();

            for (int i = 0; i < length; i++) {
                try {
                    Object obj = cookies[i].instanceCreate();

                    if (obj instanceof Toolbar) {
                        Toolbar toolbar = (Toolbar)obj;
                        // should be done by ToolbarPanel in add method
                        toolbar.removeMouseListener (listener);
                        toolbar.addMouseListener (listener);
                        toolbars.put (toolbar.getName (), toolbar);
                        continue;
                    }

                    if (obj instanceof Configuration) {
                        Configuration config = (Configuration)obj;
                        String name = config.getName ();
                        if (name == null) {
                            name = cookies[i].instanceName ();
                        }
                        conf.put (name, config);
                        continue;
                    }
                    if (obj instanceof Component) {
                        Component comp = (Component)obj;
                        String name = comp.getName ();
                        if (name == null) {
                            name = cookies[i].instanceName ();
                        }
                        conf.put (name, new ComponentConfiguration (comp));
                        continue;
                    }
                } catch (java.io.IOException ex) {
                    ErrorManager.getDefault ().notify (ex);
                } catch (ClassNotFoundException ex) {
                    ErrorManager.getDefault ().notify (ex);
                }
            }
            update (toolbars, conf);

            return ToolbarPool.this;
        }

        /** Recreate the instance in AWT thread.
        */
        protected Task postCreationTask (Runnable run) {
            return new AWTTask (run);
        }

    } // end of Folder


    /**
     * Class to showing popup menu
     */
    private class PopupListener extends MouseUtils.PopupMouseAdapter {
	PopupListener() {}
        /**
         * Called when the sequence of mouse events should lead to actual showing popup menu
         */
        protected void showPopup (MouseEvent e) {
            Configuration conf = (Configuration)toolbarConfigs.get (name);
            if (conf != null) {
                JPopupMenu pop = conf.getContextMenu();
                pop.show (e.getComponent (), e.getX (), e.getY ());
            }
        }
    } // end of PopupListener


    /**
     * Abstract class for toolbar configuration
     */
    public static interface Configuration {
        /** Activates the configuration and returns right
        * component that can display the configuration.
        * @return representation component
        */
        public abstract Component activate ();

        /** Name of the configuration.
        * @return the name
        */
        public abstract String getName ();

        /** Popup menu that should be displayed when the users presses
        * right mouse button on the panel. This menu can contain
        * contains list of possible configurations, additional actions, etc.
        *
        * @return popup menu to be displayed
        */
        public abstract JPopupMenu getContextMenu ();
    }


    /** Implementation of configuration that reacts to one
    * component */
    private static final class ComponentConfiguration extends JPopupMenu
        implements Configuration, ActionListener {
        private Component comp;

	ComponentConfiguration() {}

        static final long serialVersionUID =-409474484612485719L;
        /** @param comp component that represents this configuration */
        public ComponentConfiguration (Component comp) {
            this.comp = comp;
        }

        /** Simply returns the representation component */
        public Component activate () {
            return comp;
        }

        /** @return name of the component
        */
        public String getName () {
            return comp.getName ();
        }

        /** Updates items in popup menu and returns itself.
        */
        public JPopupMenu getContextMenu () {
            removeAll ();

            // generate list of available toolbar panels
            Iterator it = Arrays.asList (ToolbarPool.getDefault ().getConfigurations ()).iterator ();
            ButtonGroup bg = new ButtonGroup ();
            String current = ToolbarPool.getDefault ().getConfiguration ();
            while (it.hasNext()) {
                final String name = (String)it.next ();
                JRadioButtonMenuItem mi = new JRadioButtonMenuItem (name, (name.compareTo (current) == 0));
                mi.addActionListener (this);
                bg.add (mi);
                this.add (mi);
            }

            return this;
        }

        /** Reacts to action in popup menu. Switches the configuration.
        */
        public void actionPerformed (ActionEvent evt) {
            ToolbarPool.getDefault().setConfiguration (evt.getActionCommand ());
        }

    }

} // end of ToolbarPool

