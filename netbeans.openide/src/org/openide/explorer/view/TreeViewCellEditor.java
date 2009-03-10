/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.explorer.view;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.tree.*;

import org.openide.ErrorManager;
import org.openide.nodes.*;
import org.openide.util.NbBundle;

/** In-place editor in the tree view component.
*
* @author Petr Hamernik
*/
class TreeViewCellEditor extends DefaultTreeCellEditor 
implements CellEditorListener, FocusListener {
    
    /** generated Serialized Version UID */
    static final long serialVersionUID = -2171725285964032312L;

    // Attributes

    /** Indicates whether is drag and drop currently active or not */
    boolean dndActive = false;

    /** Construct a cell editor.
    * @param tree the tree
    * @param renderer the renderer to use for the cell
    */
    public TreeViewCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        super(tree, renderer);
        // deal with selection if already exists
        if (tree.getSelectionCount() == 1) {
            lastPath = tree.getSelectionPath();
        }
        addCellEditorListener(this);
    }
    
    
    /** Implements <code>CellEditorListener</code> interface method. */
    public void editingStopped(ChangeEvent e) {
        //CellEditor sometimes(probably after stopCellEditing() call) gains one focus but loses two
        if (stopped) {
            return;
        }

        stopped = true;
        TreePath lastP = lastPath;
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
                        
                    em.notify(ErrorManager.USER, exc);
                }
            }
        }
    }
    
    /** Implements <code>CellEditorListener</code> interface method. */
    public void editingCanceled(ChangeEvent e) {
        cancelled = true;
    }

    /** True, if the editation was cancelled by the user.
    */
    private boolean cancelled = false;
    /** Stopped is true, if the editation is over (editingStopped is called for the
        first time). The two variables have virtually the same function, but are kept
        separate for code clarity.
    */
    private boolean stopped = false;

    
    /** Overrides superclass method. If the source is a <code>JTextField</code>,
     * i.e. cell editor, it cancels editing, otherwise it calls superclass method. */
    public void actionPerformed(ActionEvent evt) {
        if(evt.getSource() instanceof JTextField) {
            cancelled = true;
            cancelCellEditing();
        } else {
            super.actionPerformed(evt);
        }
    }

    /** Implements <code>FocusListener</code> interface method. */
    public void focusLost (java.awt.event.FocusEvent evt) {
     if (stopped || cancelled)
         return;
     if (!stopCellEditing())
         cancelCellEditing();
    }

    /** Dummy implementation of <code>FocusListener</code> interface method. */
    public void focusGained (java.awt.event.FocusEvent evt) {}
    
    /**
     * This is invoked if a TreeCellEditor is not supplied in the constructor.
     * It returns a TextField editor.
     */
    protected TreeCellEditor createTreeCellEditor() {
        JTextField tf = new JTextField() {

                            public void addNotify() {
                                stopped = cancelled = false;
                                super.addNotify();
                                requestFocus();
                            }
                        };

        tf.registerKeyboardAction(
            this,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
            JComponent.WHEN_FOCUSED
        );

        tf.addFocusListener(this);

        Ed ed = new Ed(tf);
        ed.setClickCountToStart(1);
        return ed;
    }

    /*
    * If the realEditor returns true to this message, prepareForEditing
    * is messaged and true is returned.
    */
    public boolean isCellEditable(EventObject event) {
        if ((event != null) && (event instanceof MouseEvent)) {
            if (!SwingUtilities.isLeftMouseButton((MouseEvent)event) || ((MouseEvent)event).isPopupTrigger()) {
                return false;
            }
        }
        if (lastPath != null) {
            Node n = Visualizer.findNode (lastPath.getLastPathComponent());
            if (n == null || !n.canRename ()) {
                return false;
            }
        }
        else {
            // Disallow rename when multiple nodes are selected
            return false;
        }
        // disallow editing if we are in DnD operation
        if (dndActive) {
            return false;
        }

        return super.isCellEditable(event);
    }

    protected void determineOffset(JTree tree, Object value,
                                   boolean sel, boolean expanded,
                                   boolean leaf, int row) {
	if(renderer != null) {
	    renderer.getTreeCellRendererComponent(tree, value, sel, expanded,
			    leaf, row, true);
	    editingIcon = renderer.getIcon ();
            offset = renderer.getIconTextGap () + editingIcon.getIconWidth ();
	} else {
	    editingIcon = null;
	    offset = 0;
	}								      
    }
    
    /*** Sets the state od drag and drop operation.
    * It's here only because of JTree's bug which allows to
    * start the editing even if DnD operation occurs
    * (bug # )
    */
    void setDnDActive (boolean dndActive) {
        this.dndActive = dndActive;
    }

    /** Redefined default cell editor to convert nodes to name */
    static class Ed extends DefaultCellEditor {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -6373058702842751408L;

        public Ed(JTextField tf) {
            super(tf);
        }

        /** Main method of the editor.
        * @return component of editor
        */
        public Component getTreeCellEditorComponent(JTree tree, Object value,
                boolean isSelected, boolean expanded,
                boolean leaf, int row) {
            Node ren = Visualizer.findNode (value);
            if ((ren != null) && (ren.canRename ()))
                delegate.setValue(ren.getName());
            else
                delegate.setValue(""); // NOI18N

            ((JTextField) editorComponent).selectAll();
            return editorComponent;
        }
    }
}
