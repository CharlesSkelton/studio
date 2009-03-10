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

package org.netbeans.editor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

/**
* Text UI implementation
* 
* @author  Miloslav Metelka
* @version 1.00
*/

public class BaseTextUI extends TextUI
    implements ViewFactory, PropertyChangeListener, DocumentListener {

    /** Get rid of mantisa problems */
    private static final int MAX_SPAN = Integer.MAX_VALUE - 512;

    /** Minimum component width */
    private static final int MIN_WIDTH = 300;
    /** Minimum component height */
    private static final int MIN_HEIGHT = 200;

    /** Editor component */
    private JTextComponent component;

    /** Extended UI */
    private EditorUI editorUI;

    /** ID of the component in registry */
    int componentID = -1;

    /** Instance of the <tt>GetFocusedComponentAction</tt> */
    private static final GetFocusedComponentAction gfcAction
    = new GetFocusedComponentAction();

    /** Root view of view hierarchy */
    private RootView rootView;

    public BaseTextUI() {
        rootView = new RootView();
    }

    public static JTextComponent getFocusedComponent() {
        return gfcAction.getFocusedComponent2();
    }

    /** Called when the model of component is changed */
    protected void modelChanged(BaseDocument oldDoc, BaseDocument newDoc) {
        if (oldDoc != null) {
            oldDoc.removeDocumentListener(this);
        }

        if (newDoc != null) {
            newDoc.addDocumentListener(this);
            Registry.activate(newDoc); // Activate the new document

            ViewFactory f = rootView.getViewFactory();
            BaseKit kit = (BaseKit)getEditorKit(component);

            component.removeAll();

            Element elem = newDoc.getDefaultRootElement();
            View v = f.create(elem);
            rootView.setView(v);
            rootView.updateMainHeight(); // compute actual height of views

            component.revalidate();

            // Execute actions related to document installaction into the component
            Settings.KitAndValue[] kv = Settings.getValueHierarchy(kit.getClass(),
                                        SettingsNames.DOC_INSTALL_ACTION_NAME_LIST);
            for (int i = kv.length - 1; i >= 0; i--) {
                List actList = (List)kv[i].value;
                actList = kit.translateActionNameList(actList); // translate names to actions
                if (actList != null) {
                    for (Iterator iter = actList.iterator(); iter.hasNext();) {
                        Action a = (Action)iter.next();
                        a.actionPerformed(new ActionEvent(component,
                                                          ActionEvent.ACTION_PERFORMED, "")); // NOI18N
                    }
                }
            }
        }

    }

    /** Update height of the views */
    void updateHeight() {
        rootView.updateMainHeight();
    }

    /** Installs the UI for a component. */
    public void installUI(JComponent c) {
        if (c instanceof JTextComponent) {
            component = (JTextComponent) c; // this is associated component

            getEditorUI().installUI(component);

            component.setOpaque(true);   // opaque by default
            component.setAutoscrolls(true); // autoscrolling by default

            // attach to the model and component
            component.addPropertyChangeListener(this);

            BaseKit kit = (BaseKit)getEditorKit(component);
            // Create and attach caret
            Caret caret = kit.createCaret();
            component.setCaretColor(Color.black); // will be changed by settings later
            component.setCaret(caret);

            // assign blink rate
            int br = SettingsUtil.getInteger(Utilities.getKitClass(component), SettingsNames.CARET_BLINK_RATE,
                                             SettingsDefaults.defaultCaretBlinkRate.intValue());
            caret.setBlinkRate(br);

            // Create document
            BaseDocument doc = Utilities.getDocument(component);
            if (doc != null) {
                modelChanged(null, doc);
            }

            /** Patch for 1.3 - assigns a null UI input map into the component.
            * The following block stands for the following code:
            *
            * SwingUtilities.replaceUIInputMap(c, JComponent.WHEN_FOCUSED, null);
            *
            */
            
            try {
                Class inputMapClass = Class.forName("javax.swing.InputMap"); // NOI18N
                if (inputMapClass != null) {
                    java.lang.reflect.Method replaceUIInputMapMethod = SwingUtilities.class.getDeclaredMethod(
                                "replaceUIInputMap", new Class[] { JComponent.class, Integer.TYPE, inputMapClass }); // NOI18N
                    replaceUIInputMapMethod.invoke(null,
                                                   new Object[] { c, new Integer(JComponent.WHEN_FOCUSED), null });
                }
            } catch (Throwable t) {
            }

            Registry.addComponent(component);
            Registry.activate(component);
            component.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
    }

    /** Deinstalls the UI for a component */
    public void uninstallUI(JComponent c) {
        component.removePropertyChangeListener(this);
        component.getDocument().removeDocumentListener(this);

        rootView.setView(null); // sets inner view of root view to null
        component.removeAll();
        component.setKeymap(null);

        component.setCaret(null);
        
        getEditorUI().uninstallUI(component);
	Registry.removeComponent(component);

        // Clear the editorUI so it will be recreated according to the kit
        // of the component for which the installUI() is called
        editorUI = null;
	
	component = null;
    }

    /** Paint the UI.
    *
    * @param g the graphics context
    * @param c the editor component
    */
    public void paint(Graphics g, JComponent c) {
        if ((rootView.getViewCount() > 0) && (rootView.getView(0) != null)) {
            BaseDocument doc = Utilities.getDocument(component);
            if (c == component && doc != null) {
                doc.readLock();
                try {

                    // paint the view hierarchy
                    getEditorUI().paint(g);

                    // paint the caret
                    Caret caret = component.getCaret();
                    if (caret != null) {
                        caret.paint(g);
                    }

                    // check virtual size change
                    if (getEditorUI().virtualSizeUpdated) {
                        preferenceChanged(true, true);
                    }

                } finally {
                    doc.readUnlock();
                }
            }
        }
    }

    /** Paint either image region or classic graphics region */
    void paintRegion(Graphics g) {
        rootView.paint(g, null);
    }

    /** Gets the preferred size for the editor component.  If the component
    * has been given a size prior to receiving this request, it will
    * set the size of the view hierarchy to reflect the size of the component
    * before requesting the preferred size of the view hierarchy.  This
    * allows formatted views to format to the current component size before
    * answering the request.  Other views don't care about currently formatted
    * size and give the same answer either way.
    *
    * @param c the editor component
    * @return the size
    */
    public Dimension getPreferredSize(JComponent c) {
        Insets i = c.getInsets();
        Insets margin = getEditorUI().getTextMargin();
        Dimension d = c.getSize();
        BaseDocument doc = (c instanceof JTextComponent)
            ? Utilities.getDocument((JTextComponent)c) : null;
        if (doc != null) {
            doc.readLock();
        }
        try {
            // first try to change the root view size
            if ((d.width > (i.left + i.right)) && (d.height > (i.top + i.bottom))) {
                rootView.setSize(d.width - i.left - i.right, d.height - i.top - i.bottom);
            }
            // now get the real preferred size
            d.width = (int)Math.min(rootView.getPreferredSpan(View.X_AXIS) +
                                    i.left + i.right + margin.left + margin.right, MAX_SPAN);
            d.height = (int)Math.min(rootView.getPreferredSpan(View.Y_AXIS) +
                                     i.top + i.bottom + margin.top + margin.bottom, MAX_SPAN);
        } finally {
            if (doc != null) {
                doc.readUnlock();
            }
        }
        return d;
    }

    /** Gets the minimum size for the editor component.
    *
    * @param c the editor component
    * @return the size
    */
    public Dimension getMinimumSize(JComponent c) {
        Insets i = c.getInsets();
        Insets margin = getEditorUI().getTextMargin();
        Dimension d = new Dimension();
        BaseDocument doc = (c instanceof JTextComponent)
            ? Utilities.getDocument((JTextComponent)c) : null;
        if (doc != null) {
            doc.readLock();
        }
        try {
            d.width = (int) rootView.getMinimumSpan(View.X_AXIS)
                      + i.left + i.right + margin.left + margin.right;
            d.height = (int)  rootView.getMinimumSpan(View.Y_AXIS)
                       + i.top + i.bottom + margin.top + margin.bottom;
        } finally {
            if (doc != null) {
                doc.readUnlock();
            }
        }
        return d;
    }

    /** Gets the maximum size for the editor component.
    *
    * @param c the editor component
    * @return the size
    */
    public Dimension getMaximumSize(JComponent c) {
        Insets i = c.getInsets();
        Insets margin = getEditorUI().getTextMargin();
        Dimension d = new Dimension();
        BaseDocument doc = (c instanceof JTextComponent)
            ? Utilities.getDocument((JTextComponent)c) : null;
        if (doc != null) {
            doc.readLock();
        }
        try {
            d.width = (int)Math.min(rootView.getMaximumSpan(View.X_AXIS)
                                    + i.left + i.right + margin.left + margin.right, MAX_SPAN);
            d.height = (int)Math.min(rootView.getMaximumSpan(View.Y_AXIS)
                                     + i.top + i.bottom + margin.top + margin.bottom, MAX_SPAN);
        } finally {
            if (doc != null) {
                doc.readUnlock();
            }
        }
        return d;
    }

    public void invalidateStartY() {
        rootView.invalidateStartY();
    }

    /** Similair to modelToView() but without acquiring the document read lock. */
    public Rectangle modelToView(JTextComponent c, int pos)
    throws BadLocationException {
        return (Rectangle)rootView.modelToView(pos, null, Position.Bias.Forward);
    }

    public Rectangle modelToView(JTextComponent c, int pos, Position.Bias bias)
    throws BadLocationException {
        return (Rectangle)rootView.modelToView(pos, null, bias);
    }

    void modelToViewDG(int pos, DrawGraphics dg) throws BadLocationException {
        rootView.modelToViewDG(pos, dg);
    }


    public int getYFromPos(int pos) throws BadLocationException {
        return rootView.getYFromPos(pos);
    }

    public int getPosFromY(int y) throws BadLocationException {
        return rootView.getPosFromY(y);
    }

    public int getBaseX(int y) {
        return rootView.getBaseX(y);
    }

    public int viewToModel(JTextComponent c, Point pt) {
        return viewToModel(c, pt.x, pt.y);
    }

    public int viewToModel(JTextComponent c, int x, int y) {
        return rootView.viewToModel(x, y, null, null);
    }

    public int viewToModel(JTextComponent c, Point pt, Position.Bias[] biasReturn) {
        return rootView.viewToModel(pt.x, pt.y, null, biasReturn);
    }

    /** Next visually represented model location where caret can be placed.
    * This version works without placing read lock on the document.
    */
    public int getNextVisualPositionFrom(JTextComponent t, int pos,
                                         Position.Bias b, int direction, Position.Bias[] biasRet)
    throws BadLocationException{
        return rootView.getNextVisualPositionFrom(pos, b, null, direction, biasRet);
    }

    public void damageRange(JTextComponent c, int p0, int p1) {
        damageRange(c, p0, p1, Position.Bias.Forward, Position.Bias.Backward);
    }

    /** Causes the portion of the view responsible for the
    * given part of the model to be repainted.
    *
    * @param p0 the beginning of the range >= 0
    * @param p1 the end of the range >= p0
    */
    public void damageRange(JTextComponent t, int p0, int p1,
                            Position.Bias p0Bias, Position.Bias p1Bias) {
        BaseDocument doc = Utilities.getDocument(component);
        if (t == component && doc != null) {
            doc.readLock();
            try {
                Rectangle r = (Rectangle)rootView.modelToView(p0, p0Bias, p1, p1Bias, null);
                component.repaint(r.x, r.y, r.width, r.height);
            } catch (BadLocationException e) {
            } finally {
                doc.readUnlock();
            }
        }
    }

    /** Fetches the EditorKit for the UI.
    *
    * @return the component capabilities
    */
    public EditorKit getEditorKit(JTextComponent c) {
        JEditorPane pane = (JEditorPane)component;
        return pane.getEditorKit();
    }

    /** Fetches a root view of the view hierarchy. */
    public View getRootView(JTextComponent c) {
        return rootView;
    }


    /** Get extended UI. This is called from views to get correct extended UI. */
    public EditorUI getEditorUI() {
        if (editorUI == null) {
            BaseKit kit = (BaseKit)getEditorKit(component);
            editorUI = kit.createEditorUI();
        }
        return editorUI;
    }

    /**
    * This method gets called when a bound property is changed.
    * We are looking for document changes on the component.
    */
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if ("document".equals(propName)) {
            BaseDocument oldDoc = (evt.getOldValue() instanceof BaseDocument)
                                  ? (BaseDocument)evt.getOldValue() : null;
            BaseDocument newDoc = (evt.getNewValue() instanceof BaseDocument)
                                  ? (BaseDocument)evt.getNewValue() : null;
            modelChanged(oldDoc, newDoc);
        }
    }

    /** Insert to document notification. */
    public void insertUpdate(DocumentEvent evt) {
        rootView.insertUpdate(evt, null, rootView.getViewFactory());
        if (((BaseDocumentEvent)evt).getLFCount() > 0) {
            if (getEditorUI().updateVirtualHeight(rootView.getHeight())) {
                preferenceChanged(true, true);
            }
            getEditorUI().checkLineLimit();
        }
    }

    /** Remove from document notification. */
    public void removeUpdate(DocumentEvent evt) {
        rootView.removeUpdate(evt, null, rootView.getViewFactory());
        if (((BaseDocumentEvent)evt).getLFCount() > 0) {
            if (getEditorUI().updateVirtualHeight(rootView.getHeight())) {
                preferenceChanged(true, true);
            }
        }
    }

    /** The change in document notification.
    *
    * @param evt  The change notification from the currently associated document.
    */
    public void changedUpdate(DocumentEvent evt) {
        if (evt instanceof BaseDocumentEvent) {
            BaseDocumentEvent bdevt = (BaseDocumentEvent)evt;
            BaseDocument doc = (BaseDocument)bdevt.getDocument();
            String layerName = bdevt.getDrawLayerName();
            if (layerName != null) {
                getEditorUI().addLayer(doc.findLayer(layerName),
                        bdevt.getDrawLayerVisibility());
            } else { // some other type of change, propagate to root view
                rootView.changedUpdate(evt, null, rootView.getViewFactory());
            }
        }
    }

    /** Creates a view for an element.
    *
    * @param elem the element
    * @return the newly created view or null
    */
    public View create(Element elem) {
        View v = null;
        if (elem instanceof BaseElement) {
            v = new LeafView(elem);
        }
        return v;
    }

    /** Creates a view for an element.
    * @param elem the element
    * @param p0 the starting offset >= 0
    * @param p1 the ending offset >= p0
    * @return the view
    */
    public View create(Element elem, int p0, int p1) {
        return new LeafView(elem);
    }

    // from JEditorPane ui
    public static ComponentUI createUI(JComponent c) {
        return new BaseTextUI();
    }

    /** Specifies that some preference has changed. */
    public void preferenceChanged(boolean width, boolean height) {
        getEditorUI().virtualSizeUpdated = false;
        
        component.revalidate();

    }

    /** Root view */
    class RootView extends BaseView {

        BaseView view;

        RootView() {
            super(null);
        }

        /** Sets the only one inner view of root view to either null or a valid view. */
        void setView(View v) {
            if (v instanceof BaseView) { // only BaseView instances are supported
                if (view != null) {
                    // enable grb.col.
                    view.setParent(null);
                }
                view = (BaseView)v;
                if (view != null) {
                    view.setParent(this);
                }
            }
        }

        /** Fetches attributes associated with view */
        public AttributeSet getAttributes() {
            return null;
        }

        /** Determines the preferred span for this view along an axis.
        *
        * @param axis may be either X_AXIS or Y_AXIS
        * @return the span the view would like to be rendered into.
        */
        public float getPreferredSpan(int axis) {
            EditorUI editorUI = BaseTextUI.this.getEditorUI();
            switch (axis) {
            case X_AXIS:
                return Math.max(BaseTextUI.this.getEditorUI().virtualSize.width, MIN_WIDTH);
            case Y_AXIS:
                return Math.max(BaseTextUI.this.getEditorUI().virtualSize.height, MIN_HEIGHT);
            }
            return 0f;
        }

        /** Determines minimum span along an axis */
        public float getMinimumSpan(int axis) {
            return getPreferredSpan(axis);
        }

        /** Determines maximum span along an axis */
        public float getMaximumSpan(int axis) {
            return Integer.MAX_VALUE;
        }


        /** Specifies that a preference has changed. */
        public void preferenceChanged(View child, boolean width, boolean height) {
            BaseTextUI.this.preferenceChanged(width, height);
        }

        /** Determines the desired alignment for this view along an axis. */
        public float getAlignment(int axis) {
            if (view != null) {
                return view.getAlignment(axis);
            }
            return 0;
        }

        /** Renders the view. */
        public void paint(Graphics g, Shape allocation) {
            if (view != null) {
                view.paint(g, allocation);
            }
        }

        /** Sets the parent view. */
        public void setParent(View parent) {
            // root view has no parent
        }

        /** Returns the number of views in this view. */
        public int getViewCount() {
            return 1;
        }

        /** Gets the n-th view in this container. */
        public View getView(int n) {
            return view;
        }

        /** Fetches the allocation for the given child view.
        * Returns the whole allocated area.
        */
        public Shape getChildAllocation(int index, Shape a) {
            return a;
        }

        /** Provides a mapping from the document model coordinate space
        * to the coordinate space of the view mapped to it.
        */
        public Shape modelToView(int pos, Shape a, Position.Bias b)
        throws BadLocationException {
            if (view != null) {
                return view.modelToView(pos, a, b);
            }
            return null;
        }

        public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a)
        throws BadLocationException {
            if (view != null) {
                return view.modelToView(p0, b0, p1, b1, a);
            }
            return null;
        }

        public void modelToViewDG(int pos, DrawGraphics dg) throws BadLocationException {
            if (view != null) {
                view.modelToViewDG(pos, dg);
            }
        }

        /** Determine next visually represented model location where caret
        * can be placed.
        */
        public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a,
                                             int direction, Position.Bias[] biasRet) throws BadLocationException {
            if (view != null) {
                int nextPos = view.getNextVisualPositionFrom(pos, b, a, direction, biasRet);
                if (nextPos != -1) {
                    pos = nextPos;
                } else {
                    if (biasRet != null) {
                        biasRet[0] = b;
                    }
                }
            }
            return pos;
        }

        /** Provides a mapping from the view coordinate space to the logical
        * coordinate space of the model.
        */
        public int viewToModel(float x, float y, Shape a, Position.Bias[] b) {
            if (view != null) {
                return view.viewToModel(x, y, a, b);
            }
            return -1;
        }

        /** Get y-coord value from position */
        protected int getYFromPos(int pos) throws BadLocationException {
            if (view != null) {
                return view.getYFromPos(pos);
            }
            return 0;
        }

        /** Get position when knowing y-coord */
        protected int getPosFromY(int y) {
            if (view != null) {
                return view.getPosFromY(y);
            }
            return -1;
        }

        protected int getBaseX(int y) {
            if (view != null) {
                return view.getBaseX(y);
            }
            return 0;
        }

        /** Gives notification that something was inserted into the document
        * in a location that this view is responsible for.
        *
        * @param e the change information from the associated document
        * @param a the current allocation of the view
        * @param f the factory to use to rebuild if the view has children
        */
        public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            if (view != null) {
                view.insertUpdate(e, a, f);
            }
        }

        /** Gives notification that something was removed from the document
        * in a location that this view is responsible for.
        *
        * @param e the change information from the associated document
        * @param a the current allocation of the view
        * @param f the factory to use to rebuild if the view has children
        */
        public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            if (view != null) {
                view.removeUpdate(e, a, f);
            }
        }

        /**
        * Gives notification from the document that attributes were changed
        * in a location that this view is responsible for.
        *
        * @param e the change information from the associated document
        * @param a the current allocation of the view
        * @param f the factory to use to rebuild if the view has children
        */
        public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            if (view != null) {
                view.changedUpdate(e, a, f);
            }
        }

        /** Returns the document model underlying the view. */
        public Document getDocument() {
            return component.getDocument();
        }

        /** Returns the starting offset into the model for this view. */
        public int getStartOffset() {
            if (view != null) {
                return view.getStartOffset();
            }
            return getElement().getStartOffset();
        }

        /**
         * Returns the ending offset into the model for this view.
         *
         * @return the ending offset
         */
        public int getEndOffset() {
            if (view != null) {
                return view.getEndOffset();
            }
            return getElement().getEndOffset();
        }

        /** Gets the element that this view is mapped to. */
        public Element getElement() {
            if (view != null) {
                return view.getElement();
            }
            return component.getDocument().getDefaultRootElement();
        }

        /** Breaks this view on the given axis at the given length.  */
        public View breakView(int axis, float len, Shape a) {
            return null; // no breaking of the root view
        }

        /** Determines the resizability of the view along the
        * given axis.  A value of 0 or less is not resizable. */
        public int getResizeWeight(int axis) {
            if (view != null) {
                return view.getResizeWeight(axis);
            }
            return 0;
        }

        /** Sets the view size. */
        public void setSize(float width, float height) {
            if (view != null) {
                view.setSize(width, height);
            }
        }

        /** Fetches the container hosting the view. */
        public Container getContainer() {
            return component;
        }

        /** Fetches the factory to be used for building the
        * various view fragments that make up the view that
        * represents the model.  This is what determines
        * how the model will be represented.  This is implemented
        * to fetch the factory provided by the associated
        * EditorKit unless that is null, in which case this
        * simply returns the BasicTextUI itself which allows
        * subclasses to implement a simple factory directly without
        * creating extra objects.  
        *
        * @return the factory
        */
        public ViewFactory getViewFactory() {
            EditorKit kit = getEditorKit(component);
            ViewFactory f = kit.getViewFactory();
            if (f != null) {
                return f;
            }
            return BaseTextUI.this;
        }

        protected int getStartY() {
            return BaseTextUI.this.getEditorUI().textMargin.top;
        }

        protected int getPaintAreas(Graphics g, int clipY, int clipHeight) {
            return 0; // no paint areas
        }

        protected void paintAreas(Graphics g, int clipY, int clipHeight, int paintAreas) {
            // no painting for the root view
        }

        protected int getViewStartY(BaseView view, int helperInd) {
            return getStartY();
        }

        protected void invalidateStartY() {
            if (view != null) {
                view.invalidateStartY();
            }
        }


        public int getHeight() {
            if (view != null) {
                return view.getHeight();
            }
            return 0;
        }

        public void updateMainHeight() {
            if (view != null) {
                view.updateMainHeight();
                EditorUI editorUI = BaseTextUI.this.getEditorUI();
                if (editorUI.updateVirtualHeight(getHeight())) {
                    BaseTextUI.this.preferenceChanged(true, true);
                }
            }
        }

    }

    private static class GetFocusedComponentAction extends TextAction {

        private GetFocusedComponentAction() {
            super("get-focused-component");
        }

        public void actionPerformed(ActionEvent evt) {
        }

        JTextComponent getFocusedComponent2() {
            return super.getFocusedComponent();
        }

    }

};
