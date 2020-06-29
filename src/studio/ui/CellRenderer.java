package studio.ui;

import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.LimitedWriter;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

class CellRenderer extends DefaultTableCellRenderer {
    private static Color keyColor = new Color(220,255,220);
    private static Color altColor = new Color(220,220,255);
    private static Color nullColor = new Color(255,150,150);
    private static Color selColor = UIManager.getColor("Table.selectionBackground");
    private Color fgColor;
    private JTable table = null;

    private void initLabel(JTable table) {
        setHorizontalAlignment(SwingConstants.LEFT);
        setOpaque(true);
        int height = getPreferredSize().height;

    //    label.setFont(table.getTableHeader().getFont());
    //    label.setBackground(table.getTableHeader().getBackground());
    //    label.setForeground(table.getTableHeader().getForeground());
    // label.setBounds(1,1,1,1);
    }

    public CellRenderer(JTable t) {
        super();
        table = t;
        table.addPropertyChangeListener(new PropertyChangeListener() {
                                        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                                            if ("zoom".equals(propertyChangeEvent.getPropertyName()))
                                                setFont(table.getFont());
                                        }
                                    });

        initLabel(t);
        setFont(UIManager.getFont("Table.font"));
        setBackground(UIManager.getColor("Table.background"));
        fgColor = UIManager.getColor("Table.foreground");
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        //setText(TypeFormatter.format(value));
        //setText(value.toString());

        if (value instanceof K.KBase) {
            K.KBase kb = (K.KBase) value;
            LimitedWriter w = new LimitedWriter(Config.getInstance().getMaxCharsInTableCell());

            try {
                kb.toString(w,kb instanceof K.KBaseVector);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (LimitedWriter.LimitException ex) {
            }

            setText(w.toString());
            setForeground(kb.isNull() ? nullColor : fgColor);
        }
        else {
            // setText(value.toString());
            // setForeground(UIManager.getColor("Table.foreground"));
        }

        if (!isSelected) {
            KTableModel ktm = (KTableModel) table.getModel();
            column = table.convertColumnIndexToModel(column);
            if (ktm.isKey(column))
                setBackground(keyColor);
            else if (row % 2 == 0)
                setBackground(altColor);
            else
                setBackground(UIManager.getColor("Table.background"));
        }
        else {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(selColor);
        }
        /*
        int availableWidth= table.getColumnModel().getColumn(column).getWidth();
        availableWidth -= table.getIntercellSpacing().getWidth();
        Insets borderInsets = getBorder().getBorderInsets((Component)this);
        availableWidth -= (borderInsets.left + borderInsets.right);
        String cellText = getText();
        FontMetrics fm = getFontMetrics( getFont() );

        if (fm.stringWidth(cellText) > availableWidth)
        {
        String dots= "...";
        int textWidth = fm.stringWidth( dots );
        int nChars = cellText.length() - 1;
        for (; nChars > 0; nChars--)
        {
        textWidth += fm.charWidth(cellText.charAt(nChars));

        if (textWidth > availableWidth)
        {
        break;
        }
        }

        setText( dots + cellText.substring(nChars + 1));
        }
         **/

        return this;
    }
}
