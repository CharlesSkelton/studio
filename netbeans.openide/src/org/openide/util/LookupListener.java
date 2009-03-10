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

package org.openide.util;

import java.util.*;

/** General listener for changes in lookup.
 *
 * @author  Jaroslav Tulach
 */
public interface LookupListener extends EventListener {
    /** A change in lookup occured. Please note that this method
     * should never block since it might be called from lookup implementation
     * internal threads. If you block here you are in risk that the thread
     * you wait for might in turn to wait for the lookup internal thread to 
     * finish its work.
     * @param ev event describing the change
     */
    public void resultChanged (LookupEvent ev);
}
