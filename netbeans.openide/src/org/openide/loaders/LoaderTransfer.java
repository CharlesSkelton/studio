/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.loaders;

import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.text.MessageFormat;

import org.openide.ErrorManager;
import org.openide.util.datatransfer.*;

/** Class that contains specific datatransfer flavors and methods to work with
 * transfered DataObjects. There are flavors to allow a DataObject
 * to be copied or cut into clipboard, and to retrieve them from clipboard
 * when implementing paste operation.
 * <p>This is a dummy utility class--no instances are possible.
 *
 * @author  Vita Stejskal
 * @since 1.21
 */
public abstract class LoaderTransfer {

    /** Creates new LoaderTransfer */
    private LoaderTransfer () {}
    
    /** Constants for drag-n-drop operations.
    * Are exactly the same as constants
    * in {@link DnDConstants}.
    */
    public static final int DND_NONE = DnDConstants.ACTION_NONE;
    public static final int DND_COPY = DnDConstants.ACTION_COPY;
    public static final int DND_MOVE = DnDConstants.ACTION_MOVE;
    public static final int DND_COPY_OR_MOVE = DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE;
    public static final int DND_LINK = DnDConstants.ACTION_LINK;
    public static final int DND_REFERENCE = DnDConstants.ACTION_LINK;

    /** Constant indicating copying to the clipboard.
    * Equal to {@link #DND_COPY}, because
    * copy to clipboard and d'n'd copy should be the same.
    */
    public static final int CLIPBOARD_COPY = DND_COPY;

    /** Constant indicating cutting to the clipboard.
    */
    public static final int CLIPBOARD_CUT = 0x04;

    /** Generic mask for copying DataObjects (do not destroy the original).
    * Equal to {@link #CLIPBOARD_COPY} or {@link #DND_COPY}.
    */
    public static final int COPY = CLIPBOARD_COPY | DND_COPY;

    /** Generic mask for moving DataObjects (destroy the original).
    * Equal to {@link #CLIPBOARD_CUT} or {@link #DND_MOVE}.
    */
    public static final int MOVE = CLIPBOARD_CUT | DND_MOVE;
    

    /** message format to create and parse the mimetype
    */
    private static MessageFormat dndMimeType = new MessageFormat (
                "application/x-java-openide-dataobjectdnd;class=org.openide.loaders.DataObject;mask={0}" // NOI18N
            );
    
    /** Creates transferable that represents an operation, such as cut-to-clipboard.
    * The transferable will be recognizable by {@link #getDataObject} and {@link #getDataObjects}.
    *
    * @param d the DataObject to create a transferable for
    * @param actions the action performed on the DataObject's node
    * @return the transferable 
    */
    public static ExTransferable.Single transferable (final DataObject d, int actions) {
        return new ExTransferable.Single (createDndFlavor (actions)) {
                   public Object getData () {
                       return d;
                   }
               };
    }
    
    /** Obtain a DataObject from a transferable.
    * Probes the transferable in case it includes a flavor corresponding
    * to a DataObject's node operation (which you must specify a mask for).
    *
    * @param t transferable
    * @param action one of the <code>DND_*</code> or <code>CLIPBOARD_*</code> constants
    * @return the DataObject or <code>null</code>
    */
    public static DataObject getDataObject (Transferable t, int action) {
        DataFlavor[] flavors = t.getTransferDataFlavors ();
        if (flavors == null) {
            return null;
        }
        int len = flavors.length;

        String subtype = "x-java-openide-dataobjectdnd"; // NOI18N
        String primary = "application"; // NOI18N
        String mask = "mask"; // NOI18N

        for (int i = 0; i < len; i++) {
            DataFlavor df = flavors[i];
            
            if (
                df.getSubType ().equals (subtype) &&
                df.getPrimaryType ().equals (primary)
            ) {
                try {
                    int m = Integer.valueOf (df.getParameter (mask)).intValue ();
                    if ((m & action) != 0) {
                        // found the node
                        DataObject o = (DataObject)t.getTransferData(df);
                        if (o.isValid()) {
                            return o;
                        } else {
                            // #14344
                            return null;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    maybeReportException (nfe);
                } catch (ClassCastException cce) {
                    maybeReportException (cce);
                } catch (IOException ioe) {
                    maybeReportException (ioe);
                } catch (UnsupportedFlavorException ufe) {
                    maybeReportException (ufe);
                }
            }
        }

        return null;
    }
    
    /** Obtain a list of DataObjects from a transferable.
    * If there is only a single DataObject in the transferable, this will just return a singleton
    * array like {@link #getDataObject}.
    * If there is a {@link ExTransferable#multiFlavor multiple transfer} (of at least one element),
    * each element of which
    * contains a DataObject, then an array of these will be returned.
    * If neither of these things is true, <code>null</code> will be returned.
    * <p>This is a convenience method intended for those who wish to specially support pastes
    * of multiple DataObjects at once.
    * @param t the transferable to probe
    * @param action a DnD or clipboard constant
    * @return a non-empty array of nodes, or <code>null</code>
    */
    public static DataObject[] getDataObjects (Transferable t, int action) {
        try {
            if (t.isDataFlavorSupported (ExTransferable.multiFlavor)) {
                MultiTransferObject mto = (MultiTransferObject) t.getTransferData (ExTransferable.multiFlavor);
                int count = mto.getCount ();
                DataObject[] datas = new DataObject[count];
                boolean ok = true;
                for (int i = 0; i < count; i++) {
                    DataObject d = getDataObject (mto.getTransferableAt (i), action);
                    if (d == null) {
                        ok = false;
                        break;
                    } else {
                        datas[i] = d;
                    }
                }
                if (ok && count > 0) return datas;
            } else {
                DataObject d = getDataObject (t, action);
                if (d != null) return new DataObject[] { d };
            }
        } catch (ClassCastException cce) {
            maybeReportException (cce);
        } catch (IOException ioe) {
            maybeReportException (ioe);
        } catch (UnsupportedFlavorException ufe) {
            maybeReportException (ufe);
        }
        return null;
    }
    
    /** Creates data flavor for given mask of dnd actions.
    * @param actions any mask of dnd constants DND_* and CLIPBOARD_*
    */
    private static DataFlavor createDndFlavor (int actions) {
        try {
            return new DataFlavor (dndMimeType.format (new Object[] { new Integer (actions) }));
        } catch (ClassNotFoundException ex) {
            throw new InternalError (ex.toString());
        }
    }

    /** Print a stack trace if debugging is on.
    * Used for exceptions that could occur when probing transferables,
    * which should not interrupt the probing with an error, but
    * indicate a bug elsewhere and should be reported somehow.
    * @param e the exception
    */
    private static void maybeReportException (Exception e) {
        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
    }
}
