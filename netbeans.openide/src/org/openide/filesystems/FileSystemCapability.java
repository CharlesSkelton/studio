/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.filesystems;

import java.beans.*;
import java.util.*;

import org.openide.util.enums.FilterEnumeration;

/** This class defines the capabilities of a filesystem to
* take part in different operations. Some filesystems are
* not designed to allow compilation on them, some do not want
* to be present in class path when executing or debugging 
* a program. 
* <P>
* Moreover there can be additional capabilities to check
* and this class defines ways how one can communicated with
* a filesystem to find out whether the system is "capable"
* enough to be used in the operation.
*
* @author Jaroslav Tulach
*/
public class FileSystemCapability extends Object {
    /** Object that is capable of every thing.
    */
    public static final FileSystemCapability ALL = new FileSystemCapability () {
                public boolean capableOf (FileSystemCapability c) {
                    return true;
                }
            };

    /** Well known capability of being compiled.
     * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
     */
    public static final FileSystemCapability COMPILE = new FileSystemCapability ();
    /** Well known ability to be executed.
     * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
     */
    public static final FileSystemCapability EXECUTE = new FileSystemCapability ();
    /** Well known ability to be debugged.
     * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
     */
    public static final FileSystemCapability DEBUG = new FileSystemCapability ();
    /** Well known ability to contain documentation files */
    public static final FileSystemCapability DOC = new FileSystemCapability ();


    /** Basic operation that tests whether this object
    * is capable to do different capability.
    * <P>
    * The default implementation claims that it is 
    * capable to handle only identical capability (==).
    * 
    * @param c capability to test 
    * @return true if yes
    */
    public boolean capableOf (FileSystemCapability c) {
        return c == this;
    }

    /** All filesystems that are capable of this capability.
    * @return enumeration of FileSystems that satifies this capability
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public Enumeration fileSystems () {
        return new FilterEnumeration (ExternalUtil.getRepository ().fileSystems ()) {
                   public boolean accept (Object o) {
                       FileSystem fs = (FileSystem)o;
                       return fs.getCapability().capableOf(FileSystemCapability.this);
                   }
               };
    }

    /** Find a resource in repository, ignoring not capable filesystems.
    * @param resName name of the resource
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public FileObject findResource (String resName) {
        Enumeration en = fileSystems ();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.findResource(resName);
            if (fo != null) {
                // object found
                return fo;
            }
        }
        return null;
    }

    /** Searches for the given resource among all filesystems
    * that satifies this capability, returning all matches.
    * @param name name of the resource
    * @return enumeration of {@link FileObject}s
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public Enumeration findAllResources(String name) {
        Vector v = new Vector(8);
        Enumeration en = fileSystems ();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.findResource(name);
            if (fo != null) {
                v.addElement(fo);
            }
        }
        return v.elements();
    }

    /** Finds file when its name is provided. It scans in the list of
    * filesystems and asks them for the specified file by a call to
    * {@link FileSystem#find find}. The first object that is found is returned or <CODE>null</CODE>
    * if none of the filesystems contain such a file.
    *
    * @param aPackage package name where each package is separated by a dot
    * @param name name of the file (without dots) or <CODE>null</CODE> if
    *    one wants to obtain the name of a package and not a file in it
    * @param ext extension of the file or <CODE>null</CODE> if one needs
    *    a package and not a file name
    *
    * @return {@link FileObject} that represents file with given name or
    *   <CODE>null</CODE> if the file does not exist
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public final FileObject find (String aPackage, String name, String ext) {
        Enumeration en = fileSystems ();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.find (aPackage, name, ext);
            if (fo != null) {
                // object found
                return fo;
            }
        }
        return null;
    }

    /** Finds all files among all filesystems with this capability
    * that match a given name, returning all matches.
    * All filesystems are queried with {@link FileSystem#find}.
    *
    * @param aPackage package name where each package is separated by a dot
    * @param name name of the file (without dots) or <CODE>null</CODE> if
    *    one wants to obtain the name of a package and not a file in it
    * @param ext extension of the file or <CODE>null</CODE> if one needs
    *    a package and not a file name
    *
    * @return enumeration of {@link FileObject}s
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public final Enumeration findAll (String aPackage, String name, String ext) {
        Enumeration en = fileSystems ();
        Vector ret = new Vector();
        while (en.hasMoreElements ()) {
            FileSystem fs = (FileSystem)en.nextElement ();
            FileObject fo = fs.find (aPackage, name, ext);
            if (fo != null) {
                ret.addElement(fo);
            }
        }
        return ret.elements();
    }

    /** Adds PropertyChange listener. Every class which implements changes of capabilities
    * has to implement it's property change support.
    * @param l the listener to be added.
    */
    public synchronized void addPropertyChangeListener (PropertyChangeListener l) {}

