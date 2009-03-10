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

package org.netbeans.editor;

/**
* Token category enables dividing the tokens into groups.
* Each token by default can be contained in zero or one group.
* If a coloring for the token can't be found and it has non-null
* token-group then the coloring for the group is attempted.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface TokenCategory {

    /** Get the name of the category. */
    public String getName();

    /** Get the optional numeric identification of this token-category. It can help
    * to use the category in switch-case statements. It should default to a zero
    * if no numeric-id should be used.
    */
    public int getNumericID();

}
