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

package org.openide.util;

import java.util.HashSet;
import java.util.Iterator;

/** A task that may be executed in a separate thread and permits examination of its status.
* Other threads can check if it is finished or wait for it
* to finish.
* <P>
* For example:
* <p><code><PRE>
* Runnable r = new Runnable () {
*   public void run () {
*     // do something
*   }
* };
* Task task = new Task (r);
* RequestProcessor.postRequest (task);
* </PRE></code>
* <p>In a different thread one can then test <CODE>task.isFinished ()</CODE>
* or wait for it with <CODE>task.waitFinished ()</CODE>.
*
* @author Jaroslav Tulach
*/
public class Task extends Object implements Runnable {
    /** Dummy task which is already finished. */
    public static final Task EMPTY = new Task();
    static {
        EMPTY.finished = true;
    }

    /** what to run */
    private Runnable run;
    /** flag if we have finished */
    private boolean finished;
    /** listeners for the finish of task (TaskListener) */
    private HashSet list;

    /** Create a new task.
    * The runnable should provide its own error-handling, as
    * by default thrown exceptions are simply logged and not rethrown.
    * @param run runnable to run that computes the task
    */
    public Task(Runnable run) {
        this.run = run;
        if (run == null) {
            finished = true;
        }
    }

    /** Constructor for subclasses that wants to control whole execution
    * itself.
    * @since 1.5
    */
    protected Task () {
    }

    /** Test whether the task has finished running.
    * @return <code>true</code> if so
    */
    public final boolean isFinished () {
        return finished;
    }

    /** Wait until the task is finished. 
    * Changed not to be <code>final</code> in version 1.5
    */
    public void waitFinished () {
        synchronized (this) {
            while (!finished) {
                try {
                    wait ();
                } catch (InterruptedException ex) {
        	}
    	    }
        }
    }

    /** Changes the state of the task to be running. Any call after this
    * one and before notifyFinished to waitFinished blocks.
    * @since 1.5
    */
    protected final void notifyRunning () {
        synchronized (this) {
            this.finished = false;
            notifyAll ();
        }
    }
    
    /** Notify all waiters that this task has finished.
    * @see #run
    */
    protected final void notifyFinished () {
        Iterator it;

        synchronized (this) {
            finished = true;
            notifyAll ();

            // fire the listeners
            if (list == null) return;

            it = ((HashSet)list.clone ()).iterator ();
        }

        while (it.hasNext ()) {
            TaskListener l = (TaskListener)it.next ();
            l.taskFinished (this);
        }
    }

    /** Start the task.
    * When it finishes (even with an exception) it calls
    * {@link #notifyFinished}.
    * Subclasses may override this method, but they
    * then need to call {@link #notifyFinished} explicitly.
    * <p>Note that this call runs synchronously, but typically the creator
    * of the task will call this method in a separate thread.
    */
    public void run () {
        try {
            notifyRunning ();
            if (run != null) run.run ();
        } finally {
            notifyFinished ();
        }
    }

    /** Add a listener to the task.
    * @param l the listener to add
    */
    public synchronized void addTaskListener (TaskListener l) {
        if (list == null) list = new HashSet ();
        list.add (l);
        if (finished) {
            l.taskFinished(this);
        }
    }

    /** Remove a listener from the task.
    * @param l the listener to remove
    */
    public synchronized void removeTaskListener (TaskListener l) {
        if (list == null) return;
        list.remove (l);
    }

    public String toString () {
        return "task " + run; // NOI18N
    }
}