    /** Removes PropertyChange listener. Every class which implements changes of capabilities
    * has to implement it's property change support.
    * @param l the listener to be removed.
    */
    public void removePropertyChangeListener (PropertyChangeListener l) {}


    /** Default implementation of capabilities, that behaves like
    * JavaBean and allows to set whether the well known 
    * capabilities (like compile, execute) should be enabled
    * or not.
    */
    public static class Bean extends FileSystemCapability implements java.io.Serializable {
        /** change listeners */
        private transient PropertyChangeSupport supp;

        /** compilation */
        private boolean compilation = true;
        /** execution */
        private boolean execution = true;
        /** debugging */
        private boolean debug = true;
        /** doc */
        private boolean doc = false;

        static final long serialVersionUID =627905674809532736L;
        /** Checks for well known capabilities and if they are allowed.
        * 
        * @param c capability to test 
        * @return true if yes
        */
        public boolean capableOf (FileSystemCapability c) {
            if (c == COMPILE) return compilation;
            if (c == EXECUTE) return execution;
            if (c == DEBUG) return debug;
            if (c == DOC) return doc;
            if (c == ALL) return true;

            if (!(c instanceof Bean)) {
                return false;
            }

            // try match of values
            Bean b = (Bean)c;

            return
                compilation == b.compilation &&
                execution == b.execution &&
                debug == b.debug &&
                doc == b.doc;
        }

        /** Getter for value of compiling capability.
         * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public boolean getCompile () {
            return compilation;
        }

        /** Setter for allowing compiling capability.
         * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public void setCompile(boolean val) {
            if (val != compilation) {
                compilation = val;
                if (supp != null) {
                    supp.firePropertyChange ("compile", // NOI18N
                                             !val ? Boolean.TRUE : Boolean.FALSE,
                                             val ? Boolean.TRUE : Boolean.FALSE);
                }
            }
        }

        /** Getter for value of executiong capability.
         * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public boolean getExecute () {
            return execution;
        }

        /** Setter for allowing executing capability.
         * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public void setExecute (boolean val) {
            if (val != execution) {
                execution = val;
                if (supp != null) {
                    supp.firePropertyChange ("execute", // NOI18N
                                             !val ? Boolean.TRUE : Boolean.FALSE,
                                             val ? Boolean.TRUE : Boolean.FALSE);
                }
            }
        }

        /** Getter for value of debugging capability.
         * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public boolean getDebug () {
            return debug;
        }

        /** Setter for allowing debugging capability.
         * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public void setDebug (boolean val) {
            if (val != debug) {
                debug = val;
                if (supp != null) {
                    supp.firePropertyChange ("debug", // NOI18N
                                             !val ? Boolean.TRUE : Boolean.FALSE,
                                             val ? Boolean.TRUE : Boolean.FALSE);
                }
            }
        }

        /** Getter for value of doc capability.
        */
        public boolean getDoc () {
            return doc;
        }

        /** Setter for allowing debugging capability.
        */
        public void setDoc (boolean val) {
            if (val != doc) {
                doc = val;
                if (supp != null) {
                    supp.firePropertyChange ("doc", // NOI18N
                                             !val ? Boolean.TRUE : Boolean.FALSE,
                                             val ? Boolean.TRUE : Boolean.FALSE);
                }
            }
        }

        public synchronized void addPropertyChangeListener (PropertyChangeListener l) {
            if (supp == null) {
                supp = new PropertyChangeSupport (this);
            }
            supp.addPropertyChangeListener (l);
        }

        public void removePropertyChangeListener (PropertyChangeListener l) {
            if (supp != null) {
                supp.removePropertyChangeListener (l);
            }
        }
    }
}
