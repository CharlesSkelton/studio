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

package org.openide.explorer.propertysheet;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.*;
import java.beans.*;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import org.openide.DialogDescriptor;

import org.openide.nodes.Node;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;
import org.openide.util.HelpCtx;
import org.openide.explorer.propertysheet.editors.EnhancedCustomPropertyEditor;

/** 
 * Helper dialog box manager for showing custom property editors.
 *
 * @author Jan Jancura, Dafe Simonek, David Strupl
 */

final class PropertyDialogManager implements VetoableChangeListener {
/* JST: Made package private because PropertyPanel should be used instead. */
    
    /** Listener to editor property changes. */
    private PropertyChangeListener          listener;
    /** Cache for reverting on cancel. */
    private Object                          oldValue;
    /** Custom property editor. */
    private PropertyEditor                  editor;
    /** model of the edited property */
    private PropertyModel                   model;
    /** this is extracted from the model if possible, can be null */
    private Node.Property                   prop;
    /** Set true when property is changed. */
    private boolean                         changed = false;
    /** Given component stored for test on Enhance property ed. */
    private Component                       component;
    /** Dialog instance. */
    private Window                          dialog;
    /** Ok button can be enabled/disabled*/
    private JButton                         okButton;
    /** */
    private Runnable                        errorPerformer;
    
    private boolean                         okButtonState = true;
    
    private boolean                         defaultValue = false;
    
    private boolean                         isModal = true;
    
    private String                          title = null;
    
    private HelpCtx                         helpCtx = null;
    
    private Object defaultOption;
    
    private Object[] options;
    
    private Object envStateBeforeDisplay = null;
    
    /** Environment passed to the property editor. */
    private PropertyEnv env;
    
    private static ThreadLocal caller = new ThreadLocal();
    
    private ActionListener actionListener;
    
    private Object lastValueFromEditor;
    
    /** Name of the custom property that can be passed in PropertyEnv. */
    private static final String PROPERTY_DESCRIPTION = "description"; // NOI18N
    
    // init ......................................................................
    
