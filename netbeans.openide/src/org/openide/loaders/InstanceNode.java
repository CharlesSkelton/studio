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

import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextProxy;
import java.beans.beancontext.BeanContextMembershipEvent;
import java.beans.beancontext.BeanContextMembershipListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openide.*;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.util.*;
import org.openide.util.actions.*;
import org.openide.nodes.*;

/** Node to represent a .settings, .ser or .instance file.
 *
 * @author  Jaroslav Tulach, Jan Pokorsky
 */
final class InstanceNode extends DataNode {
    
    /** icon base */
    private static final String INSTANCE_ICON_BASE =
        "org/openide/resources/instanceObject"; // NOI18N
    
    /** File extension for xml settings. */
    private static final String XML_EXT = "settings"; //NOI18N
    
    /** listener for properties */
    private PropL propertyChangeListener = null;
    private PropertyChangeListener dobjListener;
    private boolean isSheetCreated = false;
    /** bean info is not used only if the file specifies 
     * <attr name="beaninfo" booleanvalue="false" />
     */
    private boolean noBeanInfo = false;

    /** Constructor */
    public InstanceNode (InstanceDataObject obj) {
        this (obj,  Boolean.FALSE.equals (obj.getPrimaryFile ().getAttribute ("beaninfo"))); // NOI18N
    }
     
