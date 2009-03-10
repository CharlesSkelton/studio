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

import java.awt.event.ActionEvent;

/**
* SheetButtonListener interface.
*
* @author Jan Jancura
* @version 0.11, Nov 14, 1997
*/
interface SheetButtonListener {

    /**
    * Invoked when the mouse enters a component.
    */
    public void sheetButtonEntered (ActionEvent e);

    /**
    * Invoked when the mouse exits a component.
    */
    public void sheetButtonExited (ActionEvent e);

    /**
    * Invoked when the mouse has been clicked on a component.
    */
    public void sheetButtonClicked (ActionEvent e);
}
