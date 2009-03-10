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

package org.openide.util.actions;

/** Specifies how an action should be performed.
* Should be implemented by classes which are able to perform an action's work
* on its behalf, e.g. for {@link CallbackSystemAction}s.
*
* @author   Ian Formanek
* @version  0.11, 27 Oct 1997
*/
public interface ActionPerformer {
    /** Called when the action is to be performed.
    * @param action the action to be performed by this performer
    */
    public void performAction(SystemAction action);
}
