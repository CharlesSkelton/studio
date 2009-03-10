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

import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.Component;
import java.awt.Cursor;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.openide.*;
import org.openide.actions.ActionManager;
import org.openide.loaders.*;
import org.openide.filesystems.*;
import org.openide.filesystems.FileSystem; // override java.io.FileSystem
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;


/** Wizard for creation of new objects from a template.
*
* @author Jaroslav Tulach, Jiri Rechtacek
*/
public class TemplateWizard extends WizardDescriptor {
    /** EA that defines the wizards description */
    private static final String EA_DESCRIPTION = "templateWizardURL"; // NOI18N
    /** EA that defines custom iterator */
    private static final String EA_ITERATOR = "templateWizardIterator"; // NOI18N
    /** EA that defines resource string to the description instead of raw URL
     * @deprecated
     */
    private static final String EA_DESC_RESOURCE = "templateWizardDescResource"; // NOI18N

    /** See org.openide.WizardDescriptor.PROP_CONTENT_SELECTED_INDEX
     */
    private static final String PROP_CONTENT_SELECTED_INDEX = "WizardPanel_contentSelectedIndex"; // NOI18N
    /** See org.openide.WizardDescriptor.PROP_CONTENT_DATA
     */
    private static final String PROP_CONTENT_DATA = "WizardPanel_contentData"; // NOI18N

    /** prefered dimmension of the panels */
    static java.awt.Dimension PREF_DIM = new java.awt.Dimension (560, 350);

    /** panel */
    private Panel templateChooser;
    /** panel */
    private Panel targetChooser;
    /** whether to show target chooser */
    private boolean showTargetChooser = true;
    
    /** Iterator for the targetChooser */
    private Iterator targetIterator;
    /** whole iterator */
    private TemplateWizardIterImpl iterator;

    /** values for wizards */
    private DataObject template;
    
    /** root of all templates */
    private DataFolder templatesFolder;

    /** class name of object to create */
    private String targetName = null;
    /** target folder*/
    private DataFolder targetDataFolder;
    /** This is true if we have already set a value to the title format */
    private boolean titleFormatSet = false;

    /** listenes on property (steps, index) changes and updates steps pane */
    private PropertyChangeListener pcl;
    
    /** Component which we are listening on for changes of steps */
    private Component lastComp;
    
    /** Creates new TemplateWizard */
    public TemplateWizard () {
        this (new TemplateWizardIterImpl ());
    }
     
    /** Constructor to be called from public default one.
    */
    private TemplateWizard (TemplateWizardIterImpl it) {
        super (it);
        
        this.iterator = it;
        
        // pass this to iterator
        iterator.initialize(this);

        putProperty("WizardPanel_autoWizardStyle", Boolean.TRUE); // NOI18N
        putProperty("WizardPanel_contentDisplayed", Boolean.TRUE); // NOI18N
        putProperty("WizardPanel_contentNumbered", Boolean.TRUE); // NOI18N
        setTitle(NbBundle.getMessage(TemplateWizard.class,"CTL_TemplateTitle")); //NOI18N
        setTitleFormat(new java.text.MessageFormat("{0}")); // NOI18N
    }

    /** Constructor
     *  for wizards that require the target chooser or template
     *  chooser panel.
     *  @param it panel iterator instance
     */
    protected TemplateWizard (TemplateWizard.Iterator it) {
        this();
        iterator.setIterator(it, false);
    }
    
    /** Initializes important settings.
     */
    protected void initialize () {
        if (iterator != null) {
            iterator.initialize(this);
        }
        super.initialize ();
    }

    /** Getter for default filesystem to create objects on.
     * @return the first non hidden filesystem or null if none
     */
    final FileSystem getEnabledSystem () {
        // search for a filesystem
        Enumeration en = Repository.getDefault().fileSystems();
        while (en.hasMoreElements()) {
            FileSystem fs = (FileSystem)en.nextElement();
            if (!fs.isHidden()) {
                // found first non hidden filesystem
                return fs;
            }
        }
        return null;
    }

