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
* Abstract formatting layer offers the support
* for naming the layer and creation
* of an format-support.
*
* @author Miloslav Metelka
* @version 1.00
*/

public abstract class AbstractFormatLayer implements FormatLayer {

    /** Name of the layer */
    private String name;

    /** Construct new layer with the given name. */
    public AbstractFormatLayer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Create the format-support as an abstraction
     * over the format-writer.
     */
    protected FormatSupport createFormatSupport(FormatWriter fw) {
        return null;
    }

}
