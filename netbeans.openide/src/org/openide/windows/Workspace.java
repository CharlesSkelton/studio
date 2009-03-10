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

package org.openide.windows;

import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.net.URL;
import java.util.Set;

/** Represents one user workspace that holds a list of modes into which
* components can be assigned.
* Created by WindowManager.
* When serialized only keeps "weak" reference to this workspace does not
* stores the content of the workspace (it is responsibility of window manager).
*
* @author Jaroslav Tulach
*/
public interface Workspace extends Serializable {
    /** @deprecated Only public by accident. */
    /* public static final */ long serialVersionUID = 2987897537843190271L;

    /** Name of property for modes in the workspace */
    public static final String PROP_MODES = "modes"; // NOI18N

    /** Name of property for the programmatic name of this workspace. */
    public static final String PROP_NAME = "name"; // NOI18N

    /** Name of property for the display name of this workspace. */
    public static final String PROP_DISPLAY_NAME = "displayName"; // NOI18N

    /** Get unique programmatical name of this workspace.
    * @return unique name of the workspace
    */
    public String getName ();

    /** Get human-presentable name of the workspace which
    * will be used for displaying.
    * @return the display name of the workspace
    */
    public String getDisplayName ();

    /** Array of all modes on this workspace.
    */
    public Set getModes ();

    /** Get bounds of the workspace. Returned value has slighly different
    * meaning for SDI and MDI mode. Modules should use this method for
    * correct positioning of their windows.
    * @return In SDI, returns bounds relative to whole screen, returns bounds
    * of the part of screen below main window (or above main window, if main
    * window is on bottom part of the screen).<br>
    * In MDI, bounds are relative to the main window; returned value represents
    * 'client area' of the main window */
    public Rectangle getBounds ();

    /** Activates this workspace to be current one.
    * This leads to change of current workspace of the WindowManager.
    */
    public void activate ();

    /** Create a new mode.
    * @param name a unique programmatic name of the mode 
    * @param displayName a human presentable (probably localized) name
    *                    of the mode (may be used by
                         the <b>Dock&nbsp;Into</b> submenu, e.g.)
    * @param icon a URL to the icon to use for the mode (e.g. on a tab or window corner);
    *             may be <code>null</code>
    * @return the new mode
    */
    public Mode createMode (String name, String displayName, URL icon);

    /** Search all modes on this workspace by name.
    * @param name the name of the mode to search for
    * @return the mode with that name, or <code>null</code> if no such mode
    *         can be found
    */
    public Mode findMode (String name);

    /** Finds mode the component is in on this workspace.
    *
    * @param c component to find mode for
    * @return the mode or null if the component is not visible on this workspace
    */
    public Mode findMode (TopComponent c);

    /** Removes this workspace from set of workspaces
    * in window manager. 
    */
    public void remove ();

    /** Add a property change listener.
    * @param list the listener to add
    */
    public void addPropertyChangeListener (PropertyChangeListener list);

    /** Remove a property change listener.
    * @param list the listener to remove
    */
    public void removePropertyChangeListener (PropertyChangeListener list);
}
