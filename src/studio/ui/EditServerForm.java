/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import studio.kdb.Server;
import javax.swing.JFrame;

public class EditServerForm extends ServerForm {
    public EditServerForm(JFrame owner,Server server) {
        super(owner,server);
        this.setTitle("Edit Server Details");
    }
}
