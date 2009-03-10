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

package org.netbeans.editor;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.List;

/**
 * SegmentCache caches {@link javax.swing.text.Segment} instances
 * to allow reusing of them.
 * <pre>
 *   Segment segment = segmentCache.getSegment();
 *   try {
 *       ... // use the segment
 *   } finally {
 *      segmentCache.releaseSegment(segment);
 *   }
 * </pre>
 *
 * <BR>There is one shared instance of the segment cache
 * provided by the {@link #getSharedInstance()}.
 * <BR>As the whole mechanism of obtaining/freeing of the segments
 * depends on the proper behavior of the clients
 * it's recommended that the individual SegmentCache instances
 * should be used and be e.g. package-private.
 */
public class SegmentCache {
    
    private static final SegmentCache SHARED = new SegmentCache();
    
    /**
     * @return shared cache instance.
     */
    public static SegmentCache getSharedInstance() {
        return SHARED;
    }

    /** Free segments. */
    private List freeSegments;
    
    /**
     * Constructs SegmentCache instance.
     */
    public SegmentCache() {
        freeSegments = new ArrayList();
    }
    
    /**
     * Returns a free {@link javax.swing.text.Segment}. When done, the segment
     * should be recycled by invoking {@link #releaseSegment()}.
     */
    public synchronized Segment getSegment() {
        int size = freeSegments.size();
        return (size > 0)
            ? (Segment)freeSegments.remove(size - 1)
            : new CachedSegment();
    }
    
    /**
     * Releases a shared Segment.
     * <BR>The shared segment must NOT be used after it's released.
     * <BR>The shared segment must NOT be released more than once like this:
     * <pre>
     *   segmentCache.releaseSegment(segment);
     *   segmentCache.releaseSegment(segment);
     * </pre>
     * <BR>Only the segments obtained from {@link #getSegment()}
     * can be released.
     * @param segment segment to be released.
     */
    public synchronized void releaseSegment(Segment segment) {
        if (segment instanceof CachedSegment) {
            segment.array = null;
            segment.count = 0;
            freeSegments.add(segment);

        } else { // not instance obtained by getSegment()
            throw new IllegalStateException(
                segment + " was not obtained from segment cache");
        }
    }
    
    
    /**
     * CachedSegment is used as a tagging class to determine if
     * a Segment being released was returned by {@link #getSegment()}
     */
    private static class CachedSegment extends Segment {
    }

}
