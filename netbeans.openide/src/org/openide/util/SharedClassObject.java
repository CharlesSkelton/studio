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

package org.openide.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.beans.*;
import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import java.util.*;

import org.openide.ErrorManager;

/** Shared object that allows different instances of the same class
* to share common data.
* <p>The data are shared only between instances of the same class (not subclasses).
* Thus, such "variables" have neither instance nor static behavior.
*
* @author Ian Formanek, Jaroslav Tulach
*/
public abstract class SharedClassObject extends Object
    implements Externalizable {
    /** serialVersionUID */
    private static final long serialVersionUID = 4527891234589143259L;

    /** Name of the method used to determine whether an option is global or not. */
    static final String GLOBAL_METHOD_NAME = "isGlobal"; // NOI18N
    
    private byte [] defaultInstance = null;
    
    /** property change support (PropertyChangeSupport) */
    private static final Object PROP_SUPPORT = new Object ();

    /** Map (Class, DataEntry) that maps Classes to maps of any objects */
    private static final Map values = new WeakHashMap (4);

    /** data entry for this class */
    private final DataEntry dataEntry;

    /** Lock for the object */
    private Object lock;

    /** hard reference to primary instance of this class
    * This is here not to allow the finalization till at least
    * one object exists
    */
    private final SharedClassObject first;
    
    private static final ErrorManager err = ErrorManager.getDefault().getInstance("org.openide.util.SharedClassObject"); // NOI18N

    /** Stack trace indicating where the first instance was created.
     * This is only set on the first instance; and only with the error manager on.
     */
    private Throwable firstTrace = null;

    /** A set of all classes for which we are currently inside createInstancePrivileged.
     * If a SCO constructor is called when an instance of that class already exists, normally
     * this will print a warning. However it is common to create an instance inside a static
     * block; in this case the constructor is actually called twice. Only the first instance
     * is ever returned, but this set ensures that no warning is printed during creation of the
     * second instance (because it is nobody's fault and it will be handled OK).
     * Map from class name to nesting count.
     */
    private static final Map instancesBeingCreated = new HashMap (3); // Map<String,int>

    /** Set of classes to not warn about any more.
     * Names only.
     */
    private static final Set alreadyWarnedAboutDupes = new HashSet (); // Set<String>

    /** Set by {@link SystemOption}s through the special property, see {@link #putProperty}.
     * SystemOption needs special handling, e.g. it needs to be deserialized by the lookup 
     * after its first instance is created in {@link #findObject} method, only
     * SystemOption can be reset.
     */
    private boolean systemOption = false;
    
    /** If set, this means we have a system option waiting to be loaded from lookup.
     * If anyone changes a property on it before this happens, the exception is filled in,
     * so we know when it is loaded that something went wrong.
     */
    private boolean waitingOnSystemOption = false;
    private IllegalStateException prematureSystemOptionMutation = null;
    private boolean inReadExternal = false;
    
    /** Check that addNotify, removeNotify, initialize call super sometime. */
    private boolean addNotifySuper, removeNotifySuper, initializeSuper;
    
    /* Calls a referenceLost to decrease the counter on the shared data.
    * This method is final so no descendant can override it, but
    * it calls the method unreferenced() that can be overriden to perform any
    * additional tasks on finalizing.
    */
    protected final void finalize() throws Throwable {
        referenceLost ();
    }

    /** Indicate whether the shared data of the last existing instance of this class
    * should be cleared when that instance is finalized.
    *
    * Subclasses may perform additional tasks
    * on finalization if desired. This method should be overridden
    * in lieu of {@link #finalize}.
    * <p>The default implementation returns <code>true</code>.
    * Classes which have precious shared data may want to return <code>false</code>, so that
    * all instances may be finalized, after which new instances will pick up the same shared variables
    * without requiring a recalculation.
    *
    * @return <code>true</code> if all shared data should be cleared,
    *   <code>false</code> if it should stay in memory
    */
    protected boolean clearSharedData () {
        return true;
    }

    /** Test whether the classes of the compared objects are the same.
    * @param obj the object to compare to
    * @return <code>true</code> if the classes are equal
    */
    public final boolean equals (Object obj) {
        return ((obj instanceof SharedClassObject) && (getClass().equals(obj.getClass())));
    }

    /** Get a hashcode of the shared class.
    * @return the hash code
    */
    public final int hashCode () {
        return getClass().hashCode();
    }

    /** Obtain lock for synchronization on manipulation with this
    * class.
    * Can be used by subclasses when performing nonatomic writes, e.g.
    * @return an arbitrary synchronizable lock object
    */
    protected final Object getLock () {
        if (lock == null) {
            lock = getClass ().getName ().intern ();
        }
        return lock;
    }

    /** Create a shared object.
    * Typically shared-class constructors should not take parameters, since there
    * will conventionally be no instance variables.
    * @see #initialize
    */
    protected SharedClassObject () {
        synchronized (getLock ()) {
            DataEntry de = (DataEntry)values.get (getClass ());
            //System.err.println("SCO create: " + this + " de=" + de);
            if (de == null) {
                de = new DataEntry ();
                values.put (getClass (), de);
            }
            dataEntry = de;
            de.increase();
            // finds reference for the first object of the class
            first = de.first (this);
        }
        
        if (first != null) {
            if (first == this) {
                // Could be a performance hit, so only do this when developing.
                if (err.isLoggable(ErrorManager.INFORMATIONAL)) {
                    Throwable t = new Throwable ("First instance created here"); // NOI18N
                    t.fillInStackTrace ();
                    first.firstTrace = t;
                }
            } else {
                String clazz = getClass ().getName ();
                boolean creating;
                synchronized (instancesBeingCreated) {
                    creating = instancesBeingCreated.containsKey (clazz);
                }
                if (creating) {
                    //System.err.println ("Nesting: " + getClass ().getName () + " " + instancesBeingCreated.get (clazz));
                } else {
                    if (! alreadyWarnedAboutDupes.contains (clazz)) {
                        alreadyWarnedAboutDupes.add (clazz);
                        Exception e = new IllegalStateException
                            ("Warning: multiple instances of shared class " + clazz + " created."); // NOI18N
                        if (first.firstTrace != null) {
                            err.annotate (e, first.firstTrace);
                        } else {
                            err.annotate (e, "(Run with -J-Dorg.openide.util.SharedClassObject=0 for more details.)"); // NOI18N
                        }
                        err.notify (ErrorManager.INFORMATIONAL, e);
                    }
                }
            }
        }
    }

    /** Should be called from within a finalize method to manage references
    * to the shared data (when the last reference is lost, the object is
    * removed)
    */
    private void referenceLost() {
        //System.err.println ("SharedClassObject.referenceLost:");
        //System.err.println ("\tLock: " + getLock());
        //System.err.println ("\tDataEntry: " + dataEntry);
        //System.err.println ("\tValues: " + values.containsKey(getClass()));
        synchronized (getLock ()) {
            if (dataEntry == null || dataEntry.decrease() == 0) {
                if (clearSharedData ()) {
                    // clears the data
                    values.remove (getClass());
                }
            }
        }
        //System.err.println("\tValues after: " + values.containsKey(getClass()));
    }

    /** Set a shared variable.
    * Automatically {@link #getLock locks}.
    * @param key name of the property
    * @param value value for that property (may be null)
    * @return the previous value assigned to the property, or <code>null</code> if none
    */
    protected final Object putProperty (Object key, Object value) {
        synchronized (getLock ()) {
            if (key.equals ("netbeans.systemoption.hack")) { // NOI18N
                systemOption = true;
                return null;
            }
            if (waitingOnSystemOption && key != PROP_SUPPORT &&
                    prematureSystemOptionMutation == null && !dataEntry.isInInitialize() && !inReadExternal) {
                // See below in findObject. Note that if we are still in initialize(),
                // it is harmless to set default values of properties, and from readExternal()
                // it is expected.
                prematureSystemOptionMutation = new IllegalStateException("...setting property here..."); // NOI18N
            }
            return dataEntry.getMap (this).put (key, value);
            //return dataEntry.getMap().put (key, value);
        }
    }

    /** Set a shared variable available only for string names.
    * Automatically {@link #getLock locks}. 
     * <p><strong>Important:</strong> remember that <code>SharedClassObject</code>s
     * are like singleton beans; when you use <code>putProperty</code> with a value
     * of <code>true</code>, or call {@link #firePropertyChange}, you must consider that
     * the property name should match the JavaBeans name for a natural (introspected) property
     * for the bean, if such a property uses this key. For example, if you have a method
     * <code>getFoo</code> which uses {@link #getProperty} and a method <code>setFoo</code>
     * which uses <code>putProperty(..., true)</code>, then the key used <em>must</em>
     * be named <code>foo</code> (assuming you did not override this name in a BeanInfo).
     * Otherwise various listeners may not be prepared for the property change and may just
     * ignore it. For example, the property sheet for a {@link BeanNode} based on a
     * <code>SharedClassObject</code> which stores its properties using a misnamed key
     * will probably not refresh correctly.
    * @param key name of the property
    * @param value value for that property (may be null)
    * @param notify should all listeners be notified about property change?
    * @return the previous value assigned to the property, or <code>null</code> if none
    */
    protected final Object putProperty (String key, Object value, boolean notify) {
        Object previous = putProperty (key, value);

        if (notify) {
            firePropertyChange (key, previous, value);
        }

        return previous;
    }

    /** Get a shared variable.
    * Automatically {@link #getLock locks}.
    * @param key name of the property
    * @return value of the property, or <code>null</code> if none
    */
    protected final Object getProperty (Object key) {
        synchronized (getLock ()) {
            //System.err.println("SCO: " + this + " get: " + key + " de=" + dataEntry);
            return dataEntry.get(this, key);
        }
    }


    /** Initialize shared state.
    * Should use {@link #putProperty} to set up variables.
    * Subclasses should always call the super method.
    * <p>This method need <em>not</em> be called explicitly; it will be called once
    * the first time a given shared class is used (not for each instance!).
    */
    protected void initialize () {
        initializeSuper = true;
    }

    /** Adds the specified property change listener to receive property
     * change events from this object.
     * @param         l the property change listener
     */
    public final void addPropertyChangeListener(PropertyChangeListener l) {
        boolean noListener;

        synchronized (getLock ()) {
            //      System.out.println ("added listener: " + l + " to: " + getClass ()); // NOI18N
            PropertyChangeSupport supp = (PropertyChangeSupport)getProperty (PROP_SUPPORT);
            if (supp == null) {
                //        System.out.println ("Creating support"); // NOI18N
                putProperty (PROP_SUPPORT, supp = new PropertyChangeSupport (this));
            }
            noListener = !supp.hasListeners (null);
            supp.addPropertyChangeListener(l);
        }
        if (noListener) {
            addNotifySuper = false;
            addNotify ();
            if (!addNotifySuper) {
                // [PENDING] theoretical race condition for this warning if listeners are added
                // and removed very quickly from two threads, I guess, and addNotify() impl is slow
                String msg = "You must call super.addNotify() from " + getClass().getName() + ".addNotify()"; // NOI18N
                err.log(ErrorManager.WARNING, msg);
            }
        }
    }

    /**
     * Removes the specified property change listener so that it
     * no longer receives property change events from this object.
     * @param         l     the property change listener
     */
    public final void removePropertyChangeListener(PropertyChangeListener l) {
        boolean callRemoved;

        synchronized (getLock ()) {
            PropertyChangeSupport supp = (PropertyChangeSupport)getProperty (PROP_SUPPORT);
            if (supp == null) return;

            boolean hasListener = supp.hasListeners (null);
            supp.removePropertyChangeListener(l);
            callRemoved = hasListener && !supp.hasListeners (null);
        }
        if (callRemoved) {
            putProperty (PROP_SUPPORT, null); // clean the PCS, see #25417
            removeNotifySuper = false;
            removeNotify ();
            if (!removeNotifySuper) {
                String msg = "You must call super.removeNotify() from " + getClass().getName() + ".removeNotify()"; // NOI18N
                err.log(ErrorManager.WARNING, msg);
            }
        }
    }

    /** Notify subclasses that the first listener has been added to this object.
    * Subclasses should always call the super method.
    * The default implementation does nothing.
    */
    protected void addNotify () {
        addNotifySuper = true;
    }

    /** Notify subclasses that the last listener has been removed from this object.
    * Subclasses should always call the super method.
    * The default implementation does nothing.
    */
    protected void removeNotify () {
        removeNotifySuper = true;
    }

    /** Fire a property change event to all listeners.
    * @param name the name of the property
    * @param oldValue the old value
    * @param newValue the new value
    */
    // not final - SystemOption overrides it, e.g.
    protected void firePropertyChange (String name, Object oldValue, Object newValue) {
        PropertyChangeSupport supp = (PropertyChangeSupport)getProperty (PROP_SUPPORT);
        if (supp != null)
            supp.firePropertyChange (name, oldValue, newValue);
    }

    /** Writes nothing to the stream.
    * @param oo ignored
    */
    public void writeExternal (ObjectOutput oo) throws IOException {
    }

    /** Reads nothing from the stream.
    * @param oi ignored
    */
    public void readExternal (ObjectInput oi)
    throws IOException, ClassNotFoundException {
    }

    /** This method provides correct handling of serialization and deserialization.
    * When serialized the method writeExternal is used to store the state.
    * When deserialized first an instance is located by a call to findObject (clazz, true)
    * and then a method readExternal is called to read its state from stream.
    * <P>
    * This allows to have only one instance of the class in the system and work
    * only with it.
    *
    * @return write replace object that handles the described serialization/deserialization process
    */
    protected Object writeReplace () {
        return new WriteReplace (this);
    }


    /** Obtain an instance of the desired class, if there is one.
    * @param clazz the shared class to look for
    * @return the instance, or <code>null</code> if such does not exists
    */
    public static SharedClassObject findObject (Class clazz) {
        return findObject (clazz, false);
    }

    /** Find an existing object, possibly creating a new one as needed.
    * To create a new instance the class must be public and have a public
    * default constructor.
    *
    * @param clazz the class of the object to find (must extend <code>SharedClassObject</code>)
    * @param create <code>true</code> if the object should be created if it does not yet exist
    * @return an instance, or <code>null</code> if there was none and <code>create</code> was <code>false</code>
    * @exception IllegalArgumentException if a new instance could not be created for some reason
    */
    public static SharedClassObject findObject (Class clazz, boolean create) {
        
        // synchronizing on the same object as returned from getLock()
        synchronized (clazz.getName().intern()) {
            
            DataEntry de = (DataEntry)values.get (clazz);
            // either null or the object
            SharedClassObject obj = de == null ? null : de.get ();
            boolean created = false;

            if (obj == null && create) {

                // try to create new instance
                PrivilegedExceptionAction action = new SetAccessibleAction(clazz);
                try {
                    obj = (SharedClassObject) AccessController.doPrivileged(action);
                } catch (PrivilegedActionException e) {
                    Exception ex = e.getException();
                    IllegalArgumentException newEx = new IllegalArgumentException (ex.toString());
                    err.annotate(newEx, ex);
                    throw newEx;
                }
                created = true;
            }
            de = (DataEntry) values.get (clazz);
            if (de != null) {
                SharedClassObject obj2 = de.get ();
                if (obj != null && obj != obj2) {
                    // Tricked! The static initializer for the class called findObject on itself.
                    // So we created two instances of it.
                    // Returning only the first (that created by the static initializer, rather
                    // than by us explicitly), to avoid duplication.
                    //System.err.println ("Nesting #2: " + clazz.getName ());
                    if (obj2 == null && create) throw new IllegalStateException("Inconsistent state: " + clazz); // NOI18N
                    return obj2;
                }
            }
            if (created) {
                obj.reset ();

                // This hack was created due to the remove of SystemOptions deserialization
                // from project open operation, all SystemOptions are deserialized at this place
                // the first time anybody asks for the option.
                // It's crutial to do this just for SystemOptions and not for any other SharedClassObject,
                // otherwise it can cause deadlocks.
                // Lookup in the active session is used to find serialized state of the option,
                // if such state exists it is deserialized before the object is returned from lookup.
                if (obj != null && obj.systemOption) {
                    // Lookup will find serialized version of searched object and deserialize it
                    final Lookup.Result r = Lookup.getDefault().lookup(new Lookup.Template(clazz));
                    if (r.allInstances().isEmpty()) {
                        // #17711: folder lookup not yet initialized. Try to load the option later.
                        // In the meantime the default state of the option will be available.
                        // If any attempt is made to change the option, _and_ it is later loaded,
                        // then we print a stack trace of the mutation for debugging (since the mutations
                        // would get clobbered by loading the settings from layer or whatever).
                        obj.waitingOnSystemOption = true;
                        final SharedClassObject _obj = obj;
                        final IllegalStateException start = new IllegalStateException("Making a SystemOption here that is not in lookup..."); // NOI18N
                        class SOLoader implements LookupListener {
                            public void resultChanged(LookupEvent ev) {
                                if (!r.allInstances().isEmpty()) {
                                    // Got it.
                                    r.removeLookupListener(SOLoader.this);
                                    synchronized (_obj.getLock()) {
                                        _obj.waitingOnSystemOption = false;
                                        if (_obj.prematureSystemOptionMutation != null) {
                                            warn(start);
                                            warn(_obj.prematureSystemOptionMutation);
                                            warn(new IllegalStateException("...and maybe getting clobbered here, see #17711.")); // NOI18N
                                            _obj.prematureSystemOptionMutation = null;
                                        }
                                    }
                                }
                            }
                        }
                        r.addLookupListener(new SOLoader());
                    }
                }
            }
            if (obj == null && create) throw new IllegalStateException("Inconsistent state: " + clazz); // NOI18N
            return obj;
        }
    }
    // See above:
    private static void warn(Throwable t) {
        err.notify(ErrorManager.INFORMATIONAL, t);
    }
    
    static Object createInstancePrivileged(Class clazz) throws Exception {
        java.lang.reflect.Constructor c = clazz.getDeclaredConstructor(new Class[0]);
        c.setAccessible(true);
        String name = clazz.getName ();
        synchronized (instancesBeingCreated) {
            Integer i = (Integer) instancesBeingCreated.get (name);
            instancesBeingCreated.put (name, i == null ? new Integer (1) : new Integer (i.intValue () + 1));
        }
        try {
            return c.newInstance (new Object[0]);
        } finally {
            synchronized (instancesBeingCreated) {
                Integer i = (Integer) instancesBeingCreated.get (name);
                if (i.intValue () == 1) {
                    instancesBeingCreated.remove (name);
                } else {
                    instancesBeingCreated.put (name, new Integer (i.intValue () - 1));
                }
            }
            c.setAccessible(false);
        }
    }

    /** Resets shared data to it default value. */
    private void reset () {
        if (!systemOption || !isProjectOption ()) {
            return;
        }

        synchronized (getLock ()) {
            // [PENDING] should be changed to next line after all options in layers will
            // use put{get}Property and initilaize properly
            // dataEntry.reset (this);
            
            if (defaultInstance == null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream (1024);
                    ObjectOutput oo = new org.openide.util.io.NbObjectOutputStream (baos);
                    oo.writeObject (this);
                    defaultInstance = baos.toByteArray ();
                } catch (IOException e) {
                    defaultInstance = null;
                }
                return;
            }

            try {
                ByteArrayInputStream bais = new ByteArrayInputStream (defaultInstance);
                ObjectInputStream oi = new org.openide.util.io.NbObjectInputStream (bais);
                oi.readObject ();
            } catch (Exception e) {
                // ignore and leave it as it is
            }
        }
    }
    
    /**
     * Test if the object is Project specific.
     * @return true if the object is Project specific
     */
    private boolean isProjectOption () {
        try {
            Class clazz = getClass ();
            // the old hack with undocumented method isGlobal
            Method m = clazz.getMethod(GLOBAL_METHOD_NAME, new Class[] {});
            m.setAccessible(true);
            Boolean b = (Boolean) m.invoke(this, new Object[] {});
            return !b.booleanValue();
        } catch (Exception ex) {
            // ignore and return default
        }
        return false;
    }
    
    
    /** Class that is used as default write replace.
    */
    static final class WriteReplace extends Object implements Serializable {
        /** serialVersionUID */
        static final long serialVersionUID = 1327893248974327640L;

        /** the class  */
        private Class clazz;
        /** class name, in case clazz could not be reloaded */
        private String name;
        /** shared instance */
        private transient SharedClassObject object;

        /** Constructor.
        * @param the instance
        */
        public WriteReplace (SharedClassObject object) {
            this.object = object;
            this.clazz = object.getClass ();
            this.name = clazz.getName();
        }

        /** Write object.
        */
        private void writeObject (ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject ();

            object.writeExternal (oos);
        }

        /** Read object.
        */
        private void readObject (ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
            ois.defaultReadObject ();
            if (clazz == null) {
                // Means that the class is no longer available in the restoring classloader.
                // Normal enough if the module has been uninstalled etc. #15654
                if (name != null) {
                    throw new ClassNotFoundException(name);
                } else {
                    // Compatibility with older WR's.
                    throw new ClassNotFoundException();
                }
            }
            object = findObject (clazz, true);
            object.inReadExternal = true;
            try {
                object.readExternal (ois);
            } finally {
                object.inReadExternal = false;
            }
        }

        /** Read resolve to the read object.
         * We give chance to actual instance to do its own resolution as well. It
         * is necessary for achieving back compatability of certain types of settings etc.
         */
        private Object readResolve () throws ObjectStreamException {
            
            SharedClassObject resolved = object;
            
            Method resolveMethod = findReadResolveMethod(object.getClass());
            if (resolveMethod != null) {
                // invoke resolve method and accept its result
                try {
                    // make readResolve accessible (it can have any access modifier)
                    resolveMethod.setAccessible(true);                    
                    return resolveMethod.invoke(object, null);
                    
                } catch (Exception ex) {
                    
                    // checked or runtime does not matter - we must survive
                    
                    String banner = "Skipping " + object.getClass() + " resolution:";  //NOI18N
                    err.annotate(ex, ErrorManager.UNKNOWN, banner, null, null, null);
                    err.notify (ErrorManager.INFORMATIONAL, ex);
                } finally {
                    resolveMethod.setAccessible(false);
                }
            }
                                        
            return resolved;
        }
        
        /** Tries to find readResolve method in given class. Finds
        * both public and non-public occurences of the method and
        * searches also in superclasses */
        private static Method findReadResolveMethod (Class clazz) {
            Method result = null;
            
            //  try ANY-MODIFIER occurences; search also in superclasses
            for (Class i = clazz; i != null; i = i.getSuperclass()) {
                try {
                    result = accept(i.getDeclaredMethod("readResolve", new Class[0])); // NOI18N
                    // get out of cycle if method found
                    if (result != null) break;
                } catch (NoSuchMethodException exc) {
                    // readResolve does not exist in current class
                }
            }
            return result;
        }
        
        /*
         * @return passed method if method matches exactly readResolve declaration as defined in
         *         Serializetion specification otherwise null
         */
        private static Method accept(Method candidate) {
            if (candidate != null) {
                // check exceptions clause
                Class[] result = candidate.getExceptionTypes();
                if ((result.length == 1) &&
                        ObjectStreamException.class.equals(result[0])) {
                    // returned value type
                    if (Object.class.equals(candidate.getReturnType())) {
                        return candidate;
                    }
                }
            }
            return null;
        }

    }

    /** The inner class that encapsulates the shared data together with
    * a reference counter
    */
    static final class DataEntry extends Object {
        /** The data */
        private HashMap map;
        /** The reference counter */
        private int count = 0;
        /** weak reference to an object of this class */
        private WeakReference ref = new WeakReference (null);
        /** inited? */
        private boolean initialized = false;
        private boolean initializeInProgress = false;
        /** #7479: if initialize() threw unchecked exception, keep it here */
        private Throwable invalid = null;
        
        public String toString() { // for debugging
            return "SCO.DataEntry[ref=" + ref.get() + ",count=" + count + ",initialized=" + initialized + ",invalid=" + invalid + ",map=" + map + "]"; // NOI18N
        }
        
        /** initialize() is in progress? */
        boolean isInInitialize() {
            return initializeInProgress;
        }

        /** Returns the data
        * @param obj the requestor object
        * @return the data
        */
        Map getMap (SharedClassObject obj) {
            ensureValid (obj);

            if (map == null) {
                // to signal invalid state
                map = new HashMap ();
            }
            
            if (! initialized) {
                initialized = true;
                // no data for this class yet
                tryToInitialize (obj);
            }
            
            return map;
        }
        
        /** Returns a value for given key
        * @param obj the requestor object
        * @return the data
        */
        Object get(SharedClassObject obj, Object key) {
            ensureValid (obj);

            Object ret;
            
            if (map == null) {
                // to signal invalid state
                map = new HashMap ();
                ret = null;
            } else {
                ret = map.get(key);
            }
            
            if ((ret == null) && !initialized) {
                if (key == PROP_SUPPORT) {
                    return null;
                }
                initialized = true;
                // no data for this class yet
                tryToInitialize (obj);
                ret = map.get(key);
            }
            
            return ret;
        }


        /** Returns the data
        * @return the data
        */
        Map getMap() {
            ensureValid (get ());

            if (map == null) {
                // to signal invalid state
                map = new HashMap ();
            }
            return map;
        }

        private void ensureValid (SharedClassObject obj) throws IllegalStateException {
            if (invalid != null) {
                String msg;
                if (obj != null) {
                    msg = obj.toString ();
                } else {
                    msg = "<unknown object>"; // NOI18N
                }
                IllegalStateException ise = new IllegalStateException (msg);
                err.annotate (ise, invalid);
		throw ise;
            } // else fine
        }

        private void tryToInitialize (SharedClassObject obj) throws IllegalStateException {
            initializeInProgress = true;
            obj.initializeSuper = false;
            try {
                obj.initialize ();
            } catch (Exception e) {
                invalid = e;
                IllegalStateException ise = new IllegalStateException(invalid.toString() + " from " + obj); // NOI18N
                err.annotate (ise, invalid);
		throw ise;
            } catch (LinkageError e) {
                invalid = e;
                IllegalStateException ise = new IllegalStateException(invalid.toString() + " from " + obj); // NOI18N
                err.annotate (ise, invalid);
		throw ise;
            } finally {
                initializeInProgress = false;
            }
            if (!obj.initializeSuper) {
                String msg = "You must call super.initialize() from " + obj.getClass().getName() + ".initialize()"; // NOI18N
                err.log(ErrorManager.WARNING, msg);
            }
        }
        
        /** Increases the counter (thread safe)
        * @return new counter value
        */
        int increase () {
            return ++count;
        }

        /** Dereases the counter (thread safe)
        * @return new counter value
        */
        int decrease () {
            return --count;
        }

        /** Request for first object. If there is none, use the requestor
        * @param obj requestor
        * @return the an object of this type
        */
        SharedClassObject first (SharedClassObject obj) {
            SharedClassObject s = (SharedClassObject)ref.get ();
            if (s == null) {
                ref = new WeakReference (obj);
                return obj;
            } else {
                return s;
            }
        }

        /** @return shared object or null
        */
        public SharedClassObject get () {
            return (SharedClassObject)ref.get ();
        }

        /** Reset map of values. */
        public void reset (SharedClassObject obj) {
            SharedClassObject s = get ();
            if (s != null && s != obj)
                return;
            
            invalid = null;
            getMap ().clear ();
            
            initialized = true;
            tryToInitialize (obj);
        }
    }
    
    static final class SetAccessibleAction implements PrivilegedExceptionAction {
        Class klass;
        
        SetAccessibleAction(Class klass) {
            this.klass = klass;
        }
        
        public Object run() throws Exception {
            return createInstancePrivileged(klass);
        }
    }
}
