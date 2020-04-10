package studio.kdb;

public class DictTableModel extends KTableModel {
    private K.Dict dict;

    public DictTableModel(K.Dict obj) {
        this.dict = obj;
    }

    public boolean isKey(int column) {
        K.Flip f = (K.Flip) dict.x;

        if (column < f.x.getLength())
            return true;
        return false;
    }

    public int getColumnCount() {
        return ((K.Flip) dict.x).x.getLength() + ((K.Flip) dict.y).x.getLength();
    }

    public String getColumnName(int col) {
        K.KSymbolVector v = ((K.Flip) dict.x).x;

        if (col >= ((K.Flip) dict.x).x.getLength()) {
            col -= ((K.Flip) dict.x).x.getLength();
            v = ((K.Flip) dict.y).x;
        }
        return v.at(col).toString(false);
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
