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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.plaf.metal.MetalScrollBarUI;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.accessibility.AccessibleContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.util.actions.SystemAction;
import org.openide.util.NbBundle;
import org.openide.awt.MouseUtils;

/** Explorer view. Allows to view tree of nodes on the left
 * and its properties in table on the right.
 *
 * @author  jrojcek
 * @since 1.7
 * @see <a href="../doc-files/api.html#cust-treetableview">Customizing the <code>TreeTableView</code> (Explorer API)</a>
 */
public class TreeTableView extends BeanTreeView {
        
    /** The table */
    protected JTable treeTable;
    private NodeTableModel tableModel;

    // Tree scroll support
    private JScrollBar hScrollBar;
    private JScrollPane scrollPane;
    private ScrollListener listener;

    // hiding columns allowed
    private boolean allowHideColumns = false;
    // sorting by column allowed
    private boolean allowSortingByColumn = false;
    // hide horizontal scrollbar
    private boolean hideHScrollBar = false;
    // button in corner of scroll pane
    private JButton colsButton = null;
    // tree model with sorting support
    private SortedNodeTreeModel sortedNodeTreeModel;
    /** Listener on keystroke to invoke default action */
    private ActionListener defaultTreeActionListener;
    // default treetable header renderer
    private TableCellRenderer defaultHeaderRenderer = null;
    
    private MouseUtils.PopupMouseAdapter tableMouseListener;
    
    /** Accessible context of this class (implemented by inner class AccessibleTreeTableView). */
    private AccessibleContext accessContext;

    // icon of column button
    private static final String COLUMNS_ICON = "/org/openide/resources/columns.gif"; // NOI18N
    // icons of ascending/descending order in column header
    private static final String SORT_ASC_ICON = "org/openide/resources/columnsSortedAsc.gif"; // NOI18N
    private static final String SORT_DESC_ICON = "org/openide/resources/columnsSortedDesc.gif"; // NOI18N
    
    private TreeColumnProperty treeColumnProperty = new TreeColumnProperty();
    private int treeColumnWidth;

    /** Create TreeTableView with default NodeTableModel
     */
    public TreeTableView() {
        this(new NodeTableModel());
    }

