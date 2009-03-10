/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import javax.swing.*;

public abstract class UserAction extends AbstractAction {
    public UserAction(String text,
                      ImageIcon icon,
                      String desc,
                      Integer mnemonic,
                      KeyStroke key) {
        super(text,icon);
        putValue(SHORT_DESCRIPTION,desc);
        putValue(MNEMONIC_KEY,mnemonic);
        putValue(ACCELERATOR_KEY,key);
    }
}
