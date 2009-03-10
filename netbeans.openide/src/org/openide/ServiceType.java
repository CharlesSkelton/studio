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

package org.openide;
import java.beans.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/** This class represents an abstract subclass for services
* (compilation, execution, debugging, etc.) that can be registered in
* the system.
*
* @author Jaroslav Tulach
*/
public abstract class ServiceType extends Object implements java.io.Serializable, HelpCtx.Provider {
    /** generated Serialized Version UID */
    private static final long serialVersionUID = -7573598174423654252L;

    /** Name of property for the name of the service type. */
    public static final String PROP_NAME = "name"; // NOI18N

    /** name of the service type */
    private String name;

    /** listeners support */
    private transient PropertyChangeSupport supp;

    private static final ErrorManager err = ErrorManager.getDefault().getInstance("org.openide.ServiceType"); // NOI18N

    /** Default human-presentable name of the service type.
    * In the default implementation, taken from the bean descriptor.
    * @return initial value of the human-presentable name
    * @see FeatureDescriptor#getDisplayName
    */
    protected String displayName () {
        try {
            return Introspector.getBeanInfo (getClass ()).getBeanDescriptor ().getDisplayName ();
        } catch (Exception e) {
            // Catching IntrospectionException, but also maybe NullPointerException...?
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
            return getClass ().getName ();
        }
    }
    
    
    /** Method that creates a cloned instance of this object. Subclasses
     * are encouraged to implement the {@link Cloneable}
     * interface, in such case the <code>clone</code> method is called as a result
     * of calling this method. If the subclass does not implement
     * <code>Cloneable</code>, it is serialized and deserialized,
     * thus new instance created.
     *
     * @return new instance
     * @exception IllegalStateException if something goes wrong, but should not happen
     * @deprecated Service instance files should instead be copied in order to clone them.
     */
    public final ServiceType createClone () {
        Exception anEx;
        
        if (this instanceof Cloneable) {
            try {
                return (ServiceType)clone ();
            } catch (CloneNotSupportedException ex) {
                anEx = ex;
            }
        } else {
            try {
                org.openide.util.io.NbMarshalledObject m = new org.openide.util.io.NbMarshalledObject (this);
                return (ServiceType)m.get ();
            } catch (IOException ex) {
                anEx = ex;
            } catch (ClassNotFoundException ex) {
                anEx = ex;
            }
        }
        
        // the code can get here only if an exception occured
        // moreover it should never happen that this code is executed
        IllegalStateException ex = new IllegalStateException ();
        
        ErrorManager err = ErrorManager.getDefault ();
        err.copyAnnotation(ex, anEx);
        err.annotate (ex, "Cannot createClone for " + this); // NOI18N
        
        throw ex;
        
    }
    
    /** Correctly implements the clone operation on this object. In
     * order to work really correctly, the subclass has to implement the
     * Cloneable interface.
     *
     * @return a new cloned instance that does not have any listeners
     * @deprecated Service instance files should instead be copied in order to clone them.
     */
    protected Object clone () throws CloneNotSupportedException {
        ServiceType t = (ServiceType)super.clone ();
        // clear listeners
        t.supp = null;
        // clear name
        t.name = null;
        
        return t;
    }

    /** Set the name of the service type.
    * Usually it suffices to override {@link #displayName},
    * or just to provide a {@link BeanDescriptor} for the class.
    * @param name the new human-presentable name
    */
    public void setName (String name) {
        String old = this.name;
        this.name = name;
        if (supp != null) {
            supp.firePropertyChange (PROP_NAME, old, name);
        }
    }

    /** Get the name of the service type.
    * The default value is given by {@link #displayName}.
    * @return a human-presentable name for the service type
    */
    public String getName () {
        return name == null ? displayName () : name;
    }

    /** Get context help for this service type.
    * @return context help
    */
    public abstract HelpCtx getHelpCtx ();

    /** Add a property change listener.
    * @param l the listener to add
    */
    public final synchronized void addPropertyChangeListener (PropertyChangeListener l) {
        if (supp == null) supp = new PropertyChangeSupport (this);
        supp.addPropertyChangeListener (l);
    }

    /** Remove a property change listener.
    * @param l the listener to remove
    */
    public final void removePropertyChangeListener (PropertyChangeListener l) {
        if (supp != null) supp.removePropertyChangeListener (l);
    }

    /** Fire information about change of a property in the service type.
    * @param name name of the property
    * @param o old value
    * @param n new value
    */
    protected final void firePropertyChange (String name, Object o, Object n) {
        if (supp != null) {
            supp.firePropertyChange (name, o, n);
        }
    }

    /** The registry of all services. This class is provided by the implementation
    * of the IDE and should hold all of the services registered to the system.
    * <P>
    * This class can be serialized to securely save settings of all
    * services in the system.
    * @deprecated Use lookup instead.
    */
    public static abstract class Registry implements java.io.Serializable {
        /** suid */
        final static long serialVersionUID = 8721000770371416481L;

        /** Get all available services managed by the engine.
        * @return an enumeration of {@link ServiceType}s
        */
        public abstract Enumeration services ();

