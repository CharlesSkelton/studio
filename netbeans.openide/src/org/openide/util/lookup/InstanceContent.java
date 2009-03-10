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

package org.openide.util.lookup;

import java.util.*;
import java.lang.ref.WeakReference;


import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.AbstractLookup.Pair;

/** A special content implementation that can be passed to AbstractLookup
 * and provides methods for registration of instances and lazy instances.
 * <PRE>
 *      InstanceContent ic = new InstanceContent ();
 *      AbstractLookup al = new AbstractLookup (ic);
 *      
 *      ic.add (new Object ());
 *      ic.add (new Dimension (...));
 *
 *      Dimension theDim = (Dimension)al.lookup (Dimension.class);
 * </PRE>
 *
 * @author  Jaroslav Tulach
 *
 * @since 1.25
 */
public final class InstanceContent extends AbstractLookup.Content {
    /** The method to add instance to the lookup with.
     * @param inst instance 
     */
    public final void add (Object inst) {
        addPair(new SimpleItem (inst));
    }
    
    /** The method to add instance to the lookup with.
     * @param inst instance
     * @param conv convertor which postponing an instantiation,
     * if <code>conv==null</code> then the instance is registered directly.
     */
    public final void add (Object inst, Convertor conv) {
        addPair(new ConvertingItem(inst, conv));
    }
    
    /** Remove instance.
     * @param inst instance
     */
    public final void remove (Object inst) {
        removePair (new SimpleItem (inst));
    }
    
    /** Remove instance added with a convertor.
     * @param inst instance
     * @param conv convertor, if <code>conv==null</code> it is same like
     * remove(Object)
     */
    public final void remove (Object inst, Convertor conv) {
        removePair (new ConvertingItem (inst, conv));
    }
    
    /** Changes all pairs in the lookup to new values. Converts collection of
     * instances to collection of pairs.
     * @param col the collection of (Item) objects
     * @param conv the convertor to use or null
     */
    public final void set (Collection col, Convertor conv) {
        ArrayList l = new ArrayList(col.size());
        Iterator it = col.iterator();
        if (conv == null) {
            while (it.hasNext()) {
                l.add (new SimpleItem (it.next()));
            }
        } else {
            while (it.hasNext()) {
                l.add (new ConvertingItem (it.next(), conv));
            }
        }
        setPairs (l);
    }
    
    
    /** Instance of one item representing an object.
     */
    final static class SimpleItem extends Pair {
        private Object obj;
        
        /** Create an item.
         * @obj object to register
         */
        public SimpleItem (Object obj) {
            if (obj == null) throw new NullPointerException();
            this.obj = obj;
        }
        
        /** Tests whether this item can produce object
         * of class c.
         */
        public boolean instanceOf(Class c) {
            return c.isInstance (obj);
        }
        
        /** Get instance of registered object. If convertor is specified then
         *  method InstanceLookup.Convertor.convertor is used and weak reference
         * to converted object is saved.
         * @return the instance of the object.
         */
        public Object getInstance() {
            return obj;
        }
        
        public boolean equals (Object o) {
            if (o instanceof SimpleItem) {
                return obj.equals(((SimpleItem) o).obj);
            } else {
                return false;
            }
        }
        
        public int hashCode () {
            return obj.hashCode ();
        }
        
        /** An identity of the item.
         * @return string representing the item, that can be used for
         *   persistance purposes to locate the same item next time
         */
        public String getId() {
            return "IL[" + obj.toString (); // NOI18N
        }
        
        /** Getter for display name of the item.
         */
        public String getDisplayName () {
            return obj.toString ();
        }
        
        
        /** Method that can test whether an instance of a class has been created
         * by this item.
         *
         * @param obj the instance
         * @return if the item has already create an instance and it is the same
         *  as obj.
         */
        protected boolean creatorOf(Object obj) {
            return obj == this.obj;
        }
        
        /** The class of this item.
         * @return the correct class
         */
        public Class getType() {
            return obj.getClass ();
        }
        
    } // end of SimpleItem

    /** Instance of one item registered in the map.
     */
    final static class ConvertingItem extends Pair {
        /** registered object */
        private Object obj;
        /** Reference to converted object. */
        private WeakReference ref;
        /** convertor to use */
        private Convertor conv;
        
        /** Create an item.
         * @obj object to register
         * @conv a convertor, can be <code>null</code>.
         */
        public ConvertingItem(Object obj, Convertor conv) {
            this.obj = obj;
            this.conv = conv;
        }
        
        /** Tests whether this item can produce object
         * of class c.
         */
        public boolean instanceOf(Class c) {
            return c.isAssignableFrom (getType ());
        }
        
        /** Returns converted object or null if obj has not been converted yet
         * or reference was cleared by garbage collector.
         */
        private Object getConverted() {
            if (ref == null) return null;
            return ref.get();
        }
        
        /** Get instance of registered object. If convertor is specified then
         *  method InstanceLookup.Convertor.convertor is used and weak reference
         * to converted object is saved.
         * @return the instance of the object.
         */
        public synchronized Object getInstance() {
            Object converted = getConverted();
            if (converted == null) {
                converted = conv.convert(obj);
                ref = new WeakReference(converted);
            }
            return converted;
        }
        
        public boolean equals (Object o) {
            if (o instanceof ConvertingItem) {
                return obj.equals(((ConvertingItem) o).obj);
            } else {
                return false;
            }
        }
        
        public int hashCode () {
            return obj.hashCode ();
        }
        
        /** An identity of the item.
         * @return string representing the item, that can be used for
         *   persistance purposes to locate the same item next time
         */
        public String getId() {
            return conv.id (obj);
        }
        
        /** Getter for display name of the item.
         */
        public String getDisplayName () {
            return conv.displayName (obj);
        }
        
        /** Method that can test whether an instance of a class has been created
         * by this item.
         *
         * @param obj the instance
         * @return if the item has already create an instance and it is the same
         *  as obj.
         */
        protected boolean creatorOf(Object obj) {
            if (conv == null) {
                return obj == this.obj;
            } else {
                return obj == getConverted ();
            }
        }
        
        /** The class of this item.
         * @return the correct class
         */
        public Class getType() {
            if (conv == null) {
                return obj.getClass ();
            }
            
            Object converted = getConverted();
            if (converted == null) return conv.type(obj);
            return converted.getClass ();
        }
        
    } // end of ConvertingItem
    
    /** Convertor postpones an instantiation of an object. 
     * @since 1.25
     */
    public static interface Convertor {
        /** Convert obj to other object. There is no need to implement
         * cache mechanism. It is provided by InstanceLookup.Item.getInstance().
         * Method should be called more than once because Lookup holds
         * just weak reference.
         *
         * @param obj the registered object
         * @return the object converted from this object
         */
        public Object convert (Object obj);
        
        
        /** Return type of converted object. 
         * @param obj the registered object
         * @return the class that will be produced from this object (class or 
         *      superclass of convert (obj))
         */
        public Class type (Object obj);
        
        /** Computes the ID of the resulted object.
         * @param obj the registered object
         * @return the ID for the object 
         */
        public String id (Object obj);
        
        /** The human presentable name for the object.
         * @param obj the registered object
         * @return the name representing the object for the user
         */
        public String displayName (Object obj);
    }
}
