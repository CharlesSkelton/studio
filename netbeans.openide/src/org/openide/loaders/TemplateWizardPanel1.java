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

package org.openide.loaders;

import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;

/** Implementaion of WizardDescriptor.Panel that can be used in create from template.
 *
 * @author Jiri Rechtacek
 */
final class TemplateWizardPanel1 implements WizardDescriptor.Panel {
    private TemplateWizard1 templateWizard1UI;
    /** listener to changes in the wizard */
    private ChangeListener listener;
    
    private TemplateWizard1 getPanelUI () {
        if (templateWizard1UI == null) {
            templateWizard1UI = new TemplateWizard1 ();
            templateWizard1UI.addChangeListener (listener);
        }
        return templateWizard1UI;
    }
    
    /** Add a listener to changes of the panel's validity.
    * @param l the listener to add
    * @see #isValid
    */
    public void addChangeListener (ChangeListener l) {
        if (listener != null) throw new IllegalStateException ();
        if (templateWizard1UI != null)
            templateWizard1UI.addChangeListener (l);
        listener = l;
    }

    /** Remove a listener to changes of the panel's validity.
    * @param l the listener to remove
    */
    public void removeChangeListener (ChangeListener l) {
        listener = null;
        if (templateWizard1UI != null)
            templateWizard1UI.removeChangeListener (l);
    }

    /** Get the component displayed in this panel.
     *
     * Note; method can be called from any thread, but not concurrently
     * with other methods of this interface.
     *
     * @return the UI component of this wizard panel
     *
     */
    public java.awt.Component getComponent() {
        return getPanelUI ();
    }
    
    /** Help for this panel.
    * @return the help or <code>null</code> if no help is supplied
    */
    public HelpCtx getHelp () {
        if (templateWizard1UI != null) {
            if (templateWizard1UI.getExplorerManager().getRootContext() != Node.EMPTY) {
                return new HelpCtx(TemplateWizard1.class.getName()+"."+ // NOI18N
                    templateWizard1UI.getExplorerManager().getRootContext().getName());
            }
        }
        return new HelpCtx (TemplateWizard1.class);
    }

    /** Test whether the panel is finished and it is safe to proceed to the next one.
    * If the panel is valid, the "Next" (or "Finish") button will be enabled.
    * @return <code>true</code> if the user has entered satisfactory information
    */
    public boolean isValid() {
        if (templateWizard1UI == null)
            return false;
        return getPanelUI ().implIsValid ();
    }
    
    /** Provides the wizard panel with the current data--either
    * the default data or already-modified settings, if the user used the previous and/or next buttons.
    * This method can be called multiple times on one instance of <code>WizardDescriptor.Panel</code>.
    * @param settings the object representing wizard panel state, as originally supplied to {@link WizardDescriptor#WizardDescriptor(WizardDescriptor.Iterator,Object)}
    */
    public void readSettings(Object settings) {
        getPanelUI ().implReadSettings (settings);
    }
    
    /** Provides the wizard panel with the opportunity to update the
    * settings with its current customized state.
    * Rather than updating its settings with every change in the GUI, it should collect them,
    * and then only save them when requested to by this method.
    * Also, the original settings passed to {@link #readSettings} should not be modified (mutated);
    * rather, the (copy) passed in here should be mutated according to the collected changes.
    * This method can be called multiple times on one instance of <code>WizardDescriptor.Panel</code>.
    * @param settings the object representing a settings of the wizard
    */
    public void storeSettings(Object settings) {
        getPanelUI ().implStoreSettings (settings);
    }
    
}
