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

package org.netbeans.editor;

import java.util.EventObject;

/**
* Event providing information about what was changed in settings
*
* @author Miloslav Metelka
* @version 1.00
*/

public class SettingsChangeEvent extends EventObject {

    private Class kitClass;

    private String settingName;

    private Object oldValue;

    private Object newValue;

    public SettingsChangeEvent(Object source, Class kitClass, String settingName,
                               Object oldValue, Object newValue) {
        super(source);
        this.kitClass = kitClass;
        this.settingName = settingName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Class getKitClass() {
        return kitClass;
    }

    public String getSettingName() {
        return settingName;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

}
