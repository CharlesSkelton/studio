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

package org.netbeans.editor;

import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/** Definition of the annotation type. Annotation type is defined by attributes like
 * highlight color, foreground color, glyph icon, etc. Each annotation added to document
 * has reference to the name of the annotation type which defines how the annotation
 * will be drawn.
 *
 * @author David Konecny
 * @since 07/2001
 */

public class AnnotationType {

    /** Property name for Name (String) */
    public static final String PROP_NAME = "name";

    /** Property name for Description (String) */
    public static final String PROP_DESCRIPTION = "description";

    /** Property name for Visible (boolean) */
    public static final String PROP_VISIBLE = "visible";
    
    /** Property name for Glyph (URL) */
    public static final String PROP_GLYPH_URL = "glyph";

    /** Property name for Highlight (Color) */
    public static final String PROP_HIGHLIGHT_COLOR = "highlight";

    /** Property name for Foreground (Color) */
    public static final String PROP_FOREGROUND_COLOR = "foreground";

    /** Property name for WaveUnderline (Color) */
    public static final String PROP_WAVEUNDERLINE_COLOR = "waveunderline";

    /** Property name for WholeLine (boolean) */
    public static final String PROP_WHOLE_LINE = "wholeline";
    
    /** Property name for ContentType (String) */
    public static final String PROP_CONTENT_TYPE = "contenttype";

    /** Property name for Actions (Action[]) */
    public static final String PROP_ACTIONS = "actions";

    /** Property name for TooltipText (String) */
    public static final String PROP_TOOLTIP_TEXT = "tooltipText";

    /** Property name for InheritForegroundColor (Boolean) */
    public static final String PROP_INHERIT_FOREGROUND_COLOR = "inheritForegroundColor";
    
    /** Property name for UseHighlightColor (Boolean) */
    public static final String PROP_USE_HIGHLIGHT_COLOR = "useHighlightColor";

    /** Property name for UseWaveUnderlineColor (Boolean) */
    public static final String PROP_USE_WAVEUNDERLINE_COLOR = "useWaveUnderlineColor";

    /** Property name for Combinations (AnnotationType.CombinationMember[]). 
     * If some annotation type has set this property, it means that editor
     * must check if line contains all types which are defined in this array.
     * If it contains, then all this annotation types become hidden and this 
     * type is shown instead of them. */
    public static final String PROP_COMBINATIONS = "combinations";

    public static final String PROP_COMBINATION_ORDER = "combinationOrder";

    public static final String PROP_COMBINATION_MINIMUM_OPTIONALS = "combinationMinimumOptionals";

    /** Property holding the object which represent the source of this annotation type.
     * This property is used during the saving of the changes in annotation type. */
    public static final String PROP_FILE = "file";

    public static final String PROP_LOCALIZING_BUNDLE = "bundle";
    
    public static final String PROP_DESCRIPTION_KEY = "desciptionKey";
    
    public static final String PROP_ACTIONS_FOLDER = "actionsFolder";
    
    public static final String PROP_COMBINATION_TOOLTIP_TEXT_KEY = "tooltipTextKey";
    
    /** Storage of all annotation type properties. */
    private Map properties;

    /** Support for property change listeners*/
    private PropertyChangeSupport support;
    
    /** Glyph icon loaded from URL into Image */
    private Image img = null;
    
    /** Coloring composed from foreground and highlight color*/
    private Coloring col;
    
    public AnnotationType() {
        properties = new HashMap(15*4/3);
        support = new PropertyChangeSupport(this);
    }

    /** Getter for Glyph property
     * @return  URL of the glyph icon */    
    public java.net.URL getGlyph() {
        URL u = (java.net.URL)getProp(PROP_GLYPH_URL);
        if (u == null)
            u = AnnotationTypes.getDefaultGlyphURL();
        return u;
    }

    /** Setter for the Glyph property
     * @param glyph URL to gpylh icon */    
    public void setGlyph(java.net.URL glyph) {
        putProp(PROP_GLYPH_URL, glyph);
    }

    /** Gets Image which represent the glyph. This method is called 
     * only from AWT thead and so it is not necessary to synchronize it.
     */
    public Image getGlyphImage() {
        if (img == null) {
            img = Toolkit.getDefaultToolkit().getImage(getGlyph());
        }
        return img;
    }
    
    /** Whether the annotation type has its own glyph icon or not */
    public boolean isDefaultGlyph() {
        if (getProp(PROP_GLYPH_URL) == null)
            return true;
        else
            return false;
    }
    
    /** Getter for Highlight property
     * @return  highlight color */    
    public java.awt.Color getHighlight() {
        return (java.awt.Color)getProp(PROP_HIGHLIGHT_COLOR);
    }        

