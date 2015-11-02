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
