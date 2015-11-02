package studio.ui;

import javax.swing.*;

public class TabPanel extends JPanel {
    Icon _icon;
    String _title;
    JComponent _component;

    public TabPanel(String title,Icon icon,JComponent component) {
        _title = title;
        _icon = icon;
        _component = component;
    }

    public Icon getIcon() {
        return _icon;
    }

    public void setIcon(Icon icon) {
        _icon = icon;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public JComponent getComponent() {
        return _component;
    }

    public void setComponent(JComponent component) {
        _component = component;
    }
}