        /** Get all available services that are assignable to the given superclass.
        * @param clazz the class that all services should be subclass of
        * @return an enumeration of all matching {@link ServiceType}s
        */
        public Enumeration services (final Class clazz) {
            return new org.openide.util.enums.FilterEnumeration (services ()) {
                       public boolean accept (Object o) {
                           return clazz.isInstance (o);
                       }
                   };
        }

        /** Getter for list of all service types.
        * @return a list of {@link ServiceType}s
        */
        public abstract java.util.List getServiceTypes ();

        /** Setter for list of service types. This permits changing
        * instances of the objects but only within the types that are already registered
        * in the system by manifest sections. If an instance of any other type
        * is in the list it is ignored.
        *
        * @param arr a list of {@link ServiceType}s
        * @deprecated Better to change service instance files instead.
        */
        public abstract void setServiceTypes (java.util.List arr);

        /** Find the service type implemented as a given class.
         * The whole registry is searched for a service type of that exact class (subclasses do not count).
        * <P>
        * This could be used during (de-)serialization
        * of a service type: only store its class name
        * and then try to find the type implemented by that class later.
        *
        * @param clazz the class of the service type looked for
        * @return the desired type or <code>null</code> if it does not exist
         * @deprecated Just use lookup.
        */
        public ServiceType find (Class clazz) {
            Enumeration en = services ();
            while (en.hasMoreElements ()) {
                Object o = en.nextElement ();
                if (o.getClass () == clazz) {
                    return (ServiceType)o;
                }
            }
            return null;
        }

        /** Find a service type of a supplied name in the registry.
        * <P>
        * This could be used during (de-)serialization
        * of a service type: only store its name
        * and then try to find the type later.
        *
        * @param name (display) name of service type to find
        * @return the desired type or <code>null</code> if it does not exist
        */
        public ServiceType find (String name) {
            Enumeration en = services ();
            while (en.hasMoreElements ()) {
                ServiceType o = (ServiceType)en.nextElement ();
                if (name.equals (o.getName ())) {
                    return o;
                }
            }
            return null;
        }
    }


    /** Handle for a service type. This is a serializable class that should be used
    * to store types and to recreate them after deserialization.
    */
    public static final class Handle extends Object
        implements java.io.Serializable {
        /** generated Serialized Version UID */
        static final long serialVersionUID = 7233109534462148872L;

        /** name executor */
        private String name;
        /** name of class of the executor */
        private String className;
        /** kept ServiceType may be <tt>null</tt> after deserialization */
        private transient ServiceType serviceType;

        /** Create a new handle for an service.
        * @param ex the service to store a handle for
        */
        public Handle (ServiceType ex) {
            name = ex.getName ();
            className = ex.getClass ().getName ();
            serviceType = ex;
        }

        /** Find the service for this handle.
        * @return the reconstituted service type, or <code>null</code> in case of problems
        */
        public ServiceType getServiceType () {
            if (serviceType == null) {
                // the class to search for
                Class clazz;
                // the first subclass of ServiceType to search for
                Class serviceTypeClass;
                // try to find it by class
                try {
                    clazz = Class.forName (className, true, (ClassLoader)Lookup.getDefault().lookup(ClassLoader.class));
                    
                    serviceTypeClass = clazz;
                    while (serviceTypeClass.getSuperclass () != ServiceType.class) {
                        serviceTypeClass = serviceTypeClass.getSuperclass ();
                    }
                } catch (ClassNotFoundException ex) {
                    // #32140 - do not notify user about this exception. This exception
                    // should be only thrown when module providing the service
                    // was uninstalled and in that case the exception must be ignored.
                    err.log(ErrorManager.INFORMATIONAL, "Service not found: "+ex.toString()); //NOI18N

                    // nothing better to use
                    clazz = ServiceType.class;
                    serviceTypeClass = ServiceType.class;
                }
                
                
                // try to find the executor by name
                ServiceType.Registry r = (ServiceType.Registry)Lookup.getDefault().lookup(ServiceType.Registry.class);
                Enumeration en = r.services (clazz);
                ServiceType some = r.find (clazz);
                while (en.hasMoreElements ()) {
                    ServiceType t = (ServiceType)en.nextElement ();
                    
                    if (!serviceTypeClass.isInstance (t)) {
                        // ignore non instances
                        continue;
                    }
                    
                    String n = t.getName ();
                    if (n != null && n.equals (name)) {
                        return t;
                    }
                    
                    // remember it for later use
                    if (some == null || (some.getClass () != clazz && t.getClass () == clazz)) {
                        // remember the best match
                        some = t;
                    }
                }
                // if clazz does not exist and there is no service with same name -> return null
                if (serviceTypeClass == ServiceType.class)
                    return null;
                return some;
            }
            return serviceType;
        }

        /** Old compatibility version.
        */
        private void readObject (ObjectInputStream ois) throws IOException, ClassNotFoundException {
            name = (String)ois.readObject ();
            String clazz = (String)ois.readObject ();
            className = (clazz==null)? null: org.openide.util.Utilities.translate (clazz);
        }

        /** Has also save the object.
        */
        private void writeObject (ObjectOutputStream oos) throws IOException {
            oos.writeObject (name);
            oos.writeObject (className);
        }
        
        // for debugging purposes
        public String toString () {
            return "Handle[" + className + ":" + name + "]"; // NOI18N
        }
        
    }

}
