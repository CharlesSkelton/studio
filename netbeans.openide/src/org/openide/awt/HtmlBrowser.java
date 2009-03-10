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
import java.awt.datatransfer.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Iterator;
import javax.swing.*;
import javax.accessibility.*;

import org.openide.awt.Toolbar;
import org.openide.awt.ToolbarButton;
import org.openide.*;
import org.openide.loaders.*;
import org.openide.filesystems.*;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.Workspace;
import org.openide.windows.Mode;
import org.openide.text.*;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Lookup;
import org.openide.util.io.*;
import org.openide.util.actions.*;
import org.openide.windows.WindowManager;

/**
* Object that provides viewer for HTML pages.
* <p>If all you want to do is to show some URL in the IDE's normal way, this
* is overkill. Just use {@link HtmlBrowser.URLDisplayer#showURL} instead. Using <code>HtmlBrowser</code>
* is appropriate mainly if you want to embed a web browser in some other GUI component
* (if the user has selected an external browser, this will fall back to a simple Swing
* renderer). Similarly <code>Impl</code> (coming from a <code>Factory</code>) is the lower-level
* renderer itself (sans toolbar).
* <p>Summary: for client use, try <code>URLDisplayer.showURL</code>, or for more control
* or where embedding is needed, create an <code>HtmlBrowser</code>. For provider use,
* create a <code>Factory</code> and register an instance of it to lookup.
*/
public class HtmlBrowser extends JPanel {

    // static ....................................................................

    /** generated Serialized Version UID */
    static final long                 serialVersionUID = 2912844785502987960L;

    /** Preferred width of the browser */
    public static final int           DEFAULT_WIDTH = 400;
    /** Preferred height of the browser */
    public static final int           DEFAULT_HEIGHT = 600;

    /** current implementation of html browser */
    private static Factory            browserFactory;
    /** home page URL */
    private static String             homePage = null;

    /** Resource for all localized strings in local file system. */
    private static ResourceBundle     resourceBundle;
    /** browser title format support */
    private static MessageFormat      msgFormat;

    /** Icons for buttons. */
    private static Icon               iBack;
    private static Icon               iForward;
    private static Icon               iHome;
    private static Icon               iReload;
    private static Icon               iStop;
    private static Icon               iHistory;


    /** Sets the home page.
    * @param u the home page
    */
    public static void setHomePage (String u) {
        homePage = u;
    }

    /** Getter for the home page
    * @return the home page
    */
    public static String getHomePage () {
	if (homePage == null) {
	    return NbBundle.getMessage (HtmlBrowser.class, "PROP_HomePage");
	}
        return homePage;
    }

    /**
    * Sets a new implementation of browser visual component
    * for all HtmlBrowers.
    * @deprecated Use Lookup instead to register factories
    */
    public static void setFactory (Factory brFactory) {
        browserFactory = brFactory;
    }


    // variables .................................................................

    /** currently used implementation of browser */
    private Impl                        browserImpl;
    /** true = do not listen on changes of URL on cbLocation */
    private boolean                     everythinkIListenInCheckBoxIsUnimportant = false;
    /** toolbar visible property */
    private boolean                     toolbarVisible = false;
    /** status line visible property */
    private boolean                     statusLineVisible = false;
    /**  Listens on changes in HtmlBrowser.Impl and HtmlBrowser visual components.
    */
    private BrowserListener             browserListener;

    // visual components .........................................................

    private JButton                     bBack,
    bForward,
    bHome,
    bReload,
    bStop,
    bHistory;
    /** URL chooser */
    private JComboBox                   cbLocation;
    private JLabel                      cbLabel;
    private JLabel                      lStatusLine;
    private Component                   browserComponent;
    private JPanel                      head;


    // init ......................................................................

    /**
    * Creates new html browser with toolbar and status line.
    */
    public HtmlBrowser () {
        this (true, true);
    }

