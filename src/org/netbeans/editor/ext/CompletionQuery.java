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

import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.SyntaxSupport;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
* Code completion querying SPI and support.
*
* @author Miloslav Metelka
* @version 1.01
*/

public interface CompletionQuery {

    /** Perform the query on the given component. The query usually
    * gets the component's document, the caret position and searches back
    * to find the last command start. Then it inspects the text up to the caret
    * position and returns the result.
    * <p>Implementations must be thread safe (also reentrant) because it can be
    * called speculatively from multiple threads. This requirement can be removed in future SPI
    * by passing additional flag marking speculative query. Skeletal implementation
    * could handle multithreading based on the flag.
    * @param component the component to use in this query.
    * @param offset position in the component's document to which the query will
    *   be performed. Usually it's a caret position.
    * @param support syntax-support that will be used during resolving of the query.
    * @return result of the query or null if there's no result.
    */
    public Result query(JTextComponent component, int offset, SyntaxSupport support);

    /**
     * Marker interface that should implement all providers that are
     * compatible with #13768 semantics. It requires thread safe and reentrant
     * completion query provider implementation. It's performance gain
     * to implement as it allows asynchronous speculative queries.
     *
     * @deprecated It is a workaround. It's suggested that providers
     * should wait for new completion query SPI that should better
     * support speculative queries, partial results, result
     * cancellation and result narrowing. Implement only if it's simple.
     *
     * @since CompletionQuery version 1.01
     */
    public interface SupportsSpeculativeInvocation {
        // marker interface
    }
    
    /** Result of the query or expression evaluation. Simply said it consists
    * of the list of the data and title and an internal information about
    * how to substitute the text.
    */
    public interface Result {

        /** Get the list with the items satisfying the query. The list
        * must always be non-null. If there are no data it will have a zero size.
        * @return List of objects implementing ResultItem.
        */
        public List getData();

        /** Get the title describing the result or null if there's no title. */
        public String getTitle();

        /** Substitute the text in the document if the user picks
        * the item from the data with the given index either by
        * pressing ENTER or doubleclicking the item by mouse.
        * @param dataIndex current selected item index in the current data list.
        *   It can be used for making the substitution.
        * @param shift indicates request for some kind of different behaviour,
        *   means that e.g. user hold shift while pressing ENTER.
        * @return whether the text was substituted or not
        */
        public boolean substituteText(int dataIndex, boolean shift);

        /** Substitute the text that is common for all the data entries.
        * This is used to update the document with
        * the common text when the user presses the TAB key.
        * @param dataIndex current selected item index in the current data list.
        *   Although normally it shouldn't be necessary for making
        *   the substitution, the completion implementations
        *   can use it for customized behavior.
        * @return whether the text was substituted or not
        */
        public boolean substituteCommonText(int dataIndex);

    }

    /**
    * The very basic funztionality of Result is implemented by this class,
    * but parts general enough to not need to be overriden.
    */
    public static abstract class AbstractResult implements Result {

        /** The List of the ResultItem instances - the content of the result */
        private List data;

        /** The title of the result */
        private String title;

        public AbstractResult(List data, String title) {
            this.data = data;
            this.title = title;
        }

        public List getData() {
            return data;
        }

        public String getTitle() {
            return title;
        }

    }

    
    /** Full implementation of Result, managing substitution of the text and
    * finding and substituting common prefix of items
    */
    public static class DefaultResult extends AbstractResult {

        private JTextComponent component;
        private int offset;
        private int len;

        /** Constructor for DefaultResult
        * @param component the JTextComponent the result is tightened with,
        *        used for operations on its Document, caret, selection and so.
        * @param title the title displayed in header of completion window
        * @param data the list of ResultItem instances to be displayed in
        *        completion window, may be null.
        * @param the offset in the document corresponding to the start
        *        of the text occassionally replaced by the result.
        * @param the length of the text to be replaced.
        */
        public DefaultResult(JTextComponent component, String title, List data, int offset, int len ) {
            super(data, title);
            this.component = component;
            this.offset = offset;
            this.len = len;
        }

        /** Internal method used to find longest common prefix of two Strings.
        * it is made private, because I'm going to change its interface
        * for better performance. 
        */
        private int getCommonPrefixLength( char[] commonPrefix, int len, String s ) {
            char[] c = s.toCharArray();
            int i=0;
            if( len > c.length ) len = c.length;
            for( ; i<len; i++ ) {
                if( commonPrefix[i] != c[i] ) break;
            }
            return i;
        }
        
