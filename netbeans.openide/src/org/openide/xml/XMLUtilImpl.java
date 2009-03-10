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

package org.openide.xml;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.w3c.dom.Document;

import org.openide.util.Lookup;
import org.openide.ErrorManager;

/**
 * This backend class for XMLUtil contains implementation of DOM implementation
 * "independent" write method.
 *
 * @author  Petr Kuzel
 * @version reflection based implemetation
 */
class XMLUtilImpl extends Object {

    
    /**
     * Write Document into out using introspection.
     *
     * @param out must be Writer or OutputStream
     * @param XML encoding name (ignored parametr)
     */
    static void write(Document doc, Object out, String encoding) throws IOException {
        
        Class dock = doc.getClass();
                        
        // no implementation neutral write exist
        try {
            if ("com.sun.xml.tree.XmlDocument".equals(dock.getName())           //NOI18N
            || "org.apache.crimson.tree.XmlDocument".equals(dock.getName())) {  //NOI18N
                
                // these DOM implementations are self writing
		Method write;
		
		if (out instanceof Writer) {
		    write = dock.getDeclaredMethod("write", new Class[] {Writer.class});//NOI18N
		} else {
		    write = dock.getDeclaredMethod("write", new Class[] {OutputStream.class});//NOI18N
		}
                write.invoke(doc,new Object[] {out});            
                
            } else {
                
                // try apache's serialize package
                // using introspection because calling implementation
                // specific methods 
                // may change as they prove to be stable
                
                ClassLoader cl = (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class);
                if (cl == null) cl = XMLUtilImpl.class.getClassLoader();
                
                Class serka = 
                    Class.forName("org.apache.xml.serialize.XMLSerializer", true, cl);   //NOI18N

                Class forka =
                    Class.forName("org.apache.xml.serialize.OutputFormat", true, cl);    //NOI18N
                
                Object serin = serka.newInstance();                
                Object forin = forka.newInstance();

                // hopefully it could improve output readability
                
                Method setmet = null;
                
                setmet = forka.getMethod("setMethod", new Class[] {String.class}); //NOI18N                
                setmet.invoke(forin, new Object[] {"xml"});                        //NOI18N                
                
                setmet = forka.getMethod("setIndenting", new Class[] {Boolean.TYPE}); //NOI18N                
                setmet.invoke(forin, new Object[] {Boolean.TRUE});                    //NOI18N

                setmet = forka.getMethod("setLineWidth", new Class[] {Integer.TYPE}); //NOI18N                
                setmet.invoke(forin, new Object[] {new Integer(0)});                  //NOI18N                
                
                String detectedEncoding = null;
                Method init;
                
                if (out instanceof OutputStream) {
                    init = serka.getMethod("setOutputByteStream", new Class[] {OutputStream.class});  //NOI18N
                    init.invoke(serin, new Object[] {out});                                            
                    
                    // set encoding in output format                                        
                    detectedEncoding = encoding;
                    
                    
                } else if (out instanceof Writer) {
                    init = serka.getMethod("setOutputCharStream", new Class[] {Writer.class});  //NOI18N
                    init.invoke(serin, new Object[] {out});                                                        
                    
                    //detect (same as Crimson did) and set encoding
                    if (out instanceof OutputStreamWriter) {
                        detectedEncoding = ((OutputStreamWriter)out).getEncoding();  
                        // no general way how to transorm XML encoding names to Java names
                        // and vice versa
                        detectedEncoding = null;
                    } else {
                        detectedEncoding = null;
                    }
                    
                } else {
                   throw new ClassCastException("OutputStream or Writer expected.");  //NOI18N
                }

                Method setenc = forka.getMethod("setEncoding", new Class[] {String.class});  //NOI18N              
                setenc.invoke(forin, new Object[] {detectedEncoding} );                
                
                Method setout = serka.getMethod("setOutputFormat", new Class[] {forka});     //NOI18N
                setout.invoke(serin, new Object[] {forin});                
                
                Method asDOM = serka.getMethod("asDOMSerializer", new Class[0]);//NOI18N
                Object impl = asDOM.invoke(serin, new Object[0]);

                Method serialize = impl.getClass().getMethod("serialize", new Class[] {Document.class}); //NOI18N
                serialize.invoke(impl, new Object[] {doc});
                  
            }
            
        } catch (IllegalAccessException ex) {
            handleImplementationException(ex);
        } catch (InstantiationException ex) {
            handleImplementationException(ex);
        } catch (IllegalArgumentException ex) {
            handleImplementationException(ex);
        } catch (NoSuchMethodException ex) {
            handleImplementationException(ex);
        } catch (ClassNotFoundException ex) {
            handleImplementationException(ex);
        } catch (InvocationTargetException ex) {
            handleTargetException(ex);
        }
    
    }
    
    
    /** TargetException handler */
    private static void handleTargetException(InvocationTargetException ex) throws IOException {
        Throwable t = ex.getTargetException();
        if (t instanceof IOException) {
            throw (IOException) t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } 
    }
    
    private static void handleImplementationException(Exception ex) throws IOException {
        ErrorManager err = ErrorManager.getDefault();
        IOException explained = new IOException("Unsupported DOM document implementation!"); // NOI18N
        err.annotate(explained, ex);
        throw explained;
    }
    
}
