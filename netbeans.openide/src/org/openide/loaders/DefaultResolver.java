/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
/*
 * DefaultResolver.java
 *
 * Created on July 11, 2001, 11:24 AM
 */

package org.openide.loaders;

import org.openide.filesystems.FileObject;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.net.MalformedURLException;
import org.openide.filesystems.URLMapper;

/** Default implementation of references resolver. It recognizes 
 *
 * @author  Svatopluk Dedic, Vita Stejskal
 */
class DefaultResolver extends Object implements ReferencesResolver {
    private static final String NAME_DELIMITER = "/"; // NOI18N
    private static final String DOT = "."; // NOI18N
    private static final String DOUBLE_DOT = ".."; // NOI18N

    public FileObject find (URL ref, FileObject base) {
        if ("nbfs".equals (ref.getProtocol ())) { // NOI18N
            FileObject[] f = URLMapper.findFileObjects(ref);
            if (f.length > 0) {
                return f[0];
            }
        }

        return resolveRelativeName (ref.getPath (), base);
    }

    public URL translateURL (URL ref, FileObject base) {
        if (!"nbfs".equals (ref.getProtocol ())) { // NOI18N
            return ref;
        }

        FileObject[] f = URLMapper.findFileObjects(ref);
        if (f.length == 0) {
            return ref;
        }

        try {
            return createRelativeURL (f[0], base);
        } catch (MalformedURLException e) {
            org.openide.ErrorManager.getDefault().notify(e);
        }
        return ref;
    }

    private FileObject resolveRelativeName(String relPath, FileObject base) {
        if (relPath.length() == 0) {
            // relative point to the base file itself.
            return base;
        }

        if (relPath.startsWith (NAME_DELIMITER)) {
            try {
                base = base.getFileSystem().getRoot();
            } catch (IOException ex) {
                return null;
            }
        } else {
            base = base.getParent ();
        }

        StringTokenizer t = new StringTokenizer (relPath, NAME_DELIMITER);

        while (t.hasMoreElements () && base != null) {
            String token = t.nextToken ();

            // ignore double delimiters
            if (token.length () == 0)
                continue;

            if (token.equals (DOT))
                continue;

            if (token.equals (DOUBLE_DOT)) {
                base = base.getParent ();
            } else {
                int index = token.lastIndexOf (DOT);
                if (index != -1)
                    base = base.getFileObject (token.substring (0, index), token.substring (index + 1));
                else
                    base = base.getFileObject (token);
            }
        }

        return base;
    }

    private URL createRelativeURL (FileObject to, FileObject from) throws MalformedURLException {
        if (to.equals(from))
            return new URL ("file:."); // NOI18N
        LinkedList l1 = new LinkedList();
        LinkedList l2 = new LinkedList();

        createPath(from, l1);
        createPath(to, l2);

        int s1 = l1.size();
        //int s2 = l2.size();
        int skip = 0;

        Iterator it1 = l1.iterator();
        Iterator it2 = l2.iterator();
        FileObject f1 = null;
        FileObject f2 = null;

        for (;it1.hasNext() && it2.hasNext(); skip++) {
            f1 = (FileObject)it1.next();
            f2 = (FileObject)it2.next();
            if (f1 != f2)
                break;
        }
        StringBuffer relName = new StringBuffer(50);
        while (skip < s1 - 1) {
            relName.append("../"); // NOI18N
            skip++;
        }
        if (f2 != null) {
            relName.append(f2.getNameExt());
        }
        while (it2.hasNext()) {
            f2 = (FileObject)it2.next();
            relName.append('/');
            relName.append(f2.getNameExt());
        }

        return new URL ("file:" + relName.toString()); // NOI18N
    }

    private void createPath(FileObject f, LinkedList l) {
        while (!f.isRoot()) {
            l.addFirst(f);
            f = f.getParent();
        }
    }

    public void addPropertyChangeListener (PropertyChangeListener l) {
    }
    public void removePropertyChangeListener (PropertyChangeListener l) {
    }
}
