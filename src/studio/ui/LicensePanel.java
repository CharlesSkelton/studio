/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Dimension;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class LicensePanel extends JPanel {
    public LicensePanel() {

        super();
        setLayout(new FormLayout("max(default;350dlu):grow",
                                 "fill:default:grow"));

        try {
            CellConstraints cc = new CellConstraints();
            URL url = getClass().getResource("/de/licenses/eaplicense.html");
            JEditorPane textArea = new JEditorPane(url);
            textArea.setEditable(false);
            textArea.setPreferredSize(new Dimension(350,300));
            JScrollPane sp = new JScrollPane(textArea);
            add(sp,cc.xy(1,1));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}