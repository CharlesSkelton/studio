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

import java.awt.datatransfer.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.openide.ErrorManager;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileStatusListener;
import org.openide.filesystems.FileStatusEvent;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileObject;
import org.openide.util.datatransfer.*;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;
import org.openide.util.NbBundle;
import org.openide.util.WeakListener;
import org.openide.util.actions.SystemAction;
import org.openide.nodes.*;

/** Standard node representing a data object.
*
* @author Jaroslav Tulach
*/
public class DataNode extends AbstractNode {

    /** generated Serialized Version UID */
    static final long serialVersionUID = -7882925922830244768L;

    /** DataObject of this node. */
    private DataObject obj;

    /** property change listener */
    private PropL propL;

    /** should file extensions be displayed? */
    private static boolean showFileExtensions = false;

    /** Create a data node for a given data object.
    * The provided children object will be used to hold all child nodes.
    * The name is always set to the base name of the primary file;
    * the display name may instead be set to the base name with extension.
    * @param obj object to work with
    * @param ch children container for the node
    * @see #getShowFileExtensions
    */
    public DataNode (DataObject obj, Children ch) {
        super (ch);
        this.obj = obj;

        propL = new PropL ();

        obj.addPropertyChangeListener (WeakListener.propertyChange (propL, obj));

        super.setName (obj.getName ());
        updateDisplayName ();
    }

    private void updateDisplayName () {
        FileObject prim = obj.getPrimaryFile ();
        String newDisplayName = null;
        
        if (showFileExtensions || obj instanceof DataFolder || obj instanceof DefaultDataObject) {
            newDisplayName = prim.getNameExt();
        } else {
            newDisplayName = prim.getName ();
        }

        if (displayFormat != null)
            setDisplayName (displayFormat.format (new Object[] { newDisplayName }));
        else
            setDisplayName (newDisplayName);
    }

    /** Get the represented data object.
     * @return the data object
    */
    public DataObject getDataObject() {
        return obj;
    }

    /** Changes the name of the node and may also rename the data object.
    * If the object is renamed and file extensions are to be shown,
    * the display name is also updated accordingly.
    *
    * @param name new name for the object
    * @param rename rename the data object?
    * @exception IllegalArgumentException if the rename failed
    */
    public void setName (String name, boolean rename) {
        try {
            if (rename) {
                obj.rename (name);
            }

            super.setName (name);
            if (rename) updateDisplayName ();
        } catch (IOException ex) {
            String msg = null;
            if ((ex.getLocalizedMessage() == null) || 
                (ex.getLocalizedMessage().equals(ex.getMessage()))) {
                msg = NbBundle.getMessage (DataNode.class, "MSG_renameError", getName (), name); // NOI18N
            } else {
                msg = ex.getLocalizedMessage();
            }
            
            RuntimeException e = new IllegalArgumentException();
            ErrorManager.getDefault().copyAnnotation (e, ex);
            ErrorManager.getDefault().annotate (e, ErrorManager.USER, null, msg, null, null);
            throw e;
        }
    }

    /* Rename the data object.
    * @param name new name for the object
    * @exception IllegalArgumentException if the rename failed
    */
    public void setName (String name) {
        setName (name, true);
    }


    /** Get the display name for the node.
     * A filesystem may {@link org.openide.filesystems.FileSystem#getStatus specially alter} this.
     * Subclassers overriding this method should consider the recommendations
     * in {@link DataObject#createNodeDelegate}.
     * @return the desired name
    */
    public String getDisplayName () {
        String s = super.getDisplayName ();

        try {
            s = obj.getPrimaryFile ().getFileSystem ().getStatus ().annotateName (s, obj.files ());
        } catch (FileStateInvalidException e) {
            // no fs, do nothing
        }

        return s;
    }

