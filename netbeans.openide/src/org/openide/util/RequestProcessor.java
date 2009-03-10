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

import java.lang.ref.*;
import java.util.*;

import org.openide.ErrorManager;

/** Request processor that is capable to execute requests in dedicated threads.
 * You can create your own instance or use the shared one.
 *
 * <P><A name="use_cases">There are several use cases for RequestProcessor:</A>
 * 
 * <UL><LI>Having something done asynchronously in some other thread,
 *  not insisting on any kind of serialization of the requests:
 *  Use <CODE>RequestProcessor.{@link RequestProcessor#getDefault
 *  }.{@link #post(java.lang.Runnable) post(runnable)}</CODE>
 *  for this purpose.
 * <LI>Having something done later in some other thread:
 *  Use <CODE>RequestProcessor.{@link RequestProcessor#getDefault
 *  }.{@link #post(java.lang.Runnable,int) post(runnable,&nbsp;delay)}</CODE>
 * <LI>Having something done periodically in any thread: Use the
 *  {@link RequestProcessor.Task}'s ability to
 *  {@link RequestProcessor.Task#schedule schedule()}, like
 *  <PRE>
 *      static RequestProcessor.Task CLEANER = RequestProcessor.getDefault().post(runnable,DELAY);
 *      public void run() {
 *          doTheWork();
 *          CLEANER.schedule(DELAY);
 *      }
 *  </PRE>
 *  <STRONG>Note:</STRONG> Please think twice before implementing some periodic
 *  background activity. It is generally considered evil if it will run
    regardless of user actions and the IDE state, even while the application
    is minimized / not currently used.
 * <LI>Having something done in some other thread but properly ordered:
 *  Create a private instance of the 
 *  {@link RequestProcessor#RequestProcessor(java.lang.String) RequestProcessor(name)}</CODE>
 *  and use it from all places you'd like to have serialized. It works
 *  like a simple Mutex.
 * <LI>Having some entity that will do processing in a limited
 *  number of threads paralelly: Create a private instance of the
 *  {@link RequestProcessor#RequestProcessor(java.lang.String,int) RequestProcessor(name,throughput)}</CODE>
 *  set proper throughput and use it to schedule the work.
 *  It works like a queue of requests passing through a semafore with predefined
 *  number of <CODE>DOWN()</CODE>s.
 * </UL>
 *
 * <STRONG>Note:</STRONG> If you don't need to serialize your requests but
 * you're generating them in bursts, you should use your private
 * <CODE>RequestProcessor</CODE> instance with limited throughput (probably
 * set to 1), the IDE would try to run all your requests in parallel otherwise.
 *
 * @author Petr Nejedly, Jaroslav Tulach
 */
public final class RequestProcessor {
    /** the static instance for users that do not want to have own processor */
    private static RequestProcessor DEFAULT = new RequestProcessor ();

    // 50: a conservative value, just for case of misuse
    /** the static instance for users that do not want to have own processor */
    private static RequestProcessor UNLIMITED = new RequestProcessor ("Default RequestProcessor", 50); // NOI18N

    /** A shared timer used to pass timeouted tasks to pending queue */
    private static Timer starterThread = new Timer(true);
    
    /** logger */
    private static ErrorManager logger;
    
    /** The name of the RequestProcessor instance */
    String name;
    
    /** The counter for automatic naming of unnamed RequestProcessors */
    private static int counter = 0;

    /** If the RP was stopped, this variable will be set, every new post()
     * will throw an exception and no task will be processed any further */
    boolean stopped = false;
    
    /** The lock covering following four fields. They should be accessed
     * only while having this lock held. */
    private Object processorLock = new Object();
    
    /** The set holding all the Processors assigned to this RequestProcessor */
    private HashSet processors = new HashSet();

    /** Actualy the first item is pending to be processed.
     * Can be accessed/trusted only under the above processorLock lock.
     * If null, nothing is scheduled and the processor is not running. */
    private List queue = new LinkedList();

    /** Number of currently running processors. If there is a new request
     * and this number is lower that the throughput, new Processor is asked
     * to carry over the request. */
    private int running = 0;

    /** The maximal number of processors that can perform the requests sent
     * to this RequestProcessors. If 1, all the requests are serialized. */
    private int throughput;
    
    /** Creates new RequestProcessor with automatically assigned unique name. */
    public RequestProcessor() {
        this(null,1);
    }

    /** Creates a new named RequestProcessor with throughput 1.
     * @param name the name to use for the request processor thread */
    public RequestProcessor(String name) {
        this(name,1);
    }

