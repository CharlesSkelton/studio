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

package org.openide.explorer.propertysheet;

import java.beans.*;
import java.lang.reflect.*;

import org.openide.util.WeakListener;
import org.openide.ErrorManager;

/** The default implementation of PropertyModel interface.
* It takes the bean instance and the property name which should
* be accessed through PropertyModel methods. We now implement
* the new ExPropertyModel interface (which extends PropertyModel).
*
* @author Jaroslav Tulach, Petr Hamernik
*/
public class DefaultPropertyModel extends Object implements ExPropertyModel, PropertyChangeListener {
    /** The Java Bean */
    private Object bean;

    /** Name of the property of the bean. */
    private String propertyName;

    /** support for the properties changes. */
    private PropertyChangeSupport support;

    /** Property descriptor for the bean. */
    private PropertyDescriptor prop;

    /** Read method if exists one. */
    private Method readMethod;

    /** Write method if exists one. */
    private Method writeMethod;

    /** Type of the property. */
    private Class propertyTypeClass;

    /** When we call setValue we want to disable refiring of property change*/
    private boolean donotfire = false;

    /** Creates new DefaultPropertyModel.
    * @param bean the java bean to be introspected
    * @param propertyName name of the property
    * 
    * @exception IllegalArgumentException if there is any problem
    *      with the parameters (introspection of bean,...)
    */
    public DefaultPropertyModel(Object bean, String propertyName) throws IllegalArgumentException {
        this (bean, findInfo (bean, propertyName));
    }

    /** Creates new DefaultPropertyModel with provided specific 
     * <code>PropertyDescriptor</code>. This can be useful if one needs to
     * set to provide specific attributes to the property editor.
     * <PRE>
     * PropertyDescriptor pd = new PropertyDescriptor ("myProperty", bean.getClass ());
     * pd.setPropertyEditorClass (PropertyEditorManager.findEditor (Object.class));
     *
     * // special attributes to the property editor
     * pb.setValue ("superClass", MyProperty.class);
     *
     *
     * model = new DefaultPropertyModel (bean, pd);
     * panel = new PropertyPanel (model);
     * </PRE>
     * This constructor replaces the default use of BeanInfo and that is why
     * simplifies the use of <link>ExPropertyEditor</link>s.
     *
     * @param bean the java bean to be introspected
     * @param descr the property descriptor of the property to use
     *
     * @since 2.4
     */
    public DefaultPropertyModel(Object bean, PropertyDescriptor descr) {
        this.bean = bean;
        this.propertyName = descr.getName ();
        support = new PropertyChangeSupport(this);
        
        this.prop = descr;
        this.propertyTypeClass = descr.getPropertyType();
        this.readMethod = descr.getReadMethod ();
        this.writeMethod = descr.getWriteMethod ();
        
        try {
            try {
                // bugfix #16703 addPropertyChangeListener(String, PropertyChangeListener) is looked first
                // is not then addPropertyChangeListener(PropertyChangeListener) is looked
                Method addList = bean.getClass().getMethod("addPropertyChangeListener", new Class[] { String.class, PropertyChangeListener.class }); // NOI18N

                addList.invoke(bean, new Object[] {
                                   propertyName, 
                                   WeakListener.propertyChange(this, bean)
                               });
            } catch (NoSuchMethodException nsme) {
                try {
                    Method addList = bean.getClass().getMethod("addPropertyChangeListener", new Class[] {PropertyChangeListener.class }); // NOI18N

                    addList.invoke(bean, new Object[] {
                                        WeakListener.propertyChange(this, bean)
                                   });
                } catch (NoSuchMethodException nosme) {
                    // be quiet
                }
            }
        } catch (Exception e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
        }
    }

    /** Finds property descriptor for a bean.
     * @param bean the bean
     * @param name name of the property to find
     * @return the descriptor
     * @exception IllegalArgumentException if the method is not found
     */
    private static PropertyDescriptor findInfo (Object bean, String name) 
    throws IllegalArgumentException {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] descr = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < descr.length; i++) {
                if (descr[i].getName().equals(name)) {
                    return descr[i];
                }
            }
            throw new IllegalArgumentException (
                "No property named " + name + " in class " + bean.getClass () // NOI18N
            ); 
        } catch (IntrospectionException e) {
            IllegalArgumentException newEx = new IllegalArgumentException();
            ErrorManager.getDefault().annotate (newEx, e);
            throw newEx;
        }
    }
    
    /**
     * @return the class of the property.
     */
    public Class getPropertyType() {
        return propertyTypeClass;
    }

    /** Getter for current value of a property.
     */
    public Object getValue() throws InvocationTargetException {
        try {
            return (readMethod == null) ?
                   null :
                   readMethod.invoke(bean, new Object[] {});
        }
        catch (IllegalAccessException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            throw new InvocationTargetException(e);
        }
    }

    /** Setter for a value of a property.
     * @param v the value
     * @exception InvocationTargetException
     */
    public void setValue(Object v) throws InvocationTargetException {
        try {
            if (writeMethod != null) {
                donotfire = true;
                writeMethod.invoke(bean, new Object[] {v});
                donotfire = false;
            }
        }
        catch (IllegalAccessException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            throw new InvocationTargetException(e);
        }
    }

    /** Adds listener to change of the value.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    /** Removes listener to change of the value.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    /** Implementation of PropertyChangeListener method */
    public void propertyChange(PropertyChangeEvent evt) {
        if (propertyName.equals(evt.getPropertyName()) && (!donotfire)) {
            support.firePropertyChange(PROP_VALUE, evt.getOldValue(), evt.getNewValue());
        }
    }

    /** The class of the property editor or <CODE>null</CODE>
     * if default property editor should be used.
     */
    public Class getPropertyEditorClass() {
        return prop.getPropertyEditorClass();
    }
    
    /**
     * Returns an array of beans/nodes that this property belongs
     * to. Implements the method from ExPropertyModel interface.
     */
    public Object[] getBeans() {
        return new Object[] { bean };
    }
    
    /**
     * Returns descriptor describing the property.
     * Implements the method from ExPropertyModel interface.
     */
    public FeatureDescriptor getFeatureDescriptor() {
        return prop;
    }
}
