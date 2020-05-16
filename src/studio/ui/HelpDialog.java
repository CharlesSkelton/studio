package studio.ui;

import studio.kdb.Lm;
import studio.utils.BrowserLaunch;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class HelpDialog extends JDialog {
    public HelpDialog(JFrame parent) {
        super(parent, "Studio for kdb+");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        final JEditorPane jep = new JEditorPane("text/html",
                "<html><head><title>Studio for kdb+</title></head><body><h1>Studio for kdb+</h1>"
                        + "<br>Version: " + Lm.getVersionString()
                        + "<br>Build date: " + f.format(Lm.buildDate)
                        + "<br>JVM Version: " + System.getProperty("java.version")
                        + "<br>License: <a href=\"http://github.com/CharlesSkelton/studio/blob/master/license.md\">Apache 2</a>"
                        + "<br>N.B. Some components have their own license terms, see this project on github for details."
                        + "<br>Source available from <a href=\"http://github.com/CharlesSkelton/studio\">Github</a>"
                        + "<br>Contributions and corrections welcome."
                        + "</body></html>");
        jep.setEditable(false);
        jep.setOpaque(true);
        jep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType()))
                    BrowserLaunch.openURL(hle.getURL().toString());
            }
        });
        getContentPane().add(jep);
        JPanel buttonPane = new JPanel();
        JButton button = new JButton("Close");
        buttonPane.add(button);
        button.addActionListener(new CloseActionListener());
        getContentPane().add(buttonPane, BorderLayout.PAGE_END);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public void setStartLocation(Component parent) {
        Point parentlocation = parent.getLocation();
        Dimension oursize = getPreferredSize();
        Dimension parentsize = parent.getSize();
        int x = parentlocation.x + (parentsize.width - oursize.width) / 2;
        int y = parentlocation.y + (parentsize.height - oursize.height) / 2;
        x = Math.max(0, x);
        y = Math.max(0, y);
        setLocation(x, y);
    }

    @Override
    public JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", action);
        return rootPane;
    }

    class CloseActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            dispose();
        }
    }
}
