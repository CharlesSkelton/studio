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
import org.openide.util.RequestProcessor;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
* General Completion display formatting and services
*
* @author Miloslav Metelka
* @version 1.00
*/

public class Completion
    implements PropertyChangeListener, SettingsChangeListener, ActionListener {

        
        
    /** Editor UI supporting this completion */
    protected ExtEditorUI extEditorUI;

    /** Completion query providing query support for this completion */
    private CompletionQuery query;

    /** Last result retrieved for completion. It can become null
    * if the document was modified so the replacement position
    * would be invalid.
    */
    private CompletionQuery.Result lastResult;
    
    private boolean keyPressed = false;

    /** Completion view component displaying the completion help */
    private CompletionView view;

    /** Component (usually scroll-pane) holding the view and the title
    * and possibly other necessary components.
    */
    private ExtCompletionPane pane;
    
    private JavaDocPane javaDocPane;
    
    private JDCPopupPanel jdcPopupPanel;

    private boolean autoPopup;

    private int autoPopupDelay;

    private int refreshDelay;

    private boolean instantSubstitution;
    
    Timer timer;

    private DocumentListener docL;
    private CaretListener caretL;

    private PropertyChangeListener docChangeL;
    
    private int caretPos=-1;

    // old providers was called serialy from AWT, emulate it by RP queue
    private static RequestProcessor serializingRequestProcessor;
    
    // sample property at static initialized, but allow dynamic disabling later
    private static final String PROP_DEBUG_COMPLETION = "editor.debug.completion";  // NOI18N
    private static final boolean DEBUG_COMPLETION = Boolean.getBoolean(PROP_DEBUG_COMPLETION);
    
    // Every asynchronous task can be splitted into several subtasks.
    // The task can between subtasks using simple test determine whether
    // it was not cancelled.
    // It emulates #28475 RequestProcessor enhancement request.
    private CancelableRunnable cancellable = new CancelableRunnable() {
        public void run() {}
    };
        
    public Completion(ExtEditorUI extEditorUI) {
        this.extEditorUI = extEditorUI;

        // Initialize timer
        timer = new Timer(0, new WeakTimerListener(this)); // delay will be set later
        timer.setRepeats(false);

        
        
        // Create document listener
        class CompletionDocumentListener implements DocumentListener {
           public void insertUpdate(DocumentEvent evt) {
               trace("ENTRY insertUpdate");
               setKeyPressed(true);
               invalidateLastResult();
               refreshImpl( false );  //??? why do not we batch them by posting it?
           }

           public void removeUpdate(DocumentEvent evt) {
               trace("ENTRY removeUpdate");
               setKeyPressed(true);
               invalidateLastResult();
               refreshImpl( false );  //??? why do not we batch them by posting it?
           }

           public void changedUpdate(DocumentEvent evt) {
           }
        };        
        docL = new CompletionDocumentListener();


        class CompletionCaretListener implements CaretListener {
            public void caretUpdate( CaretEvent e ) {
                trace("ENTRY caretUpdate");
                if (!isPaneVisible()){
                    // cancel timer if caret moved
                    cancelRequestImpl();
                }else{
                    //refresh completion only if a pane is already visible
                    refreshImpl( true );
                }
            }
        };        
        caretL = new CompletionCaretListener();
       
        Settings.addSettingsChangeListener(this);

        synchronized (extEditorUI.getComponentLock()) {
            // if component already installed in ExtEditorUI simulate installation
            JTextComponent component = extEditorUI.getComponent();
            if (component != null) {
                propertyChange(new PropertyChangeEvent(extEditorUI,
                                                       ExtEditorUI.COMPONENT_PROPERTY, null, component));
            }

            extEditorUI.addPropertyChangeListener(this);
        }
    }

    public void settingsChange(SettingsChangeEvent evt) {
        Class kitClass = Utilities.getKitClass(extEditorUI.getComponent());

        if (kitClass != null) {
            autoPopup = SettingsUtil.getBoolean(kitClass,
                                                ExtSettingsNames.COMPLETION_AUTO_POPUP,
                                                ExtSettingsDefaults.defaultCompletionAutoPopup);

            autoPopupDelay = SettingsUtil.getInteger(kitClass,
                             ExtSettingsNames.COMPLETION_AUTO_POPUP_DELAY,
                             ExtSettingsDefaults.defaultCompletionAutoPopupDelay);

            refreshDelay = SettingsUtil.getInteger(kitClass,
                                                   ExtSettingsNames.COMPLETION_REFRESH_DELAY,
                                                   ExtSettingsDefaults.defaultCompletionRefreshDelay);

            instantSubstitution = SettingsUtil.getBoolean(kitClass,
                                                   ExtSettingsNames.COMPLETION_INSTANT_SUBSTITUTION,
                                                   ExtSettingsDefaults.defaultCompletionInstantSubstitution);
            
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (ExtEditorUI.COMPONENT_PROPERTY.equals(propName)) {
            JTextComponent component = (JTextComponent)evt.getNewValue();
            if (component != null) { // just installed

                settingsChange(null);
                
                BaseDocument doc = Utilities.getDocument(component);
                if (doc != null) {
                    doc.addDocumentListener(docL);
                }

                component.addCaretListener( caretL );
            } else { // just deinstalled
                
                setPaneVisible(false);
                
                component = (JTextComponent)evt.getOldValue();

                BaseDocument doc = Utilities.getDocument(component);
                if (doc != null) {
                    doc.removeDocumentListener(docL);
                }
                
                if( component != null ) {
                    component.removeCaretListener( caretL );
                }
            }

        } else if ("document".equals(propName)) { // NOI18N
            if (evt.getOldValue() instanceof BaseDocument) {
                ((BaseDocument)evt.getOldValue()).removeDocumentListener(docL);
            }
            if (evt.getNewValue() instanceof BaseDocument) {
                ((BaseDocument)evt.getNewValue()).addDocumentListener(docL);
            }

        }

    }

    public CompletionPane getPane() {
        return (CompletionPane) getExtPane();
    }

    public ExtCompletionPane getExtPane() {
        if (pane == null){
            pane = new ScrollCompletionPane(extEditorUI);
        }
        return pane;
    }
    
    protected CompletionView createView() {
        return new ListCompletionView();
    }

    public final CompletionView getView() {
        if (view == null) {
            view = createView();
        }
        return view;
    }

    protected CompletionQuery createQuery() {
        return null;
    }

    public final CompletionQuery getQuery() {
        if (query == null) {
            query = createQuery();
        }
        return query;
    }
    
    public JavaDocPane getJavaDocPane(){
        if (javaDocPane == null){
            javaDocPane = new ScrollJavaDocPane(extEditorUI);
        }
        return javaDocPane;
    }
    
    /**
     * Get panel holding all aids (completion and documentation panes).
     * @return JDCPopupPanel or <code>null</code>
     */
    public final JDCPopupPanel getJDCPopupPanelIfExists() {
        return jdcPopupPanel;
    }

    /**
     * Get panel holding all aids (completion and documentation panes).
     * @return JDCPopupPanel never <code>null</code>
     */ 
    public JDCPopupPanel getJDCPopupPanel(){
        if (jdcPopupPanel == null){
            jdcPopupPanel =  new JDCPopupPanel(extEditorUI, getExtPane(), this);
        }
        return jdcPopupPanel;
    }

    /** Get the result of the last valid completion query or null
    * if there's no valid result available.
    */
    public synchronized final CompletionQuery.Result getLastResult() {
        return lastResult;
    }

    /** Reset the result of the last valid completion query. This
    * is done for example after the document was modified.
    */
    public synchronized final void invalidateLastResult() {
        currentTask().cancel();
        lastResult = null;
    }
     
    private synchronized void setKeyPressed(boolean value) {
        keyPressed = true;
    }
    
    private synchronized boolean isKeyPressed() {
        return keyPressed;
    }

    public synchronized Object getSelectedValue() {
        if (lastResult != null) {
            int index = getView().getSelectedIndex();
            if (index >= 0 && index<lastResult.getData().size()) {
                return lastResult.getData().get(index);
            }
        }
        return null;
    }

    /** Return true if the completion should popup automatically */
    public boolean isAutoPopupEnabled() {
        return autoPopup;
    }

    /** Return true when the pane exists and is visible.
    * This is the preferred method of testing the visibility of the pane
    * instead of <tt>getPane().isVisible()</tt> that forces
    * the creation of the pane.
    */
    public boolean isPaneVisible() {
        return (pane != null && pane.isVisible());
    }

    /** Set the visibility of the view. This method should
    * be used mainly for hiding the completion pane. If used
    * with visible set to true it calls the <tt>popup(false)</tt>.
    */
    public void setPaneVisible(boolean visible) {
        trace("ENTRY setPaneVisible " + visible);
        if (visible) {
            if (extEditorUI.getComponent() != null) {
                popupImpl(false);
            }
        } else {
            if (pane != null) {
                cancelRequestImpl();
                invalidateLastResult();
                getJDCPopupPanel().setCompletionVisible(false);
                caretPos=-1;
            }
        }
    }
    
    public void completionCancel(){
        trace("ENTRY completionCancel");
        if (pane != null){
            cancelRequestImpl();
            invalidateLastResult();
            caretPos=-1;
        }
    }

    /** Refresh the contents of the view if it's currently visible.
    * @param postRequest post the request instead of refreshing the view
    *   immediately. The <tt>ExtSettingsNames.COMPLETION_REFRESH_DELAY</tt>
    *   setting stores the number of milliseconds before the view is refreshed.
    */
    public void refresh(boolean postRequest) {
        trace("ENTRY refresh " + postRequest);
        refreshImpl(postRequest);
    }

    private synchronized void refreshImpl(final boolean postRequest) {

        // exit immediatelly
        if (isPaneVisible() == false) return;
        
        class RefreshTask implements Runnable {
            private final boolean batch;
            RefreshTask(boolean batch) {
                this.batch = batch;
            }
            public void run() {
                if (isPaneVisible()) {                    
                    timer.stop();
                    if (batch) {
                        timer.setInitialDelay(refreshDelay);
                        timer.setDelay(refreshDelay);
                        timer.start();
                    } else {
                        actionPerformed(null);
                    }
                }
            }            
        };
        
        SwingUtilities.invokeLater(new RefreshTask(postRequest));        
    }
    
    /** Get the help and show it in the view. If the view is already visible
    * perform the refresh of the view.
    * @param postRequest post the request instead of displaying the view
    *   immediately. The <tt>ExtSettingsNames.COMPLETION_AUTO_POPUP_DELAY</tt>
    *   setting stores the number of milliseconds before the view is displayed.
    *   If the user presses a key until the delay expires nothing is shown.
    *   This guarantees that the user which knows what to write will not be
    *   annoyed with the unnecessary help.
    */
    public void popup(boolean postRequest) {
        trace("ENTRY popup " + postRequest);
        popupImpl(postRequest);
    }

    private synchronized void popupImpl( boolean postRequest) {
        if (isPaneVisible()) {
            refreshImpl(postRequest);
        } else {
            timer.stop();
            if (postRequest) {
                timer.setInitialDelay(autoPopupDelay);
                timer.setDelay(autoPopupDelay);
                timer.start();
            } else {
                actionPerformed(null);
            }
        }        
    }
    
    /** Cancel last request for either displaying or refreshing
    * the pane. It resets the internal timer.
    */
    public void cancelRequest() {
        trace("ENTRY cancelRequest");
        cancelRequestImpl();
    }
    
    private synchronized void cancelRequestImpl() {
        timer.stop();
    }

    /** Called to do either displaying or refreshing of the view.
    * This method can be called either directly or because of the timer has fired.
    * @param evt event describing the timer firing or null
    *   if the method was called directly because of the synchronous
    *   showing/refreshing the view.
    */
    public synchronized void actionPerformed(ActionEvent evt) {
        
//        if (jdcPopupPanel == null) extEditorUI.getCompletionJavaDoc(); //init javaDoc
        
        final JTextComponent component = extEditorUI.getComponent();
        BaseDocument doc = Utilities.getDocument(component);
        
        if (component != null && doc != null) {
            
            boolean provokedByAutoPopup = evt != null;
            
            if(provokedByAutoPopup) {
                // AutoPopup performed, check whether the sources are prepared for completion
                ExtSyntaxSupport sup = (ExtSyntaxSupport)doc.getSyntaxSupport().get(ExtSyntaxSupport.class);
                if (sup!=null){
                    if (!sup.isPrepared()) {
                        return;
                    }
                }
            }
            
            try{
                if((caretPos!=-1) && (Utilities.getRowStart(component,component.getCaret().getDot()) !=
                    Utilities.getRowStart(component,caretPos)) && ((component.getCaret().getDot()-caretPos)>0) ){
                        getJDCPopupPanel().setCompletionVisible(false);
                        caretPos=-1;
                        return;
                }
            }catch(BadLocationException ble){
            }
            
            caretPos = component.getCaret().getDot();

            // show progress view
            class PendingTask extends CancelableRunnable {
                public void run() {
                    if (cancelled()) return;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (cancelled()) return;
                            performWait();
                        }
                    });
                }                
            };

            // perform query and show results
            class QueryTask extends CancelableRunnable {
                private final CancelableRunnable wait;
                private final boolean isPaneVisible;
                public QueryTask(CancelableRunnable wait, boolean isPaneVisible) {
                    this.wait = wait;
                    this.isPaneVisible = isPaneVisible;
                }
                public void run() {
                    if (cancelled()) return;
                    try {
                        performQuery(component);
                    } finally {
                        wait.cancel();
                        if (cancelled()) return;
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                if (cancelled()) return;
                                CompletionQuery.Result res = lastResult;
                                if (res != null) {
                                    if (instantSubstitution && res.getData().size() == 1 &&
                                        !isPaneVisible && instantSubstitutionImpl(caretPos)){
                                            setPaneVisible(false);
                                            return;
                                    }
                                }
                                
                                performResults();
                            }
                        });
                    }
                }
                void cancel() {
                    super.cancel();
                    wait.cancel();
                }
            };
                        
            // update current task: cancel pending task and fire new one

            currentTask().cancel();
            
             RequestProcessor rp;
            boolean reentrantProvider = getQuery() instanceof CompletionQuery.SupportsSpeculativeInvocation;
            if (reentrantProvider) {
                 rp = RequestProcessor.getDefault();
            } else {
                 rp = getSerialiazingRequestProcessor();
            }

            CancelableRunnable wait = new PendingTask();            
            CancelableRunnable task = new QueryTask(wait, getPane().isVisible());
            currentTask(task);
            if (provokedByAutoPopup == false) {
                 RequestProcessor.getDefault().post(wait, 100);
            }
            rp.post(task);
        }
    }

    /**
     * Show wait completion result. Always called from AWT.
     */
    private void performWait() {
        getPane().setTitle(LocaleSupport.getString("ext.Completion.wait"));
        getView().setResult((CompletionQuery.Result)null);
        if (isPaneVisible()) {
            getJDCPopupPanel().refresh();
        } else {
            getJDCPopupPanel().setCompletionVisible(true);
        }        
    }
    
    /**
     * Execute complegtion query subtask
     */
    private void performQuery(final JTextComponent target) {

        BaseDocument doc = Utilities.getDocument(target);
        long start = System.currentTimeMillis();
        try {
            lastResult = getQuery().query( target, caretPos, doc.getSyntaxSupport());
        } finally {
            trace("performQuery took " + (System.currentTimeMillis() - start) + "ms");
            setKeyPressed(false);
        }
    }

    /**
     * Show result popup. Always called from AWT.
     */
    private void performResults() {
        // sample
        CompletionQuery.Result res = lastResult;
        if (res != null) {
            
            if (instantSubstitution && res.getData().size() == 1 &&
                !isPaneVisible() && instantSubstitutionImpl(caretPos)) return;

            getPane().setTitle(res.getTitle());
            getView().setResult(res);
            if (isPaneVisible()) {
                getJDCPopupPanel().refresh();
            } else {
                getJDCPopupPanel().setCompletionVisible(true);
            }
        } else {
            getJDCPopupPanel().setCompletionVisible(false);
            
            if (!isKeyPressed()) {
                caretPos=-1;
            } else {
                setKeyPressed(false);
            }
        }        
    }
    
    /** Performs instant text substitution, provided that result contains only one 
     *  item and completion has been invoked at the end of the word.
     *  @param caretPos offset position of the caret
     */
    public boolean instantSubstitution(int caretPos){
        trace("ENTRY instantSubstitution " + caretPos);
        return instantSubstitutionImpl(caretPos);
    }
    
    private synchronized boolean instantSubstitutionImpl(int caretPos){
        if (getLastResult() == null) return false;
        JTextComponent comp = extEditorUI.getComponent();
        try{
            if ((comp == null) || Utilities.getWordEnd(comp,caretPos) > caretPos) return false;
            return getLastResult().substituteText(0, true);
        }catch(BadLocationException ble){
            return false;
        }
    }

    
    /** Substitute the document's text with the text
    * that is appopriate for the selection
    * in the view. This function is usually triggered
    * upon pressing the Enter key.
    * @return true if the substitution was performed
    *  false if not.
    */
    public synchronized boolean substituteText( boolean shift ) {        
        trace("ENTRY substituteText " + shift);
        if (lastResult != null) {
            int index = getView().getSelectedIndex();
            if (index >= 0) {
                lastResult.substituteText(index, shift );
            }
            return true;
        } else {
            return false;
        }
    }

    /** Substitute the text with the longest common
    * part of all the entries appearing in the view.
    * This function is usually triggered
    * upon pressing the Tab key.
    * @return true if the substitution was performed
    *  false if not.
    */
    public synchronized boolean substituteCommonText() {
        trace("ENTRY substituteCommonText");
        if (lastResult != null) {
            int index = getView().getSelectedIndex();
            if (index >= 0) {
                lastResult.substituteCommonText(index);
            }
            return true;
        } else {
            return false;
        }
    }

    
    // Task management ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    
    /**
     * Make given task current.
     */
    private void currentTask(CancelableRunnable task) {
        cancellable = task;
    }
    
    /**
     * Get current task
     */
    private CancelableRunnable currentTask() {
        return cancellable;
    }
    
    /**
     * Multistage task can test its cancel status after every atomic
     * (non-cancellable) stage.
     */
    abstract class CancelableRunnable implements Runnable {
        private boolean cancelled = false;
                
        boolean cancelled() {
            return cancelled;
        }
        
        void cancel() {
            cancelled = true;
        }
    }

    /**
     * Get serializing request processor.
     */
     private synchronized RequestProcessor getSerialiazingRequestProcessor() {
         if (serializingRequestProcessor == null) {
             serializingRequestProcessor = new RequestProcessor("editor.completion", 1);// NOI18N
         }
         return serializingRequestProcessor;
     }
    
    // Debug support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    private static void trace(String msg) {
        if (DEBUG_COMPLETION && Boolean.getBoolean(PROP_DEBUG_COMPLETION)) {
            synchronized (System.err) {
                System.err.println(msg);
            }
        }
    }
}
