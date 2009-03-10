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

package org.openide.windows;

import java.awt.Image;
import java.awt.Rectangle;
import java.io.Serializable;
import java.beans.PropertyChangeListener;

/** A window-management mode in which a
* <code>TopComponent</code> can be.
*
* <p>Some default modes are always present.
* Modules can add their own modes by calling
* {@link Workspace#createMode} in their initialization code,
* or by declaring them using XML.
* <P>
* Modules can also get a list of current modes by calling
* {@link Workspace#getModes}.<p>
* 
* A mode is valid so long as someone keeps a reference to it.
* <p>
* Each mode must have a unique name.
*/
public interface Mode extends Serializable {

    /** @deprecated Only public by accident. */
    /* public static final */ long serialVersionUID = -2650968323666215654L;

    /** Name of property for bounds of the mode */
    public static final String PROP_BOUNDS = "bounds"; // NOI18N

    /** Name of property for the unique programmatic name of this mode. */
    public static final String PROP_NAME = "name"; // NOI18N

    /** Name of property for the display name of this mode. */
    public static final String PROP_DISPLAY_NAME = "displayName"; // NOI18N

    /** Get the diplay name of the mode.
    * This name will be used by a container to create its title.
    * @return human-presentable name of the mode
    */
    public String getDisplayName ();

    /** Get the programmatic name of the mode.
    * This name should be unique, as it is used to find modes etc.
    * @return programmatic name of the mode
    */
    public String getName ();

    /** Get the icon of the mode. It will be used by component container
    * implementations as the icon (e.g. for display in tabs).
    * @return the icon of the mode (or <code>null</code> if no icon was specified)
    */
    public Image getIcon ();

    /** Attaches a component to a mode for this workspace.
    * If the component is in different mode on this workspace, it is 
    * removed from the original and moved to this one.
    *
    * @param c component
    * @return true if top component was succesfully docked to this mode, false otherwise
    */
    public boolean dockInto (TopComponent c);

    /** Allows implementor to specify some restrictive policy as to which
     * top components can be docked into this mode.
     * @return true if a given top component can be docked into this mode,
     *         false otherwise
     */
    public boolean canDock (TopComponent tc);

    /** Sets the bounds of the mode.
    * @param s the bounds for the mode 
    */
    public void setBounds (Rectangle s);

    /** Getter for current bounds of the mode.
    * @return the bounds of the mode
    */
    public Rectangle getBounds ();

    /** Getter for asociated workspace.
    * @return The workspace instance to which is this mode asociated.
    */
    public Workspace getWorkspace ();

    /** Get all top components currently docked into this mode.
     * @return the list of components; might be empty, but not null
    */
    public TopComponent[] getTopComponents ();

    /** Add a property change listener.
    * @param list the listener to add
    */
    public void addPropertyChangeListener (PropertyChangeListener list);

    /** Remove a property change listener.
    * @param list the listener to remove
    */
    public void removePropertyChangeListener (PropertyChangeListener list);

}
