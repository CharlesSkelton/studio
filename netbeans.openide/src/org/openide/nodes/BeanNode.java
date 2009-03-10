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

package org.openide.nodes;

import java.awt.Image;
import java.beans.*;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextProxy;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;

import org.openide.ErrorManager;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.WeakListener;
import org.openide.util.actions.SystemAction;


/** Represents one JavaBean in the nodes hierarchy.
* It provides all methods that are needed for communication between
* the IDE and the bean.
* <p>You may use this node type for an already-existing JavaBean (possibly
* using BeanContext) in order for its JavaBean properties to be reflected
* as corresponding node properties. Thus, it serves as a compatibility wrapper.
*
* @author Jan Jancura, Ian Formanek, Jaroslav Tulach
*/
public class BeanNode extends AbstractNode {
    // static ..................................................................................................................

    /** Icon base for bean nodes */
    private static final String ICON_BASE = "org/openide/resources/beans"; // NOI18N

    private static Children getChildren (Object bean) {
        if (bean instanceof BeanContext)
            return new BeanChildren ((BeanContext)bean);
        if (bean instanceof BeanContextProxy) {
            BeanContextChild bch = ((BeanContextProxy)bean).getBeanContextProxy();
            if (bch instanceof BeanContext)
                return new BeanChildren ((BeanContext)bch);
        }
        return Children.LEAF;
    }


    // variables .............................................................................................................

    /** bean */
    private Object bean;


    /** bean info for the bean */
    private BeanInfo beanInfo;
    /** functions to operate on beans */
    private Method nameGetter   = null;
    private Method nameSetter   = null;
    /** remove PropertyChangeListener method */
    private Method removePCLMethod = null;

    /** listener for properties */
    private PropL propertyChangeListener = null;

    /** is synchronization of name in progress */
    private boolean synchronizeName;

    // init ..................................................................................................................

    /**
    * Constructs a node for a JavaBean. If the bean is a {@link BeanContext},
    * creates a child list as well.
    *
    * @param bean the bean this node will be based on
    * @throws IntrospectionException if the bean cannot be analyzed
    */
    public BeanNode (Object bean) throws IntrospectionException {
        this (
            bean,
            getChildren (bean)
        );
    }

    /** Constructs a node for a JavaBean with a defined child list.
    * Intended for use by subclasses with different strategies for computing the children.
    * @param bean the bean this node will be based on
    * @param children children for the node (default if null)
    * @throws IntrospectionException if the bean cannot be analyzed
    */
    protected BeanNode (Object bean, Children children) throws IntrospectionException {
        super (children == null ? getChildren(bean) : children);
        this.bean = bean;
        try {
            initialization ();
        } catch (IntrospectionException ie) {
            throw ie;
        } catch (RuntimeException re) {
            throw mkie(re);
        } catch (LinkageError le) {
            throw mkie(le);
        }
    }
    private static IntrospectionException mkie(Throwable t) {
        IntrospectionException ie = new IntrospectionException(t.toString());
        ErrorManager.getDefault().annotate(ie, t);
        return ie;
    }

    /** Set whether or not to keep the node name and Bean name synchronized automatically.
    * If enabled, the node will listen to changes in the name of the bean
    * and update the (system) name of the node appropriately. The name of the bean can
    * be obtained by calling <code>getName ()</code>, <code>getDisplayName ()</code> or from {@link BeanDescriptor#getDisplayName}.
    * <p>Also when the (system) name of the node is changing, the change propagates if possible to
    * methods <code>setName (String)</code> or <code>setDisplayName (String)</code>. (This
    * does not apply to setting the display name of the node, however.)
    * <P>
    * By default this feature is turned on.
    *  
    * @param watch <code>true</code> if the name of the node should be synchronized with
    *   the name of the bean, <code>false</code> if the name of the node should be independent
    *   or manually updated
    *
    */
    protected void setSynchronizeName (boolean watch) {
        synchronizeName = watch;
    }

    /** Provides access to the bean represented by this BeanNode.
    * @return instance of the bean represented by this BeanNode
    */
    protected Object getBean () {
        return bean;
    }

    /** Detaches all listeners from the bean and destroys it.
    * @throws IOException if there was a problem
    */
    public void destroy () throws IOException {
        if (removePCLMethod != null) {
            try {
                Object o = Beans.getInstanceOf (bean, removePCLMethod.getDeclaringClass ());
                removePCLMethod.invoke (o, new Object[] {propertyChangeListener});
            } catch (Exception e) {
                NodeOp.exception (e);
            }
        }
        super.destroy ();
    }


