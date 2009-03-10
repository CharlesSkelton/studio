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

package org.openide.nodes;

import java.beans.Beans;
import java.beans.PropertyEditor;
import java.lang.reflect.*;
import java.security.*;

import org.openide.util.NbBundle;

/** Support class for <code>Node.Property</code>.
*
* @see Node.Property
* @author Jan Jancura, Jaroslav Tulach, Ian Formanek
* @version 0.21, Mar 24, 1998
*/
public abstract class PropertySupport extends Node.Property {

    /** flag whether the property is readable */
    private boolean canR;
    /** flag whether the property is writable */
    private boolean canW;

    /** Constructs a new support.
    * @param name        the name of the property
    * @param type        the class type of the property
    * @param displayName the display name of the property
    * @param canR        whether the property is readable
    * @param canW        whether the property is writable
    */
    public PropertySupport(String name, Class type, String displayName, String shortDescription, boolean canR, boolean canW) {
        super(type);
        this.setName(name);
        setDisplayName(displayName);
        setShortDescription(shortDescription);
        this.canR = canR;
        this.canW = canW;
    }

    /* Can read the value of the property.
    * Returns the value passed into constructor.
    * @return <CODE>true</CODE> if the read of the value is supported
    */
    public boolean canRead () {
        return canR;
    }

    /* Can write the value of the property.
    * Returns the value passed into constructor.
    * @return <CODE>true</CODE> if the read of the value is supported
    */
    public boolean canWrite () {
        return canW;
    }

    /** Support for properties from Java Reflection. */
    public static class Reflection extends Node.Property {
        /** Instance of a bean. */
        protected Object instance;
        /** setter method */
        private Method setter;
        /** getter method */
        private Method getter;
        /** class of property editor */
        private Class propertyEditorClass;

        /** Create a support with method objects specified.
        * The methods must be public.
        * @param instance (Bean) object to work on
        * @param valueType type of the property
        * @param getter getter method, can be <code>null</code>
        * @param setter setter method, can be <code>null</code>
        * @throws IllegalArgumentException if the methods are not public
        */
        public Reflection (Object instance, Class valueType, Method getter, Method setter) {
            super (valueType);
            if (getter != null && ! Modifier.isPublic (getter.getModifiers ()))
                throw new IllegalArgumentException ("Cannot use a non-public getter " + getter); // NOI18N
            if (setter != null && ! Modifier.isPublic (setter.getModifiers ()))
                throw new IllegalArgumentException ("Cannot use a non-public setter " + setter); // NOI18N
            this.instance = instance;
            this.setter = setter;
            this.getter = getter;
        }


        /** Create a support with methods specified by name.
        * The instance class will be examined for the named methods.
        * But if the instance class is not public, the nearest public superclass
        * will be used instead, so that the getters and setters remain accessible.
        * @param instance (Bean) object to work on
        * @param valueType type of the property
        * @param getter name of getter method, can be <code>null</code>
        * @param setter name of setter method, can be <code>null</code>
        * @exception NoSuchMethodException if the getter or setter methods cannot be found
        */
        public Reflection (Object instance, Class valueType, String getter, String setter)
        throws NoSuchMethodException {
            this (
                instance, valueType,
                // find the getter ()
                getter == null ? null : findAccessibleClass (instance.getClass ()).getMethod (
                    getter, new Class[0]
                ),
                // find the setter (valueType)
                setter == null ? null : findAccessibleClass (instance.getClass ()).getMethod (
                    setter, new Class[] { valueType }
                )
            );
        }

        /** Find the nearest superclass (or same class) that is public to this one. */
        private static Class findAccessibleClass (Class clazz) {
            if (Modifier.isPublic (clazz.getModifiers ())) {
                return clazz;
            } else {
                Class sup = clazz.getSuperclass ();
                if (sup == null) return Object.class; // handle interfaces
                return findAccessibleClass (sup);
            }
        }

        // [PENDING] should use Beans API in case there is overriding BeanInfo  --jglick
        /** Create a support based on the property name.
        * The getter and setter methods are constructed by capitalizing the first
        * letter in the name of propety and prefixing it with <code>get</code> and
        * <code>set</code>, respectively.
        *
        * @param instance object to work on
        * @param valueType type of the property
        * @param property name of property
        * @exception NoSuchMethodException if the getter or setter methods cannot be found
        */
        public Reflection (Object instance, Class valueType, String property)
        throws NoSuchMethodException {
            this (
                instance, valueType,
                firstLetterToUpperCase (property, "get"), // NOI18N
                firstLetterToUpperCase (property, "set") // NOI18N
            );
        }

        /** Helper method to convert the first letter of a string to uppercase.
        * And prefix the string with some next string.
        */
        private static String firstLetterToUpperCase (String s, String pref) {
            switch (s.length ()) {
            case 0:
                return pref;
            case 1:
                return pref + Character.toUpperCase (s.charAt (0));
            default:
                return pref + Character.toUpperCase (s.charAt (0)) + s.substring (1);
            }
        }

        /* Can read the value of the property.
        * @return <CODE>true</CODE> if the read of the value is supported
        */
        public boolean canRead () {
            return getter != null;
        }

