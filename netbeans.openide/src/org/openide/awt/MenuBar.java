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

package org.openide.awt;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import org.openide.loaders.*;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.nodes.*;
import org.openide.util.actions.Presenter;
import org.openide.util.*;

/** An extended version of swing's JMenuBar. This menubar can
 * load its content from the folder where its "disk image" is stored.<P>
 * Moreover, menu is <code>Externalizable</code> to restore its persistent
 * state with minimal storage expensiveness.
 *
 * The MenuBar recognizes following objects in the folder: <UL>
 * <LI>subfolders - they're turned into top-level JMenu instances
 * <LI>instances of <CODE>Component</CODE> - they're added directly
 *  to the menubar.
 * <LI>instances of <CODE>Presenter.Toolbar</CODE> - their toolbar presenter
 *  is added to the menubar.
 * </UL>
 * before OpenAPI version 3.2, only subfolders were recognized.
 *
 * <P>In subfolders the following objects are recognized and added to submenus:<UL>
 * <LI>nested subfolders - they're turned into submenus
 * <LI>instances of <CODE>Presenter.Menu</CODE>
 * <LI>instances of <CODE>JMenuItem</CODE>
 * <LI>instances of <CODE>JSeparator</CODE>
 * <LI>instances of <CODE>Action</CODE>
 * <LI>executable <CODE>DataObject</CODE>s
 * </UL>
 *
 * @author  David Peroutka, Dafe Simonek, Petr Nejedly
 */
public class MenuBar extends JMenuBar implements Externalizable {

    /** the folder which represents and loads content of the menubar */
    private MenuBarFolder menuBarFolder;
    
    static final long serialVersionUID =-4721949937356581268L;

    /** Don't call this constructor or this class will not get
     * initialized properly. This constructor is only for externalization.
     */
    public MenuBar() {
        super();
    }

    /** Creates a new <code>MenuBar</code> from given folder.
     * @param folder The folder from which to create the content of the menubar.
     * If the parameter is null, default menu folder is obtained.
     */
    public MenuBar(DataFolder folder) {
        super();
        // PENDING(david) -- setBorder(new MenuBorder());
        DataFolder theFolder = folder;
        if (theFolder == null) {
            FileObject fo = Repository.getDefault().getDefaultFileSystem().findResource("Menu");
            if (fo == null) throw new IllegalStateException("No Menu/"); // NOI18N
            theFolder = DataFolder.findFolder(fo);
        }
        startLoading(theFolder);

        if(folder != null) {
            getAccessibleContext().setAccessibleDescription(folder.getName());
        }
    }

    /** Blocks until the menubar is completely created. */
    public void waitFinished () {
        menuBarFolder.instanceFinished();
    }
    
    /** Saves the contents of this object to the specified stream.
     *
     * @exception IOException Includes any I/O exceptions that may occur
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(menuBarFolder.getFolder());
    }

    /**
     * Restores contents of this object from the specified stream.
     *
     * @exception ClassNotFoundException If the class for an object being
     *              restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        startLoading((DataFolder)in.readObject());
    }
    
    /** Starts loading of this menu from menu folder */
    void startLoading (final DataFolder folder) {
        menuBarFolder = new MenuBarFolder(folder);
    }
    
    /** Convert an array of instance cookies to instances, adds them
     * to given list.
     * @param arr array of instance cookies
     * @param list list to add created objects to
     */
    static void allInstances (InstanceCookie[] arr, java.util.List list) {
        org.openide.ErrorManager err = org.openide.ErrorManager.getDefault ();
        
        Exception ex = null;
        
        for (int i = 0; i < arr.length; i++) {
            
            Exception newEx = null;
            try {
                Object o = arr[i].instanceCreate();
                list.add (o);
            } catch (ClassNotFoundException e) {
                newEx = e;
            } catch (IOException e) {
                newEx = e;
            }
            
            if (newEx != null) {
                if (err.findAnnotations(newEx) == null) {
                    // if the exception is not annotated, assign it low
                    // priority
                    err.annotate(newEx, err.INFORMATIONAL, null, null, null, null);
                }
                err.copyAnnotation(newEx, ex);
                ex = newEx;
            }
        }
     
        // if there was an exception => notify it
        if (ex != null) {
            err.notify (ex);
        }
    }

    /** This class can be used to fill the content of given
     * <code>MenuBar</code> from the given <code>DataFolder</code>.
     */
    private final class MenuBarFolder extends FolderInstance {
        /** List of the components this FolderInstance manages. */
        private ArrayList managed = new ArrayList();

        /** Creates a new menubar folder on the specified <code>DataFolder</code>.
         * @param folder a <code>DataFolder</code> to work with
         */
        public MenuBarFolder (final DataFolder folder) {
            super(folder);
            recreate ();
        }

