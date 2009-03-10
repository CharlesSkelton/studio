/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.filesystems;

import java.io.*;
import java.net.*;
import java.security.*;

/** Special URL connection directly accessing an internal file object.
*
* @author Ales Novak, Petr Hamernik, Jan Jancura, Jaroslav Tulach
*/
final class FileURL extends URLConnection {
    /** Protocol name for this type of URL. */
    public static final String PROTOCOL = "nbfs"; // NOI18N

    /** url separator */
    private static final char SEPARATOR = '/';
    
    /** Default implemenatation of handler for this type of URL.
     */
    static URLStreamHandler HANDLER = new URLStreamHandler () {
        /**
        * @param u - URL to open connection to.
        * @return new URLConnection.
        */
        public URLConnection openConnection(URL u) throws IOException {
            return new FileURL (u);
        }
    };
    

    /** FileObject that we want to connect to. */
    private FileObject fo;


    /**
    * Create a new connection to a {@link FileObject}.
    * @param u URL of the connection. Please use {@link #encodeFileObject(FileObject)} to create the URL.
    */
    private FileURL(URL u) {
        super (u);
    }

    /** Provides a URL to access a file object.
    * @param fo the file object
    * @return a URL using the correct syntax and {@link #PROTOCOL protocol}
    * @exception FileStateInvalidException if the file object is not valid (typically, if its filesystem is inconsistent or no longer present)
    */
    public static URL encodeFileObject (FileObject fo) throws FileStateInvalidException {
        return encodeFileObject (fo.getFileSystem (), fo);
    }