    /**
    * Creates new html browser.
    */
    public HtmlBrowser (boolean toolbar, boolean statusLine) {
        this (null, toolbar, statusLine);
    }
    
    /**
    * Creates new html browser.
     *
     * @param fact Factory that is used for creation. Default factory is used if null is passed
     * @param toolbar visibility of toolbar
     * @param statusLine visibility of statusLine
    */
    public HtmlBrowser (Factory fact, boolean toolbar, boolean statusLine) {
        init ();

        try {
            if (fact == null) {
                Impl[] arr = new Impl[1];
                browserComponent = findComponent (arr);
                browserImpl = arr[0];
            }
            else {
                try {
                    browserImpl = fact.createHtmlBrowserImpl ();
                    browserComponent = browserImpl.getComponent();
                }
                catch (UnsupportedOperationException ex) {
                    ErrorManager.getDefault().notify(ex);
                    browserImpl = new SwingBrowserImpl ();
                    browserComponent = browserImpl.getComponent ();
                }
            }
        } catch (RuntimeException e) {
	    ErrorManager em = ErrorManager.getDefault();
            // browser was uninstlled ?
	    em.annotate(e, resourceBundle.getString ("EXC_Module"));
	    em.notify(e);
        }

        setLayout (new BorderLayout (0, 2));

        add ((browserComponent != null)? new JScrollPane (browserComponent): new JScrollPane (), "Center"); // NOI18N

        browserListener = new BrowserListener ();
        if (toolbar) initToolbar ();
        if (statusLine) initStatusLine ();

        browserImpl.addPropertyChangeListener (browserListener);

        getAccessibleContext().setAccessibleName(resourceBundle.getString("ACS_HtmlBrowser"));
        //getAccessibleContext().setAccessibleDescription(resourceBundle.getString("ACSD_HtmlBrowser"));
    }
    
    /** Find Impl of HtmlBrowser. Searches for registered factories in lookup folder.
     *  Tries to create Impl and check if it provides displayable component. 
     *  Both Component and used Impl are returned to avoid resource consuming of new 
     *  Component/Impl.
     *  </P>
     *  <P>
     *  If no browser is found then it tries to use registered factory (now deprecated method
     *  of setting browser) or it uses browser based on swing editor in the worst case.
     *
     *  @param handle used browser implementation is in first element when method 
     *                is finished
     *  @return Component for content displaying 
     */
    private static Component findComponent (Impl[] handle) {
        Lookup.Result r = Lookup.getDefault ().lookup (new Lookup.Template (Factory.class));
        Iterator it = r.allInstances ().iterator ();
        while (it.hasNext ()) {
            Factory f = (Factory)it.next ();
            try {
                Impl impl = f.createHtmlBrowserImpl ();
                Component c = (impl != null)? impl.getComponent (): null;
                if (c != null) {
                    handle[0] = impl;
                    return c;
                }
            }
            catch (UnsupportedOperationException ex) {
                // do nothing: thrown if browser doesn't work on given platform
            }
        }
        
        // 1st fallback to our deprecated method
        Factory f = browserFactory;
        if (f != null) {
            try {
                handle[0] = f.createHtmlBrowserImpl ();
                return handle[0].getComponent ();
            }
            catch (UnsupportedOperationException ex) {
                // do nothing: thrown if browser doesn't work on given platform
            }
        }
        
        // last fallback is to swing
        handle[0] = new SwingBrowserImpl ();
        return handle[0].getComponent ();
    }

