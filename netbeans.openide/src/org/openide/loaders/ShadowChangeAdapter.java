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

package org.openide.loaders;

import java.util.EventObject;

import org.openide.util.Lookup;
import org.openide.filesystems.*;

/** Adapter for listening on changes of fileobjects and refreshing data
* shadows/broken links
*
* @author Ales Kemr
*/
class ShadowChangeAdapter extends Object implements RepositoryListener, 
        OperationListener{
    
    /** Creates new ShadowChangeAdapter */
    ShadowChangeAdapter() {
        
        /* listen on repository to refresh datashadows after 
        * add/remove filesystem 
        */
        Repository.getDefault().addRepositoryListener(this);            
        
        /* listen on loader pool to refresh datashadows after 
        * create/delete dataobject
        */        
        ((DataLoaderPool)Lookup.getDefault().lookup(DataLoaderPool.class)).addOperationListener(this);
    }

    /** Checks for BrokenDataShadows */
    static void checkBrokenDataShadows(EventObject ev) {
        BrokenDataShadow.checkValidity(ev);
    }
    
    /** Checks for DataShadows */
    static void checkDataShadows(EventObject ev) {
        DataShadow.checkValidity(ev);
    }
    
    /** Called when new file system is added to the pool.
     * @param ev event describing the action
    */
    public void fileSystemAdded(RepositoryEvent ev) {
        checkBrokenDataShadows(ev);
    }
    
    /** Called when a file system is removed from the pool.
     * @param ev event describing the action
    */
    public void fileSystemRemoved(RepositoryEvent ev) {
        checkDataShadows(ev);
    }
    
    /** Called when a file system pool is reordered.  */
    public void fileSystemPoolReordered(RepositoryReorderedEvent ev) {
    }
    
    /** Object has been recognized by
     * {@link DataLoaderPool#findDataObject}.
     * This allows listeners
     * to attach additional cookies, etc.
     *
     * @param ev event describing the action
    */
    public void operationPostCreate(OperationEvent ev) {
        checkBrokenDataShadows(ev);
    }
    
    /** Object has been successfully copied.
     * @param ev event describing the action
    */
    public void operationCopy(OperationEvent.Copy ev) {
    }
    
    /** Object has been successfully moved.
     * @param ev event describing the action
    */
    public void operationMove(OperationEvent.Move ev) {
        checkDataShadows(ev);
        checkBrokenDataShadows(ev);
    }
    
    /** Object has been successfully deleted.
     * @param ev event describing the action
    */
    public void operationDelete(OperationEvent ev) {
        checkDataShadows(ev);
    }
    
    /** Object has been successfully renamed.
     * @param ev event describing the action
    */
    public void operationRename(OperationEvent.Rename ev) {
        checkDataShadows(ev);
        checkBrokenDataShadows(ev);
    }
    
    /** A shadow of a data object has been created.
     * @param ev event describing the action
    */
    public void operationCreateShadow(OperationEvent.Copy ev) {
    }
    
    /** New instance of an object has been created.
     * @param ev event describing the action
    */
    public void operationCreateFromTemplate(OperationEvent.Copy ev) {
        checkBrokenDataShadows(ev);
    }
    
}
