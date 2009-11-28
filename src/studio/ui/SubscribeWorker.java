/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package studio.ui;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;
import kx.c;
import studio.kdb.DictTableModel;
import studio.kdb.K.Dict;
import studio.kdb.K.Flip;
import studio.kdb.K.KBase;
import studio.kdb.K.KList;
import studio.kdb.KTableModel;

/**
 *
 * @author vidyser
 */
public class SubscribeWorker extends SwingWorker {

    private final c c;
    private final String tableName;
    private final QGrid grid;

    public SubscribeWorker(QGrid grid, kx.c c, String tableName) {
        this.grid = grid;
        this.c = c;
        this.tableName = tableName;
    }

    public c getC() {
        return c;
    }

    @Override
    protected Object doInBackground() throws Exception {
        try {
            Object r;
            while (!isCancelled()) {
                r = c.k();
                System.out.println("recieved response");
                publish(r);
                System.out.println("cont listening");
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void process(List chunks) {
        for (Object next : chunks) {
            if (next instanceof KList) {
                KBase update = ((KList) next).at(2);
                grid.append(update);
                grid.getWa().resizeAllColumns();
            } else {
                System.out.println("error while processing result:" + next);
            }
        }
    }

    @Override
    protected void done() {
        if(isCancelled()) return;
        JOptionPane.showMessageDialog(null, "Error while subscribing table", "Subscribing " + tableName, JOptionPane.WARNING_MESSAGE);
    }
}
