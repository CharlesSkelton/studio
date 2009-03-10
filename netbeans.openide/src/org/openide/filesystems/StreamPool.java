/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

/*
 * StreamPool.java
 *
 * Created on March 2, 2001, 9:21 AM
 */

package org.openide.filesystems;
import org.openide.util.*;

import java.util.*;
import java.io.*;

/**
 * This class keeps info about streams (these streams are registered) that was
 * not closed yet. Also for every issued stream is hold stracktrace.
 * Sometimes there is necessary to know who didn`t close stream.
 *
 * @author  rmatous
 * @version
 */
final class StreamPool extends Object {
    private static Map    fo2StreamPool = new WeakHashMap ();
    private static Map    fs2StreamPool  = new WeakHashMap ();
    
    private Set iStreams;
    private Set oStreams;
    
    /** Creates new StreamPool */
    private  StreamPool() {
    }
    
    /**
     * This method creates subclassed  NotifyInputStream (extends InputStream).
     * NotifyInputStream saves stacktrace in constrcuctor (creates new Exception) that
     * is used in method annotate.
     * This method also register this NotifyInputStream as
     * mapping (AbstractFolder, NotifyInputStream) and
     * mapping (AbstractFolder.getFileSystem(), NotifyInputStream).
     * If NotifyInputStream is closed then registration is freed.
     * For fo is also created StreamPool unless it exists yet.
     * @param fo FileObject that issues is
     * @param InputStream that should be issued
     * @return subclassed InputStream that is registered as mentioned above */
    public static synchronized InputStream createInputStream (AbstractFolder fo, InputStream is) {
        InputStream retVal = new NotifyInputStream (fo, is);
        get (fo).iStream ().add (retVal);
        get (fo.getFileSystem()).iStream ().add (retVal);
        return retVal;
    }
    
    /** This method creates subclassed  NotifyOutputStream (extends OutputStream).
     * NotifyOutputStream saves stacktrace in constrcuctor (creates new Exception) that
     * is used in method annotate.
     * This method also register this NotifyOutputStream as
     * mapping (AbstractFolder, NotifyOutputStream) and
     * mapping (AbstractFolder.getFileSystem(), NotifyOutputStream).
     * If NotifyOutputStream is closed then registration is freed.
     * For fo is also created StreamPool unless it exists yet.
     * @return subclassed OutputStream that is registered as mentioned above
     * @param fireFileChanged defines if should be fired fileChanged event after close of stream
     * @param fo FileObject that issues is
     * @param os OutputStream that should be issued */
    public static synchronized OutputStream createOutputStream (AbstractFolder fo, OutputStream os, boolean fireFileChanged) {
        OutputStream retVal = new NotifyOutputStream (fo, os, fireFileChanged);        
        get (fo).oStream ().add (retVal);
        get (fo.getFileSystem()).oStream ().add (retVal);
        return retVal;
    }
    
    /**
     * This method finds StreamPool assiciated with fo or null. This StreamPool is
     * created by means of createInputStream or createOutputStream.
     * @param fo FileObject whose StreamPool is looked for
     * @return  StreamPool or null*/
    public static synchronized StreamPool find (FileObject fo) {
        return (StreamPool) fo2StreamPool.get (fo);
    }
    
    /**
     * This method finds StreamPool assiciated with fs or null. This StreamPool is
     * created by means of createInputStream or createOutputStream.
     * @param fs FileSystem whose StreamPool is looked for
     * @return  StreamPool or null*/
    public static synchronized StreamPool find (FileSystem fs) {
        return (StreamPool) fs2StreamPool.get (fs);
    }
    
    /**
     * Annotates ex with all exceptions of unclosed streams.
     * @param ex that should be annotated */
    public void annotate (Exception ex) {
        synchronized (StreamPool.class) {
            if (iStreams != null) {
                Iterator itIs = iStreams.iterator ();
                NotifyInputStream   nis;
                while (itIs.hasNext()) {
                    nis = (NotifyInputStream) itIs.next();
                    Exception annotation = nis.getException ();
                    if (annotation != null) ExternalUtil.annotate (ex,annotation);
                }
                
            }

            if (oStreams != null) {
                Iterator itOs = oStreams.iterator ();            
                NotifyOutputStream  nos;            
                while (itOs.hasNext()) {
                    nos = (NotifyOutputStream) itOs.next();
                    Exception annotation = nos.getException ();                    
                    if (annotation != null) ExternalUtil.annotate (ex,annotation);
                }
            }
        }
    }
    