    /** Can this node be removed?
    * @return <CODE>true</CODE> in this implementation
    */
    public boolean canDestroy () {
        return true;
    }

    /** Set the node name.
    * Also may attempt to change the name of the bean,
    * according to {@link #setSynchronizeName}.
    * @param s the new name
    */
    public void setName (String s) {
        if (synchronizeName) {
            Method m = nameSetter;
            if (m != null) {
                try {
                    m.invoke (bean, new Object[] {s});
                } catch (Exception e) {
                    NodeOp.exception (e);
                }
            }
        }
        super.setName (s);
    }

    /** Can this node be renamed?
    * @return <code>true</code> if there is no name synchronization, or there is
    *         a valid setter method for the name
    */
    public boolean canRename () {
        return ! synchronizeName || nameSetter != null;
    }

    /** Get an icon for this node in the closed state.
    * Uses the Bean's icon if possible.
    *
    * @param type constant from {@link java.beans.BeanInfo}
    * @return icon to use
    */
    public Image getIcon (int type) {
        Image image = beanInfo.getIcon (type);
        if (image != null) return image;
        return super.getIcon(type);
    }

    /** Get an icon for this node in the open state.
    *
    * @param type type constants
    * @return icon to use. The default implementation just uses {@link #getIcon}.
    */
    public Image getOpenedIcon (int type) {
        return getIcon(type);
    }

    public HelpCtx getHelpCtx () {
        HelpCtx test = TMUtil.findHelp (this);
        if (test != null)
            return test;
        else
            return new HelpCtx (BeanNode.class);
    }

    /** Prepare node properties based on the bean, storing them into the current property sheet.
    * Called when the bean info is ready.
    * This implementation always creates a set for standard properties
    * and may create a set for expert ones if there are any.
    * @see #computeProperties
    * @param bean bean to compute properties for
    * @param info information about the bean
    */
    protected void createProperties (Object bean, BeanInfo info) {
        Descriptor d = computeProperties (bean, beanInfo);

        Sheet sets = getSheet ();
        Sheet.Set pset = Sheet.createPropertiesSet ();
        pset.put (d.property);
        
        BeanDescriptor bd = info.getBeanDescriptor();
        
        if ( bd != null && bd.getValue( "propertiesHelpID" ) != null ) {  // NOI18N      
            pset.setValue( "helpID", bd.getValue( "propertiesHelpID" ) ); // NOI18N
        }
            
        sets.put (pset);

        if (d.expert.length != 0) {
            Sheet.Set eset = Sheet.createExpertSet ();
            eset.put (d.expert);
            if ( bd != null && bd.getValue( "expertHelpID" ) != null ) {  // NOI18N      
                eset.setValue( "helpID", bd.getValue( "expertHelpID" ) ); // NOI18N
            }
            sets.put (eset);
        }
    }

    /** Can this node be copied?
    * @return <code>true</code> in the default implementation
    */
    public boolean canCopy () {
        return true;
    }

    /** Can this node be cut?
    * @return <code>false</code> in the default implementation
    */
    public boolean canCut () {
        return false;
    }

    /* Getter for set of actions that should be present in the
    * popup menu of this node. This set is used in construction of
    * menu returned from getContextMenu and specially when a menu for
    * more nodes is constructed.
    *
    * @return array of system actions that should be in popup menu
    */
    protected SystemAction[] createActions () {
        return NodeOp.createFromNames (new String[] {
            "CustomizeBean", null, "Copy", null, "Tools", "Properties" // NOI18N
        });
    }

    /* Test if there is a customizer for this node. If <CODE>true</CODE>
    * the customizer can be obtained via <CODE>getCustomizer</CODE> method.
    *
    * @return <CODE>true</CODE> if there is a customizer.
    */
    public boolean hasCustomizer () {
        // true if we have already computed beanInfo and it has customizer class
        return beanInfo.getBeanDescriptor ().getCustomizerClass () != null;
    }

