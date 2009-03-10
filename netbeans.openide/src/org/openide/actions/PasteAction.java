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

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.beans.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.event.*;

import org.openide.util.datatransfer.*;
import org.openide.util.HelpCtx;
import org.openide.util.actions.*;
import org.openide.awt.Actions;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.NodeListener;
import org.openide.util.UserCancelException;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;


/** Paste from clipboard. This is a callback system action,
* with enhanced behaviour. Others can plug in by adding 
* <PRE>
* topcomponent.getActionMap ().put (javax.swing.text.DefaultEditorKit.pasteAction, theActualAction);
* </PRE>
* or by using the now deprecated <code>setPasteTypes</code> and <code>setActionPerformer</code>
* methods. 
* <P>
* There is a special support for more than one type of paste to be enabled at once.
* If the <code>theActualAction</code> returns array of actions from
* <code>getValue ("delegates")</code> than those actions are offered as
* subelements by the paste action presenter.
*/
public final class PasteAction extends CallbackSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -6620328110138256516L;
    /** Imlementation of ActSubMenuInt */
    private static ActSubMenuModel globalModel;

    /** All currently possible paste types. */
    private static PasteType[] types;
    
    /** Lazy initializtion of the global model */
    private static synchronized ActSubMenuModel model () {
        if (globalModel == null) {
            globalModel = new ActSubMenuModel (null);
        }
        return globalModel;
    }

    /* Overrides superclass initialization */
    protected void initialize () {
        super.initialize();
        
        setEnabled (false);
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(PasteAction.class, "Paste");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (PasteAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/paste.gif"; // NOI18N
    }

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a MenuBar.
    * @return the JMenuItem representation for the Action
    */
    public javax.swing.JMenuItem getMenuPresenter() {
        return new Actions.SubMenu(this, model (), false);
    }

    /* Returns a JMenuItem that presents the Action, that implements this
    * interface, in a PopupMenu.
    * @return the JMenuItem representation for the Action
    */
    public javax.swing.JMenuItem getPopupPresenter() {
        return new Actions.SubMenu(this, model (), true);
    }
    
    /** Overrides superclass method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }
    
    /** Gets action map key, overrides superclass method.
     * @return key used to find an action from context's ActionMap */
    public Object getActionMapKey() {
        return javax.swing.text.DefaultEditorKit.pasteAction;
    }

    public void actionPerformed(java.awt.event.ActionEvent ev) {
        PasteType t;
        if (ev.getSource() instanceof PasteType) {
            t = (PasteType)ev.getSource ();
        } else {
            PasteType[] arr = getPasteTypes ();
            if (arr != null && arr.length > 0) {
                t = arr[0];
            } else {
                t = null;
            }
        }
        
        if (t == null) {
            // Try to find paste action 'performer' from activated TopComponent.
            Action ac = findActionFromActivatedTopComponentMap();
            if(ac != null) {
                // XXX Hack to get paste types from action 'performer',
                // which in fact doesn't perform the paste.
                // Look at ExplorerActions.OwnPaste#getValue method.
                PasteType[] arr = (PasteType[])ac.getValue("delegates"); // NOI18N
                if(arr != null && arr.length > 0) {
                    t = arr[0];
                } else {
                    ac.actionPerformed(ev);
                    return;
                }
            }
        }

        if(t != null) {
            executePasteType (t);
        } else {
            ErrorManager.getDefault().notify(
                ErrorManager.INFORMATIONAL,
                new IllegalStateException(
                    "No paste types available when performing paste action")); // NOI18N
        }
    }

    /** Does the execution of a paste type with all handling around
     */
    private static void executePasteType (PasteType t) {
        NodeSelector sel = null;
        try {
            ExplorerManager em = findExplorerManager ();
            if (em != null) {
                sel = new NodeSelector (em, null);
            }
            
            Transferable trans = t.paste();
            Clipboard clipboard = getClipboard();


            if (trans != null) {
                ClipboardOwner owner = trans instanceof ClipboardOwner ?
                                       (ClipboardOwner)trans
                                       :
                                       new StringSelection (""); // NOI18N
                clipboard.setContents(trans, owner);
            }
        } catch (UserCancelException exc) {
            // ignore - user just pressed cancel in some dialog....
        } catch (java.io.IOException e) {
            ErrorManager.getDefault().notify(e);
        } finally {
            if (sel != null) {
                sel.select ();
            }
        }
    }

    /** Set possible paste types.
    * Automatically enables or disables the paste action according to whether there are any.
    * @param types the new types to allow, or <code>null</code>
    */
    public void setPasteTypes(PasteType[] types) {
        this.types = types;
        if ((types == null) || (types.length == 0)) {
            setEnabled(false);
        }
        else {
            setEnabled(true);
        }
        model ().checkStateChanged (true);
    }

    /** Get all paste types.
     * @return all possible paste types, or <code>null</code> */
    public PasteType[] getPasteTypes() {
        return types;
    }

    /** Finds paste action from currently activated TopComponent's action map. */
    private static Action findActionFromActivatedTopComponentMap() {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        if (tc != null) {
            ActionMap map = tc.getActionMap ();
            return findActionFromMap(map);
        }
        
        return null;
    }
    
    /** Finds paste action from provided map. */
    private static javax.swing.Action findActionFromMap (ActionMap map) {
        if (map != null) {
            return map.get (javax.swing.text.DefaultEditorKit.pasteAction);
        }
        
        return null;
    }
    
    /** If our clipboard is not found return the default system clipboard. */
    private static Clipboard getClipboard() {
        Clipboard c = (java.awt.datatransfer.Clipboard)
            org.openide.util.Lookup.getDefault().lookup(java.awt.datatransfer.Clipboard.class);
        if (c == null) {
            c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        return c;
    }

    /** General implementation of Actions.SubMenuModel that works 
     * with provided lookup or without it. With lookup it attaches
     * to changes in the lookup and updates its state according to 
     * it. Without it listens on TopComponent.getActivated() and 
     * works with it.
     */
    private static class ActSubMenuModel extends EventListenerList 
    implements Actions.SubMenuModel, LookupListener, PropertyChangeListener {
        /** lookup we are attached to or null we we should work globally */
        private Lookup.Result result;
        /** previous enabled state */
        private boolean enabled;
        /** our weak listener */
        private PropertyChangeListener weakL;

        /** @param lookup can be null */
        public ActSubMenuModel (Lookup lookup) {
            weakL = org.openide.util.WeakListener.propertyChange(this, null);
            attachListenerToChangesInMap (lookup);
        }
        
        /** Finds appropriate map to work with.
         * @return map from lookup or from activated TopComponent, null no available
         */
        private ActionMap map () {
            if (result == null) {
                org.openide.windows.TopComponent tc = org.openide.windows.TopComponent.getRegistry().getActivated();
                if (tc != null) {
                    return tc.getActionMap ();
                }
            } else {
                java.util.Iterator it = result.allItems ().iterator ();
                while (it.hasNext()) {
                    Object o = ((Lookup.Item)it.next ()).getInstance ();
                    if (o instanceof ActionMap) {
                        return (ActionMap)o;
                    }
                }
            }
            
            return null;
        }

        /** Adds itself as a listener for changes in current ActionMap.
         * If the lookup is null then it means to listen on TopComponent
         * otherwise to listen on the lookup itself.
         *
         * @param lookup lookup to listen on or null
         */
        private void attachListenerToChangesInMap (Lookup lookup) {
            if (lookup == null) {
                org.openide.windows.TopComponent.getRegistry().addPropertyChangeListener(
                    org.openide.util.WeakListener.propertyChange (
                        this, org.openide.windows.TopComponent.getRegistry()
                    )
                );
            } else {
                result = lookup.lookup (new Lookup.Template (ActionMap.class));
                result.addLookupListener (this);
            }

            checkStateChanged (false);
        }
        
        /** Finds the currently active items this method should delegate to.
         * For historical reasons one can use PasteType by PasteAction.setPasteTypes
         * in the new implementation it is expected that such paste types 
         * will be replaced by Actions (obtained from getValue("delegates")).
         * 
         *
         * @param actionToWorkWith array of size 1 or null. Will be filled 
         *   with action that we actually delegate to (either the global or local
         *   found in action map)
         * @return array of either PasteTypes or Actions
         */
        private Object[] getPasteTypesOrActions (Action[] actionToWorkWith) {
            Action x = findActionFromMap(map ());
            if (x == null) {
                // No context action use the global one.
                PasteAction a = (PasteAction)findObject (PasteAction.class);
                if (actionToWorkWith != null) {
                    actionToWorkWith[0] = a;
                }

                Object[] arr = a.getPasteTypes();
                if (arr != null) {
                    return arr;
                } else {
                    return new Object[0];
                }
            }
            
            if (actionToWorkWith != null) {
                actionToWorkWith[0] = x;
            }
            
            Object obj = x.getValue ("delegates"); // NOI18N
            
            if (obj instanceof Object[]) {
                return (Object[])obj;
            } else {
                return new Object[] { x };
            }
        }
        
        public boolean isEnabled() {
            Object[] arr = getPasteTypesOrActions(null);
            if(arr.length == 1 && arr[0] instanceof Action) {
                return ((Action)arr[0]).isEnabled();
            } else {
                return arr.length > 0;
            }
        }
        
        public int getCount() {
            return getPasteTypesOrActions (null).length;
        }

        public String getLabel(int index) {
            Object[] arr = getPasteTypesOrActions (null);
            if (arr.length <= index) {
                return null;
            }
            
            if (arr[index] instanceof PasteType) {
                return ((PasteType)arr[index]).getName();
            } else {
                // is Action
                return (String) ((Action)arr[index]).getValue(Action.NAME);
            }
        }

        public HelpCtx getHelpCtx (int index) {
            Object[] arr = getPasteTypesOrActions (null);
            if (arr.length <= index) {
                return null;
            }

            if (arr[index] instanceof PasteType) {
                return ((PasteType)arr[index]).getHelpCtx ();
            } else {
                // is action
                Object helpID = ((Action)arr[index]).getValue ("helpID"); // NOI18N
                if(helpID instanceof String) {
                    return new HelpCtx((String)helpID);
                } else {
                    return null;
                }
            }
        }

        public MenuShortcut getMenuShortcut(int index) {
            return null;
        }

        public void performActionAt(int index) {
            Action[] action = new Action[1];
            
            Object[] arr = getPasteTypesOrActions (action);
            if (arr.length <= index) {
                return;
            }

            if (arr[index] instanceof PasteType) {
                PasteType t = (PasteType)arr[index];
                invokeAction(new ActionPT (t),new ActionEvent (t,ActionEvent.ACTION_PERFORMED, javax.swing.Action.NAME));
                return;
            } else {
                // is action
                Action a = (Action)arr[index];
                invokeAction (a, new ActionEvent (a, ActionEvent.ACTION_PERFORMED, a.NAME));
                return;
            }
        }


        /** Registers .ChangeListener to receive events.
         *@param listener The listener to register.
         */
        public synchronized void addChangeListener(javax.swing.event.ChangeListener listener) {
            add (javax.swing.event.ChangeListener.class, listener);
        }
        /** Removes .ChangeListener from the list of listeners.
         *@param listener The listener to remove.
         */
        public synchronized void removeChangeListener(javax.swing.event.ChangeListener listener) {
            remove (javax.swing.event.ChangeListener.class, listener);
        }
        /** Notifies all registered listeners about the event.
         *
         *@param param1 Parameter #1 of the <CODE>.ChangeEvent<CODE> constructor.
         */
        protected void checkStateChanged(boolean fire) {
            Action[] listen = new Action[1];
            Object[] arr = getPasteTypesOrActions(listen);

            Action a = null;
            if (arr.length == 1 && arr[0] instanceof Action) {
                a = (Action)arr[0];
                a.removePropertyChangeListener(weakL);
                a.addPropertyChangeListener(weakL);
            }
            // plus always make sure we are listening on the actions
            if (listen[0] != a) {
                listen[0].removePropertyChangeListener(weakL);
                listen[0].addPropertyChangeListener (weakL);
            }
            
            boolean en = isEnabled ();
            if (en == enabled) {
                return;
            }
            
            enabled = en;
            // and fire if requested....
            
            if (!fire) {
                return;
            }
            
            Object[] listeners = getListenerList ();
            if (listeners.length == 0) {
                return;
            }
            javax.swing.event.ChangeEvent e = new javax.swing.event.ChangeEvent (
                                                  this
                                              );

            for (int i = listeners.length-1; i>=0; i-=2) {
                ((javax.swing.event.ChangeListener)listeners[i]).stateChanged (e);
            }
        }
        
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            checkStateChanged (true);
        }
        
        public void resultChanged(org.openide.util.LookupEvent ev) {
            checkStateChanged (true);
        }
        
    }
    /** Utility method for invoking actions in separate thread. Note:
     * it uses reflection because it should work without
     * the rest of the IDE classes.
     */
    static void invokeAction(Action sa, ActionEvent ev) {
        Throwable t = null;
        try {
            Class c = Class.forName("org.openide.actions.ActionManager"); // NOI18N
            Object o = org.openide.util.Lookup.getDefault ().lookup(c);
            if (o != null) {
                // lookup has found the instance
                // use reflection now
                java.lang.reflect.Method m = c.getMethod("invokeAction", // NOI18N
                    new Class[] {
                        javax.swing.Action.class,
                        java.awt.event.ActionEvent.class });
                m.invoke(o, new Object[] { sa, ev } );
                // everything went ok -->
                return;
            }
        }
        // exceptions from forName:
        catch (ClassNotFoundException x) { }
        catch (ExceptionInInitializerError x) { }
        catch (LinkageError x) {  }
        // exceptions from getMethod:
        catch (SecurityException x) { t = x; }
        catch (NoSuchMethodException x) { t = x;}
        // exceptions from invoke
        catch (IllegalAccessException x) { t = x;}
        catch (IllegalArgumentException x) { t = x;}
        catch (java.lang.reflect.InvocationTargetException x) {
            t = x;
        }
        
        if (t != null) {
            ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, t);
        }
        // something went wrong --> invoke the action directly
        sa.actionPerformed(ev);
    }
    
    /** Utilitity method for finding the currently selected explorer manager.
     * it uses reflection because it should work without
     * the rest of the IDE classes.
     *
     * @return current explorer manager or null
     */
    static ExplorerManager findExplorerManager () {
        Throwable t = null;
        try {
            Class c = Class.forName("org.openide.windows.TopComponent"); // NOI18N
            // use reflection now
            java.lang.reflect.Method m = c.getMethod("getRegistry", // NOI18N
                new Class[0]
            );
            Object o = m.invoke(null, new Object[0] );
            
            c = Class.forName("org.openide.windows.TopComponent$Registry"); // NOI18N
            // use reflection now
            m = c.getMethod("getActivated", // NOI18N
                new Class[0] 
            );
            o = m.invoke (o, new Object[0]);
            
            if (o instanceof ExplorerManager.Provider) {
                return ((ExplorerManager.Provider)o).getExplorerManager();
            }
            
        }
        // exceptions from forName:
        catch (ClassNotFoundException x) { }
        catch (ExceptionInInitializerError x) { }
        catch (LinkageError x) {  }
        // exceptions from getMethod:
        catch (SecurityException x) { t = x; } 
        catch (NoSuchMethodException x) { t = x;}
        // exceptions from invoke
        catch (IllegalAccessException x) { t = x;} 
        catch (IllegalArgumentException x) { t = x;} 
        catch (java.lang.reflect.InvocationTargetException x) {
            t = x;
        }
        
        if (t != null) {
            ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, t);
        }
        
        return null;
    }
    
    /** Class that listens on a given node and when invoked listen on changes 
     * and after that tries to select the desired node.
     */
    static final class NodeSelector extends Object 
    implements NodeListener, Runnable {
        /** All added children */
        private ArrayList added;
        /** node we are listening to */
        private Node node;
        /** manager to work with */
        private ExplorerManager em;
        /** children */
        private Node[] children;

        /** @param em explorer manager to work with
         * @param n nodes to attach to or null if em's nodes should be used
         */
        public NodeSelector (ExplorerManager em, Node[] n) {
            this.em = em;
            if (n != null && n.length > 0) {
                this.node = n[0];
            } else {
                Node[] arr = em.getSelectedNodes ();
                if (arr.length != 0) {
                    this.node = arr[0];
                } else {
                    // do not initialize
                    return;
                }
            }
                
            this.children = node.getChildren().getNodes(true);
            
            this.added = new ArrayList ();
            this.node.addNodeListener (this);
        }
        
        /** Selects the added nodes */
        public void select () {
            if (added != null) {
                // if initialized => wait till finished update
                node.getChildren().getNodes(true);
                // and select the right nodes
                org.openide.nodes.Children.MUTEX.readAccess (this);
            }
        }
        
        public void run () {
            
            this.node.removeNodeListener (this);

            if (added.isEmpty()) {
                return;
            }
            
            Node[] arr = (Node[])added.toArray (new Node[0]);

            // bugfix #22698, don't select the added nodes
            // when the nodes not under managed explorer's root node
            bigloop: for (int i = 0; i < arr.length; i++) {
                Node node = arr[i];
                while (node != null) {
                    if (node.equals(em.getRootContext ())) {
                        continue bigloop;
                    }
                    node = node.getParentNode ();
                }
                return;
            }
                
            
            try {
                em.setSelectedNodes (arr);
            } catch (PropertyVetoException ex) {
                ErrorManager.getDefault().notify (ErrorManager.INFORMATIONAL, ex);
            } catch (IllegalStateException ex) {
                ErrorManager.getDefault().notify (ErrorManager.INFORMATIONAL, ex);
            }
        }
            
            
        
        
        /** Fired when a set of new children is added.
         * @param ev event describing the action
         */
        public void childrenAdded(NodeMemberEvent ev) {
            added.addAll (Arrays.asList (ev.getDelta()));
        }        
        
        /** Fired when a set of children is removed.
         * @param ev event describing the action
         */
        public void childrenRemoved(NodeMemberEvent ev) {
        }
        
        /** Fired when the order of children is changed.
         * @param ev event describing the change
         */
        public void childrenReordered(NodeReorderEvent ev) {
        }
        
        /** Fired when the node is deleted.
         * @param ev event describing the node
         */
        public void nodeDestroyed(NodeEvent ev) {
        }
        
        /** This method gets called when a bound property is changed.
         * @param evt A PropertyChangeEvent object describing the event source
         *   	and the property that has changed.
         */
        public void propertyChange(PropertyChangeEvent evt) {
        }
        
    } // end of NodeSelector
    
    /** A delegate action that is usually associated with a specific lookup and
     * extract the nodes it operates on from it. Otherwise it delegates to the
     * regular NodeAction.
     */
    private static final class DelegateAction extends javax.swing.AbstractAction
    implements Presenter.Menu, Presenter.Popup, Presenter.Toolbar, javax.swing.event.ChangeListener {
        /** action to delegate too */
        private PasteAction delegate;
        /** model to work with */
        private ActSubMenuModel model;
        
        public DelegateAction (PasteAction a, Lookup actionContext) {
            this.delegate = a;
            this.model = new ActSubMenuModel (actionContext);
            this.model.addChangeListener(this);
        }

        
        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }
        
        public void putValue(String key, Object value) { }
        
        /** Invoked when an action occurs.
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (model != null) {
                model.performActionAt(0);
            }
        }
        
        public boolean isEnabled() {
            return model != null && model.isEnabled();
        }
        
        public Object getValue(String key) {
            return delegate.getValue(key);
        }
        
        public void setEnabled(boolean b) {
        }

        public javax.swing.JMenuItem getMenuPresenter() {
            return new org.openide.awt.Actions.SubMenu (this, model, false);
        }
        
        public javax.swing.JMenuItem getPopupPresenter() {
            return new org.openide.awt.Actions.SubMenu (this, model, true);
        }
        
        public java.awt.Component getToolbarPresenter() {
            return new Actions.ToolbarButton (this);
        }
        
        public void stateChanged (javax.swing.event.ChangeEvent evt) {
            super.firePropertyChange("enabled", null, null);
        }
    } // end of DelegateAction    
    
    /** Action that wraps paste type.
     */
    private static final class ActionPT extends javax.swing.AbstractAction {
        private PasteType t;
        
        public ActionPT (PasteType t) {
            this.t = t;
        }
        
        public void actionPerformed (java.awt.event.ActionEvent ev) {
            executePasteType (t);
        }
    }
}
