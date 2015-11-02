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
