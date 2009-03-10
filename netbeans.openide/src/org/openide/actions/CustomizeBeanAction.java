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

package org.openide.actions;

import java.util.ResourceBundle;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.Rectangle;
import java.awt.event.*;
import java.beans.IntrospectionException;
import java.io.*;
import java.lang.reflect.Method;
import javax.swing.*;

import org.openide.*;
import org.openide.loaders.*;
import org.openide.cookies.*;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileLock;
import org.openide.explorer.propertysheet.*;
import org.openide.util.*;
import org.openide.util.actions.CookieAction;
import org.openide.nodes.*;

/** Customize a JavaBean.
* Opens a Property Sheet and allows making a
* serialized prototype from the modified object.
* <p>This class is final only for performance reasons.
*
* @author   Ian Formanek
*/
public final class CustomizeBeanAction extends CookieAction {

    /** constant for HelpCtx Customize Bean */
    private static final String HELP_KEY_CUSTOMIZING = "beans.customizing"; // NOI18N
    
    /** generated Serialized Version UID */
    static final long serialVersionUID = -6378495195905487716L;

    /* Actually performs this action */
    protected void performAction (final Node[] activatedNodes) {
        if (compileNodes (activatedNodes)) {
            customize ((InstanceCookie)activatedNodes[0].getCookie(InstanceCookie.class));
        }
    }

    
    
    /** Execute some data objects.
    *
    * @param nodes the array of nodes to compile
    * @return true if compilation succeeded or was not performed, false if compilation failed
     * @deprecated Use <code>AbstractCompileAction.compileNodes</code> (Compiler API) instead.
    */
    public static boolean compileNodes(Node[] nodes) {
        try {
            Class c = ((ClassLoader)Lookup.getDefault().lookup(ClassLoader.class)).loadClass("org.openide.actions.AbstractCompileAction"); // NOI18N
            Method m = c.getDeclaredMethod("compileNodes", new Class[] {Node[].class}); // NOI18N
            return ((Boolean)m.invoke(null, new Object[] {nodes})).booleanValue();
        } catch (ClassNotFoundException e) {
            // Failed, but not implausible.
            return false;
        } catch (Exception e) {
            // Something else wrong.
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            return false;
        }
    }

    /* Exactly one selected node.
    */
    protected int mode () {
        return MODE_EXACTLY_ONE;
    }

    /* @return InstanceCookie node
    */
    protected Class[] cookieClasses () {
        return new Class[] { InstanceCookie.class };
    }


    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName () {
        return NbBundle.getMessage(CustomizeBeanAction.class, "CustomizeBean");
    }

    /* Help context where to find more about the acion.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (CustomizeBeanAction.class);
    }

    /* Resource name for the icon.
    * @return resource name
    */
    protected String resourceIcon () {
        return "/org/openide/resources/actions/customize.gif"; // NOI18N
    }

