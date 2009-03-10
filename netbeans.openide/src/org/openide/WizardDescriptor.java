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

package org.openide;

import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.net.URL;
import java.lang.ref.WeakReference;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Mutex;
import org.openide.util.WeakSet;
import org.openide.awt.HtmlBrowser;

import javax.accessibility.*;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/** Implements a basic "wizard" GUI system.
* A list of <em>wizard panels</em> may be specified and these
* may be traversed at the proper times using the "Previous"
* and "Next" buttons (or "Finish" on the last one).
* @author Ian Formanek, Jaroslav Tulach
* @see DialogDisplayer#createDialog
*/
public class WizardDescriptor extends DialogDescriptor {

    /** "Next" button option.
    * @see #setOptions */
    public static final Object NEXT_OPTION = new Object ();
    /** "Finish" button option.
    * @see #setOptions */
    public static final Object FINISH_OPTION = OK_OPTION;
    /** "Previous" button option.
    * @see #setOptions */
    public static final Object PREVIOUS_OPTION = new Object ();

    /** real buttons to be placed instead of the options */
    private final JButton nextButton = new JButton ();
    private final JButton finishButton = new JButton ();
    private final JButton cancelButton = new JButton ();
    private final JButton previousButton = new JButton ();

    /** a component with wait cursor */
    transient private Component waitingComponent;
    
