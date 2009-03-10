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


import javax.swing.Action;
import org.openide.util.Lookup;


/**
 * Interface which is supposed to be implemented by action which behaviour
 * is context dependent.
 * It means the created action (by method {@link #createContextAwareInstance}
 * is valid to the provided context, i.e. enablement status
 * and action performation is related to the context
 * (provided by the <code>Lookup</code>).
 * The context is typically <code>TopComponent</code>S context.
 *
 * @author  Jaroslav Tulach, Peter Zavadsky
 *
 * @see org.openide.windows.TopComponent#getLookup
 * @see org.openide.util.Utilities#actionsToPopup
 * @since 3.29
 */
public interface ContextAwareAction extends Action {
  
    /** Creates action instance for provided context. */
    public Action createContextAwareInstance(Lookup actionContext);
}

