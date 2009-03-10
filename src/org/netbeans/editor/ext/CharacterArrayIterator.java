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

import java.text.CharacterIterator;

/**
* Character-iterator that operates on the array of characters.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class CharacterArrayIterator implements CharacterIterator {

    char[] chars;

    int beginIndex;

    int endIndex;

    int index;

    public CharacterArrayIterator(char[] chars, int beginIndex, int endIndex) {
        this.chars = chars;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        index = beginIndex;
    }

    private char currentChar() {
        return (index >= beginIndex && index < endIndex) ? chars[index] : DONE;
    }

    public char first() {
        index = beginIndex;
        return currentChar();
    }

    public char last() {
        index = endIndex - 1;
        return currentChar();
    }

    public char current() {
        return currentChar();
    }

    public char next() {
        index = Math.min(index + 1, endIndex);
        return currentChar();
    }

    public char previous() {
        if (index <= beginIndex) {
            return DONE;
        } else {
            return chars[--index];
        }
    }

    public char setIndex(int position) {
        if (position < beginIndex || position >= endIndex) {
            throw new IllegalArgumentException();
        }
        index = position;
        return currentChar();
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getIndex() {
        return index;
    }

    public Object clone() {
        return new CharacterArrayIterator(chars, beginIndex, endIndex);
    }

}
