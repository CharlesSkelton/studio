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

/** Signals that the file object is somehow corrupted.
* The required operation is not possible due to a previous deletion, or
* an unexpected (external) change in the filesystem.
*
* @author Jaroslav Tulach
* @version 0.10 October 7, 1997
*/
public class FileStateInvalidException extends java.io.IOException {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -4987532595879330362L;

    /** The name of the filesystem containing the bad FileObject */
    private String fileSystemName;

    /** Create new <code>FileStateInvalidException</code>.
    */
    public FileStateInvalidException () {
        super ();
    }

    /** Create new <code>FileStateInvalidException</code> with the specified text.
    * @param s the text describing the exception
    */
    public FileStateInvalidException (String s) {
        super (s);
    }

    /** Create new <code>FileStateInvalidException</code> with the specified text.
    * @param s the text describing the exception
    * @param fsName the name of the filesystem containing the bad FileObject
    */
    FileStateInvalidException (String s, String fsName) {
        super (s);
	fileSystemName = fsName;
    }

    /** @return the name of the fileSystem containing the bad FileObject.  null
    * if this information is unavailable.
    * @since 1.30
    */
    public String getFileSystemName() {
    	return fileSystemName;
    }
}
