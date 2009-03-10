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

package org.openide.text;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;
import java.awt.print.PrinterJob;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterAbortException;
import java.beans.PropertyChangeSupport;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoableEdit;
import org.openide.DialogDisplayer;

import org.openide.awt.UndoRedo;
import org.openide.actions.*;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.windows.*;
import org.openide.util.Task;
import org.openide.util.TaskListener;
//import org.openide.util.actions.SystemAction;
import org.openide.util.RequestProcessor;
import org.openide.util.NbBundle;
import org.openide.util.UserQuestionException;
import org.openide.text.EnhancedChangeEvent;

/** Support for associating an editor and a Swing {@link Document}.
* Can be assigned as a cookie to any editable data object.
* This class is abstract, so any subclass has to provide implementation
* for abstract method (usually for generating of messages) and also
* provide environment {@link Env} to give this support access to 
* input/output streams, mime type and other features of edited object.
*
* <P>
* This class implements methods of the interfaces
* {@link org.openide.cookies.EditorCookie}, {@link org.openide.cookies.OpenCookie},
* {@link org.openide.cookies.EditCookie},
* {@link org.openide.cookies.ViewCookie}, {@link org.openide.cookies.LineCookie},
* {@link org.openide.cookies.CloseCookie}, and {@link org.openide.cookies.PrintCookie}
* but does not implement
* those interfaces. It is up to the subclass to decide which interfaces
* really implement and which not.
*
* @author Jaroslav Tulach
*/
public abstract class CloneableEditorSupport extends CloneableOpenSupport {
    
    /** Common name for editor mode. */
    public static final String EDITOR_MODE = "editor"; // NOI18N

    /** Flag saying if the CloneableEditorSupport handles already the UserQuestionException*/
    private boolean inUserQuestionExceptionHandler;

    /** Used for allowing to pass getDocument method
     * when called from loadDocument from loadTask. */
    private static final ThreadLocal LOCAL_LOAD_TASK = new ThreadLocal();
    
    /** Task for loading the document. */
    private Task loadTask;

    /** Task for preparing the document. Consists for loading a document (runs loadTask),
    * firing </code>stateChange</code> and 
    * initializing it by attaching listeners listening to document changes, such as SavingManager and
    * LineSet. 
    */
    private Task prepareTask;

    /** editor kit to work with */
    private EditorKit kit;

    /** document we work with */
    private StyledDocument doc;



    /** Non default MIME type used to editing */
    private String mimeType;

    /** Actions to show in toolbar */
//    private SystemAction[] actions;



    /** Listener to the document changes and all other changes */
    private Listener listener;

    /** the undo/redo manager to use for this document */
    private UndoRedo.Manager undoRedo;


    
    /** lines set for this object */
    private Line.Set lineSet;
    /** Lock used when for updating lineSet. */
    private final Object LOCK_LINE_SET = new Object();


    /** Helper variable to prevent multiple cocurrent printing of this
     * instance. */
    private boolean printing;
    /** Lock used for access to <code>printing</code> variable. */
    private final Object LOCK_PRINTING = new Object();

    /** position manager */
    private PositionRef.Manager positionManager;

    /** The string which will be appended to the name of top component
    * when top component becomes modified */
//    protected String modifiedAppendix = " *"; // NOI18N

    /** Listeners for the changing of the state - document in memory X closed. */
    private HashSet listeners;

    /** last selected editor. */
    transient CloneableEditor lastSelected;


    /** The time of the last save to determine the real external modifications */
    private long lastSaveTime;

    /** Whether the reload dialog is currently opened. Prevents poping of multiple
     * reload dialogs if there is more external saves.
     */
    private boolean reloadDialogOpened;


    /** Support for property change listeners*/
    private PropertyChangeSupport propertyChangeSupport;
    
    /** Creates new CloneableEditorSupport attached to given environment.
    *
    * @param env environment that is source of all actions around the 
    *    data object
    */
    public CloneableEditorSupport(Env env) {
        super (env);
    }
    
    //
    // abstract messages section
    //
    
    /** Constructs message that should be displayed when the data object
    * is modified and is being closed.
    *
    * @return text to show to the user
    */
    protected abstract String messageSave ();

    /** Constructs message that should be used to name the editor component.
    *
    * @return name of the editor
    */
    protected abstract String messageName ();
    
    /** Text to use as tooltip for component.
    *
    * @return text to show to the user
    */
    protected abstract String messageToolTip ();

    //
    // Section of getter of default objects
    // 

    /** Getter for the environment that was provided in the constructor.
    * @return the environment
    */
    final Env env () {
        return (Env)env;
    }

    /** Getter for the kit that loaded the document.
    */
    final EditorKit kit () {
        return kit;
    }


    /** Getter for undo redo manager.
    */
    protected final synchronized UndoRedo.Manager getUndoRedo() {
        if(undoRedo == null) {
            undoRedo = createUndoRedoManager();
        }
                
        return undoRedo;
    }

    /** Provides access to position manager for the document.
    * It maintains a set of positions even the document is in memory
    * or is on the disk.
    *
    * @return position manager
    */
    final synchronized PositionRef.Manager getPositionManager() {
        if(positionManager == null) {
            positionManager = new PositionRef.Manager(this);
        }
                
        return positionManager;
    }


    /** Overrides superclass method, first processes document preparation.
     * @see #prepareDocument */
    public void open() {
        prepareDocument().waitFinished();
        super.open();
    }

    //
    // EditorCookie.Observable implementation
    // 
    