    /** This is method used by TemplateWizardPanel1 to change the template
    */
    final void setTemplateImpl (DataObject obj, boolean notify) {
        DataObject old = template;
        if (template != obj) {
            template = obj;
        }
        setTitle(NbBundle.getMessage(TemplateWizard.class, "CTL_TemplateTitle2",
        	obj.getNodeDelegate().getDisplayName()));
        if (old != template) {
            Iterator it;
            if (
                obj == null ||
                (it = getIterator (obj)) == null
            ) {
                it = defaultIterator ();
            }
            this.iterator.setIterator (it, notify);
        }
    }

    /** Getter for template to create object from.
     * @return template
     */
    public DataObject getTemplate () {
        return template;
    }

    /** Sets the template. If under the Templates/
    * directory it will be selected by the dialog.
    *
    * @param obj the template to start with
    */
    public void setTemplate (DataObject obj) {
        if (obj != null) {
            setTemplateImpl (obj, true);
        }
    }
    
    /** Setter for the folder with templates. If not specified the 
     * Templates/ folder is used.
     *
     * @param folder the root folder for all templates if null the
     *    default folder is used
     */
    public void setTemplatesFolder (DataFolder folder) {
        templatesFolder = folder;
    }
    
    /** Getter for the folder with templates.
     * @return the folder with templates that should be used as root
     */
    public DataFolder getTemplatesFolder () {
        DataFolder df = templatesFolder;
        if (df == null) {
            FileObject fo = Repository.getDefault ().getDefaultFileSystem ().findResource ("/Templates"); // NOI18N
            if (fo != null && fo.isFolder ()) {
                return DataFolder.findFolder (fo);
            }
        }
        return df;
    }
        
        

    /** Getter for target folder. If the folder does not
    * exists it is created at this point.
    *
    * @return the target folder
    * @exception IOException if the possible creation of the folder fails
    */
    public DataFolder getTargetFolder () throws IOException {
        if (targetDataFolder == null) {
            // bugfix #28357, check if filesystem is null
            if (getEnabledSystem () == null) {
                throw new IOException (NbBundle.getMessage(TemplateWizard.class, "ERR_NoFilesystem"));
            }
            targetDataFolder = DataFolder.findFolder (getEnabledSystem ().getRoot ());
        }
        return targetDataFolder;
    }
    
    /** Sets the target folder.
    *
    * @param f the folder
    */
    public void setTargetFolder (DataFolder f) {
        targetDataFolder = f;
    }

    /** Getter for the name of the target template.
    * @return the name or <code>null</code> if not yet set
    */
    public String getTargetName () {
        return targetName;
    }

    /** Setter for the name of the template.
    * @param name name for the new object, or <code>null</code>
    */
    public void setTargetName (String name) {
        targetName = name;
    }

    /** Returns wizard panel that is used to choose a template.
     * @return wizard panel
     */
    public Panel templateChooser () {
        if (templateChooser == null) {
            synchronized (this) {
                if (templateChooser == null) {
                    templateChooser = createTemplateChooser ();
                }
            }
        }
        return templateChooser;
    }

    /** Returns wizard panel that that is used to choose target folder and
     * name of the template.
     * @return wizard panel
     */
    public Panel targetChooser () {
        if (targetChooser == null) {
            synchronized (this) {
                if (targetChooser == null) {
                    targetChooser = createTargetChooser ();
                }
            }
        }
        return targetChooser;
    }
    
    /** Access method to the default iterator.
    */
    final synchronized Iterator defaultIterator () {
                if (targetIterator == null) {
                    targetIterator = createDefaultIterator ();
                }
        return targetIterator;
    }
    
    /** Method that allows subclasses to provide their own panel
     * for choosing the template (the first panel).
     * 
     * @return the panel
     */
    protected Panel createTemplateChooser () {
        return new TemplateWizardPanel1 ();
    }

    /** Method that allows subclasses to second (default) panel.
     * 
     * @return the panel
     */
    protected Panel createTargetChooser () {
        return showTargetChooser ? (Panel)new TemplateWizardPanel2 () : (Panel)new NewObjectWizardPanel ();
    }
    
    /** Allows subclasses to provide their own default iterator
     * the one that will be used if not special iterator is associated
     * with selected template.
     * <P>
     * This implementation creates iterator that just shows the targetChooser
     * panel.
     *
     * @return the iterator to use as default one
     */
    protected Iterator createDefaultIterator () {
        return new DefaultIterator ();
    }
    
