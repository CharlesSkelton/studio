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

import java.lang.ref.*;
import java.io.*;
import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import java.net.*;

import org.openide.util.enums.EmptyEnumeration;
import org.openide.util.NbBundle;

import org.xml.sax.*;

import javax.xml.parsers.SAXParserFactory;

/** XML-based filesystem.
 * <PRE>
 *  Description of format of XML file (which can be parsed by XMLFileSystem)
 * ==================================================================
 * Allowed Elements:	filesystem,file,folder,attr
 *
 * Mandatory attributes:	
 *         -for filesystem    version=... (e.g. "1.0")
 *         -for file,folder,attr name=....  (e.g.: &lt;folder name="Config"&gt;)
 *         -for attr is mandatory one of bytevalue,shortvalue,intvalue,longvalue,floatvalue,doublevalue,boolvalue,charvalue,stringvalue,methodvalue,serialvalue,urlvalue
 * 
 * Allowed atributes:
 *         -for file:	url=.... (e.g.: &lt;file name="sample.xml" url="file:/c:\sample.xml"&gt;)
 * 	-for folder,filesystem	nothing allowed
 *
 *
 *
 * Note: file can contain 	content e.g.:
 *  &lt; file name="sample.java"&gt;
 * &lt; ![CDATA[
 * package org.sample;
 * import java.io;
 * ]]&gt;
 * &lt; /file&gt;
 * But using url="..." is preferred.
 *
 *
 * This class implements virtual FileSystem. It is special case of FileSystem in XML format.
 *
 * Description of this format best ilustrate DTD file that is showed in next lines: 
 * &lt; !ELEMENT filesystem (file | folder)*&gt;
 * &lt; !ATTLIST filesystem version CDATA #REQUIRED&gt; //version not checkked yet
 * &lt; !ELEMENT folder (file |folder | attr)*&gt;
 * &lt; !ATTLIST folder name CDATA #REQUIRED&gt; //name of folder
 * &lt; !ELEMENT file (#PCDATA | attr)*&gt;
 * &lt; !ATTLIST file name CDATA #REQUIRED&gt; //name of file
 * &lt; !ATTLIST file url CDATA #IMPLIED&gt; //content of the file can be find at url
 * &lt; !ELEMENT attr EMPTY&gt;
 * &lt; !ATTLIST attr name CDATA #REQUIRED&gt; //name of attribute
 * &lt; !ATTLIST attr bytevalue CDATA #IMPLIED&gt;//the rest - types of attributes
 * &lt; !ATTLIST attr shortvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr intvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attribute longvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr floatvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr doublevalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr boolvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr charvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr stringvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr methodvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr serialvalue CDATA #IMPLIED&gt;
 * &lt; !ATTLIST attr urlvalue CDATA #IMPLIED&gt;
 * </PRE>
 * @author Radek Matous
 */

public final class XMLFileSystem extends AbstractFileSystem {
    
    static final long serialVersionUID =  28974107313702326L;
        
    /**  Url location of XML document    */            
    private URL[] urlsToXml = new URL[] {};    

    private transient FileObjRef rootRef;               
    // <?xml version="1.0"?>
    // <!DOCTYPE filesystem PUBLIC "-//NetBeans//DTD Filesystem 1.0//EN" "http://www.netbeans.org/dtds/filesystem-1_0.dtd">
    // <filesystem>...</filesystem>
        
    private static final Map DTD_MAP = new HashMap ();
    static {
        DTD_MAP.put ("-//NetBeans//DTD Filesystem 1.0//EN", "org/openide/filesystems/filesystem.dtd"); //NOI18N
        DTD_MAP.put ("-//NetBeans//DTD Filesystem 1.1//EN", "org/openide/filesystems/filesystem1_1.dtd");//NOI18N        
    }
    
    /** Constructor. Creates new XMLFileSystem */
    public XMLFileSystem() {
        Impl impl = new Impl (this);
        this.list = impl;
        this.info = impl;
        this.change = impl;
        this.attr = impl;
    }
    
    /** Constructor. Creates new XMLFileSystem.
     * @param uri to file with definition of XMLFileSystem
     * @throws SAXException if parsing is not succesful
     */    
    public XMLFileSystem(String uri) throws SAXException {
	this();
	try {
    	    setXmlUrl(new URL(uri));
        } catch(Exception e) {
    	    throw (SAXException)ExternalUtil.copyAnnotation (new SAXException (e.getMessage()),e);
        }
    }

