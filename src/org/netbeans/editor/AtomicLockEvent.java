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

package org.netbeans.editor;

import java.util.EventObject;

/**
 * Event for atomic lock listener notifications.
 */
public class AtomicLockEvent extends EventObject {

    /*
     * Construct new AtomicLockEvent.
     * @param source  the Object that is the source of the event
     */
    AtomicLockEvent(Object source) {
        super(source);
    }

}
