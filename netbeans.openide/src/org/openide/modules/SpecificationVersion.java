/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2001 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.modules;

// THIS CLASS OUGHT NOT USE NbBundle NOR org.openide CLASSES
// OUTSIDE OF openide-util.jar! UI AND FILESYSTEM/DATASYSTEM
// INTERACTIONS SHOULD GO ELSEWHERE.

import java.util.*;

/** Utility class representing a specification version.
 * @author Jesse Glick
 * @since 1.24
 */
public final class SpecificationVersion implements Comparable {
    
    // Might be a bit wasteful of memory, but many SV's are created during
    // startup, so best to not have to reparse them each time!
    // In fact sharing the int arrays might save a bit of memory overall,
    // since it is unusual for a module to be deleted.
    private static final Map parseCache = new HashMap(200); // Map<String,int[]>

    private final int[] digits;

    /** Parse from string. Must be Dewey-decimal. */
    public SpecificationVersion(String version) throws NumberFormatException {
        synchronized (parseCache) {
            int[] d = (int[])parseCache.get(version);
            if (d == null) {
                d = parse(version);
                parseCache.put(version.intern(), d);
            }
            digits = d;
        }
    }
    
    private static int[] parse(String version) throws NumberFormatException {
        List l = new ArrayList(version.length());
        StringTokenizer tok = new StringTokenizer(version, ".", true); // NOI18N
        if (tok.countTokens() % 2 == 0) {
            throw new NumberFormatException("Even number of pieces in a spec version: `" + version + "'"); // NOI18N
        }
        boolean expectingNumber = true;
        while (tok.hasMoreTokens()) {
            if (expectingNumber) {
                expectingNumber = false;
                int piece = Integer.parseInt(tok.nextToken());
                if (piece < 0) throw new NumberFormatException("Spec version component <0: " + piece); // NOI18N
                l.add(new Integer(piece));
            } else {
                if (! ".".equals(tok.nextToken())) { // NOI18N
                    throw new NumberFormatException("Expected dot in spec version: `" + version + "'"); // NOI18N
                }
                expectingNumber = true;
            }
        }
        int size = l.size();
        int[] digits = new int[size];
        for (int i = 0; i < size; i++) {
            digits[i] = ((Integer) l.get(i)).intValue();
        }
        return digits;
    }

    /** Perform a Dewey-decimal comparison. */
    public int compareTo(Object o) {
        int[] od = ((SpecificationVersion) o).digits;
        int len1 = digits.length;
        int len2 = od.length;
        int max = Math.max(len1, len2);
        for (int i = 0; i < max; i++) {
            int d1 = (i < len1 ? digits[i] : 0);
            int d2 = (i < len2 ? od[i] : 0);
            if (d1 != d2) {
                return d1 - d2;
            }
        }
        return 0;
    }

    /** Overridden to compare contents. */
    public boolean equals(Object o) {
        if (! (o instanceof SpecificationVersion)) return false;
        int[] d = ((SpecificationVersion) o).digits;
        int len = digits.length;
        if (len != d.length) return false;
        for (int i = 0; i < len; i++) {
            if (digits[i] != d[i]) return false;
        }
        return true;
    }

    /** Overridden to hash by contents. */
    public int hashCode() {
        int hash = 925295;
        int len = digits.length;
        for (int i = 0; i < len; i++) {
            hash ^= (digits[i] << i);
        }
        return hash;
    }
    
    /** String representation (Dewey-decimal). */
    public String toString() {
        StringBuffer buf = new StringBuffer(digits.length * 3 + 1);
        for (int i = 0; i < digits.length; i++) {
            if (i > 0) buf.append('.'); // NOI18N
            buf.append(digits[i]);
        }
        return buf.toString();
    }

}
