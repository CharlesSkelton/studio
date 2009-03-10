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

package org.openide;

import org.openide.util.Lookup;

/** Manages major aspects of the NetBeans lifecycle - currently saving all objects and exiting.
 * @author Jesse Glick
 * @since 3.14
 */
public abstract class LifecycleManager {

    /** Get the default lifecycle manager.
     * @return the default instance from lookup
     */
    public static LifecycleManager getDefault() {
        return (LifecycleManager)Lookup.getDefault().lookup(LifecycleManager.class);
    }

    /** Subclass constructor. */
    protected LifecycleManager() {}

    /** Save all opened objects.
     */
    public abstract void saveAll();

    /** Exit NetBeans.
     * This method will return only if {@link java.lang.System#exit} fails, or if at least one component of the
     * system refuses to exit (because it cannot be properly shut down).
     */
    public abstract void exit();

}
