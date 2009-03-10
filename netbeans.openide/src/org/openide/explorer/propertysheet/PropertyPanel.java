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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openide.ErrorManager;
import org.openide.explorer.propertysheet.editors.EnhancedCustomPropertyEditor;
import org.openide.explorer.propertysheet.editors.EnhancedPropertyEditor;
import org.openide.nodes.Node;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.WeakListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.text.Document;


/** Visual Java Bean for editing of properties. It takes the model
 * and represents the property editor for it.
 *
 * @author Jaroslav Tulach, Petr Hamernik, Jan Jancura, David Strupl
 */
public class PropertyPanel extends JComponent implements javax.accessibility.Accessible {

    /**
     * Constant defining preferences in displaying of value.
     * Value should be displayed in read-only mode.
     */
    public static final int PREF_READ_ONLY = 0x0001;

    /**
     * Constant defining preferences in displaying of value.
     * Value should be displayed in custom editor.
     */
    public static final int PREF_CUSTOM_EDITOR = 0x0002;

    /**
     * Constant defining preferences in displaying of value.
     * Value should be displayed in editor only.
     */
    public static final int PREF_INPUT_STATE = 0x0004;

    /** Name of the 'preferences' property. */
    public static final String PROP_PREFERENCES = "preferences"; // NOI18N

    /** Name of the 'model' property. */
    public static final String PROP_MODEL = "model"; // NOI18N

    /** Name of the read-only property 'propertyEditor'. */
    public static final String PROP_PROPERTY_EDITOR = "propertyEditor"; // NOI18N
    
    /** Name of property 'state' that describes the state of the embeded PropertyEditor.
    * @since 2.20
    */
    public static final String PROP_STATE = PropertyEnv.PROP_STATE; 
    
    /** Name of  'canEditAsText' property. */
    private static final String PROP_CAN_EDIT_AS_TEXT = "canEditAsText"; // NOI18N
    
    /** Static instance of empty PropertyModel. */
    private static final PropertyModel EMPTY_MODEL = new EmptyModel();

    /** Holds value of property preferences. */
    private int preferences;

    /** Holds value of property model. */
    private PropertyModel model;

    /** Current property editor. */
    private PropertyEditor editor;
    
    /** Environment passed to the property editor. */
    private PropertyEnv env;
    
    /** Descriptor for the property. */
    private FeatureDescriptor descriptor = new FeatureDescriptor();
    
    /** Holds painting style. */
    private int paintingStyle;
    
    /** Foreground color of values. */
    private Color foregroundColor;
    /** Foreground color of disabled values. */
    private Color disabledColor;
    /** Is plastic property value. */
    private boolean plastic;
    
    /** */
    static ThreadLocal current = new ThreadLocal();
    
    /** Indicates whether the property is writable. */
    private boolean canWrite = true;
    /** In the (rare) case when the property is not readable value
     * of this variable is set to false.
     */
    private boolean canRead = true;
    
    // private variables for visual controls ...........................................

    /** Component for showing property value is stored here. */
    private SheetButton readComponent;
    
    /** Component cache. */
    private SheetButton textView;
    
    /** Component cache. */
    private PropertyShow propertyShow;
    
    /** Component cache. */
    private SheetButton paintView;
    
    /** Component cache. */
    private SheetButton customizeButton;
    
    /** If this is true the read component is visible and we are not
     * performing any component switch. */
    private boolean isReadState;
    
    /** If this is true the write component is visible and we are not
     * performing any component switch. */
    private boolean isWriteState;
    
    /** Prevents firing back to ourselves. */
    private boolean ignoreEvents;
    
    /** Set to <code>true</code> when the custom dialog is showing. */
    private boolean customDialogShown;
    
    /** <code>True</code> if we represent more values in this panel. */
    private boolean differentValues;
    
    /** TextField used for editing the property value as text. */
    private JTextField textField;
    
    /** Combo used for editing the property value as tags. */
    private JComboBox comboBox;
    
    /** Listener capable of switching to the writing state. */
    private ReadComponentListener readComponentListener;
    
    /** Listener on textField and comboBox - it allows 
     * to switch back to the reading state.
     */
    private WriteComponentListener writeComponentListener;
    
    /** Listens on changes in the model. */
    private PropertyChangeListener modelListener;
    
    /** Listens on changes in the editor. */
    private PropertyChangeListener editorListener;
    
    /** Weak wrapper for the <code>editorListener</code>. */
    private PropertyChangeListener weakEditorListener;
    
    /** If this is not <code>null</code> the listener is added to
     * all newly created Sheetbuttons.
     */
    private SheetButtonListener sheetButtonListener;
    
    /**
     * A listener weakly attached to the env to capture
     * updates when the env changes. The reference is stored
     * here in order for the listener not to be garbage collected
     * before PropertyPanel.
     */
    private VetoableChangeListener envListener;
    
    /**
     * Maps ExPropertyEditor --> Reference<PropertyEnv>.
     * We remember instances of ExPropertyEditor to be
     * able to pass the same env to the same instance.
     * If we would not cache them we could end up in
     * situation where env from second PropertyPanel
     * hides the instance from a first panel.
     */
    private static final WeakHashMap envCache = new WeakHashMap();
    
    /**
     * If this is <code>true</code> the changes made in the property editor
     * are immediately propagated to the value of the property
     * (to the property model).
     */
    private boolean changeImmediate = true;
    
    // constructors -------------------------------------------------------
    
    /** Creates new PropertyPanel with the empty DefaultPropertyModel
     */
    public PropertyPanel () {
        this (EMPTY_MODEL, 0);
    }

    /** Creates new PropertyPanel with DefaultPropertyModel
     * @param bean The instance of bean
     * @param propertyName The name of the property to be displayed
     */
    public PropertyPanel (
        Object bean,
        String propertyName,
        int preferences
    ) {
        this (
            new DefaultPropertyModel (bean, propertyName),
            preferences
        );
    }

    /** Creates new PropertyPanel
     * @param model The model for displaying
     */
    public PropertyPanel (
        PropertyModel model,
        int preferences
    ) {
        this.model = model;
        this.preferences = preferences;
        setLayout (new BorderLayout ());
        
        boolean problem = false;
        try {
            Class c = Class.forName("org.openide.explorer.propertysheet.PropertyPanel$PropertySheetSettingsInvoker"); // NOI18N
            Runnable r = (Runnable)c.newInstance();
            current.set(this);
            r.run();
        } catch (Exception e) {
            problem = true;
        } catch (LinkageError e) {
            problem = true;
        }
        if (problem) {
            // set defaults without P ropertySheetSettings
            paintingStyle = PropertySheet.PAINTING_PREFERRED;
            plastic = false;
            disabledColor = UIManager.getColor("textInactiveText");
            foregroundColor = new Color (0, 0, 128);
        }
        
        model.addPropertyChangeListener (getModelListener());
        updateEditor ();
        reset();
    }

    /** Uses an instance of SimpleModel as model.*/
    PropertyPanel(Node.Property p, Object []beans) {
        this(new SimpleModel(p, beans), 0);
    }
    
    
    /** Reference to PropertySheetSettings are separated here.*/
    static class PropertySheetSettingsInvoker implements Runnable {
        public void run() {
            PropertyPanel instance = (PropertyPanel)current.get();
            current.set(null);
            if (instance == null) {
                throw new IllegalStateException();
            }
            PropertySheetSettings pss = PropertySheetSettings.getDefault();
            instance.paintingStyle = pss.getPropertyPaintingStyle();
            instance.plastic = pss.getPlastic();
            instance.disabledColor = pss.getDisabledPropertyColor();
            instance.foregroundColor = pss.getValueColor();
        }
    }

    // public methods -------------------------------------------------------
    
    /** Getter for property preferences.
     * @return Value of property preferences.
     */
    public int getPreferences () {
        return preferences;
    }

