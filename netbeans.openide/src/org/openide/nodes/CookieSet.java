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

package org.openide.nodes;

import java.util.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

/** Support class for storing cookies and
* retriving them by representation class.
* Provides simple notifications about changes
* in cookies.
*
* @author Jaroslav Tulach
*/
public final class CookieSet extends Object {
    /** list of cookies (Class, Node.Cookie) */
    private HashMap map = new HashMap (31);

    /** set of listeners */
    private EventListenerList listeners = new EventListenerList ();

    /** Default constructor. */
    public CookieSet() {}
    
    /** Add a new cookie to the set. If a cookie of the same
    * <em>actual</em> (not representation!) class is already there,
    * it is replaced.
    * <p>Cookies inserted earlier are given preference during lookup,
    * in case a supplied representation class matches more than one cookie
    * in the set.
    *
    * @param cookie cookie to add
    */
    public void add (Node.Cookie cookie) {
        synchronized (this) {
            registerCookie (cookie.getClass (), cookie);
        }

        fireChangeEvent ();
    }

    /** Remove a cookie from the set.
    * @param cookie the cookie to remove
    */
    public void remove (Node.Cookie cookie) {
        synchronized (this) {
            unregisterCookie (cookie.getClass (), cookie);
        }

        fireChangeEvent ();
    }

    /** Get a cookie.
    *
    * @param clazz the representation class
    * @return a cookie assignable to the representation class, or <code>null</code> if there is none
    */
    public Node.Cookie getCookie (Class clazz) {
        Node.Cookie ret = null;
        synchronized (this) {
            R r = findR (clazz);
            if (r == null) {
                return null;
            }
            ret = r.cookie ();
        }
        if (ret instanceof CookieEntry) {
            ret = ((CookieEntry) ret).getCookie();
        }

        return ret;
    }

    /** Add a listener to changes in the cookie set.
    * @param l the listener to add
    */
    public void addChangeListener (ChangeListener l) {
        listeners.add (ChangeListener.class, l);
    }

    /** Remove a listener to changes in the cookie set.
    * @param l the listener to remove
    */
    public void removeChangeListener (ChangeListener l) {
        listeners.remove (ChangeListener.class, l);
    }

