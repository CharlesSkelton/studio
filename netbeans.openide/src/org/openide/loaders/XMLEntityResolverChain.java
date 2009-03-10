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

import org.xml.sax.*;

/**
 * A utility class allowing to chain multiple EntityResolvers into one delegating
 * resolution process on them.
 * <p>
 * If all registered resolves are thread-safe then the implementation is thread-safe too.
 *
 * @author  Petr Kuzel
 * @version 1.0
 */
final class XMLEntityResolverChain implements EntityResolver {

    /**
     * Chain of resolvers contaning all EntityResolvers registred by a user.
     */
    private final List resolverChain = new ArrayList(3);
    
    
    /** Creates new EntityResolverChain instance */
    public XMLEntityResolverChain() {
    }
    
    /**
     * Add a given entity resolver to the resolver chain.
     * The resolver chain is then searched by resolveEntity() method
     * until some registered resolver succed.
     *
     * @param resolver - a non null resolver to be added
     *
     * @return true if successfully added
     */
    public boolean addEntityResolver(EntityResolver resolver) {
        if (resolver == null) throw new IllegalArgumentException();
        synchronized (resolverChain) {
            if (resolverChain.contains(resolver)) return false;
            return resolverChain.add(resolver);
        }
    }

    /**
     * Remove a given entity resolver from the entity resolver chain.
     *
     * @param resolver - a non null resolver to be removed
     *
     * @return removed resolver instance or null if not present
     */    
    public EntityResolver removeEntityResolver(EntityResolver resolver) {
        if (resolver == null) throw new IllegalArgumentException();
        synchronized (resolverChain) {
            int index = resolverChain.indexOf(resolver);
            if ( index < 0 ) return null;
            return (EntityResolver) resolverChain.remove(index);
        }
    }

    /**
     * Chaining resolveEntity() implementation iterating over registered resolvers.
     *
     * @param publicID - The public identifier of the external entity being referenced, 
     *        or null if none was supplied.
     * @param systemID - The system identifier of the external entity being referenced.
     *
     * @throw SAXException - Any SAX exception, possibly wrapping another exception. 
     * @throw IOException - A Java-specific IO exception, possibly the result of creating 
     *        a new InputStream or Reader for the InputSource.
     *
     * @return An InputSource object describing the new input source, 
     *         or null to request that the parser open a regular URI connection to the system identifier.
     */
    public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
        
        // user's resolver chain
        SAXException lsex = null;
        IOException lioex = null;
        
        synchronized (resolverChain) {
            Iterator it = resolverChain.iterator();
            while (it.hasNext()) {
                EntityResolver resolver = (EntityResolver) it.next();
                try {
                    InputSource test = resolver.resolveEntity(publicID, systemID);
                    if (test == null) continue;
                    return test;
                } catch (SAXException sex) {
                    lsex = sex;
                    continue;
                } catch (IOException ioex) {
                    lioex = ioex;
                    continue;
                }
            }
            
            //retain the last exception for diagnostics purposes
            
            if (lioex != null) throw lioex;  
            if (lsex != null) throw lsex;
            
            //just can not resolve it            
            return null;  
        }
    }        
}
