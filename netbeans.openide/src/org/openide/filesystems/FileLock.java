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



/** Represents an acquired lock on a <code>FileObject</code>.
* Typical usage includes locking the file in the editor on first
* modification, and then using this object to ensure exclusive access when
* overwriting the file (saving) by using {@link FileObject#getOutputStream}.
* Also used for renames, deletes, &amp;c.
* <p>Note that such locks are only used to protect against concurrent write accesses,
* and are not used for read operations (i.e. they are <em>not</em> write-one-read-many locks).
* Normally this is sufficient protection. If you really need an atomic read, you may
* simply lock the file, perform the read, and unlock it when done. The file will still
* be protected against writes, although the read operation did not request a lock.
*
* @see FileObject
*
* @author Petr Hamernik, Jaroslav Tulach, Ian Formanek
* @version 0.16, Jun 5, 1997
*
*/
public class FileLock extends Object {

    /** Determines if lock is locked or if it was released. */
    private boolean locked = true;

    // ===============================================================================
    //  This part of code could be used for monitoring of closing file streams.
    /*  public static java.util.HashMap locks = new java.util.HashMap();
      public FileLock() {
        locks.put(this, new Exception()); int size = locks.size();
        System.out.println ("locks:"+(size-1)+" => "+size);
      }
      public void releaseLock() {
        locked = false; locks.remove(this); int size = locks.size();
        System.out.println ("locks:"+(size+1)+" => "+size);
      } */
    //  End of the debug part
    // ============================================================================
    //  Begin of the original part

    /** Release this lock.
    * In typical usage this method will be called in a <code>finally</code> clause.
    */
    public void releaseLock() {
        locked = false;
    }

    //  End of the original part
    // ============================================================================

    /** Test whether this lock is still active, or released.
    * @return <code>true</code> if lock is still active
    */
    public boolean isValid() {
        return locked;
    }

    /** Finalize this object. Calls {@link #releaseLock} to release the lock if the program
    * for some reason failed to.
    */
    public void finalize () {
        releaseLock ();
    }

    // ========================= NONE file lock =====================================

    /** Constant that can be used in filesystems that do not support locking.
     * Represents a lock which is never valid.
    */
    public static final FileLock NONE = new FileLock () {
                                            /** @return false always. */
                                            public boolean isValid() {
                                                return false;
                                            }
                                        };
}
