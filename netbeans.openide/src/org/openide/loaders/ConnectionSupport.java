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

package org.openide.loaders;

import java.io.*;
import java.util.*;

import org.openide.ErrorManager;
import org.openide.cookies.ConnectionCookie;
import org.openide.nodes.Node;

/** Support implementing ConnectionCookie, that stores
* listeners in extended attributes of associated entry.
*
* @author Jaroslav Tulach, Petr Hamernik
*/
public class ConnectionSupport extends Object implements ConnectionCookie {
    /** extended attribute to store (ArrayList of Type and Node.Handle) */
    private static final String EA_LISTENERS = "EA-OpenIDE-Connection"; // NOI18N

    /** entry to work on */
    private MultiDataObject.Entry entry;
    /** array of types */
    private ConnectionCookie.Type[] types;
    /** cached the return value for getTypes method */
    private Set typesSet;

    /** table of listeners for non-persistent types. Array of Pairs.
    */
    private LinkedList listeners;

    /** Creates new connection support for given file entry.
    * @param entry entry to store listener to its extended attributes
    * @param types a list of event types to support
    */
    public ConnectionSupport (MultiDataObject.Entry entry, ConnectionCookie.Type[] types) {
        this.entry = entry;
        this.types = types;
    }

    /** Attaches new node to listen to events produced by this
    * event. The type must be one of event types supported by this
    * cookie and the listener should have ConnectionCookie.Listener cookie
    * attached so it can be notified when event of requested type occurs.
    *
    * @param type the type of event, must be supported by the cookie
    * @param listener the node that should be notified
    *
    * @exception InvalidObjectException if the type is not supported by the cookie (subclass of IOException)
    * @exception IOException if the type is persistent and the listener does not
    *    have serializable handle (listener.getHandle () is null or its serialization
    *    throws an exception)
    */
    public synchronized void register (ConnectionCookie.Type type, Node listener) throws IOException {
        // test if the file is supported, if not throws exception
        testSupported (type);

        boolean persistent = type.isPersistent ();
        LinkedList list;

        if (persistent) {
            list = (LinkedList)entry.getFile ().getAttribute (EA_LISTENERS);
        } else {
            list = listeners;
        }

        if (list == null) {
            // empty list => create new
            list = new LinkedList ();
        }

        //    System.out.println("======================================== ADD:"+entry.getFile().getName()); // NOI18N
        //    System.out.println(this);
        //    System.out.println("size:"+list.size()); // NOI18N

        Iterator it = list.iterator ();
        while (it.hasNext ()) {
            Pair pair = (Pair)it.next ();
            //      System.out.println("test:"+pair.getType()); // NOI18N
            if (type.equals (pair.getType ())) {
                Node n;
                try {
                    n = pair.getNode ();
                    //          System.out.println("  node:"+n); // NOI18N
                } catch (IOException e) {
                    // node that cannot produce handle => remove it
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    it.remove ();
                    // go on
                    continue;
                }
                //        System.out.println("  compare with:"+listener); // NOI18N
                if (n.equals (listener)) {
                    // we found our node - it is already in the list.
                    //          System.out.println("the listener found - remove it."); // NOI18N
                    it.remove();
                    continue;
                }
                else {
                    //          System.out.println("  nene"); // NOI18N
                }
            }
        }
        list.add (persistent ? new Pair (type, listener.getHandle ()) : new Pair (type, listener));

        //    System.out.println("after add:"+list.size()); // NOI18N

        if (persistent) {
            // save can throw IOException
            entry.getFile ().setAttribute (EA_LISTENERS, list);
        }

    }

    /** Unregisters an listener.
    * @param type type of event to unregister the listener from listening to
    * @param listener to unregister
    * @exception IOException if there is I/O operation error when the removing
    *   the listener from persistent storage
    */
    public synchronized void unregister (ConnectionCookie.Type type, Node listener) throws IOException {
        // test if the file is supported, if not throws exception
        testSupported (type);

        boolean persistent = type.isPersistent ();
        LinkedList list;

        if (persistent) {
            list = (LinkedList)entry.getFile ().getAttribute (EA_LISTENERS);
        } else {
            list = listeners;
        }

        if (list == null) {
            // empty list => no work
            return;
        }

        //    System.out.println("======================================== REMOVE:"+entry.getFile().getName()); // NOI18N
        //    System.out.println(this);
        //  System.out.println("size:"+list.size()); // NOI18N

        Iterator it = list.iterator ();
        while (it.hasNext ()) {
            Pair pair = (Pair)it.next ();

            if (type.equals (pair.getType ())) {
                Node n;
                try {
                    n = pair.getNode ();
                } catch (IOException e) {
                    // node that cannot produce handle => remove it
                    it.remove ();
                    // go on
                    continue;
                }
                if (n.equals (listener)) {
                    // we found our node
                    it.remove ();
                    // break the cycle but save if necessary

                    continue;
                }
            }
        }

        //System.out.println("after remove:"+list.size()); // NOI18N

        if (persistent) {
            // save can throw IOException
            entry.getFile ().setAttribute (EA_LISTENERS, list);
        }
    }

