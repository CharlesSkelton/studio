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
import java.net.URLStreamHandler;
import java.util.*;
import java.util.jar.*;

import org.openide.util.Utilities;

/** Common utilities for handling files.
 * This is a dummy class; all methods are static.
*
* @author Petr Hamernik
*/
public final class FileUtil extends Object {
    private FileUtil() {}

    /** Copies stream of files.
    * <P> 
    * Please be aware, that this method doesn't close any of passed streams.
    * @param is input stream
    * @param os output stream
    */
    public static void copy (InputStream is, OutputStream os) throws IOException {
        final byte[] BUFFER = new byte[4096];
        int len;

        for (;;) {
            len = is.read (BUFFER);
            if (len == -1) return;
            os.write (BUFFER, 0, len);
        }
    }

    /** Copies file to the selected folder.
     * This implementation simply copies the file by stream content.
    * @param source source file object
    * @param destFolder destination folder
    * @param newName file name (without extension) of destination file
    * @param newExt extension of destination file
    * @return the created file object in the destination folder
    * @exception IOException if <code>destFolder</code> is not a folder or does not exist; the destination file already exists; or
    *      another critical error occurs during copying
    */
    static FileObject copyFileImpl (
        FileObject source, FileObject destFolder, String newName, String newExt
    ) throws IOException {
        FileObject dest = destFolder.createData(newName, newExt);

        FileLock lock = null;
        InputStream bufIn = null;
        OutputStream bufOut = null;
        try {
            lock = dest.lock();
            bufIn = source.getInputStream();
            
            if (dest instanceof AbstractFileObject)
                /** prevents from firing fileChange*/
                bufOut = ((AbstractFileObject)dest).getOutputStream(lock, false);
            else
                bufOut = dest.getOutputStream(lock);

            copy (bufIn, bufOut);
            copyAttributes (source, dest);

        }
        finally {
            if (bufIn != null)
                bufIn.close();
            if (bufOut != null)
                bufOut.close();

            if (lock != null)
                lock.releaseLock();
        }

        return dest;
    }


    //
    // public methods
    //


    /** Copies file to the selected folder.
    * This implementation simply copies the file by stream content.
    * @param source source file object
    * @param destFolder destination folder
    * @param newName file name (without extension) of destination file
    * @param newExt extension of destination file
    * @return the created file object in the destination folder
    * @exception IOException if <code>destFolder</code> is not a folder or does not exist; the destination file already exists; or
    *      another critical error occurs during copying
    */
    public static FileObject copyFile(FileObject source, FileObject destFolder,
                                      String newName, String newExt) throws IOException {
        return source.copy (destFolder, newName, newExt);
    }

    /** Copies file to the selected folder.
    * This implementation simply copies the file by stream content.
    * Uses the extension of the source file.
    * @param source source file object
    * @param destFolder destination folder
    * @param newName file name (without extension) of destination file
    * @return the created file object in the destination folder
    * @exception IOException if <code>destFolder</code> is not a folder or does not exist; the destination file already exists; or
    *      another critical error occurs during copying
    */
    public static FileObject copyFile(FileObject source, FileObject destFolder,
                                      String newName) throws IOException {
        return copyFile(source, destFolder, newName, source.getExt());
    }

    /** Moves file to the selected folder.
     * This implementation uses a copy-and-delete mechanism, and automatically uses the necessary lock.
    * @param source source file object
    * @param destFolder destination folder
    * @param newName file name (without extension) of destination file
    * @return new file object
    * @exception IOException if either the {@link #copyFile copy} or {@link FileObject#delete delete} failed
    */
    public static FileObject moveFile(FileObject source, FileObject destFolder,
                                      String newName) throws IOException {
        FileLock lock = null;
        try {
            lock = source.lock();
            return source.move (lock, destFolder, newName, source.getExt ());
        }
        finally {
            if (lock != null)
                lock.releaseLock();
        }
    }

