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

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 *  HTML javadoc view. 
 *  Javadoc content is displayed in JEditorPane pane using HTMLEditorKit.
 *
 *  @author  Martin Roskanin
 *  @since   03/2002
 */
public class HTMLJavaDocView extends JEditorPane implements JavaDocView {
    
    private HTMLEditorKit htmlKit;
    
    /** Creates a new instance of HTMLJavaDocView */
    public HTMLJavaDocView(Color bgColor) {
        setEditable(false);
        setBGColor(bgColor);
        setMargin(new Insets(0,3,3,3));
    }

    /** Sets the javadoc content as HTML document */
    public void setContent(final String content) {
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                Reader in = new StringReader("<HTML><BODY>"+content+"</BODY></HTML>");//NOI18N                
                try{
                    Document doc = getDocument();
                    doc.remove(0, doc.getLength());
                    getEditorKit().read(in, getDocument(),0);  //!!! still too expensive to be called from AWT
                    setCaretPosition(0);
                    scrollRectToVisible(new Rectangle(0,0,0,0));            
                }catch(IOException ioe){
                    ioe.printStackTrace();
                }catch(BadLocationException ble){
                    ble.printStackTrace();
                }
            }
        });
    }
    
    /** Sets javadoc background color */
    public void setBGColor(Color bgColor) {
        setBackground(bgColor);
    }
    
    protected EditorKit createDefaultEditorKit() {
        // it is extremelly slow to init it
//        new RuntimeException("new HTMLEditorKit").printStackTrace();
        if (htmlKit == null){
            htmlKit= new HTMLEditorKit ();
            setEditorKit(htmlKit);
        }
        return htmlKit;
    }
}
