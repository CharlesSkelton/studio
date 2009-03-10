/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JMClassLoader extends ClassLoader {
    private Set loadedClasses = Collections.synchronizedSet(new HashSet());

    public Class loadClass(String name,boolean resolve)
        throws ClassNotFoundException {
        Class clas = null;

        // If the class has already been loaded, just return it.
        clas = findLoadedClass(name);

        if (clas != null)
            return clas;

        // Try to load the class directly from disk
        try {
            // Get the raw bytes, if they are there.
            byte classBytes[] = ClassLoaderUtil.getClassBytes(name);

            // We got them!  Turn the bytes into a class.
            clas = defineClass(name,classBytes,0,classBytes.length);
        }
        catch (IOException ie) {
        }

        // We weren't able to get the class, so
        // use the default Classloader.
        if (clas == null)
            clas = Class.forName(name);

        // If we still can't find it, then it's a real
        // exception.
        if (clas == null)
            throw new ClassNotFoundException(name);

        // Resolve the class -- load all the classes
        // that this needs, and do any necessary linking.
        if (resolve)
            resolveClass(clas);

        // Record the class, so we can put it into
        // a JAR file.
        loadedClasses.add(clas.getName());

        // Return the class to the runtime system.
        return clas;
    }

    public Set getLoadedClasses() {
        return loadedClasses;
    }
}



