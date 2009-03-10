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
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.plaf.basic.BasicTableUI;
import java.util.EventObject;

import org.openide.nodes.Node.Property;
import org.openide.nodes.Node;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;
import org.openide.awt.MouseUtils;

/**
 * TreeTable implementation.
 *
 * @author Jan Rojcek
 */
class TreeTable extends JTable {
    /** A subclass of JTree. */
    private TreeTableCellRenderer tree;
    private NodeTableModel tableModel;
    private int treeColumnIndex = -1;

    /** Tree editor stuff. */
    private int lastRow = -1;
    private boolean canEdit;
    private boolean ignoreScrolling = false;

    /** Flag to ignore clearSelection() called from super.tableChanged(). */
    private boolean ignoreClearSelection = false;

    /** Position of tree renderer, used for horizontal scrolling. */
    private int positionX;
    
    /** If true, horizontal scrolling of tree column is enabled in TreeTableView */
    private boolean treeHScrollingEnabled = true;
    
    public TreeTable(NodeTreeModel treeModel, NodeTableModel tableModel) {
	super();

        this.tree = new TreeTableCellRenderer(treeModel);
        this.tableModel = new TreeTableModelAdapter(tree, tableModel);

        NodeRenderer rend = NodeRenderer.sharedInstance ();
        tree.setCellRenderer(rend);

	// Install a tableModel representing the visible rows in the tree. 
	setModel(this.tableModel);

	// Force the JTable and JTree to share their row selection models. 
	ListToTreeSelectionModelWrapper selectionWrapper = new 
	                        ListToTreeSelectionModelWrapper();
	tree.setSelectionModel(selectionWrapper);
	setSelectionModel(selectionWrapper.getListSelectionModel());
        getTableHeader().setReorderingAllowed(false);

	// Install the tree editor renderer and editor. 
	setDefaultRenderer(TreeTableModelAdapter.class, tree); 
	setDefaultEditor(TreeTableModelAdapter.class, new TreeTableCellEditor());

        // Install property renderer and editor.
        TableSheetCell tableCell = new TableSheetCell(this.tableModel);
        tableCell.setFlat(true);
        setDefaultRenderer(Property.class, tableCell);
        setDefaultEditor(Property.class, tableCell);
        getTableHeader().setDefaultRenderer(tableCell);

        /* fix of #23873, then removed - davidjon request
        getActionMap().put("selectNextColumnExtendSelection",
            getActionMap().get("selectNextColumn"));
        getActionMap().put("selectPreviousColumnExtendSelection",
            getActionMap().get("selectPreviousColumn"));
         */

        getActionMap().put("selectNextColumn", 
            new TreeTableAction(tree.getActionMap().get("selectChild"),
                                getActionMap().get("selectNextColumn")));
        getActionMap().put("selectPreviousColumn", 
            new TreeTableAction(tree.getActionMap().get("selectParent"),
                                getActionMap().get("selectPreviousColumn")));
        
        getAccessibleContext ().setAccessibleName (
            NbBundle.getBundle (TreeTable.class).getString ("ACSN_TreeTable"));
        getAccessibleContext ().setAccessibleDescription (
            NbBundle.getBundle (TreeTable.class).getString ("ACSD_TreeTable"));
    }
    
    /*
     * Overridden to message super and forward the method to the tree.
     */
    public void updateUI() {
	super.updateUI();
	if(tree != null) {
	    tree.updateUI();
	}
	// Use the tree's default foreground and background colors in the
	// table. 
        LookAndFeel.installColorsAndFont(this, "Tree.background",
                                         "Tree.foreground", "Tree.font");
        setUI(new TreeTableUI());
        needCalcRowHeight = true;
    }

    /* Workaround for BasicTableUI anomaly. Make sure the UI never tries to 
     * paint the editor. The UI currently uses different techniques to 
     * paint the renderers and editors and overriding setBounds() below 
     * is not the right thing to do for an editor. Returning -1 for the 
     * editing row in this case, ensures the editor is never painted. 
     */
    public int getEditingRow() {
        return (getColumnClass(editingColumn) == TreeTableModelAdapter.class) ? -1 :
	        editingRow;  
    }
    