    private static final ActionListener CLOSE_PREVENTER = new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
                }
            };

    {
        // button init
        ResourceBundle b = NbBundle.getBundle ("org.openide.Bundle"); // NOI18N
        nextButton.setText (b.getString ("CTL_NEXT"));
        previousButton.setText (b.getString ("CTL_PREVIOUS"));
        finishButton.setText (b.getString ("CTL_FINISH"));
        finishButton.getAccessibleContext().setAccessibleDescription(b.getString ("ACSD_FINISH"));
        cancelButton.setText (b.getString ("CTL_CANCEL"));
        cancelButton.getAccessibleContext().setAccessibleDescription(b.getString ("ACSD_CANCEL"));        

        previousButton.setMnemonic (b.getString ("CTL_PREVIOUS_Mnemonic").charAt(0));
        finishButton.setMnemonic (b.getString ("CTL_FINISH_Mnemonic").charAt(0));

        finishButton.setDefaultCapable (true);
        nextButton.setDefaultCapable (true);
        previousButton.setDefaultCapable (false);
        cancelButton.setDefaultCapable (false);
    }
    
    /** <CODE>Boolean</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to <CODE>true</CODE> for enabling other properties. It is relevant only on 
     * initialization (client property in first panel).
     * When false or not present in JComponent.getClientProperty(), then supplied panel is 
     * used directly without content, help or panel name auto layout.
     */
    private static final String PROP_AUTO_WIZARD_STYLE = "WizardPanel_autoWizardStyle"; // NOI18N
    /** <CODE>Boolean</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to <CODE>true</CODE> for showing help pane in the left pane. It is relevant only on 
     * initialization (client property in first panel).
     */
    private static final String PROP_HELP_DISPLAYED = "WizardPanel_helpDisplayed"; // NOI18N
    /** <CODE>Boolean</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to <CODE>true</CODE> for showing content pane in the left pane. It is relevant only on 
     * initialization (client property in first panel).
     */
    private static final String PROP_CONTENT_DISPLAYED = "WizardPanel_contentDisplayed"; // NOI18N
    /** <CODE>Boolean</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to <CODE>true</CODE> for displaying numbers in the content. It is relevant only on 
     * initialization (client property in first panel).
     */
    private static final String PROP_CONTENT_NUMBERED = "WizardPanel_contentNumbered"; // NOI18N
    /** <CODE>Integer</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Represents index of content item which will be highlited.
     */
    private static final String PROP_CONTENT_SELECTED_INDEX = "WizardPanel_contentSelectedIndex"; // NOI18N
    /** <CODE>String[]</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Represents array of content items.
     */
    private static final String PROP_CONTENT_DATA = "WizardPanel_contentData"; // NOI18N
    /** <CODE>Color</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to background color of content pane.
     */
    private static final String PROP_CONTENT_BACK_COLOR = "WizardPanel_contentBackColor"; // NOI18N
    /** <CODE>Color</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to foreground color of content pane.
     */
    private static final String PROP_CONTENT_FOREGROUND_COLOR = "WizardPanel_contentForegroundColor"; // NOI18N
    /** <CODE>Image</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to image which should be displayed in the left pane (behind the content).
     */
    private static final String PROP_IMAGE = "WizardPanel_image"; // NOI18N
    /** <CODE>String</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Set to side where the image should be drawn.
     */
    private static final String PROP_IMAGE_ALIGNMENT = "WizardPanel_imageAlignment"; // NOI18N
    /** <CODE>Dimension</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Dimension of left pane, should be same as dimension of <CODE>PROP_IMAGE</CODE>.
     * It is relevant only on initialization (client property in first panel).
     */
    private static final String PROP_LEFT_DIMENSION = "WizardPanel_leftDimension"; // NOI18N
    /** <CODE>URL</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE> or
     * <CODE>((JComponent)Panel.getComponent()).getClientProperty()</CODE> in this order.
     * Represents URL of help displayed in left pane.
     */
    private static final String PROP_HELP_URL = "WizardPanel_helpURL"; // NOI18N
    /** <CODE>String</CODE> property. The value is taken from <CODE>WizardDescriptor.getProperty()</CODE>.
     * If it contains non-null value the String is displayed the bottom of the wizard
     * and should inform user why the panel is invalid and why the Next/Finish
     * buttons were disabled.
     * @since 3.39
     */
    private static final String PROP_ERROR_MESSAGE = "WizardPanel_errorMessage"; // NOI18N

    /** Reference to default image */
    private static WeakReference defaultImage;
    
    /** Whether wizard panel will be constructed from <CODE>WizardDescriptor.getProperty()</CODE>/
     * <CODE>(JComponent)Panel.getComponent()</CODE> client properties or returned 
     * <CODE>Component</CODE> will be inserted to wizard dialog directly.
     */
    private boolean autoWizardStyle = false;
    /** Whether properties from first <CODE>(JComponent)Panel.getComponent()</CODE>
     * have been initialized.
     */
    private boolean init = false;
    /** Panel which is used when in <CODE>AUTO_WIZARD_STYLE</CODE> mode.*/
    private WizardPanel wizardPanel;
    /** Image */
    private Image image;
    /** Content data */
    private String[] contentData = new String[] {};
    /** Selected content index */
    private int contentSelectedIndex = -1;
    /** Background color*/
    private Color contentBackColor;
    /** Foreground color*/
    private Color contentForegroundColor;
    /** Help URL displayed in the left pane */
    private URL helpURL;
    /** Listener on a user component client property changes*/
    private PropL propListener;
    /** Whether to use default image or not */
    private boolean useDefaultImage = true;
    /** 'North' or 'South' */
    private String imageAlignment = "North"; // NOI18N
    
    /** Iterator between panels in the wizard */
    private Iterator panels;

    /** Change listener that invokes method update state */
    private Listener listener;

    /** current panel */
    private Panel current;

    /** settings to be used for the panels */
    private Object settings;

    /** message format to create title of the document */
    private MessageFormat titleFormat;

    /** hashtable with additional settings that is usually used
    * by Panels to store their data
    */
    private Map properties;

    /** Create a new wizard from a fixed list of panels, passing some settings to the panels.
    * @param wizardPanels the panels to use
    * @param settings the settings to pass to panels, or <code>null</code>
    * @see #WizardDescriptor(WizardDescriptor.Iterator, Object)
    */
    public WizardDescriptor (Panel[] wizardPanels, Object settings) {
        this (new ArrayIterator (wizardPanels), settings);
    }

    /** Create a new wizard from a fixed list of panels with settings
    * defaulted to <CODE>this</CODE>.
    *
    * @param wizardPanels the panels to use
    * @see #WizardDescriptor(WizardDescriptor.Iterator, Object)
    */
    public WizardDescriptor (Panel[] wizardPanels) {
        // passing CLOSE_PREVENTER which is treated especially
        this (wizardPanels, CLOSE_PREVENTER);
    }

    /** Create wizard for a sequence of panels, passing some settings to the panels.
    * @param panels iterator over all {@link WizardDescriptor.Panel}s that can appear in the wizard
    * @param settings the settings to provide to the panels (may be any data understood by them)
    * @see WizardDescriptor.Panel#readSettings
    * @see WizardDescriptor.Panel#storeSettings
    */
    public WizardDescriptor (Iterator panels, Object settings) {
        super ("", "", true, DEFAULT_OPTION, null, CLOSE_PREVENTER); // NOI18N
        this.settings = settings == CLOSE_PREVENTER ? this : settings;

        listener = new Listener ();

        nextButton.addActionListener (listener);
        previousButton.addActionListener (listener);
        finishButton.addActionListener (listener);
        cancelButton.addActionListener (listener);

        super.setOptions (new Object[] { previousButton, nextButton, finishButton, cancelButton });
        super.setClosingOptions (new Object[] { finishButton, cancelButton });

        this.panels = panels;
        panels.addChangeListener (listener);
        
    }

    /** Create wizard for a sequence of panels, with settings
    * defaulted to <CODE>this</CODE>.
    *
    * @param panels iterator over all {@link WizardDescriptor.Panel}s that can appear in the wizard
    */
    public WizardDescriptor (Iterator panels) {
        // passing CLOSE_PREVENTER which is treated especially
        this (panels, CLOSE_PREVENTER);
    }
    
    /** Initializes settings.
     */
    protected void initialize () {
        super.initialize ();

        updateState ();
    }

    /** Set a different list of panels.
    * Correctly updates the buttons.
    * @param panels the new list of {@link WizardDescriptor.Panel}s 
    */
    public final synchronized void setPanels (Iterator panels) {
        if (panels != null) {
            panels.removeChangeListener (listener);
        }
        this.panels = panels;
        panels.addChangeListener (listener);
        init = false;

        updateState ();
    }

    /** Set options permitted by the wizard considered as a <code>DialogDescriptor</code>.
    * Substitutes tokens such as {@link #NEXT_OPTION} with the actual button.
    *
    * @param options the options to set
    */
    public void setOptions (Object[] options) {
        super.setOptions (convertOptions (options));
    }

    /**
    * @param options the options to set
    */
    public void setAdditionalOptions (Object[] options) {
        super.setAdditionalOptions (convertOptions (options));
    }

    /**
    * @param options the options to set
    */
    public void setClosingOptions (Object[] options) {
        super.setClosingOptions (convertOptions (options));
    }

    /** Converts some options.
    */
    private Object[] convertOptions (Object[] options) {
        Object[] clonedOptions = (Object[])options.clone ();
        for (int i = clonedOptions.length - 1; i >= 0; i--) {
            if (clonedOptions[i] == NEXT_OPTION) clonedOptions[i] = nextButton;
            if (clonedOptions[i] == PREVIOUS_OPTION) clonedOptions[i] = previousButton;
            if (clonedOptions[i] == FINISH_OPTION) clonedOptions[i] = finishButton;
            if (clonedOptions[i] == CANCEL_OPTION) clonedOptions[i] = cancelButton;
        }
        return clonedOptions;
    }
    /** Overriden to ensure that returned value is one of 
     * the XXX_OPTION constants.
     */
    public Object getValue() {
        return backConvertOption(super.getValue());
    }
    
    /** Converts the option back to one of the constants.
     * It is called from getValue().
     */
    private Object backConvertOption(Object op) {
        if (op == nextButton) {
            return NEXT_OPTION;
        }
        if (op == previousButton) {
            return PREVIOUS_OPTION;
        }
        if (op == finishButton) {
            return FINISH_OPTION;
        }
        if (op == cancelButton) {
            return CANCEL_OPTION;
        }
        // if we don't know just return the original value
        return op;
    }

    /** Sets the message format to create title of the wizard.
    * The format can take two parameters. The name of the
    * current component and the name returned by the iterator that
    * defines the order of panels. The default value is something
    * like
    * <PRE>
    *   {0} wizard {1}
    * </PRE>
    * That can be expanded to something like this
    * <PRE>
    *   EJB wizard (1 of 8)
    * </PRE>
    * This method allows anybody to provide own title format.
    * 
    * @param format message format to the title
    */
    public void setTitleFormat (MessageFormat format) {
        titleFormat = format;
        if (init)
            updateState ();
    }

    /** Getter for current format to be used to format title.
    * @return the format
    * @see #setTitleFormat
    */
    public synchronized MessageFormat getTitleFormat () {
        if (titleFormat == null) {
            // ok, initialize the default one
            titleFormat = new MessageFormat (
	    	NbBundle.getMessage (WizardDescriptor.class, "CTL_WizardName"));
        }
        return titleFormat;
    }

    /** Allows Panels that use WizardDescriptor as settings object to
    * store additional settings into it.
    *
    * @param name name of the property
    * @param value value of property
    */
    public void putProperty (final String name, Object value) {
        Object oldValue = null;
        synchronized (this) {
            if (properties == null) {
                properties = new HashMap (7);
            }
            oldValue = properties.get (name);
            properties.put (name, value);
        }
        // bugfix #27738, firing changes in a value of the property
        firePropertyChange (name, oldValue, value);
        if (propListener != null) {
            Mutex.EVENT.readAccess(new Runnable() {
                public void run() {
                    propListener.propertyChange(
                        new PropertyChangeEvent(this, name, null, null)
                    );
                }
            });
        }
        if (PROP_ERROR_MESSAGE.equals(name)) {
            WizardPanel wp = wizardPanel;
            if (wp != null) {
                wp.setErrorMessage((String)(value == null ? " " : value)); //NOI18N
            }
        }
    }

    /** Getter for stored property.
    * @param name name of the property
    * @return the value
    */
    public synchronized Object getProperty (String name) {
        return properties == null ? null : properties.get (name);
    }

    public void setHelpCtx (final HelpCtx helpCtx) {
        if ((wizardPanel != null) && (helpCtx != null)) {
            HelpCtx.setHelpIDString(wizardPanel, helpCtx.getHelpID());
        }
        // we call the inherited method after setting the ID
        // on the panel becuase super.setHelpCtx fires the change
        super.setHelpCtx(helpCtx);
    }
    
    /** Updates buttons to reflect the current state of the panels.
    * Can be overridden by subclasses
    * to change the options to special values. In such a case use:
    * <p><code><PRE>
    *   super.updateState ();
    *   setOptions (...);
    * </PRE></code>
    */
    protected synchronized void updateState () {
        
        Panel p = panels.current ();
        
        // listeners on the panel
        if (current != p) {
            if (current != null) {
                // remove
                current.removeChangeListener (listener);
                current.storeSettings (settings);
            }
            // Hack - obtain current panel again
            // It's here to allow dynamic change of panels in wizard
            // (which can be done in storeSettings method)
            p = panels.current ();

            // add to new
            p.addChangeListener (listener);

            current = p;
            current.readSettings (settings);
        }

        boolean next = panels.hasNext ();
        boolean prev = panels.hasPrevious ();
        boolean valid = p.isValid ();

        nextButton.setEnabled (next && valid);
        previousButton.setEnabled (prev);
        finishButton.setEnabled (
            valid &&
            (!next || (current instanceof FinishPanel))
        );

        //    nextButton.setVisible (next);
        //    finishButton.setVisible (!next || (current instanceof FinishPanel));

        if (next) {
            setValue (nextButton);
        } else {
//            setValue (finishButton); 
        }

        setHelpCtx (p.getHelp ());

        java.awt.Component c = p.getComponent ();
        if (c == null || c instanceof java.awt.Window) throw new IllegalStateException("Wizard panel " + p + " gave a strange component " + c); // NOI18N
        /* commented out - issue #32927. Replaced by javadoc info in WizardDescriptor.Panel
        if (c == p) {
            warnPanelIsComponent(p.getClass());
        }*/

        //initialize wizardPanel
        if (!init) {
            if (c instanceof JComponent) {
                autoWizardStyle = getBooleanProperty(
                    (JComponent)c, PROP_AUTO_WIZARD_STYLE);
                if (autoWizardStyle) {
                    wizardPanel = new WizardPanel(
                        getBooleanProperty((JComponent)c, PROP_CONTENT_DISPLAYED),
                        getBooleanProperty((JComponent)c, PROP_HELP_DISPLAYED),
                        getBooleanProperty((JComponent)c, PROP_CONTENT_NUMBERED),
                        getLeftDimension((JComponent)c)
                    );
                    initBundleProperties();
                }
            }
            if (propListener == null)
                propListener = new PropL();
            init = true;
        }
        //update wizardPanel
        if (wizardPanel != null) {
            Component oldComp = wizardPanel.getRightComponent();
            if (oldComp != null)
                oldComp.removePropertyChangeListener(propListener);
            if (c instanceof JComponent) {
                setPanelProperties((JComponent)c);
                wizardPanel.setContent(contentData);
                wizardPanel.setSelectedIndex(contentSelectedIndex);
                wizardPanel.setContentBackColor(contentBackColor);
                wizardPanel.setContentForegroundColor(contentForegroundColor);
                wizardPanel.setImage(image);
                wizardPanel.setImageAlignment(imageAlignment);
                wizardPanel.setHelpURL(helpURL);
                updateButtonAccessibleDescription();
                c.addPropertyChangeListener(propListener);
            }
            if (wizardPanel.getRightComponent() != c) {
                wizardPanel.setRightComponent(c);
                if (wizardPanel != getMessage()) {
                    setMessage(wizardPanel);
                }
                else {
                    // force revalidate and repaint because the contents of
                    // wizardPanel has changed.  See NbPresenter code
                    firePropertyChange(DialogDescriptor.PROP_MESSAGE, null, wizardPanel);
                }
            }
        } else if (c != getMessage ())
            setMessage (c);

        String panelName = c.getName ();
        if (panelName == null) {
            panelName = ""; // NOI18N
        }
        Object[] args = {
            panelName,
            panels.name ()
        };
        MessageFormat mf = getTitleFormat ();

        if (autoWizardStyle)
            wizardPanel.setPanelName(mf.format(args));
        else {
            setTitle (mf.format (args));
        }
    }

    /** Shows blocking wait cursor during updateState run */
    private void updateStateWithFeedback () {
        try {
            showWaitCursor ();
            updateState();
        } finally {
            showNormalCursor();
        }
    }

    /** Shows next step in UI of wizards, displays wait cursot during the change. 
     */
    private void goToNextStep (Dimension previousSize) {
        try {
            showWaitCursor ();
            boolean alreadyUpdated = false;
            // enable auto-resizing policy only for fonts bigger thne default
            if (((Font)UIManager.getDefaults().get("controlFont")).getSize() >
                UIManager.getDefaults().getInt("nbDefaultFontSize")) {
                Window parentWindow = SwingUtilities.getWindowAncestor((Component)getMessage());
                if (parentWindow != null) {
                    // get tree lock to prevent from drawing in between - tries
                    // to minimize flicker (hm, but not very succesfully, don't know why...)
                    synchronized (parentWindow.getTreeLock()) {
                        updateState();
                        alreadyUpdated = true;
                        resizeWizard(parentWindow, previousSize);
                    }
                }
            }
            if (!alreadyUpdated) {
                updateState();
            }
        } finally {
            showNormalCursor();
        }
    }

    /** Tries to resize wizard wisely if needed. Keeps "display inertia" so that
     * wizard is only enlarged, not shrinked, and location is changed only when
     * wizard window exceeds screen bounds after resize.
     */
    private void resizeWizard (Window parentWindow, Dimension prevSize) {
        Dimension curSize = panels.current().getComponent().getPreferredSize();
        // only enlarge if needed, don't shrink
        if ((curSize.width > prevSize.width) || (curSize.height > prevSize.height)) {
            Rectangle origBounds = parentWindow.getBounds();
            int newWidth = Math.max(origBounds.width + (curSize.width - prevSize.width),
                                    origBounds.width);
            int newHeight = Math.max(origBounds.height + (curSize.height - prevSize.height),
                                     origBounds.height);
            Rectangle screenBounds = Utilities.getUsableScreenBounds();
            Rectangle newBounds;
            // don't allow to exceed screen size, center if needed
            if (((origBounds.x + newWidth) > screenBounds.width) ||
                ((origBounds.y + newHeight) > screenBounds.height)) {
                newWidth = Math.min(screenBounds.width, newWidth);
                newHeight = Math.min(screenBounds.height, newHeight);
                newBounds = Utilities.findCenterBounds(new Dimension(newWidth, newHeight));
            } else {
                newBounds = new Rectangle(origBounds.x, origBounds.y, newWidth, newHeight);
            }
            parentWindow.setBounds(newBounds);
            parentWindow.invalidate();
            parentWindow.validate();
            parentWindow.repaint();
        }
    }
    
    private void showWaitCursor () {
        if (wizardPanel == null || wizardPanel.getRootPane () == null) {
            // if none root pane --> don't set wait cursor
            return ;
        }
        waitingComponent = wizardPanel.getRootPane ().getContentPane ();
        waitingComponent.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
    }
    
    private void showNormalCursor () {
        if (waitingComponent == null) {
            // none waitingComponent --> don't change cursor to normal
            return ;
        }
        waitingComponent.setCursor (null);
        waitingComponent = null;
    }
    
    /* commented out - issue #32927. Replaced by javadoc info in WizardDescriptor.Panel
    private static final Set warnedPanelIsComponent = new WeakSet(); // Set<Class>
    private static synchronized void warnPanelIsComponent(Class c) {
        if (warnedPanelIsComponent.add(c)) {
            StringBuffer buffer = new StringBuffer(150);
            buffer.append("WARNING - the WizardDescriptor.Panel implementation "); // NOI18N
            buffer.append(c.getName());
            buffer.append(" provides itself as the result of getComponent().\n"); // NOI18N
            buffer.append("This hurts performance and can cause a clash when Component.isValid() is overridden.\n"); // NOI18N
            buffer.append("Please use a separate component class, see details at http://performance.netbeans.org/howto/dialogs/wizard-panels.html."); // NOI18N
            ErrorManager.getDefault().log(ErrorManager.WARNING, buffer.toString());
        }
    }
    */

    /** Tryes to get property from getProperty() if doesn't succeed then tryes at
     * supplied <CODE>JComponent</CODE>s client property.
     * @param c origin of property
     * @param s name of property
     * @return boolean property
     */
    private boolean getBooleanProperty(JComponent c, String s) {
        Object property = getProperty(s);
        if (property instanceof Boolean)
            return ((Boolean)property).booleanValue();
        property = c.getClientProperty(s);
        if (property instanceof Boolean)
            return ((Boolean)property).booleanValue();
        return false;
    }

    /** Tryes to get dimension of wizard panel's left pane from getProperty() 
     * if doesn't succeed then tryes at
     * supplied <CODE>JComponent</CODE>s client property.
     * @return <CODE>Dimension</CODE> dimension of wizard panel's left pane
     */
    private Dimension getLeftDimension(JComponent c) {
        Dimension leftDimension;
        Object property = c.getClientProperty(PROP_LEFT_DIMENSION);
        if (property instanceof Dimension)
            leftDimension = (Dimension)property;
        else
            leftDimension = new Dimension(198, 233);
        return leftDimension;
    }
    
    /** Tryes to get properties from getProperty() if doesn't succeed then tryes at
     * supplied <CODE>JComponent</CODE>s client properties and store them
     * to appropriate fields.
     * @param c origin of property
     * @param s name of property
     * @return boolean property
     */
    private void setPanelProperties(JComponent c) {
        // TODO: Method should be devided into individual setter/getter methods !?
        Object property = getProperty(PROP_CONTENT_SELECTED_INDEX);
        if (property instanceof Integer)
            contentSelectedIndex = ((Integer)property).intValue();
        else {
            property = c.getClientProperty(PROP_CONTENT_SELECTED_INDEX);
            if (property instanceof Integer)
                contentSelectedIndex = ((Integer)property).intValue();
        }
        property = getProperty(PROP_CONTENT_DATA);
        if (property instanceof String[])
            contentData = (String[])property;
        else {
            property = c.getClientProperty(PROP_CONTENT_DATA);
            if (property instanceof String[])
                contentData = (String[])property;
        }
        property = getProperty(PROP_IMAGE);
        if (property instanceof Image) {
            image = (Image)property;
        } else if ((properties == null) || (!properties.containsKey(PROP_IMAGE))) {
            property = c.getClientProperty(PROP_IMAGE);
            if (property instanceof Image)
                image = (Image)property;
            else if (image == null)
                useDefaultImage = true;
        } else {
            useDefaultImage = false;
        }
        property = getProperty(PROP_IMAGE_ALIGNMENT);
        if (property instanceof String) {
            imageAlignment = (String)property;
        } else {
            property = c.getClientProperty(PROP_IMAGE_ALIGNMENT);
            if (property instanceof String)
                imageAlignment = (String)property;
        }
        property = getProperty(PROP_CONTENT_BACK_COLOR);
        if (property instanceof Color)
            contentBackColor = (Color)property;
        else {
            property = c.getClientProperty(PROP_CONTENT_BACK_COLOR);
            if (property instanceof Color)
                contentBackColor = (Color)property;
        }
        property = getProperty(PROP_CONTENT_FOREGROUND_COLOR);
        if (property instanceof Color)
            contentForegroundColor = (Color)property;
        else {
            property = c.getClientProperty(PROP_CONTENT_FOREGROUND_COLOR);
            if (property instanceof Color)
                contentForegroundColor = (Color)property;
        }
        property = c.getClientProperty(PROP_HELP_URL);
        if (property instanceof URL)
            helpURL = (URL)property;
        else if (property == null)
            helpURL = null;
    }
    
    private void initBundleProperties() {
        contentBackColor = new Color(getIntFromBundle("INT_WizardBackRed"), // NOI18N
                                     getIntFromBundle("INT_WizardBackGreen"), // NOI18N
                                     getIntFromBundle("INT_WizardBackBlue")); // NOI18N

        contentForegroundColor = new Color(getIntFromBundle("INT_WizardForegroundRed"), // NOI18N
                                           getIntFromBundle("INT_WizardForegroundGreen"), // NOI18N
                                           getIntFromBundle("INT_WizardForegroundBlue")); // NOI18N
        imageAlignment = bundle.getString("STRING_WizardImageAlignment"); //NOI18N
    }

    /** Overrides superclass method. Adds reseting of wizard
     * for <code>CLOSED_OPTION</code>. */
    public void setValue(Object value) {
        //Bugfix #25820: Call resetWizard to make sure that storeSettings
        //is called before propertyChange.
        Object convertedValue = backConvertOption(value);
        if (convertedValue == OK_OPTION) {
            resetWizard();
        }
        super.setValue(backConvertOption(value));
        
        // #17360: Reset wizard on CLOSED_OPTION too.
        if(value == CLOSED_OPTION) {
            resetWizard();
        }
    }
    
    /** Resets wizard when after closed/cancelled/finished the wizard dialog. */
    private void resetWizard() {
        if(current != null) {
            current.storeSettings (settings);
            current.removeChangeListener(listener);
            current = null;
            if (wizardPanel != null) {
                wizardPanel.resetPreferredSize();
            }
        }
        panels.removeChangeListener(listener);
    }
    
    ResourceBundle bundle = NbBundle.getBundle(WizardDescriptor.class);
    
    private int getIntFromBundle(String key) {
//        if (bundle == null)
//            bundle = NbBundle.getBundle(WizardDescriptor.class);
        return Integer.parseInt(bundle.getString(key));
    }

    private static Image getDefaultImage() {
        Image img = null;
        if (defaultImage != null)
            img = (Image)defaultImage.get();
        if (img == null) {
            java.net.URL url = NbBundle.getLocalizedFile(
                 "org.openide.resources.defaultWizard", // NOI18N
                 "gif" // NOI18N
             );

            img = url == null ? null : java.awt.Toolkit.getDefaultToolkit().getImage(url);
            if (img != null)
                defaultImage = new WeakReference(img);
        }
        return img;
    }
    
    private void updateButtonAccessibleDescription() {
        String stepName = contentData != null && contentSelectedIndex > 0 && contentSelectedIndex - 1 < contentData.length
                        ? contentData[contentSelectedIndex - 1]
                        : ""; // NOI18N
        previousButton.getAccessibleContext().setAccessibleDescription(
		NbBundle.getMessage (WizardDescriptor.class, "ACSD_PREVIOUS",
            		new Integer(contentSelectedIndex), stepName
	));

        stepName = contentData != null && contentSelectedIndex < contentData.length - 1 && contentSelectedIndex + 1 >= 0
                ? contentData[contentSelectedIndex + 1]
                : ""; // NOI18N
        nextButton.getAccessibleContext().setAccessibleDescription(
		NbBundle.getMessage (WizardDescriptor.class, "ACSD_NEXT",
            		new Integer(contentSelectedIndex + 2), stepName
        ));
    }

    /** Iterator on the sequence of panels.
    * @see WizardDescriptor.Panel
    */
    public interface Iterator {
        /** Get the current panel.
        * @return the panel
        */
        public Panel current ();

        /** Get the name of the current panel.
        * @return the name
        */
        public String name ();

        /** Test whether there is a next panel.
        * @return <code>true</code> if so
        */
        public boolean hasNext ();

        /** Test whether there is a previous panel.
        * @return <code>true</code> if so
        */
        public boolean hasPrevious ();

        /** Move to the next panel.
        * I.e. increment its index, need not actually change any GUI itself.
        * @exception NoSuchElementException if the panel does not exist
        */
        public void nextPanel ();

        /** Move to the previous panel.
        * I.e. decrement its index, need not actually change any GUI itself.
        * @exception NoSuchElementException if the panel does not exist
        */
        public void previousPanel ();

        /** Add a listener to changes of the current panel.
        * The listener is notified when the possibility to move forward/backward changes.
        * @param l the listener to add
        */
        public void addChangeListener (ChangeListener l);

        /** Remove a listener to changes of the current panel.
        * @param l the listener to remove
        */
        public void removeChangeListener (ChangeListener l);
    }

    /** One wizard panel with a component on it.
     *
     * For good performance, implementation of this interface should be as
     * lightweight as possible. Defer creation and initialization of
     * UI component of wizard panel into {@link #getComponent} method.
     *
     * Please see complete guide at http://performance.netbeans.org/howto/dialogs/wizard-panels.html
     */
    public interface Panel {
        /** Get the component displayed in this panel.
         *
         * Note; method can be called from any thread, but not concurrently
         * with other methods of this interface. Please see complete guide at
         * http://performance.netbeans.org/howto/dialogs/wizard-panels.html for
         * correct implementation.
         *
         * @return the UI component of this wizard panel
         */
        public java.awt.Component getComponent ();

        /** Help for this panel.
        * When the panel is active, this is used as the help for the wizard dialog.
        * @return the help or <code>null</code> if no help is supplied
        */
        public HelpCtx getHelp ();

        /** Provides the wizard panel with the current data--either
        * the default data or already-modified settings, if the user used the previous and/or next buttons.
        * This method can be called multiple times on one instance of <code>WizardDescriptor.Panel</code>.
        * <p>The settings object is originally supplied to {@link WizardDescriptor#WizardDescriptor(WizardDescriptor.Iterator,Object)}.
        * In the case of a <code>TemplateWizard.Iterator</code> panel, the object is
        * in fact the <code>TemplateWizard</code>.
        * @param settings the object representing wizard panel state
        * @exception IllegalStateException if the the data provided 
        * by the wizard are not valid.
        */
        public void readSettings (Object settings);

        /** Provides the wizard panel with the opportunity to update the
        * settings with its current customized state.
        * Rather than updating its settings with every change in the GUI, it should collect them,
        * and then only save them when requested to by this method.
        * Also, the original settings passed to {@link #readSettings} should not be modified (mutated);
        * rather, the object passed in here should be mutated according to the collected changes,
        * in case it is a copy.
        * This method can be called multiple times on one instance of <code>WizardDescriptor.Panel</code>.
        * <p>The settings object is originally supplied to {@link WizardDescriptor#WizardDescriptor(WizardDescriptor.Iterator,Object)}.
        * In the case of a <code>TemplateWizard.Iterator</code> panel, the object is
        * in fact the <code>TemplateWizard</code>.
        * @param settings the object representing wizard panel state
        */
        public void storeSettings (Object settings);

        /** Test whether the panel is finished and it is safe to proceed to the next one.
        * If the panel is valid, the "Next" (or "Finish") button will be enabled.
        * <p><strong>Tip:</strong> if your panel is actually the component itself
        * (so {@link #getComponent} returns <code>this</code>), be sure to specifically
        * override this method, as the unrelated implementation in {@link java.awt.Component#isValid}
        * if not overridden could cause your wizard to behave erratically.
        * @return <code>true</code> if the user has entered satisfactory information
        */
        public boolean isValid ();

        /** Add a listener to changes of the panel's validity.
        * @param l the listener to add
        * @see #isValid
        */
        public void addChangeListener (ChangeListener l);

        /** Remove a listener to changes of the panel's validity.
        * @param l the listener to remove
        */
        public void removeChangeListener (ChangeListener l);
    }

    /** A special interface for panels in middle of the
    * iterators path that would like to have the finish button
    * enabled. So both Next and Finish are enabled on panel
    * implementing this interface.
    */
    public interface FinishPanel extends Panel {
    }

    /** Special iterator that works on an array of <code>Panel</code>s.
    */
    public static class ArrayIterator extends Object implements Iterator {
        /** Array of items.
        */
        private Panel[] panels;

        /** Index into the array
        */
        private int index;

        /* Default constructor. It's here to allow subclasses to
        * be serializable easily. Panel initialization is done
        * through initializePanels() protected method. */
        public ArrayIterator () {
            panels = initializePanels();
            index = 0;
        }

        /** Construct an iterator.
        * @param array the list of panels to use
        */
        public ArrayIterator (Panel[] array) {
            panels = array;
            index = 0;
        }

        /** Allows subclasses to initialize their arrays of panels when
        * constructed using default constructor.
        * (for example during deserialization.
        * Default implementation returns empty array. */
        protected Panel[] initializePanels () {
            return new Panel[0];
        }

        /* The current panel.
        */
        public Panel current () {
            return panels[index];
        }

        /* Current name of the panel */
        public String name () {
            return NbBundle.getMessage (WizardDescriptor.class,
		    "CTL_ArrayIteratorName", new Integer (index + 1),
            	    new Integer (panels.length));
        }

        /* Is there a next panel?
        * @return true if so
        */
        public boolean hasNext () {
            return index < panels.length - 1;
        }

        /* Is there a previous panel?
        * @return true if so
        */
        public boolean hasPrevious () {
            return index > 0;
        }

        /* Moves to the next panel.
        * @exception NoSuchElementException if the panel does not exist
        */
        public synchronized void nextPanel () {
            if (index + 1 == panels.length) throw new java.util.NoSuchElementException ();
            index++;
        }

        /* Moves to previous panel.
        * @exception NoSuchElementException if the panel does not exist
        */
        public synchronized void previousPanel () {
            if (index == 0) throw new java.util.NoSuchElementException ();
            index--;
        }

        /* Ignores the listener, there are no changes in order of panels.
        */
        public void addChangeListener (ChangeListener l) {
        }

        /* Ignored.
        */
        public void removeChangeListener (ChangeListener l) {
        }

        /** Resets this iterator to initial state.
        * Called by subclasses when they need re-initialization of the iterator.
        */
        protected void reset () {
            index = 0;
        }

    }

    /** Listener to changes in the iterator and panels.
    */
    private final class Listener implements ChangeListener, ActionListener {
        Listener() {}
        /** Change in the observed objects */
        public void stateChanged (ChangeEvent ev) {
            updateState ();
        }
        /** Action listener */
        public void actionPerformed (ActionEvent ev) {
            if (ev.getSource () == nextButton) {
                Dimension previousSize = panels.current().getComponent().getSize();
                panels.nextPanel ();
                try {
                    // change UI to show next step, show wait cursor during
                    // the change
                    goToNextStep(previousSize);
                } catch (IllegalStateException ise) {
                    panels.previousPanel();
                    if (ise.getMessage() != null) {
                        // this is only for backward compatitility
                        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(ise.getMessage()));
                    } else {
                        // this should be used (it checks for exception
                        // annotations and severity)
                        ErrorManager.getDefault().notify(ise);
                    }
                    updateState();
                }
            }

            if (ev.getSource () == previousButton) {
                wizardPanel.setErrorMessage(" "); //NOI18N
                panels.previousPanel ();
                // show wait cursor when updating previous button
                updateStateWithFeedback ();
            }

            if (ev.getSource () == finishButton) {
                if (Arrays.asList(getClosingOptions()).contains(finishButton)) {
                    resetWizard();
                }
                setValue (OK_OPTION);
            }

            if (ev.getSource () == cancelButton) {
                if (Arrays.asList(getClosingOptions()).contains(cancelButton)) {
                    resetWizard();
                }
                setValue (CANCEL_OPTION);
            }
        }
    }
    
    /** Listenes on a users client property changes
     */
    private class PropL implements PropertyChangeListener {
        PropL() {}
        /** Accepts client property changes of user component */
        public void propertyChange(PropertyChangeEvent e) {
            if (wizardPanel == null)
                return;
            String propName = e.getPropertyName();
            setPanelProperties((JComponent)wizardPanel.getRightComponent());
            if (propName.equals(PROP_CONTENT_DATA)) {
                wizardPanel.setContent(contentData);
                updateButtonAccessibleDescription();
            } else if (propName.equals(PROP_CONTENT_SELECTED_INDEX)) {
                wizardPanel.setSelectedIndex(contentSelectedIndex);
                updateButtonAccessibleDescription();
            } else if (propName.equals(PROP_CONTENT_BACK_COLOR))
                wizardPanel.setContentBackColor(contentBackColor);
            else if (propName.equals(PROP_CONTENT_FOREGROUND_COLOR))
                wizardPanel.setContentForegroundColor(contentForegroundColor);
            else if (propName.equals(PROP_IMAGE))
                wizardPanel.setImage(image);
            else if (propName.equals(PROP_IMAGE_ALIGNMENT))
                wizardPanel.setImageAlignment(imageAlignment);
            else if (propName.equals(PROP_HELP_URL))
                wizardPanel.setHelpURL(helpURL);
        }
    }

    /** Panel which paints image as its background.
     */
    private static class ImagedPanel extends JComponent implements Accessible, Runnable {
        /** background image */
        Image image;
        /** helper variables for passing image between threads and painting
         * methods */
        Image tempImage, image2Load;
        /** true if default image is used */
        boolean isDefault = false;
        /** true if loading of image is in progress, false otherwise */
        boolean loadPending = false;
        boolean north = true;

        /** sync lock for image variables access */
        private final Object IMAGE_LOCK = new Object();

        /** Constrcuts panel with given image on background. 
         * @param im background image, null means default image
         */
        public ImagedPanel(Image im) {
            setImage(im);
            setLayout(new BorderLayout());
            setOpaque(true);
        }

        /** Overriden to paint backround image */
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            if (image != null) {
                graphics.drawImage(image, 0, north ? 0 : (getHeight() - image.getHeight(null)), this);
            } else if (image2Load != null) {
                loadImageInBackground(image2Load);
                image2Load = null;
            }
        }
        
        public void setImageAlignment(String align) {
            north = "North".equals(align); // NOI18N
        }

        /** Sets background image for this component. Image will be loaded 
         * asynchronously if not loaded yet. Null means default image.
         */
        public void setImage(Image im) {
            if (im != null) {
                loadImage(im);
                isDefault = false;
                return;
            }
            if (!isDefault) {
                loadImage(getDefaultImage());
                isDefault = true;
            }
        }
        
        private void loadImage (Image im) {
            // check image and just set variable if fully loaded already
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(im, 0);
            if (mt.checkID(0)) {
                image = im;
                if (isShowing()) {
                    repaint();
                }
                return;
            }
            // start loading in background or just mark that loading should
            // start when paint is invoked
            if (isShowing()) {
                loadImageInBackground(im);
            } else {
                synchronized (IMAGE_LOCK) {
                    image = null;
                }
                image2Load = im;
            }
        }
        
        private void loadImageInBackground (Image image) {
            synchronized (IMAGE_LOCK) {
                tempImage = image;
                // coalesce with previous task if hasn't really started yet
                if (loadPending) {
                    return;
                }
                loadPending = true;
            }
            // 30ms is safety time to ensure code will run asynchronously
            RequestProcessor.getDefault().post(this, 30);
        }
        
        /** Loads image stored in image2Load variable.
         * Then invokes repaint when image is fully loaded.
         */
        public void run () {
            Image localImage = null;
            // grab value 
            synchronized (IMAGE_LOCK) {
                localImage = tempImage;
                tempImage = null;
                loadPending = false;
            }
            // actually loads image
            ImageIcon localImageIcon = new ImageIcon(localImage);
            boolean shouldRepaint = false;
            synchronized (IMAGE_LOCK) {
                // don't commit results if another loading was started after us 
                if (!loadPending) {
                    image = localImageIcon.getImage();
                    // keep repaint call out of sync section
                    shouldRepaint = true;
                }
                
            }
            if (shouldRepaint) {
                repaint();
            }
        }
        
    }

    /** Text list cell renderer. Wraps text of items at specified width. Allows numbering
     * of items.
     */
    private static class WrappedCellRenderer extends JPanel implements ListCellRenderer {
        
        JTextArea ta = new JTextArea();
        JLabel numberLabel;
        int selected = -1;
        boolean contentNumbered;
        int taWidth;
        
        /**
         * @param contentNumbered Whether content will be numbered
         * @param wrappingWidth Width of list item.
         */
        private WrappedCellRenderer(boolean contentNumbered, int wrappingWidth) {
            super(new BorderLayout());
            this.contentNumbered = contentNumbered;
            
            ta.setOpaque(false);
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setFont(UIManager.getFont("Label.font")); // NOI18N
            ta.getAccessibleContext().setAccessibleDescription(""); // NOI18N
            
            taWidth = wrappingWidth - 12 - 12;
            
            numberLabel = new JLabel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    // #9804. Draw bullet if the content is not numbered.
                    if(!WrappedCellRenderer.this.contentNumbered) {
                        java.awt.Rectangle rect = g.getClipBounds();
                        g.fillOval(rect.x, rect.y, 7, 7);
                    }
                }
            };
            numberLabel.setLabelFor(ta);     // a11y
            numberLabel.setHorizontalAlignment(SwingConstants.LEFT);
            numberLabel.setVerticalAlignment(SwingConstants.TOP);
            numberLabel.setFont(ta.getFont());
            numberLabel.setOpaque(false);
            numberLabel.setPreferredSize(new Dimension(25, 0));
            add(numberLabel, BorderLayout.WEST);            
            taWidth -= 25;
            
            Insets taInsets = ta.getInsets();
            ta.setSize(taWidth, 
                       taInsets.top + taInsets.bottom + 1);

            add(ta, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setOpaque(false);
        }
        
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            if (index == selected) {
                numberLabel.setFont(numberLabel.getFont().deriveFont(Font.BOLD));
                ta.setFont(ta.getFont().deriveFont(Font.BOLD));
            } else {
                numberLabel.setFont(numberLabel.getFont().deriveFont(Font.PLAIN));
                ta.setFont(ta.getFont().deriveFont(Font.PLAIN));
            }
            
            if (contentNumbered) {
                numberLabel.setText(Integer.toString(index + 1) + "."); // NOI18N
            }
           
            // #21322: on JDK1.4 wrapping width is cleared between two rendering runs
            Insets taInsets = ta.getInsets();
            ta.setSize(taWidth, 
                       taInsets.top + taInsets.bottom + 1);
            ta.setText((String)value);
            return this;
        }
        
        private void setSelectedIndex(int i) {
            selected = i;
        }

        private void setForegroundColor(Color color) {
            if (numberLabel != null) {
                numberLabel.setForeground(color);
                numberLabel.setBackground(color);
            }
            ta.setForeground(color);
        }
    }

    /** Wizard panel. Allows auto layout of content, wizard panel name and input panel.
     */
    private static class WizardPanel extends JPanel {
        
        /** Users panel is inserted into this panel. */
        private JPanel rightPanel = new JPanel(new BorderLayout());
        /** Name of the users panel. */
        private JLabel panelName = new JLabel("Step"); //NOI18N
        /** List of content. */
        private JList contentList;
        /** Users component. Should be held for removing from rightPanel */
        private Component rightComponent;
        /** Panel which paints image */
        private ImagedPanel contentPanel;
        /** Name of content. Can be switched off.  */
        private JPanel contentLabelPanel;
        /** Wrapped list cell renderer */
        private WrappedCellRenderer cellRenderer;
        /** Tabbed pane is used only when both content and help are displayed */
        private JTabbedPane tabbedPane;
        /** HTML Browser is used only when help is displayed in the left pane */
        private HtmlBrowser htmlBrowser;
        /** Each wizard panel have to be larger or same as this */
        private Dimension cachedDimension;
        /** Label of steps pane */
        private JLabel label;
        /** Selected index of content */
        private int selectedIndex;

        private javax.swing.JLabel m_lblMessage;
        
        /** Creates new <CODE>WizardPanel<CODE>.
         * @param contentDisplayed whether content will be displayed in the left pane
         * @param helpDisplayed whether help will be displayed in the left pane
         * @param contentNumbered whether content will be numbered
         * @param leftDimension dimension of content or help pane
         */
        public WizardPanel(boolean contentDisplayed, boolean helpDispalyed,
                           boolean contentNumbered, Dimension leftDimension) {
            super(new BorderLayout());
            initComponents(contentDisplayed, helpDispalyed,
                           contentNumbered, leftDimension);
            setOpaque(false);
            resetPreferredSize();
        }
        
        private void initComponents(boolean contentDisplayed, boolean helpDisplayed,
                                    boolean contentNumbered, Dimension leftDimension) {
            if (contentDisplayed) {
                createContentPanel(contentNumbered, leftDimension);
                if (!helpDisplayed)
                    add(contentPanel, BorderLayout.WEST);
            }
            if (helpDisplayed) {
                htmlBrowser = new BoundedHtmlBrowser(leftDimension);
                htmlBrowser.setPreferredSize(leftDimension);
                if (!contentDisplayed)
                    add(htmlBrowser, BorderLayout.WEST);
            }
            if (helpDisplayed && contentDisplayed) {
                tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
                tabbedPane.addTab(NbBundle.getMessage(WizardDescriptor.class,
                                        	"CTL_ContentName"),
                                  contentPanel);
                tabbedPane.addTab(NbBundle.getMessage(WizardDescriptor.class,
                                        	"CTL_HelpName"),
                                  htmlBrowser);
                tabbedPane.setEnabledAt(1, false);
                tabbedPane.setOpaque(false);
//                tabbedPane.setPreferredSize(leftDimension);
                add(tabbedPane, BorderLayout.WEST);
            }
            panelName.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                panelName.getForeground()));
            panelName.setFont(panelName.getFont().deriveFont(Font.BOLD));
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.add(panelName, BorderLayout.NORTH);
            labelPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 11));
            rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 11, 11));
            panelName.setLabelFor(labelPanel);

            Color c = javax.swing.UIManager.getColor("nb.errorForeground"); //NOI18N
            if (c == null) {
                c = new Color(89,79,191);  // RGB suggested by Bruce in #28466
            }

            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 11));
            m_lblMessage = new javax.swing.JLabel("  "); //NOI18N
            m_lblMessage.setForeground(c);
            errorPanel.add(m_lblMessage, BorderLayout.CENTER);
            
            JPanel fullRightPanel = new JPanel(new BorderLayout());
            fullRightPanel.add(labelPanel, BorderLayout.NORTH);
            fullRightPanel.add(rightPanel, BorderLayout.CENTER);
            fullRightPanel.add(errorPanel, BorderLayout.SOUTH);

            JSeparator sep = new JSeparator();
            sep.setForeground(Color.darkGray);

            add(fullRightPanel, BorderLayout.CENTER);
            add(sep, BorderLayout.SOUTH);
        }
        
        public void setErrorMessage(String msg) {
            m_lblMessage.setText(msg);
        }

        /** Creates content panel.
         * @param contentNumbered <CODE>boolean</CODE> whether content will be numbered
         * @param leftDimension <CODE>Dimension</CODE> dimension of content pane
         */
        private void createContentPanel(boolean contentNumbered, Dimension leftDimension) {
            contentList = new JList();
            cellRenderer = new WrappedCellRenderer(contentNumbered,
                                                   leftDimension.width);
            cellRenderer.setOpaque(false);
            contentList.setCellRenderer(cellRenderer);
            contentList.setOpaque(false);
            contentList.setEnabled(false);
            contentList.getAccessibleContext().setAccessibleDescription(""); // NOI18N

            JScrollPane scroll = new JScrollPane(contentList);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(null);
            scroll.setOpaque(false);

            label = new JLabel(NbBundle.getMessage(WizardDescriptor.class,
                        			"CTL_ContentName"));
            label.setForeground(Color.white);
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, label.getForeground()));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            contentLabelPanel = new JPanel(new BorderLayout());
            contentLabelPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
            contentLabelPanel.setOpaque(false);
            contentLabelPanel.add(label, BorderLayout.NORTH);
            
            contentPanel = new ImagedPanel(null);
            contentPanel.add(contentLabelPanel, BorderLayout.NORTH);
            contentPanel.add(scroll, BorderLayout.CENTER);

            contentPanel.setPreferredSize(leftDimension);
            label.setLabelFor(contentList);
        }
        /** Setter for lists items. 
         * @param content Array of list items.
         */
        public void setContent(final String[] content) {
            final JList list = contentList;
            if(list == null) {
                return;
            }

            // #18055: Ensure it runs in AWT thread.
            // Remove this when component handling will be assured
            // by other means that runs always in AWT.
            Mutex.EVENT.writeAccess(new Runnable() {
                public void run() {
                    list.setListData(content);
                    list.revalidate();
                    list.repaint();
                    contentLabelPanel.setVisible(content.length > 0);
                }
            });
        }
        /** Setter for selected list item. 
         * @param index Index of selected item in the list.
         */
        public void setSelectedIndex(final int index) {
            selectedIndex = index;
            if (cellRenderer != null) {
                cellRenderer.setSelectedIndex(index);
                
                final JList list = contentList;
                if(list == null) {
                    return;
                }
                
                // #18055. See previous #18055 comment.
                Mutex.EVENT.readAccess(new Runnable() {
                    public void run() {
                        list.ensureIndexIsVisible(index);
                        // Fix of #10787.
                        // This is workaround for swing bug - BasicListUI doesn't ask for preferred
                        // size of rendered list cell as a result of property selectedIndex change. 
                        // It does only on certain JList property changes (e.g. fixedCellWidth).
                        // Maybe subclassing BasicListUI could be better fix.
                        list.setFixedCellWidth(0);
                        list.setFixedCellWidth(-1);
                    }
                });
            }
        }
        /** Setter for content background color. 
         * @param color content background color.
         */
        public void setContentBackColor(Color color) {
            if (contentPanel != null)
                contentPanel.setBackground(color);
        }
        /** Setter for content foreground color. 
         * @param color content foreground color.
         */
        public void setContentForegroundColor(Color color) {
            if (cellRenderer == null)
                return;
            cellRenderer.setForegroundColor(color);
            label.setForeground(color);
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, label.getForeground()));
        }
        /** Setter for content background image. 
         * @param image content background image
         */
        public void setImage(Image image) {
            if (contentPanel != null)
                contentPanel.setImage(image);
        }
        /** Setter for image alignment. 
         * @param align image alignment - 'North', 'South'
         */
        public void setImageAlignment(String align) {
            if (contentPanel != null)
                contentPanel.setImageAlignment(align);
        }
        /** Setter for user's component. 
         * @param c user's component
         */
        public void setRightComponent(Component c) {
            if (rightComponent != null)
                rightPanel.remove(rightComponent);
            rightComponent = c;
            rightPanel.add(rightComponent, BorderLayout.CENTER);
        }
        /** Getter for user's component. 
         * @return <CODE>Component</CODE> user's component
         */
        public Component getRightComponent() {
            return rightComponent;
        }
        /** Setter for wizard panel name. 
         * @param name panel name
         */
        public void setPanelName(String name) {
            panelName.setText(name);
        }
        /** Setter for help URL. 
         * @param helpURL help URL
         */
        public void setHelpURL(URL helpURL) {
            if (htmlBrowser == null)
                return;
            if (helpURL != null) {
                if (!helpURL.equals(htmlBrowser.getDocumentURL()))
                    htmlBrowser.setURL(helpURL);
                if (tabbedPane != null)
                    tabbedPane.setEnabledAt(tabbedPane.indexOfComponent(htmlBrowser),
                                            true);
            } else if (tabbedPane != null){
                tabbedPane.setSelectedComponent(contentPanel);
                tabbedPane.setEnabledAt(tabbedPane.indexOfComponent(htmlBrowser),
                                        false);
            }
        }
        
        public void resetPreferredSize() {
            cachedDimension = new Dimension(600, 365);
        }
        
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            if (dim.height > cachedDimension.height)
                cachedDimension.height = dim.height;
            if (dim.width > cachedDimension.width)
                cachedDimension.width = dim.width;
            return cachedDimension;
        }
        
        /** Overriden to delegate call to user component.
         */
        public void requestFocus() {
            if (rightComponent != null)
                rightComponent.requestFocus();
            else
                super.requestFocus();
        }
        
        /** Overriden to delegate call to user component.
         */
        public boolean requestDefaultFocus() {
            if (rightComponent instanceof JComponent)
                return ((JComponent)rightComponent).requestDefaultFocus();
            return super.requestDefaultFocus();
        }
        
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleWizardPanel();
            }
            return accessibleContext;
        }

        private class AccessibleWizardPanel extends AccessibleJPanel {
            AccessibleWizardPanel() {}
            public String getAccessibleDescription() {
                if (accessibleDescription != null) {
                    return accessibleDescription;
                }
                if (rightComponent instanceof Accessible) {
                    if (rightComponent.getAccessibleContext().getAccessibleDescription() == null) {
                        return null;
                    }
                    return NbBundle.getMessage(WizardDescriptor.class,
			"ACSD_WizardPanel", new Integer(selectedIndex + 1),
                        panelName.getText(),
                        rightComponent.getAccessibleContext().getAccessibleDescription()
                    );
                }
                return super.getAccessibleDescription();
            }
        }
    }

    /** Overriden to return wished preferred size */
    private static class BoundedHtmlBrowser extends HtmlBrowser {
        Dimension dim;
        boolean firstPage = true;
        
        public BoundedHtmlBrowser(Dimension d) {
            super(false, false);
            dim = d;
        }
        
        public Dimension getPreferredSize() {
            return dim;
        }
    }
}
