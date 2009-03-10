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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.MissingResourceException;

/**
* All the strings that should be localized will go through this place.
* Multiple custom localizers can be registered.
* The strings already retrieved are cached for the faster performance.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class LocaleSupport {

    private static final String NULL_STRING = new String();

    /** Cache for the retrieved strings */
    private static final HashMap cache = new HashMap(503);

    private static Localizer[] localizers = new Localizer[0];

    /** Add a new localizer to the localizer array. The array of localizers
    * is tracked from the lastly added localizer to the firstly added one
    * until the translation for the given key is found.
    * @param localizer localizer to add to the localizer array.
    */
    public static void addLocalizer(Localizer localizer) {
        ArrayList ll = new ArrayList(Arrays.asList(localizers));
        ll.add(localizer);
        Localizer[] la = new Localizer[ll.size()];
        ll.toArray(la);
        localizers = la;

        cache.clear();
    }

    /** Remove the existing localizer from the localizer array.
    * @param localizer localizer to remove.
    */
    public static void removeLocalizer(Localizer localizer) {
        ArrayList ll = new ArrayList(Arrays.asList(localizers));
        ll.remove(localizer);
        Localizer[] la = new Localizer[ll.size()];
        ll.toArray(la);
        localizers = la;

        cache.clear();
    }

    /** Get the localized string for the given key using the registered
    * localizers.
    * @param key key to translate to localized string.
    * @return localized string or null if there's no localization.
    */
    public static synchronized String getString(String key) {
        String ret = (String)cache.get(key);
        if (ret == null) {
            for (int i = localizers.length - 1; i >= 0; i--) {

                // Try to find a return value
                try {
                    ret = localizers[i].getString(key);
                } catch (MissingResourceException e) { // localizers are often bundles
                    ret = null;
                }

                if (ret != null) {
                    break;
                }
            }

            if (ret == null) {
                ret = NULL_STRING;
            }
            cache.put(key, ret);
        }

        return (ret != NULL_STRING) ? ret : null;
    }

    /** Get the localized string or the default value if no translation
    * for the given key exists.
    * @param key key to translate to localized string.
    * @param defaultValue default value to be returned in case no localized
    *   string is found for the given key.
    */
    public static String getString(String key, String defaultValue) {
        String ret = getString(key);
        return (ret != null) ? ret : defaultValue;
    }
    
    /** Get the localized character or the default value if no translation
     * for the given key exists. This method is mainly usable for getting
     * localized mnemonics.
     * @param key key to translate to localized character.
     * @param defaultValue default value to be returned in case no localized
     *        character is found for the given key.
     */
    public static char getChar( String key, char defaultValue ) {
        String value = getString( key );
        if( value == null || value.length() < 1 ) return defaultValue;
        return value.charAt( 0 );
    }

    /** Translates the keys to the localized strings. There can be multiple localizers
    * registered in the locale support.
    */
    public interface Localizer {

        /** Translate the key to the localized string.
        * @param key key to translate to the localized string.
        */
        public String getString(String key);

    }

}
