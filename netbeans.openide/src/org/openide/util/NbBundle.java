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

import java.io.*;
import java.lang.ref.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;

import org.openide.ErrorManager;
import org.openide.util.enums.*;

/** Convenience class permitting easy loading of localized resources of various sorts.
* Extends the functionality of the default Java resource support, and interacts
* better with class loaders in a multiple-loader system.
* <p>Example usage:
* <p><code><pre>
* package com.mycom;
* public class Foo {
*   // Search for tag Foo_theMessage in /com/mycom/Bundle.properties:
*   private static String theMessage = {@link NbBundle#getMessage(Class, String) NbBundle.getMessage} (Foo.class, "Foo_theMessage");
*   // Might also look in /com/mycom/Bundle_de.properties, etc.
* }
* </pre></code>
*
* @author   Petr Hamernik, Jaroslav Tulach, Jesse Glick
*/
public class NbBundle extends Object {

    /**
     * Do not call.
     * @deprecated There is no reason to instantiate or subclass this class.
     *             All methods in it are static.
     */
    public NbBundle () {}

    private static final boolean USE_DEBUG_LOADER = Boolean.getBoolean ("org.openide.util.NbBundle.DEBUG"); // NOI18N
    
    private static String brandingToken = null;
    /** Get the current branding token.
     * @return the branding, or <code>null</code> for none
     */
    public static String getBranding () {
        return brandingToken;
    }
    /** Set the current branding token.
     * The permitted format, as a regular expression:
     * <pre>/^[a-z][a-z0-9]*(_[a-z][a-z0-9]*)*$/</pre>
     * @param bt the new branding, or <code>null</code> to clear
     * @throws IllegalArgumentException if in an incorrect format
     */
    public static void setBranding (String bt) throws IllegalArgumentException {
        // [PENDING] check its format here acc. to above regex
        brandingToken = bt;
    }
    
    /**
     * Cache of URLs for localized files.
     * Keeps only weak references to the class loaders.
     * @see "#9275"
     */
    private static final Map localizedFileCache = new WeakHashMap(); // Map<ClassLoader,Map<String,URL>>

    /** Get a localized file in the default locale with the default class loader.
    * <p>Note that use of this call is similar to using the URL protocol <code>nbresloc</code>
    * (which is in fact implemented using the fuller form of the method).
    * <p>The extension may be null, in which case no final dot will be appended.
    * If it is the empty string, the resource will end in a dot.
    * @param baseName base name of file, as dot-separated path (e.g. <code>some.dir.File</code>)
    * @param ext      extension of file (or <code>null</code>)
    * @return URL of matching localized file
    * @throws MissingResourceException if not found
    */
    public static synchronized URL getLocalizedFile(String baseName, String ext)
    throws MissingResourceException {
        return getLocalizedFile(baseName, ext, Locale.getDefault(), getLoader());
    }

    /** Get a localized file with the default class loader.
    * @param baseName base name of file, as dot-separated path (e.g. <code>some.dir.File</code>)
    * @param ext      extension of file (or <code>null</code>)
    * @param locale   locale of file
    * @return URL of matching localized file
    * @throws MissingResourceException if not found
    */
    public static synchronized URL getLocalizedFile(String baseName, String ext,
            Locale locale) throws MissingResourceException {
        return getLocalizedFile(baseName, ext, locale, getLoader());
    }
    
