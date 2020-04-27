package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Util {
    private final static String IMAGE_BASE = "/de/skelton/images/";
    private final static String IMAGE_BASE2 = "/de/skelton/utils/";

    public final static ImageIcon LOGO_ICON = getImage(IMAGE_BASE + "32x32/dot-chart.png");
    public final static ImageIcon BLANK_ICON = getImage(IMAGE_BASE2 + "blank.png");
    public final static ImageIcon QUESTION_ICON = getImage(IMAGE_BASE + "32x32/question.png");
    public final static ImageIcon INFORMATION_ICON = getImage(IMAGE_BASE + "32x32/information.png");
    public final static ImageIcon WARNING_ICON = getImage(IMAGE_BASE + "32x32/warning.png");
    public final static ImageIcon ERROR_ICON = getImage(IMAGE_BASE + "32x32/error.png");
    public final static ImageIcon ERROR_SMALL_ICON = getImage(IMAGE_BASE2 + "error.png");
    public final static ImageIcon CHECK_ICON = getImage(IMAGE_BASE2 + "check2.png");

    public final static ImageIcon UNDO_ICON = getImage(IMAGE_BASE2 + "undo.png");
    public final static ImageIcon REDO_ICON =getImage(IMAGE_BASE2 + "redo.png");
    public final static ImageIcon COPY_ICON = getImage(IMAGE_BASE2 + "copy.png");
    public final static ImageIcon CUT_ICON = getImage(IMAGE_BASE2 + "cut.png");
    public final static ImageIcon PASTE_ICON = getImage(IMAGE_BASE2 + "paste.png");
    public final static ImageIcon NEW_DOCUMENT_ICON = getImage(IMAGE_BASE2 + "document_new.png");
    public final static ImageIcon FIND_ICON = getImage(IMAGE_BASE2 + "find.png");
    public final static ImageIcon REPLACE_ICON = getImage(IMAGE_BASE2 + "replace.png");
    public final static ImageIcon FOLDER_ICON = getImage(IMAGE_BASE2 + "folder.png");
    public final static ImageIcon TEXT_TREE_ICON = getImage(IMAGE_BASE + "text_tree.png");
    public final static ImageIcon SERVER_INFORMATION_ICON = getImage(IMAGE_BASE2 + "server_information.png");
    public final static ImageIcon ADD_SERVER_ICON = getImage(IMAGE_BASE2 + "server_add.png");
    public final static ImageIcon DELETE_SERVER_ICON = getImage(IMAGE_BASE2 + "server_delete.png");
    public final static ImageIcon DISKS_ICON = getImage(IMAGE_BASE2 + "disks.png");
    public final static ImageIcon SAVE_AS_ICON = getImage(IMAGE_BASE2 + "save_as.png");
    public final static ImageIcon EXPORT_ICON = getImage(IMAGE_BASE2 + "export2.png");
    public final static ImageIcon CHART_ICON = getImage(IMAGE_BASE2 + "chart.png");
    public final static ImageIcon STOP_ICON = getImage(IMAGE_BASE2 + "stop.png");
    public final static ImageIcon EXCEL_ICON = getImage(IMAGE_BASE + "excel_icon.gif");
    public final static ImageIcon TABLE_SQL_RUN_ICON = getImage(IMAGE_BASE2 + "table_sql_run.png");
    public final static ImageIcon RUN_ICON = getImage(IMAGE_BASE2 + "element_run.png");
    public final static ImageIcon REFRESH_ICON = getImage(IMAGE_BASE2 + "refresh.png");
    public final static ImageIcon ABOUT_ICON = getImage(IMAGE_BASE2 + "about.png");
    public final static ImageIcon TEXT_ICON = getImage(IMAGE_BASE2 + "text.png");
    public final static ImageIcon TABLE_ICON = getImage(IMAGE_BASE2 + "table.png");
    public final static ImageIcon CONSOLE_ICON = getImage(IMAGE_BASE2 + "console.png");
    public final static ImageIcon DATA_COPY_ICON = getImage(IMAGE_BASE2 + "data_copy.png");
    public final static ImageIcon CHART_BIG_ICON = Util.getImage(IMAGE_BASE2 + "chart_24.png");
    public final static ImageIcon COLUMN_ICON = getImage(IMAGE_BASE2 +"column.png");
    public final static ImageIcon SORT_ASC_ICON = getImage(IMAGE_BASE + "sort_ascending.png");
    public final static ImageIcon SORT_AZ_ASC_ICON = getImage(IMAGE_BASE + "sort_az_ascending.png");
    public final static ImageIcon SORT_DESC_ICON = getImage(IMAGE_BASE + "sort_descending.png");
    public final static ImageIcon SORT_AZ_DESC_ICON = Util.getImage(IMAGE_BASE + "sort_az_descending.png");

    public static ImageIcon getImage(String strFilename) {
        if (!strFilename.startsWith("/")) {
            strFilename = "/toolbarButtonGraphics/" + strFilename;
        }

        URL url = Util.class.getResource(strFilename);
        if (url == null) return null;

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
