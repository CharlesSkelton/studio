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

package org.openide.text;

import java.util.ResourceBundle;
import java.awt.Font;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.openide.options.ContextSystemOption;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/** Settings for output window.
*
* @author Ales Novak
*/
public final class PrintSettings extends ContextSystemOption {
    // final because overrides Externalizable methods

    /** serialVersionUID */
    static final long serialVersionUID = -9102470021814206818L;

    /** Constant for center position of page header. */
    public static final int CENTER = 0x1;
    /** Constant for right position of page header. */
    public static final int RIGHT = 0x2;
    /** Constant for left position of page header. */
    public static final int LEFT = 0x0;

    /** Property name of the wrap property */
    public static final String PROP_PAGE_FORMAT = "pageFormat"; // NOI18N
    /** Property name of the wrap property */
    public static final String PROP_WRAP = "wrap"; // NOI18N
    /** Property name of the header format  property */
    public static final String PROP_HEADER_FORMAT = "headerFormat"; // NOI18N
    /** Property name of the footer format property */
    public static final String PROP_FOOTER_FORMAT = "footerFormat"; // NOI18N
    /** Property name of the header font property */
    public static final String PROP_HEADER_FONT = "headerFont"; // NOI18N
    /** Property name of the footer font property */
    public static final String PROP_FOOTER_FONT = "footerFont"; // NOI18N
    /** Property name of the header alignment property */
    public static final String PROP_HEADER_ALIGNMENT = "headerAlignment"; // NOI18N
    /** Property name of the footer alignment property */
    public static final String PROP_FOOTER_ALIGNMENT = "footerAlignment"; // NOI18N
    /** Property name of the line ascent correction property */
    public static final String PROP_LINE_ASCENT_CORRECTION = "lineAscentCorrection"; // NOI18N

    private static final String HELP_ID = "editing.printing"; // !!! NOI18N
    
    /** PageFormat */
    private static PageFormat pageFormat;
    /** Wrap lines? */
    private static boolean wrap = true;
    /** Header format - see MessageFormat */
    private static String headerFormat;
    /** Footer format - see MessageFormat */
    private static String footerFormat;
    /** Header font */
    private static Font headerFont;
    /** Footer font */
    private static Font footerFont;
    /** Header alignment */
    private static int headerAlignment = CENTER;
    /** Footer alignment */
    private static int footerAlignment = CENTER;
    /** Line ascent correction parameter. Ranged from 0 to infinity.
    * Actually this parameter should be 1 but in that case there are
    * big spaces between lines.
    */
    private static float lineAscentCorrection = 0.7f;

    /** Externalizes this SystemOption
    * @param obtos
    * @exception IOException
    */
    public void writeExternal(ObjectOutput obtos) throws IOException {
        super.writeExternal(obtos);
        obtos.writeBoolean(wrap);
        obtos.writeObject(headerFormat);
        obtos.writeObject(footerFormat);
        obtos.writeObject(headerFont);
        obtos.writeObject(footerFont);
        obtos.writeInt(headerAlignment);
        obtos.writeInt(footerAlignment);
        externalizePageFormat(pageFormat, obtos);
    }

    /** Deexetrnalizes this SystemOption
    * @param obtis
    * @exception IOException
    * @exception ClassNotFoundException
    */
    public void readExternal(ObjectInput obtis) throws IOException, ClassNotFoundException {
        super.readExternal(obtis);
        wrap = obtis.readBoolean();
        headerFormat = (String) obtis.readObject();
        footerFormat = (String) obtis.readObject();
        headerFont = (Font) obtis.readObject();
        footerFont = (Font) obtis.readObject();
        headerAlignment = obtis.readInt();
        footerAlignment = obtis.readInt();
        pageFormat = internalizePageFormat(obtis);
    }

