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

/** Service provider interface (SPI) for adding cancel support to various jobs.
 *
 * Note that {@link #cancel} method will be typically called concurrently to 
 * other methods of job or operation in question, so proper synchronization
 * is needed.
 *
 * @author Dafe Simonek
 *
 * @since 3.36
 */
public interface Cancellable {
 
    /** Cancel processing of the job. Called not more then once for specific job. 
     *
     * @return true if the job was succesfully cancelled, false if job 
     *         can't be cancelled for some reason 
     */
    public boolean cancel ();
    
}