    /** Creates a new named RequestProcessor with defined throughput.
     * @param name the name to use for the request processor thread
     * @param throughput the maximal count of requests allowed to run in parallel
     *
     * @since OpenAPI version 2.12
     */
    public RequestProcessor(String name, int throughput) {
        this.throughput = throughput;
        this.name = (name != null) ? name : "OpenIDE-request-processor-" + (counter++);
    }

    
    /** The getter for the shared instance of the <CODE>RequestProcessor</CODE>.
     *
     * @return an instance of RequestProcessor that is capable of performing
     * "unlimited" (currently limited to 50, just for case of misuse) number
     * of requests in parallel. This instance is shared by anybody who
     * needs a way of performing sporadic or repeated asynchronous work.
     *
     * @since OpenAPI version 2.12
     */ 
    public static RequestProcessor getDefault() {
        return UNLIMITED;
    }
    
    /** This methods asks the request processor to start given
     * runnable immediately. The default priority is {@link Thread#MIN_PRIORITY}.
     *
     * @param run class to run
     * @return the task to control the request
     */
    public Task post (Runnable run) {
        return post (run, 0, Thread.MIN_PRIORITY);
    }

    /** This methods asks the request processor to start given
    * runnable after <code>timeToWait</code> milliseconds. The default priority is {@link Thread#MIN_PRIORITY}.
    *
    * @param run class to run
    * @param timeToWait to wait before execution
    * @return the task to control the request
    */
    public Task post (final Runnable run, int timeToWait) {
        return post (run, timeToWait, Thread.MIN_PRIORITY);
    }

    /** This methods asks the request processor to start given
    * runnable after <code>timeToWait</code> milliseconds. Given priority is assigned to the
    * request. <p>
    * For request relaying please consider:
    * <pre>
    *    post(run, timeToWait, Thread.currentThread().getPriority());
    * </pre>
    *
    * @param run class to run
    * @param timeToWait to wait before execution
    * @param priority the priority from {@link Thread#MIN_PRIORITY} to {@link Thread#MAX_PRIORITY}
    * @return the task to control the request
    */
    public Task post (final Runnable run, int timeToWait, int priority) {
        RequestProcessor.Task task = new Task (run, priority);        
        task.schedule (timeToWait);
        return task;
    }


    /** Creates request that can be later started by setting its delay.
    * The request is not immediatelly put into the queue. It is planned after
    * setting its delay by schedule method.
    *
    * @param run action to run in the process
    * @return the task to control execution of given action
    */
    public Task create (Runnable run) {
        return new Task (run);
    }

    /** Tests if the current thread is request processor thread.
    * This method could be used to prevent the deadlocks using
    * <CODE>waitFinished</CODE> method. Any two tasks created
    * by request processor must not wait for themself.
    *
    * @return <CODE>true</CODE> if the current thread is request processor
    *          thread, otherwise <CODE>false</CODE>
    */
    public boolean isRequestProcessorThread () {
        Thread c = Thread.currentThread();
//        return c instanceof Processor && ((Processor)c).source == this;
        synchronized(processorLock) {
	    return processors.contains(c);
	}
    }

    /** Stops processing of runnables processor.
    * The currently running runnable is finished and no new is started.
    */
    public void stop () {
        if (this == UNLIMITED || this == DEFAULT) {
            throw new IllegalArgumentException("Can't stop shared RP's"); // NOI18N
        }
        synchronized (processorLock) {
            stopped = true;
            Iterator it = processors.iterator();
            while (it.hasNext()) ((Processor)it.next()).interrupt();
        }
    }

    //
    // Static methods communicating with default request processor
    //

    /** This methods asks the request processor to start given
     * runnable after <code>timeToWait</code> milliseconds. The default priority is {@link Thread#MIN_PRIORITY}.
     *
     * @param run class to run
     * @return the task to control the request
     *
     * @deprecated Sharing of one singlethreaded <CODE>RequestProcessor</CODE>
     * among different users and posting even blocing requests is inherently
     * deadlock-prone. See <A href="#use_cases">use cases</A>. */
    public static Task postRequest (Runnable run) {
        return DEFAULT.post (run);
    }

    /** This methods asks the request processor to start given
     * runnable after <code>timeToWait</code> milliseconds.
     * The default priority is {@link Thread#MIN_PRIORITY}.
     *
     * @param run class to run
     * @param timeToWait to wait before execution
     * @return the task to control the request
     *
     * @deprecated Sharing of one singlethreaded <CODE>RequestProcessor</CODE>
     * among different users and posting even blocing requests is inherently
     * deadlock-prone. See <A href="#use_cases">use cases</A>. */
    public static Task postRequest (final Runnable run, int timeToWait) {
        return DEFAULT.post (run, timeToWait);
    }

