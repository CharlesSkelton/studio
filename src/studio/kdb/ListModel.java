package studio.kdb;

public class ListModel extends KTableModel {
    private K.KBaseVector list;

    public ListModel(K.KBaseVector list) {
        this.list = list;
    }
    @Override
    public boolean isKey(int column) {
        return false;
    }

    @Override
    public K.KBaseVector getColumn(int col) {
        return list;
    }

    @Override
    public String getColumnName(int col) {
        return "value";
    }

    @Override
    public int getColumnCount() {
        return 1;
    }
}
