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

package org.openide.options;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.openide.ErrorManager;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.SharedClassObject;

/** Base class for all system options.
* Provides methods for adding
* and working with property change and guarantees
* that all instances of the same class will share these listeners.
* <P>
* When a new option is created, it should subclass
* <CODE>SystemOption</CODE>, add <em>static</em> variables to it that will hold
* the values of properties, and write non-static setters/getters that will
* notify all listeners about property changes via
* {@link #firePropertyChange}.
* <p>JavaBeans introspection is used to find the properties,
* so it is possible to use {@link BeanInfo}.
*
* @author Jaroslav Tulach
*/
public abstract class SystemOption extends SharedClassObject implements HelpCtx.Provider {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 558589201969066966L;

    /** property to indicate that the option is currently loading its data */
    private static final Object PROP_LOADING = new Object ();
    /** property to indicate that the option is currently loading its data */
    private static final Object PROP_STORING = new Object ();

    /** Default constructor. */
    public SystemOption() {
        // SystemOption must declare this property in order to be correctly deserialized
        // by SharedClassObject.findObject function
        putProperty ("netbeans.systemoption.hack", null); // NOI18N
    }

    /** Fire a property change event to all listeners. Delays
    * this loading when readExternal is active till it finishes.
    *
    * @param name the name of the property
    * @param oldValue the old value
    * @param newValue the new value
    */
    protected void firePropertyChange (
        String name, Object oldValue, Object newValue
    ) {
        if (getProperty (PROP_LOADING) != null) {
            // somebody is loading, assign any object different than
            // this to indicate that firing should occure
            putProperty (PROP_LOADING, PROP_LOADING);
            // but do not fire the change now
            return;
        }
        super.firePropertyChange (name, oldValue, newValue);
    }

    /** Write all properties of this object (or subclasses) to an object output.
    * @param out the output stream
    * @exception IOException on error
    */
    public void writeExternal (ObjectOutput out) throws IOException {
        try {
            // gets info about all properties that were added by subclass
            BeanInfo info = org.openide.util.Utilities.getBeanInfo (getClass (), SystemOption.class);
            PropertyDescriptor[] desc = info.getPropertyDescriptors ();

            putProperty (PROP_STORING, this);

            Object[] param = new Object[0];
            synchronized (getLock ()) {
                // write all properties that have getter to stream
                for (int i = 0; i < desc.length; i++) {
                    // skip readonly Properties
                    if (desc[i].getWriteMethod () == null) {
                        continue;
                    }
                    
                    String propName = desc[i].getName();
                    Object value = getProperty(propName);
                    boolean fromRead;


                    // JST: this code handles the case when somebody needs to store
                    // different value then is the value of get/set method.
                    // in such case value (from getProperty) is not of the type
                    // of the getter/setter and is used instead of the value from getXXXX
                    Method read = desc[i].getReadMethod();
                    if (read == null) {
                        continue;
                    }
                    if (value == null || isInstance(desc[i].getPropertyType(), value)) {
                        fromRead = true;
                        try {
                            value = read.invoke (this, param);
                        } catch (InvocationTargetException ex) {
                            // exception thrown
                            IOException ne = new IOException (NbBundle.getMessage (
                                                                  SystemOption.class,
                                                                  "EXC_InGetter",
                                                                  getClass (),
                                                                  desc[i].getName ()
                                                              ));
                            ErrorManager.getDefault ().annotate (ne, ex);
                            throw ne;
                        } catch (IllegalAccessException ex) {
                            // exception thrown
                            IOException ne = new IOException (NbBundle.getMessage (
                                                                  SystemOption.class,
                                                                  "EXC_InGetter",
                                                                  getClass (),
                                                                  desc[i].getName ()
                                                              ));
                            ErrorManager.getDefault ().annotate (ne, ex);
                            throw ne;
                        }
                    } else {
                        fromRead = false;
                    }
                    // writes name of the property
                    out.writeObject (propName);
                    // writes its value
                    out.writeObject (value);
                    // from getter or stored prop?
                    out.writeObject(fromRead ? Boolean.TRUE : Boolean.FALSE);
                }
            }
        } catch (IntrospectionException ex) {
            // if we cannot found any info about properties
        } finally {
            putProperty (PROP_STORING, null);
        }
        // write null to signal end of properties
        out.writeObject (null);
    }
    