        /** Removes the components added by this FolderInstance from the MenuBar.
         * Called when menu is refreshed. */
        private void cleanUp() {
            for (Iterator it = managed.iterator(); it.hasNext(); ) {
                MenuBar.this.remove((Component)it.next());
            }
            managed.clear();
        }

        /** Adds the component to the MenuBar after the last added one */
        private void addComponent (Component c) {
            MenuBar.this.add(c, managed.size());
            managed.add(c);
        }

        /** Full name of the data folder's primary file separated by dots.
         * @return the name
         */
        public String instanceName () {
            return MenuBar.class.getName();
        }

        /** Returns the root class of all objects.
         * @return MenuBar.class
         */
        public Class instanceClass () {
            return MenuBar.class;
        }

        /** Accepts only cookies that can provide a <code>Component</code>
         * or a <code>Presenter.Toolbar</code>.
         * @param cookie the instance cookie to test
         * @return true if the cookie is accepted.
         */
        protected InstanceCookie acceptCookie (InstanceCookie cookie)
    			    throws IOException, ClassNotFoundException {
            Class cls = cookie.instanceClass();
            return Component.class.isAssignableFrom(cls) ||
                   Presenter.Toolbar.class.isAssignableFrom(cls)
                   ? cookie : null;
        }

        /** Returns an <code>InstanceCookie</code> of a JMenu
	 * for the specified <code>DataFolder</code>.
	 *
         * @param df a <code>DataFolder</code> to create the cookie for
         * @return an <code>InstanceCookie</code> for the specified folder
         */
        protected InstanceCookie acceptFolder (DataFolder df) {
	    return new InstanceSupport.Instance(new LazyMenu(df, false));
        }

        /** Updates the <code>MenuBar</code> represented by this folder.
         *
         * @param cookies array of instance cookies for the folder
         * @return the updated <code>MenuBar</code> representee
         */
        protected Object createInstance (InstanceCookie[] cookies)
        throws IOException, ClassNotFoundException {
            final LinkedList ll = new LinkedList ();
            allInstances (cookies, ll);

	    final MenuBar mb = MenuBar.this;

            cleanUp(); //remove the stuff we've added last time

            // fill with new content
            Iterator it = ll.iterator ();
            while (it.hasNext()) {
                Object obj = it.next ();
                if (obj instanceof Presenter.Toolbar) {
                    obj = ((Presenter.Toolbar)obj).getToolbarPresenter();
                    if (obj instanceof JButton) { // tune the presenter a bit
                        ((JButton)obj).setBorderPainted(false);
                    }
                }
                if (obj instanceof Component) addComponent((Component) obj);
            }
            mb.validate();
            mb.repaint();
            return mb;
        }

        /** For outer class access to the data folder */
        DataFolder getFolder () {
            return folder;
        }

        /** Recreate the instance in AWT thread. */
        protected Task postCreationTask (Runnable run) {
            return new AWTTask (run);
        }

    }

    /** Menu based on the folder content whith lazy items creation. */
    private static class LazyMenu extends JMenuPlus implements NodeListener, Runnable {
	DataFolder master;
	boolean icon;
	MenuFolder slave;
	
	/** Constructor. */
        public LazyMenu(final DataFolder df, boolean icon) {
	    master = df;
	    this.icon = icon;

	    // Listen for changes in Node's DisplayName/Icon
            Node n = master.getNodeDelegate ();
            n.addNodeListener (WeakListener.node (this, n));
	    updateProps();

        }

	private void updateProps() {
            // set the text and be aware of mnemonics
            Node n = master.getNodeDelegate ();
            Actions.setMenuText(this, n.getDisplayName (), true);
            if (icon) setIcon (new ImageIcon (
		    n.getIcon (java.beans.BeanInfo.ICON_COLOR_16x16)));
	}

        /** Update the properties. Exported via Runnable interface so it
         * can be rescheduled. */
        public void run() {
		updateProps();
        }

        /** If the display name changes, than change the name of the menu.*/
        public void propertyChange (java.beans.PropertyChangeEvent ev) {
            if (
                Node.PROP_DISPLAY_NAME.equals (ev.getPropertyName ()) ||
                Node.PROP_NAME.equals (ev.getPropertyName ()) ||
                Node.PROP_ICON.equals (ev.getPropertyName ())
            ) {
                // update the properties in AWT queue
                if (EventQueue.isDispatchThread ()) {
                    updateProps(); // do the update synchronously
                } else {
                    EventQueue.invokeLater (this);
                }
            }
        }

	// The rest of the NodeListener implementation
        public void childrenAdded (NodeMemberEvent ev) {}
        public void childrenRemoved (NodeMemberEvent ev) {}
        public void childrenReordered(NodeReorderEvent ev) {}
        public void nodeDestroyed (NodeEvent ev) {}

            
        /** Overrides superclass method to lazy create popup. */
        public JPopupMenu getPopupMenu() {
            doInitialize();
            return super.getPopupMenu();
        }
        
