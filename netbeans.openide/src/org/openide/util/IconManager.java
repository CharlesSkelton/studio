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

import java.awt.Image;
import java.awt.Toolkit;
import java.lang.ref.*;
import java.util.*;

import org.openide.ErrorManager;

/** Registers all loaded images into the AbstractNode, so nothing is loaded twice.
*
* @author Jaroslav Tulach
*/
final class IconManager extends Object {
    /** a value that indicates that the icon does not exists */
    private static final Object NO_ICON = new Object();

    /** map of resource name to loaded icon (String, SoftRefrence (Image)) or (String, NO_ICON) */
    private static final HashMap map = new HashMap ();

    private static final HashMap reverseMap = new HashMap();
    
    /** Resource paths for which we have had to strip initial slash.
     * @see "#20072"
     */
    private static final Set extraInitialSlashes = new HashSet(200); // Set<String>
    
    /**
     * Get the class loader from lookup.
     * Since this is done very frequently, it is wasteful to query lookup each time.
     * Instead, remember the last result and just listen for changes.
     */
    private static ClassLoader getLoader() {
        if (currentLoader == null) {
            if (loaderQuery == null) {
                loaderQuery = Lookup.getDefault().lookup(new Lookup.Template(ClassLoader.class));
                loaderQuery.addLookupListener(new LookupListener() {
                    public void resultChanged(LookupEvent ev) {
                        currentLoader = null;
                    }
                });
            }
            Iterator it = loaderQuery.allInstances().iterator();
            if (it.hasNext()) {
                currentLoader = (ClassLoader)it.next();
            } else if (!noLoaderWarned) {
                noLoaderWarned = true;
                ErrorManager.getDefault().log(ErrorManager.WARNING, "No ClassLoader instance found in " + Lookup.getDefault());
            }
        }
        return currentLoader;
    }
    private static ClassLoader currentLoader = null;
    private static Lookup.Result loaderQuery = null;
    private static boolean noLoaderWarned = false;

    static Image getIcon (String name) {
        Object img = map.get (name);

        // no icon for this name (already tested)
        if (img == NO_ICON) return null;

        if (img != null) {
            // then it is SoftRefrence
            img = ((Reference)img).get ();
        }

        // icon found
        if (img != null) return (Image)img;

        ClassLoader loader = getLoader();
        if (loader == null) return null;
        return getIcon(name, loader);
    }
    
    /** Finds imager for given resource.
    * @param name name of the resource
    * @param loader classloader to use for locating it
    */
    static Image getIcon (String name, ClassLoader loader) {
        Object img = map.get (name);

        // no icon for this name (already tested)
        if (img == NO_ICON) return null;

        if (img != null) {
            // then it is SoftRefrence
            img = ((Reference)img).get ();
        }

        // icon found
        if (img != null) return (Image)img;
        
        synchronized (map) {
            // again under the lock
            img = map.get (name);

    	    // no icon for this name (already tested)
    	    if (img == NO_ICON) return null;

            if (img != null) {
                // then it is SoftRefrence
                img = ((Reference)img).get ();
            }
            
            if (img != null)
                // cannot be NO_ICON, since it never disappears from the map.
                return (Image)img;

            // path for bug in classloader
            String n;
            boolean warn;
            if (name.startsWith ("/")) { // NOI18N
                warn = true;
                n = name.substring (1);
            } else {
                warn = false;
                n = name;
            }

            // we have to load it
            java.net.URL url = loader != null ?
                               loader.getResource (n) : IconManager.class.getClassLoader ().getResource (n);

            img = url == null ? null : Toolkit.getDefaultToolkit ().createImage (url);
            if (img != null) {
                if (warn && extraInitialSlashes.add(name)) {
                    ErrorManager.getDefault().log(ErrorManager.WARNING, "Initial slashes in Utilities.loadImage deprecated (cf. #20072): " + name); // NOI18N
                }
                Image img2 = toBufferedImage((Image) img);
                //System.err.println("loading icon " + n + " = " + img2);
                
                Reference r = new ActiveRef (img2);
                map.put (name, r);
                return (Image) img2;
            } else {
                // no icon found
                map.put (name, NO_ICON);
                return null;
            }
        }
    }