    /** Setter for visual preferences in displaying
     * of the value of the property.
     * @param preferences PREF_XXXX constants
     */
    public void setPreferences (int preferences) {
        int oldPreferences = this.preferences;
        this.preferences = preferences;
        readComponent = null;
        reset();
        firePropertyChange(
            PROP_PREFERENCES,
            new Integer (oldPreferences),
            new Integer (preferences)
        );
    }

    /** Getter for property model.
     * @return Value of property model.
     */
    public PropertyModel getModel() {
        return model;
    }

    /** Setter for property model.
     *@param model New value of property model.
     */
    public void setModel(PropertyModel model) {
        PropertyModel oldModel = this.model;
        this.model = model;
        oldModel.removePropertyChangeListener(getModelListener());
        model.addPropertyChangeListener(getModelListener());
        updateEditor();
        reset();
        firePropertyChange (PROP_MODEL, oldModel, model);
    }
    
    /** Getter for the state of the property editor. The editor can be in
     * not valid states just if it implements the <link>ExPropertyEditor</link>
     * and changes state by the <code>setState</code> method of the <link>PropertyEnv</link>
     * environment.
     * <P>
     * @return <code>PropertyEnv.STATE_VALID</code> if the editor is not the <code>ExPropertyEditor</code>
     *    one or other constant from <code>PropertyEnv.STATE_*</code> that was assigned to <code>PropertyEnv</code>
     * @since 2.20
     */
    public final Object getState () {
        PropertyEnv e = env;
        return e == null ? PropertyEnv.STATE_VALID : e.getState();
    }

    /** If the editor is <link>ExPropertyEditor</link> it tries to change the
     * <code>getState</code> property to <code>PropertyEnv.STATE_VALID</code>
     * state. This may be vetoed, in such case a warning is presented to the user
     * and the <code>getState</code> will still return the original value 
     * (different from STATE_VALID).
     * <P>
     * Also updates the value if 
     * <code>org.openide.explorer.propertysheet.editors.EnhancedCustomPropertyEditor</code>
     * is used. 
     */
    public void updateValue() {
        if (editor == null) return;
        
        PropertyEnv e = env;
        if (e != null && e.getState () == PropertyEnv.STATE_NEEDS_VALIDATION) {
            e.setState (PropertyEnv.STATE_VALID);
        }
        
        if (editor.supportsCustomEditor()) {
            Component customEditor = editor.getCustomEditor();
            if (customEditor instanceof EnhancedCustomPropertyEditor) {
                try {
                    Object value = ((EnhancedCustomPropertyEditor)customEditor).getPropertyValue();
                    editor.setValue(value);
                } catch (IllegalStateException ise) {
                    PropertyDialogManager.notify(ise);
                }
            }
        }
    }

    /**
     * Getter for current property editor depending on the model.
     * It could be <CODE>null</CODE> if there is not possible 
     * to obtain property editor for the current model.
     *
     * @return the property editor or <CODE>null</CODE>
     */
    public PropertyEditor getPropertyEditor() {
        return editor;
    }

    // bugfix# 10171 added setEnabled() and setComponentEnabled() methods
    
    /** Sets whether or not this component is enabled. 
     *
     * all panel components gets disabled when enabled parameter is set false
     * @param enabled flag defining the action. 
     */    
    public void setEnabled(boolean enabled) {                        
        Component[] comp = getComponents();
        if(comp!=null){            
            for(int i=0; i<comp.length; i++){                                
                setComponentEnabled(comp[i], enabled);                
            }
        }         
        super.setEnabled(enabled);         
    }
    
    /** Getter for property changeImmediate.
     * IF this is true the changes made in the property editor
     * are immediately propagated to the value of the property
     * (to the property model).
     *
     * @return Value of property changeImmediate.
     */
    public boolean isChangeImmediate() {
        return changeImmediate;
    }
    
    /** Setter for property changeImmediate.
     * IF this is true the changes made in the property editor
     * are immediately propagated to the value of the property
     * (to the property model).
     * @param changeImmediate New value of property changeImmediate.
     */
    public void setChangeImmediate(boolean changeImmediate) {
        if (this.changeImmediate == changeImmediate) {
            return;
        }
        this.changeImmediate = changeImmediate;
        if (env != null) {
            env.setChangeImmediate(changeImmediate);
        }
        firePropertyChange(PropertyEnv.PROP_CHANGE_IMMEDIATE,
            changeImmediate?Boolean.FALSE:Boolean.TRUE,
            changeImmediate?Boolean.TRUE:Boolean.FALSE);
    }
    
    
    // package private methods ----------------------------------------------
    
    /** Return a shett button serving as readComponent if it has been initailize.*/
    SheetButton getReadComponent() {
        return readComponent;
    }
    
    /**
     * Switches from reading component to writing one.
     */
    void setWriteState () {
        if (isWriteState) {
            return;
        }
        
        if ((preferences & PREF_READ_ONLY) != 0) {
            return;
        }
        if ((preferences & PREF_CUSTOM_EDITOR) != 0) {
            return;
        }
        isReadState = false;
        isWriteState = false;
        
        removeAll ();
        
        JComponent c = getWriterComponent ();
        c.setToolTipText(getPanelToolTipText());
        add (c, BorderLayout.CENTER);

        updateNeighbourPanels();
        
        revalidate();
        repaint ();

        Component focused = getDefaultFocusComponent(c);

        if(focused != null) {
            WriteComponentListener l = getWriteComponentListener();
            focused.requestFocus();
            focused.removeFocusListener(l);
            focused.removeKeyListener(l);
            focused.addFocusListener(l);
            focused.addKeyListener(l);
        }
        
        isReadState = false;
        isWriteState = true;
    }

