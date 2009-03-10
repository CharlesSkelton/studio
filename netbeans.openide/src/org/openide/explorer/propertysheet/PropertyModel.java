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

import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

/** Model defining the right behaviour of property.
* This model should be used for communication with PropertyBean bean.
*
* @author Jaroslav Tulach, Petr Hamernik
*/
public interface PropertyModel {

    /** Name of the 'value' property. */
    public static final String PROP_VALUE = "value"; // NOI18N

    /** Getter for current value of a property.
     */
    public Object getValue() throws InvocationTargetException;

    /** Setter for a value of a property.
    * @param v the value
    * @exception InvocationTargetException
    */
    public void setValue(Object v) throws InvocationTargetException;

    /** The class of the property.
    */
    public Class getPropertyType();

    /** The class of the property editor or <CODE>null</CODE>
    * if default property editor should be used.
    */
    public Class getPropertyEditorClass();

    /** Add listener to change of the value.
    */
    public void addPropertyChangeListener(PropertyChangeListener l);

    /** Remove listener to change of the value.
    */
    public void removePropertyChangeListener(PropertyChangeListener l);
}
