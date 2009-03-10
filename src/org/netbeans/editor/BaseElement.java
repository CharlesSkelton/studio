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

import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
* Element implementation. It serves as parent class
* for both leaf and branch elements. 
*
* @author Miloslav Metelka
* @version 1.00
*/

public abstract class BaseElement implements Element {

    /** Element name attribute */
    public static final String ElementNameAttribute = "$ename"; // NOI18N

    /** Reference to document this element is part of */
    protected BaseDocument doc;

    /** Parent element */
    protected BaseElement parent;

    /** Atributes of this element */
    protected AttributeSet attrs;

    public BaseElement(BaseDocument doc, BaseElement parent, AttributeSet attrs) {
        this.doc = doc;
        this.parent = parent;
        this.attrs = attrs;
    }

    /** Get document this element is part of */
    public Document getDocument() {
        return doc;
    }

    /** Get parent element */
    public Element getParentElement() {
        return parent;
    }

    /** Get element name if defined */
    public String getName() {
        if (attrs.isDefined(ElementNameAttribute)) {
            return (String)attrs.getAttribute(ElementNameAttribute);
        }
        return null;
    }

    /** Get attributes of this element */
    public AttributeSet getAttributes() {
        return attrs;
    }

    /** Get start offset of this element */
    public abstract int getStartOffset();

    /** Get start mark of this element */
    public abstract Mark getStartMark();

    /** Get end offset of this element */
    public abstract int getEndOffset();

    /** Get end mark of this element */
    public abstract Mark getEndMark();

    /** Get child of this element at specified index */
    public abstract Element getElement(int index);

    /** Gets the child element index closest to the given offset. */
    public abstract int getElementIndex(int offset);

    /** Get number of children of this element */
    public abstract int getElementCount();

    /** Does this element have any children? */
    public abstract boolean isLeaf();

}
