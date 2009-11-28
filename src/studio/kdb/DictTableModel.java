/* Studio for kdb+ by Charles Skelton
   is licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 Germany License
   http://creativecommons.org/licenses/by-nc-sa/3.0
   except for the netbeans components which retain their original copyright notice
*/

package studio.kdb;

public class DictTableModel extends KTableModel {
    private K.Dict dict;

    public void setData(K.Dict obj) {
        dict = obj;
    }

    public DictTableModel() {
    }


    public void upsert(K.Dict upd){
        setData(upd);
//        dict.upsert(upd);
        if (isSortedAsc()) {
            asc(sortedByColumn);
        } else if (isSortedDesc()) {
            desc(sortedByColumn);
        }
    }

    public DictTableModel(K.Dict obj) {
        setData(obj);
    }

    public boolean isKey(int column) {
        K.Flip f = (K.Flip) dict.x;

        if (column < f.x.getLength())
            return true;
        return false;
    }

    public void asc(int col) {
        sortIndex = null;
        sortedByColumn = col;

        K.Flip f = (K.Flip) dict.x;
        K.KBaseVector v = null;

        if (col >= f.x.getLength()) {
            col -= f.x.getLength();
            f = (K.Flip) dict.y;
        }
        v = (K.KBaseVector) f.y.at(col);
        sortIndex = v.gradeUp();
        sorted = 1;
    }

    public void desc(int col) {
        sortIndex = null;
        sortedByColumn = col;

        K.Flip f = (K.Flip) dict.x;
        K.KBaseVector v = null;

        if (col >= f.x.getLength()) {
            col -= f.x.getLength();
            f = (K.Flip) dict.y;
        }
        v = (K.KBaseVector) f.y.at(col);
        sortIndex = v.gradeDown();
        sorted = -1;
    }

    public int getColumnCount() {
        return ((K.Flip) dict.x).x.getLength() + ((K.Flip) dict.y).x.getLength();
    }

    public int getRowCount() {
        return ((K.KBaseVector) ((K.Flip) dict.x).y.at(0)).getLength();
    }

    public Object getValueAt(int row,int col) {
        Object o = null;
        row = (sortIndex == null) ? row : sortIndex[row];
        K.Flip f = (K.Flip) dict.x;
        K.KBaseVector v = null;

        if (col >= f.x.getLength()) {
            col -= f.x.getLength();
            f = (K.Flip) dict.y;
        }

        v = (K.KBaseVector) f.y.at(col);
        o = v.at(row);

        //   if( o instanceof K.KBaseVector)
        // {
        //   o=K.decode((K.KBase)o);
        //  }

        return o;
    }

    public String getColumnName(int col) {
        K.KSymbolVector v = ((K.Flip) dict.x).x;

        if (col >= ((K.Flip) dict.x).x.getLength()) {
            col -= ((K.Flip) dict.x).x.getLength();
            v = ((K.Flip) dict.y).x;
        }
        return v.at(col).toString(false);
    }

    public Class getColumnClass(int col) {
        K.Flip f = (K.Flip) dict.x;
        K.KBaseVector v = null;

        if (col >= f.x.getLength()) {
            col -= f.x.getLength();
            f = (K.Flip) dict.y;
        }

        v = (K.KBaseVector) f.y.at(col);

        return v.getClass();
    }

    public K.KBaseVector getColumn(int col) {
        K.Flip f = (K.Flip) dict.x;
        K.KBaseVector v = null;

        if (col >= f.x.getLength()) {
            col -= f.x.getLength();
            f = (K.Flip) dict.y;
        }

        return (K.KBaseVector) f.y.at(col);
    }
};