    /** Get the displayed icon for this node.
     * A filesystem may {@link org.openide.filesystems.FileSystem#getStatus specially alter} this.
     * Subclassers overriding this method should consider the recommendations
     * in {@link DataObject#createNodeDelegate}.
     * @param type the icon type from {@link java.beans.BeanInfo}
     * @return the desired icon
    */
    public java.awt.Image getIcon (int type) {
        java.awt.Image img = super.getIcon (type);

        try {
            img = obj.getPrimaryFile ().getFileSystem ().getStatus ().annotateIcon (img, type, obj.files ());
        } catch (FileStateInvalidException e) {
            // no fs, do nothing
        }

        return img;
    }

    /** Get the displayed icon for this node.
    * A filesystem may {@link org.openide.filesystems.FileSystem#getStatus specially alter} this.
     * Subclassers overriding this method should consider the recommendations
     * in {@link DataObject#createNodeDelegate}.
    * @param type the icon type from {@link java.beans.BeanInfo}
    * @return the desired icon
    */
    public java.awt.Image getOpenedIcon (int type) {
        java.awt.Image img = super.getOpenedIcon(type);

        try {
            img = obj.getPrimaryFile ().getFileSystem ().getStatus ().annotateIcon (img, type, obj.files ());
        } catch (FileStateInvalidException e) {
            // no fs, do nothing
        }

        return img;
    }
    
    public HelpCtx getHelpCtx () {
        return obj.getHelpCtx ();
    }

    /** Indicate whether the node may be renamed.
    * @return tests {@link DataObject#isRenameAllowed}
    */
    public boolean canRename () {
        return obj.isRenameAllowed ();
    }

    /** Indicate whether the node may be destroyed.
     * @return tests {@link DataObject#isDeleteAllowed}
     */
    public boolean canDestroy () {
        return obj.isDeleteAllowed ();
    }

    /* Destroyes the node
    */
    public void destroy () throws IOException {
        if (obj.isDeleteAllowed ()) {
            obj.delete ();
        }
        super.destroy ();
    }

    /* Returns true if this object allows copying.
    * @returns true if this object allows copying.
    */
    public boolean canCopy () {
        return obj.isCopyAllowed ();
    }

    /* Returns true if this object allows cutting.
    * @returns true if this object allows cutting.
    */
    public boolean canCut () {
        return obj.isMoveAllowed ();
    }

    /** This method returns null to signal that actions
    * provide by DataLoader.getActions should be returned from 
    * method getActions. If overriden to provide some actions,
    * then these actions will be preferred to the loader's ones.
    *
    * @return null
    */
    protected SystemAction[] createActions () {
        return null;
    }

    /** Get actions for this data object.
    * @see DataLoader#getActions
    * @return array of actions or <code>null</code>
    */
    public SystemAction[] getActions () {
        if (systemActions == null) {
            systemActions = createActions ();
        }

        if (systemActions != null) {
            return systemActions;
        }

        return obj.getLoader ().getActions ();
    }

    /** Get default action. In the current implementation the 
    *<code>null</code> is returned in case the underlying data 
    * object is a template. The templates should not have any default 
    * action.
    * @return no action if the underlying data object is a template. 
    *    Otherwise the abstract node's default action is returned, if <code>null</code> then
    *    the first action returned from getActions () method is used.
    */
    public SystemAction getDefaultAction () {
        if (obj.isTemplate ()) {
            return null;
        } else {
            SystemAction action = super.getDefaultAction ();
            if (action != null) {
                return action;
            }
            SystemAction[] arr = getActions ();
            if (arr != null && arr.length > 0) {
                return arr[0];
            }
            return null;
        }
    }

    /** Get a cookie.
     * First of all {@link DataObject#getCookie} is
    * called. If it produces non-<code>null</code> result, that is returned.
    * Otherwise the superclass is tried.
     * Subclassers overriding this method should consider the recommendations
     * in {@link DataObject#createNodeDelegate}.
    *
    * @return the cookie or <code>null</code>
    */
    public Node.Cookie getCookie (Class cl) {
        Node.Cookie c = obj.getCookie (cl);
        if (c != null) {
            return c;
        } else {
            return super.getCookie (cl);
        }
    }

