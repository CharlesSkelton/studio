/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/
package studio.kdb;

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

    public Properties getAsProperties() {
        Properties p = new Properties();
        p.put("NAME",name);
        p.put("HOST",host);
        p.put("PORT",new Integer(port));
        p.put("USERNAME",username);
        p.put("PASSWORD",password);
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

    public Server() {
    }

    public Server(Server s) {
        this.name = s.name;
        this.host = s.host;
        this.port = s.port;
        this.username = s.username;
        this.password = s.password;
        this.backgroundColor = s.backgroundColor;
        this.authenticationMechanism = s.authenticationMechanism;
    }

    public Server(String name,String host,int port,String username,String password,Color backgroundColor,String authenticationMechanism) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.backgroundColor = backgroundColor;
        this.authenticationMechanism = authenticationMechanism;
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
}
