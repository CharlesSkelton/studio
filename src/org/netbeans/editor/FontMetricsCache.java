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

import java.awt.*;
import java.awt.font.LineMetrics;
import java.util.HashMap;

/** Static cache that holds the font metrics for the fonts.
* This can generally speed up drawing if the metrics are not cached
* directly by the system.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class FontMetricsCache {

    private static HashMap font2FM = new HashMap();

    private static HashMap font2Info = new HashMap();

    /** Get the font-metrics for the given font.
    * @param font font for which the metrics is being retrieved.
    * @param c component that is used to retrieve the metrics in case it's
    *   not yet in the cache.
    */
    public static synchronized FontMetrics getFontMetrics(Font f, Component c) {
        Object fm = font2FM.get(f);
        if (fm == null) {
            fm = c.getFontMetrics(f);
            font2FM.put(f, fm);
        }
        return (FontMetrics)fm;
    }

    /** Get the font-metrics for the given font.
    * @param font font for which the metrics is being retrieved.
    * @param g graphics that is used to retrieve the metrics in case it's
    *   not yet in the cache.
    */
    public static synchronized FontMetrics getFontMetrics(Font f, Graphics g) {
        Object fm = font2FM.get(f);
        if (fm == null) {
            fm = g.getFontMetrics(f);
            font2FM.put(f, fm);
        }
        return (FontMetrics)fm;
    }

    /** Get the info about the space-width and strike-through and underline
    * constants.
    * @param font font for which the info is being retrieved.
    */
    public static synchronized Info getInfo(Font f) {
        Info info = (Info)font2Info.get(f);
        if (info == null) {
            info = new InfoImpl(f);
            font2Info.put(f, info);
        }
        return info;
    }

    /** Clear all the metrics from the cache. It's usually done
    * when any of the editor ui is being garbage collected to
    * ensure there will be no more unused metrics.
    */
    public static synchronized void clear() {
        font2FM.clear();
        font2Info.clear();
    }

    public interface Info {

        /** Returns the width of the space character */
        public int getSpaceWidth(Graphics g);

        /** Returns the width of the space character */
        public int getSpaceWidth(Component c);

        /** Returns the position of the strike-through line
        * relative to the baseline.
        */
        public float getStrikethroughOffset(Graphics g);

        /** Returns the position of the strike-through line
        * relative to the baseline.
        */
        public float getStrikethroughOffset(Component c);

        /** Returns the thickness of the strike-through line. */
        public float getStrikethroughThickness(Graphics g);

        /** Returns the thickness of the strike-through line. */
        public float getStrikethroughThickness(Component c);

        /** Returns the position of the underline relative to the baseline. */
        public float getUnderlineOffset(Graphics g);

        /** Returns the position of the underline relative to the baseline. */
        public float getUnderlineOffset(Component c);

        /** Returns the thickness of the underline. */
        public float getUnderlineThickness(Graphics g);

        /** Returns the thickness of the underline. */
        public float getUnderlineThickness(Component c);

    }

    private static class InfoImpl implements Info {

        private static final int SW_INITED = 1;
        private static final int ST_INITED = 2;
        private static final int UL_INITED = 4;

        private Font font;

        private int inited;

        private int spaceWidth;

        private float strikethroughOffset;

        private float strikethroughThickness;

        private float underlineOffset;

        private float underlineThickness;

        InfoImpl(Font font) {
            this.font = font;
        }

        private synchronized void initSpaceWidth(Graphics g, Component c) {
            FontMetrics fm = (g != null) ? getFontMetrics(font, g) : getFontMetrics(font, c);
            spaceWidth = fm.stringWidth(" ");
            if (spaceWidth <= 0) {
                spaceWidth = fm.stringWidth("A") / 3;
            }
            inited |= SW_INITED;
        }

        private synchronized void initStrikethrough(Graphics g) {
            LineMetrics lm = font.getLineMetrics("aAyY", ((Graphics2D)g).getFontRenderContext());
            strikethroughOffset = lm.getStrikethroughOffset();
            strikethroughThickness = lm.getStrikethroughThickness();
            inited |= ST_INITED;
        }

        private synchronized void initUnderline(Graphics g) {
            LineMetrics lm = font.getLineMetrics("aAyY", ((Graphics2D)g).getFontRenderContext());
            underlineOffset = lm.getUnderlineOffset();
            underlineThickness = lm.getUnderlineThickness();
            inited |= UL_INITED;
        }

        /** Returns the width of the space character */
        public int getSpaceWidth(Graphics g) {
            if ((inited & SW_INITED) == 0) {
                initSpaceWidth(g, null);
            }

            return spaceWidth;
        }

        /** Returns the width of the space character */
        public int getSpaceWidth(Component c) {
            if ((inited & SW_INITED) == 0) {
                initSpaceWidth(null, c);
            }

            return spaceWidth;
        }

        /** Returns the position of the strike-through line
        * relative to the baseline.
        */
        public float getStrikethroughOffset(Graphics g) {
            if ((inited & ST_INITED) == 0) {
                initStrikethrough(g);
            }

            return strikethroughOffset;
        }

        /** Returns the position of the strike-through line
        * relative to the baseline.
        */
        public float getStrikethroughOffset(Component c) {
            if ((inited & ST_INITED) == 0) {
                initStrikethrough(c.getGraphics());
            }

            return strikethroughOffset;
        }

        /** Returns the thickness of the strike-through line. */
        public float getStrikethroughThickness(Graphics g) {
            if ((inited & ST_INITED) == 0) {
                initStrikethrough(g);
            }

            return strikethroughThickness;
        }

        /** Returns the thickness of the strike-through line. */
        public float getStrikethroughThickness(Component c) {
            if ((inited & ST_INITED) == 0) {
                initStrikethrough(c.getGraphics());
            }

            return strikethroughThickness;
        }

        /** Returns the position of the underline relative to the baseline. */
        public float getUnderlineOffset(Graphics g) {
            if ((inited & UL_INITED) == 0) {
                initUnderline(g);
            }

            return underlineOffset;
        }

        /** Returns the position of the underline relative to the baseline. */
        public float getUnderlineOffset(Component c) {
            if ((inited & UL_INITED) == 0) {
                initUnderline(c.getGraphics());
            }

            return underlineOffset;
        }

        /** Returns the thickness of the underline. */
        public float getUnderlineThickness(Graphics g) {
            if ((inited & UL_INITED) == 0) {
                initUnderline(g);
            }

            return underlineThickness;
        }

        /** Returns the thickness of the underline. */
        public float getUnderlineThickness(Component c) {
            if ((inited & UL_INITED) == 0) {
                initUnderline(c.getGraphics());
            }

            return underlineThickness;
        }

    }


}
