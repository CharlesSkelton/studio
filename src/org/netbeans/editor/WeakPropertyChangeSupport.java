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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
* Similair functionality as PropertyChangeSupport but holds only
* weak references to listener classes
*
* @author Miloslav Metelka
* @version 1.00
*/

public class WeakPropertyChangeSupport {

    private transient ArrayList listeners = new ArrayList();

    private transient ArrayList interestNames = new ArrayList();


    /** Add weak listener to listen to change of any property. The caller must
    * hold the listener object in some instance variable to prevent it
    * from being garbage collected.
    */
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        addLImpl(null, l);
    }

    /** Add weak listener to listen to change of the specified property.
    * The caller must hold the listener object in some instance variable
    * to prevent it from being garbage collected.
    */
    public synchronized void addPropertyChangeListener(String propertyName,
            PropertyChangeListener l) {
        addLImpl(propertyName, l);
    }

    /** Remove listener for changes in properties */
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        int cnt = listeners.size();
        for (int i = 0; i < cnt; i++) {
            Object o = ((WeakReference)listeners.get(i)).get();
            if (o == null || o == l) { // remove null references and the required one
                listeners.remove(i);
                interestNames.remove(i);
                i--;
                cnt--;
            }
        }
    }

    public void firePropertyChange(Object source, String propertyName,
                                   Object oldValue, Object newValue) {
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }
        PropertyChangeListener la[];
        String isa[];
        int cnt;
        synchronized (this) {
            cnt = listeners.size();
            la = new PropertyChangeListener[cnt];
            for (int i = 0; i < cnt; i++) {
                PropertyChangeListener l = (PropertyChangeListener)
                                           ((WeakReference)listeners.get(i)).get();
                if (l == null) { // remove null references
                    listeners.remove(i);
                    interestNames.remove(i);
                    i--;
                    cnt--;
                } else {
                    la[i] = l;
                }
            }
            isa = (String [])interestNames.toArray(new String[cnt]);
        }

        // now create and fire the event
        PropertyChangeEvent evt = new PropertyChangeEvent(source, propertyName,
                                  oldValue, newValue);
        for (int i = 0; i < cnt; i++) {
            if (isa[i] == null || propertyName == null || isa[i].equals(propertyName)) {
                la[i].propertyChange(evt);
            }
        }
    }

    private void addLImpl(String sn, PropertyChangeListener l) {
        listeners.add(new WeakReference(l));
        interestNames.add(sn);
    }

}
