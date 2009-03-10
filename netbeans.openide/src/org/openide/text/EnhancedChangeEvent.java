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

package org.openide.text;

import javax.swing.event.ChangeEvent;
import javax.swing.text.StyledDocument;

/** Extension of ChangeEvent with additional information
 * about document and the state of the document. This
 * event is fired from CloneableEditorSupport
 *
 * @author  David Konecny, Jaroslav Tulach
 */
final class EnhancedChangeEvent extends ChangeEvent {

    /** Whether document is being closed */
    private boolean closing;
    
    /** Reference to document */
    private StyledDocument doc;

    public EnhancedChangeEvent(Object source, StyledDocument doc, boolean closing) {
        super(source);
        this.doc = doc;
        this.closing = closing;
    }
    
    /** Whether document is being closed */
    public boolean isClosingDocument() {
        return closing;
    }
    
    /** Getter for document */
    public StyledDocument getDocument() {
        return doc;
    }
    
}