    /**
     * Creates a folder on given filesystem.  The name of the new folder can be
     * specified as a multi-component pathname whose components are separated
     * by File.separatorChar or &quot;/&quot; (forward slash).
     *
     * @param folder where the new folder will be placed in
     * @param name name of the new folder
     * @return the new folder
     * @exception IOException if the creation fails
     */
    public static FileObject createFolder (FileObject folder, String name)
    throws IOException {
        String separators;
        if (File.separatorChar != '/')
            separators = "/" + File.separatorChar; // NOI18N
        else
            separators = "/";   // NOI18N
        
        StringTokenizer st = new StringTokenizer (name, separators);
        while (st.hasMoreElements ()) {
            name = st.nextToken ();
            if (name.length () > 0) {
                FileObject f = folder.getFileObject (name);
                if (f == null) {
                    try {
                        f = folder.createFolder (name);
                    } catch (SyncFailedException ex) {
                        // there might be unconsistency between the cache
                        // and the disk, that is why
                        folder.refresh();
                        // and try again
                        f = folder.getFileObject (name);
                        if (f == null) {
                            // if still not found than we have to report the
                            // exception
                            throw ex;
                        }
                    }
                }
                folder = f;
            }
        }
        return folder;
    }

    /** Creates a data file on given filesystem. The name of
    * data file can be composed as resource name (e. g. org/netbeans/myfolder/mydata )
    * and the method scans which of folders has already been created 
    * and which not. 
    *
    * @param folder to begin with creation at
    * @param name name of data file as a resource
    * @return the data file for given name
    * @exception IOException if the creation fails
    */
    public static FileObject createData (FileObject folder, String name)
    throws IOException {
        String foldername, dataname, fname, ext;
        int index = name.lastIndexOf('/');
        FileObject data;

        // names with '/' on the end are not valid
        if (index >= name.length()) throw new IOException("Wrong file name."); // NOI18N

        // if name contains '/', create necessary folder first
        if (index != -1) {
            foldername = name.substring(0, index);
            dataname = name.substring(index + 1);
            folder = createFolder(folder, foldername);
        } else {
            dataname = name;
        }

        // create data
        index = dataname.lastIndexOf('.');
        if (index != -1) {
            fname = dataname.substring(0, index);
            ext = dataname.substring(index + 1);
        } else {
            fname = dataname;
            ext = ""; // NOI18N
        }

        data = folder.getFileObject (fname, ext);
        if (data == null) {
            try {
                data = folder.createData(fname, ext);
            } catch (SyncFailedException ex) {
                // there might be unconsistency between the cache
                // and the disk, that is why
                folder.refresh();
                // and try again
                data = folder.getFileObject (fname, ext);
                if (data == null) {
                    // if still not found than we have to report the
                    // exception
                    throw ex;
                }
            }
        }
        return data;
    }

    /** Finds appropriate java.io.File to FileObject if possible. 
     * If not possible then null is returned.
     * @param fo FileObject whose coresponding File will be looked for
     * @return java.io.File or null if no corresponding File exists.
     * @since 1.29
     */    
    public static java.io.File toFile (FileObject fo) {
        return (java.io.File)fo.getAttribute ("java.io.File");// NOI18N
    }

