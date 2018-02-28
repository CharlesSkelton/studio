package studio.ui;

public class QuitHandler {
    private StudioPanel s;

    public QuitHandler(StudioPanel s) {
        this.s = s;
    }

    public boolean quit() {
        return s.quit();
    }
}
