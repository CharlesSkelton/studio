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

import org.openide.util.Lookup;
import org.openide.ErrorManager;

/** Contains utility methods to deal with repository and error manager,
* so we do not need to directly contact 
*
* @author Jaroslav Tulach
*/
final class ExternalUtil extends Object {
    /** value for the repository & error manager */
    private static Repository repository;
    
    /** Static method to find the Repository to use.
     * @return Repository instance
     */
    public static Repository getRepository () {
        initialize ();
        return repository;
    }
    
    /** Notifies an exception.
     */
    public static void exception (Exception ex) {
        ErrorManager.getDefault().notify (ErrorManager.INFORMATIONAL, ex);
    }
    
    /** Copies anotation.
     * @param newEx new exception to annotate
     * @param oldEx old exception to take annotation from
     * @return newEx
     */
    public static Throwable copyAnnotation (Throwable newEx, Throwable oldEx) {
        return ErrorManager.getDefault().annotate(newEx, oldEx);
    }
    
    
    /** Annotates the exception with a message.
     */
    public static void annotate (Throwable ex, String msg) {
        ErrorManager.getDefault().annotate (ex, msg);
    }
    
    /** Annotates the exception with a message.
     */
    public static Throwable annotate (Throwable ex, Throwable stack) {
        return ErrorManager.getDefault().annotate (ex, stack);
    }
    
    
    /** Logs a text.
     */
    public static void log (String msg) {
        ErrorManager.getDefault().log (ErrorManager.INFORMATIONAL, msg);
    }
    
    /** Loads a class of given name
     * @param name name of the class
     * @return the class
     * @exception ClassNotFoundException if class was not found
     */
    public static Class findClass (String name) throws ClassNotFoundException {
        initialize ();
        
        ClassLoader c = (ClassLoader)Lookup.getDefault ().lookup (ClassLoader.class);
        
        if (c == null) {
            return Class.forName (name);
        } else {
            return Class.forName (name, true, c);
        }
    }
        
    
    /** Initializes the context and errManager
     */
    private static void initialize () {
        if (!isInitialized ()) {
            Lookup l = Lookup.getDefault ();
            Repository rep = (Repository)l.lookup (org.openide.filesystems.Repository.class);            
            setRepository (rep);
        }
    }
    

    private static synchronized boolean isInitialized () {
        if (repository == null) 
            return false;

        return true;
    }

    /** 
     * @param rep may be null
     */
    private static synchronized void setRepository (Repository rep) {
        repository = rep;
        
        if (repository == null) {
            // if not provided use default one
            repository = new Repository (new XMLFileSystem ());
        }        
    }
        
}
