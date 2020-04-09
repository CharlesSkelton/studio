package studio.kdb;

import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.IOException;
import kx.c.K4Exception;

public class ConnectionPool {
    private static ConnectionPool instance;
    private Map freeMap = new HashMap();
    private Map busyMap = new HashMap();

    private ConnectionPool() {
    }

    public synchronized void purge(Server s) {
        List list = (List) freeMap.get(s.toString());

        if (list != null) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                kx.c c = (kx.c) i.next();
                c.close();
            }
        }

        busyMap.put(s.toString(),new LinkedList());

        if (list != null)
            list.clear();

    //    primeConnectionPool();
    }

    public static synchronized ConnectionPool getInstance() {
        if (instance == null)
            instance = new ConnectionPool();

        return instance;
    }

    public synchronized kx.c leaseConnection(Server s) // throws IOException, c.K4Exception
    {
        kx.c c = null;

        List list = (List) freeMap.get(s.toString());
        List dead = new LinkedList();

        if (list != null) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                c = (kx.c) i.next();

                if (c.isClosed()) {
//                    i.remove();
                    dead.add(c);
                    c = null;
                }
                else
                    break;
            }
        }
        else {
            list = new LinkedList();
            freeMap.put(s.toString(),list);
        }

        list.removeAll(dead);

        if (c == null)
            try {
                Class clazz = AuthenticationManager.getInstance().lookup(s.getAuthenticationMechanism());
                if (clazz == null) {
                }
                IAuthenticationMechanism authenticationMechanism = (IAuthenticationMechanism) clazz.newInstance();

                authenticationMechanism.setProperties(s.getAsProperties());
                Credentials credentials = authenticationMechanism.getCredentials();
                if (credentials.getUsername().length() > 0) {
                    String p = credentials.getPassword();

                    c = new kx.c(s.getHost(),s.getPort(),credentials.getUsername() + ((p.length() == 0) ? "" : ":" + p),s.getUseTLS());
                }
                else
                    c = new kx.c(s.getHost(),s.getPort(),"",s.getUseTLS());
            }
            catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
            }
        else
            list.remove(c);

        list = (List) busyMap.get(s.toString());
        if (list == null) {
            list = new LinkedList();
            busyMap.put(s.toString(),list);
        }

        list.add(c);

        return c;
    }

    public synchronized void freeConnection(Server s,kx.c c) {
        if (c == null)
            return;

        List list = (List) busyMap.get(s.toString());

        // If c not in our busy list it has been purged, so close it
        if (list != null)
            if (!list.remove(c))
                c.close();

        if (!c.isClosed()) {
            list = (List) freeMap.get(s.toString());
            if (list == null)
                c.close();
            else
                list.add(c);
        }
    }

    public void checkConnected(kx.c c) throws IOException,K4Exception {
        if (c.isClosed())
            c.reconnect(true);
    }
}
