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

package org.netbeans.editor;

/**
* Token-item presents a token as a piece information
* without dependence on a character buffer and it enables
* to chain the token-items in both directions.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface TokenItem {

    /** Get the token-id of this token-item */
    public TokenID getTokenID();

    /** Get the token-id of this token-item */
    public TokenContextPath getTokenContextPath();

    /** Get the position of the token in the document */
    public int getOffset();

    /** Get the image of this token. */
    public String getImage();

    /** Get next token-item in the text. It returns null
    * if there's no more next tokens in the text. It can throw
    * <tt>IllegalStateException</tt> in case the document
    * was changed so the token-item chain becomes invalid.
    */
    public TokenItem getNext();

    /** Get previous token-item in the text. It returns null
    * if there's no more previous tokens in the text. It can throw
    * <tt>IllegalStateException</tt> in case the document
    * was changed so the token-item chain becomes invalid.
    */
    public TokenItem getPrevious();

    /** Abstract implementation that doesn't contain chaining methods. */
    public static abstract class AbstractItem implements TokenItem {

        private TokenID tokenID;

        private TokenContextPath tokenContextPath;

        private String image;

        private int offset;

        public AbstractItem(TokenID tokenID, TokenContextPath tokenContextPath,
        int offset, String image) {
            this.tokenID = tokenID;
            this.tokenContextPath = tokenContextPath;
            this.offset = offset;
            this.image = image;
        }

        public TokenID getTokenID() {
            return tokenID;
        }

        public TokenContextPath getTokenContextPath() {
            return tokenContextPath;
        }

        public int getOffset() {
            return offset;
        }

        public String getImage() {
            return image;
        }

        public String toString() {
            return "'" + org.netbeans.editor.EditorDebug.debugString(getImage())
                   + "', tokenID=" + getTokenID() + ", tcp=" + getTokenContextPath()
                   + ", offset=" + getOffset();
        }

    }

    /** Implementation useful for delegation. */
    public static class FilterItem implements TokenItem {

        protected TokenItem delegate;

        public FilterItem(TokenItem delegate) {
            this.delegate = delegate;
        }

        public TokenID getTokenID() {
            return delegate.getTokenID();
        }

        public TokenContextPath getTokenContextPath() {
            return delegate.getTokenContextPath();
        }

        public int getOffset() {
            return delegate.getOffset();
        }

        public String getImage() {
            return delegate.getImage();
        }

        public TokenItem getNext() {
            return delegate.getNext();
        }

        public TokenItem getPrevious() {
            return delegate.getPrevious();
        }

        public String toString() {
            return delegate.toString();
        }

    }

}
