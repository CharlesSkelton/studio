/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.actions;

import javax.swing.Action;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOperation;
import org.openide.awt.Actions;
import org.openide.awt.JInlineMenu;

import javax.swing.JMenuItem;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

/** Get properties of a node.
*
* @see NodeOperation#showProperties(Node[])
* @author   Ian Formanek, Jan Jancura
*/
public class PropertiesAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 5485687384586248747L;
    
    /* Actually performs action of showing properties
    * @param activatedNodes Array of activated nodes
    */
    protected void performAction (Node[] activatedNodes) {
        if (activatedNodes == null) {
            throw new IllegalStateException();
        }
        if (activatedNodes.length == 1) {
            NodeOperation.getDefault().showProperties(activatedNodes[0]);
        } else {
            NodeOperation.getDefault().showProperties(activatedNodes);
        }
    }

    /* Manages enable - disable logic
    * @param activatedNodes Array of activated nodes
    * @return true if action should be enabled, false otherwise
    */
    protected boolean enable (Node[] activatedNodes) {
        if (activatedNodes == null) return false;
        // This is not quite as exact as checking if the *intersection* of their
        // properties is also nonempty, but it is pretty close.
        for (int i = 0; i < activatedNodes.length; i++)
            if (activatedNodes[i].getPropertySets ().length > 0)
                return true;
        return false;
    }

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a Popup Menu.
    * @return the JMenuItem representation for the Action
    */
    public JMenuItem getPopupPresenter() {
        JMenuItem prop = new Actions.MenuItem (this, false);
        
        CustomizeAction customizeAction =
            (CustomizeAction)SystemAction.get(CustomizeAction.class);
        if (customizeAction.isEnabled ()) {
            JInlineMenu mi = new JInlineMenu ();
            mi.setMenuItems (new JMenuItem [] {
                                 new Actions.MenuItem (customizeAction, false),
                                 prop
                             });
            return mi;
        } else {
            return prop;
        }
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(PropertiesAction.class, "Properties");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (PropertiesAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/properties.gif"; // NOI18N
    }

    /** Overrides superclass method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }
    
  
    /** Delegate action for clonned context. Used to provide a special
     * support for getPopupPresenter.
     */
    private static final class DelegateAction 
    implements javax.swing.Action, Presenter.Menu, Presenter.Toolbar, Presenter.Popup {
        /** action to delegate to */
        private Action delegate;
        /** lookup we try to work in */
        private Lookup lookup;
        
        public DelegateAction (Action a, Lookup actionContext) {
            if (! (a instanceof Presenter.Menu)) {
                throw new IllegalStateException ("Should be menu presenter: " + a); // NOI18N
            }
            if (! (a instanceof Presenter.Toolbar)) {
                throw new IllegalStateException ("Should be toolbar presenter: " + a); // NOI18N
            }
            this.delegate = a;
            
            this.lookup = actionContext;
        }
        
        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }
        
        /** Invoked when an action occurs.
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            delegate.actionPerformed (e);
        }
        
        public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
            delegate.addPropertyChangeListener (listener);
        }

        public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
            delegate.removePropertyChangeListener (listener);
        }
        
        public void putValue(String key, Object o) {}
        
        public Object getValue(String key) {
            return delegate.getValue (key);
        }
        
        public boolean isEnabled() {
            return delegate.isEnabled ();
        }
        
        public void setEnabled(boolean b) {
            delegate.setEnabled (b);
        }
        
        public javax.swing.JMenuItem getMenuPresenter() {
            return ((Presenter.Menu)delegate).getMenuPresenter ();
        }
        
        public JMenuItem getPopupPresenter() {
            JMenuItem prop = new Actions.MenuItem (delegate, false);

            javax.swing.Action customizeAction =
                (CustomizeAction)SystemAction.get(CustomizeAction.class);

            // Retrieve context sensitive action instance if possible.
            if (lookup != null && customizeAction instanceof ContextAwareAction) {
                customizeAction = ((ContextAwareAction)customizeAction)
                                        .createContextAwareInstance(lookup);
            }
            
            
            if (customizeAction.isEnabled ()) {
                JInlineMenu mi = new JInlineMenu ();
                mi.setMenuItems (new JMenuItem [] {
                                     new Actions.MenuItem (customizeAction, false),
                                     prop
                                 });
                return mi;
            } else {
                return prop;
            }
            
        }
        
        public java.awt.Component getToolbarPresenter() {
            return ((Presenter.Toolbar)delegate).getToolbarPresenter ();
        }
        
    } // end of FilterPopupAction    
}
