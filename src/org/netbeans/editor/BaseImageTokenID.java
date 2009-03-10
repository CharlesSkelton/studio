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

/**
* Token-id with the fixed token image. The image text is provided
* in constructor and can be retrieved by <tt>getImage()</tt>.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class BaseImageTokenID extends BaseTokenID implements ImageTokenID {

    private final String image;

    /** Construct new imag-token-id if the name is the same as the image. */
    public BaseImageTokenID(String nameAndImage) {
        this(nameAndImage, nameAndImage);
    }

    public BaseImageTokenID(String name, String image) {
        super(name);
        this.image = image;
    }

    public BaseImageTokenID(String nameAndImage, int numericID) {
        this(nameAndImage, numericID, nameAndImage);
    }

    public BaseImageTokenID(String name, int numericID, String image) {
        super(name, numericID);
        this.image = image;
    }

    public BaseImageTokenID(String nameAndImage, TokenCategory category) {
        this(nameAndImage, category, nameAndImage);
    }

    public BaseImageTokenID(String name, TokenCategory category, String image) {
        super(name, category);
        this.image = image;
    }

    public BaseImageTokenID(String nameAndImage, int numericID, TokenCategory category) {
        this(nameAndImage, numericID, category, nameAndImage);
    }

    public BaseImageTokenID(String name, int numericID, TokenCategory category, String image) {
        super(name, numericID, category);
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public String toString() {
        return super.toString() + ", image='" + getImage() + "'";
    }

}
