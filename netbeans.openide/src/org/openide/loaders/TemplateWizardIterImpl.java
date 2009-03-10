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

package org.openide.loaders;

import javax.swing.event.*;

import org.openide.WizardDescriptor;

/** Implementation of template wizard's iterator that allows to
* delegate all functionality to another wizard.
*
* @author  Jaroslav Tulach
*/
final class TemplateWizardIterImpl extends Object
    implements WizardDescriptor.Iterator, ChangeListener {
    /** iterator to delegate to */
    private TemplateWizard.Iterator iterator;

    /** bridge */
    private ChangeListener iteratorListener;
    
    /** is currently panel displayed? */
    private boolean showingPanel = true;

    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList = null;
    
    /** Instance of the template wizard that uses this Iterator.
     */
    private TemplateWizard wizardInstance;        
    
    /** */
    private boolean iteratorInitialized = false;

    /** Getter for the first panel.
     * @return the first and default panel
     */
    private WizardDescriptor.Panel firstPanel () {
        return wizardInstance.templateChooser();
    }

    /** Resets the iterator to first screen.
    */
    public void first () {
        showingPanel = true;
        fireStateChanged ();
    }

    /** Change the additional iterator.
    */
    public void setIterator (TemplateWizard.Iterator it, boolean notify) {
        if ((this.iterator != null) && (iteratorInitialized)) {
            this.iterator.removeChangeListener(iteratorListener);
            this.iterator.uninitialize(wizardInstance);
        }
        it.initialize(wizardInstance);
        iteratorListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                TemplateWizardIterImpl.this.stateChanged(e);
            }
        };
        it.addChangeListener(iteratorListener);
        iteratorInitialized = true;
        
        iterator = it;
        if (notify) {
            showingPanel = false;
            fireStateChanged ();
        }
    }

    /** Getter for current iterator.
    */
    public TemplateWizard.Iterator getIterator () {
        if (iterator == null) {
            // first of all initialize the default iterator
            setIterator (wizardInstance.defaultIterator (), false);
        }
        if (!iteratorInitialized) {
            if (iterator != null) {
                iterator.initialize(wizardInstance);
                iteratorInitialized = true;
            }
        }
        return iterator;
    }

    /** Get the current panel.
     * @return the panel
     */
    public WizardDescriptor.Panel current() {
        return showingPanel ? firstPanel () : getIterator ().current ();
    }


    /** Get the name of the current panel.
     * @return the name
     */
    public String name() {
        return showingPanel ? "" : getIterator ().name (); // NOI18N
    }

    /** Test whether there is a next panel.
     * @return <code>true</code> if so
     */
    public boolean hasNext() {
        return showingPanel || getIterator ().hasNext();
    }

    /** Test whether there is a previous panel.
     * @return <code>true</code> if so
     */
    public boolean hasPrevious() {
        return !showingPanel;
    }

    /** Move to the next panel.
     * I.e. increment its index, need not actually change any GUI itself.
     * @exception NoSuchElementException if the panel does not exist
     */
    public void nextPanel() {
        if (showingPanel) {
            showingPanel = false;
        } else {
            getIterator ().nextPanel ();
        }
    }

    /** Move to the previous panel.
     * I.e. decrement its index, need not actually change any GUI itself.
     * @exception NoSuchElementException if the panel does not exist
     */
    public void previousPanel() {
        if (getIterator ().hasPrevious ()) {
            getIterator ().previousPanel ();
        } else {
            showingPanel = true;
        }
    }

    /** Refires the info to listeners */
    public void stateChanged(final javax.swing.event.ChangeEvent p1) {
        fireStateChanged ();
    }

    /** Registers ChangeListener to receive events.
     *@param listener The listener to register.
     */
    public synchronized void addChangeListener(javax.swing.event.ChangeListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add (javax.swing.event.ChangeListener.class, listener);
    }
    /** Removes ChangeListener from the list of listeners.
     *@param listener The listener to remove.
     */
    public synchronized void removeChangeListener(
        javax.swing.event.ChangeListener listener
    ) {
        if (listenerList != null) {
            listenerList.remove (javax.swing.event.ChangeListener.class, listener);
        }        
    }
    
    public void initialize(TemplateWizard wiz) {
        this.wizardInstance = wiz;
        TemplateWizard.Iterator it = iterator;
	if ((it != null)&&(!iteratorInitialized)) {
	    it.initialize(wizardInstance);
            iteratorInitialized = true;
	}
        // (jrojcek) Fix of bug 9136. Maybe better place is TemplateWizard.initialize().
        //showingPanel = true;
    }
    
    public void uninitialize() {
	if (iterator != null) { 
	    iterator.uninitialize(wizardInstance);            
            iteratorInitialized = false;
	}
        showingPanel = true;
    }
    
    /** Notifies all registered listeners about the event.
     *
     *@param param1 Parameter #1 of the <CODE>ChangeEvent<CODE> constructor.
     */
    private void fireStateChanged() {
        if (listenerList == null)
            return;
        
        javax.swing.event.ChangeEvent e = null;
        Object[] listeners = listenerList.getListenerList ();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==javax.swing.event.ChangeListener.class) {
                if (e == null)
                    e = new javax.swing.event.ChangeEvent (this);
                ((javax.swing.event.ChangeListener)listeners[i+1]).stateChanged (e);
            }
        }
    }
}
