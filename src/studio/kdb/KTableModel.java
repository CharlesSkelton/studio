/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

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
