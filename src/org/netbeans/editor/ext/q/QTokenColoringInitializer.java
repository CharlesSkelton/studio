/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package org.netbeans.editor.ext.q;

import studio.kdb.Config;
import java.awt.Color;
import java.awt.Font;
import org.netbeans.editor.Coloring;
import org.netbeans.editor.Settings;
import org.netbeans.editor.SettingsDefaults;
import org.netbeans.editor.SettingsUtil;
import org.netbeans.editor.TokenCategory;
import org.netbeans.editor.TokenContextPath;

class QTokenColoringInitializer extends SettingsUtil.TokenColoringInitializer
{
    Font boldFont=SettingsDefaults.defaultFont.deriveFont(Font.BOLD);
    Font italicFont=SettingsDefaults.defaultFont.deriveFont(Font.ITALIC);
    Settings.Evaluator boldSubst=new SettingsUtil.FontStylePrintColoringEvaluator(Font.BOLD);
    Settings.Evaluator italicSubst=new SettingsUtil.FontStylePrintColoringEvaluator(Font.ITALIC);
    Settings.Evaluator lightGraySubst=new SettingsUtil.ForeColorPrintColoringEvaluator(Color.lightGray);
    private Coloring CHARVECTOR_Coloring;
    private Coloring EOL_COMMENT_Coloring;
    private Coloring IDENTIFIER_Coloring;
    private Coloring OPERATOR_Coloring;
    private Coloring BOOLEAN_Coloring;
    private Coloring BYTE_Coloring;
    private Coloring SHORT_Coloring;
    private Coloring LONG_Coloring;
    private Coloring REAL_Coloring;
    private Coloring INTEGER_Coloring;
    private Coloring FLOAT_Coloring;
    private Coloring TIMESTAMP_Coloring;
    private Coloring DATE_Coloring;
    private Coloring MONTH_Coloring;
    private Coloring MINUTE_Coloring;
    private Coloring SECOND_Coloring;
    private Coloring TIME_Coloring;
    private Coloring SYMBOL_Coloring;
    private Coloring KEYWORD_Coloring;
    private Coloring COMMAND_Coloring;
    private Coloring SYSTEM_Coloring;
    private Coloring WHITESPACE_Coloring;
    private Coloring DEFAULT_Coloring;
    
    private Coloring buildColoring(String name, Font font, Color defaultColor)
    {
        return new Coloring(font,
                            Coloring.FONT_MODE_APPLY_STYLE,
                            Config.getInstance().getColorForToken(name, defaultColor),
                            null);
    }

    public QTokenColoringInitializer()
    {
        super(QTokenContext.context);
        CHARVECTOR_Coloring=buildColoring("CHARVECTOR", SettingsDefaults.defaultFont,new Color(0,200,20));
        EOL_COMMENT_Coloring=buildColoring("EOLCOMMENT", italicFont, Color.GRAY);
        IDENTIFIER_Coloring=buildColoring("IDENTIFIER", SettingsDefaults.defaultFont,new Color(180,160,0));
        OPERATOR_Coloring=buildColoring("OPERATOR", SettingsDefaults.defaultFont,Color.BLACK);
        BOOLEAN_Coloring=buildColoring("BOOLEAN", SettingsDefaults.defaultFont,new Color(51,204,255));
        BYTE_Coloring=buildColoring("BYTE",SettingsDefaults.defaultFont,new Color(51,104,255));
        SHORT_Coloring=buildColoring("SHORT",SettingsDefaults.defaultFont,new Color(51,104,255));
        LONG_Coloring=buildColoring("LONG",SettingsDefaults.defaultFont,new Color(51,104,255));
        REAL_Coloring=buildColoring("REAL",SettingsDefaults.defaultFont,new Color(51,104,255));
        INTEGER_Coloring=buildColoring("INTEGER",SettingsDefaults.defaultFont,new Color(51,104,255));
        FLOAT_Coloring=buildColoring("FLOAT",SettingsDefaults.defaultFont,new Color(51,104,255));
        TIMESTAMP_Coloring=buildColoring("TIMESTAMP",SettingsDefaults.defaultFont,new Color(184,138,0));
        DATE_Coloring=buildColoring("DATE",SettingsDefaults.defaultFont,new Color(184,138,0));
        MONTH_Coloring=buildColoring("MONTH",SettingsDefaults.defaultFont,new Color(184,138,0));
        MINUTE_Coloring=buildColoring("MINUTE",SettingsDefaults.defaultFont,new Color(184,138,0));
        SECOND_Coloring=buildColoring("SECOND",SettingsDefaults.defaultFont,new Color(184,138,0));
        TIME_Coloring=buildColoring("TIME",SettingsDefaults.defaultFont,new Color(184,138,0));
        SYMBOL_Coloring=buildColoring("SYMBOL",SettingsDefaults.defaultFont,new Color(179,0,134));
        KEYWORD_Coloring=buildColoring("KEYWORD", boldFont,new Color(0,0,255));
        COMMAND_Coloring=buildColoring("COMMAND", SettingsDefaults.defaultFont,new Color(240,180,0));
        SYSTEM_Coloring=buildColoring("SYSTEM", SettingsDefaults.defaultFont,new Color(240,180,0));
        WHITESPACE_Coloring=buildColoring("WHITESPACE", SettingsDefaults.defaultFont,Color.black);
        DEFAULT_Coloring=buildColoring("DEFAULT", SettingsDefaults.defaultFont,Color.black);
    }