        /* Getter for the value.
        * @return the value of the property
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public Object getValue () throws
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (getter == null) throw new IllegalAccessException ();
            Object valideInstance = Beans.getInstanceOf (instance, getter.getDeclaringClass());
            try {
                return getter.invoke (valideInstance, new Object [0]);
            } catch (IllegalAccessException ex) {
                try {
                    getter.setAccessible(true);
                    return getter.invoke (valideInstance, new Object [0]);
                } finally {
                    getter.setAccessible(false);
                }
            }
        }

        /* Can write the value of the property.
        * @return <CODE>true</CODE> if the read of the value is supported
        */
        public boolean canWrite () {
            return setter != null;
        }

        /* Setter for the value.
        * @param val the value of the property
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public void setValue (Object val) throws
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (setter == null) throw new IllegalAccessException ();
            Object valideInstance = Beans.getInstanceOf (instance, setter.getDeclaringClass());
            try {
                setter.invoke (valideInstance, new Object [] {val});
            } catch (IllegalAccessException ex) {
                try {
                    setter.setAccessible(true); 
                    setter.invoke (valideInstance, new Object [] {val});
                } finally {
                    setter.setAccessible(false);
                }
            }
        }
        
        /* Returns property editor for this property.
        * @return the property editor or <CODE>null</CODE> if there should not be
        *    any editor.
        */
        public PropertyEditor getPropertyEditor () {
            if (propertyEditorClass != null)
                try {
                    return (PropertyEditor) propertyEditorClass.newInstance ();
                } catch (InstantiationException ex) {
                } catch (IllegalAccessException iex) {
                }
            return super.getPropertyEditor ();
        }

        /** Set the property editor explicitly.
        * @param clazz class type of the property editor
        */
        public void setPropertyEditorClass (Class clazz) {
            propertyEditorClass = clazz;
        }
    }

    /** A simple read/write property.
    * Subclasses should implement
    * {@link #getValue} and {@link #setValue}.
    */
    public static abstract class ReadWrite extends PropertySupport {
        /** Construct a new support.
        * @param name        the name of the property
        * @param type        the class type of the property
        * @param displayName the display name of the property
        * @param shortDescription a short description of the property
        */
        public ReadWrite(String name, Class type, String displayName,
                         String shortDescription) {
            super(name, type, displayName, shortDescription, true, true);
        }
    }

    /** A simple read-only property.
    * Subclasses should implement {@link #getValue}.
    */
    public static abstract class ReadOnly extends PropertySupport {
        /** Construct a new support.
        * @param name        the name of the property
        * @param type        the class type of the property
        * @param displayName the display name of the property
        * @param shortDescription a short description of the property
        */
        public ReadOnly(String name, Class type, String displayName,
                        String shortDescription) {
            super(name, type, displayName, shortDescription, true, false);
        }

        /* Setter for the value.
        * @param val the value of the property
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public void setValue (Object val) throws
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            throw new IllegalAccessException("Cannot write to ReadOnly property"); // NOI18N
        }
    }

    /** A simple write-only property.
    * Subclasses should implement {@link #setValue}.
    */
    public static abstract class WriteOnly extends PropertySupport {
        /** Construct a new support.
        * @param name        the name of the property
        * @param type        the class type of the property
        * @param displayName the display name of the property
        * @param shortDescription a short description of the property
        */
        public WriteOnly(String name, Class type, String displayName,
                         String shortDescription) {
            super(name, type, displayName, shortDescription, false, true);
        }

        /* Getter for the value.
        * @return the value of the property
        * @exception IllegalAccessException cannot access the called method
        * @exception IllegalArgumentException wrong argument
        * @exception InvocationTargetException an exception during invocation
        */
        public Object getValue () throws
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            throw new IllegalAccessException("Cannod read from WriteOnly property"); // NOI18N
        }
    }

    /** Support for the name property of a node. Delegates {@link #setValue} and {@link #getValue}
    * to {@link Node#setName} and {@link Node#getName}.
    * <p>(Final only for performance, can be unfinaled if desired).
    */
    public static final class Name extends PropertySupport {
        /** The node to which we delegate the work. */
        private final Node node;

        /** Create the name property for a node with the standard name and hint.
        * @param node the node
        */
        public Name (final Node node) {
            this(node,
                 NbBundle.getBundle(PropertySupport.class).getString("CTL_StandardName"),
                 NbBundle.getBundle(PropertySupport.class).getString("CTL_StandardHint"));
        }

        /** Create the name property for a node.
        * @param node the node
        * @param propName name of the "name" property
        * @param hint hint message for the "name" property
        */
        public Name (final Node node, final String propName, final String hint) {
            super(Node.PROP_NAME, String.class, propName, hint,
                  true, node.canRename());
            this.node = node;
        }

        /* Getter for the value. Delegates to Node.getName().
        * @return the name
        */
        public Object getValue ()
        throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
            return node.getName();
        }

        /* Setter for the value. Delegates to Node.setName().
        * @param val new name
        */
        public void setValue (Object val)
        throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
            if (!(val instanceof String)) throw new IllegalAccessException ();
            Object oldName = node.getName();
            node.setName((String)val);
            node.firePropertyChange(Node.PROP_NAME, oldName, val);
        }

    } // end of Name inner class

}
