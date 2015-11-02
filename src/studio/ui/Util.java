package studio.ui;

import javax.swing.*;
import java.awt.*;

public class Util {
    public static ImageIcon getImage(String strFilename) {
        Class thisClass = Util.class;

        java.net.URL url = null;

        if (strFilename.startsWith("/"))
            url = thisClass.getResource(strFilename);
        else
            url = thisClass.getResource("/toolbarButtonGraphics/" + strFilename);

        if (url == null)
            return null;

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        Image image = toolkit.getImage(url);

        return new ImageIcon(image);
    }

    public static void centerChildOnParent(Component child,Component parent) {
        Point parentlocation = parent.getLocation();
        Dimension oursize = child.getPreferredSize();
        Dimension parentsize = parent.getSize();

        int x = parentlocation.x + (parentsize.width - oursize.width) / 2;
        int y = parentlocation.y + (parentsize.height - oursize.height) / 2;

        x = Math.max(0,x);  // keep the corner on the screen
        y = Math.max(0,y);  //

        child.setLocation(x,y);
    }
}
