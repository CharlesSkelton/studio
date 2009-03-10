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

import java.net.*;
import java.io.File;
import java.util.Iterator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.openide.util.Lookup;
import org.openide.util.Utilities;

/** Mapper from FileObject -> URL.
 * Should be registered in lookup.
 * For all methods, if the passed-in file object is the root folder
 * of some filesystem, then it is assumed that any valid file object
 * in that filesystem may also have a URL constructed for it by means
 * of appending the file object's resource path to the URL of the root
 * folder. If this cannot work for all file objects on the filesystem,
 * the root folder must not be assigned a URL of that type. nbfs: URLs
 * of course always work correctly in this regard.
 * @since 2.16
 */
public abstract class URLMapper {

    /** URL which works inside this VM.*/
    public static final int INTERNAL = 0;
    /** URL which works inside this machine.*/        
    public static final int EXTERNAL = 1;
    /** URL which works from networked machines.*/
    public static final int NETWORK = 2;
    /** results with URLMapper instances*/
    private static Lookup.Result result;            
    /** Basic impl. for JarFileSystem, LocalFileSystem, MultiFileSystem */    
    private static URLMapper defMapper;
    

    static {
        result = Lookup.getDefault().lookup(new Lookup.Template (URLMapper.class));
    }            
        
    /** Find a good URL for this file object which works according to type:
     * -inside this VM
     * - inside this machine
     * - from networked machines 
     * @return a suitable URL, or null
     */        
    public static  URL findURL(FileObject fo, int type) {
        URL retVal;
        
        /** secondly registered URLMappers are asked to resolve URL */
        Iterator instances = result.allInstances ().iterator();                
        while (instances.hasNext()) {
            URLMapper mapper = (URLMapper) instances.next();
            if (mapper == getDefault ()) continue;            
            retVal = mapper.getURL (fo, type);
            if (retVal != null) return retVal;            
        }  

        /** first basic implementation */
        retVal = getDefault ().getURL (fo, type);
        if (retVal != null) return retVal;
        
        /** if not resolved yet then internal URL with nbfs protocol is returned */        
        if (type == INTERNAL) {
            try {
                retVal =  FileURL.encodeFileObject (fo);
            } catch (FileStateInvalidException iex) { return null;}
        }
        
        return retVal;
    }
       
    /** Get a good URL for this file object which works according to type:
     * -inside this VM
     * - inside this machine
     * - from networked machines 
     * @return a suitable URL, or null
     */            
    public abstract URL getURL(FileObject fo, int type);

    /** Find an array of FileObjects for this url
     * Zero or more FOs may be returned.
     *
     * For each returned FO, it must be true that FO -> URL gives the
     * exact URL which was passed in, but depends on appripriate type
     * <code> findURL(FileObject fo, int type) </code>.
     * @param url to wanted FileObjects
     * @return a suitable arry of FileObjects, or empty array if not successful
     * @since  2.22*/
    public static FileObject[] findFileObjects (URL url) {
        /** first basic implementation */
        Set retSet = new HashSet ();
        FileObject[] retVal = getDefault ().getFileObjects (url);
        if (retVal != null) retSet.addAll(Arrays.asList(retVal));
        
        /** secondly registered URLMappers are asked to resolve URL */
        Iterator instances = result.allInstances().iterator();
        while (instances.hasNext()) {
            URLMapper mapper = (URLMapper) instances.next();
            retVal = mapper.getFileObjects(url);
            if (retVal != null) retSet.addAll(Arrays.asList(retVal));
        }
        
        retVal = new FileObject [retSet.size()];
        retSet.toArray(retVal);
        return retVal;
    }

    /** Get an array of FileObjects for this url
     * @param url to wanted FileObjects
     * @return a suitable arry of FileObjects, or null
     * @since  2.22*/                
    public abstract FileObject[] getFileObjects (URL url);
       
    /** this method is expeceted to be invoked to create instance of URLMapper,
     * because of method invocation (attr name="instanceCreate" 
     * methodvalue="org.openide.filesystems.URLMapper.getDefault")*/
    private static URLMapper getDefault () {
        synchronized (URLMapper.class) {
            if (defMapper == null)
                defMapper = new DefaultURLMapper ();
            return defMapper;
        }
    }

