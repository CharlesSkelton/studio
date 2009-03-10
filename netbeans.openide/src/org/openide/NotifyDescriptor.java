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

package org.openide;

import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.*;
import javax.swing.JPanel;
import java.awt.Component;

import org.openide.util.NbBundle;

/**
* This class provides a description of a user notification to be displayed.
* <p>Simple example of usage:
* <pre>
* NotifyDescriptor d =
*     new NotifyDescriptor.Message("Hello...", NotifyDescriptor.INFORMATION_MESSAGE);
* DialogDisplayer.getDefault().notify(d);
* </pre>
* or to get a result:
* <pre>
* NotifyDescriptor d =
*     new NotifyDescriptor.Confirmation("Really do this?!", "Dialog Title",
*                                       NotifyDescriptor.OK_CANCEL_OPTION);
* if (DialogDisplayer.getDefault().notify(d) == NotifyDescriptor.OK_OPTION) {
*     // really do it...
* }
* </pre>
* @see DialogDisplayer#notify
* @author David Peroutka, Jaroslav Tulach
*/
public class NotifyDescriptor extends Object {

    // Property constants
    /** Name of property for the message to be displayed. */
    public static final String PROP_MESSAGE      = "message"; // NOI18N
    /** Name of property for the type of message to use. */
    public static final String PROP_MESSAGE_TYPE = "messageType"; // NOI18N
    /** Name of property for the style of options available. */
    public static final String PROP_OPTION_TYPE  = "optionType"; // NOI18N
    /** Name of property for the exact list of options. */
    public static final String PROP_OPTIONS      = "options"; // NOI18N
    /** Name of property for the value the user selected. */
    public static final String PROP_VALUE        = "value"; // NOI18N
    /** Name of property for the dialog title. */
    public static final String PROP_TITLE        = "title"; // NOI18N
    /** Name of property for the detail message reported. */
    public static final String PROP_DETAIL       = "detail"; // NOI18N
    /** Name of property for the OK button validation. */
    public static final String PROP_VALID        = "valid"; // NOI18N

    //
    // Properties
    //
    /** The message object specifying the message. */
    private Object message;
    /** The message type. */
    private int messageType = PLAIN_MESSAGE;
    /** The option type specifying the user-selectable options. */
    private int optionType;
    /** The option object specifying the user-selectable options. */
    private Object[] options;
    /** The option object specifying the additional user-selectable options. */
    private Object[] adOptions;
    /** The user's choice value object. */
    private Object value;
    /** The title string for the report. */
    private String title;
    /** Is OK button valid (enabled). */
    private boolean valid = true;
    /** The object specifying the detail object. */
    //  private Object detail;
    /** Property change support. */
    private PropertyChangeSupport changeSupport;

    //
    // Return values
    //
    /** Return value if YES is chosen. */
    public static final Object YES_OPTION = new Integer(JOptionPane.YES_OPTION);
    /** Return value if NO is chosen. */
    public static final Object NO_OPTION = new Integer(JOptionPane.NO_OPTION);
    /** Return value if CANCEL is chosen. */
    public static final Object CANCEL_OPTION = new Integer(JOptionPane.CANCEL_OPTION);
    /** Return value if OK is chosen. */
    public static final Object OK_OPTION = new Integer(JOptionPane.OK_OPTION);
    /** Return value if user closes the window without pressing any button. */
    public static final Object CLOSED_OPTION = new Integer(JOptionPane.CLOSED_OPTION);

    //
    // Option types
    //
    /** Option type used by default. */
    public static final int DEFAULT_OPTION = JOptionPane.DEFAULT_OPTION;
    /** Option type used for negatable confirmations. */
    public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
    /** Option type used for negatable and cancellable confirmations. */
    public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
    /** Option type used for cancellable confirmations. */
    public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;

    //
    // Message types
    //
    /** Message type for error messages. */
    public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
    /** Message type for information messages. */
    public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
    /** Message type for warning messages. */
    public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
    /** Message type for questions. */
    public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
    /** Plain message type using no icon. */
    public static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

