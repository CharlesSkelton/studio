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

import javax.swing.text.BadLocationException;

/**
* Attempt to insert or remove from the guarded block has been done.
*
* @version 1.0
* @author Miloslav Metelka
*/

public class GuardedException extends BadLocationException {

    static final long serialVersionUID =-8139460534188487509L;
    public GuardedException(String s, int offs) {
        super (s, offs);
    }

}