    /** Setter for the Highlight property
     * @param highlight highlight color */    
    public void setHighlight(java.awt.Color highlight) {
        col = null; // force the create new coloring
        putProp(PROP_HIGHLIGHT_COLOR, highlight);
        firePropertyChange(PROP_HIGHLIGHT_COLOR, null, null);
        processChange();
    }

    /** Getter for UseHighlightColor property
     * @return  whether the highlight color should be used or not */    
    public boolean isUseHighlightColor() {
        Boolean b = (Boolean)getProp(PROP_USE_HIGHLIGHT_COLOR);
        if (b == null)
            return true;
        return b.booleanValue();
    }

    /** Setter for the UseHighlightColor property
     * @param use use highlight color */    
    public void setUseHighlightColor(boolean use) {
        if (isUseHighlightColor() != use) {
            col = null; // force the create new coloring
            putProp(PROP_USE_HIGHLIGHT_COLOR, use ? Boolean.TRUE : Boolean.FALSE);
            firePropertyChange(PROP_USE_HIGHLIGHT_COLOR, null, null);
            processChange();
        }
    }
    
    /** Getter for Foreground property
     * @return  foreground color */    
    public java.awt.Color getForegroundColor() {
        return (java.awt.Color)getProp(PROP_FOREGROUND_COLOR);
    }

    /** Setter for the Foreground property
     * @param foregroundColor foreground color */    
    public void setForegroundColor(java.awt.Color foregroundColor) {
        col = null; // force the create new coloring
        putProp(PROP_FOREGROUND_COLOR, foregroundColor);
        firePropertyChange(PROP_FOREGROUND_COLOR, null, null);
        processChange();
    }

    /** Getter for InheritForegroundColor property
     * @return  whether the foreground color should be inherit or not */    
    public boolean isInheritForegroundColor() {
        Boolean b = (Boolean)getProp(PROP_INHERIT_FOREGROUND_COLOR);
        if (b == null)
            return true;
        return b.booleanValue();
    }

    /** Setter for the InheritfForegroundColor property
     * @param inherit inherit foreground color */    
    public void setInheritForegroundColor(boolean inherit) {
        if (isInheritForegroundColor() != inherit) {
            col = null; // force the create new coloring
            putProp(PROP_INHERIT_FOREGROUND_COLOR, inherit ? Boolean.TRUE : Boolean.FALSE);
            firePropertyChange(PROP_INHERIT_FOREGROUND_COLOR, null, null);
            processChange();
        }
    }
    
    /** Getter for WaveUnderline property
     * @return  waveunderline color */    
    public java.awt.Color getWaveUnderlineColor() {
        return (java.awt.Color)getProp(PROP_WAVEUNDERLINE_COLOR);
    }        

    /** Setter for the WaveUnderline property
     * @param waveunderline wave underline color */    
    public void setWaveUnderlineColor(java.awt.Color waveunderline) {
        col = null; // force the create new coloring
        putProp(PROP_WAVEUNDERLINE_COLOR, waveunderline);
        firePropertyChange(PROP_WAVEUNDERLINE_COLOR, null, null);
        processChange();
    }

    /** Getter for UseWaveUnderlineColor property
     * @return  whether the waveunderline color should be used or not */    
    public boolean isUseWaveUnderlineColor() {
        Boolean b = (Boolean)getProp(PROP_USE_WAVEUNDERLINE_COLOR);
        if (b == null)
            return true;
        return b.booleanValue();
    }

    /** Setter for the UseWaveUnderlineColor property
     * @param use use wave underline color */    
    public void setUseWaveUnderlineColor(boolean use) {
        if (isUseWaveUnderlineColor() != use) {
            col = null; // force the create new coloring
            putProp(PROP_USE_WAVEUNDERLINE_COLOR, use ? Boolean.TRUE : Boolean.FALSE);
            firePropertyChange(PROP_USE_WAVEUNDERLINE_COLOR, null, null);
            processChange();
        }
    }
    
    /** Process change of some setting. It means that 
     * listeners are notified and change is saved. */
    private void processChange() {
        // if type does not have this property it is just being loaded
        if (getProp(AnnotationType.PROP_FILE) == null)
            return;
        // force repaint of all documents
        Settings.touchValue(null, null);
        AnnotationTypes.getTypes().saveType(this);
    }

    /** Gets all the colors composed as Coloring
     * @return  coloring containing all colors */    
    public Coloring getColoring() {
        if (col == null)
            col = new Coloring(null, Coloring.FONT_MODE_DEFAULT, isInheritForegroundColor() ? null : getForegroundColor(), isUseHighlightColor() ? getHighlight() : null, null, null, isUseWaveUnderlineColor() ? getWaveUnderlineColor() : null);
        return col;
    }
    
