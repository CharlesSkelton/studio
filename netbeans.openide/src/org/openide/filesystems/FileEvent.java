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

/** Event for listening on filesystem changes.
* <P>
* By calling {@link #getFile} the original file where the action occurred
* can be obtained.
*
* @author Jaroslav Tulach, Petr Hamernik
*/
public class FileEvent extends java.util.EventObject {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = 1028087432345400108L;
    /** Original file object where the action took place. */
    private FileObject file;
    /** time when this event has been fired */
    private long time;
    /** is expected? */
    private boolean expected;
    /***/
    private EventControl.AtomicActionLink atomActionID;

    /** Creates new <code>FileEvent</code>. The <code>FileObject</code> where the action occurred
    * is assumed to be the same as the source object.
    * @param src source file which sent this event
    */
    public FileEvent(FileObject src) {
        this(src, src);
    }

    /** Creates new <code>FileEvent</code>, specifying the action object.
    * <p>
    * Note that the two arguments of this method need not be identical
    * in cases where it is reasonable that a different file object from
    * the one affected would be listened to by other components. E.g.,
    * in the case of a file creation event, the event source (which
    * listeners are attached to) would be the containing folder, while
    * the action object would be the newly created file object.
    * @param src source file which sent this event
    * @param file <code>FileObject</code> where the action occurred */
    public FileEvent(FileObject src, FileObject file) {
        super(src);
        this.file = file;
        this.time = System.currentTimeMillis ();
    }

    /** Creates new <code>FileEvent</code>. The <code>FileObject</code> where the action occurred
    * is assumed to be the same as the source object. Important if FileEvent is created according to 
    * existing FileEvent but with another source and file but with the same time.
    */
    FileEvent(FileObject src, FileObject file, long time) {
        this (src, file);        
        this.time = time;
    }
    
    /** Creates new <code>FileEvent</code>, specifying the action object.
    * <p>
    * Note that the two arguments of this method need not be identical
    * in cases where it is reasonable that a different file object from
    * the one affected would be listened to by other components. E.g.,
    * in the case of a file creation event, the event source (which
    * listeners are attached to) would be the containing folder, while
    * the action object would be the newly created file object.
    * @param src source file which sent this event
    * @param file <code>FileObject</code> where the action occurred 
    * @param expected sets flag whether the value was expected*/    
    public FileEvent(FileObject src, FileObject file, boolean expected) {
        this(src,file);
        this.expected = expected;
    }
    
    /** @return the original file where action occurred
    */
    public final FileObject getFile() {
        return file;
    }

    /** The time when this event has been created.
    * @return the milliseconds
    */
    public final long getTime () {
        return time;
    }


    /** Getter to test whether the change has been expected or not.
    */
    public final boolean isExpected () {
        return expected;
    }

    public String toString () {
        FileSystem fs;
        try {
            fs = file.getFileSystem();
        } catch (FileStateInvalidException fsie) {
            fs = null;
        }
        return super.toString () + "[file=" + file + ",time=" + time // NOI18N
            + ",expected=" + expected + ",fs=" + fs + "]"; // NOI18N
    }
    
    /** */
    void setAtomicActionLink (EventControl.AtomicActionLink atomActionID) {    
        this.atomActionID = atomActionID;
    }
    
    
    /** Tests if FileEvent was fired from atomic action.
     * @param run is tested atomic action.
     * @return true if fired from run.
     * @since 1.35     
     */    
    public boolean firedFrom (FileSystem.AtomicAction run) {
        EventControl.AtomicActionLink currentPropID = this.atomActionID;
        if (run == null)
            return false;
        
        while (currentPropID != null) {
            if (run.equals(currentPropID.getAtomicAction ()))
                return true;
            
            currentPropID = currentPropID.getPreviousLink ();
        }
        return false;
    }
}