    /* Initializes sheet of properties. Allow subclasses to
    * overwrite it.
    * @return the default sheet to use
    */
    protected Sheet createSheet () {
        Sheet s = Sheet.createDefault ();
        Sheet.Set ss = s.get (Sheet.PROPERTIES);

        Node.Property p;

        p = createNameProperty (obj);
        ss.put (p);

        if ( !getDataObject().getPrimaryFile().isReadOnly() )
            try {            
                p = new PropertySupport.Reflection (
                        obj, Boolean.TYPE, "isTemplate", "setTemplate" // NOI18N
                    );
                p.setName (DataObject.PROP_TEMPLATE);
                p.setDisplayName (DataObject.getString("PROP_template"));
                p.setShortDescription (DataObject.getString("HINT_template"));
                ss.put (p);
            } catch (Exception ex) {
                throw new InternalError ();
            }

        /*
        // Add a property with a list of all contained files, sorted by primary and then alphabetically:
        ss.put (new PropertySupport.ReadOnly (DataObject.PROP_FILES, String[].class, "Files", "Files contained in this object.") { // [PENDING] I18N
          public Object getValue () {
            Set files = obj.files ();
            String[] toret = new String[files.size ()];
            int i = 0; for (Iterator it = files.iterator (); it.hasNext (); i++)
              toret[i] = getNameExt ((FileObject) it.next ());
            final String pfilename = getNameExt (obj.getPrimaryFile ());
            Arrays.sort (toret, new Comparator () {
              public int compare (Object o1, Object o2) {
                String fname1 = (String) o1;
                String fname2 = (String) o2;
                if (fname1.equals (pfilename))
                  return -1;
                else if (fname2.equals (pfilename))
                  return 1;
                else
                  return fname1.compareTo (fname2);
              }
            });
            return toret;
          }
          private String getNameExt (FileObject fo) {
            if (fo.isRoot ()) return "<root of filesystem>"; // [PENDING] I18N
            String name = fo.getName ();
            String ext = fo.getExt ();
            return ext == null || ext.equals ("") ? name : name + '.' + ext;
          }
    });
        */

        return s;
    }
    
    /** Copy this node to the clipboard.
    *
    * @return {@link org.openide.util.datatransfer.ExTransferable.Single} with one copy flavor
    * @throws IOException if it could not copy
    * @see NodeTransfer
    */
    public Transferable clipboardCopy () throws IOException {
        ExTransferable t = ExTransferable.create (super.clipboardCopy ());
        t.put (LoaderTransfer.transferable (
            getDataObject (), 
            LoaderTransfer.CLIPBOARD_COPY)
        );
        return t;
    }

    /** Cut this node to the clipboard.
    *
    * @return {@link org.openide.util.datatransfer.ExTransferable.Single} with one cut flavor
    * @throws IOException if it could not cut
    * @see NodeTransfer
    */
    public Transferable clipboardCut () throws IOException {
        ExTransferable t = ExTransferable.create (super.clipboardCut ());
        t.put (LoaderTransfer.transferable (
            getDataObject (), 
            LoaderTransfer.CLIPBOARD_CUT)
        );
        return t;
    }
    
    /** Creates a name property for given data object.
    */
    static Node.Property createNameProperty (final DataObject obj) {
        Node.Property p = new PropertySupport.ReadWrite (
                              DataObject.PROP_NAME,
                              String.class,
                              DataObject.getString("PROP_name"),
                              DataObject.getString("HINT_name")
                          ) {
                              public Object getValue () {
                                  return obj.getName();
                              }

                              public void setValue (Object val) throws IllegalAccessException,
                                  IllegalArgumentException, InvocationTargetException {
                                  if (!canWrite())
                                      throw new IllegalAccessException();
                                  if (!(val instanceof String))
                                      throw new IllegalArgumentException();

                                  try {
                                      obj.rename ((String)val);
                                  } catch (IOException ex) {
                                      String msg = null;
                                      if ((ex.getLocalizedMessage() == null) || 
                                          (ex.getLocalizedMessage().equals(ex.getMessage()))) {
                                          msg = NbBundle.getMessage (DataNode.class, "MSG_renameError", obj.getName(), val); // NOI18N
                                      } else {
                                          msg = ex.getLocalizedMessage();
                                      }
                                      ErrorManager.getDefault().annotate (ex, ErrorManager.USER, null, msg, null, null);
                                      throw new InvocationTargetException(ex);
                                  }
                              }

                              public boolean canWrite () {
                                  return obj.isRenameAllowed();
                              }
                          };

        return p;
    }

