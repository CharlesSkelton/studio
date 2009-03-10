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

import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import org.openide.actions.ActionManager;

import org.openide.awt.JInlineMenu;
import org.openide.util.ContextAwareAction;
import org.openide.util.NbBundle;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.actions.*;

/** A "meta-action" that displays (in a submenu) a list of enabled actions provided by modules.
* Such registered actions are called "service actions":
* they are provided externally but seem to provide additional services on existing components.
* Often they will be {@link NodeAction}s or {@link CookieAction}s so that they will
* be enabled based on the node selection, i.e. the node containing this popup.
* It is desirable for most nodes to include this action somewhere in their popup menu.
*
* <p><em>Note:</em> you do not need to touch this class to add a service action!
* Just add the action to a module manifest in an <code>Action</code> section.
*
* <p>The list of registered service actions is provided to this action from the implementation
* by means of {@link ActionManager}.
*
* @author Jaroslav Tulach
*/
public class ToolsAction extends SystemAction
implements ContextAwareAction, Presenter.Menu, Presenter.Popup {
    static final long serialVersionUID =4906417339959070129L;
    
        
    // Global ActionManager listener monitoring all available actions
    // and their state
    private static G gl;
    
    /** Lazy initialization of global listener.
     */
    private static synchronized G gl () {
        if (gl == null) {
            gl = new G ();
        }
        return gl;
    }
    
    /* @return name
    */
    public String getName () {
        return getActionName ();
    }

    /* @return help for this action
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (ToolsAction.class);
    }

    /* @return menu presenter for the action
    */
    public JMenuItem getMenuPresenter () {
        return new Inline(this);
    }

    /* @return menu presenter for the action
    */
    public JMenuItem getPopupPresenter () {
        return new Popup(this);
    }

    /* Does nothing.
    */
    public void actionPerformed (java.awt.event.ActionEvent ev) {
    }

    /** Implements <code>ContextAwareAction</code> interface method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }

    /* @return name
    */
    private static String getActionName () {
        return NbBundle.getMessage(ToolsAction.class, "CTL_Tools");
    }

    /** Implementation method that regenerates the items in the menu or
    * in the array.
    *
    * @param forMenu true if Presenter.Menu should be used false if Presenter.Popup
    * @param list (can be null)
    */
    private static List generate (Action toolsAction, boolean forMenu) {
        ActionManager am = (ActionManager)Lookup.getDefault().lookup(ActionManager.class);
        SystemAction[] actions = am.getContextActions ();
        List list = new ArrayList( actions.length );

        boolean separator = false;
        boolean firstItemAdded = false; // flag to prevent adding separator before actual menu items

        // Get action context.
        Lookup lookup;
        if(toolsAction instanceof Lookup.Provider) {
            lookup = ((Lookup.Provider)toolsAction).getLookup();
        } else {
            lookup = null;
        }

        for (int i = 0; i < actions.length; i++) {
            Action a;
            // Retrieve context sensitive action instance if possible.
            if(lookup != null && actions[i] instanceof ContextAwareAction) {
                a = ((ContextAwareAction)actions[i]).createContextAwareInstance(lookup);
            } else {
                a = actions[i];
            }
            
            if (a == null) {
                if (firstItemAdded) separator = true;
            } else if( forMenu ) {
                if( a instanceof Presenter.Menu && a.isEnabled () ) {
                    if (separator) {
                        list.add (null);
                        separator = false;
                    }

                    JMenuItem mi = ((Presenter.Menu)a).getMenuPresenter ();
                    list.add (mi);

                    firstItemAdded = true;
                }
            } else if( a instanceof Presenter.Popup && a.isEnabled() ) {
                if (separator) {
                    list.add (null);
                    separator = false;
                }

                JMenuItem mi = ((Presenter.Popup)a).getPopupPresenter ();
                list.add (mi);

                firstItemAdded = true;
            }
        }
        return list;
    }    
    
    
    /** Inline menu that watches model changes only when really needed.
     */
    private static final class Inline extends JInlineMenu
        implements PropertyChangeListener, Runnable {
        static final long serialVersionUID =2269006599727576059L;

	/** timestamp of the beginning of the last regeneration */
	private int timestamp = 0;
        
        /** Associated tools action. */
        private Action toolsAction;

	Inline(Action toolsAction) {
            this.toolsAction = toolsAction;
            putClientProperty("hack.preShowUpdater", this);
        }

        /** By calling this method, our parent notifies us we've to be keep
         * updated, so we start listening on SystemAction changes, and 
         * schedule updating Runnable imediately. 
         */
        public void addNotify() {
            // We were not notified by our parent, too bad
	    if(timestamp != gl().getTimestamp()) SwingUtilities.invokeLater(this);
            
            gl ().addPropertyChangeListener( this );
            super.addNotify();
        }

        /** By calling this method, our parent notifies us we don't have
         * to be up-to-date more, so we switch to lazy mode and discard any
         * pending updates.
         */
        public void removeNotify() {
            gl ().removePropertyChangeListener( this );
            super.removeNotify();
        }
        
        /** Change of model.
        */
        public void propertyChange (PropertyChangeEvent ev) {
            String prop = ev.getPropertyName ();
            if (prop == null || prop.equals (G.PROP_STATE)) {
                SwingUtilities.invokeLater (this);
            }
        }


        /** Runs the update */
        public void run () {
	    if( timestamp == gl ().getTimestamp() ) return;

            // generate directly list of menu items
            List l = generate (toolsAction,  true);
            setMenuItems ((JMenuItem[])l.toArray(new JMenuItem[l.size ()]));
	    timestamp = gl ().getTimestamp();
        }
    }

    
    
    //--------------------------------------------------
    
    /** Inline menu that is either empty or contains one submenu.*/
    private static final class Popup extends JInlineMenu implements Runnable {

        /** A special menu that will properly update its submenu before posting */
        private class MyMenu extends org.openide.awt.JMenuPlus implements PopupMenuListener {

            /* A popup menu we've attached our listener to.
             * If null, the content is not up-to-date */
            private JPopupMenu lastPopup = null;

            MyMenu() {
                super(getActionName());
            }

            public JPopupMenu getPopupMenu() {
                JPopupMenu popup = super.getPopupMenu();
                fillSubmenu(popup);
                return popup;
            }
            
            private void fillSubmenu(JPopupMenu pop) {
                if (lastPopup == null) {
                    pop.addPopupMenuListener(this);
                    lastPopup = pop;

                    removeAll ();
                    Iterator it = generate(toolsAction, false).iterator();
                    while( it.hasNext() ) {
                        java.awt.Component item = (java.awt.Component)it.next();
                        if( item == null ) {
                            addSeparator ();
                        } else {
                            add ( item );
                        }
                    }
                    
                    // also work with empty element
                    if(getMenuComponentCount () == 0) {
                        JMenuItem empty = new JMenuItem(NbBundle.getMessage(
                                ToolsAction.class, "CTL_EmptySubMenu"));
                        empty.setEnabled(false);
                        add(empty);
                    }
                }
            }

            public void popupMenuCanceled(PopupMenuEvent e) {}
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {} 
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                lastPopup.removePopupMenuListener(this);
                lastPopup = null; // clear the status and stop listening
            }
        }
        
        static final long serialVersionUID =2269006599727576059L;

        /** sub menu */
        private JMenu menu = new MyMenu();

        private boolean inited = false;
        
        /** Associated tools action. */
        private Action toolsAction;

        public Popup (Action toolsAction) {
            super();
            this.toolsAction = toolsAction;
            HelpCtx.setHelpIDString (menu, ToolsAction.class.getName ());
            
// This can be probably swapped as the popup is short time entity and our hack
// will be called just once and very early after the constructor.
//            run();
            putClientProperty("hack.preShowUpdater", this);
        }
     
        public void addNotify() {
            if (!inited) { // should not happen
                SwingUtilities.invokeLater (this);  // too late to do it here
            }

            super.addNotify();
        }

        /** Runs the update */
        public void run () {
            if(!inited) {
                setMenuItems (
                    gl().isPopupEnabled(toolsAction)
                        ? new JMenuItem[] { menu }
                        : new JMenuItem[0] // Tools action won't be seen.
                );
                inited = true;
            }
        }
    }

    //------------------------------------------
    
    
    
    /** @deprecated Useless, see {@link ActionManager}. */
    public static void setModel (Model m) {
        throw new SecurityException ();
    }
    /** @deprecated Useless, see {@link ActionManager}. */
    public static interface Model {
        public SystemAction[] getActions ();
        public void addChangeListener (javax.swing.event.ChangeListener l);
        public void removeChangeListener (javax.swing.event.ChangeListener l);
    }


    
    //------------------------------------------------

    
    //----------------------------------------------------------
    
        
    
    

    private static class G implements PropertyChangeListener {
	private int timestamp = 1;
	
        private SystemAction[] actions = null;
        private PropertyChangeSupport supp = new PropertyChangeSupport( this );
        
        public static final String PROP_STATE = "actionsState"; // NOI18N
        
        public G() {
            ActionManager am = (ActionManager)Lookup.getDefault().lookup(ActionManager.class);
            am.addPropertyChangeListener ( this );
            actionsListChanged();
        }
        
        public final void addPropertyChangeListener(
                PropertyChangeListener listener
        ) {
            supp.addPropertyChangeListener(listener);
        }
        
        public final void removePropertyChangeListener(
                PropertyChangeListener listener
        ) {
            supp.removePropertyChangeListener (listener);
        }
        
        protected final void firePropertyChange(
            String name , Object o, Object n
        ) {
            supp.firePropertyChange(name, o, n);
        }


        
        
        private void actionsListChanged() {
	    timestamp++;
            // deregister all actions listeners
            SystemAction[] copy = actions;
            if( copy != null ) {
                for( int i = 0; i< copy.length; i++ ) {
                    SystemAction act = copy[i];
                    if( act != null ) act.removePropertyChangeListener ( this );
                }
            }

            ActionManager am = (ActionManager)Lookup.getDefault().lookup(ActionManager.class);
            copy = am.getContextActions ();
            for (int i = 0; i < copy.length; i++) {
                SystemAction act = copy[i];
                if( act != null ) act.addPropertyChangeListener ( this );
            }
            actions = copy;

            firePropertyChange( PROP_STATE, null, null ); // tell the world
        }
        
        private void actionStateChanged() {
	    timestamp++;
            firePropertyChange( PROP_STATE, null, null );  // tell the world
        }
        
        public void propertyChange (PropertyChangeEvent ev) {
            String prop = ev.getPropertyName ();
            if (prop == null || prop.equals (ActionManager.PROP_CONTEXT_ACTIONS)) {
                actionsListChanged();
            } else if( prop.equals (SystemAction.PROP_ENABLED)) {
                actionStateChanged();
            }
        }

        
        /** Tells if there is any action that is willing to provide
         * Presenter.Popup
         */
        private boolean isPopupEnabled(Action toolsAction) {
            boolean en = false;
            SystemAction[] copy = actions;

            // Get action conext.
            Lookup lookup;
            if(toolsAction instanceof Lookup.Provider) {
                lookup = ((Lookup.Provider)toolsAction).getLookup();
            }  else {
                lookup = null;
            }

            for (int i=0; i<copy.length; i++) {
                // Get context aware action instance if needed.
                Action act;
                // Retrieve context aware action instance if possible.
                if(lookup != null && copy[i] instanceof ContextAwareAction) {
                    act = ((ContextAwareAction)copy[i]).createContextAwareInstance(lookup);
                } else {
                    act = copy[i];
                }

                if( act instanceof Presenter.Popup && act.isEnabled() ) {
                    en = true;
                    break;
                }
            }

            return en;
        }
	
	private int getTimestamp() {
	    return timestamp;
	}
	
    }
    
    
    /** Delegate tools action. Which act accordingly to current context
     * (represented by lookup). */
    private static final class DelegateAction extends Object
    implements Action, Presenter.Menu, Presenter.Popup, Lookup.Provider {
        private ToolsAction delegate;
        private Lookup lookup;
        /** support for listeners */
        private PropertyChangeSupport support = new PropertyChangeSupport (this);

        
        public DelegateAction(ToolsAction delegate, Lookup actionContext) {
            this.delegate = delegate;
            this.lookup = actionContext;
        }

        
        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }

        /** Implements <code>Lookup.Provider</code>. */
        public Lookup getLookup() {
            return lookup;
        }
        
        public void actionPerformed(java.awt.event.ActionEvent e) {
        }

        public void putValue(String key, Object o) {}
        
        public Object getValue(String key) {
            return delegate.getValue(key);
        }
        
        public boolean isEnabled() {
            // Irrelevant see G#isPopupEnabled(..).
            return delegate.isEnabled();
        }
        
        public void setEnabled(boolean b) {
            // Irrelevant see G#isPopupEnabled(..).
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener (listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener (listener);
        }

        /** Implements <code>Presenter.Menu</code>. */
        public javax.swing.JMenuItem getMenuPresenter() {
            return new Inline(this);
        }
        
        /** Implements <code>Presenter.Popup</code>. */
        public javax.swing.JMenuItem getPopupPresenter() {
            return new ToolsAction.Popup(this);
        }
    } // End of DelegateAction.

}