    /** Gets default focus component from the JComponent container hierarchy,
     * i.e. component which calling on container requestDefaultComponent
     * should get the focus - it differs from SwingUtilities.findFocusOwner.
     * @return <code>Component</code> which should get the focus as default
     * or <code>null</code> if there is no such one. */
    private static Component getDefaultFocusComponent(JComponent container) {
        Component[] ca = container.getComponents();
        
        for(int i = 0; i < ca.length; i++) {
            if(ca[i].isFocusTraversable()) {
                return ca[i];
            }
            
            if(ca[i] instanceof JComponent && !((JComponent)ca[i]).isManagingFocus()) {
                Component res = getDefaultFocusComponent((JComponent)ca[i]);
                
                if(res != null) {
                    return res;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Set whether buttons in sheet should be plastic.
     * @param plastic true if so
     */
    void setPlastic (boolean plastic) {
        this.plastic = plastic;
        reset();
    }

    /**
     * Test whether buttons in sheet are plastic.
     * @return <code>true</code> if so
     */
    boolean getPlastic () {
        return plastic;
    }

    /**
     * Set the foreground color of values.
     * @param color the new color
     */
    void setForegroundColor (Color color) {
        this.foregroundColor = color;
        reset();
    }

    /**
     * Set the foreground color of disabled properties.
     * @param color the new color
     */
    void setDisabledColor (Color color) {
        disabledColor = color;
        reset();
    }

    /** Sets painting style. */
    void setPaintingStyle(int style) {
        paintingStyle = style;
        reset();
    }
    
    /** Adds sheet button listener to the <code>readComponent</code>. */
    void addSheetButtonListener(SheetButtonListener list) {
        this.sheetButtonListener = list;
        if (readComponent != null) {
            readComponent.addSheetButtonListener(list);
        }
    }

    /** Getter for <code>isWriteState</code> property. */
    boolean isWriteState() {
        return isWriteState;
    }
    
    // private methods -------------------------------------------------------
    
    /** Updates all neighbour panels. */
    private void updateNeighbourPanels() {
        Component c = getParent();
        while ((c != null) && (!(c instanceof NamesPanel))) {
            c = c.getParent();
        }
        if (c instanceof NamesPanel) {
            NamesPanel np = (NamesPanel)c;
            np.reset();
        }
    }
    
    /**
     * Update the current property editor depending on the model.
     */
    private void updateEditor() {
        if (model == EMPTY_MODEL) {
            return;
        }
        
        PropertyEditor oldEditor = editor;
        if (editor != null) {
            editor.removePropertyChangeListener(getEditorListener());
        }

        // find new editor
        editor = null;
        if (env != null) {
            env.removePropertyChangeListener(getEditorListener ());
        }
        
        env = null;
        
        if (model instanceof ExPropertyModel) {
            descriptor = ((ExPropertyModel)model).getFeatureDescriptor();

            if (descriptor instanceof Node.Property) {
                canWrite = ((Node.Property)descriptor).canWrite();
                canRead = ((Node.Property)descriptor).canRead();
                editor = ((Node.Property)descriptor).getPropertyEditor();
                
                if ((editor == null) && (descriptor instanceof Node.IndexedProperty)) {
                    editor = new IndexedPropertyEditor();
                    // indexed property editor does not want to fire immediately
                    descriptor.setValue(PropertyEnv.PROP_CHANGE_IMMEDIATE, Boolean.FALSE);
                } 
            }
        }
        // --------------------------------
        
        if (editor == null) {
            Class editorClass = model.getPropertyEditorClass();
            if (editorClass != null) {
                try {
                    java.lang.reflect.Constructor c = editorClass.getConstructor(new Class[0]);
                    c.setAccessible (true);
                    editor = (PropertyEditor) c.newInstance(new Object[0]);
                } catch (Exception e) {
                    PropertyDialogManager.notify(e);
                }
            }
        }
        
        if (editor == null) {
            Class propertyTypeClass = model.getPropertyType();
            if (propertyTypeClass != null) {
                editor = PropertyEditorManager.findEditor(propertyTypeClass);
            }
        }
        
        if (editor != null) {
            if (editor instanceof ExPropertyEditor) {
                Reference ref = (Reference)envCache.get(editor);
                if (ref != null) env = (PropertyEnv)ref.get();
                if (env == null) {
                    env = new PropertyEnv();
                    envCache.put(editor, new WeakReference(env));
                }
                setChangeImmediate(true);
                if (model instanceof ExPropertyModel) {
                    ExPropertyModel epm = (ExPropertyModel) model;
                    env.setFeatureDescriptor(epm.getFeatureDescriptor());
                    env.setBeans(epm.getBeans());
                }
                envListener = new VetoableChangeListener() {
                    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
                        refresh();
                    }
                };
                env.addVetoableChangeListener(
                    WeakListener.vetoableChange(envListener, env));
                env.addPropertyChangeListener (getEditorListener ());
                ((ExPropertyEditor)editor).attachEnv(env);
            }
            try {
                if (canRead) {
                    editor.setValue(model.getValue());
                }
            } catch (ProxyNode.DifferentValuesException dve) {
                differentValues = true;
            } catch (Exception e) {
                processThrowable(e);
            }
            if (canWrite) {
                editor.addPropertyChangeListener(getEditorListener());
            }
        }
        
        // fire the change
        firePropertyChange(PROP_PROPERTY_EDITOR, oldEditor, editor);
    }

    /** Set the panel to the read state with fresh
     * read component.
     */
    private void reset() {
        isWriteState = false;
        isReadState = false;
        
        if ((preferences & PREF_INPUT_STATE) != 0) {
            setWriteState();
            return;
        }
        if (((preferences & PREF_CUSTOM_EDITOR) != 0) && (editor != null)){
            Component c = editor.getCustomEditor();
            if ((c != null) && (c.getParent() != this)) {
                removeAll();
                add (c, BorderLayout.CENTER);
                validate();
                return;
            }
        }
        setReadState();
    }

    /** Refreshes the view after the property has changed.
     */
    void refresh() {
        if (customDialogShown) {
            // if custom dialog is currently shown the
            // refresh will happen immediately after it is closed
            return;
        }
        
        if (isReadState) {
            // [PENDING] anything more reasonable than reset here?
        }
        
        if(isWriteState && ((preferences & PREF_INPUT_STATE) == 0) ) {
            // When leaving 'write state' setReadState will be called directly,
            // see listeners at the bottom.
            return;
        }
        
        reset();
    }
    
    /**
     * Switches from writing component to reading one.
     */
    void setReadState () {
        if (isReadState) {
            return;
        }
        if ((preferences & PREF_INPUT_STATE) != 0) {
            return;
        }
        if ((preferences & PREF_CUSTOM_EDITOR) != 0) {
            return;
        }
        isWriteState = false;
        isReadState = false;
        
        removeAll ();
        
        readComponent = getReaderComponent();

        readComponent.addSheetButtonListener(getReadComponentListener());
        
        // bugfix# 10171 setComponentEnabled() call before add()... 
        setComponentEnabled(readComponent, isEnabled());
        
        readComponent.setToolTipText(getPanelToolTipText());
        updateSheetButtonVisually();
        
        add (readComponent, BorderLayout.CENTER);
        
        
        revalidate();
        repaint ();

        // Bug fix #13933 needles requesting of focus for cases when only refresh
        // needed. In case of problems some better solution is needed. 
        // Sure is only the thing, focus may not be requested when coming from refresh().
        //requestFocus();
        isReadState = true;
        isWriteState = false;
    }

    /** Lazy init of the <code>readComponentListener</code>. */
    private ReadComponentListener getReadComponentListener() {
        if (readComponentListener == null) {
            readComponentListener = new ReadComponentListener();
        }
        return readComponentListener;
    }
    
    /** Lazy init of the <code>writeComponentListener</code>. */
    private WriteComponentListener getWriteComponentListener() {
        if (writeComponentListener == null) {
            writeComponentListener = new WriteComponentListener();
        }
        return writeComponentListener;
    }
    
    /** Lazy init of the <code>modelListener</code>. */
    private PropertyChangeListener getModelListener() {
        if (modelListener == null) {
            modelListener = new ModelListener();
        }
        return modelListener;
    }
    
    /** Lazy init of the <code>weakEditorListener</code>. */
    private PropertyChangeListener getEditorListener() {
        if (editorListener == null) {
            editorListener = new EditorListener();
            weakEditorListener = WeakListener.propertyChange(
                editorListener,
                editor
            );
        }
        return weakEditorListener;
    }
    
    public void setBackground(Color bg) {
        super.setBackground(bg);
        updateSheetButtonVisually();
    }
    
    // reader component ........................

    /**
     * Creates Reader component.
     */
    private SheetButton getReaderComponent() {
        String stringValue = null;
        SheetButton c = null;

        if (editor == null) {
            //return getTextView(getTypeString(model.getPropertyType()));
            // display description in place of class name
            return getTextView (NbBundle.getMessage (PropertyPanel.class, "CTL_No_property_editor")); //NOI18N
        }
        
        try {
            if ((!differentValues) && (canRead)){
                stringValue = editor.getAsText ();
            }
          
            if ( editor.isPaintable () &&
                ( (paintingStyle == PropertySheet.PAINTING_PREFERRED) ||
                ( (paintingStyle == PropertySheet.STRING_PREFERRED) &&
                    (stringValue == null)
                  )
                )
               ) {
                   if (differentValues)
                       c = getTextView (NbBundle.getMessage (PropertyPanel.class, "CTL_Different_Values")); //NOI18N
                   else
                       c = getPaintView ();
            } else {
                if (stringValue == null) {
                    // display info the values are different in place of class name
                    c = (differentValues)?
                        getTextView (NbBundle.getMessage (PropertyPanel.class, "CTL_Different_Values")): //NOI18N
                        getTextView(getTypeString(model.getPropertyType()));
                } else {
                    c = getTextView(stringValue);
                }
            }
        } catch (Exception e) {
            //exception while getAsText () | isPaintable ()
            ErrorManager.getDefault().annotate( e,
                    getString("PS_ExcIn") +
                    " " + editor.getClass ().getName () + // NOI18N
                    " " + getString ("PS_Editor") + "." // NOI18N
            );
            c = getTextView(getExceptionString(e));
        }
        // we should never return null (dstrupl)
        if (c == null) {
            c = getTextView("null"); // NOI18N
        }
        return c;
    }

    /**
     * Creates SheetButton with text representing current value of property.
     */
    private SheetButton getTextView (String str) {
        textView = new PropertySheetButton(str, plastic, plastic);
        textView.setFocusTraversable(true);

        if (sheetButtonListener != null) {
            textView.addSheetButtonListener(sheetButtonListener);
        }
        
        if ((env != null) && 
            (env.getState() != PropertyEnv.STATE_NEEDS_VALIDATION) &&
            (!customDialogShown)) {
            textView.setInvalidMark(env.getState() == PropertyEnv.STATE_INVALID);
            if (descriptor instanceof Node.Property) {
                Node.Property np = (Node.Property)descriptor;
                if (np.supportsDefaultValue()) {
                    textView.setModifiedMark(! np.isDefaultValue());
                }
            }
        }
        
        // XXX Read-only should be handled via enabling/disabling component
        // not via settin 'active' foreground color.
        if(canWrite) {
            textView.setActiveForeground(foregroundColor);
        } else {
            textView.setActiveForeground(disabledColor);
        }
        
        return textView;
    }

    /**
     * Creates SheetButton with PropertyShow representing current value of property.
     */
    private SheetButton getPaintView () {
        if (propertyShow == null) {
            propertyShow = new PropertyShow (editor);
        } else {
            propertyShow.setEditor(editor);
        }
        
        // bugfix #26886, don't create new button for each call this method
        if (paintView == null) {
            paintView = new PropertySheetButton ();
            paintView.add (propertyShow);
        }

        if (sheetButtonListener != null) {
            paintView.addSheetButtonListener (sheetButtonListener);
        }

        // bugfix #26340, set label for paint view
        paintView.setLabel (editor.getAsText ());
        paintView.setFocusTraversable(true);

        // XXX Read-only should be handled via enabling/disabling component
        // not via settin 'active' foreground color.
        if(canWrite) {
            paintView.setActiveForeground(foregroundColor);
            propertyShow.setForeground(foregroundColor);
        } else {
            paintView.setActiveForeground(disabledColor);
            propertyShow.setForeground(disabledColor);
        }
        
        if ((env != null) && 
            (env.getState() != PropertyEnv.STATE_NEEDS_VALIDATION) &&
            (!customDialogShown)) {
            paintView.setInvalidMark(env.getState() == PropertyEnv.STATE_INVALID);
            if (descriptor instanceof Node.Property) {
                Node.Property np = (Node.Property)descriptor;
                if (np.supportsDefaultValue()) {
                    paintView.setModifiedMark(! np.isDefaultValue());
                }
            }

        }
        paintView.setPlastic(plastic);
        
        paintView.setToolTipText(getPanelToolTipText());
        
        return paintView;
    }

    // writer component ........................

    /**
     * This method returns property value editor Component like input line (if property supports
     * setAsText (String string) method) or some others.
     *
     * @return property value editor Component
     */
    private JComponent getWriterComponent () {
        if (editor == null) return getDisabledWriterComponent();

        String stringValue = null;
        boolean canEditAsText = true;
        Object customEditAsText = descriptor.getValue(PROP_CAN_EDIT_AS_TEXT);
        if (customEditAsText instanceof Boolean) {
            canEditAsText = ((Boolean)customEditAsText).booleanValue();
        }

        try {
            if (canRead) {
                if (!differentValues) {
                    stringValue = editor.getAsText ();
                // bugfix #22357 editing value in-place is enabled if there are different values
                } else {
                    stringValue = NbBundle.getMessage (PropertyPanel.class, "CTL_Different_Values"); //NOI18N
                }
            }
        } catch (Exception x) {
            processThrowable(x);
        }
        
        if ((stringValue == null) && (customEditAsText == null)){
            canEditAsText = false;
        }

        getWriteComponentListener().setOldValue(editor.getValue()); 
        
        boolean existsCustomEditor = editor.supportsCustomEditor ();
        if ((editor instanceof EnhancedPropertyEditor) && 
            (((EnhancedPropertyEditor)editor).hasInPlaceCustomEditor())) {
            return getInput (getInPlace (), existsCustomEditor);
        }

        if (descriptor instanceof Node.Property) {
            Node.Property np = (Node.Property)descriptor;
            if (!np.canWrite ()) {
                if (existsCustomEditor) {
                    return getInput (getDisabledWriterComponent(), true);// read-only
                } else {
                    return getDisabledWriterComponent();
                }
            }
        }
        
        boolean editable = (editor instanceof EnhancedPropertyEditor) &&
            (((EnhancedPropertyEditor)editor).supportsEditingTaggedValues ());

        String[] tags; // Tags
        if(((tags = editor.getTags()) != null)) {
            return getInput(
                getInputTag(
                    tags,
                    stringValue,
                    editable),
                existsCustomEditor
            );
        }

        if (canEditAsText) {
            return getInput(
                getInputLine((stringValue == null) ? "???" : stringValue, true), // NOI18N
                    existsCustomEditor);
        }
        
        if (existsCustomEditor) {
            return getInput (getDisabledWriterComponent(), true);
        }
        
        return getDisabledWriterComponent();
    }

    /** */
    private JComponent getDisabledWriterComponent() {
       SheetButton c = getReaderComponent();
       if (descriptor instanceof Node.Property) {
            if (!((Node.Property)descriptor).canWrite()) {
                c.setActiveForeground(disabledColor);
            }
        }

       c.addFocusListener(getWriteComponentListener());

       return c;
    }

    /**
     * This is helper method for method getWriterComponent () which returns Panel with Choice
     * in the "Center" and enhanced property editor open button on the "East". This Panel
     * is then returned as property value editor Component.
     *
     * @param tags There are lines for Choice stored.
     * @param selected Line to be selected.
     *
     * @return Choice Component
     */
    private JComponent getInputTag (String[] tags, final String selected, boolean editable) {
        comboBox = new PropertyComboBox();
        
        comboBox.setModel(new DefaultComboBoxModel(tags));
        
        comboBox.setMaximumRowCount(tags.length <= 12 ? tags.length : 8);

        if(selected != null) {
            for(int i = 0; i < tags.length; i++) {
                if(tags [i].equals(selected)) {
                    comboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        
        if (editable) {
            comboBox.setEditable (true);
            comboBox.setSelectedItem (selected);
            comboBox.getEditor().setItem (selected);
            comboBox.getEditor().getEditorComponent().
                addFocusListener(getWriteComponentListener());
            comboBox.getEditor().getEditorComponent().
                addKeyListener(getWriteComponentListener());
        }
        
        comboBox.addActionListener (getWriteComponentListener());    
        comboBox.setToolTipText(getPanelToolTipText());
        
        return comboBox;
    }
    
    /** Attempts to advance to next item in comboBox. */
    void tryToSelectNextTag() {
        if(comboBox == null) {
            return;
        }

        setWriteState();
        
        int index = comboBox.getSelectedIndex();
        index++;
        
        if(index >= comboBox.getItemCount()) {
            index = 0;
        }

        comboBox.setSelectedIndex(index);
    }
    
    /** Gets 'in-place' custom editor component. */
    private Component getInPlace () {
        Component c = ((EnhancedPropertyEditor) editor).
                      getInPlaceCustomEditor ();
        c.addFocusListener (getWriteComponentListener());
        c.addKeyListener(getWriteComponentListener());
        
        if (c instanceof JComponent) {
            ((JComponent)c).setToolTipText(getPanelToolTipText());
        }
        
        return c;
    }

    /**
     * This is helper method for method getWriterComponent () which returns Panel with TextField
     * in the "Center" and enhanced property editor open button on the "East". This Panel
     * is then returned as property value editor Component.
     *
     * @param String propertyStringValue initial property value.
     * @param boolean editable is true if string editing should be allowed.
     * @param boolean existsCustomEditor is true if enhanced property editor open button
     *  should be showen.
     *
     * @return Panel Component
     */
    private JComponent getInputLine (final String propertyStringValue, boolean editable) {
        textField = new PropertyTextField();

        textField.addActionListener (getWriteComponentListener());
        textField.addKeyListener(getWriteComponentListener());
        textField.addFocusListener (getWriteComponentListener());

        textField.setText(propertyStringValue);
        textField.setEditable (editable);
        
        textField.setToolTipText(getPanelToolTipText());

        if (!isWriteState) {
            textField.selectAll();
        }

        return textField;
    }
    
    /** */
    private String getExceptionString (Throwable exception) {
        if (exception instanceof InvocationTargetException)
            exception = ((InvocationTargetException) exception).
                        getTargetException ();
        return "<" + exception.getClass().getName() + ">"; // NOI18N
    }

    /** */
    private String getTypeString (Class clazz) {
        if (clazz == null) {
            return getString("CTL_NoPropertyEditor");
        }
        if (clazz.isArray()) {
            return "[" + getString ("PS_ArrayOf") +" " + // NOI18N
                   getTypeString(clazz.getComponentType()) + "]"; // NOI18N
        }
        return "[" + clazz.getName() + "]"; // NOI18N
    }

    /**
     * This is helper method for method getInput () and getInputTag () which returns Panel
     * with enhanced property editor open button on the "East".
     *
     * @param Component leftComponent this component will be added to the "Center" of this panel
     * @param boolean existsCustomEditor is true if enhanced property editor open button
     *  should be shown.
     *
     * @return <code>JPanel</code> component
     */
    private JComponent getInput(Component leftComponent, boolean existsCustomEditor) {
        JPanel panel;
        if ( (leftComponent == null) &&
                (editor != null) && (editor.isPaintable ()) &&
                (paintingStyle != PropertySheet.ALWAYS_AS_STRING)
           ) {
            panel = new PropertyShow(editor);
        } else {
            panel = new JPanel ();
        }

        panel.setLayout (new BorderLayout());
        if (leftComponent != null) {
            panel.add (leftComponent, BorderLayout.CENTER);
        }

        if (existsCustomEditor) {
            panel.add(getCustomizeButton(), BorderLayout.EAST);
        }
        
        panel.setToolTipText(getPanelToolTipText());
        
        panel.addFocusListener(getWriteComponentListener());
        panel.addKeyListener(getWriteComponentListener());
        return panel;
    }
    
    /** Gets <code>customizedButton</code>, so called 'three-dot-button'. */
    private SheetButton getCustomizeButton() {
        if (customizeButton == null) {
            customizeButton = new SheetButton("...", plastic, plastic); // NOI18N
            customizeButton.setFocusTraversable(true);
            
            Font currentFont = customizeButton.getFont ();
            customizeButton.setFont (
                new Font (
                    currentFont.getName (),
                    currentFont.getStyle () | Font.BOLD,
                    currentFont.getSize ()
                )
            );
            customizeButton.addFocusListener(getWriteComponentListener());
            customizeButton.addSheetButtonListener(new CustomizeListener());
            customizeButton.setToolTipText(getString("CTL_ElipsisHint"));
        }
        
        // XXX Read-only should be handled via enabling/disabling component
        // not via settin 'active' foreground color.
        if(canWrite) {
            customizeButton.setActiveForeground(foregroundColor);
        } else {
            customizeButton.setActiveForeground(disabledColor);
        }
        
        return customizeButton;
    }

    /** Processes <code>Exception</code> thrown from 
     * <code>setAsText</code> or <code>setValue</code> call 
     * on <code>editor</code>. Helper method. */
    private void notifyExceptionWhileSettingProperty(Exception iae) {
        // partly bugfix #10791, notify exception to an user if PREF_INPUT_STATE is set
        if (getPreferences () == 0 || getPreferences () == PREF_INPUT_STATE){
            PropertyPanel.notifyUser (iae, descriptor.getDisplayName ());
        } else {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, iae);
        }
    }

    /** 
     * Tries to find a localized message in the exception annotation
     * or directly in the exception. If the message is found it is
     * notified with user severity. If the message is not found the
     * exception is notified with informational severity.
     * @param e exception to notify
     * @param propertyName display name of property
     */
    static void notifyUser(Exception e, String propertyName) {
        
        String userMessage = extractLocalizedMessage (e);
        
        if ((userMessage == null) && (e instanceof InvocationTargetException)) {
            userMessage = extractLocalizedMessage(
                ((InvocationTargetException)e).getTargetException());
        }
        ErrorManager em = ErrorManager.getDefault ();
        if ((userMessage != null)){
            em.annotate (e, NbBundle.getMessage(PropertyPanel.class,
		    "FMT_ErrorSettingProperty", userMessage,
		    propertyName
            ));
            em.notify (ErrorManager.USER, e);
        } else {
            em.notify (ErrorManager.INFORMATIONAL, e);
        }
    }

    /**
     * Scans the annotations list for a localized message. If there
     * is none tries to get the localized message from the Throwable itself.
     */
    private static String extractLocalizedMessage(Throwable t) {
        ErrorManager em = ErrorManager.getDefault();
        
        ErrorManager.Annotation[] an = em.findAnnotations(t);
        String message = null;
        if (an != null) {
            for (int i = 0; i < an.length; i++) {
                String msg = an[i].getLocalizedMessage();
                if (msg != null) {
                    message = msg;
                    break;
                }
            }
        }
        if (message == null) {
            message = t.getLocalizedMessage();
        }
        return message;
    }
    
    /** Processes <code>Throwable</code> thrown from <code>setAsText</code>
     * or <code>setValue</code> call on <code>editor</code>. Helper method. */
    private void processThrowable(Throwable throwable) {
        if(throwable instanceof ThreadDeath) {
            throw (ThreadDeath)throwable;
        }
        
        ErrorManager em = ErrorManager.getDefault();
        em.annotate(throwable, NbBundle.getMessage(PropertyPanel.class,
                "FMT_ErrorSettingProperty", throwable.getLocalizedMessage(),
                descriptor.getDisplayName()
        ));
                
        em.notify(throwable);
    }
    

    /** Sets whether or not all components within selected component 
     * will be enabled.
     *
     * @param cmp component to be enabled/disabled
     * @param enabled flag defining the action. 
     */
    private void setComponentEnabled(Component cmp, boolean enabled){
        cmp.setEnabled(enabled);
        if (Container.class.isAssignableFrom(cmp.getClass()) ){            
            Container cont = (Container)cmp;
            Component[] comp = cont.getComponents(); 
            for(int i=0; i<comp.length; i++){                
                comp[i].setEnabled(enabled);                
                setComponentEnabled(comp[i], enabled);
            }
        }           
    }    

    /** Gets tooltip for this <code>PropertyPanel</code>.
     * @return tooltip retrieved from getToolTipText method
     * or property class type name if the former is <code>null</code>
     * or <code>null</code> if the model returns null as its class type */
    private String getPanelToolTipText() {
        String toolTip = getToolTipText();
        
        if(toolTip != null) {
            return toolTip;
        }
        
        if (editor == null)
            return null;
        
        // bugfix #24021, replace class name on value with property value
        if (differentValues)
            return NbBundle.getMessage (PropertyPanel.class,
                                        "CTL_Desc_Different_Values"); //NOI18N
        if (canRead)
            return editor.getAsText ();
        else
            return null;
    }
    
    /** Paint sheet button with PropertyPanel backround and without border when
     * used as cell renderer.
     */
    private void updateSheetButtonVisually() {
        boolean flat = false;
        Object f = getClientProperty("flat"); // NOI18N
        if (f instanceof Boolean)
            flat = ((Boolean)f).booleanValue();

        if (readComponent != null) {
            readComponent.setFlat(flat);
            readComponent.setBackground(getBackground());
        }
        if (propertyShow != null)
            propertyShow.setBackground(getBackground());
        //if (customizeButton != null)
        //    customizeButton.setFlat(flat);
    }
    
    /** Gets localized string from specified key. */
    private static String getString(String key) {
        return NbBundle.getMessage(PropertyPanel.class, key);
    }

    
    // innerclasses ..............................................................

    
    /** Empty implementation of the <code>PropertyModel</code> interface. */
    private static class EmptyModel implements PropertyModel {
        EmptyModel() {}
        /** @return <code>null</code> */
        public Object getValue() throws InvocationTargetException {
            return null;
        }

        /** Dummy implementation. Does nothing. */
        public void setValue(Object v) throws InvocationTargetException {
        }

        /** @return <code>Object.class</code> */
        public Class getPropertyType() {
            return Object.class;
        }

        /** @return <code>null</code> */
        public Class getPropertyEditorClass() {
            return null;
        }

        /** Dummy implementation. Does nothing. */
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        /** Dummy implementation. Does nothing. */
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }
    } // End of class EmptyModel.
    
    
    /** Implementation of the <code>PropertyModel</code> interface keeping 
     * a <code>Node.Property</code>. */
    static class SimpleModel implements ExPropertyModel {
        /** Property to work with. */
        private Node.Property prop;
        /** Array of beans(nodes) to which belong the property. */
        private Object []beans;
        /** Property change support. */
        private PropertyChangeSupport sup = new PropertyChangeSupport(this);

        /** Construct simple model instance.
         * @param property proeprty to work with
         * @param beans array of beans(nodes) to which belong the property */
        public SimpleModel(Node.Property property, Object[] beans) {
            this.prop = property;
            this.beans = beans;
        }
        
        
        /** Implements <code>PropertyModel</code> interface. */
        public Object getValue() throws InvocationTargetException {
            try {
                return prop.getValue();
            } catch(IllegalAccessException iae) {
                throw annotateException(iae);
            } catch(InvocationTargetException ite) {
                throw annotateException(ite);
            }
        }
        
        /** Implements <code>PropertyModel</code> interface. */
        public void setValue(Object v) throws InvocationTargetException {
            try {
                prop.setValue(v);
                sup.firePropertyChange(PropertyModel.PROP_VALUE, null, null);
            } catch(IllegalAccessException iae) {
                throw annotateException(iae);
            } catch(IllegalArgumentException iaae) {
                throw annotateException(iaae);
            } catch(InvocationTargetException ite) {
                throw annotateException(ite);
            }
        }

        /** Annotates specified exception. Helper method.
         * @param exception original exception to annotate
         * @return <code>IvocationTargetException</code> which annotates the
         *       original exception */
        private InvocationTargetException annotateException(Exception exception) {
            if(exception instanceof InvocationTargetException) {
                return (InvocationTargetException)exception;
            } else {
                return new InvocationTargetException(exception);
            }
        }

        /** Implements <code>PropertyModel</code> interface. */
        public Class getPropertyType() {
            return prop.getValueType();
        }

        /** Implements <code>PropertyModel</code> interface. */
        public Class getPropertyEditorClass() {
            Object ed = prop.getPropertyEditor();
            if (ed != null) {
                return ed.getClass();
            }
            return null;
        }

        /** Implements <code>PropertyModel</code> interface. */
        public void addPropertyChangeListener(PropertyChangeListener l) {
            sup.addPropertyChangeListener(l);
        }

        /** Implements <code>PropertyModel</code> interface. */
        public void removePropertyChangeListener(PropertyChangeListener l) {
            sup.removePropertyChangeListener(l);
        }
        
        /** Implements <code>ExPropertyModel</code> interface. */
        public Object[] getBeans() {
            return beans;
        }
        
        /** Implements <code>ExPropertyModel</code> interface. */
        public FeatureDescriptor getFeatureDescriptor() {
            return prop;
        }
        
        void fireValueChanged() {
            sup.firePropertyChange(PropertyModel.PROP_VALUE, null, null);
        }

    } // End of class SimpleModel.

    
    /** Listener on textField(<code>JTextField</code>) or comboBox(<code>JComboBox</code>)
     * - input line components controlled by this panel.
     */
    private final class WriteComponentListener extends KeyAdapter
    implements ActionListener, FocusListener {
        WriteComponentListener() {}    
        /** Holds old value. */
        private Object oldValue;

        /** Task which handles action performed from combo box. */
        private Runnable comboActionTask;
        
        /** Task which requests default focus for enclosing class
         * when setting to 'readState'. */
        private Runnable requestFocusTask;

        
        /** Sets <code>oldValue</code>. */
        private void setOldValue(Object oldValue) {
            this.oldValue = oldValue;
        }
        
        /** Implements <code>ActionListener</code> interface. */
        public void actionPerformed (ActionEvent e) {
            if (!isWriteState) return;

            if(e.getSource() == comboBox) {
                // XXX #16101 - There is a problem with actions fired
                // from combo, we can't see the diff if it was caused by mouse
                // selection in popup or key navigation in popup, thus we plan 
                // to handle this event later so we will know to decide what 
                // was the cause of the action depening on it if popup is
                // visible or not (yes->mouse, no->key navigation).
                SwingUtilities.invokeLater(getComboActionTask());
            } else {
                if(e.getSource() == textField) {
                    String val = textField.getText();

                    changeValue(val);
                }

                prepareReadState();
            }
        }
        
        /** Overrides <code>KeyAdapter</code> superclass method. */
        public void keyPressed(KeyEvent e) {
            Object source = e.getSource();
            
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if(// XXX ESC during navigation in popup resets the old value.
                    comboBox != null 
                    && comboBox.isPopupVisible()
                    && source instanceof Component
                    && SwingUtilities.isDescendingFrom((Component)source, comboBox)) {

                    resetOldValue();
                } else if(source == textField) {
                    resetOldValue();
                    e.consume();
                }
                
                prepareReadState();
            } else if(e.getKeyCode() == KeyEvent.VK_ENTER
                && comboBox != null
                && source instanceof Component
                && SwingUtilities.isDescendingFrom((Component)source, comboBox) 
                && !comboBox.isEditable()
                && ((preferences & PREF_INPUT_STATE) == 0)) {
                    
                    changeValue((String) comboBox.getSelectedItem ());
                    prepareReadState();
            }
        }
        
        /** Implements <code>FocusListener</code> interface. */
        public void focusLost (final FocusEvent e) {
            if (!isWriteState) {
                return;
            }
            
            if ((comboBox != null) && (e.isTemporary())) {
                return;
            }
            
            // help flag, don't set a value if no change
            final boolean differentValuesNoChange = differentValues &&
                                                    (e.getSource().equals(textField)) &&
                                                    NbBundle.getMessage (PropertyPanel.class,
                                                      "CTL_Different_Values").equals( //NOI18N
                                                    textField.getText ());

            boolean supportsCustom = (editor != null && editor.supportsCustomEditor());

            if(supportsCustom || comboBox != null) {
                // XXX 
                // In this case we need to find out if the focus was
                // moved to component inside this PropetyPanel hierarchy, 
                // currently to "..." customize button.
                // To figure out the new focus owner we have to skip the current
                // task.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // bugfix #18326 set value to editor w/o "read state" is reset
                        if (e.getSource().equals(textField)) {
                            if (!differentValuesNoChange) {
                                changeValue(textField.getText());
                                if(editor!=null && !editor.getAsText().equals(textField.getText())) {
                                    // if a value is invalid then old value is set back
                                    textField.setText(editor.getAsText());
                                }
                            }
                        }
                        // Reset 'read state' only in case the focus was lost
                        // from this PropertyPanel hierarchy.
                        if(SwingUtilities.findFocusOwner(PropertyPanel.this) == null && isWriteState) {
                            if (!differentValuesNoChange) {
                                resetReadState(e);
                            } else {
                                setReadState ();
                            }
                        } 
                    }
                });
            } else {
                if (!differentValuesNoChange) {
                    resetReadState(e);
                } else {
                    setReadState ();
                }
            }
        }
        
        /** Implements <code>FocusListener</code> interface. */
        public void focusGained(FocusEvent e)  {
            if(e.getSource() == textField) {
                textField.selectAll();
            } else if(comboBox != null && comboBox.isEditable()) {
                comboBox.getEditor ().selectAll ();
            }
        }

        /** Changes value if needed.
         */
        public void changeValue(String s) {
            if(s != null 
                && (editor.getValue() == null // Fix #13339
                    || !(s.equals(editor.getAsText())) ) ) {
                if(!setAsText(s)) {
                    resetOldValue();
                }
            }
        }

        /** Gets <code>comboActionTask</code>. */
        private synchronized Runnable getComboActionTask() {
            if(comboActionTask == null) {
                comboActionTask = new Runnable() {
                    public void run() {
                        // XXX For combos allow key navigation in popup.
                        if(comboBox.isPopupVisible() || !isWriteState) {
                            return;
                        }
                        changeValue((String) comboBox.getSelectedItem ());
                        prepareReadState();
                    }
                };                
            }
            
            return comboActionTask;
        }

        /** Gets <code>requestFocusTask</code>. */
        private synchronized Runnable getRequestFocusTask() {
            if(requestFocusTask == null) {
                requestFocusTask = new Runnable() {
                    public void run() {
                        Component focused = getDefaultFocusComponent(PropertyPanel.this);

                        if(focused != null)
                            focused.requestFocus();
                    }
                };
            }
            
            return requestFocusTask;
        }
        
        /** Prepares 'readState'. Sets 'readState' and requests default focus
         * for changed component tree of enclosing <code>PropertyPanel</code>. */
        private void prepareReadState() {
            boolean hasFocus = SwingUtilities.findFocusOwner(PropertyPanel.this) != null;
            
            setReadState();
            if (hasFocus) {
                // XXX direct call doesn't work - #16052.
                SwingUtilities.invokeLater(getRequestFocusTask());
            }
        }

        /** Resets 'read state' on focus lost change. Helper method. */
        private void resetReadState(FocusEvent e) {
            // bugfix #18326 change value if "..." customize button is left
            if(e.getSource() == customizeButton && textField!=null) {
                changeValue(textField.getText ());
            } else if(e.getSource() == textField) {
                changeValue(textField.getText ());
            } else if((comboBox != null) && (comboBox.isEditable())) {
                changeValue((String) comboBox.getEditor().getItem());
            }
            setReadState();
        }
        
        /** Resets specified value to <code>editor</code>. */
        private void resetOldValue() {
            try {
                // don't set old value if there are different values
                if (!differentValues)
                    editor.setValue(oldValue);
            } catch(IllegalArgumentException iae) {
                notifyExceptionWhileSettingProperty(iae);
            } catch(RuntimeException throwable) {
                processThrowable(throwable);
            }
        }

        /** 
         * Sets as text.
         * @value <code>String</code> value which is possible to convert to 
         *      the type of property.
         * @returns <code>true</code> if succesfull, <code>false</code> otherwise
         */
        private boolean setAsText (String value) {
            try {
                editor.setAsText (value);
                return true;
            } catch (IllegalArgumentException iae) {
                notifyExceptionWhileSettingProperty(iae);
            } catch (RuntimeException throwable) {
                processThrowable(throwable);
            }

            return false;
        }        
    } // End of class WriteComponentListener.

    
    /** Listener on <code>readComponent</code>. */
    private final class ReadComponentListener implements SheetButtonListener {
	ReadComponentListener() {}
        /**
         * Invoked when the mouse exits a component.
         */
        public void sheetButtonExited(ActionEvent e) {
        }
        
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void sheetButtonClicked(ActionEvent e) {
            if(SheetButton.RIGHT_MOUSE_COMMAND.equals(e.getActionCommand()) || isWriteState) {
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setWriteState();
                    }
                });
        }
        
        /**
         * Invoked when the mouse enters a component.
         */
        public void sheetButtonEntered(ActionEvent e) {
        }
        
    } // End of class ReadComponentListener.


    /** Listener on <code>customizeButton</code>. */
    private final class CustomizeListener implements SheetButtonListener {
        CustomizeListener() {}
        /**
         * Invoked when the mouse exits a component. Dummy implementation.
         * Does nothing.
         */
        public void sheetButtonExited(ActionEvent e) {
        }
        
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void sheetButtonClicked(ActionEvent e) {
            String title = descriptor.getDisplayName();
            customDialogShown = true;
            // bugfix #18326 editor's value is taken from textField
            if (textField != null) {
                try {
                    if(editor.getValue() == null // Fix #13339
                        || !(textField.getText().equals(editor.getAsText())) ) {
                        editor.setAsText(textField.getText());
                    }
                } catch (ProxyNode.DifferentValuesException dve) {
                    // old value back will be set back
                } catch (Exception ite) {
                    // old value back will be set back
                }
            }

            //set the cursor *before* causing the property editor to
            //create the component
            final Container w = getTopLevelAncestor();
            w.setCursor (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            //Strange but true, this is the safest way to do this;
            //there is no guarantee that what is shown by Window.show() will
            //be the window instance you called show() on.
            AWTEventListener listener = new AWTEventListener () {
                public void eventDispatched (AWTEvent ae) {
                    Toolkit.getDefaultToolkit().removeAWTEventListener (this);
                    w.setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            };
            try {
            
                PropertyDialogManager pdm = new PropertyDialogManager(
                    NbBundle.getMessage(PropertyPanel.class, "PS_EditorTitle",
                            title == null ? "" : title, // NOI18N
                            model.getPropertyType()),
                true,
                editor,
                model,
                env);
            final Window wd = pdm.getDialog();
            
            //Component event seems to be the most reliable; sometimes window
            //events are being consumed by something or never fired
            Toolkit.getDefaultToolkit().addAWTEventListener(listener, 
                AWTEvent.COMPONENT_EVENT_MASK);
            wd.show();
            } finally {
                Toolkit.getDefaultToolkit().removeAWTEventListener (listener);
                w.setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            // update editor from the model
            if (canRead) {
                try {
                    ignoreEvents = true;
                    Object newValue = model.getValue();
                    Object oldValue = editor.getValue();
                    // test if newValue is not equal to oldValue
                    if ((newValue != null && !newValue.equals(oldValue)) || 
                        (newValue == null && oldValue != null)) {
                        editor.setValue(newValue);
                    }
                } catch (ProxyNode.DifferentValuesException dve) {
                    differentValues = true;
                } catch (Exception ite) {
                    processThrowable(ite);
                }
                finally {
                    ignoreEvents = false;
                }
            }
            reset();
            requestFocus();
            customDialogShown = false;
        }
        
        /**
         * Invoked when the mouse enters a component. Dummy implementation.
         * Does nothing.
         */
        public void sheetButtonEntered(ActionEvent e) {
        }
        
    } // End of class CustomizeListener.
    
    
    /** Property change listener for the model.
     */
    private class ModelListener implements PropertyChangeListener {
        ModelListener() {}
        /** Property was changed. */
        public void propertyChange(PropertyChangeEvent evt) {
            if (PropertyModel.PROP_VALUE.equals(evt.getPropertyName())) {
                if (editor != null) {
                    Mutex.EVENT.readAccess(new Runnable() {
                        public void run() {
                            try {
                                ignoreEvents = true;
                                differentValues = false;
                                Object newValue = model.getValue();
                                Object oldValue = editor.getValue();
                                // test if newValue is not equal to oldValue
                                if ((newValue != null && !newValue.equals(oldValue)) || 
                                    (newValue == null && oldValue != null)) {
                                    editor.setValue(newValue);
                                }
                                refresh();
                            } catch (ProxyNode.DifferentValuesException dve) {
                                differentValues = true;
                            } catch (InvocationTargetException e) {
                                notifyExceptionWhileSettingProperty(e);
                            }
                            finally {
                                ignoreEvents = false;
                            }
                        }
                    });
                }
            }
        }
    } // End of class ModelListener.

    /** Property change listener for the editor.
     */
    private class EditorListener implements PropertyChangeListener {
        EditorListener() {}
        /** Property was changed. */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource () instanceof PropertyEnv) {
                // we are listening also on PropertyEnv
                
                if (PropertyEnv.PROP_STATE.equals (evt.getPropertyName ())) {
                    PropertyPanel.this.firePropertyChange (PropertyEnv.PROP_STATE, evt.getOldValue(), evt.getNewValue());
                }
                
                return;
            }
            
            
            if (ignoreEvents) return;
            
            if ((!isWriteState) && (!customDialogShown) && 
            (preferences < 2)) { 
                return;
            }
            
            if ((env != null) && (!env.isChangeImmediate()) && (customDialogShown)) {
                return;
            }
            
            // ExPropertyEditor can fire PROP_VALUE_VALID property change, which
            // doesn't mean the change of edited property.
            if (ExPropertyEditor.PROP_VALUE_VALID.equals (evt.getPropertyName ())) {
                return;
            }
            
            if (editor != null) {            
                try {
                    ignoreEvents = true;
                    Object newValue = editor.getValue();
                    Object oldValue = null;
                    try {
                        if (canRead) {
                            oldValue = model.getValue();
                        }
                    } catch (ProxyNode.DifferentValuesException dve) {
                        // ok - no problem here, it was just the old value
                    }
                    // test if newValue is not equal to oldValue
                    if ((newValue != null && !newValue.equals(oldValue)) || 
                        (newValue == null && oldValue != null)) {
                        model.setValue(newValue);
                    }
                } catch (InvocationTargetException e) {
                    notifyExceptionWhileSettingProperty(e);
                    try {
                        if (canRead) {
                            editor.setValue(model.getValue());
                        }
                    } catch (ProxyNode.DifferentValuesException dve) {
                        // ok - no problem here, it was just the old value
                    } catch (Exception ex) {
                        PropertyDialogManager.notify(ex);
                    }
                } finally {
                    ignoreEvents = false;
                }
            }
        }
    } // End of class EditorListener.

    /* Overriden to forward focus request to focus traversable subcomponent. */
    public void requestFocus() {
        requestDefaultFocus();
    }

    ////////////////// Accessibility support ///////////////////////////////////
    
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessiblePropertyPanel();
        }
        return accessibleContext;
    }

    private class AccessiblePropertyPanel extends AccessibleJComponent {
	AccessiblePropertyPanel() {}
        public javax.accessibility.AccessibleRole getAccessibleRole() {
            return javax.accessibility.AccessibleRole.PANEL;
        }
        
        public String getAccessibleName() {
	    String name = super.getAccessibleName();

            if (name == null && model instanceof ExPropertyModel) {
                FeatureDescriptor fd = ((ExPropertyModel)model).getFeatureDescriptor();
                name = NbBundle.getMessage(PropertyPanel.class,
                        "ACS_PropertyPanel", fd.getDisplayName());
            }
            
            return name;
        }
        
        public String getAccessibleDescription() {
            String description = super.getAccessibleDescription();

            if (description == null && model instanceof ExPropertyModel) {
                FeatureDescriptor fd = ((ExPropertyModel)model).getFeatureDescriptor();
                description = NbBundle.getMessage(PropertyPanel.class,
                        "ACSD_PropertyPanel", fd.getShortDescription());
            }
            
            return description;
        }
    }

    private String getWriteComponentAccessibleName() {
        String name = getAccessibleContext().getAccessibleName();
        if (name == null)
            return null;

        return NbBundle.getMessage(PropertyPanel.class,
                "ACS_PropertyPanelWriteComponent", name,
                (editor == null) ? getString("CTL_No_value") : editor.getAsText());
    }

    private String getWriteComponentAccessibleDescription() {
        String description = getAccessibleContext().getAccessibleDescription();
        if (description == null)
            return null;

        return NbBundle.getMessage(PropertyPanel.class, 
                "ACSD_PropertyPanelWriteComponent",
                description, getPanelToolTipText());
    }

    private class PropertyTextField extends JTextField {
        PropertyTextField() {}
        //XXX workaround of jdkbug #4670767 in jdk1.4
        // JTextField filters the new lines if the property filterNewlines is TRUE
        // which is TRUE as default
        // there is a problem when in PropertyTextFiled is set a multi-line string
        // then single-line string is get back, see issue 22450
        public void setDocument(Document doc) {
            super.setDocument(doc);
            if (doc != null) {
                doc.putProperty("filterNewlines", null);
            }
        }
        
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessiblePropertyTextField();
            }
            return accessibleContext;
        }

        private class AccessiblePropertyTextField extends AccessibleJTextField {
            public String getAccessibleName() {
                return getWriteComponentAccessibleName();
            }
            public String getAccessibleDescription() {
                return getWriteComponentAccessibleDescription();
            }
        }
    }

    private class PropertyComboBox extends JComboBox {
        PropertyComboBox() {}
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessiblePropertyComboBox();
            }
            return accessibleContext;
        }

        private class AccessiblePropertyComboBox extends AccessibleJComboBox {
	    AccessiblePropertyComboBox() {}
            public String getAccessibleName() {
                return getWriteComponentAccessibleName();
            }
            public String getAccessibleDescription() {
                return getWriteComponentAccessibleDescription();
            }
        }
    }

    private class PropertySheetButton extends SheetButton {
        
        PropertySheetButton() {
        }
        
        PropertySheetButton (String aLabel, boolean isPlasticButton, boolean plasticActionNotify) {
            super(aLabel, isPlasticButton, plasticActionNotify);
        }

        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessiblePropertySheetButton();
            }
            return accessibleContext;
        }

        private class AccessiblePropertySheetButton extends AccessibleSheetButton {
	    AccessiblePropertySheetButton() {}
            public String getAccessibleName() {
                return getReadComponentAccessibleName();
            }
            public String getAccessibleDescription() {
                return getReadComponentAccessibleDescription();
            }

            private String getReadComponentAccessibleName() {
                String name = PropertyPanel.this.getAccessibleContext().getAccessibleName();
                if (name == null)
                    return null;
                
                return NbBundle.getMessage(PropertyPanel.class,
                        "ACS_PropertyPanelReadComponent",
                        name, super.getAccessibleName());
            }

            private String getReadComponentAccessibleDescription() {
                String description = PropertyPanel.this.getAccessibleContext().getAccessibleDescription();
                if (description == null)
                    return null;
                
                String beansList = null;
                int j = 0;
                if (model instanceof ExPropertyModel) {
                    Object[] beans = ((ExPropertyModel)model).getBeans();
                    String delimiter = getString("ACSD_BeanListDelimiter"); // NOI18N
                    for (int i = 0; i < beans.length; i++) {
                        if (beans[i] instanceof Node) {
                            Node n = ((Node)beans[i]);
                            beansList = ((beansList == null) ? "" : beansList + delimiter) + n.getDisplayName(); // NOI18N
                            j++;
                        }
                    }
                }
                
                Class clazz = model.getPropertyType();
                return NbBundle.getMessage(PropertyPanel.class,
                        "ACSD_PropertyPanelReadComponent", new Object[] {
                                description,
                                clazz == null ? getString("CTL_No_type") : clazz.getName(), 
                                new Integer(j),
                                beansList
                        });
            }
        }
    }

}
