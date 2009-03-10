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


package org.openide.explorer.view;


import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.Autoscroll;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.accessibility.*;

import org.openide.actions.PopupAction;
import org.openide.awt.MouseUtils;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOp;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListener;
import org.openide.util.Utilities;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.actions.ActionPerformer;


/** Tree view abstract class.
*
* @author   Petr Hamernik, Ian Formanek, Jaroslav Tulach
*/
public abstract class TreeView extends JScrollPane {
    //
    // static fields
    //


    /** generated Serialized Version UID */
    static final long serialVersionUID = -1639001987693376168L;

    /** How long it takes before collapsed nodes are released from the tree's cache
    */
    private static final int TIME_TO_COLLAPSE = System.getProperty ("netbeans.debug.heap") != null ? 0 : 15000;


    /** Minimum width of this component. */
    private static final int MIN_TREEVIEW_WIDTH = 400;

    /** Minimum height of this component. */
    private static final int MIN_TREEVIEW_HEIGHT = 400;

    //
    // components
    //

    /** Main <code>JTree</code> component. */
    transient protected JTree tree;

    /** model */
    transient NodeTreeModel treeModel;

    /** Explorer manager, valid when this view is showing */
    transient ExplorerManager manager;


    // Attributes

    /** Mouse and action listener. */
    transient PopupSupport defaultActionListener;
    /** Property indicating whether the default action is enabled. */
    transient boolean defaultActionEnabled;

    /** not null if popup menu enabled */
    transient PopupAdapter popupListener;




    /** the most important listener (on four types of events */
    transient TreePropertyListener managerListener = null;
    /** weak variation of the listener for property change on the explorer manager */
    transient PropertyChangeListener wlpc;
    /** weak variation of the listener for vetoable change on the explorer manager */
    transient VetoableChangeListener wlvc;


    /** true if drag support is active */
    transient boolean dragActive = false;
    /** true if drop support is active */
    transient boolean dropActive = false;
    /** Drag support */
    transient TreeViewDragSupport dragSupport;
    /** Drop support */
    transient TreeViewDropSupport dropSupport;

    transient boolean dropTargetPopupAllowed = true;
    transient private Container contentPane;
    transient Boolean waitCursorDisabled;
    
    /** Constructor.
    */
    public TreeView () {
        this (true, true);
    }

    /** Constructor.
    * @param defaultAction should double click on a node open its default action?
    * @param popupAllowed should right-click open popup?
    */
    public TreeView (boolean defaultAction, boolean popupAllowed) {
        initializeTree ();

        // activation of drop target
        if (DragDropUtilities.dragAndDropEnabled) {
            ExplorerDnDManager.getDefault ().addFutureDropTarget (this);
            // note: drag target is activated on focus gained
        }

        setPopupAllowed (popupAllowed);
        setDefaultActionAllowed (defaultAction);
        
        Dimension dim = null;
        try {
            dim = getPreferredSize();
            if (dim == null) {
                dim = new Dimension(MIN_TREEVIEW_WIDTH, MIN_TREEVIEW_HEIGHT);
            }
        } catch (NullPointerException npe) {
            dim = new Dimension(MIN_TREEVIEW_WIDTH, MIN_TREEVIEW_HEIGHT);
        } 
        if (dim.width < MIN_TREEVIEW_WIDTH)
            dim.width = MIN_TREEVIEW_WIDTH;
        if (dim.height < MIN_TREEVIEW_HEIGHT)
            dim.height = MIN_TREEVIEW_HEIGHT;
        setPreferredSize(dim);
    }

    /** Initializes the tree & model.
     * [dafe] Horrible technique - overridable method called from constructor
     * may result in subclass code invoked when this object is not fully
     * constructed.
     * However I don't have enough knowledge about this code to change it.
    */
    void initializeTree () {
        // initilizes the JTree
        treeModel = createModel ();
        
        tree = new ExplorerTree(treeModel);
        
        NodeRenderer rend = NodeRenderer.sharedInstance ();
        tree.setCellRenderer(rend);
        tree.putClientProperty("JTree.lineStyle", "Angled"); // NOI18N
        setViewportView (tree);
        
        // set selection mode to DISCONTIGUOUS_TREE_SELECTION as default
        setSelectionMode (TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        ToolTipManager.sharedInstance().registerComponent(tree);


        // init listener & attach it to closing of
        managerListener = new TreePropertyListener();
        tree.addTreeExpansionListener (managerListener);
        tree.addTreeWillExpandListener (managerListener);

        // do not care about focus
        setRequestFocusEnabled (false);
        
	defaultActionListener = new PopupSupport();
        tree.addFocusListener(defaultActionListener);
        tree.addMouseListener(defaultActionListener);
    }


    /** Is it permitted to display a popup menu?
     * @return <code>true</code> if so
     */
    public boolean isPopupAllowed () {
        return popupListener != null;
    }

    /** Enable/disable displaying popup menus on tree view items.
    * Default is enabled.
    * @param value <code>true</code> to enable
    */
    public void setPopupAllowed (boolean value) {
        if (popupListener == null && value) {
            // on
            popupListener = new PopupAdapter ();
            tree.addMouseListener (popupListener);
            return;
        }
        if (popupListener != null && !value) {
            // off
            tree.removeMouseListener (popupListener);
            popupListener = null;
            return;
        }
    }

    void setDropTargetPopupAllowed(boolean value) {
        dropTargetPopupAllowed=value;
        
        if (dropSupport!=null) {
            dropSupport.setDropTargetPopupAllowed(value);
        }
    }
    
    boolean isDropTargetPopupAllowed() {
        return dropSupport!=null ?
            dropSupport.isDropTargetPopupAllowed() : 
            dropTargetPopupAllowed;
    }

    /** Does a double click invoke the default node action?
     * @return <code>true</code> if so
     */
    public boolean isDefaultActionEnabled () {
        return defaultActionEnabled;
    }

    /** Requests focus for the tree component. Overrides superclass method. */
    public void requestFocus () {
        tree.requestFocus();
    }

    /** Enable/disable double click to invoke default action.
     * If defaultAction is not enabled double click expand/collapse node.
     * @param value <code>true</code> to enable
     */
    public void setDefaultActionAllowed(boolean value) {
        defaultActionEnabled = value;
        
        if(value) {
            tree.registerKeyboardAction(
                defaultActionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                JComponent.WHEN_FOCUSED
            );
        } else {
            // Switch off.
            tree.unregisterKeyboardAction(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false)
            );
        }
    }

