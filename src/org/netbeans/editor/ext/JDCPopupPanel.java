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

package org.netbeans.editor.ext;

import org.netbeans.editor.*;

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *  Invisible panel that contains code completion panel and javadoc panel,
 *  computes preferred size and provides access to these both components.
 *
 *  @author  Martin Roskanin
 *  @since   03/2002
 */
public class JDCPopupPanel extends JPanel implements PropertyChangeListener, SettingsChangeListener {
    
    private ExtCompletionPane completion;
    
    private ExtEditorUI extEditorUI;
    private FocusListener focusL;
    private List keyActionPairsList;
    
    // Completion can create documentation pane
    private Completion documentationProvider;
    private JavaDocPane javadoc;
    
    // gap between javadoc and code completion window
    private static final int WINDOW_GAP = 1;
    
    private static final String POPUP_HIDE = "jdc-popup-hide"; //NOI18N
    
    private static final String COMPLETION_UP = "completion-up"; //NOI18N
    private static final String COMPLETION_DOWN = "completion-down"; //NOI18N
    private static final String COMPLETION_PGUP = "completion-pgup"; //NOI18N
    private static final String COMPLETION_PGDN = "completion-pgdn"; //NOI18N
    private static final String COMPLETION_BEGIN = "completion-begin"; //NOI18N
    private static final String COMPLETION_END = "completion-end"; //NOI18N
    
    private static final String JAVADOC_UP = "javadoc-up"; //NOI18N
    private static final String JAVADOC_DOWN = "javadoc-down"; //NOI18N
    private static final String JAVADOC_PGUP = "javadoc-pgup"; //NOI18N
    private static final String JAVADOC_PGDN = "javadoc-pgdn"; //NOI18N
    private static final String JAVADOC_BEGIN = "javadoc-begin"; //NOI18N
    private static final String JAVADOC_END = "javadoc-end"; //NOI18N
    private static final String JAVADOC_LEFT = "javadoc-left"; //NOI18N
    private static final String JAVADOC_RIGHT = "javadoc-right"; //NOI18N
    private static final String JAVADOC_BACK = "javadoc-back"; //NOI18N
    private static final String JAVADOC_FORWARD = "javadoc-forward"; //NOI18N    
    private static final String JAVADOC_OPEN_IN_BROWSER = "javadoc-open-in-browser"; //NOI18N    
    private static final String JAVADOC_OPEN_SOURCE = "javadoc-open-source"; //NOI18N    

    private static final int ACTION_POPUP_HIDE = 1;
    
    private static final int ACTION_COMPLETION_UP = 2;
    private static final int ACTION_COMPLETION_DOWN = 3;
    private static final int ACTION_COMPLETION_PGUP = 4;
    private static final int ACTION_COMPLETION_PGDN = 5;
    private static final int ACTION_COMPLETION_BEGIN = 6;
    private static final int ACTION_COMPLETION_END = 7;
     
