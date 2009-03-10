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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.text.MessageFormat;
import javax.swing.Timer;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.io.*;
import org.openide.windows.TopComponent;
import org.openide.windows.Workspace;
import org.openide.windows.WindowManager;

/** Simple top component capable of displaying an Explorer.
* Holds one instance of {@link ExplorerManager} and 
* implements {@link ExplorerManager.Provider} to allow child components to share 
* the same explorer manager.
* <p>Uses {@link java.awt.BorderLayout} by default.
* Pays attention to the selected nodes and explored context as indicated by the manager.
* Cut/copy/paste actions are sensitive to the activation state of the component.
* <p>It is up to you to add a view and other UI apparatus to the panel.
*
* @author Jaroslav Tulach
*/
public class ExplorerPanel extends TopComponent implements ExplorerManager.Provider {
    /** serial version UID */
    static final long serialVersionUID = 5522528786650751459L;

    /** The message formatter for Explorer title */
    private static MessageFormat formatExplorerTitle;

    /** the instance of the explorer manager*/
    private ExplorerManager manager;

    /** listens on the selected nodes in the ExporerManager */
    transient private final PropertyChangeListener managerListener = new PropL();

    /** action handler for cut/copy/paste/delete */
    private static final ExplorerActions actions = new ExplorerActions ();
    
    /** Init delay for second change of the activated nodes. */
    private static final int INIT_DELAY = 70;
    
    /** Maximum delay for repeated change of the activated nodes. */
    private static final int MAX_DELAY = 350;
    
    private static Boolean scheduleAcivatedNodes;
    

    /** Initialize the explorer panel with the provided manager.
    * @param manager the explorer manager to use
    */
    public ExplorerPanel (ExplorerManager manager) {
        this.manager = manager;
        
        setLayout (new java.awt.BorderLayout ());
        initActionMap();
        initListening();
    }

    /** Default constructor. Uses newly created manager.
    */
    public ExplorerPanel () {
        this(null);
    }
    
    /** Initializes actions map. */
    private void initActionMap() {
        ExplorerActions a = new ExplorerActions (false);
        a.attach (getExplorerManager ());

        getActionMap ().put (
            javax.swing.text.DefaultEditorKit.copyAction, a.copyAction ()
        );
        getActionMap ().put (
            javax.swing.text.DefaultEditorKit.cutAction, a.cutAction ()
        );
        getActionMap ().put (
            javax.swing.text.DefaultEditorKit.pasteAction, a.pasteAction ()
        );
        getActionMap ().put (
            "delete", a.deleteAction () // NOI18N
        );
    }

    /** Initializes listening on ExplorerManager property changes. */
    private void initListening() {
        // Attaches listener if there is not one already.
        ExplorerManager man = getExplorerManager();
        man.addPropertyChangeListener(
            org.openide.util.WeakListener.propertyChange(
                managerListener, man));
    }

    /* Add a listener to the explorer panel in addition to the normal
    * open behaviour.
    */
    public void open () {
        open(WindowManager.getDefault().getCurrentWorkspace());
    }

    /* Add a listener to the explorer panel in addition to the normal
    * open behaviour.
    */
    public void open (Workspace workspace) {
        super.open(workspace);
        setActivatedNodes (getExplorerManager ().getSelectedNodes ());
        updateTitle ();
    }

    /* Provides the explorer manager to all who are interested.
    * @return the manager
    */
    public synchronized ExplorerManager getExplorerManager () {
        if (manager == null) {
            manager = new ExplorerManager ();
        }

        return manager;
    }
    

    /* Deactivates copy/cut/paste actions.
    */
    protected void componentDeactivated () {
        if (getExplorerManager() == actions.getAttachedManager()) { // #18137
            actions.detach ();
        }
    }

    /** Called when the explored context changes.
    * The default implementation updates the title of the window.
    */
    protected void updateTitle () {
        String name = ""; // NOI18N

        ExplorerManager em = getExplorerManager ();
        if (em != null) {
            Node n = em.getExploredContext();
            if (n != null) {
                String nm = n.getDisplayName();
                if (nm != null) {
                    name = nm;
                }
            }
        }

        if (formatExplorerTitle == null) {
            formatExplorerTitle = new MessageFormat (NbBundle.getMessage (ExplorerPanel.class, "explorerTitle"));
        }
        setName(formatExplorerTitle.format (
                    new Object[] { name }
                ));
    }

    /** Get context help for an explorer window.
    * Looks at the manager's node selection.
    * @return the help context
    * @see #getHelpCtx(Node[],HelpCtx)
    */
    public HelpCtx getHelpCtx () {
        return getHelpCtx (getExplorerManager ().getSelectedNodes (),
                           new HelpCtx (ExplorerPanel.class));
    }