    /** Finds appropriate FileObjects to java.io.File if possible. 
     * If not possible then empty array is returned. More FileObjects may 
     * correspond to one java.io.File that`s why array is returned.
     * @param file File whose coresponding FileObjects will be looked for
     * @return corresponding FileObjects or empty array  if no 
     * corresponding FileObject exists.
     * @since 1.29
     */        
    public static FileObject[] fromFile (File file) {
        Enumeration en = Repository.getDefault().getFileSystems();        
        ArrayList list = new ArrayList ();
        String fileName = null;
        
        try {
            file = file.getCanonicalFile();
            fileName = file.getCanonicalPath();
        } catch (IOException iex) {
            return new FileObject[] {};
        }
        
        while (en.hasMoreElements()) {
            FileSystem fs = (FileSystem)en.nextElement();
            try {   
                String rootName = null;
                FileObject fsRoot = fs.getRoot ();                                
                File root = toFile (fsRoot);
                if (root == null) {
                    Object rootPath = fsRoot.getAttribute("FileSystem.rootPath");//NOI18N
                    if (rootPath != null && (rootPath instanceof String))
                        rootName = (String)rootPath;
                    else     
                        continue;          
                }
                if (rootName == null)
                    rootName = root.getCanonicalPath();
                    
                /**root is parent of file*/
                if (fileName.indexOf(rootName) == 0) {
                    String res = fileName.substring(rootName.length()).replace(File.separatorChar, '/');
                    FileObject fo = fs.findResource(res);
                    File       file2Fo = (fo != null)? toFile(fo) : null;
                    if (fo != null && file2Fo != null && 
                    file.equals(file2Fo.getCanonicalFile()))
                        list.add(fo);                    
                }                        
            } catch (IOException iexc) {
                // catch for getCanonical..
                continue;
            }
        }        
        FileObject[] results = new FileObject[list.size()];
        list.toArray(results);
        return results;            
    }


    
    
    /** transient attributes which should not be copied
    * of type Set<String>
    */
    static final Set transientAttributes = new HashSet ();
    static {
        transientAttributes.add ("templateWizardURL"); // NOI18N
        transientAttributes.add ("templateWizardIterator"); // NOI18N
        transientAttributes.add ("templateWizardDescResource"); // NOI18N
        transientAttributes.add ("SystemFileSystem.localizingBundle"); // NOI18N
        transientAttributes.add ("SystemFileSystem.icon"); // NOI18N
        transientAttributes.add ("SystemFileSystem.icon32"); // NOI18N
    }
    /** Copies attributes from one file to another.
    * Note: several special attributes will not be copied, as they should
    * semantically be transient. These include attributes used by the
    * template wizard (but not the template atttribute itself).
    * @param source source file object
    * @param dest destination file object
    * @exception IOException if the copying failed
    */
    public static void copyAttributes (FileObject source, FileObject dest) throws IOException {
        Enumeration attrKeys = source.getAttributes();
        while (attrKeys.hasMoreElements()) {
            String key = (String) attrKeys.nextElement();
            if (transientAttributes.contains (key)) continue;            
            if (isTransient (source, key)) continue;            
            Object value = source.getAttribute(key);
            if (value != null) {
                dest.setAttribute(key, value);
            }
        }
    }

    static boolean isTransient (FileObject fo, String attrName) {
        return XMLMapAttr.ModifiedAttribute.isTransient (fo, attrName);
    }
    
    /** Extract jar file into folder represented by file object. If the JAR contains
    * files with name filesystem.attributes, it is assumed that these files 
    * has been created by DefaultAttributes implementation and the content
    * of these files is treated as attributes and added to extracted files.
    * <p><code>META-INF/</code> directories are skipped over.
    *
    * @param fo file object of destination folder
    * @param is input stream of jar file
    * @exception IOException if the extraction fails
    * @deprecated Use of XML filesystem layers generally obsoletes this method.
    */
    public static void extractJar (final FileObject fo, final InputStream is) throws IOException {
        FileSystem fs = fo.getFileSystem();

        fs.runAtomicAction (new FileSystem.AtomicAction () {
		public void run () throws IOException {
		    extractJarImpl (fo, is);
		}
	    });
    }