    private static final int ACTION_JAVADOC_UP = 8;
    private static final int ACTION_JAVADOC_DOWN = 9;
    private static final int ACTION_JAVADOC_PGUP = 10;
    private static final int ACTION_JAVADOC_PGDN = 11;
    private static final int ACTION_JAVADOC_BEGIN = 12;
    private static final int ACTION_JAVADOC_END = 13;
    private static final int ACTION_JAVADOC_LEFT = 14;
    private static final int ACTION_JAVADOC_RIGHT = 15;
    private static final int ACTION_JAVADOC_BACK = 16;
    private static final int ACTION_JAVADOC_FORWARD = 17;
    private static final int ACTION_JAVADOC_OPEN_IN_BROWSER = 18;
    private static final int ACTION_JAVADOC_OPEN_SOURCE = 19;

    
    /** Creates a new instance of JDCPopupPanel */
    public JDCPopupPanel(ExtEditorUI extEditorUI, ExtCompletionPane completion, Completion documentationProvider) {
        super();
        this.completion = completion;
        this.documentationProvider = documentationProvider;
        this.extEditorUI = extEditorUI;
        this.keyActionPairsList = new ArrayList();
        
        setLayout(null);
        setOpaque(false); // make panel background invisible
        
        focusL = new FocusAdapter() {
            public void focusLost(FocusEvent evt) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if (isVisible()) {
                            JTextComponent component = JDCPopupPanel.this.extEditorUI.getComponent();
                            if (component != null) {
                                java.awt.Window w = SwingUtilities.windowForComponent(
                                component);
                                Component focusOwner = (w == null) ?
                                null : w.getFocusOwner();
                                
                                if (focusOwner == null) {
                                    setVisible(false);
                                }
                                
                                boolean docO = javadoc != null && focusOwner == JDCPopupPanel.this.getJavaDocView();
                                boolean docA = javadoc != null && JDCPopupPanel.this.getJavaDocPane().getComponent().isAncestorOf(focusOwner);
                                boolean comO = focusOwner == JDCPopupPanel.this.getCompletionView();
                                if (docO || comO || docA) {
                                    component.requestFocus();
                                }else if ( focusOwner != component ) {
                                    setVisible(false); // completion, javadoc and component don't own the focus
                                }
                            }
                        }
                    }
                } );
            }
        };
        
        
        synchronized (extEditorUI.getComponentLock()) {
            // if component already installed in ExtEditorUI simulate installation
            JTextComponent component = extEditorUI.getComponent();
            if (component != null) {
                propertyChange(new PropertyChangeEvent(extEditorUI,
                ExtEditorUI.COMPONENT_PROPERTY, null, component));
            }
            extEditorUI.addPropertyChangeListener(this);
        }
        
        super.setVisible(false);
        
        Settings.addSettingsChangeListener(this);
    }

    /** Returns completion pane */
    public ExtCompletionPane getCompletionPane(){
        return completion;
    }
    
    private void cancelJavaDocTask(){
/*        CompletionJavaDoc cjd = extEditorUI.getCompletionJavaDoc();
        if (cjd!=null){
            cjd.cancelPerformingThread();
        }
        */
    }

    private JavaDocView getJavaDocView(){
/*        CompletionJavaDoc cjd = extEditorUI.getCompletionJavaDoc();
        if (cjd!=null){
            return cjd.getJavaDocView();
        }
        */
        return null;
    }

    private CompletionView getCompletionView(){
        Completion c = extEditorUI.getCompletion();
        if (c!=null){
            return c.getView();
        }
        return null;
    }

    /** Returns javadoc pane */
    public JavaDocPane getJavaDocPane(){
        if (javadoc == null) {
            javadoc = documentationProvider.getJavaDocPane();
        }
        return javadoc;
    }
    
    /** Returns minimum size that this component can occupy. Because javadoc can be omitted, 
     *  the minimum size is the minimum size of code completion */
    public Dimension getMinimumSize(){
        return completion.getComponent().getMinimumSize();
    }
    
    /** Returns the maximum size of this component. Maximum size is computed from
     *  javadoc's and completion's maximum size */
    public Dimension getMaximumSize(){
        Dimension compDim = completion.getComponent().getMaximumSize();
        Dimension javadocDim = new Dimension();
        if (javadoc != null) {
            javadocDim = javadoc.getComponent().getMaximumSize();
        }
        return new Dimension(Math.max(compDim.width,javadocDim.width), compDim.height+javadocDim.height);
    }

    /** Returns the preferred size of this component. Preferred size is computed from
     *  javadoc's and completion's preferred size trimmed to fit maximum size */
    public Dimension getPreferredSize(){
        Dimension ret = new Dimension();
        Dimension compPref = completion.getComponent().getPreferredSize();
        Dimension compMax = completion.getComponent().getMaximumSize();
        
        Dimension javadocPref = new Dimension();
        Dimension javadocMax = new Dimension();
        if (javadoc != null) {
            javadocPref = javadoc.getComponent().getPreferredSize();
            javadocMax = javadoc.getComponent().getMaximumSize();
        }
        
        ret.width = Math.min(compPref.width, compMax.width) + Math.min(javadocPref.width, javadocMax.width);
        ret.height = Math.min(compPref.height, compMax.height) + Math.min(javadocPref.height, javadocMax.height);
        
        return ret;
    }
    
    /** Sets completion pane visiblility */
    public void setCompletionVisible(boolean visible){
        completion.setVisible(visible);
        setVisible(visible);
    }
    
    /** Sets javadoc pane visibility */
    public void setJavaDocVisible(boolean visible){
        getJavaDocPane().getComponent().setVisible(visible);
        if (visible) {
            extEditorUI.getPopupManager().install(this);
            setVisible(visible);
        }
        if (!getCompletionPane().isVisible()){
            setVisible(visible);
        }
    }
    
    /** Sets this popup comonent visibility */
    public void setVisible(boolean visible){
        if (visible == true){
            extEditorUI.getPopupManager().install(this);
        }
        boolean docv = javadoc != null && javadoc.getComponent().isVisible();
        if (visible && (docv || completion.isVisible())){
            super.setVisible(visible);
        }
        if (!visible){
            cancelJavaDocTask();
            Completion c = extEditorUI.getCompletion();
            if (c!=null){
                c.completionCancel();
            }
            if (javadoc != null) {
                javadoc.getComponent().setVisible(visible);
            }
            completion.getComponent().setVisible(visible);
            super.setVisible(visible);
        }
    }
    
    /** Setting size of popup panel. The height and size is computed to fit the best place on the screen */
    public void setSize(int width, int height){
        PopupManager.Placement placement = (PopupManager.Placement)getClientProperty(
            PopupManager.Placement.class);
        
        Dimension completionMinSize = completion.getComponent().getMinimumSize();
        if (completionMinSize.height > height) { // cannot fit
            putClientProperty(PopupManager.Placement.class, null);
        }

        // first we will set size to completion and then if there will be space for javadoc we will display it.
        completion.getComponent().setSize(width, height);
        
        Dimension javaDocMinSize = new Dimension();
        if (javadoc != null) {
            javaDocMinSize = javadoc.getComponent().getMinimumSize();
        }
        Dimension completionMaxSize = completion.getComponent().getMaximumSize();
        
        Rectangle completionBounds = new Rectangle(completion.getComponent().getSize());
        Rectangle javadocBounds = new Rectangle();
        if (javadoc != null) {
            javadocBounds = new Rectangle(javadoc.getComponent().getMaximumSize());
        }

        boolean showJavaDoc = true;
        if (javadoc == null || javadoc.getComponent().isVisible() == false) {
            showJavaDoc = false;
        }
        
/*        CompletionJavaDoc completionJavaDoc = extEditorUI.getCompletionJavaDoc();
        if (completionJavaDoc!=null){
            if(completionJavaDoc.autoPopup()){
                showJavaDoc = javadoc != null;
            }
        }
  */
        boolean showCompletion = getCompletionPane().isVisible();
        
        if (!showCompletion){
            completionMinSize.height = 0;
            completionMinSize.width = 0;
            completionBounds = new Rectangle();
        }else{
            completionBounds.width = Math.min(completionMaxSize.width,completionBounds.width);
            completionBounds.height = Math.min(completionMaxSize.height,completionBounds.height);
        }
        
        // do not show javaDoc. There is no space available
        if ((javaDocMinSize.height+completionMinSize.height)>height) showJavaDoc = false;
        
        if (showJavaDoc) {
            
            if ((completionBounds.height + javadocBounds.height) > height ){
                // javadocBounds.height should be resized
                completionBounds.height = Math.max(Math.min((int)(height/2),completionBounds.height), completionMinSize.height);
                completionBounds.height = Math.min(130,completionBounds.height); // [PENDING] - maybe this should be in options
                javadocBounds.height = Math.min((height - completionBounds.height - WINDOW_GAP ), javadocBounds.height);
            }
            
            if (placement == PopupManager.Below) {
                completionBounds.y = 0;
                javadocBounds.y = completionBounds.height + WINDOW_GAP;
            }else{
                completionBounds.y = javadocBounds.height + WINDOW_GAP;
                javadocBounds.y = 0;
            }
            
        }
        
        //width algorhitm
        if (width < javaDocMinSize.width){
            showJavaDoc = false;
        }else{
            // going to compute the width of both windows
            javadocBounds.width = Math.min(width,javadocBounds.width);
            
            JTextComponent component = extEditorUI.getComponent();
            Rectangle extBounds = extEditorUI.getExtentBounds();
            Rectangle caretRect = (Rectangle)component.getCaret();
            
            completionBounds.x = ((width - javadocBounds.width/2) < (caretRect.x - extBounds.x)) ?
                javadocBounds.width - completionBounds.width : 0;
        }
        
        
        completion.setVisible(false);
        
        remove(completion.getComponent());
        if (javadoc != null) {
            remove(javadoc.getComponent());
        }
        
        if (showJavaDoc) {
            if (showCompletion){
                completion.getComponent().setBounds(completionBounds);
                getJavaDocPane().getComponent().setBounds(javadocBounds);
                super.setBounds(0,0,Math.max(completionBounds.width,javadocBounds.width),completionBounds.height+javadocBounds.height+WINDOW_GAP);
                add(completion.getComponent());
                add(getJavaDocPane().getComponent());
                completion.setVisible(true);
            }else{
                javadocBounds.x = 0; javadocBounds.y = 0;
                getJavaDocPane().getComponent().setBounds(javadocBounds);
                super.setBounds(0,0,javadocBounds.width,javadocBounds.height);
                add(getJavaDocPane().getComponent());
            }
        }else{
            completionBounds.x = 0; completionBounds.y = 0;
            completion.getComponent().setBounds(completionBounds);
            super.setBounds(0,0,completionBounds.width,completionBounds.height+WINDOW_GAP);
            add(completion.getComponent());
            completion.setVisible(true);
        }
    }
    
    public void setSize(Dimension d){
        setSize(d.width, d.height);
    }
    
    /** Refreshes the code completion content */
    public void refresh(){
        setVisible(true);
    }
    
    private void performJavaDocAction(KeyStroke ks){
        ActionListener act = getJavaDocPane().getJavadocDisplayComponent().getActionForKeyStroke(ks);
        if (act!=null){
            act.actionPerformed(new ActionEvent(getJavaDocPane().getJavadocDisplayComponent(), ActionEvent.ACTION_PERFORMED, "")); //NOI18N
            getJavaDocPane().getJavadocDisplayComponent().repaint();
        }
    }
    
    /** Attempt to find the editor keystroke for the given editor action. */
    private KeyStroke[] findEditorKeys(String editorActionName, KeyStroke defaultKey) {
        // This method is implemented due to the issue
        // #25715 - Attempt to search keymap for the keybinding that logically corresponds to the action
        KeyStroke[] ret = new KeyStroke[] { defaultKey };
        if (editorActionName != null && extEditorUI != null) {
            JTextComponent component = extEditorUI.getComponent();
            if (component != null) {
                TextUI ui = component.getUI();
                Keymap km = component.getKeymap();
                if (ui != null && km != null) {
                    EditorKit kit = ui.getEditorKit(component);
                    if (kit instanceof BaseKit) {
                        Action a = ((BaseKit)kit).getActionByName(editorActionName);
                        if (a != null) {
                            KeyStroke[] keys = km.getKeyStrokesForAction(a);
                            if (keys != null && keys.length > 0) {
                                ret = keys;
                            }
                        }
                    }
                }
            }
        }

        return ret;
    }
    
    private void registerKeybinding(int action, String actionName, KeyStroke stroke, String editorActionName){
        KeyStroke[] keys = findEditorKeys(editorActionName, stroke); 
        for (int i = 0; i < keys.length; i++) {
            getInputMap().put(keys[i], actionName);
            keyActionPairsList.add(actionName); // add action-name
            keyActionPairsList.add(keys[i]); // add keystroke
        }

        getActionMap().put(actionName, new JDCPopupAction(action));
    }
    
    private void unregisterKeybinding(String actionName, KeyStroke stroke) {
        getInputMap().remove(stroke);
        getActionMap().remove(actionName);
    }
    
    private void installKeybindings() {
        // Register escape key
        registerKeybinding(ACTION_POPUP_HIDE, POPUP_HIDE,
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        ExtKit.escapeAction
        );

        // Register up key
        registerKeybinding(ACTION_COMPLETION_UP, COMPLETION_UP,
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
        BaseKit.upAction
        );

        // Register down key
        registerKeybinding(ACTION_COMPLETION_DOWN, COMPLETION_DOWN,
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
        BaseKit.downAction
        );

        // Register PgDn key
        registerKeybinding(ACTION_COMPLETION_PGDN, COMPLETION_PGDN,
        KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
        BaseKit.pageDownAction
        );

        // Register PgUp key
        registerKeybinding(ACTION_COMPLETION_PGUP, COMPLETION_PGUP,
        KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
        BaseKit.pageUpAction
        );

        // Register home key
        registerKeybinding(ACTION_COMPLETION_BEGIN, COMPLETION_BEGIN,
        KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
        BaseKit.beginLineAction
        );

        // Register end key
        registerKeybinding(ACTION_COMPLETION_END, COMPLETION_END,
        KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
        BaseKit.endLineAction
        );

        // Register javadoc up key
        registerKeybinding(ACTION_JAVADOC_UP, JAVADOC_UP,
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc down key
        registerKeybinding(ACTION_JAVADOC_DOWN, JAVADOC_DOWN,
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc PgDn key
        registerKeybinding(ACTION_JAVADOC_PGDN, JAVADOC_PGDN,
        KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc PgUp key
        registerKeybinding(ACTION_JAVADOC_PGUP, JAVADOC_PGUP,
        KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc home key
        registerKeybinding(ACTION_JAVADOC_BEGIN, JAVADOC_BEGIN,
        KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc end key
        registerKeybinding(ACTION_JAVADOC_END, JAVADOC_END,
        KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc left key
        registerKeybinding(ACTION_JAVADOC_LEFT, JAVADOC_LEFT,
        KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc right key
        registerKeybinding(ACTION_JAVADOC_RIGHT, JAVADOC_RIGHT,
        KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK),
        null
        );

        // Register javadoc back key
        registerKeybinding(ACTION_JAVADOC_BACK, JAVADOC_BACK,
        KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_MASK),
        null
        );

        // Register javadoc forward key
        registerKeybinding(ACTION_JAVADOC_FORWARD, JAVADOC_FORWARD,
        KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_MASK),
        null
        );

        // Register open in external browser key
        registerKeybinding(ACTION_JAVADOC_OPEN_IN_BROWSER, JAVADOC_OPEN_IN_BROWSER,
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK),
        null
        );

        // Register open the source in editor key
        registerKeybinding(ACTION_JAVADOC_OPEN_SOURCE, JAVADOC_OPEN_SOURCE,
        KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK),
        null
        );

    }
     
    private void uninstallKeybindings() {
        for (Iterator it = keyActionPairsList.iterator(); it.hasNext();) {
            unregisterKeybinding(
                (String)it.next(), // action-name
                (KeyStroke)it.next() // keystroke
            );
        }
        keyActionPairsList.clear();
    }
                
    
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        
        if (ExtEditorUI.COMPONENT_PROPERTY.equals(propName)) {
            if (evt.getNewValue() != null) { // just installed
                JTextComponent component = extEditorUI.getComponent();
                installKeybindings();
                component.addFocusListener(focusL);
                
            } else { // just deinstalled
                JTextComponent component = (JTextComponent)evt.getOldValue();
                uninstallKeybindings();
                component.removeFocusListener(focusL);
            }
            
        }
    }
    
    public void settingsChange(SettingsChangeEvent evt) {
        // Refresh keybindings
        uninstallKeybindings();
        installKeybindings();
    }
    
    private class JDCPopupAction extends AbstractAction {
        private int action;
        
        private JDCPopupAction(int action) {
            this.action = action;
        }
        
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            switch (action) {
                case ACTION_POPUP_HIDE:
                    setVisible(false);
                    break;
                case ACTION_COMPLETION_UP:
                    if (completion.isVisible()){
                        getCompletionView().up();
                    }else{
                        performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
                    }
                    break;
                case ACTION_COMPLETION_DOWN:
                    if (completion.isVisible()){
                        getCompletionView().down();
                    }else {
                        performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
                    }
                    break;
                case ACTION_COMPLETION_PGUP:
                    if (completion.isVisible()){
                        getCompletionView().pageUp();
                    } else{
                        performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
                    }
                    break;
                case ACTION_COMPLETION_PGDN:
                    if (completion.isVisible()){
                        getCompletionView().pageDown();
                    }else{
                        performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
                    }
                    break;
                case ACTION_COMPLETION_BEGIN:
                    if (completion.isVisible()){
                        getCompletionView().begin();
                    }else{
                        performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
                    }
                    break;
                case ACTION_COMPLETION_END:
                    if (completion.isVisible()){
                        getCompletionView().end();
                    }else{
                        performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
                    }
                    break;
                case ACTION_JAVADOC_UP:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
                    break;
                case ACTION_JAVADOC_DOWN:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
                    break;
                case ACTION_JAVADOC_PGUP:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
                    break;
                case ACTION_JAVADOC_PGDN:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
                    break;
                case ACTION_JAVADOC_BEGIN:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
                    break;
                case ACTION_JAVADOC_END:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
                    break;
                case ACTION_JAVADOC_LEFT:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
                    break;
                case ACTION_JAVADOC_RIGHT:
                    performJavaDocAction(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
                    break;
                case ACTION_JAVADOC_BACK: {
/*                    CompletionJavaDoc cjd = extEditorUI.getCompletionJavaDoc();
                    if (cjd!=null){
                        cjd.backHistory();
                    }
                    */
                }
                break;
                case ACTION_JAVADOC_FORWARD: {
/*                    CompletionJavaDoc cjd = extEditorUI.getCompletionJavaDoc();
                    if (cjd!=null){
                        cjd.forwardHistory();
                    }
                    */
                }
                break;
                case ACTION_JAVADOC_OPEN_IN_BROWSER: {
/*                    CompletionJavaDoc cjd = extEditorUI.getCompletionJavaDoc();
                    if (cjd!=null && cjd.isExternalJavaDocMounted()){
                        cjd.openInExternalBrowser();
                    }
                    */
                }
                break;
                case ACTION_JAVADOC_OPEN_SOURCE: {
/*                    CompletionJavaDoc cjd = extEditorUI.getCompletionJavaDoc();
                    if (cjd!=null){
                        cjd.goToSource();
                    }
                    */
                }
                break;
            }
        }

    }



}
