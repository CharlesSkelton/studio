/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.utils;

import java.io.*;
import java.util.Properties;

public class ClassLoaderUtil {
    static protected String classToPath(String name) {
        Properties properties = System.getProperties();
        String fileSeparator = properties.getProperty("file.separator");
        char fsc = fileSeparator.charAt(0);
        String path = name.replace('.',fsc);
        path += ".class";
        return path;
    }

    static protected byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        long len = file.length();
        byte data[] = new byte[(int) len];
        FileInputStream fin = new FileInputStream(file);
        int r = fin.read(data);
        if (r != len)
            throw new IOException("Only read " + r + " of " + len + " for " + file);
        fin.close();
        return data;
    }

    static protected byte[] getClassBytes(String name) throws IOException {
        String path = classToPath(name);
        return readFile(path);
    }

    static protected void copyFile(OutputStream out,InputStream in)
        throws IOException {
        byte buffer[] = new byte[4096];

        while (true) {
            int r = in.read(buffer);
            if (r <= 0)
                break;
            out.write(buffer,0,r);
        }
    }

    static protected void copyFile(OutputStream out,String infile)
        throws IOException {
        FileInputStream fin = new FileInputStream(infile);
        copyFile(out,fin);
        fin.close();
    }
}




