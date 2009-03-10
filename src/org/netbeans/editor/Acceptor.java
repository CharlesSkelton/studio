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

/** Accept or reject given character. The advance is that accepting can
* be done by code so the methods from java.lang.Character can be used
* and mixed.
*
* @author Jaroslav Tulach
* @version 1.00
*/


public interface Acceptor {

    /** Accept or reject character
    * @return true to accept the given character or false to not accept it
    */
    public boolean accept(char ch);

}