    /** This methods asks the request processor to start given
     * runnable after <code>timeToWait</code> milliseconds. Given priority is assigned to the
     * request.
     * @param run class to run
     * @param timeToWait to wait before execution
     * @param priority the priority from {@link Thread#MIN_PRIORITY} to {@link Thread#MAX_PRIORITY}
     * @return the task to control the request
     *
     * @deprecated Sharing of one singlethreaded <CODE>RequestProcessor</CODE>
     * among different users and posting even blocing requests is inherently
     * deadlock-prone. See <A href="#use_cases">use cases</A>. */
    public static Task postRequest (final Runnable run, int timeToWait, int priority) {
        return DEFAULT.post (run, timeToWait, priority);
    }

    /** Creates request that can be later started by setting its delay.
     * The request is not immediatelly put into the queue. It is planned after
     * setting its delay by setDelay method.
     * @param run action to run in the process
     * @return the task to control execution of given action
     *
     * @deprecated Sharing of one singlethreaded <CODE>RequestProcessor</CODE>
     * among different users and posting even blocing requests is inherently
     * deadlock-prone. See <A href="#use_cases">use cases</A>. */
    public static Task createRequest (Runnable run) {
        return DEFAULT.create (run);
    }
    
    
    /** Logger for the error manager.
     */
    private static ErrorManager logger () {
        synchronized (starterThread) {
            if (logger == null) {
                logger = ErrorManager.getDefault ().getInstance ("org.openide.util.RequestProcessor"); // NOI18N
            }
            return logger;
        }
    }

    // The task is the implementation of most of the RP semantics
    /** The task describing the request sent to the processor. */
    public final class Task extends org.openide.util.Task {
        private Item item;
        private int priority = Thread.MIN_PRIORITY;
        private long time = 0;
        private Thread lastThread = null;

        /** @param run runnable to start
        * @param delay amount of millis to wait
        * @param priority the priorty of the task
        */
        Task (Runnable run) {
            super(run);
        }

        Task (Runnable run, int priority){
            super(run);
            if (priority < Thread.MIN_PRIORITY) priority = Thread.MIN_PRIORITY;
            if (priority > Thread.MAX_PRIORITY) priority = Thread.MAX_PRIORITY;            
            this.priority = priority;
        }
        
        public void run() {
            lastThread = Thread.currentThread();
            super.run();
	    lastThread = null;
        }

        /** Getter for amount of millis till this task
        * is started.
        * @return amount of millis
        */
        public int getDelay () {
            long delay = time - System.currentTimeMillis ();
            if (delay < 0L) return 0;
            if (delay > (long)Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return (int)delay;
        } 

        /** (Re-)schedules a task to run in the future.
        * If the task has not been run yet, it is postponed to
        * the new time. If it has already run and finished, it is scheduled
        * to be started again. If it is currently running, it is nevertheless
        * left to finish, and also scheduled to run again.
        * @param delay time in milliseconds to wait (starting from now)
        */
        public void schedule (int delay) {
            if (stopped) throw new IllegalStateException("RequestProcessor already stopped!"); // NOI18N

            time = System.currentTimeMillis() + delay;
            final Item localItem;
            synchronized (processorLock) {
                notifyRunning();
                if (item != null) item.clear();
                item = new Item(this, RequestProcessor.this);
                localItem = item;
            }
            
            if (delay == 0) { // Place it to pending queue immediatelly
                enqueue(localItem);
            } else { // Post the starter
                starterThread.schedule( new TimerTask() {
                    public void run() { // enqueue the created
                        enqueue(localItem); // it may be already neutralized
                    }
                }, delay);
            }
        }
        
        
        
        /** Removes the task from the queue.
        *
        * @return true if the task has been removed from the queue,
        *   false it the task has already been processed
        */
        public boolean cancel () {
            synchronized (processorLock) {
                boolean success = (item == null) ? false : item.clear();
                if (success) notifyFinished(); // mark it as finished
                return success;
            }
        }

        /** Current priority of the task.
        */
        public int getPriority () {
            return priority;
        }
        
        /** Changes the priority the task will be performed with. */
        public void setPriority (int priority) {
            if (this.priority == priority) return;
            if (priority < Thread.MIN_PRIORITY) priority = Thread.MIN_PRIORITY;
            if (priority > Thread.MAX_PRIORITY) priority = Thread.MAX_PRIORITY;
            this.priority = priority;
            
            // update queue position accordingly
            synchronized (processorLock) {
                if (item == null) return;
                if (queue.remove(item)) {
                    prioritizedEnqueue(item);
                }
            }
        }


        /** This method is an implementation of the waitFinished method
        * in the RequestProcessor.Task. It check the current thread if it is
        * request processor thread and in such case runs the task immediatelly
        * to prevent deadlocks.
        */
        public void waitFinished () {
            if (isRequestProcessorThread()) { //System.err.println("Task.waitFinished on " + this + " from other task in RP: " + Thread.currentThread().getName());
                boolean toRun;

                synchronized (processorLock) {
// correct line:    toRun = (item == null) ? !isFinished (): (item.clear() && !isFinished ());
// the same:        toRun = !isFinished () && (item == null ? true : item.clear ());
                    toRun = !isFinished () && (item == null || item.clear ());
                }

                if (toRun) { //System.err.println("    ## running it synchronously");
                    run();
                } else { // it is already running in other thread of this RP
                    if (lastThread != Thread.currentThread()) {
                        super.waitFinished ();
		    }
//                    else {
//System.err.println("Thread waiting for itself!!!!! - semantics broken!!!");
//Thread.dumpStack();
//                    }
                }
                
            } else {
                super.waitFinished ();
            }
        }

        public String toString () {
            return "RequestProcessor.Task [" + name  + ", " + priority + "] for " + super.toString(); // NOI18N
        }
    }

//------------------------------------------------------------------------------
// The pending queue management implementation
//------------------------------------------------------------------------------
    

