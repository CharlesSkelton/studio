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
* Token-id with the fixed token image. The image text
* can be retrieved by <tt>getImage()</tt>.
* The image token-ids are treated specially in some
* editor parts for example in the indentation engine.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface ImageTokenID extends TokenID {

    /** Get the string image of the token-id. */
    public String getImage();

}
