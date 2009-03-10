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

import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.lang.ref.Reference;

import javax.swing.text.*;
import org.openide.DialogDisplayer;

import org.openide.actions.*;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.*;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeListener;
import org.openide.loaders.*;
import org.openide.windows.*;
import org.openide.util.WeakListener;
import org.openide.util.NbBundle;

/** Support for associating an editor and a Swing {@link Document} to a data object.
* 
*
* @author Jaroslav Tulach
*/
public class DataEditorSupport extends CloneableEditorSupport {
    /** Which data object we are associated with */
    private final DataObject obj;
    /** listener to asociated node's events */
    private NodeListener nodeL;
    
    /** Editor support for a given data object. The file is taken from the
    * data object and is updated if the object moves or renames itself.
    * @param obj object to work with
    * @param env environment to pass to 
    */
    public DataEditorSupport (DataObject obj, CloneableEditorSupport.Env env) {
        super (env);
        this.obj = obj;
    }
    
    /** Getter of the data object that this support is associated with.
    * @return data object passed in constructor
    */
    public final DataObject getDataObject () {
        return obj;
    }

    /** Message to display when an object is being opened.
    * @return the message or null if nothing should be displayed
    */
    protected String messageOpening () {
        return NbBundle.getMessage (DataEditorSupport.class , "CTL_ObjectOpen", // NOI18N
            obj.getName(),
            obj.getPrimaryFile().toString()
        );
    }
    

    /** Message to display when an object has been opened.
    * @return the message or null if nothing should be displayed
    */
    protected String messageOpened () {
        return NbBundle.getMessage (DataEditorSupport.class, "CTL_ObjectOpened", // NOI18N
            obj.getName (),
            obj.getPrimaryFile ().toString ()
        );
    }

    /** Constructs message that should be displayed when the data object
    * is modified and is being closed.
    *
    * @return text to show to the user
    */
    protected String messageSave () {
        return NbBundle.getMessage (
            DataEditorSupport.class,
            "MSG_SaveFile", // NOI18N
            obj.getName()
        );
    }
    
    /** Constructs message that should be used to name the editor component.
    *
    * @return name of the editor
    */
    protected String messageName () {
        if (! obj.isValid()) return ""; // NOI18N
        String name = obj.getNodeDelegate().getDisplayName();

        int version = 3;
        if (isModified ()) {
            if (obj.getPrimaryFile ().isReadOnly ()) {
                version = 2;
            } else {
                version = 1;
            }
        } else
        if (obj.getPrimaryFile ().isReadOnly ()) {
            version = 0;
        }

        return NbBundle.getMessage (DataEditorSupport.class, "LAB_EditorName",
		new Integer (version), name );
    }
    
    /** Text to use as tooltip for component.
    *
    * @return text to show to the user
    */
    protected String messageToolTip () {
        // update tooltip
        FileObject fo = obj.getPrimaryFile ();
        
        try {
            File f = FileUtil.toFile (fo);
            if (f!=null) {
                return f.getAbsolutePath ();
            } else {
                return NbBundle.getMessage (DataEditorSupport.class, "LAB_EditorToolTip", new Object[] {
                    fo.getPath(),
                    fo.getFileSystem ().getDisplayName ()
                });
            }
        } catch (FileStateInvalidException fsie) {
            return fo.getPath();
        }
    }
    
    /** Annotates the editor with icon from the data object and also sets 
     * appropriate selected node. But only in the case the data object is valid.
     * This implementation also listen to display name and icon chamges of the
     * node and keeps editor top component up-to-date. If you override this
     * method and not call super, please note that you will have to keep things
     * synchronized yourself. 
     *
     * @param editor the editor that has been created and should be annotated
     */
    protected void initializeCloneableEditor (CloneableEditor editor) {
        // Prevention to bug similar to #17134. Don't call getNodeDelegate
        // on invalid data object. Top component should be discarded later.
        if(obj.isValid()) {
            Node ourNode = obj.getNodeDelegate();
            editor.setActivatedNodes (new Node[] { ourNode });
            editor.setIcon(ourNode.getIcon (java.beans.BeanInfo.ICON_COLOR_16x16));
            NodeListener nl = new DataNodeListener(editor);
            ourNode.addNodeListener(WeakListener.node(nl, ourNode));
            nodeL = nl;
        }
    }

    /** Called when closed all components. Overrides superclass method,
     * also unregisters listening on node delegate. */
    protected void notifyClosed() {
        // #27645 All components were closed, unregister weak listener on node.
        nodeL = null;
        
        super.notifyClosed();
    }
    
