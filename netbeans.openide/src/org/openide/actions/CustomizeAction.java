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

package org.openide.actions;

import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.util.NbBundle;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOperation;

import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/** Customize a node (rather than using its property sheet).
* @see NodeOperation#customize
* @author   Ian Formanek, Jan Jancura
*/
public class CustomizeAction extends NodeAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -5135850155902185598L;

    protected void performAction (Node[] activatedNodes) {
        try {
            Class c = Class.forName("org.openide.actions.CustomizeAction$CustomizationInvoker"); // NOI18N
            Runnable r = (Runnable) c.newInstance();
            selNodes.set(activatedNodes);
            r.run();
            return;
        } catch (Exception e) {
            // if something went wrong just
            // resort to swing (IDE probably not present)
        } catch (LinkageError e) {
        }

        if (activatedNodes == null) {
            throw new IllegalStateException();
        }
        final JDialog d = new JDialog();
        d.getContentPane().setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 11));
        p.add(activatedNodes[0].getCustomizer(), BorderLayout.CENTER);
        JButton b = new JButton(NbBundle.getBundle(CustomizeAction.class).getString("CloseView"));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                d.setVisible(false);
                d.dispose();
            }
        });
        p.add(b, BorderLayout.SOUTH);
        d.getContentPane().add(p, BorderLayout.CENTER);
        d.pack();
        d.show();
        d.dispose();
    }

    protected boolean enable (Node[] activatedNodes) {
        if ((activatedNodes == null) || (activatedNodes.length != 1)) return false;
        return activatedNodes [0].hasCustomizer ();
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName() {
        return NbBundle.getMessage(CustomizeAction.class, "Customize");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx() {
        return new HelpCtx (CustomizeAction.class);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/propertysheet/customize.gif"; // NOI18N
    }
    
    static ThreadLocal selNodes = new ThreadLocal();
    
    
    /**
     * Class separating a reference to T opManager. If
     * the IDE is not present this class won't be loaded.
     */
    static class CustomizationInvoker implements Runnable {
        public void run() {
            Node[] nodes = (Node[]) selNodes.get();
            selNodes.set(null);
            if (nodes == null) {
                throw new IllegalStateException();
            }
            if (nodes.length == 1) {
                NodeOperation.getDefault().customize(nodes[0]);
            }
        }
    }

    
}
