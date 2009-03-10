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

package org.openide.explorer.propertysheet;

import java.beans.*;
import java.awt.Dimension;
import javax.swing.*;

import org.openide.nodes.Node;
import org.openide.explorer.*;

/** An Explorer view displaying a property sheet.
* @see PropertySheet
* @author   Jan Jancura, Jaroslav Tulach, Ian Formanek
*/
public class PropertySheetView extends PropertySheet {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -7568245745904766160L;
    /** helper flag for avoiding multiple initialization of the GUI */
    transient private boolean guiInitialized = false;

    /** The Listener that tracks changes in explorerManager */
    transient private PropertyIL managerListener;

    /** manager to use */
    transient private ExplorerManager explorerManager;

    /** Initializes the GUI of the view */
    private void initializeGUI() {
        guiInitialized = true;
        // (TDB) extra border deleted
        // setBorder (new javax.swing.border.EtchedBorder());
        managerListener = new PropertyIL();
    }

    public PropertySheetView() {
//        setPreferredSize(new Dimension (200, 300));
    }
    
    /* Initializes the sheet.
    */
    public void addNotify () {
        super.addNotify ();

        explorerManager = ExplorerManager.find (this);
        if (!guiInitialized)
            initializeGUI();

        // add propertyChange listeners to the explorerManager
        explorerManager.addPropertyChangeListener(managerListener);
        setNodes (explorerManager.getSelectedNodes ());
    }

    /* Deinitializes the sheet.
    */
    public void removeNotify () {
        super.removeNotify ();

        if (explorerManager != null) { //[PENDING] patch for bug in JDK1.3 Window
            // (doublecall destroy()&removeNotify() for
            // destroyed, but no garbagecollected windows
            explorerManager.removePropertyChangeListener(managerListener);
            explorerManager = null;
            setNodes (new Node[0]);
        }
    }

    // INNER CLASSES ***************************************************************************

    /**
    * The inner adaptor class for listening to the ExplorerManager's property and
    * vetoable changes.
    */
    class PropertyIL implements PropertyChangeListener {
        public void propertyChange (PropertyChangeEvent evt) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals (evt.getPropertyName ())) {
                setNodes ((Node []) evt.getNewValue ());
            }
        }
    }
}
