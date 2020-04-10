package studio.kdb;

public class FlipTableModel extends KTableModel {

    private K.Flip flip;

    public FlipTableModel(K.Flip obj) {
        flip = obj;
    }

    public boolean isKey(int column) {
        return false;
    }

    public int getColumnCount() {
        return flip.x.getLength();
    }

    public String getColumnName(int i) {
        return flip.x.at(i).toString(false);
    }

    public K.KBaseVector getColumn(int col) {
        return (K.KBaseVector) flip.y.at(col);
    }
};
