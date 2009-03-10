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

package org.openide.explorer;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;

import java.io.IOException;
import java.util.HashMap;
import java.lang.reflect.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.openide.DialogDisplayer;

import org.openide.util.datatransfer.*;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;

import org.openide.actions.CutAction;
import org.openide.actions.CopyAction;
import org.openide.actions.PasteAction;
import org.openide.actions.DeleteAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataShadow;

import org.openide.util.actions.ActionPerformer;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.WeakListener;


/**
 * This class contains the default implementation of reactions to the standard
 * explorer actions. It can be attached to any {@link ExplorerManager}. Then
 * this class will listen to changes of selected nodes or the explored context
 * of that manager, and update the state of cut/copy/paste/delete actions.  <P>
 * An instance of this class can only be attached to one manager at a time. Use
 * {@link #attach} and {@link #detach} to make the connection.
 *
 * @author Jan Jancura, Petr Hamernik, Ian Formanek, Jaroslav Tulach
 */
public class ExplorerActions
{
    /** copy action performer */
    private final CopyCutActionPerformer copyActionPerformer = new CopyCutActionPerformer (true);
    /** cut action performer */
    private final CopyCutActionPerformer cutActionPerformer = new CopyCutActionPerformer (false);
    /** delete action performer */
    private final DeleteActionPerformer deleteActionPerformer = new DeleteActionPerformer();
    /** own paste action */
    private final OwnPaste pasteActionPerformer = new OwnPaste ();

    private ActionStateUpdater actionStateUpdater;
    
    /** the manager we are listening on */
    private ExplorerManager manager;

    /** must the delete be confirmed */
    private boolean confirmDelete = true;
    /** attach as performers or just update the actions */
    private boolean attachPerformers;

    /** actions to work with */
    private static CopyAction copy = null;
    private static CutAction cut = null;
    private static DeleteAction delete = null;
    private static PasteAction paste = null;
    
    /** Creates new instance.
     */
    public ExplorerActions () {
        this (true);
    }
    
    /** Creates new instance with a decision whether the action should update
     * performers (the old behaviour) or only set the state of cut,copy,delete,
     * and paste actions.
     */
    ExplorerActions (boolean attachPerformers) {
        this.attachPerformers = attachPerformers;
    }
    
    /** Getter for the copy action.
     */
    final Action copyAction () {
        return copyActionPerformer;
    }
    
    /** The cut action */
    final Action cutAction () {
        return cutActionPerformer;
    }
    
    /** The delete action 
     */
    final Action deleteAction () {
        return deleteActionPerformer;
    }
    
    /** Own paste action 
     */
    final Action pasteAction () {
        return pasteActionPerformer;
    }
    

    /** Attach to new manager.
     * @param m the manager to listen on
     */
    public synchronized void attach (ExplorerManager m) {
        if (manager != null) {
            // first of all detach
            detach ();
        }

        manager = m;
        
        // Sets action state updater and registers listening on manager and
        // exclipboard.
        actionStateUpdater = new ActionStateUpdater();
        manager.addPropertyChangeListener(
            WeakListener.propertyChange(actionStateUpdater, manager));
        Clipboard c = getClipboard();
        if (c instanceof ExClipboard) {
            ExClipboard clip = (ExClipboard)c;
            clip.addClipboardListener(
                (ClipboardListener)WeakListener.create(
                    ClipboardListener.class, actionStateUpdater, clip));
        }
        updateActions ();
    }

    /** Detach from manager currently being listened on. */
    public synchronized void detach () {
        if (manager == null) return;

        // Unregisters (weak) listening on manager and exclipboard (see attach).
        actionStateUpdater = null;
        
        stopActions ();

        manager = null;
    }
    
    /** Access method for use from ExplorerPanel, and also
     * via reflection (!) from RegistryImpl in core.
     * @deprecated Kill me later; see #18137 for explanation.
     */
    ExplorerManager getAttachedManager() {
        return manager;
    }

    /** Set whether to confirm deletions.
    * @param yes <code>true</code> to confirm deletions
    */
    public final void setConfirmDelete (boolean yes) {
        confirmDelete = yes;
    }

