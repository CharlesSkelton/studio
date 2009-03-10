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

package org.openide.explorer.propertysheet;

import java.beans.FeatureDescriptor;

/**
 * An extension to the PropertyModel interface that allows
 * the property to supply information for ExPropertyEditor.
 * @author David Strupl
 */
public interface ExPropertyModel extends PropertyModel {

    /**
     * Returns an array of beans/nodes that this property belongs
     * to.
     */
    public Object[] getBeans();
    
    /**
     * Returns descriptor describing the property.
     */
    public FeatureDescriptor getFeatureDescriptor();    
}
