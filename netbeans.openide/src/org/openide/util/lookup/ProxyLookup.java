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

package org.openide.util.lookup;

import java.util.*;
import java.lang.ref.Reference;

import javax.swing.event.EventListenerList;

import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.LookupEvent;

/** Implementation of lookup that can delegate to others.
 *
 * @author  Jaroslav Tulach
 * @since 1.9
 */
public class ProxyLookup extends Lookup {
    /** lookups to delegate to */
    private Lookup[] lookups;
    /** map of templates to currently active results */
    private HashMap results;
    
    /** Create a proxy to some other lookups.
     * @param lookups the initial delegates
     */
    public ProxyLookup (Lookup[] lookups) {
        this.lookups = lookups;
    }
    
    /**
     * Create a lookup initially proxying to no others.
     * Permits serializable subclasses.
     * @since 3.27
     */
    protected ProxyLookup() {
        this(new Lookup[0]);
    }
    
    public String toString() {
        return "ProxyLookup(class=" + getClass() + ")->" + Arrays.asList(lookups); // NOI18N
    }

    /** Getter for the delegates.
    * @return the array of lookups we delegate to
    * @since 1.19
    */
    protected final Lookup[] getLookups () {
        return lookups;
    }
    
    /** Change the delegates. To forbid anybody else then the creator
     * of the lookup to change the delegates, this method is protected.
     * 
     * @param lookups the new lookups to delegate to
     * @since 1.19 protected
     */
    protected final void setLookups (Lookup[] lookups) {
        Reference[] arr;
        HashSet newL;
        HashSet current;
        Lookup[] old;
        
        synchronized (this) {
            current = new HashSet (Arrays.asList (this.lookups));
            newL = new HashSet (Arrays.asList (lookups));

            old = this.lookups;
            this.lookups = lookups;
            
            if (results == null || results.isEmpty ()) {
                // no affected results => exit
                return;
            }
	    arr = (Reference[])results.values ().toArray( new Reference[0] );
        
            HashSet removed = new HashSet (current);
            removed.removeAll (newL); // current contains just those lookups that have disappeared
            newL.removeAll (current); // really new lookups

            if (removed.isEmpty () && newL.isEmpty ()) {
                // no need to notify changes
                return;
            }

            for (int i = 0; i < arr.length; i++) {
                R r = (R)arr[i].get ();
                if (r != null) {
                    r.lookupChange (newL, removed, old, lookups);
                }
            }
        }
        // this cannot be done from the synchronized block
        for (int i = 0; i < arr.length; i++) {
            R r = (R)arr[i].get ();
            if (r != null) {
                r.resultChanged(null);
            }
        }
    }
    
    /** Notifies subclasses that a query is about to be processed.
     * Subclasses can update its state before the actual processing
     * begins. It is allowed to call <code>setLookups</code> method
     * to change/update the set of objects the proxy delegates to.
     *
     * @param template the template of the query
     * @since 1.31
     */
    protected void beforeLookup (Template template) {
    }
    
    
    /* Lookups an object of given interface. This is the simplest method
     * for the lookuping, if more registered objects implement the given
     * class any of them can be returned. 
     *
     * @param clazz class of the object we are searching for
     * @return the object implementing given class or null if no such 
     *    has been found
     */
    public final Object lookup (Class clazz) {
        beforeLookup (new Template (clazz));
        
        Lookup[] lookups = this.lookups;

        for (int i = 0; i < lookups.length; i++) {
            Object o = lookups[i].lookup (clazz);
            if (o != null) {
                return o;
            }
        }
        
        return null;
    }
    
    /* Lookups the first item that matches given template.
     * @param template the template to check
     * @return item or null
     */
    public final Item lookupItem(Template template) {
        beforeLookup (template);
        
        Lookup[] lookups = this.lookups;

        for (int i = 0; i < lookups.length; i++) {
            Item o = lookups[i].lookupItem (template);
            if (o != null) {
                return o;
            }
        }
        
        return null;
    }
    
    /* The general lookup method.
     * @param template the template describing the services we are looking for
     * @return object containing the results
     */
    public final synchronized Result lookup (Lookup.Template template) {
        R r;
        if (results != null) {
            Reference ref = (Reference)results.get (template);
            r = ref == null ? null : (R)ref.get ();
            if (r != null ) {
                return r;
            }
        } else {
            results = new HashMap ();
        }
        r = new R (template);
        results.put (template, new java.lang.ref.SoftReference (r));
        
        return r;
    }
    
    /** Unregisters a template from the has map.
     */
    private final synchronized void unregisterTemplate (Template template) {
        if (results == null) return;
        Reference ref = (Reference)results.remove (template);
        if (ref != null && ref.get () != null) {
            // seems like there is a reference to a result for this template
            // thta is still alive
            results.put (template, ref);
        }
    }

    /** Result of a lookup request. Allows access to single object
     * that was found (not too useful) and also to all objects found
     * (more useful).
     */
    private final class R extends WaitableResult implements LookupListener {
        /** list of listeners added */
        private javax.swing.event.EventListenerList listeners;
        
        /** template for this result */
        private Lookup.Template template;
        /** all results */
        private Lookup.Result[] results;
        /** collection of Objects */
        private Collection[] cache;
        
        
        /** Constructor.
         */
        public R (Lookup.Template t) {
            template = t;
        }
        