    /** Maximum text width to which the text is wrapped */
    private static final int MAXIMUM_TEXT_WIDTH = 100;

    /**
    * Creates a new notify descriptor with specified information to report.
    *
    * If <code>optionType</code> is {@link #YES_NO_OPTION} or {@link #YES_NO_CANCEL_OPTION}
    * and the <code>options</code> parameter is <code>null</code>, then the options are
    * supplied by the look and feel.
    *
    * The <code>messageType</code> parameter is primarily used to supply a
    * default icon from the look and feel.
    *
    * @param message the object to display
    * @param title the title string for the dialog
    * @param optionType indicates which options are available
    * @param messageType indicates what type of message should be displayed
    * @param options an array of objects indicating the possible choices
    * @param initialValue the object that represents the default value
    *
    * @see #getMessage
    * @see #getMessageType
    * @see #getOptions
    * @see #getOptionType
    * @see #getValue
    */
    public NotifyDescriptor(Object message,
                            String title,
                            int optionType,
                            int messageType,
                            Object[] options,
                            Object initialValue)
    {
        this.message     = message;
        this.messageType = messageType;
        this.options     = options;
        this.optionType  = optionType;
        this.title       = title;
        this.value       = initialValue;
    }

    /** Method that is called before a value is returned from any of
     * getter methods in this object.
     *
     * Allows subclasses to do some additional initialization actions.
     */
    protected void initialize () {
    }
    
    /** Checks for initialization.
     */
    final void getterCalled () {
        if (changeSupport != null) {
            return;
        }
        
        boolean init = false;
        synchronized (this) {
            if (changeSupport == null) {
                changeSupport = new java.beans.PropertyChangeSupport(this);
                init = true;
            }
        }
        
        if (init) {
            initialize ();
        }
    }

    //
    // Getters/setters for properties.
    //

    /**
    * Return true if OK button is valid (enabled), otherwise return false.
    * @see #setValid
    *
    * @return validity status of OK button.
    */
    public final boolean isValid() {
        getterCalled ();
        return valid;
    }
    
