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

package org.openide.util.io;

import java.awt.Image;
import java.io.*;
import java.util.*;

import org.openide.ErrorManager;
import org.openide.util.WeakSet;

// note: keep method NbObjectInputStream.resolveObject
// consistent with replaceObject method

/** Object output stream that could in the future be smart about saving certain objects.
* Also static methods to safely write an object that could cause problems during later deserialization.
*/
public class NbObjectOutputStream extends ObjectOutputStream {
    /** Create a new object output.
    * @param os the underlying output stream
    * @throws IOException for the usual reasons
    */
    public NbObjectOutputStream(OutputStream os) throws IOException {
        super (os);
        try {
            enableReplaceObject (true);
        } catch (SecurityException ex) {
            IOException ioe = new IOException (ex.toString ());
            ErrorManager.getDefault().annotate (ioe, ex);
            throw ioe;
        }
    }

    /*
    * @param obj is an Object to be checked for replace
    */
    public Object replaceObject (Object obj) throws IOException {
        if (obj instanceof Image) {
            return null;
            // [LIGHT]
            // additional code needed for full version
        }
        return super.replaceObject(obj);
    }

    /** Writes an object safely to the object output.
     * Can be read by {@link NbObjectInputStream#readSafely}.
    * @param oo object output to write to
    * @param obj the object to write
    * @exception SafeException if the object simply fails to be serialized
    * @exception IOException if something more serious fails
    */
    public static void writeSafely (ObjectOutput oo, Object obj)
    throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream (200);
        try {
            NbObjectOutputStream oos = new NbObjectOutputStream (bos);
            oos.writeObject (obj);
            oos.flush ();
            bos.close ();
        } catch (Exception exc) {
            // exception during safe of the object
            // encapsulate all exceptions into safe exception
            oo.writeInt (0);
            throw new SafeException (exc);
        }

        oo.writeInt (bos.size ());
        oo.write (bos.toByteArray ());
    }

    private static final String SVUID = "serialVersionUID"; // NOI18N
    private static final Set alreadyReported = new WeakSet(); // Set<String>
    static {
        // See below.
        alreadyReported.add("java.lang.Exception"); // NOI18N
        alreadyReported.add("java.io.IOException"); // NOI18N
        alreadyReported.add("java.util.TreeSet"); // NOI18N
    }
    private final List serializing = new ArrayList(50); // List<Class>
    
    protected void annotateClass(Class cl) throws IOException
    {
        super.annotateClass(cl);
        if (cl.isArray()) {
            return;
        }
        if (cl.isInterface()) {
            // TheInterface.class is being serialized, not an instance;
            // no need for svuid.
            return;
        }
        serializing.add(cl);
        if (isSerialVersionUIDDeclared(cl)) {
            return;
        }
        if (IOException.class.isAssignableFrom(cl)) {
            // ObjectOutputStream for some reason stores IOException's that are
            // thrown during serialization (they are rethrown later maybe?).
            // It's no problem, just ignore them here.
            return;
        }

        String classname = cl.getName();

        if (alreadyReported.add(classname)) {
            Set serializingUniq = new HashSet(); // Set<Class>
            StringBuffer b = new StringBuffer("Serializable class "); // NOI18N
            b.append(classname);
            b.append(" does not declare serialVersionUID field. Encountered while storing: ["); // NOI18N
            Iterator it = serializing.iterator();
            boolean first = true;
            while (it.hasNext()) {
                Class c = (Class)it.next();
                if (c != cl && serializingUniq.add(c)) {
                    if (first) {
                        first = false;
                    } else {
                        b.append(", "); // NOI18N
                    }
                    b.append(c.getName());
                }
            }
            b.append("] See also http://www.netbeans.org/issues/show_bug.cgi?id=19915"); // NOI18N
            String file = System.getProperty("InstanceDataObject.current.file"); // NOI18N
            if (file != null && file.length() > 0) {
                b.append(" [may have been writing "); // NOI18N
                b.append(file);
                b.append("]"); // NOI18N
            }
            ErrorManager.getDefault().log(ErrorManager.WARNING, b.toString());
        }
    }
    
    private static Map examinedClasses = new WeakHashMap(250); // Map<String,Boolean>

    private static boolean isSerialVersionUIDDeclared(Class clazz) {
        String classname = clazz.getName();
        Boolean okay = (Boolean) examinedClasses.get(classname);
        
        if (okay == null) {
            if (classname.equals("java.util.HashSet") || classname.equals("java.util.ArrayList")) { // NOI18N
                // in JDK 1.3 HashSet does not have serialVersionUID but this
                // has been corrected in JDK 1.4.  No need to worry.
                // Ditto ArrayList.
                // Note: java.util.TreeSet in JDk 1.3 also does not have
                // serialVersionUID either and this has *not* been corrected in
                // JDK 1.4
                okay = Boolean.TRUE;
            }
            else {
                okay = Boolean.FALSE;
                java.lang.reflect.Field [] flds = clazz.getDeclaredFields();
                for (int i = 0; i < flds.length; i++) {
                    if (flds[i].getName().equals(SVUID)) {
                        okay = Boolean.TRUE;
                        break;
                    }
                }
            }
            examinedClasses.put(clazz.getName(), okay);
        }
        return okay.booleanValue();
    }
}
