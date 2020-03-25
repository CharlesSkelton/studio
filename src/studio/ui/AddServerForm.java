package studio.ui;

import studio.kdb.Server;

import javax.swing.JFrame;

public class AddServerForm extends ServerForm {
    public AddServerForm(JFrame owner) {
        super(owner, "Add a new server", new Server());
    }
}
