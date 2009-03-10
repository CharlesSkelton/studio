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

import java.util.EventObject;

/** Event describing a change in annotation of files.
*
* @author Jaroslav Tulach
*/
public final class FileStatusEvent extends EventObject {
    /** changed files */
    private java.util.Set files;
    /** icon changed? */
    private boolean icon;
    /** name changed? */
    private boolean name;

    static final long serialVersionUID =-6428208118782405291L;
    /** Creates new FileStatusEvent
    * @param fs filesystem that causes the event
    * @param files set of FileObjects that has been changed
    * @param icon has icon changed?
    * @param name has name changed?
    */
    public FileStatusEvent (
        FileSystem fs, java.util.Set files, boolean icon, boolean name
    ) {
        super (fs);
        this.files = files;
        this.icon = icon;
        this.name = name;
    }

    /** Creates new FileStatusEvent
    * @param fs filesystem that causes the event
    * @param file file object that has been changed
    * @param icon has icon changed?
    * @param name has name changed?
    */
    public FileStatusEvent (
        FileSystem fs, FileObject file, boolean icon, boolean name
    ) {
        this (fs, java.util.Collections.singleton (file), icon, name);
    }

    /** Creates new FileStatusEvent. This does not specify the
    * file that changed annotation, assuming that everyone should update
    * its annotation. Please notice that this can be time consuming
    * and should be fired only when really necessary.
    *
    * @param fs filesystem that causes the event
    * @param icon has icon changed?
    * @param name has name changed?
    */
    public FileStatusEvent (
        FileSystem fs, boolean icon, boolean name
    ) {
        this (fs, (java.util.Set)null, icon, name);
    }

    /** Getter for filesystem that caused the change.
    * @return filesystem
    */
    public FileSystem getFileSystem () {
        return (FileSystem)getSource ();
    }

    /** Is the change change of name?
    */
    public boolean isNameChange () {
        return name;
    }

    /** Do the files changed their icons?
    */
    public boolean isIconChange () {
        return icon;
    }

    /** Check whether the given file has been changed.
    * @param file file to check
    * @return true if the file has been affected by the change
    */
    public boolean hasChanged (FileObject file) {
        if (files == null) {
            // all files on source filesystem are said to change
            try {
                return file.getFileSystem() == getSource ();
            } catch (FileStateInvalidException ex) {
                // invalid files should not be changed
                return false;
            }
        } else {
            // specified set of files, so check it
            return files.contains (file);
        }
    }
}
