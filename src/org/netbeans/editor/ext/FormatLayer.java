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

/**
* Formatting layer that can be added to <tt>BaseFormatter</tt>
* to format the tokens.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface FormatLayer {

    /** Get the name of the layer. This name is used to identify
    * the layer when it's being removed or replaced and it can
    * be used for debugging purposes too.
    */
    public String getName();

    /** Format the tokens begining with the firstItem till the end.
    * @param fw format-writer to be formatted. The format-layer
    *  will usually create the format-support as an abstraction
    *  level over the format-layer.
    */
    public void format(FormatWriter fw);

}