    /* Returns the customizer component.
    * @return the component or <CODE>null</CODE> if there is no customizer
    */
    public java.awt.Component getCustomizer () {
        Class clazz = beanInfo.getBeanDescriptor ().getCustomizerClass ();
        if (clazz == null) return null;

        Object o;
        try {
            o = clazz.newInstance ();
        } catch (InstantiationException e) {
            NodeOp.exception (e);
            return null;
        } catch (IllegalAccessException e) {
            NodeOp.exception (e);
            return null;
        }
        
        if (! (o instanceof Customizer) ) {
            // no customizer => no fun
            // [PENDING] this ought to perform some sort of notification!
            return null;
        }

        Customizer cust = ((java.beans.Customizer)o);

        TMUtil.attachCustomizer (this, cust);
        
        // looking for the component
        java.awt.Component comp = null;
        if (o instanceof java.awt.Component) {
            comp = (java.awt.Component)o;
        } else {
            // create the dialog from descriptor
            comp = TMUtil.createDialog (o);
        }
        
        if (comp == null) {
            // no component provided
            return null;
        }

        cust.setObject (bean);

        if (removePCLMethod == null) {
            cust.addPropertyChangeListener (
                new PropertyChangeListener () {
                    public void propertyChange(PropertyChangeEvent e) {
                        firePropertyChange (
                            e.getPropertyName (), e.getOldValue (), e.getNewValue ()
                        );
                    }
                });
        }
        
        

        return comp;
    }


    /** Computes a descriptor for properties from a bean info.
    * <p>Property code names are taken from the property descriptor names
     * according to the JavaBeans specification. For example, a pair of
     * methods <code>getFoo</code> and <code>setFoo</code> would result in
     * a node property with code name <code>foo</code>. If you call
     * <code>MyBean.setFoo(...)</code>, this should result in a property
     * change event with name <code>foo</code>; if you are using these
     * properties in some other context (attached to something other than
     * a <code>BeanNode</code>) then be careful to fire changes with the correct
     * name, or there may be problems with refreshing display of the property etc.
    * @param bean bean to create properties for
    * @param info about the bean
    * @return three property lists
    */
    public static Descriptor computeProperties (Object bean, BeanInfo info) {
        ArrayList property = new ArrayList ();
        ArrayList expert = new ArrayList ();
        ArrayList hidden = new ArrayList ();

        PropertyDescriptor[] propertyDescriptor = info.getPropertyDescriptors ();

        int k = propertyDescriptor.length;
        for (int i = 0; i < k; i ++) {
            if (propertyDescriptor[i].getPropertyType() == null)
                continue;

            Node.Property prop;

            if (propertyDescriptor[i] instanceof IndexedPropertyDescriptor) {
                IndexedPropertyDescriptor p = (IndexedPropertyDescriptor) propertyDescriptor [i];

                if ((p.getReadMethod() != null) && (!p.getReadMethod().getReturnType().isArray())) {
                    // this is fix for #17728. This situation should never happen
                    // But if the BeanInfo (IndexedPropertyDescriptor) is wrong
                    // we will ignore this property
                    continue;
                }
                
                IndexedPropertySupport support =  new IndexedPropertySupport (
                                                      bean, p.getPropertyType (),
                                                      p.getIndexedPropertyType(), p.getReadMethod (), p.getWriteMethod (),
                                                      p.getIndexedReadMethod (), p.getIndexedWriteMethod ()
                                                  );
                support.setName (p.getName ());
                support.setDisplayName (p.getDisplayName ());
                support.setShortDescription (p.getShortDescription ());
                
                for (Enumeration e = p.attributeNames(); e.hasMoreElements();) {
                    String aname = (String)e.nextElement();
                    support.setValue(aname, p.getValue(aname));
                }
                
                prop = support;
            } else {
                PropertyDescriptor p = propertyDescriptor [i];
                // Note that PS.R sets the method accessible even if it is e.g.
                // defined as public in a package-accessible superclass.
                PropertySupport.Reflection support = new PropertySupport.Reflection (
                                                         bean, p.getPropertyType (),
                                                         p.getReadMethod (), p.getWriteMethod ()
                                                     );
                support.setName (p.getName ());
                support.setDisplayName (p.getDisplayName ());
                support.setShortDescription (p.getShortDescription ());
                support.setPropertyEditorClass (p.getPropertyEditorClass ());

                for (Enumeration e = p.attributeNames(); e.hasMoreElements();) {
                    String aname = (String)e.nextElement();
                    support.setValue(aname, p.getValue(aname));
                }
                
                prop = support;
            }
            // Propagate helpID's.
            Object help = propertyDescriptor[i].getValue ("helpID"); // NOI18N
            if (help != null && (help instanceof String)) {
                prop.setValue ("helpID", help); // NOI18N
            }
            // Add to right category.
            if (propertyDescriptor[i].isHidden ()) {
                // hidden property
                hidden.add (prop);
            } else {
                if (propertyDescriptor[i].isExpert ()) {
                    expert.add (prop);
                } else {
                    property.add (prop);
                }
            }
        }// for

        return new Descriptor (property, expert, hidden);
    }



