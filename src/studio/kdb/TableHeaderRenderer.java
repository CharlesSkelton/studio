/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.kdb;

import studio.ui.BlankIcon;
import studio.ui.ScaledIcon;
import studio.ui.Util;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class TableHeaderRenderer extends DefaultTableCellRenderer {
    public TableHeaderRenderer() {
        super();
        setHorizontalAlignment(SwingConstants.RIGHT);
        setVerticalAlignment(SwingConstants.CENTER);
        setOpaque(true);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setFont(UIManager.getFont("TableHeader.font"));
        //setFont(table.getFont());
        setBackground(UIManager.getColor("TableHeader.background"));
        setForeground(UIManager.getColor("TableHeader.foreground"));
    }

    public void setFont(Font f) {
        super.setFont(f);
        invalidate();
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        // setFont(table.getFont());

        if (table.getModel() instanceof KTableModel) {
            column = table.convertColumnIndexToModel(column);
            Icon icon = null;

            Insets insets = getBorder().getBorderInsets(this);
            int targetHeight = getFontMetrics(getFont()).getHeight() - insets.bottom - insets.top;
            KTableModel ktm = (KTableModel) table.getModel();
            if (ktm.isSortedDesc()) {
                if (column == ktm.getSortByColumn())
                    if (ktm.getColumnClass(column) == K.KSymbolVector.class)
                        icon = new ScaledIcon(Util.getImage(Config.imageBase + "sort_az_ascending.png"),targetHeight);
                    else
                        icon = new ScaledIcon(Util.getImage(Config.imageBase + "sort_descending.png"),targetHeight);
            }
            else if (ktm.isSortedAsc())
                if (column == ktm.getSortByColumn())
                    if (ktm.getColumnClass(column) == K.KSymbolVector.class)
                        icon = new ScaledIcon(Util.getImage(Config.imageBase + "sort_az_descending.png"),targetHeight);
                    else
                        icon = new ScaledIcon(Util.getImage(Config.imageBase + "sort_ascending.png"),targetHeight);
            if (icon != null)
                setIcon(icon);
            else {
                icon = new ScaledIcon(Util.getImage(Config.imageBase + "sort_ascending.png"),targetHeight);
                setIcon(new BlankIcon(icon));
            }
        }

        String text = " ";
        if (value != null)
            text = value.toString() + " ";

        setText(text);

        return this;
    }
}