    /**
    * Is the root node of the tree displayed?
    *
    * @return <code>true</code> if so
    */
    public boolean isRootVisible() {
        return tree.isRootVisible();
    }

    /** Set whether or not the root node from
    * the <code>TreeModel</code> is visible.
    *
    * @param visible <code>true</code> if it is to be displayed
    * @beaninfo
    *        bound: true
    *  description: Whether or not the root node
    *               from the TreeModel is visible.
    */
    public void setRootVisible (boolean visible) {
        tree.setRootVisible (visible);
        tree.setShowsRootHandles(!visible);
    }


    /********** Support for the Drag & Drop operations *********/

    /** Drag support is disabled by default.
    * @return true if dragging from the view is enabled, false
    * otherwise.
    */
    public boolean isDragSource () {
        return dragActive;
    }

    /** Enables/disables dragging support.
    * @param state true enables dragging support, false disables it.
    */
    public void setDragSource (boolean state) {
        if (state == dragActive)
            return;
        dragActive = state;
        // create drag support if needed
        if (dragActive && (dragSupport == null))
            dragSupport = new TreeViewDragSupport(this, tree);
        // activate / deactivate support according to the state
        dragSupport.activate(dragActive);
    }

    /** Drop support is disabled by default.
    * @return true if dropping to the view is enabled, false
    * otherwise<br>
    */
    public boolean isDropTarget () {
        return dropActive;
    }

    /** Enables/disables dropping support.
    * @param state true means drops into view are allowed,
    * false forbids any drops into this view.
    */
    public void setDropTarget (boolean state) {
        if (state == dropActive)
            return;
        dropActive = state;
        // create drop support if needed
        if (dropActive && (dropSupport == null))
            dropSupport = new TreeViewDropSupport(this, tree, dropTargetPopupAllowed);
        // activate / deactivate support according to the state
        dropSupport.activate(dropActive);
    }

    /** Actions constants comes from DnDConstants.XXX constants.
    * All actions (copy, move, link) are allowed by default.
    * @return Set of actions which are allowed when dragging from
    * asociated component.
     */
    public int getAllowedDragActions () {
        // PENDING
        return DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY |
               DnDConstants.ACTION_LINK;
    }

    /** Sets allowed actions for dragging
    * @param actions new drag actions, using DnDConstants.XXX 
    */  
    public void setAllowedDragActions (int actions) {
        // PENDING
    }

    /** Actions constants comes from DnDConstants.XXX constants.
    * All actions are allowed by default.
    * @return Set of actions which are allowed when dropping
    * into the asociated component.
    */
    public int getAllowedDropActions () {
        // PENDING
        return DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY |
               DnDConstants.ACTION_LINK;
    }

    /** Sets allowed actions for dropping.
    * @param actions new allowed drop actions, using DnDConstants.XXX 
    */  
    public void setAllowedDropActions (int actions) {
        // PENDING
    }


    //
    // Control over expanded state
    //

    /** Collapses the tree under given node.
    * 
    * @param n node to collapse
    */
    public void collapseNode (Node n) {
        TreePath treePath = new TreePath (
                                treeModel.getPathToRoot (
                                    VisualizerNode.getVisualizer (null, n)
                                )
                            );
        tree.collapsePath (treePath);
    }

    /** Expandes the node in the tree.
    * 
    * @param n node 
    */
    public void expandNode (Node n) {

        lookupExplorerManager ();
        
        
                
        TreePath treePath = new TreePath (
                                treeModel.getPathToRoot (
                                    VisualizerNode.getVisualizer (null, n)
                                )
                            );

        tree.expandPath (treePath);
    }

    /** Test whether a node is expanded in the tree or not
    * @param n the node to test
    * @return true if the node is expanded
    */
    public boolean isExpanded (Node n) {
        TreePath treePath = new TreePath (
                                treeModel.getPathToRoot (
                                    VisualizerNode.getVisualizer (null, n)
                                )
                            );
        return tree.isExpanded (treePath);
    }


    /** Expands all paths.
    */
    public void expandAll () {
        int i = 0, j, k = tree.getRowCount ();
        do {
            do {
                j = tree.getRowCount ();
                tree.expandRow (i);
            } while (j != tree.getRowCount ());
            i++;
        } while (i < tree.getRowCount ());
    }

    //
    // Processing functions
    //
    
    
    

    /** Initializes the component and lookup explorer manager.
    */
    public void addNotify () {
        super.addNotify ();
        lookupExplorerManager ();
    }
        
        
    /** Registers in the tree of components.
     */
    private void lookupExplorerManager () {
        // Enter key in the tree

        ExplorerManager newManager = ExplorerManager.find (TreeView.this);

        if (newManager != manager) {
            if (manager != null) {
                manager.removeVetoableChangeListener (wlvc);
                manager.removePropertyChangeListener (wlpc);
            }

            manager = newManager;

            manager.addVetoableChangeListener(wlvc = WeakListener.vetoableChange (managerListener, manager));
            manager.addPropertyChangeListener(wlpc = WeakListener.propertyChange (managerListener, manager));

            synchronizeRootContext ();
            synchronizeExploredContext ();
            synchronizeSelectedNodes ();
        }

        // Sometimes the listener is registered twice and we get the 
        // selection events twice. Removing the listener before adding it
        // should be a safe fix.
        tree.getSelectionModel().removeTreeSelectionListener(managerListener);
        tree.getSelectionModel().addTreeSelectionListener(managerListener);
    }

