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

package org.openide.filesystems;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Ales Novak
 */
final class PathElements {
    private static final String DELIMITER = "/"; // NOI18N
    
    /** Original name */
    private String name;
    /** tokenizer */
    private StringTokenizer tokenizer;
    /** tokens */
    private List tokens;

    /** Creates new PathElements */
    public PathElements(String name) {
        this.name = name;
        tokenizer = new StringTokenizer(name, DELIMITER);
        tokens = new ArrayList(10);
    }

    /** 
     * @return original name
     */
    public String getOriginalName() {
        return name;
    }
    
    public Enumeration getEnumeration() {
        return new EnumerationImpl(this);
    }
    
    boolean contains(int i) {
        if (tokens.size() <= i) {
            scanUpTo(i);
        }
        
        return (tokens.size() > i);
    }
    
    String get(int i) throws NoSuchElementException {
        if (tokens.size() <= i) {
            scanUpTo(i);
        }
        
        if (tokens.size() <= i) {
            throw new NoSuchElementException();
        }
        
        return (String) tokens.get(i);
    }
    
    private synchronized void scanUpTo(int i) {
        if (tokenizer == null) {
            return;
        }
        
        if (tokens.size() > i) {
            return;
        }
        
        for (int k = tokens.size() - 1; (k < i) && tokenizer.hasMoreTokens(); k++) {
           tokens.add(tokenizer.nextToken()); 
        }
        if (!tokenizer.hasMoreTokens())
            tokenizer = null;
    }
    
    /** Impl of enumeration */
    static final class EnumerationImpl implements Enumeration {
        private PathElements elements;
        private int pos;
        
        EnumerationImpl(PathElements elements) {
            this.elements = elements;
            this.pos = 0;
        }
        
        /** From Enumeration */
        public boolean hasMoreElements() {
            return elements.contains(pos);
        }

        /** From Enumeration */
        public java.lang.Object nextElement() throws NoSuchElementException {
            return elements.get(pos++);
        }
    }        
}
