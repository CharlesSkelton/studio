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

package org.openide.util;

import java.beans.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.EventListener;
import java.util.EventObject;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import javax.swing.event.*;

import org.openide.ErrorManager;
import org.openide.filesystems.*;
import org.openide.loaders.OperationListener;
import org.openide.loaders.OperationEvent;
import org.openide.nodes.*;

/**
 * A listener wrapper that delegates to another listener but hold
 * only weak reference to it, so it does not prevent it to be finalized.
 *
 * In the following examples, I'll use following naming:<BR>
 * There are four objects involved in WeakListener usage:<UL>
 *  <LI>The event <em>source</em> object
 *  <LI>The <em>observer</em> - object that wants to listen on <em>source</em>
 *  <LI>The <em>listener</em> - the implementation of the corresponding
 *     <code>*Listener</code> interface, sometimes the observer itself but
 *     often some observer's inner class delegating the events to the observer.
 *  <LI>The <em>WeakListener</em> implementation.
 * </UL>
 * The examples are written for ChangeListener. The <code>WeakListener</code>
 * have factory methods for the most common listeners used in NetBeans
 * and also one universal factory method you can use for other listeners.
 *
 * <H2>How to use it:</H2>
 * Here is an example how to write a listener/observer and make it listen
 * on some source:
 * <pre>
 *  public class ListenerObserver implements ChangeListener {
 *      private void registerTo(Source source) {
 *          source.addChangeListener({@link
                #change(javax.swing.event.ChangeListener, java.lang.Object)
 *              WeakListener.change} (this, source));
 *      }
 *      
 *      public void stateChanged(ChangeEvent e) {
 *          doSomething();
 *      }
 *  }
 * </pre>
 * You can also factor out the listener implementation to some other class
 * if you don't want to expose the stateChanged method (better technique):
 * <pre>
 *  public class Observer {
 *      <b>private Listener listener;</b>
 *
 *      private void registerTo(Source source) {
 *          <b>listener = new Listener()</b>;
 *          source.addChangeListener({@link
                #change(javax.swing.event.ChangeListener, java.lang.Object)
 *              WeakListener.change} (listener, source));
 *      }
 *      
 *      private class Listener implements ChangeListener {
 *          public void stateChanged(ChangeEvent e) {
 *              doSomething();
 *          }
 *      }
 *  }
 * </pre>
 * Note: The observer keeps the reference to the listener, it won't work
 * otherwise, see below.
 *
 * <P>You can also use the universal factory for other listeners:
 * <pre>
 *  public class Observer implements SomeListener {
 *      private void registerTo(Source source) {
 *          source.addSomeListener((SomeListener){@link
 *              #create(java.lang.Class, java.util.EventListener, java.lang.Object)
 *              WeakListener.create} (
 *                  SomeListener.class, this, source));
 *      }
 *      
 *      public void someEventHappened(SomeEvent e) {
 *          doSomething();
 *      }
 *  }
 * </pre>
 *
 * <H2>How to <font color=red>not</font> use it:</H2>
 * Here are examples of a common mistakes done when using <code>WeakListener</code>:
 * <pre>
 *  public class Observer {
 *      private void registerTo(Source source) {
 *          source.addChangeListener(WeakListener.change(<b>new Listener()</b>, source));
 *      }
 *      
 *      private class Listener implements ChangeListener {
 *          public void stateChanged(ChangeEvent e) {
 *              doSomething();
 *          }
 *      }
 *  }
 * </pre>
 * Mistake: There is nobody holding strong reference to the Listener instance,
 * so it may be freed on the next GC cycle.
 *
 * <BR><pre>
 *  public class ListenerObserver implements ChangeListener {
 *      private void registerTo(Source source) {
 *          source.addChangeListener(WeakListener.change(this, <b>null</b>));
 *      }
 *      
 *      public void stateChanged(ChangeEvent e) {
 *          doSomething();
 *      }
 *  }
 * </pre>
 * Mistake: The WeakListener is unable to unregister itself from the source
 * once the listener is freed. For explanation, read below.
 *
 <H2>How does it work:</H2>
 * <P>The <code>WeakListener</code> is used as a reference-weakening wrapper
 *  around the listener. It is itself strongly referenced from the implementation
 *  of the source (e.g. from its <code>EventListenerList</code>) but it references
 *  the listener only through <code>WeakReference</code>. It also weak-references
 *  the source. Listener, on the other hand, usually strongly references
 *  the observer (typically through the outer class reference).
 *
 * This means that: <OL>
 * <LI>If the listener is not strong-referenced from elsewhere, it can be
 *  thrown away on the next GC cycle. This is why you can't use 
 *  <code>WeakListener.change(new MyListener(), ..)</code> as the only reference
 *  to the listener will be the weak one from the WeakListener.
 * <LI>If the listener-observer pair is not strong-referenced from elsewhere
 *  it can be thrown away on the next GC cycle. This is what the
 *  <code>WeakListener</code> was invented for.
 * <LI>If the source is not strong-referenced from anywhere, it can be
 *  thrown away on the next GC cycle taking the WeakListener with it,
 *  but not the listener and the observer if they are still strong-referenced
 *  (unusual case, but possible).
 * </OL>
 *
 * <P>Now what happens when the listener/observer is removed from memory:<UL>
 * <LI>The WeakListener is notified that the reference to the listener was cleared.
 * <LI>It tries to unregister itself from the source. This is why it needs
 *  the reference to the source for the registration. The unregistration
 *  is done using reflection, usually looking up the method
 *  <code>remove&lt;listenerType&gt;</code> of the source and calling it.
 *  </UL>
 *
 *  <P>This may fail if the source don't have the expected <code>remove*</code>
 *  method and/or if you provide wrong reference to source. In that case
 *  the WeakListener instance will stay in memory and registered by the source,
 *  while the listener and observer will be freed.
 *  
 *  <P>There is still one fallback method - if some event come to a WeakListener
 *  and the listener is already freed, the WeakListener tries to unregister
 *  itself from the object the event came from.
 *
 * @author Jaroslav Tulach
 */
