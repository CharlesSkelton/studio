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
import java.awt.event.*;
import java.beans.*;
import java.io.IOException;
import java.util.*;
import java.lang.ref.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.openide.*;
import org.openide.awt.Actions;
import org.openide.awt.JMenuPlus;
import org.openide.explorer.view.MenuView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.Mutex;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.*;
import org.openide.util.WeakListener;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** Creates a new object from template in the selected folder.
* @see DataObject#isTemplate
*
* @author Petr Hamernik, Dafe Simonek
*/
public class NewTemplateAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 5408651725508985475L;

    private static DataObject selectedTemplate;
    private static DataFolder targetFolder;

    /** Maximum count of recent templates. */
    private static int MAX_RECENT_ITEMS = 5;
    
    /** Getter for wizard.
     * @param the node that is currently activated
     * @return the wizard or null if the wizard should not be enabled
    */
    static TemplateWizard getWizard (Node n) {
        if (n == null) {
            Node[] arr = WindowManager.getDefault ().getRegistry ().getActivatedNodes ();
            if (arr.length == 1) {
                n = arr[0];
            }
        }

        // if activated node isn't folder try parent which should be folder
        Node folder = n;
        // bugfix #29661, start finding the target folder with null folder
        targetFolder = null;
        while (targetFolder == null && folder != null) {
            targetFolder = folder == null ? null : (DataFolder) folder.getCookie (DataFolder.class);
            folder = folder.getParentNode ();
        }
        
        Cookie c = n == null ? null : (Cookie)n.getCookie (Cookie.class);
        if (c != null) {
            TemplateWizard t = c.getTemplateWizard ();
            if (t != null) {
                return t;
            }
        }

        return new DefaultTemplateWizard();
    }

    private boolean active = false;

    // This method is called only for the File->New menu item
    // it gets the node selection from the active TC
    protected void performAction (Node[] activatedNodes) {
        if (active)
            return;
        
        active = true;
        
        Node n = activatedNodes.length == 1 ? activatedNodes[0] : null;
        TemplateWizard wizard = getWizard (n);
        if (wizard instanceof DefaultTemplateWizard) {
            if (targetFolder != null && targetFolder.isValid())
                wizard.setTargetFolder(targetFolder);
            if (selectedTemplate != null && selectedTemplate.isValid())
                wizard.setTemplate(selectedTemplate);
        }
        try {
            // clears the name to default
            wizard.setTargetName(null);
            // instantiates
            wizard.instantiate ();
        } catch (IOException e) {
            ErrorManager em = ErrorManager.getDefault();
            em.annotate(e, NbBundle.getMessage(NewTemplateAction.class, "EXC_TemplateFailed"));
            em.notify(e);
        }
        finally {
            if (wizard instanceof DefaultTemplateWizard) {
                try {
                    selectedTemplate = wizard.getTemplate();
                    // Put the template in the recent list
                    if (selectedTemplate != null) addRecent (selectedTemplate);
                    targetFolder = wizard.getTargetFolder();
                }
                catch (IOException ignore) {
                    selectedTemplate = null;
                    targetFolder = null;
                }
            }
            active = false;
        }
    }

    /* Enables itself only when activates node is DataFolder.
    */
    protected boolean enable (Node[] activatedNodes) {
        if ((activatedNodes == null) || (activatedNodes.length != 1))
            return false;

        Cookie c = (Cookie)activatedNodes[0].getCookie (Cookie.class);
        if (c != null) {
            // if the current node provides its own wizard...
            return c.getTemplateWizard () != null;
        }
        
        DataFolder cookie = (DataFolder)activatedNodes[0].getCookie(DataFolder.class);
        if (cookie != null && !cookie.getPrimaryFile ().isReadOnly ()) {
            return true;
        }
        return false;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(NewTemplateAction.class, "NewTemplate");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (NewTemplateAction.class);
    }

    /* Resource name for the icon.
    * @return resource name
    */
    protected String iconResource () {
        return "org/openide/resources/actions/new.gif"; // NOI18N
    }
    
    /* Creates presenter that invokes the associated presenter.
    */
    public JMenuItem getMenuPresenter() {
        return new Actions.MenuItem (this, true) {
                   public void setEnabled (boolean e) {
                       super.setEnabled (true);
                   }
               };
    }

    /* Creates presenter that invokes the associated presenter.
    */
    public Component getToolbarPresenter() {
        return new Actions.ToolbarButton (this) {
                   public void setEnabled (boolean e) {
                       super.setEnabled (true);
                   }
               };
    }
    
    /* Creates presenter that displayes submenu with all
    * templates.
    */
    public JMenuItem getPopupPresenter() {
        TemplateWizard tw = getWizard(null);
        if (tw instanceof DefaultTemplateWizard) {
            return new MenuWithRecent();
        } else {
            // The null is correct but depends on the impl of MenuView.Menu
            JMenuItem menu = new MenuView.Menu (null, new TemplateActionListener (), false) {
                // this is the only place MenuView.Menu needs the node ready
                // so lets prepare it on-time
                public JPopupMenu getPopupMenu() {
                    if (node == null) node = getTemplateRoot();
                    return super.getPopupMenu();
                }
            };
            Actions.connect (menu, this, true);
            return menu;
        }
    }

    private class MenuWithRecent extends JMenuPlus {
        private boolean initialized = false;
        
        public MenuWithRecent() {
            super(); //NewTemplateAction.this.getName());
            Actions.setMenuText(this, NewTemplateAction.this.getName(), false);
        }
        
        public JPopupMenu getPopupMenu() {
            JPopupMenu popup = super.getPopupMenu();
            if (!initialized) {
                popup.add(new Item(null, true)); // New... item
            
                List privileged = getPrivilegedList();
                // all fixed items
                if (privileged.size() > 0) popup.add(new JSeparator()); // separator
                for (Iterator it = privileged.iterator(); it.hasNext(); ) {
                    DataObject dobj = (DataObject)it.next();
                    if (dobj instanceof DataShadow)
                                dobj = ((DataShadow)dobj).getOriginal();
                    popup.add(new Item(dobj, true));
                }

                // all recent items
                if (getRecentList ().size() > 0) popup.add(new JSeparator()); // separator
                for (Iterator it = getRecentList ().iterator(); it.hasNext(); ) {
                    popup.add(new Item((DataObject)it.next(), false));
                }
                
                initialized = true;
            }
            return popup;
        }
        
        private class Item extends JMenuItem implements HelpCtx.Provider, ActionListener {
            DataObject template; // Null means no template -> show the chooser
            boolean fixed;
            public Item(DataObject template, boolean fixed) {
                super();
                this.template = template;
                this.fixed = fixed;
                
                setText (template == null ? 
                    NbBundle.getMessage(NewTemplateAction.class, "NewTemplateAction") :
                    template.getNodeDelegate().getDisplayName()
                );
                    
                if (template == null) {
                    setIcon (NewTemplateAction.this.getIcon());
                } else {
                    setIcon (new ImageIcon(template.getNodeDelegate().getIcon(java.beans.BeanInfo.ICON_COLOR_16x16)));
                }
                
                addActionListener(this);
            }
            
            /** Get context help for this item.*/
            public HelpCtx getHelpCtx() {
                if (template != null) {
                    return template.getHelpCtx();
                }
                return NewTemplateAction.this.getHelpCtx();
            }
            
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                doShowWizard(template);
            }
        }
    }
    
    /** Cached content of Templates/Privileged */
    private DataFolder privilegedListFolder;
    
    /** Cached content of Templates/Recent */
    private DataFolder recentListFolder;
    
    private boolean recentChanged = true;
    private List recentList = new ArrayList (0);
    
    private List getPrivilegedList() {
        if (privilegedListFolder == null) {
            FileObject fo = Repository.getDefault().getDefaultFileSystem().
                                    findResource("Templates/Privileged"); // NOI18N
            if (fo != null) privilegedListFolder = DataFolder.findFolder(fo);
        }
        if (privilegedListFolder != null) {
            DataObject[] data = privilegedListFolder.getChildren();
            List l2 = new ArrayList(data.length);
            for (int i=0; i<data.length; i++) {
                DataObject dobj = data[i];
                if (dobj instanceof DataShadow)
                                dobj = ((DataShadow)dobj).getOriginal();
                if (isValidTemplate (dobj)) {
                    l2.add(dobj);
                }
            }
            return l2;
        } else {
            return new ArrayList(0);
        }
    }

    private void doShowWizard(DataObject template) {
        targetFolder = null;
            TemplateWizard wizard = getWizard (null);
            
            try {
                wizard.setTargetName (null);
                Set created = wizard.instantiate (template, targetFolder);
                if (created != null && wizard instanceof DefaultTemplateWizard) {
                    // put the item in the recent list
                    selectedTemplate = wizard.getTemplate();
                    if (selectedTemplate != null) addRecent (selectedTemplate);
                }
            } catch (IOException e) {
                ErrorManager em = ErrorManager.getDefault();
                em.annotate(e, NbBundle.getMessage(NewTemplateAction.class, "EXC_TemplateFailed"));
                em.notify(e);
            }
    }
    
    private DataFolder getRecentFolder () {
        if (recentListFolder == null) {
            FileObject fo = Repository.getDefault ().getDefaultFileSystem ().
                                    findResource ("Templates/Recent"); // NOI18N
            if (fo != null) {
                recentListFolder = DataFolder.findFolder(fo);
            }
        }
        
        return recentListFolder;
    }
    
    private List getRecentList () {
        if (!recentChanged) return recentList;
        if (getRecentFolder () != null) {
            DataObject[] data = getRecentFolder ().getChildren ();
            List l2 = new ArrayList(data.length);
            for (int i=0; i<data.length; i++) {
                DataObject dobj = data[i];
                if (dobj instanceof DataShadow)
                                dobj = ((DataShadow)dobj).getOriginal();
                if (isValidTemplate (dobj)) {
                    l2.add(dobj);
                } else {
                    removeRecent (data[i]);
                }
            }
            recentList = l2;
        } else {
            recentList = new ArrayList (0);
        }
        
        return recentList;
    }
    
    private boolean isValidTemplate (DataObject template) {
        return (template != null) && template.isTemplate ();
    }

    private boolean addRecent (DataObject template) {
        DataFolder folder = getRecentFolder ();
        
        // no recent folder, no recent templates
        if (folder == null) return false;
        
        // check if privileged
        if (getPrivilegedList ().contains (template)) return false;
        
        // check if recent already
        if (isRecent (template)) return false;
        
        DataObject[] templates = folder.getChildren ();
        
        DataObject[] newOrder = new DataObject[templates.length + 1];
        for (int i = 1; i < newOrder.length; i++) {
            newOrder[i] = templates[i - 1];
        }
        
        try {
            newOrder[0] = template.createShadow (folder);
            folder.setOrder (newOrder);
        } catch (IOException ioe) {
            ErrorManager em = ErrorManager.getDefault();
            em.notify (ErrorManager.INFORMATIONAL, ioe);
            // can't create shadow
            return false;
        }
        
        // reread children
        templates = folder.getChildren ();
        int size = templates.length;
        
        while (size > MAX_RECENT_ITEMS) {
            // remove last
            removeRecent (templates[size - 1]);
            size--;
        }
        
        recentChanged = true;
        
        return true;
    }
    
    private boolean removeRecent (DataObject template) {
        DataFolder folder = getRecentFolder ();
        
        // no recent folder, no recent templates
        if (folder == null) return false;
        
        try {
            template.delete ();
            recentChanged = true;
            return true;
        } catch (IOException ioe) {
            ErrorManager em = ErrorManager.getDefault();
            em.notify (ErrorManager.INFORMATIONAL, ioe);
            // it couldn't be deleted
            return false;
        }
    }
    
    private boolean isRecent (DataObject template) {
        return recentList.contains (template);
    }
    
    /** Create a hierarchy of templates.
    * @return a node representing all possible templates
    */
    public static Node getTemplateRoot () {
        RootChildren ch = new RootChildren ();
        // create the root
        return ch.getRootFolder ().new FolderNode (ch);
    }
    
    /** Cookie that can be implemented by a node if it wishes to have a 
     * special templates wizard.
     */
    public static interface Cookie extends Node.Cookie {
        /** Getter for the wizard that should be used for this cookie.
         */
        public TemplateWizard getTemplateWizard ();
    }
    
    /** Checks whether an object is acceptable for display as a container.
     */
    private static boolean acceptObj (DataObject obj) {
        if (obj.isTemplate ()) {
            return true;
        }

        if (obj instanceof DataFolder) {
            Object o = obj.getPrimaryFile ().getAttribute ("simple"); // NOI18N
            return o == null || Boolean.TRUE.equals (o);
        }

        return false;
        
    }


    /** Actions listener which instantiates the template */
    private static class TemplateActionListener implements NodeAcceptor, DataFilter {
        static final long serialVersionUID =1214995994333505784L;
        TemplateActionListener() {}
        public boolean acceptNodes (Node[] nodes) {
            if ((nodes == null) || (nodes.length != 1)) {
                return false;
            }
            Node n = nodes[0];
            DataObject obj = (DataObject)n.getCookie (DataObject.class);
            if (obj == null || !obj.isTemplate ()) {
                // do not accept
                return false;
            }
            
            // in this case the modified wizard will be used as default
            TemplateWizard wizard = getWizard (null);
            
            try {
                wizard.setTargetName (null);
                wizard.instantiate (obj, targetFolder);
            } catch (IOException e) {
                ErrorManager em = ErrorManager.getDefault();
                em.annotate(e, NbBundle.getMessage(NewTemplateAction.class, "EXC_TemplateFailed"));
                em.notify(e);
            }

            // ok
            return true;
        }

        /** Data filter impl.
        */
        public boolean acceptDataObject (DataObject obj) {
            return acceptObj (obj);
        }
    }
    
    /** Root template childen.
     */
    private static class RootChildren extends Children.Keys
    implements NodeListener {
        /** last wizard used with the root */
        private TemplateWizard wizard;
        /** Folder of templates */
        private DataFolder rootFolder;
        /** node to display templates for or null if current selection
         * should be followed
         */
        private WeakReference current;
        /** weak listener */
        private NodeListener listener = WeakListener.node (this, null);
        
        /** Instance not connected to any node.
         */
        public RootChildren () {
            TopComponent.Registry reg = WindowManager.getDefault ().getRegistry ();
            reg.addPropertyChangeListener (WeakListener.propertyChange (this, reg));
            
            updateWizard (getWizard (null));
        }
        
        public DataFolder getRootFolder () {
            if (rootFolder == null) {
                // if rootFolder is null then initialize folder
                doSetKeys ();
            }
            return rootFolder;
        }
               

        /** Creates nodes for nodes.
         */
        protected Node[] createNodes (Object key) {
            Node n = (Node)key;
            String nodeName = n.getDisplayName();
            
            DataObject obj = null;
            DataShadow shadow = (DataShadow)n.getCookie (DataShadow.class);
            if (shadow != null) {
                // I need DataNode here to get localized name of the
                // shadow, but without the ugly "(->)" at the end
                DataNode dn = new DataNode(shadow, Children.LEAF);
                nodeName = dn.getDisplayName();
                obj = shadow.getOriginal();
                n = obj.getNodeDelegate();
            }
            
            if (obj == null)
                obj = (DataObject)n.getCookie (DataObject.class);
            if (obj != null) {
                if (obj.isTemplate ()) {
                    // on normal nodes stop recursion
                    return new Node[] { new DataShadowFilterNode (n, LEAF, nodeName) };
                }
            
                if (acceptObj (obj)) {
                    // on folders use normal filtering
                    return new Node[] { new DataShadowFilterNode (n, new TemplateChildren (n), nodeName) };
                }
            }
            
            return null;
        }
        
        /** Check whether the node has not been updated.
         */
        private void updateNode (Node n) {            
            if (current != null && current.get () == n) {
                return;
            }
            
            if (current != null && current.get () != null) {
                ((Node)current.get ()).removeNodeListener (listener);
            }
            
            n.addNodeListener (listener);
            current = new WeakReference (n);
        }
        
        /** Check whether the wizard was not updated.
         */
        private void updateWizard (TemplateWizard w) {
            if (wizard == w) {
                return;
            }
            
            if (wizard != null) {
                Node n = wizard.getTemplatesFolder ().getNodeDelegate ();
                n.removeNodeListener (listener);
            }
            
            Node newNode = w.getTemplatesFolder ().getNodeDelegate ();
            newNode.addNodeListener (listener);
            wizard = w;
            
            updateKeys ();
        }
        
        /** Updates the keys.
         */
        private void updateKeys () {
            // updateKeys can be called while holding Children.MUTEX
            //   --> replan getNodes(true) to a new thread
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    doSetKeys ();
                }
            });
        }
        
        // don't call this while holding Children.MUTEX
        private void doSetKeys () {
            DataFolder df = wizard.getTemplatesFolder ();
            setKeys (df.getNodeDelegate ().getChildren ().getNodes (true));
            // #31152 - the rootFolder must be set after the children were prepared
            rootFolder = df;
        }
         
         /** Fired when the order of children is changed.
        /** Fired when the order of children is changed.
         * @param ev event describing the change
         */
        public void childrenReordered(NodeReorderEvent ev) {
            updateKeys ();
        }        
        
        /** Fired when a set of children is removed.
         * @param ev event describing the action
         */
        public void childrenRemoved(NodeMemberEvent ev) {
            updateKeys ();
        }
        
        /** Fired when a set of new children is added.
         * @param ev event describing the action
         */
        public void childrenAdded(NodeMemberEvent ev) {
            updateKeys ();
        }
        
        /** Fired when the node is deleted.
         * @param ev event describing the node
         */
        public void nodeDestroyed(NodeEvent ev) {
        }

        /** Listen on changes of cookies.
         */
        public void propertyChange(java.beans.PropertyChangeEvent ev) {
            String pn = ev.getPropertyName ();
            
            if (current != null && ev.getSource () == current.get ()) {
                // change in current node
                if (Node.PROP_COOKIE.equals (pn)) {
                    final Node node = (Node) current.get();
                    Mutex.EVENT.readAccess(new Runnable() {
                        public void run() {
                            updateWizard (getWizard (node));
                        }
                    });
                }
            } else {
                // change in selected nodes
                if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals (pn)) {
                    // change the selected node
                    Node[] arr = WindowManager.getDefault ().getRegistry ().getActivatedNodes ();
                    if (arr.length == 1) {
                        // only if the size is 1
                        updateNode (arr[0]);
                    }
                }
            }   
        }
        
    }

    /** Filter node children, that stops on data objects (does not go futher)
    */
    private static class TemplateChildren extends FilterNode.Children {
        public TemplateChildren (Node or) {
            super (or);
        }
        
        /** Creates nodes for nodes.
         */
        protected Node[] createNodes (Object key) {
            Node n = (Node)key;
            String nodeName = n.getDisplayName();
            
            DataObject obj = null;
            DataShadow shadow = (DataShadow)n.getCookie (DataShadow.class);
            if (shadow != null) {
                // I need DataNode here to get localized name of the
                // shadow, but without the ugly "(->)" at the end
                DataNode dn = new DataNode(shadow, Children.LEAF);
                nodeName = dn.getDisplayName();
                obj = shadow.getOriginal();
                n = obj.getNodeDelegate();
            }
            
            if (obj == null)
                obj = (DataObject)n.getCookie (DataObject.class);
            if (obj != null) {
                if (obj.isTemplate ()) {
                    // on normal nodes stop recursion
                    return new Node[] { new DataShadowFilterNode (n, LEAF, nodeName) };
                }
            
                if (acceptObj (obj)) {
                    // on folders use normal filtering
                    return new Node[] { new DataShadowFilterNode (n, new TemplateChildren (n), nodeName) };
                }
            }
            return new Node[] {};
        }

    }

    private static class DataShadowFilterNode extends FilterNode {
        
        private String name;
        
        public DataShadowFilterNode (Node or, org.openide.nodes.Children children, String name) {
            super (or, children);
            this.name = name;
            disableDelegation(FilterNode.DELEGATE_SET_DISPLAY_NAME);
        }
        
        public String getDisplayName() {
            return name;
        }
        
    }

    private static class DefaultTemplateWizard extends TemplateWizard {
        DefaultTemplateWizard() {}
    }
    
}