    /** Customize a Bean.
    * @param cookie the object which can be instantiated
    */
    public static void customize (final InstanceCookie cookie) {
        if (cookie == null) return;

        final ResourceBundle bundle = NbBundle.getBundle(CustomizeBeanAction.class);

        Window w = null; // Visual rep. of bean
        Object b = null; // BEAN
        try {
            b = cookie.instanceCreate ();
        } catch (Exception ex) {
            ErrorManager em = ErrorManager.getDefault ();
            em.annotate (ex, bundle.getString ("EXC_IntrospectionNoClass"));
            em.notify (ErrorManager.WARNING, ex);
            return;
        }
        final Object bean = b;

        // show visual repres. of bean
        if (bean instanceof java.awt.Window) {
            w = (java.awt.Window)bean;
            w.addWindowListener (new java.awt.event.WindowAdapter () {
                                     public void windowClosing(java.awt.event.WindowEvent e) {
                                         e.getWindow ().dispose ();
                                     }
                                 });
        } else
            if (bean instanceof java.awt.Component) {
                // JST: do not use PropertyDialogManager
                //      just create the window with some reasonable title
                DialogDescriptor dd = new DialogDescriptor (
                    (java.awt.Component)bean,
                    bundle.getString ("CTL_Component_Title"),
                    false,
                    null
                );
                dd.setOptions(new Object[] { DialogDescriptor.CLOSED_OPTION });
                dd.setHelpCtx( new HelpCtx( HELP_KEY_CUSTOMIZING )); 
                
                w = DialogDisplayer.getDefault ().createDialog (dd);
            }

        final Window window = w;

        // create propertysheet
        PropertySheet propertySheet = new PropertySheet ();
        Node[] nodes = new Node [1];
        BeanNode bn = null;
        try {
            bn = new BeanNode(bean);
        } catch (IntrospectionException e) {
            ErrorManager em = ErrorManager.getDefault ();
            em.annotate (e, NbBundle.getMessage (
                CustomizeBeanAction.class, "EXC_Introspection", bean.getClass ().getName ()
            ));
            em.notify (ErrorManager.WARNING, e);
            return;
        }
        nodes [0] = bn;
        propertySheet.setNodes (nodes);

        final JButton ser = new JButton (bundle.getString ("CTL_Serialize"));
        final JButton serAs = new JButton (bundle.getString ("CTL_SerializeAs"));
        final JButton cancel = new JButton (bundle.getString ("CTL_Cancel"));
        
        serAs.setMnemonic(bundle.getString ("CTL_SerializeAs_Mnemonic").charAt(0));
        
        ser.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_Serialize"));
        serAs.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_SerializeAs"));
        cancel.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_Cancel"));
        propertySheet.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_CustomizeBeanPanel"));

        // dialog[0] = opened dialog
        final Dialog dialog[] = new Dialog[1];

        boolean brr = java.io.Serializable.class.isAssignableFrom (bean.getClass ());
        serAs.setEnabled (brr);
        ser.setEnabled (brr && cookie instanceof InstanceCookie.Origin);

        ActionListener listener = new ActionListener () {
                                      public void actionPerformed (ActionEvent ev) {
                                          if (ev.getSource () == cancel || 
                                                 ev.getSource() == NotifyDescriptor.CANCEL_OPTION || 
                                                 ev.getSource() == NotifyDescriptor.CLOSED_OPTION ) {
                                              dialog[0].dispose ();
                                              if (window != null) window.dispose ();
                                              return;
                                          }
                                          if (serializeJavaBean (bean,
                                                                 ev.getSource () == serAs ? null : ((InstanceCookie.Origin)cookie).instanceOrigin ()
                                                                )) {
                                              dialog[0].dispose ();
                                              if (window != null) window.dispose ();
                                          }
                                      }
                                  };

        DialogDescriptor descr = new DialogDescriptor (
                                     propertySheet,
                                     NbBundle.getMessage(CustomizeBeanAction.class, "FMT_CTL_CustomizeTitle", bean.getClass().getName()),
                                     false, // modal
                                     new Object[] { ser, serAs, cancel }, // options
                                     cancel, // initial value
                                     DialogDescriptor.DEFAULT_ALIGN,
                                     new HelpCtx ( HELP_KEY_CUSTOMIZING ), 
                                     listener
                                 );


        dialog[0] = DialogDisplayer.getDefault ().createDialog (descr);
        
        // add listener to close bean window  
      
              
        CustomizeWindowAdapter wa = new CustomizeWindowAdapter (window); 
            
             
        dialog[0].addWindowListener(wa);
        dialog[0].show ();
      
        synchronized (wa) { // monitor to  CustomizeWindowAdapter.windowClosed           
            if (window != null && ! wa.closedBeenWindow) {
                Rectangle r = dialog [0].getBounds ();
                window.setLocation (r.x + r.width, r.y);
                window.show ();
                wa.closedBeenWindow = true;
            }
        }
    }

