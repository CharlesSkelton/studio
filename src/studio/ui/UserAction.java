package studio.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    public String getText() {
        return (String)getValue(NAME);
    }

    public KeyStroke getKeyStroke() {
        return (KeyStroke)getValue(ACCELERATOR_KEY);
    }

    public static UserAction create(String text, ImageIcon icon,
                               String desc, int mnemonic,
                               KeyStroke key, ActionListener listener) {
        return new UserAction(text, icon, desc, mnemonic, key) {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        };
    }

    public static UserAction create(String text, ImageIcon icon,
                             String desc, int mnemonic,
                             ActionListener listener) {
        return create(text, icon, desc, mnemonic, null, listener);
    }

    public static UserAction create(String text,
                             String desc, int mnemonic,
                             KeyStroke key, ActionListener listener) {
        return create(text, Util.BLANK_ICON, desc, mnemonic, key, listener);
    }

    public static UserAction create(String text,
                             String desc, int mnemonic,
                             ActionListener listener) {
        return create(text, Util.BLANK_ICON, desc, mnemonic, null, listener);
    }

    public static UserAction create(String text, ActionListener listener) {
        return create(text, Util.BLANK_ICON, "", 0, null, listener);
    }
}
