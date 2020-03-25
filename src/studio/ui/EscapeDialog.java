package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EscapeDialog extends JDialog {

    public EscapeDialog(Frame owner,String title, boolean modal) {
        super(owner,title, modal);
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        this.getRootPane().registerKeyboardAction(e->dispose(),stroke,JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}