public abstract class WeakListener implements java.util.EventListener {
    /** weak reference to listener */
    private Reference ref;
    /** class of the listener */
    Class listenerClass;
    /** weak reference to source */
    private Reference source;

    /**
     * @param listenerClass class/interface of the listener
     * @param l listener to delegate to, <code>l</code> must be an instance of
     * listenerClass
     */
    protected WeakListener (Class listenerClass, java.util.EventListener l) {
        this.listenerClass = listenerClass;
        ref = new ListenerReference (l, this);
        if (!listenerClass.isAssignableFrom(l.getClass())) {
            throw new IllegalArgumentException(getClass().getName() + " constructor is calling WeakListner.<init> with illegal arguments"); // NOI18N
        }
    }

    /** Setter for the source field. If a WeakReference to an underlying listener is
     * cleared and enqueued, that is, the original listener is garbage collected,
     * then the source field is used for deregistration of this WeakListener, thus making
     * it eligible for garbage collection if no more references exist.
     *
     * This method is particularly useful in cases where the underlying listener was
     * garbage collected and the event source, on which this listener is listening on,
     * is quiet, i.e. does not fire any events for long periods. In this case, this listener
     * is not removed from the event source until an event is fired. If the source field is
     * set however, WeakListeners that lost their underlying listeners are removed
     * as soon as the ReferenceQueue notifies the WeakListener.
     *
     * @param source is any Object or <code>null</code>, though only setting an object
     * that has an appropriate remove*listenerClass*Listener method and on which this listener is listening on,
     * is useful.
     */
    protected final void setSource (Object source) {
        if (source == null) {
            this.source = null;
        } else {
            this.source = new WeakReference (source);
        }
    }

    /** Method name to use for removing the listener.
    * @return name of method of the source object that should be used
    *   to remove the listener from listening on source of events
    */
    protected abstract String removeMethodName ();

    /** Getter for the target listener.
    * @param ev the event the we want to distribute
    * @return null if there is no listener because it has been finalized
    */
    protected final java.util.EventListener get (java.util.EventObject ev) {
        Object l = ref.get (); // get the consumer

	// if the event consumer is gone, unregister us from the event producer
	if (l == null) removeListener (ev == null ? null : ev.getSource ());

        return (EventListener)l;
    }

