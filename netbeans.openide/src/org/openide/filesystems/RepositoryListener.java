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

package org.openide.filesystems;

/** Listener to changes in the filesystem pool.
*
* @author Jaroslav Tulach
* @version 0.10 November 4, 1997
*/
public interface RepositoryListener extends java.util.EventListener {
    /** Called when new filesystem is added to the pool.
    * @param ev event describing the action
    */
    public void fileSystemAdded (RepositoryEvent ev);

    /** Called when a filesystem is removed from the pool.
    * @param ev event describing the action
    */
    public void fileSystemRemoved (RepositoryEvent ev);

    /** Called when a filesystem pool is reordered. */
    public void fileSystemPoolReordered(RepositoryReorderedEvent ev);
}
