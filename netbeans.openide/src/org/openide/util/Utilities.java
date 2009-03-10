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

package org.openide.util;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.lang.reflect.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.net.MalformedURLException;
import java.text.BreakIterator;
import java.net.URL;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.Timer;


import org.openide.ErrorManager;
import org.openide.modules.Dependency;
import org.openide.modules.SpecificationVersion;
import org.openide.util.ContextAwareAction;

/** Otherwise uncategorized useful static methods.
*
* @author Jan Palka, Ian Formanek, Jaroslav Tulach
*/
public final class Utilities {
    private Utilities() {}

    /** Operating system is Windows NT. */
    public static final int OS_WINNT = 1;
    /** Operating system is Windows 95. */
    public static final int OS_WIN95 = 2;
    /** Operating system is Windows 98. */
    public static final int OS_WIN98 = 4;
    /** Operating system is Solaris. */
    public static final int OS_SOLARIS = 8;
    /** Operating system is Linux. */
    public static final int OS_LINUX = 16;
    /** Operating system is HP-UX. */
    public static final int OS_HP = 32;
    /** Operating system is IBM AIX. */
    public static final int OS_AIX = 64;
    /** Operating system is SGI IRIX. */
    public static final int OS_IRIX = 128;
    /** Operating system is Sun OS. */
    public static final int OS_SUNOS = 256;
    /** Operating system is Compaq TRU64 Unix */
    public static final int OS_TRU64 = 512;
    /** @deprecated please use OS_TRU64 instead */
    public static final int OS_DEC = OS_TRU64;
    /** Operating system is OS/2. */
    public static final int OS_OS2 = 1024;
    /** Operating system is Mac. */
    public static final int OS_MAC = 2048;
    /** Operating system is Windows 2000. */
    public static final int OS_WIN2000 = 4096;
    /** Operating system is Compaq OpenVMS */
    public static final int OS_VMS = 8192;
    /**
     *Operating system is one of the Windows variants but we don't know which
     *one it is
     */
    public static final int OS_WIN_OTHER = 16384;
    
    /** Operating system is unknown. */
    public static final int OS_OTHER = 65536;

    /** A mask for Windows platforms. */
    public static final int OS_WINDOWS_MASK = OS_WINNT | OS_WIN95 | OS_WIN98 | OS_WIN2000 | OS_WIN_OTHER;
    /** A mask for Unix platforms. */
    public static final int OS_UNIX_MASK = OS_SOLARIS | OS_LINUX | OS_HP | OS_AIX | OS_IRIX | OS_SUNOS | OS_TRU64 | OS_MAC;

    /** A height of the windows's taskbar */
    public static final int TYPICAL_WINDOWS_TASKBAR_HEIGHT = 27;

    /** A height of the Mac OS X's menu */
    private static final int TYPICAL_MACOSX_MENU_HEIGHT = 24;
    
    /** variable holding the activeReferenceQueue */
    private static ReferenceQueue activeReferenceQueue;
    
    /** Useful queue for all parts of system that use <code>java.lang.ref.Reference</code>s
     * together with some <code>ReferenceQueue</code> and need to do some clean up
     * when the reference is enqued. Usually, in order to be notified about that, one 
     * needs to either create a dedicated thread that blocks on the queue and is 
     * <code>Object.notify</code>-ed, which is the right approach but consumes 
     * valuable system resources (threads) or one can peridically check the content
     * of the queue by <code>RequestProcessor.Task.schedule</code> which is 
     * completelly wrong, because it wakes up the system every (say) 15 seconds.
     * In order to provide useful support for this problem, this queue has been
     * provided.
     * <P>
     * If you have a reference that needs clean up, make it implement <link>Runnable</link>
     * inteface and register it with the <code>activeReferenceQueue</code>:
     * <PRE>
     * class MyReference extends WeakReference implements Runnable {
     *   private Object dataToCleanUp;
     *
     *   public MyReference (Object ref, Object data) {
     *     super (ref, Utilities.activeReferenceQueue ()); // here you specify the queue
     *     dataToCleanUp = data;
     *   }
     *
     *   public void run () {
     *     // clean up your data
     *   }
     * }
     * </PRE>
     * When the <code>ref</code> object is garbage collected, your run method
     * will be invoked by calling 
     * <code>((Runnable)reference).run ()</code>
     * and you can perform what ever cleanup is necessary. Be sure not to block
     * in such cleanup for a long time as this prevents other waiting references 
     * to cleanup themselves.
     * <P>
     * Please do not call any methods of the ReferenceQueue yourself. They
     * will throw exceptions.
     *
     * @since 3.11
     */
    public static synchronized ReferenceQueue activeReferenceQueue () {
        if (activeReferenceQueue == null) {
            activeReferenceQueue = new ActiveQueue (false);
        }
        return activeReferenceQueue;
    }
    
    /** reference to map that maps allowed key names to their values (String, Integer) 
    and reference to map for mapping of values to their names */
    private static Reference namesAndValues;

    /** Get the operating system on which the IDE is running.
    * @return one of the <code>OS_*</code> constants (such as {@link #OS_WINNT})
    */
    public static final int getOperatingSystem () {
        if (operatingSystem == -1) {
            String osName = System.getProperty ("os.name");
            if ("Windows NT".equals (osName)) // NOI18N
                operatingSystem = OS_WINNT;
            else if ("Windows 95".equals (osName)) // NOI18N
                operatingSystem = OS_WIN95;
            else if ("Windows 98".equals (osName)) // NOI18N
                operatingSystem = OS_WIN98;
            else if ("Windows 2000".equals (osName)) // NOI18N
                operatingSystem = OS_WIN2000;
            else if (osName.startsWith("Windows ")) // NOI18N
                operatingSystem = OS_WIN_OTHER;
            else if ("Solaris".equals (osName)) // NOI18N
                operatingSystem = OS_SOLARIS;
            else if (osName.startsWith ("SunOS")) // NOI18N
                operatingSystem = OS_SOLARIS;
            // JDK 1.4 b2 defines os.name for me as "Redhat Linux" -jglick
            else if (osName.endsWith ("Linux")) // NOI18N
                operatingSystem = OS_LINUX;
            else if ("HP-UX".equals (osName)) // NOI18N
                operatingSystem = OS_HP;
            else if ("AIX".equals (osName)) // NOI18N
                operatingSystem = OS_AIX;
            else if ("Irix".equals (osName)) // NOI18N
                operatingSystem = OS_IRIX;
            else if ("SunOS".equals (osName)) // NOI18N
                operatingSystem = OS_SUNOS;
            else if ("Digital UNIX".equals (osName)) // NOI18N
                operatingSystem = OS_TRU64;
            else if ("OS/2".equals (osName)) // NOI18N
                operatingSystem = OS_OS2;
            else if ("OpenVMS".equals (osName)) // NOI18N
                operatingSystem = OS_VMS;
            else if (osName.equals ("Mac OS X")) // NOI18N
                operatingSystem = OS_MAC;
            else if (osName.startsWith ("Darwin")) // NOI18N
                operatingSystem = OS_MAC;
            else
                operatingSystem = OS_OTHER;
        }
        return operatingSystem;
    }

    /** Test whether the IDE is running on some variant of Windows.
    * @return <code>true</code> if Windows, <code>false</code> if some other manner of operating system
    */
    public static final boolean isWindows () {
        return (getOperatingSystem () & OS_WINDOWS_MASK) != 0;
    }

    /** Test whether the IDE is running on some variant of Unix.
    * Linux is included as well as the commercial vendors.
    * @return <code>true</code> some sort of Unix, <code>false</code> if some other manner of operating system
    */
    public static final boolean isUnix () {
        return (getOperatingSystem () & OS_UNIX_MASK) != 0;
    }

    /** The operating system on which NetBeans runs*/
    private static int operatingSystem = -1;

    /** Hashtable contains keywords. It is forbidden to use this
        keywords as a java identifier */
    private static Reference keywords;

    private static synchronized HashMap keywords () {
        if (keywords != null) {
            HashMap map = (HashMap)keywords.get ();
            if (map != null) {
                return map;
            }
        }

        HashMap keywords = new HashMap (71);
        keywords.put("abstract","abstract"); keywords.put("default","default"); // NOI18N
        keywords.put("if","if"); keywords.put("private","private"); // NOI18N
        keywords.put("throw","throw"); keywords.put("boolean","boolean"); // NOI18N
        keywords.put("do","do"); keywords.put("implements","implements"); // NOI18N
        keywords.put("protected","protected"); keywords.put("throws","throws"); // NOI18N
        keywords.put("break","break"); keywords.put("double","double"); // NOI18N
        keywords.put("import","import"); keywords.put("public","public"); // NOI18N
        keywords.put("transient","transient");keywords.put("byte","byte"); // NOI18N
        keywords.put("else","else");keywords.put("instanceof","instanceof"); // NOI18N
        keywords.put("return","return");keywords.put("try","try"); // NOI18N
        keywords.put("case","case");keywords.put("extends","extends"); // NOI18N
        keywords.put("int","int");keywords.put("short","short"); // NOI18N
        keywords.put("void","void");keywords.put("catch","catch"); // NOI18N
        keywords.put("final","final");keywords.put("interface","interface"); // NOI18N
        keywords.put("static","static");keywords.put("volatile","volatile"); // NOI18N
        keywords.put("char","char");keywords.put("finally","finally"); // NOI18N
        keywords.put("long","long");keywords.put("class","class"); // NOI18N
        keywords.put("while","while");keywords.put("super","super"); // NOI18N
        keywords.put("float","float");keywords.put("native","native"); // NOI18N
        keywords.put("switch","switch");keywords.put("const","const"); // NOI18N
        keywords.put("for","for");keywords.put("new","new"); // NOI18N
        keywords.put("synchronized","synchronized");keywords.put("continue","continue"); // NOI18N
        keywords.put("continue","continue");keywords.put("goto","goto"); // NOI18N
        keywords.put("package","package");keywords.put("this","this"); // NOI18N
        keywords.put("null","null");keywords.put("true","true"); // NOI18N
        keywords.put("false","false"); // NOI18N
        keywords.put("assert", "assert"); //JDK 1.4 NOI18N

        Utilities.keywords = new SoftReference (keywords);

        return keywords;
    }


