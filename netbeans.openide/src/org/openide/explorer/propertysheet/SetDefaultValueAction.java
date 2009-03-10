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

package org.openide.explorer.propertysheet;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallbackSystemAction;
import org.openide.util.NbBundle;

/** Action to set the default value of a property.
*
* @author Jan Jancura, Petr Hamernik, Ian Formanek
*/
final class SetDefaultValueAction extends CallbackSystemAction {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -1285705164427519181L;
    
    public SetDefaultValueAction () {
    }

    public String getName () {
        return getString ("SetDefaultValue");
    }

    public HelpCtx getHelpCtx () {
        return new HelpCtx (SetDefaultValueAction.class);
    }

    protected void initialize() {
        super.initialize();
        setSurviveFocusChange(false);
    }

    /* Icon resource.
    * @return name of resource for icon
    */
    protected String iconResource () {
        return "org/openide/resources/actions/setDefaultValue.gif"; // NOI18N
    }
    
    private static String getString(String key) {
        return NbBundle.getBundle(SetDefaultValueAction.class).getString(key);
    }
}