    /** Add a PropertyChangeListener to the listener list.
     * See {@link org.openide.cookies.EditorCookie.Observable}.
     * @param l  the PropertyChangeListener to be added
     * @since 3.40
     */
    public final void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        getPropertyChangeSupport().addPropertyChangeListener (l);
    }
    
    /** Remove a PropertyChangeListener from the listener list.
     * See {@link org.openide.cookies.EditorCookie.Observable}.
     * @param l the PropertyChangeListener to be removed
     * @since 3.40
     */
    public final void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        getPropertyChangeSupport().removePropertyChangeListener (l);
    }

    /** Report a bound property update to any registered listeners.
     * @param propertyName the programmatic name of the property that was changed.
     * @param oldValue rhe old value of the property.
     * @param newValue the new value of the property.
     * @since 3.40
     */
    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    }
    
    private synchronized PropertyChangeSupport getPropertyChangeSupport() {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        return propertyChangeSupport;
    }
    
    //
    // EditorCookie implementation
    // 


    // editor cookie .......................................................................


    /** Load the document into memory. This is done
    * in different thread. A task for the thread is returned
    * so anyone may test whether the loading has been finished
    * or is still in process.
    *
    * @return task for control over loading
    */
    public Task prepareDocument() {
        return prepareDocument(false);
    }

    /** @param clearDocument indicates whether the document is needed
     *                       to clear before (used for reloading) */
    private Task prepareDocument(final boolean clearDocument) {
        // first test is done outside of getLock block because the getLock
        // can be held for a long time
        Task t = prepareTask;
        if (t != null) {
            return t;
        }
        
        synchronized (getLock ()) {
            if (prepareTask != null)
                return prepareTask;

            // listen to modifications on env, but remove
            // previous instance first
            env.removePropertyChangeListener(getListener());
            env.addPropertyChangeListener(getListener());

            // after call to this method the originalDoc and kit are initialized
            // in spite of that the document is not yet fully read in

            kit = createEditorKit ();
            if (doc == null) {
                doc = createStyledDocument (kit);
            }

            // The thread nume should be: "Loading document " + env; // NOI18N
            prepareTask = RequestProcessor.getDefault().post(new Runnable () {
                public void run () {
                    try {
                        synchronized (getLock ()) {
                            if(clearDocument) {
                                // #24676. Reloading: Put positions into memory
                                // and fire document is closing (little trick
                                // to detach annotations).
                                getPositionManager().documentClosed();
                                fireDocumentChange(doc, true);

                                clearDocument();
                            }
                            
                            // uses the listener's run method to initialize whole document
                            loadTask = new Task(getListener());
                            loadTask.run ();
                        }

                        fireDocumentChange(doc, false);
                    } catch (RuntimeException t) {
                          t.printStackTrace();
                          throw t;
                    }
                }
            });

            return prepareTask;
        }
    }
    
    /** Clears the <code>doc</code> document. Helper method. */
    private void clearDocument() {
        NbDocument.runAtomic(doc, new Runnable() {
             public void run() {
                 try {
                     doc.removeDocumentListener(getListener());
                     doc.remove(0, doc.getLength()); // remove all text
                     doc.addDocumentListener(getListener());
                 } catch(BadLocationException ble) {
                     ErrorManager.getDefault().notify(
                         ErrorManager.INFORMATIONAL, ble);
                 }
             }
        });
    }

    /** Get the document associated with this cookie.
    * It is an instance of Swing's {@link StyledDocument} but it should
    * also understand the NetBeans {@link NbDocument#GUARDED} to
    * prevent certain lines from being edited by the user.
    * <P>
    * If the document is not loaded the method blocks until
    * it is.
    *
    * @return the styled document for this cookie that
    *   understands the guarded attribute
    * @exception IOException if the document could not be loaded
    */
    public StyledDocument openDocument () throws IOException {
        for (;;) {
            // load the document
            prepareDocument ().waitFinished ();
            IOException loadExc = getListener().checkLoadException();
            if (loadExc != null) {
                throw loadExc;
            }
            
            StyledDocument d = doc;
            if (d != null)
                return d;
        }
    }

    /** Get the document. This method may be called before the document initialization
     * (<code>prepareTask</code>)
     * has been completed, in such a case the document must not be modified.
     * @return document or <code>null</code> if it is not yet loaded
     */
    public StyledDocument getDocument () {
        // XXX #16048. In case there is called this method from loadTask
        // (possible only via LineListener->DocumentLine..).
        // PENDING Needs to be tried to redesign DocumentLine to avoid this.
        if(LOCAL_LOAD_TASK.get() != null) {
            return doc;
        }
        
        for (;;) {
            Task t = loadTask;
            if (t != null) {
                // if an task exists
                t.waitFinished ();
                return doc;
            } else {
                return null;
            }
        }
    }


    /** Test whether the document is modified.
    * @return <code>true</code> if the document is in memory and is modified;
    *   otherwise <code>false</code>
    */
    public boolean isModified () {
        return env ().isModified ();
    }

    /* Whether the file was externally modified or not.
     * This flag is used only in saveDocument to prevent
     * overriding of externally modified file. See issue #32777.
     */
    private boolean externallyModified;
        
        
    /** Save the document in this thread.
    * Create 'orig' document for the case that the save would fail.
    * @exception IOException on I/O error
    */
    public void saveDocument () throws IOException {
        // #17714: Don't try to save unmodified doc.
        if(!env().isModified()) {
            return;
        }
        
        //#32777: check that file was not modified externally.
        // If it was then cancel saving operation. It is not absolutely
        // correct, but there is no other way.
        if (lastSaveTime != -1) {
            externallyModified = false;
            // asking for time should if necessary refresh the underlaying object
            // (eg. FileObject) and this change can result in document reload task
            // which will set externallyModified to true
            env().getTime();
            if (externallyModified) {
                // save operation must be cancelled now. The user get message box
                // asking user to reload externally modified file. 
                return;
            }
        }
        
        StyledDocument myDoc = getDocument();

        OutputStream os = null;

        // write the document
        long oldSaveTime = lastSaveTime;
        try {
            lastSaveTime = -1;
            os = new BufferedOutputStream(env ().outputStream());
            saveFromKitToStream (myDoc, kit, os);

            if (os != null) {
                os.close(); // peforms firing
                os = null;
            }

            // remember time of last save
            lastSaveTime = System.currentTimeMillis();

            notifyUnmodified ();

        } catch (BadLocationException ex) {
            ErrorManager.getDefault().notify(ex);
        } finally {
            if (lastSaveTime == -1) // restore for unsuccessful save
                lastSaveTime = oldSaveTime;

            if (os != null) // try to close if not yet done
                os.close();
        }

        // Insert before-save undo event to enable unmodifying undo
        getUndoRedo().undoableEditHappened(
                new UndoableEditEvent(this, new BeforeSaveEdit(lastSaveTime)));

        // update cached info about lines
        updateLineSet (true);
        updateTitles ();
    }

    /* List of all JEditorPane's opened by this editor support.
    * The first item in the array should represent the component
    * that is currently selected or has been selected lastly.
    *
    * If you override this method and return also your own editor
    * panes which are not descendants of {@link CloneableEditor}
    * then it is your responsibility to fire
    * {@link org.openide.cookies.EditorCookie.Observable#PROP_OPENED_PANES}
    * property change whenever the list of opened panes changes.
    *
    * @return array of panes or null if no pane is opened.
    *   In no case empty array is returned.
    */
    public JEditorPane[] getOpenedPanes () {
        LinkedList ll = new LinkedList ();
        Enumeration en = allEditors.getComponents ();
        while (en.hasMoreElements ()) {
            Object o = en.nextElement ();
            if (o instanceof CloneableEditor) {
                CloneableEditor ed = (CloneableEditor)o;
                
                // #23491: pane could be still null, not yet shown component.
                // [PENDING] Right solution? TopComponent opened, but pane not.
                if(ed.pane == null) {
                    continue;
                }
                
                if (lastSelected == ed) {
                    ll.addFirst (ed.pane);
                } else {
                    ll.add (ed.pane);
                }
            }
        }
        return ll.isEmpty () ?
               null : (JEditorPane[])ll.toArray (new JEditorPane[ll.size ()]);
    }

    //
    // LineSet interface impl
    //

    /** Get the line set for all paragraphs in the document.
    * @return positions of all paragraphs on last save
    */
    public Line.Set getLineSet () {
        return updateLineSet (false);
    }


    //
    // Print interface
    //

    /** A printing implementation suitable for {@link org.openide.cookies.PrintCookie}. */
    public void print() {
        synchronized(LOCK_PRINTING) {
            if(printing) {
                return;
            }

            printing = true;
        }

        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            Object o = NbDocument.findPageable(openDocument());
            if (o instanceof Pageable) {
                job.setPageable((Pageable) o);
            } else {
                PageFormat pf = PrintSettings.getPageFormat(job);
                job.setPrintable((Printable) o, pf);
            }
            if (job.printDialog()) {
                job.print();
            }
        } catch (FileNotFoundException e) {
            ErrorManager.getDefault().notify(e);
            String msg = NbBundle.getBundle(CloneableEditorSupport.class)
                .getString("CTL_Bad_File"); // NOI18N
            notifyInAWT(msg);
        } catch (IOException e) {
            ErrorManager.getDefault().notify(e);
        } catch (PrinterAbortException e) { // user exception
            String msg = NbBundle.getBundle(CloneableEditorSupport.class)
                .getString("CTL_Printer_Abort"); // NOI18N
            notifyInAWT(msg);
        } catch (PrinterException e) {
            ErrorManager.getDefault().notify(e);
        } finally {
            synchronized(LOCK_PRINTING) {
                printing = false;
            }
        }
    }
    
    static void notifyInAWT(final String msg) {
        if (java.awt.EventQueue.isDispatchThread()) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg));
        } else {
            java.awt.EventQueue.invokeLater(new Runnable() { // display in the awt thread
                                                public void run() {
                                                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg));
                                                }
                                            });
        }
    }

    //
    // Methods overriden from CloneableOpenSupport
    // 


    /** Prepares document, creates and initializes
     * new <code>CloneableEditor</code> component.
     * Typically do not override this method. 
     * For creating your own <code>CloneableEditor</code> type component
     * override {@link #createCloneableEditor} method.
     *
     * @return the {@link CloneableEditor} for this support
     */
    protected CloneableTopComponent createCloneableTopComponent () {
        // initializes the document if not initialized
        prepareDocument ();
        
        CloneableEditor ed = createCloneableEditor ();
        initializeCloneableEditor (ed);
        return ed;
    }


    /** Should test whether all data is saved, and if not, prompt the user
    * to save.
    *
    * @return <code>true</code> if everything can be closed
    */
    protected boolean canClose () {
        if (env ().isModified ()) {
            String msg = messageSave ();

            ResourceBundle bundle = NbBundle.getBundle(CloneableEditorSupport.class);

            String saveOption = bundle.getString("CTL_Save");
            String discardOption = bundle.getString("CTL_Discard");

            NotifyDescriptor nd = new NotifyDescriptor(
                msg,
                bundle.getString("LBL_SaveFile_Title"),
                NotifyDescriptor.YES_NO_CANCEL_OPTION,
                NotifyDescriptor.QUESTION_MESSAGE,
                new Object[] {saveOption, discardOption, NotifyDescriptor.CANCEL_OPTION},
                saveOption
            );
                
            Object ret = DialogDisplayer.getDefault().notify(nd);

            if (NotifyDescriptor.CANCEL_OPTION.equals(ret)
                    || NotifyDescriptor.CLOSED_OPTION.equals(ret)
               ) {
                return false;
            }

            if (saveOption.equals(ret)) {
                try {
                    saveDocument ();
                } catch (IOException e) {
                    ErrorManager.getDefault().notify(e);
                    return false;
                }
            }
        }
         
        return true;
                        
/* old code was:
        SaveCookie savec = (SaveCookie) entry.getDataObject().getCookie(SaveCookie.class);
        if (savec != null) {
            MessageFormat format = new MessageFormat(NbBundle.getBundle(EditorSupport.class).getString("MSG_SaveFile")); // NOI18N
            String msg = format.format(new Object[] { entry.getDataObject().getName()});
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
            Object ret = DialogDisplayer.getDefault().notify(nd);

            if (NotifyDescriptor.CANCEL_OPTION.equals(ret)
                    || NotifyDescriptor.CLOSED_OPTION.equals(ret)
               ) {
                return false;
            }

            if (NotifyDescriptor.YES_OPTION.equals(ret)) {
                try {
                    savec.save();
                }
                catch (IOException e) {
                    ErrorManager.getDefault().notify(e);
                    return false;
                }
            }
        }
*/
    }


    //
    // public methods provided by this class
    //




    /** Test whether the document is in memory, or whether loading is still in progress.
    * @return <code>true</code> if document is loaded
    */
    public boolean isDocumentLoaded() {
        return loadTask != null;
    }

    /**
    * Set the MIME type for the document.
    * @param s the new MIME type
    */
    public void setMIMEType (String s) {
        mimeType = s;
    }

    /** Adds a listener for status changes. An event is fired
    * when the document is moved or removed from memory.
    * @param l new listener
    * @deprecated Deprecated since 3.40. Use {@link #addPropertyChangeListener} instead.
    * See also {@link org.openide.cookies.EditorCookie.Observable}.
    */
    public synchronized void addChangeListener (ChangeListener l) {
        if (listeners == null)
            listeners = new HashSet (8);
        listeners.add (l);
    }
    

    /** Removes a listener for status changes.
     * @param l listener to remove
    * @deprecated Deprecated since 3.40. Use {@link #removePropertyChangeListener} instead.
    * See also {@link org.openide.cookies.EditorCookie.Observable}.
    */
    public synchronized void removeChangeListener (ChangeListener l) {
        if (listeners != null)
            listeners.remove (l);
    }


    // Position management methods


    /** Create a position reference for the given offset.
    * The position moves as the document is modified and
    * reacts to closing and opening of the document.
    *
    * @param offset the offset to create position at
    * @param bias the Position.Bias for new creating position.
    * @return position reference for that offset
    */
    public final PositionRef createPositionRef (int offset, Position.Bias bias) {
        return new PositionRef (getPositionManager (), offset, bias);
    }


    //
    // Methods that can be overriden by subclasses
    //


    /** Allows subclasses to create their own version
     * of <code>CloneableEditor</code> component.
     * @return the {@link CloneableEditor} for this support
     */
    protected CloneableEditor createCloneableEditor () {
        return new CloneableEditor (this);
    }
    
    /** Initialize the editor. This method is called after the editor component
     * is deserialized and also when the component is created. It allows
     * the subclasses to annotate the component with icon, selected nodes, etc.
     *
     * @param editor the editor that has been created and should be annotated
     */
    protected void initializeCloneableEditor (CloneableEditor editor) {
    }

    /** Create an undo/redo manager.
    * This manager is then attached to the document, and listens to
    * all changes made in it.
    * <P>
    * The default implementation simply uses <code>UndoRedo.Manager</code>.
    *
    * @return the undo/redo manager
    */
    protected UndoRedo.Manager createUndoRedoManager () {
        return new UndoRedo.Manager ();
    }

    /**
     * Actually write file data to an output stream from an editor kit's document.
     * Called during a file save by {@link #saveDocument}.
     * <p>The default implementation just calls {@link EditorKit#write(OutputStream, Document, int, int) EditorKit.write(...)}.
     * Subclasses could override this to provide support for persistent guard blocks, for example.
     * @param doc the document to write from
     * @param kit the associated editor kit
     * @param stream the open stream to write to
     * @throws IOException if there was a problem writing the file
     * @throws BadLocationException should not normally be thrown
     * @see #loadFromStreamToKit
     */
    protected void saveFromKitToStream (StyledDocument doc, EditorKit kit, OutputStream stream) throws IOException, BadLocationException {
        kit.write(stream, doc, 0, doc.getLength());
    }


    /**
     * Actually read file data into an editor kit's document from an input stream.
     * Called during a file load by {@link #prepareDocument}.
     * <p>The default implementation just calls {@link EditorKit#read(InputStream, Document, int) EditorKit.read(...)}.
     * Subclasses could override this to provide support for persistent guard blocks, for example.
     * @param doc the document to read into
     * @param stream the open stream to read from
     * @param kit the associated editor kit
     * @throws IOException if there was a problem reading the file
     * @throws BadLocationException should not normally be thrown
     * @see #saveFromKitToStream
     */
    protected void loadFromStreamToKit (StyledDocument doc, InputStream stream, EditorKit kit) throws IOException, BadLocationException {
        kit.read(stream, doc, 0);
    }

    /** Reload the document in response to external modification.
    * @return task that reloads the document. It can be also obtained
    *  by calling <tt>prepareDocument()</tt>.
    */
    protected Task reloadDocument() {
        synchronized (getLock ()) {
            if (doc != null) {
                // UndoManager must be detached from document here because it will be attached in loadDocument()
                doc.removeUndoableEditListener (getUndoRedo ());
                // Remember caret positions in all opened panes
                final JEditorPane[] panes = getOpenedPanes();
                final int[] carets;
                if (panes != null) {
                    carets = new int[panes.length];
                    for(int i = 0; i < panes.length; i++) {
                        carets[i] =  panes[i].getCaretPosition();
                    }
                } else {
                    carets = new int[0];
                }

                prepareTask = null; // make sure new loading will occur
                final Task docLoadTask = prepareDocument(true);

                docLoadTask.addTaskListener(
                  new TaskListener() {
                      public void taskFinished (Task task) {
                          //Bugfix #12338: This Swing code replanned to AWT thread
                          SwingUtilities.invokeLater(new Runnable() {
                              public void run () {
                                  if (panes != null) {
                                      for (int i = 0; i < panes.length; i++) {
                                          // #26407 Adjusts caret position,
                                          // (reloaded doc could be shorter).
                                          int textLength = panes[i].getText().length();
                                          if(carets[i] > textLength) {
                                              carets[i] = textLength;
                                          }
                                          
                                          panes[i].setCaretPosition(carets[i]);
                                      }
                                  }
                                  getUndoRedo().discardAllEdits(); // reset undo manager
                                  // Insert before-save undo event to enable unmodifying undo
                                  getUndoRedo().undoableEditHappened(
                                      new UndoableEditEvent(
                                          CloneableEditorSupport.this,
                                          new BeforeSaveEdit(lastSaveTime)
                                      )
                                  );
                                 
                                  notifyUnmodified ();
                                  updateLineSet(true);
                              }
                          });
                          docLoadTask.removeTaskListener(this);
                      }
                    }
                  );


                return docLoadTask;
            }
        }

        return prepareDocument();
    }


    /** Creates editor kit for this source.
    * @return editor kit
    */
    protected EditorKit createEditorKit () {
        if (kit != null) return kit;

        if (mimeType != null) {
            kit = JEditorPane.createEditorKitForContentType (mimeType);
        } else {
            String defaultMIMEType = env ().getMimeType ();
            kit = JEditorPane.createEditorKitForContentType (defaultMIMEType);
        }

        if (isDumbKit (kit)) {
            kit = JEditorPane.createEditorKitForContentType ("text/plain"); // NOI18N
        }

        if (isDumbKit (kit)) {
            kit = new PlainEditorKit ();
        }

        return kit;
    }

    /** Is this a useless default kit?
     * @param kit the kit to test
     * @return true if so
     */
    private boolean isDumbKit (EditorKit kit) {
	if (kit == null) return true;
	String clazz = kit.getClass ().getName ();
	return (clazz.equals ("javax.swing.text.DefaultEditorKit") || // NOI18N
		clazz.equals ("javax.swing.JEditorPane$PlainEditorKit") || // NOI18N
		clazz.equals ("javax.swing.text.html.HTMLEditorKit")); // NOI18N
    }

    /** Method that can be overriden by children to create empty
    * styled document or attach additional document properties to it.
    * 
    * @param kit the kit to use
    * @return styled document to use 
    */
    protected StyledDocument createStyledDocument (EditorKit kit) {
        StyledDocument sd = createNetBeansDocument (kit.createDefaultDocument ());
        sd.putProperty("mimeType", mimeType != null ? mimeType : env().getMimeType()); // NOI18N
        return sd;
    }
    
    /** Notification method called when the document become unmodified.
    * Called after save or after reload of document.
    * <P>
    * This implementation simply marks the associated 
    * environement unmodified and updates titles of all components.
    */
    protected void notifyUnmodified () {
        env.unmarkModified ();
        updateTitles ();
    }
    

    /** Called when the document is being modified.
    * The responsibility of this method is to inform the environment
    * that its document is modified. Current implementation
    * Just calls env.setModified (true) to notify it about 
    * modification.
    *
    * @return true if the environment accepted being marked as modified
    *    or false if it refused it and the document should still be unmodified
    */
    protected boolean notifyModified () {
        boolean locked = true;
        try {
            env.markModified ();
        } catch (final UserQuestionException ex) {
	    synchronized (this) {
		if (! this.inUserQuestionExceptionHandler){
		    this.inUserQuestionExceptionHandler = true;
		    RequestProcessor.getDefault().post(new Runnable() {
			    public void run () {
				NotifyDescriptor nd = new NotifyDescriptor.Confirmation (ex.getLocalizedMessage ());
				Object res = DialogDisplayer.getDefault ().notify (nd);
				if (NotifyDescriptor.OK_OPTION.equals(res)) {
				    try {
					ex.confirmed ();
				    } catch (IOException ex1) {
					ErrorManager.getDefault ().notify (ex1);
				    }
				}
				synchronized (CloneableEditorSupport.this) {
				    CloneableEditorSupport.this.inUserQuestionExceptionHandler = false;
				}
			    }
			});
		}
	    }
            locked = false;
        } catch (IOException e) { // locking failed
            if (e.getMessage () != e.getLocalizedMessage ()) {
                StatusDisplayer.getDefault().setStatusText(e.getLocalizedMessage());
            }
            locked = false;
        }
        
        if (!locked) {
            revertUpcomingUndo();
            return false;            
        }
        
        updateTitles ();
        return true;
    }

    /** Resets listening on <code>UndoRedo</code>,
     * and in case next undo edit comes, schedules processesing of it. 
     * Used to revert modification e.g. of document of [read-only] env. */
    private void revertUpcomingUndo() {
        Listener l = getListener();
        l.setUndoTask(createUndoTask());
        
        UndoRedo ur = getUndoRedo();
        ur.removeChangeListener(l);
        ur.addChangeListener(l);
    }
    
    /** Creates <code>Runnable</code> which tries to make one undo. Helper method.
     * @see #revertUpcomingUndo */
    private Runnable createUndoTask() {
        return new Runnable() {
            public void run() {
                StyledDocument sd = doc;
                if(sd == null) {
                    // #20883, doc can be null(!), doCloseDocument was faster.
                    return;
                }
                UndoRedo ur = getUndoRedo();
                sd.removeDocumentListener(getListener());
                try {
                    if(ur.canUndo()) {
                        Toolkit.getDefaultToolkit().beep();
                        ur.undo();
                    }
                } catch(CannotUndoException cne) {
                    ErrorManager.getDefault().notify(
                        ErrorManager.INFORMATIONAL, cne);
                } finally {
                    sd.addDocumentListener(getListener());
                }
            }
        };
    }
    
    /** Method that is called when all components of the support are
    * closed. The default implementation closes the document.
    *
    */
    protected void notifyClosed () {
        closeDocument();
    }

    // XXX #25762 [PENDING] Needed protected method to allow subclasses to alter it.
    /** Indicates whether the <code>Env</code> is read only. */
    boolean isEnvReadOnly() {
        return false;
    }
    
    /** Allows access to the document without any checking.
    */
    final StyledDocument getDocumentHack () {
        return doc;
    }
    

    /** Getter for data object associated with this 
    * data object.
    */
    DataObject getDataObjectHack () {
        return null;
    }


    // LineSet methods .....................................................................

    /** Updates the line set.
    * @param clear clear any cached set?
    * @return the set
    */
    Line.Set updateLineSet (boolean clear) {
        synchronized(LOCK_LINE_SET) {
            if(lineSet != null && !clear) {
                return lineSet;
            }

            Line.Set oldSet = lineSet;

            if (doc == null) {
                lineSet = new EditorSupportLineSet.Closed(CloneableEditorSupport.this);
            } else {
                lineSet = new EditorSupportLineSet(CloneableEditorSupport.this, doc);
            }

            if(oldSet != null) {
                synchronized(oldSet.lines) {
                    lineSet.lines.putAll(oldSet.lines);
                }
            }

            return lineSet;
        }
    }


    // other public methods ................................................................


    /* JST: Commented out
    * Set actions for toolbar.
    * @param actions list of actions
    *
    public void setActions (SystemAction[] actions) {
        this.actions = actions;
    }

    /** Utility method which enables or disables listening to modifications
    * on asociated document.
    * <P>
    * Could be useful if we have to modify document, but do not want the
    * Save and Save All actions to be enabled/disabled automatically.
    * Initially modifications are listened to.
    * @param listenToModifs whether to listen to modifications
    *
    public void setModificationListening (final boolean listenToModifs) {
        if (this.listenToModifs == listenToModifs) return;
        this.listenToModifs = listenToModifs;
        if (doc == null) return;
        if (listenToModifs)
            doc.addi(getModifL());
        else
            doc.removeDocumentListener(getModifL());
    }
    */



    /** Loads the document for this object.
    * @param kit kit to use
    * @param d original document to load data into
    */
    private void loadDocument (EditorKit kit, StyledDocument doc) throws IOException {
        Throwable aProblem = null;
        
        try {
            InputStream is = new BufferedInputStream(env ().inputStream ());
            try {
                // read the document
                loadFromStreamToKit (doc, is, kit);
            } finally {
                is.close ();
            }
            // attach undo/redo manager
            doc.addUndoableEditListener (getUndoRedo ());
        } catch (IOException ex) {
            aProblem = ex;
            throw ex;
        } catch (Exception e) { // incl. BadLocationException
            aProblem = e;
        } finally {        
            if (aProblem != null) {
                ErrorManager err = ErrorManager.getDefault ();
                err.annotate (aProblem, NbBundle.getMessage (
                    CloneableEditorSupport.class,
                    "EXC_LoadDocument", // NOI18N
                    messageName ()
                ));
                err.notify (aProblem);
            }
        }
    }

    /** Closes all opened editors (if the user agrees) and
    * flushes content of the document to the file.
    *
    * @param ask ask whether to save the document or not?
    * @return <code>false</code> if the operation is cancelled
    */
    protected boolean close (boolean ask) {
        if (!super.close (ask)) {
            // if not all editors has been closed
            return false;
        }

        notifyClosed ();
        return true;
    }

    /** Clears all data from memory.
    */
    private void closeDocument () {
        for (;;) {
            Task prep;

            synchronized (getLock()) {
                if (doc == null) {
                    return;
                }

                if (loadTask == null) {
                    return;
                }

                prep = prepareTask;
                if (prep == null) {
                    return;
                }
                
                if (prep.isFinished ()) {
                    doCloseDocument ();
                    return;
                }
            }

            /* Wait for loading task to be finished
             * so that the document etc. stays valid
             * during the load operation.
             */
            prep.waitFinished();
        }
    }
     
    /** Is called under getLock () to close the document.
     */
    private void doCloseDocument () {
        loadTask = null;
        prepareTask = null;

        // notifies the support that 
        env ().removePropertyChangeListener(getListener());
        notifyUnmodified ();

        if (doc != null) {
            getUndoRedo().discardAllEdits();
            doc.removeUndoableEditListener (getUndoRedo ());
            doc.removeDocumentListener(getListener());
        }

        if (positionManager != null) {
            positionManager.documentClosed ();
            fireDocumentChange(doc, true);
        }
        doc = null;

        kit = null;

        updateLineSet (true);
    }

    /** Handles the actual reload of document.
    * @param doReload false if we should first ask the user
    */
    private void checkReload(boolean doReload) {
        StyledDocument doc = this.doc;
        
        if (doc == null) {
            return;
        }
        
        if (!doReload && !reloadDialogOpened) {
            String msg = NbBundle.getMessage (CloneableEditorSupport.class,
                "FMT_External_change", // NOI18N
                doc.getProperty (javax.swing.text.Document.TitleProperty)
            );

            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_OPTION);

            reloadDialogOpened = true;
            try {
                Object ret = DialogDisplayer.getDefault().notify(nd);
                if (NotifyDescriptor.YES_OPTION.equals(ret)) {
                    doReload = true;
                }
            } finally {
                reloadDialogOpened = false;
            }
        }

        if (doReload) {
            //Bugfix #9612: Call of reloadDocument() is now posted to 
            //RequestProcessor
            RequestProcessor.getDefault().post(new Runnable() {
                public void run () {
                    reloadDocument();
                }
            });
        }
    }



    /** Creates netbeans document for a given document.
    * @param d document to use as underlaying one
    * @return styled document that could support Guarded.ATTRIBUTE
    */
    private static StyledDocument createNetBeansDocument (Document d) {
        if (d instanceof StyledDocument) {
            return (StyledDocument)d;
        } else {
            // create filter
            return new FilterDocument (d);
        }
    }

    private final void fireDocumentChange(StyledDocument document, boolean closing) {
        fireStateChangeEvent(document, closing);
        firePropertyChange(EditorCookie.Observable.PROP_DOCUMENT, null, null);
    }

    /** Fires a status change event to all listeners. */
    private final void fireStateChangeEvent(StyledDocument document, boolean closing) {
        if (listeners != null) {
            EnhancedChangeEvent event = new EnhancedChangeEvent(this, document, closing);
            HashSet s;
            synchronized (this) {
                s = ((HashSet)listeners.clone ());
            }

            Iterator it = s.iterator ();
            while (it.hasNext ()) {
                ChangeListener l = (ChangeListener) it.next();
                l.stateChanged(event);
            }
        }
    }
    
    /** Updates titles of all editors.
    */
    protected void updateTitles () {
        Enumeration en = allEditors.getComponents ();
        while (en.hasMoreElements()) {
            Object o = en.nextElement();
            if (o instanceof CloneableEditor) {
                CloneableEditor e = (CloneableEditor)o;
                e.updateName();
            }
        }
    }

    // #18981. There could happen a thing also another class type
    // of CloneableTopCoponent then CloneableEditor could be in allEditors.
    /** Opens a <code>CloneableEditor</code> component. */
    private CloneableEditor openEditorComponent() {
        synchronized (getLock()) {
            CloneableEditor ce = getAnyEditor();
            
            if(ce != null) {
                ce.open();
                return ce;
            } else {
                // no opened editor
                String msg = messageOpening ();
                if (msg != null) {
                    StatusDisplayer.getDefault().setStatusText(msg);
                }

                // initializes the document if not initialized
                prepareDocument();

                ce = createCloneableEditor ();
                initializeCloneableEditor(ce);
                ce.setReference(allEditors);
                ce.open();

                msg = messageOpened ();
                if (msg == null) {
                    msg = ""; // NOI18N
                }
                StatusDisplayer.getDefault().setStatusText(msg);
                return ce;
            }
        }
    }
    
    /** If one or more editors are opened finds one.
    * @return an editor or null if none is opened
    */
    CloneableEditor getAnyEditor () {
        CloneableTopComponent ctc;
        ctc = allEditors.getArbitraryComponent();
        
        if(ctc == null) {
            return null;
        }

        if(ctc instanceof CloneableEditor) {
            return (CloneableEditor)ctc;
        } else {
            Enumeration en = allEditors.getComponents();
            while(en.hasMoreElements()) {
                Object o = en.nextElement();
                if(o instanceof CloneableEditor) {
                    return (CloneableEditor)o;
                }
            }

            return null;
        }
    }

    /** Forcibly create one editor component. Then set the caret
    * to the given position.
    * @param pos where to place the caret
    * @return always non-<code>null</code> editor
    */
    final CloneableEditor openAt(final PositionRef pos, final int column) {
        final CloneableEditor e = openEditorComponent();
        final Task t = prepareDocument ();
        e.open();
        e.requestVisible();
        
        class Selector implements TaskListener, Runnable {
            public void taskFinished (org.openide.util.Task t2) {
                javax.swing.SwingUtilities.invokeLater (this);
                t2.removeTaskListener (this);
            }
            
            public void run () {
                // #25435. Pane can be null.
                JEditorPane ePane = e.pane;
                if(ePane == null) {
                    return;
                }
                Caret caret = ePane.getCaret();
                if(caret == null) {
                    return;
                }
                
                int offset;
                if (column >= 0) {
                    javax.swing.text.Element el = NbDocument.findLineRootElement (getDocument ());
                    el = el.getElement (el.getElementIndex (pos.getOffset ()));
                    offset = el.getStartOffset () + column;
                    if (offset > el.getEndOffset ()) {
                        offset = el.getEndOffset ();
                    }
                } else {
                    offset = pos.getOffset ();
                }
                
                caret.setDot(offset);
            }
        }
        
        
        t.addTaskListener (new Selector ());
        return e;
    }

    /** Access to lock on operations on the support
    */
    Object getLock () {
        return allEditors;
    }

    /** Accessor to the <code>Listener</code> instance, lazy created on demand.
     * The instance serves as a listener on document, environment
     * and also provides document initialization task for this support.
     * @see Listener */
    private Listener getListener () {
        // Should not need to lock; it is always first
        // called within a synchronized(getLock()) block anyway.
        if(listener == null) {
            listener = new Listener();
        }
        
        return listener;
    }



    /** Default editor kit.
    */
    private static final class PlainEditorKit extends DefaultEditorKit
        implements ViewFactory {
        static final long serialVersionUID =-5788777967029507963L;
	
	PlainEditorKit() {}
	
        /** @return cloned instance
        */
        public Object clone () {
            return new PlainEditorKit ();
        }

        /** @return this (I am the ViewFactory)
        */
        public ViewFactory getViewFactory() {
            return this;
        }

        /** Plain view for the element
        */
        public View create(Element elem) {
            return new WrappedPlainView(elem);
        }
        
        /** Set to a sane font (not proportional!). */
        public void install (JEditorPane pane) {
            super.install (pane);
            pane.setFont (new Font ("Monospaced", Font.PLAIN, pane.getFont().getSize() + 1)); //NOI18N
        }
    }




    /** The listener that this support uses to communicate with
     * document, environment and also temporarilly on undoredo.
     */
    private final class Listener extends Object
    implements ChangeListener, DocumentListener, PropertyChangeListener, Runnable {

	Listener() {}

        /** Stores exception from loadDocument, can be set in run method */
        private IOException loadExc;

        /** Stores temporarilly undo task for reverting prohibited changes.
         * @see CloneableEditorSupport#createUndoTask */
        private Runnable undoTask;

                
        /** Returns exception from loadDocument, caller thread can check
         * it after load task finishes. Returns null if no exception happened.
         * It resets loadExc to null. */
        public IOException checkLoadException() {
            IOException ret = loadExc;
            loadExc = null;
            return ret;
        }


        /** Sets undo task used to revert prohibited change. */
        public void setUndoTask(Runnable undoTask) {
            this.undoTask = undoTask;
        }
        
        /** Schedules reverting(undoing) of prohibited change.
         * Implements <code>ChangeListener</code>.
         * @see #revertUpcomingUndo */
        public void stateChanged(ChangeEvent evt) {
            getUndoRedo().removeChangeListener(this);
            SwingUtilities.invokeLater(undoTask);
            undoTask = null;
        }
        
        /** Gives notification that an attribute or set of attributes changed.
        * @param ev event describing the action
        */
        public void changedUpdate(DocumentEvent ev) {
            //modified(); (bugfix #1492)
        }

        /** Gives notification that there was an insert into the document.
        * @param ev event describing the action
        */
        public void insertUpdate(DocumentEvent ev) {
            notifyModified ();
        }

        /** Gives notification that a portion of the document has been removed.
        * @param ev event describing the action
        */
        public void removeUpdate(DocumentEvent ev) {
            notifyModified ();
        }

        /** Listener to changes in the Env.
        */
        public void propertyChange(PropertyChangeEvent ev) {
            if (Env.PROP_TIME.equals (ev.getPropertyName ())) {
                  // empty new value means to force reload all the time
                  final Date time = (Date)ev.getNewValue ();
                  
                  if (lastSaveTime != -1 
                        && (time == null || time.getTime () > lastSaveTime)
                  ) {
                      //#32777 - set externallyModified to true because file was externally modified
                      externallyModified = true;
                      // post in AWT event thread because of possible dialog popup
                      SwingUtilities.invokeLater(
                          new Runnable() {
                              public void run() {
                                  checkReload(time == null || !isModified());
                              }
                          }
                      );
                  }
             }
            if (Env.PROP_MODIFIED.equals(ev.getPropertyName())) {
                CloneableEditorSupport.this.firePropertyChange(EditorCookie.Observable.PROP_MODIFIED, null, null);
            }
        }


        /** Initialization of the document.
        */
        public void run () {
             synchronized (getLock ()) {
                 /* Remove existing listener before running the loading task
                 * This should prevent firing of insertUpdate() during load (or reload)
                 * which can prevent dedloks that sometimes occured during file reload.
                 */
                 doc.removeDocumentListener(getListener());
                 try {
                    loadExc = null; 
                    LOCAL_LOAD_TASK.set(Boolean.TRUE);
                    loadDocument (kit, doc);
                 } catch (IOException e) {
                     loadExc = e;
                 } finally {
                     LOCAL_LOAD_TASK.set(null);
                 }
                 
                 // opening the document, inform position manager
                 getPositionManager ().documentOpened (doc);

                 // create new description of lines
                 updateLineSet (true);

                 lastSaveTime = System.currentTimeMillis();

                 // Insert before-save undo event to enable unmodifying undo
                 getUndoRedo().undoableEditHappened(
                         new UndoableEditEvent(this, new BeforeSaveEdit(lastSaveTime)));
                 
                 // Start listening on changes in document
                 doc.addDocumentListener(getListener());
            }   
        }
    }


