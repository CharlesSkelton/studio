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

package org.openide.loaders;

import java.beans.*;
import java.util.*;

import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.util.WeakListener;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/** Watches over a folder and represents its
* child data objects by nodes.
*
* @author Jaroslav Tulach
*/
final class FolderChildren extends Children.Keys 
implements PropertyChangeListener {
    /** the folder */
    private DataFolder folder;
    /** filter of objects */
    private DataFilter filter;
    /** listener on changes in nodes */
    private PropertyChangeListener listener;
    /** logging, if needed */
    private ErrorManager err;
    /** this is true between addNotify and removeNotify */
    private boolean active;
    /**  we wait for this task finished in getNodes(true) */
    private RequestProcessor.Task refreshTask;
    /** Runnable scheduled to refRP */
    private ChildrenRefreshRunnable refreshRunnable;
    
    /** Private req processor for the refresh tasks */
    private static RequestProcessor refRP = 
        new RequestProcessor("FolderChildren_Refresh"); // NOI18N
    
    /**
    * @param f folder to display content of
    * @param map map to use for holding of children
    */
    public FolderChildren (DataFolder f) {
        this (f, DataFilter.ALL);
    }

    /**
    * @param f folder to display content of
    * @param filter filter of objects
    */
    public FolderChildren (DataFolder f, DataFilter filter) {
        this.folder = f;
        this.filter = filter;
        this.listener = WeakListener.propertyChange (this, folder);
        err = ErrorManager.getDefault().getInstance("org.openide.loaders.FolderChildren." + f.getPrimaryFile().getPath().replace('/','.')); // NOI18N
        if (!err.isLoggable(ErrorManager.INFORMATIONAL)) {
            err = null;
        }
    }
    
    /** used from DataFolder */
    DataFilter getFilter () {
        return filter;
    }

    /** If the folder changed its children we change our nodes.
    */
    public void propertyChange (final PropertyChangeEvent ev) {
        if (DataFolder.PROP_CHILDREN.equals (ev.getPropertyName ())) {
            if (err != null) err.log("Got PROP_CHILDREN");
            refreshChildren().schedule (0);
            postClearTask();
            return;
        }
        if (
            DataFolder.PROP_SORT_MODE.equals (ev.getPropertyName ()) ||
            DataFolder.PROP_ORDER.equals (ev.getPropertyName ())
        ) {
            if (err != null) err.log("Got PROP_SORT_MODE or PROP_ORDER");
            refreshChildren().schedule (0);
            postClearTask();
            return;
        }
    }

    /**
     * refreshRunnable holds references to the data object
     * to prevent GC. This method post a task to the same request processor
     * (refRP) to clear this references after they are no longer needed.
     */ 
    private void postClearTask() {
        refRP.post(new Runnable() {
            public void run() {
                refreshRunnable.clear();
            }
        });
    }
    
    /** Refreshes the children.
    */
    synchronized RequestProcessor.Task refreshChildren() {
        if (refreshTask == null) {
            refreshTask = refRP.post(refreshRunnable = new ChildrenRefreshRunnable());
        }
        return refreshTask;    
    }

    /** Create a node for one data object.
    * @param key DataObject
    */
    protected Node[] createNodes (Object key) {
        if (err != null) err.log("createNodes: " + key);
        FileObject fo = ((Pair)key).primaryFile;
        DataObject obj;
        try {
            obj = DataObject.find (fo);
            if (filter == null || filter.acceptDataObject (obj)) {
                return new Node[] { obj.getClonedNodeDelegate (filter) };
            } else {
                return new Node[0];
            }
        } catch (DataObjectNotFoundException e) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
            return new Node[0];
        }
    }
  
    public Node[] getNodes(boolean optimalResult) {
        if (optimalResult) {
            if (checkChildrenMutex()) {
                active = true;
                FolderList.find(folder.getPrimaryFile(), true).waitProcessingFinished();
                Task task = refreshChildren();
                task.waitFinished();
            } else {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL,
                    new IllegalStateException("getNodes(true) called while holding the Children.MUTEX") // NOI18N
                );
            }
        }
        Node[] res = getNodes();
        refreshRunnable.clear(); // we can clean the references to data objects now
                                 // they are no longer needed
        return res;
    }
    
    public Node findChild(String name) {
        if (checkChildrenMutex()) {
            getNodes(true);
        }
        return super.findChild(name);
    }
    
    /**
     * This code could probably be replaced by
     * Children.MUTEX.isReadAccess, if such call would be added to Mutex
     * @return true if it is safe to wait (our thread is
     *         not in Children.MUTEX.readAccess
     */
    private static boolean checkChildrenMutex() {
        class MutexChecker implements Runnable {
            public boolean checkReadOrWrite;
            public boolean inReadAccess = true;
            public boolean inWriteAccess = true;
            public void run () {
                if (checkReadOrWrite) {
                    inReadAccess = false;
                } else {
                    inWriteAccess = false;
                }
            }
        }
        MutexChecker test = new MutexChecker();
        // the code will run either immediatally or after we leave readAccess
        // section
        Children.MUTEX.postWriteRequest(test);
        test.checkReadOrWrite = true;
        Children.MUTEX.postReadRequest(test);

        return !test.inReadAccess && !test.inWriteAccess;
    }
    
    /** Initializes the children.
    */
    protected void addNotify () {
        // add as a listener for changes on nodes
        folder.addPropertyChangeListener (listener);
        // 
        active = true;
        // start the refresh task to compute the children
        refreshChildren();
    }

    /** Deinitializes the children.
    */
    protected void removeNotify () {
        // removes the listener
        folder.removePropertyChangeListener (listener);
        //
        active = false;
        // we don't call the setKeys directly here because
        // there can be a task spawned by refreshChildren - so
        // we want to clear the children after that task is finished
        refreshChildren();
    }

    /** Display name */
    public String toString () {
        return folder.getPrimaryFile ().toString ();
    }
    
    /**
     * Instances of this class are posted to the request processor refRP
     * (FolderChildren_refresher). We do this because we do not want
     * to call setKeys synchronously.
     */
    private final class ChildrenRefreshRunnable implements Runnable {
        /** store the referneces to the data objects to
         * prevent GC.
         */
        private DataObject[] ch;
        
        /** calls setKeys with the folder children 
         * or with empty collection if active is false
         */
        public void run() {
            if (! active) {
                setKeys (java.util.Collections.EMPTY_SET);
                return;
            }
            ch = folder.getChildren();
            Object []keys = new Object[ch.length];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = new Pair(ch[i].getPrimaryFile());
            }
            setKeys(Arrays.asList(keys));
        }
        
        /** stop holding the references to the data objects. After
         * calling this they can be GCed again.
         */
        public void clear() {
            ch = null;
        }
    }
    
    /** Pair of dataobject invalidation sequence # and primary file.
     * It serves as a key for the given data object.
     * It is here to create something different then data object,
     * because the data object should be finalized when not needed and
     * that is why it should not be used as a key.
     */
    private static final class Pair extends Object {
        public FileObject primaryFile;
        public int seq;

        public Pair (FileObject primaryFile) {
            this.primaryFile = primaryFile;
            this.seq = DataObjectPool.getPOOL().registrationCount(primaryFile);
        }

        public int hashCode () {
            return primaryFile.hashCode () ^ seq;
        }

        public boolean equals (Object o) {
            if (o instanceof Pair) {
                Pair p = (Pair)o;
                return primaryFile.equals (p.primaryFile) && seq == p.seq;
            }
            return false;
        }
        
        public String toString() {
            return "FolderChildren.Pair[" + primaryFile + "," + seq + "]"; // NOI18N
        }
    }
}