    /** Support for firing property change.
    * @param ev event describing the change
    */
    void fireChange (PropertyChangeEvent ev) {
        if (DataFolder.PROP_CHILDREN.equals (ev.getPropertyName ())) {
            // the node is not interested in children changes
            return;
        }

        if (DataObject.PROP_PRIMARY_FILE.equals (ev.getPropertyName ())) {
            // the node is not interested in children changes
            propL.updateStatusListener ();
            setName (obj.getName (), false);
            return;
        }
        
        if (DataObject.PROP_NAME.equals(ev.getPropertyName())) {
            super.setName (obj.getName ());
            updateDisplayName();
            return;
        }
        if (DataObject.PROP_COOKIE.equals(ev.getPropertyName())) {
            fireCookieChange ();
        } else {
            firePropertyChange (ev.getPropertyName (), ev.getOldValue (), ev.getNewValue ());
        }
        
        // if the DataOjbect is not valid the node should be
        // removed
        if (DataObject.PROP_VALID.equals (ev.getPropertyName ())) {
            Object newVal = ev.getNewValue();
            if ((newVal instanceof Boolean)&&(! ((Boolean)newVal).booleanValue())) {
                fireNodeDestroyed();
            }
        }
    }

    /** Handle for location of given data object.
    * @return handle that remembers the data object.
    */
    public Handle getHandle () {
        return new ObjectHandle(obj, obj.isValid() ? (this != obj.getNodeDelegate()) : /* to be safe */ true);
    }

    /** Access method to fire icon change.
    */
    final void fireChangeAccess (boolean icon, boolean name) {
        if (name) {
            fireDisplayNameChange (null, null);
        }
        if (icon) {
            fireIconChange ();
        }
    }

    /** Determine whether file extensions should be shown by default.
    * By default, no.
    * @return <code>true</code> if so
    */
    public static boolean getShowFileExtensions () {
        return showFileExtensions;
    }

    /** Set whether file extensions should be shown by default.
    * @param s <code>true</code> if so
    */
    public static void setShowFileExtensions (boolean s) {
        boolean refresh = ( showFileExtensions != s );
        showFileExtensions = s;
        
        if ( refresh ) {
            // refresh current nodes display name
            RequestProcessor.postRequest(new Runnable () {
                public void run () { 
                    Iterator it = DataObjectPool.getPOOL().getActiveDataObjects();
                    while ( it.hasNext() ) {
                        DataObject obj = ((DataObjectPool.Item)it.next()).getDataObjectOrNull();
                        if ( obj != null && obj.getNodeDelegate() instanceof DataNode ) {
                            ((DataNode)obj.getNodeDelegate()).updateDisplayName();            
                        }
                    }        
                }
            }, 300, Thread.MIN_PRIORITY);                    
        }        
        
    }
    
    /** Request processor task to update a bunch of names/icons.
     * Potentially faster to do many nodes at once; see #16478.
     */
    private static RequestProcessor.Task refreshNamesIconsTask = null;
    /** nodes which should be refreshed */
    private static Set refreshNameNodes = null; // Set<DataNode>
    private static Set refreshIconNodes = null; // Set<DataNode>
    /** whether the task is current scheduled and will still look in above sets */
    private static boolean refreshNamesIconsRunning = false;
    private static final Object refreshNameIconLock = "DataNode.refreshNameIconLock"; // NOI18N
    
