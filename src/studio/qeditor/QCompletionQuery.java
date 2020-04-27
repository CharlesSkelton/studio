package studio.qeditor;

import studio.kdb.K;
import studio.kdb.Server;
import studio.kdb.ConnectionPool;
import studio.ui.Util;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.SyntaxSupport;
import org.netbeans.editor.ext.CompletionQuery;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import studio.kdb.Config;

public class QCompletionQuery implements CompletionQuery
{
    private static ImageIcon currentIcon;
    private static boolean lowerCase;

    public CompletionQuery.Result query(JTextComponent component, int offset, SyntaxSupport support)
     {
         CompletionQuery.Result r = null;
         currentIcon= null;

         try
         {
             if (component instanceof JEditorPane)
             {
                 Server s = (Server) ((JEditorPane) component).getDocument().getProperty("server");

                 if (s != null)
                 {
                     List result = new ArrayList();

                     String text = ((JEditorPane) component).getDocument().getText(0, offset);

                     StringTokenizer t= new StringTokenizer(text, " %$!&()=~#;:><?,+-'\"/*");

                     while( t.hasMoreTokens())
                     {
                         text= t.nextToken();
                     }

                     kx.c c= null;

                     try
                     {
                         c=ConnectionPool.getInstance().leaseConnection(s);

                         if(text.endsWith("."))
                         {
                             c.k(new K.KCharacterVector("cols "+text.substring(0,text.length()-1)));
                             Object res = c.getResponse();
                             if( res instanceof K.KSymbolVector)
                             {
                                  K.KSymbolVector tables= (K.KSymbolVector)res;

                                 for (int i = 0; i < tables.getLength(); i++)
                                 {
                                     result.add(new BooleanAttribItem(((K.KSymbol)tables.at(i)).s, offset, 0, false));
                                 }

                                 currentIcon= Util.COLUMN_ICON;

                                 r = new CompletionQuery.DefaultResult(component, "Columns", result, offset, 0);
                             }
                         }
                         else
                         {
                             c.k(new K.KCharacterVector("tables[]"));
                             Object res = c.getResponse();
                             if( res instanceof K.KSymbolVector)
                             {
                                  K.KSymbolVector tables= (K.KSymbolVector)res;

                                 for(int i = 0; i < tables.getLength(); i++)
                                 {
                                     result.add(new BooleanAttribItem(((K.KSymbol)tables.at(i)).s, offset, 0, false));
                                 }

                                 currentIcon= Util.TABLE_ICON;

                                 r = new CompletionQuery.DefaultResult(component, "Tables", result, offset, 0);
                             }
                         }
                     }
                     catch (Throwable th)
                     {
                     }
                     finally
                     {
                         if(c != null)
                             ConnectionPool.getInstance().freeConnection(s,c);
                     }
                 }
             }
         }
         catch (Throwable th)
         {

         }

         return r;
     }


    private static abstract class QResultItem implements CompletionQuery.ResultItem
    {
        static javax.swing.JLabel rubberStamp = new javax.swing.JLabel();

        static
        {
            rubberStamp.setOpaque(true);
        }

        String baseText;
        int offset;
        int length;

        public QResultItem(String baseText, int offset, int length)
        {
            this.baseText = lowerCase ? baseText.toLowerCase() : baseText;
            this.offset = offset;
            this.length = length;
        }

        boolean replaceText(JTextComponent component, String text)
        {
            BaseDocument doc = (BaseDocument) component.getDocument();
            doc.atomicLock();
            try
            {
                doc.remove(offset, length);
                doc.insertString(offset, text, null);
            }
            catch (BadLocationException exc)
            {
                return false;    //not sucessfull
            }
            finally
            {
                doc.atomicUnlock();
            }
            return true;
        }

        public boolean substituteCommonText(JTextComponent c, int a, int b, int subLen)
        {
            return replaceText(c, getItemText().substring(0, subLen));
        }

        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            return replaceText(c, getItemText());
        }

