/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.core;

import java.awt.Font;
import java.util.Locale;
import studio.kdb.Config;
import studio.kdb.Lm;
import studio.ui.ExceptionGroup;
import studio.ui.Studio;

import java.util.TimeZone;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleConstants;
import studio.ui.LicensePanel;

public class EntryPoint {
    public static void main(final String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        if (System.getProperty("mrj.version") != null) {
            System.setProperty("apple.laf.useScreenMenuBar","true");
            //     System.setProperty("apple.awt.brushMetalLook", "true");
            System.setProperty("apple.awt.showGrowBox","true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name","Studio for kdb+");
            System.setProperty("com.apple.mrj.application.live-resize","true");
            System.setProperty("com.apple.macos.smallTabs","true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
        }

        if(Config.getInstance().getLookAndFeel()!=null){
            try {
                UIManager.setLookAndFeel(Config.getInstance().getLookAndFeel());
            } catch (Exception ex) {
                // go on with default one
                ex.printStackTrace();
            }
        }

        studio.ui.I18n.setLocale(Locale.getDefault());

     //   studio.ui.I18n.setLocale(new Locale("zh", "cn"));

        if (!Config.getInstance().getAcceptedLicense()) {
            LicensePanel panel = new LicensePanel();
            Object[] options = new String[]{
                "Accept","Do Not Accept"
            };
            int answer = JOptionPane.showOptionDialog(null,
                                                      panel,"Studio for kdb+",
                                                      JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      Studio.getImage(Config.imageBase + "32x32/question.png"), //do not use a custom Icon
                                                      options, //the titles of buttons
                                                      options[1]); //default button title

            if (answer == JOptionPane.NO_OPTION)
                System.exit(0);

            Config.getInstance().setAcceptedLicense(Lm.buildDate);
        }
        
        UIManager.put("Table.font",new javax.swing.plaf.FontUIResource("Monospaced",Font.PLAIN,UIManager.getFont("Table.font").getSize()));
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
     
        ThreadGroup exceptionThreadGroup = new ExceptionGroup();

        new Thread(exceptionThreadGroup,"Init thread") {
            public void run() {
                Studio.init(args);
            }
        }.start();

    }
}