        /** When garbage collected, remove the template from the has map.
         */
        protected void finalize () {
            unregisterTemplate (template);
        }

        /** initializes the results 
         */
        private Result[] initResults () {
            if (results != null) return results;
            
            Result[] arr = new Result[lookups.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = lookups[i].lookup (template);
                arr[i].addLookupListener (this);
            }
            cache = new Collection[3];
            
            results = arr;
            return arr;
        }
        
        /** Called when there is a change in the list of proxied lookups.
         * @param added set of added lookups
         * @param remove set of removed lookups
         * @param current array of current lookups
         */
        protected void lookupChange (
            Set added, Set removed, Lookup[] old, Lookup[] current
        ) {
            synchronized (this) {
                if (results == null) {
                    // not computed yet, do not need to do anything
                    return;
                }

                // map (Lookup, Lookup.Result)
                HashMap map = new HashMap (old.length * 2);


                for (int i = 0; i < old.length; i++) {
                    if (removed.contains (old[i])) {
                        // removed lookup
                        results[i].removeLookupListener (this);
                    } else {
                        // remember the association
                        map.put (old[i], results[i]);
                    }
                }

                Lookup.Result[] arr = new Lookup.Result[current.length];
                for (int i = 0; i < current.length; i++) {
                    if (added.contains (current[i])) {
                        // new lookup
                        arr[i] = current[i].lookup (template);
                        arr[i].addLookupListener (this);
                    } else {
                        // old lookup
                        arr[i] = (Lookup.Result)map.get (current[i]);
                        if (arr[i] == null) {
                            // assert
                            throw new IllegalStateException ();
                        }
                    }
                }

                // remember the new results
                results = arr;
            }
        }
        
        /** Just delegates.
         */
        public void addLookupListener (LookupListener l) {
            if (listeners == null) {
                synchronized (this) {
                    if (listeners == null) {
                        listeners = new EventListenerList ();
                    }
                }
            }
            listeners.add (LookupListener.class, l);
        }
            
        /** Just delegates.
         */
        public void removeLookupListener (LookupListener l) {
            if (listeners != null) {
                listeners.remove (LookupListener.class, l);
            }
        }
        
        /** Access to all instances in the result.
         * @return collection of all instances
         */
        public java.util.Collection allInstances () {
            return computeResult (0);
        }
            
        /** Classes of all results. Set of the most concreate classes
         * that are registered in the system.
         * @return set of Class objects
         */
        public java.util.Set allClasses () {
            return (java.util.Set)computeResult (1);
        }
        
        /** All registered items. The collection of all pairs of 
         * ii and their classes.
         * @return collection of Lookup.Item
         */
        public java.util.Collection allItems () {
            return computeResult (2);
        }
            

        /** Computes results from proxied lookups.
         * @param indexToCache 0 = allInstances, 1 = allClasses, 2 = allItems
         * @return the collection or set of the objects
         */
        private java.util.Collection computeResult (int indexToCache) {
            // results to use
            Lookup.Result[] arr = myBeforeLookup ();
            
            
            // if the call to beforeLookup resulted in deletion of caches
            synchronized (this) {
                if (cache != null && cache[indexToCache] != null) {
                    return cache[indexToCache];
                }
            }
                

            // initialize the collection to hold result
            Collection ll;
            if (indexToCache == 1) {
                ll = new HashSet ();
            } else {
                ll = new ArrayList (arr.length * 2);
            }
            
            // fill the collection
            for (int i = 0; i < arr.length; i++) {
                switch (indexToCache) {
                    case 0:
                        ll.addAll (arr[i].allInstances ());
                        break;
                    case 1:
                        ll.addAll (arr[i].allClasses ());
                        break;
                    case 2:
                        ll.addAll (arr[i].allItems ());
                        break;
                }
            }

            synchronized (this) {
                if (arr == results && cache != null) {
                    // updates the results, if the results have not been
                    // changed during the computation of allInstances
                    cache[indexToCache] = ll;
                }
            }
            
            return ll;
        }
        
        /** When the result changes, fire the event.
         */
        public void resultChanged (LookupEvent ev) {
            // clear cached instances
            synchronized (this) {
                cache = null;
                if (listeners == null) return;
            }
            
            Object[] arr = listeners.getListenerList ();
            if (arr.length == 0) {
                return;
            }
            
            ev = new LookupEvent (this);
            AbstractLookup.notifyListeners(arr, ev);
        }

        /** Implementation of my before lookup.
         * @return results to work on.
         */
        private Lookup.Result[] myBeforeLookup () {
            ProxyLookup.this.beforeLookup (template);
            
            Lookup.Result[] arr;
            synchronized (this) {
                arr = initResults ();
            }

            // invoke update on the results
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] instanceof WaitableResult) {
                    WaitableResult w = (WaitableResult)arr[i];
                    w.beforeLookup (template);
                }
            }
            
            return arr;
        }

        
        /** Used by proxy results to synchronize before lookup.
         */
        protected void beforeLookup(Lookup.Template t) {
            if (t.getType () == template.getType ()) {
                myBeforeLookup ();
            }
        }        
        
    }
}
