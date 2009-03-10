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

package org.openide.explorer.view;

import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.Point;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.openide.DialogDisplayer;

import org.openide.awt.JPopupMenuPlus;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.datatransfer.*;
import org.openide.util.UserCancelException;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/** Class that provides methods for common tasks needed during
* drag and drop when working with explorer views.
*
* @author Dafe Simonek
*/
final class DragDropUtilities extends Object {

    static final boolean dragAndDropEnabled = isDragAndDropEnabled();
    
    static final int NODE_UP = -1;
    static final int NODE_CENTRAL = 0;
    static final int NODE_DOWN = 1;    
    static final Point CURSOR_CENTRAL_POINT = new Point (10, 10);
    static Runnable postDropRun = null;
    
    /** No need to instantiate this class */
    private DragDropUtilities() {
    }

    // helper constants
    static final int NoDrag = 0;
    static final int NoDrop = 1;
    
    //static final int Modifiers4Move = 
    
    /**
     * Checks system property netbeans.dnd.enabled. If it is not
     * present return false for JDK1.3 and true for JDK1.4 and newer.
     */
    private static boolean isDragAndDropEnabled() {
        // This should say: JDk1.4 and newer
        if (! System.getProperty("java.version").startsWith("1.3")) {
            // if jdk1.4(or newer) then dnd enabled as default
            if (System.getProperty("netbeans.dnd.enabled") != null) { // NOI18N
                return Boolean.getBoolean("netbeans.dnd.enabled"); // NOI18N
            } else {
                return true;
            }
        }
        return false;
    }
    
    /** Utility method - chooses and returns right cursor
    * for given user drag action.
    */
    static Cursor chooseCursor (int dragAction, boolean canDrop) {
        //System.out.print("------> chooseCursor(action: "+dragAction+", can? "+canDrop+")");
        // if the node does not provide icon use system default
        Image image;
        String name;
        try {
            switch (dragAction) {
            case DnDConstants.ACTION_COPY:
            case DnDConstants.ACTION_COPY_OR_MOVE:
                if (canDrop) {
                    image = Utilities.loadImage(
                        "org/openide/resources/cursorscopysingle.gif"); // NOI18N
                    name = "ACTION_COPY"; // NOI18N
                } else {
                    image = Utilities.loadImage(
                        "org/openide/resources/cursorsnone.gif"); // NOI18N
                    name = "NO_ACTION_COPY"; // NOI18N
                }
                break;
            case DnDConstants.ACTION_MOVE:
                if (canDrop) {
                    image = Utilities.loadImage(
                        "org/openide/resources/cursorsmovesingle.gif"); // NOI18N
                    name = "ACTION_MOVE"; // NOI18N
                } else {
                    image = Utilities.loadImage(
                        "org/openide/resources/cursorsnone.gif"); // NOI18N
                    name = "NO_ACTION_MOVE"; // NOI18N
                }
                break;
            case DnDConstants.ACTION_LINK:
                if (canDrop) {
                    image = Utilities.loadImage(
                        "org/openide/resources/cursorsunknownsingle.gif"); // NOI18N
                    name = "ACTION_LINK"; // NOI18N
                } else {
                    image = Utilities.loadImage(
                        "org/openide/resources/cursorsnone.gif"); // NOI18N
                    name = "NO_ACTION_LINK"; // NOI18N
                }
                break;
            default:
                image = Utilities.loadImage(
                    "org/openide/resources/cursorsnone.gif"); // NOI18N
                name = "ACTION_NONE"; // NOI18N
                break;
            }
            //System.out.println("--> "+image.getSource());
            return createCustomCursor(image, name);
        } catch (Exception ex) {
            ErrorManager.getDefault ().notify (ex);
        }
        return DragSource.DefaultMoveNoDrop;
    }

    /**
     * Returns cursor created from given icon.
     */
    private static Cursor createCustomCursor(Image icon, String name) {
        Toolkit t = Toolkit.getDefaultToolkit();
        Dimension d = t.getBestCursorSize(16, 16);
        Image i = icon;
        if (d.width != icon.getWidth(null)) {
            // need to resize the icon
            Image empty = createBufferedImage(d.width, d.height);
            i = Utilities.mergeImages(icon, empty, 0, 0);
        }
        return t.createCustomCursor(i, new Point(1,1), name);
    }
    