    /**
    * Default initializations.
    */
    private static void init () {
        if (iBack != null) return;

        resourceBundle = NbBundle.getBundle(HtmlBrowser.class);
        iBack = new ImageIcon (HtmlBrowser.class.getResource (
                                   "/org/openide/resources/html/back.gif" // NOI18N
                               ));
        iForward = new ImageIcon (HtmlBrowser.class.getResource (
                                      "/org/openide/resources/html/forward.gif" // NOI18N
                                  ));
        iHome = new ImageIcon (HtmlBrowser.class.getResource (
                                   "/org/openide/resources/html/home.gif" // NOI18N
                               ));
        iReload = new ImageIcon (HtmlBrowser.class.getResource (
                                     "/org/openide/resources/html/refresh.gif" // NOI18N
                                 ));
        iStop = new ImageIcon (HtmlBrowser.class.getResource (
                                   "/org/openide/resources/html/stop.gif" // NOI18N
                               ));
        iHistory = new ImageIcon (HtmlBrowser.class.getResource (
                                      "/org/openide/resources/html/history.gif" // NOI18N
                                  ));
        msgFormat = new MessageFormat (resourceBundle.getString (
                                           "CTL_Html_viewer_title" // NOI18N
                                       ));
    }

    /**
    * Default initialization of toolbar.
    */
    private void initToolbar () {
        toolbarVisible = true;

        // create visual compoments .............................
        head = new JPanel ();
        head.setLayout (new BorderLayout (11, 0));

        JPanel p = new JPanel (new GridBagLayout());
        p.add (bBack = new ToolbarButton (iBack));
        bBack.setToolTipText (resourceBundle.getString ("CTL_Back"));
        bBack.setMnemonic(resourceBundle.getString("CTL_Back_Mnemonic").charAt(0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);
        p.add (bForward = new ToolbarButton (iForward), gbc);
        bForward.setToolTipText (resourceBundle.getString ("CTL_Forward"));
        bForward.setMnemonic(resourceBundle.getString("CTL_Forward_Mnemonic").charAt(0));
        p.add (bStop = new ToolbarButton (iStop));
        bStop.setToolTipText (resourceBundle.getString ("CTL_Stop"));
        bStop.setMnemonic(resourceBundle.getString("CTL_Stop_Mnemonic").charAt(0));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);
        p.add (bReload = new ToolbarButton (iReload), gbc);
        bReload.setToolTipText (resourceBundle.getString ("CTL_Reload"));
        bReload.setMnemonic(resourceBundle.getString("CTL_Reload_Mnemonic").charAt(0));
        p.add (bHome = new ToolbarButton (iHome));
        bHome.setToolTipText (resourceBundle.getString ("CTL_Home"));
        bHome.setMnemonic(resourceBundle.getString("CTL_Home_Mnemonic").charAt(0));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);
        p.add (bHistory = new ToolbarButton (iHistory), gbc);
        bHistory.setToolTipText (resourceBundle.getString ("CTL_History"));
        bHistory.setMnemonic(resourceBundle.getString("CTL_History_Mnemonic").charAt(0));
        if (browserImpl != null) {
            bBack.setEnabled (browserImpl.isBackward ());
            bForward.setEnabled (browserImpl.isForward ());
            bHistory.setEnabled(browserImpl.isHistory());
        }
        Toolbar.Separator ts = new Toolbar.Separator ();
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);
        p.add (ts, gbc);
        ts.updateUI ();
        p.add ( cbLabel = new JLabel (resourceBundle.getString ("CTL_Location")));
        cbLabel.setDisplayedMnemonic(resourceBundle.getString ("CTL_Location_Mnemonic").charAt(0));
        head.add ("West", p); // NOI18N

        head.add ("Center", cbLocation = new JComboBox ()); // NOI18N
        cbLocation.setEditable (true);
        cbLabel.setLabelFor(cbLocation);
        add (head, "North"); // NOI18N

        // add listeners ..................... .............................
        cbLocation.addActionListener (browserListener);
        bHistory.addActionListener (browserListener);
        bBack.addActionListener (browserListener);
        bForward.addActionListener (browserListener);
        bReload.addActionListener (browserListener);
        bHome.addActionListener (browserListener);
        bStop.addActionListener (browserListener);

        bHistory.getAccessibleContext().setAccessibleName(bHistory.getToolTipText());
        bBack.getAccessibleContext().setAccessibleName(bBack.getToolTipText());
        bForward.getAccessibleContext().setAccessibleName(bForward.getToolTipText());
        bReload.getAccessibleContext().setAccessibleName(bReload.getToolTipText());
        bHome.getAccessibleContext().setAccessibleName(bHome.getToolTipText());
        bStop.getAccessibleContext().setAccessibleName(bStop.getToolTipText());
        cbLocation.getAccessibleContext().setAccessibleDescription(resourceBundle.getString("ACSD_HtmlBrowser_Location"));
    }

    /**
    * Default initialization of toolbar.
    */
    private void destroyToolbar () {
        remove (head);
        head = null;
        toolbarVisible = false;
    }

    /**
    * Default initialization of status line.
    */
    private void initStatusLine () {
        statusLineVisible = true;
        add (
            lStatusLine = new JLabel (resourceBundle.getString ("CTL_Loading")),
            "South" // NOI18N
        );
        lStatusLine.setLabelFor(this);
    }

    /**
    * Destroyes status line.
    */
    private  void destroyStatusLine () {
        remove (lStatusLine);
        lStatusLine = null;
        statusLineVisible = false;
    }


    // public methods ............................................................

    /**
    * Sets new URL.
    *
    * @param str URL to show in this browser.
    */
    public void setURL (String str) {
        URL URL;
        try {
            URL = new java.net.URL (str);
        } catch (java.net.MalformedURLException ee) {
            try {
                URL = new java.net.URL ("http://" + str); // NOI18N
            } catch (java.net.MalformedURLException e) {
                ErrorManager.getDefault ().notify (e);
                return;
            }
        }
        setURL (URL);
    }

    /**
    * Sets new URL.
    *
    * @param url URL to show in this browser.
    */
    public void setURL (final URL url) {
        if (url == null)
            return;
        
        SwingUtilities.invokeLater (
            new Runnable () {
                public void run () {
                    boolean sameHosts;
                    if ("nbfs".equals(url.getProtocol())) { // NOI18N
                        sameHosts = true;
                    }
                    else {
                        sameHosts = (url.getHost () != null)
                                        && (browserImpl.getURL () != null)
                                        && (url.getHost ().equals (browserImpl.getURL ().getHost ()));
                    }
                    if (url.equals (browserImpl.getURL ()) && sameHosts) { // see bug 9470
                        browserImpl.reloadDocument ();
                    } else {
                        browserImpl.setURL (url);
                    }
                }
            }
        );
    }

    /**
    * Gets current document url.
    */
    public final URL getDocumentURL () {
        return browserImpl.getURL ();
    }

    /**
    * Enables/disables Home button.
    */
    public final void setEnableHome (boolean b) {
        bHome.setEnabled (b);
        bHome.setVisible (b);
    }

    /**
    * Enables/disables location.
    */
    public final void setEnableLocation (boolean b) {
        cbLocation.setEditable (b);
        cbLocation.setVisible (b);
        cbLabel.setVisible (b);
    }

    /**
    * Gets status line state.
    */
    public boolean isStatusLineVisible () {
        return statusLineVisible;
    }

    /**
    * Shows/hides status line.
    */
    public void setStatusLineVisible (boolean v) {
        if (v == statusLineVisible) return;
        if (v) initStatusLine ();
        else destroyStatusLine ();
    }

    /**
    * Gets status toolbar.
    */
    public boolean isToolbarVisible () {
        return toolbarVisible;
    }

    /**
    * Shows/hides toolbar.
    */
    public void setToolbarVisible (boolean v) {
        if (v == toolbarVisible) return;
        if (v) initToolbar ();
        else destroyToolbar ();
    }

    // helper methods .......................................................................

    /**
    * Returns preferred size.
    */
    public java.awt.Dimension getPreferredSize () {
        java.awt.Dimension superPref = super.getPreferredSize ();
        return new java.awt.Dimension (
                   Math.max (DEFAULT_WIDTH, superPref.width),
                   Math.max (DEFAULT_HEIGHT, superPref.height)
               );
    }


    // innerclasses ..............................................................

    /**
    * Listens on changes in HtmlBrowser.Impl and HtmlBrowser visual components.
    */
    private class BrowserListener implements ActionListener, PropertyChangeListener {
        BrowserListener() {}

        /**
        * Listens on changes in HtmlBrowser.Impl.
        */
        public void propertyChange (PropertyChangeEvent evt) {
            String property = evt.getPropertyName ();
            if (property == null) return;

            if (property.equals (Impl.PROP_URL) ||
                property.equals (Impl.PROP_TITLE))
                HtmlBrowser.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            if (property.equals (Impl.PROP_URL)) {
                if (toolbarVisible) {
                    everythinkIListenInCheckBoxIsUnimportant = true;
                    String url = browserImpl.getURL ().toString ();
                    cbLocation.setSelectedItem (url);
                    everythinkIListenInCheckBoxIsUnimportant = false;
                }
            } else if (property.equals (Impl.PROP_STATUS_MESSAGE)) {
                String s = browserImpl.getStatusMessage ();
                if ((s == null) || (s.length () < 1))
                    s = resourceBundle.getString ("CTL_Document_done");
                if (lStatusLine != null) lStatusLine.setText (s);
            } 
            else if (property.equals (Impl.PROP_FORWARD) && bForward != null) {
                bForward.setEnabled (browserImpl.isForward ());
            } else if (property.equals (Impl.PROP_BACKWARD) && bBack != null) {
                bBack.setEnabled (browserImpl.isBackward ());
            } else if (property.equals (Impl.PROP_HISTORY) && bHistory != null) {
                bHistory.setEnabled (browserImpl.isHistory ());
            }
        }

        /**
        * Listens on changes in HtmlBrowser visual components.
        */
        public void actionPerformed (ActionEvent e) {
            if (e.getSource () == cbLocation) {
                // URL manually changed
                if (everythinkIListenInCheckBoxIsUnimportant) return;
                JComboBox cb = (JComboBox)e.getSource ();
                Object o = cb.getSelectedItem ();
                if (o == null)  // empty combo box
                    return;
                setURL ((String)o);
                ListModel lm = cb.getModel ();
                int i, k = lm.getSize ();
                for (i = 0; i < k; i++) if (o.equals (lm.getElementAt (i))) break;
                if (i != k) return;
                if (k == 20) cb.removeItem (lm.getElementAt (k - 1));
                cb.insertItemAt (o, 0);
            } else

                if (e.getSource () == bHistory) {
                    browserImpl.showHistory ();
                } else

                    if (e.getSource () == bBack) {
                        browserImpl.backward ();
                    } else

                        if (e.getSource () == bForward) {
                            browserImpl.forward ();
                        } else

                            if (e.getSource () == bReload) {
                                browserImpl.reloadDocument ();
                            } else

                                if (e.getSource () == bHome) {
                                    setURL (homePage);
                                } else

                                    if (e.getSource () == bStop) {
                                        browserImpl.stopLoading ();
                                    }
        }
    }

    /** A dockable component showing a browser.
     * @deprecated Better to use either {@link HtmlBrowser} directly
     *             if you know you want an embedded browser pane, or
     *             {@link HtmlBrowser.URLDisplayer} if you simply wish
     *             to display a page but want to use the configured web
     *             browser, possibly an external browser if available.
     */
    public static class BrowserComponent extends CloneableTopComponent {
        /** generated Serialized Version UID */
        static final long                   serialVersionUID = 2912844785502987960L;

        // variables .........................................................................................
        
        /** programmatic name of special mode for this top component */
        public static final String MODE_NAME = "webbrowser"; // NOI18N

        /** Delegating component */
        private HtmlBrowser browserComponent;
        

        // initialization ....................................................................................

        /**
        * Creates new html browser with toolbar and status line.
        */
        public BrowserComponent () {
            this (true, true);
        }

        /**
        * Creates new html browser with toolbar and status line.
        */
        public BrowserComponent (boolean toolbar, boolean statusLine) {
            this (null, toolbar, statusLine);
        }

        /**
        * Creates new html browser.
        */
        public BrowserComponent (Factory fact, boolean toolbar, boolean statusLine) {
            setName (""); // NOI18N
            setLayout (new BorderLayout ());
            add (browserComponent = new HtmlBrowser (fact, toolbar, statusLine), "Center"); // NOI18N

            // listen on changes of title and set name of top component
            browserComponent.browserImpl.addPropertyChangeListener (
                new PropertyChangeListener () {
                    public void propertyChange (PropertyChangeEvent e) {
                        if (!e.getPropertyName ().equals (Impl.PROP_TITLE)) return;
                        String title = browserComponent.browserImpl.getTitle ();
                        if ((title == null) || (title.length () < 1)) return;
                        BrowserComponent.this.setName (title);
                    }
                });

            // Ensure closed browsers are not stored:
            putClientProperty("PersistenceType", "OnlyOpened"); // NOI18N
            if (browserComponent.browserComponent != null) {
                putClientProperty("InternalBrowser", Boolean.TRUE); // NOI18N
	    }
            setToolTipText(NbBundle.getBundle(HtmlBrowser.class).getString("HINT_WebBrowser"));

        }
        
        /** always open this top component in our special mode, if
        * no mode for this component is specified yet */
        public void open (Workspace workspace) {
            // do not open this component if this is dummy browser
            if (browserComponent.browserComponent == null)
                return;
            
            Workspace realWorkspace = (workspace == null)
                                      ? WindowManager.getDefault().getCurrentWorkspace()
                                      : workspace;
            // dock into our mode if not docked yet
            Mode mode = realWorkspace.findMode(MODE_NAME);
            if (mode == null) {
                mode = realWorkspace.createMode(
                    MODE_NAME,
                    NbBundle.getBundle(HtmlBrowser.class).getString("CTL_WebBrowser"),
                    HtmlBrowser.class.getResource("/org/openide/resources/html/htmlView.gif")
                );
            }
            Mode tcMode = realWorkspace.findMode(this);
            if (tcMode == null)
                mode.dockInto(this);
            // behave like superclass
            super.open(workspace);
        }
        
        /** Serializes browser component -> writes Replacer object which
        * holds browser content and look. */
        protected Object writeReplace ()
        throws java.io.ObjectStreamException {
            return new BrowserReplacer (this);
        }
         
        /* Deserialize this top component. Now it is here for backward compatibility
        * @param in the stream to deserialize from
        */
        public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException {
            super.readExternal (in);
            setStatusLineVisible (in.readBoolean ());
            setToolbarVisible (in.readBoolean ());
            browserComponent.setURL ((URL) in.readObject ());
        }

        // TopComponent support ...................................................................

        protected CloneableTopComponent createClonedObject () {
            BrowserComponent bc = new BrowserComponent ();  // PENDING: this should pass all three params to create the same browser
            bc.setURL (getDocumentURL ());
            return bc;
        }

        public HelpCtx getHelpCtx () {
            return new HelpCtx (BrowserComponent.class);
        }

        protected void componentActivated () {
            browserComponent.browserImpl.getComponent ().requestFocus ();
            super.componentActivated ();
        }

        public java.awt.Image getIcon () {
            return new ImageIcon (HtmlBrowser.class.getResource ("/org/openide/resources/html/htmlView.gif")).getImage ();   // NOI18N
        }
        

        // public methods ....................................................................................

        /**
        * Sets new URL.
        *
        * @param str URL to show in this browser.
        */
        public void setURL (String str) {
            browserComponent.setURL (str);
        }

        /**
        * Sets new URL.
        *
        * @param url URL to show in this browser.
        */
        public void setURL (final URL url) {
            browserComponent.setURL (url);
        }

        /**
        * Gets current document url.
        */
        public final URL getDocumentURL () {
            return browserComponent.getDocumentURL ();
        }

        /**
        * Enables/disables Home button.
        */
        public final void setEnableHome (boolean b) {
            browserComponent.setEnableHome (b);
        }

        /**
        * Enables/disables location.
        */
        public final void setEnableLocation (boolean b) {
            browserComponent.setEnableLocation (b);
        }

        /**
        * Gets status line state.
        */
        public boolean isStatusLineVisible () {
            return browserComponent.isStatusLineVisible ();
        }

        /**
        * Shows/hides status line.
        */
        public void setStatusLineVisible (boolean v) {
            browserComponent.setStatusLineVisible (v);
        }

        /**
        * Gets status toolbar.
        */
        public boolean isToolbarVisible () {
            return browserComponent.isToolbarVisible ();
        }

        /**
        * Shows/hides toolbar.
        */
        public void setToolbarVisible (boolean v) {
            browserComponent.setToolbarVisible (v);
        }
    }

    private static class BrowserReplacer implements java.io.Externalizable {
        
        /** serial version UID */
        static final long serialVersionUID = 5915713034827048413L;

        
        /** browser window to be serialized */
        private transient BrowserComponent bComp = null;
        transient boolean statLine;
        transient boolean toolbar;
        transient URL url;
        
        public BrowserReplacer () {
            ;
        }
        
        public BrowserReplacer (BrowserComponent comp) {
            bComp = comp;
        }
        

        /* Serialize this top component.
        * @param out the stream to serialize to
        */
        public void writeExternal (ObjectOutput out)
        throws IOException {
            out.writeBoolean (bComp.isStatusLineVisible ());
            out.writeBoolean (bComp.isToolbarVisible ());
            out.writeObject (bComp.getDocumentURL ());
        }
         
        /* Deserialize this top component.
          * @param in the stream to deserialize from
          */
        public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException {
            statLine = in.readBoolean ();
            toolbar = in.readBoolean ();
            url = (URL) in.readObject ();
            
        }


        private Object readResolve ()
        throws java.io.ObjectStreamException {
            // return singleton instance
            try {
                if (url.getProtocol().equals("http")    // NOI18N
                &&  InetAddress.getByName (url.getHost ()).equals (InetAddress.getLocalHost ())) {
                    url.openStream ();
                }
            }
            // ignore exceptions thrown during our test of accessibility and restore browser
            catch (java.net.UnknownHostException exc) {}
            catch (java.lang.SecurityException exc) {}
            catch (java.lang.NullPointerException exc) {}
            
            catch (java.io.IOException exc) {
                // do not restore JSP/servlet pages - covers FileNotFoundException, ConnectException
                return null;
            }
            catch (java.lang.Exception exc) {
                // unknown exception - write log message & restore browser
                ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, exc);
            }
            
            bComp = new BrowserComponent (statLine, toolbar);
            bComp.setURL (url);
            return bComp;
        }

    } // end of BrowserReplacer inner class


    /**
    * This interface represents an implementation of html browser used in HtmlBrowser. Each BrowserImpl
    * implementation corresponds with some BrowserFactory implementation.
    */
    public static abstract class Impl {

        /** generated Serialized Version UID */
        static final long            serialVersionUID = 2912844785502962114L;

        /** The name of property representing status of html browser. */
        public static final String PROP_STATUS_MESSAGE = "statusMessage"; // NOI18N
        /** The name of property representing current URL. */
        public static final String PROP_URL = "url"; // NOI18N
        /** Title property */
        public static final String PROP_TITLE = "title"; // NOI18N
        /** forward property */
        public static final String PROP_FORWARD = "forward"; // NOI18N
        /** backward property name */
        public static final String PROP_BACKWARD = "backward"; // NOI18N
        /** history property name */
        public static final String PROP_HISTORY = "history"; // NOI18N

        /**
        * Returns visual component of html browser.
        *
        * @return visual component of html browser.
        */
        public abstract java.awt.Component getComponent ();

        /**
        * Reloads current html page.
        */
        public abstract void reloadDocument ();

        /**
        * Stops loading of current html page.
        */
        public abstract void stopLoading ();

        /**
        * Sets current URL.
        *
        * @param url URL to show in the browser.
        */
        public abstract void setURL (URL url);

        /**
        * Returns current URL.
        *
        * @return current URL.
        */
        public abstract URL getURL ();

        /**
        * Returns status message representing status of html browser.
        *
        * @return status message.
        */
        public abstract String getStatusMessage ();

        /** Returns title of the displayed page.
        * @return title 
        */
        public abstract String getTitle ();


        /** Is forward button enabled?
        * @return true if it is
        */
        public abstract boolean isForward ();

        /** Moves the browser forward. Failure is ignored.
        */
        public abstract void forward ();

        /** Is backward button enabled?
        * @return true if it is
        */
        public abstract boolean isBackward ();

        /** Moves the browser forward. Failure is ignored.
        */
        public abstract void backward ();

        /** Is history button enabled?
        * @return true if it is
        */
        public abstract boolean isHistory ();

        /** Invoked when the history button is pressed.
        */
        public abstract void showHistory ();

        /**
        * Adds PropertyChangeListener to this browser.
        *
        * @param l Listener to add.
        */
        public abstract void addPropertyChangeListener (PropertyChangeListener l);

        /**
        * Removes PropertyChangeListener from this browser.
        *
        * @param l Listener to remove.
        */
        public abstract void removePropertyChangeListener (PropertyChangeListener l);
    }

    /**
    * Implementation of BrowerFactory creates new instances of some Browser implementation.
    *
    * @see HtmlBrowser.Impl
    */
    public interface Factory {
        /**
        * Returns a new instance of BrowserImpl implementation.
        */
        public Impl createHtmlBrowserImpl ();
    }
    
    /** A manager class which can display URLs in the proper way.
     * Might open a selected HTML browser, knows about embedded vs. external
     * browsers, etc.
     * @since 3.14
     */
    public static abstract class URLDisplayer {
        
        /** Get the default URL displayer.
         * @return the default instance from lookup
         */
        public static URLDisplayer getDefault() {
            return (URLDisplayer)Lookup.getDefault().lookup(URLDisplayer.class);
        }
        
        /** Subclass constructor. */
        protected URLDisplayer() {}

        /** Display a URL to the user somehow.
         * @param u the URL to show
         */
        public abstract void showURL(URL u);
        
    }
    
////// Accessibility //////
    
    public void requestFocus() {
        if (browserComponent != null) {
            boolean ownerFound = false;
            if (browserComponent instanceof JComponent) {
                ownerFound = ((JComponent)browserComponent).requestDefaultFocus();
            }
            if (!ownerFound) {
                browserComponent.requestFocus();
            }
        } else {
            super.requestFocus();
        }
    }
    
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleHtmlBrowser();
        }
        return accessibleContext;
    }

    private class AccessibleHtmlBrowser extends AccessibleJPanel {
        AccessibleHtmlBrowser() {}
        public void setAccessibleName(String name) {
            super.setAccessibleName(name);
            if (browserComponent instanceof Accessible) {
                browserComponent.getAccessibleContext().setAccessibleName(name);
            }
        }
        public void setAccessibleDescription(String desc) {
            super.setAccessibleDescription(desc);
            if (browserComponent instanceof Accessible) {
                browserComponent.getAccessibleContext().setAccessibleDescription(desc);
            }
        }
    }
}
