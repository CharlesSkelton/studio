package studio.kdb;

import javax.swing.border.Border;
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
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.CENTER);
        setOpaque(true);
        final Border border = UIManager.getBorder("TableHeader.cellBorder");
        if (border != null) {
            setBorder(border);
        }
        setFont(UIManager.getFont("TableHeader.font"));
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

            Insets insets = getInsets();
            int targetHeight = getFontMetrics(getFont()).getHeight() - insets.bottom - insets.top;
            KTableModel ktm = (KTableModel) table.getModel();
            if (ktm.isSortedDesc()) {
                if (column == ktm.getSortByColumn())
                    if (ktm.getColumnClass(column) == K.KSymbolVector.class)
                        icon = new ScaledIcon(Util.SORT_AZ_ASC_ICON,targetHeight);
                    else
                        icon = new ScaledIcon(Util.SORT_DESC_ICON,targetHeight);
            }
            else if (ktm.isSortedAsc())
                if (column == ktm.getSortByColumn())
                    if (ktm.getColumnClass(column) == K.KSymbolVector.class)
                        icon = new ScaledIcon(Util.SORT_AZ_DESC_ICON,targetHeight);
                    else
                        icon = new ScaledIcon(Util.SORT_ASC_ICON,targetHeight);
            if (icon != null)
                setIcon(icon);
            else {
                icon = new ScaledIcon(Util.SORT_ASC_ICON,targetHeight);
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
