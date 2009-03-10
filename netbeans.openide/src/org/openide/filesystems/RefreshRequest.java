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

package org.openide.filesystems;

import java.beans.*;
import java.io.*;
import java.lang.ref.*;
import java.util.Enumeration;

import org.openide.util.RequestProcessor;

/** Request for parsing of an filesystem. Can be stoped.
*
* @author Jaroslav Tulach
*/
final class RefreshRequest extends Object implements Runnable {
    /** how much folders refresh at one request */
    private static final int REFRESH_COUNT = 30;
    private static RequestProcessor REFRESHER = new RequestProcessor ("FS refresher"); // NOI18N

    /** fs to work on */
    private Reference system;

    /** enumeration of folders Reference (FileObjects) to process */
    private Enumeration en;

    /** how often invoke itself */
    private int refreshTime;

    /** task to call us */
    private RequestProcessor.Task task;

    /** Constructor
    * @param fs file system to refresh
    * @param ms refresh time
    */
    public RefreshRequest (AbstractFileSystem fs, int ms) {
        system = new WeakReference (fs);
        refreshTime = ms;
        task = REFRESHER.post (this, ms, Thread.MIN_PRIORITY);
    }

    /** Getter for the time.
    */
    public int getRefreshTime () {
        return refreshTime;
    }

    /** Stops the task.
    */
    public synchronized void stop () {
        refreshTime = 0;

        if (task == null) {        
            // null task means that the request processor is running =>
            // wait for end of task execution
            try {
                wait ();
            } catch (InterruptedException ex) {
            }
        }
    }


    /** Refreshes the system.
    */
    public void run () {
        // this code is executed only in RequestProcessor thread
        
        int ms;
        RequestProcessor.Task t;
        
        synchronized (this) {
            // the synchronization is here to be sure
            // that 
            ms = refreshTime;
            
            if (ms <= 0) {
                // finish silently if already stopped
                return;
            }
            
            t = task;
        }
        
        try {
          // by setting task to null we indicate that we are currently processing
          // files and that any stop should wait till the processing is over
          task = null;
          
          doLoop (ms);
        } finally {
             synchronized (this) {
                 // reseting task variable back to indicate that 
                 // the processing is over
                 task = t;
                 
                 notifyAll ();
                 
             }
             // plan the task for next execution
             if (system != null && system.get () != null)
                t.schedule (ms);
             else 
                refreshTime = 0;
        }
    }
    
    
    private void doLoop (int ms) {
        AbstractFileSystem system = (AbstractFileSystem)this.system.get ();
        if (system == null) {
            // end for ever the fs does not exist no more
            return;
        }

        if (en == null || !en.hasMoreElements ()) {
            // start again from root
            en = existingFolders (system);
        }

        for (int i = 0; i < REFRESH_COUNT && en.hasMoreElements (); i++) {
            AbstractFolder fo = (AbstractFolder)en.nextElement ();
            if (fo != null && (!fo.isFolder() || fo.isInitialized ())) {
                fo.refresh ();
            }
            
            if (refreshTime <= 0) {
                // after each refresh check the current value of refreshTime
                // again and if it goes to zero exit as fast a you can
                return;
            }
        }

        // clear the queue
        if (!en.hasMoreElements ()) {
            en = null;
        }
    }

    /** Existing folders for abstract file objects.
    */
    private static Enumeration existingFolders (AbstractFileSystem fs) {
        return fs.existingFileObjects (fs.getAbstractRoot());
    }
    
    /**
     * Overriden for debugging/logging purposes.
     */
    public String toString() {
	AbstractFileSystem fs = (AbstractFileSystem)system.get();
	return "RefreshRequest for " + 				// NOI18N
	    (fs == null ? "gone FS" : fs.getSystemName());	// NOI18N
    }
}