    /** Constructor. Creates new XMLFileSystem.
     * @param url to definition of XMLFileSystem
     * @throws SAXException if parsing not succesful
     */
    public XMLFileSystem(URL url) throws SAXException{
	this();
        try {
	    setXmlUrl(url);
        } catch(Exception e) {
    	    throw (SAXException)ExternalUtil.copyAnnotation (new SAXException (e.getMessage()),e);
        }         
    }

       
    /** Constructor. Allows user to provide own capabilities
    * for this filesystem.
    * @param cap capabilities for this filesystem
    */
    public XMLFileSystem(FileSystemCapability cap) {
        this ();
        setCapability (cap);
    }

   
    
    /** Getter of url field.
     * @return URL associated with XMLFileSystem or null if no URL was set. 
     * In case that definition of XMLFileSystem 
     * is merged from more URLs than the first is returned.
     */
    public URL getXmlUrl () { // JST        
        return urlsToXml.length > 0?urlsToXml[0]:null;
    }

    /**
     * Setter of url field. Set name of the XML file.
     * @param url with definition of XMLFileSystem
     * @throws PropertyVetoException if the change is not allowed by a listener
     * @throws IOException if the file is not valid
     */    
    public synchronized void setXmlUrl (URL url) throws IOException, PropertyVetoException {    
        setXmlUrl (url, false);
    }
    
    /**
     * Setter of url field. Set name of the XML file.
     * @param url with definition of XMLFileSystem
     * @param validate sets validating of SAXParser
     * @throws PropertyVetoException if the change is not allowed by a listener
     * @throws IOException if the file is not valid
     */
    public synchronized void setXmlUrl (URL url, boolean validate) throws IOException, PropertyVetoException {
	setXmlUrls(new URL[] {url}, validate);
    }     

    /** Getter of url fields.
     * @return URLs associated with XMLFileSystem. 
     * @deprecated experimental method. Nobody should rely on this method yet.
     * @since 1.14
     */
    public URL[] getXmlUrls () { // JST
        return urlsToXml;
    }
        
    /** Setter of url fields. First URL in array sets name of XMLFileSystem.
     * If more then one url in array of URLs defines the same FileObject, then 
     * url with lower index in array overrides (means content and attributes) the other. 
     * @param urls array of definitions (in xml form) of XMLFileSystem
     * @throws IOException if the file is not valid
     * @throws PropertyVetoException if the change is not allowed by a listener
     * @deprecated experimental method. Nobody should rely on this method yet.
     * @since 1.14
     */    
    public synchronized void setXmlUrls (URL[] urls) throws IOException, PropertyVetoException {
	setXmlUrls(urls, false);
    }
    