    /** Getter for Actions property
     * @return array of actions */    
    public javax.swing.Action[] getActions() {
        return (javax.swing.Action[])getProp(PROP_ACTIONS);
    }

    /** Setter for Actions property
     * @return array of actions */    
    public void setActions(javax.swing.Action[] actions) {
        putProp(PROP_ACTIONS, actions);
    }

    /** Getter for Combinations property
     * @return array of combinations */    
    public CombinationMember[] getCombinations() {
        return (CombinationMember[])getProp(PROP_COMBINATIONS);
    }

    /** Setter for Combinations property */
    public void setCombinations(CombinationMember[] combs) {
        putProp(PROP_COMBINATIONS, combs);
    }

    /** Getter for Name property
     * @return annotation type name */    
    public String getName() {
        return (String)getProp(PROP_NAME);
    }

    /** Setter for the Name property
     * @param name name of the annotation type */    
    public void setName(String name) {
        putProp(PROP_NAME, name);
    }
    
    /** Getter for Description property
     * @return localized description of the annotation type */    
    public String getDescription() {
        String desc = (String)getProp(PROP_DESCRIPTION);
        if (desc == null) {
            String localizer = (String)getProp(PROP_LOCALIZING_BUNDLE);
            String key = (String)getProp(PROP_DESCRIPTION_KEY);
            ResourceBundle bundle = ImplementationProvider.getDefault().getResourceBundle(localizer);
            desc = bundle.getString(key);
            setDescription(desc); // cache it
        }
        return desc;
    }

    /** Setter for the Description property
     * @param name localized description of the annotation type */    
    public void setDescription(String name) {
        putProp(PROP_DESCRIPTION, name);
    }
    
    /** Getter for TooltipText property
     * @return localized TooltipText of the annotation type */    
    public String getTooltipText() {
        String text = (String)getProp(PROP_TOOLTIP_TEXT);
        if (text == null) {
            String localizer = (String)getProp(PROP_LOCALIZING_BUNDLE);
            String key = (String)getProp(PROP_COMBINATION_TOOLTIP_TEXT_KEY);
            ResourceBundle bundle = ImplementationProvider.getDefault().getResourceBundle(localizer);
            text = bundle.getString(key);
            setTooltipText(text); // cache it
        }
        return text;
    }

    /** Setter for the TooltipText property
     * @param name localized TooltipText of the annotation type */    
    public void setTooltipText(String text) {
        putProp(PROP_TOOLTIP_TEXT, text);
    }
    
    /** Getter for CombinationOrder property
     * @return order of the annotation type */    
    public int getCombinationOrder() {
        if (getProp(PROP_COMBINATION_ORDER) == null)
            return 0;
        return ((Integer)getProp(PROP_COMBINATION_ORDER)).intValue();
    }

    /** Setter for the CombinationOrder property
     * @param order order of the annotation type combination */    
    public void setCombinationOrder(int order) {
        putProp(PROP_COMBINATION_ORDER, new Integer(order));
    }
    
    /** Setter for the CombinationOrder property
     * @param ord order of the annotation type combination */    
    public void setCombinationOrder(String ord) {
        int order;
        try {
            order = Integer.parseInt(ord);
        } catch (NumberFormatException ex) {
            if( Boolean.getBoolean( "netbeans.debug.exceptions" ) )
                ex.printStackTrace();
            return;
        }
        putProp(PROP_COMBINATION_ORDER, new Integer(order));
    }

    /** Getter for MinimumOptionals property
     * @return minimum number of the optional annotation types which
     * must be matched */    
    public int getMinimumOptionals() {
        if (getProp(PROP_COMBINATION_MINIMUM_OPTIONALS) == null)
            return 0;
        return ((Integer)getProp(PROP_COMBINATION_MINIMUM_OPTIONALS)).intValue();
    }

    public void setMinimumOptionals(int min) {
        putProp(PROP_COMBINATION_MINIMUM_OPTIONALS, new Integer(min));
    }
    
    public void setMinimumOptionals(String m) {
        int min;
        try {
            min = Integer.parseInt(m);
        } catch (NumberFormatException ex) {
            if( Boolean.getBoolean( "netbeans.debug.exceptions" ) )
                ex.printStackTrace();
            return;
        }
        putProp(PROP_COMBINATION_MINIMUM_OPTIONALS, new Integer(min));
    }
    
    /** Getter for Visible property
     * @return whether the annoation type is visible or not */    
    public boolean isVisible() {
        Boolean b = (Boolean)getProp(PROP_VISIBLE);
        if (b == null)
            return false;
        return b.booleanValue();
    }