    private boolean needCalcRowHeight = true;
    
    public void paint (Graphics g) {
        if (needCalcRowHeight) {
            calcRowHeight(g);
        }
        super.paint(g);
    }
    
    /** Calculate the height of rows based on the current font.  This is
     *  done when the first paint occurs, to ensure that a valid Graphics 
     *  object is available.  
     *  @since 1.25   */
    private void calcRowHeight(Graphics g) {
        Font f = getFont();
        FontMetrics fm = g.getFontMetrics(f);
        int rowHeight = fm.getHeight();
        needCalcRowHeight = false;
        setRowHeight (rowHeight);
    }    

    /*
     * Overridden to pass the new rowHeight to the tree.
     */
    public void setRowHeight(int rowHeight) { 
        super.setRowHeight(rowHeight); 
	if (tree != null && tree.getRowHeight() != rowHeight) {
            tree.setRowHeight(getRowHeight()); 
	}
    }

    /**
     * Returns the tree that is being shared between the model.
     */
    JTree getTree() {
	return tree;
    }

    /**
     * Returns table column index of the column displaying the tree.
     */
    int getTreeColumnIndex() {
        return treeColumnIndex;
    }
    
    /**
     * Sets tree column index and fires property change.
     */
    void setTreeColumnIndex(int index) {
        if (treeColumnIndex == index)
            return;
        
        int old = treeColumnIndex;
        treeColumnIndex = index;
        firePropertyChange("treeColumnIndex", old, treeColumnIndex);
    }

    /* Overriden to do not clear a selection upon model changes.
     */
    public void clearSelection() {
        if (!ignoreClearSelection) {
            super.clearSelection();
        }
    }

    /* Updates tree column name and sets ignoreClearSelection flag.
     */
    public void tableChanged(TableModelEvent e) {
        // update tree column name
        int modelColumn = getTreeColumnIndex();
        if (e.getFirstRow() <= 0 && modelColumn != -1 && getColumnCount() > 0) {
            String columnName = getModel().getColumnName(modelColumn);
            TableColumn aColumn = getColumnModel().getColumn(modelColumn);
            aColumn.setHeaderValue(columnName);
        }
        
        ignoreClearSelection = true;
        try {
            super.tableChanged(e);
        } finally {
            ignoreClearSelection = false;
        }
    }

    /* Performs horizontal scrolling of the tree when editing is started.
     */
    public boolean editCellAt(int row, int column, EventObject e) {
        canEdit = (lastRow == row);
        
        boolean ret = super.editCellAt(row, column, e);
        
        if (ret && column == getTreeColumnIndex()) {
            ignoreScrolling = true;
            tree.scrollRectToVisible(tree.getRowBounds(row));
            ignoreScrolling = false;
        }
        return ret;
    }
    
    /* 
     */
    public void valueChanged(ListSelectionEvent e) {
        if (getSelectedRowCount() == 1)
            lastRow = getSelectedRow();
        else
            lastRow = -1;
        super.valueChanged(e);
    }
    

    /* Updates tree column index
     */
    public void columnAdded(TableColumnModelEvent e) {
        super.columnAdded(e);
        updateTreeColumnIndex();
    }

    /* Updates tree column index
     */
    public void columnRemoved(TableColumnModelEvent e) {
        super.columnRemoved(e);
        updateTreeColumnIndex();
    }

    /* Updates tree column index
     */
    public void columnMoved(TableColumnModelEvent e) {
        super.columnMoved(e);
        updateTreeColumnIndex();
        int from = e.getFromIndex();
        int to = e.getToIndex();
        if ( from != to )
            firePropertyChange( "column_moved", from, to );  // NOI18N
    }
    
    /* Updates tree column index
     */
    private void updateTreeColumnIndex() {
        for (int i = getColumnCount() - 1; i >= 0; i--) {
            if (getColumnClass(i) == TreeTableModelAdapter.class) {
                setTreeColumnIndex(i);
                return;
            }
        }
        setTreeColumnIndex(-1);
    }

    /** Returns x coordinate of tree renderer.
     */
    public int getPositionX() {
        return positionX;
    }