    /** Fires change event
    */
    private void fireChangeEvent () {
        Object[] arr = listeners.getListenerList();
        if (arr.length > 0) {
            ChangeEvent ev = null;
            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = arr.length-2; i>=0; i-=2) {
                if (arr[i] == ChangeListener.class) {
                    if (ev == null) {
                        ev = new ChangeEvent (this);
                    }
                
                    ((ChangeListener)arr[i + 1]).stateChanged (ev);
                }
            }
        }
    }

    /** Attaches cookie to given class and all its superclasses and
    * superinterfaces.
    *
    * @param c class or null
    * @param cookie cookie to attach
    */
    private void registerCookie (Class c, Node.Cookie cookie) {
        if ((c == null) || !Node.Cookie.class.isAssignableFrom(c)) return;

        R r = findR (c);
        if (r == null) {
            r = new R ();
            map.put (c, r);
        }
        
        r.add (cookie);

        registerCookie (c.getSuperclass (), cookie);

        Class[] inter = c.getInterfaces ();
        for (int i = 0; i < inter.length; i++) {
            registerCookie (inter[i], cookie);
        }
    }
    
    /** Removes cookie from the class and all its superclasses and
    * superinterfaces.
    *
    * @param c class or null
    * @param cookie cookie to attach
    */
    private void unregisterCookie (Class c, Node.Cookie cookie) {
        if (
            c == null || !Node.Cookie.class.isAssignableFrom(c)
        ) return;

        // if different cookie is attached to class c stop removing
        R r = findR (c);
        if (r != null) {
            // remove the cookie
            r.remove (cookie);
        }

        unregisterCookie (c.getSuperclass (), cookie);

        Class[] inter = c.getInterfaces ();
        for (int i = 0; i < inter.length; i++) {
            unregisterCookie (inter[i], cookie);
        }
    }
    
    /** Registers a Factory for given cookie class */
    public void add(Class cookieClass, Factory factory) {
        if (factory == null) {
            throw new IllegalArgumentException();
        }
        
        synchronized (this) {
            registerCookie (cookieClass, new CookieEntry(factory, cookieClass));
        }

        fireChangeEvent ();
    }

    /** Registers a Factory for given cookie classes */
    public void add(Class[] cookieClass, Factory factory) {
        if (factory == null) {
            throw new IllegalArgumentException();
        }
        
        synchronized (this) {
            for (int i = 0; i < cookieClass.length; i++) {
                registerCookie (cookieClass[i], new CookieEntry(factory, cookieClass[i]));
            }
        }

        fireChangeEvent ();
    }    
    
    /**
     * Unregisters a Factory for given cookie class
     * @since 2.6
     */
    public void remove(Class cookieClass, Factory factory) {
        if (factory == null) {
            throw new IllegalArgumentException();
        }
        
        synchronized (this) {
            R r = findR(cookieClass);
            if (r != null) {
                Node.Cookie c = r.cookie();
                if (c instanceof CookieEntry) {
                    CookieEntry ce = (CookieEntry)c;
                    if (ce.factory == factory) {
                        unregisterCookie (cookieClass, c);
                    }
                }
            }
        }

        fireChangeEvent ();
    }

    /** 
     * Unregisters a Factory for given cookie classes
     * @since 2.6
     */
    public void remove(Class[] cookieClass, Factory factory) {
        if (factory == null) {
            throw new IllegalArgumentException();
        }
        
        synchronized (this) {
            for (int i = 0; i < cookieClass.length; i++) {
                R r = findR(cookieClass[i]);
                if (r != null) {
                    Node.Cookie c = r.cookie();
                    if (c instanceof CookieEntry) {
                        CookieEntry ce = (CookieEntry)c;
                        if (ce.factory == factory) {
                            unregisterCookie (cookieClass[i], c);
                        }
                    }
                }
            }
        }

        fireChangeEvent ();
    }    
    
    /** Finds a result in a map.
     */
    private R findR (Class c) {
        return (R)map.get (c);
    }

    /** Finds base class for a cookie.
     * @param c cookie
     * @return base class
     */
    private static Class baseForCookie (Node.Cookie c) {
        if (c instanceof CookieEntry) {
            return ((CookieEntry)c).klass;
        } 
        return c.getClass ();
    }
    
    
    /** Factory for creating cookies of given Class */
    public static interface Factory {
        /** Creates a Node.Cookie of given class. The method
         * may be called more than once.
         */
        public Node.Cookie createCookie(Class klass);
    }
    
    
    /** Entry for one Cookie */
    private static class CookieEntry implements Node.Cookie {
        /** Factory for the cookie */
        final Factory factory;
        /** Class of the cookie */
        private final Class klass;
        /** A Referenec to the cookie */
        private Reference cookie;
        
        /** Constructs new FactoryEntry */
        public CookieEntry(Factory factory, Class klass) {
            this.factory = factory;
            this.klass = klass;
        }
        
        /** Getter for the cookie.
         * Synchronized because we don't want to run factory.createCookie
         * symultaneously from two threads.
         */
        public synchronized Node.Cookie getCookie() {
            Node.Cookie ret;
            if ((cookie == null) || ((ret = (Node.Cookie) cookie.get()) == null)) {
                ret = factory.createCookie(klass);
                if (ret == null) return null;
                cookie = new WeakReference(ret);
            }
            
            return ret;
        }
    }
    
    /** Implementation of the result.
     */
    private static final class R extends Object {
        /** list of registered cookies */
        public List cookies;
        /** base class of the first cookie registered here */
        public Class base;
        
        R() {}
        
        /** Adds a cookie.
         * @return true if adding should continue on superclasses should continue
         */
        public void add (Node.Cookie cookie) {
            if (cookies == null) {
                cookies = new ArrayList (1);
                cookies.add (cookie);
                base = baseForCookie (cookie);
                return;
            }
            
            Class newBase = baseForCookie (cookie);
            if (base == null || newBase.isAssignableFrom (base)) {
                cookies.set (0, cookie);
                base = newBase;
            } else {
                cookies.add (cookie);
            }
        }
        
        /** Removes a cookie.
         * @return true if empty
         */
        public boolean remove (Node.Cookie cookie) {
            if (cookies == null) {
                return true;
            }
            
            if (cookies.remove (cookie) && cookies.size () == 0) {
                base = null;
                cookies = null;
                return true;
            }
            
            base = baseForCookie ((Node.Cookie)cookies.get (0));
            
            return false;
        }
        
        /** @return the cookie for this result or null
         */
        public Node.Cookie cookie () {
            return cookies == null || cookies.isEmpty () ? null : (Node.Cookie)cookies.get (0);
        }
    }
}
