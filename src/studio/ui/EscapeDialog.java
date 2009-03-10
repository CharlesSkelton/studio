/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EscapeDialog extends JDialog {
    public EscapeDialog() {
        this((Frame) null,false);
    }

    public EscapeDialog(Frame owner) {
        this(owner,false);
    }

    public EscapeDialog(Frame owner,boolean modal) {
        this(owner,null,modal);
    }

    public EscapeDialog(Frame owner,String title) {
        this(owner,title,false);
    }

    public EscapeDialog(Frame owner,String title,boolean modal) {
        super(owner,title,modal);
    }

    public EscapeDialog(Dialog owner) {
        this(owner,false);
    }

    public EscapeDialog(Dialog owner,boolean modal) {
        this(owner,null,modal);
    }

    public EscapeDialog(Dialog owner,String title) {
        this(owner,title,false);
    }

    public EscapeDialog(Dialog owner,String title,boolean modal) {
        super(owner,title,modal);
    }

    protected void closeAttempt() {
        setVisible(false);
    }

    protected JRootPane createRootPane() {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                closeAttempt();
            }
        };
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0);
        rootPane.registerKeyboardAction(actionListener,stroke,JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }
}