    /** Create a dialog.
     *
     * @param title title of the dialog
     * @param component component to show
     * @param isModal <code>true</code> if the dialog should be modal
     * @param editor custom property editor. May be <code>null</code>.
     */
    public PropertyDialogManager(final String title, 
        final boolean isModal, final PropertyEditor editor, PropertyModel model,
        final PropertyEnv env) {
        
        this.editor = editor;
        
        if (env != null) {
            env.addVetoableChangeListener(this);
        }
        
        this.component = editor.getCustomEditor();
        this.model = model;
        this.env = env;
        this.title = title;
        this.isModal = isModal;
        
        actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                doButtonPressed(evt);
            }
        };

        if (env != null) {
            Object helpID = env.getFeatureDescriptor().getValue(ExPropertyEditor.PROPERTY_HELP_ID);
            if ( helpID != null && helpID instanceof String &&
                    component != null && component instanceof JComponent ) {
                HelpCtx.setHelpIDString((JComponent)component, (String)helpID);
                helpCtx = new HelpCtx((String) helpID);
            }
        }
        
        // create dialog instance and initialize listeners
        createDialog();
        initializeListeners();
    }
    
    
    // public methods ............................................................
    
    /** Get the created dialog instance.
     * @return the dialog instance managed by this class.
     */
    public Window getDialog() {
        return dialog;
    }
                
    /**
     * PropertyDialogManager is attached to the PropertyEnv instance by
     * vetoableChangeListener.
     */
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        if ((env != null) && (PropertyEnv.PROP_STATE.equals(evt.getPropertyName()))) {
            okButtonState = evt.getNewValue() != PropertyEnv.STATE_INVALID;
            if (okButton != null) {
                okButton.setEnabled(okButtonState);
            }
        }
    }
    
    // other methods ............................................................
    
    /** Creates proper DialogDescriptor and obtain dialog instance
     * via DialogDisplayer.createDialog() call.
     */
    private void createDialog() {
        if (component instanceof Window) {
            // custom component is already a window --> just return it
            // from getDialog
            dialog = (Window)component;
            dialog.pack();
            return;
        }
        
        // prepare our options (buttons)
        boolean cannotWrite = false;
        if (model instanceof ExPropertyModel) {
            FeatureDescriptor fd = ((ExPropertyModel)model).getFeatureDescriptor();
            if (fd instanceof Node.Property) {
                prop = (Node.Property)fd;
                cannotWrite = !prop.canWrite();
                defaultValue = prop.supportsDefaultValue();
            }
        }
        if ((editor == null) || (cannotWrite)) {
            JButton closeButton = new JButton(getString("CTL_Close"));
            closeButton.getAccessibleContext().setAccessibleDescription(getString("ACSD_CTL_Close"));
            options = new Object[] {
                closeButton
            };
            defaultOption = closeButton;
        } else {
            okButton = new JButton(getString("CTL_OK"));
            okButton.getAccessibleContext().setAccessibleDescription(getString("ACSD_CTL_OK"));
            JButton cancelButton = new JButton(getString("CTL_Cancel"));
            cancelButton.setVerifyInputWhenFocusTarget(false);
            cancelButton.getAccessibleContext().setAccessibleDescription(getString("ACSD_CTL_Cancel"));
            cancelButton.setDefaultCapable(false);
            if (defaultValue) {
                JButton defaultButton = new JButton(getString("CTL_Default"));
                defaultButton.setMnemonic(getString("CTL_DefaultMnemonic").charAt(0));
                defaultButton.getAccessibleContext().setAccessibleDescription(getString("ACSD_CTL_Default"));
                defaultButton.setDefaultCapable(false);
                defaultButton.setVerifyInputWhenFocusTarget(false);
                options = new Object[] {
                        defaultButton,
                        okButton,
                        cancelButton
                };
            } else {
                options = new Object[] {
                    okButton,
                    cancelButton
                };
            }
            defaultOption = okButton;
        }
        
        if ((env != null) && (okButton != null)) {
            okButtonState = env.getState() != PropertyEnv.STATE_INVALID;
            if (okButton != null) {
                okButton.setEnabled(okButtonState);
            }
        }

        if (env != null) {
            envStateBeforeDisplay = env.getState();
        }
        
        try {
            caller.set(this);
            Class c = Class.forName("org.openide.explorer.propertysheet.PropertyDialogManager$CreateDialogInvoker"); // NOI18N
            Runnable r = (Runnable) c.newInstance();
            r.run();
            
            return;
            
        } catch (Exception e) {
            // if something went wrong just
            // resort to swing (IDE probably not present)
        } catch (LinkageError e) {
        }

        if (dialog == null) {
            JOptionPane jop = new JOptionPane(component, JOptionPane.PLAIN_MESSAGE,
            JOptionPane.NO_OPTION, null, options, defaultOption);
            if (okButton != null) {
                okButton.addActionListener(actionListener);
            }
            dialog = jop.createDialog(null, title);
        }
        
        if (env != null) {
            Object obj = env.getFeatureDescriptor().getValue(PROPERTY_DESCRIPTION);
            if (obj instanceof String) {
                dialog.getAccessibleContext().setAccessibleDescription((String)obj);
            }
        }
    }
    
    /** Initializes dialog listeners. Must be called after
     * createDialog method call. (dialog variable must not be null)
     */
    private void initializeListeners() {
        // dialog closing reactions
        dialog.addWindowListener(new WindowAdapter() {
            /** Ensure that values are reverted when user cancelles dialog
             * by clicking on x image */
            public void windowClosing(WindowEvent e) {
                if ((editor != null) &&
                    ! (component instanceof Window)) {
                    // if someone provides a window (s)he has to handle
                    // the cancel him/herself
                    cancelValue();
                }
                // ensure that resources are released
                if (env != null) {
                    env.removeVetoableChangeListener(PropertyDialogManager.this);
                }
                dialog.dispose();
            }
            /** Remove property listener on window close */
            public void windowClosed(WindowEvent e) {
                if (component instanceof Window) {
                    // in this case we have to do similar thing as we do
                    // directly after the Ok button is pressed. The difference
                    // is in the fact that here we do not decide whether the
                    // dialog will be closed or not - it is simply being closed
                    // But we have to take care of propagating the value to
                    // the model
                    if (component instanceof EnhancedCustomPropertyEditor) {
                        try {
                            Object newValue = ((EnhancedCustomPropertyEditor)
                                component).getPropertyValue();
                            model.setValue(newValue);
                        } catch (java.lang.reflect.InvocationTargetException ite) {
                            PropertyPanel.notifyUser (ite, prop==null?"":prop.getDisplayName ()); // NOI18N
                        } catch (IllegalStateException ise) {
                            notifyUser (ise);
                        }
                    }
                    else if ((env!=null)&&(!env.isChangeImmediate())) {
                        try {
                            model.setValue(lastValueFromEditor);
                        } catch (java.lang.reflect.InvocationTargetException ite) {
                            PropertyPanel.notifyUser (ite, prop==null?"":prop.getDisplayName ()); // NOI18N
                        } catch (IllegalStateException ise) {
                            notifyUser (ise);
                        }
                    }
                }
                if (listener != null) editor.removePropertyChangeListener(listener);
                dialog.removeWindowListener(this);
            }
        });
        // reactions to editor property changes
        if (editor != null) {
            try {
                oldValue = model.getValue();
            } catch (Exception e) {
                // Ignored, there can be number of exceptions
                // when asking for old values...
            }
            lastValueFromEditor = editor.getValue();
            editor.addPropertyChangeListener(listener =
            new PropertyChangeListener() {
                /** Notify displayer about property change in editor */
                public void propertyChange(PropertyChangeEvent e) {
                    changed = true;
                    lastValueFromEditor = editor.getValue();
                    // enabling/disabling the okButton in response to
                    // firing PROP_VALUE_VALID --- usage of this firing
                    // has been deprecated in favor of directly calling
                    // PropertyEnv.setState(...)
                    if (ExPropertyEditor.PROP_VALUE_VALID.equals(e.getPropertyName())) {
                        if (okButton != null) {
                            if (e.getNewValue() instanceof Boolean) {
                                Boolean newButtonState = (Boolean)e.getNewValue();
                                okButtonState = newButtonState.booleanValue();
                                if (env != null) {
                                    env.setState(okButtonState?
                                        PropertyEnv.STATE_VALID:PropertyEnv.STATE_INVALID);
                                } else {
                                    okButton.setEnabled(okButtonState);
                                }
                                if (e.getOldValue() instanceof Runnable) {
                                    errorPerformer = (Runnable) e.getOldValue();
                                } else {
                                    errorPerformer = null;
                                }
                            }
                        }
                    }
                }
            }
            );
            component.addPropertyChangeListener(listener =
                new PropertyChangeListener() {
                    /** forward possible help context change in custom editor */
                    public void propertyChange(PropertyChangeEvent e) {
                        if (DialogDescriptor.PROP_HELP_CTX.equals(e.getPropertyName())) {
                            if ( dialog instanceof PropertyChangeListener )
                                ((PropertyChangeListener)dialog).propertyChange( e );
                        }
                    }
                }
                );
        }
    }
    
    /**
     * Reverts to old values.
     */
    private void cancelValue() {
        if ( (!changed) ||
            (component instanceof EnhancedCustomPropertyEditor) ||
            ((env!=null)&&(!env.isChangeImmediate()))
        ) {
            if ((env != null) && (envStateBeforeDisplay != null)) {
                env.setState(envStateBeforeDisplay);
            }
            return;
        }
        
        try {
            model.setValue(oldValue);
        } catch (Exception e) {
            // Ignored, there can be number of exceptions
            // when asking for old values...
        }
    }
    
    /** Called when user presses a button on some option (button) in the
     * dialog.
     * @param evt The button press event.
     */
    private void doButtonPressed(ActionEvent evt) {
        String label = evt.getActionCommand();
        if (label.equals(getString("CTL_Cancel"))) {
            cancelValue();
        } 
        if (label.equals(getString("CTL_Default"))) {
            if (prop != null) {
                try {
                    prop.restoreDefaultValue();
                } catch (IllegalAccessException iae) {
                    PropertyPanel.notifyUser (iae, prop.getDisplayName ());
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    PropertyPanel.notifyUser (ite, prop.getDisplayName ());
                }
            }
        }
        if ( label.equals(getString("CTL_OK"))) {
            if ((env!=null)&&(env.getState() == PropertyEnv.STATE_NEEDS_VALIDATION)) {
                env.setState(PropertyEnv.STATE_VALID);
                if (env.getState() != PropertyEnv.STATE_VALID) {
                    // if the change was vetoed do nothing and return
                    return;
                }
            }
            if (component instanceof EnhancedCustomPropertyEditor) {
                try {
                    Object newValue = ((EnhancedCustomPropertyEditor)
                        component).getPropertyValue();
                    model.setValue(newValue);
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    PropertyPanel.notifyUser (ite, prop==null?"":prop.getDisplayName ()); // NOI18N
                    return;
                } catch (IllegalStateException ise) {
                    notifyUser (ise);
                    return;
                }
            }
            else if ((env!=null)&&(!env.isChangeImmediate())) {
                try {
                    model.setValue(lastValueFromEditor);
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    PropertyPanel.notifyUser (ite, prop==null?"":prop.getDisplayName ()); // NOI18N
                    return;
                } catch (IllegalStateException ise) {
                    notifyUser (ise);
                    return;
                }
            }
            // this is an old hack allowing to notify a cached value
            // obtained via propertyChangeEvent from the editor
            if (!okButtonState) {
                if (errorPerformer != null) {
                    errorPerformer.run();
                }
                return;
            }
        }
        // close the dialog
        changed = false;
        if (env != null) {
            env.removeVetoableChangeListener(this);
        }
        dialog.dispose();
    }
    
    private static String getString(String key) {
        return NbBundle.getBundle(PropertyDialogManager.class).getString(key);
    }  
    
    /** For testing purposes we need to _not_ notify some exceptions.
     * That is why here is a package private method to register an exception
     * that should not be fired.
     */
    static void doNotNotify (Throwable ex) {
        doNotNotify = ex;
    }
    private static Throwable doNotNotify;
    
    /** Notifies an exception to error manager or prints its it to stderr.
     * @param ex exception to notify
     */
    static void notify(Throwable ex) {
        Throwable d = doNotNotify;
        doNotNotify = null;
        if (d == ex) return;
        
        ErrorManager.getDefault ().notify (ex);
    }
    
    /** Notifies an exception to error manager or prints its it to stderr.
     * @param ex exception to notify
     */
    static void notify(int severity, Throwable ex) {
        Throwable d = doNotNotify;
        doNotNotify = null;
        if (d == ex) return;
        
        ErrorManager.getDefault ().notify(severity, ex);
    }
    /** 
     * Tries to find a localized message in the exception annotation
     * or directly in the exception. If the message is found it is
     * notified with user severity. If the message is not found the
     * exception is notified with informational severity.
     * @param ex exception to notify
     */
    private static void notifyUser(Exception e) {
        ErrorManager em = ErrorManager.getDefault ();
        ErrorManager.Annotation[] an = em.findAnnotations(e);
        String userMessage = null;
        if (an != null) {
            for (int i = 0; i < an.length; i++) {
                String msg = an[i].getLocalizedMessage();
                if (msg != null) {
                    userMessage = msg;
                    break;
                }
            }
        }
        if (userMessage == null) {
            userMessage = e.getLocalizedMessage();
        }
        if (userMessage != null) {
            Throwable t = em.annotate(e, userMessage);
            em.notify(ErrorManager.USER, t);
        } else {
            em.notify(ErrorManager.INFORMATIONAL, e);
        }
    }
    
    static Throwable annotate(Throwable t, int severity, String message,
        String localizedMessage, Throwable stackTrace, java.util.Date date) {
        return ErrorManager.getDefault ().annotate(t, severity,
		message, localizedMessage, stackTrace, date);
    }
    
    static class CreateDialogInvoker implements Runnable {
        public void run() {
            final PropertyDialogManager pdm = (PropertyDialogManager)caller.get();
            caller.set(null);
            if (pdm == null) {
                throw new IllegalStateException("Parameter caller not passed."); // NOI18N
            }
            // create dialog descriptor, create & return the dialog
            // bugfix #24998, set helpCtx obtain from PropertyEnv.getFeatureDescriptor()
            org.openide.DialogDescriptor descriptor = new org.openide.DialogDescriptor(
                pdm.component, pdm.title, pdm.isModal, pdm.options, pdm.defaultOption,
                org.openide.DialogDescriptor.DEFAULT_ALIGN, pdm.helpCtx,
                pdm.actionListener
            );

            pdm.dialog = org.openide.DialogDisplayer.getDefault().createDialog(descriptor);
        }
    }
}