    /** Writes a PageFormat instance into ObjectOutput
    * @param pf
    * @param obtos
    */
    private static void externalizePageFormat(PageFormat pf, ObjectOutput obtos) throws IOException {
        if (pf == null) {
            obtos.writeInt(PageFormat.LANDSCAPE ^ PageFormat.REVERSE_LANDSCAPE ^ PageFormat.PORTRAIT);
            return;
        }
        obtos.writeInt(pf.getOrientation());
        Paper paper = pf.getPaper();
        // paper size
        obtos.writeDouble(paper.getWidth());
        obtos.writeDouble(paper.getHeight());
        // paper imageable area
        obtos.writeDouble(paper.getImageableX());
        obtos.writeDouble(paper.getImageableY());
        obtos.writeDouble(paper.getImageableWidth());
        obtos.writeDouble(paper.getImageableHeight());
    }

    /** Reads a PageFormat instance from ObjectInput
    * @param obtis
    * @return deserialized PageFormat instance
    */
    private static PageFormat internalizePageFormat(ObjectInput obtis)
    throws IOException, ClassNotFoundException {
        PageFormat pf = new PageFormat();
        Paper paper = pf.getPaper();
        int etc = obtis.readInt();
        if (etc == (PageFormat.LANDSCAPE ^ PageFormat.REVERSE_LANDSCAPE ^ PageFormat.PORTRAIT)) {
            return null;
        }
        pf.setOrientation(etc);
        // paper size
        paper.setSize(obtis.readDouble(), obtis.readDouble());
        // imageable
        paper.setImageableArea(obtis.readDouble(),
                               obtis.readDouble(),
                               obtis.readDouble(),
                               obtis.readDouble());
        pf.setPaper(paper);
        return pf;
    }

    public String displayName () {
        return NbBundle.getMessage(PrintSettings.class, "CTL_Print_settings");
    }

    public HelpCtx getHelpCtx () {
        return new HelpCtx (HELP_ID);
    }

    /** @return an instance of PageFormat
    * The returned page format is either previously set by
    * PageSetupAction or is acquired as a default PageFormat
    * from supported PrinterJob
    */
    public static PageFormat getPageFormat(PrinterJob pj) {
        if (pageFormat == null) {
            pageFormat = pj.defaultPage();
        }
        return pageFormat;
    }
    /** @deprecated Use {@link #getPageFormat(PrinterJob)} instead. */
    public PageFormat getPageFormat() {
        if (pageFormat == null) {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pageFormat = pj.defaultPage(new PageFormat());
            pj.cancel();
        }
        return pageFormat;
    }

    /** sets page format */
    public void setPageFormat(PageFormat pf) {
        if (pf == null) {
            return;
        }
        if (pf.equals(pageFormat)) {
            return;
        }
        PageFormat old = pageFormat;
        pageFormat = pf;
        firePropertyChange(PROP_PAGE_FORMAT, old, pageFormat);
    }

    public boolean getWrap() {
        return wrap;
    }
    public void setWrap(boolean b) {
        if (wrap == b) {
            return;
        }
        wrap = b;
        firePropertyChange(PROP_WRAP,
                           (b ? Boolean.FALSE : Boolean.TRUE),
                           (b ? Boolean.TRUE : Boolean.FALSE));
    }

    public String getHeaderFormat() {
        if (headerFormat == null) {
            headerFormat = NbBundle.getMessage(PrintSettings.class, "CTL_Header_format");
        }
        return headerFormat;
    }
    public void setHeaderFormat(String s) {
        if (s == null) {
            return;
        }
        if (s.equals(headerFormat)) {
            return;
        }
        String of = headerFormat;
        headerFormat = s;
        firePropertyChange(PROP_HEADER_FORMAT, of, headerFormat);
    }

    public String getFooterFormat() {
        if (footerFormat == null) {
            footerFormat = NbBundle.getMessage(PrintSettings.class, "CTL_Footer_format");
        }
        return footerFormat;
    }
    public void setFooterFormat(String s) {
        if (s == null) {
            return;
        }
        if (s.equals(footerFormat)) {
            return;
        }
        String of = footerFormat;
        footerFormat = s;
        firePropertyChange(PROP_FOOTER_FORMAT, of, footerFormat);
    }