        /** Update the text in response to pressing TAB key. Searches through
        * all items of this result looking for longest common prefix and then
        * calls the substitution method on selected item providing it with
        * the length of common part.
        * @return whether the text was successfully updated
        */
        public boolean substituteCommonText( int dataIndex ) {
            List data = getData();
            if( data.size() == 0 ) return false;

            Iterator i = data.iterator();
            char[] commonPrefix = ((CompletionQuery.ResultItem)i.next()).getItemText().toCharArray();
            int commonLength = commonPrefix.length;
            
            for( ; i.hasNext(); ) {
                String second = ((CompletionQuery.ResultItem)i.next()).getItemText();
                commonLength = getCommonPrefixLength( commonPrefix, commonLength, second );
            }
            CompletionQuery.ResultItem actData = (CompletionQuery.ResultItem)data.get(dataIndex);
            return actData.substituteCommonText( component, offset, len, commonLength );
        }

        
        /** Update the text in response to pressing ENTER.
        * @return whether the text was successfully updated
        */
        public boolean substituteText(int dataIndex, boolean shift ) {
            Object actData = getData().get( dataIndex );
            return ((CompletionQuery.ResultItem)actData).substituteText( component, offset, len, shift );
        }
    }

    
    /** An interface used as an item of List returned by CompletionQuery.Result.getData()
    *  Such items are then able to their part in Completion process themselves
    */
    public static interface ResultItem {
        /** Update the text in response to pressing TAB key (or any key mapped to
        * this function) on this element
        * @param c the text component to operate on, enables implementation to
        *        do things like movement of caret.
        * @param offset the offset where the item should be placed
        * @param len the length of recognized text which should be replaced
        * @param subLen the length of common part - the length of text that should
        *        be inserted after removal of recognized text
        * @return whether the text was successfully updated
        */
        public boolean substituteCommonText( JTextComponent c, int offset, int len, int subLen );

        /** Update the text in response to pressing ENTER on this element.
        * @param c the text component to operate on, enables implementation to
        *        do things like movement of caret.
        * @param offset the offset where the item should be placed
        * @param len the length of recognized text which should be replaced
        * @param shift the flag that instructs completion to behave somehow
        *        differently - enables more kinds of invocation of substituteText
        * @return whether the text was successfully updated
        */
        public boolean substituteText( JTextComponent c, int offset, int len, boolean shift );

        /** Says what text would this Element use if substituteText is called.
        * @return the substitution text, usable e.g. for finding common text/its' length
        */
        public String getItemText();

        /** Prepare proper component for painting value of <CODE>this</CODE>.
        * @param JList the list this item will be drawn into, usefull e.g. for 
        *        obtaining preferred colors.
        * @param isSelected tells if this item is just selected, for using
        *        proper color scheme.
        * @param cellHasFocus tells it this item is just focused.
        * @return the component usable for painting this value
        */
        public Component getPaintComponent( JList list, boolean isSelected, boolean cellHasFocus);
    }

    /** A class providing generic, nearly full implementation of ResultItem
    */
    public abstract static class AbstractResultItem implements CompletionQuery.ResultItem {
        /* The text this item would expand to */
        protected String text;

        /** Create new ResultItem for given text, should be used in subclass constructors
        */
        public AbstractResultItem( String text ) {
            this.text = text;
        }
        
        /** Generic implementation, behaves just as described in specification
        * in substituteCommonText() - removes <CODE>len</CODE>
        * characters at <CODE>offset</CODE> out of document and then inserts
        * <CODE>subLen<CODE> characters from the <CODE>text</CODE>
        */
        public boolean substituteCommonText( JTextComponent c, int offset, int len, int subLen ) {
            BaseDocument doc = (BaseDocument)c.getDocument();
            try {
                doc.atomicLock();
                try {
                    doc.remove( offset, len );
                    doc.insertString( offset, text.substring( 0, subLen ), null);
                } finally {
                    doc.atomicUnlock();
                }
            } catch( BadLocationException exc ) {
                return false;    //not sucessfull
            }
            return true;
        }

        /** Generic implementation, behaves just as described in specification
        * in substituteText() - removes <CODE>len</CODE> characters 
        * at <CODE>offset</CODE> out of document and then inserts
        * whole <CODE>text</CODE>. Ignores <CODE>shift</CODE> argument.
        */
        public boolean substituteText( JTextComponent c, int offset, int len, boolean shift ) {
            BaseDocument doc = (BaseDocument)c.getDocument();
            try {
                doc.atomicLock();
                try {
                    doc.remove( offset, len );
                    doc.insertString( offset, text, null);
                } finally {
                    doc.atomicUnlock();
                }
            } catch( BadLocationException exc ) {
                return false;    //not sucessfull
            }
            return true;
        }

        /** @return the text this item would expand to.
        */
        public String getItemText() {
            return text;
        }

    }

    public static class DefaultResultItem extends CompletionQuery.AbstractResultItem {
        /** The cache for component used for painting value of <CODE>this</CODE>
        * this component is reused, on every call to getPaintComponent it is
        * set up and then painted. By default, this component is hold opaque.
        */
        static JLabel rubberStamp = new JLabel();

        static {
            rubberStamp.setOpaque( true );
        }
        
        /** Color used for painting text of non-selected item */
        protected Color foreColor;
        
        public DefaultResultItem( String text, Color foreColor ) {
            super( text );
            this.foreColor = foreColor;
        }

        public Component getPaintComponent( JList list, boolean isSelected, boolean cellHasFocus ) {
            rubberStamp.setText( " " + text );
            if (isSelected) {
                rubberStamp.setBackground(list.getSelectionBackground());
                rubberStamp.setForeground(list.getSelectionForeground());
            } else {
                rubberStamp.setBackground(list.getBackground());
                rubberStamp.setForeground( foreColor );
            }
            return rubberStamp;
        }
    }

}
