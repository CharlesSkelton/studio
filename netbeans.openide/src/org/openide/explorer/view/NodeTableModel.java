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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;

import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;

/** 
* Table model with properties (<code>Node.Property</code>) as columns and nodes (<code>Node</code>) as rows.
* It is used as model for displaying node properties in table. Each column is represented by
* <code>Node.Property</code> object. Each row is represented by <code>Node</code> object.
* Each cell contains <code>Node.Property</code> property which equals with column object
* and should be in property sets of row representant (<code>Node</code>).
*
* @author Jan Rojcek
* @since 1.7
*/
public class NodeTableModel extends AbstractTableModel {

    private static final String ATTR_INVISIBLE = "InvisibleInTreeTableView"; // NOI18N
    static final String ATTR_COMPARABLE_COLUMN = "ComparableColumnTTV"; // NOI18N
    static final String ATTR_SORTING_COLUMN = "SortingColumnTTV"; // NOI18N
    static final String ATTR_DESCENDING_ORDER = "DescendingOrderTTV"; // NOI18N
    private static final String ATTR_ORDER_NUMBER = "OrderNumberTTV"; // NOI18N
    private static final String ATTR_TREE_COLUMN = "TreeColumnTTV"; // NOI18N
    
    /** all columns of model */
    private ArrayColumn[] allPropertyColumns = new ArrayColumn[]{};
    /** visible columns of model */
    private int[] propertyColumns = new int[]{};
    /** rows of model */
    private Node[] nodeRows = new Node[]{};
    /** sorting column */
    private int sortColumn = -1;
    /** if true, at least one column can be used to sort */
    private boolean existsComparableColumn = false;
    
    private Property treeColumnProperty = null;
    
    /** listener on node properties changes, recreates displayed data */
    private PropertyChangeListener pcl = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            //fireTableDataChanged();
            int row = rowForNode((Node)evt.getSource());
            if (row == -1) {
                return;
            }

