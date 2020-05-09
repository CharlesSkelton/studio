package studio.kdb;

import studio.core.Credentials;

import java.awt.Color;
import java.util.Objects;
import java.util.Properties;

public class Server {
    private String authenticationMechanism;
    private Color backgroundColor = Color.white;
    private String name = "";
    private String host = "";
    private int port = 0;
    private String username;
    private String password;
    private boolean useTLS = false;
    private ServerTreeNode folder = null;

    public Properties getAsProperties() {
        Properties p = new Properties();
        p.put("NAME", name);
        p.put("HOST", host);
        p.put("PORT", port);
        p.put("USERNAME", username);
        p.put("PASSWORD", password);
        p.put("USETLS", useTLS);
        return p;
    }

    public String getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public void setAuthenticationMechanism(String authenticationMechanism) {
        this.authenticationMechanism = authenticationMechanism;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color c) {
        backgroundColor = c;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }

    public Server() {
        authenticationMechanism = Config.getInstance().getDefaultAuthMechanism();
        Credentials credentials = Config.getInstance().getDefaultCredentials(authenticationMechanism);
        username = credentials.getUsername();
        password = credentials.getPassword();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Server)) return false;
        Server s = (Server) obj;
        return s.name.equals(name)
                && Objects.equals(s.host, host)
                && s.port == port
                && Objects.equals(s.username, username)
                && Objects.equals(s.password, password)
                && Objects.equals(s.authenticationMechanism ,authenticationMechanism)
                && s.useTLS == useTLS;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Server(Server s) {
        this.name = s.name;
        this.host = s.host;
        this.port = s.port;
        this.username = s.username;
        this.password = s.password;
        this.backgroundColor = s.backgroundColor;
        this.authenticationMechanism = s.authenticationMechanism;
        this.useTLS = s.useTLS;
    }

    public Server(String name, String host, int port, String username, String password, Color backgroundColor, String authenticationMechanism, boolean useTLS) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.backgroundColor = backgroundColor;
        this.authenticationMechanism = authenticationMechanism;
        this.useTLS = useTLS;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (folder == null) return name;
        String path = folder.fullPath();
        if (path.length() == 0) return name;
        return path + "/" + name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return getFullName();
    }

    public String getConnectionString(boolean includeCreditional) {
        String connection = "`:" + host + ":" + port;
        if (!includeCreditional) return connection;

        return connection + ":" + username + ":" + password;

    }

    public String getDescription(boolean fullName) {
        return (fullName ? getFullName() : name) + " (" + host + ":" + port + ")";
    }

    public boolean getUseTLS(){
      return useTLS;
    }

    public ServerTreeNode getFolder() {
        return folder;
    }

    public void setFolder(ServerTreeNode folder) {
        this.folder = folder;
    }

}
