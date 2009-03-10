/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.core;

import studio.kdb.Config;
import studio.kdb.Lm;
import studio.ui.ExceptionGroup;
import studio.ui.LicensePanel;
import studio.ui.Studio;

import java.util.TimeZone;
import javax.swing.JOptionPane;

public class EntryPoint {
    public static void main(String[] args) {
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
//otherwise input has been canceled
     /*   
        LicenseInformationDialog license = new LicenseInformationDialog(null);
        // license.setStartLocation(MainFrame.this);
        license.setTitle("Studio for kdb+ :: License information");
        license.setModal(true);
        license.pack();
        license.show();
         */

        ThreadGroup exceptionThreadGroup = new ExceptionGroup();

        new Thread(exceptionThreadGroup,"Init thread") {
            public void run() {
                Studio.init();
            }
        }.start();


    /*        final UncaughtExceptionHandler eh=new UncaughtExceptionHandler()
    {
    public void uncaughtException(Thread t,Throwable e)
    {
    CharArrayWriter caw=new CharArrayWriter();

    e.printStackTrace(new PrintWriter(caw));

    JOptionPane.showMessageDialog(null,
    "An uncaught exception occurred\n\nDetails - \n\n"+caw.toString(),
    "Studio for kdb+",
    JOptionPane.ERROR_MESSAGE);
    }
    };



    Runnable runner=new Runnable()
    {
    public void run()
    {
    Thread.setDefaultUncaughtExceptionHandler(eh);
    Studio.init();
    }
    };
    try
    {
    SwingUtilities.invokeAndWait(runner);
    }
    catch(InterruptedException ex)
    {
    ex.printStackTrace();
    }
    catch(InvocationTargetException ex)
    {
    ex.printStackTrace();
    }
     */
    }
}