    /** 
     * Creates BufferedImage and Transparency.BITMASK 
     * Note: this method is copied from org.openide.util.IconManager. Should
     * it be exposed in Utilities? I don't know (dstrupl).
     */
    private static final java.awt.image.BufferedImage createBufferedImage(int width, int height) {
        java.awt.image.ColorModel model = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
                                          getDefaultScreenDevice().getDefaultConfiguration().getColorModel(java.awt.Transparency.BITMASK);
        java.awt.image.BufferedImage buffImage = new java.awt.image.BufferedImage(model,
                model.createCompatibleWritableRaster(width, height), model.isAlphaPremultiplied(), null);
        return buffImage;
    }

    /** Utility method.
    * @return true if given node supports given action,
    * false otherwise.
    */
    static boolean checkNodeForAction (Node node, int dragAction) {
        if (node.canCut() &&
                ((dragAction == DnDConstants.ACTION_MOVE) ||
                 (dragAction == DnDConstants.ACTION_COPY_OR_MOVE)))
            return true;
        if (node.canCopy() &&
                ((dragAction == DnDConstants.ACTION_COPY) ||
                 (dragAction == DnDConstants.ACTION_COPY_OR_MOVE) ||
                 (dragAction == DnDConstants.ACTION_LINK) ||
                 (dragAction == DnDConstants.ACTION_REFERENCE)))
            return true;
        // hmmm, conditions not satisfied..
        return false;
    }

    /** Gets right transferable of given nodes (according to given
    * drag action) and also converts the transferable.<br>
    * Can be called only with correct action constant.
    * @return The transferable.
    */
    static Transferable getNodeTransferable (Node[] nodes, int dragAction)
    throws IOException {
        Transferable[] tArray = new Transferable[nodes.length];
        //System.out.println("Sel count: " + nodes.length); // NOI18N
        for (int i = 0; i < nodes.length; i++) {
            Clipboard c = getClipboard();
            if (c instanceof ExClipboard) {
                ExClipboard cb = (ExClipboard)c;
                if (dragAction == DnDConstants.ACTION_MOVE) {
                    tArray[i] = cb.convert (nodes[i].clipboardCut());
                    //System.out.println("Clipboard CUT for node: "+nodes[0]);
                } else {
                    tArray[i] = cb.convert (nodes[i].clipboardCopy());
                    //System.out.println("Clipboard COPY for node: "+nodes[0]);
                }
            } else {
                // In case of standalone library we cannot do
                // conversion here. Is this ok?
                if (dragAction == DnDConstants.ACTION_MOVE) {
                    tArray[i] = nodes[i].clipboardCut();
                    //System.out.println("Clipboard CUT for node: "+nodes[0]);
                } else {
                    tArray[i] = nodes[i].clipboardCopy();
                    //System.out.println("Clipboard COPY for node: "+nodes[0]);
                }
            }
        }
        if (tArray.length == 1)
            // only one node, so return regular single transferable
            return tArray[0];
        // enclose the transferables into multi transferable
        return new ExTransferable.Multi(tArray);
    }

    /** Returns transferable of given node
    * @return The transferable.
    */
    static Transferable getNodeTransferable (Node node, int dragAction)
    throws IOException {
        return getNodeTransferable(new Node[] { node }, dragAction);
    }
    
    
    /** Sets a runnable it will be executed after drop action is performed.
     * @param run a runnable for execution */    
    static void setPostDropRun (Runnable run) {
        postDropRun = run;
    }
    
    /* Invokes the stored runnable if it is there and than set to null.
     */
    static private void invokePostDropRun () {
        if (postDropRun!=null) {
            SwingUtilities.invokeLater (postDropRun);
            postDropRun = null;
        }
    }

    /** Performs the drop. Performs paste on given paste type.
    */
    static void performDrop (PasteType type) {
        //System.out.println("performing drop...."+type); // NOI18N
        try {
            Transferable trans = type.paste();
            /*Clipboard clipboard = T opManager.getDefault().getClipboard();
            if (trans != null) {
              ClipboardOwner owner = trans instanceof ClipboardOwner ?
                (ClipboardOwner)trans
              :
                new StringSelection ("");
              clipboard.setContents(trans, owner);
        }*/
        } catch (UserCancelException exc) {
            // ignore - user just pressed cancel in some dialog....
        } catch (java.io.IOException e) {
            ErrorManager.getDefault ().notify(e);
        }
    }
    
