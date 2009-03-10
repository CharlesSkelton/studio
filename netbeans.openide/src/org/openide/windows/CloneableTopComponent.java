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

package org.openide.windows;

import java.io.IOException;
import java.util.*;

import org.openide.loaders.DataObject;
import org.openide.util.io.NbMarshalledObject;
import org.openide.util.NbBundle;

/** A top component which may be cloned.
* Typically cloning is harmless, i.e. the data contents (if any)
* of the component are the same, and the new component is merely
* a different presentation.
* Also, a list of all cloned components is kept.
*
* @author Jaroslav Tulach
*/
public abstract class CloneableTopComponent extends TopComponent
    implements java.io.Externalizable, TopComponent.Cloneable {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 4893753008783256289L;

    /** reference with list of components */
    private Ref ref;

    /** Create a cloneable top component.
    */
    public CloneableTopComponent () {
    }

    /** Create a cloneable top component associated with a data object.
    * @param obj the data object
    * @see TopComponent#TopComponent(DataObject)
    */
    public CloneableTopComponent (DataObject obj) {
        super (obj);
    }

    /** Clone the top component and register the clone.
    * @return the new component
    */
    public final Object clone () {
        return cloneComponent ();
    }

    /** Clone the top component and register the clone.
    * Simply calls createClonedObject () and registers the component to
    * Ref.
    *
    * @return the new cloneable top component
    */
    public final CloneableTopComponent cloneTopComponent() {
        CloneableTopComponent top = createClonedObject ();
        // register the component if it has not been registered before
        top.setReference (getReference ());
        return top;
    }

    /** Clone the top component and register the clone.
    * @return the new component
    */
    public final TopComponent cloneComponent() {
        return cloneTopComponent ();
    }

    /** Called from {@link #clone} to actually create a new component from this one.
    * The default implementation only clones the object by calling {@link Object#clone}.
    * Subclasses may leave this as is, assuming they have no special needs for the cloned
    * data besides copying it from one object to the other. If they do, the superclass
    * method should be called, and the returned object modified appropriately.
    * @return a copy of this object
    */
    protected CloneableTopComponent createClonedObject () {
        try {
            // clones the component using serialization
            NbMarshalledObject o = new NbMarshalledObject (this);
            CloneableTopComponent top = (CloneableTopComponent)o.get ();
            return top;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new InternalError ();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new InternalError ();
        }
    }

    /** Get a list of all components which are clone-sisters of this one.
    *
    * @return the clone registry for this component's group
    */
    public synchronized final Ref getReference () {
        if (ref == null) {
            ref = new Ref (this);
        }
        return ref;
    }

    /** Changes the reference to which this components belongs.
    * @param another the new reference this component should belong
    */
    public synchronized final void setReference (Ref another) {
        if (another == EMPTY) {
            throw new IllegalArgumentException(
                NbBundle.getBundle(CloneableTopComponent.class).getString("EXC_CannotAssign")
            );
        }

        if (ref != null) {
            // Remove from old ref, we are going to belong to 'another' reference.
            ref.removeComponent(this);
        }
        // Register with the new reference.
        another.register (this);
        // Finally set the field.
        ref = another;
    }

    /** Called when this component is about to close.
    * The default implementation just unregisters the clone from its clone list.
    * <p>If this is the last component in its clone group, then
    * {@link #closeLast} is called to clean up.
    *
    * @return <CODE>true</CODE> if there are still clone sisters left, or this was the last in its group
    *    but {@link #closeLast} returned <code>true</code>
    */
    public boolean canClose (Workspace workspace, boolean last) {
        if (last) {
            return getReference ().unregister (this);
        }
        return true;
    }

    /** Called when the last component in a clone group is closing.
    * The default implementation just returns <code>true</code>.
    * Subclasses may specify some hooks to run.
    * @return <CODE>true</CODE> if the component is ready to be
    *    closed, <CODE>false</CODE> to cancel
    */
    protected boolean closeLast () {
        return true;
    }

    public void readExternal (java.io.ObjectInput oi)
    throws java.io.IOException, java.lang.ClassNotFoundException {
        super.readExternal (oi);
        if (serialVersion != 0) {
            // since serialVersion > 0
            // the reference object is also stored

            Ref ref = (Ref)oi.readObject ();
            if (ref != null) {
                setReference (ref);
            }
        }
    }

    public void writeExternal (java.io.ObjectOutput oo)
    throws java.io.IOException {
        super.writeExternal (oo);

        oo.writeObject (ref);
    }

    // say what? --jglick
    /* Empty set that should save work with testing like
    * <pre>
    * if (ref == null || ref.isEmpty ()) {
    *   CloneableTopComponent c = new CloneableTopComponent (obj);
    *   ref = c.getReference ();
    * }
    * </pre>
    * Instead one can always set <CODE>ref = Ref.EMPTY</CODE> and test only if
    * <CODE>ref.isEmpty</CODE> returns <CODE>true</CODE>.
    */
    /** Empty clone-sister list.
    */
    public static final Ref EMPTY = new Ref ();

    /** Keeps track of a group of sister clones.
    * <P>
    * <B>Warning:</B>
    * For proper use
    * subclasses should have method readResolve () and implement it
    * in right way to deal with separate serialization of TopComponent.
    */
    public static class Ref implements java.io.Serializable {
        /** generated Serialized Version UID */
        static final long serialVersionUID = 5543148876020730556L;
        /** manipulation lock */
        private static final Object LOCK = new Object ();

        /** Set of registered components. */
        private transient /*final*/ Set componentSet = new HashSet(7);

        /** Default constructor for creating empty reference.
        */
        protected Ref () {
        }

        /** Constructor.
        * @param c the component to refer to
        */
        private Ref (CloneableTopComponent c) {
            synchronized(LOCK) {
                componentSet.add (c);
            }
        }


        /** Enumeration of all registered components.
        * @return enumeration of CloneableTopComponent
        */
        public Enumeration getComponents () {
            Set components;
            synchronized (LOCK) {
                 components = new HashSet(componentSet);
            }

            return java.util.Collections.enumeration(components);
        }

        /** Test whether there is any component in this set.
        * @return <CODE>true</CODE> if the reference set is empty
        */
        public boolean isEmpty () {
            synchronized (LOCK) {
                return componentSet.isEmpty();
            }
        }

        /** Retrieve an arbitrary component from the set.
        * @return some component from the list of registered ones
        * @exception NoSuchElementException if the set is empty
         * @deprecated Use {@link #getArbitraryComponent} instead.
         *             It doesn't throw a runtime exception.
        */
        public CloneableTopComponent getAnyComponent () {
            synchronized (LOCK) {
                return (CloneableTopComponent)componentSet.iterator().next();
            }
        }
        
        /** Gets arbitrary component from the set.
         * @return arbitratry <code>CloneableTopComponent</code> from the set
         *         or <code>null</code> if the set is empty
         * @since 3.41 */
        public CloneableTopComponent getArbitraryComponent() {
            synchronized(LOCK) {
                Iterator it = componentSet.iterator();
                if(it.hasNext()) {
                    return (CloneableTopComponent)it.next();
                } else {
                    return null;
                }
            }
        }

        /** Register new component.
        * @param c the component to register
        */
        private final void register (CloneableTopComponent c) {
            synchronized (LOCK) {
                componentSet.add (c);
            }
        }

        /** Unregister the component. If this is the last asks if it is
        * allowed to unregister it.
        *
        * @param c the component to unregister
        * @return true if the component agreed to be unregister
        */
        private final boolean unregister (CloneableTopComponent c) {
            int componentCount;
            synchronized(LOCK) {
                componentCount = componentSet.size();
            }
            
            if (componentCount > 1 || c.closeLast()) {
                removeComponent(c);
                return true;
            } else {
                return false;
            }
        }
        
        private void removeComponent(CloneableTopComponent c) {
            synchronized (LOCK) {
                componentSet.remove(c);
            }
        }

        /** Adds also initializing of <code>componentSet</code> field. */
        private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            synchronized(LOCK) {
                componentSet = new HashSet(7);
            }
        }
    } // end of Ref
}