        /** @return Properly colored JLabel with text gotten from <CODE>getPaintText()</CODE>. */
        public java.awt.Component getPaintComponent(javax.swing.JList list, boolean isSelected, boolean cellHasFocus)
        {
            // The space is prepended to avoid interpretation as Q Label
            rubberStamp.setText(" " + getPaintText());  // NOI18N

            if( currentIcon != null)
            {
                rubberStamp.setIcon( currentIcon);
                rubberStamp.setIconTextGap(8);
            }

            if (isSelected)
            {
                rubberStamp.setBackground(list.getSelectionBackground());
                rubberStamp.setForeground(list.getSelectionForeground());
            }
            else
            {
                rubberStamp.setBackground(list.getBackground());
                rubberStamp.setForeground(getPaintColor());
            }
            return rubberStamp;
        }

        String getPaintText()
        {
            return getItemText();
        }

        abstract Color getPaintColor();

        public String getItemText()
        {
            return baseText;
        }
    }

    static class EndTagItem extends QResultItem
    {

        public EndTagItem(String baseText, int offset, int length)
        {
            super(baseText, offset, length);
        }

        Color getPaintColor()
        {
            return Color.blue;
        }

        public String getItemText()
        {
            return "</" + baseText + ">";
        } // NOI18N

        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            return super.substituteText(c, a, b, shift);
        }
    }

    private static class CharRefItem extends QResultItem
    {

        public CharRefItem(String name, int offset, int length)
        {
            super(name, offset, length);
        }

        Color getPaintColor()
        {
            return Color.red.darker();
        }

        public String getItemText()
        {
            return "&" + baseText + ";";
        } // NOI18N
    }

    private static class TagItem extends QResultItem
    {

        public TagItem(String name, int offset, int length)
        {
            super(name, offset, length);
        }

        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            replaceText(c, "<" + baseText + (shift ? " >" : ">")); // NOI18N
            if (shift)
            {
                Caret caret = c.getCaret();
                caret.setDot(caret.getDot() - 1);
            }
            return !shift; // flag == false;
        }

        Color getPaintColor()
        {
            return Color.blue;
        }

        public String getItemText()
        {
            return "<" + baseText + ">";
        } // NOI18N
    }

    private static class SetAttribItem extends QResultItem
    {
        boolean required;

        public SetAttribItem(String name, int offset, int length, boolean required)
        {
            super(name, offset, length);
            this.required = required;
        }

        Color getPaintColor()
        {
            return required ? Color.red : Color.green.darker();
        }

        String getPaintText()
        {
            return baseText;
        }

        public String getItemText()
        {
            return baseText + "=";
        } //NOI18N

        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            super.substituteText(c, 0, 0, shift);
            return false; // always refresh
        }
    }

    private static class BooleanAttribItem extends QResultItem
    {

        boolean required;

        public BooleanAttribItem(String name, int offset, int length, boolean required)
        {
            super(name, offset, length);
            this.required = required;
        }

        Color getPaintColor()
        {
            return required ? Color.red : Color.green.darker();
        }


        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            replaceText(c, shift ? baseText + " " : baseText);
            return false; // always refresh
        }
    }

    private static class PlainAttribItem extends QResultItem
    {

        boolean required;

        public PlainAttribItem(String name, int offset, int length, boolean required)
        {
            super(name, offset, length);
            this.required = required;
        }

        Color getPaintColor()
        {
            return required ? Color.red : Color.green.darker();
        }

        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            replaceText(c, baseText + "=''"); //NOI18N
            if (shift)
            {
                Caret caret = c.getCaret();
                caret.setDot(caret.getDot() - 1);
            }
            return false; // always refresh
        }
    }

    private static class ValueItem extends QResultItem
    {

        public ValueItem(String name, int offset, int length)
        {
            super(name, offset, length);
        }

        Color getPaintColor()
        {
            return Color.magenta;
        }

        public boolean substituteText(JTextComponent c, int a, int b, boolean shift)
        {
            replaceText(c, shift ? baseText + " " : baseText); // NOI18N
            return !shift;
        }
    }
}
