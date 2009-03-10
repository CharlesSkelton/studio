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

import java.util.*;

/**
 * A class that holds a list of EventListeners. 
 * Replacement of  EventListListener, that solves performance issue #20715  
 * @author  rm111737
 */
class ListenerList {
    private Class evListenerClass;
    private List listenerList;
    private Object[] listenerArray = null;

    /**
     * @param evListenerClass the type of the EventListener. 
     */
    ListenerList (Class evListenerClass) {
        this.evListenerClass = evListenerClass;                
        listenerList = Collections.synchronizedList(new ArrayList ());
    }    
    
    /**
     * Adds the listener .
     **/
    boolean add (EventListener listener) {
        if (!evListenerClass.isInstance(listener))
            return false;

        listenerArray = null;        
        return listenerList.add(listener);        
    }

    /**
     * Removes the listener .
     **/    
    boolean remove (EventListener listener) {
        if (!evListenerClass.isInstance(listener))
            return false;
        
        listenerArray = null;        
        return listenerList.remove (listener);                
    }
    
    /**
     * Passes back the event listener list as an array    
     */
    synchronized Object[] getAllListeners () {
        Object[] retVal = listenerArray;
        if (retVal == null) {
            retVal = new Object [listenerList.size()];
            listenerList.toArray(retVal);
            listenerArray = retVal;
        }        
        return retVal;
    }
    
}
