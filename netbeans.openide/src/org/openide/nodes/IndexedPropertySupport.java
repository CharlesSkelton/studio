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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openide.util.Utilities;

/** Support for indexed properties.
*
* @see Node
*
* @author Jan Jancura
* @version 0.14, Jan 20, 1998
*/
public class IndexedPropertySupport extends Node.IndexedProperty {
    /** Instance of the bean. */
    protected Object instance;

    /** setter method */
    private Method setter;

    /** getter method */
    private Method getter;

    /** indexed setter method */
    private Method indexedSetter;

    /** indexed getter method */
    private Method indexedGetter;

    /** Constructor.
    * @param instance the bean for which these properties exist
    * @param valueType type of the entire property
    * @param elementType type of one element of the property
    * @param getter get method for the entire property
    * @param setter set method for the entire property
    * @param indexedGetter get method for one element
    * @param indexedSetter set method for one element
    */
    public IndexedPropertySupport (
        Object instance,
        Class valueType,
        Class elementType,
        Method getter,
        Method setter,
        Method indexedGetter,
        Method indexedSetter) {
        super (valueType, elementType);
        this.instance = instance;
        this.setter = setter;
        this.getter = getter;
        this.indexedSetter = indexedSetter;
        this.indexedGetter = indexedGetter;
    }

    /* Setter for display name.
    * @param s the string
    */
    public final void setDisplayName (String s) {
        super.setDisplayName (s);
    }

    /* Setter for name.
    * @param s the string
    */
    public final void setName (String s) {
        super.setName (s);
    }

    /* Setter for short description.
    * @param s the string
    */
    public final void setShortDescription (String s) {
        super.setShortDescription (s);
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
        if (!canRead ()) throw new IllegalAccessException ();
        Object validInstance = Beans.getInstanceOf (instance, getter.getDeclaringClass());
        return getter.invoke (validInstance, new Object [0]);
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
        if (!canWrite ()) throw new IllegalAccessException ();
        Object validInstance = Beans.getInstanceOf (instance, setter.getDeclaringClass());
        if ((val!=null)&&(setter.getParameterTypes()[0].getComponentType().isPrimitive())&&(!val.getClass().getComponentType().isPrimitive())) {
            val = Utilities.toPrimitiveArray ((Object[])val);
        }
        setter.invoke (validInstance, new Object [] {val});
    }

    /* Can read the indexed value of the property.
    * @return <CODE>true</CODE> if the read of the value is supported
    */
    public boolean canIndexedRead () {
        return indexedGetter != null;
    }

    /* Getter for the indexed value.
    * @return the value of the property
    * @exception IllegalAccessException cannot access the called method
    * @exception IllegalArgumentException wrong argument
    * @exception InvocationTargetException an exception during invocation
    */
    public Object getIndexedValue (int index) throws
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (!canIndexedRead ()) throw new IllegalAccessException ();
        Object validInstance = Beans.getInstanceOf (instance, indexedGetter.getDeclaringClass());
        return indexedGetter.invoke (validInstance, new Object [] {new Integer (index)});
    }

    /* Can write the indexed value of the property.
    * @return <CODE>true</CODE> if the read of the value is supported
    */
    public boolean canIndexedWrite () {
        return indexedSetter != null;
    }

    /* Setter for the indexed value.
    * @param val the value of the property
    * @exception IllegalAccessException cannot access the called method
    * @exception IllegalArgumentException wrong argument
    * @exception InvocationTargetException an exception during invocation
    */
    public void setIndexedValue (int index, Object val) throws
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (!canIndexedWrite ()) throw new IllegalAccessException ();
        Object validInstance = Beans.getInstanceOf (instance, indexedSetter.getDeclaringClass());
        indexedSetter.invoke (validInstance, new Object [] {new Integer (index), val});
    }
}
