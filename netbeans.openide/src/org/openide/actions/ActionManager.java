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

package org.openide.actions;

import java.beans.*;

import org.openide.util.actions.SystemAction;

/** Collects access methods to implementation depended functionality
* for actions package.
*
* @author Jaroslav Tulach
*/
public abstract class ActionManager extends Object {
    /** name of property that is fired when set of context actions
    * changes.
    */
    public static final String PROP_CONTEXT_ACTIONS = "contextActions"; // NOI18N

    /** Utility field used by event firing mechanism. */
    private PropertyChangeSupport supp = new PropertyChangeSupport (this);


    /** Get all registered actions that should be displayed
    * by tools action.
    * Can contain <code>null</code>s that will be replaced by separators.
    *
    * @return array of actions
    */
    public abstract SystemAction[] getContextActions ();
    
    /** Invokes action in a RequestPrecessor dedicated to performing
     * actions.
     */
    public abstract void invokeAction(javax.swing.Action a, java.awt.event.ActionEvent e);

    /** Registers PropertyChangeListener to receive events.
     * @param listener The listener to register.
     */
    public final void addPropertyChangeListener(
        PropertyChangeListener listener
    ) {
        supp.addPropertyChangeListener(listener);
    }

    /** Removes PropertyChangeListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public final void removePropertyChangeListener(
        PropertyChangeListener listener
    ) {
        supp.removePropertyChangeListener (listener);
    }
    /** Notifies all registered listeners about the event.
     * @param name property name
     * @param o old value
     * @param n new value
     */
    protected final void firePropertyChange(
        String name , Object o, Object n
    ) {
        supp.firePropertyChange(name, o, n);
    }
}
