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

package org.openide;

import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.LookupEvent;
import org.openide.util.WeakSet;

/** A system of managing, annotating, and classifying errors and log messages.
 * Rather than printing raw exceptions to the console, or popping up dialog boxes
 * with exceptions, or implementing custom debug or logging facililities, code may
 * use the error manager to access logging and error-reporting in a higher-level
 * fashion. Standard error manager implementations can then provide generic ways
 * of customizing logging levels for different components, and so on.
 * <p>Especially important is the attaching of annotations such as stack traces to
 * exceptions, permitting you to throw an exception of a type permitted by your
 * API signature while safely encapsulating the root cause of the problem (in terms
 * of other nested exceptions). Code should use {@link #notify(Throwable)} rather
 * than directly printing caught exceptions, to make sure nested annotations are not lost.
 * <p>Also localized messages may be annotated to exceptions so that code which can deal
 * with a caught exception with a user-visible UI can display a polite and helpful message.
 * Messages with no localized annotation can be handled in a default way while the details
 * are reserved for the log file.
 * <p>A simple example of usage to keep nested stacktraces:
 * <pre>
 * public void doSomething () throws IOException {
 *     try {
 *         doSomethingElse ();
 *     } catch (IllegalArgumentException iae) {
 *         IOException ioe = new IOException ("did not work");
 *         ErrorManager.getDefault ().annotate (ioe, iae);
 *         throw ioe;
 *     }
 * }
 * // ...
 * try {
 *     foo.doSomething ();
 * } catch (IOException ioe) {
 *     ErrorManager.getDefault ().notify (ioe);
 * }
 * </pre>
 * @author Jaroslav Tulach
 * @see <a href="http://openide.netbeans.org/proposals/ErrorManagerUsage.html">Philosophy and usage scenarios</a>
 */
public abstract class ErrorManager extends Object {
    // XXX note that these levels accidentally used hex rather than binary,
    // so it goes 0, 1, 16, 256, ....
    // Unfortunately too late to change now: public int constants are part of the
    // API - documented, inlined into compiled code, etc.
    
    /**
     * Undefined severity.
     * May be used only in {@link #notify(int, Throwable)}.
     */
    public static final int UNKNOWN = 0x00000000;
    /** Message that would be useful for tracing events but which need not be a problem. */
    public static final int INFORMATIONAL = 0x00000001;
    /** Something went wrong in the software, but it is continuing and the user need not be bothered. */
    public static final int WARNING = 0x00000010;
    /** Something the user should be aware of. */
    public static final int USER = 0x00000100;
    /** Something went wrong, though it can be recovered. */
    public static final int EXCEPTION = 0x00001000;
    /** Serious problem, application may be crippled. */
    public static final int ERROR = 0x00010000;
    
    /** We keep a reference to our proxy ErrorManager here. */
    private static DelegatingErrorManager current;
    
    /** Getter for the default version of error manager.
     * @return the error manager installed in the system
     * @since 2.1
     */
    public static ErrorManager getDefault () {
        synchronized (ErrorManager.class) {
            if (current != null) {
                return current;
            }
        }
        return getDefaultDelegate();
    }

    private static DelegatingErrorManager getDefaultDelegate() {
        DelegatingErrorManager c = new DelegatingErrorManager(""); // NOI18N
        try {
            c.initialize();
            synchronized (ErrorManager.class) {
                if (current == null) {
                    current = c;
                    // r is not null after c.initialize();
                    current.r.addLookupListener(current);
                } 
            }
        } catch (RuntimeException e) {
            // #20467
            e.printStackTrace();
            current = c;
        } catch (LinkageError e) {
            // #20467
            e.printStackTrace();
            current = c;
        }
        return current;
    }
    
    /** Associates annotations with an exception.
    *
    * @param t the exception
    * @param arr array of annotations (or <code>null</code>)
    * @return the same exception <code>t</code> (as a convenience)
    */
    public abstract Throwable attachAnnotations (Throwable t, Annotation[] arr);