            int column = columnForProperty(evt.getPropertyName());
            if (column == -1) {
                fireTableRowsUpdated(row, row);
            } else {
                fireTableCellUpdated(row, column);
            }
        }
    };
    
    /** Set rows.
     * @param nodes the rows
     */
    public void setNodes(Node[] nodes) {
        for (int i = 0; i < nodeRows.length; i++)
            nodeRows[i].removePropertyChangeListener(pcl);
        nodeRows = nodes;
        for (int i = 0; i < nodeRows.length; i++)
            nodeRows[i].addPropertyChangeListener(pcl);
        fireTableDataChanged();
    }
    
    /** Set columns.
     * @param props the columns
     */
    public void setProperties(Property[] props) {
        int size = props.length;
        sortColumn = -1;
        int treePosition = -1;
        for ( int i=0; i<props.length; i++ ) {            
            Object o = props[i].getValue( ATTR_TREE_COLUMN );
            boolean x;
            if ( o != null && o instanceof Boolean )
                if ( ((Boolean)o).booleanValue() ) {
                    treeColumnProperty = props[i];
                    size--;
                    treePosition = i;
                }
        }
        
        allPropertyColumns = new ArrayColumn[size];
        
        int visibleCount = 0;
        existsComparableColumn = false;
        TreeMap sort = new TreeMap();
        int i = 0;
        int ia = 0;
        while ( i < props.length ) {        
            if ( i != treePosition ) {
                allPropertyColumns[ia] = new ArrayColumn();
                allPropertyColumns[ia].setProperty( props[i] );
                if ( isVisible(props[i]) ) {
                    visibleCount++;
                    Object o = props[i].getValue( ATTR_ORDER_NUMBER );
                    if ( o != null && o instanceof Integer )
                        sort.put( new Double(((Integer)o).doubleValue()), new Integer( ia ) );
                    else
                        sort.put( new Double( ia + 0.1 ), new Integer( ia ) );
                }
                else {
                    allPropertyColumns[ia].setVisibleIndex( -1 );
                    Object o = props[i].getValue( ATTR_SORTING_COLUMN );
                    if ( o != null && o instanceof Boolean )
                        props[i].setValue( ATTR_SORTING_COLUMN, Boolean.FALSE );
                }
                if ( !existsComparableColumn ) {
                    Object o = props[i].getValue( ATTR_COMPARABLE_COLUMN );
                    if ( o != null && o instanceof Boolean )
                        existsComparableColumn = ((Boolean)o).booleanValue();
                }
                ia++;
            }
            i++;
        }
        
        // visible columns
        propertyColumns = new int[visibleCount];
        int j=0;
        Iterator it = sort.values().iterator();
        while ( it.hasNext() ) {
            i = ((Integer)it.next()).intValue();            
            allPropertyColumns[i].setVisibleIndex( j );
            propertyColumns[j] = i;
            j++;            
        }
        
        fireTableStructureChanged();
    }
    
    /* recompute set of visible columns
     */
    private void computeVisiblePorperties(int visCount) {
        
        propertyColumns = new int[visCount];
        TreeMap sort = new TreeMap();
        for ( int i=0; i<allPropertyColumns.length; i++ ) {
            int vi = allPropertyColumns[i].getVisibleIndex();
            if ( vi == -1 )
                sort.put( new Double( i - 0.1 ), new Integer( i ) );
            else
                sort.put( new Double( vi ), new Integer( i ) );
        }
        
        int j=0;
        Iterator it = sort.values().iterator();
        while ( it.hasNext() ) {
            int i = ((Integer)it.next()).intValue();
            Property p = allPropertyColumns[i].getProperty();
            if ( isVisible( p ) ) {
                propertyColumns[j] = i;
                allPropertyColumns[i].setVisibleIndex( j );
                j++;
            }
            else {
                allPropertyColumns[i].setVisibleIndex( -1 );
                Object o = p.getValue( ATTR_SORTING_COLUMN );
                if ( o != null && o instanceof Boolean )
                    if ( ((Boolean)o).booleanValue() ) {
                        p.setValue( ATTR_SORTING_COLUMN, Boolean.FALSE );
                        p.setValue( ATTR_DESCENDING_ORDER, Boolean.FALSE );
                    }
            }
        }
        fireTableStructureChanged();
    }
    
    /** Get width of visible column.
     * @param column number
     * @return column width
     */
    int getVisibleColumnWidth(int column) {
        return allPropertyColumns[propertyColumns[column]].getWidth();
    }
    
    /** Get width of column from whole property set
     * @param column number
     * @return column width
     */
    int getArrayColumnWidth(int column) {
        return allPropertyColumns[column].getWidth();
    }
    
    /** Set width of visible column.
     * @param column number
     * @param column width
     */
    void setVisibleColumnWidth(int column, int width) {
        allPropertyColumns[propertyColumns[column]].setWidth( width );
    }
    
    /** Set width of column from whole property set
     * @param column number
     * @param column width
     */
    void setArrayColumnWidth(int column, int width) {
        allPropertyColumns[column].setWidth( width );
    }
    
    /** Get index of visible column
     * @param column number from whole property set
     * @return column index
     */
    int getVisibleIndex(int arrayIndex) {
        return allPropertyColumns[arrayIndex].getVisibleIndex();
    }
    
    /** Get index of visible column
     * @param column number from whole property set
     * @return column index
     */
    int getArrayIndex(int visibleIndex) {
        for ( int i = 0; i < allPropertyColumns.length; i++ ) {
            if ( allPropertyColumns[i].getVisibleIndex() == visibleIndex )
                return i;
        }
        return -1;
    }
    
    /* If true, column property should be comparable - allows sorting
     */
    boolean isComparableColumn(int column) {
        Property p = allPropertyColumns[propertyColumns[column]].getProperty();
        Object o = p.getValue( ATTR_COMPARABLE_COLUMN );
        if ( o != null && o instanceof Boolean )
            return ((Boolean)o).booleanValue();
        return false;
    }
    
    /* If true, at least one column is comparable
     */
    boolean existsComparableColumn() {
        return existsComparableColumn;
    }
    
    /* If true, column is currently used for sorting
     */
    boolean isSortingColumn(int column) {
        Property p = allPropertyColumns[propertyColumns[column]].getProperty();
        Object o = p.getValue( ATTR_SORTING_COLUMN );
        if ( o != null && o instanceof Boolean )
            return ((Boolean)o).booleanValue();
        return false;
    }
    
    /* Sets column to be currently used for sorting
     */
    void setSortingColumn(int column) {
        if ( sortColumn != -1 ) {
            Property p = allPropertyColumns[ sortColumn ].getProperty();
            p.setValue( ATTR_SORTING_COLUMN, Boolean.FALSE );
            p.setValue( ATTR_DESCENDING_ORDER, Boolean.FALSE );
        }
        
        if ( column != -1 ) {
            sortColumn = propertyColumns[column];
            Property p = allPropertyColumns[ sortColumn ].getProperty();
            p.setValue( ATTR_SORTING_COLUMN, Boolean.TRUE );
        }
        else
            sortColumn = -1;
    }
    
    /* Gets column index of sorting column, if it's visible.
     * Otherwise returns -1.
     */
    int getVisibleSortingColumn() {        
        if ( sortColumn == -1 ) {
            for (int i = 0; i < propertyColumns.length; i++) {
                if ( isSortingColumn( i ) ) {
                    sortColumn = propertyColumns[i];
                    return i;
                }
            }
        }
        else {
            if ( isVisible( allPropertyColumns[ sortColumn ].getProperty() ) )
                return getVisibleIndex( sortColumn );
            sortColumn = -1;
        }
        return -1;
    }
    
    /* If true, current sorting uses descending order.
     */
    boolean isSortOrderDescending() {
        if ( sortColumn == -1 )
            return false;
        
        Property p = allPropertyColumns[ sortColumn ].getProperty();
        Object o = p.getValue( ATTR_DESCENDING_ORDER );
        if ( o != null && o instanceof Boolean )
            return ((Boolean)o).booleanValue();
        return false;
    }
    
    /* Sets sorting order for current sorting.
     */
    void setSortOrderDescending(boolean descending) {
        if ( sortColumn != -1 ) {
            Property p = allPropertyColumns[ sortColumn ].getProperty();
            p.setValue( ATTR_DESCENDING_ORDER, descending ? Boolean.TRUE : Boolean.FALSE );
        }
    }
    
    /** Returns node property if found in nodes property sets. Could be overriden to
     * return property which is not in nodes property sets.
     * @param node represents single row
     * @param prop represents column
     * @return nodes property
     */
    protected Property getPropertyFor(Node node, Property prop) {
        Node.PropertySet[] propSets = node.getPropertySets();
        for (int i = 0; i < propSets.length; i++) {
            Node.Property[] props = propSets[i].getProperties();
            for (int j = 0; j < props.length; j++) {
                if (prop.equals(props[j]))
                    return props[j];
            }
        }
        return null;
    }
    
    /** Helper method to ask for a node representant of row.
     */
    Node nodeForRow(int row) {
        return nodeRows[row];
    }
    
    /** Helper method to ask for a property representant of column.
     */
    Property propertyForColumn(int column) {
        if ( column == -1 )
            return treeColumnProperty;
        else
            return allPropertyColumns[propertyColumns[column]].getProperty();
    }
    
    int getArrayColumnCount() {
        return allPropertyColumns.length;
    }
    
    private int rowForNode(Node node) {
        for (int i = 0; i < nodeRows.length; i++) {
            if (node.equals(nodeRows[i]))
                return i;
        }
        return -1;
    }
    
    private int columnForProperty(String propName) {
        for (int i = 0; i < propertyColumns.length; i++) {
            if (allPropertyColumns[propertyColumns[i]].getProperty().getName().equals(propName))
                return i;
        }
        return -1;
    }
    
    /** Helper method to ask if column representing a property should be
     * visible
     */
    private boolean isVisible(Property p) {
        Object o = p.getValue( ATTR_INVISIBLE );
        if ( o != null && o instanceof Boolean )
            return !((Boolean)o).booleanValue();
        return true;
    }
    
    /** Set column representing a property to be visible
     */
    private void setVisible(Property p, boolean visible) {
        p.setValue( ATTR_INVISIBLE, !visible ? Boolean.TRUE : Boolean.FALSE );
    }
    
    //
    // TableModel methods
    //
    
    /** Getter for row count.
     * @return row count
     */
    public int getRowCount() {
        return nodeRows.length;
    }

    /** Getter for column count.
     * @return column count
     */
    public int getColumnCount() {
        return propertyColumns.length;
    }

    /** Getter for property.
     * @param row table row index
     * @param column table column index
     * @return property at (row, column)
     */
    public Object getValueAt(int row, int column) {
        return getPropertyFor(nodeRows[row], allPropertyColumns[propertyColumns[column]].getProperty());
    }

    /** Cell is editable only if it has non null value.
     * @param row table row index
     * @param column table column index
     * @return true if cell contains non null value
     */
    public boolean isCellEditable(int row, int column) {
        return getValueAt(row, column) != null;
    }

    /** Getter for column class.
     * @param column table column index
     * @return  <code>Node.Property.class</code>
     */
    public Class getColumnClass(int column) {
        return Node.Property.class;
    }

    /** Getter for column name
     * @param column table column index
     * @return display name of property which represents column
     */
    public String getColumnName(int column) {
        return allPropertyColumns[propertyColumns[column]].getProperty().getDisplayName();
    }
    
    /* display panel to set/unset set of visible columns
     */
    boolean selectVisibleColumns(String viewName, String treeColumnName, String treeColumnDesc) {
        boolean changed = false;
        
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setLayout(new GridBagLayout());
        
        ArrayList boxes = new ArrayList(allPropertyColumns.length);
        boolean[] oldvalues = new boolean[ allPropertyColumns.length ];
        int[] sortpointer = new int[ allPropertyColumns.length ];
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        labelConstraints.anchor = java.awt.GridBagConstraints.WEST;
        labelConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        labelConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 1.0;
        JLabel desc = new JLabel( NbBundle.getBundle(NodeTableModel.class).getString("LBL_ColumnDialogDesc") );
        panel.add(desc, labelConstraints);
        
        GridBagConstraints firstConstraints = new GridBagConstraints();
        firstConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        firstConstraints.anchor = java.awt.GridBagConstraints.WEST;
        firstConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        firstConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        firstConstraints.weightx = 1.0;
        JCheckBox first = new JCheckBox( treeColumnName + ": " + treeColumnDesc, true );  // NOI18N
        first.setEnabled( false );
        panel.add(first, firstConstraints);

        String boxtext;
        TreeMap sort = new TreeMap();
        for (int i = 0; i < allPropertyColumns.length; i++) {
            oldvalues[i] = isVisible( allPropertyColumns[i].getProperty() );
            boxtext = allPropertyColumns[i].getProperty().getDisplayName()
                + ": " + allPropertyColumns[i].getProperty().getShortDescription(); // NOI18N
            sort.put( boxtext, new Integer( i ));
        }

        Iterator it = sort.keySet().iterator();
        int j = 0;
        while ( it.hasNext() ) {
            boxtext = ((String)it.next());
            int i = ((Integer)sort.get( boxtext )).intValue();
            JCheckBox b = new JCheckBox(
                boxtext,
                oldvalues[i] );
            sortpointer[j] = i;
            panel.add(b, gridBagConstraints);
            boxes.add(b);
            j++;
        }
        
        String title = NbBundle.getBundle(NodeTableModel.class).getString("LBL_ColumnDialogTitle");
        if ( viewName != null && viewName.length() > 0 )
            title = viewName + " - " + title;  // NOI18N
        DialogDescriptor dlg = new DialogDescriptor(
                panel,
                title,
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.DEFAULT_ALIGN,
                null,
                null
           );
        
        final Dialog dialog = DialogDisplayer.getDefault().createDialog(dlg);
        dialog.show();
        
        if (dlg.getValue().equals( DialogDescriptor.OK_OPTION )) {

            int num = boxes.size();
            int nv = 0;
            for ( int i = 0; i < num; i++ ) {
                JCheckBox b = (JCheckBox)boxes.get(i);
                
                j = sortpointer[i];
                if ( b.isSelected() != oldvalues[j] ) {
                    setVisible( allPropertyColumns[j].getProperty(), b.isSelected() );
                    changed = true;
                }
                if ( b.isSelected() ) {
                    nv++;
                }
            }

            // Don't allow the user to disable ALL columns
            /*
            if (nv == 0) {
                setVisible( allPropertyColumns[0].getProperty(), true );
                nv = 1;
            }
             */
            if ( changed )
                computeVisiblePorperties( nv );
        }
        return changed;
    }
    
    void moveColumn(int from, int to) {
        int i = propertyColumns[ from ];
        int j = propertyColumns[ to ];
        
        propertyColumns[ from ] = j;
        propertyColumns[ to ] = i;
        
        allPropertyColumns[i].setVisibleIndex( to );
        allPropertyColumns[j].setVisibleIndex( from );

        sortColumn = -1;
    }
    
    /* class representing property column
     */
    private class ArrayColumn {
        ArrayColumn() {}
        /** Property representing column */
        private Property property;
        
        /** Preferred width of column */
        private int width;
        
        /** Column index in table, if it's visible */
        private int visibleIndex;
        
        /** Getter for property property.
         * @return Value of property property.
         */
        public Property getProperty() {
            return this.property;
        }
        
        /** Setter for property property.
         * @param property New value of property property.
         */
        public void setProperty(Property property) {
            this.property = property;
        }
        
        /** Getter for property width.
         * @return Value of property width.
         */
        public int getWidth() {
            return this.width;
        }
        
        /** Setter for property width.
         * @param width New value of property width.
         */
        public void setWidth(int width) {
            this.width = width;
        }
        
        /** Getter for property visibleIndex.
         * @return Value of property visibleIndex.
         */
        public int getVisibleIndex() {
            return this.visibleIndex;
        }
        
        /** Setter for property visibleIndex.
         * @param visibleIndex New value of property visibleIndex.
         */
        public void setVisibleIndex(int visibleIndex) {
            this.visibleIndex = visibleIndex;
            property.setValue( ATTR_ORDER_NUMBER, new Integer( visibleIndex ) );
        }
        
    }
}
