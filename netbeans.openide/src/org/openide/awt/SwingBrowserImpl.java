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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.*;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import java.beans.PropertyChangeEvent;
import java.util.Vector;

import org.openide.ErrorManager;


/**
* Implementation of BrowserImpl in Swing.
*/
final class SwingBrowserImpl extends HtmlBrowser.Impl {
    
    /** state of history management */
    private static final int NO_NAVIGATION   = 1;
    private static final int NAVIGATION_BACK = 2;
    private static final int NAVIGATION_FWD  = 3;

    /** Current URL. */
    private URL                     url;
    /** URL loaded by JEditorPane */
    private URL                     loadingURL;
    private PropertyChangeSupport   pcs;
    private String                  statusMessage = ""; // NOI18N
    private SwingBrowser            swingBrowser;
    
    /** list of accessed URLs for back/fwd navigation */
    private Vector                  historyList;
    /** current position in history */
    private int                     historyIndex;
    /** navigation indication */
    private int                     historyNavigating = NO_NAVIGATION;


    SwingBrowserImpl () {
        pcs = new PropertyChangeSupport (this);
        swingBrowser = new SwingBrowser ();
        historyList = new Vector (5, 3);
        historyIndex = -1;
        swingBrowser.addPropertyChangeListener (
            "page",     // NOI18N
            new PropertyChangeListener () {
                public void propertyChange (PropertyChangeEvent evt) {
                    if (evt.getNewValue () instanceof URL) {
                        URL old = SwingBrowserImpl.this.url;
                        SwingBrowserImpl.this.url = (URL)evt.getNewValue ();
                        SwingBrowserImpl.this.pcs.firePropertyChange (PROP_URL, old, url);
                        if (((URL)evt.getNewValue ()).equals (loadingURL))
                            loadingURL = null;
                        // update history
                        if (historyNavigating == NAVIGATION_BACK) {
                            int idx = historyList.lastIndexOf (evt.getNewValue (), historyIndex-1);
                            if (idx != -1) 
                                historyIndex = idx;
                        }
                        else if (historyNavigating == NAVIGATION_FWD) {
                            int idx = historyList.indexOf (evt.getNewValue (), historyIndex+1);
                            if (idx != -1) 
                                historyIndex = idx;
                        }
                        else {
                            while (historyList.size () > historyIndex+1)
                                historyList.remove (historyList.size ()-1);
                            
                            historyList.add (evt.getNewValue ());
                            historyIndex = historyList.size ()-1;
                        }
                        historyNavigating = NO_NAVIGATION;
                        pcs.firePropertyChange (PROP_BACKWARD, null, null);
                        pcs.firePropertyChange (PROP_FORWARD, null, null);
                    }
                }
            }
        );
        
    }

    /**
    * Returns visual component of html browser.
    *
    * @return visual component of html browser.
    */
    public java.awt.Component getComponent () {
        return swingBrowser;
    }

    /**
    * Reloads current html page.
    */
    public synchronized void reloadDocument () {
        try {
            if (url == null || loadingURL != null)
                return;
            
            Document doc = swingBrowser.getDocument ();
            if (doc != null) {
                doc.putProperty(Document.StreamDescriptionProperty, null);
            }
            loadingURL = url;
            if (doc instanceof AbstractDocument) {
                String protocol = url.getProtocol();
                if ("ftp".equalsIgnoreCase(protocol) // NOI18N
                    || "http".equalsIgnoreCase(protocol) // NOI18N
                    ) {
                    ((AbstractDocument)doc).setAsynchronousLoadPriority(
                        Thread.NORM_PRIORITY);
                }
                else {
                    ((AbstractDocument)doc).setAsynchronousLoadPriority(-1);
                }
            }
            swingBrowser.setPage (url);
        } catch (Exception e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            pcs.firePropertyChange (PROP_STATUS_MESSAGE, null, statusMessage = "" + e); // NOI18N
        }
    }

    /**
    * Stops loading of current html page.
    */
    public void stopLoading () {
    }

    /**
    * Sets current URL.
    *
    * @param url URL to show in the browser.
    */
    public synchronized void setURL (URL url) {
        try {
            if (url == null)
                return;
            
            loadingURL = url;
            if (this.url != null && this.url.sameFile(url)) {
                boolean sameHosts;
                if ("nbfs".equals(url.getProtocol())) { // NOI18N
                    sameHosts = true;
                }
                else {
                    sameHosts = (url.getHost () != null)
                                    && (this.url != null)
                                    && (url.getHost ().equals (this.url.getHost ()));
                }
                Document doc = swingBrowser.getDocument ();
                if (doc != null) {
                    doc.putProperty(Document.StreamDescriptionProperty, null);
                }
            }

            swingBrowser.setPage (url);
        } catch (Exception e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            pcs.firePropertyChange (PROP_STATUS_MESSAGE, null, statusMessage = "" + e); // NOI18N
        }
    }

    /**
    * Returns current URL.
    *
    * @return current URL.
    */
    public URL getURL () {
        return url;
    }

