package studio.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HtmlSelection implements Transferable {

    private static final DataFlavor[] HTML_FLAVORS;

    static {
        try {
            HTML_FLAVORS = new DataFlavor[]{
                    new DataFlavor("text/html"),
            };
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final String html;

    public HtmlSelection(String html) {
        this.html = html;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return HTML_FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return "text/html".equals(dataFlavor.getMimeType());
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
        return new ByteArrayInputStream(html.getBytes());
    }
}
