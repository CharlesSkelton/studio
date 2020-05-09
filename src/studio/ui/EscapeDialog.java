package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class EscapeDialog extends JDialog {

    enum DialogResult {ACCEPTED, CANCELLED};

    private DialogResult result = DialogResult.CANCELLED;

    public EscapeDialog(Window owner,String title) {
        super(owner,title, ModalityType.APPLICATION_MODAL);
        initComponents();
    }

    public void align() {
        pack();
        Util.centerChildOnParent(this, getParent());
    }

    public void alignAndShow() {
        align();
        setVisible(true);
    }

    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        this.getRootPane().registerKeyboardAction(e->cancel(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void cancel() {
        result = DialogResult.CANCELLED;
        dispose();
    }

    public void accept() {
        result = DialogResult.ACCEPTED;
        dispose();
    }

    public DialogResult getResult() {
        return result;
    }
}

