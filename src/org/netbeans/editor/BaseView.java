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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import java.awt.*;

/**
* Base abstract view serves as parent for both
* leaf and branch views.
*
* @author  Miloslav Metelka
* @version 1.00
*/

public abstract class BaseView extends View {

    /** Top insets paint type. */
    protected static final int INSETS_TOP = 1;

    /** Main area paint type. */
    protected static final int MAIN_AREA = 2;

    /** Bottom insets paint type. */
    protected static final int INSETS_BOTTOM = 4;

    /** Is this view packed */
    protected boolean packed;

    /** Index of this view as child view in its parent.
    * This gives the parent view a hint at which index it should
    * search for this view. If this index is incorrect, the parent
    * view searches the whole array of children. However this gives
    * good optimization of child views search process.
    */
    protected int helperInd;

    /** JTextComponent hosting this view */
    private JTextComponent component;

    /** Border insets of this view. Can be null */
    protected Insets insets;

    /** Start y coord. for this view */
    private int startY = -1;

    /** Construct new base view */
    public BaseView(Element elem) {
        super(elem);
    }

    /** Getter for packed flag */
    public boolean isPacked() {
        return packed;
    }

    /** Setter for packed flag */
    public void setPacked(boolean packed) {
        this.packed = packed;
    }

    /** Get aligment along an X_AXIS or Y_AXIS */
    public float getAlignment(int axis) {
        return 0f;
    }

    public abstract void modelToViewDG(int pos, DrawGraphics dg)
    throws BadLocationException;

    /** Get y-coord value from position */
    protected abstract int getYFromPos(int pos) throws BadLocationException;

    /** Get position when knowing y-coord */
    protected abstract int getPosFromY(int y);

    protected abstract int getBaseX(int y);

    /** Returns binary composition of regions that should be painted.
    * It can be binary composition of INSETS_TOP, MAIN_AREA and INSETS_BOTTOM.
    *
    * @param g Graphics to paint through
    * @param clip clipping area of graphics object
    *
    */
    protected abstract int getPaintAreas(Graphics g, int clipY, int clipHeight);

    /** Paint either top insets, main area, or bottom insets.
    *
    * @param g Graphics to paint through
    * @param clip clipping area of graphics object
    * @param paintAreas binary composition of paint areas
    */
    protected abstract void paintAreas(Graphics g, int clipY, int clipHeight, int paintAreas);

    /** It divides painting into three areas:
    * INSETS_TOP, MAIN_AREA, INSETS_BOTTOM. It paints them sequentially
    * using <CODE>paintArea()</CODE> method until it returns false.
    * This implementation also supposes that <CODE>allocation</CODE> is
    * instance of <CODE>Rectangle</CODE> to save object creations. The root
    * view in TextUI implementation will take care to ensure child views will
    * get rectangle instances.
    */
    public void paint(Graphics g, Shape allocation) {
        Rectangle clip = g.getClipBounds();
        if (clip.height < 0 || clip.width < 0) {
            return;
        }
        int paintAreas = getPaintAreas(g, clip.y, clip.height);
        if (paintAreas != 0) {
            paintAreas(g, clip.y, clip.height, paintAreas);
        }
    }

    /** Get component hosting this view. */
    public JTextComponent getComponent() {
        if (component == null) {
            component = (JTextComponent)getContainer();
        }
        return component;
    }

    /** Get insets of this view. */
    public Insets getInsets() {
        return insets;
    }

    /** This function is used to correct information
    * about which index this child view occupies in parent view's
    * array of children. It is called by parent view.
    */
    protected void setHelperInd(int ind) {
        helperInd = ind;
    }

    /** Get child view's y base value. This function is called by children
    * of this view to get its y offset.
    * @param view is child view of this view for which
    *   the offset should be computed.
    * @param helperInd is index that child view has cached to ease
    *   the parent view to search for it in its children array.
    *   If this index is correct, parent uses it. If it's incorrect
    *   parent view searches through the whole array of its children
    *   to find the child. It then calls children's
    *   <CODE>setParentInd()</CODE> to correct its location index.
    */
    protected abstract int getViewStartY(BaseView view, int helperInd);

    /** Informs the view that if it had cached start y of itself
    * it should invalidate it as it is no longer valid and it should call
    * <CODE>getViewStartY()</CODE> to get updated value.
    */
    protected void invalidateStartY() {
        startY = -1;
    }

    /** Get y base value for this view. */
    protected int getStartY() {
        if (startY == -1) { // the value is invalid
            BaseView v = (BaseView)getParent();
            if (v != null) {
                startY = v.getViewStartY(this, helperInd);
            }
        }
        return startY;
    }

    /** Get height of the view */
    public abstract int getHeight();

    /** Update height of main area as result of some important change.
    * Propagate to child views if necessary.
    */
    public abstract void updateMainHeight();

    /** Get preferred span over axis */
    public float getPreferredSpan(int axis) {
        switch (axis) {
        case Y_AXIS:
            return getHeight();
        }
        return 0f;
    }

    /** Get editor UI */
    protected EditorUI getEditorUI() {
        return ((BaseTextUI)getComponent().getUI()).getEditorUI();
    }

    /** Display view hierarchy on console. Current view is marked with asterisk. */
    public void displayHierarchy() {
        // find the root view
        BaseView v = this;
        while (v.getParent() != null) {
            v = (BaseView)v.getParent();
        }
        v.displayHierarchyHelper(this, 0, 0);
    }

    /** Helper for displaying hierarchy - called recursively.
    * @param origView view for which the displayHierarchy method was originally
    *  called. It is marked by asterisk.
    * @param col column offset
    * @param index index of this view in parent children[] array
    */
    private void displayHierarchyHelper(View origView, int col, int index) {
        StringBuffer buf = new StringBuffer();
        buf.append(((this == origView) ? "*" : " ")); // NOI18N
        for (int i = 0; i < col; i++) {
            buf.append(' ');
        }
        buf.append('[');
        buf.append(Integer.toString(index));
        buf.append("] "); // NOI18N
        buf.append(this.toString());
        System.out.println(buf);
        int childrenCnt = getViewCount();
        if (childrenCnt > 0) {
            for (int i = 0; i < childrenCnt; i++) {
                ((BaseView)getView(i)).displayHierarchyHelper(origView, col + 1, i);
            }
        }

    }

    public String toString() {
        return "BaseView=" + System.identityHashCode(this) // NOI18N
               + ", elem=" + getElement() + ", parent=" // NOI18N
               + System.identityHashCode(getParent());
    }

}
