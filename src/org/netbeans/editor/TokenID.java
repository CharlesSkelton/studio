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
* Token-id is a unique identifier of a particular token.
* It's not a classical token, because it doesn't contain the image of the token.
* The token image is handled separately in general.
* The common place where the tokens should be defined is
* the appropriate token-context for which they are being
* created.
* The fact that <tt>TokenID</tt> extends <tt>TokenCategory</tt>
* helps to treat the colorings more easily by working with
* <tt>TokenCategory</tt> only (it can be <tt>TokenID</tt> too).
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface TokenID extends TokenCategory {

    /** Get the optional category of the token.
    */
    public TokenCategory getCategory();

}
