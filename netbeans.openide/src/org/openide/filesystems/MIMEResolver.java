/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.filesystems;

import java.util.*;

/**
 * This class is intended as superclass for individual resolvers. 
 * All registered subclasses of MIMEResolver are looked up and asked one by one 
 * to resolve MIME type of passed FileObject. Resolving is finished right after 
 * a resolver is able to resolve the FileObject or if all registered
 * resolvers returned null (not recognized).
 * <p>
 * Resolvers are registered if they have their record in the Lookup area.
 * E.g. in form : org-some-package-JavaResolver.instance file. 
 *
 * @author  rmatous
 */

public abstract class MIMEResolver {    

    /**
     * Resolves FileObject and returns recognized MIME type
     * @param fo is FileObject which should be resolved (This FileObject is not 
     * thread safe. Also this FileObject should not be cached for later use)
     * @return  recognized MIME type or null if not recognized
     */
    public abstract String findMIMEType (FileObject fo);        
    
}
