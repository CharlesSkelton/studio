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
import java.beans.PropertyChangeEvent;
import java.util.*;
import javax.swing.Action;

import javax.swing.JMenuItem;


import org.openide.awt.JInlineMenu;
import org.openide.loaders.DataObject;
import org.openide.ErrorManager;
import org.openide.filesystems.*;
import org.openide.windows.TopComponent.Registry;
import org.openide.windows.WindowManager;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.actions.SystemAction;
import org.openide.util.actions.Presenter;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.enums.*;


/** Action that presents standard file system-related actions.
* Listens until a node representing a {@link DataObject}
* is selected and then retrieves {@link SystemAction}s from its
* {@link FileSystem}.
*
* @author  Jaroslav Tulach
* @version 0.10, Jun 16, 1998
*/

public class FileSystemAction extends SystemAction
implements ContextAwareAction, Presenter.Menu, Presenter.Popup {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -7572252564167491022L;
    /** empty array of menu items */
    static JMenuItem[] NONE = new JMenuItem[] {};

    /** computes the nodes.
     */
    private static Node[] nodes (Lookup lookup) {
        Collection c;

        if (lookup != null) {
            c = lookup.lookup (new Lookup.Template (Node.class)).allInstances();
        } else {
            c = java.util.Collections.EMPTY_LIST;
        }
        return (Node[])c.toArray(new Node[0]);
    }

    /** Creates menu for currently selected nodes.
    * @param popUp create popup or normal menu
    * @param n nodes to work with or null
    */
    static JMenuItem[] createMenu (boolean popUp, Lookup lookup) {
        Node[] n = nodes (lookup);
        
        if (n == null) {
            n = WindowManager.getDefault ().getRegistry ().getActivatedNodes ();
        }
        
        
        HashMap fsSet = new HashMap ();

        if (n != null) {
            for (int i = 0; i < n.length; i++) {
                 DataObject obj = (DataObject)n[i].getCookie (DataObject.class);
                 while (obj instanceof org.openide.loaders.DataShadow)
                     obj = ((org.openide.loaders.DataShadow)obj).getOriginal();
                 if (obj != null) {
                     try {
                         FileSystem fs = obj.getPrimaryFile ().getFileSystem ();
                         Set foSet = (Set) fsSet.get (fs);
                         if (foSet == null ) {
                             fsSet.put (fs, foSet = new FileSystemAction.OrderedSet ());
                         }
                         foSet.addAll(obj.files ());
                     } catch (FileStateInvalidException ex) {continue;}
                 }  
            }
            /* At present not allowed to construct actions for selected nodes on more filesystems - its safe behaviour
             * If this restriction will be considered as right solution, then code of this method can be simplified
             */
            if (fsSet.size () == 0 || fsSet.size() > 1) return createMenu (EmptyEnumeration.EMPTY, popUp, lookup);
            
            Iterator entrySetIt = fsSet.entrySet ().iterator();
            QueueEnumeration result = new QueueEnumeration();

            while (entrySetIt.hasNext()) {
                Map.Entry entry = (Map.Entry)entrySetIt.next();
                FileSystem fs = (FileSystem)entry.getKey();
                Set  foSet = (Set) entry.getValue();
                List backupList = new LinkedList();
                Iterator itBackup = foSet.iterator();
                while (itBackup.hasNext()) {
                    backupList.add(itBackup.next());
                }
                if (foSet != null) {
                    Iterator it = backupList.iterator ();
                    while (it.hasNext ()) {
                        FileObject fo = (FileObject)it.next ();
                        try {
                            if (fo.getFileSystem () != fs) {
                                it.remove ();
                            }
                        } catch (FileStateInvalidException ex) {
                            it.remove ();
                        }
                    }
                    Set backSet = new OrderedSet();
                    backSet.addAll(backupList);
                    result.put( fs.getActions (backSet) );
                }
            }
            
            
            return createMenu ( result, popUp, lookup);
        }
        return NONE;
    }

    /** Creates list of menu items that should be used for given
    * data object.
    * @param en enumeration of SystemAction that should be added
    *   into the menu if enabled and if not duplicated
    */
    static JMenuItem[] createMenu (Enumeration en, boolean popUp, Lookup lookup) {
        en = new RemoveDuplicatesEnumeration (en);

        ArrayList items = new ArrayList ();
        while (en.hasMoreElements ()) {
            Action a = (Action)en.nextElement ();
            
            // Retrieve context sensitive action instance if possible.
            if(lookup != null && a instanceof ContextAwareAction) {
                a = ((ContextAwareAction)a).createContextAwareInstance(lookup);
            }
            
            boolean enabled = false;
            try {
                enabled = a.isEnabled();
            } catch (RuntimeException e) {
                ErrorManager em = ErrorManager.getDefault();
                em.annotate(e, ErrorManager.UNKNOWN, 
                    "Guilty action: " + a.getClass().getName(), null, null, null); // NOI18N
                em.notify(e);
            }
            if (enabled) {
                JMenuItem item = null;
                if (popUp) {
                    if (a instanceof Presenter.Popup) {
                        item = ((Presenter.Popup)a).getPopupPresenter ();
                    }
                } else {
                    if (a instanceof Presenter.Menu) {
                        item = ((Presenter.Menu)a).getMenuPresenter ();
                    }
                }
                // test if we obtained the item
                if (item != null) {
                    items.add (item);
                }
            }
        }
        JMenuItem[] array = new JMenuItem [items.size ()];
        items.toArray (array);
        return array;
    }

    /* @return menu presenter.
    */
    public JMenuItem getMenuPresenter () {
        return new Menu (false, null);
    }

    /* @return popup presenter.
    */
    public JMenuItem getPopupPresenter () {
        return new Menu (true, null);
    }

    /* Getter for name
    */
    public String getName () {
        return NbBundle.getMessage(FileSystemAction.class, "ACT_FileSystemAction");
    }

    /* Getter for help.
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (FileSystemAction.class);
    }

    /* Do nothing.
    * This action itself does nothing, it only presents other actions.
    * @param ev ignored
    */
    public void actionPerformed (java.awt.event.ActionEvent e) {}
    
    /** Implements <code>ContextAwareAction</code> interface method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(actionContext);
    }
    

    /** Presenter for this action.
    */
    private static class Menu extends JInlineMenu implements PropertyChangeListener {
        /** menu presenter (true) or popup presenter (false) */
        private boolean popup;
        /** last registered items */
        private JMenuItem[] last = NONE;
        /** context for actions or null */
        private Lookup lookup;

        static final long serialVersionUID =2650151487189209766L;

        /** Creates new instance for menu/popup presenter.
        * @param popup true if this should represent popup
        * @param arr nodes to work with or null if global one should be used
        */
        Menu (boolean popup, Lookup lookup) {
            this.popup = popup;
            this.lookup = lookup;
            
            changeMenuItems (createMenu (popup, lookup));

            if (lookup == null) {
                // listen only when nodes not provided
                Registry r = WindowManager.getDefault ().getRegistry ();

                r.addPropertyChangeListener (
                    org.openide.util.WeakListener.propertyChange (this, r)
                );
            }
        }

        /** Changes the selection to new items.
        * @param items the new items
        */
        synchronized void changeMenuItems (JMenuItem[] items) {
            removeListeners (last);
            addListeners (items);
            last = items;
            setMenuItems (items);
        }


        /** Add listeners to menu items.
        * @param items the items
        */
        private void addListeners (JMenuItem[] items) {
            int len = items.length;
            for (int i = 0; i < len; i++) {
                items[i].addPropertyChangeListener (this);
            }
        }

        /** Remove all listeners from menu items.
        * @param items the items
        */
        private void removeListeners (JMenuItem[] items) {
            int len = items.length;
            for (int i = 0; i < len; i++) {
                items[i].removePropertyChangeListener (this);
            }
        }
        
        boolean needsChange = false;
        
        public void addNotify() {
            if (needsChange) {
                changeMenuItems (createMenu (popup, lookup));
                needsChange = false;
            }
            super.addNotify();
        }

        public void removeNotify() {
            removeListeners (last);
            last = NONE;
        }

        public void propertyChange (PropertyChangeEvent ev) {
            String name = ev.getPropertyName ();
            if (
                name == null ||
                name.equals (SystemAction.PROP_ENABLED) ||
                name.equals (Registry.PROP_ACTIVATED_NODES)
            ) {
                // change items later
                needsChange = true;
            }
        }
    }
    
    /**
     * The set, that keeps the order of elements in the same order they were added.
     * Only add of collections of elements is supported in this set. 
     */
    private static final class OrderedSet extends AbstractSet {
        
        /** Queue of collections of elements. */
        private QueueEnumeration queue = new QueueEnumeration();
        /** Objects stored in this set. */
        Object[] objects = null;
        
        /** Creates a new OrderedSet */
        public OrderedSet() {
        }
        
        /**
         * Adds all of the elements in the specified collection to this collection.
         */
        public boolean addAll(Collection coll) {
            queue.put(coll);
            return true;
        }
        
        
        private Object[] getObjects() {
            if (objects == null) {
                AlterEnumeration altered = new AlterEnumeration(queue) {
                    public Object alter(Object obj) {
                        return Collections.enumeration((Collection) obj);
                    }
                };
                SequenceEnumeration sequenced = new SequenceEnumeration(altered);
                Enumeration result = new RemoveDuplicatesEnumeration(sequenced);
                ArrayList objectList = new ArrayList();
                for (int i = 0; result.hasMoreElements(); i++) {
                    objectList.add(result.nextElement());
                }
                objects = objectList.toArray();
            }
            return objects;
        }
        
        /**
         * Returns an iterator over the elements contained in this collection.
         */
        public Iterator iterator() {
            final int size = getObjects().length;
            return new Iterator() {
                int i = 0;
                public boolean hasNext() {
                    return i < size;
                }
                
                public Object next() {
                    return objects[i++];
                }
                
                public void remove() {
                    throw new UnsupportedOperationException("Remove is not supported."); // NOI18N
                }
            };
        }
        
        /**
         * Returns the number of elements in this collection.
         */
        public int size() {
            return getObjects().length;
        }
    } // end of OrderedSet
    
    /** Context aware action implementation. */
    private static final class DelegateAction extends javax.swing.AbstractAction 
    implements Presenter.Menu, Presenter.Popup {
        /** lookup to work with */
        private Lookup lookup;

        public DelegateAction(Lookup lookup) {
            this.lookup = lookup;
        }


        /** @return menu presenter.  */
        public JMenuItem getMenuPresenter () {
            return new FileSystemAction.Menu (false, lookup);
        }

        /** @return popup presenter.  */
        public JMenuItem getPopupPresenter () {
            return new FileSystemAction.Menu (true, lookup);
        }
        
        public void actionPerformed(java.awt.event.ActionEvent e) {
        }
        
        
    } // end of DelegateAction
}