    /** Sets x position.
     */
    public void setPositionX(int x) {
        if (x == positionX || !treeHScrollingEnabled)
            return;

        int old = positionX;
        positionX = x;
        
        firePropertyChange("positionX", old, x);
        
        if (isEditing() && getEditingColumn() == getTreeColumnIndex()) {
            CellEditor editor = getCellEditor();
            if (ignoreScrolling && editor instanceof TreeTableCellEditor) {
                ((TreeTableCellEditor)editor).revalidateTextField();
            } else {
                removeEditor();
            }
        }
        repaint();
    }
    
    /** Enables horizontal scrolling of tree column */
    void setTreeHScrollingEnabled(boolean enabled) {
        treeHScrollingEnabled = enabled;
    }

    /**
     * A TreeCellRenderer that displays a JTree.
     */
    class TreeTableCellRenderer extends JTree implements TableCellRenderer {
	/** Last table/tree row asked to renderer. */
	protected int visibleRow;

        /* Last width of the tree.
         */
        private int oldWidth;
        
	public TreeTableCellRenderer(TreeModel model) {
	    super(model); 
            setRowHeight(getRowHeight()); 
            setToggleClickCount(0);
            putClientProperty("JTree.lineStyle", "None"); // NOI18N
	}

	/**
	 * Sets the row height of the tree, and forwards the row height to
	 * the table.
	 */
	public void setRowHeight(int rowHeight) { 
	    if (rowHeight > 0) {
		super.setRowHeight(rowHeight); 
		if (TreeTable.this != null &&
		    TreeTable.this.getRowHeight() != rowHeight) {
		    TreeTable.this.setRowHeight(getRowHeight()); 
		}
	    }
	}