    /** Let's the super method create the document and also annotates it
    * with Title and StreamDescription properities.
    *
    * @param kit kit to user to create the document
    * @return the document annotated by the properties
    */
    protected StyledDocument createStyledDocument (EditorKit kit) {
        StyledDocument doc = super.createStyledDocument (kit);
            
        // set document name property
        doc.putProperty(javax.swing.text.Document.TitleProperty,
            obj.getPrimaryFile ().getPath()
        );
        // set dataobject to stream desc property
        doc.putProperty(javax.swing.text.Document.StreamDescriptionProperty,
            obj
        );
        
        return doc;
    }

    /** Checks whether is possible to close support components.
     * Overrides superclass method, adds checking
     * for read-only property of saving file and warns user in that case. */
    protected boolean canClose() {
        if(env().isModified() && isEnvReadOnly()) {
            Object result = DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Confirmation(
                    NbBundle.getMessage(DataEditorSupport.class,
                        "MSG_FileReadOnlyClosing", 
                        new Object[] {((Env)env).getFileImpl().getNameExt()}),
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.WARNING_MESSAGE
            ));

            return result == NotifyDescriptor.OK_OPTION;
        }
        
        return super.canClose();
    }

    /** Saves document. Overrides superclass method, adds checking
     * for read-only property of saving file and warns user in that case. */
    public void saveDocument() throws IOException {
        if(env().isModified() && isEnvReadOnly()) {
            DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(
                    NbBundle.getMessage(DataEditorSupport.class,
                        "MSG_FileReadOnlySaving", 
                        new Object[] {((Env)env).getFileImpl().getNameExt()}),
                NotifyDescriptor.WARNING_MESSAGE
            ));
            return;
        }
        super.saveDocument();
    }

    /** Indicates whether the <code>Env</code> is read only. */
    boolean isEnvReadOnly() {
        CloneableEditorSupport.Env env = env();
        return env instanceof Env && ((Env)env).getFileImpl().isReadOnly();
    }
    
    /** Getter for data object associated with this 
    * data object.
    */
    final DataObject getDataObjectHack () {
        return obj;
    }
    
    /** Environment that connects the data object and the CloneableEditorSupport.
    */
    public static abstract class Env extends OpenSupport.Env 
    implements CloneableEditorSupport.Env, java.io.Serializable,
    PropertyChangeListener, VetoableChangeListener {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -2945098431098324441L;

        /** The file object this environment is associated to.
        * This file object can be changed by a call to refresh file.
        */
        private transient FileObject fileObject;

        /** Lock acquired after the first modification and used in save.
        * Transient => is not serialized.
        */
        private transient FileLock fileLock;
        
        /** Constructor.
        * @param obj this support should be associated with
        */
        public Env (DataObject obj) {
            super (obj);
        }
        
        /** Getter for the file to work on.
        * @return the file
        */
        private FileObject getFileImpl () {
            // updates the file if there was a change
	    changeFile();
            return fileObject;
        }
        
        /** Getter for file associated with this environment.
        * @return the file input/output operation should be performed on
        */
        protected abstract FileObject getFile ();

        /** Locks the file.
        * @return the lock on the file getFile ()
        * @exception IOException if the file cannot be locked
        */
        protected abstract FileLock takeLock () throws IOException;
                
        /** Method that allows subclasses to notify this environment that
        * the file associated with this support has changed and that 
        * the environment should listen on modifications of different 
        * file object.
        */
        protected final void changeFile () {

            FileObject newFile = getFile ();
            
            if (newFile.equals (fileObject)) {
                // the file has not been updated
                return;
            }
            
            boolean lockAgain;
            if (fileLock != null) {
                fileLock.releaseLock ();
                lockAgain = true;
            } else {
                lockAgain = false;
            }

            fileObject = newFile;
            fileObject.addFileChangeListener (new EnvListener (this));

            if (lockAgain) { // refresh lock
                try {
                    fileLock = takeLock ();
                } catch (IOException e) {
                    ErrorManager.getDefault ().notify (
                	    ErrorManager.INFORMATIONAL, e);
                }
            }
            
        }
        
        
        /** Obtains the input stream.
        * @exception IOException if an I/O error occures
        */
        public InputStream inputStream() throws IOException {
            InputStream is = getFileImpl ().getInputStream ();
            return is;
        }
        
        /** Obtains the output stream.
        * @exception IOException if an I/O error occures
        */
        public OutputStream outputStream() throws IOException {
            return getFileImpl ().getOutputStream (fileLock);
        }
        
        /** The time when the data has been modified
        */
        public Date getTime() {
            // #32777 - refresh file object and return always the actual time
            getFileImpl().refresh(false);
            return getFileImpl ().lastModified ();
        }
        
        /** Mime type of the document.
        * @return the mime type to use for the document
        */
        public String getMimeType() {
            return getFileImpl ().getMIMEType ();
        }
        
        /** First of all tries to lock the primary file and
        * if it succeeds it marks the data object modified.
         * <p><b>Note: There is a contract (better saying a curse)
         * that this method has to call {@link #takeLock} method
         * in order to keep working some special filesystem's feature.
         * See <a href="http://www.netbeans.org/issues/show_bug.cgi?id=28212">issue #28212</a></b>.
        *
        * @exception IOException if the environment cannot be marked modified
        *   (for example when the file is readonly), when such exception
        *   is the support should discard all previous changes
         * @see  org.openide.filesystems.FileObject#isReadOnly
        */
        public void markModified() throws java.io.IOException {
            // XXX This shouldn't be here. But it is due to the 'contract',
            // see javadoc to this method.
            if (fileLock == null || !fileLock.isValid()) {
                fileLock = takeLock ();
            }
            
            if(getFileImpl().isReadOnly()) {
                if(fileLock != null && fileLock.isValid()) {
                    fileLock.releaseLock();
                }
                throw new IOException("File " // NOI18N
                    + getFileImpl().getNameExt() + " is read-only!"); // NOI18N
            }

            this.getDataObject ().setModified (true);
        }
        
        /** Reverse method that can be called to make the environment 
        * unmodified.
        */
        public void unmarkModified() {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.releaseLock();
            }
            
            this.getDataObject ().setModified (false);
        }
        
        /** Called from the EnvListener
        * @param expected is the change expected
        * @param time of the change
        */
        final void fileChanged (boolean expected, long time) {
            if (expected) {
                // newValue = null means do not ask user whether to reload
                firePropertyChange (PROP_TIME, null, null);
            } else {
                firePropertyChange (PROP_TIME, null, new Date (time));
            }
        }

        /** Called from the <code>EnvListener</code>.
         * The components are going to be closed anyway and in case of
         * modified document its asked before if to save the change. */
        private void fileRemoved(boolean canBeVetoed) {
            if (canBeVetoed) {
                try {
                    // Causes the 'Save' dialog to show if necessary.
                    fireVetoableChange(Env.PROP_VALID, Boolean.TRUE, Boolean.FALSE);
                } catch(PropertyVetoException pve) {
                    // Ignore it and close anyway. File doesn't exist anymore.
                }
            }
            
            // Closes the components.
            firePropertyChange(Env.PROP_VALID, Boolean.TRUE, Boolean.FALSE);            
        }
    } // end of Env
    
    /** Listener on file object that notifies the Env object
    * that a file has been modified.
    */
    private static final class EnvListener extends FileChangeAdapter {
        /** Reference (Env) */
        private Reference env;
        
        /** @param env environement to use
        */
        public EnvListener (Env env) {
            this.env = new java.lang.ref.WeakReference (env);
        }


        /** Handles <code>FileObject</code> deletion event. */
        public void fileDeleted(FileEvent fe) {
            Env env = (Env)this.env.get();
            FileObject fo = fe.getFile();
            if(env == null || env.getFileImpl() != fo) {
                // the Env change its file and we are not used
                // listener anymore => remove itself from the list of listeners
                fo.removeFileChangeListener(this);
                return;
            }
            
            fo.removeFileChangeListener(this);
            
            // #30210 - when edited file was deleted the "Do you want to save changes"
            // dialog should not be shown 
            env.fileRemoved(false);
            fo.addFileChangeListener(this);
        }
        
        /** Fired when a file is changed.
        * @param fe the event describing context where action has taken place
        */
        public void fileChanged(FileEvent fe) {
            Env env = (Env)this.env.get ();
            if (env == null || env.getFileImpl () != fe.getFile ()) {
                // the Env change its file and we are not used
                // listener anymore => remove itself from the list of listeners
                fe.getFile ().removeFileChangeListener (this);
                return;
            }

            // #16403. Added handling for virtual property of the file.
            if(fe.getFile().isVirtual()) {
                // Remove file event coming as consequence of this change.
                fe.getFile().removeFileChangeListener(this);
                // File doesn't exist on disk -> simulate env is invalid,
                // even the fileObject could be valid, see VCS FS.
                env.fileRemoved(true);
                fe.getFile().addFileChangeListener(this);
            } else {
                env.fileChanged (fe.isExpected (), fe.getTime ());
            }
        }
                
    }
    
    /** Listener on node representing asociated data object, listens to the
     * property changes of the node and updates state properly
     */
    private final class DataNodeListener extends NodeAdapter {
        /** Asociated editor */
        CloneableEditor editor;
        
        DataNodeListener (CloneableEditor editor) {
            this.editor = editor;
        }
        
        public void propertyChange (java.beans.PropertyChangeEvent ev) {
            if (Node.PROP_DISPLAY_NAME.equals(ev.getPropertyName())) {
                updateTitles();
            }
            if (Node.PROP_ICON.equals(ev.getPropertyName())) {
                if (obj.isValid()) {
                    editor.setIcon(obj.getNodeDelegate().getIcon (java.beans.BeanInfo.ICON_COLOR_16x16));
                }
            }
        }
        
    } // end of DataNodeListener
    
}
