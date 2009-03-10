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

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.swing.event.*;

import org.openide.nodes.*;

/* NodeTableModel synchronizing tree and table model. Used by TreeTable.
 *
 * @author Jan Rojcek
 */
class TreeTableModelAdapter extends NodeTableModel {
    
    private JTree tree;
    private NodeTableModel nodeTableModel;

    public TreeTableModelAdapter(JTree t, NodeTableModel ntm) {
        this.tree = t;
        this.nodeTableModel = ntm;

        Listener listener = new Listener();
	tree.addTreeExpansionListener(listener);
	tree.getModel().addTreeModelListener(listener);
        nodeTableModel.addTableModelListener(listener);
    }

    // NodeTableModel methods
    
    public void setNodes(Node[] nodes) {
        nodeTableModel.setNodes(nodes);
    }

    public void setProperties(Node.Property[] props) {
        nodeTableModel.setProperties(props);
    }

    protected Node.Property getPropertyFor(Node node, Node.Property prop) {
        return nodeTableModel.getPropertyFor(node, prop);
    }

    Node nodeForRow(int row) {
        return Visualizer.findNode(tree.getPathForRow(row).getLastPathComponent());
    }

    Node.Property propertyForColumn(int column) {
        return nodeTableModel.propertyForColumn(column - 1);
    }

    // Wrappers, implementing TableModel interface. 

    public int getColumnCount() {
	return nodeTableModel.getColumnCount() + 1;
    }

    public String getColumnName(int column) {
	return column == 0 ? Visualizer.findNode(tree.getModel().getRoot()).getDisplayName()
                           : nodeTableModel.getColumnName(column - 1);
    }

    public Class getColumnClass(int column) {
	return column == 0 ? TreeTableModelAdapter.class
                           : nodeTableModel.getColumnClass(column - 1);
    }

    public int getRowCount() {
	return tree.getRowCount();
    }
    
    public Object getValueAt(int row, int column) {
        return column == 0 ? tree.getPathForRow(row).getLastPathComponent()
                           : nodeTableModel.getPropertyFor(
                                nodeForRow(row),
                                propertyForColumn(column));
    }

    public boolean isCellEditable(int row, int column) {
        return column == 0 ? true
                           : getValueAt(row, column) != null;
    }

    public void setValueAt(Object value, int row, int column) {
    }

    /* Listener for synchronizing tree and table model.
     */
    class Listener implements TreeExpansionListener, TreeModelListener, TableModelListener, Runnable {

        // selection paths stored for restore after update
        TreePath[] tps = null;
        
        ///////// TreeExpansionListener
        public void treeExpanded(TreeExpansionEvent event) {  
            updateNodes();
        }
        public void treeCollapsed(TreeExpansionEvent event) {  
            updateNodes();
        }
        
        ///////////// TreeModelListener
        // Install a TreeModelListener that can update the table when
        // tree changes. We use delayedUpdateNodes as we can
        // not be guaranteed the tree will have finished processing
        // the event before us.
        public void treeNodesChanged(TreeModelEvent e) {
            delayedUpdateNodes(e);
        }

        public void treeNodesInserted(TreeModelEvent e) {
            delayedUpdateNodes(e);
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            delayedUpdateNodes(e);
        }

        public void treeStructureChanged(TreeModelEvent e) {
            // bugfix #23757, store selection paths
            tps = tree.getSelectionPaths ();
            
            // bugfix #30355, don't restore selection when the tree root changed
            // (see javadoc TreeModelListener.treeStructureChanged)
            if (e.getPath ().length == 1 && !e.getTreePath ().equals (e.getPath ()[0])) {
                tps = null;
            }
            
            delayedUpdateNodes(e);
        }

        ///////// TableModelListener
    
        public void tableChanged(TableModelEvent e) {
            int c = e.getColumn();
            int column = c == TableModelEvent.ALL_COLUMNS ? TableModelEvent.ALL_COLUMNS
                                                          : c + 1; 
            fireTableChanged(new TableModelEvent(TreeTableModelAdapter.this, 
                                                 e.getFirstRow(), 
                                                 e.getLastRow(), 
                                                 column, 
                                                 e.getType()));
        }
        
        /**
         * Invokes fireTableDataChanged after all the pending events have been processed.
         */
        protected void delayedUpdateNodes(TreeModelEvent e) {
            // Something like this can be used for updating tree column name ?!
            //if (tree.getModel().getRoot().equals(e.getTreePath().getLastPathComponent())) {
            //    fireTableStructureChanged();
            //}
            SwingUtilities.invokeLater(this);
        }
        
        public void run() {
            updateNodes();
        }
        
        private void updateNodes() {
            Node[] nodes = new Node[tree.getRowCount()];
            for (int i = 0; i < tree.getRowCount(); i++) {
                nodes[i] = Visualizer.findNode(tree.getPathForRow(i).getLastPathComponent());
            }
            setNodes(nodes);
            // retore selection paths
            if (tps != null) {
                tree.setSelectionPaths (tps);
                tps = null;
            }
        }
    }
}