    /** @param obj the object to use
     * @param noBeanInfo info to use
     */
    private InstanceNode (InstanceDataObject obj, boolean noBeanInfo) {
        super (obj, getChildren(obj, noBeanInfo));
        
        initIconBase();

        this.noBeanInfo = noBeanInfo;
        
        if (!noBeanInfo && !getDataObject().getPrimaryFile().hasExt(XML_EXT)) {
            initName();
        }
        
        // listen on the cookie change of the instance data object
        dobjListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(DataObject.PROP_COOKIE)) {
                    if (propertyChangeListener != null) {
                        propertyChangeListener.destroy();
                        propertyChangeListener = null;
                    }
                    if (InstanceNode.this.noBeanInfo || ic() == null) {
                       initIconBase();
                    } else {
                       fireIconChange();
                    }
                    fireNameChange(null, null);
                    fireDisplayNameChange(null, null);
                    fireShortDescriptionChange(null, null);
                    if (isSheetCreated) setSheet(createSheet());
                }
            }
        };
        obj.addPropertyChangeListener(WeakListener.propertyChange(dobjListener, obj));
    }
    
    /** initialize the icon base according to state of the settings instance (valid/broken) */
    private void initIconBase() {
        InstanceCookie.Of ic = ic();
        String iconBase = INSTANCE_ICON_BASE;
        if (ic == null) {//XXX && io.instanceOf(XMLSettingsSupport.BrokenSettings.class)) {
            iconBase = "org/openide/resources/instanceBroken"; // NOI18N
        }
        setIconBase(iconBase);
    }
    
    private static Children getChildren(DataObject dobj, boolean noBeanInfo) {
        if (noBeanInfo) {
            return Children.LEAF;
        }
        InstanceCookie inst = (InstanceCookie)dobj.getCookie(InstanceCookie.class);
        if (inst == null) return Children.LEAF;
        try {
            Class clazz = inst.instanceClass();
            if (BeanContext.class.isAssignableFrom(clazz) ||
                BeanContextProxy.class.isAssignableFrom(clazz)) {
                return new InstanceChildren ((InstanceDataObject) dobj);
            } else {
                return Children.LEAF;
            }
        } catch (Exception ex) {
            return Children.LEAF;
        }
    }
    
    /** Getter for instance data object.
     * @return instance data object
     */
    private InstanceDataObject i () {
        return (InstanceDataObject)getDataObject ();
    }
    
    private InstanceCookie.Of ic () {
        return (InstanceCookie.Of) getDataObject().getCookie(InstanceCookie.Of.class);
    }

    /** Find an icon for this node (in the closed state).
    * @param type constant from {@link java.beans.BeanInfo}
    * @return icon to use to represent the node
    */
    public Image getIcon (int type) {
        if (noBeanInfo) return super.getIcon(type);
        Image img = null;
        try {
            DataObject dobj = getDataObject();
            img = dobj.getPrimaryFile().getFileSystem().getStatus().
                annotateIcon (img, type, dobj.files ());
        } catch (FileStateInvalidException e) {
            // no fs, do nothing
        }
        
        if (img == null) img = initIcon(type);
        if (img == null) img = super.getIcon(type);
        return img;
    }

    /** Find an icon for this node (in the open state).
    * This icon is used when the node may have children and is expanded.
    *
    * @param type constant from {@link java.beans.BeanInfo}
    * @return icon to use to represent the node when open
    */
    public Image getOpenedIcon (int type) {
        return getIcon (type);
    }

    /** try to register PropertyChangeListener to instance to fire its changes.*/
    private void initPList () {
        try {
            InstanceCookie ic = ic();
            if (ic == null) return;
            BeanInfo info = Utilities.getBeanInfo(ic.instanceClass());
            java.beans.EventSetDescriptor[] descs = info.getEventSetDescriptors();
            Method setter = null;
            for (int i = 0; descs != null && i < descs.length; i++) {
                setter = descs[i].getAddListenerMethod();
                if (setter != null && setter.getName().equals("addPropertyChangeListener")) { // NOI18N
                    Object bean = ic.instanceCreate();
                    propertyChangeListener = new PropL();
                    setter.invoke(bean, new Object[] {WeakListener.propertyChange(propertyChangeListener, bean)});
                }
            }
        } catch (Exception ex) {
        } catch (LinkageError ex) {
            // #30650 - catch also LinkageError.
            // Ignoring exception the same way as the Exception handler above.
        }
    }
    
    private Image initIcon (int type) {
        Image beanInfoIcon = null;
        try {
            InstanceCookie ic = ic();
            if (ic == null) return null;
            Class clazz = ic.instanceClass();
            //Fixed bug #5610
            //Class javax.swing.JToolBar$Separator does not have icon
            //we will use temporarily icon from javax.swing.JSeparator
            //New icon is requested.

            String className = clazz.getName ();
            BeanInfo bi;
            if (
                className.equals ("javax.swing.JSeparator") ||  // NOI18N
                className.equals ("javax.swing.JToolBar$Separator") // NOI18N
            ) {
                Class clazzTmp = Class.forName ("javax.swing.JSeparator"); // NOI18N
                bi = Utilities.getBeanInfo (clazzTmp);
            } else {
                bi = Utilities.getBeanInfo (clazz);
            }

            if (bi != null) {
                beanInfoIcon = bi.getIcon (type);
                if (beanInfoIcon != null) {
                    beanInfoIcon = toBufferedImage(beanInfoIcon, true);
                }
            }
            // Also specially handle SystemAction's.
            if (SystemAction.class.isAssignableFrom (clazz)) {
                SystemAction action = SystemAction.get (clazz);
                if (beanInfoIcon == null) {
                    Icon icon = action.getIcon ();
                    // [PENDING] not very pretty, but there is no good way to
                    // get an Image from an Icon that I know of
                    if (icon instanceof ImageIcon) {
                        beanInfoIcon = ((ImageIcon) icon).getImage ();
                    }
                }
            }
        } catch (Exception e) {
            // Problem ==>> use default icon
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
        } catch (LinkageError e) {
            // #30650 - catch also LinkageError.
            // Problem ==>> use default icon
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
        }

        return beanInfoIcon;
    }

    /** try to initialize display name.
    */
    private void initName() {
        Class clazz;
        try {
            InstanceCookie ic = ic();
            if (ic == null) return;
            clazz = ic.instanceClass();
        } catch (Exception e) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, e);
            return;
        }
        
        String className = clazz.getName ();
        if (className.equals ("javax.swing.JSeparator") || // NOI18N
            className.equals ("javax.swing.JToolBar$Separator")) { // NOI18N
                
            setDisplayName (NbBundle.getMessage (InstanceDataObject.class,
                "LBL_separator_instance")); // NOI18N
            return;
        }
        // Also specially handle SystemAction's.
        if (SystemAction.class.isAssignableFrom (clazz)) {
            SystemAction action = SystemAction.get (clazz);
            // Set node's display name.
            String name = action.getName ();
            
            // #31227 - some action does not implement its name properly.
            // Throw exception with the name of the class.
            if (name == null) {
                ErrorManager.getDefault().notify(
                    new RuntimeException("Please attach following information to the issue " + // NOI18N
                    "<http://www.netbeans.org/issues/show_bug.cgi?id=31227>: " + // NOI18N
                    "SystemAction " + className + " does not implement getName() properly. It returns null!")); // NOI18N
                setDisplayName(className);
                return;
            }
            
            int amper = name.indexOf ((char) '&');
            if (amper != -1)
                name = name.substring (0, amper) + name.substring (amper + 1);
            if (name.endsWith ("...")) // NOI18N
                name = name.substring (0, name.length () - 3);
            name = name.trim ();
            setDisplayName (name);
            return;
        }
        setDisplayName(getDataObject().getName());
    }
    
    /** Try to get display name of the bean.
     */
    private String getNameForBean() {
        try {
            InstanceCookie ic = ic();
            if (ic == null) {
                // it must be unrecognized setting
                return NbBundle.getMessage(InstanceNode.class,
                    "LBL_BrokenSettings"); //NOI18N
            }
            Class clazz = ic.instanceClass();
            Method nameGetter;
            Class[] param = new Class [0];
            try {
                nameGetter = clazz.getMethod ("getName", param); // NOI18N
                if (nameGetter.getReturnType () != String.class) throw new NoSuchMethodException ();
            } catch (NoSuchMethodException e) {
                try {
                    nameGetter = clazz.getMethod ("getDisplayName", param); // NOI18N
                    if (nameGetter.getReturnType () != String.class) throw new NoSuchMethodException ();
                } catch (NoSuchMethodException ee) {
                    return null;
                }
            }
            Object bean = ic.instanceCreate();
            return (String) nameGetter.invoke (bean, null);
        } catch (Exception ex) {
            return null;
        }
    }
    
    /** try to find setter setName/setDisplayName, if none declared return null */
    private Method getDeclaredSetter() {
        Method nameSetter = null;
        try {
            InstanceCookie ic = ic();
            if (ic == null) return null;
            Class clazz = ic.instanceClass();
            Class[] param = new Class[] {String.class};
            // find the setter for the name
            try {
                nameSetter = clazz.getMethod ("setName", param); // NOI18N
            } catch (NoSuchMethodException e) {
                nameSetter = clazz.getMethod ("setDisplayName", param); // NOI18N
            }
        } catch (Exception ex) {
        }
        return nameSetter;
    }
    
    public void setName(String name) {
        if (!getDataObject().getPrimaryFile().hasExt(XML_EXT)) {
            super.setName(name);
            return ;
        }
        String old = getNameImpl();
        if (old != null && old.equals(name)) return;
        InstanceCookie ic = ic();
        if (ic == null) {
            super.setName(name);
            return;
        }
        
        Method nameSetter = getDeclaredSetter();
        if (nameSetter != null) {
            try {
                Object bean = ic.instanceCreate();
                nameSetter.invoke(bean, new Object[] {name});
                i().scheduleSave();
            } catch (Exception ex) {
            }
        }
        super.setName(name);
    }
    
    /** Get the display name for the node.
     * A filesystem may {@link org.openide.filesystems.FileSystem#getStatus specially alter} this.
     * @return the desired name
    */
    public String getDisplayName () {
        String name = (String) getDataObject().getPrimaryFile().
            getAttribute(InstanceDataObject.EA_NAME);
        if (name == null) {
            try {
                String def = "\b"; // NOI18N
                FileSystem.Status fsStatus = getDataObject().getPrimaryFile().
                    getFileSystem().getStatus();
                name = fsStatus.annotateName(def, getDataObject().files());
                if (name.indexOf(def) < 0) {
                    return name;
                } else {
                    name = getNameForBean();
                    if (name != null) {
                        name = fsStatus.annotateName (name, getDataObject().files());
                    } else {
                        name = super.getDisplayName();
                    }
                }
            } catch (FileStateInvalidException e) {
                // no fs, do nothing
            }
        }
        return name;
    }
    
    /** try to get name by colling getter on the instance. */
    private String getNameImpl() {
        String name;
        name = getNameForBean();
        if (name == null) name = getName();

        return name;
    }
    
    protected Sheet createSheet () {
        Sheet orig;
    
        if (getDataObject ().getPrimaryFile ().hasExt ("ser") || // NOI18N
            getDataObject ().getPrimaryFile ().hasExt (XML_EXT)) {
            orig = new Sheet();
            changeSheet (orig);
        } else {
            // just instance file, change here
            orig = super.createSheet ();
            Sheet.Set props = orig.get (Sheet.PROPERTIES);
            final InstanceCookie ic = ic();
            if (ic == null) {
                props.put (new PropertySupport.ReadOnly (
                    "className", String.class, // NOI18N
                    NbBundle.getMessage (InstanceDataObject.class, "PROP_instance_class"), // NOI18N
                    NbBundle.getMessage (InstanceDataObject.class, "HINT_instance_class") // NOI18N
                ) {
                    public Object getValue () {
                        return ic.instanceName ();
                    }
                });
            }
        }
        
        isSheetCreated = true;
        return orig;
    }
        
        
    private void changeSheet (Sheet orig) {
        Sheet.Set props = orig.get (Sheet.PROPERTIES);

        try {
            InstanceCookie ic = ic();
            if (ic == null) return;
            // properties
            BeanInfo beanInfo = Utilities.getBeanInfo (ic.instanceClass ());
            BeanNode.Descriptor descr = BeanNode.computeProperties (ic.instanceCreate (), beanInfo);
            initPList();

            props = Sheet.createPropertiesSet();
            if (descr.property != null) {
                convertProps (props, descr.property, i ());
            }
            orig.put (props);

            if (descr.expert != null && descr.expert.length != 0) {
                Sheet.Set p = Sheet.createExpertSet();
                convertProps (p, descr.expert, i ());
                orig.put (p);
            }
        } catch (ClassNotFoundException ex) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
        } catch (IOException ex) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
        } catch (IntrospectionException ex) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
        } catch (LinkageError ex) {
            // #30650 - catch also LinkageError.
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
        }
    }
    
    
    /** Method that converts properties of an object.
     * @param set set to add properties to
     * @param arr array of Node.Property and Node.IndexedProperty
     * @param ido IDO providing task to invoke when a property changes
     */
    private static final void convertProps (
        Sheet.Set set, Node.Property[] arr, InstanceDataObject ido
    ) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] instanceof Node.IndexedProperty) {
                set.put (new I ((Node.IndexedProperty)arr[i], ido));
            } else {
                set.put (new P (arr[i], ido));
            }
        }
    }        
    
    /** The method creates a BufferedImage which represents the same Image as the
     * parameter but consumes less memory.
     */
    private static final java.awt.Image toBufferedImage(Image img, boolean load) {
        // load the image
        if (load) {
            new javax.swing.ImageIcon(img);
        }
        
        java.awt.image.BufferedImage rep = createBufferedImage();
        java.awt.Graphics g = rep.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        img.flush();
        return rep;
    }

    /** Creates BufferedImage 16x16 and Transparency.BITMASK */
    private static final java.awt.image.BufferedImage createBufferedImage() {
        java.awt.image.ColorModel model = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
                                          getDefaultScreenDevice().getDefaultConfiguration().getColorModel(java.awt.Transparency.BITMASK);
        java.awt.image.BufferedImage buffImage = new java.awt.image.BufferedImage(model,
                model.createCompatibleWritableRaster(16, 16), model.isAlphaPremultiplied(), null);
        return buffImage;
    }
    
    /** Indicate whether the node may be renamed.
     * @return tests {@link DataObject#isRenameAllowed}
     */
    public boolean canRename() {
        if (!getDataObject().getPrimaryFile().hasExt(XML_EXT)) return super.canRename();
        return getDeclaredSetter() != null;
    }
    
    /** Indicate whether the node may be destroyed.
     * @return tests {@link DataObject#isDeleteAllowed}
     */
    public boolean canDestroy() {
        if (!getDataObject().getPrimaryFile().hasExt(XML_EXT)) return super.canDestroy();
        try {
            InstanceCookie ic = ic();
            if (ic == null) return true;
            Class clazz = ic.instanceClass();
            return (!SharedClassObject.class.isAssignableFrom(clazz));
        } catch (Exception ex) {
            return false;
        }
    }
    
    public boolean canCut() {
        if (!getDataObject().getPrimaryFile().hasExt(XML_EXT)) return super.canCut();
        try {
            InstanceCookie ic = ic();
            if (ic == null) return false;
            Class clazz = ic.instanceClass();
            return (!SharedClassObject.class.isAssignableFrom(clazz));
        } catch (Exception ex) {
            return false;
        }
    }
    
    public boolean canCopy() {
        if (!getDataObject().getPrimaryFile().hasExt(XML_EXT)) return super.canCopy();
        try {
            InstanceCookie ic = ic();
            if (ic == null) return false;
            Class clazz = ic.instanceClass();
//XXX            if (XMLSettingsSupport.BrokenSettings.class.isAssignableFrom(clazz))
//                return false;
            return (!SharedClassObject.class.isAssignableFrom(clazz));
        } catch (Exception ex) {
            return false;
        }
    }
    
    /** Gets the short description of this feature. */
    public String getShortDescription() {
        if (noBeanInfo) return super.getShortDescription();
        
        try {
            InstanceCookie ic = ic();
            if (ic == null) {
                // it must be unrecognized instance
                return getDataObject().getPrimaryFile().toString();
            }
            
            Class clazz = ic.instanceClass();
            java.beans.BeanDescriptor bd = Utilities.getBeanInfo(clazz).getBeanDescriptor();
            String desc = bd.getShortDescription();
            return (desc.equals(bd.getName()))? getDisplayName(): desc;
        } catch (Exception ex) {
            return super.getShortDescription();
        } catch (LinkageError ex) {
            // #30650 - catch also LinkageError.
            return super.getShortDescription();
        }
    }
    
    /* do not want CustomizeBean to be invoked on double-click */
    public SystemAction getDefaultAction() {
        return null;
    }
    
    //
    // inner classes - properties
    //
    
    /** A property that delegates every call to original property
     * but when modified, also starts a saving task.
     */
    private static final class P extends Node.Property {
        /** delegate */
        private Node.Property del;
        /** task to executed */
        private InstanceDataObject t;

        public P (Node.Property del, InstanceDataObject t) {
            super (del.getValueType ());
            this.del = del;
            this.t = t;
        }

        public void setName(java.lang.String str) {
            del.setName(str);
        }

        public void restoreDefaultValue() throws IllegalAccessException, InvocationTargetException {
            del.restoreDefaultValue();
        }

        public void setValue(java.lang.String str, java.lang.Object obj) {
            del.setValue(str, obj);
        }

        public boolean supportsDefaultValue() {
            return del.supportsDefaultValue();
        }

        public boolean canRead() {
            return del.canRead ();
        }

        public PropertyEditor getPropertyEditor() {
            return del.getPropertyEditor();
        }

        public boolean isHidden() {
            return del.isHidden();
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            return del.getValue ();
        }

        public void setExpert(boolean param) {
            del.setExpert(param);
        }

        /** Delegates the set value and also saves the bean.
         */
        public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            del.setValue (val);
            t.scheduleSave();
        }

        public void setShortDescription(java.lang.String str) {
            del.setShortDescription(str);
        }

        public boolean isExpert() {
            return del.isExpert();
        }

        public boolean canWrite() {
            return del.canWrite ();
        }

        public Class getValueType() {
            return del.getValueType();
        }

        public java.lang.String getDisplayName() {
            return del.getDisplayName();
        }

        public java.util.Enumeration attributeNames() {
            return del.attributeNames();
        }

        public java.lang.String getShortDescription() {
            return del.getShortDescription();
        }

        public java.lang.String getName() {
            return del.getName();
        }

        public void setHidden(boolean param) {
            del.setHidden(param);
        }

        public void setDisplayName(java.lang.String str) {
            del.setDisplayName(str);
        }

        public boolean isPreferred() {
            return del.isPreferred();
        }

        public java.lang.Object getValue(java.lang.String str) {
            return del.getValue(str);
        }

        public void setPreferred(boolean param) {
            del.setPreferred(param);
        }

    } // end of P

    /** A property that delegates every call to original property
     * but when modified, also starts a saving task.
     */
    private static final class I extends Node.IndexedProperty {
        /** delegate */
        private Node.IndexedProperty del;
        /** task to executed */
        private InstanceDataObject t;

        public I (Node.IndexedProperty del, InstanceDataObject t) {
            super (del.getValueType (), del.getElementType ());
            this.del = del;
            this.t = t;
        }

        public void setName(java.lang.String str) {
            del.setName(str);
        }

        public void restoreDefaultValue() throws IllegalAccessException, InvocationTargetException {
            del.restoreDefaultValue();
        }

        public void setValue(java.lang.String str, java.lang.Object obj) {
            del.setValue(str, obj);
        }

        public boolean supportsDefaultValue() {
            return del.supportsDefaultValue();
        }

        public boolean canRead() {
            return del.canRead ();
        }

        public PropertyEditor getPropertyEditor() {
            return del.getPropertyEditor();
        }

        public boolean isHidden() {
            return del.isHidden();
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            return del.getValue ();
        }

        public void setExpert(boolean param) {
            del.setExpert(param);
        }

        /** Delegates the set value and also saves the bean.
         */
        public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            del.setValue (val);
            t.scheduleSave();
        }

        public void setShortDescription(java.lang.String str) {
            del.setShortDescription(str);
        }

        public boolean isExpert() {
            return del.isExpert();
        }

        public boolean canWrite() {
            return del.canWrite ();
        }

        public Class getValueType() {
            return del.getValueType();
        }

        public java.lang.String getDisplayName() {
            return del.getDisplayName();
        }

        public java.util.Enumeration attributeNames() {
            return del.attributeNames();
        }

        public java.lang.String getShortDescription() {
            return del.getShortDescription();
        }

        public java.lang.String getName() {
            return del.getName();
        }

        public void setHidden(boolean param) {
            del.setHidden(param);
        }

        public void setDisplayName(java.lang.String str) {
            del.setDisplayName(str);
        }

        public boolean isPreferred() {
            return del.isPreferred();
        }

        public java.lang.Object getValue(java.lang.String str) {
            return del.getValue(str);
        }

        public void setPreferred(boolean param) {
            del.setPreferred(param);
        }

        public boolean canIndexedRead () {
            return del.canIndexedRead ();
        }

        public Class getElementType () {
            return del.getElementType ();
        }

        public Object getIndexedValue (int index) throws
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return del.getIndexedValue (index);
        }

        public boolean canIndexedWrite () {
            return del.canIndexedWrite ();
        }

        public void setIndexedValue (int indx, Object val) throws
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            del.setIndexedValue (indx, val);
            t.scheduleSave();
        }

        public PropertyEditor getIndexedPropertyEditor () {
            return del.getIndexedPropertyEditor ();
        }
    } // end of I
    
    /** Derived from BeanChildren and allow replace beancontext. */
    private final static class InstanceChildren extends Children.Keys implements PropertyChangeListener {
        java.lang.ref.WeakReference dobjListener;
        InstanceDataObject dobj;
        Object bean;
        ContextL contextL = null;
        
        public InstanceChildren(InstanceDataObject dobj) {
            this.dobj = dobj;
        }
        
        protected void addNotify () {
            super.addNotify();
            
            PropertyChangeListener p = WeakListener.propertyChange(this, dobj);
            dobjListener = new java.lang.ref.WeakReference(p);
            dobj.addPropertyChangeListener(p);
            // attaches a listener to the bean
            contextL = new ContextL (this);
            propertyChange(null);
        }
        
        protected void removeNotify () {
            if (contextL != null && bean != null)
                ((BeanContext) bean).removeBeanContextMembershipListener (contextL);
            contextL = null;
            
            PropertyChangeListener p = (PropertyChangeListener) dobjListener.get();
            if (p != null) {
                dobj.removePropertyChangeListener(p);
                dobjListener.clear();
            }

            setKeys (java.util.Collections.EMPTY_SET);
        }
        
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt != null && !evt.getPropertyName().equals(InstanceDataObject.PROP_COOKIE)) return;
            
            if (contextL != null && bean != null)
                ((BeanContext) bean).removeBeanContextMembershipListener (contextL);
            
            try {
                InstanceCookie ic = (InstanceCookie) dobj.getCookie(InstanceCookie.class);
                if (ic == null) {
                    bean = null;
                    return;
                }
                Class clazz = ic.instanceClass();
                if (BeanContext.class.isAssignableFrom(clazz)) {
                    bean = ic.instanceCreate();
                } else if (BeanContextProxy.class.isAssignableFrom(clazz)) {
                    bean = ((BeanContextProxy) dobj.instanceCreate()).getBeanContextProxy();
                } else {
                    bean = null;
                }
            } catch (Exception ex) {
                bean = null;
                ErrorManager.getDefault().notify(ex);
            }
            if (bean != null) {
                // attaches a listener to the bean
                ((BeanContext) bean).addBeanContextMembershipListener (contextL);
            }
            updateKeys();
        }
        
        private void updateKeys() {
            if (bean == null) {
                setKeys(java.util.Collections.EMPTY_SET);
            } else {
                setKeys(((BeanContext) bean).toArray());
            }
        }
        
        /** Create nodes for a given key.
         * @param key the key
         * @return child nodes for this key or null if there should be no
         *   nodes for this key
         */
        protected Node[] createNodes(Object key) {
            Object ctx = bean; 
            if (bean == null) return new Node[0];
            
            try {
                if (key instanceof java.beans.beancontext.BeanContextSupport) {
                    java.beans.beancontext.BeanContextSupport bcs = (java.beans.beancontext.BeanContextSupport)key;

                    if (((BeanContext) ctx).contains (bcs.getBeanContextPeer())) {
                        // sometimes a BeanContextSupport occures in the list of
                        // beans children even there is its peer. we think that
                        // it is desirable to hide the context if the peer is
                        // also present
                        return new Node[0];
                    }
                }

                return new Node[] { new BeanContextNode (key, dobj) };
            } catch (IntrospectionException ex) {
                // ignore the exception
                return new Node[0];
            }
        }
        
        /** Context listener.
        */
        private static final class ContextL implements BeanContextMembershipListener {
            /** weak reference to the BeanChildren object */
            private java.lang.ref.WeakReference ref;

            /** Constructor */
            ContextL (InstanceChildren bc) {
                ref = new java.lang.ref.WeakReference (bc);
            }

            /** Listener method that is called when a bean is added to
            * the bean context.
            * @param bcme event describing the action
            */
            public void childrenAdded (BeanContextMembershipEvent bcme) {
                InstanceChildren bc = (InstanceChildren)ref.get ();
                if (bc != null) {
                    bc.updateKeys();
                }
            }

            /** Listener method that is called when a bean is removed to
            * the bean context.
            * @param bcme event describing the action
            */
            public void childrenRemoved (BeanContextMembershipEvent bcme) {
                InstanceChildren bc = (InstanceChildren)ref.get ();
                if (bc != null) {
                    bc.updateKeys ();
                }
            }
        }
    }
    
    /** Creates BeanContextNode for each bean
    */
    private static class BeanFactoryImpl implements BeanChildren.Factory {
        InstanceDataObject task;
        public BeanFactoryImpl(InstanceDataObject task) {
            this.task = task;
        }
        
        /** @return bean node */
        public Node createNode (Object bean) throws IntrospectionException {
            return new BeanContextNode (bean, task);
        }
    }
    
    private static class BeanContextNode extends BeanNode {
        public BeanContextNode(Object bean, InstanceDataObject task) throws IntrospectionException {
            super(bean, getChildren(bean, task));
            changeSheet(getSheet(), task);
        }
        
        private void changeSheet(Sheet orig, InstanceDataObject task) {
            Sheet.Set props = orig.get (Sheet.PROPERTIES);
            if (props != null) {
                convertProps (props, props.getProperties(), task);
            }

            props = orig.get(Sheet.EXPERT);
            if (props != null) {
                convertProps (props, props.getProperties(), task);
            }
        }
        private static Children getChildren (Object bean, InstanceDataObject task) {
            if (bean instanceof BeanContext)
                return new BeanChildren ((BeanContext)bean, new BeanFactoryImpl(task));
            if (bean instanceof BeanContextProxy) {
                java.beans.beancontext.BeanContextChild bch = ((BeanContextProxy)bean).getBeanContextProxy();
                if (bch instanceof BeanContext)
                    return new BeanChildren ((BeanContext)bch, new BeanFactoryImpl(task));
            }
            return Children.LEAF;
        }
        
        // #7925
        public boolean canDestroy() {
            return false;
        }
        
    }
    
    /** Property change listener to update the properties of the node and
    * also the name of the node (sometimes)
    */
    private final class PropL extends Object implements PropertyChangeListener {
        private boolean doNotListen = false;
        PropL() {}
        
        public void propertyChange(PropertyChangeEvent e) {
            if (doNotListen) return;
            firePropertyChange (e.getPropertyName (), e.getOldValue (), e.getNewValue ());
        }
        
        public void destroy() {
            doNotListen = true;
        }
    }
        
}
