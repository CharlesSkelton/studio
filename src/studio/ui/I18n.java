package studio.ui;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static ResourceBundle messages;
  //  static{setLocale("en","US");}
    public static String getString(String key){return messages.getString(key);}
    public static void setLocale(Locale locale){
        messages= ResourceBundle.getBundle("studio",locale);
    }
}
