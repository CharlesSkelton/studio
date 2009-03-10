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

/** This interface provides methods for
* getting and setting various drawing attributes.
* During painting draw layer receives draw context
* and it is expected to either leave draw parameters
* as they are or change them.
*
* @author Miloslav Metelka
* @version 1.00
*/


public interface DrawContext {

    /** Get current foreground color */
    public Color getForeColor();

    /** Set current foreground color */
    public void setForeColor(Color foreColor);

    /** Get current background color */
    public Color getBackColor();

    /** Set current background color */
    public void setBackColor(Color backColor);

    /** Get current underline color */
    public Color getUnderlineColor();

    /** Set current underline color */
    public void setUnderlineColor(Color underlineColor);

    /** Get current wave underline color */
    public Color getWaveUnderlineColor();

    /** Set current wave underline color */
    public void setWaveUnderlineColor(Color waveUnderlineColor);

    /** Get current strike-through color */
    public Color getStrikeThroughColor();

    /** Set current underline color */
    public void setStrikeThroughColor(Color strikeThroughColor);

    /** Get current font */
    public Font getFont();

    /** Set current font */
    public void setFont(Font font);

    /** Get start position of the drawing. This value
    * stays unchanged during the line-number drawing.
    */
    public int getStartOffset();

    /** Get end position of the drawing. This value
    * stays unchanged during the line-number drawing.
    */
    public int getEndOffset();

    /** Is current drawing position at the begining of the line?
    * This flag is undefined for the line-number drawing.
    */
    public boolean isBOL();

    /** Is current drawing position at the end of the line
    * This flag is undefined for the line-number drawing.
    */
    public boolean isEOL();

    /** Get draw info for the component that is currently drawn. */
    public EditorUI getEditorUI();

    /** Get token type number according to the appropriate
    * syntax scanner */
    public TokenID getTokenID();

    /** Get the token-context-path for the token */
    public TokenContextPath getTokenContextPath();

    /** Get starting position in the document of the token being drawn */
    public int getTokenOffset();

    /** Get length of the token text */
    public int getTokenLength();

    /** Get the starting position in the document of the
     * fragment of the token being drawn.
     */
    public int getFragmentOffset();

    /** Get the length of the fragment of the token
     * being drawn
     */
    public int getFragmentLength();

    /** Get the buffer with the characters being drawn. No changes can
    * be done to characters in the buffer.
    */
    public char[] getBuffer();

    /** Get the position in the document where the buffer starts.
     * The area between <tt>getDrawStartOffset()</tt> and <tt>getDrawEndOffset</tt>
     * will contain valid characters. However the first token
     * can start even under <tt>getDrawStartOffset()</tt>. In this case
     * the valid area starts at <tt>getTokenOffset()</tt> of the first
     * token.
     */
    public int getBufferStartOffset();

}
