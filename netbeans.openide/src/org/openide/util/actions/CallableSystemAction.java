/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.util.actions;

import org.openide.awt.*;

/** An action which may be called programmatically.
* Typically a presenter will call its {@link #performAction} method,
* which must be implemented.
* <p>Provides default presenters using the {@link Actions} utility class.
*
* @author   Ian Formanek, Jaroslav Tulach, Jan Jancura, Petr Hamernik
*/
public abstract class CallableSystemAction extends SystemAction
    implements Presenter.Menu, Presenter.Popup, Presenter.Toolbar {
    /** serialVersionUID */
    static final long serialVersionUID = 2339794599168944156L;

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a MenuBar.
    * @return the JMenuItem representation for the Action
    */
    public javax.swing.JMenuItem getMenuPresenter() {
        return new Actions.MenuItem(this, true);
    }

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a Popup Menu.
    * @return the JMenuItem representation for the Action
    */
    public javax.swing.JMenuItem getPopupPresenter() {
        return new Actions.MenuItem(this, false);
    }

    /* Returns a Component that presents the Action, that implements this
    * interface, in a ToolBar.
    * @return the Component representation for the Action
    */
    public java.awt.Component getToolbarPresenter() {
        return new Actions.ToolbarButton (this);
    }

    /** Actually perform the action.
    * This is the method which should be called programmatically.
    * Presenters in {@link Actions} use this.
    * <p>See {@link SystemAction#actionPerformed} for a note on
    * threading usage: in particular, do not access GUI components
    * without explicitly asking for the AWT event thread!
    */
    public abstract void performAction();

    /* Implementation of method of javax.swing.Action interface.
    * Delegates the execution to performAction method.
    *
    * @param ev ignored
    */
    public void actionPerformed (java.awt.event.ActionEvent ev) {
        performAction ();
    }
}
