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

package org.openide.explorer.view;

import java.awt.event.*;
import java.awt.*;
import java.beans.*;
import java.io.*;
import javax.swing.*;

import org.openide.awt.ListPane;
import org.openide.explorer.*;
import org.openide.nodes.*;

/* TODO:
 - improve cell renderer (two lines of text or hints)
 - better behaviour during scrolling (ListPane)
 - external selection bug (BUG ID: 01110034)
*/

/** A view displaying icons.
*
* @author   Jaroslav Tulach
*/
public class IconView extends ListView implements Externalizable {

    /** generated Serialized Version UID */
    static final long serialVersionUID = -9129850245819731264L;


    public IconView () {
    }

    /** Creates the list that will display the data.
    */
    protected JList createList () {
        JList list = new ListPane () {
	    /**
	     * Overrides JComponent's getToolTipText method in order to allow
	     * renderer's tips to be used if it has text set.
	     * @param event the MouseEvent that initiated the ToolTip display
	     */
	    public String getToolTipText (MouseEvent event) {
		if (event != null) {
		    Point p = event.getPoint ();
		    int index = locationToIndex (p);
		    if (index >= 0) {
		        VisualizerNode v = (VisualizerNode)getModel().getElementAt(index);
			String tooltip = v.getShortDescription();
			String displayName = v.getDisplayName ();
			if ((tooltip != null) && !tooltip.equals (displayName)) {
		            return tooltip;
			}
		    }
		}
		return null;
	    }
	};
        list.setCellRenderer (new NodeRenderer (true));
        return list;
    }
    
    
}