    /** Unmutable set of types supported by this connection source.
    * @return a set of Type objects
    */
    public java.util.Set getTypes () {
        if (typesSet == null)
            typesSet = Collections.unmodifiableSet (new HashSet (Arrays.asList (types)));
        return typesSet;
    }

    /** Get the list of all registered types in every (persistent
    * or not persistent) connections.
    *
    * @return the list of ConnectionCookie.Type objects
    */
    public List getRegisteredTypes() {
        LinkedList typesList = new LinkedList();

        LinkedList list = listeners;
        for (int i = 0; i <= 1; i++) {
            if (i == 1)
                list = (LinkedList)entry.getFile ().getAttribute (EA_LISTENERS);

            if (list == null)
                continue;

            Iterator it = list.iterator ();
            while (it.hasNext ()) {
                typesList.add(((Pair)it.next()).getType());
            }
        }

        return typesList;
    }

    /** Fires info for all listeners of given type.
    * @param ev the event
    */
    public synchronized void fireEvent (ConnectionCookie.Event ev) {
        LinkedList list;
        ConnectionCookie.Type type = ev.getType ();

        boolean persistent = type.isPersistent ();
        if (persistent) {
            list = (LinkedList)entry.getFile ().getAttribute (EA_LISTENERS);
        } else {
            list = listeners;
        }

        if (list == null) return;

        int size = list.size ();

        Iterator it = list.iterator ();
        while (it.hasNext ()) {
            Pair pair = (Pair)it.next ();

            if (pair.getType ().overlaps(ev.getType())) {
                try {
                    ConnectionCookie.Listener l = (ConnectionCookie.Listener)pair.getNode ().getCookie (ConnectionCookie.Listener.class);
                    if (l != null) {
                        try {
                            l.notify (ev);
                        } catch (IllegalArgumentException e) {
                            it.remove ();
                        } catch (ClassCastException e) {			
                            it.remove ();
                        }
		    }
                } catch (IOException e) {
                    it.remove ();
                }
            }
        }
	
	// if something in the list has changed, save it.
        if (persistent && list.size() != size) {
            // save can throw IOException
            try {
                entry.getFile ().setAttribute (EA_LISTENERS, list);
            } catch (IOException e) {
                // ignore never mind
            }
        }
    }

    /** Obtains a set of all listeners for given type.
    * @param type type of events to test
    * @return unmutable set of all listeners (Node) for a type
    */
    public synchronized java.util.Set listenersFor (ConnectionCookie.Type type) {
        LinkedList list;

        if (type.isPersistent ()) {
            list = (LinkedList)entry.getFile ().getAttribute (EA_LISTENERS);
        } else {
            list = listeners;
        }

        if (list == null) return Collections.EMPTY_SET;

        Iterator it = list.iterator ();
        HashSet set = new HashSet (7);

        while (it.hasNext ()) {
            Pair pair = (Pair)it.next ();
            if (type.overlaps(pair.getType ())) {
                try {
                    set.add (pair.getNode ());
                } catch (IOException e) {
                    // ignore the exception
                }
            }
        }

        return set;
    }


    /** Test if the type is supported.
    * @param t type
    * @exception InvalidObjectException if type is not valid
    */
    private void testSupported (ConnectionCookie.Type t) throws InvalidObjectException {
        for (int i = 0; i < types.length; i++) {
            if (t.overlaps(types[i])) {
                return;
            }
        }
        throw new InvalidObjectException (t.toString ());
    }

    /** A pair of type of event and a handle to it.
    */
    private static final class Pair extends Object implements java.io.Serializable {
        /** type of the listener */
        private ConnectionCookie.Type type;
        /** the node or the handle to a node */
        private Object value;

        static final long serialVersionUID =387180886175136728L;
        /** @param t the type of the event
        * @param n the listener
        */
        public Pair (ConnectionCookie.Type t, Node n) {
            type = t;
            value = n;
        }

        /** @param t the type of the event
        * @param h the listener's handle
        * @exception IOException if handle is null
        */
        public Pair (ConnectionCookie.Type t, Node.Handle h) throws IOException {

            if (h == null) throw new IOException ();

            type = t;
            value = h;
        }

        /** Getter of the type.
        */
        public ConnectionCookie.Type getType () {
            return type;
        }

        /** Getter of the listener.
        * @return listener's node
        * @exception IOException if the handle is not able to create a node
        */
        public Node getNode () throws IOException {
            return value instanceof Node ? (Node)value : ((Node.Handle)value).getNode ();
        }
    }
}
