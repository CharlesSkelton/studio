/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

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

