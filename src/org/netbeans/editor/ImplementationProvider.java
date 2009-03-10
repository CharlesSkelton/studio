/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.editor;

import javax.swing.*;
import java.util.ResourceBundle;

/** This is provider of implementation. This package (org.netbeans.editor) 
 * represent editor core which can be used independently on the rest of NetBeans.
 * However this core needs access to higher level functionality like access
 * to localized bundles, access to settings storage, etc. which can be implemented
 * differently by the applications which uses this editor core. For this purpose
 * was created this abstract class and it can be extended with any other methods which
 * are more and more often required by core editor. Example implementation
 * of this provider can be found in org.netbeans.modules.editor package
 * 
 * @author David Konecny
 * @since 10/2001
 */

abstract public class ImplementationProvider {

    private static ImplementationProvider provider = null;

    /** Returns currently registered provider */
    public static ImplementationProvider getDefault() {
        return provider;
    }

    /** Register your own provider through this method */
    public static void registerDefault(ImplementationProvider prov) {
        provider = prov;
    }

    /** Return ResourceBundle for the given class.*/
    abstract public ResourceBundle getResourceBundle(String localizer);

    /** This is temporary method which allows core editor to access
     * toggle breakpoint action. This action is then used when user clicks
     * on glyph gutter. In next version this should be removed and redesigned
     * as suggested in issue #16762 */
    abstract public Action getToggleBreakpointAction();
    
}