    /** Serialize a bean to file.
    *
    * @param bean the bean to be serialized
    * @param file the file to serialize to, or <code>null</code> to prompt the user for a destination
    * @return <code>true</code> if successful
    */
    public static boolean serializeJavaBean (final Object bean,
            final FileObject file) {
        final ResourceBundle bundle = NbBundle.getBundle(CustomizeBeanAction.class);
        FileObject parent = null;
        String name = null;
        org.openide.filesystems.FileSystem targetFS;

        try {
            if (file == null) {
                JPanel p = new JPanel(new java.awt.BorderLayout(12, 0));
                JTextField tf = new JTextField (20);
                JLabel l = new JLabel(bundle.getString("CTL_SerializeTarget"));
                l.setDisplayedMnemonic(bundle.getString("CTL_SerializeTarget_Mnemonic").charAt(0));
                l.setLabelFor(tf);
                p.add(tf, java.awt.BorderLayout.CENTER);
                p.add(l, java.awt.BorderLayout.WEST);
                
                tf.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_SerializeTarget"));
                try {
                    // selects one folder from data systems
                    DataFolder df = (DataFolder)NodeOperation.getDefault().select (
                                        bundle.getString ("CTL_SerializeAs"),
                                        bundle.getString ("CTL_SaveIn"),
                                        RepositoryNodeFactory.getDefault().repository(new FolderFilter()),
                                        new FolderAcceptor(),
                                        p
                                    )[0].getCookie(DataFolder.class);
                    parent = df.getPrimaryFile ();
                    targetFS = parent.getFileSystem ();
                    name = tf.getText ();
                } catch (org.openide.util.UserCancelException ex) {
                    return false;
                }
            } else {
                parent = file.getParent ();
                name = file.getName ();
                targetFS = file.getFileSystem ();
            }
        } catch (org.openide.filesystems.FileStateInvalidException e) {
            // XXX I18N violation to separate strings this way - use format instead
            ErrorManager.getDefault().annotate(e, bundle.getString ("EXC_Serialization") + " " + name);
            ErrorManager.getDefault().notify(e);
            return false;
        }

        final String fileName = name;
        final FileObject parentFile = parent;
        try {
            targetFS.runAtomicAction(new org.openide.filesystems.FileSystem.AtomicAction() {
                                         public void run() throws IOException {
                                             ByteArrayOutputStream baos = null;
                                             ObjectOutputStream oos = null;
                                             OutputStream os = null;
                                             FileObject serFile = null;
                                             FileLock lock = null;
                                             try {
                                                 oos = new java.io.ObjectOutputStream (baos = new ByteArrayOutputStream ());
                                                 oos.writeObject (bean);
                                                 if ((serFile = parentFile.getFileObject (fileName, "ser")) == null) // NOI18N
                                                     serFile = parentFile.createData (fileName, "ser"); // NOI18N
                                                 lock = serFile.lock ();
                                                 oos.close ();
                                                 baos.writeTo (os = serFile.getOutputStream (lock));
                                             }
                                             finally {
                                                 if (lock != null) lock.releaseLock ();
                                                 if (os != null) os.close ();
                                             }
                                         }
                                     }
                                    );
        } catch (Exception e) {
            ErrorManager.getDefault().annotate(e, 
                NbBundle.getMessage(CustomizeBeanAction.class, "EXC_Serialization2", bean.getClass().getName()));
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, e);
            
            return false;
        }

        return true;
    }

    /** Filter for save as operation, accepts folders. */
    private static final class FolderFilter implements DataFilter {
        static final long serialVersionUID =6754682007992329276L;
        FolderFilter() {}
        /** Accepts only data folders but ignore read only roots of file systems
        */
        public boolean acceptDataObject (DataObject obj) {
            return obj instanceof DataFolder &&
                   (!obj.getPrimaryFile ().isReadOnly () ||
                    obj.getPrimaryFile ().getParent () != null);
        }
    } // end of FolderFilter inner class

    /** Node acceptor that accepts read-write folders only */
    private static final class FolderAcceptor implements NodeAcceptor {
        FolderAcceptor() {}
        public boolean acceptNodes (Node[] nodes) {
            if ((nodes == null) || (nodes.length == 0)) return false;
            DataFolder cookie =
                (DataFolder)nodes[0].getCookie (DataFolder.class);
            return nodes.length == 1 && cookie != null &&
                   !cookie.getPrimaryFile().isReadOnly();
        }
    } // end of FolderAcceptor inner class

    
    /** Window adapter for customize dialog. */
    private static class CustomizeWindowAdapter  extends WindowAdapter{
        /** when been is instance of window it detect its closing */
    
         public  boolean closedBeenWindow = false;
         /** window of beans */
         private Window window;
         
         /** @param window is window of been.*/
         public CustomizeWindowAdapter (Window window) {
             this.window = window;
         }
             
         /** Customize dialog closed. */
         public void windowClosed (WindowEvent we) {
            synchronized (this) { // monitor to CustomizeBeanAction.customize 
                if (window != null ) {
                   window.dispose();
                   closedBeenWindow = true;
                }
            }
        }
    }
    
}