    /** Setter for the Visible property
     * @param vis visibility of the annotation type */    
    public void setVisible(boolean vis) {
        putProp(PROP_VISIBLE, vis ? Boolean.TRUE : Boolean.FALSE);
    }

    /** Setter for the Visible property
     * @param vis visibility of the annotation type */    
    public void setVisible(String vis) {
        putProp(PROP_VISIBLE, Boolean.valueOf(vis));
    }

    /** Getter for WholeLine property
     * @return whether this annotation type is whole line or not  */    
    public boolean isWholeLine() {
        Boolean b = (Boolean)getProp(PROP_WHOLE_LINE);
        if (b == null)
            return true;
        return b.booleanValue();
    }

    /** Setter for the WholeLine property
     * @param wl whether the annotation type is whole line or not */    
    public void setWholeLine(boolean wl) {
        putProp(PROP_WHOLE_LINE, wl ? Boolean.TRUE : Boolean.FALSE);
    }

    /** Setter for the WholeLine property
     * @param wl whether the annotation type is whole line or not */    
    public void setWholeLine(String wl) {
        putProp(PROP_WHOLE_LINE, Boolean.valueOf(wl));
    }

    /** Getter for ContentType property
     * @return  list of content types separated by commas */    
    public String getContentType() {
        return (String)getProp(PROP_CONTENT_TYPE);
    }

    /** Setter for the ContentType property
     * @param ct list of content type separeted by commas */    
    public void setContentType(String ct) {
        putProp(PROP_CONTENT_TYPE, ct);
    }
    
    /** Gets property for appropriate string value */
    public Object getProp(String prop){
        return properties.get(prop);
    }
    
    /** Puts property to Map */
    public void putProp(Object key, Object value){
        if (value == null) {
            properties.remove(key);
            return;
        }
        properties.put(key,value);
    }
    
    public String toString() {
        return "AnnotationType: name='" + getName() + "', description='" + getDescription() + // NOI18N
            "', visible=" + isVisible() + ", wholeline=" + isWholeLine() + // NOI18N
            ", glyph=" + getGlyph() + ", highlight=" + getHighlight() + // NOI18N
            ", foreground=" + getForegroundColor() + // NOI18N
            "', inheritForeground=" + isInheritForegroundColor() + //NOI18N
            ", contenttype="+getContentType(); //NOI18N

    }

    /** Add listeners on changes of annotation type properties
     * @param l  change listener*/
    final public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        support.addPropertyChangeListener (l);
    }
    
    /** Remove listeners on changes of annotation type properties
     * @param l  change listener*/
    final public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        support.removePropertyChangeListener (l);
    }

    /** Fire property change to registered listeners. */
    final protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        support.firePropertyChange(propertyName, oldValue, newValue);
    }


    /** Hepler class describing annotation type and whether all
     * occurences of this type should be absorbed by combination or not.
     * The annonation type which want to combine some other types, must
     * define array of instances of this helper class. See 
     * AnnotationType.PROP_COMBINATIONS property.
     */
    public static final class CombinationMember {

        /** Name of the annotation type */
        private String type;
        
        /** Whether all occurences of this type should be absorbed or not */
        private boolean absorbAll;

        /** Whether this combination member is options or not */
        private boolean optional;
        
        /** Minimum count of this type which must be found on the line to make 
         * valid combination */
        private int minimumCount;

        public CombinationMember(String type, boolean absorbAll, boolean optional, int minimumCount) {
            this.type = type;
            this.absorbAll = absorbAll;
            this.optional = optional;
            this.minimumCount = minimumCount;
        }

        public CombinationMember(String type, boolean absorbAll, boolean optional, String minimumCount) {
            this.type = type;
            this.absorbAll = absorbAll;
            this.optional = optional;
            if (minimumCount != null && minimumCount.length() > 0) {
                try {
                    this.minimumCount = Integer.parseInt(minimumCount);
                } catch (NumberFormatException ex) {
                    if( Boolean.getBoolean( "netbeans.debug.exceptions" ) )
                        ex.printStackTrace();
                    this.minimumCount = 0;
                }
            } else
                this.minimumCount = 0;
        }

        /** Gets name of the annotation type */
        public String getName() {
            return type;
        }

        /** Getter for AbsorbAll property  */
        public boolean isAbsorbAll() {
            return absorbAll;
        }
        
        /** Getter for Optional property  */
        public boolean isOptional() {
            return optional;
        }
        
        /** Getter for MinimumCount property  */
        public int getMinimumCount() {
            return minimumCount;
        }
    }

}
