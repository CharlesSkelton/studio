package studio.ui;

import studio.kdb.Server;
import javax.swing.JFrame;

public class EditServerForm extends ServerForm {
    public EditServerForm(JFrame owner,Server server) {
        super(owner,"Edit Server Details",server);
    }
}