    /** Property listener on data object that delegates all changes of
    * properties to this node.
    */
    private class PropL extends Object
        implements PropertyChangeListener, FileStatusListener, Runnable {
        /** weak version of this listener */
        private FileStatusListener weakL;
        /** previous filesystem we were attached to */
        private FileSystem previous;

        public PropL () {
            updateStatusListener ();
        }

        public void propertyChange (PropertyChangeEvent ev) {
            fireChange (ev);
        }

        /** Updates listening on a status of filesystem.
        */
        private void updateStatusListener () {
            if (previous != null) {
                previous.removeFileStatusListener (weakL);
            }
            try {
                previous = obj.getPrimaryFile ().getFileSystem ();

                if (weakL == null) {
                    weakL = WeakListener.fileStatus (this, null);
                }

                previous.addFileStatusListener (weakL);
            } catch (FileStateInvalidException ex) {
                previous = null;
            }
        }

        /** Notifies listener about change in annotataion of a few files.
        * @param ev event describing the change
        */
        public void annotationChanged (FileStatusEvent ev) {
            // #16541: listen for changes in both primary and secondary files
            boolean thisChanged = false;
            Iterator it = obj.files().iterator();
            while (it.hasNext()) {
                FileObject fo = (FileObject)it.next();
                if (ev.hasChanged(fo)) {
                    thisChanged = true;
                    break;
                }
            }
            if (thisChanged) {
                // #12368: fire display name & icon changes asynch
                synchronized (refreshNameIconLock) {
                    boolean post = false;
                    if (ev.isNameChange()) {
                        if (refreshNameNodes == null) {
                            refreshNameNodes = new HashSet();
                        }
                        post |= refreshNameNodes.add(DataNode.this);
                    }
                    if (ev.isIconChange()) {
                        if (refreshIconNodes == null) {
                            refreshIconNodes = new HashSet();
                        }
                        post |= refreshIconNodes.add(DataNode.this);
                    }
                    if (post && !refreshNamesIconsRunning) {
                        refreshNamesIconsRunning = true;
                        if (refreshNamesIconsTask == null) {
                            refreshNamesIconsTask = RequestProcessor.postRequest(this);
                        } else {
                            // Should be OK even if it is running right now.
                            // (Cf. RequestProcessorTest.testScheduleWhileRunning.)
                            refreshNamesIconsTask.schedule(0);
                        }
                    }
                }
            }
        }
        
        /** Refreshes names and icons for a whole batch of data nodes at once.
         */
        public void run() {
            DataNode[] _refreshNameNodes, _refreshIconNodes;
            synchronized (refreshNameIconLock) {
                if (refreshNameNodes != null) {
                    _refreshNameNodes = (DataNode[])refreshNameNodes.toArray(new DataNode[refreshNameNodes.size()]);
                    refreshNameNodes.clear();
                } else {
                    _refreshNameNodes = new DataNode[0];
                }
                if (refreshIconNodes != null) {
                    _refreshIconNodes = (DataNode[])refreshIconNodes.toArray(new DataNode[refreshIconNodes.size()]);
                    refreshIconNodes.clear();
                } else {
                    _refreshIconNodes = new DataNode[0];
                }
                refreshNamesIconsRunning = false;
            }
            for (int i = 0; i < _refreshNameNodes.length; i++) {
                _refreshNameNodes[i].fireChangeAccess(false, true);
            }
            for (int i = 0; i < _refreshIconNodes.length; i++) {
                _refreshIconNodes[i].fireChangeAccess(true, false);
            }
        }
        
    }

    /** Handle for data object nodes */
    private static class ObjectHandle extends Object implements Handle {
        private FileObject obj;
        private boolean clone;

        static final long serialVersionUID =6616060729084681518L;


        public ObjectHandle (DataObject obj, boolean clone) {
            this.obj = obj.getPrimaryFile ();
            this.clone = clone;
        }

        public Node getNode () throws DataObjectNotFoundException {
            Node n = DataObject.find (obj).getNodeDelegate ();
            return clone ? n.cloneNode () : n;
        }
    }
}
