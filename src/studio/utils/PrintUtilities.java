/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/
package studio.utils;

import java.awt.*;
import javax.swing.*;
import java.awt.print.*;

public class PrintUtilities implements Printable {
    private Component componentToBePrinted;
    
    public static void printComponent(Component c) {
        new PrintUtilities(c).print();
    }
    
    public PrintUtilities(Component componentToBePrinted) {
        this.componentToBePrinted = componentToBePrinted;
    }
    
    public void print() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);
        if(printJob.printDialog())
            try {
                printJob.print();
            } catch (PrinterException pe) {
                System.err.println("Printing error: " + pe);
        }
    }
    
    public int print(Graphics g, PageFormat pf, int pageIndex) {
        int response = NO_SUCH_PAGE;
        
        Graphics2D g2 = (Graphics2D) g;
        disableDoubleBuffering(componentToBePrinted);        
        Dimension d = componentToBePrinted.getSize();
        double panelWidth  = d.width; 
        double panelHeight = d.height;
        double pageHeight = pf.getImageableHeight();
        double pageWidth  = pf.getImageableWidth();
        double scale = pageWidth / panelWidth;
        int totalNumPages = (int) Math.ceil(scale * panelHeight / pageHeight);
        if (pageIndex >= totalNumPages) {
            response = NO_SUCH_PAGE;
        } else {            
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.translate(0f, -pageIndex * pageHeight);
            g2.scale(scale, scale);
            componentToBePrinted.paint(g2);
            enableDoubleBuffering(componentToBePrinted);
            response = Printable.PAGE_EXISTS;
        }
        return response;
    }
    
    public static void disableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(false);
    }
    
    public static void enableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(true);
    }
}