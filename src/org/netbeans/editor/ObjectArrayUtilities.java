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

import java.util.Comparator;

/**
 * Utilities over object array.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public class ObjectArrayUtilities {
    
    private ObjectArrayUtilities() {
        // no instances
    }
    
    /**
     * Searches the specified object array for the specified object using the binary
     * search algorithm.  The object array must be sorted into ascending order
     * according to the <i>natural ordering</i> of its items. If it is
     * not sorted, the results are undefined.  If the object array contains multiple
     * elements equal to the specified object, there is no guarantee which one
     * will be found.
     * <BR>This method runs in log(n) time.
     * @param  objectArray object array to be searched.
     * @param  key the key to be searched for.
     * @return index of the search key, if it is contained in the object array;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the object array: the index of the first
     *	       element greater than the key, or <tt>objectArray.getItemCount()</tt>, if all
     *	       elements in the object array are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.
     * @throws ClassCastException if the object array contains elements that are not
     *	       <i>mutually comparable</i> (for example, strings and
     *	       integers), or the search key in not mutually comparable
     *	       with the elements of the object array.
     * @see    Comparable
     */
    public static int binarySearch(ObjectArray objectArray, Object key) {
        int low = 0;
        int high = objectArray.getItemCount() - 1;
        
        while (low <= high) {
            int mid = (low + high) >> 1;
            Object midVal = objectArray.getItem(mid);
            int cmp = ((Comparable)midVal).compareTo(key);
            
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }

        return -(low + 1);  // key not found
    }

    /**
     * Perform binary search with the specified comparator. The more detailed
     * description is given in the doc for {@link #binarySearch(ObjectArray, Object)}.
     * @param objectArray object array to be searched.
     * @param key key to search for.
     * @param c comparator to be used to compare the items.
     * @return index of the search key, if it is contained in the object array;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.
     * @see binarySearch(ObjectArray, Object)
     */
    public static int binarySearch(ObjectArray objectArray, Object key, Comparator c) {
        int low = 0;
        int high = objectArray.getItemCount() - 1;
        
        while (low <= high) {
            int mid = (low + high) >> 1;
            Object midVal = objectArray.getItem(mid);
            int cmp = c.compare(midVal, key);
            
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }

        return -(low + 1);  // key not found
    }
    
    /**
     * Get index of the occurrence of the given item in the object array
     * by using binary search and scanning the adjacent items
     * that are equal copmared to the searched item.
     * @param objectArray object array containing the item.
     * @param item object to be found.
     * @return index of the found object. The object must be the same physical
     *  instance as the search item.
     *  <BR>For multiple occurrences of the item in the object array
     *  a random occurrence is returned.
     *  <BR>If the object is not found in the objectArray then
     *  this method returns -1.
     */
    public static int findIndex(ObjectArray objectArray, Object item) {
        int index = binarySearch(objectArray, item);

        if (index < 0) { // nothing equal to item found
            return -1;
        }
        if (objectArray.getItem(index) == item) {
            return index;
        }

        int cnt = objectArray.getItemCount();
        for (int upIndex = index + 1; upIndex < cnt; upIndex++) {
            Object indexItem = objectArray.getItem(upIndex);
            if (indexItem == item) {
                return upIndex;
            }
            if (((Comparable)item).compareTo(indexItem) != 0) {
                break;
            }
        }

        while (--index >= 0) {
            Object indexItem = objectArray.getItem(index);
            if (indexItem == item) {
                return index;
            }
            if (((Comparable)item).compareTo(indexItem) != 0) {
                break;
            }
        }

        return -1;
    }

    /**
     * Get index of the occurrence of the given item in the object array
     * by using binary search and scanning the adjacent items
     * that are equal copmared to the searched item.
     * @param objectArray object array containing the item.
     * @param item object to be found.
     * @param c comparator to use to compare the items.
     * @return index of the found object. The object must be the same physical
     *  instance as the search item.
     *  <BR>For multiple occurrences of the item in the object array
     *  a random occurrence is returned.
     *  <BR>If the object is not found in the objectArray then
     *  this method returns -1.
     */
    public static int findIndex(ObjectArray objectArray, Object item, Comparator c) {
        int index = binarySearch(objectArray, item, c);

        if (index < 0) { // nothing equal to item found
            return -1;
        }
        if (objectArray.getItem(index) == item) {
            return index;
        }

        int cnt = objectArray.getItemCount();
        for (int upIndex = index + 1; upIndex < cnt; upIndex++) {
            Object indexItem = objectArray.getItem(upIndex);
            if (indexItem == item) {
                return upIndex;
            }
            if (c.compare(item, indexItem) != 0) {
                break;
            }
        }

        while (--index >= 0) {
            Object indexItem = objectArray.getItem(index);
            if (indexItem == item) {
                return index;
            }
            if (c.compare(item, indexItem) != 0) {
                break;
            }
        }

        return -1;
    }

    /**
     * Create array and fill it with all the items from the given objectArray.
     * @param objectArray objectArray containing items to copy.
     * @return array containing all the items from the given objectArray.
     */
    public static Object[] toArray(ObjectArray objectArray) {
        return toArray(objectArray, 0, objectArray.getItemCount());
    }
    
    /**
     * Create array and fill it with the items from the given objectArray.
     * @param objectArray objectArray containing items to copy.
     * @param startIndex index of the first item to copy.
     * @param endIndex index that follows the last item to copy.
     * @return array containing the requested items from the given objectArray.
     */
    public static Object[] toArray(ObjectArray objectArray, int startIndex, int endIndex) {
        Object[] dest = new Object[endIndex - startIndex];
        copyItems(objectArray, startIndex, endIndex, dest, 0);
        return dest;
    }

    /**
     * Copy items from the given object array into destination array.
     * @param srcObjectArray objectArray containing items to copy.
     * @param srcStartIndex index of the first item in the objectArray to copy.
     * @param srcEndIndex index that follows the last item in the objectArray to copy.
     * @param dest array into which the items will be copied.
     * @param destIndex index in the destination array at which the placing
     *  of the copied items will be started.
     */
    public static void copyItems(ObjectArray srcObjectArray,
    int srcStartIndex, int srcEndIndex, Object[] dest, int destIndex) {
     
        if (srcObjectArray instanceof ObjectArray.CopyItems) {
            ((ObjectArray.CopyItems)srcObjectArray).copyItems(
                srcStartIndex, srcEndIndex, dest, destIndex);

        } else {
            while (srcStartIndex < srcEndIndex) {
                dest[destIndex++] = srcObjectArray.getItem(srcStartIndex++);
            }
        }
    }

    /**
     * Utility method to reverse order of the elements in the given array.
     * @param array array to be reversed.
     */
    public static void reverse(Object[] array) {
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            Object o = array[i];
            array[i] = array[j];
            array[j] = o;
        }
    }

}
