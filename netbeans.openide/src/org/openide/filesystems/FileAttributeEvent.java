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

package org.openide.filesystems;

/** Event used to listen on filesystem attribute changes.
*
* @author Petr Hamernik
*/
public class FileAttributeEvent extends FileEvent {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -8601944809928093922L;
    /** Name of attribute. */
    private String name;

    /** Old value of attribute */
    private Object oldValue;

    /** New value of attribute */
    private Object newValue;

    /** Creates new <code>FileAttributeEvent</code>. The <code>FileObject</code> where the action occurred
    * is assumed to be the same as the source object.
    * @param src source file which sent this event
    * @param name name of attribute, or <code>null</code> (since 1.33 only) if any attributes may have changed
    * @param oldValue old value of attribute, or <code>null</code> if the name is
    * @param newValue new value of attribute, or <code>null</code> if the name is
    */
    public FileAttributeEvent(FileObject src, String name, Object oldValue, Object newValue) {
        this(src, src, name, oldValue, newValue);
    }

    /** Creates new <code>FileAttributeEvent</code>.
    * @param src source file which sent this event
    * @param file file object where the action occurred
    * @param name name of attribute, or <code>null</code> (since 1.33 only) if any attributes may have changed
    * @param oldValue old value of attribute, or <code>null</code> if the name is
    * @param newValue new value of attribute, or <code>null</code> if the name is
    */
    public FileAttributeEvent(FileObject src, FileObject file,
                              String name, Object oldValue, Object newValue) {
        this(src, file, name, oldValue, newValue,false);    
    }

    /** Creates new <code>FileAttributeEvent</code>.
    * @param src source file which sent this event
    * @param file file object where the action occurred
    * @param name name of attribute, or <code>null</code> (since 1.33 only) if any attributes may have changed
    * @param oldValue old value of attribute, or <code>null</code> if the name is
    * @param newValue new value of attribute, or <code>null</code> if the name is
    * @param expected sets flag whether the value was expected
    */    
    public FileAttributeEvent(FileObject src, FileObject file,
                              String name, Object oldValue, Object newValue,boolean expected) {
        super(src, file,expected);
        this.name = name;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    /** Gets the name of the attribute.
    * @return Name of the attribute, or <code>null</code> (since 1.33 only) if an unknown attribute changed
    */
    public String getName () {
        return name;
    }

    /** Gets the old value of the attribute.
    * @return Old value of the attribute, or <code>null</code> if the name is
    */
    public Object getOldValue () {
        return oldValue;
    }

    /** Gets the new value of the attribute.
    * @return New value of the attribute, or <code>null</code> if the name is
    */
    public Object getNewValue () {
        return newValue;
    }
}
