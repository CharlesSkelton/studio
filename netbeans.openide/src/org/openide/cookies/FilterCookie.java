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

package org.openide.cookies;

import org.openide.nodes.Node;

/** Cookie for node groups which can somehow be filtered.
 * This would be applied to a subclass of {@link org.openide.nodes.Children Children}.
* @deprecated Use Looks instead.
* @author Jaroslav Tulach, Jan Jancura, Dafe Simonek
*/
public interface FilterCookie extends Node.Cookie {

    /** Get the declared filter (super-)class.
     * @return the class, or may be <code>null</code> if no filter is currently in use
    */
    public Class getFilterClass ();

    /** Get the current filter.
     * @return the filter, or <code>null</code> if none is currently in use
    */
    public Object getFilter ();

    /** Set the current filter.
    * @param filter the filter, or <code>null</code> if none should be used
    */
    public void setFilter (Object filter);

}