    /** Tries to find a remove method and invoke it.
     * It tries unregister itself from the registered source first
     * and then from the passed eventSource if they are not same.
     * 
     * @param eventSource the source object to unregister from or null
     */
    private void removeListener (Object eventSource) {
        Object[] params = new Object[] { getImplementator() };
        Object src = source == null ? null : source.get();
        try {
            Method m = null;
            if (src != null) {
                m = getRemoveMethod(src);
                if (m != null) m.invoke (src, params);
            }
            if (eventSource != src && eventSource != null) {
                m = getRemoveMethod(eventSource);
                if (m != null) m.invoke (eventSource, params);
            }
            if (m == null && source == null) { // can't remove the listener
                ErrorManager.getDefault().log (ErrorManager.WARNING,
                    "Can't remove " + listenerClass.getName() +
                    "source=" + source +
                    ", src=" + src + ", eventSource=" + eventSource);
            }
        } catch (Exception ex) { // from invoke(), should not happen
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
        }
    }

    /* can return null */
    private final Method getRemoveMethod(Object src) {
        final Class[] clarray = new Class[] { listenerClass };
        String methodName = removeMethodName();
        Class methodClass = src.getClass();
        Method m = null;
        
        try {
            m = methodClass.getMethod(methodName, clarray);
        } catch (NoSuchMethodException e) {
            do {
                try {
                    m = methodClass.getDeclaredMethod(methodName, clarray);
                } catch (NoSuchMethodException ex) {
                }
                methodClass = methodClass.getSuperclass();
            } while ((m == null) && (methodClass != Object.class));
        }
        
        if (m != null && (!Modifier.isPublic(m.getModifiers()) || 
                          !Modifier.isPublic(m.getDeclaringClass().getModifiers()))) {
            m.setAccessible(true);
        }
        return m;
    }

 
    Object getImplementator() {
        return this;
    }

    public String toString () {
        Object listener = ref.get();
        return getClass().getName() + "[" + (listener == null ? "null" : listener.getClass().getName() + "]");
    }

    //
    // Methods for establishing connections
    //

