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

package org.openide.xml;

import java.io.*;
import java.util.*;

import javax.swing.event.*;

import org.xml.sax.*;

import org.openide.util.*;

/**
 * Entity resolver resolving all entities registered by modules.
 * Use {@link #getDefault} to get the master instance in system. Any parser working with
 * unknown XML documents should use it to avoid unnecessary Internet
 * connections.
 *
 * <p>You can register your own instances via lookup to add to the resolver pool,
 * for example using an XML file in the format defined by {@link #PUBLIC_ID},
 * but for reasons of performance and predictability during startup it is best to provide
 * the entity (e.g. some DTD you define) as the contents of a file in
 * the system filesystem, in the <samp>/xml/entities/</samp> folder, where the file path
 * beneath this folder is based on the public ID as follows:
 * <ol>
 * <li>US-ASCII alphanumeric characters and '_' are left as is.
 * <li>Spaces and various punctuation are converted to '_' (one per character).
 * <li>Initial '-//' is dropped.
 * <li>Final '//EN' is dropped.
 * <li>Exactly two forward slashes in a row are converted to one.
 * </ol>
 * Thus for example the public ID <samp>-//NetBeans//Entity&nbsp;Mapping&nbsp;Registration&nbsp;1.0//EN</samp>
 * would be looked for in the file <samp>/xml/entities/NetBeans/Entity_Mapping_Registration_1_0</samp>.
 * Naturally this only works if you are defining a fixed number of entities.
 * <p>It is recommended that the entity file in <samp>/xml/entities/</samp> also be given a file
 * attribute named <code>hint.originalPublicID</code> with a string value giving the public ID.
 *
 * @author  Petr Kuzel
 * @since   release 3.2
 */
public abstract class EntityCatalog implements EntityResolver {
    /** 
     * DOCTYPE public ID defining grammar used for entity registrations.
     */
    public static final String PUBLIC_ID = "-//NetBeans//Entity Mapping Registration 1.0//EN"; // NOI18N

    private static EntityCatalog instance = new Forwarder ();

    /** Get a master entity catalog which can delegate to any others that have
     * been registered via lookup.
     */
    public static EntityCatalog getDefault() {
        return instance;
    }

    /**
     * This catalog is forwarding implementation.
     */
    private static class Forwarder extends EntityCatalog {
        private Lookup.Result result;
        
	Forwarder() {}
	
        public InputSource resolveEntity(String publicID, String systemID) throws IOException, SAXException {
            if (result == null) {
                Lookup.Template temp = new Lookup.Template (EntityCatalog.class);
                result = Lookup.getDefault ().lookup (temp);
            }
            
            Iterator it = result.allInstances().iterator ();
            while (it.hasNext ()) {
                // using resolver's method because EntityCatalog extends EntityResolver
                EntityCatalog res = (EntityCatalog)it.next ();
                InputSource is = res.resolveEntity (publicID, systemID);
                if (is != null) {
                    return is;
                }
            }
            return null;
        }
        
    }
}
