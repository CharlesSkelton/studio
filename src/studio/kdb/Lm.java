/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.kdb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.TimeZone;

public class Lm {
    private static int majorVersion = 3;
    private static int minorVersion = 25;
    public static Date buildDate;
    
    static {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            buildDate = f.parse("20100131");
        }
        catch (ParseException e) {
        }
    }

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static String getVersionString() {
        NumberFormat numberFormatter = new DecimalFormat("##.00");
        double d = getMajorVersion() + getMinorVersion() / 100.0;
        return numberFormatter.format(d);
    }
}
