package studio.kdb;

import javax.swing.table.AbstractTableModel;

public abstract class KTableModel extends AbstractTableModel {
    public abstract boolean isKey(int column);

    public abstract K.KBaseVector getColumn(int col);
    protected int[] sortIndex = null;
    protected int sorted = 0;
    protected int sortedByColumn = -1;

    public abstract void asc(int col);

    public abstract void desc(int col);

    public int getSortByColumn() {
        return sortedByColumn;
    }

    public boolean isSortedAsc() {
        return sorted == 1;
    }

    public boolean isSortedDesc() {
        return sorted == -1;
    }

    public void removeSort() {
        sortIndex = null;
        sorted = 0;
        sortedByColumn = -1;
    }
}
