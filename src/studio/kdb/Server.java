package studio.kdb;

import studio.core.Credentials;

import java.awt.Color;
import java.util.Properties;

public class Server {
    private String authenticationMechanism = "";
    private Color backgroundColor = Color.white;
    private String name = "";
    private String host = "";
    private int port = 0;
    private String username = "";
    private String password = "";
    private boolean useTLS=false;

    public Properties getAsProperties() {
        Properties p = new Properties();
        p.put("NAME",name);
        p.put("HOST",host);
        p.put("PORT",new Integer(port));
        p.put("USERNAME",username);
        p.put("PASSWORD",password);
        p.put("USETLS",useTLS);
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
    public void setUseTLS(boolean useTLS){
      this.useTLS=useTLS;
    }

    public Server() {
        authenticationMechanism = Config.getInstance().getDefaultAuthMechanism();
        Credentials credentials = Config.getInstance().getDefaultCredentials(authenticationMechanism);
        username = credentials.getUsername();
        password = credentials.getPassword();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Server)) return false;
        return ((Server) obj).getName().equals(getName());
    }

    public Server(Server s) {
        this.name = s.name;
        this.host = s.host;
        this.port = s.port;
        this.username = s.username;
        this.password = s.password;
        this.backgroundColor = s.backgroundColor;
        this.authenticationMechanism = s.authenticationMechanism;
        this.useTLS=s.useTLS;
    }

    public Server(String name,String host,int port,String username,String password,Color backgroundColor,String authenticationMechanism,boolean useTLS) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.backgroundColor = backgroundColor;
        this.authenticationMechanism = authenticationMechanism;
        this.useTLS=useTLS;
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

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return name;
    }
    public boolean getUseTLS(){
      return useTLS;
    }
}
