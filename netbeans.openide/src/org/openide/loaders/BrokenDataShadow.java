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

package org.openide.loaders;

import java.io.*;
import java.text.MessageFormat;
import java.lang.reflect.*;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.EventObject;

import org.openide.filesystems.*;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.nodes.PropertySupport;
import org.openide.util.HelpCtx;
import org.openide.nodes.Children;
import org.openide.util.actions.*;
import org.openide.actions.*;

/** For representing data shadows with broken link to original file.
* Since 1.13 it extends MultiDataObject.
* @author Ales Kemr
*/
final class BrokenDataShadow extends MultiDataObject {
    /** Name of filesystem of original fileobject */
    private String origFSName;    
    
    /** Name of original fileobject */
    private String origFOName;
        
    /** Constructs new broken data shadow for given primary file.
    *
    * @param fo the primary file
    * @param loader the loader that created the object
    */
    public BrokenDataShadow (
        FileObject fo, MultiFileLoader loader
    ) throws DataObjectExistsException {        
        super (fo, loader);                                
        
        try {
            BufferedReader ois = new BufferedReader (new InputStreamReader (fo.getInputStream ()));
            origFOName = ois.readLine ();
            origFSName = ois.readLine ();
            ois.close();
        } catch (IOException e) {
        }
        enqueueBrokenDataShadow(this);
    }
        
    /** Set of all DataShadows */
    private static Set allDataShadows;
    /** ReferenceQueue for collected DataShadows */
    private static ReferenceQueue rqueue;
    
    private static final long serialVersionUID = -3046981691235483810L;
    
    /** Getter for the Set that contains all DataShadows. */
    private static Set getDataShadowsSet() {
        if (allDataShadows == null) {
            synchronized (BrokenDataShadow.class) {
                if (allDataShadows == null) {
                    allDataShadows = new HashSet();
                }
            }
        }
        
        return allDataShadows;
    }
    
    /** Getter for the ReferenceQueue that contains WeakReferences
     * for discarded DataShadows
     */
    private static ReferenceQueue getRqueue() {
        if (rqueue == null) {
            synchronized (BrokenDataShadow.class) {
                if (rqueue == null) {
                    rqueue = new ReferenceQueue();
                }
            }
        }
        
        return rqueue;
    }
    
    /** Removes WeakReference of collected DataShadows. */
    private static void checkQueue() {
        if (rqueue == null) {
            return;
        }
        
        Reference ref = rqueue.poll();
        while (ref != null) {
           getDataShadowsSet().remove(ref);
           ref = rqueue.poll();
        }
    }
    
    private static synchronized void enqueueBrokenDataShadow(BrokenDataShadow ds) {
        checkQueue();
        getDataShadowsSet().add(DataShadow.createReference(ds, getRqueue()));
    }

    /** @return all active DataShadows or null */
    private static synchronized List getAllDataShadows() {
        Set allShadows = allDataShadows;
        if ((allShadows == null) || allShadows.isEmpty()) {
            return null;
        }
        
        List ret = new ArrayList(allShadows.size());
        Iterator it = allShadows.iterator();
        while (it.hasNext()) {
            Reference ref = (Reference) it.next();
            Object shadow = ref.get();
            if (shadow != null) {
                ret.add(shadow);
            }
        }
        
        return ret;
    }
    
    /** Checks whether a change of the given dataObject
     * does not revalidate a BrokenDataShadow
     */
    static void checkValidity(EventObject ev) {
        List all = getAllDataShadows();
        if (all == null) {
            return;
        }
        
        int size = all.size();
        for (int i = 0; i < size; i++) {
            Object obj = all.get(i);
            ((BrokenDataShadow) obj).refresh();
        }
    }
    
    /** Constructs new broken data shadow for given primary file.
    * @param fo the primary file
    */
    private BrokenDataShadow (FileObject fo) throws DataObjectExistsException {
        this (fo, (MultiFileLoader)DataLoaderPool.getShadowLoader ());
    }
    
    /* Getter for delete action.
    * @return true if the object can be deleted
    */
    public boolean isDeleteAllowed() {
        return !getPrimaryFile ().isReadOnly ();
    }

    /* Check if link to original file is still broken */    
    public void refresh() {
        try {
            if (DataShadow.checkOriginal (origFOName, origFSName, 
                getPrimaryFile().getFileSystem()) != null) {
                /* Link to original file was repaired */
                this.setValid(false);
            }
        } catch (Exception e) {
        }
    }
    
    /* Getter for copy action.
    * @return true if the object can be copied
    */
    public boolean isCopyAllowed() {
        return true;
    }

