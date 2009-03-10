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
/*
 * ReferencesResolver.java
 *
 * Created on June 28, 2001, 9:36 PM
 */

package org.openide.loaders;

import org.openide.filesystems.FileObject;
import java.beans.PropertyChangeListener;
import java.net.URL;

/**
 *
 * @author  Svatopluk Dedic, Vita Stejskal
 */
interface ReferencesResolver {
    public static final String PROP_MAPPING = "mapping"; // NOI18N

    public abstract FileObject find (URL ref, FileObject base);
    public abstract URL translateURL (URL ref, FileObject base);
    public abstract void addPropertyChangeListener (PropertyChangeListener l);
    public abstract void removePropertyChangeListener (PropertyChangeListener l);
}

