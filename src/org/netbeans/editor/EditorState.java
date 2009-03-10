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

package org.netbeans.editor;

import java.util.HashMap;

/** This singleton class is an editor state encapsulation object. Every part
 * of the editor could store its state-holder here and it will be automatically
 * persistent across restarts. It is intended for any state informations
 * that are not "Settings", like the contents of the input field histories,
 * persistent, named bookmarks or so.
 * The implementation is just like a HashMap indexed by state-holders' names.
 * Typical usage is <CODE>myState = EditorState.get( MY_STATE_NAME );</CODE>
 * There is no support for state change notifications, but the inserted
 * value objects could be singletons as well and could do its own notifications.
 *
 * @author  Petr Nejedly
 * @version 1.0
 */
public class EditorState {
    private static HashMap state = new HashMap();
    
    /** This is fixed singleton, don't need instances */
    private EditorState() {
    }
  
    /** Retrieve the object specified by the key. */
    public static Object get( Object key ) {
        return state.get( key );
    }

    /** Store the object under specified key */
    public static void put( Object key, Object value ) {
        state.put( key, value );
    }
    
    public static HashMap getStateObject() {
        return state;
    }
    
    public static void setStateObject( HashMap stateObject ) {
        state = stateObject;
    }
}
