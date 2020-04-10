package studio.core;

import java.util.Properties;

public class DefaultAuthenticationMechanism implements IAuthenticationMechanism {
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String[] PROP_NAMES = new String[]{USERNAME,PASSWORD};
    private Properties props;

    public final static String NAME = "Username and password";

    public String getMechanismName() {
        return NAME;
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
