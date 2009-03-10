/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/
package studio.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AuthenticationManager {
    private static AuthenticationManager instance;
    private Map classMap = new HashMap();

    public Class lookup(String autheticationMethod) {
        return (Class) classMap.get(autheticationMethod);
    }

    public String[] getAuthenticationMechanisms() {
        Set s = classMap.keySet();
        return (String[]) s.toArray(new String[0]);
    }

    public synchronized static AuthenticationManager getInstance() throws IOException,ClassNotFoundException,InstantiationException,IllegalAccessException,NoSuchMethodException,IllegalArgumentException,InvocationTargetException {
        if (instance == null)
            instance = new AuthenticationManager();

        /*      String [] x=instance.getAuthenticationMechanisms();
        for(int i= 0; i <x.length;i++)
        System.out.println(x[i]);
         */ return instance;
    }

    private AuthenticationManager() throws IOException,ClassNotFoundException,InstantiationException,IllegalAccessException,NoSuchMethodException,IllegalArgumentException,InvocationTargetException {
        DefaultAuthenticationMechanism dam = new DefaultAuthenticationMechanism();
        classMap.put(dam.getMechanismName(),dam.getClass());

        String curDir = System.getProperty("user.dir");
        curDir = curDir + "/plugins";

        //   System.out.println("Looking for plugins at " + curDir);

        File dir = new File(curDir);
        if (!dir.exists())
            return;

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir,String name) {
                return name.endsWith(".jar");
            }
        };

        String[] children = dir.list(filter);
        if (children != null)
            for (int child = 0;child < children.length;child++) {
                String filename = dir.getAbsolutePath() + "/" + children[child];

                URL url = new URL("jar:file:" + filename + "/!/");
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jarFile = conn.getJarFile();

                Enumeration e = jarFile.entries();
                while (e.hasMoreElements()) {
                    JarEntry entry = (JarEntry) e.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.endsWith(".class")) {
                        URLClassLoader loader = new URLClassLoader(new URL[]{url});
                        String externalName = name.substring(0,name.indexOf('.')).replace('/','.');
                        Class c = loader.loadClass(externalName);
                        Class[] interfaces = c.getInterfaces();
                        for (int i = 0;i < interfaces.length;i++)
                            if (IAuthenticationMechanism.class == interfaces[0]) {
                                Method addURL = URLClassLoader.class.getDeclaredMethod("addURL",new Class[]{URL.class});
                                addURL.setAccessible(true);
                                ClassLoader cl = ClassLoader.getSystemClassLoader();
                                addURL.invoke(cl,new Object[]{url});
                                IAuthenticationMechanism am = (IAuthenticationMechanism) c.newInstance();
                                classMap.put(am.getMechanismName(),c);
                            }
                    }
                }
            }
    }
}