    /** Creates TreeTableView with provided NodeTableModel.
     * @param ntm node table model
     */
    public TreeTableView(NodeTableModel ntm) {
        tableModel = ntm;
        
        initializeTreeTable();
        setPopupAllowed(true);
        setDefaultActionAllowed(true);
        
        initializeTreeScrollSupport();
        
        // add scrollbar and scrollpane into a panel
        JPanel p = new CompoundScrollPane();
        p.setLayout(new BorderLayout());
        scrollPane.setViewportView(treeTable);
        p.add(BorderLayout.CENTER, scrollPane);

        ImageIcon ic = new ImageIcon(TreeTable.class.getResource ( COLUMNS_ICON )); // NOI18N
        colsButton = new javax.swing.JButton( ic );
        colsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
               selectVisibleColumns();
            }
        });

        JPanel sbp = new JPanel();
        sbp.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sbp.add(hScrollBar);
        p.add(BorderLayout.SOUTH, sbp);
        
        super.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        super.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        setViewportView(p);
    }

    /* Overriden to allow hide special horizontal scrollbar
     */
    public void setHorizontalScrollBarPolicy(int policy) {
        hideHScrollBar = ( policy == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        if ( hideHScrollBar ) {
            hScrollBar.setVisible( false );
            ((TreeTable)treeTable).setTreeHScrollingEnabled( false );
        }
    }
    
    /* Overriden to delegate policy of vertical scrollbar to inner scrollPane
     */
    public void setVerticalScrollBarPolicy(int policy) {
        if ( scrollPane == null )
            return;
        
        allowHideColumns = ( policy == JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        if ( allowHideColumns )
            scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, colsButton);
        treeTable.getTableHeader().setReorderingAllowed( allowHideColumns );

        scrollPane.setVerticalScrollBarPolicy( policy );
    }
    
    protected NodeTreeModel createModel() {
        return getSortedNodeTreeModel ();
    }
    
    /* Sets sorting ability
     */
    private void setAllowSortingByColumn( boolean allow ) {
        if ( allow && (allow != allowSortingByColumn )) {
            addMouseListener( new MouseAdapter() {
                public void mouseClicked (MouseEvent evt) {
                    Component c = evt.getComponent ();
                    if (c instanceof JTableHeader) {
                        JTableHeader h = (JTableHeader)c;
                        int index = h.columnAtPoint (evt.getPoint ());
                        clickOnColumnAction( index - 1 );
                    }
                }
            });
        }
        allowSortingByColumn = allow;
    }
    
    /* Change sorting after clicking on comparable column header.
     * Cycle through ascending -> descending -> no sort -> (start over)
     */
    private void clickOnColumnAction(int index ) {
        if ( index == -1 ) {
            if ( treeColumnProperty.isComparable() )
                if ( treeColumnProperty.isSortingColumn() ) {
                    if (!treeColumnProperty.isSortOrderDescending())
                        setSortingOrder(false);
                    else {
                        noSorting();
                    }
                }
                else {
                    setSortingColumn( index );
                    setSortingOrder( true );
                }
        }
        else if ( tableModel.isComparableColumn( index ) ) {
            if ( tableModel.isSortingColumn( index ) ) {
                if (!tableModel.isSortOrderDescending())
                    setSortingOrder(false);
                else {
                    noSorting();
                }
            }
            else {
                setSortingColumn( index );
                setSortingOrder( true );
            }
        }
    }
    
    private void selectVisibleColumns() {
        setCurrentWidths();
        String viewName = null;
        if ( getParent() != null )
            viewName = getParent().getName();
        if ( tableModel.selectVisibleColumns( viewName, treeTable.getColumnName(0),
                                              getSortedNodeTreeModel ().getRootDescription() ) ) {
            if ( tableModel.getVisibleSortingColumn() == -1 )
                getSortedNodeTreeModel ().setSortedByProperty( null );
            setTreePreferredWidth( treeColumnWidth );
            for (int i=0; i < tableModel.getColumnCount(); i++) {
                setTableColumnPreferredWidth( tableModel.getArrayIndex( i ), tableModel.getVisibleColumnWidth( i ) );
            }
        }
    }
    
    private void setCurrentWidths() {
        treeColumnWidth = treeTable.getColumnModel().getColumn( 0 ).getWidth();
        for (int i=0; i < tableModel.getColumnCount(); i++) {
            int w = treeTable.getColumnModel().getColumn(i + 1).getWidth();
            tableModel.setVisibleColumnWidth( i, w );
        }
    }
    
    /** Do not initialize tree now. We will do it from our constructor.
     * [dafe] Used probably because this method is called *before* superclass
     * is fully created (constructor finished) which is horrible but I don't
     * have enough knowledge about this code to change it.
     */
    void initializeTree () {
    }

    /** Initialize tree and treeTable.
     */
    private void initializeTreeTable() {
        treeModel = createModel();
        treeTable = new TreeTable(treeModel, tableModel);
        tree = ((TreeTable)treeTable).getTree();

        defaultHeaderRenderer = treeTable.getTableHeader().getDefaultRenderer();
        treeTable.getTableHeader().setDefaultRenderer( new SortingHeaderRenderer() );
        
        // init listener & attach it to closing of
        managerListener = new TreePropertyListener();
        tree.addTreeExpansionListener (managerListener);
        
        // add listener to sort a new expanded folders
        tree.addTreeExpansionListener (new TreeExpansionListener () {
            public void treeExpanded (TreeExpansionEvent event) {
                TreePath path = event.getPath ();
                if (path != null) {
                    // bugfix $32480, store and recover currently expanded subnodes
                    // store expanded paths
                    Enumeration en = TreeTableView.this.tree.getExpandedDescendants (path);
                    // sort children
                    getSortedNodeTreeModel ().sortChildren ((VisualizerNode)path.getLastPathComponent ());
                    // expand again folders
                    while (en.hasMoreElements ()) {
                        TreeTableView.this.tree.expandPath ((TreePath)en.nextElement ());
                    }
                }
            }

            public void treeCollapsed (TreeExpansionEvent event) {
                // ignore it
            }
        });
        
	defaultActionListener = new PopupSupport();
        treeTable.addFocusListener(defaultActionListener);
        tree.addMouseListener(defaultActionListener);
        
        tableMouseListener = new MouseUtils.PopupMouseAdapter() {
                public void showPopup(MouseEvent mevt) {
                    if (isPopupAllowed()) {
                        if ( mevt.getY() > treeTable.getHeight() )
                            // clear selection, if click under the table
                            treeTable.clearSelection();
                        createPopup(mevt);
                    }
                }
            };
        treeTable.addMouseListener( tableMouseListener );
        treeTable.setGridColor( UIManager.getColor("control") );
    }
    
    /** Overrides JScrollPane's getAccessibleContext() method to use internal accessible context.
     */
    public AccessibleContext getAccessibleContext()
    {
        if (accessContext == null)
            accessContext = new AccessibleTreeTableView();
        return accessContext;
    }
    
    /** This is internal accessible context for TreeTableView.
     * It delegates setAccessibleName and setAccessibleDescription methods to set these properties
     * in underlying TreeTable as well.
     */
    private class AccessibleTreeTableView extends AccessibleJScrollPane
    {
        
        AccessibleTreeTableView()
        {
        }
        
        public void setAccessibleName(String accessibleName)
        {
            super.setAccessibleName(accessibleName);
            if (treeTable != null)
                treeTable.getAccessibleContext().setAccessibleName(accessibleName);
        }
        
        public void setAccessibleDescription(String accessibleDescription)
        {
            super.setAccessibleDescription(accessibleDescription);
            if (treeTable != null)
                treeTable.getAccessibleContext().setAccessibleDescription(accessibleDescription);
        }
        
    }
    
    /** Initialize full support for horizontal scrolling.
     */
    private void initializeTreeScrollSupport() {
        scrollPane = new JScrollPane() {
            public void setBorder(Border b) {
                super.setBorder(null);
            }
        };
        scrollPane.getViewport().setBackground(UIManager.getColor("Table.background")); // NOI18N

        hScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
	hScrollBar.putClientProperty(MetalScrollBarUI.FREE_STANDING_PROP, Boolean.FALSE);

        listener = new ScrollListener();
        
        treeTable.addPropertyChangeListener(listener);
        scrollPane.getViewport().addComponentListener(listener);
        tree.addPropertyChangeListener(listener);
        hScrollBar.getModel().addChangeListener(listener);
    }

    /* Overriden to work well with treeTable.
     */
    public void setPopupAllowed (boolean value) {
        if (tree == null) {
            return;
        }
        
        if (popupListener == null && value) {
            // on
            popupListener = new PopupAdapter () {
                    protected void showPopup (MouseEvent e) {
                        int selRow = tree.getRowForLocation(e.getX(), e.getY());

                        if (!tree.isRowSelected(selRow)) {
                            tree.setSelectionRow(selRow);
                        }
                    }
            };

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
    
    /* Overriden to work well with treeTable.
     */
    public void setDefaultActionAllowed(boolean value) {
        if (tree == null)
            return;

        defaultActionEnabled = value;

        if(value) {
            defaultTreeActionListener = new DefaultTreeAction();
            treeTable.registerKeyboardAction(
                defaultTreeActionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                JComponent.WHEN_FOCUSED
            );
        } else {
            // Switch off.
            defaultTreeActionListener = null;
            treeTable.unregisterKeyboardAction(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false)
            );
        }
    }

    /** Set columns.
     * @param props each column is constructed from Node.Property
     */
    public void setProperties(Property[] props) {        
        tableModel.setProperties(props);
        treeColumnProperty.setProperty( tableModel.propertyForColumn( -1 ) );
        if ( treeColumnProperty.isComparable() || tableModel.existsComparableColumn() ) {
            setAllowSortingByColumn( true );
            if ( treeColumnProperty.isSortingColumn() ) {
                getSortedNodeTreeModel ().setSortedByName( true,
                                        !treeColumnProperty.isSortOrderDescending() );
            }
            else {
                int index = tableModel.getVisibleSortingColumn();
                if ( index != -1 ) {
                    getSortedNodeTreeModel ().setSortedByProperty( tableModel.propertyForColumn( index ), 
                                            !tableModel.isSortOrderDescending() );
                }
            }
        }
    }

    /** Sets resize mode of table.
     * 
     * @param mode - One of 5 legal values: <pre>JTable.AUTO_RESIZE_OFF,
     *                                           JTable.AUTO_RESIZE_NEXT_COLUMN,
     *                                           JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, 
     *                                           JTable.AUTO_RESIZE_LAST_COLUMN, 
     *                                           JTable.AUTO_RESIZE_ALL_COLUMNS</pre>
     */    
    public final void setTableAutoResizeMode(int mode) {
        treeTable.setAutoResizeMode(mode);
    }

    /** Gets resize mode of table.
     * 
     * @return mode - One of 5 legal values: <pre>JTable.AUTO_RESIZE_OFF,
     *                                           JTable.AUTO_RESIZE_NEXT_COLUMN,
     *                                           JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, 
     *                                           JTable.AUTO_RESIZE_LAST_COLUMN, 
     *                                           JTable.AUTO_RESIZE_ALL_COLUMNS</pre>
     */    
    public final int getTableAutoResizeMode() {
        return treeTable.getAutoResizeMode();
    }

    /** Sets preferred width of table column
     * @param index column index
     * @param width preferred column width
     */    
    public final void setTableColumnPreferredWidth(int index, int width) {
        tableModel.setArrayColumnWidth( index, width );
        int j = tableModel.getVisibleIndex( index );
        if ( j != -1 )
            treeTable.getColumnModel().getColumn(j + 1).setPreferredWidth( width );
    }
    
    /** Gets preferred width of table column
     * @param index column index
     * @return preferred column width
     */    
    public final int getTableColumnPreferredWidth(int index) {
        return tableModel.getArrayColumnWidth( index );
    }
    
    /** Set preferred size of tree view
     * @param width preferred width of tree view
     */
    public final void setTreePreferredWidth(int width) {
        treeTable.getColumnModel().getColumn(((TreeTable)treeTable).getTreeColumnIndex()).setPreferredWidth(width);
    }
    
    /** Get preferred size of tree view
     * @return preferred width of tree view
     */
    public final int getTreePreferredWidth() {
        return treeTable.getColumnModel().getColumn(((TreeTable)treeTable).getTreeColumnIndex()).getPreferredWidth();
    }

    public void addNotify() {
        // to allow displaying popup also in blank area
        if ( treeTable.getParent() != null ) {
            treeTable.getParent().addMouseListener( tableMouseListener );
        }
        super.addNotify();
    }

    public void removeNotify() {
        super.removeNotify();
        
        // clear node listeners
        tableModel.setNodes(new Node[] {});
    }

    public void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
        treeTable.getTableHeader().addMouseListener(l);
    }
    
    public void removeMouseListener(MouseListener l) {
        super.removeMouseListener(l);
        treeTable.getTableHeader().removeMouseListener(l);
    }

    /* DnD is not implemented for treeTable.
     */
    public void setDragSource (boolean state) {
    }

    /* DnD is not implemented for treeTable.
     */
    public void setDropTarget (boolean state) {
    }

    /* Overriden to get position for popup invoked by keyboard
     */
    Point getPositionForPopup() {
        int row = treeTable.getSelectedRow();
        if ( row < 0 )
            return null;
        int col = treeTable.getSelectedColumn();
        if ( col < 0 )
            col = 0;
        
        Rectangle r = null;
        if ( col == 0 )
            r = tree.getRowBounds( row );
        else
            r = treeTable.getCellRect( row, col, true );
        Point p = SwingUtilities.convertPoint( treeTable, r.x, r.y, this);
        
        return p;
    }
    
    private void createPopup(MouseEvent e) {
        int xpos=e.getX();
        int ypos=e.getY();
        Point p = SwingUtilities.convertPoint(e.getComponent(),xpos, ypos,TreeTableView.this);
        int mxpos=(int)p.getX();
        int mypos=(int)p.getY();
        xpos -= ((TreeTable)treeTable).getPositionX();
        
        if ( allowHideColumns || allowSortingByColumn ) {
            int col = treeTable.getColumnModel().getColumnIndexAtX( xpos );
            super.createExtendedPopup( mxpos, mypos, getListMenu( col ) );
        }
        else
            super.createPopup(mxpos, mypos);
    }
    
    /* creates List Options menu
     */
    private JMenu getListMenu(final int col) {
        JMenu listItem = new JMenu( NbBundle.getBundle(NodeTableModel.class).getString("LBL_ListOptions") );
        
        if ( allowHideColumns && col > 0 ) {
            JMenu colsItem = new JMenu( NbBundle.getBundle(NodeTableModel.class).getString("LBL_ColsMenu") );

            boolean addColsItem = false;
            if ( col > 1 ) {
                JMenuItem moveLItem = new JMenuItem( NbBundle.getBundle(NodeTableModel.class).getString("LBL_MoveLeft") );
                moveLItem.addActionListener( new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                        treeTable.getColumnModel().moveColumn( col, col - 1 );
                    }
                });
                colsItem.add( moveLItem );
                addColsItem = true;
            }
            if ( col < tableModel.getColumnCount() ) {
                JMenuItem moveRItem = new JMenuItem( NbBundle.getBundle(NodeTableModel.class).getString("LBL_MoveRight") );
                moveRItem.addActionListener( new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                        treeTable.getColumnModel().moveColumn( col, col + 1 );
                    }
                });
                colsItem.add( moveRItem );
                addColsItem = true;
            }
            if ( addColsItem )
                listItem.add( colsItem );
        }
        
        if ( allowSortingByColumn ) {
            JMenu sortItem = new JMenu( NbBundle.getBundle(NodeTableModel.class).getString("LBL_SortMenu") );
            JRadioButtonMenuItem noSortItem = new JRadioButtonMenuItem( 
                        NbBundle.getBundle(NodeTableModel.class).getString("LBL_NoSort"), 
                        !getSortedNodeTreeModel ().isSortingActive() );
            noSortItem.addActionListener( new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    noSorting();
                }
            });
            sortItem.add( noSortItem );
            int visibleComparable = 0;
            JRadioButtonMenuItem colItem;
            if ( treeColumnProperty.isComparable() ) {
                visibleComparable++;
                colItem = new JRadioButtonMenuItem( 
                        treeTable.getColumnName(0), 
                        treeColumnProperty.isSortingColumn() );
                colItem.setHorizontalTextPosition( SwingConstants.LEFT );
                colItem.addActionListener( new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                        setSortingColumn( -1 );
                    }
                });
                sortItem.add( colItem );
            }
            for (int i=0; i < tableModel.getColumnCount(); i++) {
                if ( tableModel.isComparableColumn( i ) ) {
                    visibleComparable++;
                    colItem = new JRadioButtonMenuItem( 
                            tableModel.getColumnName( i ), 
                            tableModel.isSortingColumn( i ) );
                    colItem.setHorizontalTextPosition( SwingConstants.LEFT );
                    final int index = i;            
                    colItem.addActionListener( new ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                            setSortingColumn( index );
                        }
                    });
                    sortItem.add( colItem );
                }
            }
            if ( visibleComparable > 0 ) {
                sortItem.addSeparator();
                boolean current_sort;
                if ( treeColumnProperty.isSortingColumn() )
                    current_sort = treeColumnProperty.isSortOrderDescending();
                else
                    current_sort = tableModel.isSortOrderDescending();
                JRadioButtonMenuItem ascItem = new JRadioButtonMenuItem( 
                        NbBundle.getBundle(NodeTableModel.class).getString("LBL_Ascending"), 
                        !current_sort );
                ascItem.setHorizontalTextPosition( SwingConstants.LEFT );
                ascItem.addActionListener( new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                        setSortingOrder( true );
                    }
                });
                sortItem.add( ascItem );

                JRadioButtonMenuItem descItem = new JRadioButtonMenuItem( 
                        NbBundle.getBundle(NodeTableModel.class).getString("LBL_Descending"), 
                        current_sort );
                descItem.setHorizontalTextPosition( SwingConstants.LEFT );
                descItem.addActionListener( new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                        setSortingOrder( false );
                    }
                });
                sortItem.add( descItem );
            
                if ( ! getSortedNodeTreeModel ().isSortingActive() ) {
                    ascItem.setEnabled( false );
                    descItem.setEnabled( false );
                }

                listItem.add( sortItem );
            }
        }
        
        if ( allowHideColumns ) {
            JMenuItem visItem = new JMenuItem( NbBundle.getBundle(NodeTableModel.class).getString("LBL_ChangeColumns") );
            visItem.addActionListener( new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                    selectVisibleColumns();
                }
            });

            listItem.add( visItem );
        }
        
        return listItem;
    }
    
    /* Sets column to be currently used for sorting
     */
    private void setSortingColumn(int index) {
        tableModel.setSortingColumn( index );
        if ( index != -1 ) {
            getSortedNodeTreeModel ().setSortedByProperty( tableModel.propertyForColumn( index ),
                    !tableModel.isSortOrderDescending());
            treeColumnProperty.setSortingColumn( false );
        }
        else {
            getSortedNodeTreeModel ().setSortedByName( true, !treeColumnProperty.isSortOrderDescending() );
            treeColumnProperty.setSortingColumn( true );
        }   
        // to change sort icon
        treeTable.getTableHeader().repaint();
    }
    
    private void noSorting() {
        tableModel.setSortingColumn( -1 );
        getSortedNodeTreeModel ().setNoSorting();
        treeColumnProperty.setSortingColumn( false );
        // to change sort icon
        treeTable.getTableHeader().repaint();
    }
    
    /* Sets sorting order for current sorting.
     */
    private void setSortingOrder(boolean ascending) {
        if ( treeColumnProperty.isSortingColumn() )
            treeColumnProperty.setSortOrderDescending( !ascending );
        else
            tableModel.setSortOrderDescending( !ascending );
        getSortedNodeTreeModel ().setSortOrder( ascending );
        // to change sort icon
        treeTable.getTableHeader().repaint();
    }

    /* Horizontal scrolling support.
     */
    private final class ScrollListener extends ComponentAdapter implements PropertyChangeListener, ChangeListener {
        ScrollListener() {}        
        boolean movecorrection = false;
        //Column width
        public void propertyChange(PropertyChangeEvent evt) {
            if (((TreeTable)treeTable).getTreeColumnIndex() == -1)
                return;
            
            if ("width".equals(evt.getPropertyName())) {   // NOI18N
                if (!treeTable.equals(evt.getSource())) {
                    Dimension dim = hScrollBar.getPreferredSize();
                    dim.width = treeTable.getColumnModel().getColumn(((TreeTable)treeTable).getTreeColumnIndex()).getWidth();
                    hScrollBar.setPreferredSize(dim);
                    hScrollBar.revalidate();
                    hScrollBar.repaint();
                }
                revalidateScrollBar();
            } else if ("positionX".equals(evt.getPropertyName())) {   // NOI18N
                revalidateScrollBar();
            } else if ("treeColumnIndex".equals(evt.getPropertyName())) {   // NOI18N
                treeTable.getColumnModel().getColumn(((TreeTable)treeTable).getTreeColumnIndex()).addPropertyChangeListener(listener);
            } else if ("column_moved".equals(evt.getPropertyName())) {   // NOI18N
                int from = ((Integer)evt.getOldValue()).intValue();
                int to = ((Integer)evt.getNewValue()).intValue();
                if ( from == 0 || to == 0 ) {
                    if ( movecorrection )
                        movecorrection = false;
                    else {
                        movecorrection = true;
                        // not allowed to move first, tree column
                        treeTable.getColumnModel().moveColumn( to, from );
                    }
                    return;
                }
                // module will be revalidated in NodeTableModel
                treeTable.getTableHeader().getColumnModel().getColumn( from ).setModelIndex( from );
                treeTable.getTableHeader().getColumnModel().getColumn( to ).setModelIndex( to );                
                tableModel.moveColumn( from - 1, to - 1);
            }
        }

        //Viewport height
        public void componentResized(ComponentEvent e) {
            revalidateScrollBar();
        }
        
        //ScrollBar change
        public void stateChanged(ChangeEvent evt) {
            int value = hScrollBar.getModel().getValue();
            ((TreeTable)treeTable).setPositionX(value);
        }
        
        private void revalidateScrollBar() {
            int extentWidth = treeTable.getColumnModel().getColumn(((TreeTable)treeTable).getTreeColumnIndex()).getWidth();
            int maxWidth = tree.getPreferredSize().width;
            int extentHeight = scrollPane.getViewport().getSize().height;
            int maxHeight = tree.getPreferredSize().height;
            int positionX = ((TreeTable)treeTable).getPositionX();

            int value = Math.max(0, Math.min(positionX, maxWidth - extentWidth));

            boolean hsbvisible = hScrollBar.isVisible();
            boolean vsbvisible = scrollPane.getVerticalScrollBar().isVisible();
            int hsbheight = hsbvisible ? hScrollBar.getHeight() : 0;
            int vsbwidth = scrollPane.getVerticalScrollBar().getWidth();

            hScrollBar.setValues(value, extentWidth, 0, maxWidth);
            
            if (hideHScrollBar || maxWidth <= extentWidth
                || (vsbvisible && (maxHeight <= extentHeight + hsbheight 
                                   && maxWidth <= extentWidth + vsbwidth)))
                hScrollBar.setVisible(false);
            else
                hScrollBar.setVisible(true);

        }
    }
    
    /** Scrollable (better say not scrollable) pane. Used as container for 
     * left (controlling) and rigth (controlled) scroll panes.
     */
    private static final class CompoundScrollPane extends JPanel implements Scrollable {
        CompoundScrollPane() {}
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
        
        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
        
        public Dimension getPreferredScrollableViewportSize() {
            return this.getPreferredSize();
        }
        
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
        
    }
    
    /** Invokes default action.
     */
    private class DefaultTreeAction implements ActionListener {
        DefaultTreeAction() {}
        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            if (treeTable.getSelectedColumn() != ((TreeTable)treeTable).getTreeColumnIndex())
                return;
            
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
    
    private synchronized SortedNodeTreeModel getSortedNodeTreeModel () {
        if (sortedNodeTreeModel == null) {
            sortedNodeTreeModel = new SortedNodeTreeModel();
        }
        return sortedNodeTreeModel;
    }
    
    /* node tree model with added sorting support
     */
    private class SortedNodeTreeModel extends NodeTreeModel {
        SortedNodeTreeModel() {}
        private Node.Property sortedByProperty;
        private boolean sortAscending = true;
        private Comparator rowComparator;
        private boolean sortedByName = false;
        private boolean noSorting = false;

        void setNoSorting() {
            noSorting = true;
            setSortedByProperty( null );
            setSortedByName( false );
        }
        
        boolean isSortingActive () {
            return ( sortedByProperty != null || sortedByName );
        }
        
        void setSortedByProperty(Node.Property prop) {
            if (sortedByProperty == prop)
                return;

            sortedByProperty = prop;
            if (prop == null)
                rowComparator = null;
            else
                sortedByName = false;
            sortingChanged();
        }
        
        void setSortedByProperty(Node.Property prop, boolean ascending) {
            if (sortedByProperty == prop && ascending == sortAscending)
                return;

            sortedByProperty = prop;
            sortAscending = ascending;
            if (prop == null)
                rowComparator = null;
            else
                sortedByName = false;
            sortingChanged();
        }
        
        void setSortedByName(boolean sorted, boolean ascending) {
            if (sortedByName == sorted && ascending == sortAscending)
                return;
            
            sortedByName = sorted;
            sortAscending = ascending;
            if ( sortedByName )
                sortedByProperty = null;
            sortingChanged();
        }
        
        void setSortedByName(boolean sorted) {
            sortedByName = sorted;
            if ( sortedByName )
                sortedByProperty = null;
            sortingChanged();
        }

        void setSortOrder(boolean ascending) {
            if (ascending == sortAscending)
                return;

            sortAscending = ascending;
            sortingChanged();
        }
        
        private Node.Property getNodeProperty(Node node, Node.Property prop) {
            Node.PropertySet[] propsets = node.getPropertySets();
            for (int i = 0, n = propsets.length; i < n; i++) {
                Node.Property[] props = propsets[i].getProperties();

                for (int j = 0, m = props.length; j < m; j++) {
                    if (props[j].equals(prop)) {
                        return props[j];
                    }
                }
            }
            return null;
        }
        
        synchronized Comparator getRowComparator() {
            if (rowComparator == null) {
                rowComparator = new Comparator() {

                    public int compare(Object o1, Object o2) {
                        if (o1 == o2)
                            return 0;

                        Node n1 = ((VisualizerNode) o1).node;
                        Node n2 = ((VisualizerNode) o2).node;
                        
                        if (n1 == null && n2 == null) return 0;
                        if (n1 == null) return 1;
                        if (n2 == null) return -1;
                        
                        if (n1.getParentNode () == null || n2.getParentNode () == null) {
                            // PENDING: throw Exception
                            ErrorManager.getDefault ().log ("Warning: TTV.compare: Node " + n1 + " or " + n2 + " has no parent!"); // NOI18N
                            return 0;
                        }
                        
                        if (!(n1.getParentNode ().equals (n2.getParentNode ()))) {
                            // PENDING: throw Exception
                            ErrorManager.getDefault ().log ("Warning: TTV.compare: Nodes " + n1 + " and " + n2 + " has different parent!"); // NOI18N
                            return 0;
                        }
                        
                        int res = 0;

                        if ( sortedByName ) {
                            res = n1.getDisplayName().compareTo(n2.getDisplayName());
                            return sortAscending ? res : -res;
                        }

                        Node.Property p1 = getNodeProperty(n1, sortedByProperty);
                        Node.Property p2 = getNodeProperty(n2, sortedByProperty);
                        
                        if ( p1 == null && p2 == null ) {
                            return 0;
                        }
                        
                        try {
                            if ( p1 == null )
                                res = -1;
                            else if ( p2 == null )
                                res = 1;
                            else {
                                Object v1 = p1.getValue();
                                Object v2 = p2.getValue();
                                if ( v1 == null && v2 == null )
                                    return 0;
                                else if( v1 == null )
                                    res = -1;
                                else if ( v2 == null )
                                    res = 1;
                                else {
                                    if (v1.getClass() != v2.getClass() || !(v1 instanceof Comparable)) {
                                        v1 = v1.toString();
                                        v2 = v2.toString();
                                    }
                                    res = ((Comparable)v1).compareTo(v2);
                                }
                            }
                            return sortAscending ? res : -res;
                        }
                        catch (Exception ex) {
                            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
                            return 0;
                        }
                    }
                    
                };
            }
            return rowComparator;
        }

        void sortChildren (VisualizerNode parent) {
            final Comparator comparator = getRowComparator();
            if (comparator == null || parent == null) {
                return;
            }
            java.util.List nodeList = parent.getChildren ();
            int size = nodeList.size ();
            java.util.List newOrder = new ArrayList (nodeList);
            for (int i = 0; i < size; i++) {
                newOrder.set (i, nodeList.get (i));
            }
            
            int [] perm = new int[size];
            boolean ignore = false;
            
            if (isSortingActive ()) {
                Collections.sort(newOrder, comparator);
                for (int i = 0; i < size; i++) {
                    perm[i] = newOrder.indexOf (nodeList.get (i));
                }
            } else {
                Node root = parent.node;
                java.util.List originalSort = Arrays.asList (root.getChildren ().getNodes ());
                
                // check sizes
                if (size != originalSort.size ()) {
                    ignore = true;
                }
                
                // calculate permutation
                for (int i = 0; !ignore && i < size; i++) {
                    perm[i] = originalSort.indexOf (( (VisualizerNode)nodeList.get (i)).node );
                    ignore = perm[i] == -1;
                }
            }
            
            // ??? how to call reorder better
            // parent.childrenReordered (new NodeReorderEvent (parent.node, perm));
            // workaround, call helper method in VisualizerNode
            if (!ignore) {
                parent.doChildrenReordered (perm);
            }
        }
        
        void sortingChanged() {
            // PENDING: remember the last sorting to avoid multiple sorting
            
            // remenber expanded folders
            TreeNode tn = (TreeNode)(this.getRoot());
            java.util.List list = new ArrayList();
            Enumeration en = TreeTableView.this.tree.getExpandedDescendants( new TreePath( tn ) );
            while ( en != null && en.hasMoreElements() ) {
                TreePath path = (TreePath)en.nextElement();
                // bugfix #32328, don't sort whole subtree but only expanded folders
                sortChildren ((VisualizerNode)path.getLastPathComponent ());
                list.add( path );
            }
            
            // expand again folders
            for (int i=0; i<list.size(); i++) {
                TreeTableView.this.tree.expandPath( (TreePath)list.get( i ) );
            }
        }
        
        String getRootDescription() {
            if ( getRoot() instanceof VisualizerNode ) {
                sortChildren ((VisualizerNode)getRoot ());
                return ((VisualizerNode)getRoot()).getShortDescription();
            }
            return "";    // NOI18N
        }
        
        // overrided mothod from DefaultTreeModel
        public void nodesWereInserted (TreeNode node, int[] childIndices) {
            super.nodesWereInserted (node, childIndices); 
            if (node instanceof VisualizerNode && isSortingActive ()) {
                sortChildren ((VisualizerNode)node);
            }
        }
        
        // overrided mothod from DefaultTreeModel
        public void nodesChanged(TreeNode node, int[] childIndices) {
            super.nodesChanged (node, childIndices);
            if (node != null && childIndices != null && isSortingActive ()) {
                sortChildren ((VisualizerNode)node);
            }
        }
        
        // overrided mothod from DefaultTreeModel
        public void setRoot(TreeNode root) {
            super.setRoot (root);
            if (root instanceof VisualizerNode && isSortingActive ()) {
                sortChildren ((VisualizerNode)root);
            }
        }

        
    }
        
    /* Cell renderer for sorting column header.
     */
    private class SortingHeaderRenderer extends DefaultTableCellRenderer {
        SortingHeaderRenderer() {}
        /** Overrides superclass method. */
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component comp = defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if ( comp instanceof JLabel ) {
                if ( column == 0 && treeColumnProperty.isSortingColumn() ) {
                    ((JLabel)comp).setIcon( getProperIcon( treeColumnProperty.isSortOrderDescending() ) );
                    ((JLabel)comp).setHorizontalTextPosition( SwingConstants.LEFT );
                    comp.setFont( comp.getFont().deriveFont( Font.BOLD ) );
                }
                else if ( column != 0 && tableModel.getVisibleSortingColumn() + 1 == column ) {
                    ((JLabel)comp).setIcon( getProperIcon( tableModel.isSortOrderDescending() ) );
                    ((JLabel)comp).setHorizontalTextPosition( SwingConstants.LEFT );
                    comp.setFont( comp.getFont().deriveFont( Font.BOLD ) );
                }
                else
                    ((JLabel)comp).setIcon( null );
            }
            return comp;
        }
        
        private ImageIcon getProperIcon(boolean descending) {
            if ( descending )
                return new ImageIcon ( org.openide.util.Utilities.loadImage( SORT_DESC_ICON ) );
            else
                return new ImageIcon ( org.openide.util.Utilities.loadImage( SORT_ASC_ICON ) );    
        }
    } // End of inner class SortingHeaderRenderer.
    
    private class TreeColumnProperty {
        TreeColumnProperty() {}
        private Property p = null;
        
        void setProperty (Property p ) {
            this.p = p;
        }
        
        boolean isComparable() {
            if ( p == null )
                return false;
            Object o = p.getValue( NodeTableModel.ATTR_COMPARABLE_COLUMN );
            if ( o != null && o instanceof Boolean )
                return ((Boolean)o).booleanValue();
            return false;
        }
        
        boolean isSortingColumn() {
            if ( p == null )
                return false;
            Object o = p.getValue( NodeTableModel.ATTR_SORTING_COLUMN );
            if ( o != null && o instanceof Boolean )
                return ((Boolean)o).booleanValue();
            return false;
        }
        
        void setSortingColumn( boolean sorting ) {
            if ( p == null )
                return;            
            p.setValue( NodeTableModel.ATTR_SORTING_COLUMN, sorting ? Boolean.TRUE : Boolean.FALSE );
        }
        
        boolean isSortOrderDescending() {
            if ( p == null )
                return false;
            Object o = p.getValue( NodeTableModel.ATTR_DESCENDING_ORDER );
            if ( o != null && o instanceof Boolean )
                return ((Boolean)o).booleanValue();
            return false;
        }
        
        void setSortOrderDescending(boolean descending) {
            if ( p == null )
                return;
            p.setValue( NodeTableModel.ATTR_DESCENDING_ORDER, descending ? Boolean.TRUE : Boolean.FALSE );
        }
    }
    
    /* For testing - use internal execution     
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Node n = //new org.netbeans.core.ModuleNode();
                    RepositoryNodeFactory.getDefault().repository(DataFilter.ALL);

                org.openide.explorer.ExplorerManager em = new org.openide.explorer.ExplorerManager();
                em.setRootContext(n);
                
                org.openide.explorer.ExplorerPanel ep = new org.openide.explorer.ExplorerPanel(em);
                ep.setLayout (new BorderLayout ());
                ep.setBorder(new EmptyBorder(20, 20, 20, 20));

                TreeTableView ttv = new TreeTableView();
                ttv.setRootVisible(false);
                ttv.setPopupAllowed(true);
                ttv.setDefaultActionAllowed(true);
                ttv.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
                ttv.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
                
                org.openide.nodes.PropertySupport.ReadOnly prop2
                    = new org.openide.nodes.PropertySupport.ReadOnly (
                            "name", // NOI18N
                            String.class,
                            "name",
                            "Name Tooltip"
                        ) {
                            public Object getValue () {
                                return null;
                            }

                        };
                //prop2.setValue( "InvisibleInTreeTableView", Boolean.TRUE );
                prop2.setValue( "SortingColumnTTV", Boolean.TRUE );
                prop2.setValue( "DescendingOrderTTV", Boolean.TRUE );
                prop2.setValue( "ComparableColumnTTV", Boolean.TRUE );
                        
                ttv.setProperties(
//                    n.getChildren().getNodes()[0].getPropertySets()[0].getProperties());
                    new Property[]{
                        new org.openide.nodes.PropertySupport.ReadWrite (
                            "hidden", // NOI18N
                            Boolean.TYPE,
                            "hidden",
                            "Hidden tooltip"
                        ) {
                            public Object getValue () {
                                return null;
                            }

                            public void setValue (Object o) {
                            }
                        },
                        prop2,
                        new org.openide.nodes.PropertySupport.ReadOnly (
                            "template", // NOI18N
                            Boolean.TYPE,
                            "template",
                            "Template Tooltip"
                        ) {
                            public Object getValue () {
                                return null;
                            }

                        }

                    }
                );
                ttv.setTreePreferredWidth(200);
                
                ttv.setTableColumnPreferredWidth(0, 60);
                ttv.setTableColumnPreferredWidth(1, 150);
                ttv.setTableColumnPreferredWidth(2, 100);
                
                ep.add("Center", ttv);
                ep.open();
            }
        });
    }
    */

}