    /** Returns true if the object is assignable to the class.
     * Also if the class is primitive and the object is of the matching wrapper type.
     */
    private static boolean isInstance(Class c, Object o) {
        return c.isInstance(o) ||
            (c == Byte.TYPE && (o instanceof Byte)) ||
            (c == Short.TYPE && (o instanceof Short)) ||
            (c == Integer.TYPE && (o instanceof Integer)) ||
            (c == Long.TYPE && (o instanceof Long)) ||
            (c == Float.TYPE && (o instanceof Float)) ||
            (c == Double.TYPE && (o instanceof Double)) ||
            (c == Boolean.TYPE && (o instanceof Boolean)) ||
            (c == Character.TYPE && (o instanceof Character));
    }

    /** Read all properties of this object (or subclasses) from an object input.
    * If there is a problem setting the value of any property, that property will be ignored;
    * other properties should still be set.
    * @param in the input stream
    * @exception IOException on error
    * @exception ClassNotFound if a class used to restore the system option is not found
    */
    public void readExternal (ObjectInput in)
    throws IOException, ClassNotFoundException {
            // hashtable that maps names of properties to setter methods
            HashMap map = new HashMap ();

        try {
            synchronized (getLock ()) {
                // indicate that we are loading files
                putProperty (PROP_LOADING, this);

                try {
                    // gets info about all properties that were added by subclass
                    BeanInfo info = org.openide.util.Utilities.getBeanInfo (getClass (), SystemOption.class);
                    PropertyDescriptor[] desc = info.getPropertyDescriptors ();

                    // write all properties that have getter to stream
                    for (int i = 0; i < desc.length; i++) {
                        Method m = desc[i].getWriteMethod ();
                        /*if (m == null) {
                          System.out.println ("HOW HOW HOW HOWHOWHOWHOWHWO: " + desc[i].getName() + " XXX " + getClass());
                          throw new IOException (new MessageFormat (NbBundle.getBundle (SystemOption.class).getString ("EXC_InSetter")).
                            format (new Object[] {getClass (), desc[i].getName ()})
                                                );
                    } */
                        map.put (desc[i].getName (), m );
                    }
                } catch (IntrospectionException ex) {
                    // if we cannot found any info about properties
                    // leave the hashtable empty and only read stream till null is found
                    ErrorManager.getDefault().notify (
			ErrorManager.INFORMATIONAL, ex);
                }

                String preread = null;
                do {
                    // read the name of property
                    String name;
                    if (preread != null) {
                        name = preread;
                        preread = null;
                    } else {
                        name = (String)in.readObject();
                    }

                    // break if the end of property stream is found
                    if (name == null) break;

                    // read the value of property
                    Object value = in.readObject ();

                    // read flag - use the setter method or store as property?
                    Object useMethodObject = in.readObject();
                    boolean useMethod;
                    boolean nullRead = false; // this should be last processed property?
                    if (useMethodObject == null) {
                        useMethod = true;
                        nullRead = true;
                    } else if (useMethodObject instanceof String) {
                        useMethod = true;
                        preread = (String) useMethodObject;
                    } else {
                        useMethod = ((Boolean) useMethodObject).booleanValue();
                    }

                    if (useMethod) {

                        // set the value
                        Method write = (Method)map.get (name);
                        if (write != null) {
                            // if you have where to set the value
                            try {
                                write.invoke (this, new Object[] { value });
                            } catch (Exception ex) {
                                ErrorManager.getDefault ().notify (
                                    ErrorManager.INFORMATIONAL,
                                    ex
                                );
                            }
                        }
                    } else {
                        putProperty(name, value, false);
                    }

                    if (nullRead) {
                        break;
                    }

                } while (true);
            }
        } finally {
            // get current state
            if (this != getProperty (PROP_LOADING)) {
                // some changes should be fired
                // loading finished
                putProperty (PROP_LOADING, null);
                firePropertyChange (null, null, null);
            } else {
                // loading finished
                putProperty (PROP_LOADING, null);
            }
        }
    }

    protected boolean clearSharedData () {
        return false;
    }

    /**
    * Get the name of this system option.
    * The default implementation just uses the {@link #displayName display name}.
    * @return the name
    */
    public final String getName () {
        return displayName ();
    }

    /**
    * Get the display name of this system option.
    * @return the display name
    */
    public abstract String displayName ();

    /** Get context help for this system option.
    * @return context help
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (SystemOption.class);
    }

    /** Allows subclasses to test whether the change of a property
    * is invoked from readExternal method or by external change invoked
    * by any other program.
    *
    * @return true if the readExternal method is in progress
    */
    protected final boolean isReadExternal () {
        return getProperty (PROP_LOADING) != null;
    }

    /** Allows subclasses to test whether the getter of a property
    * is invoked from writeExternal method or by any other part of the program.
    *
    * @return true if the writeExternal method is in progress
    */
    protected final boolean isWriteExternal () {
        return getProperty (PROP_STORING) != null;
    }
}
