package studio.ui;

import studio.kdb.Server;

import java.awt.*;

public class AddServerForm extends ServerForm {
    public AddServerForm(Window owner) {
        super(owner, "Add a new server", new Server());
    }
}
