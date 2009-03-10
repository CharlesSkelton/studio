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

package org.openide.loaders;

import org.openide.nodes.Node;
import org.openide.util.Lookup;

/** Provisional mechanism for displaying the Repository object.
 * It will show all filesystems, possibly with a filter.
 * @author Jesse Glick
 * @since 3.14
 */
public abstract class RepositoryNodeFactory {

    /** Get the default factory.
     * @return the default instance from lookup
     */
    public static RepositoryNodeFactory getDefault() {
        return (RepositoryNodeFactory)Lookup.getDefault().lookup(RepositoryNodeFactory.class);
    }

    /** Subclass constructor. */
    protected RepositoryNodeFactory() {}
    
    /** Create a node representing a subset of the repository of filesystems.
     * You may filter out certain data objects.
     * If you do not wish to filter out anything, just use {@link DataFilter#ALL}.
     * Nodes might be reused between calls, so if you plan to add this node to a
     * parent, clone it first.
     * @param f a filter
     * @return a node showing part of the repository
     */
    public abstract Node repository(DataFilter f);

}