    //
    //
    // Initialization methods
    //
    //


    /** Performs initalization of the node
    */
    private void initialization () throws IntrospectionException {
        setIconBase (ICON_BASE);
        
        // default action is org.openide.actions.PropertiesAction
        SystemAction[] arr = NodeOp.createFromNames (new String[] { "Properties" }); // NOI18N
        if (arr.length != 0) {
            setDefaultAction (arr[0]);
        }

        setSynchronizeName (true);

        // Find the first public superclass of the actual class.
        // Should not introspect on a private class, because then the method objects
        // used for the property descriptors will not be callable without an
        // IllegalAccessException, even if overriding a public method from a public superclass.
        Class clazz = bean.getClass ();
        while (! Modifier.isPublic (clazz.getModifiers ()) && !hasExplicitBeanInfo (clazz)) {
            clazz = clazz.getSuperclass ();
            if (clazz == null) clazz = Object.class; // in case it was an interface
        }
        beanInfo = Utilities.getBeanInfo (clazz);

        // resolving the name of this bean
        registerName ();
        setNameSilently (getNameForBean ());
        BeanDescriptor descriptor = beanInfo.getBeanDescriptor ();
        String sd = descriptor.getShortDescription ();
        if (! Utilities.compareObjects (sd, descriptor.getDisplayName ()))
            setShortDescription (sd);

        // add propertyChangeListener
        EventSetDescriptor[] eventSetDescriptors = beanInfo.getEventSetDescriptors();
        int i, k = eventSetDescriptors.length;
        Method method = null;
        for (i = 0; i < k; i++) {
            method = eventSetDescriptors [i].getAddListenerMethod ();
            if (method != null &&
                    method.getName().equals("addPropertyChangeListener") && // NOI18N
                    // Possible for a public class to extend a package-private class,
                    // where the private class defines addPropertyChangeListener, in which
                    // case the introspector lists an inaccessible method in the event
                    // set descriptor. In such a case, do not try to add a listener.
                    Modifier.isPublic(method.getModifiers())) {
                break;
            }
        }
        if (i != k) {
            try {
                Object o = Beans.getInstanceOf (bean, method.getDeclaringClass ());
                propertyChangeListener = new PropL ();
                method.invoke (o, new Object[] { WeakListener.propertyChange (propertyChangeListener, o) });
                removePCLMethod = eventSetDescriptors [i].getRemoveListenerMethod ();
            } catch (Exception e) {
                // Warning, not info: likely to call e.g. getters or other things used
                // during startup of the bean, so it is not good to swallow errors here
                // (e.g. SharedClassObject.initialize throws RuntimeException -> it is
                // caught here and probably someone wants to know).
                ErrorManager.getDefault().annotate(e, ErrorManager.UNKNOWN, "Trying to invoke " + method + " where introspected class is " + clazz.getName(), null, null, null); // NOI18N
                NodeOp.warning (e);
            }
        }

        createProperties (bean, beanInfo);
        
        for (Enumeration e = beanInfo.getBeanDescriptor().attributeNames(); e.hasMoreElements();) {
            String aname = (String)e.nextElement();
            setValue(aname, beanInfo.getBeanDescriptor().getValue(aname));
        }

        Node.Cookie instanceCookie = TMUtil.createInstanceCookie (bean);
        if (instanceCookie != null) {
            getCookieSet ().add (instanceCookie);
        }
    }
    
    /** Checks whether there is an explicit bean info for given class.
    * @param clazz the class to test
    * @return true if explicit bean info exists
    */
    private boolean hasExplicitBeanInfo (Class clazz) {
        String className = clazz.getName ();
        int indx = className.lastIndexOf('.');
        className = className.substring (indx + 1);
        
        String[] paths = Introspector.getBeanInfoSearchPath();
        for (int i = 0; i < paths.length; i++) {
            String s = paths[i] + '.' + className + "BeanInfo"; // NOI18N
            try {
                // test if such class exists
                Class.forName (s);
                return true;
            } catch (ClassNotFoundException ex) {
                // OK, this is normal.
            }
        }
        return false;
    }