    /** Deinitializes listeners.
    */
    public void removeNotify () {
        super.removeNotify ();

        tree.getSelectionModel().removeTreeSelectionListener(managerListener);
    }

    // *************************************
    // Methods to be overriden by subclasses
    // *************************************


    /** Allows subclasses to provide own model for displaying nodes.
    * @return the model to use for this view
    */
    protected abstract NodeTreeModel createModel();


    /** Called to allow subclasses to define the behaviour when a
    * node(s) are selected in the tree.
    *
    * @param nodes the selected nodes
    * @param em explorer manager to work on (change nodes to it)
    * @throws PropertyVetoException if the change cannot be done by the explorer
    *    (the exception is silently consumed)
    */
    protected abstract void selectionChanged (Node[] nodes, ExplorerManager em) throws PropertyVetoException;

    /** Called when explorer manager is about to change the current selection.
    * The view can forbid the change if it is not able to display such
    * selection.
    *
    * @param nodes the nodes to select
    * @return false if the view is not able to change the selection
    */
    protected abstract boolean selectionAccept (Node[] nodes);


    /** Show a given path in the screen. It depends on the kind of <code>TreeView</code>
    * if the path should be expanded or just made visible.
    *
    * @param path the path
    */
    protected abstract void showPath(TreePath path);

    /** Shows selection to reflect the current state of the selection in the explorer.
    *
    * @param paths array of paths that should be selected
    */
    protected abstract void showSelection (TreePath[] paths);


    /** Specify whether a context menu of the explored context should be used.
    * Applicable when no nodes are selected and the user wants to invoke
    * a context menu (clicks right mouse button).
    *
    * @return <code>true</code> if so; <code>false</code> in the default implementation
    */
    protected boolean useExploredContextMenu() {
        return false;
    }

    /** Check if selection of the nodes could break the selection mode set in TreeSelectionModel.
     * @param nodes the nodes for selection
     * @return true if the selection mode is broken */
    private boolean isSelectionModeBroken (Node[] nodes) {
        
        // if nodes are empty or single the everthing is ok
        // or if discontiguous selection then everthing ok
        if (nodes.length <= 1 || getSelectionMode()==TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
            return false;

        // if many nodes
        
        // brakes single selection mode
        if (getSelectionMode()==TreeSelectionModel.SINGLE_TREE_SELECTION)
            return true;
        
        // check the contiguous selection mode
        TreePath[] paths = new TreePath[nodes.length];
        RowMapper rowMapper = tree.getSelectionModel().getRowMapper();

        // if rowMapper is null then tree bahaves as discontiguous selection mode is set
        if (rowMapper==null) {
            return false;
        }

        ArrayList toBeExpaned = new ArrayList (3);
        for (int i = 0; i < nodes.length; i++) {
            toBeExpaned.clear ();
            Node n = nodes[i];
            while (n.getParentNode ()!=null) {
                if (!isExpanded (n))
                    toBeExpaned.add (n);
                n = n.getParentNode ();
            }
            for (int j = toBeExpaned.size ()-1; j>=0; j--) {
                expandNode ((Node)toBeExpaned.get (j));
            }
            TreePath treePath = new TreePath (treeModel.getPathToRoot (VisualizerNode.getVisualizer (null, nodes[i])));
            paths[i] = treePath;

        }
        int[] rows = rowMapper.getRowsForPaths(paths);
        
        // check selection's rows
        Arrays.sort (rows);
        for (int i = 1; i < rows.length; i++) {
            if ( rows[i] != rows[i - 1] + 1 ) {
                return true;
            }
        }
        
        // all is ok
        return false;
    }

    //
    // synchronizations
    //

    /** Called when selection in tree is changed.
    */
    final void callSelectionChanged (Node[] nodes) {
        manager.removePropertyChangeListener (wlpc);
        manager.removeVetoableChangeListener (wlvc);
        try {
            selectionChanged (nodes, manager);
        } catch (PropertyVetoException e) {
            synchronizeSelectedNodes ();
        } finally {
            manager.addPropertyChangeListener (wlpc);
            manager.addVetoableChangeListener (wlvc);
        }
    }


    /** Synchronize the root context from the manager of this Explorer.
    */
    final void synchronizeRootContext() {
        treeModel.setNode (manager.getRootContext ());
    }

    /** Synchronize the explored context from the manager of this Explorer.
    */
    final void synchronizeExploredContext() {
        Node n = manager.getExploredContext ();
        if (n != null) {
            TreePath treePath = new TreePath (treeModel.getPathToRoot (VisualizerNode.getVisualizer (null, n)));
            showPath(treePath);
        }
    }

    /** Sets the selection model, which must be one of SINGLE_TREE_SELECTION,
     * CONTIGUOUS_TREE_SELECTION or DISCONTIGUOUS_TREE_SELECTION.
     * <p>
     * This may change the selection if the current selection is not valid
     * for the new mode. For example, if three TreePaths are
     * selected when the mode is changed to <code>SINGLE_TREE_SELECTION</code>,
     * only one TreePath will remain selected. It is up to the particular
     * implementation to decide what TreePath remains selected.
     * Note: DISCONTIGUOUS_TREE_SELECTION is set as default.
     * @since 2.15
     * @param mode selection mode
     */
    public void setSelectionMode (int mode) {
        tree.getSelectionModel ().setSelectionMode (mode);
    }
    
    /** Returns the current selection mode, one of
     * <code>SINGLE_TREE_SELECTION</code>,
     * <code>CONTIGUOUS_TREE_SELECTION</code> or
     * <code>DISCONTIGUOUS_TREE_SELECTION</code>.
     * @since 2.15
     * @return selection mode
     */
    public int getSelectionMode () {
        return tree.getSelectionModel ().getSelectionMode ();
    }
    
    //
    // showing and removing the wait cursor
    //
    
    private void showWaitCursor () {
        if (getRootPane () == null) return ;
        contentPane = getRootPane ().getContentPane ();
        if (SwingUtilities.isEventDispatchThread ()) {
            contentPane.setCursor (Utilities.createProgressCursor (contentPane));
        } else {
            SwingUtilities.invokeLater (new Runnable () {
                public void run () {
                    contentPane.setCursor (Utilities.createProgressCursor (contentPane));
                }
            });
        }
    }
    
    private void showNormalCursor () {
        if (contentPane == null) return ;
        if (SwingUtilities.isEventDispatchThread ()) {
            contentPane.setCursor (null);
        } else {
            SwingUtilities.invokeLater (new Runnable () {
                public void run () {
                    contentPane.setCursor (null);
                }
            });
        }
    }
    
    private void prepareWaitCursor (final Node node) {
        // check type of node
        if (node == null) {
            showNormalCursor ();
        }
        
        showWaitCursor ();
        RequestProcessor.getDefault ().post (new Runnable () {
            public void run () {
                try {
                node.getChildren ().getNodes (true);
                } catch (Exception e) {
                    // log a exception
                    ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
                } finally {
                    // show normal cursor above all
                    showNormalCursor ();
                }
            }
        });
    }
    
    /** Synchronize the selected nodes from the manager of this Explorer.
    * The default implementation does nothing.
    */
    final void synchronizeSelectedNodes() {
        
        Node[] arr = manager.getSelectedNodes ();
        TreePath[] paths = new TreePath[arr.length];

        for (int i = 0; i < arr.length; i++) {
            TreePath treePath = new TreePath (treeModel.getPathToRoot (VisualizerNode.getVisualizer (null, arr[i])));
            paths[i] = treePath;
        }

        tree.getSelectionModel().removeTreeSelectionListener(managerListener);
        showSelection (paths);
        tree.getSelectionModel().addTreeSelectionListener(managerListener);
    }

    void scrollTreeToVisible(TreePath path, TreeNode child) {
        Rectangle base = tree.getVisibleRect();
        Rectangle b1 = tree.getPathBounds(path);
        Rectangle b2 = tree.getPathBounds(new TreePath(treeModel.getPathToRoot(child)));
        if (base != null && b1 != null && b2 != null) {
            tree.scrollRectToVisible(new Rectangle(base.x, b1.y, 1, b2.y - b1.y + b2.height));
        }
    }

    /** Listens to the property changes on tree */
    class TreePropertyListener implements VetoableChangeListener,
                PropertyChangeListener,
                TreeExpansionListener,
                TreeWillExpandListener,
                TreeSelectionListener, Runnable {
		
        private RequestProcessor.Task scheduled;
        
	TreePropertyListener() {}
	
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
            if (evt.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES)) {
                // issue 11928 check if selecetion mode will be broken
                if (isSelectionModeBroken ((Node[])evt.getNewValue ())) {
                    throw new PropertyVetoException ("", evt); // NOI18N
                }
                if (!selectionAccept ((Node[])evt.getNewValue ())) {
                    throw new PropertyVetoException ("", evt); // NOI18N
                }
            }
        }