    /** Utility method to get context help from a node selection.
    * Tries to find context helps for selected nodes.
    * If there are some, and they all agree, uses that.
    * In all other cases, uses the supplied generic help.
    * @param sel a list of nodes to search for help in
    * @param def the default help to use if they have none or do not agree
    * @return a help context
    */
    public static HelpCtx getHelpCtx (Node[] sel, HelpCtx def) {
        HelpCtx result = null;
        for (int i = 0; i < sel.length; i++) {
            HelpCtx attempt = sel[i].getHelpCtx ();
            if (attempt != null && ! attempt.equals (HelpCtx.DEFAULT_HELP)) {
                if (result == null || result.equals (attempt)) {
                    result = attempt;
                } else {
                    // More than one found, and they conflict. Get general help on the Explorer instead.
                    result = null;
                    break;
                }
            }
        }
        if (result != null)
            return result;
        else
            return def;
    }

    /** Set whether deletions should have to be confirmed on all Explorer panels.
    * @param confirmDelete <code>true</code> to confirm, <code>false</code> to delete at once
    */
    public static void setConfirmDelete (boolean confirmDelete) {
        actions.setConfirmDelete (confirmDelete);
    }

    /** Are deletions confirmed on all Explorer panels?
    * @return <code>true</code> if they must be confirmed
    */
    public static boolean isConfirmDelete () {
        return actions.isConfirmDelete ();
    }
    
    /** Stores the manager */
    public void writeExternal (ObjectOutput oo) throws IOException {
        super.writeExternal (oo);
        oo.writeObject (new NbMarshalledObject (manager));
    }

    /** Reads the manager.
    * Deserialization may throw {@link SafeException} in case
    * the manager cannot be loaded correctly but the stream is still uncorrupted.
    */
    public void readExternal (ObjectInput oi)
    throws IOException, ClassNotFoundException {
        super.readExternal (oi);
        Object anObj = oi.readObject ();
        if (anObj instanceof ExplorerManager) {
            manager = (ExplorerManager)anObj;
            initActionMap();
            initListening();
            return;
        }
        NbMarshalledObject obj = (NbMarshalledObject) anObj;
        
        // --- read all data from main stream, it is OK now ---
        try {
            manager = (ExplorerManager) obj.get ();
            initActionMap();
            initListening();
        } catch (SafeException se) {
            throw se;
        } catch (IOException ioe) {
            throw new SafeException (ioe);
        }
    }
    
    // temporary workaround the issue #31244
    private boolean delayActivatedNodes () {
        if (scheduleAcivatedNodes == null) {
            if (System.getProperty ("netbeans.delay.tc") != null) { // NOI18N
                scheduleAcivatedNodes = Boolean.getBoolean ("netbeans.delay.tc") ? Boolean.TRUE : Boolean.FALSE; // NOI18N
            } else {
                scheduleAcivatedNodes = Boolean.FALSE;
            }
        }
        return scheduleAcivatedNodes.booleanValue ();
    }
    
    /** Listener on the explorer manager properties.
    * Changes selected nodes of this frame.
    */
    private final class PropL extends Object implements PropertyChangeListener {
	PropL() {}
	
        public void propertyChange(PropertyChangeEvent evt) {
            if(evt.getSource() != manager) {
                return;
            }
            
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                if (delayActivatedNodes ()) {
                    scheduleActivatedNodes (manager.getSelectedNodes ());
                } else {
                    setActivatedNodes (manager.getSelectedNodes ());
                }
                return;
            }
            if (ExplorerManager.PROP_EXPLORED_CONTEXT.equals(evt.getPropertyName())) {
                updateTitle ();
                return;
            }
        }
    }

    private class DelayedSetter implements ActionListener {
	DelayedSetter () {}	
        private Node[] nodes;
        private Timer timer;
        private boolean firstChange = true;

        public void scheduleActivatedNodes (Node[] nodes) {
            synchronized (this) {
                this.nodes = nodes;
                if (timer == null) {
                    // start timer with INIT_DELAY
                    timer = new Timer (INIT_DELAY, this);
                    timer.setCoalesce (true);
                    timer.setRepeats (false);
                }
                if (timer.isRunning ()) {
                    // if timer is running then double init delay
                    if (timer.getInitialDelay () < MAX_DELAY) timer.setInitialDelay (timer.getInitialDelay () * 2);
                    firstChange = false;
                } else {
                    // the first change is set immediatelly
                    setActivatedNodes (nodes);
                    firstChange = true;
                }
                // make sure timer is running
                timer.restart();
            }
        }

        public void actionPerformed (ActionEvent evt) {
            synchronized (this) {
                synchronized (this) {
                    timer.stop ();
                }
            }
            // set activated nodes for 2nd and next changes
            if (!firstChange) {
                setActivatedNodes (nodes);
            }
        }
    }
    
    private transient DelayedSetter delayedSetter;
    
    // schudule activation the nodes
    private final void scheduleActivatedNodes (Node[] nodes) {
        synchronized (this) {
            if (delayedSetter == null)
                delayedSetter = new DelayedSetter ();
        }
        delayedSetter.scheduleActivatedNodes (nodes);
    }
    
}
