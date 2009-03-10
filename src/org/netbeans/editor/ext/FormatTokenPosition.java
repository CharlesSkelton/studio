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

package org.netbeans.editor.ext;

import org.netbeans.editor.TokenItem;

import javax.swing.text.Position;

/**
* Position consisting of the token-item
* and the offset inside it. The offset can range from zero to the
* last character in the token-text. The position right after
* the last character in the last token is expressed by token
* equal to null and offset equal to zero.
* The equality is defined as having the same offset in the same token.
* The token is compared just by equality operator.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface FormatTokenPosition {

    /** Get the token-item in which the position resides. */
    public TokenItem getToken();

    /** Get the offset inside the token-item. */
    public int getOffset();

    /** Get the bias of the position. Either Position.Bias.Forward
     * or Position.Bias.Backward.
     */
    public Position.Bias getBias();

}
