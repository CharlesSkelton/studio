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

import javax.swing.*;

/**
 *  Pane displaying the javadoc view and accompanying components
 *  like toolbar for navigation etc. 
 *
 *  @author  Martin Roskanin
 *  @since   03/2002
 */
public interface JavaDocPane {
    
    /** Returns component of JavaDocPane implementation */
    public JComponent getComponent();
    
    /** enables/disables forward button */
    public void setForwardEnabled(boolean enable);

    /** enables/disables back button */
    public void setBackEnabled(boolean enable);
    
    /** enables/disables 'show in external browser' button */
    public void setShowWebEnabled(boolean enable);
    
    public JComponent getJavadocDisplayComponent();
}