	/**
	 * This is overridden to set the height to match that of the JTable.
	 */
	public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, 0, w + positionX, TreeTable.this.getHeight());
	}

        /* Fire width property change so that we can revalidate horizontal scrollbar in TreeTableView.
         */
        public void reshape(int x, int y, int w, int h) {
            super.reshape(x, y, w, h);
            int newWidth = getPreferredSize().width;
            if (oldWidth != newWidth) {
                firePropertyChange("width", oldWidth, newWidth);
                oldWidth = newWidth;
            }
        }
        
	/**
	 * Sublcassed to translate the graphics such that the last visible
	 * row will be drawn at 0,0.
	 */
	public void paint(Graphics g) {
            g.translate(-positionX, -visibleRow * getRowHeight());
	    super.paint(g);
	}
        
        public Rectangle getVisibleRect() {
            Rectangle visibleRect = TreeTable.this.getVisibleRect();
            visibleRect.x = positionX;
            visibleRect.width = TreeTable.this.getColumnModel().getColumn(getTreeColumnIndex()).getWidth();
            return visibleRect;
        }
        
        /* Overriden to use this call for moving tree renderer.
         */
        public void scrollRectToVisible(Rectangle aRect) {
            Rectangle rect = getVisibleRect();
            rect.y = aRect.y;
            rect.height = aRect.height;

            TreeTable.this.scrollRectToVisible(rect);
            int x = rect.x;
            if (aRect.width > rect.width) {
                x = aRect.x;
            } else if (aRect.x < rect.x) {
                x = aRect.x;
            } else if (aRect.x + aRect.width > rect.x + rect.width) {
                x = aRect.x + aRect.width - rect.width;
            }
            TreeTable.this.setPositionX(x);
        }
        
        public String getToolTipText(MouseEvent event) {
    	    if(event != null) {
		Point p = event.getPoint();
                p.translate(positionX, visibleRow * getRowHeight());
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

        /* To make the tree think that it has focus when rendering focused cell.
         * It will paint focus border around node name.
         */
        boolean hasFocus;
        //public boolean hasFocus() {
        //    return hasFocus;
        //}
        
	/**
	 * TreeCellRenderer method. Overridden to update the visible row.
	 */
	public Component getTableCellRendererComponent(JTable table,
						       Object value,
						       boolean isSelected,
						       boolean hasFocus,
						       int row, int column) {
	    if(isSelected)
		setBackground(table.getSelectionBackground());
	    else
		setBackground(table.getBackground());

            this.hasFocus = hasFocus;
	    visibleRow = row;
            return this;
	}
        
        protected TreeModelListener createTreeModelListener() {
            return new JTree.TreeModelHandler() {
                public void treeNodesRemoved(TreeModelEvent e) {
                    if (tree.getSelectionCount () == 0) {
                        TreePath path = TreeView.findSiblingTreePath (e.getTreePath (), e.getChildIndices ());
                        if (path == null) return;
                        if (path.getPathCount () > 0 || tree.isRootVisible ()) {
                            tree.setSelectionPath (path);
                        }
                    }
                }
            };
        }
        
    }

    /**
     * TreeTableCellEditor implementation.
     */
    class TreeTableCellEditor extends DefaultCellEditor implements TreeSelectionListener, ActionListener, FocusListener, CellEditorListener {
        
        /** Used in editing. Indicates x position to place editingComponent. */
        protected transient int offset;

        /** Used before starting the editing session. */
        protected transient Timer timer;
        
	public TreeTableCellEditor() {
	    super(new TreeTableTextField());

            tree.addTreeSelectionListener(this);
            addCellEditorListener(this);
            super.getComponent().addFocusListener(this);
	}

	/**
	 * Overridden to determine an offset that tree would place the
	 * editor at. The offset is determined from the
	 * <code>getRowBounds</code> JTree method, and additionally
	 * from the icon DefaultTreeCellRenderer will use.
	 * <p>The offset is then set on the TreeTableTextField component
	 * created in the constructor, and returned.
	 */
	public Component getTableCellEditorComponent(JTable table,
						     Object value,
						     boolean isSelected,
						     int r, int c) {
	    Component component = super.getTableCellEditorComponent
		(table, value, isSelected, r, c);

            determineOffset(value, isSelected, r);
	    ((TreeTableTextField)getComponent()).offset = offset;

	    return component;
	}

	/**
	 * This is overridden to forward the event to the tree and start editor timer. 
	 */
	public boolean isCellEditable(EventObject e) {

            if (lastRow != -1) {
                org.openide.nodes.Node n = Visualizer.findNode (tree.getPathForRow(lastRow).getLastPathComponent());
                if (n == null || !n.canRename ()) {
                    //return false;
                    canEdit = false;
                }
            }
            
            if (canEdit && e != null && ( e.getSource() instanceof Timer ))
                return true;
            
            if (canEdit && shouldStartEditingTimer(e)) {
                startEditingTimer();
            } else if (shouldStopEditingTimer(e)) {
                timer.stop();
            }

	    if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent)e;
                int column = getTreeColumnIndex();

                if ( MouseUtils.isLeftMouseButton(me) && me.getClickCount() == 2 ) {
                    TreePath path = tree.getPathForRow(TreeTable.this.rowAtPoint(me.getPoint()));
                    Rectangle r = tree.getPathBounds(path);
                    if ( me.getX() < r.x - positionX || me.getX() > r.x - positionX + r.width ) {
                        me.translatePoint( r.x - me.getX(), 0 );
                    }
                }
                
                MouseEvent newME = new MouseEvent
                      (TreeTable.this.tree, me.getID(),
                       me.getWhen(), me.getModifiers(),
                       me.getX() - getCellRect(0, column, true).x + positionX,
                       me.getY(), me.getClickCount(),
                       me.isPopupTrigger());
                TreeTable.this.tree.dispatchEvent(newME);
	    }
	    return false;

	}
        
        /* Stop timer when selection has been changed.
         */
        public void valueChanged(TreeSelectionEvent e) {
            if (timer != null) {
                timer.stop();
            }
        }
        
        /* Timer performer.
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (lastRow != -1) {
                editCellAt(lastRow, getTreeColumnIndex(), new EventObject( timer ));
            }
        }
        
        /* Start editing timer only on certain conditions.
         */
        private boolean shouldStartEditingTimer(EventObject event) {
            if ((event instanceof MouseEvent) &&
                SwingUtilities.isLeftMouseButton((MouseEvent)event)) {
                MouseEvent        me = (MouseEvent)event;
                
                return (me.getID() == me.MOUSE_PRESSED &&
                        me.getClickCount() == 1 &&
                        inHitRegion(me));
            }
            return false;
        }
        
        /* Stop editing timer only on certain conditions.
         */
        private boolean shouldStopEditingTimer(EventObject event) {
            if (timer == null)
                return false;

            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent)event;
                return (!SwingUtilities.isLeftMouseButton(me) ||
                        me.getClickCount() > 1);
            }
            
            return false;
        }

        /**
         * Starts the editing timer.
         */
        private void startEditingTimer() {
            if(timer == null) {
                timer = new Timer(1200, this);
                timer.setRepeats(false);
            }
            timer.start();
        }

        /* Does a click go into node's label?
         */
        private boolean inHitRegion(MouseEvent me) {
            determineOffset(me);
            if (me.getX() <= offset) {
                return false;
            }
            return true;
        }

        /* Determines offset of node's label from left edge of the table.
         */
        private void determineOffset(MouseEvent me) {
            int row = TreeTable.this.rowAtPoint(me.getPoint());
            if (row == -1) {
                offset = 0;
                return;
            }
            determineOffset(tree.getPathForRow(row).getLastPathComponent(), 
                            TreeTable.this.isRowSelected(row), row);
        }
        
        /* Determines offset of node's label from left edge of the table.
         */
        private void determineOffset(Object value, boolean isSelected, int row) {
            JTree t = getTree();
	    boolean rv = t.isRootVisible();
	    int offsetRow = row;
            if ( !rv && row > 0 )
                offsetRow--;
            Rectangle bounds = t.getRowBounds(offsetRow);
            offset = bounds.x;
            
	    TreeCellRenderer tcr = t.getCellRenderer();
            Object node = t.getPathForRow(offsetRow).getLastPathComponent();
            Component comp = tcr.getTreeCellRendererComponent(
                                t, node, isSelected, t.isExpanded(offsetRow),
                                t.getModel().isLeaf(node), offsetRow, false);
            if (comp instanceof JLabel) {
                Icon icon = ((JLabel)comp).getIcon();
                if (icon != null) {
                    offset += ((JLabel)comp).getIconTextGap() + icon.getIconWidth();
                }
            }
            offset -= positionX;
        }
        
        /* Revalidates text field upon change of x position of renderer
         */
        private void revalidateTextField() {
            int row = TreeTable.this.editingRow;
            if (row == -1) {
                offset = 0;
                return;
            }
            determineOffset(tree.getPathForRow(row).getLastPathComponent(), 
                            TreeTable.this.isRowSelected(row), row);
	    ((TreeTableTextField)super.getComponent()).offset = offset;
            getComponent().setBounds(TreeTable.this.getCellRect(row, getTreeColumnIndex(), false));
        }

        // Focus listener
        
        /* Cancel editing when text field loses focus
         */
        public void focusLost (java.awt.event.FocusEvent evt) {
            /* to allow Escape functionality
            if (!stopCellEditing())
              cancelCellEditing();
             */
        }

        /* Select a text in text field when it gets focus.
         */
        public void focusGained (java.awt.event.FocusEvent evt) {
            ((TreeTableTextField)super.getComponent()).selectAll();
        }
        
        // Cell editor listener - copied from TreeViewCellEditor
        
        /** Implements <code>CellEditorListener</code> interface method. */
        public void editingStopped(ChangeEvent e) {
            TreePath lastP = tree.getPathForRow(lastRow);
            if (lastP != null) {
                Node n = Visualizer.findNode (lastP.getLastPathComponent());
                if (n != null && n.canRename ()) {
                    String newStr = (String) getCellEditorValue();
                    try {
                        // bugfix #21589 don't update name if there is not any change
                        if (!n.getName ().equals (newStr)) {
                            n.setName (newStr);
                        }
                    }
                    catch (IllegalArgumentException exc) {
                        boolean needToAnnotate = true;
                        ErrorManager em = ErrorManager.getDefault ();
                        ErrorManager.Annotation[] ann = em.findAnnotations(exc);

                        // determine if "new annotation" of this exception is needed
                        if (ann!=null && ann.length>0) {
                            for (int i=0; i<ann.length; i++) {
                                String glm = ann[i].getLocalizedMessage();
                                if (glm!=null && !glm.equals("")) { // NOI18N
                                    needToAnnotate = false;
                                }
                            }
                        }


                        // annotate new localized message only if there is no localized message yet
                        if (needToAnnotate) {
                            String msg = NbBundle.getMessage(TreeViewCellEditor.class, "RenameFailed", n.getName (), newStr);
                            em.annotate(exc, msg);
                        }

                        em.notify(exc);
                    }
                }
            }
        }

        /** Implements <code>CellEditorListener</code> interface method. */
        public void editingCanceled(ChangeEvent e) {
        }

    }

    /**
     * Component used by TreeTableCellEditor. The only thing this does
     * is to override the <code>reshape</code> method, and to ALWAYS
     * make the x location be <code>offset</code>.
     */
    static class TreeTableTextField extends JTextField {
	public int offset;

	public void reshape(int x, int y, int w, int h) {
	    int newX = Math.max(x, offset);
	    super.reshape(newX, y, w - (newX - x), h);
	}
        
        public void addNotify() {
            super.addNotify();
            requestFocus();
        }
    }

    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
     * to listen for changes in the ListSelectionModel it maintains. Once
     * a change in the ListSelectionModel happens, the paths are updated
     * in the DefaultTreeSelectionModel.
     */
    class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel { 
	/** Set to true when we are updating the ListSelectionModel. */
	protected boolean         updatingListSelectionModel;

	public ListToTreeSelectionModelWrapper() {
	    super();
	    getListSelectionModel().addListSelectionListener
	                            (createListSelectionListener());
	}

	/**
	 * Returns the list selection model. ListToTreeSelectionModelWrapper
	 * listens for changes to this model and updates the selected paths
	 * accordingly.
	 */
	ListSelectionModel getListSelectionModel() {
	    return listSelectionModel; 
	}

	/**
	 * This is overridden to set <code>updatingListSelectionModel</code>
	 * and message super. This is the only place DefaultTreeSelectionModel
	 * alters the ListSelectionModel.
	 */
	public void resetRowSelection() {
	    if(!updatingListSelectionModel) {
		updatingListSelectionModel = true;
		try {
		    super.resetRowSelection();
		}
		finally {
		    updatingListSelectionModel = false;
		}
	    }
	    // Notice how we don't message super if
	    // updatingListSelectionModel is true. If
	    // updatingListSelectionModel is true, it implies the
	    // ListSelectionModel has already been updated and the
	    // paths are the only thing that needs to be updated.
	}

	/**
	 * Creates and returns an instance of ListSelectionHandler.
	 */
	protected ListSelectionListener createListSelectionListener() {
	    return new ListSelectionHandler();
	}

	/**
	 * If <code>updatingListSelectionModel</code> is false, this will
	 * reset the selected paths from the selected rows in the list
	 * selection model.
	 */
	protected void updateSelectedPathsFromSelectedRows() {
	    if(!updatingListSelectionModel) {
		updatingListSelectionModel = true;
		try {
		    // This is way expensive, ListSelectionModel needs an
		    // enumerator for iterating.
		    int        min = listSelectionModel.getMinSelectionIndex();
		    int        max = listSelectionModel.getMaxSelectionIndex();

		    this.clearSelection();
		    if(min != -1 && max != -1) {
			for(int counter = min; counter <= max; counter++) {
			    if(listSelectionModel.isSelectedIndex(counter)) {
				TreePath     selPath = tree.getPathForRow
				                            (counter);

				if(selPath != null) {
				    addSelectionPath(selPath);
				}
			    }
			}
		    }
		}
		finally {
		    updatingListSelectionModel = false;
		}
	    }
	}

	/**
	 * Class responsible for calling updateSelectedPathsFromSelectedRows
	 * when the selection of the list changes.
	 */
	class ListSelectionHandler implements ListSelectionListener {
	    public void valueChanged(ListSelectionEvent e) {
		updateSelectedPathsFromSelectedRows();
	    }
	}
        
    }

    /* This is overriden to handle mouse events especially. E.g. do not change selection 
     * when it was clicked on tree's expand/collapse toggles.
     */
    class TreeTableUI extends BasicTableUI {
        /**
         * Creates the mouse listener for the JTable.
         */
        protected MouseInputListener createMouseInputListener() {
            return new TreeTableMouseInputHandler();
        }

        public class TreeTableMouseInputHandler extends MouseInputHandler {

            // Component recieving mouse events during editing. May not be editorComponent.
            private Component dispatchComponent;

            //  The Table's mouse listener methods.

            public void mouseClicked(MouseEvent e) {
                processMouseEvent(e);
            }

            public void mousePressed(MouseEvent e) {
                processMouseEvent(e);
            }

            public void mouseReleased(MouseEvent e) {
                if (shouldIgnore(e)) {
                    return;
                }
                
                repostEvent(e); 
                dispatchComponent = null;
                setValueIsAdjusting(false);
                
                if (!TreeTable.this.isEditing())
                    processMouseEvent(e);
            }

            public void mouseDragged(MouseEvent e) {
                return;
            }

            private void setDispatchComponent(MouseEvent e) { 
                Component editorComponent = table.getEditorComponent();
                Point p = e.getPoint();
                Point p2 = SwingUtilities.convertPoint(table, p, editorComponent);
                dispatchComponent = SwingUtilities.getDeepestComponentAt(editorComponent, 
                                                                     p2.x, p2.y);
            }

            private boolean repostEvent(MouseEvent e) { 
                if (dispatchComponent == null) {
                    return false; 
                }
                MouseEvent e2 = SwingUtilities.convertMouseEvent(table, e, dispatchComponent);
                dispatchComponent.dispatchEvent(e2); 
                return true; 
            }

            private void setValueIsAdjusting(boolean flag) {
                table.getSelectionModel().setValueIsAdjusting(flag); 
                table.getColumnModel().getSelectionModel().setValueIsAdjusting(flag); 
            }

            private boolean shouldIgnore(MouseEvent e) { 
                return !table.isEnabled(); 
            }
            
            private boolean isTreeColumn(int column) {
                return TreeTable.this.getColumnClass(column) == TreeTableModelAdapter.class;
            }

            /** Forwards mouse events to a renderer (tree).
             */
            private void processMouseEvent(MouseEvent e) {
                if (shouldIgnore(e)) {
                    return;
                }

                Point p = e.getPoint();
                int row = table.rowAtPoint(p);
                int column = table.columnAtPoint(p);
                // The autoscroller can generate drag events outside the Table's range. 
                if ((column == -1) || (row == -1)) {
                    return;
                }

                // for automatic jemmy testing purposes
                if ( getEditingColumn() == column && getEditingRow() == row ) {
                    return;
                }

                boolean changeSelection = true;
                if (isTreeColumn(column)) {
                    TreePath path = tree.getPathForRow(TreeTable.this.rowAtPoint(e.getPoint()));
                    Rectangle r = tree.getPathBounds(path);
                    if (e.getX() >= r.x - positionX && e.getX() <= r.x - positionX + r.width) {
                        changeSelection = false;
                    }
                }
                
                if ( table.getSelectionModel().isSelectedIndex( row ) &&
                        e.isPopupTrigger() )
                    return;

                if (table.editCellAt(row, column, e)) {
                    setDispatchComponent(e); 
                    repostEvent(e); 
                } 
                else { 
                    table.requestFocus();
                }

                CellEditor editor = table.getCellEditor(); 
                if (changeSelection && (editor == null || editor.shouldSelectCell(e))) { 
                    setValueIsAdjusting(true);
                    table.changeSelection(row, column, e.isControlDown(), e.isShiftDown());  
                }
            }
        }
    }

    /* When selected column is tree column then call tree's action otherwise call table's.
     */
    class TreeTableAction extends AbstractAction {
        
        Action treeAction;
        Action tableAction;
        
        TreeTableAction(Action treeAction, Action tableAction) {
            this.treeAction = treeAction;
            this.tableAction = tableAction;
        }
        
        public void actionPerformed(ActionEvent e) {
            if (TreeTable.this.getSelectedColumn() == getTreeColumnIndex()) {
                treeAction.actionPerformed(e);
            }
        }
        
    }
    
}
