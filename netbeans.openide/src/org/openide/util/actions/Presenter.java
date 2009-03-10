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
import javax.swing.*;
import java.awt.Component;

/** Provides a presentation feature for an action.
* Each {@link SystemAction action} that wants to offer a kind of presentation of itself
* to the user should implement one of the inner interfaces.
* <P>
* For example to be presented in popup menu, an action should
* implement {@link Presenter.Popup}.
* <p> Normally actions should implement both {@link Presenter.Menu} and
* {@link Presenter.Popup} together and return the same menu item for each.
* <p><em>Note:</em> implementing these interfaces yourself means that you want to
* provide some sort of unusual display format, e.g. a submenu!
* Most people will simply want to use a subclass of {@link CallableSystemAction}
* and use the default implementations of all three interfaces, according to
* {@link SystemAction#getName} and {@link SystemAction#iconResource}.
*
* @author Jaroslav Tulach
*/
public interface Presenter {
    /** The presenter interface for presenting an action in a menu.
    */
    public interface Menu extends Presenter {
        /** Get a menu item that can present this action in a {@link javax.swing.JMenu}.
        * @return the representation for this action
        */
        public JMenuItem getMenuPresenter();
    }

    /** The presenter interface for presenting an action in a popup menu.
    */
    public interface Popup extends Presenter {
        /** Get a menu item that can present this action in a {@link javax.swing.JPopupMenu}.
        * @return the representation for this action
        */
        public JMenuItem getPopupPresenter();
    }

    /** The presenter interface for presenting an action in a toolbar.
    */
    public interface Toolbar extends Presenter {

        /** Get a component that can present this action in a {@link javax.swing.JToolBar}.
        * @return the representation for this action
        */
        public Component getToolbarPresenter();
    }

}
