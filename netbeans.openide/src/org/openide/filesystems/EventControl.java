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

package org.openide.filesystems;
import java.io.*;
import java.util.Enumeration;

import org.openide.util.enums.QueueEnumeration;
/**
 *
 * @author  rmatous
 * @version
 */
class EventControl {
    /** number of requests posted and not processed. to
    * know what to do in sync.
    */
    private  int requests;
    /** number of priority requests (requested from priority atomic block) posted 
    * and not processed. to
    * know what to do in sync.
    */    
    private  int priorityRequests;    
    
    /** Holds current propagation ID and link to previous*/
    private AtomicActionLink currentAtomAction;
    
    /** List of requests */
    private QueueEnumeration requestsQueue;
     
    /**
     * Method that can fire events directly, postpone them, fire them in 
     * standalone thread or in RequestProcessor
     */
    void dispatchEvent (FileSystem.EventDispatcher dispatcher) {
      if (postponeFiring (dispatcher)) return;        
      dispatcher.run();
    }
    
    /**
     * Begin of priority atomic actions. Atomic actions from inside of org.openide.FileSystems
     * are considered as priority atomic actions. From last priority atomic actions 
     * are fired events regardless if nested in any normal atomic action. 
     *
     * Begin of block, that should be performed without firing events.
     * Firing of events is postponed after end of block .
     * There is strong necessity to use always both methods: beginAtomicAction
     * and finishAtomicAction. It is recomended use it in try - finally block.
     * @see FileSystemt#beginAtomicAction
     * @param run Events fired from this atomic action will be marked as events 
     * that were fired from this run.
     */        
    void beginAtomicAction (FileSystem.AtomicAction run) {
        enterAtomicAction (run, true);
    }    
    
    /**
     * End of priority atomic actions. Atomic actions from inside of org.openide.FileSystems
     * are considered as priority atomic actions. From last priority atomic actions 
     * are fired events regardless if nested in any normal atomic action. 
     *
     * End of block, that should be performed without firing events.
     * Firing of events is postponed after end of block . 
     * There is strong necessity to use always both methods: beginAtomicAction
     * and finishAtomicAction. It is recomended use it in try - finally block.
     * @see FileSystemt#finishAtomicAction
     */        
    void finishAtomicAction () {
        exitAtomicAction (true);
    }
        
    /** Executes atomic action. The atomic action represents a set of
    * operations constituting one logical unit. It is guaranteed that during
    * execution of such an action no events about changes in the filesystem
    * will be fired.*/    
    void runAtomicAction (final FileSystem.AtomicAction run) throws IOException {
        try {
            enterAtomicAction (run, false);
            run.run ();
        } finally {
            exitAtomicAction (false);
        }
    }

    /** Enters atomic action.
    */
    private  synchronized void enterAtomicAction (Object propID, boolean priority) {
        AtomicActionLink nextPropID = new AtomicActionLink (propID);
        nextPropID.setPreviousLink (currentAtomAction);            
        currentAtomAction = nextPropID;
                                                                     
        if (priority) priorityRequests++;
        if (requests++ == 0) {
            requestsQueue = new QueueEnumeration ();
        }
    }

    /** Exits atomic action.
    */
    private  void exitAtomicAction (boolean priority) {        
        boolean fireAll = false;
        boolean firePriority = false;        
        Enumeration reqQueueCopy;        
        
        synchronized (this) {            
            currentAtomAction = currentAtomAction.getPreviousLink();                
            
            requests--;            
            if (priority) 
                priorityRequests--;
            
            if (requests == 0) 
                fireAll = true; 
                            
            if (!fireAll && priority && priorityRequests == 0) 
                firePriority = true; 
            
            if (fireAll || firePriority) {
                reqQueueCopy = requestsQueue;
                requestsQueue = null;                
                priorityRequests = 0;
            } else 
                return;
        }        
        
        /** firing events outside synchronized block*/
        if (fireAll) {
            invokeDispatchers (false, reqQueueCopy);
            return;
        }
        
        if (firePriority) {
            requestsQueue = new QueueEnumeration ();
            QueueEnumeration newReqQueue = invokeDispatchers (true, reqQueueCopy);
            synchronized (this) {
                while (requestsQueue != null && requestsQueue.hasMoreElements()) {
                    FileSystem.EventDispatcher r = (FileSystem.EventDispatcher)requestsQueue.nextElement();                
                    newReqQueue.put(r);
                }                 
                requestsQueue = newReqQueue;
            }
        }
    }
    
    private QueueEnumeration invokeDispatchers (boolean priority, Enumeration reqQueueCopy) {
        QueueEnumeration newEnum  = new QueueEnumeration ();
        
        while (reqQueueCopy != null && reqQueueCopy.hasMoreElements()) {
            FileSystem.EventDispatcher r = (FileSystem.EventDispatcher)reqQueueCopy.nextElement();                
            r.dispatch(priority);
            if (priority) newEnum.put (r);
        }            
        return newEnum;
    }
    
    /* Adds dispatcher to queue.*/
    private  synchronized boolean postponeFiring (FileSystem.EventDispatcher disp) {
        if (priorityRequests == 0) {
            disp.setAtomicActionLink (currentAtomAction);                        
            disp.dispatch(true);
        }
        
        if (requestsQueue != null) {
            // run later
            disp.setAtomicActionLink (currentAtomAction);
            requestsQueue.put (disp);
            return true;
        }        
        return false;
    }
    /** Container that holds hierarchy of propagation IDs related to atomic actions
     *  Implemented as linked list
     */
    static final class AtomicActionLink {
        private AtomicActionLink upper;
        private Object  propagationID;
        
        AtomicActionLink (Object propagationID) {
            this.propagationID = propagationID;
        }
        
        Object getAtomicAction () {
            return propagationID;
        }
        
        void setPreviousLink (AtomicActionLink upper) {
            this.upper = upper;
        }

        AtomicActionLink getPreviousLink () {
            return upper;
        }        
    }    
}
