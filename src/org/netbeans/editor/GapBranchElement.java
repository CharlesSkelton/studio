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

import javax.swing.event.DocumentEvent;
import javax.swing.text.Element;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Branch element that uses gap array to maintain its children.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public abstract class GapBranchElement implements Element {
    
    protected static final Element[] EMPTY_ELEMENT_ARRAY = new Element[0];

    private GapObjectArray children;
    
    public GapBranchElement() {
        children = new GapObjectArray();
    }
    
    public int getElementCount() {
        return children.getItemCount();
    }
    
    public Element getElement(int index) {
        return (Element)children.getItem(index);
    }
    
    public void copyElements(int srcBegin, int srcEnd, Element dst[], int dstBegin) {
        ObjectArrayUtilities.copyItems(children, srcBegin, srcEnd, dst, dstBegin);
    }

    public int getElementIndex(int offset) {
        int low = 0;
        int high = getElementCount() - 1;
        
        while (low <= high) {
            int mid = (low + high) / 2;
            int elemStartOffset = ((Element)children.getItem(mid)).getStartOffset();
            
            if (elemStartOffset < offset) {
                low = mid + 1;
            } else if (elemStartOffset > offset) {
                high = mid - 1;
            } else { // element starts at offset
                return mid;
            }
        }

        return Math.max(0, high);
    }

    public boolean isLeaf() {
        return false;
    }
    
    protected void replace(int index, int removeCount, Element[] addedElems) {
        children.replace(index, removeCount, addedElems);
    }
    
    /** Get info about <CODE>DocMarks</CODE>. */
    public String toString() {
        return children.toString();
    }

    protected class Undo extends AbstractUndoableEdit
    implements DocumentEvent.ElementChange {
        
        private int index;
        
        private Element[] childrenAdded;
        
        private Element[] childrenRemoved;
       
        public Undo(int index, Element[] childrenRemoved, Element[] childrenAdded) {
            this.index = index;
            this.childrenRemoved = childrenRemoved;
            this.childrenAdded = childrenAdded;
        }
        
	public Element getElement() {
            return GapBranchElement.this;
        }

	public int getIndex() {
            return index;
        }

        public Element[] getChildrenRemoved() {
            return childrenRemoved;
        }

        public Element[] getChildrenAdded() {
            return childrenAdded;
        }

        public void undo() throws CannotUndoException {
            super.undo();

            replace(index, childrenAdded.length, childrenRemoved);
        }
        
        public void redo() throws CannotRedoException {
            super.redo();

            replace(index, childrenRemoved.length, childrenAdded);
        }
        
    }
    
}
