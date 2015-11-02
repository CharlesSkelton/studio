package studio.ui;

public class QuitHandler {
    private Studio s;

    public QuitHandler(Studio s) {
        this.s = s;
    }

    public boolean quit() {
        return s.quit();
    }
}
