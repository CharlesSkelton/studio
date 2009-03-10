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
* Base implementation of the token-id.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class BaseTokenID implements TokenID {

    private final String name;

    private final int numericID;

    private final TokenCategory category;

    public BaseTokenID(String name) {
        this(name, 0);
    }

    public BaseTokenID(String name, int numericID) {
        this(name, numericID, null);
    }

    public BaseTokenID(String name, TokenCategory category) {
        this(name, 0, category);
    }

    public BaseTokenID(String name, int numericID, TokenCategory category) {
        this.name = name;
        this.numericID = numericID;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public int getNumericID() {
        return numericID;
    }

    public TokenCategory getCategory() {
        return category;
    }

    public String toString() {
        return getName() + ((getCategory() != null)
                ? (", category=" + getCategory()) : ""); // NOI18N
    }

}
