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

package org.openide.explorer.propertysheet.editors;

import java.awt.Component;

/**
* Enhances standard property editor to support in-place custom editors and tagged values.
*
* @author Jan Jancura, Ian Formanek
*/
public interface EnhancedPropertyEditor extends java.beans.PropertyEditor {

    /** Get an in-place editor.
    * @return a custom property editor to be shown inside the property
    *         sheet
    */
    public Component getInPlaceCustomEditor ();

    /** Test for support of in-place custom editors.
    * @return <code>true</code> if supported
    */
    public boolean hasInPlaceCustomEditor ();

    /** Test for support of editing of tagged values.
    * Must also accept custom strings, otherwise you may may specify a standard property editor accepting only tagged values.
    * @return <code>true</code> if supported
    */
    public boolean supportsEditingTaggedValues ();

}