    /** Get a localized file.
    * @param baseName base name of file, as dot-separated path (e.g. <code>some.dir.File</code>)
    * @param ext      extension of file (or <code>null</code>)
    * @param locale   locale of file
    * @param loader  class loader to use
    * @return URL of matching localized file
    * @throws MissingResourceException if not found
    */
    public static synchronized URL getLocalizedFile(String baseName, String ext,
            Locale locale, ClassLoader loader) throws MissingResourceException {

        // [PENDING] in the future, could maybe do something neat if
        // USE_DEBUG_LOADER and ext is "html" or "txt" etc...

        URL lookup = null;
        Iterator it = new LocaleIterator (locale);
        String cachePrefix = "["+Integer.toString(loader.hashCode())+"]"; // NOI18N
        List cacheCandidates = new ArrayList(10); // List<String>
        String baseNameSlashes = baseName.replace('.', '/');
        Map perLoaderCache = (Map)localizedFileCache.get(loader);
        if (perLoaderCache == null) {
            localizedFileCache.put(loader, perLoaderCache = new HashMap());
        }
        // #31008: better use of domain cache priming.
        // [PENDING] remove this hack in case the domain cache is precomputed
        URL baseVariant;
        String path;
        if (ext != null) {
            path = baseNameSlashes + '.' + ext;
        } else {
            path = baseNameSlashes;
        }
        lookup = (URL)perLoaderCache.get(path);
        if (lookup == null) {
            baseVariant = loader.getResource(path);
        } else {
            // who cares? already in cache anyway
            baseVariant = null;
        }
        while (it.hasNext ()) {
            String suffix = (String)it.next();
            if (ext != null) {
                path = baseNameSlashes + suffix + '.' + ext;
            } else {
                path = baseNameSlashes + suffix;
            }
            lookup = (URL)perLoaderCache.get(path);
            if (lookup != null)
                break;
            cacheCandidates.add(path);
            if (suffix.length() == 0) {
                lookup = baseVariant;
            } else {
                lookup = loader.getResource (path);
            }
            if (lookup != null)
                break;
        }
        if (lookup == null) {
            path = baseName.replace ('.', '/');
            if (ext != null) path += '.' + ext;
            throw new MissingResourceException("Cannot find localized resource " + path + " in " + loader, loader.toString(), path); // NOI18N
        }
        else {
            // Note that this is not 100% accurate. If someone calls gLF on something
            // with a locale/branding combo such as _brand_ja, and the answer is found
            // as _ja, then a subsequent call with param _brand will find this _ja
            // version - since the localizing iterator does *not* have the property that
            // each subsequent item is more general than the previous. However, this
            // situation is very unlikely, so consider this close enough.
            it = cacheCandidates.iterator();
            while (it.hasNext()) {
                perLoaderCache.put(it.next(), lookup);
            }
            return lookup;
        }
    }

    /** Find a localized value for a given key and locale.
    * Scans through a map to find
    * the most localized match possible. For example:
    * <p><code><PRE>
    *   findLocalizedValue (hashTable, "keyName", new Locale ("cs_CZ"))
    * </PRE></code>
    * <p>This would return the first non-<code>null</code> value obtained from the following tests:
    * <UL>
    * <LI> <CODE>hashTable.get ("keyName_cs_CZ")</CODE>
    * <LI> <CODE>hashTable.get ("keyName_cs")</CODE>
    * <LI> <CODE>hashTable.get ("keyName")</CODE>
    * </UL>
    *
    * @param table mapping from localized strings to objects
    * @param key the key to look for
    * @param locale the locale to use
    * @return the localized object or <code>null</code> if no key matches
    */
    public static Object getLocalizedValue (Map table, String key, Locale locale) {
        if (table instanceof Attributes) {
            throw new IllegalArgumentException ("Please do not use a java.util.jar.Attributes for NbBundle.getLocalizedValue " + // NOI18N
                                                "without using the special form that works properly with Attributes.Name's as keys."); // NOI18N
        }
        Iterator it = new LocaleIterator (locale);
        while (it.hasNext ()) {
            String physicalKey = key + (String) it.next ();
            Object v = table.get (physicalKey);
            if (v != null) {
                // ok
                if (USE_DEBUG_LOADER && (v instanceof String)) {
                    // Not read from a bundle, but still localized somehow:
                    return ((String) v) + " (?:" + physicalKey + ")"; // NOI18N
                } else {
                    return v;
                }
            }
        }
        return null;
    }

    /** Find a localized value for a given key in the default system locale.
    *
    * @param table mapping from localized strings to objects
    * @param key the key to look for
    * @return the localized object or <code>null</code> if no key matches
    * @see #getLocalizedValue(Map,String,Locale)
    */
    public static Object getLocalizedValue (Map table, String key) {
        return getLocalizedValue (table, key, Locale.getDefault ());
    }

    /** Find a localized value in a JAR manifest.
    * @param attr the manifest attributes
    * @param key the key to look for (case-insensitive)
    * @param locale the locale to use
    * @return the value if found, else <code>null</code>
    */
    public static String getLocalizedValue (Attributes attr, Attributes.Name key, Locale locale) {
        return (String)getLocalizedValue (attr2Map (attr), key.toString ().toLowerCase (Locale.US), locale);
    }

    /** Find a localized value in a JAR manifest in the default system locale.
    * @param attr the manifest attributes
    * @param key the key to look for (case-insensitive)
    * @return the value if found, else <code>null</code>
    */
    public static String getLocalizedValue (Attributes attr, Attributes.Name key) {
        // Yes, US locale is intentional! The attribute name may only be ASCII anyway.
        // It is necessary to lowercase it *as ASCII* as in Turkish 'I' does not go to 'i'!
        return (String)getLocalizedValue (attr2Map (attr), key.toString ().toLowerCase (Locale.US));
    }