    /**
     * Key used for composite images -- it holds image identities
     */    
    private static class CompositeImageKey {
        Image baseImage, overlayImage;
        int x, y;
        
        CompositeImageKey(Image base, Image overlay, int x, int y) {
            this.x = x;
            this.y = y;
            this.baseImage = base;
            this.overlayImage = overlay;
        }
        
        public boolean equals(Object other) {
            if (!(other instanceof CompositeImageKey))
                return false;
            CompositeImageKey k = (CompositeImageKey)other;
            return (x == k.x) && (y == k.y) && (baseImage == k.baseImage) &&
                (overlayImage == k.overlayImage);
        }
        
        public int hashCode() {
            int hash =  ((x << 3) ^ y) << 4;
            hash = hash ^ baseImage.hashCode() ^ overlayImage.hashCode();
            return hash;
        }
        
        public String toString() {
            return "Composite key for " + baseImage + " + " + overlayImage + " at [" + x + ", " + y + "]"; // NOI18N
        }
    }

    /**
     * Method that attempts to find the merged image in the cache first, then
     * creates the image if it was not found.
     */
    static final Image mergeImages(Image im1, Image im2, int x, int y) {
        CompositeImageKey k = new CompositeImageKey(im1, im2, x, y);
        Image cached;
        
        synchronized (map) {
            Reference r = (Reference)map.get(k);
            if (r != null) {
                cached = (Image)r.get();
                if (cached != null)
                    return cached;
            }
            cached = doMergeImages(im1, im2, x, y);
            r = new ActiveRef (cached);
            map.put(k, r);
            reverseMap.put(r, k);
            return cached;
        }
    }

    /** The method creates a BufferedImage which represents the same Image as the
     * parameter but consumes less memory.
     */
    static final Image toBufferedImage(Image img) {
        // load the image
        new javax.swing.ImageIcon(img);
        java.awt.image.BufferedImage rep = createBufferedImage(img.getWidth(null), img.getHeight(null));
        java.awt.Graphics g = rep.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        img.flush();
        return rep;
    }

    private static final Image doMergeImages (Image image1, Image image2, int x, int y) {
        int w = 0, h = 0;
        //System.err.println("merging images " + image1 + " + " + image2 + " at [" + x + ", " + y + "]");
        if (image1 != null) {
            w = image1.getWidth(null);
            h = image1.getHeight(null);
        }
        if (image2 != null) {
            w = image2.getWidth(null)+x > w ? image2.getWidth(null)+x : w;
            h = image2.getHeight(null)+y > h ? image2.getHeight(null)+y : h;
        }
        if (w < 1) w = 16;
        if (h < 1) h = 16;
        
        java.awt.image.ColorModel model = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment ().
                                          getDefaultScreenDevice ().getDefaultConfiguration ().
                                          getColorModel (java.awt.Transparency.BITMASK);
        java.awt.image.BufferedImage buffImage = new java.awt.image.BufferedImage (model,
             model.createCompatibleWritableRaster (w, h), model.isAlphaPremultiplied (), null);
        
        java.awt.Graphics g = buffImage.createGraphics ();
        if (image1 != null) g.drawImage (image1, 0, 0, null);
        if (image2 != null) g.drawImage (image2, x, y, null);
        g.dispose();
        
        return buffImage;
    }
    
    /** Creates BufferedImage 16x16 and Transparency.BITMASK */
    static final java.awt.image.BufferedImage createBufferedImage(int width, int height) {
        java.awt.image.ColorModel model = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().
                                          getDefaultScreenDevice().getDefaultConfiguration().getColorModel(java.awt.Transparency.BITMASK);
        java.awt.image.BufferedImage buffImage = new java.awt.image.BufferedImage(model,
                model.createCompatibleWritableRaster(width, height), model.isAlphaPremultiplied(), null);
        return buffImage;
    }
    
    /** Cleaning reference. */
    private static final class ActiveRef extends SoftReference implements Runnable {
        public ActiveRef (Object o) {
            super (o, Utilities.activeReferenceQueue());
        }

        public void run () {
            Object k;
            synchronized (map) {
                k = reverseMap.remove(this);
                map.remove(k);
            }
        }
    } // end of ActiveRef
}