    /*** Basic impl. for JarFileSystem, LocalFileSystem, MultiFileSystem */    
    private static class DefaultURLMapper extends URLMapper {
        
        DefaultURLMapper() {}
        
        // implements  URLMapper.getFileObjects(URL url)
        public FileObject[] getFileObjects(URL url) {
            return geFileObjectBasicImpl(url);
        }
        
        // implements  URLMapper.getURL(FileObject fo, int type)
        public URL getURL(FileObject fo, int type) {
            return getURLBasicImpl(fo, type);
        }
        
        private static URL getURLBasicImpl(FileObject fo, int type) {
            if (fo == null) return null;
            if (type == NETWORK) return null;
            
            URL retURL = null;
            FileSystem fs = null;
            try {
                fs = fo.getFileSystem();
            } catch (FileStateInvalidException fsex)  {
                return null;
            }
            
            if (fs instanceof LocalFileSystem ) {
                if (type != EXTERNAL) return null;
                LocalFileSystem lfs = (LocalFileSystem) fs;
                File f = lfs.getRootDirectory();
                if (f == null) return null;
                try {
                    retURL = new URL(Utilities.toURL(f), fo.getPath());
                } catch (MalformedURLException mfx) {
                    return null;
                }
            } else if (fs instanceof JarFileSystem ) {
                if (type != EXTERNAL) return null;
                /** JarFile doesn`t contain folders as resources*/
                if (fo.isFolder()) return null;
                JarFileSystem jfs = (JarFileSystem) fs;
                File f = jfs.getJarFile();
                if (f == null) return null;
                try {
                    retURL = new URL("jar:"+Utilities.toURL(f).toExternalForm()+"!/" + fo.getPath());//NOI18N
                } catch (MalformedURLException mfx) {
                    return null;
                }
            } else if (fs instanceof MultiFileSystem ) {
                if (fo instanceof MultiFileObject) {
                    FileObject leader = ((MultiFileObject)fo).getLeader();
                    return getURLBasicImpl(leader, type);
                } else return null;
            } else if (fs instanceof XMLFileSystem ) {
                URL retVal = null;
                try {
                    retVal = ((XMLFileSystem)fs).getURL(fo.getPath());
                    if (retVal == null) return null;
                    if (type == INTERNAL) return retVal;
                    
                    boolean isInternal = retVal.getProtocol().startsWith("nbres");//NOI18N
                    if (type == EXTERNAL && !isInternal) return retVal;
                    return null;
                } catch (FileNotFoundException fnx) {
                    return null;
                }
            }
            
            if (retURL == null) {
                File fFile = FileUtil.toFile(fo);
                if (fFile != null) {
                    try {
                        return Utilities.toURL(fFile);
                    } catch (MalformedURLException mfx) {
                        return null;
                    }
                }
            }
            
            return retURL;
        }
        
        private static FileObject[] geFileObjectBasicImpl(URL url) {
            String prot = url.getProtocol();
            
            if (prot.equals("nbfs")) { //// NOI18N
                FileObject retVal = FileURL.decodeURL(url);
                return (retVal == null) ? null : new FileObject[] {retVal};
            }
            
            if (prot.equals("jar")) {  //// NOI18N
                try {
                    URLConnection ucon = url.openConnection();
                    if (ucon != null && ucon instanceof JarURLConnection) {
                        URL jarURL = ((JarURLConnection) ucon).getJarFileURL();
                        String systemName = Utilities.toFile(jarURL).getCanonicalPath();
                        FileSystem fs = Repository.getDefault().findFileSystem(systemName);
                        if (fs != null && fs instanceof JarFileSystem) {
                            FileObject retVal = fs.findResource(((JarURLConnection) ucon).getEntryName());
                            return (retVal == null) ? null : new FileObject[] {retVal};
                        }
                    }
                } catch (IOException iox) {
                    return null;
                }
                return null;
            }
            
            if (prot.equals("file")) {  //// NOI18N
                File f = Utilities.toFile(url);
                FileObject[] foRes = FileUtil.fromFile(f);
                if (foRes != null && foRes.length > 0) {
                    return foRes;
                }
            }
            
            return null;
        }        
    }    
}
