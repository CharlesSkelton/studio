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

package org.netbeans.editor.example;

import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.SyntaxSupport;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.editor.ext.ExtSyntaxSupport;
import org.netbeans.editor.ext.plain.PlainSyntax;

import javax.swing.text.Document;

/**
 * Editor kit used to edit the plain text.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public class PlainKit extends ExtKit
{

    public static final String PLAIN_MIME_TYPE = "text/tplain"; // NOI18N

    public String getContentType()
    {
        return PLAIN_MIME_TYPE;
    }

    public Syntax createSyntax(Document doc)
    {
        return new PlainSyntax();
    }

    public SyntaxSupport createSyntaxSupport(BaseDocument doc)
    {
        return new ExtSyntaxSupport(doc);
    }

}

