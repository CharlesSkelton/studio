package studio.kdb;

public class DictModel extends KTableModel {

    private K.Dict dict;

    public DictModel(K.Dict dict) {
        this.dict = dict;
    }

    @Override
    public boolean isKey(int column) {
        return column == 0;
    }

    @Override
    public K.KBaseVector getColumn(int col) {
        return (K.KBaseVector) (col == 0 ? dict.x : dict.y);
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int col) {
        return col == 0 ? "key" : "value";
    }
}