    private synchronized void setXmlUrls (URL[] urls, boolean validate) throws IOException, PropertyVetoException {
//long time = System.currentTimeMillis();
        ResourceElem rootElem;
        String oldDisplayName = getDisplayName ();        
        if (urls.length == 0) {
            urlsToXml = new URL[] {};
            refreshChildrenInAtomicAction ((AbstractFolder)getRoot(),rootElem = new ResourceElem(true,urls,null) ); // NOI18N
            rootElem = null;
            return;
        }
	
        Handler handler = new Handler(DTD_MAP,rootElem = new ResourceElem (true,urls,null), validate); // NOI18N        
        
        URL[] origUrls = urlsToXml;
        urlsToXml = new URL[urls.length];
        
        try {
            setSystemName("XML_"+urls[0].toExternalForm());// NOI18N
        } catch (PropertyVetoException pvx) {
            urlsToXml = origUrls;
            rootElem = null;
            throw pvx;
        }

	URL act = null;
	
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance ();
            factory.setValidating (validate);
            factory.setNamespaceAware(false);
	    Parser xp = factory.newSAXParser ().getParser ();
            xp.setEntityResolver(handler);
            xp.setDocumentHandler(handler);
            xp.setErrorHandler(handler);

            for (int index = 0;  index < urls.length; index++) {
		act = urls[index];
                urlsToXml[index] = act;
		handler.urlContext = act;
		xp.parse(act.toExternalForm());
            }
//time = System.currentTimeMillis() - time;
//System.err.println("Parsing took " + time + "ms");
//time = System.currentTimeMillis();
            refreshChildrenInAtomicAction ((AbstractFolder)getRoot (),rootElem ); 
//time = System.currentTimeMillis() - time;
//System.err.println("Notifying took " + time + "ms");
        } catch (IOException iox) {
            urlsToXml = origUrls;                       
            throw iox;
        } catch (Exception e) { 
            IOException x = new IOException (e.getMessage()+" : "+act); // NOI18N
            ExternalUtil.copyAnnotation (x,e);
            throw x;
        } finally {
            rootElem = null;                        
        }
        firePropertyChange (PROP_DISPLAY_NAME, oldDisplayName, getDisplayName ());                            
    }

    /**
    * @return if value of lastModified should be cached
    */    
    boolean isLastModifiedCacheEnabled () {
        return false;
    }
    
    /**
     * Test if the file is folder or contains data.
     * @param name name of the file
     * @return true if the file is folder, false otherwise
     */
    private boolean isFolder(String name) {
        Reference ref = findReference(name);
        
        if (ref != null && (ref instanceof FileObjRef) ) {
            return ((FileObjRef) ref).isFolder();
        }
        return false;
    }
    
    /**
    * Get input stream.
    *
    * @param name the file to test
    * @return an input stream to read the contents of this file
    * @exception FileNotFoundException if the file does not exists or is invalid
    */    
    private InputStream getInputStream(String name) throws java.io.FileNotFoundException  {
        Reference ref = findReference(name);        
        if (ref != null && (ref instanceof FileObjRef ) ) {
            return ((FileObjRef) ref).getInputStream(name);
        }        
        throw new FileNotFoundException (NbBundle.getMessage(XMLFileSystem.class, "EXC_CanntRead", name));// NOI18N
    }

    /**
    * Get URL.
    *
    * @param name of the file to test
    * @return URL of resource or null
    * @exception FileNotFoundException if the file does not exists or is invalid
    */        
    URL getURL (String name) throws java.io.FileNotFoundException  {
        Reference ref = findReference(name);        
        if (ref != null && (ref instanceof FileObjRef ) ) {
            return ((FileObjRef) ref).createAbsoluteUrl(name);
        }        
        throw new FileNotFoundException (NbBundle.getMessage(XMLFileSystem.class, "EXC_CanntRead", name));// NOI18N
    }    
    /** Get size of stream*/
    private long getSize (String name)   {
        Reference ref = findReference(name);        
        if (ref != null && (ref instanceof FileObjRef ) ) {
            return ((FileObjRef) ref).getSize(name);
        }        
        return 0;
    }

   /**returns value of last modification*/
   private java.util.Date lastModified(String name) {                       
        Reference ref = findReference(name);        
        if (ref != null && (ref instanceof FileObjRef ) ) {
            return ((FileObjRef) ref).lastModified (name);
        }        
        /**return value for resource that does not exists*/
        return new Date (0);       
   }
    
   /** Provides a name for the system that can be presented to the user.
    * @return user presentable name of the filesystem
    */   
    public String getDisplayName() {
        if (urlsToXml.length == 0 || urlsToXml[0] == null || urlsToXml[0].toExternalForm().length() == 0) return  NbBundle.getMessage(XMLFileSystem.class,"XML_NotValidXMLFileSystem");// NOI18N
        return "XML:"+urlsToXml[0].toExternalForm().trim();// NOI18N
    }
    
    /** Test if the filesystem is read-only or not.
     * @return true if the system is read-only
     */
    public boolean isReadOnly() {
        return true;
    }
        
    /** Initializes the root of FS.
    */
    private void readObject (ObjectInputStream ois) throws IOException, ClassNotFoundException {
        //ois.defaultReadObject ();
        ObjectInputStream.GetField fields = ois.readFields();  
        URL[] urls = (URL[]) fields.get ("urlsToXml",null); // NOI18N
        if (urls == null) {
            urls = new URL[1];
            urls[0] = (URL) fields.get ("uriId",null); // NOI18N
        }
        
        try {
            if (urlsToXml.length != 1)
                setXmlUrls(urlsToXml);
            else
                setXmlUrl(urlsToXml[0]);
        } catch (PropertyVetoException ex) {
            IOException x = new IOException (ex.getMessage());
            ExternalUtil.copyAnnotation (x,ex);
            throw x;
        }
    }
    
    /** Notifies this filesystem that it has been added to the repository.
     * Various initialization tasks should go here. Default implementation is noop.
     */
    public void addNotify() {}
    
    /** Notifies this filesystem that it has been removed from the repository.
     * Concrete filesystem implementations should perform clean-up here.
     * Default implementation is noop.
     */
    public void removeNotify() {}
            
    protected  Reference createReference(FileObject fo) {
        return new FileObjRef (fo);
    }
    
    private void refreshChildrenInAtomicAction (AbstractFolder fo, ResourceElem resElem) {
        try {
            beginAtomicAction ();
            refreshChildren (fo, resElem);
        } finally {
            finishAtomicAction ();            
        }
    }
    /** refreshes children recursively.*/
    private void refreshChildren (AbstractFolder fo, ResourceElem resElem) {
        if (fo.isRoot()) 
          initializeReference (rootRef = new FileObjRef (fo), resElem);
        
        java.util.List nameList = resElem.getChildren ();        
        String[] names = new String[nameList.size ()];
	ResourceElem[] children = new ResourceElem[names.length];        
        
        nameList.toArray(names);
        
        for (int i = 0; i < names.length; i++) 
            children[i] = resElem.getChild (names[i]);
        
        	
        fo.refresh(null, null, true, true, names);

        for (int i = 0; i < children.length; i++) {
            AbstractFolder fo2 = (AbstractFolder)fo.getFileObject (names[i]);
            FileObjRef currentRef = (FileObjRef )findReference (fo2.getPath ());
            initializeReference (currentRef, children[i]);
            if (fo2.isFolder()) refreshChildren (fo2,children[i]);
        }
    }
    
    private void initializeReference (FileObjRef currentRef, ResourceElem resElem) {
        if (!currentRef.isInitialized ()) 
            currentRef.initialize (resElem);            
        else {
            currentRef.attacheAttrs (resElem.getAttr (false));
            currentRef.setUrlContext (resElem.getUrlContext ());
            if (resElem.getContent() != null) {
                currentRef.content = resElem.getContent();
            } else if (resElem.getURI() != null) {
                currentRef.content = resElem.getURI();
            }
        }                
    }
    
    /** Temporary hierarchical structure of resources. Used while parsing.*/
    private static class ResourceElem {
        private java.util.List children;
        private java.util.List names;        
        private byte[]  content;
        private java.util.List urlContext = new ArrayList ();
        private XMLMapAttr foAttrs;
        private boolean isFolder;
        private String uri;
        
        
        public ResourceElem (boolean isFolder, URL[] urlContext, String uri) {
            this.isFolder = isFolder;
            this.uri = uri;
            this.urlContext.addAll(Arrays.asList(urlContext));
	    if (isFolder) {
                children = new ArrayList ();
                names = new ArrayList ();                
            }
        }

        public ResourceElem (boolean isFolder, URL urlContext, String uri) {
            this.isFolder = isFolder;
            this.uri = uri;
            this.urlContext.add(urlContext);
	    if (isFolder) {
                children = new ArrayList ();
                names = new ArrayList ();                
            }
        }

        
        ResourceElem addChild (String name, ResourceElem child) {
	    if (!isFolder) throw new IllegalArgumentException("not a folder"); // NOI18N
            ResourceElem retVal = child;
            int idx = names.indexOf(name);
            if (idx == -1) {
                names.add(name);
                children.add(child);
            } else {
                retVal = (ResourceElem)children.get(idx);
            }
            return retVal;
        }

        java.util.List getChildren () {
            return names;
        }
        
        ResourceElem getChild (String name) {
            return (ResourceElem )children.get(names.indexOf(name));
        }
        
        XMLMapAttr getAttr ( boolean create ) {
            if (create && foAttrs == null)
                foAttrs = new XMLMapAttr ();
            return foAttrs;
        }
        
        byte[] getContent () {
            return content;
        }
        
        URL[] getUrlContext () {
            URL[] retVal = new URL[urlContext.size()];
            urlContext.toArray(retVal);
            return retVal;
        }
        
        String getURI () {
            return uri;
        }
        
        void setContent (byte[] content) {
            if (this.content == null) {
                byte[] alloc = new byte[content.length];
                System.arraycopy(content, 0, alloc, 0, content.length);
                this.content = alloc;
            }
        }
        
        boolean isFolder () {
            return isFolder;
        }
        
    }
    
    //private void debugInfo(String dbgStr) { System.out.println(dbgStr);}
        
    /** Implementation of all interfaces List, Change, Info and Attr
     * that delegates to XMLFileSystem
     */
    public static class Impl extends Object
    implements AbstractFileSystem.List, AbstractFileSystem.Info,
    AbstractFileSystem.Change, AbstractFileSystem.Attr {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -67233358102597232L;
        
        /** the pointer to filesystem */
        private XMLFileSystem fs;
        
        /** Constructor.
         * @param fs the filesystem to delegate to
         */
        public Impl (XMLFileSystem fs) {
            this.fs = fs;
        }
        
        /**
         *
         * Scans children for given name
         * @return array of children`s names
         * @param name the folder, by name; e.g. <code>top/next/afterthat</code>
         */
        public String[] children (String name) {
            FileObject  fo2name;
            
            if (( fo2name = fs.findResource(name) ) == null) return new String[] {};                    

    	    synchronized (fo2name) {
        	return ((AbstractFolder)fo2name).getChildrenArray();
    	    }
        }
        
        
        //
        // Change
        //
        
        /**
         * Creates new folder named name.
         * @param name name of folder
         * @throws IOException if operation fails
         */
        public void createFolder (String name) throws java.io.IOException {
            throw new IOException ();
        }
        
        
        /**
         * Create new data file.
         *
         * @param name name of the file
         * @exception IOException if the file cannot be created (e.g. already exists) */
        public void createData (String name) throws IOException {
            throw new IOException ();
        }
        
        /** Rename a file.        
         * @param oldName old name of the file; fully qualified
         * @param newName new name of the file; fully qualified
         * @exception IOException if it could not be renamed
         */
        public void rename(String oldName, String newName) throws IOException {
            throw new IOException ();
        }
        
        /**
         * Delete the file.
         *
         * @param name name of file
         * @exception IOException if the file could not be deleted
         */
        public void delete (String name) throws IOException {
            throw new IOException ();
        }
        
// Info
        
        /**
         *
         * Get last modification time.
         * @param name the file to test
         * @return the date
         */
        public java.util.Date lastModified(String name) {            
            return fs.lastModified (name);            
        }
                        
        
        /**
         * Test if the file is folder or contains data.
         * @param name name of the file
         * @return true if the file is folder, false otherwise
         */
        public boolean folder (String name) {
            return fs.isFolder(name);
        }
        
        /**
         * Test whether this file can be written to or not.
         * @param name the file to test
         * @return <CODE>true</CODE> if file is read-only
         */
        public boolean readOnly (String name) {
            return true;
        }
        
    /** Get the MIME type of the file. If filesystem has no special support 
    * for MIME types then can simply return null. FileSystem can register 
    * MIME types for a well-known extensions: FileUtil.setMIMEType(String ext, String mimeType)
    * or together with filesystem supply some resolvers subclassed from MIMEResolver.
    *
    * @param name the file to test
    * @return the MIME type textual representation (e.g. <code>"text/plain"</code>)
    * or null if no special support for recognizing MIME is implemented.
     */
        public String mimeType (String name) {
            return null;
        }
        
        /**
         * Get the size of the file.
         *
         * @param name the file to test
         * @return the size of the file in bytes or zero if the file does not contain data (does not
         *  exist or is a folder).
         */
        public long size (String name) {
            if (fs.isFolder(name)) return 0;               
            return fs.getSize (name); 
        }
        
        /**
         * Get input stream.
         *
         * @param name the file to test
         * @return an input stream to read the contents of this file
         * @exception FileNotFoundException if the file does not exists or is invalid
         */
        public InputStream inputStream (String name) throws java.io.FileNotFoundException {            
            InputStream is = fs.getInputStream(name);
            if (is == null) throw new java.io.FileNotFoundException (name);
            return is; 
        }
        
        /**
         * Get output stream.
         *
         * @param name the file to test
         * @return output stream to overwrite the contents of this file
         * @exception IOException if an error occures (the file is invalid, etc.)
         */
        public OutputStream outputStream (String name) throws java.io.IOException {
            throw new IOException ();
        }
        
        /**
         * Does nothing to lock the file.
         *
         * @param name name of the file
         * @throws IOException if cannot be locked
         */
        public void lock (String name) throws IOException {
            FSException.io ("EXC_CannotLock", name, fs.getDisplayName (), name); // NOI18N
        }
        
        /**
         * Does nothing to unlock the file.
         *
         * @param name name of the file
         */
        public void unlock (String name) {
        }
        
        /**
         * Does nothing.
         *
         * @param name the file to mark
         */
        public void markUnimportant (String name) {
        }
        
        /** Get the file attribute with the specified name.
         * @param name the file
         * @param attrName name of the attribute
         * @return appropriate (serializable) value or <CODE>null</CODE> if the attribute is unset (or could not be properly restored for some reason)
         */
        public Object readAttribute(String name,String attrName) {
            FileObjRef ref = (FileObjRef)fs.findReference(name);                        
            if (ref == null  && name.length() == 0 && fs.rootRef != null)  
                ref = fs.rootRef;
            if (ref == null  )  return null;
            return ref.readAttribute(attrName);            
        }
        /** Set the file attribute with the specified name.
         * @param name the file
         * @param attrName name of the attribute
         * @param value new value or <code>null</code> to clear the attribute. Must be serializable, although particular filesystems may or may not use serialization to store attribute values.
         * @exception IOException if the attribute cannot be set. If serialization is used to store it, this may in fact be a subclass such as {@link NotSerializableException}.
         */
        public void writeAttribute(String name,String attrName,Object value) throws IOException {
            throw new IOException ();            
        }
        
        /** Get all file attribute names for the file.
         * @param name the file
         * @return enumeration of keys (as strings)
         */
        public Enumeration attributes(String name) {
            FileObjRef ref = (FileObjRef)fs.findReference(name);            
            if (ref == null  && name.length() == 0 && fs.rootRef != null)  
                ref = fs.rootRef;            
            if (ref == null  )  return EmptyEnumeration.EMPTY;
            return ref.attributes();
        }
        
        /** Called when a file is renamed, to appropriately update its attributes.
	 * @param oldName old name of the file
	 * @param newName new name of the file
	 */
        public void renameAttributes(String oldName,String newName) {
        }
        
        /** Called when a file is deleted, to also delete its attributes.
	 *
	 * @param name name of the file
	 */
        public void deleteAttributes(String name) {
        }
        
    }

    
    
    /** Strong reference to FileObject. To FileObject may be attached attributes (XMLMapAttr) 
     *  and info about if it is folder or not.
     */
    private  static class FileObjRef  extends WeakReference {
        private FileObject fo;
        
        private Object  content;
        private XMLMapAttr foAttrs;
        byte isFolder  = -1;
//        URL[] urlContext = null;
	Object urlContext = null;
        
        public FileObjRef (FileObject fo) {
            super (fo);    
            this.fo = fo;
        }
        
        public boolean isInitialized () {            
            return (isFolder != -1);
        }
        public void initialize  (ResourceElem res) {
            content = res.getContent ();
	    XMLMapAttr tmp = res.getAttr (false);
	    if (tmp != null && !tmp.isEmpty()) {
        	foAttrs = tmp;
	    }
            isFolder = (byte)(res.isFolder ()?1:0);
            if (content == null)
                content = res.getURI ();            
            
            //urlContext = res.getUrlContext ();
	    setUrlContext (res.getUrlContext ());
        }
                
        public boolean isFolder() {
            return (isFolder == 1);
        }        
        
        public void attacheAttrs (XMLMapAttr attrs) {
	    if ( attrs == null || attrs.isEmpty()) {
		return;
	    }
            if (foAttrs == null)
                foAttrs = new XMLMapAttr ();
            
            Iterator it = attrs.entrySet ().iterator ();
            while (it.hasNext()) {
                Map.Entry attrEntry = (Map.Entry )it.next();
                foAttrs.put (attrEntry.getKey(), attrEntry.getValue());
            }
        }
        
        public void setUrlContext (URL[] ctx) {
	    if (ctx.length > 0) {
		if (ctx.length > 1) {
		    urlContext = ctx;
		} else {
		    urlContext = ctx[0];
		}
	    }
            //urlContext = ctx;
        }
         
        public Enumeration attributes() {            
            if (foAttrs == null) {
                return EmptyEnumeration.EMPTY;
            } else {
                HashSet s = new HashSet (foAttrs.keySet ());
                return Collections.enumeration (s);
            }
        }
        
	private URL[] getLayers() {
	    if (urlContext == null) return null;
	    if (urlContext instanceof URL[]) return (URL[])urlContext;
            return new URL[] { (URL)urlContext };
	}
	
        public Object readAttribute(String attrName) {
            if (attrName.equals("layers")) { //NOI18N
		return getLayers();
            }
            if (foAttrs == null) return null;

            FileObject topFO = (FileObject) MultiFileObject.attrAskedFileObject.get();
            FileObject f = topFO == null ? fo : topFO;

            MultiFileObject.attrAskedFileObject.set(null);
            try {
                Object[] objs = new Object[] {f,attrName};
                return  foAttrs.get(attrName,objs);
            } finally {
                MultiFileObject.attrAskedFileObject.set(topFO);
            }
        }
        
            
        /**
         * Get input stream.
         *
         * @return an input stream to read the contents of this file
         * @param context 
         * @param name the file to test
         * @exception FileNotFoundException if the file does not exists or is invalid */        
        public InputStream getInputStream(String name) throws java.io.FileNotFoundException  {            
            InputStream is = null;            
            if (content == null) 
                return new ByteArrayInputStream (new byte[] {});

            if (content instanceof String) {
                URL absURL = createAbsoluteUrl (name);

                try {
                    is = absURL.openStream();
                } catch (IOException iox) {
                    FileNotFoundException x = new FileNotFoundException (name);
                    ExternalUtil.copyAnnotation (x,iox);
                    throw x;                                                                
                }
            }
            if (content instanceof byte[]){
                is = new ByteArrayInputStream((byte[])content);
            }
            
            if (is == null) throw new FileNotFoundException (name);
            return is;
        }
        
        private URL createAbsoluteUrl (String name) throws java.io.FileNotFoundException {
            if (!(content instanceof String)) 
                return null;
            String uri = (String)content;                                                            
                try {
		    URL[] uc = getLayers();
                    URL retVal = (uc == null || uc.length == 0)?new URL (uri):
                        new URL (uc[0],uri);

                    return retVal;
                  
                } catch(IOException ex) {// neni koser osetreni - RM
                    FileNotFoundException x = new FileNotFoundException (name);
                    ExternalUtil.copyAnnotation (x,ex);
                    throw x;                                                                
                }             
                
        }
        
        public long getSize (String name)  {
            if (content == null) return 0;            
            
            if (content instanceof byte[]) 
                return ((byte[])content).length;
                        

            if (content instanceof String) {
                try {   
                    URL absURL = createAbsoluteUrl (name);
                    return absURL.openConnection().getContentLength();
                } catch (IOException iex) {}            
            }
            return 0;
        }
        
        
        public Date lastModified(String name)  {            
            if (content == null || !(content instanceof String)) 
                return new Date (0);            
            URL url;
            try {
                url = createAbsoluteUrl (name);
            } catch (IOException iex) {
                return new Date (0);
            }                                    
            
            File localFile = getLocalFile (url);            
            if (localFile  != null) 
                return new Date (localFile.lastModified());
            
            return timeFromDateHeaderField (url);            
        }
   
        
        /** can return null*/
        private  File getLocalFile (URL url0) {
            return getFileFromResourceString (getLocalResource(url0));            
        }
       
        /** can return null*/
        private static String getLocalResource(URL url) {
            if (url == null) return null;
            if (url.getProtocol().equals("jar")) {// NOI18N
                URL testURL = null;
                try {
                    testURL = new URL(url.getFile());
                    if (testURL.getProtocol().equals("file"))// NOI18N
                        return testURL.getFile();
                } catch (MalformedURLException mfx) {
                    return null;
                }
            }
            
            if (url.getProtocol().equals("file")) {// NOI18N
                return url.getFile();
            }
            
            return null;
        }
        
        /** can return null*/        
        private static File  getFileFromResourceString (String localResource) {            
            if (localResource == null) return null;
            int idx = localResource.indexOf('!');
            String fileName = (idx != -1) ? localResource.substring(0,idx) : localResource;
            File f = new File(fileName);
            if (f.exists()) 
                return f;
                        
            return null;
        }
                
        private java.util.Date timeFromDateHeaderField (URL url) {
            URLConnection urlConn;
            try {                    
                urlConn = url.openConnection ();
                return new java.util.Date (urlConn.getDate());         
            } catch (IOException ie) {
                return new java.util.Date (0);
            }                                        
        }                
    }

    

    /** Class that can be used to parse XML document (Expects array of ElementHandler clasess).  Calls handler methods of ElementHandler clasess.
     */                    
    static class Handler extends HandlerBase  {
        private static final int FOLDER_CODE = "folder".hashCode();// NOI18N
        private static final int FILE_CODE = "file".hashCode();// NOI18N
        private static final int ATTR_CODE = "attr".hashCode();// NOI18N
        
        private ResourceElem rootElem;
        private boolean validate = false;
        Stack resElemStack = new Stack ();
        Stack elementStack = new Stack ();        
        URL urlContext;
	
        private Map dtdMap;
	private ResourceElem topRE;
        private StringBuffer pcdata = new StringBuffer();
                                                    
        Handler(Map dtdMap, ResourceElem rootElem, boolean validate) {
            this.dtdMap = dtdMap;
            this.rootElem = rootElem;
	    this.validate = validate;
        }
        
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            throw exception;                    
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
                
        
        
        public void startElement(String name, AttributeList amap) throws SAXException {            
            int controlCode = name.hashCode();            
            
            elementStack.push(name);

            String foName = amap.getValue("name");// NOI18N

            if (controlCode == FOLDER_CODE) {
    		if (foName == null) throw new SAXException (NbBundle.getMessage(XMLFileSystem.class,"XML_MisssingAttr"));// NOI18N 
                ResourceElem newRes = new ResourceElem (true, urlContext, null);

                topRE = topRE.addChild (foName, newRes);
                resElemStack.push(topRE);            
                return ;
            }
            
            if (controlCode == FILE_CODE) {
    		if (foName == null) throw new SAXException (NbBundle.getMessage(XMLFileSystem.class,"XML_MisssingAttr"));// NOI18N 
		foName = foName.intern();
                
                String uri = null;
                if (amap.getLength () > 1) 
                    uri = amap.getValue("url");// NOI18N

                
                ResourceElem newRes = new ResourceElem (false, urlContext, uri);

                topRE = topRE.addChild (foName, newRes);
                resElemStack.push(topRE);

                pcdata.setLength(0);
                return ;
            }
            
            if (controlCode == ATTR_CODE) {
    		if (foName == null) throw new SAXException (NbBundle.getMessage(XMLFileSystem.class,"XML_MisssingAttr"));// NOI18N 
				
                int len = amap.getLength ();
                for (int i = 0; i < len; i++ ) {
                    String key = amap.getName(i);
                    String value = amap.getValue(i);
                    if (XMLMapAttr.Attr.isValid(key) != -1) {
                        XMLMapAttr.Attr attr = new XMLMapAttr.Attr(key,value);
                        XMLMapAttr attrMap = topRE.getAttr (true);
                        Object retVal = attrMap.put(foName,attr);                        
                        if (retVal != null) 
                            attrMap.put(foName,retVal);
                        
                    }
                } 
                return ;
            }    
        }
        
        public void endElement(String name) throws SAXException {
            if (elementStack.peek().hashCode() == FILE_CODE && !topRE.isFolder ()) {
                String string = pcdata.toString().trim();
                if (string.length() > 0) {
                    topRE.setContent (string.getBytes());
                }
                pcdata.setLength(0);
            }

            int controlCode = name.hashCode();            
	    
            elementStack.pop();
            if (controlCode == FOLDER_CODE || controlCode == FILE_CODE) {
                resElemStack.pop();
		topRE = (ResourceElem)resElemStack.peek();
                return ;
            }            
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (elementStack.peek().hashCode() != FILE_CODE)
                return;
            if (topRE.isFolder ())
                return;
            pcdata.append(new String(ch,start,length));
        }
                                        
        public InputSource resolveEntity(java.lang.String pid,java.lang.String sid) throws SAXException   {
            String publicURL = (String) dtdMap.get (pid);
            if (publicURL != null) {
                if (validate) {
                    publicURL = getClass().getClassLoader().getResource( publicURL ).toExternalForm();                            
                    return new InputSource(publicURL);
                } else {
                    return new InputSource(new ByteArrayInputStream(new byte[0]));
                }
            }            
            return new InputSource (sid);            
        }
        
        public void startDocument() throws org.xml.sax.SAXException {
            super.startDocument();
            resElemStack = new Stack ();
            resElemStack.push(rootElem);
	    topRE = rootElem;
	    
            elementStack = new Stack ();
            elementStack.push("<root>");  // NOI18N
        }
        
        public void endDocument() throws org.xml.sax.SAXException {
            super.endDocument();
            resElemStack.pop ();
            elementStack.pop ();
        }
        
    }
}
    

