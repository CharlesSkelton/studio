/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.kdb;

public class FlipTableModel extends KTableModel {
    private K.Flip flip;

    public void append(K.Flip f) {
        flip.append(f);
        if(isSortedAsc())
            asc(sortedByColumn);
        else if(isSortedDesc())
            desc(sortedByColumn);
    }

    public void asc(int col) {
        sortIndex = null;
        K.KBaseVector v = (K.KBaseVector) flip.y.at(col);
        sortIndex = v.gradeUp();
        sorted = 1;
        sortedByColumn = col;
    }

    public void desc(int col) {
        sortIndex = null;
        K.KBaseVector v = (K.KBaseVector) flip.y.at(col);

        sortIndex = v.gradeDown();
        sorted = -1;
        sortedByColumn = col;
    }

    public void setData(K.Flip obj) {
        flip = obj;
    }

    public static boolean isTable(Object obj) {
        if (obj instanceof K.Flip)
            return true;
        else if (obj instanceof K.Dict) {
            K.Dict d = (K.Dict) obj;

            if ((d.x instanceof K.Flip) && (d.y instanceof K.Flip))
                return true;
        }

        return false;
    }

    public FlipTableModel() {
        super();
    }

    public FlipTableModel(K.Flip obj) {
        super();
        setData(obj);
    }

    public boolean isKey(int column) {
        return false;
    }

    public int getColumnCount() {
        return flip.x.getLength();
    }

    public int getRowCount() {
        return ((K.KBaseVector) flip.y.at(0)).getLength();
    }

    public Object getValueAt(int row,int col) {
        Object o = null;
        row = (sortIndex == null) ? row : sortIndex[row];
        K.KBaseVector v = (K.KBaseVector) flip.y.at(col);
        o = v.at(row);
        return o;
    }

    public String getColumnName(int i) {
        return flip.x.at(i).toString(false);
    }

    public Class getColumnClass(int col) {
        return flip.y.at(col).getClass();
    }

    public K.KBaseVector getColumn(int col) {
        return (K.KBaseVector) flip.y.at(col);
    }
};