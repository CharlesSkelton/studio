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

import java.beans.*;
import java.util.EventObject;
import java.text.MessageFormat;
import java.lang.reflect.InvocationTargetException;
import java.awt.Graphics;
import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.tree.TreeNode;

import org.openide.ErrorManager;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.explorer.propertysheet.*;

/**
 * TableCellEditor/Renderer implementation. Component returned is the PropertyPanel
 *
 * @author Jan Rojcek
 */
class TableSheetCell extends AbstractCellEditor implements TableModelListener, PropertyChangeListener, TableCellEditor, TableCellRenderer {

    /* Table sheet cell works only with NodeTableModel */
    private NodeTableModel tableModel;
    /* Determines how to paint renderer */
    private Boolean flat;

    public TableSheetCell(NodeTableModel tableModel) {
        this.tableModel = tableModel;
        setFlat(false);
    }

    /**
     * Set how to paint renderer.
     * @param f <code>true</code> means flat, <code>false</code> means with button border
     */
    public void setFlat(boolean f) {
        nullPanelBorder =
            f ? (Border)new EmptyBorder(1, 1, 1, 1)
              : (Border)new CompoundBorder(
                    new MatteBorder(0, 0, 1, 1, UIManager.getColor("controlDkShadow")),
                    new MatteBorder(1, 1, 0, 0, UIManager.getColor("controlLtHighlight"))
                );
        nullPanelFocusBorder = 
            new CompoundBorder(
                new CompoundBorder(nullPanelBorder, new EmptyBorder(1, 1, 1, 1)),
                new MatteBorder(1, 1, 1, 1, UIManager.getColor("Button.focus")) // NOI18N
            );
        flat = f ? Boolean.TRUE : Boolean.FALSE;
    }

    //
    // Editor
    //
    
    /** Property model used in cell editor property panel */
    private WrapperPropertyModel propModel;
    /** Actually edited node (its property) */
    private Node node;
    /** Edited property */
    private Property prop;

    /** Returns <code>null<code>.
     * @return <code>null</code>
     */        
    public Object getCellEditorValue() { return null; }

