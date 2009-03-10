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

package org.openide.cookies;

import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;

// Should be implemented for example by
// a data object representing a class; a control panel; or a Repository node.
/** Cookie that should be provided by all nodes that are able
* to create a "instance".
* <p>For example, Beans (class form, or serialized) may be instantiated in the usual fashion (say,
* for subsequent serialization); visual components may be instantiated into the Form Editor;
* folders of components may be "instantiated" into a Palette toolbar; and so on.
*
* @author Jaroslav Tulach
*/
public interface InstanceCookie extends Node.Cookie {
    /** The name of {@link #instanceClass}.
    * @return the instance class name
    */
    public String instanceName ();

    /** The representation type that may be created as instances.
    * Can be used to test whether the instance is of an appropriate
    * class without actually creating it.
    *
    * @return the representation class of the instance
    * @exception IOException if an I/O error occurred
    * @exception ClassNotFoundException if a class was not found
    */
    public Class instanceClass ()
    throws java.io.IOException, ClassNotFoundException;

    /** Create an instance.
    * @return the instance of type {@link #instanceClass} (or a subclass)
    * @exception IOException if an I/O error occured
    * @exception ClassNotFoundException if a class was not found
    */
    public Object instanceCreate ()
    throws java.io.IOException, ClassNotFoundException;

    /** Enhanced instance cookie that also knows the file it
    * was created from and can be serialized back to.
    * This could be used, e.g., for Beans that could be saved back to their original location after
    * instantiation and customization.
    */
    public interface Origin extends InstanceCookie {
        /** Returns the origin of the instance.
        * @return the original file
        */
        public FileObject instanceOrigin ();
    }

    /** Enhanced cookie that can answer queries about the type of the
    * instance it creates. It does not bring any additional value, but
    * should improve performance, because it is not necessary to load
    * the actual class of the object into memory.
    *
    * @since 1.4
    */
    public interface Of extends InstanceCookie {
        /** Query to found out if the object created by this cookie is 
        * instance of given type. The same code as:
        * <pre>
        *    Class actualClass = instanceClass ();
        *    result = type.isAsignableFrom (actualClass);
        * </pre>
        * But this can prevent the class <code>actualClass</code> to be
        * loaded into the Java VM.
        *
        * @param type the class type we want to check
        * @return true if this cookie can produce object of given type
        */
        public boolean instanceOf (Class type);
    }
}