    /** Chooses the template and instantiates it.
    * @return set of instantiated data objects (DataObject) 
    *   or null if user canceled the dialog
    * @exception IOException I/O error
    */
    public java.util.Set instantiate () throws IOException {
        showTargetChooser = true;
        return instantiateImpl (null, null);
    }

    /** Chooses the template and instantiates it.
    *
    * @param template predefined template that should be instantiated
    * @return set of instantiated data objects (DataObject) 
    *   or null if user canceled the dialog
    * @exception IOException I/O error
    */
    public java.util.Set instantiate (DataObject template) throws IOException {
        showTargetChooser = true;
        return instantiateImpl (template, null);
    }

    /** Chooses the template and instantiates it.
    *
    * @param template predefined template that should be instantiated
    * @param targetFolder the target folder
    *
    * @return set of instantiated data objects (DataObject) 
    *   or null if user canceled the dialog
    * @exception IOException I/O error
    */
    public java.util.Set instantiate (
        DataObject template, DataFolder targetFolder
    ) throws IOException {
        showTargetChooser = false;
        return instantiateImpl (template, targetFolder);
    }

    /** Chooses the template and instantiates it.
    * @param template predefined template or nothing
    * @return set of instantiated data objects (DataObject) 
    *   or null if user canceled the dialog
    * @exception IOException I/O error
    */
    private java.util.Set instantiateImpl (
        DataObject template, DataFolder targetFolder
    ) throws IOException {

        showTargetChooser |= targetFolder == null;
        targetChooser = null;

        // Bugfix #16161
        // Message which cancelled the previous attempt to instantiate the 
        // template
        Throwable   thrownMessage = null;
        
        try {
            while (true) {
                // it should loop only in case of IllegalStateException

                // Bugfix #15458: Target folder should be set before readSettings of 
                // template wizard first panel is called.
                if (targetFolder != null) {
                    setTargetFolder (targetFolder);
                }

                if (template != null) {
                    // force new template selection 
                    this.template = null;
                    setTemplate (template);

                    // make sure that iterator is initialized
                    if (iterator != null) {
                        iterator.initialize(this);
                    }
                } else if (iterator != null) {
                    iterator.initialize(this);
                    iterator.first();
                }

                try {
                    updateState();
                    final java.awt.Dialog d = DialogDisplayer.getDefault().createDialog(this);
                    // Bugfix #16161: if there was a message to the user, notify it
                    // after the dialog has been shown on screen:
                    if (thrownMessage != null) {
                        final Throwable t = thrownMessage;
                        thrownMessage = null;
                        d.addComponentListener(new java.awt.event.ComponentAdapter() {
                            public void componentShown(java.awt.event.ComponentEvent e) {
                                if (t.getMessage() != null) {
                                    // this is only for backward compatitility (plus bugfix #15618, using errMan to log stack trace)
                                    ErrorManager.getDefault ().notify (ErrorManager.USER, t);
                                } else {
                                    // this should be used (it checks for exception
                                    // annotations and severity)
                                    ErrorManager.getDefault().notify(t);
                                }
                                d.removeComponentListener(this);
                            }
                        });
                    }
                    d.show ();
                    d.dispose();

                    if (lastComp != null) {
                        lastComp.removePropertyChangeListener(propL());
                        lastComp = null;
                    }

                    // #17341. The problem is handling ESC -> value is not
                    // set to CANCEL_OPTION for such cases.
                    Object option = getValue();
                    if(option == FINISH_OPTION || option == YES_OPTION
                        || option == OK_OPTION) {
                            
                        // show wait cursor when handling instantiate
                        showWaitCursor (); 
                        
                        return handleInstantiate();
                    } else {
                        return null;
                    }

                } catch (IllegalStateException ise) {
                    thrownMessage = ise;
                }
            }
        } finally {
            if (iterator != null) {
                iterator.uninitialize();
            }
            // set normal cursor back
            showNormalCursor ();
        }
    }
    
