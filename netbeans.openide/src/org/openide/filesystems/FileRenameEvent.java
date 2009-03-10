/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.filesystems;

/** Event indicating a file rename.
*
* @author Petr Hamernik
*/
public class FileRenameEvent extends FileEvent {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = -3947658371806653711L;
    /** Original name of the file. */
    private String name;

    /** Original extension of the file. */
    private String ext;

    /** Creates new <code>FileRenameEvent</code>. The <code>FileObject</code> where the action took place
    * is assumed to be the same as the source object.
    * @param src source file which sent this event
    * @param name original file name
    * @param ext original file extension
    */
    public FileRenameEvent(FileObject src, String name, String ext) {
        this(src, src, name, ext);
    }

    /** Creates new <code>FileRenameEvent</code>, specifying an event location.
    * @param src source file which sent this event
    * @param file file object where the action took place
    * @param name original file name
    * @param ext original file extension
    */
    public FileRenameEvent(FileObject src, FileObject file, String name, String ext) {
        this(src, file, name, ext, false);
    }

    /** Creates new <code>FileRenameEvent</code>, specifying an event location
    * and whether the event was expected by the system.
    * @param src source file which sent this event
    * @param file file object where the action took place
    * @param name original file name
    * @param ext original file extension
    * @param expected whether the value was expected
    */    
    public FileRenameEvent(FileObject src, FileObject file, String name, String ext, boolean expected) {
        super(src, file, expected);
        this.name = name;
        this.ext = ext;
    }
    
    
    /** Get original name of the file.
    * @return old name of the file
    */
    public String getName() {
        return name;
    }

    /** Get original extension of the file.
    * @return old extension of the file
    */
    public String getExt() {
        return ext;
    }
}
