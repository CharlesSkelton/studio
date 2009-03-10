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

package org.openide.loaders;

import java.beans.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.openide.filesystems.*;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.CloneableOpenSupport;
import org.openide.util.WeakListener;
import org.openide.util.WeakSet;
import org.openide.util.NbBundle;

/** Simple support for an openable file.
* Can be used either as an {@link org.openide.cookies.OpenCookie},
* {@link org.openide.cookies.ViewCookie}, or {@link org.openide.cookies.CloseCookie},
* depending on which cookies the subclass implements.
*
* @author Jaroslav Tulach
*/
public abstract class OpenSupport extends CloneableOpenSupport {
    /** Entry to work with. */
    protected MultiDataObject.Entry entry;

    /** New support for a given entry. The file is taken from the
    * entry and is updated if the entry moves or renames itself.
    * @param entry entry to create instance from
    */
    public OpenSupport (MultiDataObject.Entry entry) {
        this (entry, new Env (entry.getDataObject ()));
    }
    
    /** Constructor that allows subclasses to provide their own environment.
    * Used probably only by EditorSupport.
    *
    * @param entry the entry to work on
    * @param env the environment to work on
    */
    protected OpenSupport (MultiDataObject.Entry entry, Env env) {
        super (env);
        this.entry = entry;
    }
    

    /** Message to display when an object is being opened.
    * @return the message or null if nothing should be displayed
    */
    protected String messageOpening () {
        DataObject obj = entry.getDataObject ();

        return NbBundle.getMessage (OpenSupport.class , "CTL_ObjectOpen", // NOI18N
            obj.getName(),
            obj.getPrimaryFile().toString()
        );
    }
    

    /** Message to display when an object has been opened.
    * @return the message or null if nothing should be displayed
    */
    protected String messageOpened () {
        return NbBundle.getMessage (OpenSupport.class, "CTL_ObjectOpened");
    }


    /** Method to access all editors from subclasses. Needed for compilation by 1.2
    */
    final CloneableTopComponent.Ref allEditors () {
        return allEditors;
    }