    /** Test whether a given string is a valid Java identifier.
    * @param id string which should be checked
    * @return <code>true</code> if a valid identifier
    */
    public static final boolean isJavaIdentifier(String id) {
        if (id == null) return false;
        if (id.equals("")) return false; // NOI18N
        if (!(java.lang.Character.isJavaIdentifierStart(id.charAt(0))) )
            return false;
        for (int i = 1; i < id.length(); i++) {
            if (!(java.lang.Character.isJavaIdentifierPart(id.charAt(i))) )
                return false;
        }
        // test if id is a keyword
        if (keywords ().containsKey(id)) return false;

        return true;
    }

    /** Central method for obtaining <code>BeanInfo</code> for potential JavaBean classes.
    * @param clazz class of the bean to provide the <code>BeanInfo</code> for
    * @return the bean info
    * @throws java.beans.IntrospectionException for the usual reasons
    * @see java.beans.Introspector#getBeanInfo(Class)
    */
    public static java.beans.BeanInfo getBeanInfo(Class clazz) throws java.beans.IntrospectionException {
        java.beans.BeanInfo bi;
        try {
            bi = java.beans.Introspector.getBeanInfo(clazz);
        } catch (java.beans.IntrospectionException ie) {
            ErrorManager.getDefault().annotate(ie, ErrorManager.UNKNOWN, "Encountered while introspecting " + clazz.getName(), null, null, null); // NOI18N
            throw ie;
        } catch (Error e) {
            // Could be a bug in Introspector triggered by NB code.
            ErrorManager.getDefault().annotate(e, ErrorManager.UNKNOWN, "Encountered while introspecting " + clazz.getName(), null, null, null); // NOI18N
            throw e;
        }
        if (java.awt.Component.class.isAssignableFrom (clazz)) {
            java.beans.PropertyDescriptor[] pds = bi.getPropertyDescriptors ();
            for (int i = 0; i < pds.length; i++) {
                if (pds[i].getName ().equals ("cursor")) { // NOI18N
                    try {
                        Method getter = Component.class.getDeclaredMethod ("getCursor", new Class[0]); // NOI18N
                        Method setter = Component.class.getDeclaredMethod ("setCursor", new Class[] { Cursor.class }); // NOI18N
                        pds[i] = new java.beans.PropertyDescriptor ("cursor", getter, setter); // NOI18N
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // clears about 1000 instances of Method
        if (bi != null) {
            if (clearIntrospector == null) {
                doClear = new ActionListener() {
                              public void actionPerformed(ActionEvent ev) {
                                  java.beans.Introspector.flushCaches();
                              }
                          };
                clearIntrospector = new Timer(15000, doClear);
                clearIntrospector.setRepeats(false);
            }
            clearIntrospector.restart();
        }
        return bi;
    }

    private static Timer clearIntrospector;
    private static ActionListener doClear;

    /** Central method for obtaining <code>BeanInfo</code> for potential JavaBean classes, with a stop class.
    * @param clazz class of the bean to provide the <code>BeanInfo</code> for
    * @param stopClass the stop class
    * @return the bean info
    * @throws java.beans.IntrospectionException for the usual reasons
    * @see java.beans.Introspector#getBeanInfo(Class, Class)
    */
    public static java.beans.BeanInfo getBeanInfo (Class clazz, Class stopClass) throws java.beans.IntrospectionException {
        return java.beans.Introspector.getBeanInfo(clazz, stopClass);
    }
    
    /** Wrap multi-line strings (and get the individual lines).
    * @param original  the original string to wrap
    * @param width     the maximum width of lines
    * @param wrapWords if <code>true</code>, the lines are wrapped on word boundaries (if possible);
    *                  if <code>false</code>, character boundaries are used
    * @param removeNewLines if <code>true</code>, any newlines in the original string are ignored
    * @return the lines after wrapping
    * @deprecated use {@link #wrapStringToArray(String, int, BreakIterator, boolean)} since it is better for I18N
    */
    public static String[] wrapStringToArray(String original, int width, boolean wrapWords, boolean removeNewLines) {
        BreakIterator bi = (wrapWords ? BreakIterator.getWordInstance() : BreakIterator.getCharacterInstance());
        return wrapStringToArray(original, width, bi, removeNewLines);
    }
    
    /** Wrap multi-line strings (and get the individual lines).
    * @param original  the original string to wrap
    * @param width     the maximum width of lines
    * @param breakIterator breaks original to chars, words, sentences, depending on what instance you provide.
    * @param removeNewLines if <code>true</code>, any newlines in the original string are ignored
    * @return the lines after wrapping
    */
    public static String[] wrapStringToArray(String original, int width, BreakIterator breakIterator, boolean removeNewLines) {
        
        if (original.length() == 0) {
            return new String[] { original };
        }
        
        String[] workingSet;
        
        // substitute original newlines with spaces,
        // remove newlines from head and tail
        if (removeNewLines) {
            original = trimString(original);
            original = original.replace ('\n', ' ');
            workingSet = new String[] { original };
        } else {
            StringTokenizer tokens = new StringTokenizer(original, "\n"); // NOI18N
            int len = tokens.countTokens();
            workingSet = new String[len];
            
            for (int i = 0; i < len; i++) {
                workingSet[i] = tokens.nextToken();
            }
        }

        if (width < 1) width = 1;
        if (original.length () <= width) {
            return workingSet;
        }
        
widthcheck: {
            boolean ok = true;
            for (int i = 0; i < workingSet.length; i++) {
                ok = ok && (workingSet[i].length() < width);
                if (!ok) {
                    break widthcheck;
                }
            }
            
            return workingSet;
        }
        
        java.util.ArrayList lines = new java.util.ArrayList();
        
        int lineStart = 0; // the position of start of currently processed line in the original string
        for (int i = 0; i < workingSet.length; i++) {
            if (workingSet[i].length() < width) {
                lines.add(workingSet[i]);
            } else {
                breakIterator.setText(workingSet[i]);
                int nextStart = breakIterator.next();
                int prevStart = 0;
                do {
                    while (((nextStart - lineStart) < width) && (nextStart != BreakIterator.DONE)) {
                        prevStart = nextStart;
                        nextStart = breakIterator.next();
                    }
                    
                    if (nextStart == BreakIterator.DONE) {
                        nextStart = prevStart = workingSet[i].length();
                    }
                    
                    if (prevStart == 0) {
                        prevStart = nextStart;
                    }
                    
                    lines.add(workingSet[i].substring(lineStart, prevStart));
                    
                    lineStart = prevStart;
                    prevStart = 0;
                } while (lineStart < workingSet[i].length());
                
                lineStart = 0;
            }
        }

        String s[] = new String [lines.size()];
        return (String[]) lines.toArray(s);
    }
    
    /** trims String
    * @param s a String to trim
    * @return trimmed String
    */
    private static String trimString(String s) {
        int idx = 0;
        char c;
        final int slen = s.length();

        if (slen == 0) {
            return s;
        }

        do {
            c = s.charAt(idx++);
        } while ((c == '\n' || c == '\r') &&
                 (idx < slen));

        s = s.substring(--idx);
        idx = s.length() - 1;

        if (idx < 0) {
            return s;
        }

        do {
            c = s.charAt(idx--);
        } while ((c == '\n' || c == '\r') &&
                 (idx >= 0));

        return s.substring(0, idx + 2);
    }

    /** Wrap multi-line strings.
    * @param original  the original string to wrap
    * @param width     the maximum width of lines
    * @param breakIterator algorithm for breaking lines
    * @param removeNewLines if <code>true</code>, any newlines in the original string are ignored
    * @return the whole string with embedded newlines
    */
    public static String wrapString (String original, int width, BreakIterator breakIterator, boolean removeNewLines) {
        
        String[] sarray = wrapStringToArray(original, width, breakIterator, removeNewLines);
        StringBuffer retBuf = new StringBuffer ();
        
        for (int i = 0; i < sarray.length; i++) {
            retBuf.append (sarray[i]);
            retBuf.append ('\n');
        }
        return retBuf.toString ();
    }

    /** Wrap multi-line strings.
    * @param original  the original string to wrap
    * @param width     the maximum width of lines
    * @param wrapWords if <code>true</code>, the lines are wrapped on word boundaries (if possible);
    *                  if <code>false</code>, character boundaries are used
    * @param removeNewLines if <code>true</code>, any newlines in the original string are ignored
    * @return the whole string with embedded newlines
    * @deprecated Use {@link #wrapString (String, int, BreakIterator, boolean)} as it is friendlier to I18N.
    */
    public static String wrapString (String original, int width, boolean wrapWords, boolean removeNewLines) {
        // substitute original newlines with spaces,
        // remove newlines from head and tail
        if (removeNewLines) {
            while (original.startsWith ("\n")) // NOI18N
                original = original.substring (1);
            while (original.endsWith ("\n")) // NOI18N
                original = original.substring (0, original.length () - 1);
            original = original.replace ('\n', ' ');
        }

        if (width < 1) width = 1;
        if (original.length () <= width) return original;

        java.util.Vector lines = new java.util.Vector ();
        int lineStart = 0; // the position of start of currently processed line in the original string
        int lastSpacePos = -1;
        for (int i = 0; i < original.length (); i++) {
            if (lineStart >= original.length () - 1)
                break;

            // newline in the original string
            if (original.charAt (i) == '\n') {
                lines.addElement (original.substring (lineStart, i));
                lineStart = i+1;
                lastSpacePos = -1;
                continue;
            }

            // remember last space position
            if (Character.isSpaceChar (original.charAt (i)))
                lastSpacePos = i;

            // last position in the original string
            if (i == original.length () - 1) {
                lines.addElement (original.substring (lineStart));
                break;
            }

            // reached width
            if (i - lineStart == width) {
                if (wrapWords && (lastSpacePos != -1)) {
                    lines.addElement (original.substring (lineStart, lastSpacePos));
                    lineStart = lastSpacePos + 1; // the space is consumed for the newline
                    lastSpacePos = -1;
                } else {
                    lines.addElement (original.substring (lineStart, i));
                    lineStart = i;
                    lastSpacePos = -1;
                }
            }
        }

        StringBuffer retBuf = new StringBuffer ();
        for (java.util.Enumeration e = lines.elements (); e.hasMoreElements ();) {
            retBuf.append ((String) e.nextElement ());
            retBuf.append ('\n');
        }
        return retBuf.toString ();
    }

    /** Search-and-replace fixed string matches within a string.
    * @param original the original string
    * @param replaceFrom the substring to be find
    * @param replaceTo the substring to replace it with
    * @return a new string with all occurrences replaced
    */
    public static String replaceString (String original, String replaceFrom, String replaceTo) {
        int index = 0;
        if ("".equals (replaceFrom)) return original; // NOI18N

        StringBuffer buf = new StringBuffer ();
        while (true) {
            int pos = original.indexOf (replaceFrom, index);
            if (pos == -1) {
                buf.append (original.substring (index));
                return buf.toString ();
            }
            buf.append (original.substring (index, pos));
            buf.append (replaceTo);
            index = pos + replaceFrom.length ();
            if (index == original.length ())
                return buf.toString ();
        }
    }

    /** Turn full name of an inner class into its pure form.
    * @param fullName e.g. <code>some.pkg.SomeClass$Inner</code>
    * @return e.g. <code>Inner</code>
    */
    public static final String pureClassName (final String fullName) {
        final int index = fullName.indexOf('$');
        if ((index >= 0) && (index < fullName.length()))
            return fullName.substring(index+1, fullName.length());
        return fullName;
    }

    /** Test whether the operating system supports icons on frames (windows).
    * @return <code>true</code> if it does <em>not</em>
    *
    */
    public static final boolean isLargeFrameIcons() {
        return (getOperatingSystem () == OS_SOLARIS) || (getOperatingSystem () == OS_HP);
    }

    /** Compute hash code of array.
    * Asks all elements for their own code and composes the
    * values.
    * @param arr array of objects, can contain <code>null</code>s
    * @return the hash code
    * @see Object#hashCode
    */
    public static int arrayHashCode (Object[] arr) {
        int c = 0;
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            Object o = arr[i];
            int v = o == null ? 1 : o.hashCode ();
            c += (v ^ i);
        }
        return c;
    }

    /** Safe equality check.
    * The supplied objects are equal if: <UL>
    * <LI> both are <code>null</code>
    * <LI> both are arrays with same length and equal items (if the items are arrays,
    *      they are <em>not</em> checked the same way again)
    * <LI> the two objects are {@link Object#equals}
    * </UL>
    * This method is <code>null</code>-safe, so if one of the parameters is true and the second not,
    * it returns <code>false</code>.
    * @param  o1 the first object to compare
    * @param  o2 the second object to compare
    * @return <code>true</code> if the objects are equal
    */
    public static boolean compareObjects (Object o1, Object o2) {
        return compareObjectsImpl (o1, o2, 1);
    }

    /** Safe equality check with array recursion.
    * @param  o1 the first object to compare
    * @param  o2 the second object to compare
    * @param  checkArraysDepth the depth to which arrays should be compared for equality (negative for infinite depth, zero for no comparison of elements, one for shallow, etc.)
    * @return <code>true</code> if the objects are equal
    * @see #compareObjects(Object, Object)
    */
    public static boolean compareObjectsImpl (Object o1, Object o2, int checkArraysDepth) {
        // handle null values
        if (o1 == null)
            return (o2 == null);
        else if (o2 == null) return false;

        // handle arrays
        if (checkArraysDepth > 0) {
            if ((o1 instanceof Object[]) && (o2 instanceof Object[])) {
                // Note: also handles multidimensional arrays of primitive types correctly.
                // I.e. new int[0][] instanceof Object[]
                Object[] o1a = (Object[]) o1;
                Object[] o2a = (Object[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++) {
                    if (! compareObjectsImpl (o1a[i], o2a[i], checkArraysDepth - 1)) {
                        return false;
                    }
                }
                return true;
            } else if ((o1 instanceof byte[]) && (o2 instanceof byte[])) {
                byte[] o1a = (byte[]) o1;
                byte[] o2a = (byte[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof short[]) && (o2 instanceof short[])) {
                short[] o1a = (short[]) o1;
                short[] o2a = (short[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof int[]) && (o2 instanceof int[])) {
                int[] o1a = (int[]) o1;
                int[] o2a = (int[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof long[]) && (o2 instanceof long[])) {
                long[] o1a = (long[]) o1;
                long[] o2a = (long[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof float[]) && (o2 instanceof float[])) {
                float[] o1a = (float[]) o1;
                float[] o2a = (float[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof double[]) && (o2 instanceof double[])) {
                double[] o1a = (double[]) o1;
                double[] o2a = (double[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof char[]) && (o2 instanceof char[])) {
                char[] o1a = (char[]) o1;
                char[] o2a = (char[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            } else if ((o1 instanceof boolean[]) && (o2 instanceof boolean[])) {
                boolean[] o1a = (boolean[]) o1;
                boolean[] o2a = (boolean[]) o2;
                int l1 = o1a.length;
                int l2 = o2a.length;
                if (l1 != l2) return false;
                for (int i = 0; i < l1; i++)
                    if (o1a[i] != o2a[i]) return false;
                return true;
            }
            // else not array type
        }

        // handle common objects--non-arrays, or arrays when depth == 0
        return o1.equals (o2);
    }

    /** Assemble a human-presentable class name for a specified class.
    * Arrays are represented as e.g. <code>java.lang.String[]</code>.
    * @param clazz the class to name
    * @return the human-presentable name
    */
    public static String getClassName (Class clazz) {
        // if it is an array, get short name of element type and append []
        if (clazz.isArray ())
            return getClassName (clazz.getComponentType ()) + "[]"; // NOI18N
        else
            return clazz.getName ();
    }

    /** Assemble a human-presentable class name for a specified class (omitting the package).
    * Arrays are represented as e.g. <code>String[]</code>.
    * @param clazz the class to name
    * @return the human-presentable name
    */
    public static String getShortClassName (Class clazz) {
        // if it is an array, get short name of element type and append []
        if (clazz.isArray ())
            return getShortClassName (clazz.getComponentType ()) + "[]"; // NOI18N

        String name = clazz.getName ().replace ('$', '.');
        return name.substring (name.lastIndexOf (".") + 1, name.length ()); // NOI18N
    }

    /**
    * Convert an array of objects to an array of primitive types.
    * E.g. an <code>Integer[]</code> would be changed to an <code>int[]</code>.
    * @param array the wrapper array
    * @return a primitive array
    * @throws IllegalArgumentException if the array element type is not a primitive wrapper
    */
    public static Object toPrimitiveArray (Object[] array) {
        if (array instanceof Integer[]) {
            int[] r = new int [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Integer)array[i]) == null) ? 0 : ((Integer)array[i]).intValue ();
            return r;
        }
        if (array instanceof Boolean[]) {
            boolean[] r = new boolean [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Boolean)array[i]) == null) ? false : ((Boolean)array[i]).booleanValue ();
            return r;
        }
        if (array instanceof Byte[]) {
            byte[] r = new byte [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Byte)array[i]) == null) ? 0 : ((Byte)array[i]).byteValue ();
            return r;
        }
        if (array instanceof Character[]) {
            char[] r = new char [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Character)array[i]) == null) ? 0 : ((Character)array[i]).charValue ();
            return r;
        }
        if (array instanceof Double[]) {
            double[] r = new double [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Double)array[i]) == null) ? 0 : ((Double)array[i]).doubleValue ();
            return r;
        }
        if (array instanceof Float[]) {
            float[] r = new float [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Float)array[i]) == null) ? 0 : ((Float)array[i]).floatValue ();
            return r;
        }
        if (array instanceof Long[]) {
            long[] r = new long [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Long)array[i]) == null) ? 0 : ((Long)array[i]).longValue ();
            return r;
        }
        if (array instanceof Short[]) {
            short[] r = new short [array.length];
            int i, k = array.length;
            for (i = 0; i < k; i++) r [i] = (((Short)array[i]) == null) ? 0 : ((Short)array[i]).shortValue ();
            return r;
        }
        throw new IllegalArgumentException ();
    }

    /**
    * Convert an array of primitive types to an array of objects.
    * E.g. an <code>int[]</code> would be turned into an <code>Integer[]</code>.
    * @param array the primitive array
    * @return a wrapper array
    * @throws IllegalArgumentException if the array element type is not primitive
    */
    public static Object[] toObjectArray (Object array) {
        if (array instanceof Object[]) return (Object[]) array;
        if (array instanceof int[]) {
            int i, k = ((int[])array).length;
            Integer[] r = new Integer [k];
            for (i = 0; i < k; i++) r [i] = new Integer (((int[]) array)[i]);
            return r;
        }
        if (array instanceof boolean[]) {
            int i, k = ((boolean[])array).length;
            Boolean[] r = new Boolean [k];
            for (i = 0; i < k; i++) r [i] = ((boolean[]) array)[i] ? Boolean.TRUE : Boolean.FALSE;
            return r;
        }
        if (array instanceof byte[]) {
            int i, k = ((byte[])array).length;
            Byte[] r = new Byte [k];
            for (i = 0; i < k; i++) r [i] = new Byte (((byte[]) array)[i]);
            return r;
        }
        if (array instanceof char[]) {
            int i, k = ((char[])array).length;
            Character[] r = new Character [k];
            for (i = 0; i < k; i++) r [i] = new Character (((char[]) array)[i]);
            return r;
        }
        if (array instanceof double[]) {
            int i, k = ((double[])array).length;
            Double[] r = new Double [k];
            for (i = 0; i < k; i++) r [i] = new Double (((double[]) array)[i]);
            return r;
        }
        if (array instanceof float[]) {
            int i, k = ((float[])array).length;
            Float[] r = new Float [k];
            for (i = 0; i < k; i++) r [i] = new Float (((float[]) array)[i]);
            return r;
        }
        if (array instanceof long[]) {
            int i, k = ((long[])array).length;
            Long[] r = new Long [k];
            for (i = 0; i < k; i++) r [i] = new Long (((long[]) array)[i]);
            return r;
        }
        if (array instanceof short[]) {
            int i, k = ((short[])array).length;
            Short[] r = new Short [k];
            for (i = 0; i < k; i++) r [i] = new Short (((short[]) array)[i]);
            return r;
        }
        throw new IllegalArgumentException ();
    }

    /**
    * Get the object type for given primitive type.
    *
    * @param c primitive type (e.g. <code>int</code>)
    * @return object type (e.g. <code>Integer</code>)
    */
    public static Class getObjectType (Class c) {
        if (!c.isPrimitive ()) return c;
        if (c == Integer.TYPE) return Integer.class;
        if (c == Boolean.TYPE) return Boolean.class;
        if (c == Byte.TYPE) return Byte.class;
        if (c == Character.TYPE) return Character.class;
        if (c == Double.TYPE) return Double.class;
        if (c == Float.TYPE) return Float.class;
        if (c == Long.TYPE) return Long.class;
        if (c == Short.TYPE) return Short.class;
        throw new IllegalArgumentException ();
    }

    /**
    * Get the primitive type for given object type.
    *
    * @param c object type (e.g. <code>Integer</code>)
    * @return primitive type (e.g. <code>int</code>)
    */
    public static Class getPrimitiveType (Class c) {
        if (!c.isPrimitive ()) return c;
        if (c == Integer.class) return Integer.TYPE;
        if (c == Boolean.class) return Boolean.TYPE;
        if (c == Byte.class) return Byte.TYPE;
        if (c == Character.class) return Character.TYPE;
        if (c == Double.class) return Double.TYPE;
        if (c == Float.class) return Float.TYPE;
        if (c == Long.class) return Long.TYPE;
        if (c == Short.class) return Short.TYPE;
        throw new IllegalArgumentException ();
    }

    /** Find a focus-traverable component.
    * @param c the component to look in
    * @return the same component if traversable, else a child component if present, else <code>null</code>
    * @see Component#isFocusTraversable
    */
    public static Component getFocusTraversableComponent (Component c) {
        if (c.isFocusTraversable ()) return c;
        if (!(c instanceof Container)) return null;
        int i, k = ((Container)c).getComponentCount ();
        for (i = 0; i < k; i++) {
            Component v = ((Container)c).getComponent (i);
            if (v != null) return v;
        }
        return null;
    }

    /** Parses parameters from a given string in shell-like manner.
    * Users of the Bourne shell (e.g. on Unix) will already be familiar with the behavior.
    * For example, when using <code>org.openide.execution.NbProcessDescriptor</code> (Execution API)
    * you should be able to:
    * <ul>
    * <li>Include command names with embedded spaces, such as <code>c:\Program Files\jdk\bin\javac</code>.
    * <li>Include extra command arguments, such as <code>-Dname=value</code>.
    * <li>Do anything else which might require unusual characters or processing. For example:
    * <p><code><pre>
    * "c:\program files\jdk\bin\java" -Dmessage="Hello /\\/\\ there!" -Xmx128m
    * </pre></code>
    * <p>This example would create the following executable name and arguments:
    * <ol>
    * <li> <code>c:\program files\jdk\bin\java</code>
    * <li> <code>-Dmessage=Hello /\/\ there!</code>
    * <li> <code>-Xmx128m</code>
    * </ol>
    * Note that the command string does not escape its backslashes--under the assumption
    * that Windows users will not think to do this, meaningless escapes are just left
    * as backslashes plus following character.
    * </ul>
    * <em>Caveat</em>: even after parsing, Windows programs (such as the Java launcher)
    * may not fully honor certain
    * characters, such as quotes, in command names or arguments. This is because programs
    * under Windows frequently perform their own parsing and unescaping (since the shell
    * cannot be relied on to do this). On Unix, this problem should not occur.
    * @param s a string to parse
    * @return an array of parameters
    */
    public static String[] parseParameters(String s) {
        int NULL = 0x0;  // STICK + whitespace or NULL + non_"
        int INPARAM = 0x1; // NULL + " or STICK + " or INPARAMPENDING + "\ // NOI18N
        int INPARAMPENDING = 0x2; // INPARAM + \
        int STICK = 0x4; // INPARAM + " or STICK + non_" // NOI18N
        int STICKPENDING = 0x8; // STICK + \
        Vector params = new Vector(5,5);
        char c;

        int state = NULL;
        StringBuffer buff = new StringBuffer(20);
        int slength = s.length();
        for (int i = 0; i < slength; i++) {
            c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                if (state == NULL) {
                    if (buff.length () > 0) {
                        params.addElement(buff.toString());
                        buff.setLength(0);
                    }
                } else if (state == STICK) {
                    params.addElement(buff.toString());
                    buff.setLength(0);
                    state = NULL;
                } else if (state == STICKPENDING) {
                    buff.append('\\');
                    params.addElement(buff.toString());
                    buff.setLength(0);
                    state = NULL;
                } else if (state == INPARAMPENDING) {
                    state = INPARAM;
                    buff.append('\\');
                    buff.append(c);
                } else {    // INPARAM
                    buff.append(c);
                }
                continue;
            }

            if (c == '\\') {
                if (state == NULL) {
                    ++i;
                    if (i < slength) {
                        char cc = s.charAt(i);
                        if (cc == '"' || cc == '\\') {
                            buff.append(cc);
                        } else if (Character.isWhitespace(cc)) {
                            buff.append(c);
                            --i;
                        } else {
                            buff.append(c);
                            buff.append(cc);
                        }
                    } else {
                        buff.append('\\');
                        break;
                    }
                    continue;
                } else if (state == INPARAM) {
                    state = INPARAMPENDING;
                } else if (state == INPARAMPENDING) {
                    buff.append('\\');
                    state = INPARAM;
                } else if (state == STICK) {
                    state = STICKPENDING;
                } else if (state == STICKPENDING) {
                    buff.append('\\');
                    state = STICK;
                }
                continue;
            }

            if (c == '"') {
                if (state == NULL) {
                    state = INPARAM;
                } else if (state == INPARAM) {
                    state = STICK;
                } else if (state == STICK) {
                    state = INPARAM;
                } else if (state == STICKPENDING) {
                    buff.append('"');
                    state = STICK;
                } else { // INPARAMPENDING
                    buff.append('"');
                    state = INPARAM;
                }
                continue;
            }

            if (state == INPARAMPENDING) {
                buff.append('\\');
                state = INPARAM;
            } else if (state == STICKPENDING) {
                buff.append('\\');
                state = STICK;
            }
            buff.append(c);
        }
        // collect
        if (state == INPARAM) {
            params.addElement(buff.toString());
        } else if ((state & (INPARAMPENDING | STICKPENDING)) != 0) {
            buff.append('\\');
            params.addElement(buff.toString());
        } else { // NULL or STICK
            if (buff.length() != 0) {
                params.addElement(buff.toString());
            }
        }
        String[] ret = new String[params.size()];
        params.copyInto(ret);
        return ret;
    }
    
    /** Complementary method to parseParameters
     * @see #parseParameters
     */
    public static String escapeParameters(String[] params) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < params.length; i++) {
            escapeString(params[i], sb);
            sb.append(' ');
        }
        final int len = sb.length();
        if (len > 0) {
            sb.setLength(len - 1);
        }
        return sb.toString().trim();
    }
    
    /** Escapes one string
     * @see #escapeParameters
     */
    private static void escapeString(String s, StringBuffer sb) {
        if (s.length() == 0) {
            sb.append("\"\"");
            return;
        }

        boolean hasSpace = false;
        final int sz = sb.length();
        final int slen = s.length();
        char c;
        
        for (int i = 0; i < slen; i++) {
            c = s.charAt(i);         
            
            if (Character.isWhitespace(c)) {
                hasSpace = true;
                sb.append(c);
                continue;
            }
            
            if (c == '\\') {
                sb.append('\\').append('\\');
                continue;
            }
            
            if (c == '"') {
                sb.append('\\').append('"');
                continue;
            }
            
            sb.append(c);
        }
        
        if (hasSpace) {
            sb.insert(sz, '"');
            sb.append('"');
        }
    }


    //
    // Key conversions
    //


    /** Initialization of the names and values
    * @return array of two hashmaps first maps 
    *   allowed key names to their values (String, Integer) 
    *  and second     
    * hashtable for mapping of values to their names (Integer, String)
    */
    private static synchronized HashMap[] initNameAndValues () {
        if (namesAndValues != null) {
            HashMap[] arr = (HashMap[])namesAndValues.get ();
            if (arr != null) {
                return arr;
            }
        }

        Field[] fields = KeyEvent.class.getDeclaredFields ();

        HashMap names = new HashMap (fields.length * 4 / 3 + 1, 0.75f);
        HashMap values = new HashMap (fields.length * 4 / 3 + 1, 0.75f);
        
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic (fields[i].getModifiers ())) {
                String name = fields[i].getName ();
                if (name.startsWith ("VK_")) { // NOI18N
                    // exclude VK
                    name = name.substring (3);
                    try {
                        int numb = fields[i].getInt (null);
                        Integer value = new Integer (numb);
                        names.put (name, value);
                        values.put (value, name);
                    } catch (IllegalArgumentException ex) {
                    } catch (IllegalAccessException ex) {
                    }
                }
            }
        }

        HashMap[] arr = { names, values };

        namesAndValues = new SoftReference (arr);

        return arr;
    }


    /** Converts a Swing key stroke descriptor to a familiar Emacs-like name.
    * @param stroke key description
    * @return name of the key (e.g. <code>CS-F1</code> for control-shift-function key one)
    * @see #stringToKey
    */
    public static String keyToString (KeyStroke stroke) {
        StringBuffer sb = new StringBuffer ();

        // add modifiers that must be pressed
        if (addModifiers (sb, stroke.getModifiers ())) {
            sb.append ('-');
        }

        HashMap[] namesAndValues = initNameAndValues ();

        String c = (String)namesAndValues[1].get (new Integer (stroke.getKeyCode ()));
        if (c == null) {
            sb.append (stroke.getKeyChar ());
        } else {
            sb.append (c);
        }

        return sb.toString ();
    }

    /** Construct a new key description from a given universal string
    * description.
    * Provides mapping between Emacs-like textual key descriptions and the
    * <code>KeyStroke</code> object used in Swing.
    * <P>
    * This format has following form:
    * <P><code>[C][A][S][M]-<em>identifier</em></code>
    * <p>Where:
    * <UL>
    * <LI> <code>C</code> stands for the Control key
    * <LI> <code>A</code> stands for the Alt key
    * <LI> <code>S</code> stands for the Shift key
    * <LI> <code>M</code> stands for the Meta key
    * </UL>
    * Every modifier before the hyphen must be pressed.
    * <em>identifier</EM> can be any text constant from {@link KeyEvent} but
    * without the leading <code>VK_</code> characters. So {@link KeyEvent#VK_ENTER} is described as
    * <code>ENTER</code>.
    *
    * @param s the string with the description of the key
    * @return key description object, or <code>null</code> if the string does not represent any valid key
    */
    public static KeyStroke stringToKey (String s) {
        StringTokenizer st = new StringTokenizer (s.toUpperCase (), "-", true); // NOI18N

        int needed = 0;

        HashMap names = initNameAndValues ()[0];

        int lastModif = -1;
        try {
            for (;;) {
                String el = st.nextToken ();
                // required key
                if (el.equals ("-")) { // NOI18N
                    if (lastModif != -1) {
                        needed |= lastModif;
                        lastModif = -1;
                    }
                    continue;
                }
                // if there is more elements
                if (st.hasMoreElements ()) {
                    // the text should describe modifiers
                    lastModif = readModifiers (el);
                } else {
                    // last text must be the key code
                    Integer i = (Integer)names.get (el);
                    if (i != null) {
                        return KeyStroke.getKeyStroke (i.intValue (), needed);
                    } else {
                        return null;
                    }
                }
            }
        } catch (NoSuchElementException ex) {
            return null;
        }
    }

    /** Convert a space-separated list of user-friendly key binding names to a list of Swing key strokes.
    * @param s the string with keys
    * @return array of key strokes, or <code>null</code> if the string description is not valid
    * @see #stringToKey
    */
    public static KeyStroke[] stringToKeys (String s) {
        StringTokenizer st = new StringTokenizer (s.toUpperCase (), " "); // NOI18N
        ArrayList arr = new ArrayList ();

        while (st.hasMoreElements ()) {
            s = st.nextToken ();
            KeyStroke k = stringToKey (s);
            if (k == null) return null;
            arr.add (k);
        }

        return (KeyStroke[])arr.toArray (new KeyStroke[arr.size ()]);
    }


    /** Adds characters for modifiers to the buffer.
    * @param buf buffer to add to
    * @param modif modifiers to add (KeyEvent.XXX_MASK)
    * @return true if something has been added
    */
    private static boolean addModifiers (StringBuffer buf, int modif) {
        boolean b = false;

        if ((modif & KeyEvent.CTRL_MASK) != 0) {
            buf.append("C"); // NOI18N
            b = true;
        }
        if ((modif & KeyEvent.ALT_MASK) != 0) {
            buf.append("A"); // NOI18N
            b = true;
        }
        if ((modif & KeyEvent.SHIFT_MASK) != 0) {
            buf.append("S"); // NOI18N
            b = true;
        }
        if ((modif & KeyEvent.META_MASK) != 0) {
            buf.append("M"); // NOI18N
            b = true;
        }

        return b;
    }

    /** Reads for modifiers and creates integer with required mask.
    * @param s string with modifiers
    * @return integer with mask
    * @exception NoSuchElementException if some letter is not modifier
    */
    private static int readModifiers (String s) throws NoSuchElementException {
        int m = 0;
        for (int i = 0; i < s.length (); i++) {
            switch (s.charAt (i)) {
            case 'C':
                m |= KeyEvent.CTRL_MASK;
                break;
            case 'A':
                m |= KeyEvent.ALT_MASK;
                break;
            case 'M':
                m |= KeyEvent.META_MASK;
                break;
            case 'S':
                m |= KeyEvent.SHIFT_MASK;
                break;
            default:
                throw new NoSuchElementException ();
            }
        }
        return m;
    }

    /**
     * Finds out the monitor where the user currently has the input focus.
     * This method is usually used to help the client code to figure out on
     * which monitor it should place newly created windows/frames/dialogs.
     * 
     * @return the GraphicsConfiguration of the monitor which currently has the
     * input focus
     */
    private static GraphicsConfiguration getCurrentGraphicsConfiguration() {
        Frame[] frames = Frame.getFrames();

        for (int i = 0; i < frames.length; i++) {
            if (javax.swing.SwingUtilities.findFocusOwner(frames[i]) != null) {
                return frames[i].getGraphicsConfiguration();
            }
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Returns the usable area of the screen where applications can place its
     * windows.  The method subtracts from the screen the area of taskbars,
     * system menus and the like.  The screen this method applies to is the one
     * which is considered current, ussually the one where the current input
     * focus is.
     * 
     * @return the rectangle of the screen where one can place windows
     * 
     * @since 2.5
     */
    public static Rectangle getUsableScreenBounds() {
        return getUsableScreenBounds(getCurrentGraphicsConfiguration());
    }
    
    /**
     * Returns the usable area of the screen where applications can place its
     * windows.  The method subtracts from the screen the area of taskbars,
     * system menus and the like.
     * 
     * @param gconf the GraphicsConfiguration of the monitor
     * @return the rectangle of the screen where one can place windows
     * 
     * @since 2.5
     */
    public static Rectangle getUsableScreenBounds(GraphicsConfiguration gconf) {
        if (gconf == null)
            gconf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        Rectangle bounds = new Rectangle(gconf.getBounds());
        
        String str;

        str = System.getProperty("netbeans.screen.insets"); // NOI18N
        if (str != null) {
            StringTokenizer st = new StringTokenizer(str, ", "); // NOI18N
            if (st.countTokens() == 4) {
                try {
                    bounds.y = Integer.parseInt(st.nextToken());
                    bounds.x = Integer.parseInt(st.nextToken());
                    bounds.height -= bounds.y + Integer.parseInt(st.nextToken());
                    bounds.width -= bounds.x + Integer.parseInt(st.nextToken());
                }
                catch (NumberFormatException ex) {
                    ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
                }
            }
            return bounds;
        }
        
        str = System.getProperty("netbeans.taskbar.height"); // NOI18N
        if (str != null) {
            bounds.height -= Integer.getInteger(str, 0).intValue();
            return bounds;
        }

        // if JDK 1.4 or later

        if (Dependency.JAVA_SPEC.compareTo(new SpecificationVersion("1.4")) >= 0) { // NOI18N
            try {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Method m = Toolkit.class.getMethod("getScreenInsets", // NOI18N
                                                   new Class[] { GraphicsConfiguration.class });
                if (m == null)
                    return bounds;
                
                Insets insets = (Insets) m.invoke(toolkit, new Object[] { gconf });
                bounds.y += insets.top;
                bounds.x += insets.left;
                bounds.height -= insets.top + insets.bottom;
                bounds.width -= insets.left + insets.right;
            }
            catch (Exception ex) {
                ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
            }
            return bounds;
        }
        
        if (Utilities.isWindows ()) {
            bounds.height -= Utilities.TYPICAL_WINDOWS_TASKBAR_HEIGHT;
            return bounds;
        }

        if ((getOperatingSystem() & OS_MAC) != 0) {
            bounds.height -= TYPICAL_MACOSX_MENU_HEIGHT;
            bounds.y += TYPICAL_MACOSX_MENU_HEIGHT;
            return bounds;
        }

        return bounds;
    }

    /**
     * Helps client code place components on the center of the screen.  It
     * handles multiple monitor configuration correctly
     *
     * @param componentSize the size of the component
     * @return bounds of the centered component
     *
     * @since 2.5
     */
    public static Rectangle findCenterBounds(Dimension componentSize) {
        return findCenterBounds(getCurrentGraphicsConfiguration(),
                                componentSize);
    }

    /**
     * Helps client code place components on the center of the screen.  It
     * handles multiple monitor configuration correctly
     *
     * @param gconf the GraphicsConfiguration of the monitor
     * @param componentSize the size of the component
     * @return bounds of the centered component
     */
    private static Rectangle findCenterBounds(GraphicsConfiguration gconf,
                                              Dimension componentSize) {
        if (gconf == null)
            gconf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        Rectangle bounds = gconf.getBounds();
        return new Rectangle(bounds.x + (bounds.width - componentSize.width) / 2,
                             bounds.y + (bounds.height - componentSize.height) / 2,
                             componentSize.width,
                             componentSize.height);
    }
    
    /** @return size of the screen. The size is modified for Windows OS
     * - some pointes are subtracted to reflect a presence of the taskbar
     *
     * @deprecated this method is almost useless in multiple monitor configuration
     * 
     * @see #getUsableScreenBounds()
     * @see #findCenterBounds(Dimension)
     */
    public static final Dimension getScreenSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        if (isWindows() && !Boolean.getBoolean ("netbeans.no.taskbar")) {
            screenSize.height -= TYPICAL_WINDOWS_TASKBAR_HEIGHT;
        } else if ((getOperatingSystem() & OS_MAC) != 0)
            screenSize.height -= TYPICAL_MACOSX_MENU_HEIGHT;
        return screenSize;
    }
    
    /** Utility method for avoiding of memory leak in JDK 1.3 / JFileChooser.showDialog(...) 
     * @param parent
     * @param approveButtonText
     */
    public static final int showJFileChooser(javax.swing.JFileChooser chooser, java.awt.Component parent, java.lang.String approveButtonText) {
	if(approveButtonText != null) {
	    chooser.setApproveButtonText(approveButtonText);
	    chooser.setDialogType(javax.swing.JFileChooser.CUSTOM_DIALOG);
	}
        
        Frame frame = null;              
        Dialog parentDlg = null;
        
        if (parent instanceof Dialog)
            parentDlg = (Dialog) parent;
        else 
            frame = parent instanceof java.awt.Frame ? (Frame) parent
              : (Frame)javax.swing.SwingUtilities.getAncestorOfClass(Frame.class, parent);
            
              
	String title = chooser.getDialogTitle(); 
        
        if (title == null) {
	    title = chooser.getUI().getDialogTitle(chooser);
	}

        final javax.swing.JDialog dialog;
        if (parentDlg != null)
            dialog = new javax.swing.JDialog(parentDlg, title, true);
        else
            dialog = new javax.swing.JDialog(frame, title, true);
        
        dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(chooser, BorderLayout.CENTER);
 
        dialog.pack();
        dialog.setBounds(findCenterBounds(parent.getGraphicsConfiguration(),
                                          dialog.getSize()));

	chooser.rescanCurrentDirectory();
        final int[] retValue = new int[] { javax.swing.JFileChooser.CANCEL_OPTION };
 
        java.awt.event.ActionListener l = new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                if (ev.getActionCommand() == javax.swing.JFileChooser.APPROVE_SELECTION) {
                    retValue[0] = javax.swing.JFileChooser.APPROVE_OPTION;
                }
                dialog.setVisible(false);
                dialog.dispose();
            }
        };
        chooser.addActionListener(l);
        
        dialog.show();
	return retValue[0];
    }
    
    /** Exception indicating that a given list could not be partially-ordered.
    * @see #partialSort
    * @deprecated Used only by the deprecated {@link #partialSort}
    */
    public static class UnorderableException extends RuntimeException {
        private Collection unorderable;
        private Map deps;

        static final long serialVersionUID =6749951134051806661L;
        /** Create a new unorderable-list exception with no detail message.
        * @param unorderable a collection of list elements which could not be ordered
        *                    (because there was some sort of cycle)
        * @param deps dependencies associated with the list; a map from list elements
        *             to sets of list elements which that element must appear after
        */
        public UnorderableException (Collection unorderable, Map deps) {
            super (/* "Cannot be ordered: " + unorderable */); // NOI18N
            this.unorderable = unorderable;
            this.deps = deps;
        }

        /** Create a new unorderable-list exception with a specified detail message.
        * @param message the detail message
        * @param unorderable a collection of list elements which could not be ordered
        *                    (because there was some sort of cycle)
        * @param deps dependencies associated with the list; a map from list elements
        *             to sets of list elements which that element must appear after
        */
        public UnorderableException (String message, Collection unorderable, Map deps) {
            super (message);
            this.unorderable = unorderable;
            this.deps = deps;
        }

        /** Get the unorderable elements.
        * @return the elements
        * @see Utilities.UnorderableException#Utilities.UnorderableException(Collection,Map)
        */
        public Collection getUnorderable () {
            return unorderable;
        }

        /** Get the dependencies.
        * @return the dependencies
        * @see Utilities.UnorderableException#Utilities.UnorderableException(Collection,Map)
        */
        public Map getDeps () {
            return deps;
        }

    }

    /** Sort a list according to a specified partial order.
    * Note that in the current implementation, the comparator will be called
    * exactly once for each distinct pair of list elements, ignoring order,
    * so caching its results is a waste of time.
    * @param l the list to sort (will not be modified)
    * @param c a comparator to impose the partial order; "equal" means that the elements
    *          are not ordered with respect to one another, i.e. may be only a partial order
    * @param stable whether to attempt a stable sort, meaning that the position of elements
    *               will be disturbed as little as possible; might be slightly slower
    * @return the partially-sorted list
    * @throws UnorderableException if the specified partial order is inconsistent on this list
    * @deprecated Deprecated in favor of the potentially much faster (and possibly more correct) {@link #topologicalSort}.
    */
    public static List partialSort (List l, Comparator c, boolean stable) throws UnorderableException {
        // map from objects in the list to null or sets of objects they are greater than
        // (i.e. must appear after):
        Map deps = new HashMap (); // Map<Object,Set<Object>>
        int size = l.size ();
        // Create a table of dependencies.
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                int cmp = c.compare (l.get (i), l.get (j));
                if (cmp != 0) {
                    Object earlier = l.get (cmp < 0 ? i : j);
                    Object later = l.get (cmp > 0 ? i : j);
                    Set s = (Set) deps.get (later);
                    if (s == null)
                        deps.put (later, s = new HashSet ());
                    s.add (earlier);
                }
            }
        }
        // Lists of items to process, and items sorted.
        List left = new LinkedList (l);
        List sorted = new ArrayList (size);
        while (left.size () > 0) {
            boolean stillGoing = false;
            Iterator it = left.iterator ();
            while (it.hasNext ()) {
                Object elt = it.next ();
                Set eltDeps = (Set) deps.get (elt);
                if (eltDeps == null || eltDeps.size () == 0) {
                    // This one is OK to add to the result now.
                    it.remove ();
                    stillGoing = true;
                    sorted.add (elt);
                    // Mark other elements that should be later
                    // than this as having their dep satisfied.
                    Iterator it2 = left.iterator ();
                    while (it2.hasNext ()) {
                        Object elt2 = it2.next ();
                        Set eltDeps2 = (Set) deps.get (elt2);
                        if (eltDeps2 != null) eltDeps2.remove (elt);
                    }
                    if (stable) break;
                }
            }
            if (! stillGoing) {
                // Clean up deps to only include "interesting" problems.
                it = deps.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry me = (Map.Entry)it.next();
                    if (!left.contains(me.getKey())) {
                        it.remove();
                    } else {
                        Set s = (Set)me.getValue();
                        Iterator it2 = s.iterator();
                        while (it2.hasNext()) {
                            if (!left.contains(it2.next())) {
                                it2.remove();
                            }
                        }
                        if (s.isEmpty()) {
                            it.remove();
                        }
                    }
                }
                throw new UnorderableException (left, deps);
            }
        }
        return sorted;
    }
    
    /**
     * Topologically sort some objects.
     * <p>There may not be any nulls among the objects, nor duplicates
     * (as per hash/equals), nor duplicates among the edge lists.
     * The edge map need not contain an entry for every object, only if it
     * has some outgoing edges (empty but not null map values are permitted).
     * The edge map may contain neither keys nor value entries for objects not
     * in the collection to be sorted.
     * <p>The incoming parameters will not be modified; they must not be changed
     * during the call and possible calls to TopologicalSortException methods. 
     * The returned list will support modifications.
     * <p>There is a <em>weak</em> stability guarantee: if there are no edges
     * which contradict the incoming order, the resulting list will be in the same
     * order as the incoming elements. However if some elements need to be rearranged,
     * it is <em>not</em> guaranteed that others will not also be rearranged, even
     * if they did not strictly speaking need to be.
     * @param c a collection of objects to be topologically sorted
     * @param edges constraints among those objects, of type <code>Map&lt;Object,Collection&gt;</code>;
     *              if an object is a key in this map, the resulting order will
     *              have that object before any objects listed in the value
     * @return a partial ordering of the objects in the collection, 
     * @exception TopologicalSortException if the sort cannot succeed due to cycles in the graph, the
     *   exception contains additional information to describe and possibly recover from the error
     * @since 3.30
     * @see "#27286"
     */
    public static List topologicalSort(Collection c, Map edges) throws TopologicalSortException {
        Map finished = new HashMap();
        List r = new ArrayList(Math.max(c.size(), 1));
        List cRev = new ArrayList(c);
        Collections.reverse(cRev);
        Iterator it = cRev.iterator();
        while (it.hasNext()) {
            List cycle = visit(it.next(), edges, finished, r);
            if (cycle != null) {
                throw new TopologicalSortException (cRev, edges);
            }
        }
        Collections.reverse(r);
        return r;
    }
    
    /**
     * Visit one node in the DAG.
     * @param node node to visit
     * @param edges edges in the DAG
     * @param finished which nodes are finished; a node has no entry if it has not yet
     *                 been visited, else it is set to false while recurring and true
     *                 when it has finished
     * @param r the order in progress
     * @return list with detected cycle
     */
    static List visit(Object node, Map edges, Map finished, List r) {
        Boolean b = (Boolean)finished.get(node);
        //System.err.println("node=" + node + " color=" + b);
        if (b != null) {
            if (b.booleanValue ()) {
                return null;
            }
            
            ArrayList cycle = new ArrayList ();
            cycle.add (node);
            finished.put (node, null);
            return cycle;
        }
        Collection e = (Collection)edges.get(node);
        if (e != null) {
            finished.put(node, Boolean.FALSE);
            Iterator it = e.iterator();
            while (it.hasNext()) {
                List cycle = visit(it.next(), edges, finished, r);
                if (cycle != null) {
                    if (cycle instanceof ArrayList) {
                        // if cycle instanceof ArrayList we are still in the
                        // cycle and we want to collect new members
                        
                        if (Boolean.FALSE == finished.get (node)) {
                            // another member in the cycle
                            cycle.add (node);
                        } else {
                            // we have reached the head of the cycle
                            // do not add additional cycles anymore
                            Collections.reverse(cycle);
                            // changing cycle to not be ArrayList
                            cycle = Collections.unmodifiableList(cycle);
                        }
                    }
                    
                    // mark this node as tested
                    finished.put (node, Boolean.TRUE);

                    // and report an error
                    return cycle;
                }
            }
        }
        finished.put(node, Boolean.TRUE);
        r.add(node);
        return null;
    }
    
    
    // Package retranslation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    private static final String TRANS_LOCK = "TRANS_LOCK";
    
    /** last used classloader or if run in test mode the TRANS_LOCK */
    private static Object transLoader;
    /** regular expression to with all changes */
    private static RE transExp;
    
    /** Provides support for parts of the system that deal with classnames
     * (use <code>Class.forName</code>, <code>NbObjectInputStream</code>, etc.).
     * <P>
     * Often class names (especially package names) changes during lifecycle 
     * of a module. When some piece of the system stores the name of a class
     * in certain point of a time and wants to find the correct <code>Class</code>
     * later it needs to count with the possibility of rename.
     * <P>
     * For such purposes this method has been created. It allows modules to 
     * register their classes that changed names and other parts of system that
     * deal with class names to find the correct names.
     * <P>
     * To register a mapping from old class names to new ones create a file
     * <code>META-INF/netbeans/translate.names</code> in your module and fill it
     * with your mapping:
     * <PRE>
     * # 
     * # Mapping of legacy classes to new ones
     * # 
     *
     * org.oldpackage.MyClass=org.newpackage.MyClass # rename of package for one class
     * org.mypackage.OldClass=org.mypackage.NewClass # rename of class in a package
     *
     * # rename of class and package
     * org.oldpackage.OldClass=org.newpackage.NewClass
     * 
     * # rename of whole package
     * org.someoldpackage=org.my.new.package.structure
     *
     * </PRE>
     * Btw. one can use spaces instead of <code>=</code> sign.
     * For a real world example
     * check the 
     * <a href="http://www.netbeans.org/source/browse/xml/text-edit/compat/src/META-INF/netbeans/">
     * xml module</a>.
     * 
     * <P>
     * For purposes of <link>org.openide.util.io.NbObjectInputStream</link> there is 
     * a following special convention: 
     * If the 
     * className is not listed as one that is to be renamed, the returned
     * string == className, if the className is registered to be renamed
     * than the className != returned value, even in a case when className.equals (retValue)
     *
     * @param className fully qualified name of a class to translate
     * @return new name of the class according to renaming rules. 
     */
    public static String translate(final String className) {
        checkMapping ();
        
        RE exp;
        synchronized (TRANS_LOCK) {
            exp = transExp;
        }
        
        if (exp == null) {
            // no transition table found
            return className;
        }

        synchronized (exp) {
            // refusing convertions as fast as possible
            return exp.convert (className);
        }
    }
    

    /** Loads all resources that contain renaming information.
     * @param l classloader to load packages from
     */
    private static void checkMapping () {
        // test if we run in test mode
        if (transLoader == TRANS_LOCK) {
            // no check
            return;
        }
        
        ClassLoader current = (ClassLoader)Lookup.getDefault ().lookup (ClassLoader.class);
        if (current == null) {
            current = ClassLoader.getSystemClassLoader();
        }
        if (transLoader == current) {
            // no change, no rescan
            return;
        }
        
        initForLoader (current, current);
    }
    
    /* Initializes the content of transition table from a classloader.
     * @param loader loader to read data from
     * @param set loader to set as the transLoader or null if we run in test mode
     */
    static void initForLoader (ClassLoader current, Object set) {
        if (set == null) {
            set = TRANS_LOCK;
        }
        
        Enumeration en;

        try {
            en = current.getResources("META-INF/netbeans/translate.names");
        } catch (IOException ex) {
            ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
            en = null;
        }
            
        if (en == null || !en.hasMoreElements()) {
            synchronized (TRANS_LOCK) {
                transLoader = set;
                transExp = null;
            }
            
            return;
        }

        // format of line in the meta files
        //
        // # comments are allowed
        // a.name.in.a.Package=another.Name # with comment is allowed
        // for.compatibility.one.can.use.Space instead.of.Equal
        //
        
        RE re = null;
// [pnejedly:perf] commented out. The RegExp based translation was way slower
// than the hand-written RE13
//        if (Dependency.JAVA_SPEC.compareTo(new SpecificationVersion("1.4")) >= 0) { // NOI18N
//            try {
//                re = (RE)Class.forName ("org.openide.util.RE14").newInstance ();
//            } catch (ThreadDeath t) {
//                throw t;
//            } catch (Throwable t) {
//            }
//        }
        
        if (re == null) {
            re = new RE13 ();
        }
        
        
        
        TreeSet list = new TreeSet (new Comparator () {
            public int compare (Object o1, Object o2) {
                String s1 = ((String[])o1)[0];
                String s2 = ((String[])o2)[0];
                
                int i1 = s1.length ();
                int i2 = s2.length ();
                
                if (i1 != i2) return i2 - i1;
                
                return s2.compareTo (s1);
            }
        });
        
        while (en.hasMoreElements ()) {
            URL u = (URL)en.nextElement();
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader (u.openStream(), "UTF8") // use explicit encoding  //NOI18N
                );  
                loadTranslationFile (re, reader, list);
                reader.close ();
            } catch (IOException ex) {
		ErrorManager.getDefault ().annotate(ex, ErrorManager.UNKNOWN,
			"Problematic file: " + u, null, null, null);
                ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
            }
        }
        
        // construct a regular expression of following form. Let "1", "2", "3", "4"
        // be the keys:
        // "^
        // thus if 4 is matched five groups will be created

        String[] arr = new String[list.size ()];
        String[] pattern = new String[arr.length];
        
        int i = 0;
        Iterator it = list.iterator ();
        while (it.hasNext ()) {
            String[] pair = (String[])it.next ();
            arr[i] = pair[1].intern (); // name of the track
            pattern[i] = pair[0]; // original object
            i++;
        }

        synchronized (TRANS_LOCK) {
            // last check
            if (arr.length == 0) {
                transExp = null;
            } else {
                transExp = re;
                transExp.init (pattern, arr);
            }
            transLoader = set;
        }
    }

    /** 
     * Load single translation file. 
     * @param resource URL identifiing transaction table
     * @param results will be filled with String[2]
     */
    private static void loadTranslationFile(
        RE re,
        BufferedReader reader, Set results
    ) throws IOException {
        for (;;) {
            String line = reader.readLine ();
            if (line == null) {
                break;
            }
            
            if (line.length () == 0 || line.startsWith ("#")) { // NOI18N
                continue;
            }
            
            String[] pair = re.readPair (line);
            if (pair == null) {
                throw new java.io.InvalidObjectException ("Line is invalid: " + line);
            }
            
            results.add (pair);
        }
        
    }
    
    /** This method merges two images into the new one. The second image is drawn
     * over the first one with its top-left corner at x, y. Images mustn't be of the same size.
     * New image will have a size of max(second image size + top-left corner, first image size).
     * Method is used mostly when second image contains transparent pixels (e.g. for badging).
     * If both images are <code>null</code>, it makes default transparent 16x16 image.
     * @param image1 underlying image
     * @param image2 second image
     * @param x x position of top-left corner
     * @param y y position of top-left corner
     * @return new merged image
     */
    public static final Image mergeImages (Image image1, Image image2, int x, int y) {
        return IconManager.mergeImages(image1, image2, x, y);
    }
    
    /**
     * Loads an image from the specified resource ID. The image is loaded using the "system" classloader registered in
     * Lookup.
     * @param resourceID resource path of the icon (no initial slash)
     * @return icon's Image, or null, if the icon cannot be loaded.
     */
    public static final Image loadImage (String resourceID) {
        return IconManager.getIcon(resourceID);
    }
    
    //
    // Support for work with actions
    //
    
    /** type of Class or of an Exception thrown */
    private static Object actionClassForPopupMenu;
    
    /** Builds a popup menu from actions for provided context specified by 
     * <code>Lookup</code>. 
     * Takes list of actions and for actions whic are instances of 
     * <code>ContextAwareAction</code> creates and uses the context aware instance.
     * Then gets the action presenter or simple menu item for the action to the 
     * popup menu for each action (or separator for each 'lonely' null array member).
     *
     * @param actions array of actions to build menu for. Can contain null
     *   elements, they will be replaced by separators
     * @param context the context for which the popup is build
     * @return the constructed popup menu
     * @see ContextAwareAction
     * @since 3.29
     */
    public static javax.swing.JPopupMenu actionsToPopup (
        Action[] actions, Lookup context
    ) {
        javax.swing.JPopupMenu menu = null;

        try {
            if (actionClassForPopupMenu == null) {
                actionClassForPopupMenu = Class.forName ("org.openide.awt.JPopupMenuPlus");
            }
            
            if (actionClassForPopupMenu instanceof Class) {
                menu = (javax.swing.JPopupMenu)((Class)actionClassForPopupMenu).newInstance ();
            }
        } catch (IllegalAccessException ex) {
            actionClassForPopupMenu = new Object ();
        } catch (InstantiationException ex) {
            actionClassForPopupMenu = new Object ();
        } catch (ClassNotFoundException ex) {
            actionClassForPopupMenu = new Object ();
        }
        
        if (menu == null) {
            menu = new javax.swing.JPopupMenu ();
        }

        // keeps actions for which was menu item created already
        HashSet counted = new HashSet ();
        boolean canSep = false;
        for (int i = 0; i < actions.length; i++) {
            boolean addSep = true;

            Action action = actions[i];

            if (action != null) {
                // if this action has menu item already, skip to next iteration
                if (counted.contains (action))
                    continue;

                counted.add (action);

                // switch to replacement action if there is some
                if(action instanceof ContextAwareAction) {
                    action = ((ContextAwareAction)action).createContextAwareInstance(context);
                }

                addSep = false;
                canSep = true;
                javax.swing.JMenuItem item;
                if (action instanceof org.openide.util.actions.Presenter.Popup) {
                    item = ((org.openide.util.actions.Presenter.Popup)action).getPopupPresenter ();
                    if (item == null) {
                        NullPointerException npe = new NullPointerException(
                            "findContextMenuImpl, getPopupPresenter returning null for " + action); // NOI18N
                        ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, npe);
                    }
                    menu.add (item);
                } else {
                    menu.add (action);
                }
            }

            if (addSep && canSep) {
                menu.addSeparator ();
                canSep = false;
            }
        }
        
        return menu;
    }
    
    
    /** Builds a popup menu for provided component. It retrieves context
     * (lookkup) from provided component instance or one of its parent
     * (it searches up to the hierarchy for <code>Lookup.Provider</code> instance).
     * If none of the components is <code>Lookup.Provider</code> instance, then
     * it is created context which is fed with composite ActionMap which delegates
     * to all components up to hierarchy started from the specified one.
     * Then it is called method {@link #actionsToPopup(Action[], Lookup)} whith
     * the found <code>Lookup</code> instance, which actualy creates a popup menu.
     *
     * @param actions array of actions to build menu for. Can contain null
     *   elements, they will be replaced by separators
     * @param component a component in which to search for a context
     * @return the constructed popup menu
     * @see Lookup.Provider
     * @see #actionsToPopup(Action[], Lookup)
     * @since 3.29
     */
    public static javax.swing.JPopupMenu actionsToPopup (
        Action[] actions, java.awt.Component component
    ) { 
        Lookup lookup = null;
        for (Component c = component; c != null; c = c.getParent()) {
            if (c instanceof Lookup.Provider) {
                lookup = ((Lookup.Provider)c).getLookup ();
                if (lookup != null) {
                    break;
                }
            }
        }
        
        if(lookup == null) {
            // Fallback to composite action map, even it is questionable,
            // whether we should support component which is not (nor
            // none of its parents) lookup provider.
            UtilitiesCompositeActionMap map = new UtilitiesCompositeActionMap (component);
            lookup = org.openide.util.lookup.Lookups.singleton(map);
        }
        
        return actionsToPopup (actions, lookup);
    }
    
    
    //
    // end of actions stuff
    //
    
    /**
     * Loads an image based on resource path.
     * Exactly like {@link #loadImage(String)} but may do a localized search.
     * For example, requesting <samp>org/netbeans/modules/foo/resources/foo.gif</samp>
     * might actually find <samp>org/netbeans/modules/foo/resources/foo_ja.gif</samp>
     * or <samp>org/netbeans/modules/foo/resources/foo_mybranding.gif</samp>.
     * @since 3.24
     */
    public static final Image loadImage(String resource, boolean localized) {
        if (localized) {
            String base, ext;
            int idx = resource.lastIndexOf('.');
            if (idx != -1 && idx > resource.lastIndexOf('/')) {
                base = resource.substring(0, idx);
                ext = resource.substring(idx);
            } else {
                base = resource;
                ext = ""; // NOI18N
            }
            // #31008. [PENDING] remove in case package cache is precomputed
            Image baseVariant = loadImage(base + ext);
            Iterator it = NbBundle.getLocalizingSuffixes();
            while (it.hasNext()) {
                String suffix = (String)it.next();
                Image i;
                if (suffix.length() == 0) {
                    i = baseVariant;
                } else {
                    i = loadImage(base + suffix + ext);
                }
                if (i != null) {
                    return i;
                }
            }
            return null;
        } else {
            return loadImage(resource);
        }
    }
    
    /**
     *  Returns a cursor with an arrow and an hourglass (or stop watch) badge,
     *  to be used when a component is busy but the UI is still responding to the user. 
     *
     *  Similar to the predefined {@link Cursor#WAIT_CURSOR}, but has an arrow to indicate
     *  a still-responsive UI.
     *
     *  <p>Typically you will set the cursor only temporarily:
     *
     *  <pre>
     *  <font class="comment">// code is running in other then event dispatch thread</font>
     *  currentComponent.setCursor(Utilities.createProgressCursor(currentComponent));
     *  <font class="keyword">try</font> {
     *      <font class="comment">// perform some work in other than event dispatch thread
     *      // (do not block UI)</font>
     *  } <font class="keyword">finally</font> {
     *      currentComponent.setCursor(<font class="constant">null</font>);
     *  } 
     *  </pre>
     *
     *  <p>This implementation provides one cursor for all Mac systems, one for all
     *  Unix systems (regardless of window manager), and one for all other systems
     *  including Windows.
     *
     *  @param   component the non-null component that will use the progress cursor
     *  @return  a progress cursor (Unix, Windows or Mac)
     *
     * @since 3.23
     */
    public static final Cursor createProgressCursor(Component component) {
        // refuse null component
        if (component == null) {
            throw new NullPointerException("Given component is null"); //NOI18N
        }
        
        Image image = null;
        
        // First check for Mac because its part of the Unix_Mask
        if (getOperatingSystem() == OS_MAC) {
            image = loadImage("org/openide/resources/progress-cursor-mac.gif");    //NOI18N
        }
        else if (isUnix()) {
            image = loadImage("org/openide/resources/progress-cursor-motif.gif");    //NOI18N
        }
        // All other OS, including Windows, use Windows cursor
        else  {
            image = loadImage("org/openide/resources/progress-cursor-win.gif");    //NOI18N
        }
        
        return createCustomCursor (component, image, "PROGRESS_CURSOR"); //NOI18N
    }
    
    // added to fix issue #30665 (bad size on linux)
    private static Cursor createCustomCursor(Component component, Image icon, String name) {
        Toolkit t = component.getToolkit();
        Dimension d = t.getBestCursorSize(16, 16);
        Image i = icon;
        if (d.width != icon.getWidth(null)) {
            if ((d.width) == 0 && (d.height == 0)) {
                // system doesn't support custom cursors, falling back
                return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
            }
            // need to resize the icon
            Image empty = IconManager.createBufferedImage(d.width, d.height);
            i = Utilities.mergeImages(icon, empty, 0, 0);
        }
        return t.createCustomCursor(i, new Point(1,1), name);
    }
    
    /** Attaches asynchronous init job to given component. 
     * {@link AsyncGUIJob#construct()} will be called after first
     * paint, when paint event arrives. Later, {@link AsyncGUIJob#finished()}
     * will be called according to the rules of the <code>AsyncGUIJob</code> interface.
     *
     * Useful for components that have slower initialization phase, component
     * can benefit from more responsive behaviour during init.
     *
     * @param comp4Init Regular component in its pre-inited state, state in which
     *        component will be shown between first paint and init completion.
     * @param initJob Initialization job to be called asynchronously. Job can 
     *            optionally implement {@link Cancellable}
     *            interface for proper cancel logic. Cancel method will be called
     *            when component stops to be showing during job's progress.
     *            See {@link java.awt.Component#isShowing}
     *
     * @since 3.36
     */
    public static final void attachInitJob (Component comp4Init, AsyncGUIJob initJob) {
        new AsyncInitSupport(comp4Init, initJob);
    }
    
    /** Interfaces for communication between Utilities.translate and regular 
     * expression impl.
     *
     * Order of methods is:
     * readPair few times
     * init once
     * convert many times
     */
    static interface RE {
        public void init (String[] original, String[] newversion);
        public String convert (String pattern);
        
        /** Parses line of text to two parts: the key and the rest
         */
        public String[] readPair (String line);
    }
    
    /** Implementation of the active queue.
     */
    private static final class ActiveQueue extends ReferenceQueue 
    implements Runnable {
        private boolean running;
        private boolean deprecated;
        
        public ActiveQueue (boolean deprecated) {
            this.deprecated = deprecated;
            
            Thread t = new Thread (this, "Active Reference Queue Daemon"); // NOI18N
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true); // to not prevent exit of VM
            t.start ();
        }
            
        
        public Reference poll() {
            throw new java.lang.UnsupportedOperationException ();
        }
        
        public Reference remove(long timeout) throws IllegalArgumentException, InterruptedException {
            throw new java.lang.InterruptedException ();
        }
        
        public Reference remove() throws InterruptedException {
            throw new java.lang.InterruptedException ();
        }
        
        /** Called either from Thread.run or RequestProcessor.post. In first case
         * calls scanTheQueue (only once) in the second and nexts calls cleanTheQueue
         */
        public void run () {
            synchronized (this) {
                if (running) {
                    return;
                }
                running = true;
            }
            
            for (;;) {
                try {
                    Reference ref = super.remove (0);
                    if (! (ref instanceof Runnable)) {
                        ErrorManager.getDefault().log (
                            ErrorManager.ERROR, 
                            "A reference not implementing runnable has been added to the Utilities.activeReferenceQueue (): " + ref.getClass () // NOI18N
                        ); 
                        continue;
                    }
                    
                    if (deprecated) {
                        ErrorManager.getDefault().log (
                            ErrorManager.WARNING, 
                            "Utilities.ACTIVE_REFERENCE_QUEUE has been deprecated for " + ref.getClass () + " use Utilities.activeReferenceQueue" // NOI18N
                        ); 
                    }
                        
                    
                    // do the cleanup
                    try {
                        ((Runnable)ref).run ();
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t) {
			// Should not happen.
			// If it happens, it is a bug in client code, notify!
                        ErrorManager.getDefault().notify(t);
                    }
                } catch (InterruptedException ex) {
                    ErrorManager.getDefault ().notify (ErrorManager.INFORMATIONAL, ex);
                }
            }
        }
    }
    
    /**
     * A convertor between files and URLs.
     */
    interface FileURLConvertor {
        /** @see Utilities#toURL */
        URL toURL(File f) throws MalformedURLException;
        /** @see Utilities#toFile */
        File toFile(URL u);
    }
    
    /**
     * A convertor between files and URLs that (sort of) works under JDK 1.3.
     */
    private static class FileURLConvertor13 implements FileURLConvertor {
        FileURLConvertor13() {}
        public URL toURL(File f) throws MalformedURLException {
            URL u = f.toURL();
            String u2 = u.toExternalForm();
            if (u2.indexOf('#') != -1) {
                // #27330: installation in a dir containing hash marks
                int i;
                while ((i = u2.indexOf('#')) != -1) {
                    u2 = u2.substring(0, i) + "%23" + u2.substring(i + 1); // NOI18N
                }
                u = new URL(u2);
            }
            return u;
        }
        public File toFile(URL u) {
            if (!"file".equals(u.getProtocol())) { // NOI18N
                return null;
            }
            String path = u.getPath();
            int i;
            while ((i = path.indexOf("%23")) != -1) { // NOI18N
                path = path.substring(0, i) + '#' + path.substring(i + 3);
            }
            return new File(path.replace('/', File.separatorChar));
        }
    }

    /** currently available File &#8596; URL convertor */
    private static FileURLConvertor convertor = null;
    
    /**
     * Convert a file to a matching <code>file:</code> URL.
     * Under JDK 1.4, the result should be properly escaped as a URL;
     * under JDK 1.3, it may not be, and so filenames containing
     * strange characters may not work well, though hash marks are
     * supported as a special case.
     * <p>The resulting URL should be openable as a means of accessing the
     * file contents, except for some unusual characters under JDK 1.3.
     * @param f a file (absolute only)
     * @return a URL using the <code>file</code> protocol
     * @throws MalformedURLException for no good reason
     * @see File#toURI
     * @see java.net.URI#toURL
     * @see File#toURL
     * @see URL#openConnection
     * @see #toFile
     * @see "#29711"
     * @since 3.26
     */
    public static URL toURL(File f) throws MalformedURLException {
        if (f == null) throw new NullPointerException();
        if (!f.isAbsolute()) throw new IllegalArgumentException("Relative path: " + f); // NOI18N
        if (convertor == null) {
            // No access to Dependency.JAVA_SPEC from here, so just always try to load it.
            try {
                Class clazz = Class.forName("org.openide.util.FileURLConvertor14"); // NOI18N
                FileURLConvertor c = (FileURLConvertor)clazz.newInstance();
                try {
                    URL u = c.toURL(f);
                    convertor = c;
                    return u;
                } catch (MalformedURLException mfue) {
                    convertor = c;
                    throw mfue;
                }
            } catch (Exception e) {
                // ClassNotFoundException, etc.
            } catch (LinkageError e) {
                // NoSuchMethodError, etc.
            }
            // Fallback - JDK 1.3, or problem under 1.4.
            convertor = new FileURLConvertor13();
        }
        return convertor.toURL(f);
    }
    
    /**
     * Convert a <code>file:</code> URL to a matching file.
     * Under JDK 1.4, this should be the exact inverse of
     * converting the file to a URL. Under JDK 1.3, the
     * conversion is sloppier and may fail for files named
     * strangely (including unusual characters), though as a
     * special case the hash mark (<samp>#</samp>) is supported.
     * <p>You may not use a URL generated from a file on a different
     * platform, as file name conventions may make the result meaningless
     * or even unparsable.
     * @param u a URL with the <code>file</code> protocol
     * @return an absolute file it points to, or <code>null</code> if the URL
     *         does not seem to point to a file at all
     * @see java.net.URI#URI(String)
     * @see File#File(java.net.URI)
     * @see #toURL
     * @see "#29711"
     * @since 3.26
     */
    public static File toFile(URL u) {
        if (u == null) throw new NullPointerException();
        if (convertor == null) {
            try {
                Class clazz = Class.forName("org.openide.util.FileURLConvertor14"); // NOI18N
                FileURLConvertor c = (FileURLConvertor)clazz.newInstance();
                File f = c.toFile(u);
                // Only set it after it has successfully gone through the whole code
                // path. Some VMs (e.g. -Xverify:none) will not resolve all methods
                // until they are actually run. Making the instance is not enough
                // to guarantee that all of its references have been resolved.
                convertor = c;
                return f;
            } catch (Exception e) {
                // ClassNotFoundException, etc.
            } catch (LinkageError e) {
                // NoSuchMethodError, etc.
            }
            // Fallback - JDK 1.3, or problem under 1.4.
            convertor = new FileURLConvertor13();
        }
        return convertor.toFile(u);
    }
    
}