    /** Place the Task to the queue of pending tasks for immediate processing.
     * If there is no other Task planned, this task is immediatelly processed
     * in the Processor.
     */
    void enqueue (Item item) {
        synchronized (processorLock) {
            if (item.getTask() == null) return;
            prioritizedEnqueue(item);
            if (running < throughput) {
                running++;
                Processor proc = Processor.get();
                processors.add(proc);
                proc.setName(name);
                proc.attachTo(this);
            }
        }
    }

    // call it under queue lock i.e. processorLock
    private void prioritizedEnqueue(Item item) {
        int iprio = item.getPriority();
        if (queue.isEmpty()) {
            queue.add(item);
            item.enqueued = true;
            return;
        } else if (iprio <= ((Item)queue.get(queue.size()-1)).getPriority()) {
            queue.add(item);
            item.enqueued = true;
        } else {            
            for (ListIterator it = queue.listIterator(); it.hasNext();) {
                Item next = (Item) it.next();
                if (iprio > next.getPriority()) {
                    it.set(item);
                    it.add(next);
                    item.enqueued = true;
                    return;
                }
            }
            throw new IllegalStateException("Prioritized enqueue failed!");
        }
    }

    Task askForWork (Processor worker) {
        synchronized (processorLock) {
            if (stopped || queue.isEmpty()) { // no more work in this burst, return him
                processors.remove(worker);
                Processor.put(worker);
                running--;
                return null;
            } else { // we have some work for the worker, pass it
               return ((Item) queue.remove(0)).getTask();
            }
        }
    }

    static final boolean SLOW = Boolean.getBoolean("org.openide.util.RequestProcessor.Item.SLOW");
    
    /* One item representing the task pending in the pending queue */ 
    private static class Item extends Exception {
        private final RequestProcessor owner;
        private Task action;
        private boolean enqueued;
        
        Item (Task task, RequestProcessor rp) {
            super ("Posted StackTrace"); // NOI18N
            action = task;
            owner = rp;
        }

        Task getTask() {
            return action;
        }
            
        /** Annulate this request iff still possible.
         * @returns true if it was possible to skip this item, false
         * if the item was/is already processed */ 
        boolean clear() {
            synchronized (owner.processorLock) {
                action = null;
                return enqueued ? owner.queue.remove(this) : true;
            }
        }

        int getPriority() {
            return action.getPriority();
        }
        
        public Throwable fillInStackTrace() {
            return SLOW ? super.fillInStackTrace() : this;
        }
    }
    
//------------------------------------------------------------------------------
// The Processor management implementation
//------------------------------------------------------------------------------
  
    /**
    /** A special thread that processes timouted Tasks from a RequestProcessor.
     * It uses the RequestProcessor as a synchronized queue (a Channel),
     * so it is possible to run more Processors in paralel for one RequestProcessor
     */
    private static class Processor extends Thread {