    /* Getter for move action.
    * @return true if the object can be moved
    */
    public boolean isMoveAllowed() {
        return !getPrimaryFile ().isReadOnly ();
    }

    /* Getter for rename action.
    * @return true if the object can be renamed
    */
    public boolean isRenameAllowed () {
        return !getPrimaryFile ().isReadOnly ();
    }

    /* Help context for this object.
    * @return help context
    */
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    /* Creates node delegate.
    */    
    protected Node createNodeDelegate () {
        return new BrokenShadowNode (this);
    }
    
    /** Node for a broken shadow object. */
    private static final class BrokenShadowNode extends DataNode {
        
        /** message to create name of node */
        private static MessageFormat format;
        
        /** the sheet computed for this node or null */
        private Sheet sheet;

        private static final String ICON_NAME = "org/openide/resources/brokenShadow"; // NOI18N

        /** Create a node.
         * @param broken data shadow
         */        
        public BrokenShadowNode (BrokenDataShadow par) {            
            super (par,Children.LEAF);
            setIconBase(ICON_NAME);
        }
        
        /** Get the display name for the node.
         * A filesystem may {@link org.openide.filesystems.FileSystem#getStatus specially alter} this.
         * @return the desired name
        */
        public String getDisplayName () {
            if (format == null) {
                format = new MessageFormat (DataObject.getString ("FMT_brokenShadowName"));
            }
            return format.format (createArguments ());
        }
        
        /** Create actions for this data object.
        * @return array of actions or <code>null</code>
        */    
        protected SystemAction[] createActions () {
            return new SystemAction[] {
                        SystemAction.get (CutAction.class),
                        SystemAction.get (CopyAction.class),
                        SystemAction.get (PasteAction.class),
                        null,
                        SystemAction.get (DeleteAction.class),
                        null,
                        SystemAction.get (ToolsAction.class),
                        SystemAction.get (PropertiesAction.class)
                    };
        }
    
        /** Returns modified properties of the original node.
        * @return property sets 
        */
        public PropertySet[] getPropertySets () {
            if (sheet == null) {
                sheet = cloneSheet ();                
            }
            return sheet.toArray ();
        }
        
        /** Clones the property sheet of original node.
        */
        private Sheet cloneSheet () {
            PropertySet[] sets = super.getPropertySets ();

            Sheet s = new Sheet ();
            for (int i = 0; i < sets.length; i++) {
                Sheet.Set ss = new Sheet.Set ();
                ss.put (sets[i].getProperties ());
                ss.setName (sets[i].getName ());
                ss.setDisplayName (sets[i].getDisplayName ());
                ss.setShortDescription (sets[i].getShortDescription ());

                // modifies the set if it contains name of object property
                modifySheetSet (ss);
                
                s.put (ss);
            }

            return s;
        }
        
        /** Modifies the sheet set to contain name of property and name of
        * original object.
        */
        private void modifySheetSet (Sheet.Set ss) {
            Property p = ss.remove (DataObject.PROP_NAME);
            if (p != null) {
                p = new PropertySupport.Name (this);
                ss.put (p);

                p = new Name ();
                ss.put (p);
                
                p = new FileSystemProperty ();
                ss.put (p);
            }
        }
        
        /** Creates arguments for given shadow node */
        private Object[] createArguments () {
            return new Object[] {
                       getDataObject().getName ()
                   };
        }    
    
        /** Class for original name property of broken link
        */
        private final class Name extends PropertySupport.ReadWrite {
            
            public Name () {
                super (
                    "BrokenLink", // NOI18N
                    String.class,
                    DataObject.getString ("PROP_brokenShadowOriginalName"),
                    DataObject.getString ("HINT_brokenShadowOriginalName")
                );
            }

            /* Getter */
            public Object getValue () {
                BrokenDataShadow bds = (BrokenDataShadow)getDataObject();
                return bds.origFOName;
            }
            
            /* Does nothing, property is readonly */
            public void setValue (Object val) {                
            }

            /* Property is readonly */
            public boolean canWrite () {
                return false;
            }
        }                
        
        /** Class for original filesystem name property of broken link
        */
        private final class FileSystemProperty extends PropertySupport.ReadOnly {
            
            public FileSystemProperty () {
                super (
                    "BrokenLinkFileSystem", // NOI18N
                    String.class,
                    DataObject.getString ("PROP_brokenShadowFileSystem"),
                    DataObject.getString ("HINT_brokenShadowFileSystem")
                );
            }

            /* Getter */
            public Object getValue () {
                BrokenDataShadow bds = (BrokenDataShadow)getDataObject();
                return bds.origFSName;
            }                        
        }
    }
}