    /** Finds annotations associated with a given exception.
    * @param t the exception
    * @return array of annotations or <code>null</code>
    */
    public abstract Annotation[] findAnnotations (Throwable t);

    /** Annotates given exception with given values. All the
    * previous annotations are kept and this new one is added at 
    * the top of the annotation stack (index 0 of the annotation
    * array).
    *
    * @param t the exception
    * @param severity integer describing severity, e.g. {@link #EXCEPTION}
    * @param message message to attach to the exception or <code>null</code>
    * @param localizedMessage localized message for the user or <code>null</code>
    * @param stackTrace exception representing the stack trace or <code>null</code>
    * @param date date or <code>null</code>
    * @return the same exception <code>t</code> (as a convenience)
    */
    public abstract Throwable annotate (
        Throwable t, int severity,
        String message, String localizedMessage,
        Throwable stackTrace, java.util.Date date
    );

    /** Prints the exception to the log file and (possibly) notifies the user.
     * Use of {@link #UNKNOWN} severity means that the error manager should automatically
     * select an appropriate severity level, for example based on the contents of
     * annotations in the throwable.
    * @param severity the severity to be applied to the exception (overrides default), e.g. {@link #EXCEPTION}
    * @param t the exception to notify
    */
    public abstract void notify (int severity, Throwable t);


    /** Prints the exception to the log file and (possibly) notifies the user.
     * Guesses at the severity.
    * @param t the exception to notify
     * @see #UNKNOWN
     * @see #notify(int, Throwable)
    */
    public final void notify (Throwable t) {
        notify(UNKNOWN, t);
    }

    /** Logs the message to a file and (possibly) tells the user.
    * @param severity the severity to be applied (overrides default)
    * @param s the log message
    */
    public abstract void log(int severity, String s);

    /** Logs the message to log file and (possibly) tells the user.
     * Uses a default severity.
    * @param s the log message
    */
    public final void log(String s) {
      //  log(INFORMATIONAL, s);
    }
    
    /** Test whether a messages with given severity will be logged in advance.
     * Can be used to avoid the construction of complicated and expensive
     * logging messages.
     * <p>The default implementation just returns true. Subclasses
     * should override to be more precise - <strong>treat this method as abstract</strong>.
     * @param severity the severity to check, e.g. {@link #EXCEPTION}
     * @return <code>false</code> if the next call to {@link #log(int,String)} with this severity will
     *    discard the message
     */
    public boolean isLoggable (int severity) {
        return true;
    }

    /**
     * Test whether a throwable, if {@link #notify(int, Throwable) notified} at the given
     * level, will actually be displayed in any way (even to a log file etc.).
     * If not, there is no point in constructing it.
     * <p>This method is distinct from {@link #isLoggable} because an error manager
     * implementation may choose to notify stack traces at a level where it would
     * not log messages. See issue #24056 for justification.
     * <p>The default implementation just calls {@link #isLoggable}. Subclasses
     * should override to be more precise - <strong>treat this method as abstract</strong>.
     * @param severity a notification severity
     * @return true if a throwable notified at this severity will be used; false if it will be ignored
     * @since 3.18
     */
    public boolean isNotifiable(int severity) {
        return isLoggable(severity);
    }

    /** Returns an instance with given name.
     * <p>By convention, you can name error managers the same as packages (or classes)
     * they are designed to report information from.
     * For example, <code>org.netbeans.modules.mymodule.ComplicatedParser</code>.
     * <p>The error manager implementation should provide some way of configuring e.g.
     * the logging level for error managers of different names. For example, in the basic
     * NetBeans core implementation, you can define a system property with the same name
     * as the future error manager (or a package prefix of it) whose value is the numeric
     * logging level (e.g. <samp>-J-Dorg.netbeans.modules.mymodule.ComplicatedParser=0</samp>
     * to log everything). Other implementations may have quite different ways of configuring
     * the error managers.
     * @param name the desired identifying name
     * @return a new error manager keyed off of that name
     */
    public abstract ErrorManager getInstance(String name);
    
    //
    // Helper methods
    //