    private static class AttributesMap extends HashMap {
        private Attributes attrs;
        
        public AttributesMap (Attributes attrs) {
            super (7);
            this.attrs = attrs;
        }

        public Object get (Object obj) {
            Attributes.Name an;
            try {
                an = new Attributes.Name ((String)obj);
            } catch (IllegalArgumentException iae) {
                // Robustness, and workaround for reported MRJ locale bug:
                ErrorManager em = ErrorManager.getDefault();
                em.annotate (iae, ErrorManager.WARNING, (String) obj,
                             getMessage (NbBundle.class, "EXC_bad_attributes_name",
                                         obj, Locale.getDefault ().toString ()),
                             null, null);
                em.notify (iae);
                return null;
            }
            return attrs.getValue (an);
        }
    }
    
    /** Necessary because Attributes implements Map; however this is dangerous!
    * The keys are Attributes.Name's, not Strings.
    * Also manifest lookups should not be case-sensitive.
    * (Though the locale suffix still will be!)
    */
    private static Map attr2Map (Attributes attr) {
        return new AttributesMap (attr);
    }
    
    // ---- LOADING RESOURCE BUNDLES ----

    /**
    * Get a resource bundle with the default class loader and locale.
    * <strong>Caution:</strong> {@link #getBundle(Class)} is generally
    * safer when used from a module as this method relies on the module's
    * classloader to currently be part of the system classloader. The
    * IDE does add enabled modules to this classloader, however calls to
    * this variant of the method made in {@link org.openide.modules.ModuleInstall#validate},
    * or made soon after a module is uninstalled (due to background threads)
    * could fail unexpectedly.
    * @param baseName bundle basename
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static final ResourceBundle getBundle(String baseName) throws MissingResourceException {
        return getBundle(baseName, Locale.getDefault(), getLoader());
    }

    /** Get a resource bundle in the same package as the provided class,
    * with the default locale and the class' own classloader.
    * This is the usual style of invocation.
    *
    * @param clazz the class to take the package name from
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static ResourceBundle getBundle (Class clazz) throws MissingResourceException {
        String name = findName (clazz);
        return getBundle(name, Locale.getDefault(), clazz.getClassLoader());
    }

    /** Finds package name for given class */
    private static String findName (Class clazz) {
        String pref = clazz.getName ();
        int last = pref.lastIndexOf ('.');
        if (last >= 0) {
            pref = pref.substring (0, last + 1);
            return pref + "Bundle"; // NOI18N
        } else {
            // base package, search for bundle
            return "Bundle"; // NOI18N
        }
    }

    /**
    * Get a resource bundle with the default class loader.
    * @param baseName bundle basename
    * @param locale the locale to use
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static final ResourceBundle getBundle(String baseName, Locale locale)
    throws MissingResourceException {
        return getBundle(baseName, locale, getLoader());
    }

    /** Get a resource bundle the hard way.
    * @param baseName bundle basename
    * @param locale the locale to use
    * @param loader the class loader to use
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static final ResourceBundle getBundle(String baseName, Locale locale,
            ClassLoader loader) throws MissingResourceException {
        if (USE_DEBUG_LOADER) loader = DebugLoader.get (loader);
        // Could more simply use ResourceBundle.getBundle (plus some special logic
        // with MergedBundle to handle branding) instead of manually finding bundles.
        // However this code is faster and has some other desirable properties.
        // Cf. #13847.
        ResourceBundle b = getBundleFast(baseName, locale, loader);
        if (b != null) {
            return b;
        } else {
            MissingResourceException e = new MissingResourceException("No such bundle " + baseName, baseName, null); // NOI18N
            if (Lookup.getDefault().lookup(ClassLoader.class) == null) {
                ErrorManager.getDefault().annotate(e, ErrorManager.UNKNOWN, "Class loader not yet initialized in lookup", null, null, null); // NOI18N
            } else {
                ErrorManager.getDefault().annotate(e, ErrorManager.UNKNOWN, "Offending classloader: " + loader, null, null, null); // NOI18N
            }
            throw e;
        }
    }
    
    /**
     * Cache of resource bundles.
     */
    private static final Map bundleCache = new WeakHashMap(); // Map<ClassLoader,Map<String,Reference<ResourceBundle>>>
    /**
     * Get a resource bundle by name.
     * Like {@link ResourceBundle#getBundle(String,Locale,ClassLoader)} but faster,
     * and also understands branding.
     * First looks for <samp>.properties</samp>-based bundles, then <samp>.class</samp>-based.
     * @param name the base name of the bundle, e.g. <samp>org.netbeans.modules.foo.Bundle</samp>
     * @param locale the locale to use
     * @param loader a class loader to search in
     * @return a resource bundle (locale- and branding-merged), or null if not found
     */
    private static ResourceBundle getBundleFast(String name, Locale locale, ClassLoader loader) {
        Map m;
        synchronized (bundleCache) {
            m = (Map)bundleCache.get(loader); // Map<String,Reference<ResourceBundle>>
            if (m == null) {
                bundleCache.put(loader, m = new HashMap());
            }
        }
        String key = name + '/' + (brandingToken != null ? brandingToken : "-") + '/' + locale; // NOI18N
        synchronized (m) {
            Object o = m.get(key);
            ResourceBundle b = (o != null) ? (ResourceBundle)((Reference)o).get() : null;
            if (b != null) {
                return b;
            } else {
                b = loadBundle(name, locale, loader);
                if (b != null) {
                    m.put(key, new TimedSoftReference(b, m, key));
                } else {
                    // Used to cache misses as well, to make the negative test faster.
                    // However this caused problems: see #31578.
                }
                return b;
            }
        }
    }
    
