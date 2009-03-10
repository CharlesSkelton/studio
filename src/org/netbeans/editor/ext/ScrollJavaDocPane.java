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
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.net.URL;

/**
 *  JScrollPane implementation of JavaDocPane.
 *
 *  @author  Martin Roskanin
 *  @since   03/2002
 */
public class ScrollJavaDocPane extends JPanel implements JavaDocPane, SettingsChangeListener{

    protected  ExtEditorUI extEditorUI;
    private JComponent view;
//    private  CompletionJavaDoc cjd;
    protected JScrollPane scrollPane = new JScrollPane();
    Border lineBorder;
    
    /** Creates a new instance of ScrollJavaDocPane */
    public ScrollJavaDocPane(ExtEditorUI extEditorUI) {
        
//        new RuntimeException("ScrollJavaDocPane.<init>").printStackTrace();
        
        setLayout(null);
 
        this.extEditorUI = extEditorUI;
        
        // Add the completionJavaDoc view
//        cjd = extEditorUI.getCompletionJavaDoc();
/*        if (cjd!=null){
            JavaDocView javaDocView = cjd.getJavaDocView();
            if (javaDocView instanceof JComponent) {
                if (javaDocView instanceof JEditorPane){
                    ((JEditorPane)javaDocView).addHyperlinkListener(new HyperlinkAction());
                }
                view = (JComponent)javaDocView;
                scrollPane.setViewportView(view);
            }

            Settings.addSettingsChangeListener(this);
            setMinimumSize(new Dimension(100,100)); //[PENDING] put it into the options
            setMaximumSize(getMaxPopupSize());
        }else{
            setMinimumSize(new Dimension(0,0));
            setMaximumSize(new Dimension(0,0));
        }
        */
        super.setVisible(false);
        add(scrollPane);
        getAccessibleContext().setAccessibleDescription(LocaleSupport.getString("ACSD_JAVADOC_javaDocPane")); //NOI18N

        // !!! virtual method called from contructor!!
        installTitleComponent();
        setBorder(new LineBorder(javax.swing.UIManager.getColor("controlDkShadow"))); //NOI18N
    }


    public void setBounds(Rectangle r){
        super.setBounds(r);
        scrollPane.setBounds(r.x, 0, r.width+1, r.height );
    }

    public void setVisible(boolean visible){
        super.setVisible(visible);
/*        if (cjd!=null && !visible){
            cjd.clearHistory();
        }
        */
    }

    protected ImageIcon resolveIcon(String res){
        ClassLoader loader = this.getClass().getClassLoader();
        URL resource = loader.getResource( res );
        if( resource == null ) resource = ClassLoader.getSystemResource( res );
        return  ( resource != null ) ? new ImageIcon( resource ) : null;
    }

    protected void installTitleComponent() {
    }

    private Dimension getMaxPopupSize(){
        Class kitClass = Utilities.getKitClass(extEditorUI.getComponent());
        if (kitClass != null) {
            return (Dimension)SettingsUtil.getValue(kitClass,
                      ExtSettingsNames.JAVADOC_PREFERRED_SIZE,
                      ExtSettingsDefaults.defaultJavaDocAutoPopupDelay);

        }
        return ExtSettingsDefaults.defaultJavaDocPreferredSize;
    }

    public void settingsChange(SettingsChangeEvent evt) {
        if (ExtSettingsNames.JAVADOC_PREFERRED_SIZE.equals(evt.getSettingName())){
            setMaximumSize(getMaxPopupSize());
        }
    }

    public JComponent getComponent() {
        return this;
    }

    public void setForwardEnabled(boolean enable) {
    }

    public void setBackEnabled(boolean enable) {
    }

    public void setShowWebEnabled(boolean enable) {
    }

    public JComponent getJavadocDisplayComponent() {
        return scrollPane;
    }

    public class BrowserButton extends JButton {
        public BrowserButton() {
            setBorderPainted(false);
            setFocusPainted(false);
        }

        public BrowserButton(String text){
            super(text);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        public BrowserButton(Icon icon){
            super(icon);
            setBorderPainted(false);
            setFocusPainted(false);
        }
    }


    protected  class HyperlinkAction implements HyperlinkListener{
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e!=null && HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())){
/*                if (e.getDescription() != null){
                    Object obj = cjd.parseLink(e.getDescription(), null);
                    if (obj!=null){
                        cjd.setContent(obj);
                        cjd.addToHistory(obj);
                    }
                }
                */
            }
        }
    }
    /*
    private class BackAction implements ActionListener{
        public void actionPerformed(ActionEvent evt) {
            if (cjd!=null){
                System.out.println("back");
                cjd.backHistory();
            }
        }
    }

    private class ForwardAction implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            if (cjd!=null){
                System.out.println("fwd");
                cjd.forwardHistory();
            }
        }
    }
    */
}
