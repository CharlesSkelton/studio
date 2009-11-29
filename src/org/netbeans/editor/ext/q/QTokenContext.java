/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/
package org.netbeans.editor.ext.q;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.editor.BaseTokenID;
import org.netbeans.editor.TokenContext;
import org.netbeans.editor.TokenContextPath;

public class QTokenContext extends TokenContext
{
    private QTokenContext()
    {
        super("q-");

        try
        {
            addDeclaredTokenIDs();
        }
        catch(Exception e)
        {
            Logger.getLogger("QTokenContext").log(Level.SEVERE,"Unexpected exception",e);
        }
    }
    // Numeric-ids for token-ids
    public static final int SYMBOL_ID      = 1;
    public static final int CHARVECTOR_ID  = 2;
    public static final int IDENTIFIER_ID  = 3;
    public static final int OPERATOR_ID    = 4;
    public static final int EOL_COMMENT_ID = 5;
    public static final int KEYWORD_ID     = 6;
    public static final int WHITESPACE_ID  = 7;
    public static final int UNKNOWN_ID     = 8;
    public static final int INTEGER_ID     = 9;
    public static final int MINUTE_ID      = 11;
    public static final int SECOND_ID      = 12;
    public static final int TIME_ID        = 13;
    public static final int DATE_ID        = 14;
    public static final int MONTH_ID       = 15;
    public static final int FLOAT_ID       = 16;
    public static final int LONG_ID        = 17;
    public static final int SHORT_ID       = 18;
    public static final int REAL_ID        = 19;    
    public static final int BYTE_ID        = 20; 
    public static final int BOOLEAN_ID     = 21; 
    public static final int DATETIME_ID    = 22;
    public static final int COMMAND_ID     = 23; 
    public static final int SYSTEM_ID      = 24; 
    public static final int TIMESTAMP_ID   = 25;
    public static final int TIMESPAN_ID    = 26;
    //    public static final int TEMPORAL_ID    = 10;

    //public static final BaseTokenID TEXT=    new BaseTokenID("text",    TEXT_ID);
   // public static final BaseTokenID KEYWORD= new BaseTokenID("keyword", KEYWORD_ID);
    //public static final BaseTokenID COMMENT= new BaseTokenID("comment", COMMENT_ID);
   // public static final BaseTokenID CHARVECTOR= new BaseTokenID("charvector", CHARVECTOR_ID);
    //public static final BaseImageTokenID EOL= new BaseImageTokenID("EOL", EOL_ID, "\n");


    public static final BaseTokenID SYMBOL=      new BaseTokenID("symbol",     SYMBOL_ID);
    public static final BaseTokenID CHAR_VECTOR= new BaseTokenID("charvector", CHARVECTOR_ID);
    public static final BaseTokenID IDENTIFIER=  new BaseTokenID("identifier", IDENTIFIER_ID);
    public static final BaseTokenID OPERATOR=    new BaseTokenID("operator",   OPERATOR_ID);
    public static final BaseTokenID EOL_COMMENT= new BaseTokenID("eolComment", EOL_COMMENT_ID);
    public static final BaseTokenID KEYWORD=     new BaseTokenID("keyword",    KEYWORD_ID);
    public static final BaseTokenID WHITESPACE=  new BaseTokenID("whitespace", WHITESPACE_ID);
    public static final BaseTokenID UNKNOWN=     new BaseTokenID("unknown",    UNKNOWN_ID);
    public static final BaseTokenID INTEGER=     new BaseTokenID("integer",    INTEGER_ID);
    public static final BaseTokenID MINUTE=      new BaseTokenID("minute",     MINUTE_ID);
    public static final BaseTokenID SECOND=      new BaseTokenID("second",     SECOND_ID);
    public static final BaseTokenID TIME=        new BaseTokenID("time",       TIME_ID);
    public static final BaseTokenID DATE=        new BaseTokenID("date",       DATE_ID);
    public static final BaseTokenID MONTH=       new BaseTokenID("month",      MONTH_ID);
    public static final BaseTokenID FLOAT=       new BaseTokenID("float",      FLOAT_ID);
    public static final BaseTokenID LONG=        new BaseTokenID("long",       LONG_ID);
    public static final BaseTokenID SHORT=       new BaseTokenID("short",      SHORT_ID);
    public static final BaseTokenID REAL=        new BaseTokenID("real",       REAL_ID);
    public static final BaseTokenID BYTE=        new BaseTokenID("byte",       BYTE_ID);
    public static final BaseTokenID BOOLEAN=     new BaseTokenID("boolean",    BOOLEAN_ID);
    public static final BaseTokenID DATETIME=    new BaseTokenID("datetime" ,  DATETIME_ID);
    public static final BaseTokenID TIMESTAMP=   new BaseTokenID("timestamp",  TIMESTAMP_ID);
    public static final BaseTokenID TIMESPAN=    new BaseTokenID("timespan",   TIMESPAN_ID);
    public static final BaseTokenID SYSTEM=      new BaseTokenID("system",     SYSTEM_ID);
    public static final BaseTokenID COMMAND=     new BaseTokenID("command",    COMMAND_ID);

//    public static final BaseImageTokenID EOL= new BaseImageTokenID("EOL", EOL_ID, "\n");


    public static final QTokenContext context=new QTokenContext();
    public static final TokenContextPath contextPath=context.getContextPath();
}