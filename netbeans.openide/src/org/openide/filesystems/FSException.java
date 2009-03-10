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

package org.openide.filesystems;

import java.io.IOException;
import org.openide.ErrorManager;

/** Localized IOException for filesystems.
*
* @author Jaroslav Tulach
*/
final class FSException extends IOException {
    /** name of resource to use for localized message */
    //  private String resource;
    /** arguments to pass to the resource */
    private Object[] args;

    /** Creates new FSException. */
    private FSException (String resource, Object[] args) {
        super (resource);
        this.args = args;
    }

    /** Message should be meaning full, but different from localized one.
    */
    public String getMessage () {
        return " " + getLocalizedMessage (); // NOI18N
    }

    /** Localized message.
    */
    public String getLocalizedMessage () {
        String res = super.getMessage ();
        String format = FileSystem.getString (res);

        if (args != null) {
            return java.text.MessageFormat.format (format, args);
        } else {
            return format;
        }
    }

    /** Creates the localized exception.
    * @param resource to take localization string from
    * @exception the exception
    */
    public static void io (String resource) throws IOException {        
        FSException fsExc = new FSException (resource, null);
        ErrorManager.getDefault().annotate (fsExc, ErrorManager.WARNING, null, 
        fsExc.getLocalizedMessage(), null, null);        
        throw fsExc;        
    }

    public static void io (String resource, Object[] args) throws IOException {        
        FSException fsExc = new FSException (resource, args);
        ErrorManager.getDefault().annotate (fsExc, ErrorManager.WARNING, null, 
        fsExc.getLocalizedMessage(), null, null);        
        throw fsExc;        
    }

    public static void io (String resource, Object arg1) throws IOException {
        FSException fsExc = new FSException (resource, new Object[] { arg1 });
        ErrorManager.getDefault().annotate (fsExc, ErrorManager.WARNING, null, 
        fsExc.getLocalizedMessage(), null, null);        
        throw fsExc;
    }

    public static void io (
        String resource, Object arg1, Object arg2
    ) throws IOException {        
        FSException fsExc = new FSException (resource, new Object[] { arg1, arg2 });
        ErrorManager.getDefault().annotate (fsExc, ErrorManager.WARNING, null, 
        fsExc.getLocalizedMessage(), null, null);        
        throw fsExc;        
    }

    public static void io (
        String resource, Object arg1, Object arg2, Object arg3
    ) throws IOException {        
        FSException fsExc = new FSException (resource, new Object[] { arg1, arg2, arg3 });
        ErrorManager.getDefault().annotate (fsExc, ErrorManager.WARNING, null, 
        fsExc.getLocalizedMessage(), null, null);        
        throw fsExc;                
    }

    public static void io (
        String resource, Object arg1, Object arg2, Object arg3, Object arg4
    ) throws IOException {        
        FSException fsExc = new FSException (resource, new Object[] { arg1, arg2, arg3, arg4 });
        ErrorManager.getDefault().annotate (fsExc, ErrorManager.WARNING, null, 
        fsExc.getLocalizedMessage(), null, null);        
        throw fsExc;                        
    }

}
