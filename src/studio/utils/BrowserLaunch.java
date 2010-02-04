/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.utils;

import java.lang.reflect.Method;
import javax.swing.JOptionPane;

public class BrowserLaunch {
    private static final String errMsg = "Error attempting to launch web browser";

    public static void openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                                                           new Class[]{String.class});
                openURL.invoke(null,new Object[]{url});
            }
            else if (osName.startsWith("Windows"))
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            else { //assume Unix or Linux
                String[] browsers = {
                    "firefox","opera","konqueror","epiphany","mozilla","netscape"
                };
                String browser = null;
                for (int count = 0;count < browsers.length && browser == null;count++)
                    if (Runtime.getRuntime().exec(
                        new String[]{"which",browsers[count]}).waitFor() == 0)
                        browser = browsers[count];
                if (browser == null)
                    throw new Exception("Could not find web browser");
                else
                    Runtime.getRuntime().exec(new String[]{browser,url});
            }
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(null,errMsg + ":\n" + e.getLocalizedMessage());
        }
    }
}