    /**
    * Set validity of OK button.
    * @see #isValid
    */
    public final void setValid(boolean newValid) {
        boolean oldValid = valid;
        valid = newValid;
        firePropertyChange(PROP_VALID,
                           oldValid ? Boolean.TRUE : Boolean.FALSE,
                           newValid ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /**
    * Define a descriptive message to be reported.  In the most common
    * usage, the message is just a <code>String</code>.  However, the type
    * of this parameter is actually <code>Object</code>.  Its interpretation depends on
    * its type:
    * <dl compact>
    * <dt><code>Object[]</code><dd> A recursively interpreted series of messages.
    * <dt>{@link Component}<dd> The <code>Component</code> is displayed in the dialog.
    * <dt>{@link javax.swing.Icon}<dd> The <code>Icon</code> is wrapped in a {@link JLabel} and displayed in the dialog.
    * <dt>anything else<dd> The {@link Object#toString string representation} of the object.
    * </dl>
    *
    * @param newMessage the <code>Object</code> to report
    * @see #getMessage
    */
    public void setMessage(Object newMessage) {
        Object oldMessage = message;
        message = newMessage;
        firePropertyChange(PROP_MESSAGE, oldMessage, newMessage);
    }

    /**
    * Get the message object.
    * @see #setMessage
    *
    * @return the <code>Object</code> that is to be reported
    */
    public Object getMessage() {
        getterCalled ();
        return message;
    }

    /**
    * Define the style of the message.  The look and feel manager may lay out
    * the dialog differently depending on this value, and will often provide
    * a default icon.  The possible values are:
    * <ul>
    * <li>{@link #ERROR_MESSAGE}
    * <li>{@link #INFORMATION_MESSAGE}
    * <li>{@link #WARNING_MESSAGE}
    * <li>{@link #QUESTION_MESSAGE}
    * <li>{@link #PLAIN_MESSAGE}
    * </ul>
    *
    * @param newType the kind of message
    *
    * @see #getMessageType
    */
    public void setMessageType(int newType) {
        if (newType != ERROR_MESSAGE && newType != INFORMATION_MESSAGE &&
                newType != WARNING_MESSAGE && newType != QUESTION_MESSAGE && newType != PLAIN_MESSAGE)
            throw new IllegalArgumentException(
                "Message type must be one of the following:" // NOI18N
                + " ERROR_MESSAGE, INFORMATION_MESSAGE," // NOI18N
                + " WARNING_MESSAGE, QUESTION_MESSAGE or PLAIN_MESSAGE." // NOI18N
            );

        int oldType = messageType;
        messageType = newType;
        firePropertyChange(PROP_MESSAGE_TYPE, new Integer(oldType), new Integer(messageType));
    }

    /**
    * Get the message type.
    *
    * @return the message type
    *
    * @see #setMessageType
    */
    public int getMessageType() {
        getterCalled ();
        return messageType;
    }

    /**
    * Define the set of options.  The option type is used by the look and
    * feel to determine what options to show (unless explicit options are supplied):
    * <ul>
    * <li>{@link #DEFAULT_OPTION}
    * <li>{@link #YES_NO_OPTION}
    * <li>{@link #YES_NO_CANCEL_OPTION}
    * <li>{@link #OK_CANCEL_OPTION}
    * </ul>
    *
    * @param newType the options the look and feel is to display
    *
    * @see #getOptionType
    * @see #setOptions
    */
    public void setOptionType(int newType) {
        if (newType != DEFAULT_OPTION && newType != YES_NO_OPTION &&
                newType != YES_NO_CANCEL_OPTION && newType != OK_CANCEL_OPTION)
            throw new IllegalArgumentException(
                "Option type must be one of the following:" // NOI18N
                + " DEFAULT_OPTION, YES_NO_OPTION," // NOI18N
                + " YES_NO_CANCEL_OPTION or OK_CANCEL_OPTION." // NOI18N
            );

        int oldType = optionType;
        optionType = newType;
        firePropertyChange(PROP_OPTION_TYPE, new Integer(oldType), new Integer(optionType));
    }

    /**
    * Get the type of options that are to be displayed.
    *
    * @return the option type
    *
    * @see #setOptionType
    */
    public int getOptionType() {
        getterCalled ();
        return optionType;
    }

    /**
    * Define an explicit description of the set of user-selectable options.
    * The usual value for the options parameter is an array of
    * <code>String</code>s.  But the parameter type is an array of <code>Object</code>s.  Its
    * interpretation depends on its type:
    * <dl compact>
    * <dt>{@link Component}<dd>The component is added to the button row directly.
    * <dt>{@link javax.swing.Icon}<dd>A {@link javax.swing.JButton} is created with this icon as its label.
    * <dt>anything else<dd>The <code>Object</code> is {@link Object#toString converted} to a string and the result is used to
    *     label a <code>JButton</code>.
    * </dl>
    *
    * @param newOptions an array of user-selectable options
    *
    * @see #getOptions
    */
    public void setOptions(Object[] newOptions) {
        Object[] oldOptions = options;
        options = newOptions;
        firePropertyChange(PROP_OPTIONS, oldOptions, newOptions);
    }

    /**
    * Get the explicit choices the user can make.
    * @return the array of <code>Object</code>s that give the user's choices
    *
    * @see #setOptions
    */
    public Object[] getOptions() {
        getterCalled ();
        if (options != null) {
            return (Object[])options.clone ();
        }
        return options;
    }

    /**
    * Define an explicit description of the set of additional user-selectable options.
    * Additional options are supposed to be used for help button, etc.
    * <P>
    * The usual value for the options parameter is an array of
    * <code>String</code>s.  But the parameter type is an array of <code>Object</code>s.  Its
    * interpretation depends on its type:
    * <dl compact>
    * <dt>{@link Component}<dd>The component is added to the button row directly.
    * <dt>{@link javax.swing.Icon}<dd>A {@link javax.swing.JButton} is created with this icon as its label.
    * <dt>anything else<dd>The <code>Object</code> is {@link Object#toString converted} to a string and the result is used to
    *     label a <code>JButton</code>.
    * </dl>
    *
    * @param newOptions an array of user-selectable options
    *
    * @see #getOptions
    */
    public void setAdditionalOptions(Object[] newOptions) {
        Object[] oldOptions = adOptions;
        adOptions = newOptions;
        firePropertyChange(PROP_OPTIONS, oldOptions, newOptions);
    }

    /**
    * Get the explicit additional choices the user can make.
    * @return the array of <code>Object</code>s that give the user's choices
    *
    * @see #setOptions
    */
    public Object[] getAdditionalOptions() {
        getterCalled ();
        if (adOptions != null) {
            return (Object[])adOptions.clone ();
        }
        return null;
    }

    /**
    * Set the value the user has chosen.
    * You probably do not want to call this yourself, of course.
    *
    * @param newValue the chosen value
    *
    * @see #getValue
    */
    public void setValue(Object newValue) {
        Object oldValue = value;
        value = newValue;
        firePropertyChange(PROP_VALUE, oldValue, newValue);
    }

    /**
    * Get the value the user has selected.
    *
    * @return an <code>Object</code> indicating the option selected by the user
    *
    * @see #setValue
    */
    public Object getValue() {
        getterCalled ();
        return value;
    }

    /**
    * Set the title string for this report description.
    *
    * @param newTitle the title of this description
    *
    * @see #getTitle
    */
    public void setTitle(String newTitle) {
        Object oldTitle = title;
        title = newTitle;
        firePropertyChange(PROP_TITLE, oldTitle, newTitle);
    }


    /**
    * Get the title string for this report description.
    *
    * @return the title of this description
    *
    * @see #setTitle
    */
    public String getTitle() {
        getterCalled ();
        return title;
    }

    /**
    * Define a detail message to be reported.  In the most common usage,
    * this message is just a <code>String</code>.  However, the type of this
    * parameter is actually <code>Object</code>.  Its interpretation depends on its type:
    * <dl compact>
    * <dt><code>Object[]</code><dd> A recursively interpreted series of messages.
    * <dt><code>Throwable</code><dd> A stack trace is displayed.
    * <dt>anything else<dd> The {@link Object#toString string representation} of the object is used.
    * </dl>
    *
    * @param newDetail the detail object of this description
    *
    * @see #getDetail
    *
    public void setDetail(Object newDetail) {
      Object oldDetail = detail;
      detail = newDetail;
      firePropertyChange(PROP_DETAIL, oldDetail, newDetail);
}

    /**
    * Get the detail object for this description.
    *
    * @return details of this description
    *
    * @see #setTitle
    *
    public Object getDetail() {
      return detail;
      }
      */

    //
    // Support for reporting bound property changes.
    //

    /**
    * Add a {@link PropertyChangeListener} to the listener list.
    *
    * @param listener  the <code>PropertyChangeListener</code> to be added
    */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getterCalled ();
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
    * Remove a {@link PropertyChangeListener} from the listener list.
    *
    * @param listener  the <code>PropertyChangeListener</code> to be removed
    */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
    * Fire a {@link PropertyChangeEvent} to each listener.
    *
    * @param propertyName the programmatic name of the property that was changed
    * @param oldValue the old value of the property
    * @param newValue the new value of the property
    */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
    * Get the title to use for the indicated type.
    * @param messageType the type of message
    * @return the title to use
    */
    protected static String getTitleForType(int messageType) {
        switch(messageType) {
        case ERROR_MESSAGE:
            return NbBundle.getMessage (NotifyDescriptor.class, "NTF_ErrorTitle");
        case WARNING_MESSAGE:
            return NbBundle.getMessage (NotifyDescriptor.class, "NTF_WarningTitle");
        case QUESTION_MESSAGE:
            return NbBundle.getMessage (NotifyDescriptor.class, "NTF_QuestionTitle");
        case INFORMATION_MESSAGE:
            return NbBundle.getMessage (NotifyDescriptor.class, "NTF_InformationTitle");
        case PLAIN_MESSAGE:
            return NbBundle.getMessage (NotifyDescriptor.class, "NTF_PlainTitle");
        }
        return ""; // NOI18N
    }

    /**
    * Provides information about the results of a command.  Offers
    * no user choices; the user can only acknowledge the message.
    */
    public static class Message extends NotifyDescriptor
    {
        /**
        * Create an informational report about the results of a command.
        *
        * @param message the message object
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Message(Object message) {
            this(message, INFORMATION_MESSAGE);
        }

        /**
        * Create a report about the results of a command.
        *
        * @param message the message object
        * @param messageType the type of message to be displayed
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Message(Object message, int messageType) {
            super(
                message,
                NotifyDescriptor.getTitleForType(messageType),
                DEFAULT_OPTION,
                messageType,
                new Object[] { OK_OPTION },
                OK_OPTION
            );
        }
    }

    /**
    * Provides a description of a possible action and requests confirmation from the user before proceeding.
    * This should be used to alert the user to a condition
    * or situation that requires the user's decision before proceeding, such
    * as an impending action with potentially destructive or irreversible
    * consequences.  It is conventionally in the form of a question: for example,
    * "Save changes to TestForm?"
    */
    public static class Confirmation extends NotifyDescriptor
    {
        /**
        * Create a yes/no/cancel question with default title.
        *
        * @param message the message object
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Confirmation(Object message) {
            this(message, YES_NO_CANCEL_OPTION);
        }

        /**
        * Create a yes/no/cancel question.
        *
        * @param message the message object
        * @param title the dialog title
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Confirmation(Object message, String title) {
            this(message, title, YES_NO_CANCEL_OPTION);
        }

        /**
        * Create a question with default title.
        *
        * @param message the message object
        * @param optionType the type of options to display to the user
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Confirmation(Object message, int optionType) {
            this(message, optionType, QUESTION_MESSAGE);
        }

        /**
        * Create a question.
        *
        * @param message the message object
        * @param title the dialog title
        * @param optionType the type of options to display to the user
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Confirmation(Object message, String title, int optionType) {
            this(message, title, optionType, QUESTION_MESSAGE);
        }

        /**
        * Create a confirmation with default title.
        *
        * @param message the message object
        * @param optionType the type of options to display to the user
        * @param messageType the type of message to use
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Confirmation(Object message, int optionType, int messageType) {
            super(
                message,
                NotifyDescriptor.getTitleForType(messageType),
                optionType,
                messageType,
                optionType == DEFAULT_OPTION ? new Object[] { OK_OPTION } : null,
                OK_OPTION
            );
        }

        /**
        * Create a confirmation.
        *
        * @param message the message object
        * @param title the dialog title
        * @param optionType the type of options to display to the user
        * @param messageType the type of message to use
        * @see NotifyDescriptor#NotifyDescriptor
        */
        public Confirmation(Object message, String title, int optionType, int messageType) {
            super(
                message, title, optionType, messageType,
                optionType == DEFAULT_OPTION ? new Object[] { OK_OPTION } : null,
                OK_OPTION
            );
        }
    }

    /**
    * Provides a description of an exception that occurred during
    * execution of the IDE.
    * @deprecated Better to use {@link ErrorManager#notify}.
    */
    public static final class Exception extends Confirmation
    {
        static final long serialVersionUID =-3387516993124229948L;
        /**
        * Create an exception report with default message.
        *
        * @param detail the detail object
        */
        public Exception(Throwable detail) {
            this(detail, detail.getMessage());
            // handle InvocationTargetExceptions
            if (detail instanceof InvocationTargetException) {
                Throwable target = ((InvocationTargetException)detail).getTargetException();
                this.setMessage (target);
                if (this.getMessage() == null || "".equals(this.getMessage())) { // NOI18N
                    String msg = target.getMessage();
                    msg = org.openide.util.Utilities.wrapString (msg, MAXIMUM_TEXT_WIDTH, java.text.BreakIterator.getCharacterInstance(), false);
                    this.setMessage(msg);
                }
            }
            // emphasize user-non-friendly exceptions
            if (this.getMessage() == null || "".equals(this.getMessage())) { // NOI18N
                this.setMessage(NbBundle.getMessage(NotifyDescriptor.class,
		    "NTF_ExceptionalException", detail.getClass().getName()));
                this.setTitle(NbBundle.getMessage (NotifyDescriptor.class, "NTF_ExceptionalExceptionTitle"));
            }
        }

        /**
        * Create an exception report.
        *
        * @param detail the detail object
        * @param message the message object
        */
        public Exception(Throwable detail, Object message) {
            super(message, DEFAULT_OPTION, ERROR_MESSAGE);
            // customize descriptor
            //      this.setDetail(detail);
            this.setTitle(NbBundle.getMessage (NotifyDescriptor.class, "NTF_ExceptionTitle"));
        }
    }

    /** Notification providing for a line of text input.
    * @author Dafe Simonek
    */
    public static class InputLine extends NotifyDescriptor {
        /**
        * The text field used to enter the input.
        */
        protected JTextField textField;

        /** Construct dialog with the specified title and label text.
        * @param text label text
        * @param title title of the dialog
        */
        public InputLine (final String text, final String title) {
            this(text, title, OK_CANCEL_OPTION, PLAIN_MESSAGE);
        }

        /** Construct dialog with the specified title, label text, option and
        * message types.
        * @param text label text
        * @param title title of the dialog
        * @param optionType option type (ok, cancel, ...)
        * @param messageType message type (question, ...)
        */
        public InputLine (final String text, final String title, final int optionType,
                          final int messageType) {
            super(null, title, optionType, messageType, null, null);
            super.setMessage(createDesign(text));
        }

        /**
        * Get the text which the user typed into the input line.
        * @return the text entered by the user
        */
        public String getInputText () {
            return textField.getText ();
        }

        /**
        * Set the text on the input line.
        * @param text the new text
        */
        public void setInputText (final String text) {
            textField.setText(text);
            textField.selectAll ();
        }

        /** Make a component representing the input line.
        * @param text a label for the input line
        * @return the component
        */
        protected Component createDesign (final String text) {
            int index;
            char mnemonic = 0;
            String txt;
            JPanel panel = new JPanel();
            if(text != null && (index = text.indexOf('&')) >= 0 && index + 1 < text.length()) {
                mnemonic = text.charAt(index + 1);
                txt = text.substring(0, index) + text.substring(index + 1);
            } else {
                txt = text;
            }
            JLabel textLabel = new JLabel(txt);
            if(txt != null && txt.length() > 0) {
                textLabel.setDisplayedMnemonic(mnemonic == 0 ? text.charAt(0) : mnemonic);
            }
            textLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
            panel.setLayout(new BorderLayout());
            panel.setBorder(new EmptyBorder(11, 12, 1, 11));
            panel.add("West", textLabel); // NOI18N
            panel.add("Center", textField = new JTextField(25)); // NOI18N
            textLabel.setLabelFor(textField);
            textField.setBorder(new CompoundBorder(textField.getBorder(), new EmptyBorder(2, 0, 2, 0)));
            textField.requestFocus();

            javax.swing.KeyStroke enter = javax.swing.KeyStroke.getKeyStroke(
                                              java.awt.event.KeyEvent.VK_ENTER, 0
                                          );
            javax.swing.text.Keymap map = textField.getKeymap ();

            map.removeKeyStrokeBinding (enter);

            /*
                  
                  textField.addActionListener (new java.awt.event.ActionListener () {
                      public void actionPerformed (java.awt.event.ActionEvent evt) {
            System.out.println("action: " + evt);            
                        InputLine.this.setValue (OK_OPTION);
                      }
                    }
                  );
            */

            panel.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(NotifyDescriptor.class, "ACSD_InputPanel"));
            textField.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(NotifyDescriptor.class, "ACSD_InputField"));

            return panel;
        }

    } // end of InputLine

}
