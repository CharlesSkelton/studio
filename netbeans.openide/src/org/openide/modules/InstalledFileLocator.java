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

package org.openide.modules;

import java.io.File;
import java.util.Iterator;
import org.openide.util.Lookup;

/**
 * Service providing the ability to locate a module-installed file in
 * the NetBeans application's installation.
 * Zero or more instances may be registered to lookup.
 * @author Jesse Glick
 * @since 3.21
 * @see "#28683"
 */
public abstract class InstalledFileLocator {
    
    /**
     * No-op constructor for use by subclasses.
     */
    protected InstalledFileLocator() {}
    
    /**
     * Try to locate a file.
     * @param relativePath path from install root, e.g. <samp>docs/OpenAPIs.zip</samp>
     *                     (always using <samp>/</samp> as a separator, regardless of platform)
     * @param codeNameBase name of the supplying module, e.g. <samp>org.netbeans.modules.foo</samp>;
                           may be <code>null</code> if unknown
     * @param localized true to perform a localized and branded lookup (useful for documentation etc.)
     * @return the requested File, if it can be found, else <code>null</code>
     */
    public abstract File locate(String relativePath, String codeNameBase, boolean localized);
    
    /**
     * Get a master locator.
     * Lookup is searched for all registered locators.
     * They are merged together and called in sequence
     * until one of them is able to service a request.
     * If you use this call, require the token <code>org.openide.modules.InstalledFileLocator</code>
     * to require any autoload modules which can provide locators.
     * @return a master merging locator (never null)
     */
    public static InstalledFileLocator getDefault() {
        return new InstalledFileLocator() {
            public File locate(String rp, String cnb, boolean l) {
                Iterator it = Lookup.getDefault().lookup(new Lookup.Template(InstalledFileLocator.class)).
                    allInstances().iterator();
                while (it.hasNext()) {
                    File f = ((InstalledFileLocator)it.next()).locate(rp, cnb, l);
                    if (f != null) {
                        return f;
                    }
                }
                return null;
            }
        };
    }
    
}