    /**
     * Load a resource bundle (without caching).
     * @param name the base name of the bundle, e.g. <samp>org.netbeans.modules.foo.Bundle</samp>
     * @param locale the locale to use
     * @param loader a class loader to search in
     * @return a resource bundle (locale- and branding-merged), or null if not found
     */
    private static ResourceBundle loadBundle(String name, Locale locale, ClassLoader loader) {
        String sname = name.replace('.', '/');
        Iterator it = new LocaleIterator(locale);
        LinkedList l = new LinkedList();
        while (it.hasNext()) {
            l.addFirst(it.next());
        }
        it = l.iterator();
        Properties p = new Properties();
        boolean first = true;
        while (it.hasNext()) {
            String res = sname + (String)it.next() + ".properties";
            InputStream is = loader.getResourceAsStream(res);
            if (is != null) {
                //System.err.println("Loading " + res);
                try {
                    try {
                        p.load(is);
                    } finally {
                        is.close();
                    }
                } catch (IOException e) {
                    ErrorManager.getDefault().notify(ErrorManager.WARNING, e);
                    return null;
                }
            } else if (first) {
                // No base *.properties. Try *.class.
                // Note that you may not mix *.properties w/ *.class this way.
                return loadBundleClass(name, sname, locale, l, loader);
            }
            first = false;
        }
        return new PBundle(p, locale);
    }
    
    /**
     * A resource bundle based on <samp>.properties</samp> files (or any map).
     */
    private static final class PBundle extends ResourceBundle {
        private final Map m; // Map<String,String>
        private final Locale locale;
        /**
         * Create a new bundle based on a map.
         * @param m a map from resources keys to values (typically both strings)
         * @param locale the locale it represents <em>(informational)</em>
         */
        public PBundle(Map m, Locale locale) {
            this.m = m;
            this.locale = locale;
        }
        public Enumeration getKeys() {
            return Collections.enumeration(m.keySet());
        }
        protected Object handleGetObject(String key) {
            return m.get(key);
        }
        public Locale getLocale() {
            return locale;
        }
    }
    
