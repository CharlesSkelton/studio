package studio.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Vector;
import javax.swing.Icon;

public class ScaledIcon implements Icon {
    private Vector icons = new Vector();
    private double factor;

    public void add(Icon icon) {
        icons.add(icon);
    }

    public ScaledIcon(Icon icon,int targetHeight) {
        this.factor = ((double) targetHeight) / (double) icon.getIconHeight();
        icons.add(icon);
    }

    public int getIconWidth() {
        int w = 0;
        for (int i = 0;i < icons.size();i++)
            w += ((Icon) icons.elementAt(i)).getIconWidth();
        w *= factor;
        return (int) (w + 5);
    }

    public int getIconHeight() {
        Icon icon = (Icon) icons.elementAt(0);
        return (int) (icon.getIconHeight() * factor);
    }

    public void paintIcon(Component c,Graphics g,int x,int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(x,y);
        g2d.scale(factor,factor);
        int xoff = 0;
        for (int i = 0;i < icons.size();i++) {
            Icon icon = ((Icon) icons.elementAt(i));
            icon.paintIcon(c,g2d,xoff,0);
            xoff += icon.getIconWidth() + 5;
        }
        g2d.dispose();
    }
}