    /** Returns array of paste types for given transferable.
    * If given transferable contains multiple transferables,
    * multi paste type which encloses pate types of all contained
    * transferables is returned.
    * Returns empty array if given node did not accepted the transferable
    * (or some sub-transferables in multi transferable)
    * 
    * @param node given node to ask fro paste types
    * @param trans transferable to discover
    */
    static PasteType[] getPasteTypes (Node node, Transferable trans) {
        // find out if given transferable is multi
        boolean isMulti = false;
        try {
            isMulti = trans.isDataFlavorSupported(ExTransferable.multiFlavor);
        } catch (Exception e) {
            // patch to get the Netbeans start under Solaris
            // [PENDINGworkaround]
        }
        if (!isMulti) {
            // only single, so return paste types
            PasteType [] pt = null;
            try {
                pt = node.getPasteTypes(trans);
            } catch (NullPointerException npe) {
                ErrorManager.getDefault ().notify (npe);
                // there are not paste types
            }
            return pt;
        } else {
            // multi transferable, we must do extra work
            try {
                MultiTransferObject obj = (MultiTransferObject)
                                          trans.getTransferData(ExTransferable.multiFlavor);
                int count = obj.getCount();
                Transferable[] t = new Transferable[count];
                PasteType[] p = new PasteType[count];
                PasteType[] curTypes = null;
                // extract default paste types of transferables
                for (int i = 0; i < count; i++) {
                    t[i] = obj.getTransferableAt(i);
                    curTypes = node.getPasteTypes(t[i]);
                    // return if not accepted
                    if (curTypes.length == 0)
                        return curTypes;
                    p[i] = curTypes[0];
                }
                // return new multi paste type
                return new PasteType[] { new MultiPasteType(t, p) };
            } catch (UnsupportedFlavorException e) {
                // ignore and return empty array
            }
            catch (IOException e) {
                // ignore and return empty array
            }
        }
        return new PasteType[0];
    }

    /** Notifies user that the drop was not succesfull. */
    static void dropNotSuccesfull () {
        DialogDisplayer.getDefault().notify(
            new NotifyDescriptor.Message(
            NbBundle.getBundle(TreeViewDropSupport.class).
                getString("MSG_NoPasteTypes"),
            NotifyDescriptor.WARNING_MESSAGE)
        );
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
    static final class MultiPasteType extends PasteType {

        // Attributes

        /** Array of transferables */
        Transferable[] t;
        /** Array of paste types */
        PasteType[] p;

        // Operations

        /** Constructs new MultiPasteType for the given
        * transferables and paste types.*/
        MultiPasteType(Transferable[] t, PasteType[] p) {
            this.t = t;
            this.p = p;
        }

        /** Performs the paste action.
        * @return Transferable which should be inserted into the
        *   clipboard after paste action. It can be null, which means
        *   that clipboard content should be cleared.
        */
        public Transferable paste() throws IOException {
            int size = p.length;
            Transferable[] arr = new Transferable[size];
            // perform paste for all source transferables
            for (int i = 0; i < size; i++) {
                //System.out.println("Pasting #" + i); // NOI18N
                arr[i] = p[i].paste();
            }
            return new ExTransferable.Multi(arr);
        }
    } // end of MultiPasteType

    /** Utility method created by Enno Sandner. Is it needed?
     * I don't know (dstrupl).
     */
    static Node secureFindNode(Object o) {
        try {
            return Visualizer.findNode(o);
        }
        catch (ClassCastException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and populates popup as a result of
     * dropping an item.
     * @author Enno Sandner
     */
    static JPopupMenu createDropFinishPopup(final TreeSet pasteTypes) {
        
        JPopupMenu menu = new JPopupMenuPlus();
        //System.arraycopy(pasteTypes, 0, pasteTypes_, 0, pasteTypes.length);
        final JMenuItem[] items_ = new JMenuItem[pasteTypes.size ()];
        
        ActionListener aListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JMenuItem source = (JMenuItem)e.getSource();

                final Iterator it = pasteTypes.iterator ();
                for (int i=0; it.hasNext(); i++) {
                    PasteType action = (PasteType)it.next();
                    if (items_[i].equals(source)) {
                        DragDropUtilities.performDrop(action);
                        invokePostDropRun ();
                        break;
                    }
                }
            }
        };
           
        Iterator it = pasteTypes.iterator ();
        for (int i=0; it.hasNext(); i++) {
            items_[i]=new JMenuItem(((PasteType)it.next()).getName ());
            items_[i].addActionListener(aListener);
            menu.add(items_[i]);
        }
           
        menu.addSeparator();
           
        JMenuItem abortItem=new JMenuItem(NbBundle.getBundle(DragDropUtilities.class).getString("MSG_ABORT"));
        menu.add(abortItem);
        return menu;
    }
}