    /**
     * @return  true if there is any InputStream that was not closed yet  */
    public  boolean isInputStreamOpen () {
        return iStreams != null && !iStreams.isEmpty ();
    }
    
    /**
     * @return  true if there is any OutputStream that was not closed yet  */
    public  boolean isOutputStreamOpen () {
        return oStreams != null && !oStreams.isEmpty ();        
    }
    
    
    
    
    /** All next methods are private (Not visible outside this class)*/
    
    
    
    
    private static  StreamPool get (FileObject fo) {
        StreamPool strPool = (StreamPool) fo2StreamPool.get (fo);
        if (strPool == null) fo2StreamPool.put( fo, strPool = new StreamPool ());
        
        return strPool;
    }

    private static StreamPool get (FileSystem fs) {    
        StreamPool strPool = (StreamPool) fs2StreamPool.get (fs);
        if (strPool == null) fs2StreamPool.put( fs, strPool = new StreamPool ());

        return strPool;        
    }
    
    private Set iStream () {
        if (iStreams == null) iStreams = new WeakSet ();
        return iStreams;
    }
    
    private Set oStream () {
        if (oStreams == null) oStreams = new WeakSet ();
        return oStreams;
    }
    
    /** fireFileChange defines if should be fired fileChanged event after close of stream*/
    private static void closeOutputStream (AbstractFolder fo, OutputStream os, boolean fireFileChanged) {
        StreamPool foPool = find (fo);
        StreamPool fsPool = find (fo.getFileSystem ());
        Set  foSet = (foPool != null)? foPool.oStreams : null;       
        Set  fsSet = (fsPool != null)? fsPool.oStreams : null;               
        
        removeStreams (fsSet, foSet, os);
        removeStreamPools (fsPool, foPool, fo);
        fo.outputStreamClosed (fireFileChanged);        
    }
    
    
    private static void closeInputStream (AbstractFolder fo, InputStream is) {
        StreamPool foPool = find (fo);
        StreamPool fsPool = find (fo.getFileSystem ());
        Set  foSet = (foPool != null)? foPool.iStreams : null;       
        Set  fsSet = (fsPool != null)? fsPool.iStreams : null;               
        
        removeStreams (fsSet, foSet, is);
        removeStreamPools (fsPool, foPool, fo);
    }

    
    
    private  static synchronized void removeStreams (Set fsSet,Set foSet,Object stream) {
        if (foSet != null) foSet.remove(stream);
        if (fsSet != null) fsSet.remove(stream);        
    }
            
    private  static synchronized void removeStreamPools (StreamPool fsPool,StreamPool foPool,AbstractFolder fo) {        
        boolean isIStreamEmpty = (foPool == null || foPool.iStreams == null || foPool.iStreams.isEmpty());
        boolean isOStreamEmpty = (foPool == null || foPool.oStreams == null || foPool.oStreams.isEmpty());        
                
        
        if (isIStreamEmpty && isOStreamEmpty) 
            fo2StreamPool.remove(fo);
        

        isIStreamEmpty = (fsPool == null || fsPool.iStreams == null || fsPool.iStreams.isEmpty());
        isOStreamEmpty = (fsPool == null || fsPool.oStreams == null || fsPool.oStreams.isEmpty());                
        
        if (isIStreamEmpty && isOStreamEmpty) 
            fs2StreamPool.remove(fo.getFileSystem ());        
    }
  
    
    static final class NotifyOutputStream extends FilterOutputStream {
        private Exception ex;
        AbstractFolder fo;
        /** defines if should be fired fileChanged event after close of stream */
        private boolean fireFileChanged;
        public NotifyOutputStream (AbstractFolder fo, OutputStream os, boolean fireFileChanged) {
            super (os);
            this.fo = fo;
            ex = new Exception ();
            this.fireFileChanged = fireFileChanged;
        }
        
        /** Faster implementation of writing than is implemented in
         * the filter output stream.
         */
        public void write (byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }
        
        public void close ()  throws IOException {
            if (ex != null) {
                ex = null;
                super.close ();
                closeOutputStream (fo, this, fireFileChanged);
            }
        }
        public Exception getException () {
            return ex;
        }
    }
    
    static final class NotifyInputStream extends FilterInputStream {
        private Exception ex;
        AbstractFolder fo;
        public NotifyInputStream (AbstractFolder fo, InputStream is) {
            super (is);
            this.fo = fo;            
            ex = new Exception ();            
        }
        public void close ()  throws IOException {
            if (ex != null) {            
                ex = null;                
                super.close ();
                closeInputStream  (fo, this);
            }
        }
        
        public Exception getException () {
            return ex;
        }
    }
    
}