    public Font getHeaderFont() {
        if (headerFont == null) {
            headerFont = new Font("Monospaced", java.awt.Font.PLAIN, 6); // NOI18N
        }
        return headerFont;
    }
    public void setHeaderFont(Font f) {
        if (f == null) {
            return;
        }
        if (f.equals(headerFont)) {
            return;
        }
        Font old = headerFont;
        headerFont = f;
        firePropertyChange(PROP_HEADER_FONT, old, headerFont);
    }

    public Font getFooterFont() {
        if (footerFont == null) {
            footerFont = getHeaderFont();
        }
        return footerFont;
    }
    public void setFooterFont(Font f) {
        if (f == null) {
            return;
        }
        if (f.equals(footerFont)) {
            return;
        }
        Font old = headerFont;
        footerFont = f;
        firePropertyChange(PROP_FOOTER_FONT, old, footerFont);
    }

    public int getHeaderAlignment() {
        return headerAlignment;
    }
    public void setHeaderAlignment(int alignment) {
        if (alignment == headerAlignment) {
            return;
        }
        if ((alignment != LEFT) &&
                (alignment != CENTER) &&
                (alignment != RIGHT)) {
            throw new IllegalArgumentException();
        }
        int old = headerAlignment;
        headerAlignment = alignment;
        firePropertyChange(PROP_HEADER_ALIGNMENT, new Integer(old), new Integer(headerAlignment));
    }

    public int getFooterAlignment() {
        return footerAlignment;
    }
    public void setFooterAlignment(int alignment) {
        if (alignment == footerAlignment) {
            return;
        }
        if ((alignment != LEFT) &&
                (alignment != CENTER) &&
                (alignment != RIGHT)) {
            throw new IllegalArgumentException();
        }
        int old = footerAlignment;
        footerAlignment = alignment;
        firePropertyChange(PROP_FOOTER_ALIGNMENT, new Integer(old), new Integer(footerAlignment));
    }

    /** Getter for lineAscentCorrection property. */
    public float getLineAscentCorrection() {
        return lineAscentCorrection;
    }
    /** Setter for lineAscentCorrection property.
    * @param correction the correction
    * @exception IllegalArgumentException if <tt>correction</tt> is less than 0.
    */
    public void setLineAscentCorrection(float correction) {
        if (correction == lineAscentCorrection) {
            return;
        } else if (correction < 0) {
            throw new IllegalArgumentException();
        }
        float old = lineAscentCorrection;
        lineAscentCorrection = correction;
        firePropertyChange(PROP_LINE_ASCENT_CORRECTION, new Float(old), new Float(lineAscentCorrection));
    }

    /** Property editor for alignment properties */
    public static class AlignmentEditor extends java.beans.PropertyEditorSupport {

        private String sCENTER, sRIGHT, sLEFT;
        private String[] tags = new String[] {
            sLEFT = NbBundle.getMessage(PrintSettings.class, "CTL_LEFT"),
            sCENTER = NbBundle.getMessage(PrintSettings.class, "CTL_CENTER"),
            sRIGHT = NbBundle.getMessage(PrintSettings.class, "CTL_RIGHT")};

        public String[] getTags() {
            return tags;
        }

        public String getAsText() {
            return tags[((Integer) getValue()).intValue()];
        }
        public void setAsText(String s) {
            if (s.equals(sLEFT)) {
                setValue(new Integer(0));
            } else if (s.equals(sCENTER)) {
                setValue(new Integer(1));
            } else {
                setValue(new Integer(2));
            }
        }
    }

    /** Property editor for PageFormat instances */
    public static class PageFormatEditor extends java.beans.PropertyEditorSupport {


        /** No text */
        public String getAsText() {
            return null;
        }

        /* @return <tt>true</tt> */
        public boolean supportsCustomEditor() {
            return true;
        }

        /**
        * @return <tt>null</tt> Shows pageDialog, however.
        */
        public java.awt.Component getCustomEditor() {
            PageFormat pf = (PageFormat) getValue();
            PrinterJob pj = PrinterJob.getPrinterJob();
            PageFormat npf = pj.pageDialog(pf);
            //setValue(npf);
            ((PrintSettings) PrintSettings.findObject(PrintSettings.class)).setPageFormat((PageFormat) npf.clone());
            pj.cancel();
            return null;
        }
    }
}
