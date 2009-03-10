/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.util.lookup;

import java.util.*;

import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.LookupEvent;

/**
 * Simple proxy lookup. Keeps reference to a lookup it delegates to and 
 * forwards all requests.
 *
 * @author Jaroslav Tulach
 */
final class SimpleProxyLookup extends org.openide.util.Lookup {
    /** the provider to check for the status */
    private Provider provider;
    /** the lookup we currently delegate to */
    private Lookup delegate;
    /** set of all results associated to this lookup */
    private org.openide.util.WeakSet results;
    
    /** 
     * @param provider provider to delegate to
     */
    SimpleProxyLookup(Provider provider) {
        this.provider = provider;
    }

    /** Checks whether we still delegate to the same lookup */
    private Lookup checkLookup () {
        Lookup l = provider.getLookup ();
        
        
        Iterator toCheck = null;
        synchronized (this) {
            if (l != delegate) {
                this.delegate = l;
                if (results != null) {
                    toCheck = Arrays.asList (results.toArray ()).iterator();
                }
            }
        }
        
        if (toCheck != null) {
            // update
            Iterator it = toCheck;
            while (it.hasNext()) {
                ProxyResult p = (ProxyResult)it.next ();
                if (p.updateLookup (l)) {
                    p.resultChanged (null);
                }
            }
        }
        
        return delegate;
    }
    
    public Result lookup(Template template) {
        ProxyResult p = new ProxyResult (template);
        
        synchronized (this) {
            if (results == null) {
                results = new org.openide.util.WeakSet ();
            }
            results.add (p);
        }
        
        return p;
    }
    
    public Object lookup(Class clazz) {
        return checkLookup ().lookup (clazz);
    }
    
    public Item lookupItem(Template template) {
        return checkLookup ().lookupItem (template);
    }    
    
    /**
     * Result used in SimpleLookup. It holds a reference to the collection
     * passed in constructor. As the contents of this lookup result never
     * changes the addLookupListener and removeLookupListener are empty.
     */
    private final class ProxyResult extends WaitableResult 
    implements LookupListener {
        /** Template used for this result. It is never null.*/
        private Template template;
        /** result to delegate to */
        private Lookup.Result delegate;
        /** listeners set */
        private javax.swing.event.EventListenerList listeners;
        
        /** Just remembers the supplied argument in variable template.*/
        ProxyResult (Template template) {
            this.template = template;
        }

        /** Checks state of the result
         */
        private Result checkResult () {
            updateLookup (checkLookup ());
            return this.delegate;
        }
        
        /** Updates the state of the lookup.
         * @return true if the lookup really changed
         */
        public boolean updateLookup (Lookup l) {
            Collection oldPairs = delegate != null ? delegate.allItems () : null;
            
            synchronized (this) {
                if (delegate != null) {
                    delegate.removeLookupListener (this);
                }
                delegate = l.lookup (template);
                delegate.addLookupListener (this);
            }
            
            if (oldPairs == null) {
                // nobody knows about a change
                return false;
            }
            
            Collection newPairs = delegate.allItems ();
            
            return !oldPairs.equals (newPairs);
        }
        
        
        public synchronized void addLookupListener(LookupListener l) {
            if (listeners == null) {
                listeners = new javax.swing.event.EventListenerList ();
            }
            listeners.add (LookupListener.class, l);
        }
        
        public synchronized void removeLookupListener(LookupListener l) {
            if (listeners != null) {
                listeners.remove (LookupListener.class, l);
            }
        }
        
        public java.util.Collection allInstances() {
            return checkResult ().allInstances ();
        }
        
        public Set allClasses () {
            return checkResult ().allClasses ();
        }
        
        public Collection allItems () {
            return checkResult ().allItems ();
        }
        
        protected void beforeLookup(Lookup.Template t) {
            Lookup.Result r = checkResult ();
            if (r instanceof WaitableResult) {
                ((WaitableResult)r).beforeLookup (t);
            }
        }
        
        /** A change in lookup occured.
         * @param ev event describing the change
         *
         */
        public void resultChanged(LookupEvent anEvent) {
            javax.swing.event.EventListenerList l = this.listeners;
            if (l == null) return;
            
            Object[] listeners = l.getListenerList();
            if (listeners.length == 0) return;
            
            LookupEvent ev = new LookupEvent (this);
            AbstractLookup.notifyListeners(listeners, ev);
        }
        
    }  // end of ProxyResult
}
