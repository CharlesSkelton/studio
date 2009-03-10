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

import java.util.Map;

/** Support for comparing part of char array
* to hash map with strings as keys.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class StringMap extends java.util.HashMap {

    char[] testChars;

    int testOffset;

    int testLen;

    static final long serialVersionUID =967608225972123714L;
    public StringMap() {
        super();
    }

    public StringMap(int initialCapacity) {
        super(initialCapacity);
    }

    public StringMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public StringMap(Map t) {
        super(t);
    }

    public Object get(char[] chars, int offset, int len) {
        testChars = chars;
        testOffset = offset;
        testLen = len;
        Object o = get(this);
        testChars = null; // enable possible GC
        return o;
    }

    public boolean containsKey(char[] chars, int offset, int len) {
        testChars = chars;
        testOffset = offset;
        testLen = len;
        boolean b = containsKey(this);
        testChars = null; // enable possible GC
        return b;
    }

    public Object remove(char[] chars, int offset, int len) {
        testChars = chars;
        testOffset = offset;
        testLen = len;
        Object o = remove(this);
        testChars = null;
        return o;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof String) {
            String s = (String)o;
            if (testLen == s.length()) {
                for (int i = testLen - 1; i >= 0; i--) {
                    if (testChars[testOffset + i] != s.charAt(i)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        if (o instanceof char[]) {
            char[] chars = (char[])o;
            if (testLen == chars.length) {
                for (int i = testLen - 1; i >= 0; i--) {
                    if (testChars[testOffset + i] != chars[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        return false;
    }

    public int hashCode() {
        int h = 0;
        char[] chars = testChars;
        int off = testOffset;

        for (int i = testLen; i > 0; i--) {
            h = 31 * h + chars[off++];
        }

        return h;
    }

}