    /**
    * Returns status message representing status of html browser.
    *
    * @return status message.
    */
    public String getStatusMessage () {
        return statusMessage;
    }

    /** Returns title of the displayed page.
    * @return title 
    */
    public String getTitle () {
        return ""; // NOI18N
    }


    /** Is forward button enabled?
    * @return true if it is
    */
    public boolean isForward () {
        return historyIndex >=0 && historyIndex < historyList.size ()-1 && historyNavigating == NO_NAVIGATION;
    }

    /** Moves the browser forward. Failure is ignored.
    */
    public void forward () {
        if (isForward ()) {
            historyNavigating = NAVIGATION_FWD;
            setURL ((URL)historyList.elementAt (historyIndex+1));
        }
    }

    /** Is backward button enabled?
    * @return true if it is
    */
    public boolean isBackward () {
        return historyIndex > 0 && historyIndex < historyList.size () && historyNavigating == NO_NAVIGATION;
    }

    /** Moves the browser forward. Failure is ignored.
    */
    public void backward () {
        if (isBackward ()) {
            historyNavigating = NAVIGATION_BACK;
            setURL ((URL)historyList.elementAt (historyIndex-1));
        }
    }

    /** Is history button enabled?
    * @return true if it is
    */
    public boolean isHistory () {
        return false;
    }

    /** Invoked when the history button is pressed.
    */
    public void showHistory () {
    }

    /**
    * Adds PropertyChangeListener to this browser.
    *
    * @param l Listener to add.
    */
    public void addPropertyChangeListener (PropertyChangeListener l) {
        pcs.addPropertyChangeListener (l);
    }

    /**
    * Removes PropertyChangeListener from this browser.
    *
    * @param l Listener to remove.
    */
    public void removePropertyChangeListener (PropertyChangeListener l) {
        pcs.removePropertyChangeListener (l);
    }


    // innerclasses ..............................................................

    private class SwingBrowser extends JEditorPane {

        private SwingBrowser () {
            setEditable (false);
            setEditorKitForContentType ("text/html", new HTMLEditorKit ()); // NOI18N
            addHyperlinkListener (new HyperlinkListener () {
                                      public void hyperlinkUpdate (HyperlinkEvent e) {
                                          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                              if (e instanceof HTMLFrameHyperlinkEvent) {
                                                  HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent) e;
                                                  HTMLDocument doc = (HTMLDocument) getDocument ();
                                                  URL old = getURL ();
                                                  doc.processHTMLFrameHyperlinkEvent (evt);
                                                  pcs.firePropertyChange (PROP_URL, old, e.getURL ());
                                              } else {
                                                  try {
                                                      SwingBrowserImpl.this.setURL (e.getURL ());
                                                  } catch (Exception ex) {
                                                      ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                                                  }
                                              }
                                          }
                                      }
                                  });
        }
        
        /**
         * Fetches a stream for the given URL, which is about to
         * be loaded by the <code>setPage</code> method.  
         * This method is expected to have the the side effect of
         * establishing the content type, and therefore setting the
         * appropriate <code>EditorKit</code> to use for loading the stream.
         * <p>
         * If debugger is not running returns super implementation.
         * <p>
         * If debugger runs it will set content type to text/html.
         * Forwarding is not supported is that case.
         * <p>Control using sysprop org.openide.awt.SwingBrowserImpl.do-not-block-awt=true.
         *
         * @param page  the URL of the page
         */
        protected InputStream getStream (URL page) throws IOException {
            // XXX debugger ought to set this temporarily
            if (Boolean.getBoolean("org.openide.awt.SwingBrowserImpl.do-not-block-awt")) {
                // try to set contentType quickly and return (don't block AWT Thread)
                setContentType ("text/html"); // NOI18N
                return new FilteredInputStream (page.openConnection());
            } else {
                return super.getStream (page);
            }
        }
        
    }
    
    /**
     * FilterInputStream that delays opening of stream.
     * The purpose is not to initialize the stream when it is created in getStream()
     * but to do it later when the content is asynchronously loaded in separate thread.
     */
    private static class FilteredInputStream extends FilterInputStream {
        
        private URLConnection conn;
        
        FilteredInputStream (URLConnection conn) {
            super ((FilterInputStream)null);
            this.conn = conn;
        }
        
        private void openStream () throws IOException {
            if (in == null) {
                synchronized (this) {
                    if (in == null)
                        in = conn.getInputStream ();
                }
            }
        }
        
        public int available () throws IOException {
            openStream ();
            return super.available ();
        }
        
        public long skip (long n) throws IOException {
            openStream ();
            return super.skip (n);
        }
        
        public void reset () throws IOException {
            openStream ();
            super.reset ();
        }
        
        public void close () throws IOException {
            openStream ();
            super.close ();
        }
        
        public int read (byte[] b) throws IOException {
            openStream ();
            return super.read (b);
        }
        
        public int read (byte[] b, int off, int len) throws IOException {
            openStream ();
            return super.read (b, off, len);
        }
        
        public int read () throws IOException {
            openStream ();
            return super.read ();
        }
        
    }
    
}
