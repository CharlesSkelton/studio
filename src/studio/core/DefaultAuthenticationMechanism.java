/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.core;

import java.util.Properties;

public class DefaultAuthenticationMechanism implements IAuthenticationMechanism {
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String[] PROP_NAMES = new String[]{USERNAME,PASSWORD};
    private Properties props;

    public String getMechanismName() {
        return "Username and password";
    }

    public String[] getMechanismPropertyNames() {
        return PROP_NAMES;
    }

    public void setProperties(Properties props) {
        // ignore host and port
        this.props = props;
    }

    public Credentials getCredentials() {
        return new Credentials((String) props.get(USERNAME),(String) props.get(PASSWORD));
    }
}