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

package org.openide.filesystems;

import java.beans.*;
import java.io.IOException;
import java.text.MessageFormat;

import org.openide.util.actions.SystemAction;
import org.openide.util.NbBundle;

/** Interface that provides basic information about a virtual
* filesystem in the IDE. Classes that implement it
* should follow JavaBean conventions because when a new
* instance of a filesystem class is inserted into the system, it should
* permit the user to modify it with standard Bean properties.
* <P>
* Implementing classes should also have associated subclasses of {@link FileObject}.
* <p>Although the class is serializable, only the {@link #isHidden hidden state} and {@link #getSystemName system name}
* are serialized, and the deserialized object is by default {@link #isValid invalid} (and may be a distinct
* object from a valid filesystem in the Repository). If you wish to safely deserialize a file
* system, you should after deserialization try to replace it with a filesystem of the
* {@link Repository#findFileSystem same name} in the Repository.
* @author Jaroslav Tulach
*/
public abstract class FileSystem implements java.io.Serializable {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -8931487924240189180L;

    /** Property name indicating validity of filesystem. */
    public static final String PROP_VALID = "valid"; // NOI18N
    /** Property name indicating whether filesystem is hidden. */
    public static final String PROP_HIDDEN = "hidden"; // NOI18N
    /** Property name giving internal system name of filesystem. */
    public static final String PROP_SYSTEM_NAME = "systemName"; // NOI18N
    /** Property name giving display name of filesystem.
     * @since 2.1
     */
    public static final String PROP_DISPLAY_NAME = "displayName"; // NOI18N    
    /** Property name giving root folder of filesystem. */
    public static final String PROP_ROOT = "root"; // NOI18N
    /** Property name giving read-only state. */
    public static final String PROP_READ_ONLY = "readOnly"; // NOI18N
    /** Property name giving capabilities state. */    
    static final String PROP_CAPABILITIES  = "capabilities"; // NOI18N    


    /** is this filesystem valid?
    * It can be invalid if there is another filesystem with the
    * same name in the filesystem pool.
    */
    transient private boolean valid = false;

    /** True if the filesystem is assigned to pool.
    * Is modified from Repository methods.
    */
    transient boolean assigned = false;
    
    /**Repository that contains this FileSystem or null*/
    private transient Repository repository = null;
    
    private transient FCLSupport fclSupport;
    

    /** Describes capabilities of the filesystem.
    */
    private FileSystemCapability capability;

    /** property listener on FileSystemCapability. */
    private transient PropertyChangeListener  capabilityListener;

    /** hidden flag */
    private boolean hidden = false;

    /** system name */
    private String systemName = "".intern (); // NOI18N

    /** Utility field used by event firing mechanism. */
    private transient ListenerList fileStatusList;
    private transient ListenerList vetoableChangeList;    
    

    private transient PropertyChangeSupport changeSupport;
    
    /** Used for synchronization purpose*/
    private static  Object internLock = new Object ();
    
    
    /** Default constructor. */    
    public  FileSystem () {
        capability = new FileSystemCapability.Bean ();
        capability.addPropertyChangeListener(getCapabilityChangeListener ());        
    }

    /** Should check for external modifications. All existing FileObjects will be
     * refreshed. For folders it should reread the content of disk,
     * for data file it should check for the last time the file has been modified.
     *
     * The default implementation is to do nothing, in contradiction to the rest 
     * of the description. Unless subclasses override it, the method does not work.      
     *
     * @param expected should the file events be marked as expected change or not?
     * @see FileEvent#isExpected
     * @since 2.16
     */
    public void refresh (boolean expected) {
    }
    
    /** Test whether filesystem is valid.
    * Generally invalidity would be caused by a name conflict in the filesystem pool.
    * @return true if the filesystem is valid
    */
    public final boolean isValid () {
        return valid;
    }

    /** Setter for validity. Accessible only from filesystem pool.
    * @param v the new value
    */
    final void setValid (boolean v) {
        if (v != valid) {
            valid = v;
            firePropertyChange (PROP_VALID,
                                !v ? Boolean.TRUE : Boolean.FALSE,
                                v ? Boolean.TRUE : Boolean.FALSE,
                                Boolean.FALSE);
        }
    }