    private void showWaitCursor () {
        //
        // waiting times
        //
        try {
            JFrame f = (JFrame)WindowManager.getDefault ().getMainWindow ();
            Component c = f.getGlassPane ();
            c.setVisible (true);
            c.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
        } catch (NullPointerException npe) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, npe);
        }
    }
    
    private void showNormalCursor () {
        //
        // normal times
        //
        try {
            JFrame f = (JFrame)WindowManager.getDefault ().getMainWindow ();
            Component c = f.getGlassPane ();
            c.setCursor (null);
            c.setVisible (false);
        } catch (NullPointerException npe) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, npe);
        }
    }
    

    /** Overriden to be able to set own default value for the title format.
     * @param format message format
     */
    public void setTitleFormat (MessageFormat format) {
        titleFormatSet = true; // someone have set the title format
        super.setTitleFormat(format);
    }

    /** Overriden to be able to set a default value for the title format.
     * @return message format in title
     */
    public MessageFormat getTitleFormat () {
        if (!titleFormatSet) {
            // we want to call this just for the first time getTitleFormat was called
            // and not to call it when someone else called setTitleFormat
            setTitleFormat (new MessageFormat (NbBundle.getMessage(TemplateWizard.class, "CTL_TemplateTitle")));
        }
        return super.getTitleFormat();
    }

    
    /** Calls iterator's instantiate. It is called when user selects
     * a option which is not CANCEL_OPTION or CLOSED_OPTION.
     * @throws IOException if the instantiation fails
     * @return set of data objects that have been created (should contain
     * at least one)
     */
    protected java.util.Set handleInstantiate() throws IOException {
        return iterator.getIterator ().instantiate (this);
    }
    
    /** Method to attach a description to a data object.
    * It is suggested that the URL use the <code>nbresloc</code> protocol.
    * @param obj data object to attach description to
    * @param url the url with description or null if there should be
    *   no description
    * @exception IOException if I/O fails
    */
    public static void setDescription (DataObject obj, URL url) throws IOException {
        obj.getPrimaryFile().setAttribute(EA_DESCRIPTION, url);
    }

    /** Method to get a description for a data object.
    * @param obj data object to attach description to
    * @return the url with description or null
    */
    public static URL getDescription (DataObject obj) {
        URL desc = (URL)obj.getPrimaryFile().getAttribute(EA_DESCRIPTION);
        if (desc != null) return desc;
	// Backwards compatibility:
        String rsrc = (String) obj.getPrimaryFile ().getAttribute (EA_DESC_RESOURCE);
	if (rsrc != null) {
	    try {
		URL better = new URL ("nbresloc:/" + rsrc); // NOI18N
		try {
		    setDescription (obj, better);
		} catch (IOException ioe) {
		    // Oh well, just ignore.
		}
		return better;
	    } catch (MalformedURLException mfue) {
		ErrorManager.getDefault().notify(mfue);
	    }
	}
        return null;
    }

    /** Set a description for a data object by resource path rather than raw URL.
     * @deprecated Use {@link #setDescription} instead.
     * @param obj data object to set description for
     * @param rsrc a resource string, e.g. "com/foo/MyPage.html", or <code>null</code> to clear
     * @throws IOException if the attribute cannot be set
     */
    public static void setDescriptionAsResource (DataObject obj, String rsrc) throws IOException {
        if (rsrc != null && rsrc.startsWith ("/")) { // NOI18N
            ErrorManager.getDefault().log(ErrorManager.WARNING, "Warning: auto-stripping leading slash from resource path in TemplateWizard.setDescriptionAsResource: " + rsrc);
            rsrc = rsrc.substring (1);
        }
        obj.getPrimaryFile ().setAttribute (EA_DESC_RESOURCE, rsrc);
    }

    /** Get a description as a resource.
    * @param obj the data object
    * @return the resource path, or <code>null</code> if unset (incl. if only set as a raw URL)
    * @deprecated Use {@link #getDescription} instead.
    */
    public static String getDescriptionAsResource (DataObject obj) {
        return (String) obj.getPrimaryFile ().getAttribute (EA_DESC_RESOURCE);
    }

    /** Allows to attach a special Iterator to a template. This allows
    * templates to completelly control the way they are instantiated.
    * <P>
    * Better way for providing an Iterator is to return it from the
    * <code>dataobject.getCookie (TemplateWizard.Iterator.class)</code>
    * call.
    *
    * @param obj data object
    * @param iter TemplateWizard.Iterator to use for instantiation of this
    *    data object, or <code>null</code> to clear
    * @exception IOException if I/O fails
     *
    * @deprecated since 2.13 you should provide the iterator from <code>getCookie</code> method
    */
    public static void setIterator (DataObject obj, Iterator iter)
    throws IOException {
        obj.getPrimaryFile().setAttribute(EA_ITERATOR, iter);
    }

    /** Finds a custom iterator attached to a template that should
    * be used to instantiate the object. First of all it checks
    * whether there is an iterator attached by <code>setIterator</code> method, if not it asks the
    * data object for the Iterator as cookie.
    * 
    * @param obj the data object
    * @return custom iterator or null
    */
    public static Iterator getIterator (DataObject obj) {
        Iterator it = (Iterator)obj.getPrimaryFile ().getAttribute(EA_ITERATOR);
        if (it != null) {
            return it;
        }
        
        return (Iterator)obj.getCookie (Iterator.class);
    }
    
    /** Overriden to add/remove listener to/from displayed component. Also make recreation
     * of steps and content.
     */
    protected void updateState() {
        super.updateState();

        if (lastComp != null) {
            lastComp.removePropertyChangeListener(propL());
        }

        // listener
        lastComp = iterator.current().getComponent();
        lastComp.addPropertyChangeListener(propL());
        
        // compoun steps pane info
        putProperty(PROP_CONTENT_SELECTED_INDEX,
            new Integer(getContentSelectedIndex()));
        if (getContentData() != null) {
            putProperty(PROP_CONTENT_DATA, getContentData());
        }
    }

    /**
     * @return String[] content taken from first panel and delegated iterator or null if
     * delegated iterator doesn't supplied content steps name
     */
    private String[] getContentData() {
        Component first = templateChooser().getComponent();
        if (iterator.current() == templateChooser()) {
            // return first panel steps
            return (String[])((JComponent)first).getClientProperty(PROP_CONTENT_DATA);
        }
        String[] cd = null;
        Component c = iterator.current().getComponent();
        if (c instanceof JComponent) {
            // merge first panel name with delegated iterator steps
            Object property = ((JComponent)c).getClientProperty(PROP_CONTENT_DATA);
            if (property instanceof String[]) {
                String[] cont = (String[])property;
                cd = new String[cont.length + 1];
                cd[0] = ((String[])((JComponent)first).getClientProperty(PROP_CONTENT_DATA))[0];
                System.arraycopy(cont, 0, cd, 1, cont.length);
            }
        }
        return cd;
    }
    
    /** Returns selected item in content
     * @return int selected index of content
     */
    private int getContentSelectedIndex() {
        if (iterator.current() == templateChooser()) {
            return 0;
        }
        Component c = iterator.current().getComponent();
        if (c instanceof JComponent) {
            // increase supplied selected index by one (template chooser)
            Object property = ((JComponent)c).getClientProperty(PROP_CONTENT_SELECTED_INDEX);
            if ((property instanceof Integer)) {
                return ((Integer)property).intValue() + 1;
            }
        }
        return 1;
    }
    
    /** Listens on content property changes in delegated iterator. Updates Wizard
     * descriptor properties.
     */
    private PropertyChangeListener propL() {
        if (pcl == null) {
            pcl = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent ev) {
                    if (PROP_CONTENT_SELECTED_INDEX.equals(ev.getPropertyName())) {
                        putProperty(PROP_CONTENT_SELECTED_INDEX,
                            new Integer(getContentSelectedIndex()));
                    } else { 
                        if ((PROP_CONTENT_DATA.equals(ev.getPropertyName())) && (getContentData() != null)) {
                            putProperty(PROP_CONTENT_DATA, getContentData());
                        }
                    }
                }
            };
        }
        return pcl;
    }

    /** The interface for custom iterator. Enhances to WizardDescriptor.Iterator
    * by serialization and ability to instantiate the object.
    * <P>
    * All Panels provided by this iterator will receive a TemplateWizard
    * as the settings object and they are encourage to store its data by the 
    * use of <CODE>putProperty</CODE> method and read it using <code>getProperty</code>.
    * <P>
    * Implements <code>Node.Cookie</code> since version 2.13
    */
    public interface Iterator extends WizardDescriptor.Iterator,
    java.io.Serializable, org.openide.nodes.Node.Cookie {
        /** Instantiates the template using information provided by
         * the wizard.
         *
         * @return set of data objects that have been created (should contain
         *   at least one)
         * @param wiz the wizard
         * @exception IOException if the instantiation fails
         */
        public java.util.Set instantiate (TemplateWizard wiz)
            throws IOException;
        
        /** Initializes the iterator after it is constructed.
         * The iterator can for example obtain the {@link #targetChooser target chooser}
         * from the wizard if it does not wish to provide its own.
         * @param wiz template wizard that wishes to use the iterator
         */
        public void initialize(TemplateWizard wiz);
        
        /** Informs the Iterator that the TemplateWizard finished using the Iterator.
         * The main purpose of this method is to perform cleanup tasks that should
         * not be left on the garbage collector / default cleanup mechanisms.
         * @param wiz wizard which is no longer being used
         */
        public void uninitialize(TemplateWizard wiz);
    } // end of Iterator

    /** Implementation of default iterator.
    */
    private final class DefaultIterator implements Iterator {
        DefaultIterator() {}
        
        /** Name */
        public String name () {
            return ""; // NOI18N
        }

        /** Instantiates the template using informations provided by
        * the wizard.
        *
        * @param wiz the wizard
        * @return set of data objects that has been created (should contain
        *   at least one) 
        * @exception IOException if the instantiation fails
        */
        public java.util.Set instantiate(TemplateWizard wiz) throws IOException {
            String n = wiz.getTargetName ();
            DataFolder folder = wiz.getTargetFolder ();
            DataObject template = wiz.getTemplate ();
            DataObject obj = n == null ?
                             template.createFromTemplate (folder)
                             :
                             template.createFromTemplate (folder, n);

            // run default action (hopefully should be here)
            org.openide.nodes.Node node = obj.getNodeDelegate ();
            org.openide.util.actions.SystemAction sa = node.getDefaultAction ();
            if (sa != null) {
                ((ActionManager)Lookup.getDefault ().lookup (ActionManager.class)).invokeAction
                    (sa, new ActionEvent (node, ActionEvent.ACTION_PERFORMED, "")); // NOI18N
            }

            return java.util.Collections.singleton(obj);
        }
        
        /** No-op implementation.
         */
        public void initialize(TemplateWizard wiz) {
        }
        
        /** No-op implementation.
         */
        public void uninitialize(TemplateWizard wiz) {
        }
        
        /** Get the current panel.
        * @return the panel
        */
        public Panel current() {
            return targetChooser ();
        }
        
        /** Test whether there is a next panel.
        * @return <code>true</code> if so
        */
        public boolean hasNext() {
            return false;
        }
        
        /** Test whether there is a previous panel.
        * @return <code>true</code> if so
        */
        public boolean hasPrevious() {
            return false;
        }
        
        /** Move to the next panel.
        * I.e. increment its index, need not actually change any GUI itself.
        * @exception NoSuchElementException if the panel does not exist
        */
        public void nextPanel() {
            throw new java.util.NoSuchElementException ();
        }
        
        /** Move to the previous panel.
        * I.e. decrement its index, need not actually change any GUI itself.
        * @exception NoSuchElementException if the panel does not exist
        */
        public void previousPanel() {
            throw new java.util.NoSuchElementException ();
        }
        
        /** Add a listener to changes of the current panel.
        * The listener is notified when the possibility to move forward/backward changes.
        * @param l the listener to add
        */
        public void addChangeListener(javax.swing.event.ChangeListener l) {
        }
        
        /** Remove a listener to changes of the current panel.
        * @param l the listener to remove
        */
        public void removeChangeListener(javax.swing.event.ChangeListener l) {
        }
    }

    /*
      public static void main (String[] args) throws java.lang.Exception {
        TemplateWizard wiz = new TemplateWizard ();
        
        
        FileObject fo = FileSystemCapability.ALL.findResource(
          "Templates/AWTForms/Frame.java"
        );
        DataObject obj = DataObject.find (fo);

        fo = FileSystemCapability.ALL.findResource(
          "test"
        );
        DataFolder f = DataFolder.findFolder(fo);
        
        
        wiz.instantiate();
      }
    */  

}
