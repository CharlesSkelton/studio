/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.util;

import java.net.URL;
import java.lang.reflect.*;
import javax.swing.JComponent;

import org.openide.ErrorManager;

/** Provides help for any window or other feature in the system.
* It is designed to be JavaHelp-compatible and to use the same tactics when
* assigning help to {@link JComponent} instances.
* @see <a href="http://www.netbeans.org/download/dev/javadoc/JavaHelpAPI/org/netbeans/api/javahelp/package-summary.html">JavaHelp Integration API</a></p>
* @author Petr Hamernik, Jaroslav Tulach, Jesse Glick
*/
public final class HelpCtx extends Object {
    
    private static final ErrorManager err;
    static {
        ErrorManager master = ErrorManager.getDefault();
        ErrorManager sub = master.getInstance("org.openide.util.HelpCtx"); // NOI18N
        if (sub.isLoggable(ErrorManager.UNKNOWN)) {
            err = sub;
        } else {
	    err = null;
	}
    }

    // JST: I do not want to have every class deprecated!
    //     * @deprecated Please give a specific help page instead.

    /** Default help page.
    * This (hopefully) points to a note explaining to the user that no help is available.
    * Precisely, the Help ID is set to <code>org.openide.util.HelpCtx.DEFAULT_HELP</code>.
    */
    public final static HelpCtx DEFAULT_HELP = new HelpCtx (HelpCtx.class.getName () + ".DEFAULT_HELP"); // NOI18N

    /** URL of the help page */
    private final URL helpCtx;

    /** JavaHelp ID for the help */
    private final String helpID;

    /** Create a help context by URL.
     * @deprecated Does not work nicely with JavaHelp.
    * @param helpCtx URL to point help to
    */
    public HelpCtx(URL helpCtx) {
        this.helpCtx = helpCtx;
        this.helpID = null;
    }

    /** Create a help context by tag.
    * You must provide an ID of the
    * desired help for the item. The ID should refer to an
    * already installed help; this can be easily installed by specifying
    * a JavaHelp help set for the module (see the Modules API for details).
    *
    * @param helpID the JavaHelp ID of the help
    */
    public HelpCtx(String helpID) {
        this.helpID = helpID;
        this.helpCtx = null;
    }

    /** Create a help context by class.
    * Assigns the name of a class as
    * the ID.
    *
    * @param clazz the class to take the name from
    */
    public HelpCtx (Class clazz) {
        this (clazz.getName ());
    }

    /** Get a URL to the help page, if applicable.
    * @return a URL to the page, or <code>null</code> if the target was specified by ID
    */
    public URL getHelp () {
        return helpCtx;
    }

    /** Get the ID of the help page, if applicable.
    * @return the JavaHelp ID string, or <code>null</code> if specified by URL
    */
    public String getHelpID () {
        return helpID;
    }

    // object identity

    public int hashCode () {
        int base = HelpCtx.class.hashCode ();
        if (helpCtx != null) base ^= helpCtx.hashCode ();
        if (helpID != null) base ^= helpID.hashCode ();
        return base;
    }

    public boolean equals (Object o) {
        if (o == null || ! (o instanceof HelpCtx))
            return false;
        HelpCtx oo = (HelpCtx) o;
        return (helpCtx == oo.helpCtx || (helpCtx != null && helpCtx.equals(oo.helpCtx))) &&
               (helpID == oo.helpID || (helpID != null && helpID.equals(oo.helpID)));
    }

    public String toString () {
        if (helpID != null) {
            return "HelpCtx[" + helpID + "]"; // NOI18N
        } else {
            return "HelpCtx[" + helpCtx + "]"; // NOI18N
        }
    }

    /** Set the help ID for a component.
    * @param comp the visual component to associate help to
    * @param helpID help ID, or <code>null</code> if the help ID should be removed
    */
    public static void setHelpIDString (JComponent comp, String helpID) {
        if (err != null) err.log("setHelpIDString: " + helpID + " on " + comp);
        comp.putClientProperty("HelpID", helpID); // NOI18N
    }

    /** Find the help ID for a component.
     * If the component implements {@link org.openide.util.HelpCtx.Provider},
     * its method {@link org.openide.util.HelpCtx.Provider#getHelpCtx} is called.
     * If the component has help attached by {@link #setHelpIDString}, it returns that.
     * Otherwise it checks the parent component recursively.
     *
     * @param comp the component to find help for
     * @return the help for that component (never <code>null</code>)
     */
    public static HelpCtx findHelp (java.awt.Component comp) {
        if (err != null) err.log("findHelp on " + comp);
        while (comp != null) {
            if (comp instanceof HelpCtx.Provider) {
		HelpCtx h = ((HelpCtx.Provider)comp).getHelpCtx();
                if (err != null) err.log("found help " + h + " through HelpCtx.Provider interface");
                return h;
            }
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent)comp;
                String hid = (String) jc.getClientProperty("HelpID"); // NOI18N
                if (hid != null) {
                    if (err != null) err.log("found help " + hid + " by client property");
                    return new HelpCtx (hid);
                }
            }

            comp = comp.getParent ();
            if (err != null) err.log("no luck, trying parent " + comp);
        }
        if (err != null) err.log("nothing found");
        return DEFAULT_HELP;
    }

    /**
     * An object implementing this interface is willing to answer
     * the HelpCtx.findHelp() query itself.
     *
     * @since 3.20
     */
    public static interface Provider {
        /**
         * Get the {@link HelpCtx} associated with implementing object.
         * @return assigned <code>HelpCtx</code> or
         *         {@link #DEFAULT_HELP}, never <code>null</code>.
         */
        public HelpCtx getHelpCtx();
    }
}
