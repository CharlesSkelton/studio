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

import java.util.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.openide.ErrorManager;

import org.openide.actions.*;
import org.openide.nodes.*;
import org.openide.util.HelpCtx;
import org.openide.util.WeakListener;

/** 
 * A node used by PropertySheet to display common properties of
 * more nodes.
 * @author David Strupl
 */
final class ProxyNode extends AbstractNode {

    private Node[] original;
    private PropertyChangeListener pcl;
    
    public ProxyNode(Node[] original) {
        super (Children.LEAF);
        this.original = original;
        pcl = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce) {
                firePropertyChange(pce.getPropertyName(), pce.getOldValue(), pce.getNewValue());
            }
        };
        for (int i = 0; i < original.length; i++) {
            original[i].addPropertyChangeListener(
                WeakListener.propertyChange(pcl, original[i]));
        }
    }

    public HelpCtx getHelpCtx () {
        for (int i = 0; i < original.length; i++) {
            if (original[i].getHelpCtx() != HelpCtx.DEFAULT_HELP) {
                return original[i].getHelpCtx();
            }
        }
        return HelpCtx.DEFAULT_HELP;
    }

    public Node cloneNode () {
        return new ProxyNode(original);
    }

    protected Sheet createSheet () {
	Sheet sheet = super.createSheet ();
        Sheet.Set[] computedSet = computePropertySets();
        for (int i = 0; i < computedSet.length; i++) {
            sheet.put(computedSet[i]);
       }
        return sheet;
    }
    
    /** */
    Node[] getOriginalNodes() {
        return original;
    }
    
    /** Computes intersection of tabs and intersection
     * of properties in those tabs.
     */
    private Sheet.Set[] computePropertySets() {
        if (original.length > 0) {
            Node.PropertySet []firstSet = original[0].getPropertySets();
            java.util.Set sheets = new HashSet(
                Arrays.asList(firstSet));
            
            // compute intersection of all Node.PropertySets for given nodes
            for (int i = 1; i < original.length; i++) {
                sheets.retainAll(
                    new HashSet(Arrays.asList(original[i].getPropertySets())));
            }
            
            ArrayList resultSheets = new ArrayList(sheets.size());
            // now for all resulting sheets take common properties
            for (int i = 0; i < firstSet.length; i++) {
                if (! sheets.contains(firstSet[i]) || firstSet[i].isHidden ()) {
                    continue;
                }
                Node.PropertySet current = firstSet[i];
                
                // creates an empty Sheet.Set with same names as current
                Sheet.Set res = new Sheet.Set();
                res.setName(current.getName());
                res.setDisplayName(current.getDisplayName());
                res.setShortDescription(current.getShortDescription());
                java.util.Set props = new HashSet(
                    Arrays.asList(current.getProperties()));
                
                // intersection of properties from the corresponding tabs
                for (int j = 0; j < original.length; j++) {
                    Node.PropertySet[] p = original[j].getPropertySets();
                    for (int k = 0; k < p.length; k++) {
                        Node.Property[] arr = p[k].getProperties();
                        if (current.getName().equals(p[k].getName())) {
                            props.retainAll(new HashSet(
                                Arrays.asList(p[k].getProperties())));
                        }
                    }
                }
                Node.Property []p = current.getProperties();
                for (int j = 0; j < p.length; j++) {
                    if (! props.contains(p[j])) {
                        continue;
                    }
                    if (p[j].isHidden ()) {
                        continue;
                    }
                    ProxyProperty pp = createProxyProperty(
                        p[j].getName(),
                        res.getName()
                    );
                    res.put(pp);
                }
                resultSheets.add(res);
            }
            
            return (Sheet.Set[])resultSheets.toArray(
                new Sheet.Set[resultSheets.size()]);
        }
        return new Sheet.Set[0];
    }

    /** Finds properties in original with specified
     * name in all tabs and constructs a ProxyProperty instance.
     */
    private ProxyProperty createProxyProperty(String propName, String setName) {
        Node.Property []arr = new Node.Property[original.length];
        for (int i = 0; i < original.length; i++) {
            Node.PropertySet[] p = original[i].getPropertySets();
            for (int j = 0; j < p.length; j++) {
                if (p[j].getName().equals(setName)) {
                    Node.Property[] np = p[j].getProperties();
                    for (int k = 0; k < np.length; k++) {
                        if (np[k].getName().equals(propName)) {
                            arr[i] = np[k];
                        }
                    }
                }
            }
        }
        return new ProxyProperty(arr);
    }
    
    /** Property delegating to an array of Properties. It either
     * delegates to original[0] or applies changes to all
     * original properties.
     */
    private static class ProxyProperty extends Node.Property {
        
        private Node.Property[] original;
       
        /** It sets name, displayName and short description.
         * Remembers original.
         */
        public ProxyProperty(Node.Property[] original) {
            super(original[0].getValueType());
            this.original = original;
            setName(original[0].getName());
            setDisplayName(original[0].getDisplayName());
            setShortDescription(original[0].getShortDescription());
        }
        
        /** Test whether the property is writable.Calls all delegates.
         * If any of them returns false returns false, otherwise return true.
         */
        public boolean canWrite() {
            for (int i = 0; i < original.length; i++) {
                if (!original[i].canWrite()) {
                    return false;
                }
            }
            return true;
        }
        
        /** Test whether the property is readable. Calls all delegates.
         * If any of them returns false returns false, otherwise return true.
         * @return <CODE>true</CODE> if all delegates returned true
         */
        public boolean canRead() {
            for (int i = 0; i < original.length; i++) {
                if (!original[i].canRead()) {
                    return false;
                }
            }
            return true;
        }
        
        /** If all values are the same returns the value otherwise returns null.
         * @return the value of the property
         * @exception IllegalAccessException cannot access the called method
         * @exception InvocationTargetException an exception during invocation
         */
        public Object getValue() throws IllegalAccessException, java.lang.reflect.InvocationTargetException {
            Object o = original[0].getValue();
            if (o == null) {
                return null;
            }
            for (int i = 0; i < original.length; i++) {
                if (! o.equals(original[i].getValue())) {
                    throw new DifferentValuesException();
                }
            }
            return o;
        }
        
        /** Set the value. Calls setValue on all delegates.
         * @param val the new value of the property
         * @exception IllegalAccessException cannot access the called method
         * @exception IllegalArgumentException wrong argument
         * @exception InvocationTargetException an exception during invocation
         */
        public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException, java.lang.reflect.InvocationTargetException {
            for (int i = 0; i < original.length; i++) {
                original[i].setValue(val);
            }
        }
        
        /** Retrieve a named attribute with this feature.
         * If all values are the same returns the value otherwise returns null.
         * @param attributeName  The locale-independent name of the attribute
         * @return The value of the attribute.  May be null if
         *      the attribute is unknown.
         */
        public Object getValue (String attributeName) {
            Object o = original[0].getValue (attributeName);
            if (o == null) {
                return null;
            }
            for (int i = 0; i < original.length; i++) {
                if (! o.equals (original[i].getValue (attributeName))) {
                    // avoid propagate DifferentValuesException outside propertysheet package
                    //throw new DifferentValuesException();
                    // notify in log as informational and retrun null
                    ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL,
                        new DifferentValuesException ("Different values in attribute "+attributeName)); // NOI18N
                    return null;
                }
            }
            return o;
        }
        
        /** Associate a named attribute with this feature. Calls setValue on all delegates.
         * @param attributeName  The locale-independent name of the attribute
         * @param value  The value.
         */
        public void setValue (String attributeName, Object value) {
            for (int i = 0; i < original.length; i++) {
                original[i].setValue (attributeName, value);
            }
        }
        
        /**
         * @returns property editor from the first delegate
         */
        public java.beans.PropertyEditor getPropertyEditor () {
            return original[0].getPropertyEditor();
        }
        
        /** Test whether the property has a default value. If any of
         * the delegates does not support default value returns false,
         * otherwise returns true.
         * @return <code>true</code> if all delegates returned true
         */
        public boolean supportsDefaultValue () {
            for (int i = 0; i < original.length; i++) {
                if (!original[i].supportsDefaultValue()) {
                    return false;
                }
            }
            return true;
        }

        /** 
         * Calls restoreDefaultValue on all delegates (original).
         * @exception IllegalAccessException cannot access the called method
         * @exception InvocationTargetException an exception during invocation
         */
        public void restoreDefaultValue() throws IllegalAccessException, java.lang.reflect.InvocationTargetException {
            for (int i = 0; i < original.length; i++) {
                original[i].restoreDefaultValue();
            }
        }
    }
    
    /** We cannot return a single value when there are different values */
    static class DifferentValuesException extends RuntimeException {
        public DifferentValuesException () {
            super ();
        }
        public DifferentValuesException (String message) {
            super (message);
        }
    }
}