    /** Returns editor of property.
     * @param table
     * @param value
     * @param isSelected
     * @param r row
     * @param c column
     * @return <code>PropertyPanel</code>
     */
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int r, int c) {
        prop = (Property)value;
        node = tableModel.nodeForRow(r);
        propModel = new WrapperPropertyModel(node, prop);
        node.addPropertyChangeListener(this);
        tableModel.addTableModelListener(this);
        // create property panel
        PropertyPanel propPanel =  new FocusHackedPropertyPanel(propModel, 
                                   prop.canWrite() ? 0 //PropertyPanel.PREF_INPUT_STATE
                                                   : PropertyPanel.PREF_READ_ONLY);
        propPanel.putClientProperty("flat", flat); // NOI18N
        propPanel.setBackground(table.getSelectionBackground());
        return propPanel;
    }

    /** Cell should not be selected
     * @param ev event
     * @return <code>false</code>
     */
    public boolean shouldSelectCell(EventObject ev) {
        return true;
    }

    /** Return true.
     * @param e event
     * @return <code>true</code>
     */
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    /** Forwards node property change to property model
     * @param evt event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        Mutex.EVENT.readAccess(new Runnable() {
            public void run() {
                if (propModel != null) {
                    propModel.firePropertyChange();
                }
            }
        });
    }

    /**
     * Detaches listeners.
     * Calls <code>fireEditingStopped</code> and returns true.
     * @return true
     */
    public boolean stopCellEditing() {
        if (prop != null)
            detachEditor();
        return super.stopCellEditing();
    }

    /**
     * Detaches listeners.
     * Calls <code>fireEditingCanceled</code>.
     */
    public void  cancelCellEditing() { 
        if (prop != null)
            detachEditor();
        super.cancelCellEditing();
    }

    /** Table has changed. If underlied property was switched then cancel editing.
     * @param e event
     */
    public void tableChanged(TableModelEvent e) {
        cancelCellEditing();
    }

    /** Removes listeners and frees resources.
     */
    private void detachEditor() {
        node.removePropertyChangeListener(this);
        tableModel.removeTableModelListener(this);
        node = null;
        prop = null;
        propModel = null;
    }

    //
    // Renderer
    //

    /** Default header renderer */
    private TableCellRenderer headerRenderer = (new JTableHeader()).getDefaultRenderer();

    /** Null panel is used if cell value is null */
    private NullPanel nullPanel;
    private Border nullPanelBorder;
    private Border nullPanelFocusBorder;

    /** Two-tier cache for property panels 
     * Map<TreeNode, WeakHashMap<Node.Property, Reference<FocusedPropertyPanel>> */
    private Map panelCache = new WeakHashMap(); // weak! #31275

    /** Getter for actual cell renderer.
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return <code>PropertyPanel</code>
     */
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row, int column) {

        // Header renderer
        if (row == -1) {
            Component comp = headerRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            if (comp instanceof JComponent) {
                String tip = column > 0 ? tableModel.propertyForColumn(column).getShortDescription()
                                        : table.getColumnName( 0 );
                ((JComponent) comp).setToolTipText(tip);
            }
            return comp;
        }

        Property prop = (Property)value;
        Node node = tableModel.nodeForRow(row);

        if (prop != null) {
            FocusedPropertyPanel propPanel = obtainPanel(node, prop);
            propPanel.setFocused(hasFocus);
            propPanel.setToolTipText(prop.getShortDescription());
            propPanel.setOpaque(true);
            if (isSelected)
                propPanel.setBackground(table.getSelectionBackground());
            else
                propPanel.setBackground(table.getBackground());
            return propPanel;
        }

        if (nullPanel == null) {
            nullPanel = new NullPanel(node);
            nullPanel.setOpaque(true);
        } else {
            nullPanel.setNode(node);
        }

        if (isSelected)
            nullPanel.setBackground(table.getSelectionBackground());
        else
            nullPanel.setBackground(table.getBackground());


        if (hasFocus) {
            nullPanel.setBorder(nullPanelFocusBorder);
        } else {
            nullPanel.setBorder(nullPanelBorder);
        }

        return nullPanel;
    }

    private FocusedPropertyPanel obtainPanel (Node node, Property prop) {
        FocusedPropertyPanel propPanel = null;
        TreeNode visualizer = Visualizer.findVisualizer(node);
        Map innerCache = (Map)panelCache.get(visualizer);
        if (innerCache == null) {
            // outer cache miss
            innerCache = new WeakHashMap();
            propPanel = createPropPanel(node, prop);
            propPanel.putClientProperty("flat", flat); // NOI18N
            innerCache.put(prop, propPanel);
            panelCache.put(visualizer, innerCache);
        } else {
            propPanel = (FocusedPropertyPanel)innerCache.get(prop);
            if (propPanel == null) {
                // inner cache miss
                propPanel = createPropPanel(node, prop);
                propPanel.putClientProperty("flat", flat); // NOI18N
                innerCache.put(prop, propPanel);
            } else {
                // cache hit
                ((WrapperPropertyModel)propPanel.getModel()).firePropertyChange();
            }
        }
        return propPanel;
    }
    
    private static FocusedPropertyPanel createPropPanel (Node node, Property prop) {
        WrapperPropertyModel propModel = new WrapperPropertyModel(node, prop);
        // Now it doesn't differentiate between read-only and read-write property
        FocusedPropertyPanel propPanel = new FocusedPropertyPanel(propModel, 0);
                                      //prop.canWrite() ? 0 : PropertyPanel.PREF_READ_ONLY);
        return propPanel;
    }

    /** Wraps Node.Property with PropertyModel.
     */
    private static class WrapperPropertyModel implements ExPropertyModel {
        
        PropertyChangeSupport support = new PropertyChangeSupport(this);
        /** Wrapped property */
        Property prop;
        /** Node the property belongs to */
        Node node;
        
        /** Creates new wrapped property
         * @param prop wrapped property
         */
        public WrapperPropertyModel(Node node, Property prop) {
            this.prop = prop;
            this.node = node;
        }
        
        /** Getter for current value of a property.
         */
        public Object getValue() throws InvocationTargetException {
            try {
                return prop.getValue();
            }
            catch (IllegalAccessException e) {
	        ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, e);
                throw new InvocationTargetException(e);
            }
        }
        
        /** Setter for a value of a property.
         * @param v the value
         * @exeception InvocationTargetException
         */
        public void setValue(Object v) throws InvocationTargetException {
            if (!prop.canWrite()) return;
            try {
               prop.setValue(v);
            }
            catch (IllegalAccessException e) {
	        ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, e);
                throw new InvocationTargetException(e);
            }
        }
        
        /** The class of the property.
         */
        public Class getPropertyType() {
            return prop.getValueType();
        }
        
        /** The class of the property editor or <CODE>null</CODE>
         * if default property editor should be used.
         */
        public Class getPropertyEditorClass() {
            return prop.getPropertyEditor().getClass();
        }
        
        /** Add listener to change of the value.
         */
        public void addPropertyChangeListener(PropertyChangeListener l) {
            support.addPropertyChangeListener(l);
        }
        
        /** Remove listener to change of the value.
         */
        public void removePropertyChangeListener(PropertyChangeListener l) {
            support.removePropertyChangeListener(l);
        }
        
        public void firePropertyChange() {
            support.firePropertyChange(PROP_VALUE, null, null);
        }
        
        /**
         * Returns an array of beans/nodes that this property belongs
         * to.
         */
        public Object[] getBeans() {
            return new Object[] {node};
        }
        
        /**
         * Returns descriptor describing the property.
         */
        public FeatureDescriptor getFeatureDescriptor() {
            return prop;
        }
        
    }

    private static class NullPanel extends JPanel {
        private WeakReference weakNode;
        
        NullPanel(Node node) {
            this.weakNode = new WeakReference(node);
        }
        
        void setNode(Node node) {
            this.weakNode = new WeakReference(node);
        }

        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleNullPanel();
            }
            return accessibleContext;
        }

        private class AccessibleNullPanel extends AccessibleJPanel {
            AccessibleNullPanel() {}
            public String getAccessibleName() {
                String name = super.getAccessibleName();

                if (name == null) {
                    name = getString("ACS_NullPanel");
                }
                return name;
            }

            public String getAccessibleDescription() {
                String description = super.getAccessibleDescription();

                if (description == null) {
                    Node node = (Node)weakNode.get();
                    if (node != null) {
                        description = MessageFormat.format(
                            getString("ACSD_NullPanel"),
                            new Object[] {
                                node.getDisplayName()
                            }
                        );
                    }
                }
                return description;
            }
        }
    }

    /** Table cell renderer component. Paints focus border on property panel. */
    private static class FocusedPropertyPanel extends PropertyPanel {
        
        boolean focused;
        
        public FocusedPropertyPanel(PropertyModel model, int preferences) {
            super(model, preferences);
        }
        
        public void setFocused(boolean focused) {
            this.focused = focused;
        }
        
        public void paint(Graphics g) {
            super.paint(g);

            if (focused) {
                g.setColor(UIManager.getColor("Button.focus")); // NOI18N
                g.drawRect(2, 1, getWidth() - 5, getHeight() - 4);
            }
        }
        
        ////////////////// Accessibility support ///////////////////////////////

        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleFocusedPropertyPanel();
            }
            return accessibleContext;
        }

        private class AccessibleFocusedPropertyPanel extends AccessibleJComponent {
            AccessibleFocusedPropertyPanel() {}
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.PANEL;
            }

            public String getAccessibleName() {
                FeatureDescriptor fd = ((ExPropertyModel)getModel()).getFeatureDescriptor();
                PropertyEditor editor = getPropertyEditor();
                
                return MessageFormat.format(
                    getString("ACS_PropertyPanelRenderer"),
                    new Object[] {
                        fd.getDisplayName(),
                        (editor == null) ? getString("CTL_No_value") : editor.getAsText()
                    }
                );
            }

            public String getAccessibleDescription() {
                FeatureDescriptor fd = ((ExPropertyModel)getModel()).getFeatureDescriptor();
                Node node = (Node)((ExPropertyModel)getModel()).getBeans()[0];
                Class clazz = getModel().getPropertyType();
                return MessageFormat.format(
                    getString("ACSD_PropertyPanelRenderer"),
                    new Object[] {
                        fd.getShortDescription(),
                        clazz == null ? getString("CTL_No_type") : clazz.getName(), 
                        node.getDisplayName()
                    }
                );
            }
        }
    }
    
    private static String getString(String key) {
        return NbBundle.getBundle(TableSheetCell.class).getString(key);
    }

    /** Table cell editor component. Contains special focus control. */
    private static class FocusHackedPropertyPanel extends PropertyPanel {
        
        public FocusHackedPropertyPanel(PropertyModel model, int preferences) {
            super(model, preferences);
        }

        /** Forward focus request to component that can be focused. */
        public void requestFocus() {
            requestDefaultFocus();
        }
        
        /** JTable.processKeyBinding forgets to call request focus on cell editor.
         * So lets do it ourselves.
         */
        public void addNotify() {
            super.addNotify();
            requestFocus();
        }
        
    }
}
