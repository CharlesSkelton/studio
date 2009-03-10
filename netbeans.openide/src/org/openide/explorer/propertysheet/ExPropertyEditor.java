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

import java.beans.PropertyEditor;

/**
 * An extension interface for property editors that hides 
 * all the necessary communication with the IDE. ExPropertyEditor
 * is able to accept an instance of PropertyEnv class - this
 * environment passes additional information to the editor.
 * Whatever should be ever added to the mechanism
 * should go into the PropertyEnv class. 
 * @author  dstrupl
 */

public interface ExPropertyEditor extends PropertyEditor {
    
    /**
     * If you want to enable/disable the OK button on the custom
     * property editor panel you can fire a property change event
     * with boolean value. You don't have to implement the ExPropertyEditor
     * interface for this feature to be turned on.
     * When firing property change event PROP_VALUE_VALID is the name
     * and an instance of java.lang.Boolean should be passed as a value.
     */
    public static final String PROP_VALUE_VALID = "propertyValueValid"; // NOI18N
    
    /**
     * If you want to add custom help ID on the custom property 
     * editor panel you can store its value in PROPERTY_HELP_ID property.
     */
    public static final String PROPERTY_HELP_ID = "helpID"; // NOI18N
    
    /** 
     * This method is called by the IDE to pass
     * the environment to the property editor.
     */
    public void attachEnv(PropertyEnv env);
}