    /** Does the actual extraction of the Jar file.
     */
    private static void extractJarImpl (FileObject fo, InputStream is) throws IOException {
        JarInputStream jis;
        JarEntry je;

        // files with extended attributes (name, DefaultAttributes.Table)
        HashMap attributes = new HashMap (7);
	
        jis = new JarInputStream(is);

        while ((je = jis.getNextJarEntry()) != null) {
            String name = je.getName();
            if (name.toLowerCase ().startsWith ("meta-inf/")) continue; // NOI18N

            if (je.isDirectory ()) {
                createFolder (fo, name);
                continue;
            }

            if (DefaultAttributes.acceptName (name)) {
                // file with extended attributes
                DefaultAttributes.Table table = DefaultAttributes.loadTable (jis,name);
                attributes.put (name, table);
            } else {
                // copy the file
                FileObject fd = createData(fo, name);
                FileLock lock = fd.lock ();
                try {
                    OutputStream os = fd.getOutputStream (lock);
                    try {
                        copy (jis, os);
                    } finally {
                        os.close ();
                    }
                } finally {
                    lock.releaseLock ();
                }
            }
        }

        //
        // apply all extended attributes
        //

        Iterator it = attributes.entrySet ().iterator ();
        while (it.hasNext ()) {
            Map.Entry entry = (Map.Entry)it.next ();

            String fileName = (String)entry.getKey ();
            int last = fileName.lastIndexOf ('/');
            String dirName;
            if (last != -1)
                dirName = fileName.substring (0, last + 1);
            else
                dirName = ""; // NOI18N
            String prefix = fo.isRoot () ? dirName : fo.getPath() + '/' + dirName;

            DefaultAttributes.Table t = (DefaultAttributes.Table)entry.getValue ();
            Iterator files = t.keySet ().iterator ();
            while (files.hasNext ()) {
                String orig = (String)files.next ();
                String fn = prefix + orig;
                FileObject obj = fo.getFileSystem ().findResource (fn);

                if (obj == null) {
                    continue;
                }

                Enumeration attrEnum = t.attrs (orig);
                while (attrEnum.hasMoreElements ()) {
                    // iterate thru all arguments
                    String attrName = (String)attrEnum.nextElement ();
                    // Note: even transient attributes set here!
                    Object value = t.getAttr (orig, attrName);
                    if (value != null) {
                        obj.setAttribute (attrName, value);
                    }
                }
            }
        }

    } // extractJar


    /** Gets the extension of a specified file name. The extension is
    * everything after the last dot.
    *
    * @param fileName name of the file
    * @return extension of the file (or <code>""</code> if it had none)
    */
    public static String getExtension(String fileName) {
        int index = fileName.lastIndexOf("."); // NOI18N
        if (index == -1)
            return ""; // NOI18N
        else
            return fileName.substring(index + 1);
    }

    /** Finds an unused file name similar to that requested in the same folder.
     * The specified file name is used if that does not yet exist.
     * Otherwise, the first available name of the form <code>basename_nnn.ext</code> (counting from one) is used.
     *
     * <p><em>Caution:</em> this method does not lock the parent folder
     * to prevent race conditions: i.e. it is possible (though unlikely)
     * that the resulting name will have been created by another thread
     * just as you were about to create the file yourself (if you are,
     * in fact, intending to create it just after this call). Since you
     * cannot currently lock a folder against child creation actions,
     * the safe approach is to use a loop in which a free name is
     * retrieved; an attempt is made to {@link FileObject#createData create}
     * that file; and upon an <code>IOException</code> during
     * creation, retry the loop up to a few times before giving up.
     *
    * @param folder parent folder
    * @param name preferred base name of file
    * @param ext extension to use
    * @return a free file name */
    public static String findFreeFileName (
        FileObject folder, String name, String ext
    ) {
        if (checkFreeName (folder, name, ext)) {
            return name;
        }
        for (int i = 1;;i++) {
            String destName = name + "_"+i; // NOI18N
            if (checkFreeName (folder, destName, ext)) {
                return destName;
            }
        }
    }

    /** Finds an unused folder name similar to that requested in the same parent folder.
     * <p>See caveat for <code>findFreeFileName</code>.
     * @see #findFreeFileName findFreeFileName
    * @param folder parent folder
    * @param name preferred folder name
    * @return a free folder name
    */
    public static String findFreeFolderName (
        FileObject folder, String name
    ) {
        if (checkFreeName (folder, name, null)) {
            return name;
        }
        for (int i = 1;;i++) {
            String destName = name + "_"+i; // NOI18N
            if (checkFreeName (folder, destName, null)) {
                return destName;
            }
        }
    }
    
