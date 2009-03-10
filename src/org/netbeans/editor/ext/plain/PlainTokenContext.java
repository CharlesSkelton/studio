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

package org.netbeans.editor.ext.plain;

import org.netbeans.editor.BaseImageTokenID;
import org.netbeans.editor.BaseTokenID;
import org.netbeans.editor.TokenContext;
import org.netbeans.editor.TokenContextPath;

/**
 * Tokens used in formatting
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public class PlainTokenContext extends TokenContext
{

    // Numeric-ids for token-ids
    public static final int TEXT_ID = 1;
    public static final int EOL_ID = 2;

    public static final BaseTokenID TEXT
            = new BaseTokenID("text", TEXT_ID);

    public static final BaseImageTokenID EOL
            = new BaseImageTokenID("EOL", EOL_ID, "\n");

    // Context declaration
    public static final PlainTokenContext context = new PlainTokenContext();

    public static final TokenContextPath contextPath = context.getContextPath();

    private PlainTokenContext()
    {
        super("format-");

        try
        {
            addDeclaredTokenIDs();
        }
        catch (Exception e)
        {
            if (Boolean.getBoolean("netbeans.debug.exceptions"))
            { // NOI18N
                e.printStackTrace();
            }
        }

    }

}