    /** Should deletions be confirmed?
    * @return <code>true</code> if deletions must be confirmed
    */
    public final boolean isConfirmDelete () {
        return confirmDelete;
    }

    /** Stops listening on all actions */
    private void stopActions () {
        if (copyActionPerformer != null) {
            if (attachPerformers) {
                if (copy.getActionPerformer() instanceof CopyCutActionPerformer) {
                    copy.setActionPerformer (null);
                }
                if (cut.getActionPerformer() instanceof CopyCutActionPerformer) {
                    cut.setActionPerformer (null);
                }
                
                paste.setPasteTypes (null);
                
                if (delete.getActionPerformer() instanceof DeleteActionPerformer) {
                    delete.setActionPerformer (null);
                }
            } else {
                copyActionPerformer.setEnabled (false);
                cutActionPerformer.setEnabled (false);
                deleteActionPerformer.setEnabled (false);
                pasteActionPerformer.setEnabled (false);
            }
        }
    }

    /** Updates the state of all actions.
     * @param path list of selected nodes
     */
    private void updateActions () {
        if (manager == null) return;
        Node[] path = manager.getSelectedNodes();

        if (copy == null) {
            copy = (CopyAction) CopyAction.findObject(CopyAction.class, true);
            cut = (CutAction) CutAction.findObject(CutAction.class, true);
            paste = (PasteAction) PasteAction.findObject(PasteAction.class, true);
            delete = (DeleteAction) DeleteAction.findObject(DeleteAction.class, true);
        }

        int i;
        int k = path != null ? path.length : 0;
        if (k > 0) {
            
            boolean incest = false;
            if (k > 1) {
                // Do a special check for parenthood. Affects delete (for a long time),
                // copy (#13418), cut (#13426). If one node is a parent of another,
                // assume that the situation is sketchy and prevent it.
                // For k==1 it is impossible so do not waste time on it.
                HashMap allNodes = new HashMap(101);
                for (i = 0; i < k; i++) {
                    if (! checkParents(path[i], allNodes)) {
                        incest = true;
                        break;
                    }
                }
            }
            
            for (i = 0; i < k; i++) {
                if (incest || !path[i].canCopy()) {
                    if (attachPerformers) {
                        copy.setActionPerformer (null);
                    } else {
                        copyActionPerformer.setEnabled (false);
                    }
                    break;
                }
            }
            if (i == k) {
                if (attachPerformers) { 
                    copy.setActionPerformer (copyActionPerformer);
                } else {
                    copyActionPerformer.setEnabled (true);
                }
            }

            for (i = 0; i < k; i++) {
                if (incest || !path[i].canCut()) {
                    if (attachPerformers) {
                        cut.setActionPerformer (null);
                    } else {
                        cutActionPerformer.setEnabled (false);
                    }
                    break;
                }
            }
            if (i == k) {
                if (attachPerformers) {
                    cut.setActionPerformer (cutActionPerformer);
                } else {
                    cutActionPerformer.setEnabled (true);
                }
            }

            for (i = 0; i < k; i++) {
                if (incest || !path[i].canDestroy()) {
                    if (attachPerformers) {
                        delete.setActionPerformer (null);
                    } else {
                        deleteActionPerformer.setEnabled(false);
                    }
                    break;
                }

            }
            if (i == k) {
                if (attachPerformers) {
                    delete.setActionPerformer (deleteActionPerformer);
                } else {
                    deleteActionPerformer.setEnabled (true);
                }
            }

        } else { // k==0, i.e. no nodes selected
            if (attachPerformers) {
                copy.setActionPerformer (null);
                cut.setActionPerformer (null);
                delete.setActionPerformer (null);
            } else {
                copyActionPerformer.setEnabled(false);
                cutActionPerformer.setEnabled(false);
                deleteActionPerformer.setEnabled(false);
            }
        }
        updatePasteAction(path);
    }
    
    /** Adds all parent nodes into the set.
     * @param set set of all nodes
     * @param node the node to check
     * @return false if one of the nodes is parent of another
     */
    private boolean checkParents (Node node, HashMap set) {
        if (set.get (node) != null) {
            return false;
        }
        
        // this signals that this node is the original one
        set.put (node, this);
        
        for (;;) {
            node = node.getParentNode ();
            
            if (node == null) {
                return true;
            }
            
        
            if (set.put (node, node) == this) {
                // our parent is a node that is also in the set
                return false;
            }
        }
    }
    