    /** Environment that connects the support together with DataObject.
    */
    public static class Env extends Object 
    implements CloneableOpenSupport.Env, java.io.Serializable,
    PropertyChangeListener, VetoableChangeListener {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -1934890789745432531L;
        /** object to serialize and be connected to*/
        private DataObject obj;
        
        /** support for firing of property changes
        */
        private transient PropertyChangeSupport propSupp;
        /** support for firing of vetoable changes
        */
        private transient VetoableChangeSupport vetoSupp;

        // #27587
        /** Map of FileSystem to its listener (weak reference of it). 
         * One listener per one filesystem for all env's from that fs. */
        private static final Map fsListenerMap = new WeakHashMap(30);
        
        // A private lock  
        private static final Object LOCK_SUPPORT = new Object();
        
        /** Constructor. Attaches itself as listener to 
        * the data object so, all property changes of the data object
        * are also rethrown to own listeners.
        *
        * @param obj data object to be attached to
        */
        public Env (DataObject obj) {
            this.obj = obj;
            init();
        }
        
        private void readObject (ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            init();
        }
        
        private void init() {
            obj.addPropertyChangeListener(WeakListener.propertyChange(this, obj));

            // XXX #25400. Ugly patch for being able to react
            // on change of root directory of filesystem, see more in the issue.
            final FileSystem fs;
            try {
                fs = obj.getPrimaryFile().getFileSystem();
            } catch(FileStateInvalidException fsie) {
                IllegalStateException ise = new IllegalStateException(
                    "FileSystem is invalid for " + obj.getPrimaryFile() + "!" // NOI18N
                );
                org.openide.ErrorManager.getDefault().annotate(ise, fsie);
                throw ise;
            }

            FileSystemNameListener fsListener;
            boolean initListening = false;
            synchronized(fsListenerMap) {
                Reference fsListenerRef = (Reference)fsListenerMap.get(fs);
                fsListener = fsListenerRef == null 
                                ? null
                                : (FileSystemNameListener)fsListenerRef.get();
                        
                if(fsListener == null) {
                    // Create listener for that filesystem.
                    fsListener = new FileSystemNameListener();
                    fsListenerMap.put(fs, new WeakReference(fsListener));
                    initListening = true;
                }
            }

            if(initListening) {
                fs.addPropertyChangeListener(fsListener);
                fs.addVetoableChangeListener(fsListener);
            }

            fsListener.add(this);
            // End of patch #25400.
        }
        
        /** Getter for data object.
        */
        protected final DataObject getDataObject () {
            return obj;
        }

        /** Adds property listener.
         */
        public void addPropertyChangeListener(PropertyChangeListener l) {
            prop ().addPropertyChangeListener (l);
        }

        /** Removes property listener.
         */
        public void removePropertyChangeListener(PropertyChangeListener l) {
            prop ().removePropertyChangeListener (l);
        }

        /** Adds veto listener.
         */
        public void addVetoableChangeListener(VetoableChangeListener l) {
            veto ().addVetoableChangeListener (l);
        }

        /** Removes veto listener.
         */
        public void removeVetoableChangeListener(VetoableChangeListener l) {
            veto ().removeVetoableChangeListener (l);
        }

        /** Test whether the support is in valid state or not.
        * It could be invalid after deserialization when the object it
        * referenced to does not exist anymore.
        *
        * @return true or false depending on its state
        */
        public boolean isValid () {
            return getDataObject ().isValid ();
        }
        
        /** Test whether the object is modified or not.
         * @return true if the object is modified
         */
        public boolean isModified() {
            return getDataObject ().isModified ();
        }

        /** Support for marking the environement modified.
        * @exception IOException if the environment cannot be marked modified
        *   (for example when the file is readonly), when such exception
        *   is the support should discard all previous changes
        */
        public void markModified() throws java.io.IOException {
            getDataObject ().setModified (true);
        }
        
        /** Reverse method that can be called to make the environment 
        * unmodified.
        */
        public void unmarkModified() {
            getDataObject ().setModified (false);
        }
        
        /** Method that allows environment to find its 
         * cloneable open support.
        * @return the support or null if the environemnt is not in valid 
        * state and the CloneableOpenSupport cannot be found for associated
        * data object
        */
        public CloneableOpenSupport findCloneableOpenSupport() {
            return (CloneableOpenSupport)getDataObject ().getCookie (CloneableOpenSupport.class);
        }
        
        /** Accepts property changes from DataObject and fires them to
        * own listeners.
        */
        public void propertyChange(PropertyChangeEvent ev) {
            if (DataObject.PROP_MODIFIED.equals (ev.getPropertyName())) {
                if (getDataObject ().isModified ()) {
                    getDataObject ().addVetoableChangeListener(this);
                } else {
                    getDataObject ().removeVetoableChangeListener(this);
                }
            }
            
            firePropertyChange (
                ev.getPropertyName (),
                ev.getOldValue (),
                ev.getNewValue ()
            );
        }
        
        /** Accepts vetoable changes and fires them to own listeners.
        */
        public void vetoableChange(PropertyChangeEvent ev) throws PropertyVetoException {
            fireVetoableChange (
                ev.getPropertyName (),
                ev.getOldValue (),
                ev.getNewValue ()
            );
        }
        
        /** Fires property change.
        * @param name the name of property that changed
        * @param oldValue old value
        * @param newValue new value
        */
        protected void firePropertyChange (String name, Object oldValue, Object newValue) {
            prop ().firePropertyChange (name, oldValue, newValue);
        }
        
        /** Fires vetoable change.
        * @param name the name of property that changed
        * @param oldValue old value
        * @param newValue new value
        */
        protected void fireVetoableChange (String name, Object oldValue, Object newValue) 
        throws PropertyVetoException {
            veto ().fireVetoableChange (name, oldValue, newValue);
        }
        
        /** Lazy getter for change support.
        */
        private PropertyChangeSupport prop () {
            synchronized (LOCK_SUPPORT) {
                if (propSupp == null) {
                    propSupp = new PropertyChangeSupport (this);
                }
                return propSupp;
            }
        }
        
        /** Lazy getter for veto support.
        */
        private VetoableChangeSupport veto () {
            synchronized (LOCK_SUPPORT) {
                if (vetoSupp == null) {
                    vetoSupp = new VetoableChangeSupport (this);
                }
                return vetoSupp;
            }
        }
    }
    
    
    /** Listener for <code>FileSystem.PROP_SYSTEM_NAME</code> proeperty. */
    private static final class FileSystemNameListener
    implements PropertyChangeListener, VetoableChangeListener {
        /** Set of Env's interested in changes on fs name. */
        private final Set environments = new WeakSet(30);
        
        public FileSystemNameListener() {
        }

        /** Adds another Env which is interested on fs name changes. */
        public void add(Env env) {
            synchronized(environments) {
                environments.add(env);
            }
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            if(FileSystem.PROP_SYSTEM_NAME.equals(evt.getPropertyName())) {
                Set envs;
                synchronized(environments) {
                    envs = new HashSet(environments);
                }
                
                for(Iterator it = envs.iterator(); it.hasNext(); ) {
                    Env env = (Env)it.next();
                    env.firePropertyChange(DataObject.PROP_VALID,
                        Boolean.TRUE, Boolean.FALSE);
                }
            }
        }
        
        public void vetoableChange(PropertyChangeEvent evt) 
        throws PropertyVetoException {
            if(FileSystem.PROP_SYSTEM_NAME.equals(evt.getPropertyName())) {
                Set envs;
                synchronized(environments) {
                    envs = new HashSet(environments);
                }
                
                for(Iterator it = envs.iterator(); it.hasNext(); ) {
                    Env env = (Env)it.next();
                    env.fireVetoableChange(DataObject.PROP_VALID,
                        Boolean.TRUE, Boolean.FALSE);
                }
            }
        }
    } // End of class FileSystemNameListener.

    /** Only for backward compatibility of settings
    */
    private static final class Listener extends CloneableTopComponent.Ref {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -1934890789745432531L;
        /** entry to serialize */
        private MultiDataObject.Entry entry;
        
        Listener() {}

        public Object readResolve () {
            DataObject obj = entry.getDataObject ();
            OpenSupport os = (OpenSupport)obj.getCookie (OpenSupport.class);
            if (os == null) {
                // problem! no replace!?
                return this;
            }
            // use the editor support's CloneableTopComponent.Ref
            return os.allEditors ();
        }
    }
}

