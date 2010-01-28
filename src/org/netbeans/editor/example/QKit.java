package org.netbeans.editor.example;

import javax.swing.text.Document;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.SyntaxSupport;
import org.netbeans.editor.ext.Completion;
import org.netbeans.editor.ext.ExtEditorUI;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.editor.ext.q.QCompletion;
import org.netbeans.editor.ext.q.QSyntax;
import org.netbeans.editor.ext.q.QSyntaxSupport;


public class QKit extends ExtKit
{
    public QKit()
    {
        super();
    }

    public String getContentType()
    {
        return "text/q"; // NOI18N
    }

    public Syntax createSyntax(Document document)
    {
        return new QSyntax();
    }
    
    public SyntaxSupport createSyntaxSupport(BaseDocument doc)
    {
        return new QSyntaxSupport(doc);
    }

    public Completion createCompletion(ExtEditorUI extEditorUI)
    {
        return new QCompletion(extEditorUI);
    }
}
