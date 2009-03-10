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

package org.netbeans.editor;

/**
* Base implementation of the token category.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class BaseTokenCategory implements TokenCategory {

    private final String name;

    private final int numericID;

    public BaseTokenCategory(String name) {
        this(name, 0);
    }

    public BaseTokenCategory(String name, int numericID) {
        this.name = name;
        this.numericID = numericID;
    }

    /** Get the name of the category. */
    public String getName() {
        return name;
    }

    /** Get the optional numeric identification of this token-category. It can help
    * to use the category in switch-case statements. It should default to a zero
    * if no numeric-id should be used.
    */
    public int getNumericID() {
        return numericID;
    }

    public String toString() {
        return getName();
    }

}
