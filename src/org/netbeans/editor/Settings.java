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

import java.util.*;

/**
* Configurable settings that editor uses. All the methods are static
* The editor is configurable mainly by using the following static
* method in Settings class:
* 
*   org.netbeans.editor.Settings.setValue(Class kitClass, String settingName, Object newValue);
* 
* kitClass - this is the class of the editor kit for which the setting is changed.
*   The current hierarchy of editor kits starts
*   with the <tt>org.netbeans.editor.BaseKit</tt> kit, the begining of the whole
*   kit hierarchy. There should be a different editor kit for each mime-type.
* 
*   When the particular setting is not set foar a given kit, then the superclass of
*   the given kit class is retrieved and the search for the setting value is performed.
*   Example: If the java document calls Settings.getValue() to retrieve the value
*   for TAB_SIZE setting and it passes JavaKit.class as the kitClass
*   parameter and the setting has no value on this level, then the super class
*   of the JavaKit is retrieved (by using Class.getSuperclass() call) which is BaseKit
*   in this case and the search for the value of TAB_SIZE setting
*   is performed again. It is finished by reaching the null value for the kitClass.
*   The null value can be also used as the kitClass parameter value.
*   In a more general look not only the kit-class hierarchy could be used
*   in <tt>Settings</tt>. Any class inheritance hierarchy could be used here
*   having the null as the common root.
*
*   This way the inheritance of the setting values is guaranteed. By changing
*   the setting value on the BaseKit level (or even on the null level),
*   all the kit classes that don't
*   override the particular setting are affected.
* 
* settingName - name of the setting to change. The base setting names
*   are defined as public String constants in <tt>SettingsNames</tt> class.
*   The additional packages that extend the basic editor functionality
*   can define additional setting names.
* 
* newValue - new value for the setting. It must be always an object even
*   if the setting is logicaly the basic datatype such as int (java.lang.Integer
*   would be used in this case). A particular class types that can be used for
*   the value of the settings are documented for each setting.
*
* WARNING! Please read carefully the description for each option you're
*   going to change as you can make the editor stop working if you'll
*   change the setting in a wrong way.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class Settings {

    /** Core level used by the settings initializers. This is the level used
    * for the base and ext editor packages initializers only.
    */
    public static final int CORE_LEVEL = 0;

    /** System level used by the settings initializers. This is the (default)
    * first level.
    * It should be used by all the modules that install the new kits
    * into the editor.
    */
    public static final int SYSTEM_LEVEL = 1;

    /** Extension level used by the settings initializers. This is the second
    * level. It should be used by all the modules that want to extend
    * or modify the settings but they don't install their own kits.
    * The example can be a module extending the popup menu of an existing
    * kit.
    */
    public static final int EXTENSION_LEVEL = 2;

    /** Option level used by the settings initializers. This is the third
    * level. It should be used by the visual options created by the IDE.
    */
    public static final int OPTION_LEVEL = 3;

    /** User level used by the settings initializers. This is the fourth level.
    * All the initializers with this level will be called AFTER
    * all the initializers at the system level. All the user custom
    * initializers should be added at this level to guarantee
    * they will overwrite the settings added by the system.
    */
    public static final int USER_LEVEL = 4;

    /** List of Initializers */
    private static final ArrayList initializerLists = new ArrayList();

    /** Current initializer sorter. */
    private static InitializerSorter currentInitializerSorter;

    /** List of Filters */
    private static final ArrayList filterList = new ArrayList();

    /** [kit-class, map-of-settings] pairs */
    private static final Map kit2Maps = new HashMap();

    /** Support for firing change events */
    private static final WeakEventListenerList listenerList = new WeakEventListenerList();

    /** Internal map instance signing that initializer returned null
    * map for particular kit. To sign this fact and not query initializer
    * again, this simple map is used.
    */
    private static final Map NULL_MAP = new HashMap(1);

    private static boolean firingEnabled = true;

    /** Save repetitive creation of the empty maps using this variable */
    private static HashMap emptyMap = null;

    private Settings() {
        // no instances allowed
    }

    /** Add the initializer at the system level and perform reset. */
    public static synchronized void addInitializer(Initializer i) {
        addInitializer(i, SYSTEM_LEVEL);
        reset();
    }

    /** Add initializer instance to the list of current initializers.
    * You can call reset() after adding the initializer to make sure
    * it will update the current settings with its values.
    * However all the changes
    * that were made explicitly by calling setValue() will be lost
    * in this case.
    *
    * @param i initializer to add to the current list of initializers
    * @param level initializer level. It defines in which order
    *  the initializers will be called. There are currently three levels
    *  <tt>CORE_LEVEL</tt>, <tt>SYSTEM_LEVEL</tt> and <tt>USER_LEVEL</tt>.
    *  It's guaranteed that initializers with the particular level
    *  will be called in the order shown above.
    *  The order of the initializers at the same
    *  level is given by the order of their addition.
    */
    public static synchronized void addInitializer(Initializer i, int level) {
        int size = initializerLists.size();
        for (int j = size; j <= level; j++) {
            initializerLists.add(new ArrayList());
        }
        ((List)initializerLists.get(level)).add(i);

        // Sort the initializers if there's a valid sorter
        if (currentInitializerSorter != null) {
            currentInitializerSorter.sort(initializerLists);
        }
    }

    /** Remove the initializer of the given name from all the levels
    * where it occurs.
    * @param name name of the initializer sorter to remove.
    */
    public static synchronized void removeInitializer(String name) {
        Iterator itit = initializerLists.iterator();
        while (itit.hasNext()) {
            Iterator it = ((List)itit.next()).iterator();
            while (it.hasNext()) {
                if (name.equals(((Initializer)it.next()).getName())) {
                    it.remove();
                }
            }
        }

        // Sort the initializers if there's a valid sorter
        if (currentInitializerSorter != null) {
            currentInitializerSorter.sort(initializerLists);
        }
    }

    /** Get the current initializer sorter. */
    public static synchronized InitializerSorter getInitializerSorter() {
        return currentInitializerSorter;
    }

    /** Set the current initializer sorter. */
    public static synchronized void setInitializerSorter(InitializerSorter initializerSorter) {
        currentInitializerSorter = initializerSorter;
    }

    /** Add filter instance to the list of current filters.
    * If there are already existing editor components,
    * and you want to apply the changes that this filter makes
    * to these existing
    * components, you can call reset(). However all the changes
    * that were made explicitly by calling setValue() will be lost
    * in this case.
    *
    * @param f filter to add to the list of the filters
    */
    public static synchronized void addFilter(Filter f) {
        filterList.add(f);
    }

    public static synchronized void removeFilter(Filter f) {
        Iterator it = filterList.iterator();
        while (it.hasNext()) {
            if (it.next() == f) {
                it.remove();
            }
        }
    }

    /** Get the value and evaluate the evaluators. */
    public static Object getValue(Class kitClass, String settingName) {
        return getValue(kitClass, settingName, true);
    }

    /** Get the property by searching the given kit class settings and if not
    * found then the settings for super class and so on.
    * @param kitClass editor kit class for which the value of setting should
    *   be retrieved. The null can be used as the root of the whole hierarchy.
    * @param settingName name of the setting for which the value should
    *   be retrieved
    * @return the value of the setting
    */
    public static synchronized Object getValue(Class kitClass, String settingName,
            boolean evaluateEvaluators) {
        Object value = null;
        Class kc = kitClass;
        while (true) {
            Map map = getKitMap(kc, false);
            if (map != null) {
                value = map.get(settingName);
                if (evaluateEvaluators && value instanceof Evaluator) {
                    value = ((Evaluator)value).getValue(kitClass, settingName);
                }
                if (value != null) {
                    break;
                }
            }
            if (kc == null) {
                break;
            }
            kc = kc.getSuperclass();
        }

        // filter the value if necessary
        int cnt = filterList.size();
        for (int i = 0; i < cnt; i++) {
            value = ((Filter)filterList.get(i)).filterValue(kitClass, settingName, value);
        }

        return value;
    }

    /** Get the value hierarchy and evaluate the evaluators */
    public static KitAndValue[] getValueHierarchy(Class kitClass,
            String settingName) {
        return getValueHierarchy(kitClass, settingName, true);
    }

    /** Get array of KitAndValue objects sorted from the given kit class to its
    * deepest superclass and the last member can be filled whether there
    * is global setting (kit class of that member would be null).
    * This method is useful for objects like keymaps that
    * need to create all the parent keymaps to work properly.
    * The method can either evaluate evaluators or leave them untouched
    * which can become handy in some cases.
    * @param kitClass editor kit class for which the value of setting should
    *   be retrieved. The null can be used as the root of the whole hierarchy.
    * @param settingName name of the setting for which the value should
    *   be retrieved
    * @param evaluateEvaluators whether the evaluators should be evaluated or not
    * @return the array containing KitAndValue instances describing the particular
    *   setting's value on the specific kit level.
    */
    public static synchronized KitAndValue[] getValueHierarchy(Class kitClass,
            String settingName, boolean evaluateEvaluators) {
        ArrayList kavList = new ArrayList();
        Class kc = kitClass;
        while (true) {
            Map map = getKitMap(kc, false);
            if (map != null) {
                Object value = map.get(settingName);
                if (evaluateEvaluators && value instanceof Evaluator) {
                    value = ((Evaluator)value).getValue(kitClass, settingName);
                }
                if (value != null) {
                    kavList.add(new KitAndValue(kc, value));
                }
            }
            if (kc == null) {
                break;
            }
            kc = kc.getSuperclass();
        }
        KitAndValue[] kavArray = (KitAndValue[])kavList.toArray(
                                     new KitAndValue[kavList.size()]);

        // filter the value if necessary
        int cnt = filterList.size();
        for (int i = 0; i < cnt; i++) {
            kavArray = ((Filter)filterList.get(i)).filterValueHierarchy(
                           kitClass, settingName, kavArray);
        }

        return kavArray;
    }

    /** Set the new value for property on kit level. The old and new values
    * are compared and if they are equal the setting is not changed and
    * nothing is fired.
    * 
    * @param kitClass editor kit class for which the value of setting should
    *   be set. The null can be used as the root of the whole hierarchy.
    * @param settingName the string used for searching the value
    * @param newValue new value to set for the property; the value can
    *   be null to clear the value for the specified kit
    */
    public static synchronized void setValue(Class kitClass, String settingName,
            Object newValue) {
        Map map = getKitMap(kitClass, true);
        Object oldValue = map.get(settingName);
        if (oldValue == null && newValue == null
                || (oldValue != null && oldValue.equals(newValue))
           ) {
            return; // no change
        }
        if (newValue != null) {
            map.put(settingName, newValue);
        } else {
            map.remove(settingName);
        }
        fireSettingsChange(kitClass, settingName, oldValue, newValue);
    }

    /** Don't change the value of the setting, but fire change
    * event. This is useful when there's internal change in the value object
    * of some setting.
    */
    public static synchronized void touchValue(Class kitClass, String settingName) {
        fireSettingsChange(kitClass, settingName, null, null); // kit class currently not used
    }

    /** Set the value for the current kit and propagate it to all
    * the children of the given kit by removing
    * the possible values for the setting from the children kit setting maps.
    * Note: This call only affects the settings for the kit classes for which
    * the kit setting map with the setting values currently exists, i.e. when
    * there was at least one getValue() or setValue() call performed for any
    * setting on that particular kit class level. Other kit classes maps
    * will be initialized by the particular initializer(s) as soon as
    * the first getValue() or setValue() will be performed for them.
    * However that future process will not be affected by the current
    * propagateValue() call.
    * This method is useful for the visual options that always set
    * the value on all the kit levels without regard whether it's necessary or not.
    * If the value is then changed for the base kit, it's not propagated
    * naturally as there's a special setting
    * This method enables 
    * 
    * The current implementation always fires the change regardless whether
    * there was real change in setting value or not.
    * @param kitClass editor kit class for which the value of setting should
    *   be set.  The null can be used as the root of the whole hierarchy.
    * @param settingName the string used for searching the value
    * @param newValue new value to set for the property; the value can
    *   be null to clear the value for the specified kit
    */
    public static synchronized void propagateValue(Class kitClass,
            String settingName, Object newValue) {
        Map map = getKitMap(kitClass, true);
        if (newValue != null) {
            map.put(settingName, newValue);
        } else {
            map.remove(settingName);
        }
        // resolve kits
        Iterator it = kit2Maps.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry me = (Map.Entry)it.next();
            Class kc = (Class)me.getKey();
            if (kitClass != kc && (kitClass == null || kitClass.isAssignableFrom(kc))) {
                ((Map)me.getValue()).remove(settingName);
            }
        }
        fireSettingsChange(null, settingName, null, null);
    }

    /** Run the given runnable. All the changes in the settings are not fired until
    * the whole runnable completes. Nesting of <tt>update()</tt> call is allowed.
    * Only one firing is performed after the whole runnable completes
    * using the 'null triple'.
    */
    public static synchronized void update(Runnable r) {
        boolean turnedOff = firingEnabled;
        firingEnabled = false;
        try {
            r.run();
        } finally {
            if (turnedOff) {
                firingEnabled = true;
                fireSettingsChange(null, null, null, null);
            }
        }
    }

    /** Reset all the settings and fire the change of the settings
    * so that all the listeners will be notified and will reload
    * the settings.
    * The settings that were changed using setValue() and propagateValue()
    * are lost. Initializers will be asked for the settings values when
    * necessary.
    */
    public static synchronized void reset() {
        kit2Maps.clear();
        fireSettingsChange(null, null, null, null);
    }

    /** Debug the current initializers */
    public static synchronized String initializersToString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < initializerLists.size(); i++) {
            // debug the level
            switch (i) {
            case CORE_LEVEL:
                sb.append("CORE_LEVEL");
                break;

            case SYSTEM_LEVEL:
                sb.append("SYSTEM_LEVEL");
                break;

            case EXTENSION_LEVEL:
                sb.append("EXTENSION_LEVEL");
                break;
                
            case OPTION_LEVEL:
                sb.append("OPTION_LEVEL");
                break;

            case USER_LEVEL:
                sb.append("USER_LEVEL");
                break;

            default:
                sb.append("level " + i);
                break;
            }
            sb.append(":\n");

            // debug the initializers
            sb.append(EditorDebug.debugList((List)initializerLists.get(i)));
            sb.append('\n');
        }

        return sb.toString();
    }

    /** Add weak listener to listen to change of any property. The caller must
    * hold the listener object in some instance variable to prevent it
    * from being garbage collected.
    */
    public static void addSettingsChangeListener(SettingsChangeListener l) {
        listenerList.add(SettingsChangeListener.class, l);
    }

    /** Remove listener for changes in properties */
    public static void removeSettingsChangeListener(SettingsChangeListener l) {
        listenerList.remove(SettingsChangeListener.class, l);
    }

    private static void fireSettingsChange(Class kitClass, String settingName,
                                           Object oldValue, Object newValue) {
        if (firingEnabled) {
            SettingsChangeListener[] listeners = (SettingsChangeListener[])
                                                 listenerList.getListeners(SettingsChangeListener.class);
            SettingsChangeEvent evt = new SettingsChangeEvent(Settings.class,
                                      kitClass, settingName, oldValue, newValue);
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].settingsChange(evt);
            }
        }
    }

    /** Get (and possibly create) kit map for particular kit */
    private static Map getKitMap(Class kitClass, boolean forceCreation) {
        Map kitMap = (Map)kit2Maps.get(kitClass);
        if (kitMap == null) {
            if (emptyMap == null) {
                emptyMap = new HashMap();
            }

            // Go through all the initializers
            Iterator itit = initializerLists.iterator();
            while (itit.hasNext()) {
                Iterator it = ((List)itit.next()).iterator();
                while (it.hasNext()) {
                    Initializer i = (Initializer)it.next();

                    // A call to initializer shouldn't break the whole updating
                    try {
                        i.updateSettingsMap(kitClass, emptyMap);
                    } catch (Throwable t) {
                        if (System.getProperty("netbeans.debug.exceptions") != null) { // NOI18N
                            t.printStackTrace();
                        }
                    }
                }
            }

            if (emptyMap.size() > 0) {
                kitMap = emptyMap;
                emptyMap = null;
            }

            if (kitMap == null) { // no initialization done for this kit
                kitMap = NULL_MAP; // initializer will not be called again
            }
            kit2Maps.put(kitClass, kitMap);
        }

        if (kitMap == NULL_MAP) {
            if (!forceCreation) {
                return null;
            } else {
                kitMap = new HashMap(); // create empty map
                kit2Maps.put(kitClass, kitMap);
            }
        }
        return kitMap;
    }


    /** Kit class and value pair */
    public static class KitAndValue {

        public Class kitClass;

        public Object value;

        public KitAndValue(Class kitClass, Object value) {
            this.kitClass = kitClass;
            this.value = value;
        }

    }


    /** Initializer of the settings updates the map filled
    * with settings for the particular kit class when asked.
    * If the settings are being initialized all the initializers registered
    * by the <tt>Settings.addInitializer()</tt> are being asked to update
    * the settings-map through calling their <tt>updateSettingsMap()</tt>.
    */
    public static interface Initializer {

        /** Each initializer must have a name. The name should be unique.
        * The name is used for identifying the initializer during removal
        * and sort operations and for debuging purposes.
        */
        public String getName();

        /** Update map filled with the settings.
        * @param kitClass kit class for which the settings are being updated.
        *    It can be null which means the root of the whole kit class hierarchy.
        * @param settingsMap map holding [setting-name, setting-value] pairs.
        *   The map can be empty if this is the first initializer
        *   that updates it or if no previous initializers updated it.
        */
        public void updateSettingsMap(Class kitClass, Map settingsMap);

    }

    /** Abstract implementation of the initializer dealing with the name. */
    public static abstract class AbstractInitializer implements Initializer {

        private String name;

        public AbstractInitializer(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return getName();
        }

    }

    /** Sort the settings initializers that were added to the settings.
    * There can be only one sorter for the Settings, but it can delegate
    * to previously registered sorter.
    */
    public static interface InitializerSorter {

        public void sort(List initializersList);

    }

    /** Initializer sorter that delegates to another sorter. */
    public static abstract class FilterInitializerSorter {

        private InitializerSorter delegate;

        public FilterInitializerSorter(InitializerSorter delegate) {
            this.delegate = delegate;
        }

        public void sort(List initializersList) {
            if (delegate != null) {
                delegate.sort(initializersList);
            }
        }

    }



    /** Evaluator can be used in cases when value of some setting
    * depends on the value for other setting and it allows to compute
    * the value dynamically based on the other setting(s) value.
    * The <tt>Evaluator</tt> instance can be used as the value
    * in the <tt>Settings.setValue()</tt> call. In that case the call
    * to the <tt>Settings.getValue()</tt> call will 'evaluate' the Evaluator
    * by calling its <tt>getValue()</tt>.
    */
    public static interface Evaluator {

        /** Compute the particular setting's value.
        * @param kitClass kit class for which the setting is being retrieved.
        * @param settingName name of the setting to retrieve. Although the Evaluator
        *   are usually constructed only for the concrete setting, this parameter
        *   allows creation of the Evaluator for multiple settings.
        * @return the value for the requested setting. The substitution
        *   is not attempted again, so the return value cannot be another
        *   Evaluator instance. If the returned value is null, the same
        *   action is taken as if there would no value set on the particular
        *   kit level.
        *   
        */
        public Object getValue(Class kitClass, String settingName);

    }


    /** Filter is applied on every value or KitAndValue pairs returned from getValue().
    * The filter can be registered by calling <tt>Settings.addFilter()</tt>.
    * Each call to <tt>Settings.getValue()</tt> will first retrieve the value and
    * then call the <tt>Filter.filterValue()</tt> to get the final value. Each call
    * to <tt>Settings.getValueHierarchy()</tt> will first retrieve the kit-and-value
    * array and then call the <tt>Filter.filterValueHierarchy()</tt>.
    * If more filters are registered they are all used in the order they were added.
    */
    public static interface Filter {

        /** Filter single value. The value can be substituted here.
        * @param kitClass class of the kit for which the value is retrieved
        * @param settingName name of the retrieved setting
        * @param value value to be optionally filtered
        */
        public Object filterValue(Class kitClass, String settingName, Object value);

        /** Filter array of kit-class and value pairs. The pairs can be completely
        * substituted with an array with different length and different members.
        * @param kitClass class of the kit for which the value is retrieved
        * @param settingName name of the retrieved setting
        * @param kavArray kit-class and value array to be filtered
        */
        public KitAndValue[] filterValueHierarchy(Class kitClass, String settingName,
                KitAndValue[] kavArray);

    }


}
