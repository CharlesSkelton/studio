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
import javax.swing.JEditorPane;
import javax.swing.text.*;

/** performance trick - 18% of time saved during open of an editor
*
* @author Ales Novak
*/
final class QuietEditorPane extends JEditorPane {
    final static int FIRE = 0x1;
    final static int PAINT = 0x2;

    final static int ALL = FIRE | PAINT;

    // #21120. Caret was null while serializing CloneableEditor.
    /** Saves last position of caret when, doing it's UI reinstallation. */
    private int lastPosition = -1;
    

    /** is firing of events enabled? */
    int working = FIRE; // [Mila] firing since begining, otherwise doesn't work well

    public void setWorking(int x) {
        working = x;
    }

    public void firePropertyChange(String s, Object val1, Object val2) {
        if ((working & FIRE) != 0) {
            super.firePropertyChange(s, val1, val2);
        }
    }

    /** Overrides superclass method, to keep old caret position.
     * While is reinstallation of UI in progress, there
     * is a gap between the uninstallUI
     * and intstallUI when caret set to <code>null</code>. */
    public void setCaret(Caret caret) {
        if(caret == null) {
            Caret oldCaret = getCaret();
            if(oldCaret != null) {
                lastPosition = oldCaret.getDot();
            }
        }
        
        super.setCaret(caret);
    }

    /** Gets the last caret position, for the case the serialization
     * is done during the time of pane UI reinstallation. */
    int getLastPosition() {
        return lastPosition;
    }
    
    /*
    public void setDocument(Document doc) {
      if (working) {
        super.setDocument(doc);
      }
}

    public void setUI(javax.swing.plaf.TextUI ui) {
      if (working) {
        super.setUI(ui);
      }
}*/

    public void revalidate() {
        if ((working & PAINT) != 0) {
            super.revalidate();
        }
    }

    public void repaint() {
        if ((working & PAINT) != 0) {
            super.repaint();
        }
    }
}

