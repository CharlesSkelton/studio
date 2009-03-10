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

import org.openide.util.Lookup;
import org.openide.util.LookupListener;

/**
 * Simple lookup implementation. It can be used to create temporary lookups
 * that do not change over time. The result stores references to all objects
 * passed in the constructor. Those objecst are the only ones returned as
 * result.
 * @author David Strupl
 */
class SimpleLookup extends org.openide.util.Lookup {

    /** This variable is initialized in constructor and thus null
     * value is not allowed as its value. */
    private Collection allItems;
    
    /** 
     * Creates new Result object with supplied instances parameter.
     * @param instances to be used to return from the lookup
     */
    SimpleLookup(Collection instances) {
        allItems = new ArrayList(instances.size());
        for (Iterator i = instances.iterator(); i.hasNext(); ) {
            allItems.add(new InstanceContent.SimpleItem(i.next()));
        }
    }
    
    SimpleLookup(Collection keys, InstanceContent.Convertor conv) {
        allItems = new ArrayList(keys.size());
        for (Iterator i = keys.iterator(); i.hasNext(); ) {
            allItems.add(new InstanceContent.ConvertingItem(i.next(), conv));
        }
    }
    
    public String toString() {
        return "SimpleLookup" + lookup(new Template(Object.class)).allInstances();
    }
    
    public Result lookup(Template template) {
        if (template == null) {
            throw new NullPointerException();
        }
        return new SimpleResult(template);
    }
    
    public Object lookup(Class clazz) {
        // this can be further tuned to better performance
        Lookup.Item item = lookupItem (new Lookup.Template (clazz));
        return item == null ? null : item.getInstance ();
    }
    
    /** A method that defines matching between Item and Template.
     * @param item the item to match
     * @return true if item matches the template requirements, false if not
     */
    private static boolean matches (Template t, AbstractLookup.Pair item) {
        if ( !AbstractLookup.matches(t, item)) {
            return false;
        }
        
        Class type = t.getType();

        if (type != null && !type.isAssignableFrom(item.getType())) {
            return false;
        }
        
        return true;
    }        

    
    /**
     * Result used in SimpleLookup. It holds a reference to the collection
     * passed in constructor. As the contents of this lookup result never
     * changes the addLookupListener and removeLookupListener are empty.
     */
    private class SimpleResult extends Lookup.Result {
        /** can be null and is initialized lazily */
        private Set classes;
        /** can be null and is initialized lazily */
        private Collection items;
        /** Template used for this result. It is never null.*/
        private Template template;
        /** can be null and is initialized lazily */
        private Collection results;
        
        /** Just remembers the supplied argument in variable template.*/
        SimpleResult(Template template) {
            this.template = template;
        }
        
        /**
         * Intentionally does nothing because the lookup does not change
         * and no notification is needed.
         */
        public void addLookupListener(LookupListener l) {
        }
        
        /**
         * Intentionally does nothing because the lookup does not change
         * and no notification is needed.
         */
        public void removeLookupListener(LookupListener l) {
        }
        
        /**
         * Lazy initializes the results collection. Uses a call to allItems
         * to obtain the instances.
         */
        public java.util.Collection allInstances() {
            synchronized (this) {
                if (results != null) {
                    return results;
                }
            }
            Collection res = new ArrayList(allItems.size());
            for (Iterator i = allItems().iterator(); i.hasNext(); ) {
                res.add(((Lookup.Item)i.next()).getInstance());
            }
            synchronized (this) {
                results = Collections.unmodifiableCollection(res);
            }
            return results;
        }
        
        /**
         * Lazy initializes variable classes. Uses a call to allItems to
         * compute the result.
         */
        public Set allClasses () {
            synchronized (this) {
                if (classes != null) {
                    return classes;
                }
            }
            Set res = new TreeSet();
            for (Iterator i = allItems().iterator(); i.hasNext(); ) {
                res.add(((Lookup.Item)i.next()).getType());
            }
            synchronized (this) {
                classes = Collections.unmodifiableSet(res);
            }
            return classes;
        }
        
        /**
         * Lazy initializes variable items. Creates an item for each
         * element in the instances collection. It puts either SimpleItem
         * or ConvertingItem to the collection.
         */
        public Collection allItems () {
            synchronized (this) {
                if (items != null) {
                    return items;
                }
            }
            Collection res = new ArrayList(allItems.size());
            for (Iterator i = allItems.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o instanceof AbstractLookup.Pair) {
                    if (matches(template, (AbstractLookup.Pair)o)) {
                        res.add(o);
                    }
                }
            }
            synchronized (this) {
                items = Collections.unmodifiableCollection(res);
            }
            return items;
        }
    }
}