    /** Updates paste action.
    * @param path selected nodes
    */
    private void updatePasteAction (Node[] path) {
        ExplorerManager man = manager;

        if (man == null) {
            if (attachPerformers) {
                paste.setPasteTypes (null);
            } else {
                pasteActionPerformer.setPasteTypes (null);
            }
            return;
        }

        if (path != null && ((path.length > 1)/* ||
                             ((path.length == 1) && (path [0].isLeaf()))*/)
        ) {
            if (attachPerformers) {
                paste.setPasteTypes(null);
            } else {
                pasteActionPerformer.setPasteTypes (null);
            }
            return;
        } else {
            Node node = man.getExploredContext ();
            Node[] selectedNodes = man.getSelectedNodes ();
            if (selectedNodes != null && (selectedNodes.length == 1)
            /*&& (!selectedNodes[0].isLeaf())*/) {
                node = selectedNodes[0];
            }
            
            if(node != null) {
                Transferable trans = getClipboard().getContents(this);
                updatePasteTypes(trans, node);
            }
        }
    }

    /** Actually updates paste types. */
    private void updatePasteTypes(Transferable trans, Node pan) {
        if (trans != null) {
            // First, just ask the node if it likes this transferable, whatever it may be.
            // If it does, then fine.
            PasteType[] pasteTypes = pan == null ? new PasteType[] { } : pan.getPasteTypes(trans);
            if (pasteTypes.length != 0) {
                if (attachPerformers) {
                    paste.setPasteTypes(pasteTypes);
                } else {
                    pasteActionPerformer.setPasteTypes (pasteTypes);
                }
                return;
            }

            boolean flavorSupported = false;
            try {

                flavorSupported = trans.isDataFlavorSupported(ExTransferable.multiFlavor);

            } catch (java.lang.Exception e) {
                // patch to get the Netbeans start under Solaris
                // [PENDINGworkaround]
            }

            if (flavorSupported) {
                // The node did not accept this multitransfer as is--try to break it into
                // individual transfers and paste them in sequence instead.
                try {
                    MultiTransferObject obj = (MultiTransferObject) trans.getTransferData(ExTransferable.multiFlavor);
                    int count = obj.getCount();
                    boolean ok = true;
                    Transferable[] t = new Transferable[count];
                    PasteType[] p = new PasteType[count];

                    for (int i = 0; i < count; i++) {
                        t[i] = obj.getTransferableAt(i);
                        pasteTypes = pan == null ? new PasteType[] { } : pan.getPasteTypes(t[i]);
                        if (pasteTypes.length == 0) {
                            ok = false;
                            break;
                        }
                        // [PENDING] this is ugly! ideally should be some way of comparing PasteType's for similarity?
                        p[i] = pasteTypes[0];
                    }
                    if (ok) {
                        PasteType[] arrOfPaste = new PasteType[] { new MultiPasteType(t, p) };
                        if (attachPerformers) {
                            paste.setPasteTypes(arrOfPaste);
                        } else {
                            pasteActionPerformer.setPasteTypes (arrOfPaste);
                        }
                        return;
                    }
                }
                catch (UnsupportedFlavorException e) {
                    // [PENDING] notify?!
                }
                catch (IOException e) {
                    // [PENDING] notify?!
                }
            }
        }

        if (attachPerformers) {
            if(paste != null) {
                paste.setPasteTypes(null);
            }
        } else {
            pasteActionPerformer.setPasteTypes (null);
        }
    }

