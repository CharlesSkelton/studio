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

import java.awt.*;

/**
* Container for printed text. The parts of text attributed by font,
* fore and back color are added to it for the whole printed area.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface PrintContainer {

    /** Add the attributed characters to the container.
     * @param chars characters being added.
     * @param font font of the added characters
     * @param foreColor foreground color of the added characters
     * @param backColor background color of the added characters
     */
    public void add(char[] chars, Font font, Color foreColor, Color backColor);

    /** End of line was found. */
    public void eol();

    /**
     * @return true if the container needs to init empty line with
     * at least one character. Printing then adds one space
     * to each empty line.
     * False means that the container is able to accept
     * lines with no characters.
     */
    public boolean initEmptyLines();

}
