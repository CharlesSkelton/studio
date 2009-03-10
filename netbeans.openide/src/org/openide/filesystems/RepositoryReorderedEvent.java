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

import java.util.EventObject;


/** Fired when a filesystem pool is reordered.
 * @see Repository#reorder
 */
public class RepositoryReorderedEvent extends EventObject {

    /** permutation */
    private int[] perm;

    static final long serialVersionUID =-5473107156345392581L;
    /** Create a new reorder event.
     * @param fsp the filesystem pool being reordered
     * @param perm the permutation of filesystems in the pool
     */
    public RepositoryReorderedEvent(Repository fsp, int[] perm) {
        super(fsp);
        this.perm = perm;
    }

    /** Get the affected filesystem pool.
     * @return the pool
     */
    public Repository getRepository() {
        return (Repository)getSource();
    }

    /** Get the permutation of filesystems.
     * @return the permutation
     */
    public int[] getPermutation() {
        int[] nperm = new int[perm.length];
        System.arraycopy(perm, 0, nperm, 0, perm.length);
        return nperm;
    }
}