        /** A stack containing all the inactive Processors */
        private static Stack pool = new Stack();
        
        /** Provide an inactive Processor instance. It will return either
         * existing inactive processor from the pool or will create a new instance
         * if no instance is in the pool.
         *
         * @return inactive Processor
         */
        static Processor get() {
            synchronized (pool) {
                if (pool.isEmpty()) {
                    Processor proc = new Processor();
                    proc.idle = false;
                    proc.start();
                    return proc;
                } else {
                    Processor proc = (Processor)pool.pop();
                    proc.idle = false;
                    return proc;
                }
            }
        }
    
        /** A way of returning a Processor to the inactive pool.
         *
         * @param ret the Processor to return to the pool. It shall be inactive.
         */
        static void put (Processor proc) {    
            synchronized (pool) {
                proc.setName("Inactive RequestProcessor thread"); // NOI18N
                proc.idle = true;
                pool.push(proc);
            }
        }



        /** Internal variable holding the Runnable to be run.
	 * Used for passing Runnable through Thread boundaries.
	 */
	//private Item task;

        private RequestProcessor source;
        
        /* One minute of inactivity and the Thread will die if not assigned */
        private static final int INACTIVE_TIMEOUT = 60000;
	
        private boolean idle = true;
        
	/** Waiting lock */
	private Object lock = new Object();

        public Processor () {
            super (getTopLevelThreadGroup(),
                "Inactive RequestProcessor thread"); // NOI18N
            setDaemon (true);
        }
        
        /** setPriority wrapper that skips setting the same priority
         * we'return already running at */
        void setPrio(int priority) {
            if (priority != getPriority()) setPriority(priority);
        }
        
	/**
	 * Sets an Item to be performed and notifies the performing Thread
	 * to start the processing.
	 *
	 * @param r the Item to run.
	 */
	public void attachTo (RequestProcessor src) {
	    synchronized (lock) {
                //assert(source == null);
                source = src;
		lock.notify();
	    }
	}

  	/**
	 * The method that will repeatedly wait for a request and perform it.
	 */
	public void run () {
	    for(;;) {
                RequestProcessor current = null;

                synchronized (lock) {
                    try {
                        if (source == null) lock.wait (INACTIVE_TIMEOUT); // wait for the job
                    } catch (InterruptedException e) {} // not interesting
		    current = source;
            	    source = null;
                    
                    if (current == null) { // We've timeouted
                        synchronized (pool) {
                            if (idle) { // and we're idle
                                pool.remove(this);
                                break; // exit the thread
                            } else { // this will happen if we've been just
                                continue;  // before timeout when we were assigned
                            }
                        }
                    }
		}
                
                Task todo;

                //logger ().log (ErrorManager.INFORMATIONAL, "Begining work " + getName ()); // NOI18N
                // while we have something to do
                while((todo = current.askForWork(this)) != null) {
                    if(todo != null) {
                        setPrio(todo.getPriority());
                        try {
                            //logger ().log ("  Executing " + todo); // NOI18N
                            todo.run ();
                            //logger ().log ("  Execution finished in" + getName ()); // NOI18N
                        } catch (RuntimeException e) {
                            doNotify(todo, e);
                        } catch (LinkageError e) {
                            doNotify(todo, e);
                        } catch (StackOverflowError e) {
                            // recoverable too
                            doNotify(todo, e);
                        }
                        // do not catch e.g. OutOfMemoryError, etc.
                    }
                }
                //logger ().log (ErrorManager.INFORMATIONAL, "Work finished " + getName ()); // NOI18N
	    }
	}
        
        /** @see "#20467" */
        private static void doNotify(RequestProcessor.Task todo, Throwable ex) {
            ErrorManager err = ErrorManager.getDefault ();
            err.annotate(ex, ErrorManager.EXCEPTION, null,
                NbBundle.getMessage (RequestProcessor.class, "EXC_IN_REQUEST_PROCESSOR"),
                SLOW ? todo.item : null, null );
            err.notify(ex);
        }
	
        /**
         * @return a top level ThreadGroup. The method ensures that even
         * Processors created by internal execution will survive the
         * end of the task.
         */
        static ThreadGroup getTopLevelThreadGroup() {
            java.security.PrivilegedAction run = new java.security.PrivilegedAction() {
                public Object run() {
                    ThreadGroup current = Thread.currentThread().getThreadGroup();
                    while (current.getParent() != null) {
                        current = current.getParent();
                    }
                    return current;
                }
            };
            return (ThreadGroup) java.security.AccessController.doPrivileged(run);
        }
    }
}