    /** Test if given name is free in given folder.
     * @param fo folder to check in
     * @param name name of the file or folder to check
     * @param ext extension of the file (null for folders)
     * @return true, if such name does not exists
     */
    private static boolean checkFreeName (
        FileObject fo, String name, String ext
    ) {
        if (Utilities.isWindows()) {
            // case-insensitive, do some special check
            Enumeration en = fo.getChildren(false);
            while (en.hasMoreElements()) {
                fo = (FileObject)en.nextElement();
                String n = fo.getName ();
                String e = fo.getExt();
                
                // different names => check others
                if (!n.equalsIgnoreCase (name)) continue;
                
                // same name + without extension => no
                if ((ext == null || ext.trim().length() == 0) && (e == null || e.trim().length() == 0)) return false;
                
                // one of there is witout extension => check next
                if (ext == null || e == null) continue;
                
                if (ext.equalsIgnoreCase (e)) {
                  // same name + same extension => no
                  return false;
                }
            }
            
            // no of the files has similar name and extension
            return true;
        } else {
          if (ext == null) {
              return fo.getFileObject(name) == null;
          } else {
              return fo.getFileObject(name, ext) == null;
          }
        }
    }

    // note: "sister" is preferred in English, please don't ask me why --jglick // NOI18N
    /** Finds brother file with same base name but different extension.
    * @param fo the file to find the brother for or <CODE>null</CODE>
    * @param ext extension for the brother file
    * @return a brother file (with the requested extension and the same parent folder as the original) or
    *   <CODE>null</CODE> if the brother file does not exist or the original file was <CODE>null</CODE>
    */
    public static FileObject findBrother (FileObject fo, String ext) {
        if (fo == null) return null;
        FileObject parent = fo.getParent ();
        if (parent == null) return null;

        return parent.getFileObject (fo.getName (), ext);
    }

    /** Obtain MIME type for a well-known extension.
    * If there is a case-sensitive match, that is used, else will fall back
    * to a case-insensitive match.
    * @param ext the extension: <code>"jar"</code>, <code>"zip"</code>, etc.
    * @return the MIME type for the extension, or <code>null</code> if the extension is unrecognized
    * @deprecated in favour of {@link #getMIMEType(FileObject) getMIMEType(FileObject)} as MIME cannot
    * be generaly detected by a file object extension.
    */
    public static String getMIMEType (String ext) {
        String s = (String) map.get (ext);
        if (s != null)
            return s;
        else
            return (String) map.get (ext.toLowerCase ());
    }

    /** Resolves MIME type. Registered resolvers are invoked and used to achieve this goal.
    * Resolvers must subclass MIMEResolver. If resolvers don`t recognize MIME type then  
    * MIME type is obtained  for a well-known extension.
    * @param fo whose MIME type should be recognized
    * @return the MIME type for the FileObject, or <code>null</code> if the FileObject is unrecognized
    */    
    public static String getMIMEType (FileObject fo) {
        String retVal = MIMESupport.findMIMEType(fo);
        if (retVal == null) retVal = getMIMEType (fo.getExt ());
        return retVal;
    }

    /* mapping of file extensions to content-types */
    private static java.util.Dictionary map = new java.util.Hashtable();

    /**
     * Register MIME type for a new extension.
     * Note that you may register a case-sensitive extension if that is
     * relevant (for example <samp>*.C</samp> for C++) but if you register
     * a lowercase extension it will by default apply to uppercase extensions
     * too (for use on Windows or generally for situations where filenames
     * become accidentally upcased).
     * @param ext the file extension (should be lowercase unless you specifically care about case)
     * @param mimeType the new MIME type
     * @throws IllegalArgumentException if this extension was already registered with a <em>different</em> MIME type
     * @see #getMIMEType
     * @deprecated You should instead use the more general {@link MIMEResolver} system.
     */
    public static void setMIMEType(String ext, String mimeType) {
        synchronized (map) {
            String old=(String)map.get(ext);
            if (old == null) {
                map.put(ext, mimeType);
            } else {
                if (!old.equals(mimeType))
                    throw new IllegalArgumentException
                    ("Cannot overwrite existing MIME type mapping for extension `" + // NOI18N
                     ext + "' with " + mimeType + " (was " + old + ")"); // NOI18N
                // else do nothing
            }
        }
    }

