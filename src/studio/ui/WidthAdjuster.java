/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.ui;

import studio.kdb.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

public class WidthAdjuster extends MouseAdapter {
    public WidthAdjuster(JTable table) {
        this.table = table;
        table.getTableHeader().addMouseListener(this);
    }

    public void mousePressed(MouseEvent evt) {
        if (evt.getClickCount() > 1 && usingResizeCursor())
            if ((table.getSelectedRowCount() == table.getRowCount()) && (table.getSelectedColumnCount() == table.getColumnCount()))
                resizeAllColumns();
            else
                resize(getLeftColumn(evt.getPoint()));
    }

    public void mouseClicked(final MouseEvent e) {
        if (!usingResizeCursor()) {
            JTableHeader h = (JTableHeader) e.getSource();
            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            if (viewColumn >= 0) {
                final int column = columnModel.getColumn(viewColumn).getModelIndex();

                KTableModel ktm = (KTableModel) table.getModel();
                //         if(Sorter.isSortable(ktm.getColumn(column)))
                {
                    if (ktm.isSortedAsc())
                        ktm.desc(column);
                    else if (ktm.isSortedDesc())
                        ktm.removeSort();
                    else
                        ktm.asc(column);

                    ktm.fireTableDataChanged();
                    if (h != null)
                        h.repaint();
                }
            }
        }
    }

    private JTableHeader getTableHeader() {
        return table.getTableHeader();
    }

    private boolean usingResizeCursor() {
        Cursor cursor = getTableHeader().getCursor();
        return cursor.equals(EAST) || cursor.equals(WEST);
    }
    private static final Cursor EAST = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    private static final Cursor WEST = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
    //if near the boundary, will choose left column
    private int getLeftColumn(Point pt) {
        pt.x -= EPSILON;
        return getTableHeader().columnAtPoint(pt);
    }

    public void resizeAllColumns() {
        for (int i = 0;i < table.getColumnCount();i++)
            resize(i);
    }

    private void resize(int col) {
        TableColumnModel tcm = table.getColumnModel();
        TableColumn tc = tcm.getColumn(col);
        TableCellRenderer tcr = tc.getHeaderRenderer();
        if (tcr == null)
            tcr = table.getTableHeader().getDefaultRenderer();

        int maxWidth = 0;
        Component comp = tcr.getTableCellRendererComponent(table,tc.getHeaderValue(),false,false,0,col);
        maxWidth = comp.getPreferredSize().width;

        int ub = table.getRowCount();

        int stepSize = ub / 1000;

        if (stepSize == 0)
            stepSize = 1;

        for (int i = 0;i < ub;i += stepSize) {
            tcr = table.getCellRenderer(i,col);
            Object obj = table.getValueAt(i,col);
            comp = tcr.getTableCellRendererComponent(table,obj,false,false,i,col);
            maxWidth = Math.max(maxWidth,comp.getPreferredSize().width);
        }

        maxWidth += 10; //and room to grow...
        tc.setPreferredWidth(maxWidth); //remembers the value
        tc.setWidth(maxWidth);          //forces layout, repaint
    }
    private JTable table;
    private static final int EPSILON = 5;   //boundary sensitivity
}