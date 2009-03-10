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

import java.beans.PropertyChangeListener;
import javax.swing.event.*;
import org.openide.DialogDisplayer;

import org.openide.util.datatransfer.NewType;
import org.openide.util.HelpCtx;
import org.openide.util.actions.*;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/** Creates a new child of the activated node, if appropriate.
* @see Node#getNewTypes
*
* @author   Petr Hamernik, Ian Formanek
*/
public final class NewAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 5569219524388004456L;
    /** Imlementation of ActSubMenuInt */
    private static ActSubMenuModel model = new ActSubMenuModel(null);

    protected void performAction (Node[] activatedNodes) {
        performAction (activatedNodes, 0);
    }

    /** Performs action on index and nodes.
    */
    private static void performAction (Node[] activatedNodes, int indx) {
        NewType[] types = getNewTypes (activatedNodes);
        if (types.length <= indx) {
            return;
        }
        
        performAction (activatedNodes, types[indx]);
    }

    /** Performs action on given type
     */
    private static void performAction (Node[] activatedNodes, NewType type) {
        PasteAction.NodeSelector sel = null;
        try {
            ExplorerManager em = PasteAction.findExplorerManager();
            if (em != null) {
                sel = new PasteAction.NodeSelector (em, activatedNodes);
            }
            type.create();
        } catch (java.io.IOException e) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(e.getMessage(), NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            if (sel != null) {
                sel.select ();
            }
        }
    }

    /** Getter for array of activated new types.
    */
    private static  NewType[] getNewTypes () {
        return getNewTypes (
                   WindowManager.getDefault().getRegistry ().getCurrentNodes ()
               );
    }

    /** Getter for array of activated new types.
    */
    private static  NewType[] getNewTypes (Node[] activatedNodes) {
        if (activatedNodes == null || activatedNodes.length != 1) {
            return new NewType[0];
        } else {
            return activatedNodes[0].getNewTypes ();
        }
    }

    protected boolean enable (Node[] activatedNodes) {
        NewType[] types = getNewTypes ();

        // notify listeners
        Object[] listeners = model.getListenerList();
        if (listeners.length > 0) {
            ChangeEvent ev = new ChangeEvent (model);
            for (int i = listeners.length-1; i>=0; i-=2) {
                ((ChangeListener)listeners[i]).stateChanged (ev);
            }
        }

        return (types.length > 0);
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return createName(getNewTypes());
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (NewAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/new.gif"; // NOI18N
    }

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a MenuBar.
    * @return the JMenuItem representation for the Action
    */
    public javax.swing.JMenuItem getMenuPresenter() {
        return new Actions.SubMenu(this, model, false);
    }

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a PopuMenu.
    * @return the JMenuItem representation for the Action
    */
    public javax.swing.JMenuItem getPopupPresenter() {
        return new Actions.SubMenu(this, model, true);
    }
    
    /** Overrides superclass method. */
    public javax.swing.Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }

    /** Utility method, creates name for action depending on specified new types. */
    private static String createName(NewType[] newTypes) {
        if ((newTypes != null) && (newTypes.length == 1)) {
            return NbBundle.getMessage(NewAction.class, "NewArg", newTypes[0].getName());
        } else {
            return NbBundle.getMessage(NewAction.class, "New");
        }
    }


    /** Implementation of ActSubMenuInt */
    private static class ActSubMenuModel extends EventListenerList implements Actions.SubMenuModel {
        static final long serialVersionUID =-4273674308662494596L;
        
        /** lookup to read the new types from or null if they whould be taken 
         * directly from top component's selected nodes
         */
        private Lookup lookup;

	ActSubMenuModel (Lookup lookup) {
            this.lookup = lookup;
        }
        
        private NewType[] newTypes () {
            if (lookup != null) {
                java.util.Collection c = lookup.lookup (
                    new Lookup.Template (Node.class)
                ).allItems ();
                if (c.size () == 1) {
                    java.util.Iterator it = c.iterator();
                    while (it.hasNext()) {
                        Lookup.Item item = (Lookup.Item)it.next ();
                        Node n = (Node)item.getInstance();
                        if (n != null) {
                            return n.getNewTypes();
                        }
                    }
                }
            }
            return getNewTypes ();
        }
	
        public int getCount() {
            return newTypes ().length;
        }

        public String getLabel(int index) {
            NewType[] newTypes = newTypes();
            if (newTypes.length <= index)
                return null;
            else
                return newTypes[index].getName();
        }

        public HelpCtx getHelpCtx (int index) {
            NewType[] newTypes = newTypes();
            if (newTypes.length <= index)
                return null;
            else
                return newTypes[index].getHelpCtx();
        }

        public void performActionAt(int index) {
            NewType[] nt = newTypes ();
            if (nt.length < index) {
                return;
            }
            
            Node[] arr;
            if (lookup != null) {
                arr = (Node[])lookup.lookup (new Lookup.Template (Node.class)).allInstances ().toArray (new Node[0]);
            } else {
                arr = WindowManager.getDefault().getRegistry ().getCurrentNodes ();
            }
            
            performAction (arr, nt[index]);
        }

        /** Adds change listener for changes of the model.
        */
        public void addChangeListener (ChangeListener l) {
            add (ChangeListener.class, l);
        }

        /** Removes change listener for changes of the model.
        */
        public void removeChangeListener (ChangeListener l) {
            remove (ChangeListener.class, l);
        }

    } // end of ActSubMenuModel
    
    /** A delegate action that is usually associated with a specific lookup and
     * extract the nodes it operates on from it. Otherwise it delegates to the
     * regular NodeAction.
     */
    private static final class DelegateAction extends Object
    implements javax.swing.Action, 
    Presenter.Menu, Presenter.Popup {
        /** Action to delegate to. */
        private final NodeAction delegate;
        /** Associated model to use. */
        private final ActSubMenuModel model;
        
        public DelegateAction (NodeAction a, Lookup actionContext) {
            this.delegate = a;
            this.model = new ActSubMenuModel(actionContext);
        }
        

        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }
        
        /** Invoked when an action occurs.
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            model.performActionAt(0);
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

        public void putValue(String key, Object o) {}
        
        public Object getValue(String key) {
            if(javax.swing.Action.NAME.equals(key)) {
                // #30266 Name of action depends on new types.
                return createName(model.newTypes());
            } else {
                return delegate.getValue(key);
            }
        }
        
        public boolean isEnabled() {
            return model.getCount() > 0;
        }
        
        public void setEnabled(boolean b) {
        }

        public javax.swing.JMenuItem getMenuPresenter() {
            return new Actions.SubMenu(this, model, false);
        }
        
        public javax.swing.JMenuItem getPopupPresenter() {
            return new Actions.SubMenu(this, model, true);
        }
    } // end of DelegateAction    
}
