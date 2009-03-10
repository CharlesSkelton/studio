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

package org.openide.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Convert between files and URLs under JDK 1.4.
 * @author Jesse Glick
 * @see "#29711"
 */
class FileURLConvertor14 implements Utilities.FileURLConvertor {
    
    public FileURLConvertor14() {}
    
    public URL toURL(File f) throws MalformedURLException {
        URI uri = f.toURI();
        return uri.toURL();
    }
    
    public File toFile(URL u) {
        try {
            URI uri = new URI(u.toExternalForm());
            return new File(uri);
        } catch (URISyntaxException use) {
            // malformed URL
            return null;
        } catch (IllegalArgumentException iae) {
            // not a file: URL
            return null;
        }
    }
    
}
