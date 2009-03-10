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

package org.netbeans.editor.ext;


import org.netbeans.editor.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
* Pane displaying the completion view and accompanying components
* like label for title etc.
*
* @author Miloslav Metelka, Martin Roskanin
* @version 1.00
*/

public class ScrollCompletionPane extends JScrollPane implements ExtCompletionPane,
    PropertyChangeListener, SettingsChangeListener {

    private ExtEditorUI extEditorUI;

    private JComponent view;

    private JLabel topLabel;

    private Dimension minSize;

    private Dimension maxSize;

    private ViewMouseListener viewMouseL;

    private Dimension scrollBarSize;
    
    private Dimension minSizeDefault;

    public ScrollCompletionPane(ExtEditorUI extEditorUI) {
        this.extEditorUI = extEditorUI;

        // Compute size of the scrollbars
        Dimension smallSize = getPreferredSize();
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        scrollBarSize = getPreferredSize();
        scrollBarSize.width -= smallSize.width;
        scrollBarSize.height -= smallSize.height;
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);

        // Make it invisible initially
        super.setVisible(false);

        // Add the title component
        installTitleComponent();

        // Add the completion view
        CompletionView completionView = extEditorUI.getCompletion().getView();
        if (completionView instanceof JComponent) {
            view = (JComponent)completionView;
            setViewportView(view);
        }

        // Prevent the bug with displaying without the scrollbar
        getViewport().setMinimumSize(new Dimension(4,4));

        Settings.addSettingsChangeListener(this);

        viewMouseL = new ViewMouseListener();
        synchronized (extEditorUI.getComponentLock()) {
            // if component already installed in ExtEditorUI simulate installation
            JTextComponent component = extEditorUI.getComponent();
            if (component != null) {
                propertyChange(new PropertyChangeEvent(extEditorUI,
                                                       ExtEditorUI.COMPONENT_PROPERTY, null, component));
            }

            extEditorUI.addPropertyChangeListener(this);
        }
        
        putClientProperty ("HelpID", ScrollCompletionPane.class.getName ()); // !!! NOI18N
    }
    
    
    public void settingsChange(SettingsChangeEvent evt) {
        Class kitClass = Utilities.getKitClass(extEditorUI.getComponent());

        if (kitClass != null) {
            minSize = (Dimension)SettingsUtil.getValue(kitClass,
                      ExtSettingsNames.COMPLETION_PANE_MIN_SIZE,
                      ExtSettingsDefaults.defaultCompletionPaneMinSize);
            minSizeDefault = new Dimension(minSize);
            setMinimumSize(minSize);
            
            maxSize = (Dimension)SettingsUtil.getValue(kitClass,
                      ExtSettingsNames.COMPLETION_PANE_MAX_SIZE,
                      ExtSettingsDefaults.defaultCompletionPaneMaxSize);
            setMaximumSize(maxSize);
            
        }
    }

    
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (ExtEditorUI.COMPONENT_PROPERTY.equals(propName)) {
            if (evt.getNewValue() != null) { // just installed
     
                settingsChange(null);

                if (view != null) {
                    // Add mouse listener
                    view.addMouseListener(viewMouseL);
                }

                
            } else { // just deinstalled

                if (view != null) {
                     // Unregister Escape key
                    view.removeMouseListener(viewMouseL);
                }
            }
        }
    }
    
    public void setVisible(boolean visible){
        //new RuntimeException("ScrollCompletionPane.setVisible(" + visible + ")").printStackTrace();
        if (view instanceof JList) {
            JList listView = (JList)view;
            listView.ensureIndexIsVisible(listView.getSelectedIndex());
        }
        
        super.setVisible(visible);
    }
    
    public void refresh() {
        if (view instanceof JList) {
            JList listView = (JList)view;
            listView.ensureIndexIsVisible(listView.getSelectedIndex());
        }
        
        SwingUtilities.invokeLater( // !!! ? is it needed
            new Runnable() {
                public void run() {
                    if (isShowing()) { // #18810
//                        extEditorUI.getPopupManager().reset(extEditorUI.getComponent());
                        revalidate();
                    }
                }
            }
        );
    }

    /** Set the title of the pane according to the completion query results. */
    public void setTitle(String title) {
        topLabel.setText(title);
    }

    
    protected void installTitleComponent() {
        topLabel = new JLabel();
        topLabel.setForeground(Color.blue);
        topLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        setColumnHeaderView(topLabel);
    }

    protected Dimension getTitleComponentPreferredSize() {
        return topLabel.getPreferredSize();
    }
    
    public void setSize(int width, int height){
        int maxWidth = width;
        int maxHeight = height;

        minSize.width = minSizeDefault.width;
        minSize.height = minSizeDefault.height;
        setMinimumSize(minSize);
        
        Dimension ps = getPreferredSize();

        /* Add size of the vertical scrollbar by default. This could be improved
        * to be done only if the height exceeds the bounds. */
        ps.width += scrollBarSize.width;
        ps.width = Math.max(Math.max(ps.width, minSize.width),
                            getTitleComponentPreferredSize().width);

        maxWidth = Math.min(maxWidth, maxSize.width);
        maxHeight = Math.min(maxHeight, maxSize.height);
        boolean displayHorizontalScrollbar = (ps.width-scrollBarSize.width)>maxWidth;

        if (ps.width > maxWidth) {
            ps.width = maxWidth;
            if (displayHorizontalScrollbar){
                ps.height += scrollBarSize.height; // will show horizontal scrollbar
                minSize.height += scrollBarSize.height;
                setMinimumSize(minSize);
            }
            
        }

        ps.height = Math.min(Math.max(ps.height, minSize.height), maxHeight);
        super.setSize(ps.width, ps.height);
    }
    
    public void setSize(Dimension d){
        setSize(d.width, d.height);
    }
    
    public JComponent getComponent() {
        return this;
    }    
    
    class ViewMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                JTextComponent component = extEditorUI.getComponent();
                if( component != null && evt.getClickCount() == 2 ) {
                    BaseKit kit = Utilities.getKit(component);
                    if (kit != null) {
                        Action a = kit.getActionByName(BaseKit.insertBreakAction);
                        if (a != null) {
                            a.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "")); // NOI18N
                        }
                    }
                }
            }
        }
    }

}