    /** Encodes fileobject into URL.
    * @param fs file system the object is on
    * @param fo file object
    * @return URL
    */
    private static URL encodeFileObject (FileSystem fs, FileObject fo) {
        String fsName = encodeFileSystemName (fs.getSystemName());
        String fileName = fo.getPath();
        String name = fsName + SEPARATOR + fileName;
        boolean needOfSlash = needOfSlash = (fo.isFolder() && fileName.length () != 0 && 
            fileName.charAt(fileName.length()-1) != '/');
        final String url = (needOfSlash)?(name+"/"):name;
        // #13038: the URL constructor accepting a handler is a security-sensitive
        // operation. Sometimes a user class loaded internally (customized bean...),
        // which has no privileges, needs to make and use an nbfs: URL, since this
        // may be the URL used by e.g. ClassLoader.getResource for resources.
        try {
            return (URL)AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    // #30397: the host name cannot be null
                    return new URL(PROTOCOL, "", -1, url, HANDLER); // NOI18N
                }
            });
        } catch (PrivilegedActionException pae) {
            // MalformedURLException is declared but should not happen.
            IllegalStateException ise = new IllegalStateException(pae.toString());
            ExternalUtil.annotate(ise, pae);
            throw ise;
        }
    }

    /** Retrieves the file object specified by an internal URL.
    * @param u the url to decode
    * @return the file object that is represented by the URL, or <code>null</code> if the URL is somehow invalid or the file does not exist
    */
    public static FileObject decodeURL (URL u) {
        if (!u.getProtocol ().equals (PROTOCOL)) return null;
        // resource name
        String resourceName = u.getFile ();
        if (resourceName.startsWith ("/")) resourceName = resourceName.substring (1); // NOI18N

        // first part is FS name
        int first = resourceName.indexOf ('/');
        if (first == -1) return null;

        String fileSystemName = decodeFileSystemName (resourceName.substring (0, first));
        resourceName = resourceName.substring (first);
        FileSystem fsys = ExternalUtil.getRepository ().findFileSystem(fileSystemName);

        return (fsys == null) ? null : fsys.findResource (resourceName);
    }

    /* A method for connecting to a FileObject.
    */
    public void connect() throws IOException {
        if (fo != null) return;

        fo = decodeURL (url);
        if (fo == null) {
            throw new FileNotFoundException("Cannot find: " + url); // NOI18N
        }
    }

    /*
    * @return InputStream or given FileObject.
    */
    public InputStream getInputStream()
    throws IOException, UnknownServiceException {
        connect ();
        try {
            if (fo.isFolder()) return new FIS (fo);
            return fo.getInputStream();
        } catch (FileNotFoundException e) {
            ExternalUtil.exception (e);
            throw e;
        }
    }

    /*
    * @return OutputStream for given FileObject.
    */
    public OutputStream getOutputStream()
    throws IOException, UnknownServiceException {
        connect();
        if (fo.isFolder()) throw new UnknownServiceException();
        org.openide.filesystems.FileLock flock = fo.lock();
        return new LockOS (fo.getOutputStream(flock), flock);
    }

    /*
    * @return length of FileObject.
    */
    public int getContentLength() {
        try {
            connect();
            return (int)fo.getSize();
        } catch (IOException ex) {
            return 0;
        }
    }


    /** Get a header field (currently, content type only).
    * @param name the header name. Only <code>content-type</code> is guaranteed to be present.
    * @return the value (i.e., MIME type)
    */
    public String getHeaderField(String name) {
        if (name.equalsIgnoreCase("content-type")) { // NOI18N
            try {
                connect();
                if (fo.isFolder())
                    return "text/html"; // NOI18N
                else
                    return fo.getMIMEType ();
            }
            catch (IOException e) {
            }
        }
        return super.getHeaderField(name);
    }
    
    // #13038: URLClassPath is going to check this.
    // Better not return AllPermission!
    // SocketPermission on localhost might also work.
    public Permission getPermission() throws IOException {
        // Note this is normally called by URLClassPath with an unconnected
        // URLConnection, so the fo will probably be null anyway.
        if (fo != null) {
            File f = FileUtil.toFile(fo);
            if (f != null) {
                return new FilePermission(f.getAbsolutePath(), "read"); // NOI18N
            }
            try {
                FileSystem fs = fo.getFileSystem();
                if (fs instanceof JarFileSystem) {
                    return new FilePermission(((JarFileSystem)fs).getJarFile().getAbsolutePath(), "read"); // NOI18N
                }
                // [PENDING] could do XMLFileSystem too...
            } catch (FileStateInvalidException fsie) {
                // ignore
            }
        }
        // fallback
        return new FilePermission("<<ALL FILES>>", "read"); // NOI18N
    }

    /** Encodes filesystem name.
    * @param fs original filesystem name
    * @return new encoded name
    */
    static String encodeFileSystemName (String fs) {
	// [PENDING] this ought to use standard URL encoding, not this weird scheme
        StringBuffer sb = new StringBuffer ();
        for (int i = 0; i < fs.length (); i++) {
            switch (fs.charAt (i)) {
            case 'Q':
                sb.append ("QQ"); // NOI18N
                break;
            case '/':
                sb.append ("QB"); // NOI18N
                break;
            case ':':
                sb.append ("QC"); // NOI18N
                break;
            case '\\':
                sb.append ("QD"); // NOI18N
                break;
            case '#':
                sb.append ("QE"); // NOI18N
                break;                
            default:
                sb.append (fs.charAt (i));
                break;
            }
        }
        return sb.toString ();
    }

    /** Decodes name to FS one.
    * @param name encoded name
    * @return original name of the filesystem
    */
    static String decodeFileSystemName (String name) {
        StringBuffer sb = new StringBuffer ();
        int i = 0;
        int len = name.length ();
        while (i < len) {
            char ch = name.charAt (i++);
            if (ch == 'Q' && i < len) {
                switch (name.charAt (i++)) {
                case 'B':
                    sb.append ('/');
                    break;
                case 'C':
                    sb.append (':');
                    break;
                case 'D':
                    sb.append ('\\');
                    break;
                case 'E':
                    sb.append ('#');
                    break;                    
                default:
                    sb.append ('Q');
                    break;
                }
            } else {
                // not Q
                sb.append (ch);
            }
        }
        return sb.toString ();
    }


    /** Stream that also closes the lock, if closed.
     */
    private static class LockOS extends java.io.BufferedOutputStream {
        /** lock */
        private FileLock flock;

        /**
        * @param os is an OutputStream for writing in
        * @param lock is a lock for the stream
        */
        public LockOS (OutputStream os, FileLock lock)
        throws IOException {
            super(os);
            flock = lock;
        }

        /** overriden */
        public void close()
        throws IOException {
            flock.releaseLock();
            super.close();
        }
    }
    
    /** The class allows reading of folder via URL. Because of html
    * oriented user interface the document has html format.
    *
    * @author Ales Novak
    * @version 0.10 May 15, 1998
    */
    private static final class FIS extends InputStream {

        /** delegated reader that reads the document */
        private StringReader reader;

        /**
        * @param folder is a folder
        */
        public FIS (FileObject folder)
        throws IOException {
            reader = new StringReader(createDocument(folder));
        }

        /** creates html document as string */
        private String createDocument(FileObject folder)
        throws IOException {
            StringBuffer buff = new StringBuffer(150);
            StringBuffer lit = new StringBuffer(15);
            FileObject[] fobia = folder.getChildren();
            String name;

            buff.append("<HTML>\n"); // NOI18N
            buff.append("<BODY>\n"); // NOI18N

            FileObject parent = folder.getParent();
            if (parent != null) {
                // lit.setLength(0);
                // lit.append('/').append(parent.getPackageName('/'));
                buff.append("<P>"); // NOI18N
                buff.append("<A HREF=").append("..").append(">").append("..").append("</A>").append("\n"); // NOI18N
                buff.append("</P>"); // NOI18N
            }

            for (int i = 0; i < fobia.length; i++) {
                lit.setLength(0);
                lit.append(fobia[i].getNameExt());
                name = lit.toString ();
                if (fobia[i].isFolder()) {
                    lit.append('/'); // NOI18N
                }
                buff.append("<P>"); // NOI18N
                buff.append("<A HREF=").append((Object)lit).append(">").append(name).append("</A>").append("\n"); // NOI18N
                buff.append("</P>"); // NOI18N
            }

            buff.append("</BODY>\n"); // NOI18N
            buff.append("</HTML>\n"); // NOI18N
            return buff.toString();
        }

        //************************************** stream methods **********
        public int read() throws IOException {
            return reader.read();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            char[] ch = new char[len];
            int r = reader.read(ch, 0, len);
            for (int i = 0; i < r; i++)
                b[off + i] = (byte) ch[i];
            return r;
        }

        public long skip(long skip) throws IOException {
            return reader.skip(skip);
        }

        public void close() throws IOException {
            reader.close();
        }

        public void reset() throws IOException {
            reader.reset();
        }

        public boolean markSupported() {
            return false;
        }
    } // end of FIS
    
}

