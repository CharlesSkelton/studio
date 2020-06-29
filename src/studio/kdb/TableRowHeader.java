package studio.kdb;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

public class TableRowHeader extends JList {
    private JTable table;

    public void recalcWidth() {
        Insets i = new RowHeaderRenderer().getInsets();
        int w = i.left + i.right;
        int width = SwingUtilities.computeStringWidth(table.getFontMetrics(getFont()),
                                                      (table.getRowCount() < 9999 ? "9999" : "" + (table.getRowCount() - 1)));
        // used to be rowcount - 1 as 0 based index
        setFixedCellWidth(w + width);
    }

    public TableRowHeader(final JTable table) {
        //  super();
        this.table = table;
        table.addPropertyChangeListener(new PropertyChangeListener() {
                                        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                                            if ("zoom".equals(propertyChangeEvent.getPropertyName())) {
                                                setFont(table.getFont());
                                                setFixedCellHeight(table.getRowHeight());
                                                recalcWidth();
                                                setCellRenderer(new RowHeaderRenderer());
                                            }
                                        }
                                    });
        setAutoscrolls(false);
        setCellRenderer(new RowHeaderRenderer());
        setFixedCellHeight(table.getRowHeight());
        setFont(table.getFont());
        recalcWidth();

        //setPreferredSize(new Dimension(w+width,table.getRowHeight()));
// setPreferredSize(new Dimension(width, 0));
        setFocusable(false);
        setModel(new TableListModel());
        setOpaque(false);
        setSelectionModel(table.getSelectionModel());
        if (table.getRowCount() > 0) {
            MouseInputAdapter mia = new MouseInputAdapter() {
                int startIndex = 0;

                public void mousePressed(MouseEvent e) {
                    int index = locationToIndex(e.getPoint());
                    startIndex = index;
                    table.setColumnSelectionInterval(0,table.getColumnCount() - 1);
                    table.setRowSelectionInterval(index,index);
                    table.requestFocus();
                }

                public void mouseReleased(MouseEvent e) {
                    int index = locationToIndex(e.getPoint());
                    table.setColumnSelectionInterval(0,table.getColumnCount() - 1);
                    table.setRowSelectionInterval(startIndex,index);
                    table.requestFocus();
                }

                public void mouseDragged(MouseEvent e) {
                    int index = locationToIndex(e.getPoint());
                    table.setColumnSelectionInterval(0,table.getColumnCount() - 1);
                    table.setRowSelectionInterval(startIndex,index);
                    table.requestFocus();
                }
                /*
                public void mouseMoved(MouseEvent e)
                {
                System.out.println("moved");
                int index= locationToIndex(e.getPoint());
                table.setColumnSelectionInterval(0,table.getColumnCount()-1);
                table.setRowSelectionInterval(startIndex, index);
                table.requestFocus();
                }
                 **/
            };
            addMouseListener(mia);
            addMouseMotionListener(mia);
        }
    }

    /*
    public void updateUI()
    {
    super.updateUI();
    setCellRenderer(new RowHeaderRenderer());

    //  setHeight(getFontMetrics(UIManager.getFont("TableHeader.font")).getHeight());
    if(table != null)
    setFixedCellHeight( table.getRowHeight());
    }
     **/
    class TableListModel extends AbstractListModel {
        public int getSize() {
            return table.getRowCount();
        }

        public Object getElementAt(int index) {
            return String.valueOf(index);
        }
    }

    class RowHeaderRenderer extends JLabel implements ListCellRenderer {
        RowHeaderRenderer() {
            super();
            setHorizontalAlignment(RIGHT);
            setVerticalAlignment(CENTER);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                        UIManager.getBorder("TableHeader.cellBorder"),
                        BorderFactory.createEmptyBorder(0,0,0,5)
                      ));
            //setFont(UIManager.getFont("Table.font"));
            setFont(table.getFont());
            setBackground(UIManager.getColor("TableHeader.background"));
            setForeground(UIManager.getColor("TableHeader.foreground"));
        }

        public Component getListCellRendererComponent(JList list,Object value,int index,boolean isSelected,boolean cellHasFocus) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
        /*        public void updateUI()
        {
        super.updateUI();
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setFont(UIManager.getFont("Table.font"));
        setBackground(UIManager.getColor("TableHeader.background"));
        setForeground(UIManager.getColor("TableHeader.foreground"));

        //setHeight(getFontMetrics(getFont()).getHeight());
        }
         **/
    }
}
