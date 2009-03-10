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

/** Listener for changes in <code>FileObject</code>s. Can be attached to any <code>FileObject</code>.
* <P>
* When attached to a file it listens for file changes (due to saving from the IDE) and
* for deletes and renames.
* <P>
* When attached to a folder it listens for all actions taken on this folder.
* These include any modifications of data files or folders,
* and creation of new data files or folders.
*
* @see FileObject#addFileChangeListener
*
* @author Jaroslav Tulach, Petr Hamernik
* @version 0.16, May 6, 1998
*/
public interface FileChangeListener extends java.util.EventListener {
    /** Fired when a new folder is created. This action can only be
     * listened to in folders containing the created folder up to the root of
     * filesystem.
      *
     * @param fe the event describing context where action has taken place
     */
    public abstract void fileFolderCreated (FileEvent fe);

    /** Fired when a new file is created. This action can only be
    * listened in folders containing the created file up to the root of
    * filesystem.
    *
    * @param fe the event describing context where action has taken place
    */
    public abstract void fileDataCreated (FileEvent fe);

    /** Fired when a file is changed.
    * @param fe the event describing context where action has taken place
    */
    public abstract void fileChanged (FileEvent fe);

    /** Fired when a file is deleted.
    * @param fe the event describing context where action has taken place
    */
    public abstract void fileDeleted (FileEvent fe);

    /** Fired when a file is renamed.
    * @param fe the event describing context where action has taken place
    *           and the original name and extension.
    */
    public abstract void fileRenamed (FileRenameEvent fe);

    /** Fired when a file attribute is changed.
    * @param fe the event describing context where action has taken place,
    *           the name of attribute and the old and new values.
    */
    public abstract void fileAttributeChanged (FileAttributeEvent fe);
}
