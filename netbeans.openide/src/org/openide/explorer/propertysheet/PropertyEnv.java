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

package org.openide.explorer.propertysheet;

import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.beans.PropertyVetoException;


/**
 * Instance of this class contains information that
 * is being passed to the property editor (instance of
 * ExtendedPropertyEditor) from the IDE.
 * @author  dstrupl
 */
public final class PropertyEnv {
  
    /** Name of the state property. */
    public static final String PROP_STATE = "state"; //NOI18N

    /**
     * One possible value for the setState/getState methods. With this
     * value the editor is in a valid state.
     */
    public static final Object STATE_VALID = "valid"; //NOI18N

    /**
     * One possible value for the setState/getState methods. 
     * This one means that the editor does not know its state and
     * it has to validate it later.
     */
    public static final Object STATE_NEEDS_VALIDATION = "needs_validation"; //NOI18N
    

    /**
     * One possible value for the setState/getState methods. With this
     * one the editor is in invalid state (Ok button on custom editor
     * panel is disabled and an invalid glyph shown on the property panel).
     */
    public static final Object STATE_INVALID = "invalid"; // NOI18N
    
    static final String PROP_CHANGE_IMMEDIATE = "changeImmediate"; // NOI18N
    
    // bugfix #25106, don't return null as feature descriptor
    /** Empty FeatureDescriptor as initiation value. */
    static final FeatureDescriptor dummyDescriptor = new FeatureDescriptor ();

    /** The value returned from getFeatureDescriptor. */
    private FeatureDescriptor featureDescriptor = dummyDescriptor;
    
    /** The value returned from getBeans.*/
    private Object[] beans;
    
    /** Current state. */
    private Object state = STATE_VALID;
    
    /** The support is lazy initialized in getSupport. */
    private VetoableChangeSupport support;
    /** change support here */
    private PropertyChangeSupport change;
    
    /**
     * The value of this field is basically taken from
     * the property panel. The property panel is responsible
     * for propagating the value to this field by calling
     * setchangeImmediate.
     */
    private boolean changeImmediate = true;

    
    /** Default constructor has package access -
     * we do not want the instances to be created outside
     * our package.
     */
    PropertyEnv() {
    }
    
    /**
     * Array of beans that the edited property belongs to.
     */
    public Object[] getBeans() {
        return beans;
    } 
    
    /**
     * Array of nodes that the edited property belongs to. 
     */
    void setBeans(Object[] beans) {
        this.beans = beans;
    } 
    
    /** 
     * Feature descritor that describes the property. It is feature
     * descriptor so one can plug in PropertyDescritor and also Node.Property
     * which both inherit from FeatureDescriptor
     */
    public FeatureDescriptor getFeatureDescriptor() {
        return featureDescriptor;
    }
    
    /** 
     * Feature descritor that describes the property. It is feature
     * descriptor so one can plug in PropertyDescritor and also Node.Property
     * which both inherit from FeatureDescriptor
     */
    void setFeatureDescriptor(FeatureDescriptor desc) {
        if (desc == null) {
            throw new IllegalArgumentException ("Cannot set FeatureDescriptor to null."); //NOI18N
        }
        this.featureDescriptor = desc;
        if (featureDescriptor != null) {
            Object obj = featureDescriptor.getValue(PROP_CHANGE_IMMEDIATE);
            if (obj instanceof Boolean) {
                setChangeImmediate(((Boolean)obj).booleanValue());
            }
        }
    }
    
    // [PENDING]
        /** The editor may be able to edit properties of different classes. It can decide to
         * be able to edit descendants of a base class.*/
    
    //Class propertyClass; // read-only property
    
    /**
     * A setter that should be used by the property editor
     * to change the state of the environment. 
     * Even the state property is bound, changes made from the editor itself
     * are allowed without restrictions.
     */
    public void setState (Object newState) {
        if (getState ().equals (newState))
            // no change, no fire vetoable and property change
            return ;
        try {
            getSupport().fireVetoableChange(PROP_STATE, getState (), newState);
            state = newState;
            
            // always notify state change
            getChange ().firePropertyChange (PROP_STATE, null, newState);
        } catch (PropertyVetoException pve) {
            // and notify the user that the change cannot happen
            PropertyDialogManager.notify(pve);
        }
    }

    /**
     * A getter for the current state of the environment.
     * @return one of the constants STATE_VALID, STATE_INVALID,
     * STATE_NEEDS_VALIDATION.
     */
    public Object getState () {
        return state;
    }

    /**
     * Vetoable change listener: listenning here you will be notified
     * when the state of the environment is being changed (when the setState
     * method is being called). You can veto the change and provide
     * a displayable information in the thrown exception. Use
     * the ErrorManager annotaion feature for the your exception to modify
     * the message and severity.
     */
    public void addVetoableChangeListener(VetoableChangeListener l) {
        getSupport().addVetoableChangeListener(l);
    }

    /**
     * Property change listener: listenning here you will be notified
     * when the state of the environment is has been changed.
     * @since 2.20
     */
    public void addPropertyChangeListener (PropertyChangeListener l) {
        getChange ().addPropertyChangeListener (l);
    }
    
    /**
     * Vetoable change listener removal.
     */
    public void removeVetoableChangeListener(VetoableChangeListener l) {
        getSupport().removeVetoableChangeListener(l);
    }
    
    /**
     * Removes Property change listener.
     * @since 2.20
     */
    public void removePropertyChangeListener (PropertyChangeListener l) {
        getChange ().removePropertyChangeListener (l);
    }
    
    
    /** Getter for property changeImmediate.
     * @return Value of property changeImmediate.
     */
    boolean isChangeImmediate() {
        return changeImmediate;
    }
    
    /** Setter for property changeImmediate.
     * @param changeImmediate New value of property changeImmediate.
     */
    void setChangeImmediate(boolean changeImmediate) {
        this.changeImmediate = changeImmediate;
    }

    /**
     * Lazy initialization of the VetoableChangeSupport.
     */
    private synchronized VetoableChangeSupport getSupport() {
        if (support == null) {
            support = new VetoableChangeSupport(this);
        }
        return support;
    }
    
    /**
     * Lazy initialization of the PropertyChangeSupport.
     */
    private synchronized PropertyChangeSupport getChange () {
        if (change == null) {
            change = new PropertyChangeSupport (this);
        }
        return change;
    }
    
}
  
