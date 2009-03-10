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

/** Listener to changes in annotation of file objects.
* The annotation can be obtained by a call to FileSystem.getStatus ().annotateName (...)
* or FileSystem.getStatus ().annotateIcon (...)
*
* @author Jaroslav Tulach
*/
public interface FileStatusListener extends java.util.EventListener {
    /** Notifies listener about change in annotataion of a few files.
    * @param ev event describing the change
    */
    public void annotationChanged (FileStatusEvent ev);
}