	private void doInitialize() {
	    if(slave == null) {
		slave = new MenuFolder(); // will do the tracking
		slave.waitFinished();
	    }
	}
	    
	/** This class can be used to update a <code>JMenu</code> instance
	 * from the given <code>DataFolder</code>.
	 */
	private class MenuFolder extends FolderInstance {

    	    /**
             * Start tracking the content of the master folder.
	     * It will cause initial update of the Menu
             */
    	    public MenuFolder () {
        	super(master);
        	recreate ();
    	    }


    	    /** The name of the menu
             * @return the name
             */
    	    public String instanceName () {
        	return LazyMenu.class.getName();
    	    }

    	    /** Returns the class of represented menu.
             * @return JMenu.class
             */
    	    public Class instanceClass () {
    		return JMenu.class;
    	    }

    	    /** If no instance cookie, tries to create execution action on the
             * data object.
             */
    	    protected InstanceCookie acceptDataObject (DataObject dob) {
        	InstanceCookie ic = super.acceptDataObject (dob);
        	if (ic == null) {
            	    JMenuItem item = ExecBridge.createMenuItem (dob);
            	    return item != null ? new InstanceSupport.Instance (item) : null;
        	} else {
            	    return ic;
        	}
    	    }

    	    /**
             * Accepts only cookies that can provide <code>Menu</code>.
             * @param cookie an <code>InstanceCookie</code> to test
             * @return true if the cookie can provide accepted instances
             */
    	    protected InstanceCookie acceptCookie(InstanceCookie cookie)
    	    throws IOException, ClassNotFoundException {
		// [pnejedly] Don't try to optimize this by InstanceCookie.Of
		// It will load the classes few ms later from instanceCreate
		// anyway and more instanceOf calls take longer
            	Class c = cookie.instanceClass();
            	boolean is =
                	Presenter.Menu.class.isAssignableFrom (c) ||
                	JMenuItem.class.isAssignableFrom (c) ||
                	JSeparator.class.isAssignableFrom (c) ||
                	Action.class.isAssignableFrom (c);
        	return is ? cookie : null;
    	    }

    	    /**
    	     * Returns a <code>Menu.Folder</code> cookie for the specified
    	     * <code>DataFolder</code>.
             * @param df a <code>DataFolder</code> to create the cookie for
    	     * @return a <code>Menu.Folder</code> for the specified folder
    	     */
    	    protected InstanceCookie acceptFolder(DataFolder df) {
		return new InstanceSupport.Instance(new LazyMenu(df, true));
    	    }

    	    /** Updates the <code>JMenu</code> represented by this folder.
    	     * @param cookies array of instance cookies for the folder
    	     * @return the updated <code>JMenu</code> representee
    	     */
    	    protected Object createInstance(InstanceCookie[] cookies)
    			    throws IOException, ClassNotFoundException {
		JMenu m = LazyMenu.this;

        	//synchronized (this) { // see #15917 - attachment from 2001/09/27
        	LinkedList cInstances = new LinkedList();
        	allInstances (cookies, cInstances);

        	m.removeAll();

        	// #11848, #13013. Enablement should be set immediatelly,
        	// popup will be created on-demand.
        	// m.setEnabled(!cInstances.isEmpty());
		// TODO: fill it with empty sign instead
		if(cInstances.isEmpty()) {
		    JMenuItem item = new JMenuItem(
                            NbBundle.getMessage(MenuBar.class, "CTL_EmptyMenu"));

		    item.setEnabled(false);
		    m.add(item);
		}

	    
        	// clear first - refresh the menu's content
        	boolean addSeparator = false;

        	// sync to prevent from concurrent modifications of
        	// cookie instances list 
        	Iterator it = cInstances.iterator();
        	while (it.hasNext()) {
            	    Object obj = it.next();

            	    if (obj instanceof Presenter.Menu) {
                	obj = ((Presenter.Menu)obj).getMenuPresenter();
            	    }
		
		    if (obj instanceof JMenuItem) {
                	if(addSeparator) {
                	    m.addSeparator();
                    	    addSeparator = false;
                	}
            		m.add((JMenuItem)obj);
            	    } else if (obj instanceof JSeparator) {
                	addSeparator = getMenuComponentCount() > 0;
            	    } else if (obj instanceof Action) {
                	if(addSeparator) {
                	    m.addSeparator();
                    	    addSeparator = false;
                	}
                	Action a = (Action)obj;
                	JMenuItem item = new JMenuItem ();
                	Actions.connect (item, a, false);
                	m.add (item);
            	    }
        	}
        	return m;
    	    }

    	    /** Recreate the instance in AWT thread.
    	     */
    	    protected Task postCreationTask (Runnable run) {
        	return new AWTTask (run);
    	    }
	}
    }
}
