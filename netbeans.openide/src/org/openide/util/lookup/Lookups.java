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

import java.util.Collections;
import java.util.Arrays;

import org.openide.util.Lookup;

/**
 * A convinience class with couple of static factory methods. It is impossible
 * to create an instance of this class.
 * 
 * @author David Strupl
 * @since 2.21
 */
public class Lookups {
    
    /** Noone should ever create intstances of this class. */
    private Lookups() {
    }
    
    /**
     * Creates a singleton lookup. It means lookup that contains only
     * one object specified via the supplied parameter. The lookup will
     * either return the object or null if the supplied template does
     * not match the class. If the specified argument is null the method
     * will end with NullPointerException.
     * @return Fully initialized lookup object ready to use
     * @throws NullPointerException if the supplied argument is null
     * @since 2.21
     */
    public static Lookup singleton(Object objectToLookup) {
        if (objectToLookup == null) {
            throw new NullPointerException();
        }
        // performance of the resulting lookup might be further
        // improved by providing specialized singleton result (and lookup)
        // instead of using SimpleResult
        return new SimpleLookup(Collections.singleton(objectToLookup));
    }
    
    /**
     * Creates a lookup that contains an array of objects specified via the
     * parameter. The resulting lookup is fixed in the following sense: it
     * contains only fixed set of objects passed in by the array parameter.
     * Its contents never changes so registering listeners on such lookup
     * does not have any observable effect (the listeners are never called).
     *
     * @return Fully initialized lookup object ready to use
     * @throws NullPointerException if the supplied argument is null
     * @since 2.21
     * 
     */
    public static Lookup fixed(Object[] objectsToLookup) {
        if (objectsToLookup == null) {
            throw new NullPointerException();
        }
        return new SimpleLookup(Arrays.asList(objectsToLookup));
    }
    
    /**
     * Creates a lookup that contains an array of objects specified via the
     * parameter. The resulting lookup is fixed in the following sense: it
     * contains only fixed set of objects passed in by the array parameter.
     * The objects returned from this lookup are converted to real objects
     * before they are returned by the lookup.
     * Its contents never changes so registering listeners on such lookup
     * does not have any observable effect (the listeners are never called).
     *
     * @return Fully initialized lookup object ready to use
     * @throws NullPointerException if the any of the arguments is null
     * @since 2.21
     * 
     */
    public static Lookup fixed(Object[] keys, InstanceContent.Convertor convertor) {
        if (keys == null) {
            throw new NullPointerException();
        }
        if    (convertor == null) {
            throw new NullPointerException();
        }
            
        return new SimpleLookup(Arrays.asList(keys), convertor);
    }
    
     /** Creates a lookup that delegates to another one but that one can change
      * from time to time. The returned lookup checks every time somebody calls
      * <code>lookup</code> or <code>lookupItem</code> method whether the 
      * provider still returns the same lookup. If not, it updates state of 
      * all <code>Lookup.Result</code>s that it created (and that still exists).
      * <P>
      * The user of this method has to implement its provider's <code>getLookup</code>
      * method (must be thread safe and fast, will be called often and from any thread)
      * pass it to this method and use the returned lookup. Whenever the user
      * changes the return value from the <code>getLookup</code> method and wants
      * to notify listeners on the lookup about that it should trigger the event 
      * firing, for example by calling <code>lookup.lookup (Object.class)</code>
      * that forces check of the return value of <code>getLookup</code>.
      *
      * @param provider the provider that returns a lookup to delegate to
      * @return lookup delegating to the lookup returned by the provider
      */
     public static Lookup proxy (Lookup.Provider provider) {
         return new SimpleProxyLookup (provider);
     }
     
    /** Returns a lookup that implements the JDK1.3 JAR services mechanism and delegates
     * to META-INF/services/name.of.class files.
     * <p>Note: It is not dynamic - so if you need to change the classloader or JARs,
     * wrap it in a ProxyLookup and change the delegate when necessary.
     * Existing instances will be kept if the implementation classes are unchanged,
     * so there is "stability" in doing this provided some parent loaders are the same
     * as the previous ones.
     * @since 3.35
     */
     public static Lookup metaInfServices(ClassLoader classLoader) {
        return new MetaInfServicesLookup(classLoader);
     }
}
