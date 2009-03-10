/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.loaders;

import java.io.*;
import java.util.*;

import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;

/** Property class that collects a modifiable list of file extensions
* and permits checking of whether a name or a file object has a given extension.
* It comes with a property editor to allow the user to modify the extensions.
*
* @author Jaroslav Tulach
*/
public class ExtensionList extends Object
    implements Cloneable, java.io.Serializable {
    
    /** if true, ignore case of file extensions (not MIME types tho!) */
    private static final boolean CASE_INSENSITIVE =
        Utilities.isWindows() || Utilities.getOperatingSystem() == Utilities.OS_VMS;
    
    /** set of extensions to recognize */
    private TreeSet list;
    /** set of mime types to recognize */
    private TreeSet mimeTypes;

    static final long serialVersionUID =8868581349510386291L;
    /** Default constructor.
    */
    public ExtensionList () {
    }

    /** Clone new object.
    */
    public synchronized Object clone () {
        try {
            ExtensionList l = (ExtensionList)super.clone ();
            
            if (list != null) {
                l.list = createExtensionSet ();
                l.list.addAll (list);
            }
            
            l.mimeTypes = mimeTypes == null ? null : (TreeSet)mimeTypes.clone ();
            
            return l;
        } catch (CloneNotSupportedException ex) {
            // has to be supported we implement the right interface
            throw new InternalError ();
        }
    }

    /** Add a new extension.
    * @param ext the extension
    */
    public synchronized void addExtension (String ext) {
        if (list == null) {
            list = createExtensionSet ();
        }
        
        list.add (ext);
    }

    /** Remove an extension.
    * @param ext the extension
    */
    public void removeExtension (String ext) {
        if (list != null) {
            list.remove (ext);
        }
    }
    
    /** Adds new mime type.
    * @param mime the mime type
    */
    public synchronized void addMimeType (String mime) {
        if (mimeTypes == null) {
            mimeTypes = new TreeSet ();
        }

        mimeTypes.add (mime);
    }
    
    /** Removes a mime type.
     * @param mime the name of the type
     */
    public void removeMimeType (String mime) {
        if (mimeTypes != null) {
            mimeTypes.remove(mime);
        }
    }

    /** Test whether the name in the string is acceptable.
    * It should end with a dot and be one of the registered extenstions.
    * @param s the name
    * @return <CODE>true</CODE> if the name is acceptable
    */
    public boolean isRegistered (String s) {
        if (list == null) {
            return false;
        }
      
        try {
            String ext = s.substring (s.lastIndexOf ('.') + 1);
            return list.contains (ext);
        } catch (StringIndexOutOfBoundsException ex) {
            return false;
        }
    }

    /** Tests whether the file object is acceptable.
    * Its extension should be registered.
    * @param fo the file object to test
    * @return <CODE>true</CODE> if the file object is acceptable
    */
    public boolean isRegistered (FileObject fo) {
        if (list != null && list.contains (fo.getExt ())) {
            return true;
        }

        if (mimeTypes != null && mimeTypes.contains(fo.getMIMEType())) {
            return true;
        }
        
        return false;
    }

    /** Get all extensions.
    * @return enumeration of <CODE>String</CODE>s
    */
    public Enumeration extensions () {
        return en (list);
    }
    
    /** Get all mime types.
     * @return enumeration of <CODE>String</CODE>s
     */
    public Enumeration mimeTypes () {
        return en (mimeTypes);
    }
    
    public String toString() {
        return "ExtensionList[" + list + mimeTypes + "]"; // NOI18N
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ExtensionList)) return false;
        ExtensionList e = (ExtensionList)o;
        return equalSets(list, e.list, CASE_INSENSITIVE) &&
               equalSets(mimeTypes, e.mimeTypes, false);
    }
    
    public int hashCode() {
        int x = 0;
        if (list != null) x = normalizeSet(list, CASE_INSENSITIVE).hashCode();
        if (mimeTypes != null) x += normalizeSet(mimeTypes, false).hashCode();
        return x;
    }
    
    // Helper methods for equals/hashCode.
    // Note that these are unsorted sets; we don't care about order.
    private static boolean equalSets(Set s1, Set s2, boolean flattenCase) {
        if (s1 == null && s2 == null) return true; // quick return
        Set s1a = normalizeSet(s1, flattenCase);
        Set s2a = normalizeSet(s2, flattenCase);
        return s1a.equals(s2a);
    }
    private static Set normalizeSet(Set s, boolean flattenCase) {
        if (s == null || s.isEmpty()) return Collections.EMPTY_SET;
        if (flattenCase) {
            Set s2 = new HashSet(s.size() * 4 / 3 + 1);
            Iterator it = s.iterator();
            while (it.hasNext()) {
                s2.add(((String)it.next()).toLowerCase(Locale.US));
            }
            return s2;
        } else {
            return s;
        }
    }
    
    /** Enumeration from set
     * @param set set or null
     */
    private static Enumeration en (Collection c) {
        if (c == null) {
            return org.openide.util.enums.EmptyEnumeration.EMPTY;
        } else {
            return Collections.enumeration(c);
        }
    }
    
    /** Creates a set for holding the extensions. It is platform 
    * dependent whether case sensitive or insensitive.
    */
    private static TreeSet createExtensionSet () {
        if (CASE_INSENSITIVE) {
            return new TreeSet (String.CASE_INSENSITIVE_ORDER);
        } else {
            return new TreeSet ();
        }
    }
    
    /** Backward compatibility settings read.
    */
    private void readObject (ObjectInputStream ois) 
    throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField gf = ois.readFields();
        
        Object list = gf.get ("list", null); // NOI18N
        if (list instanceof Map) {
            // backward compatible serialization
            list = ((Map)list).keySet ();
        }
        
        if (list != null) {
            // have to reinsert everything because we could migrate from
            // different operating system and we might need to change
            // case-sensitivity
            this.list = createExtensionSet ();
            this.list.addAll ((Set)list);
        }
        
        this.mimeTypes = (TreeSet)gf.get ("mimeTypes", null); // NOI18N
    }
}