    /** Annotates given exception with given values. All the
    * previous annotations are kept and this new is added at 
    * the top of the annotation stack (index 0 of the annotation
    * array).
    *
    * @param t the exception
    * @param localizedMessage localized message for the user or null
    * @return the same exception <code>t</code> (as a convenience)
    */
    public final Throwable annotate (
        Throwable t, String localizedMessage
    ) {
        return annotate (t, UNKNOWN, null, localizedMessage, null, null);
    }

    /** Annotates target exception with given exception. All the
    * previous annotations are kept and this new is added at 
    * the top of the annotation stack (index 0 of the annotation
    * array).
    *
    * @param target the exception to be annotated
    * @param t the exception that will be added
    * @return the same exception <code>target</code> (as a convenience)
    */    
    public final Throwable annotate (Throwable target, Throwable t) {
        return annotate (target, UNKNOWN, null, null, t, null);        
    }
    
    /** Takes annotations from one exception and associates
    * them with another one.
    *
    * @param t the exception to annotate
    * @param copyFrom exception to take annotations from
    * @return the same exception <code>t</code> (as a convenience)
    * @deprecated Now does the same thing as {@link #annotate(Throwable,Throwable)}
    *             except marks the annotation {@link #UNKNOWN} severity. Otherwise
    *             you used to have inadvertent data loss when <code>copyFrom</code>
    *             had annotations of its own: the subannotations were kept but the
    *             main stack trace in <code>copyFrom</code> was discarded. In practice
    *             you usually want to keep all of <code>copyFrom</code>; if for some
    *             reason you just want to keep annotations, please do so explicitly
    *             using {@link #findAnnotations} and {@link #attachAnnotations}.
    */
    public final Throwable copyAnnotation (Throwable t, Throwable copyFrom) {
        // Cf. #17874 for the change in behavior.
        /*
        Annotation[] arr = findAnnotations (copyFrom);

        if (arr != null) {
            return attachAnnotations (
                       t, arr
                   );
        } else {
        */
            return annotate (t, UNKNOWN, null, null, copyFrom, null);
        /*
        }
        */
    }


    /**
     * Implementation of ErrorManager that delegates to the ones found by
     * lookup.
     */
    private static class DelegatingErrorManager extends ErrorManager 
        implements LookupListener {

        private String name = null;
        
        /**
         * The set of instances we delegate to. Elements type is ErrorManager.
         */
        private Set delegates = new HashSet();
        
        /**
         * A set that has to be updated when the list of delegates
         * changes. All instances created by getInstance are held here.
         * It is a set of DelagatingErrorManager.
         */
        private WeakSet createdByMe = new WeakSet(); 
        
        /** If we are the "central" delagate this is not null and
         * we listen on the result. On newly created delegates this
         * is null.
         */
        Lookup.Result r;
        
        public DelegatingErrorManager(String name) {
            this.name = name;
        }

        /** If the name is not empty creates new instance of
         * DelegatingErrorManager. Adds it to createdByMe.
         */
        public ErrorManager getInstance(String name) { 
            if ((name == null) || ("".equals(name))) { // NOI18N
                return this;
            }
            DelegatingErrorManager dem = new DelegatingErrorManager(name);
            synchronized (this) {
                attachNewDelegates(dem, name);
                createdByMe.add(dem);
            }
            return dem;
        }
        
        /** Calls all delegates. */
        public Throwable attachAnnotations (Throwable t, Annotation[] arr) {
            for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                ErrorManager em = (ErrorManager)i.next();
                em.attachAnnotations(t, arr);
            }
            return t;
        }
        
