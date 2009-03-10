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

import java.io.*;
import java.util.HashSet;

import org.openide.filesystems.*;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.nodes.Node;

/** An implementation of a data object which consumes file objects not recognized by any other loaders.
*
* @author Ian Formanek
*/
class DefaultDataObject extends MultiDataObject {
    static final long serialVersionUID =-4936309935667095746L;
    /** generated Serialized Version UID */
    //  static final long serialVersionUID = 6305590675982925167L;

    /** Constructs new data shadow for given primary file and referenced original.
    * @param fo the primary file
    * @param original original data object
    */
    DefaultDataObject (FileObject fo, MultiFileLoader loader) throws DataObjectExistsException {
        super (fo, loader);
    }

    /* Creates node delegate.
    */
    protected Node createNodeDelegate () {
        DataNode dn = new DataNode (this, org.openide.nodes.Children.LEAF);
        
        // netbeans.core.nodes.description    
        dn.setShortDescription (NbBundle.getMessage (DefaultDataObject.class, 
                                "HINT_DefaultDataObject")); // NOI18N
        return dn;
    }

    /** Get the name of the data object.
    * <p>The implementation uses the name of the primary file and its exten.
    * @return the name
    */
    
    public String getName() {
        return getPrimaryFile ().getNameExt ();
    }

    /* Help context for this object.
    * @return help context
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (DefaultDataObject.class);
    }

    
    /* Handles renaming of the object.
    * Must be overriden in children.
    *
    * @param name name to rename the object to
    * @return new primary file of the object
    * @exception IOException if an error occures
    */
    protected FileObject handleRename (String name) throws IOException {
        FileLock lock = getPrimaryFile ().lock ();
        int pos = name.lastIndexOf('.');
        
        try {
            if (pos < 0){
                // file without separator
                getPrimaryFile ().rename (lock, name, null);
            } else if (pos == 0){
                getPrimaryFile ().rename (lock, name, getPrimaryFile ().getExt ());
            } else {
                if (!name.equals(getPrimaryFile ().getNameExt())){
                    getPrimaryFile ().rename (lock, name.substring(0, pos), 
                        name.substring(pos+1, name.length()));
                    DataObjectPool.getPOOL().revalidate(
                        new HashSet (java.util.Collections.singleton(getPrimaryFile ()))
                    );
                }
            }
        } finally {
            lock.releaseLock ();
        }
        return getPrimaryFile ();
    }
    
    /* Creates new object from template.
    * @exception IOException
    */
    protected DataObject handleCreateFromTemplate (
        DataFolder df, String name
    ) throws IOException {
        // avoid doubling of extension
        if (name != null && name.endsWith("." + getPrimaryFile ().getExt ())) // NOI18N
            name = name.substring(0, name.lastIndexOf("." + getPrimaryFile ().getExt ())); // NOI18N
        
        return super.handleCreateFromTemplate (df, name);
    }
}
