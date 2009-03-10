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

import org.netbeans.editor.LocaleSupport;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
* Code completion view component interface. It best fits the <tt>JList</tt>
* but some users may require something else e.g. JTable.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class ListCompletionView extends JList implements CompletionView {

    public ListCompletionView() {
        this(null);
    }

    public ListCompletionView(ListCellRenderer renderer) {
        setSelectionMode( javax.swing.ListSelectionModel.SINGLE_SELECTION );
        if (renderer != null) {
            setCellRenderer(renderer);
        }
        getAccessibleContext().setAccessibleName(LocaleSupport.getString("ACSN_CompletionView"));
        getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_CompletionView"));
        
        
    }

    /** Populate the view with the result from a query. */
    public void setResult(CompletionQuery.Result result) {
        if (result != null) {
            setResult(result.getData());
        } else {
            setResult(Collections.EMPTY_LIST);
        }
    }
    
    public void setResult(List data) {
        if (data != null) {
            setModel(new Model(data));
            if (data.size() > 0) {
                setSelectedIndex(0);
            }
        }
    }

    /** Force the list to ignore the visible-row-count property */
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public void up() {
        if (getModel().getSize() > 0) {
            setSelectedIndex(getSelectedIndex() - 1);
            ensureIndexIsVisible(getSelectedIndex());
        }
    }

    public void down() {
        int lastInd = getModel().getSize() - 1;
        if (lastInd >= 0) {
            setSelectedIndex(Math.min(getSelectedIndex() + 1, lastInd));
            ensureIndexIsVisible(getSelectedIndex());
        }
    }

    public void pageUp() {
        if (getModel().getSize() > 0) {
            int pageSize = Math.max(getLastVisibleIndex() - getFirstVisibleIndex(), 0);
            int firstInd = Math.max(getFirstVisibleIndex() - pageSize, 0);
            int ind = Math.max(getSelectedIndex() - pageSize, firstInd);

            ensureIndexIsVisible(firstInd);
            setSelectedIndex(ind);
            ensureIndexIsVisible(ind);
        }
    }

    public void pageDown() {
        int lastInd = getModel().getSize() - 1;
        if (lastInd >= 0) {
            int pageSize = Math.max(getLastVisibleIndex() - getFirstVisibleIndex(), 0);
            lastInd = Math.max(Math.min(getLastVisibleIndex() + pageSize, lastInd), 0);
            int ind = Math.max(Math.min(getSelectedIndex() + pageSize, lastInd), 0);

            ensureIndexIsVisible(lastInd);
            setSelectedIndex(ind);
            ensureIndexIsVisible(ind);
        }
    }

    public void begin() {
        if (getModel().getSize() > 0) {
            setSelectedIndex(0);
            ensureIndexIsVisible(0);
        }
    }

    public void end() {
        int lastInd = getModel().getSize() - 1;
        if (lastInd >= 0) {
            setSelectedIndex(lastInd);
            ensureIndexIsVisible(lastInd);
        }
    }

    public void setVisible(boolean visible) {
        // ??? never called
//        System.err.println("ListCompletionView.setVisible(" + visible + ")");
        super.setVisible(visible);
    }
    
    static class Model extends AbstractListModel {

        List data;

        static final long serialVersionUID = 3292276783870598274L;

        public Model(List data) {
            this.data = data;
        }

        public int getSize() {
            return data.size();
        }

        public Object getElementAt(int index) {
            return (index >= 0 && index < data.size()) ? data.get(index) : null;
        }

        List getData() {
            return data;
        }

    }

}
