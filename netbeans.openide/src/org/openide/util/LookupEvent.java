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

/** An event describing the change in the lookup's result.
 *
 * @author  Jaroslav Tulach
 */
public final class LookupEvent extends EventObject {
    /** Create a new lookup event.
     * @param source the lookup result which has changed
     */
    public LookupEvent (Lookup.Result source) {
        super (source);
    }
}
