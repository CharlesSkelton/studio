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


import java.awt.Component;
import java.awt.Image;
import java.beans.BeanInfo;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import org.openide.ErrorManager;

import org.openide.nodes.Node;
import org.openide.util.Utilities;


/** Default renderer for nodes. Can paint either Nodes directly or
 * can be used to paint objects produced by NodeTreeModel, etc.
 *
 * @see org.openide.nodes.Node
 *
 * @author Jaroslav Tulach
 */
public class NodeRenderer extends Object
implements TreeCellRenderer, ListCellRenderer {
    /** Shared instance of <code>NodeRenderer</code>. */
    private static NodeRenderer sharedInstance;

    /** Flag indicating if to use big icons. */
    private boolean bigIcons;

    static Border emptyBorder = new EmptyBorder(1, 1, 1, 1);
    
    /** Creates default renderer. */
    public NodeRenderer () {
    }


    /** Creates renderer.
     * @param bigIcons use big icons if possible
     */
    public NodeRenderer (boolean bigIcons) {
        this.bigIcons = bigIcons;
    }


    /** Gets for one singleton <code>sharedInstance</code>. */
    public static NodeRenderer sharedInstance () {
        if (sharedInstance == null) {
            sharedInstance = new NodeRenderer ();
        }
        return sharedInstance;
    }

    
    //
    // Rendering methods
    //

    /** Finds the component that is capable of drawing the cell in a tree.
     * @param value value can be either <code>Node</code> 
     * or a <code>VisualizerNode</code>.
     * @return component to draw the value
     */
    public Component getTreeCellRendererComponent(
        JTree tree, Object value,
        boolean sel, boolean expanded,
        boolean leaf, int row, boolean hasFocus
    ) {
        // accepting either Node or Visualizers
        VisualizerNode vis = (value instanceof Node) ?
                             VisualizerNode.getVisualizer (null, (Node)value)
                             :
                             (VisualizerNode)value;

        return getTree().getTreeCellRendererComponent (
                   tree, value, sel, expanded, leaf, row, hasFocus
               );
    }


    /** This is the only method defined by <code>ListCellRenderer</code>.  We just
     * reconfigure the <code>Jlabel</code> each time we're called.
     */
    public Component getListCellRendererComponent (
        JList list,
        Object value,            // value to display
        int index,               // cell index
        boolean isSelected,      // is the cell selected
        boolean cellHasFocus     // the list and the cell have the focus
    ) {
        // accepting either Node or Visualizers
        VisualizerNode vis = (value instanceof Node) ?
                             VisualizerNode.getVisualizer (null, (Node)value)
                             :
                             (VisualizerNode)value;

        if (vis == null) {
            vis = VisualizerNode.EMPTY;
        }

        ListCellRenderer r = bigIcons ? (ListCellRenderer)getPane() : getList();

        return r.getListCellRendererComponent (
                   list, vis, index, isSelected, cellHasFocus
               );
    }

    // ********************
    // Support for dragging
    // ********************

    /** Value of the cell with 'drag under' visual feedback */
    private static VisualizerNode draggedOver;


    /** DnD operation enters. Update look and feel to the 'drag under' state.
     * @param value the value of cell which should have 'drag under' visual feedback
     */
    static void dragEnter (Object dragged) {
        draggedOver = (VisualizerNode)dragged;
    }

    /** DnD operation exits. Revert to the normal look and feel. */
    static void dragExit () {
        draggedOver = null;
    }


    // ********************
    // Cache for ImageIcons
    // ********************

    /** default icon to use when none is present */
    private static final String DEFAULT_ICON = "org/openide/resources/defaultNode.gif"; // NOI18N

    /** loaded default icon */
    private static ImageIcon defaultIcon;

    /** of icons used (Image, IconImage)*/
    private static final WeakHashMap map = new WeakHashMap ();

    /** Loades default icon if not loaded. */
    static ImageIcon getDefaultIcon () {
        if (defaultIcon == null) {
            defaultIcon = new ImageIcon(Utilities.loadImage(DEFAULT_ICON));
        }

        return defaultIcon;
    }

    /** Finds imager for given resource.
     * @param image image to get
     * @return icon for the image
     */
    static ImageIcon getIcon (Image image) {
        Reference ref = (Reference)map.get (image);

        ImageIcon icon = ref == null ? null : (ImageIcon)ref.get ();
        if (icon != null) {
            return icon;
        }

        icon = new ImageIcon (image);
        map.put (image, new WeakReference (icon));

        return icon;
    }

    //
    // Renderers
    //


    private static NodeRenderer.Tree tree = null;

    private synchronized static NodeRenderer.Tree getTree () {
        if (tree == null)
            tree = new NodeRenderer.Tree ();
        return tree;
    }

    private static NodeRenderer.Pane pane = null;

    private synchronized static NodeRenderer.Pane getPane() {
        if (pane == null)
            pane = new NodeRenderer.Pane ();
        return pane;
    }

    private static NodeRenderer.List list = null;

    private synchronized static NodeRenderer.List getList() {
        if (list == null)
            list = new NodeRenderer.List ();
        return list;
    }


    /** Tree cell renderer. Accepts only <code>VisualizerNode</code> values. */
    final static class Tree extends DefaultTreeCellRenderer {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -183570483117501696L;

        /** The borders for visual feedback during DnD operation. */
        static Border activeBorder = new LineBorder(UIManager.getColor("List.focusCellHighlight")); // NOI18N

	/** Empty 0/1pixel border used by the Renderer */
	static Border inactiveBorder;
	static {
	    int width = DragDropUtilities.dragAndDropEnabled ? 1 : 0;
	    inactiveBorder = new EmptyBorder(width, width, width, width);
	}

        /** @return Rendered cell component */
        public Component getTreeCellRendererComponent(
            JTree tree, Object value,
            boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus
        ) {
            setEnabled(tree.isEnabled());

            // accepts only VisualizerNode
            VisualizerNode vis = (VisualizerNode)value;
            
	    Image iconImg;
            if (expanded) {
                iconImg = vis.node.getOpenedIcon(BeanInfo.ICON_COLOR_16x16);
            } else {
                iconImg = vis.node.getIcon(BeanInfo.ICON_COLOR_16x16);
	    }
            
            // bugfix #28515, check if getIcon contract isn't broken
            if (iconImg == null) {
                ErrorManager.getDefault ().log (ErrorManager.WARNING, "Node " + vis.node.getName () + // NOI18N
                    "[" +vis.node.getClass ()+ "] cannot return null icon. See Node.getIcon/getOpenedIcon contract."); // NOI18N
            } else {
                ImageIcon nodeicon = NodeRenderer.getIcon(iconImg);

                setIconTextGap (4 - nodeicon.getIconWidth()
                                    + ( nodeicon.getIconWidth() > 24 ? nodeicon.getIconWidth() : 24 ) );
                setIcon(nodeicon);
            }

            setText(vis.getDisplayName ());

            // provide "drag under" feedback if DnD operation is active // NOI18N
            if (vis == draggedOver) {
                sel = true;
            }

	    this.hasFocus = hasFocus;
            selected = sel;

            if(sel) {
                setForeground(getTextSelectionColor());
            } else {
                setForeground(getTextNonSelectionColor());
            }

            return this;
        }

	protected void firePropertyChange(String name, Object old, Object nw) {
	    // do really nothing!
        }

    } // End of class Tree.

    
    /** Implements a <code>ListCellRenderer</code> for rendering items 
     * of a <code>List</code> containing <code>Node</code>s.
     * It displays the node's 16x16 icon and its display name.
     *
     * @author   Ian Formanek
     */
    static final class List extends JLabel implements ListCellRenderer {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -8387317362588264203L;

	/** Focused Node border. */
        protected static Border focusBorder = new LineBorder(UIManager.getColor("List.focusCellHighlight")); // NOI18N

	public List() {
            setOpaque(true);
	}

        /** This is the only method defined by ListCellRenderer.  We just
         * reconfigure the Jlabel each time we're called.
         */
        public Component getListCellRendererComponent (
            JList list,
            Object value,            // value to display
            int index,               // cell index
            boolean isSelected,      // is the cell selected
            boolean cellHasFocus)    // the list and the cell have the focus
        {
            VisualizerNode vis = (VisualizerNode)value;
            ImageIcon nodeicon = NodeRenderer.getIcon(vis.node.getIcon(BeanInfo.ICON_COLOR_16x16));
            setIcon(nodeicon);
            setText(vis.getDisplayName ());

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setIconTextGap (4 - nodeicon.getIconWidth()
                            + ( nodeicon.getIconWidth() > 24 ? nodeicon.getIconWidth() : 24 ) );


            int delta = NodeListModel.findVisualizerDepth (list.getModel (), vis);

            Border border = (cellHasFocus || value == draggedOver) ? focusBorder : emptyBorder;
            if (delta > 0) {
                border = new CompoundBorder(
                    new EmptyBorder (0, nodeicon.getIconWidth() * delta, 0, 0),
                    border
                );
            }
            setBorder(border);

            return this;
        }

	protected void firePropertyChange(String name, Object old, Object nw) {
	    // do really nothing!
        }

    } // End of class List.


    /** List cell renderer which renders icon and display name from <code>VisualizerNode</code>. */
    final static class Pane extends JLabel implements ListCellRenderer {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -5100925551665387243L;

	/** Focused Node border. */
	static Border focusBorder = LineBorder.createBlackLineBorder();

        /** Creates a new NetbeansListCellRenderer */
        public Pane () {
            setOpaque(true);
            setVerticalTextPosition(JLabel.BOTTOM);
            setHorizontalAlignment(JLabel.CENTER);
            setHorizontalTextPosition(JLabel.CENTER);
        }

        /** This is the only method defined by ListCellRenderer.  We just
         * reconfigure the Jlabel each time we're called.
         * @param list the JList
         * @param value the value returned by list.getModel().getElementAt(index)
         * @param index the cells index
         * @param isSelected <code>true</code> if the specified cell was selected
         * @param cellHasFocus <code>true</code> if the specified cell has the focus
         * @return a component whose paint() method will render the specified value
         */
        public Component getListCellRendererComponent (
            JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus
        ) {
            VisualizerNode vis = (VisualizerNode)value;

            setIcon(NodeRenderer.getIcon(vis.node.getIcon(BeanInfo.ICON_COLOR_32x32)));
            setText(vis.getDisplayName ());

            if (isSelected){
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setBorder(cellHasFocus ? focusBorder : emptyBorder);
            return this;
        }

	protected void firePropertyChange(String name, Object old, Object nw) {
	    // do really nothing!
        }

    } // End of class Pane.


}