    /** If our clipboard is not found return the default system clipboard. */
    private static Clipboard getClipboard() {
        Clipboard c = (java.awt.datatransfer.Clipboard)
            org.openide.util.Lookup.getDefault().lookup(java.awt.datatransfer.Clipboard.class);
        if (c == null) {
            c = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        return c;
    }
    
    /** Paste type used when in clipbopard is MultiTransferable */
    private static class MultiPasteType extends PasteType {
        /** Array of transferables */
        Transferable[] t;

        /** Array of paste types */
        PasteType[] p;

        /** Constructs new MultiPasteType for the given content of the clipboard */
        MultiPasteType(Transferable[] t, PasteType[] p) {
            this.t = t;
            this.p = p;
        }

        /** Performs the paste action.
        * @return Transferable which should be inserted into the clipboard after
        *         paste action. It can be null, which means that clipboard content
        *         should be cleared.
        */
        public Transferable paste() throws IOException {
            int size = p.length;
            Transferable[] arr = new Transferable[size];
            for (int i = 0; i < size; i++) {
                Transferable newTransferable = p[i].paste();
                if (newTransferable != null) {
                    arr[i] = newTransferable;
                } else {
                    // keep the orginal
                    arr[i] = t[i];
                }
            }
            return new ExTransferable.Multi (arr);
        }
    }
    
    /** Own implementation of paste action
     */
    private class OwnPaste extends AbstractAction {
        private PasteType[] pasteTypes;
        
        OwnPaste() {}
        
        public boolean isEnabled() {
            updateActionsState();
            return super.isEnabled();
        }
        
        public void setPasteTypes (PasteType[] arr) {
            synchronized (this) {
                this.pasteTypes = arr;
            }
            setEnabled (arr != null);
        }
        
        public void actionPerformed(ActionEvent e) {
            throw new IllegalStateException ("Should not be invoked at all. Paste types: " + java.util.Arrays.asList (pasteTypes)); // NOI18N
        }
        
        public Object getValue (String s) {
            if ("delegates".equals (s)) { // NOI18N
                return pasteTypes;
            }
            return super.getValue (s);
        }
    }
    
    /** Class which performs copy and cut actions */
    private class CopyCutActionPerformer extends AbstractAction
        implements org.openide.util.actions.ActionPerformer
    {
        /** determine if adapter is used for copy or cut action. */
        private boolean copyCut;

        /** Create new adapter */
        public CopyCutActionPerformer (boolean b) {
            copyCut = b;
        }

        public boolean isEnabled() {
            updateActionsState();
            return super.isEnabled();
        }
        
        /** Perform copy or cut action. */
        public void performAction(org.openide.util.actions.SystemAction action) {
            Transferable trans = null;
            Node[] sel = manager.getSelectedNodes ();

            if (sel.length != 1) {
                Transferable[] arrayTrans = new Transferable[sel.length];

                for (int i = 0; i < sel.length; i++)
                    if ((arrayTrans[i] = getTransferableOwner(sel[i])) == null) return;
                trans = new ExTransferable.Multi (arrayTrans);
            }
            else {
                trans = getTransferableOwner(sel[0]);
            }
            if (trans != null) {
                Clipboard clipboard = getClipboard();

                clipboard.setContents(trans, new StringSelection ("")); // NOI18N
            }
        }

        private Transferable getTransferableOwner(Node node) {
            try {
                return copyCut ? node.clipboardCopy() : node.clipboardCut();
            } catch (java.io.IOException e) {
                ErrorManager.getDefault ().notify(ErrorManager.INFORMATIONAL, e);
                return null;
            }
        }
        
        /** Invoked when an action occurs.
         *
         */
        public void actionPerformed(ActionEvent e) {
            performAction (null);
        }
        
    }

    /** Class which performs delete action */
    private class DeleteActionPerformer extends AbstractAction implements ActionPerformer {
        
        DeleteActionPerformer() {}
        
        public boolean isEnabled() {
            updateActionsState();
            return super.isEnabled();
        }
        
        /** Perform delete action. */
        public void performAction(org.openide.util.actions.SystemAction action) {
            final Node[] sel = manager.getSelectedNodes ();
            if ((sel == null) || (sel.length == 0))
                return;

            // perform action if confirmed
            if (!confirmDelete || doConfirm(sel)) {

                // clear selected nodes
                try {
                    if (manager != null) {
                        manager.setSelectedNodes(new Node[] {});
                    }
                } catch (java.beans.PropertyVetoException e) {
                    // never thrown, setting empty selected nodes cannot be vetoed
                }
                
                doDestroy(sel);
                
                if (attachPerformers) {
                    delete.setActionPerformer (null); // fixes bug #673
                } else {
                    setEnabled (false);
                }
            }
        }
        
        private boolean doConfirm(Node[] sel) {
            String message, title;
            if (sel.length == 1) {
                if (sel[0].getCookie(DataShadow.class) != null) {
                    title = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteShadowTitle");
                    DataShadow obj = (DataShadow)sel[0].getCookie(DataShadow.class);
                    message = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteShadow",
                        new Object[] {
                            obj.getName(), // name of the shadow
                            sel[0].getDisplayName(), // name of original
                            fullName(obj), // full name of file for shadow
                            fullName(obj.getOriginal()) // full name of original file
                        });
                } else if (sel[0].getCookie(org.openide.loaders.DataFolder.class) != null) {
                    message = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteFolder",
                    sel[0].getDisplayName());
                    title = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteFolderTitle");
                } else {
                    message = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteObject",
                    sel[0].getDisplayName());
                    title = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteObjectTitle");
                }
            }
            else {
                message = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteObjects",
                new Integer(sel.length));
                title = NbBundle.getMessage(ExplorerActions.class, "MSG_ConfirmDeleteObjectsTitle");
            }
            NotifyDescriptor desc = new NotifyDescriptor.Confirmation(message, title, NotifyDescriptor.YES_NO_OPTION);
            return NotifyDescriptor.YES_OPTION.equals(DialogDisplayer.getDefault().notify(desc));
        }
        private String fullName(DataObject obj) {
            FileObject f = obj.getPrimaryFile();
            if (f.isRoot()) {
                try {
                    return f.getFileSystem().getDisplayName();
                } catch (FileStateInvalidException e) {
                    return ""; //NOI18N
                }
            } else {
                return f.toString();
            }
        }
        
        private void doDestroy(final Node[] sel) {
            try {
                Repository.getDefault().getDefaultFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
                    public void run() throws IOException {
                        for (int i=0; i< sel.length; i++) {
                            try {
                                sel[i].destroy();
                            } catch (IOException e) {
                                ErrorManager.getDefault().notify(e);
                            }
                        }
                    }
                });
            } catch (IOException ioe) {
                IllegalStateException ise = new IllegalStateException();
                ErrorManager.getDefault().annotate(ise, ioe);
                throw ise;
            }
        }
        
        /** Invoked when an action occurs.
         *
         */
        public void actionPerformed(ActionEvent e) {
            performAction (null);
        }
        
    }

    /** Updates actions state via updater (if the updater is present). */
    private void updateActionsState() {
        ActionStateUpdater asu;
        synchronized(this) {
            asu = actionStateUpdater;
        }
        if(asu != null) {
            actionStateUpdater.update();
        }
    }
    
    /** Class which register changes in manager, and clipboard, coalesces
     * them if they are frequent and performs the update of actions state. */
    private class ActionStateUpdater implements PropertyChangeListener,
                                                ClipboardListener,
                                                ActionListener
    {
        private final Timer timer;

        ActionStateUpdater() {
            timer = new FixIssue29405Timer(150, this);
            timer.setCoalesce(true);
            timer.setRepeats(false);
        }
        
        public void propertyChange(PropertyChangeEvent e) {
            timer.restart();
        }

        public void clipboardChanged(ClipboardEvent ev) {
            if (!ev.isConsumed()) {
                updatePasteAction(manager.getSelectedNodes());
            }
        }

        public void actionPerformed(ActionEvent evt) {
            updateActions();
            timer.stop();
        }
        
        /** Updates actions states now if there is pending event. */
        public void update() {
            if(timer.isRunning()) {
                timer.stop();
                updateActions();
            }
        }
    }
    
    /** Timer which fixes problem with running status (issue #29405). */
    private static class FixIssue29405Timer extends javax.swing.Timer {
        private boolean running;
        
        public FixIssue29405Timer(int delay, ActionListener l) {
            super(delay, l);
        }
        
        public void restart() {
            super.restart();
            running = true;
        }
        
        public void stop() {
            running = false;
            super.stop();
        }
        
        public boolean isRunning() {
            return running;
        }
        
    } // End of FixIssue29405Timer class.
}
