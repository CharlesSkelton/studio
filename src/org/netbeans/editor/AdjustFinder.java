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

/**
* Advanced finder that can adjust the start and limit position
* of the search. The finder can be used in the <tt>BaseDocument.find()</tt>
* which calls its adjust-methods automatically.
* The order of the methods called for the search is
* <br>
* 1. <tt>adjustStartPos()</tt> is called<br>
* 2. <tt>adjustStartPos()</tt> is called<br>
* 3. <tt>reset()</tt> is called<br>
* If the search is void i.e. <tt>doc.find(finder, pos, pos)</tt>
* is called, no adjust-methods are called, only the <tt>reset()</tt>
* is called.
* For backward search the start-position is higher than the limit-position.
* The relation <tt>startPos &lt; endPos</tt> defines whether the search
* will be forward or backward. The adjust-methods could in fact
* revert this relation turning the forward search into the backward one
* and vice versa. This is not allowed. If that happens the search
* is considered void.
* The adjust-methods must NOT use the shortcut -1 for the end of document.
*
* @author Miloslav Metelka
* @version 1.00
*/

public interface AdjustFinder extends Finder {

    /** Adjust start position of the search to be either the same or lower.
    * This method can be used
    * for example to scan the whole line by the reg-exp finder even 
    * if the original start position is not at the begining of the line.
    * Although it's not specifically checked the finder should NOT in any case
    * return the position that is lower than the original 
    * @param doc document to search on
    * @param startPos start position originally requested in <tt>BaseDocument.find()</tt>.
    * @return possibly modified start position. The returned position must be
    *   the same or lower than the original start position for forward search
    *   and the same or high.
    */
    public int adjustStartPos(BaseDocument doc, int startPos);

    /** Adjust the limit position of the search
    * (it's the position where the search will end) to be either the same or greater.
    * @param doc document to search on
    * @param limitPos limit position originally requested in <tt>BaseDocument.find()</tt>
    * @return possibly modified limit position. The returned position must be
    *   the same or greater than the original limit position.
    */
    public int adjustLimitPos(BaseDocument doc, int limitPos);

}