    /** Set hidden state of the object.
     * A hidden filesystem is not presented to the user in the Repository list (though it may be present in the Repository Settings list).
    *
    * @param hide <code>true</code> if the filesystem should be hidden
    */
    public final void setHidden (boolean hide) {
        if (hide != hidden) {
            hidden = hide;
            firePropertyChange (PROP_HIDDEN,
                                !hide ? Boolean.TRUE : Boolean.FALSE,
                                hide ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    /** Getter for the hidden property.
    */
    public final boolean isHidden () {
        return hidden;
    }

    /** Tests whether filesystem will survive reloading of system pool.
    * If true then when
    * {@link Repository} is reloading its content, it preserves this
    * filesystem in the pool.
    * <P>
    * This can be used when the pool contains system level and user level
    * filesystems. The system ones should be preserved when the user changes
    * the content (for example when he is loading a new project).
    * <p>The default implementation returns <code>false</code>.
    *
    * @return true if the filesystem should be persistent
    */
    protected boolean isPersistent () {
        return false;
    }

    /** Provides a name for the system that can be presented to the user.
    * <P>
    * This call should <STRONG>never</STRONG> be used to attempt to identify the file root
    * of the filesystem. On some systems it may happen to look the same but this is a
    * coincidence and may well change in the future. Either check whether
    * you are working with a {@link LocalFileSystem} or similar implementation and use
    * {@link LocalFileSystem#getRootDirectory}; or better, try
    * {@link FileUtil#toFile} which is designed to do this correctly.
    *
    * @return user presentable name of the filesystem
    */
    public abstract String getDisplayName ();

    /** Internal (system) name of the filesystem.
    * Should uniquely identify the filesystem, as it will
    * be used during serialization of its files. The preferred way of doing this is to concatenate the
    * name of the filesystem type (e.g. the class) and the textual form of its parameters.
    * <P>
    * A change of the system name should be interpreted as a change of the internal
    * state of the filesystem. For example, if the root directory is moved to different
    * location, one should rebuild representations for all files
    * in the system.
    * <P>
    * This call should <STRONG>never</STRONG> be used to attempt to identify the file root
    * of the filesystem. On Unix systems it may happen to look the same but this is a
    * coincidence and may well change in the future. Either check whether
    * you are working with a {@link LocalFileSystem} or similar implementation and use
    * {@link LocalFileSystem#getRootDirectory}; or better, try
    * {@link FileUtil#toFile} which is designed to do this correctly.
    *
    * @return string with system name
    */
    public final String getSystemName () {
        return systemName;
    }

    /** Changes system name of the filesystem.
    * This property is bound and constrained: first of all
    * all vetoable listeners are asked whether they agree with the change. If so,
    * the change is made and all change listeners are notified of
    * the change.
    *
    * <p><em>Warning:</em> this method is protected so that only subclasses can change
    *    the system name.
    *
    * @param name new system name
    * @exception PropertyVetoException if the change is not allowed by a listener
    */
    protected final void setSystemName (String name)
    throws PropertyVetoException {
        synchronized (Repository.class) {
            if (systemName.equals (name)) {
                return;
            }

            // I must be the only one who works with system pool (that is listening)
            // on this interface
            fireVetoableChange (PROP_SYSTEM_NAME, systemName, name);

            String old = systemName;
            systemName = name.intern ();

            firePropertyChange (PROP_SYSTEM_NAME, old, systemName);
            /** backward compatibility for FileSystems that don`t fire 
             * PROP_DISPLAY_NAME*/            
            firePropertyChange (PROP_DISPLAY_NAME, null, null);        
        }
    }
    
    /** Returns <code>true</code> if the filesystem is default one of the IDE.
     * @see Repository#getDefaultFileSystem
    */
    public final boolean isDefault () {
        return this == ExternalUtil.getRepository ().getDefaultFileSystem ();
    }

    /** Test if the filesystem is read-only or not.
    * @return true if the system is read-only
    */
    public abstract boolean isReadOnly ();

    /** Getter for root folder in the filesystem.
    *
    * @return root folder of whole filesystem
    */
    public abstract FileObject getRoot ();

    /** Finds file in the filesystem by name.
    * <P>
    * The default implementation converts dots in the package name into slashes,
    * concatenates the strings, adds any extension prefixed by a dot and calls
    * the {@link #findResource findResource} method.
    *
    * <p><em>Note:</em> when both of <code>name</code> and <code>ext</code> are <CODE>null</CODE> then name and
    *    extension should be ignored and scan should look only for a package.
    *
    * @param aPackage package name where each package component is separated by a dot
    * @param name name of the file (without dots) or <CODE>null</CODE> if
    *    one wants to obtain a folder (package) and not a file in it
    * @param ext extension of the file (without leading dot) or <CODE>null</CODE> if one needs
    *    a package and not a file
    *
    * @return a file object that represents a file with the given name or
    *   <CODE>null</CODE> if the file does not exist
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead, or use {@link #findResource} if you are not interested in classpaths.
    */
    public FileObject find (String aPackage, String name, String ext) {
        StringBuffer bf = new StringBuffer ();

        // append package and name
        if (!aPackage.equals ("")) { // NOI18N
            String p = aPackage.replace ('.', '/');
            bf.append (p);
            bf.append ('/');
        }

        // append name
        if (name != null) {
            bf.append (name);
        }

        // append extension if there is one
        if (ext != null) {
            bf.append ('.');
            bf.append (ext);
        }
        return findResource (bf.toString ());
    }

    /** Finds a file given its full resource path.
    * @param name the resource path, e.g. "dir/subdir/file.ext" or "dir/subdir" or "dir"
    * @return a file object with the given path or
    *   <CODE>null</CODE> if no such file exists
    */
    public abstract FileObject findResource (String name);

    /** Returns an array of actions that can be invoked on any file in
    * this filesystem.
    * These actions should preferably
    * support the {@link org.openide.util.actions.Presenter.Menu Menu},
    * {@link org.openide.util.actions.Presenter.Popup Popup},
    * and {@link org.openide.util.actions.Presenter.Toolbar Toolbar} presenters.
    *
    * @return array of available actions
    */
    public abstract SystemAction[] getActions ();

    public SystemAction[] getActions (java.util.Set foSet) {
         return this.getActions();
     }
  

    /** Reads object from stream and creates listeners.
    * @param in the input stream to read from
    * @exception IOException error during read
    * @exception ClassNotFoundException when class not found
    */
    private void readObject (java.io.ObjectInputStream in)
    throws java.io.IOException, java.lang.ClassNotFoundException {
        in.defaultReadObject ();

        if (capability == null) 
            capability = new FileSystemCapability.Bean ();
        
        capability.addPropertyChangeListener(getCapabilityChangeListener ());                            
    }

    public String toString () {
        return getSystemName () + "[" + super.toString () + "]"; // NOI18N
    }


    /** Allows filesystems to set up the environment for external execution
    * and compilation.
    * Each filesystem can add its own values that
    * influence the environment. The set of operations that can modify
    * environment is described by the {@link Environment} interface.
    * <P>
    * The default implementation throws an exception to signal that it does not
    * support external compilation or execution.
    *
    * @param env the environment to setup
    * @exception EnvironmentNotSupportedException if external execution
    *    and compilation cannot be supported
    */
    public void prepareEnvironment (Environment env)
    throws EnvironmentNotSupportedException {
        throw new EnvironmentNotSupportedException (this);
    }

    /** Get a status object that can annotate a set of files by changing the names or icons
    * associated with them.
    * <P>
    * The default implementation returns a status object making no modifications.
    *
    * @return the status object for this filesystem
    */
    public Status getStatus () {
        return STATUS_NONE;
    }

    /** The object describing capabilities of this filesystem.
    * Subclasses can override it.
    */
    public final FileSystemCapability getCapability () {
        return capability;
    }

    /** Allows subclasses to change a set of capabilities of the
    * filesystem.
    * @param capability the capability to use
    */
    protected final void setCapability (FileSystemCapability capability) {
        if (this.capability != null)
            this.capability.removePropertyChangeListener(getCapabilityChangeListener ());
        this.capability = capability;
        if (this.capability != null)
            this.capability.addPropertyChangeListener (getCapabilityChangeListener ());
    }

    /** Executes atomic action. The atomic action represents a set of
    * operations constituting one logical unit. It is guaranteed that during
    * execution of such an action no events about changes in the filesystem
    * will be fired.
    * <P>
    * <em>Warning:</em> the action should not take a significant amount of time, and should finish as soon as
    * possible--otherwise all event notifications will be blocked.
    * <p><strong>Warning:</strong> do not be misled by the name of this method;
    * it does not require the filesystem to treat the changes as an atomic block of
    * commits in the database sense! That is, if an exception is thrown in the middle
    * of the action, partial results will not be undone (in general this would be
    * impossible to implement for all filesystems anyway).
    * @param run the action to run
    * @exception IOException if there is an <code>IOException</code> thrown in the actions' {@link AtomicAction#run run}
    *    method
    */
    public  final void runAtomicAction (final AtomicAction run) throws IOException {
        getEventControl ().runAtomicAction (run);
    }

    /**
     * Begin of block, that should be performed without firing events.
     * Firing of events is postponed after end of block .
     * There is strong necessity to use always both methods: beginAtomicAction
     * and finishAtomicAction. It is recomended use it in try - finally block.
     * @param run Events fired from this atomic action will be marked as events 
     * that were fired from this run.
     */    
    void beginAtomicAction (FileSystem.AtomicAction run) {
        getEventControl ().beginAtomicAction (run);
    }
    
    void beginAtomicAction () {
        beginAtomicAction (null);
    }

    /**
     * End of block, that should be performed without firing events.
     * Firing of events is postponed after end of block .
     * There is strong necessity to use always both methods: beginAtomicAction
     * and finishAtomicAction. It is recomended use it in try - finally block.
     */        
    void finishAtomicAction () {
        getEventControl ().finishAtomicAction ();
    }

    
    /** 
     *  Inside atomicAction adds an event dispatcher to the queue of FS events
     *  and firing of events is postponed. If not event handlers are called directly.
     * @param run dispatcher to run
     */
    void dispatchEvent (EventDispatcher run) {
        getEventControl ().dispatchEvent (run);    
    }

    /** returns property listener on FileSystemCapability. */
    private synchronized PropertyChangeListener getCapabilityChangeListener () {
        if (capabilityListener == null) {
            capabilityListener = new PropertyChangeListener() {                
                public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
                    firePropertyChange(PROP_CAPABILITIES,
                    propertyChangeEvent.getOldValue() , propertyChangeEvent.getNewValue());
                }
            };
        }        
        return capabilityListener;
    }    
    
    private transient  static ThreadLocal thrLocal = new ThreadLocal ();    
    private final EventControl getEventControl () {
        EventControl evnCtrl = (EventControl)thrLocal.get();    
        if (evnCtrl == null)
            thrLocal.set(evnCtrl = new EventControl ());
        return evnCtrl;
    }

    /** Registers FileStatusListener to receive events.
    * The implementation registers the listener only when getStatus () is 
    * overriden to return a special value.
    *
    * @param listener The listener to register.
    */
    public final void addFileStatusListener (
        org.openide.filesystems.FileStatusListener listener
    ) {
        synchronized (internLock) {
            // JST: Ok? Do not register listeners when the fs cannot change status?
            if (getStatus () == STATUS_NONE) return;

            if (fileStatusList == null) 
                fileStatusList = new ListenerList (FileStatusListener.class);
            
            fileStatusList.add (listener);
        }
    }

    /** Removes FileStatusListener from the list of listeners.
     *@param listener The listener to remove.
     */
    public final void removeFileStatusListener (
        org.openide.filesystems.FileStatusListener listener
    ) {
        if (fileStatusList == null) return;

        fileStatusList.remove (listener);
    }

    /** Notifies all registered listeners about change of status of some files.
    *
    * @param event The event to be fired
    */
    protected final void fireFileStatusChanged(FileStatusEvent event) {
        if (fileStatusList == null) return;


        Object[] listeners = fileStatusList.getAllListeners ();
        for (int i = 0; i < listeners.length; i++) {
            ((org.openide.filesystems.FileStatusListener)listeners[i]).annotationChanged (event);
            
        }
    }

    /** Adds listener for the veto of property change.
    * @param listener the listener
    */
    public final void addVetoableChangeListener(
        java.beans.VetoableChangeListener listener
    ) {
        synchronized (internLock) {
            if (vetoableChangeList == null) 
                vetoableChangeList = new ListenerList (VetoableChangeListener.class);

            vetoableChangeList.add (listener);
        }
    }

    /** Removes listener for the veto of property change.
    * @param listener the listener
    */
    public final void removeVetoableChangeListener(
        java.beans.VetoableChangeListener listener
    ) {
        if (vetoableChangeList == null) return;

        vetoableChangeList.remove (listener);
    }

    /** Fires property vetoable event.
    * @param name name of the property
    * @param o old value of the property
    * @param n new value of the property
    * @exception PropertyVetoException if an listener vetoed the change
    */
    protected final void fireVetoableChange (
        java.lang.String name,
        java.lang.Object o,
        java.lang.Object n
    ) throws PropertyVetoException {
        if (vetoableChangeList == null) return;


        java.beans.PropertyChangeEvent e = null;
        Object[] listeners = vetoableChangeList.getAllListeners ();
        for (int i = 0; i < listeners.length; i++) {
                if (e == null)
                    e = new java.beans.PropertyChangeEvent (this, name, o, n);
                ((java.beans.VetoableChangeListener)listeners[i]).vetoableChange (e);
        }
    }

    /** Registers PropertyChangeListener to receive events.
    *@param listener The listener to register.
    */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized (internLock) {
            if (changeSupport == null)
                changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    /** Removes PropertyChangeListener from the list of listeners.
    *@param listener The listener to remove.
    */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null)
            changeSupport.removePropertyChangeListener(listener);
    }
    
    /** Fires property change event.
    * @param name name of the property
    * @param o old value of the property
    * @param n new value of the property
    */
    protected final void firePropertyChange (String name, Object o, Object n) {
        firePropertyChange (name, o, n, null);
    }
        
    final void firePropertyChange (String name, Object o, Object n, Object propagationId) {
        if (changeSupport == null)
            return;

        if (o != null && n != null && o.equals(n))
            return;
        
        PropertyChangeEvent e = new PropertyChangeEvent(this, name, o, n);
        e.setPropagationId(propagationId);
        changeSupport.firePropertyChange(e);
    }

    /** Notifies this filesystem that it has been added to the repository.
    * Various initialization tasks could go here. The default implementation does nothing.
    * <p>Note that this method is <em>advisory</em> and serves as an optimization
    * to avoid retaining resources for too long etc. Filesystems should maintain correct
    * semantics regardless of whether and when this method is called.
    */
    public void addNotify () {
    }

    /** Notifies this filesystem that it has been removed from the repository.
    * Concrete filesystem implementations could perform clean-up here.
    * The default implementation does nothing.
    * <p>Note that this method is <em>advisory</em> and serves as an optimization
    * to avoid retaining resources for too long etc. Filesystems should maintain correct
    * semantics regardless of whether and when this method is called.
    */
    public void removeNotify () {
    }

    /** An action that it is to be called atomically with respect to filesystem event notification.
    * During its execution (via {@link FileSystem#runAtomicAction runAtomicAction})
    * no events about changes in filesystems are fired.
    */
    public static interface AtomicAction {
        /** Executed when it is guaranteed that no events about changes
        * in filesystems will be notified.
        *
        * @exception IOException if there is an error during execution
        */
        public void run () throws IOException;
    }

    /** Interface that allows filesystems to set up the Java environment
    * for external execution and compilation.
    * Currently just used to append entries to the external class path.
    * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
    */
    public static abstract class Environment extends Object {
        /** Adds one element to the class path environment variable.
        * @param classPathElement string representing the one element
        * @deprecated Please use the <a href="@JAVA/API@/org/netbeans/api/java/classpath/api.html">ClassPath API</a> instead.
        */
        public void addClassPath (String classPathElement) {
        }
    }

    /** Allows a filesystem to annotate a group of files (typically comprising a data object) with additional markers.
     * <p>This could be useful, for
    * example, for a filesystem supporting version control.
    * It could annotate names and icons of data nodes according to whether the files were current, locked, etc.
    */
    public static interface Status {
        /** Annotate the name of a file cluster.
        * @param name the name suggested by default
        * @param files an immutable set of {@link FileObject}s belonging to this filesystem
        * @return the annotated name (may be the same as the passed-in name)
        * @exception ClassCastException if the files in the set are not of valid types
        */
        public String annotateName (String name, java.util.Set files);

        /** Annotate the icon of a file cluster.
         * <p>Please do <em>not</em> modify the original; create a derivative icon image,
         * using a weak-reference cache if necessary.
        * @param icon the icon suggested by default
        * @param iconType an icon type from {@link java.beans.BeanInfo}
        * @param files an immutable set of {@link FileObject}s belonging to this filesystem
        * @return the annotated icon (may be the same as the passed-in icon)
        * @exception ClassCastException if the files in the set are not of valid types
        */
        public java.awt.Image annotateIcon (java.awt.Image icon, int iconType, java.util.Set files);
    }

    /** Empty status */
    private static final Status STATUS_NONE = new Status () {
                public String annotateName (String name, java.util.Set files) {
                    return name;
                }

                public java.awt.Image annotateIcon (java.awt.Image icon, int iconType, java.util.Set files) {
                    return icon;
                }
            };

    /** Class used to notify events for the filesystem.
    */
    static abstract class EventDispatcher extends Object implements Runnable {
        public final void run () {
            dispatch ();
        }

        final void dispatch () {
            dispatch (false);
        }
        /** @param onlyPriority if true then invokes only priority listeners
         *  else all listeners are invoked.
         */        
        protected abstract void dispatch (boolean onlyPriority);                
        /** @param propID  */        
        protected abstract void setAtomicActionLink (EventControl.AtomicActionLink propID);                        
    }
    
    
    /** Getter for the resource string
    * @param s the resource name
    * @return the resource
    */
    static String getString(String s) {
        /*This call to getBundle should ensure that currentClassLoader is not used to load resources from. 
         This should prevent from deadlock, that occured: one waits for FileObject and has resource, 
         second one waits for resource and has FileObject*/
        return NbBundle.getBundle("org.openide.filesystems.Bundle", java.util.Locale.getDefault(), FileSystem.class.getClassLoader ()).getString (s);
        
    }

    /** Creates message for given string property with one parameter.
    * @param s resource name
    * @param obj the parameter to the message
    * @return the string for that text
    */
    static String getString (String s, Object obj) {
        return MessageFormat.format (getString (s), new Object[] { obj });
    }

    /** Creates message for given string property with two parameters.
    * @param s resource name
    * @param obj1 the parameter to the message
    * @param obj2 the parameter to the message
    * @return the string for that text
    */
    static String getString (String s, Object obj1, Object obj2) {
        return MessageFormat.format (getString (s), new Object[] { obj1, obj2 });
    }

    /** Creates message for given string property with three parameters.
    * @param s resource name
    * @param obj1 the parameter to the message
    * @param obj2 the parameter to the message
    * @param obj3 the parameter to the message
    * @return the string for that text
    */
    static String getString (String s, Object obj1, Object obj2, Object obj3) {
        return MessageFormat.format (getString (s), new Object[] { obj1, obj2, obj3 });
    }

    /** getter for Repository
    * @return Repository that contains this FileSystem or null if FileSystem
    * is not part of any Repository
    */
    final Repository getRepository() {
        return repository;
    }

    void setRepository(Repository rep) {
        repository = rep;
    }
    
    final FCLSupport getFCLSupport() {
        synchronized (FCLSupport.class) {
            if (fclSupport == null)
                fclSupport = new FCLSupport ();
        }        
        return fclSupport;
    }
    
    /** Add new listener to this object.
    * @param fcl the listener
    * @since 2.8    
    */    
    public final void addFileChangeListener(FileChangeListener fcl) {
        getFCLSupport ().addFileChangeListener(fcl);
    }

    /** Remove listener from this object.
    * @param fcl the listener
    * @since 2.8    
    */    
    public final void removeFileChangeListener(FileChangeListener fcl) {
        getFCLSupport ().removeFileChangeListener(fcl);        
    }
    
}