        public final void propertyChange(final PropertyChangeEvent evt) {
            if (manager == null) return; // the tree view has been removed before the event got delivered
            if (evt.getPropertyName().equals(ExplorerManager.PROP_ROOT_CONTEXT))
                synchronizeRootContext();
            if (evt.getPropertyName().equals(ExplorerManager.PROP_EXPLORED_CONTEXT))
                synchronizeExploredContext();
            if (evt.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES))
                synchronizeSelectedNodes();
        }

        public synchronized void treeExpanded (TreeExpansionEvent ev) {
            final TreePath path = ev.getPath ();

            RequestProcessor.Task t = scheduled;
            if (t != null) {
                t.cancel ();
            }
            
	    // It is OK to use multithreaded shared RP as the requests
	    // will be serialized in event queue later
            scheduled = RequestProcessor.getDefault().post (new Runnable () {
                public void run () {
		    if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater (this);
                        
			return;
		    }

                    if (!tree.isVisible(path)) {
                        // if the path is not visible - don't check the children
                        
                        return;
                    }
		    
                    if (treeModel == null) {
                	// no model, no action, no problem
                        return;
                    }
		    
                    TreeNode myNode = (TreeNode)path.getLastPathComponent();
                    
		    if (treeModel.getPathToRoot(myNode)[0] != treeModel.getRoot()) {
                        // the way from the path no longer
                        // goes to the root, probably someone
                        // has removed the node on the way up
                        // System.out.println("different roots.");
                        return;
                    }
                    
                    // show wait cursor
                    //showWaitCursor ();
		    
                    int lastChildIndex = myNode.getChildCount()-1;
                    if (lastChildIndex >= 0) {
                        TreeNode lastChild = myNode.getChildAt(lastChildIndex);

                        Rectangle base = tree.getVisibleRect();
                        Rectangle b1 = tree.getPathBounds(path);
                        Rectangle b2 = tree.getPathBounds(new TreePath(treeModel.getPathToRoot(lastChild)));
                        if (base != null && b1 != null && b2 != null) {
                            tree.scrollRectToVisible(new Rectangle(base.x, b1.y, 1, b2.y - b1.y + b2.height));
                        }

//                        scrollTreeToVisible(path, lastChild);
                    }
                }
            }, 250); // hope that all children are there after this time
        }

        public synchronized void treeCollapsed (final TreeExpansionEvent ev) {
            final TreePath path = ev.getPath ();
            showNormalCursor ();
            
            RequestProcessor.Task t = scheduled;
            if (t != null) {
                t.cancel ();
            }
            
	    // It is OK to use multithreaded shared RP as the requests
	    // will be serialized in event queue later
            scheduled = RequestProcessor.getDefault().post (new Runnable () {
                public void run () {
                    if (!SwingUtilities.isEventDispatchThread()) {
                        SwingUtilities.invokeLater (this);
                        return;
                    }
                    
                    if (tree.isExpanded(path)) {
                        // the tree shows the path - do not collapse
                        // the tree
                        return;
                    }

                    if (!tree.isVisible(path)) {
                        // if the path is not visible do not collapse
                        // the tree
                        return;
                    }

                    if (treeModel == null) {
                        // no model, no action, no problem
                        return;
                    }

                    TreeNode myNode = (TreeNode)path.getLastPathComponent();

                    if (treeModel.getPathToRoot(myNode)[0]
                    != treeModel.getRoot()) {
                        // the way from the path no longer
                        // goes to the root, probably someone
                        // has removed the node on the way up
                        // System.out.println("different roots.");
                        return;
                    }

                    treeModel.nodeStructureChanged(myNode);
                }
            }, TIME_TO_COLLAPSE);
        }

        /* Called whenever the value of the selection changes.
        * @param ev the event that characterizes the change.
        */
        public void valueChanged(TreeSelectionEvent ev) {
            
            TreePath[] paths = tree.getSelectionPaths ();
            
            if (paths == null) {
                callSelectionChanged (new Node[0]);
            } else {
                // we need to force no changes to nodes hierarchy =>
                // we are requesting read request, but it is not necessary
                // to execute the next action immediatelly, so postReadRequest
                // should be enough
                readAccessPaths = paths;
                Children.MUTEX.postReadRequest(this);
            }
        }
        
        private TreePath[] readAccessPaths;
       
        /** Called under Children.MUTEX to refresh the currently selected nodes.
        */
        public void run () {

            if (readAccessPaths == null) {
                return;
            }
            
            TreePath[] paths = readAccessPaths;
            // non null value caused leak in
            // ComponentInspector
            // When the last Form was closed then the ComponentInspector was
            // closed as well. Since this variable was not null - 
            // last selected Node (RADComponentNode) was held ---> FormManager2 was held, etc.
            readAccessPaths = null;
            java.util.List ll = new java.util.ArrayList(paths.length);
            
            for (int i = 0; i < paths.length; i++) {
                Node n = Visualizer.findNode (paths[i].getLastPathComponent ());
                if (n == manager.getRootContext() || n.getParentNode() != null) {
                    ll.add (n);
                }
            }

            callSelectionChanged ((Node[])ll.toArray (new Node[ll.size ()]));
        }

        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        }
        
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            // prepare wait cursor and optionally show it
            TreePath path = event.getPath ();
            prepareWaitCursor (DragDropUtilities.secureFindNode (path.getLastPathComponent ()));
        }
        
    } // end of TreePropertyListener


    /** Popup adapter.
    */
    class PopupAdapter extends MouseUtils.PopupMouseAdapter {

	PopupAdapter() {}
	
        protected void showPopup (MouseEvent e) {
            int selRow = tree.getRowForLocation(e.getX(), e.getY());

            if (!tree.isRowSelected(selRow)) {
//                try {
//                    manager.setSelectedNodes(new Node[0]); // David's workaround
//                } catch (PropertyVetoException exc) {
//                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, exc);
//                }
                // This will set ExplorerManager selection, no need to push
                // it more (the commented out code above).
                tree.setSelectionRow(selRow);
            }

            if (selRow != -1) {
                Point p = SwingUtilities.convertPoint(e.getComponent(),e.getX(), e.getY(),TreeView.this);

                createPopup((int)p.getX(), (int)p.getY());
            }
        }
    }

    /**
     * Method created to fix #12520. It returns false in the situation
     * where the popup is invoked on non-activated top component or
     * dialog.
     * Note: Using lookup and reflection bacause we are in standalone
     *   explorer library.
     */
    static boolean shouldPopupBeDisplayed(Component comp) {
        try {
            Class c = Class.forName("org.openide.windows.TopComponent$Registry"); // NOI18N
            Object registry = Lookup.getDefault().lookup(c);
            if (registry == null) {
                // in case of standalone library we always return true
                return true;
            }
            java.lang.reflect.Method m = c.getMethod("getActivated", new Class[0]);   // NOI18N
            Object activated = m.invoke(registry, new Object[0]);
            boolean fromActivated = SwingUtilities.isDescendingFrom(comp, (Component)activated);
            if (fromActivated) {
                return true;
            }
            // check for dialogs (they are not managed by the window system)
            Window w = SwingUtilities.getWindowAncestor(comp);
            if (w instanceof Dialog) {
                return true;
            }
            return false;
        } catch (Exception x) {
            ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, x);
        }
        // if we had any problems it is safe to just say "popup go"
        return true;
    }
    
    private void createPopup(int xpos, int ypos, JPopupMenu popup) {
        if ((popup != null) && (popup.getSubElements().length > 0) && (shouldPopupBeDisplayed(TreeView.this))) {
            popup.show(TreeView.this, xpos, ypos);
        }
    }
    
    void createPopup(int xpos, int ypos) {
        // bugfix #23932, don't create if it's disabled
        if (isPopupAllowed ()) {
            Node[] arr = manager.getSelectedNodes ();
            Action[] actions = NodeOp.findActions (arr);
            if (actions.length > 0) {
                createPopup ( xpos, ypos, Utilities.actionsToPopup(actions, this) );
            }
        }
    }
    
    /* create standard popup menu and add newMenu to it
     */
    void createExtendedPopup(int xpos, int ypos, JMenu newMenu) {
        Node[] ns = manager.getSelectedNodes ();
        JPopupMenu popup = null;
        if (ns.length > 0) {
            // if any nodes are selected --> find theirs actions
            Action[] actions = NodeOp.findActions (ns);
            popup = Utilities.actionsToPopup (actions, this);
        } else {
            // if none node is selected --> get context actions from view's root
            if (manager.getRootContext () != null) {
                popup = manager.getRootContext ().getContextMenu ();
            }
        }

        int cnt = 0;
        if ( popup != null && ( cnt = popup.getComponentCount() ) > 1  ) {
            popup.insert( newMenu, cnt - 1 );
            popup.insert( new JPopupMenu.Separator(), cnt );
        }
        else {
            if ( popup == null )
                popup = SystemAction.createPopupMenu( new SystemAction[] {} );
            popup.add( newMenu );
        }

        createPopup ( xpos, ypos, popup );
    }

    /** Utility method for invoking actions in separate thread. Note:
     * it uses reflection because it should work without
     * the rest of the IDE classes.
     */
    static void invokeAction(SystemAction sa, ActionEvent ev) {
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
    

    /** Returns the the point at which the popup menu is to be showed. May return null.
     * @return the point or null
     */    
    Point getPositionForPopup () {
        int i = tree.getLeadSelectionRow();
        if (i < 0) return null;

        Rectangle rect = tree.getRowBounds(i);
        if (rect == null) return null;

        // bugfix #28360, convert the coordinates by the root pane
        Point p = new Point (rect.x, rect.y);
        if (tree.getRootPane () != null) {
            p =  SwingUtilities.convertPoint (tree, rect.x, rect.y, tree.getRootPane ());
        }
        return p;
    }
    
    final class PopupSupport extends MouseAdapter
	implements ActionPerformer, Runnable, FocusListener, ActionListener {

        CallbackSystemAction csa;
        
        boolean firstFocus = true;

        public void performAction(SystemAction act) {
            SwingUtilities.invokeLater(this);
        }
	
	public void run() {
            Point p = getPositionForPopup ();
            if (p == null) {
                return ;
            }
	    createPopup(p.x, p.y);
        }
    
        public void focusGained(java.awt.event.FocusEvent ev) {
            if (csa == null) {
                csa = (CallbackSystemAction) SystemAction.get (PopupAction.class);
            }
            csa.setActionPerformer(this);
            if (firstFocus) {
                firstFocus = false;
                // lazy activation of drag source
                if (DragDropUtilities.dragAndDropEnabled) {
                    setDragSource(true);
                    // note: dropTarget is activated in constructor
                }
                // lazy cell editor init
                tree.setCellEditor(new TreeViewCellEditor(tree, new NodeRenderer.Tree ()));
                tree.setEditable(true);
            }
        }
        
        public void focusLost(java.awt.event.FocusEvent ev) {
            if (csa != null && (csa.getActionPerformer() instanceof PopupSupport)) {
                csa.setActionPerformer(null);
            }
        } 
        
	/* clicking adapter */
        public void mouseClicked(MouseEvent e) {
            int selRow = tree.getRowForLocation(e.getX(), e.getY());

            if((selRow != -1) && SwingUtilities.isLeftMouseButton(e) 
                && MouseUtils.isDoubleClick(e)) {
                    
                // Default action.
                if(defaultActionEnabled) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    Node node = Visualizer.findNode (selPath.getLastPathComponent());

                    SystemAction sa = node.getDefaultAction ();
                    if (sa != null) {
                        TreeView.invokeAction
                            (sa, new ActionEvent (node, ActionEvent.ACTION_PERFORMED, "")); // NOI18N
                        e.consume();
                        return;
                    }
                }

                if(tree.isExpanded(selRow)) {
                    tree.collapseRow(selRow);
                } else {
                    tree.expandRow(selRow);
                }
            }
        }

	/* VK_ENTER key processor */
        public void actionPerformed(ActionEvent evt) {
            Node[] nodes = manager.getSelectedNodes();
            if (nodes.length == 1) {
                SystemAction sa = nodes[0].getDefaultAction ();
                if (sa != null) {
                    TreeView.invokeAction
                        (sa, new ActionEvent (nodes[0], ActionEvent.ACTION_PERFORMED, "")); // NOI18N
                }
            }
        }

    }

    private final class ExplorerTree extends JTree implements Autoscroll {
	AutoscrollSupport support;
        private String maxPrefix;
	
        ExplorerTree(TreeModel model) {
            super(model);
	    toggleClickCount = 0;

    	    if (System.getProperty("java.version").startsWith("1.4")) {
        	// fix for #18292 (only for JDK 1.4)
        	// default action map for JTree defines these shortcuts
        	// but we use our own mechanism for handling them
        	// following lines disable default L&F handling (if it is
        	// defined on Ctrl-c, Ctrl-v and Ctrl-x)
        	getInputMap().put(KeyStroke.getKeyStroke("control C"), "none"); // NOI18N
        	getInputMap().put(KeyStroke.getKeyStroke("control V"), "none"); // NOI18N
        	getInputMap().put(KeyStroke.getKeyStroke("control X"), "none"); // NOI18N
                getInputMap().put(KeyStroke.getKeyStroke("COPY"), "none"); // NOI18N
                getInputMap().put(KeyStroke.getKeyStroke("PASTE"), "none"); // NOI18N
                getInputMap().put(KeyStroke.getKeyStroke("CUT"), "none"); // NOI18N
            } else {
                // fix for #19094. the bug does not occur on JDK1.4 because
                // TreeCancelEditingAction was fixed in BasicTreeUI for JDK1.4
                getActionMap().put("cancel", new OurTreeCancelEditingAction()); // NOI18N
            }
            setupSearch();
        }
    
        // searchTextField manages focus because it handles VK_TAB key
        private JTextField searchTextField = new JTextField() {
            public boolean isManagingFocus() {
                return true;
            }
            
            public void processKeyEvent (KeyEvent ke) {
                //override the default handling so that
                //the parent will never receive the escape key and
                //close a modal dialog
                if (ke.getKeyCode() == ke.VK_ESCAPE) {
                    removeSearchField();
                } else {
                    super.processKeyEvent(ke);
                }
            }
        };
        
        final private int heightOfTextField = searchTextField.getPreferredSize().height;

        private void setupSearch() {
            // Remove the default key listeners
            KeyListener keyListeners[] = (KeyListener[]) (getListeners(KeyListener.class));
            for (int i = 0; i < keyListeners.length; i++) {
                removeKeyListener(keyListeners[i]);
            }
            // Add new key listeners
            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                   int modifiers = e.getModifiers();
                   int keyCode = e.getKeyCode();
                   if (modifiers > 0 || e.isActionKey())
                       return ;
                   char c = e.getKeyChar();
                   if (!Character.isISOControl(c)) {
                       searchTextField.setText(String.valueOf(c));
                       displaySearchField();
                   }
                }
            });
            // Create a the "multi-event" listener for the text field. Instead of
            // adding separate instances of each needed listener, we're using a
            // class which implements them all. This approach is used in order 
            // to avoid the creation of 4 instances which takes some time
            SearchFieldListener searchFieldListener = new SearchFieldListener();
            searchTextField.addKeyListener(searchFieldListener);
            searchTextField.addFocusListener(searchFieldListener);
            searchTextField.getDocument().addDocumentListener(searchFieldListener);
        }
        
        private class SearchFieldListener extends KeyAdapter 
                implements DocumentListener, FocusListener {
            SearchFieldListener() {}
            /** The last search results */
            private ArrayList results = new ArrayList();
            /** The last selected index from the search results. */
            private int currentSelectionIndex;
            
            public void changedUpdate(DocumentEvent e) {
                searchForNode();
            }
            
            public void insertUpdate(DocumentEvent e) {
                searchForNode();
            }
            
            public void removeUpdate(DocumentEvent e) {
                searchForNode();
            }
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    removeSearchField();
                    ExplorerTree.this.requestFocus();
                } else if (keyCode == KeyEvent.VK_UP) {
                    currentSelectionIndex--;
                    displaySearchResult();
                    // Stop processing the event here. Otherwise it's dispatched
                    // to the tree too (which scrolls)
                    e.consume();
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    currentSelectionIndex++;
                    displaySearchResult();
                    // Stop processing the event here. Otherwise it's dispatched
                    // to the tree too (which scrolls)
                    e.consume();
                } else if (keyCode == KeyEvent.VK_TAB) {
                    if (maxPrefix != null)
                        searchTextField.setText (maxPrefix);
                    e.consume();
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    removeSearchField();
                    expandPath(getSelectionPath());
                    ExplorerTree.this.requestFocus();
                    ExplorerTree.this.dispatchEvent(e);
                }
            }
            
            /** Searches for a node in the tree. */
            private void searchForNode() {
                currentSelectionIndex = 0;
                results.clear();
                maxPrefix = null;
                String text = searchTextField.getText().toUpperCase();
                if (text.length() > 0) {
                    doSearch(text, results);
                    displaySearchResult();
                }
            }
            
            private void displaySearchResult() {
                int sz = results.size();
                if (sz > 0) {
                    if (currentSelectionIndex < 0) {
                        currentSelectionIndex = sz - 1;
                    } else if (currentSelectionIndex >= sz) {
                        currentSelectionIndex = 0;
                    }
                    TreePath path = (TreePath) results.get(currentSelectionIndex);
                    setSelectionPath(path);
                    scrollPathToVisible(path);
                } else {
                    clearSelection();
                }
            }
            
            public void focusGained(FocusEvent e) {
                // Do nothing
            }
            
            public void focusLost(FocusEvent e) {
                removeSearchField();
            }
        }

        private int originalScrollMode;
        
        private void doSearch(String str, ArrayList results) {
            int rows[] = getSelectionRows();
            int row = (rows == null || rows.length == 0) ? 0 : rows[0];
            int rowCount = getRowCount();
            for (int i = row; i < getRowCount(); i++) {
                addPathIfMatches(str, i, results);
            }
            for (int i = 0; i < row && i < rowCount; i++) {
                addPathIfMatches(str, i, results);
            }
        }
        
        private void addPathIfMatches(String str, int row, ArrayList results) {
            TreePath path = getPathForRow(row);
            TreeNode node = (TreeNode) path.getLastPathComponent();
            String nodeName = node.toString();
            if (nodeName.toUpperCase().startsWith(str)) {
                if (maxPrefix == null)
                    maxPrefix = nodeName;
                else
                    maxPrefix = findMaxPrefix(maxPrefix, nodeName);
                results.add(path);
            }
        }
        
        private String findMaxPrefix (String str1, String str2) {
            String res = null;
            for (int i = 0; str1.regionMatches (true, 0, str2, 0, i); i++) {
                res = str1.substring (0, i);
            }
            return res;
        }

        /**
         * Adds the search field to the tree.
         */
        private void displaySearchField() {
            if (!searchTextField.isDisplayable()) {
                JViewport viewport = TreeView.this.getViewport();
                originalScrollMode = viewport.getScrollMode();
                viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
                Rectangle visibleTreeRect = getVisibleRect();
                add(searchTextField);
                repaint();
                // bugfix #28501, avoid the chars duplicated on jdk1.3
                SwingUtilities.invokeLater (new Runnable () {
                    public void run () {
                        searchTextField.requestFocus ();
                    }
                });
            }
        }

        public void paint(Graphics g) {
            Rectangle visibleRect = getVisibleRect();
            if (searchTextField.isDisplayable()) {
                searchTextField.setBounds(
                        Math.max(3, visibleRect.x + visibleRect.width - 163),
                        visibleRect.y + 3,
                        Math.min(getPreferredSize().width - 6, 160),
                        heightOfTextField);
            }
            super.paint(g);
        }
        /**
         * Removes the search field from the tree.
         */
        private void removeSearchField() {
            if (searchTextField.isDisplayable ()) {
                remove(searchTextField);
                TreeView.this.getViewport().setScrollMode(originalScrollMode);
                this.repaint();
            }
        }
	
        /** notify the Component to autoscroll */
        public void autoscroll (Point cursorLoc) {
	    getSupport().autoscroll(cursorLoc);
	}
	
	/** @return the Insets describing the autoscrolling
	 * region or border relative to the geometry of the
	 * implementing Component.
	 */
	public Insets getAutoscrollInsets () {
	    return getSupport().getAutoscrollInsets();
	}
	
	/** Safe getter for autoscroll support. */
	AutoscrollSupport getSupport() {
	    if (support == null)
		support = new AutoscrollSupport(
		    this, new Insets(15, 10, 15, 10));
	    return support;
	}

        
	public String getToolTipText(MouseEvent event) {
    	    if(event != null) {
		Point p = event.getPoint();
		int selRow = getRowForLocation(p.x, p.y);
		if(selRow != -1) {
		    TreePath path = getPathForRow(selRow);
		    VisualizerNode v = (VisualizerNode)path.getLastPathComponent();
		    String tooltip = v.getShortDescription();
		    String displayName = v.getDisplayName ();
		    if ((tooltip != null) && !tooltip.equals (displayName))
			                    return tooltip;
		}
	    }										
	    return null;
	}
	
        protected TreeModelListener createTreeModelListener() {
            return new ModelHandler();
        }
        
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleExplorerTree();
            }
            return accessibleContext;
        }
        
        private class AccessibleExplorerTree extends AccessibleJTree {
	    AccessibleExplorerTree() {}
	    
            public String getAccessibleName() {
                return TreeView.this.getAccessibleContext().getAccessibleName();
            }
            public String getAccessibleDescription() {
                return TreeView.this.getAccessibleContext().getAccessibleDescription();
            }
        }
        
        private class ModelHandler extends JTree.TreeModelHandler {
            ModelHandler() {}
	    
            public void treeStructureChanged(TreeModelEvent e) {
              
                // Remember selections and expansions
                TreePath selectionPaths[] = getSelectionPaths();
                java.util.Enumeration expanded = getExpandedDescendants( e.getTreePath() );
                
                // Restructure the node
                super.treeStructureChanged( e );                
                
                // Expand previously expanded paths
                if ( expanded != null ) {
                    while( expanded.hasMoreElements() ) {
                        expandPath( (TreePath)expanded.nextElement() );
                    }
                }
                
                // Select previously selected paths
                if ( selectionPaths != null && selectionPaths.length > 0 ) {
                    boolean wasSelected = isPathSelected(selectionPaths[0]);

                    setSelectionPaths( selectionPaths );

                    if (!wasSelected) {
                        // do not scroll if the first selection path survived structure change
                        scrollPathToVisible( selectionPaths[0] );
                    }
                }
            }
            
            public void treeNodesRemoved (TreeModelEvent e) {
                // called to removed from JTree.expandedState
                super.treeNodesRemoved (e);
                if (tree.getSelectionCount () == 0) {
                    TreePath path = findSiblingTreePath (e.getTreePath (), e.getChildIndices ());
                    if (path != null && path.getPathCount () > 0) {
                        tree.setSelectionPath (path);
                    }
                }
            }
        }
        
        /**
         * This class is copy of BasicTreeUI.TreeCancelEditingAction
         * from JDK1.4 - in JDK 1.3 the isEnabled method is wrong.
         */
        private class OurTreeCancelEditingAction extends AbstractAction {
            OurTreeCancelEditingAction() {}
            public void actionPerformed(ActionEvent e) {
                if(tree != null) {
                    tree.cancelEditing();
                }
            }

            public boolean isEnabled() {
                return (tree != null &&
                    tree.isEnabled() &&
                    ExplorerTree.this.getUI().isEditing(tree)); }
        } // End of class TreeCancelEditingAction

    }
    
    /** Returns the tree path nearby to given tree node. Either a sibling if there is or the parent.
     * @param parentPath tree path to parent of changed nodes
     * @param childIndices indexes of changed children
     * @return the tree path or null if there no changed children
     */
    final static TreePath findSiblingTreePath (TreePath parentPath, int[] childIndices) {
        if (childIndices == null) {
            throw new IllegalArgumentException ("Indexes of changed children are null."); // NOI18N
        }
        if (parentPath == null) {
            throw new IllegalArgumentException ("The tree path to parent is null."); // NOI18N
        }
        // bugfix #29342, if childIndices is the empty then don't change the selection
        if (childIndices.length == 0) {
            return null;
        }
        TreeNode parent = (TreeNode)parentPath.getLastPathComponent ();
        Object[] parentPaths = parentPath.getPath();
        TreePath newSelection = null;
        if (parent.getChildCount() > 0) {
            // get parent path, add child to it
            int childPathLength = parentPaths.length + 1;
            Object[] childPath = new Object[childPathLength];
            System.arraycopy(parentPaths, 0, childPath, 0, parentPaths.length);
            int selectedChild = childIndices[0] - 1;
            if (selectedChild < 0) {
                selectedChild = 0;
            }
            childPath[childPathLength - 1] = parent.getChildAt(selectedChild);
            newSelection = new TreePath(childPath);
        } else { 
            // all children removed, select parent
            newSelection = new TreePath(parentPaths);
        }
        return newSelection;
    }

}