    static {
        setMIMEType("", "content/unknown"); // NOI18N
        setMIMEType("uu", "application/octet-stream"); // NOI18N
        setMIMEType("exe", "application/octet-stream"); // NOI18N
        setMIMEType("ps", "application/postscript"); // NOI18N
        setMIMEType("zip", "application/zip"); // NOI18N
        setMIMEType("class", "application/octet-stream"); // Sun uses application/java-vm // NOI18N
        setMIMEType("jar", "application/x-jar"); // NOI18N
        setMIMEType("sh", "application/x-shar"); // NOI18N
        setMIMEType("tar", "application/x-tar"); // NOI18N
        setMIMEType("snd", "audio/basic"); // NOI18N
        setMIMEType("au", "audio/basic"); // NOI18N
        setMIMEType("wav", "audio/x-wav"); // NOI18N
        setMIMEType("gif", "image/gif"); // NOI18N
        setMIMEType("jpg", "image/jpeg"); // NOI18N
        setMIMEType("jpeg", "image/jpeg"); // NOI18N
        setMIMEType("htm", "text/html"); // NOI18N
        setMIMEType("html", "text/html"); // NOI18N
        setMIMEType("xml", "text/xml"); // NOI18N
        setMIMEType("xsl", "text/xml"); // NOI18N
        setMIMEType("xsd", "text/xml"); // NOI18N
        setMIMEType("dtd", "text/x-dtd"); // NOI18N
        setMIMEType("css", "text/css"); // NOI18N
        setMIMEType("text", "text/plain"); // NOI18N
        setMIMEType("pl", "text/plain"); // NOI18N
        setMIMEType("txt", "text/plain"); // NOI18N
        setMIMEType("properties", "text/plain"); // NOI18N
        setMIMEType("java", "text/x-java"); // NOI18N
        // mime types from Jetty web server
        setMIMEType("ra", "audio/x-pn-realaudio"); // NOI18N
        setMIMEType("ram", "audio/x-pn-realaudio"); // NOI18N
        setMIMEType("rm", "audio/x-pn-realaudio"); // NOI18N
        setMIMEType("rpm", "audio/x-pn-realaudio"); // NOI18N
        setMIMEType("mov", "video/quicktime"); // NOI18N
        setMIMEType("jsp", "text/plain"); // NOI18N
    }
    
    /**
     * Construct a stream handler that handles the <code>nbfs</code> URL protocol
     * used for accessing file objects directly.
     * This method is not intended for module use; only the core
     * should need to call it.
     * Modules probably need only use {@link URLMapper} to create and decode such
     * URLs.
     * @since 3.17
     */
    public static URLStreamHandler nbfsURLStreamHandler() {
        return FileURL.HANDLER;
    }

    /** Recursively checks whether the file is underneath the folder. It checks whether
     * the file and folder are located on the same filesystem, in such case it checks the
     * parent <code>FileObject</code> of the file recursively untill the folder is found
     * or the root of the filesystem is reached.
     * 
     * @param folder the root of folders hierarchy to search in
     * @param fo the file to search for
     * @return <code>true</code>, if <code>fo</code> lies somewhere underneath the <code>folder</code>,
     * <code>false</code> otherwise
     * @since 3.16
     */
    public static boolean isParentOf (FileObject folder, FileObject fo) {
        if (folder.isData ()) {
            return false;
        }

        try {
            if (folder.getFileSystem () != fo.getFileSystem ()) {
                return false;
            }
        } catch (FileStateInvalidException e) {
            return false;
        }
        
        FileObject parent = fo.getParent ();
        while (parent != null) {
            if (parent == folder) {
                return true;
            }
            
            parent = parent.getParent ();
        }
        
        return false;
    }
}
