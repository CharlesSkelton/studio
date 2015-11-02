package studio.core;

import java.util.Properties;

public interface IAuthenticationMechanism {
    public String getMechanismName();

    public String[] getMechanismPropertyNames();

    public void setProperties(Properties props);

    public Credentials getCredentials();
}