//
// Interfaces to abstract away from the DataSystem and FileSystem level
//

    /** Interface for providing data for the support and also
    * locking the source of data.  
    */
    public static interface Env extends CloneableOpenSupport.Env {
        /** property that is fired when time of the data is changed */
        public static final String PROP_TIME = "time"; // NOI18N

        /** Obtains the input stream.
        * @exception IOException if an I/O error occures
        */
        public InputStream inputStream () throws IOException;

        /** Obtains the output stream.
        * @exception IOException if an I/O error occures
        */
        public OutputStream outputStream () throws IOException;

        /** The time when the data has been modified
        */
        public Date getTime ();

        /** Mime type of the document.
        * @return the mime type to use for the document
        */
        public String getMimeType ();
    }


    /** Generic undoable edit that delegates to the given undoable edit. */
    private class FilterUndoableEdit implements UndoableEdit {

        protected UndoableEdit delegate;

        FilterUndoableEdit() {
        }

        public void undo() throws CannotUndoException {
            if (delegate != null) {
                delegate.undo();
            }
        }

        public boolean canUndo() {
            if (delegate != null) {
                return delegate.canUndo();
            } else {
                return false;
            }
        }

        public void redo() throws CannotRedoException {
            if (delegate != null) {
                delegate.redo();
            }
        }

        public boolean canRedo() {
            if (delegate != null) {
                return delegate.canRedo();
            } else {
                return false;
            }
        }

        public void die() {
            if (delegate != null) {
                delegate.die();
            }
        }

        public boolean addEdit(UndoableEdit anEdit) {
            if (delegate != null) {
                return delegate.addEdit(anEdit);
            } else {
                return false;
            }
        }

        public boolean replaceEdit(UndoableEdit anEdit) {
            if (delegate != null) {
                return delegate.replaceEdit(anEdit);
            } else {
                return false;
            }
        }

        public boolean isSignificant() {
            if (delegate != null) {
                return delegate.isSignificant();
            } else {
                return true;
            }
        }

        public String getPresentationName() {
            if (delegate != null) {
                return delegate.getPresentationName();
            } else {
                return ""; // NOI18N
            }
        }

        public String getUndoPresentationName() {
            if (delegate != null) {
                return delegate.getUndoPresentationName();
            } else {
                return ""; // NOI18N
            }
        }

        public String getRedoPresentationName() {
            if (delegate != null) {
                return delegate.getRedoPresentationName();
            } else {
                return ""; // NOI18N
            }
        }

    }

    /** Undoable edit that is put before the savepoint. Its replaceEdit()
     * method will consume and wrap the edit that precedes the save.
     * If the edit is added to the begining of the queue then
     * the isSignificant() implementation guarantees that the edit
     * will not be removed from the queue.
     * When redone it marks the document as not modified.
     */
    private class BeforeSaveEdit extends FilterUndoableEdit {

        private long saveTime;

        BeforeSaveEdit(long saveTime) {
            this.saveTime = saveTime;
        }

        public boolean replaceEdit(UndoableEdit anEdit) {
            if (delegate == null) {
                delegate = anEdit;
                return true; // signal consumed
            }

            return false;
        }

        public boolean addEdit(UndoableEdit anEdit) {
            if (!(anEdit instanceof BeforeModificationEdit)) {
                /* UndoRedo.addEdit() must not be done lazily
                 * because the edit must be "inserted" before the current one.
                 */
                getUndoRedo().addEdit(new BeforeModificationEdit(saveTime, anEdit));
                return true;
            }
            return false;
        }

        public void redo() {
            super.redo();

            if (saveTime == lastSaveTime) {
                notifyUnmodified();
            }
        }
            
        public boolean isSignificant() {
            return (delegate != null);
        }

    }

    /** Edit that is created by wrapping the given edit.
     * When undone it marks the document as not modified.
     */
    private class BeforeModificationEdit extends FilterUndoableEdit {

        private long saveTime;

        BeforeModificationEdit(long saveTime, UndoableEdit delegate) {
            this.saveTime = saveTime;
            this.delegate = delegate;
        }

        public boolean addEdit(UndoableEdit anEdit) {
            if (delegate == null) {
                delegate = anEdit;
                return true;
            }

            return false;
        }

        public void undo() {
            super.undo();

            if (saveTime == lastSaveTime) {
                notifyUnmodified();
            }
        }

    }
}