        /** Calls all delegates. */
        public Annotation[] findAnnotations (Throwable t) {
            for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                ErrorManager em = (ErrorManager)i.next();
                Annotation[] res = em.findAnnotations(t);
                if ((res != null) && (res.length > 0)) {
                    return res;
                }
            }
            return new Annotation[0];
        }

        /** Calls all delegates. */
        public Throwable annotate (
            Throwable t, int severity,
            String message, String localizedMessage,
            Throwable stackTrace, java.util.Date date) {
                
            for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                ErrorManager em = (ErrorManager)i.next();
                em.annotate(t, severity, message, localizedMessage, stackTrace, date);
            }
            return t;

        }
        
        /** Calls all delegates. */
        public void notify (int severity, Throwable t) {
            if (delegates.isEmpty()) {
                t.printStackTrace();
            }
            try {
                for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                    ErrorManager em = (ErrorManager)i.next();
                    em.notify(severity, t);
                }
            } catch (RuntimeException e) {
                // #20467
                e.printStackTrace();
                t.printStackTrace();
            } catch (LinkageError e) {
                // #20467
                e.printStackTrace();
                t.printStackTrace();
            }
        }
        
        /** Calls all delegates. */
        public void log(int severity, String s) {
            if (delegates.isEmpty()) {
                System.err.println ("Log: " + severity + " msg: " + s); // NOI18N
            }
            for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                ErrorManager em = (ErrorManager)i.next();
                em.log(severity, s);
            }
        }
        
        /** Calls all delegates. */
        public boolean isLoggable (int severity) {
            if (delegates.isEmpty()) {
                return true;
            }
            for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                ErrorManager em = (ErrorManager)i.next();
                if (em.isLoggable(severity)) {
                    return true;
                }
            }
            return false;
        }

        /** Calls all delegates. */
        public boolean isNotifiable(int severity) {
            if (delegates.isEmpty()) {
                return true;
            }
            for (Iterator i = delegates.iterator(); i.hasNext(); ) {
                ErrorManager em = (ErrorManager)i.next();
                if (em.isNotifiable(severity)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Updates the list of delegates. Also updates all instances created
         * by ourselves.
         */
        public synchronized void setDelegates(Collection newDelegates) {
            HashSet d = new HashSet(newDelegates);
            for (Iterator i = createdByMe.iterator(); i.hasNext(); ) {
                DelegatingErrorManager dem = (DelegatingErrorManager)i.next();
                attachNewDelegates(dem, dem.getName());
            }
            delegates = d;
        }
        
        private String getName() {
            return name;
        }
        
        /**
         * Takes all our delegates, asks them for an instance identified by
         * name and adds those results as new delegates for dem.
         * @param String name
         * @param DelagatingErrorManager d the instance to which we will attach
         */
        private void attachNewDelegates(DelegatingErrorManager dem, String name) {
            Set newDelegatesForDem = new HashSet();
            for (Iterator j = delegates.iterator(); j.hasNext(); ) {
                ErrorManager e = (ErrorManager)j.next();
                newDelegatesForDem.add(e.getInstance(name));
            }
            dem.setDelegates(newDelegatesForDem);
        }

        /** Blocks on lookup and after the lookup returns updates
         * delegates and adds a listener.
         */
        public void initialize() {
            r = Lookup.getDefault().lookup(
                new Lookup.Template(ErrorManager.class));
            Collection instances = r.allInstances();
            setDelegates(instances);
        }
        
        /** Updates the delegates.*/
        public void resultChanged (LookupEvent ev) {
            if (r != null) {
                Collection instances = r.allInstances();
                setDelegates(instances);
            }
        }
    }

    /** Annotation that can be attached to an error.
    */
    public static interface Annotation {
        /** Non-localized message.
        * @return associated message or <code>null</code>
        */
        public abstract String getMessage ();

        /** Localized message.
        * @return message to be presented to the user or <code>null</code>
        */
        public abstract String getLocalizedMessage ();

        /** Stack trace. The stack trace should locate the method
        * and position in the method where the error occurred.
        *
        * @return exception representing the location of the error or <code>null</code>
        */
        public abstract Throwable getStackTrace ();

        /** Time at which the exception occurred.
        * @return the time or <code>null</code>
        */
        public abstract java.util.Date getDate ();

        /** Severity of the exception.
         * {@link #UNKNOWN} serves as the default.
        * @return number representing the severity, e.g. {@link ErrorManager#EXCEPTION}
        */
        public abstract int getSeverity ();
    } // end of Annotation
}
