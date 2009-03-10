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

package org.netbeans.editor.ext;

/**
* Pane displaying the completion view and accompanying components
* like label for title etc. It can be a scroll-pane with the label
* at the top or something else.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface CompletionPane {

    /** Is the pane visible? */
    public boolean isVisible();

    /** Set the pane to be visible. */
    public void setVisible(boolean visible);

    /** Possibly refresh the look after either the view was changed
    * or title was changed or both.
    */
    public void refresh();

    /** Set the title of the pane according to the completion query results. */
    public void setTitle(String title);

}