    /**
     * Load a class-based resource bundle.
     * @param name the base name of the bundle, e.g. <samp>org.netbeans.modules.foo.Bundle</samp>
     * @param sname the name with slashes, e.g. <samp>org/netbeans/modules/foo/Bundle</samp>
     * @param locale the locale to use
     * @param suffixes a list of suffixes to apply to the bundle name, in <em>increasing</em> order of specificity
     * @param loader a class loader to search in
     * @return a resource bundle (merged according to the suffixes), or null if not found
     */
    private static ResourceBundle loadBundleClass(String name, String sname, Locale locale, List suffixes, ClassLoader l) {
        if (l.getResource(sname + ".class") == null) { // NOI18N
            // No chance - no base bundle. Don't waste time catching CNFE.
            return null;
        }
        ResourceBundle master = null;
        Iterator it = suffixes.iterator();
        while (it.hasNext()) {
            try {
                Class c = Class.forName(name + (String)it.next(), true, l);
                ResourceBundle b = (ResourceBundle)c.newInstance();
                if (master == null) {
                    master = b;
                } else {
                    master = new MergedBundle(locale, b, master);
                }
            } catch (ClassNotFoundException cnfe) {
                // fine - ignore
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.WARNING, e);
            } catch (LinkageError e) {
                ErrorManager.getDefault().notify(ErrorManager.WARNING, e);
            }
        }
        return master;
    }
    
    /** Special resource bundle which delegates to two others.
     * Ideally could just set the parent on the first, but this is protected, so...
     */
    private static class MergedBundle extends ResourceBundle {
        private Locale loc;
        private ResourceBundle sub1, sub2;
        /**
         * Create a new bundle delegating to two others.
         * @param loc the locale it represents <em>(informational)</em>
         * @param sub1 one delegate (taking precedence over the other in case of overlap)
         * @param sub2 the other (weaker) delegate
         */
        public MergedBundle (Locale loc, ResourceBundle sub1, ResourceBundle sub2) {
            this.loc = loc;
            this.sub1 = sub1;
            this.sub2 = sub2;
        }
        public Locale getLocale () {
            return loc;
        }
        public Enumeration getKeys () {
            return new RemoveDuplicatesEnumeration
                (new SequenceEnumeration (sub1.getKeys (), sub2.getKeys ()));
        }
        protected Object handleGetObject (String key) throws MissingResourceException {
            try {
                return sub1.getObject (key);
            } catch (MissingResourceException mre) {
                // Ignore exception, and...
                return sub2.getObject (key);
            }
        }
    }

    //
    // Helper methods to simplify localization of messages
    //

    /** Finds a localized string in a bundle.
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    */
    public static String getMessage (Class clazz, String resName) throws MissingResourceException {
        return getBundle (clazz).getString(resName);
    }

    /** Finds a localized string in a bundle and formats the message
    * by passing requested parameters.
    *
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @param param1 the argument to use when formatting the message
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    * @see java.text.MessageFormat#format(String,Object[])
    */
    public static String getMessage (
        Class clazz, String resName, Object param1
    ) throws MissingResourceException {
        return getMessage (clazz, resName, new Object[] { param1 });
    }

    /** Finds a localized string in a bundle and formats the message
    * by passing requested parameters.
    *
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @param param1 the argument to use when formatting the message
    * @param param2 the second argument to use for formatting
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    * @see java.text.MessageFormat#format(String,Object[])
    */
    public static String getMessage (
        Class clazz, String resName, Object param1, Object param2
    ) throws MissingResourceException {
        return getMessage (clazz, resName, new Object[] { param1, param2 });
    }

    /** Finds a localized string in a bundle and formats the message
    * by passing requested parameters.
    *
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @param param1 the argument to use when formatting the message
    * @param param2 the second argument to use for formatting
    * @param param3 the third argument to use for formatting
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    * @see java.text.MessageFormat#format(String,Object[])
    */
    public static String getMessage (
        Class clazz, String resName, Object param1, Object param2, Object param3
    ) throws MissingResourceException {
        return getMessage (clazz, resName, new Object[] { param1, param2, param3 });
    }

    /** Finds a localized string in a bundle and formats the message
    * by passing requested parameters.
    *
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @param arr array of parameters to use for formatting the message
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    * @see java.text.MessageFormat#format(String,Object[])
    */
    public static String getMessage (
        Class clazz, String resName, Object[] arr
    ) throws MissingResourceException {
        return java.text.MessageFormat.format (
                   getMessage (clazz, resName), arr
               );
    }


    /** @return default class loader which is used, when we don't have
    * any other class loader. (in function getBundle(String), getLocalizedFile(String),
    * and so on...
    */
    private static ClassLoader getLoader() {
        ClassLoader c = (ClassLoader)Lookup.getDefault ().lookup (ClassLoader.class);
        return c != null ? c : ClassLoader.getSystemClassLoader ();
    }

    /** Get a list of all suffixes used to search for localized resources.
     * Based on the default locale and branding, returns the list of suffixes
     * which various <code>NbBundle</code> methods use as the search order.
     * For example, you might get a sequence such as:
     * <ol>
     * <li><samp>"_branding_de"</samp>
     * <li><samp>"_branding"</samp>
     * <li><samp>"_de"</samp>
     * <li><samp>""</samp>
     * </ol>
     * @return a read-only iterator of type <code>String</code>
     * @since 1.1.5
     */
    public static Iterator getLocalizingSuffixes () {
        return new LocaleIterator (Locale.getDefault ());
    }

    /** This class (enumeration) gives all localized sufixes using nextElement
    * method. It goes through given Locale and continues through Locale.getDefault()
    * Example 1:
    *   Locale.getDefault().toString() -> "_en_US"
    *   you call new LocaleIterator(new Locale("cs", "CZ"));
    *  ==> You will gets: "_cs_CZ", "_cs", "", "_en_US", "_en"
    *
    * Example 2:
    *   Locale.getDefault().toString() -> "_cs_CZ"
    *   you call new LocaleIterator(new Locale("cs", "CZ"));
    *  ==> You will gets: "_cs_CZ", "_cs", ""
    *
    * If there is a branding token in effect, you will get it too as an extra
    * prefix, taking precedence, e.g. for the token "f4jce":
    *
    * "_f4jce_cs_CZ", "_f4jce_cs", "_f4jce", "_f4jce_en_US", "_f4jce_en", "_cs_CZ", "_cs", "", "_en_US", "_en"
    *
    * Branding tokens with underscores are broken apart naturally: so e.g.
    * branding "f4j_ce" looks first for "f4j_ce" branding, then "f4j" branding, then none.
    */
    private static class LocaleIterator extends Object implements Iterator {
        /** this flag means, if default locale is in progress */
        private boolean defaultInProgress = false;

        /** this flag means, if empty sufix was exported yet */
        private boolean empty = false;

        /** current locale, and initial locale */
        private Locale locale, initLocale;

        /** current sufix which will be returned in next calling nextElement */
        private String current;
        
        /** the branding string in use */
        private String branding;
        
        /** Creates new LocaleIterator for given locale.
        * @param locale given Locale
        */
        public LocaleIterator(Locale locale) {
            this.locale = this.initLocale = locale;
            if (locale.equals(Locale.getDefault())) {
                defaultInProgress = true;
            }
            current = '_' + locale.toString();
            if (brandingToken == null)
                branding = null;
            else
                branding = "_" + brandingToken; // NOI18N
            //System.err.println("Constructed: " + this);
        }

        /** @return next sufix.
        * @exception NoSuchElementException if there is no more locale sufix.
        */
        public Object next () throws NoSuchElementException {
            if (current == null)
                throw new NoSuchElementException();
            
            final String ret;
            if (branding == null) {
                ret = current;
            } else {
                ret = branding + current;
            }
            int lastUnderbar = current.lastIndexOf('_');
            if (lastUnderbar == 0) {
                if (empty)
                    reset ();
                else {
                    current = ""; // NOI18N
                    empty = true;
                }
            }
            else {
                if (lastUnderbar == -1) {
                    if (defaultInProgress)
                        reset ();
                    else {
                        // [PENDING] stuff with trying the default locale
                        // after the real one does not actually seem to work...
                        locale = Locale.getDefault();
                        current = '_' + locale.toString();
                        defaultInProgress = true;
                    }
                }
                else {
                    current = current.substring(0, lastUnderbar);
                }
            }
            //System.err.println("Returning: `" + ret + "' from: " + this);
            return ret;
        }
        
        /** Finish a series.
         * If there was a branding prefix, restart without that prefix
         * (or with a shorter prefix); else finish.
         */
        private void reset () {
            if (branding != null) {
                current = '_' + initLocale.toString ();
                int idx = branding.lastIndexOf ('_');
                if (idx == 0)
                    branding = null;
                else
                    branding = branding.substring (0, idx);
                empty = false;
            } else {
                current = null;
            }
        }

        /** Tests if there is any sufix.*/
        public boolean hasNext () {
            return (current != null);
        }

        public void remove () throws UnsupportedOperationException {
            throw new UnsupportedOperationException ();
        }

    } // end of LocaleIterator

    /**
     * Do not use.
     * @param loaderFinder ignored
     * @deprecated Useless.
     */
    public static void setClassLoaderFinder (ClassLoaderFinder loaderFinder) {
        throw new Error ();
    }
    /**
     * Do not use.
     * @deprecated Useless.
     */
    public static interface ClassLoaderFinder {
        /**
         * Do not use.
         * @return nothing
         * @deprecated Useless.
         */
        public ClassLoader find ();
    }

    /** Classloader whose special trick is inserting debug information
     * into any *.properties files it loads.
     */
    private static final class DebugLoader extends ClassLoader {

        /** global bundle index, each loaded bundle gets its own */
        private static int count = 0;

        /** indices of known bundles; needed since DebugLoader's can be collected
         * when softly reachable, but this should be transparent to the user
         */
        private static final Map knownIDs = new HashMap (); // Map<String,int>

        /** cache of existing debug loaders for regular loaders */
        private static final Map existing = new WeakHashMap (); // Map<ClassLoader,Reference<DebugLoader>>

        private static int getID (String name) {
            synchronized (knownIDs) {
                Integer i = (Integer) knownIDs.get (name);
                if (i == null) {
                    i = new Integer (++count);
                    knownIDs.put (name, i);
                    System.err.println ("NbBundle trace: #" + i + " = " + name); // NOI18N
                }
                return i.intValue ();
            }
        }

        public static ClassLoader get (ClassLoader normal) {
            //System.err.println("Lookup: normal=" + normal);
            synchronized (existing) {
                Reference r = (Reference) existing.get (normal);
                if (r != null) {
                    ClassLoader dl = (ClassLoader) r.get ();
                    if (dl != null) {
                        //System.err.println("\tcache hit");
                        return dl;
                    } else {
                        //System.err.println("\tcollected ref");
                    }
                } else {
                    //System.err.println("\tnot in cache");
                }
                ClassLoader dl = new DebugLoader (normal);
                existing.put (normal, new WeakReference (dl));
                return dl;
            }
        }

        private DebugLoader (ClassLoader cl) {
            super (cl);
            //System.err.println ("new DebugLoader: cl=" + cl);
        }

        public InputStream getResourceAsStream (String name) {
            InputStream base = super.getResourceAsStream (name);
            if (base == null) return null;
            if (name.endsWith (".properties")) { // NOI18N
                int id = getID (name);
                //System.err.println ("\tthis=" + this + " parent=" + getParent ());
                boolean loc = name.indexOf ("/Bundle.") != -1 || name.indexOf ("/Bundle_") != -1; // NOI18N
                return new DebugInputStream (base, id, loc);
            } else {
                return base;
            }
        }

        // [PENDING] getResource not overridden; but ResourceBundle uses getResourceAsStream anyhow

        /** Wrapper input stream which parses the text as it goes and adds annotations.
         * Resource-bundle values are annotated with their current line number and also
         * the supplied it, so e.g. if in the original input stream on line 50 we have:
         *   somekey=somevalue
         * so in the wrapper stream (id 123) this line will read:
         *   somekey=somevalue (123:50)
         * Since you see on stderr what #123 is, you can then pinpoint where any bundle key
         * originally came from, assuming NbBundle loaded it from a *.properties file.
         * @see {@link Properties#load} for details on the syntax of *.properties files.
         */
        private static final class DebugInputStream extends InputStream {

            private final InputStream base;
            private final int id;
            private final boolean localizable;
            /** current line number */
            private int line = 0;
            /** state transition diagram constants */
            private static final int
                WAITING_FOR_KEY = 0,
                IN_COMMENT = 1,
                IN_KEY = 2,
                IN_KEY_BACKSLASH = 3,
                AFTER_KEY = 4,
                WAITING_FOR_VALUE = 5,
                IN_VALUE = 6,
                IN_VALUE_BACKSLASH = 7;
            /** current state in state machine */
            private int state = WAITING_FOR_KEY;
            /** if true, the last char was a CR, waiting to see if we get a NL too */
            private boolean twixtCrAndNl = false;
            /** if non-null, a string to serve up before continuing (length must be > 0) */
            private String toInsert = null;
            /** if true, the next value encountered should be localizable if normally it would not be, or vice-versa */
            private boolean reverseLocalizable = false;
            /** text of currently read comment, including leading comment character */
            private StringBuffer lastComment = null;

            /** Create a new InputStream which will annotate resource bundles.
             * Bundles named Bundle*.properties will be treated as localizable by default,
             * and so annotated; other bundles will be treated as nonlocalizable and not annotated.
             * Messages can be individually marked as localizable or not to override this default,
             * in accordance with some I18N conventions for NetBeans.
             * @param base the unannotated stream
             * @param id an identifying number to use in annotations
             * @param localizable if true, this bundle is expected to be localizable
             * @see http://www.netbeans.org/i18n/
             */
            public DebugInputStream (InputStream base, int id, boolean localizable) {
                this.base = base;
                this.id = id;
                this.localizable = localizable;
            }

            public int read () throws IOException {
                //try{
                if (toInsert != null) {
                    char result = toInsert.charAt (0);
                    if (toInsert.length () > 1) {
                        toInsert = toInsert.substring (1);
                    } else {
                        toInsert = null;
                    }
                    return result;
                }
                int next = base.read ();
                if (next == '\n') {
                    twixtCrAndNl = false;
                    line++;
                } else if (next == '\r') {
                    if (twixtCrAndNl) {
                        line++;
                    } else {
                        twixtCrAndNl = true;
                    }
                } else {
                    twixtCrAndNl = false;
                }
                switch (state) {
                case WAITING_FOR_KEY:
                    switch (next) {
                    case '#':
                    case '!':
                        state = IN_COMMENT;
                        lastComment = new StringBuffer ();
                        lastComment.append ((char) next);
                        return next;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                    case -1:
                        return next;
                    case '\\':
                        state = IN_KEY_BACKSLASH;
                        return next;
                    default:
                        state = IN_KEY;
                        return next;
                    }
                case IN_COMMENT:
                    switch (next) {
                    case '\n':
                    case '\r':
                        String comment = lastComment.toString ();
                        lastComment = null;
                        if (localizable && comment.equals ("#NOI18N")) { // NOI18N
                            reverseLocalizable = true;
                        } else if (localizable && comment.equals ("#PARTNOI18N")) { // NOI18N
                            System.err.println ("NbBundle WARNING (" + id + ":" + line + "): #PARTNOI18N encountered, will not annotate I18N parts"); // NOI18N
                            reverseLocalizable = true;
                        } else if (! localizable && comment.equals ("#I18N")) { // NOI18N
                            reverseLocalizable = true;
                        } else if (! localizable && comment.equals ("#PARTI18N")) { // NOI18N
                            System.err.println ("NbBundle WARNING (" + id + ":" + line + "): #PARTI18N encountered, will not annotate I18N parts"); // NOI18N
                            reverseLocalizable = false;
                        } else if ((localizable && (comment.equals ("#I18N") || comment.equals ("#PARTI18N"))) || // NOI18N
                                   (! localizable && (comment.equals ("#NOI18N") || comment.equals ("#PARTNOI18N")))) { // NOI18N
                            System.err.println ("NbBundle WARNING (" + id + ":" + line + "): incongruous comment " + comment + " found for bundle"); // NOI18N
                            reverseLocalizable = false;
                        }
                        state = WAITING_FOR_KEY;
                        return next;
                    default:
                        lastComment.append ((char) next);
                        return next;
                    }
                case IN_KEY:
                    switch (next) {
                    case '\\':
                        state = IN_KEY_BACKSLASH;
                        return next;
                    case ' ':
                    case '\t':
                        state = AFTER_KEY;
                        return next;
                    case '=':
                    case ':':
                        state = WAITING_FOR_VALUE;
                        return next;
                    case '\r':
                    case '\n':
                        state = WAITING_FOR_KEY;
                        return next;
                    default:
                        return next;
                    }
                case IN_KEY_BACKSLASH:
                    state = IN_KEY;
                    return next;
                case AFTER_KEY:
                    switch (next) {
                    case '=':
                    case ':':
                        state = WAITING_FOR_VALUE;
                        return next;
                    case '\r':
                    case '\n':
                        state = WAITING_FOR_KEY;
                        return next;
                    default:
                        return next;
                    }
                case WAITING_FOR_VALUE:
                    switch (next) {
                    case '\r':
                    case '\n':
                        state = WAITING_FOR_KEY;
                        return next;
                    case ' ':
                    case '\t':
                        return next;
                    case '\\':
                        state = IN_VALUE_BACKSLASH;
                        return next;
                    default:
                        state = IN_VALUE;
                        return next;
                    }
                case IN_VALUE:
                    switch (next) {
                    case '\\':
                        // Gloss over distinction between simple escapes and \u1234, which is not important for us.
                        // Also no need to deal specially with continuation lines; for us, there is an escaped
                        // newline, after which will be more value, and that is all that is important.
                        state = IN_VALUE_BACKSLASH;
                        return next;
                    case '\n':
                    case '\r':
                        // End of value. This is the tricky part.
                        boolean revLoc = reverseLocalizable;
                        reverseLocalizable = false;
                        state = WAITING_FOR_KEY;
                        // XXX don't annotate keys ending in _Mnemonic
                        if (localizable ^ revLoc) {
                            // This value is intended to be localizable. Annotate it.
                            toInsert = "(" + id + ":" + line + ")" + new Character ((char) next); // NOI18N
                            // Now return the space before the rest of the string explicitly.
                            return ' ';
                        } else {
                            // This is not supposed to be a localizable value, leave it alone.
                            return next;
                        }
                    default:
                        return next;
                    }
                case IN_VALUE_BACKSLASH:
                    state = IN_VALUE;
                    return next;
                default:
                    throw new IOException ("should never happen"); // NOI18N
                }
            }
            //catch(IOException ioe) {ioe.printStackTrace(); throw ioe;}
            //catch(RuntimeException re) {re.printStackTrace(); throw re;}
            //}

            /** For testing correctness of the transformation. Run:
             * java org.openide.util.NbBundle$DebugLoader$DebugInputStream true < test.properties
             * (The argument says whether to treat the input as localizable by default.)
             */
            public static void main (String[] args) throws Exception {
                if (args.length != 1) throw new Exception ();
                boolean loc = Boolean.valueOf (args[0]).booleanValue ();
                DebugInputStream dis = new DebugInputStream (System.in, 123, loc);
                int c;
                while ((c = dis.read ()) != -1) {
                    System.out.write (c);
                }
            }

        }

    }

}
