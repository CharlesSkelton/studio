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

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.w3c.dom.*;

import org.openide.*;
import org.openide.xml.*;
import org.openide.filesystems.*;

/**
 *
 * Class that hide implementations details of deprecated utility
 * methods provided at XMLDataObject.
 *
 * @author  Petr Kuzel
 * @version 1.0
 */
class XMLDataObjectImpl extends Object {


    /** Create DOM builder using JAXP libraries. */
    static DocumentBuilder makeBuilder(boolean validate) throws IOException, SAXException {
        
        DocumentBuilder builder;
        DocumentBuilderFactory factory;

        //create factory according to javax.xml.parsers.SAXParserFactory property 
        //or platform default (i.e. com.sun...)
        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validate);
            factory.setNamespaceAware(false);
        } catch (FactoryConfigurationError err) {
            notifyFactoryErr(err, "javax.xml.parsers.DocumentBuilderFactory"); //NOI18N
            throw err;
        }

        try {
            builder = factory.newDocumentBuilder();                
        } catch (ParserConfigurationException ex) {
            SAXException sex = new SAXException("Configuration exception."); // NOI18N
            ErrorManager emgr = ErrorManager.getDefault();
            emgr.annotate(sex, ex);
            emgr.annotate(sex, "Can not create a DOM builder!\nCheck javax.xml.parsers.DocumentBuilderFactory property and the builder library presence on classpath."); // NOI18N
            throw sex;
        }
        
        return builder;
    }
    

    private static SAXParserFactory makeParserFactory(boolean validating, boolean ns) {
        SAXParserFactory factory = null;

        //create factory according to javax.xml.parsers.SAXParserFactory property 
        //or platform default (i.e. com.sun...)
        try {
            factory = SAXParserFactory.newInstance();
            factory.setValidating(validating);
            factory.setNamespaceAware(ns);
            
        } catch (FactoryConfigurationError err) {
            notifyFactoryErr(err, "javax.xml.parsers.SAXParserFactory"); //NOI18N
            throw err;
        }
        
        return factory;
    }
    
    static Parser makeParser(boolean validate) {
        
        SAXParserFactory factory = makeParserFactory(validate, false); 

        try {
            return factory.newSAXParser().getParser();
        } catch (ParserConfigurationException ex) {
            notifyNewSAXParserEx(ex);
            return null;
        } catch (SAXException ex) {
            notifyNewSAXParserEx(ex);
            return null;
        }
        
    }

    /** Return XML reader or null if no provider exists. */
    static XMLReader makeXMLReader(boolean validating, boolean namespaces) {

        SAXParserFactory factory = makeParserFactory(validating, namespaces); 

        try {
            return factory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException ex) {
            notifyNewSAXParserEx(ex);
            return null;
        } catch (SAXException ex) {
            notifyNewSAXParserEx(ex);
            return null;
        }
        
    }
    
    /** Annotate & notify the exception. */
    private static void notifyNewSAXParserEx (Exception ex) {
        ErrorManager emgr = ErrorManager.getDefault();
        emgr.annotate(ex, "Can not create a SAX parser!\nCheck javax.xml.parsers.SAXParserFactory property features and the parser library presence on classpath."); // NOI18N
        emgr.notify(ex);
    }

    /** Annotate & notify the error. */
    private static void notifyFactoryErr(Error err, String property) {
        ErrorManager emgr = ErrorManager.getDefault();
        emgr.annotate(err, "Can not create a factory!\nCheck " + property + "  property and the factory library presence on classpath."); // NOI18N
        emgr.notify(err);
    }

    /** Annotate & notify the error. */
    private static void notifyException(Throwable err) {
        ErrorManager emgr = ErrorManager.getDefault();        
        emgr.notify(emgr.INFORMATIONAL, err);
    }

    private static EntityCatalog runtimeCatalogInstance = null;
    
    static synchronized EntityCatalog createEntityCatalog() {
        if (runtimeCatalogInstance == null) {
            runtimeCatalogInstance = new RuntimeCatalog();
        }
        return runtimeCatalogInstance;
    }
    

    // warning back compatability code!!!    
    static synchronized void registerCatalogEntry(String publicId, String uri) {
        
        ((RuntimeCatalog)createEntityCatalog()).registerCatalogEntry(publicId, uri);

/*        
        // EntityCatalog grammar names
        final String _URI = "uri";
        final String _PUBLIC = "public";
        final String _PUBLIC_ID = "publicId";
        
        // put it at XMLayer
        try {
            
            final String NAME = "org-openide-loaders-XMLDataObject-catalog";
            final String EXT = "xml";

            FileObject services = Repository.getDefault().
                getDefaultFileSystem().findResource("Services/Hidden");

            if (services == null) {
                //XMLayer not initialized yet
                throw new Error("#897 DefaultFileSystem not initialized yet.");
            }
                        
            FileObject peer = services.getFileObject(NAME, EXT);
            if (peer == null) {
                
                peer = services.createData(NAME, EXT);
                Document doc = XMLUtil.createDocument(
                    "catalog", null,
                    EntityCatalog.PUBLIC_ID,
                    "http://www.netbeans.org/dtds/EntityCatalog-1_0.dtd"
                );
                FileLock lock = null;
                try { 
                    lock = peer.lock();
                    OutputStream out = peer.getOutputStream(lock);
                    XMLUtil.write(doc, out, "UTF-8");
                } finally {
                    if (lock != null) lock.releaseLock();
                }
            }
            
            InputSource in = new InputSource(peer.getInputStream());
            in.setSystemId(peer.getURL().toExternalForm());

            EntityResolver resolver = new EntityResolver() {
                public InputSource resolveEntity(String pid, String sid) {
                    if ( EntityCatalog.PUBLIC_ID.equals(pid) ) {
                        return new InputSource("nbres:/org/openide/xml/EntityCatalog.dtd");
                    }
                    return null;
                }
            };
            
            Document doc = 
                XMLUtil.parse(in, true, false, new XMLDataObject.ErrorPrinter(), resolver);
            
            boolean match = false;
            Element root = doc.getDocumentElement();
            NodeList list = root.getElementsByTagName(_PUBLIC);
            for (int i = 0; i<list.getLength(); i++) {
                Element next = (Element) list.item(i);                
                String key = next.getAttributeNode(_PUBLIC_ID).getValue();
                
                if (publicId.equals(key)) {
                    if (uri != null) {
                        next.getAttributeNode(_URI).setValue(uri);
                    } else {
                        root.removeChild(next);
                    }             
                    match = true;
                }
            }
            
            if (match == false) {
                // no current registration matched

                Element newRegistration = doc.createElement(_PUBLIC);
                Attr pubAttr = doc.createAttribute(_PUBLIC_ID);
                pubAttr.setValue(publicId);
                Attr uriAttr = doc.createAttribute(_URI);
                uriAttr.setValue(uri);            

                newRegistration.setAttributeNode(pubAttr);
                newRegistration.setAttributeNode(uriAttr);

                root.appendChild(newRegistration);
            }

            FileLock lock = null;
            try { 
                lock = peer.lock();
                OutputStream out = peer.getOutputStream(lock);
                XMLUtil.write(doc, out, "UTF-8");
            } finally {
                if (lock != null) lock.releaseLock();
            }
                        
        } catch (IOException ex) {
            notifyException(ex);
        } catch (SAXException ex) {  
            notifyException(ex);
        } catch (DOMException ex) {
            notifyException(ex);
        }    
 */
    }
    
    /** 
     * Implements non-persistent catalog functionality as EntityResolver.
     * <p>Registations using this resolver are:
     * <li>transient
     * <li>of the hihgest priority
     * <li>last registration prevails     
     * @version com.sun.xml.parser.Resolver based
     */
    static final class RuntimeCatalog extends EntityCatalog {    
        // table mapping public IDs to (local) URIs
        private Hashtable id2uri;

        // tables mapping public IDs to resources and classloaders
        private Hashtable id2resource;
        private Hashtable id2loader;
        
        /** SAX entity resolver */
        public InputSource resolveEntity (String name, String uri) throws IOException, SAXException {    	    
            
            InputSource retval;        
            String mappedURI = name2uri(name);
            InputStream	stream  = mapResource(name);

            // prefer explicit URI mappings, then bundled resources...
            if (mappedURI != null) {
                retval = new InputSource(mappedURI);
                retval.setPublicId(name);
                return retval;
                
            } else if (stream != null) {
                uri = "java:resource:" + (String) id2resource.get(name); // NOI18N
                retval = new InputSource(stream);
                retval.setPublicId(name);
                return retval;

            } else {
                return null;
            }
        }

        public void registerCatalogEntry (String publicId, String uri) {
            if (id2uri == null)
                id2uri = new Hashtable(17);
            id2uri.put(publicId, uri);
        }

        /** Map publicid to a resource accessible by a classloader. */
        public void registerCatalogEntry (String publicId, String resourceName, ClassLoader loader) {
            if (id2resource == null)
                id2resource = new Hashtable(17);
            id2resource.put(publicId, resourceName);

            if (loader != null) {
                if (id2loader == null)
                    id2loader = new Hashtable(17);
                id2loader.put(publicId, loader);
            }
        }
        
        // maps the public ID to an alternate URI, if one is registered
        private String name2uri (String publicId) {
            
            if (publicId == null || id2uri == null)
                return null;
            return (String) id2uri.get(publicId);
        }
        
        
        // return the resource as a stream
        private InputStream mapResource (String publicId)
        {
            if (publicId == null || id2resource == null)
                return null;

            String resourceName = (String) id2resource.get(publicId);
            ClassLoader	loader = null;

            if (resourceName == null)
                return null;
    
            if (id2loader != null)
                loader = (ClassLoader) id2loader.get(publicId);
    
            if (loader == null)
                return ClassLoader.getSystemResourceAsStream(resourceName);
            return loader.getResourceAsStream(resourceName);
        }               
    }
    
}