    // name resolving methods

    /**
    * Finds setter and getter methods for the name of the bean. Resisters listener
    * for changing of name.
    */
    private void registerName () {
        // [PENDING] ought to use introspection, rather than look up the methods by name  --jglick
        Class clazz = bean.getClass ();
        // Do not want to use getName, even if public, on a private class:
        while (! Modifier.isPublic (clazz.getModifiers ())) {
            clazz = clazz.getSuperclass ();
            if (clazz == null) clazz = Object.class;
        }
        Class[] param = new Class [0];

        // find getter for the name
        try {
            try {
                nameGetter = clazz.getMethod ("getName", param); // NOI18N
                if (nameGetter.getReturnType () != String.class) throw new NoSuchMethodException ();
            } catch (NoSuchMethodException e) {
                try {
                    nameGetter = clazz.getMethod ("getDisplayName", param); // NOI18N
                    if (nameGetter.getReturnType () != String.class) throw new NoSuchMethodException ();
                } catch (NoSuchMethodException ee) {
                    nameGetter = null;
                    return;
                }
            }
        } catch (SecurityException se) {
            NodeOp.exception (se);
            nameGetter = null;
            return;
        }

        // this code tests wheter everything is fine and the getter is
        // invokable
        try {
            // make sure this is cast to String too:
            String result = (String) nameGetter.invoke (bean, null);
        } catch (Exception e) {
            ErrorManager em = ErrorManager.getDefault ();
	    em.annotate (e, ErrorManager.WARNING,
                "Bad method: " + clazz.getName () + "." + nameGetter.getName (), //NOI18N
            		    null, null, null);
	    em.notify ( ErrorManager.WARNING, e);
            
            nameGetter = null;
            return;
        }

        // find the setter for the name
        param = new Class[] {String.class};
        try {
            try {
                // tries to find method setName (String)
                nameSetter = clazz.getMethod ("setName", param); // NOI18N
                if (nameSetter.getReturnType () != Void.TYPE) throw new NoSuchMethodException ();
            } catch (NoSuchMethodException e) {
                try {
                    nameSetter = clazz.getMethod ("setDisplayName", param); // NOI18N
                    if (nameSetter.getReturnType () != Void.TYPE) throw new NoSuchMethodException ();
                } catch (NoSuchMethodException ee) {
                    nameSetter = null;
                }
            }
        } catch (SecurityException se) {
            NodeOp.exception (se);
        }

    }

    /**
    * Returns name of the bean.
    */
    private String getNameForBean () {
        if (nameGetter != null) {
            try {
                String name = (String) nameGetter.invoke (bean, null);
                return name != null ? name : ""; // NOI18N
            } catch (Exception ex) {
                NodeOp.warning (ex);
            }
        }
        BeanDescriptor descriptor = beanInfo.getBeanDescriptor ();
        return descriptor.getDisplayName ();
    }

    /** To allow innerclasses to access the super.setName method.
    */
    void setNameSilently (String name) {
        super.setName (name);
    }


    /** Descriptor of three types of properties. Regular,
    * expert and hidden.
    */
    public static final class Descriptor extends Object {
        /** Regular properties. */
        public final Node.Property[] property;
        /** Expert properties. */
        public final Node.Property[] expert;
        /** Hidden properties. */
        public final Node.Property[] hidden;

        /** private constructor */
        Descriptor (ArrayList p, ArrayList e, ArrayList h) {
            property = new Node.Property[p.size ()];
            p.toArray (property);

            expert = new Node.Property[e.size ()];
            e.toArray (expert);

            hidden = new Node.Property[h.size ()];
            h.toArray (hidden);
        }
    }

    /** Property change listener to update the properties of the node and
    * also the name of the node (sometimes)
    */
    private final class PropL extends Object implements PropertyChangeListener {
        PropL() {}
        public void propertyChange(PropertyChangeEvent e) {
            firePropertyChange (e.getPropertyName (), e.getOldValue (), e.getNewValue ());

            if (synchronizeName) {
                String name = e.getPropertyName ();
                if (name == null || name.equals ("name") || name.equals ("displayName")) { // NOI18N
                    String newName = getNameForBean ();
                    if (!newName.equals (getName ())) {
                        setNameSilently (newName);
                    }
                }
            }
        }
    }
}
