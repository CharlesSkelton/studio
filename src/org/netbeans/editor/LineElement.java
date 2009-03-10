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

import javax.swing.text.*;

/**
 * Line element implementation.
 * <BR>The implementation consist of only one backward bias mark.
 * There is a link to next mark to satisfy
 * {@link javax.swing.text.Element#getEndOffset()}.
 * <BR>This way allows to have just three objects
 * (element, element-finalizer, mark) per line of text
 * compared to seven (element, 2 * (position, position-finalizer, mark))
 * in regular leaf element.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

final class LineElement implements Element {
    
    /** Parent and root element */
    private LineRootElement root;
    
    /** Mark at the begining of the line */
    private MultiMark mark;
    
    /** Next line or null if this is the last line. */
    private LineElement next;
    
    /** Attributes of this line element */
    private AttributeSet attributes;
    
    private Syntax.StateInfo syntaxStateInfo;

    LineElement(LineRootElement root, int offset, LineElement next) {
        this.root = root;
        
        try {
            this.mark = ((BaseDocument)root.getDocument())
                .createBiasMark(offset, Position.Bias.Backward);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e.toString());
        }
        
        this.next = next;
    }

    public Document getDocument() {
        return root.getDocument();
    }

    public int getStartOffset() {
        return mark.getOffset();
    }

    public int getEndOffset() {
        return (next != null)
            ? next.getStartOffset()
            : (getDocument().getLength() + 1); // compatibility with PlainDocument
    }

    public Element getParentElement() {
        return root;
    }

    public String getName() {
        return AbstractDocument.ParagraphElementName;
    }

    public AttributeSet getAttributes() {
        return attributes;
    }
    
    public void setAttributes(AttributeSet attributes) {
        this.attributes = attributes;
    }

    public int getElementIndex(int offset) {
        return -1;
    }

    public int getElementCount() {
        return 0;
    }

    public Element getElement(int index) {
        return null;
    }

    public boolean isLeaf() {
        return true;
    }
    
    LineElement getNext() {
        return next;
    }

    void setNext(LineElement next) {
        this.next = next;
    }

    protected void finalize() throws Throwable {
        mark.dispose();

        super.finalize();
    }
    
    Syntax.StateInfo getSyntaxStateInfo() {
        return syntaxStateInfo;
    }

    void updateSyntaxStateInfo(Syntax syntax) {
        if (syntaxStateInfo == null) {
            syntaxStateInfo = syntax.createStateInfo();
        }
        syntax.storeState(syntaxStateInfo);
    }

    void clearSyntaxStateInfo() {
        syntaxStateInfo = null;
    }

    public String toString() {
        return "getStartOffset()=" + getStartOffset() // NOI18N
            + ", getEndOffset()=" + getEndOffset(); // NOI18N
    }

}