    public Object getTokenColoring(TokenContextPath tokenContextPath,
                                   TokenCategory tokenIDOrCategory,
                                   boolean printingSet)
    {
        if(!printingSet)
        {
            switch(tokenIDOrCategory.getNumericID())
            {
                case QTokenContext.CHARVECTOR_ID:
                    return CHARVECTOR_Coloring;
                case QTokenContext.EOL_COMMENT_ID:
                    return EOL_COMMENT_Coloring;
                case QTokenContext.IDENTIFIER_ID:
                    return IDENTIFIER_Coloring;
                case QTokenContext.OPERATOR_ID:
                    return OPERATOR_Coloring;
                case QTokenContext.BOOLEAN_ID:
                    return BOOLEAN_Coloring;
                case QTokenContext.BYTE_ID:
                    return BYTE_Coloring;
                case QTokenContext.SHORT_ID:
                    return SHORT_Coloring;
                case QTokenContext.LONG_ID:
                    return LONG_Coloring;
                case QTokenContext.REAL_ID:
                    return REAL_Coloring;
                case QTokenContext.INTEGER_ID:
                    return INTEGER_Coloring;
                case QTokenContext.FLOAT_ID:
                    return FLOAT_Coloring;
                case QTokenContext.TIMESTAMP_ID:
                    return TIMESTAMP_Coloring;
                case QTokenContext.DATE_ID:
                    return DATE_Coloring;
                case QTokenContext.MONTH_ID:
                    return MONTH_Coloring;
                case QTokenContext.MINUTE_ID:
                    return MINUTE_Coloring;
                case QTokenContext.SECOND_ID:
                    return SECOND_Coloring;
                case QTokenContext.TIME_ID:
                    return TIME_Coloring;
                case QTokenContext.SYMBOL_ID:
                    return SYMBOL_Coloring;
                case QTokenContext.UNKNOWN_ID:
                    return new Coloring(boldFont,Coloring.FONT_MODE_APPLY_STYLE,Color.red,null);
                case QTokenContext.KEYWORD_ID:
                    return KEYWORD_Coloring;
                case QTokenContext.COMMAND_ID:
                    return COMMAND_Coloring;
                case QTokenContext.SYSTEM_ID:
                    return SYSTEM_Coloring;
                case QTokenContext.WHITESPACE_ID:
                    return WHITESPACE_Coloring;
                default:
                    return DEFAULT_Coloring;
            }
        }
        else
        { // printing set
            switch(tokenIDOrCategory.getNumericID())
            {
                default:
                    return SettingsUtil.defaultPrintColoringEvaluator;
            }
        }
    }
}