/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.util;

/** Service provider interface (SPI) for executing of time consuming task which
 * results are visible in UI.
 *
 * Typical usage is post-initialization of UI components or various long lasting
 * operations like network accessing invoked directly or indirectly by user
 * from UI.
 *
 * Note that it's often desirable to provide cancel support, at least for
 * longer lasting jobs. See {@link org.openide.util.Cancellable} support.
 * Keep in mind that methods {@link #construct} and
 * {@link org.openide.util.Cancellable#cancel} can be called concurrently and
 * require proper synchronization as such.
 * 
 * @author  Dafe Simonek
 *
 * @since 3.36
 */
public interface AsyncGUIJob {
    
    /** Worker method, can be called in any thread but event dispatch thread.
     * Implement your time consuming work here.
     * Always called and completed before {@link #finished} method.
     */
    public void construct ();
    
    /** Method to update UI using given data constructed in {@link #construct}
     * method. Always called in event dispatch thread, after {@link #construct}
     * method completed its execution.
     */
    public void finished ();

}