    /** Creates a weak implementation of NodeListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a NodeListener delegating to <CODE>l</CODE>.
     */
    public static NodeListener node (NodeListener l, Object source) {
        WeakListener.Node wl = new WeakListener.Node (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of PropertyChangeListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a PropertyChangeListener delegating to <CODE>l</CODE>.
     */
    public static PropertyChangeListener propertyChange (PropertyChangeListener l, Object source) {
        WeakListener.PropertyChange wl = new WeakListener.PropertyChange (l);
        wl.setSource (source);
        return wl;
    }
    
    /** Creates a weak implementation of VetoableChangeListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a VetoableChangeListener delegating to <CODE>l</CODE>.
     */
    public static VetoableChangeListener vetoableChange (VetoableChangeListener l, Object source) {
        WeakListener.VetoableChange wl = new WeakListener.VetoableChange (l);
        wl.setSource (source);
        return wl;
    }
    
    /** Creates a weak implementation of FileChangeListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a FileChangeListener delegating to <CODE>l</CODE>.
     */
    public static FileChangeListener fileChange (FileChangeListener l, Object source) {
        WeakListener.FileChange wl = new WeakListener.FileChange (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of FileStatusListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a FileStatusListener delegating to <CODE>l</CODE>.
     */
    public static FileStatusListener fileStatus (FileStatusListener l, Object source) {
        WeakListener.FileStatus wl = new WeakListener.FileStatus (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of RepositoryListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a RepositoryListener delegating to <CODE>l</CODE>.
     */
    public static RepositoryListener repository (RepositoryListener l, Object source) {
        WeakListener.Repository wl = new WeakListener.Repository (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of DocumentListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a DocumentListener delegating to <CODE>l</CODE>.
     */
    public static DocumentListener document (DocumentListener l, Object source) {
        WeakListener.Document wl = new WeakListener.Document (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of ChangeListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a ChangeListener delegating to <CODE>l</CODE>.
     */
    public static ChangeListener change (ChangeListener l, Object source) {
        WeakListener.Change wl = new WeakListener.Change (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of FocusListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a FocusListener delegating to <CODE>l</CODE>.
     */
    public static FocusListener focus (FocusListener l, Object source) {
        WeakListener.Focus wl = new WeakListener.Focus (l);
        wl.setSource (source);
        return wl;
    }

    /** Creates a weak implementation of OperationListener.
     *
     * @param l the listener to delegate to
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return a OperationListener delegating to <CODE>l</CODE>.
     */
    public static OperationListener operation (OperationListener l, Object source) {
        WeakListener.Operation wl = new WeakListener.Operation (l);
        wl.setSource (source);
        return wl;
    }

    /** A generic WeakListener factory.
     * Creates a weak implementation of a listener of type <CODE>lType</CODE>.
     *
     * @param lType the type of listener to create. It can be any interface,
     *     but only interfaces are allowed.
     * @param l the listener to delegate to, <CODE>l</CODE> must be an instance
     *     of <CODE>lType</CODE>
     * @param source the source that the listener should detach from when
     *     listener <CODE>l</CODE> is freed, can be <CODE>null</CODE>
     * @return an instance of <CODE>lType</CODE> delegating all the interface
     * calls to <CODE>l</CODE>.
     */
    public static EventListener create (Class lType, EventListener l, Object source) {
        ProxyListener pl = new ProxyListener (lType, l);
        pl.setSource (source);
        return (EventListener)pl.proxy;
    }

    /** Weak property change listener
    * @deprecated use appropriate method instead
    */
    public static class PropertyChange extends WeakListener
        implements PropertyChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public PropertyChange (PropertyChangeListener l) {
            super (PropertyChangeListener.class, l);
        }

        /** Constructor.
        * @param clazz required class
        * @param l listener to delegate to
        */
        PropertyChange (Class clazz, PropertyChangeListener l) {
            super (clazz, l);
        }

        /** Tests if the object we reference to still exists and
        * if so, delegate to it. Otherwise remove from the source
        * if it has removePropertyChangeListener method.
        */
        public void propertyChange (PropertyChangeEvent ev) {
            PropertyChangeListener l = (PropertyChangeListener)super.get (ev);
            if (l != null) l.propertyChange (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removePropertyChangeListener"; // NOI18N
        }
    }

    /** Weak vetoable change listener
    * @deprecated use appropriate method instead
    */
    public static class VetoableChange extends WeakListener
        implements VetoableChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public VetoableChange (VetoableChangeListener l) {
            super (VetoableChangeListener.class, l);
        }

        /** Tests if the object we reference to still exists and
        * if so, delegate to it. Otherwise remove from the source
        * if it has removePropertyChangeListener method.
        */
        public void vetoableChange (PropertyChangeEvent ev) throws PropertyVetoException {
            VetoableChangeListener l = (VetoableChangeListener)super.get (ev);
            if (l != null) l.vetoableChange (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeVetoableChangeListener"; // NOI18N
        }
    }

    /** Weak file change listener.
    * @deprecated use appropriate method instead
    */
    public static class FileChange extends WeakListener
        implements FileChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public FileChange (FileChangeListener l) {
            super (FileChangeListener.class, l);
        }

        /** Fired when a new folder has been created. This action can only be
        * listened in folders containing the created file up to the root of
        * file system.
        *
        * @param ev the event describing context where action has taken place
        */
        public void fileFolderCreated (FileEvent ev) {
            FileChangeListener l = (FileChangeListener)super.get (ev);
            if (l != null) l.fileFolderCreated (ev);
        }

        /** Fired when a new file has been created. This action can only be
        * listened in folders containing the created file up to the root of
        * file system.
        *
        * @param ev the event describing context where action has taken place
        */
        public void fileDataCreated (FileEvent ev) {
            FileChangeListener l = (FileChangeListener)super.get (ev);
            if (l != null) l.fileDataCreated (ev);
        }

        /** Fired when a file has been changed.
        * @param ev the event describing context where action has taken place
        */
        public void fileChanged (FileEvent ev) {
            FileChangeListener l = (FileChangeListener)super.get (ev);
            if (l != null) l.fileChanged (ev);
        }

        /** Fired when a file has been deleted.
        * @param ev the event describing context where action has taken place
        */
        public void fileDeleted (FileEvent ev) {
            FileChangeListener l = (FileChangeListener)super.get (ev);
            if (l != null) l.fileDeleted (ev);
        }

        /** Fired when a file has been renamed.
        * @param ev the event describing context where action has taken place
        *           and the original name and extension.
        */
        public void fileRenamed (FileRenameEvent ev) {
            FileChangeListener l = (FileChangeListener)super.get (ev);
            if (l != null) l.fileRenamed (ev);
        }

        /** Fired when a file attribute has been changed.
        * @param ev the event describing context where action has taken place,
        *           the name of attribute and old and new value.
        */
        public void fileAttributeChanged (FileAttributeEvent ev) {
            FileChangeListener l = (FileChangeListener)super.get (ev);
            if (l != null) l.fileAttributeChanged (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeFileChangeListener"; // NOI18N
        }
    }

    /** Weak file status listener.
    * @deprecated use appropriate method instead
    */
    public static class FileStatus extends WeakListener
        implements FileStatusListener {
        /** Constructor.
        */
        public FileStatus (FileStatusListener l) {
            super (FileStatusListener.class, l);
        }

        /** Notifies listener about change in annotataion of a few files.
         * @param ev event describing the change
         */
        public void annotationChanged(FileStatusEvent ev) {
            FileStatusListener l = (FileStatusListener)super.get (ev);
            if (l != null) l.annotationChanged (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeFileStatusListener"; // NOI18N
        }

    }

    /** Weak file system pool listener.
    * @deprecated use appropriate method instead
    */
    public static class Repository extends WeakListener
        implements RepositoryListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Repository (RepositoryListener l) {
            super (RepositoryListener.class, l);
        }


        /** Called when new file system is added to the pool.
        * @param ev event describing the action
        */
        public void fileSystemAdded (RepositoryEvent ev) {
            RepositoryListener l = (RepositoryListener)super.get (ev);
            if (l != null) l.fileSystemAdded (ev);
        }

        /** Called when a file system is deleted from the pool.
        * @param ev event describing the action
        */
        public void fileSystemRemoved (RepositoryEvent ev) {
            RepositoryListener l = (RepositoryListener)super.get (ev);
            if (l != null) l.fileSystemRemoved (ev);
        }

        /** Called when a Repository is reordered. */
        public void fileSystemPoolReordered(RepositoryReorderedEvent ev) {
            RepositoryListener l = (RepositoryListener)super.get (ev);
            if (l != null) l.fileSystemPoolReordered (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeRepositoryListener"; // NOI18N
        }

    }

    /** Weak document modifications listener.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    public static final class Document extends WeakListener
        implements DocumentListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Document (final DocumentListener l) {
            super (DocumentListener.class, l);
        }

        /** Gives notification that an attribute or set of attributes changed.
        * @param ev event describing the action
        */
        public void changedUpdate(DocumentEvent ev) {
            final DocumentListener l = docGet(ev);
            if (l != null) l.changedUpdate(ev);
        }

        /** Gives notification that there was an insert into the document.
        * @param ev event describing the action
        */
        public void insertUpdate(DocumentEvent ev) {
            final DocumentListener l = docGet(ev);
            if (l != null) l.insertUpdate(ev);
        }

        /** Gives notification that a portion of the document has been removed.
        * @param ev event describing the action
        */
        public void removeUpdate(DocumentEvent ev) {
            final DocumentListener l = docGet(ev);
            if (l != null) l.removeUpdate(ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeDocumentListener"; // NOI18N
        }

        /** Getter for the target listener.
        * @param event the event the we want to distribute
        * @return null if there is no listener because it has been finalized
        */
        private DocumentListener docGet (DocumentEvent ev) {
            DocumentListener l = (DocumentListener)super.ref.get ();
            if (l == null) super.removeListener (ev.getDocument());
            return l;
        }
    } // end of Document inner class

    /** Weak swing change listener.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    public static final class Change extends WeakListener
        implements ChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Change (ChangeListener l) {
            super (ChangeListener.class, l);
        }

        /** Called when new file system is added to the pool.
        * @param ev event describing the action
        */
        public void stateChanged (final ChangeEvent ev) {
            ChangeListener l = (ChangeListener)super.get(ev);
            if (l != null) l.stateChanged (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeChangeListener"; // NOI18N
        }

    }

    /** Weak version of listener for changes in one node.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    public static final class Node extends WeakListener.PropertyChange
        implements NodeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Node (NodeListener l) {
            super (NodeListener.class, l);
        }

        /** Delegates to the original listener.
        */
        public void childrenAdded (NodeMemberEvent ev) {
            NodeListener l = (NodeListener)super.get (ev);
            if (l != null) l.childrenAdded (ev);
        }

        /** Delegates to the original listener.
        */
        public void childrenRemoved (NodeMemberEvent ev) {
            NodeListener l = (NodeListener)super.get (ev);
            if (l != null) l.childrenRemoved (ev);
        }

        /** Delegates to the original listener.
        */
        public void childrenReordered (NodeReorderEvent ev) {
            NodeListener l = (NodeListener)super.get (ev);
            if (l != null) l.childrenReordered (ev);
        }

        /** Delegates to the original listener.
        */
        public void nodeDestroyed (NodeEvent ev) {
            NodeListener l = (NodeListener)super.get (ev);
            if (l != null) l.nodeDestroyed (ev);
        }


        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeNodeListener"; // NOI18N
        }

    }



    /** Weak version of focus listener.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    public static final class Focus extends WeakListener
        implements FocusListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Focus (FocusListener l) {
            super (FocusListener.class, l);
        }

        /** Delegates to the original listener.
        */
        public void focusGained(FocusEvent ev) {
            FocusListener l = (FocusListener)super.get (ev);
            if (l != null) l.focusGained (ev);
        }

        /** Delegates to the original listener.
        */
        public void focusLost(FocusEvent ev) {
            FocusListener l = (FocusListener)super.get (ev);
            if (l != null) l.focusLost (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeFocusListener"; // NOI18N
        }

    }

    /** Weak property change listener
    */
    final static class Operation extends WeakListener
        implements OperationListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Operation (OperationListener l) {
            super (OperationListener.class, l);
        }


        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeOperationListener"; // NOI18N
        }

        /** Object has been recognized by
         * {@link DataLoaderPool#findDataObject}.
         * This allows listeners
         * to attach additional cookies, etc.
         *
         * @param ev event describing the action
         */
        public void operationPostCreate(OperationEvent ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationPostCreate (ev);
        }
        /** Object has been successfully copied.
         * @param ev event describing the action
         */
        public void operationCopy(OperationEvent.Copy ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationCopy (ev);
        }
        /** Object has been successfully moved.
         * @param ev event describing the action
         */
        public void operationMove(OperationEvent.Move ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationMove (ev);
        }
        /** Object has been successfully deleted.
         * @param ev event describing the action
         */
        public void operationDelete(OperationEvent ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationDelete (ev);
        }
        /** Object has been successfully renamed.
         * @param ev event describing the action
         */
        public void operationRename(OperationEvent.Rename ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationRename (ev);
        }

        /** A shadow of a data object has been created.
         * @param ev event describing the action
         */
        public void operationCreateShadow (OperationEvent.Copy ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationCreateShadow (ev);
        }
        /** New instance of an object has been created.
         * @param ev event describing the action
         */
        public void operationCreateFromTemplate(OperationEvent.Copy ev) {
            OperationListener l = (OperationListener)super.get (ev);
            if (l != null) l.operationCreateFromTemplate (ev);
        }
    }

    
        /** Proxy interface that delegates to listeners.
    */
    private static class ProxyListener extends WeakListener implements InvocationHandler {
        /** proxy generated for this listener */
        public final Object proxy;
        
        /** Equals method */
        private static Method equalsMth;
        
        /** */
        private static Method getEquals() {
            if (equalsMth == null) {
                try {
                    equalsMth = Object.class.getMethod("equals", new Class[] { Object.class }); // NOI18N
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            return equalsMth;
        }

        /** @param listener listener to delegate to
        */
        public ProxyListener (Class c, java.util.EventListener listener) {
            super (c, listener);

            proxy = Proxy.newProxyInstance (
                        c.getClassLoader (), new Class[] { c }, this
                    );
        }

        public java.lang.Object invoke (
            Object proxy, Method method, Object[] args
        ) throws Throwable {
            if (method.getDeclaringClass () == Object.class) {
                // a method from object => call it on your self
                if (method == getEquals()) {
                    boolean ret = equals(args[0]);
                    return (ret ? Boolean.TRUE : Boolean.FALSE);
                }
                
                return method.invoke (this, args);
            }

            // listeners method
            EventObject ev = 
                args[0] instanceof EventObject ? (EventObject)args[0] : null;

            Object listener = super.get (ev);
            if (listener != null) {
                return method.invoke (listener, args);
            } else {
                return null;
            }
        }

        /** Remove method name is composed from the name of the listener.
        */
        protected String removeMethodName () {
            String name = listenerClass.getName ();

            // strip package name
            int dot = name.lastIndexOf('.');
            name = name.substring (dot + 1);

            // in case of inner interfaces/classes we also strip the outer
            // class' name
            int i = name.lastIndexOf('$'); // NOI18N
            if (i >= 0) {
                name = name.substring(i + 1);
            }

            return "remove".concat(name); // NOI18N
        }

        /** To string prints class.
        */
        public String toString () {
            return super.toString () + "[" + listenerClass + "]"; // NOI18N
        }

        /** Equal is extended to equal also with proxy object.
        */
        public boolean equals (Object obj) {
            return proxy == obj || this == obj;
        }
        
        Object getImplementator() {
            return proxy;
        }
    }
    
    /** Reference that also holds ref to WeakListener.
    */
    private static final class ListenerReference extends WeakReference 
    implements Runnable {
        private static Class lastClass;
        private static String lastMethodName;
        private static Method lastRemove;
        private static Object LOCK = new Object ();
            
            
        final WeakListener weakListener;

        public ListenerReference (
            Object ref,
            WeakListener weakListener
        ) {
            super (ref, Utilities.activeReferenceQueue());
            this.weakListener = weakListener;
        }
        
        public void run () {
            ListenerReference lr = this;

            // prepare array for passing arguments to getMethod/invoke
            Object[] params = new Object[1];
            Class[] types = new Class[1];
            Object src = null; // On whom we're listening
            Method remove = null;
            
            WeakListener ref = lr.weakListener;
            if (ref.source == null || (src = ref.source.get()) == null) return;

            Class methodClass = src.getClass();
            String methodName = ref.removeMethodName();
            

            synchronized (LOCK) {
                if (lastClass == methodClass && lastMethodName == methodName && lastRemove != null) {
                    remove = lastRemove;
                }
            }

            // get the remove method or use the last one
            if (remove == null) {
                types[0] = ref.listenerClass;
                remove = null;

                try {
                    remove = methodClass.getMethod(methodName, types);
                } catch (NoSuchMethodException e) {
                    for (;;) {
                        methodClass = methodClass.getSuperclass ();
                        if (methodClass == null) break;
                        
                        try {
                            remove = methodClass.getDeclaredMethod(methodName, types);
                            break;
                        } catch (NoSuchMethodException ex) {
                        }
                    } 
                }

                
                if (remove == null) {
                    ErrorManager.getDefault().log (ErrorManager.WARNING,
                        "Can't remove " + ref.listenerClass.getName() + "from " + src); // NOI18N
                    return;
                } else {
                    
                    if (
                        !Modifier.isPublic(remove.getModifiers()) || 
                        !Modifier.isPublic(remove.getDeclaringClass().getModifiers())
                    ) {
                        remove.setAccessible(true);
                    }
                    
                    synchronized (LOCK) {
                        lastClass = methodClass;
                        lastMethodName = methodName;
                        lastRemove = remove;
                    }
                }                    

            } else { // already resolved
                if (remove == null) return; // there was no such method
            }

            params[0] = ref.getImplementator(); // Whom to unregister
            try {
                remove.invoke (src, params);
            } catch (Exception ex) { // from invoke(), should not happen
                ErrorManager.getDefault().annotate(ex, "Problem encountered while calling " + methodClass + "." + methodName + "(...) on " + src); // NOI18N
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
        }
        
    }
}
