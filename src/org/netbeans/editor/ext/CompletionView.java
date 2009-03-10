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
* Code copmletion view component interface. It best fits the <tt>JList</tt>
* but some users may require something else e.g. JTable for displaying
* the result of the completion query.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface CompletionView {

    /**
     * Populate the view with the result from a query.
     * @param result completions query result or <code>null</code> if not
     * computed yet.
     */
    public void setResult(CompletionQuery.Result result);

    /** Get the index of the currently selected item. */
    public int getSelectedIndex();

    /** Go up to the previous item in the data list.
    * The <tt>getSelectedIndex</tt> must reflect the change.
    */
    public void up();

    /** Go down to the next item in the data list.
    * The <tt>getSelectedIndex</tt> must reflect the change.
    */
    public void down();

    /** Go up one page in the data item list.
    * The <tt>getSelectedIndex</tt> must reflect the change.
    */
    public void pageUp();

    /** Go down one page in the data item list.
    * The <tt>getSelectedIndex</tt> must reflect the change.
    */
    public void pageDown();

    /** Go to the first item in the data item list.
    * The <tt>getSelectedIndex</tt> must reflect the change.
    */
    public void begin();

    /** Go to the last item in the data item list.
    * The <tt>getSelectedIndex</tt> must reflect the change.
    */
    public void end();

}
